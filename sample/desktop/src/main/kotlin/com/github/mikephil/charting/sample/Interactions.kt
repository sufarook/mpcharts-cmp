package com.github.mikephil.charting.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import java.io.File
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.github.mikephil.charting.compose.BarChart
import com.github.mikephil.charting.compose.ChartState
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import org.jetbrains.skia.EncodedImageFormat

/**
 * Drives the sample with desktop mouse input, headless, and writes a PNG after each step: the desktop input path
 * from Compose pointer events to the chart's touch listener. Argument: output folder.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val out = File(args.getOrElse(0) { "interactions" }).apply { mkdirs() }
    val start = System.nanoTime()
    fun now() = (System.nanoTime() - start) / 1_000_000

    fun ImageComposeScene.frames(millis: Long) {
        val end = now() + millis
        while (now() < end) {
            render(now() * 1_000_000)
            Thread.sleep(16)
        }
    }

    fun ImageComposeScene.save(name: String) {
        val image = render(now() * 1_000_000)
        File(out, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        println("wrote $name.png")
    }

    fun ImageComposeScene.mouse(type: PointerEventType, at: Offset, pressed: Boolean) {
        sendPointerEvent(
            eventType = type,
            position = at,
            timeMillis = now(),
            type = PointerType.Mouse,
            buttons = PointerButtons(isPrimaryPressed = pressed),
            button = if (type == PointerEventType.Move) null else PointerButton.Primary,
        )
    }

    fun ImageComposeScene.click(at: Offset) {
        mouse(PointerEventType.Press, at, true)
        frames(30)
        mouse(PointerEventType.Release, at, false)
    }

    fun ImageComposeScene.drag(from: Offset, to: Offset, steps: Int = 20) {
        mouse(PointerEventType.Press, from, true)
        frames(16)
        for (i in 1..steps) {
            mouse(PointerEventType.Move, from + (to - from) * (i / steps.toFloat()), true)
            frames(16)
        }
        mouse(PointerEventType.Release, to, false)
    }

    ImageComposeScene(1080, 2000, Density(2.625f)) { App(startScreen = "hbar") }.use { scene ->
        scene.frames(3200) // font load and intro animation
        scene.save("hbar-0-start")
        scene.click(Offset(400f, 850f)) // the 48.9 bar
        scene.frames(500) // past the double tap timeout
        scene.save("hbar-1-tapped")
        scene.click(Offset(500f, 1200f))
        scene.frames(80)
        scene.click(Offset(500f, 1200f))
        scene.frames(600)
        scene.save("hbar-2-double-click-zoom")
        scene.drag(Offset(500f, 700f), Offset(500f, 1300f)) // downward: reveals the higher categories
        scene.frames(1500) // drag deceleration
        scene.save("hbar-3-dragged")
    }

    ImageComposeScene(1080, 2000, Density(2.625f)) { App(startScreen = "combined") }.use { scene ->
        scene.frames(2000)
        scene.save("mix-0-start")
        scene.click(Offset(440f, 1500f)) // upper (Stack 2) segment of the stacked bar left of Jul
        scene.frames(500)
        scene.save("mix-1-stack-tapped")
        scene.click(Offset(422f, 318f)) // the 189 bubble
        scene.frames(500)
        scene.save("mix-2-bubble-tapped")
        scene.click(Offset(540f, 1000f))
        scene.frames(80)
        scene.click(Offset(540f, 1000f))
        scene.frames(600)
        scene.save("mix-3-double-click-zoom")
        scene.drag(Offset(800f, 1000f), Offset(300f, 1000f))
        scene.frames(1500)
        scene.save("mix-4-dragged")
    }

    ImageComposeScene(1080, 2000, Density(2.625f)) { App(startScreen = "pie") }.use { scene ->
        scene.frames(2000)
        scene.save("pie-0-start")
        scene.drag(Offset(800f, 700f), Offset(800f, 1400f))
        scene.frames(2000)
        scene.save("pie-1-rotated")
        scene.click(Offset(300f, 1100f))
        scene.frames(500)
        scene.save("pie-2-tapped")
    }

    ImageComposeScene(1080, 2000, Density(2.625f)) { App(startScreen = "radar") }.use { scene ->
        scene.frames(2000)
        scene.save("radar-0-start")
        scene.click(Offset(905f, 990f)) // near the Steak spoke
        scene.frames(500)
        scene.save("radar-1-tapped")
        scene.drag(Offset(750f, 850f), Offset(750f, 1350f)) // inside the web: rotation only starts on the chart
        scene.frames(2000)
        scene.save("radar-2-rotated")
    }

    ImageComposeScene(1080, 2000, Density(2.625f)) { App(startScreen = "bar") }.use { scene ->
        scene.frames(2000)
        scene.click(Offset(540f, 1500f)) // a bar in the middle
        scene.frames(500)
        scene.save("bar-marker")
    }

    ImageComposeScene(1080, 2000, Density(2.625f)) { App(startScreen = "line") }.use { scene ->
        scene.frames(2500)
        scene.click(Offset(540f, 1000f))
        scene.frames(500)
        scene.save("line-marker")
    }

    // ChartState: a tap selects, a double click zooms, both reported back as Compose state
    val state = ChartState()
    val data = BarData(BarDataSet((0 until 10).map { BarEntry(it.toFloat(), (it + 1) * 10f) }, "state"))
    ImageComposeScene(1080, 1000, Density(2.625f)) { BarChart(data, Modifier.fillMaxSize(), state = state) }.use { scene ->
        scene.frames(500)
        println("state before: selected=${state.selectedEntry}, zoomX=${state.zoomX}, attached=${state.isAttached}")
        scene.click(Offset(1000f, 900f)) // the last, tallest bar
        scene.frames(500)
        println("state after tap: selected=${state.selectedEntry}")
        scene.click(Offset(540f, 500f))
        scene.frames(80)
        scene.click(Offset(540f, 500f))
        scene.frames(600)
        println("state after double click: zoomX=${state.zoomX}, visible x ${state.lowestVisibleX}..${state.highestVisibleX}")
    }
    println("state after dispose: attached=${state.isAttached}")
}
