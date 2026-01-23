package com.attendance.app.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.entity.Attendance
import com.attendance.app.data.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    val allAttendance: LiveData<List<Attendance>> =
        attendanceRepository.getAllAttendance().asLiveData()

    fun clearAllHistory() {
        viewModelScope.launch {
            attendanceRepository.deleteAllAttendance()
        }
    }
}