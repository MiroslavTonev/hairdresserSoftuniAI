/* owner.js — owner dashboard: list, create, edit salons */

const ROLE_KEY = 'role';
const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
const DAY_LABELS = { MON: 'Monday', TUE: 'Tuesday', WED: 'Wednesday',
                     THU: 'Thursday', FRI: 'Friday', SAT: 'Saturday', SUN: 'Sunday' };

let miniMap = null;
let miniMarker = null;

// ===== Role toggle =====
function getRole() { return localStorage.getItem(ROLE_KEY) || 'client'; }
function setRole(role) { localStorage.setItem(ROLE_KEY, role); applyRole(); }
function applyRole() {
    const btn = document.getElementById('roleBtn');
    if (btn) btn.textContent = getRole();
}
function toggleRole() { setRole(getRole() === 'owner' ? 'client' : 'owner'); }

// ===== Salon list =====
async function loadSalonList() {
    const list = document.getElementById('salonList');
    list.innerHTML = '';
    try {
        const salons = await api.getSalons();
        if (salons.length === 0) {
            list.innerHTML = '<li style="color:#888;font-size:.9rem">No salons yet.</li>';
            return;
        }
        salons.forEach(salon => {
            const li = document.createElement('li');
            li.className = 'salon-list-item';
            li.innerHTML = `
                <div>
                    <div class="salon-item-name">${salon.name}</div>
                    <div class="salon-item-addr">${salon.address || ''}</div>
                </div>
                <button class="btn-secondary" data-id="${salon.id}">Edit</button>
            `;
            li.querySelector('button').addEventListener('click', () => openEditForm(salon.id));
            list.appendChild(li);
        });
    } catch (err) {
        list.innerHTML = `<li class="error-msg">Failed to load salons.</li>`;
    }
}

// ===== Working hours grid =====
function buildWorkingHoursGrid(existing = []) {
    const grid = document.getElementById('workingHoursGrid');
    grid.innerHTML = '';
    const map = {};
    existing.forEach(wh => { map[wh.dayOfWeek] = wh; });

    DAYS.forEach(day => {
        const wh = map[day];
        const row = document.createElement('div');
        row.className = 'wh-row';
        row.dataset.day = day;
        const checked = !!wh;
        row.innerHTML = `
            <input type="checkbox" class="wh-check" ${checked ? 'checked' : ''}>
            <span class="day-label">${DAY_LABELS[day]}</span>
            <input type="time" class="wh-open" value="${wh ? wh.openTime.slice(0,5) : '09:00'}" ${!checked ? 'disabled' : ''}>
            <input type="time" class="wh-close" value="${wh ? wh.closeTime.slice(0,5) : '18:00'}" ${!checked ? 'disabled' : ''}>
        `;
        const cb = row.querySelector('.wh-check');
        const open = row.querySelector('.wh-open');
        const close = row.querySelector('.wh-close');
        cb.addEventListener('change', () => {
            open.disabled = !cb.checked;
            close.disabled = !cb.checked;
        });
        grid.appendChild(row);
    });
}

function collectWorkingHours() {
    const rows = document.querySelectorAll('.wh-row');
    const result = [];
    rows.forEach(row => {
        const cb = row.querySelector('.wh-check');
        if (!cb.checked) return;
        result.push({
            dayOfWeek: row.dataset.day,
            openTime: row.querySelector('.wh-open').value,
            closeTime: row.querySelector('.wh-close').value,
        });
    });
    return result;
}

// ===== Reverse geocoding =====
async function reverseGeocode(lat, lng) {
    const addrField = document.getElementById('salonAddress');
    addrField.value = 'Fetching address…';
    try {
        const res = await fetch(
            `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json`,
            { headers: { 'Accept-Language': 'en' } }
        );
        if (!res.ok) throw new Error('Nominatim error');
        const data = await res.json();
        addrField.value = data.display_name || `${lat.toFixed(5)}, ${lng.toFixed(5)}`;
    } catch {
        addrField.value = `${lat.toFixed(5)}, ${lng.toFixed(5)}`;
    }
}

