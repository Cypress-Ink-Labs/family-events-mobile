package com.familyevents.core

data class EnvConfig(
    val supabaseUrl: String,
    val supabaseAnonKey: String,
    val mapStyleUrl: String,
    val googleWebClientId: String?,
) {
    val googleSignInEnabled: Boolean = !googleWebClientId.isNullOrBlank()

    init {
        require(supabaseUrl.isNotBlank()) { "SUPABASE_URL is required" }
        require(supabaseAnonKey.isNotBlank()) { "SUPABASE_ANON_KEY is required" }
    }

    companion object {
        const val DebugSupabaseUrl = "http://10.0.2.2:55321"
        const val DebugSupabaseAnonKey = "sb_publishable_ACJWlzQHlZjBrEguHvfOxg_3BJgxAaH"
    }
}
