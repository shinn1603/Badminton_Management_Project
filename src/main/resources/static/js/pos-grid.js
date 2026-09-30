/**
 * UTE SPORT - POS GRID REALTIME LOGIC
 * Module: Lễ tân & Bán hàng POS (MH-NVQ01 -> MH-NVQ07)
 */

let appData = {};
let currentBranch = 'CN01';
let currentDateStr = '';
let activeSlotCell = null;
let posItems = [];
let currentBasePrice = 120000;
let currentDeposit = 36000;

const mockNames = ['Lê Bá Đạt', 'Trần Nam', 'Đoàn Thanh Niên', 'Vũ Quốc Cường', 'Nguyễn Hải', 'Nhóm IT UTE', 'CLB Tân Bình', 'Khách vãng lai', 'Chị Lan', 'Anh Hoàng Vũ'];
const mockPhones = ['0901234567', '0988777666', '0912345678', '0933444555', '0909888777'];
const timeSlots = ['06:00 - 07:00', '07:00 - 08:00', '08:00 - 09:00', '09:00 - 10:00', '10:00 - 11:00', '14:00 - 15:00', '15:00 - 16:00', '17:00 - 18:00', '18:00 - 19:00', '19:00 - 20:00', '20:00 - 21:00', '21:00 - 22:00'];
let courtsList = ['Sân 01 (VIP)', 'Sân 02', 'Sân 03', 'Sân 04', 'Sân Pickleball 01', 'Sân Pickleball 02'];
const branchesList = ['CN01', 'CN02', 'CN03'];

