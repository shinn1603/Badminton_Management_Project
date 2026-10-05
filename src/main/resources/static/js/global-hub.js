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

// 2. Sidebar Toggle
function toggleSidebar() {
  const sidebar = document.getElementById('appSidebar') || document.querySelector('.sidebar');
  if (sidebar) {
    sidebar.classList.toggle('collapsed');
    const isCollapsed = sidebar.classList.contains('collapsed');
    localStorage.setItem('sidebar_collapsed', isCollapsed ? 'true' : 'false');
  }
}

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

// 5. Toast Notification System
function showToast(message, type = 'success') {
  let container = document.getElementById('toastContainer');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  
  const icon = type === 'success' ? '✔' : type === 'error' ? '✖' : 'ℹ';
  toast.innerHTML = `
    <span style="font-weight: 700; color: var(--primary);">${icon}</span>
    <span>${message}</span>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}
