package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_code", nullable = false, unique = true, length = 30)
    private String invoiceCode;

    @Column(name = "booking_code", length = 30)
    private String bookingCode;

    @Column(name = "staff_code", length = 20)
    private String staffCode;

    @Column(name = "invoice_date")
    private LocalDateTime invoiceDate = LocalDateTime.now();

    @Column(name = "court_fee", nullable = false)
    private BigDecimal courtFee = BigDecimal.ZERO;

    @Column(name = "equipment_fee")
    private BigDecimal equipmentFee = BigDecimal.ZERO;

    @Column(name = "product_fee")
    private BigDecimal productFee = BigDecimal.ZERO;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "deposit_paid")
    private BigDecimal depositPaid = BigDecimal.ZERO;

    @Column(name = "total_payment", nullable = false)
    private BigDecimal totalPayment = BigDecimal.ZERO;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod = "Tiền mặt";

    @Column(length = 30)
    private String status = "Đã thanh toán";
}
