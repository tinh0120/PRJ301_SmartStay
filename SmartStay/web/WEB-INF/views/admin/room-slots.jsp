<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="${e:esc(room.displayTitle)} (${e:esc(room.roomNumber)}) Slots" active="admin-rooms">
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/rooms" class="boutique-breadcrumb-link">
                    Rooms
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">${e:esc(room.displayTitle)} Slots</li>
        </ol>
    </nav>

    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white py-3">
            <div class="d-flex justify-content-between align-items-center">
                <div>
                    <h1 class="h4 mb-0 fw-bold">Time Slots &amp; Pricing: ${e:esc(room.displayTitle)} <span class="text-muted fw-normal">(${e:esc(room.roomNumber)})</span></h1>
                    <p class="text-muted small mb-0">
                        Floor ${room.floorNumber} &bull; ${room.capacity} guests &bull; Current Status: 
                        <t:if test="${room.status == 'AVAILABLE'}"><span class="badge bg-success">AVAILABLE</span></t:if>
                        <t:if test="${room.status == 'NEEDS_CLEANING'}"><span class="badge bg-warning text-dark">NEEDS_CLEANING</span></t:if>
                        <t:if test="${room.status == 'MAINTENANCE'}"><span class="badge bg-danger">MAINTENANCE</span></t:if>
                        <t:if test="${room.status == 'OCCUPIED'}"><span class="badge bg-primary">OCCUPIED</span></t:if>
                        <t:if test="${room.status != 'AVAILABLE' and room.status != 'NEEDS_CLEANING' and room.status != 'MAINTENANCE' and room.status != 'OCCUPIED'}"><span class="badge bg-secondary">${e:esc(room.status)}</span></t:if>
                    </p>
                </div>
                <div>
                    <a href="${pageContext.request.contextPath}/admin/rooms/edit?roomId=${room.roomId}" class="btn btn-outline-primary btn-sm me-2">
                        &larr; Edit Room
                    </a>
                    <a href="${pageContext.request.contextPath}/admin/rooms" class="btn btn-outline-secondary btn-sm">
                        Rooms List
                    </a>
                </div>
            </div>
        </div>

        <div class="card-body p-4">
            <t:if test="${not empty errorMessage}">
                <div class="alert alert-danger flash-alert alert-dismissible fade show d-flex align-items-center py-2 px-3 shadow-sm rounded-3 mb-3" role="alert">
                    <i class="bi bi-x-circle-fill fs-5 text-danger me-2 flex-shrink-0"></i>
                    <div class="fw-semibold small flex-grow-1">${e:esc(errorMessage)}</div>
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                </div>
            </t:if>

            <form action="${pageContext.request.contextPath}/admin/rooms/slots" method="post">
                <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                <input type="hidden" name="roomId" value="${room.roomId}">

                <div class="card mb-4 shadow-sm" style="background-color: var(--color-ivory, #FAF7F0); border: 1px solid var(--color-latte, #D1B89A);">
                    <div class="card-body py-2 px-3">
                        <div class="d-flex align-items-center gap-3 flex-wrap">
                            <label for="roomStatus" class="form-label fw-bold mb-0 text-nowrap" style="color: var(--color-mocha, #3B2A20);">
                                <i class="bi bi-door-open me-1 text-primary"></i>Overall Room Status:
                            </label>
                            <select class="form-select form-select-sm" id="roomStatus" name="roomStatus" style="width: auto; min-width: 280px;">
                                <option value="AVAILABLE" ${room.status == 'AVAILABLE' ? 'selected' : ''}>AVAILABLE (Ready for guests)</option>
                                <option value="NEEDS_CLEANING" ${room.status == 'NEEDS_CLEANING' ? 'selected' : ''}>NEEDS_CLEANING (Housekeeping needed)</option>
                                <option value="MAINTENANCE" ${room.status == 'MAINTENANCE' ? 'selected' : ''}>MAINTENANCE (Full room maintenance)</option>
                                <option value="OCCUPIED" ${room.status == 'OCCUPIED' ? 'selected' : ''}>OCCUPIED (Guest in room)</option>
                            </select>
                            <span class="help-tip" data-bs-toggle="tooltip" data-bs-placement="right"
                                  title="Switch to NEEDS_CLEANING when housekeeping is required, or MAINTENANCE for room repairs. Once individual slots are cleaned, set their slot status below back to AVAILABLE.">?</span>
                        </div>
                    </div>
                </div>

                <div class="table-responsive mb-4">
                    <table class="table table-bordered align-middle">
                        <thead class="table-light">
                            <tr>
                                <th style="width: 80px;">Slot</th>
                                <th style="min-width: 150px;">Slot Name</th>
                                <th style="width: 130px;">Start Time</th>
                                <th style="width: 130px;">End Time</th>
                                <th style="width: 130px;">Overnight</th>
                                <th style="min-width: 150px;">List Price (VND)</th>
                                <th style="min-width: 170px;">Slot Status</th>
                                <th style="width: 80px;" class="text-center">Active</th>
                            </tr>
                        </thead>
                        <tbody>
                            <t:forEach items="${slots}" var="s">
                                <tr>
                                    <td class="fw-bold text-center">
                                        ${e:esc(s.slotCode)}
                                        <input type="hidden" name="slotId" value="${s.roomSlotId}">
                                        <input type="hidden" name="slotCode" value="${e:esc(s.slotCode)}">
                                        <input type="hidden" name="slotId_${s.slotCode}" value="${s.roomSlotId}">
                                    </td>
                                    <td>
                                        <input type="text" class="form-control form-control-sm" name="slotName_${s.slotCode}"
                                               value="${e:esc(s.slotName)}" maxlength="50" required>
                                    </td>
                                    <td>
                                        <input type="time" class="form-control form-control-sm" name="startTime_${s.slotCode}"
                                               value="${s.startTime}" required>
                                    </td>
                                    <td>
                                        <input type="time" class="form-control form-control-sm" name="endTime_${s.slotCode}"
                                               value="${s.endTime}" required>
                                    </td>
                                    <td class="text-center">
                                        <t:if test="${s.crossesMidnight}">
                                            <span class="badge bg-warning text-dark">+1 Day (Overnight)</span>
                                        </t:if>
                                        <t:if test="${!s.crossesMidnight}">
                                            <span class="badge bg-light text-muted border">Same Day</span>
                                        </t:if>
                                    </td>
                                    <td>
                                        <div class="input-group input-group-sm">
                                            <input type="number" class="form-control" name="price_${s.slotCode}"
                                                   value="${s.listPrice}" min="1000" step="1000" required>
                                            <span class="input-group-text">VND</span>
                                        </div>
                                    </td>
                                    <td>
                                        <select class="form-select form-select-sm" name="status_${s.slotCode}">
                                            <option value="AVAILABLE" ${s.status == 'AVAILABLE' ? 'selected' : ''}>AVAILABLE (Ready)</option>
                                            <option value="NEEDS_CLEANING" ${s.status == 'NEEDS_CLEANING' ? 'selected' : ''}>NEEDS_CLEANING (Cleaning)</option>
                                            <option value="MAINTENANCE" ${s.status == 'MAINTENANCE' ? 'selected' : ''}>MAINTENANCE (Maintenance)</option>
                                            <t:if test="${s.status == 'BOOKED'}">
                                                <option value="BOOKED" selected>BOOKED (Reserved)</option>
                                            </t:if>
                                        </select>
                                    </td>
                                    <td class="text-center">
                                        <div class="form-check form-switch d-inline-block">
                                            <input class="form-check-input" type="checkbox" name="active_${s.slotCode}"
                                                   ${s.active ? 'checked' : ''}>
                                        </div>
                                    </td>
                                </tr>
                            </t:forEach>
                        </tbody>
                    </table>
                </div>

                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-primary px-4">
                        Save Room &amp; Time Slots
                    </button>
                    <a href="${pageContext.request.contextPath}/admin/rooms" class="btn btn-outline-secondary px-4">
                        Cancel
                    </a>
                </div>
            </form>
        </div>
    </div>
</t:layout>
