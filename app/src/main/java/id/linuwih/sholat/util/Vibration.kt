package id.linuwih.sholat.util

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Setelan getar milik aplikasi sendiri, terpisah dari setelan haptic bawaan sistem. */
class HapticPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("haptic_prefs", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    private companion object {
        const val KEY_ENABLED = "haptic_enabled"
    }
}

/**
 * Getar sekali — dipakai Tasbih (hitungan mencapai target) & Kiblat (arah sejajar).
 *
 * Perjalanan bug ini ada dua lapis:
 *
 * 1. Awalnya vibrate() dipanggil tanpa atribut usage sama sekali, jadi Android
 *    mengklasifikasikannya USAGE_UNKNOWN — kategori paling gampang diredam sistem.
 * 2. Perbaikan pertama memakai USAGE_TOUCH. Itu masih belum cukup: USAGE_TOUCH terikat pada
 *    setelan "Touch feedback / getar sentuh" bawaan HP. Kalau user mematikan setelan itu
 *    (umum, apalagi di HP yang haptic-nya diatur ketat pabrikan), semua getaran USAGE_TOUCH
 *    ikut dibuang sistem — itulah kenapa Tasbih & Kiblat tetap sunyi walau kodenya jalan.
 *
 * Sekarang memakai USAGE_ALARM, kategori yang TIDAK ikut dimatikan oleh setelan getar-sentuh,
 * sehingga getar di app ini dikendalikan oleh setelan app sendiri ([HapticPrefs]) — sesuai
 * permintaan: mati/hidupnya diatur di sini, bukan menumpang setelan sistem.
 *
 * Batas yang jujur: mode Jangan Ganggu dan penghemat baterai agresif di sebagian ROM masih bisa
 * meredam getaran kelas alarm. Itu di luar kendali aplikasi.
 */
fun vibrateOnce(context: Context, durationMs: Long = 200) {
    if (!HapticPrefs(context).enabled) return

    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    if (!vibrator.hasVibrator()) return

    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, amplitudeFor(vibrator)),
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
            // Overload ini deprecated di Java sejak API 33 (digantikan VibrationAttributes di atas),
            // tapi untuk rentang API 26-32 ini SATU-SATUNYA overload non-legacy yang ada — bukan
            // pilihan yang bisa dihindari, sama seperti cabang legacy di bawah.
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, amplitudeFor(vibrator)),
                alarmAudioAttributes(),
            )
        }
        else -> {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs, alarmAudioAttributes())
        }
    }
}

/**
 * Pakai amplitudo penuh kalau perangkat mendukung pengaturan kekuatan getar. DEFAULT_AMPLITUDE
 * menyerahkan kekuatan ke setelan bawaan, yang di sebagian HP sangat lemah sampai nyaris tidak
 * terasa untuk getaran sesingkat ini.
 */
private fun amplitudeFor(vibrator: Vibrator): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl()) {
        255
    } else {
        VibrationEffect.DEFAULT_AMPLITUDE
    }

private fun alarmAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ALARM)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()
