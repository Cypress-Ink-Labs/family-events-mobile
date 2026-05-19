// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "FEDesignSystem",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "FEDesignSystem", targets: ["FEDesignSystem"]),
    ],
    dependencies: [
        .package(path: "../FECore"),
        .package(url: "https://github.com/pointfreeco/swift-snapshot-testing", from: "1.19.2"),
    ],
    targets: [
        .target(
            name: "FEDesignSystem",
            dependencies: ["FECore"],
            path: "Sources/FEDesignSystem",
            resources: [
                // Bundles every font file dropped into Resources/Fonts/.
                // FEDesignSystem.registerFonts() iterates the directory at
                // runtime via CTFontManager, so adding a new .ttf/.otf
                // requires no code changes — the file just needs to be
                // listed in the design-system font roster.
                .process("Resources/Fonts"),
            ]
        ),
        .testTarget(
            name: "FEDesignSystemTests",
            dependencies: [
                "FEDesignSystem",
                .product(name: "SnapshotTesting", package: "swift-snapshot-testing"),
            ],
            path: "Tests/FEDesignSystemTests"
        ),
    ]
)
