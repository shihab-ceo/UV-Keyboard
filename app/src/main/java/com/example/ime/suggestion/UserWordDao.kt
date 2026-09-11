package com.example.ime.suggestion

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserWordDao {
    @Query("SELECT * FROM user_words WHERE word = :word LIMIT 1")
    suspend fun getWord(word: String): UserWordEntity?

    @Query("SELECT * FROM user_words WHERE word LIKE :prefix || '%' ORDER BY frequency DESC, lastUsedTimestamp DESC LIMIT :limit")
    suspend fun getWordsMatchingPrefix(prefix: String, limit: Int = 10): List<UserWordEntity>

    @Query("SELECT * FROM user_words ORDER BY frequency DESC, lastUsedTimestamp DESC LIMIT :limit")
    suspend fun getTopWords(limit: Int = 50): List<UserWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UserWordEntity): Long

    @Query("DELETE FROM user_words")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM user_words")
    suspend fun getCount(): Int
}
