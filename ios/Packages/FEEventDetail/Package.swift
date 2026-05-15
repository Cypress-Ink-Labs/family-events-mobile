// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FEEventDetail",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FEEventDetail", targets: ["FEEventDetail"]),
    ],
    dependencies: [
        .package(path: "../FECore"),
        .package(path: "../FEData"),
        .package(path: "../FEDesignSystem"),
    ],
    targets: [
        .target(
            name: "FEEventDetail",
            dependencies: ["FECore", "FEData", "FEDesignSystem"],
            path: "Sources/FEEventDetail"
        ),
        .testTarget(
            name: "FEEventDetailTests",
            dependencies: [
                "FEEventDetail",
                "FECore",
                "FEData",
                .product(name: "FEDataTesting", package: "FEData"),
            ],
            path: "Tests/FEEventDetailTests"
        ),
    ]
)
