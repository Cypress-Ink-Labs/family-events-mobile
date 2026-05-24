// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FEMap",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FEMap", targets: ["FEMap"]),
    ],
    dependencies: [
        .package(path: "../FECore"),
        .package(path: "../FEData"),
        .package(path: "../FEDesignSystem"),
        .package(path: "../FEEventDetail"),
        .package(path: "../FECityPicker"),
    ],
    targets: [
        .target(
            name: "FEMap",
            dependencies: ["FECore", "FEData", "FEDesignSystem", "FEEventDetail", "FECityPicker"],
            path: "Sources/FEMap"
        ),
        .testTarget(
            name: "FEMapTests",
            dependencies: ["FEMap", "FECore", "FEData", .product(name: "FEDataTesting", package: "FEData")],
            path: "Tests/FEMapTests"
        ),
    ]
)
