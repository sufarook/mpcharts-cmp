import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":sample:shared"))
    implementation(compose.desktop.currentOs)
    runtimeOnly(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "com.github.mikephil.charting.sample.MainKt"
    }
}

tasks.register<JavaExec>("screenshots") {
    description = "Renders the sample screens to PNG files in build/screenshots."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.github.mikephil.charting.sample.ScreenshotKt")
    val width = providers.gradleProperty("shotWidth").orElse("1080").get()
    val height = providers.gradleProperty("shotHeight").orElse("2000").get()
    val density = providers.gradleProperty("shotDensity").orElse("2.625").get()
    args(layout.buildDirectory.dir("screenshots").get().asFile.absolutePath, width, height, density)
}

tasks.register<JavaExec>("interactions") {
    description = "Drives the sample with mouse input headless and writes a PNG per step to build/interactions."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.github.mikephil.charting.sample.InteractionsKt")
    args(layout.buildDirectory.dir("interactions").get().asFile.absolutePath)
}
