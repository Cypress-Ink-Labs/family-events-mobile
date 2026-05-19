package com.familyevents.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.familyevents.core.GeoCoordinate
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface LocationProvider {
    /**
     * Returns the device's last-known coordinate, or null when:
     * - Location permission has not been granted.
     * - Play Services has no cached location yet (cold start).
     * - The request fails for any other reason.
     */
    suspend fun lastKnownLocation(): GeoCoordinate?
}

class FusedLocationProvider(private val application: Application) : LocationProvider {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(application) }

    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission") // guarded by hasPermission()
    override suspend fun lastKnownLocation(): GeoCoordinate? {
        if (!hasPermission()) return null
        return suspendCancellableCoroutine { cont ->
            client.lastLocation
                .addOnSuccessListener { loc ->
                    cont.resume(loc?.let { GeoCoordinate(it.latitude, it.longitude) })
                }
                .addOnFailureListener { cont.resume(null) }
                .addOnCanceledListener { cont.resume(null) }
        }
    }
}

/** Stub used by previews and tests. */
class NoOpLocationProvider : LocationProvider {
    override suspend fun lastKnownLocation(): GeoCoordinate? = null
}
