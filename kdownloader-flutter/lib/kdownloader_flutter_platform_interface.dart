import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'kdownloader_flutter.dart';
import 'kdownloader_flutter_method_channel.dart';

abstract class KDownloaderPlatform extends PlatformInterface {
  KDownloaderPlatform() : super(token: _token);

  static final Object _token = Object();

  static KDownloaderPlatform _instance = MethodChannelKDownloader();

  static KDownloaderPlatform get instance => _instance;

  static set instance(KDownloaderPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String> enqueue(DownloadRequest request) {
    throw UnimplementedError('enqueue() has not been implemented.');
  }

  Future<void> pause(String id) {
    throw UnimplementedError('pause() has not been implemented.');
  }

  Future<void> resume(String id) {
    throw UnimplementedError('resume() has not been implemented.');
  }

  Future<void> cancel(String id) {
    throw UnimplementedError('cancel() has not been implemented.');
  }

  Stream<List<DownloadState>> observeAll() {
    throw UnimplementedError('observeAll() has not been implemented.');
  }
}
