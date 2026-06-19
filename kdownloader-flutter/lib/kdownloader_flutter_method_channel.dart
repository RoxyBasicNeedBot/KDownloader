import 'package:flutter/services.dart';
import 'kdownloader_flutter.dart';
import 'kdownloader_flutter_platform_interface.dart';

class MethodChannelKDownloader extends KDownloaderPlatform {
  final _methodChannel = const MethodChannel('com.roxybasicneedbot.kdownloader/methods');
  final _eventChannel = const EventChannel('com.roxybasicneedbot.kdownloader/events');

  @override
  Future<String> enqueue(DownloadRequest request) async {
    final result = await _methodChannel.invokeMethod<String>('enqueue', request.toMap());
    return result ?? request.id;
  }

  @override
  Future<void> pause(String id) async {
    await _methodChannel.invokeMethod<void>('pause', {'id': id});
  }

  @override
  Future<void> resume(String id) async {
    await _methodChannel.invokeMethod<void>('resume', {'id': id});
  }

  @override
  Future<void> cancel(String id) async {
    await _methodChannel.invokeMethod<void>('cancel', {'id': id});
  }

  @override
  Stream<List<DownloadState>> observeAll() {
    return _eventChannel.receiveBroadcastStream().map((event) {
      if (event is List) {
        return event.map((e) => DownloadState.fromMap(e as Map)).toList();
      }
      return const [];
    });
  }
}
