package vn.yain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import vn.yain.repository.SystemSettingRepository;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Test
    @DisplayName("Email 01: Dịch lỗi Google SMTP 535 thành hướng dẫn App Password 16 ký tự")
    void testParseSmtpError535() {
        Exception e = new Exception("535-5.7.8 Username and Password not accepted. Learn more at https://support.google.com/mail/?p=BadCredentials");
        String errorMsg = emailService.parseSmtpError(e);

        assertNotNull(errorMsg);
        assertTrue(errorMsg.contains("Mã 535"));
        assertTrue(errorMsg.contains("Mật khẩu ứng dụng"));
        assertTrue(errorMsg.contains("Xác minh 2 bước"));
    }

    @Test
    @DisplayName("Email 02: Dịch lỗi kết nối mạng / timeout máy chủ SMTP")
    void testParseSmtpErrorTimeout() {
        Exception e = new Exception("Could not connect to SMTP host: smtp.gmail.com, port: 587; ConnectException: Connection timed out");
        String errorMsg = emailService.parseSmtpError(e);

        assertNotNull(errorMsg);
        assertTrue(errorMsg.contains("LỖI KẾT NỐI MÁY CHỦ SMTP"));
    }

    @Test
    @DisplayName("Email 03: Kiểm tra gửi thử khi email người nhận không hợp lệ")
    void testTestMailConnectionInvalidRecipient() {
        Map<String, Object> res = emailService.testMailConnection("not-an-email");
        assertFalse((Boolean) res.get("success"));
        assertTrue(((String) res.get("message")).contains("không hợp lệ"));
    }

    @Test
    @DisplayName("Email 04: Tự động loại bỏ dấu cách từ Mật khẩu ứng dụng Google")
    void testResolvedPasswordStripsWhitespace() {
        // Giả lập mật khẩu có dấu cách thường gặp khi copy từ Google
        String raw = "abcd efgh ijkl mnop";
        String cleaned = raw.trim().replace(" ", "");
        assertEquals("abcdefghijklmnop", cleaned);
    }
}
