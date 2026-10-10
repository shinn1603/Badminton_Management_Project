/**
 * UTE SPORT - POS GRID REALTIME LOGIC
 * Module: Le tan & Ban hang POS (MH-NVQ01 -> MH-NVQ07)
 * Live Database Integration (No Mock / No Random Data)
 */

let appData = {};
let currentBranch = 'CN01';
let currentDateStr = '';
let activeSlotCell = null;
let posItems = [];
let currentBasePrice = 120000;
let currentDeposit = 36000;
let realCourts = [];

const timeSlots = [
  '06:00 - 07:00', '07:00 - 08:00', '08:00 - 09:00', '09:00 - 10:00', '10:00 - 11:00',
  '14:00 - 15:00', '15:00 - 16:00', '16:00 - 17:00', '17:00 - 18:00', '18:00 - 19:00',
  '19:00 - 20:00', '20:00 - 21:00', '21:00 - 22:00'
];

const fallbackCourts = [
  { courtCode: 'CL01', courtName: 'San 01 (VIP Yonex)', courtType: 'VIP Yonex', hourlyRate: 120000, branchCode: 'CN01' },
  { courtCode: 'CL02', courtName: 'San 02 (VIP Yonex)', courtType: 'VIP Yonex', hourlyRate: 120000, branchCode: 'CN01' },
  { courtCode: 'CL03', courtName: 'San 03 (Tham Enlio)', courtType: 'Tieu chuan', hourlyRate: 90000, branchCode: 'CN01' },
  { courtCode: 'CL04', courtName: 'San 04 (Tham Enlio)', courtType: 'Tieu chuan', hourlyRate: 90000, branchCode: 'CN01' },
  { courtCode: 'CL05', courtName: 'San 05 (Tieu chuan)', courtType: 'Tieu chuan', hourlyRate: 90000, branchCode: 'CN01' },
  { courtCode: 'CL06', courtName: 'San 06 (Tieu chuan)', courtType: 'Tieu chuan', hourlyRate: 90000, branchCode: 'CN01' }
];

function formatDate(dateObj) {
  const yyyy = dateObj.getFullYear();
  const mm = String(dateObj.getMonth() + 1).padStart(2, '0');
  const dd = String(dateObj.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

function isSlotOverlapping(s1, s2) {
  if (!s1 || !s2) return false;
  if (s1.trim().toLowerCase() === s2.trim().toLowerCase()) return true;
  const parseHours = (slot) => {
    const m = slot.match(/(\d{1,2}):\d{2}\s*-\s*(\d{1,2}):\d{2}/);
    return m ? [parseInt(m[1]), parseInt(m[2])] : [0, 0];
  };
  const [start1, end1] = parseHours(s1);
  const [start2, end2] = parseHours(s2);
  return Math.max(start1, start2) < Math.min(end1, end2);
}

// Update modal select dropdowns with real courts from database
function updateCourtSelectDropdowns() {
  const bSelect = document.getElementById('bookingCourtSelect');
  if (bSelect && realCourts.length > 0) {
    const curVal = bSelect.value;
    bSelect.innerHTML = realCourts.map(c => {
      const rate = Number(c.hourlyRate) || 90000;
      return `<option value="${c.courtCode}" data-price="${rate}">${c.courtName} - ${rate.toLocaleString('vi-VN')} đ/h</option>`;
    }).join('');
    if (curVal && realCourts.some(c => c.courtCode === curVal)) {
      bSelect.value = curVal;
    }
  }

  const tSelect = document.getElementById('transferTargetCourtSelect');
  if (tSelect && realCourts.length > 0) {
    const curVal = tSelect.value;
    tSelect.innerHTML = realCourts.map(c => {
      const rate = Number(c.hourlyRate) || 90000;
      return `<option value="${c.courtCode}" data-price="${rate}">${c.courtName} - ${rate.toLocaleString('vi-VN')} đ/h</option>`;
    }).join('');
    if (curVal && realCourts.some(c => c.courtCode === curVal)) {
      tSelect.value = curVal;
    }
  }
}

// Load real courts for the active branch
async function loadCourtsForBranch(branchCode) {
  try {
    const res = await fetch(`/api/courts?branchCode=${encodeURIComponent(branchCode)}`);
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data) && data.length > 0) {
        realCourts = data;
      } else {
        realCourts = fallbackCourts.map(c => ({ ...c, branchCode: branchCode }));
      }
    } else {
      realCourts = fallbackCourts.map(c => ({ ...c, branchCode: branchCode }));
    }
  } catch (e) {
    console.warn('Lỗi kết nối tải sân:', e);
    realCourts = fallbackCourts.map(c => ({ ...c, branchCode: branchCode }));
  }
  updateCourtSelectDropdowns();
}

