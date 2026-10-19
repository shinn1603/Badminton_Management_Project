-- =====================================================================
-- BẢN SAO LƯU TOÀN DIỆN HỆ THỐNG QUẢN LÝ SÂN CẦU LÔNG UTE SPORT
-- Thời gian xuất: 2026-10-19 21:29:47
-- Người tạo: admin
-- =====================================================================

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

-- 2. XÓA CÁC BẢNG, VIEW, PROCEDURE, TRIGGER VÀ FUNCTION CŨ ĐỂ KHỞI TẠO MỚI TOÀN DIỆN
-- 2.0 Xóa Triggers, Procedures và Functions cũ nếu đã tồn tại
IF OBJECT_ID('dbo.trg_CheckBookingOverlap', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_CheckBookingOverlap;
IF OBJECT_ID('dbo.trg_UpdateStockOnInvoiceProduct', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_UpdateStockOnInvoiceProduct;
IF OBJECT_ID('dbo.trg_UpdateStockOnReceiptDetails', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_UpdateStockOnReceiptDetails;
IF OBJECT_ID('dbo.trg_UpdateCustomerMembershipLevel', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_UpdateCustomerMembershipLevel;
IF OBJECT_ID('dbo.trg_AuditCourtStatusChange', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_AuditCourtStatusChange;
IF OBJECT_ID('dbo.trg_ValidatePromotionDates', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_ValidatePromotionDates;

IF OBJECT_ID('dbo.sp_LockSlotAndCreateBooking', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_LockSlotAndCreateBooking;
IF OBJECT_ID('dbo.sp_ConfirmBookingDeposit', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_ConfirmBookingDeposit;
IF OBJECT_ID('dbo.sp_CheckInBooking', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_CheckInBooking;
IF OBJECT_ID('dbo.sp_CheckOutAndCreateInvoice', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_CheckOutAndCreateInvoice;
IF OBJECT_ID('dbo.sp_CancelBookingWithRefundPolicy', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_CancelBookingWithRefundPolicy;
IF OBJECT_ID('dbo.sp_ImportStockReceipt', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_ImportStockReceipt;
IF OBJECT_ID('dbo.sp_GetBranchDailyRevenueReport', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_GetBranchDailyRevenueReport;

IF OBJECT_ID('dbo.fn_CheckSlotAvailability', 'FN') IS NOT NULL DROP FUNCTION dbo.fn_CheckSlotAvailability;
IF OBJECT_ID('dbo.fn_CalculateBookingPrice', 'FN') IS NOT NULL DROP FUNCTION dbo.fn_CalculateBookingPrice;
IF OBJECT_ID('dbo.fn_GetCustomerTierDiscount', 'FN') IS NOT NULL DROP FUNCTION dbo.fn_GetCustomerTierDiscount;
IF OBJECT_ID('dbo.fn_GetBranchRevenue', 'FN') IS NOT NULL DROP FUNCTION dbo.fn_GetBranchRevenue;
IF OBJECT_ID('dbo.fn_GetAvailableCourtsByBranch', 'IF') IS NOT NULL DROP FUNCTION dbo.fn_GetAvailableCourtsByBranch;
GO

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
IF OBJECT_ID('pricing_proposals', 'U') IS NOT NULL DROP TABLE pricing_proposals;
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
-- 3. ĐỊNH NGHĨA 25 BẢNG DỮ LIỆU CỐT LÕI (CORE TABLES)
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
    customer_email VARCHAR(100) NULL,
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
    category NVARCHAR(50) DEFAULT N'Đôi nam nữ', -- Đơn nam, Đơn nữ, Đôi nam, Đôi nữ, Đôi nam nữ, Tổng hợp nhiều nội dung
    max_participants INT DEFAULT 32,
    current_participants INT DEFAULT 24,
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
    category NVARCHAR(50) DEFAULT N'Đôi nam nữ', -- Đơn nam, Đơn nữ, Đôi nam, Đôi nữ, Đôi nam nữ
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NULL,
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
    shift_code VARCHAR(50) NOT NULL UNIQUE,
    staff_code VARCHAR(20) NOT NULL REFERENCES staff(staff_code),
    branch_code VARCHAR(20) DEFAULT 'CN01' REFERENCES branches(branch_code),
    day_of_week VARCHAR(10) NULL,
    shift_type NVARCHAR(30) NOT NULL, -- morning, night, Ca Sáng, Ca Chiều, Ca Tối
    duty NVARCHAR(100) NULL, -- POS, Thu ngân, Lễ tân, Kỹ thuật, Quản lý
    icon VARCHAR(30) DEFAULT 'POS',
    shift_date DATE NOT NULL,
    start_time VARCHAR(10) NOT NULL,
    end_time VARCHAR(10) NOT NULL,
    status NVARCHAR(50) DEFAULT N'Đã phân ca' -- Đã phân ca, Đang trực, Đã hoàn thành
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
    details NVARCHAR(MAX) NOT NULL,
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

-- BẢNG 25: ĐỀ XUẤT ĐIỀU CHỈNH GIÁ SÂN (pricing_proposals / DeXuatGia)
CREATE TABLE pricing_proposals (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    proposal_code VARCHAR(20) NOT NULL UNIQUE,
    branch_code VARCHAR(20) NOT NULL REFERENCES branches(branch_code),
    proposer_name NVARCHAR(100) NOT NULL,
    court_type NVARCHAR(100) NOT NULL,
    slot_info NVARCHAR(100) NOT NULL,
    current_price DECIMAL(18,2) NOT NULL,
    proposed_price DECIMAL(18,2) NOT NULL,
    reason NVARCHAR(255) NULL,
    status NVARCHAR(30) DEFAULT N'Chờ phê duyệt', -- Chờ phê duyệt, Đã phê duyệt, Từ chối
    created_at DATETIME2 DEFAULT GETDATE(),
    approved_at DATETIME2 NULL,
    note NVARCHAR(255) NULL
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
CREATE VIEW DeXuatGia AS SELECT proposal_code AS MaDeXuat, branch_code AS MaCN, proposer_name AS NguoiDeXuat, court_type AS LoaiSan, slot_info AS KhungGio, current_price AS GiaHienTai, proposed_price AS GiaDeXuat, reason AS LyDo, status AS TrangThai, created_at AS NgayDeXuat, approved_at AS NgayPheDuyet, note AS GhiChuPheDuyet FROM pricing_proposals;
GO

-- =====================================================================

-- 5. DỮ LIỆU THỰC TẾ SAO LƯU
EXEC sp_MSforeachtable "ALTER TABLE ? NOCHECK CONSTRAINT all";
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: branches (5 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT branches ON;
INSERT INTO branches (id, branch_code, branch_name, address, phone, operating_hours, manager_name, total_courts, status) VALUES
(1, N'CN01', N'Chi nhánh 1 - Sân Cầu Lông Thủ Đức', N'Số 01 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức', N'0903123456', N'06:00 - 23:00 hàng ngày', N'Trần Phúc Bảo', 8, N'Hoạt động'),
(2, N'CN02', N'Chi nhánh 2 - Sân Cầu Lông Bình Thạnh', N'Số 234 Điện Biên Phủ, Phường 25, Bình Thạnh', N'0903333444', N'06:00 - 23:00 hàng ngày', N'Nguyễn Văn Quản', 6, N'Hoạt động'),
(3, N'CN03', N'Chi nhánh 3 - Sân Cầu Lông Quận 9', N'Số 70 Đỗ Xuân Hợp, Phước Long B, TP. Thủ Đức', N'0903789012', N'06:00 - 23:00 hàng ngày', N'Trần Hữu Nam', 6, N'Hoạt động'),
(4, N'CN04', N'Chi nhánh 4 - Sân Cầu Lông Gò Vấp', N'Số 18 Quang Trung, Phường 10, Gò Vấp', N'0903555777', N'06:00 - 23:00 hàng ngày', N'Lê Hoàng Khang', 6, N'Hoạt động'),
(5, N'ALL', N'Hội Sở / Ban Giám Đốc', N'Số 01 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức', N'0901111222', N'08:00 - 17:00 hàng ngày', N'Võ Hoàng Nam', 0, N'Hoạt động');
SET IDENTITY_INSERT branches OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: courts (26 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT courts ON;
INSERT INTO courts (id, court_code, branch_code, court_name, court_type, hourly_rate, status, image_url) VALUES
(1, N'CL01', N'CN01', N'Sân cầu lông 1 (VIP Yonex)', N'Cầu lông VIP', 120000.00, N'Trống', N'/images/badminton-court.jpg'),
(2, N'CL02', N'CN01', N'Sân cầu lông 2', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(3, N'CL03', N'CN01', N'Sân cầu lông 3', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(4, N'CL04', N'CN01', N'Sân cầu lông 4', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(5, N'CL05', N'CN01', N'Sân cầu lông 5', N'Cầu lông tiêu chuẩn', 90000.00, N'Đang sử dụng', N'/images/badminton-court.jpg'),
(6, N'CL06', N'CN01', N'Sân cầu lông 6 (VIP Yonex)', N'Cầu lông VIP', 120000.00, N'Bảo trì', N'/images/badminton-court.jpg'),
(7, N'CL07', N'CN01', N'Sân cầu lông 7', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(8, N'CL08', N'CN01', N'Sân cầu lông 8', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(9, N'BT01', N'CN02', N'Sân BT 1 (VIP Victor)', N'Cầu lông VIP', 120000.00, N'Đang sử dụng', N'/images/badminton-court.jpg'),
(10, N'BT02', N'CN02', N'Sân BT 2', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(11, N'BT03', N'CN02', N'Sân BT 3', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(12, N'BT04', N'CN02', N'Sân BT 4', N'Cầu lông tiêu chuẩn', 90000.00, N'Đang sử dụng', N'/images/badminton-court.jpg'),
(13, N'BT05', N'CN02', N'Sân BT 5', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(14, N'BT06', N'CN02', N'Sân BT 6 (VIP)', N'Cầu lông VIP', 120000.00, N'Trống', N'/images/badminton-court.jpg'),
(15, N'Q901', N'CN03', N'Sân Q9 1 (VIP Lining)', N'Cầu lông VIP', 120000.00, N'Trống', N'/images/badminton-court.jpg'),
(16, N'Q902', N'CN03', N'Sân Q9 2', N'Cầu lông tiêu chuẩn', 90000.00, N'Đang sử dụng', N'/images/badminton-court.jpg'),
(17, N'Q903', N'CN03', N'Sân Q9 3', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(18, N'Q904', N'CN03', N'Sân Q9 4', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(19, N'Q905', N'CN03', N'Sân Q9 5', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(20, N'Q906', N'CN03', N'Sân Q9 6', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(21, N'GV01', N'CN04', N'Sân GV 1 (VIP Yonex)', N'Cầu lông VIP', 120000.00, N'Trống', N'/images/badminton-court.jpg'),
(22, N'GV02', N'CN04', N'Sân GV 2', N'Cầu lông tiêu chuẩn', 90000.00, N'Đang sử dụng', N'/images/badminton-court.jpg'),
(23, N'GV03', N'CN04', N'Sân GV 3', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(24, N'GV04', N'CN04', N'Sân GV 4', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(25, N'GV05', N'CN04', N'Sân GV 5', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg'),
(26, N'GV06', N'CN04', N'Sân GV 6', N'Cầu lông tiêu chuẩn', 90000.00, N'Trống', N'/images/badminton-court.jpg');
SET IDENTITY_INSERT courts OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: time_slots (4 bản ghi)
-- -----------------------------------------------------
INSERT INTO time_slots (slot_code, start_time, end_time, slot_type) VALUES
(N'KG01', N'06:00', N'09:00', N'Thường (Sáng sớm)'),
(N'KG02', N'09:00', N'17:00', N'Thường (Ban ngày)'),
(N'KG03', N'17:00', N'22:00', N'Giờ vàng cao điểm'),
(N'KG04', N'22:00', N'23:00', N'Tối muộn');
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: pricing_rules (11 bản ghi)
-- -----------------------------------------------------
INSERT INTO pricing_rules (pricing_code, branch_code, slot_code, day_type, start_date, end_date, hourly_rate) VALUES
(N'BG01', N'CN01', N'KG01', N'Ngày thường (T2-T6)', N'2026-01-01', NULL, 80000.00),
(N'BG02', N'CN01', N'KG02', N'Ngày thường (T2-T6)', N'2026-01-01', NULL, 90000.00),
(N'BG03', N'CN01', N'KG03', N'Ngày thường (T2-T6)', N'2026-01-01', NULL, 130000.00),
(N'BG04', N'CN01', N'KG04', N'Ngày thường (T2-T6)', N'2026-01-01', NULL, 90000.00),
(N'BG05', N'CN01', N'KG01', N'Cuối tuần (T7-CN)', N'2026-01-01', NULL, 110000.00),
(N'BG06', N'CN01', N'KG02', N'Cuối tuần (T7-CN)', N'2026-01-01', NULL, 120000.00),
(N'BG07', N'CN01', N'KG03', N'Cuối tuần (T7-CN)', N'2026-01-01', NULL, 150000.00),
(N'BG08', N'CN01', N'KG04', N'Cuối tuần (T7-CN)', N'2026-01-01', NULL, 110000.00),
(N'BG09', N'CN02', N'KG03', N'Ngày thường (T2-T6)', N'2026-01-01', NULL, 125000.00),
(N'BG10', N'CN03', N'KG03', N'Ngày thường (T2-T6)', N'2026-01-01', NULL, 120000.00),
(N'BG11', N'CN04', N'KG03', N'Ngày thường (T2-T6)', N'2026-01-01', NULL, 120000.00);
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: customers (7 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT customers ON;
INSERT INTO customers (id, customer_code, full_name, phone, email, password, total_play_hours, reward_points, membership_tier, status, created_at) VALUES
(1, N'KH001', N'Lê Bá Đạt', N'0912345678', N'dat.le@gmail.com', N'123456', 42.5, 4250, N'Kim Cương', N'Hoạt động', N'2026-10-15 14:22:27.617'),
(2, N'KH002', N'Nguyễn Hoàng Nam', N'0903123456', N'nam.nguyen@gmail.com', N'123456', 28.0, 2800, N'Vàng', N'Hoạt động', N'2026-10-15 14:22:27.617'),
(3, N'KH003', N'Phạm Gia Huy', N'0988777666', N'huy.pham@gmail.com', N'123456', 15.0, 1500, N'Bạc', N'Hoạt động', N'2026-10-15 14:22:27.617'),
(4, N'KH004', N'Vũ Thị Mai Anh', N'0977555444', N'maianh.vu@gmail.com', N'123456', 8.0, 800, N'Bạc', N'Hoạt động', N'2026-10-15 14:22:27.617'),
(5, N'KH005', N'Trần Thanh Hằng', N'0933444555', N'hang.tran@gmail.com', N'123456', 4.0, 400, N'Đồng', N'Hoạt động', N'2026-10-15 14:22:27.617'),
(6, N'KH006', N'Bùi Khánh Linh', N'0909111222', N'linh.bui@gmail.com', N'123456', 3.5, 350, N'Đồng', N'Hoạt động', N'2026-10-15 14:22:27.617'),
(14, N'KH87872', N'Nguyen Van A', N'0908777666', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-19 14:09:35.27');
SET IDENTITY_INSERT customers OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: staff (12 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT staff ON;
INSERT INTO staff (id, staff_code, branch_code, full_name, position, phone, email, address, status) VALUES
(1, N'NS01', N'ALL', N'Võ Hoàng Nam', N'Giám đốc điều hành', N'0901111222', N'nam.vh@utesport.vn', N'Quận 1, TP. Hồ Chí Minh', N'Đang làm việc'),
(2, N'NS02', N'CN01', N'Trần Phúc Bảo', N'Quản lý chi nhánh Thủ Đức', N'0903123456', N'bao.tran@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh', N'Đang làm việc'),
(3, N'NS03', N'CN01', N'Nguyễn Thanh Tâm', N'Thu ngân quầy POS', N'0904555666', N'tam.nguyen@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh', N'Đang làm việc'),
(4, N'NS04', N'CN01', N'Lê Thị Mai', N'Thu ngân quầy POS', N'0904777888', N'mai.lt@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh', N'Đang làm việc'),
(5, N'NS05', N'CN02', N'Nguyễn Văn Quản', N'Quản lý chi nhánh Bình Thạnh', N'0903333444', N'quan.nv@utesport.vn', N'Bình Thạnh, TP. Hồ Chí Minh', N'Đang làm việc'),
(6, N'NS06', N'CN03', N'Trần Hữu Nam', N'Quản lý chi nhánh Quận 9', N'0903789012', N'nam.th@utesport.vn', N'Quận 9, TP. Hồ Chí Minh', N'Đang làm việc'),
(7, N'NS07', N'CN04', N'Lê Hoàng Khang', N'Quản lý chi nhánh Gò Vấp', N'0903555777', N'khang.lh@utesport.vn', N'Gò Vấp, TP. Hồ Chí Minh', N'Đang làm việc'),
(8, N'NS08', N'CN04', N'Lê Văn Hùng', N'Nhân viên kỹ thuật sân', N'0908111333', N'hung.lv@utesport.vn', N'Gò Vấp, TP. Hồ Chí Minh', N'Đang làm việc'),
(9, N'NS09', N'CN01', N'Bùi Khánh Linh', N'Lễ tân & Dịch vụ', N'0909111222', N'linh.bk@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh', N'Đang làm việc'),
(10, N'NS10', N'CN01', N'Nguyễn Hoàng Nam', N'Thu ngân POS quầy', N'0903888777', N'nam.nh@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh', N'Đang làm việc'),
(11, N'NS11', N'CN01', N'Phạm Gia Huy', N'Kỹ thuật & Bảo dưỡng sân', N'0988777666', N'huy.pg@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh', N'Đang làm việc'),
(12, N'NS12', N'CN01', N'Trịnh Gia Vinh', N'Kỹ thuật & Bảo dưỡng sân', N'0977222333', N'vinh.tg@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh', N'Đang làm việc');
SET IDENTITY_INSERT staff OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: users (10 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT users ON;
INSERT INTO users (id, username, password, full_name, email, phone, role, staff_code, branch_code, status, created_at, last_login) VALUES
(1, N'director', N'$2a$10$4YxyiJrN2fnXRF1XRI3ZBeoVJ9PMRijK.tl8M6veMfELkxw26hGOO', N'Nguyễn Phước Thọ', N'director@utesport.vn', N'0901111222', N'DIRECTOR', N'NS01', N'ALL', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL),
(2, N'manager', N'$2a$10$6FTz80RaQJpWOhhTEXnbaeQzOpiHbR8TpaBzCin.F3SdP5JTo3jIG', N'Trần Phúc Bảo', N'manager@utesport.vn', N'0903123456', N'MANAGER', N'NS02', N'CN01', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL),
(3, N'pos', N'$2a$10$mZZymrebYdkouM3QTa91z.WfjxfpYe.gYoGeWWSygklyZ24l76Ym2', N'Nguyễn Thanh Tâm', N'pos@utesport.vn', N'0904555666', N'POS', N'NS03', N'CN01', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL),
(4, N'admin', N'$2a$10$Rv6iWqvKG.8BQ8mT5688e.y.umw24Ikp2jgoAOSvWlY8DTdn6fWbW', N'Trần Biểu Hương', N'admin@utesport.vn', N'0908888999', N'ADMIN', NULL, N'ALL', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL),
(5, N'customer', N'$2a$10$xBd2zWe6O6bXIdDTymGSjuRP5KofOHFFh.XdfSLDUfP9VSt.LD76C', N'Lê Bá Đạt', N'khachhang@gmail.com', N'0912345678', N'CUSTOMER', NULL, N'CN01', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL),
(6, N'nam.vh', N'123456', N'Võ Hoàng Nam', N'nam.vh@utesport.vn', N'0901111222', N'DIRECTOR', N'NS01', N'ALL', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL),
(7, N'quan.nv', N'123456', N'Nguyễn Văn Quản', N'quan.nv@utesport.vn', N'0903333444', N'MANAGER', N'NS05', N'CN02', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL),
(8, N'nam.th', N'123456', N'Trần Hữu Nam', N'nam.th@utesport.vn', N'0903789012', N'MANAGER', N'NS06', N'CN03', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL),
(9, N'khang.lh', N'123456', N'Lê Hoàng Khang', N'khang.lh@utesport.vn', N'0903555777', N'MANAGER', N'NS07', N'CN04', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL),
(10, N'mai.lt', N'123456', N'Lê Thị Mai', N'mai.lt@utesport.vn', N'0904777888', N'POS', N'NS04', N'CN01', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL);
SET IDENTITY_INSERT users OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: equipment (9 bản ghi)
-- -----------------------------------------------------
INSERT INTO equipment (equipment_code, branch_code, equipment_name, stock_quantity, rental_price, condition) VALUES
(N'DC01', N'CN01', N'Vợt Yonex Astrox 77 Play (4U/G5)', 18, 25000.00, N'Mới 99%'),
(N'DC02', N'CN01', N'Vợt Yonex Astrox 88D Tour', 12, 35000.00, N'Tốt'),
(N'DC03', N'CN01', N'Vợt Victor Thruster K 9900', 10, 30000.00, N'Tốt'),
(N'DC04', N'CN01', N'Vợt Lining Axforce 80', 8, 30000.00, N'Tốt'),
(N'DC05', N'CN01', N'Ống Cầu Lông Hải Yến Đỏ (12 quả)', 45, 180000.00, N'Mới nguyên hộp'),
(N'DC06', N'CN01', N'Ống Cầu Lông Thành Công 77', 35, 210000.00, N'Mới nguyên hộp'),
(N'DC07', N'CN02', N'Vợt Yonex Astrox 77 Play', 15, 25000.00, N'Tốt'),
(N'DC08', N'CN03', N'Vợt Yonex Astrox 77 Play', 15, 25000.00, N'Tốt'),
(N'DC09', N'CN04', N'Vợt Yonex Astrox 77 Play', 15, 25000.00, N'Tốt');
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: products (11 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT products ON;
INSERT INTO products (id, product_code, branch_code, product_name, category, unit, unit_price, cost_price, stock_quantity, min_quantity) VALUES
(1, N'P01', N'CN01', N'Nước bù khoáng Revive Chanh muối 500ml', N'Nước giải khát', N'Chai', 15000.00, 7115.54, 150, 20),
(2, N'P02', N'CN01', N'Nước điện giải Pocari Sweat 500ml', N'Nước giải khát', N'Chai', 18000.00, 11000.00, 120, 20),
(3, N'P03', N'CN01', N'Nước suối tinh khiết Aquafina 500ml', N'Nước giải khát', N'Chai', 10000.00, 5000.00, 220, 30),
(4, N'P04', N'CN01', N'Nước tăng lực Bò Húc Red Bull Thái', N'Nước giải khát', N'Lon', 20000.00, 12000.00, 95, 15),
(5, N'P05', N'CN01', N'Ống Cầu Lông Hải Yến Đỏ (12 quả)', N'Cầu lông', N'Ống', 180000.00, 145000.00, 65, 10),
(6, N'P06', N'CN01', N'Ống Cầu Lông Thành Công 77 (12 quả)', N'Cầu lông', N'Ống', 210000.00, 175000.00, 40, 10),
(7, N'P07', N'CN01', N'Quấn cán vợt cầu lông VS chống trượt', N'Phụ kiện', N'Cái', 15000.00, 7000.00, 85, 15),
(8, N'P08', N'CN01', N'Khăn lau mồ hôi thể thao Yonex', N'Phụ kiện', N'Cái', 45000.00, 25000.00, 30, 5),
(9, N'P09', N'CN02', N'Nước bù khoáng Revive Chanh muối 500ml', N'Nước giải khát', N'Chai', 15000.00, 8000.00, 110, 20),
(10, N'P10', N'CN03', N'Nước bù khoáng Revive Chanh muối 500ml', N'Nước giải khát', N'Chai', 15000.00, 8000.00, 90, 20),
(11, N'P11', N'CN04', N'Nước bù khoáng Revive Chanh muối 500ml', N'Nước giải khát', N'Chai', 15000.00, 8000.00, 100, 20);
SET IDENTITY_INSERT products OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: bookings (48 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT bookings ON;
INSERT INTO bookings (id, booking_code, customer_code, customer_name, customer_phone, customer_email, court_code, branch_code, booking_date, time_slot, slot_code, hourly_price, total_price, deposit_amount, payment_method, status, notes, created_at, check_in_time, check_out_time) VALUES
(1, N'DS0101', N'KH002', N'Nguyễn Hoàng Nam', N'0903123456', NULL, N'CL02', N'CN01', N'2026-10-15', N'08:00 - 10:00', N'KG01', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Đang sử dụng', N'Khách quen tuần 3 buổi', N'2026-10-15 14:22:27.62', NULL, NULL),
(2, N'DS0102', N'KH001', N'Lê Bá Đạt', N'0912345678', NULL, N'CL01', N'CN01', N'2026-10-15', N'09:00 - 10:00', N'KG02', 120000.00, 120000.00, 36000.00, N'Tiền mặt', N'Hoàn thành', N'Đã thanh toán đủ tiền mặt', N'2026-10-15 14:22:27.62', NULL, NULL),
(3, N'DS0103', N'KH003', N'Phạm Gia Huy', N'0988777666', NULL, N'CL05', N'CN01', N'2026-10-15', N'18:00 - 20:00', N'KG03', 120000.00, 240000.00, 72000.00, N'Chuyển khoản VietQR', N'Đang sử dụng', N'Đặt sân VIP tối', N'2026-10-15 14:22:27.62', NULL, NULL),
(4, N'DS0104', N'KH004', N'Vũ Thị Mai Anh', N'0977555444', NULL, N'CL03', N'CN01', N'2026-10-15', N'19:00 - 20:00', N'KG03', 90000.00, 90000.00, 27000.00, N'VietQR Pro', N'Chờ xác nhận', N'Khách đặt qua web portal', N'2026-10-15 14:22:27.62', NULL, NULL),
(5, N'DS0105', N'KH005', N'Trần Thanh Hằng', N'0933444555', NULL, N'CL01', N'CN01', N'2026-10-16', N'17:00 - 18:00', N'KG03', 120000.00, 120000.00, 36000.00, N'Chuyển khoản VietQR', N'Đã cọc 30%', N'Chuyển khoản VietQR giữ chỗ 10p', N'2026-10-15 14:22:27.62', NULL, NULL),
(6, N'DS0106', N'KH006', N'Bùi Khánh Linh', N'0909111222', NULL, N'CL04', N'CN01', N'2026-10-16', N'20:00 - 22:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Đã xác nhận', N'Giao lưu công ty FPT', N'2026-10-15 14:22:27.62', NULL, NULL),
(7, N'DS0107', N'KH002', N'Đỗ Quang Vinh', N'0944333222', NULL, N'CL02', N'CN01', N'2026-10-14', N'15:00 - 16:00', N'KG02', 90000.00, 90000.00, 0.00, N'Tiền mặt', N'Đã hủy', N'Bận công tác đột xuất', N'2026-10-15 14:22:27.62', NULL, NULL),
(8, N'DS01_1001_1', N'KH001', N'Lê Bá Đạt', N'0912345678', NULL, N'CL01', N'CN01', N'2026-10-01', N'08:00 - 10:00', N'KG01', 120000.00, 240000.00, 72000.00, N'Tiền mặt', N'Hoàn thành', N'Khách quen tập sáng', N'2026-10-15 14:22:27.62', NULL, NULL),
(9, N'DS01_1001_2', N'KH003', N'Phạm Gia Huy', N'0988777666', NULL, N'CL03', N'CN01', N'2026-10-01', N'17:00 - 19:00', N'KG03', 90000.00, 180000.00, 54000.00, N'VietQR Pro', N'Hoàn thành', N'Đặt sân sau giờ làm', N'2026-10-15 14:22:27.62', NULL, NULL),
(10, N'DS01_1001_3', N'KH004', N'Vũ Thị Mai Anh', N'0977555444', NULL, N'CL07', N'CN01', N'2026-10-01', N'19:00 - 21:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Chuyển khoản VietQR', N'Hoàn thành', N'Đánh đôi nữ', N'2026-10-15 14:22:27.62', NULL, NULL),
(11, N'DS01_1002_1', N'KH002', N'Nguyễn Hoàng Nam', N'0903123456', NULL, N'CL02', N'CN01', N'2026-10-02', N'09:00 - 11:00', N'KG02', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Tập thể lực', N'2026-10-15 14:22:27.62', NULL, NULL),
(12, N'DS01_1002_2', N'KH005', N'Trần Thanh Hằng', N'0933444555', NULL, N'CL04', N'CN01', N'2026-10-02', N'18:00 - 20:00', N'KG03', 90000.00, 180000.00, 54000.00, N'VietQR Pro', N'Hoàn thành', N'CLB Thủ Đức', N'2026-10-15 14:22:27.62', NULL, NULL),
(13, N'DS01_1002_3', N'KH006', N'Bùi Khánh Linh', N'0909111222', NULL, N'CL08', N'CN01', N'2026-10-02', N'20:00 - 22:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Giao lưu', N'2026-10-15 14:22:27.62', NULL, NULL),
(14, N'DS01_1003_1', N'KH001', N'Lê Bá Đạt', N'0912345678', NULL, N'CL01', N'CN01', N'2026-10-03', N'15:00 - 17:00', N'KG02', 120000.00, 240000.00, 72000.00, N'Chuyển khoản VietQR', N'Hoàn thành', N'Tập đôi nam', N'2026-10-15 14:22:27.62', NULL, NULL),
(15, N'DS01_1003_2', N'KH003', N'Phạm Gia Huy', N'0988777666', NULL, N'CL05', N'CN01', N'2026-10-03', N'17:00 - 19:00', N'KG03', 90000.00, 180000.00, 54000.00, N'VietQR Pro', N'Hoàn thành', N'Khách vãng lai', N'2026-10-15 14:22:27.62', NULL, NULL),
(16, N'DS01_1003_3', N'KH004', N'Vũ Thị Mai Anh', N'0977555444', NULL, N'CL07', N'CN01', N'2026-10-03', N'19:00 - 21:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Đặt trực tiếp tại quầy', N'2026-10-15 14:22:27.62', NULL, NULL),
(17, N'DS01_1004_1', N'KH002', N'Nguyễn Hoàng Nam', N'0903123456', NULL, N'CL02', N'CN01', N'2026-10-04', N'08:00 - 10:00', N'KG01', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Khách cố định', N'2026-10-15 14:22:27.62', NULL, NULL),
(18, N'DS01_1004_2', N'KH005', N'Trần Thanh Hằng', N'0933444555', NULL, N'CL03', N'CN01', N'2026-10-04', N'10:00 - 12:00', N'KG02', 90000.00, 180000.00, 54000.00, N'VietQR Pro', N'Hoàn thành', N'Tập kỹ thuật', N'2026-10-15 14:22:27.62', NULL, NULL),
(19, N'DS01_1004_3', N'KH006', N'Bùi Khánh Linh', N'0909111222', NULL, N'CL08', N'CN01', N'2026-10-04', N'16:00 - 18:00', N'KG02', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Giao lưu cuối tuần', N'2026-10-15 14:22:27.62', NULL, NULL),
(20, N'DS01_1005_3', N'KH004', N'Vũ Thị Mai Anh', N'0977555444', NULL, N'CL06', N'CN01', N'2026-10-05', N'08:00 - 10:00', N'KG01', 120000.00, 240000.00, 72000.00, N'Tiền mặt', N'Hoàn thành', N'Trước khi sân bảo trì', N'2026-10-15 14:22:27.62', NULL, NULL),
(21, N'DS01_1005_1', N'KH001', N'Lê Bá Đạt', N'0912345678', NULL, N'CL01', N'CN01', N'2026-10-05', N'18:00 - 20:00', N'KG03', 120000.00, 240000.00, 72000.00, N'VietQR Pro', N'Hoàn thành', N'Tối thứ 2', N'2026-10-15 14:22:27.62', NULL, NULL),
(22, N'DS01_1005_2', N'KH003', N'Phạm Gia Huy', N'0988777666', NULL, N'CL04', N'CN01', N'2026-10-05', N'19:00 - 21:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Giao lưu nội bộ', N'2026-10-15 14:22:27.62', NULL, NULL),
(23, N'DS01_1006_1', N'KH002', N'Nguyễn Hoàng Nam', N'0903123456', NULL, N'CL02', N'CN01', N'2026-10-06', N'17:00 - 19:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Tập phát cầu', N'2026-10-15 14:22:27.62', NULL, NULL),
(24, N'DS01_1006_2', N'KH005', N'Trần Thanh Hằng', N'0933444555', NULL, N'CL05', N'CN01', N'2026-10-06', N'19:00 - 21:00', N'KG03', 90000.00, 180000.00, 54000.00, N'VietQR Pro', N'Hoàn thành', N'Khách hội viên', N'2026-10-15 14:22:27.62', NULL, NULL),
(25, N'DS01_1006_3', N'KH006', N'Bùi Khánh Linh', N'0909111222', NULL, N'CL07', N'CN01', N'2026-10-06', N'20:00 - 22:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Trận chung kết mini', N'2026-10-15 14:22:27.62', NULL, NULL),
(26, N'DS01_1007_1', N'KH001', N'Lê Bá Đạt', N'0912345678', NULL, N'CL01', N'CN01', N'2026-10-07', N'09:00 - 11:00', N'KG02', 120000.00, 240000.00, 72000.00, N'Chuyển khoản VietQR', N'Hoàn thành', N'Khách quen VIP', N'2026-10-15 14:22:27.62', NULL, NULL),
(27, N'DS01_1007_2', N'KH003', N'Phạm Gia Huy', N'0988777666', NULL, N'CL03', N'CN01', N'2026-10-07', N'18:00 - 20:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Thi đấu giao hữu', N'2026-10-15 14:22:27.62', NULL, NULL),
(28, N'DS01_1007_3', N'KH004', N'Vũ Thị Mai Anh', N'0977555444', NULL, N'CL08', N'CN01', N'2026-10-07', N'19:00 - 21:00', N'KG03', 90000.00, 180000.00, 54000.00, N'VietQR Pro', N'Hoàn thành', N'Đánh đôi nữ', N'2026-10-15 14:22:27.62', NULL, NULL),
(29, N'DS01_1008_3', N'KH006', N'Bùi Khánh Linh', N'0909111222', NULL, N'CL07', N'CN01', N'2026-10-08', N'17:00 - 19:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Khách vãng lai', N'2026-10-15 14:22:27.62', NULL, NULL),
(30, N'DS01_1008_1', N'KH002', N'Nguyễn Hoàng Nam', N'0903123456', NULL, N'CL02', N'CN01', N'2026-10-08', N'18:00 - 20:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Định kỳ thứ 5', N'2026-10-15 14:22:27.62', NULL, NULL),
(31, N'DS01_1008_2', N'KH005', N'Trần Thanh Hằng', N'0933444555', NULL, N'CL04', N'CN01', N'2026-10-08', N'20:00 - 22:00', N'KG03', 90000.00, 180000.00, 54000.00, N'VietQR Pro', N'Hoàn thành', N'CLB Ngân Hàng', N'2026-10-15 14:22:27.62', NULL, NULL),
(32, N'DS01_1009_1', N'KH001', N'Lê Bá Đạt', N'0912345678', NULL, N'CL01', N'CN01', N'2026-10-09', N'17:00 - 19:00', N'KG03', 120000.00, 240000.00, 72000.00, N'Chuyển khoản VietQR', N'Hoàn thành', N'VIP Thứ 6', N'2026-10-15 14:22:27.62', NULL, NULL),
(33, N'DS01_1009_3', N'KH004', N'Vũ Thị Mai Anh', N'0977555444', NULL, N'CL08', N'CN01', N'2026-10-09', N'18:00 - 20:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Khách nhóm', N'2026-10-15 14:22:27.62', NULL, NULL),
(34, N'DS01_1009_2', N'KH003', N'Phạm Gia Huy', N'0988777666', NULL, N'CL03', N'CN01', N'2026-10-09', N'19:00 - 21:00', N'KG03', 90000.00, 180000.00, 54000.00, N'VietQR Pro', N'Hoàn thành', N'Giao lưu cuối tuần', N'2026-10-15 14:22:27.62', NULL, NULL),
(35, N'DS01_1012_1', N'KH002', N'Nguyễn Hoàng Nam', N'0903123456', NULL, N'CL02', N'CN01', N'2026-10-12', N'18:00 - 20:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Tối thứ 2', N'2026-10-15 14:22:27.62', NULL, NULL),
(36, N'DS01_1012_2', N'KH004', N'Vũ Thị Mai Anh', N'0977555444', NULL, N'CL07', N'CN01', N'2026-10-12', N'19:00 - 21:00', N'KG03', 90000.00, 180000.00, 54000.00, N'VietQR Pro', N'Hoàn thành', N'Tập đánh đôi', N'2026-10-15 14:22:27.62', NULL, NULL),
(37, N'DS01_1012_3', N'KH006', N'Bùi Khánh Linh', N'0909111222', NULL, N'CL08', N'CN01', N'2026-10-12', N'20:00 - 22:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Hoàn thành', N'Giao lưu câu lạc bộ', N'2026-10-15 14:22:27.62', NULL, NULL),
(38, N'DS01_1013_1', N'KH002', N'Nguyễn Hoàng Nam', N'0903123456', NULL, N'CL02', N'CN01', N'2026-10-13', N'17:00 - 19:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Đang sử dụng', N'Hôm nay - Ca chiều', N'2026-10-15 14:22:27.62', NULL, NULL),
(39, N'DS01_1013_3', N'KH005', N'Trần Thanh Hằng', N'0933444555', NULL, N'CL04', N'CN01', N'2026-10-13', N'18:00 - 20:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Đã xác nhận', N'Hôm nay - CLB Nữ', N'2026-10-15 14:22:27.62', NULL, NULL),
(40, N'DS01_1013_2', N'KH003', N'Phạm Gia Huy', N'0988777666', NULL, N'CL03', N'CN01', N'2026-10-13', N'19:00 - 21:00', N'KG03', 90000.00, 180000.00, 54000.00, N'VietQR Pro', N'Đã xác nhận', N'Hôm nay - Ca tối', N'2026-10-15 14:22:27.62', NULL, NULL),
(41, N'DS01_1013_4', N'KH004', N'Vũ Thị Mai Anh', N'0977555444', NULL, N'CL07', N'CN01', N'2026-10-13', N'20:00 - 22:00', N'KG03', 90000.00, 180000.00, 54000.00, N'VietQR Pro', N'Đã xác nhận', N'Hôm nay - Đánh đôi', N'2026-10-15 14:22:27.62', NULL, NULL),
(42, N'DS0201', N'KH001', N'Lê Bá Đạt', N'0912345678', NULL, N'BT01', N'CN02', N'2026-10-15', N'18:00 - 20:00', N'KG03', 120000.00, 240000.00, 72000.00, N'Chuyển khoản VietQR', N'Đang sử dụng', N'Sân VIP Bình Thạnh', N'2026-10-15 14:22:27.62', NULL, NULL),
(43, N'DS0202', N'KH003', N'Phạm Gia Huy', N'0988777666', NULL, N'BT04', N'CN02', N'2026-10-15', N'19:00 - 21:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Đang sử dụng', N'Tập luyện đôi nam', N'2026-10-15 14:22:27.62', NULL, NULL),
(44, N'DS0301', N'KH004', N'Vũ Thị Mai Anh', N'0977555444', NULL, N'Q902', N'CN03', N'2026-10-15', N'17:00 - 19:00', N'KG03', 90000.00, 180000.00, 54000.00, N'VietQR Pro', N'Đang sử dụng', N'Nhóm bạn sinh viên', N'2026-10-15 14:22:27.62', NULL, NULL),
(45, N'DS0401', N'KH005', N'Trần Thanh Hằng', N'0933444555', NULL, N'GV02', N'CN04', N'2026-10-15', N'18:00 - 20:00', N'KG03', 90000.00, 180000.00, 54000.00, N'Tiền mặt', N'Đang sử dụng', N'Khách vãng lai chi nhánh GV', N'2026-10-15 14:22:27.62', NULL, NULL),
(46, N'DS852888', N'KH001', N'Nguyễn Văn An', N'0912345678', N'dat.le@gmail.com', N'CL01', N'CN01', N'2026-10-15', N'06:00 - 07:00', NULL, 120000.00, 120000.00, 36000.00, N'Chuyển khoản VietQR', N'Chờ cọc 30%', N'Đặt qua cổng trực tuyến', N'2026-10-15 14:32:41.947', NULL, NULL),
(47, N'DS918320', NULL, N'Nguyễn Phước Thọ', N'0901111222', NULL, N'Q901', N'CN03', N'2026-10-18', N'07:00 - 08:00', NULL, 120000.00, 120000.00, 36000.00, N'VietQR Pro', N'Đã cọc 30%', N'Đặt qua Lưới giờ Sân trực tuyến', N'2026-10-18 22:20:17.083', NULL, NULL),
(50, N'DS_CANCEL_511', NULL, N'Khach Huy', N'0908888999', NULL, N'CL02', N'CN01', N'2027-03-18', N'18:00 - 19:00', NULL, 100000.00, 100000.00, 30000.00, N'Tiền mặt', N'Đã hủy', N' [Hủy: Ly do ca nhan | Hoàn cọc: 30,000đ]', N'2026-10-19 13:59:20.513', NULL, NULL);
SET IDENTITY_INSERT bookings OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: booking_services (4 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT booking_services ON;
INSERT INTO booking_services (id, booking_code, product_name, quantity, unit_price, total_amount) VALUES
(1, N'DS0101', N'Nước Revive Chanh muối', 2, 15000.00, 30000.00),
(2, N'DS0102', N'Thuê Vợt Yonex Astrox 77', 1, 25000.00, 25000.00),
(3, N'DS0103', N'Nước điện giải Pocari Sweat', 2, 18000.00, 36000.00),
(4, N'DS0105', N'Ống Cầu Hải Yến Đỏ', 1, 180000.00, 180000.00);
SET IDENTITY_INSERT booking_services OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: invoices (3 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT invoices ON;
INSERT INTO invoices (id, invoice_code, booking_code, staff_code, invoice_date, court_fee, equipment_fee, product_fee, discount_amount, deposit_paid, total_payment, payment_method, status) VALUES
(1, N'HD0101', N'DS0102', N'NS03', N'2026-10-15 13:37:27.62', 120000.00, 25000.00, 30000.00, 15000.00, 36000.00, 124000.00, N'Tiền mặt', N'Đã thanh toán'),
(2, N'HD0102', N'DS0101', N'NS03', N'2026-10-15 12:22:27.62', 180000.00, 0.00, 45000.00, 18000.00, 54000.00, 153000.00, N'Chuyển khoản VietQR', N'Đã thanh toán'),
(3, N'HD0103', N'DS0107', N'NS03', N'2026-10-14 14:22:27.62', 90000.00, 25000.00, 15000.00, 0.00, 0.00, 130000.00, N'Tiền mặt', N'Đã thanh toán');
SET IDENTITY_INSERT invoices OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: invoice_equipment_details (2 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT invoice_equipment_details ON;
INSERT INTO invoice_equipment_details (id, invoice_code, equipment_code, quantity, unit_price, total_amount) VALUES
(1, N'HD0101', N'DC01', 1, 25000.00, 25000.00),
(2, N'HD0103', N'DC01', 1, 25000.00, 25000.00);
SET IDENTITY_INSERT invoice_equipment_details OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: invoice_product_details (4 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT invoice_product_details ON;
INSERT INTO invoice_product_details (id, invoice_code, product_code, quantity, unit_price, total_amount) VALUES
(1, N'HD0101', N'P01', 2, 15000.00, 30000.00),
(2, N'HD0102', N'P01', 1, 15000.00, 15000.00),
(3, N'HD0102', N'P02', 1, 18000.00, 18000.00),
(4, N'HD0102', N'P07', 1, 12000.00, 12000.00);
SET IDENTITY_INSERT invoice_product_details OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: tournaments (3 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT tournaments ON;
INSERT INTO tournaments (id, tournament_code, branch_code, tournament_name, start_date, end_date, rules, category, max_participants, current_participants, entry_fee, total_prize, status) VALUES
(1, N'GD01', N'CN01', N'Giải Cầu Lông Đôi Nam Nữ Mùa Thu UTE Open 2026', N'2026-10-10 08:00:00.0', N'2026-10-12 18:00:00.0', N'Thi đấu theo thể thức loại trực tiếp 3 hiệp 21 điểm theo luật BWF. Mỗi VĐV chỉ được đăng ký 1 nội dung.', N'Đôi nam nữ', 32, 27, 300000.00, 20000000.00, N'Đang mở đăng ký'),
(2, N'GD02', N'CN01', N'Giải Cầu Lông Doanh Nghiệp & Sinh Viên Cúp Thủ Đức 2026', N'2026-10-25 08:00:00.0', N'2026-10-26 17:00:00.0', N'Giải đấu phong trào kết nối các doanh nghiệp và cựu sinh viên đam mê cầu lông.', N'Tổng hợp nhiều nội dung', 24, 8, 250000.00, 15000000.00, N'Sắp mở đăng ký'),
(3, N'GD03', N'CN02', N'Giải Đơn Nam Bình Thạnh Mở Rộng 2026', N'2026-11-05 08:00:00.0', N'2026-11-06 18:00:00.0', N'Giải đơn nam các CLB phong trào khu vực Bình Thạnh - Phú Nhuận.', N'Đơn nam', 16, 0, 200000.00, 10000000.00, N'Sắp mở đăng ký');
SET IDENTITY_INSERT tournaments OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: tournament_registrations (5 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT tournament_registrations ON;
INSERT INTO tournament_registrations (id, tournament_code, customer_code, team_name, category, phone, email, registration_date, status) VALUES
(1, N'GD01', N'KH001', N'Cặp Đôi Đạt & Trang (CLB Thủ Đức)', N'Đôi nam nữ', N'0912345678', NULL, N'2026-10-15 14:22:27.62', N'Đã xác nhận'),
(2, N'GD01', N'KH002', N'Cặp Đôi Nam & Thảo (ĐH Sư Phạm Kỹ Thuật)', N'Đôi nam nữ', N'0903123456', NULL, N'2026-10-15 14:22:27.62', N'Đã xác nhận'),
(3, N'GD01', N'KH003', N'Cặp Đôi Huy & Hằng (Công ty TMA)', N'Đôi nam nữ', N'0988777666', NULL, N'2026-10-15 14:22:27.62', N'Đã xác nhận'),
(4, N'GD01', N'KH004', N'Cặp Đôi Mai Anh & Đức (CLB Yonex)', N'Đôi nam nữ', N'0977555444', NULL, N'2026-10-15 14:22:27.62', N'Đã xác nhận'),
(5, N'GD01', N'KH005', N'Cặp Đôi Tuấn & Lan (CLB Bình Thạnh)', N'Đôi nam nữ', N'0933221100', NULL, N'2026-10-15 14:22:27.62', N'Đã xác nhận');
SET IDENTITY_INSERT tournament_registrations OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: tournament_matches (3 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT tournament_matches ON;
INSERT INTO tournament_matches (id, match_code, tournament_code, court_code, player1_name, player2_name, match_time, result, status) VALUES
(1, N'TR01', N'GD01', N'CL01', N'Đạt & Trang', N'Nam & Tuấn', N'2026-10-10 08:30:00.0', N'2 - 1 (21-18, 19-21, 21-16)', N'Đã kết thúc'),
(2, N'TR02', N'GD01', N'CL02', N'Huy & Hằng', N'Mai Anh & Đức', N'2026-10-10 09:30:00.0', N'0 - 2 (15-21, 18-21)', N'Đã kết thúc'),
(3, N'TR03', N'GD01', N'CL01', N'Đạt & Trang', N'Mai Anh & Đức', N'2026-10-11 15:00:00.0', N'Chưa diễn ra', N'Sắp diễn ra');
SET IDENTITY_INSERT tournament_matches OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: payments (8 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT payments ON;
INSERT INTO payments (id, payment_code, booking_code, amount, method, transaction_type, transaction_ref, payment_time, status) VALUES
(1, N'TT0101', N'DS0101', 54000.00, N'Tiền mặt', N'Đặt cọc 30%', N'CASH-20260919-01', N'2026-10-15 14:22:27.623', N'Thành công'),
(2, N'TT0102', N'DS0102', 36000.00, N'Tiền mặt', N'Đặt cọc 30%', N'CASH-20260919-02', N'2026-10-15 14:22:27.623', N'Thành công'),
(3, N'TT0103', N'DS0103', 72000.00, N'VietQR Pro', N'Đặt cọc 30%', N'MB-998822110034', N'2026-10-15 14:22:27.623', N'Thành công'),
(4, N'TT0104', N'DS0104', 27000.00, N'VietQR Pro', N'Đặt cọc 30%', N'VCB-776655441122', N'2026-10-15 14:22:27.623', N'Đang xử lý'),
(5, N'TT0105', N'DS0105', 36000.00, N'VietQR Pro', N'Đặt cọc 30%', N'TCB-334455667788', N'2026-10-15 14:22:27.623', N'Thành công'),
(6, N'TT0106', N'DS0201', 72000.00, N'VietQR Pro', N'Đặt cọc 30%', N'MB-556677889900', N'2026-10-15 14:22:27.623', N'Thành công'),
(7, N'PAY706008', N'DS918320', 36000.00, N'VietQR Pro', N'Đặt cọc 30%', N'VQR-825965', N'2026-10-18 22:20:25.973', N'Thành công'),
(8, N'HT_CANCEL_511', N'DS_CANCEL_511', 30000.00, N'Chuyển khoản VietQR', N'Hoàn tiền cọc', N'REFUND-100%-DS_CANCEL_511', N'2026-10-19 13:59:20.537', N'Thành công');
SET IDENTITY_INSERT payments OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: work_shifts (19 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT work_shifts ON;
INSERT INTO work_shifts (id, shift_code, staff_code, branch_code, day_of_week, shift_type, duty, icon, shift_date, start_time, end_time, status) VALUES
(1, N'CA01', N'NS03', N'CN01', NULL, N'Ca Sáng', NULL, N'POS', N'2026-10-15', N'06:00', N'14:00', N'Đang trực'),
(2, N'CA02', N'NS04', N'CN01', NULL, N'Ca Chiều', NULL, N'POS', N'2026-10-15', N'14:00', N'22:00', N'Đã phân ca'),
(3, N'CA03', N'NS03', N'CN01', NULL, N'Ca Sáng', NULL, N'POS', N'2026-10-16', N'06:00', N'14:00', N'Đã phân ca'),
(4, N'CA04', N'NS04', N'CN01', NULL, N'Ca Chiều', NULL, N'POS', N'2026-10-16', N'14:00', N'22:00', N'Đã phân ca'),
(5, N'CA05', N'NS08', N'CN01', NULL, N'Ca Tối', NULL, N'POS', N'2026-10-15', N'17:00', N'23:00', N'Đang trực'),
(6, N'BG719949', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'14:03', N'Đã kết ca'),
(7, N'BG856822', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'14:06', N'Đã kết ca'),
(8, N'BG1064680', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'14:09', N'Đã kết ca'),
(9, N'BG936688', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'14:10', N'Đã kết ca'),
(10, N'BG614739', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'14:10', N'Đã kết ca'),
(11, N'BG753582', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'20:39', N'Đã kết ca'),
(12, N'BG388576', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'20:40', N'Đã kết ca'),
(13, N'BG451667', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:04', N'Đã kết ca'),
(14, N'BG686871', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:05', N'Đã kết ca'),
(15, N'BG649265', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:06', N'Đã kết ca'),
(16, N'BG474839', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:09', N'Đã kết ca'),
(17, N'BG927292', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:15', N'Đã kết ca'),
(18, N'BG675613', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:17', N'Đã kết ca'),
(19, N'BG642026', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:29', N'Đã kết ca');
SET IDENTITY_INSERT work_shifts OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: stock_receipts (3 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT stock_receipts ON;
INSERT INTO stock_receipts (id, receipt_code, branch_code, staff_code, receipt_date, supplier_name, total_amount, notes) VALUES
(1, N'PN0101', N'CN01', N'NS02', N'2026-10-10 14:22:27.623', N'Công ty TNHH Thể Thao Yonex Việt Nam', 14500000.00, N'Nhập bổ sung vợt Yonex Astrox và 50 ống cầu Hải Yến'),
(2, N'PN0102', N'CN01', N'NS02', N'2026-10-13 14:22:27.623', N'NPP Dụng Cụ Cầu Lông Đại Phát', 4200000.00, N'Nhập nước bù khoáng Revive và Pocari Sweat đợt cuối tháng'),
(22, N'PNK904401', N'CN01', N'NS02', N'2026-10-19 21:04:12.507', N'Nha Cung Cap Yonex Viet Nam', 175000.00, N'Nhap kho dau tuan test SQL - Nước bù khoáng Revive Chanh muối 500ml x25');
SET IDENTITY_INSERT stock_receipts OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: stock_equipment_details (2 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT stock_equipment_details ON;
INSERT INTO stock_equipment_details (id, receipt_code, equipment_code, quantity, import_price, total_amount) VALUES
(1, N'PN0101', N'DC01', 10, 1800000.00, 18000000.00),
(2, N'PN0101', N'DC05', 30, 145000.00, 4350000.00);
SET IDENTITY_INSERT stock_equipment_details OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: stock_product_details (4 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT stock_product_details ON;
INSERT INTO stock_product_details (id, receipt_code, product_code, quantity, import_price, total_amount) VALUES
(1, N'PN0102', N'P01', 200, 8000.00, 1600000.00),
(2, N'PN0102', N'P02', 150, 11000.00, 1650000.00),
(3, N'PN0102', N'P03', 190, 5000.00, 950000.00),
(9, N'PNK904401', N'P01', 25, 7000.00, 175000.00);
SET IDENTITY_INSERT stock_product_details OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: promotions (3 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT promotions ON;
INSERT INTO promotions (id, promo_code, promo_name, discount_type, discount_value, start_date, end_date, scope, status) VALUES
(1, N'KM01', N'Ưu Đãi Giờ Vàng Thứ Tư Hàng Tuần', N'Phần trăm (%)', 15.00, N'2026-09-01', N'2026-12-31', N'Toàn chuỗi', N'Đang áp dụng'),
(2, N'KM02', N'Đặc Quyền Hội Viên Kim Cương & Vàng', N'Phần trăm (%)', 15.00, N'2026-01-01', N'2026-12-31', N'Toàn chuỗi', N'Đang áp dụng'),
(3, N'KM03', N'Khai Trương Cụm Sân Gò Vấp - Tặng 1 Giờ Đánh', N'Số tiền (VNĐ)', 90000.00, N'2026-10-01', N'2026-10-31', N'Chi nhánh Gò Vấp', N'Đang áp dụng');
SET IDENTITY_INSERT promotions OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: system_settings (6 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT system_settings ON;
INSERT INTO system_settings (id, setting_code, setting_name, setting_value, description, updated_at) VALUES
(1, N'CFG_DEPOSIT_RATE', N'Tỷ lệ đặt cọc tối thiểu (%)', N'30', N'Khách hàng phải thanh toán cọc ít nhất 30% để giữ sân hợp lệ (QĐ-KH02)', N'2026-10-15 14:22:27.623'),
(2, N'CFG_HOLDING_MINUTES', N'Thời gian tạm giữ chỗ chờ cọc (Phút)', N'10', N'Sau 10 phút nếu không có giao dịch cọc, hệ thống tự động giải phóng ô sân (QĐ-KH02)', N'2026-10-15 14:22:27.623'),
(3, N'CFG_CANCEL_HOURS', N'Thời hạn hủy sân tối thiểu để được hoàn cọc (Giờ)', N'4', N'Hủy trước giờ chơi tối thiểu 4 tiếng để được nhận lại 100% tiền cọc (QĐ-KH03)', N'2026-10-15 14:22:27.623'),
(4, N'CFG_ADVANCE_DAYS', N'Số ngày tối đa được phép đặt trước', N'30', N'Giới hạn đặt sân trong vòng 30 ngày tới', N'2026-10-15 14:22:27.623'),
(5, N'CFG_VNPAY_TMN', N'Mã đối tác VNPay (TMN Code)', N'UTESPORT2026', N'Tích hợp cổng thanh toán VNPay QR Sandbox/Production', N'2026-10-15 14:22:27.623'),
(6, N'CFG_MOMO_PARTNER', N'Partner Code MoMo Business', N'MOMO_UTE_BADMINTON', N'Tích hợp cổng Ví điện tử MoMo Business', N'2026-10-15 14:22:27.623');
SET IDENTITY_INSERT system_settings OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: audit_logs (147 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT audit_logs ON;
INSERT INTO audit_logs (log_id, username, action_type, action_time, details, ip_address, module, status, user_code) VALUES
(1, N'nam.vh', N'Phê duyệt bảng giá giờ vàng Sân VIP Yonex', N'2026-10-15 14:07:27.623', N'Phê duyệt giá 120.000đ cho khung giờ 17:00-22:00', N'192.168.1.105', N'Phân hệ Giám đốc', N'Thành công', NULL),
(2, N'mai.lt', N'Xác nhận nhận sân (Check-in) phiếu BK-1082', N'2026-10-15 13:34:27.623', N'Check-in sân CL02 cho khách Nguyễn Hoàng Nam', N'192.168.1.112', N'Phân hệ POS Bán Hàng', N'Thành công', NULL),
(3, N'admin', N'Cập nhật ma trận phân quyền chi nhánh', N'2026-10-15 12:22:27.623', N'Phân quyền quản lý dữ liệu chi nhánh CN01 cho tài khoản bao.tran', N'192.168.1.200', N'Phân hệ Quản trị', N'Thành công', NULL),
(4, N'hacker_guest', N'Đăng nhập thất bại quá 5 lần liên tiếp', N'2026-10-15 10:22:27.623', N'Cảnh báo bảo mật: Brute-force thử mật khẩu tài khoản admin', N'14.232.18.99', N'Cổng bảo mật xác thực', N'Cảnh báo', NULL),
(5, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 13:59:20.43', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Bảo trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(6, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 13:59:20.47', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Bảo trì] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(10, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 13:59:20.987', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(12, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 13:59:21.0', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(15, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:00:22.553', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Bảo trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(16, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:00:22.557', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Bảo trì] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(21, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:00:22.613', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(23, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:00:22.617', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(26, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:03:12.083', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(27, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:03:12.16', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(28, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:03:12.197', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(29, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:06:52.797', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Trống] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(30, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:06:52.87', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(31, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:06:52.903', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(32, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:09:35.4', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Trống] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(33, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:09:35.473', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(34, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:09:35.5', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(35, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.163', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Trống] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(36, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.233', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(37, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.263', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(38, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.383', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Bảo trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(39, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.387', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Bảo trì] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(44, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.43', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(46, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.433', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(49, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.653', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Trống] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(50, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.71', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(51, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.733', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(52, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.843', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Bảo trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(53, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.847', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Bảo trì] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(58, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.903', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(60, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.907', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(63, N'director', N'Đăng nhập hệ thống', N'2026-10-19 20:39:05.65', N'Xác thực tài khoản [director] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(64, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 20:39:05.737', N'Xác thực tài khoản [manager] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(65, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 20:39:05.817', N'Xác thực tài khoản [pos] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(66, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 20:39:05.893', N'Xác thực tài khoản [admin] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(67, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 20:39:05.97', N'Xác thực tài khoản [customer] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(68, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:06.223', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Trống] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(69, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:06.387', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(70, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:06.533', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(71, N'manager', N'Đăng nhập thất bại', N'2026-10-19 20:39:06.667', N'Đăng nhập không thành công: Tài khoản hoặc mật khẩu không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(72, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:06.777', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Bảo trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(73, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:06.79', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Bảo trì] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(78, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:06.973', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(80, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:07.003', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(83, N'director', N'Đăng nhập hệ thống', N'2026-10-19 20:40:31.843', N'Xác thực tài khoản [director] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(84, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 20:40:31.92', N'Xác thực tài khoản [manager] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(85, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 20:40:31.997', N'Xác thực tài khoản [pos] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(86, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 20:40:32.077', N'Xác thực tài khoản [admin] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(87, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 20:40:32.157', N'Xác thực tài khoản [customer] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(88, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:32.4', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Trống] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(89, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:32.53', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(90, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:32.63', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(91, N'manager', N'Đăng nhập thất bại', N'2026-10-19 20:40:32.74', N'Đăng nhập không thành công: Tài khoản hoặc mật khẩu không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(92, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:32.813', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Bảo trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(93, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:32.82', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Bảo trì] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(98, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:33.0', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(100, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:33.01', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(103, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:04:12.08', N'Xác thực tài khoản [director] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(104, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:04:12.163', N'Xác thực tài khoản [manager] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(105, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:04:12.237', N'Xác thực tài khoản [pos] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(106, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:04:12.307', N'Xác thực tài khoản [admin] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(107, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:04:12.397', N'Xác thực tài khoản [customer] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(108, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:12.627', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Trống] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(109, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:12.72', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(110, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:12.78', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(111, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:04:12.887', N'Đăng nhập không thành công: Tài khoản hoặc mật khẩu không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(112, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:12.957', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Bảo trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(113, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:12.963', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Bảo trì] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(118, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:13.09', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(120, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:13.1', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(123, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:05:44.297', N'Xác thực tài khoản [director] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(124, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:05:44.37', N'Xác thực tài khoản [manager] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(125, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:05:44.447', N'Xác thực tài khoản [pos] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(126, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:05:44.52', N'Xác thực tài khoản [admin] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(127, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:05:44.593', N'Xác thực tài khoản [customer] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(128, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:05:44.917', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Trống] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(129, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:05:45.043', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(130, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:05:45.137', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(131, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:05:45.277', N'Đăng nhập không thành công: Tài khoản hoặc mật khẩu không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(132, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:06:18.693', N'Xác thực tài khoản [director] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(133, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:06:18.76', N'Xác thực tài khoản [manager] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(134, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:06:18.82', N'Xác thực tài khoản [pos] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(135, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:06:18.89', N'Xác thực tài khoản [admin] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(136, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:06:18.95', N'Xác thực tài khoản [customer] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(137, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.167', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Trống] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(138, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.253', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(139, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.307', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(140, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:06:19.41', N'Đăng nhập không thành công: Tài khoản hoặc mật khẩu không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(141, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.493', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Bảo trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(142, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.5', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Bảo trì] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(147, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.64', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(149, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.65', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(152, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:09:38.11', N'Xác thực tài khoản [director] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(153, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:09:38.173', N'Xác thực tài khoản [manager] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(154, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:09:38.237', N'Xác thực tài khoản [pos] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(155, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:09:38.307', N'Xác thực tài khoản [admin] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(156, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:09:38.367', N'Xác thực tài khoản [customer] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(157, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.527', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Trống] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(158, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.587', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(159, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.627', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(160, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:09:38.72', N'Đăng nhập không thành công: Tài khoản hoặc mật khẩu không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(161, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.777', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Bảo trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(162, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.78', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Bảo trì] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(167, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.89', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(169, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.9', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(172, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:15:34.103', N'Xác thực tài khoản [director] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(173, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:15:34.17', N'Xác thực tài khoản [manager] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(174, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:15:34.233', N'Xác thực tài khoản [pos] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(175, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:15:34.297', N'Xác thực tài khoản [admin] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(176, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:15:34.357', N'Xác thực tài khoản [customer] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(177, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:15:34.557', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Trống] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(178, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:15:34.64', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(179, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:15:34.683', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(180, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:15:34.767', N'Đăng nhập không thành công: Tài khoản hoặc mật khẩu không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(181, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-19 21:16:10.843', N'Tải lên tệp sao lưu test_unit_backup.sql (Dung lượng: 0.03 KB) từ máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(182, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-19 21:16:42.26', N'Tải lên tệp sao lưu test_unit_backup.sql (Dung lượng: 0.03 KB) từ máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(183, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-19 21:16:42.41', N'Tạo bản sao lưu backup_ute_sport_initial.sql (Dung lượng: 124.55 KB, 330 bản ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(184, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-19 21:17:44.557', N'Tải lên tệp sao lưu test_unit_backup.sql (Dung lượng: 0.03 KB) từ máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(185, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-19 21:17:44.69', N'Tạo bản sao lưu backup_ute_sport_initial.sql (Dung lượng: 125.03 KB, 332 bản ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(186, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:17:48.0', N'Xác thực tài khoản [director] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(187, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:17:48.06', N'Xác thực tài khoản [manager] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(188, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:17:48.12', N'Xác thực tài khoản [pos] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(189, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:17:48.18', N'Xác thực tài khoản [admin] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(190, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:17:48.24', N'Xác thực tài khoản [customer] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(191, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.387', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Trống] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(192, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.45', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(193, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.487', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(194, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:17:48.567', N'Đăng nhập không thành công: Tài khoản hoặc mật khẩu không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(195, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.617', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Bảo trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(196, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.62', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Bảo trì] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(201, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.717', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Trống] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(203, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.727', N'Sân [CL01 - Sân cầu lông 1 (VIP Yonex)] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(206, N'user_reset_1792420163365', N'Đăng nhập hệ thống', N'2026-10-19 21:29:23.593', N'Xác thực tài khoản [user_reset_1792420163365] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(207, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:29:23.867', N'Xác thực tài khoản [director] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(208, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:29:23.937', N'Xác thực tài khoản [manager] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(209, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:29:24.0', N'Xác thực tài khoản [pos] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(210, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:29:24.063', N'Xác thực tài khoản [admin] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(211, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:29:24.14', N'Xác thực tài khoản [customer] thành công qua cổng bảo mật', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(212, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:29:24.363', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Trống] sang [Đã đặt trước]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(213, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:29:24.47', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đã đặt trước] sang [Đang sử dụng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(214, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:29:24.54', N'Sân [CL02 - Sân cầu lông 2] chuyển từ [Đang sử dụng] sang [Trống]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(215, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:29:24.653', N'Đăng nhập không thành công: Tài khoản hoặc mật khẩu không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(216, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-19 21:29:47.483', N'Tải lên tệp sao lưu test_unit_backup.sql (Dung lượng: 0.03 KB) từ máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL);
SET IDENTITY_INSERT audit_logs OFF;
GO

EXEC sp_MSforeachtable "ALTER TABLE ? WITH CHECK CHECK CONSTRAINT all";
GO

-- 6. TẠO CÁC CHỈ MỤC INDEX TỐI ƯU HIỆU NĂNG TRUY VẤN (BUG-MED-25)
IF NOT EXISTS (SELECT name FROM sys.indexes WHERE name = N'idx_bookings_date_court')
    CREATE INDEX idx_bookings_date_court ON bookings(booking_date, court_code);

IF NOT EXISTS (SELECT name FROM sys.indexes WHERE name = N'idx_bookings_customer_phone')
    CREATE INDEX idx_bookings_customer_phone ON bookings(customer_phone);

IF NOT EXISTS (SELECT name FROM sys.indexes WHERE name = N'idx_courts_branch_status')
    CREATE INDEX idx_courts_branch_status ON courts(branch_code, status);

IF NOT EXISTS (SELECT name FROM sys.indexes WHERE name = N'idx_invoices_booking_date')
    CREATE INDEX idx_invoices_booking_date ON invoices(booking_code, invoice_date);
GO

PRINT N'>>> ĐÃ KHỞI TẠO TOÀN DIỆN 25 BẢNG DỮ LIỆU, 25 VIEW TIẾNG VIỆT VÀ CHÈN SEED DATA HOÀN TẤT THÀNH CÔNG!';
GO

-- =====================================================================
-- 7. HÀM NGƯỜI DÙNG ĐỊNH NGHĨA (USER-DEFINED FUNCTIONS - UDF)
-- =====================================================================

-- 7.1 Hàm kiểm tra tính khả dụng của sân trong khung giờ cụ thể
CREATE FUNCTION dbo.fn_CheckSlotAvailability
(
    @court_code VARCHAR(20),
    @booking_date DATE,
    @time_slot VARCHAR(50)
)
RETURNS BIT
AS
BEGIN
    IF EXISTS (SELECT 1 FROM courts WHERE court_code = @court_code AND status = N'Bảo trì')
    BEGIN
        RETURN 0;
    END

    IF EXISTS (
        SELECT 1 
        FROM bookings 
        WHERE court_code = @court_code 
          AND booking_date = @booking_date 
          AND status <> N'Đã hủy'
          AND (
              time_slot = @time_slot
              OR CHARINDEX(@time_slot, time_slot) > 0
              OR CHARINDEX(time_slot, @time_slot) > 0
          )
    )
    BEGIN
        RETURN 0;
    END

    RETURN 1;
END;
GO

-- 7.2 Hàm tính tiền thuê sân theo thời lượng khung giờ và chiết khấu khuyến mãi
CREATE FUNCTION dbo.fn_CalculateBookingPrice
(
    @court_code VARCHAR(20),
    @time_slot VARCHAR(50),
    @promo_code VARCHAR(20) = NULL
)
RETURNS DECIMAL(18,2)
AS
BEGIN
    DECLARE @hourly_rate DECIMAL(18,2) = 90000;
    DECLARE @total_price DECIMAL(18,2) = 0;
    DECLARE @duration INT = 1;

    SELECT @hourly_rate = ISNULL(hourly_rate, 90000)
    FROM courts
    WHERE court_code = @court_code;

    IF CHARINDEX('-', @time_slot) > 0
    BEGIN
        DECLARE @start_str VARCHAR(10) = LTRIM(RTRIM(SUBSTRING(@time_slot, 1, CHARINDEX('-', @time_slot) - 1)));
        DECLARE @end_str VARCHAR(10) = LTRIM(RTRIM(SUBSTRING(@time_slot, CHARINDEX('-', @time_slot) + 1, LEN(@time_slot))));
        DECLARE @start_h INT = TRY_CAST(SUBSTRING(@start_str, 1, 2) AS INT);
        DECLARE @end_h INT = TRY_CAST(SUBSTRING(@end_str, 1, 2) AS INT);
        
        IF @start_h IS NOT NULL AND @end_h IS NOT NULL AND @end_h > @start_h
            SET @duration = @end_h - @start_h;
    END
    ELSE IF CHARINDEX(',', @time_slot) > 0
    BEGIN
        SET @duration = LEN(@time_slot) - LEN(REPLACE(@time_slot, ',', '')) + 1;
    END

    SET @total_price = @hourly_rate * @duration;

    IF @promo_code IS NOT NULL AND LEN(@promo_code) > 0
    BEGIN
        DECLARE @disc_type NVARCHAR(30);
        DECLARE @disc_val DECIMAL(18,2);
        
        SELECT @disc_type = discount_type, @disc_val = discount_value
        FROM promotions
        WHERE promo_code = @promo_code 
          AND status = N'Đang áp dụng'
          AND CAST(GETDATE() AS DATE) BETWEEN start_date AND end_date;

        IF @disc_type IS NOT NULL
        BEGIN
            IF @disc_type LIKE N'%Phần trăm%' OR @disc_type LIKE '%Percent%'
                SET @total_price = @total_price - (@total_price * (@disc_val / 100.0));
            ELSE
                SET @total_price = @total_price - @disc_val;

            IF @total_price < 0 SET @total_price = 0;
        END
    END

    RETURN @total_price;
END;
GO

-- 7.3 Hàm lấy tỷ lệ giảm giá theo hạng thẻ thành viên
CREATE FUNCTION dbo.fn_GetCustomerTierDiscount
(
    @customer_code VARCHAR(20)
)
RETURNS DECIMAL(5,2)
AS
BEGIN
    DECLARE @tier NVARCHAR(30);
    DECLARE @discount_percent DECIMAL(5,2) = 0.0;

    SELECT @tier = membership_tier
    FROM customers
    WHERE customer_code = @customer_code;

    IF @tier = N'Bạc'
        SET @discount_percent = 5.0;
    ELSE IF @tier = N'Vàng'
        SET @discount_percent = 10.0;
    ELSE IF @tier = N'Kim Cương'
        SET @discount_percent = 15.0;

    RETURN @discount_percent;
END;
GO

-- 7.4 Hàm tổng hợp tổng doanh thu của một chi nhánh trong một khoảng thời gian
CREATE FUNCTION dbo.fn_GetBranchRevenue
(
    @branch_code VARCHAR(20),
    @from_date DATETIME,
    @to_date DATETIME
)
RETURNS DECIMAL(18,2)
AS
BEGIN
    DECLARE @total DECIMAL(18,2) = 0;

    SELECT @total = ISNULL(SUM(i.total_payment), 0)
    FROM invoices i
    INNER JOIN bookings b ON i.booking_code = b.booking_code
    WHERE b.branch_code = @branch_code
      AND i.invoice_date BETWEEN @from_date AND @to_date
      AND i.status = N'Đã thanh toán';

    RETURN @total;
END;
GO

-- 7.5 Hàm trả về danh sách các sân còn trống theo chi nhánh và khung giờ (Table-Valued Function)
CREATE FUNCTION dbo.fn_GetAvailableCourtsByBranch
(
    @branch_code VARCHAR(20),
    @booking_date DATE,
    @time_slot VARCHAR(50)
)
RETURNS TABLE
AS
RETURN
(
    SELECT 
        c.court_code,
        c.court_name,
        c.court_type,
        c.hourly_rate,
        c.status AS current_court_status,
        c.branch_code
    FROM courts c
    WHERE c.branch_code = @branch_code
      AND c.status <> N'Bảo trì'
      AND NOT EXISTS (
          SELECT 1 
          FROM bookings b
          WHERE b.court_code = c.court_code
            AND b.booking_date = @booking_date
            AND b.status <> N'Đã hủy'
            AND (
                b.time_slot = @time_slot
                OR CHARINDEX(@time_slot, b.time_slot) > 0
                OR CHARINDEX(b.time_slot, @time_slot) > 0
            )
      )
);
GO

-- =====================================================================
-- 8. THỦ TỤC LƯU TRỮ NGHIỆP VỤ (STORED PROCEDURES)
-- =====================================================================

-- 8.1 Thủ tục đặt & khóa sân trực tuyến với cơ chế bảo vệ giao dịch (Concurrency Transaction)
CREATE PROCEDURE dbo.sp_LockSlotAndCreateBooking
    @court_code VARCHAR(20),
    @branch_code VARCHAR(20),
    @booking_date DATE,
    @time_slot VARCHAR(50),
    @customer_name NVARCHAR(100),
    @customer_phone VARCHAR(20),
    @notes NVARCHAR(255) = NULL,
    @promo_code VARCHAR(20) = NULL,
    @addon_amount DECIMAL(18,2) = 0,
    @booking_code VARCHAR(30) OUTPUT,
    @deposit_amount DECIMAL(18,2) OUTPUT,
    @total_price DECIMAL(18,2) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        BEGIN TRANSACTION;

        IF dbo.fn_CheckSlotAvailability(@court_code, @booking_date, @time_slot) = 0
        BEGIN
            THROW 50001, N'Khung giờ hoặc sân đã có người đặt trước hoặc đang bảo trì!', 1;
        END

        DECLARE @new_code VARCHAR(30);
        DECLARE @random_num INT;
        SET @random_num = CAST(RAND(CHECKSUM(NEWID())) * 899999 + 100000 AS INT);
        SET @new_code = 'DS' + CAST(@random_num AS VARCHAR(10));
        WHILE EXISTS (SELECT 1 FROM bookings WHERE booking_code = @new_code)
        BEGIN
            SET @random_num = CAST(RAND(CHECKSUM(NEWID())) * 899999 + 100000 AS INT);
            SET @new_code = 'DS' + CAST(@random_num AS VARCHAR(10));
        END
        SET @booking_code = @new_code;

        DECLARE @hourly_price DECIMAL(18,2);
        SELECT @hourly_price = ISNULL(hourly_rate, 90000) FROM courts WHERE court_code = @court_code;

        DECLARE @court_fee DECIMAL(18,2);
        SET @court_fee = dbo.fn_CalculateBookingPrice(@court_code, @time_slot, @promo_code);

        SET @total_price = @court_fee + ISNULL(@addon_amount, 0);
        SET @deposit_amount = ROUND(@total_price * 0.3, 0);

        DECLARE @cust_code VARCHAR(20) = NULL;
        SELECT @cust_code = customer_code FROM customers WHERE phone = @customer_phone;

        INSERT INTO bookings (
            booking_code, customer_code, customer_name, customer_phone,
            court_code, branch_code, booking_date, time_slot,
            hourly_price, total_price, deposit_amount,
            payment_method, status, notes, created_at
        )
        VALUES (
            @booking_code, @cust_code, @customer_name, @customer_phone,
            @court_code, @branch_code, @booking_date, @time_slot,
            @hourly_price, @total_price, @deposit_amount,
            N'Chuyển khoản VietQR', N'Chờ cọc 30%', 
            ISNULL(@notes, N'Đặt qua hệ thống trực tuyến'), GETDATE()
        );

        INSERT INTO audit_logs (username, action_type, action_time, details, module, status)
        VALUES (
            ISNULL(@cust_code, 'GUEST'), 
            N'Khóa slot đặt sân trực tuyến', 
            GETDATE(), 
            N'Mã đơn: ' + @booking_code + N', Sân: ' + @court_code + N', Giờ: ' + @time_slot + N', Tiền cọc: ' + FORMAT(@deposit_amount, 'N0') + N'đ',
            N'Phân hệ Đặt Sân', N'Thành công'
        );

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- 8.2 Thủ tục xác nhận thanh toán cọc 30% qua VietQR
CREATE PROCEDURE dbo.sp_ConfirmBookingDeposit
    @booking_code VARCHAR(30),
    @amount DECIMAL(18,2),
    @method NVARCHAR(50) = N'VietQR Pro',
    @transaction_ref VARCHAR(100) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        BEGIN TRANSACTION;

        IF NOT EXISTS (SELECT 1 FROM bookings WHERE booking_code = @booking_code)
        BEGIN
            THROW 50002, N'Không tìm thấy mã đơn đặt sân trong cơ sở dữ liệu!', 1;
        END

        UPDATE bookings
        SET status = N'Đã cọc 30%',
            deposit_amount = @amount
        WHERE booking_code = @booking_code;

        DECLARE @pay_code VARCHAR(30) = 'TT' + SUBSTRING(@booking_code, 3, LEN(@booking_code));
        IF EXISTS (SELECT 1 FROM payments WHERE payment_code = @pay_code)
        BEGIN
            SET @pay_code = 'TT' + CAST(CAST(RAND(CHECKSUM(NEWID())) * 899999 + 100000 AS INT) AS VARCHAR(10));
        END

        INSERT INTO payments (
            payment_code, booking_code, amount, method, 
            transaction_type, transaction_ref, payment_time, status
        )
        VALUES (
            @pay_code, @booking_code, @amount, @method,
            N'Đặt cọc 30%', ISNULL(@transaction_ref, 'AUTO-VIETQR-' + @booking_code),
            GETDATE(), N'Thành công'
        );

        DECLARE @b_date DATE;
        DECLARE @c_code VARCHAR(20);
        SELECT @b_date = booking_date, @c_code = court_code FROM bookings WHERE booking_code = @booking_code;
        
        IF @b_date = CAST(GETDATE() AS DATE)
        BEGIN
            UPDATE courts SET status = N'Đặt trước' WHERE court_code = @c_code AND status = N'Trống';
        END

        INSERT INTO audit_logs (username, action_type, action_time, details, module, status)
        VALUES (
            'SYSTEM_VIETQR', 
            N'Xác nhận cọc 30% đơn đặt sân', 
            GETDATE(), 
            N'Đơn ' + @booking_code + N' đã nhận cọc: ' + FORMAT(@amount, 'N0') + N'đ qua ' + @method,
            N'Cổng Thanh Toán', N'Thành công'
        );

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- 8.3 Thủ tục Check-in nhận sân tại quầy POS
CREATE PROCEDURE dbo.sp_CheckInBooking
    @booking_code VARCHAR(30),
    @staff_code VARCHAR(20) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        BEGIN TRANSACTION;

        DECLARE @court_code VARCHAR(20);
        SELECT @court_code = court_code FROM bookings WHERE booking_code = @booking_code;

        IF @court_code IS NULL
        BEGIN
            THROW 50003, N'Không tìm thấy đơn đặt sân!', 1;
        END

        UPDATE bookings
        SET status = N'Đang sử dụng',
            check_in_time = GETDATE()
        WHERE booking_code = @booking_code;

        UPDATE courts
        SET status = N'Đang sử dụng'
        WHERE court_code = @court_code;

        INSERT INTO audit_logs (username, action_type, action_time, details, module, status)
        VALUES (
            ISNULL(@staff_code, 'POS_STAFF'),
            N'Check-in nhận sân tại quầy POS',
            GETDATE(),
            N'Khách nhận sân ' + @court_code + N' cho đơn ' + @booking_code,
            N'Phân hệ POS', N'Thành công'
        );

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- 8.4 Thủ tục Check-out trả sân & Xuất hóa đơn POS
CREATE PROCEDURE dbo.sp_CheckOutAndCreateInvoice
    @booking_code VARCHAR(30),
    @staff_code VARCHAR(20) = NULL,
    @payment_method NVARCHAR(50) = N'Tiền mặt',
    @invoice_code VARCHAR(30) OUTPUT,
    @final_payment DECIMAL(18,2) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        BEGIN TRANSACTION;

        DECLARE @court_code VARCHAR(20);
        DECLARE @court_fee DECIMAL(18,2);
        DECLARE @deposit_paid DECIMAL(18,2);
        DECLARE @cust_code VARCHAR(20);

        SELECT 
            @court_code = court_code,
            @court_fee = total_price,
            @deposit_paid = ISNULL(deposit_amount, 0),
            @cust_code = customer_code
        FROM bookings 
        WHERE booking_code = @booking_code;

        IF @court_code IS NULL
        BEGIN
            THROW 50004, N'Không tìm thấy thông tin đơn đặt sân để thanh toán!', 1;
        END

        UPDATE bookings
        SET status = N'Hoàn thành',
            check_out_time = GETDATE()
        WHERE booking_code = @booking_code;

        UPDATE courts
        SET status = N'Trống'
        WHERE court_code = @court_code;

        DECLARE @count INT;
        SELECT @count = COUNT(*) FROM invoices;
        SET @invoice_code = 'HD' + RIGHT('000000' + CAST(@count + 1 AS VARCHAR(10)), 6);

        SET @final_payment = @court_fee - @deposit_paid;
        IF @final_payment < 0 SET @final_payment = 0;

        INSERT INTO invoices (
            invoice_code, booking_code, staff_code, invoice_date,
            court_fee, equipment_fee, product_fee, discount_amount,
            deposit_paid, total_payment, payment_method, status
        )
        VALUES (
            @invoice_code, @booking_code, @staff_code, GETDATE(),
            @court_fee, 0, 0, 0,
            @deposit_paid, @final_payment, @payment_method, N'Đã thanh toán'
        );

        IF @cust_code IS NOT NULL
        BEGIN
            UPDATE customers
            SET total_play_hours = total_play_hours + 1.0,
                reward_points = reward_points + CAST(@court_fee / 10000 AS INT)
            WHERE customer_code = @cust_code;
        END

        INSERT INTO audit_logs (username, action_type, action_time, details, module, status)
        VALUES (
            ISNULL(@staff_code, 'POS_STAFF'),
            N'Check-out & Xuất hóa đơn POS',
            GETDATE(),
            N'Xuất hóa đơn ' + @invoice_code + N' cho đơn ' + @booking_code + N', thực thu: ' + FORMAT(@final_payment, 'N0') + N'đ',
            N'Phân hệ POS', N'Thành công'
        );

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- 8.5 Thủ tục hủy đơn đặt sân và áp dụng chính sách hoàn cọc (trước 4 tiếng hoàn 100%)
CREATE PROCEDURE dbo.sp_CancelBookingWithRefundPolicy
    @booking_code VARCHAR(30),
    @cancel_reason NVARCHAR(255) = N'Khách báo hủy lịch',
    @refund_amount DECIMAL(18,2) OUTPUT,
    @is_refunded BIT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        BEGIN TRANSACTION;

        DECLARE @booking_date DATE;
        DECLARE @time_slot VARCHAR(50);
        DECLARE @deposit DECIMAL(18,2);
        DECLARE @court_code VARCHAR(20);

        SELECT 
            @booking_date = booking_date,
            @time_slot = time_slot,
            @deposit = ISNULL(deposit_amount, 0),
            @court_code = court_code
        FROM bookings
        WHERE booking_code = @booking_code;

        IF @booking_date IS NULL
        BEGIN
            THROW 50005, N'Không tìm thấy mã đơn đặt sân để thực hiện hủy!', 1;
        END

        DECLARE @start_hour INT = 8;
        IF CHARINDEX('-', @time_slot) > 0
        BEGIN
            SET @start_hour = TRY_CAST(SUBSTRING(LTRIM(RTRIM(@time_slot)), 1, 2) AS INT);
        END
        IF @start_hour IS NULL SET @start_hour = 8;

        DECLARE @play_datetime DATETIME;
        SET @play_datetime = DATEADD(HOUR, @start_hour, CAST(@booking_date AS DATETIME));

        DECLARE @min_cancel_hours INT = 4;
        SELECT @min_cancel_hours = TRY_CAST(setting_value AS INT)
        FROM system_settings
        WHERE setting_code = 'CFG_CANCEL_HOURS';
        IF @min_cancel_hours IS NULL SET @min_cancel_hours = 4;

        DECLARE @diff_hours INT = DATEDIFF(HOUR, GETDATE(), @play_datetime);

        IF @diff_hours >= @min_cancel_hours AND @deposit > 0
        BEGIN
            SET @refund_amount = @deposit;
            SET @is_refunded = 1;

            INSERT INTO payments (
                payment_code, booking_code, amount, method,
                transaction_type, transaction_ref, payment_time, status
            )
            VALUES (
                'HT' + SUBSTRING(@booking_code, 3, LEN(@booking_code)),
                @booking_code, @deposit, N'Chuyển khoản VietQR',
                N'Hoàn tiền cọc', N'REFUND-100%-' + @booking_code,
                GETDATE(), N'Thành công'
            );
        END
        ELSE
        BEGIN
            SET @refund_amount = 0;
            SET @is_refunded = 0;
        END

        UPDATE bookings
        SET status = N'Đã hủy',
            notes = ISNULL(notes, '') + N' [Hủy: ' + @cancel_reason + N' | Hoàn cọc: ' + FORMAT(@refund_amount, 'N0') + N'đ]'
        WHERE booking_code = @booking_code;

        UPDATE courts
        SET status = N'Trống'
        WHERE court_code = @court_code AND status = N'Đặt trước';

        INSERT INTO audit_logs (username, action_type, action_time, details, module, status)
        VALUES (
            'CUSTOMER_PORTAL',
            N'Hủy đặt sân theo chính sách',
            GETDATE(),
            N'Hủy đơn ' + @booking_code + N'. Hoàn cọc: ' + FORMAT(@refund_amount, 'N0') + N'đ (' + CASE WHEN @is_refunded = 1 THEN N'Hoàn 100%' ELSE N'Không hoàn do hủy trễ' END + N')',
            N'Chính Sách Hoàn Cọc', N'Thành công'
        );

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- 8.6 Thủ tục lập phiếu nhập kho vật tư
CREATE PROCEDURE dbo.sp_ImportStockReceipt
    @receipt_code VARCHAR(30),
    @branch_code VARCHAR(20),
    @staff_code VARCHAR(20),
    @supplier_name NVARCHAR(100),
    @total_amount DECIMAL(18,2),
    @notes NVARCHAR(255) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        BEGIN TRANSACTION;

        INSERT INTO stock_receipts (receipt_code, branch_code, staff_code, receipt_date, supplier_name, total_amount, notes)
        VALUES (@receipt_code, @branch_code, @staff_code, GETDATE(), @supplier_name, @total_amount, @notes);

        INSERT INTO audit_logs (username, action_type, action_time, details, module, status)
        VALUES (
            @staff_code,
            N'Lập phiếu nhập kho vật tư',
            GETDATE(),
            N'Phiếu nhập ' + @receipt_code + N' tại chi nhánh ' + @branch_code + N', tổng tiền: ' + FORMAT(@total_amount, 'N0') + N'đ',
            N'Phân hệ Kho', N'Thành công'
        );

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- 8.7 Thủ tục báo cáo tổng kết doanh thu theo ngày của chi nhánh
CREATE PROCEDURE dbo.sp_GetBranchDailyRevenueReport
    @branch_code VARCHAR(20),
    @report_date DATE = NULL
AS
BEGIN
    SET NOCOUNT ON;
    IF @report_date IS NULL SET @report_date = CAST(GETDATE() AS DATE);

    SELECT 
        b.branch_code,
        br.branch_name,
        @report_date AS report_date,
        COUNT(DISTINCT b.booking_code) AS total_bookings,
        COUNT(DISTINCT CASE WHEN b.status = N'Hoàn thành' THEN b.booking_code END) AS completed_bookings,
        COUNT(DISTINCT CASE WHEN b.status = N'Đã hủy' THEN b.booking_code END) AS cancelled_bookings,
        ISNULL(SUM(i.court_fee), 0) AS total_court_fee,
        ISNULL(SUM(i.equipment_fee), 0) AS total_equipment_fee,
        ISNULL(SUM(i.product_fee), 0) AS total_product_fee,
        ISNULL(SUM(i.discount_amount), 0) AS total_discount,
        ISNULL(SUM(i.total_payment), 0) AS net_revenue
    FROM branches br
    LEFT JOIN bookings b ON br.branch_code = b.branch_code AND b.booking_date = @report_date
    LEFT JOIN invoices i ON b.booking_code = i.booking_code AND i.status = N'Đã thanh toán'
    WHERE br.branch_code = @branch_code
    GROUP BY b.branch_code, br.branch_name;
END;
GO

-- =====================================================================
-- 9. TRIGGER TỰ ĐỘNG HÓA VÀ RÀNG BUỘC TOÀN VẸN (TRIGGERS)
-- =====================================================================

-- 9.1 Trigger kiểm soát chống đặt trùng giờ trên cùng một sân (Chống Race Condition)
CREATE TRIGGER dbo.trg_CheckBookingOverlap
ON dbo.bookings
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        INNER JOIN dbo.bookings b ON i.court_code = b.court_code 
                                AND i.booking_date = b.booking_date
                                AND i.booking_code <> b.booking_code
        WHERE b.status <> N'Đã hủy'
          AND i.status <> N'Đã hủy'
          AND (
              i.time_slot = b.time_slot
              OR CHARINDEX(i.time_slot, b.time_slot) > 0
              OR CHARINDEX(b.time_slot, i.time_slot) > 0
          )
    )
    BEGIN
        RAISERROR (N'Lỗi xung đột lịch: Đã có đơn đặt sân khác sử dụng khung giờ này cho cùng một sân trong ngày!', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END
END;
GO

-- 9.2 Trigger tự động giảm số lượng tồn kho sản phẩm khi xuất hóa đơn POS
CREATE TRIGGER dbo.trg_UpdateStockOnInvoiceProduct
ON dbo.invoice_product_details
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE p
    SET p.stock_quantity = p.stock_quantity - i.quantity
    FROM dbo.products p
    INNER JOIN inserted i ON p.product_code = i.product_code;

    IF EXISTS (SELECT 1 FROM dbo.products WHERE stock_quantity < 0)
    BEGIN
        RAISERROR (N'Lỗi xuất kho: Số lượng tồn kho sản phẩm trong kho không đủ để bán!', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END
END;
GO

-- 9.3 Trigger tự động tăng số lượng tồn kho khi nhập hàng từ nhà cung cấp
CREATE TRIGGER dbo.trg_UpdateStockOnReceiptDetails
ON dbo.stock_product_details
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE p
    SET p.stock_quantity = p.stock_quantity + i.quantity
    FROM dbo.products p
    INNER JOIN inserted i ON p.product_code = i.product_code;
END;
GO

-- 9.4 Trigger tự động thăng hạng thẻ thành viên theo lũy kế giờ chơi
CREATE TRIGGER dbo.trg_UpdateCustomerMembershipLevel
ON dbo.customers
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(total_play_hours)
    BEGIN
        UPDATE c
        SET c.membership_tier = CASE
            WHEN c.total_play_hours >= 70 THEN N'Kim Cương'
            WHEN c.total_play_hours >= 30 THEN N'Vàng'
            WHEN c.total_play_hours >= 10 THEN N'Bạc'
            ELSE N'Đồng'
        END
        FROM dbo.customers c
        INNER JOIN inserted i ON c.customer_code = i.customer_code;
    END
END;
GO

-- 9.5 Trigger tự động ghi nhật ký hệ thống khi trạng thái sân bãi thay đổi
CREATE TRIGGER dbo.trg_AuditCourtStatusChange
ON dbo.courts
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(status)
    BEGIN
        INSERT INTO dbo.audit_logs (username, action_type, action_time, details, module, status)
        SELECT 
            'SYSTEM_COURT_MONITOR',
            N'Cập nhật trạng thái sân',
            GETDATE(),
            N'Sân [' + i.court_code + N' - ' + i.court_name + N'] chuyển từ [' + ISNULL(d.status, N'Chưa rõ') + N'] sang [' + ISNULL(i.status, N'Chưa rõ') + N']',
            N'Giám Sát Sân Bãi',
            N'Thành công'
        FROM inserted i
        INNER JOIN deleted d ON i.court_code = d.court_code
        WHERE i.status <> d.status;
    END
END;
GO

-- 9.6 Trigger xác thực tính hợp lệ của chương trình khuyến mãi
CREATE TRIGGER dbo.trg_ValidatePromotionDates
ON dbo.promotions
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1 
        FROM inserted 
        WHERE end_date < start_date OR discount_value <= 0
    )
    BEGIN
        RAISERROR (N'Lỗi: Ngày kết thúc khuyến mãi phải lớn hơn hoặc bằng ngày bắt đầu và mức giảm giá phải lớn hơn 0!', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END
END;
GO

PRINT N'=====================================================================';
PRINT N'>>> ĐÃ KHỞI TẠO TOÀN DIỆN 25 BẢNG DỮ LIỆU, 25 VIEW TIẾNG VIỆT,';
PRINT N'>>> 5 FUNCTIONS, 7 STORED PROCEDURES VÀ 6 TRIGGERS HOÀN TẤT THÀNH CÔNG!';
PRINT N'=====================================================================';
GO

