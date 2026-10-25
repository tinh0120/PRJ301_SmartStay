<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Booking ${detail.booking.bookingCode}" active="admin-bookings">
    <!-- Breadcrumb -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/bookings" class="boutique-breadcrumb-link">
                    Bookings
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">${e:esc(detail.booking.bookingCode)}</li>
        </ol>
    </nav>

    <!-- Header & Action Toolbar -->
    <div class="card shadow-sm border-0 mb-4">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">
                <div>
                    <div class="d-flex align-items-center gap-2 mb-1">
                        <h1 class="h3 fw-bold mb-0">${e:esc(detail.booking.bookingCode)}</h1>
                        <t:if test="${detail.booking.status == 'CONFIRMED'}">
                            <span class="badge bg-success fs-6">CONFIRMED</span>
                        </t:if>
                        <t:if test="${detail.booking.status == 'PENDING_PAYMENT'}">
                            <span class="badge bg-warning text-dark fs-6">PENDING_PAYMENT</span>
                        </t:if>
                        <t:if test="${detail.booking.status == 'CHECKED_IN'}">
                            <t:if test="${detail.booking.earlyCheckin}">
                                <span class="badge bg-primary fs-6 d-inline-flex align-items-center gap-1">
                                    <i class="bi bi-clock-history"></i> CHECKED_IN &middot; Early Check-in (${detail.booking.earlyCheckinMinutes} mins)
                                </span>
                            </t:if>
                            <t:if test="${!detail.booking.earlyCheckin}">
                                <span class="badge bg-primary fs-6">CHECKED_IN</span>
                            </t:if>
                        </t:if>
                        <t:if test="${detail.booking.status == 'CHECKED_OUT'}">
                            <span class="badge bg-info text-dark fs-6">CHECKED_OUT</span>
                        </t:if>
                        <t:if test="${detail.booking.status == 'CANCELLED'}">
                            <span class="badge bg-secondary fs-6">CANCELLED</span>
                        </t:if>
                        <t:if test="${detail.booking.status == 'EXPIRED'}">
                            <span class="badge bg-danger fs-6">EXPIRED</span>
                            <t:if test="${not empty detail.booking.expireReason}">
                                <span class="badge bg-danger-subtle text-danger border border-danger">${e:esc(detail.booking.expireReason)}</span>
                            </t:if>
                        </t:if>
                        <t:if test="${detail.booking.hasHeldSlots}">
                            <span class="badge bg-warning text-dark border">Held slots</span>
                        </t:if>
                    </div>
                    <p class="text-muted small mb-0">Created on ${e:datetime(detail.booking.createdAt)}</p>
                </div>

                <!-- Action Buttons -->
                <div class="d-flex flex-wrap gap-2">
                    <!-- Check-in -->
                    <t:if test="${detail.booking.status == 'CONFIRMED'}">
                        <!-- Allow Early Check-in Toggle -->
                        <form action="${pageContext.request.contextPath}/admin/bookings/early-checkin" method="post" class="d-inline m-0">
                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                            <input type="hidden" name="id" value="${detail.booking.bookingId}">
                            <input type="hidden" name="allow" value="${detail.booking.allowEarlyCheckin ? 'false' : 'true'}">
                            <button type="submit" class="btn ${detail.booking.allowEarlyCheckin ? 'btn-warning text-dark' : 'btn-outline-warning'} btn-sm fw-semibold">
                                <i class="bi ${detail.booking.allowEarlyCheckin ? 'bi-toggle-on' : 'bi-toggle-off'} me-1"></i>
                                ${detail.booking.allowEarlyCheckin ? 'Allow Early Check-in: ON' : 'Allow Early Check-in: OFF'}
                            </button>
                        </form>

                        <form action="${pageContext.request.contextPath}/admin/bookings/checkin" method="post" class="d-inline m-0"
                              onsubmit="return confirm('Confirm manual check-in for guest ${e:esc(detail.booking.guestName)}? Room will be marked OCCUPIED.');">
                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                            <input type="hidden" name="id" value="${detail.booking.bookingId}">
                            <button type="submit" class="btn btn-success btn-sm">Check-in</button>
                        </form>
                    </t:if>

                    <!-- Check-out -->
                    <t:if test="${detail.booking.status == 'CHECKED_IN'}">
                        <form action="${pageContext.request.contextPath}/admin/bookings/checkout" method="post" class="d-inline m-0"
                              onsubmit="return confirm('Confirm manual check-out for guest ${e:esc(detail.booking.guestName)}? Room will be marked NEEDS_CLEANING and active credentials expired.');">
                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                            <input type="hidden" name="id" value="${detail.booking.bookingId}">
                            <button type="submit" class="btn btn-info btn-sm text-white">Check-out</button>
                        </form>
                    </t:if>

                    <!-- Edit Booking -->
                    <t:if test="${detail.booking.status == 'PENDING_PAYMENT' or detail.booking.status == 'CONFIRMED'}">
                        <a href="${pageContext.request.contextPath}/admin/bookings/edit?id=${detail.booking.bookingId}" class="btn btn-outline-primary btn-sm">
                            ${detail.booking.status == 'CONFIRMED' ? 'Edit note' : 'Edit booking'}
                        </a>
                    </t:if>

                    <!-- Cancel Booking -->
                    <t:if test="${detail.booking.status == 'PENDING_PAYMENT' or detail.booking.status == 'CONFIRMED'}">
                        <button type="button" class="btn btn-outline-danger btn-sm" data-bs-toggle="modal" data-bs-target="#cancelModal">
                            Cancel booking
                        </button>
                    </t:if>

                    <!-- Mark Refunded for Cancelled Booking -->
                    <t:if test="${detail.booking.status == 'CANCELLED' && detail.booking.paymentStatus == 'REFUND_PENDING'}">
                        <button type="button" class="btn btn-warning btn-sm" data-bs-toggle="modal" data-bs-target="#refundModal">
                            <i class="bi bi-arrow-counterclockwise me-1"></i>Mark refunded
                        </button>
                    </t:if>

                    <!-- No-show interventions -->
                    <t:if test="${detail.booking.status == 'EXPIRED' and detail.booking.expireReason == 'NO_SHOW' and sessionScope.permissions.contains('NOSHOW_INTERVENE')}">
                        <form action="${pageContext.request.contextPath}/admin/bookings/noshow" method="post" class="d-inline m-0"
                              onsubmit="return confirm('Release all held slots for this no-show booking so others can book them?');">
                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                            <input type="hidden" name="id" value="${detail.booking.bookingId}">
                            <input type="hidden" name="action" value="release">
                            <button type="submit" class="btn btn-warning btn-sm">Release slots</button>
                        </form>

                        <button type="button" class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#reinstateModal">
                            Reinstate booking
                        </button>
                    </t:if>
                </div>
            </div>
        </div>
    </div>

    <!-- Booking Overview Grid -->
    <div class="row g-4 mb-4">
        <!-- Guest Details -->
        <div class="col-md-4">
            <div class="card shadow-sm border-0 h-100">
                <div class="card-header bg-white fw-bold py-3">Guest Information</div>
                <div class="card-body">
                    <div class="mb-2">
                        <span class="text-muted small d-block">Full Name</span>
                        <strong class="fs-6">${e:esc(detail.booking.guestName)}</strong>
                    </div>
                    <div class="mb-2">
                        <span class="text-muted small d-block">Email</span>
                        <a href="mailto:${e:esc(detail.booking.guestEmail)}" class="text-decoration-none">${e:esc(detail.booking.guestEmail)}</a>
                    </div>
                    <div class="mb-0">
                        <span class="text-muted small d-block">Phone</span>
                        <span>${e:esc(detail.booking.guestPhone)}</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- Room & Stay Details -->
        <div class="col-md-4">
            <div class="card shadow-sm border-0 h-100">
                <div class="card-header bg-white fw-bold py-3">Room &amp; Schedule</div>
                <div class="card-body">
                    <div class="mb-2">
                        <span class="text-muted small d-block">Room</span>
                        <strong>${e:esc(detail.room.displayTitle)}</strong>
                        <span class="badge bg-light text-secondary border ms-1">Room ${e:esc(detail.room.roomNumber)}</span>
                        <span class="badge bg-light text-secondary border ms-1">Floor ${detail.room.floorNumber}</span>
                        <span class="badge bg-light text-secondary border ms-1">Status: ${e:esc(detail.room.status)}</span>
                    </div>
                    <div class="mb-2">
                        <span class="text-muted small d-block">Stay Date</span>
                        <span>${e:date(detail.booking.slotDate)}</span>
                    </div>
                    <div class="mb-2">
                        <span class="text-muted small d-block">Scheduled Check-in / Check-out</span>
                        <span>${e:datetime(detail.booking.checkinAt)} &ndash; ${e:datetime(detail.booking.checkoutAt)}</span>
                    </div>
                    <t:if test="${not empty detail.booking.actualCheckinAt or not empty detail.booking.actualCheckoutAt}">
                        <div class="mb-0 small text-muted">
                            <t:if test="${not empty detail.booking.actualCheckinAt}">
                                <div>Actual in: ${e:datetime(detail.booking.actualCheckinAt)}</div>
                            </t:if>
                            <t:if test="${not empty detail.booking.actualCheckoutAt}">
                                <div>Actual out: ${e:datetime(detail.booking.actualCheckoutAt)}</div>
                            </t:if>
                        </div>
                    </t:if>
                </div>
            </div>
        </div>

        <!-- Financials & Audit Details -->
        <div class="col-md-4">
            <div class="card shadow-sm border-0 h-100">
                <div class="card-header bg-white fw-bold py-3">Billing &amp; Admin</div>
                <div class="card-body">
                    <div class="d-flex justify-content-between mb-1">
                        <span class="text-muted small">Subtotal</span>
                        <span>${e:money(detail.booking.subtotalAmount)}</span>
                    </div>
                    <t:if test="${detail.booking.discountPercent > 0}">
                        <div class="d-flex justify-content-between mb-1 text-success small">
                            <span>Discount (${detail.booking.discountPercent}%)</span>
                            <span>-${e:money(detail.booking.discountAmount)}</span>
                        </div>
                    </t:if>
                    <div class="d-flex justify-content-between mb-2 pt-1 border-top fw-bold fs-6">
                        <span>Total Amount</span>
                        <span class="text-primary">${e:money(detail.booking.totalAmount)}</span>
                    </div>
                    <t:if test="${detail.booking.status == 'PENDING_PAYMENT' and not empty detail.booking.paymentDeadline}">
                        <div class="mb-2 small text-danger">
                            Payment deadline: ${e:datetime(detail.booking.paymentDeadline)}
                        </div>
                    </t:if>
                    <t:if test="${not empty detail.booking.createdByName}">
                        <div class="small text-muted">
                            Created by: ${e:esc(detail.booking.createdByName)}
                        </div>
                    </t:if>
                    <t:if test="${not empty detail.booking.note}">
                        <div class="mt-2 pt-2 border-top small">
                            <span class="text-muted d-block fw-semibold">Note:</span>
                            <span class="fst-italic">${e:esc(detail.booking.note)}</span>
                        </div>
                    </t:if>
                    <t:if test="${not empty detail.booking.cancelReason}">
                        <div class="mt-2 pt-2 border-top small text-danger">
                            <span class="fw-semibold">Cancelled:</span> ${e:esc(detail.booking.cancelReason)}
                            <t:if test="${not empty detail.booking.cancelledAt}">
                                (${e:datetime(detail.booking.cancelledAt)})
                            </t:if>
                        </div>
                    </t:if>
                </div>
            </div>
        </div>
    </div>

    <!-- Booked Slots Table -->
    <div class="card shadow-sm border-0 mb-4">
        <div class="card-header bg-white fw-bold py-3">Booked Slots</div>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th class="ps-3">Slot</th>
                        <th>Name</th>
                        <th>Time Range</th>
                        <th>List Price</th>
                        <th class="pe-3">Hold Status</th>
                    </tr>
                </thead>
                <tbody>
                    <t:forEach items="${detail.slots}" var="slot">
                        <tr>
                            <td class="ps-3 fw-bold">${e:esc(slot.slotCode)}</td>
                            <td>${e:esc(slot.slotName)}</td>
                            <td>${e:esc(slot.timeRange)}</td>
                            <td>${e:money(slot.listPrice)}</td>
                            <td class="pe-3">
                                <t:if test="${slot.holdStatus == 'HELD'}">
                                    <span class="badge bg-warning text-dark">HELD</span>
                                </t:if>
                                <t:if test="${slot.holdStatus == 'RELEASED'}">
                                    <span class="badge bg-secondary">RELEASED</span>
                                </t:if>
                            </td>
                        </tr>
                    </t:forEach>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Payment Attempts Table -->
    <div class="card shadow-sm border-0 mb-4">
        <div class="card-header bg-white fw-bold py-3">
            <span>Payment Attempts</span>
        </div>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th class="ps-3">Order Code</th>
                        <th>Provider</th>
                        <th>Amount</th>
                        <th>Status</th>
                        <th>Created</th>
                        <th>Paid At</th>
                        <th class="pe-3">Resolution / Note</th>
                    </tr>
                </thead>
                <tbody>
                    <t:if test="${empty detail.payments}">
                        <tr>
                            <td colspan="7" class="text-center py-3 text-muted">No payment transactions recorded.</td>
                        </tr>
                    </t:if>
                    <t:forEach items="${detail.payments}" var="pm">
                        <tr>
                            <td class="ps-3 fw-semibold">${pm.orderCode}</td>
                            <td>${e:esc(pm.provider)}</td>
                            <td>${e:money(pm.amount)}</td>
                            <td>
                                <t:if test="${pm.status == 'PAID'}">
                                    <span class="badge bg-success">PAID</span>
                                </t:if>
                                <t:if test="${pm.status == 'PENDING'}">
                                    <span class="badge bg-warning text-dark">PENDING</span>
                                </t:if>
                                <t:if test="${pm.status == 'REVIEW_REQUIRED'}">
                                    <span class="badge bg-danger">REVIEW_REQUIRED</span>
                                </t:if>
                                <t:if test="${pm.status == 'REFUND_PENDING'}">
                                    <span class="badge bg-info text-dark">REFUND_PENDING</span>
                                </t:if>
                                <t:if test="${pm.status == 'REFUNDED'}">
                                    <span class="badge bg-secondary">REFUNDED</span>
                                </t:if>
                                <t:if test="${pm.status == 'FAILED'}">
                                    <span class="badge bg-danger">FAILED</span>
                                </t:if>
                                <t:if test="${pm.status == 'EXPIRED'}">
                                    <span class="badge bg-secondary">EXPIRED</span>
                                </t:if>
                            </td>
                            <td class="small text-muted">${e:datetime(pm.createdAt)}</td>
                            <td class="small text-muted">${e:datetime(pm.paidAt)}</td>
                            <td class="pe-3 small">
                                <t:if test="${not empty pm.resolutionNote}">
                                    <span>${e:esc(pm.resolutionNote)}</span>
                                </t:if>
                                <t:if test="${pm.status == 'REVIEW_REQUIRED' and sessionScope.permissions.contains('PAYMENT_RESOLVE')}">
                                    <a href="${pageContext.request.contextPath}/admin/payments?q=${e:url(detail.booking.bookingCode)}" class="btn btn-outline-danger btn-sm py-0 px-2 ms-2">
                                        Resolve
                                    </a>
                                </t:if>
                            </td>
                        </tr>
                    </t:forEach>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Access Credentials Metadata Table -->
    <div class="card shadow-sm border-0 mb-4">
        <div class="card-header bg-white fw-bold py-3">Access Credentials (Metadata Only &ndash; PIN Hidden)</div>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th class="ps-3">ID</th>
                        <th>Type</th>
                        <th>Device</th>
                        <th>Status</th>
                        <th>Valid From</th>
                        <th>Valid Until</th>
                        <th>Used Count</th>
                        <th>Revoke Info</th>
                        <th class="pe-3 text-end">Action</th>
                    </tr>
                </thead>
                <tbody>
                    <t:if test="${empty detail.credentials}">
                        <tr>
                            <td colspan="9" class="text-center py-3 text-muted">No door credentials generated yet.</td>
                        </tr>
                    </t:if>
                    <t:forEach items="${detail.credentials}" var="cr">
                        <tr>
                            <td class="ps-3 text-muted">#${cr.credentialId}</td>
                            <td>${e:esc(cr.credentialType)}</td>
                            <td><code>${e:esc(cr.deviceId)}</code></td>
                            <td>
                                <t:if test="${cr.status == 'ACTIVE'}">
                                    <span class="badge bg-success">ACTIVE</span>
                                </t:if>
                                <t:if test="${cr.status == 'PENDING'}">
                                    <span class="badge bg-warning text-dark">PENDING</span>
                                </t:if>
                                <t:if test="${cr.status == 'REVOKED'}">
                                    <span class="badge bg-danger">REVOKED</span>
                                </t:if>
                                <t:if test="${cr.status == 'EXPIRED'}">
                                    <span class="badge bg-secondary">EXPIRED</span>
                                </t:if>
                            </td>
                            <td class="small text-muted">${e:datetime(cr.validFrom)}</td>
                            <td class="small text-muted">${e:datetime(cr.validUntil)}</td>
                            <td>${cr.usedCount}</td>
                            <td class="small text-muted">
                                <t:if test="${not empty cr.revokeReason}">
                                    <div>${e:esc(cr.revokeReason)}</div>
                                    <div class="small">${e:datetime(cr.revokedAt)}</div>
                                </t:if>
                            </td>
                            <td class="pe-3 text-end">
                                <t:if test="${(cr.status == 'ACTIVE' or cr.status == 'PENDING') and sessionScope.permissions.contains('CREDENTIAL_REVOKE')}">
                                    <form action="${pageContext.request.contextPath}/admin/credentials/revoke" method="post" class="d-inline m-0"
                                          onsubmit="return confirm('Revoke this door PIN credential immediately? The guest will no longer be able to unlock the door with this PIN.');">
                                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                        <input type="hidden" name="credentialId" value="${cr.credentialId}">
                                        <input type="hidden" name="bookingId" value="${detail.booking.bookingId}">
                                        <button type="submit" class="btn btn-outline-danger btn-sm py-0 px-2">Revoke</button>
                                    </form>
                                </t:if>
                            </td>
                        </tr>
                    </t:forEach>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Recent Access Logs Table -->
    <div class="card shadow-sm border-0 mb-4">
        <div class="card-header bg-white fw-bold py-3">Recent Physical Door Access Logs (Latest 20)</div>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th class="ps-3">Attempted At</th>
                        <th>Device</th>
                        <th>Request ID</th>
                        <th>Result</th>
                        <th class="pe-3">Reason Code</th>
                    </tr>
                </thead>
                <tbody>
                    <t:if test="${empty detail.accessLogs}">
                        <tr>
                            <td colspan="5" class="text-center py-3 text-muted">No access attempts recorded for this booking.</td>
                        </tr>
                    </t:if>
                    <t:forEach items="${detail.accessLogs}" var="al">
                        <tr>
                            <td class="ps-3 small">${e:datetime(al.attemptedAt)}</td>
                            <td><code>${e:esc(al.deviceId)}</code></td>
                            <td class="small text-muted font-monospace">${e:esc(al.requestId)}</td>
                            <td>
                                <t:if test="${al.result == 'GRANTED'}">
                                    <span class="badge bg-success">GRANTED</span>
                                </t:if>
                                <t:if test="${al.result != 'GRANTED'}">
                                    <span class="badge bg-danger">${e:esc(al.result)}</span>
                                </t:if>
                            </td>
                            <td class="pe-3 small text-muted">${e:esc(al.reasonCode)}</td>
                        </tr>
                    </t:forEach>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Related Alerts Table (if any) -->
    <t:if test="${not empty detail.alertLogs}">
        <div class="card shadow-sm border-0 mb-4 border-warning">
            <div class="card-header bg-warning-subtle fw-bold py-3 text-warning-emphasis">Related Security / Operational Alerts</div>
            <div class="table-responsive">
                <table class="table table-sm table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3">Created</th>
                            <th>Alert Type</th>
                            <th>Severity</th>
                            <th>Status</th>
                            <th class="pe-3">Message</th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:forEach items="${detail.alertLogs}" var="alt">
                            <tr>
                                <td class="ps-3 small">${e:datetime(alt.createdAt)}</td>
                                <td><code>${e:esc(alt.alertType)}</code></td>
                                <td>
                                    <span class="badge badge-sev-${alt.severity == 'CRITICAL' ? 'critical' : (alt.severity == 'HIGH' ? 'high' : (alt.severity == 'MEDIUM' ? 'medium' : 'low'))}">${e:esc(alt.severity)}</span>
                                </td>
                                <td><span class="badge ${alt.status == 'NEW' ? 'badge-status-danger' : (alt.status == 'ACKNOWLEDGED' ? 'badge-status-warning' : 'badge-status-success')}">${e:esc(alt.status)}</span></td>
                                <td class="pe-3 small">${e:esc(alt.message)}</td>
                            </tr>
                        </t:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </t:if>

    <!-- Modal: Cancel Booking -->
    <div class="modal fade" id="cancelModal" tabindex="-1" aria-labelledby="cancelModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/bookings/cancel" method="post">
                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                    <input type="hidden" name="id" value="${detail.booking.bookingId}">
                    <div class="modal-header">
                        <h5 class="modal-title" id="cancelModalLabel">Cancel Booking ${e:esc(detail.booking.bookingCode)}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="cancelReason" class="form-label fw-semibold">Cancellation Reason <span class="text-danger">*</span></label>
                            <textarea class="form-control" id="cancelReason" name="reason" rows="3" maxlength="255" required
                                      placeholder="State the reason for cancelling this booking..."></textarea>
                            <div class="form-text">Held slots will be immediately released. Max 255 characters.</div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="submit" class="btn btn-danger">Confirm Cancellation</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal: Reinstate No-Show Booking -->
    <div class="modal fade" id="reinstateModal" tabindex="-1" aria-labelledby="reinstateModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/bookings/noshow" method="post">
                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                    <input type="hidden" name="id" value="${detail.booking.bookingId}">
                    <input type="hidden" name="action" value="reinstate">
                    <div class="modal-header">
                        <h5 class="modal-title" id="reinstateModalLabel">Reinstate No-Show Booking ${e:esc(detail.booking.bookingCode)}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <p class="small text-muted">
                            Reinstating this booking will restore its status to <strong>CONFIRMED</strong> and generate a fresh door PIN credential. Reinstatement is only possible if slots are still held and have not ended.
                        </p>
                        <div class="form-check mb-3">
                            <input class="form-check-input" type="checkbox" id="refundPayment" name="refundPayment" value="true">
                            <label class="form-check-label fw-semibold" for="refundPayment">
                                Mark paid transaction as REFUND_PENDING
                            </label>
                        </div>
                        <div class="mb-3">
                            <label for="paymentNote" class="form-label small fw-semibold">Resolution Note (optional)</label>
                            <input type="text" class="form-control form-control-sm" id="paymentNote" name="paymentNote"
                                   placeholder="Reason for payment refund / reinstatement note..." maxlength="255">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="submit" class="btn btn-primary">Confirm Reinstatement</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Refund Confirmation Modal -->
    <div class="modal fade" id="refundModal" tabindex="-1" aria-labelledby="refundModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/bookings/refund" method="post">
                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                    <input type="hidden" name="bookingId" value="${detail.booking.bookingId}">
                    <input type="hidden" name="returnUrl" value="${pageContext.request.contextPath}/admin/bookings/detail?id=${detail.booking.bookingId}">
                    <div class="modal-header">
                        <h5 class="modal-title" id="refundModalLabel"><i class="bi bi-arrow-counterclockwise me-2 text-warning"></i>Mark Payment as Refunded</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <p>Mark payment as <strong>REFUNDED</strong> for cancelled booking <strong class="text-espresso font-monospace">${e:esc(detail.booking.bookingCode)}</strong>?</p>
                        <div class="mb-3">
                            <label for="refundDetailNote" class="form-label small fw-semibold">Refund resolution note / transfer reference:</label>
                            <input type="text" class="form-control form-control-sm" id="refundDetailNote" name="note" maxlength="255"
                                   value="Bank refund completed for ${e:esc(detail.booking.bookingCode)}">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-warning btn-sm">Confirm Refunded</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</t:layout>
