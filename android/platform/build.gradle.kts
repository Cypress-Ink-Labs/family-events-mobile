plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.familyevents.platform"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
}

dependencies {
    implementation(project(":core"))
    testImplementation(libs.junit)
}
