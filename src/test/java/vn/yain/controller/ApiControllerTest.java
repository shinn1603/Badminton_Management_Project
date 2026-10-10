package vn.yain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.yain.entity.Booking;
import vn.yain.repository.BookingRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test cho ApiController (Bao phu cac API RESTful trong 10 Thiet ke xu ly)
 */
@SpringBootTest
class ApiControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private ApiController apiController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(apiController).build();
    }

    @Test
    @DisplayName("API XL-01: Dang nhap voi tai khoan khong ton tai tra ve 401 Unauthorized")
    void testApiLogin_Unauthorized() throws Exception {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "non_existing_user_9999");
        credentials.put("password", "invalid_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("API Tra cuu danh sach san theo chi nhanh CN01")
    void testApiGetCourts() throws Exception {
        mockMvc.perform(get("/api/courts?branchCode=CN01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("API XL-02: Khoa slot giu cho truc tuyen va tra ve ma VietQR coc 30%")
    void testApiSlotLock_Success() throws Exception {
        String testCourt = "CL01";
        String testSlot = "06:00 - 07:00";
        LocalDate testDate = LocalDate.now().plusDays(50);
        String dateStr = testDate.toString();

        // Xoa booking cu neu co de dam bao tinh doc lap khi chay test nhieu lan
        bookingRepository.findByCourtCodeAndBookingDate(testCourt, testDate)
                .forEach(b -> bookingRepository.delete(b));

        Map<String, Object> payload = new HashMap<>();
        payload.put("branchCode", "CN01");
        payload.put("courtCode", testCourt);
        payload.put("date", dateStr);
        payload.put("timeSlot", testSlot);
        payload.put("customerName", "Nguyen Van Test");
        payload.put("customerPhone", "0908888777");

        mockMvc.perform(post("/api/bookings/slot-lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.bookingCode", startsWith("DS")))
                .andExpect(jsonPath("$.courtCode", is(testCourt)))
                .andExpect(jsonPath("$.timeSlot", is(testSlot)))
                .andExpect(jsonPath("$.depositAmount", notNullValue()))
                .andExpect(jsonPath("$.qrUrl", containsString("vietqr.io")));
    }

    @Test
    @DisplayName("API XL-02: Tu choi khoa slot khi khung gio da co nguoi dat truoc (Chong trung lich)")
    void testApiSlotLock_Conflict() throws Exception {
        String testCourt = "CL02";
        String testSlot = "07:00 - 08:00";
        LocalDate futureDate = LocalDate.now().plusDays(20);

        // Tao san 1 booking truoc
        Booking b = new Booking();
        b.setBookingCode("DS_CONFLICT_TEST_" + System.currentTimeMillis() % 10000);
        b.setBranchCode("CN01");
        b.setCourtCode(testCourt);
        b.setCustomerName("Khach Hang Cu");
        b.setCustomerPhone("0901112223");
        b.setBookingDate(futureDate);
        b.setTimeSlot(testSlot);
        b.setHourlyPrice(new BigDecimal("100000"));
        b.setTotalPrice(new BigDecimal("100000"));
        b.setDepositAmount(new BigDecimal("30000"));
        b.setStatus("Chờ cọc 30%");
        bookingRepository.save(b);

        // Thu khoa lai dung khung gio va ngay do
        Map<String, Object> payload = new HashMap<>();
        payload.put("branchCode", "CN01");
        payload.put("courtCode", testCourt);
        payload.put("date", futureDate.toString());
        payload.put("timeSlot", testSlot);
        payload.put("customerName", "Khach Thu Hai");
        payload.put("customerPhone", "0901234567");

        mockMvc.perform(post("/api/bookings/slot-lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("đã có người đặt trước")));
    }

    @Test
    @DisplayName("API XL-03: Webhook VietQR tu dong doi trang thai sang Da coc 30%")
    void testApiWebhookVietQR_AutoConfirm() throws Exception {
        String uniqueBookingCode = "DS" + (200000 + (int)(Math.random() * 700000));
        Booking b = new Booking();
        b.setBookingCode(uniqueBookingCode);
        b.setBranchCode("CN01");
        b.setCourtCode("CL03");
        b.setCustomerName("Khach Webhook");
        b.setCustomerPhone("0905556667");
        b.setBookingDate(LocalDate.now());
        b.setTimeSlot("10:00 - 11:00");
        b.setHourlyPrice(new BigDecimal("100000"));
        b.setTotalPrice(new BigDecimal("100000"));
        b.setDepositAmount(new BigDecimal("30000"));
        b.setStatus("Chờ cọc 30%");
        bookingRepository.save(b);

        // Gia lap Webhook tu cong thanh toan Sepay / Casso / VietQR
        Map<String, Object> webhookPayload = new HashMap<>();
        webhookPayload.put("content", "CK " + uniqueBookingCode + " THANH TOAN COC SAN");
        webhookPayload.put("transferAmount", 30000);

        mockMvc.perform(post("/api/webhook/vietqr")
                        .header("X-Webhook-Secret", "UTE_VIETQR_SECRET_TOKEN_2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Kiem tra du lieu trong Database da duoc cap nhat sang Da coc 30%
        Booking updated = bookingRepository.findByBookingCode(uniqueBookingCode).orElseThrow();
        assertEquals("Đã cọc 30%", updated.getStatus());
    }

    @Test
    @DisplayName("API Tra cuu danh muc san pham va nuoc uong (XL-05)")
    void testApiGetProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("API XL-06: Chuyen san linh hoat qua API RESTful")
    void testApiTransferCourt_Success() throws Exception {
        String transferBookingCode = "DS_API_TRF_" + (System.currentTimeMillis() % 10000);
        LocalDate date = LocalDate.now().plusDays(30);

        Booking b = new Booking();
        b.setBookingCode(transferBookingCode);
        b.setBranchCode("CN01");
        b.setCourtCode("CL01");
        b.setCustomerName("Khach Chuyen San");
        b.setCustomerPhone("0903334445");
        b.setBookingDate(date);
        b.setTimeSlot("19:00 - 20:00");
        b.setHourlyPrice(new BigDecimal("90000"));
        b.setTotalPrice(new BigDecimal("90000"));
        b.setStatus("Đang sử dụng");
        bookingRepository.save(b);

        // Don sach booking cu neu co o san dich
        bookingRepository.findByCourtCodeAndBookingDate("CL02", date)
                .forEach(old -> bookingRepository.delete(old));

        Map<String, String> payload = new HashMap<>();
        payload.put("targetCourtCode", "CL02");
        payload.put("newTimeSlot", "19:00 - 20:00");

        mockMvc.perform(post("/api/bookings/" + transferBookingCode + "/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.newCourtCode", is("CL02")));
    }

    @Test
    @DisplayName("API XL-10: Nhap kho hang hoa phu kien qua API RESTful")
    void testApiInventoryIntake_Success() throws Exception {
        Map<String, Object> intakePayload = new HashMap<>();
        intakePayload.put("supplierName", "Cong Ty TNHH The Thao Viet");
        intakePayload.put("branchCode", "CN01");
        intakePayload.put("staffCode", "NS02");
        intakePayload.put("notes", "Nhap hang dau tuan");

        Map<String, Object> item = new HashMap<>();
        item.put("productCode", "P01"); // Sp co san trong seed data
        item.put("quantity", 10);
        item.put("importPrice", 15000);
        intakePayload.put("items", java.util.Collections.singletonList(item));

        mockMvc.perform(post("/api/inventory/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intakePayload)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.receipt.receiptCode", startsWith("PNK")));
    }

    @Test
    @DisplayName("API XL-08: Thong ke doanh thu va chot ca thu ngan")
    void testApiShiftEndpoints() throws Exception {
        // 1. Xem thong ke ca
        mockMvc.perform(get("/api/shifts/summary?staffCode=NVQ01&branchCode=CN01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staffCode", is("NVQ01")))
                .andExpect(jsonPath("$.systemCashTotal", notNullValue()));

        // 2. Chot ca
        Map<String, Object> closePayload = new HashMap<>();
        closePayload.put("staffCode", "NVQ01");
        closePayload.put("branchCode", "CN01");
        closePayload.put("actualCash", 1000000);
        closePayload.put("notes", "Chot ca ket thuc ngay");

        mockMvc.perform(post("/api/shifts/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.handoverCode", startsWith("BG")))
                .andExpect(jsonPath("$.status", is("CLOSED")));
    }

    @Test
    @DisplayName("API Cloudinary: Upload anh san va san pham")
    void testApiUploadImage() throws Exception {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "court-sample.jpg",
                "image/jpeg",
                "sample court photo bytes".getBytes()
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/upload/image")
                        .file(file)
                        .param("folder", "courts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.url", notNullValue()))
                .andExpect(jsonPath("$.folder", is("courts")));
    }

    @Test
    @DisplayName("API Staff & WorkShifts: Quan ly nhan su va xem lich truc tuan")
    void testApiStaffAndShifts_Success() throws Exception {
        // 1. Lay danh sach nhan su
        mockMvc.perform(get("/api/staff?branchCode=CN01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));

        // 2. Them nhan vien moi
        Map<String, String> staffPayload = new HashMap<>();
        staffPayload.put("staffCode", "NV-TEST-01");
        staffPayload.put("branchCode", "CN01");
        staffPayload.put("fullName", "Nhan Vien Test");
        staffPayload.put("position", "Thu ngan POS quầy");
        staffPayload.put("phone", "0909999888");
        staffPayload.put("email", "test@utesport.vn");

        mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.staff.staffCode", is("NV-TEST-01")));

        // 3. Lay lich truc tuan
        mockMvc.perform(get("/api/shifts/week?branchCode=CN01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.schedule.mon", notNullValue()));

        // 4. Phan ca truc
        Map<String, String> assignPayload = new HashMap<>();
        assignPayload.put("day", "mon");
        assignPayload.put("shift", "morning");
        assignPayload.put("staffCode", "NV-TEST-01");
        assignPayload.put("duty", "POS");
        assignPayload.put("icon", "POS");
        assignPayload.put("branchCode", "CN01");

        mockMvc.perform(post("/api/shifts/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("API Tournaments: Tra cuu danh sach va dang ky giai dau")
    void testApiTournaments_GetAndRegister() throws Exception {
        // 1. Lay danh sach giai dau
        mockMvc.perform(get("/api/tournaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$[0].tournamentCode", notNullValue()));

        // 2. Dang ky tham gia giai dau GD01
        Map<String, Object> regPayload = new HashMap<>();
        regPayload.put("tournamentCode", "GD01");
        regPayload.put("tournamentName", "Giai Cau Long Mua Thu UTE Open 2026");
        regPayload.put("teamName", "CLB UTE Masters Test");
        regPayload.put("category", "Doi Nam Nu");
        regPayload.put("player1Name", "Nguyen Van Test");
        regPayload.put("phone", "0908123456");

        mockMvc.perform(post("/api/tournaments/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.currentParticipants", notNullValue()))
                .andExpect(jsonPath("$.remainingSlots", notNullValue()));
    }
}
