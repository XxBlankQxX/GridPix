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
        versionCode = 4
        versionName = "1.1.0"

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
        release {
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

    // Tests (game/ is pure Kotlin and is unit-tested here)
    testImplementation(libs.junit)
    testImplementation(libs.org.json) // real org.json so PackParser runs in JVM tests
}
