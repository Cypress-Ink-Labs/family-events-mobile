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
        // Pin by revision (v2.46.0) so CI/SPM cache restores don't fail exact-version lookup.
        .package(
            url: "https://github.com/supabase/supabase-swift",
            revision: "dd29b624b9ceea87612d0b00457e1400f7d22c2e"
        ),
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
            dependencies: ["FEData", "FEDataTesting", "FECore"],
            path: "Tests/FEDataTests"
        ),
    ]
)
