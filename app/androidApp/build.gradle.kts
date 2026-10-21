import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

if (providers.gradleProperty("playRelease").orNull == "true") {
    layout.buildDirectory.set(layout.projectDirectory.dir("build/play"))
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation("io.github.vinceglb:filekit-dialogs:0.14.1")
    implementation(project(":app:shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.restor.gamereel"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.restor.gamereel"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 4
        versionName = "1.2.0"
    }
    packaging {
        resources {
            merges += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        val uploadStore = providers.environmentVariable("GAMEREEL_UPLOAD_STORE").orNull
        if (uploadStore != null)
            create("upload") {
                storeFile = file(uploadStore)
                storePassword =
                    providers.environmentVariable("GAMEREEL_UPLOAD_STORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("GAMEREEL_UPLOAD_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("GAMEREEL_UPLOAD_KEY_PASSWORD").get()
            }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("play") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.findByName("upload")
            matchingFallbacks += "release"
        }
        create("preview") {
            initWith(getByName("release"))
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            resValue("string", "app_name", "GameReel Preview")
            matchingFallbacks += "release"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        resValues = true
    }
}

androidComponents {
    beforeVariants(selector().withBuildType("play")) { variant ->
        variant.enable = providers.gradleProperty("playRelease").orNull == "true"
    }
    beforeVariants(selector().withBuildType("preview")) { variant ->
        variant.enable = providers.gradleProperty("playRelease").orNull == "true"
    }
}
