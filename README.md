# mpcharts-cmp

[MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) v4.0.1 as a Compose Multiplatform library for
Android, iOS and desktop. Ported so far: **BarChart**, **HorizontalBarChart**, **PieChart**, **LineChart**, **CombinedChart** (line, bar,
scatter, candle and bubble data in one plot) and **RadarChart**.

The charts are not a lookalike: the chart, renderer, axis, legend, highlight, animation and touch code is
MPAndroidChart's own Kotlin source, moved to `commonMain`. Only the Android platform calls underneath it
(`android.graphics`, `View`, `MotionEvent`, `ValueAnimator`, ...) are replaced by a small layer in
`com.github.mikephil.charting.platform` with the same names and behavior:

| Platform | Draws with |
|---|---|
| Android | the real `android.graphics.Canvas` / `Paint` / `Path`, so it draws exactly what MPAndroidChart draws |
| Desktop, iOS | Skia (through Skiko), the engine `android.graphics` itself runs on |

To make desktop and iOS match Android:
- Text uses **Roboto** (and Roboto Italic for italic text), bundled from Android's own font files and Google Fonts, shaped with HarfBuzz, with line metrics read from
  the font tables the way Android's FreeType backend reads them.
- Taps, double taps, long presses, drags and flings go through a port of Android's `GestureDetector` and
  `VelocityTracker`, with Android's timeouts and slops, on every platform.

## Usage

```kotlin
import com.github.mikephil.charting.compose.BarChart
import com.github.mikephil.charting.compose.CombinedChart
import com.github.mikephil.charting.compose.HorizontalBarChart
import com.github.mikephil.charting.compose.PieChart
import com.github.mikephil.charting.compose.LineChart
import com.github.mikephil.charting.compose.RadarChart

BarChart(
    data = barData,                       // a new instance is applied, the same instance is left alone
    modifier = Modifier.fillMaxSize(),
    animateChanges = true,                // animate from the old values to a new data instance
    setup = {                             // runs once, `this` is MPAndroidChart's BarChart
        description.isEnabled = false
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        axisRight.isEnabled = false
        onValueSelected { entry, highlight -> println(entry) }
    },
    update = { /* runs on every recomposition */ },
)

PieChart(data = pieData, modifier = Modifier.size(300.dp), setup = { holeRadius = 58f })
LineChart(data = lineData, modifier = Modifier.fillMaxSize(), setup = { animateX(1500) })
RadarChart(data = radarData, modifier = Modifier.fillMaxSize(), setup = { webAlpha = 100 })
```

The composables have the same parameters as MPAndroidChart's own Compose module (`MPChartCompose`):
`data, modifier, state, contentDescription, marker, animateChanges, setup, update`.

```kotlin
val state = rememberChartState()            // selection, zoom, scroll and rotation as Compose state
LineChart(
    data = lineData,
    state = state,
    marker = { entry, highlight ->          // composable popup drawn at the tapped entry
        Text("${entry.y}", Modifier.background(Color.DarkGray).padding(4.dp))
    },
)
Text("Selected: ${state.selectedEntry?.y}")
Button(onClick = { state.zoomIn() }) { Text("Zoom in") }
```

Pie center text takes a `SpannableString` with `RelativeSizeSpan`, `AbsoluteSizeSpan`, `ForegroundColorSpan`,
`StyleSpan` and `TypefaceSpan`, as on Android (`com.github.mikephil.charting.platform`).

`chart.toBitmap()` draws the chart into an image, `bitmap.compress(Bitmap.CompressFormat.PNG, 100)` encodes it and
`bitmap.asImageBitmap()` shows it in Compose. `chart.saveToGallery("chart")` saves to the gallery: DCIM through the
MediaStore on Android, the photo library on iOS, the Pictures folder on desktop.

Inside `setup` and `update` the whole MPAndroidChart API is available, including `animateY(1400, Easing.EaseInOutQuad)`.
Data classes (`BarEntry`, `BarDataSet`, `PieEntry`, `PieDataSet`, ...) and formatters keep their names and
packages. Colors are ARGB ints, as in MPAndroidChart: `com.github.mikephil.charting.platform.Color`.

## Modules

- `mpchart`: the library (Android, desktop JVM, iosArm64, iosSimulatorArm64)
  - `commonMain/.../charting/*`: MPAndroidChart code (bar, horizontal bar, pie, line, combined and radar paths, including scatter, candle and bubble data and renderers)
  - `commonMain/.../charting/platform`: the stand-ins for the Android classes
  - `androidMain`: Android backend, `skikoMain`: Skia backend shared by desktop and iOS
  - `commonMain/.../charting/compose`: the `BarChart`, `HorizontalBarChart`, `PieChart`, `LineChart`, `CombinedChart` and `RadarChart` composables
- `sample/shared`: the sample UI, MPAndroidChart's bar, horizontal bar, pie, line, combined and radar example screens
- `sample/android`, `sample/desktop`: launchers. iOS: `MainViewController()` in `sample/shared/src/iosMain`

