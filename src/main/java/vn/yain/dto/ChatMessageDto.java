package vn.yain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private String text;
    private String sender; // "user" or "bot"
    private String intent;
    private List<String> quickReplies;
    private String actionUrl;
    private String actionText;
    private String actionType; // "OPEN_PAYMENT", "REDIRECT", "LOOKUP_RESULT", "TOURNAMENT_LIST", "NONE"
    private Map<String, Object> paymentData;
    private Map<String, Object> lookupData;
    private List<Map<String, Object>> tournamentData;
    private LocalDateTime timestamp;
}
