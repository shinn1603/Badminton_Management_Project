/**
 * UTE SPORT - GLOBAL COMPONENT & THEME CONTROLLER
 * Supports: Sidebar Toggle, Theme Toggle (Light/Dark), Tab Switching, Toast Alerts, Modal Controls, Device Detection
 */

// 0. DEVICE DETECTION & ADAPTIVE COMPACT MODE (PHONE vs DESKTOP)
(function detectDevice() {
  function checkDevice() {
    const ua = navigator.userAgent || navigator.vendor || window.opera || '';
    const isMobileUA = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile/i.test(ua);
    const isSmallScreen = window.innerWidth <= 768;
    const isTouch = ('ontouchstart' in window) || (navigator.maxTouchPoints > 0);
    const isMobile = isMobileUA || isSmallScreen;

    window.UTE_DEVICE = {
      isMobile: isMobile,
      isMobileUA: isMobileUA,
      isTouch: isTouch,
      screenWidth: window.innerWidth,
      screenHeight: window.innerHeight
    };

    const root = document.documentElement;
    if (isMobile) {
      root.classList.add('is-mobile');
      root.classList.remove('is-desktop');
      if (document.body) {
        document.body.classList.add('is-mobile');
        document.body.classList.remove('is-desktop');
      }
    } else {
      root.classList.add('is-desktop');
      root.classList.remove('is-mobile');
      if (document.body) {
        document.body.classList.add('is-desktop');
        document.body.classList.remove('is-mobile');
      }
    }
  }

  checkDevice();
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', checkDevice);
  }
  window.addEventListener('resize', checkDevice);
  window.addEventListener('orientationchange', checkDevice);
})();

