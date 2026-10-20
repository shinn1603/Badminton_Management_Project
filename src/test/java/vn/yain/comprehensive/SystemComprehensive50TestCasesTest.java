package vn.yain.comprehensive;

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
import vn.yain.entity.Booking;
import vn.yain.entity.Court;
import vn.yain.entity.Product;
import vn.yain.entity.Staff;
import vn.yain.repository.*;
import vn.yain.security.JwtTokenProvider;
import vn.yain.service.DatabaseManagementService;
import vn.yain.service.EmailService;
import vn.yain.service.UserService;
import vn.yain.service.WarehouseService;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * =========================================================================
 * BỘ 50 TEST CASE TOÀN DIỆN HỆ THỐNG QUẢN LÝ CHUỖI SÂN CẦU LÔNG UTE SPORT
 * =========================================================================
 * Kiểm thử toàn diện 10 phân hệ chức năng:
 *  1. Xác thực & Phân quyền RBAC (TC01 - TC08)
 *  2. Khôi phục mật khẩu & OTP qua Email (TC09 - TC12)
 *  3. Chi nhánh, Sân & Danh mục sản phẩm/dụng cụ (TC13 - TC18)
 *  4. Khung giờ, Bảng giá & Tính khả dụng (TC19 - TC24)
 *  5. Đặt sân, VietQR & Xác nhận cọc 30% (TC25 - TC31)
 *  6. Check-in, Dịch vụ trong trận & Check-out hóa đơn (TC32 - TC36)
 *  7. Chính sách hủy sân, hoàn cọc & Chuyển sân (TC37 - TC40)
 *  8. Quản lý kho, Nhập hàng NCC & Tự động tăng tồn kho (TC41 - TC44)
 *  9. Ca trực thu ngân, Bàn giao tiền mặt & Báo cáo doanh thu (TC45 - TC47)
 * 10. Giải đấu, Trợ lý AI & Sao lưu CSDL an toàn (TC48 - TC50)
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
public class SystemComprehensive50TestCasesTest {

    private MockMvc mockMvc;

