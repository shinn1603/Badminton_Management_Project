package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_proposals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricingProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proposal_code", nullable = false, unique = true, length = 20)
    private String proposalCode;

    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;

    @Column(name = "proposer_name", nullable = false, length = 100)
    private String proposerName;

    @Column(name = "court_type", nullable = false, length = 100)
    private String courtType;

    @Column(name = "slot_info", nullable = false, length = 100)
    private String slotInfo;

    @Column(name = "current_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "proposed_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal proposedPrice;

    @Column(length = 255)
    private String reason;

    @Column(length = 30)
    private String status = "Chờ phê duyệt";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(length = 255)
    private String note;
}
