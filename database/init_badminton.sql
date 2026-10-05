-- =====================================================================
-- DỰ ÁN QUẢN LÝ CHUỖI SÂN CẦU LÔNG - UTE SPORT
-- HỆ QUẢN TRỊ CƠ SỞ DỮ LIỆU: MICROSOFT SQL SERVER
-- TÀI KHOẢN KẾT NỐI MẶC ĐỊNH: sa / 123456
-- TIÊU CHUẨN THIẾT KẾ: ĐỒNG BỘ 100% THEO FILE WORD ĐẶC TẢ DỰ ÁN & 25 MÀN HÌNH WEB
-- =====================================================================

-- 1. TẠO CƠ SỞ DỮ LIỆU NẾU CHƯA CÓ
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'badminton')
BEGIN
    CREATE DATABASE badminton;
END
GO

USE badminton;
GO

-- 2. XÓA CÁC BẢNG VÀ VIEW CŨ ĐỂ KHỞI TẠO MỚI TOÀN DIỆN
-- 2.1 Xóa các View tiếng Việt nếu đã tồn tại
IF OBJECT_ID('ChiNhanh', 'V') IS NOT NULL DROP VIEW ChiNhanh;
IF OBJECT_ID('San', 'V') IS NOT NULL DROP VIEW San;
IF OBJECT_ID('KhungGio', 'V') IS NOT NULL DROP VIEW KhungGio;
IF OBJECT_ID('BangGia', 'V') IS NOT NULL DROP VIEW BangGia;
IF OBJECT_ID('KhachHang', 'V') IS NOT NULL DROP VIEW KhachHang;
IF OBJECT_ID('DonDatSan', 'V') IS NOT NULL DROP VIEW DonDatSan;
IF OBJECT_ID('DungCu', 'V') IS NOT NULL DROP VIEW DungCu;
IF OBJECT_ID('SanPham', 'V') IS NOT NULL DROP VIEW SanPham;
IF OBJECT_ID('HoaDon', 'V') IS NOT NULL DROP VIEW HoaDon;
IF OBJECT_ID('ChiTietThueDungCu', 'V') IS NOT NULL DROP VIEW ChiTietThueDungCu;
IF OBJECT_ID('ChiTietBanSanPham', 'V') IS NOT NULL DROP VIEW ChiTietBanSanPham;
IF OBJECT_ID('NhanVien', 'V') IS NOT NULL DROP VIEW NhanVien;
IF OBJECT_ID('TaiKhoan', 'V') IS NOT NULL DROP VIEW TaiKhoan;
IF OBJECT_ID('GiaiDau', 'V') IS NOT NULL DROP VIEW GiaiDau;
IF OBJECT_ID('DangKyGiaiDau', 'V') IS NOT NULL DROP VIEW DangKyGiaiDau;
IF OBJECT_ID('ThanhToan', 'V') IS NOT NULL DROP VIEW ThanhToan;
IF OBJECT_ID('PhanCa', 'V') IS NOT NULL DROP VIEW PhanCa;
IF OBJECT_ID('PhieuNhapKho', 'V') IS NOT NULL DROP VIEW PhieuNhapKho;
IF OBJECT_ID('ChiTietNhapDungCu', 'V') IS NOT NULL DROP VIEW ChiTietNhapDungCu;
IF OBJECT_ID('ChiTietNhapSanPham', 'V') IS NOT NULL DROP VIEW ChiTietNhapSanPham;
IF OBJECT_ID('KhuyenMai', 'V') IS NOT NULL DROP VIEW KhuyenMai;
IF OBJECT_ID('NhatKyHoatDong', 'V') IS NOT NULL DROP VIEW NhatKyHoatDong;
IF OBJECT_ID('TranDau', 'V') IS NOT NULL DROP VIEW TranDau;
IF OBJECT_ID('CauHinhHeThong', 'V') IS NOT NULL DROP VIEW CauHinhHeThong;
GO

-- 2.2 Xóa các bảng dữ liệu cũ
IF OBJECT_ID('tournament_matches', 'U') IS NOT NULL DROP TABLE tournament_matches;
IF OBJECT_ID('tournament_registrations', 'U') IS NOT NULL DROP TABLE tournament_registrations;
IF OBJECT_ID('tournaments', 'U') IS NOT NULL DROP TABLE tournaments;
IF OBJECT_ID('invoice_equipment_details', 'U') IS NOT NULL DROP TABLE invoice_equipment_details;
IF OBJECT_ID('invoice_product_details', 'U') IS NOT NULL DROP TABLE invoice_product_details;
IF OBJECT_ID('invoices', 'U') IS NOT NULL DROP TABLE invoices;
IF OBJECT_ID('payments', 'U') IS NOT NULL DROP TABLE payments;
IF OBJECT_ID('booking_services', 'U') IS NOT NULL DROP TABLE booking_services;
IF OBJECT_ID('bookings', 'U') IS NOT NULL DROP TABLE bookings;
IF OBJECT_ID('pricing_rules', 'U') IS NOT NULL DROP TABLE pricing_rules;
IF OBJECT_ID('time_slots', 'U') IS NOT NULL DROP TABLE time_slots;
IF OBJECT_ID('courts', 'U') IS NOT NULL DROP TABLE courts;
IF OBJECT_ID('stock_equipment_details', 'U') IS NOT NULL DROP TABLE stock_equipment_details;
IF OBJECT_ID('stock_product_details', 'U') IS NOT NULL DROP TABLE stock_product_details;
IF OBJECT_ID('stock_receipts', 'U') IS NOT NULL DROP TABLE stock_receipts;
IF OBJECT_ID('equipment', 'U') IS NOT NULL DROP TABLE equipment;
IF OBJECT_ID('products', 'U') IS NOT NULL DROP TABLE products;
IF OBJECT_ID('work_shifts', 'U') IS NOT NULL DROP TABLE work_shifts;
IF OBJECT_ID('audit_logs', 'U') IS NOT NULL DROP TABLE audit_logs;
IF OBJECT_ID('users', 'U') IS NOT NULL DROP TABLE users;
IF OBJECT_ID('staff', 'U') IS NOT NULL DROP TABLE staff;
IF OBJECT_ID('customers', 'U') IS NOT NULL DROP TABLE customers;
IF OBJECT_ID('promotions', 'U') IS NOT NULL DROP TABLE promotions;
IF OBJECT_ID('system_settings', 'U') IS NOT NULL DROP TABLE system_settings;
IF OBJECT_ID('branches', 'U') IS NOT NULL DROP TABLE branches;
GO

-- =====================================================================
-- 3. ĐỊNH NGHĨA 24 BẢNG DỮ LIỆU CỐT LÕI (CORE TABLES)
-- =====================================================================

-- BẢNG 1: CHI NHÁNH TOÀN CHUỖI (branches / ChiNhanh)
CREATE TABLE branches (
    id INT IDENTITY(1,1) PRIMARY KEY,
    branch_code VARCHAR(20) NOT NULL UNIQUE,
    branch_name NVARCHAR(100) NOT NULL,
    address NVARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    operating_hours NVARCHAR(100) DEFAULT N'06:00 - 23:00 hàng ngày',
    manager_name NVARCHAR(100),
    total_courts INT DEFAULT 8,
    status NVARCHAR(30) DEFAULT N'Hoạt động'
);
GO

-- BẢNG 2: SÂN CẦU LÔNG (courts / San)
CREATE TABLE courts (
    id INT IDENTITY(1,1) PRIMARY KEY,
    court_code VARCHAR(20) NOT NULL UNIQUE,
    branch_code VARCHAR(20) NOT NULL REFERENCES branches(branch_code),
    court_name NVARCHAR(100) NOT NULL,
    court_type NVARCHAR(50) NOT NULL, -- Cầu lông VIP (Thảm Yonex), Cầu lông tiêu chuẩn (BWF), Cầu lông thi đấu
    hourly_rate DECIMAL(18,2) NOT NULL,
    status NVARCHAR(30) DEFAULT N'Trống', -- Trống, Đang sử dụng, Bảo trì, Đặt trước
    image_url VARCHAR(255) DEFAULT '/images/badminton-court.jpg'
);
GO

