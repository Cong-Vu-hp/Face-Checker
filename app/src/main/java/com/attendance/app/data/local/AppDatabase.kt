package com.attendance.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.attendance.app.data.local.dao.AttendanceDao
import com.attendance.app.data.local.dao.FaceEmbeddingDao
import com.attendance.app.data.local.dao.StudentDao
import com.attendance.app.data.local.entity.Attendance
import com.attendance.app.data.local.entity.FaceEmbedding
import com.attendance.app.data.local.entity.Student
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Database(
    entities = [Student::class, FaceEmbedding::class, Attendance::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun faceEmbeddingDao(): FaceEmbeddingDao
    abstract fun attendanceDao(): AttendanceDao
}

class Converters {
    @TypeConverter
    fun fromFloatArray(value: FloatArray): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toFloatArray(value: String): FloatArray {
        val type = object : TypeToken<FloatArray>() {}.type
        return Gson().fromJson(value, type)
    }
}