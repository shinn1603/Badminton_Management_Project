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

    private final Map<String, Integer> failedAttempts = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Long> lockoutExpiry = new java.util.concurrent.ConcurrentHashMap<>();

    public Map<String, Object> authenticate(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Vui lòng nhập tài khoản và mật khẩu!");
        }

        String userKey = username.trim().toLowerCase();
        Long lockoutUntil = lockoutExpiry.get(userKey);
        if (lockoutUntil != null) {
            if (System.currentTimeMillis() < lockoutUntil) {
                long remainingSec = (lockoutUntil - System.currentTimeMillis()) / 1000;
                throw new IllegalStateException("Tài khoản tạm thời bị khóa do nhập sai mật khẩu quá 5 lần. Vui lòng thử lại sau " + Math.max(1, remainingSec / 60) + " phút!");
            } else {
                lockoutExpiry.remove(userKey);
                failedAttempts.remove(userKey);
            }
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
            int attempts = failedAttempts.merge(userKey, 1, Integer::sum);
            if (attempts >= 5) {
                lockoutExpiry.put(userKey, System.currentTimeMillis() + 15 * 60 * 1000);
            }
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác!");
        }

        // Reset failed attempts upon successful login
        failedAttempts.remove(userKey);
        lockoutExpiry.remove(userKey);

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
        boolean hasDigitOrLetter = password.trim().matches(".*[a-zA-Z].*") && password.trim().matches(".*[0-9].*");
        if (!hasDigitOrLetter) {
            throw new IllegalArgumentException("Mật khẩu phải chứa cả chữ cái và số để đảm bảo tính an toàn!");
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
        List<User> users = new ArrayList<>(userRepository.findAll());
        Set<String> existingPhones = users.stream()
                .map(User::getPhone)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> existingUsernames = users.stream()
                .map(User::getUsername)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toSet());

        try {
            List<Customer> customers = customerRepository.findAll();
            for (Customer c : customers) {
                String uname = c.getCustomerCode() != null ? c.getCustomerCode().toLowerCase() : "";
                String phone = c.getPhone() != null ? c.getPhone().trim() : "";
                if ((uname.isEmpty() || !existingUsernames.contains(uname)) && (phone.isEmpty() || !existingPhones.contains(phone))) {
                    User u = new User();
                    u.setId(c.getId() != null ? c.getId() + 1000L : (long) (users.size() + 1));
                    u.setUsername(c.getCustomerCode() != null && !c.getCustomerCode().isBlank() ? c.getCustomerCode() : (phone.isEmpty() ? ("kh_" + c.getId()) : phone));
                    u.setFullName(c.getFullName() != null ? c.getFullName() : "Khách hàng");
                    u.setPhone(c.getPhone() != null ? c.getPhone() : "");
                    u.setEmail(c.getEmail() != null ? c.getEmail() : "");
                    u.setRole("CUSTOMER");
                    u.setBranchCode("CN01");
                    u.setStatus(c.getStatus() != null ? c.getStatus() : "Hoạt động");
                    u.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt() : LocalDateTime.now());
                    users.add(u);
                    existingUsernames.add(u.getUsername().toLowerCase());
                    if (u.getPhone() != null && !u.getPhone().trim().isEmpty()) {
                        existingPhones.add(u.getPhone().trim());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi đồng bộ danh sách khách hàng vào tài khoản người dùng: {}", e.getMessage());
        }
        return users;
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
            u.setPassword(passwordEncoder.encode("Badminton@2026"));
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

    public User toggleUserStatus(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống!");
        }
        String cleanUser = username.trim();
        Optional<User> userOpt = userRepository.findByUsername(cleanUser);

        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            Optional<Customer> custOpt = customerRepository.findByCustomerCode(cleanUser)
                    .or(() -> customerRepository.findByPhone(cleanUser));
            if (custOpt.isPresent()) {
                Customer c = custOpt.get();
                user = new User();
                user.setUsername(cleanUser);
                user.setFullName(c.getFullName() != null ? c.getFullName() : "Khách hàng");
                user.setPhone(c.getPhone() != null ? c.getPhone() : "");
                user.setEmail(c.getEmail() != null ? c.getEmail() : "");
                user.setRole("CUSTOMER");
                user.setBranchCode("CN01");
                user.setStatus(c.getStatus() != null ? c.getStatus() : "Hoạt động");
                user.setPassword(c.getPassword() != null ? c.getPassword() : passwordEncoder.encode("123456"));
            } else {
                throw new IllegalArgumentException("Không tìm thấy tài khoản người dùng: " + cleanUser);
            }
        }

        String newStatus;
        if ("Hoạt động".equalsIgnoreCase(user.getStatus())) {
            newStatus = "Bị khóa";
        } else {
            newStatus = "Hoạt động";
        }
        user.setStatus(newStatus);
        User saved = userRepository.save(user);

        // Sync to Customer table if matching customer exists
        customerRepository.findByCustomerCode(cleanUser)
                .or(() -> customerRepository.findByPhone(cleanUser))
                .or(() -> (user.getPhone() != null && !user.getPhone().isEmpty()) ? customerRepository.findByPhone(user.getPhone()) : Optional.empty())
                .ifPresent(cust -> {
                    cust.setStatus(newStatus);
                    customerRepository.save(cust);
                });

        log.info("Cập nhật trạng thái người dùng {} thành {}", saved.getUsername(), saved.getStatus());
        return saved;
    }
}
