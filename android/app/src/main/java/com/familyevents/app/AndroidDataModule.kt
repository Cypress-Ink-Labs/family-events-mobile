package com.familyevents.app

import android.content.Context
import androidx.room.Room
import com.familyevents.BuildConfig
import com.familyevents.core.EnvConfig
import com.familyevents.data.FamilyEventsDatabase
import com.familyevents.data.RepositoryGraph
import com.familyevents.data.SessionStore
import com.familyevents.platform.PlatformActions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AndroidDataModule {
    @Provides
    @Singleton
    fun provideEnvConfig(): EnvConfig = EnvConfig(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
        mapStyleUrl = BuildConfig.MAP_STYLE_URL,
        googleWebClientId = BuildConfig.ANDROID_GOOGLE_WEB_CLIENT_ID.ifBlank { null },
    )

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FamilyEventsDatabase =
        Room.databaseBuilder(context, FamilyEventsDatabase::class.java, "family-events.db")
            .build()

    @Provides
    @Singleton
    fun provideSessionStore(@ApplicationContext context: Context): SessionStore = EncryptedSessionStore(context)

    @Provides
    @Singleton
    fun provideRepositoryGraph(
        database: FamilyEventsDatabase,
        config: EnvConfig,
        sessionStore: SessionStore,
    ): RepositoryGraph = RepositoryGraph.roomBacked(database, config, sessionStore)

    @Provides
    @Singleton
    fun providePlatformActions(@ApplicationContext context: Context): PlatformActions = PlatformActions(context)
}
