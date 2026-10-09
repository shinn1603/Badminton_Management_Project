package vn.yain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho BookingService (Bao phu Use Case XL-02, XL-03, XL-04, XL-07)
 */
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CourtService courtService;

    @Mock
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("XL-02: Tao don dat san thanh cong va tu tinh 30% tien coc")
    void testCreateBooking_Success() {
        Booking input = new Booking();
        input.setCourtCode("CL01");
        input.setBranchCode("CN01");
        input.setTimeSlot("18:00 - 19:00");
        input.setTotalPrice(new BigDecimal("100000"));
        input.setCustomerName("Nguyen Phuoc Tho");

        Court mockCourt = new Court();
        mockCourt.setCourtCode("CL01");
        mockCourt.setStatus("Trống");

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courtRepository.findByCourtCode("CL01")).thenReturn(Optional.of(mockCourt));

        Booking created = bookingService.createBooking(input);

        assertNotNull(created);
        assertNotNull(created.getBookingCode());
        assertEquals("Chờ cọc 30%", created.getStatus());
        assertEquals(0, created.getDepositAmount().compareTo(new BigDecimal("30000")));
        assertEquals("Đã đặt trước", mockCourt.getStatus());
        verify(courtRepository, times(1)).save(mockCourt);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("XL-03: Xac nhan thanh toan tien coc 30% qua VietQR va tao giao dich Payment")
    void testConfirmDeposit_Success() {
        Booking booking = new Booking();
        booking.setBookingCode("DS123456");
        booking.setStatus("Chờ cọc 30%");
        booking.setDepositAmount(new BigDecimal("30000"));

        when(bookingRepository.findByBookingCode("DS123456")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking updated = bookingService.confirmDeposit("DS123456", new BigDecimal("30000"), "VietQR Pro", "VQR-TX999");

        assertNotNull(updated);
        assertEquals("Đã cọc 30%", updated.getStatus());
        assertEquals("VietQR Pro", updated.getPaymentMethod());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("XL-03: Xac nhan tien coc that bai khi ma dat san khong ton tai")
    void testConfirmDeposit_NotFound() {
        when(bookingRepository.findByBookingCode("DS000000")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.confirmDeposit("DS000000", new BigDecimal("30000"), "VietQR", "VQR-000");
        });
    }

    @Test
    @DisplayName("XL-04: Check-in nhan san tai quay POS va cap nhat trang thai san thanh Dang su dung")
    void testCheckIn_Success() {
        Booking booking = new Booking();
        booking.setBookingCode("DS123456");
        booking.setCourtCode("CL03");
        booking.setStatus("Đã cọc 30%");

        Court court = new Court();
        court.setCourtCode("CL03");
        court.setStatus("Đã đặt trước");

        when(bookingRepository.findByBookingCode("DS123456")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courtRepository.findByCourtCode("CL03")).thenReturn(Optional.of(court));

        Booking checkedIn = bookingService.checkIn("DS123456");

        assertNotNull(checkedIn);
        assertEquals("Đang sử dụng", checkedIn.getStatus());
        assertNotNull(checkedIn.getCheckInTime());
        assertEquals("Đang sử dụng", court.getStatus());
        verify(courtRepository, times(1)).save(court);
        verify(courtService, times(1)).updateCourtStatus("CL03", "Đang sử dụng");
    }

    @Test
    @DisplayName("XL-07: Check-out quyet toan hoa don, khau tru tien coc va giai phong san ve Trang thai Trong")
    void testCheckOut_Success() {
        Booking booking = new Booking();
        booking.setBookingCode("DS123456");
        booking.setCourtCode("CL04");
        booking.setStatus("Đang sử dụng");
        booking.setTotalPrice(new BigDecimal("120000")); // Tien gio
        booking.setDepositAmount(new BigDecimal("36000")); // Da coc 30%

        Court court = new Court();
        court.setCourtCode("CL04");
        court.setStatus("Đang sử dụng");

        when(bookingRepository.findByBookingCode("DS123456")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courtRepository.findByCourtCode("CL04")).thenReturn(Optional.of(court));

        // Phat sinh them: Thue vot 20,000 + Nuoc uong 30,000
        BigDecimal eqFee = new BigDecimal("20000");
        BigDecimal prodFee = new BigDecimal("30000");

        // Tong con lai phai thu = 120,000 + 20,000 + 30,000 - 36,000 = 134,000
        Invoice invoice = bookingService.checkOut("DS123456", eqFee, prodFee, "Tiền mặt", "NVQ01");

        assertNotNull(invoice);
        assertEquals("DS123456", invoice.getBookingCode());
        assertEquals(new BigDecimal("120000"), invoice.getCourtFee());
        assertEquals(new BigDecimal("20000"), invoice.getEquipmentFee());
        assertEquals(new BigDecimal("30000"), invoice.getProductFee());
        assertEquals(new BigDecimal("36000"), invoice.getDepositPaid());
        assertEquals(new BigDecimal("134000"), invoice.getTotalPayment());
        assertEquals("Đã thanh toán", invoice.getStatus());
        assertEquals("NVQ01", invoice.getStaffCode());

        assertEquals("Hoàn thành", booking.getStatus());
        assertNotNull(booking.getCheckOutTime());
        verify(courtService, times(1)).updateCourtStatus("CL04", "Trống");
    }

    @Test
    @DisplayName("Huy don dat san va giai phong san cho khach khac")
    void testCancelBooking_Success() {
        Booking booking = new Booking();
        booking.setBookingCode("DS123456");
        booking.setCourtCode("CL05");
        booking.setStatus("Chờ cọc 30%");

        Court court = new Court();
        court.setCourtCode("CL05");
        court.setStatus("Đã đặt trước");

        when(bookingRepository.findByBookingCode("DS123456")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courtRepository.findByCourtCode("CL05")).thenReturn(Optional.of(court));

        Booking cancelled = bookingService.cancelBooking("DS123456", "Bận việc đột xuất");

        assertNotNull(cancelled);
        assertEquals("Đã hủy", cancelled.getStatus());
        assertTrue(cancelled.getNotes().contains("Hủy: Bận việc đột xuất"));
        verify(courtService, times(1)).updateCourtStatus("CL05", "Trống");
    }

    @Test
    @DisplayName("XL-06: Chuyen sang san VIP thanh cong va tinh dung chenh lech gia bu tien")
    void testTransferCourt_Success() {
        Booking booking = new Booking();
        booking.setBookingCode("DS_TRANSFER");
        booking.setCourtCode("CL01");
        booking.setTimeSlot("18:00 - 19:00");
        booking.setHourlyPrice(new BigDecimal("90000"));
        booking.setTotalPrice(new BigDecimal("90000"));
        booking.setStatus("Đang sử dụng");
        booking.setBookingDate(LocalDate.now());

        Court oldCourt = new Court();
        oldCourt.setCourtCode("CL01");
        oldCourt.setStatus("Đang sử dụng");

        Court targetCourt = new Court();
        targetCourt.setCourtCode("CL06"); // San VIP Yonex
        targetCourt.setHourlyRate(new BigDecimal("120000"));
        targetCourt.setStatus("Trống");

        when(bookingRepository.findByBookingCode("DS_TRANSFER")).thenReturn(Optional.of(booking));
        when(courtRepository.findByCourtCode("CL01")).thenReturn(Optional.of(oldCourt));
        when(courtRepository.findByCourtCode("CL06")).thenReturn(Optional.of(targetCourt));
        when(bookingRepository.findByCourtCodeAndBookingDate(eq("CL06"), any())).thenReturn(java.util.Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> res = bookingService.transferCourt("DS_TRANSFER", "CL06", "18:00 - 19:00");

        assertNotNull(res);
        assertTrue((boolean) res.get("success"));
        assertEquals("CL06", res.get("newCourtCode"));
        // 120,000 - 90,000 = 30,000 chenh lech bu them
        assertEquals(new BigDecimal("30000"), res.get("priceDifference"));
        assertEquals(new BigDecimal("120000"), res.get("newTotalPrice"));
        verify(courtService, times(1)).updateCourtStatus("CL01", "Trống");
        verify(courtService, times(1)).updateCourtStatus("CL06", "Đang sử dụng");
    }

    @Test
    @DisplayName("XL-06: Chuyen san that bai khi san dich dang bao tri")
    void testTransferCourt_TargetMaintenance() {
        Booking booking = new Booking();
        booking.setBookingCode("DS_MAINTENANCE");
        booking.setCourtCode("CL01");
        booking.setTimeSlot("18:00 - 19:00");

        Court targetCourt = new Court();
        targetCourt.setCourtCode("CL06");
        targetCourt.setStatus("Bảo trì");

        when(bookingRepository.findByBookingCode("DS_MAINTENANCE")).thenReturn(Optional.of(booking));
        when(courtRepository.findByCourtCode("CL06")).thenReturn(Optional.of(targetCourt));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            bookingService.transferCourt("DS_MAINTENANCE", "CL06", "18:00 - 19:00");
        });

        assertTrue(ex.getMessage().contains("đang bảo trì"));
    }

    @Test
    @DisplayName("WebSocket: Phat song su kien dat san qua kenh /topic/bookings")
    void testWebSocketBroadcast_OnCreateBooking() {
        Booking booking = new Booking();
        booking.setBookingCode("DS_WS_TEST");
        booking.setCourtCode("CL01");
        booking.setBranchCode("CN01");
        booking.setCustomerName("Khach Hang WS");
        booking.setTotalPrice(new BigDecimal("100000"));

        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(courtRepository.findByCourtCode("CL01")).thenReturn(Optional.empty());

        bookingService.createBooking(booking);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/bookings"), any(Object.class));
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/notifications"), any(Object.class));
    }
}
