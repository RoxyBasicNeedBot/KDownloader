import SwiftUI
import kdownloader_core

public struct KDownloadButton: View {
    public let taskId: String
    public let request: DownloadRequest?
    
    @State private var progress: Double = 0.0
    @State private var status: String = "IDLE"
    
    public init(taskId: String, request: DownloadRequest? = nil) {
        self.taskId = taskId
        self.request = request
    }
    
    public var body: some View {
        Button(action: {
            toggleDownload()
        }) {
            ZStack {
                // Background track
                Circle()
                    .stroke(lineWidth: 3.0)
                    .opacity(0.2)
                    .foregroundColor(Color.accentColor)
                
                // Progress or Icon
                if status == "DOWNLOADING" {
                    Circle()
                        .trim(from: 0.0, to: CGFloat(min(self.progress, 1.0)))
                        .stroke(style: StrokeStyle(lineWidth: 3.0, lineCap: .round, lineJoin: .round))
                        .foregroundColor(Color.accentColor)
                        .rotationEffect(Angle(degrees: 270.0))
                        .animation(.linear(duration: 0.2), value: progress)
                    
                    Image(systemName: "pause.fill")
                        .font(.system(size: 14, weight: .bold))
                } else if status == "DONE" {
                    Image(systemName: "checkmark")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.green)
                } else if status == "PAUSED" {
                    Image(systemName: "play.fill")
                        .font(.system(size: 14, weight: .bold))
                } else {
                    Image(systemName: "arrow.down")
                        .font(.system(size: 14, weight: .bold))
                }
            }
            .frame(width: 36, height: 36)
        }
        .buttonStyle(PlainButtonStyle())
        .task {
            await observeTask()
        }
    }
    
    private func toggleDownload() {
        let downloader = KDownloader.companion.instance
        if status == "IDLE" || status == "FAILED" || status == "CANCELLED" {
            // Initiate download
            if let req = request {
                _ = downloader.enqueue(request: req)
            }
        } else if status == "DOWNLOADING" {
            downloader.pause(id: taskId)
        } else if status == "PAUSED" {
            downloader.resume(id: taskId)
        }
    }
    
    private func observeTask() async {
        let downloader = KDownloader.companion.instance
        
        do {
            // SKIE translates Kotlin Flow to Swift AsyncSequence automatically
            let stateFlow = downloader.observe(id: taskId)
            
            for await state in stateFlow {
                await MainActor.run {
                    if let downloading = state as? DownloadState.Downloading {
                        self.status = "DOWNLOADING"
                        self.progress = Double(downloading.progress.percent) / 100.0
                    } else if state is DownloadState.Done {
                        self.status = "DONE"
                        self.progress = 1.0
                    } else if state is DownloadState.Paused {
                        self.status = "PAUSED"
                    } else if state is DownloadState.Failed {
                        self.status = "FAILED"
                        self.progress = 0.0
                    } else {
                        self.status = "IDLE"
                    }
                }
            }
        } catch {
            print("KDownloader UI observation error: \(error)")
        }
    }
}
