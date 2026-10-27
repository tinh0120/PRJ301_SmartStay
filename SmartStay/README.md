# SmartStay - Automated Homestay Management System

SmartStay (Tini SmartStay) is a self-service homestay web application. Guests book a room by time slot
(S1 Morning, S2 Afternoon, S3 Evening, S4 Overnight; hours differ per room), pay through a PayOS-style
sandbox, and open the door with a 6-digit PIN checked by a real ESP32 lock. Owner, Admin and Staff use the
back office for bookings, rooms and pricing, devices, activity logs, security alerts, reviews and revenue.

Built on plain Java Servlet 4.0 / JSP 2.3 / EL 3.0 with custom tags (`WEB-INF/tags`) and EL functions
(`WEB-INF/tld/smartstay.tld`). No JSTL, no frameworks, no CDN: everything runs offline.

---

## Technology stack

- **Runtime**: Java JDK 1.8, Apache Tomcat 9.0.113
- **Database**: Microsoft SQL Server 2017+ / Express (`SmartStayDB`)
- **Libraries (`lib/`)**: `sqljdbc4.jar`, `jbcrypt-0.4.jar`, `gson-2.8.9.jar`, `javax.mail-1.6.2.jar` +
  `activation-1.1.1.jar` (e-mail), `org.eclipse.paho.client.mqttv3-1.2.5.jar` (MQTT)
- **Frontend**: Bootstrap 5.3.3 and Bootstrap Icons (local), Chart.js 4.4.4, Flatpickr 4.6.13, vanilla JS.
  New styles live in `assets/css/modern-ui.css` and `assets/css/animations.css` (`ss-*` BEM classes,
  7-colour Warm Luxury palette). `app.css` is legacy.

---

## How to run

### 1. Database
SQL Server on `localhost:1433`. Run the scripts in order (take a backup first if the database already
has data):
```powershell
sqlcmd -S localhost -U sa -P 12345 -C -i sql/01_schema.sql
sqlcmd -S localhost -U sa -P 12345 -C -i sql/02_seed_data.sql
sqlcmd -S localhost -U sa -P 12345 -C -i sql/03_round2_migration.sql
```
`03_round2_migration.sql` is idempotent (safe to run again): it gives Admin all Owner permissions and
clears the seeded sample snapshot paths.

### 2. Connection settings
`src/java/db.properties` (copy from `db.properties.sample`):
```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=SmartStayDB;encrypt=false
db.user=sa
db.password=12345
```

### 3. Build
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_202"
& "C:\Program Files\NetBeans-13\netbeans\extide\ant\bin\ant.bat" -f build.xml dist
Copy-Item "dist\SmartStay.war" "C:\path-to-tomcat\webapps\SmartStay.war"
```
Or open the project in NetBeans and press Run.

### 4. Start Tomcat
```powershell
& "C:\path-to-tomcat\bin\catalina.bat" run
```
Open `http://localhost:8080/SmartStay/` (or `/` when deployed as ROOT).

### Demo mode
`web/WEB-INF/web.xml` has the context-param `demo.mode` (default `true`). It enables the **DEMO** tab
(virtual clock and payment simulation). Set it to `false` for a production-like run: `/demo/*` then
returns 404 and the tab disappears.

---

## Demo accounts

All demo accounts use the password **`12345`**.

| Role | Email | What to show |
|---|---|---|
| Owner | `owner@gmail.com` | Everything; the Owner account is protected from edits by others |
| Admin | `admin1@gmail.com`, `admin2@gmail.com` | Same permissions as Owner (dashboard, bookings, rooms, devices, logs, alerts, reviews, revenue, users) |
| Staff | `staff1@gmail.com`, `staff2@gmail.com` | Cleaning tasks only (`/staff/assignments`); "Mark as clean" asks to tap again within 3 s |
| Guest | `guest1@smartstay.local` … `guest35@smartstay.local` | Booking, payment, door PIN, My bookings, reviews |

---

## 5-minute demo script

Two browser windows help: one normal window for the guest, one private window for the admin.

1. **Book (guest)** – Log in as `guest1@smartstay.local`. Open **Rooms**, pick a room, choose one or
   more slots in the slot picker (2 or more slots get the combo discount shown in the booking bar).
   Choose a date at least 3 days ahead so the booking is refundable. Press **Continue**.
