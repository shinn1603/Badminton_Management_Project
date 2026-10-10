package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_code", nullable = false, unique = true, length = 30)
    private String paymentCode;

    @Column(name = "booking_code", nullable = false, length = 30)
    private String bookingCode;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 50)
    private String method; // VietQR Pro, Tiền mặt, VNPay, MoMo

    @Column(name = "transaction_type", nullable = false, length = 50)
    private String transactionType; // Đặt cọc 30%, Thanh toán đủ, Hoàn tiền cọc

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime = LocalDateTime.now();

    @Column(length = 30)
    private String status = "Thành công";
}
