package vn.yain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.yain.entity.Booking;
import vn.yain.entity.Product;
import vn.yain.entity.StockProductDetail;
import vn.yain.entity.StockReceipt;
import vn.yain.repository.BookingRepository;
import vn.yain.repository.ProductRepository;
import vn.yain.repository.StockProductDetailRepository;
import vn.yain.repository.StockReceiptRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Autowired(required = false)
    private StockProductDetailRepository stockProductDetailRepository;

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

        String targetBranch = (branchCode != null && !branchCode.trim().isEmpty() && !"ALL".equalsIgnoreCase(branchCode.trim()))
                ? branchCode.trim() : "CN01";

        String receiptCode = "PNK" + (100000 + (int)(Math.random() * 900000));
        BigDecimal totalAmount = BigDecimal.ZERO;
        StringBuilder detailNotes = new StringBuilder();
        List<StockProductDetail> savedDetails = new ArrayList<>();

        for (Map<String, Object> item : items) {
            String productCode = item.get("productCode") != null ? item.get("productCode").toString().trim() : "";
            if (productCode.isEmpty()) {
                throw new IllegalArgumentException("Mã sản phẩm không được để trống!");
            }
            String productName = item.get("productName") != null ? item.get("productName").toString().trim() : null;

            Object qtyObj = item.get("quantity") != null ? item.get("quantity") : item.get("qty");
            if (qtyObj == null) {
                throw new IllegalArgumentException("Số lượng nhập không được để trống!");
            }
            int quantity;
            try {
                quantity = new BigDecimal(qtyObj.toString().trim()).intValue();
            } catch (Exception e) {
                throw new IllegalArgumentException("Số lượng nhập không hợp lệ: " + qtyObj);
            }

            Object priceObj = item.get("importPrice") != null ? item.get("importPrice") : item.get("unitPrice");
            if (priceObj == null) {
                priceObj = item.get("price");
            }
            if (priceObj == null) {
                throw new IllegalArgumentException("Đơn giá nhập của sản phẩm không được để trống!");
            }
            BigDecimal importPrice;
            try {
                importPrice = new BigDecimal(priceObj.toString().trim());
            } catch (Exception e) {
                throw new IllegalArgumentException("Đơn giá nhập không hợp lệ: " + priceObj);
            }

            if (quantity <= 0 || importPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Số lượng và giá nhập của từng sản phẩm phải hợp lệ!");
            }

            // 1. Tìm sản phẩm trong chi nhánh mục tiêu
            Product product = productRepository.findByBranchCode(targetBranch).stream()
                    .filter(p -> p.getProductCode().equalsIgnoreCase(productCode)
                            || (productName != null && p.getProductName() != null && p.getProductName().equalsIgnoreCase(productName)))
                    .findFirst()
                    .orElse(null);

            // 2. Tìm theo mã sản phẩm chính xác trên toàn hệ thống
            if (product == null) {
                product = productRepository.findByProductCode(productCode).orElse(null);
            }

            // 3. Tìm theo tên hoặc mã tương tự trên toàn bộ danh mục
            if (product == null) {
                final String searchName = (productName != null && !productName.isBlank()) ? productName : productCode;
                product = productRepository.findAll().stream()
                        .filter(p -> p.getProductCode().equalsIgnoreCase(productCode)
                                || (p.getProductName() != null && p.getProductName().equalsIgnoreCase(searchName)))
                        .findFirst()
                        .orElse(null);
            }

            // 4. Nếu tìm thấy sản phẩm ở chi nhánh khác nhưng chưa có ở targetBranch:
            // Tự động phân bổ/tạo mới bản ghi sản phẩm cho targetBranch để quản lý tồn kho riêng biệt
            if (product != null && product.getBranchCode() != null && !targetBranch.equalsIgnoreCase(product.getBranchCode())) {
                final String pName = product.getProductName();
                Optional<Product> existingInBranch = productRepository.findByBranchCode(targetBranch).stream()
                        .filter(p -> p.getProductName() != null && p.getProductName().equalsIgnoreCase(pName))
                        .findFirst();
                if (existingInBranch.isPresent()) {
                    product = existingInBranch.get();
                } else {
                    Product newBranchProduct = new Product();
                    String newCode = "P" + String.format("%02d", (productRepository.count() + 1));
                    while (productRepository.findByProductCode(newCode).isPresent()) {
                        newCode = "P" + (productRepository.count() + 10 + (int)(Math.random() * 900));
                    }
                    newBranchProduct.setProductCode(newCode);
                    newBranchProduct.setBranchCode(targetBranch);
                    newBranchProduct.setProductName(product.getProductName());
                    newBranchProduct.setCategory(product.getCategory());
                    newBranchProduct.setUnit(product.getUnit());
                    newBranchProduct.setUnitPrice(product.getUnitPrice());
                    newBranchProduct.setCostPrice(importPrice);
                    newBranchProduct.setStockQuantity(0);
                    newBranchProduct.setMinQuantity(product.getMinQuantity() != null ? product.getMinQuantity() : 10);
                    product = productRepository.save(newBranchProduct);
                }
            }

            // 5. Nếu không tìm thấy trong bất kỳ chi nhánh nào: Tự động khởi tạo sản phẩm vào kho
            if (product == null) {
                Product newProd = new Product();
                String codeToUse = productCode.toUpperCase();
                if (productRepository.findByProductCode(codeToUse).isPresent()) {
                    codeToUse = "P" + String.format("%02d", (productRepository.count() + 1));
                    while (productRepository.findByProductCode(codeToUse).isPresent()) {
                        codeToUse = "P" + (productRepository.count() + 10 + (int)(Math.random() * 900));
                    }
                }
                newProd.setProductCode(codeToUse);
                newProd.setBranchCode(targetBranch);
                String resolvedName = (productName != null && !productName.isBlank()) ? productName : getStandardCatalogName(productCode);
                newProd.setProductName(resolvedName);
                newProd.setCategory(getStandardCategory(resolvedName));
                newProd.setUnit(getStandardUnit(resolvedName));
                newProd.setCostPrice(importPrice);
                newProd.setUnitPrice(importPrice.multiply(new BigDecimal("1.30")).setScale(0, java.math.RoundingMode.HALF_UP));
                newProd.setStockQuantity(0);
                newProd.setMinQuantity(10);
                product = productRepository.save(newProd);
            }

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

            // Lưu chi tiết nhập hàng vào bảng stock_product_details
            StockProductDetail detail = new StockProductDetail();
            detail.setReceiptCode(receiptCode);
            detail.setProductCode(product.getProductCode());
            detail.setQuantity(quantity);
            detail.setImportPrice(importPrice);
            detail.setTotalAmount(lineTotal);
            savedDetails.add(detail);

            if (detailNotes.length() > 0) detailNotes.append(", ");
            detailNotes.append(product.getProductName()).append(" x").append(quantity);
        }

        StockReceipt receipt = new StockReceipt();
        receipt.setReceiptCode(receiptCode);
        receipt.setBranchCode(targetBranch);
        receipt.setStaffCode(staffCode != null && !staffCode.trim().isEmpty() ? staffCode.trim() : "NS02");
        receipt.setSupplierName(supplierName.trim());
        receipt.setReceiptDate(LocalDateTime.now());
        receipt.setTotalAmount(totalAmount);

        String combinedNotes = (notes != null && !notes.trim().isEmpty() ? notes.trim() + " - " : "") + detailNotes;
        if (combinedNotes.length() > 250) {
            combinedNotes = combinedNotes.substring(0, 247) + "...";
        }
        receipt.setNotes(combinedNotes);

        StockReceipt savedReceipt = stockReceiptRepository.save(receipt);

        if (stockProductDetailRepository != null && !savedDetails.isEmpty()) {
            stockProductDetailRepository.saveAll(savedDetails);
        }

        return savedReceipt;
    }

    private String getStandardCatalogName(String code) {
        if (code == null) return "Hàng hóa nhập mới";
        return switch (code.toUpperCase().trim()) {
            case "P01" -> "Nước bù khoáng Revive Chanh muối 500ml";
            case "P02" -> "Nước điện giải Pocari Sweat 500ml";
            case "P03" -> "Nước suối tinh khiết Aquafina 500ml";
            case "P04" -> "Nước tăng lực Bò Húc Red Bull Thái";
            case "P05" -> "Ống Cầu Lông Hải Yến Đỏ (12 quả)";
            case "P06" -> "Ống Cầu Lông Thành Công 77 (12 quả)";
            case "P07" -> "Quấn cán vợt cầu lông VS chống trượt";
            case "P08" -> "Khăn lau mồ hôi thể thao Yonex";
            default -> "Sản phẩm " + code;
        };
    }

    private String getStandardCategory(String name) {
        if (name == null) return "Phụ kiện";
        String lower = name.toLowerCase();
        if (lower.contains("cầu") || lower.contains("ống")) return "Cầu lông";
        if (lower.contains("nước") || lower.contains("pocari") || lower.contains("revive") || lower.contains("aquafina") || lower.contains("bò húc")) return "Nước giải khát";
        if (lower.contains("vợt")) return "Dụng cụ";
        return "Phụ kiện";
    }

    private String getStandardUnit(String name) {
        if (name == null) return "Cái";
        String lower = name.toLowerCase();
        if (lower.contains("cầu") || lower.contains("ống")) return "Ống";
        if (lower.contains("nước") || lower.contains("pocari") || lower.contains("revive") || lower.contains("aquafina")) return "Chai";
        if (lower.contains("lon") || lower.contains("bò húc")) return "Lon";
        return "Cái";
    }

    public List<StockReceipt> getReceiptsByBranch(String branchCode) {
        if (branchCode != null && !branchCode.trim().isEmpty() && !"ALL".equalsIgnoreCase(branchCode.trim())) {
            return stockReceiptRepository.findByBranchCodeOrderByReceiptDateDesc(branchCode.trim());
        }
        return stockReceiptRepository.findAll();
    }
}
