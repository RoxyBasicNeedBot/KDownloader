package com.roxybasicneedbot.kdownloader.core.util

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun epochMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
