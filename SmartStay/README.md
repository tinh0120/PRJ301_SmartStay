# SmartStay - Automated Homestay Management System

SmartStay (Tini SmartStay) is an intelligent, self-service homestay web application designed for automated boutique accommodation operations. Built on a pure Java MVC architecture (Servlet 4.0, JSP 2.3, EL 3.0, custom tag libraries), SmartStay provides time-slot room booking, automated smart lock door PIN generation, housekeeping task assignment, device health tracking, real-time security alerts, and revenue reporting.

---

## Technology Stack

- **Runtime & Language**: Java JDK 1.8 (Java SE 8)
- **Application Server**: Apache Tomcat 9.0.113
- **Database**: Microsoft SQL Server 2017+ / Express (`SmartStayDB`)
- **Libraries (`lib/`)**:
  - `sqljdbc4.jar` (Microsoft JDBC Driver for SQL Server)
  - `jbcrypt-0.4.jar` (BCrypt password hashing)
  - `gson-2.10.1.jar` (JSON serialization for charts and telemetry APIs)
- **Frontend Architecture**:
  - Custom responsive styling (`assets/css/app.css`) following the warm 7-color boutique palette (Espresso, Cocoa, Caramel, Latte, Cream, Sand, Milk).
  - Bootstrap 5.3 (local vendor bundle) + Bootstrap Icons.
  - Zero third-party runtime frameworks or extension dependencies; 100% scriptless JSP views utilizing custom JSP tags (`WEB-INF/tags/`) and EL functions (`WEB-INF/tld/smartstay.tld`).

---

## How to Run

### 1. Database Setup
Ensure Microsoft SQL Server is running on `localhost:1433`. Execute the database migration and seed scripts:
```powershell
# In PowerShell:
sqlcmd -S localhost -U sa -P 12345 -C -i sql/01_schema.sql
sqlcmd -S localhost -U sa -P 12345 -C -i sql/02_seed_data.sql
```

### 2. Database Connection Configuration
Verify `src/java/db.properties` (or copy from `src/java/db.properties.sample`):
```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=SmartStayDB;encrypt=false
db.user=sa
db.password=12345
```

### 3. Build & Deploy with Ant / NetBeans
Using Apache Ant with JDK 1.8:
```powershell
# Set Java Home to JDK 1.8
$env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_202"

# Compile and package WAR
ant dist

# Deploy to Tomcat webapps (e.g. ROOT.war or SmartStay.war)
Copy-Item "dist\SmartStay.war" "C:\path-to-tomcat\webapps\ROOT.war"
```

### 4. Start Tomcat
```powershell
& "C:\path-to-tomcat\bin\catalina.bat" run
```
Access the application at: **`http://localhost:8080/`**

---

## Demo Accounts

All demo accounts use password: **`12345`**

| Role | Email | Password | Permissions & Scope |
|------|-------|:--------:|---------------------|
| **Owner** | `owner@gmail.com` | `12345` | Full administrative authority, financial revenue reports, user accounts, system configuration. |
| **Admin 1** | `admin1@gmail.com` | `12345` | Booking operations, walk-in reservations, refund management, device management, security alerts, review moderation. |
| **Admin 2** | `admin2@gmail.com` | `12345` | Operations manager, room slot pricing, alerts handling. |
| **Staff 1** | `staff1@gmail.com` | `12345` | Housekeeping staff portal, room cleaning task completion. |
| **Staff 2** | `staff2@gmail.com` | `12345` | Housekeeping staff portal. |
| **Guest 1** | `guest1@smartstay.local` | `12345` | Room browsing, multi-slot reservation, simulated checkout, door PIN display. |
| **Guest 2–35** | `guest2@smartstay.local` ... `guest35@smartstay.local` | `12345` | Seed guest accounts with realistic booking, access log, and review histories. |

---

## End-to-End Demo Script

Follow this 5-minute walkthrough to demonstrate the complete guest lifecycle and administrative supervision:

### Step 1: Guest Room Discovery & Time-Slot Booking
1. Open `http://localhost:8080/` in a browser.
2. Click **Browse Rooms** or navigate to `/rooms`.
3. View the 7-day strip and use the multi-slot selector to pick time slots (e.g., **S1 (06:00–12:00)** and **S2 (12:00–18:00)**). Note the 10% multi-slot consecutive discount calculation.
4. Click **View Room & Availability** on *Butter Beige* (Room 101).
5. On the Room Detail page, inspect the photo gallery with full lightbox view, room overview, house rules, and verified guest reviews.
6. Select 1 or 2 slots on the availability matrix. The sticky summary card computes the original amount, discount percentage, and total due.
7. Click **Continue to Payment** (if prompted, login with `guest1@smartstay.local` / `12345`).

### Step 2: Payment Simulation (Mock PayOS / VietQR Napas 247)
1. On the Checkout page (`/mock-payos/checkout`), view the 4-step stepper (Step 3 active).
2. The page renders an authentic **VietQR** code, beneficiary account details, payment order code, and a live 15-minute countdown progress bar.
3. In the bottom-right developer dock, expand the **Sandbox &middot; Simulate bank transfer** panel.
4. Click **Simulate successful transfer**.
5. The payment status transitions to `PAID` via WebSocket/polling, triggering an automated redirect to the Payment Result page within 2 seconds.

