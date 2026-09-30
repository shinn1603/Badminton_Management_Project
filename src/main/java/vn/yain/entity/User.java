package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 30)
    private String role; // DIRECTOR, MANAGER, POS, CUSTOMER, ADMIN

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(length = 30)
    private String status = "Hoạt động";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
