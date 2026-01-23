package com.attendance.app.data.repository

import com.attendance.app.data.local.dao.AttendanceDao
import com.attendance.app.data.local.entity.Attendance
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val attendanceDao: AttendanceDao
) {
    fun getAllAttendance(): Flow<List<Attendance>> = attendanceDao.getAllAttendance()

    fun getAttendanceByDate(date: String): Flow<List<Attendance>> =
        attendanceDao.getAttendanceByDate(date)

    fun getTodayAttendanceCount(date: String): Flow<Int> =
        attendanceDao.getAttendanceCountByDate(date)

    suspend fun checkIfMarkedToday(studentId: String, date: String): Boolean {
        return attendanceDao.getAttendanceByStudentAndDate(studentId, date) != null
    }

    suspend fun insertAttendance(attendance: Attendance) {
        attendanceDao.insertAttendance(attendance)
    }

    suspend fun deleteAllAttendance() {
        attendanceDao.deleteAllAttendance()
    }
}