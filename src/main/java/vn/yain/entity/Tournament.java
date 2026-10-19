package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournaments")
@Data
public class Tournament {
    public Tournament() {
    }

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

    @Lob
    @Column(name = "rules")
    private String rules;

    @Column(name = "category", length = 50)
    private String category = "Đôi nam nữ";

    public String getCategory() {
        return category != null ? category : "Đôi nam nữ";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Column(name = "max_participants")
    private Integer maxParticipants = 32;

    @Column(name = "current_participants")
    private Integer currentParticipants = 24;

    @Column(name = "entry_fee", precision = 15, scale = 2)
    private BigDecimal entryFee = BigDecimal.ZERO;

    @Column(name = "total_prize", precision = 15, scale = 2)
    private BigDecimal totalPrize = BigDecimal.ZERO;

    @Column(length = 50)
    private String status = "Đang mở đăng ký";

    public Integer getCurrentParticipants() {
        return currentParticipants != null ? currentParticipants : 24;
    }
}
