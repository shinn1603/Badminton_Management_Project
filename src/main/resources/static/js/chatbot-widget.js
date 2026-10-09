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
      <div id="ute-chatbot-toggle" title="Trợ lý ảo tư vấn và đặt sân">
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
              <div class="chatbot-title">Trợ lý ảo UTE Sport</div>
              <div class="chatbot-status">
                <span class="chatbot-status-dot"></span>
                <span>Tư vấn & Đặt sân tự động</span>
              </div>
            </div>
          </div>
          <div class="chatbot-header-actions">
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
    const chatWin = document.getElementById('ute-chatbot-window');
    const msgList = document.getElementById('chatbot-messages-list');
    const inputField = document.getElementById('chatbot-input-field');
    const sendBtn = document.getElementById('chatbot-send-btn');

    let isFirstOpen = true;

    // Restore state from sessionStorage without flickering
    const savedOpen = sessionStorage.getItem('chatbot_open') === 'true';
    if (savedOpen) {
      chatWin.classList.add('open');
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
        inputField.focus();
        if (isFirstOpen && msgList.children.length === 0) {
          isFirstOpen = false;
          sendMessage('');
        }
      }
    }

    toggleBtn.addEventListener('click', toggleChat);
    closeBtn.addEventListener('click', () => {
      chatWin.classList.remove('open');
      sessionStorage.setItem('chatbot_open', 'false');
    });

    // Reset Chat logic
    resetBtn.addEventListener('click', async () => {
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
      if (text && text.trim()) {
        appendMessage('user', text.trim());
        inputField.value = '';
      }

      showTypingIndicator();

      try {
        const res = await fetch('/api/chatbot/message', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ message: text || '', sessionId: sessionId })
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
        html += `
          <div class="chat-payment-card">
            <div class="chat-pay-header">Cổng thanh toán tiền cọc 30% VietQR</div>
            <div class="chat-pay-row"><span>Mã đơn đặt:</span><span class="chat-pay-val">${escapeHtml(p.bookingCode || '')}</span></div>
            <div class="chat-pay-row"><span>Sân thi đấu:</span><span class="chat-pay-val">${escapeHtml(p.courtName || '')}</span></div>
            <div class="chat-pay-row"><span>Khung giờ:</span><span class="chat-pay-val">${escapeHtml(p.timeSlot || '')}</span></div>
            <div class="chat-pay-row"><span>Cơ sở:</span><span class="chat-pay-val">${escapeHtml(p.branchName || '')}</span></div>
            <div class="chat-pay-row" style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #cbd5e1;">
              <span style="font-weight: 700;">Tiền cọc cần thanh toán:</span>
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
          </div>
        `;
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
            try {
              const payRes = await fetch(`/api/bookings/${bCode}/deposit`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ amount: data.paymentData.depositAmount, paymentMethod: 'VietQR Pro' })
              });
              if (payRes.ok) {
                confBtn.innerText = 'Đã thanh toán cọc thành công';
                confBtn.style.background = '#059669';
                appendMessage('bot', `Hệ thống đã nhận được tiền cọc 30% cho mã đơn ${bCode}. Sân của bạn đã được khóa cố định. Bạn vui lòng đến trước 10 phút để nhận sân tại quầy lễ tân.`);
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
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initChatbot);
  } else {
    initChatbot();
  }
})();
