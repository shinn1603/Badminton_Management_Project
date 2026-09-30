package vn.yain.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.yain.entity.*;
import vn.yain.repository.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    // COURTS API
    @GetMapping("/courts")
    public List<Court> getCourts(@RequestParam(required = false) String branchCode) {
        if (branchCode != null && !branchCode.isEmpty()) {
            return courtRepository.findByBranchCode(branchCode);
        }
        return courtRepository.findAll();
    }

    // BOOKINGS API
    @GetMapping("/bookings")
    public List<Booking> getBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(@RequestBody Booking booking) {
        if (booking.getBookingCode() == null || booking.getBookingCode().isEmpty()) {
            booking.setBookingCode("DS0" + (100 + (int)(Math.random() * 900)));
        }
        if (booking.getBranchCode() == null) {
            booking.setBranchCode("CN01");
        }
        Booking saved = bookingRepository.save(booking);
        return ResponseEntity.ok(saved);
    }

    // PRODUCTS / POS ITEMS API
    @GetMapping("/products")
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    // BRANCHES API
    @GetMapping("/branches")
    public List<Branch> getBranches() {
        return branchRepository.findAll();
    }

    // AUTH LOGIN API
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu thông tin đăng nhập!"));
        }

        Optional<User> userOpt = userRepository.findByUsernameAndPassword(username.trim(), password);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("username", u.getUsername());
            resp.put("fullName", u.getFullName());
            resp.put("role", u.getRole());
            resp.put("branchCode", u.getBranchCode());
            return ResponseEntity.ok(resp);
        }

        return ResponseEntity.status(401).body(Map.of("success", false, "message", "Sai tài khoản hoặc mật khẩu trong SQL Server!"));
    }
}
