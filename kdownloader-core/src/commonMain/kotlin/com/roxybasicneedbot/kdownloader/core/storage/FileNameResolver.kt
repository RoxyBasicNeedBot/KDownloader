package com.roxybasicneedbot.kdownloader.core.storage

object FileNameResolver {
    fun resolve(url: String, contentDisposition: String?): String {
        if (!contentDisposition.isNullOrBlank()) {
            val filenameRegex = """filename\*?=\s*(?:([^;']*)'[^']*')?\s*"?([^";]+)"?""".toRegex(RegexOption.IGNORE_CASE)
            val match = filenameRegex.find(contentDisposition)
            if (match != null) {
                val utf8Filename = match.groupValues.getOrNull(2)
                if (!utf8Filename.isNullOrBlank()) {
                    return utf8Filename.replace("%20", " ")
                }
            }
        }

        val uriPath = url.substringBefore("?").substringBefore("#")
        val filename = uriPath.substringAfterLast("/")
        if (filename.isNotBlank()) {
            return filename
        }

        return "download_file"
    }
}
