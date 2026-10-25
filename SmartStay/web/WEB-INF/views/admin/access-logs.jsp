<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Access logs" active="access-logs">
    <!-- Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Access logs</li>
        </ol>
    </nav>

    <div class="d-flex justify-content-between align-items-baseline mb-3">
        <div>
            <h1 class="h2 mb-1">Access logs</h1>
            <p class="text-muted small mb-0">Physical door unlock verification attempts and security history.</p>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <div class="card mb-4 shadow-sm">
        <div class="card-body">
            <form action="${pageContext.request.contextPath}/admin/access-logs" method="get" class="row g-2 needs-validation" novalidate>
                <input type="hidden" name="sort" value="${e:esc(result.request.sort)}">
                <input type="hidden" name="dir" value="${e:esc(result.request.dir)}">

                <div class="col-md-2">
                    <label for="device" class="form-label small fw-semibold">Device</label>
                    <select class="form-select form-select-sm" id="device" name="device">
                        <option value="">All devices</option>
                        <t:forEach items="${devices}" var="dev">
                            <option value="${e:esc(dev)}" ${dev == result.request.getFilter('device') ? 'selected' : ''}>
                                ${e:esc(dev)}
                            </option>
                        </t:forEach>
                    </select>
                </div>

                <div class="col-md-2">
                    <label for="result" class="form-label small fw-semibold">Result</label>
                    <select class="form-select form-select-sm" id="result" name="result">
                        <option value="">All results</option>
                        <option value="ALLOWED" ${'ALLOWED' == result.request.getFilter('result') ? 'selected' : ''}>ALLOWED</option>
                        <option value="DENIED" ${'DENIED' == result.request.getFilter('result') ? 'selected' : ''}>DENIED</option>
                    </select>
                </div>

                <div class="col-md-2">
                    <label for="reason" class="form-label small fw-semibold">Reason code</label>
                    <select class="form-select form-select-sm" id="reason" name="reason">
                        <option value="">All reasons</option>
                        <t:forEach items="${reasons}" var="rsn">
                            <option value="${e:esc(rsn)}" ${rsn == result.request.getFilter('reason') ? 'selected' : ''}>
                                ${e:esc(rsn)}
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
                    <label for="keyword" class="form-label small fw-semibold">Request ID</label>
                    <input type="text" class="form-control form-control-sm" id="keyword" name="keyword"
                           placeholder="REQ-..." value="${e:esc(result.request.keyword)}">
                </div>

                <div class="col-12 d-flex justify-content-end gap-2 mt-3">
                    <button type="submit" class="btn btn-primary btn-sm px-3">Filter</button>
                    <a href="${pageContext.request.contextPath}/admin/access-logs" class="btn btn-outline-secondary btn-sm px-3">Reset</a>
                </div>
            </form>
        </div>
    </div>

    <div class="card shadow-sm mb-4">
        <div class="card-body p-0">
            <div class="table-responsive" style="max-height: 720px; overflow-y: auto;">
                <table class="table table-hover table-striped align-middle mb-0">
                    <thead class="table-light sticky-top" style="position: sticky; top: 0; z-index: 10; box-shadow: 0 1px 2px rgba(0,0,0,0.06);">
                        <tr>
                            <th scope="col" class="ps-3">
                                <t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/access-logs" column="time" label="Time"/>
                            </th>
                            <th scope="col">
                                <t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/access-logs" column="device" label="Device"/>
                            </th>
                            <th scope="col">Room</th>
                            <th scope="col">Guest</th>
                            <th scope="col">Booking</th>
                            <th scope="col">
                                <t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/access-logs" column="result" label="Result"/>
                            </th>
                            <th scope="col">Reason</th>
                            <th scope="col">Credential ID</th>
                            <th scope="col" class="pe-3">Request ID</th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:if test="${empty result.items}">
                            <tr><td colspan="9" class="text-center py-4 text-muted">No records found</td></tr>
                        </t:if>
                        <t:forEach items="${result.items}" var="row">
                            <tr>
                                <td class="ps-3 font-monospace text-nowrap">${e:datetime(row.attemptedAt)}</td>
                                <td class="font-monospace fw-semibold">${e:esc(row.deviceId)}</td>
                                <td>
                                    <t:if test="${not empty row.roomNumber}">
                                        <span class="badge bg-light text-dark border">Room ${e:esc(row.roomNumber)}</span>
                                    </t:if>
                                    <t:if test="${empty row.roomNumber}">
                                        <span class="badge bg-secondary-subtle text-secondary">- (Shared)</span>
                                    </t:if>
                                </td>
                                <td>
                                    <t:if test="${not empty row.guestName}">
                                        <span class="fw-semibold text-espresso">${e:esc(row.guestName)}</span>
                                    </t:if>
                                    <t:if test="${empty row.guestName}">
                                        <span class="text-muted">&mdash;</span>
                                    </t:if>
                                </td>
                                <td>
                                    <t:if test="${not empty row.bookingCode}">
                                        <a href="${pageContext.request.contextPath}/admin/bookings/detail?id=${row.bookingId}"
                                           class="badge border bg-white text-espresso font-monospace text-decoration-none shadow-2xs"
                                           style="border-color: #D9B991 !important;">
                                            ${e:esc(row.bookingCode)}
                                        </a>
                                    </t:if>
                                    <t:if test="${empty row.bookingCode}">
                                        <span class="text-muted">&mdash;</span>
                                    </t:if>
                                </td>
                                <td>
                                    <span class="badge-status status-${e:esc(row.result)}">${e:esc(row.result)}</span>
                                </td>
                                <td>
                                    <span class="badge bg-light text-dark border font-monospace">${e:esc(row.reasonCode)}</span>
                                </td>
                                <td>
                                    <t:if test="${row.credentialId != null}">
                                        <span class="font-monospace">${row.credentialId}</span>
                                    </t:if>
                                    <t:if test="${row.credentialId == null}">
                                        <span class="text-muted">&mdash;</span>
                                    </t:if>
                                </td>
                                <td class="pe-3 font-monospace text-muted small">${e:esc(row.requestId)}</td>
                            </tr>
                        </t:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <t:pagination result="${result}" baseUrl="${pageContext.request.contextPath}/admin/access-logs"/>
</t:layout>