-- BẢNG 3: KHUNG GIỜ HOẠT ĐỘNG (time_slots / KhungGio)
CREATE TABLE time_slots (
    slot_code VARCHAR(20) NOT NULL PRIMARY KEY,
    start_time VARCHAR(10) NOT NULL,
    end_time VARCHAR(10) NOT NULL,
    slot_type NVARCHAR(50) NOT NULL -- Thường (Sáng), Thường (Chiều), Giờ vàng cao điểm, Tối muộn
);
GO

-- BẢNG 4: BẢNG GIÁ THEO KHUNG GIỜ & NGÀY (pricing_rules / BangGia)
CREATE TABLE pricing_rules (
    pricing_code VARCHAR(20) NOT NULL PRIMARY KEY,
    branch_code VARCHAR(20) NOT NULL REFERENCES branches(branch_code),
    slot_code VARCHAR(20) NOT NULL REFERENCES time_slots(slot_code),
    day_type NVARCHAR(50) NOT NULL, -- Ngày thường (T2-T6), Cuối tuần (T7-CN), Ngày lễ
    start_date DATE NOT NULL,
    end_date DATE NULL,
    hourly_rate DECIMAL(18,2) NOT NULL
);
GO

-- BẢNG 5: KHÁCH HÀNG THÀNH VIÊN (customers / KhachHang)
CREATE TABLE customers (
    id INT IDENTITY(1,1) PRIMARY KEY,
    customer_code VARCHAR(20) NOT NULL UNIQUE,
    full_name NVARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100),
    password VARCHAR(100) NOT NULL DEFAULT '123456',
    total_play_hours DECIMAL(10,1) DEFAULT 0,
    reward_points INT DEFAULT 0,
    membership_tier NVARCHAR(30) DEFAULT N'Đồng', -- Đồng, Bạc, Vàng, Kim Cương
    status NVARCHAR(30) DEFAULT N'Hoạt động',
    created_at DATETIME DEFAULT GETDATE()
);
GO

-- BẢNG 6: ĐƠN ĐẶT SÂN (bookings / DonDatSan)
CREATE TABLE bookings (
    id INT IDENTITY(1,1) PRIMARY KEY,
    booking_code VARCHAR(30) NOT NULL UNIQUE,
    customer_code VARCHAR(20) NULL REFERENCES customers(customer_code),
    customer_name NVARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    court_code VARCHAR(20) NOT NULL REFERENCES courts(court_code),
    branch_code VARCHAR(20) NOT NULL REFERENCES branches(branch_code),
    booking_date DATE NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    slot_code VARCHAR(20) NULL REFERENCES time_slots(slot_code),
    hourly_price DECIMAL(18,2) NOT NULL,
    total_price DECIMAL(18,2) NOT NULL,
    deposit_amount DECIMAL(18,2) DEFAULT 0,
    payment_method NVARCHAR(50) DEFAULT N'Tiền mặt', -- Tiền mặt, Chuyển khoản VietQR, VNPay, MoMo
    status NVARCHAR(50) NOT NULL, -- Đang sử dụng, Đã xác nhận, Chờ xác nhận, Đã cọc 30%, Hoàn thành, Đã hủy
    notes NVARCHAR(255),
    created_at DATETIME DEFAULT GETDATE(),
    check_in_time DATETIME NULL,
    check_out_time DATETIME NULL
);
GO

-- BẢNG 7: DỤNG CỤ CHO THUÊ (equipment / DungCu)
CREATE TABLE equipment (
    equipment_code VARCHAR(20) NOT NULL PRIMARY KEY,
    branch_code VARCHAR(20) NOT NULL REFERENCES branches(branch_code),
    equipment_name NVARCHAR(100) NOT NULL,
    stock_quantity INT DEFAULT 0,
    rental_price DECIMAL(18,2) NOT NULL,
    condition NVARCHAR(50) DEFAULT N'Tốt' -- Mới 99%, Tốt, Cần căng cước
);
GO

-- BẢNG 8: SẢN PHẨM & ĐỒ UỐNG (products / SanPham)
CREATE TABLE products (
    id INT IDENTITY(1,1) PRIMARY KEY,
    product_code VARCHAR(20) NOT NULL UNIQUE,
    branch_code VARCHAR(20) NOT NULL REFERENCES branches(branch_code),
    product_name NVARCHAR(100) NOT NULL,
    category NVARCHAR(50) NOT NULL, -- Nước giải khát, Dụng cụ, Cầu lông, Phụ kiện
    unit NVARCHAR(20) NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    cost_price DECIMAL(18,2) NOT NULL,
    stock_quantity INT DEFAULT 0,
    min_quantity INT DEFAULT 10
);
GO

-- BẢNG 9: HÓA ĐƠN THANH TOÁN TẠI QUẦY POS (invoices / HoaDon)
CREATE TABLE invoices (
    id INT IDENTITY(1,1) PRIMARY KEY,
    invoice_code VARCHAR(30) NOT NULL UNIQUE,
    booking_code VARCHAR(30) NULL REFERENCES bookings(booking_code),
    staff_code VARCHAR(20) NULL,
    invoice_date DATETIME DEFAULT GETDATE(),
    court_fee DECIMAL(18,2) NOT NULL,
    equipment_fee DECIMAL(18,2) DEFAULT 0,
    product_fee DECIMAL(18,2) DEFAULT 0,
    discount_amount DECIMAL(18,2) DEFAULT 0,
    deposit_paid DECIMAL(18,2) DEFAULT 0,
    total_payment DECIMAL(18,2) NOT NULL,
    payment_method NVARCHAR(50) DEFAULT N'Tiền mặt',
    status NVARCHAR(30) DEFAULT N'Đã thanh toán'
);
GO

-- BẢNG 10: CHI TIẾT THUÊ DỤNG CỤ THEO HÓA ĐƠN (invoice_equipment_details / ChiTietThueDungCu)
CREATE TABLE invoice_equipment_details (
    id INT IDENTITY(1,1) PRIMARY KEY,
    invoice_code VARCHAR(30) NOT NULL REFERENCES invoices(invoice_code),
    equipment_code VARCHAR(20) NOT NULL REFERENCES equipment(equipment_code),
    quantity INT NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL
);
GO

-- BẢNG 11: CHI TIẾT BÁN SẢN PHẨM / ĐỒ UỐNG THEO HÓA ĐƠN (invoice_product_details / ChiTietBanSanPham)
CREATE TABLE invoice_product_details (
    id INT IDENTITY(1,1) PRIMARY KEY,
    invoice_code VARCHAR(30) NOT NULL REFERENCES invoices(invoice_code),
    product_code VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL
);
GO

-- BẢNG DỊCH VỤ ĐI KÈM ĐƠN ĐẶT SÂN (booking_services - Dành cho tương thích ngược)
CREATE TABLE booking_services (
    id INT IDENTITY(1,1) PRIMARY KEY,
    booking_code VARCHAR(30) NOT NULL REFERENCES bookings(booking_code),
    product_name NVARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL
);
GO

-- BẢNG 12: NHÂN VIÊN CHI NHÁNH (staff / NhanVien)
CREATE TABLE staff (
    id INT IDENTITY(1,1) PRIMARY KEY,
    staff_code VARCHAR(20) NOT NULL UNIQUE,
    branch_code VARCHAR(20) NOT NULL REFERENCES branches(branch_code),
    full_name NVARCHAR(100) NOT NULL,
    position NVARCHAR(50) NOT NULL, -- Quản lý chi nhánh, Thu ngân quầy POS, Kỹ thuật sân
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    address NVARCHAR(255),
    status NVARCHAR(30) DEFAULT N'Đang làm việc'
);
GO

