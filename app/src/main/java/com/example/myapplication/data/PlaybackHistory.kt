package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_history",
    indices = [
        Index(value = ["username"]),
        Index(value = ["timestamp"]),
        Index(value = ["username", "mediaTitle"]),
        Index(value = ["username", "artist"])
    ]
)
data class PlaybackHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val mediaTitle: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
