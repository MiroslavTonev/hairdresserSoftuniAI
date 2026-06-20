# AGENTS.md — Hairdresser Booking App

This file provides context and rules for AI agents working on this project.
Read it in full before making any changes to the codebase.

---

## Project Goal

Build a locally-run web application for booking hairdresser appointments.

- The **owner** manages one or more salon locations: sets their position on a map, configures working days/hours per salon.
- **Clients** browse salon locations on an interactive map, select a salon, pick a date, and book a free 30-minute slot.
- Everything runs on a single machine with no external services, no Docker, no cloud.

---

## Roles (Current Phase)

There is no authentication yet. The active role is selected via a top-nav toggle and stored in `localStorage` under the key `role`.

| Value | Who | What they can do |
|---|---|---|
| `client` | Any visitor | View map, pick salon, view availability, book a slot |
| `owner` | The salon owner | View/add/edit salons, set working hours, view bookings per date |

> **Important:** Role switching is UI-only. There is no server-side authorization. Do NOT add auth logic, JWT, sessions, or Spring Security unless explicitly asked.

---

## Technology Stack

### Backend
| Concern | Choice | Notes |
|---|---|---|
| Language | Java 17+ | |
| Framework | Spring Boot 3 | Embedded Tomcat — no separate server needed |
| Persistence | Spring Data JPA + Hibernate | |
| Database | H2 file-mode | File: `./hairdresser.mv.db`. Persists between restarts. |
| Build | Maven (`pom.xml`) | Run with `mvn spring-boot:run` |
| Port | 8080 | `http://localhost:8080` |

### Frontend
| Concern | Choice | Notes |
|---|---|---|
| Language | Vanilla JavaScript (ES2020) | No React, no npm, no build step |
| HTML/CSS | Plain HTML5 + CSS3 | No frameworks, no Tailwind |
| Map | MapLibre GL JS via CDN | Style from openfreemap.org |
| Serving | Spring Boot static files | Placed in `src/main/resources/static/` |

---


---

## REST API Contract

All endpoints return JSON. Base path: `/api`.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/salons` | List all salons (id, name, lat, lng) — used for map markers |
| `GET` | `/api/salons/{id}` | Salon detail with embedded working_hours list |
| `POST` | `/api/salons` | Create a salon (owner action) |
| `PUT` | `/api/salons/{id}` | Update salon name/address/location/working hours (owner) |
| `GET` | `/api/salons/{id}/availability?date=YYYY-MM-DD` | Returns array of free time strings (e.g. `["09:00","09:30",...]`) |
| `POST` | `/api/bookings` | Create a booking `{salonId, date, time, clientName, clientContact}` |
| `GET` | `/api/salons/{id}/bookings?date=YYYY-MM-DD` | All bookings for a date (owner view) |

---

## Domain Rules

- **Slot duration**: always 30 minutes. This is not configurable.
- **One booking per slot**: a time slot is unavailable once one booking exists for it.
- **Availability calculation**: generate all 30-min slots within working hours for the requested day-of-week; subtract slots that already have an `ACTIVE` booking on that date.
- **Booking status**: `ACTIVE` or `CANCELLED`. New bookings are always `ACTIVE`.
- **Working hours**: stored per `(salon_id, day_of_week)`. Missing entry for a day means the salon is closed that day.

---

## Coding Conventions

### Java
- Package root: `com.hairdresser`
- Use constructor injection (not `@Autowired` on fields)
- Return `ResponseEntity<?>` from controllers; use `404 Not Found` when a salon/booking does not exist
- Use `LocalDate` / `LocalTime` for date/time fields (mapped via JPA `@Column`)
- Do not use `Optional.get()` without checking `isPresent()` first
- Keep services free of HTTP concerns; keep controllers free of business logic

### JavaScript
- No `var` — use `const` / `let`
- Use `async/await` with `try/catch` for all `fetch()` calls (helpers in `api.js`)
- Do not inline API URLs — use the helpers in `api.js`
- `map.js`, `owner.js`, `salon.js` each manage their own page; they do not share globals except through `api.js`

### HTML/CSS
- No inline styles; all styling goes in `css/style.css`
- Use semantic HTML elements (`<nav>`, `<main>`, `<section>`, `<form>`, etc.)

---

## Constraints / Hard Limits

| Constraint | Detail |
|---|---|
| No Docker | The app must run with `mvn spring-boot:run` only |
| No npm / Node | No JavaScript build tools, bundlers, or package managers |
| No external CDN exceptions | Only MapLibre GL JS and the openfreemap style URL are loaded from CDN |
| No auth yet | Do not add Spring Security, sessions, JWT, or login pages |
| No React | Use plain Vanilla JS |
| Local only | H2 file DB, embedded Tomcat — no cloud, no remote DB |
| No Docker | Repeated deliberately: do not suggest or add Docker/Compose files |

---

## Out of Scope (Do Not Implement Unless Asked)

- User authentication / login / registration
- Multiple owners
- Owner cancelling a client's booking
- Notes / comments system (owner or client)
- Email / SMS notifications
- Deployment to any remote server
- Unit or integration tests (unless explicitly requested)

---

## How to Run

```bash
cd hairdresser
mvn spring-boot:run
# Open http://localhost:8080
```

The H2 database file (`hairdresser.mv.db`) is created in the working directory on first run. `data.sql` seeds a demo salon so the map is not empty on first open.

---

## Planned Future Features (Context Only)

These are documented so agents do not accidentally design against them:

1. **Authentication** — login for owner and clients; role determined by credentials, not `localStorage`
2. **Multiple owners** — each owner manages their own salons only
3. **Owner cancels bookings** — owner can cancel any booking in their salon
4. **Notes** — owner can leave notes on time slots; clients can add notes when booking
