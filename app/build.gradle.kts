plugins {
    id("com.android.application")
}

android {
    namespace = "com.layzee"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.layzee"
        minSdk = 26
        // > 29: below that, dispatchGesture sampling is locked to 100 ms.
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sideload MVP: sign with the debug key so the release APK is installable.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    buildFeatures {
        // BuildConfig.DEBUG gates the per-stage timing log; R8 drops it from release builds.
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.camera:camera-core:1.6.2")
    implementation("androidx.camera:camera-camera2:1.6.2")
    implementation("androidx.camera:camera-lifecycle:1.6.2")
    implementation("com.google.mediapipe:tasks-vision:1.1.0")
    testImplementation("junit:junit:4.13.2")
}
