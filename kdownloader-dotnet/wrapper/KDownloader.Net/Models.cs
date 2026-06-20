using System.Text.Json.Serialization;

namespace KDownloader.Net
{
    public class DownloadRequest
    {
        [JsonPropertyName("id")]
        public string Id { get; set; } = string.Empty;

        [JsonPropertyName("url")]
        public string Url { get; set; } = string.Empty;

        [JsonPropertyName("destinationDir")]
        public string DestinationDir { get; set; } = string.Empty;

        [JsonPropertyName("fileName")]
        public string FileName { get; set; } = string.Empty;

        [JsonPropertyName("chunkCount")]
        public int ChunkCount { get; set; } = 8;
    }

    public class DownloadState
    {
        [JsonPropertyName("status")]
        public string Status { get; set; } = "IDLE";

        [JsonPropertyName("percent")]
        public int Percent { get; set; }

        [JsonPropertyName("downloadedBytes")]
        public long DownloadedBytes { get; set; }

        [JsonPropertyName("totalBytes")]
        public long TotalBytes { get; set; }

        [JsonPropertyName("speedFormatted")]
        public string SpeedFormatted { get; set; } = string.Empty;

        [JsonPropertyName("errorMessage")]
        public string? ErrorMessage { get; set; }
    }
}
