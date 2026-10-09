package vn.yain.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho thuat toan kiem tra trung khung gio da gio (XL-02)
 */
class SlotOverlappingTest {

    private boolean invokeIsSlotOverlapping(String s1, String s2) throws Exception {
        ApiController controller = new ApiController();
        Method method = ApiController.class.getDeclaredMethod("isSlotOverlapping", String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(controller, s1, s2);
    }

    @Test
    @DisplayName("Khung gio trung nhau hoan toan phai bao trung lap")
    void testExactSameSlot() throws Exception {
        assertTrue(invokeIsSlotOverlapping("18:00 - 19:00", "18:00 - 19:00"));
        assertTrue(invokeIsSlotOverlapping("06:00 - 07:00", "06:00 - 07:00"));
    }

    @Test
    @DisplayName("Khung gio chong lan mot phan (vi du 18:00-20:00 voi 19:00-21:00)")
    void testPartialOverlap() throws Exception {
        assertTrue(invokeIsSlotOverlapping("18:00 - 20:00", "19:00 - 21:00"));
        assertTrue(invokeIsSlotOverlapping("17:00 - 19:00", "18:00 - 20:00"));
    }

    @Test
    @DisplayName("Khung gio lien ke nhau khong duoc tinh la trung (17:00-18:00 voi 18:00-19:00)")
    void testAdjacentSlotsNotOverlapping() throws Exception {
        assertFalse(invokeIsSlotOverlapping("17:00 - 18:00", "18:00 - 19:00"));
        assertFalse(invokeIsSlotOverlapping("08:00 - 09:00", "09:00 - 10:00"));
    }

    @Test
    @DisplayName("Hai khung gio tach biet hoan toan khong trung lap")
    void testDisjointSlots() throws Exception {
        assertFalse(invokeIsSlotOverlapping("06:00 - 07:00", "20:00 - 21:00"));
        assertFalse(invokeIsSlotOverlapping("09:00 - 10:00", "14:00 - 15:00"));
    }
}
