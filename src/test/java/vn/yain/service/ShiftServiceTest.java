package vn.yain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import vn.yain.entity.Invoice;
import vn.yain.repository.InvoiceRepository;
import vn.yain.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit Test cho ShiftService (Bao phu Use Case XL-08: Ket ca thu ngan)
 */
class ShiftServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private ShiftService shiftService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("XL-08: Thong ke tong doanh thu tien mat va chuyen khoan trong ca")
    void testGetShiftSummary_Success() {
        // 1 hoa don tien mat 150,000d
        Invoice invCash = new Invoice();
        invCash.setStaffCode("NVQ01");
        invCash.setInvoiceDate(LocalDateTime.now());
        invCash.setPaymentMethod("Tiền mặt");
        invCash.setTotalPayment(new BigDecimal("150000"));

        // 1 hoa don chuyen khoan 200,000d
        Invoice invTransfer = new Invoice();
        invTransfer.setStaffCode("NVQ01");
        invTransfer.setInvoiceDate(LocalDateTime.now());
        invTransfer.setPaymentMethod("Chuyển khoản VietQR");
        invTransfer.setTotalPayment(new BigDecimal("200000"));

        when(invoiceRepository.findAllByOrderByInvoiceDateDesc()).thenReturn(Arrays.asList(invCash, invTransfer));
        when(paymentRepository.findAll()).thenReturn(Collections.emptyList());

        Map<String, Object> summary = shiftService.getShiftSummary("NVQ01", "CN01");

        assertNotNull(summary);
        assertEquals(2, summary.get("invoiceCount"));
        assertEquals(new BigDecimal("150000"), summary.get("systemCashTotal"));
        assertEquals(new BigDecimal("200000"), summary.get("systemTransferTotal"));
        assertEquals(new BigDecimal("350000"), summary.get("totalRevenue"));
    }

    @Test
    @DisplayName("XL-08: Dong ca thu ngan voi tien mat thuc te khop 100%")
    void testCloseShift_MatchedCash() {
        Invoice inv = new Invoice();
        inv.setStaffCode("NVQ01");
        inv.setInvoiceDate(LocalDateTime.now());
        inv.setPaymentMethod("Tiền mặt");
        inv.setTotalPayment(new BigDecimal("500000"));

        when(invoiceRepository.findAllByOrderByInvoiceDateDesc()).thenReturn(Collections.singletonList(inv));
        when(paymentRepository.findAll()).thenReturn(Collections.emptyList());

        // Kiem ke dung 500,000d
        Map<String, Object> receipt = shiftService.closeShift("NVQ01", "CN01", new BigDecimal("500000"), "Ket ca chieu");

        assertNotNull(receipt);
        assertTrue((boolean) receipt.get("success"));
        assertEquals(new BigDecimal("0"), receipt.get("difference"));
        assertEquals("CLOSED", receipt.get("status"));
        assertTrue(receipt.get("message").toString().contains("Khớp quỹ tiền mặt 100%"));
    }

    @Test
    @DisplayName("XL-08: Dong ca thu ngan ghi nhan chenh lech thieu quy tien mat")
    void testCloseShift_DeficitCash() {
        Invoice inv = new Invoice();
        inv.setStaffCode("NVQ01");
        inv.setInvoiceDate(LocalDateTime.now());
        inv.setPaymentMethod("Tiền mặt");
        inv.setTotalPayment(new BigDecimal("500000"));

        when(invoiceRepository.findAllByOrderByInvoiceDateDesc()).thenReturn(Collections.singletonList(inv));
        when(paymentRepository.findAll()).thenReturn(Collections.emptyList());

        // Kiem ke chi co 480,000d (thieu 20,000d)
        Map<String, Object> receipt = shiftService.closeShift("NVQ01", "CN01", new BigDecimal("480000"), "Thieu 20k");

        assertNotNull(receipt);
        assertTrue((boolean) receipt.get("success"));
        assertEquals(new BigDecimal("-20000"), receipt.get("difference"));
        assertTrue(receipt.get("message").toString().contains("Thiếu quỹ: -20000"));
    }
}
