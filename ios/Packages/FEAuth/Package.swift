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
        // Keep in sync with FEData — supabase-swift v2.46.0 (revision pin for CI determinism).
        .package(
            url: "https://github.com/supabase/supabase-swift",
            revision: "dd29b624b9ceea87612d0b00457e1400f7d22c2e"
        ),
        .package(url: "https://github.com/google/GoogleSignIn-iOS", from: "9.1.0"),
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
                .product(name: "GoogleSignIn", package: "GoogleSignIn-iOS"),
                .product(name: "GoogleSignInSwift", package: "GoogleSignIn-iOS"),
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
