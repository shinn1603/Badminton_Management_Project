package vn.yain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.yain.entity.Invoice;
import vn.yain.entity.Payment;
import vn.yain.entity.Staff;
import vn.yain.entity.WorkShift;
import vn.yain.repository.InvoiceRepository;
import vn.yain.repository.PaymentRepository;
import vn.yain.repository.StaffRepository;
import vn.yain.repository.WorkShiftRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service quan ly ca truc, nhan su chi nhanh & Ban giao ca thu ngan (Bao phu Use Case XL-08)
 */
@Service
@Transactional
public class ShiftService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private WorkShiftRepository workShiftRepository;

    private static final String[] DAYS_ORDER = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};

    // ==========================================
    // 1. STAFF CRUD & PERSISTENCE
    // ==========================================

    public List<Staff> getAllStaff(String branchCode) {
        ensureDefaultStaffSeeded();
        if (branchCode != null && !branchCode.trim().isEmpty() && !"ALL".equalsIgnoreCase(branchCode.trim())) {
            List<Staff> list = staffRepository.findByBranchCode(branchCode.trim());
            if (!list.isEmpty()) {
                return list;
            }
        }
        return staffRepository.findAll();
    }

    public Staff saveStaff(Staff staff) {
        if (staff.getStaffCode() == null || staff.getStaffCode().trim().isEmpty()) {
            long count = staffRepository.count() + 1;
            staff.setStaffCode("NV-0" + count);
        }
        if (staff.getBranchCode() == null || staff.getBranchCode().trim().isEmpty()) {
            staff.setBranchCode("CN01");
        }
        if (staff.getStatus() == null || staff.getStatus().trim().isEmpty()) {
            staff.setStatus("Đang làm việc");
        }

        Optional<Staff> existing = staffRepository.findByStaffCode(staff.getStaffCode().trim());
        if (existing.isPresent()) {
            Staff target = existing.get();
            target.setFullName(staff.getFullName());
            target.setPosition(staff.getPosition());
            target.setPhone(staff.getPhone());
            target.setEmail(staff.getEmail());
            target.setAddress(staff.getAddress());
            target.setStatus(staff.getStatus());
            target.setBranchCode(staff.getBranchCode());
            return staffRepository.save(target);
        }

        return staffRepository.save(staff);
    }

    public void deleteStaff(String staffCode) {
        if (staffCode == null || staffCode.trim().isEmpty()) return;
        String code = staffCode.trim();
        workShiftRepository.deleteByStaffCode(code);
        staffRepository.deleteByStaffCode(code);
    }

    private static volatile boolean staffSeeded = false;

    private void ensureDefaultStaffSeeded() {
        if (staffSeeded) return;
        List<Staff> defaults = List.of(
            Staff.builder().staffCode("NV-01").branchCode("CN01").fullName("Trần Phúc Bảo").position("Quản lý chi nhánh").phone("0903123456").email("manager@utesport.vn").status("Đang làm việc").build(),
            Staff.builder().staffCode("NV-02").branchCode("CN01").fullName("Nguyễn Thanh Tâm").position("Thu ngân POS quầy").phone("0904555666").email("pos@utesport.vn").status("Đang làm việc").build(),
            Staff.builder().staffCode("NV-03").branchCode("CN01").fullName("Bùi Khánh Linh").position("Lễ tân & Dịch vụ").phone("0909111222").email("linh.bk@utesport.vn").status("Đang làm việc").build(),
            Staff.builder().staffCode("NV-04").branchCode("CN01").fullName("Nguyễn Hoàng Nam").position("Thu ngân POS quầy").phone("0903888777").email("nam.nh@utesport.vn").status("Đang làm việc").build(),
            Staff.builder().staffCode("NV-05").branchCode("CN01").fullName("Phạm Gia Huy").position("Kỹ thuật & Bảo dưỡng sân").phone("0988777666").email("huy.pg@utesport.vn").status("Đang làm việc").build(),
            Staff.builder().staffCode("NV-06").branchCode("CN01").fullName("Trịnh Gia Vinh").position("Kỹ thuật & Bảo dưỡng sân").phone("0977222333").email("vinh.tg@utesport.vn").status("Đang làm việc").build()
        );
        for (Staff s : defaults) {
            if (!staffRepository.existsByStaffCode(s.getStaffCode())) {
                staffRepository.save(s);
            }
        }
        staffSeeded = true;
    }

    // ==========================================
    // 2. WEEKLY WORK SHIFTS PERSISTENCE
    // ==========================================

    public Map<String, Object> getWeeklySchedule(String branchCode) {
        ensureDefaultStaffSeeded();
        ensureDefaultShiftsSeeded(branchCode);

        String branch = (branchCode != null && !branchCode.trim().isEmpty()) ? branchCode.trim() : "CN01";
        List<WorkShift> shifts = workShiftRepository.findByBranchCode(branch);
        if (shifts.isEmpty()) {
            shifts = workShiftRepository.findAll();
        }

        Map<String, Map<String, List<Map<String, Object>>>> schedule = new LinkedHashMap<>();
        for (String day : DAYS_ORDER) {
            Map<String, List<Map<String, Object>>> dayMap = new LinkedHashMap<>();
            dayMap.put("morning", new ArrayList<>());
            dayMap.put("night", new ArrayList<>());
            schedule.put(day, dayMap);
        }

        for (WorkShift ws : shifts) {
            String day = ws.getDayOfWeek() != null ? ws.getDayOfWeek().toLowerCase() : "mon";
            String type = ws.getShiftType() != null ? ws.getShiftType().toLowerCase() : "morning";
            if (!schedule.containsKey(day)) {
                schedule.put(day, new LinkedHashMap<>());
            }
            if (!schedule.get(day).containsKey(type)) {
                schedule.get(day).put(type, new ArrayList<>());
            }

            Map<String, Object> item = new HashMap<>();
            item.put("staffId", ws.getStaffCode());
            item.put("duty", ws.getDuty() != null ? ws.getDuty() : "Trực");
            item.put("icon", ws.getIcon() != null ? ws.getIcon() : "POS");
            schedule.get(day).get(type).add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("branchCode", branch);
        response.put("schedule", schedule);
        return response;
    }

    public void assignShift(String day, String shift, String staffCode, String duty, String icon, String branchCode) {
        if (day == null || shift == null || staffCode == null) return;
        String d = day.trim().toLowerCase();
        String s = shift.trim().toLowerCase();
        String st = staffCode.trim();
        String br = (branchCode != null && !branchCode.trim().isEmpty()) ? branchCode.trim() : "CN01";
        String dt = (duty != null && !duty.trim().isEmpty()) ? duty.trim() : "Trực";
        String ic = (icon != null && !icon.trim().isEmpty()) ? icon.trim() : "POS";

        // Ensure staff exists in DB to prevent foreign key violation
        if (!staffRepository.existsByStaffCode(st)) {
            staffRepository.save(Staff.builder()
                    .staffCode(st)
                    .branchCode(br)
                    .fullName("Nhân viên " + st)
                    .position("Nhân viên chi nhánh")
                    .phone("0900000000")
                    .status("Đang làm việc")
                    .build());
        }

        // Remove existing identical assignment to prevent duplicate
        workShiftRepository.deleteByDayOfWeekAndShiftTypeAndStaffCode(d, s, st);

        String shiftCode = "WS-" + d.toUpperCase() + "-" + s.substring(0, Math.min(3, s.length())).toUpperCase() + "-" + st + "-" + (System.currentTimeMillis() % 100000);
        String startTime = "night".equalsIgnoreCase(s) ? "13:30" : "05:00";
        String endTime = "night".equalsIgnoreCase(s) ? "22:30" : "13:30";
        WorkShift ws = WorkShift.builder()
                .shiftCode(shiftCode)
                .staffCode(st)
                .branchCode(br)
                .dayOfWeek(d)
                .shiftType(s)
                .duty(dt)
                .icon(ic)
                .shiftDate(LocalDate.now())
                .startTime(startTime)
                .endTime(endTime)
                .status("Đã phân ca")
                .build();
        workShiftRepository.save(ws);
    }

    public void removeShift(String day, String shift, String staffCode) {
        if (day == null || shift == null || staffCode == null) return;
        workShiftRepository.deleteByDayOfWeekAndShiftTypeAndStaffCode(day.trim().toLowerCase(), shift.trim().toLowerCase(), staffCode.trim());
    }

    @SuppressWarnings("unchecked")
    public void saveWeeklySchedule(Map<String, Object> scheduleMap, String branchCode) {
        String br = (branchCode != null && !branchCode.trim().isEmpty()) ? branchCode.trim() : "CN01";
        workShiftRepository.deleteByBranchCode(br);

        List<WorkShift> newShifts = new ArrayList<>();
        int seq = 1;

        for (String day : DAYS_ORDER) {
            Object dayObj = scheduleMap.get(day);
            if (dayObj instanceof Map) {
                Map<String, Object> dayShifts = (Map<String, Object>) dayObj;
                for (String shiftType : List.of("morning", "night")) {
                    Object shiftListObj = dayShifts.get(shiftType);
                    if (shiftListObj instanceof List) {
                        List<?> list = (List<?>) shiftListObj;
                        for (Object itemObj : list) {
                            if (itemObj instanceof Map) {
                                Map<String, Object> item = (Map<String, Object>) itemObj;
                                String staffId = (String) item.get("staffId");
                                String duty = (String) item.getOrDefault("duty", "Trực");
                                String icon = (String) item.getOrDefault("icon", "POS");
                                if (staffId != null && !staffId.trim().isEmpty()) {
                                    String shiftCode = "WS-" + br + "-" + day.toUpperCase() + "-" + (seq++);
                                    String itemStartTime = "night".equalsIgnoreCase(shiftType) ? "13:30" : "05:00";
                                    String itemEndTime = "night".equalsIgnoreCase(shiftType) ? "22:30" : "13:30";
                                    newShifts.add(WorkShift.builder()
                                            .shiftCode(shiftCode)
                                            .staffCode(staffId.trim())
                                            .branchCode(br)
                                            .dayOfWeek(day)
                                            .shiftType(shiftType)
                                            .duty(duty)
                                            .icon(icon)
                                            .shiftDate(LocalDate.now())
                                            .startTime(itemStartTime)
                                            .endTime(itemEndTime)
                                            .status("Đã phân ca")
                                            .build());
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!newShifts.isEmpty()) {
            workShiftRepository.saveAll(newShifts);
        }
    }

    public void resetWeeklySchedule(String branchCode) {
        String br = (branchCode != null && !branchCode.trim().isEmpty()) ? branchCode.trim() : "CN01";
        workShiftRepository.deleteByBranchCode(br);
        seedDefaultShifts(br);
    }

    private void ensureDefaultShiftsSeeded(String branchCode) {
        String br = (branchCode != null && !branchCode.trim().isEmpty()) ? branchCode.trim() : "CN01";
        if (workShiftRepository.findByBranchCode(br).isEmpty()) {
            seedDefaultShifts(br);
        }
    }

    private void seedDefaultShifts(String branchCode) {
        List<WorkShift> list = new ArrayList<>();
        int seq = 100;

        // Monday
        list.add(createShift(branchCode, "mon", "morning", "NV-02", "POS", "POS", seq++));
        list.add(createShift(branchCode, "mon", "morning", "NV-05", "Kỹ thuật", "TECH", seq++));
        list.add(createShift(branchCode, "mon", "night", "NV-03", "Lễ tân", "REC", seq++));
        list.add(createShift(branchCode, "mon", "night", "NV-01", "Quản lý", "MGR", seq++));
        list.add(createShift(branchCode, "mon", "night", "NV-06", "Kỹ thuật", "TECH", seq++));

        // Tuesday
        list.add(createShift(branchCode, "tue", "morning", "NV-02", "POS", "POS", seq++));
        list.add(createShift(branchCode, "tue", "morning", "NV-04", "Thu ngân", "CASH", seq++));
        list.add(createShift(branchCode, "tue", "night", "NV-03", "Lễ tân", "REC", seq++));
        list.add(createShift(branchCode, "tue", "night", "NV-05", "Kỹ thuật", "TECH", seq++));

        // Wednesday
        list.add(createShift(branchCode, "wed", "morning", "NV-02", "POS", "POS", seq++));
        list.add(createShift(branchCode, "wed", "morning", "NV-05", "Kỹ thuật", "TECH", seq++));
        list.add(createShift(branchCode, "wed", "night", "NV-03", "Lễ tân", "REC", seq++));
        list.add(createShift(branchCode, "wed", "night", "NV-01", "Quản lý", "MGR", seq++));
        list.add(createShift(branchCode, "wed", "night", "NV-06", "Kỹ thuật", "TECH", seq++));

        // Thursday
        list.add(createShift(branchCode, "thu", "morning", "NV-04", "Thu ngân", "CASH", seq++));
        list.add(createShift(branchCode, "thu", "morning", "NV-06", "Kỹ thuật", "TECH", seq++));
        list.add(createShift(branchCode, "thu", "night", "NV-02", "POS", "POS", seq++));
        list.add(createShift(branchCode, "thu", "night", "NV-05", "Kỹ thuật", "TECH", seq++));

        // Friday
        list.add(createShift(branchCode, "fri", "morning", "NV-02", "POS", "POS", seq++));
        list.add(createShift(branchCode, "fri", "morning", "NV-04", "Thu ngân", "CASH", seq++));
        list.add(createShift(branchCode, "fri", "night", "NV-03", "Lễ tân", "REC", seq++));
        list.add(createShift(branchCode, "fri", "night", "NV-01", "Quản lý", "MGR", seq++));
        list.add(createShift(branchCode, "fri", "night", "NV-06", "Kỹ thuật", "TECH", seq++));

        // Saturday
        list.add(createShift(branchCode, "sat", "morning", "NV-02", "POS", "POS", seq++));
        list.add(createShift(branchCode, "sat", "morning", "NV-05", "Kỹ thuật", "TECH", seq++));
        list.add(createShift(branchCode, "sat", "morning", "NV-03", "Lễ tân", "REC", seq++));
        list.add(createShift(branchCode, "sat", "night", "NV-04", "Thu ngân", "CASH", seq++));
        list.add(createShift(branchCode, "sat", "night", "NV-01", "Quản lý", "MGR", seq++));
        list.add(createShift(branchCode, "sat", "night", "NV-06", "Kỹ thuật", "TECH", seq++));

        // Sunday
        list.add(createShift(branchCode, "sun", "morning", "NV-04", "Thu ngân", "CASH", seq++));
        list.add(createShift(branchCode, "sun", "morning", "NV-06", "Kỹ thuật", "TECH", seq++));
        list.add(createShift(branchCode, "sun", "morning", "NV-03", "Lễ tân", "REC", seq++));
        list.add(createShift(branchCode, "sun", "night", "NV-02", "POS", "POS", seq++));
        list.add(createShift(branchCode, "sun", "night", "NV-01", "Quản lý", "MGR", seq++));
        list.add(createShift(branchCode, "sun", "night", "NV-05", "Kỹ thuật", "TECH", seq++));

        workShiftRepository.saveAll(list);
    }

    private WorkShift createShift(String branchCode, String day, String type, String staffCode, String duty, String icon, int seq) {
        String startTime = "night".equalsIgnoreCase(type) ? "13:30" : "05:00";
        String endTime = "night".equalsIgnoreCase(type) ? "22:30" : "13:30";
        return WorkShift.builder()
                .shiftCode("WS-" + branchCode + "-" + day.toUpperCase() + "-" + seq)
                .staffCode(staffCode)
                .branchCode(branchCode)
                .dayOfWeek(day)
                .shiftType(type)
                .duty(duty)
                .icon(icon)
                .shiftDate(LocalDate.now())
                .startTime(startTime)
                .endTime(endTime)
                .status("Đã phân ca")
                .build();
    }

    // ==========================================
    // 3. CASHIER SHIFT SUMMARY & CLOSE (XL-08)
    // ==========================================

    public Map<String, Object> getShiftSummary(String staffCode, String branchCode) {
        String staff = (staffCode != null && !staffCode.trim().isEmpty()) ? staffCode.trim() : "NVQ01";

        List<Invoice> invoices = invoiceRepository.findAllByOrderByInvoiceDateDesc();
        LocalDate today = LocalDate.now();

        BigDecimal cashTotal = BigDecimal.ZERO;
        BigDecimal transferTotal = BigDecimal.ZERO;
        BigDecimal courtFeeTotal = BigDecimal.ZERO;
        BigDecimal productFeeTotal = BigDecimal.ZERO;
        int invoiceCount = 0;

        for (Invoice inv : invoices) {
            if (inv.getInvoiceDate() != null && inv.getInvoiceDate().toLocalDate().isEqual(today)) {
                invoiceCount++;
                BigDecimal payment = inv.getTotalPayment() != null ? inv.getTotalPayment() : BigDecimal.ZERO;
                BigDecimal cFee = inv.getCourtFee() != null ? inv.getCourtFee() : BigDecimal.ZERO;
                BigDecimal pFee = inv.getProductFee() != null ? inv.getProductFee() : BigDecimal.ZERO;
                BigDecimal eqFee = inv.getEquipmentFee() != null ? inv.getEquipmentFee() : BigDecimal.ZERO;

                courtFeeTotal = courtFeeTotal.add(cFee);
                productFeeTotal = productFeeTotal.add(pFee).add(eqFee);

                if ("Tiền mặt".equalsIgnoreCase(inv.getPaymentMethod())) {
                    cashTotal = cashTotal.add(payment);
                } else {
                    transferTotal = transferTotal.add(payment);
                }
            }
        }

        List<Payment> payments = paymentRepository.findAll();
        for (Payment p : payments) {
            if (p.getPaymentTime() != null && p.getPaymentTime().toLocalDate().isEqual(today)) {
                if ("Thành công".equalsIgnoreCase(p.getStatus())) {
                    if ("Tiền mặt".equalsIgnoreCase(p.getMethod())) {
                        cashTotal = cashTotal.add(p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
                    } else {
                        transferTotal = transferTotal.add(p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
                    }
                }
            }
        }

        BigDecimal totalRevenue = cashTotal.add(transferTotal);

        Map<String, Object> summary = new HashMap<>();
        summary.put("staffCode", staff);
        summary.put("branchCode", branchCode != null ? branchCode : "CN01");
        summary.put("date", today.toString());
        summary.put("invoiceCount", invoiceCount);
        summary.put("systemCashTotal", cashTotal);
        summary.put("systemTransferTotal", transferTotal);
        summary.put("cashRevenue", cashTotal);
        summary.put("transferRevenue", transferTotal);
        summary.put("courtFee", courtFeeTotal);
        summary.put("productFee", productFeeTotal);
        summary.put("totalRevenue", totalRevenue);
        return summary;
    }

    public Map<String, Object> closeShift(String staffCode, String branchCode, BigDecimal actualCash, String notes) {
        if (actualCash == null || actualCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số tiền mặt kiểm kê thực tế không được âm!");
        }

        Map<String, Object> summary = getShiftSummary(staffCode, branchCode);
        BigDecimal systemCash = (BigDecimal) summary.get("systemCashTotal");
        BigDecimal difference = actualCash.subtract(systemCash);

        String handoverCode = "BG" + (100000 + (int)(Math.random() * 900000));
        LocalDateTime closedAt = LocalDateTime.now();

        String effectiveStaff = (staffCode != null && !staffCode.isBlank()) ? staffCode.trim() : "NVQ01";
        if (!staffRepository.existsByStaffCode(effectiveStaff)) {
            staffRepository.save(Staff.builder()
                    .staffCode(effectiveStaff)
                    .branchCode(branchCode != null ? branchCode : "CN01")
                    .fullName("Nhân viên " + effectiveStaff)
                    .position("Thu ngân POS quầy")
                    .phone("0903000000")
                    .email(effectiveStaff.toLowerCase() + "@utesport.vn")
                    .status("Đang làm việc")
                    .build());
        }

        // Persist shift handover record to database
        WorkShift closedShift = WorkShift.builder()
                .shiftCode(handoverCode)
                .staffCode(effectiveStaff)
                .branchCode(branchCode != null ? branchCode : "CN01")
                .shiftDate(closedAt.toLocalDate())
                .dayOfWeek(closedAt.getDayOfWeek().toString().substring(0, 3).toLowerCase())
                .icon("POS")
                .startTime("06:00")
                .endTime(String.format("%02d:%02d", closedAt.getHour(), closedAt.getMinute()))
                .shiftType("handover")
                .duty("POS")
                .status("Đã kết ca")
                .build();
        workShiftRepository.save(closedShift);

        Map<String, Object> receipt = new HashMap<>();
        receipt.put("success", true);
        receipt.put("handoverCode", handoverCode);
        receipt.put("staffCode", staffCode != null ? staffCode : "NVQ01");
        receipt.put("branchCode", branchCode != null ? branchCode : "CN01");
        receipt.put("closedAt", closedAt.toString());
        receipt.put("systemCash", systemCash);
        receipt.put("actualCash", actualCash);
        receipt.put("difference", difference);
        receipt.put("status", "CLOSED");
        receipt.put("notes", notes != null ? notes : "");
        receipt.put("message", difference.compareTo(BigDecimal.ZERO) == 0
                ? "Khớp quỹ tiền mặt 100%!"
                : (difference.compareTo(BigDecimal.ZERO) > 0
                    ? "Thừa quỹ: +" + difference + "đ"
                    : "Thiếu quỹ: " + difference + "đ"));

        return receipt;
    }
}
