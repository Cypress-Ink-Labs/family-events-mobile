// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FESaved",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [.library(name: "FESaved", targets: ["FESaved"])],
    dependencies: [
        .package(path: "../FECore"),
        .package(path: "../FEData"),
        .package(path: "../FEDesignSystem"),
        .package(path: "../FEEventDetail"),
    ],
    targets: [
        .target(name: "FESaved", dependencies: ["FECore", "FEData", "FEDesignSystem", "FEEventDetail"], path: "Sources/FESaved"),
        .testTarget(
            name: "FESavedTests",
            dependencies: [
                "FESaved",
                "FECore",
                "FEData",
                .product(name: "FEDataTesting", package: "FEData"),
            ],
            path: "Tests/FESavedTests"
        ),
    ]
)
