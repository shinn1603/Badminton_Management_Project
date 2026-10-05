# Hướng Dẫn Cài Đặt Cơ Sở Dữ Liệu SQL Server - UTE Sport

Tài liệu này hướng dẫn thiết lập và khởi tạo cơ sở dữ liệu `badminton` đồng bộ 100% theo **Chương 5: Thiết kế dữ liệu** của tệp đặc tả Word dự án và 25 màn hình giao diện web.

---

## 1. Thông tin cấu hình kết nối (theo `application.properties`)
* **Hệ quản trị CSDL:** Microsoft SQL Server 2022 / 2019
* **Host / Port:** `localhost:1433`
* **Tên Database:** `badminton`
* **Tài khoản:** `sa`
* **Mật khẩu:** `123456`

---

## 2. Cách chạy file khởi tạo `database/init_badminton.sql`

### Cách 1: Chạy bằng lệnh PowerShell (Khuyên dùng - UTF-8 chuẩn)
Mở PowerShell tại thư mục gốc dự án và chạy:
```powershell
sqlcmd -f 65001 -S localhost,1433 -U sa -P 123456 -i database\init_badminton.sql
```
*(Tham số `-f 65001` đảm bảo nạp tiếng Việt có dấu UTF-8 chuẩn xác, không bị lỗi font).*

### Cách 2: Sử dụng SQL Server Management Studio (SSMS)
1. Mở **SSMS** và kết nối tới `localhost:1433` với tài khoản `sa` / `123456`.
2. Mở tệp `database/init_badminton.sql`.
3. Nhấn **Execute (F5)** để khởi tạo tự động.

---

## 3. Kiến trúc CSDL: 24 Bảng thực thể & 24 View tiếng Việt (Chương 5 File Word)

Hệ thống hỗ trợ song song 2 cơ chế truy vấn:
1. **Tên bảng Tiếng Anh:** Dành cho Spring Boot Data JPA & REST API (`branches`, `courts`, `users`, `bookings`, `products`, `invoices`, v.v.).
2. **Tên View Tiếng Việt:** Ánh xạ 1-1 theo đúng tệp Word đặc tả (`ChiNhanh`, `San`, `KhungGio`, `BangGia`, `KhachHang`, `DonDatSan`, `DungCu`, `SanPham`, `HoaDon`, `NhanVien`, `TaiKhoan`, `GiaiDau`, v.v.).

