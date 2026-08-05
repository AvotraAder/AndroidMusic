package com.example.myapplication.data

import androidx.room.*

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
    suspend fun insertHistory(history: PlaybackHistory)

    @Query("SELECT mediaTitle, COUNT(*) as count FROM playback_history WHERE username = :username GROUP BY mediaTitle ORDER BY count DESC LIMIT 5")
    suspend fun getTopMedias(username: String): List<TopMedia>

    @Query("SELECT * FROM playback_history WHERE username = :username ORDER BY timestamp DESC LIMIT 5")
    suspend fun getRecentHistory(username: String): List<PlaybackHistory>

    @Query("SELECT SUM(durationMs) FROM playback_history WHERE username = :username")
    suspend fun getTotalListenTime(username: String): Long?

    @Query("SELECT artist, COUNT(*) as count FROM playback_history WHERE username = :username GROUP BY artist ORDER BY count DESC LIMIT 5")
    suspend fun getTopArtists(username: String): List<TopArtist>

    @Query("SELECT COUNT(*) FROM playback_history WHERE username = :username AND timestamp >= :todayStart")
    suspend fun getPlaysToday(username: String, todayStart: Long): Int
}

data class TopMedia(val mediaTitle: String, val count: Int)
data class TopArtist(val artist: String, val count: Int)
