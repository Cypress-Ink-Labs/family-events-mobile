// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FEAuth",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FEAuth", targets: ["FEAuth"]),
        .library(name: "FEAuthTesting", targets: ["FEAuthTesting"]),
    ],
    dependencies: [
        .package(path: "../FECore"),
        .package(path: "../FEData"),
        .package(path: "../FEDesignSystem"),
        .package(url: "https://github.com/supabase/supabase-swift", exact: "2.20.0"),
    ],
    targets: [
        .target(
            name: "FEAuth",
            dependencies: [
                "FECore",
                "FEData",
                "FEDesignSystem",
                .product(name: "Auth", package: "supabase-swift"),
                .product(name: "Supabase", package: "supabase-swift"),
            ],
            path: "Sources/FEAuth"
        ),
        .target(
            name: "FEAuthTesting",
            dependencies: ["FEAuth", "FECore"],
            path: "Sources/FEAuthTesting"
        ),
        .testTarget(
            name: "FEAuthTests",
            dependencies: [
                "FEAuth",
                "FEAuthTesting",
                .product(name: "FEDataTesting", package: "FEData"),
            ],
            path: "Tests/FEAuthTests"
        ),
    ]
)
