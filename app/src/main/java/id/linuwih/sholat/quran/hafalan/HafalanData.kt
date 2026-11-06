package id.linuwih.sholat.quran.hafalan

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Satu baris = satu ayat yang sudah ditandai hafal. Ayat yang belum hafal tidak punya baris,
 * jadi status surah (Belum/Sedang/Hafal) cukup diturunkan dari jumlah baris per surah.
 *
 * Kolom murajaah (spaced repetition): [reviewStage] menunjuk ke [REVIEW_INTERVAL_DAYS], dan
 * [nextReviewDay] adalah hari (epoch day, zona waktu lokal) ayat ini jatuh tempo diulang.
 */
@Entity(tableName = "hafalan_ayah", primaryKeys = ["surahNumber", "ayahNumber"])
data class HafalanAyahEntity(
    val surahNumber: Int,
    val ayahNumber: Int,
    val memorizedAt: Long,
    @ColumnInfo(defaultValue = "0") val memorizedDay: Long,
    @ColumnInfo(defaultValue = "0") val reviewStage: Int,
    @ColumnInfo(defaultValue = "0") val nextReviewDay: Long,
)

/** Hari-hari user berlatih (menghafal baru atau murajaah) - dasar hitungan hari beruntun. */
@Entity(tableName = "hafalan_day")
data class HafalanDayEntity(@PrimaryKey val epochDay: Long)

/**
 * Jarak murajaah dalam hari per tahap. Lancar naik satu tahap, Ragu tetap di tahap yang
 * sama (jarak dipersingkat), Lupa kembali ke tahap 0 (diulang besok).
 */
val REVIEW_INTERVAL_DAYS = listOf(1, 3, 7, 14, 30, 60)

enum class ReviewRating { LANCAR, RAGU, LUPA }

data class SurahMemorizedCount(val surahNumber: Int, val count: Int)

@Dao
interface HafalanDao {
    @Query("SELECT surahNumber, COUNT(*) AS count FROM hafalan_ayah GROUP BY surahNumber")
    fun observeCounts(): Flow<List<SurahMemorizedCount>>

    @Query("SELECT ayahNumber FROM hafalan_ayah WHERE surahNumber = :surah")
    fun observeMemorized(surah: Int): Flow<List<Int>>

    @Query("SELECT surahNumber, COUNT(*) AS count FROM hafalan_ayah WHERE nextReviewDay <= :today GROUP BY surahNumber")
    fun observeDueCounts(today: Long): Flow<List<SurahMemorizedCount>>

    @Query("SELECT * FROM hafalan_ayah WHERE surahNumber = :surah AND nextReviewDay <= :today ORDER BY ayahNumber")
    suspend fun dueAyahs(surah: Int, today: Long): List<HafalanAyahEntity>

    @Query("SELECT * FROM hafalan_ayah WHERE surahNumber = :surah AND ayahNumber IN (:ayahs)")
    suspend fun get(surah: Int, ayahs: List<Int>): List<HafalanAyahEntity>

    @Query("SELECT COUNT(*) FROM hafalan_ayah WHERE nextReviewDay <= :today")
    suspend fun dueCount(today: Long): Int

