package vn.yain.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "staff")
@Data
public class Staff {
    public Staff() {
    }

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

    @Column(name = "status", columnDefinition = "NVARCHAR(30)")
    private String status = "Đang làm việc";

    public static StaffBuilder builder() {
        return new StaffBuilder();
    }

    public static class StaffBuilder {
        private Long id;
        private String staffCode;
        private String branchCode = "CN01";
        private String fullName;
        private String position;
        private String phone;
        private String email;
        private String address;
        private String status = "Đang làm việc";

        public StaffBuilder id(Long id) { this.id = id; return this; }
        public StaffBuilder staffCode(String staffCode) { this.staffCode = staffCode; return this; }
        public StaffBuilder branchCode(String branchCode) { this.branchCode = branchCode; return this; }
        public StaffBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public StaffBuilder position(String position) { this.position = position; return this; }
        public StaffBuilder phone(String phone) { this.phone = phone; return this; }
        public StaffBuilder email(String email) { this.email = email; return this; }
        public StaffBuilder address(String address) { this.address = address; return this; }
        public StaffBuilder status(String status) { this.status = status; return this; }

        public Staff build() {
            Staff s = new Staff();
            s.id = this.id;
            s.staffCode = this.staffCode;
            s.branchCode = this.branchCode;
            s.fullName = this.fullName;
            s.position = this.position;
            s.phone = this.phone;
            s.email = this.email;
            s.address = this.address;
            s.status = this.status;
            return s;
        }
    }
}
