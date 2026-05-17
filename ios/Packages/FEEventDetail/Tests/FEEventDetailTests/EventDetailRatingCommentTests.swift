import XCTest
import FECore
import FEData
import FEDataTesting
@testable import FEEventDetail

@MainActor
final class EventDetailRatingCommentTests: XCTestCase {
    private let userID = UserID("u")
    private let eventID = EventID("evt_1")

    private func makeVM(
        eventRepo: FakeEventRepository = FakeEventRepository(),
        favoriteRepo: FakeFavoriteRepo = FakeFavoriteRepo(),
        ratingRepo: FakeRatingRepo? = FakeRatingRepo(),
        commentRepo: FakeCommentRepo? = FakeCommentRepo()
    ) -> EventDetailViewModel {
        EventDetailViewModel(
            eventRepo: eventRepo,
            favoriteRepo: favoriteRepo,
            ratingRepo: ratingRepo,
            commentRepo: commentRepo,
            userID: userID,
            eventID: eventID
        )
    }

    func testLoadFetchesUserRatingAndComments() async {
        let eventRepo = FakeEventRepository()
        eventRepo.fetchByIDsResult = .success([EventDTO.fixture(id: "evt_1", title: "x")])
        let ratingRepo = FakeRatingRepo(userRatingResult: RatingDTO(
            id: "r1", userID: userID, eventID: eventID, score: 4, createdAt: Date()
        ))
        let commentRepo = FakeCommentRepo(comments: [
            CommentDTO(id: "c1", userID: userID, eventID: eventID, body: "Yay",
                       isApproved: true, isFlagged: false,
                       createdAt: Date(), updatedAt: Date())
        ])
        let vm = makeVM(eventRepo: eventRepo, ratingRepo: ratingRepo, commentRepo: commentRepo)
        await vm.load()
        XCTAssertEqual(vm.userRating?.score, 4)
        XCTAssertEqual(vm.comments.count, 1)
        XCTAssertEqual(vm.comments.first?.body, "Yay")
    }

    func testSetRatingUpsertsViaRepo() async {
        let repo = FakeRatingRepo()
        let vm = makeVM(ratingRepo: repo)
        await vm.setRating(5)
        XCTAssertEqual(repo.upsertedScores, [5])
        XCTAssertEqual(vm.userRating?.score, 5)
    }

    func testSetRatingRollsBackOnError() async {
        struct Boom: Error {}
        let repo = FakeRatingRepo()
        repo.upsertError = Boom()
        let vm = makeVM(ratingRepo: repo)
        await vm.setRating(3)
        XCTAssertNil(vm.userRating)
        XCTAssertNotNil(vm.errorMessage)
    }

    func testAddCommentInsertsAtFront() async {
        let repo = FakeCommentRepo()
        let vm = makeVM(commentRepo: repo)
        await vm.addComment("First!")
        XCTAssertEqual(repo.addedBodies, ["First!"])
        XCTAssertEqual(vm.comments.first?.body, "First!")
    }

    func testAddCommentIgnoresEmpty() async {
        let repo = FakeCommentRepo()
        let vm = makeVM(commentRepo: repo)
        await vm.addComment("   ")
        XCTAssertTrue(repo.addedBodies.isEmpty)
        XCTAssertTrue(vm.comments.isEmpty)
    }

    func testAddCommentSetsErrorOnFailure() async {
        struct Boom: Error {}
        let repo = FakeCommentRepo()
        repo.addError = Boom()
        let vm = makeVM(commentRepo: repo)
        await vm.addComment("Hi")
        XCTAssertNotNil(vm.commentError)
        XCTAssertTrue(vm.comments.isEmpty)
    }

    func testNilReposAreNoOps() async {
        let vm = makeVM(ratingRepo: nil, commentRepo: nil)
        await vm.setRating(5)
        await vm.addComment("x")
        XCTAssertNil(vm.userRating)
        XCTAssertTrue(vm.comments.isEmpty)
    }
}
