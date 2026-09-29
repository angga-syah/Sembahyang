package id.linuwih.sholat.prayer.ui

import com.batoulapps.adhan2.Prayer
import id.linuwih.sholat.prayer.data.PrayerCalculationMethod

data class PrayerTimeRow(val label: String, val prayer: Prayer, val timeText: String, val isNext: Boolean)

data class PrayerUiState(
    val loading: Boolean = true,
    val permissionDenied: Boolean = false,
    val locationLabel: String? = null,
    val isManualLocation: Boolean = false,
    val method: PrayerCalculationMethod = PrayerCalculationMethod.KEMENAG_RI,
    val rows: List<PrayerTimeRow> = emptyList(),
    /** Mis. "Senin, 25 Agustus 2026" dan "12 Rabiul Awal 1448 H". */
    val gregorianDate: String = "",
    val hijriDate: String = "",
    val nextPrayerLabel: String? = null,
    /** Mis. "2 jam 15 menit lagi" — diperbarui berkala oleh ticker di PrayerViewModel. */
    val nextPrayerCountdown: String? = null,
    val errorMessage: String? = null,
    val notificationsEnabled: Map<Prayer, Boolean> = emptyMap(),
)
