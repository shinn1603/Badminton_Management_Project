/**
 * UTE SPORT - RBAC ROLE GUARD & DYNAMIC USER PROFILE
 * Manages role-based access control, protects routes, and dynamically
 * synchronizes user profile across all screens without hardcoded templates.
 */

(function () {
  'use strict';

  // Role permissions definitions - Strict Role Isolation
  const ROLE_CONFIG = {
    POS: {
      roleTitle: 'Thu ngân POS',
      defaultName: 'Nguyễn Thanh Tâm',
      branchCode: 'CN01',
      branchName: 'CN Thủ Đức',
      allowedPrefixes: ['/pos/', '/login'],
      prohibitedPrefixes: ['/manager/', '/director/', '/admin/'],
      homeUrl: '/pos/grid',
      deniedMessage: 'Tài khoản Thu ngân (POS) chỉ có quyền sử dụng phân hệ Bán hàng & Lịch sân tại quầy!'
    },
    MANAGER: {
      roleTitle: 'Quản lý chi nhánh',
      defaultName: 'Trần Phúc Bảo',
      branchCode: 'CN01',
      branchName: 'CN Thủ Đức',
      allowedPrefixes: ['/manager/', '/pos/grid', '/booking/grid', '/login'],
      prohibitedPrefixes: ['/director/', '/admin/'],
      homeUrl: '/manager/dashboard',
      deniedMessage: 'Tài khoản Quản lý chi nhánh chỉ có quyền quản lý phân hệ Chi nhánh Thủ Đức!'
    },
    DIRECTOR: {
      roleTitle: 'Giám đốc chuỗi',
      defaultName: 'Nguyễn Phước Thọ',
      branchCode: 'ALL',
      branchName: 'Toàn hệ thống',
      allowedPrefixes: ['/director/', '/pos/grid', '/booking/grid', '/manager/', '/login'],
      prohibitedPrefixes: ['/admin/'],
      homeUrl: '/director/dashboard',
      deniedMessage: 'Tài khoản Giám Đốc chỉ quản lý phân hệ Ban Điều Hành chuỗi!'
    },
    ADMIN: {
      roleTitle: 'Quản trị hệ thống',
      defaultName: 'Trần Biểu Hương',
      branchCode: 'ALL',
      branchName: 'Trung tâm kỹ thuật',
      allowedPrefixes: ['/admin/', '/director/', '/manager/', '/pos/', '/login'],
      prohibitedPrefixes: [],
      homeUrl: '/admin/users',
      deniedMessage: 'Tài khoản Quản trị viên chỉ quản trị phân hệ Cấu hình & Tài khoản hệ thống!'
    },
    CUSTOMER: {
      roleTitle: 'Hội viên trực tuyến',
      defaultName: 'Lê Bá Đạt',
      branchCode: 'ALL',
      branchName: 'Toàn hệ thống',
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

    // Auto-recover token from cookie if missing in localStorage
    if (!localStorage.getItem('jwt_token') && typeof document !== 'undefined' && document.cookie) {
      const match = document.cookie.match(/(?:^|;\s*)jwt_token=([^;]+)/);
      if (match) {
        localStorage.setItem('jwt_token', match[1]);
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
      const activeBr = localStorage.getItem('utesport_active_branch') || 'CN01';
      user.branchCode = activeBr;
      user.branchName = localStorage.getItem('utesport_active_branch_name') || (activeBr === 'CN02' ? 'Chi nhánh 2 - Bình Thạnh' : 'Chi nhánh 1 - Thủ Đức');
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
      user.branchCode = 'ALL';
      user.branchName = 'Toàn hệ thống';
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
    const lowerPath = currentPath.toLowerCase();

    // Customer member-only pages: Require login before accessing profile or booking history
    if (lowerPath === '/customer/profile' || lowerPath === '/customer/history') {
      if (!currentUser || !currentUser.fullName) {
        window.location.href = '/customer/auth?redirect=' + encodeURIComponent(currentPath);
        return;
      }
    }

    const target = resolveTargetRoleForPath(lowerPath);
    if (target && target !== 'CUSTOMER') {
      let token = localStorage.getItem('jwt_token');
      if (!token && typeof document !== 'undefined' && document.cookie) {
        const m = document.cookie.match(/(?:^|;\s*)jwt_token=([^;]+)/);
        if (m) {
          token = m[1];
          localStorage.setItem('jwt_token', token);
        }
      }
      if (!currentUser) {
        window.location.href = '/login';
        return;
      }
      if (!token) {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('utesport_user');
        window.location.href = '/login?expired=true';
        return;
      }
      // Strict role isolation: Each role can ONLY access its own subsystem
      if (currentUser.role === 'CUSTOMER') {
        alert('Tài khoản Khách Hàng không có quyền truy cập khu vực quản trị nội bộ!');
        window.location.href = '/customer/home';
        return;
      }
      if (currentUser.role === 'DIRECTOR') {
        const isAllowedDirector = target === 'DIRECTOR' || lowerPath === '/pos/grid' || lowerPath === '/booking/grid' || target === 'MANAGER';
        if (!isAllowedDirector) {
          alert(ROLE_CONFIG.DIRECTOR.deniedMessage);
          window.location.href = '/director/dashboard';
          return;
        }
      }
      if (currentUser.role === 'ADMIN') {
        // Superadmin has full access across management and operational modules
      }
      if (currentUser.role === 'MANAGER') {
        const isAllowedManager = target === 'MANAGER' || lowerPath === '/pos/grid' || lowerPath === '/booking/grid';
        if (!isAllowedManager) {
          alert(ROLE_CONFIG.MANAGER.deniedMessage);
          window.location.href = '/manager/dashboard';
          return;
        }
      }
      if (currentUser.role === 'POS' && target !== 'POS') {
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

      // Update POS branch brand indicators if present
      document.querySelectorAll('#posSidebarBranchText').forEach(el => {
        el.textContent = (currentUser.branchName || ('Chi nhánh ' + branchLabel)).toUpperCase();
      });
      document.querySelectorAll('#posHeaderBranchText').forEach(el => {
        el.textContent = `Sơ đồ & Lưới đặt sân thời gian thực - ${currentUser.branchName || branchLabel}`;
      });

      // Strict role isolation in UI navigation
      if (currentUser.role === 'DIRECTOR') {
        document.querySelectorAll('a[href^="/admin/"]').forEach(el => {
          el.style.display = 'none';
        });
      } else if (currentUser.role === 'ADMIN') {
        // Admin retains technical access to all modules
      } else if (currentUser.role === 'MANAGER') {
        document.querySelectorAll('a[href^="/director/"], a[href^="/admin/"]').forEach(el => {
          el.style.display = 'none';
        });
      } else if (currentUser.role === 'POS') {
        document.querySelectorAll('a[href^="/director/"], a[href^="/admin/"], a[href^="/manager/"]').forEach(el => {
          el.style.display = 'none';
        });
      }

      // Supervisory Return Buttons for POS Grid
      const currentPathLower = window.location.pathname.toLowerCase();
      if (currentPathLower.startsWith('/pos/')) {
        const dBackSidebar = document.getElementById('directorPosBackLink');
        const dBackHeader = document.getElementById('directorHeaderBackBtn');
        if (currentUser.role === 'DIRECTOR') {
          if (dBackSidebar) {
            dBackSidebar.href = '/director/branches';
            const mText = dBackSidebar.querySelector('.menu-text');
            if (mText) mText.textContent = 'Quay lại Chi nhánh';
            dBackSidebar.style.display = 'flex';
          }
          if (dBackHeader) {
            dBackHeader.href = '/director/branches';
            const span = dBackHeader.querySelector('span');
            if (span) span.textContent = 'Quay lại Chi nhánh';
            dBackHeader.style.display = 'inline-flex';
          }
        } else if (currentUser.role === 'ADMIN') {
          if (dBackSidebar) {
            dBackSidebar.href = '/admin/users';
            const mText = dBackSidebar.querySelector('.menu-text');
            if (mText) mText.textContent = 'Quay lại Quản trị';
            dBackSidebar.style.display = 'flex';
          }
          if (dBackHeader) {
            dBackHeader.href = '/admin/users';
            const span = dBackHeader.querySelector('span');
            if (span) span.textContent = 'Quay lại Quản trị';
            dBackHeader.style.display = 'inline-flex';
          }
        } else if (currentUser.role === 'MANAGER') {
          if (dBackSidebar) {
            dBackSidebar.href = '/manager/dashboard';
            const mText = dBackSidebar.querySelector('.menu-text');
            if (mText) mText.textContent = 'Quay lại Quản lý';
            dBackSidebar.style.display = 'flex';
          }
          if (dBackHeader) {
            dBackHeader.href = '/manager/dashboard';
            const span = dBackHeader.querySelector('span');
            if (span) span.textContent = 'Quay lại Quản lý';
            dBackHeader.style.display = 'inline-flex';
          }
        }
      }

      // Khởi tạo bộ chọn / chuyển đổi chi nhánh thông minh cho Quản lý & Thu ngân
      initBranchSwitcherUI();
    }

    // Globally synchronize Customer Nav User Area across all customer pages
    syncNavUserArea();

    // Globally synchronize Internal Topbar User Pill across all management & POS pages
    syncInternalHeaderUser();

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

  function toggleUserDropdown(e) {
    if (e) e.stopPropagation();
    const dd = document.getElementById('custUserMenuDropdown');
    if (!dd) return;
    const isVisible = dd.style.display === 'block';
    dd.style.display = isVisible ? 'none' : 'block';
  }

  function toggleInternalDropdown(e) {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    let dd = document.getElementById('internalUserMenuDropdown');
    const user = getCurrentUser();
    if (!user) return;

    if (!dd) {
      dd = document.createElement('div');
      dd.id = 'internalUserMenuDropdown';
      dd.style.cssText = 'display:none; position:fixed; width:220px; background:var(--surface); border:1px solid var(--border-subtle); border-radius:12px; box-shadow:0 12px 32px rgba(0,0,0,0.15); padding:8px; z-index:999999; text-align:left;';
      document.body.appendChild(dd);
    }

    const isVisible = dd.style.display === 'block';
    if (isVisible) {
      dd.style.display = 'none';
      return;
    }

    const roleTitle = user.roleTitle || user.role || 'Nhân sự';
    const homeUrl = (ROLE_CONFIG[user.role] || {}).homeUrl || '/customer/home';
    const isCustomer = user.role === 'CUSTOMER';

    dd.innerHTML = `
      <div style="padding: 8px 10px; border-bottom: 1px solid var(--border-subtle); margin-bottom: 4px;">
        <div style="font-size: 13px; font-weight: 700; color: var(--text-dark);">${user.fullName}</div>
        <div style="font-size: 11px; color: var(--primary); font-weight: 600;">${roleTitle} • ${user.branchCode || 'Hệ thống'}</div>
      </div>
      ${!isCustomer ? `
      <a href="${homeUrl}" style="display: flex; align-items: center; gap: 8px; padding: 8px 10px; color: var(--text-dark); font-size: 12.5px; font-weight: 500; text-decoration: none; border-radius: 6px;">
        <svg class="svg-icon" viewBox="0 0 24 24" style="width: 14px; height: 14px; color: var(--primary);"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
        <span>Bảng điều khiển</span>
      </a>` : ''}
      <a href="/customer/home" style="display: flex; align-items: center; gap: 8px; padding: 8px 10px; color: var(--text-dark); font-size: 12.5px; font-weight: 500; text-decoration: none; border-radius: 6px;">
        <svg class="svg-icon" viewBox="0 0 24 24" style="width: 14px; height: 14px; color: var(--primary);"><circle cx="12" cy="12" r="10"></circle><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"></path><path d="M2 12h20"></path></svg>
        <span>Cổng khách hàng</span>
      </a>
      <div style="height: 1px; background: var(--border-subtle); margin: 4px 0;"></div>
      <button type="button" onclick="window.RBAC.logoutCustomer()" style="width: 100%; display: flex; align-items: center; gap: 8px; padding: 8px 10px; border: none; background: transparent; color: #ef4444; font-size: 12.5px; font-weight: 600; border-radius: 6px; cursor: pointer; text-align: left;">
        <svg class="svg-icon" viewBox="0 0 24 24" style="width: 14px; height: 14px;"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
        <span>Đăng xuất</span>
      </button>
    `;

    const target = e ? (e.currentTarget || e.target.closest('.user-pill-header')) : document.querySelector('.user-pill-header');
    if (target) {
      const rect = target.getBoundingClientRect();
      dd.style.top = (rect.bottom + 6) + 'px';
      dd.style.right = Math.max(12, (window.innerWidth - rect.right)) + 'px';
      dd.style.left = 'auto';
    }
    dd.style.display = 'block';
  }

  document.addEventListener('click', function (e) {
    const internalDd = document.getElementById('internalUserMenuDropdown');
    if (internalDd && internalDd.style.display === 'block') {
      const pill = e.target.closest('.user-pill-header');
      if (!pill && !internalDd.contains(e.target)) {
        internalDd.style.display = 'none';
      }
    }

    const dd = document.getElementById('custUserMenuDropdown');
    const btn = document.getElementById('btnUserMenu');
    if (dd && dd.style.display === 'block') {
      if (btn && btn.contains(e.target)) return;
      if (!dd.contains(e.target)) {
        dd.style.display = 'none';
      }
    }
  });

  function syncInternalHeaderUser() {
    const user = getCurrentUser();
    if (!user || !user.fullName) return;

    const initials = getInitials(user.fullName);
    const roleLabel = user.roleTitle || user.role || 'Nhân sự';
    const isCustomer = user.role === 'CUSTOMER';
    const tierBadge = isCustomer ? 'Hội viên VIP' : roleLabel;

    // If page has a header actions/right container but no user pill, create one automatically
    const isCustPage = window.location.pathname.startsWith('/customer/');
    if (!isCustPage && document.querySelectorAll('.user-pill-header').length === 0 && !document.getElementById('navUserArea')) {
      const headerContainer = document.querySelector('.header-right') || document.querySelector('.header-actions');
      if (headerContainer) {
        const pill = document.createElement('div');
        pill.className = 'user-pill-header';
        pill.id = 'headerUserWidget';
        headerContainer.appendChild(pill);
      }
    }

    // Populate all .user-pill-header instances
    document.querySelectorAll('.user-pill-header').forEach(pill => {
      pill.innerHTML = `
        <div class="user-avatar" style="width: 28px; height: 28px; font-size: 11px; font-weight: 700; border-radius: 50%; background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: #fff; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">${initials}</div>
        <div style="text-align: left; line-height: 1.25;">
          <div class="user-name" style="font-size: 12.5px; font-weight: 700; color: var(--text-dark); white-space: nowrap; max-width: 130px; overflow: hidden; text-overflow: ellipsis;">${user.fullName}</div>
          <div class="user-title" style="font-size: 10px; color: var(--primary); font-weight: 600;">${tierBadge}</div>
        </div>
        <svg class="svg-icon" viewBox="0 0 24 24" style="width: 12px; height: 12px; color: var(--text-muted); flex-shrink: 0;"><polyline points="6 9 12 15 18 9"></polyline></svg>
      `;
      pill.style.display = 'inline-flex';
      pill.style.alignItems = 'center';
      pill.style.gap = '8px';
      pill.style.cursor = 'pointer';
      pill.onclick = function (e) {
        toggleInternalDropdown(e);
      };
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
        <a href="/customer/auth" class="btn btn-primary" id="btnNavAuth" style="padding: 8px 18px; border-radius: 9999px; font-size: 13.5px; font-weight: 600; text-decoration: none; display: inline-flex; align-items: center; gap: 7px; box-shadow: 0 4px 14px rgba(16, 185, 129, 0.3); white-space: nowrap;">
          <svg class="svg-icon" viewBox="0 0 24 24" style="width: 15px; height: 15px;"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path><polyline points="10 17 15 12 10 7"></polyline><line x1="15" y1="12" x2="3" y2="12"></line></svg>
          <span>Đăng nhập</span>
        </a>
      `;
      return;
    }

    const initials = getInitials(user.fullName);
    const isCustomer = user.role === 'CUSTOMER';
    const tierBadge = isCustomer ? 'Hội viên VIP' : (user.roleTitle || user.role);
    const adminLink = !isCustomer && (ROLE_CONFIG[user.role] || {}).homeUrl 
      ? `<a href="${ROLE_CONFIG[user.role].homeUrl}" class="cust-dropdown-link" style="display:flex; align-items:center; gap:9px; padding:9px 12px; text-decoration:none; color:var(--text-dark); font-size:13px; font-weight:600; border-radius:8px;">
           <svg class="svg-icon" viewBox="0 0 24 24" style="width:15px;height:15px; color:var(--primary);"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
           <span>Vào Dashboard ${user.roleTitle || 'Quản trị'}</span>
         </a>` 
      : '';

    navUserArea.innerHTML = `
      <div class="cust-user-dropdown-container" style="position: relative; display: inline-block;">
        <button type="button" id="btnUserMenu" onclick="window.RBAC.toggleUserDropdown(event)" style="display: flex; align-items: center; gap: 10px; background: var(--surface-subtle); border: 1px solid var(--border-subtle); border-radius: 9999px; padding: 4px 12px 4px 4px; cursor: pointer; transition: all 0.2s ease;">
          <div style="width: 32px; height: 32px; border-radius: 50%; background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: #fff; font-weight: 700; font-size: 13px; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 8px rgba(16, 185, 129, 0.35);">
            ${initials}
          </div>
          <div style="text-align: left; line-height: 1.25;">
            <div style="font-size: 13px; font-weight: 700; color: var(--text-dark); white-space: nowrap; max-width: 130px; overflow: hidden; text-overflow: ellipsis;">${user.fullName}</div>
            <div style="font-size: 10.5px; color: var(--primary); font-weight: 600;">${tierBadge}</div>
          </div>
          <svg class="svg-icon" viewBox="0 0 24 24" style="width: 13px; height: 13px; color: var(--text-muted);"><polyline points="6 9 12 15 18 9"></polyline></svg>
        </button>

        <div id="custUserMenuDropdown" style="display: none; position: absolute; right: 0; top: calc(100% + 8px); width: 230px; background: var(--surface); border: 1px solid var(--border-subtle); border-radius: 14px; box-shadow: 0 12px 32px rgba(0,0,0,0.12); padding: 8px; z-index: 1000;">
          <div style="padding: 10px 12px; border-bottom: 1px solid var(--border-subtle); margin-bottom: 4px;">
            <div style="font-size: 13px; font-weight: 700; color: var(--text-dark);">${user.fullName}</div>
            <div style="font-size: 11px; color: var(--text-muted);">${user.phone || user.username || ''}</div>
          </div>
          <a href="/customer/profile" class="cust-dropdown-link" style="display: flex; align-items: center; gap: 9px; padding: 9px 12px; text-decoration: none; color: var(--text-dark); font-size: 13px; font-weight: 500; border-radius: 8px;">
            <svg class="svg-icon" viewBox="0 0 24 24" style="width: 15px; height: 15px; color: var(--primary);"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
            <span>Hồ sơ & Thẻ hội viên</span>
          </a>
          <a href="/customer/history" class="cust-dropdown-link" style="display: flex; align-items: center; gap: 9px; padding: 9px 12px; text-decoration: none; color: var(--text-dark); font-size: 13px; font-weight: 500; border-radius: 8px;">
            <svg class="svg-icon" viewBox="0 0 24 24" style="width: 15px; height: 15px; color: var(--primary);"><path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            <span>Lịch sử đặt sân & Vé QR</span>
          </a>
          ${adminLink}
          <div style="height: 1px; background: var(--border-subtle); margin: 6px 0;"></div>
          <button type="button" onclick="window.RBAC.logoutCustomer()" style="width: 100%; display: flex; align-items: center; gap: 9px; padding: 9px 12px; border: none; background: transparent; color: #ef4444; font-size: 13px; font-weight: 600; border-radius: 8px; cursor: pointer; text-align: left;">
            <svg class="svg-icon" viewBox="0 0 24 24" style="width: 15px; height: 15px;"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
            <span>Đăng xuất</span>
          </button>
        </div>
      </div>
    `;
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', syncUserProfileUI);
  } else {
    syncUserProfileUI();
  }

  // Intercept window.fetch to attach JWT Bearer token and handle 401/403 Unauthorized/Forbidden
  const originalFetch = window.fetch;
  window.fetch = async function (url, options = {}) {
    let token = localStorage.getItem('jwt_token');
    if (!token && typeof document !== 'undefined' && document.cookie) {
      const match = document.cookie.match(/(?:^|;\s*)jwt_token=([^;]+)/);
      if (match) {
        token = match[1];
        localStorage.setItem('jwt_token', token);
      }
    }
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

    // Strict branch isolation ONLY for branch staff (MANAGER, POS, STAFF):
    // CUSTOMER, DIRECTOR, and ADMIN can freely query any branch across the chain.
    const isInternalBranchStaff = currentUser && currentUser.role !== 'CUSTOMER' && currentUser.role !== 'DIRECTOR' && currentUser.role !== 'ADMIN' && (currentUser.role === 'MANAGER' || currentUser.role === 'POS' || currentUser.role === 'STAFF');
    if (isInternalBranchStaff) {
      const assignedBranch = currentUser.branchCode || 'CN01';
      if (typeof url === 'string' && url.includes('branchCode=')) {
        url = url.replace(/branchCode=[^&]*/g, 'branchCode=' + encodeURIComponent(assignedBranch));
      }
    }
    const response = await originalFetch(url, options);
    if (response.status === 401 && typeof url === 'string' && url.startsWith('/api') && !url.includes('/api/auth/login')) {
      const p = window.location.pathname.toLowerCase();
      if (p.startsWith('/pos') || p.startsWith('/manager') || p.startsWith('/director') || p.startsWith('/admin')) {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('utesport_user');
        if (typeof document !== 'undefined') {
          document.cookie = 'jwt_token=; path=/; max-age=0';
        }
        window.location.href = '/login?expired=true';
      }
    }
    return response;
  };

  // ========================================================
  // BỘ CHUYỂN ĐỔI CHI NHÁNH TỰ ĐỘNG CHO QUẢN LÝ / GIÁM ĐỐC
  // ========================================================
  let myAccessibleBranches = [];

  async function fetchUserBranches() {
    if (myAccessibleBranches && myAccessibleBranches.length > 0) return myAccessibleBranches;
    try {
      let token = localStorage.getItem('jwt_token') || '';
      if (!token && typeof document !== 'undefined' && document.cookie) {
        const match = document.cookie.match(/(?:^|;\s*)jwt_token=([^;]+)/);
        if (match) token = match[1];
      }
      const headers = { 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;

      const res = await fetch('/api/user/my-branches', { headers, credentials: 'include' });
      if (res.ok) {
        const data = await res.json();
        if (data.branches && data.branches.length > 0) {
          myAccessibleBranches = data.branches;
          return myAccessibleBranches;
        }
      }
    } catch (e) {
      console.warn('Lỗi tải danh sách chi nhánh phụ trách:', e);
    }

    // Default fallback cho quản lý (mặc định phụ trách CN01 và CN02 theo phân công)
    myAccessibleBranches = [
      { branchCode: 'CN01', branchName: 'Chi nhánh 1 - Sân Cầu Lông Thủ Đức', address: 'Số 01 Võ Văn Ngân, TP. Thủ Đức' },
      { branchCode: 'CN02', branchName: 'Chi nhánh 2 - Sân Cầu Lông Bình Thạnh', address: 'Số 234 Điện Biên Phủ, Bình Thạnh' }
    ];
    return myAccessibleBranches;
  }

  async function initBranchSwitcherUI() {
    const badges = document.querySelectorAll('.branch-select-badge');
    if (!badges || badges.length === 0) return;

    const branches = await fetchUserBranches();
    let activeBranch = localStorage.getItem('utesport_active_branch');
    if (!activeBranch || !branches.some(b => b.branchCode === activeBranch)) {
      activeBranch = (currentUser && currentUser.branchCode && currentUser.branchCode !== 'ALL' && branches.some(b => b.branchCode === currentUser.branchCode))
        ? currentUser.branchCode : (branches[0] ? branches[0].branchCode : 'CN01');
      localStorage.setItem('utesport_active_branch', activeBranch);
    }
    localStorage.setItem('manager_active_branch', activeBranch);

    const activeObj = branches.find(b => b.branchCode === activeBranch) || branches[0];
    const activeName = activeObj ? activeObj.branchName : ('Chi nhánh ' + activeBranch);
    localStorage.setItem('utesport_active_branch_name', activeName);

    // Đồng bộ lại các select chi nhánh trên trang (nếu có)
    document.querySelectorAll('select#branchFilterSelect, select#dashboardBranchSelect').forEach(sel => {
      let optHtml = '';
      branches.forEach(b => {
        const selAttr = (b.branchCode === activeBranch) ? 'selected' : '';
        optHtml += `<option value="${b.branchCode}" ${selAttr}>${b.branchCode} - ${b.branchName}</option>`;
      });
      sel.innerHTML = optHtml;
      sel.value = activeBranch;
      sel.disabled = false;
      sel.style.cursor = 'pointer';
      sel.style.opacity = '1';
      sel.onchange = function() {
        switchBranch(this.value);
      };
    });

    document.querySelectorAll('select#inpItemBranch, select#inpStaffBranch, select#receiptBranchCode, select#inpProposalBranch').forEach(sel => {
      if (sel) {
        sel.value = activeBranch;
      }
    });

    badges.forEach(badge => {
      badge.removeAttribute('onclick');
      badge.style.position = 'relative';
      badge.style.display = 'inline-flex';
      badge.style.alignItems = 'center';
      badge.style.cursor = 'pointer';
      badge.style.userSelect = 'none';

      badge.innerHTML = `
        <svg class="svg-icon" viewBox="0 0 24 24" style="width: 15px; height: 15px; color: var(--primary); flex-shrink: 0;">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
        </svg>
        <span class="branch-badge-label" style="font-weight: 600; max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
          ${escapeHtml(activeName)}
        </span>
        <span class="badge badge-primary" style="font-size: 10px; padding: 1.5px 6px; font-weight: 700; border-radius: 4px; margin-left: 2px;">
          ${escapeHtml(activeBranch)}
        </span>
        <svg class="svg-icon branch-chevron-arrow" viewBox="0 0 24 24" style="width: 13px; height: 13px; color: var(--text-muted); transition: transform 0.2s; margin-left: 3px; flex-shrink: 0;">
          <polyline points="6 9 12 15 18 9"></polyline>
        </svg>
      `;

      // Tạo menu popover dropdown
      const parentWrap = badge.parentElement;
      if (parentWrap) parentWrap.style.position = 'relative';

      let oldMenu = parentWrap ? parentWrap.querySelector('.branch-dropdown-popover') : null;
      if (oldMenu) oldMenu.remove();

      const menu = document.createElement('div');
      menu.className = 'branch-dropdown-popover';
      menu.style.cssText = `
        position: absolute;
        top: calc(100% + 8px);
        left: 0;
        min-width: 340px;
        max-width: 400px;
        background: var(--surface, #ffffff);
        border: 1px solid var(--border-subtle, #e2e8f0);
        border-radius: 12px;
        box-shadow: 0 14px 35px -5px rgba(0,0,0,0.18), 0 6px 12px -3px rgba(0,0,0,0.08);
        padding: 8px 0;
        z-index: 999999;
        display: none;
      `;

      let itemsHtml = `
        <div style="padding: 6px 16px 8px 16px; border-bottom: 1px solid var(--border-subtle, #e2e8f0); display: flex; align-items: center; justify-content: space-between;">
          <div style="font-size: 11px; font-weight: 800; color: var(--text-muted, #64748b); text-transform: uppercase; letter-spacing: 0.5px;">
            Chi nhánh phụ trách (${branches.length})
          </div>
          <span style="font-size: 11px; color: var(--primary); font-weight: 600;">1-Click chuyển</span>
        </div>
        <div style="max-height: 260px; overflow-y: auto; padding: 4px 0;">
      `;

      branches.forEach(b => {
        const isCurrent = (b.branchCode === activeBranch);
        const activeBg = isCurrent ? 'background: rgba(16, 185, 129, 0.08);' : '';
        const borderStyle = isCurrent ? 'border-left: 3px solid var(--primary);' : 'border-left: 3px solid transparent;';

        itemsHtml += `
          <div class="branch-popover-item" data-code="${b.branchCode}" style="padding: 10px 16px; cursor: pointer; display: flex; align-items: center; justify-content: space-between; gap: 10px; transition: background 0.15s; ${activeBg} ${borderStyle}">
            <div style="flex: 1; min-width: 0;">
              <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 3px;">
                <span class="badge ${isCurrent ? 'badge-primary' : 'badge-outline'}" style="font-size: 10.5px; padding: 1.5px 6px; font-weight: 700;">
                  ${b.branchCode}
                </span>
                <strong style="font-size: 13px; color: var(--text-dark, #0f172a); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                  ${escapeHtml(b.branchName)}
                </strong>
              </div>
              <div style="font-size: 11.5px; color: var(--text-muted, #64748b); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: flex; align-items: center;">
                <svg viewBox="0 0 24 24" style="width: 11px; height: 11px; stroke: currentColor; fill: none; stroke-width: 2; margin-right: 4px; flex-shrink: 0;"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
                <span>${escapeHtml(b.address || 'Hệ thống sân cầu lông UTE Sport')}</span>
              </div>
            </div>
            ${isCurrent ? `
              <span style="color: var(--primary); font-weight: 700; font-size: 12px; display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0; background: rgba(16, 185, 129, 0.15); padding: 2px 8px; border-radius: 999px;">
                <svg viewBox="0 0 24 24" style="width: 12px; height: 12px; stroke: currentColor; fill: none; stroke-width: 2.5;"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>Đang chọn</span>
              </span>
            ` : `
              <span class="btn btn-outline" style="font-size: 11px; padding: 3px 8px; flex-shrink: 0; border-color: var(--border-subtle);">
                Chuyển
              </span>
            `}
          </div>
        `;
      });

      itemsHtml += `
        </div>
        <div style="padding: 8px 16px 4px 16px; border-top: 1px solid var(--border-subtle, #e2e8f0); background: var(--surface-subtle, #f8fafc); border-radius: 0 0 12px 12px;">
          <div style="font-size: 11px; color: var(--text-muted, #64748b); line-height: 1.4;">
            Đổi chi nhánh để cập nhật Lưới sân, Đơn đặt, Kho hàng, Báo cáo & Nhân sự cho cơ sở được chọn.
          </div>
        </div>
      `;

      menu.innerHTML = itemsHtml;
      if (parentWrap) parentWrap.appendChild(menu);

      // Toggle click
      badge.addEventListener('click', (e) => {
        e.stopPropagation();
        const isOpen = menu.style.display === 'block';
        document.querySelectorAll('.branch-dropdown-popover').forEach(m => m.style.display = 'none');
        document.querySelectorAll('.branch-chevron-arrow').forEach(a => a.style.transform = 'rotate(0deg)');

        if (!isOpen) {
          menu.style.display = 'block';
          const arr = badge.querySelector('.branch-chevron-arrow');
          if (arr) arr.style.transform = 'rotate(180deg)';
        }
      });

      // Item click
      menu.querySelectorAll('.branch-popover-item').forEach(item => {
        item.addEventListener('mouseenter', () => {
          if (!item.style.background.includes('185, 129')) {
            item.style.background = 'var(--surface-subtle, #f1f5f9)';
          }
        });
        item.addEventListener('mouseleave', () => {
          if (!item.style.borderLeft.includes('var(--primary)')) {
            item.style.background = '';
          }
        });

        item.addEventListener('click', (e) => {
          e.stopPropagation();
          const targetCode = item.getAttribute('data-code');
          switchBranch(targetCode);
        });
      });
    });

    // Close on click outside
    document.addEventListener('click', () => {
      document.querySelectorAll('.branch-dropdown-popover').forEach(m => m.style.display = 'none');
      document.querySelectorAll('.branch-chevron-arrow').forEach(a => a.style.transform = 'rotate(0deg)');
    });
  }

  function switchBranch(targetCode) {
    if (!targetCode) return;
    const branches = myAccessibleBranches || [];
    const targetObj = branches.find(b => b.branchCode === targetCode);
    const targetName = targetObj ? targetObj.branchName : ('Chi nhánh ' + targetCode);

    localStorage.setItem('utesport_active_branch', targetCode);
    localStorage.setItem('manager_active_branch', targetCode);
    localStorage.setItem('utesport_active_branch_name', targetName);

    if (currentUser) {
      currentUser.branchCode = targetCode;
      currentUser.branchName = targetName;
      localStorage.setItem('utesport_user', JSON.stringify(currentUser));
    }

    try {
      fetch('/api/user/active-branch', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ branchCode: targetCode })
      });
    } catch (e) {}

    if (typeof showToast === 'function') {
      showToast(`Đã chuyển sang [${targetName}]! Đang tải lại dữ liệu...`, 'success');
    }

    // Sync pos grid filter if present
    const bFilter = document.getElementById('branchFilter');
    if (bFilter) bFilter.value = targetCode;

    window.dispatchEvent(new CustomEvent('utesport:branch-changed', {
      detail: { branchCode: targetCode, branchName: targetName }
    }));

    setTimeout(() => {
      window.location.reload();
    }, 350);
  }


  // Expose global helper for testing or role switching
  window.RBAC = {
    getCurrentUser: () => currentUser,
    syncNavUserArea: syncNavUserArea,
    syncInternalHeaderUser: syncInternalHeaderUser,
    toggleUserDropdown: toggleUserDropdown,
    toggleInternalDropdown: toggleInternalDropdown,
    logoutCustomer: function () {
      localStorage.removeItem('utesport_user');
      localStorage.removeItem('jwt_token');
      document.cookie = 'jwt_token=; path=/; max-age=0';
      const p = window.location.pathname.toLowerCase();
      if (p === '/customer/profile' || p === '/customer/history') {
        window.location.href = '/customer/auth';
      } else {
        window.location.reload();
      }
    },
    switchBranch: switchBranch,
    getActiveBranch: () => localStorage.getItem('utesport_active_branch') || (currentUser ? currentUser.branchCode : 'CN01'),
    switchRole: function (roleName) {
      console.warn('Direct role mutation via navigation is disabled. Authenticated session is preserved.');
    }
  };
  window.AuthGuard = window.RBAC;
})();
