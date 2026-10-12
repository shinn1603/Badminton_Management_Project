package vn.yain.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.yain.entity.*;
import vn.yain.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private StockReceiptRepository stockReceiptRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("====== KHỞI TẠO CƠ SỞ DỮ LIỆU TỰ ĐỘNG UTE SPORT ARENA ======");
        initBranches();
        initCourts();
        initUsers();
        initCustomers();
        initProducts();
        initEquipment();
        initStaff();
        initStockReceipts();
        initSampleBookings();
        log.info("====== KHỞI TẠO DỮ LIỆU HOÀN TẤT - SẴN SÀNG TRẢI NGHIỆM GIAO DIỆN ======");
    }

    private void initBranches() {
        if (branchRepository.count() == 0) {
            log.info("Seeding 4 chi nhánh chính...");
            Branch b1 = new Branch(null, "CN01", "Chi nhánh Thủ Đức", "01 Võ Văn Ngân, TP. Thủ Đức", "0901234567", "Trần Phúc Bảo", 8, "Hoạt động");
            Branch b2 = new Branch(null, "CN02", "Chi nhánh Bình Thạnh", "152 Điện Biên Phủ, Bình Thạnh", "0902345678", "Nguyễn Văn Quản", 6, "Hoạt động");
            Branch b3 = new Branch(null, "CN03", "Chi nhánh Quận 9", "70 Tô Ngọc Vân, TP. Thủ Đức (Q.9)", "0903456789", "Trần Hữu Nam", 6, "Hoạt động");
            Branch b4 = new Branch(null, "CN04", "Chi nhánh Gò Vấp", "45 Quang Trung, Gò Vấp", "0904567890", "Lê Hoàng Khang", 6, "Hoạt động");
            branchRepository.saveAll(Arrays.asList(b1, b2, b3, b4));
        }
    }

    private void initCourts() {
        if (courtRepository.count() == 0) {
            log.info("Seeding 26 sân cầu lông toàn chuỗi...");
            List<Court> courts = Arrays.asList(
                // Chi nhánh 1: Thủ Đức (8 sân) - CL01..CL08
                new Court(null, "CL01", "Sân cầu lông 1 (VIP Yonex)", "Cầu lông VIP", "CN01", new BigDecimal("120000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "CL02", "Sân cầu lông 2", "Cầu lông tiêu chuẩn", "CN01", new BigDecimal("90000"), "Đang sử dụng", "/images/badminton-court.jpg"),
                new Court(null, "CL03", "Sân cầu lông 3", "Cầu lông tiêu chuẩn", "CN01", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "CL04", "Sân cầu lông 4", "Cầu lông tiêu chuẩn", "CN01", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "CL05", "Sân cầu lông 5", "Cầu lông tiêu chuẩn", "CN01", new BigDecimal("90000"), "Đang sử dụng", "/images/badminton-court.jpg"),
                new Court(null, "CL06", "Sân cầu lông 6 (VIP Yonex)", "Cầu lông VIP", "CN01", new BigDecimal("120000"), "Bảo trì", "/images/badminton-court.jpg"),
                new Court(null, "CL07", "Sân cầu lông 7", "Cầu lông tiêu chuẩn", "CN01", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "CL08", "Sân cầu lông 8", "Cầu lông tiêu chuẩn", "CN01", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),

                // Chi nhánh 2: Bình Thạnh (6 sân) - BT01..BT06
                new Court(null, "BT01", "Sân BT 1 (VIP Victor)", "Cầu lông VIP", "CN02", new BigDecimal("120000"), "Đang sử dụng", "/images/badminton-court.jpg"),
                new Court(null, "BT02", "Sân BT 2", "Cầu lông tiêu chuẩn", "CN02", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "BT03", "Sân BT 3", "Cầu lông tiêu chuẩn", "CN02", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "BT04", "Sân BT 4", "Cầu lông tiêu chuẩn", "CN02", new BigDecimal("90000"), "Đang sử dụng", "/images/badminton-court.jpg"),
                new Court(null, "BT05", "Sân BT 5", "Cầu lông tiêu chuẩn", "CN02", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "BT06", "Sân BT 6 (VIP)", "Cầu lông VIP", "CN02", new BigDecimal("120000"), "Trống", "/images/badminton-court.jpg"),

                // Chi nhánh 3: Quận 9 (6 sân) - Q901..Q906
                new Court(null, "Q901", "Sân Q9 1 (VIP Lining)", "Cầu lông VIP", "CN03", new BigDecimal("120000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "Q902", "Sân Q9 2", "Cầu lông tiêu chuẩn", "CN03", new BigDecimal("90000"), "Đang sử dụng", "/images/badminton-court.jpg"),
                new Court(null, "Q903", "Sân Q9 3", "Cầu lông tiêu chuẩn", "CN03", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "Q904", "Sân Q9 4", "Cầu lông tiêu chuẩn", "CN03", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "Q905", "Sân Q9 5", "Cầu lông tiêu chuẩn", "CN03", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "Q906", "Sân Q9 6", "Cầu lông tiêu chuẩn", "CN03", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),

                // Chi nhánh 4: Gò Vấp (6 sân) - GV01..GV06
                new Court(null, "GV01", "Sân GV 1 (VIP Yonex)", "Cầu lông VIP", "CN04", new BigDecimal("120000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "GV02", "Sân GV 2", "Cầu lông tiêu chuẩn", "CN04", new BigDecimal("90000"), "Đang sử dụng", "/images/badminton-court.jpg"),
                new Court(null, "GV03", "Sân GV 3", "Cầu lông tiêu chuẩn", "CN04", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "GV04", "Sân GV 4", "Cầu lông tiêu chuẩn", "CN04", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "GV05", "Sân GV 5", "Cầu lông tiêu chuẩn", "CN04", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg"),
                new Court(null, "GV06", "Sân GV 6", "Cầu lông tiêu chuẩn", "CN04", new BigDecimal("90000"), "Trống", "/images/badminton-court.jpg")
            );
            courtRepository.saveAll(courts);
        }
    }

    private void initUsers() {
        if (userRepository.count() == 0) {
            log.info("Seeding tài khoản người dùng...");
            String hashPass = passwordEncoder.encode("123456");

            User u1 = new User(null, "director", hashPass, "Nguyễn Phước Thọ", "director@utesport.vn", "0901111222", "DIRECTOR", "ALL", "Hoạt động", LocalDateTime.now());
            User u2 = new User(null, "manager", hashPass, "Trần Phúc Bảo", "manager@utesport.vn", "0903123456", "MANAGER", "CN01", "Hoạt động", LocalDateTime.now());
            User u3 = new User(null, "pos", hashPass, "Nguyễn Thanh Tâm", "pos@utesport.vn", "0904555666", "POS", "CN01", "Hoạt động", LocalDateTime.now());
            User u4 = new User(null, "admin", hashPass, "Trần Biểu Hương", "admin@utesport.vn", "0908888999", "ADMIN", "ALL", "Hoạt động", LocalDateTime.now());
            User u5 = new User(null, "customer", hashPass, "Lê Bá Đạt", "khachhang@gmail.com", "0912345678", "CUSTOMER", "CN01", "Hoạt động", LocalDateTime.now());
            User u6 = new User(null, "mai.lt", hashPass, "Lê Thị Mai", "mai.lt@utesport.vn", "0904777888", "POS", "CN01", "Hoạt động", LocalDateTime.now());

            userRepository.saveAll(Arrays.asList(u1, u2, u3, u4, u5, u6));
        }
    }

    private void initCustomers() {
        if (customerRepository.count() == 0) {
            log.info("Seeding khách hàng hội viên...");
            String hashPass = passwordEncoder.encode("123456");

            Customer c1 = new Customer(null, "KH01", "Lê Bá Đạt", "0912345678", "khachhang@gmail.com", hashPass, new BigDecimal("42.5"), 350, "Vàng", "Hoạt động", LocalDateTime.now());
            Customer c2 = new Customer(null, "KH02", "Trần Minh Quân", "0934567890", "quan.tm@gmail.com", hashPass, new BigDecimal("24.0"), 180, "Bạc", "Hoạt động", LocalDateTime.now());
            Customer c3 = new Customer(null, "KH03", "Phạm Hoàng Anh", "0988776655", "anh.ph@gmail.com", hashPass, new BigDecimal("98.0"), 820, "Kim Cương", "Hoạt động", LocalDateTime.now());
            Customer c4 = new Customer(null, "KH04", "Vũ Hải Đăng", "0977112233", "dang.vh@gmail.com", hashPass, new BigDecimal("6.0"), 50, "Đồng", "Hoạt động", LocalDateTime.now());

            customerRepository.saveAll(Arrays.asList(c1, c2, c3, c4));
        }
    }

    private void initProducts() {
        if (productRepository.count() == 0) {
            log.info("Seeding sản phẩm quầy F&B và dụng cụ...");
            List<Product> products = Arrays.asList(
                new Product(null, "P01", "Nước bù khoáng Revive Chanh muối 500ml", "Nước giải khát", "Chai", new BigDecimal("15000"), new BigDecimal("8000"), 150, 20, "CN01"),
                new Product(null, "P02", "Nước điện giải Pocari Sweat 500ml", "Nước giải khát", "Chai", new BigDecimal("18000"), new BigDecimal("11000"), 120, 20, "CN01"),
                new Product(null, "P03", "Nước suối tinh khiết Aquafina 500ml", "Nước giải khát", "Chai", new BigDecimal("10000"), new BigDecimal("5000"), 220, 30, "CN01"),
                new Product(null, "P04", "Nước tăng lực Bò Húc Red Bull Thái", "Nước giải khát", "Lon", new BigDecimal("20000"), new BigDecimal("12000"), 95, 15, "CN01"),
                new Product(null, "P05", "Ống Cầu Lông Hải Yến Đỏ (12 quả)", "Cầu lông", "Ống", new BigDecimal("180000"), new BigDecimal("145000"), 65, 10, "CN01"),
                new Product(null, "P06", "Ống Cầu Lông Thành Công 77 (12 quả)", "Cầu lông", "Ống", new BigDecimal("210000"), new BigDecimal("175000"), 40, 10, "CN01"),
                new Product(null, "P07", "Quấn cán vợt VS chống trượt", "Phụ kiện", "Cái", new BigDecimal("15000"), new BigDecimal("7000"), 85, 15, "CN01"),
                new Product(null, "P08", "Khăn lau mồ hôi thể thao Yonex", "Phụ kiện", "Cái", new BigDecimal("45000"), new BigDecimal("25000"), 30, 5, "CN01"),
                new Product(null, "P09", "Nước bù khoáng Revive Chanh muối 500ml", "Nước giải khát", "Chai", new BigDecimal("15000"), new BigDecimal("8000"), 110, 20, "CN02"),
                new Product(null, "P10", "Nước bù khoáng Revive Chanh muối 500ml", "Nước giải khát", "Chai", new BigDecimal("15000"), new BigDecimal("8000"), 90, 20, "CN03"),
                new Product(null, "P11", "Nước bù khoáng Revive Chanh muối 500ml", "Nước giải khát", "Chai", new BigDecimal("15000"), new BigDecimal("8000"), 100, 20, "CN04")
            );
            productRepository.saveAll(products);
        }
    }

    private void initEquipment() {
        if (equipmentRepository.count() == 0) {
            log.info("Seeding dụng cụ thuê sân...");
            List<Equipment> equipment = Arrays.asList(
                new Equipment("DC01", "CN01", "Vợt Yonex Astrox 77 Play (4U/G5)", 18, new BigDecimal("25000"), "Mới 99%"),
                new Equipment("DC02", "CN01", "Vợt Yonex Astrox 88D Tour", 12, new BigDecimal("35000"), "Tốt"),
                new Equipment("DC03", "CN01", "Vợt Victor Thruster K 9900", 10, new BigDecimal("30000"), "Tốt"),
                new Equipment("DC04", "CN01", "Vợt Lining Axforce 80", 8, new BigDecimal("30000"), "Tốt"),
                new Equipment("DC05", "CN01", "Ống Cầu Lông Hải Yến Đỏ (12 quả)", 45, new BigDecimal("180000"), "Mới nguyên hộp"),
                new Equipment("DC06", "CN01", "Ống Cầu Lông Thành Công 77", 35, new BigDecimal("210000"), "Mới nguyên hộp"),
                new Equipment("DC07", "CN02", "Vợt Yonex Astrox 77 Play", 15, new BigDecimal("25000"), "Tốt"),
                new Equipment("DC08", "CN03", "Vợt Yonex Astrox 77 Play", 15, new BigDecimal("25000"), "Tốt"),
                new Equipment("DC09", "CN04", "Vợt Yonex Astrox 77 Play", 15, new BigDecimal("25000"), "Tốt")
            );
            equipmentRepository.saveAll(equipment);
        }
    }

    private void initStaff() {
        if (staffRepository.count() == 0) {
            log.info("Seeding danh sách nhân sự chi nhánh...");
            List<Staff> staffList = Arrays.asList(
                Staff.builder().staffCode("NV-01").branchCode("CN01").fullName("Trần Phúc Bảo").position("Quản lý chi nhánh").phone("0903123456").email("manager@utesport.vn").address("Thủ Đức, TP.HCM").status("Đang làm việc").build(),
                Staff.builder().staffCode("NV-02").branchCode("CN01").fullName("Nguyễn Thanh Tâm").position("Thu ngân POS quầy").phone("0904555666").email("pos@utesport.vn").address("Bình Thạnh, TP.HCM").status("Đang làm việc").build(),
                Staff.builder().staffCode("NV-03").branchCode("CN01").fullName("Bùi Khánh Linh").position("Lễ tân & Dịch vụ").phone("0909111222").email("linh.bk@utesport.vn").address("Thủ Đức, TP.HCM").status("Đang làm việc").build(),
                Staff.builder().staffCode("NV-04").branchCode("CN01").fullName("Nguyễn Hoàng Nam").position("Thu ngân POS quầy").phone("0903888777").email("nam.nh@utesport.vn").address("Quận 9, TP.HCM").status("Đang làm việc").build(),
                Staff.builder().staffCode("NV-05").branchCode("CN01").fullName("Phạm Gia Huy").position("Kỹ thuật & Bảo dưỡng sân").phone("0988777666").email("huy.pg@utesport.vn").address("Thủ Đức, TP.HCM").status("Đang làm việc").build(),
                Staff.builder().staffCode("NV-06").branchCode("CN01").fullName("Trịnh Gia Vinh").position("Kỹ thuật & Bảo dưỡng sân").phone("0977222333").email("vinh.tg@utesport.vn").address("Gò Vấp, TP.HCM").status("Đang làm việc").build()
            );
            staffRepository.saveAll(staffList);
        }
    }

    private void initStockReceipts() {
        if (stockReceiptRepository.count() == 0) {
            log.info("Seeding lịch sử phiếu nhập kho mẫu...");
            StockReceipt sr1 = new StockReceipt(null, "PNK-2026-001", "CN01", "NS02",
                    LocalDateTime.now().minusDays(3), "Đại lý Nước giải khát Tuấn Anh", new BigDecimal("1850000"), "Nhập 50 chai Nước Aquafina, 30 chai Pocari Sweat");
            StockReceipt sr2 = new StockReceipt(null, "PNK-2026-002", "CN01", "NS02",
                    LocalDateTime.now().minusDays(1), "Công ty TNHH Thể Thao Yonex VN", new BigDecimal("4500000"), "Nhập 10 ống cầu Yonex Aerosensa 50, 20 quấn cán AC102EX");
            StockReceipt sr3 = new StockReceipt(null, "PNK-2026-003", "CN01", "NS02",
                    LocalDateTime.now(), "Phụ kiện Cầu lông Victor Sài Gòn", new BigDecimal("2900000"), "Nhập 20 quấn cán vợt VS, 5 ống cầu Victor Master No.1");
            stockReceiptRepository.saveAll(Arrays.asList(sr1, sr2, sr3));
        }
    }

    private void initSampleBookings() {
        LocalDate today = LocalDate.now();
        List<Booking> todayBookings = bookingRepository.findByBranchCodeAndBookingDate("CN01", today);
        if (todayBookings.size() < 10) {
            log.info("Seeding dữ liệu đặt sân mẫu cho toàn bộ 4 chi nhánh ngày hôm nay ({}) và các ngày trong tháng...", today);
            bookingRepository.deleteAll();

            List<Booking> bookings = new java.util.ArrayList<>();

            // Seed historical bookings across the month (days 1, 3, 5, 7, 8, 10, etc.)
            for (int dayOffset = 1; dayOffset <= 12; dayOffset++) {
                LocalDate histDate = today.withDayOfMonth(Math.min(dayOffset, today.getDayOfMonth()));
                if (histDate.isEqual(today)) continue;
                bookings.add(createBooking("DSH" + dayOffset + "01", "Trần Văn Bình", "0912111222", "CL01", "CN01", histDate, "07:00 - 08:00", 120000, "Đã thanh toán", "VietQR", "Khách tập sáng"));
                bookings.add(createBooking("DSH" + dayOffset + "02", "Nguyễn Minh Khang", "0934333444", "CL02", "CN01", histDate, "09:00 - 11:00", 240000, "Đã thanh toán", "VietQR", "Đánh đôi giải CLB"));
                bookings.add(createBooking("DSH" + dayOffset + "03", "Phan Hoàng Yến", "0987666777", "CL03", "CN01", histDate, "14:00 - 16:00", 180000, "Đã thanh toán", "Tiền mặt", "Học viên tập luyện"));
                bookings.add(createBooking("DSH" + dayOffset + "04", "Lê Thanh Sơn", "0909888999", "CL04", "CN01", histDate, "17:00 - 19:00", 180000, "Đã thanh toán", "VietQR", "Khách cố định"));
                bookings.add(createBooking("DSH" + dayOffset + "05", "Vũ Đình Trọng", "0976555444", "CL05", "CN01", histDate, "18:00 - 20:00", 180000, "Đã thanh toán", "VietQR", "Giao lưu công ty"));
                bookings.add(createBooking("DSH" + dayOffset + "06", "Hoàng Kim Ngọc", "0965444333", "CL06", "CN01", histDate, "19:00 - 21:00", 180000, "Đã thanh toán", "Tiền mặt", "CLB phong trào"));
                bookings.add(createBooking("DSH" + dayOffset + "07", "Đỗ Quốc Việt", "0911222333", "CL07", "CN01", histDate, "08:00 - 10:00", 180000, "Đã thanh toán", "VietQR", "Tập đôi nam"));
                bookings.add(createBooking("DSH" + dayOffset + "08", "Đặng Minh Tâm", "0933444555", "CL08", "CN01", histDate, "16:00 - 18:00", 180000, "Đã thanh toán", "VietQR", "Tập cơ bản"));
            }

            // ==========================================
            // CHI NHÁNH 1: THỦ ĐỨC (CN01: CL01 -> CL08)
            // ==========================================
            // Sân 01 (VIP Yonex - CL01)
            bookings.add(createBooking("DS0101", "Nguyễn Tuấn Kiệt", "0911223344", "CL01", "CN01", today, "06:00 - 07:00", 120000, "Đã thanh toán", "VietQR", "Khách tập sáng quen"));
            bookings.add(createBooking("DS0102", "Phạm Hoàng Anh", "0988776655", "CL01", "CN01", today, "08:00 - 09:00", 120000, "Đã xác nhận", "VietQR", "Khách VIP Kim Cương"));
            bookings.add(createBooking("DS0103", "Đỗ Thành Đạt", "0933112233", "CL01", "CN01", today, "17:00 - 18:00", 120000, "Đã xác nhận", "VietQR", "Đã cọc 30%"));
            bookings.add(createBooking("DS0104", "Hoàng Minh Trí", "0977889900", "CL01", "CN01", today, "19:00 - 20:00", 120000, "Đã xác nhận", "VietQR", "Đánh đôi giải UTE"));

            // Sân 02 (VIP Yonex - CL02) - Đang sử dụng khớp giao diện!
            bookings.add(createBooking("DS0201", "Lý Thanh Tùng", "0902334455", "CL02", "CN01", today, "07:00 - 08:00", 120000, "Đã thanh toán", "Tiền mặt", "Khách vãng lai sáng"));
            bookings.add(createBooking("DS0202", "Bùi Gia Huy", "0966554433", "CL02", "CN01", today, "08:00 - 10:00", 120000, "Đang sử dụng", "VietQR", "Đang đánh set 2"));
            bookings.add(createBooking("DS0203", "Trần Minh Quân", "0934567890", "CL02", "CN01", today, "18:00 - 19:00", 120000, "Đã xác nhận", "VietQR", "Hội viên Bạc"));
            bookings.add(createBooking("DS0204", "Võ Nhật Nam", "0981234567", "CL02", "CN01", today, "20:00 - 21:00", 120000, "Chờ xác nhận", "Tiền mặt", "Hẹn tới quầy cọc"));

            // Sân 03 (Thảm Enlio - CL03)
            bookings.add(createBooking("DS0301", "Hồ Quốc Bảo", "0938112233", "CL03", "CN01", today, "06:00 - 07:00", 90000, "Đã thanh toán", "Tiền mặt", "Ca sáng sớm"));
            bookings.add(createBooking("DS0302", "Ngô Đình Trọng", "0908776655", "CL03", "CN01", today, "14:00 - 15:00", 90000, "Đã xác nhận", "VietQR", "Giao lưu sinh viên"));
            bookings.add(createBooking("DS0303", "Lê Bá Đạt", "0912345678", "CL03", "CN01", today, "17:00 - 18:00", 90000, "Đã xác nhận", "VietQR", "Khách quen CLB"));
            bookings.add(createBooking("DS0304", "Trương Vĩnh Phát", "0919223344", "CL03", "CN01", today, "19:00 - 20:00", 90000, "Đã xác nhận", "VietQR", "CLB Cầu Lông UTE"));

            // Sân 04 (Thảm Enlio - CL04)
            bookings.add(createBooking("DS0401", "Dương Hoàng Phúc", "0932445566", "CL04", "CN01", today, "08:00 - 09:00", 90000, "Đã thanh toán", "Tiền mặt", "Đã trả sân đúng giờ"));
            bookings.add(createBooking("DS0402", "Phan Đức Thắng", "0976112233", "CL04", "CN01", today, "15:00 - 16:00", 90000, "Đã xác nhận", "VietQR", "Thuê thêm 2 vợt Astrox"));
            bookings.add(createBooking("DS0403", "Vũ Hải Đăng", "0977112233", "CL04", "CN01", today, "18:00 - 19:00", 90000, "Chờ xác nhận", "Tiền mặt", "Chờ thanh toán cọc"));
            bookings.add(createBooking("DS0404", "Lâm Hoài Nam", "0909556677", "CL04", "CN01", today, "20:00 - 21:00", 90000, "Đã xác nhận", "VietQR", "Đã cọc 30%"));

            // Sân 05 (Taraflex - CL05)
            bookings.add(createBooking("DS0501", "Nguyễn Văn Hùng", "0913445566", "CL05", "CN01", today, "07:00 - 08:00", 90000, "Đã thanh toán", "Tiền mặt", "Đã chốt xong"));
            bookings.add(createBooking("DS0502", "Đặng Quốc Hưng", "0944556677", "CL05", "CN01", today, "10:00 - 11:00", 90000, "Đã xác nhận", "VietQR", "Đang tập giao bóng"));
            bookings.add(createBooking("DS0503", "Tạ Minh Khang", "0982334455", "CL05", "CN01", today, "17:00 - 18:00", 90000, "Đã xác nhận", "VietQR", "Khách cố định thứ 2"));
            bookings.add(createBooking("DS0504", "Cao Văn Tiệp", "0967889900", "CL05", "CN01", today, "19:00 - 20:00", 90000, "Đã xác nhận", "VietQR", "Đã cọc 27.000đ"));

            // Sân 06 (Taraflex - CL06)
            bookings.add(createBooking("DS0601", "Trần Quốc Toản", "0935667788", "CL06", "CN01", today, "06:00 - 07:00", 90000, "Đã thanh toán", "Tiền mặt", "Tập thể lực sáng"));
            bookings.add(createBooking("DS0602", "Huỳnh Hải Long", "0907112233", "CL06", "CN01", today, "14:00 - 15:00", 90000, "Đang sử dụng", "VietQR", "Kèm 1 ống cầu Victor"));
            bookings.add(createBooking("DS0603", "Mai Xuân Trường", "0985443322", "CL06", "CN01", today, "18:00 - 19:00", 90000, "Đã xác nhận", "VietQR", "Đánh đơn phong trào"));

            // Sân 07 (Tiêu chuẩn - CL07)
            bookings.add(createBooking("DS0701", "Đinh Tiến Dũng", "0918776655", "CL07", "CN01", today, "08:00 - 09:00", 90000, "Đã thanh toán", "Tiền mặt", "Xong ca sáng"));
            bookings.add(createBooking("DS0702", "Trịnh Công Minh", "0974332211", "CL07", "CN01", today, "16:00 - 17:00", 90000, "Đang sử dụng", "VietQR", "Đang chơi cùng bạn"));
            bookings.add(createBooking("DS0703", "Bạch Thái Bưởi", "0903887766", "CL07", "CN01", today, "19:00 - 20:00", 90000, "Đã xác nhận", "VietQR", "Nhóm công ty FPT"));

            // Sân 08 (Tiêu chuẩn - CL08)
            bookings.add(createBooking("DS0801", "Hà Quang Huy", "0939221100", "CL08", "CN01", today, "09:00 - 10:00", 90000, "Đang sử dụng", "VietQR", "Nhóm sinh viên năm 3"));
            bookings.add(createBooking("DS0802", "Phạm Tuấn Anh", "0986778899", "CL08", "CN01", today, "17:00 - 18:00", 90000, "Đã xác nhận", "VietQR", "Đã cọc 27.000đ"));
            bookings.add(createBooking("DS0803", "Lê Hoàng Quân", "0914556677", "CL08", "CN01", today, "20:00 - 21:00", 90000, "Chờ xác nhận", "Tiền mặt", "Giữ chỗ 10 phút"));

            // ==========================================
            // CHI NHÁNH 2: BÌNH THẠNH (CN02: BT01 -> BT06)
            // ==========================================
            // Sân BT01 (VIP Victor)
            bookings.add(createBooking("DS0901", "Lê Bá Đạt", "0912345678", "BT01", "CN02", today, "18:00 - 20:00", 120000, "Đang sử dụng", "Chuyển khoản VietQR", "Sân VIP Bình Thạnh"));
            bookings.add(createBooking("DS0902", "Nguyễn Minh Châu", "0912223344", "BT01", "CN02", today, "09:00 - 10:00", 120000, "Đã thanh toán", "VietQR", "Khách VIP Bình Thạnh"));
            bookings.add(createBooking("DS0903", "Lê Hoài Bảo", "0978889911", "BT01", "CN02", today, "15:00 - 16:00", 120000, "Đã xác nhận", "VietQR", "Giao lưu công ty"));
            bookings.add(createBooking("DS0904", "Võ Hùng Cường", "0934556677", "BT01", "CN02", today, "19:00 - 20:00", 120000, "Đã xác nhận", "VietQR", "Đã cọc 30%"));

            // Sân BT02
            bookings.add(createBooking("DS1001", "Phan Thanh Sơn", "0908881122", "BT02", "CN02", today, "08:00 - 09:00", 90000, "Đã thanh toán", "Tiền mặt", "CLB Hàng Xanh"));
            bookings.add(createBooking("DS1002", "Đặng Ngọc Long", "0931112233", "BT02", "CN02", today, "17:00 - 18:00", 90000, "Đã xác nhận", "VietQR", "Khách hội viên"));
            bookings.add(createBooking("DS1003", "Hoàng Kim Trọng", "0965554433", "BT02", "CN02", today, "20:00 - 21:00", 90000, "Chờ xác nhận", "Tiền mặt", "Giữ sân tối"));

            // Sân BT03
            bookings.add(createBooking("DS1101", "Nguyễn Văn Đức", "0907778899", "BT03", "CN02", today, "06:00 - 07:00", 90000, "Đã thanh toán", "Tiền mặt", "Tập sáng"));
            bookings.add(createBooking("DS1102", "Bùi Thế Anh", "0918882233", "BT03", "CN02", today, "15:00 - 16:00", 90000, "Đang sử dụng", "VietQR", "Sinh viên Ngoại Thương"));
            bookings.add(createBooking("DS1103", "Vũ Mạnh Hùng", "0943334455", "BT03", "CN02", today, "19:00 - 20:00", 90000, "Đã xác nhận", "VietQR", "Đã cọc 27.000đ"));

            // Sân BT04
            bookings.add(createBooking("DS1201", "Phạm Gia Huy", "0988777666", "BT04", "CN02", today, "19:00 - 21:00", 90000, "Đang sử dụng", "Tiền mặt", "Tập luyện đôi nam"));
            bookings.add(createBooking("DS1202", "Trần Bá Hoàng", "0987776655", "BT04", "CN02", today, "16:00 - 17:00", 90000, "Đã thanh toán", "VietQR", "Thuê thêm 1 vợt"));
            bookings.add(createBooking("DS1203", "Mai Thành Đạt", "0915556677", "BT04", "CN02", today, "18:00 - 19:00", 90000, "Đã xác nhận", "VietQR", "Đã cọc"));

            // Sân BT05
            bookings.add(createBooking("DS1301", "Tạ Quang Bửu", "0909998877", "BT05", "CN02", today, "07:00 - 08:00", 90000, "Đã thanh toán", "Tiền mặt", "Khách quen"));
            bookings.add(createBooking("DS1302", "Ngô Khắc Hưng", "0921113344", "BT05", "CN02", today, "17:00 - 18:00", 90000, "Đã xác nhận", "VietQR", "Nhóm bạn trẻ"));
            bookings.add(createBooking("DS1303", "Trịnh Đình Quang", "0972223344", "BT05", "CN02", today, "20:00 - 21:00", 90000, "Chờ xác nhận", "Tiền mặt", "Đợi đến check-in"));

            // Sân BT06 (VIP)
            bookings.add(createBooking("DS1401", "Dương Minh Tiến", "0983332211", "BT06", "CN02", today, "08:00 - 09:00", 120000, "Đã thanh toán", "VietQR", "Nhóm văn phòng"));
            bookings.add(createBooking("DS1402", "Phạm Văn Sơn", "0938887766", "BT06", "CN02", today, "18:00 - 19:00", 120000, "Đã xác nhận", "VietQR", "Đã chuyển khoản"));
            bookings.add(createBooking("DS1403", "Lê Gia Khiêm", "0904445566", "BT06", "CN02", today, "19:00 - 20:00", 120000, "Đã xác nhận", "VietQR", "Đã cọc 30%"));

            // ==========================================
            // ==========================================
            // CHI NHÁNH 3: QUẬN 9 (CN03: Q901 -> Q906)
            // ==========================================
            // Sân Q901 (VIP Lining)
            bookings.add(createBooking("DS1501", "Nguyễn Hữu Tài", "0917778899", "Q901", "CN03", today, "07:00 - 08:00", 120000, "Đã thanh toán", "VietQR", "Khách VIP khu CNC"));
            bookings.add(createBooking("DS1502", "Lê Văn Thắng", "0982221100", "Q901", "CN03", today, "09:00 - 10:00", 120000, "Đã xác nhận", "VietQR", "Khu công nghệ cao"));
            bookings.add(createBooking("DS1503", "Trần Quốc Huy", "0935556644", "Q901", "CN03", today, "17:00 - 18:00", 120000, "Đã xác nhận", "VietQR", "Đã cọc 30%"));

            // Sân Q902
            bookings.add(createBooking("DS1601", "Vũ Thị Mai Anh", "0977555444", "Q902", "CN03", today, "17:00 - 19:00", 90000, "Đang sử dụng", "VietQR Pro", "Nhóm bạn sinh viên"));
            bookings.add(createBooking("DS1602", "Bạch Văn Quân", "0913334466", "Q902", "CN03", today, "09:00 - 10:00", 90000, "Đã thanh toán", "VietQR", "Hội viên Bạc"));
            bookings.add(createBooking("DS1603", "Phan Hoàng Phúc", "0984445577", "Q902", "CN03", today, "20:00 - 21:00", 90000, "Chờ xác nhận", "Tiền mặt", "Giữ chỗ"));

            // Sân Q903
            bookings.add(createBooking("DS1701", "Huỳnh Văn Nam", "0905556688", "Q903", "CN03", today, "06:00 - 07:00", 90000, "Đã thanh toán", "Tiền mặt", "Tập thể thao"));
            bookings.add(createBooking("DS1702", "Lý Minh Đức", "0937778811", "Q903", "CN03", today, "14:00 - 15:00", 90000, "Đang sử dụng", "VietQR", "Sinh viên FPT"));
            bookings.add(createBooking("DS1703", "Đặng Văn Lâm", "0968889922", "Q903", "CN03", today, "19:00 - 20:00", 90000, "Đã xác nhận", "VietQR", "Đã cọc"));

            // Sân Q904
            bookings.add(createBooking("DS1801", "Tô Văn Hùng", "0919990033", "Q904", "CN03", today, "09:00 - 10:00", 90000, "Đang sử dụng", "VietQR", "Đánh phong trào"));
            bookings.add(createBooking("DS1802", "Mai Văn Toàn", "0975554422", "Q904", "CN03", today, "16:00 - 17:00", 90000, "Đã xác nhận", "VietQR", "Kèm 2 chai nước"));
            bookings.add(createBooking("DS1803", "Bùi Văn Phát", "0908889933", "Q904", "CN03", today, "18:00 - 19:00", 90000, "Đã xác nhận", "VietQR", "Đã cọc 27.000đ"));

            // Sân Q905
            bookings.add(createBooking("DS1901", "Trịnh Công Tuấn", "0932221155", "Q905", "CN03", today, "07:00 - 08:00", 90000, "Đã thanh toán", "Tiền mặt", "Khách quen"));
            bookings.add(createBooking("DS1902", "Hồ Minh Nhật", "0981119944", "Q905", "CN03", today, "17:00 - 18:00", 90000, "Đã xác nhận", "VietQR", "Đánh đôi"));
            bookings.add(createBooking("DS1903", "Võ Văn Kiên", "0914443322", "Q905", "CN03", today, "20:00 - 21:00", 90000, "Chờ xác nhận", "Tiền mặt", "Hẹn 15 phút tới"));

            // Sân Q906
            bookings.add(createBooking("DS2001", "Cao Văn Dũng", "0963332211", "Q906", "CN03", today, "08:00 - 09:00", 90000, "Đang sử dụng", "VietQR", "Nhóm sinh viên"));
            bookings.add(createBooking("DS2002", "Đoàn Văn Hậu", "0901114477", "Q906", "CN03", today, "18:00 - 19:00", 90000, "Đã xác nhận", "VietQR", "Đã cọc"));
            bookings.add(createBooking("DS2003", "Phạm Văn Quyến", "0976665533", "Q906", "CN03", today, "19:00 - 20:00", 90000, "Đã xác nhận", "VietQR", "Khách đặt app"));

            // ==========================================
            // CHI NHÁNH 4: GÒ VẤP (CN04: GV01 -> GV06)
            // ==========================================
            // Sân GV01 (VIP Yonex)
            bookings.add(createBooking("DS2101", "Trần Thanh Hằng", "0933444555", "GV01", "CN04", today, "18:00 - 20:00", 120000, "Đang sử dụng", "Tiền mặt", "Khách vãng lai chi nhánh GV"));
            bookings.add(createBooking("DS2102", "Lê Văn Lương", "0987652233", "GV01", "CN04", today, "09:00 - 10:00", 120000, "Đã thanh toán", "VietQR", "Khách VIP Quang Trung"));
            bookings.add(createBooking("DS2103", "Trần Đình Trọng", "0934563344", "GV01", "CN04", today, "15:00 - 16:00", 120000, "Đã xác nhận", "VietQR", "Đã cọc 30%"));

            // Sân GV02
            bookings.add(createBooking("DS2201", "Đỗ Hùng Dũng", "0971235566", "GV02", "CN04", today, "08:00 - 09:00", 90000, "Đang sử dụng", "Tiền mặt", "Ca sáng"));
            bookings.add(createBooking("DS2202", "Bùi Tiến Dũng", "0919876677", "GV02", "CN04", today, "17:00 - 18:00", 90000, "Đã xác nhận", "VietQR", "Hội viên"));
            bookings.add(createBooking("DS2203", "Phan Văn Đức", "0983457788", "GV02", "CN04", today, "20:00 - 21:00", 90000, "Chờ xác nhận", "Tiền mặt", "Giữ chỗ"));

            // Sân GV03
            bookings.add(createBooking("DS2301", "Hà Đức Chinh", "0902348899", "GV03", "CN04", today, "06:00 - 07:00", 90000, "Đã thanh toán", "Tiền mặt", "Tập thể lực"));
            bookings.add(createBooking("DS2302", "Lương Xuân Trường", "0936789900", "GV03", "CN04", today, "15:00 - 16:00", 90000, "Đang sử dụng", "VietQR", "Giao lưu"));
            bookings.add(createBooking("DS2303", "Nguyễn Tuấn Anh", "0967890011", "GV03", "CN04", today, "19:00 - 20:00", 90000, "Đã xác nhận", "VietQR", "Đã cọc 27.000đ"));

            // Sân GV04
            bookings.add(createBooking("DS2401", "Nguyễn Phong Hồng Duy", "0914561122", "GV04", "CN04", today, "09:00 - 10:00", 90000, "Đang sử dụng", "VietQR", "Ca sáng"));
            bookings.add(createBooking("DS2402", "Trần Minh Vương", "0978902233", "GV04", "CN04", today, "16:00 - 17:00", 90000, "Đang sử dụng", "VietQR", "Thuê vợt Yonex"));
            bookings.add(createBooking("DS2403", "Nguyễn Công Phượng", "0905673344", "GV04", "CN04", today, "18:00 - 19:00", 90000, "Đã xác nhận", "VietQR", "Đã cọc 30%"));

            // Sân GV05
            bookings.add(createBooking("DS2501", "Nguyễn Quang Hải", "0931234455", "GV05", "CN04", today, "07:00 - 08:00", 90000, "Đã thanh toán", "Tiền mặt", "Khách quen"));
            bookings.add(createBooking("DS2502", "Đoàn Văn Hậu", "0989015566", "GV05", "CN04", today, "17:00 - 18:00", 90000, "Đã xác nhận", "VietQR", "CLB Cầu Lông"));
            bookings.add(createBooking("DS2503", "Quế Ngọc Hải", "0912346677", "GV05", "CN04", today, "20:00 - 21:00", 90000, "Chờ xác nhận", "Tiền mặt", "Hẹn đến check-in"));

            // Sân GV06
            bookings.add(createBooking("DS2601", "Đặng Văn Lâm", "0965437788", "GV06", "CN04", today, "08:00 - 09:00", 90000, "Đang sử dụng", "VietQR", "Đang đánh set 3"));
            bookings.add(createBooking("DS2602", "Nguyễn Tiến Linh", "0908768899", "GV06", "CN04", today, "18:00 - 19:00", 90000, "Đã xác nhận", "VietQR", "Đã cọc"));
            bookings.add(createBooking("DS2603", "Phạm Tuấn Hải", "0976549900", "GV06", "CN04", today, "19:00 - 20:00", 90000, "Đã xác nhận", "VietQR", "Khách app"));

            bookingRepository.saveAll(bookings);
            log.info("Đã tạo {} đơn đặt sân mẫu phủ kín toàn bộ 26 sân của 4 chi nhánh ngày hôm nay ({}) thành công!", bookings.size(), today);
        }
    }

    private Booking createBooking(String code, String cust, String phone, String court, String branch, LocalDate date, String slot, int hourlyRate, String status, String method, String notes) {
        Booking b = new Booking();
        b.setBookingCode(code);
        b.setCustomerName(cust);
        b.setCustomerPhone(phone);
        b.setCourtCode(court);
        b.setBranchCode(branch);
        b.setBookingDate(date);
        b.setTimeSlot(slot);
        b.setHourlyPrice(new BigDecimal(hourlyRate));
        b.setTotalPrice(new BigDecimal(hourlyRate));
        b.setDepositAmount(new BigDecimal(Math.round(hourlyRate * 0.3)));
        b.setPaymentMethod(method);
        b.setStatus(status);
        b.setNotes(notes);
        if ("Đang sử dụng".equals(status)) {
            b.setCheckInTime(LocalDateTime.now().minusMinutes(30));
        } else if ("Đã thanh toán".equals(status) || "Hoàn thành".equals(status)) {
            b.setCheckInTime(LocalDateTime.now().minusHours(2));
            b.setCheckOutTime(LocalDateTime.now().minusHours(1));
        }
        return b;
    }
}
