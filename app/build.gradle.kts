import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

/**
 * Secrets come from local.properties (gitignored) and land in BuildConfig.
 * Missing keys default to empty so a fresh clone still builds.
 *   PLAY_LICENSE_KEY=<Base64 RSA key from Play Console > Monetisation setup>
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(name: String): String = localProperties.getProperty(name, "")

/**
 * AdMob (decision D29). Debug always uses Google's published test IDs (tapping your own live ads
 * gets the AdMob account banned). Release reads the real IDs from local.properties:
 *   ADMOB_APP_ID=ca-app-pub-...~...   ADMOB_BANNER_ID=   ADMOB_INTERSTITIAL_ID=   ADMOB_REWARDED_ID=
 * and falls back to the test IDs with a build warning when one is missing.
 * Test IDs: https://developers.google.com/admob/android/test-ads
 */
val admobTestIds = mapOf(
    "ADMOB_APP_ID" to "ca-app-pub-3940256099942544~3347511713",
    "ADMOB_BANNER_ID" to "ca-app-pub-3940256099942544/9214589741",
    "ADMOB_INTERSTITIAL_ID" to "ca-app-pub-3940256099942544/1033173712",
    "ADMOB_REWARDED_ID" to "ca-app-pub-3940256099942544/5224354917",
)

fun admobId(name: String, release: Boolean): String {
    val test = admobTestIds.getValue(name)
    if (!release) return test
    return secret(name).ifBlank {
        logger.warn("local.properties: $name missing, release will use the AdMob TEST id")
        test
    }
}

/**
 * Release signing (upload key for Play App Signing). keystore.properties is gitignored:
 *   storeFile=C:/dev/Apps/keys/gridpix-upload.jks, storePassword=, keyAlias=gridpix-upload, keyPassword=
 * Without it the release build is unsigned, so a CI/clean clone still compiles.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasReleaseKey = keystoreProperties.getProperty("storeFile")?.let { file(it).exists() } == true

android {
    namespace = "com.blanksstudio.gridpix"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.blanksstudio.gridpix"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PLAY_LICENSE_KEY", "\"${secret("PLAY_LICENSE_KEY")}\"")
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = admobId("ADMOB_APP_ID", release = false)
            listOf("ADMOB_BANNER_ID", "ADMOB_INTERSTITIAL_ID", "ADMOB_REWARDED_ID").forEach {
                buildConfigField("String", it, "\"${admobId(it, release = false)}\"")
            }
            // ADS_DEBUG_EEA=true in local.properties makes the consent form show as if in the EU.
            buildConfigField("boolean", "ADS_DEBUG_EEA", secret("ADS_DEBUG_EEA").equals("true", ignoreCase = true).toString())
            // ADS_DEBUG_FAST=true (debug only): no ad-free first day and a between-puzzle ad after every solve, to test ads quickly.
            buildConfigField("boolean", "ADS_DEBUG_FAST", secret("ADS_DEBUG_FAST").equals("true", ignoreCase = true).toString())
        }
        release {
            manifestPlaceholders["admobAppId"] = admobId("ADMOB_APP_ID", release = true)
            listOf("ADMOB_BANNER_ID", "ADMOB_INTERSTITIAL_ID", "ADMOB_REWARDED_ID").forEach {
                buildConfigField("String", it, "\"${admobId(it, release = true)}\"")
            }
            buildConfigField("boolean", "ADS_DEBUG_EEA", "false")
            buildConfigField("boolean", "ADS_DEBUG_FAST", "false")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
            else logger.warn("keystore.properties missing: release build will be unsigned")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Built-in Kotlin: jvmTarget follows targetCompatibility, no kotlinOptions needed.
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room {
    // Exported schema JSON per DB version (CLAUDE.md rule: migrations are written, never destructive).
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Core + lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose (versions from the BOM)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core) // material3 1.4+ no longer brings icons transitively
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Google Play Billing: hints, packs, large grids, everything bundle (SPEC section 6). The only network use.
    implementation(libs.play.billing)

    // Daily reminder notification (opt-in) and the Play rating prompt (decision D27).
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.play.review)
    // Ads (decision D29): AdMob banner/interstitial/rewarded + Google's consent form (required in the EU/UK).
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Tests (game/ is pure Kotlin and is unit-tested here)
    testImplementation(libs.junit)
    testImplementation(libs.org.json) // real org.json so PackParser runs in JVM tests
}
