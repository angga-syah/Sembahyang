package id.linuwih.sholat.prayer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.batoulapps.adhan2.Prayer
import id.linuwih.sholat.R
import id.linuwih.sholat.prayer.data.PrayerCalculationMethod
import id.linuwih.sholat.prayer.data.PrayerLabels
import id.linuwih.sholat.prayer.data.PrayerPrefs
import id.linuwih.sholat.prayer.data.PrayerTimesFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Widget home screen sederhana pakai RemoteViews klasik (bukan Glance) — supaya tidak
 * nambah dependency baru, sesuai prinsip "ringan" (lihat plan). Isi widget dihitung dari
 * lokasi terakhir tersimpan (PrayerPrefs), sama seperti PrayerAlarmScheduler.
 */
class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    companion object {
        /** Dipanggil dari PrayerAlarmReceiver (tiap prayer alarm/reschedule) & PrayerViewModel
         * (tiap kali lokasi/metode baru dihitung) supaya widget selalu segar tanpa menunggu
         * updatePeriodMillis (minimum dipaksa 30 menit oleh Android). */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))
            ids.forEach { id -> updateWidget(context, manager, id) }
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_prayer)
            val result = computeNextPrayer(context)
            if (result != null) {
                views.setTextViewText(R.id.widget_prayer_name, result.label)
                views.setTextViewText(R.id.widget_prayer_time, result.timeText)
                views.setTextViewText(R.id.widget_prayer_countdown, result.countdownText)
            } else {
                views.setTextViewText(R.id.widget_prayer_name, context.getString(R.string.widget_open_app))
                views.setTextViewText(R.id.widget_prayer_time, context.getString(R.string.widget_detect_location))
                views.setTextViewText(R.id.widget_prayer_countdown, "")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private data class WidgetContent(val label: String, val timeText: String, val countdownText: String)

        /**
         * Hitung mundur di widget dihitung saat widget di-update, bukan berdetak sendiri —
         * widget tidak boleh punya timer sendiri (boros baterai, dan Android membatasi
         * updatePeriodMillis minimum 30 menit). Widget disegarkan lewat updateAll() yang
         * dipanggil tiap alarm sholat, reschedule tengah malam, boot, dan tiap kali app
         * menghitung ulang jadwal — jadi angkanya bisa agak tertinggal di antara dua titik itu.
         */
        private fun formatCountdown(context: Context, remainingMillis: Long): String {
            if (remainingMillis <= 0) return context.getString(R.string.prayer_countdown_now)
            val totalMinutes = remainingMillis / 60_000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return when {
                totalMinutes < 1 -> context.getString(R.string.prayer_countdown_soon)
                hours > 0 -> context.getString(R.string.prayer_countdown_hm, hours, minutes)
                else -> context.getString(R.string.prayer_countdown_m, minutes)
            }
        }

        private fun computeNextPrayer(context: Context): WidgetContent? {
            val prefs = PrayerPrefs(context)
            val lat = prefs.lastLatitude ?: return null
            val lng = prefs.lastLongitude ?: return null
            val method = prefs.calculationMethod ?: PrayerCalculationMethod.defaultFor(lat, lng)

            val now = kotlin.time.Clock.System.now()
            val todayTimes = PrayerTimesFactory.forToday(context, lat, lng, method)
            val next = todayTimes.nextPrayer(now)

            val (label, instant) = if (next == Prayer.NONE) {
                val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomorrowTimes = PrayerTimesFactory.forDate(context, lat, lng, method, tomorrow)
                PrayerLabels.label(context, Prayer.FAJR) to tomorrowTimes.fajr
            } else {
                // Tanpa !! — kode ini berjalan di dalam BroadcastReceiver, dan pengecualian di
                // sini bukan sekadar layar kosong melainkan crash proses yang terlihat user.
                val instant = todayTimes.timeForPrayer(next) ?: return null
                PrayerLabels.label(context, next) to instant
            }

            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() }
            val remaining = instant.toEpochMilliseconds() - now.toEpochMilliseconds()
            return WidgetContent(
                label = label,
                timeText = formatter.format(Date(instant.toEpochMilliseconds())),
                countdownText = formatCountdown(context, remaining),
            )
        }
    }
}