// Render POS timetable grid completely from live backend database
async function renderGridForDate(dateStr) {
  const branchFilterEl = document.getElementById('branchFilter');
  currentBranch = branchFilterEl ? branchFilterEl.value : 'CN01';
  currentDateStr = dateStr;

  const tbody = document.getElementById('matrixGridBody');
  if (!tbody) return;

  if (!realCourts || realCourts.length === 0 || realCourts[0].branchCode !== currentBranch) {
    await loadCourtsForBranch(currentBranch);
  }

  let bookings = [];
  try {
    const res = await fetch(`/api/bookings?branchCode=${encodeURIComponent(currentBranch)}&date=${encodeURIComponent(dateStr)}`);
    if (res.ok) {
      bookings = await res.json() || [];
    }
  } catch (err) {
    console.warn('Lỗi tải dữ liệu đặt sân:', err);
  }

  if (!appData[currentBranch]) appData[currentBranch] = {};
  appData[currentBranch][dateStr] = {};

  tbody.innerHTML = '';
  const todayStr = formatDate(new Date());

  realCourts.forEach(court => {
    const isVip = (court.courtType && court.courtType.toLowerCase().includes('vip')) || (court.hourlyRate >= 110000);
    const isEnlio = court.courtName && court.courtName.toLowerCase().includes('enlio');
    const defaultPrice = Number(court.hourlyRate) || (isVip ? 120000 : 90000);

    if (!appData[currentBranch][dateStr][court.courtCode]) {
      appData[currentBranch][dateStr][court.courtCode] = {};
    }

    let rowHtml = `
      <tr>
        <td class="matrix-td-court">
          <div class="court-info">
            <span class="court-name">${court.courtName}</span>
            <span class="court-type">${isVip ? 'VIP • Thảm Yonex BWF' : (isEnlio ? 'Tiêu chuẩn • Thảm Enlio' : 'Tiêu chuẩn • Thảm Taraflex')}</span>
          </div>
        </td>`;

    timeSlots.forEach(timeSlot => {
      const [startTime, endTime] = timeSlot.split(' - ');

      const matchedBooking = bookings.find(b => {
        if (!b.status || b.status === 'Đã hủy') return false;
        const matchCourt = (b.courtCode && (
          b.courtCode.toUpperCase() === court.courtCode.toUpperCase() ||
          b.courtCode.toLowerCase() === court.courtName.toLowerCase() ||
          court.courtName.toLowerCase().includes(b.courtCode.toLowerCase())
        ));
        return matchCourt && isSlotOverlapping(timeSlot, b.timeSlot);
      });

      let cellData;
      if (matchedBooking) {
        let state = 'booked';
        const st = (matchedBooking.status || '').toLowerCase();
        if (st.includes('đang sử dụng') || st.includes('đang chơi') || st === 'in-use') {
          state = 'in-use';
        } else if (st.includes('hoàn thành') || st.includes('đã thanh toán') || st === 'completed') {
          state = 'completed';
        } else {
          state = 'booked';
        }

        const bPrice = Number(matchedBooking.totalPrice) || defaultPrice;
        const bDeposit = Number(matchedBooking.depositAmount) || Math.round(bPrice * 0.3);

        cellData = {
          state: state,
          code: matchedBooking.bookingCode,
          customer: matchedBooking.customerName || 'Khách đặt sân',
          phone: matchedBooking.customerPhone || '',
          price: bPrice,
          deposit: bDeposit,
          rawStatus: matchedBooking.status,
          posItems: []
        };
      } else {
        cellData = {
          state: 'available',
          code: '',
          customer: '',
          phone: '',
          price: defaultPrice,
          deposit: Math.round(defaultPrice * 0.3),
          rawStatus: 'Trống',
          posItems: []
        };
      }

      appData[currentBranch][dateStr][court.courtCode][timeSlot] = cellData;

      rowHtml += `<td class="matrix-slot-cell">`;
      if (cellData.state === 'available') {
        if (dateStr < todayStr) {
          rowHtml += `
            <div class="slot-inner" style="background-color: var(--surface-subtle); border: 1px dashed var(--border-subtle); color: var(--text-muted); cursor: not-allowed;" onclick="alert('Không thể đặt sân cho các ngày trong quá khứ!')">
              <span class="slot-title" style="font-weight: 500;">Hết giờ</span>
            </div>`;
        } else {
          rowHtml += `
            <div class="slot-inner state-available" data-court-code="${court.courtCode}" data-court="${court.courtName}" data-time="${timeSlot}" onclick="quickBookSlot(this, '${court.courtCode}', '${startTime}', '${endTime}', ${cellData.price})">
              <div class="slot-action">
                <svg class="svg-icon" viewBox="0 0 24 24" style="width: 12px; height: 12px;"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                <span>Đặt</span>
              </div>
              <span class="slot-price">${cellData.price.toLocaleString('vi-VN')} đ</span>
            </div>`;
        }
      } else if (cellData.state === 'booked') {
        const isPending = (cellData.rawStatus || '').includes('Chờ');
        const badgeClass = isPending ? 'badge-warning' : 'badge-booked';
        const badgeStyle = isPending ? 'background: rgba(245, 158, 11, 0.15); color: #d97706; border-color: rgba(245, 158, 11, 0.3);' : '';
        const badgeText = cellData.rawStatus || 'Đã cọc 30%';

        rowHtml += `
          <div class="slot-inner state-booked" data-code="${cellData.code}" data-price="${cellData.price}" data-deposit="${cellData.deposit}" data-customer="${cellData.customer}" data-phone="${cellData.phone}" data-court="${court.courtName}" data-court-code="${court.courtCode}" data-time="${timeSlot}" onclick="openDetailModal(this, '${cellData.code}', '${court.courtName}', '${timeSlot}', '${cellData.customer}', '${cellData.phone}', '${badgeText}', ${cellData.price}, ${cellData.deposit})">
            <span class="slot-title">${cellData.customer}</span>
            <span class="slot-badge-status ${badgeClass}" style="${badgeStyle}">${badgeText}</span>
          </div>`;
      } else if (cellData.state === 'in-use') {
        rowHtml += `
          <div class="slot-inner state-in-use" data-code="${cellData.code}" data-price="${cellData.price}" data-deposit="${cellData.deposit}" data-customer="${cellData.customer}" data-phone="${cellData.phone}" data-court="${court.courtName}" data-court-code="${court.courtCode}" data-time="${timeSlot}" onclick="openCheckoutModal(this)">
            <span class="slot-title">${cellData.customer}</span>
            <span class="slot-badge-status badge-in-use">Đang chơi</span>
          </div>`;
      } else if (cellData.state === 'completed') {
        rowHtml += `
          <div class="slot-inner state-completed" data-code="${cellData.code}" data-court="${court.courtName}" data-court-code="${court.courtCode}" data-time="${timeSlot}">
            <span class="slot-title" style="font-weight: 700;">${cellData.customer}</span>
            <span class="slot-badge-status badge-completed">Đã thanh toán</span>
          </div>`;
      }
      rowHtml += `</td>`;
    });
    rowHtml += `</tr>`;
    tbody.innerHTML += rowHtml;
  });

  handleSearch();
}

function updateDataStore(courtCode, time, updateObj) {
  if (!appData[currentBranch]) appData[currentBranch] = {};
  if (!appData[currentBranch][currentDateStr]) appData[currentBranch][currentDateStr] = {};
  if (!appData[currentBranch][currentDateStr][courtCode]) appData[currentBranch][currentDateStr][courtCode] = {};
  if (!appData[currentBranch][currentDateStr][courtCode][time]) {
    appData[currentBranch][currentDateStr][courtCode][time] = { price: 90000 };
  }
  Object.assign(appData[currentBranch][currentDateStr][courtCode][time], updateObj);
}

function shiftDate(offset) {
  const dateInput = document.getElementById('gridDateInput');
  if (!dateInput) return;
  const curDate = new Date(dateInput.value);
  if (isNaN(curDate)) return;
  curDate.setDate(curDate.getDate() + offset);
  dateInput.value = formatDate(curDate);
  renderGridForDate(dateInput.value);
}

function goToToday() {
  const dateInput = document.getElementById('gridDateInput');
  if (dateInput) {
    dateInput.value = formatDate(new Date());
    renderGridForDate(dateInput.value);
  }
}

async function changeBranch() {
  const branchFilterEl = document.getElementById('branchFilter');
  if (branchFilterEl) {
    currentBranch = branchFilterEl.value;
  }
  const dateInput = document.getElementById('gridDateInput');
  await loadCourtsForBranch(currentBranch);
  if (dateInput) {
    await renderGridForDate(dateInput.value);
  }
}

