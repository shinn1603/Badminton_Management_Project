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

---

## 5. Danh mục Hàm (Functions), Thủ tục (Stored Procedures) & Trigger

Dự án cung cấp tập lệnh chuyên sâu trong tệp [database/procedures_triggers_functions.sql](file:///c:/Users/ASUS/Documents/web/Badminton_Management_Project/database/procedures_triggers_functions.sql) và tích hợp sẵn trong [database/init_badminton.sql](file:///c:/Users/ASUS/Documents/web/Badminton_Management_Project/database/init_badminton.sql):

### 5.1 Hàm người dùng định nghĩa (User-Defined Functions)
1. **`dbo.fn_CheckSlotAvailability(@court_code, @booking_date, @time_slot)`**: Trả về `1` nếu sân và khung giờ khả dụng (không trùng lịch, không bảo trì), `0` nếu không khả dụng.
   ```sql
   SELECT dbo.fn_CheckSlotAvailability('CL01', '2026-10-15', '18:00 - 19:00') AS IsAvailable;
   ```
2. **`dbo.fn_CalculateBookingPrice(@court_code, @time_slot, @promo_code)`**: Tính tổng tiền thuê sân dựa trên thời lượng số giờ và tự động trừ chiết khấu nếu mã khuyến mãi hợp lệ.
   ```sql
   SELECT dbo.fn_CalculateBookingPrice('CL01', '18:00 - 20:00', 'KM01') AS TotalPrice;
   ```
3. **`dbo.fn_GetCustomerTierDiscount(@customer_code)`**: Trả về phần trăm giảm giá theo hạng thành viên (`Đồng` = 0%, `Bạc` = 5%, `Vàng` = 10%, `Kim Cương` = 15%).
   ```sql
   SELECT dbo.fn_GetCustomerTierDiscount('KH001') AS DiscountPercent;
   ```
4. **`dbo.fn_GetBranchRevenue(@branch_code, @from_date, @to_date)`**: Tổng hợp doanh thu thực tế từ các hóa đơn đã thanh toán của chi nhánh trong một khoảng thời gian.
   ```sql
   SELECT dbo.fn_GetBranchRevenue('CN01', '2026-10-01', '2026-10-31') AS OctRevenue;
   ```
5. **`dbo.fn_GetAvailableCourtsByBranch(@branch_code, @booking_date, @time_slot)`** *(Table-Valued Function)*: Trả về danh sách bảng các sân còn trống cho chi nhánh và khung giờ.
   ```sql
   SELECT * FROM dbo.fn_GetAvailableCourtsByBranch('CN01', '2026-10-15', '17:00 - 18:00');
   ```

### 5.2 Thủ tục lưu trữ nghiệp vụ (Stored Procedures)
1. **`dbo.sp_LockSlotAndCreateBooking`**: Đặt và khóa slot trực tuyến có bảo vệ giao dịch (Concurrency Transaction), tự sinh mã `DSxxxxxx`, tính tiền cọc 30% và ghi `audit_logs`.
   ```sql
   DECLARE @bCode VARCHAR(30), @dep DECIMAL(18,2), @total DECIMAL(18,2);
   EXEC dbo.sp_LockSlotAndCreateBooking 
       @court_code = 'CL01', @branch_code = 'CN01', 
       @booking_date = '2026-10-15', @time_slot = '18:00 - 19:00',
       @customer_name = N'Nguyễn Văn A', @customer_phone = '0903112233',
       @promo_code = 'KM01', @addon_amount = 30000,
       @booking_code = @bCode OUTPUT, @deposit_amount = @dep OUTPUT, @total_price = @total OUTPUT;
   SELECT @bCode AS BookingCode, @dep AS Deposit30, @total AS TotalPrice;
   ```
2. **`dbo.sp_ConfirmBookingDeposit`**: Xác nhận giao dịch cọc 30% VietQR, cập nhật đơn sang `Đã cọc 30%`, tạo bản ghi bảng `payments` và chuyển trạng thái sân sang `Đặt trước`.
   ```sql
   EXEC dbo.sp_ConfirmBookingDeposit @booking_code = 'DS0104', @amount = 27000, @method = N'VietQR Pro', @transaction_ref = 'MB-123456';
   ```
3. **`dbo.sp_CheckInBooking`**: Khách nhận sân tại quầy POS, chuyển đơn sang `Đang sử dụng`, chuyển sân sang `Đang sử dụng`, lưu thời gian check-in.
   ```sql
   EXEC dbo.sp_CheckInBooking @booking_code = 'DS0103', @staff_code = 'NS03';
   ```
4. **`dbo.sp_CheckOutAndCreateInvoice`**: Khách trả sân, giải phóng sân về `Trống`, lập hóa đơn thanh toán `invoices`, trừ tiền cọc đã trả, tích giờ chơi và điểm thưởng cho hội viên.
   ```sql
   DECLARE @invCode VARCHAR(30), @finalPay DECIMAL(18,2);
   EXEC dbo.sp_CheckOutAndCreateInvoice 
       @booking_code = 'DS0103', @staff_code = 'NS03', @payment_method = N'Tiền mặt',
       @invoice_code = @invCode OUTPUT, @final_payment = @finalPay OUTPUT;
   SELECT @invCode AS InvoiceCode, @finalPay AS FinalPayment;
   ```
5. **`dbo.sp_CancelBookingWithRefundPolicy`**: Hủy đơn đặt sân và áp dụng chính sách hoàn cọc (hủy trước 4 tiếng theo cấu hình `CFG_CANCEL_HOURS` thì hoàn cọc 100%, hủy trễ không hoàn cọc).
   ```sql
   DECLARE @refAmount DECIMAL(18,2), @isRef BIT;
   EXEC dbo.sp_CancelBookingWithRefundPolicy 
       @booking_code = 'DS0105', @cancel_reason = N'Khách bận đột xuất',
       @refund_amount = @refAmount OUTPUT, @is_refunded = @isRef OUTPUT;
   SELECT @refAmount AS RefundAmount, @isRef AS IsRefunded;
   ```
6. **`dbo.sp_ImportStockReceipt`**: Nhập kho dụng cụ/sản phẩm từ nhà cung cấp, lưu phiếu `stock_receipts` và ghi vết `audit_logs`.
   ```sql
   EXEC dbo.sp_ImportStockReceipt 
       @receipt_code = 'PN0103', @branch_code = 'CN01', @staff_code = 'NS02',
       @supplier_name = N'Công ty Yonex VN', @total_amount = 5000000, @notes = N'Nhập bổ sung vợt mới';
   ```
7. **`dbo.sp_GetBranchDailyRevenueReport`**: Báo cáo tổng kết doanh thu theo ngày theo từng chi nhánh (tiền sân, dụng cụ, đồ uống, chiết khấu, thực thu).
   ```sql
   EXEC dbo.sp_GetBranchDailyRevenueReport @branch_code = 'CN01', @report_date = '2026-10-13';
   ```

### 5.3 Triggers tự động hóa ràng buộc và sự kiện (Triggers)
1. **`trg_CheckBookingOverlap`** *(bảng `bookings`)*: Ngăn chặn 100% đặt trùng giờ trên cùng một sân thi đấu (loại trừ các đơn `Đã hủy`).
2. **`trg_UpdateStockOnInvoiceProduct`** *(bảng `invoice_product_details`)*: Tự động trừ số lượng tồn kho sản phẩm/nước uống khi xuất hóa đơn POS và chặn nếu tồn kho bị âm.
3. **`trg_UpdateStockOnReceiptDetails`** *(bảng `stock_product_details`)*: Tự động cộng số lượng tồn kho sản phẩm khi nhập kho.
4. **`trg_UpdateCustomerMembershipLevel`** *(bảng `customers`)*: Tự động cập nhật hạng thành viên (`Đồng`, `Bạc`, `Vàng`, `Kim Cương`) khi số giờ chơi lũy kế tăng.
5. **`trg_AuditCourtStatusChange`** *(bảng `courts`)*: Tự động ghi nhật ký bảo mật vào bảng `audit_logs` mỗi khi trạng thái sân bãi thay đổi.
6. **`trg_ValidatePromotionDates`** *(bảng `promotions`)*: Đảm bảo ngày kết thúc khuyến mãi `>=` ngày bắt đầu và mức giảm giá `> 0`.

