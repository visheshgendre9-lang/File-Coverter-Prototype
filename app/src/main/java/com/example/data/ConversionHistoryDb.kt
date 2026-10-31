package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversion_history")
data class ConversionEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val inputFormat: String,
    val targetFormat: String,
    val originalSizeBytes: Long,
    val resultSizeBytes: Long,
    val durationMs: Long,
    val resultPath: String?,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ConversionDao {
    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ConversionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ConversionEntity)

    @Query("DELETE FROM conversion_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM conversion_history")
    suspend fun clearAll()
}

@Database(entities = [ConversionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversionDao(): ConversionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omniconvert_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class HistoryRepository(private val dao: ConversionDao) {
    val allHistory: Flow<List<ConversionEntity>> = dao.getAllHistory()

    suspend fun recordConversion(record: ConversionEntity) = dao.insert(record)
    suspend fun deleteRecord(id: String) = dao.deleteById(id)
    suspend fun clearHistory() = dao.clearAll()
}
