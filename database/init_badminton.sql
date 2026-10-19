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
-- 5. CHÈN DỮ LIỆU MẪU ĐẦY ĐỦ CHO TẤT CẢ 25 BẢNG (SEED DATA)
-- =====================================================================

-- 5.1 DỮ LIỆU CHI NHÁNH TOÀN CHUỖI (branches)
INSERT INTO branches (branch_code, branch_name, address, phone, operating_hours, manager_name, total_courts, status) VALUES
('CN01', N'Chi nhánh 1 - Sân Cầu Lông Thủ Đức', N'Số 01 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức', '0903123456', N'06:00 - 23:00 hàng ngày', N'Trần Phúc Bảo', 8, N'Hoạt động'),
('CN02', N'Chi nhánh 2 - Sân Cầu Lông Bình Thạnh', N'Số 234 Điện Biên Phủ, Phường 25, Bình Thạnh', '0903333444', N'06:00 - 23:00 hàng ngày', N'Nguyễn Văn Quản', 6, N'Hoạt động'),
('CN03', N'Chi nhánh 3 - Sân Cầu Lông Quận 9', N'Số 70 Đỗ Xuân Hợp, Phước Long B, TP. Thủ Đức', '0903789012', N'06:00 - 23:00 hàng ngày', N'Trần Hữu Nam', 6, N'Hoạt động'),
('CN04', N'Chi nhánh 4 - Sân Cầu Lông Gò Vấp', N'Số 18 Quang Trung, Phường 10, Gò Vấp', '0903555777', N'06:00 - 23:00 hàng ngày', N'Lê Hoàng Khang', 6, N'Hoạt động'),
('ALL', N'Hội Sở / Ban Giám Đốc', N'Số 01 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức', '0901111222', N'08:00 - 17:00 hàng ngày', N'Võ Hoàng Nam', 0, N'Hoạt động');

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
('DS01_1001_1', 'KH001', N'Lê Bá Đạt', '0912345678', 'CL01', 'CN01', '2026-10-01', '08:00 - 10:00', 'KG01', 120000, 240000, 72000, N'Tiền mặt', N'Hoàn thành', N'Khách quen tập sáng'),
('DS01_1001_2', 'KH003', N'Phạm Gia Huy', '0988777666', 'CL03', 'CN01', '2026-10-01', '17:00 - 19:00', 'KG03', 90000, 180000, 54000, N'VietQR Pro', N'Hoàn thành', N'Đặt sân sau giờ làm'),
('DS01_1001_3', 'KH004', N'Vũ Thị Mai Anh', '0977555444', 'CL07', 'CN01', '2026-10-01', '19:00 - 21:00', 'KG03', 90000, 180000, 54000, N'Chuyển khoản VietQR', N'Hoàn thành', N'Đánh đôi nữ'),
('DS01_1002_1', 'KH002', N'Nguyễn Hoàng Nam', '0903123456', 'CL02', 'CN01', '2026-10-02', '09:00 - 11:00', 'KG02', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Tập thể lực'),
('DS01_1002_2', 'KH005', N'Trần Thanh Hằng', '0933444555', 'CL04', 'CN01', '2026-10-02', '18:00 - 20:00', 'KG03', 90000, 180000, 54000, N'VietQR Pro', N'Hoàn thành', N'CLB Thủ Đức'),
('DS01_1002_3', 'KH006', N'Bùi Khánh Linh', '0909111222', 'CL08', 'CN01', '2026-10-02', '20:00 - 22:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Giao lưu'),
('DS01_1003_1', 'KH001', N'Lê Bá Đạt', '0912345678', 'CL01', 'CN01', '2026-10-03', '15:00 - 17:00', 'KG02', 120000, 240000, 72000, N'Chuyển khoản VietQR', N'Hoàn thành', N'Tập đôi nam'),
('DS01_1003_2', 'KH003', N'Phạm Gia Huy', '0988777666', 'CL05', 'CN01', '2026-10-03', '17:00 - 19:00', 'KG03', 90000, 180000, 54000, N'VietQR Pro', N'Hoàn thành', N'Khách vãng lai'),
('DS01_1003_3', 'KH004', N'Vũ Thị Mai Anh', '0977555444', 'CL07', 'CN01', '2026-10-03', '19:00 - 21:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Đặt trực tiếp tại quầy'),
('DS01_1004_1', 'KH002', N'Nguyễn Hoàng Nam', '0903123456', 'CL02', 'CN01', '2026-10-04', '08:00 - 10:00', 'KG01', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Khách cố định'),
('DS01_1004_2', 'KH005', N'Trần Thanh Hằng', '0933444555', 'CL03', 'CN01', '2026-10-04', '10:00 - 12:00', 'KG02', 90000, 180000, 54000, N'VietQR Pro', N'Hoàn thành', N'Tập kỹ thuật'),
('DS01_1004_3', 'KH006', N'Bùi Khánh Linh', '0909111222', 'CL08', 'CN01', '2026-10-04', '16:00 - 18:00', 'KG02', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Giao lưu cuối tuần'),
('DS01_1005_3', 'KH004', N'Vũ Thị Mai Anh', '0977555444', 'CL06', 'CN01', '2026-10-05', '08:00 - 10:00', 'KG01', 120000, 240000, 72000, N'Tiền mặt', N'Hoàn thành', N'Trước khi sân bảo trì'),
('DS01_1005_1', 'KH001', N'Lê Bá Đạt', '0912345678', 'CL01', 'CN01', '2026-10-05', '18:00 - 20:00', 'KG03', 120000, 240000, 72000, N'VietQR Pro', N'Hoàn thành', N'Tối thứ 2'),
('DS01_1005_2', 'KH003', N'Phạm Gia Huy', '0988777666', 'CL04', 'CN01', '2026-10-05', '19:00 - 21:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Giao lưu nội bộ'),
('DS01_1006_1', 'KH002', N'Nguyễn Hoàng Nam', '0903123456', 'CL02', 'CN01', '2026-10-06', '17:00 - 19:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Tập phát cầu'),
('DS01_1006_2', 'KH005', N'Trần Thanh Hằng', '0933444555', 'CL05', 'CN01', '2026-10-06', '19:00 - 21:00', 'KG03', 90000, 180000, 54000, N'VietQR Pro', N'Hoàn thành', N'Khách hội viên'),
('DS01_1006_3', 'KH006', N'Bùi Khánh Linh', '0909111222', 'CL07', 'CN01', '2026-10-06', '20:00 - 22:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Trận chung kết mini'),
('DS01_1007_1', 'KH001', N'Lê Bá Đạt', '0912345678', 'CL01', 'CN01', '2026-10-07', '09:00 - 11:00', 'KG02', 120000, 240000, 72000, N'Chuyển khoản VietQR', N'Hoàn thành', N'Khách quen VIP'),
('DS01_1007_2', 'KH003', N'Phạm Gia Huy', '0988777666', 'CL03', 'CN01', '2026-10-07', '18:00 - 20:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Thi đấu giao hữu'),
('DS01_1007_3', 'KH004', N'Vũ Thị Mai Anh', '0977555444', 'CL08', 'CN01', '2026-10-07', '19:00 - 21:00', 'KG03', 90000, 180000, 54000, N'VietQR Pro', N'Hoàn thành', N'Đánh đôi nữ'),
('DS01_1008_3', 'KH006', N'Bùi Khánh Linh', '0909111222', 'CL07', 'CN01', '2026-10-08', '17:00 - 19:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Khách vãng lai'),
('DS01_1008_1', 'KH002', N'Nguyễn Hoàng Nam', '0903123456', 'CL02', 'CN01', '2026-10-08', '18:00 - 20:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Định kỳ thứ 5'),
('DS01_1008_2', 'KH005', N'Trần Thanh Hằng', '0933444555', 'CL04', 'CN01', '2026-10-08', '20:00 - 22:00', 'KG03', 90000, 180000, 54000, N'VietQR Pro', N'Hoàn thành', N'CLB Ngân Hàng'),
('DS01_1009_1', 'KH001', N'Lê Bá Đạt', '0912345678', 'CL01', 'CN01', '2026-10-09', '17:00 - 19:00', 'KG03', 120000, 240000, 72000, N'Chuyển khoản VietQR', N'Hoàn thành', N'VIP Thứ 6'),
('DS01_1009_3', 'KH004', N'Vũ Thị Mai Anh', '0977555444', 'CL08', 'CN01', '2026-10-09', '18:00 - 20:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Khách nhóm'),
('DS01_1009_2', 'KH003', N'Phạm Gia Huy', '0988777666', 'CL03', 'CN01', '2026-10-09', '19:00 - 21:00', 'KG03', 90000, 180000, 54000, N'VietQR Pro', N'Hoàn thành', N'Giao lưu cuối tuần'),
('DS01_1012_1', 'KH002', N'Nguyễn Hoàng Nam', '0903123456', 'CL02', 'CN01', '2026-10-12', '18:00 - 20:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Tối thứ 2'),
('DS01_1012_2', 'KH004', N'Vũ Thị Mai Anh', '0977555444', 'CL07', 'CN01', '2026-10-12', '19:00 - 21:00', 'KG03', 90000, 180000, 54000, N'VietQR Pro', N'Hoàn thành', N'Tập đánh đôi'),
('DS01_1012_3', 'KH006', N'Bùi Khánh Linh', '0909111222', 'CL08', 'CN01', '2026-10-12', '20:00 - 22:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Hoàn thành', N'Giao lưu câu lạc bộ'),
('DS01_1013_1', 'KH002', N'Nguyễn Hoàng Nam', '0903123456', 'CL02', 'CN01', '2026-10-13', '17:00 - 19:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Đang sử dụng', N'Hôm nay - Ca chiều'),
('DS01_1013_3', 'KH005', N'Trần Thanh Hằng', '0933444555', 'CL04', 'CN01', '2026-10-13', '18:00 - 20:00', 'KG03', 90000, 180000, 54000, N'Tiền mặt', N'Đã xác nhận', N'Hôm nay - CLB Nữ'),
('DS01_1013_2', 'KH003', N'Phạm Gia Huy', '0988777666', 'CL03', 'CN01', '2026-10-13', '19:00 - 21:00', 'KG03', 90000, 180000, 54000, N'VietQR Pro', N'Đã xác nhận', N'Hôm nay - Ca tối'),
('DS01_1013_4', 'KH004', N'Vũ Thị Mai Anh', '0977555444', 'CL07', 'CN01', '2026-10-13', '20:00 - 22:00', 'KG03', 90000, 180000, 54000, N'VietQR Pro', N'Đã xác nhận', N'Hôm nay - Đánh đôi'),
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
('NS01', 'ALL', N'Võ Hoàng Nam', N'Giám đốc điều hành', '0901111222', 'nam.vh@utesport.vn', N'Quận 1, TP. Hồ Chí Minh'),
('NS02', 'CN01', N'Trần Phúc Bảo', N'Quản lý chi nhánh Thủ Đức', '0903123456', 'bao.tran@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh'),
('NS03', 'CN01', N'Nguyễn Thanh Tâm', N'Thu ngân quầy POS', '0904555666', 'tam.nguyen@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh'),
('NS04', 'CN01', N'Lê Thị Mai', N'Thu ngân quầy POS', '0904777888', 'mai.lt@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh'),
('NS05', 'CN02', N'Nguyễn Văn Quản', N'Quản lý chi nhánh Bình Thạnh', '0903333444', 'quan.nv@utesport.vn', N'Bình Thạnh, TP. Hồ Chí Minh'),
('NS06', 'CN03', N'Trần Hữu Nam', N'Quản lý chi nhánh Quận 9', '0903789012', 'nam.th@utesport.vn', N'Quận 9, TP. Hồ Chí Minh'),
('NS07', 'CN04', N'Lê Hoàng Khang', N'Quản lý chi nhánh Gò Vấp', '0903555777', 'khang.lh@utesport.vn', N'Gò Vấp, TP. Hồ Chí Minh'),
('NS08', 'CN04', N'Lê Văn Hùng', N'Nhân viên kỹ thuật sân', '0908111333', 'hung.lv@utesport.vn', N'Gò Vấp, TP. Hồ Chí Minh'),
('NS09', 'CN01', N'Bùi Khánh Linh', N'Lễ tân & Dịch vụ', '0909111222', 'linh.bk@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh'),
('NS10', 'CN01', N'Nguyễn Hoàng Nam', N'Thu ngân POS quầy', '0903888777', 'nam.nh@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh'),
('NS11', 'CN01', N'Phạm Gia Huy', N'Kỹ thuật & Bảo dưỡng sân', '0988777666', 'huy.pg@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh'),
('NS12', 'CN01', N'Trịnh Gia Vinh', N'Kỹ thuật & Bảo dưỡng sân', '0977222333', 'vinh.tg@utesport.vn', N'TP. Thủ Đức, TP. Hồ Chí Minh');

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
INSERT INTO tournaments (tournament_code, branch_code, tournament_name, category, start_date, end_date, rules, max_participants, current_participants, entry_fee, total_prize, status) VALUES
('GD01', 'CN01', N'Giải Cầu Lông Đôi Nam Nữ Mùa Thu UTE Open 2026', N'Đôi nam nữ', '2026-10-10 08:00:00', '2026-10-12 18:00:00', N'Thi đấu theo thể thức loại trực tiếp 3 hiệp 21 điểm theo luật BWF. Mỗi VĐV chỉ được đăng ký 1 nội dung.', 32, 24, 300000, 20000000, N'Đã hoàn thành'),
('GD02', 'CN01', N'Giải Cầu Lông Doanh Nghiệp & Sinh Viên Cúp Thủ Đức 2026', N'Tổng hợp nhiều nội dung', '2026-10-25 08:00:00', '2026-10-26 17:00:00', N'Giải đấu phong trào kết nối các doanh nghiệp và cựu sinh viên đam mê cầu lông.', 24, 0, 250000, 15000000, N'Sắp mở đăng ký'),
('GD03', 'CN02', N'Giải Đơn Nam Bình Thạnh Mở Rộng 2026', N'Đơn nam', '2026-11-05 08:00:00', '2026-11-06 18:00:00', N'Giải đơn nam các CLB phong trào khu vực Bình Thạnh - Phú Nhuận.', 16, 0, 200000, 10000000, N'Sắp mở đăng ký');

-- 5.15 DỮ LIỆU ĐĂNG KÝ THAM GIA GIẢI ĐẤU (tournament_registrations)
INSERT INTO tournament_registrations (tournament_code, customer_code, team_name, category, phone, status) VALUES
('GD01', 'KH001', N'Cặp Đôi Đạt & Trang (CLB Thủ Đức)', N'Đôi nam nữ', '0912345678', N'Đã xác nhận'),
('GD01', 'KH002', N'Cặp Đôi Nam & Thảo (ĐH Sư Phạm Kỹ Thuật)', N'Đôi nam nữ', '0903123456', N'Đã xác nhận'),
('GD01', 'KH003', N'Cặp Đôi Huy & Hằng (Công ty TMA)', N'Đôi nam nữ', '0988777666', N'Đã xác nhận'),
('GD01', 'KH004', N'Cặp Đôi Mai Anh & Đức (CLB Yonex)', N'Đôi nam nữ', '0977555444', N'Đã xác nhận'),
('GD01', 'KH005', N'Cặp Đôi Tuấn & Lan (CLB Bình Thạnh)', N'Đôi nam nữ', '0933221100', N'Đã xác nhận');

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

