<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>

<t:layout title="Staff Assignments" active="admin-assignments">
    <%-- V3: same anatomy as the other admin lists. Edit is the visible action; Cancel moves to "⋯". --%>
    <div class="ss-page-header">
        <div>
            <h1 class="ss-page-header__title">Cleaning assignments</h1>
            <p class="ss-page-header__subtitle">Who cleans which room, and when. Each assignment gives the staff member a door PIN for the shift.</p>
        </div>
        <a href="${pageContext.request.contextPath}/admin/assignments/edit" class="btn btn-primary btn-sm">New assignment</a>
    </div>

    <form action="${pageContext.request.contextPath}/admin/assignments" method="get" class="ss-toolbar">
        <t:if test="${not empty status}"><input type="hidden" name="status" value="${e:esc(status)}"></t:if>
        <div class="ss-toolbar__main">
            <div class="ss-toolbar__search">
                <i class="bi bi-search ss-toolbar__search-icon" aria-hidden="true"></i>
                <label for="asgSearch" class="visually-hidden">Search assignments</label>
                <input type="search" class="ss-toolbar__search-input" id="asgSearch" name="search" maxlength="100"
                       placeholder="Staff name, email or room" value="${e:esc(search)}">
            </div>
            <div class="ss-toolbar__chips" role="group" aria-label="Assignment status">
                <a class="ss-chip ${empty status ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/assignments?search=${e:url(search)}&amp;staffId=${staffId}&amp;roomId=${roomId}&amp;shiftDate=${shiftDate}">All</a>
                <a class="ss-chip ${status == 'ASSIGNED' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/assignments?search=${e:url(search)}&amp;staffId=${staffId}&amp;roomId=${roomId}&amp;shiftDate=${shiftDate}&amp;status=ASSIGNED">Assigned</a>
                <a class="ss-chip ${status == 'COMPLETED' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/assignments?search=${e:url(search)}&amp;staffId=${staffId}&amp;roomId=${roomId}&amp;shiftDate=${shiftDate}&amp;status=COMPLETED">Completed</a>
                <a class="ss-chip ${status == 'EXPIRED' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/assignments?search=${e:url(search)}&amp;staffId=${staffId}&amp;roomId=${roomId}&amp;shiftDate=${shiftDate}&amp;status=EXPIRED">Expired</a>
                <a class="ss-chip ${status == 'CANCELLED' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/assignments?search=${e:url(search)}&amp;staffId=${staffId}&amp;roomId=${roomId}&amp;shiftDate=${shiftDate}&amp;status=CANCELLED">Cancelled</a>
            </div>
            <button class="ss-toolbar__more-toggle" type="button" data-bs-toggle="collapse" data-bs-target="#asgMoreFilters"
                    aria-expanded="${not empty staffId || not empty roomId || not empty shiftDate ? 'true' : 'false'}" aria-controls="asgMoreFilters">
                <i class="bi bi-sliders" aria-hidden="true"></i>More filters
            </button>
        </div>
        <div class="collapse ${not empty staffId || not empty roomId || not empty shiftDate ? 'show' : ''}" id="asgMoreFilters">
            <div class="ss-toolbar__collapse ss-bk-more">
                <div class="ss-bk-more__field">
                    <label for="asgStaff" class="ss-bk-more__label">Staff</label>
                    <select id="asgStaff" name="staffId" class="form-select form-select-sm">
                        <option value="">Anyone</option>
                        <t:forEach items="${staffList}" var="s">
                            <option value="${s.userId}" ${staffId == s.userId ? 'selected' : ''}>${e:esc(s.fullName)}</option>
                        </t:forEach>
                    </select>
                </div>
                <div class="ss-bk-more__field">
                    <label for="asgRoom" class="ss-bk-more__label">Room</label>
                    <select id="asgRoom" name="roomId" class="form-select form-select-sm">
                        <option value="">Any room</option>
                        <t:forEach items="${roomList}" var="r">
                            <option value="${r.roomId}" ${roomId == r.roomId ? 'selected' : ''}>Room ${e:esc(r.roomNumber)}</option>
                        </t:forEach>
                    </select>
                </div>
                <div class="ss-bk-more__field">
                    <label for="asgDate" class="ss-bk-more__label">Shift date</label>
                    <input type="date" id="asgDate" name="shiftDate" class="form-control form-control-sm" data-datepicker
                           value="${shiftDate != null ? shiftDate.toString() : ''}">
                </div>
                <div class="ss-bk-more__actions">
                    <button type="submit" class="btn btn-primary btn-sm px-3">Apply</button>
                    <a href="${pageContext.request.contextPath}/admin/assignments" class="btn btn-outline-soft btn-sm px-3">Clear all</a>
                </div>
            </div>
        </div>
    </form>

    <div class="ss-dev-table">
        <t:if test="${empty assignments}">
            <div class="ss-empty">
                <i class="bi bi-clipboard-check ss-empty__icon" aria-hidden="true"></i>
                <p class="ss-empty__title">No assignments found</p>
                <p class="ss-empty__desc">Nothing matches these filters. Create an assignment when a room needs cleaning.</p>
            </div>
        </t:if>
        <t:if test="${not empty assignments}">
            <div class="table-responsive">
                <table class="table align-middle mb-0 ss-table">
                    <thead>
                        <tr>
                            <th scope="col" class="ps-3">Staff</th>
                            <th scope="col">Room</th>
                            <th scope="col">Shift</th>
                            <th scope="col">Status</th>
                            <th scope="col">Note</th>
                            <th scope="col" class="pe-3 text-end"><span class="visually-hidden">Actions</span></th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:forEach items="${assignments}" var="a">
                            <tr class="${a.status == 'CANCELLED' ? 'ss-dev-row--off' : ''}">
                                <td class="ps-3" title="${e:esc(a.staffEmail)}">
                                    <t:if test="${a.assigned}"><a class="ss-row-link" href="${pageContext.request.contextPath}/admin/assignments/edit?id=${a.assignmentId}">${e:esc(a.staffName)}</a></t:if>
                                    <t:if test="${!a.assigned}">${e:esc(a.staffName)}</t:if>
                                </td>
                                <td class="text-nowrap">
                                    <span class="ss-bk__room">${e:esc(a.roomNumber)}</span>
                                    <span class="ss-bk__sub">${e:esc(a.roomTitle)}</span>
                                </td>
                                <td class="text-nowrap ss-num">
                                    ${e:stayLine(a.shiftStart, a.shiftEnd)}
                                </td>
                                <td>
                                    <t:if test="${a.status == 'EXPIRED'}">
                                        <span class="ss-badge ss-badge--warning">Expired</span>
                                    </t:if>
                                    <t:if test="${a.status != 'EXPIRED'}">
                                        <span class="ss-dev__status">
                                            <span class="ss-status-dot ${a.status == 'COMPLETED' ? 'ss-status-dot--ok' : (a.status == 'ASSIGNED' ? 'ss-status-dot--warn' : 'ss-status-dot--muted')}" aria-hidden="true"></span>
                                            ${e:esc(e:humanStatus(a.status))}
                                        </span>
                                    </t:if>
                                    <t:if test="${a.status == 'ASSIGNED' and a.sessionStatus == 'ACTIVE'}">
                                        <div class="ss-act__sub">Cleaning now</div>
                                    </t:if>
                                    <t:if test="${a.status != 'ASSIGNED' and not empty a.sessionStatus and a.sessionStatus != a.status and a.sessionStatus != 'PENDING'}">
                                        <div class="ss-act__sub">Cleaning: ${e:esc(e:humanStatus(a.sessionStatus))}</div>
                                    </t:if>
                                </td>
                                <td class="ss-act__sub text-truncate" style="max-width: 200px;" title="${not ((a.status == 'COMPLETED' and a.note == 'Completed on schedule') or (a.status == 'EXPIRED' and a.note == 'Expired')) ? e:esc(a.note) : ''}">
                                    <t:if test="${not ((a.status == 'COMPLETED' and a.note == 'Completed on schedule') or (a.status == 'EXPIRED' and a.note == 'Expired'))}">
                                        ${e:esc(a.note)}
                                    </t:if>
                                </td>
                                <td class="pe-3 text-end text-nowrap">
                                    <t:if test="${a.assigned}">
                                        <div class="dropdown d-inline-block ss-row-menu">
                                            <button class="btn-trigger" type="button" data-bs-toggle="dropdown" aria-expanded="false"
                                                    aria-label="More actions for ${e:esc(a.staffName)}, room ${e:esc(a.roomNumber)}">
                                                <i class="bi bi-three-dots" aria-hidden="true"></i>
                                            </button>
                                            <ul class="dropdown-menu dropdown-menu-end">
                                                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/assignments/edit?id=${a.assignmentId}">Edit</a></li>
                                                <li><hr class="dropdown-divider"></li>
                                                <li>
                                                    <form action="${pageContext.request.contextPath}/admin/assignments/cancel" method="post" class="m-0"
                                                          onsubmit="return confirm('Cancel this assignment? The staff door PIN stops working.');">
                                                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                                        <input type="hidden" name="id" value="${a.assignmentId}">
                                                        <button type="submit" class="dropdown-item dropdown-item--destructive">Cancel assignment</button>
                                                    </form>
                                                </li>
                                            </ul>
                                        </div>
                                    </t:if>
                                </td>
                            </tr>
                        </t:forEach>
                    </tbody>
                </table>
            </div>
        </t:if>
    </div>

    <t:if test="${pageResult.totalItems > 0}">
        <nav class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3" aria-label="Assignments pages">
            <span class="ss-act__sub">${pageResult.fromIndex}&ndash;${pageResult.toIndex} of ${pageResult.totalItems}</span>
            <ul class="pagination pagination-sm mb-0">
                <li class="page-item ${!pageResult.hasPrevious() ? 'disabled' : ''}">
                    <a class="page-link" href="${pageContext.request.contextPath}/admin/assignments?page=${pageResult.page - 1}&amp;search=${e:url(search)}&amp;status=${e:url(status)}&amp;staffId=${staffId}&amp;roomId=${roomId}&amp;shiftDate=${shiftDate}">Previous</a>
                </li>
                <li class="page-item active" aria-current="page"><span class="page-link">${pageResult.page}</span></li>
                <li class="page-item ${!pageResult.hasNext() ? 'disabled' : ''}">
                    <a class="page-link" href="${pageContext.request.contextPath}/admin/assignments?page=${pageResult.page + 1}&amp;search=${e:url(search)}&amp;status=${e:url(status)}&amp;staffId=${staffId}&amp;roomId=${roomId}&amp;shiftDate=${shiftDate}">Next</a>
                </li>
            </ul>
        </nav>
    </t:if>
</t:layout>
