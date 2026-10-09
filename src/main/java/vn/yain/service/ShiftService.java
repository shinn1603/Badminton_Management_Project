package vn.yain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.yain.entity.Invoice;
import vn.yain.entity.Payment;
import vn.yain.repository.InvoiceRepository;
import vn.yain.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service quan ly ca truc & Ban giao ca thu ngan (Bao phu Use Case XL-08)
 */
@Service
@Transactional
public class ShiftService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * XL-08: Thong ke doanh thu theo ca thu ngan hien tai
     */
    public Map<String, Object> getShiftSummary(String staffCode, String branchCode) {
        String staff = (staffCode != null && !staffCode.trim().isEmpty()) ? staffCode.trim() : "NVQ01";

        List<Invoice> invoices = invoiceRepository.findAllByOrderByInvoiceDateDesc();
        LocalDate today = LocalDate.now();

        BigDecimal cashTotal = BigDecimal.ZERO;
        BigDecimal transferTotal = BigDecimal.ZERO;
        int invoiceCount = 0;

        for (Invoice inv : invoices) {
            if (inv.getInvoiceDate() != null && inv.getInvoiceDate().toLocalDate().isEqual(today)) {
                if (staff.equalsIgnoreCase(inv.getStaffCode()) || "ADMIN".equalsIgnoreCase(staff)) {
                    invoiceCount++;
                    BigDecimal payment = inv.getTotalPayment() != null ? inv.getTotalPayment() : BigDecimal.ZERO;
                    if ("Tiền mặt".equalsIgnoreCase(inv.getPaymentMethod())) {
                        cashTotal = cashTotal.add(payment);
                    } else {
                        transferTotal = transferTotal.add(payment);
                    }
                }
            }
        }

        // Tinh them tien coc truc tuyen qua Payment
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
        summary.put("totalRevenue", totalRevenue);
        return summary;
    }

    /**
     * XL-08: Dong ca truc, doi soat tien thuc te & in bien ban ban giao
     */
    public Map<String, Object> closeShift(String staffCode, String branchCode, BigDecimal actualCash, String notes) {
        if (actualCash == null || actualCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số tiền mặt kiểm kê thực tế không được âm!");
        }

        Map<String, Object> summary = getShiftSummary(staffCode, branchCode);
        BigDecimal systemCash = (BigDecimal) summary.get("systemCashTotal");
        BigDecimal difference = actualCash.subtract(systemCash);

        String handoverCode = "BG" + (100000 + (int)(Math.random() * 900000));
        LocalDateTime closedAt = LocalDateTime.now();

        Map<String, Object> receipt = new HashMap<>();
        receipt.put("success", true);
        receipt.put("handoverCode", handoverCode);
        receipt.put("staffCode", staffCode != null ? staffCode : "NVQ01");
        receipt.put("branchCode", branchCode != null ? branchCode : "CN01");
        receipt.put("closedAt", closedAt.toString());
        receipt.put("systemCash", systemCash);
        receipt.put("actualCash", actualCash);
        receipt.put("difference", difference); // > 0: Thua quy, < 0: Thieu quy, = 0: Khop
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
