<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>

<t:layout title="Assignment #${assignment.assignmentId}" active="assignments">
    <div class="staff-portal-detail pb-4">
        <a href="${pageContext.request.contextPath}/staff/assignments" class="ss-back-link"><i class="bi bi-chevron-left" aria-hidden="true"></i> My cleaning tasks</a>
        <div class="ss-page-header">
            <div>
                <h1 class="ss-page-header__title">Room ${e:esc(assignment.roomNumber)}, ${e:esc(assignment.roomTitle)}</h1>
                <p class="ss-page-header__subtitle">Cleaning task #${assignment.assignmentId}</p>
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
                            ${e:esc(e:humanStatus(assignment.status))}
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
                                    <div class="text-muted small">Tap Start when you begin, so the admin sees the room is being cleaned.</div>
                                    <form action="${pageContext.request.contextPath}/staff/cleaning" method="post" class="m-0">
                                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                        <input type="hidden" name="assignmentId" value="${assignment.assignmentId}">
                                        <input type="hidden" name="action" value="start">
                                        <button type="submit" class="btn btn-primary ss-staff__btn px-4">Start cleaning</button>
                                    </form>
                                </div>
                            </t:if>

                            <t:if test="${assignment.status == 'ASSIGNED' and assignment.startedAt != null}">
                                <div class="d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3">
                                    <div>
                                        <span class="ss-badge ss-badge--warning mb-1">Cleaning now</span>
                                        <div class="text-muted small">When the room is ready for guests, mark it clean. It becomes available for booking.</div>
                                    </div>
                                    <%-- Decision: "Mark as clean" needs a second tap within 3 s (no browser confirm dialog) --%>
                                    <form action="${pageContext.request.contextPath}/staff/cleaning" method="post" class="m-0" data-tap-confirm-form>
                                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                        <input type="hidden" name="assignmentId" value="${assignment.assignmentId}">
                                        <input type="hidden" name="action" value="complete">
                                        <button type="submit" class="btn btn-primary ss-staff__btn ss-tap-confirm px-4" data-tap-confirm aria-live="polite">
                                            <span class="ss-tap-confirm__text">Mark as clean</span>
                                            <span class="ss-tap-confirm__bar" aria-hidden="true"></span>
                                        </button>
                                    </form>
                                </div>
                            </t:if>

                            <t:if test="${assignment.status == 'COMPLETED'}">
                                <div class="alert alert-success py-3 px-4 mb-0 rounded-3 d-flex align-items-center gap-3">
                                    <i class="bi bi-check-circle-fill fs-3 text-success"></i>
                                    <div>
                                        <strong>Room is clean</strong>
                                        <div class="small text-secondary">Guests can book it again.</div>
                                    </div>
                                </div>
                            </t:if>

                            <t:if test="${assignment.status == 'EXPIRED'}">
                                <div class="alert alert-danger py-3 px-4 mb-0 rounded-3 d-flex align-items-center gap-3">
                                    <i class="bi bi-exclamation-octagon-fill fs-3 text-danger"></i>
                                    <div>
                                        <strong>This shift has ended</strong>
                                        <div class="small">Ask the admin to assign the room again if it still needs cleaning.</div>
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
                                Door PIN ${credentialInfo != null ? e:esc(e:humanStatus(credentialInfo.status)) : 'not available'}
                            </span>
                        </div>

                        <t:if test="${not empty shownPin}">
                            <%-- V3: same Digital Keycard look as the guest PIN --%>
                            <div class="ss-pin-vault my-3">
                                <div class="ss-pin-vault__label">Main entrance door PIN</div>
                                <div class="ss-pin-vault__display">
                                    <div class="ss-pin-vault__digits ss-pin-vault__digits--reveal">${e:esc(shownPin)}</div>
                                </div>
                                <span class="ss-pin-vault__state">Works during your shift</span>
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
                                <button type="submit" class="btn btn-primary ss-staff__btn w-100">
                                    Show door PIN
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
    <script src="${pageContext.request.contextPath}/assets/js/tap-confirm.js"></script>
</t:layout>
