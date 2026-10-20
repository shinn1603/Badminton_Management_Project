package vn.yain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.yain.dto.ChatMessageDto;
import vn.yain.entity.Branch;
import vn.yain.entity.Court;
import vn.yain.repository.BookingRepository;
import vn.yain.repository.BranchRepository;
import vn.yain.repository.CourtRepository;
import vn.yain.repository.CustomerRepository;
import vn.yain.repository.TournamentRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatbotBookingFlowTest {

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BookingService bookingService;

    @Mock
    private CourtService courtService;

    @Mock
    private PricingService pricingService;

    @InjectMocks
    private ChatbotService chatbotService;

    private Court sampleCourt;
    private Branch sampleBranch;

    @BeforeEach
    void setUp() {
        sampleBranch = new Branch();
        sampleBranch.setBranchCode("CN01");
        sampleBranch.setBranchName("Chi nhánh 1 (Thủ Đức - Số 1 Võ Văn Ngân)");

        sampleCourt = new Court();
        sampleCourt.setCourtCode("CL01");
        sampleCourt.setBranchCode("CN01");
        sampleCourt.setCourtName("Sân 1 (VIP)");
        sampleCourt.setCourtType("VIP");
        sampleCourt.setStatus("Trống");
        sampleCourt.setHourlyRate(new BigDecimal("140000"));

        lenient().when(branchRepository.findAll()).thenReturn(List.of(sampleBranch));
        lenient().when(courtRepository.findByBranchCode("CN01")).thenReturn(List.of(sampleCourt));
        lenient().when(courtRepository.findByCourtCode("CL01")).thenReturn(Optional.of(sampleCourt));
        lenient().when(pricingService.calculateHourlyRate(any(), anyInt())).thenReturn(new BigDecimal("140000"));
        lenient().when(pricingService.calculateTotalPrice(any(), anyInt())).thenReturn(new BigDecimal("280000"));
        lenient().when(pricingService.calculateDepositAmount(any())).thenReturn(new BigDecimal("84000"));
        lenient().when(bookingService.generateUniqueBookingCode()).thenReturn("DS123456");
        lenient().when(bookingService.isSlotOverlapping(anyString(), anyString())).thenReturn(false);
    }

    @Test
    void testBookingWithoutInfo_ShouldAskSlotAndBranch_AndNotLockCourt() {
        String session = "test_sess_1";
        ChatMessageDto reply = chatbotService.processMessage("tôi muốn đặt sân", session);

        assertNotNull(reply);
        assertEquals("BOOKING_PROMPT", reply.getIntent());
        assertNotEquals("OPEN_PAYMENT", reply.getActionType());
        assertTrue(reply.getText().contains("khung giờ nào"));
    }

    @Test
    void testStepByStepCollection_EnsureAll6FieldsBeforeLocking() {
        String session = "test_sess_step";

        // Bước 1: Khách chọn chi nhánh & giờ
        ChatMessageDto step1 = chatbotService.processMessage("Đặt sân ở chi nhánh 1 từ 18h đến 20h hôm nay", session);
        assertNotNull(step1);
        assertNotEquals("OPEN_PAYMENT", step1.getActionType(), "Chưa đủ 6 thông tin, không được hiện mã khóa sân!");

        // Bước 2: Khách chọn sân VIP
        ChatMessageDto step2 = chatbotService.processMessage("Cho tôi đặt sân VIP", session);
        assertNotNull(step2);
        assertNotEquals("OPEN_PAYMENT", step2.getActionType(), "Chưa hỏi dịch vụ đi kèm & tên/SĐT, không được hiện mã khóa sân!");
        assertTrue(step2.getText().contains("dịch vụ") || step2.getText().contains("phụ kiện"));

        // Bước 3: Khách trả lời về dịch vụ đi kèm
        ChatMessageDto step3 = chatbotService.processMessage("Không lấy thêm gì", session);
        assertNotNull(step3);
        assertNotEquals("OPEN_PAYMENT", step3.getActionType(), "Chưa có tên và SĐT, không được hiện mã khóa sân!");
        assertTrue(step3.getText().contains("Họ tên") || step3.getText().contains("Số điện thoại"));

        // Bước 4: Khách cung cấp tên và SĐT
        ChatMessageDto step4 = chatbotService.processMessage("Mình tên Tuấn Anh, SĐT 0987654321", session);
        assertNotNull(step4);
        assertNotEquals("OPEN_PAYMENT", step4.getActionType(), "Vừa đủ thông tin, phải hiện bảng tóm tắt xác nhận trước khi khóa sân!");
        assertEquals("AWAITING_CONFIRMATION", step4.getIntent());
        assertTrue(step4.getText().contains("Tuấn Anh"));
        assertTrue(step4.getText().contains("0987654321"));
        assertTrue(step4.getText().contains("Chi nhánh 1"));
        assertTrue(step4.getText().contains("Sân 1 (VIP)"));

        // Bước 5: Khách xác nhận chốt
        ChatMessageDto step5 = chatbotService.processMessage("Xác nhận và xuất mã khóa sân", session);
        assertNotNull(step5);
        assertEquals("OPEN_PAYMENT", step5.getActionType(), "Đã đủ 6 thông tin và khách xác nhận -> PHẢI HIỆN MÃ KHÓA SÂN!");
        assertNotNull(step5.getPaymentData());
        assertEquals("DS123456", step5.getPaymentData().get("bookingCode"));
        assertTrue(step5.getText().contains("MÃ KHÓA SÂN GIỮ CHỖ CỦA BẠN ĐÃ ĐƯỢC XUẤT THÀNH CÔNG"));
    }

    @Test
    void testOneShotBooking_WithAll6Fields_ShouldDirectlyLockAndShowCode() {
        String session = "test_sess_oneshot";
        // Cung cấp đầy đủ: Tên, SĐT, Chi nhánh, Sân, Giờ, Dịch vụ đi kèm + Yêu cầu khóa sân
        String input = "Tôi tên Nguyễn Văn Nam, SĐT 0912345678, muốn đặt sân VIP ở chi nhánh 1 từ 18h đến 20h hôm nay, không chọn thêm gì, khóa sân giùm tôi";
        
        ChatMessageDto reply = chatbotService.processMessage(input, session);

        assertNotNull(reply);
        assertEquals("OPEN_PAYMENT", reply.getActionType(), "Đầy đủ 6 thông tin và yêu cầu khóa sân -> Hiện mã khóa sân ngay!");
        assertNotNull(reply.getPaymentData());
        assertEquals("DS123456", reply.getPaymentData().get("bookingCode"));
        assertTrue(reply.getText().contains("DS123456"));
        assertTrue(reply.getText().contains("Nguyễn Văn Nam"));
        assertTrue(reply.getText().contains("0912345678"));
    }
}
