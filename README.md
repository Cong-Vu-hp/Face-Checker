# 📱 Attendance Face Recognition App

An Android application for automated student attendance tracking using face recognition technology.

## 🌟 Features

- **Face Recognition**: Real-time face detection and recognition using ML Kit and TensorFlow Lite
- **Student Management**: Add, view, and manage student profiles with facial data
- **Camera Integration**: Capture student photos with live face detection overlay
- **Gallery Support**: Import student photos from device gallery
- **Attendance Tracking**: Automated attendance marking through face recognition
- **History Logs**: View attendance history for all students

## 🏗️ Architecture

### Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Dagger Hilt
- **Database**: Room
- **Machine Learning**: 
  - ML Kit Face Detection
  - TensorFlow Lite (FaceNet model)
- **Camera**: CameraX
- **Async**: Kotlin Coroutines

### Project Structure

```
app/
├── data/
│   ├── local/          # Room database entities and DAOs
│   └── repository/     # Data repositories
├── di/                 # Dependency injection modules
├── ml/                 # Face recognition ML logic
├── presentation/
│   ├── adapter/        # RecyclerView adapters
│   ├── fragment/       # UI fragments
│   ├── view/           # Custom views
│   └── viewmodel/      # ViewModels
└── utils/              # Utility classes
```

## 📋 Prerequisites

- Android Studio Arctic Fox or later
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Gradle 8.0+

## 🚀 Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/attendance-face-recognition.git
cd attendance-face-recognition
```

### 2. Add FaceNet Model

Download the FaceNet mobile model and place it in the assets folder:

```
app/src/main/assets/facenet_mobile.tflite
```

You can obtain the model from:
- [FaceNet TensorFlow Lite Model](https://github.com/sirius-ai/MobileFaceNet_TF)
- Or train your own FaceNet model

### 3. Configure Permissions

The app requires the following permissions (already configured in `AndroidManifest.xml`):

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

### 4. Build and Run

1. Open the project in Android Studio
2. Sync Gradle files
3. Run the app on a physical device (camera required)

## 📖 Usage Guide

### Adding Students

1. Navigate to **Add Student** fragment
2. Click **"Bật camera"** (Turn on camera)
3. Position face in the oval guide
4. Wait for green "✓ Phát hiện khuôn mặt" message
5. Click **"Chụp ảnh"** (Take photo)
6. Fill in student details:
   - Student ID
   - Name
   - Class
7. Click **"Lưu"** (Save)

**Alternative**: Click **"Chọn từ thư viện"** to select an existing photo

### Taking Attendance

1. Navigate to **Attendance** fragment
2. Click **"Bật camera"**
3. Point camera at student's face
4. System automatically recognizes and marks attendance
5. View real-time results on screen

### Viewing History

1. Navigate to **History** fragment
2. View all attendance records
3. Filter by date, student, or class

## 🔧 Key Components

### FaceRecognizer

The core ML component that handles:
- Face detection using ML Kit
- Face embedding extraction using FaceNet
- Face matching against database
- Multiple detection strategies for robustness

**Key Methods**:
```kotlin
suspend fun extractEmbedding(bitmap: Bitmap): FloatArray?
fun findBestMatch(queryEmbedding: FloatArray, database: List<FaceEmbedding>): Pair<String, Float>?
```

### AddStudentFragment

Manages student registration with:
- Real-time face detection overlay
- Camera and gallery image support
- Auto camera shutdown after capture
- Image orientation correction
- Validation before saving

### Camera Features

- **Live Face Detection**: Shows oval guide and detection status
- **Auto-rotation**: Corrects image orientation using EXIF data
- **Mirror Effect**: Applies for front camera
- **Resource Management**: Camera stops after capture to save battery

## 🐛 Troubleshooting

### Face Not Detected

**Symptoms**: "Không tìm thấy khuôn mặt" error when saving

**Solutions**:
1. Ensure good lighting conditions
2. Face camera directly (avoid extreme angles)
3. Move closer to camera (face should fill oval guide)
4. Check logcat for detailed detection logs:
   ```
   adb logcat | grep FaceRecognizer
   ```

**Debug Mode**: Use `FaceDetectionDebugHelper` to visualize detection:
```kotlin
val result = FaceDetectionDebugHelper.debugFaceDetection(context, bitmap)
// Check cache/face_debug/ for annotated images
```

### Camera Issues

**Camera won't start**:
- Grant camera permission in Settings
- Restart the app
- Check device has working camera

**Image rotated incorrectly**:
- The app auto-corrects using EXIF data
- If issues persist, check `fixImageOrientation()` method

### Database Issues

**Clear app data**:
```bash
adb shell pm clear com.attendance.app
```

**Export database for inspection**:
```bash
adb pull /data/data/com.attendance.app/databases/attendance_db.db
```

## 📊 Recognition Algorithm

1. **Face Detection**: ML Kit detects face boundaries
2. **Face Cropping**: Extracts face region with 30% padding
3. **Preprocessing**: Resizes to 160x160, normalizes pixels
4. **Embedding**: FaceNet generates 128-dimensional vector
5. **Matching**: Cosine similarity against database
6. **Threshold**: Match accepted if distance < 0.6

## 🔐 Privacy & Security

- All face data stored locally on device
- No cloud upload of biometric data
- Database encrypted at rest (if using SQLCipher)
- Permissions requested only when needed

## 🛠️ Configuration

### Adjust Recognition Threshold

In `FaceRecognizer.kt`:
```kotlin
private val threshold = 0.6f // Lower = stricter matching
```

### Adjust Face Detection Sensitivity

In `FaceRecognizer.kt`:
```kotlin
.setMinFaceSize(0.1f) // Lower = detect smaller faces
.setPerformanceMode(PERFORMANCE_MODE_ACCURATE) // or FAST
```

### Camera Resolution

In `AddStudentFragment.kt`:
```kotlin
ImageCapture.Builder()
    .setTargetResolution(Size(1280, 720)) // Add this line
    .build()
