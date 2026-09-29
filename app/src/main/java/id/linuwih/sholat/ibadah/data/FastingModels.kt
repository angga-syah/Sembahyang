package id.linuwih.sholat.ibadah.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

enum class FastingType {
    SENIN_KAMIS,
    AYYAMUL_BIDH,
    DAUD,
    SYAWAL,
    ARAFAH,
    LAINNYA,
}

/** Satu baris per tanggal (epoch day, bukan timestamp) — cukup satu catatan puasa per hari. */
@Entity(tableName = "fasting_log")
data class FastingLogEntity(
    @PrimaryKey val epochDay: Long,
    val type: String,
)

@Dao
interface FastingDao {
    @Query("SELECT * FROM fasting_log ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<FastingLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FastingLogEntity)

    @Query("DELETE FROM fasting_log WHERE epochDay = :epochDay")
    suspend fun delete(epochDay: Long)
}

/** Menambah tabel amal_harian_log tanpa menyentuh fasting_log — data puasa yang sudah tercatat
 *  user tidak boleh hilang cuma gara-gara fitur baru ditambahkan. */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS amal_harian_log (
                epochDay INTEGER NOT NULL,
                item TEXT NOT NULL,
                checked INTEGER NOT NULL,
                PRIMARY KEY(epochDay, item)
            )
            """.trimIndent(),
        )
    }
}

@Database(
    entities = [FastingLogEntity::class, AmalHarianEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class IbadahDatabase : RoomDatabase() {
    abstract fun fastingDao(): FastingDao
    abstract fun amalHarianDao(): AmalHarianDao

    companion object {
        @Volatile private var instance: IbadahDatabase? = null

        fun getInstance(context: Context): IbadahDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    IbadahDatabase::class.java,
                    "ibadah_local.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
