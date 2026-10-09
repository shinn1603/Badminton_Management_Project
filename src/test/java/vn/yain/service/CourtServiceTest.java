package vn.yain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import vn.yain.dto.CourtEventDto;
import vn.yain.entity.Court;
import vn.yain.repository.CourtRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho CourtService (Bao phu Use Case XL-02 & XL-04)
 */
class CourtServiceTest {

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private CourtService courtService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Tra cuu danh sach san theo ma chi nhanh")
    void testGetCourts_ByBranch() {
        Court c1 = new Court();
        c1.setCourtCode("CL01");
        c1.setBranchCode("CN01");

        Court c2 = new Court();
        c2.setCourtCode("CL02");
        c2.setBranchCode("CN01");

        when(courtRepository.findByBranchCode("CN01")).thenReturn(Arrays.asList(c1, c2));

        List<Court> results = courtService.getCourts("CN01");

        assertEquals(2, results.size());
        assertEquals("CL01", results.get(0).getCourtCode());
        verify(courtRepository, times(1)).findByBranchCode("CN01");
    }

    @Test
    @DisplayName("Tra cuu toan bo san khi khong truyen branchCode")
    void testGetCourts_All() {
        when(courtRepository.findAll()).thenReturn(Arrays.asList(new Court(), new Court(), new Court()));

        List<Court> results = courtService.getCourts(null);

        assertEquals(3, results.size());
        verify(courtRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("XL-04: Cap nhat trang thai san thanh cong va phat WebSocket event")
    void testUpdateCourtStatus_Success() {
        Court court = new Court();
        court.setCourtCode("CL01");
        court.setBranchCode("CN01");
        court.setStatus("Trống");

        when(courtRepository.findByCourtCode("CL01")).thenReturn(Optional.of(court));
        when(courtRepository.save(any(Court.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Court updated = courtService.updateCourtStatus("CL01", "Đang sử dụng");

        assertNotNull(updated);
        assertEquals("Đang sử dụng", updated.getStatus());
        verify(courtRepository, times(1)).save(court);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/courts"), any(CourtEventDto.class));
    }

    @Test
    @DisplayName("Cap nhat trang thai san bat loi khi ma san khong ton tai")
    void testUpdateCourtStatus_NotFound() {
        when(courtRepository.findByCourtCode("UNKNOWN")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            courtService.updateCourtStatus("UNKNOWN", "Đang sử dụng");
        });

        assertTrue(ex.getMessage().contains("Không tìm thấy sân"));
    }

    @Test
    @DisplayName("XL-02: Tam khoa giu cho san trong 10 phut (600 giay)")
    void testHoldCourtForBooking_Success() {
        Court court = new Court();
        court.setCourtCode("CL02");
        court.setBranchCode("CN01");
        court.setStatus("Trống");
        court.setHourlyRate(new BigDecimal("100000"));

        when(courtRepository.findByCourtCode("CL02")).thenReturn(Optional.of(court));
        when(courtRepository.save(any(Court.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourtEventDto event = courtService.holdCourtForBooking("CL02", "CN01", "18:00 - 19:00", "Nguyen Thanh Tam");

        assertNotNull(event);
        assertEquals("COURT_HELD", event.getEventType());
        assertEquals("CL02", event.getCourtCode());
        assertEquals("Đang giữ chỗ", event.getStatus());
        assertEquals(600, event.getHoldRemainingSeconds());
        assertEquals("Nguyen Thanh Tam", event.getCustomerName());
        verify(courtRepository, times(1)).save(court);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/courts"), any(CourtEventDto.class));
    }
}
