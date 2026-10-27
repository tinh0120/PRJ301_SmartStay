# V4 – Motion, accessibility, offline and final design critique

Run date: 26/10/2026. Unlike R12, this pass was run live: Tomcat 9 (bundled copy, separate CATALINA_BASE pointing at
`build/web`) against the team's SQL Server, driven by headless Chrome through the DevTools protocol (scripts kept
outside the repo). Test data written: bookings BK000079 (paid, time-travelled to check-out) and BK000080 (paid, then
cancelled > 48 h, Refund pending), one alert acknowledged + resolved. The virtual clock was reset afterwards.

## 1. Motion audit

| Animation | Where | Trigger | Decision |
|---|---|---|---|
| `ssFadeUp` | home hero text + search box | page load (the one orchestrated moment) | Kept |
| `ssSlotSpring` | selected slot tile | click | Kept |
| `ssPulseOnce` | combo ladder step reached | click | Kept |
| `ssTotalBump` | booking total / mobile bar total | click | Kept |
| `ssCopySpring` | copy buttons (PIN, bank fields) | click | Kept |
| `ssPinReveal` | keycard PIN digits | page load after payment | Kept, now opacity only (letter-spacing changed the row width = layout shift) |
| `ssSuccessIn` | sandbox "payment received" view | payment result | Kept |
| `qrScanLaser` → `ssQrScan` | VietQR scan line | waiting for payment | Restyled: caramel hairline, one pass every 4 s (was green glow, 2.8 s back and forth) |
| `ssProgressCount` | staff "Tap again to confirm" bar | click | Kept |
| `ssPulseCritical` | NEW critical alerts, critical "Needs attention" items | data state | Kept (was defined but unused), 2.4 s |
| `ssInboxFresh`, `ssRowAck` | alerts inbox rows | realtime event / click | Kept |
| `ssToastIn/Out`, `ssTabIn` | admin toasts, revenue tabs | click | Kept |
| Demo drawer, admin drawers, mobile booking sheet, lightbox, weekly calendar | slide / fade | click | Kept |
| `slideDownAlert`, `smartstaySpin`, `smartstayRadar`, matrix toast, slot shake | flash, sync spinner, payment radar, error feedback | action | Kept, all stop under reduced motion |
| `mobileBarSlideUp` | room page mobile booking bar | page load | **Removed** |
| `.ss-anim-fade-up`, `.ss-skeleton` / `ssShimmer` | – | unused | **Removed** |
| ~50 hover / focus transforms in `app.css` and inline styles (card lifts, photo zooms, button nudges, footer links, slot tiles) | many | hover | **Neutralised** in `animations.css` §8.1 (colour / border hover feedback stays) |

`prefers-reduced-motion: reduce`: the global rule plus explicit overrides for the inline-page animations, the scan
line (hidden), the critical pulse and the sheet transitions.

## 2. Accessibility

| Area | Result |
|---|---|
| Keyboard-only booking | Passed: login (9 Tabs), room page → slot S1 (Enter), S2 (Space), terms (Space), Continue (Enter) → booking detail → Proceed to Payment → checkout → DEMO tab (Enter, focus moves to the drawer title) → Simulate successful transfer → payment result with PIN |
| Keyboard cancel | Passed: open cancel panel, type reason, open modal (focus inside), "Yes, cancel" |
| Slot tiles | `aria-label` "Tue, 27/10/2026, Morning 09:30–12:30, 259,000 VND"; `aria-pressed` kept in sync; selected state also has icon + "Selected" text |
| Lightbox | already had focus trap, Esc, focus return |
| Mobile booking sheet | added: `aria-modal` while open, focus to close button, Tab trap, Esc, focus return, toggle `aria-expanded`; hidden from Tab while closed |
| Demo drawer / admin drawers | Esc closes, focus moves in and returns (verified live for the demo drawer) |
| Weekly week picker (home, rooms) | added: Space / Arrow Down opens it, week rows are focusable buttons with labels, Enter / Space selects, focus returns, Tab out closes |
| Skip link | added ("Skip to main content") |
| Focus ring | caramel on light surfaces; Latte on navbar, footer, hero, keycard (caramel on espresso is ~3:1) |
| Icon-only buttons | static scan: none without a label; matrix toast close button got one |
| Form labels | fixed: assignment form (6 fields), device "Last seen", API key fields |
| Error text | `.invalid-feedback` linked through `aria-describedby`; `aria-invalid` on failed submit and server errors |
| Colour only | admin room cards now show slot code + icon per state; status dots elsewhere already had text |

## 3. Offline check (all requests to non-localhost hosts were failed on purpose)

| Role | Page | External requests | Failed local requests | JS errors | Fonts | Flatpickr | Chart.js |
|---|---|---|---|---|---|---|---|
| Anonymous | `/`, `/rooms`, `/contact`, `/terms`, `/login`, `/register` | 0 | 0 | 0 | loaded | not loaded (not needed) | – |
| Anonymous | `/rooms/detail` | 0 | 0 | 0 | loaded | loaded, 1/1 field | – |
| Guest | `/rooms/detail`, `/guest/bookings` | 0 | 0 | 0 | loaded | loaded, all fields | – |
| Guest | booking detail, checkout, payment result | 0 | 0 | 0 | loaded | – | – |
| Staff | `/staff/assignments` | 0 | 0 | 0 | loaded | – | – |
| Admin | dashboard | 0 | 0 | 0 | loaded | – | revenue chart mounted |
| Admin | bookings, assignments, activity, alerts | 0 | 0 | 0 | loaded | loaded, all fields | – |
| Admin | payments, rooms, room edit, slots, users, devices, thresholds, reviews | 0 | 0 | 0 | loaded | not loaded | – |
| Admin | revenue | 0 | 0 | 0 | loaded | loaded | revenue chart mounted |

