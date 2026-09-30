# Hướng Dẫn Cài Đặt Cơ Sở Dữ Liệu SQL Server - UTE Sport

Tài liệu này hướng dẫn các thành viên trong nhóm thiết lập và khởi tạo cơ sở dữ liệu `badminton` cho dự án.

---

## 1. Thông tin cấu hình kết nối (theo `application.properties`)
* **Host / Port:** `localhost:1433`
* **Tên Database:** `badminton`
* **Tài khoản:** `sa`
* **Mật khẩu:** `123456`

---

## 2. Cách chạy file khởi tạo `init_badminton.sql`

### Cách 1: Sử dụng SQL Server Management Studio (SSMS) (Khuyên dùng)
1. Mở **SQL Server Management Studio (SSMS)** và đăng nhập vào SQL Server (Authentication: SQL Server Authentication, User: `sa`, Pass: `123456`).
2. Mở file: `database/init_badminton.sql` (hoặc kéo thả file vào SSMS).
3. Nhấn **Execute** (hoặc phím tắt **F5**).
4. Script sẽ tự động kiểm tra, tạo database `badminton`, tạo 6 bảng dữ liệu và chèn sẵn dữ liệu mẫu.

### Cách 2: Sử dụng Azure Data Studio hoặc VS Code (Extension mssql)
1. Mở file `database/init_badminton.sql`.
2. Kết nối đến server SQL Server cục bộ.
3. Chọn **Run** (Ctrl + Shift + E).

### Cách 3: Chạy nhanh bằng lệnh PowerShell (Không cần mở SSMS)
Mở PowerShell tại thư mục gốc dự án và chạy:
```powershell
sqlcmd -S localhost,1433 -U sa -P 123456 -i database\init_badminton.sql
```

---

## 3. Danh sách tài khoản thử nghiệm đăng nhập (Mật khẩu: 123456)
| Tài khoản (Username) | Mật khẩu | Họ và tên | Vai trò (Role) | Chi nhánh |
| :--- | :--- | :--- | :--- | :--- |
| `director` | `123456` | Nguyễn Phước Thọ | Giám đốc chuỗi | Toàn chuỗi (`ALL`) |
| `manager` | `123456` | Trần Phúc Bảo | Quản lý chi nhánh | Thủ Đức (`CN01`) |
| `pos` | `123456` | Nguyễn Thanh Tâm | Thu ngân quầy POS | Thủ Đức (`CN01`) |
| `admin` | `123456` | Trần Biểu Hương | Quản trị hệ thống | Toàn chuỗi (`ALL`) |
| `customer` | `123456` | Lê Bá Đạt | Khách hàng thành viên | Thủ Đức (`CN01`) |
