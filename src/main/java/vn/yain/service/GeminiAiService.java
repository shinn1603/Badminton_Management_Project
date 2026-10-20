package vn.yain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.yain.dto.ChatMessageDto;
import vn.yain.entity.Booking;
import vn.yain.entity.Branch;
import vn.yain.entity.Court;
import vn.yain.entity.Tournament;
import vn.yain.repository.BookingRepository;
import vn.yain.repository.BranchRepository;
import vn.yain.repository.CourtRepository;
import vn.yain.repository.TournamentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiAiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String modelName;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CourtService courtService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public ChatMessageDto chatWithGemini(String userMessage, ChatbotService.ChatSessionContext ctx) {
        if (!isConfigured() || userMessage == null || userMessage.trim().isEmpty()) {
            return null;
        }

        try {
            String systemPrompt = buildSystemPrompt(ctx);

            Map<String, Object> requestBody = new HashMap<>();

            // System instructions
            Map<String, Object> systemInstruction = new HashMap<>();
            systemInstruction.put("parts", List.of(Map.of("text", systemPrompt)));
            requestBody.put("system_instruction", systemInstruction);

            // Multi-turn dialog contents
            List<Map<String, Object>> contents = new ArrayList<>();
            if (ctx.dialogHistory != null) {
                int start = Math.max(0, ctx.dialogHistory.size() - 8);
                for (int i = start; i < ctx.dialogHistory.size(); i++) {
                    Map<String, String> turn = ctx.dialogHistory.get(i);
                    String role = turn.get("role");
                    String text = turn.get("text");
                    if (role != null && text != null && !text.trim().isEmpty()) {
                        contents.add(Map.of("role", role, "parts", List.of(Map.of("text", text))));
                    }
                }
            }

            // Current user message
            contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", userMessage))));
            requestBody.put("contents", contents);

            // Generation config
            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("temperature", 0.6);
            genConfig.put("maxOutputTokens", 800);
            requestBody.put("generationConfig", genConfig);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            // Try candidate models
            List<String> candidateModels = List.of(
                    modelName != null && !modelName.isEmpty() ? modelName : "gemini-1.5-flash",
                    "gemini-flash-latest",
                    "gemini-1.5-flash-latest",
                    "gemini-1.5-pro"
            );

            for (String mod : candidateModels) {
                String endpoint = String.format(
                        "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                        mod, apiKey.trim()
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(12))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode candidateTextNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                    if (!candidateTextNode.isMissingNode()) {
                        String botReply = candidateTextNode.asText();

                        // Add to session history
                        ctx.addDialogTurn("user", userMessage);
                        ctx.addDialogTurn("model", botReply);

                        return processGeminiOutput(botReply, ctx);
                    }
                } else if (response.statusCode() == 404) {
                    continue;
                } else {
                    System.err.println("Gemini API returned status: " + response.statusCode() + " body: " + response.body());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Gemini AI invocation failed, using dual-engine fallback: " + e.getMessage());
        }

        return null;
    }

    private String buildSystemPrompt(ChatbotService.ChatSessionContext ctx) {
        List<Branch> branches = branchRepository != null ? branchRepository.findAll() : Collections.emptyList();
        StringBuilder branchText = new StringBuilder();
        for (Branch b : branches) {
            branchText.append("- ").append(b.getBranchCode()).append(": ").append(b.getBranchName())
                    .append(" (Địa chỉ: ").append(b.getAddress()).append(" | Hotline: ").append(b.getPhone()).append(")\n");
        }

        List<Tournament> tournaments = tournamentRepository != null ? tournamentRepository.findAll() : Collections.emptyList();
        StringBuilder tourText = new StringBuilder();
        for (Tournament t : tournaments) {
            tourText.append("- ").append(t.getTournamentName()).append(" (Giải thưởng: ")
                    .append(t.getTotalPrize() != null ? String.format("%,d đ", t.getTotalPrize().longValue()) : "Hiện kim + Cúp")
                    .append(" | Trạng thái: ").append(t.getStatus()).append(")\n");
        }

        return "Bạn là Trợ lý AI đàm thoại thông minh chính thức của Hệ thống Sân Cầu Lông UTE Sport.\n" +
                "Nhiệm vụ của bạn là trò chuyện tự nhiên, tư vấn chi nhánh gần nhất, tra cứu sân trống, tra cứu đơn đặt sân, hỗ trợ hủy sân và tự động đặt và khóa sân giữ chỗ cho khách.\n\n" +
                "DANH SÁCH CHI NHÁNH HỆ THỐNG:\n" +
                branchText +
                "Bản đồ lân cận:\n" +
                "- CN01 (Thủ Đức - Số 1 Võ Văn Ngân): Gần Hoàng Diệu 2, Kha Vạn Cân, Đặng Văn Bi, Linh Chiểu, Linh Trung, Làng Đại học, Dĩ An.\n" +
                "- CN02 (Bình Thạnh - 234 Điện Biên Phủ): Gần Hàng Xanh, D2/Nguyễn Gia Trí, Cầu Sài Gòn, Bạch Đằng, Xô Viết Nghệ Tĩnh, Quận 1, Quận 3.\n" +
                "- CN03 (Quận 9 - 70 Đỗ Xuân Hợp): Gần Lê Văn Việt, Man Thiện, Tăng Nhơn Phú, Khu Công Nghệ Cao, Vinhomes Grand Park, Thủ Thiêm.\n" +
                "- CN04 (Gò Vấp - 18 Quang Trung): Gần Phan Văn Trị, Nguyễn Oanh, Lê Đức Thọ, Nguyễn Kiệm, Tân Bình, Quận 12.\n\n" +
                "GIỜ MỞ CỬA: 05:30 - 22:30 mỗi ngày.\n" +
                "BẢNG GIÁ:\n" +
                "- Sáng (05:30 - 14:00): 80.000 đ/h\n" +
                "- Chiều (14:00 - 17:00): 100.000 đ/h\n" +
                "- Giờ Vàng (17:00 - 21:00): 140.000 đ/h\n" +
                "- Tối muộn (21:00 - 22:30): 90.000 đ/h\n" +
                "- Cuối tuần: Phụ thu 15.000 đ/h.\n" +
                "QUY ĐỊNH CỌC & HỦY SÂN:\n" +
                "- Đặt sân cọc trước 30% qua VietQR tự động. Hệ thống giữ chỗ 10 phút.\n" +
                "- Hủy sân trước giờ chơi trên 4 tiếng: Hoàn cọc 100%.\n" +
                "- Hủy trong vòng 4 tiếng: Không hỗ trợ hoàn cọc.\n\n" +
                "GIẢI ĐẤU HIỆN CÓ:\n" +
                tourText +
                "DỊCH VỤ ĐI KÈM:\n" +
                "- Cho thuê vợt Yonex Astrox / Lining Axforce: 50.000 đ/ca\n" +
                "- Thuê giày thi đấu: 20.000 đ/đôi\n" +
                "- Cầu lông: Yonex AS-40, Thành Công 77, VinaStar (250.000 - 420.000 đ/ống)\n" +
                "- Dịch vụ đan vợt điện tử: 50.000 - 90.000 đ\n" +
                "- Bãi giữ xe và phòng tắm nóng lạnh hoàn toàn miễn phí.\n\n" +
                "QUY TẮC BẮT BUỘC:\n" +
                "1. TUYỆT ĐỐI KHÔNG dùng bất kỳ emoji hoặc icon nào.\n" +
                "2. Giọng điệu lịch sự, chuyên nghiệp, súc tích, phong cách tư vấn thể thao đẳng cấp.\n" +
                "3. QUY TRÌNH ĐẶT VÀ KHÓA SÂN:\n" +
                "   Bạn PHẢI đảm bảo thu thập đầy đủ 6 thông tin bắt buộc sau trước khi cho hiện mã khóa sân:\n" +
                "   (1) Họ tên khách hàng\n" +
                "   (2) Số điện thoại liên hệ (10 số)\n" +
                "   (3) Chi nhánh (CN01, CN02, CN03 hoặc CN04)\n" +
                "   (4) Đặt sân gì (Sân VIP hay Sân tiêu chuẩn)\n" +
                "   (5) Mấy giờ và ngày nào (Khung giờ bắt đầu - kết thúc)\n" +
                "   (6) Có chọn thêm dịch vụ/phụ kiện đi kèm không (Thuê vợt, giày, mua cầu, nước uống hoặc 'Không chọn thêm gì')\n" +
                "   - Nếu THIẾU BẤT KỲ THÔNG TIN NÀO trong 6 thông tin trên, TUYỆT ĐỐI KHÔNG xuất mã khóa sân, hãy lịch sự hỏi thông tin còn thiếu.\n" +
                "   - Khi đã đủ cả 6 thông tin, hãy tóm tắt rõ ràng cho khách và hỏi khách xác nhận.\n" +
                "   - CHỈ KHI ĐÃ ĐỦ 6 THÔNG TIN VÀ KHÁCH XÁC NHẬN CHỐT, bạn mới đính kèm thẻ tạo mã khóa sân:\n" +
                "   [ACTION:BOOKING name=\"" + (ctx.customerName != null ? ctx.customerName : "") + "\" phone=\"" + (ctx.customerPhone != null ? ctx.customerPhone : "") + "\" court=\"" + (ctx.courtCode != null ? ctx.courtCode : "") + "\" branch=\"" + (ctx.branchCode != null ? ctx.branchCode : "") + "\" slot=\"" + (ctx.timeSlot != null ? ctx.timeSlot : "") + "\" addons=\"" + (ctx.additionalServices != null ? ctx.additionalServices : "Không có") + "\" price=\"" + ctx.totalPrice + "\" deposit=\"" + ctx.depositAmount + "\"]\n" +
                "4. Khi khách muốn tra cứu mã đơn đặt sân, đính kèm thẻ:\n" +
                "   [ACTION:LOOKUP code=\"DS...\"]\n" +
                "5. Khi khách muốn hủy đơn đặt sân, đính kèm thẻ:\n" +
                "   [ACTION:CANCEL code=\"DS...\"]";
    }

    private ChatMessageDto processGeminiOutput(String rawReply, ChatbotService.ChatSessionContext ctx) {
        String cleanText = rawReply;
        String actionType = "NONE";
        Map<String, Object> paymentData = null;
        Map<String, Object> lookupData = null;

        // Check for [ACTION:BOOKING ...] tag
        Pattern bookingPattern = Pattern.compile("\\[ACTION:BOOKING\\s+([^\\]]+)\\]");
        Matcher bookingMatcher = bookingPattern.matcher(rawReply);

        if (bookingMatcher.find()) {
            String attributes = bookingMatcher.group(1);
            cleanText = rawReply.replace(bookingMatcher.group(0), "").trim();

            String name = extractAttr(attributes, "name", ctx.customerName);
            String phone = extractAttr(attributes, "phone", ctx.customerPhone);
            String courtCode = extractAttr(attributes, "court", ctx.courtCode);
            String branchCode = extractAttr(attributes, "branch", ctx.branchCode);
            String slot = extractAttr(attributes, "slot", ctx.timeSlot);
            String addons = extractAttr(attributes, "addons", ctx.additionalServices != null ? ctx.additionalServices : "Không có");
            String priceStr = extractAttr(attributes, "price", ctx.totalPrice.toString());
            String depositStr = extractAttr(attributes, "deposit", ctx.depositAmount.toString());

            // Đảm bảo đủ thông tin cơ bản trước khi tạo đơn
            if (name == null || name.isBlank() || phone == null || !phone.matches("0[3|5|7|8|9]\\d{8}")) {
                return ChatMessageDto.builder()
                        .sender("bot")
                        .text("Để hoàn tất tạo mã khóa sân giữ chỗ, bạn vui lòng cung cấp đầy đủ Họ tên và Số điện thoại liên hệ (10 chữ số) nhé!")
                        .quickReplies(List.of("Bắt đầu lại"))
                        .actionType("NONE")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            actionType = "OPEN_PAYMENT";
            BigDecimal totalPrice = new BigDecimal(priceStr != null && !priceStr.isBlank() ? priceStr : "280000");
            BigDecimal depositAmount = new BigDecimal(depositStr != null && !depositStr.isBlank() ? depositStr : "84000");

            String bookingCode = bookingService.generateUniqueBookingCode();
            ctx.lastBookingCode = bookingCode;
            ctx.customerName = name;
            ctx.customerPhone = phone;

            Booking booking = new Booking();
            booking.setBookingCode(bookingCode);
            booking.setBranchCode(branchCode != null ? branchCode : "CN01");
            booking.setCourtCode(courtCode != null ? courtCode : "CL01");
            booking.setCustomerName(name);
            booking.setCustomerPhone(phone);
            booking.setCustomerCode(ctx.customerCode);
            booking.setBookingDate(ctx.date != null ? ctx.date : LocalDate.now());
            booking.setTimeSlot(slot != null ? slot : "18:00 - 20:00");
            booking.setHourlyPrice(totalPrice.divide(new BigDecimal(2), RoundingMode.HALF_UP));
            booking.setTotalPrice(totalPrice);
            booking.setDepositAmount(depositAmount);
            booking.setPaymentMethod("Chuyển khoản VietQR");
            booking.setStatus("Chờ cọc 30%");
            booking.setNotes("Dịch vụ đi kèm: " + addons + " (Đặt tự động qua Google Gemini AI)");

            try {
                bookingService.createBooking(booking);
                courtService.holdCourtForBooking(booking.getCourtCode(), booking.getBranchCode(), booking.getTimeSlot(), booking.getCustomerName());

                String qrUrl = String.format(
                        "https://img.vietqr.io/image/970422-0903123456-compact2.png?amount=%d&addInfo=CK%%20%s&accountName=UTE%%20BADMINTON%%20CLUB",
                        depositAmount.longValue(), bookingCode
                );

                paymentData = new HashMap<>();
                paymentData.put("bookingCode", bookingCode);
                paymentData.put("courtName", ctx.courtName != null ? ctx.courtName : "Sân thi đấu tiêu chuẩn");
                paymentData.put("branchName", ctx.branchName != null ? ctx.branchName : "Chi nhánh 1");
                paymentData.put("bookingDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                paymentData.put("timeSlot", slot);
                paymentData.put("additionalServices", addons);
                paymentData.put("totalPrice", totalPrice);
                paymentData.put("depositAmount", depositAmount);
                paymentData.put("qrUrl", qrUrl);
                paymentData.put("bankName", "MBBank (Ngân hàng Quân Đội)");
                paymentData.put("accountNo", "0903123456");
                paymentData.put("accountName", "UTE BADMINTON CLUB");
            } catch (Exception e) {
                actionType = "NONE";
                cleanText = "Không thể hoàn tất tạo đơn đặt sân vì: " + e.getMessage() + ". Vui lòng thử lại với khung giờ hoặc sân khác.";
            }

            return ChatMessageDto.builder()
                    .sender("bot")
                    .text(cleanText)
                    .actionType(actionType)
                    .paymentData(paymentData)
                    .quickReplies(List.of("Xác nhận đã chuyển khoản cọc", "Xem lại thông tin đơn", "Hướng dẫn hủy ca chơi"))
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // Check for [ACTION:LOOKUP ...] tag
        Pattern lookupPattern = Pattern.compile("\\[ACTION:LOOKUP\\s+code=\"([^\"]+)\"\\]");
        Matcher lookupMatcher = lookupPattern.matcher(rawReply);
        if (lookupMatcher.find()) {
            String code = lookupMatcher.group(1);
            cleanText = rawReply.replace(lookupMatcher.group(0), "").trim();
            Optional<Booking> opt = bookingRepository.findByBookingCode(code);
            if (opt.isPresent()) {
                Booking b = opt.get();
                actionType = "LOOKUP_RESULT";
                lookupData = new HashMap<>();
                lookupData.put("bookingCode", b.getBookingCode());
                lookupData.put("courtCode", b.getCourtCode());
                lookupData.put("branchCode", b.getBranchCode());
                lookupData.put("date", b.getBookingDate() != null ? b.getBookingDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
                lookupData.put("timeSlot", b.getTimeSlot());
                lookupData.put("status", b.getStatus());
                lookupData.put("totalPrice", b.getTotalPrice());
                lookupData.put("depositAmount", b.getDepositAmount());
            }
        }

        // Check for [ACTION:CANCEL ...] tag
        Pattern cancelPattern = Pattern.compile("\\[ACTION:CANCEL\\s+code=\"([^\"]+)\"\\]");
        Matcher cancelMatcher = cancelPattern.matcher(rawReply);
        if (cancelMatcher.find()) {
            String code = cancelMatcher.group(1);
            cleanText = rawReply.replace(cancelMatcher.group(0), "").trim();
            try {
                bookingService.cancelBooking(code, "Hủy qua yêu cầu trợ lý AI");
            } catch (Exception e) {
                cleanText = "Không thể hủy đơn " + code + ": " + e.getMessage();
            }
        }

        return ChatMessageDto.builder()
                .sender("bot")
                .text(cleanText)
                .actionType(actionType)
                .paymentData(paymentData)
                .lookupData(lookupData)
                .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Chi nhánh nào gần tôi?", "Bảng giá thuê sân"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String extractAttr(String text, String key, String defaultValue) {
        Pattern p = Pattern.compile(key + "=\"([^\"]+)\"");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return defaultValue;
    }
}
