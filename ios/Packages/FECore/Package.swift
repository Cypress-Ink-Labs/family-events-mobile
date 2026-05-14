// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FECore",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FECore", targets: ["FECore"]),
    ],
    targets: [
        .target(name: "FECore", path: "Sources/FECore"),
        .testTarget(name: "FECoreTests", dependencies: ["FECore"], path: "Tests/FECoreTests"),
    ]
)
