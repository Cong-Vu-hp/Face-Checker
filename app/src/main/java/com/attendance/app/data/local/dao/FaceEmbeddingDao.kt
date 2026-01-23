package com.attendance.app.data.local.dao

import androidx.room.*
import com.attendance.app.data.local.entity.FaceEmbedding

@Dao
interface FaceEmbeddingDao {

    @Query("SELECT * FROM face_embeddings")
    suspend fun getAllEmbeddings(): List<FaceEmbedding>

    @Query("SELECT * FROM face_embeddings WHERE studentId = :studentId")
    suspend fun getEmbeddingByStudentId(studentId: String): FaceEmbedding?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: FaceEmbedding)

    @Delete
    suspend fun deleteEmbedding(embedding: FaceEmbedding)

    @Query("DELETE FROM face_embeddings")
    suspend fun deleteAllEmbeddings()
}