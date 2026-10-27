<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Booking ${detail.booking.bookingCode}" active="admin-bookings">
    <%-- V3: page anatomy as the other admin pages: one state badge in plain words, one primary
         action for the current step, everything else in the "⋯" menu (forms, modals and ids unchanged). --%>
    <a href="${pageContext.request.contextPath}/admin/bookings" class="ss-back-link"><i class="bi bi-chevron-left" aria-hidden="true"></i> Bookings</a>
    <div class="ss-page-header">
        <div>
            <div class="d-flex align-items-center flex-wrap gap-2">
                <h1 class="ss-page-header__title mb-0">${e:esc(detail.booking.bookingCode)}</h1>
                <span class="ss-badge ss-badge--${e:adminState(detail.booking).tone}">${e:esc(e:adminState(detail.booking).label)}</span>
                <t:if test="${detail.booking.status == 'CHECKED_IN' && detail.booking.earlyCheckin}">
                    <span class="ss-bk__flag"><i class="bi bi-clock-history" aria-hidden="true"></i>Checked in ${detail.booking.earlyCheckinMinutes} min early</span>
                </t:if>
                <t:if test="${detail.booking.status == 'EXPIRED' && not empty detail.booking.expireReason}">
                    <span class="ss-bk__flag">${e:esc(e:humanStatus(detail.booking.expireReason))}</span>
                </t:if>
                <t:if test="${detail.booking.hasHeldSlots}">
                    <span class="ss-bk__flag" title="Slots are still blocked for other guests">Slots held</span>
                </t:if>
            </div>
            <p class="ss-page-header__subtitle">Created ${e:datetime(detail.booking.createdAt)} &middot; ${e:esc(detail.booking.guestName)}</p>
        </div>

        <div class="d-flex flex-wrap align-items-center gap-2">
            <%-- Primary action for the current step --%>
            <t:if test="${detail.booking.status == 'CONFIRMED'}">
                <form action="${pageContext.request.contextPath}/admin/bookings/checkin" method="post" class="d-inline m-0"
                      onsubmit="return confirm('Check in ${e:esc(detail.booking.guestName)} now? The room becomes Occupied.');">
                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                    <input type="hidden" name="id" value="${detail.booking.bookingId}">
                    <button type="submit" class="btn btn-primary btn-sm">Check in</button>
                </form>
            </t:if>
            <t:if test="${detail.booking.status == 'CHECKED_IN'}">
                <form action="${pageContext.request.contextPath}/admin/bookings/checkout" method="post" class="d-inline m-0"
                      onsubmit="return confirm('Check out ${e:esc(detail.booking.guestName)} now? The room needs cleaning and the door PIN stops working.');">
                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                    <input type="hidden" name="id" value="${detail.booking.bookingId}">
                    <button type="submit" class="btn btn-primary btn-sm">Check out</button>
                </form>
            </t:if>
            <t:if test="${detail.booking.status == 'CANCELLED' && detail.booking.paymentStatus == 'REFUND_PENDING'}">
                <button type="button" class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#refundModal">Mark refunded</button>
            </t:if>
            <t:if test="${detail.booking.status == 'EXPIRED' and detail.booking.expireReason == 'NO_SHOW' and sessionScope.permissions.contains('NOSHOW_INTERVENE')}">
                <form action="${pageContext.request.contextPath}/admin/bookings/noshow" method="post" class="d-inline m-0"
                      onsubmit="return confirm('Release the held slots of this no-show so other guests can book them?');">
                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                    <input type="hidden" name="id" value="${detail.booking.bookingId}">
                    <input type="hidden" name="action" value="release">
                    <button type="submit" class="btn btn-primary btn-sm">Release slots</button>
                </form>
            </t:if>

            <%-- Secondary actions --%>
            <t:if test="${detail.booking.status == 'PENDING_PAYMENT' or detail.booking.status == 'CONFIRMED' or (detail.booking.status == 'EXPIRED' and detail.booking.expireReason == 'NO_SHOW' and sessionScope.permissions.contains('NOSHOW_INTERVENE'))}">
                <div class="dropdown ss-row-menu">
                    <button class="btn-trigger" type="button" data-bs-toggle="dropdown" aria-expanded="false" aria-label="More actions for this booking">
                        <i class="bi bi-three-dots" aria-hidden="true"></i>
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end">
                        <t:if test="${detail.booking.status == 'PENDING_PAYMENT' or detail.booking.status == 'CONFIRMED'}">
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/bookings/edit?id=${detail.booking.bookingId}">${detail.booking.status == 'CONFIRMED' ? 'Edit note' : 'Edit booking'}</a></li>
                        </t:if>
                        <t:if test="${detail.booking.status == 'CONFIRMED'}">
                            <li>
                                <form action="${pageContext.request.contextPath}/admin/bookings/early-checkin" method="post" class="m-0">
                                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                    <input type="hidden" name="id" value="${detail.booking.bookingId}">
                                    <input type="hidden" name="allow" value="${detail.booking.allowEarlyCheckin ? 'false' : 'true'}">
                                    <button type="submit" class="dropdown-item">${detail.booking.allowEarlyCheckin ? 'Turn off early check-in' : 'Allow early check-in'}</button>
                                </form>
                            </li>
                        </t:if>
                        <t:if test="${detail.booking.status == 'EXPIRED' and detail.booking.expireReason == 'NO_SHOW' and sessionScope.permissions.contains('NOSHOW_INTERVENE')}">
                            <li><button type="button" class="dropdown-item" data-bs-toggle="modal" data-bs-target="#reinstateModal">Reinstate booking</button></li>
                        </t:if>
                        <t:if test="${detail.booking.status == 'PENDING_PAYMENT' or detail.booking.status == 'CONFIRMED'}">
                            <li><hr class="dropdown-divider"></li>
                            <li><button type="button" class="dropdown-item dropdown-item--destructive" data-bs-toggle="modal" data-bs-target="#cancelModal">Cancel booking</button></li>
                        </t:if>
                    </ul>
                </div>
            </t:if>
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
                        <span class="badge bg-light text-secondary border ms-1">Floor ${detail.room.floorNumber}</span>
                        <span class="badge bg-light text-secondary border ms-1">${e:esc(e:humanStatus(detail.room.status))}</span>
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
                    <%-- 48-hour cancellation policy (R3): deadline + refund outcome --%>
                    <t:if test="${e:cancellationInfo(detail).known}">
                        <div class="mb-2 small ss-cancel-admin">
                            <div class="d-flex justify-content-between gap-2">
                                <span class="text-muted">Free cancellation until</span>
                                <span class="fw-semibold">${e:datetime(e:cancellationInfo(detail).deadline)}</span>
                            </div>
                            <div class="d-flex justify-content-between gap-2">
                                <span class="text-muted">Refund outcome</span>
                                <span class="fw-semibold">
                                    <t:if test="${detail.booking.status == 'CANCELLED'}">
                                        <t:if test="${detail.booking.paymentStatus == 'REFUND_PENDING'}">Full refund, pending</t:if>
                                        <t:if test="${detail.booking.paymentStatus == 'REFUNDED'}">Refunded in full</t:if>
                                        <t:if test="${detail.booking.paymentStatus == 'PAID'}">No refund (cancelled after the deadline)</t:if>
                                        <t:if test="${detail.booking.paymentStatus != 'REFUND_PENDING' && detail.booking.paymentStatus != 'REFUNDED' && detail.booking.paymentStatus != 'PAID'}">Nothing was paid</t:if>
                                    </t:if>
                                    <t:if test="${detail.booking.status == 'PENDING_PAYMENT' || detail.booking.status == 'CONFIRMED'}">
                                        <t:if test="${e:cancellationInfo(detail).outcome == 'FULL_REFUND'}">Guest cancels now: full refund</t:if>
                                        <t:if test="${e:cancellationInfo(detail).outcome == 'NO_REFUND'}">Guest cancels now: no refund</t:if>
                                        <t:if test="${e:cancellationInfo(detail).outcome == 'NOTHING_PAID'}">Not paid yet</t:if>
                                    </t:if>
                                    <t:if test="${detail.booking.status != 'CANCELLED' && detail.booking.status != 'PENDING_PAYMENT' && detail.booking.status != 'CONFIRMED'}">Not cancellable</t:if>
                                </span>
                            </div>
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
                                    <span class="ss-badge ss-badge--warning">Held</span>
                                </t:if>
                                <t:if test="${slot.holdStatus == 'RELEASED'}">
                                    <span class="ss-badge ss-badge--neutral">Released</span>
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
                            <td>${pm.provider == 'MOCK' ? 'Sandbox' : e:esc(pm.provider)}</td>
                            <td>${e:money(pm.amount)}</td>
                            <td>
                                <t:if test="${pm.status == 'PAID'}">
                                    <span class="ss-badge ss-badge--success">Paid</span>
                                </t:if>
                                <t:if test="${pm.status == 'PENDING'}">
                                    <span class="ss-badge ss-badge--warning">Waiting</span>
                                </t:if>
                                <t:if test="${pm.status == 'REVIEW_REQUIRED'}">
                                    <span class="ss-badge ss-badge--danger">Needs review</span>
                                </t:if>
                                <t:if test="${pm.status == 'REFUND_PENDING'}">
                                    <span class="ss-badge ss-badge--warning">Refund pending</span>
                                </t:if>
                                <t:if test="${pm.status == 'REFUNDED'}">
                                    <span class="ss-badge ss-badge--neutral">Refunded</span>
                                </t:if>
                                <t:if test="${pm.status == 'FAILED'}">
                                    <span class="ss-badge ss-badge--danger">Failed</span>
                                </t:if>
                                <t:if test="${pm.status == 'EXPIRED'}">
                                    <span class="ss-badge ss-badge--neutral">Expired</span>
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
                            <td>${e:esc(e:humanStatus(cr.credentialType))}</td>
                            <td><code>${e:esc(cr.deviceId)}</code></td>
                            <td>
                                <t:if test="${cr.status == 'ACTIVE'}">
                                    <span class="ss-badge ss-badge--success">Active</span>
                                </t:if>
                                <t:if test="${cr.status == 'PENDING'}">
                                    <span class="ss-badge ss-badge--warning">Waiting</span>
                                </t:if>
                                <t:if test="${cr.status == 'REVOKED'}">
                                    <span class="ss-badge ss-badge--danger">Revoked</span>
                                </t:if>
                                <t:if test="${cr.status == 'EXPIRED'}">
                                    <span class="ss-badge ss-badge--neutral">Expired</span>
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
                                    <span class="ss-badge ss-badge--success">Allowed</span>
                                </t:if>
                                <t:if test="${al.result != 'GRANTED'}">
                                    <span class="ss-badge ss-badge--danger">${e:esc(e:humanStatus(al.result))}</span>
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
                                <td>${e:esc(e:alertTitle(alt.alertType))} <code class="small text-muted">${e:esc(alt.alertType)}</code></td>
                                <td>
                                    <span class="badge badge-sev-${alt.severity == 'CRITICAL' ? 'critical' : (alt.severity == 'HIGH' ? 'high' : (alt.severity == 'MEDIUM' ? 'medium' : 'low'))}">${e:esc(e:humanStatus(alt.severity))}</span>
                                </td>
                                <td><span class="badge ${alt.status == 'NEW' ? 'badge-status-danger' : (alt.status == 'ACKNOWLEDGED' ? 'badge-status-warning' : 'badge-status-success')}">${e:esc(e:humanStatus(alt.status))}</span></td>
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
    <script>
        // R7: the bookings list "Cancel booking" menu item opens this page with ?cancel=1
        document.addEventListener('DOMContentLoaded', function () {
            if (/[?&]cancel=1(&|$)/.test(window.location.search)) {
                var el = document.getElementById('cancelModal');
                if (el && window.bootstrap) { new bootstrap.Modal(el).show(); }
            }
        });
    </script>
</t:layout>
