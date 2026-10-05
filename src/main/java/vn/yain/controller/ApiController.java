package vn.yain.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.yain.dto.CourtEventDto;
import vn.yain.entity.*;
import vn.yain.repository.*;
import vn.yain.security.JwtTokenProvider;
import vn.yain.service.BookingService;
import vn.yain.service.CourtService;
import vn.yain.service.UserService;

import java.math.BigDecimal;
import java.util.*;

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
    private JwtTokenProvider jwtTokenProvider;

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
}
