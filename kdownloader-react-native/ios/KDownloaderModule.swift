import Foundation
import React
import kdownloader_core

@objc(KDownloader)
class KDownloaderModule: RCTEventEmitter {
    
    private let downloader = KDownloader.companion.instance
    private var observeTask: Task<Void, Never>?
    
    override static func requiresMainQueueSetup() -> Bool {
        return true
    }
    
    override func supportedEvents() -> [String]! {
        return ["onDownloadStateChange"]
    }
    
    override func startObserving() {
        observeTask?.cancel()
        observeTask = Task {
            do {
                // Skie bridging Flow to Swift AsyncSequence
                for try await states in downloader.observeAll() {
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
                        self.sendEvent(withName: "onDownloadStateChange", body: array)
                    }
                }
            } catch {
                print("Observe error: \(error)")
            }
        }
    }
    
    override func stopObserving() {
        observeTask?.cancel()
    }
    
    @objc
    func enqueue(_ requestMap: NSDictionary,
                 resolver resolve: @escaping RCTPromiseResolveBlock,
                 rejecter reject: @escaping RCTPromiseRejectBlock) {
        
        guard let id = requestMap["id"] as? String,
              let url = requestMap["url"] as? String,
              let destinationDir = requestMap["destinationDir"] as? String,
              let fileName = requestMap["fileName"] as? String else {
            reject("INVALID_ARGS", "Missing required arguments", nil)
            return
        }
        
        let priorityStr = requestMap["priority"] as? String ?? "NORMAL"
        let priority: DownloadPriority = priorityStr == "HIGH" ? .high : (priorityStr == "LOW" ? .low : .normal)
        
        let request = DownloadRequest(
            id: id,
            url: url,
            destinationDir: destinationDir,
            fileName: fileName,
            priority: priority,
            chunkCount: Int32(requestMap["chunkCount"] as? Int ?? 4),
            headers: requestMap["headers"] as? [String: String] ?? [:],
            wifiOnly: requestMap["wifiOnly"] as? Bool ?? false,
            speedLimit: requestMap["speedLimit"] as? Int64 ?? 0,
            mirrorUrls: requestMap["mirrorUrls"] as? [String] ?? [],
            hashAlgorithm: requestMap["hashAlgorithm"] as? String,
            expectedHash: requestMap["expectedHash"] as? String,
            scheduleAt: (requestMap["scheduleAt"] as? Int64).map { KotlinLong(value: $0) },
            groupTag: requestMap["groupTag"] as? String
        )
        
        Task {
            do {
                let taskId = try await downloader.enqueue(request: request)
                DispatchQueue.main.async { resolve(taskId) }
            } catch {
                DispatchQueue.main.async { reject("ENQUEUE_ERROR", error.localizedDescription, error) }
            }
        }
    }
    
    @objc
    func pause(_ id: String,
               resolver resolve: @escaping RCTPromiseResolveBlock,
               rejecter reject: @escaping RCTPromiseRejectBlock) {
        Task {
            do {
                try await downloader.pause(id: id)
                DispatchQueue.main.async { resolve(nil) }
            } catch {
                DispatchQueue.main.async { reject("PAUSE_ERROR", error.localizedDescription, error) }
            }
        }
    }
    
    @objc
    func resume(_ id: String,
                resolver resolve: @escaping RCTPromiseResolveBlock,
                rejecter reject: @escaping RCTPromiseRejectBlock) {
        Task {
            do {
                try await downloader.resume(id: id)
                DispatchQueue.main.async { resolve(nil) }
            } catch {
                DispatchQueue.main.async { reject("RESUME_ERROR", error.localizedDescription, error) }
            }
        }
    }
    
    @objc
    func cancel(_ id: String,
                resolver resolve: @escaping RCTPromiseResolveBlock,
                rejecter reject: @escaping RCTPromiseRejectBlock) {
        Task {
            do {
                try await downloader.cancel(id: id)
                DispatchQueue.main.async { resolve(nil) }
            } catch {
                DispatchQueue.main.async { reject("CANCEL_ERROR", error.localizedDescription, error) }
            }
        }
    }
}
