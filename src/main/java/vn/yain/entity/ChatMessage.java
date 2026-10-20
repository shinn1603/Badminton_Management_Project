package vn.yain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    // "customer", "staff", "bot", "system"
    @Column(name = "sender", nullable = false, length = 30)
    private String sender;

    @Column(name = "staff_name", length = 100)
    private String staffName;

    @Column(name = "staff_code", length = 50)
    private String staffCode;

    @Column(name = "branch_code", length = 50)
    private String branchCode;

    @Column(name = "content", length = 2500, nullable = false)
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "read_by_staff")
    @Builder.Default
    private Boolean readByStaff = false;

    @Column(name = "read_by_customer")
    @Builder.Default
    private Boolean readByCustomer = false;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (readByStaff == null) {
            readByStaff = false;
        }
        if (readByCustomer == null) {
            readByCustomer = false;
        }
    }
}
