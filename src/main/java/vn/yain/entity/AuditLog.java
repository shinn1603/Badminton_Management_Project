package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_code", length = 20)
    private String userCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "action_name", nullable = false, length = 100)
    private String actionName;

    @Column(name = "target_module", nullable = false, length = 50)
    private String targetModule;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(length = 30)
    private String status = "Thành công";

    @Column(name = "log_time")
    private LocalDateTime logTime = LocalDateTime.now();

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String details;
}
