---
name: restart-app
description: >
  Use when the Spring Boot server needs to be restarted after backend or frontend changes.
  Triggers: "restart the app", "restart the server", "apply changes", "reload the backend",
  "restart after changes", "pick up my changes", "kill and restart", "bounce the server",
  "changes not taking effect", "server is stale", "refresh the backend".
  Handles: finding the running process, killing it cleanly, rebuilding if backend Java files
  changed, then starting the server again and confirming it is healthy on port 8080.
  NOT for: first-time project setup (scaffold the project first), running unit tests
  (use pre-build-unit-tests), browser smoke tests (use browser-validation).
---

# Restart App Skill

## Purpose

Stop the currently running Spring Boot process (if any), optionally recompile when Java source files changed, restart the server, and confirm it is healthy before handing control back.

---

## Step 1 — Detect what changed

Determine the type of changes made in the current session:

| Change type | Examples | Recompile needed? |
|---|---|---|
| **Backend** | Any file under `src/main/java/` or `src/main/resources/` | Yes — `mvn compile` required |
| **Frontend only** | Files under `src/main/resources/static/` (`*.html`, `*.css`, `*.js`) | No — static files are served directly |
| **Both** | Mix of Java and static files | Yes |

---

## Step 2 — Stop the running server

Check whether the server is already running on port 8080 and stop it cleanly.

```bash
# Find and kill any process listening on port 8080
EXISTING_PID=$(lsof -ti tcp:8080 2>/dev/null)
if [ -n "$EXISTING_PID" ]; then
  echo "Stopping PID $EXISTING_PID on port 8080…"
  kill "$EXISTING_PID"
  sleep 2
  # Force-kill if still alive
  kill -9 "$EXISTING_PID" 2>/dev/null || true
  echo "Server stopped."
else
  echo "No server running on port 8080."
fi
```

Wait up to 5 seconds for the port to be released before continuing.

---

## Step 3 — Recompile (backend changes only)

Skip this step if only frontend (static) files changed.

```bash
cd /home/tonev/SoftuniProjects/heardresser/hairdresser
mvn compile -q
```

- `BUILD SUCCESS` → proceed to Step 4
- `BUILD FAILURE` → **stop here**, report the compiler errors to the user, and do NOT start the server. Fix the errors first.

---

## Step 4 — Start the server

```bash
cd /home/tonev/SoftuniProjects/heardresser/hairdresser
nohup mvn spring-boot:run > /tmp/hairdresser-app.log 2>&1 &
echo "Server starting with PID $!"
```

Wait for startup — Spring Boot typically starts in 3–5 seconds on this machine.

```bash
# Poll until healthy (max 30 seconds)
for i in $(seq 1 15); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/salons 2>/dev/null)
  if [ "$STATUS" = "200" ]; then
    echo "Server is UP (HTTP 200) after ${i} polls."
    break
  fi
  echo "Waiting… (attempt $i, status=$STATUS)"
  sleep 2
done
```

---

## Step 5 — Health check

After the polling loop, confirm the server is healthy:

```bash
curl -s http://localhost:8080/api/salons | head -c 200
```

- Response contains JSON (starts with `[`) → **server is running correctly**
- Empty response or error → check the log:

```bash
tail -40 /tmp/hairdresser-app.log | grep -E "ERROR|Exception|Caused|Failed|SEVERE"
```

Report any errors found in the log to the user.

---

## Step 6 — Report

Produce a short summary:

```
Restart complete
================
Change type   : backend / frontend / both
Recompiled    : yes / no / skipped (frontend-only)
Server PID    : <pid>
Port 8080     : HEALTHY (HTTP 200 from /api/salons)
Log file      : /tmp/hairdresser-app.log

Next steps:
- Open http://localhost:8080 to verify the app
- Run the browser-validation skill for a full smoke test
```

If the server failed to start, summarise the relevant log lines and ask the user to fix the underlying issue before retrying.

---

## Notes

- The correct run directory is always `/home/tonev/SoftuniProjects/heardresser/hairdresser` (the inner Maven project folder, not the outer workspace root).
- Logs are written to `/tmp/hairdresser-app.log` — safe to share or inspect at any time.
- Frontend-only changes (HTML/CSS/JS under `static/`) are served from the classpath and take effect immediately without recompilation; only a browser hard-refresh (`Ctrl+Shift+R`) is needed in that case. However, a server restart is still valid and always safe.
- If `lsof` is not available, fall back to: `fuser -k 8080/tcp 2>/dev/null || true`
