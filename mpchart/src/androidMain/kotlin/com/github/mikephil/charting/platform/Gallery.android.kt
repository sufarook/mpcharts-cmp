package com.github.mikephil.charting.platform

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException

/** Application context for the MediaStore, set by the chart composables. */
internal object AndroidPlatform {
    var context: Context? = null
}

internal actual object PlatformGallery {
    actual fun save(bytes: ByteArray, name: String, mimeType: String, subFolderPath: String, description: String): Boolean {
        val context = AndroidPlatform.context ?: return false
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.DESCRIPTION, description)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/$subFolderPath".trimEnd('/'))
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStorageDirectory(), "DCIM/$subFolderPath")
                if (!dir.exists() && !dir.mkdirs()) return false
                @Suppress("DEPRECATION")
                put(MediaStore.Images.Media.DATA, File(dir, name).absolutePath)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false

        return try {
            val stream = resolver.openOutputStream(uri) ?: throw IOException("Could not open $uri for writing")
            stream.use { it.write(bytes) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (e: IOException) {
            resolver.delete(uri, null, null)
            false
        }
    }
}
