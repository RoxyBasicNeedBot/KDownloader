using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace KDownloader.Net
{
    public class KDownloaderClient
    {
        public string Enqueue(DownloadRequest request)
        {
            var json = JsonSerializer.Serialize(request);
            var ptr = NativeBindings.kdownloader_enqueue(json);
            if (ptr == IntPtr.Zero) return string.Empty;
            
            // Kotlin/Native uses UTF-8 natively
            var id = Marshal.PtrToStringUTF8(ptr) ?? string.Empty;
            NativeBindings.kdownloader_free_string(ptr);
            return id;
        }

        public void Pause(string taskId) => NativeBindings.kdownloader_pause(taskId);

        public void Resume(string taskId) => NativeBindings.kdownloader_resume(taskId);

        public void Cancel(string taskId) => NativeBindings.kdownloader_cancel(taskId);

        public async IAsyncEnumerable<DownloadState> ObserveStateAsync(
            string taskId, 
            [System.Runtime.CompilerServices.EnumeratorCancellation] CancellationToken cancellationToken = default)
        {
            string lastStateJson = string.Empty;
            
            while (!cancellationToken.IsCancellationRequested)
            {
                var ptr = NativeBindings.kdownloader_get_state(taskId);
                if (ptr != IntPtr.Zero)
                {
                    var json = Marshal.PtrToStringUTF8(ptr);
                    NativeBindings.kdownloader_free_string(ptr);
                    
                    if (json != null && json != lastStateJson)
                    {
                        lastStateJson = json;
                        var state = JsonSerializer.Deserialize<DownloadState>(json);
                        if (state != null)
                        {
                            yield return state;
                            
                            if (state.Status == "DONE" || state.Status == "FAILED" || state.Status == "CANCELLED")
                            {
                                break;
                            }
                        }
                    }
                }
                
                // Poll at 100ms intervals
                await Task.Delay(100, cancellationToken);
            }
        }
    }
}
