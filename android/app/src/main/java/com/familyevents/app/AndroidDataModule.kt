package com.familyevents.app

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.familyevents.core.EnvConfig
import com.familyevents.data.FamilyEventsDatabase
import com.familyevents.data.RepositoryGraph
import com.familyevents.data.SessionStore
import com.familyevents.platform.FusedLocationProvider
import com.familyevents.platform.LocationProvider
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
            .addMigrations(ProfileV2Migration)
            .addMigrations(ProfileV3RoleMigration)
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

    @Provides
    @Singleton
    fun provideLocationProvider(@ApplicationContext context: Context): LocationProvider =
        FusedLocationProvider(context.applicationContext as android.app.Application)
}

private object ProfileV2Migration : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profiles ADD COLUMN email TEXT")
        db.execSQL("ALTER TABLE profiles ADD COLUMN displayName TEXT")
        db.execSQL("ALTER TABLE profiles ADD COLUMN avatarUrl TEXT")
        db.execSQL("ALTER TABLE profiles ADD COLUMN childName TEXT")
    }
}

private object ProfileV3RoleMigration : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profiles ADD COLUMN role TEXT NOT NULL DEFAULT 'user'")
    }
}
