<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%
    javax.servlet.http.HttpSession currentSession = request.getSession(false);
    String roleName = (currentSession != null) ? (String) currentSession.getAttribute("roleName") : null;
    java.util.Set<?> perms = (currentSession != null && currentSession.getAttribute("permissions") instanceof java.util.Set)
            ? (java.util.Set<?>) currentSession.getAttribute("permissions") : null;
    boolean canHandleAlert = (perms != null && perms.contains("ALERT_HANDLE"))
            || "ADMIN".equalsIgnoreCase(roleName)
            || "OWNER".equalsIgnoreCase(roleName);
%>
<t:layout title="Security alerts" active="alerts">
    <!-- Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Security alerts</li>
        </ol>
    </nav>

    <div class="d-flex justify-content-between align-items-baseline mb-3">
        <div>
            <h1 class="h2 mb-1">Security alerts</h1>
            <p class="text-muted small mb-0">Hardware telemetry and security incident lifecycle management.</p>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <div class="card mb-4 shadow-sm">
        <div class="card-body">
            <form action="${pageContext.request.contextPath}/admin/alerts" method="get" class="row g-2 needs-validation" novalidate>
                <input type="hidden" name="sort" value="${e:esc(result.request.sort)}">
                <input type="hidden" name="dir" value="${e:esc(result.request.dir)}">

                <div class="col-md-2">
                    <label for="status" class="form-label small fw-semibold">Status</label>
                    <select class="form-select form-select-sm" id="status" name="status">
                        <option value="">All statuses</option>
                        <option value="NEW" ${'NEW' == result.request.getFilter('status') ? 'selected' : ''}>NEW</option>
                        <option value="ACKNOWLEDGED" ${'ACKNOWLEDGED' == result.request.getFilter('status') ? 'selected' : ''}>ACKNOWLEDGED</option>
                        <option value="RESOLVED" ${'RESOLVED' == result.request.getFilter('status') ? 'selected' : ''}>RESOLVED</option>
                    </select>
                </div>

                <div class="col-md-2">
                    <label for="severity" class="form-label small fw-semibold">Severity</label>
                    <select class="form-select form-select-sm" id="severity" name="severity">
                        <option value="">All severities</option>
                        <option value="LOW" ${'LOW' == result.request.getFilter('severity') ? 'selected' : ''}>LOW</option>
                        <option value="MEDIUM" ${'MEDIUM' == result.request.getFilter('severity') ? 'selected' : ''}>MEDIUM</option>
                        <option value="HIGH" ${'HIGH' == result.request.getFilter('severity') ? 'selected' : ''}>HIGH</option>
                        <option value="CRITICAL" ${'CRITICAL' == result.request.getFilter('severity') ? 'selected' : ''}>CRITICAL</option>
                    </select>
                </div>

                <div class="col-md-2">
                    <label for="type" class="form-label small fw-semibold">Alert type</label>
                    <select class="form-select form-select-sm" id="type" name="type">
                        <option value="">All types</option>
                        <t:forEach items="${types}" var="typ">
                            <option value="${e:esc(typ)}" ${typ == result.request.getFilter('type') ? 'selected' : ''}>
                                ${e:esc(typ)}
                            </option>
                        </t:forEach>
                    </select>
                </div>

                <div class="col-md-2">
                    <label for="from" class="form-label small fw-semibold">From date</label>
                    <input type="date" class="form-control form-control-sm ${errors['from'] != null ? 'is-invalid' : ''}"
                           id="from" name="from" data-datepicker value="${e:esc(result.request.getFilter('from'))}">
                    <t:if test="${not empty errors['from']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['from'])}</div>
                    </t:if>
                </div>

                <div class="col-md-2">
                    <label for="to" class="form-label small fw-semibold">To date</label>
                    <input type="date" class="form-control form-control-sm ${errors['to'] != null ? 'is-invalid' : ''}"
                           id="to" name="to" data-datepicker value="${e:esc(result.request.getFilter('to'))}">
                    <t:if test="${not empty errors['to']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['to'])}</div>
                    </t:if>
                </div>

                <div class="col-md-2">
                    <label for="keyword" class="form-label small fw-semibold">Message</label>
                    <input type="text" class="form-control form-control-sm" id="keyword" name="keyword"
                           placeholder="Search message..." value="${e:esc(result.request.keyword)}">
                </div>

                <div class="col-12 d-flex justify-content-end gap-2 mt-3">
                    <button type="submit" class="btn btn-primary btn-sm px-3">Filter</button>
                    <a href="${pageContext.request.contextPath}/admin/alerts" class="btn btn-outline-secondary btn-sm px-3">Reset</a>
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
                            <th scope="col" class="ps-3 text-nowrap" style="min-width: 155px;">
                                <t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/alerts" column="time" label="Time"/>
                            </th>
                            <th scope="col" style="width: 160px;">
                                <t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/alerts" column="type" label="Device & Type"/>
                            </th>
                            <th scope="col" style="width: 100px;">
                                <t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/alerts" column="severity" label="Severity"/>
                            </th>
                            <th scope="col" style="width: 110px;">
                                <t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/alerts" column="status" label="Status"/>
                            </th>
                            <th scope="col">Message</th>
                            <th scope="col" style="width: 180px;">Lifecycle</th>
                            <th scope="col" class="pe-3 text-end" style="width: 110px;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:if test="${empty result.items}">
                            <tr><td colspan="7" class="text-center py-4 text-muted">No records found</td></tr>
                        </t:if>
                        <t:forEach items="${result.items}" var="row">
                            <tr>
                                <td class="ps-3 font-monospace small text-nowrap" style="white-space: nowrap;">${e:datetime(row.createdAt)}</td>
                                <td>
                                    <div class="font-monospace fw-semibold small text-espresso">${e:esc(row.alertType)}</div>
                                    <div class="small text-muted font-monospace" style="font-size: 0.72rem;">
                                        <i class="bi bi-cpu me-1"></i>${row.deviceId != null ? e:esc(row.deviceId) : '&mdash;'}
                                    </div>
                                </td>
                                <td>
                                    <span class="badge badge-sev-${row.severity == 'CRITICAL' ? 'critical' : (row.severity == 'HIGH' ? 'high' : (row.severity == 'MEDIUM' ? 'medium' : 'low'))}">${e:esc(row.severity)}</span>
                                </td>
                                <td>
                                    <span class="badge ${row.status == 'NEW' ? 'badge-status-danger' : (row.status == 'ACKNOWLEDGED' ? 'badge-status-warning' : 'badge-status-success')}">${e:esc(row.status)}</span>
                                </td>
                                <td>
                                    <div class="text-truncate-2 small text-dark" title="${e:esc(row.message)}" data-bs-toggle="tooltip">
                                        ${e:esc(row.message)}
                                    </div>
                                </td>
                                <td class="small">
                                    <t:if test="${row.acknowledgedAt != null}">
                                        <div class="text-muted text-truncate" title="Ack by ${e:esc(row.acknowledgedByName)} at ${e:esc(row.acknowledgedAtFormatted)}">
                                            <i class="bi bi-eye me-1"></i>Ack: <span class="fw-semibold">${e:esc(row.acknowledgedByName)}</span>
                                        </div>
                                    </t:if>
                                    <t:if test="${row.resolvedAt != null}">
                                        <div class="text-success text-truncate" title="Res by ${e:esc(row.resolvedByName)}: ${e:esc(row.resolutionNote)}">
                                            <i class="bi bi-check-circle me-1"></i>Res: <span class="fw-semibold">${e:esc(row.resolvedByName)}</span>
                                        </div>
                                    </t:if>
                                    <t:if test="${row.acknowledgedAt == null && row.resolvedAt == null}">
                                        <span class="text-muted">&mdash;</span>
                                    </t:if>
                                </td>
                                <td class="pe-3 text-end text-nowrap">
                                    <div class="d-inline-flex align-items-center gap-1 justify-content-end">
                                        <t:if test="${row.status == 'NEW' && canHandleAlert}">
                                            <form action="${pageContext.request.contextPath}/admin/alerts/action" method="post" class="d-inline m-0">
                                                <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                                <input type="hidden" name="action" value="acknowledge">
                                                <input type="hidden" name="alertId" value="${row.alertId}">
                                                <button type="submit" class="btn btn-warning btn-sm py-1 px-2 fw-semibold" style="font-size: 0.78rem;" title="Acknowledge alert">
                                                    <i class="bi bi-eye me-1"></i>Acknowledge
                                                </button>
                                            </form>
                                        </t:if>

                                        <t:if test="${row.status == 'ACKNOWLEDGED' && canHandleAlert}">
                                            <button type="button" class="btn btn-success btn-sm py-1 px-2 fw-semibold" style="font-size: 0.78rem;" data-bs-toggle="modal" data-bs-target="#resolveModal${row.alertId}">
                                                <i class="bi bi-check2-circle me-1"></i>Resolve
                                            </button>
                                            <div class="modal fade text-start" id="resolveModal${row.alertId}" tabindex="-1" aria-labelledby="resolveModalLabel${row.alertId}" aria-hidden="true">
                                                <div class="modal-dialog modal-dialog-centered">
                                                    <div class="modal-content">
                                                        <form action="${pageContext.request.contextPath}/admin/alerts/action" method="post">
                                                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                                            <input type="hidden" name="action" value="resolve">
                                                            <input type="hidden" name="alertId" value="${row.alertId}">
                                                            <div class="modal-header">
                                                                <h5 class="modal-title h6 mb-0" id="resolveModalLabel${row.alertId}">Resolve Alert #${row.alertId}</h5>
                                                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                            </div>
                                                            <div class="modal-body">
                                                                <div class="mb-3">
                                                                    <span class="badge badge-sev-${row.severity == 'CRITICAL' ? 'critical' : (row.severity == 'HIGH' ? 'high' : (row.severity == 'MEDIUM' ? 'medium' : 'low'))} mb-1">${e:esc(row.severity)}</span>
                                                                    <span class="font-monospace fw-bold text-espresso small ms-1">${e:esc(row.alertType)}</span>
                                                                    <p class="small text-muted mt-1 mb-0">${e:esc(row.message)}</p>
                                                                </div>
                                                                <label for="note${row.alertId}" class="form-label small fw-semibold">Resolution Note <span class="text-muted fw-normal">(optional)</span></label>
                                                                <textarea class="form-control form-control-sm" id="note${row.alertId}" name="note" rows="3" maxlength="500" placeholder="Describe corrective action taken (optional)..."></textarea>
                                                            </div>
                                                            <div class="modal-footer">
                                                                <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                                                                <button type="submit" class="btn btn-success btn-sm">Confirm Resolve</button>
                                                            </div>
                                                        </form>
                                                    </div>
                                                </div>
                                            </div>
                                        </t:if>

                                        <t:if test="${row.status == 'RESOLVED'}">
                                            <span class="badge badge-status-success">Closed</span>
                                        </t:if>

                                        <!-- Clear text link Details (replaces View only) -->
                                        <a href="#detailsModal${row.alertId}" data-bs-toggle="modal" class="small text-decoration-none fw-semibold ms-1">Details</a>

                                        <!-- Details Modal -->
                                        <div class="modal fade text-start" id="detailsModal${row.alertId}" tabindex="-1" aria-labelledby="detailsModalLabel${row.alertId}" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content">
                                                    <div class="modal-header">
                                                        <h5 class="modal-title h6 mb-0" id="detailsModalLabel${row.alertId}">
                                                            <i class="bi bi-shield-exclamation me-1" style="color: var(--st-terracotta, #9A5B2E);"></i>Alert #${row.alertId} Details
                                                        </h5>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                    </div>
                                                    <div class="modal-body">
                                                        <div class="d-flex align-items-center gap-2 mb-3">
                                                            <span class="badge badge-sev-${row.severity == 'CRITICAL' ? 'critical' : (row.severity == 'HIGH' ? 'high' : (row.severity == 'MEDIUM' ? 'medium' : 'low'))}">${e:esc(row.severity)}</span>
                                                            <span class="badge ${row.status == 'NEW' ? 'badge-status-danger' : (row.status == 'ACKNOWLEDGED' ? 'badge-status-warning' : 'badge-status-success')}">${e:esc(row.status)}</span>
                                                            <span class="font-monospace fw-bold small text-espresso">${e:esc(row.alertType)}</span>
                                                        </div>
                                                        <div class="p-3 bg-light rounded-3 mb-3 small">
                                                            <span class="fw-semibold text-muted text-uppercase" style="font-size: 0.72rem;">Incident Description</span>
                                                            <div class="mt-1 fw-medium text-dark">${e:esc(row.message)}</div>
                                                        </div>
                                                        <table class="table table-sm table-borderless small mb-0">
                                                            <tr>
                                                                <td class="text-muted" style="width: 140px;">Timestamp:</td>
                                                                <td class="font-monospace text-nowrap" style="white-space: nowrap;">${e:datetime(row.createdAt)}</td>
                                                            </tr>
                                                            <tr>
                                                                <td class="text-muted">Device ID:</td>
                                                                <td class="font-monospace">${row.deviceId != null ? e:esc(row.deviceId) : '&mdash;'}</td>
                                                            </tr>
                                                            <tr>
                                                                <td class="text-muted">Room / Zone:</td>
                                                                <td><t:if test="${row.roomId != null}">Room #${row.roomId}</t:if><t:if test="${row.roomId == null && row.zoneId != null}">${e:esc(row.zoneId)}</t:if><t:if test="${row.roomId == null && row.zoneId == null}">&mdash;</t:if></td>
                                                            </tr>
                                                            <tr>
                                                                <td class="text-muted">Booking:</td>
                                                                <td><t:if test="${row.bookingId != null}">Booking #${row.bookingId}</t:if><t:if test="${row.bookingId == null}">&mdash;</t:if></td>
                                                            </tr>
                                                            <t:if test="${not empty row.acknowledgedByName}">
                                                                <tr>
                                                                    <td class="text-muted">Acknowledged by:</td>
                                                                    <td>${e:esc(row.acknowledgedByName)} <span class="text-muted font-monospace">(${e:datetime(row.acknowledgedAt)})</span></td>
                                                                </tr>
                                                            </t:if>
                                                            <t:if test="${not empty row.resolvedByName}">
                                                                <tr>
                                                                    <td class="text-muted">Resolved by:</td>
                                                                    <td>${e:esc(row.resolvedByName)} <span class="text-muted font-monospace">(${e:datetime(row.resolvedAt)})</span></td>
                                                                </tr>
                                                                <tr>
                                                                    <td class="text-muted">Resolution Note:</td>
                                                                    <td class="fst-italic text-espresso">${e:esc(row.resolutionNote)}</td>
                                                                </tr>
                                                            </t:if>
                                                        </table>
                                                    </div>
                                                    <div class="modal-footer">
                                                        <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Close</button>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </td>
                            </tr>
                        </t:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <t:pagination result="${result}" baseUrl="${pageContext.request.contextPath}/admin/alerts"/>
</t:layout>
