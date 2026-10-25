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

<t:layout title="Housekeeping Portal" active="assignments">
    <div class="staff-portal pb-4">
        <!-- Boutique Breadcrumb Navigation -->
        <nav aria-label="breadcrumb" class="mb-3">
            <ol class="breadcrumb boutique-breadcrumb m-0">
                <li class="breadcrumb-item">
                    <a href="${pageContext.request.contextPath}/" class="boutique-breadcrumb-link">
                        <i class="bi bi-house-door me-1"></i>Home
                    </a>
                </li>
                <li class="breadcrumb-item active" aria-current="page">Housekeeping Portal</li>
            </ol>
        </nav>

        <!-- 1. Header Banner -->
        <div class="card border-0 shadow-sm rounded-3 mb-4 bg-milk"
             style="border: 1px solid #EBE1D3 !important;">
            <div class="card-body p-4">
                <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
                    <div>
                        <div class="d-inline-flex align-items-center px-3 py-1 rounded-pill mb-2"
                             style="background-color: #F0E4D3; color: #4A3326; font-size: 0.8rem; font-weight: 600;">
                            <span>Housekeeping Operations</span>
                        </div>
                        <h1 class="h3 fw-bold mb-1" style="color: #2B1B12; letter-spacing: -0.01em;">
                            My Cleaning Assignments
                        </h1>
                        <p class="mb-0 small" style="color: #6B5646; font-size: 0.92rem;">
                            Hello, <strong style="color: #2B1B12;">${not empty sessionScope.authUser ? e:esc(sessionScope.authUser.fullName) : 'Staff Member'}</strong>.
                            Manage room turnovers, shift schedules, and building access keys.
                        </p>
                    </div>

                    <div class="d-flex align-items-center gap-2">
                        <div class="bg-white border rounded-3 px-3 py-2 shadow-2xs text-end" style="border-color: #E8DDCE !important;">
                            <div style="font-size: 0.72rem; text-transform: uppercase; font-weight: 600; letter-spacing: 0.05em; color: #6B5646;">
                                Active Staff
                            </div>
                            <div class="fw-bold d-flex align-items-center gap-2" style="font-size: 0.92rem; color: #2B1B12;">
                                <i class="bi bi-person-badge text-caramel"></i>
                                <span>${not empty sessionScope.authUser ? e:esc(sessionScope.authUser.fullName) : 'Staff'}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 2. Bento KPI Metric Cards (Canonical Palette) -->
        <div class="row g-3 mb-4">
            <!-- Tasks to do -->
            <div class="col-6 col-lg-3">
                <div class="card border-0 shadow-sm rounded-3 h-100 bg-milk"
                     style="border: 1px solid ${kpiAssigned > 0 ? '#DFC9B0' : '#EBE1D3'} !important;">
                    <div class="card-body p-3 p-xl-4 d-flex align-items-center justify-content-between">
                        <div>
                            <span class="d-block small text-uppercase fw-semibold" style="letter-spacing: 0.04em; color: #6B5646; font-size: 0.75rem;">Tasks To Do</span>
                            <div class="fs-2 fw-bold lh-1 mt-1" style="color: #9A5B2E;">${kpiAssigned}</div>
                            <small class="d-block mt-1" style="font-size: 0.76rem; color: #8C7868;">Requires cleaning</small>
                        </div>
                        <div class="rounded-circle d-flex align-items-center justify-content-center"
                             style="width: 44px; height: 44px; background-color: #FAF0E4; color: #9A5B2E;">
                            <i class="bi bi-bell-fill fs-5"></i>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Completed Shifts -->
            <div class="col-6 col-lg-3">
                <div class="card border-0 shadow-sm rounded-3 h-100 bg-milk"
                     style="border: 1px solid #EBE1D3 !important;">
                    <div class="card-body p-3 p-xl-4 d-flex align-items-center justify-content-between">
                        <div>
                            <span class="d-block small text-uppercase fw-semibold" style="letter-spacing: 0.04em; color: #6B5646; font-size: 0.75rem;">Completed</span>
                            <div class="fs-2 fw-bold lh-1 mt-1" style="color: #2E7D32;">${kpiCompleted}</div>
                            <small class="d-block mt-1" style="font-size: 0.76rem; color: #8C7868;">Ready for guests</small>
                        </div>
                        <div class="rounded-circle d-flex align-items-center justify-content-center"
                             style="width: 44px; height: 44px; background-color: #E8F5E9; color: #2E7D32;">
                            <i class="bi bi-check-circle-fill fs-5"></i>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Archived / Past -->
            <div class="col-6 col-lg-3">
                <div class="card border-0 shadow-sm rounded-3 h-100 bg-milk"
                     style="border: 1px solid #EBE1D3 !important;">
                    <div class="card-body p-3 p-xl-4 d-flex align-items-center justify-content-between">
                        <div>
                            <span class="d-block small text-uppercase fw-semibold" style="letter-spacing: 0.04em; color: #6B5646; font-size: 0.75rem;">Archived</span>
                            <div class="fs-2 fw-bold lh-1 mt-1" style="color: #4A3326;">${kpiExpired}</div>
                            <small class="d-block mt-1" style="font-size: 0.76rem; color: #8C7868;">Past shift windows</small>
                        </div>
                        <div class="rounded-circle d-flex align-items-center justify-content-center"
                             style="width: 44px; height: 44px; background-color: #F0E4D3; color: #4A3326;">
                            <i class="bi bi-archive-fill fs-5"></i>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Total Assigned -->
            <div class="col-6 col-lg-3">
                <div class="card border-0 shadow-sm rounded-3 h-100 bg-milk"
                     style="border: 1px solid #EBE1D3 !important;">
                    <div class="card-body p-3 p-xl-4 d-flex align-items-center justify-content-between">
                        <div>
                            <span class="d-block small text-uppercase fw-semibold" style="letter-spacing: 0.04em; color: #6B5646; font-size: 0.75rem;">Total Assigned</span>
                            <div class="fs-2 fw-bold lh-1 mt-1" style="color: #2B1B12;">${pageResult.totalItems}</div>
                            <small class="d-block mt-1" style="font-size: 0.76rem; color: #8C7868;">Overall records</small>
                        </div>
                        <div class="rounded-circle d-flex align-items-center justify-content-center"
                             style="width: 44px; height: 44px; background-color: #FAF5EC; color: #2B1B12; border: 1px solid #E8DDCE;">
                            <i class="bi bi-clipboard2-check-fill fs-5" style="color: #9A5B2E;"></i>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 3. Main Data Card: Filter Bar + Clean Table -->
        <div class="card border-0 shadow-sm rounded-3 overflow-hidden bg-milk"
             style="border: 1px solid #EBE1D3 !important;">
            
            <!-- Toolbar Header: Filter Pills & Search -->
            <div class="card-header bg-white p-3 p-md-4 border-bottom d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3"
                 style="border-color: #EBE1D3 !important;">
                
                <!-- Filter Pills -->
                <div class="d-flex align-items-center flex-wrap gap-2" id="filterPillsGroup">
                    <button type="button" class="btn btn-sm rounded-pill px-3 filter-pill active" data-filter="ALL">
                        All (${kpiTotal})
                    </button>
                    <button type="button" class="btn btn-sm rounded-pill px-3 filter-pill" data-filter="ASSIGNED">
                        To Do (${kpiAssigned})
                    </button>
                    <button type="button" class="btn btn-sm rounded-pill px-3 filter-pill" data-filter="COMPLETED">
                        Completed (${kpiCompleted})
                    </button>
                    <button type="button" class="btn btn-sm rounded-pill px-3 filter-pill" data-filter="EXPIRED">
                        Archived (${kpiExpired})
                    </button>
                </div>

                <!-- Live Search Input -->
                <div class="d-flex align-items-center" style="min-width: 250px;">
                    <div class="input-group input-group-sm">
                        <span class="input-group-text bg-white border-end-0 text-muted ps-3" style="border-color: #E0D5C7;">
                            <i class="bi bi-search"></i>
                        </span>
                        <input type="text" id="assignmentSearchInput" class="form-control bg-white border-start-0 pe-3"
                               style="border-color: #E0D5C7; color: #2B1B12;"
                               placeholder="Search room or task ID..." aria-label="Search assignments">
                    </div>
                </div>
            </div>

            <!-- Table of Assignments (Clean, High-Readability, Canonical Colors) -->
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0" id="assignmentsTable">
                    <thead style="background-color: #F0E4D3; border-bottom: 2px solid #E2D4C0;">
                        <tr style="font-size: 0.78rem; letter-spacing: 0.05em; color: #4A3326;">
                            <th class="ps-4 py-3" style="width: 90px;">TASK ID</th>
                            <th class="py-3" style="width: 220px;">ROOM</th>
                            <th class="py-3" style="width: 220px;">SHIFT SCHEDULE</th>
                            <th class="py-3" style="width: 170px;">STATUS</th>
                            <th class="py-3">HOUSEKEEPING NOTES</th>
                            <th class="py-3 pe-4 text-end" style="width: 140px;">ACTION</th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:if test="${empty assignments}">
                            <tr id="serverEmptyRow">
                                <td colspan="6" class="text-center py-5">
                                    <div class="text-muted">
                                        <i class="bi bi-clipboard-x fs-1 d-block mb-2" style="color: #B5A496;"></i>
                                        <h5 class="fw-semibold mb-1" style="color: #2B1B12;">No cleaning assignments scheduled</h5>
                                        <p class="small text-muted mb-0">When new turnovers are assigned, they will appear here.</p>
                                    </div>
                                </td>
                            </tr>
                        </t:if>

                        <t:forEach items="${assignments}" var="a">
                            <tr class="assignment-row ${a.status == 'ASSIGNED' ? 'row-assigned' : ''}"
                                data-status="${e:esc(a.status)}"
                                data-search="${e:esc(a.assignmentId)} ${e:esc(a.roomTitle)} ${e:esc(a.roomNumber)} ${e:esc(a.note)} ${e:esc(a.status)}"
                                style="border-bottom: 1px solid #F0E4D3;">
                                
                                <!-- Task ID -->
                                <td class="ps-4">
                                    <span class="font-monospace fw-bold" style="color: #9A5B2E; font-size: 0.88rem;">
                                        #${a.assignmentId}
                                    </span>
                                </td>

                                <!-- Room Details (Clean & Easy to Read) -->
                                <td>
                                    <div>
                                        <span class="fw-bold d-inline-block" style="color: #2B1B12; font-size: 0.95rem;">
                                            ${e:esc(a.roomTitle)}
                                        </span>
                                        <span class="badge ms-1" style="background-color: #FAF5EC; color: #4A3326; border: 1px solid #E2D7C8; font-size: 0.72rem; font-weight: 600;">
                                            Room ${e:esc(a.roomNumber)}
                                        </span>
                                    </div>
                                </td>

                                <!-- Shift Schedule -->
                                <td>
                                    <div class="fw-semibold" style="color: #2B1B12; font-size: 0.88rem;">
                                        ${a.shiftStart != null ? e:date(a.shiftStart) : '-'}
                                    </div>
                                    <div class="small" style="color: #6B5646;">
                                        ${a.shiftStart != null ? e:time(a.shiftStart) : '-'} &ndash; ${a.shiftEnd != null ? e:time(a.shiftEnd) : '-'}
                                    </div>
                                </td>

                                <!-- Status (Clean Single-Line Badges) -->
                                <td>
                                    <t:if test="${a.status == 'ASSIGNED'}">
                                        <span class="badge rounded-pill px-3 py-2 fw-semibold"
                                              style="background-color: #FAF0E4; color: #9A5B2E; border: 1px solid #DFC9B0; font-size: 0.75rem;">
                                            ASSIGNED
                                        </span>
                                        <t:if test="${a.sessionStatus == 'ACTIVE'}">
                                            <span class="badge rounded-pill bg-warning text-dark px-2 py-1 ms-1" style="font-size: 0.70rem;">
                                                IN PROGRESS
                                            </span>
                                        </t:if>
                                    </t:if>

                                    <t:if test="${a.status == 'COMPLETED'}">
                                        <span class="badge rounded-pill px-3 py-2 fw-semibold"
                                              style="background-color: #E8F5E9; color: #2E7D32; border: 1px solid #C8E6C9; font-size: 0.75rem;">
                                            COMPLETED
                                        </span>
                                    </t:if>

                                    <t:if test="${a.status == 'EXPIRED'}">
                                        <span class="badge rounded-pill px-3 py-2 fw-normal"
                                              style="background-color: #F3EFE8; color: #7A6658; border: 1px solid #E2D7C8; font-size: 0.75rem;">
                                            EXPIRED
                                        </span>
                                    </t:if>

                                    <t:if test="${a.status != 'ASSIGNED' and a.status != 'COMPLETED' and a.status != 'EXPIRED'}">
                                        <span class="badge rounded-pill px-3 py-2 bg-secondary text-white" style="font-size: 0.75rem;">
                                            ${e:esc(a.status)}
                                        </span>
                                    </t:if>
                                </td>

                                <!-- Housekeeping Notes -->
                                <td>
                                    <t:if test="${not empty a.note}">
                                        <div class="small text-truncate" style="max-width: 250px; color: #4A3326;" title="${e:esc(a.note)}">
                                            ${e:esc(a.note)}
                                        </div>
                                    </t:if>
                                    <t:if test="${empty a.note}">
                                        <span style="color: #A39384; font-size: 0.88rem;">&mdash;</span>
                                    </t:if>
                                </td>

                                <!-- Action Buttons (Uniform Dimensions & Canonical Styling) -->
                                <td class="pe-4 text-end" style="white-space: nowrap;">
                                    <t:if test="${a.status == 'ASSIGNED'}">
                                        <a href="${pageContext.request.contextPath}/staff/assignments?id=${a.assignmentId}"
                                           class="staff-action-btn btn-action-clean">
                                            View &amp; Clean
                                        </a>
                                    </t:if>
                                    <t:if test="${a.status == 'COMPLETED'}">
                                        <a href="${pageContext.request.contextPath}/staff/assignments?id=${a.assignmentId}"
                                           class="staff-action-btn btn-action-details">
                                            Details
                                        </a>
                                    </t:if>
                                    <t:if test="${a.status == 'EXPIRED'}">
                                        <a href="${pageContext.request.contextPath}/staff/assignments?id=${a.assignmentId}"
                                           class="staff-action-btn btn-action-view">
                                            View
                                        </a>
                                    </t:if>
                                </td>
                            </tr>
                        </t:forEach>

                        <!-- Client-side No Results Filter Row -->
                        <tr id="clientNoMatchRow" style="display: none;">
                            <td colspan="6" class="text-center py-5">
                                <div class="text-muted">
                                    <i class="bi bi-search fs-2 d-block mb-2" style="color: #B5A496;"></i>
                                    <h6 class="fw-semibold mb-1" style="color: #2B1B12;">No matching assignments found</h6>
                                    <p class="small text-muted mb-0">Try clearing your search input or switching the status filter tab above.</p>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- 4. Pagination Footer -->
            <div class="card-footer bg-white border-0 py-3 px-4 d-flex flex-column flex-sm-row justify-content-between align-items-center gap-2"
                 style="border-top: 1px solid #F0E4D3 !important;">
                <span class="small" style="color: #6B5646;">
                    Showing <strong>${pageResult.fromIndex}</strong> &ndash; <strong>${pageResult.toIndex}</strong> of <strong>${pageResult.totalItems}</strong> assignments
                </span>
                <ul class="pagination pagination-sm mb-0">
                    <li class="page-item ${!pageResult.hasPrevious() ? 'disabled' : ''}">
                        <a class="page-link rounded-start px-3" style="color: #4A3326; border-color: #E2D7C8;" href="${pageContext.request.contextPath}/staff/assignments?page=${pageResult.page - 1}">
                            Previous
                        </a>
                    </li>
                    <li class="page-item active">
                        <span class="page-link px-3 fw-bold" style="background-color: #9A5B2E; border-color: #9A5B2E; color: #FFF;">${pageResult.page}</span>
                    </li>
                    <li class="page-item ${!pageResult.hasNext() ? 'disabled' : ''}">
                        <a class="page-link rounded-end px-3" style="color: #4A3326; border-color: #E2D7C8;" href="${pageContext.request.contextPath}/staff/assignments?page=${pageResult.page + 1}">
                            Next
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </div>

    <!-- Scoped Styling & Real-Time Filter Script -->
    <style>
        .staff-portal .bg-milk {
            background-color: #FFFDF9 !important;
        }
        .staff-portal .text-espresso {
            color: #2B1B12 !important;
        }
        .staff-portal .text-cocoa {
            color: #4A3326 !important;
        }
        .staff-portal .text-caramel {
            color: #9A5B2E !important;
        }
        .staff-portal .filter-pill {
            background-color: #FAF5EC;
            color: #4A3326;
            border: 1px solid #E5D9CB;
            font-size: 0.82rem;
            font-weight: 500;
            transition: all 0.2s ease;
        }
        .staff-portal .filter-pill:hover {
            background-color: #EFE4D3;
            color: #2B1B12;
        }
        .staff-portal .filter-pill.active {
            background-color: #9A5B2E !important;
            color: #FFFFFF !important;
            border-color: #9A5B2E !important;
            box-shadow: 0 2px 6px rgba(154, 91, 46, 0.25);
        }
        .staff-portal .row-assigned {
            background-color: #FDFBF7 !important;
        }
        .staff-portal .assignment-row:hover {
            background-color: #FAF5EC !important;
        }
        .staff-portal .staff-action-btn {
            display: inline-flex !important;
            align-items: center !important;
            justify-content: center !important;
            width: 110px !important;
            height: 32px !important;
            font-size: 0.82rem !important;
            font-weight: 600 !important;
            white-space: nowrap !important;
            border-radius: 6px !important;
            text-decoration: none !important;
            line-height: 1 !important;
            box-sizing: border-box !important;
            transition: all 0.15s ease;
        }
        .staff-portal .btn-action-clean {
            background-color: #9A5B2E !important;
            color: #FFFFFF !important;
            border: 1px solid #9A5B2E !important;
        }
        .staff-portal .btn-action-clean:hover {
            background-color: #7E451E !important;
            border-color: #7E451E !important;
            color: #FFFFFF !important;
            box-shadow: 0 2px 6px rgba(154, 91, 46, 0.25);
        }
        .staff-portal .btn-action-details {
            background-color: #F4FAF5 !important;
            color: #2E7D32 !important;
            border: 1px solid #B7DFBE !important;
        }
        .staff-portal .btn-action-details:hover {
            background-color: #E8F5E9 !important;
            border-color: #2E7D32 !important;
            color: #2E7D32 !important;
        }
        .staff-portal .btn-action-view {
            background-color: #FAF5EC !important;
            color: #6B5646 !important;
            border: 1px solid #D9CEC1 !important;
        }
        .staff-portal .btn-action-view:hover {
            background-color: #EFE4D3 !important;
            color: #2B1B12 !important;
            border-color: #C8B9A6 !important;
        }
    </style>

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