2. **Pay** – On the checkout page the QR code and a 5-minute countdown appear (the slots stay held for
   15 minutes). Open the **DEMO** tab on the right edge and press **Simulate successful transfer**. The page
   switches to the payment result within a few seconds (WebSocket, with polling as a fallback).
3. **PIN** – The result page shows the Digital Keycard: the 6-digit door PIN, a copy button and the
   validity window (check-in to check-out). The PIN also appears under **My bookings → booking detail**.
4. **Time travel** – Open the **DEMO** tab again, select the booking and press, in order:
   - **15 min before check-in** – the stay is about to start; the booking still shows Confirmed.
   - **Check-in now** – the stay starts. The booking shows Checked in once the PIN is used at the door (or on
     the next jump, which checks the guest in first so the stay is not a no-show).
   - **15 min before check-out** – the check-out reminder job runs (e-mail failures do not stop the demo).
   - **Check-out now** – the booking completes and the room changes to "Needs cleaning". The admin can
     then assign the cleaning under **Assignments**, and staff marks it clean.
   Each jump asks for confirmation and says how many other future bookings the global clock affects.
   A banner "Simulated time dd/MM/yyyy HH:mm · Reset" stays on top while the clock is moved.
5. **Watch the back office (admin window)** – Log in as `admin1@gmail.com`:
   - **Dashboard**: KPIs, "Needs attention" and the room tiles update live.
   - **Bookings**: the booking moves through the status tabs.
   - **Security → Activity logs**: door access and motion events, grouped by day; click a row for details.
   - **Security → Alerts**: Acknowledge, then Resolve an alert in place; the header badge count drops.
6. **Cancellation policy** – As the guest, open a booking whose first slot is more than 48 hours away and
   cancel it: the full amount is marked for refund (admin sees "Refund pending"). A booking starting in
   less than 48 hours shows "Non-refundable" before payment and in the cancel dialog.
7. **Reset** – Press **Reset** in the banner or the DEMO tab. The clock returns to real time; data changes
   made during the demo stay (time travel is forward-only and is not an undo).

Optional extras: **Revenue** (quick ranges 7 / 30 / 90 days, Export CSV), **Reviews** (hide a review with a
reason), **Rooms → Slots & pricing**, **Staff** (assign the room to `staff1@gmail.com`, log in as staff, mark it clean).

---

## Cancellation policy

- Free cancellation with a **100% refund until 48 hours before the first booked slot**.
- After that, or for bookings made less than 48 hours before the first slot, the booking is
  **non-refundable**; guests see this before paying.
- Refunds are marked "Refund pending" for the admin, who confirms the bank transfer ("Mark refunded").

---

## What is simulated and what is real

| Part | Simulated or real |
|---|---|
| PayOS / VietQR payment | **Simulated** (sandbox): `/mock-payos/checkout`, success or failure from the DEMO tab or the sandbox buttons. No money moves. |
| Business clock | **Virtual** when moved from the DEMO tab (`ClockService`): check-in/out, no-shows, PIN validity, reminders, dashboard "today". Forward-only; Reset returns to real time. |
| Payment and hold deadlines, device heartbeats, ESP32 anti-replay, access log timestamps, sessions | **Real** time, always (see `docs/time-inventory.md`). |
| ESP32 door lock and motion sensor | **Real** hardware: the lock calls `/api/iot/access/verify` with its API key; the PIN is checked in Java with the virtual clock. Motion events arrive through `/api/iot/motion-events`. |
| E-mails | Real SMTP when configured; failures are logged and never block bookings or jobs. |
| Snapshots | No sample images are seeded; a snapshot is shown only when the device uploads one. |

---

## Tests

```powershell
node web/assets/js/booking-detail.test.js
node web/assets/js/mockpay.test.js
node web/assets/js/cancel-policy.test.js
node web/assets/js/room-search.test.js
node web/assets/js/tap-confirm.test.js
```
Java unit tests are in `tools/` (plain `main` methods, compile against `build/web/WEB-INF/classes` with
JDK 1.8), for example `ClockServiceTest`, `CancellationPolicyTest`, `DemoTimeTest`, `RevenueReportTest`.
QA notes and the permission matrix: `docs/r12-qa.md`.
