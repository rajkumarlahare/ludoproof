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
val ludoProofBillingEnabled =
    providers.gradleProperty("LUDOPROOF_BILLING_ENABLED")
        .orElse("false")
        .get()
        .toBooleanStrictOrNull()
        ?: false

android {
    namespace = "com.ludoproof.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ludoproof.game"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0-rc1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "LUDOPROOF_API_BASE_URL",
            "\"$escapedApiBaseUrl\"",
        )
        buildConfigField(
            "boolean",
            "LUDOPROOF_BILLING_ENABLED",
            ludoProofBillingEnabled.toString(),
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation("androidx.activity:activity:1.13.0")
    implementation("com.android.billingclient:billing:9.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")

    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
