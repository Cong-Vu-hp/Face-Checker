package com.attendance.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.os.Build
import android.provider.MediaStore
import com.attendance.app.data.local.entity.FaceEmbedding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import android.net.Uri


@Singleton
class FaceRecognizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val openCVDetector: OpenCVFaceDetector
) {
    private var interpreter: Interpreter? = null
    private val inputSize = 160
    private val embeddingSize = 128
    private val threshold = 1.0f

    // Cấu hình ML Kit giống như project mẫu
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setMinFaceSize(0.01f) // 1% - rất nhỏ
        .build()

    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)

    init {
        try {
            val modelFile = loadModelFile()
            interpreter = Interpreter(modelFile, Interpreter.Options())
            android.util.Log.d("FaceRecognizer", "TensorFlow Lite model loaded successfully")
        } catch (e: Exception) {
            android.util.Log.e("FaceRecognizer", "Failed to load TF Lite model", e)
            interpreter = null
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("facenet_mobile.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    suspend fun extractEmbedding(bitmap: Bitmap): FloatArray? {
        return try {
            android.util.Log.d("FaceRecognizer", "=== EXTRACTING EMBEDDING ===")
            android.util.Log.d("FaceRecognizer", "Input: ${bitmap.width}x${bitmap.height}")

            if (interpreter == null) {
                android.util.Log.e("FaceRecognizer", "TensorFlow interpreter is null!")
                return null
            }

            // DÙNG OPENCV thay vì ML Kit
            val processedBitmap = preprocessBitmap(bitmap)

            // Detect face với OpenCV
            val faceRect = openCVDetector.detectLargestFace(processedBitmap)

            if (faceRect == null) {
                android.util.Log.w("FaceRecognizer", "No face detected by OpenCV")
                return null
            }

            android.util.Log.d("FaceRecognizer", "✅ OpenCV detected face: $faceRect")

            // Extract face bitmap
            val faceBitmap = openCVDetector.extractFace(processedBitmap, faceRect, 0.3f)
            if (faceBitmap == null) {
                android.util.Log.e("FaceRecognizer", "Failed to extract face")
                return null
            }

            // Resize về input size
            val resizedBitmap = Bitmap.createScaledBitmap(
                faceBitmap,
                inputSize,
                inputSize,
                true
            )

            // Extract embedding
            val inputBuffer = bitmapToByteBuffer(resizedBitmap)
            val outputArray = Array(1) { FloatArray(embeddingSize) }
            interpreter?.run(inputBuffer, outputArray)

            val embedding = outputArray[0]
            normalizeEmbedding(embedding)

            android.util.Log.d("FaceRecognizer", "✅ Embedding extracted successfully")

            embedding

        } catch (e: Exception) {
            android.util.Log.e("FaceRecognizer", "Error extracting embedding", e)
            null
        }
    }
    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        /* Resize về kích thước tối ưu (800x800)
        val maxSize = 800
        val ratio = maxSize.toFloat() / Math.max(bitmap.width, bitmap.height)
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()

        val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        // Tăng độ tương phản (optional)
        val canvas = android.graphics.Canvas(resized)
        val paint = android.graphics.Paint()
        val colorMatrix = android.graphics.ColorMatrix()
        colorMatrix.setSaturation(1.2f) // Tăng độ bão hòa
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(resized, 0f, 0f, paint)*/

        return bitmap
    }
    // Thêm hàm lưu ảnh
    private fun saveBitmapToFile(bitmap: Bitmap, filename: String) {
        try {
            val file = java.io.File(context.cacheDir, filename)
            val fos = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()
            android.util.Log.d("FaceRecognizer", "Saved debug image: ${file.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("FaceRecognizer", "Failed to save debug image", e)
        }
    }

    // Thử nhiều cách xử lý ảnh


    // Preprocessing như project mẫu

    private fun processFace(bitmap: Bitmap, face: Face): FloatArray? {
        try {
            // Crop face với padding
            val faceBitmap = cropFaceWithPadding(bitmap, face.boundingBox)

            // Resize về input size của model
            val resizedBitmap = Bitmap.createScaledBitmap(
                faceBitmap,
                inputSize,
                inputSize,
                true
            )

            // Convert sang ByteBuffer
            val inputBuffer = bitmapToByteBuffer(resizedBitmap)

            // Run inference
            val outputArray = Array(1) { FloatArray(embeddingSize) }
            interpreter?.run(inputBuffer, outputArray)

            // Normalize embedding
            val embedding = outputArray[0]
            normalizeEmbedding(embedding)

            return embedding
        } catch (e: Exception) {
            android.util.Log.e("FaceRecognizer", "Error processing face", e)
            return null
        }
    }

    private fun cropFaceWithPadding(bitmap: Bitmap, boundingBox: Rect): Bitmap {
        // Padding 30% để có context xung quanh khuôn mặt
        val padding = (boundingBox.width()).toInt()

        val left = (boundingBox.left - padding).coerceAtLeast(0)
        val top = (boundingBox.top - padding).coerceAtLeast(0)
        val right = (boundingBox.right + padding).coerceAtMost(bitmap.width)
        val bottom = (boundingBox.bottom + padding).coerceAtMost(bitmap.height)

        val width = right - left
        val height = bottom - top

        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val value = intValues[pixel++]
                // Normalize to [-1, 1] range (standard for FaceNet)
                byteBuffer.putFloat(((value shr 16 and 0xFF) - 127.5f) / 128.0f)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - 127.5f) / 128.0f)
                byteBuffer.putFloat(((value and 0xFF) - 127.5f) / 128.0f)
            }
        }

        return byteBuffer
    }

    private fun normalizeEmbedding(embedding: FloatArray) {
        var sum = 0f
        for (value in embedding) {
            sum += value * value
        }
        val norm = sqrt(sum)
        if (norm > 0) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
    }

    fun findBestMatch(
        queryEmbedding: FloatArray,
        database: List<FaceEmbedding>
    ): Pair<String, Float>? {
        if (database.isEmpty()) {
            android.util.Log.w("FaceRecognizer", "Database is empty")
            return null
        }

        var bestMatch: String? = null
        var bestDistance = Float.MAX_VALUE

        for (entry in database) {
            val distance = calculateDistance(queryEmbedding, entry.embedding)

            android.util.Log.d("FaceRecognizer", "Student ${entry.studentId}: distance = $distance")

            if (distance < threshold && distance < bestDistance) {
                bestDistance = distance
                bestMatch = entry.studentId
            }
        }

        if (bestMatch != null) {
            android.util.Log.d("FaceRecognizer", "Best match: $bestMatch with distance $bestDistance")
        } else {
            android.util.Log.d("FaceRecognizer", "No match found (threshold: $threshold)")
        }

        return bestMatch?.let { it to (1f - bestDistance) }
    }

    // Cosine distance giống project mẫu
    private fun calculateDistance(embedding1: FloatArray, embedding2: FloatArray): Float {
        require(embedding1.size == embedding2.size) { "Embeddings must be same size" }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            normA += embedding1[i] * embedding1[i]
            normB += embedding2[i] * embedding2[i]
        }

        val cosineDistance = 1f - (dotProduct / (sqrt(normA) * sqrt(normB)))
        return cosineDistance
    }

    fun release() {
        interpreter?.close()
        faceDetector.close()
        android.util.Log.d("FaceRecognizer", "Resources released")
   }
}


