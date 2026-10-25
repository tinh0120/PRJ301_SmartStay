<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>

<t:layout title="Staff Assignments" active="admin-assignments">
    <!-- Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Staff Assignments</li>
        </ol>
    </nav>

    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h1 class="h3 mb-0">Staff Room Assignments</h1>
            <p class="text-muted small mb-0">Manage housekeeping room assignments and cleaning sessions</p>
        </div>
        <div>
            <a href="${pageContext.request.contextPath}/admin/assignments/edit" class="btn btn-primary">
                + New Assignment
            </a>
        </div>
    </div>

    <!-- Mobile Filter Toggle Button (< 768px) -->
    <div class="d-md-none mb-3">
        <button class="btn btn-outline-secondary btn-sm w-100 d-flex justify-content-between align-items-center" type="button" data-bs-toggle="collapse" data-bs-target="#assignmentFilterCollapse" aria-expanded="false" aria-controls="assignmentFilterCollapse">
            <span><i class="bi bi-funnel me-1"></i> Filters &amp; Search</span>
            <i class="bi bi-chevron-down"></i>
        </button>
    </div>

    <!-- Filter Form -->
    <div class="card mb-4 border-0 shadow-sm collapse d-md-block" id="assignmentFilterCollapse">
        <div class="card-body">
            <form action="${pageContext.request.contextPath}/admin/assignments" method="get" class="row g-3">
                <div class="col-md-3">
                    <label class="form-label small text-muted">Search Staff or Room</label>
                    <input type="text" name="search" class="form-control form-control-sm"
                           placeholder="Name, email, room..." value="${e:esc(search)}">
                </div>
                <div class="col-md-2">
                    <label class="form-label small text-muted">Status</label>
                    <select name="status" class="form-select form-select-sm">
                        <option value="">All Statuses</option>
                        <option value="ASSIGNED" ${status == 'ASSIGNED' ? 'selected' : ''}>ASSIGNED</option>
                        <option value="COMPLETED" ${status == 'COMPLETED' ? 'selected' : ''}>COMPLETED</option>
                        <option value="CANCELLED" ${status == 'CANCELLED' ? 'selected' : ''}>CANCELLED</option>
                        <option value="EXPIRED" ${status == 'EXPIRED' ? 'selected' : ''}>EXPIRED</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label small text-muted">Staff</label>
                    <select name="staffId" class="form-select form-select-sm">
                        <option value="">All Staff</option>
                        <t:forEach items="${staffList}" var="s">
                            <option value="${s.userId}" ${staffId == s.userId ? 'selected' : ''}>${e:esc(s.fullName)}</option>
                        </t:forEach>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label small text-muted">Room</label>
                    <select name="roomId" class="form-select form-select-sm">
                        <option value="">All Rooms</option>
                        <t:forEach items="${roomList}" var="r">
                            <option value="${r.roomId}" ${roomId == r.roomId ? 'selected' : ''}>Room ${e:esc(r.roomNumber)}</option>
                        </t:forEach>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label small text-muted">Shift Date</label>
                    <input type="date" name="shiftDate" class="form-control form-control-sm" data-datepicker
                           value="${shiftDate != null ? shiftDate.toString() : ''}">
                </div>
                <div class="col-md-1 d-flex align-items-end gap-1">
                    <button type="submit" class="btn btn-sm btn-primary w-100">Filter</button>
                    <a href="${pageContext.request.contextPath}/admin/assignments" class="btn btn-sm btn-outline-secondary">Reset</a>
                </div>
            </form>
        </div>
    </div>

    <!-- Assignments Table -->
    <div class="card border-0 shadow-sm">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-light text-muted small text-uppercase">
                    <tr>
                        <th>ID</th>
                        <th>Staff Member</th>
                        <th>Room</th>
                        <th>Shift Window</th>
                        <th>Status</th>
                        <th>Session</th>
                        <th>Note</th>
                        <th class="text-end">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <t:if test="${empty assignments}">
                        <tr><td colspan="8" class="text-center py-4 text-muted">No staff assignments found matching criteria.</td></tr>
                    </t:if>
                    <t:forEach items="${assignments}" var="a">
                        <tr>
                            <td><strong>#${a.assignmentId}</strong></td>
                            <td><div class="fw-bold">${e:esc(a.staffName)}</div><div class="small text-muted">${e:esc(a.staffEmail)}</div></td>
                            <td>
                                <div class="fw-semibold small">${e:esc(a.roomTitle)}</div>
                                <span class="badge bg-light text-dark border">Room ${e:esc(a.roomNumber)}</span>
                            </td>
                            <td class="small">${e:datetime(a.shiftStart)}<br><span class="text-muted">to ${e:datetime(a.shiftEnd)}</span></td>
                            <td><span class="badge-status status-${a.status}">${e:esc(a.status)}</span></td>
                            <td><span class="badge-status status-${a.sessionStatus != null ? a.sessionStatus : 'PENDING'}">${a.sessionStatus != null ? e:esc(a.sessionStatus) : "PENDING"}</span></td>
                            <td class="small text-truncate" style="max-width: 180px;" title="${e:esc(a.note)}">${e:esc(a.note)}</td>
                            <td class="text-end">
                                <t:if test="${a.assigned}">
                                    <div class="btn-group btn-group-sm">
                                        <a href="${pageContext.request.contextPath}/admin/assignments/edit?id=${a.assignmentId}" class="btn btn-outline-primary">Edit</a>
                                        <form action="${pageContext.request.contextPath}/admin/assignments/cancel" method="post" class="d-inline" onsubmit="return confirm('Cancel this assignment and revoke staff PIN?');">
                                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                            <input type="hidden" name="id" value="${a.assignmentId}">
                                            <button type="submit" class="btn btn-outline-danger">Cancel</button>
                                        </form>
                                    </div>
                                </t:if>
                                <t:if test="${not a.assigned}">
                                    <span class="text-muted small">-</span>
                                </t:if>
                            </td>
                        </tr>
                    </t:forEach>
                </tbody>
            </table>
        </div>

        <!-- Pagination -->
        <div class="card-footer bg-white border-0 py-3 d-flex justify-content-between align-items-center">
            <span class="small text-muted">
                Showing ${pageResult.fromIndex} - ${pageResult.toIndex} of ${pageResult.totalItems} assignments
            </span>
            <ul class="pagination pagination-sm mb-0">
                <li class="page-item ${!pageResult.hasPrevious() ? 'disabled' : ''}">
                    <a class="page-link" href="${pageContext.request.contextPath}/admin/assignments?page=${pageResult.page - 1}&search=${e:esc(search)}&status=${e:esc(status)}&staffId=${staffId}&roomId=${roomId}&shiftDate=${shiftDate}">Previous</a>
                </li>
                <li class="page-item active">
                    <span class="page-link">${pageResult.page}</span>
                </li>
                <li class="page-item ${!pageResult.hasNext() ? 'disabled' : ''}">
                    <a class="page-link" href="${pageContext.request.contextPath}/admin/assignments?page=${pageResult.page + 1}&search=${e:esc(search)}&status=${e:esc(status)}&staffId=${staffId}&roomId=${roomId}&shiftDate=${shiftDate}">Next</a>
                </li>
            </ul>
        </div>
    </div>
</t:layout>
