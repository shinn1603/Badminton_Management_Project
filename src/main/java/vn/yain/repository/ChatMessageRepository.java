package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.ChatMessage;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<ChatMessage> findBySessionIdAndIdGreaterThanOrderByCreatedAtAsc(String sessionId, Long id);

    long countBySessionIdAndSenderAndReadByStaffFalse(String sessionId, String sender);

    long countBySessionIdAndSenderAndReadByCustomerFalse(String sessionId, String sender);
}
