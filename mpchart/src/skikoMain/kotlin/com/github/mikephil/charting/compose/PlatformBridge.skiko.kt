package com.github.mikephil.charting.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.github.mikephil.charting.platform.Bitmap
import com.github.mikephil.charting.platform.SkiaBitmap
import com.github.mikephil.charting.platform.Canvas
import com.github.mikephil.charting.platform.SkiaCanvas
import com.github.mikephil.charting.platform.SkiaFonts
import com.github.mikephil.charting.resources.Res

internal actual fun androidx.compose.ui.graphics.Canvas.toChartCanvas(width: Int, height: Int): Canvas =
    SkiaCanvas(skiaCanvas, width, height)

@Composable
internal actual fun rememberChartFontsReady(): Boolean {
    var ready by remember { mutableStateOf(SkiaFonts.isLoaded) }
    if (!ready) {
        LaunchedEffect(Unit) {
            SkiaFonts.loadRoboto(Res.readBytes("files/Roboto-Regular.ttf"))
            SkiaFonts.loadRobotoItalic(Res.readBytes("files/Roboto-Italic.ttf"))
            ready = true
        }
    }
    return ready
}

@Composable
internal actual fun BindPlatformContext() {}

public actual fun Bitmap.asImageBitmap(): ImageBitmap = (this as SkiaBitmap).surface.makeImageSnapshot().toComposeImageBitmap()
