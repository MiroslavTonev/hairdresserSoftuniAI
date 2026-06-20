# Plan: Hairdresser Booking App

## TL;DR
A locally-run hairdresser booking web app. Spring Boot (embedded Tomcat) + H2 file database as backend. Vanilla JS + HTML/CSS as frontend served as static files from Spring Boot. OpenFreeMap (MapLibre GL JS) for the interactive map. Single owner, multiple salon locations. Clients book time slots by clicking a salon on the map.

---

## Architecture

```
Browser (Vanilla JS)
   ↕ REST API (JSON)
Spring Boot (embedded Tomcat, port 8080)
   ↕ JPA/JDBC
H2 file-based DB (hairdresser.mv.db)
```

Static files served from `src/main/resources/static/`.

---

## Database Schema

**salons** (id, name, address, lat, lng)
**working_hours** (id, salon_id, day_of_week ENUM[MON..SUN], open_time TIME, close_time TIME)
**bookings** (id, salon_id, booking_date DATE, booking_time TIME, client_name VARCHAR, client_contact VARCHAR, status ENUM[ACTIVE, CANCELLED])

All slots are fixed at 30 minutes duration.

---

## REST API Endpoints

- `GET  /api/salons`                            → all salons (id, name, lat, lng) for map markers
- `GET  /api/salons/{id}`                       → salon detail + working_hours
- `POST /api/salons`                            → create salon (owner)
- `PUT  /api/salons/{id}`                       → update salon (owner)
- `GET  /api/salons/{id}/availability?date=`    → list of available time slots for a date
- `POST /api/bookings`                          → create booking (client)
- `GET  /api/salons/{id}/bookings?date=`        → all bookings for date (owner view)

---

## Frontend Pages

1. **index.html** – Full-screen map with salon markers. Click marker → navigate to salon.html?id=X
2. **owner.html** – Owner dashboard: list salons, add/edit salon (name, address, pick location on mini-map, working hours per day)
3. **salon.html?id=X** – Salon detail: calendar date picker + time slot grid. Client fills name/contact → confirms booking.

Role selection: simple top-nav toggle stored in `localStorage` (client / owner). No auth logic yet.

---

## Project Structure

```
hairdresser/
├── pom.xml
└── src/main/
    ├── java/com/hairdresser/
    │   ├── HairdresserApplication.java
    │   ├── model/
    │   │   ├── Salon.java
    │   │   ├── WorkingHours.java
    │   │   └── Booking.java
    │   ├── repository/
    │   │   ├── SalonRepository.java
    │   │   ├── WorkingHoursRepository.java
    │   │   └── BookingRepository.java
    │   ├── service/
    │   │   ├── SalonService.java
    │   │   └── BookingService.java
    │   └── controller/
    │       ├── SalonController.java
    │       └── BookingController.java
    └── resources/
        ├── application.properties
        ├── data.sql (optional seed data)
        └── static/
            ├── index.html
            ├── owner.html
            ├── salon.html
            ├── css/
            │   └── style.css
            └── js/
                ├── map.js        (MapLibre GL + marker logic)
                ├── owner.js      (owner dashboard CRUD)
                ├── salon.js      (calendar + slot grid + booking form)
                └── api.js        (shared fetch helpers)
```

---

## Implementation Phases

### Phase 1 – Project Scaffold
1. Create Maven project with Spring Boot 3 + Web + Data JPA + H2 dependencies in pom.xml
2. Write `application.properties` (H2 file mode, Hibernate DDL auto-create)
3. Define JPA entities: Salon, WorkingHours, Booking
4. Write Spring Data repositories for all three entities

### Phase 2 – Backend Services & API
5. `SalonService`: CRUD for salons + working hours
6. `BookingService`: availability calculation (generate slots from working hours, subtract existing bookings for a date)
7. `SalonController` REST endpoints (GET all, GET by id, POST, PUT)
8. `BookingController` REST endpoints (POST booking, GET availability, GET bookings by date)
9. Add CORS config to allow localhost dev

### Phase 3 – Frontend Map (index.html)
10. Load MapLibre GL JS from CDN + openfreemap style URL
11. `map.js`: fetch `/api/salons`, add a marker per salon, on-click → `salon.html?id=X`
12. Basic layout: full-screen map, small role-toggle button in top-right

### Phase 4 – Owner Dashboard (owner.html)
13. List existing salons with edit buttons
14. Form: salon name, address, pick lat/lng by clicking a mini map
15. Working hours section: per-day toggle + time range inputs
16. `owner.js`: calls POST/PUT `/api/salons`

### Phase 5 – Booking Page (salon.html)
17. Show salon name/address
18. Date picker (plain `<input type="date">`)
19. On date select → fetch `/api/salons/{id}/availability?date=` → render time slot buttons (green=free, disabled=taken)
20. On slot click → show modal with name + contact fields → POST `/api/bookings` → confirmation message
21. `salon.js`: orchestrates above

### Phase 6 – Polish & Verification
22. Seed data.sql with one demo salon and some working hours so the app opens usefully
23. Cross-browser test: open map, pin salon, book a slot, verify slot disappears
24. Owner dashboard: add/edit salon, set working hours, check calendar reflects changes

---

## Verification Steps
1. `mvn spring-boot:run` → app starts on http://localhost:8080 with no errors
2. `GET /api/salons` returns JSON array with at least seed salon
3. Map renders on index.html with marker at seed salon coordinates
4. Click marker → salon.html → pick a date → time slots appear
5. Book a slot → re-fetch availability → slot is gone
6. Owner page adds new salon → marker appears on map after refresh
7. Owner page sets working hours → availability reflects them

---

## Key Decisions

- **Spring Boot + embedded Tomcat**: simplest Java "just run it" setup, no separate server install
- **H2 file-mode DB**: zero install, persists between restarts (unlike in-memory)
- **Vanilla JS**: React not needed for this scope
- **MapLibre GL JS via CDN**: no npm/build step needed
- **Slot duration**: fixed 30 minutes per slot (one booking per slot)
- **Role toggle via localStorage**: no auth, just UI visibility control (owner vs client view)

---

## Out of Scope (Now)
- Authentication / login
- Multiple owners
- Owner cancels booking
- Notes system
- Docker / deployment
