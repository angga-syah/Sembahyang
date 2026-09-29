package id.linuwih.sholat.ibadah.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Sengaja cuma item yang statusnya BOOLEAN sederhana (sudah/belum) — bukan penghitung (tilawah
 * berapa ayat, dzikir berapa kali), supaya checklist ini tetap cepat diisi tiap hari, bukan jadi
 * beban input lagi. Urutan enum menentukan urutan tampil di layar.
 */
enum class AmalHarianItem {
    SUBUH, DZUHUR, ASHAR, MAGHRIB, ISYA,
    DZIKIR_PAGI, DZIKIR_PETANG,
    TILAWAH,
    SEDEKAH,
}

/** Satu baris per (tanggal, item) — sama seperti [FastingLogEntity], epoch day bukan timestamp. */
@Entity(tableName = "amal_harian_log", primaryKeys = ["epochDay", "item"])
data class AmalHarianEntity(
    val epochDay: Long,
    val item: String,
    val checked: Boolean,
)

@Dao
interface AmalHarianDao {
    @Query("SELECT * FROM amal_harian_log WHERE epochDay = :epochDay")
    fun observeForDay(epochDay: Long): Flow<List<AmalHarianEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AmalHarianEntity)
}
