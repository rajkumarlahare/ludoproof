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

val releaseStoreFile = providers.environmentVariable("LUDOPROOF_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("LUDOPROOF_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("LUDOPROOF_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("LUDOPROOF_RELEASE_KEY_PASSWORD").orNull
val releaseSigningValues =
    listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    )
val releaseSigningConfigured = releaseSigningValues.all { !it.isNullOrBlank() }
val releaseSigningPartiallyConfigured =
    releaseSigningValues.any { !it.isNullOrBlank() } && !releaseSigningConfigured

if (releaseSigningPartiallyConfigured) {
    throw org.gradle.api.GradleException(
        "Release signing is partially configured. Set all LUDOPROOF_RELEASE_* variables or none of them.",
    )
}

android {
    namespace = "com.ludoproof.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ludoproof.game"
        minSdk = 28
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0-rc2"
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

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.squareup.okhttp3:okhttp-tls:4.12.0")

    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
