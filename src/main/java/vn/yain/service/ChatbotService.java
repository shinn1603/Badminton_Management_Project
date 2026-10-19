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
        public String branchCode = "CN01";
        public String branchName = "Chi nhánh 1 (Thủ Đức - Số 1 Võ Văn Ngân)";
        public String courtCode = "CL01";
        public String courtName = "Sân 1 (VIP Yonex)";
        public LocalDate date = LocalDate.now();
        public String timeSlot = "18:00 - 20:00";
        public int durationHours = 2;
        public BigDecimal hourlyRate = new BigDecimal("140000");
        public BigDecimal totalPrice = new BigDecimal("280000");
        public BigDecimal depositAmount = new BigDecimal("84000");
        public String stage = "GREETING";
        public String lastBookingCode = null;
        public List<Map<String, String>> dialogHistory = new ArrayList<>();

        public void addDialogTurn(String role, String text) {
            lastAccessed = System.currentTimeMillis();
            if (dialogHistory.size() > 14) {
                dialogHistory.remove(0);
            }
            dialogHistory.add(Map.of("role", role, "text", text));
        }

        public void reset() {
            lastAccessed = System.currentTimeMillis();
            branchCode = "CN01";
            branchName = "Chi nhánh 1 (Thủ Đức - Số 1 Võ Văn Ngân)";
            courtCode = "CL01";
            courtName = "Sân 1 (VIP Yonex)";
            date = LocalDate.now();
            timeSlot = "18:00 - 20:00";
            durationHours = 2;
            hourlyRate = new BigDecimal("140000");
            totalPrice = new BigDecimal("280000");
            depositAmount = new BigDecimal("84000");
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
        String sid = (sessionId != null && !sessionId.trim().isEmpty()) ? sessionId.trim() : "default_session";
        ChatSessionContext ctx = sessionStore.computeIfAbsent(sid, k -> new ChatSessionContext());
        ctx.lastAccessed = System.currentTimeMillis();

        // 1. Try Google Gemini Cloud AI if configured
        if (geminiAiService != null && geminiAiService.isConfigured() && rawInput != null && !rawInput.trim().isEmpty()) {
            ChatMessageDto aiReply = geminiAiService.chatWithGemini(rawInput.trim(), ctx);
            if (aiReply != null) {
                return aiReply;
            }
        }

        // Welcome Greeting
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return ChatMessageDto.builder()
                    .sender("bot")
                    .text("Xin chào bạn. Tôi là trợ lý ảo chính thức của Hệ thống Sân Cầu Lông UTE Sport. Tôi có thể hỗ trợ bạn tìm chi nhánh gần nhất, tra cứu sân trống theo khung giờ cụ thể, kiểm tra thông tin đơn đặt sân, hướng dẫn hủy sân và tự động đặt sân giữ chỗ cho bạn ngay trong cuộc trò chuyện này. Bạn cần tôi hỗ trợ điều gì?")
                    .quickReplies(List.of("Hệ thống có những chi nhánh nào?", "Tôi ở đường Hoàng Diệu 2 thì chi nhánh nào gần tôi?", "Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        String text = rawInput.trim();
        String normalized = removeAccents(text.toLowerCase());

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
        // INTENT 1: XÁC NHẬN ĐẶT SÂN ("ok dat san cho toi", "dat san di", "dong y dat")
        // =========================================================================
        if ("CONFIRM_BOOKING".equals(mlTag) || matches(normalized, "ok dat san", "dat san cho toi", "dat san di", "dat di", "dat luon", "dong y dat", "tien hanh dat", "dat cho toi", "dat giup toi", "xac nhan dat", "dong y giu cho")) {
            String bookingCode = bookingService.generateUniqueBookingCode();
            ctx.lastBookingCode = bookingCode;

            Booking booking = new Booking();
            booking.setBookingCode(bookingCode);
            booking.setBranchCode(ctx.branchCode);
            booking.setCourtCode(ctx.courtCode);
            booking.setCustomerName("Khách đặt qua AI");
            booking.setCustomerPhone("0903123456");
            booking.setBookingDate(ctx.date != null ? ctx.date : LocalDate.now());
            booking.setTimeSlot(ctx.timeSlot);
            booking.setHourlyPrice(ctx.hourlyRate);
            booking.setTotalPrice(ctx.totalPrice);
            booking.setDepositAmount(ctx.depositAmount);
            booking.setPaymentMethod("Chuyển khoản VietQR");
            booking.setStatus("Chờ cọc 30%");
            booking.setNotes("Đặt tự động qua AI Chatbot");

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
            paymentData.put("totalPrice", ctx.totalPrice);
            paymentData.put("depositAmount", ctx.depositAmount);
            paymentData.put("qrUrl", qrUrl);
            paymentData.put("bankName", "MBBank (Ngân hàng Quân Đội)");
            paymentData.put("accountNo", "0903123456");
            paymentData.put("accountName", "UTE BADMINTON CLUB");

            ctx.stage = "BOOKED";

            String confirmText = String.format(
                    "Tôi đã đặt giữ chỗ thành công %s tại %s cho bạn trong khung giờ %s ngày %s.\n\n" +
                    "Mã đơn đặt sân: %s\n" +
                    "Tổng tiền ca chơi: %,d đ\n" +
                    "Tiền cọc cần thanh toán (30%%): %,d đ\n\n" +
                    "Hệ thống đang tạm khóa giữ chỗ sân trong 10 phút. Bạn vui lòng quét mã VietQR bên dưới hoặc chuyển khoản theo thông tin để hoàn tất xác nhận giữ sân.",
                    ctx.courtName, ctx.branchName, ctx.timeSlot, ctx.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    bookingCode, ctx.totalPrice.longValue(), ctx.depositAmount.longValue()
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

        if (!normalized.contains("huy") && ("CHECK_BOOKING".equals(mlTag) || matches(normalized, "kiem tra don", "tra cuu don", "xem don", "don cua toi", "ma don", "thong tin don", "xem lai thong tin don", "xem lai don", "don dat san"))) {
            String targetCode = null;
            Pattern codePattern = Pattern.compile("(?:ds\\s*)?(\\d{6})", Pattern.CASE_INSENSITIVE);
            Matcher mCode = codePattern.matcher(normalized);
            if (mCode.find()) {
                targetCode = "DS" + mCode.group(1);
            } else if (ctx.lastBookingCode != null) {
                targetCode = ctx.lastBookingCode;
            }

            if (targetCode != null) {
                Optional<Booking> optBooking = bookingRepository.findByBookingCode(targetCode);
                if (optBooking.isPresent()) {
                    Booking b = optBooking.get();
                    String extraMsg = (b.getStatus() != null && b.getStatus().contains("Chờ cọc"))
                        ? "Đơn của bạn đang trong trạng thái chờ cọc 30%. Bạn có thể chuyển khoản qua mã VietQR để hoàn tất."
                        : "Đơn của bạn đã được xác nhận an toàn trên hệ thống. Bạn vui lòng đến trước 10 phút để nhận sân.";

                    String reply = String.format(
                            "Thông tin chi tiết đơn đặt sân của bạn:\n\n" +
                            "Mã đơn: %s\n" +
                            "Cơ sở: %s\n" +
                            "Sân: %s\n" +
                            "Ngày chơi: %s\n" +
                            "Khung giờ: %s\n" +
                            "Tổng tiền: %,d đ\n" +
                            "Tiền cọc đã tính (30%%): %,d đ\n" +
                            "Trạng thái đơn: %s\n\n%s",
                            b.getBookingCode(), b.getBranchCode(), b.getCourtCode(),
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
                            .quickReplies(List.of("Hướng dẫn hủy ca chơi", "Hôm nay có sân nào trống từ 18h đến 20h không?", "Bảng giá thuê sân"))
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
        // INTENT 3: YÊU CẦU HỦY ĐẶT SÂN
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
                        if (rawInput != null && (rawInput.contains(bPhone) || rawInput.contains(last4))) {
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
        // INTENT 4: HỎI SÂN TRỐNG THEO GIỜ CỤ THỂ ("hom nay co san nao trong, toi muon danh luc 18h den 20h")
        // =========================================================================
        if (matches(normalized, "co san nao trong", "san nao trong", "con san khong", "muon danh luc", "danh luc", "tu may gio", "gio nao trong", "co san khong", "gio den gio", "kiem tra san", "tim san", "san trong")) {
            int startHour = 18;
            int endHour = 20;

            Pattern rangePattern = Pattern.compile("(\\d{1,2})(?:h|:00)?\\s*(?:đến|den|-|toi)\\s*(\\d{1,2})(?:h|:00)?");
            Matcher mRange = rangePattern.matcher(normalized);
            if (mRange.find()) {
                try {
                    startHour = Integer.parseInt(mRange.group(1));
                    endHour = Integer.parseInt(mRange.group(2));
                } catch (Exception ignored) {
                }
            } else {
                Pattern singlePattern = Pattern.compile("(?:luc|vao|tam)\\s*(\\d{1,2})(?:h|:00)?");
                Matcher mSingle = singlePattern.matcher(normalized);
                if (mSingle.find()) {
                    try {
                        startHour = Integer.parseInt(mSingle.group(1));
                        endHour = startHour + 1;
                    } catch (Exception ignored) {
                    }
                }
            }

            if (endHour <= startHour) {
                endHour = startHour + 1;
            }
            int duration = endHour - startHour;
            ctx.durationHours = duration;

            String slotStr = String.format("%02d:00 - %02d:00", startHour, endHour);
            ctx.timeSlot = slotStr;

            if (matches(normalized, "ngay mai", "mai")) {
                ctx.date = LocalDate.now().plusDays(1);
            } else {
                ctx.date = LocalDate.now();
            }

            // Determine pricing via centralized PricingService
            ctx.hourlyRate = pricingService.calculateHourlyRate(null, startHour);
            ctx.totalPrice = pricingService.calculateTotalPrice(ctx.hourlyRate, duration);
            ctx.depositAmount = pricingService.calculateDepositAmount(ctx.totalPrice);

            // Check actual courts from database and cross-reference with bookings
            List<Court> branchCourts = courtRepository.findByBranchCode(ctx.branchCode);
            Optional<Court> availableCourt = branchCourts.stream()
                    .filter(c -> !"Bảo trì".equalsIgnoreCase(c.getStatus()))
                    .filter(c -> {
                        List<Booking> dayBookings = bookingRepository.findByCourtCodeAndBookingDate(c.getCourtCode(), ctx.date);
                        return dayBookings.stream().noneMatch(b ->
                                b.getStatus() != null && !"Đã hủy".equalsIgnoreCase(b.getStatus())
                                && bookingService.isSlotOverlapping(slotStr, b.getTimeSlot())
                        );
                    })
                    .findFirst();

            if (availableCourt.isEmpty()) {
                return ChatMessageDto.builder()
                        .sender("bot")
                        .intent("COURT_UNAVAILABLE")
                        .text(String.format("Rất tiếc, vào ngày %s trong khung giờ %s tại %s, tất cả các sân đều đã có khách đặt trước hoặc đang bảo trì.\n\nBạn có muốn đổi sang khung giờ khác (ví dụ trước 17h hoặc sau 20h) hoặc tham khảo chi nhánh khác không?",
                                ctx.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), slotStr, ctx.branchName))
                        .quickReplies(List.of("Hôm nay có sân nào trống từ 14h đến 16h không?", "Hôm nay có sân nào trống từ 20h đến 22h không?", "Chi nhánh khác còn sân không?"))
                        .actionType("NONE")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            ctx.courtCode = availableCourt.get().getCourtCode();
            ctx.courtName = availableCourt.get().getCourtName();
            ctx.hourlyRate = pricingService.calculateHourlyRate(availableCourt.get(), startHour);
            ctx.totalPrice = pricingService.calculateTotalPrice(ctx.hourlyRate, duration);
            ctx.depositAmount = pricingService.calculateDepositAmount(ctx.totalPrice);

            ctx.stage = "COURT_PROPOSED";

            String reply = String.format(
                    "Ngày %s trong khung giờ %s tại %s, hệ thống đang còn %s sẵn sàng phục vụ.\n\n" +
                    "Thông tin chi tiết:\n" +
                    "• Thời gian: %s (Thời lượng: %d tiếng)\n" +
                    "• Đơn giá: %,d đ/giờ\n" +
                    "• Tổng tiền sân: %,d đ\n" +
                    "• Số tiền cọc 30%%: %,d đ\n\n" +
                    "Bạn có muốn tôi đặt giữ chỗ sân này cho bạn luôn không? Bạn chỉ cần nhắn 'Ok đặt sân cho tôi'.",
                    ctx.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), slotStr, ctx.branchName, ctx.courtName, slotStr, duration,
                    ctx.hourlyRate.longValue(), ctx.totalPrice.longValue(), ctx.depositAmount.longValue()
            );

            return ChatMessageDto.builder()
                    .sender("bot")
                    .intent("COURT_AVAILABILITY_SPECIFIC")
                    .text(reply)
                    .quickReplies(List.of("Ok đặt sân cho tôi", "Tôi muốn đổi sang giờ khác", "Chi nhánh khác có sân không?"))
                    .actionType("NONE")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // =========================================================================
        // INTENT 5: TRA CỨU GIẢI ĐẤU & LỊCH THI ĐẤU (TOURNAMENTS)
        // =========================================================================
        if ("TOURNAMENTS".equals(mlTag) || matches(normalized, "giai dau", "giai cau long", "co giai nao", "lich thi dau", "dang ky giai", "tournament")) {
            List<Tournament> tournaments = tournamentRepository.findAll();
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
        // INTENT 6: DỊCH VỤ ĐI KÈM & THUÊ DỤNG CỤ
        // =========================================================================
        if ("SERVICES_EQUIPMENT".equals(mlTag) || matches(normalized, "thue vot", "co vot khong", "ban cau", "mua cau", "dan vot", "cang vot", "nuoc uong", "dich vu", "phu kien")) {
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
        // INTENT 7: HỎI CHI NHÁNH GẦN TÔI ("toi o dia chi vv thi chi nhanh nao gan toi")
        // =========================================================================
        if (matches(normalized, "gan toi", "gan nhat", "toi o", "o dia chi", "o duong", "o khu vuc", "cach bao xa", "o gan", "dia chi nao gan")) {
            String recommendedBranchCode = "CN01";
            String recommendedBranchName = "Chi nhánh 1 (Thủ Đức - Số 1 Võ Văn Ngân)";
            String distNote = "khoảng 1.5 - 2.5 km";

            // Geolocation matching across HCMC
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
            ctx.stage = "BRANCH_SELECTED";

            String reply = String.format(
                    "Dựa trên vị trí của bạn, %s là cơ sở gần bạn nhất (khoảng cách %s).\n\n" +
                    "Cơ sở này được trang bị thảm Yonex BWF tiêu chuẩn thi đấu, đèn chống chói 500 Lux và bãi giữ xe rộng rãi.\n\n" +
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
        // INTENT 8: HỎI CÁC CHI NHÁNH ("ban co chi nhanh nao", "he thong co may chi nhanh")
        // =========================================================================
        if ("FIND_BRANCH".equals(mlTag) || matches(normalized, "chi nhanh nao", "cac chi nhanh", "danh sach chi nhanh", "co may chi nhanh", "co nhung co so nao", "he thong o dau")) {
            List<Branch> branches = branchRepository.findAll();
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
        // INTENT 9: HỎI BẢNG GIÁ
        // =========================================================================
        if ("PRICING_INFO".equals(mlTag) || matches(normalized, "gia", "bang gia", "bao nhieu tien", "chi phi", "gio vang", "don gia")) {
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
        // INTENT 10: THÔNG TIN SÂN & CƠ SỞ VẬT CHẤT (COURT_FACILITIES)
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
        // INTENT 11: HỘI VIÊN & TÍCH LŨY ĐIỂM THƯỞNG (MEMBERSHIP_LOYALTY)
        // =========================================================================
        if ("MEMBERSHIP_LOYALTY".equals(mlTag) || matches(normalized, "tich diem", "the hoi vien", "hang thanh vien", "diem thuong", "doi voucher", "dong bac vang", "kim cuong", "vip", "uu dai")) {
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
        // INTENT 12: PHƯƠNG THỨC THANH TOÁN & HÓA ĐƠN VAT (PAYMENT_METHODS)
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
        // INTENT 13: NỘI QUY SÂN ĐẤU & TRANG PHỤC (RULES_REGULATIONS)
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
        // INTENT 14: ĐỒ THẤT LẠC, KHIẾU NẠI & HOTLINE HỖ TRỢ (LOST_FOUND_SUPPORT)
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

    private boolean matches(String input, String... keywords) {
        for (String kw : keywords) {
            if (input.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private String removeAccents(String text) {
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("").replaceAll("đ", "d").replaceAll("Đ", "D");
    }
}