function formatDate(dateObj) {
  const yyyy = dateObj.getFullYear();
  const mm = String(dateObj.getMonth() + 1).padStart(2, '0');
  const dd = String(dateObj.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

// Khởi tạo dữ liệu thời gian thực cho ngày truy cập
function ensureDataForDate(dateStr) {
  const todayStr = formatDate(new Date());

  branchesList.forEach(branch => {
    if (!appData[branch]) appData[branch] = {};
    if (!appData[branch][dateStr]) {
      appData[branch][dateStr] = {};

      courtsList.forEach(court => {
        appData[branch][dateStr][court] = {};
        let price = court.includes('VIP') || court.includes('Pickleball') ? 120000 : 90000;
        let hasInUse = false;

        timeSlots.forEach(time => {
          let rand = Math.random();
          let state = 'available';
          let customer = '', phone = '';

          if (dateStr < todayStr) {
            state = rand > 0.6 ? 'completed' : 'available';
          } else if (dateStr === todayStr) {
            if (!hasInUse && rand < 0.15) {
              state = 'in-use';
              hasInUse = true;
            } else if (rand < 0.4) {
              state = 'booked';
            }
          } else {
            state = rand > 0.7 ? 'booked' : 'available';
          }

          if (state !== 'available') {
            customer = mockNames[Math.floor(Math.random() * mockNames.length)];
            phone = mockPhones[Math.floor(Math.random() * mockPhones.length)];
          }

          appData[branch][dateStr][court][time] = {
            state: state, customer: customer, phone: phone, price: price, deposit: price * 0.3, posItems: []
          };
        });
      });
    }
  });
}

function initAppData() {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  for (let i = -15; i <= 15; i++) {
    let d = new Date(today);
    d.setDate(d.getDate() + i);
    ensureDataForDate(formatDate(d));
  }
}

function updateDataStore(court, time, updateObj) {
  if (!appData[currentBranch][currentDateStr][court]) appData[currentBranch][currentDateStr][court] = {};
  if (!appData[currentBranch][currentDateStr][court][time]) {
    appData[currentBranch][currentDateStr][court][time] = { price: 90000 };
  }
  Object.assign(appData[currentBranch][currentDateStr][court][time], updateObj);
}

function renderGridForDate(dateStr) {
  ensureDataForDate(dateStr);
  currentDateStr = dateStr;
  currentBranch = document.getElementById('branchFilter') ? document.getElementById('branchFilter').value : 'CN01';
  const tbody = document.getElementById('matrixGridBody');
  if (!tbody) return;
  tbody.innerHTML = '';

  let gridState = appData[currentBranch][dateStr];
  const todayStr = formatDate(new Date());

  courtsList.forEach(court => {
    const isVip = court.includes('VIP');
    const isPickleball = court.includes('Pickleball');
    const defaultPrice = isVip || isPickleball ? 120000 : 90000;

    let rowHtml = `
      <tr>
        <td class="matrix-td-court">
          <div class="court-info">
            <span class="court-name">${court}</span>
            <span class="court-type">${isVip ? 'VIP - Thảm Yonex' : (isPickleball ? 'Sân Pickleball' : 'Tiêu chuẩn - Taraflex')}</span>
          </div>
        </td>`;

    timeSlots.forEach(timeObj => {
      const [startTime, endTime] = timeObj.split(' - ');
      let cellData = gridState[court][timeObj] || { state: 'available', price: defaultPrice };

      rowHtml += `<td class="matrix-slot-cell">`;
      if (cellData.state === 'available') {
        if (dateStr < todayStr) {
          rowHtml += `
            <div class="slot-inner" style="background-color: var(--surface-subtle); border: 1px dashed var(--border-subtle); color: var(--text-muted); cursor: not-allowed;" onclick="alert('Không thể đặt sân cho các ngày trong quá khứ!')">
              <span class="slot-title" style="font-weight: 500;">Hết giờ</span>
            </div>`;
        } else {
          rowHtml += `
            <div class="slot-inner state-available" data-court="${court}" data-time="${timeObj}" onclick="quickBookSlot(this, '${court}', '${startTime}', '${endTime}', ${cellData.price})">
              <div class="slot-action">
                <svg class="svg-icon" viewBox="0 0 24 24" style="width: 12px; height: 12px;"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                <span>Đặt</span>
              </div>
              <span class="slot-price">${cellData.price.toLocaleString('vi-VN')} đ</span>
            </div>`;
        }
      } else if (cellData.state === 'booked') {
        rowHtml += `
          <div class="slot-inner state-booked" data-price="${cellData.price}" data-deposit="${cellData.deposit}" data-customer="${cellData.customer}" data-phone="${cellData.phone}" data-court="${court}" data-time="${timeObj}" onclick="openDetailModal(this, 'BK-10X', '${court}', '${timeObj}', '${cellData.customer}', '${cellData.phone}', 'Đã cọc 30%', ${cellData.price}, ${cellData.deposit})">
            <span class="slot-title">${cellData.customer}</span>
            <span class="slot-badge-status badge-booked">Đã cọc 30%</span>
          </div>`;
      } else if (cellData.state === 'in-use') {
        rowHtml += `
          <div class="slot-inner state-in-use" data-price="${cellData.price}" data-deposit="${cellData.deposit}" data-customer="${cellData.customer}" data-phone="${cellData.phone}" data-court="${court}" data-time="${timeObj}" onclick="openCheckoutModal(this)">
            <span class="slot-title">${cellData.customer}</span>
            <span class="slot-badge-status badge-in-use">Đang chơi</span>
          </div>`;
      } else if (cellData.state === 'completed') {
        rowHtml += `
          <div class="slot-inner state-completed" data-court="${court}" data-time="${timeObj}">
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

function shiftDate(offset) {
  const dateInput = document.getElementById('gridDateInput');
  if (!dateInput) return;
  const curDate = new Date(dateInput.value);
  if (isNaN(curDate)) return;
  curDate.setDate(curDate.getDate() + offset);
  dateInput.value = formatDate(curDate);
  renderGridForDate(dateInput.value);
  handleSearch();
}

function goToToday() {
  const dateInput = document.getElementById('gridDateInput');
  if (dateInput) {
    dateInput.value = formatDate(new Date());
    renderGridForDate(dateInput.value);
    handleSearch();
  }
}

function changeBranch() {
  const dateInput = document.getElementById('gridDateInput');
  if (dateInput) renderGridForDate(dateInput.value);
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

// SEARCH & HIGHLIGHT
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
      for (let court in appData[branch][date]) {
        for (let time in appData[branch][date][court]) {
          const data = appData[branch][date][court][time];
          if (data.state !== 'available') {
            const matchName = data.customer && data.customer.toLowerCase().includes(query);
            const matchPhone = data.phone && data.phone.includes(query);

            if (matchName || matchPhone) {
              matchCount++;
              if (resultList) {
                const li = document.createElement('li');
                li.className = 'search-result-item';
                li.innerHTML = `
                  <div class="sri-name">${data.customer} <span class="sri-phone">(${data.phone})</span></div>
                  <div class="sri-details">
                    <span>${court}</span>
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
                  renderGridForDate(date);
                  if (dropdown) dropdown.classList.remove('active');
                  highlightExactSlot(court, time);
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

function highlightExactSlot(court, time) {
  setTimeout(() => {
    document.querySelectorAll('.slot-inner').forEach(slot => {
      const parentCourt = slot.closest('tr')?.querySelector('.court-name')?.textContent;
      const slotTime = slot.dataset.time;
      if (parentCourt === court && slotTime === time) {
        slot.classList.add('slot-highlight');
        slot.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });
      } else {
        slot.classList.add('slot-dimmed');
      }
    });
  }, 100);
}

// BOOKING MODAL LOGIC
function openCreateBookingModal() {
  document.getElementById('bookingCustName').value = '';
  document.getElementById('bookingCustPhone').value = '';
  recalculatePrice();
  document.getElementById('createBookingModal').classList.add('active');
}

function quickBookSlot(element, court, startTime, endTime, price) {
  const courtSelect = document.getElementById('bookingCourtSelect');
  if (courtSelect) courtSelect.value = court;
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
  if (!courtSelect) return;
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
  const deposit = Math.round(total * 0.3);

  document.getElementById('displayTotalRental').textContent = total.toLocaleString('vi-VN') + ' đ';
  document.getElementById('displayDepositRequired').textContent = deposit.toLocaleString('vi-VN') + ' đ';
}

function submitBookingForm() {
  const name = document.getElementById('bookingCustName').value.trim();
  const phone = document.getElementById('bookingCustPhone').value.trim();
  if (!name || !phone) return alert('Vui lòng nhập họ tên và số điện thoại.');

  const courtName = document.getElementById('bookingCourtSelect').value;
  const startTime = document.getElementById('bookingStartTime').value;
  const endTime = document.getElementById('bookingEndTime').value;

  if (startTime >= endTime) return alert('Giờ kết thúc phải diễn ra sau giờ bắt đầu!');

  let targetSlots = [];
  timeSlots.forEach(slot => {
    const [slotStart, slotEnd] = slot.split(' - ');
    if (slotStart >= startTime && slotEnd <= endTime) targetSlots.push(slot);
  });

  if (targetSlots.length === 0) return alert('Không tìm thấy khung giờ phù hợp!');

  const total = Number(document.getElementById('displayTotalRental').textContent.replace(/\D/g, ''));
  const deposit = Number(document.getElementById('displayDepositRequired').textContent.replace(/\D/g, ''));
  const pricePerSlot = total / targetSlots.length;
  const depositPerSlot = deposit / targetSlots.length;

  targetSlots.forEach(slot => {
    updateDataStore(courtName, slot, {
      state: 'booked', customer: name, phone: phone, price: pricePerSlot, deposit: depositPerSlot, posItems: []
    });
  });

  renderGridForDate(currentDateStr);
  alert(`✅ Đã đặt thành công ${targetSlots.length} khung giờ cho khách: ${name}`);
  closeCreateBookingModal();
}

function openDetailModal(element, code, court, time, customer, phone, status, price, deposit = 0) {
  activeSlotCell = element;
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

function checkInBooking() {
  if (activeSlotCell) {
    const court = activeSlotCell.dataset.court;
    const timeObj = activeSlotCell.dataset.time;
    updateDataStore(court, timeObj, { state: 'in-use' });
    renderGridForDate(currentDateStr);
  }
  closeDetailModal();
}

// CHECKOUT & POS SERVICES MODAL
function openCheckoutModal(element) {
  if (element) {
    activeSlotCell = element;
    currentBasePrice = Number(activeSlotCell.dataset.price) || 120000;
    currentDeposit = Number(activeSlotCell.dataset.deposit) || 36000;

    const court = activeSlotCell.dataset.court;
    const timeObj = activeSlotCell.dataset.time;
    const slotData = appData[currentBranch][currentDateStr][court][timeObj] || {};
    posItems = slotData.posItems ? [...slotData.posItems] : [];
  } else {
    activeSlotCell = null;
    currentBasePrice = 90000;
    currentDeposit = 0;
    posItems = [];
  }

  renderInvoice();
  document.getElementById('checkoutModal').classList.add('active');
}

function quickAddProduct(name, price) {
  const existing = posItems.find(i => i.name === name);
  if (existing) existing.qty += 1;
  else posItems.push({ name, price, qty: 1 });

  if (activeSlotCell) {
    const court = activeSlotCell.dataset.court;
    const timeObj = activeSlotCell.dataset.time;
    updateDataStore(court, timeObj, { posItems: [...posItems] });
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
    tbody.innerHTML += `
      <tr>
        <td style="padding: 6px 8px; border-bottom: 1px solid var(--border-subtle);">${item.name}</td>
        <td style="padding: 6px 8px; border-bottom: 1px solid var(--border-subtle); text-align: center;">${item.qty}</td>
        <td style="padding: 6px 8px; border-bottom: 1px solid var(--border-subtle); text-align: right;">${item.price.toLocaleString('vi-VN')} đ</td>
        <td style="padding: 6px 8px; border-bottom: 1px solid var(--border-subtle); text-align: right; font-weight: bold;">${lineTotal.toLocaleString('vi-VN')} đ</td>
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

function submitCheckout() {
  alert('✅ Đã xuất Hóa đơn K80 và thanh toán thành công!');
  if (activeSlotCell) {
    const court = activeSlotCell.dataset.court;
    const timeObj = activeSlotCell.dataset.time;
    updateDataStore(court, timeObj, { state: 'completed', deposit: 0, posItems: [] });
    renderGridForDate(currentDateStr);
  }
  closeCheckoutModal();
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

// FETCH REAL COURTS FROM SQL SERVER
async function loadCourtsFromSql() {
  try {
    const res = await fetch('/api/courts');
    if (res.ok) {
      const data = await res.json();
      if (data && data.length > 0) {
        courtsList = data.map(c => c.courtName);
        renderGridForDate(currentDateStr);
      }
    }
  } catch (e) {
    console.warn('SQL courts fallback:', e);
  }
}

// KHỞI CHẠY
window.addEventListener('DOMContentLoaded', () => {
  const savedTheme = localStorage.getItem('utesport_theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);

  initAppData();
  goToToday();
  loadCourtsFromSql();

  document.addEventListener('click', function(e) {
    const container = document.getElementById('searchContainer');
    const dropdown = document.getElementById('searchResultsDropdown');
    if (container && !container.contains(e.target) && dropdown) {
      dropdown.classList.remove('active');
    }
  });

  const gridDateInput = document.getElementById('gridDateInput');
  if (gridDateInput) {
    gridDateInput.addEventListener('change', function(e) {
      renderGridForDate(e.target.value);
    });
  }
});
