/* salon.js — booking page: weekly calendar view, slot grid, booking modal */

const ROLE_KEY = 'role';

function getRole() { return localStorage.getItem(ROLE_KEY) || 'client'; }
function setRole(role) { localStorage.setItem(ROLE_KEY, role); applyRole(); }
function applyRole() {
    const btn = document.getElementById('roleBtn');
    if (btn) btn.textContent = getRole();
}
function toggleRole() { setRole(getRole() === 'owner' ? 'client' : 'owner'); }

// ===== Read salonId from URL =====
function getSalonId() {
    return new URLSearchParams(window.location.search).get('id');
}

// ===== Week helpers =====
let weekOffset = 0; // 0 = current week, 1 = next week, etc.

function getMondayOfCurrentWeek() {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const day = today.getDay(); // 0=Sun
    const diff = day === 0 ? -6 : 1 - day;
    today.setDate(today.getDate() + diff);
    return today;
}

function getWeekDates(offset) {
    const monday = getMondayOfCurrentWeek();
    monday.setDate(monday.getDate() + offset * 7);
    return Array.from({ length: 7 }, (_, i) => {
        const d = new Date(monday);
        d.setDate(monday.getDate() + i);
        return d;
    });
}

function formatDate(d) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
}

const DAY_NAMES  = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                     'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function formatDayHeader(d) {
    return `${DAY_NAMES[d.getDay()]} ${d.getDate()} ${MONTH_NAMES[d.getMonth()]}`;
}

function formatWeekRange(weekDates) {
    const s = weekDates[0], e = weekDates[6];
    return `${s.getDate()} ${MONTH_NAMES[s.getMonth()]} – ${e.getDate()} ${MONTH_NAMES[e.getMonth()]} ${e.getFullYear()}`;
}

// ===== Load and render week =====
async function loadWeek(salonId) {
    const weekDates = getWeekDates(weekOffset);

    document.getElementById('weekRangeLabel').textContent = formatWeekRange(weekDates);
    document.getElementById('prevWeekBtn').disabled = weekOffset <= 0;

    const container = document.getElementById('weekGrid');
    container.innerHTML = '<p class="slots-hint">Loading week…</p>';

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const slotsPerDay = await Promise.all(
        weekDates.map(d =>
            api.getAvailability(salonId, formatDate(d)).catch(() => [])
        )
    );

    renderWeekGrid(salonId, weekDates, slotsPerDay, today);
}

function renderWeekGrid(salonId, weekDates, slotsPerDay, today) {
    const container = document.getElementById('weekGrid');
    container.innerHTML = '';

    const todayStr = formatDate(today);

    weekDates.forEach((d, i) => {
        const col = document.createElement('div');
        col.className = 'week-day-col';

        const dStr = formatDate(d);
        const isPast   = d < today;
        const isToday  = dStr === todayStr;
        if (isToday) col.classList.add('week-day-today');
        if (isPast)  col.classList.add('week-day-past');

        const header = document.createElement('div');
        header.className = 'week-day-header';
        header.textContent = formatDayHeader(d);
        col.appendChild(header);

        const slots = slotsPerDay[i];

        if (isPast) {
            const msg = document.createElement('p');
            msg.className = 'week-day-msg';
            msg.textContent = '–';
            col.appendChild(msg);
        } else if (slots.length === 0) {
            const msg = document.createElement('p');
            msg.className = 'week-day-msg';
            msg.textContent = 'No slots';
            col.appendChild(msg);
        } else {
            const slotList = document.createElement('div');
            slotList.className = 'week-slot-list';
            slots.forEach(time => {
                const btn = document.createElement('button');
                btn.className = 'slot-btn';
                btn.textContent = time;
                btn.dataset.date = dStr;
                btn.dataset.time = time;
                btn.addEventListener('click', () => openModal(salonId, dStr, time));
                slotList.appendChild(btn);
            });
            col.appendChild(slotList);
        }

        container.appendChild(col);
    });
}

