package com.example.clipboard

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, timestamp DESC")
    fun getAllClips(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, timestamp DESC LIMIT 40")
    fun getRecentClips(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentClip(): ClipboardItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItem): Long

    @Query("SELECT * FROM clipboard_items WHERE text = :text LIMIT 1")
    suspend fun findByText(text: String): ClipboardItem?

    @Update
    suspend fun update(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0 AND timestamp < :cutoffTime")
    suspend fun deleteOldUnpinned(cutoffTime: Long): Int

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0")
    suspend fun clearUnpinned(): Int
}
