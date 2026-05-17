package com.familyevents.data

import com.familyevents.core.EnvConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientFactory {
    fun create(config: EnvConfig) = createSupabaseClient(
        supabaseUrl = config.supabaseUrl,
        supabaseKey = config.supabaseAnonKey,
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Functions)
    }
}
