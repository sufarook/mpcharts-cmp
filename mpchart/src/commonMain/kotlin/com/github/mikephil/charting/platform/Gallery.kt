package com.github.mikephil.charting.platform

/** Where `Chart.saveToGallery` puts images. */
internal expect object PlatformGallery {
    /** Stores encoded image [bytes] as [name]; false when that failed. */
    fun save(bytes: ByteArray, name: String, mimeType: String, subFolderPath: String, description: String): Boolean
}
