import SwiftUI
import kdownloader_core

public struct DownloadProgressRow: View {
    let fileName: String
    let state: DownloadState
    let onPause: () -> Void
    let onResume: () -> Void
    let onCancel: () -> Void
    
    @Environment(\.kdownloaderTheme) var theme
    
    public init(
        fileName: String,
        state: DownloadState,
        onPause: @escaping () -> Void,
        onResume: @escaping () -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.fileName = fileName
        self.state = state
        self.onPause = onPause
        self.onResume = onResume
        self.onCancel = onCancel
    }
    
    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "doc.fill")
                    .foregroundColor(theme.accentColor)
                Text(fileName)
                    .font(.headline)
                    .lineLimit(1)
                Spacer()
                Text(stateDescription)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            if let progress = extractProgress(from: state) {
                ProgressView(value: Double(progress.percent), total: 100.0)
                    .progressViewStyle(.linear)
                    .tint(theme.accentColor)
                
                HStack {
                    Text("\(progress.downloadedBytes) bytes")
                    Spacer()
                    Text("\(progress.speedFormatted) - ETA: \(progress.etaFormatted)")
                }
                .font(.caption)
                .foregroundColor(.secondary)
            } else {
                ProgressView()
                    .progressViewStyle(.linear)
                    .opacity(0.5)
            }
            
            HStack {
                Spacer()
                if canPause {
                    Button(action: onPause) {
                        Image(systemName: "pause.circle.fill")
                            .font(.title2)
                    }
                    .buttonStyle(.plain)
                    .foregroundColor(theme.accentColor)
                }
                if canResume {
                    Button(action: onResume) {
                        Image(systemName: "play.circle.fill")
                            .font(.title2)
                    }
                    .buttonStyle(.plain)
                    .foregroundColor(theme.accentColor)
                }
                Button(action: onCancel) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title2)
                }
                .buttonStyle(.plain)
                .foregroundColor(.red)
            }
        }
        .padding()
        .background(theme.rowBackgroundColor)
        .cornerRadius(theme.cornerRadius)
        .shadow(radius: theme.shadowRadius)
    }
    
    private var stateDescription: String {
        switch state {
        case is DownloadState.Idle: return "Idle"
        case is DownloadState.Queued: return "Queued"
        case is DownloadState.Connecting: return "Connecting"
        case is DownloadState.Downloading: return "Downloading"
        case is DownloadState.Paused: return "Paused"
        case is DownloadState.Done: return "Done"
        case is DownloadState.Failed: return "Failed"
        case is DownloadState.Cancelled: return "Cancelled"
        default: return "Unknown"
        }
    }
    
    private var canPause: Bool {
        state is DownloadState.Downloading || state is DownloadState.Connecting || state is DownloadState.Queued
    }
    
    private var canResume: Bool {
        state is DownloadState.Paused || state is DownloadState.Failed
    }
    
    private func extractProgress(from state: DownloadState) -> DownloadProgress? {
        if let downloading = state as? DownloadState.Downloading {
            return downloading.progress
        }
        return nil
    }
}
