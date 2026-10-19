# HƯỚNG DẪN TRIỂN KHAI HỆ THỐNG LÊN MÔI TRƯỜNG CLOUD (RAILWAY.APP / RENDER.COM)

Tài liệu hướng dẫn đóng gói **Docker** và triển khai ứng dụng **Spring Boot + Microsoft SQL Server 2022** lên môi trường Internet thực tế theo kế hoạch: **GitHub -> Docker -> Railway.app / Render.com**.

---

## 1. Kiến trúc Đóng gói & Triển khai (Overview)

```mermaid
flowchart TD
    Dev["Developer (Local Machine)"] -->|git push| GitHub["GitHub Repository"]
    
    subgraph CloudHosting ["Môi trường Cloud (Railway.app / Render.com)"]
        GitHub -->|Webhook Trigger Build| DockerBuild["Docker Multi-Stage Build"]
        DockerBuild -->|Stage 1: Maven 3.9 + JDK 21| AppJar["Build target/*.jar (Skip Tests)"]
        AppJar -->|Stage 2: Temurin JRE 21| Container["Production Container (MaxRAM 75%)"]
        
        Container <-->|Port ${PORT:-8080}| PublicWeb["Public Domain (HTTPS)"]
        Container <-->|JDBC Port 1433| Database[("MS SQL Server 2022 (Cloud/Private Net)")]
    end
    
    Client["Người dùng / Trình duyệt"] -->|HTTPS / WSS| PublicWeb
```

### Các tối ưu hóa đã được cấu hình sẵn trong mã nguồn:
1. **Multi-Stage Build Dockerfile**: Không phụ thuộc vào file JAR local, máy chủ Cloud sẽ tự kéo mã nguồn và build đóng gói sạch sẽ từ đầu.
2. **Quản lý RAM Container**: Tích hợp cờ `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` nhằm ngăn chặn lỗi Out-Of-Memory (OOM Killed) khi chạy trên gói Cloud miễn phí (512MB RAM).
3. **Cổng động (`PORT`)**: Tự động nhận biến môi trường `$PORT` do Render / Railway cấp phát thông qua `${PORT:8080}`.
4. **CORS & WebSocket**: Đã cấu hình cho phép các domain `*.onrender.com`, `*.railway.app`, `*.up.railway.app` và giao thức WebSocket STOMP (`/ws-court`).
5. **Health Check Endpoint**: Cung cấp sẵn endpoint `GET /api/health` phục vụ giám sát tự động của Cloud Platform.

---

## 2. Chuẩn bị & Đẩy mã nguồn lên GitHub

Mở PowerShell tại thư mục gốc dự án và chạy các lệnh sau:

```bash
# 1. Kiểm tra trạng thái các file đã thay đổi
git status

# 2. Thêm toàn bộ các file đã tối ưu (Dockerfile, .dockerignore, config, guides)
git add Dockerfile .dockerignore pom.xml DEPLOYMENT_GUIDE.md src/

# 3. Commit thay đổi
git commit -m "chore(deploy): prepare multi-stage dockerfile and cloud deployment configs for Render and Railway"

# 4. Đẩy lên nhánh main trên GitHub
git push origin main
```

---

## 3. PHƯƠNG ÁN A: Triển khai trên Railway.app (KHUYẾN NGHỊ CAO NHẤT)

> **Tại sao chọn Railway?** 
> Dự án sử dụng **Microsoft SQL Server 2022**. Railway cho phép chạy trực tiếp cả **Container Spring Boot** và **Container SQL Server** trong cùng một Project thông qua **Private Network** nội bộ (tốc độ cao, bảo mật, không tốn băng thông ngoài).

