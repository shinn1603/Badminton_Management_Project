package vn.yain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtEventDto {
    private String eventType; // COURT_STATUS_CHANGED, COURT_HELD, COURT_RELEASED, COURT_CHECKIN
    private String courtCode;
    private String branchCode;
    private String timeSlot;
    private String status; // Trống, Đang giữ chỗ, Đang sử dụng, Đã đặt cọc, Bảo trì
    private String customerName;
    private String bookingCode;
    private Integer holdRemainingSeconds;
    private LocalDateTime timestamp;
}