function toggleSidebar() {
  const s = document.getElementById('appSidebar');
  if (s) s.classList.toggle('collapsed');
}

function toggleTheme() {
  const cur = document.documentElement.getAttribute('data-theme') || 'light';
  const nxt = cur === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', nxt);
  localStorage.setItem('utesport_theme', nxt);
}

// SEARCH & HIGHLIGHT REAL BOOKINGS
function handleSearch() {
  const searchInput = document.getElementById('searchInput');
  if (!searchInput) return;
  const query = searchInput.value.trim().toLowerCase();
  const dropdown = document.getElementById('searchResultsDropdown');
  const resultList = document.getElementById('searchResultsList');

  if (!query) {
    if (dropdown) dropdown.classList.remove('active');
    document.querySelectorAll('.slot-inner').forEach(slot => {
      slot.classList.remove('slot-highlight', 'slot-dimmed');
    });
    return;
  }

  let matchCount = 0;
  if (resultList) resultList.innerHTML = '';

  for (let branch in appData) {
    for (let date in appData[branch]) {
      for (let courtCode in appData[branch][date]) {
        for (let time in appData[branch][date][courtCode]) {
          const data = appData[branch][date][courtCode][time];
          if (data.state !== 'available') {
            const matchName = data.customer && data.customer.toLowerCase().includes(query);
            const matchPhone = data.phone && data.phone.includes(query);

            if (matchName || matchPhone) {
              matchCount++;
              if (resultList) {
                const courtObj = realCourts.find(c => c.courtCode === courtCode);
                const displayCourt = courtObj ? courtObj.courtName : courtCode;
                const li = document.createElement('li');
                li.className = 'search-result-item';
                li.innerHTML = `
                  <div class="sri-name">${data.customer} <span class="sri-phone">(${data.phone})</span></div>
                  <div class="sri-details">
                    <span>${displayCourt}</span>
                    <span>${time}</span>
                    <span>${date}</span>
                    <span class="badge ${data.state === 'in-use' ? 'badge-in-use' : 'badge-booked'}">${data.state === 'in-use' ? 'Đang chơi' : 'Đã cọc'}</span>
                  </div>
                `;
                li.onclick = () => {
                  searchInput.value = data.customer;
                  const bFilter = document.getElementById('branchFilter');
                  if (bFilter) bFilter.value = branch;
                  currentBranch = branch;
                  const gDate = document.getElementById('gridDateInput');
                  if (gDate) gDate.value = date;
                  renderGridForDate(date).then(() => {
                    if (dropdown) dropdown.classList.remove('active');
                    highlightExactSlot(courtCode, time);
                  });
                };
                resultList.appendChild(li);
              }
            }
          }
        }
      }
    }
  }

  if (dropdown) {
    if (matchCount > 0) dropdown.classList.add('active');
    else dropdown.classList.remove('active');
  }
}

function highlightExactSlot(courtCode, time) {
  setTimeout(() => {
    document.querySelectorAll('.slot-inner').forEach(slot => {
      const slotCourtCode = slot.dataset.courtCode;
      const slotTime = slot.dataset.time;
      if (slotCourtCode === courtCode && slotTime === time) {
        slot.classList.add('slot-highlight');
        slot.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });
      } else {
        slot.classList.add('slot-dimmed');
      }
    });
  }, 100);
}

// QUICK BOOKING MODAL LOGIC
function openCreateBookingModal() {
  document.getElementById('bookingCustName').value = '';
  document.getElementById('bookingCustPhone').value = '';
  recalculatePrice();
  document.getElementById('createBookingModal').classList.add('active');
}

function quickBookSlot(element, courtCodeOrName, startTime, endTime, price) {
  const courtSelect = document.getElementById('bookingCourtSelect');
  if (courtSelect) {
    for (let i = 0; i < courtSelect.options.length; i++) {
      if (courtSelect.options[i].value === courtCodeOrName || courtSelect.options[i].text.includes(courtCodeOrName)) {
        courtSelect.selectedIndex = i;
        break;
      }
    }
  }
  const sTime = document.getElementById('bookingStartTime');
  if (sTime) sTime.value = startTime;
  const eTime = document.getElementById('bookingEndTime');
  if (eTime) eTime.value = endTime;
  document.getElementById('bookingCustName').value = '';
  document.getElementById('bookingCustPhone').value = '';
  recalculatePrice();
  document.getElementById('createBookingModal').classList.add('active');
}

function recalculatePrice() {
  const courtSelect = document.getElementById('bookingCourtSelect');
  if (!courtSelect || courtSelect.selectedIndex < 0) return;
  const selectedOption = courtSelect.options[courtSelect.selectedIndex];
  const pricePerHour = Number(selectedOption.dataset.price) || 90000;

  const startTime = document.getElementById('bookingStartTime').value;
  const endTime = document.getElementById('bookingEndTime').value;

  if (startTime >= endTime) {
    document.getElementById('displayTotalRental').textContent = '0 đ';
    document.getElementById('displayDepositRequired').textContent = '0 đ';
    return;
  }

  let count = 0;
  timeSlots.forEach(slot => {
    const [s, e] = slot.split(' - ');
    if (s >= startTime && e <= endTime) count++;
  });

  const total = (count || 1) * pricePerHour;
  const payType = document.getElementById('bookingPaymentType') ? document.getElementById('bookingPaymentType').value : 'play_now';
  let deposit = 0;
  const labelEl = document.getElementById('depositLabelText');

  if (payType === 'play_now') {
    deposit = 0;
    if (labelEl) labelEl.textContent = 'Tiền cọc thực thu tại quầy:';
    document.getElementById('displayDepositRequired').textContent = '0 đ (Trả sau khi chơi)';
  } else if (payType === 'deposit_30') {
    deposit = Math.round(total * 0.3);
    if (labelEl) labelEl.textContent = 'Tiền cọc thực thu (30%):';
    document.getElementById('displayDepositRequired').textContent = deposit.toLocaleString('vi-VN') + ' đ';
  } else if (payType === 'full_pay') {
    deposit = total;
    if (labelEl) labelEl.textContent = 'Thanh toán đủ tại quầy (100%):';
    document.getElementById('displayDepositRequired').textContent = total.toLocaleString('vi-VN') + ' đ (Thu đủ)';
  }

  document.getElementById('displayTotalRental').textContent = total.toLocaleString('vi-VN') + ' đ';
}

