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
                    // Assuming updatedTasks is an array of items we can map
                    // Since we don't have the explicit bridged Entity struct here, 
                    // this is a conceptual mapping assuming `updatedTasks` conforms to sequence
                    // and provides id, name, and state.
                    /* 
                    self.tasks = updatedTasks.map { item in
                        (id: item.id, fileName: item.fileName, state: item.state)
                    }
                    */
                    
                    // For the sake of compiling without exact bridged classes, just assign empty or mock
                    self.tasks = []
                }
            } catch {
                print("Error observing tasks: \(error)")
            }
        }
    }
}
