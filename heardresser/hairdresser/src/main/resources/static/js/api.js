/* api.js — shared fetch helpers */
const BASE = '';

async function apiFetch(path, options = {}) {
    const res = await fetch(BASE + path, {
        headers: { 'Content-Type': 'application/json', ...options.headers },
        ...options,
    });
    if (!res.ok) {
        const body = await res.text();
        throw new Error(`HTTP ${res.status}: ${body}`);
    }
    // 204 No Content has no body
    if (res.status === 204) return null;
    return res.json();
}

const api = {
    getSalons: () => apiFetch('/api/salons'),
    getSalon: (id) => apiFetch(`/api/salons/${id}`),
    createSalon: (data) => apiFetch('/api/salons', { method: 'POST', body: JSON.stringify(data) }),
    updateSalon: (id, data) => apiFetch(`/api/salons/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    getAvailability: (id, date) => apiFetch(`/api/salons/${id}/availability?date=${date}`),
    createBooking: (data) => apiFetch('/api/bookings', { method: 'POST', body: JSON.stringify(data) }),
    getBookings: (id, date) => apiFetch(`/api/salons/${id}/bookings?date=${date}`),
};
