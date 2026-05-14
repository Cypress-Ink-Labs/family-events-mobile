// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FEAuth",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FEAuth", targets: ["FEAuth"]),
    ],
    dependencies: [
        .package(path: "../FECore"),
        .package(path: "../FEData"),
    ],
    targets: [
        .target(name: "FEAuth", dependencies: ["FECore", "FEData"], path: "Sources/FEAuth"),
        .testTarget(name: "FEAuthTests", dependencies: ["FEAuth"], path: "Tests/FEAuthTests"),
    ]
)
