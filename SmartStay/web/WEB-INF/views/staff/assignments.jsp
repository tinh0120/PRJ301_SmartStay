<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, model.Assignment" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%
    int countTotal = 0;
    int countAssigned = 0;
    int countCompleted = 0;
    int countExpired = 0;
    List<?> items = (List<?>) request.getAttribute("assignments");
    if (items != null) {
        countTotal = items.size();
        for (Object obj : items) {
            if (obj instanceof Assignment) {
                Assignment a = (Assignment) obj;
                if ("ASSIGNED".equalsIgnoreCase(a.getStatus())) {
                    countAssigned++;
                } else if ("COMPLETED".equalsIgnoreCase(a.getStatus())) {
                    countCompleted++;
                } else if ("EXPIRED".equalsIgnoreCase(a.getStatus())) {
                    countExpired++;
                }
            }
        }
    }
    pageContext.setAttribute("kpiTotal", countTotal);
    pageContext.setAttribute("kpiAssigned", countAssigned);
    pageContext.setAttribute("kpiCompleted", countCompleted);
    pageContext.setAttribute("kpiExpired", countExpired);
%>


<t:layout title="My cleaning tasks" active="assignments">
    <%-- V3: one task per card, large tap targets, quiet colours (only "To do" stands out).
         The filter script hooks are unchanged: #filterPillsGroup .filter-pill[data-filter],
         #assignmentSearchInput, .assignment-row[data-status][data-search], #clientNoMatchRow. --%>
    <div class="staff-portal pb-4">
        <div class="ss-page-header">
            <div>
                <h1 class="ss-page-header__title">My cleaning tasks</h1>
                <p class="ss-page-header__subtitle">Hello ${not empty sessionScope.authUser ? e:esc(sessionScope.authUser.fullName) : ''}. Open a task to see the room PIN and mark it clean.</p>
            </div>
        </div>

        <div class="ss-toolbar">
            <div class="ss-toolbar__main">
                <div class="ss-toolbar__search">
                    <i class="bi bi-search ss-toolbar__search-icon" aria-hidden="true"></i>
                    <label for="assignmentSearchInput" class="visually-hidden">Search tasks</label>
                    <input type="search" id="assignmentSearchInput" class="ss-toolbar__search-input" placeholder="Room name or number">
                </div>
                <div class="ss-toolbar__chips" id="filterPillsGroup" role="group" aria-label="Task status">
                    <button type="button" class="ss-chip filter-pill active" data-filter="ALL">All ${kpiTotal}</button>
                    <button type="button" class="ss-chip filter-pill" data-filter="ASSIGNED">To do ${kpiAssigned}</button>
                    <button type="button" class="ss-chip filter-pill" data-filter="COMPLETED">Done ${kpiCompleted}</button>
                    <button type="button" class="ss-chip filter-pill" data-filter="EXPIRED">Past ${kpiExpired}</button>
                </div>
            </div>
        </div>

        <t:if test="${empty assignments}">
            <div class="ss-empty">
                <i class="bi bi-clipboard-check ss-empty__icon" aria-hidden="true"></i>
                <p class="ss-empty__title">No cleaning tasks yet</p>
                <p class="ss-empty__desc">When the admin assigns you a room, it appears here with its time window.</p>
            </div>
        </t:if>

        <ul class="ss-staff__list" id="assignmentsTable" aria-label="Cleaning tasks">
            <t:forEach items="${assignments}" var="a">
                <li class="assignment-row ss-staff__task position-relative ${a.status == 'ASSIGNED' ? 'ss-staff__task--todo' : ''}"
                    data-status="${e:esc(a.status)}"
                    data-search="${e:esc(a.assignmentId)} ${e:esc(a.roomTitle)} ${e:esc(a.roomNumber)} ${e:esc(a.note)} ${e:esc(a.status)}">
                    <div class="ss-staff__main">
                        <div class="ss-staff__room">
                            <a class="ss-row-link stretched-link" href="${pageContext.request.contextPath}/staff/assignments?id=${a.assignmentId}">
                                Room ${e:esc(a.roomNumber)} <span class="ss-staff__title">${e:esc(a.roomTitle)}</span>
                            </a>
                        </div>
                        <div class="ss-staff__when ss-num">
                            <i class="bi bi-clock" aria-hidden="true"></i>
                            ${a.shiftStart != null ? e:date(a.shiftStart) : '-'},
                            ${a.shiftStart != null ? e:time(a.shiftStart) : '-'}&ndash;${a.shiftEnd != null ? e:time(a.shiftEnd) : '-'}
                        </div>
                        <t:if test="${not empty a.note}"><p class="ss-staff__note">${e:esc(a.note)}</p></t:if>
                    </div>
                    <div class="ss-staff__side">
                        <t:if test="${a.status == 'ASSIGNED'}">
                            <span class="ss-badge ss-badge--warning">${a.sessionStatus == 'ACTIVE' ? 'Cleaning now' : 'To do'}</span>
                            <a href="${pageContext.request.contextPath}/staff/assignments?id=${a.assignmentId}" class="btn btn-primary ss-staff__btn position-relative">Open task</a>
                        </t:if>
                        <t:if test="${a.status != 'ASSIGNED'}">
                            <span class="ss-dev__status">
                                <span class="ss-status-dot ${a.status == 'COMPLETED' ? 'ss-status-dot--ok' : 'ss-status-dot--muted'}" aria-hidden="true"></span>
                                ${a.status == 'COMPLETED' ? 'Done' : (a.status == 'EXPIRED' ? 'Past shift' : e:esc(e:humanStatus(a.status)))}
                            </span>
                        </t:if>
                    </div>
                </li>
            </t:forEach>
            <li id="clientNoMatchRow" class="ss-staff__nomatch" style="display: none;">
                No task matches this search or filter. Clear the search or choose "All".
            </li>
        </ul>

        <t:if test="${pageResult.totalItems > 0}">
            <nav class="d-flex justify-content-between align-items-center flex-wrap gap-2 mt-3" aria-label="Task pages">
                <span class="ss-act__sub">${pageResult.fromIndex}&ndash;${pageResult.toIndex} of ${pageResult.totalItems}</span>
                <ul class="pagination pagination-sm mb-0">
                    <li class="page-item ${!pageResult.hasPrevious() ? 'disabled' : ''}">
                        <a class="page-link" href="${pageContext.request.contextPath}/staff/assignments?page=${pageResult.page - 1}">Previous</a>
                    </li>
                    <li class="page-item active" aria-current="page"><span class="page-link">${pageResult.page}</span></li>
                    <li class="page-item ${!pageResult.hasNext() ? 'disabled' : ''}">
                        <a class="page-link" href="${pageContext.request.contextPath}/staff/assignments?page=${pageResult.page + 1}">Next</a>
                    </li>
                </ul>
            </nav>
        </t:if>
    </div>

    <script>
        document.addEventListener('DOMContentLoaded', function () {
            var filterPills = document.querySelectorAll('#filterPillsGroup .filter-pill');
            var searchInput = document.getElementById('assignmentSearchInput');
            var rows = document.querySelectorAll('.assignment-row');
            var noMatchRow = document.getElementById('clientNoMatchRow');

            var currentFilter = 'ALL';

            function applyFilters() {
                var query = searchInput.value.toLowerCase().trim();
                var visibleCount = 0;

                rows.forEach(function (row) {
                    var status = row.getAttribute('data-status') || '';
                    var searchText = (row.getAttribute('data-search') || '').toLowerCase();

                    var matchesFilter = (currentFilter === 'ALL') || (status === currentFilter);
                    var matchesQuery = !query || searchText.indexOf(query) !== -1;

                    if (matchesFilter && matchesQuery) {
                        row.style.display = '';
                        visibleCount++;
                    } else {
                        row.style.display = 'none';
                    }
                });

                if (noMatchRow) {
                    noMatchRow.style.display = (visibleCount === 0 && rows.length > 0) ? '' : 'none';
                }
            }

            filterPills.forEach(function (pill) {
                pill.addEventListener('click', function () {
                    filterPills.forEach(function (p) { p.classList.remove('active'); });
                    this.classList.add('active');
                    currentFilter = this.getAttribute('data-filter') || 'ALL';
                    applyFilters();
                });
            });

            if (searchInput) {
                searchInput.addEventListener('input', applyFilters);
            }
        });
    </script>
</t:layout>
