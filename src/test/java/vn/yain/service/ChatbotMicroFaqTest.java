package vn.yain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.yain.dto.ChatMessageDto;

import static org.junit.jupiter.api.Assertions.*;

public class ChatbotMicroFaqTest {

    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService();
        // Không inject Gemini, không inject DB để test tầng Micro-FAQ
    }

    @Test
    void testBathroomDetails() {
        ChatMessageDto reply = chatbotService.processMessage("phòng tắm mấy chỗ, có nước nóng không", "session_test");
        assertNotNull(reply);
        assertEquals("FAQ_BATHROOM_DETAIL", reply.getIntent());
        assertTrue(reply.getText().contains("4 - 6 buồng"));
        assertTrue(reply.getText().contains("Ariston"));
    }

    @Test
    void testSocksDetails() {
        ChatMessageDto reply = chatbotService.processMessage("tất là 1 đôi à bạn", "session_test");
        assertNotNull(reply);
        assertEquals("FAQ_SOCKS_DETAIL", reply.getIntent());
        assertTrue(reply.getText().contains("1 đôi"));
        assertTrue(reply.getText().contains("25.000 đ/đôi"));
    }

    @Test
    void testRacketDetails() {
        ChatMessageDto reply = chatbotService.processMessage("cho hỏi vợt cho thuê tên gì", "session_test");
        assertNotNull(reply);
        assertEquals("FAQ_RACKETS_DETAIL", reply.getIntent());
        assertTrue(reply.getText().contains("Astrox 88D Pro"));
        assertTrue(reply.getText().contains("100ZZ"));
        assertTrue(reply.getText().contains("10.5 kg"));
    }

    @Test
    void testShuttlecockDetails() {
        ChatMessageDto reply = chatbotService.processMessage("1 ống cầu tên gì và có mấy quả vậy", "session_test");
        assertNotNull(reply);
        assertEquals("FAQ_SHUTTLECOCK_DETAIL", reply.getIntent());
        assertTrue(reply.getText().contains("12 quả"));
        assertTrue(reply.getText().contains("Yonex Aerosensa AS-40"));
        assertTrue(reply.getText().contains("Thành Công 77"));
    }

    @Test
    void testParkingDetails() {
        ChatMessageDto reply = chatbotService.processMessage("gửi xe máy với ô tô có mất tiền không", "session_test");
        assertNotNull(reply);
        assertEquals("FAQ_PARKING_DETAIL", reply.getIntent());
        assertTrue(reply.getText().contains("MIỄN PHÍ 100%"));
    }

    @Test
    void testFoodAndDrinkDetails() {
        ChatMessageDto reply = chatbotService.processMessage("ở sân có bán mì ly với xúc xích không", "session_test");
        assertNotNull(reply);
        assertEquals("FAQ_FOOD_DRINK_DETAIL", reply.getIntent());
        assertTrue(reply.getText().contains("Mì ly Modern"));
        assertTrue(reply.getText().contains("Xúc xích"));
    }

    @Test
    void testCourtSpecsDetails() {
        ChatMessageDto reply = chatbotService.processMessage("chiều cao trần bao nhiêu mét và đèn mấy lux", "session_test");
        assertNotNull(reply);
        assertEquals("FAQ_COURT_SPECS_DETAIL", reply.getIntent());
        assertTrue(reply.getText().contains("9.5m"));
        assertTrue(reply.getText().contains("500 Lux"));
    }

    @Test
    void testAutoLockExplain() {
        ChatMessageDto reply = chatbotService.processMessage("chức năng tự khóa sân là gì", "session_test");
        assertNotNull(reply);
        assertEquals("FAQ_AUTO_LOCK_EXPLAIN", reply.getIntent());
        assertTrue(reply.getText().contains("Tự Khóa Sân"));
        assertTrue(reply.getText().contains("10 phút"));
        assertTrue(reply.getText().contains("Lịch Sử Đặt Sân"));
    }

    @Test
    void testBookingPromptNoTime() {
        ChatMessageDto reply = chatbotService.processMessage("tôi muốn đặt sân", "session_test");
        assertNotNull(reply);
        assertEquals("BOOKING_PROMPT", reply.getIntent());
        assertTrue(reply.getText().contains("khung giờ nào"));
        assertTrue(reply.getText().contains("10 phút"));
    }
}
