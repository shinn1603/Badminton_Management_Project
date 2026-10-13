package vn.yain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "staff")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_code", unique = true, nullable = false, length = 20)
    private String staffCode;

    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;

    @Column(name = "full_name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String fullName;

    @Column(name = "position", nullable = false, columnDefinition = "NVARCHAR(50)")
    private String position;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(name = "address", columnDefinition = "NVARCHAR(255)")
    private String address;

    @Builder.Default
    @Column(name = "status", columnDefinition = "NVARCHAR(30)")
    private String status = "Đang làm việc";
}
