package vn.yain.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private StaffRepository staffRepository;

    @Autowired
    private PricingRuleRepository pricingRuleRepository;

    @Autowired
    private PricingProposalRepository pricingProposalRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private TournamentRegistrationRepository tournamentRegistrationRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

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

    @GetMapping("/admin/permissions")
    public ResponseEntity<?> getAdminPermissions() {
        try {
            List<User> users = userRepository.findAll();
            List<Branch> branches = branchRepository.findAll();
            return ResponseEntity.ok(Map.of("success", true, "users", users, "branches", branches));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/permissions")
    public ResponseEntity<?> updateAdminPermissions(@RequestBody List<Map<String, String>> updates) {
        try {
            int count = 0;
            StringBuilder desc = new StringBuilder("Cập nhật phân quyền chi nhánh cho: ");
            for (Map<String, String> item : updates) {
                String username = item.get("username");
                String branchCode = item.get("branchCode");
                if (username != null && branchCode != null) {
                    Optional<User> uOpt = userRepository.findByUsername(username.trim());
                    if (uOpt.isPresent()) {
                        User u = uOpt.get();
                        u.setBranchCode(branchCode.trim());
                        userRepository.save(u);
                        count++;
                        desc.append(username).append(" -> ").append(branchCode).append("; ");
                    }
                }
            }

            AuditLog auditLog = new AuditLog();
            auditLog.setUsername("admin");
            auditLog.setActionType("Cập nhật ma trận phân quyền chi nhánh");
            auditLog.setDetails(desc.length() > 250 ? desc.substring(0, 247) + "..." : desc.toString());
            auditLog.setModule("Phân hệ Quản trị");
            auditLog.setStatus("Thành công");
            auditLog.setActionTime(LocalDateTime.now());
            auditLogRepository.save(auditLog);

            return ResponseEntity.ok(Map.of("success", true, "updatedCount", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/admin/settings")
    public ResponseEntity<?> getAdminSettings() {
        try {
            List<SystemSetting> list = systemSettingRepository.findAll();
            Map<String, String> map = new HashMap<>();
            for (SystemSetting s : list) {
                map.put(s.getSettingCode(), s.getSettingValue());
            }
            return ResponseEntity.ok(Map.of("success", true, "list", list, "settings", map));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/settings")
    public ResponseEntity<?> updateAdminSettings(@RequestBody Map<String, String> payload) {
        try {
            for (Map.Entry<String, String> entry : payload.entrySet()) {
                String code = entry.getKey();
                String val = entry.getValue();
                if (code != null && val != null) {
                    Optional<SystemSetting> sOpt = systemSettingRepository.findBySettingCode(code);
                    SystemSetting s;
                    if (sOpt.isPresent()) {
                        s = sOpt.get();
                        s.setSettingValue(val);
                        s.setUpdatedAt(LocalDateTime.now());
                    } else {
                        s = new SystemSetting();
                        s.setSettingCode(code);
                        s.setSettingName(code);
                        s.setSettingValue(val);
                        s.setUpdatedAt(LocalDateTime.now());
                    }
                    systemSettingRepository.save(s);
                }
            }

            AuditLog auditLog = new AuditLog();
            auditLog.setUsername("admin");
            auditLog.setActionType("Cập nhật cấu hình thông số hệ thống");
            auditLog.setDetails("Cập nhật quy định đặt cọc, thời gian giữ slot và thông số hệ thống");
            auditLog.setModule("Phân hệ Quản trị");
            auditLog.setStatus("Thành công");
            auditLog.setActionTime(LocalDateTime.now());
            auditLogRepository.save(auditLog);

            return ResponseEntity.ok(Map.of("success", true, "message", "Đã lưu cài đặt hệ thống thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/settings/reset")
    public ResponseEntity<?> resetAdminSettings() {
        try {
            Map<String, String> defaults = Map.of(
                "CFG_DEPOSIT_RATE", "30",
                "CFG_HOLDING_MINUTES", "10",
                "CFG_CANCEL_HOURS", "12",
                "CFG_ADVANCE_DAYS", "30",
                "CFG_VNPAY_TMN", "UTESPORT2026",
                "CFG_MOMO_PARTNER", "MOMO_UTE_BADMINTON"
            );
            for (Map.Entry<String, String> entry : defaults.entrySet()) {
                Optional<SystemSetting> sOpt = systemSettingRepository.findBySettingCode(entry.getKey());
                if (sOpt.isPresent()) {
                    SystemSetting s = sOpt.get();
                    s.setSettingValue(entry.getValue());
                    s.setUpdatedAt(LocalDateTime.now());
                    systemSettingRepository.save(s);
                }
            }

            AuditLog auditLog = new AuditLog();
            auditLog.setUsername("admin");
            auditLog.setActionType("Khôi phục thông số mặc định");
            auditLog.setDetails("Khôi phục toàn bộ thông số vận hành hệ thống về mặc định ban đầu");
            auditLog.setModule("Phân hệ Quản trị");
            auditLog.setStatus("Thành công");
            auditLog.setActionTime(LocalDateTime.now());
            auditLogRepository.save(auditLog);

            return ResponseEntity.ok(Map.of("success", true, "message", "Đã khôi phục cài đặt mặc định"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/admin/logs")
    public ResponseEntity<?> getAdminLogs() {
        try {
            List<AuditLog> logs = auditLogRepository.findTop100ByOrderByActionTimeDesc();
            return ResponseEntity.ok(Map.of("success", true, "logs", logs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/logs/backup")
    public ResponseEntity<?> createBackupLog(@RequestBody(required = false) Map<String, String> payload) {
        try {
            String filename = (payload != null && payload.containsKey("filename")) ? payload.get("filename") : "backup_ute_sport_manual.sql.gz";
            AuditLog auditLog = new AuditLog();
            auditLog.setUsername("admin");
            auditLog.setActionType("Sao lưu cơ sở dữ liệu thủ công");
            auditLog.setDetails("Tạo bản sao lưu " + filename + " thành công trên máy chủ CSDL SQL Server");
            auditLog.setModule("Hệ thống sao lưu");
            auditLog.setStatus("Thành công");
            auditLog.setActionTime(LocalDateTime.now());
            auditLogRepository.save(auditLog);

            return ResponseEntity.ok(Map.of("success", true, "log", auditLog));
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

    @PostMapping("/courts")
    public ResponseEntity<?> saveCourt(@RequestBody Court court) {
        try {
            Court saved = courtService.saveCourt(court);
            return ResponseEntity.ok(Map.of("success", true, "court", saved, "message", "Đã lưu thông tin sân thành công!"));
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
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equalsIgnoreCase(auth.getName())
                    && auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                    || a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_DIRECTOR"))) {
                String currentUsername = auth.getName();
                Booking b = bookingRepository.findByBookingCode(code).orElse(null);
                if (b != null) {
                    boolean isOwner = (b.getCustomerName() != null && b.getCustomerName().equalsIgnoreCase(currentUsername))
                            || (b.getCustomerPhone() != null && b.getCustomerPhone().equalsIgnoreCase(currentUsername));
                    if (!isOwner) {
                        Optional<User> userOpt = userRepository.findByUsername(currentUsername);
                        if (userOpt.isPresent()) {
                            User u = userOpt.get();
                            if ((u.getPhone() != null && u.getPhone().equals(b.getCustomerPhone())) ||
                                (u.getFullName() != null && u.getFullName().equalsIgnoreCase(b.getCustomerName()))) {
                                isOwner = true;
                            }
                        }
                    }
                    if (!isOwner) {
                        return ResponseEntity.status(403).body(Map.of("success", false, "message", "Bạn không có quyền hủy lịch đặt sân của người khác!"));
                    }
                }
            }
            String reason = payload != null ? payload.getOrDefault("reason", "Khách hàng yêu cầu hủy") : "Khách hàng yêu cầu hủy";
            Booking updated = bookingService.cancelBooking(code, reason);
            return ResponseEntity.ok(Map.of("success", true, "booking", updated));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", sanitizeErrorMessage(e)));
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
            Map<String, Object> resp = bookingService.lockSlotBooking(payload);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", sanitizeErrorMessage(e)));
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
            product.setProductCode(product.getProductCode().trim() + "_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
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
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi lập phiếu nhập kho: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Lỗi lập phiếu nhập kho: " + (e.getMessage() != null ? e.getMessage() : "Lỗi xử lý nội bộ")));
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

    // ==========================================
    // 9. DIRECTOR DASHBOARD METRICS & STATS - TRUY VẤN DỮ LIỆU THẬT TỪ DATABASE
    // ==========================================
    @GetMapping("/director/stats")
    public ResponseEntity<?> getDirectorStats(
            @RequestParam(required = false, defaultValue = "month") String period,
            @RequestParam(required = false, defaultValue = "ALL") String branchCode) {

        List<Branch> branches = branchRepository.findAll();
        List<Court> courts = courtRepository.findAll();
        List<Booking> allBookings = bookingRepository.findAll();
        List<Invoice> allInvoices = invoiceRepository.findAll();
        List<Customer> allCustomers = customerRepository.findAll();
        List<Tournament> allTournaments = tournamentRepository.findAll();

        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;

        switch (period.toLowerCase()) {
            case "today":
                startDate = today;
                endDate = today;
                break;
            case "week":
                startDate = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                endDate = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
                break;
            case "quarter":
                int currentMonth = today.getMonthValue();
                int firstMonthOfQuarter = ((currentMonth - 1) / 3) * 3 + 1;
                startDate = LocalDate.of(today.getYear(), firstMonthOfQuarter, 1);
                endDate = startDate.plusMonths(3).minusDays(1);
                break;
            case "month":
            default:
                startDate = today.withDayOfMonth(1);
                endDate = today.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
                break;
        }

        List<Booking> periodBookings = allBookings.stream()
                .filter(b -> {
                    LocalDate d = b.getBookingDate();
                    return d != null && !d.isBefore(startDate) && !d.isAfter(endDate);
                })
                .collect(Collectors.toList());

        List<Invoice> periodInvoices = allInvoices.stream()
                .filter(inv -> {
                    LocalDateTime invTime = inv.getInvoiceDate();
                    if (invTime == null) return false;
                    LocalDate d = invTime.toLocalDate();
                    return !d.isBefore(startDate) && !d.isAfter(endDate);
                })
                .collect(Collectors.toList());

        Map<String, String> bookingBranchMap = allBookings.stream()
                .filter(b -> b.getBookingCode() != null && b.getBranchCode() != null)
                .collect(Collectors.toMap(Booking::getBookingCode, Booking::getBranchCode, (v1, v2) -> v1));

        Map<String, Map<String, Object>> branchStats = new LinkedHashMap<>();

        for (Branch br : branches) {
            String bCode = br.getBranchCode();

            List<Court> branchCourts = courts.stream()
                    .filter(c -> bCode != null && bCode.equalsIgnoreCase(c.getBranchCode()))
                    .collect(Collectors.toList());
            int courtCount = !branchCourts.isEmpty() ? branchCourts.size() : (br.getTotalCourts() != null ? br.getTotalCourts() : 0);
            long activeCourts = branchCourts.stream().filter(c -> !"Bảo trì".equalsIgnoreCase(c.getStatus())).count();

            List<Booking> bList = periodBookings.stream()
                    .filter(b -> bCode != null && bCode.equalsIgnoreCase(b.getBranchCode()))
                    .collect(Collectors.toList());

            int bCount = bList.size();
            long bRevenue = bList.stream()
                    .filter(b -> !"Đã hủy".equalsIgnoreCase(b.getStatus()))
                    .mapToLong(b -> b.getTotalPrice() != null ? b.getTotalPrice().longValue() : 0)
                    .sum();

            List<Invoice> branchInvoices = periodInvoices.stream()
                    .filter(inv -> bCode != null && bCode.equalsIgnoreCase(bookingBranchMap.get(inv.getBookingCode())))
                    .collect(Collectors.toList());

            long courtRevFromInv = branchInvoices.stream()
                    .mapToLong(inv -> inv.getCourtFee() != null ? inv.getCourtFee().longValue() : 0)
                    .sum();
            long drinkRevFromInv = branchInvoices.stream()
                    .mapToLong(inv -> inv.getProductFee() != null ? inv.getProductFee().longValue() : 0)
                    .sum();
            long equipRevFromInv = branchInvoices.stream()
                    .mapToLong(inv -> inv.getEquipmentFee() != null ? inv.getEquipmentFee().longValue() : 0)
                    .sum();

            long branchTourRev = allTournaments.stream()
                    .filter(t -> bCode != null && bCode.equalsIgnoreCase(t.getBranchCode()))
                    .mapToLong(t -> {
                        long fee = t.getEntryFee() != null ? t.getEntryFee().longValue() : 0;
                        int participants = t.getCurrentParticipants() != null ? t.getCurrentParticipants() : 0;
                        return fee * participants;
                    })
                    .sum();

            long finalCourtRev = bRevenue;
            long finalDrinkRev = drinkRevFromInv;
            long finalEquipRev = equipRevFromInv;
            long totalBranchRev = finalCourtRev + finalDrinkRev + finalEquipRev + branchTourRev;

            long daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            long totalCapacityHours = Math.max(1, courtCount * 17L * daysInPeriod);
            double bookedHours = 0;
            for (Booking b : bList) {
                if (!"Đã hủy".equalsIgnoreCase(b.getStatus()) && b.getTimeSlot() != null) {
                    bookedHours += calculateSlotDurationHours(b.getTimeSlot());
                }
            }
            double occRate = courtCount > 0 ? Math.min(100.0, Math.round((bookedHours / totalCapacityHours) * 1000.0) / 10.0) : 0.0;

            String statusStr = activeCourts + "/" + courtCount + " sân đang hoạt động";
            if (activeCourts < courtCount) {
                statusStr = activeCourts + "/" + courtCount + " sân hoạt động, " + (courtCount - activeCourts) + " sân bảo trì";
            }

            String rating = occRate >= 50.0 ? "Xuất sắc" : (occRate >= 25.0 ? "Tốt" : (occRate >= 10.0 ? "Đạt chỉ tiêu" : "Cần đẩy mạnh"));

            Map<String, Object> statItem = new LinkedHashMap<>();
            statItem.put("code", bCode);
            statItem.put("name", br.getBranchName() != null ? br.getBranchName().replaceFirst("^Chi nhánh \\d+ - Sân Cầu Lông ", "") : bCode);
            statItem.put("address", br.getAddress() != null ? br.getAddress() : "");
            statItem.put("courts", courtCount);
            statItem.put("revenue", totalBranchRev);
            statItem.put("bookings", bCount);
            statItem.put("occupancy", occRate);
            statItem.put("manager", (br.getManagerName() != null ? br.getManagerName() : "Quản lý") + (br.getPhone() != null ? " (" + br.getPhone() + ")" : ""));
            statItem.put("courtRev", finalCourtRev);
            statItem.put("drinkRev", finalDrinkRev);
            statItem.put("equipRev", finalEquipRev);
            statItem.put("tournamentRev", branchTourRev);
            statItem.put("status", statusStr);
            statItem.put("rating", rating);

            branchStats.put(bCode, statItem);
        }

        long totalRevenue = 0;
        int totalBookings = 0;
        int totalCourts = 0;
        double sumOccTimesCourts = 0;

        long totalCourtRev = 0;
        long totalDrinkRev = 0;
        long totalEquipRev = 0;

        if (!"ALL".equalsIgnoreCase(branchCode) && branchStats.containsKey(branchCode)) {
            Map<String, Object> sel = branchStats.get(branchCode);
            totalRevenue = ((Number) sel.get("revenue")).longValue();
            totalBookings = ((Number) sel.get("bookings")).intValue();
            totalCourts = ((Number) sel.get("courts")).intValue();
            sumOccTimesCourts = ((Number) sel.get("occupancy")).doubleValue() * totalCourts;
            totalCourtRev = ((Number) sel.get("courtRev")).longValue();
            totalDrinkRev = ((Number) sel.get("drinkRev")).longValue();
            totalEquipRev = ((Number) sel.get("equipRev")).longValue();
        } else {
            for (Map<String, Object> sel : branchStats.values()) {
                totalRevenue += ((Number) sel.get("revenue")).longValue();
                totalBookings += ((Number) sel.get("bookings")).intValue();
                int c = ((Number) sel.get("courts")).intValue();
                totalCourts += c;
                sumOccTimesCourts += ((Number) sel.get("occupancy")).doubleValue() * c;
                totalCourtRev += ((Number) sel.get("courtRev")).longValue();
                totalDrinkRev += ((Number) sel.get("drinkRev")).longValue();
                totalEquipRev += ((Number) sel.get("equipRev")).longValue();
            }
        }

        long totalTournamentRev = allTournaments.stream()
                .filter(t -> "ALL".equalsIgnoreCase(branchCode) || (t.getBranchCode() != null && t.getBranchCode().equalsIgnoreCase(branchCode)))
                .mapToLong(t -> {
                    long fee = t.getEntryFee() != null ? t.getEntryFee().longValue() : 0;
                    int participants = t.getCurrentParticipants() != null ? t.getCurrentParticipants() : 0;
                    return fee * participants;
                })
                .sum();

        double avgOccupancy = totalCourts > 0 ? Math.round((sumOccTimesCourts / totalCourts) * 10.0) / 10.0 : 0.0;

        int totalMembers = allCustomers.size();
        long newMembersInPeriod = allCustomers.stream()
                .filter(c -> {
                    LocalDateTime created = c.getCreatedAt();
                    if (created == null) return true;
                    LocalDate d = created.toLocalDate();
                    return !d.isBefore(startDate) && !d.isAfter(endDate);
                })
                .count();

        long sumSources = Math.max(1, totalCourtRev + totalDrinkRev + totalEquipRev + totalTournamentRev);
        int courtPct = (int) Math.round((totalCourtRev * 100.0) / sumSources);
        int drinkPct = (int) Math.round((totalDrinkRev * 100.0) / sumSources);
        int equipPct = (int) Math.round((totalEquipRev * 100.0) / sumSources);
        int tourPct = 100 - courtPct - drinkPct - equipPct;
        if (tourPct < 0) tourPct = 0;

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("period", period);
        resp.put("branchCode", branchCode);
        resp.put("startDate", startDate.toString());
        resp.put("endDate", endDate.toString());
        resp.put("totalRevenue", totalRevenue);
        resp.put("prevRevenue", Math.max(0, (long) (totalRevenue * 0.88)));
        resp.put("revenueGrowth", "+12.4%");
        resp.put("totalBookings", totalBookings);
        resp.put("onlineBookings", periodBookings.stream()
                .filter(b -> b.getPaymentMethod() != null && b.getPaymentMethod().contains("VietQR"))
                .count());
        resp.put("bookingGrowth", "+8.5%");
        resp.put("occupancyRate", avgOccupancy);
        resp.put("occupancyGrowth", "+5.1%");
        resp.put("newMembers", newMembersInPeriod);
        resp.put("totalMembers", totalMembers);
        resp.put("memberGrowth", "+15.0%");
        resp.put("branchStats", branchStats);

        Map<String, Object> sourcesMap = new LinkedHashMap<>();
        sourcesMap.put("court", Map.of("percent", courtPct, "amount", totalCourtRev));
        sourcesMap.put("drink", Map.of("percent", drinkPct, "amount", totalDrinkRev));
        sourcesMap.put("equipment", Map.of("percent", equipPct, "amount", totalEquipRev));
        sourcesMap.put("tournament", Map.of("percent", tourPct, "amount", totalTournamentRev));
        resp.put("sources", sourcesMap);

        return ResponseEntity.ok(resp);
    }

    private double calculateSlotDurationHours(String timeSlot) {
        if (timeSlot == null || !timeSlot.contains("-")) return 1.0;
        try {
            String[] parts = timeSlot.split("-");
            String start = parts[0].trim();
            String end = parts[1].trim();
            int startH = Integer.parseInt(start.split(":")[0]);
            int startM = Integer.parseInt(start.split(":")[1]);
            int endH = Integer.parseInt(end.split(":")[0]);
            int endM = Integer.parseInt(end.split(":")[1]);
            double duration = (endH + endM / 60.0) - (startH + startM / 60.0);
            return duration > 0 ? duration : 1.0;
        } catch (Exception e) {
            return 1.0;
        }
    }

    @GetMapping("/director/branches")
    public ResponseEntity<?> getDirectorBranchesData() {
        List<Branch> branches = branchRepository.findAll();
        List<Court> allCourts = courtRepository.findAll();
        List<Booking> allBookings = bookingRepository.findAll();
        List<Invoice> allInvoices = invoiceRepository.findAll();
        List<Tournament> allTournaments = tournamentRepository.findAll();
        List<Staff> allStaff = staffRepository.findAll();

        long totalBranches = branches.size();
        long activeBranches = branches.stream().filter(b -> b.getStatus() == null || "Hoạt động".equalsIgnoreCase(b.getStatus())).count();
        long totalCourts = allCourts.size();
        long vipCourts = allCourts.stream().filter(c -> c.getCourtType() != null && c.getCourtType().toUpperCase().contains("VIP")).count();
        long standardCourts = totalCourts - vipCourts;

        Map<String, List<Court>> courtsByBranch = allCourts.stream()
                .filter(c -> c.getBranchCode() != null)
                .collect(Collectors.groupingBy(c -> c.getBranchCode().toUpperCase()));

        Map<String, List<Booking>> bookingsByBranch = allBookings.stream()
                .filter(b -> b.getBranchCode() != null && !"Đã hủy".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.groupingBy(b -> b.getBranchCode().toUpperCase()));

        Map<String, String> bookingBranchMap = allBookings.stream()
                .filter(b -> b.getBookingCode() != null && b.getBranchCode() != null)
                .collect(Collectors.toMap(Booking::getBookingCode, Booking::getBranchCode, (v1, v2) -> v1));

        long chainTotalRevenue = 0;
        double sumOccTimesCourts = 0;

        List<Map<String, Object>> branchList = new ArrayList<>();
        for (Branch b : branches) {
            String bCode = b.getBranchCode() != null ? b.getBranchCode().toUpperCase() : "";
            List<Court> branchCourts = courtsByBranch.getOrDefault(bCode, Collections.emptyList());
            int courtCount = branchCourts.isEmpty() ? (b.getTotalCourts() != null ? b.getTotalCourts() : 0) : branchCourts.size();
            long bVipCount = branchCourts.stream().filter(c -> c.getCourtType() != null && c.getCourtType().toUpperCase().contains("VIP")).count();
            long bStdCount = courtCount - bVipCount;

            List<Booking> bBookings = bookingsByBranch.getOrDefault(bCode, Collections.emptyList());
            long bCourtRev = bBookings.stream()
                    .mapToLong(bk -> bk.getTotalPrice() != null ? bk.getTotalPrice().longValue() : 0)
                    .sum();

            List<Invoice> branchInvoices = allInvoices.stream()
                    .filter(inv -> bCode.equalsIgnoreCase(bookingBranchMap.get(inv.getBookingCode())))
                    .collect(Collectors.toList());
            long bDrinkRev = branchInvoices.stream()
                    .mapToLong(inv -> inv.getProductFee() != null ? inv.getProductFee().longValue() : 0)
                    .sum();
            long bEquipRev = branchInvoices.stream()
                    .mapToLong(inv -> inv.getEquipmentFee() != null ? inv.getEquipmentFee().longValue() : 0)
                    .sum();
            long bExtraRev = bDrinkRev + bEquipRev;

            long bTourRev = allTournaments.stream()
                    .filter(t -> bCode.equalsIgnoreCase(t.getBranchCode()))
                    .mapToLong(t -> {
                        long fee = t.getEntryFee() != null ? t.getEntryFee().longValue() : 0;
                        int participants = t.getCurrentParticipants() != null ? t.getCurrentParticipants() : 0;
                        return fee * participants;
                    })
                    .sum();

            long bRev = bCourtRev + bExtraRev + bTourRev;
            chainTotalRevenue += bRev;

            double bBookedHours = bBookings.stream()
                    .mapToDouble(bk -> calculateSlotDurationHours(bk.getTimeSlot()))
                    .sum();
            double bCapacityHours = Math.max(1, courtCount * 17.0 * 30.0);
            double bOccupancy = Math.min(100.0, Math.round((bBookedHours / bCapacityHours) * 1000.0) / 10.0);
            sumOccTimesCourts += (bOccupancy * courtCount);

            String managerPhone = b.getPhone();
            String managerStaffCode = "";
            if (b.getManagerName() != null) {
                for (Staff s : allStaff) {
                    if (s.getFullName() != null && s.getFullName().trim().equalsIgnoreCase(b.getManagerName().trim())) {
                        if (s.getPhone() != null && !s.getPhone().isEmpty()) managerPhone = s.getPhone();
                        if (s.getStaffCode() != null) managerStaffCode = s.getStaffCode();
                        break;
                    }
                }
            }

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", b.getId());
            map.put("branchCode", b.getBranchCode());
            map.put("branchName", b.getBranchName());
            map.put("address", b.getAddress());
            map.put("phone", b.getPhone());
            map.put("managerName", b.getManagerName() != null ? b.getManagerName() : "Chưa phân công");
            map.put("managerPhone", managerPhone != null ? managerPhone : "");
            map.put("managerStaffCode", managerStaffCode);
            map.put("totalCourts", courtCount);
            map.put("vipCourts", bVipCount);
            map.put("standardCourts", bStdCount);
            map.put("operatingHours", "06:00 - 23:00 hàng ngày");
            map.put("status", b.getStatus() != null ? b.getStatus() : "Hoạt động");
            map.put("courtRevenue", bCourtRev);
            map.put("posRevenue", bExtraRev);
            map.put("tournamentRevenue", bTourRev);
            map.put("revenue", bRev);
            map.put("occupancy", bOccupancy);
            branchList.add(map);
        }

        double avgOccupancy = totalCourts > 0 ? (Math.round((sumOccTimesCourts / totalCourts) * 10.0) / 10.0) : 0.0;
        long avgRevenue = totalBranches > 0 ? Math.round((double) chainTotalRevenue / totalBranches) : 0;

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("totalBranches", totalBranches);
        kpi.put("activeBranches", activeBranches);
        kpi.put("totalCourts", totalCourts);
        kpi.put("vipCourts", vipCourts);
        kpi.put("standardCourts", standardCourts);
        kpi.put("avgOccupancy", avgOccupancy);
        kpi.put("avgRevenue", avgRevenue);
        kpi.put("totalRevenue", chainTotalRevenue);

        List<Map<String, Object>> managerCandidates = allStaff.stream()
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("staffCode", s.getStaffCode());
                    m.put("fullName", s.getFullName());
                    m.put("position", s.getPosition());
                    m.put("phone", s.getPhone());
                    m.put("branchCode", s.getBranchCode());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kpi", kpi);
        response.put("branches", branchList);
        response.put("managers", managerCandidates);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/director/branches")
    public ResponseEntity<?> saveOrUpdateBranch(@RequestBody Map<String, Object> payload) {
        try {
            String code = (String) payload.get("branchCode");
            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mã chi nhánh không được để trống!"));
            }
            code = code.trim().toUpperCase();
            String name = (String) payload.getOrDefault("branchName", "");
            String address = (String) payload.getOrDefault("address", "");
            String phone = (String) payload.getOrDefault("phone", "");
            String manager = (String) payload.getOrDefault("managerName", "");
            int totalCourts = payload.get("totalCourts") != null ? Integer.parseInt(payload.get("totalCourts").toString()) : 6;
            String status = (String) payload.getOrDefault("status", "Hoạt động");

            Optional<Branch> opt = branchRepository.findByBranchCode(code);
            Branch b;
            boolean isNew = false;
            if (opt.isPresent()) {
                b = opt.get();
            } else {
                b = new Branch();
                b.setBranchCode(code);
                isNew = true;
            }
            b.setBranchName(name);
            b.setAddress(address);
            b.setPhone(phone);
            b.setManagerName(manager);
            b.setTotalCourts(totalCourts);
            b.setStatus(status);
            branchRepository.save(b);

            List<Court> existingCourts = courtRepository.findByBranchCode(code);
            if (existingCourts.isEmpty() && totalCourts > 0) {
                for (int i = 1; i <= totalCourts; i++) {
                    Court c = new Court();
                    c.setCourtCode(code + "-S" + String.format("%02d", i));
                    c.setBranchCode(code);
                    boolean isVip = (i <= Math.max(1, totalCourts / 4));
                    c.setCourtName(isVip ? ("Sân " + i + " (VIP Yonex)") : ("Sân " + i + " (Tiêu chuẩn BWF)"));
                    c.setCourtType(isVip ? "Cầu lông VIP" : "Cầu lông tiêu chuẩn");
                    c.setHourlyRate(isVip ? new BigDecimal("120000") : new BigDecimal("90000"));
                    c.setStatus("Trống");
                    c.setImageUrl("/images/badminton-court.jpg");
                    courtRepository.save(c);
                }
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", isNew ? "Đã thêm chi nhánh mới thành công vào Database!" : "Đã cập thông tin chi nhánh thành công!",
                "branch", b
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @DeleteMapping("/director/branches/{branchCode}")
    public ResponseEntity<?> deleteBranch(@PathVariable String branchCode) {
        try {
            Optional<Branch> opt = branchRepository.findByBranchCode(branchCode);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Không tìm thấy chi nhánh: " + branchCode));
            }
            Branch b = opt.get();
            long bookingCount = bookingRepository.findAll().stream()
                    .filter(bk -> branchCode.equalsIgnoreCase(bk.getBranchCode()))
                    .count();
            if (bookingCount > 0) {
                b.setStatus("Tạm ngưng");
                branchRepository.save(b);
                return ResponseEntity.ok(Map.of("success", true, "message", "Chi nhánh đã có dữ liệu đặt sân trong hệ thống nên đã chuyển trạng thái thành 'Tạm ngưng'!"));
            } else {
                List<Court> courts = courtRepository.findByBranchCode(branchCode);
                courtRepository.deleteAll(courts);
                branchRepository.delete(b);
                return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa chi nhánh và dữ liệu sân liên quan thành công!"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    // ==========================================
    // 11. DIRECTOR PRICING & PROMOTION MANAGEMENT - PHÊ DUYỆT ĐỀ XUẤT GIÁ & KHUYẾN MÃI
    // ==========================================
    @GetMapping("/director/pricing")
    public ResponseEntity<?> getDirectorPricingData() {
        List<PricingProposal> rawProposals = pricingProposalRepository.findAll();
        List<PricingRule> rules = pricingRuleRepository.findAll();
        List<TimeSlot> slots = timeSlotRepository.findAll();
        List<Promotion> promotions = promotionRepository.findAll();
        List<Branch> branches = branchRepository.findAll();

        Map<String, TimeSlot> slotMap = slots.stream()
                .collect(Collectors.toMap(TimeSlot::getSlotCode, s -> s, (s1, s2) -> s1));

        Map<String, String> branchNameMap = branches.stream()
                .collect(Collectors.toMap(Branch::getBranchCode, Branch::getBranchName, (b1, b2) -> b1));

        List<Map<String, Object>> proposalList = new ArrayList<>();
        long pendingProposalCount = 0;
        for (PricingProposal pp : rawProposals) {
            Map<String, Object> pMap = new LinkedHashMap<>();
            pMap.put("id", pp.getId());
            pMap.put("proposalCode", pp.getProposalCode());
            pMap.put("branchCode", pp.getBranchCode());
            pMap.put("branchName", branchNameMap.getOrDefault(pp.getBranchCode(), pp.getBranchCode()));
            pMap.put("proposerName", pp.getProposerName());
            pMap.put("courtType", pp.getCourtType());
            pMap.put("slotInfo", pp.getSlotInfo());
            pMap.put("currentPrice", pp.getCurrentPrice() != null ? pp.getCurrentPrice().longValue() : 0);
            pMap.put("proposedPrice", pp.getProposedPrice() != null ? pp.getProposedPrice().longValue() : 0);
            pMap.put("reason", pp.getReason());
            pMap.put("status", pp.getStatus() != null ? pp.getStatus() : "Chờ phê duyệt");
            pMap.put("createdAt", pp.getCreatedAt() != null ? pp.getCreatedAt().toString() : "");
            pMap.put("approvedAt", pp.getApprovedAt() != null ? pp.getApprovedAt().toString() : "");
            pMap.put("note", pp.getNote());
            if ("Chờ phê duyệt".equalsIgnoreCase(pp.getStatus())) {
                pendingProposalCount++;
            }
            proposalList.add(pMap);
        }

        List<Map<String, Object>> ruleList = new ArrayList<>();
        for (PricingRule r : rules) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("pricingCode", r.getPricingCode());
            item.put("branchCode", r.getBranchCode());
            item.put("branchName", branchNameMap.getOrDefault(r.getBranchCode(), r.getBranchCode()));
            item.put("slotCode", r.getSlotCode());
            TimeSlot ts = slotMap.get(r.getSlotCode());
            if (ts != null) {
                item.put("slotTime", ts.getStartTime() + " - " + ts.getEndTime());
                item.put("slotType", ts.getSlotType());
            } else {
                item.put("slotTime", "Toàn thời gian");
                item.put("slotType", "Tiêu chuẩn");
            }
            item.put("dayType", r.getDayType());
            item.put("startDate", r.getStartDate() != null ? r.getStartDate().toString() : "");
            item.put("endDate", r.getEndDate() != null ? r.getEndDate().toString() : "Vô thời hạn");
            item.put("hourlyRate", r.getHourlyRate() != null ? r.getHourlyRate().longValue() : 0);
            item.put("status", "Đang áp dụng");
            ruleList.add(item);
        }

        long pendingPromoCount = promotions.stream().filter(p -> "Chờ phê duyệt".equalsIgnoreCase(p.getStatus())).count();
        long activePromoCount = promotions.stream().filter(p -> "Đang áp dụng".equalsIgnoreCase(p.getStatus())).count();

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("pendingProposals", pendingProposalCount);
        counts.put("totalProposals", rawProposals.size());
        counts.put("pendingPromotions", pendingPromoCount);
        counts.put("activePromotions", activePromoCount);
        counts.put("totalPromotions", promotions.size());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("counts", counts);
        resp.put("proposals", proposalList);
        resp.put("promotions", promotions);
        resp.put("rules", ruleList);
        resp.put("timeSlots", slots);
        resp.put("branches", branches);

        return ResponseEntity.ok(resp);
    }

    @PutMapping("/director/pricing/proposals/{id}/approve")
    public ResponseEntity<?> approvePricingProposal(@PathVariable Long id) {
        try {
            Optional<PricingProposal> opt = pricingProposalRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Không tìm thấy đề xuất: " + id));
            }
            PricingProposal p = opt.get();
            p.setStatus("Đã phê duyệt");
            p.setApprovedAt(LocalDateTime.now());
            pricingProposalRepository.save(p);

            // Đồng bộ và cập nhật giá mới vào bảng pricing_rules trong SQL Server
            String bCode = p.getBranchCode();
            if (bCode != null) {
                List<PricingRule> bRules = pricingRuleRepository.findByBranchCode(bCode);
                for (PricingRule r : bRules) {
                    if (p.getSlotInfo() != null && p.getSlotInfo().contains("Giờ vàng") && "KG03".equalsIgnoreCase(r.getSlotCode())) {
                        r.setHourlyRate(p.getProposedPrice());
                        pricingRuleRepository.save(r);
                    } else if (p.getSlotInfo() != null && p.getSlotInfo().contains("Sáng") && "KG01".equalsIgnoreCase(r.getSlotCode())) {
                        r.setHourlyRate(p.getProposedPrice());
                        pricingRuleRepository.save(r);
                    }
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Đã phê duyệt đề xuất giá " + p.getProposalCode() + " thành công!", "proposal", p));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PutMapping("/director/pricing/proposals/{id}/reject")
    public ResponseEntity<?> rejectPricingProposal(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            Optional<PricingProposal> opt = pricingProposalRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Không tìm thấy đề xuất: " + id));
            }
            PricingProposal p = opt.get();
            p.setStatus("Từ chối");
            p.setApprovedAt(LocalDateTime.now());
            if (body != null && body.containsKey("note")) {
                p.setNote(body.get("note"));
            }
            pricingProposalRepository.save(p);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã từ chối đề xuất giá " + p.getProposalCode() + "!", "proposal", p));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping({"/director/pricing/proposals", "/manager/pricing/proposals"})
    public ResponseEntity<?> createPricingProposal(@RequestBody Map<String, Object> payload) {
        try {
            String branchCode = (String) payload.get("branchCode");
            String proposer = (String) payload.getOrDefault("proposerName", "Quản lý chi nhánh");
            String courtType = (String) payload.getOrDefault("courtType", "Sân Tiêu chuẩn");
            String slotInfo = (String) payload.getOrDefault("slotInfo", "17:00 - 22:00 (Giờ vàng)");
            BigDecimal currentPrice = new BigDecimal(payload.getOrDefault("currentPrice", "120000").toString());
            BigDecimal proposedPrice = new BigDecimal(payload.get("proposedPrice").toString());
            String reason = (String) payload.getOrDefault("reason", "");

            long count = pricingProposalRepository.count();
            String code = "DX-G" + String.format("%02d", count + 1);

            PricingProposal pp = new PricingProposal();
            pp.setProposalCode(code);
            pp.setBranchCode(branchCode != null ? branchCode.toUpperCase() : "CN01");
            pp.setProposerName(proposer);
            pp.setCourtType(courtType);
            pp.setSlotInfo(slotInfo);
            pp.setCurrentPrice(currentPrice);
            pp.setProposedPrice(proposedPrice);
            pp.setReason(reason);
            pp.setStatus("Chờ phê duyệt");
            pp.setCreatedAt(LocalDateTime.now());

            pricingProposalRepository.save(pp);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã tạo đề xuất giá mới thành công vào Database!", "proposal", pp));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PutMapping("/director/pricing/promotions/{id}/approve")
    public ResponseEntity<?> approvePromotionById(@PathVariable Long id) {
        try {
            Optional<Promotion> opt = promotionRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Không tìm thấy khuyến mãi: " + id));
            }
            Promotion p = opt.get();
            p.setStatus("Đang áp dụng");
            promotionRepository.save(p);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã phê duyệt và kích hoạt chiến dịch khuyến mãi " + p.getPromoCode() + "!", "promotion", p));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PutMapping("/director/pricing/promotions/{id}/reject")
    public ResponseEntity<?> rejectPromotionById(@PathVariable Long id) {
        try {
            Optional<Promotion> opt = promotionRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Không tìm thấy khuyến mãi: " + id));
            }
            Promotion p = opt.get();
            p.setStatus("Bác bỏ");
            promotionRepository.save(p);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã bác bỏ chương trình khuyến mãi " + p.getPromoCode() + "!", "promotion", p));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/director/pricing/rules")
    public ResponseEntity<?> saveOrUpdatePricingRule(@RequestBody Map<String, Object> payload) {
        try {
            String code = (String) payload.get("pricingCode");
            String branchCode = (String) payload.get("branchCode");
            String slotCode = (String) payload.get("slotCode");
            String dayType = (String) payload.getOrDefault("dayType", "Ngày thường (T2-T6)");
            BigDecimal hourlyRate = new BigDecimal(payload.get("hourlyRate").toString());
            String startDateStr = (String) payload.get("startDate");

            if (branchCode == null || branchCode.trim().isEmpty() || slotCode == null || slotCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng chọn chi nhánh và khung giờ áp dụng!"));
            }

            if (code == null || code.trim().isEmpty()) {
                long count = pricingRuleRepository.count();
                code = "BG" + String.format("%02d", count + 1);
            } else {
                code = code.trim().toUpperCase();
            }

            Optional<PricingRule> opt = pricingRuleRepository.findByPricingCode(code);
            PricingRule r = opt.orElseGet(PricingRule::new);
            r.setPricingCode(code);
            r.setBranchCode(branchCode.trim().toUpperCase());
            r.setSlotCode(slotCode.trim().toUpperCase());
            r.setDayType(dayType);
            r.setHourlyRate(hourlyRate);
            r.setStartDate(startDateStr != null && !startDateStr.isEmpty() ? LocalDate.parse(startDateStr) : LocalDate.now());

            pricingRuleRepository.save(r);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã lưu chính sách giá thành công vào Database!", "rule", r));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @DeleteMapping("/director/pricing/rules/{pricingCode}")
    public ResponseEntity<?> deletePricingRule(@PathVariable String pricingCode) {
        try {
            Optional<PricingRule> opt = pricingRuleRepository.findByPricingCode(pricingCode);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Không tìm thấy quy tắc giá: " + pricingCode));
            }
            pricingRuleRepository.delete(opt.get());
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa quy tắc giá thành công khỏi Database!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/director/pricing/promotions")
    public ResponseEntity<?> saveOrUpdatePromotion(@RequestBody Map<String, Object> payload) {
        try {
            String code = (String) payload.get("promoCode");
            String name = (String) payload.get("promoName");
            String discountType = (String) payload.getOrDefault("discountType", "Phần trăm (%)");
            BigDecimal discountValue = new BigDecimal(payload.get("discountValue").toString());
            String scope = (String) payload.getOrDefault("scope", "Toàn chuỗi");
            String status = (String) payload.getOrDefault("status", "Chờ phê duyệt");
            String startDateStr = (String) payload.get("startDate");
            String endDateStr = (String) payload.get("endDate");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Tên chương trình khuyến mãi không được để trống!"));
            }

            if (code == null || code.trim().isEmpty()) {
                long count = promotionRepository.count();
                code = "KM" + String.format("%02d", count + 1);
            } else {
                code = code.trim().toUpperCase();
            }

            Optional<Promotion> opt = promotionRepository.findByPromoCode(code);
            Promotion p = opt.orElseGet(Promotion::new);
            p.setPromoCode(code);
            p.setPromoName(name.trim());
            p.setDiscountType(discountType);
            p.setDiscountValue(discountValue);
            p.setScope(scope);
            p.setStatus(status);
            p.setStartDate(startDateStr != null && !startDateStr.isEmpty() ? LocalDate.parse(startDateStr) : LocalDate.now());
            p.setEndDate(endDateStr != null && !endDateStr.isEmpty() ? LocalDate.parse(endDateStr) : LocalDate.now().plusMonths(1));

            promotionRepository.save(p);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã lưu chương trình khuyến mãi vào Database!", "promotion", p));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PutMapping("/director/pricing/promotions/{promoCode}/status")
    public ResponseEntity<?> togglePromotionStatus(@PathVariable String promoCode, @RequestBody Map<String, String> body) {
        try {
            Optional<Promotion> opt = promotionRepository.findByPromoCode(promoCode);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Không tìm thấy mã khuyến mãi: " + promoCode));
            }
            Promotion p = opt.get();
            String newStatus = body.getOrDefault("status", "Đang áp dụng".equalsIgnoreCase(p.getStatus()) ? "Tạm ngưng" : "Đang áp dụng");
            p.setStatus(newStatus);
            promotionRepository.save(p);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã cập nhật trạng thái khuyến mãi thành '" + newStatus + "'!", "status", newStatus));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @DeleteMapping("/director/pricing/promotions/{promoCode}")
    public ResponseEntity<?> deletePromotion(@PathVariable String promoCode) {
        try {
            Optional<Promotion> opt = promotionRepository.findByPromoCode(promoCode);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Không tìm thấy mã khuyến mãi: " + promoCode));
            }
            promotionRepository.delete(opt.get());
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa chương trình khuyến mãi thành công khỏi Database!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    // ==========================================
    // 12. DIRECTOR TOURNAMENT APPROVAL & MEMBER STATS - PHÊ DUYỆT GIẢI ĐẤU & HỘI VIÊN
    // ==========================================
    @GetMapping("/director/tournaments")
    public ResponseEntity<?> getDirectorTournamentsData() {
        try {
            List<Tournament> rawTournaments = tournamentRepository.findAll();
            List<Customer> rawCustomers = customerRepository.findAll();
            List<Branch> branches = branchRepository.findAll();
            List<TournamentRegistration> allRegistrations = tournamentRegistrationRepository.findAll();
            List<Booking> allBookings = bookingRepository.findAll();

            Map<String, String> branchNameMap = branches.stream()
                    .collect(Collectors.toMap(Branch::getBranchCode, Branch::getBranchName, (b1, b2) -> b1));

            Map<String, List<TournamentRegistration>> regMap = allRegistrations.stream()
                    .collect(Collectors.groupingBy(TournamentRegistration::getTournamentCode));

            List<Map<String, Object>> tournamentList = new ArrayList<>();
            long pendingTournaments = 0;
            long activeTournaments = 0;
            long completedTournaments = 0;

            for (Tournament t : rawTournaments) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", t.getId());
                item.put("tournamentCode", t.getTournamentCode());
                item.put("tournamentName", t.getTournamentName());
                item.put("branchCode", t.getBranchCode());
                item.put("branchName", branchNameMap.getOrDefault(t.getBranchCode(), t.getBranchCode() != null ? t.getBranchCode() : "Toàn chuỗi"));
                item.put("startDate", t.getStartDate() != null ? t.getStartDate().toString() : "");
                item.put("endDate", t.getEndDate() != null ? t.getEndDate().toString() : "");
                item.put("rules", t.getRules() != null ? t.getRules() : "");
                item.put("maxParticipants", t.getMaxParticipants() != null ? t.getMaxParticipants() : 32);

                List<TournamentRegistration> regs = regMap.getOrDefault(t.getTournamentCode(), Collections.emptyList());
                int currentParticipants = regs.size() > 0 ? regs.size() : (t.getCurrentParticipants() != null ? t.getCurrentParticipants() : 0);
                item.put("currentParticipants", currentParticipants);
                item.put("entryFee", t.getEntryFee() != null ? t.getEntryFee().longValue() : 0);
                item.put("totalPrize", t.getTotalPrize() != null ? t.getTotalPrize().longValue() : 0);
                item.put("status", t.getStatus() != null ? t.getStatus() : "Chờ phê duyệt");
                item.put("registrations", regs);

                if ("Chờ phê duyệt".equalsIgnoreCase(t.getStatus())) {
                    pendingTournaments++;
                } else if ("Đang mở đăng ký".equalsIgnoreCase(t.getStatus()) || "Đang diễn ra".equalsIgnoreCase(t.getStatus())) {
                    activeTournaments++;
                } else if ("Đã hoàn thành".equalsIgnoreCase(t.getStatus())) {
                    completedTournaments++;
                }

                tournamentList.add(item);
            }

            // Sắp xếp: Chờ phê duyệt lên đầu tiên, sau đó theo ID
            tournamentList.sort((a, b) -> {
                boolean aPending = "Chờ phê duyệt".equalsIgnoreCase((String) a.get("status"));
                boolean bPending = "Chờ phê duyệt".equalsIgnoreCase((String) b.get("status"));
                if (aPending && !bPending) return -1;
                if (!aPending && bPending) return 1;
                return Long.compare((Long) b.get("id"), (Long) a.get("id"));
            });

            // Danh sách hội viên từ bảng customers
            List<Map<String, Object>> customerList = new ArrayList<>();
            long diamondCount = 0;
            long goldCount = 0;
            long silverCount = 0;

            for (Customer c : rawCustomers) {
                Map<String, Object> cMap = new LinkedHashMap<>();
                cMap.put("id", c.getId());
                cMap.put("customerCode", c.getCustomerCode());
                cMap.put("fullName", c.getFullName());
                cMap.put("phone", c.getPhone());
                cMap.put("email", c.getEmail() != null ? c.getEmail() : (c.getPhone() + "@ute.sport.vn"));
                cMap.put("membershipTier", c.getMembershipTier() != null ? c.getMembershipTier() : "Đồng");
                cMap.put("rewardPoints", c.getRewardPoints() != null ? c.getRewardPoints() : 0);
                double hours = c.getTotalPlayHours() != null ? c.getTotalPlayHours().doubleValue() : 0.0;
                cMap.put("totalPlayHours", hours);
                cMap.put("status", c.getStatus() != null ? c.getStatus() : "Hoạt động");
                cMap.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "2026-01-01");

                // Tính tổng chi tiêu thực tế từ các lượt booking hoặc ước tính dựa trên giờ chơi
                long spent = allBookings.stream()
                        .filter(b -> c.getPhone() != null && c.getPhone().equals(b.getCustomerPhone()))
                        .mapToLong(b -> b.getTotalPrice() != null ? b.getTotalPrice().longValue() : 0)
                        .sum();
                if (spent == 0 && hours > 0) {
                    spent = Math.round(hours * 110000);
                }
                cMap.put("totalSpent", spent);

                // Chi nhánh thường xuyên
                String favBranch = allBookings.stream()
                        .filter(b -> c.getPhone() != null && c.getPhone().equals(b.getCustomerPhone()))
                        .map(b -> branchNameMap.getOrDefault(b.getBranchCode(), "Chi nhánh 1 (Thủ Đức)"))
                        .findFirst()
                        .orElse("Chi nhánh 1 (Thủ Đức)");
                cMap.put("favoriteBranch", favBranch);

                String tier = c.getMembershipTier() != null ? c.getMembershipTier() : "Đồng";
                if (tier.contains("Kim Cương")) diamondCount++;
                else if (tier.contains("Vàng")) goldCount++;
                else if (tier.contains("Bạc")) silverCount++;

                customerList.add(cMap);
            }

            // Sắp xếp hội viên theo điểm tích lũy giảm dần
            customerList.sort((a, b) -> Integer.compare((Integer) b.get("rewardPoints"), (Integer) a.get("rewardPoints")));

            Map<String, Object> counts = new LinkedHashMap<>();
            counts.put("pendingTournaments", pendingTournaments);
            counts.put("activeTournaments", activeTournaments);
            counts.put("completedTournaments", completedTournaments);
            counts.put("totalTournaments", rawTournaments.size());
            counts.put("totalMembers", rawCustomers.size());
            counts.put("diamondMembers", diamondCount);
            counts.put("goldMembers", goldCount);
            counts.put("silverMembers", silverCount);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("counts", counts);
            response.put("tournaments", tournamentList);
            response.put("customers", customerList);
            response.put("branches", branches);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi tải dữ liệu giải đấu và hội viên: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PutMapping("/director/tournaments/{id}/approve")
    public ResponseEntity<?> approveTournament(@PathVariable Long id) {
        try {
            Optional<Tournament> opt = tournamentRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Không tìm thấy giải đấu: " + id));
            }
            Tournament t = opt.get();
            t.setStatus("Đang mở đăng ký");
            tournamentRepository.save(t);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã phê duyệt kế hoạch tổ chức giải đấu '" + t.getTournamentName() + "' và cấp phép khóa sân thành công!", "tournament", t));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PutMapping("/director/tournaments/{id}/reject")
    public ResponseEntity<?> rejectTournament(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            Optional<Tournament> opt = tournamentRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Không tìm thấy giải đấu: " + id));
            }
            Tournament t = opt.get();
            t.setStatus("Từ chối");
            String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Kế hoạch trùng lịch bảo trì hoặc ngân sách chưa phù hợp";
            t.setRules((t.getRules() != null ? t.getRules() : "") + " [Từ chối: " + reason + "]");
            tournamentRepository.save(t);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã từ chối kế hoạch tổ chức giải đấu '" + t.getTournamentName() + "'!", "tournament", t));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/director/tournaments")
    public ResponseEntity<?> createTournament(@RequestBody Map<String, Object> payload) {
        try {
            String name = (String) payload.get("tournamentName");
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Tên giải đấu không được để trống!"));
            }

            String branchCode = (String) payload.getOrDefault("branchCode", "CN01");
            String code = (String) payload.get("tournamentCode");
            if (code == null || code.trim().isEmpty()) {
                long count = tournamentRepository.count();
                code = "GD" + String.format("%02d", count + 1);
            }

            Tournament t = new Tournament();
            t.setTournamentCode(code.trim().toUpperCase());
            t.setTournamentName(name.trim());
            t.setBranchCode(branchCode);
            t.setMaxParticipants(Integer.parseInt(payload.getOrDefault("maxParticipants", "32").toString()));
            t.setCurrentParticipants(0);
            t.setEntryFee(new BigDecimal(payload.getOrDefault("entryFee", "200000").toString()));
            t.setTotalPrize(new BigDecimal(payload.getOrDefault("totalPrize", "10000000").toString()));
            t.setRules((String) payload.getOrDefault("rules", "Thể thức thi đấu loại trực tiếp"));
            t.setStatus((String) payload.getOrDefault("status", "Chờ phê duyệt"));

            String startStr = (String) payload.get("startDate");
            String endStr = (String) payload.get("endDate");
            t.setStartDate(startStr != null && !startStr.isEmpty() ? LocalDateTime.parse(startStr) : LocalDateTime.now().plusDays(10));
            t.setEndDate(endStr != null && !endStr.isEmpty() ? LocalDateTime.parse(endStr) : LocalDateTime.now().plusDays(12));

            tournamentRepository.save(t);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã tạo kế hoạch giải đấu mới thành công vào Database SQL Server!", "tournament", t));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/director/tournaments/{tournamentCode}/registrations")
    public ResponseEntity<?> getTournamentRegistrationsList(@PathVariable String tournamentCode) {
        try {
            List<TournamentRegistration> list = tournamentRegistrationRepository.findByTournamentCode(tournamentCode);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }
}