    @Query("SELECT COUNT(*) FROM hafalan_ayah WHERE memorizedDay = :day")
    fun observeMemorizedOn(day: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM hafalan_ayah WHERE memorizedDay = :day")
    suspend fun memorizedOn(day: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<HafalanAyahEntity>)

    @Update
    suspend fun updateAll(entities: List<HafalanAyahEntity>)

    @Query("DELETE FROM hafalan_ayah WHERE surahNumber = :surah AND ayahNumber IN (:ayahs)")
    suspend fun delete(surah: Int, ayahs: List<Int>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markDay(day: HafalanDayEntity)

    @Query("SELECT epochDay FROM hafalan_day ORDER BY epochDay DESC LIMIT 400")
    fun observeDays(): Flow<List<Long>>
}

/** Hari beruntun yang berakhir hari ini (atau kemarin, kalau hari ini belum latihan). */
fun streakFrom(daysDesc: List<Long>, today: Long): Int {
    if (daysDesc.isEmpty()) return 0
    var expected = if (daysDesc.first() == today) today else today - 1
    var streak = 0
    for (day in daysDesc) {
        if (day == expected) {
            streak++
            expected--
        } else if (day < expected) {
            break
        }
    }
    return streak
}

/** Jadwal berikutnya untuk satu ayat setelah dinilai. */
fun HafalanAyahEntity.reviewed(rating: ReviewRating, today: Long): HafalanAyahEntity {
    val maxStage = REVIEW_INTERVAL_DAYS.lastIndex
    val stage = when (rating) {
        ReviewRating.LANCAR -> (reviewStage + 1).coerceAtMost(maxStage)
        ReviewRating.RAGU -> reviewStage
        ReviewRating.LUPA -> 0
    }
    val days = when (rating) {
        ReviewRating.LUPA -> 1
        ReviewRating.RAGU -> maxOf(1, REVIEW_INTERVAL_DAYS[stage] / 2)
        ReviewRating.LANCAR -> REVIEW_INTERVAL_DAYS[stage]
    }
    return copy(reviewStage = stage, nextReviewDay = today + days)
}

/**
 * Database terpisah dari quran_local.db (penanda baca & bookmark) supaya fitur hafalan tidak
 * memerlukan migrasi skema database lama - risiko kehilangan bookmark user nol.
 */
@Database(entities = [HafalanAyahEntity::class, HafalanDayEntity::class], version = 2, exportSchema = false)
abstract class HafalanDatabase : RoomDatabase() {
    abstract fun dao(): HafalanDao

    companion object {
        @Volatile private var instance: HafalanDatabase? = null

        /** v1 -> v2: kolom murajaah + tabel hari latihan. Ayat lama dijadwalkan murajaah besok. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hafalan_ayah ADD COLUMN memorizedDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hafalan_ayah ADD COLUMN reviewStage INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hafalan_ayah ADD COLUMN nextReviewDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE hafalan_ayah SET memorizedDay = memorizedAt / 86400000, " +
                        "nextReviewDay = memorizedAt / 86400000 + 1",
                )
                db.execSQL("CREATE TABLE IF NOT EXISTS `hafalan_day` (`epochDay` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))")
                // Hari-hari menghafal yang sudah tercatat ikut dihitung ke hari beruntun.
                db.execSQL("INSERT OR IGNORE INTO hafalan_day (epochDay) SELECT DISTINCT memorizedDay FROM hafalan_ayah")
            }
        }

        fun getInstance(context: Context): HafalanDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HafalanDatabase::class.java,
                    "hafalan.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}

/** Hari ini sebagai epoch day zona waktu lokal - satuan semua jadwal murajaah. */
fun todayEpochDay(): Long = java.time.LocalDate.now().toEpochDay()

/** Cakupan daftar hafalan. Juz Amma = surah 78–114 (juz 30 tepat dimulai di 78:1). */
enum class HafalanScope { JUZ_AMMA, SEMUA }

/** Jeda setelah tiap ayat untuk menirukan, sebagai kelipatan durasi ayat itu sendiri. */
enum class RepeatGap(val factor: Float) { MATI(0f), SATU_KALI(1f), SATU_SETENGAH(1.5f) }

const val JUZ_AMMA_FIRST_SURAH = 78

/** 0 = ulangi terus sampai dihentikan. */
val AYAH_REPEAT_OPTIONS = listOf(1, 3, 5, 7, 10)
val RANGE_REPEAT_OPTIONS = listOf(1, 2, 3, 5, 0)
val SPEED_OPTIONS = listOf(0.75f, 1f, 1.25f)
val DAILY_TARGET_OPTIONS = listOf(1, 2, 3, 5, 10)
val REMINDER_HOUR_OPTIONS = listOf(5, 6, 7, 8, 12, 16, 19, 20, 21)

/**
 * Tingkat penyembunyian teks Arab di mode uji. Kata yang tersembunyi dibuka satu per satu
 * dengan mengetuk ayatnya - seperti menyetor ke guru yang memberi petunjuk seperlunya.
 */
enum class TestLevel {
    /** Tiap kata hanya tampil huruf pertamanya. */
    HURUF_AWAL,

    /** Hanya kata pertama ayat yang tampil. */
    TERSEMBUNYI,
}

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

    /** Target ayat baru per hari. */
    var dailyTarget: Int
        get() = prefs.getInt(KEY_DAILY_TARGET, 3).takeIf { it in DAILY_TARGET_OPTIONS } ?: 3
        set(value) = prefs.edit().putInt(KEY_DAILY_TARGET, value).apply()

    /** Jam pengingat harian (0-23), -1 = mati. */
    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, -1).takeIf { it == -1 || it in REMINDER_HOUR_OPTIONS } ?: -1
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply()

    var testLevel: TestLevel
        get() = enumOr(prefs.getString(KEY_TEST_LEVEL, null), TestLevel.HURUF_AWAL)
        set(value) = prefs.edit().putString(KEY_TEST_LEVEL, value.name).apply()

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
        const val KEY_DAILY_TARGET = "daily_target"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_TEST_LEVEL = "test_level"
    }
}
