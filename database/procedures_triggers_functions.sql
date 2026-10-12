-- =====================================================================
-- DỰ ÁN QUẢN LÝ CHUỖI SÂN CẦU LÔNG - UTE SPORT
-- HỆ QUẢN TRỊ CƠ SỞ DỮ LIỆU: MICROSOFT SQL SERVER
-- TẬP LỆNH: STORED PROCEDURES, TRIGGERS & USER-DEFINED FUNCTIONS
-- =====================================================================

USE badminton;
GO

-- =====================================================================
-- 0. XÓA CÁC ĐỐI TƯỢNG CŨ NẾU ĐÃ TỒN TẠI (IDEMPOTENT SCRIPT)
-- =====================================================================

-- Xóa Triggers
IF OBJECT_ID('dbo.trg_CheckBookingOverlap', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_CheckBookingOverlap;
IF OBJECT_ID('dbo.trg_UpdateStockOnInvoiceProduct', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_UpdateStockOnInvoiceProduct;
IF OBJECT_ID('dbo.trg_UpdateStockOnReceiptDetails', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_UpdateStockOnReceiptDetails;
IF OBJECT_ID('dbo.trg_UpdateCustomerMembershipLevel', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_UpdateCustomerMembershipLevel;
IF OBJECT_ID('dbo.trg_AuditCourtStatusChange', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_AuditCourtStatusChange;
IF OBJECT_ID('dbo.trg_ValidatePromotionDates', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_ValidatePromotionDates;
GO

-- Xóa Stored Procedures
IF OBJECT_ID('dbo.sp_LockSlotAndCreateBooking', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_LockSlotAndCreateBooking;
IF OBJECT_ID('dbo.sp_ConfirmBookingDeposit', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_ConfirmBookingDeposit;
IF OBJECT_ID('dbo.sp_CheckInBooking', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_CheckInBooking;
IF OBJECT_ID('dbo.sp_CheckOutAndCreateInvoice', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_CheckOutAndCreateInvoice;
IF OBJECT_ID('dbo.sp_CancelBookingWithRefundPolicy', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_CancelBookingWithRefundPolicy;
IF OBJECT_ID('dbo.sp_ImportStockReceipt', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_ImportStockReceipt;
IF OBJECT_ID('dbo.sp_GetBranchDailyRevenueReport', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_GetBranchDailyRevenueReport;
GO

-- Xóa Functions
IF OBJECT_ID('dbo.fn_CheckSlotAvailability', 'FN') IS NOT NULL DROP FUNCTION dbo.fn_CheckSlotAvailability;
IF OBJECT_ID('dbo.fn_CalculateBookingPrice', 'FN') IS NOT NULL DROP FUNCTION dbo.fn_CalculateBookingPrice;
IF OBJECT_ID('dbo.fn_GetCustomerTierDiscount', 'FN') IS NOT NULL DROP FUNCTION dbo.fn_GetCustomerTierDiscount;
IF OBJECT_ID('dbo.fn_GetBranchRevenue', 'FN') IS NOT NULL DROP FUNCTION dbo.fn_GetBranchRevenue;
IF OBJECT_ID('dbo.fn_GetAvailableCourtsByBranch', 'IF') IS NOT NULL DROP FUNCTION dbo.fn_GetAvailableCourtsByBranch;
GO

-- =====================================================================
-- PHẦN 1: HÀM NGƯỜI DÙNG ĐỊNH NGHĨA (USER-DEFINED FUNCTIONS - UDF)
-- =====================================================================

-- 1.1 Hàm kiểm tra tính khả dụng của sân trong khung giờ cụ thể
-- Trả về 1 nếu slot trống và sân đang hoạt động; Trả về 0 nếu bị trùng lịch hoặc bảo trì
CREATE FUNCTION dbo.fn_CheckSlotAvailability
(
    @court_code VARCHAR(20),
    @booking_date DATE,
    @time_slot VARCHAR(50)
)
RETURNS BIT
AS
BEGIN
    -- Nếu sân đang ở trạng thái Bảo trì thì không khả dụng
    IF EXISTS (SELECT 1 FROM courts WHERE court_code = @court_code AND status = N'Bảo trì')
    BEGIN
        RETURN 0;
    END

    -- Kiểm tra xem đã có đơn đặt sân nào chưa bị hủy trong khung giờ này chưa
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

-- 1.2 Hàm tính tiền thuê sân theo thời lượng khung giờ và chiết khấu khuyến mãi
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

    -- Lấy giá niêm yết theo giờ của sân
    SELECT @hourly_rate = ISNULL(hourly_rate, 90000)
    FROM courts
    WHERE court_code = @court_code;

    -- Tính số giờ từ time_slot (ví dụ "06:00 - 08:00" -> 2 tiếng)
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

    -- Kiểm tra và áp dụng mã khuyến mãi nếu hợp lệ
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

-- 1.3 Hàm lấy tỷ lệ giảm giá theo hạng thẻ thành viên của khách hàng
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

-- 1.4 Hàm tổng hợp tổng doanh thu của một chi nhánh trong một khoảng thời gian
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

-- 1.5 Hàm trả về bảng danh sách các sân còn trống theo chi nhánh và khung giờ (Table-Valued Function)
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
-- PHẦN 2: THỦ TỤC LƯU TRỮ NGHIỆP VỤ (STORED PROCEDURES)
-- =====================================================================

-- 2.1 Thủ tục đặt & khóa sân trực tuyến với cơ chế bảo vệ giao dịch (Concurrency Transaction)
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

        -- 1. Kiểm tra tính khả dụng bằng Function
        IF dbo.fn_CheckSlotAvailability(@court_code, @booking_date, @time_slot) = 0
        BEGIN
            THROW 50001, N'Khung giờ hoặc sân đã có người đặt trước hoặc đang bảo trì!', 1;
        END

        -- 2. Sinh mã đơn đặt sân duy nhất dạng DSxxxxxx
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

        -- 3. Tính toán tiền thuê và tiền cọc
        DECLARE @hourly_price DECIMAL(18,2);
        SELECT @hourly_price = ISNULL(hourly_rate, 90000) FROM courts WHERE court_code = @court_code;

        DECLARE @court_fee DECIMAL(18,2);
        SET @court_fee = dbo.fn_CalculateBookingPrice(@court_code, @time_slot, @promo_code);

        SET @total_price = @court_fee + ISNULL(@addon_amount, 0);
        SET @deposit_amount = ROUND(@total_price * 0.3, 0);

        -- 4. Tự động liên kết mã thành viên nếu số điện thoại đã tồn tại
        DECLARE @cust_code VARCHAR(20) = NULL;
        SELECT @cust_code = customer_code FROM customers WHERE phone = @customer_phone;

        -- 5. Chèn đơn đặt sân vào bảng bookings
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

        -- 6. Ghi nhật ký hệ thống
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

-- 2.2 Thủ tục xác nhận thanh toán cọc 30% qua VietQR
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

        -- Cập nhật trạng thái đơn đặt sân
        UPDATE bookings
        SET status = N'Đã cọc 30%',
            deposit_amount = @amount
        WHERE booking_code = @booking_code;

        -- Sinh mã thanh toán TT...
        DECLARE @pay_code VARCHAR(30) = 'TT' + SUBSTRING(@booking_code, 3, LEN(@booking_code));
        IF EXISTS (SELECT 1 FROM payments WHERE payment_code = @pay_code)
        BEGIN
            SET @pay_code = 'TT' + CAST(CAST(RAND(CHECKSUM(NEWID())) * 899999 + 100000 AS INT) AS VARCHAR(10));
        END

        -- Chèn bản ghi vào bảng payments
        INSERT INTO payments (
            payment_code, booking_code, amount, method, 
            transaction_type, transaction_ref, payment_time, status
        )
        VALUES (
            @pay_code, @booking_code, @amount, @method,
            N'Đặt cọc 30%', ISNULL(@transaction_ref, 'AUTO-VIETQR-' + @booking_code),
            GETDATE(), N'Thành công'
        );

        -- Cập nhật trạng thái sân thành Đặt trước nếu là ngày hôm nay
        DECLARE @b_date DATE;
        DECLARE @c_code VARCHAR(20);
        SELECT @b_date = booking_date, @c_code = court_code FROM bookings WHERE booking_code = @booking_code;
        
        IF @b_date = CAST(GETDATE() AS DATE)
        BEGIN
            UPDATE courts SET status = N'Đặt trước' WHERE court_code = @c_code AND status = N'Trống';
        END

        -- Ghi nhật ký
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

-- 2.3 Thủ tục Check-in nhận sân tại quầy POS
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

        -- Cập nhật đơn đặt sân sang trạng thái Đang sử dụng
        UPDATE bookings
        SET status = N'Đang sử dụng',
            check_in_time = GETDATE()
        WHERE booking_code = @booking_code;

        -- Cập nhật trạng thái sân
        UPDATE courts
        SET status = N'Đang sử dụng'
        WHERE court_code = @court_code;

        -- Ghi nhật ký
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

-- 2.4 Thủ tục Check-out trả sân & Xuất hóa đơn thanh toán toàn diện
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

        -- Cập nhật đơn đặt sân và giải phóng sân
        UPDATE bookings
        SET status = N'Hoàn thành',
            check_out_time = GETDATE()
        WHERE booking_code = @booking_code;

        UPDATE courts
        SET status = N'Trống'
        WHERE court_code = @court_code;

        -- Sinh mã hóa đơn mới
        DECLARE @count INT;
        SELECT @count = COUNT(*) FROM invoices;
        SET @invoice_code = 'HD' + RIGHT('000000' + CAST(@count + 1 AS VARCHAR(10)), 6);

        -- Tính số tiền thanh toán còn lại (trừ khoản cọc đã nạp)
        SET @final_payment = @court_fee - @deposit_paid;
        IF @final_payment < 0 SET @final_payment = 0;

        -- Tạo bản ghi hóa đơn
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

        -- Tích điểm thưởng và giờ chơi nếu là khách hàng thành viên
        IF @cust_code IS NOT NULL
        BEGIN
            UPDATE customers
            SET total_play_hours = total_play_hours + 1.0,
                reward_points = reward_points + CAST(@court_fee / 10000 AS INT)
            WHERE customer_code = @cust_code;
        END

        -- Ghi nhật ký
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

-- 2.5 Thủ tục hủy đơn đặt sân và áp dụng chính sách hoàn cọc (trước 4 tiếng hoàn 100%)
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

        -- Lấy giờ bắt đầu từ time_slot (ví dụ "18:00 - 19:00" -> 18)
        DECLARE @start_hour INT = 8;
        IF CHARINDEX('-', @time_slot) > 0
        BEGIN
            SET @start_hour = TRY_CAST(SUBSTRING(LTRIM(RTRIM(@time_slot)), 1, 2) AS INT);
        END
        IF @start_hour IS NULL SET @start_hour = 8;

        DECLARE @play_datetime DATETIME;
        SET @play_datetime = DATEADD(HOUR, @start_hour, CAST(@booking_date AS DATETIME));

        -- Đọc cấu hình thời gian hủy tối thiểu để được hoàn cọc (mặc định 4 giờ)
        DECLARE @min_cancel_hours INT = 4;
        SELECT @min_cancel_hours = TRY_CAST(setting_value AS INT)
        FROM system_settings
        WHERE setting_code = 'CFG_CANCEL_HOURS';
        IF @min_cancel_hours IS NULL SET @min_cancel_hours = 4;

        -- Tính số tiếng chênh lệch từ hiện tại đến giờ chơi
        DECLARE @diff_hours INT = DATEDIFF(HOUR, GETDATE(), @play_datetime);

        IF @diff_hours >= @min_cancel_hours AND @deposit > 0
        BEGIN
            SET @refund_amount = @deposit;
            SET @is_refunded = 1;

            -- Tạo giao dịch hoàn tiền cọc
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

        -- Cập nhật đơn đặt sân sang Đã hủy
        UPDATE bookings
        SET status = N'Đã hủy',
            notes = ISNULL(notes, '') + N' [Hủy: ' + @cancel_reason + N' | Hoàn cọc: ' + FORMAT(@refund_amount, 'N0') + N'đ]'
        WHERE booking_code = @booking_code;

        -- Giải phóng trạng thái sân
        UPDATE courts
        SET status = N'Trống'
        WHERE court_code = @court_code AND status = N'Đặt trước';

        -- Ghi nhật ký
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

-- 2.6 Thủ tục lập phiếu nhập kho vật tư / hàng hóa
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

-- 2.7 Thủ tục báo cáo tổng kết doanh thu theo ngày của chi nhánh
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
-- PHẦN 3: TRIGGER TỰ ĐỘNG HÓA VÀ RÀNG BUỘC TOÀN VẸN (TRIGGERS)
-- =====================================================================

-- 3.1 Trigger kiểm soát chống đặt trùng giờ trên cùng một sân (Chống Race Condition)
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

-- 3.2 Trigger tự động giảm số lượng tồn kho sản phẩm khi xuất hóa đơn POS
CREATE TRIGGER dbo.trg_UpdateStockOnInvoiceProduct
ON dbo.invoice_product_details
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    -- Tự động trừ tồn kho sản phẩm / đồ uống khi xuất bán
    UPDATE p
    SET p.stock_quantity = p.stock_quantity - i.quantity
    FROM dbo.products p
    INNER JOIN inserted i ON p.product_code = i.product_code;

    -- Kiểm tra nếu tồn kho bị âm thì ngăn chặn
    IF EXISTS (SELECT 1 FROM dbo.products WHERE stock_quantity < 0)
    BEGIN
        RAISERROR (N'Lỗi xuất kho: Số lượng tồn kho sản phẩm trong kho không đủ để bán!', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END
END;
GO

-- 3.3 Trigger tự động tăng số lượng tồn kho khi nhập hàng từ nhà cung cấp
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

-- 3.4 Trigger tự động thăng hạng thẻ thành viên theo lũy kế giờ chơi
CREATE TRIGGER dbo.trg_UpdateCustomerMembershipLevel
ON dbo.customers
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    -- Tự động cập nhật hạng thành viên:
    -- Dưới 10 giờ: Đồng
    -- Từ 10 đến 29 giờ: Bạc
    -- Từ 30 đến 69 giờ: Vàng
    -- Từ 70 giờ trở lên: Kim Cương
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

-- 3.5 Trigger tự động ghi nhật ký hệ thống khi trạng thái sân bãi thay đổi
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

-- 3.6 Trigger xác thực tính hợp lệ của chương trình khuyến mãi
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

PRINT N'>>> ĐÃ TẠO TOÀN DIỆN CÁC STORED PROCEDURES, TRIGGERS VÀ FUNCTIONS THÀNH CÔNG CHO DỰ ÁN!';
GO
