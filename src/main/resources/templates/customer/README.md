# Phân Hệ Cổng Khách Hàng (Customer Portal - Trần Biểu Hương - MSSV: 24110234)

Trọn bộ 7 màn hình giao diện theo đặc tả nghiệp vụ Chương 5:
- **MH-KH01: `customer/home.html`** - Trang chủ giới thiệu chuỗi sân, tra cứu bảng giá & tìm sân trống theo thời gian thực.
- **MH-KH02: `customer/auth.html`** - Đăng ký / Đăng nhập khách hàng (Quy định SĐT duy nhất `QĐ-KH01`, Biểu mẫu `BM-KH01`).
- **MH-KH03: `customer/booking.html`** - Đặt sân & khóa giữ chỗ 10 phút đếm ngược (`QĐ-KH02`), chọn slot sân tiêu chuẩn / VIP, chọn tiện ích thuê vợt, cầu lông, nước uống.
- **MH-KH04: `customer/payment.html`** - Thanh toán tiền cọc 30% (`QĐ-KH02`), phiếu xác nhận đặt sân (`BM-KH02`), quét mã VietQR tự động / VNPay / MoMo.
- **MH-KH05: `customer/history.html`** - Tra cứu lịch sử đơn đặt chỗ, vé điện tử QR check-in tại quầy, xử lý hủy hoàn tiền cọc 100% khi hủy trước $\ge 4$ giờ (`QĐ-KH03`), đánh giá chất lượng sân 5 sao.
- **MH-KH06: `customer/profile.html`** - Thẻ hội viên điện tử VIP (Đồng/Bạc/Vàng/Kim Cương), tiến trình thăng hạng, theo dõi tích lũy điểm thưởng (1.000đ = 1 điểm), cập nhật hồ sơ cá nhân.
- **MH-KH07: `customer/tournaments.html`** - Danh sách sự kiện giải đấu thể thao phong trào, điều lệ quy định (`QĐ-KH04`), phiếu đăng ký tham gia thi đấu (`BM-KH03`).

### Định tuyến Controller (`DashboardController.java`):
- `/customer`, `/customer/home` $\rightarrow$ `customer/home`
- `/customer/auth`, `/customer/login` $\rightarrow$ `customer/auth`
- `/customer/booking` $\rightarrow$ `customer/booking`
- `/customer/payment` $\rightarrow$ `customer/payment`
- `/customer/history` $\rightarrow$ `customer/history`
- `/customer/profile` $\rightarrow$ `customer/profile`
- `/customer/tournaments` $\rightarrow$ `customer/tournaments`

### Thiết kế & Thẩm mỹ:
- Sử dụng chuẩn thiết kế Warm Zinc & Teal (`/css/main.css`).
- Tích hợp đầy đủ tương tác JavaScript (Bộ đếm thời gian 10:00 giữ slot, tính toán tiền cọc 30%, tự động sinh mã VietQR, kiểm tra điều kiện hủy sân $\ge 4$ giờ, chuyển đổi trạng thái lọc).
