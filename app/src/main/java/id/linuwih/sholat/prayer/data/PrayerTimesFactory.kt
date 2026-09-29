package id.linuwih.sholat.prayer.data

import android.content.Context
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Prayer
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import java.util.Calendar

/**
 * Titik tunggal untuk membangun PrayerTimes, dipakai baik oleh UI (PrayerViewModel) maupun
 * scheduler notifikasi di background (PrayerAlarmScheduler) supaya hasilnya selalu konsisten.
 *
 * Koreksi menit milik user juga diterapkan DI SINI, bukan di masing-masing pemanggil. Kalau
 * diterapkan terpisah, satu pemanggil yang terlewat akan membuat jam yang tampil di layar berbeda
 * dari jam adzan yang berbunyi — kesalahan yang jauh lebih membingungkan daripada tidak ada fitur
 * koreksi sama sekali. Memakai PrayerAdjustments bawaan adhan2, jadi koreksinya sudah terhitung
 * sebelum PrayerTimes terbentuk.
 */
object PrayerTimesFactory {

    fun forToday(
        context: Context,
        latitude: Double,
        longitude: Double,
        method: PrayerCalculationMethod,
    ): PrayerTimes = forDate(context, latitude, longitude, method, Calendar.getInstance())

    fun forDate(
        context: Context,
        latitude: Double,
        longitude: Double,
        method: PrayerCalculationMethod,
        calendar: Calendar,
    ): PrayerTimes {
        val prefs = PrayerPrefs(context)
        val coordinates = Coordinates(latitude, longitude)
        val date = DateComponents(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
        )
        // Ihtiyat baku metode (lihat PrayerCalculationMethod.defaultIhtiyat) DIJUMLAHKAN dengan
        // koreksi pribadi user, bukan ditimpa — keduanya lapisan berbeda: yang pertama membuat
        // hisabnya cocok dengan standar resmi, yang kedua urusan selisih ke jadwal masjid
        // setempat. Menimpa salah satunya akan membuat yang lain hilang diam-diam.
        val ihtiyat = method.defaultIhtiyat()
        val parameters = method.toCalculationParameters().copy(
            prayerAdjustments = PrayerAdjustments(
                fajr = ihtiyat.fajr + prefs.offsetMinutes(Prayer.FAJR),
                sunrise = ihtiyat.sunrise + prefs.offsetMinutes(Prayer.SUNRISE),
                dhuhr = ihtiyat.dhuhr + prefs.offsetMinutes(Prayer.DHUHR),
                asr = ihtiyat.asr + prefs.offsetMinutes(Prayer.ASR),
                maghrib = ihtiyat.maghrib + prefs.offsetMinutes(Prayer.MAGHRIB),
                isha = ihtiyat.isha + prefs.offsetMinutes(Prayer.ISHA),
            ),
        )
        return PrayerTimes(coordinates, date, parameters)
    }
}
