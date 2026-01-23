package com.attendance.app.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun checkModelExists(): Boolean {
        return try {
            val files = context.assets.list("") ?: emptyArray()
            val hasModel = files.contains("facenet_mobile.tflite")

            if (hasModel) {
                val fd = context.assets.openFd("facenet_mobile.tflite")
                val size = fd.length
                fd.close()
                android.util.Log.d("ModelChecker", "Model found! Size: ${size / 1024}KB")
            } else {
                android.util.Log.e("ModelChecker", "Model NOT found in assets!")
                android.util.Log.d("ModelChecker", "Available files: ${files.joinToString()}")
            }

            hasModel
        } catch (e: Exception) {
            android.util.Log.e("ModelChecker", "Error checking model", e)
            false
        }
    }
}