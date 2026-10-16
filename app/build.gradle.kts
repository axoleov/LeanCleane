plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lean.cleaner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lean.cleaner"
        minSdk = 26
        targetSdk = 26
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
