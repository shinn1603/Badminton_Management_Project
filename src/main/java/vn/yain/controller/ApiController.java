package vn.yain.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
import vn.yain.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import vn.yain.service.CloudinaryService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private CourtService courtService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ProductRepository productRepository;

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
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Lỗi máy chủ: " + e.getMessage()));
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
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Lỗi máy chủ: " + e.getMessage()));
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
        return ResponseEntity.ok(Map.of(
                "success", true,
                "username", username,
                "role", role,
                "valid", true
        ));
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
    public List<Booking> getBookings() {
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
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Lỗi máy chủ: " + e.getMessage()));
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
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Lỗi máy chủ: " + e.getMessage()));
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

            // Hourly rate check from court or default
            BigDecimal hourlyRate = (startHour >= 17 && startHour < 21) ? new BigDecimal("140000") : new BigDecimal("90000");
            Optional<Court> courtOpt = courtRepository.findByCourtCode(courtCode);
            if (courtOpt.isPresent() && courtOpt.get().getHourlyRate() != null) {
                hourlyRate = courtOpt.get().getHourlyRate();
                if (startHour >= 17 && startHour < 21) {
                    hourlyRate = hourlyRate.multiply(new BigDecimal("1.25")); // peak hour boost
                }
            }

            BigDecimal totalPrice = hourlyRate.multiply(new BigDecimal(duration));
            BigDecimal deposit = totalPrice.multiply(new BigDecimal("0.3"));

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
    public ResponseEntity<?> handleVietQRWebhook(@RequestBody Map<String, Object> webhookData) {
        try {
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
                    b.setStatus("Đã cọc 30%");
                    bookingRepository.save(b);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Auto-confirmed deposit for " + bookingCode));
                }
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "Webhook received successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private boolean isSlotOverlapping(String s1, String s2) {
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

    @GetMapping("/branches")
    public List<Branch> getBranches() {
        return branchRepository.findAll();
    }

    @GetMapping("/invoices")
    public List<Invoice> getInvoices() {
        return invoiceRepository.findAllByOrderByInvoiceDateDesc();
    }

    @GetMapping("/tournaments")
    public List<Tournament> getTournaments() {
        return tournamentRepository.findAll();
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
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Lỗi máy chủ: " + e.getMessage()));
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
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Lỗi máy chủ: " + e.getMessage()));
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
