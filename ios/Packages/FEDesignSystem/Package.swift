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
    ],
    targets: [
        .target(name: "FEDesignSystem", dependencies: ["FECore"], path: "Sources/FEDesignSystem"),
        .testTarget(name: "FEDesignSystemTests", dependencies: ["FEDesignSystem"], path: "Tests/FEDesignSystemTests"),
    ]
)
