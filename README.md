# ⚡ KDownloader

[![License](https://img.shields.io/badge/License-BSD--3--Clause-blue.svg)](LICENSE)
[![Build & Test](https://github.com/RoxyBasicNeedBot/KDownloader/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/RoxyBasicNeedBot/KDownloader/actions/workflows/build-and-test.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20Desktop%20%7C%20C%23%20%7C%20Flutter%20%7C%20React%20Native-lightgrey.svg)](#)

A modern, Kotlin-first **cross-platform download engine** designed for maximum speed and reliability. Featuring multi-chunk parallel downloading, dynamic chunk splitting, mirror server support, token-bucket speed throttling, and WorkManager persistence.

Provides first-class, idiomatic SDKs for **Android, iOS/macOS (Swift), Desktop (JVM), C# (.NET), Flutter (Dart), and React Native**.

---

## ✨ Features

- 🚀 **Dynamic Chunk Splitting**: IDM-style dynamic range theft automatically shifts workload from slow streams to faster connections.
- 📂 **Multi-Chunk Parallel Downloading**: Splits files into multiple segments and downloads them concurrently.
- 🪞 **Mirror Downloads**: Round-robin mirror server assignment for concurrent downloading from alternative sources.
- 🛡️ **WorkManager & Background Persistence**: Survives application termination and device reboots (Android 14-16 compliant).
- 📶 **Network Awareness**: Auto-pauses on network loss and auto-resumes when connection is restored.
- ⏱️ **Speed Throttling**: Configure global or per-task download speed caps using token-bucket rate limiting.
- 🔑 **Comprehensive Auth**: Built-in support for Basic, Bearer, Digest, and OAuth credentials.
- 🔗 **Clipboard Sniffer**: Automatically checks clipboard copy operations for downloadable link patterns.
- 📦 **Post-Processing Hooks**: Chains operations like auto-extracting (ZIP/TAR/GZ) and hashing (MD5, SHA-256).

---

## 🛠️ Module Architecture

```
KDownloader/
├── kdownloader-core/              → Shared KMP core logic (Pure Kotlin)
├── kdownloader-android/           → Android platform (WorkManager + Room + Notifications)
├── kdownloader-ios/               → iOS Swift SDK (URLSession + BGTaskScheduler + SKIE)
├── kdownloader-desktop/           → Desktop JVM wrapper (SQLite + System Tray)
├── kdownloader-hilt/              → Dagger Hilt integration
├── kdownloader-compose/           → Jetpack Compose UI components
├── kdownloader-swiftui/           → SwiftUI components
├── kdownloader-dotnet/            → C# .NET NuGet package via P/Invoke
├── kdownloader-flutter/           → Flutter/Dart plugin
└── kdownloader-react-native/      → React Native bridge
```

---

## 📦 Installation

**Android / Desktop (Gradle)**
```kotlin
implementation("com.roxybasicneedbot.kdownloader:core:2.6.0")
// Optional
implementation("com.roxybasicneedbot.kdownloader:android:2.6.0")
implementation("com.roxybasicneedbot.kdownloader:compose:2.6.0")
```

**iOS (Swift Package Manager)**
```swift
dependencies: [
    .package(url: "https://github.com/RoxyBasicNeedBot/KDownloader.git", from: "2.6.0")
]
```

**.NET (NuGet)**
```bash
dotnet add package KDownloader.Net --version 2.6.0
```

**Flutter (pub.dev)**
```yaml
dependencies:
  kdownloader_flutter: ^2.6.0
```

**React Native (npm)**
```bash
npm install kdownloader-react-native
```

---

## 💻 Idiomatic Usage Examples

### 🟣 Kotlin (Android / Desktop)
```kotlin
val downloader = KDownloader.getInstance(context)

val id = downloader.enqueue(
    DownloadRequest.Builder("https://example.com/largefile.zip", "/downloads/", "file.zip")
        .setPriority(DownloadPriority.HIGH)
        .setChunkCount(8)
        .setWifiOnly(true)
        .setSpeedLimit(2_000_000) // 2 MB/s cap
        .addMirrorUrl("https://mirror.example.com/file.zip")
        .build()
)

// Observe download progress reactively
downloader.observe(id).collect { state ->
    when (state) {
        is DownloadState.Downloading -> println("${state.progress.percent}% at ${state.progress.speedFormatted}")
        is DownloadState.Done -> println("Saved to ${state.result.filePath}")
        is DownloadState.Failed -> println("Failed: ${state.error.message}")
        else -> Unit
    }
}
```

### 🍎 Swift (iOS / macOS)
```swift
let downloader = KDownloader.shared

let id = try await downloader.enqueue(
    DownloadRequest(
        url: "https://example.com/movie.mp4",
        destination: .documentsDirectory,
        fileName: "movie.mp4",
        chunkCount: 6
    )
)

// Exhaustive Swift matching via SKIE interop
for await state in downloader.observe(id) {
    switch state {
    case .downloading(let progress):
        print("\(progress.percent)% downloaded")
    case .done(let result):
        print("Completed: \(result.filePath)")
    case .failed(let error, _):
        print("Error: \(error.localizedDescription)")
    default: break
    }
}
```

### 🔷 C# (.NET)
```csharp
using KDownloader;

var client = new KDownloaderClient();
var id = await client.EnqueueAsync(new DownloadRequest 
{
    Url = "https://example.com/asset.pkg",
    DestinationPath = @"C:\Downloads\",
    FileName = "asset.pkg",
    ChunkCount = 8
});

await foreach (var state in client.ObserveAsync(id)) 
{
    if (state is DownloadState.Downloading d) {
        Console.WriteLine($"{d.Progress.Percent}% | {d.Progress.SpeedFormatted}");
    }
}
```

### 🐦 Dart (Flutter)
```dart
import 'package:kdownloader/kdownloader.dart';

final downloader = KDownloader();
final id = await downloader.enqueue(
  'https://example.com/file.zip',
  savePath: '/downloads/',
  chunkCount: 4,
);

downloader.observe(id).listen((state) {
  state.when(
    downloading: (progress) => print('${progress.percent}%'),
    done: (result) => print('Downloaded to: ${result.filePath}'),
    failed: (error, _) => print('Error: $error'),
  );
});
```

### ⚛️ React Native (TypeScript)
```typescript
import KDownloader from 'kdownloader-react-native';

const id = await KDownloader.enqueue({
  id: 'task-1',
  url: 'https://example.com/file.zip',
  destinationDir: '/downloads',
  fileName: 'file.zip',
  chunkCount: 4
});

KDownloader.observe((states) => {
  const task = states.find(s => s.id === id);
  if (task && task.status === 'DOWNLOADING') {
    console.log(`${task.progress.percent}% | ${task.progress.speedFormatted}`);
  }
});
```

---

## 📄 License

This project is licensed under the BSD 3-Clause License - see the [LICENSE](LICENSE) file for details.
