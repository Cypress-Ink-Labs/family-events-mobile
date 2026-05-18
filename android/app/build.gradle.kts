import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.legacy.kapt)
    alias(libs.plugins.hilt)
}

fun getEnvValue(key: String): String? {
    // 1. System environment variable
    val env = System.getenv(key) ?: System.getenv("VITE_$key")
    if (!env.isNullOrBlank()) return env

    // 2. Project property (-Pkey=value)
    val prop = project.findProperty(key) as? String ?: project.findProperty("VITE_$key") as? String
    if (!prop.isNullOrBlank()) return prop

    // 3. .env file in root
    val envFile = project.rootProject.file("../../.env")
    if (envFile.exists()) {
        val properties = Properties()
        envFile.inputStream().use { properties.load(it) }
        val fileVal = properties.getProperty(key) ?: properties.getProperty("VITE_$key")
        if (!fileVal.isNullOrBlank()) return fileVal
    }

    return null
}

android {
    namespace = "com.familyevents"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId = "com.familyevents.app"
        minSdk = 26
        targetSdk = 36
        versionCode = getEnvValue("ANDROID_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = getEnvValue("ANDROID_VERSION_NAME") ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val supabaseUrl = getEnvValue("SUPABASE_URL") ?: "http://10.0.2.2:55321"
        val supabaseKey = getEnvValue("SUPABASE_ANON_KEY") ?: "sb_publishable_ACJWlzQHlZjBrEguHvfOxg_3BJgxAaH"
        val mapStyleUrl = getEnvValue("MAP_STYLE_URL") ?: "https://demotiles.maplibre.org/style.json"
        val webClientId = getEnvValue("ANDROID_GOOGLE_WEB_CLIENT_ID") ?: ""

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseKey\"")
        buildConfigField("String", "MAP_STYLE_URL", "\"$mapStyleUrl\"")
        buildConfigField("String", "ANDROID_GOOGLE_WEB_CLIENT_ID", "\"$webClientId\"")
    }

    signingConfigs {
        create("release") {
            val keystorePath = getEnvValue("ANDROID_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = getEnvValue("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = getEnvValue("ANDROID_KEY_ALIAS")
                keyPassword = getEnvValue("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val isSigningConfigured = getEnvValue("ANDROID_KEYSTORE_PATH") != null
            signingConfig = if (isSigningConfigured) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    packaging {
        jniLibs.keepDebugSymbols += setOf(
            "**/libandroidx.graphics.path.so",
            "**/libmaplibre.so",
        )
    }
}

gradle.taskGraph.whenReady {
    if (allTasks.any { it.name.contains("Release") || it.name == "bundle" }) {
        val releaseUrl = getEnvValue("SUPABASE_URL")
        val releaseKey = getEnvValue("SUPABASE_ANON_KEY")
        if (releaseUrl.isNullOrBlank() || releaseKey.isNullOrBlank()) {
            throw GradleException("Release builds require SUPABASE_URL and SUPABASE_ANON_KEY. Please set them in your environment or root .env file (VITE_ prefix supported).")
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":designsystem"))
    implementation(project(":auth"))
    implementation(project(":plan"))
    implementation(project(":explore"))
    implementation(project(":saved"))
    implementation(project(":eventdetail"))
    implementation(project(":platform"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.security.crypto)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    testImplementation(libs.junit)
}
