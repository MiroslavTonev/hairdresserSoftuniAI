/* map.js — full-screen map with salon markers */

const ROLE_KEY = 'role';

function getRole() {
    return localStorage.getItem(ROLE_KEY) || 'client';
}

function setRole(role) {
    localStorage.setItem(ROLE_KEY, role);
    applyRole();
}

function applyRole() {
    const role = getRole();
    const btn = document.getElementById('roleBtn');
    const ownerLink = document.getElementById('ownerLink');
    if (btn) btn.textContent = role;
    if (ownerLink) ownerLink.classList.toggle('hidden', role !== 'owner');
}

function toggleRole() {
    setRole(getRole() === 'owner' ? 'client' : 'owner');
}

document.addEventListener('DOMContentLoaded', () => {
    applyRole();
    document.getElementById('roleBtn').addEventListener('click', toggleRole);

    const map = new maplibregl.Map({
        container: 'map',
        style: 'https://tiles.openfreemap.org/styles/liberty',
        center: [23.3219, 42.6977], // Sofia default
        zoom: 12,
    });

    map.on('load', async () => {
        try {
            const salons = await api.getSalons();
            salons.forEach(salon => {
                const el = document.createElement('div');
                el.className = 'map-marker';
                el.title = salon.name;
                el.innerHTML = '<span>✂</span>';
                el.style.cssText = `
                    background:#4a90d9;color:#fff;border-radius:50%;
                    width:36px;height:36px;display:flex;align-items:center;
                    justify-content:center;font-size:1rem;cursor:pointer;
                    box-shadow:0 2px 8px rgba(0,0,0,0.3);
                `;

                new maplibregl.Marker({ element: el })
                    .setLngLat([salon.lng, salon.lat])
                    .setPopup(
                        new maplibregl.Popup({ offset: 25 }).setHTML(
                            `<strong>${salon.name}</strong><br>
                             <small>${salon.address || ''}</small><br>
                             <a href="salon.html?id=${salon.id}" style="color:#4a90d9;font-size:0.85rem;">Book a slot →</a>`
                        )
                    )
                    .addTo(map);

                el.addEventListener('click', () => {
                    window.location.href = `salon.html?id=${salon.id}`;
                });
            });
        } catch (err) {
            console.error('Failed to load salons', err);
        }
    });
});
