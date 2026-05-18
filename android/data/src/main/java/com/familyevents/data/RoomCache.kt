package com.familyevents.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "events")
data class CachedEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val startsAt: String,
    val endsAt: String?,
    val venueName: String?,
    val address: String?,
    val imageUrl: String?,
    val sourceUrl: String?,
    val cityId: String,
    val latitude: Double?,
    val longitude: Double?,
)

@Entity(tableName = "favorites", primaryKeys = ["userId", "eventId"])
data class CachedFavoriteEntity(val userId: String, val eventId: String, val createdAt: String)

@Entity(tableName = "plan_events", primaryKeys = ["userId", "eventId"])
data class CachedPlanEventEntity(val userId: String, val eventId: String, val section: String, val rank: Int)

@Entity(tableName = "profiles")
data class CachedProfileEntity(
    @PrimaryKey val userId: String,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val currentCityId: String?,
    val childName: String?,
    val kidAge: Int?,
    val notificationsEnabled: Boolean,
)

@Entity(tableName = "cities")
data class CachedCityEntity(@PrimaryKey val id: String, val name: String, val region: String?)

@Entity(tableName = "weather", primaryKeys = ["cityId", "dateKey"])
data class CachedWeatherEntity(
    val cityId: String,
    val dateKey: String,
    val summary: String,
    val temperatureHighF: Int?,
    val precipitationChance: Double?,
)

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE cityId = COALESCE(:cityId, cityId) ORDER BY startsAt LIMIT :limit OFFSET :offset")
    fun observeEvents(cityId: String?, limit: Int, offset: Int): Flow<List<CachedEventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun observeEvent(id: String): Flow<CachedEventEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(events: List<CachedEventEntity>)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeFavorites(userId: String): Flow<List<CachedFavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND eventId = :eventId LIMIT 1")
    suspend fun favorite(userId: String, eventId: String): CachedFavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: CachedFavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND eventId = :eventId")
    suspend fun delete(userId: String, eventId: String)
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan_events WHERE userId = :userId ORDER BY rank")
    fun observePlan(userId: String): Flow<List<CachedPlanEventEntity>>

    @Query("DELETE FROM plan_events WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rows: List<CachedPlanEventEntity>)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE userId = :userId")
    fun observeProfile(userId: String): Flow<CachedProfileEntity?>

    @Query("SELECT * FROM profiles WHERE userId = :userId")
    suspend fun profile(userId: String): CachedProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: CachedProfileEntity)
}

@Dao
interface CityDao {
    @Query("SELECT * FROM cities ORDER BY name")
    fun observeCities(): Flow<List<CachedCityEntity>>

    @Query("SELECT name FROM cities WHERE id = :id")
    suspend fun cityName(id: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cities: List<CachedCityEntity>)
}

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather WHERE cityId = :cityId ORDER BY dateKey")
    fun observeForecast(cityId: String): Flow<List<CachedWeatherEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rows: List<CachedWeatherEntity>)
}

@Database(
    entities = [
        CachedEventEntity::class,
        CachedFavoriteEntity::class,
        CachedPlanEventEntity::class,
        CachedProfileEntity::class,
        CachedCityEntity::class,
        CachedWeatherEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class FamilyEventsDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun planDao(): PlanDao
    abstract fun profileDao(): ProfileDao
    abstract fun cityDao(): CityDao
    abstract fun weatherDao(): WeatherDao
}
