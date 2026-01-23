package com.attendance.app.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.entity.Attendance
import com.attendance.app.data.local.entity.FaceEmbedding
import com.attendance.app.data.local.entity.Student
import com.attendance.app.data.repository.AttendanceRepository
import com.attendance.app.data.repository.StudentRepository
import com.attendance.app.ml.FaceRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val faceRecognizer: FaceRecognizer
) : ViewModel() {

    val studentCount: LiveData<Int> = studentRepository.getStudentCount().asLiveData()

    private val todayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    val todayAttendanceCount: LiveData<Int> =
        attendanceRepository.getTodayAttendanceCount(todayDate).asLiveData()

    val todayAttendance: LiveData<List<Attendance>> =
        attendanceRepository.getAttendanceByDate(todayDate).asLiveData()

    private val _recognitionResult = MutableLiveData<RecognitionResult>()
    val recognitionResult: LiveData<RecognitionResult> = _recognitionResult

    private var allEmbeddings: List<FaceEmbedding> = emptyList()

    init {
        loadEmbeddings()
    }

    private fun loadEmbeddings() {
        viewModelScope.launch {
            allEmbeddings = studentRepository.getAllEmbeddings()
        }
    }

    suspend fun recognizeFace(faceEmbedding: FloatArray): RecognitionResult {
        return withContext(Dispatchers.Default) {
            if (allEmbeddings.isEmpty()) {
                return@withContext RecognitionResult.NoStudentsInDatabase
            }

            val matchResult = faceRecognizer.findBestMatch(faceEmbedding, allEmbeddings)

            if (matchResult == null) {
                return@withContext RecognitionResult.NoMatch
            }

            val (studentId, confidence) = matchResult
            val student = studentRepository.getStudentById(studentId)
                ?: return@withContext RecognitionResult.NoMatch

            val alreadyMarked = attendanceRepository.checkIfMarkedToday(studentId, todayDate)

            if (alreadyMarked) {
                return@withContext RecognitionResult.AlreadyMarked(student)
            }

            RecognitionResult.Success(student, confidence)
        }
    }

    fun markAttendance(student: Student, confidence: Float, photoPath: String? = null) {
        viewModelScope.launch {
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            val attendance = Attendance(
                studentId = student.id,
                studentName = student.name,
                className = student.className,
                date = todayDate,
                time = currentTime,
                timestamp = System.currentTimeMillis(),
                photoPath = photoPath,
                confidence = confidence
            )

            attendanceRepository.insertAttendance(attendance)
            _recognitionResult.postValue(RecognitionResult.Marked(student))
        }
    }

    sealed class RecognitionResult {
        data class Success(val student: Student, val confidence: Float) : RecognitionResult()
        data class AlreadyMarked(val student: Student) : RecognitionResult()
        data class Marked(val student: Student) : RecognitionResult()
        object NoMatch : RecognitionResult()
        object NoStudentsInDatabase : RecognitionResult()
    }
}