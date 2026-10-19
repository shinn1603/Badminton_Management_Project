package vn.yain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.yain.dto.BookingNotificationDto;
import vn.yain.dto.CourtEventDto;
import vn.yain.entity.Booking;
import vn.yain.entity.Court;
import vn.yain.entity.Invoice;
import vn.yain.entity.Payment;
import vn.yain.repository.BookingRepository;
import vn.yain.repository.CourtRepository;
import vn.yain.repository.InvoiceRepository;
import vn.yain.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CourtService courtService;

    @Autowired
    private PricingService pricingService;

    @Autowired(required = false)
    private vn.yain.repository.CustomerRepository customerRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @Value("${vietqr.bank.bin:970422}")
    private String bankBin;

    @Value("${vietqr.bank.name:MBBank (Ngân hàng Quân Đội)}")
    private String bankName;

    @Value("${vietqr.account.no:0903123456}")
    private String accountNo;

    @Value("${vietqr.account.name:UTE BADMINTON CLUB}")
    private String accountName;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Booking> getBookingByCode(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode);
    }

    public String generateUniqueBookingCode() {
        for (int i = 0; i < 20; i++) {
            String code = "DS" + String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
            if (bookingRepository.findByBookingCode(code).isEmpty()) {
                return code;
            }
        }
        return "DS" + (System.currentTimeMillis() % 900000 + 100000);
    }

    public String generateUniquePaymentCode() {
        for (int i = 0; i < 20; i++) {
            String code = "PAY" + String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
            if (paymentRepository.findByPaymentCode(code).isEmpty()) {
                return code;
            }
        }
        return "PAY" + (System.currentTimeMillis() % 900000 + 100000);
    }

    public String generateUniqueInvoiceCode() {
        for (int i = 0; i < 20; i++) {
            String code = "HD" + String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
            if (invoiceRepository.findByInvoiceCode(code).isEmpty()) {
                return code;
            }
        }
        return "HD" + (System.currentTimeMillis() % 900000 + 100000);
    }

    public synchronized Booking createBooking(Booking booking) {
        if (booking.getBookingCode() != null && !booking.getBookingCode().trim().isEmpty()) {
            Optional<Booking> opt = bookingRepository.findByBookingCode(booking.getBookingCode().trim());
            if (opt.isPresent()) {
                return updateBooking(booking.getBookingCode().trim(), booking);
            }
        }

        if (booking.getCourtCode() == null || booking.getCourtCode().trim().isEmpty() ||
            booking.getTimeSlot() == null || booking.getTimeSlot().trim().isEmpty()) {
            throw new IllegalArgumentException("Thông tin sân và khung giờ không được để trống!");
        }

        int[] hours = parseSlotHours(booking.getTimeSlot());
        if (hours[1] <= hours[0]) {
            throw new IllegalArgumentException("Khung giờ không hợp lệ: giờ kết thúc phải sau giờ bắt đầu!");
        }
        if (hours[0] < 5 || hours[1] > 23) {
            throw new IllegalArgumentException("Khung giờ ngoài thời gian hoạt động của câu lạc bộ (05:00 - 23:00)!");
        }

        LocalDate date = booking.getBookingDate() != null ? booking.getBookingDate() : LocalDate.now();
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày đặt sân không thể ở trong quá khứ!");
        }
        booking.setBookingDate(date);

        courtRepository.findByCourtCode(booking.getCourtCode().trim()).ifPresent(court -> {
            if ("Bảo trì".equalsIgnoreCase(court.getStatus())) {
                throw new IllegalStateException("Sân " + booking.getCourtCode() + " đang bảo trì, không thể đặt!");
            }
        });

        // Atomic conflict check to prevent race condition double-booking
        List<Booking> existing = bookingRepository.findByCourtCodeAndBookingDate(booking.getCourtCode(), date);
        boolean conflict = existing.stream().anyMatch(b ->
                b.getStatus() != null && !"Đã hủy".equalsIgnoreCase(b.getStatus())
                && isSlotOverlapping(booking.getTimeSlot(), b.getTimeSlot())
        );
        if (conflict) {
            throw new IllegalStateException("Khung giờ " + booking.getTimeSlot() + " của sân " + booking.getCourtCode() + " đã có người đặt trước!");
        }

        if (booking.getBookingCode() == null || booking.getBookingCode().trim().isEmpty()) {
            booking.setBookingCode(generateUniqueBookingCode());
        }
        if (booking.getStatus() == null) {
            booking.setStatus("Chờ cọc 30%");
        }
        if ((booking.getDepositAmount() == null || booking.getDepositAmount().compareTo(BigDecimal.ZERO) == 0) && booking.getTotalPrice() != null) {
            // Default 30% deposit calculation
            booking.setDepositAmount(booking.getTotalPrice().multiply(new BigDecimal("0.3")));
        }

        Booking saved = bookingRepository.save(booking);

        // Update court status and broadcast
        courtRepository.findByCourtCode(booking.getCourtCode()).ifPresent(court -> {
            court.setStatus("Đã đặt trước");
            courtRepository.save(court);
        });

        broadcastBookingEvent(BookingNotificationDto.builder()
                .eventType("NEW_BOOKING")
                .bookingCode(saved.getBookingCode())
                .courtCode(saved.getCourtCode())
                .branchCode(saved.getBranchCode())
                .customerName(saved.getCustomerName())
                .customerPhone(saved.getCustomerPhone())
                .timeSlot(saved.getTimeSlot())
                .totalPrice(saved.getTotalPrice())
                .depositAmount(saved.getDepositAmount())
                .status(saved.getStatus())
                .message("Đơn đặt sân mới [" + saved.getBookingCode() + "] - Sân " + saved.getCourtCode() + " (" + saved.getTimeSlot() + ")")
                .timestamp(LocalDateTime.now())
                .build());

        return saved;
    }

    public synchronized Booking updateBooking(String bookingCode, Booking booking) {
        Booking existing = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt sân: " + bookingCode));

        String courtCode = booking.getCourtCode() != null && !booking.getCourtCode().trim().isEmpty()
                ? booking.getCourtCode().trim() : existing.getCourtCode();
        String timeSlot = booking.getTimeSlot() != null && !booking.getTimeSlot().trim().isEmpty()
                ? booking.getTimeSlot().trim() : existing.getTimeSlot();
        LocalDate date = booking.getBookingDate() != null ? booking.getBookingDate() : existing.getBookingDate();

        // Conflict check excluding current booking
        List<Booking> sameCourtBookings = bookingRepository.findByCourtCodeAndBookingDate(courtCode, date);
        boolean conflict = sameCourtBookings.stream().anyMatch(b ->
                !b.getBookingCode().equalsIgnoreCase(existing.getBookingCode())
                && b.getStatus() != null && !"Đã hủy".equalsIgnoreCase(b.getStatus())
                && isSlotOverlapping(timeSlot, b.getTimeSlot())
        );
        if (conflict) {
            throw new IllegalStateException("Khung giờ " + timeSlot + " của sân " + courtCode + " đã có người đặt trước!");
        }

        if (booking.getCustomerName() != null && !booking.getCustomerName().trim().isEmpty()) {
            existing.setCustomerName(booking.getCustomerName().trim());
        }
        if (booking.getCustomerPhone() != null && !booking.getCustomerPhone().trim().isEmpty()) {
            existing.setCustomerPhone(booking.getCustomerPhone().trim());
        }
        if (booking.getBranchCode() != null && !booking.getBranchCode().trim().isEmpty()) {
            existing.setBranchCode(booking.getBranchCode().trim());
        }
        existing.setCourtCode(courtCode);
        existing.setTimeSlot(timeSlot);
        existing.setBookingDate(date);
        if (booking.getHourlyPrice() != null) existing.setHourlyPrice(booking.getHourlyPrice());
        if (booking.getTotalPrice() != null) existing.setTotalPrice(booking.getTotalPrice());
        if (booking.getDepositAmount() != null) existing.setDepositAmount(booking.getDepositAmount());
        if (booking.getPaymentMethod() != null) existing.setPaymentMethod(booking.getPaymentMethod());
        if (booking.getStatus() != null && !booking.getStatus().trim().isEmpty()) {
            existing.setStatus(booking.getStatus().trim());
        }
        if (booking.getNotes() != null) existing.setNotes(booking.getNotes());

        Booking saved = bookingRepository.save(existing);

        broadcastBookingEvent(BookingNotificationDto.builder()
                .eventType("UPDATE_BOOKING")
                .bookingCode(saved.getBookingCode())
                .courtCode(saved.getCourtCode())
                .branchCode(saved.getBranchCode())
                .customerName(saved.getCustomerName())
                .customerPhone(saved.getCustomerPhone())
                .timeSlot(saved.getTimeSlot())
                .totalPrice(saved.getTotalPrice())
                .depositAmount(saved.getDepositAmount())
                .status(saved.getStatus())
                .message("Cập nhật đơn đặt sân [" + saved.getBookingCode() + "] - Sân " + saved.getCourtCode() + " (" + saved.getTimeSlot() + ")")
                .timestamp(LocalDateTime.now())
                .build());

        return saved;
    }

    public synchronized Map<String, Object> lockSlotBooking(Map<String, Object> payload) {
        String courtCode = (String) payload.get("courtCode");
        String branchCode = (String) payload.get("branchCode");
        String dateStr = (String) payload.get("date");
        String timeSlot = (String) payload.get("timeSlot");
        String customerName = (String) payload.getOrDefault("customerName", "Khách đặt trực tuyến");
        String customerPhone = (String) payload.getOrDefault("customerPhone", "0903123456");

        if (courtCode == null || courtCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã sân không được để trống!");
        }
        if (timeSlot == null || timeSlot.trim().isEmpty()) {
            throw new IllegalArgumentException("Khung giờ không được để trống!");
        }

        LocalDate bookingDate = (dateStr != null && !dateStr.trim().isEmpty())
                ? LocalDate.parse(dateStr.trim())
                : LocalDate.now();

        if (bookingDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày đặt sân không thể ở trong quá khứ!");
        }

        Court court = courtRepository.findByCourtCode(courtCode.trim()).orElse(null);
        if (court != null && "Bảo trì".equalsIgnoreCase(court.getStatus())) {
            throw new IllegalStateException("Sân " + courtCode + " đang bảo trì, vui lòng chọn sân khác!");
        }

        // Validate that the slot is not already booked (including multi-hour overlaps)
        List<Booking> existing = bookingRepository.findByCourtCodeAndBookingDate(courtCode.trim(), bookingDate);
        boolean alreadyBooked = existing.stream()
                .anyMatch(b -> b.getStatus() != null && !"Đã hủy".equalsIgnoreCase(b.getStatus()) && isSlotOverlapping(timeSlot, b.getTimeSlot()));
        if (alreadyBooked) {
            throw new IllegalStateException("Khung giờ " + timeSlot + " của sân này đã có người đặt trước!");
        }

        // Duration and pricing
        int duration = 1;
        if (timeSlot.contains(",")) {
            duration = timeSlot.split(",").length;
        } else {
            int[] hours = parseSlotHours(timeSlot);
            if (hours[1] > hours[0]) {
                duration = hours[1] - hours[0];
            }
        }

        BigDecimal courtTotal = BigDecimal.ZERO;
        if (payload.get("totalPrice") != null) {
            try {
                courtTotal = new BigDecimal(payload.get("totalPrice").toString());
            } catch (Exception ignored) {}
        }
        if (courtTotal.compareTo(BigDecimal.ZERO) <= 0) {
            if (timeSlot.contains(",")) {
                String[] parts = timeSlot.split(",");
                for (String part : parts) {
                    int[] h = parseSlotHours(part.trim());
                    BigDecimal rate = pricingService != null ? pricingService.calculateHourlyRate(court, h[0]) : (court != null && court.getHourlyRate() != null ? court.getHourlyRate() : new BigDecimal("90000"));
                    courtTotal = courtTotal.add(rate);
                }
            } else {
                int[] hours = parseSlotHours(timeSlot);
                BigDecimal hourlyRate = pricingService != null ? pricingService.calculateHourlyRate(court, hours[0]) : (court != null && court.getHourlyRate() != null ? court.getHourlyRate() : new BigDecimal("90000"));
                courtTotal = pricingService != null ? pricingService.calculateTotalPrice(hourlyRate, duration) : hourlyRate.multiply(BigDecimal.valueOf(duration));
            }
        }

        BigDecimal addonAmount = BigDecimal.ZERO;
        if (payload.get("addonAmount") != null) {
            try {
                addonAmount = new BigDecimal(payload.get("addonAmount").toString());
            } catch (Exception ignored) {}
        }
        BigDecimal totalPrice = courtTotal.add(addonAmount);

        BigDecimal discountAmount = BigDecimal.ZERO;
        String promoCode = (String) payload.get("promoCode");
        if (payload.get("discountAmount") != null) {
            try {
                discountAmount = new BigDecimal(payload.get("discountAmount").toString());
            } catch (Exception ignored) {}
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            totalPrice = totalPrice.subtract(discountAmount);
            if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
                totalPrice = BigDecimal.ZERO;
            }
        }

        BigDecimal deposit = BigDecimal.ZERO;
        if (payload.get("depositAmount") != null) {
            try {
                deposit = new BigDecimal(payload.get("depositAmount").toString());
            } catch (Exception ignored) {}
        }
        if (deposit.compareTo(BigDecimal.ZERO) <= 0) {
            deposit = pricingService != null ? pricingService.calculateDepositAmount(totalPrice) : totalPrice.multiply(new BigDecimal("0.3")).setScale(0, java.math.RoundingMode.HALF_UP);
        }

        BigDecimal avgHourlyRate = duration > 0 ? courtTotal.divide(BigDecimal.valueOf(duration), 0, java.math.RoundingMode.HALF_UP) : courtTotal;

        String bookingCode = generateUniqueBookingCode();
        Booking booking = new Booking();
        booking.setBookingCode(bookingCode);
        booking.setBranchCode(branchCode != null ? branchCode.trim() : (court != null ? court.getBranchCode() : "CN01"));
        booking.setCourtCode(courtCode.trim());
        booking.setCustomerName(customerName != null ? customerName.trim() : "Khách đặt trực tuyến");
        booking.setCustomerPhone(customerPhone != null ? customerPhone.trim() : "0903123456");
        booking.setBookingDate(bookingDate);
        booking.setTimeSlot(timeSlot.trim());
        booking.setHourlyPrice(avgHourlyRate);
        booking.setTotalPrice(totalPrice);
        booking.setDepositAmount(deposit);
        booking.setPaymentMethod("Chuyển khoản VietQR");
        booking.setStatus("Chờ cọc 30%");

        String customNotes = (String) payload.get("notes");
        StringBuilder noteBuilder = new StringBuilder();
        if (customNotes != null && !customNotes.trim().isEmpty()) {
            noteBuilder.append(customNotes.trim());
        } else {
            noteBuilder.append("Đặt qua Lưới giờ Sân trực tuyến");
        }
        if (promoCode != null && !promoCode.trim().isEmpty() && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            noteBuilder.append(" | Áp dụng mã: ").append(promoCode.trim().toUpperCase())
                       .append(" (-").append(String.format("%,d", discountAmount.longValue())).append("đ)");
        }
        booking.setNotes(noteBuilder.toString());

        // Liên kết mã khách hàng từ cơ sở dữ liệu nếu tồn tại
        String customerEmail = (String) payload.get("customerEmail");
        if (customerEmail == null || customerEmail.isBlank()) {
            customerEmail = (String) payload.get("email");
        }
        if (customerEmail != null && !customerEmail.isBlank()) {
            booking.setCustomerEmail(customerEmail.trim());
        }

        if (customerRepository != null && customerPhone != null && !customerPhone.isBlank()) {
            customerRepository.findByPhone(customerPhone.trim())
                    .ifPresent(c -> {
                        booking.setCustomerCode(c.getCustomerCode());
                        if (booking.getCustomerEmail() == null || booking.getCustomerEmail().isBlank()) {
                            booking.setCustomerEmail(c.getEmail());
                        }
                    });
        }

        Booking saved = bookingRepository.save(booking);

        broadcastBookingEvent(BookingNotificationDto.builder()
                .eventType("NEW_BOOKING")
                .bookingCode(bookingCode)
                .courtCode(courtCode)
                .branchCode(booking.getBranchCode())
                .customerName(booking.getCustomerName())
                .customerPhone(booking.getCustomerPhone())
                .timeSlot(timeSlot)
                .totalPrice(totalPrice)
                .depositAmount(deposit)
                .status("Chờ cọc 30%")
                .message("Đơn đặt sân mới trực tuyến [" + bookingCode + "] - Sân " + courtCode + " (" + timeSlot + ")")
                .timestamp(LocalDateTime.now())
                .build());

        String bin = (bankBin != null && !bankBin.isBlank()) ? bankBin : "970422";
        String accNo = (accountNo != null && !accountNo.isBlank()) ? accountNo : "0903123456";
        String accName = (accountName != null && !accountName.isBlank()) ? accountName : "UTE BADMINTON CLUB";
        String bName = (bankName != null && !bankName.isBlank()) ? bankName : "MBBank (Ngân hàng Quân Đội)";

        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=CK%%20%s&accountName=%s",
                bin, accNo, deposit.longValue(), bookingCode, java.net.URLEncoder.encode(accName, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20")
        );

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("bookingCode", bookingCode);
        resp.put("courtCode", courtCode);
        resp.put("courtName", court != null ? court.getCourtName() : courtCode);
        resp.put("branchCode", booking.getBranchCode());
        resp.put("date", bookingDate.toString());
        resp.put("timeSlot", timeSlot);
        resp.put("totalPrice", totalPrice);
        resp.put("depositAmount", deposit);
        resp.put("qrUrl", qrUrl);
        resp.put("bankName", bName);
        resp.put("accountNo", accNo);
        resp.put("accountName", accName);

        return resp;
    }

    /**
     * Confirm 30% deposit payment via VietQR (XL-03)
     */
    public Booking confirmDeposit(String bookingCode, BigDecimal depositAmount, String paymentMethod, String txRef) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt sân: " + bookingCode));

        if ("Đã hủy".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Đơn đặt sân [" + bookingCode + "] đã bị hủy, không thể xác nhận cọc!");
        }
        if ("Hoàn thành".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Đơn đặt sân [" + bookingCode + "] đã hoàn thành!");
        }

        booking.setStatus("Đã cọc 30%");
        booking.setDepositAmount(depositAmount != null ? depositAmount : booking.getDepositAmount());
        booking.setPaymentMethod(paymentMethod != null ? paymentMethod : "VietQR Pro");
        Booking updated = bookingRepository.save(booking);

        // Khóa cố định sân trên hệ thống và hủy bỏ bộ đếm tạm giữ chỗ 10 phút
        if (updated.getCourtCode() != null) {
            courtService.updateCourtStatus(updated.getCourtCode(), "Đang sử dụng");
        }

        // Tự động liên kết mã khách hàng & tích lũy điểm thưởng thành viên
        if (customerRepository != null) {
            if (updated.getCustomerCode() == null && updated.getCustomerPhone() != null && !updated.getCustomerPhone().isBlank()) {
                customerRepository.findByPhone(updated.getCustomerPhone().trim()).ifPresent(c -> {
                    updated.setCustomerCode(c.getCustomerCode());
                    bookingRepository.save(updated);
                });
            }
            if (updated.getCustomerCode() != null) {
                customerRepository.findByCustomerCode(updated.getCustomerCode()).ifPresent(c -> {
                    int addPts = depositAmount != null
                            ? depositAmount.divide(BigDecimal.valueOf(1000), java.math.RoundingMode.HALF_UP).intValue()
                            : 30;
                    c.setRewardPoints((c.getRewardPoints() != null ? c.getRewardPoints() : 0) + addPts);
                    customerRepository.save(c);
                });
            }
        }

        // Record payment transaction
        Payment payment = new Payment();
        payment.setPaymentCode(generateUniquePaymentCode());
        payment.setBookingCode(bookingCode);
        payment.setAmount(booking.getDepositAmount());
        payment.setMethod(booking.getPaymentMethod());
        payment.setTransactionType("Đặt cọc 30%");
        payment.setTransactionRef(txRef != null ? txRef : "VQR-" + bookingCode);
        payment.setStatus("Thành công");
        paymentRepository.save(payment);

        broadcastBookingEvent(BookingNotificationDto.builder()
                .eventType("DEPOSIT_CONFIRMED")
                .bookingCode(updated.getBookingCode())
                .courtCode(updated.getCourtCode())
                .branchCode(updated.getBranchCode())
                .customerName(updated.getCustomerName())
                .customerPhone(updated.getCustomerPhone())
                .timeSlot(updated.getTimeSlot())
                .totalPrice(updated.getTotalPrice())
                .depositAmount(updated.getDepositAmount())
                .status(updated.getStatus())
                .message("Đã nhận tiền cọc cho đơn [" + updated.getBookingCode() + "] - Sân " + updated.getCourtCode())
                .timestamp(LocalDateTime.now())
                .build());

        if (emailService != null) {
            try {
                emailService.sendBookingConfirmation(updated);
            } catch (Exception e) {
                // Log only, do not rollback transaction
            }
        }

        return updated;
    }

    /**
     * Check-in court at POS reception (XL-04)
     */
    public Booking checkIn(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn: " + bookingCode));

        if ("Đã hủy".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Đơn đặt sân [" + bookingCode + "] đã bị hủy, không thể nhận sân!");
        }
        if ("Hoàn thành".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Đơn đặt sân [" + bookingCode + "] đã hoàn thành, không thể nhận sân lại!");
        }
        if ("Đang sử dụng".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Đơn đặt sân [" + bookingCode + "] hiện đang sử dụng!");
        }

        booking.setStatus("Đang sử dụng");
        booking.setCheckInTime(LocalDateTime.now());
        Booking updated = bookingRepository.save(booking);

        courtRepository.findByCourtCode(booking.getCourtCode()).ifPresent(court -> {
            court.setStatus("Đang sử dụng");
            courtRepository.save(court);
            courtService.updateCourtStatus(court.getCourtCode(), "Đang sử dụng");
        });

        broadcastBookingEvent(BookingNotificationDto.builder()
                .eventType("CHECKED_IN")
                .bookingCode(updated.getBookingCode())
                .courtCode(updated.getCourtCode())
                .branchCode(updated.getBranchCode())
                .customerName(updated.getCustomerName())
                .status(updated.getStatus())
                .message("Khách đã nhận sân [" + updated.getCourtCode() + "] - Đơn [" + updated.getBookingCode() + "]")
                .timestamp(LocalDateTime.now())
                .build());

        return updated;
    }

    /**
     * Check-out court, settle balance & generate invoice (XL-07)
     */
    public Invoice checkOut(String bookingCode, BigDecimal extraEquipmentFee, BigDecimal extraProductFee, String paymentMethod, String staffCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn: " + bookingCode));

        if ("Hoàn thành".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Đơn đặt sân [" + bookingCode + "] đã hoàn tất thanh toán và trả sân!");
        }
        if ("Đã hủy".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Đơn đặt sân [" + bookingCode + "] đã bị hủy, không thể thanh toán trả sân!");
        }

        booking.setStatus("Hoàn thành");
        booking.setCheckOutTime(LocalDateTime.now());
        bookingRepository.save(booking);

        BigDecimal courtFee = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal eqFee = extraEquipmentFee != null ? extraEquipmentFee : BigDecimal.ZERO;
        BigDecimal prodFee = extraProductFee != null ? extraProductFee : BigDecimal.ZERO;
        BigDecimal depositPaid = booking.getDepositAmount() != null ? booking.getDepositAmount() : BigDecimal.ZERO;

        BigDecimal totalPayment = courtFee.add(eqFee).add(prodFee).subtract(depositPaid);
        if (totalPayment.compareTo(BigDecimal.ZERO) < 0) {
            totalPayment = BigDecimal.ZERO;
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceCode(generateUniqueInvoiceCode());
        invoice.setBookingCode(bookingCode);
        invoice.setStaffCode(staffCode != null ? staffCode : "NVQ01");
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setCourtFee(courtFee);
        invoice.setEquipmentFee(eqFee);
        invoice.setProductFee(prodFee);
        invoice.setDepositPaid(depositPaid);
        invoice.setTotalPayment(totalPayment);
        invoice.setPaymentMethod(paymentMethod != null ? paymentMethod : "Tiền mặt");
        invoice.setStatus("Đã thanh toán");

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Reset court status to "Trống"
        courtRepository.findByCourtCode(booking.getCourtCode()).ifPresent(court -> {
            courtService.updateCourtStatus(court.getCourtCode(), "Trống");
        });

        broadcastBookingEvent(BookingNotificationDto.builder()
                .eventType("CHECKED_OUT")
                .bookingCode(bookingCode)
                .courtCode(booking.getCourtCode())
                .branchCode(booking.getBranchCode())
                .customerName(booking.getCustomerName())
                .status("Hoàn thành")
                .message("Hoàn tất thanh toán & trả sân [" + booking.getCourtCode() + "] - Đơn [" + bookingCode + "]")
                .timestamp(LocalDateTime.now())
                .build());

        return savedInvoice;
    }

    /**
     * Cancel booking
     */
    public Booking cancelBooking(String bookingCode, String reason) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn: " + bookingCode));

        if ("Hoàn thành".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Đơn đặt sân [" + bookingCode + "] đã hoàn thành, không thể hủy!");
        }
        if ("Đã hủy".equalsIgnoreCase(booking.getStatus())) {
            return booking;
        }

        booking.setStatus("Đã hủy");
        booking.setNotes((booking.getNotes() != null ? booking.getNotes() + " | " : "") + "Hủy: " + reason);
        Booking saved = bookingRepository.save(booking);

        courtRepository.findByCourtCode(booking.getCourtCode()).ifPresent(court -> {
            courtService.updateCourtStatus(court.getCourtCode(), "Trống");
        });

        broadcastBookingEvent(BookingNotificationDto.builder()
                .eventType("BOOKING_CANCELLED")
                .bookingCode(bookingCode)
                .courtCode(saved.getCourtCode())
                .branchCode(saved.getBranchCode())
                .customerName(saved.getCustomerName())
                .status(saved.getStatus())
                .message("Đã hủy đơn đặt sân [" + bookingCode + "]")
                .timestamp(LocalDateTime.now())
                .build());

        return saved;
    }

    /**
     * Court Transfer / Change Court & Time Slot (XL-06)
     */
    public Map<String, Object> transferCourt(String bookingCode, String targetCourtCode, String newTimeSlot) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt sân: " + bookingCode));

        String oldCourtCode = booking.getCourtCode();
        if (oldCourtCode.equalsIgnoreCase(targetCourtCode) && (newTimeSlot == null || newTimeSlot.equalsIgnoreCase(booking.getTimeSlot()))) {
            throw new IllegalArgumentException("Sân đích và khung giờ trùng với sân hiện tại!");
        }

        Court targetCourt = courtRepository.findByCourtCode(targetCourtCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sân đích: " + targetCourtCode));

        if ("Bảo trì".equalsIgnoreCase(targetCourt.getStatus())) {
            throw new IllegalStateException("Sân đích đang bảo trì, không thể chuyển!");
        }

        String effectiveSlot = (newTimeSlot != null && !newTimeSlot.trim().isEmpty()) ? newTimeSlot.trim() : booking.getTimeSlot();

        // Check if target court has conflicting active booking
        List<Booking> conflicts = bookingRepository.findByCourtCodeAndBookingDate(targetCourtCode, booking.getBookingDate());
        boolean hasConflict = conflicts.stream().anyMatch(b -> 
            !b.getBookingCode().equalsIgnoreCase(bookingCode) &&
            b.getStatus() != null && !"Đã hủy".equalsIgnoreCase(b.getStatus()) &&
            isSlotOverlapping(effectiveSlot, b.getTimeSlot())
        );
        if (hasConflict) {
            throw new IllegalStateException("Sân đích đã được đặt trong khung giờ " + effectiveSlot + "!");
        }

        // Calculate price accurately based on duration
        BigDecimal oldHourly = booking.getHourlyPrice() != null ? booking.getHourlyPrice() : BigDecimal.ZERO;
        BigDecimal newHourly = targetCourt.getHourlyRate() != null ? targetCourt.getHourlyRate() : oldHourly;
        int[] hours = parseSlotHours(effectiveSlot);
        int duration = (hours[1] > hours[0]) ? (hours[1] - hours[0]) : 1;
        BigDecimal newTotalPrice = newHourly.multiply(BigDecimal.valueOf(duration));
        BigDecimal oldTotal = booking.getTotalPrice() != null ? booking.getTotalPrice() : oldHourly.multiply(BigDecimal.valueOf(duration));
        BigDecimal priceDifference = newTotalPrice.subtract(oldTotal);

        booking.setCourtCode(targetCourtCode);
        booking.setTimeSlot(effectiveSlot);
        booking.setHourlyPrice(newHourly);
        booking.setTotalPrice(newTotalPrice);
        booking.setNotes((booking.getNotes() != null ? booking.getNotes() + " | " : "") + 
                "Chuyển từ " + oldCourtCode + " sang " + targetCourtCode + " (Chênh lệch: " + priceDifference + "đ)");
        Booking updatedBooking = bookingRepository.save(booking);

        // Update court statuses and broadcast
        courtRepository.findByCourtCode(oldCourtCode).ifPresent(c -> {
            c.setStatus("Trống");
            courtRepository.save(c);
            courtService.updateCourtStatus(oldCourtCode, "Trống");
        });

        String newCourtStatus = "Đang sử dụng".equalsIgnoreCase(booking.getStatus()) ? "Đang sử dụng" : "Đã đặt trước";
        targetCourt.setStatus(newCourtStatus);
        courtRepository.save(targetCourt);
        courtService.updateCourtStatus(targetCourtCode, newCourtStatus);

        broadcastBookingEvent(BookingNotificationDto.builder()
                .eventType("COURT_TRANSFERRED")
                .bookingCode(bookingCode)
                .courtCode(targetCourtCode)
                .branchCode(booking.getBranchCode())
                .customerName(booking.getCustomerName())
                .timeSlot(effectiveSlot)
                .status(booking.getStatus())
                .message("Đã chuyển đơn [" + bookingCode + "] sang sân " + targetCourtCode)
                .timestamp(LocalDateTime.now())
                .build());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("bookingCode", bookingCode);
        result.put("oldCourtCode", oldCourtCode);
        result.put("newCourtCode", targetCourtCode);
        result.put("timeSlot", effectiveSlot);
        result.put("priceDifference", priceDifference);
        result.put("newTotalPrice", booking.getTotalPrice());
        result.put("message", "Chuyển sân thành công! " + (priceDifference.compareTo(BigDecimal.ZERO) > 0 ? "Thu thêm: " + priceDifference + "đ" : (priceDifference.compareTo(BigDecimal.ZERO) < 0 ? "Hoàn lại: " + priceDifference.abs() + "đ" : "Không chênh lệch giá")));
        return result;
    }

    public void broadcastBookingEvent(BookingNotificationDto event) {
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/bookings", event);
                messagingTemplate.convertAndSend("/topic/notifications", event);
                if (event.getBranchCode() != null && !event.getBranchCode().trim().isEmpty()) {
                    messagingTemplate.convertAndSend("/topic/bookings/" + event.getBranchCode(), event);
                }
            } catch (Exception e) {
                // Non-blocking fallback if socket broker is initializing
            }
        }
    }

    public boolean isSlotOverlapping(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        if (s1.trim().equalsIgnoreCase(s2.trim())) return true;
        if (s1.contains(",") || s2.contains(",")) {
            String[] parts1 = s1.split(",");
            String[] parts2 = s2.split(",");
            for (String p1 : parts1) {
                for (String p2 : parts2) {
                    if (isSingleSlotOverlapping(p1.trim(), p2.trim())) {
                        return true;
                    }
                }
            }
            return false;
        }
        return isSingleSlotOverlapping(s1.trim(), s2.trim());
    }

    private boolean isSingleSlotOverlapping(String s1, String s2) {
        if (s1.equalsIgnoreCase(s2)) return true;
        try {
            int[] r1 = parseSlotHours(s1);
            int[] r2 = parseSlotHours(s2);
            return Math.max(r1[0], r2[0]) < Math.min(r1[1], r2[1]);
        } catch (Exception e) {
            return s1.equalsIgnoreCase(s2);
        }
    }

    public int[] parseSlotHours(String slot) {
        Pattern p = Pattern.compile("(\\d{1,2}):\\d{2}\\s*-\\s*(\\d{1,2}):\\d{2}");
        Matcher m = p.matcher(slot);
        if (m.find()) {
            return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
        }
        return new int[]{0, 0};
    }
}
