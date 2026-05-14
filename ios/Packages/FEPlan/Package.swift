// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FEPlan",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FEPlan", targets: ["FEPlan"]),
    ],
    dependencies: [
        .package(path: "../FECore"),
        .package(path: "../FEData"),
        .package(path: "../FEDesignSystem"),
    ],
    targets: [
        .target(
            name: "FEPlan",
            dependencies: ["FECore", "FEData", "FEDesignSystem"],
            path: "Sources/FEPlan"
        ),
        .testTarget(name: "FEPlanTests", dependencies: ["FEPlan"], path: "Tests/FEPlanTests"),
    ]
)
