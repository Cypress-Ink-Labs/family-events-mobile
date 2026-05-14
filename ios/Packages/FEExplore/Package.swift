// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FEExplore",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FEExplore", targets: ["FEExplore"]),
    ],
    dependencies: [
        .package(path: "../FECore"),
        .package(path: "../FEData"),
        .package(path: "../FEDesignSystem"),
    ],
    targets: [
        .target(
            name: "FEExplore",
            dependencies: ["FECore", "FEData", "FEDesignSystem"],
            path: "Sources/FEExplore"
        ),
        .testTarget(name: "FEExploreTests", dependencies: ["FEExplore"], path: "Tests/FEExploreTests"),
    ]
)
