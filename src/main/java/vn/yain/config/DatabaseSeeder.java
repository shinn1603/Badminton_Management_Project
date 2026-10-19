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
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ProductRepository productRepository;
    private final EquipmentRepository equipmentRepository;
    private final BookingRepository bookingRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;
    private final PaymentRepository paymentRepository;
    private final WorkShiftRepository workShiftRepository;
    private final PromotionRepository promotionRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final StockReceiptRepository stockReceiptRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            log.info(">>> ĐANG KIỂM TRA VÀ ĐỒNG BỘ TOÀN DIỆN DỮ LIỆU ĐẶC TẢ UTE SPORT <<<");
            seedBranches();
            seedCourts();
            seedTimeSlots();
            seedPricingRules();
            seedCustomers();
            seedStaff();
            seedUsers();
            seedEquipment();
            seedProducts();
            seedBookings();
            seedTournaments();
            seedTournamentRegistrations();
            seedPayments();
            seedWorkShifts();
            seedPromotions();
            seedSystemSettings();
            seedStockReceipts();
            log.info(">>> HOÀN TẤT ĐỒNG BỘ TOÀN BỘ 25 PHÂN HỆ DỮ LIỆU MẪU CHUẨN ĐẶC TẢ! <<<");
        } catch (Exception e) {
            log.warn(">>> CẢNH BÁO DATABASE SEEDER: {} <<<", e.getMessage());
        }
    }

    private void seedBranches() {
        if (branchRepository.count() < 5) {
            branchRepository.saveAll(List.of(
                createBranch("CN01", "Chi nhánh 1 - Sân Cầu Lông Thủ Đức", "Số 01 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức", "0903123456", "Trần Phúc Bảo", 8, "Hoạt động"),
                createBranch("CN02", "Chi nhánh 2 - Sân Cầu Lông Bình Thạnh", "Số 234 Điện Biên Phủ, Phường 25, Bình Thạnh", "0903333444", "Nguyễn Văn Quản", 6, "Hoạt động"),
                createBranch("CN03", "Chi nhánh 3 - Sân Cầu Lông Quận 9", "Số 70 Đỗ Xuân Hợp, Phước Long B, TP. Thủ Đức", "0903789012", "Trần Hữu Nam", 6, "Hoạt động"),
                createBranch("CN04", "Chi nhánh 4 - Sân Cầu Lông Gò Vấp", "Số 18 Quang Trung, Phường 10, Gò Vấp", "0903555777", "Lê Hoàng Khang", 6, "Hoạt động"),
                createBranch("ALL", "Hội Sở / Ban Giám Đốc", "Số 01 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức", "0901111222", "Võ Hoàng Nam", 0, "Hoạt động")
            ));
        }
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
        if (courtRepository.count() < 26) {
            List<Court> courts = List.of(
                // Chi nhánh 1 - Thủ Đức (8 sân)
                createCourt("CL01", "CN01", "Sân cầu lông 1 (VIP Yonex)", "Cầu lông VIP", new BigDecimal("120000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("CL02", "CN01", "Sân cầu lông 2", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Đang sử dụng", "/images/badminton-court.jpg"),
                createCourt("CL03", "CN01", "Sân cầu lông 3", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("CL04", "CN01", "Sân cầu lông 4", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("CL05", "CN01", "Sân cầu lông 5", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Đang sử dụng", "/images/badminton-court.jpg"),
                createCourt("CL06", "CN01", "Sân cầu lông 6 (VIP Yonex)", "Cầu lông VIP", new BigDecimal("120000"), "Bảo trì", "/images/badminton-court.jpg"),
                createCourt("CL07", "CN01", "Sân cầu lông 7", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("CL08", "CN01", "Sân cầu lông 8", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                // Chi nhánh 2 - Bình Thạnh (6 sân)
                createCourt("BT01", "CN02", "Sân BT 1 (VIP Victor)", "Cầu lông VIP", new BigDecimal("120000"), "Đang sử dụng", "/images/badminton-court.jpg"),
                createCourt("BT02", "CN02", "Sân BT 2", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("BT03", "CN02", "Sân BT 3", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("BT04", "CN02", "Sân BT 4", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Đang sử dụng", "/images/badminton-court.jpg"),
                createCourt("BT05", "CN02", "Sân BT 5", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("BT06", "CN02", "Sân BT 6 (VIP)", "Cầu lông VIP", new BigDecimal("120000"), "Trống", "/images/badminton-court.jpg"),
                // Chi nhánh 3 - Quận 9 (6 sân)
                createCourt("Q901", "CN03", "Sân Q9 1 (VIP Lining)", "Cầu lông VIP", new BigDecimal("120000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("Q902", "CN03", "Sân Q9 2", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Đang sử dụng", "/images/badminton-court.jpg"),
                createCourt("Q903", "CN03", "Sân Q9 3", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("Q904", "CN03", "Sân Q9 4", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("Q905", "CN03", "Sân Q9 5", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("Q906", "CN03", "Sân Q9 6", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                // Chi nhánh 4 - Gò Vấp (6 sân)
                createCourt("GV01", "CN04", "Sân GV 1 (VIP Yonex)", "Cầu lông VIP", new BigDecimal("120000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("GV02", "CN04", "Sân GV 2", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Đang sử dụng", "/images/badminton-court.jpg"),
                createCourt("GV03", "CN04", "Sân GV 3", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("GV04", "CN04", "Sân GV 4", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("GV05", "CN04", "Sân GV 5", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                createCourt("GV06", "CN04", "Sân GV 6", "Cầu lông tiêu chuẩn", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg")
            );
            for (Court c : courts) {
                if (courtRepository.findByCourtCode(c.getCourtCode()).isEmpty()) {
                    courtRepository.save(c);
                }
            }
        }
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
        if (timeSlotRepository.count() < 4) {
            timeSlotRepository.saveAll(List.of(
                createTimeSlot("KG01", "06:00", "09:00", "Thường (Sáng sớm)"),
                createTimeSlot("KG02", "09:00", "17:00", "Thường (Ban ngày)"),
                createTimeSlot("KG03", "17:00", "22:00", "Giờ vàng cao điểm"),
                createTimeSlot("KG04", "22:00", "23:00", "Tối muộn")
            ));
        }
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
        if (pricingRuleRepository.count() < 11) {
            pricingRuleRepository.saveAll(List.of(
                createPricingRule("BG01", "CN01", "KG01", "Ngày thường (T2-T6)", LocalDate.of(2026, 1, 1), new BigDecimal("80000")),
                createPricingRule("BG02", "CN01", "KG02", "Ngày thường (T2-T6)", LocalDate.of(2026, 1, 1), new BigDecimal("90000")),
                createPricingRule("BG03", "CN01", "KG03", "Ngày thường (T2-T6)", LocalDate.of(2026, 1, 1), new BigDecimal("130000")),
                createPricingRule("BG04", "CN01", "KG04", "Ngày thường (T2-T6)", LocalDate.of(2026, 1, 1), new BigDecimal("90000")),
                createPricingRule("BG05", "CN01", "KG01", "Cuối tuần (T7-CN)", LocalDate.of(2026, 1, 1), new BigDecimal("110000")),
                createPricingRule("BG06", "CN01", "KG02", "Cuối tuần (T7-CN)", LocalDate.of(2026, 1, 1), new BigDecimal("120000")),
                createPricingRule("BG07", "CN01", "KG03", "Cuối tuần (T7-CN)", LocalDate.of(2026, 1, 1), new BigDecimal("150000")),
                createPricingRule("BG08", "CN01", "KG04", "Cuối tuần (T7-CN)", LocalDate.of(2026, 1, 1), new BigDecimal("110000")),
                createPricingRule("BG09", "CN02", "KG03", "Ngày thường (T2-T6)", LocalDate.of(2026, 1, 1), new BigDecimal("125000")),
                createPricingRule("BG10", "CN03", "KG03", "Ngày thường (T2-T6)", LocalDate.of(2026, 1, 1), new BigDecimal("120000")),
                createPricingRule("BG11", "CN04", "KG03", "Ngày thường (T2-T6)", LocalDate.of(2026, 1, 1), new BigDecimal("120000"))
            ));
        }
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

    private void seedCustomers() {
        if (customerRepository.count() < 6) {
            List<Customer> customers = List.of(
                createCustomer("KH001", "Lê Bá Đạt", "0912345678", "dat.le@gmail.com", "123456", new BigDecimal("42.5"), 4250, "Kim Cương"),
                createCustomer("KH002", "Nguyễn Hoàng Nam", "0903123456", "nam.nguyen@gmail.com", "123456", new BigDecimal("28.0"), 2800, "Vàng"),
                createCustomer("KH003", "Phạm Gia Huy", "0988777666", "huy.pham@gmail.com", "123456", new BigDecimal("15.0"), 1500, "Bạc"),
                createCustomer("KH004", "Vũ Thị Mai Anh", "0977555444", "maianh.vu@gmail.com", "123456", new BigDecimal("8.0"), 800, "Bạc"),
                createCustomer("KH005", "Trần Thanh Hằng", "0933444555", "hang.tran@gmail.com", "123456", new BigDecimal("4.0"), 400, "Đồng"),
                createCustomer("KH006", "Bùi Khánh Linh", "0909111222", "linh.bui@gmail.com", "123456", new BigDecimal("3.5"), 350, "Đồng")
            );
            for (Customer c : customers) {
                if (customerRepository.findByCustomerCode(c.getCustomerCode()).isEmpty()) {
                    customerRepository.save(c);
                }
            }
        }
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

    private void seedStaff() {
        if (staffRepository.count() < 12) {
            List<Staff> staffList = List.of(
                createStaff("NS01", "ALL", "Võ Hoàng Nam", "Giám đốc điều hành", "0901111222", "nam.vh@utesport.vn", "Quận 1, TP. Hồ Chí Minh"),
                createStaff("NS02", "CN01", "Trần Phúc Bảo", "Quản lý chi nhánh Thủ Đức", "0903123456", "bao.tran@utesport.vn", "TP. Thủ Đức, TP. Hồ Chí Minh"),
                createStaff("NS03", "CN01", "Nguyễn Thanh Tâm", "Thu ngân quầy POS", "0904555666", "tam.nguyen@utesport.vn", "TP. Thủ Đức, TP. Hồ Chí Minh"),
                createStaff("NS04", "CN01", "Lê Thị Mai", "Thu ngân quầy POS", "0904777888", "mai.lt@utesport.vn", "TP. Thủ Đức, TP. Hồ Chí Minh"),
                createStaff("NS05", "CN02", "Nguyễn Văn Quản", "Quản lý chi nhánh Bình Thạnh", "0903333444", "quan.nv@utesport.vn", "Bình Thạnh, TP. Hồ Chí Minh"),
                createStaff("NS06", "CN03", "Trần Hữu Nam", "Quản lý chi nhánh Quận 9", "0903789012", "nam.th@utesport.vn", "Quận 9, TP. Hồ Chí Minh"),
                createStaff("NS07", "CN04", "Lê Hoàng Khang", "Quản lý chi nhánh Gò Vấp", "0903555777", "khang.lh@utesport.vn", "Gò Vấp, TP. Hồ Chí Minh"),
                createStaff("NS08", "CN04", "Lê Văn Hùng", "Nhân viên kỹ thuật sân", "0908111333", "hung.lv@utesport.vn", "Gò Vấp, TP. Hồ Chí Minh"),
                createStaff("NS09", "CN01", "Bùi Khánh Linh", "Lễ tân & Dịch vụ", "0909111222", "linh.bk@utesport.vn", "TP. Thủ Đức, TP. Hồ Chí Minh"),
                createStaff("NS10", "CN01", "Nguyễn Hoàng Nam", "Thu ngân POS quầy", "0903888777", "nam.nh@utesport.vn", "TP. Thủ Đức, TP. Hồ Chí Minh"),
                createStaff("NS11", "CN01", "Phạm Gia Huy", "Kỹ thuật & Bảo dưỡng sân", "0988777666", "huy.pg@utesport.vn", "TP. Thủ Đức, TP. Hồ Chí Minh"),
                createStaff("NS12", "CN01", "Trịnh Gia Vinh", "Kỹ thuật & Bảo dưỡng sân", "0977222333", "vinh.tg@utesport.vn", "TP. Thủ Đức, TP. Hồ Chí Minh")
            );
            for (Staff s : staffList) {
                if (staffRepository.findByStaffCode(s.getStaffCode()).isEmpty()) {
                    staffRepository.save(s);
                }
            }
        }
    }

    private Staff createStaff(String code, String branch, String name, String pos, String phone, String email, String addr) {
        Staff s = new Staff();
        s.setStaffCode(code);
        s.setBranchCode(branch);
        s.setFullName(name);
        s.setPosition(pos);
        s.setPhone(phone);
        s.setEmail(email);
        s.setAddress(addr);
        s.setStatus("Đang làm việc");
        return s;
    }

    private void seedUsers() {
        if (userRepository.count() < 12) {
            List<User> users = List.of(
                createUser("director", "123456", "Nguyễn Phước Thọ", "DIRECTOR", "ALL", "0901111222", "director@utesport.vn", "NS01"),
                createUser("manager", "123456", "Trần Phúc Bảo", "MANAGER", "CN01", "0903123456", "manager@utesport.vn", "NS02"),
                createUser("pos", "123456", "Nguyễn Thanh Tâm", "POS", "CN01", "0904555666", "pos@utesport.vn", "NS03"),
                createUser("admin", "123456", "Trần Biểu Hương", "ADMIN", "ALL", "0908888999", "admin@utesport.vn", null),
                createUser("customer", "123456", "Lê Bá Đạt", "CUSTOMER", "CN01", "0912345678", "khachhang@gmail.com", null),
                createUser("nam.vh", "123456", "Võ Hoàng Nam", "DIRECTOR", "ALL", "0901111222", "nam.vh@utesport.vn", "NS01"),
                createUser("quan.nv", "123456", "Nguyễn Văn Quản", "MANAGER", "CN02", "0903333444", "quan.nv@utesport.vn", "NS05"),
                createUser("nam.th", "123456", "Trần Hữu Nam", "MANAGER", "CN03", "0903789012", "nam.th@utesport.vn", "NS06"),
                createUser("khang.lh", "123456", "Lê Hoàng Khang", "MANAGER", "CN04", "0903555777", "khang.lh@utesport.vn", "NS07"),
                createUser("mai.lt", "123456", "Lê Thị Mai", "POS", "CN01", "0904777888", "mai.lt@utesport.vn", "NS04"),
                createUser("ql_cn01", "123456", "Trần Phúc Bảo", "MANAGER", "CN01", "0903123456", "bao.tran@utesport.vn", "NS02"),
                createUser("nv_cn01", "123456", "Nguyễn Thị Mai", "STAFF", "CN01", "0903999888", "mai.nguyen@utesport.vn", "NS04")
            );
            for (User u : users) {
                if (userRepository.findByUsername(u.getUsername()).isEmpty()) {
                    userRepository.save(u);
                }
            }
        }
    }

    private User createUser(String username, String rawPass, String fullName, String role, String branch, String phone, String email, String staffCode) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPass));
        u.setFullName(fullName);
        u.setRole(role);
        u.setBranchCode(branch);
        u.setPhone(phone);
        u.setEmail(email);
        u.setStaffCode(staffCode);
        u.setStatus("Hoạt động");
        return u;
    }

    private void seedEquipment() {
        if (equipmentRepository.count() < 9) {
            equipmentRepository.saveAll(List.of(
                createEquipment("DC01", "CN01", "Vợt Yonex Astrox 77 Play (4U/G5)", 18, new BigDecimal("25000"), "Mới 99%"),
                createEquipment("DC02", "CN01", "Vợt Yonex Astrox 88D Tour", 12, new BigDecimal("35000"), "Tốt"),
                createEquipment("DC03", "CN01", "Vợt Victor Thruster K 9900", 10, new BigDecimal("30000"), "Tốt"),
                createEquipment("DC04", "CN01", "Vợt Lining Axforce 80", 8, new BigDecimal("30000"), "Tốt"),
                createEquipment("DC05", "CN01", "Ống Cầu Lông Hải Yến Đỏ (12 quả)", 45, new BigDecimal("180000"), "Mới nguyên hộp"),
                createEquipment("DC06", "CN01", "Ống Cầu Lông Thành Công 77", 35, new BigDecimal("210000"), "Mới nguyên hộp"),
                createEquipment("DC07", "CN02", "Vợt Yonex Astrox 77 Play", 15, new BigDecimal("25000"), "Tốt"),
                createEquipment("DC08", "CN03", "Vợt Yonex Astrox 77 Play", 15, new BigDecimal("25000"), "Tốt"),
                createEquipment("DC09", "CN04", "Vợt Yonex Astrox 77 Play", 15, new BigDecimal("25000"), "Tốt")
            ));
        }
    }

    private Equipment createEquipment(String code, String branch, String name, int stock, BigDecimal price, String condition) {
        Equipment e = new Equipment();
        e.setEquipmentCode(code);
        e.setBranchCode(branch);
        e.setEquipmentName(name);
        e.setStockQuantity(stock);
        e.setRentalPrice(price);
        e.setCondition(condition);
        return e;
    }

    private void seedProducts() {
        if (productRepository.count() < 11) {
            List<Product> products = List.of(
                createProduct("P01", "CN01", "Nước bù khoáng Revive Chanh muối 500ml", "Nước giải khát", "Chai", new BigDecimal("15000"), new BigDecimal("8000"), 150, 20),
                createProduct("P02", "CN01", "Nước điện giải Pocari Sweat 500ml", "Nước giải khát", "Chai", new BigDecimal("18000"), new BigDecimal("11000"), 120, 20),
                createProduct("P03", "CN01", "Nước suối tinh khiết Aquafina 500ml", "Nước giải khát", "Chai", new BigDecimal("10000"), new BigDecimal("5000"), 220, 30),
                createProduct("P04", "CN01", "Nước tăng lực Bò Húc Red Bull Thái", "Nước giải khát", "Lon", new BigDecimal("20000"), new BigDecimal("12000"), 95, 15),
                createProduct("P05", "CN01", "Ống Cầu Lông Hải Yến Đỏ (12 quả)", "Cầu lông", "Ống", new BigDecimal("180000"), new BigDecimal("145000"), 65, 10),
                createProduct("P06", "CN01", "Ống Cầu Lông Thành Công 77 (12 quả)", "Cầu lông", "Ống", new BigDecimal("210000"), new BigDecimal("175000"), 40, 10),
                createProduct("P07", "CN01", "Quấn cán vợt cầu lông VS chống trượt", "Phụ kiện", "Cái", new BigDecimal("15000"), new BigDecimal("7000"), 85, 15),
                createProduct("P08", "CN01", "Khăn lau mồ hôi thể thao Yonex", "Phụ kiện", "Cái", new BigDecimal("45000"), new BigDecimal("25000"), 30, 5),
                createProduct("P09", "CN02", "Nước bù khoáng Revive Chanh muối 500ml", "Nước giải khát", "Chai", new BigDecimal("15000"), new BigDecimal("8000"), 110, 20),
                createProduct("P10", "CN03", "Nước bù khoáng Revive Chanh muối 500ml", "Nước giải khát", "Chai", new BigDecimal("15000"), new BigDecimal("8000"), 90, 20),
                createProduct("P11", "CN04", "Nước bù khoáng Revive Chanh muối 500ml", "Nước giải khát", "Chai", new BigDecimal("15000"), new BigDecimal("8000"), 100, 20)
            );
            for (Product p : products) {
                if (productRepository.findByProductCode(p.getProductCode()).isEmpty()) {
                    productRepository.save(p);
                }
            }
        }
    }

    private Product createProduct(String code, String branch, String name, String cat, String unit, BigDecimal price, BigDecimal cost, int stock, int min) {
        Product p = new Product();
        p.setProductCode(code);
        p.setBranchCode(branch);
        p.setProductName(name);
        p.setCategory(cat);
        p.setUnit(unit);
        p.setUnitPrice(price);
        p.setCostPrice(cost);
        p.setStockQuantity(stock);
        p.setMinQuantity(min);
        return p;
    }

    private void seedBookings() {
        if (bookingRepository.count() < 25) {
            LocalDate today = LocalDate.now();
            List<Booking> bookings = List.of(
                // Hôm nay (Live Operations)
                createBooking("DS0101", "KH002", "Nguyễn Hoàng Nam", "0903123456", "CL02", "CN01", today, "08:00 - 10:00", new BigDecimal("90000"), new BigDecimal("180000"), new BigDecimal("54000"), "Tiền mặt", "Đang sử dụng", "Khách quen tuần 3 buổi"),
                createBooking("DS0102", "KH001", "Lê Bá Đạt", "0912345678", "CL01", "CN01", today, "09:00 - 10:00", new BigDecimal("120000"), new BigDecimal("120000"), new BigDecimal("36000"), "Tiền mặt", "Hoàn thành", "Đã thanh toán đủ tiền mặt"),
                createBooking("DS0103", "KH003", "Phạm Gia Huy", "0988777666", "CL05", "CN01", today, "18:00 - 20:00", new BigDecimal("120000"), new BigDecimal("240000"), new BigDecimal("72000"), "Chuyển khoản VietQR", "Đang sử dụng", "Đặt sân VIP tối"),
                createBooking("DS0104", "KH004", "Vũ Thị Mai Anh", "0977555444", "CL03", "CN01", today, "19:00 - 20:00", new BigDecimal("90000"), new BigDecimal("90000"), new BigDecimal("27000"), "VietQR Pro", "Chờ xác nhận", "Khách đặt qua web portal"),
                // Ngày mai
                createBooking("DS0105", "KH005", "Trần Thanh Hằng", "0933444555", "CL01", "CN01", today.plusDays(1), "17:00 - 18:00", new BigDecimal("120000"), new BigDecimal("120000"), new BigDecimal("36000"), "Chuyển khoản VietQR", "Đã cọc 30%", "Chuyển khoản VietQR giữ chỗ 10p"),
                createBooking("DS0106", "KH006", "Bùi Khánh Linh", "0909111222", "CL04", "CN01", today.plusDays(1), "20:00 - 22:00", new BigDecimal("90000"), new BigDecimal("180000"), new BigDecimal("54000"), "Tiền mặt", "Đã xác nhận", "Giao lưu công ty FPT"),
                // Hôm qua
                createBooking("DS0107", "KH002", "Đỗ Quang Vinh", "0944333222", "CL02", "CN01", today.minusDays(1), "15:00 - 16:00", new BigDecimal("90000"), new BigDecimal("90000"), BigDecimal.ZERO, "Tiền mặt", "Đã hủy", "Bận công tác đột xuất"),
                // Các chi nhánh khác
                createBooking("DS0201", "KH001", "Lê Bá Đạt", "0912345678", "BT01", "CN02", today, "18:00 - 20:00", new BigDecimal("120000"), new BigDecimal("240000"), new BigDecimal("72000"), "Chuyển khoản VietQR", "Đang sử dụng", "Sân VIP Bình Thạnh"),
                createBooking("DS0202", "KH003", "Phạm Gia Huy", "0988777666", "BT04", "CN02", today, "19:00 - 21:00", new BigDecimal("90000"), new BigDecimal("180000"), new BigDecimal("54000"), "Tiền mặt", "Đang sử dụng", "Tập luyện đôi nam"),
                createBooking("DS0301", "KH004", "Vũ Thị Mai Anh", "0977555444", "Q902", "CN03", today, "17:00 - 19:00", new BigDecimal("90000"), new BigDecimal("180000"), new BigDecimal("54000"), "VietQR Pro", "Đang sử dụng", "Nhóm bạn sinh viên"),
                createBooking("DS0401", "KH005", "Trần Thanh Hằng", "0933444555", "GV02", "CN04", today, "18:00 - 20:00", new BigDecimal("90000"), new BigDecimal("180000"), new BigDecimal("54000"), "Tiền mặt", "Đang sử dụng", "Khách vãng lai chi nhánh GV"),
                // Lịch sử doanh thu
                createBooking("DS01_1001_1", "KH001", "Lê Bá Đạt", "0912345678", "CL01", "CN01", today.minusDays(5), "08:00 - 10:00", new BigDecimal("120000"), new BigDecimal("240000"), new BigDecimal("72000"), "Tiền mặt", "Hoàn thành", "Khách quen tập sáng"),
                createBooking("DS01_1001_2", "KH003", "Phạm Gia Huy", "0988777666", "CL03", "CN01", today.minusDays(5), "17:00 - 19:00", new BigDecimal("90000"), new BigDecimal("180000"), new BigDecimal("54000"), "VietQR Pro", "Hoàn thành", "Đặt sân sau giờ làm"),
                createBooking("DS01_1002_1", "KH002", "Nguyễn Hoàng Nam", "0903123456", "CL02", "CN01", today.minusDays(4), "09:00 - 11:00", new BigDecimal("90000"), new BigDecimal("180000"), new BigDecimal("54000"), "Tiền mặt", "Hoàn thành", "Tập thể lực"),
                createBooking("DS01_1002_2", "KH005", "Trần Thanh Hằng", "0933444555", "CL04", "CN01", today.minusDays(4), "18:00 - 20:00", new BigDecimal("90000"), new BigDecimal("180000"), new BigDecimal("54000"), "VietQR Pro", "Hoàn thành", "CLB Thủ Đức"),
                createBooking("DS01_1003_1", "KH001", "Lê Bá Đạt", "0912345678", "CL01", "CN01", today.minusDays(3), "15:00 - 17:00", new BigDecimal("120000"), new BigDecimal("240000"), new BigDecimal("72000"), "Chuyển khoản VietQR", "Hoàn thành", "Tập đôi nam"),
                createBooking("DS01_1003_2", "KH003", "Phạm Gia Huy", "0988777666", "CL05", "CN01", today.minusDays(3), "17:00 - 19:00", new BigDecimal("90000"), new BigDecimal("180000"), new BigDecimal("54000"), "VietQR Pro", "Hoàn thành", "Khách vãng lai"),
                createBooking("DS01_1004_1", "KH002", "Nguyễn Hoàng Nam", "0903123456", "CL02", "CN01", today.minusDays(2), "08:00 - 10:00", new BigDecimal("90000"), new BigDecimal("180000"), new BigDecimal("54000"), "Tiền mặt", "Hoàn thành", "Khách cố định"),
                createBooking("DS01_1004_2", "KH005", "Trần Thanh Hằng", "0933444555", "CL03", "CN01", today.minusDays(2), "10:00 - 12:00", new BigDecimal("90000"), new BigDecimal("180000"), new BigDecimal("54000"), "VietQR Pro", "Hoàn thành", "Tập kỹ thuật"),
                createBooking("DS01_1005_1", "KH001", "Lê Bá Đạt", "0912345678", "CL01", "CN01", today.minusDays(2), "18:00 - 20:00", new BigDecimal("120000"), new BigDecimal("240000"), new BigDecimal("72000"), "VietQR Pro", "Hoàn thành", "Tối thứ 2"),
                createBooking("DS01_1006_1", "KH002", "Nguyễn Hoàng Nam", "0903123456", "CL02", "CN01", today.minusDays(1), "17:00 - 19:00", new BigDecimal("90000"), new BigDecimal("180000"), new BigDecimal("54000"), "Tiền mặt", "Hoàn thành", "Tập phát cầu"),
                createBooking("DS01_1007_1", "KH001", "Lê Bá Đạt", "0912345678", "CL01", "CN01", today.minusDays(1), "09:00 - 11:00", new BigDecimal("120000"), new BigDecimal("240000"), new BigDecimal("72000"), "Chuyển khoản VietQR", "Hoàn thành", "Khách quen VIP"),
                createBooking("DS01_1008_1", "KH002", "Nguyễn Hoàng Nam", "0903123456", "CL02", "CN01", today.minusDays(1), "18:00 - 20:00", new BigDecimal("90000"), new BigDecimal("180000"), new BigDecimal("54000"), "Tiền mặt", "Hoàn thành", "Định kỳ thứ 5"),
                createBooking("DS01_1009_1", "KH001", "Lê Bá Đạt", "0912345678", "CL01", "CN01", today.minusDays(1), "17:00 - 19:00", new BigDecimal("120000"), new BigDecimal("240000"), new BigDecimal("72000"), "Chuyển khoản VietQR", "Hoàn thành", "VIP Thứ 6")
            );
            for (Booking b : bookings) {
                if (bookingRepository.findByBookingCode(b.getBookingCode()).isEmpty()) {
                    bookingRepository.save(b);
                }
            }
        }
    }

    private Booking createBooking(String code, String custCode, String custName, String phone, String court, String branch, LocalDate date, String slot, BigDecimal hourly, BigDecimal total, BigDecimal deposit, String method, String status, String notes) {
        Booking b = new Booking();
        b.setBookingCode(code);
        b.setCustomerCode(custCode);
        b.setCustomerName(custName);
        b.setCustomerPhone(phone);
        b.setCourtCode(court);
        b.setBranchCode(branch);
        b.setBookingDate(date);
        b.setTimeSlot(slot);
        b.setHourlyPrice(hourly);
        b.setTotalPrice(total);
        b.setDepositAmount(deposit);
        b.setPaymentMethod(method);
        b.setStatus(status);
        b.setNotes(notes);
        b.setCreatedAt(LocalDateTime.now());
        return b;
    }

    private void seedTournaments() {
        if (tournamentRepository.count() < 3) {
            tournamentRepository.saveAll(List.of(
                createTournament("GD01", "CN01", "Giải Cầu Lông Đôi Nam Nữ Mùa Thu UTE Open 2026", "Đôi nam nữ", LocalDateTime.of(2026, 10, 10, 8, 0), LocalDateTime.of(2026, 10, 12, 18, 0), "Thi đấu theo thể thức loại trực tiếp 3 hiệp 21 điểm theo luật BWF. Mỗi VĐV chỉ được đăng ký 1 nội dung.", 32, 24, new BigDecimal("300000"), new BigDecimal("20000000"), "Đang mở đăng ký"),
                createTournament("GD02", "CN01", "Giải Cầu Lông Doanh Nghiệp & Sinh Viên Cúp Thủ Đức 2026", "Tổng hợp nhiều nội dung", LocalDateTime.of(2026, 10, 25, 8, 0), LocalDateTime.of(2026, 10, 26, 17, 0), "Giải đấu phong trào kết nối các doanh nghiệp và cựu sinh viên đam mê cầu lông.", 24, 0, new BigDecimal("250000"), new BigDecimal("15000000"), "Sắp mở đăng ký"),
                createTournament("GD03", "CN02", "Giải Đơn Nam Bình Thạnh Mở Rộng 2026", "Đơn nam", LocalDateTime.of(2026, 11, 5, 8, 0), LocalDateTime.of(2026, 11, 6, 18, 0), "Giải đơn nam các CLB phong trào khu vực Bình Thạnh - Phú Nhuận.", 16, 0, new BigDecimal("200000"), new BigDecimal("10000000"), "Sắp mở đăng ký")
            ));
        }
    }

    private Tournament createTournament(String code, String branch, String name, String cat, LocalDateTime start, LocalDateTime end, String rules, int max, int cur, BigDecimal fee, BigDecimal prize, String status) {
        Tournament t = new Tournament();
        t.setTournamentCode(code);
        t.setBranchCode(branch);
        t.setTournamentName(name);
        t.setCategory(cat);
        t.setStartDate(start);
        t.setEndDate(end);
        t.setRules(rules);
        t.setMaxParticipants(max);
        t.setCurrentParticipants(cur);
        t.setEntryFee(fee);
        t.setTotalPrize(prize);
        t.setStatus(status);
        return t;
    }

    private void seedTournamentRegistrations() {
        if (tournamentRegistrationRepository.count() < 5) {
            tournamentRegistrationRepository.saveAll(List.of(
                createTournamentReg("GD01", "KH001", "Cặp Đôi Đạt & Trang (CLB Thủ Đức)", "Đôi nam nữ", "0912345678", "Đã xác nhận"),
                createTournamentReg("GD01", "KH002", "Cặp Đôi Nam & Thảo (ĐH Sư Phạm Kỹ Thuật)", "Đôi nam nữ", "0903123456", "Đã xác nhận"),
                createTournamentReg("GD01", "KH003", "Cặp Đôi Huy & Hằng (Công ty TMA)", "Đôi nam nữ", "0988777666", "Đã xác nhận"),
                createTournamentReg("GD01", "KH004", "Cặp Đôi Mai Anh & Đức (CLB Yonex)", "Đôi nam nữ", "0977555444", "Đã xác nhận"),
                createTournamentReg("GD01", "KH005", "Cặp Đôi Tuấn & Lan (CLB Bình Thạnh)", "Đôi nam nữ", "0933221100", "Đã xác nhận")
            ));
        }
    }

    private TournamentRegistration createTournamentReg(String tourCode, String custCode, String team, String cat, String phone, String status) {
        TournamentRegistration reg = new TournamentRegistration();
        reg.setTournamentCode(tourCode);
        reg.setCustomerCode(custCode);
        reg.setTeamName(team);
        reg.setCategory(cat);
        reg.setPhone(phone);
        reg.setStatus(status);
        reg.setRegistrationDate(LocalDateTime.now());
        return reg;
    }

    private void seedPayments() {
        if (paymentRepository.count() < 6) {
            paymentRepository.saveAll(List.of(
                createPayment("TT0101", "DS0101", new BigDecimal("54000"), "Tiền mặt", "Đặt cọc 30%", "CASH-20260919-01", "Thành công"),
                createPayment("TT0102", "DS0102", new BigDecimal("36000"), "Tiền mặt", "Đặt cọc 30%", "CASH-20260919-02", "Thành công"),
                createPayment("TT0103", "DS0103", new BigDecimal("72000"), "VietQR Pro", "Đặt cọc 30%", "MB-998822110034", "Thành công"),
                createPayment("TT0104", "DS0104", new BigDecimal("27000"), "VietQR Pro", "Đặt cọc 30%", "VCB-776655441122", "Đang xử lý"),
                createPayment("TT0105", "DS0105", new BigDecimal("36000"), "VietQR Pro", "Đặt cọc 30%", "TCB-334455667788", "Thành công"),
                createPayment("TT0106", "DS0201", new BigDecimal("72000"), "VietQR Pro", "Đặt cọc 30%", "MB-556677889900", "Thành công")
            ));
        }
    }

    private Payment createPayment(String code, String bookingCode, BigDecimal amount, String method, String type, String ref, String status) {
        Payment p = new Payment();
        p.setPaymentCode(code);
        p.setBookingCode(bookingCode);
        p.setAmount(amount);
        p.setMethod(method);
        p.setTransactionType(type);
        p.setTransactionRef(ref);
        p.setStatus(status);
        p.setPaymentTime(LocalDateTime.now());
        return p;
    }

    private void seedWorkShifts() {
        if (workShiftRepository.count() < 5) {
            LocalDate today = LocalDate.now();
            workShiftRepository.saveAll(List.of(
                createWorkShift("CA01", "NS03", today, "06:00", "14:00", "Ca Sáng", "Đang trực"),
                createWorkShift("CA02", "NS04", today, "14:00", "22:00", "Ca Chiều", "Đã phân ca"),
                createWorkShift("CA03", "NS03", today.plusDays(1), "06:00", "14:00", "Ca Sáng", "Đã phân ca"),
                createWorkShift("CA04", "NS04", today.plusDays(1), "14:00", "22:00", "Ca Chiều", "Đã phân ca"),
                createWorkShift("CA05", "NS08", today, "17:00", "23:00", "Ca Tối", "Đang trực")
            ));
        }
    }

    private WorkShift createWorkShift(String code, String staffCode, LocalDate date, String start, String end, String type, String status) {
        WorkShift ws = new WorkShift();
        ws.setShiftCode(code);
        ws.setStaffCode(staffCode);
        ws.setShiftDate(date);
        ws.setStartTime(start);
        ws.setEndTime(end);
        ws.setShiftType(type);
        ws.setStatus(status);
        ws.setBranchCode("CN01");
        ws.setDuty("POS Bán hàng & Quản lý");
        return ws;
    }

    private void seedPromotions() {
        if (promotionRepository.count() < 3) {
            promotionRepository.saveAll(List.of(
                createPromotion("KM01", "Ưu Đãi Giờ Vàng Thứ Tư Hàng Tuần", "Phần trăm (%)", new BigDecimal("15"), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), "Toàn chuỗi", "Đang áp dụng"),
                createPromotion("KM02", "Đặc Quyền Hội Viên Kim Cương & Vàng", "Phần trăm (%)", new BigDecimal("15"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "Toàn chuỗi", "Đang áp dụng"),
                createPromotion("KM03", "Khai Trương Cụm Sân Gò Vấp - Tặng 1 Giờ Đánh", "Số tiền (VNĐ)", new BigDecimal("90000"), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "Chi nhánh Gò Vấp", "Đang áp dụng")
            ));
        }
    }

    private Promotion createPromotion(String code, String name, String type, BigDecimal val, LocalDate start, LocalDate end, String scope, String status) {
        Promotion p = new Promotion();
        p.setPromoCode(code);
        p.setPromoName(name);
        p.setDiscountType(type);
        p.setDiscountValue(val);
        p.setStartDate(start);
        p.setEndDate(end);
        p.setScope(scope);
        p.setStatus(status);
        return p;
    }

    private void seedSystemSettings() {
        if (systemSettingRepository.count() < 6) {
            systemSettingRepository.saveAll(List.of(
                createSetting("CFG_DEPOSIT_RATE", "Tỷ lệ đặt cọc tối thiểu (%)", "30", "Khách hàng phải thanh toán cọc ít nhất 30% để giữ sân hợp lệ (QĐ-KH02)"),
                createSetting("CFG_HOLDING_MINUTES", "Thời gian tạm giữ chỗ chờ cọc (Phút)", "10", "Sau 10 phút nếu không có giao dịch cọc, hệ thống tự động giải phóng ô sân (QĐ-KH02)"),
                createSetting("CFG_CANCEL_HOURS", "Thời hạn hủy sân tối thiểu để được hoàn cọc (Giờ)", "4", "Hủy trước giờ chơi tối thiểu 4 tiếng được hoàn 100% tiền cọc (QĐ-KH03)"),
                createSetting("CFG_ADVANCE_DAYS", "Số ngày tối đa được phép đặt trước", "30", "Giới hạn đặt sân trong vòng 30 ngày tới"),
                createSetting("CFG_VNPAY_TMN", "Mã đối tác VNPay (TMN Code)", "UTESPORT2026", "Tích hợp cổng thanh toán VNPay QR Sandbox/Production"),
                createSetting("CFG_MOMO_PARTNER", "Partner Code MoMo Business", "MOMO_UTE_BADMINTON", "Tích hợp cổng Ví điện tử MoMo Business")
            ));
        }
    }

    private SystemSetting createSetting(String code, String name, String val, String desc) {
        SystemSetting s = new SystemSetting();
        s.setSettingCode(code);
        s.setSettingName(name);
        s.setSettingValue(val);
        s.setDescription(desc);
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }

    private void seedStockReceipts() {
        if (stockReceiptRepository.count() < 2) {
            StockReceipt r1 = new StockReceipt();
            r1.setReceiptCode("PN0101");
            r1.setBranchCode("CN01");
            r1.setStaffCode("NS02");
            r1.setReceiptDate(LocalDateTime.now().minusDays(5));
            r1.setSupplierName("Công ty TNHH Thể Thao Yonex Việt Nam");
            r1.setTotalAmount(new BigDecimal("14500000"));
            r1.setNotes("Nhập bổ sung vợt Yonex Astrox và 50 ống cầu Hải Yến");
            stockReceiptRepository.save(r1);

            StockReceipt r2 = new StockReceipt();
            r2.setReceiptCode("PN0102");
            r2.setBranchCode("CN01");
            r2.setStaffCode("NS02");
            r2.setReceiptDate(LocalDateTime.now().minusDays(2));
            r2.setSupplierName("NPP Dụng Cụ Cầu Lông Đại Phát");
            r2.setTotalAmount(new BigDecimal("4200000"));
            r2.setNotes("Nhập nước bù khoáng Revive và Pocari Sweat đợt cuối tháng");
            stockReceiptRepository.save(r2);
        }
    }
}
