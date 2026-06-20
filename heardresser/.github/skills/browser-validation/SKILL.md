---
name: browser-validation
description: >
  Use when asked to "validate in the browser", "check the UI", "open the app",
  "verify the frontend works", "browser smoke test", "end-to-end check",
  "does the map load", "test the booking flow in the browser", or
  "validate the app is working". Starts the Spring Boot server if needed,
  then uses browser tools to walk through every page of the hairdresser app
  and verify it behaves correctly. Produces a pass/fail report for each check.
  NOT for: Java unit tests (use pre-build-unit-tests), API-only checks.
---

# Browser Validation Skill

## Purpose

Automate in-browser smoke testing of the hairdresser app after code changes. The agent opens each page, asserts that key elements are visible and interactive, exercises the booking flow end-to-end, and reports every check as PASS or FAIL.

---

## Prerequisites

Before opening the browser, confirm the server is running.

```bash
# Check if port 8080 is listening
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/salons
```

- Returns `200` → server is up, proceed to browser checks
- Returns anything else or connection refused → start the server first:

```bash
cd hairdresser/hairdresser
mvn spring-boot:run &
# Wait ~10 seconds for startup, then proceed
```

---

## Check Suite

Run all checks in order. Record each as **PASS** or **FAIL** with a note.

---

### Check 1 — API sanity (`/api/salons`)

Use `curl` or the browser fetch to confirm:

```
GET http://localhost:8080/api/salons
```

Expected: HTTP 200, JSON array with at least one salon entry containing `id`, `name`, `lat`, `lng`.

---

### Check 2 — Map page (`index.html`)

1. Open `http://localhost:8080/` in the browser.
2. Take a screenshot.
3. Assert:
   - `<nav>` top bar is visible with the "Hairdresser Booking" title
   - Role toggle button is visible (text is `client` or `owner`)
   - Map container (`#map`) is rendered and fills the viewport (not blank/white)
   - At least one map marker (the seed salon) is visible on the map

---

### Check 3 — Role toggle

1. On the map page, click the role toggle button.
2. Read the page / take screenshot.
3. Assert: button text changes from `client` → `owner` (or vice versa).
4. When role is `owner`, assert the "Owner Dashboard" link appears in the nav.
5. Toggle back to `client`.

---

### Check 4 — Salon booking page (`salon.html`)

1. Navigate to `http://localhost:8080/salon.html?id=1`.
2. Assert:
   - Salon name heading is visible and not "Loading…" or "Salon not found"
   - Address text is visible below the name
   - Date picker (`#datePicker`) is visible and pre-filled with today's date
   - At least one time-slot button is rendered in `.slots-grid` (the seed salon has Mon–Sat hours, so a weekday should show slots)

If today is Sunday (salon closed), change the date picker to the next Monday and re-check that slots appear.

---

### Check 5 — Booking flow

1. On `salon.html?id=1`, click the first available slot button.
2. Assert: booking modal (`#bookingModal`) becomes visible.
3. Assert: modal shows the selected time in `#modalSlotInfo`.
4. Fill in:
   - `#clientName` → `Test Client`
   - `#clientContact` → `test@example.com`
5. Click the "Book" submit button.
6. Assert: modal closes AND the success toast (`#successToast`) appears with "Booking confirmed!" text.
7. Assert: the slot button that was just booked is now disabled / shows as taken (re-read page after toast disappears).

---

### Check 6 — Double-booking prevention

1. Immediately try clicking the same slot again (it should now be disabled).
2. Assert: the button has class `taken` and is `disabled`.
3. If somehow still clickable, submit the form again and assert the response is a 409 conflict (not a second booking confirmation).

---

### Check 7 — Owner dashboard (`owner.html`)

1. Navigate to `http://localhost:8080/owner.html`.
2. Assert:
   - Salon list renders at least one salon (the seed salon)
   - "Add Salon" button is visible
3. Click "Edit" on the seed salon.
4. Assert:
   - The form section becomes visible (`#salonFormSection` not hidden)
   - Salon name field is populated with the existing name
   - Working hours grid shows at least one day checkbox checked with time inputs

---

### Check 8 — Owner can add a new salon

1. Click "Add Salon" on the owner dashboard.
2. Assert the form clears (name field is empty).
3. Fill in:
   - Name: `Browser Test Salon`
   - Address: `Test Street 1`
   - Lat: `42.700`
   - Lng: `23.330`
   - Check Monday, set `09:00`–`17:00`
4. Click Save.
5. Assert: form hides and the salon list now includes "Browser Test Salon".
6. Navigate back to `http://localhost:8080/` and assert a new marker appears on the map (page reload may be required).

---

## Report Format

After running all checks, output a table:

| # | Check | Status | Notes |
|---|-------|--------|-------|
| 1 | API sanity | ✅ PASS / ❌ FAIL | … |
| 2 | Map page renders | ✅ PASS / ❌ FAIL | … |
| 3 | Role toggle | ✅ PASS / ❌ FAIL | … |
| 4 | Booking page + slots | ✅ PASS / ❌ FAIL | … |
| 5 | Booking flow | ✅ PASS / ❌ FAIL | … |
| 6 | Double-booking blocked | ✅ PASS / ❌ FAIL | … |
| 7 | Owner dashboard | ✅ PASS / ❌ FAIL | … |
| 8 | Add new salon | ✅ PASS / ❌ FAIL | … |

If any check FAILS:
1. State which element was missing or which assertion failed
2. Show the screenshot or page snapshot that revealed the failure
3. Identify the likely root cause (missing CSS class, JS error, API error, etc.)
4. Fix the issue before re-running the failed check

**All 8 checks must PASS before the validation is considered complete.**
