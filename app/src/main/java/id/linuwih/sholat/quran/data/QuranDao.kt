package id.linuwih.sholat.quran.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Query("SELECT * FROM last_read WHERE id = 0")
    fun observeLastRead(): Flow<LastReadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLastRead(entity: LastReadEntity)

    @Query("SELECT * FROM bookmark ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookmark(entity: BookmarkEntity)

    @Delete
    suspend fun removeBookmark(entity: BookmarkEntity)
}
