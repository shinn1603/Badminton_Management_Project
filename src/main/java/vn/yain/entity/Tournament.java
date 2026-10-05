package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournaments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_code", nullable = false, unique = true, length = 20)
    private String tournamentCode;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "tournament_name", nullable = false, length = 150)
    private String tournamentName;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String rules;

    @Column(name = "max_participants")
    private Integer maxParticipants = 32;

    @Column(name = "entry_fee")
    private BigDecimal entryFee = BigDecimal.ZERO;

    @Column(name = "total_prize")
    private BigDecimal totalPrize = BigDecimal.ZERO;

    @Column(length = 50)
    private String status = "Đang mở đăng ký";
}
