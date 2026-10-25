<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Rooms" active="admin-rooms">
    <!-- Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Rooms</li>
        </ol>
    </nav>

    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h1 class="h2 mb-1">Room management</h1>
            <p class="text-muted small mb-0">Configure homestay rooms, status, and pricing</p>
        </div>
        <div>
            <a href="${pageContext.request.contextPath}/admin/rooms/edit" class="btn btn-primary btn-sm">
                + New room
            </a>
        </div>
    </div>

    <!-- Room Status KPI Summary Strip -->
    <div class="card border-0 shadow-sm rounded-4 p-3 mb-4" style="background: #FFFDF9; border: 1.5px solid #D9B991 !important;">
        <div class="d-flex align-items-center justify-content-between flex-wrap gap-2">
            <div class="d-flex align-items-center gap-2 flex-wrap">
                <span class="fw-bold text-espresso fs-6 me-2"><i class="bi bi-door-open-fill text-caramel me-1"></i>Total Rooms: <strong>${pageResult.totalItems}</strong></span>
                <a href="${pageContext.request.contextPath}/admin/rooms?status=AVAILABLE" class="badge rounded-pill text-decoration-none py-2 px-3 ${pr.getFilter('status') == 'AVAILABLE' ? 'bg-success text-white shadow-sm' : 'badge-boutique-available'}">
                    🟢 Available: <strong>${roomCounts['AVAILABLE'] != null ? roomCounts['AVAILABLE'] : 0}</strong>
                </a>
                <a href="${pageContext.request.contextPath}/admin/rooms?status=OCCUPIED" class="badge rounded-pill text-decoration-none py-2 px-3 ${pr.getFilter('status') == 'OCCUPIED' ? 'bg-caramel text-white shadow-sm' : 'badge-boutique-occupied'}">
                    🔴 Occupied: <strong>${roomCounts['OCCUPIED'] != null ? roomCounts['OCCUPIED'] : 0}</strong>
                </a>
                <a href="${pageContext.request.contextPath}/admin/rooms?status=NEEDS_CLEANING" class="badge rounded-pill text-decoration-none py-2 px-3 ${pr.getFilter('status') == 'NEEDS_CLEANING' ? 'bg-warning text-dark shadow-sm' : 'badge-boutique-cleaning'}">
                    🟡 Needs Cleaning: <strong>${roomCounts['NEEDS_CLEANING'] != null ? roomCounts['NEEDS_CLEANING'] : 0}</strong>
                </a>
                <a href="${pageContext.request.contextPath}/admin/rooms?status=MAINTENANCE" class="badge rounded-pill text-decoration-none py-2 px-3 ${pr.getFilter('status') == 'MAINTENANCE' ? 'bg-cocoa text-white shadow-sm' : 'badge-boutique-maintenance'}">
                    ⚪ Maintenance: <strong>${roomCounts['MAINTENANCE'] != null ? roomCounts['MAINTENANCE'] : 0}</strong>
                </a>
            </div>
            <t:if test="${not empty pr.getFilter('status')}">
                <a href="${pageContext.request.contextPath}/admin/rooms" class="btn btn-sm btn-outline-secondary rounded-pill px-3">
                    <i class="bi bi-x-circle me-1"></i>View all
                </a>
            </t:if>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <div class="card mb-4 shadow-sm">
        <div class="card-body">
            <form action="${pageContext.request.contextPath}/admin/rooms" method="get" class="row g-2 align-items-end">
                <input type="hidden" name="sort" value="${e:esc(pr.sort)}">
                <input type="hidden" name="dir" value="${e:esc(pr.dir)}">

                <div class="col-md-3">
                    <label for="keyword" class="form-label small fw-semibold">Search</label>
                    <input type="text" class="form-control form-control-sm" id="keyword" name="keyword"
                           placeholder="Room number or description..." maxlength="100"
                           value="${e:esc(pr.keyword)}">
                </div>

                <div class="col-md-2">
                    <label for="status" class="form-label small fw-semibold">Status</label>
                    <select class="form-select form-select-sm" id="status" name="status">
                        <option value="">All statuses</option>
                        <option value="AVAILABLE" ${pr.getFilter('status') == 'AVAILABLE' ? 'selected' : ''}>Available</option>
                        <option value="OCCUPIED" ${pr.getFilter('status') == 'OCCUPIED' ? 'selected' : ''}>Occupied</option>
                        <option value="NEEDS_CLEANING" ${pr.getFilter('status') == 'NEEDS_CLEANING' ? 'selected' : ''}>Needs cleaning</option>
                        <option value="MAINTENANCE" ${pr.getFilter('status') == 'MAINTENANCE' ? 'selected' : ''}>Maintenance</option>
                    </select>
                </div>

                <div class="col-md-2">
                    <label for="floor" class="form-label small fw-semibold">Floor</label>
                    <input type="number" class="form-control form-control-sm" id="floor" name="floor" min="0"
                           placeholder="Any floor" value="${e:esc(pr.getFilter('floor'))}">
                </div>

                <div class="col-md-2">
                    <label for="active" class="form-label small fw-semibold">Active</label>
                    <select class="form-select form-select-sm" id="active" name="active">
                        <option value="">All</option>
                        <option value="1" ${pr.getFilter('active') == '1' ? 'selected' : ''}>Active only</option>
                        <option value="0" ${pr.getFilter('active') == '0' ? 'selected' : ''}>Inactive only</option>
                    </select>
                </div>

                <div class="col-md-3 d-flex gap-2">
                    <button type="submit" class="btn btn-primary btn-sm px-3">Filter</button>
                    <a href="${pageContext.request.contextPath}/admin/rooms" class="btn btn-outline-secondary btn-sm px-3">Reset</a>
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
                            <th scope="col" class="ps-3">
                                <t:sortLink result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/rooms" column="number" label="Room #"/>
                            </th>
                            <th scope="col">
                                <t:sortLink result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/rooms" column="floor" label="Floor"/>
                            </th>
                            <th scope="col">
                                <t:sortLink result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/rooms" column="capacity" label="Capacity"/>
                            </th>
                            <th scope="col">Active</th>
                            <th scope="col">
                                <t:sortLink result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/rooms" column="price" label="Cheapest slot"/>
                            </th>
                            <th scope="col" class="pe-3 text-end">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:if test="${empty pageResult.items}">
                            <tr>
                                <td colspan="6" class="text-center text-muted py-4">No records found</td>
                            </tr>
                        </t:if>
                        <t:forEach items="${pageResult.items}" var="r">
                            <tr>
                                <td class="ps-3 fw-semibold">
                                    <div class="d-flex align-items-center flex-wrap gap-1">
                                        <span>${e:esc(r.displayTitle)}</span>
                                        <span class="text-muted small fw-normal">(${e:esc(r.roomNumber)})</span>
                                        <t:if test="${r.status == 'MAINTENANCE'}">
                                            <span class="badge bg-danger ms-1" style="font-size: 0.68rem;">MAINTENANCE</span>
                                        </t:if>
                                    </div>
                                    <t:if test="${not empty r.locationDescription}">
                                        <div class="text-muted small fw-normal">${e:esc(r.locationDescription)}</div>
                                    </t:if>
                                    <!-- Today's 4 Slot Timeline Badges -->
                                    <t:if test="${not empty r.slots}">
                                        <div class="mt-1 d-flex flex-wrap gap-1 align-items-center">
                                            <span class="text-muted small fw-normal me-1" style="font-size: 0.72rem;">Today:</span>
                                            <t:forEach items="${r.slots}" var="s">
                                                <span class="badge ${s.todayStatus == 'AVAILABLE' ? 'bg-success' : (s.todayStatus == 'BOOKED' ? 'bg-primary' : (s.todayStatus == 'NEEDS_CLEANING' ? 'bg-warning text-dark' : (s.todayStatus == 'MAINTENANCE' ? 'bg-danger' : 'bg-secondary')))}"
                                                      style="font-size: 0.72rem; padding: 0.2rem 0.4rem; cursor: default;"
                                                      title="${e:esc(s.slotCode)}: ${e:esc(s.slotName)} (${s.timeRange}) &bull; Status: ${s.todayStatus}">
                                                    ${e:esc(s.slotCode)}: ${s.todayStatus == 'AVAILABLE' ? 'Available' : (s.todayStatus == 'BOOKED' ? 'Booked' : (s.todayStatus == 'NEEDS_CLEANING' ? 'Cleaning' : (s.todayStatus == 'MAINTENANCE' ? 'Maint' : 'Off')))}
                                                </span>
                                            </t:forEach>
                                        </div>
                                    </t:if>
                                </td>
                                <td>Floor ${r.floorNumber}</td>
                                <td>${r.capacity} guests</td>
                                <td>
                                    <t:if test="${r.active}">
                                        <span class="badge bg-success">Active</span>
                                    </t:if>
                                    <t:if test="${!r.active}">
                                        <span class="badge bg-secondary">Inactive</span>
                                    </t:if>
                                </td>
                                <td>
                                    <t:if test="${not empty r.minPrice and r.minPrice > 0}">
                                        ${e:money(r.minPrice)}
                                    </t:if>
                                    <t:if test="${empty r.minPrice or r.minPrice <= 0}">
                                        <span class="text-muted">-</span>
                                    </t:if>
                                </td>
                                <td class="pe-3 text-end">
                                    <div class="d-inline-flex gap-1 align-items-center">
                                        <a href="${pageContext.request.contextPath}/admin/rooms/edit?roomId=${r.roomId}"
                                           class="btn btn-outline-primary btn-sm py-1 px-3" title="Edit room details and status">
                                            Edit
                                        </a>

                                        <!-- Delete Form -->
                                        <form action="${pageContext.request.contextPath}/admin/rooms/delete" method="post" class="d-inline m-0"
                                              onsubmit="return confirm('Are you sure you want to delete this room?');">
                                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                            <input type="hidden" name="roomId" value="${r.roomId}">
                                            <button type="submit" class="btn btn-outline-danger btn-sm py-1 px-3" title="Delete room">
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

    <t:pagination result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/rooms"/>
</t:layout>
