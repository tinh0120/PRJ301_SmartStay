<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Dashboard" active="dashboard">
    <%-- R6: "what do I need to do today?" Normal states stay quiet, problems are coloured.
         #dashLive is re-rendered in place by dashboard-live.js on realtime events. --%>
    <div class="ss-page-header ss-dash__header">
        <div>
            <h1 class="ss-page-header__title">Dashboard</h1>
            <p class="ss-page-header__subtitle">${e:esc(todayLabel)}</p>
        </div>
        <a href="${pageContext.request.contextPath}/admin/bookings/edit" class="btn btn-primary btn-sm rounded-pill px-3">
            <i class="bi bi-plus-lg me-1" aria-hidden="true"></i>New booking
        </a>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <div id="dashLive" class="ss-dash">

        <!-- Row 1: four KPIs, each opens its page -->
        <div class="ss-dash__kpis">
            <a class="ss-kpi" href="${pageContext.request.contextPath}/admin/bookings?chip=checkins_today">
                <span class="ss-kpi__label">Check-ins today</span>
                <span class="ss-kpi__value">${checkinsDone}<span class="ss-kpi__of"> of ${checkinsExpected} expected</span></span>
            </a>
            <a class="ss-kpi" href="${pageContext.request.contextPath}/admin/bookings?chip=checkouts_today">
                <span class="ss-kpi__label">Check-outs today</span>
                <span class="ss-kpi__value">${checkoutsDone}<span class="ss-kpi__of"> of ${checkoutsExpected} expected</span></span>
            </a>
            <a class="ss-kpi ${roomsToClean > 0 ? 'ss-kpi--warn' : ''}" href="${pageContext.request.contextPath}/admin/rooms?status=NEEDS_CLEANING">
                <span class="ss-kpi__label">Rooms to clean</span>
                <span class="ss-kpi__value">${roomsToClean}</span>
            </a>
            <a class="ss-kpi ${newAlerts['CRITICAL'] > 0 ? 'ss-kpi--danger' : (newAlerts['HIGH'] > 0 ? 'ss-kpi--warn' : '')}"
               href="${pageContext.request.contextPath}/admin/alerts?status=NEW">
                <span class="ss-kpi__label">Open alerts</span>
                <span class="ss-kpi__value">${newAlerts['TOTAL']}</span>
                <t:if test="${newAlerts['CRITICAL'] > 0 || newAlerts['HIGH'] > 0}">
                    <span class="ss-kpi__split">${newAlerts['CRITICAL']} critical, ${newAlerts['HIGH']} high</span>
                </t:if>
            </a>
        </div>

        <!-- Row 2: needs attention (wide) + rooms -->
        <div class="ss-dash__main">
            <section class="ss-dash__card ss-dash__attention" aria-labelledby="dashAttentionTitle">
                <div class="ss-dash__card-head">
                    <h2 class="ss-dash__card-title" id="dashAttentionTitle">Needs attention</h2>
                    <t:if test="${attentionTotal > attentionItems.size()}">
                        <span class="ss-dash__card-meta">Most urgent ${attentionItems.size()} of ${attentionTotal}</span>
                    </t:if>
                </div>
                <t:if test="${empty attentionItems}">
                    <div class="ss-empty ss-dash__clear">
                        <i class="bi bi-check2-circle ss-empty__icon" aria-hidden="true"></i>
                        <p class="ss-empty__title">All clear</p>
                        <p class="ss-empty__desc">Nothing needs your attention right now.</p>
                    </div>
                </t:if>
                <t:if test="${not empty attentionItems}">
                    <ul class="ss-attn">
                        <t:forEach items="${attentionItems}" var="it">
                            <li class="ss-attn__item ss-attn__item--${it.tone}">
                                <div class="ss-attn__text">
                                    <p class="ss-attn__title">${e:esc(it.title)}</p>
                                    <p class="ss-attn__detail">${e:esc(it.detail)}</p>
                                </div>
                                <a href="${pageContext.request.contextPath}${e:esc(it.actionPath)}" class="btn btn-sm btn-outline-soft rounded-pill px-3 ss-attn__action">${e:esc(it.actionLabel)}</a>
                            </li>
                        </t:forEach>
                    </ul>
                </t:if>
            </section>

            <section class="ss-dash__card ss-dash__rooms" aria-labelledby="dashRoomsTitle">
                <div class="ss-dash__card-head">
                    <h2 class="ss-dash__card-title" id="dashRoomsTitle">Rooms</h2>
                    <a href="${pageContext.request.contextPath}/admin/rooms" class="ss-dash__card-link">All rooms</a>
                </div>
                <div class="ss-room-tiles">
                    <t:forEach items="${roomTiles}" var="rt">
                        <a href="${pageContext.request.contextPath}/admin/rooms/slots?roomId=${rt.roomId}"
                           class="ss-room-tile ${rt.problem ? 'ss-room-tile--problem ss-room-tile--'.concat(rt.tone) : ''}"
                           aria-label="Room ${e:esc(rt.roomNumber)}, ${e:esc(rt.statusLabel)}">
                            <span class="ss-room-tile__code">${e:esc(rt.roomNumber)}</span>
                            <span class="ss-room-tile__name">${e:esc(rt.displayName)}</span>
                            <span class="ss-room-tile__status">
                                <span class="ss-status-dot ss-status-dot--${rt.tone}" aria-hidden="true"></span>${e:esc(rt.statusLabel)}
                            </span>
                            <t:if test="${not empty rt.nextEvent}">
                                <span class="ss-room-tile__next">${e:esc(rt.nextEvent)}</span>
                            </t:if>
                        </a>
                    </t:forEach>
                </div>
            </section>
        </div>

        <!-- Row 3: compact strips -->
        <div class="ss-dash__strips">
            <p class="ss-dash__strip ${deviceCounts['FAULT'] > 0 || deviceCounts['OFFLINE'] > 0 ? 'ss-dash__strip--warn' : ''}">
                <i class="bi bi-cpu ss-dash__strip-icon" aria-hidden="true"></i>
                <span class="ss-dash__strip-label">Devices</span>
                <span>${deviceCounts['ONLINE']} online</span>
                <span aria-hidden="true">&middot;</span>
                <span>${deviceCounts['OFFLINE']} offline</span>
                <span aria-hidden="true">&middot;</span>
                <span>${deviceCounts['FAULT']} fault</span>
                <a href="${pageContext.request.contextPath}/admin/devices" class="ss-dash__strip-link">Manage</a>
            </p>
            <p class="ss-dash__strip">
                <i class="bi bi-cash-stack ss-dash__strip-icon" aria-hidden="true"></i>
                <span class="ss-dash__strip-label">Revenue this month</span>
                <span class="ss-dash__strip-num">${e:money(revenueMonth)}</span>
                <t:if test="${revenueChangePct != null}">
                    <span class="ss-dash__change ${revenueChangePct >= 0 ? 'ss-dash__change--up' : 'ss-dash__change--down'}"
                          title="Compared with the same days of last month">${revenueChangePct >= 0 ? '+' : ''}${revenueChangePct}%</span>
                </t:if>
                <a href="${pageContext.request.contextPath}/admin/reports/revenue" class="ss-dash__strip-link">Report</a>
            </p>
        </div>
    </div>

    <!-- Trends (below the fold): the existing revenue chart, hooks unchanged -->
    <section class="ss-dash__card ss-dash__trends" aria-labelledby="dashTrendsTitle">
        <div class="ss-dash__card-head">
            <h2 class="ss-dash__card-title" id="dashTrendsTitle">Revenue, last 7 days</h2>
            <a href="${pageContext.request.contextPath}/admin/reports/revenue" class="ss-dash__card-link">Full report</a>
        </div>
        <div id="revenue7Status"></div>
        <div style="height: 280px; position: relative;">
            <canvas id="revenue7Chart"></canvas>
        </div>
    </section>

    <!-- Chart.js local vendor and loader scripts -->
    <script src="${pageContext.request.contextPath}/assets/vendor/chartjs/chart.umd.min.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/chart-loader.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/dashboard-live.js"></script>
    <script>
        (function() {
            var chartsUrl = '${pageContext.request.contextPath}/admin/dashboard/charts';
            if (window.SmartStayCharts) {
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
                                statusEl.innerHTML = '<div class="alert alert-info py-2 px-3 small">No revenue in the last 7 days.</div>';
                            }
                            return {
                                type: 'bar',
                                data: { labels: [], datasets: [] },
                                options: { responsive: true, maintainAspectRatio: false }
                            };
                        }
                        var labels = rData.labels.map(function(l) {
                            var p = String(l).split('-');
                            return p.length === 3 ? p[2] + '/' + p[1] : l;
                        });
                        return {
                            type: 'bar',
                            data: {
                                labels: labels,
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
                                plugins: { legend: { display: false } },
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
            }
        })();
    </script>
</t:layout>