    @Autowired
    private ApiController apiController;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private DatabaseManagementService databaseManagementService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(apiController).build();
    }

    // =========================================================================
    // NHÓM 1: XÁC THỰC, PHÂN QUYỀN RBAC & BẢO MẬT (TC01 - TC08)
    // =========================================================================

    @Test
    @DisplayName("TC01: Đăng nhập thành công với vai trò Quản trị viên (ADMIN)")
    void test01_Auth_LoginSuccess_AdminRole() throws Exception {
        Map<String, String> creds = Map.of("username", "admin", "password", "123456");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.role", is("ADMIN")))
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    @DisplayName("TC02: Đăng nhập thành công với vai trò Giám đốc chuỗi (DIRECTOR)")
    void test02_Auth_LoginSuccess_DirectorRole() throws Exception {
        Map<String, String> creds = Map.of("username", "director", "password", "123456");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.role", is("DIRECTOR")));
    }

    @Test
    @DisplayName("TC03: Đăng nhập thành công với vai trò Quản lý chi nhánh (MANAGER)")
    void test03_Auth_LoginSuccess_ManagerRole() throws Exception {
        Map<String, String> creds = Map.of("username", "manager", "password", "123456");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.role", is("MANAGER")));
    }

    @Test
    @DisplayName("TC04: Đăng nhập thành công với vai trò Thu ngân (POS)")
    void test04_Auth_LoginSuccess_PosRole() throws Exception {
        Map<String, String> creds = Map.of("username", "pos", "password", "123456");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.role", is("POS")));
    }

    @Test
    @DisplayName("TC05: Đăng nhập thành công với vai trò Khách hàng (CUSTOMER)")
    void test05_Auth_LoginSuccess_CustomerRole() throws Exception {
        Map<String, String> creds = Map.of("username", "customer", "password", "123456");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.role", is("CUSTOMER")));
    }

    @Test
    @DisplayName("TC06: Đăng nhập thất bại khi nhập sai mật khẩu (Báo lỗi 401)")
    void test06_Auth_LoginFailure_InvalidPassword() throws Exception {
        Map<String, String> creds = Map.of("username", "admin", "password", "wrong_pass_999");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("TC07: Đăng nhập thất bại khi tài khoản không tồn tại trong hệ thống")
    void test07_Auth_LoginFailure_NonExistentUser() throws Exception {
        Map<String, String> creds = Map.of("username", "non_existent_user_xyz", "password", "123456");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("TC08: API /api/auth/me trả về thông tin danh tính từ JWT Token hợp lệ")
    void test08_Auth_GetMe_WithValidJwtToken() throws Exception {
        Map<String, Object> auth = userService.authenticate("admin", "123456");
        String token = (String) auth.get("token");
        assertNotNull(token);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.username", is("admin")));
    }

    // =========================================================================
    // NHÓM 2: KHÔI PHỤC MẬT KHẨU & OTP QUA EMAIL (TC09 - TC12)
    // =========================================================================

    @Test
    @DisplayName("TC09: Yêu cầu OTP khôi phục mật khẩu thành công với tài khoản có email")
    void test09_Auth_ForgotPassword_SuccessWithRegisteredUser() throws Exception {
        String testUser = "tc09_user_" + System.currentTimeMillis();
        String testEmail = "tc09_" + System.currentTimeMillis() + "@gmail.com";
        jdbcTemplate.update("""
            INSERT INTO users (username, password, full_name, email, phone, role, status, created_at)
            VALUES (?, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', N'Khách TC09', ?, '0911000111', 'CUSTOMER', N'Hoạt động', GETDATE())
        """, testUser, testEmail);

        try {
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("identifier", testUser))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.email", containsString("@")));
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE username = ?", testUser);
        }
    }

    @Test
    @DisplayName("TC10: Yêu cầu OTP thất bại khi định danh tài khoản không tồn tại")
    void test10_Auth_ForgotPassword_FailsWhenUserNotFound() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("identifier", "unknown_account_99999"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("TC11: Đặt lại mật khẩu bị từ chối nếu không có mã OTP hợp lệ")
    void test11_Auth_ResetPassword_FailsWithoutOtp() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "identifier", "customer",
                                "newPassword", "newSecret123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("TC12: Đặt lại mật khẩu thành công với OTP chính xác và đăng nhập mật khẩu mới")
    void test12_Auth_ResetPassword_SuccessWithValidOtpAndAllowsLogin() throws Exception {
        String testUser = "tc12_user_" + System.currentTimeMillis();
        String testEmail = "tc12_" + System.currentTimeMillis() + "@gmail.com";
        String testPhone = "0988" + (int)(Math.random() * 900000 + 100000);
        jdbcTemplate.update("""
            INSERT INTO users (username, password, full_name, email, phone, role, status, created_at)
            VALUES (?, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', N'Khách TC12', ?, ?, 'CUSTOMER', N'Hoạt động', GETDATE())
        """, testUser, testEmail, testPhone);

        try {
            // Bước 1: Yêu cầu mã OTP
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("identifier", testUser))))
                    .andExpect(status().isOk());

            // Bước 2: Lấy OTP nội bộ
            java.lang.reflect.Field otpField = ApiController.class.getDeclaredField("forgotPasswordOtpMap");
            otpField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> otpMap = (Map<String, Map<String, Object>>) otpField.get(apiController);
            String validOtp = (String) otpMap.get(testUser.toLowerCase()).get("otp");

            // Bước 2b: Xác thực mã OTP trước khi đổi mật khẩu (Nghiệp vụ mới 3 bước)
            mockMvc.perform(post("/api/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "identifier", testUser,
                                    "otp", validOtp
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));

            // Bước 3: Đặt lại mật khẩu với OTP
            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "identifier", testUser,
                                    "newPassword", "passTC12_OK",
                                    "otp", validOtp
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));

            // Bước 4: Đăng nhập thành công với mật khẩu mới
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("username", testUser, "password", "passTC12_OK"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE username = ?", testUser);
        }
    }

    // =========================================================================
    // NHÓM 3: CHI NHÁNH, SÂN & DANH MỤC SẢN PHẨM / DỤNG CỤ (TC13 - TC18)
    // =========================================================================

    @Test
    @DisplayName("TC13: Lấy danh sách toàn bộ chi nhánh chuỗi sân UTE Sport")
    void test13_Branch_GetAllBranches_ReturnsActiveBranches() throws Exception {
        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[0].branchCode", notNullValue()));
    }

    @Test
    @DisplayName("TC14: Kiểm tra chi tiết chi nhánh chính CN01 (Võ Văn Ngân, Thủ Đức)")
    void test14_Branch_GetBranchDetails_ReturnsBranchCN01() throws Exception {
        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.branchCode == 'CN01')].branchName", hasItem(containsString("Thủ Đức"))))
                .andExpect(jsonPath("$[?(@.branchCode == 'CN01')].address", hasItem(containsString("Võ Văn Ngân"))));
    }

    @Test
    @DisplayName("TC15: Lấy danh sách sân thi đấu theo chi nhánh CN01")
    void test15_Court_GetCourtsByBranch_ReturnsCourtList() throws Exception {
        mockMvc.perform(get("/api/courts").param("branch", "CN01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("TC16: Lấy chi tiết sân cầu lông CL01 kèm giá tiêu chuẩn")
    void test16_Court_GetCourtDetails_ReturnsValidCourt() throws Exception {
        Court court = courtRepository.findByCourtCode("CL01").orElse(null);
        assertNotNull(court);
        assertEquals("CL01", court.getCourtCode());
        assertTrue(court.getHourlyRate().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("TC17: Lấy danh mục sản phẩm đồ uống / phụ kiện thống nhất (catalog=true)")
    void test17_Product_GetProductsCatalog_ReturnsAvailableProducts() throws Exception {
        mockMvc.perform(get("/api/products").param("catalog", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].productCode", notNullValue()));
    }

    @Test
    @DisplayName("TC18: Lấy danh mục dụng cụ cho thuê (Vợt Yonex, Giày, Thảm)")
    void test18_Equipment_GetEquipmentList_ReturnsRacketsAndShoes() throws Exception {
        mockMvc.perform(get("/api/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // =========================================================================
    // NHÓM 4: KHUNG GIỜ, BẢNG GIÁ & TÍNH KHẢ DỤNG (TC19 - TC24)
    // =========================================================================

    @Test
    @DisplayName("TC19: Lấy danh sách 16 khung giờ hoạt động trong ngày (06:00 - 22:00)")
    void test19_Slot_GetTimeSlots_ReturnsDailySlots() throws Exception {
        mockMvc.perform(get("/api/bookings/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(greaterThanOrEqualTo(16))));
    }

    @Test
    @DisplayName("TC20: Bảng giá tổng hợp phân biệt giá Thường và giá Cao điểm")
    void test20_Pricing_GetPricingSummary_ReturnsNormalAndPeakRates() throws Exception {
        mockMvc.perform(get("/api/pricing/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.branchCode", is("CN01")))
                .andExpect(jsonPath("$.pricingRules", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("TC21: Áp dụng mã khuyến mãi hợp lệ KM01 (giảm 15% tổng tiền)")
    void test21_Promotion_ApplyPromotion_CalculatesDiscount() throws Exception {
        Map<String, Object> promoReq = Map.of(
                "promoCode", "KM01",
                "branchCode", "CN01",
                "orderAmount", 200000
        );
        mockMvc.perform(post("/api/promotions/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(promoReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.discountAmount", is(30000)));
    }

    @Test
    @DisplayName("TC22: SQL Function fn_CheckSlotAvailability trả về 1 cho khung giờ trống tương lai")
    void test22_SqlFunction_fnCheckSlotAvailability_ReturnsOneForFreeSlot() {
        LocalDate farDate = LocalDate.now().plusDays(150);
        Integer avail = jdbcTemplate.queryForObject(
                "SELECT dbo.fn_CheckSlotAvailability('CL01', ?, '06:00 - 07:00')",
                Integer.class,
                farDate.toString()
        );
        assertNotNull(avail);
        assertEquals(1, avail, "Khung giờ chưa có đơn đặt phải khả dụng (1)");
    }

    @Test
    @DisplayName("TC23: Chống trùng lịch đặt sân trên cùng 1 sân và khung giờ")
    void test23_Booking_CheckSlotOverlap_PreventsDoubleBooking() {
        LocalDate testDate = LocalDate.now().plusDays(25);
        String slot = "10:00 - 11:00";
        String code1 = "DS_DUP1_" + System.currentTimeMillis();
        String code2 = "DS_DUP2_" + System.currentTimeMillis();

        jdbcTemplate.update("""
            INSERT INTO bookings (booking_code, customer_name, customer_phone, branch_code, court_code, booking_date, time_slot, total_price, deposit_amount, status)
            VALUES (?, N'Khách 1', '0901111222', 'CN01', 'CL01', ?, ?, 100000, 30000, N'Đã cọc 30%')
        """, code1, testDate.toString(), slot);

        try {
            Integer checkAvail = jdbcTemplate.queryForObject(
                    "SELECT dbo.fn_CheckSlotAvailability('CL01', ?, ?)",
                    Integer.class,
                    testDate.toString(), slot
            );
            assertEquals(0, checkAvail, "Sau khi có đơn cọc, slot phải trả về 0 (Đã bị chiếm)");
        } finally {
            jdbcTemplate.update("DELETE FROM bookings WHERE booking_code IN (?, ?)", code1, code2);
        }
    }

    @Test
    @DisplayName("TC24: Khóa giữ chỗ tạm thời (Slot-Lock) cho thanh toán trực tuyến")
    void test24_Booking_SlotLock_LocksSlotTemporarily() throws Exception {
        String testPhone = "0903333444";
        Map<String, Object> lockReq = Map.of(
                "courtCode", "CL02",
                "branchCode", "CN01",
                "date", LocalDate.now().plusDays(10).toString(),
                "timeSlot", "14:00 - 15:00",
                "customerPhone", testPhone
        );
        try {
            mockMvc.perform(post("/api/bookings/slot-lock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(lockReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));
        } finally {
            jdbcTemplate.update("DELETE FROM bookings WHERE customer_phone = ?", testPhone);
        }
    }

    // =========================================================================
    // NHÓM 5: ĐẶT SÂN, VIETQR & XÁC NHẬN CỌC 30% (TC25 - TC31)
    // =========================================================================

    @Test
    @DisplayName("TC25: Đặt sân trực tuyến tạo đơn mới ở trạng thái Chờ thanh toán cọc")
    void test25_Booking_CreateBooking_ReturnsCreatedBookingWithPendingDeposit() throws Exception {
        String testCode = "DS_TC25_" + System.currentTimeMillis();
        Map<String, Object> req = Map.of(
                "bookingCode", testCode,
                "courtCode", "CL01",
                "branchCode", "CN01",
                "bookingDate", LocalDate.now().plusDays(15).toString(),
                "timeSlot", "08:00 - 09:00",
                "customerName", "Nguyễn Văn TC25",
                "customerPhone", "0901234567",
                "totalPrice", 100000,
                "depositAmount", 30000
        );
        try {
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingCode", is(testCode)));
        } finally {
            jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", testCode);
        }
    }

    @Test
    @DisplayName("TC26: Lấy thông tin chi tiết đơn đặt sân theo mã bookingCode")
    void test26_Booking_GetBookingByCode_ReturnsCorrectDetails() throws Exception {
        String testCode = "DS_TC26_" + System.currentTimeMillis();
        jdbcTemplate.update("""
            INSERT INTO bookings (booking_code, customer_name, customer_phone, branch_code, court_code, booking_date, time_slot, total_price, deposit_amount, status)
            VALUES (?, N'Nguyễn Văn TC26', '0901234567', 'CN01', 'CL01', ?, '09:00 - 10:00', 120000, 36000, N'Chờ thanh toán cọc')
        """, testCode, LocalDate.now().plusDays(16).toString());

        try {
            mockMvc.perform(get("/api/bookings/" + testCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.booking.bookingCode", is(testCode)))
                    .andExpect(jsonPath("$.booking.courtCode", is("CL01")));
        } finally {
            jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", testCode);
        }
    }

    @Test
    @DisplayName("TC27: Webhook VietQR xác nhận nạp tiền cọc 30% và cập nhật trạng thái Đã cọc 30%")
    void test27_Booking_VietQrWebhook_ConfirmsThirtyPercentDeposit() throws Exception {
        String testCode = "DS" + (100000 + (int)(System.currentTimeMillis() % 900000));
        jdbcTemplate.update("""
            INSERT INTO bookings (booking_code, customer_name, customer_phone, branch_code, court_code, booking_date, time_slot, total_price, deposit_amount, status)
            VALUES (?, N'Nguyễn Văn TC27', '0901234567', 'CN01', 'CL01', ?, '10:00 - 11:00', 100000, 30000, N'Chờ thanh toán cọc')
        """, testCode, LocalDate.now().plusDays(17).toString());

        try {
            Map<String, Object> webhookReq = Map.of(
                    "content", "CK " + testCode + " THANH TOAN COC",
                    "transferAmount", 30000,
                    "referenceCode", "TX_TEST_27"
            );
            mockMvc.perform(post("/api/webhook/vietqr")
                            .header("X-Webhook-Secret", "UTE_VIETQR_SECRET_TOKEN_2026")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(webhookReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));

            String status = jdbcTemplate.queryForObject("SELECT status FROM bookings WHERE booking_code = ?", String.class, testCode);
            assertEquals("Đã cọc 30%", status);
        } finally {
            jdbcTemplate.update("DELETE FROM payments WHERE booking_code = ?", testCode);
            jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", testCode);
        }
    }

    @Test
    @DisplayName("TC28: Gửi phiếu xác nhận & vé điện tử E-Ticket về Email chỉ định thành công")
    void test28_Booking_SendTicketEmail_UpdatesEmailAndSendsETicket() throws Exception {
        String testCode = "DS_TC28_" + System.currentTimeMillis();
        jdbcTemplate.update("""
            INSERT INTO bookings (booking_code, customer_name, customer_phone, branch_code, court_code, booking_date, time_slot, total_price, deposit_amount, status)
            VALUES (?, N'Nguyễn Văn TC28', '0901234567', 'CN01', 'CL01', ?, '11:00 - 12:00', 100000, 30000, N'Đã cọc 30%')
        """, testCode, LocalDate.now().plusDays(18).toString());

        try {
            mockMvc.perform(post("/api/bookings/" + testCode + "/send-ticket-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("email", "khachhang_tc28@gmail.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.email", is("khachhang_tc28@gmail.com")));

            String savedEmail = jdbcTemplate.queryForObject("SELECT customer_email FROM bookings WHERE booking_code = ?", String.class, testCode);
            assertEquals("khachhang_tc28@gmail.com", savedEmail);
        } finally {
            jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", testCode);
        }
    }

    @Test
    @DisplayName("TC29: Gửi vé thất bại khi đơn không có email và payload không cung cấp email")
    void test29_Booking_SendTicketEmail_FailsWhenEmailMissing() throws Exception {
        String testCode = "DS_TC29_" + System.currentTimeMillis();
        jdbcTemplate.update("""
            INSERT INTO bookings (booking_code, customer_name, customer_phone, branch_code, court_code, booking_date, time_slot, total_price, deposit_amount, status)
            VALUES (?, N'Khách Không Mail', '0900000000', 'CN01', 'CL01', ?, '12:00 - 13:00', 100000, 30000, N'Đã cọc 30%')
        """, testCode, LocalDate.now().plusDays(19).toString());

        try {
            mockMvc.perform(post("/api/bookings/" + testCode + "/send-ticket-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        } finally {
            jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", testCode);
        }
    }

    @Test
    @DisplayName("TC30: Tra cứu lịch sử đặt sân của khách hàng")
    void test30_Booking_CustomerMyBookings_ReturnsCustomerBookingHistory() throws Exception {
        Map<String, Object> auth = userService.authenticate("customer", "123456");
        String customerToken = (String) auth.get("token");
        assertNotNull(customerToken);

        mockMvc.perform(get("/api/customer/my-bookings")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("TC31: Chuyển sân linh hoạt (Court Transfer) sang sân mới khả dụng")
    void test31_Booking_TransferCourtOrSlot_SuccessfullyTransfers() throws Exception {
        String testCode = "DS_TC31_" + System.currentTimeMillis();
        LocalDate matchDate = LocalDate.now().plusDays(20);
        jdbcTemplate.update("""
            INSERT INTO bookings (booking_code, customer_name, customer_phone, branch_code, court_code, booking_date, time_slot, total_price, deposit_amount, status)
            VALUES (?, N'Nguyễn Văn TC31', '0901234567', 'CN01', 'CL01', ?, '14:00 - 15:00', 100000, 30000, N'Đã cọc 30%')
        """, testCode, matchDate.toString());

        try {
            Map<String, String> transferReq = Map.of(
                    "targetCourtCode", "CL02",
                    "newTimeSlot", "15:00 - 16:00"
            );
            mockMvc.perform(post("/api/bookings/" + testCode + "/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transferReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));

            String court = jdbcTemplate.queryForObject("SELECT court_code FROM bookings WHERE booking_code = ?", String.class, testCode);
            assertEquals("CL02", court);
        } finally {
            jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", testCode);
        }
    }

    // =========================================================================
    // NHÓM 6: CHECK-IN, DỊCH VỤ TRONG TRẬN & CHECK-OUT HÓA ĐƠN (TC32 - TC36)
    // =========================================================================

    @Test
    @DisplayName("TC32: Check-in nhận sân tại quầy POS cập nhật đơn và sân sang Đang sử dụng")
    void test32_Checkin_PosCheckin_SetsBookingAndCourtToInUse() throws Exception {
        String testCode = "DS_TC32_" + System.currentTimeMillis();
        jdbcTemplate.update("""
            INSERT INTO bookings (booking_code, customer_name, customer_phone, branch_code, court_code, booking_date, time_slot, total_price, deposit_amount, status)
            VALUES (?, N'Nguyễn Văn TC32', '0901234567', 'CN01', 'CL01', ?, '16:00 - 17:00', 100000, 30000, N'Đã cọc 30%')
        """, testCode, LocalDate.now().toString());

        try {
            mockMvc.perform(post("/api/bookings/" + testCode + "/checkin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.booking.status", is("Đang sử dụng")));

            String courtStatus = jdbcTemplate.queryForObject("SELECT status FROM courts WHERE court_code = 'CL01'", String.class);
            assertEquals("Đang sử dụng", courtStatus);
        } finally {
            courtRepository.findByCourtCode("CL01").ifPresent(c -> { c.setStatus("Trống"); courtRepository.save(c); });
            jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", testCode);
        }
    }

    @Test
    @DisplayName("TC33: Gọi dịch vụ F&B trong trận đấu làm giảm tồn kho sản phẩm trong CSDL")
    void test33_Service_OrderProductDuringMatch_DeductsInventoryStock() throws Exception {
        String testCode = "DS_TC33_" + System.currentTimeMillis();
        jdbcTemplate.update("""
            INSERT INTO bookings (booking_code, customer_name, customer_phone, branch_code, court_code, booking_date, time_slot, total_price, deposit_amount, status)
            VALUES (?, N'Nguyễn Văn TC33', '0901234567', 'CN01', 'CL01', ?, '17:00 - 18:00', 100000, 30000, N'Đang sử dụng')
        """, testCode, LocalDate.now().toString());

        Integer stockBefore = jdbcTemplate.queryForObject("SELECT stock_quantity FROM products WHERE product_code = 'P01'", Integer.class);
        assertNotNull(stockBefore);

        try {
            mockMvc.perform(post("/api/bookings/" + testCode + "/order-service")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("productCode", "P01", "quantity", 1))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));

            Integer stockAfter = jdbcTemplate.queryForObject("SELECT stock_quantity FROM products WHERE product_code = 'P01'", Integer.class);
            assertEquals(stockBefore - 1, stockAfter);
        } finally {
            jdbcTemplate.update("UPDATE products SET stock_quantity = ? WHERE product_code = 'P01'", stockBefore);
            jdbcTemplate.update("DELETE FROM booking_services WHERE booking_code = ?", testCode);
            jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", testCode);
        }
    }

    @Test
    @DisplayName("TC34: Check-out trả sân & Xuất hóa đơn giải phóng sân về Trạng thái Trống")
    void test34_Checkout_PosCheckout_GeneratesInvoiceAndFreesCourt() throws Exception {
        String testCode = "DS_TC34_" + System.currentTimeMillis();
        jdbcTemplate.update("""
            INSERT INTO bookings (booking_code, customer_name, customer_phone, branch_code, court_code, booking_date, time_slot, total_price, deposit_amount, status)
            VALUES (?, N'Nguyễn Văn TC34', '0901234567', 'CN01', 'CL01', ?, '18:00 - 19:00', 100000, 30000, N'Đang sử dụng')
        """, testCode, LocalDate.now().toString());

        try {
            Map<String, Object> checkoutReq = Map.of(
                    "staffCode", "NS03",
                    "paymentMethod", "Tiền mặt",
                    "equipmentFee", 0,
                    "productFee", 15000
            );
            mockMvc.perform(post("/api/bookings/" + testCode + "/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(checkoutReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.invoice.status", is("Đã thanh toán")));

            String courtStatus = jdbcTemplate.queryForObject("SELECT status FROM courts WHERE court_code = 'CL01'", String.class);
            assertEquals("Trống", courtStatus);
        } finally {
            jdbcTemplate.update("DELETE FROM invoices WHERE booking_code = ?", testCode);
            jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", testCode);
        }
    }

    @Test
    @DisplayName("TC35: Lấy danh sách hóa đơn thanh toán trong hệ thống")
    void test35_Invoice_GetInvoicesList_ReturnsPersistedInvoices() throws Exception {
        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("TC36: Lập hóa đơn bán lẻ dịch vụ tại quầy POS không qua đặt sân")
    void test36_Invoice_RetailSale_GeneratesRetailInvoice() throws Exception {
        Map<String, Object> retailReq = Map.of(
                "branchCode", "CN01",
                "staffCode", "NS03",
                "customerPhone", "0901234567",
                "customerName", "Khách Vãng Lai",
                "paymentMethod", "Tiền mặt",
                "totalAmount", 50000
        );
        try {
            mockMvc.perform(post("/api/invoices/retail")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(retailReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.invoice.invoiceCode", notNullValue()));
        } finally {
            jdbcTemplate.update("DELETE FROM invoices WHERE booking_code IS NULL AND staff_code = 'NS03' AND total_payment = 50000");
        }
    }

    // =========================================================================
    // NHÓM 7: CHÍNH SÁCH HỦY SÂN, HOÀN CỌC & QUY ĐỊNH 4 TIẾNG (TC37 - TC40)
    // =========================================================================

    @Test
    @DisplayName("TC37: Hủy sân trước giờ đấu > 4 tiếng được chấp thuận và hoàn trả trạng thái sân")
    void test37_Cancel_CancelBookingOverFourHours_GivesFullRefund() throws Exception {
        String testCode = "DS_TC37_" + System.currentTimeMillis();
        LocalDate matchDate = LocalDate.now().plusDays(5);
        jdbcTemplate.update("""
            INSERT INTO bookings (booking_code, customer_name, customer_phone, branch_code, court_code, booking_date, time_slot, total_price, deposit_amount, status)
            VALUES (?, N'Khách TC37', '0901234567', 'CN01', 'CL01', ?, '19:00 - 20:00', 100000, 30000, N'Đã cọc 30%')
        """, testCode, matchDate.toString());

        try {
            mockMvc.perform(post("/api/bookings/" + testCode + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("reason", "Khách bận việc gia đình"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.booking.status", is("Đã hủy")));

            String courtStatus = jdbcTemplate.queryForObject("SELECT status FROM courts WHERE court_code = 'CL01'", String.class);
            assertEquals("Trống", courtStatus);
        } finally {
            jdbcTemplate.update("DELETE FROM audit_logs WHERE details LIKE ?", "%" + testCode + "%");
            jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", testCode);
        }
    }

    @Test
    @DisplayName("TC38: Cấu hình hệ thống CFG_CANCEL_HOURS quy định chuẩn 4 tiếng hủy hoàn cọc")
    void test38_Cancel_CancelPolicyConfigurationIsFourHours() throws Exception {
        mockMvc.perform(get("/api/policy/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settings.CFG_CANCEL_HOURS", is("4")));
    }

    @Test
    @DisplayName("TC39: Từ chối hủy đơn khi đơn đặt sân đã hoàn tất thanh toán")
    void test39_Cancel_RejectsCancellingCompletedBooking() throws Exception {
        String testCode = "DS_TC39_" + System.currentTimeMillis();
        jdbcTemplate.update("""
            INSERT INTO bookings (booking_code, customer_name, customer_phone, branch_code, court_code, booking_date, time_slot, total_price, deposit_amount, status)
            VALUES (?, N'Khách TC39', '0901234567', 'CN01', 'CL01', ?, '20:00 - 21:00', 100000, 30000, N'Hoàn thành')
        """, testCode, LocalDate.now().toString());

        try {
            mockMvc.perform(post("/api/bookings/" + testCode + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("reason", "Muốn hủy sau khi chơi"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        } finally {
            jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", testCode);
        }
    }

    @Test
    @DisplayName("TC40: Từ chối chuyển sân khi sân đích đang bị xung đột lịch hoặc bảo trì")
    void test40_Booking_TransferCourt_RejectsConflictTarget() throws Exception {
        String testCode = "DS_TC40_" + System.currentTimeMillis();
        LocalDate matchDate = LocalDate.now().plusDays(22);
        jdbcTemplate.update("""
            INSERT INTO bookings (booking_code, customer_name, customer_phone, branch_code, court_code, booking_date, time_slot, total_price, deposit_amount, status)
            VALUES (?, N'Khách TC40', '0901234567', 'CN01', 'CL01', ?, '10:00 - 11:00', 100000, 30000, N'Đã cọc 30%')
        """, testCode, matchDate.toString());

        try {
            // Chuyển tới cùng sân và cùng khung giờ -> phải báo lỗi
            Map<String, String> transferReq = Map.of(
                    "targetCourtCode", "CL01",
                    "newTimeSlot", "10:00 - 11:00"
            );
            mockMvc.perform(post("/api/bookings/" + testCode + "/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transferReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        } finally {
            jdbcTemplate.update("DELETE FROM bookings WHERE booking_code = ?", testCode);
        }
    }

    // =========================================================================
    // NHÓM 8: NHẬP HÀNG KHO & TỰ ĐỘNG CẬP NHẬT TỒN KHO CSDL (TC41 - TC44)
    // =========================================================================

    @Test
    @DisplayName("TC41: Lập phiếu nhập kho từ nhà cung cấp và tăng tồn kho CSDL chính xác")
    void test41_Warehouse_CreateStockReceipt_IncreasesProductStock() throws Exception {
        Integer stockBefore = jdbcTemplate.queryForObject("SELECT stock_quantity FROM products WHERE product_code = 'P01'", Integer.class);
        assertNotNull(stockBefore);

        Map<String, Object> intakeReq = Map.of(
                "supplierName", "Công ty TNHH Thể Thao Động Lực",
                "branchCode", "CN01",
                "staffCode", "NS03",
                "notes", "Nhập bổ sung nước giải khát TC41",
                "items", List.of(
                        Map.of("productCode", "P01", "quantity", 10, "unitPrice", 8000, "type", "PRODUCT")
                )
        );

        mockMvc.perform(post("/api/inventory/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intakeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.receipt.receiptCode", notNullValue()));

        Integer stockAfter = jdbcTemplate.queryForObject("SELECT stock_quantity FROM products WHERE product_code = 'P01'", Integer.class);
        assertEquals(stockBefore + 10, stockAfter);

        // Khôi phục số lượng
        jdbcTemplate.update("UPDATE products SET stock_quantity = ? WHERE product_code = 'P01'", stockBefore);
    }

    @Test
    @DisplayName("TC42: Cơ chế tự động phân bổ danh mục tránh lỗi 'Không tìm thấy sản phẩm'")
    void test42_Warehouse_AutoAllocateProductAcrossBranches() {
        assertDoesNotThrow(() -> {
            List<Map<String, Object>> items = List.of(
                    Map.of("productCode", "P02", "quantity", 5, "unitPrice", 12000, "type", "PRODUCT")
            );
            warehouseService.createStockReceipt("NCC Test TC42", "CN01", "NS03", items, "Test auto allocate");
        });
    }

    @Test
    @DisplayName("TC43: Truy vấn lịch sử các phiếu nhập kho theo chi nhánh CN01")
    void test43_Warehouse_GetStockReceipts_ReturnsReceiptWithLineItems() throws Exception {
        mockMvc.perform(get("/api/inventory/receipts").param("branchCode", "CN01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("TC44: Bảng stock_product_details lưu trữ chi tiết từng mặt hàng nhập kho")
    void test44_Warehouse_StockProductDetailsPersistence() {
        Long detailCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stock_product_details", Long.class);
        assertNotNull(detailCount);
        assertTrue(detailCount > 0, "Bảng stock_product_details phải có dữ liệu chi tiết nhập kho");
    }

    // =========================================================================
    // NHÓM 9: CA TRỰC THU NGÂN, BÀN GIAO TIỀN MẶT & BÁO CÁO (TC45 - TC47)
    // =========================================================================

    @Test
    @DisplayName("TC45: Lấy thông tin tóm tắt ca trực thu ngân (Doanh thu tiền mặt, thẻ, QR)")
    void test45_Shift_GetShiftSummary_ReturnsActiveShiftData() throws Exception {
        mockMvc.perform(get("/api/shifts/summary")
                        .param("staffCode", "NS03")
                        .param("branchCode", "CN01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staffCode", is("NS03")))
                .andExpect(jsonPath("$.branchCode", is("CN01")));
    }

    @Test
    @DisplayName("TC46: Kết ca thu ngân & Kiểm kê số tiền thực tế trong két")
    void test46_Shift_CloseShift_CalculatesCashDrawerBalance() throws Exception {
        Map<String, Object> closeReq = Map.of(
                "staffCode", "NS03",
                "branchCode", "CN01",
                "actualCash", 500000,
                "notes", "Bàn giao ca tối TC46"
        );
        mockMvc.perform(post("/api/shifts/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("TC47: Báo cáo doanh thu chuỗi sân cho cấp Quản lý & Giám đốc")
    void test47_Manager_GetRevenueReports_AggregatesData() throws Exception {
        mockMvc.perform(get("/api/manager/reports"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // NHÓM 10: GIẢI ĐẤU, TRỢ LÝ AI & SAO LƯU CSDL AN TOÀN (TC48 - TC50)
    // =========================================================================

    @Test
    @DisplayName("TC48: Đăng ký tham gia giải đấu và tự động cập nhật số lượng VĐV")
    void test48_Tournament_RegisterParticipant_IncreasesParticipantCount() throws Exception {
        jdbcTemplate.update("UPDATE tournaments SET end_date = '2026-12-31 23:59:59', status = N'Đang mở đăng ký', current_participants = 10, max_participants = 32 WHERE tournament_code = 'GD01'");
        String testPhone = "0977" + (int)(Math.random() * 900000 + 100000);
        Map<String, Object> regReq = Map.of(
                "tournamentCode", "GD01",
                "teamName", "CLB Cầu Lông Thủ Đức TC48",
                "category", "Đôi Nam",
                "player1Name", "Lê Văn A",
                "player2Name", "Lê Văn B",
                "phone", testPhone
        );
        try {
            mockMvc.perform(post("/api/tournaments/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(regReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.tournamentCode", is("GD01")));
        } finally {
            jdbcTemplate.update("DELETE FROM tournament_registrations WHERE phone = ?", testPhone);
        }
    }

    @Test
    @DisplayName("TC49: Trợ lý Chatbot AI phản hồi thông tin tra cứu sân và hướng dẫn đặt")
    void test49_Chatbot_SendMessage_ReturnsAiAssistanceResponse() throws Exception {
        Map<String, String> chatReq = Map.of("message", "Cho tôi hỏi chi nhánh 1 có những loại sân nào?");
        mockMvc.perform(post("/api/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", notNullValue()));
    }

    @Test
    @DisplayName("TC50: Tạo bản sao lưu Snapshot CSDL an toàn (.sql) lưu vĩnh viễn trên đĩa")
    void test50_Database_CreateBackupSnapshot_GeneratesSqlFileOnDisk() throws Exception {
        String backupName = "backup_tc50_test_" + System.currentTimeMillis() + ".sql";
        Map<String, Object> result = databaseManagementService.createBackupSnapshot(backupName, "admin");
        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));

        File file = databaseManagementService.getBackupFile(backupName);
        assertNotNull(file);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        // Dọn dẹp tệp test
        file.delete();
    }
}
