<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Revenue report" active="revenue">
    <!-- Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Revenue report</li>
        </ol>
    </nav>

    <div class="d-flex justify-content-between align-items-baseline mb-3">
        <div>
            <h1 class="h2 mb-1">Financial Revenue Report</h1>
            <p class="text-muted small mb-0">Aggregated revenue based on confirmed payments grouped by day, month, or year.</p>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <!-- Filter Form (Default Last 30 Days) -->
    <div class="card mb-4 shadow-sm border-0">
        <div class="card-body">
            <form action="${pageContext.request.contextPath}/admin/reports/revenue" method="get" class="row g-3 needs-validation" novalidate>
                <div class="col-md-3">
                    <label for="mode" class="form-label small fw-semibold">Group by</label>
                    <select class="form-select form-select-sm" id="mode" name="mode">
                        <option value="day" ${mode == 'day' ? 'selected' : ''}>Day (Daily)</option>
                        <option value="month" ${mode == 'month' ? 'selected' : ''}>Month (Monthly)</option>
                        <option value="year" ${mode == 'year' ? 'selected' : ''}>Year (Yearly)</option>
                    </select>
                </div>

                <div class="col-md-3">
                    <label for="from" class="form-label small fw-semibold">From date</label>
                    <input type="date" class="form-control form-control-sm ${errors['from'] != null ? 'is-invalid' : ''}"
                           id="from" name="from" data-datepicker value="${e:esc(from)}">
                    <t:if test="${not empty errors['from']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['from'])}</div>
                    </t:if>
                </div>

                <div class="col-md-3">
                    <label for="to" class="form-label small fw-semibold">To date</label>
                    <input type="date" class="form-control form-control-sm ${errors['to'] != null ? 'is-invalid' : ''}"
                           id="to" name="to" data-datepicker value="${e:esc(to)}">
                    <t:if test="${not empty errors['to']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['to'])}</div>
                    </t:if>
                </div>

                <div class="col-md-3 d-flex align-items-end gap-2">
                    <button type="submit" class="btn btn-primary btn-sm px-3">Filter</button>
                    <a href="${pageContext.request.contextPath}/admin/reports/revenue" class="btn btn-outline-secondary btn-sm px-3">Reset</a>
                </div>
            </form>
        </div>
    </div>

    <!-- 5 Operational & Financial KPI Cards -->
    <div class="row g-3 mb-3">
        <!-- 1. Total Revenue -->
        <div class="col-6 col-md-4 col-xl">
            <div class="card shadow-sm border-0 h-100">
                <div class="card-body p-3">
                    <span class="text-muted small fw-semibold text-uppercase" style="font-size: 0.72rem;">Total Revenue</span>
                    <div class="h4 fw-bold text-success mb-0 mt-1">${e:money(totalRevenue)}</div>
                    <span class="text-muted small" style="font-size: 0.75rem;">Selected period</span>
                </div>
            </div>
        </div>

        <!-- 2. Paid Bookings -->
        <div class="col-6 col-md-4 col-xl">
            <div class="card shadow-sm border-0 h-100">
                <div class="card-body p-3">
                    <span class="text-muted small fw-semibold text-uppercase" style="font-size: 0.72rem;">Paid Bookings</span>
                    <div class="h4 fw-bold text-espresso mb-0 mt-1">${totalBookings}</div>
                    <span class="text-muted small" style="font-size: 0.75rem;">Transactions</span>
                </div>
            </div>
        </div>

        <!-- 3. Average Per Booking -->
        <div class="col-6 col-md-4 col-xl">
            <div class="card shadow-sm border-0 h-100">
                <div class="card-body p-3">
                    <span class="text-muted small fw-semibold text-uppercase" style="font-size: 0.72rem;">Avg Per Booking</span>
                    <div class="h4 fw-bold text-dark mb-0 mt-1">${e:money(kpis.avgPerBooking)}</div>
                    <span class="text-muted small" style="font-size: 0.75rem;">Revenue / booking</span>
                </div>
            </div>
        </div>

        <!-- 4. Occupancy Rate -->
        <div class="col-6 col-md-6 col-xl">
            <div class="card shadow-sm border-0 h-100">
                <div class="card-body p-3">
                    <span class="text-muted small fw-semibold text-uppercase" style="font-size: 0.72rem;">Occupancy Rate</span>
                    <div class="h4 fw-bold text-espresso mb-0 mt-1">${kpis.occupancyRate}%</div>
                    <span class="text-muted small" style="font-size: 0.75rem;">${kpis.bookedSlots} / ${kpis.totalCapacitySlots} slots</span>
                </div>
            </div>
        </div>

        <!-- 5. Change vs Previous Period -->
        <div class="col-12 col-md-6 col-xl">
            <div class="card shadow-sm border-0 h-100">
                <div class="card-body p-3">
                    <span class="text-muted small fw-semibold text-uppercase" style="font-size: 0.72rem;">Change vs Prev Period</span>
                    <div class="h4 fw-bold mb-0 mt-1">
                        <t:if test="${kpis.changeDirection == 'up'}">
                            <span class="text-success"><i class="bi bi-arrow-up-right me-1"></i>+${kpis.changePercent}%</span>
                        </t:if>
                        <t:if test="${kpis.changeDirection == 'down'}">
                            <span class="text-danger"><i class="bi bi-arrow-down-right me-1"></i>${kpis.changePercent}%</span>
                        </t:if>
                        <t:if test="${kpis.changeDirection == 'flat'}">
                            <span class="text-muted"><i class="bi bi-dash me-1"></i>0.0%</span>
                        </t:if>
                    </div>
                    <span class="text-muted small" style="font-size: 0.75rem;">Preceding ${mode} window</span>
                </div>
            </div>
        </div>
    </div>

    <!-- Exclusion Note -->
    <div class="alert alert-light border small text-muted d-flex align-items-center mb-4 py-2">
        <i class="bi bi-info-circle text-caramel me-2 fs-6"></i>
        <span><strong>Note:</strong> Revenue totals strictly exclude cancelled bookings and refunded payments.</span>
    </div>

    <!-- Horizontal View Tabs (English Only): Overview Chart vs Detailed Table -->
    <ul class="nav nav-pills mb-3 gap-2" id="revenueViewTabs" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link active rounded-pill px-4 py-2 fw-semibold" id="tab-chart-btn" data-bs-toggle="tab" data-bs-target="#tab-chart-pane" type="button" role="tab" aria-controls="tab-chart-pane" aria-selected="true">
                <i class="bi bi-bar-chart-line me-1"></i> Overview Chart
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link rounded-pill px-4 py-2 fw-semibold" id="tab-table-btn" data-bs-toggle="tab" data-bs-target="#tab-table-pane" type="button" role="tab" aria-controls="tab-table-pane" aria-selected="false">
                <i class="bi bi-table me-1"></i> Detailed Table
            </button>
        </li>
    </ul>

    <div class="tab-content" id="revenueTabContent">
        <!-- Tab 1: Chart Section -->
        <div class="tab-pane fade show active" id="tab-chart-pane" role="tabpanel" aria-labelledby="tab-chart-btn">
            <!-- Timeline Chart Card -->
            <div class="card shadow-sm border-0 mb-4">
                <div class="card-header bg-white py-3 border-0">
                    <h5 class="card-title h6 mb-0 fw-bold">Revenue Timeline (${e:esc(mode)})</h5>
                </div>
                <div class="card-body">
                    <div id="revenueChartStatus"></div>
                    <div style="height: 320px; position: relative;">
                        <canvas id="revenueChart"></canvas>
                    </div>
                </div>
            </div>

            <!-- Second Breakdown: Revenue by Room and by Slot (S1-S4) -->
            <div class="row g-4 mb-4">
                <!-- Revenue by Room -->
                <div class="col-lg-6">
                    <div class="card shadow-sm border-0 h-100">
                        <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                            <h5 class="card-title h6 mb-0 fw-bold">Revenue by Room</h5>
                            <span class="badge bg-light text-espresso border">${empty revenueByRoom ? 0 : revenueByRoom.size()} Rooms</span>
                        </div>
                        <div class="card-body p-0">
                            <div class="table-responsive">
                                <table class="table table-hover align-middle mb-0">
                                    <thead class="table-light">
                                        <tr>
                                            <th scope="col" class="ps-3">Room</th>
                                            <th scope="col" class="text-center">Bookings</th>
                                            <th scope="col" class="text-end">Revenue (VND)</th>
                                            <th scope="col" class="pe-3" style="width: 110px;">Share</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <t:forEach items="${revenueByRoom}" var="rm">
                                            <tr>
                                                <td class="ps-3">
                                                    <span class="badge bg-light text-dark border me-1">Room ${e:esc(rm.roomNumber)}</span>
                                                    <span class="small fw-semibold text-espresso">${e:esc(rm.displayName)}</span>
                                                </td>
                                                <td class="text-center font-monospace small">${rm.bookingCount}</td>
                                                <td class="text-end font-monospace small fw-semibold ${rm.revenue > 0 ? 'text-success' : 'text-muted'}">${e:money(rm.revenue)}</td>
                                                <td class="pe-3">
                                                    <div class="d-flex align-items-center gap-1">
                                                        <div class="progress flex-grow-1" style="height: 6px;">
                                                            <div class="progress-bar" role="progressbar" style="width: ${rm.percent}%; background-color: #9A5B2E;" aria-valuenow="${rm.percent}" aria-valuemin="0" aria-valuemax="100"></div>
                                                        </div>
                                                        <span class="small text-muted font-monospace" style="font-size: 0.7rem; width: 32px; text-align: right;">${rm.percent}%</span>
                                                    </div>
                                                </td>
                                            </tr>
                                        </t:forEach>
                                        <t:if test="${empty revenueByRoom}">
                                            <tr><td colspan="4" class="text-center py-3 text-muted small">No room revenue data</td></tr>
                                        </t:if>
                                    </tbody>
                                    <tfoot class="table-light fw-bold border-top">
                                        <tr>
                                            <td class="ps-3 small">Total</td>
                                            <td class="text-center font-monospace small">${totalBookings}</td>
                                            <td class="text-end font-monospace small text-success">${e:money(totalRevenue)}</td>
                                            <td class="pe-3 text-end small font-monospace">100%</td>
                                        </tr>
                                    </tfoot>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Revenue by Slot (S1-S4) -->
                <div class="col-lg-6">
                    <div class="card shadow-sm border-0 h-100">
                        <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                            <h5 class="card-title h6 mb-0 fw-bold">Revenue by Time Slot (S1–S4)</h5>
                            <span class="badge bg-light text-espresso border">4 Slots</span>
                        </div>
                        <div class="card-body p-0">
                            <div class="table-responsive">
                                <table class="table table-hover align-middle mb-0">
                                    <thead class="table-light">
                                        <tr>
                                            <th scope="col" class="ps-3">Slot</th>
                                            <th scope="col" class="text-center">Slots Booked</th>
                                            <th scope="col" class="text-end">Revenue (VND)</th>
                                            <th scope="col" class="pe-3" style="width: 110px;">Share</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <t:forEach items="${revenueBySlot}" var="sl">
                                            <tr>
                                                <td class="ps-3">
                                                    <span class="badge bg-light text-espresso border font-monospace me-1">${e:esc(sl.slotCode)}</span>
                                                    <span class="small fw-semibold text-espresso">${e:esc(sl.slotName)}</span>
                                                </td>
                                                <td class="text-center font-monospace small">${sl.slotCount}</td>
                                                <td class="text-end font-monospace small fw-semibold ${sl.revenue > 0 ? 'text-success' : 'text-muted'}">${e:money(sl.revenue)}</td>
                                                <td class="pe-3">
                                                    <div class="d-flex align-items-center gap-1">
                                                        <div class="progress flex-grow-1" style="height: 6px;">
                                                            <div class="progress-bar" role="progressbar" style="width: ${sl.percent}%; background-color: #9A5B2E;" aria-valuenow="${sl.percent}" aria-valuemin="0" aria-valuemax="100"></div>
                                                        </div>
                                                        <span class="small text-muted font-monospace" style="font-size: 0.7rem; width: 32px; text-align: right;">${sl.percent}%</span>
                                                    </div>
                                                </td>
                                            </tr>
                                        </t:forEach>
                                        <t:if test="${empty revenueBySlot}">
                                            <tr><td colspan="4" class="text-center py-3 text-muted small">No slot revenue data</td></tr>
                                        </t:if>
                                    </tbody>
                                    <tfoot class="table-light fw-bold border-top">
                                        <tr>
                                            <td class="ps-3 small">Total</td>
                                            <td class="text-center font-monospace small">${kpis.bookedSlots}</td>
                                            <td class="text-end font-monospace small text-success">${e:money(totalRevenue)}</td>
                                            <td class="pe-3 text-end small font-monospace">100%</td>
                                        </tr>
                                    </tfoot>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Tab 2: Detailed Table Section with Zero Revenue Filter Switch -->
        <div class="tab-pane fade" id="tab-table-pane" role="tabpanel" aria-labelledby="tab-table-btn">
            <div class="card shadow-sm border-0 mb-4">
                <div class="card-header bg-white py-3 border-0 d-flex justify-content-between align-items-center flex-wrap gap-2">
                    <h5 class="card-title h6 mb-0 fw-bold">Revenue Details Table</h5>
                    <!-- Quick Toggle: Hide 0 VND rows -->
                    <div class="form-check form-switch m-0 d-flex align-items-center gap-2">
                        <input class="form-check-input" type="checkbox" id="hideZeroRevenueSwitch" role="switch">
                        <label class="form-check-label small fw-semibold text-muted" for="hideZeroRevenueSwitch">
                            Hide zero-revenue rows
                        </label>
                    </div>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive" style="max-height: 680px; overflow-y: auto;">
                        <table class="table table-hover table-striped align-middle mb-0" id="revenueDetailsTable">
                            <thead class="table-light sticky-top" style="position: sticky; top: 0; z-index: 10; box-shadow: 0 1px 2px rgba(0,0,0,0.06);">
                                <tr>
                                    <th scope="col" class="ps-3">Period</th>
                                    <th scope="col" class="text-center">Bookings Paid</th>
                                    <th scope="col" class="pe-3 text-end">Revenue (VND)</th>
                                </tr>
                            </thead>
                            <tbody>
                                <t:if test="${empty buckets}">
                                    <tr><td colspan="3" class="text-center py-4 text-muted">No revenue in this range.</td></tr>
                                </t:if>
                                <t:forEach items="${buckets}" var="b">
                                    <tr class="revenue-row ${b.revenue == 0 ? 'zero-revenue-row' : ''}">
                                        <td class="ps-3 font-monospace">${e:esc(b.period)}</td>
                                        <td class="text-center font-monospace">${b.bookingCount}</td>
                                        <td class="pe-3 text-end font-monospace fw-semibold ${b.revenue > 0 ? 'text-success' : 'text-muted'}">${e:money(b.revenue)}</td>
                                    </tr>
                                </t:forEach>
                            </tbody>
                            <tfoot class="table-light fw-bold border-top">
                                <tr>
                                    <td class="ps-3">Total</td>
                                    <td class="text-center font-monospace">${totalBookings}</td>
                                    <td class="pe-3 text-end font-monospace text-success">${e:money(totalRevenue)}</td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="${pageContext.request.contextPath}/assets/vendor/chartjs/chart.umd.min.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/chart-loader.js"></script>
    <script>
        (function() {
            var mode = '${e:esc(mode)}';
            var fromDate = '${e:esc(from)}';
            var toDate = '${e:esc(to)}';
            var cacheKey = 'smartstay.chart.revenue.' + mode + '.' + fromDate + '.' + toDate;
            var dataUrl = '${pageContext.request.contextPath}/admin/reports/revenue/data?mode=' + encodeURIComponent(mode)
                        + '&from=' + encodeURIComponent(fromDate)
                        + '&to=' + encodeURIComponent(toDate);

            if (window.SmartStayCharts) {
                window.SmartStayCharts.load({
                    canvasId: 'revenueChart',
                    statusId: 'revenueChartStatus',
                    url: dataUrl,
                    cacheKey: cacheKey,
                    build: function(data) {
                        var statusEl = document.getElementById('revenueChartStatus');
                        if (!data || !data.labels || data.labels.length === 0 || data.totalRevenue === 0) {
                            if (statusEl) {
                                statusEl.innerHTML = '<div class="alert alert-info py-2 px-3 small">No revenue in this range.</div>';
                            }
                            return {
                                type: 'bar',
                                data: { labels: (data && data.labels) ? data.labels : [], datasets: [] },
                                options: { responsive: true, maintainAspectRatio: false }
                            };
                        }

                        // Format labels for X-axis (dd/MM for day mode)
                        var formattedLabels = data.labels.map(function(lbl) {
                            if (mode === 'day' && lbl && lbl.length === 10 && lbl.indexOf('-') === 4) {
                                var parts = lbl.split('-');
                                return parts[2] + '/' + parts[1];
                            }
                            return lbl;
                        });

                        return {
                            type: 'bar',
                            data: {
                                labels: formattedLabels,
                                datasets: [{
                                    label: 'Revenue (\u20AB)',
                                    data: data.values,
                                    backgroundColor: '#9A5B2E',
                                    borderRadius: 4
                                }]
                            },
                            options: {
                                responsive: true,
                                maintainAspectRatio: false,
                                plugins: {
                                    tooltip: {
                                        callbacks: {
                                            title: function(items) {
                                                if (items && items.length > 0) {
                                                    var idx = items[0].dataIndex;
                                                    return data.labels[idx] || items[0].label;
                                                }
                                                return '';
                                            },
                                            label: function(ctx) {
                                                var val = ctx.raw || 0;
                                                var bCnt = (data.bookingCounts && data.bookingCounts[ctx.dataIndex]) || 0;
                                                return [
                                                    'Revenue: ' + Number(val).toLocaleString('en-US') + ' VND',
                                                    'Paid bookings: ' + bCnt
                                                ];
                                            }
                                        }
                                    }
                                },
                                scales: {
                                    x: {
                                        grid: { display: false },
                                        ticks: {
                                            autoSkip: true,
                                            maxRotation: 0,
                                            minRotation: 0,
                                            font: { size: 11 }
                                        }
                                    },
                                    y: {
                                        beginAtZero: true,
                                        grid: { color: 'rgba(74, 51, 38, 0.08)' },
                                        ticks: {
                                            font: { size: 11 },
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
            }

            var zeroSwitch = document.getElementById('hideZeroRevenueSwitch');
            if (zeroSwitch) {
                zeroSwitch.addEventListener('change', function() {
                    var zeroRows = document.querySelectorAll('.zero-revenue-row');
                    zeroRows.forEach(function(row) {
                        row.style.display = zeroSwitch.checked ? 'none' : '';
                    });
                });
            }

            var chartTabBtn = document.getElementById('tab-chart-btn');
            if (chartTabBtn) {
                chartTabBtn.addEventListener('shown.bs.tab', function() {
                    window.dispatchEvent(new Event('resize'));
                });
            }
        })();
    </script>
</t:layout>