## Running

```powershell
./gradlew :sample:desktop:run
```

```powershell
./gradlew :sample:android:installDebug
```

```powershell
./gradlew :sample:desktop:screenshots -PshotWidth=2000 -PshotHeight=1086
```

The last command renders the sample screens to `sample/desktop/build/screenshots` without a window.

```powershell
./gradlew :sample:desktop:interactions
```

This drives the sample with mouse input without a window (click to highlight, double-click to zoom, drag to pan,
drag to rotate the pie and the radar web) and writes a PNG per step to `sample/desktop/build/interactions`.

iOS builds need a Mac: create an Xcode app that links the `SampleShared` framework
(`./gradlew :sample:shared:linkDebugFrameworkIosSimulatorArm64`) and shows `MainViewControllerKt.MainViewController()`.

## Verified so far

- Library and sample compile for Android and desktop, and the common code compiles as multiplatform metadata.
- Android (emulator) vs desktop, same data and chart size: **97.3% (bar), 97.9% (pie) and 92.4% (line) of chart
  pixels are identical**. Shapes, positions, labels, limit lines, fills and legend match; the rest is antialiasing
  at edges and glyph rasterization (GPU vs CPU rasterizer). The line chart scores lower because it is mostly thin
  diagonal dashed lines, which are all edge.
- HorizontalBarChart: renders on desktop like the example app; not yet compared against Android pixel by pixel
  (the emulator kept crashing).
- CombinedChart: renders on desktop like the example app; not compared against Android.
- RadarChart: renders on desktop like the example app (web, filled data sets, spoke labels, legend); not compared
  against Android. The example's `RadarMarkerView` popup is not shown, since `MarkerView` is not ported.
- Desktop mouse input (`:sample:desktop:interactions`): click highlights a bar, a single stack segment or a bubble;
  double-click zooms; drag pans the zoomed horizontal bar and combined charts and rotates the pie and radar
  (rotation only starts inside the chart's radius, as on Android).
- **All 69 of MPAndroidChart's own unit tests pass against the port**, plus 10 tests for the `Matrix`,
  `DecimalFormat`, `Color`, text span and bitmap code the port adds: `./gradlew :mpchart:desktopTest` (79 tests). One upstream test relied on
  Android's unit test stubs returning zero font metrics; it now reproduces that stub explicitly.
- The library compiles for Android, desktop and common code with 3 compiler warnings, all in MPAndroidChart's own
  code (parameter names that differ from the interface they implement).
- On Android: tap highlights a bar, drag rotates the pie.
- Markers (desktop): tapping the bar, line and radar samples shows the example apps' marker popups
  (`XYMarkerView`, `MyMarkerView`, `RadarMarkerView`) as `marker` composables.
- `ChartState` (desktop): reports the tapped entry, the zoom and visible range after a double click, and detaches when
  the chart leaves the composition.
- Pie center text with spans (desktop): renders the example's `SpannableString` (large title, gray line, blue italic
  name). On Android the real `android.text.StaticLayout` lays out all chart text now; that path is compiled, not run.
- Number formatting follows the device locale (`DecimalFormatSymbols` of the current locale), tested with German and
  Arabic symbols.
- `toBitmap` and `compress` are covered by tests; `saveToGallery` is compiled for Android and desktop, not run.

## Not done yet

- iOS has not been compiled or run (this was built on Windows).
- Not ported yet: the standalone ScatterChart, CandleStickChart and BubbleChart. Scatter, candle and bubble data
  already work inside CombinedChart, so these are mostly a composable each.
- `MarkerView` (Android XML layouts) is not ported; use the `marker` composable, as MPAndroidChart's Compose module
  does. `MarkerImage` takes a `Drawable`.
- iOS code for locale symbols and `saveToGallery` is written but, like the rest of iOS, not compiled.
- Drawables are code, not Android resources: `GradientDrawable` covers `<shape><gradient>` fills such as the
  example's `fade_red`; other drawables implement `Drawable.draw(canvas)`.
- Desktop: no mouse wheel zoom yet (Android has no wheel either); pinch needs a touch screen.
- Roboto (2.3 MB) and Roboto Italic (0.5 MB) are bundled for every target, including Android, which doesn't need them.
- The packages stay `com.github.mikephil.charting`, so this library and the original MPAndroidChart can't both be on one classpath.

## License

Apache 2.0, see `LICENSE` and `NOTICE`. Derived from MPAndroidChart by Philipp Jahoda and contributors.
Roboto is the font shipped with Android (Apache 2.0 / SIL OFL). Roboto Italic is the variable `Roboto-Italic[wdth,wght].ttf`
from Google Fonts (github.com/google/fonts, `ofl/roboto`), SIL Open Font License 1.1, see
`mpchart/src/commonMain/composeResources/files/Roboto-OFL.txt`.
