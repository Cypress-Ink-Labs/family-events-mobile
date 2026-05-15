// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FEData",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FEData", targets: ["FEData"]),
        .library(name: "FEDataTesting", targets: ["FEDataTesting"]),
    ],
    dependencies: [
        .package(path: "../FECore"),
        .package(url: "https://github.com/supabase/supabase-swift", exact: "2.20.0"),
    ],
    targets: [
        .target(
            name: "FEData",
            dependencies: [
                "FECore",
                .product(name: "Supabase", package: "supabase-swift"),
            ],
            path: "Sources/FEData"
        ),
        .target(
            name: "FEDataTesting",
            dependencies: ["FEData", "FECore"],
            path: "Sources/FEDataTesting"
        ),
        .testTarget(
            name: "FEDataTests",
            dependencies: ["FEData", "FEDataTesting"],
            path: "Tests/FEDataTests"
        ),
    ]
)
