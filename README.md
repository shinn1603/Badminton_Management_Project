# UTE SPORT - Hệ thống Quản lý Chuỗi Sân Cầu Lông

![Java](https://img.shields.io/badge/Java-26-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=Thymeleaf&logoColor=white)
![SQL Server](https://img.shields.io/badge/SQL_Server-2022-CC2927?style=for-the-badge&logo=microsoft-sql-server&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)
![Cloudinary](https://img.shields.io/badge/Cloudinary-3448C5?style=for-the-badge&logo=Cloudinary&logoColor=white)
![Gemini AI](https://img.shields.io/badge/Gemini_AI-8E75B2?style=for-the-badge&logo=googlebard&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socketdotio&logoColor=white)

**BÁO CÁO ĐỒ ÁN CÔNG NGHỆ PHẦN MỀM - NHÓM 11** 
---

## Mục lục
- [Giới thiệu chung](#giới-thiệu-chung)
- [Chức năng chính](#chức-năng-chính)
- [Công nghệ & Kiến trúc](#công-nghệ--kiến-trúc)
- [Cấu trúc thư mục dự án](#cấu-trúc-thư-mục-dự-án)
- [Cấu trúc phân hệ (Role-Based)](#cấu-trúc-phân-hệ-role-based)
- [Hướng dẫn cài đặt & Chạy dự án](#hướng-dẫn-cài-đặt--chạy-dự-án)
- [Thành viên phát triển](#thành-viên-phát-triển)

---

## Giới thiệu chung
**UTE SPORT** là một hệ thống phần mềm toàn diện nhằm quản lý vận hành, đặt lịch và thanh toán cho chuỗi sân cầu lông. Dự án được thiết kế với kiến trúc phân quyền chặt chẽ, tối ưu hóa trải nghiệm khách hàng với tính năng đặt sân thời gian thực, tích hợp AI thông minh và tự động hóa quy trình quản lý sân, kho bãi và nhân sự tại nhiều chi nhánh.

---

## Chức năng chính
- **Cổng Khách Hàng Trực Tuyến (Customer Portal)**:
  - Xem danh sách sân của từng chi nhánh với bộ lọc linh hoạt.
  - Lưới chọn giờ đặt sân trực quan theo thời gian thực (06:00 - 22:00).
  - **Slot-Level Locking (Real-time WebSocket)**: Khóa giữ chỗ cấp khung giờ thời gian thực. Chỉ khóa duy nhất sân và khung giờ đang được chọn, đảm bảo không trùng lặp (Overbooking).
  - Giao diện thiết kế theo ngôn ngữ Luxury Emerald Dashboard.
- **Thanh toán VietQR & Xác nhận Tự Động**:
  - Sinh mã VietQR động để thanh toán tiền cọc.
  - Bộ đếm thời gian giữ chỗ (Countdown timer).
  - Tích hợp đối soát giao dịch và tự động xác nhận đặt sân không cần duyệt tay. Cung cấp mã QR Check-in tức thì tại quầy.
- **Trợ Lý Thông Minh AI (Gemini AI Consultant)**:
  - Tích hợp Chatbot AI để tư vấn chi nhánh gần nhất, tra cứu khung giờ trống trực tiếp qua hội thoại và hỗ trợ quy trình đặt lịch tự động.
- **Quản lý đặt sân & Thu ngân (Booking & POS)**: 
  - Lưới hiển thị trạng thái sân trực quan cho nhân viên (Check-in mã QR, Check-out, Thanh toán hóa đơn, bán lẻ dịch vụ F&B, cho thuê dụng cụ).
- **Quản lý Chi nhánh (Store Manager)**: 
  - Theo dõi tình trạng các sân.
  - Quản lý kho vật tư (đồ uống, dụng cụ, vợt, cầu) với hệ thống lập phiếu nhập kho.
  - Quản lý lịch làm việc và xếp ca (Work shifts) cho nhân viên.
- **Quản lý Chuỗi (Director)**: 
  - Dashboard tổng hợp doanh thu, so sánh tỷ lệ lấp đầy giữa các chi nhánh.
  - Phê duyệt và quản lý các giải đấu (Tournaments).
- **Quản trị Hệ thống (Admin)**: 
  - Quản lý toàn bộ tài khoản người dùng, phân quyền truy cập, và theo dõi nhật ký hoạt động (Audit Logs).

---

## Công nghệ & Kiến trúc
- **Backend**:
  - **Java 26 & Spring Boot 4.1.1**: Core Framework.
  - **Spring Data JPA / Hibernate**: ORM tương tác với Database.
  - **Spring Security & JWT**: Xác thực và phân quyền (Authentication & Authorization).
  - **Spring WebSockets**: Xử lý thời gian thực cho tính năng khóa khung giờ (Slot Locking).
  - **Google Gemini AI SDK**: Tích hợp trợ lý ảo thông minh.
- **Frontend**:
  - **Thymeleaf, HTML5, CSS3, JavaScript**: Template Engine và UI/UX (Custom Design System).
  - **Chart.js**: Vẽ biểu đồ thống kê Dashboard.
- **Cơ sở dữ liệu & Lưu trữ**:
  - **H2 Database**: Dùng cho môi trường Dev/Test.
  - **Microsoft SQL Server 2022**: Dùng cho môi trường Production (Chạy qua Docker).
  - **Cloudinary**: Lưu trữ và quản lý hình ảnh (Avatar, hình ảnh sân, sản phẩm).
- **Công cụ DevOps & Quản lý**:
  - **Maven**: Quản lý Dependency và Build.
  - **Docker & Docker Compose**: Đóng gói và triển khai (Containerization).

---

## Cấu trúc thư mục dự án

```text
Badminton_Management_Project/
├── database/                    # Chứa script khởi tạo CSDL (init_badminton.sql)
├── scripts/                     # Chứa các script hỗ trợ hệ thống (reseed_db.ps1)
├── src/
│   ├── main/java/vn/yain/       # Mã nguồn Java (Spring Boot)
│   │   ├── config/              # Cấu hình hệ thống (Security, WebSocket,...)
│   │   ├── controller/          # Các API và Controller xử lý HTTP requests
│   │   ├── dto/                 # Các Data Transfer Object (Truyền tải dữ liệu)
│   │   ├── entity/              # Các Entity ánh xạ CSDL (JPA/Hibernate)
│   │   ├── repository/          # Lớp tương tác CSDL (Spring Data JPA)
│   │   ├── security/            # Xử lý xác thực/phân quyền (JWT)
│   │   ├── service/             # Xử lý logic nghiệp vụ, AI, Cloudinary,...
│   │   └── BadmintonManagementProjectApplication.java
│   └── main/resources/
│       ├── static/              # Các file tĩnh (CSS, JS, Images)
│       ├── templates/           # Giao diện hiển thị (Thymeleaf HTML)
│       └── application.properties # Cấu hình môi trường Spring Boot
├── docker-compose.yml           # Cấu hình triển khai với Docker (Web & SQL Server)
├── Dockerfile                   # Build image cho Spring Boot app
├── pom.xml                      # Cấu hình dependency của Maven
└── README.md                    # Tài liệu dự án
```

---

## Cấu trúc phân hệ (Role-Based)
Hệ thống áp dụng mô hình phân quyền Role-Based Access Control (RBAC), chia làm 5 phân hệ không gian làm việc:

1. **`/admin/*`**: Quản trị viên hệ thống (Admin).
2. **`/director/*`**: Giám đốc điều hành toàn chuỗi (Director).
3. **`/manager/*`**: Quản lý chi nhánh (Store Manager - Dashboard, Danh mục sân, Giá, Kho, Nhân sự...).
4. **`/pos/*`**: Nhân viên quầy thu ngân (Staff).
5. **`/customer/*`**: Khách hàng vãng lai / Thành viên trực tuyến.

---

## Hướng dẫn cài đặt & Chạy dự án

### 1. Yêu cầu hệ thống
- **Java Development Kit (JDK) 24/26** trở lên.
- **Maven 3.6+** (Dự án đã có sẵn `mvnw`).
- **Docker Desktop** (nếu muốn chạy môi trường Production với SQL Server).

### 2. Khởi chạy môi trường phát triển (Sử dụng H2 Database)
Trong môi trường phát triển, dự án cấu hình sử dụng **H2 Database** (in-memory) phục vụ việc chạy nhanh.

1. Clone dự án về máy:
   ```bash
   git clone https://github.com/shinn1603/Badminton_Management_Project.git
   ```
2. Mở terminal tại thư mục gốc của dự án.
3. Chạy lệnh khởi động Spring Boot:
   - Trên Windows: `mvnw.cmd spring-boot:run`
   - Trên Mac/Linux: `./mvnw spring-boot:run`
4. Mở trình duyệt và truy cập các cổng chức năng:
   - Trang chủ khách hàng: `http://localhost:8080/`
   - Giám đốc: `http://localhost:8080/director/dashboard`
   - Quản lý chi nhánh: `http://localhost:8080/manager/dashboard`
   - Quản trị viên: `http://localhost:8080/admin/users`

### 3. Triển khai bằng Docker (Môi trường Production)
Để chạy dự án đầy đủ cùng Microsoft SQL Server 2022 thông qua Docker Compose:

1. Đảm bảo Docker đang chạy.
2. Tại thư mục gốc của dự án, chạy lệnh:
   ```bash
   docker-compose up -d --build
   ```
3. Docker sẽ tự động:
   - Tải và chạy MS SQL Server (`1433`).
   - Build Image và chạy ứng dụng Spring Boot (`8080`) liên kết với SQL Server.
4. Cơ sở dữ liệu sẽ tự động được khởi tạo theo file script nằm tại `database/init_badminton.sql`.

---

## Thành viên phát triển

| Họ và tên         | MSSV     | 
|-------------------|----------|
| Nguyễn Phước Thọ  | 24110343 | 
| Trần Phúc Bảo     | 24133005 | 
| Nguyễn Thanh Tâm  | 24133052 | 
| Trần Biểu Hương   | 24110234 | 

---
*(Báo cáo đồ án môn Công Nghệ Phần Mềm)*