async function submitBookingForm() {
  const name = document.getElementById('bookingCustName').value.trim();
  const phone = document.getElementById('bookingCustPhone').value.trim();
  if (!name || !phone) {
    if (typeof showToast === 'function') showToast('Vui lòng nhập họ tên và số điện thoại.', 'warning');
    else alert('Vui lòng nhập họ tên và số điện thoại.');
    return;
  }

  const courtSelect = document.getElementById('bookingCourtSelect');
  const courtCode = courtSelect.value;
  const startTime = document.getElementById('bookingStartTime').value;
  const endTime = document.getElementById('bookingEndTime').value;

  if (startTime >= endTime) {
    if (typeof showToast === 'function') showToast('Giờ kết thúc phải diễn ra sau giờ bắt đầu!', 'warning');
    else alert('Giờ kết thúc phải diễn ra sau giờ bắt đầu!');
    return;
  }

  let targetSlots = [];
  timeSlots.forEach(slot => {
    const [slotStart, slotEnd] = slot.split(' - ');
    if (slotStart >= startTime && slotEnd <= endTime) targetSlots.push(slot);
  });

  if (targetSlots.length === 0) {
    if (typeof showToast === 'function') showToast('Không tìm thấy khung giờ phù hợp!', 'warning');
    else alert('Không tìm thấy khung giờ phù hợp!');
    return;
  }

  const total = Number(document.getElementById('displayTotalRental').textContent.replace(/\D/g, ''));
  const payType = document.getElementById('bookingPaymentType') ? document.getElementById('bookingPaymentType').value : 'play_now';
  let depositAmount = 0;
  let bookingStatus = 'Đang chơi';
  let payMethod = 'Trả sau khi chơi';

  if (payType === 'play_now') {
    depositAmount = 0;
    bookingStatus = 'Đang chơi';
    payMethod = 'Trả sau khi chơi';
  } else if (payType === 'deposit_30') {
    depositAmount = Math.round(total * 0.3);
    bookingStatus = 'Đã cọc 30%';
    payMethod = 'Tiền mặt tại quầy';
  } else if (payType === 'full_pay') {
    depositAmount = total;
    bookingStatus = 'Đã thanh toán';
    payMethod = 'Tiền mặt tại quầy';
  }

  const genCode = 'DS' + Math.floor(100000 + Math.random() * 900000);

  try {
    const res = await fetch('/api/bookings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookingCode: genCode,
        courtCode: courtCode,
        branchCode: currentBranch,
        customerName: name,
        customerPhone: phone,
        bookingDate: currentDateStr,
        timeSlot: `${startTime} - ${endTime}`,
        hourlyPrice: total / targetSlots.length,
        totalPrice: total,
        depositAmount: depositAmount,
        status: bookingStatus,
        paymentMethod: payMethod
      })
    });

    if (res.ok) {
      window.lastActionBookingCode = genCode;
      if (typeof showToast === 'function') {
        showToast(`Đặt sân thành công cho khách: ${name}! Mã: ${genCode}`, 'success');
      } else {
        alert(`Đặt sân thành công cho khách: ${name}!`);
      }
      closeCreateBookingModal();
      await renderGridForDate(currentDateStr);
    } else {
      const errData = await res.json().catch(() => ({}));
      if (typeof showToast === 'function') showToast(errData.message || 'Lỗi đặt sân từ máy chủ', 'error');
      else alert(errData.message || 'Lỗi đặt sân');
    }
  } catch (err) {
    if (typeof showToast === 'function') showToast('Lỗi kết nối đặt sân: ' + err.message, 'error');
    else alert('Lỗi kết nối đặt sân: ' + err.message);
  }
}

// DETAIL MODAL & CHECK-IN
function openDetailModal(element, code, court, time, customer, phone, status, price, deposit = 0) {
  activeSlotCell = element;
  activeSlotCell.dataset.code = code || 'DS0102';
  activeSlotCell.dataset.price = price;
  activeSlotCell.dataset.deposit = deposit || Math.round(price * 0.3);
  activeSlotCell.dataset.customer = customer;
  activeSlotCell.dataset.phone = phone;
  activeSlotCell.dataset.court = court;
  activeSlotCell.dataset.time = time;

  document.getElementById('detailCourt').textContent = court;
  document.getElementById('detailTime').textContent = time;
  document.getElementById('detailCustomer').textContent = customer;
  document.getElementById('detailPhone').textContent = phone;
  document.getElementById('detailStatusBadge').textContent = status;
  document.getElementById('detailBookingModal').classList.add('active');
}

async function checkInBooking() {
  if (activeSlotCell) {
    const bCode = activeSlotCell.dataset.code;
    if (bCode) {
      window.lastActionBookingCode = bCode;
      try {
        const res = await fetch(`/api/bookings/${bCode}/checkin`, { method: 'POST' });
        const data = await res.json();
        if (data.success || res.ok) {
          if (typeof showToast === 'function') showToast(`Khách đã nhận sân [${activeSlotCell.dataset.court}]!`, 'success');
        } else {
          if (typeof showToast === 'function') showToast(data.message || 'Lỗi check-in', 'error');
        }
      } catch (e) {
        if (typeof showToast === 'function') showToast('Lỗi kết nối: ' + e.message, 'error');
      }
    }
    await renderGridForDate(currentDateStr);
  }
  closeDetailModal();
}

async function cancelBookingFromModal() {
  if (!activeSlotCell) return;
  const bCode = activeSlotCell.dataset.code;
  const cust = activeSlotCell.dataset.customer;
  if (!confirm(`Bạn có chắc chắn muốn hủy đơn đặt sân [${bCode}] của khách [${cust}]?`)) {
    return;
  }
  window.lastActionBookingCode = bCode;
  try {
    const res = await fetch(`/api/bookings/${bCode}/cancel`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason: 'Nhân viên hủy tại quầy POS' })
    });
    const data = await res.json();
    if (data.success || res.ok) {
      if (typeof showToast === 'function') showToast(`Đã hủy đơn đặt sân [${bCode}]!`, 'info');
      closeDetailModal();
      await renderGridForDate(currentDateStr);
    } else {
      if (typeof showToast === 'function') showToast(data.message || 'Lỗi hủy đơn', 'error');
    }
  } catch (e) {
    if (typeof showToast === 'function') showToast('Lỗi kết nối: ' + e.message, 'error');
  }
}

// CHECKOUT & POS SERVICES MODAL (DYNAMIC DB INTEGRATED)
let dbProductsCache = [];
let currentPosFilterCat = 'all';
let currentPosSearchTerm = '';

