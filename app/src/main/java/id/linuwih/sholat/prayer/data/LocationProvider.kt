package id.linuwih.sholat.prayer.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Lokasi via LocationManager bawaan Android — sengaja TIDAK pakai FusedLocationProvider
 * (Google Play Services) supaya tetap jalan di hp murah/ROM custom yang GMS-nya tidak
 * lengkap (lihat plan, bagian "Tanpa Google Play Services").
 */
class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Coba last-known-location dulu (instan) dari semua provider yang aktif, lalu kalau
     * tidak ada, minta satu fix baru dengan timeout. Return null kalau gagal total
     * (GPS+network mati, atau permission ditolak).
     */
    suspend fun getCurrentLocation(timeoutMs: Long = 15_000L): Location? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        lastKnown(manager)?.let { return it }

        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }

        return requestSingleUpdate(manager, provider, timeoutMs)
    }

    @Suppress("MissingPermission")
    private fun lastKnown(manager: LocationManager): Location? {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        return providers
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    @Suppress("MissingPermission")
    private suspend fun requestSingleUpdate(
        manager: LocationManager,
        provider: String,
        timeoutMs: Long,
    ): Location? = suspendCancellableCoroutine { continuation ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)
                if (continuation.isActive) continuation.resume(location)
            }

            @Deprecated("Deprecated in Java", ReplaceWith(""))
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        val requested = runCatching {
            manager.requestLocationUpdates(provider, 0L, 0f, listener, context.mainLooper)
        }.isSuccess

        if (!requested) {
            if (continuation.isActive) continuation.resume(null)
        } else {
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            android.os.Handler(context.mainLooper).postDelayed({
                manager.removeUpdates(listener)
                if (continuation.isActive) continuation.resume(null)
            }, timeoutMs)
        }
    }
}
