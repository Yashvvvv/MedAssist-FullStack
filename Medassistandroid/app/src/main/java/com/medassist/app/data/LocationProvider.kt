package com.medassist.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class GeoPoint(val lat: Double, val lng: Double, val label: String)

/**
 * The phone's position for "pharmacies near me". Coarse accuracy is plenty (we sort by km) and
 * kinder to the battery. Without permission or a fix, the app falls back to a default city.
 */
class LocationProvider(private val context: Context) {

    /** The point the patient is browsing around (their location or the demo city), shared by all patient screens. */
    val point = kotlinx.coroutines.flow.MutableStateFlow<GeoPoint?>(null)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // checked by hasPermission()
    suspend fun current(): GeoPoint? {
        if (!hasPermission()) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                val cts = CancellationTokenSource()
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc -> cont.resume(loc?.let { GeoPoint(it.latitude, it.longitude, "Your location") }) }
                    .addOnFailureListener { cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }
        }
    }

    companion object {
        /** Noida Sector 18, where the demo pharmacy is: lets anyone try the app without sharing location. */
        val DEMO = GeoPoint(28.5700, 77.3210, "Noida (demo)")
    }
}
