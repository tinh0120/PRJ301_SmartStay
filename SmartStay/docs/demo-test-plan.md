# SmartStay – Kịch bản demo đầy đủ (test case theo thứ tự chạy)

> Vì sao phải theo thứ tự: đồng hồ ảo **chỉ chạy tới** và là **chung cho cả hệ thống**; **Reset không hoàn tác
> dữ liệu**. Mọi việc cần "giờ thật" làm ở Phần A, mọi việc cần nhảy giờ làm ở Phần B, xong mới Reset.
> Backup DB trước khi demo. Mật khẩu mọi tài khoản demo: `12345`.

## 0. Chuẩn bị

| Cửa sổ | Tài khoản |
|---|---|
| W1 Khách | `guest1@smartstay.local` |
| W2 Admin | `admin1@gmail.com` (ẩn danh) |
| W3 Staff | `staff1@gmail.com` – Sam Wilson (trình duyệt khác) |
| (W4) Owner | `owner@gmail.com` – chỉ cần cho phần phân quyền |

Phần cứng (nếu có): bật Mobile Hotspot laptop `anhyeuem` (2.4 GHz), Tomcat chạy, ESP32 khóa cửa + ESP32-CAM
cấp nguồn, kiểm tra trang Devices hiện **Online**.

Booking cần tạo trong Phần A (đều **ngày mai**, khác phòng nếu cùng slot):

| Mã | Slot | Dùng cho |
|---|---|---|
| B1 | S1 | Luồng ở chính: Demo jump 4 mốc |
| B2 | S1 | Admin check-in tay → tự check-out cùng B1 → task hết hạn |
| B3 | S1 | Admin check-in / check-out tay |
| B5 | S1 | Không check-in → **No-show** |
| B4 | S3 hoặc S4 | Mốc nhảy giờ "xa" (> 2 giờ sau khi S1 kết thúc) |
| B6 | ≥ 3 ngày sau | Hủy > 48 h → hoàn 100% |
| B7 | ngày mai | Hủy / thanh toán < 48 h → "Non-refundable" |

---

## Phần A – Giờ thật (chưa nhảy giờ)

### A1. Đăng nhập, đăng ký, phân quyền
| ID | Bước | Kết quả mong đợi |
|---|---|---|
| AUTH-01 | Đăng ký tài khoản khách mới ở `/register` | Tạo được, đăng nhập được |
| AUTH-02 | Đăng nhập sai mật khẩu | Báo lỗi, không vào được |
| AUTH-03 | Đăng nhập từng vai trò | Mỗi vai về đúng trang chủ của mình (Guest → trang khách, Staff → My cleaning tasks, Admin/Owner → Dashboard) |
| AUTH-04 | Khách gõ `/admin/dashboard` | 403 |
| AUTH-05 | Staff gõ `/admin/bookings` | Về trang staff |
| AUTH-06 | Admin mở `/staff/assignments` | 403 |
| AUTH-07 | Chưa đăng nhập mở `/guest/bookings` | Chuyển về Login |
| AUTH-08 | Admin vào Users, thử sửa tài khoản Owner | Bị chặn (Owner được bảo vệ) |
| AUTH-09 | Khách mở booking của khách khác (đổi id trên URL) | Không xem được |

### A2. Trang công khai và tìm phòng
| ID | Bước | Kết quả |
|---|---|---|
| PUB-01 | Trang chủ: ô tìm kiếm ở hero | Chuyển sang Rooms với bộ lọc đã chọn |
| PUB-02 | Rooms: lọc theo ngày, slot, giá, sức chứa | Danh sách đổi đúng; không có kết quả thì hiện thông báo trống |
| PUB-03 | Room detail: ảnh (lightbox), tabs, reviews | Hiển thị đủ, tiền dạng "259,000 VND" |
| PUB-04 | Ngắt mạng Internet, tải lại trang | Vẫn đủ font, icon, lịch (100% offline) |

### A3. Đặt phòng và thanh toán (W1)
| ID | Bước | Kết quả |
|---|---|---|
| BOOK-01 | Chọn 1 slot | Thanh booking hiện tạm tính |
| BOOK-02 | Chọn 2+ slot liền nhau | Combo discount hiện ở thanh booking |
| BOOK-03 | Chọn slot đã có người đặt | Không chọn được (slot khóa) |
| BOOK-04 | Continue → checkout | QR card + đồng hồ đếm ngược 5 phút |
| PAY-01 | DEMO → **Simulate successful transfer** (tạo B1) | Trang tự chuyển sang kết quả, hiện Digital Keycard (PIN 6 số, nút copy, khung giờ hiệu lực) |
| PAY-02 | Booking khác → **Simulate failure** | Báo thất bại, slot được nhả |
| PAY-03 | Booking khác → để quá 5 phút không trả | Link hết hạn; quá 15 phút slot được nhả |
| PAY-04 | W2 Admin → Payments | Thấy giao dịch thành công/thất bại |
| PAY-05 | Lặp PAY-01 để có B2, B3, B4, B5, B6, B7 | — |
| BOOK-05 | My bookings: tabs Upcoming / Past / Cancelled / All | Booking nằm đúng tab |

### A4. Chính sách hủy
| ID | Bước | Kết quả |
|---|---|---|
| CAN-01 | Hủy B6 (> 48 h) | Hoàn 100%, admin thấy "Refund pending" |
| CAN-02 | Mở checkout / hộp hủy B7 (< 48 h) | Hiện "Non-refundable" trước khi trả và khi hủy |

