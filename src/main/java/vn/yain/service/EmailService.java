package vn.yain.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.yain.entity.*;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import vn.yain.repository.BranchRepository;
import vn.yain.repository.CourtRepository;
import vn.yain.repository.CustomerRepository;
import vn.yain.repository.SystemSettingRepository;
import vn.yain.repository.UserRepository;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String springMailHost;

    @Value("${spring.mail.port:587}")
    private int springMailPort;

    @Value("${spring.mail.username:tho8189@gmail.com}")
    private String springMailUsername;

    @Value("${spring.mail.password:}")
    private String springMailPassword;

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    public String getResolvedUsername() {
        String dbUser = getSetting("CFG_MAIL_USERNAME", null);
        if (dbUser != null && !dbUser.isBlank()) {
            return dbUser.trim();
        }
        if (springMailUsername != null && !springMailUsername.isBlank()) {
            return springMailUsername.trim();
        }
        return "tho8189@gmail.com";
    }

    public String getResolvedPassword() {
        String dbPass = getSetting("CFG_MAIL_PASSWORD", null);
        if (dbPass != null && !dbPass.isBlank()) {
            return dbPass.trim().replace(" ", "");
        }
        if (springMailPassword != null && !springMailPassword.isBlank()) {
            return springMailPassword.trim().replace(" ", "");
        }
        return "";
    }

    public String getResolvedHost() {
        String dbHost = getSetting("CFG_MAIL_HOST", null);
        if (dbHost != null && !dbHost.isBlank()) {
            return dbHost.trim();
        }
        if (springMailHost != null && !springMailHost.isBlank()) {
            return springMailHost.trim();
        }
        return "smtp.gmail.com";
    }

    public int getResolvedPort() {
        String dbPort = getSetting("CFG_MAIL_PORT", null);
        if (dbPort != null && !dbPort.isBlank()) {
            try {
                return Integer.parseInt(dbPort.trim());
            } catch (Exception ignored) {}
        }
        return springMailPort > 0 ? springMailPort : 587;
    }

    public String getResolvedFromName() {
        String dbName = getSetting("CFG_MAIL_FROM_NAME", null);
        if (dbName != null && !dbName.isBlank()) {
            return dbName.trim();
        }
        return "UTE Sport Badminton Arena";
    }

    private String getSetting(String code, String fallback) {
        if (systemSettingRepository != null) {
            try {
                return systemSettingRepository.findBySettingCode(code)
                        .map(SystemSetting::getSettingValue)
                        .filter(v -> v != null && !v.isBlank())
                        .orElse(fallback);
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    /**
     * Khởi tạo đối tượng JavaMailSender linh hoạt từ cấu hình CSDL hoặc application.properties
     */
    public JavaMailSender getDynamicMailSender() {
        return getDynamicMailSender(getResolvedHost(), getResolvedPort(), getResolvedUsername(), getResolvedPassword());
    }

    public JavaMailSender getDynamicMailSender(int port) {
        return getDynamicMailSender(getResolvedHost(), port, getResolvedUsername(), getResolvedPassword());
    }

    public JavaMailSender getDynamicMailSender(String host, int port, String username, String password) {
        if (password == null || password.isBlank() || username == null || username.isBlank()) {
            return null;
        }

        if (host == null || host.isBlank()) {
            host = "smtp.gmail.com";
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host.trim());
        sender.setPort(port);
        sender.setUsername(username.trim());
        sender.setPassword(password.trim().replace(" ", ""));
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.smtp.ssl.trust", "*");

        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        } else {
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        }

        return sender;
    }

    /**
     * Lưu hoặc cập nhật thông số hệ thống vào CSDL
     */
    public void saveOrUpdateSetting(String code, String value) {
        if (systemSettingRepository == null || code == null || value == null) return;
        try {
            systemSettingRepository.findBySettingCode(code).ifPresentOrElse(s -> {
                s.setSettingValue(value);
                systemSettingRepository.save(s);
            }, () -> {
                SystemSetting s = new SystemSetting();
                s.setSettingCode(code);
                s.setSettingName(code);
                s.setSettingValue(value);
                systemSettingRepository.save(s);
            });
        } catch (Exception e) {
            log.warn("[EMAIL SERVICE] Không thể lưu cấu hình {}: {}", code, e.getMessage());
        }
    }

    /**
     * Phân tích và dịch mã lỗi SMTP từ Google hoặc nhà cung cấp thư thành thông báo tiếng Việt dễ hiểu
     */
    public String parseSmtpError(Exception e) {
        return parseSmtpError(e, getResolvedPort(), getResolvedHost());
    }

    public String parseSmtpError(Exception e, int port, String host) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        String full = e.toString() + " " + msg;
        if (e.getCause() != null) {
            full += " " + e.getCause().toString() + " " + e.getCause().getMessage();
        }

        if (full.contains("535") || full.contains("AuthenticationFailedException") || full.contains("Username and Password not accepted") || full.contains("BadCredentials")) {
            return "LỖI XÁC THỰC TÀI KHOẢN GOOGLE (Mã 535 - Username and Password not accepted):\n"
                 + "• Máy chủ SMTP Google (" + host + ":" + port + ") đã KẾT NỐI THÀNH CÔNG, nhưng Google từ chối mật khẩu xác thực!\n"
                 + "• Google KHÔNG cho phép dùng mật khẩu đăng nhập Gmail thông thường.\n"
                 + "• HƯỚNG DẪN KHẮC PHỤC (1 PHÚT):\n"
                 + "  1. Bật 'Xác minh 2 bước' (2-Step Verification) trên tài khoản Google: " + getResolvedUsername() + "\n"
                 + "  2. Mở trình duyệt và truy cập: https://myaccount.google.com/apppasswords\n"
                 + "  3. Tạo một 'Mật khẩu ứng dụng' mới (gồm 16 ký tự, ví dụ: 'abcd efgh ijkl mnop').\n"
                 + "  4. Sao chép và dán 16 ký tự này vào ô 'Mật khẩu ứng dụng Google (App Password)' rồi bấm 'Lưu thay đổi' và gửi lại.\n"
                 + "  (LƯU Ý: Nếu bạn vừa đổi mật khẩu chính của Gmail gần đây, Google đã tự động hủy tất cả Mật khẩu ứng dụng cũ. Bạn chỉ cần tạo lại mã 16 ký tự mới!)";
        }
        if (full.contains("Could not connect to SMTP host") || full.contains("ConnectException") || full.contains("timed out") || full.contains("SocketTimeoutException") || full.contains("MailConnectException")) {
            int altPort = port == 587 ? 465 : 587;
            return "LỖI KẾT NỐI MÁY CHỦ SMTP: Không thể kết nối đến máy chủ SMTP (" + host + ":" + port + ").\n"
                 + "• NGUYÊN NHÂN: Mạng Wi-Fi / Nhà mạng Internet hoặc Tường lửa trên máy tính đang chặn cổng SMTP " + port + ".\n"
                 + "• CÁCH KHẮC PHỤC NHANH:\n"
                 + "  1. Chuyển 'Cổng SMTP' từ " + port + " sang " + altPort + " (" + (altPort == 465 ? "SSL" : "TLS") + ") rồi bấm 'Lưu thay đổi' và gửi lại.\n"
                 + "  2. Thử phát Wi-Fi từ điện thoại (4G/5G) để kiểm tra nếu mạng Wi-Fi hiện tại chặn cổng gửi mail.\n"
                 + "  3. Tắt tạm tính năng 'Mail Shield' của phần mềm diệt virus (nếu có).";
        }
        if (full.contains("AddressException") || full.contains("Illegal address") || full.contains("From address")) {
            return "LỖI ĐỊA CHỈ EMAIL: Địa chỉ email người gửi hoặc người nhận không hợp lệ. Vui lòng kiểm tra định dạng email (vd: tho8189@gmail.com).";
        }
        return "Lỗi gửi email qua SMTP (" + host + ":" + port + "): " + msg;
    }

    /**
     * Kiểm tra kết nối SMTP thực tế và gửi thư thử nghiệm
     */
    public Map<String, Object> testMailConnection(String targetEmail) {
        return testMailConnection(targetEmail, null, null, null, null, null);
    }

    public Map<String, Object> testMailConnection(String targetEmail, String overrideUsername, String overridePassword, String overridePort, String overrideHost, String overrideFromName) {
        String username = (overrideUsername != null && !overrideUsername.isBlank()) ? overrideUsername.trim() : getResolvedUsername();
        String password = (overridePassword != null && !overridePassword.isBlank()) ? overridePassword.trim().replace(" ", "") : getResolvedPassword();
        String host = (overrideHost != null && !overrideHost.isBlank()) ? overrideHost.trim() : getResolvedHost();
        String fromName = (overrideFromName != null && !overrideFromName.isBlank()) ? overrideFromName.trim() : getResolvedFromName();

        int port = getResolvedPort();
        if (overridePort != null && !overridePort.isBlank()) {
            try {
                port = Integer.parseInt(overridePort.trim());
            } catch (Exception ignored) {}
        }

        String recipient = (targetEmail != null && !targetEmail.isBlank()) ? targetEmail.trim() : username;
        if (recipient == null || recipient.isBlank() || !recipient.contains("@")) {
            return Map.of(
                "success", false,
                "message", "Địa chỉ email người nhận thử nghiệm không hợp lệ: " + recipient
            );
        }

        if (password == null || password.isBlank()) {
            return Map.of(
                "success", false,
                "message", "Chưa nhập Mật khẩu ứng dụng Google (App Password)! Vui lòng lấy mã 16 ký tự từ https://myaccount.google.com/apppasswords và nhập vào ô cấu hình."
            );
        }

        JavaMailSender sender = getDynamicMailSender(host, port, username, password);
        if (sender == null) {
            return Map.of(
                "success", false,
                "message", "Chưa thể khởi tạo kết nối SMTP. Vui lòng kiểm tra lại địa chỉ Gmail người gửi và Mật khẩu ứng dụng."
            );
        }

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(username, fromName);
            helper.setTo(recipient);
            helper.setSubject("[UTE Sport] Kiểm tra kết nối dịch vụ Email SMTP thành công!");

            String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b;">
                    <div style="max-width: 540px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.06); border: 1px solid #e2e8f0;">
                        <div style="background: linear-gradient(135deg, #059669 0%%, #10b981 100%%); padding: 24px; text-align: center; color: #ffffff;">
                            <h2 style="margin: 0; font-size: 20px; font-weight: 800;">UTE SPORT BADMINTON ARENA</h2>
                            <p style="margin: 6px 0 0; font-size: 13px; opacity: 0.95;">KIỂM TRA KẾT NỐI HỆ THỐNG GỬI EMAIL TỰ ĐỘNG</p>
                        </div>
                        <div style="padding: 24px;">
                            <div style="background: #ecfdf5; border-left: 4px solid #10b981; padding: 14px 16px; border-radius: 6px; margin-bottom: 18px;">
                                <strong style="color: #065f46; font-size: 15px;">Kết nối máy chủ SMTP Google thành công!</strong>
                                <p style="margin: 6px 0 0; font-size: 13px; color: #047857; line-height: 1.5;">
                                    Hệ thống đã xác thực thành công tài khoản gửi thư với máy chủ <strong>%s:%d</strong>.
                                </p>
                            </div>
                            <table style="width: 100%%; font-size: 13px; border-collapse: collapse;">
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; width: 36%%;">Email người gửi:</td>
                                    <td style="padding: 8px 0; font-weight: 600; color: #0f172a;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b;">Tên hiển thị:</td>
                                    <td style="padding: 8px 0; font-weight: 600; color: #0f172a;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b;">Email người nhận:</td>
                                    <td style="padding: 8px 0; font-weight: 600; color: #2563eb;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b;">Thời gian kiểm tra:</td>
                                    <td style="padding: 8px 0; color: #334155;">%s</td>
                                </tr>
                            </table>
                            <div style="margin-top: 18px; padding-top: 14px; border-top: 1px solid #f1f5f9; font-size: 12px; color: #64748b; line-height: 1.5;">
                                Giờ đây, các email đặt sân (E-Ticket), mã OTP khôi phục mật khẩu và thông báo giải đấu sẽ được gửi trực tiếp từ tài khoản này.
                            </div>
                        </div>
                        <div style="background: #f8fafc; padding: 12px 24px; text-align: center; border-top: 1px solid #e2e8f0; font-size: 11.5px; color: #94a3b8;">
                            Hệ Thống Quản Lý Thể Thao UTE Sport Badminton Club | 01 Võ Văn Ngân, Thủ Đức
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(host, port, username, fromName, recipient, java.time.LocalDateTime.now().format(TIME_FMT));

            helper.setText(html, true);
            sender.send(message);

            // Tự động lưu cấu hình hợp lệ vào CSDL
            saveOrUpdateSetting("CFG_MAIL_USERNAME", username);
            saveOrUpdateSetting("CFG_MAIL_PASSWORD", password);
            saveOrUpdateSetting("CFG_MAIL_PORT", String.valueOf(port));
            saveOrUpdateSetting("CFG_MAIL_HOST", host);
            saveOrUpdateSetting("CFG_MAIL_FROM_NAME", fromName);

            log.info("[EMAIL TEST SUCCESS] Đã gửi thư kiểm tra thành công từ [{}] tới [{}] qua cổng {}", username, recipient, port);
            return Map.of(
                "success", true,
                "message", "Kết nối SMTP thành công! Email thử nghiệm đã được gửi tới " + recipient + " qua cổng " + port + "."
            );
        } catch (Exception e) {
            log.error("[EMAIL TEST ERROR] Gửi thử nghiệm thất bại trên cổng {}: {}", port, e.getMessage(), e);

            String fullErr = e.toString() + " " + (e.getMessage() != null ? e.getMessage() : "");
            if (e.getCause() != null) {
                fullErr += " " + e.getCause().toString() + " " + e.getCause().getMessage();
            }

            boolean isAuthErr = fullErr.contains("535") || fullErr.contains("AuthenticationFailedException") || fullErr.contains("Username and Password not accepted") || fullErr.contains("BadCredentials");
            boolean isConnErr = fullErr.contains("Could not connect") || fullErr.contains("ConnectException") || fullErr.contains("timed out") || fullErr.contains("SocketTimeoutException") || fullErr.contains("MailConnectException");

            // Nếu chỉ lỗi kết nối mạng/tường lửa mà không phải sai mật khẩu, tự động thử cổng thay thế (587 <-> 465)
            if (isConnErr && !isAuthErr) {
                int altPort = (port == 587) ? 465 : 587;
                try {
                    log.info("[EMAIL TEST] Cổng {} bị lỗi kết nối, đang tự động thử kết nối dự phòng qua cổng thay thế: {}...", port, altPort);
                    JavaMailSender altSender = getDynamicMailSender(host, altPort, username, password);
                    if (altSender != null) {
                        MimeMessage altMsg = altSender.createMimeMessage();
                        MimeMessageHelper altHelper = new MimeMessageHelper(altMsg, true, "UTF-8");
                        altHelper.setFrom(username, fromName);
                        altHelper.setTo(recipient);
                        altHelper.setSubject("[UTE Sport] Kiểm tra kết nối dịch vụ Email SMTP thành công (qua cổng " + altPort + ")!");
                        String altHtml = "<div style=\"font-family:sans-serif;padding:20px;\"><h2>UTE SPORT</h2><p>Kiểm tra kết nối email SMTP thành công qua cổng " + altPort + ".</p></div>";
                        altHelper.setText(altHtml, true);
                        altSender.send(altMsg);

                        // Lưu cổng thành công vào database
                        saveOrUpdateSetting("CFG_MAIL_USERNAME", username);
                        saveOrUpdateSetting("CFG_MAIL_PASSWORD", password);
                        saveOrUpdateSetting("CFG_MAIL_PORT", String.valueOf(altPort));
                        saveOrUpdateSetting("CFG_MAIL_HOST", host);
                        saveOrUpdateSetting("CFG_MAIL_FROM_NAME", fromName);

                        return Map.of(
                            "success", true,
                            "message", "Cổng " + port + " bị nhà mạng/tường lửa chặn kết nối, nhưng hệ thống đã TỰ ĐỘNG CHUYỂN SANG CỔNG " + altPort + " (" + (altPort == 465 ? "SSL" : "TLS") + ") VÀ GỬI THÀNH CÔNG! Đã tự động lưu cổng " + altPort + " làm mặc định."
                        );
                    }
                } catch (Exception altEx) {
                    log.warn("[EMAIL TEST] Cổng thay thế {} cũng thất bại: {}", altPort, altEx.getMessage());
                }
            }

            String parsedError = parseSmtpError(e, port, host);
            return Map.of(
                "success", false,
                "message", parsedError,
                "rawError", e.getMessage() != null ? e.getMessage() : e.toString()
            );
        }
    }

    /**
     * Gửi email HTML bất đồng bộ (Non-blocking)
     */
    public CompletableFuture<Boolean> sendHtmlEmailAsync(String to, String subject, String htmlBody) {
        return CompletableFuture.supplyAsync(() -> sendHtmlEmail(to, subject, htmlBody));
    }

    /**
     * Gửi email HTML đồng bộ, tự động fallback sang chế độ mô phỏng log nếu SMTP chưa cấu hình
     */
    public boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        if (to == null || to.trim().isEmpty() || !to.contains("@")) {
            log.warn("[EMAIL SERVICE] Địa chỉ email nhận không hợp lệ: {}", to);
            return false;
        }

        String recipient = to.trim();
        JavaMailSender sender = getDynamicMailSender();

        if (sender != null) {
            try {
                MimeMessage mimeMessage = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                String from = getResolvedUsername();
                String fromName = getResolvedFromName();
                helper.setFrom(from, fromName);
                helper.setTo(recipient);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);

                sender.send(mimeMessage);
                log.info("[EMAIL SERVICE] Đã gửi email thành công qua SMTP tới: {} | Tiêu đề: {}", recipient, subject);
                return true;
            } catch (Exception e) {
                log.error("[EMAIL SERVICE] Lỗi khi gửi qua SMTP (chuyển sang mô phỏng log): {} | Chi tiết: {}", e.getMessage(), parseSmtpError(e));
            }
        } else {
            log.warn("[EMAIL SERVICE] Chưa cấu hình SMTP (username/password trống), chuyển sang chế độ mô phỏng log.");
        }

        // Chế độ mô phỏng (Development / Offline Logger)
        log.info("================================================================================");
        log.info("[EMAIL SIMULATION - UTE SPORT NOTIFICATION]");
        log.info("From: {} ({})", getResolvedUsername(), getResolvedFromName());
        log.info("To: {}", recipient);
        log.info("Subject: {}", subject);
        log.info("Timestamp: {}", java.time.LocalDateTime.now().format(TIME_FMT));
        log.info("Status: SIMULATED_DELIVERED_SUCCESSFULLY");
        log.info("================================================================================");
        return true;
    }

    // =========================================================================
    // 1. GỬI MÃ OTP / THÔNG BÁO QUÊN MẬT KHẨU
    // =========================================================================

    public void sendPasswordResetOtp(String toEmail, String fullName, String otpCode) {
        String safeName = (fullName != null && !fullName.isBlank()) ? fullName : "Quý khách";
        String subject = "[UTE Sport] Mã xác nhận khôi phục mật khẩu tài khoản: " + otpCode;

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px; color: #1e293b;">
                <div style="max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.08); border: 1px solid #e2e8f0;">
                    <div style="background: linear-gradient(135deg, #0f172a 0%%, #1e293b 100%%); padding: 28px 24px; text-align: center; border-bottom: 3px solid #10b981;">
                        <h1 style="color: #ffffff; margin: 0; font-size: 22px; font-weight: 800; letter-spacing: 0.5px;">UTE SPORT BADMINTON ARENA</h1>
                        <p style="color: #94a3b8; margin: 6px 0 0; font-size: 13px;">Hệ Thống Quản Lý Chuỗi Trung Tâm Thể Thao Cao Cấp</p>
                    </div>
                    <div style="padding: 28px 24px;">
                        <h2 style="color: #0f172a; font-size: 18px; margin-top: 0;">Yêu Cầu Đặt Lại Mật Khẩu</h2>
                        <p style="font-size: 14px; line-height: 1.6; color: #334155;">
                            Xin chào <strong>%s</strong>,<br>
                            Hệ thống UTE Sport nhận được yêu cầu khôi phục mật khẩu cho tài khoản đăng ký với email này. Vui lòng sử dụng mã xác nhận bên dưới để hoàn tất:
                        </p>
                        <div style="margin: 24px 0; text-align: center;">
                            <div style="display: inline-block; background: #f8fafc; border: 2px dashed #10b981; border-radius: 12px; padding: 14px 32px; font-size: 32px; font-weight: 800; letter-spacing: 6px; color: #047857;">
                                %s
                            </div>
                            <p style="font-size: 12px; color: #64748b; margin: 8px 0 0;">Mã có hiệu lực trong vòng <strong>10 phút</strong>.</p>
                        </div>
                        <p style="font-size: 13px; line-height: 1.6; color: #64748b; background: #fef2f2; border-left: 4px solid #ef4444; padding: 10px 14px; border-radius: 4px;">
                            <strong>Cảnh báo bảo mật:</strong> Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc liên hệ ngay hotline <strong>0903.123.456</strong> để khóa tài khoản khẩn cấp.
                        </p>
                    </div>
                    <div style="background: #f8fafc; padding: 16px 24px; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b;">
                        © 2026 UTE Sport Badminton Club | Chi nhánh chính: 01 Võ Văn Ngân, TP. Thủ Đức<br>
                        Hotline hỗ trợ: 0903.123.456 - Website: utesport.vn
                    </div>
                </div>
            </body>
            </html>
            """.formatted(safeName, otpCode);

        sendHtmlEmailAsync(toEmail, subject, html);
    }

    public void sendViewPasswordOtp(String toEmail, String fullName, String otpCode) {
        String safeName = (fullName != null && !fullName.isBlank()) ? fullName : "Quý khách";
        String subject = "[UTE Sport] Mã xác thực OTP để xem mật khẩu tài khoản: " + otpCode;

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px; color: #1e293b;">
                <div style="max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.08); border: 1px solid #e2e8f0;">
                    <div style="background: linear-gradient(135deg, #0f172a 0%%, #1e293b 100%%); padding: 28px 24px; text-align: center; border-bottom: 3px solid #10b981;">
                        <h1 style="color: #ffffff; margin: 0; font-size: 22px; font-weight: 800; letter-spacing: 0.5px;">UTE SPORT BADMINTON ARENA</h1>
                        <p style="color: #94a3b8; margin: 6px 0 0; font-size: 13px;">Hệ Thống Quản Lý Chuỗi Trung Tâm Thể Thao Cao Cấp</p>
                    </div>
                    <div style="padding: 28px 24px;">
                        <h2 style="color: #0f172a; font-size: 18px; margin-top: 0;">Xác Thực Bảo Mật Xem Mật Khẩu</h2>
                        <p style="font-size: 14px; line-height: 1.6; color: #334155;">
                            Xin chào <strong>%s</strong>,<br>
                            Hệ thống nhận được yêu cầu xem lại mật khẩu tài khoản tại mục Thông Tin Cá Nhân. Vui lòng sử dụng mã xác nhận bên dưới để hoàn tất:
                        </p>
                        <div style="margin: 24px 0; text-align: center;">
                            <div style="display: inline-block; background: #f8fafc; border: 2px dashed #10b981; border-radius: 12px; padding: 14px 32px; font-size: 32px; font-weight: 800; letter-spacing: 6px; color: #047857;">
                                %s
                            </div>
                            <p style="font-size: 12px; color: #64748b; margin: 8px 0 0;">Mã có hiệu lực trong vòng <strong>5 phút</strong>.</p>
                        </div>
                        <p style="font-size: 13px; line-height: 1.6; color: #64748b; background: #fef2f2; border-left: 4px solid #ef4444; padding: 10px 14px; border-radius: 4px;">
                            <strong>Cảnh báo bảo mật:</strong> Tuyệt đối không chia sẻ mã OTP này cho bất kỳ ai để bảo vệ quyền riêng tư và an toàn tài khoản của bạn.
                        </p>
                    </div>
                    <div style="background: #f8fafc; padding: 16px 24px; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b;">
                        © 2026 UTE Sport Badminton Club | Chi nhánh chính: 01 Võ Văn Ngân, TP. Thủ Đức<br>
                        Hotline hỗ trợ: 0903.123.456 - Website: utesport.vn
                    </div>
                </div>
            </body>
            </html>
            """.formatted(safeName, otpCode);

        sendHtmlEmailAsync(toEmail, subject, html);
    }

    public void sendPasswordChangedNotification(String toEmail, String fullName, String newPassword) {
        String safeName = (fullName != null && !fullName.isBlank()) ? fullName : "Quý khách";
        String subject = "[UTE Sport] Thông báo: Mật khẩu của bạn đã được thay đổi thành công";

        String passNote = (newPassword != null && !newPassword.isBlank())
                ? "<p style=\"background: #ecfdf5; border: 1px solid #10b981; padding: 10px 14px; border-radius: 8px; font-size: 13px; color: #065f46;\">Mật khẩu mới của bạn là: <strong style=\"font-size: 15px;\">" + newPassword + "</strong></p>"
                : "";

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px; color: #1e293b;">
                <div style="max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.08); border: 1px solid #e2e8f0;">
                    <div style="background: #0f172a; padding: 24px; text-align: center; border-bottom: 3px solid #10b981;">
                        <h1 style="color: #ffffff; margin: 0; font-size: 20px; font-weight: 800;">UTE SPORT BADMINTON ARENA</h1>
                    </div>
                    <div style="padding: 26px 24px;">
                        <h2 style="color: #047857; font-size: 17px; margin-top: 0;">Cập Nhật Mật Khẩu Thành Công</h2>
                        <p style="font-size: 14px; line-height: 1.6; color: #334155;">
                            Xin chào <strong>%s</strong>,<br>
                            Mật khẩu tài khoản của bạn tại hệ thống UTE Sport đã được thay đổi thành công lúc <strong>%s</strong>.
                        </p>
                        %s
                        <p style="font-size: 13px; color: #475569;">
                            Bây giờ bạn có thể đăng nhập vào ứng dụng và đặt sân bình thường. Nếu bạn không thực hiện thay đổi này, hãy liên hệ ngay với Ban quản lý.
                        </p>
                    </div>
                    <div style="background: #f8fafc; padding: 14px 24px; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b;">
                        © 2026 UTE Sport | Hotline hỗ trợ 24/7: 0903.123.456
                    </div>
                </div>
            </body>
            </html>
            """.formatted(safeName, java.time.LocalDateTime.now().format(TIME_FMT), passNote);

        sendHtmlEmailAsync(toEmail, subject, html);
    }

    // =========================================================================
    // 2. GỬI PHIẾU XÁC NHẬN ĐẶT SÂN & VÉ ĐIỆN TỬ (E-TICKET BM-KH02)
    // =========================================================================

    public void sendBookingConfirmation(Booking booking) {
        if (booking == null) return;

        // Tìm email người nhận
        String toEmail = booking.getCustomerEmail();
        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            if (booking.getCustomerCode() != null) {
                Customer c = customerRepository.findByCustomerCode(booking.getCustomerCode()).orElse(null);
                if (c != null && c.getEmail() != null && c.getEmail().contains("@")) {
                    toEmail = c.getEmail();
                }
            }
            if (toEmail == null || toEmail.isBlank()) {
                if (booking.getCustomerPhone() != null) {
                    User u = userRepository.findByPhone(booking.getCustomerPhone()).orElse(null);
                    if (u != null && u.getEmail() != null && u.getEmail().contains("@")) {
                        toEmail = u.getEmail();
                    }
                }
            }
        }

        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            log.info("[EMAIL SERVICE] Đơn đặt sân [{}] không có email khách hàng, bỏ qua gửi mail.", booking.getBookingCode());
            return;
        }

        // Thông tin sân và chi nhánh
        Court court = courtRepository.findByCourtCode(booking.getCourtCode()).orElse(null);
        String courtName = court != null ? court.getCourtName() : ("Sân " + booking.getCourtCode());
        String courtType = court != null ? court.getCourtType() : "Tiêu chuẩn";

        Branch branch = branchRepository.findByBranchCode(booking.getBranchCode()).orElse(null);
        String branchName = branch != null ? branch.getBranchName() : ("Chi nhánh " + booking.getBranchCode());
        String branchAddr = branch != null ? branch.getAddress() : "Khu liên hợp thể thao UTE";

        BigDecimal total = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal deposit = booking.getDepositAmount() != null ? booking.getDepositAmount() : BigDecimal.ZERO;
        BigDecimal remaining = total.subtract(deposit);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

        String dateStr = booking.getBookingDate() != null ? booking.getBookingDate().format(DATE_FMT) : "Hôm nay";
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=" + booking.getBookingCode();

        String subject = "[UTE Sport] Vé điện tử E-Ticket xác nhận đặt sân: " + booking.getBookingCode();

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px; color: #1e293b;">
                <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.08); border: 1px solid #e2e8f0;">
                    
                    <!-- HEADER -->
                    <div style="background: linear-gradient(135deg, #0f172a 0%%, #1e293b 100%%); padding: 24px; text-align: center; border-bottom: 3px solid #10b981;">
                        <h1 style="color: #ffffff; margin: 0; font-size: 21px; font-weight: 800; letter-spacing: 0.5px;">UTE SPORT BADMINTON ARENA</h1>
                        <p style="color: #10b981; font-weight: 700; margin: 6px 0 0; font-size: 14px; text-transform: uppercase; letter-spacing: 1px;">
                            PHIẾU XÁC NHẬN ĐẶT SÂN & VÉ ĐIỆN TỬ (E-TICKET)
                        </p>
                    </div>

                    <!-- BODY -->
                    <div style="padding: 24px;">
                        
                        <!-- BOOKING CODE HIGHLIGHT -->
                        <div style="background: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 12px; padding: 14px 18px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center;">
                            <div>
                                <span style="font-size: 12px; color: #047857; font-weight: 600; text-transform: uppercase;">MÃ ĐẶT SÂN / CHECK-IN</span><br>
                                <strong style="font-size: 22px; color: #065f46; letter-spacing: 1px;">%s</strong>
                            </div>
                            <div style="text-align: right;">
                                <span style="display: inline-block; background: #10b981; color: #ffffff; font-size: 12px; font-weight: 700; padding: 4px 10px; border-radius: 20px;">
                                    %s
                                </span>
                            </div>
                        </div>

                        <!-- DETAILS TABLE -->
                        <table style="width: 100%%; border-collapse: collapse; font-size: 13.5px; margin-bottom: 20px;">
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 9px 0; color: #64748b; width: 38%%;">Khách hàng:</td>
                                <td style="padding: 9px 0; font-weight: 600; color: #0f172a;">%s (%s)</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 9px 0; color: #64748b;">Chi nhánh:</td>
                                <td style="padding: 9px 0; font-weight: 600; color: #0f172a;">%s</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 9px 0; color: #64748b;">Địa chỉ cơ sở:</td>
                                <td style="padding: 9px 0; color: #334155;">%s</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 9px 0; color: #64748b;">Sân thi đấu:</td>
                                <td style="padding: 9px 0; font-weight: 700; color: #047857;">%s <span style="font-weight: normal; font-size: 12px; color: #64748b;">(%s)</span></td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 9px 0; color: #64748b;">Ngày chơi:</td>
                                <td style="padding: 9px 0; font-weight: 700; color: #0f172a;">%s</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 9px 0; color: #64748b;">Khung giờ thi đấu:</td>
                                <td style="padding: 9px 0; font-weight: 700; color: #2563eb;">%s</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 9px 0; color: #64748b;">Tổng giá trị đơn:</td>
                                <td style="padding: 9px 0; font-weight: 600; color: #0f172a;">%,d đ</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9; background: #f8fafc;">
                                <td style="padding: 9px 8px; color: #047857; font-weight: 600;">Tiền cọc đã thanh toán (30%%):</td>
                                <td style="padding: 9px 8px; font-weight: 800; color: #047857;">%,d đ <span style="font-size: 11px; font-weight: normal;">(VietQR Pro)</span></td>
                            </tr>
                            <tr style="background: #fffbeb;">
                                <td style="padding: 9px 8px; color: #b45309; font-weight: 600;">Còn lại thanh toán tại quầy (70%%):</td>
                                <td style="padding: 9px 8px; font-weight: 800; color: #b45309;">%,d đ</td>
                            </tr>
                        </table>

                        <!-- QR CODE SECTION -->
                        <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 18px; text-align: center; margin-bottom: 20px;">
                            <p style="margin: 0 0 10px; font-size: 13px; font-weight: 700; color: #0f172a;">MÃ QR CHECK-IN NHẬN SÂN 1 CHẠM</p>
                            <img src="%s" alt="QR Code" style="width: 140px; height: 140px; border: 4px solid #ffffff; border-radius: 8px; box-shadow: 0 4px 10px rgba(0,0,0,0.08); display: inline-block;">
                            <p style="margin: 10px 0 0; font-size: 12px; color: #64748b;">
                                Quý khách vui lòng xuất trình mã QR này tại quầy lễ tân để nhân viên quét nhận sân nhanh chóng.
                            </p>
                        </div>

                        <!-- POLICY NOTE -->
                        <div style="font-size: 12px; color: #64748b; line-height: 1.5; border-left: 3px solid #3b82f6; padding-left: 10px;">
                            * <strong>Lưu ý:</strong> Quý khách vui lòng có mặt trước giờ chơi 10 phút. Hủy đặt sân trước giờ thi đấu tối thiểu 4 tiếng sẽ được hoàn 100%% tiền cọc tự động theo quy định <strong>QĐ-KH03</strong>.
                        </div>
                    </div>

                    <!-- FOOTER -->
                    <div style="background: #0f172a; padding: 16px 24px; text-align: center; font-size: 12px; color: #94a3b8;">
                        Cảm ơn quý khách đã tin tưởng lựa chọn UTE Sport!<br>
                        Hotline hỗ trợ: <strong>0903.123.456</strong> | Email: support@utesport.vn
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                booking.getBookingCode(),
                booking.getStatus() != null ? booking.getStatus() : "Đã xác nhận",
                booking.getCustomerName() != null ? booking.getCustomerName() : "Khách hàng",
                booking.getCustomerPhone() != null ? booking.getCustomerPhone() : "",
                branchName,
                branchAddr,
                courtName,
                courtType,
                dateStr,
                booking.getTimeSlot() != null ? booking.getTimeSlot() : "",
                total.longValue(),
                deposit.longValue(),
                remaining.longValue(),
                qrUrl
            );

        sendHtmlEmailAsync(toEmail, subject, html);
    }

    // =========================================================================
    // 3. GỬI PHIẾU XÁC NHẬN THAM GIA GIẢI ĐẤU (BM-KH03)
    // =========================================================================

    public void sendTournamentRegistration(Tournament tournament, TournamentRegistration reg, String targetEmail) {
        if (tournament == null || reg == null) return;

        String toEmail = targetEmail;
        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            toEmail = reg.getEmail();
        }
        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            if (reg.getCustomerCode() != null) {
                Customer c = customerRepository.findByCustomerCode(reg.getCustomerCode()).orElse(null);
                if (c != null && c.getEmail() != null && c.getEmail().contains("@")) {
                    toEmail = c.getEmail();
                }
            }
            if (toEmail == null || toEmail.isBlank()) {
                if (reg.getPhone() != null) {
                    User u = userRepository.findByPhone(reg.getPhone()).orElse(null);
                    if (u != null && u.getEmail() != null && u.getEmail().contains("@")) {
                        toEmail = u.getEmail();
                    }
                }
            }
        }

        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            log.info("[EMAIL SERVICE] Hồ sơ giải đấu [{}] không có email người đăng ký, bỏ qua gửi mail.", reg.getTournamentCode());
            return;
        }

        String startDateStr = tournament.getStartDate() != null ? tournament.getStartDate().format(DATE_FMT) : "Sắp diễn ra";
        String endDateStr = tournament.getEndDate() != null ? tournament.getEndDate().format(DATE_FMT) : "";
        BigDecimal prize = tournament.getTotalPrize() != null ? tournament.getTotalPrize() : BigDecimal.ZERO;
        BigDecimal fee = tournament.getEntryFee() != null ? tournament.getEntryFee() : BigDecimal.ZERO;

        String subject = "[UTE Sport] Xác nhận đăng ký tham gia giải đấu: " + tournament.getTournamentName();

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px; color: #1e293b;">
                <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.08); border: 1px solid #e2e8f0;">
                    
                    <div style="background: linear-gradient(135deg, #1e1b4b 0%%, #312e81 100%%); padding: 26px 24px; text-align: center; border-bottom: 3px solid #f59e0b;">
                        <h1 style="color: #ffffff; margin: 0; font-size: 20px; font-weight: 800;">UTE SPORT BADMINTON TOURNAMENT</h1>
                        <p style="color: #fbbf24; font-weight: 700; margin: 6px 0 0; font-size: 13px; text-transform: uppercase;">
                            PHIẾU XÁC NHẬN ĐĂNG KÝ GIẢI ĐẤU (BM-KH03)
                        </p>
                    </div>

                    <div style="padding: 24px;">
                        <div style="background: #fef3c7; border: 1px solid #fde68a; border-radius: 12px; padding: 14px 18px; margin-bottom: 20px;">
                            <span style="font-size: 12px; color: #92400e; font-weight: 700;">TÊN GIẢI ĐẤU</span>
                            <h2 style="margin: 4px 0 0; font-size: 18px; color: #78350f;">%s</h2>
                            <p style="margin: 4px 0 0; font-size: 12.5px; color: #92400e;">Mã giải: <strong>%s</strong> | Tổng giải thưởng: <strong>%,d đ</strong></p>
                        </div>

                        <table style="width: 100%%; border-collapse: collapse; font-size: 13.5px; margin-bottom: 20px;">
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 9px 0; color: #64748b; width: 36%%;">Đội / Vận động viên:</td>
                                <td style="padding: 9px 0; font-weight: 700; color: #0f172a;">%s</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 9px 0; color: #64748b;">Số điện thoại liên hệ:</td>
                                <td style="padding: 9px 0; font-weight: 600; color: #0f172a;">%s</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 9px 0; color: #64748b;">Thời gian khởi tranh:</td>
                                <td style="padding: 9px 0; font-weight: 600; color: #2563eb;">%s %s</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 9px 0; color: #64748b;">Địa điểm tổ chức:</td>
                                <td style="padding: 9px 0; color: #0f172a;">Trung tâm Thể thao Cầu lông UTE Sport Arena</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #f1f5f9;">
                                <td style="padding: 9px 0; color: #64748b;">Lệ phí tham gia:</td>
                                <td style="padding: 9px 0; font-weight: 700; color: #059669;">%,d đ</td>
                            </tr>
                            <tr style="background: #ecfdf5;">
                                <td style="padding: 9px 8px; color: #065f46; font-weight: 600;">Trạng thái hồ sơ:</td>
                                <td style="padding: 9px 8px; font-weight: 800; color: #059669;">%s</td>
                            </tr>
                        </table>

                        <div style="background: #f8fafc; border-left: 4px solid #f59e0b; padding: 12px 16px; border-radius: 4px; font-size: 12.5px; color: #475569; line-height: 1.6;">
                            <strong>Kế hoạch tiếp theo:</strong> Ban Tổ Chức sẽ công bố lịch bốc thăm và danh sách chia bảng chính thức trên cổng thông tin UTE Sport trước ngày khai mạc 2 ngày. Chúc bạn và đội thi đấu đạt thành tích xuất sắc!
                        </div>
                    </div>

                    <div style="background: #0f172a; padding: 16px 24px; text-align: center; font-size: 12px; color: #94a3b8;">
                        Ban Tổ Chức Giải Đấu UTE Sport Badminton Club<br>
                        Hotline giải đấu: 0903.123.456 - Email: tournament@utesport.vn
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                tournament.getTournamentName(),
                tournament.getTournamentCode(),
                prize.longValue(),
                reg.getTeamName(),
                reg.getPhone() != null ? reg.getPhone() : "",
                startDateStr,
                !endDateStr.isBlank() ? "đến " + endDateStr : "",
                fee.longValue(),
                reg.getStatus() != null ? reg.getStatus() : "Đã xác nhận đăng ký"
            );

        sendHtmlEmailAsync(toEmail, subject, html);
    }
}
