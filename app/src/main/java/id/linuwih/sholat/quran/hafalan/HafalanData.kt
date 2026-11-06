package id.linuwih.sholat.quran.hafalan

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Satu baris = satu ayat yang sudah ditandai hafal. Ayat yang belum hafal tidak punya baris,
 * jadi status surah (Belum/Sedang/Hafal) cukup diturunkan dari jumlah baris per surah.
 */
@Entity(tableName = "hafalan_ayah", primaryKeys = ["surahNumber", "ayahNumber"])
data class HafalanAyahEntity(
    val surahNumber: Int,
    val ayahNumber: Int,
    val memorizedAt: Long,
)

data class SurahMemorizedCount(val surahNumber: Int, val count: Int)

@Dao
interface HafalanDao {
    @Query("SELECT surahNumber, COUNT(*) AS count FROM hafalan_ayah GROUP BY surahNumber")
    fun observeCounts(): Flow<List<SurahMemorizedCount>>

    @Query("SELECT ayahNumber FROM hafalan_ayah WHERE surahNumber = :surah")
    fun observeMemorized(surah: Int): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<HafalanAyahEntity>)

    @Query("DELETE FROM hafalan_ayah WHERE surahNumber = :surah AND ayahNumber IN (:ayahs)")
    suspend fun delete(surah: Int, ayahs: List<Int>)
}

/**
 * Database terpisah dari quran_local.db (penanda baca & bookmark) supaya fitur hafalan tidak
 * memerlukan migrasi skema database lama — risiko kehilangan bookmark user nol.
 */
@Database(entities = [HafalanAyahEntity::class], version = 1, exportSchema = false)
abstract class HafalanDatabase : RoomDatabase() {
    abstract fun dao(): HafalanDao

    companion object {
        @Volatile private var instance: HafalanDatabase? = null

        fun getInstance(context: Context): HafalanDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HafalanDatabase::class.java,
                    "hafalan.db",
                ).build().also { instance = it }
            }
    }
}

/** Cakupan daftar hafalan. Juz Amma = surah 78–114 (juz 30 tepat dimulai di 78:1). */
enum class HafalanScope { JUZ_AMMA, SEMUA }

/** Jeda setelah tiap ayat untuk menirukan, sebagai kelipatan durasi ayat itu sendiri. */
enum class RepeatGap(val factor: Float) { MATI(0f), SATU_KALI(1f), SATU_SETENGAH(1.5f) }

const val JUZ_AMMA_FIRST_SURAH = 78

/** 0 = ulangi terus sampai dihentikan. */
val AYAH_REPEAT_OPTIONS = listOf(1, 3, 5, 7, 10)
val RANGE_REPEAT_OPTIONS = listOf(1, 2, 3, 5, 0)
val SPEED_OPTIONS = listOf(0.75f, 1f, 1.25f)

class HafalanPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("hafalan_prefs", Context.MODE_PRIVATE)

    var scope: HafalanScope
        get() = enumOr(prefs.getString(KEY_SCOPE, null), HafalanScope.JUZ_AMMA)
        set(value) = prefs.edit().putString(KEY_SCOPE, value.name).apply()

    /** Juz Amma lazimnya dihafal dari surah terpendek (An-Nas) naik ke atas. */
    var juzAmmaFromEnd: Boolean
        get() = prefs.getBoolean(KEY_FROM_END, true)
        set(value) = prefs.edit().putBoolean(KEY_FROM_END, value).apply()

    var ayahRepeat: Int
        get() = prefs.getInt(KEY_AYAH_REPEAT, 3).takeIf { it in AYAH_REPEAT_OPTIONS } ?: 3
        set(value) = prefs.edit().putInt(KEY_AYAH_REPEAT, value).apply()

    var rangeRepeat: Int
        get() = prefs.getInt(KEY_RANGE_REPEAT, 2).takeIf { it in RANGE_REPEAT_OPTIONS } ?: 2
        set(value) = prefs.edit().putInt(KEY_RANGE_REPEAT, value).apply()

    var gap: RepeatGap
        get() = enumOr(prefs.getString(KEY_GAP, null), RepeatGap.SATU_KALI)
        set(value) = prefs.edit().putString(KEY_GAP, value.name).apply()

    var speed: Float
        get() = prefs.getFloat(KEY_SPEED, 1f).takeIf { it in SPEED_OPTIONS } ?: 1f
        set(value) = prefs.edit().putFloat(KEY_SPEED, value).apply()

    var reciterName: String?
        get() = prefs.getString(KEY_RECITER, null)
        set(value) = prefs.edit().putString(KEY_RECITER, value).apply()

    var showTranslation: Boolean
        get() = prefs.getBoolean(KEY_TRANSLATION, true)
        set(value) = prefs.edit().putBoolean(KEY_TRANSLATION, value).apply()

    var showLatin: Boolean
        get() = prefs.getBoolean(KEY_LATIN, false)
        set(value) = prefs.edit().putBoolean(KEY_LATIN, value).apply()

    /** Rentang terakhir yang dilatih per surah, supaya sesi berikutnya langsung lanjut. */
    fun lastRange(surah: Int): IntRange? {
        val raw = prefs.getString(KEY_RANGE_PREFIX + surah, null) ?: return null
        val parts = raw.split('-').mapNotNull { it.toIntOrNull() }
        return if (parts.size == 2 && parts[0] <= parts[1]) parts[0]..parts[1] else null
    }

    fun setLastRange(surah: Int, range: IntRange) {
        prefs.edit().putString(KEY_RANGE_PREFIX + surah, "${range.first}-${range.last}").apply()
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> runCatching { enumValueOf<E>(n) }.getOrNull() } ?: default

    private companion object {
        const val KEY_SCOPE = "scope"
        const val KEY_FROM_END = "juz_amma_from_end"
        const val KEY_AYAH_REPEAT = "ayah_repeat"
        const val KEY_RANGE_REPEAT = "range_repeat"
        const val KEY_GAP = "gap"
        const val KEY_SPEED = "speed"
        const val KEY_RECITER = "reciter"
        const val KEY_TRANSLATION = "show_translation"
        const val KEY_LATIN = "show_latin"
        const val KEY_RANGE_PREFIX = "range_"
    }
}
