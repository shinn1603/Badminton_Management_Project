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

    @Test
    void testCourtFacilitiesIntent() {
        IntentClassificationService.PredictionResult res = service.predict("sân có bãi đỗ xe ô tô và phòng tắm nóng lạnh không");
        assertEquals("COURT_FACILITIES", res.getIntent());
        assertFalse(res.isOutOfScope());
        assertTrue(res.getDefaultResponse().contains("thảm"));
    }

    @Test
    void testServicesEquipmentIntent() {
        IntentClassificationService.PredictionResult res = service.predict("ở đây có cho thuê vợt yonex và thuê giày không shop");
        assertEquals("SERVICES_EQUIPMENT", res.getIntent());
        assertFalse(res.isOutOfScope());
        assertTrue(res.getDefaultResponse().contains("vợt"));
    }

    @Test
    void testMembershipLoyaltyIntent() {
        IntentClassificationService.PredictionResult res = service.predict("làm sao để tích điểm lên hạng thành viên vàng và đổi voucher");
        assertEquals("MEMBERSHIP_LOYALTY", res.getIntent());
        assertFalse(res.isOutOfScope());
        assertTrue(res.getDefaultResponse().contains("điểm"));
    }

    @Test
    void testPaymentMethodsIntent() {
        IntentClassificationService.PredictionResult res = service.predict("công ty tôi muốn thanh toán và xuất hóa đơn vat đỏ có được ko");
        assertEquals("PAYMENT_METHODS", res.getIntent());
        assertFalse(res.isOutOfScope());
        assertTrue(res.getDefaultResponse().contains("hóa đơn"));
    }

    @Test
    void testLostAndFoundSupportIntent() {
        IntentClassificationService.PredictionResult res = service.predict("tôi để quên cái áo khoác và cây vợt ở sân chiều qua");
        assertEquals("LOST_FOUND_SUPPORT", res.getIntent());
        assertFalse(res.isOutOfScope());
        assertTrue(res.getDefaultResponse().contains("quên"));
    }

    @Test
    void testRulesRegulationsIntent() {
        IntentClassificationService.PredictionResult res = service.predict("nội quy sân mang giày gì có bắt buộc giày đế kếp non marking ko");
        assertEquals("RULES_REGULATIONS", res.getIntent());
        assertFalse(res.isOutOfScope());
        assertTrue(res.getDefaultResponse().contains("giày"));
    }
}
