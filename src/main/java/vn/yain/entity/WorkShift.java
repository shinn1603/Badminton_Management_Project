package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "work_shifts")
@Data
public class WorkShift {
    public WorkShift() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_code", unique = true, nullable = false, length = 50)
    private String shiftCode;

    @Column(name = "staff_code", nullable = false, length = 20)
    private String staffCode;

    @Column(name = "branch_code", length = 20)
    private String branchCode = "CN01";

    @Column(name = "day_of_week", length = 10)
    private String dayOfWeek; // mon, tue, wed, thu, fri, sat, sun

    @Column(name = "shift_type", length = 30)
    private String shiftType; // morning, night

    @Column(name = "duty", columnDefinition = "NVARCHAR(100)")
    private String duty; // POS, Thu ngan, Le tan, Ky thuat, Quan ly

    @Column(name = "icon", length = 30)
    private String icon = "POS";

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate = LocalDate.now();

    @Column(name = "start_time", nullable = false, length = 10)
    private String startTime = "06:00";

    @Column(name = "end_time", nullable = false, length = 10)
    private String endTime = "14:00";

    @Column(name = "status", columnDefinition = "NVARCHAR(50)")
    private String status = "Đã phân ca";

    public static WorkShiftBuilder builder() {
        return new WorkShiftBuilder();
    }

    public static class WorkShiftBuilder {
        private Long id;
        private String shiftCode;
        private String staffCode;
        private String branchCode = "CN01";
        private String dayOfWeek;
        private String shiftType;
        private String duty;
        private String icon = "POS";
        private LocalDate shiftDate = LocalDate.now();
        private String startTime = "06:00";
        private String endTime = "14:00";
        private String status = "Đã phân ca";

        public WorkShiftBuilder id(Long id) { this.id = id; return this; }
        public WorkShiftBuilder shiftCode(String shiftCode) { this.shiftCode = shiftCode; return this; }
        public WorkShiftBuilder staffCode(String staffCode) { this.staffCode = staffCode; return this; }
        public WorkShiftBuilder branchCode(String branchCode) { this.branchCode = branchCode; return this; }
        public WorkShiftBuilder dayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; return this; }
        public WorkShiftBuilder shiftType(String shiftType) { this.shiftType = shiftType; return this; }
        public WorkShiftBuilder duty(String duty) { this.duty = duty; return this; }
        public WorkShiftBuilder icon(String icon) { this.icon = icon; return this; }
        public WorkShiftBuilder shiftDate(LocalDate shiftDate) { this.shiftDate = shiftDate; return this; }
        public WorkShiftBuilder startTime(String startTime) { this.startTime = startTime; return this; }
        public WorkShiftBuilder endTime(String endTime) { this.endTime = endTime; return this; }
        public WorkShiftBuilder status(String status) { this.status = status; return this; }

        public WorkShift build() {
            WorkShift ws = new WorkShift();
            ws.id = this.id;
            ws.shiftCode = this.shiftCode;
            ws.staffCode = this.staffCode;
            ws.branchCode = this.branchCode;
            ws.dayOfWeek = this.dayOfWeek;
            ws.shiftType = this.shiftType;
            ws.duty = this.duty;
            ws.icon = this.icon;
            ws.shiftDate = this.shiftDate;
            ws.startTime = this.startTime;
            ws.endTime = this.endTime;
            ws.status = this.status;
            return ws;
        }
    }
}