// 0.1. Auto-load RBAC Role Guard & User Sync
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
  const sbBackdrop = document.getElementById('sidebarMobileBackdrop');
  if (sidebar) {
    if (window.innerWidth <= 768) {
      sidebar.classList.toggle('mobile-open');
      if (sbBackdrop) {
        sbBackdrop.classList.toggle('open', sidebar.classList.contains('mobile-open'));
      }
    } else {
      sidebar.classList.toggle('collapsed');
      const isCollapsed = sidebar.classList.contains('collapsed');
      localStorage.setItem('sidebar_collapsed', isCollapsed ? 'true' : 'false');
    }
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

// =========================================================================
// 8. MOBILE RESPONSIVE NAVIGATION SYSTEM (BOTTOM TAB BAR + OFF-CANVAS DRAWER)
// =========================================================================
(function initMobileNavSystem() {
  function setupMobileNav() {
    const p = window.location.pathname.toLowerCase();
    const isCustomerPage = p.startsWith('/customer/') || p === '/' || p.startsWith('/booking') || document.querySelector('.cust-navbar');
    const isAuthPage = p.startsWith('/customer/auth') || p.startsWith('/customer/login') || p === '/login';

    // 1. Customer Pages: Inject Mobile Bottom Navigation Bar & Mobile Off-Canvas Drawer
    if (isCustomerPage && !isAuthPage) {
      document.body.classList.add('has-mobile-nav');

      // 1.1 Mobile Bottom Navigation Bar
      if (!document.getElementById('mobileBottomNav')) {
        const bottomNav = document.createElement('nav');
        bottomNav.id = 'mobileBottomNav';
        bottomNav.className = 'mobile-bottom-nav';
        bottomNav.setAttribute('aria-label', 'Điều hướng dưới chân trang');

        const isHome = p === '/' || p === '/customer/home';
        const isBooking = p.startsWith('/customer/booking') || p.startsWith('/booking');
        const isTournaments = p.startsWith('/customer/tournaments');
        const isHistory = p.startsWith('/customer/history');
        const isProfile = p.startsWith('/customer/profile');

        bottomNav.innerHTML = `
          <a href="/customer/home" class="mob-nav-item ${isHome ? 'active' : ''}">
            <div class="mob-nav-icon">
              <svg class="svg-icon" viewBox="0 0 24 24"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>
            </div>
            <span class="mob-nav-label">Trang chủ</span>
          </a>
          <a href="/customer/tournaments" class="mob-nav-item ${isTournaments ? 'active' : ''}">
            <div class="mob-nav-icon">
              <svg class="svg-icon" viewBox="0 0 24 24"><path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"></path><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"></path><path d="M4 22h16"></path><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"></path><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"></path><path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"></path></svg>
            </div>
            <span class="mob-nav-label">Giải đấu</span>
          </a>
          <a href="/customer/booking" class="mob-nav-item mob-nav-booking ${isBooking ? 'active' : ''}">
            <div class="mob-nav-highlight-btn">
              <svg class="svg-icon" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line><line x1="12" y1="14" x2="12" y2="18"></line><line x1="10" y1="16" x2="14" y2="16"></line></svg>
            </div>
            <span class="mob-nav-label">Đặt sân</span>
          </a>
          <a href="/customer/history" class="mob-nav-item ${isHistory ? 'active' : ''}">
            <div class="mob-nav-icon">
              <svg class="svg-icon" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
            </div>
            <span class="mob-nav-label">Lịch sử</span>
          </a>
          <a href="/customer/profile" class="mob-nav-item ${isProfile ? 'active' : ''}">
            <div class="mob-nav-icon">
              <svg class="svg-icon" viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
            </div>
            <span class="mob-nav-label">Hội viên</span>
          </a>
        `;
        document.body.appendChild(bottomNav);
      }

      // 1.2 Hamburger Button in Top Navbar
      const navActions = document.querySelector('.cust-nav-actions');
      if (navActions && !document.getElementById('btnCustomerMenuOpen')) {
        const hamburgerBtn = document.createElement('button');
        hamburgerBtn.type = 'button';
        hamburgerBtn.id = 'btnCustomerMenuOpen';
        hamburgerBtn.className = 'cust-hamburger-btn';
        hamburgerBtn.title = 'Mở danh mục';
        hamburgerBtn.setAttribute('aria-label', 'Mở danh mục');
        hamburgerBtn.innerHTML = `
          <svg class="svg-icon" viewBox="0 0 24 24" style="width: 20px; height: 20px;">
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        `;
        hamburgerBtn.addEventListener('click', toggleCustomerDrawer);
        navActions.appendChild(hamburgerBtn);
      }

      // 1.3 Off-Canvas Slide Drawer
      if (!document.getElementById('mobileNavDrawer')) {
        const backdrop = document.createElement('div');
        backdrop.id = 'mobileDrawerBackdrop';
        backdrop.className = 'mobile-drawer-backdrop';
        backdrop.addEventListener('click', closeCustomerDrawer);
        document.body.appendChild(backdrop);

        const drawer = document.createElement('aside');
        drawer.id = 'mobileNavDrawer';
        drawer.className = 'mobile-nav-drawer';

        let user = null;
        try {
          const raw = localStorage.getItem('utesport_user');
          if (raw) user = JSON.parse(raw);
        } catch (e) {}

        const userSectionHtml = user && user.fullName ? `
          <div class="drawer-user-card">
            <div class="drawer-user-avatar">${(user.fullName || 'KH').substring(0, 2).toUpperCase()}</div>
            <div class="drawer-user-info">
              <div class="drawer-user-name">${user.fullName}</div>
              <div class="drawer-user-tier">${user.tier || 'HỘI VIÊN TIÊU CHUẨN'}</div>
            </div>
          </div>
          <div class="drawer-user-quick-actions">
            <a href="/customer/profile" class="drawer-quick-btn">
              <svg class="svg-icon" viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
              <span>Hồ sơ & Điểm</span>
            </a>
            <button type="button" class="drawer-quick-btn danger" onclick="if(window.RBAC && window.RBAC.logoutCustomer) window.RBAC.logoutCustomer(); else { localStorage.clear(); window.location.href='/customer/auth'; }">
              <svg class="svg-icon" viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
              <span>Đăng xuất</span>
            </button>
          </div>
        ` : `
          <div class="drawer-guest-card">
            <div class="drawer-guest-title">Chào mừng quý khách!</div>
            <div class="drawer-guest-sub">Đăng nhập để nhận nhiều ưu đãi và tích điểm thành viên.</div>
            <a href="/customer/auth" class="drawer-login-btn">
              <svg class="svg-icon" viewBox="0 0 24 24"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path><polyline points="10 17 15 12 10 7"></polyline><line x1="15" y1="12" x2="3" y2="12"></line></svg>
              <span>Đăng Nhập / Đăng Ký</span>
            </a>
          </div>
        `;

        const isHome = p === '/' || p === '/customer/home';
        const isBooking = p.startsWith('/customer/booking') || p.startsWith('/booking');
        const isTournaments = p.startsWith('/customer/tournaments');
        const isHistory = p.startsWith('/customer/history');
        const isProfile = p.startsWith('/customer/profile');

        drawer.innerHTML = `
          <div class="drawer-header">
            <div class="drawer-brand">
              <div class="drawer-brand-icon">
                <svg class="svg-icon" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"></circle><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"></path><path d="M2 12h20"></path></svg>
              </div>
              <div>
                <div class="drawer-brand-title">UTE SPORT</div>
                <div class="drawer-brand-sub">Hệ Thống Cầu Lông</div>
              </div>
            </div>
            <button type="button" class="drawer-close-btn" id="btnCustomerDrawerClose" aria-label="Đóng menu">
              <svg class="svg-icon" viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            </button>
          </div>

          <div class="drawer-body">
            ${userSectionHtml}

            <div class="drawer-nav-section">
              <div class="drawer-section-title">DANH MỤC TRẢI NGHIỆM</div>
              <nav class="drawer-nav-list">
                <a href="/customer/home" class="drawer-nav-link ${isHome ? 'active' : ''}">
                  <span class="drawer-nav-icon"><svg class="svg-icon" viewBox="0 0 24 24"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg></span>
                  <span>Trang chủ</span>
                  <svg class="drawer-chevron" viewBox="0 0 24 24"><polyline points="9 18 15 12 9 6"></polyline></svg>
                </a>
                <a href="/customer/booking" class="drawer-nav-link ${isBooking ? 'active' : ''}">
                  <span class="drawer-nav-icon"><svg class="svg-icon" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg></span>
                  <span>Đặt sân trực tuyến</span>
                  <span class="drawer-badge-hot">HOT</span>
                  <svg class="drawer-chevron" viewBox="0 0 24 24"><polyline points="9 18 15 12 9 6"></polyline></svg>
                </a>
                <a href="/customer/tournaments" class="drawer-nav-link ${isTournaments ? 'active' : ''}">
                  <span class="drawer-nav-icon"><svg class="svg-icon" viewBox="0 0 24 24"><path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"></path><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"></path><path d="M4 22h16"></path><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"></path><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"></path><path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"></path></svg></span>
                  <span>Giải đấu & Sự kiện</span>
                  <svg class="drawer-chevron" viewBox="0 0 24 24"><polyline points="9 18 15 12 9 6"></polyline></svg>
                </a>
                <a href="/customer/history" class="drawer-nav-link ${isHistory ? 'active' : ''}">
                  <span class="drawer-nav-icon"><svg class="svg-icon" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg></span>
                  <span>Lịch sử đặt sân</span>
                  <svg class="drawer-chevron" viewBox="0 0 24 24"><polyline points="9 18 15 12 9 6"></polyline></svg>
                </a>
                <a href="/customer/profile" class="drawer-nav-link ${isProfile ? 'active' : ''}">
                  <span class="drawer-nav-icon"><svg class="svg-icon" viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg></span>
                  <span>Hội viên & Điểm thưởng</span>
                  <svg class="drawer-chevron" viewBox="0 0 24 24"><polyline points="9 18 15 12 9 6"></polyline></svg>
                </a>
                <a href="/customer/policy" class="drawer-nav-link ${p.startsWith('/customer/policy') ? 'active' : ''}">
                  <span class="drawer-nav-icon"><svg class="svg-icon" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg></span>
                  <span>Bảng giá & Chính sách</span>
                  <svg class="drawer-chevron" viewBox="0 0 24 24"><polyline points="9 18 15 12 9 6"></polyline></svg>
                </a>
              </nav>
            </div>

            <div class="drawer-support-box">
              <div class="drawer-support-title">HỖ TRỢ TRỰC TIẾP</div>
              <div class="drawer-support-item">
                <svg class="svg-icon" viewBox="0 0 24 24"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path></svg>
                <div>
                  <div class="drawer-support-label">Hotline 24/7:</div>
                  <a href="tel:0987654321" class="drawer-support-phone">0987.654.321</a>
                </div>
              </div>
              <div class="drawer-support-hours">Giờ phục vụ: 05:00 - 23:00 hàng ngày</div>
            </div>
          </div>

          <div class="drawer-footer">
            <button type="button" class="drawer-theme-toggle" onclick="toggleTheme()">
              <svg class="svg-icon" viewBox="0 0 24 24"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line></svg>
              <span>Đổi giao diện Sáng / Tối</span>
            </button>
          </div>
        `;

        document.body.appendChild(drawer);
        document.getElementById('btnCustomerDrawerClose').addEventListener('click', closeCustomerDrawer);
      }
    }

    // 2. Internal / Management Pages: Sidebar Off-Canvas Support on Mobile
    const sidebar = document.getElementById('appSidebar') || document.querySelector('.sidebar');
    if (sidebar) {
      if (!document.getElementById('sidebarMobileBackdrop')) {
        const sbBackdrop = document.createElement('div');
        sbBackdrop.id = 'sidebarMobileBackdrop';
        sbBackdrop.className = 'sidebar-mobile-backdrop';
        sbBackdrop.addEventListener('click', () => {
          sidebar.classList.remove('mobile-open');
          sbBackdrop.classList.remove('open');
        });
        document.body.appendChild(sbBackdrop);
      }

      sidebar.querySelectorAll('.menu-item, a').forEach(link => {
        link.addEventListener('click', () => {
          if (window.innerWidth <= 768) {
            sidebar.classList.remove('mobile-open');
            const sbBackdrop = document.getElementById('sidebarMobileBackdrop');
            if (sbBackdrop) sbBackdrop.classList.remove('open');
          }
        });
      });

      document.querySelectorAll('.btn-sidebar-toggle, #btnToggleSidebar, [onclick*="toggleSidebar"]').forEach(btn => {
        btn.onclick = function (e) {
          e.preventDefault();
          e.stopPropagation();
          toggleSidebar();
        };
      });
    }
  }

  function toggleCustomerDrawer() {
    const drawer = document.getElementById('mobileNavDrawer');
    const backdrop = document.getElementById('mobileDrawerBackdrop');
    if (!drawer) return;
    const isOpen = drawer.classList.contains('open');
    if (isOpen) {
      closeCustomerDrawer();
    } else {
      drawer.classList.add('open');
      if (backdrop) backdrop.classList.add('open');
      document.body.style.overflow = 'hidden';
    }
  }

  function closeCustomerDrawer() {
    const drawer = document.getElementById('mobileNavDrawer');
    const backdrop = document.getElementById('mobileDrawerBackdrop');
    if (drawer) drawer.classList.remove('open');
    if (backdrop) backdrop.classList.remove('open');
    document.body.style.overflow = '';
  }

  window.toggleCustomerDrawer = toggleCustomerDrawer;
  window.closeCustomerDrawer = closeCustomerDrawer;
  window.toggleSidebar = toggleSidebar;

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', setupMobileNav);
  } else {
    setupMobileNav();
  }
})();


