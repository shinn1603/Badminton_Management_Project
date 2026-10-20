package vn.yain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.yain.entity.ChatMessage;
import vn.yain.entity.SupportTicket;
import vn.yain.repository.ChatMessageRepository;
import vn.yain.repository.SupportTicketRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SupportChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @InjectMocks
    private SupportChatService supportChatService;

    private String sessionId;

    @BeforeEach
    void setUp() {
        sessionId = "test_sess_123";
    }

    @Test
    void testRequestHumanSupport_createsTicketAndSystemMessage() {
        when(supportTicketRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        SupportTicket ticket = supportChatService.requestHumanSupport(sessionId, "Nguyen Van A", "0903123456", "CN01");

        assertNotNull(ticket);
        assertEquals("WAITING_STAFF", ticket.getStatus());
        assertEquals("Nguyen Van A", ticket.getCustomerName());
        assertEquals("0903123456", ticket.getCustomerPhone());
        assertEquals("CN01", ticket.getBranchCode());
        verify(supportTicketRepository, times(1)).save(any(SupportTicket.class));
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void testSendCustomerMessage_savesMessageAndUpdatesTicket() {
        SupportTicket existingTicket = SupportTicket.builder()
                .sessionId(sessionId)
                .status("WAITING_STAFF")
                .unreadStaffCount(0)
                .build();

        when(supportTicketRepository.findBySessionId(sessionId)).thenReturn(Optional.of(existingTicket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatMessage msg = supportChatService.sendCustomerMessage(sessionId, "Xin chào nhân viên!", "Nguyen Van A", "0903123456");

        assertNotNull(msg);
        assertEquals("customer", msg.getSender());
        assertEquals("Xin chào nhân viên!", msg.getContent());
        assertEquals(1, existingTicket.getUnreadStaffCount());
    }

    @Test
    void testSendStaffMessage_updatesTicketToActiveStaff() {
        SupportTicket existingTicket = SupportTicket.builder()
                .sessionId(sessionId)
                .status("WAITING_STAFF")
                .unreadCustomerCount(0)
                .build();

        when(supportTicketRepository.findBySessionId(sessionId)).thenReturn(Optional.of(existingTicket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatMessage msg = supportChatService.sendStaffMessage(sessionId, "Nguyễn Thu Ngân", "NV001", "CN01", "Chào bạn, mình có thể hỗ trợ gì?");

        assertNotNull(msg);
        assertEquals("staff", msg.getSender());
        assertEquals("Nguyễn Thu Ngân", msg.getStaffName());
        assertEquals("ACTIVE_STAFF", existingTicket.getStatus());
        assertEquals("Nguyễn Thu Ngân", existingTicket.getAssignedStaff());
        assertEquals(1, existingTicket.getUnreadCustomerCount());
    }

    @Test
    void testCloseSession_marksStatusClosed() {
        SupportTicket existingTicket = SupportTicket.builder()
                .sessionId(sessionId)
                .status("ACTIVE_STAFF")
                .build();

        when(supportTicketRepository.findBySessionId(sessionId)).thenReturn(Optional.of(existingTicket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        SupportTicket closed = supportChatService.closeSession(sessionId, "Nhân viên");

        assertNotNull(closed);
        assertEquals("CLOSED", closed.getStatus());
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void testGetMessages_marksReadForStaffRole() {
        ChatMessage m1 = ChatMessage.builder()
                .id(1L)
                .sessionId(sessionId)
                .sender("customer")
                .readByStaff(false)
                .build();

        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).thenReturn(List.of(m1));
        when(supportTicketRepository.findBySessionId(sessionId)).thenReturn(Optional.of(SupportTicket.builder().sessionId(sessionId).unreadStaffCount(1).build()));

        List<ChatMessage> list = supportChatService.getMessages(sessionId, null, "staff");

        assertEquals(1, list.size());
        assertTrue(m1.getReadByStaff());
    }
}
