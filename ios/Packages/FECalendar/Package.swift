// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FECalendar",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FECalendar", targets: ["FECalendar"]),
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
            name: "FECalendar",
            dependencies: ["FECore", "FEData", "FEDesignSystem", "FEEventDetail", "FECityPicker"],
            path: "Sources/FECalendar"
        ),
        .testTarget(
            name: "FECalendarTests",
            dependencies: ["FECalendar", "FECore", "FEData", .product(name: "FEDataTesting", package: "FEData")],
            path: "Tests/FECalendarTests"
        ),
    ]
)
