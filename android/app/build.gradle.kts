plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ludoProofApiBaseUrl =
    providers.gradleProperty("LUDOPROOF_API_BASE_URL")
        .orElse("https://ludoproof-game-api.ai-8f3.workers.dev")
        .get()
val escapedApiBaseUrl =
    ludoProofApiBaseUrl
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

android {
    namespace = "com.ludoproof.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ludoproof.game"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-rc1"

        buildConfigField(
            "String",
            "LUDOPROOF_API_BASE_URL",
            "\"$escapedApiBaseUrl\"",
        )
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt",
                ),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.maxHeapSize = "1g"
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

kotlin {
    jvmToolchain(17)
}


dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
