-- =====================================================================
-- DỰ ÁN QUẢN LÝ CHUỖI SÂN CẦU LÔNG - UTE SPORT
-- HỆ QUẢN TRỊ CƠ SỞ DỮ LIỆU: MICROSOFT SQL SERVER
-- TÀI KHOẢN KẾT NỐI MẶC ĐỊNH: sa / 123456
-- =====================================================================

-- 1. TẠO CƠ SỞ DỮ LIỆU NẾU CHƯA CÓ
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'badminton')
BEGIN
    CREATE DATABASE badminton;
END
GO

USE badminton;
GO

-- 2. XÓA CÁC BẢNG CŨ NẾU ĐÃ TỒN TẠI (ĐỂ RE-INIT SẠCH SẼ)
IF OBJECT_ID('booking_services', 'U') IS NOT NULL DROP TABLE booking_services;
IF OBJECT_ID('bookings', 'U') IS NOT NULL DROP TABLE bookings;
IF OBJECT_ID('courts', 'U') IS NOT NULL DROP TABLE courts;
IF OBJECT_ID('products', 'U') IS NOT NULL DROP TABLE products;
IF OBJECT_ID('users', 'U') IS NOT NULL DROP TABLE users;
IF OBJECT_ID('branches', 'U') IS NOT NULL DROP TABLE branches;
GO

-- 3. BẢNG CHI NHÁNH TOÀN CHUỖI (branches)
CREATE TABLE branches (
    id INT IDENTITY(1,1) PRIMARY KEY,
    branch_code VARCHAR(20) NOT NULL UNIQUE,
    branch_name NVARCHAR(100) NOT NULL,
    address NVARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    manager_name NVARCHAR(100),
    total_courts INT DEFAULT 6,
    status NVARCHAR(30) DEFAULT N'Hoạt động'
);
GO

-- 4. BẢNG NGƯỜI DÙNG & PHÂN QUYỀN HỆ THỐNG (users)
CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(30) NOT NULL, -- DIRECTOR, MANAGER, POS, CUSTOMER, ADMIN
    branch_code VARCHAR(20),
    status NVARCHAR(30) DEFAULT N'Hoạt động',
    created_at DATETIME DEFAULT GETDATE()
);
GO

-- 5. BẢNG SÂN THI ĐẤU (courts)
CREATE TABLE courts (
    id INT IDENTITY(1,1) PRIMARY KEY,
    court_code VARCHAR(20) NOT NULL UNIQUE,
    court_name NVARCHAR(100) NOT NULL,
    court_type NVARCHAR(50) NOT NULL, -- Cầu lông VIP, Cầu lông tiêu chuẩn, Cầu lông thi đấu
    branch_code VARCHAR(20) NOT NULL,
    hourly_rate DECIMAL(18,2) NOT NULL,
    status NVARCHAR(30) DEFAULT N'Trống', -- Trống, Đang sử dụng, Bảo trì
    image_url VARCHAR(255)
);
GO

-- 6. BẢNG SẢN PHẨM & KHO HÀNG DỊCH VỤ (products)
CREATE TABLE products (
    id INT IDENTITY(1,1) PRIMARY KEY,
    product_code VARCHAR(20) NOT NULL UNIQUE,
    product_name NVARCHAR(100) NOT NULL,
    category NVARCHAR(50) NOT NULL, -- Nước giải khát, Dụng cụ, Cầu lông, Phụ kiện
    unit NVARCHAR(20) NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    cost_price DECIMAL(18,2) NOT NULL,
    stock_quantity INT DEFAULT 0,
    min_quantity INT DEFAULT 10,
    branch_code VARCHAR(20) NOT NULL
);
GO