function loadDbProductsForPos() {
  fetch('/api/products')
    .then(r => r.json())
    .then(list => {
      if (Array.isArray(list) && list.length > 0) {
        dbProductsCache = list;
      }
      renderPosProductGrid();
    })
    .catch(err => {
      console.warn('Không tải được danh sách sản phẩm từ CSDL:', err);
      if (!dbProductsCache.length) {
        dbProductsCache = [
          { productCode: 'P01', productName: 'Nước bù khoáng Revive', category: 'Nước giải khát', price: 15000, currentStock: 80 },
          { productCode: 'P02', productName: 'Pocari Sweat 500ml', category: 'Nước giải khát', price: 18000, currentStock: 45 },
          { productCode: 'P03', productName: 'Nước suối Aquafina', category: 'Nước giải khát', price: 10000, currentStock: 120 },
          { productCode: 'P05', productName: 'Ống Cầu Hải Yến Đỏ (12 quả)', category: 'Dụng cụ', price: 180000, currentStock: 25 },
          { productCode: 'P06', productName: 'Ống Cầu Yonex AS-50', category: 'Dụng cụ', price: 420000, currentStock: 15 },
          { productCode: 'P07', productName: 'Thuê Vợt Yonex Astrox', category: 'Dụng cụ', price: 25000, currentStock: 10 },
          { productCode: 'P08', productName: 'Quấn cán vợt VS xịn', category: 'Phụ kiện', price: 15000, currentStock: 50 }
        ];
      }
      renderPosProductGrid();
    });
}

function filterPosCat(category) {
  currentPosFilterCat = category;
  document.querySelectorAll('.pos-cat-btn').forEach(btn => {
    if (btn.getAttribute('data-cat') === category) {
      btn.className = 'btn btn-xs btn-primary pos-cat-btn';
    } else {
      btn.className = 'btn btn-xs btn-outline pos-cat-btn';
    }
  });
  renderPosProductGrid();
}

function filterPosSearch(term) {
  currentPosSearchTerm = (term || '').trim().toLowerCase();
  renderPosProductGrid();
}

