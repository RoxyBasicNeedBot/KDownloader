import SwiftUI
import kdownloader_core

// Assuming DownloadTaskEntity is bridged, or we use a basic tuple representation
public struct DownloadQueueView: View {
    @State private var tasks: [(id: String, fileName: String, state: DownloadState)] = []
    private let downloader = KDownloader.companion.instance
    
    public init() {}
    
    public var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                if tasks.isEmpty {
                    Text("No downloads currently in queue.")
                        .foregroundColor(.secondary)
                        .padding()
                } else {
                    ForEach(tasks, id: \.id) { task in
                        DownloadProgressRow(
                            fileName: task.fileName,
                            state: task.state,
                            onPause: {
                                Task { try? await downloader.pause(id: task.id) }
                            },
                            onResume: {
                                Task { try? await downloader.resume(id: task.id) }
                            },
                            onCancel: {
                                Task { try? await downloader.cancel(id: task.id) }
                            }
                        )
                    }
                }
            }
            .padding()
        }
        .task {
            do {
                // Skie bridges Flow to Swift AsyncSequence
                for try await updatedTasks in downloader.observeAll() {
                    let mapped = updatedTasks.map { item in
                        (id: item.id, fileName: item.fileName, state: item.state)
                    }
                    
                    DispatchQueue.main.async {
                        self.tasks = mapped
                    }
                }
            } catch {
                print("Error observing tasks: \(error)")
            }
        }
    }
}
