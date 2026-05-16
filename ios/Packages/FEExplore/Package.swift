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
        .package(path: "../FEEventDetail"),
    ],
    targets: [
        .target(
            name: "FEExplore",
            dependencies: ["FECore", "FEData", "FEDesignSystem", "FEEventDetail"],
            path: "Sources/FEExplore"
        ),
        .testTarget(
            name: "FEExploreTests",
            dependencies: [
                "FEExplore",
                "FECore",
                .product(name: "FEDataTesting", package: "FEData"),
            ],
            path: "Tests/FEExploreTests"
        ),
    ]
)
