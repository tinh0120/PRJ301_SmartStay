<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>

<t:layout title="${mode == 'edit' ? 'Edit Assignment' : 'New Assignment'}" active="admin-assignments">
    <div class="row justify-content-center">
        <div class="col-lg-8">
            <a href="${pageContext.request.contextPath}/admin/assignments" class="ss-back-link"><i class="bi bi-chevron-left" aria-hidden="true"></i> Cleaning assignments</a>

            <div class="card border-0 shadow-sm">
                <div class="card-header bg-white py-3">
                    <h1 class="h4 mb-0">${mode == 'edit' ? 'Edit cleaning assignment' : 'New cleaning assignment'}</h1>
                    <p class="text-muted small mb-0">
                        ${mode == 'edit' ? 'Update shift times or notes for active assignment' : 'Assign an active housekeeper to clean a turnover room'}
                    </p>
                </div>
                <div class="card-body p-4">
                    <form action="${pageContext.request.contextPath}/admin/assignments/edit" method="post">
                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                        <t:if test="${mode == 'edit'}">
                            <input type="hidden" name="assignmentId" value="${assignment.assignmentId}">
                        </t:if>

                        <!-- Room Selection / Display -->
                        <div class="mb-3">
                            <label class="form-label fw-bold" for="asgRoom">Room</label>
                            <t:if test="${mode == 'edit'}">
                                <div class="input-group">
                                    <span class="input-group-text bg-light">${e:esc(assignment.roomTitle)} (Room ${e:esc(assignment.roomNumber)})</span>
                                    <input type="text" class="form-control" id="asgRoom" disabled value="Status: ${e:esc(e:humanStatus(assignment.roomStatus))}">
                                </div>
                            </t:if>
                            <t:if test="${mode != 'edit'}">
                                <t:if test="${empty roomList}">
                                    <div class="alert alert-warning py-2 small mb-0">No rooms need cleaning right now. Assignments can only be created for rooms that need cleaning.</div>
                                </t:if>
                                <t:if test="${not empty roomList}">
                                    <select name="roomId" id="asgRoom" class="form-select" required>
                                        <t:forEach items="${roomList}" var="r">
                                            <option value="${r.roomId}">${e:esc(r.displayTitle)} (${e:esc(r.roomNumber)}) - ${e:esc(e:humanStatus(r.status))}</option>
                                        </t:forEach>
                                    </select>
                                    <div class="form-text">Only rooms that need cleaning are listed.</div>
                                </t:if>
                            </t:if>
                        </div>

                        <!-- Staff Member Selection -->
                        <div class="mb-3">
                            <label class="form-label fw-bold" for="asgStaff">Housekeeper / Staff</label>
                            <select name="staffId" id="asgStaff" class="form-select" required>
                                <option value="">-- Select Active Staff Member --</option>
                                <t:forEach items="${staffList}" var="s">
                                    <option value="${s.userId}" ${mode == 'edit' and assignment.staffId == s.userId ? 'selected' : ''}>
                                        ${e:esc(s.fullName)} (${e:esc(s.email)})
                                    </option>
                                </t:forEach>
                            </select>
                            <div class="form-text">Only active accounts with the STAFF role are listed.</div>
                        </div>

                        <!-- Shift Window -->
                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label class="form-label fw-bold" for="asgShiftStart">Shift Start</label>
                                <input type="datetime-local" name="shiftStart" id="asgShiftStart" class="form-control" required
                                       value="${mode == 'edit' && assignment.shiftStart != null ? assignment.shiftStart.toString().substring(0, 16) : ''}">
                                <div class="form-text">When the housekeeper shift begins.</div>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-bold" for="asgShiftEnd">Shift End</label>
                                <input type="datetime-local" name="shiftEnd" id="asgShiftEnd" class="form-control" required
                                       value="${mode == 'edit' && assignment.shiftEnd != null ? assignment.shiftEnd.toString().substring(0, 16) : ''}">
                                <div class="form-text">When the shift ends (must be after shift start).</div>
                            </div>
                        </div>

                        <!-- Note -->
                        <div class="mb-4">
                            <label class="form-label fw-bold" for="asgNote">Assignment Note / Instructions</label>
                            <textarea name="note" id="asgNote" class="form-control" rows="3" maxlength="255"
                                      placeholder="e.g. Full linen change, replenish bath amenities...">${mode == 'edit' ? e:esc(assignment.note) : ''}</textarea>
                            <div class="form-text">Optional housekeeping instructions (up to 255 characters).</div>
                        </div>

                        <!-- Action Buttons -->
                        <div class="d-flex justify-content-between align-items-center pt-3 border-top">
                            <a href="${pageContext.request.contextPath}/admin/assignments" class="btn btn-outline-secondary">
                                Cancel
                            </a>
                            <button type="submit" class="btn btn-primary px-4" ${mode != 'edit' && empty roomList ? 'disabled' : ''}>
                                ${mode == 'edit' ? 'Update Assignment' : 'Create Assignment & Issue PIN'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</t:layout>
