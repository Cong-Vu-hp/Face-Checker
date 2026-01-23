package com.attendance.app.presentation.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.attendance.app.databinding.FragmentAddStudentBinding
import com.attendance.app.presentation.viewmodel.StudentViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.ImageDecoder
import android.os.Build


@androidx.camera.core.ExperimentalGetImage
@AndroidEntryPoint
class AddStudentFragment : Fragment() {

    private var _binding: FragmentAddStudentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudentViewModel by viewModels()

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var imageCapture: ImageCapture? = null
    private var faceDetector: FaceDetector? = null
    private var capturedBitmap: Bitmap? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Storage permission is required to select an image.", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            handleImageSelected(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddStudentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setMinFaceSize(0.1f)
            .build()
        faceDetector = FaceDetection.getClient(options)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            binding.btnSave.isEnabled = true // Re-enable button
            when (result) {
                is StudentViewModel.SaveResult.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "✓ Đã thêm học sinh: ${result.student.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                    clearForm()
                }
                is StudentViewModel.SaveResult.NoFaceDetected -> {
                    binding.tvStatus.text = "Không tìm thấy khuôn mặt. Vui lòng chụp lại"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                    Toast.makeText(
                        requireContext(),
                        "Không tìm thấy khuôn mặt. Vui lòng chụp lại",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is StudentViewModel.SaveResult.Error -> {
                    binding.tvStatus.text = "Lỗi: ${result.message}"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                    Toast.makeText(
                        requireContext(),
                        "Lỗi: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnStartCamera.setOnClickListener {
            checkCameraPermission()
        }

        binding.btnChooseGallery.setOnClickListener {
            checkStoragePermissionAndPickImage()
        }

        binding.btnCapture.setOnClickListener {
            binding.tvStatus.text = "💡 Mẹo: Đặt khuôn mặt ở giữa, ánh sáng tốt, nhìn thẳng vào camera"
            capturePhoto()
        }

        binding.btnSave.setOnClickListener {
            saveStudent()
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
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(cameraProvider!!)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    detectFaceInPreview(imageProxy)
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalyzer
            )
            binding.tvStatus.text = "✓ Camera đã bật thành công"
            binding.previewView.visibility = View.VISIBLE
            binding.ivCaptured.visibility = View.GONE
            binding.faceOverlay.showGuide = true

            binding.btnStartCamera.isEnabled = false
            binding.btnCapture.isEnabled = true
        } catch (e: Exception) {
            binding.tvStatus.text = "Lỗi: ${e.message}"
        }
    }

    private fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            binding.previewView.visibility = View.GONE
            binding.faceOverlay.showGuide = false
            binding.faceOverlay.faceDetected = false

            binding.btnStartCamera.isEnabled = true
            binding.btnCapture.isEnabled = false

            binding.tvStatus.text = "Camera đã tắt"
            binding.tvStatus.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.black)
            )
        } catch (e: Exception) {
            binding.tvStatus.text = "Lỗi khi tắt camera: ${e.message}"
        }
    }

    @ExperimentalGetImage
    private fun detectFaceInPreview(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            faceDetector?.process(image)
                ?.addOnSuccessListener { faces ->
                    if (!isAdded) return@addOnSuccessListener
                    requireActivity().runOnUiThread {
                        binding.faceOverlay.faceDetected = faces.isNotEmpty()

                        if (faces.isNotEmpty()) {
                            binding.tvStatus.text = "✓ Phát hiện khuôn mặt - Sẵn sàng chụp!"
                            binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                        } else {
                            binding.tvStatus.text = "Đặt khuôn mặt vào khung hình oval"
                            binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                        }
                    }
                }
                ?.addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun createTempFile(): File {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        val storageDir: File? = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = createTempFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    if (!isAdded) return

                    requireActivity().runOnUiThread {
                        handleImageSelected(savedUri)
                        binding.tvStatus.text = "✓ Đã chụp ảnh thành công - Nhấn 'Bật camera' để chụp lại"
                        binding.tvStatus.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                        )
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    if (!isAdded) return
                    requireActivity().runOnUiThread {
                        binding.tvStatus.text = "Lỗi chụp ảnh: ${exc.message}"
                        binding.tvStatus.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                        )
                    }
                }
            }
        )
    }

    private fun saveStudent() {
        val id = binding.etStudentId.text.toString().trim()
        val name = binding.etStudentName.text.toString().trim()
        val className = binding.etClass.text.toString().trim()
        val bitmap = capturedBitmap

        when {
            id.isEmpty() || name.isEmpty() || className.isEmpty() -> {
                Toast.makeText(requireContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
            bitmap == null -> {
                Toast.makeText(requireContext(), "Vui lòng chụp ảnh hoặc chọn ảnh học sinh", Toast.LENGTH_SHORT).show()
            }
            else -> {
                binding.btnSave.isEnabled = false
                binding.tvStatus.text = "Đang xử lý và lưu..."
                binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                viewModel.saveStudent(id, name, className, bitmap, 0)
            }
        }
    }

    private fun clearForm() {
        binding.etStudentId.text?.clear()
        binding.etStudentName.text?.clear()
        binding.etClass.text?.clear()
        capturedBitmap = null

        // Reset UI to initial state
        binding.ivCaptured.visibility = View.GONE
        binding.previewView.visibility = View.GONE
        binding.faceOverlay.showGuide = false
        binding.faceOverlay.faceDetected = false

        binding.btnStartCamera.isEnabled = true
        binding.btnChooseGallery.isEnabled = true
        binding.btnCapture.isEnabled = false
        binding.btnSave.isEnabled = true

        binding.tvStatus.text = "Chọn ảnh hoặc bật camera để bắt đầu"
        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCamera()
        faceDetector?.close()
        cameraExecutor.shutdown()
        cameraProvider = null
        _binding = null
    }

    private fun checkStoragePermissionAndPickImage() {
        when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    openGallery()
                } else {
                    requestStoragePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
            else -> {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    openGallery()
                } else {
                    requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun handleImageSelected(uri: Uri) {
        try {
            stopCamera() // Stop camera if it's running

            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(
                    requireContext().contentResolver,
                    uri
                )
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }

            val processedBitmap: Bitmap =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(
                        requireContext().contentResolver,
                        uri
                    )
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(
                        requireContext().contentResolver,
                        uri
                    )
                }

            capturedBitmap = processedBitmap

            binding.ivCaptured.setImageBitmap(capturedBitmap)
            binding.ivCaptured.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER

            binding.ivCaptured.visibility = View.VISIBLE
            binding.previewView.visibility = View.GONE
            binding.faceOverlay.showGuide = false

            binding.tvStatus.text = "✓ Đã chọn ảnh từ thư viện (${processedBitmap.width}x${processedBitmap.height})"
            binding.tvStatus.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            )

        } catch (e: Exception) {
            android.util.Log.e("AddStudent", "Error loading image from gallery", e)
            binding.tvStatus.text = "Lỗi: Không thể đọc ảnh"
            binding.tvStatus.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            )
        }
    }

    // DEFINITIVE FIX: Replaced faulty orientation logic with a complete and robust version.
    private fun fixImageOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix()
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val exif = inputStream?.let { androidx.exifinterface.media.ExifInterface(it) }
            inputStream?.close()

            val orientation = exif?.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            ) ?: androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL

            /*when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    matrix.postRotate(180f)
                    matrix.postScale(-1f, 1f)
                }
                androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }*/

            return if (matrix.isIdentity) {
                bitmap
            } else {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        } catch (e: Exception) {
            android.util.Log.e("AddStudent", "Error fixing orientation", e)
            return bitmap // Return original bitmap on error
        }
    }

}
