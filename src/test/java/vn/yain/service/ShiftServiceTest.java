package vn.yain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import vn.yain.entity.Invoice;
import vn.yain.entity.Staff;
import vn.yain.entity.WorkShift;
import vn.yain.repository.InvoiceRepository;
import vn.yain.repository.PaymentRepository;
import vn.yain.repository.StaffRepository;
import vn.yain.repository.WorkShiftRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho ShiftService (Bao phu Use Case XL-08: Ket ca thu ngan, Quan ly Nhan su & Phan ca)
 */
class ShiftServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private WorkShiftRepository workShiftRepository;

    @InjectMocks
    private ShiftService shiftService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("XL-08: Thong ke tong doanh thu tien mat va chuyen khoan trong ca")
    void testGetShiftSummary_Success() {
        Invoice invCash = new Invoice();
        invCash.setStaffCode("NVQ01");
        invCash.setInvoiceDate(LocalDateTime.now());
        invCash.setPaymentMethod("Tiền mặt");
        invCash.setTotalPayment(new BigDecimal("150000"));

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

        Map<String, Object> receipt = shiftService.closeShift("NVQ01", "CN01", new BigDecimal("480000"), "Thieu 20k");

        assertNotNull(receipt);
        assertTrue((boolean) receipt.get("success"));
        assertEquals(new BigDecimal("-20000"), receipt.get("difference"));
        assertTrue(receipt.get("message").toString().contains("Thiếu quỹ: -20000"));
    }

    @Test
    @DisplayName("Staff CRUD: Lay danh sach nhan su va them moi nhan vien")
    void testStaffCrud_Success() {
        Staff s = Staff.builder()
                .staffCode("NV-99")
                .branchCode("CN01")
                .fullName("Nguyen Van A")
                .position("Thu ngan")
                .phone("0901234567")
                .status("Đang làm việc")
                .build();

        when(staffRepository.count()).thenReturn(1L);
        when(staffRepository.findByBranchCode("CN01")).thenReturn(Collections.singletonList(s));
        when(staffRepository.save(any(Staff.class))).thenReturn(s);

        List<Staff> list = shiftService.getAllStaff("CN01");
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("NV-99", list.get(0).getStaffCode());

        Staff saved = shiftService.saveStaff(s);
        assertNotNull(saved);
        assertEquals("NV-99", saved.getStaffCode());
    }

    @Test
    @DisplayName("WorkShift: Phan ca truc va go nhan vien khoi ca")
    void testWorkShift_AssignAndRemove() {
        shiftService.assignShift("mon", "morning", "NV-02", "POS", "POS", "CN01");
        verify(workShiftRepository, times(1)).save(any(WorkShift.class));

        shiftService.removeShift("mon", "morning", "NV-02");
        verify(workShiftRepository, times(2)).deleteByDayOfWeekAndShiftTypeAndStaffCode("mon", "morning", "NV-02");
    }

    @Test
    @DisplayName("WorkShift: Khong cho phep 2 quan ly trong cung 1 ca truc")
    void testAssignShift_CannotAssignTwoManagersToSameShift() {
        WorkShift existingMgrShift = WorkShift.builder()
                .shiftCode("WS-MON-MOR-NS02")
                .staffCode("NS02")
                .branchCode("CN01")
                .dayOfWeek("mon")
                .shiftType("morning")
                .duty("Quản lý")
                .icon("MGR")
                .build();

        Staff existingStaff = Staff.builder()
                .staffCode("NS02")
                .fullName("Trần Phúc Bảo")
                .position("Quản lý chi nhánh")
                .build();

        when(workShiftRepository.findByBranchCodeAndDayOfWeekAndShiftType("CN01", "mon", "morning"))
                .thenReturn(Collections.singletonList(existingMgrShift));
        when(staffRepository.findByStaffCode("NS02")).thenReturn(Optional.of(existingStaff));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            shiftService.assignShift("mon", "morning", "Test-QL", "Quản lý", "MGR", "CN01");
        });

        assertTrue(ex.getMessage().contains("Mỗi ca trực chỉ được bố trí tối đa 1 Quản lý"));
    }

    @Test
    @DisplayName("WorkShift: Cho phep phan cong Quan ly khi ca chua co quan ly nao")
    void testAssignShift_AllowManagerWhenNoExistingManager() {
        when(workShiftRepository.findByBranchCodeAndDayOfWeekAndShiftType("CN01", "mon", "morning"))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> {
            shiftService.assignShift("mon", "morning", "NS02", "Quản lý", "MGR", "CN01");
        });

        verify(workShiftRepository, atLeastOnce()).save(any(WorkShift.class));
    }

    @Test
    @DisplayName("WorkShift: Tu choi luu lich tuan neu co ca bi trung 2 quan ly")
    void testSaveWeeklySchedule_RejectsMultipleManagersInSameShift() {
        Map<String, Object> scheduleMap = new HashMap<>();
        Map<String, Object> monday = new HashMap<>();
        List<Map<String, Object>> morningShifts = List.of(
                Map.of("staffId", "NS02", "duty", "Quản lý", "icon", "MGR"),
                Map.of("staffId", "Test-QL", "duty", "Quản lý", "icon", "MGR")
        );
        monday.put("morning", morningShifts);
        monday.put("night", Collections.emptyList());
        scheduleMap.put("mon", monday);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            shiftService.saveWeeklySchedule(scheduleMap, "CN01");
        });

        assertTrue(ex.getMessage().contains("Mỗi ca trực chỉ được bố trí tối đa 1 Quản lý"));
    }
}
