# file: main_menu.py
from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.screenmanager import ScreenManager, Screen
from kivy.uix.image import Image
from kivy.clock import Clock
from kivy.graphics.texture import Texture
from kivy.uix.popup import Popup
from kivy.uix.textinput import TextInput
from kivy.uix.filechooser import FileChooserListView
from kivy.uix.scrollview import ScrollView
from kivy.uix.gridlayout import GridLayout
import cv2
import os
import shutil
import csv
from datetime import datetime
from recognition import AttendanceSystem

# ============================================
# CẤU HÌNH MÃ PIN ADMIN
# ============================================
ADMIN_PIN = "1234"

# ============== SCREEN: MENU CHÍNH ==============
class MenuScreen(Screen):
    def __init__(self, **kwargs):
        super(MenuScreen, self).__init__(**kwargs)
        layout = BoxLayout(orientation='vertical', padding=20, spacing=15)
        
        title = Label(text='HỆ THỐNG ĐIỂM DANH KHUÔN MẶT', font_size='28sp', size_hint=(1, 0.2), bold=True)
        layout.add_widget(title)
        
        btn_start = Button(text='📷 BẮT ĐẦU ĐIỂM DANH', size_hint=(1, 0.15), font_size='20sp', background_color=(0.2, 0.8, 0.2, 1))
        btn_start.bind(on_release=lambda x: self.go_to_screen('attendance'))
        
        btn_add = Button(text='➕ THÊM SINH VIÊN MỚI (Admin)', size_hint=(1, 0.15), font_size='20sp', background_color=(0.2, 0.6, 1, 1))
        btn_add.bind(on_release=lambda x: self.check_pin_and_go('add_student'))
        
        btn_list = Button(text='📋 DANH SÁCH SINH VIÊN', size_hint=(1, 0.15), font_size='20sp', background_color=(0.9, 0.6, 0.1, 1))
        btn_list.bind(on_release=lambda x: self.go_to_screen('student_list', refresh_list=True))
        
        btn_history = Button(text='🕒 LỊCH SỬ ĐIỂM DANH (Admin)', size_hint=(1, 0.15), font_size='20sp', background_color=(0.6, 0.3, 0.8, 1))
        btn_history.bind(on_release=lambda x: self.check_pin_and_go('history', load_history=True))
        
        btn_exit = Button(text='❌ THOÁT', size_hint=(1, 0.15), font_size='20sp', background_color=(0.8, 0.2, 0.2, 1))
        btn_exit.bind(on_release=self.exit_app)
        
        layout.add_widget(btn_start)
        layout.add_widget(btn_add)
        layout.add_widget(btn_list)
        layout.add_widget(btn_history)
        layout.add_widget(btn_exit)
        self.add_widget(layout)
        self.pin_popup = None

    def check_pin_and_go(self, screen_name, **kwargs):
        content = BoxLayout(orientation='vertical', padding=10, spacing=10)
        pin_input = TextInput(password=True, multiline=False, hint_text="Nhập mã PIN Admin", font_size='20sp', halign='center')
        btn_box = BoxLayout(size_hint=(1, 0.4), spacing=10)
        btn_confirm = Button(text='Xác nhận', background_color=(0.2, 0.8, 0.2, 1))
        btn_cancel = Button(text='Hủy', background_color=(0.8, 0.2, 0.2, 1))
        btn_box.add_widget(btn_confirm)
        btn_box.add_widget(btn_cancel)
        content.add_widget(pin_input)
        content.add_widget(btn_box)
        self.pin_popup = Popup(title='Yêu cầu quyền Admin', content=content, size_hint=(0.8, 0.4), auto_dismiss=False)

        def on_confirm(instance):
            if pin_input.text == ADMIN_PIN:
                self.pin_popup.dismiss()
                self.go_to_screen(screen_name, **kwargs)
            else:
                pin_input.text = ""
                pin_input.hint_text = "Sai mã PIN! Thử lại."

        btn_confirm.bind(on_release=on_confirm)
        btn_cancel.bind(on_release=self.pin_popup.dismiss)
        self.pin_popup.open()

    def go_to_screen(self, screen_name, refresh_list=False, load_history=False):
        if refresh_list: self.manager.get_screen(screen_name).refresh_list()
        if load_history: self.manager.get_screen(screen_name).load_history()
        self.manager.current = screen_name
    
    def exit_app(self, instance): App.get_running_app().stop()

