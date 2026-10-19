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

        String identifier = username.trim();
        String userKey = identifier.toLowerCase();
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

        // 1. Tìm kiếm trong bảng users theo username, phone, hoặc email
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByPhone(identifier))
                .or(() -> identifier.contains("@") ? userRepository.findByEmail(identifier) : Optional.empty())
                .orElse(null);

        // 2. Nếu chưa có trong bảng users, tìm trong bảng customers (KH001 - KH006, SĐT, Email)
        if (user == null) {
            Customer cust = customerRepository.findByPhone(identifier)
                    .or(() -> customerRepository.findByCustomerCode(identifier))
                    .or(() -> identifier.contains("@") ? customerRepository.findByEmail(identifier) : Optional.empty())
                    .orElse(null);

            if (cust != null) {
                // Xác thực mật khẩu theo hồ sơ khách hàng
                String custStoredPass = cust.getPassword();
                boolean custMatch = false;
                if (custStoredPass != null && (custStoredPass.startsWith("$2a$") || custStoredPass.startsWith("$2b$") || custStoredPass.startsWith("$2y$"))) {
                    custMatch = passwordEncoder.matches(password.trim(), custStoredPass);
                } else if (custStoredPass != null && custStoredPass.equals(password.trim())) {
                    custMatch = true;
                    // Nâng cấp mật khẩu khách hàng lên BCrypt
                    cust.setPassword(passwordEncoder.encode(password.trim()));
                    customerRepository.save(cust);
                }

                if (!custMatch) {
                    int attempts = failedAttempts.merge(userKey, 1, Integer::sum);
                    if (attempts >= 5) {
                        lockoutExpiry.put(userKey, System.currentTimeMillis() + 15 * 60 * 1000);
                    }
                    throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác!");
                }

                // Tự động tạo và đồng bộ tài khoản người dùng tương ứng
                String preferredUsername = (cust.getPhone() != null && !cust.getPhone().trim().isEmpty())
                        ? cust.getPhone().trim()
                        : cust.getCustomerCode().toLowerCase();
                if (userRepository.findByUsername(preferredUsername).isPresent()) {
                    preferredUsername = cust.getCustomerCode().toLowerCase();
                }

                user = new User();
                user.setUsername(preferredUsername);
                user.setPassword(passwordEncoder.encode(password.trim()));
                user.setFullName(cust.getFullName() != null ? cust.getFullName() : "Khách Hàng");
                user.setPhone(cust.getPhone() != null ? cust.getPhone() : "");
                user.setEmail(cust.getEmail() != null ? cust.getEmail() : "");
                user.setRole("CUSTOMER");
                user.setBranchCode("CN01");
                user.setStatus(cust.getStatus() != null ? cust.getStatus() : "Hoạt động");
                user.setCreatedAt(LocalDateTime.now());
                user = userRepository.save(user);
                log.info("Tự động đồng bộ tài khoản người dùng cho khách hàng: {}", user.getUsername());
            } else {
                throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác!");
            }
        } else {
            // Xác thực mật khẩu với bản ghi User
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
        if (payload.containsKey("status") && payload.get("status") != null && !payload.get("status").isBlank()) {
            String newStat = payload.get("status").trim();
            u.setStatus(newStat);
            customerRepository.findByCustomerCode(username.trim())
                    .or(() -> customerRepository.findByCustomerCodeIgnoreCase(username.trim()))
                    .or(() -> customerRepository.findByPhone(username.trim()))
                    .ifPresent(c -> {
                        c.setStatus(newStat);
                        customerRepository.save(c);
                    });
        }
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
                .or(() -> search.contains("@") ? userRepository.findByEmail(search) : Optional.empty())
                .orElse(null);

        if (user == null) {
            Customer cust = customerRepository.findByPhone(search)
                    .or(() -> customerRepository.findByCustomerCode(search))
                    .or(() -> search.contains("@") ? customerRepository.findByEmail(search) : Optional.empty())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với thông tin đã cung cấp!"));

            String encodedNewPass = passwordEncoder.encode(newPassword.trim());
            cust.setPassword(encodedNewPass);
            customerRepository.save(cust);

            String preferredUsername = (cust.getPhone() != null && !cust.getPhone().trim().isEmpty())
                    ? cust.getPhone().trim()
                    : cust.getCustomerCode().toLowerCase();
            User newUser = new User();
            newUser.setUsername(preferredUsername);
            newUser.setPassword(encodedNewPass);
            newUser.setFullName(cust.getFullName() != null ? cust.getFullName() : "Khách Hàng");
            newUser.setPhone(cust.getPhone() != null ? cust.getPhone() : "");
            newUser.setEmail(cust.getEmail() != null ? cust.getEmail() : "");
            newUser.setRole("CUSTOMER");
            newUser.setBranchCode("CN01");
            newUser.setStatus("Hoạt động");
            newUser.setCreatedAt(LocalDateTime.now());
            userRepository.save(newUser);
            log.info("Đặt lại mật khẩu thành công cho tài khoản khách hàng: {}", newUser.getUsername());
        } else {
            String encodedNewPass = passwordEncoder.encode(newPassword.trim());
            user.setPassword(encodedNewPass);
            userRepository.save(user);

            // Cập nhật bản ghi customer nếu tồn tại
            if (user.getPhone() != null && !user.getPhone().isBlank()) {
                customerRepository.findByPhone(user.getPhone().trim()).ifPresent(c -> {
                    c.setPassword(encodedNewPass);
                    customerRepository.save(c);
                });
            }
            log.info("Đặt lại mật khẩu thành công cho tài khoản: {}", user.getUsername());
        }
    }

    public User toggleUserStatus(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống!");
        }
        String cleanUser = username.trim();
        Optional<User> userOpt = userRepository.findByUsername(cleanUser)
                .or(() -> userRepository.findByUsernameIgnoreCase(cleanUser))
                .or(() -> userRepository.findByPhone(cleanUser));

        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            Optional<Customer> custOpt = customerRepository.findByCustomerCode(cleanUser)
                    .or(() -> customerRepository.findByCustomerCodeIgnoreCase(cleanUser))
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

        String curStatus = (user.getStatus() != null) ? user.getStatus().trim() : "Hoạt động";
        boolean isCurrentlyLocked = curStatus.toLowerCase().contains("khóa") || curStatus.equalsIgnoreCase("locked") || curStatus.equalsIgnoreCase("inactive");
        String newStatus = isCurrentlyLocked ? "Hoạt động" : "Bị khóa";

        user.setStatus(newStatus);
        User saved = userRepository.save(user);

        // Sync to Customer table if matching customer exists
        customerRepository.findByCustomerCode(cleanUser)
                .or(() -> customerRepository.findByCustomerCodeIgnoreCase(cleanUser))
                .or(() -> customerRepository.findByPhone(cleanUser))
                .or(() -> (user.getPhone() != null && !user.getPhone().isEmpty()) ? customerRepository.findByPhone(user.getPhone()) : Optional.empty())
                .ifPresent(cust -> {
                    cust.setStatus(newStatus);
                    customerRepository.save(cust);
                });

        log.info("Cập nhật trạng thái người dùng {} thành {}", saved.getUsername(), saved.getStatus());
        return saved;
    }

    public Customer createCustomer(String fullName, String phone, String email, String membershipTier) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên khách hàng không được để trống!");
        }
        if (phone == null || !phone.trim().matches("0\\d{9}")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (yêu cầu 10 chữ số, bắt đầu bằng 0)!");
        }
        String cleanPhone = phone.trim();
        if (customerRepository.findByPhone(cleanPhone).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại " + cleanPhone + " đã tồn tại trong hệ thống!");
        }

        Customer c = new Customer();
        long count = customerRepository.count();
        c.setCustomerCode("KH" + String.format("%04d", count + 1));
        c.setFullName(fullName.trim());
        c.setPhone(cleanPhone);
        c.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : (cleanPhone + "@utesport.vn"));
        c.setPassword(passwordEncoder.encode("123456"));
        c.setMembershipTier(membershipTier != null && !membershipTier.trim().isEmpty() ? membershipTier : "Đồng");
        c.setStatus("Hoạt động");
        c.setCreatedAt(LocalDateTime.now());
        Customer saved = customerRepository.save(c);

        // Sync to User table
        if (!userRepository.findByUsername(cleanPhone).isPresent()) {
            User u = new User();
            u.setUsername(cleanPhone);
            u.setFullName(saved.getFullName());
            u.setPhone(cleanPhone);
            u.setEmail(saved.getEmail());
            u.setPassword(saved.getPassword());
            u.setRole("CUSTOMER");
            u.setBranchCode("CN01");
            u.setStatus("Hoạt động");
            u.setCreatedAt(LocalDateTime.now());
            userRepository.save(u);
        }

        log.info("Đã tạo mới khách hàng thành công: {} ({})", saved.getFullName(), saved.getCustomerCode());
        return saved;
    }
}
