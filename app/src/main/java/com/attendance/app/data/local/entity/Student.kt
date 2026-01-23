package com.attendance.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey
    val id: String,
    val name: String,
    val className: String,
    val photoPath: String,
    val createdAt: Long = System.currentTimeMillis()
)