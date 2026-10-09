# HƯỚNG DẪN TRIỂN KHAI HỆ THỐNG LÊN MÔI TRƯỜNG INTERNET & DOCKER

Tài liệu phục vụ nghiệm thu đề tài: **Website Quản lý chuỗi sân cầu lông UTE Sport** (Tuần 3: 06/10 - 10/10/2026).

---

## 1. Triển khai bằng Docker Compose (Khuyến nghị cục bộ & VPS)

Hệ thống đã được đóng gói sẵn với [Dockerfile](file:///c:/Users/ASUS/Documents/web/Badminton_Management_Project/Dockerfile) và [docker-compose.yml](file:///c:/Users/ASUS/Documents/web/Badminton_Management_Project/docker-compose.yml).

### Các bước thực hiện:

1. **Đóng gói tệp JAR ứng dụng:**
   ```bash
   mvn clean package -DskipTests
   ```
2. **Khởi chạy toàn bộ hệ thống (Web App + SQL Server 2022):**
   ```bash
   docker-compose up -d --build
   ```
3. **Kiểm tra trạng thái container:**
   ```bash
   docker-compose ps
   ```
4. **Khởi tạo dữ liệu mẫu lần đầu (nếu cơ sở dữ liệu trống):**
   ```bash
   docker exec -i badminton_sqlserver /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P 'YourStrong@Passw0rd' -i database/init_badminton.sql
   ```
5. **Truy cập ứng dụng:**
   - Cổng giao diện chính: `http://localhost:8080`
   - Đăng nhập POS thu ngân: `http://localhost:8080/pos/grid`
   - Quản lý cơ sở: `http://localhost:8080/manager/dashboard`

---

## 2. Triển khai lên Cloud (Render / Railway / VPS)

### Cấu hình biến môi trường (Environment Variables):
- `SPRING_PROFILES_ACTIVE`: `prod`
- `SPRING_DATASOURCE_URL`: Chuỗi kết nối JDBC SQL Server trên Cloud (vd: Azure SQL, Render PostgreSQL, hoặc Railway SQL Server).
- `SPRING_DATASOURCE_USERNAME`: Tên tài khoản cơ sở dữ liệu.
- `SPRING_DATASOURCE_PASSWORD`: Mật khẩu cơ sở dữ liệu.
- `PORT`: `8080` (hoặc cổng do nhà cung cấp Cloud cấp phát tự động).
- `CLOUDINARY_CLOUD_NAME`: Tên Cloudinary cloud của nhóm.
- `CLOUDINARY_API_KEY`: API Key Cloudinary.
- `CLOUDINARY_API_SECRET`: API Secret Cloudinary.

---

## 3. Danh sách Endpoint API chính thức phục vụ kiểm thử và chấm điểm

- **Đăng nhập & Xác thực JWT**: `POST /api/auth/login`
- **Khóa slot thời gian thực**: `POST /api/bookings/slot-lock`
- **Chuyển sân (XL-06)**: `POST /api/bookings/{bookingCode}/transfer`
- **Gọi dịch vụ F&B & Trừ kho (XL-05)**: `POST /api/bookings/{bookingCode}/order-service`
- **Bàn giao ca trực & chốt quỹ (XL-08)**: `GET /api/shifts/summary`, `POST /api/shifts/close`
- **Lập phiếu nhập kho (XL-10)**: `POST /api/inventory/intake`, `GET /api/inventory/receipts`
- **Upload ảnh Cloudinary**: `POST /api/upload/image`
