<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Alert Thresholds" active="thresholds">
    <!-- Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Alert Thresholds</li>
        </ol>
    </nav>

    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h1 class="h2 mb-1">Alert thresholds</h1>
            <p class="text-muted small mb-0">Configure sensor metric rules and alert severities</p>
        </div>
        <div>
            <a href="${pageContext.request.contextPath}/admin/thresholds/edit" class="btn btn-primary btn-sm">
                + New threshold
            </a>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <div class="card mb-4 shadow-sm">
        <div class="card-body">
            <form action="${pageContext.request.contextPath}/admin/thresholds" method="get" class="row g-2 align-items-end">
                <input type="hidden" name="sort" value="${e:esc(pr.sort)}">
                <input type="hidden" name="dir" value="${e:esc(pr.dir)}">

                <div class="col-md-3">
                    <label for="keyword" class="form-label small fw-semibold">Search</label>
                    <input type="text" class="form-control form-control-sm" id="keyword" name="keyword"
                           placeholder="Alert type or description..." maxlength="100"
                           value="${e:esc(pr.keyword)}">
                </div>

                <div class="col-md-2">
                    <label for="metric" class="form-label small fw-semibold">Metric</label>
                    <select class="form-select form-select-sm ${not empty errors['metric'] ? 'is-invalid' : ''}" id="metric" name="metric">
                        <option value="">All metrics</option>
                        <option value="MOTION" ${pr.getFilter('metric') == 'MOTION' || pr.getFilter('metric') == 'PIR_TRIGGER' ? 'selected' : ''}>Motion (MOTION)</option>
                        <option value="DISTANCE_CM" ${pr.getFilter('metric') == 'DISTANCE_CM' ? 'selected' : ''}>Distance (cm)</option>
                        <option value="DOOR_OPEN_SECONDS" ${pr.getFilter('metric') == 'DOOR_OPEN_SECONDS' ? 'selected' : ''}>Door Open (sec)</option>
                    </select>
                    <t:if test="${not empty errors['metric']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['metric'])}</div>
                    </t:if>
                </div>

                <div class="col-md-3">
                    <label for="device" class="form-label small fw-semibold">Device scope</label>
                    <select class="form-select form-select-sm ${not empty errors['device'] ? 'is-invalid' : ''}" id="device" name="device">
                        <option value="">All scopes</option>
                        <option value="__GLOBAL__" ${pr.getFilter('device') == '__GLOBAL__' ? 'selected' : ''}>Global (All devices)</option>
                        <t:forEach items="${devices}" var="dev">
                            <option value="${dev.deviceId}" ${pr.getFilter('device') == dev.deviceId ? 'selected' : ''}>
                                ${e:esc(dev.deviceId)} - ${e:esc(dev.deviceName)}
                            </option>
                        </t:forEach>
                    </select>
                    <t:if test="${not empty errors['device']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['device'])}</div>
                    </t:if>
                </div>

                <div class="col-md-2">
                    <label for="enabled" class="form-label small fw-semibold">Status</label>
                    <select class="form-select form-select-sm ${not empty errors['enabled'] ? 'is-invalid' : ''}" id="enabled" name="enabled">
                        <option value="">All</option>
                        <option value="1" ${pr.getFilter('enabled') == '1' ? 'selected' : ''}>Enabled only</option>
                        <option value="0" ${pr.getFilter('enabled') == '0' ? 'selected' : ''}>Disabled only</option>
                    </select>
                    <t:if test="${not empty errors['enabled']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['enabled'])}</div>
                    </t:if>
                </div>

                <div class="col-md-2 d-flex gap-2">
                    <button type="submit" class="btn btn-primary btn-sm flex-grow-1">Filter</button>
                    <a href="${pageContext.request.contextPath}/admin/thresholds" class="btn btn-outline-secondary btn-sm">Reset</a>
                </div>
            </form>
        </div>
    </div>

    <div class="card shadow-sm mb-4">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover table-striped align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3">ID</th>
                            <th>Device Scope</th>
                            <th><t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/thresholds" column="metric" label="Metric"/></th>
                            <th>Condition</th>
                            <th><t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/thresholds" column="type" label="Alert Type"/></th>
                            <th><t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/thresholds" column="severity" label="Severity"/></th>
                            <th>Enabled</th>
                            <th>Description</th>
                            <th><t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/thresholds" column="updated" label="Updated"/></th>
                            <th class="pe-3 text-end">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:if test="${empty result.items}">
                            <tr>
                                <td colspan="10" class="text-center py-4 text-muted">
                                    No alert thresholds found matching your criteria.
                                </td>
                            </tr>
                        </t:if>
                        <t:forEach items="${result.items}" var="t">
                            <tr>
                                <td class="ps-3"><code class="fw-bold">#${t.thresholdId}</code></td>
                                <td>
                                    <t:if test="${empty t.deviceId}">
                                        <span class="badge bg-secondary">All devices</span>
                                    </t:if>
                                    <t:if test="${not empty t.deviceId}">
                                        <code>${e:esc(t.deviceId)}</code>
                                    </t:if>
                                </td>
                                <td>
                                    <span class="badge bg-light text-dark border">${e:esc(t.metricType)}</span>
                                </td>
                                <td>
                                    <code class="fw-bold text-dark">
                                        <t:if test="${t.operator == 'GT'}">&gt;</t:if>
                                        <t:if test="${t.operator == 'GTE' || t.operator == 'GE'}">&gt;=</t:if>
                                        <t:if test="${t.operator == 'LT'}">&lt;</t:if>
                                        <t:if test="${t.operator == 'LTE' || t.operator == 'LE'}">&lt;=</t:if>
                                        ${t.thresholdValue}
                                    </code>
                                </td>
                                <td>
                                    <span class="fw-semibold">${e:esc(t.alertType)}</span>
                                </td>
                                <td>
                                    <span class="badge-status severity-${e:esc(t.severity)}">${e:esc(t.severity)}</span>
                                </td>
                                <td>
                                    <t:if test="${t.enabled}">
                                        <span class="badge bg-success">Yes</span>
                                    </t:if>
                                    <t:if test="${not t.enabled}">
                                        <span class="badge bg-secondary">No</span>
                                    </t:if>
                                </td>
                                <td class="small text-muted text-truncate" style="max-width: 200px;">
                                    ${e:esc(t.description)}
                                </td>
                                <td class="small text-muted">
                                    ${e:datetime(t.updatedAt)}
                                </td>
                                <td class="pe-3 text-end">
                                    <div class="d-inline-flex gap-1 align-items-center">
                                        <a href="${pageContext.request.contextPath}/admin/thresholds/edit?thresholdId=${t.thresholdId}"
                                           class="btn btn-outline-primary btn-sm py-0 px-2" title="Edit threshold">
                                            Edit
                                        </a>

                                        <form action="${pageContext.request.contextPath}/admin/thresholds/delete" method="post" class="d-inline m-0">
                                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                            <input type="hidden" name="thresholdId" value="${t.thresholdId}">
                                            <button type="submit" class="btn btn-outline-danger btn-sm py-0 px-2"
                                                    data-confirm="Delete threshold #${t.thresholdId}?" title="Delete threshold">
                                                Delete
                                            </button>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                        </t:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <t:pagination result="${result}" baseUrl="${pageContext.request.contextPath}/admin/thresholds"/>
</t:layout>
