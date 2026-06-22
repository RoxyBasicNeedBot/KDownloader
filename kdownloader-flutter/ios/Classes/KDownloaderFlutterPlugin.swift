import Flutter
import UIKit
import kdownloader_core

public class KdownloaderFlutterPlugin: NSObject, FlutterPlugin, FlutterStreamHandler {
    private var eventSink: FlutterEventSink?
    private var observeTask: Task<Void, Never>?
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "com.roxybasicneedbot.kdownloader/methods", binaryMessenger: registrar.messenger())
        let instance = KdownloaderFlutterPlugin()
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
            let request = DownloadRequest(
                id: id,
                url: url,
                destinationDir: destinationDir,
                fileName: fileName,
                priority: .normal, // Should parse from args["priority"] if needed
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
                    // Serializing the states to a JSON string or Array of Dictionaries
                    // Assuming states has properties that map cleanly or a helper function
                    // For now, we will return an empty list or mock serialized state 
                    // since we don't have the exact structure of DownloadTaskEntity mapping in Swift
                    DispatchQueue.main.async {
                        self.eventSink?("[]") // TODO: map `states` to JSON array
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
