package id.linuwih.sholat.prayer.data

import android.content.Context
import com.batoulapps.adhan2.Prayer
import id.linuwih.sholat.R

/**
 * Label tampilan untuk [Prayer] — dipisah dari identitas enum (yang dipakai sebagai key
 * penyimpanan/penjadwalan) supaya aman kalau bahasa aplikasi berganti (ikut bahasa sistem).
 * Bisa dipanggil dari mana saja yang punya Context (ViewModel, BroadcastReceiver, Compose).
 */
object PrayerLabels {
    fun label(context: Context, prayer: Prayer): String = when (prayer) {
        Prayer.FAJR -> context.getString(R.string.prayer_fajr)
        Prayer.SUNRISE -> context.getString(R.string.prayer_sunrise)
        Prayer.DHUHR -> context.getString(R.string.prayer_dhuhr)
        Prayer.ASR -> context.getString(R.string.prayer_asr)
        Prayer.MAGHRIB -> context.getString(R.string.prayer_maghrib)
        Prayer.ISHA -> context.getString(R.string.prayer_isha)
        Prayer.NONE -> ""
    }
}
