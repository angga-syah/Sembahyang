package id.linuwih.sholat.qibla.ui

import android.app.Application
import android.hardware.GeomagneticField
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Qibla
import id.linuwih.sholat.R
import id.linuwih.sholat.prayer.data.LocationProvider
import id.linuwih.sholat.prayer.data.PlaceNameResolver
import id.linuwih.sholat.prayer.data.PrayerPrefs
import id.linuwih.sholat.qibla.data.CompassSensor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Ambang lama (masuk 6, lepas 12) terlalu longgar: meleset 12 derajat masih ditandai hijau,
// padahal untuk arah kiblat selisih sebesar itu sudah terasa. Sekarang dipersempit, tapi tetap
// menyisakan jarak antara "masuk" dan "lepas" supaya statusnya tidak kedip-kedip di ambang.
private const val ALIGNMENT_ENTER_DEGREES = 3f
private const val ALIGNMENT_RELEASE_DEGREES = 6f

class QiblaViewModel(application: Application) : AndroidViewModel(application) {

    private val locationProvider = LocationProvider(application)
    private val placeNameResolver = PlaceNameResolver(application)
    private val prefs = PrayerPrefs(application)
    private val compassSensor = CompassSensor(application)

    private val _uiState = MutableStateFlow(QiblaUiState(hasCompassSensor = compassSensor.hasSensor()))
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    private var declinationDegrees: Float = 0f

    init {
        val lat = prefs.lastLatitude
        val lng = prefs.lastLongitude
        if (lat != null && lng != null) {
            onLocationReady(lat, lng)
        }
        if (locationProvider.hasPermission()) {
            refreshLocation()
        } else if (lat == null) {
            _uiState.value = _uiState.value.copy(loading = false, permissionDenied = true)
        }
        observeCompass()
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) refreshLocation() else _uiState.value = _uiState.value.copy(loading = false, permissionDenied = true)
    }

    fun refreshLocation() {
        _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            val location = locationProvider.getCurrentLocation()
            if (location != null) {
                prefs.saveLocation(location.latitude, location.longitude, manual = false)
                onLocationReady(location.latitude, location.longitude, location.altitude)
            } else {
                val lat = prefs.lastLatitude
                val lng = prefs.lastLongitude
                if (lat != null && lng != null) {
                    onLocationReady(lat, lng)
                } else {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        errorMessage = getApplication<Application>().getString(R.string.qibla_location_error),
                    )
                }
            }
        }
    }

    private fun onLocationReady(latitude: Double, longitude: Double, altitude: Double = 0.0) {
        val bearing = Qibla(Coordinates(latitude, longitude)).direction.toFloat()
        declinationDegrees = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            altitude.toFloat(),
            System.currentTimeMillis(),
        ).declination

        _uiState.value = _uiState.value.copy(
            loading = false,
            permissionDenied = false,
            errorMessage = null,
            locationLabel = "%.4f, %.4f".format(latitude, longitude),
            qiblaBearing = bearing,
        )

        viewModelScope.launch {
            val name = placeNameResolver.resolve(latitude, longitude)
            if (name != null) _uiState.value = _uiState.value.copy(locationLabel = name)
        }
    }

    private fun observeCompass() {
        if (!compassSensor.hasSensor()) return
        viewModelScope.launch {
            compassSensor.readings().collect { reading ->
                val bearing = _uiState.value.qiblaBearing ?: return@collect
                val trueAzimuth = (reading.azimuthDegrees + declinationDegrees + 360f) % 360f
                val needleRotation = (bearing - trueAzimuth + 360f) % 360f
                val accuracyLow = reading.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW

                // Selisih absolut ke arah kiblat (0..180), supaya 359 derajat dihitung sebagai
                // meleset 1 derajat — bukan 359.
                val offBy = minOf(needleRotation, 360f - needleRotation)
                val wasAligned = _uiState.value.isAligned
                val isAligned = if (wasAligned) {
                    offBy <= ALIGNMENT_RELEASE_DEGREES
                } else {
                    offBy <= ALIGNMENT_ENTER_DEGREES
                }

                _uiState.value = _uiState.value.copy(
                    needleRotation = needleRotation,
                    northRotation = -trueAzimuth,
                    accuracyLow = accuracyLow,
                    isAligned = isAligned,
                    offByDegrees = offBy,
                )
            }
        }
    }
}
