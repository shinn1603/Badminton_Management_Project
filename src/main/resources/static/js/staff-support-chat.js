/**
 * UTE SPORT - STAFF LIVE SUPPORT CHAT MODULE
 * Real-time customer support bridge for POS Cashiers & Branch Managers
 * ZERO EMOJIS - LUXURY SPORT-TECH DESIGN
 */

(function () {
  'use strict';

  // Only initialize on staff pages (POS and Manager)
  const path = window.location.pathname.toLowerCase();
  const isStaffPage = path.startsWith('/pos/') || path.startsWith('/manager/') || path.startsWith('/director/');
  if (!isStaffPage) return;

  function initStaffSupportChat() {
    if (document.getElementById('staff-support-chat-trigger')) return;

    // 1. Inject Stylesheet
    if (!document.getElementById('staff-support-chat-css')) {
      const link = document.createElement('link');
      link.id = 'staff-support-chat-css';
      link.rel = 'stylesheet';
      link.href = '/css/staff-support-chat.css';
      document.head.appendChild(link);
    }

    // 2. Identify Staff User
    let staffUser = {
      name: 'Nhân viên trực',
      code: 'STAFF',
      branchCode: 'CN01',
      branchName: 'Chi nhánh Thủ Đức'
    };
    try {
      const stored = JSON.parse(localStorage.getItem('utesport_user') || 'null');
      if (stored) {
        staffUser.name = stored.fullName || stored.name || 'Nhân viên trực';
        staffUser.code = stored.staffCode || stored.username || 'STAFF';
        staffUser.branchCode = stored.branchCode || 'CN01';
        staffUser.branchName = stored.branchName || 'Chi nhánh Thủ Đức';
      }
    } catch (e) {}

    // Audio Notification using Web Audio API
    let soundEnabled = true;
    function playNotificationSound() {
      if (!soundEnabled) return;
      try {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        if (!AudioContext) return;
        const ctx = new AudioContext();
        const now = ctx.currentTime;

        const osc1 = ctx.createOscillator();
        const gain1 = ctx.createGain();
        osc1.type = 'sine';
        osc1.frequency.setValueAtTime(587.33, now); // D5
        osc1.frequency.exponentialRampToValueAtTime(880, now + 0.15); // A5
        gain1.gain.setValueAtTime(0.12, now);
        gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.3);

        osc1.connect(gain1);
        gain1.connect(ctx.destination);
        osc1.start(now);
        osc1.stop(now + 0.3);
      } catch (err) {}
    }

    // 3. Inject Floating Launcher & Modal HTML
    const root = document.createElement('div');
    root.id = 'staff-support-root';
    root.innerHTML = `
      <!-- Launcher Button -->
      <div id="staff-support-chat-trigger" title="Trung tâm hỗ trợ khách hàng">
        <div class="staff-trigger-icon-wrap">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
        </div>
        <span>Hỗ Trợ Khách Hàng</span>
        <span class="staff-trigger-badge" id="staff-support-badge">0</span>
      </div>

      <!-- Backdrop & Modal -->
      <div id="staff-support-backdrop">
        <div id="staff-support-modal">
          <!-- Header -->
          <div class="staff-modal-header">
            <div class="staff-header-left">
              <div class="staff-header-icon">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                  <circle cx="9" cy="7" r="4"></circle>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
              </div>
              <div>
                <div class="staff-header-title">TRUNG TÂM HỖ TRỢ TRỰC TUYẾN</div>
                <div class="staff-header-subtitle">
                  <span>Hệ thống UTE Sport</span>
                  <span>•</span>
                  <span id="staff-modal-branch-title">${escapeHtml(staffUser.branchName)}</span>
                </div>
              </div>
            </div>

            <div class="staff-header-right">
              <div class="staff-user-chip">
                <span class="staff-user-dot"></span>
                <span>${escapeHtml(staffUser.name)} (${escapeHtml(staffUser.code)})</span>
              </div>
              <button class="staff-header-btn" id="staff-sound-toggle-btn" title="Bật/Tắt âm báo">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" id="staff-sound-icon">
                  <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                  <path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>
                </svg>
              </button>
              <button class="staff-header-btn" id="staff-support-close-btn" title="Đóng cửa sổ">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>

          <!-- Body -->
          <div class="staff-modal-body">
            <!-- Sidebar (Tickets) -->
            <div class="staff-ticket-sidebar">
              <div class="staff-ticket-filters">
                <div class="staff-filter-tab active" data-filter="ALL">Tất cả</div>
                <div class="staff-filter-tab" data-filter="WAITING">Chờ nhận (<span id="staff-tab-waiting-count">0</span>)</div>
                <div class="staff-filter-tab" data-filter="ACTIVE">Đang phục vụ</div>
              </div>
              <div class="staff-ticket-list" id="staff-ticket-list-container">
                <div class="staff-empty-tickets">Đang tải danh sách...</div>
              </div>
            </div>

            <!-- Chat View -->
            <div class="staff-chat-view" id="staff-chat-view-container">
              <div class="staff-no-selection">
                <div class="staff-no-selection-icon">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                  </svg>
                </div>
                <div>Chọn một khách hàng trong danh sách bên trái để hỗ trợ</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
    document.body.appendChild(root);

    // Elements
    const triggerBtn = document.getElementById('staff-support-chat-trigger');
    const badgeEl = document.getElementById('staff-support-badge');
    const backdropEl = document.getElementById('staff-support-backdrop');
    const closeBtn = document.getElementById('staff-support-close-btn');
    const soundToggleBtn = document.getElementById('staff-sound-toggle-btn');
    const soundIcon = document.getElementById('staff-sound-icon');
    const ticketListContainer = document.getElementById('staff-ticket-list-container');
    const chatViewContainer = document.getElementById('staff-chat-view-container');
    const tabWaitingCount = document.getElementById('staff-tab-waiting-count');

    // State
    let activeTickets = [];
    let selectedTicket = null;
    let activeFilter = 'ALL';
    let lastWaitingCount = 0;
    let ticketPollingTimer = null;
    let messagePollingTimer = null;
    let lastStaffMsgId = 0;

    // Toggle Modal
    function openModal() {
      backdropEl.classList.add('open');
      pollTickets();
    }

    function closeModal() {
      backdropEl.classList.remove('open');
      if (messagePollingTimer) {
        clearInterval(messagePollingTimer);
        messagePollingTimer = null;
      }
    }

    triggerBtn.addEventListener('click', openModal);
    closeBtn.addEventListener('click', closeModal);
    backdropEl.addEventListener('click', (e) => {
      if (e.target === backdropEl) closeModal();
    });

    // Sound Toggle
    soundToggleBtn.addEventListener('click', () => {
      soundEnabled = !soundEnabled;
      if (soundEnabled) {
        soundIcon.innerHTML = `
          <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
          <path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>
        `;
      } else {
        soundIcon.innerHTML = `
          <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
          <line x1="23" y1="9" x2="17" y2="15"></line>
          <line x1="17" y1="9" x2="23" y2="15"></line>
        `;
      }
    });

    // Filter Tabs
    document.querySelectorAll('.staff-filter-tab').forEach(tab => {
      tab.addEventListener('click', function () {
        document.querySelectorAll('.staff-filter-tab').forEach(t => t.classList.remove('active'));
        this.classList.add('active');
        activeFilter = this.getAttribute('data-filter');
        renderTicketList();
      });
    });

    // 4. Poll Tickets
    async function pollTickets() {
      try {
        const res = await fetch('/api/support/tickets');
        if (!res.ok) return;
        const data = await res.json();
        const tickets = data.tickets || [];
        const waitingCount = Number(data.waitingCount || 0);

        // Check if new waiting customer arrived
        if (waitingCount > lastWaitingCount) {
          playNotificationSound();
        }
        lastWaitingCount = waitingCount;

        // Update badge
        if (waitingCount > 0) {
          badgeEl.style.display = 'inline-flex';
          badgeEl.textContent = waitingCount;
          badgeEl.classList.add('pulse');
        } else {
          badgeEl.style.display = 'none';
          badgeEl.classList.remove('pulse');
        }
        tabWaitingCount.textContent = waitingCount;

        activeTickets = tickets;
        renderTicketList();

        // If currently viewing a ticket, check if it's still present
        if (selectedTicket) {
          const updated = tickets.find(t => t.sessionId === selectedTicket.sessionId);
          if (updated) {
            selectedTicket = updated;
            updateChatHeaderInfo();
          }
        }
      } catch (err) {}
    }

    // Render Ticket List in Sidebar
    function renderTicketList() {
      let filtered = activeTickets;
      if (activeFilter === 'WAITING') {
        filtered = activeTickets.filter(t => t.status === 'WAITING_STAFF');
      } else if (activeFilter === 'ACTIVE') {
        filtered = activeTickets.filter(t => t.status === 'ACTIVE_STAFF');
      }

      if (filtered.length === 0) {
        ticketListContainer.innerHTML = `<div class="staff-empty-tickets">Không có phiên hỗ trợ nào.</div>`;
        return;
      }

      let html = '';
      filtered.forEach(t => {
        const isSelected = selectedTicket && selectedTicket.sessionId === t.sessionId;
        const custName = escapeHtml(t.customerName || (t.customerPhone ? ('Khách ' + t.customerPhone) : 'Khách vãng lai'));
        const lastMsg = escapeHtml(t.lastMessage || 'Yêu cầu kết nối nhân viên tư vấn');
        const timeStr = t.lastMessageAt ? formatTime(t.lastMessageAt) : '';

        let badgeClass = 'waiting';
        let badgeText = 'Chờ nhận';
        if (t.status === 'ACTIVE_STAFF') {
          badgeClass = 'active';
          badgeText = 'Đang chat';
        } else if (t.status === 'CLOSED') {
          badgeClass = 'closed';
          badgeText = 'Đã đóng';
        }

        html += `
          <div class="staff-ticket-item ${isSelected ? 'active' : ''}" data-session="${escapeHtml(t.sessionId)}">
            <div class="staff-ticket-row-top">
              <span class="staff-ticket-cust-name" title="${custName}">${custName}</span>
              <span class="staff-status-badge ${badgeClass}">${badgeText}</span>
            </div>
            <div class="staff-ticket-row-mid" title="${lastMsg}">${lastMsg}</div>
            <div class="staff-ticket-row-bot">
              <span class="staff-ticket-branch">${escapeHtml(t.branchCode || 'CN')}</span>
              <div style="display: flex; align-items: center; gap: 6px;">
                ${t.unreadStaffCount && t.unreadStaffCount > 0 ? `<span class="staff-unread-count">${t.unreadStaffCount}</span>` : ''}
                <span>${timeStr}</span>
              </div>
            </div>
          </div>
        `;
      });

      ticketListContainer.innerHTML = html;

      // Bind click events
      ticketListContainer.querySelectorAll('.staff-ticket-item').forEach(item => {
        item.addEventListener('click', function () {
          const sid = this.getAttribute('data-session');
          const ticket = activeTickets.find(t => t.sessionId === sid);
          if (ticket) selectTicket(ticket);
        });
      });
    }

    // 5. Select & Open Ticket Chat
    async function selectTicket(ticket) {
      selectedTicket = ticket;
      lastStaffMsgId = 0;
      renderTicketList();

      const custName = escapeHtml(ticket.customerName || (ticket.customerPhone ? ('Khách ' + ticket.customerPhone) : 'Khách vãng lai'));
      const phone = ticket.customerPhone ? escapeHtml(ticket.customerPhone) : 'Chưa cập nhật';
      const branch = escapeHtml(ticket.branchCode || 'Toàn hệ thống');

      chatViewContainer.innerHTML = `
        <!-- View Header -->
        <div class="staff-view-header">
          <div class="staff-view-cust-info">
            <div class="staff-view-cust-title" id="staff-view-cust-title">${custName}</div>
            <div class="staff-view-cust-sub">
              <span>SĐT: <strong style="color: #0f172a;">${phone}</strong></span>
              <span>•</span>
              <span>Cơ sở: <strong style="color: #047857;">${branch}</strong></span>
              <span>•</span>
              <span id="staff-view-status-tag"></span>
            </div>
          </div>

          <div class="staff-view-actions">
            <button class="staff-btn-accept" id="staff-btn-accept" style="display: none;">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
              <span>Tiếp Nhận Chat</span>
            </button>
            <button class="staff-btn-close-session" id="staff-btn-close">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
              <span>Kết Thúc Hỗ Trợ</span>
            </button>
          </div>
        </div>

        <!-- Message List -->
        <div class="staff-msg-list" id="staff-msg-list">
          <div style="text-align: center; color: #94a3b8; font-size: 12px; padding: 20px;">Đang tải tin nhắn...</div>
        </div>

        <!-- Canned Quick Replies -->
        <div class="staff-canned-replies">
          <span style="font-size: 11px; font-weight: 700; color: #64748b; white-space: nowrap;">Mẫu câu nhanh:</span>
          <button class="staff-canned-pill" data-text="Chào bạn! Tôi là ${escapeHtml(staffUser.name)} từ UTE Sport. Tôi có thể hỗ trợ gì cho bạn ạ?">Chào hỏi</button>
          <button class="staff-canned-pill" data-text="Dạ sân tại chi nhánh đang trống khung giờ này nhé, bạn có muốn khóa sân luôn không ạ?">Báo sân trống</button>
          <button class="staff-canned-pill" data-text="Bạn vui lòng đợi nhân viên kiểm tra hệ thống trong giây lát nhé!">Chờ 1 phút</button>
          <button class="staff-canned-pill" data-text="Đã cập nhật yêu cầu cho bạn thành công rồi nhé!">Đã xử lý xong</button>
          <button class="staff-canned-pill" data-text="Cảm ơn bạn đã liên hệ UTE Sport. Chúc bạn có buổi chơi thể thao vui vẻ!">Kết thúc & Cảm ơn</button>
        </div>

        <!-- Input Area -->
        <div class="staff-input-area">
          <input type="text" class="staff-input-field" id="staff-input-field" placeholder="Nhập tin nhắn phản hồi cho khách..." autocomplete="off">
          <button class="staff-btn-send" id="staff-btn-send">
            <span>Gửi</span>
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2"><line x1="22" y1="2" x2="11" y2="13"></line><polygon points="22 2 15 22 11 13 2 9 22 2"></polygon></svg>
          </button>
        </div>
      `;

      updateChatHeaderInfo();

      // Bind Accept Button
      const btnAccept = document.getElementById('staff-btn-accept');
      btnAccept.addEventListener('click', acceptTicket);

      // Bind Close Button
      const btnClose = document.getElementById('staff-btn-close');
      btnClose.addEventListener('click', closeSession);

      // Bind Canned Replies
      chatViewContainer.querySelectorAll('.staff-canned-pill').forEach(pill => {
        pill.addEventListener('click', function () {
          const txt = this.getAttribute('data-text');
          const input = document.getElementById('staff-input-field');
          input.value = txt;
          input.focus();
        });
      });

      // Bind Send
      const sendBtn = document.getElementById('staff-btn-send');
      const inputField = document.getElementById('staff-input-field');
      sendBtn.addEventListener('click', () => {
        const val = inputField.value.trim();
        if (val) submitStaffMessage(val);
      });
      inputField.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          const val = inputField.value.trim();
          if (val) submitStaffMessage(val);
        }
      });
      inputField.focus();

      // Load Messages & Start Polling
      await loadMessages();
      if (messagePollingTimer) clearInterval(messagePollingTimer);
      messagePollingTimer = setInterval(loadMessages, 2000);
    }

    function updateChatHeaderInfo() {
      if (!selectedTicket) return;
      const statusTag = document.getElementById('staff-view-status-tag');
      const btnAccept = document.getElementById('staff-btn-accept');
      if (!statusTag) return;

      if (selectedTicket.status === 'WAITING_STAFF') {
        statusTag.innerHTML = `<span class="staff-status-badge waiting">Chờ Tiếp Nhận</span>`;
        if (btnAccept) btnAccept.style.display = 'inline-flex';
      } else if (selectedTicket.status === 'ACTIVE_STAFF') {
        statusTag.innerHTML = `<span class="staff-status-badge active">Đang Phục Vụ (NV: ${escapeHtml(selectedTicket.assignedStaff || staffUser.name)})</span>`;
        if (btnAccept) btnAccept.style.display = 'none';
      } else {
        statusTag.innerHTML = `<span class="staff-status-badge closed">Đã Kết Thúc</span>`;
        if (btnAccept) btnAccept.style.display = 'none';
      }
    }

    // Accept Ticket
    async function acceptTicket() {
      if (!selectedTicket) return;
      const greetText = `Xin chào bạn! Tôi là ${staffUser.name} - nhân viên trực ban tại UTE Sport. Tôi có thể hỗ trợ điều gì cho bạn ạ?`;
      await submitStaffMessage(greetText);
      selectedTicket.status = 'ACTIVE_STAFF';
      selectedTicket.assignedStaff = staffUser.name;
      updateChatHeaderInfo();
      pollTickets();
    }

    // Submit Staff Message
    async function submitStaffMessage(content) {
      if (!selectedTicket || !content.trim()) return;
      const input = document.getElementById('staff-input-field');
      if (input) input.value = '';

      try {
        await fetch('/api/support/message', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            sessionId: selectedTicket.sessionId,
            content: content.trim(),
            sender: 'staff',
            staffName: staffUser.name,
            staffCode: staffUser.code,
            branchCode: staffUser.branchCode
          })
        });
        await loadMessages();
        pollTickets();
      } catch (err) {}
    }

    // Close Support Session
    async function closeSession() {
      if (!selectedTicket) return;
      const ok = confirm(`Bạn có chắc chắn muốn kết thúc phiên hỗ trợ với khách hàng ${selectedTicket.customerName || ''}?`);
      if (!ok) return;

      try {
        await fetch('/api/support/close', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            sessionId: selectedTicket.sessionId,
            closedBy: staffUser.name
          })
        });

        // Add closing system message in view
        appendBubbleToStaffView({
          sender: 'system',
          content: `Phiên hỗ trợ đã được kết thúc bởi nhân viên ${staffUser.name}.`,
          createdAt: new Date().toISOString()
        });

        selectedTicket.status = 'CLOSED';
        updateChatHeaderInfo();
        pollTickets();
      } catch (err) {}
    }

    // Load Messages for Selected Ticket
    async function loadMessages() {
      if (!selectedTicket) return;
      try {
        const res = await fetch(`/api/support/messages?sessionId=${encodeURIComponent(selectedTicket.sessionId)}&role=staff`);
        if (!res.ok) return;
        const data = await res.json();
        const messages = data.messages || [];
        const msgList = document.getElementById('staff-msg-list');
        if (!msgList) return;

        // If initial load or list was empty
        if (lastStaffMsgId === 0 || msgList.children.length === 0 || msgList.innerText.includes('Đang tải')) {
          msgList.innerHTML = '';
          messages.forEach(m => appendBubbleToStaffView(m));
          if (messages.length > 0) {
            lastStaffMsgId = Math.max(...messages.map(m => m.id || 0));
          }
          scrollStaffToBottom();
        } else {
          // Append only new messages
          let hasNew = false;
          messages.forEach(m => {
            if (m.id > lastStaffMsgId) {
              lastStaffMsgId = m.id;
              appendBubbleToStaffView(m);
              hasNew = true;
              if (m.sender === 'customer') {
                playNotificationSound();
              }
            }
          });
          if (hasNew) scrollStaffToBottom();
        }
      } catch (err) {}
    }

    function appendBubbleToStaffView(m) {
      const msgList = document.getElementById('staff-msg-list');
      if (!msgList) return;

      const bubble = document.createElement('div');
      const sender = m.sender || 'customer';
      bubble.className = `staff-bubble ${sender}`;

      let authorText = 'Khách hàng';
      if (sender === 'staff') {
        authorText = `Nhân viên: ${m.staffName || staffUser.name}`;
      } else if (sender === 'bot') {
        authorText = 'Trợ lý AI UTE Sport';
      } else if (sender === 'system') {
        authorText = 'Hệ thống';
      }

      const timeStr = m.createdAt ? formatTime(m.createdAt) : '';

      bubble.innerHTML = `
        ${sender !== 'system' ? `<div class="staff-bubble-author">${escapeHtml(authorText)}</div>` : ''}
        <div style="white-space: pre-line;">${escapeHtml(m.content || '')}</div>
        ${timeStr ? `<div class="staff-bubble-time">${timeStr}</div>` : ''}
      `;

      msgList.appendChild(bubble);
    }

    function scrollStaffToBottom() {
      const msgList = document.getElementById('staff-msg-list');
      if (msgList) msgList.scrollTop = msgList.scrollHeight;
    }

    function escapeHtml(str) {
      if (!str) return '';
      return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function formatTime(isoStr) {
      try {
        const d = new Date(isoStr);
        return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
      } catch (e) {
        return '';
      }
    }

    // Start background polling for new tickets every 3.5 seconds
    pollTickets();
    ticketPollingTimer = setInterval(pollTickets, 3500);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initStaffSupportChat);
  } else {
    initStaffSupportChat();
  }
})();
