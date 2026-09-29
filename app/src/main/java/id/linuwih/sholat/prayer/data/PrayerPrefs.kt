package id.linuwih.sholat.prayer.data

import android.content.Context
import com.batoulapps.adhan2.Prayer
import java.util.Locale

/** Cara notifikasi adzan dibunyikan. */
enum class NotificationStyle { SOUND, VIBRATE, SILENT }

/**
 * Penyimpanan ringan (SharedPreferences) untuk pilihan metode & koordinat terakhir.
 * Koordinat yang tersimpan di sini dipakai ulang oleh PrayerAlarmScheduler di background
 * (BroadcastReceiver saat boot/reschedule tidak punya akses ke GPS live).
 */
class PrayerPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE)

    var calculationMethod: PrayerCalculationMethod?
        get() = prefs.getString(KEY_METHOD, null)?.let { name ->
            runCatching { PrayerCalculationMethod.valueOf(name) }.getOrNull()
        }
        set(value) = prefs.edit().putString(KEY_METHOD, value?.name).apply()

    var lastLatitude: Double?
        get() = if (prefs.contains(KEY_LAT)) prefs.getFloat(KEY_LAT, 0f).toDouble() else null
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_LAT) else putFloat(KEY_LAT, value.toFloat())
        }.apply()

    var lastLongitude: Double?
        get() = if (prefs.contains(KEY_LNG)) prefs.getFloat(KEY_LNG, 0f).toDouble() else null
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_LNG) else putFloat(KEY_LNG, value.toFloat())
        }.apply()

    var isManualLocation: Boolean
        get() = prefs.getBoolean(KEY_MANUAL, false)
        set(value) = prefs.edit().putBoolean(KEY_MANUAL, value).apply()

    /**
     * Nama tempat hasil geocoding, disimpan bersama koordinat yang menghasilkannya. Dibandingkan
     * dengan pembulatan 2 desimal (~1 km) — cukup untuk menganggap "masih di tempat yang sama",
     * sekaligus mencegah nama basi saat user benar-benar berpindah kota.
     */
    fun cachedPlaceNameFor(latitude: Double, longitude: Double): String? {
        val name = prefs.getString(KEY_PLACE_NAME, null) ?: return null
        val savedKey = prefs.getString(KEY_PLACE_COORD, null) ?: return null
        return if (savedKey == coordKey(latitude, longitude)) name else null
    }

    fun savePlaceName(latitude: Double, longitude: Double, name: String) {
        prefs.edit()
            .putString(KEY_PLACE_NAME, name)
            .putString(KEY_PLACE_COORD, coordKey(latitude, longitude))
            .apply()
    }

    private fun coordKey(latitude: Double, longitude: Double): String =
        "%.2f,%.2f".format(Locale.US, latitude, longitude)

    fun saveLocation(latitude: Double, longitude: Double, manual: Boolean) {
        lastLatitude = latitude
        lastLongitude = longitude
        isManualLocation = manual
    }

    /**
     * Notifikasi per-waktu sholat, default aktif semua. Key pakai [Prayer.name] (mis. "FAJR")
     * — bukan label yang ditampilkan ke user — supaya tidak rusak kalau bahasa aplikasi
     * (jadi label tampilan) berubah. Lihat juga PrayerAlarmScheduler.
     */
    fun isNotificationEnabled(prayer: Prayer): Boolean =
        prefs.getBoolean(KEY_NOTIF_PREFIX + prayer.name, true)

    fun setNotificationEnabled(prayer: Prayer, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_PREFIX + prayer.name, enabled).apply()
    }

    /**
     * Koreksi menit per waktu sholat (boleh negatif). Perhitungan astronomis bisa berbeda satu-dua
     * menit dari jadwal masjid setempat — yang lazim memakai ihtiyath (waktu pengaman) atau
     * pembulatan tersendiri. Daripada memaksa user memilih antara aplikasi dan masjidnya, selisih
     * itu bisa disetel sendiri di sini.
     */
    fun offsetMinutes(prayer: Prayer): Int = prefs.getInt(KEY_OFFSET_PREFIX + prayer.name, 0)

    fun setOffsetMinutes(prayer: Prayer, minutes: Int) {
        prefs.edit().putInt(KEY_OFFSET_PREFIX + prayer.name, minutes).apply()
    }

    fun hasAnyOffset(): Boolean =
        Prayer.entries.any { it != Prayer.NONE && offsetMinutes(it) != 0 }

    var notificationStyle: NotificationStyle
        get() = prefs.getString(KEY_NOTIF_STYLE, null)?.let { name ->
            runCatching { NotificationStyle.valueOf(name) }.getOrNull()
        } ?: NotificationStyle.SOUND
        set(value) = prefs.edit().putString(KEY_NOTIF_STYLE, value.name).apply()

    /**
     * URI nada yang dipilih user lewat pemilih nada bawaan Android; null = nada notifikasi
     * default sistem. Sengaja TIDAK membundel berkas adzan di APK — audio adzan hampir selalu
     * punya pemilik hak cipta, jadi user memilih sendiri dari koleksi nada di HP-nya.
     */
    var notificationSoundUri: String?
        get() = prefs.getString(KEY_NOTIF_SOUND, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_NOTIF_SOUND) else putString(KEY_NOTIF_SOUND, value)
        }.apply()

    /** Sumber suara adzan; default memakai rekaman adzan bawaan yang dibundel (CC0). */
    var adhanSoundSource: AdhanSoundSource
        get() = prefs.getString(KEY_ADHAN_SOURCE, null)?.let { name ->
            runCatching { AdhanSoundSource.valueOf(name) }.getOrNull()
        } ?: AdhanSoundSource.BUNDLED_ADHAN
        set(value) = prefs.edit().putString(KEY_ADHAN_SOURCE, value.name).apply()

    /** 0 = tidak ada pengingat sebelum waktu sholat. */
    var reminderMinutesBefore: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTES, 0)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTES, value).apply()

    /**
     * Sejak Android 8, suara & getar sebuah notification channel TIDAK bisa diubah setelah
     * channel dibuat. Supaya perubahan setelan benar-benar berlaku, channel lama dihapus dan
     * channel baru dibuat dengan id ber-versi — nomor versi inilah yang disimpan di sini.
     */
    var channelVersion: Int
        get() = prefs.getInt(KEY_CHANNEL_VERSION, 0)
        set(value) = prefs.edit().putInt(KEY_CHANNEL_VERSION, value).apply()

    private companion object {
        const val KEY_METHOD = "calculation_method"
        const val KEY_NOTIF_STYLE = "notif_style"
        const val KEY_NOTIF_SOUND = "notif_sound_uri"
        const val KEY_ADHAN_SOURCE = "adhan_sound_source"
        const val KEY_REMINDER_MINUTES = "reminder_minutes_before"
        const val KEY_CHANNEL_VERSION = "notif_channel_version"
        const val KEY_PLACE_NAME = "place_name"
        const val KEY_PLACE_COORD = "place_name_coord"
        const val KEY_LAT = "last_lat"
        const val KEY_LNG = "last_lng"
        const val KEY_MANUAL = "is_manual_location"
        const val KEY_NOTIF_PREFIX = "notif_enabled_"
        const val KEY_OFFSET_PREFIX = "offset_minutes_"
    }
}