-- 7. BẢNG ĐẶT SÂN & HÓA ĐƠN POS (bookings)
CREATE TABLE bookings (
    id INT IDENTITY(1,1) PRIMARY KEY,
    booking_code VARCHAR(30) NOT NULL UNIQUE,
    customer_name NVARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    court_code VARCHAR(20) NOT NULL,
    branch_code VARCHAR(20) NOT NULL,
    booking_date DATE NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    hourly_price DECIMAL(18,2) NOT NULL,
    total_price DECIMAL(18,2) NOT NULL,
    deposit_amount DECIMAL(18,2) DEFAULT 0,
    payment_method NVARCHAR(50) DEFAULT N'Tiền mặt',
    status NVARCHAR(50) NOT NULL, -- Chờ xác nhận, Đã xác nhận, Đang sử dụng, Hoàn thành, Đã hủy, Đã cọc 30%
    notes NVARCHAR(255),
    created_at DATETIME DEFAULT GETDATE()
);
GO

-- 8. BẢNG DỊCH VỤ ĐI KÈM ĐƠN ĐẶT SÂN (booking_services)
CREATE TABLE booking_services (
    id INT IDENTITY(1,1) PRIMARY KEY,
    booking_code VARCHAR(30) NOT NULL,
    product_name NVARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL
);
GO

-- =====================================================================
-- DỮ LIỆU MẪU ĐẦY ĐỦ (SEED DATA)
-- =====================================================================

-- 8.1 Danh sách chi nhánh
INSERT INTO branches (branch_code, branch_name, address, phone, manager_name, total_courts, status) VALUES
('CN01', N'Chi nhánh 1 - Sân Cầu Lông Thủ Đức', N'Số 01 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức', '0903123456', N'Trần Phúc Bảo', 8, N'Hoạt động'),
('CN02', N'Chi nhánh 2 - Sân Cầu Lông Quận 7', N'Số 15 Nguyễn Thị Thập, Tân Phú, Quận 7', '0903789012', N'Nguyễn Thanh Tâm', 6, N'Hoạt động'),
('CN03', N'Chi nhánh 3 - Sân Thể Thao Bình Thạnh', N'Số 234 Điện Biên Phủ, Phường 25, Bình Thạnh', '0903333444', N'Trần Biểu Hương', 6, N'Hoạt động');

-- 8.2 Danh sách tài khoản đăng nhập (Tất cả mật khẩu mặc định: 123456)
INSERT INTO users (username, password, full_name, email, phone, role, branch_code) VALUES
('director', '123456', N'Nguyễn Phước Thọ', 'director@utesport.vn', '0901111222', 'DIRECTOR', 'ALL'),
('manager', '123456', N'Trần Phúc Bảo', 'manager@utesport.vn', '0903123456', 'MANAGER', 'CN01'),
('pos', '123456', N'Nguyễn Thanh Tâm', 'pos@utesport.vn', '0904555666', 'POS', 'CN01'),
('admin', '123456', N'Trần Biểu Hương', 'admin@utesport.vn', '0908888999', 'ADMIN', 'ALL'),
('customer', '123456', N'Lê Bá Đạt', 'khachhang@gmail.com', '0912345678', 'CUSTOMER', 'CN01');

-- 8.3 Danh sách sân đấu
INSERT INTO courts (court_code, court_name, court_type, branch_code, hourly_rate, status, image_url) VALUES
('CL01', N'Sân cầu lông 1 (VIP)', N'Cầu lông VIP', 'CN01', 120000, N'Trống', '/images/badminton-court.jpg'),
('CL02', N'Sân cầu lông 2', N'Cầu lông tiêu chuẩn', 'CN01', 90000, N'Đang sử dụng', '/images/badminton-court.jpg'),
('CL03', N'Sân cầu lông 3', N'Cầu lông tiêu chuẩn', 'CN01', 90000, N'Trống', '/images/badminton-court.jpg'),
('CL04', N'Sân cầu lông 4', N'Cầu lông tiêu chuẩn', 'CN01', 90000, N'Trống', '/images/badminton-court.jpg'),
('CL05', N'Sân cầu lông 5', N'Cầu lông tiêu chuẩn', 'CN01', 90000, N'Trống', '/images/badminton-court.jpg'),
('CL06', N'Sân cầu lông 6 (VIP)', N'Cầu lông VIP', 'CN01', 120000, N'Bảo trì', '/images/badminton-court.jpg');