### A5. Back office – quản trị (W2)
| ID | Bước | Kết quả |
|---|---|---|
| ADM-01 | Dashboard | 4 KPI, Needs attention, room tiles, biểu đồ cột |
| ADM-02 | Bookings: chip trạng thái, tìm kiếm, menu dòng, drawer chi tiết | Số đếm đúng từng trạng thái; chi tiết có thông tin khách |
| ADM-03 | Admin check-in B3 lúc này | Báo "Too early to check in" |
| ADM-04 | Rooms: sửa phòng, tabs | Lưu được |
| ADM-05 | Slots & Pricing: đổi giá/giờ slot riêng cho 1 phòng | Giá mới hiện ở trang khách |
| ADM-06 | Users: tạo / khóa 1 tài khoản | Tài khoản bị khóa không đăng nhập được |
| ADM-07 | Reviews: ẩn 1 review có lý do | Review biến mất ở trang công khai |
| ADM-08 | Revenue: 7 / 30 / 90 ngày, Export CSV | Số liệu đổi, tải được CSV |
| ADM-09 | Alert rules (thresholds): sửa 1 ngưỡng | Lưu được |
| ADM-10 | Thu nhỏ màn hình < 768 px | Bảng thành dạng thẻ, không cuộn ngang |

### A6. IoT – khóa cửa và camera (cần phần cứng)
| ID | Bước | Kết quả |
|---|---|---|
| IOT-01 | Devices | Khóa cửa và camera **Online** (heartbeat) |
| IOT-02 | Nhập PIN của B1 ở khóa **trước giờ check-in** | Từ chối (PIN chưa tới hiệu lực) |
| IOT-03 | Nhập sai PIN 5 lần | Bàn phím khóa; Alerts có cảnh báo mới ngay (realtime); CAM chụp ảnh; ảnh hiện trong drawer cảnh báo và Activity logs; có e-mail |
| IOT-04 | Alerts: Acknowledge → Resolve | Đổi trạng thái tại chỗ, badge giảm |
| IOT-05 | Devices → Unlock (bấm 2 lần xác nhận) | Khóa mở, có DEVICE_ACK |
| IOT-06 | Devices → Silence | Còi tắt |
| IOT-07 | Activity logs: nhóm theo ngày, mở drawer | Thấy lần mở cửa / sai PIN / chuyển động |
| IOT-08 | Mở `http://192.168.137.20/capture` | CAM trả ảnh |

---

## Phần B – Nhảy giờ (W1 mở DEMO, chọn booking)

| ID | Bước | Kết quả |
|---|---|---|
| TIME-01 | B1 → **15 min before check-in** | Banner "Simulated time …" hiện; hộp xác nhận báo số booking khác bị ảnh hưởng; B1 vẫn Confirmed |
| TIME-02 | B1 → **Check-in now** | Tới giờ ở |
| IOT-09 | Nhập PIN B1 ở khóa | Mở cửa, B1 → Checked in (realtime trên Dashboard/Bookings) |
| ADM-11 | Admin check-in tay B2, B3 | Thành công (không còn "Too early") |
| CLN-01 | Admin check-out tay B3 | Phòng → Needs cleaning; **trang Sam tự tải lại, có task phòng của B3** |
| CLN-02 | Sam mở task: thấy PIN cửa; Start cleaning | Thành công (đang trong ca) |
| CLN-03 | Mark as clean bấm 1 lần rồi chờ > 3 s | Không hoàn thành |
| CLN-04 | Mark as clean bấm 2 lần trong 3 s | Task Done, phòng → Available |
| IOT-10 | Sam nhập PIN staff ở khóa (trong ca) | Mở cửa |
| TIME-03 | B1 → **15 min before check-out** | Job nhắc check-out chạy; **B5 → No-show** (quá 30 phút sau giờ bắt đầu mà chưa check-in), Alerts có cảnh báo no-show |
| TIME-04 | B1 → **Check-out now** | B1 và B2 cùng → Checked out; 2 phòng Needs cleaning |
| CLN-05 | Trang Sam | Nhận **2 task cùng lúc** (không bị chặn trùng ca), người giao = Owner |
| CLN-06 | Sam làm xong task phòng B1; **bỏ dở task phòng B2** | — |
| REV-01 | W1 viết review cho B1 (đã check-out) | Review hiện ở Room detail và admin Reviews |
| REV-02 | Thử review B5 (no-show) | Không được |
| TIME-05 | B4 → **Check-in now** (đã quá ca 2 giờ) | Task phòng B2 → **Past/Expired**, có cảnh báo "cleaning session expired"; PIN staff hết hạn |
| CLN-07 | Admin giao tay lại phòng B2 cho Sam, ca bao trùm giờ ảo | Tạo được; giao thêm ca trùng giờ → báo lỗi trùng ca |
| CLN-08 | Admin giao 1 task ca ở tương lai (3 h sau giờ ảo); Sam bấm Start | "Cleaning can only start during your shift" |
| TIME-06 | **Reset** | Banner biến mất, giờ thật; dữ liệu vẫn giữ nguyên |
| DEMO-01 | Đăng nhập Staff | Không thấy tab DEMO; gọi `/demo/state` → 403 |

---

## Bản rút gọn (~10 phút)
AUTH-03 → BOOK-02 → PAY-01 → CAN-01/CAN-02 → TIME-01..02 → IOT-09 → TIME-03 (no-show) → TIME-04 → CLN-05
→ CLN-04 → REV-01 → IOT-03 (sai PIN 5 lần + ảnh CAM) → IOT-04 → ADM-01 → ADM-08 → TIME-06.

## Không demo được trên UI (chỉ nói)
- Phòng đã có task đang mở thì không tạo thêm task tự động.
- `demo.mode=false` (web.xml) → `/demo/*` trả 404, tab DEMO biến mất.
- `AUTO_CLEANING_STAFF_EMAIL = ""` → tắt tự giao task (phải build lại).
