-- =====================================================================
-- BẢN SAO LƯU TOÀN DIỆN HỆ THỐNG QUẢN LÝ SÂN CẦU LÔNG UTE SPORT
-- Thời gian xuất: 2026-10-20 23:14:01
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
(1, N'CL01', N'CN01', N'Sân cầu lông 1 (VIP Yonex)', N'Cầu lông VIP', 120000.00, N'Đã đặt trước', N'/images/badminton-court.jpg'),
(2, N'CL02', N'CN01', N'Sân cầu lông 2', N'Cầu lông tiêu chuẩn', 90000.00, N'Đã đặt trước', N'/images/badminton-court.jpg'),
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
-- Dữ liệu bảng: customers (37 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT customers ON;
INSERT INTO customers (id, customer_code, full_name, phone, email, password, total_play_hours, reward_points, membership_tier, status, created_at, raw_password) VALUES
(1, N'KH001', N'Lê Bá Đạt', N'0912345678', N'dat.le@gmail.com', N'123456', 42.5, 4250, N'Kim Cương', N'Hoạt động', N'2026-10-15 14:22:27.617', NULL),
(2, N'KH002', N'Nguyễn Hoàng Nam', N'0903123456', N'nam.nguyen@gmail.com', N'123456', 28.0, 2800, N'Vàng', N'Hoạt động', N'2026-10-15 14:22:27.617', NULL),
(3, N'KH003', N'Phạm Gia Huy', N'0988777666', N'huy.pham@gmail.com', N'123456', 15.0, 1500, N'Bạc', N'Hoạt động', N'2026-10-15 14:22:27.617', NULL),
(4, N'KH004', N'Vũ Thị Mai Anh', N'0977555444', N'maianh.vu@gmail.com', N'123456', 8.0, 800, N'Bạc', N'Hoạt động', N'2026-10-15 14:22:27.617', NULL),
(5, N'KH005', N'Trần Thanh Hằng', N'0933444555', N'hang.tran@gmail.com', N'123456', 4.0, 400, N'Đồng', N'Hoạt động', N'2026-10-15 14:22:27.617', NULL),
(6, N'KH006', N'Bùi Khánh Linh', N'0909111222', N'linh.bui@gmail.com', N'123456', 3.5, 350, N'Đồng', N'Hoạt động', N'2026-10-15 14:22:27.617', NULL),
(14, N'KH87872', N'Nguyen Van A', N'0908777666', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-19 14:09:35.27', NULL),
(36, N'KH61279', N'Lê Văn A', N'0977255426', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-19 21:49:16.563', NULL),
(37, N'KH86465', N'Lê Văn A', N'0977450917', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-19 21:49:40.617', NULL),
(40, N'KH11137', N'Lê Văn A', N'0977706455', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 17:34:43.88', NULL),
(41, N'KH27956', N'Lê Văn A', N'0977213090', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 17:38:07.577', NULL),
(42, N'KH12536', N'Lê Văn A', N'0977400545', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 17:44:51.093', NULL),
(43, N'KH66054', N'Lê Văn A', N'0977248973', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 17:49:06.4', NULL),
(44, N'KH94877', N'Lê Văn A', N'0977889799', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 17:56:32.0', NULL),
(45, N'KH55336', N'Lê Văn A', N'0977304111', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 18:07:18.04', NULL),
(46, N'KH67539', N'Lê Văn A', N'0977850267', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 18:20:23.237', NULL),
(47, N'KH73369', N'Lê Văn A', N'0977219039', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 18:26:00.197', NULL),
(48, N'KH78448', N'Lê Văn A', N'0977775893', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 18:41:40.16', NULL),
(49, N'KH41405', N'Lê Văn A', N'0977614828', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 18:48:02.32', NULL),
(50, N'KH19513', N'Lê Văn A', N'0977244753', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 18:58:16.0', NULL),
(51, N'KH98866', N'Lê Văn A', N'0977585020', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 19:05:48.317', NULL),
(52, N'KH91718', N'Lê Văn A', N'0977835065', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 19:13:45.31', NULL),
(53, N'KH65519', N'Lê Văn A', N'0977273456', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 19:24:32.537', NULL),
(54, N'KH71811', N'Lê Văn A', N'0977127820', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 19:29:32.86', NULL),
(55, N'KH97751', N'Lê Văn A', N'0977900221', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 19:46:16.213', NULL),
(56, N'KH82595', N'Lê Văn A', N'0977383047', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 20:05:32.127', NULL),
(57, N'KH0074', N'Nguyen Phuoc Tho', N'0559303350', N'tho8189@gmail.com', N'$2a$10$NjTmrmojZCm6NvqmQ5uh2.r9Ad.Haau04vs6jiHtKqPx1q/t24Yoe', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 20:20:48.093', NULL),
(58, N'KH93702', N'Lê Văn A', N'0977223106', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 20:24:49.353', NULL),
(59, N'KH93554', N'Lê Văn A', N'0977647593', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 22:22:09.03', NULL),
(60, N'KH67848', N'Lê Văn A', N'0977186896', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 22:26:49.32', NULL),
(63, N'KH16044', N'Lê Văn A', N'0977640640', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 22:29:56.827', NULL),
(66, N'KH11173', N'Lê Văn A', N'0977753770', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 22:41:13.043', NULL),
(67, N'KH59450', N'Lê Văn A', N'0977444137', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 22:49:18.887', NULL),
(70, N'KH44659', N'Lê Văn A', N'0977497234', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 22:55:40.393', NULL),
(73, N'KH28414', N'Lê Văn A', N'0977591867', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 23:00:40.76', NULL),
(76, N'KH68192', N'Lê Văn A', N'0977274644', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 23:09:52.853', NULL),
(79, N'KH43806', N'Lê Văn A', N'0977116919', NULL, N'123456', 0.0, 0, N'Đồng', N'Hoạt động', N'2026-10-20 23:13:56.997', NULL);
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
-- Dữ liệu bảng: users (11 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT users ON;
INSERT INTO users (id, username, password, full_name, email, phone, role, staff_code, branch_code, status, created_at, last_login, raw_password) VALUES
(1, N'director', N'$2a$10$4YxyiJrN2fnXRF1XRI3ZBeoVJ9PMRijK.tl8M6veMfELkxw26hGOO', N'Nguyễn Phước Thọ', N'director@utesport.vn', N'0901111222', N'DIRECTOR', N'NS01', N'ALL', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL, NULL),
(2, N'manager', N'$2a$10$6FTz80RaQJpWOhhTEXnbaeQzOpiHbR8TpaBzCin.F3SdP5JTo3jIG', N'Trần Phúc Bảo', N'manager@utesport.vn', N'0903123456', N'MANAGER', N'NS02', N'CN01', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL, NULL),
(3, N'pos', N'$2a$10$mZZymrebYdkouM3QTa91z.WfjxfpYe.gYoGeWWSygklyZ24l76Ym2', N'Nguyễn Thanh Tâm', N'pos@utesport.vn', N'0904555666', N'POS', N'NS03', N'CN01', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL, NULL),
(4, N'admin', N'$2a$10$Rv6iWqvKG.8BQ8mT5688e.y.umw24Ikp2jgoAOSvWlY8DTdn6fWbW', N'Trần Biểu Hương', N'admin@utesport.vn', N'0908888999', N'ADMIN', NULL, N'ALL', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL, NULL),
(5, N'customer', N'$2a$10$xBd2zWe6O6bXIdDTymGSjuRP5KofOHFFh.XdfSLDUfP9VSt.LD76C', N'Lê Bá Đạt', N'khachhang@gmail.com', N'0912345678', N'CUSTOMER', NULL, N'CN01', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL, NULL),
(6, N'nam.vh', N'123456', N'Võ Hoàng Nam', N'nam.vh@utesport.vn', N'0901111222', N'DIRECTOR', N'NS01', N'ALL', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL, NULL),
(7, N'quan.nv', N'123456', N'Nguyễn Văn Quản', N'quan.nv@utesport.vn', N'0903333444', N'MANAGER', N'NS05', N'CN02', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL, NULL),
(8, N'nam.th', N'123456', N'Trần Hữu Nam', N'nam.th@utesport.vn', N'0903789012', N'MANAGER', N'NS06', N'CN03', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL, NULL),
(9, N'khang.lh', N'123456', N'Lê Hoàng Khang', N'khang.lh@utesport.vn', N'0903555777', N'MANAGER', N'NS07', N'CN04', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL, NULL),
(10, N'mai.lt', N'123456', N'Lê Thị Mai', N'mai.lt@utesport.vn', N'0904777888', N'POS', N'NS04', N'CN01', N'Hoạt động', N'2026-10-15 14:22:27.62', NULL, NULL),
(74, N'0559303350', N'$2a$10$NjTmrmojZCm6NvqmQ5uh2.r9Ad.Haau04vs6jiHtKqPx1q/t24Yoe', N'Nguyen Phuoc Tho', N'tho8189@gmail.com', N'0559303350', N'CUSTOMER', NULL, N'CN01', N'Hoạt động', N'2026-10-20 20:20:48.08', NULL, NULL);
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
(1, N'P01', N'CN01', N'Nước bù khoáng Revive Chanh muối 500ml', N'Nước giải khát', N'Chai', 15000.00, 7454.57, 150, 20),
(2, N'P02', N'CN01', N'Nước điện giải Pocari Sweat 500ml', N'Nước giải khát', N'Chai', 18000.00, 11555.56, 270, 20),
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
(1, N'GD01', N'CN01', N'Giải Cầu Lông Đôi Nam Nữ Mùa Thu UTE Open 2026', N'2026-10-10 08:00:00.0', N'2026-12-31 23:59:59.0', N'Thi d?u theo th? th?c lo?i tr?c ti?p 3 hi?p 21 di?m theo lu?t BWF. M?i VÐV ch? du?c dang ký 1 n?i dung.', N'Đôi nam nữ', 32, 11, 300000.00, 20000000.00, N'Đang mở đăng ký'),
(2, N'GD02', N'CN01', N'Giải Cầu Lông Doanh Nghiệp & Sinh Viên Cúp Thủ Đức 2026', N'2026-10-25 08:00:00.0', N'2026-10-26 17:00:00.0', N'Gi?i d?u phong trào k?t n?i các doanh nghi?p và c?u sinh viên dam mê c?u lông.', N'Tổng hợp nhiều nội dung', 24, 16, 250000.00, 15000000.00, N'Sắp mở đăng ký'),
(3, N'GD03', N'CN02', N'Giải Đơn Nam Bình Thạnh Mở Rộng 2026', N'2026-11-05 08:00:00.0', N'2026-11-06 18:00:00.0', N'Gi?i don nam các CLB phong trào khu v?c Bình Th?nh - Phú Nhu?n.', N'Đơn nam', 16, 0, 200000.00, 10000000.00, N'Sắp mở đăng ký');
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
-- Dữ liệu bảng: work_shifts (59 bản ghi)
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
(19, N'BG642026', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:29', N'Đã kết ca'),
(20, N'BG628496', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:29', N'Đã kết ca'),
(21, N'BG960337', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:39', N'Đã kết ca'),
(22, N'BG967663', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:41', N'Đã kết ca'),
(23, N'BG632283', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:49', N'Đã kết ca'),
(24, N'BG372310', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:49', N'Đã kết ca'),
(25, N'BG878499', N'NS03', N'CN01', N'mon', N'handover', N'POS', N'POS', N'2026-10-19', N'06:00', N'21:49', N'Đã kết ca'),
(26, N'BG987926', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'17:34', N'Đã kết ca'),
(27, N'BG331514', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'17:38', N'Đã kết ca'),
(28, N'BG635934', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'17:44', N'Đã kết ca'),
(29, N'BG310046', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'17:47', N'Đã kết ca'),
(30, N'BG602631', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'17:49', N'Đã kết ca'),
(31, N'BG791272', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'17:56', N'Đã kết ca'),
(32, N'BG250382', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'18:07', N'Đã kết ca'),
(33, N'BG974738', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'18:20', N'Đã kết ca'),
(34, N'BG447159', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'18:26', N'Đã kết ca'),
(35, N'BG515609', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'18:41', N'Đã kết ca'),
(36, N'BG363131', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'18:48', N'Đã kết ca'),
(37, N'BG959084', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'18:58', N'Đã kết ca'),
(38, N'BG922829', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'19:05', N'Đã kết ca'),
(39, N'BG909353', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'19:13', N'Đã kết ca'),
(40, N'BG770312', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'19:24', N'Đã kết ca'),
(41, N'BG922638', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'19:29', N'Đã kết ca'),
(42, N'BG943063', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'19:46', N'Đã kết ca'),
(43, N'BG867873', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'20:05', N'Đã kết ca'),
(44, N'BG855301', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'20:24', N'Đã kết ca'),
(45, N'BG1073214', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'22:22', N'Đã kết ca'),
(46, N'BG374076', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'22:26', N'Đã kết ca'),
(47, N'BG412489', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'22:26', N'Đã kết ca'),
(48, N'BG633336', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'22:29', N'Đã kết ca'),
(49, N'BG636969', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'22:30', N'Đã kết ca'),
(50, N'BG498955', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'22:41', N'Đã kết ca'),
(51, N'BG826237', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'22:49', N'Đã kết ca'),
(52, N'BG792584', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'22:49', N'Đã kết ca'),
(53, N'BG454449', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'22:55', N'Đã kết ca'),
(54, N'BG923735', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'22:55', N'Đã kết ca'),
(55, N'BG290323', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'23:00', N'Đã kết ca'),
(56, N'BG753051', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'23:00', N'Đã kết ca'),
(57, N'BG363618', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'23:09', N'Đã kết ca'),
(58, N'BG1064712', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'23:10', N'Đã kết ca'),
(59, N'BG247162', N'NS03', N'CN01', N'tue', N'handover', N'POS', N'POS', N'2026-10-20', N'06:00', N'23:13', N'Đã kết ca');
SET IDENTITY_INSERT work_shifts OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: stock_receipts (63 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT stock_receipts ON;
INSERT INTO stock_receipts (id, receipt_code, branch_code, staff_code, receipt_date, supplier_name, total_amount, notes) VALUES
(1, N'PN0101', N'CN01', N'NS02', N'2026-10-10 14:22:27.623', N'Công ty TNHH Thể Thao Yonex Việt Nam', 14500000.00, N'Nhập bổ sung vợt Yonex Astrox và 50 ống cầu Hải Yến'),
(2, N'PN0102', N'CN01', N'NS02', N'2026-10-13 14:22:27.623', N'NPP Dụng Cụ Cầu Lông Đại Phát', 4200000.00, N'Nhập nước bù khoáng Revive và Pocari Sweat đợt cuối tháng'),
(22, N'PNK904401', N'CN01', N'NS02', N'2026-10-19 21:04:12.507', N'Nha Cung Cap Yonex Viet Nam', 175000.00, N'Nhap kho dau tuan test SQL - Nước bù khoáng Revive Chanh muối 500ml x25'),
(40, N'PNK938582', N'CN01', N'NS03', N'2026-10-19 21:49:17.593', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(41, N'PNK354262', N'CN01', N'NS03', N'2026-10-19 21:49:18.253', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(42, N'PNK130683', N'CN01', N'NS03', N'2026-10-19 21:49:41.463', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(43, N'PNK382387', N'CN01', N'NS03', N'2026-10-19 21:49:42.023', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(47, N'PNK549334', N'CN01', N'NS03', N'2026-10-20 17:34:45.777', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(48, N'PNK753109', N'CN01', N'NS03', N'2026-10-20 17:34:46.867', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(49, N'PNK985127', N'CN01', N'NS03', N'2026-10-20 17:38:09.163', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(50, N'PNK625252', N'CN01', N'NS03', N'2026-10-20 17:38:10.23', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(51, N'PNK428185', N'CN01', N'NS03', N'2026-10-20 17:44:52.29', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(52, N'PNK821963', N'CN01', N'NS03', N'2026-10-20 17:44:52.983', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(53, N'PNK866437', N'CN01', N'NS03', N'2026-10-20 17:47:55.547', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(54, N'PNK762697', N'CN01', N'NS03', N'2026-10-20 17:47:56.127', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(55, N'PNK844541', N'CN01', N'NS03', N'2026-10-20 17:49:08.157', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(56, N'PNK671383', N'CN01', N'NS03', N'2026-10-20 17:49:09.023', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(57, N'PNK276596', N'CN01', N'NS03', N'2026-10-20 17:56:33.87', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(58, N'PNK737179', N'CN01', N'NS03', N'2026-10-20 17:56:34.82', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(59, N'PNK704813', N'CN01', N'NS03', N'2026-10-20 18:07:19.003', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(60, N'PNK408913', N'CN01', N'NS03', N'2026-10-20 18:07:19.593', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(61, N'PNK107660', N'CN01', N'NS03', N'2026-10-20 18:20:24.287', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(62, N'PNK693491', N'CN01', N'NS03', N'2026-10-20 18:20:24.91', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(63, N'PNK935484', N'CN01', N'NS03', N'2026-10-20 18:26:01.28', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(64, N'PNK397712', N'CN01', N'NS03', N'2026-10-20 18:26:01.913', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(65, N'PNK791923', N'CN01', N'NS03', N'2026-10-20 18:41:41.18', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(66, N'PNK794546', N'CN01', N'NS03', N'2026-10-20 18:41:41.763', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(67, N'PNK654415', N'CN01', N'NS03', N'2026-10-20 18:48:03.34', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(68, N'PNK891008', N'CN01', N'NS03', N'2026-10-20 18:48:03.957', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(69, N'PNK961478', N'CN01', N'NS03', N'2026-10-20 18:58:17.03', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(70, N'PNK434841', N'CN01', N'NS03', N'2026-10-20 18:58:17.647', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(71, N'PNK164032', N'CN01', N'NS03', N'2026-10-20 19:05:49.477', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(72, N'PNK371179', N'CN01', N'NS03', N'2026-10-20 19:05:50.21', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(73, N'PNK388581', N'CN01', N'NS03', N'2026-10-20 19:13:46.51', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(74, N'PNK578048', N'CN01', N'NS03', N'2026-10-20 19:13:47.117', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(75, N'PNK567696', N'CN01', N'NS03', N'2026-10-20 19:24:33.613', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(76, N'PNK422014', N'CN01', N'NS03', N'2026-10-20 19:24:34.247', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(77, N'PNK463018', N'CN01', N'NS03', N'2026-10-20 19:29:33.897', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(78, N'PNK694927', N'CN01', N'NS03', N'2026-10-20 19:29:34.503', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(79, N'PNK168337', N'CN01', N'NS03', N'2026-10-20 19:46:17.347', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(80, N'PNK276667', N'CN01', N'NS03', N'2026-10-20 19:46:18.077', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(81, N'PNK505263', N'CN01', N'NS03', N'2026-10-20 20:05:33.19', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(82, N'PNK899295', N'CN01', N'NS03', N'2026-10-20 20:05:33.787', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(83, N'PNK326473', N'CN01', N'NS03', N'2026-10-20 20:24:50.68', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(84, N'PNK113984', N'CN01', N'NS03', N'2026-10-20 20:24:51.397', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(85, N'PNK274720', N'CN01', N'NS03', N'2026-10-20 22:22:10.167', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(86, N'PNK625839', N'CN01', N'NS03', N'2026-10-20 22:22:10.763', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(87, N'PNK857377', N'CN01', N'NS03', N'2026-10-20 22:26:50.393', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(88, N'PNK337108', N'CN01', N'NS03', N'2026-10-20 22:26:51.04', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(92, N'PNK391110', N'CN01', N'NS03', N'2026-10-20 22:29:57.877', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(93, N'PNK551825', N'CN01', N'NS03', N'2026-10-20 22:29:58.503', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(97, N'PNK635826', N'CN01', N'NS03', N'2026-10-20 22:41:14.26', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(98, N'PNK735372', N'CN01', N'NS03', N'2026-10-20 22:41:15.087', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(99, N'PNK216661', N'CN01', N'NS03', N'2026-10-20 22:49:19.94', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(100, N'PNK241622', N'CN01', N'NS03', N'2026-10-20 22:49:20.537', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(104, N'PNK293713', N'CN01', N'NS03', N'2026-10-20 22:55:41.49', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(105, N'PNK334998', N'CN01', N'NS03', N'2026-10-20 22:55:42.067', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(109, N'PNK910077', N'CN01', N'NS03', N'2026-10-20 23:00:41.79', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(110, N'PNK484820', N'CN01', N'NS03', N'2026-10-20 23:00:42.41', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(114, N'PNK250375', N'CN01', N'NS03', N'2026-10-20 23:09:54.127', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(115, N'PNK424441', N'CN01', N'NS03', N'2026-10-20 23:09:54.893', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10'),
(119, N'PNK966902', N'CN01', N'NS03', N'2026-10-20 23:13:58.01', N'NCC Test TC42', 60000.00, N'Test auto allocate - Nước điện giải Pocari Sweat 500ml x5'),
(120, N'PNK944339', N'CN01', N'NS03', N'2026-10-20 23:13:58.613', N'Công ty TNHH Thể Thao Động Lực', 80000.00, N'Nhập bổ sung nước giải khát TC41 - Nước bù khoáng Revive Chanh muối 500ml x10');
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
-- Dữ liệu bảng: stock_product_details (64 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT stock_product_details ON;
INSERT INTO stock_product_details (id, receipt_code, product_code, quantity, import_price, total_amount) VALUES
(1, N'PN0102', N'P01', 200, 8000.00, 1600000.00),
(2, N'PN0102', N'P02', 150, 11000.00, 1650000.00),
(3, N'PN0102', N'P03', 190, 5000.00, 950000.00),
(9, N'PNK904401', N'P01', 25, 7000.00, 175000.00),
(22, N'PNK938582', N'P02', 5, 12000.00, 60000.00),
(23, N'PNK354262', N'P01', 10, 8000.00, 80000.00),
(24, N'PNK130683', N'P02', 5, 12000.00, 60000.00),
(25, N'PNK382387', N'P01', 10, 8000.00, 80000.00),
(28, N'PNK549334', N'P02', 5, 12000.00, 60000.00),
(29, N'PNK753109', N'P01', 10, 8000.00, 80000.00),
(30, N'PNK985127', N'P02', 5, 12000.00, 60000.00),
(31, N'PNK625252', N'P01', 10, 8000.00, 80000.00),
(32, N'PNK428185', N'P02', 5, 12000.00, 60000.00),
(33, N'PNK821963', N'P01', 10, 8000.00, 80000.00),
(34, N'PNK866437', N'P02', 5, 12000.00, 60000.00),
(35, N'PNK762697', N'P01', 10, 8000.00, 80000.00),
(36, N'PNK844541', N'P02', 5, 12000.00, 60000.00),
(37, N'PNK671383', N'P01', 10, 8000.00, 80000.00),
(38, N'PNK276596', N'P02', 5, 12000.00, 60000.00),
(39, N'PNK737179', N'P01', 10, 8000.00, 80000.00),
(40, N'PNK704813', N'P02', 5, 12000.00, 60000.00),
(41, N'PNK408913', N'P01', 10, 8000.00, 80000.00),
(42, N'PNK107660', N'P02', 5, 12000.00, 60000.00),
(43, N'PNK693491', N'P01', 10, 8000.00, 80000.00),
(44, N'PNK935484', N'P02', 5, 12000.00, 60000.00),
(45, N'PNK397712', N'P01', 10, 8000.00, 80000.00),
(46, N'PNK791923', N'P02', 5, 12000.00, 60000.00),
(47, N'PNK794546', N'P01', 10, 8000.00, 80000.00),
(48, N'PNK654415', N'P02', 5, 12000.00, 60000.00),
(49, N'PNK891008', N'P01', 10, 8000.00, 80000.00),
(50, N'PNK961478', N'P02', 5, 12000.00, 60000.00),
(51, N'PNK434841', N'P01', 10, 8000.00, 80000.00),
(52, N'PNK164032', N'P02', 5, 12000.00, 60000.00),
(53, N'PNK371179', N'P01', 10, 8000.00, 80000.00),
(54, N'PNK388581', N'P02', 5, 12000.00, 60000.00),
(55, N'PNK578048', N'P01', 10, 8000.00, 80000.00),
(56, N'PNK567696', N'P02', 5, 12000.00, 60000.00),
(57, N'PNK422014', N'P01', 10, 8000.00, 80000.00),
(58, N'PNK463018', N'P02', 5, 12000.00, 60000.00),
(59, N'PNK694927', N'P01', 10, 8000.00, 80000.00),
(60, N'PNK168337', N'P02', 5, 12000.00, 60000.00),
(61, N'PNK276667', N'P01', 10, 8000.00, 80000.00),
(62, N'PNK505263', N'P02', 5, 12000.00, 60000.00),
(63, N'PNK899295', N'P01', 10, 8000.00, 80000.00),
(64, N'PNK326473', N'P02', 5, 12000.00, 60000.00),
(65, N'PNK113984', N'P01', 10, 8000.00, 80000.00),
(66, N'PNK274720', N'P02', 5, 12000.00, 60000.00),
(67, N'PNK625839', N'P01', 10, 8000.00, 80000.00),
(68, N'PNK857377', N'P02', 5, 12000.00, 60000.00),
(69, N'PNK337108', N'P01', 10, 8000.00, 80000.00),
(72, N'PNK391110', N'P02', 5, 12000.00, 60000.00),
(73, N'PNK551825', N'P01', 10, 8000.00, 80000.00),
(76, N'PNK635826', N'P02', 5, 12000.00, 60000.00),
(77, N'PNK735372', N'P01', 10, 8000.00, 80000.00),
(78, N'PNK216661', N'P02', 5, 12000.00, 60000.00),
(79, N'PNK241622', N'P01', 10, 8000.00, 80000.00),
(82, N'PNK293713', N'P02', 5, 12000.00, 60000.00),
(83, N'PNK334998', N'P01', 10, 8000.00, 80000.00),
(86, N'PNK910077', N'P02', 5, 12000.00, 60000.00),
(87, N'PNK484820', N'P01', 10, 8000.00, 80000.00),
(90, N'PNK250375', N'P02', 5, 12000.00, 60000.00),
(91, N'PNK424441', N'P01', 10, 8000.00, 80000.00),
(94, N'PNK966902', N'P02', 5, 12000.00, 60000.00),
(95, N'PNK944339', N'P01', 10, 8000.00, 80000.00);
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
-- Dữ liệu bảng: system_settings (18 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT system_settings ON;
INSERT INTO system_settings (id, setting_code, setting_name, setting_value, description, updated_at) VALUES
(1, N'CFG_DEPOSIT_RATE', N'Tỷ lệ đặt cọc tối thiểu (%)', N'30', N'Khách hàng phải thanh toán cọc ít nhất 30% để giữ sân hợp lệ (QĐ-KH02)', N'2026-10-20 20:18:06.183'),
(2, N'CFG_HOLDING_MINUTES', N'Thời gian tạm giữ chỗ chờ cọc (Phút)', N'10', N'Sau 10 phút nếu không có giao dịch cọc, hệ thống tự động giải phóng ô sân (QĐ-KH02)', N'2026-10-20 20:18:06.213'),
(3, N'CFG_CANCEL_HOURS', N'Thời hạn hủy sân tối thiểu để được hoàn cọc (Giờ)', N'4', N'Hủy trước giờ chơi tối thiểu 4 tiếng để được nhận lại 100% tiền cọc (QĐ-KH03)', N'2026-10-20 20:18:06.223'),
(4, N'CFG_ADVANCE_DAYS', N'Số ngày tối đa được phép đặt trước', N'30', N'Giới hạn đặt sân trong vòng 30 ngày tới', N'2026-10-20 20:18:06.233'),
(5, N'CFG_VNPAY_TMN', N'Mã đối tác VNPay (TMN Code)', N'UTESPORT2026', N'Tích hợp cổng thanh toán VNPay QR Sandbox/Production', N'2026-10-20 20:18:06.24'),
(6, N'CFG_MOMO_PARTNER', N'Partner Code MoMo Business', N'MOMO_UTE_BADMINTON', N'Tích hợp cổng Ví điện tử MoMo Business', N'2026-10-20 20:18:06.267'),
(7, N'CFG_VNPAY_ENV', N'CFG_VNPAY_ENV', N'SANDBOX', NULL, N'2026-10-20 20:18:06.247'),
(8, N'CFG_VNPAY_SECRET', N'CFG_VNPAY_SECRET', N'••••••••••••••••••••••••', NULL, N'2026-10-20 20:18:06.26'),
(9, N'CFG_MOMO_KEY', N'CFG_MOMO_KEY', N'F8B24C5E9012A34B', NULL, N'2026-10-20 20:18:06.273'),
(10, N'CFG_POINTS_PER_1000', N'CFG_POINTS_PER_1000', N'1', NULL, N'2026-10-20 20:18:06.277'),
(11, N'CFG_TIER_SILVER_POINTS', N'CFG_TIER_SILVER_POINTS', N'500', NULL, N'2026-10-20 20:18:06.28'),
(12, N'CFG_TIER_GOLD_POINTS', N'CFG_TIER_GOLD_POINTS', N'2000', NULL, N'2026-10-20 20:18:06.29'),
(13, N'CFG_TIER_DIAMOND_POINTS', N'CFG_TIER_DIAMOND_POINTS', N'4000', NULL, N'2026-10-20 20:18:06.3'),
(14, N'CFG_MAIL_USERNAME', N'CFG_MAIL_USERNAME', N'tho8189@gmail.com', NULL, N'2026-10-20 20:18:06.303'),
(15, N'CFG_MAIL_PASSWORD', N'CFG_MAIL_PASSWORD', N'igzgnxjqpqecixuf', NULL, N'2026-10-20 20:18:06.313'),
(16, N'CFG_MAIL_FROM_NAME', N'CFG_MAIL_FROM_NAME', N'UTE Sport Badminton Arena', NULL, N'2026-10-20 20:18:06.32'),
(17, N'CFG_MAIL_PORT', N'CFG_MAIL_PORT', N'587', NULL, N'2026-10-20 20:18:06.327'),
(18, N'CFG_MAIL_HOST', N'CFG_MAIL_HOST', N'smtp.gmail.com', NULL, N'2026-10-20 20:18:06.33');
SET IDENTITY_INSERT system_settings OFF;
GO


-- -----------------------------------------------------
-- Dữ liệu bảng: audit_logs (726 bản ghi)
-- -----------------------------------------------------
SET IDENTITY_INSERT audit_logs ON;
INSERT INTO audit_logs (log_id, username, action_type, action_time, details, ip_address, module, status, user_code) VALUES
(1, N'nam.vh', N'Phê duyệt bảng giá giờ vàng Sân VIP Yonex', N'2026-10-15 14:07:27.623', N'Phê duy?t giá 120.000d cho khung gi? 17:00-22:00', N'192.168.1.105', N'Phân hệ Giám đốc', N'Thành công', NULL),
(2, N'mai.lt', N'Xác nhận nhận sân (Check-in) phiếu BK-1082', N'2026-10-15 13:34:27.623', N'Check-in sân CL02 cho khách Nguy?n Hoàng Nam', N'192.168.1.112', N'Phân hệ POS Bán Hàng', N'Thành công', NULL),
(3, N'admin', N'Cập nhật ma trận phân quyền chi nhánh', N'2026-10-15 12:22:27.623', N'Phân quy?n qu?n lý d? li?u chi nhánh CN01 cho tài kho?n bao.tran', N'192.168.1.200', N'Phân hệ Quản trị', N'Thành công', NULL),
(4, N'hacker_guest', N'Đăng nhập thất bại quá 5 lần liên tiếp', N'2026-10-15 10:22:27.623', N'C?nh báo b?o m?t: Brute-force th? m?t kh?u tài kho?n admin', N'14.232.18.99', N'Cổng bảo mật xác thực', N'Cảnh báo', NULL),
(5, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 13:59:20.43', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(6, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 13:59:20.47', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(10, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 13:59:20.987', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(12, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 13:59:21.0', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(15, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:00:22.553', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(16, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:00:22.557', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(21, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:00:22.613', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(23, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:00:22.617', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(26, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:03:12.083', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(27, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:03:12.16', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(28, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:03:12.197', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(29, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:06:52.797', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(30, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:06:52.87', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(31, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:06:52.903', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(32, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:09:35.4', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(33, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:09:35.473', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(34, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:09:35.5', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(35, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.163', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(36, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.233', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(37, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.263', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(38, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.383', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(39, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.387', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(44, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.43', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(46, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:20.433', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(49, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.653', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(50, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.71', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(51, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.733', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(52, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.843', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(53, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.847', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(58, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.903', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(60, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 14:10:49.907', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(63, N'director', N'Đăng nhập hệ thống', N'2026-10-19 20:39:05.65', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(64, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 20:39:05.737', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(65, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 20:39:05.817', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(66, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 20:39:05.893', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(67, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 20:39:05.97', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(68, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:06.223', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(69, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:06.387', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(70, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:06.533', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(71, N'manager', N'Đăng nhập thất bại', N'2026-10-19 20:39:06.667', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(72, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:06.777', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(73, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:06.79', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(78, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:06.973', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(80, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:39:07.003', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(83, N'director', N'Đăng nhập hệ thống', N'2026-10-19 20:40:31.843', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(84, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 20:40:31.92', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(85, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 20:40:31.997', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(86, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 20:40:32.077', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(87, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 20:40:32.157', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(88, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:32.4', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(89, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:32.53', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(90, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:32.63', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(91, N'manager', N'Đăng nhập thất bại', N'2026-10-19 20:40:32.74', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(92, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:32.813', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(93, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:32.82', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(98, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:33.0', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(100, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 20:40:33.01', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(103, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:04:12.08', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(104, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:04:12.163', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(105, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:04:12.237', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(106, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:04:12.307', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(107, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:04:12.397', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(108, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:12.627', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(109, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:12.72', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(110, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:12.78', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(111, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:04:12.887', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(112, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:12.957', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(113, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:12.963', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(118, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:13.09', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(120, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:04:13.1', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(123, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:05:44.297', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(124, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:05:44.37', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(125, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:05:44.447', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(126, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:05:44.52', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(127, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:05:44.593', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(128, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:05:44.917', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(129, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:05:45.043', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(130, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:05:45.137', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(131, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:05:45.277', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(132, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:06:18.693', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(133, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:06:18.76', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(134, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:06:18.82', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(135, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:06:18.89', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(136, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:06:18.95', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(137, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.167', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(138, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.253', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(139, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.307', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(140, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:06:19.41', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(141, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.493', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(142, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.5', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(147, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.64', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(149, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:06:19.65', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(152, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:09:38.11', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(153, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:09:38.173', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(154, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:09:38.237', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(155, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:09:38.307', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(156, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:09:38.367', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(157, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.527', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(158, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.587', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(159, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.627', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(160, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:09:38.72', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(161, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.777', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(162, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.78', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(167, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.89', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(169, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:09:38.9', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(172, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:15:34.103', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(173, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:15:34.17', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(174, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:15:34.233', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(175, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:15:34.297', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(176, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:15:34.357', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(177, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:15:34.557', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(178, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:15:34.64', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(179, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:15:34.683', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(180, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:15:34.767', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(181, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-19 21:16:10.843', N'T?i lên t?p sao luu test_unit_backup.sql (Dung lu?ng: 0.03 KB) t? máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(182, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-19 21:16:42.26', N'T?i lên t?p sao luu test_unit_backup.sql (Dung lu?ng: 0.03 KB) t? máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(183, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-19 21:16:42.41', N'T?o b?n sao luu backup_ute_sport_initial.sql (Dung lu?ng: 124.55 KB, 330 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(184, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-19 21:17:44.557', N'T?i lên t?p sao luu test_unit_backup.sql (Dung lu?ng: 0.03 KB) t? máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(185, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-19 21:17:44.69', N'T?o b?n sao luu backup_ute_sport_initial.sql (Dung lu?ng: 125.03 KB, 332 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(186, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:17:48.0', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(187, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:17:48.06', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(188, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:17:48.12', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(189, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:17:48.18', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(190, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:17:48.24', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(191, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.387', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(192, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.45', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(193, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.487', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(194, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:17:48.567', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(195, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.617', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(196, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.62', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(201, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.717', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(203, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:17:48.727', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(206, N'user_reset_1792420163365', N'Đăng nhập hệ thống', N'2026-10-19 21:29:23.593', N'Xác th?c tài kho?n [user_reset_1792420163365] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(207, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:29:23.867', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(208, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:29:23.937', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(209, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:29:24.0', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(210, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:29:24.063', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(211, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:29:24.14', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(212, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:29:24.363', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(213, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:29:24.47', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(214, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:29:24.54', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(215, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:29:24.653', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(216, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-19 21:29:47.483', N'T?i lên t?p sao luu test_unit_backup.sql (Dung lu?ng: 0.03 KB) t? máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(217, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-19 21:29:47.633', N'T?o b?n sao luu backup_ute_sport_initial.sql (Dung lu?ng: 131.00 KB, 359 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(218, N'user_reset_1792420190804', N'Đăng nhập hệ thống', N'2026-10-19 21:29:50.97', N'Xác th?c tài kho?n [user_reset_1792420190804] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(219, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:29:51.21', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(220, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:29:51.27', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(221, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:29:51.333', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(222, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:29:51.407', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(223, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:29:51.477', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(224, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:29:51.617', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(225, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:29:51.673', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(226, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:29:51.713', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(227, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:29:51.797', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(228, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:29:51.843', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(229, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:29:51.847', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(234, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:29:51.943', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(236, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:29:51.953', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(239, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:39:34.343', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(241, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:39:34.797', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(242, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:39:34.92', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(243, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-19 21:39:35.103', N'T?o b?n sao luu backup_tc50_test_1792420775025.sql (Dung lu?ng: 135.71 KB, 380 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(244, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:39:35.263', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(245, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:39:35.357', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(246, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:39:35.37', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(247, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:39:35.527', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(248, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:39:35.633', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(249, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-19 21:39:35.68', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(250, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:39:35.863', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(251, N'admin', N'Đăng nhập thất bại', N'2026-10-19 21:39:35.937', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(252, N'tc12_user_1792420776007', N'Đăng nhập hệ thống', N'2026-10-19 21:39:36.14', N'Xác th?c tài kho?n [tc12_user_1792420776007] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(253, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:41:57.02', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(255, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:41:57.503', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(256, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:41:57.633', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(257, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-19 21:41:57.81', N'T?o b?n sao luu backup_tc50_test_1792420917738.sql (Dung lu?ng: 138.85 KB, 394 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(258, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:41:57.977', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(259, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:41:58.077', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(260, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:41:58.09', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(261, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:41:58.233', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(262, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:41:58.337', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(263, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-19 21:41:58.377', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(264, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:41:58.57', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(265, N'admin', N'Đăng nhập thất bại', N'2026-10-19 21:41:58.647', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(266, N'tc12_user_1792420918698', N'Đăng nhập hệ thống', N'2026-10-19 21:41:58.84', N'Xác th?c tài kho?n [tc12_user_1792420918698] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(267, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:16.747', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(269, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:49:17.197', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(270, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:49:17.34', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(271, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-19 21:49:17.533', N'T?o b?n sao luu backup_tc50_test_1792421357454.sql (Dung lu?ng: 141.83 KB, 408 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(272, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:49:17.71', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(273, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:17.813', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(274, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:17.827', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(275, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:49:18.083', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(276, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:49:18.193', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(277, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-19 21:49:18.23', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(278, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:18.29', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(279, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:18.437', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(280, N'admin', N'Đăng nhập thất bại', N'2026-10-19 21:49:18.517', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(281, N'tc12_user_1792421358568', N'Đăng nhập hệ thống', N'2026-10-19 21:49:18.707', N'Xác th?c tài kho?n [tc12_user_1792421358568] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(282, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:40.743', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(284, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:49:41.153', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(285, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:49:41.27', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(286, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-19 21:49:41.403', N'T?o b?n sao luu backup_tc50_test_1792421381360.sql (Dung lu?ng: 145.84 KB, 428 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(287, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:49:41.567', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(288, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:41.64', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(289, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:41.65', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(290, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:49:41.87', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(291, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:49:41.967', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(292, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-19 21:49:42.0', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(293, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:42.07', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(294, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:42.213', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(295, N'admin', N'Đăng nhập thất bại', N'2026-10-19 21:49:42.287', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(296, N'tc12_user_1792421382339', N'Đăng nhập hệ thống', N'2026-10-19 21:49:42.48', N'Xác th?c tài kho?n [tc12_user_1792421382339] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(297, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-19 21:49:45.243', N'T?i lên t?p sao luu test_unit_backup.sql (Dung lu?ng: 0.03 KB) t? máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(298, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-19 21:49:45.337', N'T?o b?n sao luu backup_ute_sport_initial.sql (Dung lu?ng: 149.16 KB, 444 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(299, N'user_reset_1792421389521', N'Đăng nhập hệ thống', N'2026-10-19 21:49:49.657', N'Xác th?c tài kho?n [user_reset_1792421389521] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(300, N'director', N'Đăng nhập hệ thống', N'2026-10-19 21:49:49.857', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(301, N'manager', N'Đăng nhập hệ thống', N'2026-10-19 21:49:49.92', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(302, N'pos', N'Đăng nhập hệ thống', N'2026-10-19 21:49:49.977', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(303, N'admin', N'Đăng nhập hệ thống', N'2026-10-19 21:49:50.04', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(304, N'customer', N'Đăng nhập hệ thống', N'2026-10-19 21:49:50.1', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(305, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:50.19', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(306, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:50.243', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(307, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:50.28', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(308, N'manager', N'Đăng nhập thất bại', N'2026-10-19 21:49:50.36', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(309, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:50.417', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(310, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:50.42', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(315, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:50.517', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(317, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-19 21:49:50.53', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(320, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:34:44.217', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(322, N'director', N'Đăng nhập hệ thống', N'2026-10-20 17:34:45.03', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(323, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 17:34:45.203', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(324, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 17:34:45.69', N'T?o b?n sao luu backup_tc50_test_1792492485505.sql (Dung lu?ng: 153.68 KB, 465 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(325, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 17:34:46.01', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(326, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:34:46.187', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(327, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:34:46.24', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(328, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 17:34:46.647', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(329, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 17:34:46.77', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(330, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 17:34:46.83', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(331, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:34:46.92', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(332, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:34:47.24', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(333, N'admin', N'Đăng nhập thất bại', N'2026-10-20 17:34:47.32', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(334, N'tc12_user_1792492487408', N'Đăng nhập hệ thống', N'2026-10-20 17:34:47.593', N'Xác th?c tài kho?n [tc12_user_1792492487408] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(335, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:38:07.837', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(337, N'director', N'Đăng nhập hệ thống', N'2026-10-20 17:38:08.547', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(338, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 17:38:08.727', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(339, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 17:38:09.06', N'T?o b?n sao luu backup_tc50_test_1792492688981.sql (Dung lu?ng: 157.69 KB, 485 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL);
INSERT INTO audit_logs (log_id, username, action_type, action_time, details, ip_address, module, status, user_code) VALUES
(340, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 17:38:09.347', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(341, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:38:09.567', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(342, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:38:09.603', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(343, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 17:38:10.003', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(344, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 17:38:10.137', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(345, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 17:38:10.203', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(346, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:38:10.28', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(347, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:38:10.593', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(348, N'admin', N'Đăng nhập thất bại', N'2026-10-20 17:38:10.697', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(349, N'tc12_user_1792492690749', N'Đăng nhập hệ thống', N'2026-10-20 17:38:10.897', N'Xác th?c tài kho?n [tc12_user_1792492690749] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(350, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:44:51.33', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(352, N'director', N'Đăng nhập hệ thống', N'2026-10-20 17:44:51.847', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(353, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 17:44:51.987', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(354, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 17:44:52.213', N'T?o b?n sao luu backup_tc50_test_1792493092109.sql (Dung lu?ng: 161.81 KB, 505 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(355, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 17:44:52.407', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(356, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:44:52.523', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(357, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:44:52.537', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(358, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 17:44:52.797', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(359, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 17:44:52.91', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(360, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 17:44:52.957', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(361, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:44:53.03', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(362, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:44:53.23', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(363, N'admin', N'Đăng nhập thất bại', N'2026-10-20 17:44:53.307', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(364, N'tc12_user_1792493093373', N'Đăng nhập hệ thống', N'2026-10-20 17:44:53.52', N'Xác th?c tài kho?n [tc12_user_1792493093373] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(365, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:47:54.72', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(367, N'director', N'Đăng nhập hệ thống', N'2026-10-20 17:47:55.2', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(368, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 17:47:55.327', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(369, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 17:47:55.48', N'T?o b?n sao luu backup_tc50_test_1792493275425.sql (Dung lu?ng: 165.69 KB, 524 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(370, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 17:47:55.65', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(371, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:47:55.74', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(372, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:47:55.75', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(373, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 17:47:55.97', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(374, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 17:47:56.073', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(375, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 17:47:56.107', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(376, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:47:56.163', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(377, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:47:56.3', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(378, N'admin', N'Đăng nhập thất bại', N'2026-10-20 17:47:56.377', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(379, N'tc12_user_1792493276424', N'Đăng nhập hệ thống', N'2026-10-20 17:47:56.56', N'Xác th?c tài kho?n [tc12_user_1792493276424] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(380, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:49:06.773', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(382, N'director', N'Đăng nhập hệ thống', N'2026-10-20 17:49:07.45', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(383, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 17:49:07.633', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(384, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 17:49:08.057', N'T?o b?n sao luu backup_tc50_test_1792493347905.sql (Dung lu?ng: 169.69 KB, 544 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(385, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 17:49:08.303', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(386, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:49:08.47', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(387, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:49:08.49', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(388, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 17:49:08.83', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(389, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 17:49:08.95', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(390, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 17:49:09.0', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(391, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:49:09.07', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(392, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:49:09.32', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(393, N'admin', N'Đăng nhập thất bại', N'2026-10-20 17:49:09.41', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(394, N'tc12_user_1792493349513', N'Đăng nhập hệ thống', N'2026-10-20 17:49:09.673', N'Xác th?c tài kho?n [tc12_user_1792493349513] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(395, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:56:32.37', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(397, N'director', N'Đăng nhập hệ thống', N'2026-10-20 17:56:33.143', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(398, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 17:56:33.333', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(399, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 17:56:33.75', N'T?o b?n sao luu backup_tc50_test_1792493793539.sql (Dung lu?ng: 173.69 KB, 564 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(400, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 17:56:34.093', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(401, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:56:34.243', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(402, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:56:34.26', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(403, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 17:56:34.58', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(404, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 17:56:34.753', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(405, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 17:56:34.8', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(406, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:56:34.903', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(407, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 17:56:35.133', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(408, N'admin', N'Đăng nhập thất bại', N'2026-10-20 17:56:35.22', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(409, N'tc12_user_1792493795349', N'Đăng nhập hệ thống', N'2026-10-20 17:56:35.513', N'Xác th?c tài kho?n [tc12_user_1792493795349] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(410, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:07:18.22', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(412, N'director', N'Đăng nhập hệ thống', N'2026-10-20 18:07:18.643', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(413, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 18:07:18.763', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(414, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 18:07:18.943', N'T?o b?n sao luu backup_tc50_test_1792494438852.sql (Dung lu?ng: 177.69 KB, 584 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(415, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 18:07:19.107', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(416, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:07:19.187', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(417, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:07:19.197', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(418, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 18:07:19.417', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(419, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 18:07:19.513', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(420, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 18:07:19.573', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(421, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:07:19.633', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(422, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:07:19.783', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(423, N'admin', N'Đăng nhập thất bại', N'2026-10-20 18:07:19.857', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(424, N'tc12_user_1792494439907', N'Đăng nhập hệ thống', N'2026-10-20 18:07:20.043', N'Xác th?c tài kho?n [tc12_user_1792494439907] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(425, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:20:23.437', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(427, N'director', N'Đăng nhập hệ thống', N'2026-10-20 18:20:23.897', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(428, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 18:20:24.027', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(429, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 18:20:24.213', N'T?o b?n sao luu backup_tc50_test_1792495224127.sql (Dung lu?ng: 181.71 KB, 604 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(430, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 18:20:24.4', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(431, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:20:24.507', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(432, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:20:24.52', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(433, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 18:20:24.75', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(434, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 18:20:24.847', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(435, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 18:20:24.887', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(436, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:20:24.943', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(437, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:20:25.08', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(438, N'admin', N'Đăng nhập thất bại', N'2026-10-20 18:20:25.16', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(439, N'tc12_user_1792495225347', N'Đăng nhập hệ thống', N'2026-10-20 18:20:25.487', N'Xác th?c tài kho?n [tc12_user_1792495225347] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(440, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:26:00.403', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(442, N'director', N'Đăng nhập hệ thống', N'2026-10-20 18:26:00.897', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(443, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 18:26:01.04', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(444, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 18:26:01.217', N'T?o b?n sao luu backup_tc50_test_1792495561134.sql (Dung lu?ng: 185.71 KB, 624 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(445, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 18:26:01.393', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(446, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:26:01.483', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(447, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:26:01.497', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(448, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 18:26:01.74', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(449, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 18:26:01.857', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(450, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 18:26:01.893', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(451, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:26:01.953', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(452, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:26:02.103', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(453, N'admin', N'Đăng nhập thất bại', N'2026-10-20 18:26:02.18', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(454, N'tc12_user_1792495562261', N'Đăng nhập hệ thống', N'2026-10-20 18:26:02.417', N'Xác th?c tài kho?n [tc12_user_1792495562261] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(455, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:41:40.357', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(457, N'director', N'Đăng nhập hệ thống', N'2026-10-20 18:41:40.8', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(458, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 18:41:40.93', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(459, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 18:41:41.113', N'T?o b?n sao luu backup_tc50_test_1792496501028.sql (Dung lu?ng: 189.71 KB, 644 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(460, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 18:41:41.287', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(461, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:41:41.37', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(462, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:41:41.383', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(463, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 18:41:41.61', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(464, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 18:41:41.707', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(465, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 18:41:41.743', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(466, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:41:41.803', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(467, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:41:41.953', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(468, N'admin', N'Đăng nhập thất bại', N'2026-10-20 18:41:42.03', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(469, N'tc12_user_1792496502083', N'Đăng nhập hệ thống', N'2026-10-20 18:41:42.22', N'Xác th?c tài kho?n [tc12_user_1792496502083] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(470, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:48:02.51', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(472, N'director', N'Đăng nhập hệ thống', N'2026-10-20 18:48:02.96', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(473, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 18:48:03.083', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(474, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 18:48:03.273', N'T?o b?n sao luu backup_tc50_test_1792496883193.sql (Dung lu?ng: 193.72 KB, 664 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(475, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 18:48:03.45', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(476, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:48:03.533', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(477, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:48:03.547', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(478, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 18:48:03.783', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(479, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 18:48:03.887', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(480, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 18:48:03.933', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(481, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:48:03.993', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(482, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:48:04.14', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(483, N'admin', N'Đăng nhập thất bại', N'2026-10-20 18:48:04.22', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(484, N'tc12_user_1792496884274', N'Đăng nhập hệ thống', N'2026-10-20 18:48:04.413', N'Xác th?c tài kho?n [tc12_user_1792496884274] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(485, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:58:16.18', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(487, N'director', N'Đăng nhập hệ thống', N'2026-10-20 18:58:16.647', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(488, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 18:58:16.77', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(489, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 18:58:16.963', N'T?o b?n sao luu backup_tc50_test_1792497496879.sql (Dung lu?ng: 197.72 KB, 684 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(490, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 18:58:17.147', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(491, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:58:17.237', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(492, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:58:17.25', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(493, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 18:58:17.483', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(494, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 18:58:17.587', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(495, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 18:58:17.627', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(496, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:58:17.687', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(497, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 18:58:17.83', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(498, N'admin', N'Đăng nhập thất bại', N'2026-10-20 18:58:17.907', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(499, N'tc12_user_1792497497972', N'Đăng nhập hệ thống', N'2026-10-20 18:58:18.117', N'Xác th?c tài kho?n [tc12_user_1792497497972] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(500, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:05:48.523', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(502, N'director', N'Đăng nhập hệ thống', N'2026-10-20 19:05:49.047', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(503, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 19:05:49.187', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(504, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 19:05:49.407', N'T?o b?n sao luu backup_tc50_test_1792497949307.sql (Dung lu?ng: 201.73 KB, 704 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(505, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 19:05:49.597', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(506, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:05:49.803', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(507, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:05:49.817', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(508, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 19:05:50.047', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(509, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 19:05:50.153', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(510, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 19:05:50.19', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(511, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:05:50.257', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(512, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:05:50.403', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(513, N'admin', N'Đăng nhập thất bại', N'2026-10-20 19:05:50.48', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(514, N'tc12_user_1792497950535', N'Đăng nhập hệ thống', N'2026-10-20 19:05:50.677', N'Xác th?c tài kho?n [tc12_user_1792497950535] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(515, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:13:45.657', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(517, N'director', N'Đăng nhập hệ thống', N'2026-10-20 19:13:46.117', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(518, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 19:13:46.243', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(519, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 19:13:46.447', N'T?o b?n sao luu backup_tc50_test_1792498426355.sql (Dung lu?ng: 205.74 KB, 724 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(520, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 19:13:46.62', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(521, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:13:46.703', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(522, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:13:46.717', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(523, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 19:13:46.953', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(524, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 19:13:47.057', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(525, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 19:13:47.097', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(526, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:13:47.157', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(527, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:13:47.313', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(528, N'admin', N'Đăng nhập thất bại', N'2026-10-20 19:13:47.387', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(529, N'tc12_user_1792498427444', N'Đăng nhập hệ thống', N'2026-10-20 19:13:47.583', N'Xác th?c tài kho?n [tc12_user_1792498427444] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(530, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:24:32.727', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(532, N'director', N'Đăng nhập hệ thống', N'2026-10-20 19:24:33.21', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(533, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 19:24:33.347', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(534, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 19:24:33.543', N'T?o b?n sao luu backup_tc50_test_1792499073449.sql (Dung lu?ng: 209.75 KB, 744 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(535, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 19:24:33.737', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(536, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:24:33.843', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(537, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:24:33.857', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(538, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 19:24:34.09', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(539, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 19:24:34.187', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(540, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 19:24:34.227', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(541, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:24:34.29', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(542, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:24:34.443', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(543, N'admin', N'Đăng nhập thất bại', N'2026-10-20 19:24:34.52', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(544, N'tc12_user_1792499074579', N'Đăng nhập hệ thống', N'2026-10-20 19:24:34.717', N'Xác th?c tài kho?n [tc12_user_1792499074579] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(545, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:29:33.05', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(547, N'director', N'Đăng nhập hệ thống', N'2026-10-20 19:29:33.527', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(548, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 19:29:33.647', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(549, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 19:29:33.833', N'T?o b?n sao luu backup_tc50_test_1792499373749.sql (Dung lu?ng: 213.75 KB, 764 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(550, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 19:29:34.007', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(551, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:29:34.1', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(552, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:29:34.11', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(553, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 19:29:34.337', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(554, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 19:29:34.447', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(555, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 19:29:34.483', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(556, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:29:34.54', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(557, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:29:34.703', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(558, N'admin', N'Đăng nhập thất bại', N'2026-10-20 19:29:34.777', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(559, N'tc12_user_1792499374842', N'Đăng nhập hệ thống', N'2026-10-20 19:29:34.987', N'Xác th?c tài kho?n [tc12_user_1792499374842] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(560, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:46:16.41', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(562, N'director', N'Đăng nhập hệ thống', N'2026-10-20 19:46:16.927', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(563, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 19:46:17.06', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(564, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 19:46:17.277', N'T?o b?n sao luu backup_tc50_test_1792500377183.sql (Dung lu?ng: 217.76 KB, 784 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(565, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 19:46:17.467', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(566, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:46:17.587', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(567, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:46:17.6', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(568, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 19:46:17.857', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(569, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 19:46:18.003', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(570, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 19:46:18.05', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(571, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:46:18.15', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(572, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 19:46:18.333', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(573, N'admin', N'Đăng nhập thất bại', N'2026-10-20 19:46:18.42', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(574, N'tc12_user_1792500378495', N'Đăng nhập hệ thống', N'2026-10-20 19:46:18.647', N'Xác th?c tài kho?n [tc12_user_1792500378495] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(575, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 20:05:32.367', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(577, N'director', N'Đăng nhập hệ thống', N'2026-10-20 20:05:32.823', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(578, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 20:05:32.943', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(579, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 20:05:33.127', N'T?o b?n sao luu backup_tc50_test_1792501533044.sql (Dung lu?ng: 221.77 KB, 804 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(580, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 20:05:33.297', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(581, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 20:05:33.38', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(582, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 20:05:33.39', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(583, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 20:05:33.62', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(584, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 20:05:33.723', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(585, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 20:05:33.767', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(586, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 20:05:33.833', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(587, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 20:05:33.977', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(588, N'admin', N'Đăng nhập thất bại', N'2026-10-20 20:05:34.053', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(589, N'tc12_user_1792501534113', N'Đăng nhập hệ thống', N'2026-10-20 20:05:34.25', N'Xác th?c tài kho?n [tc12_user_1792501534113] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(590, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 20:17:33.57', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'0:0:0:0:0:0:0:1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(591, N'admin', N'Cập nhật cấu hình thông số hệ thống', N'2026-10-20 20:18:06.333', N'C?p nh?t quy d?nh d?t c?c, thông s? SMTP email và chính sách tích di?m', N'127.0.0.1', N'Phân hệ Quản trị', N'Thành công', NULL),
(592, N'tho8189@gmail.com', N'Đăng nhập hệ thống', N'2026-10-20 20:23:30.52', N'Xác th?c tài kho?n [tho8189@gmail.com] thành công qua c?ng b?o m?t', N'0:0:0:0:0:0:0:1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(593, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 20:24:49.597', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(595, N'director', N'Đăng nhập hệ thống', N'2026-10-20 20:24:50.193', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(596, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 20:24:50.343', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(597, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 20:24:50.597', N'T?o b?n sao luu backup_tc50_test_1792502690484.sql (Dung lu?ng: 222.64 KB, 841 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(598, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 20:24:50.797', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(599, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 20:24:50.933', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(600, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 20:24:50.947', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(601, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 20:24:51.2', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(602, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 20:24:51.327', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(603, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 20:24:51.367', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(604, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 20:24:51.48', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(605, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 20:24:51.683', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(606, N'admin', N'Đăng nhập thất bại', N'2026-10-20 20:24:51.76', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL);
INSERT INTO audit_logs (log_id, username, action_type, action_time, details, ip_address, module, status, user_code) VALUES
(607, N'tc12_user_1792502691856', N'Đăng nhập hệ thống', N'2026-10-20 20:24:52.007', N'Xác th?c tài kho?n [tc12_user_1792502691856] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(608, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 21:08:56.18', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'0:0:0:0:0:0:0:1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(609, N'director', N'Đăng nhập hệ thống', N'2026-10-20 21:09:45.173', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'0:0:0:0:0:0:0:1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(610, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:22:09.26', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(612, N'director', N'Đăng nhập hệ thống', N'2026-10-20 22:22:09.783', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(613, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 22:22:09.913', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(614, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 22:22:10.103', N'T?o b?n sao luu backup_tc50_test_1792509730019.sql (Dung lu?ng: 227.29 KB, 863 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(615, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 22:22:10.273', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(616, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:22:10.36', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(617, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:22:10.37', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(618, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 22:22:10.597', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(619, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 22:22:10.703', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(620, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 22:22:10.74', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(621, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:22:10.8', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(622, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:22:10.943', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(623, N'admin', N'Đăng nhập thất bại', N'2026-10-20 22:22:11.017', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(624, N'tc12_user_1792509731063', N'Đăng nhập hệ thống', N'2026-10-20 22:22:11.197', N'Xác th?c tài kho?n [tc12_user_1792509731063] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(625, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:26:49.487', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(627, N'director', N'Đăng nhập hệ thống', N'2026-10-20 22:26:49.96', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(628, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 22:26:50.083', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(629, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 22:26:50.317', N'T?o b?n sao luu backup_tc50_test_1792510010203.sql (Dung lu?ng: 231.15 KB, 883 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(630, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 22:26:50.513', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(631, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:26:50.61', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(632, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:26:50.62', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(633, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 22:26:50.877', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(634, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 22:26:50.983', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(635, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 22:26:51.02', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(636, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:26:51.08', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(637, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:26:51.21', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(638, N'admin', N'Đăng nhập thất bại', N'2026-10-20 22:26:51.287', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(639, N'tc12_user_1792510011336', N'Đăng nhập hệ thống', N'2026-10-20 22:26:51.473', N'Xác th?c tài kho?n [tc12_user_1792510011336] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(640, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-20 22:26:54.0', N'T?i lên t?p sao luu test_unit_backup.sql (Dung lu?ng: 0.03 KB) t? máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(641, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 22:26:54.117', N'T?o b?n sao luu backup_ute_sport_initial.sql (Dung lu?ng: 234.34 KB, 899 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(642, N'user_reset_1792510018387', N'Đăng nhập hệ thống', N'2026-10-20 22:26:58.553', N'Xác th?c tài kho?n [user_reset_1792510018387] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(643, N'director', N'Đăng nhập hệ thống', N'2026-10-20 22:26:58.81', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(644, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 22:26:58.877', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(645, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 22:26:58.94', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(646, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 22:26:59.0', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(647, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 22:26:59.057', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(648, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:26:59.177', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(649, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:26:59.243', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(650, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:26:59.29', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(651, N'manager', N'Đăng nhập thất bại', N'2026-10-20 22:26:59.38', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(652, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:26:59.43', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(653, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:26:59.44', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(658, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:27:00.03', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(660, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:27:00.043', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(663, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:29:57.017', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(665, N'director', N'Đăng nhập hệ thống', N'2026-10-20 22:29:57.467', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(666, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 22:29:57.607', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(667, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 22:29:57.807', N'T?o b?n sao luu backup_tc50_test_1792510197714.sql (Dung lu?ng: 238.67 KB, 920 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(668, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 22:29:57.99', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(669, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:29:58.083', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(670, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:29:58.097', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(671, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 22:29:58.337', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(672, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 22:29:58.45', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(673, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 22:29:58.483', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(674, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:29:58.54', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(675, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:29:58.673', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(676, N'admin', N'Đăng nhập thất bại', N'2026-10-20 22:29:58.747', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(677, N'tc12_user_1792510198799', N'Đăng nhập hệ thống', N'2026-10-20 22:29:58.937', N'Xác th?c tài kho?n [tc12_user_1792510198799] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(678, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-20 22:30:01.783', N'T?i lên t?p sao luu test_unit_backup.sql (Dung lu?ng: 0.03 KB) t? máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(679, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 22:30:01.883', N'T?o b?n sao luu backup_ute_sport_initial.sql (Dung lu?ng: 241.87 KB, 936 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(680, N'user_reset_1792510205846', N'Đăng nhập hệ thống', N'2026-10-20 22:30:05.993', N'Xác th?c tài kho?n [user_reset_1792510205846] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(681, N'director', N'Đăng nhập hệ thống', N'2026-10-20 22:30:06.18', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(682, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 22:30:06.243', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(683, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 22:30:06.307', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(684, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 22:30:06.37', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(685, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 22:30:06.43', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(686, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:30:06.533', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(687, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:30:06.597', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(688, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:30:06.627', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(689, N'manager', N'Đăng nhập thất bại', N'2026-10-20 22:30:06.71', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(690, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:30:06.757', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(691, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:30:06.767', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(696, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:30:06.873', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(698, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:30:06.887', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(701, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:41:13.31', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(703, N'director', N'Đăng nhập hệ thống', N'2026-10-20 22:41:13.843', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(704, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 22:41:13.967', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(705, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 22:41:14.187', N'T?o b?n sao luu backup_tc50_test_1792510874088.sql (Dung lu?ng: 246.20 KB, 957 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(706, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 22:41:14.39', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(707, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:41:14.563', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(708, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:41:14.58', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(709, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 22:41:14.917', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(710, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 22:41:15.013', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(711, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 22:41:15.06', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(712, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:41:15.133', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(713, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:41:15.297', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(714, N'admin', N'Đăng nhập thất bại', N'2026-10-20 22:41:15.383', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(715, N'tc12_user_1792510875465', N'Đăng nhập hệ thống', N'2026-10-20 22:41:15.613', N'Xác th?c tài kho?n [tc12_user_1792510875465] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(716, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:49:19.037', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(718, N'director', N'Đăng nhập hệ thống', N'2026-10-20 22:49:19.463', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(719, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 22:49:19.593', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(720, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 22:49:19.873', N'T?o b?n sao luu backup_tc50_test_1792511359756.sql (Dung lu?ng: 250.06 KB, 977 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(721, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 22:49:20.047', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(722, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:49:20.14', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(723, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:49:20.15', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(724, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 22:49:20.36', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(725, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 22:49:20.477', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(726, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 22:49:20.517', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(727, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:49:20.593', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(728, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:49:20.717', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(729, N'admin', N'Đăng nhập thất bại', N'2026-10-20 22:49:20.787', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(730, N'tc12_user_1792511360831', N'Đăng nhập hệ thống', N'2026-10-20 22:49:20.96', N'Xác th?c tài kho?n [tc12_user_1792511360831] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(731, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-20 22:49:23.493', N'T?i lên t?p sao luu test_unit_backup.sql (Dung lu?ng: 0.03 KB) t? máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(732, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 22:49:23.58', N'T?o b?n sao luu backup_ute_sport_initial.sql (Dung lu?ng: 253.25 KB, 993 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(733, N'user_reset_1792511367139', N'Đăng nhập hệ thống', N'2026-10-20 22:49:27.283', N'Xác th?c tài kho?n [user_reset_1792511367139] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(734, N'director', N'Đăng nhập hệ thống', N'2026-10-20 22:49:27.507', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(735, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 22:49:27.57', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(736, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 22:49:27.633', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(737, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 22:49:27.687', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(738, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 22:49:27.743', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(739, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:49:27.827', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(740, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:49:27.883', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(741, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:49:27.91', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(742, N'manager', N'Đăng nhập thất bại', N'2026-10-20 22:49:27.99', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(743, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:49:28.12', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(744, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:49:28.123', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(749, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:49:28.22', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(751, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:49:28.23', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(754, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:55:40.567', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(756, N'director', N'Đăng nhập hệ thống', N'2026-10-20 22:55:41.08', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(757, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 22:55:41.207', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(758, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 22:55:41.41', N'T?o b?n sao luu backup_tc50_test_1792511741315.sql (Dung lu?ng: 257.59 KB, 1014 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(759, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 22:55:41.59', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(760, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:55:41.67', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(761, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:55:41.683', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(762, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 22:55:41.907', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(763, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 22:55:42.007', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(764, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 22:55:42.043', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(765, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:55:42.11', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(766, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:55:42.257', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(767, N'admin', N'Đăng nhập thất bại', N'2026-10-20 22:55:42.33', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(768, N'tc12_user_1792511742383', N'Đăng nhập hệ thống', N'2026-10-20 22:55:42.52', N'Xác th?c tài kho?n [tc12_user_1792511742383] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(769, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-20 22:55:45.273', N'T?i lên t?p sao luu test_unit_backup.sql (Dung lu?ng: 0.03 KB) t? máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(770, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 22:55:45.37', N'T?o b?n sao luu backup_ute_sport_initial.sql (Dung lu?ng: 260.78 KB, 1030 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(771, N'user_reset_1792511748968', N'Đăng nhập hệ thống', N'2026-10-20 22:55:49.11', N'Xác th?c tài kho?n [user_reset_1792511748968] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(772, N'director', N'Đăng nhập hệ thống', N'2026-10-20 22:55:49.327', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(773, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 22:55:49.387', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(774, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 22:55:49.443', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(775, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 22:55:49.5', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(776, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 22:55:49.557', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(777, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:55:49.653', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(778, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:55:49.71', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(779, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:55:49.74', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(780, N'manager', N'Đăng nhập thất bại', N'2026-10-20 22:55:49.83', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(781, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:55:49.873', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(782, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:55:49.88', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(787, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:55:49.983', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(789, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 22:55:49.993', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(792, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:00:40.923', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(794, N'director', N'Đăng nhập hệ thống', N'2026-10-20 23:00:41.387', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(795, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 23:00:41.51', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(796, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 23:00:41.707', N'T?o b?n sao luu backup_tc50_test_1792512041615.sql (Dung lu?ng: 265.11 KB, 1051 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(797, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 23:00:41.92', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(798, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:00:42.02', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(799, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:00:42.033', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(800, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 23:00:42.253', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(801, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 23:00:42.36', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(802, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 23:00:42.39', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(803, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:00:42.457', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(804, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:00:42.623', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(805, N'admin', N'Đăng nhập thất bại', N'2026-10-20 23:00:42.7', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(806, N'tc12_user_1792512042752', N'Đăng nhập hệ thống', N'2026-10-20 23:00:42.883', N'Xác th?c tài kho?n [tc12_user_1792512042752] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(807, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-20 23:00:45.717', N'T?i lên t?p sao luu test_unit_backup.sql (Dung lu?ng: 0.03 KB) t? máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(808, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 23:00:45.803', N'T?o b?n sao luu backup_ute_sport_initial.sql (Dung lu?ng: 268.30 KB, 1067 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(809, N'user_reset_1792512049755', N'Đăng nhập hệ thống', N'2026-10-20 23:00:49.907', N'Xác th?c tài kho?n [user_reset_1792512049755] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(810, N'director', N'Đăng nhập hệ thống', N'2026-10-20 23:00:50.117', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(811, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 23:00:50.183', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(812, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 23:00:50.25', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(813, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 23:00:50.313', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(814, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 23:00:50.373', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(815, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:00:50.453', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(816, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:00:50.503', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(817, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:00:50.55', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(818, N'manager', N'Đăng nhập thất bại', N'2026-10-20 23:00:50.637', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(819, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:00:50.68', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(820, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:00:50.683', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(825, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:00:50.773', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(827, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:00:50.78', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(830, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:09:53.07', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(832, N'director', N'Đăng nhập hệ thống', N'2026-10-20 23:09:53.587', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(833, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 23:09:53.723', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(834, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 23:09:54.033', N'T?o b?n sao luu backup_tc50_test_1792512593875.sql (Dung lu?ng: 272.64 KB, 1088 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(835, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 23:09:54.24', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(836, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:09:54.373', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(837, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:09:54.39', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(838, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 23:09:54.687', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(839, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 23:09:54.827', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(840, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 23:09:54.87', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(841, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:09:54.967', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(842, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:09:55.163', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(843, N'admin', N'Đăng nhập thất bại', N'2026-10-20 23:09:55.24', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(844, N'tc12_user_1792512595317', N'Đăng nhập hệ thống', N'2026-10-20 23:09:55.46', N'Xác th?c tài kho?n [tc12_user_1792512595317] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(845, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-20 23:09:58.143', N'T?i lên t?p sao luu test_unit_backup.sql (Dung lu?ng: 0.03 KB) t? máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(846, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 23:09:58.3', N'T?o b?n sao luu backup_ute_sport_initial.sql (Dung lu?ng: 275.83 KB, 1104 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(847, N'user_reset_1792512602072', N'Đăng nhập hệ thống', N'2026-10-20 23:10:02.233', N'Xác th?c tài kho?n [user_reset_1792512602072] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(848, N'director', N'Đăng nhập hệ thống', N'2026-10-20 23:10:02.47', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(849, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 23:10:02.53', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(850, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 23:10:02.59', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(851, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 23:10:02.65', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(852, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 23:10:02.71', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(853, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:10:02.85', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðã d?t tru?c] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(854, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:10:02.907', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðã d?t tru?c] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(855, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:10:02.937', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(856, N'manager', N'Đăng nhập thất bại', N'2026-10-20 23:10:03.023', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(857, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:10:03.067', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [B?o trì]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(858, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:10:03.07', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [B?o trì] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(863, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:10:03.18', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(865, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:10:03.193', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(868, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:13:57.183', N'Sân [CL02 - Sân c?u lông 2] chuy?n t? [Tr?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(870, N'director', N'Đăng nhập hệ thống', N'2026-10-20 23:13:57.593', N'Xác th?c tài kho?n [director] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(871, N'pos', N'Đăng nhập hệ thống', N'2026-10-20 23:13:57.747', N'Xác th?c tài kho?n [pos] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(872, N'admin', N'Sao lưu cơ sở dữ liệu', N'2026-10-20 23:13:57.94', N'T?o b?n sao luu backup_tc50_test_1792512837844.sql (Dung lu?ng: 280.17 KB, 1125 b?n ghi) thành công', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL),
(873, N'admin', N'Đăng nhập hệ thống', N'2026-10-20 23:13:58.117', N'Xác th?c tài kho?n [admin] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(874, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:13:58.213', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(875, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:13:58.223', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Tr?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(876, N'manager', N'Đăng nhập hệ thống', N'2026-10-20 23:13:58.443', N'Xác th?c tài kho?n [manager] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(877, N'customer', N'Đăng nhập hệ thống', N'2026-10-20 23:13:58.543', N'Xác th?c tài kho?n [customer] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(878, N'non_existent_user_xyz', N'Đăng nhập thất bại', N'2026-10-20 23:13:58.593', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(879, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:13:58.667', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Tr?ng] sang [Ðang s? d?ng]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(880, N'SYSTEM_COURT_MONITOR', N'Cập nhật trạng thái sân', N'2026-10-20 23:13:58.84', N'Sân [CL01 - Sân c?u lông 1 (VIP Yonex)] chuy?n t? [Ðang s? d?ng] sang [Ðã d?t tru?c]', N'127.0.0.1', N'Giám Sát Sân Bãi', N'Thành công', NULL),
(881, N'admin', N'Đăng nhập thất bại', N'2026-10-20 23:13:58.92', N'Ðang nh?p không thành công: Tài kho?n ho?c m?t kh?u không chính xác!', N'127.0.0.1', N'An ninh & Đăng nhập', N'Cảnh báo', NULL),
(882, N'tc12_user_1792512838975', N'Đăng nhập hệ thống', N'2026-10-20 23:13:59.12', N'Xác th?c tài kho?n [tc12_user_1792512838975] thành công qua c?ng b?o m?t', N'127.0.0.1', N'An ninh & Đăng nhập', N'Thành công', NULL),
(883, N'test_admin', N'Tải lên bản sao lưu', N'2026-10-20 23:14:01.853', N'T?i lên t?p sao luu test_unit_backup.sql (Dung lu?ng: 0.03 KB) t? máy tính', N'127.0.0.1', N'Hệ thống sao lưu', N'Thành công', NULL);
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

