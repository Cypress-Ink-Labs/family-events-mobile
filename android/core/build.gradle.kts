plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.familyevents.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    testImplementation(libs.junit)
}
