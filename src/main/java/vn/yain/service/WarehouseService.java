package vn.yain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.yain.entity.Booking;
import vn.yain.entity.Product;
import vn.yain.entity.StockReceipt;
import vn.yain.repository.BookingRepository;
import vn.yain.repository.ProductRepository;
import vn.yain.repository.StockReceiptRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service quan ly kho hang & Dich vu phat sinh (Bao phu Use Case XL-05 & XL-10)
 */
@Service
@Transactional
public class WarehouseService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockReceiptRepository stockReceiptRepository;

    @Autowired
    private BookingRepository bookingRepository;

    /**
     * XL-05: Ban hang & Dich vu phat sinh tai quay POS (Tru ton kho tu dong)
     */
    public Map<String, Object> orderServiceForBooking(String bookingCode, String productCode, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng gọi dịch vụ phải lớn hơn 0!");
        }

        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm có mã: " + productCode));

        if (product.getStockQuantity() < quantity) {
            throw new IllegalStateException("Số lượng tồn kho không đủ! Sản phẩm " + product.getProductName() +
                    " hiện chỉ còn " + product.getStockQuantity() + " " + product.getUnit());
        }

        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt sân: " + bookingCode));

        // Trừ tồn kho
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);

        // Tính thành tiền và cộng vào đơn đặt sân
        BigDecimal itemTotal = product.getUnitPrice().multiply(new BigDecimal(quantity));
        if (booking.getTotalPrice() != null) {
            booking.setTotalPrice(booking.getTotalPrice().add(itemTotal));
        } else {
            booking.setTotalPrice(itemTotal);
        }

        String itemNote = quantity + "x " + product.getProductName() + " (" + itemTotal + "đ)";
        booking.setNotes((booking.getNotes() != null ? booking.getNotes() + " | " : "") + "+ DV: " + itemNote);
        Booking updatedBooking = bookingRepository.save(booking);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("bookingCode", bookingCode);
        result.put("productCode", productCode);
        result.put("productName", product.getProductName());
        result.put("quantity", quantity);
        result.put("unitPrice", product.getUnitPrice());
        result.put("itemTotal", itemTotal);
        result.put("remainingStock", product.getStockQuantity());
        result.put("newBookingTotal", updatedBooking.getTotalPrice());
        result.put("message", "Đã thêm dịch vụ: " + itemNote);
        return result;
    }

    /**
     * XL-10: Lap phieu nhap kho hang hoa phu kien & Do uong chi nhanh
     */
    public StockReceipt createStockReceipt(String supplierName, String branchCode, String staffCode,
                                           List<Map<String, Object>> items, String notes) {
        if (supplierName == null || supplierName.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên nhà cung cấp!");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Danh sách hàng hóa nhập kho không được để trống!");
        }

        String receiptCode = "PNK" + (100000 + (int)(Math.random() * 900000));
        BigDecimal totalAmount = BigDecimal.ZERO;
        StringBuilder detailNotes = new StringBuilder();

        for (Map<String, Object> item : items) {
            String productCode = (String) item.get("productCode");
            int quantity = Integer.parseInt(item.get("quantity").toString());
            BigDecimal importPrice = new BigDecimal(item.get("importPrice").toString());

            if (quantity <= 0 || importPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Số lượng và giá nhập của từng sản phẩm phải hợp lệ!");
            }

            Product product = productRepository.findByProductCode(productCode)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm mã: " + productCode));

            // Tăng tồn kho và cập nhật giá vốn bình quân gia quyền (BUG-MED-26)
            int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            BigDecimal currentCost = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            int newTotalStock = currentStock + quantity;
            if (newTotalStock > 0 && currentStock > 0 && currentCost.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal oldStockVal = currentCost.multiply(new BigDecimal(currentStock));
                BigDecimal newStockVal = importPrice.multiply(new BigDecimal(quantity));
                BigDecimal weightedAvgCost = oldStockVal.add(newStockVal)
                        .divide(new BigDecimal(newTotalStock), 2, java.math.RoundingMode.HALF_UP);
                product.setCostPrice(weightedAvgCost);
            } else {
                product.setCostPrice(importPrice);
            }
            product.setStockQuantity(newTotalStock);
            productRepository.save(product);

            BigDecimal lineTotal = importPrice.multiply(new BigDecimal(quantity));
            totalAmount = totalAmount.add(lineTotal);

            if (detailNotes.length() > 0) detailNotes.append(", ");
            detailNotes.append(product.getProductName()).append(" x").append(quantity);
        }

        StockReceipt receipt = new StockReceipt();
        receipt.setReceiptCode(receiptCode);
        receipt.setBranchCode(branchCode != null ? branchCode : "CN01");
        receipt.setStaffCode(staffCode != null ? staffCode : "NS03");
        receipt.setSupplierName(supplierName.trim());
        receipt.setReceiptDate(LocalDateTime.now());
        receipt.setTotalAmount(totalAmount);
        receipt.setNotes((notes != null && !notes.trim().isEmpty() ? notes.trim() + " - " : "") + detailNotes);

        return stockReceiptRepository.save(receipt);
    }

    public List<StockReceipt> getReceiptsByBranch(String branchCode) {
        if (branchCode != null && !branchCode.trim().isEmpty()) {
            return stockReceiptRepository.findByBranchCodeOrderByReceiptDateDesc(branchCode);
        }
        return stockReceiptRepository.findAll();
    }
}
