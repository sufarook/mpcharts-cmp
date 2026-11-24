package com.github.mikephil.charting.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

/** Saves into the photo library; the app's Info.plist needs NSPhotoLibraryAddUsageDescription. */
internal actual object PlatformGallery {
    @OptIn(ExperimentalForeignApi::class)
    actual fun save(bytes: ByteArray, name: String, mimeType: String, subFolderPath: String, description: String): Boolean {
        if (bytes.isEmpty()) return false
        val data = bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()) }
        val image = UIImage.imageWithData(data) ?: return false
        UIImageWriteToSavedPhotosAlbum(image, null, null, null)
        return true
    }
}