Outbound links (Facebook, Instagram, Zalo, Google Maps) are plain links, not requests.

## 4. Performance

| Item | Before | After |
|---|---|---|
| Amenity icons (shown at 22 px / 14 px) | 10 PNG, 0.7–4.8 MB each (≈ 22 MB) | `amenities/sm/*.png` 72 px, 4–6 KB each (49 KB); originals kept |
| Logo (3 per page, 44 px) | 519 KB | `tini-smartstay-logo-sm.png` 22 KB |
| Favicon + booked-slot marks | 527 KB | `tini-smartstay-favicon-96.png` 11 KB |
| Flatpickr (css + js) | every page | only pages with `[data-datepicker]` (7) |
| `realtime.js` on checkout | loaded twice (two sockets) | once, plus a guard |
| Fonts | 4 files, only the weights used | unchanged (132 KB) |
| `<img>` width / height | 18 missing | set (the activity lightbox image is filled by script) |

## 5. Visual critique (1440 / 768 / 390) – one accessory off per page, then fixes

| Page | Finding | Change |
|---|---|---|
| Home | room cards carried a capital tag repeating the room name | tag removed |
| Home / Rooms | check-in date shown as 2026-10-26 (decision: dd/MM/yyyy) | field shows 26/10/2026; the server also accepts dd/MM/yyyy |
| Rooms | the same "SHORT STAY" tag on all six photos | removed |
| Room detail | "Floor 1" pill next to "Floor 1, East Wing" | floor pill only when there is no location text |
| Checkout | blue "Automated Gateway" + green "256-Bit SSL Encrypted" badges, header wraps on phones | SSL badge removed (decorative, off-palette, untrue for the sandbox) |
| All pages | footer column titles in tracked capitals | sentence case, Newsreader |
| Admin rooms | a portrait photo broke the card grid | photo cropped to the 16:9 frame |
| Admin bookings (390) | the page was zoomed out to 855 px (a screen-reader label in the last table column escaped the scroll box) | scroll box contains it; every page now renders at 390 |
| Admin revenue (390) | "By room / By time slot" tables widened the page to 450 px | tighter cells, the card scrolls instead |

## 6. Regression

- JS: booking-detail 13/13, mockpay 11/11, cancel-policy 12/12, room-search 11/11, tap-confirm 11/11.
- Java (`tools/`): 13 suites pass (ActivityLog, AlertInbox, BookingState, CancellationPolicy, ClockService, DashboardLogic,
  DemoTime, JobCoordinator, RevenueReport, RoomFilter, RoomSlotState, TldConsistency 24/24, BookingDetailFormatter).
  `PricingSelfTest` (round 1) no longer compiles: it imports `model.Quote`, now `model.Booking.Quote`; not changed.
- All 38 JSPs and 8 tags compile with Jasper (JspC); Ant `dist` builds.
- Demo script (README) end to end: book → pay from the DEMO drawer → PIN → 4 time-travel jumps (0 other bookings
  affected; Confirmed → Checked in → Checked out, room A101 "Needs cleaning") → dashboard / bookings / rooms / alerts →
  cancel > 48 h (Refund pending 538,200 VND) → Reset (offset back to 0). Reminder e-mails were sent to the
  `@smartstay.local` guest address.

## 7. Remaining issues

| # | Page | Problem | Severity | Status |
|---|---|---|---|---|
| 1 | Alerts | In-place Acknowledge / Resolve always failed ("Unexpected response"): the form has `<input name="action">`, so `form.action` was the input, not the URL | High | Fixed (`getAttribute('action')`) |
| 2 | Admin bookings | Lock icon "Slots are still held after a no-show" on every paid booking (paid / checked-out bookings keep HELD slot rows) | Medium | Fixed (only EXPIRED + NO_SHOW) |
| 3 | Admin bookings, revenue | Page wider than the phone screen | Medium | Fixed |
| 4 | Checkout | `realtime.js` loaded twice (two WebSockets, duplicate events) | Medium | Fixed |
| 5 | Room page / keycard | Layout shift from the PIN reveal animation; load-time slide of the booking bar | Medium | Fixed |
| 6 | Mobile booking sheet, week picker | No keyboard / screen-reader support | Medium | Fixed |
| 7 | All guest pages | 22 MB of amenity icons, 0.5 MB logo / favicon | Medium | Fixed (small copies) |
| 8 | Home / Rooms | Date in yyyy-MM-dd | Medium | Fixed |
| 9 | Assignment form | Labels not tied to fields; raw `NEEDS_CLEANING` text | Medium | Fixed |
| 10 | Alerts | Each action shows two toasts (fetch result + the realtime echo) | Low | Left |
| 11 | Checkout, house rules modal, matrix toast | Legacy inline colours outside the palette (blue badge, purple / blue icons, orange toast border) | Low | Left (inline styles from round 1) |
| 12 | Checkout, home | All-caps eyebrows ("BENEFICIARY ACCOUNT DETAILS", quote attribution) | Low | Left |
| 13 | Vendor files | `sourceMappingURL` comments point to `.map` files that are not shipped (DevTools warning only) | Low | Left (vendor files untouched) |
| 14 | README | "Check-in now" keeps the booking Confirmed until the PIN is used; the next jump checks it in | Low | README wording adjusted |
| 15 | tools/PricingSelfTest.java | Does not compile (old `model.Quote` import) | Low | Left (round-1 tool) |
| 16 | R12 #10–12 | Unused old log pages, payment-review alert not realtime, hide reason not stored | Low | Unchanged |
