package vn.yain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "work_shifts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_code", unique = true, nullable = false, length = 50)
    private String shiftCode;

    @Column(name = "staff_code", nullable = false, length = 20)
    private String staffCode;

    @Builder.Default
    @Column(name = "branch_code", length = 20)
    private String branchCode = "CN01";

    @Column(name = "day_of_week", length = 10)
    private String dayOfWeek; // mon, tue, wed, thu, fri, sat, sun

    @Column(name = "shift_type", length = 30)
    private String shiftType; // morning, night

    @Column(name = "duty", length = 50)
    private String duty; // POS, Thu ngan, Le tan, Ky thuat, Quan ly

    @Builder.Default
    @Column(name = "icon", length = 30)
    private String icon = "POS";

    @Builder.Default
    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate = LocalDate.now();

    @Builder.Default
    @Column(name = "start_time", nullable = false, length = 10)
    private String startTime = "06:00";

    @Builder.Default
    @Column(name = "end_time", nullable = false, length = 10)
    private String endTime = "14:00";

    @Builder.Default
    @Column(name = "status", length = 30)
    private String status = "Đã phân ca";
}
