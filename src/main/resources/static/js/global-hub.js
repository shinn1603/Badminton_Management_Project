/**
 * UTE SPORT - GLOBAL COMPONENT & THEME CONTROLLER
 * Supports: Sidebar Toggle, Theme Toggle (Light/Dark), Tab Switching, Toast Alerts, Modal Controls
 */

// 0. Auto-load RBAC Role Guard & User Sync
(function () {
  if (!window.RBAC && !document.querySelector('script[src*="auth-guard.js"]')) {
    const s = document.createElement('script');
    s.src = '/js/auth-guard.js';
    document.head.appendChild(s);
  }

  // Auto-load AI Chatbot widget on customer browsing pages (exclude auth / login screens)
  const p = window.location.pathname.toLowerCase();
  const isAuthPage = p.startsWith('/customer/auth') || p.startsWith('/customer/login') || p === '/login';
  if (!isAuthPage && (p.startsWith('/customer/') || p === '/' || p.startsWith('/booking')) && !document.querySelector('script[src*="chatbot-widget.js"]')) {
    const cb = document.createElement('script');
    cb.src = '/js/chatbot-widget.js';
    document.head.appendChild(cb);
  }
})();

// 1. Theme Management (Light / Dark)
function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'light';
  const next = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem('theme', next);
  updateThemeIcon(next);
}

function updateThemeIcon(theme) {
  const sunIcon = document.getElementById('themeIconSun');
  if (sunIcon) {
    if (theme === 'dark') {
      sunIcon.innerHTML = '<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>';
    } else {
      sunIcon.innerHTML = '<circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>';
    }
  }
}

// Initialize theme on load
(function initTheme() {
  const saved = localStorage.getItem('theme') || 'light';
  document.documentElement.setAttribute('data-theme', saved);
  document.addEventListener('DOMContentLoaded', () => {
    updateThemeIcon(saved);
  });
})();

// 2. Sidebar Toggle & State
function toggleSidebar() {
  const sidebar = document.getElementById('appSidebar') || document.querySelector('.sidebar');
  if (sidebar) {
    sidebar.classList.toggle('collapsed');
    const isCollapsed = sidebar.classList.contains('collapsed');
    localStorage.setItem('sidebar_collapsed', isCollapsed ? 'true' : 'false');
  }
}

// 2.1 Sidebar Scroll Memory & Active Route Synchronization
(function initSidebarScrollAndActive() {
  function getScrollContainer() {
    return document.querySelector('.sidebar-content') || document.getElementById('sidebarContent') || document.getElementById('appSidebar') || document.querySelector('.sidebar');
  }

  function restoreScroll() {
    const sc = getScrollContainer();
    if (!sc) return;
    const saved = sessionStorage.getItem('sidebar_scroll_top');
    if (saved !== null) {
      sc.scrollTop = parseInt(saved, 10);
    }
  }

  function saveScroll() {
    const sc = getScrollContainer();
    if (sc) {
      sessionStorage.setItem('sidebar_scroll_top', sc.scrollTop);
    }
  }

  function syncActiveMenuItem() {
    const currentPath = window.location.pathname.toLowerCase();
    const currentHash = window.location.hash.toLowerCase();
    const menuItems = document.querySelectorAll('.sidebar .menu-item');
    if (!menuItems || menuItems.length === 0) return;

    let matchedItem = null;
    menuItems.forEach(item => {
      const href = (item.getAttribute('href') || '').toLowerCase();
      if (!href) return;
      if (currentHash && href.includes(currentHash)) {
        matchedItem = item;
      } else if (!matchedItem && href.split('#')[0] === currentPath) {
        matchedItem = item;
      }
    });

    if (matchedItem) {
      menuItems.forEach(i => i.classList.remove('active'));
      matchedItem.classList.add('active');
    }
  }

  document.addEventListener('click', function (e) {
    const a = e.target.closest('a');
    if (a && a.closest('.sidebar')) {
      saveScroll();
    }
  });

  window.addEventListener('beforeunload', saveScroll);

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      syncActiveMenuItem();
      restoreScroll();
      setTimeout(restoreScroll, 60);
    });
  } else {
    syncActiveMenuItem();
    restoreScroll();
    setTimeout(restoreScroll, 60);
  }
})();