# ============== SCREEN: ĐIỂM DANH (Có lật hình & vẽ khung) ==============
class AttendanceScreen(Screen):
    def __init__(self, **kwargs):
        super(AttendanceScreen, self).__init__(**kwargs)
        self.layout = BoxLayout(orientation='vertical')
        self.image = Image(size_hint=(1, 0.85))
        self.layout.add_widget(self.image)
        btn_back = Button(text='⬅ QUAY LẠI MENU', size_hint=(1, 0.15), font_size='18sp', background_color=(0.8, 0.2, 0.2, 1))
        btn_back.bind(on_release=self.go_back)
        self.layout.add_widget(btn_back)
        self.add_widget(self.layout)
        self.capture = None
        self.system = None
        self.event = None
        self.attendance_log = {}
        self.frame_count = 0
    
    def on_enter(self):
        self.capture = cv2.VideoCapture(0)
        self.system = AttendanceSystem()
        self.attendance_log = {}
        self.frame_count = 0
        self.event = Clock.schedule_interval(self.update, 1.0 / 30.0)
    
    def update(self, dt):
        ret, frame = self.capture.read()
        if ret:
            # 1. Lật hình soi gương NGAY LẬP TỨC
            frame = cv2.flip(frame, 1)
            self.frame_count += 1

            # 2. Nhận diện (Chạy mỗi 3 frame để tối ưu hiệu năng)
            if self.frame_count % 3 == 0:
                self.current_frame, self.recognized_results = self.system.process_frame_with_ids(frame)
            
            # 3. Vẽ phản hồi thị giác lên frame đã lật
            if hasattr(self, 'recognized_results'):
                for ((x, y, w, h), id_num) in self.recognized_results:
                    if id_num != -1:
                        name = self.system.names.get(id_num, "Unknown")
                        color = (0, 255, 0) # Xanh lá
                        if id_num not in self.attendance_log:
                            self.attendance_log[id_num] = name
                            self.save_attendance(id_num, name)
                    else:
                        name = "Unknown"
                        color = (0, 0, 255) # Đỏ
                    cv2.rectangle(frame, (x, y), (x+w, y+h), color, 2)
                    cv2.putText(frame, name, (x, y-10), cv2.FONT_HERSHEY_SIMPLEX, 0.8, color, 2)

            # 4. Hiển thị lên Kivy (Lật ngược lại để texture hiển thị đúng chiều)
            buf1 = cv2.flip(frame, 0)
            buf = buf1.tostring()
            texture = Texture.create(size=(frame.shape[1], frame.shape[0]), colorfmt='bgr')
            texture.blit_buffer(buf, colorfmt='bgr', bufferfmt='ubyte')
            self.image.texture = texture
    
    def save_attendance(self, id_num, name):
        if not os.path.exists('attendance_logs'): os.makedirs('attendance_logs')
        today = datetime.now().strftime("%Y-%m-%d")
        time_now = datetime.now().strftime("%H:%M:%S")
        log_file = f'attendance_logs/{today}.txt'
        with open(log_file, 'a', encoding='utf-8') as f:
            f.write(f"{time_now},{id_num},{name}\n")
    
    def go_back(self, instance):
        if self.event: self.event.cancel()
        if self.capture: self.capture.release()
        self.manager.current = 'menu'
    
    def on_leave(self):
        if self.event: self.event.cancel()
        if self.capture: self.capture.release()

