package vn.yain.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.yain.entity.*;
import vn.yain.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final BranchRepository branchRepository;
    private final CourtRepository courtRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final ProductRepository productRepository;
    private final EquipmentRepository equipmentRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            if (branchRepository.count() == 0) {
                log.info(">>> DATABASE TRỐNG - ĐANG TỰ ĐỘNG KHỞI TẠO DỮ LIỆU TOÀN HỆ THỐNG <<<");
                seedBranches();
                seedCourts();
                seedTimeSlots();
                seedPricingRules();
                seedCustomersAndUsers();
                seedStaff();
                seedProductsAndEquipment();
                seedSampleBookings();
                log.info(">>> TỰ ĐỘNG NẠP DỮ LIỆU HOÀN TẤT 100%! HỆ THỐNG ĐÃ SẴN SÀNG HOẠT ĐỘNG <<<");
            } else {
                log.info(">>> DATABASE ĐÃ CÓ DỮ LIỆU ({} CHI NHÁNH) - BỎ QUA SEEDER <<<", branchRepository.count());
            }
        } catch (Exception e) {
            log.warn(">>> THÔNG BÁO TỰ ĐỘNG SEEDER: {} <<<", e.getMessage());
        }
    }

    private void seedBranches() {
        branchRepository.saveAll(List.of(
            createBranch("CN01", "Chi nhánh 1 - Sân Cầu Lông Thủ Đức", "Số 01 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức", "0903123456", "Trần Phúc Bảo", 8, "Hoạt động"),
            createBranch("CN02", "Chi nhánh 2 - Sân Cầu Lông Bình Thạnh", "Số 234 Điện Biên Phủ, Phường 25, Bình Thạnh", "0903333444", "Nguyễn Văn Quản", 6, "Hoạt động"),
            createBranch("CN03", "Chi nhánh 3 - Sân Cầu Lông Quận 9", "Số 70 Đỗ Xuân Hợp, Phước Long B, TP. Thủ Đức", "0903789012", "Trần Hữu Nam", 6, "Hoạt động"),
            createBranch("CN04", "Chi nhánh 4 - Sân Cầu Lông Gò Vấp", "Số 18 Quang Trung, Phường 10, Gò Vấp", "0903555777", "Lê Hoàng Khang", 6, "Hoạt động"),
            createBranch("ALL", "Hội Sở / Ban Giám Đốc", "Số 01 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức", "0901111222", "Võ Hoàng Nam", 0, "Hoạt động")
        ));
    }

    private Branch createBranch(String code, String name, String addr, String phone, String mgr, int courts, String status) {
        Branch b = new Branch();
        b.setBranchCode(code);
        b.setBranchName(name);
        b.setAddress(addr);
        b.setPhone(phone);
        b.setManagerName(mgr);
        b.setTotalCourts(courts);
        b.setStatus(status);
        return b;
    }

    private void seedCourts() {
        courtRepository.saveAll(List.of(
            createCourt("CL01", "CN01", "Sân cầu lông 1 (VIP Yonex)", "Cầu lông VIP", new BigDecimal("120000"), "Trống", "/images/badminton-court.jpg"),
            createCourt("CL02", "CN01", "Sân cầu lông 2", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Đang sử dụng", "/images/badminton-court.jpg"),
            createCourt("CL03", "CN01", "Sân cầu lông 3", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
            createCourt("CL04", "CN01", "Sân cầu lông 4", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
            createCourt("CL05", "CN01", "Sân cầu lông 5", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
            createCourt("CL06", "CN01", "Sân cầu lông 6 (VIP Yonex)", "Cầu lông VIP", new BigDecimal("120000"), "Bảo trì", "/images/badminton-court.jpg"),
            createCourt("CL07", "CN01", "Sân cầu lông 7", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
            createCourt("CL08", "CN01", "Sân cầu lông 8", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
            createCourt("BT01", "CN02", "Sân BT 1 (VIP Victor)", "Cầu lông VIP", new BigDecimal("120000"), "Trống", "/images/badminton-court.jpg"),
            createCourt("BT02", "CN02", "Sân BT 2", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
            createCourt("Q901", "CN03", "Sân Q9 1 (VIP Lining)", "Cầu lông VIP", new BigDecimal("120000"), "Trống", "/images/badminton-court.jpg"),
            createCourt("GV01", "CN04", "Sân GV 1 (VIP Yonex)", "Cầu lông VIP", new BigDecimal("120000"), "Trống", "/images/badminton-court.jpg")
        ));
    }

    private Court createCourt(String code, String branch, String name, String type, BigDecimal rate, String status, String img) {
        Court c = new Court();
        c.setCourtCode(code);
        c.setBranchCode(branch);
        c.setCourtName(name);
        c.setCourtType(type);
        c.setHourlyRate(rate);
        c.setStatus(status);
        c.setImageUrl(img);
        return c;
    }

    private void seedTimeSlots() {
        timeSlotRepository.saveAll(List.of(
            createTimeSlot("KG01", "06:00", "09:00", "Thường (Sáng sớm)"),
            createTimeSlot("KG02", "09:00", "17:00", "Thường (Ban ngày)"),
            createTimeSlot("KG03", "17:00", "22:00", "Giờ vàng cao điểm"),
            createTimeSlot("KG04", "22:00", "23:00", "Tối muộn")
        ));
    }

    private TimeSlot createTimeSlot(String code, String start, String end, String type) {
        TimeSlot ts = new TimeSlot();
        ts.setSlotCode(code);
        ts.setStartTime(start);
        ts.setEndTime(end);
        ts.setSlotType(type);
        return ts;
    }

    private void seedPricingRules() {
        pricingRuleRepository.saveAll(List.of(
            createPricingRule("BG01", "CN01", "KG01", "Ngày thường (T2-T6)", LocalDate.of(2026, 1, 1), new BigDecimal("80000")),
            createPricingRule("BG02", "CN01", "KG02", "Ngày thường (T2-T6)", LocalDate.of(2026, 1, 1), new BigDecimal("90000")),
            createPricingRule("BG03", "CN01", "KG03", "Ngày thường (T2-T6)", LocalDate.of(2026, 1, 1), new BigDecimal("130000")),
            createPricingRule("BG04", "CN01", "KG04", "Ngày thường (T2-T6)", LocalDate.of(2026, 1, 1), new BigDecimal("90000"))
        ));
    }

    private PricingRule createPricingRule(String code, String branch, String slot, String dayType, LocalDate start, BigDecimal rate) {
        PricingRule pr = new PricingRule();
        pr.setPricingCode(code);
        pr.setBranchCode(branch);
        pr.setSlotCode(slot);
        pr.setDayType(dayType);
        pr.setStartDate(start);
        pr.setHourlyRate(rate);
        return pr;
    }

    private void seedCustomersAndUsers() {
        customerRepository.saveAll(List.of(
            createCustomer("KH001", "Lê Bá Đạt", "0912345678", "dat.le@gmail.com", "123456", new BigDecimal("42.5"), 4250, "Kim Cương"),
            createCustomer("KH002", "Nguyễn Hoàng Nam", "0903123456", "nam.nguyen@gmail.com", "123456", new BigDecimal("28.0"), 2800, "Vàng"),
            createCustomer("KH003", "Phạm Gia Huy", "0988777666", "huy.pham@gmail.com", "123456", new BigDecimal("15.0"), 1500, "Bạc")
        ));

        userRepository.saveAll(List.of(
            createUser("admin", "123456", "Quản Trị Viên Hệ Thống", "ADMIN", "ALL", "0901111222", "admin@utesport.vn"),
            createUser("ql_cn01", "123456", "Trần Phúc Bảo", "MANAGER", "CN01", "0903123456", "bao.tran@utesport.vn"),
            createUser("nv_cn01", "123456", "Nguyễn Thị Mai", "STAFF", "CN01", "0903999888", "mai.nguyen@utesport.vn")
        ));
    }

    private Customer createCustomer(String code, String name, String phone, String email, String pass, BigDecimal hours, int pts, String tier) {
        Customer c = new Customer();
        c.setCustomerCode(code);
        c.setFullName(name);
        c.setPhone(phone);
        c.setEmail(email);
        c.setPassword(pass);
        c.setTotalPlayHours(hours);
        c.setRewardPoints(pts);
        c.setMembershipTier(tier);
        return c;
    }

    private User createUser(String username, String rawPass, String fullName, String role, String branch, String phone, String email) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPass));
        u.setFullName(fullName);
        u.setRole(role);
        u.setBranchCode(branch);
        u.setPhone(phone);
        u.setEmail(email);
        u.setStatus("Hoạt động");
        u.setCreatedAt(LocalDateTime.now());
        return u;
    }

    private void seedStaff() {
        staffRepository.saveAll(List.of(
            createStaff("NV001", "Trần Phúc Bảo", "CN01", "Quản lý chi nhánh", "0903123456", "bao.tran@utesport.vn"),
            createStaff("NV002", "Nguyễn Thị Mai", "CN01", "Thu ngân - Lễ tân", "0903999888", "mai.nguyen@utesport.vn")
        ));
    }

    private Staff createStaff(String code, String name, String branch, String pos, String phone, String email) {
        Staff s = new Staff();
        s.setStaffCode(code);
        s.setFullName(name);
        s.setBranchCode(branch);
        s.setPosition(pos);
        s.setPhone(phone);
        s.setEmail(email);
        s.setStatus("Đang làm việc");
        return s;
    }

    private void seedProductsAndEquipment() {
        equipmentRepository.saveAll(List.of(
            createEquipment("DC01", "CN01", "Vợt Cầu Lông Yonex Astrox 88D Pro", new BigDecimal("50000"), 15, "Tốt"),
            createEquipment("DC02", "CN01", "Giày Cầu Lông Lefus Non-marking", new BigDecimal("20000"), 25, "Tốt")
        ));

        productRepository.saveAll(List.of(
            createProduct("SP01", "CN01", "Ống Cầu Yonex Aerosensa AS-40 (12 quả)", "Quả cầu lông", "Ống", new BigDecimal("420000"), new BigDecimal("350000"), 50),
            createProduct("SP02", "CN01", "Ống Cầu Thành Công 77 (12 quả)", "Quả cầu lông", "Ống", new BigDecimal("260000"), new BigDecimal("210000"), 80),
            createProduct("SP03", "CN01", "Vớ thể thao Yonex cotton chống trượt (1 đôi)", "Phụ kiện", "Đôi", new BigDecimal("25000"), new BigDecimal("15000"), 120),
            createProduct("SP05", "CN01", "Nước uống bù khoáng Revive 500ml", "Nước giải khát", "Chai", new BigDecimal("15000"), new BigDecimal("10000"), 150),
            createProduct("SP06", "CN01", "Nước ion điện giải Pocari Sweat 500ml", "Nước giải khát", "Chai", new BigDecimal("20000"), new BigDecimal("14000"), 100),
            createProduct("SP07", "CN01", "Mì ly Modern lẩu thái tôm", "Thức ăn nhanh", "Ly", new BigDecimal("15000"), new BigDecimal("10000"), 60)
        ));
    }

    private Equipment createEquipment(String code, String branch, String name, BigDecimal rent, int total, String cond) {
        Equipment eq = new Equipment();
        eq.setEquipmentCode(code);
        eq.setBranchCode(branch);
        eq.setEquipmentName(name);
        eq.setRentalPrice(rent);
        eq.setStockQuantity(total);
        eq.setCondition(cond);
        return eq;
    }

    private Product createProduct(String code, String branch, String name, String cat, String unit, BigDecimal price, BigDecimal cost, int qty) {
        Product p = new Product();
        p.setProductCode(code);
        p.setBranchCode(branch);
        p.setProductName(name);
        p.setCategory(cat);
        p.setUnit(unit);
        p.setUnitPrice(price);
        p.setCostPrice(cost);
        p.setStockQuantity(qty);
        return p;
    }

    private void seedSampleBookings() {
        Booking b = new Booking();
        b.setBookingCode("DS0101");
        b.setCustomerCode("KH002");
        b.setCustomerName("Nguyễn Hoàng Nam");
        b.setCustomerPhone("0903123456");
        b.setCourtCode("CL02");
        b.setBranchCode("CN01");
        b.setBookingDate(LocalDate.now());
        b.setTimeSlot("08:00 - 10:00");
        b.setHourlyPrice(new BigDecimal("90000"));
        b.setTotalPrice(new BigDecimal("180000"));
        b.setDepositAmount(new BigDecimal("54000"));
        b.setPaymentMethod("Tiền mặt");
        b.setStatus("Đang sử dụng");
        b.setNotes("Khách quen tuần 3 buổi");
        b.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(b);
    }
}
