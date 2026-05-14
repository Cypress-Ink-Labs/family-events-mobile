// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FEData",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FEData", targets: ["FEData"]),
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
        .testTarget(
            name: "FEDataTests",
            dependencies: ["FEData", "FECore"],
            path: "Tests/FEDataTests"
        ),
    ]
)
