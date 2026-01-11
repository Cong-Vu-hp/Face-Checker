import cv2
import os
import numpy as np

class AttendanceSystem:
    def __init__(self):
        self.recognizer = cv2.face.LBPHFaceRecognizer_create()
        self.detector = cv2.CascadeClassifier("haarcascade_frontalface_default.xml")
        self.names = {} # Lưu tên tương ứng với ID: {1: "Tuan", 2: "Vy"}
        self.load_database()

    def load_database(self):
        """
        Đọc ảnh từ thư mục 'students', train model và lưu lại.
        Quy tắc đặt tên file ảnh: "ID.Ten.jpg" (Ví dụ: "1.Tuan.jpg", "2.Vy.jpg")
        """
        if not os.path.exists('students'):
            os.makedirs('students')
            print("Đã tạo thư mục 'students'. Hãy bỏ ảnh vào theo tên: '1.Ten.jpg'")
            return

        print("Đang huấn luyện dữ liệu khuôn mặt...")
        faces = []
        ids = []

        path = 'students'
        image_paths = [os.path.join(path, f) for f in os.listdir(path) if f.endswith('.jpg')]

        if len(image_paths) == 0:
            print("Cảnh báo: Thư mục students đang trống!")
            return

        for image_path in image_paths:
            # Đọc ảnh và chuyển sang đen trắng (Grayscale)
            img_numpy = cv2.imread(image_path)
            gray = cv2.cvtColor(img_numpy, cv2.COLOR_BGR2GRAY)
            
            # Lấy ID và Tên từ tên file (Ví dụ: 1.Tuan.jpg -> id=1, name="Tuan")
            filename = os.path.split(image_path)[-1]
            parts = filename.split(".")
            
            if len(parts) >= 2:
                try:
                    id_num = int(parts[0])
                    name = parts[1]
                    self.names[id_num] = name
                    
                    # Phát hiện khuôn mặt trong ảnh để học
                    faces_rect = self.detector.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5)
                    for (x, y, w, h) in faces_rect:
                        faces.append(gray[y:y+h, x:x+w])
                        ids.append(id_num)
                except ValueError:
                    print(f"Bỏ qua file {filename}: Tên file phải bắt đầu bằng số ID (vd: 1.Tuan.jpg)")

        if len(faces) > 0:
            self.recognizer.train(faces, np.array(ids))
            print(f"-> Hoàn tất! Đã học được {len(self.names)} người.")
        else:
            print("Không tìm thấy khuôn mặt nào trong ảnh mẫu.")

    def process_frame(self, frame):
        frame = cv2.flip(frame, 1)
        """Phương thức gốc - chỉ xử lý và vẽ lên frame"""
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = self.detector.detectMultiScale(gray, scaleFactor=1.2, minNeighbors=5)

        for (x, y, w, h) in faces:
            id_num, confidence = self.recognizer.predict(gray[y:y+h, x:x+w])
            
            if confidence < 100:
                name = self.names.get(id_num, "Unknown")
                confidence_text = f"  {round(100 - confidence)}%"
            else:
                name = "Unknown"
                confidence_text = f"  {round(100 - confidence)}%"

            color = (0, 255, 0) if name != "Unknown" else (0, 0, 255)
            cv2.rectangle(frame, (x, y), (x+w, y+h), color, 2)
            cv2.putText(frame, str(name), (x+5, y-5), cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)
            cv2.putText(frame, str(confidence_text), (x+5, y+h-5), cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 0), 1)

        return frame
    
    def process_frame_with_ids(self, frame):
        """
        Phương thức mới - xử lý frame và trả về cả danh sách ID đã nhận diện
        Returns: (frame_đã_vẽ, list_các_id_nhận_diện_được)
        """
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = self.detector.detectMultiScale(gray, scaleFactor=1.2, minNeighbors=5)
        
        detected_ids = []

        for (x, y, w, h) in faces:
            id_num, confidence = self.recognizer.predict(gray[y:y+h, x:x+w])
            
            if confidence < 100:
                name = self.names.get(id_num, "Unknown")
                confidence_text = f"  {round(100 - confidence)}%"
                # Chỉ thêm vào detected_ids nếu nhận diện thành công
                if name != "Unknown":
                    detected_ids.append(id_num)
            else:
                name = "Unknown"
                confidence_text = f"  {round(100 - confidence)}%"

            color = (0, 255, 0) if name != "Unknown" else (0, 0, 255)
            cv2.rectangle(frame, (x, y), (x+w, y+h), color, 2)
            cv2.putText(frame, str(name), (x+5, y-5), cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)
            cv2.putText(frame, str(confidence_text), (x+5, y+h-5), cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 0), 1)

        return frame, detected_ids