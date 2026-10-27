# R12 – Final QA round 2 (audit before fixes)

Audit date: 26/10/2026. Method: static audit of every JSP/tag/JS/CSS file, code trace of filters and
servlets. Tomcat was NOT started by the agent (a running app starts the scheduler, which writes to the
real database), so the browser checks at 1440 / 768 / 390 px and the live end-to-end run are left for the
team with the demo script in `README.md`.

## Automated checks

| Check | Result |
|---|---|
| JSTL prefixes (`c:`, `fn:`, `fmt:`) in JSP/tag files | 0 hits |
| CDN / remote resources (script, stylesheet, font, fetch) | 0 hits; only normal outbound links (Facebook, Instagram, Zalo, Google Maps) |
| New rules in `app.css` this round | 0 (git diff of `app.css` is empty) |
| Money format | "259,000 VND" everywhere (`e:money`, JS formatters); no ₫ / VNĐ left |
| `<input type="date">` without Flatpickr (`data-datepicker`) | 0 on live pages (only in the unused `access-logs.jsp` / `motion-logs.jsp`) |
| `font-weight: 700` / `fw-bold` | 149 `fw-bold` uses in older pages (guest booking detail, room detail, staff, terms) |
| Nested scrollbars | room-detail info tabs (`max-height: 380px; overflow-y: auto`); old log pages (unused) |
| Raw enum codes in lists | thresholds, staff assignments, admin assignments, booking form, booking detail (logs/alerts), device form, assignment form |

## Remaining issues (found before fixing)

| # | Page | Problem | Severity | Status |
|---|---|---|---|---|
| 1 | README.md | Demo script is out of date: describes the old sandbox dock, a 15-minute checkout countdown (real: 300 s), old alert/devices/revenue screens, says the ESP32 is simulated (it is real), no time travel, no cancellation policy, no migration 03 step | High | Fixed |
| 2 | admin/thresholds.jsp | Alert type shown as raw code (`DOOR_OPEN_TOO_LONG`), severity as `HIGH` | Medium | Fixed (`e:alertTitle`, `e:humanStatus`) |
| 3 | staff/assignments.jsp, staff/assignment-detail.jsp | Assignment status badge shows raw code (`IN_PROGRESS`) | Medium | Fixed |
| 4 | admin/assignments.jsp, admin/assignment-form.jsp | Status shown as raw code in the list and in the room dropdown | Medium | Fixed |
| 5 | admin/booking-form.jsp | Booking status shown as `PENDING_PAYMENT` in two notices and a badge | Medium | Fixed |
| 6 | admin/booking-detail.jsp | Access-log result, alert type, severity and status as raw codes in the related logs / alerts tables | Medium | Fixed (title + code kept small for the alert type) |
| 7 | All older pages | `fw-bold` renders font-weight 700, which the design rule forbids (only 400 / 600 are loaded) | Medium | Fixed globally (`.fw-bold`, `b`, `strong` → 600 in modern-ui.css) |
| 8 | publicsite/room-detail.jsp | Overview / house rules / reviews tabs scroll inside a 380 px box (nested scrollbar) | Medium | Fixed (box grows with content) |
| 9 | admin/device-form.jsp | Room status in the room dropdown is a raw code | Low | Fixed |
| 10 | admin/access-logs.jsp, admin/motion-logs.jsp | Unused since R9 (old URLs redirect to `/admin/activity`), still contain old patterns | Low | Left; delete only with the team's OK |
| 11 | PaymentService | `PAYMENT_REVIEW_REQUIRED` alerts do not trigger the realtime `ALERT_CREATED` event (payment flow is out of scope) | Low | Left; visible after reload |
| 12 | Reviews | Hide reason is logged and flashed, not stored (REVIEW has no column) | Low | Left; needs a schema change |

## Permission matrix (from `AuthFilter`, `PermissionFilter`, servlet checks and the seeded role permissions)

Admin has every Owner permission (R1). "Login" = redirect to `/login?next=…`; "Staff home" = redirect to
`/staff/assignments`.

| URL | Anonymous | Guest | Staff | Admin | Owner |
|---|---|---|---|---|---|
| `/`, `/rooms`, `/rooms/detail`, `/contact`, `/terms` | 200 | 200 | 200 | 200 | 200 |
| `/login`, `/register` (GET) | 200 | redirect to role home | redirect | redirect | redirect |
| `/guest/bookings/pay` (create booking + payment link, POST) | Login | 200 | 403 | 200 | 200 |
| `/mock-payos/checkout`, `/mock-payos/simulate` (sandbox gateway) | 200 (public, like a real gateway page) | 200 | 200 | 200 | 200 |
| `/guest/bookings`, `/guest/bookings/detail`, `/cancel`, `/pin`, `/guest/reviews/create` | Login | 200 (own bookings only) | 403 | 200 | 200 |
| `/guest/payment/result`, `/guest/payment/status` | 200 (public result, PIN only for the owner) | 200 | 200 | 200 | 200 |
| `/staff/assignments` | Login | 403 | 200 | 403 | 403 |
| `/admin/dashboard`, `/admin/reports/revenue(+/data, /export)` | Login | 403 | Staff home | 200 | 200 |
| `/admin/bookings`, `/admin/payments`, `/admin/rooms`, `/admin/rooms/slots` | Login | 403 | Staff home | 200 | 200 |
| `/admin/users` | Login | 403 | Staff home | 200 (Owner row protected) | 200 |
| `/admin/devices`, `/admin/thresholds`, `/admin/assignments` | Login | 403 | Staff home | 200 | 200 |
| `/admin/activity`, `/admin/access-logs`, `/admin/motion-logs` (redirect to activity) | Login | 403 | Staff home | 200 | 200 |
| `/admin/alerts` (GET) / `/admin/alerts/action` (POST) | Login | 403 | Staff home | 200 | 200 |
| `/admin/reviews` | Login | 403 | Staff home | 200 | 200 |
| `/demo/*` (`demo.mode=true`) | Login | 200 for own bookings, 403 for others' | 403 | 200 | 200 |
| `/demo/*` (`demo.mode=false`) | 404 | 404 | 404 | 404 | 404 |
| `/api/iot/*` (ESP32, API key) | API-key check (401/403 without a valid key) | same | same | same | same |

## End-to-end (to run live)

Code-traced, not run by the agent: book → pay from the Demo drawer → PIN page → 4 time-travel jumps
(each runs one job cycle and broadcasts booking / room updates) → dashboard, alerts and activity logs
update → cancel > 48 h (full refund, REFUND_PENDING) and < 48 h (non-refundable) → Reset time. The
steps are in `README.md` → "5-minute demo script". Please report any failing step.
