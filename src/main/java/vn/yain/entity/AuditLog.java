package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
public class AuditLog {
    public AuditLog() {
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "user_code", length = 20)
    private String userCode;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Column(name = "module", length = 50)
    private String module = "Hệ thống";

    @Column(name = "ip_address", length = 50)
    private String ipAddress = "127.0.0.1";

    @Column(length = 30)
    private String status = "Thành công";

    @Column(name = "action_time")
    private LocalDateTime actionTime = LocalDateTime.now();

    @Lob
    @Column(name = "details")
    private String details;
}
