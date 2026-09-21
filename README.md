# UTE SPORT - Hệ thống Quản lý Chuỗi Sân Cầu Lông

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=Thymeleaf&logoColor=white)
![SQL Server](https://img.shields.io/badge/SQL_Server-CC2927?style=for-the-badge&logo=microsoft-sql-server&logoColor=white)

**BÁO CÁO ĐỒ ÁN CÔNG NGHỆ PHẦN MỀM - NHÓM 11** 
---

| Họ và tên | MSSV     |
|-----------|----------|
| Nguyễn Phước Thọ | 24110343 | 
| Trần Phúc Bảo | 24133005 |
| Nguyễn Thanh Tâm | 24133052 |
| Trần Biểu Hương | 24110234 |

---

## Mục lục
- [Chức năng chính](#-chức-năng-chính)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Cấu trúc phân hệ](#-cấu-trúc-phân-hệ-role-based)
- [Hướng dẫn cài đặt](#-hướng-dẫn-cài-đặt-chạy-dự-án)
- [Thành viên phát triển](#-thành-viên-phát-triển)

---

## Chức năng chính
- **Quản lý đặt sân (Booking & POS)**: Giao diện dạng lưới trực quan giúp nhân viên dễ dàng sắp xếp ca, check-in, check-out và thanh toán.
- **Quản lý Chi nhánh (Store Manager)**: Theo dõi tình trạng sân, quản lý kho vật tư (đồ uống, vợt, cầu), lập phiếu nhập kho và xếp ca làm việc cho nhân viên.
- **Quản lý Chuỗi (Director)**: Bảng điều khiển (Dashboard) tổng hợp doanh thu, so sánh tỷ lệ lấp đầy giữa các chi nhánh, phê duyệt giải đấu và ban hành bảng giá.
- **Quản trị Hệ thống (Admin)**: Quản lý tài khoản, phân quyền truy cập, theo dõi nhật ký hoạt động (Logs).

---

## Công nghệ sử dụng
- **Backend**: Java, Spring Boot, Spring MVC, Spring Data JPA.
- **Frontend**: HTML5, CSS3 (Custom UI Design System), Thymeleaf (Template Engine), Chart.js (Biểu đồ thống kê).
- **Cơ sở dữ liệu**: H2 Database (Môi trường Dev/Test UI), Microsoft SQL Server (Môi trường Production).
- **Công cụ quản lý thư viện**: Maven.

---

## Cấu trúc phân hệ

Hệ thống được thiết kế theo mô hình phân quyền chặt chẽ, mỗi vai trò có một không gian làm việc riêng biệt:

1. **`/admin/*`**: Dành cho Quản trị viên hệ thống.
2. **`/director/*`**: Dành cho Giám đốc điều hành toàn chuỗi.
3. **`/manager/*`**: Dành cho Quản lý chi nhánh. (Bao gồm các màn hình: Dashboard, Danh mục sân, Cấu hình giá, Kho hàng, Nhập kho, Nhân sự, Báo cáo).
4. **`/pos/*`**: Dành cho Nhân viên quầy thu ngân.
5. **`/customer/*`**: Dành cho Khách hàng vãng lai / Thành viên đặt sân trực tuyến.

---

## Hướng dẫn cài đặt & Chạy dự án

### 1. Yêu cầu hệ thống
- Java Development Kit (JDK) 17 trở lên.
- Maven 3.6+ (Dự án đã tích hợp sẵn `mvnw`).
- Trình duyệt web hiện đại (Chrome, Edge, Safari...).

### 2. Khởi chạy môi trường phát triển (Mockup UI)
Hiện tại, dự án đang được cấu hình sử dụng **H2 Database** trên RAM để phục vụ việc thiết kế và kiểm thử giao diện Frontend.

1. Clone kho lưu trữ về máy:
   ```bash
   git clone https://github.com/shinn1603/Badminton_Management_Project.git
   ```
2. Mở terminal tại thư mục gốc của project (nơi chứa file `pom.xml`).
3. Chạy lệnh khởi động Spring Boot:
   - Trên Windows: `mvnw.cmd spring-boot:run`
   - Trên Mac/Linux: `./mvnw spring-boot:run`
4. Mở trình duyệt và truy cập các đường dẫn sau để xem kết quả:
   - Giám đốc: `http://localhost:8080/director/dashboard`
   - Quản lý chi nhánh: `http://localhost:8080/manager/dashboard`
   - Quản trị viên: `http://localhost:8080/admin/users`

### 3. Kết nối Database thật (SQL Server) - *Sắp tới*

---

## Thành viên phát triển

