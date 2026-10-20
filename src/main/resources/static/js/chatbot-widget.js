/**
 * UTE SPORT - CONVERSATIONAL AI CHATBOT ASSISTANT
 * Multi-turn dialog state tracking, branch locator, court availability & auto-booking execution
 * ZERO EMOJIS - LUXURY SPORT-TECH DESIGN
 */

(function () {
  'use strict';

  // Only initialize on Customer browsing pages (exclude auth / login screens)
  const path = window.location.pathname.toLowerCase();
  const isAuthPage = path.startsWith('/customer/auth') || path.startsWith('/customer/login') || path === '/login';
  const isCustomerPage = path.startsWith('/customer/') || path === '/' || path.startsWith('/booking');
  if (isAuthPage || !isCustomerPage) return;

  function initChatbot() {
    if (document.getElementById('ute-chatbot-toggle')) return;

    // Session ID management for conversational memory
    let sessionId = localStorage.getItem('chatbot_session_id');
    if (!sessionId) {
      sessionId = 'sess_' + Math.random().toString(36).substring(2, 12);
      localStorage.setItem('chatbot_session_id', sessionId);
    }

    // 1. Inject Stylesheet if not present
    if (!document.getElementById('chatbot-widget-css')) {
      const link = document.createElement('link');
      link.id = 'chatbot-widget-css';
      link.rel = 'stylesheet';
      link.href = '/css/chatbot-widget.css';
      document.head.appendChild(link);
    }

    // 2. Inject DOM Elements (Clean text, no emojis)
    const container = document.createElement('div');
    container.id = 'ute-chatbot-root';
    container.innerHTML = `
      <div id="ute-chatbot-toggle" title="Hỗ trợ & Đặt sân trực tuyến">
        <div class="chatbot-pulse-ring"></div>
        <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
        </svg>
      </div>

      <div id="ute-chatbot-window">
        <div class="chatbot-header">
          <div class="chatbot-header-info">
            <div class="chatbot-avatar">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#ffffff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"></path>
                <path d="M2 12h20"></path>
              </svg>
            </div>
            <div>
              <div class="chatbot-title" id="chatbot-header-title">Hỗ trợ UTE Sport</div>
              <div class="chatbot-status">
                <span class="chatbot-status-dot" id="chatbot-status-dot"></span>
                <span id="chatbot-status-text">Tư vấn & Đặt sân trực tuyến</span>
              </div>
            </div>
          </div>
          <div class="chatbot-header-actions">
            <button class="chatbot-call-staff-pill" id="chatbot-call-staff-btn" title="Gặp nhân viên tư vấn trực tiếp">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
              <span>Nhân viên</span>
            </button>
            <button class="chatbot-live-leave-btn" id="chatbot-leave-staff-btn" style="display: none;" title="Dừng chat với nhân viên & quay lại trợ lý ảo">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
              <span>Dừng chat</span>
            </button>
            <button class="chatbot-header-action-btn" id="chatbot-reset-btn" title="Làm mới cuộc trò chuyện">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 12a9 9 0 0 1 15-6.7L21 8"></path>
                <path d="M21 3v5h-5"></path>
                <path d="M21 12a9 9 0 0 1-15 6.7L3 16"></path>
                <path d="M3 21v-5h5"></path>
              </svg>
            </button>
            <button class="chatbot-close-btn" id="chatbot-close" title="Đóng">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>
        </div>

        <div class="chatbot-messages" id="chatbot-messages-list"></div>

        <div class="chatbot-input-wrap">
          <input type="text" class="chatbot-input" id="chatbot-input-field" placeholder="Hỏi chi nhánh, giờ chơi, tra cứu đơn..." autocomplete="off">
          <button class="chatbot-send-btn" id="chatbot-send-btn" title="Gửi câu hỏi">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          </button>
        </div>
      </div>
    `;
    document.body.appendChild(container);

    // Elements
    const toggleBtn = document.getElementById('ute-chatbot-toggle');
    const closeBtn = document.getElementById('chatbot-close');
    const resetBtn = document.getElementById('chatbot-reset-btn');
    const callStaffBtn = document.getElementById('chatbot-call-staff-btn');
    const leaveStaffBtn = document.getElementById('chatbot-leave-staff-btn');
    const headerTitle = document.getElementById('chatbot-header-title');
    const statusDot = document.getElementById('chatbot-status-dot');
    const statusText = document.getElementById('chatbot-status-text');
    const chatWin = document.getElementById('ute-chatbot-window');
    const msgList = document.getElementById('chatbot-messages-list');
    const inputField = document.getElementById('chatbot-input-field');
    const sendBtn = document.getElementById('chatbot-send-btn');

    let isLiveSupportMode = false;
    let supportPollTimer = null;
    let lastSupportMsgId = 0;
    let isFirstOpen = true;

    function getUserInfo() {
      try {
        return JSON.parse(localStorage.getItem('utesport_user') || 'null');
      } catch (e) {
        return null;
      }
    }

    function enterLiveSupportMode() {
      isLiveSupportMode = true;
      if (headerTitle) headerTitle.textContent = 'Tư Vấn Trực Tuyến';
      if (statusDot) {
        statusDot.className = 'chatbot-status-dot waiting';
      }
      if (statusText) {
        statusText.textContent = 'Đang kết nối nhân viên trực...';
      }
      if (callStaffBtn) callStaffBtn.style.display = 'none';
      if (leaveStaffBtn) leaveStaffBtn.style.display = 'inline-flex';
      if (inputField) inputField.placeholder = 'Nhập tin nhắn gửi nhân viên...';

      if (supportPollTimer) clearInterval(supportPollTimer);
      supportPollTimer = setInterval(pollSupportMessages, 2500);
      pollSupportMessages();
    }

    async function exitLiveSupportMode(notifyServer) {
      isLiveSupportMode = false;
      if (supportPollTimer) {
        clearInterval(supportPollTimer);
        supportPollTimer = null;
      }
      if (headerTitle) headerTitle.textContent = 'Hỗ trợ UTE Sport';
      if (statusDot) {
        statusDot.className = 'chatbot-status-dot';
      }
      if (statusText) {
        statusText.textContent = 'Tư vấn & Đặt sân trực tuyến';
      }
      if (callStaffBtn) callStaffBtn.style.display = 'inline-flex';
      if (leaveStaffBtn) leaveStaffBtn.style.display = 'none';
      if (inputField) inputField.placeholder = 'Hỏi chi nhánh, giờ chơi, tra cứu đơn...';

      if (notifyServer) {
        try {
          await fetch('/api/support/close', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: sessionId, closedBy: 'Khách hàng' })
          });
        } catch (e) {}
        appendSystemNotice('Đã kết thúc phiên trò chuyện với nhân viên. Trợ lý ảo UTE Sport đã sẵn sàng phục vụ tiếp!');
      }
    }

    async function pollSupportMessages() {
      if (!isLiveSupportMode) return;
      try {
        const res = await fetch(`/api/support/messages?sessionId=${encodeURIComponent(sessionId)}&afterId=${lastSupportMsgId}&role=customer`);
        if (!res.ok) return;
        const data = await res.json();

        if (data.ticket) {
          if (data.ticket.status === 'ACTIVE_STAFF') {
            if (statusDot) statusDot.className = 'chatbot-status-dot live';
            if (statusText) statusText.textContent = 'Nhân viên: ' + (data.ticket.assignedStaff || 'Trực ban');
          } else if (data.ticket.status === 'WAITING_STAFF') {
            if (statusDot) statusDot.className = 'chatbot-status-dot waiting';
            if (statusText) statusText.textContent = 'Đang chờ nhân viên tiếp nhận...';
          } else if (data.ticket.status === 'CLOSED') {
            exitLiveSupportMode(false);
            appendSystemNotice('Nhân viên đã kết thúc phiên hỗ trợ. Cảm ơn quý khách!');
            return;
          }
        }

        if (data.messages && data.messages.length > 0) {
          data.messages.forEach(msg => {
            if (msg.id > lastSupportMsgId) {
              lastSupportMsgId = msg.id;
              if (msg.sender === 'staff') {
                appendStaffMessage(msg);
              } else if (msg.sender === 'system') {
                appendSystemNotice(msg.content);
              }
            }
          });
        }
      } catch (err) {}
    }

    function appendStaffMessage(msg) {
      const bubble = document.createElement('div');
      bubble.className = 'chat-bubble staff';
      const staffName = escapeHtml(msg.staffName || 'Nhân viên trực');
      const timeStr = msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '';
      bubble.innerHTML = `
        <div class="chat-staff-badge">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
          Nhân viên: ${staffName}
        </div>
        <div class="chat-bubble-text">${formatBotText(msg.content || '')}</div>
        <div class="chat-msg-time">${timeStr}</div>
      `;
      msgList.appendChild(bubble);
      scrollToBottom();
    }

    function appendSystemNotice(text) {
      const notice = document.createElement('div');
      notice.className = 'chat-system-notice';
      notice.innerHTML = escapeHtml(text);
      msgList.appendChild(notice);
      scrollToBottom();
    }

    async function checkExistingSupportTicket() {
      try {
        const res = await fetch(`/api/support/messages?sessionId=${encodeURIComponent(sessionId)}&role=customer`);
        if (!res.ok) return;
        const data = await res.json();
        if (data.ticket && (data.ticket.status === 'WAITING_STAFF' || data.ticket.status === 'ACTIVE_STAFF')) {
          if (data.messages && data.messages.length > 0) {
            data.messages.forEach(m => {
              if (m.id > lastSupportMsgId) {
                lastSupportMsgId = m.id;
              }
            });
          }
          enterLiveSupportMode();
        }
      } catch (e) {}
    }

    // Restore state from sessionStorage without flickering
    const savedOpen = sessionStorage.getItem('chatbot_open') === 'true';
    if (savedOpen) {
      chatWin.classList.add('open');
      toggleBtn.classList.add('chatbot-hidden');
      if (container) container.classList.add('chatbot-open');
      isFirstOpen = false;
      if (msgList.children.length === 0) {
        sendMessage('');
      }
    }

    // Toggle logic
    function toggleChat() {
      const isOpen = chatWin.classList.toggle('open');
      sessionStorage.setItem('chatbot_open', isOpen ? 'true' : 'false');
      if (isOpen) {
        toggleBtn.classList.add('chatbot-hidden');
        if (container) container.classList.add('chatbot-open');
        inputField.focus();
        if (isFirstOpen && msgList.children.length === 0) {
          isFirstOpen = false;
          sendMessage('');
        }
        checkAndSuggestPendingBooking();
        checkExistingSupportTicket();
      } else {
        toggleBtn.classList.remove('chatbot-hidden');
        if (container) container.classList.remove('chatbot-open');
      }
    }

    function checkAndSuggestPendingBooking() {
      try {
        const last = JSON.parse(localStorage.getItem('ute_last_pending_booking') || 'null');
        if (!last || !last.bookingCode || !last.createdAt) return;
        const createdMs = new Date(last.createdAt).getTime();
        const rem = Math.floor((createdMs + 10 * 60 * 1000 - Date.now()) / 1000);
        if (rem > 0) {
          const existingBanner = document.getElementById('chat-pending-banner');
          if (!existingBanner) {
            const m = Math.floor(rem / 60);
            const s = rem % 60;
            const timeStr = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
            const banner = document.createElement('div');
            banner.id = 'chat-pending-banner';
            banner.style.cssText = 'background: #fef3c7; border: 1px solid #fde68a; border-radius: 10px; padding: 10px 12px; margin: 8px 12px; font-size: 12px; color: #92400e; display: flex; align-items: center; justify-content: space-between; gap: 8px; box-shadow: 0 2px 6px rgba(0,0,0,0.04);';
            banner.innerHTML = `
              <div>
                <strong>Bạn có đơn giữ chỗ ${escapeHtml(last.bookingCode)}</strong>
                <div style="font-size: 11px; color: #b45309;">Thời gian còn lại: <strong id="chat-banner-timer">${timeStr}</strong></div>
              </div>
              <button type="button" style="padding: 6px 12px; font-size: 11px; font-weight: 700; border-radius: 6px; white-space: nowrap; background: #059669; border: none; color: #fff; cursor: pointer;">
                Thanh Toán Ngay
              </button>
            `;
            banner.querySelector('button').addEventListener('click', () => {
              banner.remove();
              sendMessage(`tiếp tục thanh toán đơn ${last.bookingCode}`);
            });
            msgList.parentNode.insertBefore(banner, msgList);

            const bannerInterval = setInterval(() => {
              const currentRem = Math.floor((createdMs + 10 * 60 * 1000 - Date.now()) / 1000);
              const timerTxt = document.getElementById('chat-banner-timer');
              if (currentRem <= 0) {
                clearInterval(bannerInterval);
                if (timerTxt) timerTxt.textContent = '00:00 (Hết hạn)';
              } else if (timerTxt) {
                const bm = Math.floor(currentRem / 60);
                const bs = currentRem % 60;
                timerTxt.textContent = `${String(bm).padStart(2, '0')}:${String(bs).padStart(2, '0')}`;
              } else {
                clearInterval(bannerInterval);
              }
            }, 1000);
          }
        }
      } catch (e) {}
    }

    const activeChatTimers = {};
    function startChatPaymentTimer(bookingCode, createdAt) {
      if (activeChatTimers[bookingCode]) clearInterval(activeChatTimers[bookingCode]);
      const createdMs = createdAt ? new Date(createdAt).getTime() : Date.now();
      const expiresAt = createdMs + 10 * 60 * 1000;

      const update = () => {
        const rem = Math.floor((expiresAt - Date.now()) / 1000);
        const timerValEl = document.getElementById(`chat-timer-val-${bookingCode}`);
        if (!timerValEl) {
          clearInterval(activeChatTimers[bookingCode]);
          return;
        }
        if (rem <= 0) {
          clearInterval(activeChatTimers[bookingCode]);
          timerValEl.textContent = '00:00 (Hết hạn)';
          timerValEl.style.color = '#dc2626';
        } else {
          const m = Math.floor(rem / 60);
          const s = rem % 60;
          timerValEl.textContent = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
        }
      };

      update();
      activeChatTimers[bookingCode] = setInterval(update, 1000);
    }

    toggleBtn.addEventListener('click', toggleChat);
    closeBtn.addEventListener('click', () => {
      chatWin.classList.remove('open');
      toggleBtn.classList.remove('chatbot-hidden');
      if (container) container.classList.remove('chatbot-open');
      sessionStorage.setItem('chatbot_open', 'false');
    });

    if (callStaffBtn) {
      callStaffBtn.addEventListener('click', () => {
        sendMessage('Gặp nhân viên tư vấn');
      });
    }

    if (leaveStaffBtn) {
      leaveStaffBtn.addEventListener('click', () => {
        exitLiveSupportMode(true);
      });
    }

    // Reset Chat logic
    resetBtn.addEventListener('click', async () => {
      if (isLiveSupportMode) {
        await exitLiveSupportMode(true);
      }
      msgList.innerHTML = '';
      showTypingIndicator();
      try {
        await fetch('/api/chatbot/reset', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ sessionId: sessionId })
        });
      } catch (e) {
      }
      hideTypingIndicator();
      sendMessage('');
    });

    // Send Message
    async function sendMessage(text) {
      const cleanText = (text || '').trim();
      if (!cleanText && !isFirstOpen && msgList.children.length > 0) return;

      if (cleanText) {
        appendMessage('user', cleanText);
        inputField.value = '';
      }

      // Check if user is in human live support mode
      if (isLiveSupportMode) {
        const lower = cleanText.toLowerCase();
        if (lower.includes('ket thuc') || lower.includes('quay lai bot') || lower.includes('dung chat') || lower.includes('thoat chat')) {
          await exitLiveSupportMode(true);
          return;
        }

        const user = getUserInfo();
        try {
          await fetch('/api/support/message', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              sessionId: sessionId,
              content: cleanText,
              sender: 'customer',
              customerName: user ? (user.fullName || user.name) : 'Khách hàng',
              customerPhone: user ? user.phone : ''
            })
          });
        } catch (e) {
          console.error('Failed to send support message', e);
        }
        return;
      }

      showTypingIndicator();

      // Retrieve logged-in user and token if available
      const token = localStorage.getItem('jwt_token') || localStorage.getItem('utesport_token');
      let user = null;
      try {
        user = JSON.parse(localStorage.getItem('utesport_user') || 'null');
      } catch (e) {}

      const reqHeaders = { 'Content-Type': 'application/json' };
      if (token) {
        reqHeaders['Authorization'] = 'Bearer ' + token;
      }

      const payload = {
        message: text || '',
        sessionId: sessionId,
        fullName: user ? (user.fullName || user.name) : null,
        phone: user ? user.phone : null,
        customerCode: user ? user.customerCode : null
      };

      try {
        const res = await fetch('/api/chatbot/message', {
          method: 'POST',
          headers: reqHeaders,
          body: JSON.stringify(payload)
        });

        hideTypingIndicator();

        if (res.ok) {
          const data = await res.json();
          appendBotMessage(data);
        } else {
          appendMessage('bot', 'Hiện tại máy chủ đang bận xử lý. Bạn vui lòng thử lại sau giây lát hoặc liên hệ hotline 1900 6868.');
        }
      } catch (err) {
        hideTypingIndicator();
        appendMessage('bot', 'Đã có lỗi kết nối mạng. Bạn vui lòng thử lại.');
      }
    }

    function appendMessage(sender, text) {
      const bubble = document.createElement('div');
      bubble.className = `chat-bubble ${sender}`;
      bubble.innerHTML = `<div class="chat-bubble-text">${formatBotText(text)}</div>`;
      msgList.appendChild(bubble);
      scrollToBottom();
    }

    function appendBotMessage(data) {
      if (data.actionType === 'LIVE_CHAT_CONNECT') {
        enterLiveSupportMode();
      } else if (data.actionType === 'LIVE_CHAT_DISCONNECT') {
        exitLiveSupportMode(false);
      }

      const bubble = document.createElement('div');
      bubble.className = 'chat-bubble bot';

      let html = `<div class="chat-bubble-text">${formatBotText(data.text || '')}</div>`;

      // Render Structured Card for Booking Lookup
      if (data.actionType === 'LOOKUP_RESULT' && data.lookupData) {
        const lk = data.lookupData;
        const statusClass = lk.status && lk.status.includes('Đã cọc') ? 'confirmed' : (lk.status && lk.status.includes('hủy') ? 'cancelled' : 'pending');
        html += `
          <div class="chat-lookup-card">
            <div class="chat-lookup-header">
              <span>Đơn: <b>${escapeHtml(lk.bookingCode || '')}</b></span>
              <span class="chat-status-badge ${statusClass}">${escapeHtml(lk.status || '')}</span>
            </div>
            <div class="chat-pay-row"><span>Sân:</span><span class="chat-pay-val">${escapeHtml(lk.courtCode || '')}</span></div>
            <div class="chat-pay-row"><span>Cơ sở:</span><span class="chat-pay-val">${escapeHtml(lk.branchCode || '')}</span></div>
            <div class="chat-pay-row"><span>Ngày:</span><span class="chat-pay-val">${escapeHtml(lk.date || '')}</span></div>
            <div class="chat-pay-row"><span>Khung giờ:</span><span class="chat-pay-val">${escapeHtml(lk.timeSlot || '')}</span></div>
            <div class="chat-pay-row" style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #cbd5e1;">
              <span>Tổng tiền:</span><span class="chat-pay-val">${Number(lk.totalPrice || 0).toLocaleString('vi-VN')} đ</span>
            </div>
          </div>
        `;
      }

      // Render Embedded Payment Box if action is OPEN_PAYMENT
      if (data.actionType === 'OPEN_PAYMENT' && data.paymentData) {
        const p = data.paymentData;

        // Save pending booking code into localStorage so history page can reopen it
        try {
          let pending = JSON.parse(localStorage.getItem('ute_pending_bookings') || '[]');
          if (!pending.includes(p.bookingCode)) {
            pending.unshift(p.bookingCode);
            localStorage.setItem('ute_pending_bookings', JSON.stringify(pending.slice(0, 10)));
          }
          localStorage.setItem('ute_last_pending_booking', JSON.stringify({
            bookingCode: p.bookingCode,
            createdAt: p.createdAt || new Date().toISOString()
          }));
        } catch (e) {}

        html += `
          <div class="chat-payment-card">
            <div class="chat-pay-header" style="display: flex; align-items: center; justify-content: space-between;">
              <span style="font-weight: 700; color: #047857;">MÃ KHÓA SÂN GIỮ CHỖ</span>
              <span class="chat-status-badge pending" style="font-size: 10px;">Tạm khóa 10 phút</span>
            </div>

            <div style="background: linear-gradient(135deg, rgba(16,185,129,0.1) 0%, rgba(5,150,105,0.15) 100%); border: 1px dashed #10b981; border-radius: 8px; padding: 10px; margin-bottom: 10px; text-align: center;">
              <div style="font-size: 11px; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; font-weight: 600;">Mã Khóa Sân Của Bạn</div>
              <div style="font-size: 20px; font-weight: 800; color: #047857; letter-spacing: 1.5px; margin: 3px 0;">${escapeHtml(p.bookingCode || '')}</div>
              <div style="font-size: 11px; color: #059669;">Đang tạm khóa giữ chỗ thời gian thực trên hệ thống</div>
            </div>

            <div style="background: rgba(16,185,129,0.08); border: 1px solid rgba(16,185,129,0.25); border-radius: 8px; padding: 7px 12px; margin-bottom: 10px; display: flex; align-items: center; justify-content: space-between;">
              <span style="font-size: 11.5px; color: #047857; display: flex; align-items: center; gap: 5px; font-weight: 600;">
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                Giữ chỗ còn lại:
              </span>
              <strong id="chat-timer-val-${p.bookingCode}" style="font-size: 13.5px; font-family: monospace; color: #b45309; font-weight: 800;">10:00</strong>
            </div>

            <div class="chat-pay-row"><span>Sân thi đấu:</span><span class="chat-pay-val">${escapeHtml(p.courtName || '')}</span></div>
            <div class="chat-pay-row"><span>Khung giờ:</span><span class="chat-pay-val">${escapeHtml(p.timeSlot || '')} (${escapeHtml(p.bookingDate || '')})</span></div>
            <div class="chat-pay-row"><span>Cơ sở:</span><span class="chat-pay-val">${escapeHtml(p.branchName || '')}</span></div>
            ${p.additionalServices ? `<div class="chat-pay-row"><span>Dịch vụ đi kèm:</span><span class="chat-pay-val" style="font-size: 11px; max-width: 60%; text-align: right;">${escapeHtml(p.additionalServices)}</span></div>` : ''}
            <div class="chat-pay-row" style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #cbd5e1;">
              <span style="font-weight: 700;">Tiền cọc cần thanh toán (30%):</span>
              <span class="chat-pay-val highlight">${Number(p.depositAmount || 0).toLocaleString('vi-VN')} đ</span>
            </div>

            <div class="chat-qr-wrap">
              <img src="${p.qrUrl}" alt="VietQR Thanh Toán Cọc" class="chat-qr-img">
              <div style="font-size: 11px; color: #64748b; margin-top: 6px; line-height: 1.4;">
                ${escapeHtml(p.bankName)}<br>
                STK: <b>${escapeHtml(p.accountNo)}</b> • Tên: <b>${escapeHtml(p.accountName)}</b><br>
                Nội dung chuyển khoản: <b style="color: #059669;">CK ${escapeHtml(p.bookingCode)}</b>
              </div>
            </div>

            <button type="button" class="chat-pay-btn-submit" id="btn-confirm-pay-${p.bookingCode}" data-code="${p.bookingCode}">
              Xác nhận đã chuyển khoản cọc
            </button>

            <a href="/customer/history" class="chat-pay-btn-secondary" style="display: flex; align-items: center; justify-content: center; gap: 6px; padding: 8px 12px; margin-top: 8px; border-radius: 8px; background: #ecfdf5; border: 1px solid #10b981; color: #047857; font-size: 12px; font-weight: 700; text-decoration: none;">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              <span>Mở Trong Lịch Sử & Tiếp Tục Thanh Toán</span>
            </a>
          </div>
        `;

        setTimeout(() => {
          startChatPaymentTimer(p.bookingCode, p.createdAt);
        }, 50);
      }

      // Action Link
      if (data.actionUrl && data.actionType !== 'OPEN_PAYMENT') {
        html += `<a href="${data.actionUrl}" class="chat-action-btn">${escapeHtml(data.actionText || 'Xem chi tiết')}</a>`;
      }

      // Quick Replies
      if (data.quickReplies && data.quickReplies.length > 0) {
        html += `<div class="chat-quick-replies">`;
        data.quickReplies.forEach(qr => {
          html += `<button class="chat-quick-pill" data-query="${escapeHtml(qr)}">${escapeHtml(qr)}</button>`;
        });
        html += `</div>`;
      }

      bubble.innerHTML = html;
      msgList.appendChild(bubble);

      // Bind quick replies
      bubble.querySelectorAll('.chat-quick-pill').forEach(btn => {
        btn.addEventListener('click', function () {
          const query = this.getAttribute('data-query');
          sendMessage(query);
        });
      });

      // Bind Confirm Payment Button
      if (data.paymentData && data.paymentData.bookingCode) {
        const bCode = data.paymentData.bookingCode;
        const confBtn = bubble.querySelector(`#btn-confirm-pay-${bCode}`);
        if (confBtn) {
          confBtn.addEventListener('click', async function () {
            confBtn.disabled = true;
            confBtn.innerText = 'Đang kiểm tra giao dịch...';

            const token = localStorage.getItem('jwt_token') || localStorage.getItem('utesport_token');
            let user = null;
            try {
              user = JSON.parse(localStorage.getItem('utesport_user') || 'null');
            } catch (e) {}

            const payHeaders = { 'Content-Type': 'application/json' };
            if (token) {
              payHeaders['Authorization'] = 'Bearer ' + token;
            }

            const payBody = {
              amount: data.paymentData.depositAmount,
              paymentMethod: 'VietQR Pro',
              customerPhone: user ? user.phone : null,
              customerName: user ? (user.fullName || user.name) : null,
              customerCode: user ? user.customerCode : null
            };

            try {
              const payRes = await fetch(`/api/bookings/${bCode}/deposit`, {
                method: 'POST',
                headers: payHeaders,
                body: JSON.stringify(payBody)
              });
              if (payRes.ok) {
                confBtn.innerText = 'Đã thanh toán cọc thành công';
                confBtn.style.background = '#059669';

                try {
                  let pending = JSON.parse(localStorage.getItem('ute_pending_bookings') || '[]');
                  pending = pending.filter(c => c !== bCode);
                  localStorage.setItem('ute_pending_bookings', JSON.stringify(pending));
                  localStorage.removeItem('ute_last_pending_booking');
                  const banner = document.getElementById('chat-pending-banner');
                  if (banner) banner.remove();
                } catch (e) {}

                appendMessage('bot', `Hệ thống đã nhận được tiền cọc 30% cho mã đơn **${bCode}**. Sân của bạn đã được khóa cố định chính thức và đơn được lưu vào [Lịch Sử Đặt Sân](/customer/history). Bạn vui lòng đến trước 10 phút để nhận sân tại quầy lễ tân.`);
              } else {
                confBtn.disabled = false;
                confBtn.innerText = 'Xác nhận đã chuyển khoản cọc';
                appendMessage('bot', `Chưa thể xác thực giao dịch cho đơn ${bCode}. Bạn có thể xuất trình biên lai chuyển khoản tại quầy lễ tân khi đến sân.`);
              }
            } catch (e) {
              confBtn.disabled = false;
              confBtn.innerText = 'Xác nhận đã chuyển khoản cọc';
            }
          });
        }
      }

      scrollToBottom();
    }

    function showTypingIndicator() {
      if (document.getElementById('chatbot-typing-ind')) return;
      const ind = document.createElement('div');
      ind.id = 'chatbot-typing-ind';
      ind.className = 'chat-typing';
      ind.innerHTML = `
        <span class="typing-dot"></span>
        <span class="typing-dot"></span>
        <span class="typing-dot"></span>
      `;
      msgList.appendChild(ind);
      scrollToBottom();
    }

    function hideTypingIndicator() {
      const ind = document.getElementById('chatbot-typing-ind');
      if (ind) ind.remove();
    }

    function scrollToBottom() {
      msgList.scrollTop = msgList.scrollHeight;
    }

    function escapeHtml(str) {
      if (!str) return '';
      return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function formatBotText(str) {
      if (!str) return '';
      let clean = escapeHtml(str);
      // Format **bold**
      clean = clean.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
      // Format markdown link [text](url)
      clean = clean.replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" style="color: #059669; font-weight: 700; text-decoration: underline;" target="_self">$1</a>');
      // Format new lines
      clean = clean.replace(/\n/g, '<br>');
      return clean;
    }

    // Input Events
    sendBtn.addEventListener('click', () => {
      const val = inputField.value.trim();
      if (val) sendMessage(val);
    });

    inputField.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        const val = inputField.value.trim();
        if (val) sendMessage(val);
      }
    });

    inputField.addEventListener('focus', () => {
      setTimeout(scrollToBottom, 320);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initChatbot);
  } else {
    initChatbot();
  }
})();
