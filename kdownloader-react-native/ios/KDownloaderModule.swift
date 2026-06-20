import React
import kdownloader_core

@objc(KDownloader)
class KDownloaderModule: RCTEventEmitter, DownloadListener {
    
    override func supportedEvents() -> [String]! {
        return ["onDownloadStateChange"]
    }
    
    @objc func enqueue(_ requestMap: [String: Any], resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        let downloader = KDownloader.companion.instance
        let taskId = downloader.enqueue(
            id: requestMap["id"] as! String,
            url: requestMap["url"] as! String,
            destinationDir: requestMap["destinationDir"] as! String,
            fileName: requestMap["fileName"] as! String,
            priority: requestMap["priority"] as? String ?? "NORMAL",
            chunkCount: requestMap["chunkCount"] as? Int32 ?? 8,
            headers: [:],
            wifiOnly: requestMap["wifiOnly"] as? Bool ?? false,
            speedLimit: 0,
            mirrorUrls: [],
            hashAlgorithm: nil,
            expectedHash: nil,
            scheduleAt: nil,
            groupTag: nil
        )
        resolve(taskId)
    }
    
    @objc func pause(_ taskId: String, resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
        KDownloader.companion.instance.pause(id: taskId)
        resolve(nil)
    }
    
    @objc func resume(_ taskId: String, resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
        KDownloader.companion.instance.resume(id: taskId)
        resolve(nil)
    }
    
    @objc func cancel(_ taskId: String, resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
        KDownloader.companion.instance.cancel(id: taskId)
        resolve(nil)
    }
    
    override func startObserving() {
        KDownloader.companion.instance.registerListener(listener: self)
    }
    
    override func stopObserving() {
        KDownloader.companion.instance.unregisterListener(listener: self)
    }
    
    func onTasksUpdated(tasks: [Any]) {
        // Map tasks into React Native events
        DispatchQueue.main.async {
            self.sendEvent(withName: "onDownloadStateChange", body: [])
        }
    }
    
    @objc override static func requiresMainQueueSetup() -> Bool {
        return true
    }
}
