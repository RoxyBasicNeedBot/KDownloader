import 'dart:async';
import 'kdownloader_flutter_platform_interface.dart';

enum DownloadPriority {
  low,
  normal,
  high,
  critical,
}

class DownloadRequest {
  final String id;
  final String url;
  final String destinationDir;
  final String fileName;
  final DownloadPriority priority;
  final int chunkCount;
  final Map<String, String> headers;
  final bool wifiOnly;
  final int speedLimit;
  final List<String> mirrorUrls;
  final String? hashAlgorithm;
  final String? expectedHash;
  final int? scheduleAt;
  final String? groupTag;

  DownloadRequest({
    required this.id,
    required this.url,
    required this.destinationDir,
    required this.fileName,
    this.priority = DownloadPriority.normal,
    this.chunkCount = 4,
    this.headers = const {},
    this.wifiOnly = false,
    this.speedLimit = 0,
    this.mirrorUrls = const [],
    this.hashAlgorithm,
    this.expectedHash,
    this.scheduleAt,
    this.groupTag,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'url': url,
      'destinationDir': destinationDir,
      'fileName': fileName,
      'priority': priority.name.toUpperCase(),
      'chunkCount': chunkCount,
      'headers': headers,
      'wifiOnly': wifiOnly,
      'speedLimit': speedLimit,
      'mirrorUrls': mirrorUrls,
      'hashAlgorithm': hashAlgorithm,
      'expectedHash': expectedHash,
      'scheduleAt': scheduleAt,
      'groupTag': groupTag,
    };
  }
}

class DownloadProgress {
  final int downloadedBytes;
  final int totalBytes;
  final int percent;
  final int speedBytesPerSec;
  final String speedFormatted;
  final int etaSeconds;
  final String etaFormatted;

  DownloadProgress({
    required this.downloadedBytes,
    required this.totalBytes,
    required this.percent,
    required this.speedBytesPerSec,
    required this.speedFormatted,
    required this.etaSeconds,
    required this.etaFormatted,
  });

  factory DownloadProgress.fromMap(Map<dynamic, dynamic> map) {
    return DownloadProgress(
      downloadedBytes: map['downloadedBytes'] ?? 0,
      totalBytes: map['totalBytes'] ?? 0,
      percent: map['percent'] ?? 0,
      speedBytesPerSec: map['speedBytesPerSec'] ?? 0,
      speedFormatted: map['speedFormatted'] ?? '0 B/s',
      etaSeconds: map['etaSeconds'] ?? 0,
      etaFormatted: map['etaFormatted'] ?? '0s',
    );
  }
}

class DownloadState {
  final String id;
  final String status;
  final DownloadProgress? progress;
  final String? errorMessage;

  DownloadState({
    required this.id,
    required this.status,
    this.progress,
    this.errorMessage,
  });

  factory DownloadState.fromMap(Map<dynamic, dynamic> map) {
    final progressMap = map['progress'] as Map<dynamic, dynamic>?;
    return DownloadState(
      id: map['id'] ?? '',
      status: map['status'] ?? 'IDLE',
      progress: progressMap != null ? DownloadProgress.fromMap(progressMap) : null,
      errorMessage: map['errorMessage'],
    );
  }
}

class KDownloaderFlutter {
  static final KDownloaderFlutter _instance = KDownloaderFlutter._internal();
  factory KDownloaderFlutter() => _instance;
  KDownloaderFlutter._internal();

  /// Enqueues a new download request.
  Future<String> enqueue(DownloadRequest request) {
    return KDownloaderPlatform.instance.enqueue(request);
  }

  /// Pauses an active download.
  Future<void> pause(String id) {
    return KDownloaderPlatform.instance.pause(id);
  }

  /// Resumes a paused/failed download.
  Future<void> resume(String id) {
    return KDownloaderPlatform.instance.resume(id);
  }

  /// Cancels a download and deletes intermediate files.
  Future<void> cancel(String id) {
    return KDownloaderPlatform.instance.cancel(id);
  }

  /// Observes status updates for all tasks.
  Stream<List<DownloadState>> observeAll() {
    return KDownloaderPlatform.instance.observeAll();
  }
}
