import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    explicitApi()

    android {
        namespace = "com.github.mikephil.charting"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    // iOS targets are declared here; they only compile on macOS.
    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        named("desktopTest").dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.junit)
        }

        // Skia backend shared by desktop and iOS
        val skikoMain by creating {
            dependsOn(commonMain.get())
        }
        named("desktopMain") { dependsOn(skikoMain) }
        val iosMain by creating { dependsOn(skikoMain) }
        named("iosArm64Main") { dependsOn(iosMain) }
        named("iosSimulatorArm64Main") { dependsOn(iosMain) }
    }
}

compose.resources {
    packageOfResClass = "com.github.mikephil.charting.resources"
    publicResClass = false
}