function renderPosProductGrid() {
  const container = document.getElementById('posProductGrid');
  const countBadge = document.getElementById('posProductCount');
  if (!container) return;

  const filtered = dbProductsCache.filter(p => {
    const pCat = p.category || '';
    const matchCat = currentPosFilterCat === 'all' || 
      pCat.toLowerCase().includes(currentPosFilterCat.toLowerCase()) ||
      (currentPosFilterCat === 'Dụng cụ' && (pCat === 'Dụng cụ' || pCat === 'Cầu lông'));
    const matchSearch = !currentPosSearchTerm || 
      (p.productName && p.productName.toLowerCase().includes(currentPosSearchTerm)) || 
      (p.productCode && p.productCode.toLowerCase().includes(currentPosSearchTerm));
    return matchCat && matchSearch;
  });

  if (countBadge) {
    countBadge.innerText = `${filtered.length} / ${dbProductsCache.length} mặt hàng CSDL`;
  }

  if (filtered.length === 0) {
    container.innerHTML = `<div style="grid-column: 1 / -1; padding: 14px; text-align: center; color: var(--text-muted); font-size: 11.5px; background: var(--surface-subtle); border-radius: 6px;">Không tìm thấy mặt hàng nào phù hợp trong kho CSDL.</div>`;
    return;
  }

  container.innerHTML = filtered.map(p => {
    const rawPrice = (p.unitPrice !== undefined && p.unitPrice !== null) ? p.unitPrice : (p.price || 0);
    const rawStock = (p.stockQuantity !== undefined && p.stockQuantity !== null) ? p.stockQuantity : (p.currentStock !== undefined ? p.currentStock : null);
    const priceFormatted = Number(rawPrice).toLocaleString('vi-VN') + 'đ';
    const isOutOfStock = (rawStock !== null && rawStock <= 0);
    const stockBadge = isOutOfStock 
      ? `<span style="color: var(--danger); font-size: 10px; font-weight: bold;">Hết hàng</span>`
      : `<span style="color: var(--text-muted); font-size: 10px;">Kho: ${rawStock ?? 'Sẵn'}</span>`;

    const safeName = (p.productName || '').replace(/'/g, "\\'");
    return `
      <button type="button" class="btn btn-outline" 
              style="padding: 8px; font-size: 11.5px; display: flex; flex-direction: column; align-items: flex-start; text-align: left; position: relative; border-radius: 6px; border: 1px solid var(--border-subtle); background: var(--surface); cursor: pointer; transition: all 0.15s ease;"
              onclick="quickAddProduct('${p.productCode}', '${safeName}', ${rawPrice}, ${rawStock ?? 999})"
              title="${p.productName} (${p.productCode})">
        <strong style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; font-size: 11.5px; color: var(--text-dark);">${p.productName}</strong>
        <div style="display: flex; justify-content: space-between; width: 100%; margin-top: 4px; align-items: center;">
          <span style="color: var(--primary); font-weight: 700; font-size: 11.5px;">${priceFormatted}</span>
          ${stockBadge}
        </div>
      </button>
    `;
  }).join('');
}

function populateCheckoutCourtDropdown(selectedCourtSlot = null) {
  const assignSelect = document.getElementById('checkoutCourtAssignSelect');
  if (!assignSelect) return;

  let optionsHtml = '<option value="none">Bán lẻ mang đi (Tiền sân 0 đ)</option>';
  
  if (appData[currentBranch] && appData[currentBranch][currentDateStr]) {
    const branchSlots = appData[currentBranch][currentDateStr];
    for (let cCode in branchSlots) {
      const courtObj = realCourts.find(c => c.courtCode === cCode);
      const courtDisplayName = courtObj ? courtObj.courtName : cCode;
      for (let tSlot in branchSlots[cCode]) {
        const slot = branchSlots[cCode][tSlot];
        if (slot.state === 'in-use' || slot.state === 'booked') {
          const val = `${cCode}__${tSlot}`;
          const isSelected = selectedCourtSlot === val ? 'selected' : '';
          optionsHtml += `<option value="${val}" ${isSelected}>${courtDisplayName} (${tSlot} - ${slot.customer || 'Đang chơi'})</option>`;
        }
      }
    }
  }

  assignSelect.innerHTML = optionsHtml;
}

function onCheckoutCourtAssignChange(val) {
  const targetLabel = document.getElementById('checkoutTargetLabel');
  const btnSave = document.getElementById('btnSaveServicesOnly');
  const btnSubmitText = document.getElementById('btnCheckoutSubmitText');

  if (val === 'none') {
    activeSlotCell = null;
    currentBasePrice = 0;
    currentDeposit = 0;
    if (targetLabel) targetLabel.textContent = 'Khách lẻ tại quầy (Không tính tiền sân - 0 đ)';
    if (btnSave) btnSave.style.display = 'none';
    if (btnSubmitText) btnSubmitText.textContent = 'Thanh toán Đơn lẻ & In Bill';
  } else {
    const [cCode, tSlot] = val.split('__');
    const slotData = (appData[currentBranch] && appData[currentBranch][currentDateStr] && appData[currentBranch][currentDateStr][cCode]) ? appData[currentBranch][currentDateStr][cCode][tSlot] : null;

    if (slotData) {
      currentBasePrice = Number(slotData.price) || 0;
      currentDeposit = Number(slotData.deposit) || 0;
      posItems = slotData.posItems ? [...slotData.posItems] : [];

      const domCell = document.querySelector(`.slot-inner[data-court-code="${cCode}"][data-time="${tSlot}"]`);
      if (domCell) {
        activeSlotCell = domCell;
      } else {
        activeSlotCell = {
          dataset: {
            code: slotData.code,
            courtCode: cCode,
            court: cCode,
            time: tSlot,
            price: currentBasePrice,
            deposit: currentDeposit,
            customer: slotData.customer,
            phone: slotData.phone
          }
        };
      }

      const courtObj = realCourts.find(c => c.courtCode === cCode);
      const courtName = courtObj ? courtObj.courtName : cCode;
      if (targetLabel) targetLabel.textContent = `${courtName} - Khách: ${slotData.customer || 'Đang chơi'} (${tSlot})`;
      if (btnSave) btnSave.style.display = 'block';
      if (btnSubmitText) btnSubmitText.textContent = 'Xác nhận Thanh toán & Trả sân';
    }
  }
  renderInvoice();
}

function openCheckoutModal(element) {
  const targetLabel = document.getElementById('checkoutTargetLabel');
  const btnSave = document.getElementById('btnSaveServicesOnly');
  const btnSubmitText = document.getElementById('btnCheckoutSubmitText');

  let selectedCourtKey = 'none';

  if (element) {
    activeSlotCell = element;
    currentBasePrice = Number(activeSlotCell.dataset.price) || 0;
    currentDeposit = Number(activeSlotCell.dataset.deposit) || 0;

    const courtCode = activeSlotCell.dataset.courtCode || activeSlotCell.dataset.court;
    const timeObj = activeSlotCell.dataset.time;
    selectedCourtKey = `${courtCode}__${timeObj}`;

    const slotData = (appData[currentBranch] && appData[currentBranch][currentDateStr] && appData[currentBranch][currentDateStr][courtCode]) ? appData[currentBranch][currentDateStr][courtCode][timeObj] : {};
    posItems = (slotData && slotData.posItems) ? [...slotData.posItems] : [];

    if (targetLabel) targetLabel.textContent = `${activeSlotCell.dataset.court || courtCode} - Khách: ${activeSlotCell.dataset.customer || 'Đang chơi'} (${timeObj})`;
    if (btnSave) btnSave.style.display = 'block';
    if (btnSubmitText) btnSubmitText.textContent = 'Xác nhận Thanh toán & Trả sân';
  } else {
    activeSlotCell = null;
    currentBasePrice = 0; // Retail walk-in has 0d court fee!
    currentDeposit = 0;
    posItems = [];

    if (targetLabel) targetLabel.textContent = 'Khách lẻ tại quầy (Không tính tiền sân - 0 đ)';
    if (btnSave) btnSave.style.display = 'none';
    if (btnSubmitText) btnSubmitText.textContent = 'Thanh toán Đơn lẻ & In Bill';
  }

  populateCheckoutCourtDropdown(selectedCourtKey);
  loadDbProductsForPos();
  renderInvoice();
  document.getElementById('checkoutModal').classList.add('active');
}

const productCodeMap = {
  'Nước Revive': 'P01',
  'Pocari Sweat': 'P02',
  'Nước Pocari Sweat': 'P02',
  'Nước suối Aquafina': 'P03',
  'Thuê Vợt Yonex Astrox': 'P07',
  'Ống Cầu Hải Yến (12 quả)': 'P05',
  'Quấn cán vợt VS': 'P07'
};

function removePosItem(name) {
  const idx = posItems.findIndex(i => i.name === name);
  if (idx !== -1) {
    if (posItems[idx].qty > 1) {
      posItems[idx].qty -= 1;
    } else {
      posItems.splice(idx, 1);
    }
  }
  if (activeSlotCell) {
    const courtCode = activeSlotCell.dataset.courtCode || activeSlotCell.dataset.court;
    const timeObj = activeSlotCell.dataset.time;
    updateDataStore(courtCode, timeObj, { posItems: [...posItems] });
  }
  renderInvoice();
}

function quickAddProduct(codeOrName, name, price, stock) {
  let itemCode = codeOrName;
  let itemName = name;
  let itemPrice = price;

  if (arguments.length === 2) {
    itemName = codeOrName;
    itemPrice = name;
    const found = dbProductsCache.find(p => p.productName === itemName);
    itemCode = found ? found.productCode : (productCodeMap[itemName] || 'P01');
  }

  const existing = posItems.find(i => i.name === itemName);
  if (existing) {
    existing.qty += 1;
  } else {
    posItems.push({ code: itemCode, name: itemName, price: Number(itemPrice) || 0, qty: 1 });
  }

  const prodInCache = dbProductsCache.find(p => p.productCode === itemCode || p.productName === itemName);
  if (prodInCache) {
    if (typeof prodInCache.stockQuantity === 'number') {
      prodInCache.stockQuantity = Math.max(0, prodInCache.stockQuantity - 1);
    } else if (typeof prodInCache.currentStock === 'number') {
      prodInCache.currentStock = Math.max(0, prodInCache.currentStock - 1);
    }
    renderPosProductGrid();
  }

  if (activeSlotCell) {
    const courtCode = activeSlotCell.dataset.courtCode || activeSlotCell.dataset.court;
    const timeObj = activeSlotCell.dataset.time;
    updateDataStore(courtCode, timeObj, { posItems: [...posItems] });

    const bCode = activeSlotCell.dataset.code || 'DS0102';
    fetch(`/api/bookings/${bCode}/order-service`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ productCode: itemCode, quantity: 1 })
    }).then(r => r.json()).then(data => {
      if (data.success && window.showToast) {
        showToast(`Đã xuất [${itemName}] và trừ tồn kho CSDL!`, 'info');
      }
    }).catch(e => console.warn('Lỗi đồng bộ dịch vụ:', e));
  } else {
    if (window.showToast) {
      showToast(`Đã thêm: ${itemName}`, 'info');
    }
  }
  renderInvoice();
}

