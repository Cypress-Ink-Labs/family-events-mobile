package com.familyevents.core

data class GeoCoordinate(val latitude: Double, val longitude: Double) {
    val isValid: Boolean = latitude in -90.0..90.0 && longitude in -180.0..180.0
}
