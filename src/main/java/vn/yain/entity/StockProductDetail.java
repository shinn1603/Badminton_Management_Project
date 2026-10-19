package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "stock_product_details")
@Data
public class StockProductDetail {
    public StockProductDetail() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_code", nullable = false, length = 30)
    private String receiptCode;

    @Column(name = "product_code", nullable = false, length = 20)
    private String productCode;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "import_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal importPrice;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;
}
