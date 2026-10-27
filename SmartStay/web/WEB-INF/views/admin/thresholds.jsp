<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Alert Thresholds" active="thresholds">
    <%-- V3: same anatomy as the other admin lists; Delete moves into the "⋯" menu (app.js data-confirm kept). --%>
    <div class="ss-page-header">
        <div>
            <h1 class="ss-page-header__title">Alert rules</h1>
            <p class="ss-page-header__subtitle">When a sensor reading crosses a rule, an alert is raised with that severity.</p>
        </div>
        <a href="${pageContext.request.contextPath}/admin/thresholds/edit" class="btn btn-primary btn-sm">New rule</a>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <form action="${pageContext.request.contextPath}/admin/thresholds" method="get" class="ss-toolbar">
        <input type="hidden" name="sort" value="${e:esc(pr.sort)}">
        <input type="hidden" name="dir" value="${e:esc(pr.dir)}">
        <t:if test="${not empty pr.getFilter('enabled')}"><input type="hidden" name="enabled" value="${e:esc(pr.getFilter('enabled'))}"></t:if>
        <div class="ss-toolbar__main">
            <div class="ss-toolbar__search">
                <i class="bi bi-search ss-toolbar__search-icon" aria-hidden="true"></i>
                <label for="keyword" class="visually-hidden">Search rules</label>
                <input type="search" class="ss-toolbar__search-input" id="keyword" name="keyword" maxlength="100"
                       placeholder="Alert type or description" value="${e:esc(pr.keyword)}">
            </div>
            <div class="ss-toolbar__chips" role="group" aria-label="Rule state">
                <a class="ss-chip ${empty pr.getFilter('enabled') ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/thresholds?keyword=${e:url(pr.keyword)}&amp;metric=${e:url(pr.getFilter('metric'))}&amp;device=${e:url(pr.getFilter('device'))}">All</a>
                <a class="ss-chip ${pr.getFilter('enabled') == '1' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/thresholds?keyword=${e:url(pr.keyword)}&amp;metric=${e:url(pr.getFilter('metric'))}&amp;device=${e:url(pr.getFilter('device'))}&amp;enabled=1">On</a>
                <a class="ss-chip ${pr.getFilter('enabled') == '0' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/thresholds?keyword=${e:url(pr.keyword)}&amp;metric=${e:url(pr.getFilter('metric'))}&amp;device=${e:url(pr.getFilter('device'))}&amp;enabled=0">Off</a>
            </div>
            <button class="ss-toolbar__more-toggle" type="button" data-bs-toggle="collapse" data-bs-target="#thrMoreFilters"
                    aria-expanded="${not empty pr.getFilter('metric') || not empty pr.getFilter('device') ? 'true' : 'false'}" aria-controls="thrMoreFilters">
                <i class="bi bi-sliders" aria-hidden="true"></i>More filters
            </button>
        </div>
        <div class="collapse ${not empty pr.getFilter('metric') || not empty pr.getFilter('device') || not empty errors ? 'show' : ''}" id="thrMoreFilters">
            <div class="ss-toolbar__collapse ss-bk-more">
                <div class="ss-bk-more__field">
                    <label for="metric" class="ss-bk-more__label">Sensor reading</label>
                    <select class="form-select form-select-sm ${not empty errors['metric'] ? 'is-invalid' : ''}" id="metric" name="metric">
                        <option value="">Any reading</option>
                        <option value="MOTION" ${pr.getFilter('metric') == 'MOTION' || pr.getFilter('metric') == 'PIR_TRIGGER' ? 'selected' : ''}>Motion</option>
                        <option value="DISTANCE_CM" ${pr.getFilter('metric') == 'DISTANCE_CM' ? 'selected' : ''}>Distance (cm)</option>
                        <option value="DOOR_OPEN_SECONDS" ${pr.getFilter('metric') == 'DOOR_OPEN_SECONDS' ? 'selected' : ''}>Door open (seconds)</option>
                    </select>
                    <t:if test="${not empty errors['metric']}"><div class="invalid-feedback d-block">${e:esc(errors['metric'])}</div></t:if>
                </div>
                <div class="ss-bk-more__field">
                    <label for="device" class="ss-bk-more__label">Device</label>
                    <select class="form-select form-select-sm ${not empty errors['device'] ? 'is-invalid' : ''}" id="device" name="device">
                        <option value="">Any device</option>
                        <option value="__GLOBAL__" ${pr.getFilter('device') == '__GLOBAL__' ? 'selected' : ''}>Rules for all devices</option>
                        <t:forEach items="${devices}" var="dev">
                            <option value="${dev.deviceId}" ${pr.getFilter('device') == dev.deviceId ? 'selected' : ''}>${e:esc(dev.deviceName)}</option>
                        </t:forEach>
                    </select>
                    <t:if test="${not empty errors['device']}"><div class="invalid-feedback d-block">${e:esc(errors['device'])}</div></t:if>
                </div>
                <t:if test="${not empty errors['enabled']}"><div class="invalid-feedback d-block">${e:esc(errors['enabled'])}</div></t:if>
                <div class="ss-bk-more__actions">
                    <button type="submit" class="btn btn-primary btn-sm px-3">Apply</button>
                    <a href="${pageContext.request.contextPath}/admin/thresholds" class="btn btn-outline-soft btn-sm px-3">Clear all</a>
                </div>
            </div>
        </div>
    </form>

    <div class="ss-dev-table">
        <t:if test="${empty result.items}">
            <div class="ss-empty">
                <i class="bi bi-sliders ss-empty__icon" aria-hidden="true"></i>
                <p class="ss-empty__title">No rules found</p>
                <p class="ss-empty__desc">Nothing matches these filters. Add a rule to raise alerts from sensor readings.</p>
            </div>
        </t:if>
        <t:if test="${not empty result.items}">
            <div class="table-responsive">
                <table class="table align-middle mb-0 ss-table">
                    <thead>
                        <tr>
                            <th scope="col" class="ps-3"><t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/thresholds" column="type" label="Alert"/></th>
                            <th scope="col"><t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/thresholds" column="metric" label="Rule"/></th>
                            <th scope="col">Device</th>
                            <th scope="col"><t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/thresholds" column="severity" label="Severity"/></th>
                            <th scope="col">State</th>
                            <th scope="col"><t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/thresholds" column="updated" label="Updated"/></th>
                            <th scope="col" class="pe-3 text-end"><span class="visually-hidden">Actions</span></th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:forEach items="${result.items}" var="t">
                            <tr class="${t.enabled ? '' : 'ss-dev-row--off'}">
                                <td class="ps-3" title="${e:esc(t.description)}">
                                    <div class="ss-dev__name"><a class="ss-row-link" href="${pageContext.request.contextPath}/admin/thresholds/edit?thresholdId=${t.thresholdId}">${e:esc(e:alertTitle(t.alertType))}</a></div>
                                </td>
                                <td class="ss-num text-nowrap">
                                    ${t.metricType == 'DISTANCE_CM' ? 'Distance' : (t.metricType == 'DOOR_OPEN_SECONDS' ? 'Door open' : 'Motion')}
                                    <t:if test="${t.operator == 'GT'}">&gt;</t:if><t:if test="${t.operator == 'GTE' || t.operator == 'GE'}">&ge;</t:if><t:if test="${t.operator == 'LT'}">&lt;</t:if><t:if test="${t.operator == 'LTE' || t.operator == 'LE'}">&le;</t:if>
                                    ${t.thresholdValueFormatted}${t.metricType == 'DISTANCE_CM' ? ' cm' : (t.metricType == 'DOOR_OPEN_SECONDS' ? ' s' : '')}
                                </td>
                                <td>${empty t.deviceId ? 'All devices' : e:esc(t.deviceId)}</td>
                                <td><span class="ss-badge ss-badge--${e:esc(t.severity == null ? 'low' : t.severity.toLowerCase())}">${e:esc(e:humanStatus(t.severity))}</span></td>
                                <td>
                                    <span class="ss-dev__status">
                                        <span class="ss-status-dot ${t.enabled ? 'ss-status-dot--ok' : 'ss-status-dot--muted'}" aria-hidden="true"></span>
                                        ${t.enabled ? 'On' : 'Off'}
                                    </span>
                                </td>
                                <td class="ss-act__sub text-nowrap">${e:date(t.updatedAt)}</td>
                                <td class="pe-3 text-end text-nowrap">
                                    <div class="dropdown d-inline-block ss-row-menu">
                                        <button class="btn-trigger" type="button" data-bs-toggle="dropdown" aria-expanded="false"
                                                aria-label="More actions for the rule ${e:esc(e:alertTitle(t.alertType))}">
                                            <i class="bi bi-three-dots" aria-hidden="true"></i>
                                        </button>
                                        <ul class="dropdown-menu dropdown-menu-end">
                                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/thresholds/edit?thresholdId=${t.thresholdId}">Edit</a></li>
                                            <li><hr class="dropdown-divider"></li>
                                            <li>
                                                <form action="${pageContext.request.contextPath}/admin/thresholds/delete" method="post" class="m-0">
                                                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                                    <input type="hidden" name="thresholdId" value="${t.thresholdId}">
                                                    <button type="submit" class="dropdown-item dropdown-item--destructive"
                                                            data-confirm="Delete this alert rule? Alerts already raised stay in the inbox.">Delete rule</button>
                                                </form>
                                            </li>
                                        </ul>
                                    </div>
                                </td>
                            </tr>
                        </t:forEach>
                    </tbody>
                </table>
            </div>
        </t:if>
    </div>

    <t:pagination result="${result}" baseUrl="${pageContext.request.contextPath}/admin/thresholds"/>
</t:layout>