-- BẢNG 13: TÀI KHOẢN NGƯỜI DÙNG & PHÂN QUYỀN (users / TaiKhoan)
CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(30) NOT NULL, -- DIRECTOR, MANAGER, POS, CUSTOMER, ADMIN
    staff_code VARCHAR(20) NULL REFERENCES staff(staff_code),
    branch_code VARCHAR(20) DEFAULT 'ALL',
    status NVARCHAR(30) DEFAULT N'Hoạt động',
    created_at DATETIME DEFAULT GETDATE(),
    last_login DATETIME NULL
);
GO

-- BẢNG 14: GIẢI ĐẤU VÀ SỰ KIỆN CẦU LÔNG (tournaments / GiaiDau)
CREATE TABLE tournaments (
    id INT IDENTITY(1,1) PRIMARY KEY,
    tournament_code VARCHAR(20) NOT NULL UNIQUE,
    branch_code VARCHAR(20) NULL REFERENCES branches(branch_code),
    tournament_name NVARCHAR(150) NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    rules NVARCHAR(MAX),
    max_participants INT DEFAULT 32,
    entry_fee DECIMAL(18,2) DEFAULT 0,
    total_prize DECIMAL(18,2) DEFAULT 0,
    status NVARCHAR(50) DEFAULT N'Đang mở đăng ký' -- Sắp mở đăng ký, Đang mở đăng ký, Đang diễn ra, Đã kết thúc
);
GO

-- BẢNG 15: ĐĂNG KÝ THAM GIA GIẢI ĐẤU (tournament_registrations / DangKyGiaiDau)
CREATE TABLE tournament_registrations (
    id INT IDENTITY(1,1) PRIMARY KEY,
    tournament_code VARCHAR(20) NOT NULL REFERENCES tournaments(tournament_code),
    customer_code VARCHAR(20) NOT NULL REFERENCES customers(customer_code),
    team_name NVARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    registration_date DATETIME DEFAULT GETDATE(),
    status NVARCHAR(30) DEFAULT N'Đã xác nhận' -- Chờ xác nhận, Đã xác nhận, Đã hủy
);
GO

-- BẢNG 16: GIAO DỊCH THANH TOÁN TRỰC TUYẾN & TẠI QUẦY (payments / ThanhToan)
CREATE TABLE payments (
    id INT IDENTITY(1,1) PRIMARY KEY,
    payment_code VARCHAR(30) NOT NULL UNIQUE,
    booking_code VARCHAR(30) NOT NULL REFERENCES bookings(booking_code),
    amount DECIMAL(18,2) NOT NULL,
    method NVARCHAR(50) NOT NULL, -- VietQR Pro, Tiền mặt, VNPay, MoMo
    transaction_type NVARCHAR(50) NOT NULL, -- Đặt cọc 30%, Thanh toán đủ, Hoàn tiền cọc
    transaction_ref VARCHAR(100),
    payment_time DATETIME DEFAULT GETDATE(),
    status NVARCHAR(30) DEFAULT N'Thành công'
);
GO

-- BẢNG 17: PHÂN CA LÀM VIỆC NHÂN VIÊN (work_shifts / PhanCa)
CREATE TABLE work_shifts (
    id INT IDENTITY(1,1) PRIMARY KEY,
    shift_code VARCHAR(20) NOT NULL UNIQUE,
    staff_code VARCHAR(20) NOT NULL REFERENCES staff(staff_code),
    shift_date DATE NOT NULL,
    start_time VARCHAR(10) NOT NULL,
    end_time VARCHAR(10) NOT NULL,
    shift_type NVARCHAR(30) NOT NULL, -- Ca Sáng (06:00-14:00), Ca Chiều (14:00-22:00), Ca Tối (17:00-23:00)
    status NVARCHAR(30) DEFAULT N'Đã phân ca' -- Đã phân ca, Đang trực, Đã hoàn thành
);
GO

-- BẢNG 18: PHIẾU NHẬP KHO VẬT TƯ (stock_receipts / PhieuNhapKho)
CREATE TABLE stock_receipts (
    id INT IDENTITY(1,1) PRIMARY KEY,
    receipt_code VARCHAR(30) NOT NULL UNIQUE,
    branch_code VARCHAR(20) NOT NULL REFERENCES branches(branch_code),
    staff_code VARCHAR(20) NOT NULL REFERENCES staff(staff_code),
    receipt_date DATETIME DEFAULT GETDATE(),
    supplier_name NVARCHAR(100) NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL,
    notes NVARCHAR(255)
);
GO

-- BẢNG 19: CHI TIẾT NHẬP DỤNG CỤ (stock_equipment_details / ChiTietNhapDungCu)
CREATE TABLE stock_equipment_details (
    id INT IDENTITY(1,1) PRIMARY KEY,
    receipt_code VARCHAR(30) NOT NULL REFERENCES stock_receipts(receipt_code),
    equipment_code VARCHAR(20) NOT NULL REFERENCES equipment(equipment_code),
    quantity INT NOT NULL,
    import_price DECIMAL(18,2) NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL
);
GO

-- BẢNG 20: CHI TIẾT NHẬP SẢN PHẨM / ĐỒ UỐNG (stock_product_details / ChiTietNhapSanPham)
CREATE TABLE stock_product_details (
    id INT IDENTITY(1,1) PRIMARY KEY,
    receipt_code VARCHAR(30) NOT NULL REFERENCES stock_receipts(receipt_code),
    product_code VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    import_price DECIMAL(18,2) NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL
);
GO

