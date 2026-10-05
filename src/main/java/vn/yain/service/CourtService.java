package vn.yain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.yain.dto.CourtEventDto;
import vn.yain.entity.Court;
import vn.yain.repository.CourtRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CourtService {

    @Autowired
    private CourtRepository courtRepository;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public List<Court> getCourts(String branchCode) {
        if (branchCode != null && !branchCode.isEmpty()) {
            return courtRepository.findByBranchCode(branchCode);
        }
        return courtRepository.findAll();
    }

    public Optional<Court> getCourtByCode(String courtCode) {
        return courtRepository.findByCourtCode(courtCode);
    }

    public Court updateCourtStatus(String courtCode, String newStatus) {
        Court court = courtRepository.findByCourtCode(courtCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sân với mã: " + courtCode));

        court.setStatus(newStatus);
        Court saved = courtRepository.save(court);

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

        court.setStatus("Đang giữ chỗ");
        courtRepository.save(court);

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

    private void broadcastCourtEvent(CourtEventDto event) {
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/courts", event);
                messagingTemplate.convertAndSend("/topic/courts/" + event.getBranchCode(), event);
            } catch (Exception e) {
                // Non-blocking fallback if socket broker is initializing
            }
        }
    }
}
