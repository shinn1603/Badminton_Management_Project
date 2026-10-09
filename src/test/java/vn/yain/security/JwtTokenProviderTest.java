package vn.yain.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho JwtTokenProvider (Bao phu co che xac thuc JWT theo Role - XL-01)
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
    }

    @Test
    @DisplayName("Sinh va xac thuc JWT token hop le cho Nhan vien Quan ly")
    void testGenerateAndValidateToken_Success() {
        String token = jwtTokenProvider.generateToken("quanly01", "ROLE_MANAGER", "Nguyen Van Quan Ly", "CN01");

        assertNotNull(token);
        assertTrue(token.length() > 20);

        // Validate token
        assertTrue(jwtTokenProvider.validateToken(token));

        // Extract claims
        assertEquals("quanly01", jwtTokenProvider.getUsernameFromToken(token));
        assertEquals("ROLE_MANAGER", jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("Sinh va xac thuc JWT token hop le cho Khach hang")
    void testGenerateToken_CustomerRole() {
        String token = jwtTokenProvider.generateToken("khachhang99", "ROLE_CUSTOMER", "Tran Bieu Huong", "CN02");

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("khachhang99", jwtTokenProvider.getUsernameFromToken(token));
        assertEquals("ROLE_CUSTOMER", jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("Validate token gia mao hoac bi loi cau truc")
    void testValidateToken_Invalid() {
        assertFalse(jwtTokenProvider.validateToken("invalid.jwt.token.string"));
        assertFalse(jwtTokenProvider.validateToken(null));
        assertFalse(jwtTokenProvider.validateToken(""));
    }
}
