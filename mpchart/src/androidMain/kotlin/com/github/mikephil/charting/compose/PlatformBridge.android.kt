package com.github.mikephil.charting.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap as toComposeImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.github.mikephil.charting.platform.AndroidBitmap
import com.github.mikephil.charting.platform.AndroidPlatform
import com.github.mikephil.charting.platform.Bitmap
import androidx.compose.ui.graphics.nativeCanvas
import com.github.mikephil.charting.platform.AndroidCanvas
import com.github.mikephil.charting.platform.Canvas

internal actual fun androidx.compose.ui.graphics.Canvas.toChartCanvas(width: Int, height: Int): Canvas =
    AndroidCanvas(nativeCanvas, width, height)

@Composable
internal actual fun rememberChartFontsReady(): Boolean = true

@Composable
internal actual fun BindPlatformContext() {
    val context = LocalContext.current.applicationContext
    SideEffect { AndroidPlatform.context = context }
}

public actual fun Bitmap.asImageBitmap(): ImageBitmap = (this as AndroidBitmap).native.toComposeImageBitmap()
