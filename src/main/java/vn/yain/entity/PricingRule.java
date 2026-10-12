package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pricing_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricingRule {
    @Id
    @Column(name = "pricing_code", nullable = false, length = 20)
    private String pricingCode;

    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;

    @Column(name = "slot_code", nullable = false, length = 20)
    private String slotCode;

    @Column(name = "day_type", nullable = false, length = 50)
    private String dayType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "hourly_rate", nullable = false, precision = 18, scale = 2)
    private BigDecimal hourlyRate;
}
