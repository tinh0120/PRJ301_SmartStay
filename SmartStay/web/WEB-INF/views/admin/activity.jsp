<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Activity logs" active="activity">
    <%-- R9: Door access + Motion in one page. Rows are grouped by day (sticky headers); technical IDs
         (credential, request, event) only appear in the detail drawer opened by clicking a row. --%>
    <div class="ss-page-header">
        <div>
            <h1 class="ss-page-header__title">Activity logs</h1>
            <p class="ss-page-header__subtitle">Door unlocks and motion detected by the lobby sensors.</p>
        </div>
    </div>

    <nav class="ss-tabs ss-act-tabs" aria-label="Log type">
        <t:if test="${canDoor}">
            <a class="ss-tabs__link ${tab == 'door' ? 'ss-tabs__link--active' : ''}" ${tab == 'door' ? 'aria-current="page"' : ''}
               href="${pageContext.request.contextPath}/admin/activity?tab=door">Door access</a>
        </t:if>
        <t:if test="${canMotion}">
            <a class="ss-tabs__link ${tab == 'motion' ? 'ss-tabs__link--active' : ''}" ${tab == 'motion' ? 'aria-current="page"' : ''}
               href="${pageContext.request.contextPath}/admin/activity?tab=motion">Motion</a>
        </t:if>
    </nav>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <!-- Toolbar -->
    <form action="${pageContext.request.contextPath}/admin/activity" method="get" class="ss-toolbar">
        <input type="hidden" name="tab" value="${e:esc(tab)}">
        <t:if test="${tab == 'door' && deniedOnly}"><input type="hidden" name="result" value="DENIED"></t:if>
        <t:if test="${tab == 'motion' && closeOnly}"><input type="hidden" name="close" value="1"></t:if>
        <div class="ss-toolbar__main">
            <div class="ss-toolbar__search">
                <i class="bi bi-search ss-toolbar__search-icon" aria-hidden="true"></i>
                <label for="keyword" class="visually-hidden">Search logs</label>
                <input type="search" class="ss-toolbar__search-input" id="keyword" name="keyword" maxlength="100"
                       placeholder="${tab == 'door' ? 'Guest, booking code, room or door' : 'Device name or ID'}" value="${e:esc(pr.keyword)}">
            </div>
            <div class="ss-toolbar__chips" role="group" aria-label="Quick filters">
                <t:if test="${tab == 'door'}">
                    <a class="ss-chip ${deniedOnly ? 'ss-chip--active' : ''}" ${deniedOnly ? 'aria-current="true"' : ''}
                       href="${pageContext.request.contextPath}/admin/activity?${chipBaseQs}${deniedOnly ? '' : '&result=DENIED'}">Denied only</a>
                </t:if>
                <t:if test="${tab == 'motion'}">
                    <a class="ss-chip ${closeOnly ? 'ss-chip--active' : ''}" ${closeOnly ? 'aria-current="true"' : ''}
                       href="${pageContext.request.contextPath}/admin/activity?${chipBaseQs}${closeOnly ? '' : '&close=1'}">Closer than 50 cm</a>
                </t:if>
            </div>
            <button class="ss-toolbar__more-toggle" type="button" data-bs-toggle="collapse" data-bs-target="#actMoreFilters"
                    aria-expanded="${moreFiltersOpen ? 'true' : 'false'}" aria-controls="actMoreFilters">
                <i class="bi bi-sliders" aria-hidden="true"></i>More filters
            </button>
        </div>
        <div class="collapse ${moreFiltersOpen ? 'show' : ''}" id="actMoreFilters">
            <div class="ss-toolbar__collapse ss-bk-more">
                <div class="ss-bk-more__field">
                    <label for="device" class="ss-bk-more__label">Device</label>
                    <select class="form-select form-select-sm" id="device" name="device">
                        <option value="">Any device</option>
                        <t:forEach items="${devices}" var="dv">
                            <option value="${e:esc(dv)}" ${pr.getFilter('device') == dv ? 'selected' : ''}>${e:esc(dv)}</option>
                        </t:forEach>
                    </select>
                </div>
                <t:if test="${tab == 'motion'}">
                    <div class="ss-bk-more__field">
                        <label for="zone" class="ss-bk-more__label">Zone</label>
                        <select class="form-select form-select-sm" id="zone" name="zone">
                            <option value="">Any zone</option>
                            <t:forEach items="${zones}" var="z">
                                <option value="${e:esc(z)}" ${pr.getFilter('zone') == z ? 'selected' : ''}>${e:esc(z == 'MAIN_ENTRANCE' ? 'Main entrance' : (z == 'LOBBY' ? 'Lobby' : z))}</option>
                            </t:forEach>
                        </select>
                    </div>
                </t:if>
                <div class="ss-bk-more__field">
                    <label for="from" class="ss-bk-more__label">From</label>
                    <input type="date" class="form-control form-control-sm" id="from" name="from" data-datepicker value="${e:esc(pr.getFilter('from'))}">
                    <t:if test="${not empty errors['from']}"><div class="invalid-feedback d-block">${e:esc(errors['from'])}</div></t:if>
                </div>
                <div class="ss-bk-more__field">
                    <label for="to" class="ss-bk-more__label">To</label>
                    <input type="date" class="form-control form-control-sm" id="to" name="to" data-datepicker value="${e:esc(pr.getFilter('to'))}">
                    <t:if test="${not empty errors['to']}"><div class="invalid-feedback d-block">${e:esc(errors['to'])}</div></t:if>
                </div>
                <div class="ss-bk-more__actions">
                    <button type="submit" class="btn btn-primary btn-sm px-3">Apply</button>
                    <a href="${pageContext.request.contextPath}/admin/activity?tab=${e:esc(tab)}" class="btn btn-outline-soft btn-sm px-3">Clear all</a>
                </div>
            </div>
        </div>
    </form>

    <div class="ss-act-table">
        <t:if test="${empty result.items && empty dbError}">
            <div class="ss-empty">
                <i class="bi bi-journal-text ss-empty__icon" aria-hidden="true"></i>
                <p class="ss-empty__title">No activity found</p>
                <p class="ss-empty__desc">Nothing matches these filters. Clear them or choose another date range.</p>
            </div>
        </t:if>

        <!-- Door access -->
        <t:if test="${tab == 'door' && not empty result.items}">
            <div class="table-responsive">
                <table class="table align-middle mb-0 ss-table ss-act" data-act-table="door">
                    <thead>
                        <tr>
                            <th scope="col" class="ps-3">Time</th>
                            <th scope="col">Guest &amp; room</th>
                            <th scope="col">Door</th>
                            <th scope="col">Result</th>
                            <th scope="col">Reason</th>
                        </tr>
                    </thead>
                    <t:forEach items="${dayGroups}" var="day">
                        <tbody>
                            <tr class="ss-act__day"><th scope="rowgroup" colspan="5" class="ps-3">${e:esc(day.label)}</th></tr>
                            <t:forEach items="${day.items}" var="a">
                                <tr class="ss-act__row ${a.denied ? 'ss-act__row--denied' : ''}" tabindex="0" data-act-row
                                    data-d-time="${e:esc(a.attemptedAtFormatted)}"
                                    data-d-result="${a.denied ? 'Denied' : 'Allowed'}"
                                    data-d-reason="${e:esc(a.denied ? a.reasonText : 'Allowed')}"
                                    data-d-guest="${e:esc(a.guestName)}"
                                    data-d-booking="${e:esc(a.bookingCode)}"
                                    data-d-room="${e:esc(a.roomNumber)}"
                                    data-d-door="${e:esc(not empty a.deviceName ? a.deviceName : a.deviceId)}"
                                    data-d-device="${e:esc(a.deviceId)}"
                                    data-d-credential="${a.credentialId}"
                                    data-d-request="${e:esc(a.requestId)}">
                                    <td class="ps-3 ss-act__time">${empty a.attemptedAtFormatted ? '' : e:esc(a.attemptedAtFormatted.substring(11))}</td>
                                    <td>
                                        <t:if test="${not empty a.guestName}">
                                            <%-- Item 3: one line per row; the booking code is in the detail drawer --%>
                                            <div class="ss-act__main" title="${e:esc(a.bookingCode)}">${e:esc(a.guestName)}<t:if test="${not empty a.roomNumber}"> &middot; Room ${e:esc(a.roomNumber)}</t:if></div>
                                        </t:if>
                                        <t:if test="${empty a.guestName}"><span class="ss-act__sub">Unknown PIN</span></t:if>
                                    </td>
                                    <td class="ss-act__sub">${e:esc(not empty a.deviceName ? a.deviceName : a.deviceId)}</td>
                                    <td>
                                        <t:if test="${a.denied}"><span class="ss-badge ss-badge--danger">Denied</span></t:if>
                                        <t:if test="${!a.denied}"><span class="ss-act__ok"><span class="ss-status-dot ss-status-dot--ok" aria-hidden="true"></span>Allowed</span></t:if>
                                    </td>
                                    <td>${e:esc(a.reasonText)}</td>
                                </tr>
                            </t:forEach>
                        </tbody>
                    </t:forEach>
                </table>
            </div>
        </t:if>

        <!-- Motion -->
        <t:if test="${tab == 'motion' && not empty result.items}">
            <div class="table-responsive">
                <table class="table align-middle mb-0 ss-table ss-act" data-act-table="motion">
                    <thead>
                        <tr>
                            <th scope="col" class="ps-3">Time</th>
                            <th scope="col">Source</th>
                            <th scope="col">Distance</th>
                            <th scope="col">Snapshot</th>
                        </tr>
                    </thead>
                    <t:forEach items="${dayGroups}" var="day">
                        <tbody>
                            <tr class="ss-act__day"><th scope="rowgroup" colspan="4" class="ps-3">${e:esc(day.label)}</th></tr>
                            <t:forEach items="${day.items}" var="m">
                                <tr class="ss-act__row" tabindex="0" data-act-row
                                    data-d-time="${e:esc(m.detectedAtFormatted)}"
                                    data-d-door="${e:esc(not empty m.deviceName ? m.deviceName : m.deviceId)}"
                                    data-d-device="${e:esc(m.deviceId)}"
                                    data-d-zone="${e:esc(m.zoneLabel)}"
                                    data-d-distance="${empty m.distanceCm ? '' : m.distanceCm}"
                                    data-d-event="${e:esc(m.eventId)}"
                                    data-d-received="${empty m.receivedAt ? '' : e:datetime(m.receivedAt)}">
                                    <td class="ps-3 ss-act__time">${empty m.detectedAtFormatted ? '' : e:esc(m.detectedAtFormatted.substring(11))}</td>
                                    <td title="${e:esc(m.zoneLabel)}">
                                        <span class="ss-act__main">${e:esc(not empty m.deviceName ? m.deviceName : m.deviceId)}</span>
                                    </td>
                                    <td>
                                        <t:if test="${not empty m.distanceRounded}">
                                            <div class="ss-act__dist ${m.close ? 'ss-act__dist--close' : ''}">
                                                <span class="ss-act__bar" aria-hidden="true"><span style="width: ${m.distancePercent}%;"></span></span>
                                                <span class="ss-act__dist-num">${m.distanceRounded} cm</span>
                                            </div>
                                        </t:if>
                                        <t:if test="${empty m.distanceRounded}"><span class="ss-act__sub">No reading</span></t:if>
                                    </td>
                                    <td>
                                        <t:if test="${m.snapshotAvailable}">
                                            <button type="button" class="ss-act__thumb" data-snap-src="${pageContext.request.contextPath}/${e:esc(m.snapshotPath)}"
                                                    aria-label="Open snapshot from ${e:esc(m.detectedAtFormatted)}">
                                                <img src="${pageContext.request.contextPath}/${e:esc(m.snapshotPath)}" alt="" width="1360" height="752" loading="lazy"
                                                     onerror="this.onerror=null; this.closest('.ss-act__thumb').classList.add('ss-act__thumb--broken');">
                                                <span class="ss-act__thumb-none">No snapshot</span>
                                            </button>
                                        </t:if>
                                    </td>
                                </tr>
                            </t:forEach>
                        </tbody>
                    </t:forEach>
                </table>
            </div>
        </t:if>
    </div>

    <t:pagination result="${result}" baseUrl="${pageContext.request.contextPath}/admin/activity"/>

    <!-- Detail drawer (technical IDs live here, not in the table) -->
    <aside class="ss-drawer ss-act-drawer" id="actDrawer" role="dialog" aria-modal="false" aria-labelledby="actDrawerTitle" aria-hidden="true">
        <div class="ss-drawer__header">
            <h2 class="ss-drawer__title" id="actDrawerTitle" tabindex="-1">${tab == 'door' ? 'Door access detail' : 'Motion detail'}</h2>
            <button type="button" class="ss-drawer__close" data-act-close aria-label="Close detail"><i class="bi bi-x-lg" aria-hidden="true"></i></button>
        </div>
        <div class="ss-drawer__body">
            <dl class="ss-act-dl" id="actDrawerList" data-tab="${e:esc(tab)}"></dl>
        </div>
    </aside>

    <!-- Snapshot lightbox -->
    <div class="modal fade ss-act-lightbox" id="actSnapModal" tabindex="-1" aria-label="Snapshot" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content">
                <button type="button" class="btn-close btn-close-white ss-act-lightbox__close" data-bs-dismiss="modal" aria-label="Close"></button>
                <img id="actSnapImg" src="" alt="Motion snapshot" class="ss-act-lightbox__img"
                     onerror="this.onerror=null; this.style.display='none';">
            </div>
        </div>
    </div>

    <script src="${pageContext.request.contextPath}/assets/js/activity.js"></script>
</t:layout>