-- 8.4 Danh sách mặt hàng quầy & Dụng cụ cho thuê
INSERT INTO products (product_code, product_name, category, unit, unit_price, cost_price, stock_quantity, min_quantity, branch_code) VALUES
('P01', N'Nước bù khoáng Revive Chanh muối 500ml', N'Nước giải khát', N'Chai', 15000, 8000, 150, 20, 'CN01'),
('P02', N'Nước điện giải Pocari Sweat 500ml', N'Nước giải khát', N'Chai', 18000, 11000, 120, 20, 'CN01'),
('P03', N'Nước suối tinh khiết Aquafina 500ml', N'Nước giải khát', N'Chai', 10000, 5000, 220, 30, 'CN01'),
('P04', N'Ống Cầu Lông Hải Yến Đỏ (12 quả)', N'Cầu lông', N'Ống', 180000, 145000, 65, 10, 'CN01'),
('P05', N'Ống Cầu Lông Thành Công 77 (12 quả)', N'Cầu lông', N'Ống', 210000, 175000, 40, 10, 'CN01'),
('P06', N'Thuê Vợt Yonex Astrox 77 Play', N'Dụng cụ', N'Lượt', 25000, 0, 20, 5, 'CN01'),
('P07', N'Quấn cán vợt cầu lông VS chống trượt', N'Phụ kiện', N'Cái', 15000, 7000, 85, 15, 'CN01');

-- 8.5 Danh sách đơn đặt sân mẫu
INSERT INTO bookings (booking_code, customer_name, customer_phone, court_code, branch_code, booking_date, time_slot, hourly_price, total_price, deposit_amount, status, notes) VALUES
('DS0101', N'Nguyễn Hoàng Nam', '0903123456', 'CL02', 'CN01', CAST(GETDATE() AS DATE), '08:00 - 10:00', 90000, 180000, 54000, N'Đang sử dụng', N'Khách quen tuần 3 buổi'),
('DS0102', N'Lê Minh Đức', '0912888999', 'CL01', 'CN01', CAST(GETDATE() AS DATE), '09:00 - 10:00', 120000, 120000, 36000, N'Hoàn thành', N'Đã thanh toán đủ tiền mặt'),
('DS0103', N'Phạm Gia Huy', '0988777666', 'CL06', 'CN01', CAST(GETDATE() AS DATE), '18:00 - 20:00', 120000, 240000, 72000, N'Đã xác nhận', N'Đặt sân VIP tối'),
('DS0104', N'Vũ Thị Mai Anh', '0977555444', 'CL03', 'CN01', CAST(GETDATE() AS DATE), '19:00 - 20:00', 90000, 90000, 27000, N'Chờ xác nhận', N'Khách đặt qua app trực tuyến'),
('DS0105', N'Trần Thanh Hằng', '0933444555', 'CL01', 'CN01', DATEADD(DAY, 1, CAST(GETDATE() AS DATE)), '17:00 - 18:00', 120000, 120000, 36000, N'Đã cọc 30%', N'Chuyển khoản VietQR'),
('DS0106', N'Bùi Khánh Linh', '0909111222', 'CL04', 'CN01', DATEADD(DAY, 1, CAST(GETDATE() AS DATE)), '20:00 - 22:00', 90000, 180000, 54000, N'Đã xác nhận', N'Giao lưu công ty'),
('DS0107', N'Đỗ Quang Vinh', '0944333222', 'CL02', 'CN01', DATEADD(DAY, -1, CAST(GETDATE() AS DATE)), '15:00 - 16:00', 90000, 90000, 0, N'Đã hủy', N'Bận đột xuất');
GO
