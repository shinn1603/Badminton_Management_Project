/**
 * UTE SPORT - RBAC ROLE GUARD & DYNAMIC USER PROFILE
 * Manages role-based access control, protects routes, and dynamically
 * synchronizes user profile across all screens without hardcoded templates.
 */

(function () {
  'use strict';

  // Role permissions definitions
  const ROLE_CONFIG = {
    POS: {
      roleTitle: 'Thu ngân POS',
      defaultName: 'Nguyễn Thanh Tâm',
      branchCode: 'CN01',
      branchName: 'CN Thủ Đức',
      allowedPrefixes: ['/pos/', '/booking/grid', '/login'],
      prohibitedPrefixes: ['/manager/', '/director/', '/admin/'],
      homeUrl: '/pos/grid',
      deniedMessage: 'Tài khoản Thu ngân (POS) không có quyền truy cập phân hệ Quản trị / Quản lý!'
    },
    MANAGER: {
      roleTitle: 'Quản lý chi nhánh',
      defaultName: 'Trần Phúc Bảo',
      branchCode: 'CN01',
      branchName: 'CN Thủ Đức',
      allowedPrefixes: ['/manager/', '/pos/grid', '/booking/grid', '/login'],
      prohibitedPrefixes: ['/director/', '/admin/'],
      homeUrl: '/manager/dashboard',
      deniedMessage: 'Tài khoản Quản lý chi nhánh không có quyền truy cập phân hệ Giám đốc / Quản trị!'
    },
    DIRECTOR: {
      roleTitle: 'Giám đốc chuỗi',
      defaultName: 'Nguyễn Phước Thọ',
      branchCode: 'ALL',
      branchName: 'Toàn hệ thống',
      allowedPrefixes: ['/director/', '/admin/', '/manager/', '/pos/', '/booking/grid', '/login'],
      prohibitedPrefixes: [],
      homeUrl: '/director/dashboard',
      deniedMessage: 'Truy cập bị từ chối!'
    },
    ADMIN: {
      roleTitle: 'Quản trị hệ thống',
      defaultName: 'Trần Biểu Hương',
      branchCode: 'ALL',
      branchName: 'Trung tâm kỹ thuật',
      allowedPrefixes: ['/admin/', '/director/', '/manager/', '/pos/', '/booking/grid', '/login'],
      prohibitedPrefixes: [],
      homeUrl: '/admin/users',
      deniedMessage: 'Truy cập bị từ chối!'
    },
    CUSTOMER: {
      roleTitle: 'Hội viên trực tuyến',
      defaultName: 'Lê Bá Đạt',
      branchCode: 'CN01',
      branchName: 'CN Thủ Đức',
      allowedPrefixes: ['/customer/', '/', '/login'],
      prohibitedPrefixes: ['/pos/', '/manager/', '/director/', '/admin/'],
      homeUrl: '/customer/home',
      deniedMessage: 'Khu vực nội bộ dành riêng cho nhân sự quản lý sân!'
    }
  };

  // Helper to generate initials from full name
  function getInitials(name) {
    if (!name) return 'US';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[parts.length - 2][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  // 1. Resolve Current User
  function getCurrentUser() {
    let raw = localStorage.getItem('utesport_user');
    let user = null;
    if (raw) {
      try {
        user = JSON.parse(raw);
      } catch (e) {
        user = null;
      }
    }

    const currentPath = window.location.pathname.toLowerCase();

    // If no user in localStorage, default according to current context
    if (!user || !user.role) {
      if (currentPath.startsWith('/pos/') || currentPath === '/booking/grid') {
        user = { username: 'pos', role: 'POS' };
      } else if (currentPath.startsWith('/manager/')) {
        user = { username: 'manager', role: 'MANAGER' };
      } else if (currentPath.startsWith('/director/')) {
        user = { username: 'director', role: 'DIRECTOR' };
      } else if (currentPath.startsWith('/admin/')) {
        user = { username: 'admin', role: 'ADMIN' };
      } else if (currentPath.startsWith('/customer/')) {
        user = { username: 'customer', role: 'CUSTOMER' };
      } else {
        user = { username: 'director', role: 'DIRECTOR' };
      }
    }

    const cfg = ROLE_CONFIG[user.role] || ROLE_CONFIG.DIRECTOR;
    if (!user.fullName) user.fullName = cfg.defaultName;
    if (!user.roleTitle) user.roleTitle = cfg.roleTitle;
    if (!user.branchName) user.branchName = cfg.branchName;
    if (!user.branchCode) user.branchCode = cfg.branchCode;

    // Persist normalized user
    localStorage.setItem('utesport_user', JSON.stringify(user));
    return user;
  }

  const currentUser = getCurrentUser();
  const currentRoleCfg = ROLE_CONFIG[currentUser.role] || ROLE_CONFIG.DIRECTOR;
  const currentPath = window.location.pathname;

  // 2. Route Guard Enforcement: Check if user is navigating into a forbidden path
  for (const prohibited of currentRoleCfg.prohibitedPrefixes) {
    if (currentPath.startsWith(prohibited)) {
      console.warn(`[RBAC] Access denied: User ${currentUser.username} (${currentUser.role}) attempted to access ${currentPath}`);
      sessionStorage.setItem('rbac_denied_msg', currentRoleCfg.deniedMessage);
      window.location.replace(currentRoleCfg.homeUrl);
      return; // Stop further execution
    }
  }

  // 3. UI Synchronization on DOM Ready
  function syncUserProfileUI() {
    const initials = getInitials(currentUser.fullName);
    const branchLabel = currentUser.branchName || currentUser.branchCode || 'CN Thủ Đức';
    const titleLabel = `${currentUser.roleTitle} • ${branchLabel}`;

    // Update avatar elements
    document.querySelectorAll('.user-avatar, #sidebarAvatar').forEach(el => {
      el.textContent = initials;
      el.title = `${currentUser.fullName} (${currentUser.roleTitle})`;
    });

    // Update name elements
    document.querySelectorAll('.user-name, #sidebarName').forEach(el => {
      el.textContent = currentUser.fullName;
    });

    // Update title / role elements
    document.querySelectorAll('.user-title, #sidebarTitle').forEach(el => {
      el.textContent = titleLabel;
    });

    // Show flash denied message if redirected
    const deniedMsg = sessionStorage.getItem('rbac_denied_msg');
    if (deniedMsg) {
      sessionStorage.removeItem('rbac_denied_msg');
      setTimeout(() => {
        if (typeof showToast === 'function') {
          showToast(deniedMsg, 'error');
        } else {
          alert(deniedMsg);
        }
      }, 300);
    }

    // Intercept sidebar / menu navigation links to prevent prohibited jumps
    document.querySelectorAll('a[href]').forEach(a => {
      const href = a.getAttribute('href');
      if (!href || href.startsWith('#') || href.startsWith('javascript:')) return;

      // Check if href matches any prohibited prefix
      for (const prohibited of currentRoleCfg.prohibitedPrefixes) {
        if (href.startsWith(prohibited)) {
          a.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            const msg = `Quyền hạn '${currentUser.roleTitle}' không được phép truy cập chức năng này!`;
            if (typeof showToast === 'function') {
              showToast(msg, 'error');
            } else {
              alert(msg);
            }
          });
          break;
        }
      }
    });

    // Setup logout handler
    document.querySelectorAll('a[href="/login"], a[title="Đăng xuất"]').forEach(btn => {
      btn.addEventListener('click', function (e) {
        e.preventDefault();
        localStorage.removeItem('utesport_user');
        window.location.href = '/login';
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', syncUserProfileUI);
  } else {
    syncUserProfileUI();
  }

  // Intercept window.fetch to attach JWT Bearer token
  const originalFetch = window.fetch;
  window.fetch = function (url, options = {}) {
    const token = localStorage.getItem('jwt_token');
    if (token && typeof url === 'string' && url.startsWith('/api')) {
      options.headers = options.headers || {};
      if (options.headers instanceof Headers) {
        if (!options.headers.has('Authorization')) {
          options.headers.set('Authorization', 'Bearer ' + token);
        }
      } else if (!options.headers['Authorization']) {
        options.headers['Authorization'] = 'Bearer ' + token;
      }
    }
    return originalFetch(url, options);
  };

  // Expose global helper for testing or role switching
  window.RBAC = {
    getCurrentUser: () => currentUser,
    switchRole: function (roleName) {
      if (ROLE_CONFIG[roleName]) {
        const cfg = ROLE_CONFIG[roleName];
        const newUser = {
          username: roleName.toLowerCase(),
          role: roleName,
          fullName: cfg.defaultName,
          roleTitle: cfg.roleTitle,
          branchName: cfg.branchName,
          branchCode: cfg.branchCode
        };
        localStorage.setItem('utesport_user', JSON.stringify(newUser));
        window.location.href = cfg.homeUrl;
      }
    }
  };
})();
