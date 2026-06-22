import Flutter
import UIKit
import kdownloader_core

public class KDownloaderFlutterPlugin: NSObject, FlutterPlugin, FlutterStreamHandler {
    private var eventSink: FlutterEventSink?
    private var observeTask: Task<Void, Never>?
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "com.roxybasicneedbot.kdownloader/methods", binaryMessenger: registrar.messenger())
        let instance = KDownloaderFlutterPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
        
        let eventChannel = FlutterEventChannel(name: "com.roxybasicneedbot.kdownloader/events", binaryMessenger: registrar.messenger())
        eventChannel.setStreamHandler(instance)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        let downloader = KDownloader.companion.instance
        
        switch call.method {
        case "enqueue":
            guard let args = call.arguments as? [String: Any],
                  let id = args["id"] as? String,
                  let url = args["url"] as? String,
                  let destinationDir = args["destinationDir"] as? String,
                  let fileName = args["fileName"] as? String else {
                result(FlutterError(code: "INVALID_ARGS", message: "Missing required arguments", details: nil))
                return
            }
            
            // Build the priority enum if exposed to iOS
            // Assuming KDownloader_core exposes the Builder or directly DownloadRequest
            let priorityStr = args["priority"] as? String ?? "NORMAL"
            let priority: DownloadPriority = priorityStr == "HIGH" ? .high : (priorityStr == "LOW" ? .low : .normal)
            let request = DownloadRequest(
                id: id,
                url: url,
                destinationDir: destinationDir,
                fileName: fileName,
                priority: priority,
                chunkCount: Int32(args["chunkCount"] as? Int ?? 4),
                headers: args["headers"] as? [String: String] ?? [:],
                wifiOnly: args["wifiOnly"] as? Bool ?? false,
                speedLimit: args["speedLimit"] as? Int64 ?? 0,
                mirrorUrls: args["mirrorUrls"] as? [String] ?? [],
                hashAlgorithm: args["hashAlgorithm"] as? String,
                expectedHash: args["expectedHash"] as? String,
                scheduleAt: (args["scheduleAt"] as? Int64).map { KotlinLong(value: $0) },
                groupTag: args["groupTag"] as? String
            )
            
            Task {
                do {
                    let taskId = try await downloader.enqueue(request: request)
                    DispatchQueue.main.async { result(taskId) }
                } catch {
                    DispatchQueue.main.async { result(FlutterError(code: "ENQUEUE_ERROR", message: error.localizedDescription, details: nil)) }
                }
            }
            
        case "pause":
            guard let args = call.arguments as? [String: Any],
                  let id = args["id"] as? String else {
                result(FlutterError(code: "INVALID_ARGS", message: "Missing id", details: nil))
                return
            }
            Task {
                do {
                    try await downloader.pause(id: id)
                    DispatchQueue.main.async { result(nil) }
                } catch {
                    DispatchQueue.main.async { result(FlutterError(code: "PAUSE_ERROR", message: error.localizedDescription, details: nil)) }
                }
            }
            
        case "resume":
            guard let args = call.arguments as? [String: Any],
                  let id = args["id"] as? String else {
                result(FlutterError(code: "INVALID_ARGS", message: "Missing id", details: nil))
                return
            }
            Task {
                do {
                    try await downloader.resume(id: id)
                    DispatchQueue.main.async { result(nil) }
                } catch {
                    DispatchQueue.main.async { result(FlutterError(code: "RESUME_ERROR", message: error.localizedDescription, details: nil)) }
                }
            }
            
        case "cancel":
            guard let args = call.arguments as? [String: Any],
                  let id = args["id"] as? String else {
                result(FlutterError(code: "INVALID_ARGS", message: "Missing id", details: nil))
                return
            }
            Task {
                do {
                    try await downloader.cancel(id: id)
                    DispatchQueue.main.async { result(nil) }
                } catch {
                    DispatchQueue.main.async { result(FlutterError(code: "CANCEL_ERROR", message: error.localizedDescription, details: nil)) }
                }
            }
            
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    // MARK: - FlutterStreamHandler
    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        self.eventSink = events
        
        observeTask?.cancel()
        observeTask = Task {
            do {
                // Skie bridging Flow<List<DownloadTaskEntity>> to Swift AsyncSequence
                for try await states in KDownloader.companion.instance.observeAll() {
                    let array: [[String: Any]] = states.map { task in
                        return [
                            "id": task.id,
                            "url": task.url,
                            "destinationDir": task.destinationDir,
                            "fileName": task.fileName,
                            "status": task.status,
                            "downloadedBytes": task.downloadedBytes,
                            "totalBytes": task.totalBytes,
                            "errorMessage": task.errorMessage ?? ""
                        ]
                    }
                    DispatchQueue.main.async {
                        self.eventSink?(array)
                    }
                }
            } catch {
                DispatchQueue.main.async {
                    self.eventSink?(FlutterError(code: "OBSERVE_ERROR", message: error.localizedDescription, details: nil))
                }
            }
        }
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        observeTask?.cancel()
        self.eventSink = nil
        return nil
    }
}
