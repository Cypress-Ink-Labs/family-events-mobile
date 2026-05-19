// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FEAdmin",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FEAdmin", targets: ["FEAdmin"]),
    ],
    dependencies: [
        .package(path: "../FECore"),
        .package(path: "../FEData"),
        .package(path: "../FEDesignSystem"),
    ],
    targets: [
        .target(
            name: "FEAdmin",
            dependencies: ["FECore", "FEData", "FEDesignSystem"],
            path: "Sources/FEAdmin"
        ),
        .testTarget(
            name: "FEAdminTests",
            dependencies: ["FEAdmin"],
            path: "Tests/FEAdminTests"
        ),
    ]
)
