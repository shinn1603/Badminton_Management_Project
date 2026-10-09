package vn.yain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Booking> getBookingByCode(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode);
    }

    public Booking createBooking(Booking booking) {
        if (booking.getBookingCode() == null || booking.getBookingCode().trim().isEmpty()) {
            booking.setBookingCode("DS" + System.currentTimeMillis() % 1000000);
        }
        if (booking.getBookingDate() == null) {
            booking.setBookingDate(LocalDate.now());
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

        return saved;
    }

    /**
     * Confirm 30% deposit payment via VietQR (XL-03)
     */
    public Booking confirmDeposit(String bookingCode, BigDecimal depositAmount, String paymentMethod, String txRef) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt sân: " + bookingCode));

        booking.setStatus("Đã cọc 30%");
        booking.setDepositAmount(depositAmount != null ? depositAmount : booking.getDepositAmount());
        booking.setPaymentMethod(paymentMethod != null ? paymentMethod : "VietQR Pro");
        Booking updated = bookingRepository.save(booking);

        // Record payment transaction
        Payment payment = new Payment();
        payment.setPaymentCode("PAY" + System.currentTimeMillis() % 1000000);
        payment.setBookingCode(bookingCode);
        payment.setAmount(booking.getDepositAmount());
        payment.setMethod(booking.getPaymentMethod());
        payment.setTransactionType("Đặt cọc 30%");
        payment.setTransactionRef(txRef != null ? txRef : "VQR-" + bookingCode);
        payment.setStatus("Thành công");
        paymentRepository.save(payment);

        return updated;
    }

    /**
     * Check-in court at POS reception (XL-04)
     */
    public Booking checkIn(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn: " + bookingCode));

        booking.setStatus("Đang sử dụng");
        booking.setCheckInTime(LocalDateTime.now());
        Booking updated = bookingRepository.save(booking);

        courtRepository.findByCourtCode(booking.getCourtCode()).ifPresent(court -> {
            court.setStatus("Đang sử dụng");
            courtRepository.save(court);
            courtService.updateCourtStatus(court.getCourtCode(), "Đang sử dụng");
        });

        return updated;
    }

    /**
     * Check-out court, settle balance & generate invoice (XL-07)
     */
    public Invoice checkOut(String bookingCode, BigDecimal extraEquipmentFee, BigDecimal extraProductFee, String paymentMethod, String staffCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn: " + bookingCode));

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
        invoice.setInvoiceCode("HD" + System.currentTimeMillis() % 1000000);
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

        return savedInvoice;
    }

    /**
     * Cancel booking
     */
    public Booking cancelBooking(String bookingCode, String reason) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn: " + bookingCode));

        booking.setStatus("Đã hủy");
        booking.setNotes((booking.getNotes() != null ? booking.getNotes() + " | " : "") + "Hủy: " + reason);
        Booking saved = bookingRepository.save(booking);

        courtRepository.findByCourtCode(booking.getCourtCode()).ifPresent(court -> {
            courtService.updateCourtStatus(court.getCourtCode(), "Trống");
        });

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
            effectiveSlot.equalsIgnoreCase(b.getTimeSlot())
        );
        if (hasConflict) {
            throw new IllegalStateException("Sân đích đã được đặt trong khung giờ " + effectiveSlot + "!");
        }

        // Calculate price difference
        BigDecimal oldHourly = booking.getHourlyPrice() != null ? booking.getHourlyPrice() : BigDecimal.ZERO;
        BigDecimal newHourly = targetCourt.getHourlyRate() != null ? targetCourt.getHourlyRate() : oldHourly;
        BigDecimal priceDifference = newHourly.subtract(oldHourly);

        booking.setCourtCode(targetCourtCode);
        booking.setTimeSlot(effectiveSlot);
        booking.setHourlyPrice(newHourly);
        if (booking.getTotalPrice() != null) {
            booking.setTotalPrice(booking.getTotalPrice().add(priceDifference));
        } else {
            booking.setTotalPrice(newHourly);
        }
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
}
