package com.attendance.app.presentation.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.attendance.app.databinding.FragmentAttendanceBinding
import com.attendance.app.ml.FaceRecognizer
import com.attendance.app.presentation.viewmodel.AttendanceViewModel
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class AttendanceFragment : Fragment() {

    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AttendanceViewModel by viewModels()

    @Inject
    lateinit var faceRecognizer: FaceRecognizer

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null
    private var isRecognitionActive = false
    private val markedToday = mutableSetOf<String>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.studentCount.observe(viewLifecycleOwner) { count ->
            binding.tvTotalStudents.text = count.toString()
        }

        viewModel.todayAttendanceCount.observe(viewLifecycleOwner) { count ->
            binding.tvTodayAttendance.text = count.toString()
        }

        viewModel.recognitionResult.observe(viewLifecycleOwner) { result ->
            handleRecognitionResult(result)
        }
    }

    private fun setupClickListeners() {
        binding.btnStartCamera.setOnClickListener {
            checkCameraPermission()
        }

        binding.btnStartRecognition.setOnClickListener {
            toggleRecognition()
        }

        binding.btnExportCsv.setOnClickListener {
            exportCsv()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (isRecognitionActive) {
                        processFrame(imageProxy)
                    } else {
                        imageProxy.close()
                    }
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            binding.tvStatus.text = "✓ Camera đã bật thành công"
        } catch (e: Exception) {
            binding.tvStatus.text = "Lỗi: ${e.message}"
        }
    }

    private fun toggleRecognition() {
        if (isRecognitionActive) {
            stopRecognition()
        } else {
            startRecognition()
        }
    }

    private fun startRecognition() {
        isRecognitionActive = true
        markedToday.clear()
        binding.btnStartRecognition.text = "Dừng điểm danh"
        binding.tvStatus.text = "🔍 Đang nhận diện..."

        // Auto-stop after 5 minutes
        lifecycleScope.launch {
            delay(5 * 60 * 1000L)
            if (isRecognitionActive) {
                stopRecognition()
            }
        }
    }

    private fun stopRecognition() {
        isRecognitionActive = false
        binding.btnStartRecognition.text = "Bắt đầu điểm danh"
        binding.tvStatus.text = "Đã dừng nhận diện"
    }

    private fun processFrame(imageProxy: ImageProxy) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val bitmap = imageProxy.toBitmap()
                val embedding = faceRecognizer.extractEmbedding(bitmap)

                if (embedding != null) {
                    val result = viewModel.recognizeFace(embedding)

                    when (result) {
                        is AttendanceViewModel.RecognitionResult.Success -> {
                            if (result.student.id !in markedToday) {
                                markedToday.add(result.student.id)
                                viewModel.markAttendance(result.student, result.confidence)
                            }
                        }
                        else -> {
                            // Handle other cases if needed
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                imageProxy.close()
            }
        }
    }

    private fun handleRecognitionResult(result: AttendanceViewModel.RecognitionResult) {
        when (result) {
            is AttendanceViewModel.RecognitionResult.Marked -> {
                binding.tvStatus.text = "✅ Điểm danh thành công: ${result.student.name}"
                showToast("Điểm danh: ${result.student.name}")
            }
            is AttendanceViewModel.RecognitionResult.AlreadyMarked -> {
                binding.tvStatus.text = "${result.student.name} đã điểm danh hôm nay"
            }
            is AttendanceViewModel.RecognitionResult.NoMatch -> {
                binding.tvStatus.text = "Không nhận diện được học sinh"
            }
            is AttendanceViewModel.RecognitionResult.NoStudentsInDatabase -> {
                binding.tvStatus.text = "Chưa có học sinh trong hệ thống"
            }
            else -> {}
        }
    }

    private fun exportCsv() {
        // Navigate to history fragment which has export functionality
        showToast("Vui lòng xuất CSV từ tab Lịch sử")
    }

    private fun showToast(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}

@androidx.camera.core.ExperimentalGetImage
private fun ImageProxy.toBitmap(): Bitmap {
    val image = this.image ?: throw IllegalStateException("Image is null")
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}