function updatePin(lat, lng) {
    document.getElementById('salonLat').value = lat.toFixed(6);
    document.getElementById('salonLng').value = lng.toFixed(6);
    reverseGeocode(lat, lng);
}

// ===== Mini map =====
function initMiniMap(lat, lng) {
    if (miniMap) { miniMap.remove(); miniMap = null; miniMarker = null; }

    miniMap = new maplibregl.Map({
        container: 'miniMap',
        style: 'https://tiles.openfreemap.org/styles/liberty',
        center: [lng || 23.3219, lat || 42.6977],
        zoom: 12,
    });

    if (lat && lng) {
        miniMarker = new maplibregl.Marker({ draggable: true })
            .setLngLat([lng, lat])
            .addTo(miniMap);
        miniMarker.on('dragend', () => {
            const pos = miniMarker.getLngLat();
            updatePin(pos.lat, pos.lng);
        });
    }

    miniMap.on('click', (e) => {
        const { lng: clickLng, lat: clickLat } = e.lngLat;
        updatePin(clickLat, clickLng);
        if (!miniMarker) {
            miniMarker = new maplibregl.Marker({ draggable: true })
                .setLngLat([clickLng, clickLat])
                .addTo(miniMap);
            miniMarker.on('dragend', () => {
                const pos = miniMarker.getLngLat();
                updatePin(pos.lat, pos.lng);
            });
        } else {
            miniMarker.setLngLat([clickLng, clickLat]);
        }
    });
}

// ===== Form open/close =====
function showForm() { document.getElementById('salonFormSection').classList.remove('hidden'); }
function hideForm() {
    document.getElementById('salonFormSection').classList.add('hidden');
    document.getElementById('salonForm').reset();
    document.getElementById('salonId').value = '';
    if (miniMap) { miniMap.remove(); miniMap = null; miniMarker = null; }
}

function openNewForm() {
    document.getElementById('formTitle').textContent = 'New Salon';
    document.getElementById('salonId').value = '';
    buildWorkingHoursGrid();
    showForm();
    setTimeout(() => initMiniMap(null, null), 100);
}

async function openEditForm(id) {
    try {
        const salon = await api.getSalon(id);
        document.getElementById('formTitle').textContent = 'Edit Salon';
        document.getElementById('salonId').value = salon.id;
        document.getElementById('salonName').value = salon.name;
        document.getElementById('salonAddress').value = salon.address || '';
        document.getElementById('salonLat').value = salon.lat;
        document.getElementById('salonLng').value = salon.lng;
        buildWorkingHoursGrid(salon.workingHours || []);
        showForm();
        setTimeout(() => initMiniMap(salon.lat, salon.lng), 100);
    } catch (err) {
        alert('Failed to load salon.');
    }
}

// ===== Form submit =====
async function handleFormSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('salonId').value;
    const payload = {
        name: document.getElementById('salonName').value.trim(),
        address: document.getElementById('salonAddress').value.trim(),
        lat: parseFloat(document.getElementById('salonLat').value),
        lng: parseFloat(document.getElementById('salonLng').value),
        workingHours: collectWorkingHours(),
    };

    try {
        if (id) {
            await api.updateSalon(id, payload);
        } else {
            await api.createSalon(payload);
        }
        hideForm();
        await loadSalonList();
    } catch (err) {
        alert('Save failed: ' + err.message);
    }
}

// ===== Init =====
document.addEventListener('DOMContentLoaded', () => {
    applyRole();
    document.getElementById('roleBtn').addEventListener('click', toggleRole);
    document.getElementById('addSalonBtn').addEventListener('click', openNewForm);
    document.getElementById('cancelFormBtn').addEventListener('click', hideForm);
    document.getElementById('salonForm').addEventListener('submit', handleFormSubmit);
    loadSalonList();
});
