package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_registrations")
@Data
public class TournamentRegistration {
    public TournamentRegistration() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_code", nullable = false, length = 20)
    private String tournamentCode;

    @Column(name = "customer_code", length = 20)
    private String customerCode;

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    @Column(length = 20)
    private String phone;

    @Column(name = "category", length = 50)
    private String category = "Đôi nam nữ";

    @Column(length = 100)
    private String email;

    @Column(name = "registration_date")
    private LocalDateTime registrationDate = LocalDateTime.now();

    @Column(length = 50)
    private String status = "Đã xác nhận";

    public String getCategory() {
        return category != null ? category : "Đôi nam nữ";
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
