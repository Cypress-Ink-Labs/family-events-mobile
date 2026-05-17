plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.familyevents.platform"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(project(":core"))
    testImplementation(libs.junit)
}