// ===== Next available slot banner =====
async function updateNextAvailableBanner(salonId) {
    const banner = document.getElementById('nextAvailableBanner');

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // Check 28 days ahead in parallel
    const days = Array.from({ length: 28 }, (_, i) => {
        const d = new Date(today);
        d.setDate(today.getDate() + i);
        return d;
    });

    const results = await Promise.all(
        days.map(d =>
            api.getAvailability(salonId, formatDate(d))
               .then(slots => ({ d, slots }))
               .catch(() => ({ d, slots: [] }))
        )
    );

    const first = results.find(r => r.slots.length > 0);
    if (first) {
        banner.textContent = `Next available slot: ${formatDayHeader(first.d)} at ${first.slots[0]}`;
        banner.className = 'next-available-banner na-found';
    } else {
        banner.textContent = 'No available slots in the next 28 days.';
        banner.className = 'next-available-banner na-empty';
    }
}

// ===== Booking modal =====
let pendingBooking = null;

function openModal(salonId, date, time) {
    pendingBooking = { salonId, date, time };
    document.getElementById('modalSlotInfo').textContent = `${date} at ${time}`;
    document.getElementById('clientName').value = '';
    document.getElementById('clientContact').value = '';
    document.getElementById('bookingModal').classList.remove('hidden');
}

function closeModal() {
    document.getElementById('bookingModal').classList.add('hidden');
    pendingBooking = null;
}

async function handleBookingSubmit(e) {
    e.preventDefault();
    if (!pendingBooking) return;

    const payload = {
        salonId: Number(pendingBooking.salonId),
        date: pendingBooking.date,
        time: pendingBooking.time,
        clientName: document.getElementById('clientName').value.trim(),
        clientContact: document.getElementById('clientContact').value.trim(),
    };

    const salonId = payload.salonId;

    try {
        await api.createBooking(payload);
        closeModal();
        showToast('Booking confirmed!');
        // Mark the slot as taken in-place (keep it visible but disabled)
        markSlotTaken(payload.date, payload.time);
        updateNextAvailableBanner(salonId); // fire and forget
    } catch (err) {
        if (err.message.includes('409')) {
            alert('Sorry, this slot was just taken. Please pick another time.');
            closeModal();
            markSlotTaken(payload.date, payload.time);
        } else {
            alert('Booking failed: ' + err.message);
        }
    }
}

// ===== Mark a slot as taken in-place =====
function markSlotTaken(date, time) {
    const btn = document.querySelector(`.slot-btn[data-date="${date}"][data-time="${time}"]`);
    if (!btn) return;
    btn.classList.add('slot-btn-taken');
    btn.disabled = true;
    btn.title = 'This slot is now booked';
    btn.removeEventListener('click', btn._clickHandler);
}

// ===== Toast =====
function showToast(msg) {
    const t = document.getElementById('successToast');
    t.textContent = msg;
    t.classList.remove('hidden');
    setTimeout(() => t.classList.add('hidden'), 3000);
}

// ===== Init =====
document.addEventListener('DOMContentLoaded', async () => {
    applyRole();
    document.getElementById('roleBtn').addEventListener('click', toggleRole);
    document.getElementById('closeModalBtn').addEventListener('click', closeModal);
    document.getElementById('bookingForm').addEventListener('submit', handleBookingSubmit);

    const salonId = getSalonId();
    if (!salonId) {
        document.getElementById('salonName').textContent = 'Salon not found';
        return;
    }

    try {
        const salon = await api.getSalon(salonId);
        document.title = `${salon.name} – Hairdresser Booking`;
        document.getElementById('salonName').textContent = salon.name;
        document.getElementById('salonAddress').textContent = salon.address || '';
    } catch {
        document.getElementById('salonName').textContent = 'Salon not found';
        return;
    }

    document.getElementById('prevWeekBtn').addEventListener('click', () => {
        if (weekOffset > 0) { weekOffset--; loadWeek(salonId); }
    });
    document.getElementById('nextWeekBtn').addEventListener('click', () => {
        weekOffset++;
        loadWeek(salonId);
    });

    // Load current week and next-available banner in parallel
    await loadWeek(salonId);
    updateNextAvailableBanner(salonId); // async, updates banner when ready
});

