// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FECityPicker",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FECityPicker", targets: ["FECityPicker"]),
    ],
    dependencies: [
        .package(path: "../FECore"),
        .package(path: "../FEData"),
        .package(path: "../FEDesignSystem"),
    ],
    targets: [
        .target(
            name: "FECityPicker",
            dependencies: ["FECore", "FEData", "FEDesignSystem"],
            path: "Sources/FECityPicker"
        ),
        .testTarget(
            name: "FECityPickerTests",
            dependencies: ["FECityPicker", "FECore", "FEData", .product(name: "FEDataTesting", package: "FEData")],
            path: "Tests/FECityPickerTests"
        ),
    ]
)
