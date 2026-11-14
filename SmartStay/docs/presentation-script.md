# SmartStay – Kịch bản trình bày (Guest → Thiết bị → Staff → Admin)

Thời lượng gợi ý: **20–25 phút** (bản 10 phút ở cuối). Mật khẩu mọi tài khoản demo: `12345`.
Danh sách test case chi tiết: `docs/demo-test-plan.md`.

> Quy tắc vàng: đồng hồ ảo **chỉ chạy tới** và chung cho cả hệ thống; **Reset không hoàn tác dữ liệu**.
> Vì vậy kịch bản đi theo đúng dòng thời gian của một lượt ở: đặt → trả tiền → vào phòng → trả phòng →
> dọn phòng → quản lý tổng kết. Tập dượt 1 lần trên bản backup DB.

---

## 0. Chuẩn bị (trước giờ lên trình bày, ~15 phút)

**Mạng và server**
1. Laptop bật **Mobile hotspot** `anhyeuem` (2.4 GHz). Điện thoại + 2 ESP32 vào Wi-Fi này.
2. Chạy Tomcat. Điện thoại mở `http://192.168.137.1:8080/SmartStay/` (nếu timeout: mở firewall cổng 8080).

**Thiết bị** (cấp nguồn sớm: PIR cần ~60 s để ổn định)
- Khóa cửa: LCD sáng, Devices hiện **Online** trong 20–30 s.
- ESP32-CAM: đèn flash nháy 2 lần (Wi-Fi OK); mở `http://192.168.137.20/capture` thấy ảnh.

**Cửa sổ trình chiếu**
| Màn hình | Tài khoản | Vai |
|---|---|---|
| Điện thoại (chiếu qua máy chiếu nếu được) | `guest1@smartstay.local` | Khách |
| Laptop – cửa sổ 1 | `admin1@gmail.com` | Admin (để sẵn Dashboard) |
| Laptop – cửa sổ 2 (trình duyệt khác) | `staff1@gmail.com` (Sam Wilson) | Staff (để sẵn My cleaning tasks) |

**Booking chuẩn bị sẵn** (đã thanh toán, tất cả **ngày mai**, khác phòng):
| Mã | Slot | Vai trò trong kịch bản |
|---|---|---|
| B-Main | S1 | Booking "nhân vật chính" (nếu muốn tạo trực tiếp lúc trình bày thì để trống, tạo ở Cảnh 1) |
| B-Late | S1 | Không check-in → minh họa **No-show** |
| B-Far | ≥ 3 ngày sau | Minh họa hủy > 48 h hoàn tiền 100% |

---

## Cảnh 1 – KHÁCH: tìm và đặt phòng (điện thoại, ~5 phút)

| Thao tác | Lời nói gợi ý |
|---|---|
| Trang chủ → ô tìm kiếm ở hero → chọn ngày mai | "SmartStay cho thuê phòng thông minh theo **khung giờ**: sáng, chiều, tối, qua đêm. Khách tự đặt, tự trả tiền, tự vào phòng bằng mã PIN – không cần lễ tân." |
| Rooms: lọc theo slot / giá / sức chứa | "Bộ lọc chạy ngay trên trang, giá luôn hiển thị dạng VND." |
| Mở 1 phòng: ảnh, tiện nghi, review | — |
| Slot picker: chọn 1 slot → chọn thêm slot liền kề | "Chọn từ 2 slot trở lên được **combo discount**, thanh đặt phòng dưới đáy màn hình cập nhật tạm tính ngay." |
| Chỉ vào slot đã kín | "Slot đã có người đặt bị khóa, không thể đặt trùng." |
| Continue → Checkout: QR + đếm ngược 5:00 | "Link thanh toán sống 5 phút, slot được **giữ 15 phút** cho khách. Nếu quá hạn hệ thống tự nhả slot." |
| Mở tab **DEMO** → *Simulate successful transfer* | "Đây là cổng thanh toán kiểu PayOS ở chế độ sandbox. Thực tế ngân hàng gọi webhook; ở đây ta giả lập giao dịch thành công." |
| Trang tự chuyển → **Digital Keycard** | "Không cần tải lại trang – server đẩy kết quả qua **WebSocket**. Khách nhận ngay mã PIN 6 số, nút copy và khung giờ PIN có hiệu lực." |
| My bookings → tabs Upcoming / Past / Cancelled | — |
| Mở B-Far → Cancel | "Chính sách hủy: **hoàn 100% nếu hủy trước 48 giờ** so với slot đầu tiên; dưới 48 giờ là *Non-refundable* và hệ thống cảnh báo ngay **trước khi trả tiền**." |

