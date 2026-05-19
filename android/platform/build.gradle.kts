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
    implementation(libs.play.services.location)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
}
