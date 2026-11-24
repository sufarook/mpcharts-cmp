package com.github.mikephil.charting.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.github.mikephil.charting.platform.Bitmap
import com.github.mikephil.charting.platform.Canvas

/** Wraps a Compose canvas into the chart [Canvas]: android.graphics on Android, Skia on desktop and iOS. */
internal expect fun androidx.compose.ui.graphics.Canvas.toChartCanvas(width: Int, height: Int): Canvas

/** True once the fonts the charts draw with are available. Android uses its system Roboto right away. */
@Composable
internal expect fun rememberChartFontsReady(): Boolean

/** Hands the platform what `Chart.saveToGallery` needs: the application context on Android, nothing elsewhere. */
@Composable
internal expect fun BindPlatformContext()

/** The chart [Bitmap], for example from `chart.toBitmap()`, as a Compose [ImageBitmap] to show in an `Image`. */
public expect fun Bitmap.asImageBitmap(): ImageBitmap
