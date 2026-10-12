package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_registrations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentRegistration {
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

    @Column(name = "registration_date")
    private LocalDateTime registrationDate = LocalDateTime.now();

    @Column(length = 50)
    private String status = "Đã xác nhận";
}
