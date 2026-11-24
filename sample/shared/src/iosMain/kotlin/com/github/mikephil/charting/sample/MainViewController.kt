package com.github.mikephil.charting.sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Entry point for the iOS app: `MainViewControllerKt.MainViewController()` from Swift. */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
