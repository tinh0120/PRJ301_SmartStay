<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Revenue report" active="revenue">
    <%-- R11: 4 KPIs (change chip inside Total revenue), quick ranges, no nested scroll, server-side CSV. --%>
    <div class="ss-page-header">
        <div>
            <h1 class="ss-page-header__title">Revenue</h1>
            <p class="ss-page-header__subtitle">Paid bookings in the selected period
                <i class="bi bi-info-circle ss-rev__info" tabindex="0" role="img" aria-label="Cancelled and refunded bookings are excluded"
                   data-bs-toggle="tooltip" title="Cancelled and refunded bookings are excluded"></i>
            </p>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <!-- Range toolbar -->
    <form action="${pageContext.request.contextPath}/admin/reports/revenue" method="get" class="ss-toolbar ss-rev__toolbar">
        <div class="ss-toolbar__main">
            <div class="ss-toolbar__chips" role="group" aria-label="Quick range">
                <a class="ss-chip ${activeRange == '7d' ? 'ss-chip--active' : ''}" ${activeRange == '7d' ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/reports/revenue?mode=${e:esc(mode)}&amp;range=7d">7 days</a>
                <a class="ss-chip ${activeRange == '30d' ? 'ss-chip--active' : ''}" ${activeRange == '30d' ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/reports/revenue?mode=${e:esc(mode)}&amp;range=30d">30 days</a>
                <a class="ss-chip ${activeRange == '90d' ? 'ss-chip--active' : ''}" ${activeRange == '90d' ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/reports/revenue?mode=${e:esc(mode)}&amp;range=90d">90 days</a>
                <a class="ss-chip ${activeRange == 'month' ? 'ss-chip--active' : ''}" ${activeRange == 'month' ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/reports/revenue?mode=${e:esc(mode)}&amp;range=month">This month</a>
            </div>
            <div class="ss-rev__dates">
                <label for="from" class="visually-hidden">From</label>
                <input type="date" class="form-control form-control-sm" id="from" name="from" data-datepicker value="${e:esc(from)}">
                <span class="ss-rev__dash" aria-hidden="true">&ndash;</span>
                <label for="to" class="visually-hidden">To</label>
                <input type="date" class="form-control form-control-sm" id="to" name="to" data-datepicker value="${e:esc(to)}">
            </div>
            <div class="ss-rev__mode">
                <label for="mode" class="visually-hidden">Group by</label>
                <select class="form-select form-select-sm" id="mode" name="mode">
                    <option value="day" ${mode == 'day' ? 'selected' : ''}>By day</option>
                    <option value="month" ${mode == 'month' ? 'selected' : ''}>By month</option>
                    <option value="year" ${mode == 'year' ? 'selected' : ''}>By year</option>
                </select>
            </div>
            <button type="submit" class="btn btn-primary btn-sm px-3">Apply</button>
        </div>
        <t:if test="${not empty errors['from']}"><div class="invalid-feedback d-block">${e:esc(errors['from'])}</div></t:if>
        <t:if test="${not empty errors['to']}"><div class="invalid-feedback d-block">${e:esc(errors['to'])}</div></t:if>
    </form>

    <!-- 4 KPIs -->
    <div class="ss-rev__kpis">
        <div class="ss-kpi ss-rev__kpi">
            <span class="ss-kpi__label">Total revenue</span>
            <span class="ss-kpi__value">${e:money(totalRevenue)}</span>
            <t:if test="${not empty changePct}">
                <span class="ss-rev__change ss-rev__change--${changeDir}"
                      title="Previous ${e:esc(mode == 'day' ? 'period of the same length' : mode)}: ${e:money(kpis.prevRevenue)}">
                    <i class="bi ${changeDir == 'up' ? 'bi-arrow-up-right' : (changeDir == 'down' ? 'bi-arrow-down-right' : 'bi-dash')}" aria-hidden="true"></i>
                    ${changePct > 0 ? '+' : ''}${changePct}% vs previous period
                </span>
            </t:if>
            <t:if test="${empty changePct}">
                <span class="ss-rev__change ss-rev__change--flat">No revenue in the previous period</span>
            </t:if>
        </div>
        <div class="ss-kpi ss-rev__kpi">
            <span class="ss-kpi__label">Paid bookings</span>
            <span class="ss-kpi__value">${totalBookings}</span>
        </div>
        <div class="ss-kpi ss-rev__kpi">
            <span class="ss-kpi__label">Avg per booking</span>
            <span class="ss-kpi__value">${e:money(totalAvg)}</span>
        </div>
        <div class="ss-kpi ss-rev__kpi">
            <span class="ss-kpi__label">Occupancy rate</span>
            <span class="ss-kpi__value">${empty kpis.occupancyRate ? 0 : kpis.occupancyRate}%</span>
            <span class="ss-kpi__of">${kpis.bookedSlots} of ${kpis.totalCapacitySlots} slots</span>
        </div>
    </div>

    <!-- View tabs: Overview vs Detailed table -->
    <ul class="nav ss-tabs mb-3" id="revenueViewTabs" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="ss-tabs__link ss-tabs__link--active active" id="tab-chart-btn" data-bs-toggle="tab" data-bs-target="#tab-chart-pane"
                    type="button" role="tab" aria-controls="tab-chart-pane" aria-selected="true">Overview</button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="ss-tabs__link" id="tab-table-btn" data-bs-toggle="tab" data-bs-target="#tab-table-pane"
                    type="button" role="tab" aria-controls="tab-table-pane" aria-selected="false">Detailed table</button>
        </li>
    </ul>

    <div class="tab-content" id="revenueTabContent">
        <!-- Overview -->
        <div class="tab-pane fade show active" id="tab-chart-pane" role="tabpanel" aria-labelledby="tab-chart-btn">
            <section class="ss-rev__card">
                <h2 class="ss-rev__card-title">Revenue ${mode == 'day' ? 'per day' : (mode == 'month' ? 'per month' : 'per year')}</h2>
                <div id="revenueChartStatus"></div>
                <div class="ss-rev__chart">
                    <canvas id="revenueChart"></canvas>
                </div>
            </section>

            <div class="ss-rev__split">
                <section class="ss-rev__card">
                    <h2 class="ss-rev__card-title">By room</h2>
                    <t:if test="${empty revenueByRoom}"><p class="ss-rev__empty">No room revenue in this range.</p></t:if>
                    <t:if test="${not empty revenueByRoom}">
                        <table class="ss-rev__share">
                            <thead>
                                <tr><th scope="col">Room</th><th scope="col" class="ss-rev__num">Bookings</th><th scope="col" class="ss-rev__num">Revenue</th><th scope="col">Share</th></tr>
                            </thead>
                            <tbody>
                                <t:forEach items="${revenueByRoom}" var="rm">
                                    <tr>
                                        <td><span class="ss-rev__name">Room ${e:esc(rm.roomNumber)}</span> <span class="ss-rev__sub">${e:esc(rm.displayName)}</span></td>
                                        <td class="ss-rev__num">${rm.bookingCount}</td>
                                        <td class="ss-rev__num">${e:money(rm.revenue)}</td>
                                        <td class="ss-rev__bar-cell">
                                            <span class="ss-rev__bar" aria-hidden="true"><span style="width: ${rm.percent}%;"></span></span>
                                            <span class="ss-rev__pct">${rm.percent}%</span>
                                        </td>
                                    </tr>
                                </t:forEach>
                            </tbody>
                        </table>
                    </t:if>
                </section>

                <section class="ss-rev__card">
                    <h2 class="ss-rev__card-title">By time slot</h2>
                    <t:if test="${empty revenueBySlot}"><p class="ss-rev__empty">No slot revenue in this range.</p></t:if>
                    <t:if test="${not empty revenueBySlot}">
                        <table class="ss-rev__share">
                            <thead>
                                <tr><th scope="col">Slot</th><th scope="col" class="ss-rev__num">Slots booked</th><th scope="col" class="ss-rev__num">Revenue</th><th scope="col">Share</th></tr>
                            </thead>
                            <tbody>
                                <t:forEach items="${revenueBySlot}" var="sl">
                                    <tr>
                                        <td><span class="ss-rev__name">${e:esc(sl.slotName)}</span> <span class="ss-rev__sub">${e:esc(sl.slotCode)}</span></td>
                                        <td class="ss-rev__num">${sl.slotCount}</td>
                                        <td class="ss-rev__num">${e:money(sl.revenue)}</td>
                                        <td class="ss-rev__bar-cell">
                                            <span class="ss-rev__bar" aria-hidden="true"><span style="width: ${sl.percent}%;"></span></span>
                                            <span class="ss-rev__pct">${sl.percent}%</span>
                                        </td>
                                    </tr>
                                </t:forEach>
                            </tbody>
                        </table>
                    </t:if>
                </section>
            </div>
        </div>

        <!-- Detailed table: one page scroll, sticky header and total row -->
        <div class="tab-pane fade" id="tab-table-pane" role="tabpanel" aria-labelledby="tab-table-btn">
            <section class="ss-rev__card">
                <div class="ss-rev__card-head">
                    <h2 class="ss-rev__card-title">Revenue ${mode == 'day' ? 'per day' : (mode == 'month' ? 'per month' : 'per year')}</h2>
                    <div class="ss-rev__card-actions">
                        <div class="form-check form-switch m-0">
                            <input class="form-check-input" type="checkbox" id="hideZeroRevenueSwitch" role="switch">
                            <label class="form-check-label" for="hideZeroRevenueSwitch">Hide zero rows</label>
                        </div>
                        <a class="btn btn-outline-soft btn-sm" id="revenueExportBtn"
                           href="${pageContext.request.contextPath}/admin/reports/revenue/export?mode=${e:esc(mode)}&amp;from=${e:esc(from)}&amp;to=${e:esc(to)}"
                           data-base-href="${pageContext.request.contextPath}/admin/reports/revenue/export?mode=${e:esc(mode)}&amp;from=${e:esc(from)}&amp;to=${e:esc(to)}">
                            <i class="bi bi-download" aria-hidden="true"></i> Export CSV
                        </a>
                    </div>
                </div>
                <table class="ss-rev__detail" id="revenueDetailsTable">
                    <thead>
                        <tr>
                            <th scope="col">Period</th>
                            <th scope="col" class="ss-rev__num" style="width: 120px;">Bookings</th>
                            <th scope="col" class="ss-rev__num" style="width: 160px;">Revenue</th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:if test="${empty buckets}">
                            <tr><td colspan="3" class="ss-rev__empty">No revenue in this range.</td></tr>
                        </t:if>
                        <t:forEach items="${buckets}" var="b">
                            <tr class="revenue-row ${b.revenue == 0 ? 'zero-revenue-row ss-rev__row--zero' : ''}">
                                <td>${e:esc(b.label)}</td>
                                <td class="ss-rev__num">${b.bookingCount}</td>
                                <td class="ss-rev__num">${e:money(b.revenue)}</td>
                            </tr>
                        </t:forEach>
                    </tbody>
                    <tfoot>
                        <tr>
                            <th scope="row">Total</th>
                            <td class="ss-rev__num">${totalBookings}</td>
                            <td class="ss-rev__num">${e:money(totalRevenue)}</td>
                        </tr>
                    </tfoot>
                </table>
            </section>
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
            var css = getComputedStyle(document.documentElement);
            var barColor = (css.getPropertyValue('--color-caramel') || '').trim() || '#9A5B2E';

            function compactVnd(val) {
                if (val >= 1000000) {
                    return (val / 1000000).toLocaleString('en-US', { maximumFractionDigits: 1 }) + 'M';
                } else if (val >= 1000) {
                    return (val / 1000).toLocaleString('en-US', { maximumFractionDigits: 0 }) + 'k';
                }
                return Number(val).toLocaleString('en-US');
            }

            function niceLabel(lbl) {
                if (lbl && lbl.length === 10 && lbl.indexOf('-') === 4) {
                    var p = lbl.split('-');
                    return p[2] + '/' + p[1] + '/' + p[0];
                }
                if (lbl && lbl.length === 7 && lbl.indexOf('-') === 4) {
                    var q = lbl.split('-');
                    return q[1] + '/' + q[0];
                }
                return lbl;
            }

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
                                statusEl.innerHTML = '<p class="ss-rev__empty">No revenue in this range.</p>';
                            }
                            return {
                                type: 'bar',
                                data: { labels: (data && data.labels) ? data.labels : [], datasets: [] },
                                options: { responsive: true, maintainAspectRatio: false }
                            };
                        }

                        // dd/MM on the axis for days, MM/yyyy for months; auto-skip keeps it readable
                        var formattedLabels = data.labels.map(function(lbl) {
                            if (mode === 'day' && lbl && lbl.length === 10 && lbl.indexOf('-') === 4) {
                                var parts = lbl.split('-');
                                return parts[2] + '/' + parts[1];
                            }
                            return niceLabel(lbl);
                        });

                        return {
                            type: 'bar',
                            data: {
                                labels: formattedLabels,
                                datasets: [{
                                    label: 'Revenue (VND)',
                                    data: data.values,
                                    backgroundColor: barColor,
                                    borderRadius: 4
                                }]
                            },
                            options: {
                                responsive: true,
                                maintainAspectRatio: false,
                                plugins: {
                                    legend: { display: false },
                                    tooltip: {
                                        callbacks: {
                                            title: function(items) {
                                                if (items && items.length > 0) {
                                                    return niceLabel(data.labels[items[0].dataIndex]) || items[0].label;
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
                                        ticks: { autoSkip: true, maxRotation: 0, minRotation: 0, font: { size: 11 } }
                                    },
                                    y: {
                                        beginAtZero: true,
                                        grid: { color: 'rgba(74, 51, 38, 0.08)' },
                                        ticks: { font: { size: 11 }, callback: compactVnd }
                                    }
                                }
                            }
                        };
                    }
                });
            }

            var zeroSwitch = document.getElementById('hideZeroRevenueSwitch');
            var exportBtn = document.getElementById('revenueExportBtn');
            if (zeroSwitch) {
                zeroSwitch.addEventListener('change', function() {
                    document.querySelectorAll('.zero-revenue-row').forEach(function(row) {
                        row.style.display = zeroSwitch.checked ? 'none' : '';
                    });
                    // CSV follows the same rows as the table
                    if (exportBtn) {
                        exportBtn.href = exportBtn.getAttribute('data-base-href') + (zeroSwitch.checked ? '&hideZero=1' : '');
                    }
                });
            }

            var chartTabBtn = document.getElementById('tab-chart-btn');
            if (chartTabBtn) {
                chartTabBtn.addEventListener('shown.bs.tab', function() {
                    window.dispatchEvent(new Event('resize'));
                });
            }
            // ss-tabs active style follows Bootstrap's tab state
            document.querySelectorAll('#revenueViewTabs [data-bs-toggle="tab"]').forEach(function(btn) {
                btn.addEventListener('shown.bs.tab', function() {
                    document.querySelectorAll('#revenueViewTabs .ss-tabs__link').forEach(function(b) {
                        b.classList.toggle('ss-tabs__link--active', b === btn);
                    });
                });
            });
            if (window.bootstrap && bootstrap.Tooltip) {
                document.querySelectorAll('.ss-rev__info[data-bs-toggle="tooltip"]').forEach(function(el) {
                    new bootstrap.Tooltip(el);
                });
            }
        })();
    </script>
</t:layout>
