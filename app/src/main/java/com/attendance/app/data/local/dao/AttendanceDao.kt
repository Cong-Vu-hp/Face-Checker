package com.attendance.app.data.local.dao

import androidx.room.*
import com.attendance.app.data.local.entity.Attendance
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Query("SELECT * FROM attendance ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE date = :date ORDER BY timestamp DESC")
    fun getAttendanceByDate(date: String): Flow<List<Attendance>>

    @Query("SELECT COUNT(*) FROM attendance WHERE date = :date")
    fun getAttendanceCountByDate(date: String): Flow<Int>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId AND date = :date")
    suspend fun getAttendanceByStudentAndDate(studentId: String, date: String): Attendance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Delete
    suspend fun deleteAttendance(attendance: Attendance)

    @Query("DELETE FROM attendance")
    suspend fun deleteAllAttendance()
}