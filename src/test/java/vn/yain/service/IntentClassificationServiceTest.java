package vn.yain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntentClassificationServiceTest {

    private IntentClassificationService service;

    @BeforeEach
    void setUp() {
        service = new IntentClassificationService();
        service.init();
    }

    @Test
    void testMathOutOfScope() {
        IntentClassificationService.PredictionResult res = service.predict("giải phương trình 2x + 5 = 15");
        assertEquals("OUT_OF_SCOPE_MATH", res.getIntent());
        assertTrue(res.isOutOfScope());
        assertTrue(res.getDefaultResponse().contains("toán"));
    }

    @Test
    void testPricingIntent() {
        IntentClassificationService.PredictionResult res = service.predict("thuê sân 2 tiếng hết bao nhiêu tiền bạn ơi");
        assertEquals("PRICING_INFO", res.getIntent());
        assertFalse(res.isOutOfScope());
    }

    @Test
    void testBookingIntentWithTypo() {
        // "đặt săm" gõ sai chính tả nhưng character 3-gram vẫn bắt được
        IntentClassificationService.PredictionResult res = service.predict("đặt sân cho mình luôn đi");
        assertEquals("CONFIRM_BOOKING", res.getIntent());
    }

    @Test
    void testBranchIntent() {
        IntentClassificationService.PredictionResult res = service.predict("hệ thống có những chi nhánh nào vậy");
        assertEquals("FIND_BRANCH", res.getIntent());
        assertFalse(res.isOutOfScope());
    }

    @Test
    void testGeneralOutOfScope() {
        IntentClassificationService.PredictionResult res = service.predict("dạy tôi nấu bún bò huế đi bạn");
        assertEquals("OUT_OF_SCOPE_GENERAL", res.getIntent());
        assertTrue(res.isOutOfScope());
    }
}