// 3. Tab Switching
function switchTab(tabId, btnElement) {
  const container = btnElement.closest('.section-card') || document;
  
  // Update buttons
  const buttons = container.querySelectorAll('.nav-tab-btn, .pill-btn');
  buttons.forEach(btn => btn.classList.remove('active'));
  if (btnElement) {
    btnElement.classList.add('active');
  }

  // Update panes
  const panes = container.querySelectorAll('.tab-pane');
  panes.forEach(pane => pane.classList.remove('active'));
  
  const targetPane = document.getElementById(tabId);
  if (targetPane) {
    targetPane.classList.add('active');
  }
}

// 4. Period Pill Selection
function selectPeriod(btnElement, period) {
  const group = btnElement.closest('.pill-group') || btnElement.parentElement;
  if (group) {
    group.querySelectorAll('.pill-btn').forEach(b => b.classList.remove('active'));
    btnElement.classList.add('active');
  }
  showToast(`Đã lọc báo cáo theo: ${btnElement.innerText.trim()}`, 'info');
}

// 5. Toast & In-Modal Prominent Notification System
const activeToastMessages = new Set();
function showToast(message, type = 'success') {
  if (!message) return;
  const cleanMsg = message.trim();
  if (activeToastMessages.has(cleanMsg)) {
    return;
  }
  activeToastMessages.add(cleanMsg);
  setTimeout(() => activeToastMessages.delete(cleanMsg), 2500);

  let container = document.getElementById('toastContainer');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  container.style.zIndex = '999999';

  while (container.children.length >= 2) {
    container.removeChild(container.firstChild);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  
  const icon = type === 'success' ? '✔' : type === 'error' ? '✖' : type === 'warning' ? '⚠' : 'ℹ';
  toast.innerHTML = `
    <span style="font-weight: 800; font-size: 15px; color: ${type === 'error' || type === 'warning' ? '#ef4444' : 'var(--primary)'};">${icon}</span>
    <span style="flex: 1; line-height: 1.4;">${message}</span>
  `;

  container.appendChild(toast);

  // If a modal dialog is currently open, inject an alert directly into the active modal
  const activeModal = document.querySelector('.modal-overlay.active, .modal-overlay.open, .slot-modal-overlay.active, .modal.show, [class*="modal"][class*="active"], [class*="modal"][class*="open"]');
  if (activeModal && (type === 'warning' || type === 'error')) {
    const targetCard = activeModal.querySelector('.modal-body, .modal-card, .modal-container, .vietqr-modal-card') || activeModal;
    let inlineAlert = targetCard.querySelector('.modal-inline-alert');
    if (!inlineAlert) {
      inlineAlert = document.createElement('div');
      inlineAlert.className = `modal-inline-alert ${type}`;
      targetCard.insertBefore(inlineAlert, targetCard.firstChild);
    } else {
      inlineAlert.className = `modal-inline-alert ${type}`;
    }
    inlineAlert.innerHTML = `
      <span style="font-size: 14px; font-weight: 800;">⚠</span>
      <span style="flex: 1;">${message}</span>
    `;

    // Highlight any empty required input inside the modal
    const emptyInputs = targetCard.querySelectorAll('input:required, input[placeholder*="*"], input[type="text"], input[type="tel"]');
    emptyInputs.forEach(inp => {
      if (!inp.value.trim()) {
        inp.classList.add('input-error-highlight');
        inp.focus();
        setTimeout(() => inp.classList.remove('input-error-highlight'), 3500);
      }
    });

    setTimeout(() => {
      if (inlineAlert && inlineAlert.parentNode) {
        inlineAlert.style.opacity = '0';
        inlineAlert.style.transition = 'opacity 0.3s ease';
        setTimeout(() => inlineAlert.remove(), 300);
      }
    }, 4500);
  }

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(-10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 3800);
}

// ==========================================
// 6. REAL-TIME WEBSOCKET HUB (STOMP OVER WS)
// Subscribes to /topic/bookings, /topic/notifications, /topic/courts
// Broadcasts live toast alerts and dispatches DOM events
// ==========================================
(function initRealtimeWebSocketHub() {
  let ws = null;
  let heartbeatTimer = null;
  let reconnectTimer = null;
  let isConnected = false;

  function connect() {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      return;
    }

    try {
      const isHttps = window.location.protocol === 'https:';
      const wsProto = isHttps ? 'wss:' : 'ws:';
      const host = window.location.host;
      if (!host) return;

      const wsUrl = `${wsProto}//${host}/ws-court/websocket`;
      ws = new WebSocket(wsUrl);

      ws.onopen = function () {
        const connectFrame = "CONNECT\naccept-version:1.2,1.1,1.0\nheart-beat:10000,10000\n\n\0";
        ws.send(connectFrame);
      };

      ws.onmessage = function (event) {
        const data = event.data;
        if (!data) return;

        if (data === '\n' || data === '\r\n') return;

        if (data.startsWith('CONNECTED')) {
          isConnected = true;

          ws.send("SUBSCRIBE\nid:sub-bookings\ndestination:/topic/bookings\n\n\0");
          ws.send("SUBSCRIBE\nid:sub-notifications\ndestination:/topic/notifications\n\n\0");
          ws.send("SUBSCRIBE\nid:sub-courts\ndestination:/topic/courts\n\n\0");

          clearInterval(heartbeatTimer);
          heartbeatTimer = setInterval(() => {
            if (ws && ws.readyState === WebSocket.OPEN) {
              ws.send('\n');
            }
          }, 10000);

        } else if (data.startsWith('MESSAGE')) {
          const bodyIndex = data.indexOf('\n\n');
          if (bodyIndex !== -1) {
            let body = data.substring(bodyIndex + 2);
            if (body.endsWith('\0')) {
              body = body.substring(0, body.length - 1);
            }
            try {
              const payload = JSON.parse(body);
              handleIncomingNotification(payload);
            } catch (err) {
              // Non-JSON payload
            }
          }
        }
      };

      ws.onclose = function () {
        isConnected = false;
        clearInterval(heartbeatTimer);
        scheduleReconnect();
      };

      ws.onerror = function () {
        isConnected = false;
      };
    } catch (e) {
      scheduleReconnect();
    }
  }

  function scheduleReconnect() {
    clearTimeout(reconnectTimer);
    reconnectTimer = setTimeout(connect, 6000);
  }

  const recentEventKeys = new Set();

  function handleIncomingNotification(payload) {
    if (!payload) return;

    // Deduplicate identical events received across multiple topic subscriptions (/topic/bookings, /topic/notifications)
    const eventKey = `${payload.eventType || ''}_${payload.bookingCode || ''}_${payload.message || ''}`;
    if (recentEventKeys.has(eventKey)) {
      return;
    }
    recentEventKeys.add(eventKey);
    setTimeout(() => recentEventKeys.delete(eventKey), 5000);

    window.dispatchEvent(new CustomEvent('badminton:booking-event', { detail: payload }));
    window.dispatchEvent(new CustomEvent('badminton:notification', { detail: payload }));

    // If this booking or action was performed directly in this active tab, skip the duplicate broadcast toast
    if (window.lastActionBookingCode && payload.bookingCode && window.lastActionBookingCode === payload.bookingCode) {
      return;
    }

    const path = window.location.pathname.toLowerCase();
    const isDashboardOrPos = path.startsWith('/manager') || path.startsWith('/pos') || path.startsWith('/director');

    if (payload.message) {
      if (isDashboardOrPos) {
        const toastType = payload.eventType === 'BOOKING_CANCELLED' ? 'warning' : 'success';
        showToast(payload.message, toastType);
      }
    } else if (payload.bookingCode) {
      if (isDashboardOrPos) {
        showToast(`Đơn đặt sân mới [${payload.bookingCode}] - Sân ${payload.courtCode || ''}`, 'success');
      }
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      setTimeout(connect, 600);
    });
  } else {
    setTimeout(connect, 600);
  }

  window.addEventListener('beforeunload', () => {
    clearTimeout(reconnectTimer);
    clearInterval(heartbeatTimer);
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      try {
        ws.send("DISCONNECT\n\n\0");
        ws.close();
      } catch (e) {}
    }
  });
})();

