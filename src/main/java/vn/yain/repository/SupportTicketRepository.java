package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.SupportTicket;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findBySessionId(String sessionId);

    List<SupportTicket> findByStatusInOrderByLastMessageAtDesc(List<String> statuses);

    List<SupportTicket> findAllByOrderByLastMessageAtDesc();

    long countByStatus(String status);
}
