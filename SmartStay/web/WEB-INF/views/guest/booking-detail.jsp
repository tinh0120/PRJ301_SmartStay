<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Booking ${detail.booking.bookingCode}" active="bookings">
    <!-- Breadcrumb & Header Row (Same-row title & human-readable status badge) -->
    <div class="mb-4">
        <nav aria-label="breadcrumb" class="mb-2">
            <ol class="breadcrumb boutique-breadcrumb m-0">
                <li class="breadcrumb-item">
                    <a href="${pageContext.request.contextPath}/" class="boutique-breadcrumb-link">
                        <i class="bi bi-house-door me-1"></i>Home
                    </a>
                </li>
                <li class="breadcrumb-item">
                    <a href="${pageContext.request.contextPath}/guest/bookings" class="boutique-breadcrumb-link">
                        My bookings
                    </a>
                </li>
                <li class="breadcrumb-item active" aria-current="page">${e:esc(detail.booking.bookingCode)}</li>
            </ol>
        </nav>
        <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
            <h1 class="h3 fw-bold mb-0">Booking <span class="font-monospace text-primary">${e:esc(detail.booking.bookingCode)}</span></h1>
            <div class="d-flex align-items-center gap-2">
                <t:if test="${detail.booking.status == 'CHECKED_IN' && detail.booking.earlyCheckin}">
                    <span class="badge-status status-CHECKED_IN d-inline-flex align-items-center gap-1" data-status="CHECKED_IN">
                        <i class="bi bi-clock-history"></i> CHECKED_IN &middot; Early Check-in (${detail.booking.earlyCheckinMinutes} mins)
                    </span>
                </t:if>
                <t:if test="${!(detail.booking.status == 'CHECKED_IN' && detail.booking.earlyCheckin)}">
                    <span class="badge-status status-${detail.booking.status}" data-status="${detail.booking.status}">
                        ${e:humanStatus(detail.booking.status)}
                    </span>
                </t:if>
                <t:if test="${detail.booking.paymentStatus != 'NONE' && not empty detail.booking.paymentStatus && detail.booking.paymentStatus != detail.booking.status}">
                    <span class="badge-status status-${detail.booking.paymentStatus}" data-status="${detail.booking.paymentStatus}">
                        ${e:humanStatus(detail.booking.paymentStatus)}
                    </span>
                </t:if>
            </div>
        </div>
    </div>

    <!-- Payment Pending Banner (No Pay Now CTA, Live mm:ss countdown, Expiry transition at 0) -->
    <t:if test="${detail.booking.status == 'PENDING_PAYMENT'}">
        <div id="paymentPendingBanner"
             class="alert alert-warning booking-pending-banner d-flex align-items-center justify-content-between flex-wrap gap-2 mb-4 order-mob-banner"
             role="alert"
             aria-live="polite"
             data-deadline="${detail.booking.paymentDeadline}">
            <div class="d-flex align-items-center gap-3">
                <i class="bi bi-clock-history fs-4 text-warning" id="bannerClockIcon"></i>
                <div>
                    <strong>Payment Pending:</strong> Complete payment within
                    <span id="countdownTimer" class="font-monospace fw-bold fs-6">${remainingMinutes} minute<t:if test="${remainingMinutes != 1}">s</t:if></span>
                    (before <span id="bannerExpiryText" class="fw-semibold">${e:datetime(detail.booking.paymentDeadline)}</span>)
                    or the hold expires and the slots are released.
                </div>
            </div>
        </div>
    </t:if>

    <!-- Desktop 2-column Grid (~65% / 35%) with Mobile Reordering -->
    <div class="booking-detail-grid">

        <!-- Main Column (Left ~65%): Details, Slots, Guide, PIN, Review -->
        <div class="booking-col-main">

            <!-- 1. Reservation Details Card -->
            <div class="card shadow-sm border-0 booking-card order-mob-details">
                <div class="card-header bg-white border-bottom py-3">
                    <h2 class="h5 fw-bold mb-0">Reservation Details</h2>
                </div>
                <div class="card-body p-4">
                    <dl class="row g-3 mb-0 reservation-dl">
                        <dt class="col-sm-4 text-muted fw-normal">Room</dt>
                        <dd class="col-sm-8 mb-2">
                            <div class="fw-bold fs-6 text-dark">${e:esc(detail.room.displayTitle)}</div>
                            <div class="text-muted small">
                                <t:if test="${not empty detail.room.locationDescription}">
                                    ${e:esc(detail.room.locationDescription)}
                                </t:if>
                                <t:if test="${empty detail.room.locationDescription}">
                                    Room ${e:esc(detail.room.roomNumber)} &middot; Floor ${detail.room.floorNumber}
                                </t:if>
                            </div>
                        </dd>

                        <dt class="col-sm-4 text-muted fw-normal">Check-in</dt>
                        <dd class="col-sm-8 mb-2 fw-semibold text-dark">
                            ${e:weekdayDatetime(detail.booking.checkinAt)}
                        </dd>

                        <dt class="col-sm-4 text-muted fw-normal">Check-out</dt>
                        <dd class="col-sm-8 mb-2 fw-semibold text-dark">
                            ${e:weekdayDatetime(detail.booking.checkoutAt)}
                        </dd>

                        <dt class="col-sm-4 text-muted fw-normal">Duration</dt>
                        <dd class="col-sm-8 mb-2 fw-semibold text-dark">
                            ${e:duration(detail.booking.checkinAt, detail.booking.checkoutAt)}
                        </dd>

                        <t:if test="${not empty detail.booking.note}">
                            <dt class="col-sm-4 text-muted fw-normal">Special Request</dt>
                            <dd class="col-sm-8 mb-2">${e:esc(detail.booking.note)}</dd>
                        </t:if>

                        <t:if test="${not empty detail.booking.cancelReason}">
                            <dt class="col-sm-4 text-danger fw-normal">Cancellation Reason</dt>
                            <dd class="col-sm-8 mb-2 text-danger">${e:esc(detail.booking.cancelReason)}</dd>
                        </t:if>

                        <t:if test="${not empty detail.booking.expireReason}">
                            <dt class="col-sm-4 text-muted fw-normal">Expiry Reason</dt>
                            <dd class="col-sm-8 mb-2 text-muted">${e:esc(detail.booking.expireReason)}</dd>
                        </t:if>
                    </dl>
                    <div class="border-top pt-3 mt-3 text-muted small">
                        Created on ${e:datetime(detail.booking.createdAt)}
                    </div>
                </div>
            </div>

            <!-- 2. Reserved Time Slots Card (Table on desktop, stacked cards on mobile) -->
            <div class="card shadow-sm border-0 booking-card order-mob-slots">
                <div class="card-header bg-white border-bottom py-3">
                    <h2 class="h5 fw-bold mb-0">Reserved Time Slots</h2>
                </div>
                <!-- Desktop Table View -->
                <div class="card-body p-0 d-none d-md-block">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th scope="col" style="width: 110px;">Slot</th>
                                    <th scope="col">Name</th>
                                    <th scope="col">Time Range</th>
                                    <th scope="col" class="text-end" style="width: 140px;">Price</th>
                                    <th scope="col" class="text-center" style="width: 130px;">Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                <t:forEach items="${detail.slots}" var="s">
                                    <tr>
                                        <td class="font-monospace fw-bold text-dark">${e:esc(s.slotCode)}</td>
                                        <td>${e:esc(s.slotName)}</td>
                                        <td class="text-dark">${e:esc(s.timeRange)}</td>
                                        <td class="text-end fw-semibold">${e:money(s.listPrice)}</td>
                                        <td class="text-center">
                                            <span class="badge-status status-${s.holdStatus}">${e:humanStatus(s.holdStatus)}</span>
                                        </td>
                                    </tr>
                                </t:forEach>
                            </tbody>
                            <tfoot>
                                <tr class="table-light fw-bold border-top">
                                    <td colspan="3" class="text-end text-muted">Total:</td>
                                    <td class="text-end fs-6 text-primary">${e:money(detail.booking.total)}</td>
                                    <td></td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                </div>
                <!-- Mobile Stacked Card View -->
                <div class="card-body p-3 d-md-none">
                    <div class="d-flex flex-column gap-3">
                        <t:forEach items="${detail.slots}" var="s">
                            <div class="p-3 bg-light rounded-3 border">
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <span class="font-monospace fw-bold text-dark">${e:esc(s.slotCode)} &middot; ${e:esc(s.slotName)}</span>
                                    <span class="badge-status status-${s.holdStatus}">${e:humanStatus(s.holdStatus)}</span>
                                </div>
                                <div class="d-flex justify-content-between align-items-center text-muted small">
                                    <span class="text-dark"><i class="bi bi-clock me-1 text-muted"></i>${e:esc(s.timeRange)}</span>
                                    <span class="fw-bold text-dark fs-6">${e:money(s.listPrice)}</span>
                                </div>
                            </div>
                        </t:forEach>
                        <div class="p-3 bg-white rounded-3 border d-flex justify-content-between align-items-center">
                            <span class="fw-bold text-muted">Total Amount:</span>
                            <span class="fw-bold text-primary fs-6">${e:money(detail.booking.total)}</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 3. Check-in Guide Card (Locked when pending payment, full when confirmed) -->
            <div class="card shadow-sm border-0 booking-card order-mob-guide">
                <div class="card-header bg-white border-bottom py-3 d-flex align-items-center justify-content-between">
                    <h2 class="h5 fw-bold mb-0">Check-in Guide</h2>
                    <t:if test="${detail.booking.status == 'PENDING_PAYMENT'}">
                        <span class="badge bg-light text-muted border"><i class="bi bi-lock-fill me-1"></i>Locked</span>
                    </t:if>
                </div>
                <div class="card-body p-4">
                    <t:if test="${detail.booking.status == 'PENDING_PAYMENT'}">
                        <!-- Locked state for pending payment -->
                        <div class="alert alert-light border d-flex align-items-center gap-3 mb-0" role="status">
                            <div class="fs-4 text-muted"><i class="bi bi-lock-fill"></i></div>
                            <div>
                                <div class="fw-semibold text-dark">Check-in instructions locked</div>
                                <div class="text-muted small">Detailed check-in instructions will be available after payment.</div>
                            </div>
                        </div>
                    </t:if>

                    <t:if test="${detail.booking.status != 'PENDING_PAYMENT'}">
                        <!-- Full check-in instructions for confirmed bookings -->
                        <div class="row g-3 mb-3">
                            <div class="col-sm-6">
                                <span class="text-muted small fw-medium">Assigned Room:</span>
                                <div class="fw-bold text-dark fs-6">${e:esc(detail.room.displayTitle)}</div>
                                <div class="text-muted small">Room ${e:esc(detail.room.roomNumber)} &middot; Floor ${detail.room.floorNumber}</div>
                            </div>
                            <div class="col-sm-6">
                                <span class="text-muted small fw-medium">Location / Directions:</span>
                                <div class="text-dark small">${e:esc(detail.room.locationDescription)}</div>
                            </div>
                        </div>

                        <!-- Restyled Neutral Access Instructions Info Box -->
                        <div class="mb-3">
                            <span class="text-muted small fw-medium d-block mb-1">Access Instructions:</span>
                            <div class="bg-light p-3 rounded-3 border small">
                                <div class="d-flex align-items-start gap-2">
                                    <i class="bi bi-info-circle text-primary mt-1"></i>
                                    <div class="text-dark">${e:esc(areaInstruction)}</div>
                                </div>
                            </div>
                        </div>

                        <!-- Restyled Amber Warning Box -->
                        <div class="alert alert-warning border small d-flex align-items-start gap-2 mb-0" role="note"
                             style="background-color: #FFFDF0; border-color: #FFE082; color: #8D5B00;">
                            <i class="bi bi-exclamation-triangle-fill text-warning mt-1 fs-6"></i>
                            <div>${e:esc(disclaimer)}</div>
                        </div>
                    </t:if>
                </div>
            </div>

            <!-- Smart Entrance PIN Section (When credentials available) -->
            <t:if test="${credential != null}">
                <div class="card shadow-sm border-0 booking-card order-mob-pin">
                    <div class="card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center">
                        <h2 class="h5 fw-bold mb-0">Main Entrance Access PIN</h2>
                        <span class="badge-status status-${credential.status}">${e:humanStatus(credential.status)}</span>
                    </div>
                    <div class="card-body text-center p-4">
                        <t:if test="${not empty shownPin}">
                            <div class="mb-2 text-muted small">Your Smart Lock Access PIN:</div>
                            <div class="display-4 font-monospace fw-bold text-primary my-2 tracking-wide">${e:esc(shownPin)}</div>
                            <div class="small text-muted mb-2">
                                Valid from <strong>${e:datetime(pinValidFrom)}</strong> to <strong>${e:datetime(pinValidUntil)}</strong>
                            </div>
                            <div class="text-secondary small">
                                Valid for the whole stay, including gaps between your slots.
                            </div>
                        </t:if>

                        <t:if test="${empty shownPin && pinAvailable}">
                            <p class="text-muted small mb-3">
                                Your smart lock PIN has been issued. Click below to reveal your entry PIN code.
                            </p>
                            <form action="${pageContext.request.contextPath}/guest/bookings/pin" method="post" class="d-inline">
                                <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                <input type="hidden" name="credentialId" value="${credential.credentialId}">
                                <button type="submit" class="btn btn-primary px-4 fw-bold btn-touch-target">
                                    Show my PIN
                                </button>
                            </form>
                        </t:if>

                        <t:if test="${credential.status == 'EXPIRED'}">
                            <div class="alert alert-secondary py-2 mb-0" role="status">
                                This PIN has expired.
                            </div>
                        </t:if>

                        <t:if test="${credential.status == 'REVOKED'}">
                            <div class="alert alert-danger py-2 mb-0" role="status">
                                This PIN has been revoked.
                            </div>
                        </t:if>
                    </div>
                </div>
            </t:if>

            <!-- 4. Guest Review Card (Only render when completed/checked-out or review submitted) -->
            <t:if test="${detail.booking.status == 'CHECKED_OUT' || detail.booking.status == 'COMPLETED' || userReview != null || canReview}">
                <div class="card shadow-sm border-0 booking-card order-mob-review">
                    <div class="card-header bg-white border-bottom py-3">
                        <h2 class="h5 fw-bold mb-0">Guest Review</h2>
                    </div>
                    <div class="card-body p-4">
                        <t:if test="${userReview != null}">
                            <div class="d-flex align-items-center mb-2">
                                <div class="text-warning fs-5 me-2">
                                    <t:if test="${userReview.rating >= 1}">&#9733;</t:if><t:if test="${userReview.rating >= 2}">&#9733;</t:if><t:if test="${userReview.rating >= 3}">&#9733;</t:if><t:if test="${userReview.rating >= 4}">&#9733;</t:if><t:if test="${userReview.rating >= 5}">&#9733;</t:if>
                                </div>
                                <span class="fw-bold">${userReview.rating} / 5</span>
                                <t:if test="${userReview.status == 'HIDDEN'}">
                                    <span class="badge bg-secondary ms-2">Hidden by moderator</span>
                                </t:if>
                            </div>
                            <t:if test="${not empty userReview.comment}">
                                <p class="mb-2 text-dark">${e:esc(userReview.comment)}</p>
                            </t:if>
                            <div class="text-muted small">Submitted on ${e:datetime(userReview.createdAt)}</div>
                        </t:if>

                        <t:if test="${userReview == null && canReview}">
                            <t:if test="${not empty commentError}">
                                <div class="alert alert-danger py-2 mb-3" role="alert">${e:esc(commentError)}</div>
                            </t:if>
                            <form action="${pageContext.request.contextPath}/guest/reviews/create" method="post" class="needs-validation">
                                <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                <input type="hidden" name="bookingId" value="${detail.booking.bookingId}">

                                <div class="mb-3">
                                    <label for="rating" class="form-label small fw-semibold">Overall Rating</label>
                                    <select class="form-select form-select-sm" id="rating" name="rating" required style="max-width: 200px;">
                                        <option value="5" ${enteredRating == 5 || empty enteredRating ? 'selected' : ''}>5 - Excellent (&#9733;&#9733;&#9733;&#9733;&#9733;)</option>
                                        <option value="4" ${enteredRating == 4 ? 'selected' : ''}>4 - Very Good (&#9733;&#9733;&#9733;&#9733;)</option>
                                        <option value="3" ${enteredRating == 3 ? 'selected' : ''}>3 - Good (&#9733;&#9733;&#9733;)</option>
                                        <option value="2" ${enteredRating == 2 ? 'selected' : ''}>2 - Fair (&#9733;&#9733;)</option>
                                        <option value="1" ${enteredRating == 1 ? 'selected' : ''}>1 - Poor (&#9733;)</option>
                                    </select>
                                </div>

                                <div class="mb-3">
                                    <label for="comment" class="form-label small fw-semibold">Your Review Comment (optional, max 1000 characters)</label>
                                    <textarea class="form-control form-control-sm" id="comment" name="comment" rows="3" maxlength="1000"
                                              placeholder="Tell future guests about your stay...">${e:esc(enteredComment)}</textarea>
                                </div>

                                <button type="submit" class="btn btn-primary btn-sm px-4 btn-touch-target">
                                    Submit Review
                                </button>
                            </form>
                        </t:if>
                    </div>
                </div>
            </t:if>

        </div>

        <!-- Right Column (Sticky 24px top offset): Payment Summary, Need Help, Cancel Booking -->
        <div class="booking-col-side">

            <!-- 1. Payment Summary Card (Primary Action Card) -->
            <div class="card shadow-sm border-0 booking-card order-mob-summary">
                <div class="card-header bg-white border-bottom py-3">
                    <h2 class="h5 fw-bold mb-0">Payment Summary</h2>
                </div>
                <div class="card-body p-4">
                    <div class="d-flex justify-content-between mb-2">
                        <span class="text-muted">Subtotal</span>
                        <span class="fw-semibold text-dark">${e:money(detail.booking.subtotal)}</span>
                    </div>

                    <t:if test="${detail.booking.discountAmount > 0}">
                        <div class="d-flex justify-content-between mb-2">
                            <span class="text-muted">Discount (${detail.booking.discountPercent}%)</span>
                            <span class="text-success fw-semibold">&minus;${e:money(detail.booking.discountAmount)}</span>
                        </div>
                    </t:if>

                    <hr class="my-3">

                    <div class="d-flex justify-content-between align-items-baseline mb-3">
                        <span class="fw-bold fs-6">Total Amount</span>
                        <span class="booking-total-text">${e:money(detail.booking.total)}</span>
                    </div>

                    <t:if test="${detail.booking.status == 'PENDING_PAYMENT'}">
                        <!-- Single Primary Payment CTA on desktop -->
                        <form action="${pageContext.request.contextPath}/guest/bookings/pay" method="post" class="mt-2 m-0" id="desktopPaymentForm">
                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                            <input type="hidden" name="id" value="${detail.booking.bookingId}">
                            <button type="submit" class="btn btn-primary w-100 py-2 fw-bold btn-pay-action">
                                Proceed to Payment
                            </button>
                        </form>
                        <div class="text-muted small text-center mt-2 slot-held-until-container">
                            Your slots are held until <span class="fw-semibold slot-held-until">${e:datetime(detail.booking.paymentDeadline)}</span>
                        </div>
                    </t:if>
                </div>
            </div>

            <!-- 2. Need Help? Card -->
            <div class="card shadow-sm border-0 booking-card bg-light order-mob-help">
                <div class="card-body p-4">
                    <h3 class="h6 fw-bold mb-2 text-dark d-flex align-items-center">
                        <i class="bi bi-question-circle text-primary me-2"></i>Need Help?
                    </h3>
                    <p class="text-muted small mb-0">
                        If you have questions about your stay or encounter issues with your booking, please contact the front desk or support team.
                    </p>
                </div>
            </div>

            <!-- 3. Cancel Booking Card (Low-emphasis, collapsed by default, modal confirmation) -->
            <t:if test="${detail.canCancel}">
                <div class="card shadow-sm border-0 booking-card order-mob-cancel">
                    <div class="card-body p-3 text-center">
                        <button type="button" class="btn btn-link text-decoration-none text-muted p-0 small"
                                data-bs-toggle="collapse" data-bs-target="#cancelBookingCollapse"
                                aria-expanded="false" aria-controls="cancelBookingCollapse" id="toggleCancelBtn">
                            <i class="bi bi-x-circle me-1"></i>Cancel this booking
                        </button>
                        <div class="collapse mt-3 text-start border-top pt-3" id="cancelBookingCollapse">
                            <p class="text-muted small mb-2">
                                You can cancel this booking and release your reserved time slots before the first slot starts.
                            </p>
                            <div class="mb-3">
                                <label for="cancelReasonInput" class="form-label small fw-semibold text-muted">Cancellation reason (optional)</label>
                                <textarea class="form-control form-control-sm" id="cancelReasonInput" rows="2" maxlength="255"
                                          placeholder="Please let us know why you are cancelling..."></textarea>
                            </div>
                            <button type="button" class="btn btn-outline-danger btn-sm w-100 btn-touch-target"
                                    data-bs-toggle="modal" data-bs-target="#cancelModal">
                                <i class="bi bi-x-circle me-1"></i>Cancel booking
                            </button>
                        </div>
                    </div>
                </div>
            </t:if>

        </div>

    </div>

    <!-- Cancel Booking Confirmation Modal (Outside sticky grid to prevent z-index/backdrop trapping) -->
    <t:if test="${detail.canCancel}">
        <div class="modal fade" id="cancelModal" tabindex="-1" aria-labelledby="cancelModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow">
                    <div class="modal-header border-bottom py-3">
                        <h5 class="modal-title fw-bold" id="cancelModalLabel">Cancel Booking</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body py-4">
                        <p class="mb-0">
                            Cancel booking <strong>${e:esc(detail.booking.bookingCode)}</strong>? Your reserved slots will be released.
                        </p>
                    </div>
                    <div class="modal-footer border-top bg-light py-2 d-flex justify-content-end gap-2">
                        <button type="button" class="btn btn-light px-3 btn-touch-target" data-bs-dismiss="modal">
                            Keep booking
                        </button>
                        <form action="${pageContext.request.contextPath}/guest/bookings/cancel" method="post" id="cancelBookingForm" class="m-0">
                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                            <input type="hidden" name="id" value="${detail.booking.bookingId}">
                            <input type="hidden" name="reason" id="modalCancelReason" value="">
                            <button type="submit" class="btn btn-outline-danger px-3 btn-touch-target">
                                Yes, cancel
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </t:if>

    <!-- Fixed Mobile Bottom Bar (Only on mobile < 768px when PENDING_PAYMENT) -->
    <t:if test="${detail.booking.status == 'PENDING_PAYMENT'}">
        <div class="d-md-none booking-mobile-paybar">
            <div class="container p-0 d-flex align-items-center justify-content-between gap-3">
                <div>
                    <div class="small text-muted">Total Amount</div>
                    <div class="fw-bold fs-5" style="color: var(--color-caramel);">${e:money(detail.booking.total)}</div>
                </div>
                <form action="${pageContext.request.contextPath}/guest/bookings/pay" method="post" class="m-0 flex-grow-1 text-end" id="mobilePaymentForm">
                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                    <input type="hidden" name="id" value="${detail.booking.bookingId}">
                    <button type="submit" class="btn btn-primary px-4 py-2 fw-bold w-100 btn-pay-action">
                        Pay now
                    </button>
                </form>
            </div>
        </div>
        <script>
            document.body.classList.add('has-mobile-paybar');
        </script>
    </t:if>

    <!-- Client-side Booking Detail Utilities & Live Countdown Hook -->
    <script src="${pageContext.request.contextPath}/assets/js/booking-detail.js"></script>
</t:layout>
