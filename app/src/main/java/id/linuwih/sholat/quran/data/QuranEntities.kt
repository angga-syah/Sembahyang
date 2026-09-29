package id.linuwih.sholat.quran.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Selalu 1 baris (id tetap 0) — posisi terakhir dibaca. */
@Entity(tableName = "last_read")
data class LastReadEntity(
    @PrimaryKey val id: Int = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val updatedAt: Long,
)

@Entity(tableName = "bookmark", primaryKeys = ["surahNumber", "ayahNumber"])
data class BookmarkEntity(
    val surahNumber: Int,
    val ayahNumber: Int,
    val createdAt: Long,
)
