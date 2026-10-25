<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>

<t:layout title="Assignment #${assignment.assignmentId}" active="assignments">
    <div class="staff-portal-detail pb-4">
        <!-- Breadcrumb & Top Bar -->
        <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-3 mb-4">
            <div>
                <nav aria-label="breadcrumb" class="mb-2">
                    <ol class="breadcrumb boutique-breadcrumb m-0">
                        <li class="breadcrumb-item">
                            <a href="${pageContext.request.contextPath}/staff/assignments" class="boutique-breadcrumb-link">
                                <i class="bi bi-house-door me-1"></i>Housekeeping
                            </a>
                        </li>
                        <li class="breadcrumb-item active" aria-current="page">Assignment #${assignment.assignmentId}</li>
                    </ol>
                </nav>
                <h1 class="h3 fw-bold mb-0 text-dark">
                    Housekeeping Task: <span style="color: #9A5B2E;">${e:esc(assignment.roomTitle)}</span>
                </h1>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/staff/assignments" class="btn btn-outline-secondary rounded-pill px-3 shadow-2xs">
                    <i class="bi bi-chevron-left me-1"></i> Back to Schedule
                </a>
            </div>
        </div>

        <div class="row g-4">
            <!-- Main Task Details -->
            <div class="col-lg-8">
                <!-- Task Overview Card -->
                <div class="card border-0 shadow-sm rounded-4 mb-4 overflow-hidden bg-white" style="border: 1px solid rgba(0,0,0,0.08) !important;">
                    <div class="card-header bg-white py-3 px-4 border-bottom d-flex align-items-center justify-content-between">
                        <h5 class="mb-0 fw-bold text-dark d-flex align-items-center gap-2" style="font-size: 1.05rem;">
                            <i class="bi bi-clipboard-check text-primary"></i> Task Overview
                        </h5>
                        <span class="badge rounded-pill px-3 py-1 ${assignment.status == 'ASSIGNED' ? 'bg-primary text-white' : (assignment.status == 'COMPLETED' ? 'bg-success text-white' : 'bg-secondary text-white')}">
                            ${e:esc(assignment.status)}
                        </span>
                    </div>
                    <div class="card-body p-4">
                        <div class="row g-3 mb-4">
                            <div class="col-sm-6">
                                <div class="text-muted small text-uppercase fw-semibold" style="letter-spacing: 0.04em;">Assigned Room</div>
                                <div class="fs-5 fw-bold text-dark mt-1">${e:esc(assignment.roomTitle)}</div>
                                <div class="mt-1">
                                    <span class="badge bg-light text-dark border me-1">Room ${e:esc(assignment.roomNumber)}</span>
                                    <span class="badge ${assignment.roomStatus == 'NEEDS_CLEANING' ? 'bg-warning-subtle text-warning-emphasis' : (assignment.roomStatus == 'AVAILABLE' ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary')}">
                                        Room Status: ${e:esc(assignment.roomStatus)}
                                    </span>
                                </div>
                            </div>

                            <div class="col-sm-6">
                                <div class="text-muted small text-uppercase fw-semibold" style="letter-spacing: 0.04em;">Assigned Supervisor</div>
                                <div class="fw-bold text-dark mt-1 d-flex align-items-center gap-2">
                                    <i class="bi bi-person-check text-primary"></i> ${e:esc(assignment.assignedByName)}
                                </div>
                                <small class="text-muted">Administrator dispatch</small>
                            </div>

                            <div class="col-sm-12 pt-2 border-top">
                                <div class="text-muted small text-uppercase fw-semibold mb-2" style="letter-spacing: 0.04em;">Shift Window</div>
                                <div class="p-3 rounded-3" style="background-color: #FAF6F0; border: 1px solid #EFE6D8;">
                                    <div class="d-flex flex-wrap align-items-center justify-content-between gap-2">
                                        <div>
                                            <span class="text-muted small d-block">Start Time</span>
                                            <strong class="text-dark"><i class="bi bi-clock-history me-1 text-primary"></i>${assignment.shiftStart != null ? e:datetime(assignment.shiftStart) : "-"}</strong>
                                        </div>
                                        <div class="text-muted"><i class="bi bi-arrow-right fs-5"></i></div>
                                        <div>
                                            <span class="text-muted small d-block">End Time</span>
                                            <strong class="text-dark"><i class="bi bi-clock-history me-1 text-primary"></i>${assignment.shiftEnd != null ? e:datetime(assignment.shiftEnd) : "-"}</strong>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div>
                            <div class="text-muted small text-uppercase fw-semibold mb-2" style="letter-spacing: 0.04em;">Housekeeping Instructions</div>
                            <div class="p-3 rounded-3" style="background: #FDFDFD; border: 1px dashed #D9CFC4;">
                                <div class="text-secondary small">
                                    <t:if test="${empty assignment.note}">
                                        <i class="bi bi-info-circle me-1 text-muted"></i><em>Standard room turnover required. Replenish linens, sanitize surfaces, verify room inventory and ensure doors are locked.</em>
                                    </t:if>
                                    <t:if test="${not empty assignment.note}">
                                        <i class="bi bi-chat-quote-fill me-2 text-primary fs-6"></i>${e:esc(assignment.note)}
                                    </t:if>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Cleaning Session Progress Card -->
                <div class="card border-0 shadow-sm rounded-4 overflow-hidden bg-white" style="border: 1px solid rgba(0,0,0,0.08) !important;">
                    <div class="card-header bg-white py-3 px-4 border-bottom d-flex justify-content-between align-items-center">
                        <h5 class="mb-0 fw-bold text-dark d-flex align-items-center gap-2" style="font-size: 1.05rem;">
                            <i class="bi bi-hourglass-split text-primary"></i> Turnover Session Progress
                        </h5>
                        <span class="badge rounded-pill px-3 py-1 ${assignment.sessionStatus == 'ACTIVE' ? 'bg-warning text-dark' : (assignment.sessionStatus == 'COMPLETED' ? 'bg-success text-white' : 'bg-light text-dark border')}">
                            ${assignment.sessionStatus != null ? e:esc(assignment.sessionStatus) : 'PENDING'}
                        </span>
                    </div>
                    <div class="card-body p-4">
                        <div class="row g-3 mb-4">
                            <div class="col-sm-4">
                                <div class="text-muted small text-uppercase fw-semibold">Session Started</div>
                                <div class="fw-bold text-dark mt-1">
                                    ${assignment.startedAt != null ? e:datetime(assignment.startedAt) : '<span class="text-muted fw-normal fst-italic">Not started yet</span>'}
                                </div>
                            </div>
                            <div class="col-sm-4">
                                <div class="text-muted small text-uppercase fw-semibold">Scheduled End</div>
                                <div class="fw-bold text-dark mt-1">
                                    ${assignment.shiftEnd != null ? e:datetime(assignment.shiftEnd) : '-'}
                                </div>
                            </div>
                            <div class="col-sm-4">
                                <div class="text-muted small text-uppercase fw-semibold">Session Completed</div>
                                <div class="fw-bold text-dark mt-1">
                                    ${assignment.completedAt != null ? e:datetime(assignment.completedAt) : '<span class="text-muted fw-normal fst-italic">—</span>'}
                                </div>
                            </div>
                        </div>

                        <!-- Action Controls -->
                        <div class="pt-3 border-top">
                            <t:if test="${assignment.status == 'ASSIGNED' and assignment.startedAt == null}">
                                <div class="d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3">
                                    <div class="text-muted small">
                                        <i class="bi bi-info-circle me-1 text-primary"></i> Ready to begin cleaning? Click below to record session start.
                                    </div>
                                    <form action="${pageContext.request.contextPath}/staff/cleaning" method="post" class="m-0">
                                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                        <input type="hidden" name="assignmentId" value="${assignment.assignmentId}">
                                        <input type="hidden" name="action" value="start">
                                        <button type="submit" class="btn btn-success rounded-pill px-4 shadow-sm fw-semibold">
                                            <i class="bi bi-play-circle-fill me-1"></i> Start Cleaning Session
                                        </button>
                                    </form>
                                </div>
                            </t:if>

                            <t:if test="${assignment.status == 'ASSIGNED' and assignment.startedAt != null}">
                                <div class="d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3">
                                    <div>
                                        <span class="badge bg-warning-subtle text-warning-emphasis px-3 py-1 rounded-pill mb-1">
                                            <i class="bi bi-arrow-repeat me-1"></i> Cleaning in progress
                                        </span>
                                        <div class="text-muted small">Finished room turnover? Mark completed to release room to AVAILABLE status.</div>
                                    </div>
                                    <form action="${pageContext.request.contextPath}/staff/cleaning" method="post" class="m-0"
                                          onsubmit="return confirm('Complete this cleaning session and mark the room as AVAILABLE?');">
                                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                        <input type="hidden" name="assignmentId" value="${assignment.assignmentId}">
                                        <input type="hidden" name="action" value="complete">
                                        <button type="submit" class="btn btn-primary rounded-pill px-4 shadow-sm fw-semibold" style="background-color: #9A5B2E; border-color: #9A5B2E;">
                                            <i class="bi bi-check-circle-fill me-1"></i> Complete &amp; Release Room
                                        </button>
                                    </form>
                                </div>
                            </t:if>

                            <t:if test="${assignment.status == 'COMPLETED'}">
                                <div class="alert alert-success py-3 px-4 mb-0 rounded-3 d-flex align-items-center gap-3">
                                    <i class="bi bi-check-circle-fill fs-3 text-success"></i>
                                    <div>
                                        <strong>Turnover Complete!</strong>
                                        <div class="small text-secondary">This room has been cleaned and returned to AVAILABLE status for new guest bookings.</div>
                                    </div>
                                </div>
                            </t:if>

                            <t:if test="${assignment.status == 'EXPIRED'}">
                                <div class="alert alert-danger py-3 px-4 mb-0 rounded-3 d-flex align-items-center gap-3">
                                    <i class="bi bi-exclamation-octagon-fill fs-3 text-danger"></i>
                                    <div>
                                        <strong>Session Window Expired</strong>
                                        <div class="small">The scheduled shift window for this turnover task has ended. Please contact your manager for re-assignment.</div>
                                    </div>
                                </div>
                            </t:if>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Sidebar / Entrance PIN -->
            <div class="col-lg-4">
                <div class="card border-0 shadow-sm rounded-4 overflow-hidden bg-white mb-4" style="border: 1px solid rgba(0,0,0,0.08) !important;">
                    <div class="card-header bg-white py-3 px-4 border-bottom">
                        <h5 class="mb-0 fw-bold text-dark d-flex align-items-center gap-2" style="font-size: 1.05rem;">
                            <i class="bi bi-key-fill text-primary"></i> Main Entrance PIN
                        </h5>
                    </div>
                    <div class="card-body text-center p-4">
                        <div class="mb-3">
                            <span class="badge rounded-pill px-3 py-1 ${credentialInfo != null && (credentialInfo.status == 'PENDING' || credentialInfo.status == 'ACTIVE') ? 'bg-success text-white' : 'bg-secondary text-white'}">
                                Key ${credentialInfo != null ? e:esc(credentialInfo.status) : 'UNAVAILABLE'}
                            </span>
                        </div>

                        <t:if test="${not empty shownPin}">
                            <div class="my-3 p-4 rounded-4" style="background: linear-gradient(145deg, #FAF5EE, #F3E9DA); border: 1px solid #DFC9B0;">
                                <div class="text-muted small text-uppercase fw-semibold mb-1" style="letter-spacing: 0.05em;">Your 6-Digit Door PIN</div>
                                <div class="display-3 fw-bold font-monospace my-2 tracking-wider" style="color: #9A5B2E; letter-spacing: 0.15em;">
                                    ${e:esc(shownPin)}
                                </div>
                                <span class="badge bg-white text-muted border px-2 py-1 small"><i class="bi bi-shield-lock me-1"></i> Shift Authorized</span>
                            </div>
                            <p class="small text-muted mb-0 mt-3">
                                Enter this 6-digit PIN on the main door smart keypad followed by <strong>#</strong> to enter the homestay during your shift.
                            </p>
                        </t:if>

                        <t:if test="${empty shownPin and credentialInfo != null and (credentialInfo.status == 'PENDING' or credentialInfo.status == 'ACTIVE')}">
                            <div class="p-3 rounded-3 bg-light mb-3">
                                <i class="bi bi-lock-fill fs-2 text-primary d-block mb-1"></i>
                                <p class="small text-muted mb-0">A dedicated smart lock PIN is provisioned for this shift window.</p>
                            </div>
                            <form action="${pageContext.request.contextPath}/staff/pin" method="post">
                                <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                <input type="hidden" name="assignmentId" value="${assignment.assignmentId}">
                                <button type="submit" class="btn btn-primary rounded-pill w-100 py-2 fw-semibold shadow-sm" style="background-color: #9A5B2E; border-color: #9A5B2E;">
                                    <i class="bi bi-eye-fill me-1"></i> Show Entrance PIN
                                </button>
                            </form>
                        </t:if>

                        <t:if test="${empty shownPin and (credentialInfo == null or (credentialInfo.status != 'PENDING' and credentialInfo.status != 'ACTIVE'))}">
                            <div class="p-3 rounded-3 bg-light text-muted small">
                                <i class="bi bi-shield-slash fs-3 d-block mb-1 text-secondary"></i>
                                No active entrance PIN is currently valid for this assignment.
                            </div>
                        </t:if>

                        <div class="mt-4 pt-3 border-top text-start small text-muted">
                            <div class="d-flex justify-content-between py-1">
                                <span>Valid From:</span>
                                <strong class="text-dark">${credentialInfo != null && credentialInfo.validFrom != null ? credentialInfo.validFrom.toString().replace("T", " ") : "-"}</strong>
                            </div>
                            <div class="d-flex justify-content-between py-1">
                                <span>Valid Until:</span>
                                <strong class="text-dark">${credentialInfo != null && credentialInfo.validUntil != null ? credentialInfo.validUntil.toString().replace("T", " ") : "-"}</strong>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</t:layout>
