package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "courts")
@Data
public class Court {
    public Court() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "court_code", nullable = false, unique = true, length = 20)
    private String courtCode;

    @Column(name = "court_name", nullable = false, length = 100)
    private String courtName;

    @Column(name = "court_type", nullable = false, length = 50)
    private String courtType;

    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;

    @Column(name = "hourly_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal hourlyRate;

    @Column(length = 30)
    private String status = "Trống";

    @Column(name = "image_url", length = 255)
    private String imageUrl;
}