👉 Admin (laptop, chỉ cần liếc): Dashboard / Bookings đã có booking mới, không cần F5.

---

## Cảnh 2 – THIẾT BỊ: vào phòng bằng PIN (khóa cửa thật, ~5 phút)

| Thao tác | Lời nói gợi ý |
|---|---|
| Admin → **Devices**: khóa cửa + camera Online | "Hai thiết bị thật: khóa cửa ESP32 (bàn phím, servo, LCD, cảm biến cửa, PIR + siêu âm, còi) và ESP32-CAM. Mỗi 20 giây gửi heartbeat về server." |
| Gõ PIN của B-Main lên bàn phím **ngay bây giờ** | "Mới là hôm nay, chưa tới giờ nhận phòng → **từ chối**. PIN được kiểm tra trên server theo giờ của hệ thống, không lưu sẵn trong thiết bị." |
| Khách mở DEMO → chọn B-Main → **Check-in now** (đọc hộp xác nhận) | "Để demo không phải chờ tới mai, hệ thống có **đồng hồ ảo**. Banner trên cùng cho biết đang ở *Simulated time*. Đồng hồ chỉ tiến, không lùi." |
| Gõ lại PIN (gõ đủ 6 số là tự kiểm tra) | "Server trả lời trong vài chục mili giây → servo mở, đèn xanh. Booking tự chuyển **Checked in**." |
| Chỉ sang Dashboard admin | "Admin thấy trạng thái phòng đổi **realtime**." |
| Mở cửa, đóng lại | "Cảm biến từ báo cửa đóng → tự khóa lại sau 1,5 giây. Để cửa mở quá lâu thì còi nhắc và có cảnh báo." |
| (tùy chọn) Bấm nút thoát bên trong | "Từ trong phòng ra không cần PIN." |
| **Gõ sai PIN 5 lần** | "Bàn phím khóa 30 giây, còi hú, **camera chụp ảnh kèm flash**, gửi ảnh lên server và e-mail cho chủ nhà." |
| Admin → **Alerts**: cảnh báo mới hiện ngay, mở drawer xem **ảnh camera** | "Cảnh báo đẩy realtime, có ảnh của người đứng trước cửa." |
| Acknowledge → Resolve | "Xử lý ngay tại chỗ, số trên chuông giảm." |
| Devices → **Silence** (nếu còi còn kêu), **Unlock** (bấm 2 lần xác nhận) | "Admin mở cửa / tắt còi từ xa qua MQTT, lệnh có **chữ ký HMAC** để không ai giả mạo được." |
| (tùy chọn) Khóa đang đóng, kéo cửa ra + đứng trước cảm biến | "Cửa bị mở khi chưa mở khóa + có người → cảnh báo **mức nghiêm trọng**, camera chụp liên tiếp 3 ảnh." |
| Admin → **Activity logs** | "Mọi lần mở cửa, sai PIN, chuyển động đều được ghi, nhóm theo ngày, có ảnh." |

---

## Cảnh 3 – TRẢ PHÒNG và STAFF dọn phòng (~4 phút)