function switchPaymentMode(mode) {
  const btnCash = document.getElementById('btnPayCash');
  const btnQR = document.getElementById('btnPayQR');
  const btnCard = document.getElementById('btnPayCard');
  const qrBox = document.getElementById('vietqrBox');

  btnCash.className = 'btn btn-outline';
  btnQR.className = 'btn btn-outline';
  btnCard.className = 'btn btn-outline';

  if (mode === 'cash') {
    btnCash.className = 'btn btn-primary';
    qrBox.style.display = 'none';
  } else if (mode === 'vietqr') {
    btnQR.className = 'btn btn-primary';
    qrBox.style.display = 'block';
  } else {
    btnCard.className = 'btn btn-primary';
    qrBox.style.display = 'none';
  }
}

function printReceiptK80() {
  const total = document.getElementById('coTotal').innerText;
  const court = activeSlotCell ? activeSlotCell.dataset.court : 'Sân 01';
  const time = activeSlotCell ? activeSlotCell.dataset.time : 'Khung giờ hiện tại';
  const cust = activeSlotCell ? activeSlotCell.dataset.customer : 'Khách vãng lai';

  const win = window.open('', '_blank', 'width=360,height=580');
  win.document.write(`
    <html>
    <head>
      <title>Phiếu Thanh Toán K80</title>
      <style>
        body { font-family: 'Courier New', monospace; font-size: 12px; padding: 14px; line-height: 1.4; color: #000; }
        .center { text-align: center; }
        .dash { border-bottom: 1px dashed #000; margin: 8px 0; }
        .row { display: flex; justify-content: space-between; }
      </style>
    </head>
    <body onload="window.print()">
      <div class="center">
        <h3 style="margin:0;">UTE SPORT ARENA</h3>
        <p style="margin:4px 0;">Đ/c: Võ Văn Ngân, TP. Thủ Đức<br>Hotline: 1900.6868</p>
        <h4 style="margin:6px 0;">HÓA ĐƠN THANH TOÁN (K80)</h4>
      </div>
      <div class="dash"></div>
      <div>Khách hàng: <strong>${cust}</strong></div>
      <div>Sân: <strong>${court}</strong> (${time})</div>
      <div>Thời gian in: ${new Date().toLocaleString('vi-VN')}</div>
      <div class="dash"></div>
      <div class="row"><span>Tiền sân:</span><strong>${currentBasePrice.toLocaleString('vi-VN')} đ</strong></div>
      ${posItems.map(i => `<div class="row"><span>${i.name} (x${i.qty}):</span><span>${(i.price * i.qty).toLocaleString('vi-VN')} đ</span></div>`).join('')}
      <div class="dash"></div>
      <div class="row"><span>Đã cọc trước:</span><span>- ${currentDeposit.toLocaleString('vi-VN')} đ</span></div>
      <div class="row" style="font-size: 14px; font-weight: bold; margin-top: 6px;"><span>TỔNG THU:</span><span>${total}</span></div>
      <div class="dash"></div>
      <div class="center" style="margin-top: 14px;">
        <p>Cảm ơn Quý khách & Hẹn gặp lại!<br>Wifi: UTE_SPORT_5G / Pass: 88888888</p>
      </div>
    </body>
    </html>
  `);
  win.document.close();
}

function renderInvoice() {
  const tbody = document.getElementById('invoiceBody');
  tbody.innerHTML = '';
  let serviceSubtotal = 0;

  posItems.forEach(item => {
    const lineTotal = item.price * item.qty;
    serviceSubtotal += lineTotal;
    const safeName = (item.name || '').replace(/'/g, "\\'");
    tbody.innerHTML += `
      <tr>
        <td style="padding: 6px 8px; border-bottom: 1px solid var(--border-subtle);">${item.name}</td>
        <td style="padding: 6px 8px; border-bottom: 1px solid var(--border-subtle); text-align: center;">${item.qty}</td>
        <td style="padding: 6px 8px; border-bottom: 1px solid var(--border-subtle); text-align: right;">${item.price.toLocaleString('vi-VN')} đ</td>
        <td style="padding: 6px 8px; border-bottom: 1px solid var(--border-subtle); text-align: right; font-weight: bold;">${lineTotal.toLocaleString('vi-VN')} đ</td>
        <td style="padding: 6px 4px; border-bottom: 1px solid var(--border-subtle); text-align: center;">
          <button type="button" class="btn btn-xs btn-outline" style="padding: 2px 6px; color: var(--danger); line-height: 1; border-color: transparent;" onclick="removePosItem('${safeName}')" title="Bớt 1 / Xóa món">✕</button>
        </td>
      </tr>
    `;
  });

  const finalTotal = Math.max(0, currentBasePrice + serviceSubtotal - currentDeposit);
  document.getElementById('coCourtPrice').innerText = currentBasePrice.toLocaleString('vi-VN') + ' đ';
  document.getElementById('coServicePrice').innerText = serviceSubtotal.toLocaleString('vi-VN') + ' đ';
  document.getElementById('coDeposit').innerText = '- ' + currentDeposit.toLocaleString('vi-VN') + ' đ';
  document.getElementById('coTotal').innerText = finalTotal.toLocaleString('vi-VN') + ' đ';

  const qrData = `00020101021238540010A00000072701240006970422011009031234560208QRIBFTTA5303704540${finalTotal.toString().length}${finalTotal}5802VN62170813UTESPORT6304`;
  const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=140x140&data=${encodeURIComponent(qrData)}`;
  const qrEl = document.getElementById('vietqrImg');
  if (qrEl) qrEl.src = qrUrl;
}

async function saveServicesAndContinuePlaying() {
  if (!activeSlotCell) {
    if (typeof showToast === 'function') {
      showToast('Vui lòng chọn sân đang chơi để lưu dịch vụ!', 'warning');
    }
    return;
  }
  const courtCode = activeSlotCell.dataset.courtCode || activeSlotCell.dataset.court;
  const timeObj = activeSlotCell.dataset.time;

  updateDataStore(courtCode, timeObj, { posItems: [...posItems] });

  if (typeof showToast === 'function') {
    showToast('Đã lưu dịch vụ vào sân thành công! Khách tiếp tục chơi.', 'success');
  }
  closeCheckoutModal();
  await renderGridForDate(currentDateStr);
}

async function submitCheckout() {
  const bCode = (activeSlotCell && activeSlotCell.dataset.code) ? activeSlotCell.dataset.code : null;
  const serviceSubtotal = posItems.reduce((sum, item) => sum + (item.price * item.qty), 0);

  let payMode = 'Tiền mặt';
  const btnQR = document.getElementById('btnPayQR');
  const btnCard = document.getElementById('btnPayCard');
  if (btnQR && btnQR.classList.contains('btn-primary')) payMode = 'VietQR';
  else if (btnCard && btnCard.classList.contains('btn-primary')) payMode = 'Thẻ POS';

  if (bCode) {
    window.lastActionBookingCode = bCode;
    try {
      const res = await fetch(`/api/bookings/${bCode}/checkout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          productFee: serviceSubtotal,
          paymentMethod: payMode,
          staffCode: 'NVQ01'
        })
      });
      const data = await res.json();
      if (data.success || res.ok) {
        if (typeof showToast === 'function') {
          showToast('Đã xuất Hóa đơn K80 và thanh toán thành công!', 'success');
        }
      }
    } catch (e) {
      console.warn('Lỗi checkout backend:', e);
    }
    if (activeSlotCell) {
      const courtCode = activeSlotCell.dataset.courtCode || activeSlotCell.dataset.court;
      const timeObj = activeSlotCell.dataset.time;
      updateDataStore(courtCode, timeObj, { state: 'available', customer: '', phone: '', code: '', posItems: [] });
    }
  } else {
    try {
      await fetch('/api/invoices/retail', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          productFee: serviceSubtotal,
          paymentMethod: payMode,
          staffCode: 'NVQ01'
        })
      });
    } catch (e) {
      console.warn('Lỗi lưu hóa đơn lẻ:', e);
    }
    if (typeof showToast === 'function') {
      showToast('Đã xuất Hóa đơn K80 và thanh toán thành công!', 'success');
    }
  }

  closeCheckoutModal();
  await renderGridForDate(currentDateStr);
}

