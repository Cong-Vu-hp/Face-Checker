package com.attendance.app.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.attendance.app.data.local.entity.Attendance
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun exportToCsv(attendanceList: List<Attendance>): File? {
        if (attendanceList.isEmpty()) return null

        return try {
            val date = attendanceList.firstOrNull()?.date?.replace("/", "-") ?: "export"
            val fileName = "diem_danh_$date.csv"

            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            FileWriter(file).use { writer ->
                writer.append("STT,Mã học sinh,Họ và tên,Lớp,Ngày,Giờ,Độ chính xác\n")

                attendanceList.forEachIndexed { index, attendance ->
                    writer.append("${index + 1},")
                    writer.append("${attendance.studentId},")
                    writer.append("${attendance.studentName},")
                    writer.append("${attendance.className},")
                    writer.append("${attendance.date},")
                    writer.append("${attendance.time},")
                    writer.append("${String.format("%.1f%%", attendance.confidence * 100)}\n")
                }
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareCsvFile(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}