package vn.yain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.yain.dto.ChatMessageDto;
import vn.yain.entity.Booking;
import vn.yain.entity.Branch;
import vn.yain.entity.Court;
import vn.yain.entity.Tournament;
import vn.yain.repository.BookingRepository;
import vn.yain.repository.BranchRepository;
import vn.yain.repository.CourtRepository;
import vn.yain.repository.CustomerRepository;
import vn.yain.repository.TournamentRepository;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatbotService {

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CourtService courtService;

    @Autowired
    private GeminiAiService geminiAiService;

    @Autowired
    private PricingService pricingService;

    @Autowired(required = false)
    private IntentClassificationService intentClassifier;

    // Session Memory Store (sessionId -> ChatSessionContext)
    private final Map<String, ChatSessionContext> sessionStore = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 600000)
    public void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        // Evict sessions inactive for over 30 minutes
        sessionStore.entrySet().removeIf(entry -> (now - entry.getValue().lastAccessed) > 1800000);
    }

    public static class ChatSessionContext {
        public long lastAccessed = System.currentTimeMillis();
        public String customerName = null;
        public String customerPhone = null;
        public String customerCode = null;

        public String branchCode = null;
        public String branchName = null;

        public String courtCode = null;
        public String courtName = null;
        public String courtType = null; // "VIP", "STANDARD"

        public LocalDate date = null;
        public String timeSlot = null; // e.g. "18:00 - 20:00"
        public int durationHours = 0;

        public String additionalServices = null; // e.g. "Không chọn thêm (Chỉ thuê sân)" or "Thuê 2 vợt, 2 nước"
        public BigDecimal additionalFee = BigDecimal.ZERO;
        public BigDecimal courtPrice = BigDecimal.ZERO;
        public BigDecimal hourlyRate = BigDecimal.ZERO;
        public BigDecimal totalPrice = BigDecimal.ZERO;
        public BigDecimal depositAmount = BigDecimal.ZERO;

        public String stage = "GREETING"; // GREETING, COLLECTING_INFO, PROPOSING_COURT, PROPOSING_ADDONS, ASK_NAME, ASK_PHONE, AWAITING_CONFIRMATION, BOOKED
        public String lastBookingCode = null;
        public List<Map<String, String>> dialogHistory = new ArrayList<>();

        public boolean isNameProvided() {
            return customerName != null && !customerName.isBlank() && !"Khách đặt qua AI".equalsIgnoreCase(customerName.trim());
        }

        public boolean isPhoneProvided() {
            return customerPhone != null && customerPhone.matches("0[3|5|7|8|9]\\d{8}");
        }

        public boolean isBranchProvided() {
            return branchCode != null && !branchCode.isBlank();
        }

        public boolean isSlotProvided() {
            return timeSlot != null && !timeSlot.isBlank() && date != null;
        }

        public boolean isCourtProvided() {
            return courtCode != null && !courtCode.isBlank();
        }

        public boolean isAddOnsProvided() {
            return additionalServices != null && !additionalServices.isBlank();
        }

        public boolean hasAllBookingInfo() {
            return isNameProvided() && isPhoneProvided() && isBranchProvided() && isSlotProvided() && isCourtProvided() && isAddOnsProvided();
        }

        public void addDialogTurn(String role, String text) {
            lastAccessed = System.currentTimeMillis();
            if (dialogHistory.size() > 14) {
                dialogHistory.remove(0);
            }
            dialogHistory.add(Map.of("role", role, "text", text));
        }

        public void reset() {
            lastAccessed = System.currentTimeMillis();
            customerName = null;
            customerPhone = null;
            customerCode = null;
            branchCode = null;
            branchName = null;
            courtCode = null;
            courtName = null;
            courtType = null;
            date = null;
            timeSlot = null;
            durationHours = 0;
            additionalServices = null;
            additionalFee = BigDecimal.ZERO;
            courtPrice = BigDecimal.ZERO;
            hourlyRate = BigDecimal.ZERO;
            totalPrice = BigDecimal.ZERO;
            depositAmount = BigDecimal.ZERO;
            stage = "GREETING";
            lastBookingCode = null;
            dialogHistory.clear();
        }
    }

    public void resetSession(String sessionId) {
        if (sessionId != null) {
            ChatSessionContext ctx = sessionStore.get(sessionId);
            if (ctx != null) {
                ctx.reset();
            }
        }
    }

    public ChatMessageDto processMessage(String rawInput, String sessionId) {
        return processMessage(rawInput, sessionId, null, null, null);
    }

    public ChatMessageDto processMessage(String rawInput, String sessionId, String customerName, String customerPhone, String customerCode) {
        String sid = (sessionId != null && !sessionId.trim().isEmpty()) ? sessionId.trim() : "default_session";
        ChatSessionContext ctx = sessionStore.computeIfAbsent(sid, k -> new ChatSessionContext());
        ctx.lastAccessed = System.currentTimeMillis();

        if (customerPhone != null && !customerPhone.isBlank() && customerPhone.matches("0[3|5|7|8|9]\\d{8}")) {
            ctx.customerPhone = customerPhone.trim();
        }
        if (customerName != null && !customerName.isBlank() && !"Khách đặt qua AI".equalsIgnoreCase(customerName.trim())) {
            ctx.customerName = customerName.trim();
        }
        if (customerCode != null && !customerCode.isBlank()) {
            ctx.customerCode = customerCode.trim();
        }

        // Welcome Greeting if empty input
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .text("Xin chào bạn! Tôi là tư vấn viên hỗ trợ trực tuyến của Hệ thống Sân Cầu Lông UTE Sport. Tôi có thể hỗ trợ bạn tìm chi nhánh gần nhất, tra cứu sân trống theo khung giờ cụ thể, kiểm tra thông tin đơn đặt sân, hướng dẫn hủy sân và tự động đặt và khóa sân giữ chỗ cho bạn ngay trong cuộc trò chuyện này. Bạn cần tôi hỗ trợ điều gì?")
                    .quickReplies(List.of("Hệ thống có những chi nhánh nào?", "Tôi ở đường Hoàng Diệu 2 thì chi nhánh nào gần tôi?", "Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        String text = rawInput.trim();
        String normalized = removeAccents(text.toLowerCase());

        // Extract slots / entities proactively from user message
        extractBookingSlots(text, normalized, ctx);

        // 1. Try Google Gemini Cloud AI if configured
        if (geminiAiService != null && geminiAiService.isConfigured()) {
            ChatMessageDto aiReply = geminiAiService.chatWithGemini(text, ctx);
            if (aiReply != null) {
                return aiReply;
            }
        }

        // 2. Machine Learning Intent Classification (TF-IDF + Character 3-Gram)
        IntentClassificationService.PredictionResult mlResult = null;
        if (intentClassifier != null) {
            mlResult = intentClassifier.predict(text);
        }
        String mlTag = (mlResult != null) ? mlResult.getIntent() : "";

        // Chặn sớm nếu người dùng hỏi ngoài luồng (Toán học, Thời tiết, Nấu ăn...)
        if (mlResult != null && mlResult.isOutOfScope()) {
            if ("OUT_OF_SCOPE_MATH".equals(mlTag)) {
                return ChatMessageDto.builder()
                        .sender("bot")
                        .intent("OUT_OF_SCOPE_MATH")
                        .text(mlResult.getDefaultResponse())
                        .quickReplies(List.of("Bảng giá thuê sân", "Hôm nay có sân nào trống từ 18h đến 20h không?", "Hệ thống có những chi nhánh nào?"))
                        .actionType("NONE")
                        .timestamp(LocalDateTime.now())
                        .build();
            } else if ("OUT_OF_SCOPE_GENERAL".equals(mlTag)) {
                return ChatMessageDto.builder()
                        .sender("bot")
                        .intent("OUT_OF_SCOPE_GENERAL")
                        .text(mlResult.getDefaultResponse())
                        .quickReplies(List.of("Bảng giá thuê sân", "Hệ thống có những chi nhánh nào?", "Tra cứu sân trống"))
                        .actionType("NONE")
                        .timestamp(LocalDateTime.now())
                        .build();
            }
        }

        // =========================================================================
        // MICRO-FAQ 1: CHI TIẾT PHÒNG TẮM & NƯỚC NÓNG
        // =========================================================================
        if ((normalized.contains("tam") && (normalized.contains("phong") || normalized.contains("buong") || normalized.contains("nuoc nong") || normalized.contains("may cho") || normalized.contains("khan") || normalized.contains("say toc")))
                || matches(normalized, "phong tam may cho", "may buong tam", "co nuoc nong", "nuoc nong khong", "may say toc", "dau goi", "sua tam", "khan tam", "tam o dau")) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("FAQ_BATHROOM_DETAIL")
                    .text("Chi tiết tiện ích phòng tắm tại các cơ sở UTE Sport:\n\n" +
                          "• Số lượng buồng tắm: Mỗi chi nhánh được thiết kế từ 4 - 6 buồng tắm riêng biệt khép kín (chia khu nam và nữ riêng).\n" +
                          "• Bình nước nóng: 100% buồng tắm đều trang bị bình nước nóng Ariston cao cấp, phục vụ nước nóng tắm sảng khoái và thư giãn cơ bắp sau trận đấu.\n" +
                          "• Tiện ích miễn phí: Có sẵn dầu gội, sữa tắm, máy sấy tóc tại bàn trang điểm và móc treo đồ sạch sẽ.\n" +
                          "• Khăn tắm: Có cho thuê khăn bông tiệt trùng (10.000đ/cái) hoặc bạn có thể tự mang khăn cá nhân.")
                    .quickReplies(List.of("Bãi đỗ xe ô tô thế nào?", "Bảng giá thuê sân", "Hôm nay có sân nào trống từ 18h đến 20h không?"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // MICRO-FAQ 2: CHI TIẾT VỚ / TẤT THỂ THAO
        // =========================================================================
        if (((containsWord(normalized, "tat") || containsWord(normalized, "vo")) && (normalized.contains("doi") || normalized.contains("chong truot") || normalized.contains("co ngan") || normalized.contains("co trung") || matches(normalized, "ban tat", "ban vo", "mua tat", "mua vo", "tat the thao", "vo the thao", "tat bao nhieu", "vo bao nhieu", "gia tat", "gia vo")))
                || matches(normalized, "tat la 1 doi", "tat la mot doi", "ban tat", "ban vo", "vo may tien", "tat bao nhieu", "vo 1 doi", "tat the thao", "vo chong truot")) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("FAQ_SOCKS_DETAIL")
                    .text("Thông tin về Vớ/Tất thể thao tại quầy phụ kiện UTE Sport:\n\n" +
                          "• Quy cách bán: Bán theo từng đôi (1 đôi / gói nguyên seal chính hãng).\n" +
                          "• Chủng loại: Vớ thể thao Yonex & Victor cổ ngắn và cổ trung, dệt kim sợi Cotton co giãn 4 chiều dày dặn, lòng bàn chân có đệm xù chống trơn trượt và bảo vệ gót chân.\n" +
                          "• Đơn giá: 25.000 đ/đôi (có đủ màu đen, trắng, xám).")
                    .quickReplies(List.of("Thuê giày cầu lông", "Thuê vợt thi đấu", "Bảng giá thuê sân"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // MICRO-FAQ 3: CHI TIẾT TÊN CÁC DÒNG VỢT CHO THUÊ & MỨC CĂNG
        // =========================================================================
        if ((normalized.contains("vot") && (normalized.contains("ten gi") || normalized.contains("hang gi") || normalized.contains("loai gi") || normalized.contains("hieu gi") || normalized.contains("cang") || normalized.contains("do cang") || normalized.contains("thong so") || normalized.contains("astrox") || normalized.contains("100zz") || normalized.contains("88d") || normalized.contains("lining")))
                || matches(normalized, "vot ten gi", "thue vot gi", "vot hang gi", "cac loai vot", "cang may kg", "do cang vot", "hang vot nao")) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("FAQ_RACKETS_DETAIL")
                    .text("Chi tiết các dòng vợt cho thuê tại UTE Sport:\n\n" +
                          "1. Dòng Cao Cấp / Thi Đấu Chuyên Nghiệp (50.000đ/ca):\n" +
                          "   • Yonex Astrox 88D Pro / 88S Pro (Vợt công thủ toàn diện thế hệ mới)\n" +
                          "   • Yonex Astrox 100ZZ (Vợt tấn công smash cắm sân cực mạnh)\n" +
                          "   • Yonex Nanoflare 1000Z (Vợt tốc độ phản tạt chớp nhoáng)\n" +
                          "   • Lining Axforce 80 / Halbertec 8000 (Đầm tay, trợ lực tốt)\n" +
                          "2. Dòng Phổ Thông / Dễ Chơi (30.000đ/ca):\n" +
                          "   • Yonex Play / Game series, Victor DriveX\n\n" +
                          "• Thông số kỹ thuật: Trọng lượng 4U (83g), chu vi cán G5 vừa tay, đan sẵn cước Yonex BG65 / BG66 Ultimax với mức căng chuẩn 10.5 kg - 11.0 kg (23 - 24.5 lbs).")
                    .quickReplies(List.of("1 ống cầu tên gì và mấy quả?", "Thuê giày cầu lông", "Bảng giá thuê sân"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // MICRO-FAQ 4: CHI TIẾT CÁC LOẠI CẦU & ỐNG CẦU
        // =========================================================================
        if ((normalized.contains("cau") && (normalized.contains("ong") || normalized.contains("ten gi") || normalized.contains("may qua") || normalized.contains("hang gi") || normalized.contains("ban le") || normalized.contains("loai gi") || normalized.contains("hieu gi")))
                || matches(normalized, "ong cau ten gi", "1 ong cau", "ong cau may qua", "cau ten gi", "cau hieu gi", "ban le tung qua", "gia 1 qua cau", "cau ben nhat")) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("FAQ_SHUTTLECOCK_DETAIL")
                    .text("Chi tiết các loại quả cầu lông tại quầy lễ tân UTE Sport:\n\n" +
                          "• Quy cách đóng gói: 1 ống tiêu chuẩn gồm 12 quả cầu lông vũ cao cấp.\n" +
                          "• Các thương hiệu có sẵn:\n" +
                          "   1. Cầu Yonex Aerosensa AS-40: 420.000 đ/ống (Chuẩn thi đấu quốc tế BWF, đường bay ổn định tuyệt đối, lông bền nhất).\n" +
                          "   2. Cầu Thành Công 77: 260.000 đ/ống (Được ưa chuộng số 1 phong trào, bay chuẩn, đầm tay).\n" +
                          "   3. Cầu VinaStar ruột đỏ: 250.000 đ/ống (Tốc độ bay vừa phải, đường cầu sắc nét).\n" +
                          "   4. Cầu Hải Yến S90: 240.000 đ/ống.\n" +
                          "• Bán lẻ: Có bán lẻ từng quả cho khách đánh thiếu cầu (Cầu Thành Công: 25.000 đ/quả; Cầu Yonex AS-40: 38.000 đ/quả).")
                    .quickReplies(List.of("Vợt cho thuê tên gì?", "Cho thuê giày", "Hôm nay có sân nào trống từ 18h đến 20h không?"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // MICRO-FAQ 5: CHI TIẾT BÃI ĐỖ XE & PHÍ GIỮ XE
        // =========================================================================
        if ((normalized.contains("xe") && (normalized.contains("gui") || normalized.contains("do") || normalized.contains("o to") || normalized.contains("oto") || normalized.contains("7 cho") || normalized.contains("mat tien") || normalized.contains("phi gui") || normalized.contains("bai")))
                || matches(normalized, "phi gui xe", "tien gui xe", "gui xe may", "dau o to", "do xe o to", "xe 7 cho", "bai xe", "co mat tien gui xe")) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("FAQ_PARKING_DETAIL")
                    .text("Thông tin Bãi giữ xe tại Chuỗi Sân Cầu Lông UTE Sport:\n\n" +
                          "• Chi phí gửi xe: MIỄN PHÍ 100% tiền gửi xe máy và xe ô tô cho tất cả khách hàng đến chơi sân.\n" +
                          "• Bãi xe ô tô: Khuôn viên rộng rãi, đỗ thoải mái các dòng xe 4 chỗ, 7 chỗ (Innova, Fortuner, Carnival...) và xe bán tải, quay đầu xe dễ dàng.\n" +
                          "• Bãi xe máy: Có mái che thoáng mát, sức chứa trên 150 xe mỗi cơ sở.\n" +
                          "• An ninh: Có nhân viên bảo vệ phát vé thẻ từ và camera an ninh hồng ngoại giám sát 24/24.")
                    .quickReplies(List.of("Phòng tắm có nước nóng không?", "Bảng giá thuê sân", "Chi nhánh nào gần tôi?"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // MICRO-FAQ 6: CHI TIẾT GIÀY & BẢNG SIZE
        // =========================================================================
        if ((normalized.contains("giay") && (normalized.contains("size") || normalized.contains("lefus") || normalized.contains("kawasaki") || normalized.contains("khu khuan") || normalized.contains("co lon") || normalized.contains("co nho")))
                || matches(normalized, "size bao nhieu", "co size", "size giay", "thue giay size", "giay hang gi", "giay lefus", "giay kawasaki")) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("FAQ_SHOES_DETAIL")
                    .text("Chi tiết Dịch vụ cho thuê giày cầu lông tại UTE Sport:\n\n" +
                          "• Thương hiệu: Giày Lefus, Kawasaki, Victor chuyên dụng có đế cao su kếp (Non-marking) giảm chấn khớp chân và không trầy xước mặt thảm BWF.\n" +
                          "• Bảng size: Có sẵn đầy đủ size từ Size 36 đến Size 44 (cả nam và nữ).\n" +
                          "• Giá thuê: 20.000 đ/đôi/ca chơi.\n" +
                          "• Vệ sinh & Khử khuẩn: 100% giày sau mỗi lượt chơi đều được xịt cồn y tế khử khuẩn và sấy khô bằng máy khử mùi tia cực tím UV.")
                    .quickReplies(List.of("Tất là 1 đôi à?", "Vợt cho thuê tên gì?", "Hôm nay có sân nào trống từ 18h đến 20h không?"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // MICRO-FAQ 7: CHI TIẾT MENU NƯỚC & ĐỒ ĂN NHẸ F&B
        // =========================================================================
        if (matches(normalized, "mi ly", "xuc xich", "do an nhe", "bo huc", "redbull", "pocari", "revive", "aquafina", "nuoc ngot", "ban nuoc gi", "ban do an", "chuoi")) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("FAQ_FOOD_DRINK_DETAIL")
                    .text("Menu Quầy Nước & Đồ Ăn Nhanh tại UTE Sport:\n\n" +
                          "• Nước giải khát thể thao (Ướp lạnh):\n" +
                          "   - Revive bù khoáng / chanh muối: 15.000 đ/chai\n" +
                          "   - Pocari Sweat điện giải ion: 20.000 đ/chai\n" +
                          "   - Bò Húc (RedBull Thái nhập): 20.000 đ/lon\n" +
                          "   - Nước suối Aquafina 500ml: 10.000 đ/chai\n" +
                          "   - Nước dừa tươi đóng lon: 20.000 đ/lon\n" +
                          "• Đồ ăn nhẹ nạp năng lượng:\n" +
                          "   - Mì ly Modern / Cung Đình (sẵn nước sôi, đũa): 15.000 đ/ly\n" +
                          "   - Xúc xích tiệt trùng CP: 15.000 đ/cây\n" +
                          "   - Chuối tươi thể thao giàu Kali: 8.000 đ/trái.")
                    .quickReplies(List.of("Bảng giá thuê sân", "Hôm nay có sân nào trống từ 18h đến 20h không?", "Chi nhánh nào gần tôi?"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // MICRO-FAQ 8: CHI TIẾT THÔNG SỐ SÂN, ĐÈN & TRẦN NHÀ
        // =========================================================================
        if (matches(normalized, "chieu cao tran", "tran cao", "may quat", "den may lux", "lux", "khoang cach san", "day may mm", "thong so san")) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("FAQ_COURT_SPECS_DETAIL")
                    .text("Thông số kỹ thuật chuẩn hóa tại Chuỗi Sân Cầu Lông UTE Sport:\n\n" +
                          "• Mặt thảm: Thảm vân cát Yonex & Enlio độ dày 5.0mm (lớp bọt PVC chống sốc đàn hồi cao, đạt chuẩn quốc tế BWF).\n" +
                          "• Chiều cao trần: Đạt 9.5m tiêu chuẩn thi đấu quốc tế, đánh cầu cao sâu hoàn toàn không bị chạm trần.\n" +
                          "• Hệ thống chiếu sáng: Đèn LED rọi chống lóa cao cấp gắn nghiêng góc 45°, độ sáng đạt 500 Lux, không gây chói mắt khi nhìn lên đỡ cầu.\n" +
                          "• Làm mát: Bố trí từ 6 - 8 quạt công nghiệp xung quanh, đối lưu gió mát mẻ mà không làm bạt luồng bay của quả cầu.\n" +
                          "• Khoảng cách: Cách biên giữa hai sân 1.2m, cách tường sau 1.5m đảm bảo an toàn tối đa khi cứu cầu.")
                    .quickReplies(List.of("Phòng tắm có nước nóng không?", "Bãi đỗ xe ô tô thế nào?", "Bảng giá thuê sân"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // FAQ: GIẢI THÍCH CHỨC NĂNG TỰ KHÓA SÂN
        // =========================================================================
        if (matches(normalized, "chuc nang tu khoa san", "tu khoa san la gi", "khoa san la gi", "tu khoa san hoat dong the nao", "lam sao de tu khoa san", "co che tu khoa san", "khoa san the nao")) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("FAQ_AUTO_LOCK_EXPLAIN")
                    .text("Cơ chế **Tự Khóa Sân** tại UTE Sport hoạt động như sau:\n\n" +
                          "1. **Tạm khóa giữ chỗ 10 phút**: Khi bạn chọn sân và xác nhận đặt, hệ thống sẽ tạm giữ sân ngay lập tức (trạng thái 'Đang giữ chỗ'), ngăn khách khác đặt trùng.\n" +
                          "2. **Thanh toán VietQR**: Hệ thống tạo sẵn mã VietQR kèm tiền cọc 30% và mã đơn ca chơi.\n" +
                          "3. **Khóa sân cố định chính thức**: Sau khi bạn chuyển khoản cọc và bấm 'Xác nhận đã chuyển khoản cọc', sân sẽ được khóa cố định chính thức (trạng thái 'Đang sử dụng') và đơn được ghi nhận ngay vào [Lịch Sử Đặt Sân](/customer/history) của tài khoản bạn.\n" +
                          "4. **Nhận sân**: Bạn chỉ cần đến quầy lễ tân đúng giờ để nhận sân thi đấu.\n\n" +
                          "Bạn có muốn đặt và tự khóa sân ngay bây giờ không?")
                    .quickReplies(List.of("Đặt sân 18h - 20h hôm nay cơ sở 1", "Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT 0: BẮT ĐẦU LẠI / LÀM MỚI HỘI THOẠI
        // =========================================================================
        if ("GREETING_RESET".equals(mlTag) || matches(normalized, "bat dau lai", "lam moi", "xoa lich su", "reset", "chao bot", "hello", "xin chao")) {
            ctx.reset();
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("RESET")
                    .text("Hội thoại đã được làm mới. Tôi sẵn sàng hỗ trợ bạn tìm chi nhánh, tra cứu lịch sân trống và đặt sân giữ chỗ. Bạn muốn tìm sân ở khu vực nào hoặc vào khung giờ nào?")
                    .quickReplies(List.of("Hệ thống có những chi nhánh nào?", "Tôi ở Thủ Đức thì chi nhánh nào gần?", "Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT: TIẾP TỤC THANH TOÁN / MỞ LẠI MÃ KHÓA SÂN & VIETQR
        // =========================================================================
        if (!normalized.contains("huy") && ("CONTINUE_PAYMENT".equals(mlTag) || matches(normalized,
                "tiep tuc thanh toan", "thanh toan tiep", "thanh toan don", "mo lai thanh toan", "mo lai ma khoa san",
                "chua chuyen khoan", "chua chuyen khoan xong", "ma khoa san cua toi", "ma khoa san",
                "chuyen khoan coc", "lay lai ma coc", "xem lai thanh toan", "thanh toan", "mo lai ma qr", "ma qr"))) {

            String targetCode = null;
            Pattern codePattern = Pattern.compile("(?:ds\\s*)?(\\d{6})", Pattern.CASE_INSENSITIVE);
            Matcher mCode = codePattern.matcher(normalized);
            if (mCode.find()) {
                targetCode = "DS" + mCode.group(1);
            } else if (ctx.lastBookingCode != null) {
                targetCode = ctx.lastBookingCode;
            } else if (ctx.customerPhone != null && !ctx.customerPhone.isBlank()) {
                List<Booking> userBookings = bookingRepository.findByCustomerPhoneOrderByBookingDateDesc(ctx.customerPhone);
                if (userBookings != null && !userBookings.isEmpty()) {
                    targetCode = userBookings.get(0).getBookingCode();
                }
            } else if (ctx.customerCode != null && !ctx.customerCode.isBlank()) {
                List<Booking> userBookings = bookingRepository.findByCustomerCodeOrderByBookingDateDesc(ctx.customerCode);
                if (userBookings != null && !userBookings.isEmpty()) {
                    targetCode = userBookings.get(0).getBookingCode();
                }
            }

            if (targetCode != null) {
                Optional<Booking> optBooking = bookingRepository.findByBookingCode(targetCode);
                if (optBooking.isPresent()) {
                    Booking b = optBooking.get();
                    if (b.getStatus() != null && (b.getStatus().contains("Đã cọc") || b.getStatus().contains("hoàn thành") || b.getStatus().contains("Hoàn thành"))) {
                        return ChatMessageDto.builder()
                                .sender("bot")
                                .intent("ALREADY_PAID")
                                .text(String.format("Đơn đặt sân **%s** của bạn đã được ghi nhận thanh toán tiền cọc thành công! Bạn có thể xem vé và mã QR check-in tại [Lịch Sử Đặt Sân](/customer/history).", b.getBookingCode()))
                                .actionType("NONE")
                                .quickReplies(List.of("Xem Lịch Sử Đặt Sân", "Hướng dẫn hủy ca chơi", "Bảng giá thuê sân"))
                                .timestamp(LocalDateTime.now())
                                .build();
                    } else if (b.getStatus() != null && b.getStatus().contains("hủy")) {
                        return ChatMessageDto.builder()
                                .sender("bot")
                                .intent("BOOKING_CANCELLED")
                                .text(String.format("Đơn đặt sân **%s** đã bị hủy. Bạn có thể chọn đặt sân mới nếu muốn thi đấu.", b.getBookingCode()))
                                .actionType("NONE")
                                .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân"))
                                .timestamp(LocalDateTime.now())
                                .build();
                    }

                    // Booking is currently pending deposit -> open VietQR payment card!
                    BigDecimal dep = b.getDepositAmount() != null ? b.getDepositAmount()
                            : (b.getTotalPrice() != null ? b.getTotalPrice().multiply(new BigDecimal("0.3")).setScale(0, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
                    String bin = (bookingService != null) ? bookingService.getBankBin() : "970422";
                    String accNo = (bookingService != null) ? bookingService.getAccountNo() : "0903123456";
                    String accName = (bookingService != null) ? bookingService.getAccountName() : "UTE BADMINTON CLUB";
                    String bName = (bookingService != null) ? bookingService.getBankName() : "MBBank (Ngân hàng Quân Đội)";

                    String qrUrl = String.format(
                            "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=CK%%20%s&accountName=%s",
                            bin, accNo, dep.longValue(), b.getBookingCode(),
                            java.net.URLEncoder.encode(accName, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20")
                    );

                    Map<String, Object> paymentData = new HashMap<>();
                    paymentData.put("bookingCode", b.getBookingCode());
                    paymentData.put("courtName", b.getCourtCode());
                    paymentData.put("branchName", b.getBranchCode());
                    paymentData.put("bookingDate", b.getBookingDate() != null ? b.getBookingDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
                    paymentData.put("timeSlot", b.getTimeSlot());
                    paymentData.put("additionalServices", b.getNotes());
                    paymentData.put("totalPrice", b.getTotalPrice());
                    paymentData.put("depositAmount", dep);
                    paymentData.put("qrUrl", qrUrl);
                    paymentData.put("bankName", bName);
                    paymentData.put("accountNo", accNo);
                    paymentData.put("accountName", accName);
                    paymentData.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : LocalDateTime.now().toString());

                    String reopenText = String.format(
                            "Hệ thống đã mở lại mã khóa sân và giao diện thanh toán tiền cọc cho đơn **%s** của bạn!\n\n" +
                            "• **Mã khóa sân**: **%s**\n" +
                            "• **Khách hàng**: %s (%s)\n" +
                            "• **Sân**: %s (Cơ sở: %s)\n" +
                            "• **Khung giờ**: %s (%s)\n" +
                            "• **Tổng tiền**: %,d đ\n" +
                            "• **Tiền cọc cần thanh toán (30%%)**: **%,d đ**\n\n" +
                            "Hệ thống đang tạm khóa giữ chỗ sân trong 10 phút. Đơn này cũng đã được lưu trong [Lịch Sử Đặt Sân](/customer/history). Bạn vui lòng quét mã VietQR bên dưới hoặc chuyển khoản để hoàn tất.",
                            b.getBookingCode(), b.getBookingCode(),
                            b.getCustomerName() != null ? b.getCustomerName() : "Khách hàng",
                            b.getCustomerPhone() != null ? b.getCustomerPhone() : "",
                            b.getCourtCode(), b.getBranchCode(),
                            b.getTimeSlot(), b.getBookingDate() != null ? b.getBookingDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "",
                            b.getTotalPrice() != null ? b.getTotalPrice().longValue() : 0,
                            dep.longValue()
                    );

                    return ChatMessageDto.builder()
                            .sender("bot")
                            .intent("OPEN_PAYMENT")
                            .text(reopenText)
                            .actionType("OPEN_PAYMENT")
                            .paymentData(paymentData)
                            .quickReplies(List.of("Xác nhận đã chuyển khoản cọc", "Lịch sử đặt sân của tôi", "Hướng dẫn hủy ca chơi"))
                            .timestamp(LocalDateTime.now())
                            .build();
                }
            }
        }

        // =========================================================================
        // INTENT: TRA CỨU ĐƠN ĐẶT SÂN
        // =========================================================================
        if (!normalized.contains("huy") && ("CHECK_BOOKING".equals(mlTag) || matches(normalized, "kiem tra don", "tra cuu don", "xem don", "don cua toi", "ma don", "thong tin don", "xem lai thong tin don", "xem lai don", "don dat san", "lich su dat", "lich su don", "don hang cua toi"))) {
            String targetCode = null;
            Pattern codePattern = Pattern.compile("(?:ds\\s*)?(\\d{6})", Pattern.CASE_INSENSITIVE);
            Matcher mCode = codePattern.matcher(normalized);
            if (mCode.find()) {
                targetCode = "DS" + mCode.group(1);
            } else if (ctx.lastBookingCode != null) {
                targetCode = ctx.lastBookingCode;
            } else if (ctx.customerPhone != null && !ctx.customerPhone.isBlank()) {
                List<Booking> userBookings = bookingRepository.findByCustomerPhoneOrderByBookingDateDesc(ctx.customerPhone);
                if (userBookings != null && !userBookings.isEmpty()) {
                    targetCode = userBookings.get(0).getBookingCode();
                }
            } else if (ctx.customerCode != null && !ctx.customerCode.isBlank()) {
                List<Booking> userBookings = bookingRepository.findByCustomerCodeOrderByBookingDateDesc(ctx.customerCode);
                if (userBookings != null && !userBookings.isEmpty()) {
                    targetCode = userBookings.get(0).getBookingCode();
                }
            }

            if (targetCode != null) {
                Optional<Booking> optBooking = bookingRepository.findByBookingCode(targetCode);
                if (optBooking.isPresent()) {
                    Booking b = optBooking.get();
                    String extraMsg = (b.getStatus() != null && b.getStatus().contains("Chờ cọc"))
                        ? "Đơn của bạn đang trong trạng thái chờ cọc 30%. Bạn có thể chuyển khoản qua mã VietQR để hoàn tất."
                        : "Đơn của bạn đã được xác nhận an toàn trên hệ thống. Bạn có thể xem toàn bộ đơn tại [Lịch Sử Đặt Sân](/customer/history).";

                    String reply = String.format(
                            "Thông tin chi tiết đơn đặt sân của bạn:\n\n" +
                            "• Mã đơn: **%s**\n" +
                            "• Cơ sở: %s\n" +
                            "• Sân: %s\n" +
                            "• Khách hàng: %s (%s)\n" +
                            "• Ngày chơi: %s\n" +
                            "• Khung giờ: %s\n" +
                            "• Tổng tiền: %,d đ\n" +
                            "• Tiền cọc đã tính (30%%): %,d đ\n" +
                            "• Trạng thái đơn: **%s**\n\n%s",
                            b.getBookingCode(), b.getBranchCode(), b.getCourtCode(),
                            b.getCustomerName() != null ? b.getCustomerName() : "Khách hàng",
                            b.getCustomerPhone() != null ? b.getCustomerPhone() : "",
                            b.getBookingDate() != null ? b.getBookingDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Hôm nay",
                            b.getTimeSlot(),
                            b.getTotalPrice() != null ? b.getTotalPrice().longValue() : 0,
                            b.getDepositAmount() != null ? b.getDepositAmount().longValue() : 0,
                            b.getStatus(),
                            extraMsg
                    );

                    Map<String, Object> lookupData = new HashMap<>();
                    lookupData.put("bookingCode", b.getBookingCode());
                    lookupData.put("courtCode", b.getCourtCode());
                    lookupData.put("branchCode", b.getBranchCode());
                    lookupData.put("date", b.getBookingDate() != null ? b.getBookingDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
                    lookupData.put("timeSlot", b.getTimeSlot());
                    lookupData.put("status", b.getStatus());
                    lookupData.put("totalPrice", b.getTotalPrice());
                    lookupData.put("depositAmount", b.getDepositAmount());

                    return ChatMessageDto.builder()
                            .sender("bot")
                            .intent("LOOKUP_RESULT")
                            .text(reply)
                            .actionType("LOOKUP_RESULT")
                            .lookupData(lookupData)
                            .quickReplies(List.of("Lịch sử đặt sân của tôi", "Hướng dẫn hủy ca chơi", "Bảng giá thuê sân"))
                            .timestamp(LocalDateTime.now())
                            .build();
                }
            }

            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("LOOKUP_PROMPT")
                    .text("Để tra cứu thông tin đơn, bạn vui lòng cung cấp mã đơn đặt sân (ví dụ: DS123456) hoặc kiểm tra lại lịch sử đơn hàng trên trang cá nhân.")
                    .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Các chi nhánh của bạn", "Bảng giá thuê sân"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT: YÊU CẦU HỦY ĐẶT SÂN
        // =========================================================================
        if ("CANCEL_BOOKING".equals(mlTag) || matches(normalized, "huy san", "huy don", "muon huy", "khong den duoc", "huy ca choi", "yeu cau huy")) {
            String targetCode = null;
            Pattern codePattern = Pattern.compile("(?:ds\\s*)?(\\d{6})", Pattern.CASE_INSENSITIVE);
            Matcher mCode = codePattern.matcher(normalized);
            if (mCode.find()) {
                targetCode = "DS" + mCode.group(1);
            } else if (ctx.lastBookingCode != null) {
                targetCode = ctx.lastBookingCode;
            }

            if (targetCode != null) {
                Optional<Booking> optBooking = bookingService.getBookingByCode(targetCode);
                if (optBooking.isEmpty()) {
                    return ChatMessageDto.builder()
                            .sender("bot")
                            .intent("CANCEL_ERROR")
                            .text("Không tìm thấy đơn đặt sân " + targetCode + " trong hệ thống. Bạn vui lòng kiểm tra lại mã đơn.")
                            .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân"))
                            .actionType("NONE")
                            .timestamp(LocalDateTime.now())
                            .build();
                }

                Booking b = optBooking.get();
                boolean isSessionOwner = ctx.lastBookingCode != null && ctx.lastBookingCode.equalsIgnoreCase(targetCode);
                if (!isSessionOwner) {
                    String bPhone = b.getCustomerPhone() != null ? b.getCustomerPhone().replaceAll("\\D", "") : "";
                    boolean phoneMatched = false;
                    if (!bPhone.isEmpty() && bPhone.length() >= 4) {
                        String last4 = bPhone.substring(bPhone.length() - 4);
                        if (rawInput.contains(bPhone) || rawInput.contains(last4)) {
                            phoneMatched = true;
                        }
                    }
                    if (!phoneMatched) {
                        return ChatMessageDto.builder()
                                .sender("bot")
                                .intent("CANCEL_VERIFY")
                                .text("Vì lý do bảo mật, để hủy đơn " + targetCode + ", bạn vui lòng cung cấp kèm số điện thoại đã dùng để đặt sân (Ví dụ: 'Hủy đơn " + targetCode + " SĐT " + (bPhone.length() >= 4 ? "***" + bPhone.substring(bPhone.length() - 3) : "") + "').")
                                .quickReplies(List.of("Tra cứu đơn " + targetCode, "Bảng giá thuê sân"))
                                .actionType("NONE")
                                .timestamp(LocalDateTime.now())
                                .build();
                    }
                }

                try {
                    Booking cancelled = bookingService.cancelBooking(targetCode, "Khách yêu cầu hủy qua Chatbot");
                    String reply = String.format(
                            "Đơn đặt sân mã %s đã được hủy thành công.\n\n" +
                            "Hệ thống đã tự động giải phóng %s tại %s để khách hàng khác có thể đặt.\n\n" +
                            "Theo chính sách của UTE Sport, nếu ca chơi được hủy trước giờ bắt đầu trên 4 tiếng, 100%% số tiền cọc sẽ được hoàn lại vào tài khoản của quý khách.",
                            targetCode, cancelled.getCourtCode(), cancelled.getBranchCode()
                    );
                    return ChatMessageDto.builder()
                            .sender("bot")
                            .intent("CANCEL_SUCCESS")
                            .text(reply)
                            .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Chi nhánh nào gần tôi?", "Bảng giá thuê sân"))
                            .actionType("NONE")
                            .timestamp(LocalDateTime.now())
                            .build();
                } catch (Exception e) {
                    return ChatMessageDto.builder()
                            .sender("bot")
                            .intent("CANCEL_ERROR")
                            .text("Không thể hủy đơn đặt sân " + targetCode + " do mã đơn không tồn tại hoặc đơn đã hoàn thành. Bạn vui lòng liên hệ hotline 1900 6868 để được nhân viên hỗ trợ trực tiếp.")
                            .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân"))
                            .actionType("NONE")
                            .timestamp(LocalDateTime.now())
                            .build();
                }
            }

            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("CANCEL_PROMPT")
                    .text("Để hủy đơn đặt sân, bạn vui lòng nhắn kèm mã đơn (ví dụ: 'Hủy đơn DS123456'). Lưu ý: Đơn hủy trước giờ đánh trên 4 tiếng sẽ được hoàn 100% tiền cọc.")
                    .quickReplies(List.of("Quy định cọc và hủy sân", "Bảng giá thuê sân", "Chi nhánh nào gần tôi?"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT: HỎI CHI NHÁNH GẦN TÔI
        // =========================================================================
        if (matches(normalized, "gan toi", "gan nhat", "o dia chi", "o duong", "o khu vuc", "cach bao xa", "o gan", "dia chi nao gan")
                || (normalized.startsWith("toi o ") && !normalized.contains("thue") && !normalized.contains("dat"))) {
            String recommendedBranchCode = "CN01";
            String recommendedBranchName = "Chi nhánh 1 (Thủ Đức - Số 1 Võ Văn Ngân)";
            String distNote = "khoảng 1.5 - 2.5 km";

            if (matches(normalized, "quan 9", "q9", "le van viet", "do xuan hop", "tang nhon phu", "man thien", "hiep phu", "phuoc long", "vinhome", "nguyen duy trinh", "xa lo ha noi", "khu cong nghe cao", "cong nghe cao", "quan 2", "q2", "thu thiem", "an phu")) {
                recommendedBranchCode = "CN03";
                recommendedBranchName = "Chi nhánh 3 (Quận 9 - Số 70 Đỗ Xuân Hợp)";
                distNote = "khoảng 1.0 - 2.0 km";
            } else if (matches(normalized, "binh thanh", "hang xanh", "dien bien phu", "d2", "nguyen gia tri", "xo viet nghe tinh", "bach dang", "dinh bo linh", "cau sai gon", "thi nghe", "quan 1", "q1", "quan 3", "q3", "phu nhuan")) {
                recommendedBranchCode = "CN02";
                recommendedBranchName = "Chi nhánh 2 (Bình Thạnh - Số 234 Điện Biên Phủ)";
                distNote = "khoảng 1.2 - 2.8 km";
            } else if (matches(normalized, "go vap", "quang trung", "phan van tri", "nguyen oanh", "le duc tho", "nguyen kiem", "cay tram", "thong nhat", "pham van chieu", "tan binh", "tan phu", "quan 12", "q12", "hoc mon")) {
                recommendedBranchCode = "CN04";
                recommendedBranchName = "Chi nhánh 4 (Gò Vấp - Số 18 Quang Trung)";
                distNote = "khoảng 1.5 - 3.0 km";
            } else {
                recommendedBranchCode = "CN01";
                recommendedBranchName = "Chi nhánh 1 (Thủ Đức - Số 1 Võ Văn Ngân)";
                distNote = "khoảng 1.5 - 2.5 km";
            }

            ctx.branchCode = recommendedBranchCode;
            ctx.branchName = recommendedBranchName;

            String reply = String.format(
                    "Dựa trên vị trí của bạn, **%s** là cơ sở gần bạn nhất (%s).\n\n" +
                    "Cơ sở này được trang bị thảm Yonex BWF tiêu chuẩn thi đấu, đèn chống chói 500 Lux và bãi giữ xe ô tô/xe máy rộng rãi miễn phí.\n\n" +
                    "Hôm nay bạn dự định chơi vào lúc mấy giờ để tôi kiểm tra sân trống cho bạn?",
                    recommendedBranchName, distNote
            );

            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("RECOMMEND_BRANCH")
                    .text(reply)
                    .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Tối nay từ 19h đến 21h", "Chiều nay lúc 17h"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT: DANH SÁCH CHI NHÁNH
        // =========================================================================
        if ("FIND_BRANCH".equals(mlTag) || matches(normalized, "chi nhanh nao", "cac chi nhanh", "danh sach chi nhanh", "co may chi nhanh", "co nhung co so nao", "he thong o dau")) {
            List<Branch> branches = branchRepository != null ? branchRepository.findAll() : Collections.emptyList();
            StringBuilder sb = new StringBuilder("Hệ thống sân cầu lông UTE Sport hiện có các cơ sở thi đấu sau:\n\n");
            int idx = 1;
            for (Branch b : branches) {
                sb.append(idx++).append(". ").append(b.getBranchName()).append("\n")
                  .append("   Địa chỉ: ").append(b.getAddress() != null ? b.getAddress() : "TP. Hồ Chí Minh").append("\n")
                  .append("   Hotline: ").append(b.getPhone() != null ? b.getPhone() : "1900 6868").append("\n\n");
            }
            sb.append("Giờ mở cửa toàn hệ thống: 05:30 - 22:30 mỗi ngày.\n\n")
              .append("Bạn đang ở địa chỉ hoặc khu vực nào để tôi tư vấn cơ sở thuận tiện nhất cho bạn?");

            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("BRANCH_LIST")
                    .text(sb.toString())
                    .quickReplies(List.of("Tôi ở đường Hoàng Diệu 2 thì cơ sở nào gần tôi?", "Tôi ở Hàng Xanh Bình Thạnh", "Tôi ở Lê Văn Việt Quận 9", "Tôi ở đường Quang Trung Gò Vấp"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT: BẢNG GIÁ
        // =========================================================================
        if ("PRICING_INFO".equals(mlTag) || matches(normalized, "bang gia", "bao nhieu tien", "chi phi", "gio vang", "don gia", "gia thue san")) {
            String pricingText =
                    "Biểu phí thuê sân cầu lông tiêu chuẩn tại UTE Sport:\n\n" +
                    "1. Khung giờ sáng (05:30 - 14:00): 80.000 đ/giờ\n" +
                    "2. Khung giờ chiều (14:00 - 17:00): 100.000 đ/giờ\n" +
                    "3. Khung Giờ Vàng (17:00 - 21:00): 140.000 đ/giờ (Thảm Yonex BWF cao cấp)\n" +
                    "4. Khung giờ tối muộn (21:00 - 22:30): 90.000 đ/giờ\n" +
                    "5. Cuối tuần (Thứ 7, Chủ Nhật): Phụ thu 15.000 đ/giờ.\n\n" +
                    "Thành viên VIP tích điểm được chiết khấu thêm từ 5% đến 15% trên mỗi hóa đơn.\n\n" +
                    "Bạn muốn tìm sân vào khung giờ nào?";

            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("PRICING")
                    .text(pricingText)
                    .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Chi nhánh nào gần tôi?", "Quy định cọc tiền sân"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT: TIỆN ÍCH & CƠ SỞ VẬT CHẤT
        // =========================================================================
        if ("COURT_FACILITIES".equals(mlTag) || matches(normalized, "co so vat chat", "loai tham", "tham yonex", "tham enlio", "may lanh", "quat", "dau xe", "do xe", "bai xe", "phong tam", "phong thay do", "wifi", "anh sang", "chieu cao", "khan dai")) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("COURT_FACILITIES")
                    .text("Cơ sở vật chất & Tiện ích tại Chuỗi Sân Cầu Lông UTE Sport:\n\n" +
                          "• Mặt sân thi đấu: 100% thảm Yonex & Enlio dày 5.0mm đạt chuẩn quốc tế BWF, độ bám cao chống trơn trượt và giảm chấn khớp gối tối đa.\n" +
                          "• Không gian & Ánh sáng: Hệ thống đèn LED rọi chống chói 500 Lux, trần cao 9.5m thông thoáng, quạt làm mát công nghiệp mát mẻ.\n" +
                          "• Bãi đỗ xe: Bãi đỗ ô tô và xe máy rộng rãi ngay trong khuôn viên có bảo vệ túc trực và camera an ninh 24/7.\n" +
                          "• Tiện ích miễn phí: Phòng tắm nóng lạnh & phòng thay đồ riêng biệt nam/nữ, Wifi cáp quang tốc độ cao phủ kín sân.")
                    .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân", "Chi nhánh nào gần tôi?"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT: GIẢI ĐẤU (TOURNAMENTS)
        // =========================================================================
        if ("TOURNAMENTS".equals(mlTag) || matches(normalized, "giai dau", "giai cau long", "co giai nao", "lich thi dau", "dang ky giai", "tournament")) {
            List<Tournament> tournaments = tournamentRepository != null ? tournamentRepository.findAll() : Collections.emptyList();
            StringBuilder sb = new StringBuilder("Hệ thống giải đấu cầu lông đang mở đăng ký tại UTE Sport:\n\n");

            List<Map<String, Object>> tourList = new ArrayList<>();
            int idx = 1;
            for (Tournament t : tournaments) {
                sb.append(idx++).append(". ").append(t.getTournamentName()).append("\n")
                  .append("   • Tổng giải thưởng: ").append(t.getTotalPrize() != null ? String.format("%,d đ", t.getTotalPrize().longValue()) : "Hiện kim + Cúp").append("\n")
                  .append("   • Thời gian thi đấu: ").append(t.getStartDate() != null ? t.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Sắp diễn ra").append("\n")
                  .append("   • Trạng thái: ").append(t.getStatus() != null ? t.getStatus() : "Đang mở đăng ký").append("\n\n");

                Map<String, Object> tMap = new HashMap<>();
                tMap.put("name", t.getTournamentName());
                tMap.put("prize", t.getTotalPrize());
                tMap.put("status", t.getStatus());
                tourList.add(tMap);
            }

            sb.append("Bạn có thể truy cập mục 'Giải Đấu' trên thanh điều hướng để đăng ký tham gia thi đấu và theo dõi bảng điểm trực tiếp.");

            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("TOURNAMENTS")
                    .text(sb.toString())
                    .actionUrl("/customer/tournaments")
                    .actionText("Xem chi tiết giải đấu")
                    .actionType("TOURNAMENT_LIST")
                    .tournamentData(tourList)
                    .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân", "Chi nhánh nào gần tôi?"))
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT: DỊCH VỤ ĐI KÈM & THUÊ DỤNG CỤ THỂ THAO
        // =========================================================================
        if (("SERVICES_EQUIPMENT".equals(mlTag) || matches(normalized, "thue vot", "co vot khong", "ban cau", "mua cau", "dan vot", "cang vot", "nuoc uong", "dich vu", "phu kien"))
                && !isBookingFlow(normalized, ctx)) {
            String serviceInfo =
                    "Danh mục dịch vụ và thiết bị thể thao tại các cơ sở UTE Sport:\n\n" +
                    "1. Cho thuê vợt thi đấu:\n" +
                    "   • Vợt Yonex Astrox 88D Pro / 100ZZ: 50.000 đ/ca\n" +
                    "   • Vợt Lining Axforce 80 / Halbertec: 50.000 đ/ca\n" +
                    "   • Thuê giày cầu lông chuyên dụng (đế kếp): 20.000 đ/đôi\n\n" +
                    "2. Quả cầu lông chính hãng:\n" +
                    "   • Cầu Yonex Aerosensa AS-40: 420.000 đ/ống\n" +
                    "   • Cầu Thành Công 77: 260.000 đ/ống\n" +
                    "   • Cầu VinaStar: 250.000 đ/ống\n\n" +
                    "3. Dịch vụ đan vợt điện tử chuẩn 4 nút Yonex:\n" +
                    "   • Cước Yonex BG65: 120.000 đ (gồm công)\n" +
                    "   • Cước Yonex BG66 Ultimax: 160.000 đ (gồm công)\n" +
                    "   • Tiền công đan (khách mang cước): 50.000 đ\n\n" +
                    "4. Tiện ích miễn phí: Bãi đỗ ô tô/xe máy có bảo vệ, phòng tắm nóng lạnh, nước lọc tại quầy.";

            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("SERVICES")
                    .text(serviceInfo)
                    .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân", "Chi nhánh nào gần tôi?"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT: HỘI VIÊN & ĐIỂM THƯỞNG
        // =========================================================================
        if (("MEMBERSHIP_LOYALTY".equals(mlTag) || matches(normalized, "tich diem", "the hoi vien", "hang thanh vien", "diem thuong", "doi voucher", "dong bac vang", "kim cuong", "thanh vien vip", "hoi vien vip", "the vip", "uu dai"))
                && !isBookingFlow(normalized, ctx)) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("MEMBERSHIP_LOYALTY")
                    .text("Chính sách Hội viên & Điểm thưởng UTE Sport Rewards:\n\n" +
                          "• Cơ chế tích lũy: Mỗi 1.000 đ chi tiêu thuê sân hoặc mua nước/dụng cụ được tích ngay 1 điểm thưởng.\n" +
                          "• Các hạng thẻ thành viên:\n" +
                          "   1. Hạng Đồng (Mới đăng ký): Tận hưởng dịch vụ tiêu chuẩn.\n" +
                          "   2. Hạng Bạc (Từ 1.000 điểm): Giảm 5% tổng hóa đơn.\n" +
                          "   3. Hạng Vàng (Từ 3.000 điểm): Giảm 10% tiền sân + miễn phí thuê vợt thi đấu.\n" +
                          "   4. Hạng Kim Cương (Từ 5.000 điểm): Giảm 15% tiền sân + tặng 1 ống cầu Yonex mỗi tháng.\n\n" +
                          "Điểm thưởng có thể dùng cấn trừ trực tiếp khi thanh toán tiền sân hoặc đổi voucher giảm giá trên cổng khách hàng.")
                    .quickReplies(List.of("Bảng giá thuê sân", "Hôm nay có sân nào trống từ 18h đến 20h không?", "Hướng dẫn đặt sân"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT: PHƯƠNG THỨC THANH TOÁN
        // =========================================================================
        if ("PAYMENT_METHODS".equals(mlTag) || matches(normalized, "thanh toan the nao", "phuong thuc thanh toan", "chuyen khoan", "quet ma qr", "vietqr", "tien mat", "hoa don do", "vat")) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("PAYMENT_METHODS")
                    .text("Phương thức thanh toán & Hóa đơn tại UTE Sport:\n\n" +
                          "1. Đặt sân trực tuyến: Quét mã VietQR chuyển khoản nhanh 24/7 qua mọi ứng dụng ngân hàng hoặc ví điện tử (MoMo, ZaloPay). Hệ thống tự động khóa giữ sân sau 3 giây.\n" +
                          "2. Mức cọc: Quý khách cọc trước 30% giá trị ca chơi khi đặt online, 70% còn lại thanh toán tại quầy khi nhận sân.\n" +
                          "3. Thanh toán tại quầy POS: Hỗ trợ tiền mặt, chuyển khoản VietQR, quẹt thẻ ngân hàng (Visa/Master/Napas).\n" +
                          "4. Xuất hóa đơn VAT: Hỗ trợ xuất hóa đơn điện tử đỏ VAT cho doanh nghiệp hoặc câu lạc bộ có nhu cầu.")
                    .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân", "Chi nhánh nào gần tôi?"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT: NỘI QUY SÂN
        // =========================================================================
        if ("RULES_REGULATIONS".equals(mlTag) || matches(normalized, "noi quy", "quy dinh", "giay de kep", "giay non marking", "di tre", "mang thuc an", "hut thuoc")) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("RULES_REGULATIONS")
                    .text("Nội quy sân đấu tại Chuỗi Sân UTE Sport:\n\n" +
                          "1. Giày thi đấu: Bắt buộc mang giày cầu lông chuyên dụng đế kếp (Non-marking) không để lại vệt đen lên thảm BWF.\n" +
                          "2. Giờ giấc thi đấu: Quý khách vui lòng đến trước 10 phút để khởi động và nhận sân. Nếu đi trễ quá 15 phút mà không thông báo trước cho lễ tân, hệ thống có quyền nhượng slot cho khách chờ tiếp theo.\n" +
                          "3. Giữ gìn vệ sinh: Không hút thuốc, không nhai kẹo cao su hoặc mang thức ăn có dầu mỡ vào khu vực thảm thi đấu.")
                    .quickReplies(List.of("Bảng giá thuê sân", "Cho thuê vợt và giày", "Hôm nay có sân nào trống từ 18h đến 20h không?"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT: ĐỒ THẤT LẠC & HỖ TRỢ
        // =========================================================================
        if ("LOST_FOUND_SUPPORT".equals(mlTag) || matches(normalized, "quen do", "that lac", "mat do", "quen ao", "quen vot", "hotline", "so dien thoai", "khieu nai", "gap nhan vien")) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("LOST_FOUND_SUPPORT")
                    .text("Hỗ trợ khách hàng & Xử lý đồ thất lạc tại UTE Sport:\n\n" +
                          "• Tìm đồ bỏ quên: Toàn bộ đồ đạc khách để quên tại sân (vợt, giày, áo khoác, bình nước, chìa khóa...) đều được ban quản lý ca trực thu gom và niêm phong trong tủ đồ thất lạc.\n" +
                          "• Cách nhận lại đồ: Vui lòng liên hệ hotline cơ sở hoặc đến quầy lễ tân chi nhánh cung cấp ngày giờ chơi và mô tả vật phẩm để nhận lại ngay.\n" +
                          "• Hotline tổng đài 24/7: 1900 6868 hoặc 0903.123.456\n" +
                          "• Hòm thư khiếu nại dịch vụ: cskh@utesport.vn (Cam kết phản hồi trong 2 giờ).")
                    .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân", "Chi nhánh nào gần tôi?"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // =========================================================================
        // LUỒNG ĐẶT VÀ KHÓA SÂN THÔNG MINH (MULTI-TURN SLOT-FILLING BOOKING ENGINE)
        // Bắt buộc đảm bảo đủ 6 thông tin:
        // 1. Tên khách hàng
        // 2. Số điện thoại
        // 3. Chi nhánh
        // 4. Đặt sân gì
        // 5. Mấy giờ & ngày nào
        // 6. Có chọn thêm gì đi kèm không
        // RỒI MỚI CHO HIỆN MÃ KHÓA SÂN!
        // =========================================================================
        // =========================================================================

        boolean isBookingIntent = isBookingFlow(normalized, ctx) || "CONFIRM_BOOKING".equals(mlTag) || "SEARCH_SLOTS".equals(mlTag) || "REQUEST_LOCK_CODE".equals(mlTag) || "SELECT_ADDONS".equals(mlTag) || "PROVIDE_CUSTOMER_INFO".equals(mlTag);

        // Trường hợp người dùng mới chỉ nói "tôi muốn đặt sân" mà chưa có bất kỳ thông tin nào
        boolean isSimpleBookingPrompt = matches(normalized, "toi muon dat san", "muon dat san", "dat san di", "cho toi dat san") && !ctx.isBranchProvided() && !ctx.isSlotProvided();
        if (isSimpleBookingPrompt) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("BOOKING_PROMPT")
                    .text("Dạ, bạn muốn đặt và tự khóa sân vào khung giờ nào, ngày nào và tại chi nhánh nào?\n\n" +
                          "Hệ thống UTE Sport hỗ trợ **tự động giữ chỗ và khóa sân tức thì trong 10 phút** khi tạo đơn. Sau khi xác nhận cọc 30%, sân sẽ được khóa cố định chính thức và đơn lưu vào [Lịch Sử Đặt Sân](/customer/history) của bạn.\n\n" +
                          "Bạn có thể nhắn nhanh khung giờ muốn chơi (ví dụ: 'Đặt sân từ 18h đến 20h tối nay tại chi nhánh 1').")
                    .quickReplies(List.of("Hôm nay 18h - 20h cơ sở 1", "Hôm nay 19h - 21h cơ sở 2", "Ngày mai 17h - 19h cơ sở 1", "Bảng giá thuê sân"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        if (isBookingIntent) {
            // Cập nhật giá nếu đã có giờ và sân
            recalculatePrices(ctx);

            // Kiểm tra các thông tin bắt buộc
            boolean hasName = ctx.isNameProvided();
            boolean hasPhone = ctx.isPhoneProvided();
            boolean hasBranch = ctx.isBranchProvided();
            boolean hasSlot = ctx.isSlotProvided();
            boolean hasCourt = ctx.isCourtProvided();
            boolean hasAddOns = ctx.isAddOnsProvided();

            boolean hasAll6 = hasName && hasPhone && hasBranch && hasSlot && hasCourt && hasAddOns;

            boolean userWantsToLock = matches(normalized, "ok dat san", "dat san cho toi", "dat luon", "dong y dat", "xac nhan dat", "chot san", "lay san nay", "khoa san cho toi", "khoa san nay", "hien ma khoa san", "cho ma khoa san", "xuat ma khoa san", "lay ma khoa san", "khoa san gium", "xac nhan va xuat ma khoa san", "xac nhan");

            // =====================================================================
            // CASE 1: ĐÃ ĐỦ CẢ 6 THÔNG TIN VÀ XÁC NHẬN CHỐT -> CHO HIỆN MÃ KHÓA SÂN
            // =====================================================================
            if (hasAll6 && (userWantsToLock || "AWAITING_CONFIRMATION".equals(ctx.stage))) {
                return executeCourtLocking(ctx);
            }

            // =====================================================================
            // CASE 2: ĐÃ ĐỦ CẢ 6 THÔNG TIN NHƯNG CHƯA BẤM CHỐT -> TÓM TẮT & XIN XÁC NHẬN
            // =====================================================================
            if (hasAll6) {
                ctx.stage = "AWAITING_CONFIRMATION";
                String summary = String.format(
                        "Tuyệt vời! Tôi đã tổng hợp đầy đủ các thông tin cần thiết cho đơn đặt sân của bạn:\n\n" +
                        "📋 **PHIẾU THÔNG TIN ĐẶT VÀ TỰ KHÓA SÂN GIỮ CHỖ**\n" +
                        "• **Họ tên khách hàng**: **%s**\n" +
                        "• **Số điện thoại**: **%s**\n" +
                        "• **Chi nhánh thi đấu**: **%s**\n" +
                        "• **Sân đặt**: **%s**\n" +
                        "• **Thời gian thi đấu**: **%s** ngày **%s** (%d tiếng)\n" +
                        "• **Dịch vụ đi kèm**: %s\n" +
                        "• **Tiền thuê sân**: %,d đ\n" +
                        "• **Tiền phụ kiện / dịch vụ**: %,d đ\n" +
                        "• **TỔNG CHI PHÍ**: **%,d đ**\n" +
                        "• **TIỀN CỌC GIỮ SÂN (30%%)**: **%,d đ**\n\n" +
                        "Thông tin đã chính xác rồi chứ bạn? Bạn bấm **'Xác nhận và xuất mã khóa sân'** bên dưới để hệ thống kích hoạt tự động khóa giữ chỗ sân trong 10 phút nhé!",
                        ctx.customerName, ctx.customerPhone, ctx.branchName, ctx.courtName,
                        ctx.timeSlot, ctx.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), ctx.durationHours,
                        ctx.additionalServices, ctx.courtPrice.longValue(), ctx.additionalFee.longValue(),
                        ctx.totalPrice.longValue(), ctx.depositAmount.longValue()
                );

                return ChatMessageDto.builder()
                        .sender("bot")
                        .intent("AWAITING_CONFIRMATION")
                        .text(summary)
                        .quickReplies(List.of("Xác nhận và xuất mã khóa sân", "Tôi muốn đổi khung giờ", "Thêm phụ kiện đi kèm", "Bắt đầu lại"))
                        .actionType("NONE")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            // =====================================================================
            // CASE 3: THIẾU THÔNG TIN -> HỎI THEO THỨ TỰ LOGIC CHẶT CHẼ
            // =====================================================================

            // 1. Thiếu Chi nhánh
            if (!hasBranch) {
                ctx.stage = "COLLECTING_INFO";
                String promptText = "Dạ, để đặt và giữ chỗ sân, bạn muốn chơi tại **chi nhánh nào** trong hệ thống UTE Sport ạ?" +
                        (hasSlot ? String.format(" (Tôi đã ghi nhận khung giờ: %s ngày %s)", ctx.timeSlot, ctx.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) : "");
                return ChatMessageDto.builder()
                        .sender("bot")
                        .intent("ASK_BRANCH")
                        .text(promptText)
                        .quickReplies(List.of("Chi nhánh 1 (Thủ Đức)", "Chi nhánh 2 (Bình Thạnh)", "Chi nhánh 3 (Quận 9)", "Chi nhánh 4 (Gò Vấp)"))
                        .actionType("NONE")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            // 2. Thiếu Khung giờ & Ngày
            if (!hasSlot) {
                ctx.stage = "COLLECTING_INFO";
                String promptText = String.format("Bạn dự định chơi tại **%s** vào **ngày nào** và **từ mấy giờ đến mấy giờ** ạ?", ctx.branchName);
                return ChatMessageDto.builder()
                        .sender("bot")
                        .intent("ASK_SLOT")
                        .text(promptText)
                        .quickReplies(List.of("Hôm nay 18h - 20h", "Hôm nay 19h - 21h", "Hôm nay 17h - 19h", "Tối mai 18h - 20h"))
                        .actionType("NONE")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            // 3. Thiếu Sân gì (đã có Chi nhánh và Giờ)
            if (!hasCourt) {
                ctx.stage = "PROPOSING_COURT";
                List<Court> branchCourts = (courtRepository != null && ctx.branchCode != null) ? courtRepository.findByBranchCode(ctx.branchCode) : Collections.emptyList();
                if (branchCourts == null) branchCourts = Collections.emptyList();
                List<Court> availableCourts = branchCourts.stream()
                        .filter(c -> !"Bảo trì".equalsIgnoreCase(c.getStatus()))
                        .filter(c -> {
                            if (bookingRepository == null || bookingService == null) return true;
                            List<Booking> dayBookings = bookingRepository.findByCourtCodeAndBookingDate(c.getCourtCode(), ctx.date);
                            if (dayBookings == null) dayBookings = Collections.emptyList();
                            return dayBookings.stream().noneMatch(b ->
                                    b.getStatus() != null && !"Đã hủy".equalsIgnoreCase(b.getStatus())
                                    && bookingService.isSlotOverlapping(ctx.timeSlot, b.getTimeSlot())
                            );
                        })
                        .toList();

                if (availableCourts.isEmpty()) {
                    return ChatMessageDto.builder()
                            .sender("bot")
                            .intent("COURT_UNAVAILABLE")
                            .text(String.format("Rất tiếc, vào ngày %s trong khung giờ %s tại %s, tất cả các sân đều đã có khách đặt trước hoặc đang bảo trì.\n\nBạn có muốn đổi sang khung giờ khác (ví dụ trước 17h hoặc sau 20h) hoặc tham khảo cơ sở khác không?",
                                    ctx.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), ctx.timeSlot, ctx.branchName))
                            .quickReplies(List.of("Hôm nay có sân nào trống từ 14h đến 16h không?", "Hôm nay có sân nào trống từ 20h đến 22h không?", "Chi nhánh khác còn sân không?"))
                            .actionType("NONE")
                            .timestamp(LocalDateTime.now())
                            .build();
                }

                // Nếu người dùng có chọn loại sân VIP hay Standard
                Optional<Court> matchedCourt = Optional.empty();
                if ("VIP".equalsIgnoreCase(ctx.courtType)) {
                    matchedCourt = availableCourts.stream().filter(c -> c.getCourtName().toUpperCase().contains("VIP") || c.getCourtType().toUpperCase().contains("VIP")).findFirst();
                } else if ("STANDARD".equalsIgnoreCase(ctx.courtType)) {
                    matchedCourt = availableCourts.stream().filter(c -> !c.getCourtName().toUpperCase().contains("VIP")).findFirst();
                }

                if (matchedCourt.isPresent()) {
                    Court c = matchedCourt.get();
                    ctx.courtCode = c.getCourtCode();
                    ctx.courtName = c.getCourtName();
                    recalculatePrices(ctx);
                } else {
                    StringBuilder courtOptionsText = new StringBuilder();
                    courtOptionsText.append(String.format("Trong khung giờ **%s** ngày **%s** tại **%s**, hệ thống đang còn các sân sau sẵn sàng phục vụ:\n\n",
                            ctx.timeSlot, ctx.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), ctx.branchName));

                    List<String> replies = new ArrayList<>();
                    for (Court c : availableCourts) {
                        BigDecimal rate = (pricingService != null) ? pricingService.calculateHourlyRate(c, extractStartHour(ctx.timeSlot)) : (c.getHourlyRate() != null ? c.getHourlyRate() : new BigDecimal("100000"));
                        BigDecimal total = rate.multiply(BigDecimal.valueOf(Math.max(1, ctx.durationHours)));
                        courtOptionsText.append(String.format("• **%s** (%s): %,d đ/h (Tổng: %,d đ/%dh)\n",
                                c.getCourtName(), c.getCourtType() != null ? c.getCourtType() : "Tiêu chuẩn", rate.longValue(), total.longValue(), ctx.durationHours));
                        replies.add("Đặt " + c.getCourtName());
                    }
                    courtOptionsText.append("\nBạn muốn chọn đặt sân nào ạ?");
                    replies.add("Đổi khung giờ khác");

                    return ChatMessageDto.builder()
                            .sender("bot")
                            .intent("PROPOSE_COURTS")
                            .text(courtOptionsText.toString())
                            .quickReplies(replies)
                            .actionType("NONE")
                            .timestamp(LocalDateTime.now())
                            .build();
                }
            }

            // 4. Thiếu Dịch vụ đi kèm
            if (!hasAddOns) {
                ctx.stage = "PROPOSING_ADDONS";
                String promptText = String.format(
                        "Tôi đã ghi nhận bạn chọn **%s** tại **%s** lúc **%s** ngày **%s**.\n\n" +
                        "Bạn có muốn **chọn thêm dịch vụ / phụ kiện gì đi kèm** cho ca chơi không ạ?\n" +
                        "• Thuê vợt thi đấu Yonex/Lining: 50.000 đ/ca\n" +
                        "• Thuê giày chuyên dụng (đế kếp): 20.000 đ/đôi (đủ size 36 - 44)\n" +
                        "• Ống cầu Thành Công 77: 260.000 đ/ống (hoặc mua lẻ 25.000 đ/quả)\n" +
                        "• Nước giải khát ướp lạnh (Revive, Pocari, Bò Húc, Aquafina): 10.000 - 20.000 đ/chai.\n\n" +
                        "Nếu không có nhu cầu, bạn chỉ cần chọn **'Không chọn thêm gì'** nhé!",
                        ctx.courtName, ctx.branchName, ctx.timeSlot, ctx.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                );
                return ChatMessageDto.builder()
                        .sender("bot")
                        .intent("ASK_ADDONS")
                        .text(promptText)
                        .quickReplies(List.of("Không chọn thêm gì (Chỉ thuê sân)", "Thuê thêm 2 vợt thi đấu", "Thuê 1 đôi giày & 2 chai Revive", "Mua 1 ống cầu Thành Công 77"))
                        .actionType("NONE")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            // 5. Thiếu Tên hoặc Số điện thoại
            if (!hasName || !hasPhone) {
                if (!hasName && !hasPhone) {
                    ctx.stage = "ASK_NAME";
                    String promptText = String.format(
                            "Tôi đã chuẩn bị thông tin sân **%s** (%s) tại **%s** cho bạn.\n\n" +
                            "Để hệ thống tiến hành xuất **Mã Khóa Sân giữ chỗ**, bạn vui lòng cho tôi xin **Họ tên** và **Số điện thoại** của bạn (Ví dụ: 'Nguyễn Văn Tuấn - 0987654321') để lễ tân liên hệ và nhận diện khi bạn đến sân thi đấu nhé!",
                            ctx.courtName, ctx.timeSlot, ctx.branchName
                    );
                    return ChatMessageDto.builder()
                            .sender("bot")
                            .intent("ASK_NAME_AND_PHONE")
                            .text(promptText)
                            .quickReplies(List.of("Tôi dùng SĐT đăng nhập", "Bắt đầu lại"))
                            .actionType("NONE")
                            .timestamp(LocalDateTime.now())
                            .build();
                } else if (!hasName) {
                    ctx.stage = "ASK_NAME";
                    return ChatMessageDto.builder()
                            .sender("bot")
                            .intent("ASK_NAME")
                            .text(String.format("Bạn vui lòng cho tôi xin **Họ và tên** của bạn (SĐT đã ghi nhận: **%s**) để ghi trên phiếu giữ chỗ và xuất mã khóa sân nhé!", ctx.customerPhone))
                            .quickReplies(List.of("Bắt đầu lại"))
                            .actionType("NONE")
                            .timestamp(LocalDateTime.now())
                            .build();
                } else {
                    ctx.stage = "ASK_PHONE";
                    return ChatMessageDto.builder()
                            .sender("bot")
                            .intent("ASK_PHONE")
                            .text(String.format("Chào bạn **%s**! Bạn vui lòng cung cấp **Số điện thoại** liên hệ (10 chữ số) để hệ thống gửi mã khóa sân và liên hệ khi nhận sân nhé!", ctx.customerName))
                            .quickReplies(List.of("Bắt đầu lại"))
                            .actionType("NONE")
                            .timestamp(LocalDateTime.now())
                            .build();
                }
            }
        }

        // =========================================================================
        // DEFAULT FALLBACK
        // =========================================================================
        return ChatMessageDto.builder()
                .sender("bot")
                .intent("DEFAULT")
                .text("Tôi có thể hỗ trợ bạn tư vấn chi nhánh gần nhất, tra cứu lịch sân trống theo khung giờ mong muốn, tra cứu mã đơn đặt sân và tự động đặt giữ chỗ cho bạn. Bạn có thể chọn câu hỏi gợi ý bên dưới:")
                .quickReplies(List.of("Hệ thống có những chi nhánh nào?", "Tôi ở đường Hoàng Diệu 2 thì chi nhánh nào gần tôi?", "Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân"))
                .actionType("NONE")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // HÀM TẠO ĐƠN VÀ CHO HIỆN MÃ KHÓA SÂN
    // =========================================================================
    private ChatMessageDto executeCourtLocking(ChatSessionContext ctx) {
        if (bookingService == null || courtService == null) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("BOOKING_ERROR")
                    .text("Dịch vụ đặt sân tạm thời gián đoạn. Bạn vui lòng liên hệ hotline 1900 6868.")
                    .build();
        }

        // Re-verify court availability
        List<Court> branchCourts = (courtRepository != null && ctx.branchCode != null) ? courtRepository.findByBranchCode(ctx.branchCode) : Collections.emptyList();
        if (branchCourts == null) branchCourts = Collections.emptyList();
        Optional<Court> availableCourt = branchCourts.stream()
                .filter(c -> !"Bảo trì".equalsIgnoreCase(c.getStatus()))
                .filter(c -> {
                    if (bookingRepository == null) return true;
                    List<Booking> dayBookings = bookingRepository.findByCourtCodeAndBookingDate(c.getCourtCode(), ctx.date != null ? ctx.date : LocalDate.now());
                    if (dayBookings == null) dayBookings = Collections.emptyList();
                    return dayBookings.stream().noneMatch(b ->
                            b.getStatus() != null && !"Đã hủy".equalsIgnoreCase(b.getStatus())
                            && bookingService != null && bookingService.isSlotOverlapping(ctx.timeSlot, b.getTimeSlot())
                    );
                })
                .findFirst();

        if (availableCourt.isPresent()) {
            ctx.courtCode = availableCourt.get().getCourtCode();
            ctx.courtName = availableCourt.get().getCourtName();
        } else if (courtRepository != null && courtRepository.findByCourtCode(ctx.courtCode).isEmpty()) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("BOOKING_ERROR")
                    .text(String.format("Rất tiếc, các sân tại %s trong khung giờ %s ngày %s hiện đều đã kín hoặc đang bảo trì. Bạn vui lòng chọn khung giờ khác.",
                            ctx.branchName, ctx.timeSlot, ctx.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
                    .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Chi nhánh khác còn sân không?", "Bảng giá thuê sân"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        String bookingCode = bookingService.generateUniqueBookingCode();
        ctx.lastBookingCode = bookingCode;

        Booking booking = new Booking();
        booking.setBookingCode(bookingCode);
        booking.setBranchCode(ctx.branchCode);
        booking.setCourtCode(ctx.courtCode);
        booking.setCustomerName(ctx.customerName);
        booking.setCustomerPhone(ctx.customerPhone);
        booking.setCustomerCode(ctx.customerCode);
        booking.setBookingDate(ctx.date != null ? ctx.date : LocalDate.now());
        booking.setTimeSlot(ctx.timeSlot);
        booking.setHourlyPrice(ctx.hourlyRate);
        booking.setTotalPrice(ctx.totalPrice);
        booking.setDepositAmount(ctx.depositAmount);
        booking.setPaymentMethod("Chuyển khoản VietQR");
        booking.setStatus("Chờ cọc 30%");
        booking.setNotes("Dịch vụ đi kèm: " + (ctx.additionalServices != null ? ctx.additionalServices : "Không có") + " (Đặt tự động qua AI Chatbot)");

        try {
            bookingService.createBooking(booking);
            courtService.holdCourtForBooking(ctx.courtCode, ctx.branchCode, ctx.timeSlot, booking.getCustomerName());
        } catch (Exception e) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("BOOKING_ERROR")
                    .text("Rất tiếc, không thể tạo đơn đặt sân vì: " + e.getMessage() + ". Bạn vui lòng chọn khung giờ khác hoặc liên hệ hotline.")
                    .quickReplies(List.of("Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // VietQR Dynamic URL (MBBank)
        String qrUrl = String.format(
                "https://img.vietqr.io/image/970422-0903123456-compact2.png?amount=%d&addInfo=CK%%20%s&accountName=UTE%%20BADMINTON%%20CLUB",
                ctx.depositAmount.longValue(), bookingCode
        );

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("bookingCode", bookingCode);
        paymentData.put("courtName", ctx.courtName);
        paymentData.put("branchName", ctx.branchName);
        paymentData.put("bookingDate", ctx.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        paymentData.put("timeSlot", ctx.timeSlot);
        paymentData.put("additionalServices", ctx.additionalServices);
        paymentData.put("courtPrice", ctx.courtPrice);
        paymentData.put("additionalFee", ctx.additionalFee);
        paymentData.put("totalPrice", ctx.totalPrice);
        paymentData.put("depositAmount", ctx.depositAmount);
        paymentData.put("qrUrl", qrUrl);
        paymentData.put("bankName", "MBBank (Ngân hàng Quân Đội)");
        paymentData.put("accountNo", "0903123456");
        paymentData.put("accountName", "UTE BADMINTON CLUB");
        paymentData.put("createdAt", booking.getCreatedAt() != null ? booking.getCreatedAt().toString() : LocalDateTime.now().toString());

        ctx.stage = "BOOKED";

        String confirmText = String.format(
                "MÃ KHÓA SÂN GIỮ CHỖ CỦA BẠN ĐÃ ĐƯỢC XUẤT THÀNH CÔNG!\n\n" +
                "• **Mã khóa sân**: **%s**\n" +
                "• **Khách hàng**: **%s** (%s)\n" +
                "• **Cơ sở thi đấu**: %s\n" +
                "• **Sân đặt**: %s\n" +
                "• **Khung giờ**: %s ngày %s\n" +
                "• **Dịch vụ đi kèm**: %s\n" +
                "• **Tổng chi phí**: %,d đ\n" +
                "• **Tiền cọc cần thanh toán (30%%)**: **%,d đ**\n\n" +
                "Hệ thống đang **tạm khóa giữ chỗ sân trong 10 phút**. Bạn vui lòng quét mã VietQR bên dưới hoặc chuyển khoản theo thông tin để hoàn tất xác nhận giữ sân. Sau khi xác nhận cọc, đơn sẽ được lưu ngay vào [Lịch Sử Đặt Sân](/customer/history).",
                bookingCode, ctx.customerName, ctx.customerPhone, ctx.branchName, ctx.courtName,
                ctx.timeSlot, ctx.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                ctx.additionalServices, ctx.totalPrice.longValue(), ctx.depositAmount.longValue()
        );

        return ChatMessageDto.builder()
                .sender("bot")
                .intent("BOOKING_EXECUTED")
                .text(confirmText)
                .actionType("OPEN_PAYMENT")
                .paymentData(paymentData)
                .quickReplies(List.of("Xác nhận đã chuyển khoản cọc", "Xem lại thông tin đơn", "Hướng dẫn hủy ca chơi"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // HÀM TRÍCH XUẤT THỰC THỂ (SLOT EXTRACTION)
    // =========================================================================
    private void extractBookingSlots(String rawInput, String normalized, ChatSessionContext ctx) {
        if (rawInput == null) return;

        // 1. Số điện thoại (10 chữ số)
        Matcher phoneMatcher = Pattern.compile("(?:0[3|5|7|8|9])([0-9]{8})\\b").matcher(rawInput);
        if (phoneMatcher.find()) {
            String detectedPhone = phoneMatcher.group(0);
            ctx.customerPhone = detectedPhone;
            if (customerRepository != null) {
                customerRepository.findByPhone(detectedPhone).ifPresent(c -> {
                    ctx.customerCode = c.getCustomerCode();
                    if (!ctx.isNameProvided()) {
                        ctx.customerName = c.getFullName();
                    }
                });
            }
        }

        // 2. Họ tên khách hàng
        Matcher namePattern = Pattern.compile("(?i)(?:tôi tên là|mình tên là|em tên là|tôi tên|mình tên|em tên|tên tôi là|tên mình là|tôi là|mình là)\\s+([^,.;!?\n]{2,30})").matcher(rawInput);
        if (namePattern.find()) {
            String detectedName = namePattern.group(1).trim();
            detectedName = detectedName.replaceAll("(?i)(?:sdt|số điện thoại|sđt|phone).*", "").trim();
            if (!detectedName.isBlank() && detectedName.length() >= 2) {
                ctx.customerName = capitalizeWords(detectedName);
            }
        } else {
            Matcher nameNoAccPattern = Pattern.compile("(?i)(?:toi ten la|minh ten la|em ten la|toi ten|minh ten|em ten|ten toi la|ten minh la|toi la|minh la)\\s+([^,.;!?\n]{2,30})").matcher(normalized);
            if (nameNoAccPattern.find()) {
                String detectedName = nameNoAccPattern.group(1).trim();
                detectedName = detectedName.replaceAll("(?i)(?:sdt|so dien thoai|phone).*", "").trim();
                if (!detectedName.isBlank() && detectedName.length() >= 2) {
                    ctx.customerName = capitalizeWords(detectedName);
                }
            } else if ("ASK_NAME".equals(ctx.stage)) {
                // Khi đang hỏi tên, nếu người dùng trả lời 1 chuỗi chữ hợp lệ không chứa lệnh
                String clean = rawInput.trim();
                if (!clean.matches(".*\\d.*") && clean.length() >= 2 && clean.length() <= 30 && !clean.contains("?") && !matches(normalized, "dat san", "huy", "chi nhanh", "gia", "khong")) {
                    ctx.customerName = capitalizeWords(clean);
                }
            }
        }

        // 3. Chi nhánh
        if (matches(normalized, "co so 3", "chi nhanh 3", "quan 9", "q9", "do xuan hop", "le van viet", "man thien", "tang nhon phu", "phuoc long", "vinhomes grand park")) {
            ctx.branchCode = "CN03";
            ctx.branchName = "Chi nhánh 3 (Quận 9 - Số 70 Đỗ Xuân Hợp)";
        } else if (matches(normalized, "co so 2", "chi nhanh 2", "binh thanh", "dien bien phu", "hang xanh", "nguyen gia tri", "d2", "xo viet nghe tinh", "bach dang")) {
            ctx.branchCode = "CN02";
            ctx.branchName = "Chi nhánh 2 (Bình Thạnh - Số 234 Điện Biên Phủ)";
        } else if (matches(normalized, "co so 4", "chi nhanh 4", "go vap", "quang trung", "phan van tri", "nguyen oanh", "le duc tho")) {
            ctx.branchCode = "CN04";
            ctx.branchName = "Chi nhánh 4 (Gò Vấp - Số 18 Quang Trung)";
        } else if (matches(normalized, "co so 1", "chi nhanh 1", "thu duc", "vo van ngan", "hoang dieu 2", "kha van can", "dang van bi", "linh chieu", "linh trung")) {
            ctx.branchCode = "CN01";
            ctx.branchName = "Chi nhánh 1 (Thủ Đức - Số 1 Võ Văn Ngân)";
        }

        // 4. Ngày và Khung giờ
        if (matches(normalized, "ngay mai", "mai")) {
            ctx.date = LocalDate.now().plusDays(1);
        } else if (matches(normalized, "ngay kia", "mot")) {
            ctx.date = LocalDate.now().plusDays(2);
        } else if (matches(normalized, "hom nay", "toi nay", "chieu nay", "sang nay") || ctx.date == null) {
            ctx.date = LocalDate.now();
        }

        int startHour = -1;
        int endHour = -1;

        Pattern rangePattern = Pattern.compile("(\\d{1,2})(?:h|:00)?\\s*(?:đến|den|-|toi)\\s*(\\d{1,2})(?:h|:00)?");
        Matcher mRange = rangePattern.matcher(normalized);
        if (mRange.find()) {
            try {
                startHour = Integer.parseInt(mRange.group(1));
                endHour = Integer.parseInt(mRange.group(2));
            } catch (Exception ignored) {}
        } else {
            Pattern singlePattern = Pattern.compile("(?:luc|vao|tam|tu)?\\s*(\\d{1,2})(?:h|:00)?");
            Matcher mSingle = singlePattern.matcher(normalized);
            if (mSingle.find()) {
                try {
                    int h = Integer.parseInt(mSingle.group(1));
                    if (h >= 5 && h <= 22) {
                        startHour = h;
                        endHour = startHour + 2; // mặc định 2 tiếng
                    }
                } catch (Exception ignored) {}
            }
        }

        if (startHour != -1 && endHour != -1) {
            if (endHour <= startHour) {
                endHour = startHour + 1;
            }
            ctx.timeSlot = String.format("%02d:00 - %02d:00", startHour, endHour);
            ctx.durationHours = endHour - startHour;
        }

        // 5. Đặt sân gì (Sân VIP / Sân tiêu chuẩn / Sân cụ thể)
        if (matches(normalized, "san vip", "san 1", "cl01", "san so 1", "vip")) {
            ctx.courtType = "VIP";
            ctx.courtCode = "CL01";
            ctx.courtName = "Sân 1 (VIP)";
        } else if (matches(normalized, "san tieu chuan", "san 2", "san 3", "san 4", "san 5", "cl02", "cl03", "cl04", "cl05", "san thuong")) {
            ctx.courtType = "STANDARD";
            if (normalized.contains("san 2") || normalized.contains("cl02")) {
                ctx.courtCode = "CL02";
                ctx.courtName = "Sân 2 (Tiêu chuẩn)";
            } else if (normalized.contains("san 3") || normalized.contains("cl03")) {
                ctx.courtCode = "CL03";
                ctx.courtName = "Sân 3 (Tiêu chuẩn)";
            } else {
                ctx.courtCode = "CL02";
                ctx.courtName = "Sân tiêu chuẩn";
            }
        }

        // 6. Có chọn thêm gì đi kèm không
        if (matches(normalized, "khong lay them gi", "khong can gi them", "khong co", "khong can", "ko lay gi", "ko can", "chi thue san thoi", "chi thue san", "thoi khoi", "du roi", "khong them do", "ko lay phu kien", "khong chon them")) {
            ctx.additionalServices = "Không chọn thêm (Chỉ thuê sân)";
            ctx.additionalFee = BigDecimal.ZERO;
        } else if (matches(normalized, "thue vot", "thue giay", "mua cau", "mua nuoc", "pocari", "revive", "ong cau", "2 vot", "1 vot")) {
            List<String> items = new ArrayList<>();
            BigDecimal addFee = BigDecimal.ZERO;

            if (matches(normalized, "2 vot", "hai vot")) {
                items.add("Thuê 2 vợt thi đấu Yonex (100.000 đ)");
                addFee = addFee.add(new BigDecimal("100000"));
            } else if (matches(normalized, "thue vot", "1 vot", "mot vot", "vot thi dau")) {
                items.add("Thuê 1 vợt thi đấu Yonex (50.000 đ)");
                addFee = addFee.add(new BigDecimal("50000"));
            }

            if (matches(normalized, "thue giay", "doi giay", "giay")) {
                items.add("Thuê 1 đôi giày chuyên dụng (20.000 đ)");
                addFee = addFee.add(new BigDecimal("20000"));
            }

            if (matches(normalized, "ong cau", "thanh cong 77", "mua cau")) {
                items.add("1 ống cầu Thành Công 77 (260.000 đ)");
                addFee = addFee.add(new BigDecimal("260000"));
            }

            if (matches(normalized, "2 chai revive", "hai chai revive", "2 chai nuoc")) {
                items.add("2 chai nước khoáng Revive (30.000 đ)");
                addFee = addFee.add(new BigDecimal("30000"));
            } else if (matches(normalized, "revive", "pocari", "mua nuoc", "nuoc")) {
                items.add("1 chai nước thể thao ướp lạnh (15.000 đ)");
                addFee = addFee.add(new BigDecimal("15000"));
            }

            if (!items.isEmpty()) {
                ctx.additionalServices = String.join(", ", items);
                ctx.additionalFee = addFee;
            }
        }
    }

    private void recalculatePrices(ChatSessionContext ctx) {
        int startHour = extractStartHour(ctx.timeSlot);
        if (pricingService != null) {
            ctx.hourlyRate = pricingService.calculateHourlyRate(null, startHour);
            ctx.courtPrice = pricingService.calculateTotalPrice(ctx.hourlyRate, Math.max(1, ctx.durationHours));
            ctx.totalPrice = ctx.courtPrice.add(ctx.additionalFee != null ? ctx.additionalFee : BigDecimal.ZERO);
            ctx.depositAmount = pricingService.calculateDepositAmount(ctx.totalPrice);
        } else {
            ctx.hourlyRate = new BigDecimal("140000");
            ctx.courtPrice = ctx.hourlyRate.multiply(BigDecimal.valueOf(Math.max(1, ctx.durationHours)));
            ctx.totalPrice = ctx.courtPrice.add(ctx.additionalFee != null ? ctx.additionalFee : BigDecimal.ZERO);
            ctx.depositAmount = ctx.totalPrice.multiply(new BigDecimal("0.30")).setScale(0, java.math.RoundingMode.HALF_UP);
        }
    }

    private int extractStartHour(String timeSlot) {
        if (timeSlot == null) return 18;
        Matcher m = Pattern.compile("(\\d{1,2}):00").matcher(timeSlot);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }
        return 18;
    }

    private boolean isBookingFlow(String normalized, ChatSessionContext ctx) {
        if (matches(normalized, "dat san", "khoa san", "tu khoa san", "book san", "giu cho", "thue san", "san trong", "lay san nay", "chot san", "hien ma khoa san", "cho ma khoa san", "xuat ma khoa san", "lay ma khoa san", "khoa san gium")) {
            return true;
        }
        return "COLLECTING_INFO".equals(ctx.stage) || "PROPOSING_COURT".equals(ctx.stage) || "PROPOSING_ADDONS".equals(ctx.stage) || "ASK_NAME".equals(ctx.stage) || "ASK_PHONE".equals(ctx.stage) || "AWAITING_CONFIRMATION".equals(ctx.stage);
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isBlank()) return "";
        String[] words = str.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private boolean matches(String input, String... keywords) {
        for (String kw : keywords) {
            if (input.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsWord(String text, String word) {
        if (text == null || word == null) return false;
        return text.matches(".*\\b" + Pattern.quote(word) + "\\b.*");
    }

    private String removeAccents(String text) {
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("").replaceAll("đ", "d").replaceAll("Đ", "D");
    }
}
