package id.linuwih.sholat.ibadah.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.linuwih.sholat.ibadah.data.AmalHarianEntity
import id.linuwih.sholat.ibadah.data.AmalHarianItem
import id.linuwih.sholat.ibadah.data.IbadahDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class AmalHarianViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = IbadahDatabase.getInstance(application).amalHarianDao()

    // Sama seperti PuasaViewModel: epoch day (YYYYMMDD), bukan timestamp -- satu baris cukup
    // per hari kalender, tidak peduli jam berapa dicentangnya.
    val todayEpochDay: Long = localDateKey(Calendar.getInstance())

    /** Set item yang sudah dicentang HARI INI — dipakai UI untuk state checkbox. */
    val checkedToday: StateFlow<Set<AmalHarianItem>> = dao.observeForDay(todayEpochDay)
        .map { rows ->
            rows.filter { it.checked }
                .mapNotNull { row -> runCatching { AmalHarianItem.valueOf(row.item) }.getOrNull() }
                .toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggle(item: AmalHarianItem, checked: Boolean) {
        viewModelScope.launch {
            dao.upsert(AmalHarianEntity(epochDay = todayEpochDay, item = item.name, checked = checked))
        }
    }

    private fun localDateKey(calendar: Calendar): Long {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return year * 10000L + month * 100L + day
    }
}