| Thao tác | Lời nói gợi ý |
|---|---|
| Khách: DEMO → **15 min before check-out** | "Hệ thống chạy job nhắc khách sắp hết giờ." |
| Admin → Bookings: **B-Late → No-show** | "Booking không ai nhận phòng quá 30 phút sau giờ bắt đầu → tự chuyển **No-show**, có cảnh báo cho quản lý." |
| Khách: DEMO → **Check-out now** | "Hết giờ: booking hoàn tất, PIN của khách **hết hiệu lực ngay**, phòng chuyển *Needs cleaning*. Khóa cửa hiện thông báo trên LCD." |
| Cửa sổ Staff: trang **tự tải lại**, có task mới | "Không cần admin giao: phòng vừa trả là task dọn phòng **tự giao cho nhân viên trực** (Sam Wilson), ca 2 tiếng tính từ lúc trả phòng." |
| Gõ lại PIN của khách ở khóa | "PIN khách đã hết hạn → từ chối." |
| Staff mở task: thấy **PIN cửa của nhân viên** → gõ ở khóa | "Nhân viên có PIN riêng, chỉ dùng được trong ca." |
| Start cleaning → Mark as clean (bấm 1 lần, chờ) → bấm lại 2 lần | "Nút hoàn thành cần **bấm lần nữa trong 3 giây** để tránh bấm nhầm." |
| Admin Dashboard: phòng → **Available** | "Phòng sẵn sàng cho khách tiếp theo – vòng đời khép kín." |
| Khách: viết **review** cho B-Main | "Chỉ booking đã ở xong mới được đánh giá." |

---

## Cảnh 4 – ADMIN: quản lý tổng thể (laptop, ~5 phút)

| Màn hình | Điểm nhấn |
|---|---|
| **Dashboard** | 4 KPI, *Needs attention*, ô trạng thái từng phòng, biểu đồ cột – cập nhật trực tiếp |
| **Bookings** | Chip lọc theo trạng thái (số đếm), tìm kiếm, drawer chi tiết có thông tin khách, check-in / check-out tay |
| **Payments** | Giao dịch thành công / thất bại / hoàn tiền (thấy B-Far *Refund pending*) |
| **Rooms → Slots & Pricing** | Giờ và giá từng slot có thể khác nhau theo phòng |
| **Assignments** | Task tự giao vẫn sửa / giao tay được; giao trùng ca bị chặn |
| **Users** | Tạo, khóa tài khoản; tài khoản Owner được bảo vệ |
| **Reviews** | Ẩn review có lý do |
| **Revenue** | 7 / 30 / 90 ngày, **Export CSV** |
| **Alert rules** | Ngưỡng cảnh báo cho thiết bị |
| Phân quyền (nói nhanh) | Guest / Staff / Admin / Owner – mỗi vai một trang chủ, truy cập trái phép bị 403 |

Kết: bấm **Reset** – "Đồng hồ về giờ thật; dữ liệu demo vẫn giữ để đối chiếu."

---

## Câu hỏi hay gặp – trả lời sẵn
| Câu hỏi | Trả lời |
|---|---|
| Thanh toán có thật không? | Sandbox kiểu PayOS (QR, hết hạn link, webhook), không trừ tiền thật. |
| Khóa cửa có thật không? | Thật: ESP32 gọi API server; PIN kiểm tra trên server theo đồng hồ hệ thống; lệnh từ xa ký HMAC. |
| Đồng hồ ảo ảnh hưởng gì? | Chỉ logic nghiệp vụ (giờ check-in/out, PIN, no-show). Hạn giữ slot / link thanh toán, heartbeat, log thiết bị dùng **giờ thật**. |
| Mất mạng Internet thì sao? | Web chạy 100% offline (font, icon, thư viện đều local); chỉ e-mail và MQTT broker cần Internet. |
| Công nghệ? | JSP/Servlet, Tomcat 9, Java 8, SQL Server, WebSocket, MQTT, ESP32 / ESP32-CAM. |

---

## Bản 10 phút
Cảnh 1 (đặt + trả tiền + PIN) → Cảnh 2 (PIN bị từ chối → Check-in now → mở cửa → sai 5 lần + ảnh camera
→ Resolve) → Cảnh 3 (Check-out now → task tự hiện bên Staff → Mark as clean) → Dashboard + Revenue → Reset.

## Phương án dự phòng
| Sự cố | Xử lý |
|---|---|
| Thiết bị Offline | Kiểm tra hotspot + Tomcat; demo phần web trước, quay lại thiết bị sau |
| Camera không chụp | Mở `http://192.168.137.20/capture`; nếu ảnh trắng xóa, chụp lại lần nữa |
| Điện thoại không vào được web | Mở firewall cổng 8080 hoặc dùng cửa sổ laptop thu nhỏ (< 768 px) thay điện thoại |
| E-mail chậm | Không chờ – ảnh đã có trong Alerts |
