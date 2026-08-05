package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    // Playback History
    @Insert
    suspend fun insertHistory(history: PlaybackHistory): Long

    @Update
    suspend fun updateHistory(history: PlaybackHistory)

    @Query("UPDATE playback_history SET durationMs = durationMs + :delta WHERE id = :id")
    suspend fun incrementDuration(id: Long, delta: Long)

    @Query("SELECT * FROM playback_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): PlaybackHistory?

    @Query("SELECT mediaTitle, COUNT(*) as count FROM playback_history WHERE username = :username GROUP BY mediaTitle ORDER BY count DESC LIMIT 5")
    fun getTopMedias(username: String): Flow<List<TopMedia>>

    @Query("SELECT * FROM playback_history WHERE username = :username ORDER BY timestamp DESC LIMIT 5")
    fun getRecentHistory(username: String): Flow<List<PlaybackHistory>>

    @Query("SELECT SUM(durationMs) FROM playback_history WHERE username = :username")
    fun getTotalListenTime(username: String): Flow<Long?>

    @Query("SELECT artist, COUNT(*) as count FROM playback_history WHERE username = :username GROUP BY artist ORDER BY count DESC LIMIT 5")
    fun getTopArtists(username: String): Flow<List<TopArtist>>

    @Query("SELECT COUNT(*) FROM playback_history WHERE username = :username AND timestamp >= :todayStart")
    fun getPlaysToday(username: String, todayStart: Long): Flow<Int>
}

data class TopMedia(val mediaTitle: String, val count: Int)
data class TopArtist(val artist: String, val count: Int)
