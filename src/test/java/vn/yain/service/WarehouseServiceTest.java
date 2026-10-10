package vn.yain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import vn.yain.entity.Booking;
import vn.yain.entity.Product;
import vn.yain.entity.StockReceipt;
import vn.yain.repository.BookingRepository;
import vn.yain.repository.ProductRepository;
import vn.yain.repository.StockReceiptRepository;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho WarehouseService (Bao phu Use Case XL-05 & XL-10)
 */
class WarehouseServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockReceiptRepository stockReceiptRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("XL-05: Ban hang dich vu phat sinh tai POS, tru ton kho va cong vao don dat san")
    void testOrderServiceForBooking_Success() {
        Product water = new Product();
        water.setProductCode("SP_REVIVE");
        water.setProductName("Nước bù khoáng Revive");
        water.setUnitPrice(new BigDecimal("15000"));
        water.setStockQuantity(20);
        water.setUnit("chai");

        Booking booking = new Booking();
        booking.setBookingCode("DS123456");
        booking.setTotalPrice(new BigDecimal("100000"));

        when(productRepository.findByProductCode("SP_REVIVE")).thenReturn(Optional.of(water));
        when(bookingRepository.findByBookingCode("DS123456")).thenReturn(Optional.of(booking));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Khach goi 2 chai Revive (tong 30,000d)
        Map<String, Object> res = warehouseService.orderServiceForBooking("DS123456", "SP_REVIVE", 2);

        assertNotNull(res);
        assertTrue((boolean) res.get("success"));
        assertEquals(18, water.getStockQuantity()); // 20 - 2 = 18
        assertEquals(new BigDecimal("30000"), res.get("itemTotal"));
        assertEquals(new BigDecimal("130000"), booking.getTotalPrice()); // 100,000 + 30,000 = 130,000
        verify(productRepository, times(1)).save(water);
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("XL-05: Goi dich vu bat loi khi ton kho khong du so luong yeu cau")
    void testOrderServiceForBooking_OutOfStock() {
        Product shuttlecock = new Product();
        shuttlecock.setProductCode("SP_YONEX_TUBE");
        shuttlecock.setProductName("Ống cầu Yonex AS40");
        shuttlecock.setStockQuantity(1);
        shuttlecock.setUnit("ống");

        when(productRepository.findByProductCode("SP_YONEX_TUBE")).thenReturn(Optional.of(shuttlecock));

        // Khach muon mua 5 ong nhung kho chi con 1
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            warehouseService.orderServiceForBooking("DS123456", "SP_YONEX_TUBE", 5);
        });

        assertTrue(ex.getMessage().contains("Số lượng tồn kho không đủ"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("XL-10: Lap phieu nhap kho tang ton kho san pham va cap nhat gia von chinh xac")
    void testCreateStockReceipt_Success() {
        Product grip = new Product();
        grip.setProductCode("SP_GRIP");
        grip.setProductName("Quấn cán Yonex AC102EX");
        grip.setStockQuantity(10);
        grip.setCostPrice(new BigDecimal("25000"));

        when(productRepository.findByProductCode("SP_GRIP")).thenReturn(Optional.of(grip));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockReceiptRepository.save(any(StockReceipt.class))).thenAnswer(inv -> inv.getArgument(0));

        // Nhap kho 50 chiec quan can voi gia 28,000d/chiec -> Tong: 1,400,000d
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("productCode", "SP_GRIP");
        item.put("quantity", 50);
        item.put("importPrice", new BigDecimal("28000"));
        items.add(item);

        StockReceipt receipt = warehouseService.createStockReceipt("Yonex Sunrise VN", "CN01", "NVQ01", items, "Nhap lo quan can thang 10");

        assertNotNull(receipt);
        assertNotNull(receipt.getReceiptCode());
        assertEquals(new BigDecimal("1400000"), receipt.getTotalAmount());
        assertEquals("Yonex Sunrise VN", receipt.getSupplierName());

        // Kiem tra hang hoa da duoc cong them vao kho va tinh dung gia von binh quan gia quyen
        assertEquals(60, grip.getStockQuantity()); // 10 + 50 = 60
        assertEquals(new BigDecimal("27500.00"), grip.getCostPrice());
        verify(productRepository, times(1)).save(grip);
        verify(stockReceiptRepository, times(1)).save(any(StockReceipt.class));
    }
}
