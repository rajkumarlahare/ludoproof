plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ludoproof.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ludoproof.game"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField(
            "String",
            "LUDOPROOF_API_BASE_URL",
            "\"https://ludoproof-game-api.ai-8f3.workers.dev\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}
