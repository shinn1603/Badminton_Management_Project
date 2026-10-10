package vn.yain.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.yain.dto.CourtEventDto;
import vn.yain.entity.*;
import vn.yain.repository.*;
import vn.yain.repository.CourtRepository;
import vn.yain.repository.BookingRepository;
import vn.yain.security.JwtTokenProvider;
import vn.yain.service.BookingService;
import vn.yain.service.CourtService;
import vn.yain.service.PricingService;
import vn.yain.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import vn.yain.service.CloudinaryService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    @Value("${vietqr.webhook.token:UTE_VIETQR_SECRET_TOKEN_2026}")
    private String webhookToken;

    private String sanitizeErrorMessage(Exception e) {
        log.error("Internal processing error: {}", e.getMessage(), e);
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            return e.getMessage();
        }
        return "Đã xảy ra lỗi trong quá trình xử lý hệ thống. Vui lòng liên hệ ban quản trị.";
    }

    @Autowired
    private UserService userService;

    @Autowired
    private CourtService courtService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private vn.yain.service.ChatbotService chatbotService;

    @Autowired
    private vn.yain.service.WarehouseService warehouseService;

    @Autowired
    private vn.yain.service.ShiftService shiftService;

    @Autowired
    private CloudinaryService cloudinaryService;

    // ==========================================
    // 1. AUTHENTICATION & JWT ENDPOINTS
    // ==========================================

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");
            Map<String, Object> authResult = userService.authenticate(username, password);
            authResult.put("success", true);
            return ResponseEntity.ok(authResult);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", sanitizeErrorMessage(e)));
        }
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        try {
            String username = payload.get("username");
            String password = payload.get("password");
            String fullName = payload.get("fullName");
            String phone = payload.get("phone");
            String email = payload.get("email");

            if ((username == null || username.trim().isEmpty()) && phone != null) {
                username = phone.trim();
            }

            Map<String, Object> registerResult = userService.registerCustomer(username, password, fullName, phone, email);
            registerResult.put("success", true);
            registerResult.put("message", "Đăng ký tài khoản thành công!");
            return ResponseEntity.ok(registerResult);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", sanitizeErrorMessage(e)));
        }
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        try {
            String identifier = payload.get("identifier");
            if (identifier == null || identifier.trim().isEmpty()) {
                identifier = payload.get("username");
            }
            if (identifier == null || identifier.trim().isEmpty()) {
                identifier = payload.get("phone");
            }
            if (identifier == null || identifier.trim().isEmpty()) {
                identifier = payload.get("email");
            }

            String newPassword = payload.get("newPassword");
            if (newPassword == null) {
                newPassword = payload.get("password");
            }

            userService.resetPassword(identifier, newPassword);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đặt lại mật khẩu thành công! Quý khách vui lòng đăng nhập với mật khẩu mới."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", sanitizeErrorMessage(e)));
        }
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Chưa cung cấp JWT token hợp lệ!"));
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "JWT token đã hết hạn hoặc không hợp lệ!"));
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        String role = jwtTokenProvider.getRoleFromToken(token);
        User u = userRepository.findByUsername(username).orElse(null);
        String fullName = u != null ? u.getFullName() : username;
        String phone = u != null && u.getPhone() != null ? u.getPhone() : "";
        String email = u != null && u.getEmail() != null ? u.getEmail() : "";

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("username", username);
        resp.put("fullName", fullName);
        resp.put("phone", phone);
        resp.put("email", email);
        resp.put("role", role);
        resp.put("valid", true);
        if (u != null) {
            resp.put("userId", u.getId());
            resp.put("branchCode", u.getBranchCode());
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/customer/profile")
    public ResponseEntity<?> getCustomerProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập để xem thông tin hội viên!"));
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại!"));
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        User u = userRepository.findByUsername(username).orElse(null);
        if (u == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Không tìm thấy thông tin tài khoản!"));
        }

        Customer cust = null;
        if (u.getPhone() != null && !u.getPhone().trim().isEmpty()) {
            cust = customerRepository.findByPhone(u.getPhone().trim()).orElse(null);
        }
        if (cust == null) {
            cust = customerRepository.findByCustomerCode(u.getUsername()).orElse(null);
        }

        List<Booking> userBookings = new ArrayList<>();
        if (u.getPhone() != null && !u.getPhone().trim().isEmpty()) {
            userBookings = bookingRepository.findByCustomerPhoneOrderByBookingDateDesc(u.getPhone().trim());
        }
        if (userBookings.isEmpty() && u.getFullName() != null) {
            userBookings = bookingRepository.findByCustomerNameContainingIgnoreCase(u.getFullName().trim());
        }

        BigDecimal totalSpent = BigDecimal.ZERO;
        long totalMinutes = 0;
        for (Booking b : userBookings) {
            if (b.getTotalPrice() != null) {
                totalSpent = totalSpent.add(b.getTotalPrice());
            }
            if (b.getTimeSlot() != null && b.getTimeSlot().contains("-")) {
                int[] hrs = parseSlotHours(b.getTimeSlot());
                totalMinutes += Math.max(0, (hrs[1] - hrs[0]) * 60L);
            }
        }
        BigDecimal totalHours = BigDecimal.valueOf(totalMinutes / 60.0);
        if (cust != null && cust.getTotalPlayHours() != null && cust.getTotalPlayHours().compareTo(totalHours) > 0) {
            totalHours = cust.getTotalPlayHours();
        }

        int points = cust != null && cust.getRewardPoints() != null ? cust.getRewardPoints() : 0;
        if (points == 0 && totalSpent.compareTo(BigDecimal.ZERO) > 0) {
            points = totalSpent.divide(BigDecimal.valueOf(1000), 0, java.math.RoundingMode.FLOOR).intValue();
        }

        String tier = "Đồng";
        double hoursVal = totalHours.doubleValue();
        if (hoursVal >= 80) tier = "Kim Cương";
        else if (hoursVal >= 40) tier = "Vàng";
        else if (hoursVal >= 20) tier = "Bạc";
        else tier = "Đồng";

        if (cust != null && cust.getMembershipTier() != null && !cust.getMembershipTier().isBlank()) {
            tier = cust.getMembershipTier();
        }

        String customerCode = cust != null && cust.getCustomerCode() != null 
            ? cust.getCustomerCode() 
            : ("KH" + String.format("%04d", u.getId()));

        String cardNumber = "MB • 8892 • " + String.format("%04d", u.getId()) + " • 9988";
        String joinDate = u.getCreatedAt() != null 
            ? u.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
            : "15/01/2025";

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("fullName", u.getFullName());
        resp.put("phone", u.getPhone() != null ? u.getPhone() : "");
        resp.put("email", u.getEmail() != null ? u.getEmail() : "");
        resp.put("customerCode", customerCode);
        resp.put("cardNumber", cardNumber);
        resp.put("membershipTier", tier);
        resp.put("rewardPoints", points);
        resp.put("totalPlayHours", totalHours.setScale(1, java.math.RoundingMode.HALF_UP));
        resp.put("totalSpent", totalSpent);
        resp.put("savings", totalSpent.multiply(new BigDecimal("0.10")));
        resp.put("joinDate", joinDate);
        resp.put("tournamentCount", tournamentRepository.count());
        resp.put("bookingCount", userBookings.size());
        resp.put("recentBookings", userBookings);

        return ResponseEntity.ok(resp);
    }

    @PutMapping("/customer/profile")
    public ResponseEntity<?> updateCustomerProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> payload) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Chưa đăng nhập!"));
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Token không hợp lệ!"));
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        User u = userRepository.findByUsername(username).orElse(null);
        if (u == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Tài khoản không tồn tại!"));
        }

        String newName = payload.get("fullName");
        String newPhone = payload.get("phone");
        String newEmail = payload.get("email");

        if (newName != null && !newName.trim().isEmpty()) {
            u.setFullName(newName.trim());
        }
        if (newPhone != null && !newPhone.trim().isEmpty()) {
            u.setPhone(newPhone.trim());
        }
        if (newEmail != null) {
            u.setEmail(newEmail.trim());
        }
        userRepository.save(u);

        if (u.getPhone() != null && !u.getPhone().trim().isEmpty()) {
            Optional<Customer> cOpt = customerRepository.findByPhone(u.getPhone().trim());
            if (cOpt.isPresent()) {
                Customer c = cOpt.get();
                if (newName != null) c.setFullName(newName.trim());
                if (newEmail != null) c.setEmail(newEmail.trim());
                customerRepository.save(c);
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cập nhật hồ sơ thành công!",
                "fullName", u.getFullName(),
                "phone", u.getPhone() != null ? u.getPhone() : "",
                "email", u.getEmail() != null ? u.getEmail() : ""
        ));
    }

    @GetMapping("/customer/my-bookings")
    public ResponseEntity<?> getMyBookings(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Chưa đăng nhập!"));
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Token không hợp lệ!"));
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        User u = userRepository.findByUsername(username).orElse(null);
        if (u == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Booking> bookings = new ArrayList<>();
        if (u.getPhone() != null && !u.getPhone().trim().isEmpty()) {
            bookings = bookingRepository.findByCustomerPhoneOrderByBookingDateDesc(u.getPhone().trim());
        }
        if (bookings.isEmpty() && u.getFullName() != null) {
            bookings = bookingRepository.findByCustomerNameContainingIgnoreCase(u.getFullName().trim());
        }

        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/admin/users")
    public List<User> getAdminUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/admin/users")
    public ResponseEntity<?> saveAdminUser(@RequestBody Map<String, String> payload) {
        try {
            User u = userService.saveOrUpdateUser(payload);
            return ResponseEntity.ok(Map.of("success", true, "user", u));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/users/{username}/toggle-status")
    public ResponseEntity<?> toggleAdminUserStatus(@PathVariable String username) {
        try {
            User u = userService.toggleUserStatus(username);
            return ResponseEntity.ok(Map.of("success", true, "status", u.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==========================================
    // 2. COURTS & REALTIME STATUS (XL-02)
    // ==========================================

    @GetMapping("/courts")
    public List<Court> getCourts(@RequestParam(required = false) String branchCode) {
        return courtService.getCourts(branchCode);
    }

    @PostMapping("/courts/hold")
    public ResponseEntity<?> holdCourt(@RequestBody Map<String, String> payload) {
        try {
            String courtCode = payload.get("courtCode");
            String branchCode = payload.get("branchCode");
            String timeSlot = payload.get("timeSlot");
            String customerName = payload.get("customerName");

            CourtEventDto event = courtService.holdCourtForBooking(courtCode, branchCode, timeSlot, customerName);
            return ResponseEntity.ok(Map.of("success", true, "data", event));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/courts/status")
    public ResponseEntity<?> updateCourtStatus(@RequestBody Map<String, String> payload) {
        try {
            String courtCode = payload.get("courtCode");
            String status = payload.get("status");
            Court updated = courtService.updateCourtStatus(courtCode, status);
            return ResponseEntity.ok(Map.of("success", true, "court", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==========================================
    // 3. BOOKINGS & WORKFLOW (XL-03, XL-04, XL-07)
    // ==========================================

    @GetMapping("/bookings")
    public List<Booking> getBookings(
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (branchCode != null && !branchCode.trim().isEmpty() && date != null) {
            return bookingRepository.findByBranchCodeAndBookingDate(branchCode.trim(), date);
        } else if (branchCode != null && !branchCode.trim().isEmpty()) {
            return bookingRepository.findByBranchCodeOrderByBookingDateDesc(branchCode.trim());
        }
        return bookingService.getAllBookings();
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(@RequestBody Booking booking) {
        Booking saved = bookingService.createBooking(booking);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/bookings/{code}/deposit")
    public ResponseEntity<?> confirmDeposit(@PathVariable String code, @RequestBody Map<String, Object> payload) {
        try {
            BigDecimal amount = payload.get("amount") != null ? new BigDecimal(payload.get("amount").toString()) : null;
            String method = (String) payload.getOrDefault("paymentMethod", "VietQR Pro");
            String txRef = (String) payload.getOrDefault("txRef", "VQR-" + System.currentTimeMillis() % 1000000);

            Booking updated = bookingService.confirmDeposit(code, amount, method, txRef);
            return ResponseEntity.ok(Map.of("success", true, "booking", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/bookings/{code}/checkin")
    public ResponseEntity<?> checkIn(@PathVariable String code) {
        try {
            Booking updated = bookingService.checkIn(code);
            return ResponseEntity.ok(Map.of("success", true, "booking", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/bookings/{code}/checkout")
    public ResponseEntity<?> checkOut(@PathVariable String code, @RequestBody Map<String, Object> payload) {
        try {
            BigDecimal eqFee = payload.get("equipmentFee") != null ? new BigDecimal(payload.get("equipmentFee").toString()) : BigDecimal.ZERO;
            BigDecimal prodFee = payload.get("productFee") != null ? new BigDecimal(payload.get("productFee").toString()) : BigDecimal.ZERO;
            String paymentMethod = (String) payload.getOrDefault("paymentMethod", "Tiền mặt");
            String staffCode = (String) payload.getOrDefault("staffCode", "NVQ01");

            Invoice invoice = bookingService.checkOut(code, eqFee, prodFee, paymentMethod, staffCode);
            return ResponseEntity.ok(Map.of("success", true, "invoice", invoice));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/bookings/{code}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String code, @RequestBody(required = false) Map<String, String> payload) {
        try {
            String reason = payload != null ? payload.getOrDefault("reason", "Khách hàng yêu cầu hủy") : "Khách hàng yêu cầu hủy";
            Booking updated = bookingService.cancelBooking(code, reason);
            return ResponseEntity.ok(Map.of("success", true, "booking", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==========================================
    // 3.1. COURT TRANSFER (XL-06)
    // ==========================================
    @PostMapping("/bookings/{code}/transfer")
    public ResponseEntity<?> transferCourt(@PathVariable String code, @RequestBody Map<String, String> payload) {
        try {
            String targetCourtCode = payload.get("targetCourtCode");
            String newTimeSlot = payload.get("newTimeSlot");
            Map<String, Object> result = bookingService.transferCourt(code, targetCourtCode, newTimeSlot);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", sanitizeErrorMessage(e)));
        }
    }

    // ==========================================
    // 3.2. EXTRA SERVICES & POS ORDER (XL-05)
    // ==========================================
    @PostMapping("/bookings/{code}/order-service")
    public ResponseEntity<?> orderService(@PathVariable String code, @RequestBody Map<String, Object> payload) {
        try {
            String productCode = (String) payload.get("productCode");
            int quantity = Integer.parseInt(payload.getOrDefault("quantity", 1).toString());
            Map<String, Object> result = warehouseService.orderServiceForBooking(code, productCode, quantity);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", sanitizeErrorMessage(e)));
        }
    }

    @GetMapping("/bookings/slots")
    public ResponseEntity<?> getBookedSlots(
            @RequestParam String branchCode,
            @RequestParam(required = false) String courtCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<Booking> bookings;
        if (courtCode != null && !courtCode.trim().isEmpty()) {
            bookings = bookingRepository.findByCourtCodeAndBookingDate(courtCode.trim(), targetDate);
        } else {
            bookings = bookingRepository.findByBranchCodeAndBookingDate(branchCode.trim(), targetDate);
        }

        List<Map<String, Object>> activeSlots = bookings.stream()
                .filter(b -> b.getStatus() != null && !"Đã hủy".equalsIgnoreCase(b.getStatus()))
                .map(b -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("bookingCode", b.getBookingCode());
                    map.put("courtCode", b.getCourtCode());
                    map.put("branchCode", b.getBranchCode());
                    map.put("timeSlot", b.getTimeSlot());
                    map.put("status", b.getStatus());
                    map.put("customerName", b.getCustomerName());
                    map.put("customerPhone", b.getCustomerPhone());
                    map.put("totalPrice", b.getTotalPrice());
                    map.put("depositAmount", b.getDepositAmount());
                    map.put("paymentMethod", b.getPaymentMethod());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(activeSlots);
    }

    @PostMapping("/bookings/slot-lock")
    public ResponseEntity<?> lockSlotBooking(@RequestBody Map<String, Object> payload) {
        try {
            String courtCode = (String) payload.get("courtCode");
            String branchCode = (String) payload.get("branchCode");
            String dateStr = (String) payload.get("date");
            String timeSlot = (String) payload.get("timeSlot");
            String customerName = (String) payload.getOrDefault("customerName", "Khách đặt trực tuyến");
            String customerPhone = (String) payload.getOrDefault("customerPhone", "0903123456");

            LocalDate bookingDate = (dateStr != null && !dateStr.trim().isEmpty())
                    ? LocalDate.parse(dateStr.trim())
                    : LocalDate.now();

            // Validate that the slot is not already booked (including multi-hour overlaps)
            List<Booking> existing = bookingRepository.findByCourtCodeAndBookingDate(courtCode, bookingDate);
            boolean alreadyBooked = existing.stream()
                    .anyMatch(b -> b.getStatus() != null && !"Đã hủy".equalsIgnoreCase(b.getStatus()) && isSlotOverlapping(timeSlot, b.getTimeSlot()));
            if (alreadyBooked) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Khung giờ " + timeSlot + " của sân này đã có người đặt trước!"));
            }

            // Duration and pricing
            int duration = 1;
            Pattern p = Pattern.compile("(\\d{2}):00\\s*-\\s*(\\d{2}):00");
            Matcher m = p.matcher(timeSlot);
            int startHour = 18;
            if (m.find()) {
                startHour = Integer.parseInt(m.group(1));
                int endHour = Integer.parseInt(m.group(2));
                duration = Math.max(1, endHour - startHour);
            }

            // Hourly rate check from court or default via PricingService
            Optional<Court> courtOpt = courtRepository.findByCourtCode(courtCode);
            BigDecimal hourlyRate = pricingService.calculateHourlyRate(courtOpt.orElse(null), startHour);
            BigDecimal totalPrice = pricingService.calculateTotalPrice(hourlyRate, duration);
            BigDecimal deposit = pricingService.calculateDepositAmount(totalPrice);

            String bookingCode = "DS" + (100000 + (int) (Math.random() * 900000));
            Booking booking = new Booking();
            booking.setBookingCode(bookingCode);
            booking.setBranchCode(branchCode);
            booking.setCourtCode(courtCode);
            booking.setCustomerName(customerName);
            booking.setCustomerPhone(customerPhone);
            booking.setBookingDate(bookingDate);
            booking.setTimeSlot(timeSlot);
            booking.setHourlyPrice(hourlyRate);
            booking.setTotalPrice(totalPrice);
            booking.setDepositAmount(deposit);
            booking.setPaymentMethod("Chuyển khoản VietQR");
            booking.setStatus("Chờ cọc 30%");
            booking.setNotes("Đặt qua Lưới giờ Sân trực tuyến");

            Booking saved = bookingRepository.save(booking);

            bookingService.broadcastBookingEvent(vn.yain.dto.BookingNotificationDto.builder()
                    .eventType("NEW_BOOKING")
                    .bookingCode(bookingCode)
                    .courtCode(courtCode)
                    .branchCode(branchCode)
                    .customerName(customerName)
                    .customerPhone(customerPhone)
                    .timeSlot(timeSlot)
                    .totalPrice(totalPrice)
                    .depositAmount(deposit)
                    .status("Chờ cọc 30%")
                    .message("Đơn đặt sân mới trực tuyến [" + bookingCode + "] - Sân " + courtCode + " (" + timeSlot + ")")
                    .timestamp(java.time.LocalDateTime.now())
                    .build());

            // Dynamic VietQR payment URL (MBBank)
            String qrUrl = String.format(
                    "https://img.vietqr.io/image/970422-0903123456-compact2.png?amount=%d&addInfo=CK%%20%s&accountName=UTE%%20BADMINTON%%20CLUB",
                    deposit.longValue(), bookingCode
            );

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("bookingCode", bookingCode);
            resp.put("courtCode", courtCode);
            resp.put("courtName", courtOpt.isPresent() ? courtOpt.get().getCourtName() : courtCode);
            resp.put("branchCode", branchCode);
            resp.put("date", bookingDate.toString());
            resp.put("timeSlot", timeSlot);
            resp.put("totalPrice", totalPrice);
            resp.put("depositAmount", deposit);
            resp.put("qrUrl", qrUrl);
            resp.put("bankName", "MBBank (Ngân hàng Quân Đội)");
            resp.put("accountNo", "0903123456");
            resp.put("accountName", "UTE BADMINTON CLUB");

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==========================================
    // 3.5. WEBHOOK VIETQR / SEPAY / CASSO
    // ==========================================

    @PostMapping("/webhook/vietqr")
    public ResponseEntity<?> handleVietQRWebhook(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secretHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> webhookData) {
        try {
            boolean authorized = (secretHeader != null && secretHeader.trim().equals(webhookToken)) ||
                                 (authHeader != null && authHeader.contains(webhookToken));
            if (!authorized) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "error", "Unauthorized webhook caller"));
            }

            String content = (String) webhookData.getOrDefault("content", "");
            if (content == null || content.isEmpty()) {
                content = (String) webhookData.getOrDefault("description", "");
            }
            if (content == null) content = "";

            Pattern p = Pattern.compile("DS\\d{6}", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(content);
            if (m.find()) {
                String bookingCode = m.group(0).toUpperCase();
                Optional<Booking> opt = bookingRepository.findByBookingCode(bookingCode);
                if (opt.isPresent()) {
                    Booking b = opt.get();
                    bookingService.confirmDeposit(bookingCode, b.getDepositAmount(), "Chuyển khoản VietQR", "SEPAY-" + bookingCode);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Auto-confirmed deposit for " + bookingCode));
                }
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "Webhook received successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private boolean isSlotOverlapping(String s1, String s2) {
        if (bookingService != null) {
            return bookingService.isSlotOverlapping(s1, s2);
        }
        if (s1 == null || s2 == null) return false;
        if (s1.trim().equalsIgnoreCase(s2.trim())) return true;
        try {
            int[] r1 = parseSlotHours(s1);
            int[] r2 = parseSlotHours(s2);
            return Math.max(r1[0], r2[0]) < Math.min(r1[1], r2[1]);
        } catch (Exception e) {
            return s1.trim().equalsIgnoreCase(s2.trim());
        }
    }

    private int[] parseSlotHours(String slot) {
        if (bookingService != null) {
            return bookingService.parseSlotHours(slot);
        }
        Pattern p = Pattern.compile("(\\d{1,2}):\\d{2}\\s*-\\s*(\\d{1,2}):\\d{2}");
        Matcher m = p.matcher(slot);
        if (m.find()) {
            return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
        }
        return new int[]{0, 0};
    }


    // ==========================================
    // 4. PRODUCTS, BRANCHES, INVOICES, TOURNAMENTS
    // ==========================================

    @GetMapping("/products")
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Tên sản phẩm không được để trống!"));
        }
        if (product.getProductCode() == null || product.getProductCode().trim().isEmpty()) {
            product.setProductCode("P" + String.format("%02d", (productRepository.count() + 1)));
        }
        if (product.getCategory() == null || product.getCategory().trim().isEmpty()) {
            product.setCategory("Cầu lông");
        }
        if (product.getUnit() == null || product.getUnit().trim().isEmpty()) {
            product.setUnit("Ống");
        }
        if (product.getCostPrice() == null) product.setCostPrice(BigDecimal.ZERO);
        if (product.getUnitPrice() == null) product.setUnitPrice(BigDecimal.ZERO);
        if (product.getStockQuantity() == null) product.setStockQuantity(0);
        if (product.getMinQuantity() == null) product.setMinQuantity(10);
        if (product.getBranchCode() == null || product.getBranchCode().trim().isEmpty()) {
            product.setBranchCode("CN01");
        }

        if (productRepository.findByProductCode(product.getProductCode().trim()).isPresent()) {
            product.setProductCode(product.getProductCode().trim() + "_" + (System.currentTimeMillis() % 1000));
        }

        Product saved = productRepository.save(product);
        return ResponseEntity.ok(Map.of("success", true, "product", saved, "message", "Đã lưu mặt hàng vào cơ sở dữ liệu thành công!"));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product payload) {
        Optional<Product> pOpt = productRepository.findById(id);
        if (pOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Không tìm thấy sản phẩm ID: " + id));
        }
        Product p = pOpt.get();
        if (payload.getProductName() != null && !payload.getProductName().trim().isEmpty()) {
            p.setProductName(payload.getProductName().trim());
        }
        if (payload.getCategory() != null) p.setCategory(payload.getCategory());
        if (payload.getUnit() != null) p.setUnit(payload.getUnit());
        if (payload.getCostPrice() != null) p.setCostPrice(payload.getCostPrice());
        if (payload.getUnitPrice() != null) p.setUnitPrice(payload.getUnitPrice());
        if (payload.getStockQuantity() != null) p.setStockQuantity(payload.getStockQuantity());
        if (payload.getMinQuantity() != null) p.setMinQuantity(payload.getMinQuantity());
        if (payload.getBranchCode() != null) p.setBranchCode(payload.getBranchCode());

        Product updated = productRepository.save(p);
        return ResponseEntity.ok(Map.of("success", true, "product", updated, "message", "Đã cập nhật sản phẩm thành công!"));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Không tìm thấy sản phẩm!"));
        }
        try {
            productRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa sản phẩm khỏi cơ sở dữ liệu thành công!"));
        } catch (Exception e) {
            log.warn("Cannot delete product id={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", "Không thể xóa sản phẩm do đã có dữ liệu giao dịch hoặc hóa đơn liên quan!"));
        }
    }

    @GetMapping("/branches")
    public List<Branch> getBranches() {
        return branchRepository.findAll();
    }

    @GetMapping("/invoices")
    public List<Invoice> getInvoices() {
        return invoiceRepository.findAllByOrderByInvoiceDateDesc();
    }

    @PostMapping("/invoices/retail")
    public ResponseEntity<?> createRetailInvoice(@RequestBody Map<String, Object> payload) {
        try {
            BigDecimal prodFee = payload.get("productFee") != null ? new BigDecimal(payload.get("productFee").toString()) : BigDecimal.ZERO;
            BigDecimal eqFee = payload.get("equipmentFee") != null ? new BigDecimal(payload.get("equipmentFee").toString()) : BigDecimal.ZERO;
            String paymentMethod = (String) payload.getOrDefault("paymentMethod", "Tiền mặt");
            String staffCode = (String) payload.getOrDefault("staffCode", "NVQ01");

            Invoice invoice = new Invoice();
            invoice.setInvoiceCode("HD" + (System.currentTimeMillis() % 1000000));
            invoice.setBookingCode("LE" + (System.currentTimeMillis() % 100000));
            invoice.setStaffCode(staffCode);
            invoice.setInvoiceDate(LocalDateTime.now());
            invoice.setCourtFee(BigDecimal.ZERO);
            invoice.setEquipmentFee(eqFee);
            invoice.setProductFee(prodFee);
            invoice.setDepositPaid(BigDecimal.ZERO);
            invoice.setTotalPayment(prodFee.add(eqFee));
            invoice.setPaymentMethod(paymentMethod);
            invoice.setStatus("Đã thanh toán");

            Invoice saved = invoiceRepository.save(invoice);
            return ResponseEntity.ok(Map.of("success", true, "invoice", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/tournaments")
    public List<Tournament> getTournaments() {
        List<Tournament> list = tournamentRepository.findAll();
        if (list.isEmpty()) {
            Tournament t1 = new Tournament();
            t1.setTournamentCode("GD01");
            t1.setBranchCode("CN01");
            t1.setTournamentName("Giải Cầu Lông Đôi Nam Nữ Mùa Thu UTE Open 2026");
            t1.setStartDate(LocalDateTime.of(2026, 10, 10, 8, 0));
            t1.setEndDate(LocalDateTime.of(2026, 10, 12, 18, 0));
            t1.setRules("Thi đấu theo thể thức loại trực tiếp 3 hiệp 21 điểm theo luật BWF.");
            t1.setMaxParticipants(32);
            t1.setCurrentParticipants(24);
            t1.setEntryFee(new BigDecimal("300000"));
            t1.setTotalPrize(new BigDecimal("20000000"));
            t1.setStatus("Đang mở đăng ký");

            Tournament t2 = new Tournament();
            t2.setTournamentCode("GD02");
            t2.setBranchCode("CN01");
            t2.setTournamentName("Giải Cầu Lông Doanh Nghiệp & Sinh Viên Cúp Thủ Đức 2026");
            t2.setStartDate(LocalDateTime.of(2026, 11, 20, 8, 0));
            t2.setEndDate(LocalDateTime.of(2026, 11, 22, 18, 0));
            t2.setRules("Giải đấu phong trào kết nối các doanh nghiệp và cựu sinh viên.");
            t2.setMaxParticipants(24);
            t2.setCurrentParticipants(0);
            t2.setEntryFee(BigDecimal.ZERO);
            t2.setTotalPrize(new BigDecimal("30000000"));
            t2.setStatus("Sắp mở đăng ký");

            try {
                tournamentRepository.save(t1);
                tournamentRepository.save(t2);
                list = tournamentRepository.findAll();
            } catch (Exception ignored) {
                list = List.of(t1, t2);
            }
        }
        return list;
    }

    @PostMapping("/tournaments/register")
    public ResponseEntity<?> registerTournament(@RequestBody Map<String, Object> payload) {
        String tournamentCode = (String) payload.getOrDefault("tournamentCode", "GD01");
        Optional<Tournament> opt = tournamentRepository.findByTournamentCode(tournamentCode);
        if (opt.isEmpty()) {
            List<Tournament> all = tournamentRepository.findAll();
            if (!all.isEmpty()) {
                opt = Optional.of(all.get(0));
            }
        }

        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy giải đấu yêu cầu."
            ));
        }

        Tournament t = opt.get();
        int max = t.getMaxParticipants() != null ? t.getMaxParticipants() : 32;
        int current = t.getCurrentParticipants() != null ? t.getCurrentParticipants() : 0;

        if ("Đã đóng đăng ký".equalsIgnoreCase(t.getStatus()) || "Hết chỗ".equalsIgnoreCase(t.getStatus()) || current >= max) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Giải đấu đã hết chỗ hoặc cổng đăng ký đã chính thức đóng.",
                    "currentParticipants", current,
                    "maxParticipants", max,
                    "status", "Hết chỗ"
            ));
        }

        current++;
        t.setCurrentParticipants(current);
        if (current >= max) {
            t.setStatus("Hết chỗ");
        }
        tournamentRepository.save(t);

        int remaining = max - current;
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đăng ký tham gia giải đấu thành công!",
                "tournamentCode", t.getTournamentCode(),
                "tournamentName", t.getTournamentName(),
                "currentParticipants", current,
                "maxParticipants", max,
                "remainingSlots", Math.max(0, remaining),
                "status", t.getStatus()
        ));
    }

    @GetMapping("/equipment")
    public List<Equipment> getEquipment(@RequestParam(required = false) String branchCode) {
        if (branchCode != null && !branchCode.isEmpty()) {
            return equipmentRepository.findByBranchCode(branchCode);
        }
        return equipmentRepository.findAll();
    }

    // ==========================================
    // 5. CHATBOT CONSULTATION ENDPOINT
    // ==========================================

    @PostMapping("/chatbot/message")
    public ResponseEntity<?> sendChatMessage(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        String sessionId = payload.get("sessionId");
        vn.yain.dto.ChatMessageDto reply = chatbotService.processMessage(message, sessionId);
        return ResponseEntity.ok(reply);
    }

    @PostMapping("/chatbot/reset")
    public ResponseEntity<?> resetChatbotSession(@RequestBody Map<String, String> payload) {
        String sessionId = payload.get("sessionId");
        chatbotService.resetSession(sessionId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Session reset"));
    }

    // ==========================================
    // 6. INVENTORY INTAKE & WAREHOUSE (XL-10)
    // ==========================================

    @PostMapping("/inventory/intake")
    public ResponseEntity<?> createStockIntake(@RequestBody Map<String, Object> payload) {
        try {
            String supplierName = (String) payload.get("supplierName");
            String branchCode = (String) payload.get("branchCode");
            String staffCode = (String) payload.get("staffCode");
            String notes = (String) payload.get("notes");
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
            StockReceipt receipt = warehouseService.createStockReceipt(supplierName, branchCode, staffCode, items, notes);
            return ResponseEntity.ok(Map.of("success", true, "receipt", receipt));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", sanitizeErrorMessage(e)));
        }
    }

    @GetMapping("/inventory/receipts")
    public ResponseEntity<?> getStockReceipts(@RequestParam(required = false) String branchCode) {
        return ResponseEntity.ok(warehouseService.getReceiptsByBranch(branchCode));
    }

    // ==========================================
    // 7. CASHIER SHIFT HANDOVER (XL-08)
    // ==========================================

    @GetMapping("/shifts/summary")
    public ResponseEntity<?> getShiftSummary(@RequestParam(required = false) String staffCode,
                                            @RequestParam(required = false) String branchCode) {
        return ResponseEntity.ok(shiftService.getShiftSummary(staffCode, branchCode));
    }

    @PostMapping("/shifts/close")
    public ResponseEntity<?> closeShift(@RequestBody Map<String, Object> payload) {
        try {
            String staffCode = (String) payload.get("staffCode");
            String branchCode = (String) payload.get("branchCode");
            String notes = (String) payload.get("notes");
            BigDecimal actualCash = new BigDecimal(payload.get("actualCash").toString());
            Map<String, Object> result = shiftService.closeShift(staffCode, branchCode, actualCash, notes);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", sanitizeErrorMessage(e)));
        }
    }

    // ==========================================
    // 7.1 STAFF & WEEKLY SHIFT SCHEDULE (XL-08)
    // ==========================================

    @GetMapping("/staff")
    public List<Staff> getStaffList(@RequestParam(required = false) String branchCode) {
        return shiftService.getAllStaff(branchCode);
    }

    @PostMapping("/staff")
    public ResponseEntity<?> saveStaff(@RequestBody Staff staff) {
        try {
            Staff saved = shiftService.saveStaff(staff);
            return ResponseEntity.ok(Map.of("success", true, "staff", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/staff/{staffCode}")
    public ResponseEntity<?> deleteStaff(@PathVariable String staffCode) {
        try {
            shiftService.deleteStaff(staffCode);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa nhân viên thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/shifts/week")
    public ResponseEntity<?> getWeeklySchedule(@RequestParam(required = false) String branchCode) {
        return ResponseEntity.ok(shiftService.getWeeklySchedule(branchCode));
    }

    @PostMapping("/shifts/assign")
    public ResponseEntity<?> assignShift(@RequestBody Map<String, String> payload) {
        try {
            String day = payload.get("day");
            String shift = payload.get("shift");
            String staffCode = payload.get("staffCode");
            String duty = payload.get("duty");
            String icon = payload.get("icon");
            String branchCode = payload.get("branchCode");
            shiftService.assignShift(day, shift, staffCode, duty, icon, branchCode);
            return ResponseEntity.ok(Map.of("success", true, "message", "Phân ca trực thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/shifts/remove")
    public ResponseEntity<?> removeShift(@RequestBody Map<String, String> payload) {
        try {
            String day = payload.get("day");
            String shift = payload.get("shift");
            String staffCode = payload.get("staffCode");
            shiftService.removeShift(day, shift, staffCode);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã gỡ nhân viên khỏi ca trực!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/shifts/week/save")
    public ResponseEntity<?> saveWeeklySchedule(@RequestBody Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> schedule = (Map<String, Object>) payload.get("schedule");
            String branchCode = (String) payload.getOrDefault("branchCode", "CN01");
            shiftService.saveWeeklySchedule(schedule, branchCode);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã lưu lịch phân ca trực tuần!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/shifts/week/reset")
    public ResponseEntity<?> resetWeeklySchedule(@RequestBody(required = false) Map<String, String> payload) {
        try {
            String branchCode = payload != null ? payload.getOrDefault("branchCode", "CN01") : "CN01";
            shiftService.resetWeeklySchedule(branchCode);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã cài lại lịch phân ca trực mặc định!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==========================================
    // 8. CLOUDINARY IMAGE UPLOAD (WEEK 2 GOAL)
    // ==========================================
    @PostMapping("/upload/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "folder", defaultValue = "general") String folder) {
        try {
            String url = cloudinaryService.uploadImage(file, folder);
            return ResponseEntity.ok(Map.of("success", true, "url", url, "folder", folder));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Lỗi upload ảnh: " + e.getMessage()));
        }
    }
}