-- BẢNG 21: CHƯƠNG TRÌNH KHUYẾN MÃI (promotions / KhuyenMai)
CREATE TABLE promotions (
    id INT IDENTITY(1,1) PRIMARY KEY,
    promo_code VARCHAR(20) NOT NULL UNIQUE,
    promo_name NVARCHAR(150) NOT NULL,
    discount_type NVARCHAR(30) NOT NULL, -- Phần trăm (%), Số tiền (VNĐ)
    discount_value DECIMAL(18,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    scope NVARCHAR(50) DEFAULT N'Toàn chuỗi',
    status NVARCHAR(30) DEFAULT N'Đang áp dụng'
);
GO

-- BẢNG 22: NHẬT KÝ HOẠT ĐỘNG HỆ THỐNG (audit_logs / NhatKyHoatDong)
CREATE TABLE audit_logs (
    log_id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    action_type NVARCHAR(100) NOT NULL,
    action_time DATETIME DEFAULT GETDATE(),
    details NVARCHAR(255) NOT NULL,
    ip_address VARCHAR(50) DEFAULT '127.0.0.1',
    module NVARCHAR(50) DEFAULT N'Hệ thống',
    status NVARCHAR(30) DEFAULT N'Thành công'
);
GO

-- BẢNG 23: LỊCH THI ĐẤU & KẾT QUẢ TRẬN ĐẤU (tournament_matches / TranDau)
CREATE TABLE tournament_matches (
    id INT IDENTITY(1,1) PRIMARY KEY,
    match_code VARCHAR(20) NOT NULL UNIQUE,
    tournament_code VARCHAR(20) NOT NULL REFERENCES tournaments(tournament_code),
    court_code VARCHAR(20) NOT NULL REFERENCES courts(court_code),
    player1_name NVARCHAR(100) NOT NULL,
    player2_name NVARCHAR(100) NOT NULL,
    match_time DATETIME NOT NULL,
    result NVARCHAR(50) DEFAULT N'Chưa diễn ra',
    status NVARCHAR(30) DEFAULT N'Sắp diễn ra' -- Sắp diễn ra, Đang thi đấu, Đã kết thúc
);
GO

-- BẢNG 24: CẤU HÌNH THÔNG SỐ HỆ THỐNG (system_settings / CauHinhHeThong)
CREATE TABLE system_settings (
    id INT IDENTITY(1,1) PRIMARY KEY,
    setting_code VARCHAR(50) NOT NULL UNIQUE,
    setting_name NVARCHAR(100) NOT NULL,
    setting_value NVARCHAR(255) NOT NULL,
    description NVARCHAR(255),
    updated_at DATETIME DEFAULT GETDATE()
);
GO

-- =====================================================================
-- 4. TẠO CÁC VIEW ÁNH XẠ TIẾNG VIỆT THEO CHUẨN ĐẶC TẢ CHƯƠNG 5 FILE WORD
-- =====================================================================

CREATE VIEW ChiNhanh AS SELECT branch_code AS MaCN, branch_name AS TenCN, address AS DiaChi, phone AS SoDienThoai, operating_hours AS GioHoatDong FROM branches;
GO
CREATE VIEW San AS SELECT court_code AS MaSan, branch_code AS MaCN, court_name AS TenSan, court_type AS LoaiSan, status AS TinhTrang FROM courts;
GO
CREATE VIEW KhungGio AS SELECT slot_code AS MaKG, start_time AS ThoiGianBatDau, end_time AS ThoiGianKetThuc, slot_type AS LoaiKhungGio FROM time_slots;
GO
CREATE VIEW BangGia AS SELECT pricing_code AS MaBG, branch_code AS MaCN, slot_code AS MaKG, day_type AS LoaiNgay, start_date AS NgayBatDau, end_date AS NgayKetThuc, hourly_rate AS DonGia FROM pricing_rules;
GO
CREATE VIEW KhachHang AS SELECT customer_code AS MaKH, full_name AS HoTen, phone AS SoDienThoai, email AS Email, password AS MatKhau, total_play_hours AS TongGioChoi, reward_points AS DiemTichLuy, membership_tier AS HangThanhVien FROM customers;
GO
CREATE VIEW DonDatSan AS SELECT booking_code AS MaDatSan, customer_code AS MaKH, court_code AS MaSan, booking_date AS NgayChoi, slot_code AS MaKG, 1 AS SoGioDat, deposit_amount AS TienCoc, status AS TrangThai, created_at AS ThoiGianTao, check_in_time AS ThoiGianCheckIn, check_out_time AS ThoiGianCheckOut FROM bookings;
GO
CREATE VIEW DungCu AS SELECT equipment_code AS MaDungCu, branch_code AS MaCN, equipment_name AS TenDungCu, stock_quantity AS SoLuongTon, rental_price AS DonGiaThue, condition AS TinhTrang FROM equipment;
GO
CREATE VIEW SanPham AS SELECT product_code AS MaSP, branch_code AS MaCN, product_name AS TenSP, unit_price AS DonGia, stock_quantity AS SoLuongTon FROM products;
GO
CREATE VIEW HoaDon AS SELECT invoice_code AS MaHD, booking_code AS MaDatSan, staff_code AS MaNV, invoice_date AS NgayLap, court_fee AS TienSan, equipment_fee AS TienDungCu, product_fee AS TienDoUong, discount_amount AS ChietKhauThanhVien, deposit_paid AS TienCocDaTra, total_payment AS TongThanhToan FROM invoices;
GO
CREATE VIEW ChiTietThueDungCu AS SELECT invoice_code AS MaHD, equipment_code AS MaDungCu, quantity AS SoLuongThue, total_amount AS ThanhTien FROM invoice_equipment_details;
GO
CREATE VIEW ChiTietBanSanPham AS SELECT invoice_code AS MaHD, product_code AS MaSP, quantity AS SoLuongBan, total_amount AS ThanhTien FROM invoice_product_details;
GO
CREATE VIEW NhanVien AS SELECT staff_code AS MaNV, branch_code AS MaCN, full_name AS HoTen, position AS ChucVu, phone AS SoDienThoai, address AS DiaChi FROM staff;
GO
CREATE VIEW TaiKhoan AS SELECT username AS TenDangNhap, staff_code AS MaNV, password AS MatKhau, role AS VaiTro, status AS TrangThai FROM users;
GO
CREATE VIEW GiaiDau AS SELECT tournament_code AS MaGiaiDau, branch_code AS MaCN, tournament_name AS TenGiaiDau, start_date AS ThoiGianToChuc, rules AS TheLe, max_participants AS SoLuongToiDa, status AS TrangThai FROM tournaments;
GO
CREATE VIEW DangKyGiaiDau AS SELECT tournament_code AS MaGiaiDau, customer_code AS MaKH, registration_date AS NgayDangKy, status AS TrangThai FROM tournament_registrations;
GO
CREATE VIEW ThanhToan AS SELECT payment_code AS MaTT, booking_code AS MaDatSan, amount AS SoTien, method AS PhuongThuc, transaction_type AS LoaiGiaoDich, transaction_ref AS MaGiaoDich, payment_time AS ThoiGianThanhToan, status AS TrangThai FROM payments;
GO
CREATE VIEW PhanCa AS SELECT shift_code AS MaCa, staff_code AS MaNV, shift_date AS NgayLam, start_time AS GioBatDau, end_time AS GioKetThuc, status AS TrangThai FROM work_shifts;
GO
CREATE VIEW PhieuNhapKho AS SELECT receipt_code AS MaPN, branch_code AS MaCN, staff_code AS MaNV, receipt_date AS NgayNhap, total_amount AS TongTien, notes AS GhiChu FROM stock_receipts;
GO
CREATE VIEW ChiTietNhapDungCu AS SELECT receipt_code AS MaPN, equipment_code AS MaDungCu, quantity AS SoLuongNhap, import_price AS DonGiaNhap, total_amount AS ThanhTien FROM stock_equipment_details;
GO
CREATE VIEW ChiTietNhapSanPham AS SELECT receipt_code AS MaPN, product_code AS MaSP, quantity AS SoLuongNhap, import_price AS DonGiaNhap, total_amount AS ThanhTien FROM stock_product_details;
GO
CREATE VIEW KhuyenMai AS SELECT promo_code AS MaKM, promo_name AS TenKM, discount_type AS LoaiKM, discount_value AS GiaTriGiam, start_date AS NgayBatDau, end_date AS NgayKetThuc, scope AS PhamViApDung, status AS TrangThai FROM promotions;
GO
CREATE VIEW NhatKyHoatDong AS SELECT log_id AS MaLog, username AS TenDangNhap, action_type AS HanhDong, action_time AS ThoiGian, details AS NoiDung FROM audit_logs;
GO
CREATE VIEW TranDau AS SELECT match_code AS MaTran, tournament_code AS MaGiaiDau, court_code AS MaSan, player1_name AS MaKH1, player2_name AS MaKH2, match_time AS ThoiGianThiDau, result AS KetQua, status AS TrangThai FROM tournament_matches;
GO
CREATE VIEW CauHinhHeThong AS SELECT setting_code AS MaCauHinh, setting_name AS TenCauHinh, setting_value AS GiaTri, description AS MoTa, updated_at AS NgayCapNhat FROM system_settings;
GO

-- =====================================================================
-- 5. CHÈN DỮ LIỆU MẪU ĐẦY ĐỦ CHO TẤT CẢ 24 BẢNG (SEED DATA)
-- =====================================================================

-- 5.1 DỮ LIỆU CHI NHÁNH TOÀN CHUỖI (branches)
INSERT INTO branches (branch_code, branch_name, address, phone, operating_hours, manager_name, total_courts, status) VALUES
('CN01', N'Chi nhánh 1 - Sân Cầu Lông Thủ Đức', N'Số 01 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức', '0903123456', N'06:00 - 23:00 hàng ngày', N'Trần Phúc Bảo', 8, N'Hoạt động'),
('CN02', N'Chi nhánh 2 - Sân Cầu Lông Bình Thạnh', N'Số 234 Điện Biên Phủ, Phường 25, Bình Thạnh', '0903333444', N'06:00 - 23:00 hàng ngày', N'Nguyễn Văn Quản', 6, N'Hoạt động'),
('CN03', N'Chi nhánh 3 - Sân Cầu Lông Quận 9', N'Số 70 Đỗ Xuân Hợp, Phước Long B, TP. Thủ Đức', '0903789012', N'06:00 - 23:00 hàng ngày', N'Trần Hữu Nam', 6, N'Hoạt động'),
('CN04', N'Chi nhánh 4 - Sân Cầu Lông Gò Vấp', N'Số 18 Quang Trung, Phường 10, Gò Vấp', '0903555777', N'06:00 - 23:00 hàng ngày', N'Lê Hoàng Khang', 6, N'Hoạt động');

-- 5.2 DỮ LIỆU SÂN CẦU LÔNG (courts)
INSERT INTO courts (court_code, branch_code, court_name, court_type, hourly_rate, status, image_url) VALUES
-- Chi nhánh 1 - Thủ Đức
('CL01', 'CN01', N'Sân cầu lông 1 (VIP Yonex)', N'Cầu lông VIP', 120000, N'Trống', '/images/badminton-court.jpg'),
('CL02', 'CN01', N'Sân cầu lông 2', N'Cầu lông tiêu chuẩn', 90000, N'Đang sử dụng', '/images/badminton-court.jpg'),
('CL03', 'CN01', N'Sân cầu lông 3', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
('CL04', 'CN01', N'Sân cầu lông 4', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
('CL05', 'CN01', N'Sân cầu lông 5', N'Cầu lông tiêu chuẩn', 90000, N'Đang sử dụng', '/images/badminton-court.jpg'),
('CL06', 'CN01', N'Sân cầu lông 6 (VIP Yonex)', N'Cầu lông VIP', 120000, N'Bảo trì', '/images/badminton-court.jpg'),
('CL07', 'CN01', N'Sân cầu lông 7', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
('CL08', 'CN01', N'Sân cầu lông 8', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
-- Chi nhánh 2 - Bình Thạnh
('BT01', 'CN02', N'Sân BT 1 (VIP Victor)', N'Cầu lông VIP', 120000, N'Đang sử dụng', '/images/badminton-court.jpg'),
('BT02', 'CN02', N'Sân BT 2', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
('BT03', 'CN02', N'Sân BT 3', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
('BT04', 'CN02', N'Sân BT 4', N'Cầu lông tiêu chuẩn', 90000, N'Đang sử dụng', '/images/badminton-court.jpg'),
('BT05', 'CN02', N'Sân BT 5', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
('BT06', 'CN02', N'Sân BT 6 (VIP)', N'Cầu lông VIP', 120000, N'Trống', '/images/badminton-court.jpg'),
-- Chi nhánh 3 - Quận 9
('Q901', 'CN03', N'Sân Q9 1 (VIP Lining)', N'Cầu lông VIP', 120000, N'Trống', '/images/badminton-court.jpg'),
('Q902', 'CN03', N'Sân Q9 2', N'Cầu lông tiêu chuẩn', 90000, N'Đang sử dụng', '/images/badminton-court.jpg'),
('Q903', 'CN03', N'Sân Q9 3', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
('Q904', 'CN03', N'Sân Q9 4', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
('Q905', 'CN03', N'Sân Q9 5', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
('Q906', 'CN03', N'Sân Q9 6', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
-- Chi nhánh 4 - Gò Vấp
('GV01', 'CN04', N'Sân GV 1 (VIP Yonex)', N'Cầu lông VIP', 120000, N'Trống', '/images/badminton-court.jpg'),
('GV02', 'CN04', N'Sân GV 2', N'Cầu lông tiêu chuẩn', 90000, N'Đang sử dụng', '/images/badminton-court.jpg'),
('GV03', 'CN04', N'Sân GV 3', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
('GV04', 'CN04', N'Sân GV 4', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
('GV05', 'CN04', N'Sân GV 5', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg'),
('GV06', 'CN04', N'Sân GV 6', N'Cầu lông tiêu chuẩn', 90000, N'Trống', '/images/badminton-court.jpg');

-- 5.3 DỮ LIỆU KHUNG GIỜ HOẠT ĐỘNG (time_slots)
INSERT INTO time_slots (slot_code, start_time, end_time, slot_type) VALUES
('KG01', '06:00', '09:00', N'Thường (Sáng sớm)'),
('KG02', '09:00', '17:00', N'Thường (Ban ngày)'),
('KG03', '17:00', '22:00', N'Giờ vàng cao điểm'),
('KG04', '22:00', '23:00', N'Tối muộn');

-- 5.4 DỮ LIỆU BẢNG GIÁ (pricing_rules)
INSERT INTO pricing_rules (pricing_code, branch_code, slot_code, day_type, start_date, end_date, hourly_rate) VALUES
('BG01', 'CN01', 'KG01', N'Ngày thường (T2-T6)', '2026-01-01', NULL, 80000),
('BG02', 'CN01', 'KG02', N'Ngày thường (T2-T6)', '2026-01-01', NULL, 90000),
('BG03', 'CN01', 'KG03', N'Ngày thường (T2-T6)', '2026-01-01', NULL, 130000),
('BG04', 'CN01', 'KG04', N'Ngày thường (T2-T6)', '2026-01-01', NULL, 90000),
('BG05', 'CN01', 'KG01', N'Cuối tuần (T7-CN)', '2026-01-01', NULL, 110000),
('BG06', 'CN01', 'KG02', N'Cuối tuần (T7-CN)', '2026-01-01', NULL, 120000),
('BG07', 'CN01', 'KG03', N'Cuối tuần (T7-CN)', '2026-01-01', NULL, 150000),
('BG08', 'CN01', 'KG04', N'Cuối tuần (T7-CN)', '2026-01-01', NULL, 110000),
('BG09', 'CN02', 'KG03', N'Ngày thường (T2-T6)', '2026-01-01', NULL, 125000),
('BG10', 'CN03', 'KG03', N'Ngày thường (T2-T6)', '2026-01-01', NULL, 120000),
('BG11', 'CN04', 'KG03', N'Ngày thường (T2-T6)', '2026-01-01', NULL, 120000);

-- 5.5 DỮ LIỆU KHÁCH HÀNG THÀNH VIÊN (customers)
INSERT INTO customers (customer_code, full_name, phone, email, password, total_play_hours, reward_points, membership_tier) VALUES
('KH001', N'Lê Bá Đạt', '0912345678', 'dat.le@gmail.com', '123456', 42.5, 4250, N'Kim Cương'),
('KH002', N'Nguyễn Hoàng Nam', '0903123456', 'nam.nguyen@gmail.com', '123456', 28.0, 2800, N'Vàng'),
('KH003', N'Phạm Gia Huy', '0988777666', 'huy.pham@gmail.com', '123456', 15.0, 1500, N'Bạc'),
('KH004', N'Vũ Thị Mai Anh', '0977555444', 'maianh.vu@gmail.com', '123456', 8.0, 800, N'Bạc'),
('KH005', N'Trần Thanh Hằng', '0933444555', 'hang.tran@gmail.com', '123456', 4.0, 400, N'Đồng'),
('KH006', N'Bùi Khánh Linh', '0909111222', 'linh.bui@gmail.com', '123456', 3.5, 350, N'Đồng');

-- 5.6 DỮ LIỆU ĐƠN ĐẶT SÂN (bookings)
INSERT INTO bookings (booking_code, customer_code, customer_name, customer_phone, court_code, branch_code, booking_date, time_slot, slot_code, hourly_price, total_price, deposit_amount, payment_method, status, notes) VALUES
('DS0101', 'KH002', N'Nguyễn Hoàng Nam', '0903123456', 'CL02', 'CN01', CAST(GETDATE() AS DATE), '08:00 - 10:00', 'KG01', 90000, 180000, 54000, N'Tiền mặt', N'Đang sử dụng', N'Khách quen tuần 3 buổi'),
('DS0102', 'KH001', N'Lê Bá Đạt', '0912345678', 'CL01', 'CN01', CAST(GETDATE() AS DATE), '09:00 - 10:00', 'KG02', 120000, 120000, 36000, N'Tiền mặt', N'Hoàn thành', N'Đã thanh toán đủ tiền mặt'),
('DS0103', 'KH003', N'Phạm Gia Huy', '0988777666', 'CL05', 'CN01', CAST(GETDATE() AS DATE), '18:00 - 20:00', 'KG03', 120000, 240000, 72000, N'Chuyển khoản VietQR', N'Đang sử dụng', N'Đặt sân VIP tối'),
('DS0104', 'KH004', N'Vũ Thị Mai Anh', '0977555444', 'CL03', 'CN01', CAST(GETDATE() AS DATE), '19:00 - 20:00', 'KG03', 90000, 90000, 27000, N'VietQR Pro', N'Chờ xác nhận', N'Khách đặt qua web portal'),
('DS0105', 'KH005', N'Trần Thanh Hằng', '0933444555', 'CL01', 'CN01', DATEADD(DAY, 1, CAST(GETDATE() AS DATE)), '17:00 - 18:00', 'KG03', 120000, 120000, 36000, N'Chuyển khoản VietQR', N'Đã cọc 30%', N'Chuyển khoản VietQR giữ chỗ 10p'),
('DS0106', 'KH006', N'Bùi Khánh Linh', '0909111222', 'CL04', 'CN01', DATEADD(DAY, 1, CAST(GETDATE() AS DATE)), '20:00 - 22:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Đã xác nhận', N'Giao lưu công ty FPT'),
('DS0107', 'KH002', N'Đỗ Quang Vinh', '0944333222', 'CL02', 'CN01', DATEADD(DAY, -1, CAST(GETDATE() AS DATE)), '15:00 - 16:00', 'KG02', 90000, 90000, 0, N'Tiền mặt', N'Đã hủy', N'Bận công tác đột xuất'),
('DS0201', 'KH001', N'Lê Bá Đạt', '0912345678', 'BT01', 'CN02', CAST(GETDATE() AS DATE), '18:00 - 20:00', 'KG03', 120000, 240000, 72000, N'Chuyển khoản VietQR', N'Đang sử dụng', N'Sân VIP Bình Thạnh'),
('DS0202', 'KH003', N'Phạm Gia Huy', '0988777666', 'BT04', 'CN02', CAST(GETDATE() AS DATE), '19:00 - 21:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Đang sử dụng', N'Tập luyện đôi nam'),
('DS0301', 'KH004', N'Vũ Thị Mai Anh', '0977555444', 'Q902', 'CN03', CAST(GETDATE() AS DATE), '17:00 - 19:00', 'KG03', 90000, 180000, 54000, N'VietQR Pro', N'Đang sử dụng', N'Nhóm bạn sinh viên'),
('DS0401', 'KH005', N'Trần Thanh Hằng', '0933444555', 'GV02', 'CN04', CAST(GETDATE() AS DATE), '18:00 - 20:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Đang sử dụng', N'Khách vãng lai chi nhánh GV');

-- 5.7 DỮ LIỆU DỤNG CỤ CHO THUÊ (equipment)
INSERT INTO equipment (equipment_code, branch_code, equipment_name, stock_quantity, rental_price, condition) VALUES
('DC01', 'CN01', N'Vợt Yonex Astrox 77 Play (4U/G5)', 18, 25000, N'Mới 99%'),
('DC02', 'CN01', N'Vợt Yonex Astrox 88D Tour', 12, 35000, N'Tốt'),
('DC03', 'CN01', N'Vợt Victor Thruster K 9900', 10, 30000, N'Tốt'),
('DC04', 'CN01', N'Vợt Lining Axforce 80', 8, 30000, N'Tốt'),
('DC05', 'CN01', N'Ống Cầu Lông Hải Yến Đỏ (12 quả)', 45, 180000, N'Mới nguyên hộp'),
('DC06', 'CN01', N'Ống Cầu Lông Thành Công 77', 35, 210000, N'Mới nguyên hộp'),
('DC07', 'CN02', N'Vợt Yonex Astrox 77 Play', 15, 25000, N'Tốt'),
('DC08', 'CN03', N'Vợt Yonex Astrox 77 Play', 15, 25000, N'Tốt'),
('DC09', 'CN04', N'Vợt Yonex Astrox 77 Play', 15, 25000, N'Tốt');

-- 5.8 DỮ LIỆU SẢN PHẨM & ĐỒ UỐNG (products)
INSERT INTO products (product_code, branch_code, product_name, category, unit, unit_price, cost_price, stock_quantity, min_quantity) VALUES
('P01', 'CN01', N'Nước bù khoáng Revive Chanh muối 500ml', N'Nước giải khát', N'Chai', 15000, 8000, 150, 20),
('P02', 'CN01', N'Nước điện giải Pocari Sweat 500ml', N'Nước giải khát', N'Chai', 18000, 11000, 120, 20),
('P03', 'CN01', N'Nước suối tinh khiết Aquafina 500ml', N'Nước giải khát', N'Chai', 10000, 5000, 220, 30),
('P04', 'CN01', N'Nước tăng lực Bò Húc Red Bull Thái', N'Nước giải khát', N'Lon', 20000, 12000, 95, 15),
('P05', 'CN01', N'Ống Cầu Lông Hải Yến Đỏ (12 quả)', N'Cầu lông', N'Ống', 180000, 145000, 65, 10),
('P06', 'CN01', N'Ống Cầu Lông Thành Công 77 (12 quả)', N'Cầu lông', N'Ống', 210000, 175000, 40, 10),
('P07', 'CN01', N'Quấn cán vợt cầu lông VS chống trượt', N'Phụ kiện', N'Cái', 15000, 7000, 85, 15),
('P08', 'CN01', N'Khăn lau mồ hôi thể thao Yonex', N'Phụ kiện', N'Cái', 45000, 25000, 30, 5),
('P09', 'CN02', N'Nước bù khoáng Revive Chanh muối 500ml', N'Nước giải khát', N'Chai', 15000, 8000, 110, 20),
('P10', 'CN03', N'Nước bù khoáng Revive Chanh muối 500ml', N'Nước giải khát', N'Chai', 15000, 8000, 90, 20),
('P11', 'CN04', N'Nước bù khoáng Revive Chanh muối 500ml', N'Nước giải khát', N'Chai', 15000, 8000, 100, 20);

-- 5.9 DỮ LIỆU HÓA ĐƠN THANH TOÁN (invoices)
INSERT INTO invoices (invoice_code, booking_code, staff_code, invoice_date, court_fee, equipment_fee, product_fee, discount_amount, deposit_paid, total_payment, payment_method, status) VALUES
('HD0101', 'DS0102', 'NS03', DATEADD(MINUTE, -45, GETDATE()), 120000, 25000, 30000, 15000, 36000, 124000, N'Tiền mặt', N'Đã thanh toán'),
('HD0102', 'DS0101', 'NS03', DATEADD(HOUR, -2, GETDATE()), 180000, 0, 45000, 18000, 54000, 153000, N'Chuyển khoản VietQR', N'Đã thanh toán'),
('HD0103', 'DS0107', 'NS03', DATEADD(DAY, -1, GETDATE()), 90000, 25000, 15000, 0, 0, 130000, N'Tiền mặt', N'Đã thanh toán');

-- 5.10 CHI TIẾT THUÊ DỤNG CỤ THEO HÓA ĐƠN (invoice_equipment_details)
INSERT INTO invoice_equipment_details (invoice_code, equipment_code, quantity, unit_price, total_amount) VALUES
('HD0101', 'DC01', 1, 25000, 25000),
('HD0103', 'DC01', 1, 25000, 25000);

-- 5.11 CHI TIẾT BÁN SẢN PHẨM THEO HÓA ĐƠN (invoice_product_details)
INSERT INTO invoice_product_details (invoice_code, product_code, quantity, unit_price, total_amount) VALUES
('HD0101', 'P01', 2, 15000, 30000),
('HD0102', 'P01', 1, 15000, 15000),
('HD0102', 'P02', 1, 18000, 18000),
('HD0102', 'P07', 1, 12000, 12000);

-- DỊCH VỤ ĐI KÈM ĐƠN ĐẶT SÂN (booking_services)
INSERT INTO booking_services (booking_code, product_name, quantity, unit_price, total_amount) VALUES
('DS0101', N'Nước Revive Chanh muối', 2, 15000, 30000),
('DS0102', N'Thuê Vợt Yonex Astrox 77', 1, 25000, 25000),
('DS0103', N'Nước điện giải Pocari Sweat', 2, 18000, 36000),
('DS0105', N'Ống Cầu Hải Yến Đỏ', 1, 180000, 180000);

-- 5.12 DỮ LIỆU NHÂN VIÊN CHI NHÁNH (staff)
INSERT INTO staff (staff_code, branch_code, full_name, position, phone, email, address) VALUES
('NS01', 'CN01', N'Võ Hoàng Nam', N'Giám đốc điều hành', '0901111222', 'nam.vh@utesport.vn', N'Quận 1, TP. Hồ Chí Minh'),
('NS02', 'CN01', N'Trần Phúc Bảo', N'Quản lý chi nhánh Thủ Đức', '0903123456', 'bao.tran@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh'),
('NS03', 'CN01', N'Nguyễn Thanh Tâm', N'Thu ngân quầy POS', '0904555666', 'tam.nguyen@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh'),
('NS04', 'CN01', N'Lê Thị Mai', N'Thu ngân quầy POS', '0904777888', 'mai.lt@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh'),
('NS05', 'CN02', N'Nguyễn Văn Quản', N'Quản lý chi nhánh Bình Thạnh', '0903333444', 'quan.nv@utesport.vn', N'Bình Thạnh, TP. Hồ Chí Minh'),
('NS06', 'CN03', N'Trần Hữu Nam', N'Quản lý chi nhánh Quận 9', '0903789012', 'nam.th@utesport.vn', N'Quận 9, TP. Hồ Chí Minh'),
('NS07', 'CN04', N'Lê Hoàng Khang', N'Quản lý chi nhánh Gò Vấp', '0903555777', 'khang.lh@utesport.vn', N'Gò Vấp, TP. Hồ Chí Minh'),
('NS08', 'CN04', N'Lê Văn Hùng', N'Nhân viên kỹ thuật sân', '0908111333', 'hung.lv@utesport.vn', N'Gò Vấp, TP. Hồ Chí Minh');

-- 5.13 DỮ LIỆU TÀI KHOẢN ĐĂNG NHẬP HỆ THỐNG (users)
INSERT INTO users (username, password, full_name, email, phone, role, staff_code, branch_code) VALUES
('director', '123456', N'Nguyễn Phước Thọ', 'director@utesport.vn', '0901111222', 'DIRECTOR', 'NS01', 'ALL'),
('manager', '123456', N'Trần Phúc Bảo', 'manager@utesport.vn', '0903123456', 'MANAGER', 'NS02', 'CN01'),
('pos', '123456', N'Nguyễn Thanh Tâm', 'pos@utesport.vn', '0904555666', 'POS', 'NS03', 'CN01'),
('admin', '123456', N'Trần Biểu Hương', 'admin@utesport.vn', '0908888999', 'ADMIN', NULL, 'ALL'),
('customer', '123456', N'Lê Bá Đạt', 'khachhang@gmail.com', '0912345678', 'CUSTOMER', NULL, 'CN01'),
('nam.vh', '123456', N'Võ Hoàng Nam', 'nam.vh@utesport.vn', '0901111222', 'DIRECTOR', 'NS01', 'ALL'),
('quan.nv', '123456', N'Nguyễn Văn Quản', 'quan.nv@utesport.vn', '0903333444', 'MANAGER', 'NS05', 'CN02'),
('nam.th', '123456', N'Trần Hữu Nam', 'nam.th@utesport.vn', '0903789012', 'MANAGER', 'NS06', 'CN03'),
('khang.lh', '123456', N'Lê Hoàng Khang', 'khang.lh@utesport.vn', '0903555777', 'MANAGER', 'NS07', 'CN04'),
('mai.lt', '123456', N'Lê Thị Mai', 'mai.lt@utesport.vn', '0904777888', 'POS', 'NS04', 'CN01');

-- 5.14 DỮ LIỆU GIẢI ĐẤU VÀ SỰ KIỆN (tournaments)
INSERT INTO tournaments (tournament_code, branch_code, tournament_name, start_date, end_date, rules, max_participants, entry_fee, total_prize, status) VALUES
('GD01', 'CN01', N'Giải Cầu Lông Đôi Nam Nữ Mùa Thu UTE Open 2026', '2026-10-10 08:00:00', '2026-10-12 18:00:00', N'Thi đấu theo thể thức loại trực tiếp 3 hiệp 21 điểm theo luật BWF. Mỗi VĐV chỉ được đăng ký 1 nội dung.', 32, 300000, 20000000, N'Đang mở đăng ký'),
('GD02', 'CN01', N'Giải Cầu Lông Doanh Nghiệp & Sinh Viên Cúp Thủ Đức 2026', '2026-10-25 08:00:00', '2026-10-26 17:00:00', N'Giải đấu phong trào kết nối các doanh nghiệp và cựu sinh viên đam mê cầu lông.', 24, 250000, 15000000, N'Sắp mở đăng ký'),
('GD03', 'CN02', N'Giải Đơn Nam Bình Thạnh Mở Rộng 2026', '2026-11-05 08:00:00', '2026-11-06 18:00:00', N'Giải đơn nam các CLB phong trào khu vực Bình Thạnh - Phú Nhuận.', 16, 200000, 10000000, N'Sắp mở đăng ký');

-- 5.15 DỮ LIỆU ĐĂNG KÝ THAM GIA GIẢI ĐẤU (tournament_registrations)
INSERT INTO tournament_registrations (tournament_code, customer_code, team_name, phone, status) VALUES
('GD01', 'KH001', N'Cặp Đôi Đạt & Trang (CLB Thủ Đức)', '0912345678', N'Đã xác nhận'),
('GD01', 'KH002', N'Cặp Đôi Nam & Tuấn (ĐH Sư Phạm Kỹ Thuật)', '0903123456', N'Đã xác nhận'),
('GD01', 'KH003', N'Cặp Đôi Huy & Hằng (Công ty TMA)', '0988777666', N'Đã xác nhận'),
('GD01', 'KH004', N'Cặp Đôi Mai Anh & Đức (CLB Yonex)', '0977555444', N'Đã xác nhận');

-- 5.16 DỮ LIỆU GIAO DỊCH THANH TOÁN (payments)
INSERT INTO payments (payment_code, booking_code, amount, method, transaction_type, transaction_ref, status) VALUES
('TT0101', 'DS0101', 54000, N'Tiền mặt', N'Đặt cọc 30%', 'CASH-20260919-01', N'Thành công'),
('TT0102', 'DS0102', 36000, N'Tiền mặt', N'Đặt cọc 30%', 'CASH-20260919-02', N'Thành công'),
('TT0103', 'DS0103', 72000, N'VietQR Pro', N'Đặt cọc 30%', 'MB-998822110034', N'Thành công'),
('TT0104', 'DS0104', 27000, N'VietQR Pro', N'Đặt cọc 30%', 'VCB-776655441122', N'Đang xử lý'),
('TT0105', 'DS0105', 36000, N'VietQR Pro', N'Đặt cọc 30%', 'TCB-334455667788', N'Thành công'),
('TT0106', 'DS0201', 72000, N'VietQR Pro', N'Đặt cọc 30%', 'MB-556677889900', N'Thành công');

-- 5.17 DỮ LIỆU PHÂN CA LÀM VIỆC (work_shifts)
INSERT INTO work_shifts (shift_code, staff_code, shift_date, start_time, end_time, shift_type, status) VALUES
('CA01', 'NS03', CAST(GETDATE() AS DATE), '06:00', '14:00', N'Ca Sáng', N'Đang trực'),
('CA02', 'NS04', CAST(GETDATE() AS DATE), '14:00', '22:00', N'Ca Chiều', N'Đã phân ca'),
('CA03', 'NS03', DATEADD(DAY, 1, CAST(GETDATE() AS DATE)), '06:00', '14:00', N'Ca Sáng', N'Đã phân ca'),
('CA04', 'NS04', DATEADD(DAY, 1, CAST(GETDATE() AS DATE)), '14:00', '22:00', N'Ca Chiều', N'Đã phân ca'),
('CA05', 'NS08', CAST(GETDATE() AS DATE), '17:00', '23:00', N'Ca Tối', N'Đang trực');

-- 5.18 DỮ LIỆU PHIẾU NHẬP KHO (stock_receipts)
INSERT INTO stock_receipts (receipt_code, branch_code, staff_code, receipt_date, supplier_name, total_amount, notes) VALUES
('PN0101', 'CN01', 'NS02', DATEADD(DAY, -5, GETDATE()), N'Công ty TNHH Thể Thao Yonex Việt Nam', 14500000, N'Nhập bổ sung vợt Yonex Astrox và 50 ống cầu Hải Yến'),
('PN0102', 'CN01', 'NS02', DATEADD(DAY, -2, GETDATE()), N'NPP Dụng Cụ Cầu Lông Đại Phát', 4200000, N'Nhập nước bù khoáng Revive và Pocari Sweat đợt cuối tháng');

-- 5.19 CHI TIẾT NHẬP DỤNG CỤ (stock_equipment_details)
INSERT INTO stock_equipment_details (receipt_code, equipment_code, quantity, import_price, total_amount) VALUES
('PN0101', 'DC01', 10, 1800000, 18000000),
('PN0101', 'DC05', 30, 145000, 4350000);

-- 5.20 CHI TIẾT NHẬP SẢN PHẨM / ĐỒ UỐNG (stock_product_details)
INSERT INTO stock_product_details (receipt_code, product_code, quantity, import_price, total_amount) VALUES
('PN0102', 'P01', 200, 8000, 1600000),
('PN0102', 'P02', 150, 11000, 1650000),
('PN0102', 'P03', 190, 5000, 950000);

-- 5.21 DỮ LIỆU CHƯƠNG TRÌNH KHUYẾN MÃI (promotions)
INSERT INTO promotions (promo_code, promo_name, discount_type, discount_value, start_date, end_date, scope, status) VALUES
('KM01', N'Ưu Đãi Giờ Vàng Thứ Tư Hàng Tuần', N'Phần trăm (%)', 15, '2026-09-01', '2026-12-31', N'Toàn chuỗi', N'Đang áp dụng'),
('KM02', N'Đặc Quyền Hội Viên Kim Cương & Vàng', N'Phần trăm (%)', 15, '2026-01-01', '2026-12-31', N'Toàn chuỗi', N'Đang áp dụng'),
('KM03', N'Khai Trương Cụm Sân Gò Vấp - Tặng 1 Giờ Đánh', N'Số tiền (VNĐ)', 90000, '2026-10-01', '2026-10-31', N'Chi nhánh Gò Vấp', N'Đang áp dụng');

-- 5.22 DỮ LIỆU NHẬT KÝ HOẠT ĐỘNG (audit_logs)
INSERT INTO audit_logs (username, action_type, action_time, details, ip_address, module, status) VALUES
('nam.vh', N'Phê duyệt bảng giá giờ vàng Sân VIP Yonex', DATEADD(MINUTE, -15, GETDATE()), N'Phê duyệt giá 120.000đ cho khung giờ 17:00-22:00', '192.168.1.105', N'Phân hệ Giám đốc', N'Thành công'),
('mai.lt', N'Xác nhận nhận sân (Check-in) phiếu BK-1082', DATEADD(MINUTE, -48, GETDATE()), N'Check-in sân CL02 cho khách Nguyễn Hoàng Nam', '192.168.1.112', N'Phân hệ POS Bán Hàng', N'Thành công'),
('admin', N'Cập nhật ma trận phân quyền chi nhánh', DATEADD(HOUR, -2, GETDATE()), N'Phân quyền quản lý dữ liệu chi nhánh CN01 cho tài khoản bao.tran', '192.168.1.200', N'Phân hệ Quản trị', N'Thành công'),
('hacker_guest', N'Đăng nhập thất bại quá 5 lần liên tiếp', DATEADD(HOUR, -4, GETDATE()), N'Cảnh báo bảo mật: Brute-force thử mật khẩu tài khoản admin', '14.232.18.99', N'Cổng bảo mật xác thực', N'Cảnh báo');

-- 5.23 DỮ LIỆU TRẬN ĐẤU GIẢI ĐẤU (tournament_matches)
INSERT INTO tournament_matches (match_code, tournament_code, court_code, player1_name, player2_name, match_time, result, status) VALUES
('TR01', 'GD01', 'CL01', N'Đạt & Trang', N'Nam & Tuấn', '2026-10-10 08:30:00', N'2 - 1 (21-18, 19-21, 21-16)', N'Đã kết thúc'),
('TR02', 'GD01', 'CL02', N'Huy & Hằng', N'Mai Anh & Đức', '2026-10-10 09:30:00', N'0 - 2 (15-21, 18-21)', N'Đã kết thúc'),
('TR03', 'GD01', 'CL01', N'Đạt & Trang', N'Mai Anh & Đức', '2026-10-11 15:00:00', N'Chưa diễn ra', N'Sắp diễn ra');

-- 5.24 DỮ LIỆU CẤU HÌNH THÔNG SỐ QUY ĐỊNH (system_settings)
INSERT INTO system_settings (setting_code, setting_name, setting_value, description) VALUES
('CFG_DEPOSIT_RATE', N'Tỷ lệ đặt cọc tối thiểu (%)', '30', N'Khách hàng phải thanh toán cọc ít nhất 30% để giữ sân hợp lệ (QĐ-KH02)'),
('CFG_HOLDING_MINUTES', N'Thời gian tạm giữ chỗ chờ cọc (Phút)', '10', N'Sau 10 phút nếu không có giao dịch cọc, hệ thống tự động giải phóng ô sân (QĐ-KH02)'),
('CFG_CANCEL_HOURS', N'Thời hạn hủy sân tối thiểu để được hoàn cọc (Giờ)', '4', N'Hủy trước giờ chơi tối thiểu 4 tiếng được hoàn 100% tiền cọc (QĐ-KH03)'),
('CFG_ADVANCE_DAYS', N'Số ngày tối đa được phép đặt trước', '30', N'Giới hạn đặt sân trong vòng 30 ngày tới'),
('CFG_VNPAY_TMN', N'Mã đối tác VNPay (TMN Code)', 'UTESPORT2026', N'Tích hợp cổng thanh toán VNPay QR Sandbox/Production'),
('CFG_MOMO_PARTNER', N'Partner Code MoMo Business', 'MOMO_UTE_BADMINTON', N'Tích hợp cổng Ví điện tử MoMo Business');
GO

PRINT N'>>> ĐÃ KHỞI TẠO TOÀN DIỆN 24 BẢNG DỮ LIỆU, 24 VIEW TIẾNG VIỆT VÀ CHÈN SEED DATA HOÀN TẤT THÀNH CÔNG!';
GO
