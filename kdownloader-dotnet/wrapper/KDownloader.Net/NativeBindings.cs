using System;
using System.Runtime.InteropServices;

namespace KDownloader.Net
{
    internal static class NativeBindings
    {
        private const string LibName = "kdownloader";

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl, CharSet = CharSet.Ansi)]
        public static extern IntPtr kdownloader_enqueue(string requestJson);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl, CharSet = CharSet.Ansi)]
        public static extern void kdownloader_pause(string taskId);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl, CharSet = CharSet.Ansi)]
        public static extern void kdownloader_resume(string taskId);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl, CharSet = CharSet.Ansi)]
        public static extern void kdownloader_cancel(string taskId);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl, CharSet = CharSet.Ansi)]
        public static extern IntPtr kdownloader_get_state(string taskId);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl, CharSet = CharSet.Ansi)]
        public static extern void kdownloader_free_string(IntPtr ptr);
    }
}
