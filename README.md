HỆ THỐNG ĐIỂM DANH KHUÔN MẶT
Một ứng dụng điểm danh tự động sử dụng nhận diện khuôn mặt, được xây dựng bằng Python, OpenCV và giao diện Kivy hiện đại, hỗ trợ cả PC và Android.

📋 Yêu cầu hệ thống
Python 3.7+

Webcam (cho PC) hoặc Camera (cho Android)

🔧 Cài đặt & Chuẩn bị
1. Cài đặt các thư viện cần thiết
Chạy lệnh sau trong terminal để cài đặt các thư viện Python:

Bash

pip install opencv-python opencv-contrib-python kivy numpy
2. Chuẩn bị file nhận diện (Bắt buộc)
Bạn cần tải file model khuôn mặt của OpenCV và đặt vào cùng thư mục dự án. 👉 Tải file haarcascade_frontalface_default.xml tại đây (Nhấn chuột phải vào link -> Lưu liên kết thành...)

3. Cấu trúc thư mục dự án
Sau khi cài đặt và chạy lần đầu, cấu trúc thư mục sẽ như sau:

attendance_system/
│
├── main_menu.py                   # File chính chạy ứng dụng (Giao diện Kivy)
├── recognition.py                 # Module xử lý nhận diện khuôn mặt (AI Logic)
├── haarcascade_frontalface_default.xml # [QUAN TRỌNG] File model nhận diện
│
├── students/                      # Thư mục chứa ảnh sinh viên (Tự động tạo)
│   ├── 1.Tuan.jpg
│   ├── 2.Vy.jpg
│   └── ...
│
└── attendance_logs/               # Thư mục lưu lịch sử điểm danh (Tự động tạo)
    ├── 2026-01-08.txt
    └── ...
🚀 Cách chạy ứng dụng
Mở terminal tại thư mục dự án và chạy lệnh:

Bash

python main_menu.py
📖 Hướng dẫn sử dụng
⚠️ Lưu ý về bảo mật: Các chức năng quản trị (Thêm sinh viên và Lịch sử) yêu cầu nhập mã PIN Admin. Mặc định là 1234.

1. ➕ THÊM SINH VIÊN MỚI (Cần PIN Admin)
Có 2 cách để thêm ảnh sinh viên:

Cách 1: Chụp ảnh trực tiếp từ camera (Khuyên dùng)
Từ menu chính, chọn "THÊM SINH VIÊN MỚI" và nhập PIN.

Nhập ID (số nguyên duy nhất) và Tên sinh viên.

Nhấn "📷 CHỤP ẢNH TỪ CAMERA".

Trên điện thoại: Nhấn nút "🔄 ĐỔI CAM" để chuyển camera trước/sau nếu cần.

Đặt mặt vào trong khung hình oval màu xanh và nhấn "📷 CHỤP ẢNH".

Xem lại ảnh vừa chụp:

Nhấn "✓ SỬ DỤNG" nếu hài lòng.

Nhấn "↻ CHỤP LẠI" nếu ảnh bị mờ hoặc nhắm mắt.

Cuối cùng, nhấn "✓ THÊM SINH VIÊN" để hoàn tất.

Cách 2: Chọn ảnh có sẵn
Nhập ID và Tên sinh viên.

Nhấn "📁 CHỌN ẢNH CÓ SẴN".

Chọn file ảnh (JPG, PNG). Ảnh sẽ tự động được copy và đổi tên chuẩn vào thư mục hệ thống.

Nhấn "✓ THÊM SINH VIÊN".

2. 📷 BẮT ĐẦU ĐIỂM DANH
Từ menu chính, chọn "BẮT ĐẦU ĐIỂM DANH".

Camera sẽ bật lên. Hệ thống tự động nhận diện với phản hồi thị giác:

✅ Người đã đăng ký: Hiện khung màu XANH LÁ kèm ID và Tên. Hệ thống tự động lưu thời gian điểm danh.

❌ Người lạ: Hiện khung màu ĐỎ kèm chữ "Unknown".

Nhấn "⬅ QUAY LẠI MENU" để kết thúc phiên.

3. 📋 DANH SÁCH SINH VIÊN
Xem danh sách tất cả sinh viên (ID và Tên) hiện có trong cơ sở dữ liệu.

4. 🕒 LỊCH SỬ ĐIỂM DANH (Cần PIN Admin)
Chọn "LỊCH SỬ ĐIỂM DANH" và nhập PIN.

Xem lịch sử được nhóm theo từng ngày (hiển thị 10 ngày gần nhất).

[MỚI] Xuất báo cáo: Nhấn nút "📊 XUẤT FILE EXCEL (.CSV)" để tải về file báo cáo đầy đủ, dễ dàng mở bằng Excel để thống kê.

🎨 Tính năng Nổi bật
✅ Nhận diện Real-time: Phản hồi khung xanh/đỏ ngay lập tức trên màn hình. ✅ Hỗ trợ Mobile: Có nút đổi camera trước/sau, giao diện tối ưu cho cảm ứng. ✅ Chế độ Soi gương: Hình ảnh camera được lật tự nhiên, dễ căn chỉnh. ✅ Bảo mật Admin: Mã PIN bảo vệ các chức năng quan trọng. ✅ Xuất báo cáo CSV: Dễ dàng trích xuất dữ liệu để quản lý. ✅ Quy trình thêm mới hoàn thiện: Hỗ trợ chụp ảnh với khung hướng dẫn, xem lại ảnh trước khi lưu.

🔍 Cách hoạt động
Tự động huấn luyện: Mỗi khi vào màn hình điểm danh, hệ thống sẽ quét thư mục students/. Nếu phát hiện thay đổi, nó tự động huấn luyện lại mô hình AI (sử dụng thuật toán LBPH).

Ghi nhận thông minh: Mỗi sinh viên chỉ được ghi nhận điểm danh 1 lần duy nhất trong một phiên hoạt động để tránh trùng lặp dữ liệu.

⚙️ Tinh chỉnh & Xử lý lỗi
Thay đổi mã PIN Admin
Mở file main_menu.py, tìm dòng đầu tiên và sửa đổi: ADMIN_PIN = "1234"

Lỗi: "No module named..."
Chạy lại lệnh cài đặt thư viện ở phần đầu.

Lỗi: Camera bị đen hoặc không mở được
Trên PC: Kiểm tra webcam. Thử đổi dòng cv2.VideoCapture(0) thành cv2.VideoCapture(1) trong file main_menu.py.

Trên Android: Đảm bảo bạn đã cấp quyền truy cập Camera cho ứng dụng trong Cài đặt điện thoại.

Nhận diện không chính xác
Đảm bảo ảnh mẫu khi thêm mới phải rõ nét, đủ sáng, nhìn thẳng.

Khi điểm danh, môi trường ánh sáng nên tương đồng với lúc chụp ảnh mẫu.

📝 Ghi chú về File Log
File lịch sử được lưu trong thư mục attendance_logs/ với tên file là ngày tháng (VD: 2026-01-08.txt).

Định dạng bên trong file là dạng CSV đơn giản: Giờ,ID,Tên. Ví dụ:

08:30:15,1,Tuan
08:35:22,2,Vy