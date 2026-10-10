package vn.yain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.yain.entity.Customer;
import vn.yain.entity.User;
import vn.yain.repository.CustomerRepository;
import vn.yain.repository.UserRepository;
import vn.yain.security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Map<String, Object> authenticate(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Vui lòng nhập tài khoản và mật khẩu!");
        }

        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác!"));

        String storedPassword = user.getPassword();
        boolean isMatch = false;

        if (storedPassword != null && (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$"))) {
            isMatch = passwordEncoder.matches(password.trim(), storedPassword);
        } else {
            // Legacy plaintext fallback - verify and upgrade to BCrypt immediately
            if (storedPassword != null && storedPassword.equals(password.trim())) {
                isMatch = true;
                user.setPassword(passwordEncoder.encode(password.trim()));
                userRepository.save(user);
                log.info("Nâng cấp mật khẩu sang BCrypt thành công cho người dùng: {}", user.getUsername());
            }
        }

        if (!isMatch) {
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác!");
        }

        if ("Khóa".equalsIgnoreCase(user.getStatus()) || "Bị khóa".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("Tài khoản đã bị tạm khóa, vui lòng liên hệ quản trị viên!");
        }

        String token = jwtTokenProvider.generateToken(
                user.getUsername(),
                user.getRole(),
                user.getFullName(),
                user.getBranchCode()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");
        result.put("username", user.getUsername());
        result.put("fullName", user.getFullName());
        result.put("role", user.getRole());
        result.put("branchCode", user.getBranchCode());
        result.put("phone", user.getPhone() != null ? user.getPhone() : "");
        result.put("email", user.getEmail() != null ? user.getEmail() : "");
        return result;
    }

    @Transactional
    public Map<String, Object> registerCustomer(String username, String password, String fullName, String phone, String email) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập và mật khẩu không được để trống!");
        }
        if (password.trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có độ dài tối thiểu từ 6 ký tự trở lên!");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống!");
        }

        if (userRepository.findByUsername(username.trim()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại trong hệ thống!");
        }

        if (phone != null && !phone.trim().isEmpty() && userRepository.findByPhone(phone.trim()).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại đã được đăng ký bởi tài khoản khác!");
        }

        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPassword(passwordEncoder.encode(password.trim()));
        newUser.setFullName(fullName.trim());
        newUser.setPhone(phone != null ? phone.trim() : "");
        newUser.setEmail(email != null ? email.trim() : "");
        newUser.setRole("CUSTOMER");
        newUser.setBranchCode("CN01");
        newUser.setStatus("Hoạt động");
        newUser.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(newUser);

        // Synchronize customer profile table
        try {
            Customer c = new Customer();
            c.setCustomerCode("KH" + String.format("%04d", savedUser.getId()));
            c.setFullName(savedUser.getFullName());
            c.setPhone(savedUser.getPhone());
            c.setEmail(savedUser.getEmail());
            c.setPassword(savedUser.getPassword());
            c.setMembershipTier("Đồng");
            c.setStatus("Hoạt động");
            c.setCreatedAt(LocalDateTime.now());
            customerRepository.save(c);
        } catch (Exception e) {
            log.warn("Không thể đồng bộ hồ sơ khách hàng: {}", e.getMessage());
        }

        String token = jwtTokenProvider.generateToken(
                savedUser.getUsername(),
                savedUser.getRole(),
                savedUser.getFullName(),
                savedUser.getBranchCode()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");
        result.put("userId", savedUser.getId());
        result.put("username", savedUser.getUsername());
        result.put("fullName", savedUser.getFullName());
        result.put("role", savedUser.getRole());
        result.put("branchCode", savedUser.getBranchCode());
        return result;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User saveOrUpdateUser(Map<String, String> payload) {
        String username = payload.get("username");
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống!");
        }
        Optional<User> existing = userRepository.findByUsername(username.trim());
        User u = existing.orElseGet(User::new);
        u.setUsername(username.trim());
        if (payload.containsKey("password") && payload.get("password") != null && !payload.get("password").isBlank()) {
            u.setPassword(passwordEncoder.encode(payload.get("password").trim()));
        } else if (u.getPassword() == null) {
            u.setPassword(passwordEncoder.encode("123456"));
        }
        if (payload.containsKey("fullName")) u.setFullName(payload.get("fullName"));
        if (payload.containsKey("role")) u.setRole(payload.get("role").toUpperCase());
        if (payload.containsKey("branchCode")) u.setBranchCode(payload.get("branchCode"));
        if (payload.containsKey("phone")) u.setPhone(payload.get("phone"));
        if (payload.containsKey("email")) u.setEmail(payload.get("email"));
        if (payload.containsKey("status")) u.setStatus(payload.get("status"));
        return userRepository.save(u);
    }

    public void resetPassword(String identifier, String newPassword) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên tài khoản, số điện thoại hoặc email!");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }

        String search = identifier.trim();
        User user = userRepository.findByUsername(search)
                .or(() -> userRepository.findByPhone(search))
                .or(() -> userRepository.findAll().stream().filter(u -> search.equalsIgnoreCase(u.getEmail())).findFirst())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với thông tin đã cung cấp!"));

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);

        // Also update Customer record if exists
        customerRepository.findAll().stream()
                .filter(c -> search.equalsIgnoreCase(c.getPhone()) || search.equalsIgnoreCase(c.getEmail()))
                .findFirst()
                .ifPresent(cust -> {
                    cust.setPassword(user.getPassword());
                    customerRepository.save(cust);
                });

        log.info("Đặt lại mật khẩu thành công cho tài khoản: {}", user.getUsername());
    }
}
