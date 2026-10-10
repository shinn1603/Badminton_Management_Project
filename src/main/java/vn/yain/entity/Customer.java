package vn.yain.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_code", nullable = false, unique = true, length = 20)
    private String customerCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "total_play_hours", precision = 10, scale = 2)
    private BigDecimal totalPlayHours = BigDecimal.ZERO;

    @Column(name = "reward_points")
    private Integer rewardPoints = 0;

    @Column(name = "membership_tier", length = 30)
    private String membershipTier = "Đồng";

    @Column(length = 30)
    private String status = "Hoạt động";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