```

## 📝 Code Examples

### Extracting Face Embedding

```kotlin
// In ViewModel or Repository
val embedding = faceRecognizer.extractEmbedding(bitmap)
if (embedding != null) {
    // Save to database
    val faceEmbedding = FaceEmbedding(
        studentId = studentId,
        embedding = embedding
    )
    database.faceEmbeddingDao().insert(faceEmbedding)
}
```

### Finding Match

```kotlin
val allEmbeddings = database.faceEmbeddingDao().getAll()
val match = faceRecognizer.findBestMatch(queryEmbedding, allEmbeddings)

match?.let { (studentId, confidence) ->
    println("Matched: $studentId with confidence: $confidence")
}
```

## 🧪 Testing

### Manual Testing Checklist

- [ ] Add student with front camera
- [ ] Add student from gallery
- [ ] Recognize registered student
- [ ] Reject unregistered person
- [ ] Handle poor lighting
- [ ] Handle multiple faces
- [ ] Camera rotation handling
- [ ] Permission flows

### Debug Logs

Enable verbose logging:
```kotlin
// In FaceRecognizer
android.util.Log.d("FaceRecognizer", "Your debug message")
```

View logs:
```bash
adb logcat -s FaceRecognizer:D FaceDebug:D
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Authors

- Your Name - Initial work

## 🙏 Acknowledgments

- ML Kit for face detection
- TensorFlow Lite for model inference
- FaceNet architecture for face embeddings
- CameraX for camera integration

## 📞 Support

For issues and questions:
- Open an issue on GitHub
- Check existing issues for solutions
- Review logcat output for errors

## 🔄 Version History

### v1.0.0 (Current)
- Initial release
- Basic face recognition
- Student management
- Attendance tracking
- Camera integration with auto-shutdown
- Gallery support
- Multiple detection strategies

### Planned Features
- [ ] Export attendance to Excel
- [ ] Bulk student import
- [ ] Attendance statistics dashboard
- [ ] Cloud backup option
- [ ] Multiple class support
- [ ] Teacher authentication

---

**Built with ❤️ for educational institutions**