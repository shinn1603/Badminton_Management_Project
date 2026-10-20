package vn.yain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 100)
    private String sessionId;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Column(name = "branch_code", length = 50)
    private String branchCode;

    // "BOT", "WAITING_STAFF", "ACTIVE_STAFF", "CLOSED"
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "BOT";

    @Column(name = "assigned_staff", length = 100)
    private String assignedStaff;

    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "unread_staff_count")
    @Builder.Default
    private Integer unreadStaffCount = 0;

    @Column(name = "unread_customer_count")
    @Builder.Default
    private Integer unreadCustomerCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (lastMessageAt == null) {
            lastMessageAt = now;
        }
        if (status == null) {
            status = "BOT";
        }
        if (unreadStaffCount == null) {
            unreadStaffCount = 0;
        }
        if (unreadCustomerCount == null) {
            unreadCustomerCount = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
