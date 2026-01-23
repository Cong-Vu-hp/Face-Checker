package com.attendance.app.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.entity.Student
import com.attendance.app.data.repository.StudentRepository
import com.attendance.app.ml.FaceRecognizer
import com.attendance.app.utils.ImageProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StudentViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val faceRecognizer: FaceRecognizer,
    private val imageProcessor: ImageProcessor
) : ViewModel() {

    val allStudents: LiveData<List<Student>> = studentRepository.getAllStudents().asLiveData()

    private val _saveResult = MutableLiveData<SaveResult>()
    val saveResult: LiveData<SaveResult> = _saveResult

    fun saveStudent(id: String, name: String, className: String, photoBitmap: Bitmap, rotation: Int) {
        viewModelScope.launch {
            try {
                android.util.Log.d("StudentViewModel", "Starting to save student: $name")

                val result = withContext(Dispatchers.Default) {
                    android.util.Log.d("StudentViewModel", "Bitmap size: ${photoBitmap.width}x${photoBitmap.height}")

                    // Extract face embedding
                    val embedding = faceRecognizer.extractEmbedding(photoBitmap)

                    if (embedding == null) {
                        android.util.Log.e("StudentViewModel", "No face detected in image")
                        return@withContext SaveResult.NoFaceDetected
                    }

                    android.util.Log.d("StudentViewModel", "Face embedding extracted successfully")

                    // Save photo to internal storage
                    val photoPath = imageProcessor.saveBitmapToInternalStorage(photoBitmap, id)

                    if (photoPath == null) {
                        android.util.Log.e("StudentViewModel", "Failed to save photo")
                        return@withContext SaveResult.Error("Failed to save photo")
                    }

                    // Create student entity
                    val student = Student(
                        id = id,
                        name = name,
                        className = className,
                        photoPath = photoPath
                    )

                    // Save to database
                    studentRepository.insertStudent(student, embedding)
                    android.util.Log.d("StudentViewModel", "Student saved successfully")

                    SaveResult.Success(student)
                }

                _saveResult.postValue(result)
            } catch (e: Exception) {
                android.util.Log.e("StudentViewModel", "Error saving student", e)
                _saveResult.postValue(SaveResult.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            studentRepository.deleteStudent(student)
            imageProcessor.deleteImageFile(student.photoPath)
        }
    }

    fun deleteAllStudents() {
        viewModelScope.launch {
            allStudents.value?.forEach { student ->
                imageProcessor.deleteImageFile(student.photoPath)
            }
            studentRepository.deleteAllStudents()
        }
    }

    sealed class SaveResult {
        data class Success(val student: Student) : SaveResult()
        object NoFaceDetected : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
}