package com.github.mikephil.charting.sample

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import java.io.File
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the sample screens offscreen and writes PNGs, for comparing against Android screenshots.
 * Arguments: output folder, width px, height px, density.
 */
fun main(args: Array<String>) {
    val out = File(args.getOrElse(0) { "screenshots" }).apply { mkdirs() }
    val width = args.getOrElse(1) { "1080" }.toInt()
    val height = args.getOrElse(2) { "2000" }.toInt()
    val density = args.getOrElse(3) { "2.625" }.toFloat()
    for (name in listOf("bar", "hbar", "pie", "line", "combined", "radar")) {
        ImageComposeScene(width, height, Density(density)) { App(startScreen = name) }.use { scene ->
            val start = System.nanoTime()
            // keep rendering until the font is loaded and the 2.5 s intro animation is over
            while (System.nanoTime() - start < 3_200_000_000L) {
                scene.render(System.nanoTime() - start)
                Thread.sleep(16)
            }
            val image = scene.render(System.nanoTime() - start)
            val data = image.encodeToData(EncodedImageFormat.PNG) ?: error("encode failed")
            File(out, "$name.png").writeBytes(data.bytes)
            println("wrote ${File(out, "$name.png").absolutePath}")
        }
    }
}
