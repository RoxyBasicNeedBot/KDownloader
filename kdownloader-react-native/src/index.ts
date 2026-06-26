import { NativeModules, NativeEventEmitter } from 'react-native';

const { KDownloader } = NativeModules;
const KDownloaderEmitter = new NativeEventEmitter(KDownloader);

export interface DownloadRequest {
  id: string;
  url: string;
  destinationDir: string;
  fileName: string;
  priority?: 'LOW' | 'NORMAL' | 'HIGH' | 'CRITICAL';
  chunkCount?: number;
  headers?: Record<string, string>;
  wifiOnly?: boolean;
}

export interface DownloadState {
  id: string;
  status: 'IDLE' | 'DOWNLOADING' | 'PAUSED' | 'DONE' | 'FAILED' | 'CANCELLED';
  progress?: {
    percent: number;
    downloadedBytes: number;
    totalBytes: number;
    speedFormatted: string;
    etaFormatted: string;
  };
}

export default {
  enqueue: (request: DownloadRequest): Promise<string> => KDownloader.enqueue(request),
  pause: (id: string): Promise<void> => KDownloader.pause(id),
  resume: (id: string): Promise<void> => KDownloader.resume(id),
  cancel: (id: string): Promise<void> => KDownloader.cancel(id),
  
  observe: (callback: (states: DownloadState[]) => void) => {
    return KDownloaderEmitter.addListener('onDownloadStateChange', callback);
  }
};
