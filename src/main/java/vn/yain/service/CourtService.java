package vn.yain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.yain.dto.CourtEventDto;
import vn.yain.entity.Court;
import vn.yain.repository.CourtRepository;
import vn.yain.repository.BookingRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class CourtService {

    private static final Logger log = LoggerFactory.getLogger(CourtService.class);
    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private CourtRepository courtRepository;

    @Autowired(required = false)
    private BookingRepository bookingRepository;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    // Track held courts and their expiration times (600 seconds = 10 minutes)
    private final Map<String, LocalDateTime> heldCourtTimestamps = new ConcurrentHashMap<>();

    public int[] parseSlotHours(String slot) {
        if (slot == null || slot.isBlank()) return new int[]{0, 0};
        Pattern p = Pattern.compile("(\\d{1,2}):\\d{2}\\s*-\\s*(\\d{1,2}):\\d{2}");
        Matcher m = p.matcher(slot);
        int minStart = 24;
        int maxEnd = 0;
        boolean found = false;
        while (m.find()) {
            found = true;
            int start = Integer.parseInt(m.group(1));
            int end = Integer.parseInt(m.group(2));
            if (end > start) {
                minStart = Math.min(minStart, start);
                maxEnd = Math.max(maxEnd, end);
            }
        }
        if (found && minStart < maxEnd) {
            return new int[]{minStart, maxEnd};
        }
        return new int[]{0, 0};
    }

    /**
     * Tự động đồng bộ trạng thái sân thời gian thực theo đơn đặt sân và giờ thực tế (Vietnam Time UTC+7)
     * - Nếu khách đã đánh xong (giờ hiện tại >= giờ kết thúc khung giờ) -> Tự động chuyển về "Trống"
     * - Nếu có đơn đặt sân đang thi đấu trong khung giờ này -> Chuyển về "Đang sử dụng"
     * - Nếu sắp đến giờ đánh trong vòng 30 phút -> "Đã đặt trước"
     * - Giữ nguyên các sân đang "Bảo trì"
     */
    public synchronized void syncCourtStatusesWithCurrentBookings(List<Court> courts) {
        if (bookingRepository == null || courtRepository == null || courts == null || courts.isEmpty()) return;

        ZonedDateTime nowVN = ZonedDateTime.now(VN_ZONE);
        LocalDate today = nowVN.toLocalDate();
        double currentDecimal = nowVN.getHour() + (nowVN.getMinute() / 60.0);

        for (Court court : courts) {
            if (court == null || court.getCourtCode() == null || court.getCourtCode().isBlank()) {
                continue;
            }

            String currentStatus = court.getStatus() != null ? court.getStatus() : "Trống";
            // Không can thiệp nếu sân đang bảo trì
            if ("Bảo trì".equalsIgnoreCase(currentStatus)) {
                continue;
            }

            // Kiểm tra nếu sân đang bị tạm giữ chỗ (10 phút)
            if (heldCourtTimestamps.containsKey(court.getCourtCode())) {
                LocalDateTime expire = heldCourtTimestamps.get(court.getCourtCode());
                if (LocalDateTime.now().isAfter(expire)) {
                    heldCourtTimestamps.remove(court.getCourtCode());
                } else {
                    continue; // Vẫn đang trong thời gian giữ chỗ 10 phút
                }
            }

            // 1. Quét các đơn cũ từ các ngày trước mà vẫn bị kẹt trạng thái "Đang sử dụng"
            try {
                List<vn.yain.entity.Booking> allCourtBookings = bookingRepository.findByCourtCode(court.getCourtCode());
                if (allCourtBookings != null) {
                    for (vn.yain.entity.Booking b : allCourtBookings) {
                        if (b.getBookingDate() != null && b.getBookingDate().isBefore(today)
                                && "Đang sử dụng".equalsIgnoreCase(b.getStatus())) {
                            b.setStatus("Hoàn thành");
                            b.setCheckOutTime(nowVN.toLocalDateTime());
                            bookingRepository.save(b);
                        }
                    }
                }
            } catch (Exception ignored) {}

            // 2. Lấy danh sách đơn đặt sân của sân trong ngày hôm nay
            List<vn.yain.entity.Booking> todayBookings = bookingRepository.findByCourtCodeAndBookingDate(court.getCourtCode(), today);

            boolean isCurrentlyInUse = false;
            boolean isUpcomingSoon = false;

            if (todayBookings != null) {
                for (vn.yain.entity.Booking b : todayBookings) {
                    String bStatus = b.getStatus() != null ? b.getStatus() : "";
                    if (bStatus.equalsIgnoreCase("Đã hủy") || bStatus.equalsIgnoreCase("Đã hoàn cọc")) {
                        continue;
                    }

                    int[] hours = parseSlotHours(b.getTimeSlot());
                    int startHour = hours[0];
                    int endHour = hours[1];
                    if (startHour == 0 && endHour == 0) continue;

                    // Khách đã đánh xong (giờ hiện tại >= giờ kết thúc)
                    if (currentDecimal >= endHour) {
                        if ("Đang sử dụng".equalsIgnoreCase(bStatus) || "Đã nhận sân".equalsIgnoreCase(bStatus)) {
                            b.setStatus("Hoàn thành");
                            b.setCheckOutTime(nowVN.toLocalDateTime());
                            bookingRepository.save(b);
                        }
                        continue;
                    }

                    // Đang trong khung giờ thi đấu (startHour <= currentDecimal < endHour)
                    if (currentDecimal >= startHour && currentDecimal < endHour) {
                        if (!"Hoàn thành".equalsIgnoreCase(bStatus) && !bStatus.contains("hủy")) {
                            isCurrentlyInUse = true;
                            if (!"Đang sử dụng".equalsIgnoreCase(bStatus)) {
                                b.setStatus("Đang sử dụng");
                                if (b.getCheckInTime() == null) b.setCheckInTime(nowVN.toLocalDateTime());
                                bookingRepository.save(b);
                            }
                            break;
                        }
                    }

                    // Sắp diễn ra trong vòng 30 phút (startHour - currentDecimal <= 0.5)
                    if (currentDecimal < startHour && (startHour - currentDecimal) <= 0.5) {
                        if (!"Hoàn thành".equalsIgnoreCase(bStatus) && !bStatus.contains("hủy")) {
                            isUpcomingSoon = true;
                        }
                    }
                }
            }

            // 3. Quyết định trạng thái chuẩn của sân
            String targetStatus = "Trống";
            if (isCurrentlyInUse) {
                targetStatus = "Đang sử dụng";
            } else if (isUpcomingSoon) {
                targetStatus = "Đã đặt trước";
            }

            // Nếu trạng thái trong CSDL khác targetStatus -> Cập nhật và phát sự kiện realtime
            if (!targetStatus.equalsIgnoreCase(currentStatus)) {
                court.setStatus(targetStatus);
                courtRepository.save(court);
                log.info("Hệ thống tự động cập nhật trạng thái sân {}: {} -> {}", court.getCourtCode(), currentStatus, targetStatus);
                broadcastCourtEvent(CourtEventDto.builder()
                        .eventType("COURT_STATUS_CHANGED")
                        .courtCode(court.getCourtCode())
                        .branchCode(court.getBranchCode())
                        .status(targetStatus)
                        .timestamp(nowVN.toLocalDateTime())
                        .build());
            }
        }
    }

    /**
     * Scheduled task: Quét và cập nhật trạng thái sân thời gian thực mỗi 10 giây
     * Tự động giải phóng sân về "Trống" ngay khi khách hết giờ chơi
     */
    @Scheduled(fixedRate = 10000)
    public void scheduleSyncCourtStatuses() {
        if (courtRepository != null) {
            syncCourtStatusesWithCurrentBookings(courtRepository.findAll());
        }
    }

    public List<Court> getCourts(String branchCode) {
        List<Court> courts = (branchCode != null && !branchCode.isEmpty())
                ? courtRepository.findByBranchCode(branchCode)
                : courtRepository.findAll();
        if (courts != null && !courts.isEmpty()) {
            syncCourtStatusesWithCurrentBookings(courts);
        }
        return courts;
    }

    public Optional<Court> getCourtByCode(String courtCode) {
        return courtRepository.findByCourtCode(courtCode);
    }

    public Court updateCourtStatus(String courtCode, String newStatus) {
        Court court = courtRepository.findByCourtCode(courtCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sân với mã: " + courtCode));

        court.setStatus(newStatus);
        if ("Trống".equalsIgnoreCase(newStatus) && bookingRepository != null) {
            try {
                ZonedDateTime nowVN = ZonedDateTime.now(VN_ZONE);
                List<vn.yain.entity.Booking> todayBookings = bookingRepository.findByCourtCodeAndBookingDate(courtCode, nowVN.toLocalDate());
                if (todayBookings != null) {
                    for (vn.yain.entity.Booking b : todayBookings) {
                        if ("Đang sử dụng".equalsIgnoreCase(b.getStatus()) || "Đã nhận sân".equalsIgnoreCase(b.getStatus())) {
                            b.setStatus("Hoàn thành");
                            b.setCheckOutTime(nowVN.toLocalDateTime());
                            bookingRepository.save(b);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        Court saved = courtRepository.save(court);

        // Remove from hold timer if status changes
        if (!"Đang giữ chỗ".equalsIgnoreCase(newStatus)) {
            heldCourtTimestamps.remove(courtCode);
        }

        // Broadcast realtime update to all subscribers
        broadcastCourtEvent(CourtEventDto.builder()
                .eventType("COURT_STATUS_CHANGED")
                .courtCode(saved.getCourtCode())
                .branchCode(saved.getBranchCode())
                .status(saved.getStatus())
                .timestamp(LocalDateTime.now())
                .build());

        return saved;
    }

    /**
     * Temporary hold court for 10 minutes (Use Case XL-02)
     */
    public CourtEventDto holdCourtForBooking(String courtCode, String branchCode, String timeSlot, String customerName) {
        Court court = courtRepository.findByCourtCode(courtCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sân: " + courtCode));

        if ("Bảo trì".equalsIgnoreCase(court.getStatus())) {
            throw new IllegalStateException("Sân " + courtCode + " đang bảo trì, không thể giữ chỗ!");
        }

        court.setStatus("Đang giữ chỗ");
        courtRepository.save(court);

        // Record expiration time (10 minutes from now)
        heldCourtTimestamps.put(courtCode, LocalDateTime.now().plusSeconds(600));

        CourtEventDto event = CourtEventDto.builder()
                .eventType("COURT_HELD")
                .courtCode(courtCode)
                .branchCode(branchCode != null ? branchCode : court.getBranchCode())
                .timeSlot(timeSlot)
                .status("Đang giữ chỗ")
                .customerName(customerName != null ? customerName : "Khách trực tuyến")
                .holdRemainingSeconds(600) // 10 minutes = 600s
                .timestamp(LocalDateTime.now())
                .build();

        broadcastCourtEvent(event);
        return event;
    }

    /**
     * Scheduled task: Release held courts after 10 minutes if not confirmed
     */
    @Scheduled(fixedRate = 30000)
    public void releaseExpiredHeldCourts() {
        LocalDateTime now = LocalDateTime.now();
        heldCourtTimestamps.forEach((code, expireTime) -> {
            if (now.isAfter(expireTime)) {
                heldCourtTimestamps.remove(code);
                courtRepository.findByCourtCode(code).ifPresent(court -> {
                    if ("Đang giữ chỗ".equalsIgnoreCase(court.getStatus())) {
                        court.setStatus("Trống");
                        courtRepository.save(court);
                        log.info("Tự động giải phóng sân giữ chỗ quá hạn: {}", code);
                        broadcastCourtEvent(CourtEventDto.builder()
                                .eventType("COURT_HOLD_RELEASED")
                                .courtCode(court.getCourtCode())
                                .branchCode(court.getBranchCode())
                                .status("Trống")
                                .timestamp(now)
                                .build());
                    }
                });
            }
        });
    }

    public Court saveCourt(Court court) {
        if (court.getCourtCode() == null || court.getCourtCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã sân không được để trống!");
        }
        if (court.getBranchCode() == null || court.getBranchCode().trim().isEmpty()) {
            court.setBranchCode("CN01");
        }
        if (court.getCourtName() == null || court.getCourtName().trim().isEmpty()) {
            court.setCourtName("Sân " + court.getCourtCode());
        }
        if (court.getCourtType() == null || court.getCourtType().trim().isEmpty()) {
            court.setCourtType("Cầu lông tiêu chuẩn");
        }
        if (court.getHourlyRate() == null || court.getHourlyRate().compareTo(BigDecimal.ZERO) <= 0) {
            court.setHourlyRate(new BigDecimal("90000"));
        }
        if (court.getStatus() == null || court.getStatus().trim().isEmpty()) {
            court.setStatus("Trống");
        }

        Optional<Court> existingOpt = courtRepository.findByCourtCode(court.getCourtCode().trim());
        if (existingOpt.isPresent()) {
            Court existing = existingOpt.get();
            existing.setCourtName(court.getCourtName());
            existing.setCourtType(court.getCourtType());
            existing.setHourlyRate(court.getHourlyRate());
            existing.setStatus(court.getStatus());
            if (court.getImageUrl() != null && !court.getImageUrl().isBlank()) {
                existing.setImageUrl(court.getImageUrl());
            }
            if (court.getBranchCode() != null && !court.getBranchCode().isBlank()) {
                existing.setBranchCode(court.getBranchCode());
            }
            Court saved = courtRepository.save(existing);
            broadcastCourtEvent(CourtEventDto.builder()
                    .eventType("COURT_STATUS_CHANGED")
                    .courtCode(saved.getCourtCode())
                    .branchCode(saved.getBranchCode())
                    .status(saved.getStatus())
                    .timestamp(LocalDateTime.now())
                    .build());
            return saved;
        }

        Court saved = courtRepository.save(court);
        broadcastCourtEvent(CourtEventDto.builder()
                .eventType("COURT_STATUS_CHANGED")
                .courtCode(saved.getCourtCode())
                .branchCode(saved.getBranchCode())
                .status(saved.getStatus())
                .timestamp(LocalDateTime.now())
                .build());
        return saved;
    }

    public void deleteCourt(String courtCode) {
        Court court = courtRepository.findByCourtCode(courtCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sân với mã: " + courtCode));
        String branch = court.getBranchCode();
        courtRepository.delete(court);
        broadcastCourtEvent(CourtEventDto.builder()
                .eventType("COURT_DELETED")
                .courtCode(courtCode)
                .branchCode(branch)
                .status("Đã xóa")
                .timestamp(LocalDateTime.now())
                .build());
    }

    private void broadcastCourtEvent(CourtEventDto event) {
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/courts", event);
                messagingTemplate.convertAndSend("/topic/courts/" + event.getBranchCode(), event);
            } catch (Exception e) {
                log.warn("Không thể phát sự kiện realtime sân đấu: {}", e.getMessage());
            }
        }
    }
}
