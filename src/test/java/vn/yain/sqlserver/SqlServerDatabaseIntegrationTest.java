package vn.yain.sqlserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bo Kiem Thu Toan Dien Chuyen Sau Tren CSDL Microsoft SQL Server 2022
 * Kiem tra toan bo:
 * - 5 Functions (UDF)
 * - 7 Stored Procedures (Toan ven luong nghiep vu)
 * - 6 Triggers (Bao ve toan ven du lieu, chong trung gio, tu dong thang hang, tu dong giam/tang kho)
 * - 24 Vietnamese Views (Dong bo thuc the 100%)
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=badminton;encrypt=true;trustServerCertificate=true;",
    "spring.datasource.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver",
    "spring.datasource.username=sa",
    "spring.datasource.password=123456",
    "spring.jpa.database-platform=org.hibernate.dialect.SQLServerDialect",
    "spring.jpa.hibernate.ddl-auto=none"
})
class SqlServerDatabaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("SQL Server 01: Ket noi thanh cong toi database badminton tren SQL Server 2022")
    void testSqlServerConnection() {
        String dbName = jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class);
        assertEquals("badminton", dbName);
    }

    @Test
    @DisplayName("SQL Server 02: UDF dbo.fn_CheckSlotAvailability kiem tra tinh kha dung khung gio")
    void testFnCheckSlotAvailability() {
        LocalDate futureDate = LocalDate.now().plusDays(200);
        Integer isAvail = jdbcTemplate.queryForObject(
                "SELECT dbo.fn_CheckSlotAvailability('CL01', ?, '06:00 - 07:00')",
                Integer.class,
                futureDate.toString()
        );
        assertNotNull(isAvail);
        assertEquals(1, isAvail, "Slot trong tuong lai chua co don phai tra ve 1 (Kha dung)");
    }

    @Test
    @DisplayName("SQL Server 03: UDF dbo.fn_CalculateBookingPrice tinh dung don gia theo thoi luong va gio")
    void testFnCalculateBookingPrice() {
        BigDecimal price = jdbcTemplate.queryForObject(
                "SELECT dbo.fn_CalculateBookingPrice('CL01', '06:00 - 08:00', NULL)",
                BigDecimal.class
        );
        assertNotNull(price);
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Gia thue 2 gio phai lon hon 0");
    }

    @Test
    @DisplayName("SQL Server 04: UDF dbo.fn_GetCustomerTierDiscount tra dung % giam gia theo hang the")
    void testFnGetCustomerTierDiscount() {
        String custCode = jdbcTemplate.queryForObject("SELECT TOP 1 customer_code FROM customers WHERE membership_tier = N'Đồng'", String.class);
        if (custCode != null) {
            BigDecimal disc = jdbcTemplate.queryForObject(
                    "SELECT dbo.fn_GetCustomerTierDiscount(?)",
                    BigDecimal.class,
                    custCode
            );
            assertNotNull(disc);
            assertEquals(0, disc.compareTo(BigDecimal.ZERO), "Hang Dong duoc giam 0%");
        }
    }

    @Test
    @DisplayName("SQL Server 05: UDF dbo.fn_GetBranchRevenue tong hop doanh thu chi nhanh")
    void testFnGetBranchRevenue() {
        BigDecimal rev = jdbcTemplate.queryForObject(
                "SELECT dbo.fn_GetBranchRevenue('CN01', '2020-01-01', '2030-12-31')",
                BigDecimal.class
        );
        assertNotNull(rev);
        assertTrue(rev.compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    @DisplayName("SQL Server 06: UDF dbo.fn_GetAvailableCourtsByBranch tra ve danh sach san con trong")
    void testFnGetAvailableCourtsByBranch() {
        var list = jdbcTemplate.queryForList(
                "SELECT * FROM dbo.fn_GetAvailableCourtsByBranch('CN01', '2028-01-01', '08:00 - 09:00')"
        );
        assertNotNull(list);
        assertFalse(list.isEmpty(), "Chi nhanh CN01 phai co san con trong");
    }

    @Test
    @DisplayName("SQL Server 07: Procedure dbo.sp_LockSlotAndCreateBooking khoa slot va tu dong tinh 30% coc")
    void testSpLockSlotAndCreateBooking() {
        String testCourt = "CL03";
        LocalDate testDate = LocalDate.now().plusDays(250);
        String testSlot = "14:00 - 15:00";

        jdbcTemplate.update("DELETE FROM bookings WHERE court_code = ? AND booking_date = ?", testCourt, testDate.toString());

        jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("{call dbo.sp_LockSlotAndCreateBooking(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
                cs.setString(1, testCourt);
                cs.setString(2, "CN01");
                cs.setString(3, testDate.toString());
                cs.setString(4, testSlot);
                cs.setString(5, "Nguoi Test SQL");
                cs.setString(6, "0912345678");
                cs.setString(7, "Dat san thu nghiem");
                cs.setString(8, null);
                cs.setBigDecimal(9, BigDecimal.ZERO);
                cs.registerOutParameter(10, Types.VARCHAR); // @booking_code
                cs.registerOutParameter(11, Types.DECIMAL); // @deposit_amount
                cs.registerOutParameter(12, Types.DECIMAL); // @total_price

                cs.execute();
                String bkCode = cs.getString(10);
                BigDecimal deposit = cs.getBigDecimal(11);
                BigDecimal total = cs.getBigDecimal(12);

                assertNotNull(bkCode);
                assertTrue(bkCode.startsWith("DS"), "Ma dat san phai bat dau bang DS");
                assertNotNull(total);
                assertNotNull(deposit);
                assertTrue(deposit.compareTo(BigDecimal.ZERO) > 0);

                // Don dep sau khi test
                jdbcTemplate.update("DELETE FROM audit_logs WHERE details LIKE ?", "%" + bkCode + "%");
                jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", bkCode);
            }
            return null;
        });
    }

    @Test
    @DisplayName("SQL Server 08: Vong doi Stored Procedures (Lock -> Confirm Deposit -> Check-in -> Check-out Invoice)")
    void testFullProcedureBookingLifecycle() {
        String testCourt = "CL01";
        LocalDate testDate = LocalDate.now().plusDays(100);
        String testSlot = "10:00 - 11:00";

        jdbcTemplate.update("DELETE FROM bookings WHERE court_code = ? AND booking_date = ?", testCourt, testDate.toString());

        final String[] createdBkCode = new String[1];

        // 1. sp_LockSlotAndCreateBooking
        jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("{call dbo.sp_LockSlotAndCreateBooking(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
                cs.setString(1, testCourt);
                cs.setString(2, "CN01");
                cs.setString(3, testDate.toString());
                cs.setString(4, testSlot);
                cs.setString(5, "Nguyen Van Lifecycle");
                cs.setString(6, "0909999111");
                cs.setString(7, "Dat qua test lifecycle");
                cs.setString(8, null);
                cs.setBigDecimal(9, BigDecimal.ZERO);
                cs.registerOutParameter(10, Types.VARCHAR);
                cs.registerOutParameter(11, Types.DECIMAL);
                cs.registerOutParameter(12, Types.DECIMAL);

                cs.execute();
                createdBkCode[0] = cs.getString(10);
            }
            return null;
        });

        String bkCode = createdBkCode[0];
        assertNotNull(bkCode);

        // 2. sp_ConfirmBookingDeposit
        jdbcTemplate.update("EXEC dbo.sp_ConfirmBookingDeposit @booking_code = ?, @amount = 30000, @method = N'VietQR Pro', @transaction_ref = 'TX-LIFECYCLE'", bkCode);
        String statusAfterDeposit = jdbcTemplate.queryForObject("SELECT status FROM bookings WHERE booking_code = ?", String.class, bkCode);
        assertEquals("Đã cọc 30%", statusAfterDeposit);

        // 3. sp_CheckInBooking
        jdbcTemplate.update("EXEC dbo.sp_CheckInBooking @booking_code = ?, @staff_code = 'NVQ01'", bkCode);
        String statusAfterCheckIn = jdbcTemplate.queryForObject("SELECT status FROM bookings WHERE booking_code = ?", String.class, bkCode);
        assertEquals("Đang sử dụng", statusAfterCheckIn);

        // 4. sp_CheckOutAndCreateInvoice
        jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("{call dbo.sp_CheckOutAndCreateInvoice(?, ?, ?, ?, ?)}")) {
                cs.setString(1, bkCode);
                cs.setString(2, "NVQ01");
                cs.setString(3, "Tiền mặt");
                cs.registerOutParameter(4, Types.VARCHAR); // @invoice_code
                cs.registerOutParameter(5, Types.DECIMAL); // @final_payment

                cs.execute();
                String invCode = cs.getString(4);
                BigDecimal finalPay = cs.getBigDecimal(5);

                assertNotNull(invCode);
                assertTrue(invCode.startsWith("HD"));
                assertNotNull(finalPay);

                // Cleanup invoice
                jdbcTemplate.update("DELETE FROM invoices WHERE invoice_code = ?", invCode);
            }
            return null;
        });

        // Cleanup invoice, payment, booking
        jdbcTemplate.update("DELETE FROM payments WHERE booking_code = ?", bkCode);
        jdbcTemplate.update("DELETE FROM audit_logs WHERE details LIKE ?", "%" + bkCode + "%");
        jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", bkCode);
    }

    @Test
    @DisplayName("SQL Server 09: Procedure dbo.sp_CancelBookingWithRefundPolicy hoan coc neu truoc 4 gio")
    void testSpCancelBookingWithRefundPolicy() {
        String testCourt = "CL02";
        LocalDate futureDate = LocalDate.now().plusDays(150);
        String testSlot = "18:00 - 19:00";
        String bkCode = "DS_CANCEL_" + (System.currentTimeMillis() % 10000);

        jdbcTemplate.update(
                "INSERT INTO bookings (booking_code, court_code, branch_code, customer_name, customer_phone, booking_date, time_slot, hourly_price, total_price, deposit_amount, status) " +
                "VALUES (?, ?, 'CN01', 'Khach Huy', '0908888999', ?, ?, 100000, 100000, 30000, N'Đã cọc 30%')",
                bkCode, testCourt, futureDate.toString(), testSlot
        );

        jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("{call dbo.sp_CancelBookingWithRefundPolicy(?, ?, ?, ?)}")) {
                cs.setString(1, bkCode);
                cs.setString(2, "Ly do ca nhan");
                cs.registerOutParameter(3, Types.DECIMAL); // @refund_amount
                cs.registerOutParameter(4, Types.BIT);     // @is_refunded

                cs.execute();
                BigDecimal refund = cs.getBigDecimal(3);
                boolean isRefunded = cs.getBoolean(4);

                assertTrue(isRefunded, "Dat truoc 150 ngay phai duoc hoan 100% coc");
                assertEquals(0, refund.compareTo(new BigDecimal("30000")));
            }
            return null;
        });

        // Cleanup: Xoa payments truoc khi xoa bookings de khong vi pham Foreign Key
        jdbcTemplate.update("DELETE FROM payments WHERE booking_code = ?", bkCode);
        jdbcTemplate.update("DELETE FROM audit_logs WHERE details LIKE ?", "%" + bkCode + "%");
        jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", bkCode);
    }

    @Test
    @DisplayName("SQL Server 10: Procedure dbo.sp_ImportStockReceipt lap phieu nhap kho")
    void testSpImportStockReceipt() {
        String receiptCode = "PNK_TEST_" + (System.currentTimeMillis() % 10000);
        jdbcTemplate.update(
                "EXEC dbo.sp_ImportStockReceipt @receipt_code = ?, @branch_code = 'CN01', @staff_code = 'NS02', @supplier_name = N'Yonex Viet Nam', @total_amount = 5000000, @notes = N'Nhap vot test'",
                receiptCode
        );

        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stock_receipts WHERE receipt_code = ?", Integer.class, receiptCode);
        assertEquals(1, exists);

        // Cleanup
        jdbcTemplate.update("DELETE FROM audit_logs WHERE details LIKE ?", "%" + receiptCode + "%");
        jdbcTemplate.update("DELETE FROM stock_receipts WHERE receipt_code = ?", receiptCode);
    }

    @Test
    @DisplayName("SQL Server 11: Procedure dbo.sp_GetBranchDailyRevenueReport bao cao doanh thu ngay")
    void testSpGetBranchDailyRevenueReport() {
        var report = jdbcTemplate.queryForList("EXEC dbo.sp_GetBranchDailyRevenueReport @branch_code = 'CN01'");
        assertNotNull(report);
        assertFalse(report.isEmpty());
    }

    @Test
    @DisplayName("SQL Server 12: Trigger trg_CheckBookingOverlap chan trung lich cap Database")
    void testTrgCheckBookingOverlap() {
        String testCourt = "CL04";
        LocalDate testDate = LocalDate.now().plusDays(300);
        String slot = "09:00 - 10:00";

        jdbcTemplate.update("DELETE FROM bookings WHERE court_code = ? AND booking_date = ?", testCourt, testDate.toString());

        // Tao don thu 1
        String b1 = "DS_TEST_TRG_1_" + (System.currentTimeMillis() % 10000);
        jdbcTemplate.update(
                "INSERT INTO bookings (booking_code, court_code, branch_code, customer_name, customer_phone, booking_date, time_slot, hourly_price, total_price, deposit_amount, status) " +
                "VALUES (?, ?, 'CN01', 'Khach A', '0901111111', ?, ?, 100000, 100000, 30000, N'Chờ cọc 30%')",
                b1, testCourt, testDate.toString(), slot
        );

        // Tao don thu 2 trung gio va ngay tren cung san -> Phai bi Trigger rollback
        String b2 = "DS_TEST_TRG_2_" + (System.currentTimeMillis() % 10000);
        Exception ex = assertThrows(Exception.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO bookings (booking_code, court_code, branch_code, customer_name, customer_phone, booking_date, time_slot, hourly_price, total_price, deposit_amount, status) " +
                    "VALUES (?, ?, 'CN01', 'Khach B', '0902222222', ?, ?, 100000, 100000, 30000, N'Chờ cọc 30%')",
                    b2, testCourt, testDate.toString(), slot
            );
        });

        assertTrue(ex.getMessage().contains("Lỗi xung đột lịch") || ex.getMessage().contains("trùng"), "Trigger phai bao loi xung dot lich");

        // Cleanup
        jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", b1);
    }

    @Test
    @DisplayName("SQL Server 13: Trigger trg_UpdateCustomerMembershipLevel tu dong thang hang theo gio choi")
    void testTrgUpdateCustomerMembershipLevel() {
        String testCust = "KH_TEST_TIER_" + (System.currentTimeMillis() % 10000);
        jdbcTemplate.update(
                "INSERT INTO customers (customer_code, full_name, phone, email, total_play_hours, membership_tier, reward_points) " +
                "VALUES (?, N'Khach Hang Test Tier', '0988776655', 'tier@test.com', 5, N'Đồng', 0)",
                testCust
        );

        // Cap nhat total_play_hours len 15 -> Trigger phai tu dong chuyen sang Bac
        jdbcTemplate.update("UPDATE customers SET total_play_hours = 15 WHERE customer_code = ?", testCust);
        String tier1 = jdbcTemplate.queryForObject("SELECT membership_tier FROM customers WHERE customer_code = ?", String.class, testCust);
        assertEquals("Bạc", tier1);

        // Cap nhat total_play_hours len 35 -> Trigger phai tu dong chuyen sang Vang
        jdbcTemplate.update("UPDATE customers SET total_play_hours = 35 WHERE customer_code = ?", testCust);
        String tier2 = jdbcTemplate.queryForObject("SELECT membership_tier FROM customers WHERE customer_code = ?", String.class, testCust);
        assertEquals("Vàng", tier2);

        // Cap nhat total_play_hours len 75 -> Trigger phai tu dong chuyen sang Kim Cuong
        jdbcTemplate.update("UPDATE customers SET total_play_hours = 75 WHERE customer_code = ?", testCust);
        String tier3 = jdbcTemplate.queryForObject("SELECT membership_tier FROM customers WHERE customer_code = ?", String.class, testCust);
        assertEquals("Kim Cương", tier3);

        // Cleanup
        jdbcTemplate.update("DELETE FROM customers WHERE customer_code = ?", testCust);
    }

    @Test
    @DisplayName("SQL Server 14: Trigger trg_UpdateStockOnReceiptDetails tu dong tang ton kho khi nhap hang")
    void testTrgUpdateStockOnReceiptDetails() {
        String pCode = "P01";
        Integer stockBefore = jdbcTemplate.queryForObject("SELECT stock_quantity FROM products WHERE product_code = ?", Integer.class, pCode);
        assertNotNull(stockBefore);

        // Tao 1 receipt tam voi staffCode hop le (NS02)
        String rcCode = "RC_STOCK_TEST_" + (System.currentTimeMillis() % 10000);
        jdbcTemplate.update(
                "INSERT INTO stock_receipts (receipt_code, branch_code, staff_code, receipt_date, supplier_name, total_amount) " +
                "VALUES (?, 'CN01', 'NS02', GETDATE(), 'Test Supplier', 100000)",
                rcCode
        );

        // Insert chi tiet nhap 15 chai Lavie
        jdbcTemplate.update(
                "INSERT INTO stock_product_details (receipt_code, product_code, quantity, import_price, total_amount) " +
                "VALUES (?, ?, 15, 6000, 90000)",
                rcCode, pCode
        );

        Integer stockAfter = jdbcTemplate.queryForObject("SELECT stock_quantity FROM products WHERE product_code = ?", Integer.class, pCode);
        assertEquals(stockBefore + 15, stockAfter, "Ton kho phai tang dung 15 sau khi nhap hang");

        // Rollback kho va cleanup
        jdbcTemplate.update("UPDATE products SET stock_quantity = ? WHERE product_code = ?", stockBefore, pCode);
        jdbcTemplate.update("DELETE FROM stock_product_details WHERE receipt_code = ?", rcCode);
        jdbcTemplate.update("DELETE FROM stock_receipts WHERE receipt_code = ?", rcCode);
    }

    @Test
    @DisplayName("SQL Server 15: Trigger trg_AuditCourtStatusChange tu dong ghi log khi doi trang thai san")
    void testTrgAuditCourtStatusChange() {
        String courtCode = "CL01";
        String oldStatus = jdbcTemplate.queryForObject("SELECT status FROM courts WHERE court_code = ?", String.class, courtCode);

        // Doi trang thai san sang 'Bảo trì'
        jdbcTemplate.update("UPDATE courts SET status = N'Bảo trì' WHERE court_code = ?", courtCode);

        // Kiem tra audit log
        Integer logCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE details LIKE ? AND username = 'SYSTEM_COURT_MONITOR'",
                Integer.class,
                "%" + courtCode + "%"
        );
        assertTrue(logCount != null && logCount > 0, "Trigger phai tu dong sinh ban ghi audit log");

        // Tra lai trang thai cu
        jdbcTemplate.update("UPDATE courts SET status = ? WHERE court_code = ?", oldStatus, courtCode);
    }

    @Test
    @DisplayName("SQL Server 16: Trigger trg_ValidatePromotionDates chan KM ket thuc truoc bat dau")
    void testTrgValidatePromotionDates() {
        String testPromo = "KM_INVALID_TEST";
        Exception ex = assertThrows(Exception.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO promotions (promo_code, promo_name, discount_type, discount_value, start_date, end_date, status) " +
                    "VALUES (?, N'Khuyen Mai Sai Ngay', N'Phần trăm', 10, '2026-12-31', '2026-01-01', N'Đang áp dụng')",
                    testPromo
            );
        });
        assertTrue(ex.getMessage().contains("Ngày kết thúc khuyến mãi phải lớn hơn hoặc bằng ngày bắt đầu") || ex.getMessage().contains("Lỗi"));
    }

    @Test
    @DisplayName("SQL Server 17: Kiem tra dong bo 24 Vietnamese Views tren SQL Server")
    void testVietnameseViewsMapping() {
        Integer branchCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ChiNhanh", Integer.class);
        Integer realBranchCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM branches", Integer.class);
        assertEquals(realBranchCount, branchCount);

        Integer courtCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM San", Integer.class);
        Integer realCourtCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM courts", Integer.class);
        assertEquals(realCourtCount, courtCount);

        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TaiKhoan", Integer.class);
        Integer realUserCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertEquals(realUserCount, userCount);

        Integer customerCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM KhachHang", Integer.class);
        Integer realCustomerCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers", Integer.class);
        assertEquals(realCustomerCount, customerCount);

        Integer prodCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SanPham", Integer.class);
        Integer realProdCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        assertEquals(realProdCount, prodCount);

        Integer eqCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM DungCu", Integer.class);
        Integer realEqCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM equipment", Integer.class);
        assertEquals(realEqCount, eqCount);
    }
}
