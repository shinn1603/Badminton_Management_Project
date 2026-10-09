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
    const targetRole = resolveTargetRoleForPath(currentPath);

    // If unauthenticated:
    if (!user || !user.role) {
      return null;
    }

    // Auto-heal legacy mock usernames to standard database accounts
    const legacyMap = {
      'tho.nguyen': 'director',
      'admin_system': 'admin',
      'tam.nguyen': 'pos',
      'bao.tran': 'manager',
      'huong.tran': 'admin'
    };
    if (user && legacyMap[user.username]) {
      user.username = legacyMap[user.username];
    }

    // Auto-heal: Ensure role consistency based on logged-in username
    if (user.username === 'director') {
      user.role = 'DIRECTOR';
      user.fullName = 'Nguyễn Phước Thọ';
      user.roleTitle = 'Giám đốc chuỗi';
      user.branchCode = 'ALL';
      user.branchName = 'Toàn hệ thống';
    } else if (user.username === 'manager') {
      user.role = 'MANAGER';
      user.fullName = 'Trần Phúc Bảo';
      user.roleTitle = 'Quản lý chi nhánh';
      user.branchCode = 'CN01';
      user.branchName = 'CN Thủ Đức';
    } else if (user.username === 'admin') {
      user.role = 'ADMIN';
      user.fullName = 'Trần Biểu Hương';
      user.roleTitle = 'Quản trị hệ thống';
      user.branchCode = 'ALL';
      user.branchName = 'Trung tâm kỹ thuật';
    } else if (user.username === 'pos') {
      user.role = 'POS';
      user.fullName = 'Nguyễn Thanh Tâm';
      user.roleTitle = 'Thu ngân POS';
      user.branchCode = 'CN01';
      user.branchName = 'CN Thủ Đức';
    } else if (user.username === 'customer') {
      user.role = 'CUSTOMER';
      user.fullName = 'Lê Bá Đạt';
      user.roleTitle = 'Hội viên trực tuyến';
      user.branchCode = 'CN01';
      user.branchName = 'CN Thủ Đức';
    }

    // CRITICAL: NEVER mutate an authenticated user's account or role based on current URL!
    // A Director inspecting /admin/users or /pos/grid remains Director (Nguyễn Phước Thọ).

    const cfg = ROLE_CONFIG[user.role] || ROLE_CONFIG.CUSTOMER;
    if (!user.fullName) user.fullName = cfg.defaultName;
    if (!user.roleTitle) user.roleTitle = cfg.roleTitle;
    if (!user.branchName) user.branchName = cfg.branchName;
    if (!user.branchCode) user.branchCode = cfg.branchCode;

    // Persist normalized user
    localStorage.setItem('utesport_user', JSON.stringify(user));
    return user;
  }

  function resolveTargetRoleForPath(path) {
    if (path.startsWith('/pos/') || path === '/booking/grid') return 'POS';
    if (path.startsWith('/manager/')) return 'MANAGER';
    if (path.startsWith('/director/')) return 'DIRECTOR';
    if (path.startsWith('/admin/')) return 'ADMIN';
    if (path.startsWith('/customer/') || path === '/') return 'CUSTOMER';
    return null;
  }

  const currentUser = getCurrentUser();
  const currentRoleCfg = currentUser ? (ROLE_CONFIG[currentUser.role] || ROLE_CONFIG.CUSTOMER) : null;
  const currentPath = window.location.pathname;

  // 2. RBAC Route Guard - Enforce access control hierarchy
  (function enforceRouteGuard() {
    const target = resolveTargetRoleForPath(currentPath.toLowerCase());
    if (target && target !== 'CUSTOMER') {
      if (!currentUser) {
        window.location.href = '/login';
        return;
      }
      // Restrict external customer accounts from internal staff portals
      if (currentUser.role === 'CUSTOMER') {
        alert('Tài khoản Khách Hàng không có quyền truy cập khu vực quản trị nội bộ!');
        window.location.href = '/customer/home';
        return;
      }
      // Hierarchy enforcement:
      // DIRECTOR and ADMIN have full access to all areas (no restrictions)
      // MANAGER cannot access DIRECTOR or ADMIN
      if (currentUser.role === 'MANAGER' && (target === 'DIRECTOR' || target === 'ADMIN')) {
        alert(ROLE_CONFIG.MANAGER.deniedMessage);
        window.location.href = '/manager/dashboard';
        return;
      }
      // POS cannot access MANAGER, DIRECTOR or ADMIN
      if (currentUser.role === 'POS' && (target === 'MANAGER' || target === 'DIRECTOR' || target === 'ADMIN')) {
        alert(ROLE_CONFIG.POS.deniedMessage);
        window.location.href = '/pos/grid';
        return;
      }
    }
  })();

  // 3. UI Synchronization on DOM Ready
  function syncUserProfileUI() {
    if (currentUser) {
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

      // Role-based navigation visibility based on top-down hierarchy:
      // DIRECTOR and ADMIN: Full access to all dashboards (Director, Manager, POS, Admin)
      // MANAGER: Access to Manager and POS, hide Director and Admin links
      // POS: Access to POS only, hide Manager, Director and Admin links
      if (currentUser.role === 'MANAGER') {
        document.querySelectorAll('a[href^="/director/"], a[href^="/admin/"]').forEach(el => {
          el.style.display = 'none';
        });
      } else if (currentUser.role === 'POS') {
        document.querySelectorAll('a[href^="/director/"], a[href^="/admin/"], a[href^="/manager/"]').forEach(el => {
          el.style.display = 'none';
        });
      }
    }

    // Globally synchronize Customer Nav User Area across all customer pages
    syncNavUserArea();

    // Setup logout handler
    document.querySelectorAll('a[href="/login"], a[title="Đăng xuất"]').forEach(btn => {
      btn.addEventListener('click', function (e) {
        if (btn.getAttribute('data-bypass-logout') === 'true') return;
        e.preventDefault();
        localStorage.removeItem('utesport_user');
        localStorage.removeItem('jwt_token');
        document.cookie = 'jwt_token=; path=/; max-age=0';
        window.location.href = '/login';
      });
    });
  }

  function syncNavUserArea() {
    const navUserArea = document.getElementById('navUserArea');
    if (!navUserArea) return;

    const p = window.location.pathname.toLowerCase();
    const isAuthPage = p.startsWith('/customer/auth') || p.startsWith('/customer/login') || p === '/login';
    if (isAuthPage) return;

    const user = getCurrentUser();
    if (!user || !user.fullName) {
      navUserArea.innerHTML = `
        <a href="/customer/auth" class="btn btn-primary" id="btnNavAuth" style="padding: 9px 18px; border-radius: 8px; font-size: 13.5px; font-weight: 600; text-decoration: none; display: inline-flex; align-items: center; gap: 8px;">
          <svg class="svg-icon" viewBox="0 0 24 24" style="width: 15px; height: 15px;"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path><polyline points="10 17 15 12 10 7"></polyline><line x1="15" y1="12" x2="3" y2="12"></line></svg>
          Đăng Nhập / Đăng Ký
        </a>
      `;
      return;
    }

    const initials = getInitials(user.fullName);

    if (user.role === 'CUSTOMER') {
      navUserArea.innerHTML = `
        <div style="display: flex; align-items: center; gap: 10px;">
          <a href="/customer/profile" style="text-decoration: none; display: flex; align-items: center; gap: 8px;">
            <div style="width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: #fff; font-weight: 700; font-size: 13px; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 8px rgba(16, 185, 129, 0.35);">
              ${initials}
            </div>
            <div style="text-align: left;">
              <div style="font-size: 13px; font-weight: 700; color: var(--text-dark);">${user.fullName}</div>
              <div style="font-size: 11px; color: var(--primary); font-weight: 600;">Hội Viên VIP</div>
            </div>
          </a>
          <button type="button" class="btn btn-outline" onclick="window.RBAC.logoutCustomer()" title="Đăng xuất tài khoản" style="padding: 6px 10px; font-size: 11.5px; border-radius: 6px; cursor: pointer;">
            Đăng xuất
          </button>
        </div>
      `;
    } else {
      const cfg = ROLE_CONFIG[user.role] || ROLE_CONFIG.DIRECTOR;
      const roleBadgeColors = {
        DIRECTOR: 'background: rgba(139, 92, 246, 0.15); color: #8b5cf6; border: 1px solid rgba(139, 92, 246, 0.3);',
        ADMIN: 'background: rgba(239, 68, 68, 0.15); color: #ef4444; border: 1px solid rgba(239, 68, 68, 0.3);',
        MANAGER: 'background: rgba(59, 130, 246, 0.15); color: #3b82f6; border: 1px solid rgba(59, 130, 246, 0.3);',
        POS: 'background: rgba(245, 158, 11, 0.15); color: #f59e0b; border: 1px solid rgba(245, 158, 11, 0.3);'
      };
      const badgeStyle = roleBadgeColors[user.role] || 'background: rgba(16, 185, 129, 0.15); color: #10b981;';

      navUserArea.innerHTML = `
        <div style="display: flex; align-items: center; gap: 10px;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <div style="width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%); color: #38bdf8; font-weight: 700; font-size: 13px; display: flex; align-items: center; justify-content: center; border: 1px solid rgba(56, 189, 248, 0.4); box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);">
              ${initials}
            </div>
            <div style="text-align: left;">
              <div style="font-size: 13px; font-weight: 700; color: var(--text-dark);">${user.fullName}</div>
              <span style="font-size: 10px; font-weight: 700; padding: 1px 6px; border-radius: 4px; ${badgeStyle}">
                ${user.roleTitle || cfg.roleTitle}
              </span>
            </div>
          </div>
          <a href="${cfg.homeUrl}" class="btn btn-primary" style="padding: 6px 12px; font-size: 11.5px; border-radius: 6px; text-decoration: none; font-weight: 600; white-space: nowrap;">
            Vào Dashboard
          </a>
          <button type="button" class="btn btn-outline" onclick="window.RBAC.logoutCustomer()" title="Đăng xuất tài khoản" style="padding: 6px 10px; font-size: 11.5px; border-radius: 6px; cursor: pointer;">
            Đăng xuất
          </button>
        </div>
      `;
    }
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
    syncNavUserArea: syncNavUserArea,
    logoutCustomer: function () {
      localStorage.removeItem('utesport_user');
      localStorage.removeItem('jwt_token');
      document.cookie = 'jwt_token=; path=/; max-age=0';
      if (typeof showToast === 'function') {
        showToast('Đã đăng xuất tài khoản thành công', 'info');
      }
      setTimeout(() => {
        window.location.href = '/customer/auth';
      }, 400);
    },
    switchRole: function (roleName) {
      console.warn('Direct role mutation via navigation is disabled. Authenticated session is preserved.');
    }
  };
})();
