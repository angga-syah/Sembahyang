package id.linuwih.sholat.ibadah.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.linuwih.sholat.ibadah.data.FastingLogEntity
import id.linuwih.sholat.ibadah.data.FastingType
import id.linuwih.sholat.ibadah.data.HijriDate
import id.linuwih.sholat.ibadah.data.IbadahDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PuasaViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = IbadahDatabase.getInstance(application).fastingDao()
    private val dateFormatter = SimpleDateFormat("EEE, d MMMM yyyy", Locale.getDefault())

    val logs: StateFlow<List<FastingLogEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayEpochDay: Long = localDateKey(Calendar.getInstance())

    fun markToday(type: FastingType) {
        viewModelScope.launch { dao.upsert(FastingLogEntity(todayEpochDay, type.name)) }
    }

    fun unmarkToday() {
        viewModelScope.launch { dao.delete(todayEpochDay) }
    }

    /** Deteksi sederhana: Senin/Kamis, atau tanggal 13-15 Hijriyah (Ayyamul Bidh). */
    fun suggestionForToday(): FastingType? {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.MONDAY || dayOfWeek == Calendar.THURSDAY) return FastingType.SENIN_KAMIS

        val hijri = HijriDate.from(cal)
        if (hijri.day in 13..15) return FastingType.AYYAMUL_BIDH

        return null
    }

    fun countThisMonth(logs: List<FastingLogEntity>): Int {
        val cal = Calendar.getInstance()
        val monthPrefix = (cal.get(Calendar.YEAR) * 10000L) + ((cal.get(Calendar.MONTH) + 1) * 100L)
        return logs.count { it.epochDay in (monthPrefix + 1)..(monthPrefix + 31) }
    }

    fun formatEpochDay(epochDay: Long): String {
        val year = (epochDay / 10000).toInt()
        val month = ((epochDay / 100) % 100).toInt() - 1
        val day = (epochDay % 100).toInt()
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month, day, 12, 0, 0)
        return dateFormatter.format(cal.time)
    }

    /**
     * Key tanggal LOKAL murni (YYYYMMDD, mis. 20260824) — sengaja TIDAK lewat konversi
     * epoch-millis/UTC sama sekali. Percobaan sebelumnya (epoch-day dari timeInMillis lalu
     * diformat ulang pakai timezone lokal) menghasilkan tanggal mundur 1 hari untuk semua
     * timezone UTC+ (termasuk WIB/WITA/WIT) — lihat riwayat commit untuk detail bug-nya.
     */
    private fun localDateKey(calendar: Calendar): Long {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return year * 10000L + month * 100L + day
    }
}
