package com.attendance.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "face_embeddings")
data class FaceEmbedding(
    @PrimaryKey
    val studentId: String,
    val embedding: FloatArray,
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FaceEmbedding
        return studentId == other.studentId
    }

    override fun hashCode(): Int = studentId.hashCode()
}