// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FEDesignSystem",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FEDesignSystem", targets: ["FEDesignSystem"]),
    ],
    dependencies: [
        .package(path: "../FECore"),
        .package(url: "https://github.com/pointfreeco/swift-snapshot-testing", from: "1.18.0"),
    ],
    targets: [
        .target(name: "FEDesignSystem", dependencies: ["FECore"], path: "Sources/FEDesignSystem"),
        .testTarget(
            name: "FEDesignSystemTests",
            dependencies: [
                "FEDesignSystem",
                .product(name: "SnapshotTesting", package: "swift-snapshot-testing"),
            ],
            path: "Tests/FEDesignSystemTests"
        ),
    ]
)
