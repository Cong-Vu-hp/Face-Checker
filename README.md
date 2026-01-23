<<<<<<< HEAD
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
=======
# 🎓 Hệ thống Điểm danh Học sinh - Nhận diện Khuôn mặt

Ứng dụng web điểm danh học sinh sử dụng camera với giao diện thân thiện, hoạt động hoàn toàn offline và lưu trữ dữ liệu cục bộ.

[![Netlify Status](https://api.netlify.com/api/v1/badges/your-badge-id/deploy-status)](https://app.netlify.com/sites/your-site-name/deploys)

## ✨ Tính năng

- 📸 **Điểm danh bằng Camera** - Chụp ảnh và nhận diện học sinh
- 👥 **Quản lý Học sinh** - Thêm, xóa, xem danh sách học sinh
- 📊 **Thống kê Realtime** - Hiển thị số lượng học sinh và điểm danh hôm nay
- 📅 **Lịch sử Điểm danh** - Xem chi tiết các buổi điểm danh
- 📥 **Xuất CSV** - Tải file báo cáo điểm danh
- 💾 **Lưu trữ Offline** - Dữ liệu lưu trên LocalStorage, không cần server
- 📱 **Responsive Design** - Hoạt động tốt trên mọi thiết bị
- 🔄 **Flip Camera** - Chuyển đổi camera trước/sau
- 🪞 **Mirror Mode** - Hiển thị camera như nhìn gương

## 🚀 Demo

Truy cập: [https://your-app.netlify.app](https://your-app.netlify.app)

## 📸 Screenshots

### Giao diện Điểm danh
![Điểm danh](https://via.placeholder.com/800x400?text=Screenshot+Diem+Danh)

### Quản lý Học sinh
![Quản lý](https://via.placeholder.com/800x400?text=Screenshot+Quan+Ly)

## 🛠️ Công nghệ

- **HTML5** - Cấu trúc trang web
- **CSS3** - Styling với gradient và animations
- **JavaScript (Vanilla)** - Logic và xử lý camera
- **LocalStorage API** - Lưu trữ dữ liệu cục bộ
- **MediaDevices API** - Truy cập camera
- **Canvas API** - Xử lý ảnh

## 📋 Yêu cầu

- Trình duyệt hiện đại (Chrome 80+, Firefox 75+, Safari 13+, Edge 80+)
- Thiết bị có camera (webcam hoặc camera điện thoại)
- Kết nối HTTPS hoặc localhost (bắt buộc để truy cập camera)

## 🚀 Cài đặt & Sử dụng

### Cách 1: Sử dụng trực tiếp

1. **Download file `index.html`**
2. **Mở file bằng trình duyệt**
3. **Cấp quyền camera** khi được yêu cầu
4. **Bắt đầu sử dụng!**

### Cách 2: Deploy lên hosting

#### Deploy lên Netlify (Khuyến nghị)

```bash
# 1. Đảm bảo có file index.html

# 2. Drag & drop vào Netlify Drop
# https://app.netlify.com/drop

# Hoặc dùng Netlify CLI
npm install -g netlify-cli
netlify deploy --prod
```

#### Deploy lên Vercel

```bash
# 1. Cài đặt Vercel CLI
npm install -g vercel

# 2. Deploy
vercel
```

#### Deploy lên GitHub Pages

```bash
# 1. Tạo repository mới trên GitHub
# 2. Upload file index.html
# 3. Settings → Pages → Deploy from main branch
```

### Cách 3: Chạy local server

```bash
# Dùng Python
python -m http.server 8000

# Dùng Node.js
npx http-server

# Truy cập: http://localhost:8000
```

## 📖 Hướng dẫn sử dụng

### 1. Thêm Học sinh

1. Chuyển sang tab **"Thêm HS"**
2. Nhập **Mã học sinh**, **Họ và tên**, **Lớp**
3. Nhấn **"Bật Camera"**
4. Đặt khuôn mặt vào giữa màn hình
5. Nhấn **"Chụp ảnh"**
6. Kiểm tra ảnh và nhấn **"Lưu học sinh"**

### 2. Điểm danh

1. Tab **"Điểm danh"**
2. Nhấn **"Bật Camera"**
3. Nhấn **"Điểm danh"**
4. Nhập mã học sinh khi được yêu cầu
5. Hệ thống ghi nhận và hiển thị thông báo

### 3. Quản lý Học sinh

1. Tab **"Quản lý"**
2. Xem danh sách học sinh
3. Nhấn **"Xóa"** để xóa học sinh (sau khi xác nhận)

### 4. Xem Lịch sử

1. Tab **"Lịch sử"**
2. Xem danh sách điểm danh hôm nay
3. Nhấn **"Xuất CSV"** để tải file báo cáo

### 5. Xuất Dữ liệu

- Nhấn nút **"Xuất CSV"** trong tab Điểm danh hoặc Lịch sử
- File CSV sẽ tự động tải về với tên `diem_danh_[ngày].csv`
- Mở bằng Excel, Google Sheets, hoặc LibreOffice

## 🔐 Bảo mật & Quyền riêng tư

### Lưu trữ Dữ liệu

- ✅ **Lưu trữ cục bộ**: Tất cả dữ liệu lưu trong LocalStorage của trình duyệt
- ✅ **Không có server**: Không gửi dữ liệu lên cloud
- ✅ **Riêng tư hoàn toàn**: Mỗi thiết bị có database riêng

### Quyền truy cập

- 📷 **Camera**: Yêu cầu quyền để chụp ảnh học sinh và điểm danh
- 💾 **LocalStorage**: Lưu thông tin học sinh và lịch sử điểm danh

### Lưu ý Quan trọng

⚠️ **Dữ liệu sẽ bị mất nếu:**
- Xóa dữ liệu trình duyệt (Clear browsing data)
- Xóa cache/cookies
- Gỡ cài đặt trình duyệt (trên một số thiết bị)

💡 **Khuyến nghị:**
- Xuất CSV định kỳ để sao lưu
- Không dùng chế độ Incognito/Private
- Giữ trình duyệt luôn cập nhật

## 📂 Cấu trúc Dữ liệu

### LocalStorage Keys

```javascript
// Danh sách học sinh
localStorage.getItem('students')
// Format: 
{
  "HS001": {
    "id": "HS001",
    "name": "Nguyễn Văn A",
    "class": "10A1",
    "photo": "data:image/jpeg;base64,...",
    "createdAt": "2025-01-18T10:30:00.000Z"
  }
}

// Lịch sử điểm danh
localStorage.getItem('attendance')
// Format:
[
  {
    "studentId": "HS001",
    "studentName": "Nguyễn Văn A",
    "studentClass": "10A1",
    "date": "18/1/2025",
    "time": "08:30:15",
    "timestamp": "2025-01-18T08:30:15.000Z",
    "photo": "data:image/jpeg;base64,..."
  }
]
```

## 🎨 Tùy chỉnh

### Thay đổi màu sắc chủ đạo

Trong file `index.html`, tìm và thay đổi:

```css
/* Gradient chính */
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

/* Thay thành màu khác, ví dụ: */
background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
```

### Thay đổi font chữ

```css
font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;

/* Thay thành: */
font-family: 'Roboto', 'Arial', sans-serif;
```

### Điều chỉnh kích thước camera

```javascript
const constraints = {
    video: {
        facingMode: currentFacingMode,
        width: { ideal: 1280 },  // Thay đổi độ phân giải
        height: { ideal: 720 }
    }
};
```

## 🐛 Xử lý Lỗi

### Camera không hoạt động

**Triệu chứng:** Thông báo "Permission denied" hoặc camera không bật

**Giải pháp:**
1. Kiểm tra quyền camera trong cài đặt trình duyệt
2. Đảm bảo không có app nào khác đang dùng camera
3. Thử trình duyệt khác
4. Kiểm tra đang dùng HTTPS hoặc localhost

### Dữ liệu bị mất

**Triệu chứng:** Danh sách học sinh hoặc lịch sử điểm danh trống

**Giải pháp:**
1. Kiểm tra có vô tình xóa dữ liệu trình duyệt không
2. Kiểm tra đang dùng đúng trình duyệt/profile
3. Khôi phục từ file CSV đã xuất (nếu có)

### Ảnh không hiển thị

**Triệu chứng:** Ảnh học sinh không hiển thị sau khi thêm

**Giải pháp:**
1. Kiểm tra dung lượng LocalStorage (giới hạn ~5-10MB)
2. Giảm số lượng học sinh hoặc chất lượng ảnh
3. Xóa dữ liệu cũ không cần thiết

### File CSV không tải về

**Triệu chứng:** Nhấn "Xuất CSV" nhưng không tải file

**Giải pháp:**
1. Kiểm tra popup blocker
2. Cho phép download trong cài đặt trình duyệt
3. Thử trình duyệt khác

## 🔄 Cập nhật

### Version History

#### v1.0.0 (18/01/2025)
- ✨ Release đầu tiên
- 📸 Chụp ảnh và quản lý học sinh
- 📊 Thống kê và lịch sử điểm danh
- 📥 Xuất CSV
- 🔄 Flip camera
- 🪞 Mirror mode

## 🤝 Đóng góp

Mọi đóng góp đều được chào đón! Nếu bạn muốn cải thiện dự án:

1. Fork repository
2. Tạo branch mới (`git checkout -b feature/AmazingFeature`)
3. Commit thay đổi (`git commit -m 'Add some AmazingFeature'`)
4. Push lên branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

## 📝 License

Dự án này được phát hành dưới giấy phép [MIT License](LICENSE).

Bạn được tự do:
- ✅ Sử dụng cho mục đích cá nhân và thương mại
- ✅ Chỉnh sửa và phân phối
- ✅ Tích hợp vào dự án của bạn

Điều kiện:
- 📋 Giữ thông tin bản quyền gốc

## 👨‍💻 Tác giả

**Tên của bạn**
- GitHub: [@yourusername](https://github.com/yourusername)
- Email: your.email@example.com

## 🙏 Cảm ơn

- Camera API documentation
- LocalStorage tutorials
- Community feedback

## 📞 Hỗ trợ

Nếu gặp vấn đề hoặc có câu hỏi:

1. 🐛 [Tạo Issue](https://github.com/yourusername/attendance-app/issues)
2. 💬 [Discussions](https://github.com/yourusername/attendance-app/discussions)
3. 📧 Email: your.email@example.com

## 🗺️ Roadmap

### Phiên bản tiếp theo (v2.0.0)

- [ ] 🤖 Tích hợp AI nhận diện khuôn mặt thật
- [ ] ☁️ Đồng bộ dữ liệu qua cloud (tùy chọn)
- [ ] 📊 Biểu đồ thống kê chi tiết
- [ ] 🔔 Thông báo nhắc nhở điểm danh
- [ ] 📱 PWA - Cài đặt như app native
- [ ] 🌐 Đa ngôn ngữ (Tiếng Anh, Tiếng Việt)
- [ ] 🎨 Themes (Light/Dark mode)
- [ ] 📸 Upload ảnh từ thư viện
- [ ] 📤 Xuất PDF report

---

<div align="center">

**⭐ Nếu thấy hữu ích, hãy cho dự án 1 star nhé! ⭐**

Made with ❤️ by [Your Name]

</div>
>>>>>>> d32e04e2f799749fc2269d925a5850de9f078f61