| STT | Bảng Tiếng Anh (JPA / API) | View Tiếng Việt (File Word Ch.5) | Mô tả nghiệp vụ |
| :--- | :--- | :--- | :--- |
| 1 | `branches` | `ChiNhanh` | Quản lý 4 chi nhánh chuỗi sân (Thủ Đức, Bình Thạnh, Q.9, Gò Vấp) |
| 2 | `courts` | `San` | Danh sách 26 sân cầu lông (VIP Yonex, BWF tiêu chuẩn, Taraflex) |
| 3 | `time_slots` | `KhungGio` | 4 khung giờ hoạt động (Sáng, Ban ngày, Giờ vàng cao điểm, Tối) |
| 4 | `pricing_rules` | `BangGia` | Chính sách giá theo chi nhánh, khung giờ, ngày thường / cuối tuần |
| 5 | `customers` | `KhachHang` | Hội viên tích điểm (Đồng, Bạc, Vàng, Kim Cương), giờ chơi lũy kế |
| 6 | `bookings` | `DonDatSan` | Đơn đặt sân (Đang sử dụng, Đã xác nhận, Chờ xác nhận, Đã cọc 30%, Hủy) |
| 7 | `equipment` | `DungCu` | Dụng cụ cho thuê (Vợt Yonex Astrox 77/88, Victor, ống cầu) |
| 8 | `products` | `SanPham` | Đồ uống và phụ kiện quầy (Revive, Pocari, Aquafina, quấn cán VS) |
| 9 | `invoices` | `HoaDon` | Hóa đơn thanh toán POS (Tiền sân, dụng cụ, đồ uống, chiết khấu VIP) |
| 10 | `invoice_equipment_details` | `ChiTietThueDungCu` | Chi tiết dụng cụ thuê trên từng hóa đơn |
| 11 | `invoice_product_details` | `ChiTietBanSanPham` | Chi tiết sản phẩm bán ra trên từng hóa đơn |
| 12 | `booking_services` | *(Tương thích ngược)* | Dịch vụ đi kèm đơn đặt sân |
| 13 | `staff` | `NhanVien` | Nhân sự các chi nhánh (Quản lý, Thu ngân POS, Kỹ thuật sân) |
| 14 | `users` | `TaiKhoan` | Tài khoản đăng nhập & RBAC phân quyền 5 vai trò hệ thống |
| 15 | `tournaments` | `GiaiDau` | Giải đấu phong trào (UTE Open 2026, Cúp Doanh Nghiệp & Sinh Viên) |
| 16 | `tournament_registrations` | `DangKyGiaiDau` | Danh sách các cặp VĐV đăng ký tham gia thi đấu |
| 17 | `payments` | `ThanhToan` | Giao dịch thanh toán cọc VietQR, tiền mặt, VNPay, MoMo |
| 18 | `work_shifts` | `PhanCa` | Lịch phân ca trực Sáng / Chiều / Tối theo tuần của nhân viên |
| 19 | `stock_receipts` | `PhieuNhapKho` | Phiếu nhập kho từ NCC Yonex VN, NPP Đại Phát |
| 20 | `stock_equipment_details` | `ChiTietNhapDungCu` | Chi tiết số lượng và giá nhập dụng cụ vợt, ống cầu |
| 21 | `stock_product_details` | `ChiTietNhapSanPham` | Chi tiết số lượng và giá nhập đồ uống, nước giải khát |
| 22 | `promotions` | `KhuyenMai` | Chương trình ưu đãi giờ vàng thứ 4, giảm 15% VIP, khai trương |
| 23 | `audit_logs` | `NhatKyHoatDong` | Nhật ký truy vết bảo mật (Đổi giá, check-in, phân quyền, login) |
| 24 | `tournament_matches` | `TranDau` | Lịch thi đấu và kết quả các trận đấu trong giải |
| 25 | `system_settings` | `CauHinhHeThong` | Cấu hình quy định: % cọc (30%), giữ chỗ (10p), hủy hoàn cọc |

---

## 4. Danh sách tài khoản thử nghiệm đăng nhập (Mật khẩu: `123456`)

| Tài khoản (Username) | Mật khẩu | Họ và tên | Vai trò (Role) | Chi nhánh |
| :--- | :--- | :--- | :--- | :--- |
| `director` | `123456` | Nguyễn Phước Thọ | Giám đốc chuỗi | Toàn chuỗi (`ALL`) |
| `manager` | `123456` | Trần Phúc Bảo | Quản lý chi nhánh | Thủ Đức (`CN01`) |
| `pos` | `123456` | Nguyễn Thanh Tâm | Thu ngân quầy POS | Thủ Đức (`CN01`) |
| `admin` | `123456` | Trần Biểu Hương | Quản trị hệ thống | Toàn chuỗi (`ALL`) |
| `customer` | `123456` | Lê Bá Đạt | Khách hàng thành viên | Thủ Đức (`CN01`) |
| `nam.vh` | `123456` | Võ Hoàng Nam | Giám đốc điều hành | Toàn chuỗi (`ALL`) |
| `quan.nv` | `123456` | Nguyễn Văn Quản | Quản lý chi nhánh | Bình Thạnh (`CN02`) |
| `nam.th` | `123456` | Trần Hữu Nam | Quản lý chi nhánh | Quận 9 (`CN03`) |
| `khang.lh` | `123456` | Lê Hoàng Khang | Quản lý chi nhánh | Gò Vấp (`CN04`) |
| `mai.lt` | `123456` | Lê Thị Mai | Thu ngân quầy POS | Thủ Đức (`CN01`) |
