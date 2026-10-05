package vn.yain.service;

import org.springframework.beans.factory.annotation.Autowired;
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
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public Map<String, Object> authenticate(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Vui lòng nhập tài khoản và mật khẩu!");
        }

        Optional<User> userOpt = userRepository.findByUsernameAndPassword(username.trim(), password.trim());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác!");
        }

        User user = userOpt.get();
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

    public Map<String, Object> registerCustomer(String username, String password, String fullName, String phone, String email) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập và mật khẩu không được để trống!");
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
        newUser.setPassword(password.trim());
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
        } catch (Exception ignored) {
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
}
