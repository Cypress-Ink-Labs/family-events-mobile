// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FEAppIntents",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FEAppIntents", targets: ["FEAppIntents"]),
    ],
    dependencies: [
        .package(path: "../FECore"),
    ],
    targets: [
        .target(name: "FEAppIntents", dependencies: ["FECore"], path: "Sources/FEAppIntents"),
        .testTarget(name: "FEAppIntentsTests", dependencies: ["FEAppIntents"], path: "Tests/FEAppIntentsTests"),
    ]
)
