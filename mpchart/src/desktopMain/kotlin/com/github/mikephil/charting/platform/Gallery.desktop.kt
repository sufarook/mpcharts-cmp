package com.github.mikephil.charting.platform

import java.io.File
import java.io.IOException

/** Desktop has no gallery: images go to the user's Pictures folder. */
internal actual object PlatformGallery {
    actual fun save(bytes: ByteArray, name: String, mimeType: String, subFolderPath: String, description: String): Boolean {
        if (bytes.isEmpty()) return false
        val pictures = File(java.lang.System.getProperty("user.home"), "Pictures")
        val dir = if (subFolderPath.isEmpty()) pictures else File(pictures, subFolderPath)
        if (!dir.exists() && !dir.mkdirs()) return false
        return try {
            File(dir, name).writeBytes(bytes)
            true
        } catch (e: IOException) {
            false
        }
    }
}
