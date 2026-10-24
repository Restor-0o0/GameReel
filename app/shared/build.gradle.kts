import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
}

// A separate resource tree prevents unlicensed store images from entering the Play artifact.
if (providers.gradleProperty("playRelease").orNull == "true") {
    // Isolate incremental resource outputs from the local-artwork build.
    layout.buildDirectory.set(layout.projectDirectory.dir("build/play"))
    val playResources = layout.buildDirectory.dir("playComposeResources")
    val preparePlayResources =
        tasks.register<Sync>("preparePlayResources") {
            from(layout.projectDirectory.dir("src/commonMain/composeResources")) {
                exclude("files/art/**")
                exclude(
                    "files/libraries/steam_broad.json",
                    "files/libraries/steam_alltime.json",
                    "files/libraries/google_play.json",
                )
                filesMatching("files/art-policy.txt") { filter { "no-store-art" } }
            }
            into(playResources)
        }
    compose.resources {
        customDirectory(
            "commonMain",
            preparePlayResources.map { playResources.get() },
        )
    }
}

kotlin {
    listOf(
            iosArm64(),
            iosSimulatorArm64(),
        )
        .forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "Shared"
                isStatic = true
            }
        }

    jvm()

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    android {
        namespace = "com.restor.gamereel.app.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
            .configure {
                instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.sqldelight.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.compose.uiToolingPreview)
        }
        commonMain.dependencies {
            implementation(libs.serialization.json)
            implementation(libs.filekit.dialogs)
            implementation(libs.sqldelight.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            api(project(":core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native)
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite)
            implementation(libs.ktor.client.okhttp)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation(libs.ktor.client.js)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

sqldelight {
    databases {
        create("GameReelDatabase") {
            packageName.set("com.restor.gamereel.database")
        }
    }
}
