// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "KDownloaderUI",
    platforms: [
        .iOS(.v14), .macOS(.v11)
    ],
    products: [
        .library(
            name: "KDownloaderUI",
            targets: ["KDownloaderUI"]),
    ],
    targets: [
        .target(
            name: "KDownloaderUI",
            dependencies: ["kdownloader_core"]
        ),
        .binaryTarget(
            name: "kdownloader_core",
            path: "../kdownloader-flutter/ios/kdownloader_core.xcframework"
        )
    ]
)