### Bước 3.1: Đăng ký & Tạo Project
1. Truy cập [railway.app](https://railway.app/) và đăng nhập bằng tài khoản **GitHub**.
2. Chọn **"New Project"** -> Chọn **"Empty Project"**.

### Bước 3.2: Triển khai SQL Server 2022 Container
1. Trong màn hình Project, nhấn nút **"+ Create"** -> chọn **"Docker Image"**.
2. Nhập tên image:
   ```text
   mcr.microsoft.com/mssql/server:2022-latest
   ```
3. Sau khi service được tạo, bấm vào service đó -> chọn tab **"Variables"** -> thêm các biến:
   - `ACCEPT_EULA` = `Y`
   - `MSSQL_SA_PASSWORD` = `YourStrong@Passw0rd` *(hoặc mật khẩu mạnh tuỳ chọn)*
   - `MSSQL_PID` = `Developer`
4. Chọn tab **"Settings"**:
   - Đổi tên service thành: `sqlserver`
   - Mục **Networking**: Nhấn **"Add TCP Proxy"** nếu muốn kết nối từ máy local (SSMS/DBeaver) để nạp dữ liệu.

### Bước 3.3: Triển khai Ứng dụng Web Spring Boot
1. Cũng trong Project đó, nhấn **"+ Create"** -> chọn **"GitHub Repo"**.
2. Chọn repository `Badminton_Management_Project`.
3. Railway sẽ tự động phát hiện [Dockerfile](file:///c:/Users/ASUS/Documents/web/Badminton_Management_Project/Dockerfile) và bắt đầu quá trình Multi-Stage Build.
4. Bấm vào Web Service vừa tạo -> chọn tab **"Variables"** -> thêm các biến môi trường:
   ```env
   SPRING_PROFILES_ACTIVE=prod
   SPRING_DATASOURCE_URL=jdbc:sqlserver://sqlserver.railway.internal:1433;databaseName=badminton;encrypt=false;trustServerCertificate=true
   SPRING_DATASOURCE_USERNAME=sa
   SPRING_DATASOURCE_PASSWORD=YourStrong@Passw0rd
   JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
   CLOUDINARY_CLOUD_NAME=utesport-arena
   CLOUDINARY_API_KEY=348571948271842
   CLOUDINARY_API_SECRET=XyZ9876543210-abcdef
   ```
5. Chọn tab **"Settings"**:
   - Mục **Networking** -> Bấm **"Generate Domain"** để nhận domain công khai (ví dụ: `https://badminton-project-production.up.railway.app`).

### Bước 3.4: Nạp dữ liệu ban đầu vào SQL Server trên Railway
- Dùng **DBeaver**, **Azure Data Studio**, hoặc **SSMS**:
  - Host: Địa chỉ TCP Proxy do Railway cấp (ví dụ `monorail.proxy.rlwy.net`)
  - Port: Cổng proxy tương ứng (ví dụ `54321`)
  - User: `sa`
  - Password: `YourStrong@Passw0rd`
- Mở file [database/init_badminton.sql](file:///c:/Users/ASUS/Documents/web/Badminton_Management_Project/database/init_badminton.sql) và nhấn **Execute** để khởi tạo database `badminton` và toàn bộ 25 bảng dữ liệu mẫu.

---

## 4. PHƯƠNG ÁN B: Triển khai trên Render.com

> **Lưu ý:** Render cung cấp gói Web Service miễn phí rất tốt, nhưng không có dịch vụ quản lý Microsoft SQL Server miễn phí đi kèm. Do đó, bạn sẽ chạy Web App trên Render và kết nối tới một cơ sở dữ liệu SQL Server Cloud bên ngoài (ví dụ: Azure SQL Database Free Tier, Railway SQL Server, hoặc VPS).

### Bước 4.1: Tạo Web Service trên Render
1. Truy cập [render.com](https://render.com/) và đăng nhập bằng **GitHub**.
2. Nhấn nút **"New +"** -> Chọn **"Web Service"**.
3. Chọn GitHub repository: `Badminton_Management_Project`.
4. Điền các thông số cơ bản:
   - **Name**: `badminton-management-app`
   - **Region**: Singapore (hoặc Oregon/Frankfurt)
   - **Branch**: `main`
   - **Runtime**: Chọn **Docker** *(Render sẽ tự động dùng Dockerfile đa giai đoạn)*
   - **Instance Type**: Chọn **Free** (512MB RAM, 0.1 CPU)

### Bước 4.2: Cấu hình Environment Variables trên Render
Tại mục **Environment Variables**, bấm **"Add Environment Variable"** và cấu hình:

| Key | Value (Mẫu) | Ghi chú |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` | Kích hoạt profile production |
| `SPRING_DATASOURCE_URL` | `jdbc:sqlserver://<HOST_DB>:<PORT>;databaseName=badminton;encrypt=true;trustServerCertificate=true` | Chuỗi kết nối SQL Server ngoài |
| `SPRING_DATASOURCE_USERNAME` | `sa` *(hoặc user cloud)* | Tên đăng nhập database |
| `SPRING_DATASOURCE_PASSWORD` | `<mật_khẩu_db>` | Mật khẩu database |
| `JWT_SECRET` | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | Khóa bảo mật mã hóa JWT |
| `CLOUDINARY_CLOUD_NAME` | `utesport-arena` | Cloud name lưu trữ hình ảnh |
| `CLOUDINARY_API_KEY` | `348571948271842` | Cloudinary API Key |
| `CLOUDINARY_API_SECRET` | `XyZ9876543210-abcdef` | Cloudinary API Secret |
| `GEMINI_API_KEY` | `<your_gemini_api_key>` | API Key trợ lý ảo thông minh |

### Bước 4.3: Cấu hình Health Check Path
- Tại tab **Settings** -> mục **Health Check Path**, nhập: `/api/health`
- Nhấn **"Save Changes"** và Render sẽ tự động trigger quá trình build container và kích hoạt service.

---

## 5. Danh mục Tài khoản Kiểm thử trên Môi trường Internet

Sau khi hệ thống khởi động thành công, người dùng và hội đồng chấm thi có thể truy cập bằng các tài khoản phân quyền mặc định:

| Vai trò (Role) | Tên đăng nhập | Mật khẩu | Trang quản trị & Chức năng |
| :--- | :--- | :--- | :--- |
| **Giám đốc chuỗi (Director)** | `giamdoc` | `123456` | `/director/dashboard` (Xem doanh thu đa chi nhánh, duyệt đề xuất giá) |
| **Quản lý cơ sở (Manager)** | `quanly_cn1` | `123456` | `/manager/dashboard` (Phân ca, báo cáo ca, quản lý kho cơ sở) |
| **Thu ngân / Lễ tân (POS)** | `thungan_cn1` | `123456` | `/pos/grid` (Đặt sân, khóa slot thời gian thực, bán nước F&B, in bill) |
| **Khách hàng đặt sân** | `khachhang1` | `123456` | `/customer/booking` (Chọn ngày giờ, chọn sân, thanh toán VietQR) |

---

## 6. Các lệnh kiểm tra & Xử lý sự cố (Troubleshooting)

### 1. Kiểm tra trạng thái hoạt động:
Truy cập qua trình duyệt hoặc cURL:
```bash
curl https://<your-app-domain>/api/health
```
Kết quả trả về chuẩn:
```json
{
  "status": "UP",
  "service": "Badminton Management System",
  "profile": "prod",
  "timestamp": 1729322400000
}
```

### 2. Sự cố thường gặp & Giải pháp:
- **Lỗi `HikariPool - Connection refused`**: Kiểm tra lại chuỗi `SPRING_DATASOURCE_URL`, đảm bảo container cơ sở dữ liệu đã khởi động và cho phép kết nối qua cổng 1433.
- **Lỗi `OutOfMemoryError`**: Container đã được cấu hình cờ `MaxRAMPercentage=75.0`. Nếu dữ liệu báo cáo quá lớn, hãy hạn chế khoảng ngày truy vấn hoặc nâng gói instance.
- **Render ngủ đông (Spin-down sau 15 phút không có request)**: Bản miễn phí của Render sẽ tạm dừng sau 15 phút không có tương tác. Lần gọi đầu tiên sẽ mất 30-45 giây để khởi động lại (Cold Start). Bạn có thể dùng công cụ miễn phí như [cron-job.org](https://cron-job.org/) hoặc [UptimeRobot](https://uptimerobot.com/) ping vào `/api/health` 10 phút một lần để giữ ứng dụng luôn chạy.