### Step 3: Self Check-in Door PIN & Access Instructions
1. On `/guest/payment/result`, observe the active Step 4 (**Get PIN**).
2. The owning guest receives their unique **6-digit main entrance door PIN** (e.g., `529 184`), displayed in large monospaced format with a one-click **Copy PIN** button and its exact validity period (`valid from ... until ...`).
3. Follow the 3-step entrance guide:
   - *Step 1*: Main building ground floor keypad.
   - *Step 2*: Type 6-digit PIN + `#` key to disengage the electromagnetic lock.
   - *Step 3*: Key locker at Room 101 to collect the private room key.
4. Test the **Print Access Pass** button to verify print-friendly `@media print` styling (clean monochrome voucher with navigation stripped).

### Step 4: Guest Portal Verification
1. Click **My Bookings** (`/guest/bookings`).
2. Verify that the new booking is listed as `CONFIRMED` with payment `PAID`, room details, and slot window.
3. Click the booking detail chevron to inspect the comprehensive breakdown.

### Step 5: Admin Dashboard & Operations
1. Click **Logout**, then log in as Admin: `admin1@gmail.com` / `12345`.
2. Navigate to `/admin/dashboard`.
3. Inspect the operational KPI cards:
   - *Check-ins Today*, *Check-outs Today*, *Pending Cleaning Tasks*, and *Open Alerts by Severity*.
4. Switch between the 3 dashboard tabs:
   - **Occupancy & Hardware**: Real-time room states (Available, Occupied, Needs Cleaning, Maintenance) and IoT lock device status counts (derived consistently from device last-seen heartbeats).
   - **Bookings & Revenue**: Today's bookings breakdown and the 7-day revenue bar chart.
   - **Security & Alerts**: Alerts-by-type horizontal bar list with severity color tokens and numbers.

### Step 6: Security Alert Lifecycle Management
1. Go to **Security & Devices > Alerts** (`/admin/alerts`).
2. Locate any alert with status `NEW` (e.g., `DEVICE_OFFLINE` or `MOTION_NIGHT_DOOR`).
3. Click the **Acknowledge** button (CSRF-protected POST). The status updates to `ACKNOWLEDGED` and records the acting administrator's name.
4. Click **Resolve**. A modal prompts for an optional resolution note (e.g., "Network gateway restarted; lock telemetry verified").
5. Click **Confirm Resolve**. The alert lifecycle transitions to `Closed` (Resolved) and the navbar alert badge count updates immediately.
6. Click **Details** to review the full event timestamp, device ID, and room location.

### Step 7: Booking Management & Refund Processing
1. Navigate to **Operations > Bookings** (`/admin/bookings`).
2. Notice the "Date & Slots" column (`S1+S2 &middot; 06:00–18:00`), status tab counts, and tooltips for "Held slots" and "Early CI".
3. Filter by status `CANCELLED` or locate a booking with payment status `REFUND_PENDING`.
4. Click **Mark refunded**. In the confirmation modal, enter the transfer reference and confirm. The payment transitions to `REFUNDED` and writes an audit entry.

### Step 8: Device Management & Room Scope
1. Navigate to **Security & Devices > Devices** (`/admin/devices`).
2. Note the clear scope distinction: Room `- (Shared)` indicates the common lobby/entrance lock, while numbered rooms represent private room locks.
3. Click **Regenerate API Key** on a device. Confirm the security warning modal to view the new 32-character API key in the one-time reveal dialog with a copy button.
4. Click **Delete** on an inactive device. A confirmation modal requires typing the device ID before enabling the confirm action.

### Step 9: Financial Revenue Report
1. Navigate to **Management > Revenue** (`/admin/reports/revenue`).
2. Verify default pre-filled 30-day date range and KPI cards: *Total revenue, Paid bookings, Average per booking, Occupancy rate, Change vs previous period*.
3. Note that cancelled bookings and refunded payments are strictly excluded from totals.
4. Inspect the timeline chart rendered in warm boutique brown tones with compact formatted currency ticks (`250k / 500k ₫`).
5. Switch to the **Detailed Table** tab to review bucketed records, and toggle **Hide zero-revenue rows** to inspect active billing days.

---

## Simulated Components

To ensure complete portability without requiring external merchant contracts or physical IoT microcontroller testbenches, the following components are simulated in software:

1. **PayOS / VietQR Banking Gateway**:
   - Simulated via `MockPaymentServlet` (`/mock-payos/checkout`).
   - Generates authentic VietQR payment representations, QR download payloads, and beneficiary account details.
   - Simulates gateway webhook notifications, real-time WebSocket payment detection, and automated 15-minute timeout hold expirations.
   - Developer sandbox dock allows instant testing of successful transfers and transaction failures.

2. **MQTT IoT Door Locks & Physical Sensors**:
   - ESP32 digital keypad door locks and PIR motion sensors are modeled via `DeviceService`, `MqttService`, and `SchedulerJobs`.
   - Simulated bidirectional telemetry captures keypad unlock attempts, wrong PIN retries, and motion distance readings with realistic surveillance snapshots (`/assets/images/snapshots/`).
   - The scheduled background job (`job/FastCloseJobWorker`) automatically marks devices offline if their heartbeat timestamp exceeds 5 minutes, creates single `DEVICE_OFFLINE` alerts without duplicates, and automatically releases expired room holds.
