package vn.yain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.yain.entity.ChatMessage;
import vn.yain.entity.SupportTicket;
import vn.yain.repository.ChatMessageRepository;
import vn.yain.repository.SupportTicketRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class SupportChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Transactional
    public SupportTicket requestHumanSupport(String sessionId, String customerName, String customerPhone, String branchCode) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "sess_" + System.currentTimeMillis();
        }

        Optional<SupportTicket> opt = supportTicketRepository.findBySessionId(sessionId);
        SupportTicket ticket;
        if (opt.isPresent()) {
            ticket = opt.get();
            ticket.setStatus("WAITING_STAFF");
            if (customerName != null && !customerName.isBlank()) ticket.setCustomerName(customerName);
            if (customerPhone != null && !customerPhone.isBlank()) ticket.setCustomerPhone(customerPhone);
            if (branchCode != null && !branchCode.isBlank()) ticket.setBranchCode(branchCode);
            ticket.setLastMessage("Khách hàng yêu cầu hỗ trợ trực tiếp từ nhân viên");
            ticket.setLastMessageAt(LocalDateTime.now());
            ticket.setUnreadStaffCount(ticket.getUnreadStaffCount() == null ? 1 : ticket.getUnreadStaffCount() + 1);
        } else {
            ticket = SupportTicket.builder()
                    .sessionId(sessionId)
                    .customerName(customerName != null && !customerName.isBlank() ? customerName : "Khách hàng")
                    .customerPhone(customerPhone != null && !customerPhone.isBlank() ? customerPhone : "")
                    .branchCode(branchCode != null && !branchCode.isBlank() ? branchCode : "CN01")
                    .status("WAITING_STAFF")
                    .lastMessage("Khách hàng yêu cầu hỗ trợ trực tiếp từ nhân viên")
                    .lastMessageAt(LocalDateTime.now())
                    .unreadStaffCount(1)
                    .unreadCustomerCount(0)
                    .build();
        }
        ticket = supportTicketRepository.save(ticket);

        // System notification message
        ChatMessage sysMsg = ChatMessage.builder()
                .sessionId(sessionId)
                .sender("system")
                .content("Hệ thống đã gửi thông báo đến nhân viên trực ban chi nhánh. Vui lòng đặt câu hỏi tại đây, nhân viên sẽ tiếp nhận và trò chuyện cùng bạn ngay!")
                .customerName(ticket.getCustomerName())
                .customerPhone(ticket.getCustomerPhone())
                .readByStaff(false)
                .readByCustomer(true)
                .build();
        chatMessageRepository.save(sysMsg);

        return ticket;
    }

    @Transactional
    public ChatMessage sendCustomerMessage(String sessionId, String content, String customerName, String customerPhone) {
        if (sessionId == null || sessionId.isBlank() || content == null || content.isBlank()) {
            return null;
        }

        Optional<SupportTicket> opt = supportTicketRepository.findBySessionId(sessionId);
        SupportTicket ticket;
        if (opt.isPresent()) {
            ticket = opt.get();
            if ("CLOSED".equalsIgnoreCase(ticket.getStatus()) || "BOT".equalsIgnoreCase(ticket.getStatus())) {
                ticket.setStatus("WAITING_STAFF");
            }
            if (customerName != null && !customerName.isBlank()) ticket.setCustomerName(customerName);
            if (customerPhone != null && !customerPhone.isBlank()) ticket.setCustomerPhone(customerPhone);
            ticket.setLastMessage(content);
            ticket.setLastMessageAt(LocalDateTime.now());
            ticket.setUnreadStaffCount(ticket.getUnreadStaffCount() == null ? 1 : ticket.getUnreadStaffCount() + 1);
            supportTicketRepository.save(ticket);
        } else {
            ticket = SupportTicket.builder()
                    .sessionId(sessionId)
                    .customerName(customerName != null && !customerName.isBlank() ? customerName : "Khách hàng")
                    .customerPhone(customerPhone != null && !customerPhone.isBlank() ? customerPhone : "")
                    .status("WAITING_STAFF")
                    .lastMessage(content)
                    .lastMessageAt(LocalDateTime.now())
                    .unreadStaffCount(1)
                    .unreadCustomerCount(0)
                    .build();
            supportTicketRepository.save(ticket);
        }

        ChatMessage msg = ChatMessage.builder()
                .sessionId(sessionId)
                .sender("customer")
                .content(content)
                .customerName(ticket.getCustomerName())
                .customerPhone(ticket.getCustomerPhone())
                .readByStaff(false)
                .readByCustomer(true)
                .build();

        return chatMessageRepository.save(msg);
    }

    @Transactional
    public ChatMessage sendStaffMessage(String sessionId, String staffName, String staffCode, String branchCode, String content) {
        if (sessionId == null || sessionId.isBlank() || content == null || content.isBlank()) {
            return null;
        }

        String displayName = (staffName != null && !staffName.isBlank()) ? staffName : "Nhân viên hỗ trợ";

        Optional<SupportTicket> opt = supportTicketRepository.findBySessionId(sessionId);
        if (opt.isPresent()) {
            SupportTicket ticket = opt.get();
            ticket.setStatus("ACTIVE_STAFF");
            ticket.setAssignedStaff(displayName);
            if (branchCode != null && !branchCode.isBlank()) ticket.setBranchCode(branchCode);
            ticket.setLastMessage(content);
            ticket.setLastMessageAt(LocalDateTime.now());
            ticket.setUnreadCustomerCount(ticket.getUnreadCustomerCount() == null ? 1 : ticket.getUnreadCustomerCount() + 1);
            supportTicketRepository.save(ticket);
        }

        ChatMessage msg = ChatMessage.builder()
                .sessionId(sessionId)
                .sender("staff")
                .staffName(displayName)
                .staffCode(staffCode)
                .branchCode(branchCode)
                .content(content)
                .readByStaff(true)
                .readByCustomer(false)
                .build();

        return chatMessageRepository.save(msg);
    }

    @Transactional
    public SupportTicket closeSession(String sessionId, String closedBy) {
        if (sessionId == null || sessionId.isBlank()) return null;

        Optional<SupportTicket> opt = supportTicketRepository.findBySessionId(sessionId);
        if (opt.isEmpty()) return null;

        SupportTicket ticket = opt.get();
        ticket.setStatus("CLOSED");
        ticket.setLastMessage("Phiên hỗ trợ kết thúc bởi " + (closedBy != null ? closedBy : "người dùng"));
        ticket.setLastMessageAt(LocalDateTime.now());
        ticket.setUnreadStaffCount(0);
        ticket.setUnreadCustomerCount(0);
        ticket = supportTicketRepository.save(ticket);

        ChatMessage sysMsg = ChatMessage.builder()
                .sessionId(sessionId)
                .sender("system")
                .content("Phiên hỗ trợ trực tiếp với nhân viên đã kết thúc. Chatbot AI UTE Sport đã sẵn sàng hỗ trợ bạn tiếp tục bất cứ lúc nào!")
                .readByStaff(true)
                .readByCustomer(true)
                .build();
        chatMessageRepository.save(sysMsg);

        return ticket;
    }

    @Transactional
    public List<ChatMessage> getMessages(String sessionId, Long afterId, String role) {
        if (sessionId == null || sessionId.isBlank()) {
            return Collections.emptyList();
        }

        List<ChatMessage> list;
        if (afterId != null && afterId > 0) {
            list = chatMessageRepository.findBySessionIdAndIdGreaterThanOrderByCreatedAtAsc(sessionId, afterId);
        } else {
            list = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        }

        // Mark read
        if ("staff".equalsIgnoreCase(role)) {
            for (ChatMessage m : list) {
                if ("customer".equals(m.getSender()) && Boolean.FALSE.equals(m.getReadByStaff())) {
                    m.setReadByStaff(true);
                    chatMessageRepository.save(m);
                }
            }
            supportTicketRepository.findBySessionId(sessionId).ifPresent(t -> {
                t.setUnreadStaffCount(0);
                supportTicketRepository.save(t);
            });
        } else if ("customer".equalsIgnoreCase(role)) {
            for (ChatMessage m : list) {
                if ("staff".equals(m.getSender()) && Boolean.FALSE.equals(m.getReadByCustomer())) {
                    m.setReadByCustomer(true);
                    chatMessageRepository.save(m);
                }
            }
            supportTicketRepository.findBySessionId(sessionId).ifPresent(t -> {
                t.setUnreadCustomerCount(0);
                supportTicketRepository.save(t);
            });
        }

        return list;
    }

    public List<SupportTicket> getActiveTickets() {
        return supportTicketRepository.findByStatusInOrderByLastMessageAtDesc(List.of("WAITING_STAFF", "ACTIVE_STAFF"));
    }

    public List<SupportTicket> getAllTickets() {
        return supportTicketRepository.findAllByOrderByLastMessageAtDesc();
    }

    public Optional<SupportTicket> getTicket(String sessionId) {
        return supportTicketRepository.findBySessionId(sessionId);
    }

    public long getWaitingCount() {
        return supportTicketRepository.countByStatus("WAITING_STAFF");
    }

    public boolean isSessionLiveWithStaff(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return false;
        Optional<SupportTicket> opt = supportTicketRepository.findBySessionId(sessionId);
        return opt.isPresent() && ("WAITING_STAFF".equalsIgnoreCase(opt.get().getStatus()) || "ACTIVE_STAFF".equalsIgnoreCase(opt.get().getStatus()));
    }
}
