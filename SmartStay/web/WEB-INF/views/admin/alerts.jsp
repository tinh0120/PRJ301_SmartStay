<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Security alerts" active="alerts">
    <%-- R10: alerts as an inbox. Titles come from util.AlertTitles; raw codes only in the detail drawer.
         Actions are plain POST forms (CSRF) that alerts.js upgrades to in-place fetch updates. --%>
    <div class="ss-page-header">
        <div>
            <h1 class="ss-page-header__title">Security alerts</h1>
            <p class="ss-page-header__subtitle">Most severe first. Acknowledge what you are looking at, resolve it when it is fixed.</p>
        </div>
    </div>

    <nav class="ss-tabs ss-bk-tabs" aria-label="Alert status" data-alert-tabs>
        <a class="ss-tabs__link ${tab == 'open' ? 'ss-tabs__link--active' : ''}" ${tab == 'open' ? 'aria-current="page"' : ''}
           href="${pageContext.request.contextPath}/admin/alerts?${tabBaseQs}&amp;tab=open">
            Open <span class="ss-bk-tabs__count" data-tab-count="NEW">${empty statusCounts ? 0 : statusCounts['NEW']}</span>
        </a>
        <a class="ss-tabs__link ${tab == 'ack' ? 'ss-tabs__link--active' : ''}" ${tab == 'ack' ? 'aria-current="page"' : ''}
           href="${pageContext.request.contextPath}/admin/alerts?${tabBaseQs}&amp;tab=ack">
            Acknowledged <span class="ss-bk-tabs__count" data-tab-count="ACKNOWLEDGED">${empty statusCounts ? 0 : statusCounts['ACKNOWLEDGED']}</span>
        </a>
        <a class="ss-tabs__link ${tab == 'resolved' ? 'ss-tabs__link--active' : ''}" ${tab == 'resolved' ? 'aria-current="page"' : ''}
           href="${pageContext.request.contextPath}/admin/alerts?${tabBaseQs}&amp;tab=resolved">
            Resolved <span class="ss-bk-tabs__count" data-tab-count="RESOLVED">${empty statusCounts ? 0 : statusCounts['RESOLVED']}</span>
        </a>
        <a class="ss-tabs__link ${tab == 'all' ? 'ss-tabs__link--active' : ''}" ${tab == 'all' ? 'aria-current="page"' : ''}
           href="${pageContext.request.contextPath}/admin/alerts?${tabBaseQs}&amp;tab=all">
            All <span class="ss-bk-tabs__count" data-tab-count="ALL">${empty statusCounts ? 0 : statusCounts['ALL']}</span>
        </a>
    </nav>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <!-- Toolbar -->
    <form action="${pageContext.request.contextPath}/admin/alerts" method="get" class="ss-toolbar">
        <input type="hidden" name="tab" value="${e:esc(tab)}">
        <t:if test="${not empty pr.getFilter('severity')}"><input type="hidden" name="severity" value="${e:esc(pr.getFilter('severity'))}"></t:if>
        <div class="ss-toolbar__main">
            <div class="ss-toolbar__search">
                <i class="bi bi-search ss-toolbar__search-icon" aria-hidden="true"></i>
                <label for="keyword" class="visually-hidden">Search alerts</label>
                <input type="search" class="ss-toolbar__search-input" id="keyword" name="keyword" maxlength="100"
                       placeholder="Message, device, room or booking code" value="${e:esc(pr.keyword)}">
            </div>
            <div class="ss-toolbar__chips" role="group" aria-label="Severity">
                <a class="ss-chip ${empty pr.getFilter('severity') ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/alerts?${chipBaseQs}">Any severity</a>
                <a class="ss-chip ${pr.getFilter('severity') == 'CRITICAL' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/alerts?${chipBaseQs}&amp;severity=CRITICAL">Critical</a>
                <a class="ss-chip ${pr.getFilter('severity') == 'HIGH' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/alerts?${chipBaseQs}&amp;severity=HIGH">High</a>
                <a class="ss-chip ${pr.getFilter('severity') == 'MEDIUM' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/alerts?${chipBaseQs}&amp;severity=MEDIUM">Medium</a>
                <a class="ss-chip ${pr.getFilter('severity') == 'LOW' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/alerts?${chipBaseQs}&amp;severity=LOW">Low</a>
            </div>
            <button class="ss-toolbar__more-toggle" type="button" data-bs-toggle="collapse" data-bs-target="#alertMoreFilters"
                    aria-expanded="${moreFiltersOpen ? 'true' : 'false'}" aria-controls="alertMoreFilters">
                <i class="bi bi-sliders" aria-hidden="true"></i>More filters
            </button>
        </div>
        <div class="collapse ${moreFiltersOpen ? 'show' : ''}" id="alertMoreFilters">
            <div class="ss-toolbar__collapse ss-bk-more">
                <div class="ss-bk-more__field">
                    <label for="type" class="ss-bk-more__label">Type</label>
                    <select class="form-select form-select-sm" id="type" name="type">
                        <option value="">Any type</option>
                        <t:forEach items="${types}" var="typ">
                            <option value="${e:esc(typ)}" ${typ == pr.getFilter('type') ? 'selected' : ''}>${e:esc(typeTitles[typ])}</option>
                        </t:forEach>
                    </select>
                </div>
                <div class="ss-bk-more__field">
                    <label for="from" class="ss-bk-more__label">From</label>
                    <input type="date" class="form-control form-control-sm" id="from" name="from" data-datepicker value="${e:esc(pr.getFilter('from'))}">
                    <t:if test="${not empty errors['from']}"><div class="invalid-feedback d-block">${e:esc(errors['from'])}</div></t:if>
                    <t:if test="${not empty errors['range']}"><div class="invalid-feedback d-block">${e:esc(errors['range'])}</div></t:if>
                </div>
                <div class="ss-bk-more__field">
                    <label for="to" class="ss-bk-more__label">To</label>
                    <input type="date" class="form-control form-control-sm" id="to" name="to" data-datepicker value="${e:esc(pr.getFilter('to'))}">
                    <t:if test="${not empty errors['to']}"><div class="invalid-feedback d-block">${e:esc(errors['to'])}</div></t:if>
                </div>
                <div class="ss-bk-more__actions">
                    <button type="submit" class="btn btn-primary btn-sm px-3">Apply</button>
                    <a href="${pageContext.request.contextPath}/admin/alerts?tab=${e:esc(tab)}" class="btn btn-outline-soft btn-sm px-3">Clear all</a>
                </div>
            </div>
        </div>
    </form>

    <!-- Inbox -->
    <div class="ss-inbox" id="alertInbox" data-tab="${e:esc(tab)}" data-page="${result.request.page}"
         data-can-handle="${canHandle ? 'true' : 'false'}">
        <ul class="ss-inbox__list" id="alertList" aria-label="Alerts">
            <t:forEach items="${result.items}" var="a">
                <li class="ss-inbox__item ss-inbox__item--${a.severityTone} ${a.status == 'RESOLVED' ? 'ss-inbox__item--done' : ''}"
                    tabindex="0" data-alert-row data-alert-id="${a.alertId}" data-status="${e:esc(a.status)}"
                    data-d-title="${e:esc(a.title)}"
                    data-d-type="${e:esc(a.alertType)}"
                    data-d-severity="${e:esc(a.severityTone)}"
                    data-d-status="${e:esc(a.status)}"
                    data-d-message="${e:esc(a.message)}"
                    data-d-device="${e:esc(empty a.deviceId ? '' : (empty a.deviceName ? a.deviceId : a.deviceName.concat(' (').concat(a.deviceId).concat(')')))}"
                    data-d-room="${e:esc(a.roomNumber)}"
                    data-d-booking="${e:esc(a.bookingCode)}"
                    data-d-created="${empty a.createdAt ? '' : e:datetime(a.createdAt)}"
                    data-d-ackby="${e:esc(a.acknowledgedByName)}"
                    data-d-ackat="${empty a.acknowledgedAt ? '' : e:datetime(a.acknowledgedAt)}"
                    data-d-resby="${e:esc(a.resolvedByName)}"
                    data-d-resat="${empty a.resolvedAt ? '' : e:datetime(a.resolvedAt)}"
                    data-d-note="${e:esc(a.resolutionNote)}"
                    data-d-snapshot="${a.snapshotAvailable ? pageContext.request.contextPath.concat('/').concat(e:esc(a.snapshotPath)) : ''}">
                    <div class="ss-inbox__body">
                        <div class="ss-inbox__head">
                            <span class="ss-inbox__title">${e:esc(a.title)}</span>
                            <span class="ss-inbox__sev ss-badge ss-badge--${a.severityTone}">${e:esc(a.severityTone)}</span>
                        </div>
                        <p class="ss-inbox__msg">${e:esc(a.message)}</p>
                        <div class="ss-inbox__meta">
                            <t:if test="${not empty a.source}"><span>${e:esc(a.source)}</span><span aria-hidden="true">&middot;</span></t:if>
                            <time title="${empty a.createdAt ? '' : e:datetime(a.createdAt)}">${e:timeAgo(a.createdAt)}</time>
                        </div>
                    </div>
                    <div class="ss-inbox__actions" data-alert-actions>
                        <t:if test="${a.status == 'NEW' && canHandle}">
                            <form method="post" action="${pageContext.request.contextPath}/admin/alerts/action" data-alert-form>
                                <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                <input type="hidden" name="alertId" value="${a.alertId}">
                                <input type="hidden" name="action" value="acknowledge">
                                <input type="hidden" name="returnUrl" value="${pageContext.request.contextPath}/admin/alerts?${e:esc(returnQs)}">
                                <button type="submit" class="btn btn-outline-soft btn-sm">Acknowledge</button>
                            </form>
                        </t:if>
                        <t:if test="${a.status == 'ACKNOWLEDGED' && canHandle}">
                            <details class="ss-inbox__resolve">
                                <summary class="btn btn-primary btn-sm">Resolve</summary>
                                <form method="post" action="${pageContext.request.contextPath}/admin/alerts/action" class="ss-inbox__pop" data-alert-form>
                                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                    <input type="hidden" name="alertId" value="${a.alertId}">
                                    <input type="hidden" name="action" value="resolve">
                                    <input type="hidden" name="returnUrl" value="${pageContext.request.contextPath}/admin/alerts?${e:esc(returnQs)}">
                                    <label for="note-${a.alertId}" class="ss-inbox__pop-label">Note (optional)</label>
                                    <textarea class="form-control form-control-sm" id="note-${a.alertId}" name="note" rows="2" maxlength="500"
                                              placeholder="What was done?"></textarea>
                                    <button type="submit" class="btn btn-primary btn-sm">Mark resolved</button>
                                </form>
                            </details>
                        </t:if>
                        <t:if test="${a.status == 'ACKNOWLEDGED' && !canHandle}">
                            <span class="ss-inbox__done">Acknowledged<t:if test="${not empty a.acknowledgedByName}"> by ${e:esc(a.acknowledgedByName)}</t:if></span>
                        </t:if>
                        <t:if test="${a.status == 'RESOLVED'}">
                            <span class="ss-inbox__done">Resolved<t:if test="${not empty a.resolvedByName}"> by ${e:esc(a.resolvedByName)}</t:if> &middot;
                                <time title="${empty a.resolvedAt ? '' : e:datetime(a.resolvedAt)}">${e:timeAgo(a.resolvedAt)}</time></span>
                        </t:if>
                    </div>
                </li>
            </t:forEach>
        </ul>

        <t:if test="${empty result.items && empty dbError}">
            <div class="ss-empty" id="alertEmpty">
                <i class="bi bi-shield-check ss-empty__icon" aria-hidden="true"></i>
                <p class="ss-empty__title">${tab == 'open' ? 'No open alerts' : (tab == 'ack' ? 'Nothing waiting to be resolved' : (tab == 'resolved' ? 'No resolved alerts yet' : 'No alerts found'))}</p>
                <p class="ss-empty__desc">${empty pr.keyword && empty pr.getFilter('severity') && empty pr.getFilter('type') ? 'All clear. New alerts appear here automatically.' : 'Nothing matches these filters.'}</p>
            </div>
        </t:if>
    </div>

    <t:pagination result="${result}" baseUrl="${pageContext.request.contextPath}/admin/alerts"/>

    <!-- Detail drawer (raw type code and the full timeline live here) -->
    <aside class="ss-drawer" id="alertDrawer" role="dialog" aria-modal="false" aria-labelledby="alertDrawerTitle" aria-hidden="true">
        <div class="ss-drawer__header">
            <h2 class="ss-drawer__title" id="alertDrawerTitle" tabindex="-1">Alert detail</h2>
            <button type="button" class="ss-drawer__close" data-alert-close aria-label="Close detail"><i class="bi bi-x-lg" aria-hidden="true"></i></button>
        </div>
        <div class="ss-drawer__body">
            <p class="ss-inbox__drawer-msg" id="alertDrawerMsg"></p>
            <figure class="ss-inbox__snap" id="alertDrawerSnap" hidden>
                <a id="alertDrawerSnapLink" href="#" target="_blank" rel="noopener">
                    <img id="alertDrawerSnapImg" src="" alt="Camera snapshot taken when the alert was raised" width="640" height="480" loading="lazy">
                </a>
                <figcaption class="ss-inbox__snap-caption">Main entrance camera</figcaption>
            </figure>
            <dl class="ss-act-dl" id="alertDrawerList"></dl>
            <h3 class="ss-inbox__tl-title">Timeline</h3>
            <ol class="ss-inbox__tl" id="alertDrawerTimeline"></ol>
        </div>
    </aside>

    <t:if test="${canHandle}">
        <%-- Resolve control used by alerts.js after an in-place Acknowledge (__ID__ = alert id) --%>
        <template id="alertResolveTpl">
            <details class="ss-inbox__resolve">
                <summary class="btn btn-primary btn-sm">Resolve</summary>
                <form method="post" action="${pageContext.request.contextPath}/admin/alerts/action" class="ss-inbox__pop" data-alert-form>
                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                    <input type="hidden" name="alertId" value="__ID__">
                    <input type="hidden" name="action" value="resolve">
                    <input type="hidden" name="returnUrl" value="${pageContext.request.contextPath}/admin/alerts?${e:esc(returnQs)}">
                    <label for="note-__ID__" class="ss-inbox__pop-label">Note (optional)</label>
                    <textarea class="form-control form-control-sm" id="note-__ID__" name="note" rows="2" maxlength="500"
                              placeholder="What was done?"></textarea>
                    <button type="submit" class="btn btn-primary btn-sm">Mark resolved</button>
                </form>
            </details>
        </template>
    </t:if>

    <script src="${pageContext.request.contextPath}/assets/js/alerts.js"></script>
</t:layout>
