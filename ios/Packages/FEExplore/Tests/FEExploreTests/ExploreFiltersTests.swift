import XCTest
@testable import FEExplore

final class ExploreFiltersTests: XCTestCase {
    func testDefaultFiltersAreEmpty() {
        let f = ExploreFilters()
        XCTAssertEqual(f.keyword, "")
        XCTAssertEqual(f.dateFilter, .anytime)
        XCTAssertFalse(f.onlyFree)
        XCTAssertEqual(f.activeCount, 0)
    }

    func testActiveCountKeywordOnly() {
        var f = ExploreFilters()
        f.keyword = "park"
        XCTAssertEqual(f.activeCount, 1)
    }

    func testActiveCountAllFilters() {
        var f = ExploreFilters()
        f.keyword = "music"
        f.dateFilter = .today
        f.onlyFree = true
        XCTAssertEqual(f.activeCount, 3)
    }

    func testAnytimeDateRangeIsNil() {
        let f = ExploreFilters(dateFilter: .anytime)
        let range = f.dateRange
        XCTAssertNil(range.from)
        XCTAssertNil(range.to)
    }

    func testTodayDateRangeIsOneDay() {
        let f = ExploreFilters(dateFilter: .today)
        let range = f.dateRange
        XCTAssertNotNil(range.from)
        XCTAssertNotNil(range.to)
        let diff = range.to!.timeIntervalSince(range.from!)
        XCTAssertEqual(diff, 86400, accuracy: 1.0)
    }

    func testWeekDateRangeIsSevenDays() {
        let f = ExploreFilters(dateFilter: .week)
        let range = f.dateRange
        XCTAssertNotNil(range.from)
        XCTAssertNotNil(range.to)
        let diff = range.to!.timeIntervalSince(range.from!)
        XCTAssertEqual(diff, 7 * 86400, accuracy: 3600.0)
    }

    func testMonthDateRangeFromStartOfToday() {
        let f = ExploreFilters(dateFilter: .month)
        let range = f.dateRange
        XCTAssertNotNil(range.from)
        XCTAssertNotNil(range.to)
        XCTAssertTrue(range.to! > range.from!)
    }

    func testEquality() {
        let f1 = ExploreFilters(keyword: "art", dateFilter: .today, onlyFree: true)
        let f2 = ExploreFilters(keyword: "art", dateFilter: .today, onlyFree: true)
        XCTAssertEqual(f1, f2)
    }

    func testInequalityOnKeyword() {
        let f1 = ExploreFilters(keyword: "art")
        let f2 = ExploreFilters(keyword: "music")
        XCTAssertNotEqual(f1, f2)
    }

    func testDateFilterCasesHaveLabels() {
        XCTAssertEqual(ExploreFilters.DateFilter.anytime.rawValue, "Anytime")
        XCTAssertEqual(ExploreFilters.DateFilter.today.rawValue, "Today")
        XCTAssertEqual(ExploreFilters.DateFilter.weekend.rawValue, "This weekend")
        XCTAssertEqual(ExploreFilters.DateFilter.week.rawValue, "This week")
        XCTAssertEqual(ExploreFilters.DateFilter.month.rawValue, "This month")
    }

    // MARK: - AgeFilter

    func testAgeFilterIncrementsActiveCount() {
        var f = ExploreFilters()
        f.ageFilter = .zeroToOne
        XCTAssertEqual(f.activeCount, 1)
    }

    func testActiveCategoryIncrementsActiveCount() {
        var f = ExploreFilters()
        f.activeCategory = "playgroup"
        XCTAssertEqual(f.activeCount, 1)
    }

    func testAllFiveFiltersActiveCount() {
        var f = ExploreFilters()
        f.keyword = "park"
        f.dateFilter = .today
        f.onlyFree = true
        f.ageFilter = .zeroToOne
        f.activeCategory = "playgroup"
        XCTAssertEqual(f.activeCount, 5)
    }

    func testAgeFilterBucketValues() {
        XCTAssertEqual(ExploreFilters.AgeFilter.zeroToOne.min, 0)
        XCTAssertEqual(ExploreFilters.AgeFilter.zeroToOne.max, 1)

        XCTAssertEqual(ExploreFilters.AgeFilter.oneToThree.min, 1)
        XCTAssertEqual(ExploreFilters.AgeFilter.oneToThree.max, 3)

        XCTAssertEqual(ExploreFilters.AgeFilter.twoToFour.min, 2)
        XCTAssertEqual(ExploreFilters.AgeFilter.twoToFour.max, 4)

        XCTAssertEqual(ExploreFilters.AgeFilter.fiveToEight.min, 5)
        XCTAssertEqual(ExploreFilters.AgeFilter.fiveToEight.max, 8)

        XCTAssertEqual(ExploreFilters.AgeFilter.nineAndUp.min, 9)
        XCTAssertNil(ExploreFilters.AgeFilter.nineAndUp.max)
    }

    func testAgeFilterRawValues() {
        XCTAssertEqual(ExploreFilters.AgeFilter.zeroToOne.rawValue, "0–1 yr")
        XCTAssertEqual(ExploreFilters.AgeFilter.oneToThree.rawValue, "1–3 yrs")
        XCTAssertEqual(ExploreFilters.AgeFilter.twoToFour.rawValue, "2–4 yrs")
        XCTAssertEqual(ExploreFilters.AgeFilter.fiveToEight.rawValue, "5–8 yrs")
        XCTAssertEqual(ExploreFilters.AgeFilter.nineAndUp.rawValue, "9+ yrs")
    }

    func testAgeFilterHasFiveCases() {
        XCTAssertEqual(ExploreFilters.AgeFilter.allCases.count, 5)
    }
}
