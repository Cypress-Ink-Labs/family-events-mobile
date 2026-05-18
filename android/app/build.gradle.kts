plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.legacy.kapt)
    alias(libs.plugins.hilt)
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
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${providers.environmentVariable("SUPABASE_URL").orElse("http://10.0.2.2:55321").get()}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${providers.environmentVariable("SUPABASE_ANON_KEY").orElse("sb_publishable_ACJWlzQHlZjBrEguHvfOxg_3BJgxAaH").get()}\"")
        buildConfigField("String", "MAP_STYLE_URL", "\"${providers.environmentVariable("MAP_STYLE_URL").orElse("https://demotiles.maplibre.org/style.json").get()}\"")
        buildConfigField("String", "ANDROID_GOOGLE_WEB_CLIENT_ID", "\"${providers.environmentVariable("ANDROID_GOOGLE_WEB_CLIENT_ID").orElse("").get()}\"")
    }

    signingConfigs {
        create("release") {
            val keystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
        val releaseUrl = providers.environmentVariable("SUPABASE_URL").orElse("").get()
        val releaseKey = providers.environmentVariable("SUPABASE_ANON_KEY").orElse("").get()
        if (releaseUrl.isBlank() || releaseKey.isBlank()) {
            throw GradleException("Release builds require SUPABASE_URL and SUPABASE_ANON_KEY.")
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
