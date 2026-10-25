<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Dashboard" active="dashboard">
    <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
        <div>
            <h1 class="h2 mb-1">Operational dashboard</h1>
            <p class="text-muted small mb-0">Overview of room occupancy, daily bookings, revenue KPIs, devices, and security alerts.</p>
        </div>
        <div class="d-flex align-items-center gap-2">
            <button id="btnSyncNow" class="btn btn-outline-primary btn-sm rounded-pill px-3 shadow-2xs d-inline-flex align-items-center gap-2 fw-semibold"
                    onclick="window.triggerManualSync(this)" title="Synchronize system data immediately">
                <i class="bi bi-arrow-repeat fs-6"></i>
                <span id="syncText">Sync Now</span>
            </button>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <!-- Room Status KPI Summary Strip (Task 5: Quick Glance KPI) -->
    <div class="card border-0 shadow-2xs mb-4" style="background: linear-gradient(135deg, #FAF5EC 0%, #FFFDF9 100%); border-left: 4px solid var(--st-terracotta, #9A5B2E) !important; border-radius: 14px;">
        <div class="card-body py-2 px-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
            <div class="d-flex align-items-center gap-2">
                <i class="bi bi-door-open fs-5" style="color: var(--st-espresso, #4A3326);"></i>
                <span class="fw-bold" style="color: var(--st-deep-brown, #2B1B12);">Real-time Room Status:</span>
            </div>
            <div class="d-flex flex-wrap align-items-center gap-2">
                <span class="badge rounded-pill px-3 py-2 bg-espresso">
                    Total: <strong>${(roomCounts['AVAILABLE'] != null ? roomCounts['AVAILABLE'] : 0) + (roomCounts['OCCUPIED'] != null ? roomCounts['OCCUPIED'] : 0) + (roomCounts['NEEDS_CLEANING'] != null ? roomCounts['NEEDS_CLEANING'] : 0) + (roomCounts['MAINTENANCE'] != null ? roomCounts['MAINTENANCE'] : 0)}</strong> rooms
                </span>
                <span class="badge rounded-pill px-3 py-2 badge-boutique-available">
                    Available: <strong>${roomCounts['AVAILABLE'] != null ? roomCounts['AVAILABLE'] : 0}</strong>
                </span>
                <span class="badge rounded-pill px-3 py-2 badge-boutique-occupied">
                    Occupied: <strong>${roomCounts['OCCUPIED'] != null ? roomCounts['OCCUPIED'] : 0}</strong>
                </span>
                <span class="badge rounded-pill px-3 py-2 badge-boutique-cleaning">
                    Needs Cleaning: <strong>${roomCounts['NEEDS_CLEANING'] != null ? roomCounts['NEEDS_CLEANING'] : 0}</strong>
                </span>
                <span class="badge rounded-pill px-3 py-2 badge-boutique-maintenance">
                    Maintenance: <strong>${roomCounts['MAINTENANCE'] != null ? roomCounts['MAINTENANCE'] : 0}</strong>
                </span>
            </div>
        </div>
    </div>

    <!-- Operational KPIs Strip (Task 8: Daily Operations & Open Alerts) -->
    <div class="row g-3 mb-4">
        <!-- Check-ins Today -->
        <div class="col-6 col-md-3">
            <div class="card border-0 shadow-2xs h-100 rounded-4" style="background: linear-gradient(135deg, #FAF5EC 0%, #FFFDF9 100%); border-left: 4px solid var(--st-terracotta, #9A5B2E) !important;">
                <div class="card-body p-3">
                    <div class="d-flex align-items-center justify-content-between mb-1">
                        <span class="small fw-bold text-uppercase text-muted" style="font-size: 0.72rem;">Check-ins Today</span>
                        <i class="bi bi-box-arrow-in-right fs-5 text-terracotta"></i>
                    </div>
                    <div class="h3 fw-bold text-espresso mb-0">${todayBookings['CHECKED_IN'] != null ? todayBookings['CHECKED_IN'] : 0}</div>
                    <div class="text-muted small" style="font-size: 0.75rem;">of ${todayBookings['TOTAL'] != null ? todayBookings['TOTAL'] : 0} total bookings</div>
                </div>
            </div>
        </div>

        <!-- Check-outs Today -->
        <div class="col-6 col-md-3">
            <div class="card border-0 shadow-2xs h-100 rounded-4" style="background: linear-gradient(135deg, #FAF5EC 0%, #FFFDF9 100%); border-left: 4px solid #16A34A !important;">
                <div class="card-body p-3">
                    <div class="d-flex align-items-center justify-content-between mb-1">
                        <span class="small fw-bold text-uppercase text-muted" style="font-size: 0.72rem;">Check-outs Today</span>
                        <i class="bi bi-box-arrow-left fs-5 text-success"></i>
                    </div>
                    <div class="h3 fw-bold text-espresso mb-0">${todayBookings['CHECKED_OUT'] != null ? todayBookings['CHECKED_OUT'] : 0}</div>
                    <div class="text-muted small" style="font-size: 0.75rem;">stays completed</div>
                </div>
            </div>
        </div>

        <!-- Pending Cleaning Tasks -->
        <div class="col-6 col-md-3">
            <div class="card border-0 shadow-2xs h-100 rounded-4" style="background: linear-gradient(135deg, #FAF5EC 0%, #FFFDF9 100%); border-left: 4px solid #D97706 !important;">
                <div class="card-body p-3">
                    <div class="d-flex align-items-center justify-content-between mb-1">
                        <span class="small fw-bold text-uppercase text-muted" style="font-size: 0.72rem;">Pending Cleaning</span>
                        <i class="bi bi-brush fs-5" style="color: #D97706;"></i>
                    </div>
                    <div class="h3 fw-bold text-espresso mb-0">${pendingCleaningCount}</div>
                    <div class="text-muted small" style="font-size: 0.75rem;">rooms need housekeeping</div>
                </div>
            </div>
        </div>

        <!-- Open Alerts by Severity -->
        <div class="col-6 col-md-3">
            <div class="card border-0 shadow-2xs h-100 rounded-4" style="background: linear-gradient(135deg, #FAF5EC 0%, #FFFDF9 100%); border-left: 4px solid var(--sev-critical, #DC2626) !important;">
                <div class="card-body p-3">
                    <div class="d-flex align-items-center justify-content-between mb-1">
                        <span class="small fw-bold text-uppercase text-muted" style="font-size: 0.72rem;">Open Alerts</span>
                        <i class="bi bi-shield-exclamation fs-5 text-danger"></i>
                    </div>
                    <div class="d-flex flex-wrap gap-1 align-items-center mt-1">
                        <span class="badge badge-sev-critical py-1 px-2" title="Critical">Crit: <strong>${openAlerts['CRITICAL'] != null ? openAlerts['CRITICAL'] : 0}</strong></span>
                        <span class="badge badge-sev-high py-1 px-2" title="High">High: <strong>${openAlerts['HIGH'] != null ? openAlerts['HIGH'] : 0}</strong></span>
                        <span class="badge badge-sev-medium py-1 px-2" title="Medium">Med: <strong>${openAlerts['MEDIUM'] != null ? openAlerts['MEDIUM'] : 0}</strong></span>
                        <span class="badge badge-sev-low py-1 px-2" title="Low">Low: <strong>${openAlerts['LOW'] != null ? openAlerts['LOW'] : 0}</strong></span>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Navigation Tabs for Zero-Scroll Dashboard (Task 5: Tabbed Dashboard) -->
    <ul class="nav nav-pills nav-fill mb-4 p-1 rounded-4" id="dashboardTabs" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link active fw-bold py-2 px-3 rounded-pill d-flex align-items-center justify-content-center gap-2"
                    id="tab-occupancy-btn" data-bs-toggle="pill" data-bs-target="#tab-occupancy" type="button" role="tab" aria-controls="tab-occupancy" aria-selected="true">
                <i class="bi bi-door-closed fs-6"></i>
                <span>Occupancy &amp; Hardware</span>
                <span class="badge rounded-pill ms-1 badge-boutique-available">${roomCounts['AVAILABLE'] != null ? roomCounts['AVAILABLE'] : 0} Available</span>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link fw-bold py-2 px-3 rounded-pill d-flex align-items-center justify-content-center gap-2"
                    id="tab-bookings-revenue-btn" data-bs-toggle="pill" data-bs-target="#tab-bookings-revenue" type="button" role="tab" aria-controls="tab-bookings-revenue" aria-selected="false">
                <i class="bi bi-graph-up-arrow fs-6"></i>
                <span>Bookings &amp; Revenue</span>
                <span class="badge rounded-pill ms-1 badge-boutique-occupied">${todayBookings['TOTAL'] != null ? todayBookings['TOTAL'] : 0} Bookings</span>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link fw-bold py-2 px-3 rounded-pill d-flex align-items-center justify-content-center gap-2"
                    id="tab-security-btn" data-bs-toggle="pill" data-bs-target="#tab-security" type="button" role="tab" aria-controls="tab-security" aria-selected="false">
                <i class="bi bi-shield-check fs-6"></i>
                <span>Security &amp; Alerts</span>
                <t:if test="${newAlertCount > 0}">
                    <span class="badge rounded-pill ms-1 badge-boutique-alert">${newAlertCount} NEW</span>
                </t:if>
            </button>
        </li>
    </ul>

    <!-- Tab Contents -->
    <div class="tab-content" id="dashboardTabsContent">
        <!-- ========================================== -->
        <!-- TAB 1: OCCUPANCY & HARDWARE -->
        <!-- ========================================== -->
        <div class="tab-pane fade show active" id="tab-occupancy" role="tabpanel" aria-labelledby="tab-occupancy-btn">
            <div class="row g-4 mb-4">
                <!-- Room Status Card -->
                <div class="col-md-6 col-xl-6">
                    <div class="card shadow-sm h-100 border-0 rounded-4">
                        <div class="card-body p-4">
                            <div class="d-flex justify-content-between align-items-center mb-3">
                                <span class="text-muted small fw-bold text-uppercase">Room Occupancy Breakdown</span>
                                <a href="${pageContext.request.contextPath}/admin/rooms" class="btn btn-sm btn-outline-primary rounded-pill px-3">Manage rooms &rarr;</a>
                            </div>
                            <div class="row g-3 mb-3">
                                <div class="col-6">
                                    <div class="p-3 rounded-3 kpi-box-boutique kpi-box-available">
                                        <div class="small fw-semibold kpi-label">Available</div>
                                        <div class="h3 fw-bold kpi-num mb-0">${roomCounts['AVAILABLE'] != null ? roomCounts['AVAILABLE'] : 0}</div>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="p-3 rounded-3 kpi-box-boutique kpi-box-occupied">
                                        <div class="small fw-semibold kpi-label">Occupied</div>
                                        <div class="h3 fw-bold kpi-num mb-0">${roomCounts['OCCUPIED'] != null ? roomCounts['OCCUPIED'] : 0}</div>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="p-3 rounded-3 kpi-box-boutique kpi-box-cleaning">
                                        <div class="small fw-semibold kpi-label">Needs Cleaning</div>
                                        <div class="h3 fw-bold kpi-num mb-0">${roomCounts['NEEDS_CLEANING'] != null ? roomCounts['NEEDS_CLEANING'] : 0}</div>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="p-3 rounded-3 kpi-box-boutique kpi-box-maintenance">
                                        <div class="small fw-semibold kpi-label">Maintenance</div>
                                        <div class="h3 fw-bold kpi-num mb-0">${roomCounts['MAINTENANCE'] != null ? roomCounts['MAINTENANCE'] : 0}</div>
                                    </div>
                                </div>
                            </div>
                            <p class="small text-muted mb-0">
                                <i class="bi bi-clock-history me-1"></i>Live room occupancy updated automatically.
                            </p>
                        </div>
                    </div>
                </div>

                <!-- Hardware Devices Card -->
                <div class="col-md-6 col-xl-6">
                    <div class="card shadow-sm h-100 border-0 rounded-4">
                        <div class="card-body p-4">
                            <div class="d-flex justify-content-between align-items-center mb-3">
                                <span class="text-muted small fw-bold text-uppercase">Hardware &amp; IoT Lock Devices</span>
                                <a href="${pageContext.request.contextPath}/admin/devices" class="btn btn-sm btn-outline-primary rounded-pill px-3">Manage devices &rarr;</a>
                            </div>
                            <div class="row g-3 mb-3">
                                <div class="col-4">
                                    <div class="p-3 rounded-3 kpi-box-boutique kpi-box-available text-center">
                                        <div class="small fw-semibold kpi-label">Online</div>
                                        <div class="h3 fw-bold kpi-num mb-0">${deviceCounts['ONLINE'] != null ? deviceCounts['ONLINE'] : 0}</div>
                                    </div>
                                </div>
                                <div class="col-4">
                                    <div class="p-3 rounded-3 kpi-box-boutique kpi-box-maintenance text-center">
                                        <div class="small fw-semibold kpi-label">Offline</div>
                                        <div class="h3 fw-bold kpi-num mb-0">${deviceCounts['OFFLINE'] != null ? deviceCounts['OFFLINE'] : 0}</div>
                                    </div>
                                </div>
                                <div class="col-4">
                                    <div class="p-3 rounded-3 kpi-box-boutique kpi-box-danger text-center">
                                        <div class="small fw-semibold kpi-label">Fault</div>
                                        <div class="h3 fw-bold kpi-num mb-0">${deviceCounts['FAULT'] != null ? deviceCounts['FAULT'] : 0}</div>
                                    </div>
                                </div>
                            </div>
                            <div class="p-3 bg-cream border border-latte rounded-3 d-flex align-items-center justify-content-between">
                                <div>
                                    <div class="fw-semibold small text-espresso">SmartStay ESP32 Door Lock</div>
                                    <div class="text-muted" style="font-size: 0.78rem;">Bidirectional sync for PIN entry, RFID tap, door sensors, and buzzer events.</div>
                                </div>
                                <span class="badge badge-boutique-available rounded-pill px-2 py-1 small">Live Sync Active</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- ========================================== -->
        <!-- TAB 2: BOOKINGS & REVENUE -->
        <!-- ========================================== -->
        <div class="tab-pane fade" id="tab-bookings-revenue" role="tabpanel" aria-labelledby="tab-bookings-revenue-btn">
            <div class="row g-3 mb-4">
                <!-- Today's Bookings Card -->
                <div class="col-sm-6 col-xl-4">
                    <div class="card shadow-sm h-100 border-0 rounded-4">
                        <div class="card-body p-4">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <span class="text-muted small fw-semibold text-uppercase">Today's bookings</span>
                                <a href="${pageContext.request.contextPath}/admin/bookings" class="small text-decoration-none">View all &rarr;</a>
                            </div>
                            <div class="h3 fw-bold mb-2">${todayBookings['TOTAL'] != null ? todayBookings['TOTAL'] : 0} <span class="fs-6 text-muted fw-normal">today</span></div>
                            <div class="small text-muted d-flex flex-wrap gap-2">
                                <span>Confirmed: <strong>${todayBookings['CONFIRMED'] != null ? todayBookings['CONFIRMED'] : 0}</strong></span>
                                <span>Checked in: <strong>${todayBookings['CHECKED_IN'] != null ? todayBookings['CHECKED_IN'] : 0}</strong></span>
                                <span>Completed: <strong>${todayBookings['CHECKED_OUT'] != null ? todayBookings['CHECKED_OUT'] : 0}</strong></span>
                                <span>Expired: <strong>${todayBookings['EXPIRED'] != null ? todayBookings['EXPIRED'] : 0}</strong></span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Revenue Card -->
                <div class="col-sm-6 col-xl-4">
                    <div class="card shadow-sm h-100 border-0 rounded-4">
                        <div class="card-body p-4">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <span class="text-muted small fw-semibold text-uppercase">Revenue (PAID)</span>
                                <a href="${pageContext.request.contextPath}/admin/reports/revenue" class="small text-decoration-none">Report &rarr;</a>
                            </div>
                            <div class="h3 fw-bold text-success mb-2">${e:money(revenueToday)} <span class="fs-6 text-muted fw-normal">today</span></div>
                            <div class="small text-muted">
                                This month: <strong class="text-dark">${e:money(revenueMonth)}</strong>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- No-Show Held Slots Card -->
                <div class="col-sm-6 col-xl-4">
                    <div class="card shadow-sm h-100 border-0 rounded-4">
                        <div class="card-body p-4">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <span class="text-muted small fw-semibold text-uppercase">No-show held bookings</span>
                                <a href="${pageContext.request.contextPath}/admin/bookings?flag=noshow_held" class="small text-decoration-none">Resolve &rarr;</a>
                            </div>
                            <div class="h3 fw-bold ${noshowHeldCount > 0 ? 'text-danger' : 'text-muted'} mb-1">
                                ${noshowHeldCount} <span class="fs-6 fw-normal text-muted">held</span>
                            </div>
                            <div class="small text-muted mt-2">
                                <a href="${pageContext.request.contextPath}/admin/bookings?flag=noshow_held" class="text-decoration-none text-muted">
                                    Expired bookings holding slots &rarr;
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Revenue Last 7 Days (Bar Chart) -->
            <div class="card shadow-sm border-0 rounded-4 mb-4">
                <div class="card-header bg-white py-3 border-0 d-flex justify-content-between align-items-center">
                    <h5 class="card-title h6 mb-0 fw-bold">Revenue &mdash; Last 7 days</h5>
                    <a href="${pageContext.request.contextPath}/admin/reports/revenue" class="small text-decoration-none">Full report &rarr;</a>
                </div>
                <div class="card-body p-4">
                    <div id="revenue7Status"></div>
                    <div style="height: 320px; position: relative;">
                        <canvas id="revenue7Chart"></canvas>
                    </div>
                </div>
            </div>
        </div>

        <!-- ========================================== -->
        <!-- TAB 3: SECURITY & ALERTS -->
        <!-- ========================================== -->
        <div class="tab-pane fade" id="tab-security" role="tabpanel" aria-labelledby="tab-security-btn">
            <div class="row g-4 mb-4">
                <!-- Active Alerts Card -->
                <div class="col-lg-4">
                    <div class="card shadow-sm border-0 rounded-4 h-100">
                        <div class="card-body p-4">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <span class="text-muted small fw-semibold text-uppercase">Security alerts</span>
                                <a href="${pageContext.request.contextPath}/admin/alerts" class="small text-decoration-none">Alerts &rarr;</a>
                            </div>
                            <div class="h3 fw-bold mb-1">
                                <span class="badge ${newAlertCount > 0 ? 'bg-danger' : 'bg-light text-dark border'}">
                                    ${newAlertCount} NEW
                                </span>
                            </div>
                            <div class="small text-muted mt-2">Unacknowledged security notifications</div>
                            <hr>
                            <!-- Alerts by Type: Horizontal Bar List -->
                            <div class="d-flex justify-content-between align-items-center mb-3">
                                <h6 class="fw-bold small text-muted text-uppercase mb-0">Alerts by type</h6>
                                <span class="badge bg-light text-muted border small">${alertsByType.totalCount != null ? alertsByType.totalCount : 0} total</span>
                            </div>
                            <div class="alerts-bar-list d-flex flex-column gap-2" id="alertsBarList">
                                <t:if test="${empty alertsByType.items}">
                                    <div class="text-muted small py-3 text-center">No alert records found.</div>
                                </t:if>
                                <t:forEach items="${alertsByType.items}" var="item">
                                    <div class="alert-bar-item">
                                        <div class="d-flex justify-content-between align-items-center mb-1 small">
                                            <div class="d-flex align-items-center gap-2 text-truncate">
                                                <span class="badge badge-sev-${item.severity == 'CRITICAL' ? 'critical' : (item.severity == 'HIGH' ? 'high' : (item.severity == 'MEDIUM' ? 'medium' : 'low'))} py-0 px-1" style="font-size: 0.68rem;">
                                                    ${item.severity}
                                                </span>
                                                <a href="${pageContext.request.contextPath}/admin/alerts?type=${e:url(item.type)}" class="font-monospace text-dark text-decoration-none fw-semibold text-truncate" title="${e:esc(item.type)}">
                                                    ${e:esc(item.type)}
                                                </a>
                                            </div>
                                            <span class="fw-bold text-espresso ms-2">${item.count}</span>
                                        </div>
                                        <div class="progress" style="height: 6px; background-color: var(--st-cream, #FAF5EC); border-radius: 4px;">
                                            <div class="progress-bar severity-bar-${item.severity == 'CRITICAL' ? 'critical' : (item.severity == 'HIGH' ? 'high' : (item.severity == 'MEDIUM' ? 'medium' : 'low'))}"
                                                 role="progressbar"
                                                 style="width: ${item.percent}%; border-radius: 4px;"
                                                 aria-valuenow="${item.count}" aria-valuemin="0" aria-valuemax="${alertsByType.maxCount}"></div>
                                        </div>
                                    </div>
                                </t:forEach>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Latest Alerts Section Table -->
                <div class="col-lg-8">
                    <div class="card shadow-sm border-0 rounded-4 h-100">
                        <div class="card-header bg-white py-3 border-0 d-flex justify-content-between align-items-center">
                            <h5 class="card-title h6 mb-0 fw-bold">Latest security alerts</h5>
                            <a href="${pageContext.request.contextPath}/admin/alerts" class="btn btn-outline-primary btn-sm rounded-pill px-3">View all alerts</a>
                        </div>
                        <div class="card-body p-0">
                            <div class="table-responsive">
                                <table class="table table-hover align-middle mb-0">
                                    <thead class="table-light">
                                        <tr>
                                            <th scope="col" class="ps-3">Time</th>
                                            <th scope="col">Type</th>
                                            <th scope="col">Severity</th>
                                            <th scope="col">Status</th>
                                            <th scope="col">Message</th>
                                            <th scope="col" class="pe-3 text-end">Action</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        ${empty latestAlerts ? '<tr><td colspan="6" class="text-center py-4 text-muted">No alerts recorded</td></tr>' : ''}
                                        <t:forEach items="${latestAlerts}" var="al">
                                            <tr>
                                                <td class="ps-3 font-monospace small text-muted text-nowrap" style="white-space: nowrap;">${e:datetime(al.createdAt)}</td>
                                                <td><span class="badge bg-light text-dark border font-monospace">${e:esc(al.alertType)}</span></td>
                                                <td>
                                                    <span class="badge badge-sev-${al.severity == 'CRITICAL' ? 'critical' : (al.severity == 'HIGH' ? 'high' : (al.severity == 'MEDIUM' ? 'medium' : 'low'))}">
                                                        ${e:esc(al.severity)}
                                                    </span>
                                                </td>
                                                <td>
                                                    <span class="badge ${al.status == 'NEW' ? 'badge-status-danger' : (al.status == 'ACKNOWLEDGED' ? 'badge-status-warning' : 'badge-status-success')}">
                                                        ${e:esc(al.status)}
                                                    </span>
                                                </td>
                                                <td class="small text-break">${e:esc(al.message)}</td>
                                                <td class="pe-3 text-end">
                                                    <a href="${pageContext.request.contextPath}/admin/alerts?keyword=${e:url(al.message)}" class="btn btn-sm btn-outline-soft">Review</a>
                                                </td>
                                            </tr>
                                        </t:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Chart.js local vendor and loader scripts -->
    <script src="${pageContext.request.contextPath}/assets/vendor/chartjs/chart.umd.min.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/chart-loader.js"></script>
    <script>
        (function() {
            var chartsUrl = '${pageContext.request.contextPath}/admin/dashboard/charts';

            // Resize charts upon switching tabs
            document.querySelectorAll('#dashboardTabs button[data-bs-toggle="pill"]').forEach(function(pillBtn) {
                pillBtn.addEventListener('shown.bs.tab', function() {
                    setTimeout(function() {
                        window.dispatchEvent(new Event('resize'));
                    }, 50);
                });
            });

            if (window.SmartStayCharts) {
                // 1. Revenue Last 7 Days (Bar Chart)
                window.SmartStayCharts.load({
                    canvasId: 'revenue7Chart',
                    statusId: 'revenue7Status',
                    url: chartsUrl,
                    cacheKey: 'smartstay.chart.revenue7',
                    build: function(data) {
                        var statusEl = document.getElementById('revenue7Status');
                        var rData = (data && data.revenue7) ? data.revenue7 : data;
                        if (!rData || !rData.labels || rData.labels.length === 0) {
                            if (statusEl) {
                                statusEl.innerHTML = '<div class="alert alert-info py-2 px-3 small">No revenue data for the last 7 days.</div>';
                            }
                            return {
                                type: 'bar',
                                data: { labels: [], datasets: [] },
                                options: { responsive: true, maintainAspectRatio: false }
                            };
                        }

                        return {
                            type: 'bar',
                            data: {
                                labels: rData.labels,
                                datasets: [{
                                    label: 'Revenue (VND)',
                                    data: rData.values,
                                    backgroundColor: (window.SmartStayCharts && window.SmartStayCharts.palette) ? window.SmartStayCharts.palette.barPrimary : '#9A5B2E',
                                    borderRadius: 4
                                }]
                            },
                            options: {
                                responsive: true,
                                maintainAspectRatio: false,
                                scales: {
                                    y: {
                                        beginAtZero: true,
                                        ticks: {
                                            callback: function(val) {
                                                if (val >= 1000000) {
                                                    return (val / 1000000).toLocaleString('en-US', { maximumFractionDigits: 1 }) + 'M';
                                                } else if (val >= 1000) {
                                                    return (val / 1000).toLocaleString('en-US', { maximumFractionDigits: 0 }) + 'k';
                                                }
                                                return Number(val).toLocaleString('en-US');
                                            }
                                        }
                                    }
                                }
                            }
                        };
                    }
                });

                // Alerts by Type is rendered as a clean horizontal bar list with severity tokens and numbers
            }
        })();
    </script>
</t:layout>