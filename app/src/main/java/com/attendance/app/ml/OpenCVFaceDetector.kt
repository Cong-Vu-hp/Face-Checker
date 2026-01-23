package com.attendance.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import dagger.hilt.android.qualifiers.ApplicationContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenCVFaceDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var faceDetector: CascadeClassifier? = null
    private var isInitialized = false

    init {
        initializeOpenCV()
    }

    private fun initializeOpenCV() {
        try {
            // Load OpenCV library
            if (!OpenCVLoader.initDebug()) {
                android.util.Log.e("OpenCVFaceDetector", "OpenCV initialization failed!")
                return
            }

            android.util.Log.d("OpenCVFaceDetector", "OpenCV loaded successfully")

            // Load Haar Cascade classifier for face detection
            val cascadeFile = loadCascadeFile()
            faceDetector = CascadeClassifier(cascadeFile.absolutePath)

            if (faceDetector?.empty() == true) {
                android.util.Log.e("OpenCVFaceDetector", "Failed to load cascade classifier!")
                faceDetector = null
            } else {
                isInitialized = true
                android.util.Log.d("OpenCVFaceDetector", "✅ OpenCV Face Detector initialized successfully")
            }
        } catch (e: Exception) {
            android.util.Log.e("OpenCVFaceDetector", "Error initializing OpenCV", e)
            isInitialized = false
        }
    }

    private fun loadCascadeFile(): File {
        // Copy cascade file from assets to cache
        val cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE)
        val cascadeFile = File(cascadeDir, "haarcascade_frontalface_default.xml")

        if (!cascadeFile.exists()) {
            try {
                val inputStream = context.assets.open("haarcascade_frontalface_default.xml")
                val outputStream = FileOutputStream(cascadeFile)

                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                inputStream.close()
                outputStream.close()

                android.util.Log.d("OpenCVFaceDetector", "Cascade file copied to: ${cascadeFile.absolutePath}")
            } catch (e: Exception) {
                android.util.Log.e("OpenCVFaceDetector", "Error copying cascade file", e)
                throw e
            }
        }

        return cascadeFile
    }

    /**
     * Detect faces in bitmap
     * Returns list of face bounding boxes
     */
    fun detectFaces(bitmap: Bitmap): List<Rect> {
        if (!isInitialized || faceDetector == null) {
            android.util.Log.e("OpenCVFaceDetector", "Detector not initialized!")
            return emptyList()
        }

        try {
            android.util.Log.d("OpenCVFaceDetector", "Detecting faces in ${bitmap.width}x${bitmap.height} image")

            // Convert Bitmap to OpenCV Mat
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)

            // Convert to grayscale
            val grayMat = Mat()
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)

            // Equalize histogram for better detection
            Imgproc.equalizeHist(grayMat, grayMat)

            // Detect faces
            val faces = MatOfRect()
            faceDetector?.detectMultiScale(
                grayMat,
                faces,
                1.1,  // scaleFactor
                3,    // minNeighbors
                0,    // flags
                Size(30.0, 30.0), // minSize
                Size() // maxSize (empty = no limit)
            )

            val faceArray = faces.toArray()
            android.util.Log.d("OpenCVFaceDetector", "✅ Detected ${faceArray.size} face(s)")

            // Convert OpenCV Rect to Android Rect
            val androidRects = faceArray.map { cvRect ->
                Rect(
                    cvRect.x,
                    cvRect.y,
                    cvRect.x + cvRect.width,
                    cvRect.y + cvRect.height
                )
            }

            // Log face details
            androidRects.forEachIndexed { index, rect ->
                android.util.Log.d("OpenCVFaceDetector", "Face $index: $rect")
            }

            // Cleanup
            mat.release()
            grayMat.release()
            faces.release()

            return androidRects

        } catch (e: Exception) {
            android.util.Log.e("OpenCVFaceDetector", "Error detecting faces", e)
            return emptyList()
        }
    }

    /**
     * Detect largest face in bitmap
     * Returns bounding box of largest face or null
     */
    fun detectLargestFace(bitmap: Bitmap): Rect? {
        val faces = detectFaces(bitmap)
        if (faces.isEmpty()) return null

        // Find largest face
        return faces.maxByOrNull { rect ->
            val width = rect.width()
            val height = rect.height()
            width * height
        }
    }

    /**
     * Extract face bitmap from original bitmap
     */
    fun extractFace(bitmap: Bitmap, faceRect: Rect, padding: Float = 0.2f): Bitmap? {
        try {
            val paddingX = (faceRect.width() * padding).toInt()
            val paddingY = (faceRect.height() * padding).toInt()

            val left = (faceRect.left - paddingX).coerceAtLeast(0)
            val top = (faceRect.top - paddingY).coerceAtLeast(0)
            val right = (faceRect.right + paddingX).coerceAtMost(bitmap.width)
            val bottom = (faceRect.bottom + paddingY).coerceAtMost(bitmap.height)

            val width = right - left
            val height = bottom - top

            return Bitmap.createBitmap(bitmap, left, top, width, height)
        } catch (e: Exception) {
            android.util.Log.e("OpenCVFaceDetector", "Error extracting face", e)
            return null
        }
    }

    /**
     * Check if face is detected in bitmap
     */
    fun hasFace(bitmap: Bitmap): Boolean {
        return detectFaces(bitmap).isNotEmpty()
    }

    /**
     * Get face count in bitmap
     */
    fun getFaceCount(bitmap: Bitmap): Int {
        return detectFaces(bitmap).size
    }

    fun release() {
        faceDetector = null
        isInitialized = false
        android.util.Log.d("OpenCVFaceDetector", "Resources released")
    }
}