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

android {
    namespace = "com.blanksstudio.gridpix"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.blanksstudio.gridpix"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PLAY_LICENSE_KEY", "\"${secret("PLAY_LICENSE_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

    // Tests (game/ is pure Kotlin and is unit-tested here)
    testImplementation(libs.junit)
    testImplementation(libs.org.json) // real org.json so PackParser runs in JVM tests
}