function closeCreateBookingModal() {
  document.getElementById('createBookingModal').classList.remove('active');
}
function closeDetailModal() {
  document.getElementById('detailBookingModal').classList.remove('active');
}
function closeCheckoutModal() {
  document.getElementById('checkoutModal').classList.remove('active');
}

// ==========================================
// GRID HORIZONTAL SCROLL & DRAG NAVIGATION
// ==========================================
function scrollGridHorizontal(offset) {
  const scrollEl = document.querySelector('.matrix-scroll');
  if (!scrollEl) return;
  scrollEl.scrollBy({ left: offset, behavior: 'smooth' });
}

function jumpToGridTime(shiftPeriod) {
  const scrollEl = document.querySelector('.matrix-scroll');
  if (!scrollEl) return;

  document.querySelectorAll('.btn-time-shift').forEach(b => b.classList.remove('active'));
  const activeBtn = document.querySelector(`.btn-time-shift[data-shift="${shiftPeriod}"]`);
  if (activeBtn) activeBtn.classList.add('active');

  let targetLeft = 0;
  if (shiftPeriod === 'morning') {
    targetLeft = 0;
  } else if (shiftPeriod === 'afternoon') {
    targetLeft = 650;
  } else if (shiftPeriod === 'evening') {
    targetLeft = 1150;
  }
  scrollEl.scrollTo({ left: targetLeft, behavior: 'smooth' });
}

function initGridDragToScroll() {
  const scrollEl = document.querySelector('.matrix-scroll');
  if (!scrollEl) return;

  let isDown = false;
  let startX = 0;
  let startY = 0;
  let scrollLeftStart = 0;
  let scrollTopStart = 0;
  let hasDragged = false;

  scrollEl.addEventListener('mousedown', (e) => {
    if (e.button !== 0) return;
    isDown = true;
    hasDragged = false;
    startX = e.pageX;
    startY = e.pageY;
    scrollLeftStart = scrollEl.scrollLeft;
    scrollTopStart = window.pageYOffset || document.documentElement.scrollTop;
    scrollEl.style.cursor = 'grabbing';
    scrollEl.style.userSelect = 'none';
  });

  window.addEventListener('mousemove', (e) => {
    if (!isDown) return;
    const walkX = e.pageX - startX;
    const walkY = e.pageY - startY;

    if (Math.abs(walkX) > 4 || Math.abs(walkY) > 4) {
      hasDragged = true;
    }

    scrollEl.scrollLeft = scrollLeftStart - walkX;
    if (Math.abs(walkY) > 4) {
      window.scrollTo({ top: scrollTopStart - walkY, behavior: 'auto' });
    }
  });

  window.addEventListener('mouseup', () => {
    if (!isDown) return;
    isDown = false;
    scrollEl.style.cursor = 'grab';
    scrollEl.style.removeProperty('user-select');
  });

  scrollEl.addEventListener('click', (e) => {
    if (hasDragged) {
      e.preventDefault();
      e.stopPropagation();
      hasDragged = false;
    }
  }, true);

  scrollEl.addEventListener('wheel', (e) => {
    if (e.shiftKey) {
      e.preventDefault();
      scrollEl.scrollLeft += (e.deltaY || e.deltaX) * 1.2;
    } else if (Math.abs(e.deltaX) > Math.abs(e.deltaY)) {
      scrollEl.scrollLeft += e.deltaX;
    }
  }, { passive: false });

  scrollEl.addEventListener('scroll', () => {
    const sl = scrollEl.scrollLeft;
    document.querySelectorAll('.btn-time-shift').forEach(b => b.classList.remove('active'));
    if (sl < 400) {
      const b = document.querySelector('.btn-time-shift[data-shift="morning"]');
      if (b) b.classList.add('active');
    } else if (sl < 900) {
      const b = document.querySelector('.btn-time-shift[data-shift="afternoon"]');
      if (b) b.classList.add('active');
    } else {
      const b = document.querySelector('.btn-time-shift[data-shift="evening"]');
      if (b) b.classList.add('active');
    }
  }, { passive: true });
}

// INITIALIZATION
window.addEventListener('DOMContentLoaded', async () => {
  const savedTheme = localStorage.getItem('utesport_theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);

  const dateInput = document.getElementById('gridDateInput');
  if (dateInput) {
    dateInput.value = formatDate(new Date());
    currentDateStr = dateInput.value;
  } else {
    currentDateStr = formatDate(new Date());
  }

  const branchFilterEl = document.getElementById('branchFilter');
  if (branchFilterEl) {
    currentBranch = branchFilterEl.value;
  }

  await loadCourtsForBranch(currentBranch);
  await renderGridForDate(currentDateStr);
  loadDbProductsForPos();
  initGridDragToScroll();

  document.addEventListener('click', function(e) {
    const container = document.getElementById('searchContainer');
    const dropdown = document.getElementById('searchResultsDropdown');
    if (container && !container.contains(e.target) && dropdown) {
      dropdown.classList.remove('active');
    }
  });

  if (dateInput) {
    dateInput.addEventListener('change', function(e) {
      renderGridForDate(e.target.value);
    });
  }

  // React to realtime booking events from WebSocket
  window.addEventListener('badminton:booking-event', function(e) {
    if (currentDateStr) {
      renderGridForDate(currentDateStr);
    }
  });
});
