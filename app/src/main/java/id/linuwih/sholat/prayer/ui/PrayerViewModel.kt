package id.linuwih.sholat.prayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan2.Prayer
import com.batoulapps.adhan2.PrayerTimes
import id.linuwih.sholat.ibadah.data.HijriDate
import id.linuwih.sholat.prayer.data.LocationProvider
import id.linuwih.sholat.prayer.data.PrayerCalculationMethod
import id.linuwih.sholat.prayer.data.PrayerLabels
import id.linuwih.sholat.prayer.data.PlaceNameResolver
import id.linuwih.sholat.prayer.data.PrayerPrefs
import id.linuwih.sholat.prayer.data.PrayerTimesFactory
import id.linuwih.sholat.prayer.notification.PrayerAlarmScheduler
import id.linuwih.sholat.prayer.widget.PrayerWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PrayerPrefs(application)
    private val locationProvider = LocationProvider(application)
    private val placeNameResolver = PlaceNameResolver(application)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }
    private val dateFormatter = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())

    private val _uiState = MutableStateFlow(PrayerUiState(method = prefs.calculationMethod ?: PrayerCalculationMethod.KEMENAG_RI))
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    // Disimpan supaya ticker hitung-mundur bisa menghitung ulang tanpa mengambil lokasi lagi.
    private var currentTimes: PrayerTimes? = null
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentMethod: PrayerCalculationMethod = prefs.calculationMethod ?: PrayerCalculationMethod.KEMENAG_RI
    private var countdownJob: Job? = null

    init {
        val lat = prefs.lastLatitude
        val lng = prefs.lastLongitude
        if (lat != null && lng != null) {
            // Tampilkan hasil terakhir dulu (instan), lalu coba refresh lokasi di background.
            recompute(lat, lng, prefs.isManualLocation)
        }
        if (locationProvider.hasPermission()) {
            refreshLocation()
        } else if (lat == null) {
            _uiState.value = _uiState.value.copy(loading = false, permissionDenied = true)
        }
        startCountdownTicker()
    }

    /**
     * Hitung-mundur ke waktu sholat berikutnya. Interval 15 detik (bukan per-detik) karena yang
     * ditampilkan cuma presisi menit — cukup akurat tapi tetap ringan, dan job ini otomatis mati
     * bersama ViewModel saat layar ditutup, jadi tidak ada polling di background.
     */
    private fun startCountdownTicker() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                tickCountdown()
                delay(15_000)
            }
        }
    }

    private fun tickCountdown() {
        val times = currentTimes ?: return
        val lat = currentLatitude ?: return
        val lng = currentLongitude ?: return
        val now = kotlin.time.Clock.System.now()
        val next = times.nextPrayer(now)

        if (next == Prayer.NONE) {
            // Isya hari ini sudah lewat — target berikutnya Subuh besok.
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowTimes = PrayerTimesFactory.forDate(getApplication(), lat, lng, currentMethod, tomorrow)
            val remaining = tomorrowTimes.fajr.toEpochMilliseconds() - now.toEpochMilliseconds()
            _uiState.value = _uiState.value.copy(
                rows = buildRows(times, Prayer.NONE),
                nextPrayerLabel = labelFor(Prayer.FAJR),
                nextPrayerCountdown = formatCountdown(remaining),
            )
            return
        }

        val target = instantFor(times, next) ?: return
        val remaining = target.toEpochMilliseconds() - now.toEpochMilliseconds()
        _uiState.value = _uiState.value.copy(
            rows = buildRows(times, next),
            nextPrayerLabel = labelFor(next),
            nextPrayerCountdown = formatCountdown(remaining),
        )
    }

    private fun instantFor(times: PrayerTimes, prayer: Prayer) = when (prayer) {
        Prayer.FAJR -> times.fajr
        Prayer.SUNRISE -> times.sunrise
        Prayer.DHUHR -> times.dhuhr
        Prayer.ASR -> times.asr
        Prayer.MAGHRIB -> times.maghrib
        Prayer.ISHA -> times.isha
        Prayer.NONE -> null
    }

    private fun formatCountdown(remainingMillis: Long): String {
        val context = getApplication<Application>()
        if (remainingMillis <= 0) return context.getString(id.linuwih.sholat.R.string.prayer_countdown_now)
        val totalMinutes = remainingMillis / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            totalMinutes < 1 -> context.getString(id.linuwih.sholat.R.string.prayer_countdown_soon)
            hours > 0 -> context.getString(id.linuwih.sholat.R.string.prayer_countdown_hm, hours, minutes)
            else -> context.getString(id.linuwih.sholat.R.string.prayer_countdown_m, minutes)
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            refreshLocation()
        } else {
            _uiState.value = _uiState.value.copy(loading = false, permissionDenied = true)
        }
    }

    fun refreshLocation() {
        _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            val location = locationProvider.getCurrentLocation()
            if (location != null) {
                prefs.saveLocation(location.latitude, location.longitude, manual = false)
                recompute(location.latitude, location.longitude, manual = false)
            } else {
                val lat = prefs.lastLatitude
                val lng = prefs.lastLongitude
                if (lat != null && lng != null) {
                    recompute(lat, lng, prefs.isManualLocation)
                } else {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        errorMessage = getApplication<Application>().getString(id.linuwih.sholat.R.string.prayer_location_error),
                    )
                }
            }
        }
    }

    fun setManualLocation(latitude: Double, longitude: Double) {
        prefs.saveLocation(latitude, longitude, manual = true)
        recompute(latitude, longitude, manual = true)
    }

    fun setCalculationMethod(method: PrayerCalculationMethod) {
        prefs.calculationMethod = method
        val lat = prefs.lastLatitude
        val lng = prefs.lastLongitude
        if (lat != null && lng != null) {
            recompute(lat, lng, prefs.isManualLocation)
        } else {
            _uiState.value = _uiState.value.copy(method = method)
        }
    }

    fun setNotificationEnabled(prayer: Prayer, enabled: Boolean) {
        prefs.setNotificationEnabled(prayer, enabled)
        _uiState.value = _uiState.value.copy(
            notificationsEnabled = _uiState.value.notificationsEnabled + (prayer to enabled),
        )
        // Reschedule pakai lokasi terakhir supaya alarm yang baru dimatikan langsung dibatalkan,
        // dan yang baru dinyalakan langsung dipasang.
        PrayerAlarmScheduler.scheduleForToday(getApplication())
        PrayerWidgetProvider.updateAll(getApplication())
    }

    private fun currentNotificationSettings(): Map<Prayer, Boolean> =
        PrayerAlarmScheduler.prayers.associateWith { prefs.isNotificationEnabled(it) }

    fun hasSavedLocation(): Boolean = prefs.lastLatitude != null && prefs.lastLongitude != null

    /** null kalau belum ada lokasi tersimpan sama sekali (belum pernah buka tab Jadwal). */
    fun monthlySchedule(monthsFromNow: Int): MonthlySchedule? {
        val lat = prefs.lastLatitude ?: return null
        val lng = prefs.lastLongitude ?: return null
        val method = prefs.calculationMethod ?: PrayerCalculationMethod.defaultFor(lat, lng)

        val monthCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, monthsFromNow)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("EEE, d", Locale.getDefault())

        val today = Calendar.getInstance()
        val rows = (1..daysInMonth).map { day ->
            val dayCal = monthCal.clone() as Calendar
            dayCal.set(Calendar.DAY_OF_MONTH, day)
            val isToday = dayCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                dayCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            val times = PrayerTimesFactory.forDate(getApplication(), lat, lng, method, dayCal)
            MonthlyDayRow(
                dateLabel = dateFormatter.format(dayCal.time),
                fajr = timeFormatter.format(Date(times.fajr.toEpochMilliseconds())),
                dhuhr = timeFormatter.format(Date(times.dhuhr.toEpochMilliseconds())),
                asr = timeFormatter.format(Date(times.asr.toEpochMilliseconds())),
                maghrib = timeFormatter.format(Date(times.maghrib.toEpochMilliseconds())),
                isha = timeFormatter.format(Date(times.isha.toEpochMilliseconds())),
                isToday = isToday,
            )
        }
        return MonthlySchedule(monthLabel = monthFormatter.format(monthCal.time), rows = rows)
    }

    private fun recompute(latitude: Double, longitude: Double, manual: Boolean) {
        val method = prefs.calculationMethod ?: PrayerCalculationMethod.defaultFor(latitude, longitude).also {
            prefs.calculationMethod = it
        }
        val times = PrayerTimesFactory.forToday(getApplication(), latitude, longitude, method)
        val now = kotlin.time.Clock.System.now()
        val next = times.nextPrayer(now)

        currentTimes = times
        currentLatitude = latitude
        currentLongitude = longitude
        currentMethod = method

        val today = Calendar.getInstance()
        _uiState.value = PrayerUiState(
            gregorianDate = dateFormatter.format(today.time),
            hijriDate = HijriDate.from(today).toString(),
            loading = false,
            permissionDenied = false,
            locationLabel = "%.4f, %.4f".format(latitude, longitude),
            isManualLocation = manual,
            method = method,
            rows = buildRows(times, next),
            nextPrayerLabel = next.takeIf { it != Prayer.NONE }?.let { labelFor(it) },
            notificationsEnabled = currentNotificationSettings(),
        )
        // Jangan tunggu tick berikutnya (15 detik) — isi hitung-mundur langsung.
        tickCountdown()

        // Nama tempat dicari belakangan supaya jadwal tampil seketika: koordinat sudah cukup
        // untuk menghitung, nama hanya mempercantik. Kalau gagal (offline / perangkat tanpa
        // layanan geocoder), label tetap menampilkan koordinat.
        viewModelScope.launch {
            val name = placeNameResolver.resolve(latitude, longitude)
            if (name != null && _uiState.value.locationLabel != null) {
                _uiState.value = _uiState.value.copy(locationLabel = name)
            }
        }

        PrayerAlarmScheduler.scheduleForToday(getApplication())
        PrayerWidgetProvider.updateAll(getApplication())
    }

    private fun buildRows(times: PrayerTimes, next: Prayer): List<PrayerTimeRow> {
        val entries = listOf(
            Prayer.FAJR to times.fajr,
            Prayer.SUNRISE to times.sunrise,
            Prayer.DHUHR to times.dhuhr,
            Prayer.ASR to times.asr,
            Prayer.MAGHRIB to times.maghrib,
            Prayer.ISHA to times.isha,
        )
        return entries.map { (prayer, instant) ->
            PrayerTimeRow(
                label = labelFor(prayer),
                prayer = prayer,
                timeText = timeFormatter.format(Date(instant.toEpochMilliseconds())),
                isNext = prayer == next,
            )
        }
    }

    private fun labelFor(prayer: Prayer): String = PrayerLabels.label(getApplication(), prayer)
}
