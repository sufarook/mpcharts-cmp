package com.github.mikephil.charting.sample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MPChart Compose Multiplatform",
        state = rememberWindowState(width = 420.dp, height = 760.dp),
    ) {
        App()
    }
}
