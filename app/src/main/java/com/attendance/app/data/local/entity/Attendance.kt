package com.attendance.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: String,
    val studentName: String,
    val className: String,
    val date: String,
    val time: String,
    val timestamp: Long,
    val photoPath: String? = null,
    val confidence: Float
)