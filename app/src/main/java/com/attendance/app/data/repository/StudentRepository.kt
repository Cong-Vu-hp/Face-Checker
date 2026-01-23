package com.attendance.app.data.repository

import com.attendance.app.data.local.dao.FaceEmbeddingDao
import com.attendance.app.data.local.dao.StudentDao
import com.attendance.app.data.local.entity.FaceEmbedding
import com.attendance.app.data.local.entity.Student
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepository @Inject constructor(
    private val studentDao: StudentDao,
    private val faceEmbeddingDao: FaceEmbeddingDao
) {
    fun getAllStudents(): Flow<List<Student>> = studentDao.getAllStudents()

    fun getStudentCount(): Flow<Int> = studentDao.getStudentCount()

    suspend fun getStudentById(id: String): Student? = studentDao.getStudentById(id)

    suspend fun insertStudent(student: Student, embedding: FloatArray) {
        studentDao.insertStudent(student)
        faceEmbeddingDao.insertEmbedding(
            FaceEmbedding(studentId = student.id, embedding = embedding)
        )
    }

    suspend fun deleteStudent(student: Student) {
        studentDao.deleteStudent(student)
        faceEmbeddingDao.getEmbeddingByStudentId(student.id)?.let {
            faceEmbeddingDao.deleteEmbedding(it)
        }
    }

    suspend fun deleteAllStudents() {
        studentDao.deleteAllStudents()
        faceEmbeddingDao.deleteAllEmbeddings()
    }

    suspend fun getAllEmbeddings(): List<FaceEmbedding> = faceEmbeddingDao.getAllEmbeddings()
}