import Flutter
import UIKit
import kdownloader_core

public class KDownloaderFlutterPlugin: NSObject, FlutterPlugin, FlutterStreamHandler, DownloadListener {
    private var eventSink: FlutterEventSink?
    
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
            guard let args = call.arguments as? [String: Any] else {
                result(FlutterError(code: "INVALID_ARGS", message: "Arguments must be a Map", details: nil))
                return
            }
            let id = args["id"] as! String
            let url = args["url"] as! String
            let destinationDir = args["destinationDir"] as! String
            let fileName = args["fileName"] as! String
            let priority = args["priority"] as? String ?? "NORMAL"
            let chunkCount = args["chunkCount"] as? Int32 ?? 4
            let headers = args["headers"] as? [String: String] ?? [:]
            let wifiOnly = args["wifiOnly"] as? Bool ?? false
            let speedLimit = args["speedLimit"] as? Int64 ?? 0
            let mirrorUrls = args["mirrorUrls"] as? [String] ?? []
            let hashAlgorithm = args["hashAlgorithm"] as? String
            let expectedHash = args["expectedHash"] as? String
            let scheduleAt = args["scheduleAt"] as? Int64
            let groupTag = args["groupTag"] as? String
            
            let taskId = downloader.enqueue(
                id: id,
                url: url,
                destinationDir: destinationDir,
                fileName: fileName,
                priority: priority,
                chunkCount: chunkCount,
                headers: headers,
                wifiOnly: wifiOnly,
                speedLimit: speedLimit,
                mirrorUrls: mirrorUrls,
                hashAlgorithm: hashAlgorithm,
                expectedHash: expectedHash,
                scheduleAt: scheduleAt != nil ? KotlinLong(value: scheduleAt!) : nil,
                groupTag: groupTag
            )
            result(taskId)
            
        case "pause":
            guard let args = call.arguments as? [String: Any],
                  let id = args["id"] as? String else {
                result(FlutterError(code: "INVALID_ARGS", message: "Missing id", details: nil))
                return
            }
            downloader.pause(id: id)
            result(nil)
            
        case "resume":
            guard let args = call.arguments as? [String: Any],
                  let id = args["id"] as? String else {
                result(FlutterError(code: "INVALID_ARGS", message: "Missing id", details: nil))
                return
            }
            downloader.resume(id: id)
            result(nil)
            
        case "cancel":
            guard let args = call.arguments as? [String: Any],
                  let id = args["id"] as? String else {
                result(FlutterError(code: "INVALID_ARGS", message: "Missing id", details: nil))
                return
            }
            downloader.cancel(id: id)
            result(nil)
            
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    // MARK: - FlutterStreamHandler
    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        self.eventSink = events
        KDownloader.companion.instance.registerListener(listener: self)
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        KDownloader.companion.instance.unregisterListener(listener: self)
        self.eventSink = nil
        return nil
    }
    
    // MARK: - DownloadListener
    public func onTasksUpdated(tasks: [Any]) {
        DispatchQueue.main.async {
            self.eventSink?(tasks)
        }
    }
}
