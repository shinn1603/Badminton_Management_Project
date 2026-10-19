package vn.yain.sqlserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.yain.controller.ApiController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bo Kiem Thu Tich Hop Toan Bo Luong Nghiep Vu REST API Tren CSDL Microsoft SQL Server 2022
 * Bao phu:
 * 1. Xac thuc & Phan quyen 5 Role (Director, Manager, POS, Admin, Customer)
 * 2. Vong doi dat san truc tuyen (Khoa slot -> VietQR 30% -> Webhook -> Checkin -> Order F&B -> Checkout Hoa don)
 * 3. Chuyen san linh hoat & Chuyen khung gio
 * 4. Nhap kho tu NCC & Tu dong tang ton kho CSDL
 * 5. Chot ca thu ngan & Kiem ke tien mat
 * 6. Cau hinh he thong Admin & Tich hop AI Chatbot
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
class SqlServerApiWorkflowIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private ApiController apiController;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(apiController).build();
    }

    // ==========================================
    // 1. PHAN HE XAC THUC & PHAN QUYEN RBAC
    // ==========================================

    @Test
    @DisplayName("API SQL 01: Dang nhap 5 vai tro he thong (Director, Manager, POS, Admin, Customer)")
    void testAuthenticationAllRoles() throws Exception {
        String[] users = {"director", "manager", "pos", "admin", "customer"};
        for (String u : users) {
            Map<String, String> creds = Map.of("username", u, "password", "123456");
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(creds)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.username", is(u)));
        }
    }

    @Test
    @DisplayName("API SQL 02: Dang nhap sai mat khau tra ve 401 Unauthorized")
    void testLoginInvalidPassword() throws Exception {
        Map<String, String> creds = Map.of("username", "manager", "password", "wrong_pass_999");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("API SQL 03: Dang ky khach hang moi va tu dong luu vao bang customers trong SQL Server")
    void testRegisterNewCustomer() throws Exception {
        String uniquePhone = "098" + String.format("%07d", (int)(Math.random() * 10000000));
        Map<String, String> reg = new HashMap<>();
        reg.put("username", uniquePhone);
        reg.put("password", "Pass123456");
        reg.put("fullName", "Khach Hang Test SQL");
        reg.put("phone", uniquePhone);
        reg.put("email", uniquePhone + "@utesport.test");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Kiem tra du lieu thuc te trong bang customers cua SQL Server
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers WHERE phone = ?", Integer.class, uniquePhone);
        assertEquals(1, count, "Khach hang phai ton tai trong CSDL SQL Server");

        // Cleanup
        jdbcTemplate.update("DELETE FROM users WHERE username = ?", uniquePhone);
        jdbcTemplate.update("DELETE FROM customers WHERE phone = ?", uniquePhone);
    }

    // ==========================================
    // 2. PHAN HE DAT SAN TRUC TUYEN & THANH TOAN VIETQR
    // ==========================================

    @Test
    @DisplayName("API SQL 04: Tra cuu danh sach san theo chi nhanh CN01")
    void testGetCourtsByBranch() throws Exception {
        mockMvc.perform(get("/api/courts?branchCode=CN01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", isA(List.class)))
                .andExpect(jsonPath("$[0].branchCode", is("CN01")));
    }

    @Test
    @DisplayName("API SQL 05: Vong doi toan dien Don Dat San (Khoa slot -> Webhook VietQR -> Checkin -> Order Nuoc -> Checkout)")
    void testCompleteBookingApiLifecycle() throws Exception {
        String court = "CL02";
        LocalDate date = LocalDate.now().plusDays(45);
        String slot = "15:00 - 16:00";

        // Xoa booking cu de bao dam tinh lap lai
        jdbcTemplate.update("DELETE FROM bookings WHERE court_code = ? AND booking_date = ?", court, date.toString());

        // BƯỚC 1: Khóa slot đặt sân trực tuyến qua API REST
        Map<String, Object> lockReq = new HashMap<>();
        lockReq.put("branchCode", "CN01");
        lockReq.put("courtCode", court);
        lockReq.put("date", date.toString());
        lockReq.put("timeSlot", slot);
        lockReq.put("customerName", "Pham Van Lifecycle");
        lockReq.put("customerPhone", "0907123999");

        var lockResult = mockMvc.perform(post("/api/bookings/slot-lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.bookingCode", startsWith("DS")))
                .andExpect(jsonPath("$.qrUrl", containsString("vietqr.io")))
                .andExpect(jsonPath("$.depositAmount", notNullValue()))
                .andReturn();

        String respStr = lockResult.getResponse().getContentAsString();
        Map<String, Object> lockMap = objectMapper.readValue(respStr, Map.class);
        String bookingCode = (String) lockMap.get("bookingCode");
        Number depositNum = (Number) lockMap.get("depositAmount");

        assertNotNull(bookingCode);

        // BƯỚC 2: Thử khóa lại cùng slot đó -> Phải bị từ chối 400 Bad Request
        mockMvc.perform(post("/api/bookings/slot-lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("đã có người đặt trước")));

        // BƯỚC 3: Giả lập Webhook VietQR tự động xác nhận đặt cọc
        Map<String, Object> webhookReq = new HashMap<>();
        webhookReq.put("content", "CK " + bookingCode + " THANH TOAN COC SAN");
        webhookReq.put("transferAmount", depositNum.intValue());

        mockMvc.perform(post("/api/webhook/vietqr")
                        .header("X-Webhook-Secret", "UTE_VIETQR_SECRET_TOKEN_2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Kiểm tra CSDL SQL Server: trạng thái phải là 'Đã cọc 30%'
        String st1 = jdbcTemplate.queryForObject("SELECT status FROM bookings WHERE booking_code = ?", String.class, bookingCode);
        assertEquals("Đã cọc 30%", st1);

        // BƯỚC 4: Check-in nhận sân tại quầy POS
        mockMvc.perform(post("/api/bookings/" + bookingCode + "/checkin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.booking.status", is("Đang sử dụng")));

        // BƯỚC 5: Gọi dịch vụ F&B (mua 2 chai Lavie) -> Kiểm tra trừ tồn kho trong CSDL
        Integer stockBefore = jdbcTemplate.queryForObject("SELECT stock_quantity FROM products WHERE product_code = 'P01'", Integer.class);
        assertNotNull(stockBefore);

        Map<String, Object> serviceReq = Map.of("productCode", "P01", "quantity", 2);
        mockMvc.perform(post("/api/bookings/" + bookingCode + "/order-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serviceReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.remainingStock", is(stockBefore - 2)));

        Integer stockAfter = jdbcTemplate.queryForObject("SELECT stock_quantity FROM products WHERE product_code = 'P01'", Integer.class);
        assertEquals(stockBefore - 2, stockAfter, "Tồn kho trong SQL Server phải giảm đúng 2");

        // BƯỚC 6: Check-out trả sân & Xuất hóa đơn
        Map<String, Object> checkoutReq = new HashMap<>();
        checkoutReq.put("staffCode", "NS03");
        checkoutReq.put("paymentMethod", "Tiền mặt");
        checkoutReq.put("equipmentFee", 0);
        checkoutReq.put("productFee", 20000);

        mockMvc.perform(post("/api/bookings/" + bookingCode + "/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.invoice.invoiceCode", startsWith("HD")))
                .andExpect(jsonPath("$.invoice.status", is("Đã thanh toán")));

        // Kiểm tra trạng thái sân đã được giải phóng sang 'Trống'
        String courtStatus = jdbcTemplate.queryForObject("SELECT status FROM courts WHERE court_code = ?", String.class, court);
        assertEquals("Trống", courtStatus);

        // Dọn dẹp CSDL test
        jdbcTemplate.update("UPDATE products SET stock_quantity = ? WHERE product_code = 'P01'", stockBefore);
        jdbcTemplate.update("DELETE FROM invoices WHERE booking_code = ?", bookingCode);
        jdbcTemplate.update("DELETE FROM payments WHERE booking_code = ?", bookingCode);
        jdbcTemplate.update("DELETE FROM audit_logs WHERE details LIKE ?", "%" + bookingCode + "%");
        jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", bookingCode);
    }

    // ==========================================
    // 3. CHUYEN SAN LINH HOAT & CHUYEN GIO (XL-06)
    // ==========================================

    @Test
    @DisplayName("API SQL 06: Chuyen san linh hoat tu CL01 sang CL02 qua API")
    void testTransferCourtApi() throws Exception {
        String transferCode = "DS_TRF_" + (System.currentTimeMillis() % 10000);
        LocalDate d = LocalDate.now().plusDays(55);

        jdbcTemplate.update("DELETE FROM bookings WHERE court_code IN ('CL01', 'CL02') AND booking_date = ?", d.toString());

        jdbcTemplate.update(
                "INSERT INTO bookings (booking_code, court_code, branch_code, customer_name, customer_phone, booking_date, time_slot, hourly_price, total_price, deposit_amount, status) " +
                "VALUES (?, 'CL01', 'CN01', 'Khach Chuyen', '0901239876', ?, '19:00 - 20:00', 90000, 90000, 27000, N'Đã cọc 30%')",
                transferCode, d.toString()
        );

        Map<String, String> payload = Map.of("targetCourtCode", "CL02", "newTimeSlot", "19:00 - 20:00");
        mockMvc.perform(post("/api/bookings/" + transferCode + "/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.newCourtCode", is("CL02")));

        // Cleanup
        jdbcTemplate.update("DELETE FROM audit_logs WHERE details LIKE ?", "%" + transferCode + "%");
        jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", transferCode);
    }

    // ==========================================
    // 4. PHAN HE KHO HANG & NHAP KHO (XL-10)
    // ==========================================

    @Test
    @DisplayName("API SQL 07: Lap phieu nhap kho tu dong tang so luong ton kho san pham trong SQL Server")
    void testInventoryIntakeApi() throws Exception {
        Integer p01Initial = jdbcTemplate.queryForObject("SELECT stock_quantity FROM products WHERE product_code = 'P01'", Integer.class);
        assertNotNull(p01Initial);

        Map<String, Object> intake = new HashMap<>();
        intake.put("supplierName", "Nha Cung Cap Yonex Viet Nam");
        intake.put("branchCode", "CN01");
        intake.put("staffCode", "NS02");
        intake.put("notes", "Nhap kho dau tuan test SQL");

        Map<String, Object> item = Map.of("productCode", "P01", "quantity", 25, "importPrice", 7000);
        intake.put("items", List.of(item));

        var intakeRes = mockMvc.perform(post("/api/inventory/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intake)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.receipt.receiptCode", startsWith("PNK")))
                .andReturn();

        Map<String, Object> resMap = objectMapper.readValue(intakeRes.getResponse().getContentAsString(), Map.class);
        Map<String, Object> receiptMap = (Map<String, Object>) resMap.get("receipt");
        String rCode = (String) receiptMap.get("receiptCode");

        Integer p01After = jdbcTemplate.queryForObject("SELECT stock_quantity FROM products WHERE product_code = 'P01'", Integer.class);
        assertEquals(p01Initial + 25, p01After, "Ton kho P01 phai tang dung 25");

        // Cleanup
        jdbcTemplate.update("UPDATE products SET stock_quantity = ? WHERE product_code = 'P01'", p01Initial);
        jdbcTemplate.update("DELETE FROM stock_receipts WHERE receipt_code = ?", rCode);
    }

    // ==========================================
    // 5. CA TRUC VA CHOT CA THU NGAN (XL-08)
    // ==========================================

    @Test
    @DisplayName("API SQL 08: Chot ca thu ngan quay POS va kiem ke tien mat")
    void testShiftCloseApi() throws Exception {
        Map<String, Object> close = new HashMap<>();
        close.put("staffCode", "NS03");
        close.put("branchCode", "CN01");
        close.put("actualCash", 1500000);
        close.put("notes", "Chot ca chieu POS CN01");

        mockMvc.perform(post("/api/shifts/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(close)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.handoverCode", startsWith("BG")))
                .andExpect(jsonPath("$.status", is("CLOSED")));
    }

    // ==========================================
    // 6. GIAI DAU & CHATBOT GEMINI AI
    // ==========================================

    @Test
    @DisplayName("API SQL 09: Dang ky tham gia giai dau GD01 va kiem tra cap nhat so slot")
    void testTournamentRegisterApi() throws Exception {
        Map<String, Object> reg = new HashMap<>();
        reg.put("tournamentCode", "GD01");
        reg.put("tournamentName", "Giai Cau Long Mua Thu UTE Open 2026");
        reg.put("teamName", "Doi VDV Sao Vang Test");
        reg.put("category", "Doi Nam");
        reg.put("player1Name", "Nguyen Van A");
        reg.put("player2Name", "Tran Van B");
        reg.put("phone", "0908777666");

        mockMvc.perform(post("/api/tournaments/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.currentParticipants", notNullValue()));

        // Cleanup
        jdbcTemplate.update("DELETE FROM tournament_registrations WHERE phone = '0908777666'");
    }

    @Test
    @DisplayName("API SQL 10: Trợ lý Chatbot AI tra cuu thong tin san va dat lich")
    void testChatbotApi() throws Exception {
        Map<String, String> chatReq = Map.of("message", "Cho tôi hỏi chi nhánh Thủ Đức còn sân trống tối nay không?");
        mockMvc.perform(post("/api/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", notNullValue()));
    }
}
