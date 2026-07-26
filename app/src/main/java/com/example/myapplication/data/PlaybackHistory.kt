package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val mediaTitle: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
