package vn.yain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import vn.yain.entity.Customer;
import vn.yain.entity.User;
import vn.yain.repository.CustomerRepository;
import vn.yain.repository.UserRepository;
import vn.yain.security.JwtTokenProvider;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho UserService (Bao phu Use Case XL-01 & UC-02)
 */
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(passwordEncoder.encode(any(CharSequence.class))).thenAnswer(inv -> "encoded_" + inv.getArgument(0));
        when(passwordEncoder.matches(any(CharSequence.class), anyString())).thenAnswer(inv -> {
            CharSequence raw = inv.getArgument(0);
            String encoded = inv.getArgument(1);
            return raw != null && (raw.toString().equals(encoded) || ("encoded_" + raw).equals(encoded));
        });
    }

    @Test
    @DisplayName("XL-01: Dang nhap thanh cong voi thong tin hop le")
    void testAuthenticate_Success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("thopham");
        user.setPassword("123456");
        user.setFullName("Nguyen Phuoc Tho");
        user.setRole("CUSTOMER");
        user.setBranchCode("CN01");
        user.setStatus("Hoat dong");

        when(userRepository.findByUsername("thopham")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyString(), anyString())).thenReturn("mock-jwt-token");

        Map<String, Object> result = userService.authenticate("thopham", "123456");

        assertNotNull(result);
        assertEquals("mock-jwt-token", result.get("token"));
        assertEquals("thopham", result.get("username"));
        assertEquals("CUSTOMER", result.get("role"));
        assertEquals("CN01", result.get("branchCode"));
        verify(userRepository, times(1)).findByUsername("thopham");
    }

    @Test
    @DisplayName("XL-01: Dang nhap that bai khi sai tai khoan hoac mat khau")
    void testAuthenticate_WrongCredentials() {
        when(userRepository.findByUsername("thopham")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.authenticate("thopham", "wrongpass");
        });

        assertEquals("Tài khoản hoặc mật khẩu không chính xác!", ex.getMessage());
    }

    @Test
    @DisplayName("XL-01: Dang nhap that bai khi tai khoan bi khoa")
    void testAuthenticate_AccountLocked() {
        User lockedUser = new User();
        lockedUser.setUsername("locked_user");
        lockedUser.setPassword("123456");
        lockedUser.setStatus("Khóa");

        when(userRepository.findByUsername("locked_user")).thenReturn(Optional.of(lockedUser));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            userService.authenticate("locked_user", "123456");
        });

        assertTrue(ex.getMessage().contains("Tài khoản đã bị tạm khóa"));
    }

    @Test
    @DisplayName("XL-01: Dang nhap bat loi khi de trong thong tin")
    void testAuthenticate_NullCredentials() {
        assertThrows(IllegalArgumentException.class, () -> userService.authenticate(null, "123"));
        assertThrows(IllegalArgumentException.class, () -> userService.authenticate("user", null));
    }

    @Test
    @DisplayName("UC-02: Dang ky hoi vien moi thanh cong")
    void testRegisterCustomer_Success() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0909123456")).thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setUsername("newuser");
        savedUser.setFullName("Tran Phuc Bao");
        savedUser.setPhone("0909123456");
        savedUser.setRole("CUSTOMER");
        savedUser.setBranchCode("CN01");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(customerRepository.save(any(Customer.class))).thenReturn(new Customer());
        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyString(), anyString())).thenReturn("new-jwt-token");

        Map<String, Object> result = userService.registerCustomer("newuser", "pass123", "Tran Phuc Bao", "0909123456", "bao@gmail.com");

        assertNotNull(result);
        assertEquals("new-jwt-token", result.get("token"));
        assertEquals("newuser", result.get("username"));
        assertEquals("CUSTOMER", result.get("role"));
        verify(userRepository, times(1)).save(any(User.class));
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("UC-02: Dang ky that bai khi ten dang nhap da ton tai")
    void testRegisterCustomer_DuplicateUsername() {
        when(userRepository.findByUsername("existing_user")).thenReturn(Optional.of(new User()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerCustomer("existing_user", "pass123", "Nguyen Van A", "0901111222", "a@gmail.com");
        });

        assertEquals("Tên đăng nhập đã tồn tại trong hệ thống!", ex.getMessage());
    }

    @Test
    @DisplayName("UC-02: Dang ky that bai khi so dien thoai da duoc su dung")
    void testRegisterCustomer_DuplicatePhone() {
        when(userRepository.findByUsername("unique_user")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0909999888")).thenReturn(Optional.of(new User()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerCustomer("unique_user", "pass123", "Nguyen Van B", "0909999888", "b@gmail.com");
        });

        assertEquals("Số điện thoại đã được đăng ký bởi tài khoản khác!", ex.getMessage());
    }
}