# ============== SCREEN: CHỤP ẢNH TỪ CAMERA (Có đổi Cam, Lật hình, Xem lại) ==============
class CapturePhotoScreen(Screen):
    def __init__(self, **kwargs):
        super(CapturePhotoScreen, self).__init__(**kwargs)
        self.layout = BoxLayout(orientation='vertical', padding=10, spacing=10)
        self.camera_index = 0 # 0: Sau, 1: Trước
        
        title = Label(text='CHỤP ẢNH TỪ CAMERA', font_size='24sp', size_hint=(1, 0.1))
        self.layout.add_widget(title)
        self.image = Image(size_hint=(1, 0.7))
        self.layout.add_widget(self.image)
        self.instruction = Label(text='Hướng mặt vào camera và nhấn CHỤP khi sẵn sàng', size_hint=(1, 0.05), font_size='16sp', color=(1, 1, 0, 1))
        self.layout.add_widget(self.instruction)
        
        btn_layout = BoxLayout(size_hint=(1, 0.15), spacing=10)
        self.btn_switch = Button(text='🔄 ĐỔI CAM', font_size='18sp', background_color=(0.2, 0.6, 1, 1), size_hint=(0.4, 1))
        self.btn_switch.bind(on_release=self.switch_camera)
        btn_layout.add_widget(self.btn_switch)
        
        self.btn_capture = Button(text='📷 CHỤP ẢNH', font_size='18sp', background_color=(0.2, 0.8, 0.2, 1))
        self.btn_capture.bind(on_release=self.capture_photo)
        btn_layout.add_widget(self.btn_capture)
        
        btn_cancel = Button(text='HỦY', font_size='18sp', background_color=(0.8, 0.2, 0.2, 1))
        btn_cancel.bind(on_release=self.cancel)
        btn_layout.add_widget(btn_cancel)
        
        self.layout.add_widget(btn_layout)
        self.add_widget(self.layout)
        self.capture = None
        self.event = None
        self.current_frame = None
        self.captured_image = None
        self.callback = None
    
    def start_camera(self, callback):
        self.callback = callback
        self.captured_image = None
        self.capture = cv2.VideoCapture(self.camera_index)
        self.event = Clock.schedule_interval(self.update, 1.0 / 30.0)
        self.instruction.text = 'Hướng mặt vào camera và nhấn CHỤP khi sẵn sàng'
        self.instruction.color = (1, 1, 0, 1)
        if hasattr(self, 'btn_switch'): self.btn_switch.disabled = False
        if hasattr(self, 'btn_capture'): self.btn_capture.disabled = False

    def switch_camera(self, instance):
        if self.event: self.event.cancel()
        if self.capture: self.capture.release()
        self.camera_index = 1 if self.camera_index == 0 else 0
        self.capture = cv2.VideoCapture(self.camera_index)
        if not self.capture.isOpened():
            self.camera_index = 0
            self.capture = cv2.VideoCapture(0)
        self.event = Clock.schedule_interval(self.update, 1.0 / 30.0)

    def update(self, dt):
        ret, frame = self.capture.read()
        if ret:
            # 1. Lật hình soi gương NGAY LẬP TỨC
            frame = cv2.flip(frame, 1)
            self.current_frame = frame.copy()
            
            # 2. Vẽ khung hướng dẫn lên frame đã lật
            h, w = frame.shape[:2]
            center_x, center_y = w // 2, h // 2
            box_size = min(w, h) // 2
            cv2.ellipse(frame, (center_x, center_y), (box_size // 2, int(box_size * 0.65)), 0, 0, 360, (0, 255, 0), 3)
            cv2.putText(frame, "Dat mat vao trong khung", (center_x - 150, 50), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
            
            # 3. Hiển thị lên Kivy (Lật ngược lại để texture hiển thị đúng chiều)
            buf1 = cv2.flip(frame, 0)
            buf = buf1.tostring()
            texture = Texture.create(size=(frame.shape[1], frame.shape[0]), colorfmt='bgr')
            texture.blit_buffer(buf, colorfmt='bgr', bufferfmt='ubyte')
            self.image.texture = texture
    
    def capture_photo(self, instance):
        if self.current_frame is not None:
            self.captured_image = self.current_frame.copy()
            if self.event: self.event.cancel()
            if self.capture: self.capture.release()
            self.show_captured_image()
    
    def show_captured_image(self):
        # Hiển thị ảnh đã chụp (Lật ngược lại để texture hiển thị đúng chiều)
        buf1 = cv2.flip(self.captured_image, 0)
        buf = buf1.tostring()
        texture = Texture.create(size=(self.captured_image.shape[1], self.captured_image.shape[0]), colorfmt='bgr')
        texture.blit_buffer(buf, colorfmt='bgr', bufferfmt='ubyte')
        self.image.texture = texture
        self.instruction.text = 'Ảnh đã chụp! Bạn có muốn sử dụng ảnh này không?'
        self.instruction.color = (0, 1, 0, 1)
        if hasattr(self, 'btn_switch'): self.btn_switch.disabled = True
        if hasattr(self, 'btn_capture'): self.btn_capture.disabled = True
        
        # Thay đổi nút
        self.layout.remove_widget(self.layout.children[0])
        btn_layout = BoxLayout(size_hint=(1, 0.15), spacing=10)
        btn_use = Button(text='✓ SỬ DỤNG', font_size='18sp', background_color=(0.2, 0.8, 0.2, 1))
        btn_use.bind(on_release=self.use_photo)
        btn_retake = Button(text='↻ CHỤP LẠI', font_size='18sp', background_color=(0.9, 0.6, 0.1, 1))
        btn_retake.bind(on_release=self.retake)
        btn_cancel = Button(text='HỦY', font_size='18sp', background_color=(0.8, 0.2, 0.2, 1))
        btn_cancel.bind(on_release=self.cancel)
        btn_layout.add_widget(btn_use)
        btn_layout.add_widget(btn_retake)
        btn_layout.add_widget(btn_cancel)
        self.layout.add_widget(btn_layout)
    
    def use_photo(self, instance):
        if self.callback and self.captured_image is not None: self.callback(self.captured_image)
        self.manager.current = 'add_student'
    
    def retake(self, instance):
        self.layout.remove_widget(self.layout.children[0])
        # Tạo lại nút ban đầu
        btn_layout = BoxLayout(size_hint=(1, 0.15), spacing=10)
        self.btn_switch = Button(text='🔄 ĐỔI CAM', font_size='18sp', background_color=(0.2, 0.6, 1, 1), size_hint=(0.4, 1))
        self.btn_switch.bind(on_release=self.switch_camera)
        self.btn_capture = Button(text='📷 CHỤP ẢNH', font_size='18sp', background_color=(0.2, 0.8, 0.2, 1))
        self.btn_capture.bind(on_release=self.capture_photo)
        btn_cancel = Button(text='HỦY', font_size='18sp', background_color=(0.8, 0.2, 0.2, 1))
        btn_cancel.bind(on_release=self.cancel)
        btn_layout.add_widget(self.btn_switch)
        btn_layout.add_widget(self.btn_capture)
        btn_layout.add_widget(btn_cancel)
        self.layout.add_widget(btn_layout)
        self.start_camera(self.callback)
    
    def cancel(self, instance):
        if self.event: self.event.cancel()
        if self.capture: self.capture.release()
        self.manager.current = 'add_student'
    
    def on_leave(self):
        if self.event: self.event.cancel()
        if self.capture: self.capture.release()

# ============== SCREEN: THÊM SINH VIÊN (Giữ nguyên) ==============
class AddStudentScreen(Screen):
    def __init__(self, **kwargs):
        super(AddStudentScreen, self).__init__(**kwargs)
        self.layout = BoxLayout(orientation='vertical', padding=20, spacing=10)
        title = Label(text='THÊM SINH VIÊN MỚI', font_size='24sp', size_hint=(1, 0.1))
        self.layout.add_widget(title)
        form_layout = BoxLayout(orientation='vertical', spacing=10, size_hint=(1, 0.4))
        id_box = BoxLayout(orientation='horizontal', size_hint=(1, None), height=40)
        id_box.add_widget(Label(text='ID:', size_hint=(0.3, 1)))
        self.id_input = TextInput(multiline=False, size_hint=(0.7, 1))
        id_box.add_widget(self.id_input)
        form_layout.add_widget(id_box)
        name_box = BoxLayout(orientation='horizontal', size_hint=(1, None), height=40)
        name_box.add_widget(Label(text='Tên:', size_hint=(0.3, 1)))
        self.name_input = TextInput(multiline=False, size_hint=(0.7, 1))
        name_box.add_widget(self.name_input)
        form_layout.add_widget(name_box)
        btn_camera = Button(text='📷 CHỤP ẢNH TỪ CAMERA', size_hint=(1, None), height=50, background_color=(0.2, 0.8, 0.2, 1))
        btn_camera.bind(on_release=self.open_camera)
        form_layout.add_widget(btn_camera)
        btn_choose = Button(text='📁 CHỌN ẢNH CÓ SẴN', size_hint=(1, None), height=50, background_color=(0.2, 0.6, 1, 1))
        btn_choose.bind(on_release=self.choose_image)
        form_layout.add_widget(btn_choose)
        self.image_path_label = Label(text='Chưa có ảnh', size_hint=(1, None), height=30, color=(1, 1, 0, 1))
        form_layout.add_widget(self.image_path_label)
        self.layout.add_widget(form_layout)
        self.preview_image = Image(size_hint=(1, 0.3))
        self.layout.add_widget(self.preview_image)
        btn_add = Button(text='✓ THÊM SINH VIÊN', size_hint=(1, 0.1), font_size='18sp', background_color=(0.2, 0.8, 0.2, 1))
        btn_add.bind(on_release=self.add_student)
        self.layout.add_widget(btn_add)
        btn_back = Button(text='QUAY LẠI', size_hint=(1, 0.1), font_size='18sp', background_color=(0.8, 0.2, 0.2, 1))
        btn_back.bind(on_release=self.go_back)
        self.layout.add_widget(btn_back)
        self.add_widget(self.layout)
        self.selected_image_path = None
        self.captured_image = None
    def open_camera(self, instance):
        capture_screen = self.manager.get_screen('capture_photo')
        capture_screen.start_camera(self.on_photo_captured)
        self.manager.current = 'capture_photo'
    def on_photo_captured(self, image):
        self.captured_image = image
        self.selected_image_path = None
        self.image_path_label.text = 'Đã chụp ảnh từ camera ✓'
        self.image_path_label.color = (0, 1, 0, 1)
        self.show_preview(image)
    def show_preview(self, image):
        h, w = image.shape[:2]
        max_h = 300
        if h > max_h:
            ratio = max_h / h
            new_w = int(w * ratio)
            image = cv2.resize(image, (new_w, max_h))
        buf1 = cv2.flip(image, 0)
        buf = buf1.tostring()
        texture = Texture.create(size=(image.shape[1], image.shape[0]), colorfmt='bgr')
        texture.blit_buffer(buf, colorfmt='bgr', bufferfmt='ubyte')
        self.preview_image.texture = texture
    def choose_image(self, instance):
        content = BoxLayout(orientation='vertical')
        filechooser = FileChooserListView(filters=['*.jpg', '*.jpeg', '*.png'])
        content.add_widget(filechooser)
        btn_box = BoxLayout(size_hint=(1, 0.1), spacing=10)
        btn_select = Button(text='Chọn')
        btn_cancel = Button(text='Hủy')
        btn_box.add_widget(btn_select)
        btn_box.add_widget(btn_cancel)
        content.add_widget(btn_box)
        popup = Popup(title='Chọn ảnh', content=content, size_hint=(0.9, 0.9))
        def select_file(instance):
            if filechooser.selection:
                self.selected_image_path = filechooser.selection[0]
                self.captured_image = None
                self.image_path_label.text = f'Đã chọn: {os.path.basename(self.selected_image_path)}'
                self.image_path_label.color = (0, 1, 0, 1)
                img = cv2.imread(self.selected_image_path)
                self.show_preview(img)
            popup.dismiss()
        btn_select.bind(on_release=select_file)
        btn_cancel.bind(on_release=popup.dismiss)
        popup.open()
    def add_student(self, instance):
        id_text = self.id_input.text.strip()
        name_text = self.name_input.text.strip()
        if not id_text or not name_text:
            self.show_popup('Lỗi', 'Vui lòng nhập đầy đủ ID và Tên!')
            return
        if not self.selected_image_path and self.captured_image is None:
            self.show_popup('Lỗi', 'Vui lòng chụp ảnh hoặc chọn ảnh!')
            return
        try: id_num = int(id_text)
        except ValueError:
            self.show_popup('Lỗi', 'ID phải là số!')
            return
        if not os.path.exists('students'): os.makedirs('students')
        new_filename = f"{id_num}.{name_text}.jpg"
        dest_path = os.path.join('students', new_filename)
        if self.captured_image is not None: cv2.imwrite(dest_path, self.captured_image)
        else: shutil.copy(self.selected_image_path, dest_path)
        self.show_popup('Thành công', f'Đã thêm sinh viên: {name_text} (ID: {id_num})')
        self.id_input.text = ''
        self.name_input.text = ''
        self.selected_image_path = None
        self.captured_image = None
        self.image_path_label.text = 'Chưa có ảnh'
        self.image_path_label.color = (1, 1, 0, 1)
        self.preview_image.texture = None
    def show_popup(self, title, message):
        popup = Popup(title=title, content=Label(text=message), size_hint=(0.7, 0.3))
        popup.open()
    def go_back(self, instance): self.manager.current = 'menu'

# ============== SCREEN: DANH SÁCH SINH VIÊN (Giữ nguyên) ==============
class StudentListScreen(Screen):
    def __init__(self, **kwargs):
        super(StudentListScreen, self).__init__(**kwargs)
        self.layout = BoxLayout(orientation='vertical', padding=10, spacing=10)
        title = Label(text='DANH SÁCH SINH VIÊN', font_size='24sp', size_hint=(1, 0.1))
        self.layout.add_widget(title)
        scroll = ScrollView(size_hint=(1, 0.75))
        self.list_layout = GridLayout(cols=1, spacing=5, size_hint_y=None)
        self.list_layout.bind(minimum_height=self.list_layout.setter('height'))
        scroll.add_widget(self.list_layout)
        self.layout.add_widget(scroll)
        btn_back = Button(text='QUAY LẠI', size_hint=(1, 0.15), font_size='18sp', background_color=(0.8, 0.2, 0.2, 1))
        btn_back.bind(on_release=self.go_back)
        self.layout.add_widget(btn_back)
        self.add_widget(self.layout)
    def refresh_list(self):
        self.list_layout.clear_widgets()
        if not os.path.exists('students'):
            self.list_layout.add_widget(Label(text='Chưa có sinh viên nào', size_hint_y=None, height=40))
            return
        files = [f for f in os.listdir('students') if f.endswith('.jpg')]
        if len(files) == 0:
            self.list_layout.add_widget(Label(text='Chưa có sinh viên nào', size_hint_y=None, height=40))
            return
        for filename in sorted(files):
            parts = filename.split('.')
            if len(parts) >= 2:
                id_num = parts[0]
                name = parts[1]
                item = Label(text=f'ID: {id_num} - Tên: {name}', size_hint_y=None, height=40, font_size='16sp')
                self.list_layout.add_widget(item)
    def go_back(self, instance): self.manager.current = 'menu'

# ============== SCREEN: LỊCH SỬ ĐIỂM DANH (Có xuất CSV) ==============
class HistoryScreen(Screen):
    def __init__(self, **kwargs):
        super(HistoryScreen, self).__init__(**kwargs)
        self.layout = BoxLayout(orientation='vertical', padding=10, spacing=10)
        title = Label(text='LỊCH SỬ ĐIỂM DANH', font_size='24sp', size_hint=(1, 0.1))
        self.layout.add_widget(title)
        btn_export = Button(text='📊 XUẤT FILE EXCEL (.CSV)', size_hint=(1, 0.1), font_size='18sp', background_color=(0.2, 0.6, 1, 1))
        btn_export.bind(on_release=self.export_to_csv)
        self.layout.add_widget(btn_export)
        scroll = ScrollView(size_hint=(1, 0.65))
        self.history_layout = GridLayout(cols=1, spacing=5, size_hint_y=None)
        self.history_layout.bind(minimum_height=self.history_layout.setter('height'))
        scroll.add_widget(self.history_layout)
        self.layout.add_widget(scroll)
        btn_back = Button(text='QUAY LẠI', size_hint=(1, 0.15), font_size='18sp', background_color=(0.8, 0.2, 0.2, 1))
        btn_back.bind(on_release=self.go_back)
        self.layout.add_widget(btn_back)
        self.add_widget(self.layout)
    def export_to_csv(self, instance):
        if not os.path.exists('attendance_logs'):
            self.show_popup("Lỗi", "Chưa có dữ liệu để xuất!")
            return
        files = sorted([f for f in os.listdir('attendance_logs') if f.endswith('.txt')])
        if not files:
            self.show_popup("Lỗi", "Chưa có dữ liệu để xuất!")
            return
        export_filename = f"Bao_cao_diem_danh_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
        try:
            with open(export_filename, mode='w', newline='', encoding='utf-8-sig') as csv_file:
                writer = csv.writer(csv_file)
                writer.writerow(['Ngày', 'Giờ', 'ID Sinh Viên', 'Tên Sinh Viên'])
                for log_file in files:
                    date_str = log_file.replace('.txt', '')
                    with open(os.path.join('attendance_logs', log_file), 'r', encoding='utf-8') as txt_file:
                        for line in txt_file:
                            parts = line.strip().split(',')
                            if len(parts) == 3:
                                time_str, id_num, name = parts
                                writer.writerow([date_str, time_str, id_num, name])
            self.show_popup("Thành công", f"Đã xuất file:\n{export_filename}")
        except Exception as e:
            self.show_popup("Lỗi", f"Không thể xuất file: {e}")
    def show_popup(self, title, message):
        popup = Popup(title=title, content=Label(text=message), size_hint=(0.7, 0.3))
        popup.open()
    def load_history(self):
        self.history_layout.clear_widgets()
        if not os.path.exists('attendance_logs'):
            self.history_layout.add_widget(Label(text='Chưa có lịch sử điểm danh', size_hint_y=None, height=40))
            return
        files = sorted([f for f in os.listdir('attendance_logs') if f.endswith('.txt')], reverse=True)
        if len(files) == 0:
            self.history_layout.add_widget(Label(text='Chưa có lịch sử điểm danh', size_hint_y=None, height=40))
            return
        for log_file in files[:10]:
            date = log_file.replace('.txt', '')
            date_label = Label(text=f'\n--- {date} ---', size_hint_y=None, height=40, font_size='18sp', bold=True)
            self.history_layout.add_widget(date_label)
            with open(os.path.join('attendance_logs', log_file), 'r', encoding='utf-8') as f:
                for line in f:
                    parts = line.strip().split(',')
                    display_text = line.strip()
                    if len(parts) == 3:
                        display_text = f"{parts[0]} - ID: {parts[1]} - Tên: {parts[2]}"
                    item = Label(text=display_text, size_hint_y=None, height=30, font_size='14sp')
                    self.history_layout.add_widget(item)
    def go_back(self, instance): self.manager.current = 'menu'

# ============== APP CHÍNH ==============
class AttendanceApp(App):
    def build(self):
        sm = ScreenManager()
        sm.add_widget(MenuScreen(name='menu'))
        sm.add_widget(AttendanceScreen(name='attendance'))
        sm.add_widget(CapturePhotoScreen(name='capture_photo'))
        sm.add_widget(AddStudentScreen(name='add_student'))
        sm.add_widget(StudentListScreen(name='student_list'))
        sm.add_widget(HistoryScreen(name='history'))
        return sm

if __name__ == '__main__':
    AttendanceApp().run()