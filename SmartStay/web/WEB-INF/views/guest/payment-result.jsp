<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Payment Result &bull; SmartStay" active="bookings">
    <div class="container py-3">

        <!-- Stepper: 1 Choose room › 2 Review › 3 Payment › 4 Get PIN (Step 4 active) -->
        <div class="checkout-stepper no-print">
            <div class="checkout-stepper-inner">
                <div class="stepper-step completed">
                    <span class="stepper-num"><i class="bi bi-check-lg"></i></span>
                    <span>1 Choose room</span>
                </div>
                <div class="stepper-divider"></div>
                <div class="stepper-step completed">
                    <span class="stepper-num"><i class="bi bi-check-lg"></i></span>
                    <span>2 Review</span>
                </div>
                <div class="stepper-divider"></div>
                <div class="stepper-step completed">
                    <span class="stepper-num"><i class="bi bi-check-lg"></i></span>
                    <span>3 Payment</span>
                </div>
                <div class="stepper-divider"></div>
                <div class="stepper-step active">
                    <span class="stepper-num">4</span>
                    <span>4 Get PIN</span>
                </div>
            </div>
        </div>

        <div class="row justify-content-center">
            <div class="col-md-9 col-lg-8">

                <!-- Boutique Breadcrumb Navigation -->
                <nav aria-label="breadcrumb" class="mb-3 no-print">
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
                        <li class="breadcrumb-item active" aria-current="page">Payment Result</li>
                    </ol>
                </nav>

                <div class="card shadow-sm border rounded-3 overflow-hidden bg-white">
                    <div class="card-body p-4 p-md-5">

                        <!-- =======================================================
                             1. SUCCESS STATE: PAID or CONFIRMED
                             ======================================================= -->
                        <t:if test="${paymentStatus == 'PAID' || bookingStatus == 'CONFIRMED' || bookingStatus == 'CHECKED_IN'}">
                            <div class="text-center mb-4">
                                <div class="d-inline-flex align-items-center justify-content-center bg-success bg-opacity-10 text-success rounded-circle mb-3" style="width: 72px; height: 72px;">
                                    <i class="bi bi-check-circle-fill fs-1"></i>
                                </div>
                                <h1 class="h3 fw-bold text-dark mb-1">Payment Confirmed!</h1>
                                <p class="text-muted small mb-0">
                                    Your reservation <strong>${e:esc(bookingCode)}</strong> is confirmed and door access credentials have been activated.
                                </p>
                            </div>

                            <!-- Booking Overview Summary -->
                            <div class="card border rounded-3 p-3 mb-4 bg-milk">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <span class="small fw-semibold text-muted text-uppercase" style="letter-spacing: 0.05em;">Reservation Details</span>
                                    <span class="font-monospace fw-bold text-caramel">${e:esc(bookingCode)}</span>
                                </div>
                                <div class="fs-6 fw-bold text-dark mb-1">
                                    ${e:esc(e:roomName(roomNumber))} &middot; Room ${e:esc(roomNumber)}
                                    <span class="badge bg-light text-muted border ms-1 font-monospace">Floor ${floor}</span>
                                </div>
                                <t:if test="${not empty bookingDetail && not empty bookingDetail.booking}">
                                    <t:if test="${not empty bookingDetail.booking.slotDate}">
                                        <div class="small text-muted mb-1">
                                            <i class="bi bi-calendar3 me-1 text-caramel"></i>Date:
                                            <strong>${e:date(bookingDetail.booking.slotDate)}</strong>
                                        </div>
                                    </t:if>
                                    <div class="small text-muted mb-2">
                                        <i class="bi bi-clock me-1 text-caramel"></i>Access window:
                                        <strong>${e:datetime(bookingDetail.booking.checkinAt)}</strong> &ndash; <strong>${e:datetime(bookingDetail.booking.checkoutAt)}</strong>
                                    </div>
                                    <div class="mb-2">
                                        <t:forEach items="${bookingDetail.slots}" var="slot">
                                            <span class="badge border bg-white text-dark me-1 mb-1 font-monospace" style="font-size: 0.75rem;">
                                                ${e:esc(slot.slotCode)} &middot; ${e:esc(slot.timeRange)}
                                            </span>
                                        </t:forEach>
                                    </div>
                                    <div class="d-flex justify-content-between align-items-baseline border-top pt-2 mt-2">
                                        <span class="small fw-semibold text-muted">Total Paid:</span>
                                        <span class="fs-5 fw-bold text-success">${e:money(totalAmount)}</span>
                                    </div>
                                </t:if>
                            </div>

                            <!-- Large PIN Display Card -->
                            <div class="pin-display-box">
                                <div class="small fw-bold text-muted text-uppercase mb-1" style="letter-spacing: 0.1em; font-size: 0.78rem;">
                                    Main Entrance Door PIN
                                </div>

                                <t:if test="${not empty pinDetails}">
                                    <div class="pin-code-large font-monospace" id="doorPinDisplay">
                                        ${pinDetails.formattedPin}
                                    </div>

                                    <div class="d-flex align-items-center justify-content-center gap-2 mt-2">
                                        <button type="button" class="btn btn-sm btn-outline-soft btn-copy-field px-3" data-copy-value="${pinDetails.rawPin}" style="min-height: 38px;">
                                            <i class="bi bi-copy me-1"></i><span>Copy PIN</span>
                                        </button>
                                    </div>

                                    <div class="small text-muted mt-3" style="font-size: 0.8rem;">
                                        <i class="bi bi-shield-check text-success me-1"></i>Valid from
                                        <strong>${e:datetime(pinDetails.validFrom)}</strong> until <strong>${e:datetime(pinDetails.validUntil)}</strong>
                                    </div>
                                </t:if>

                                <t:if test="${empty pinDetails}">
                                    <div class="py-2">
                                        <p class="text-muted small mb-2">Your door PIN has either expired or is being generated.</p>
                                        <p class="small text-muted">A copy of your credentials has also been dispatched to your email address.</p>
                                    </div>
                                </t:if>
                            </div>

                            <!-- 3-Step "How to Enter" Instruction Guide -->
                            <div class="card border rounded-3 p-3 p-md-4 mb-4 bg-light">
                                <h2 class="h6 fw-bold text-dark mb-3 d-flex align-items-center gap-2">
                                    <i class="bi bi-key-fill text-caramel"></i>How to Enter SmartStay
                                </h2>

                                <div class="how-to-enter-list">
                                    <!-- Step 1: Main entrance -->
                                    <div class="how-to-enter-step">
                                        <span class="how-to-step-badge">1</span>
                                        <div>
                                            <div class="how-to-step-title">Main Building Entrance (Ground Floor)</div>
                                            <p class="how-to-step-desc">
                                                Locate the digital access keypad next to the front glass door. Ensure keypad screen lights up when approached.
                                            </p>
                                        </div>
                                    </div>

                                    <!-- Step 2: Keypad input -->
                                    <div class="how-to-enter-step">
                                        <span class="how-to-step-badge">2</span>
                                        <div>
                                            <div class="how-to-step-title">
                                                Enter PIN + <span class="badge bg-secondary font-monospace">#</span>
                                            </div>
                                            <p class="how-to-step-desc">
                                                Type your 6-digit PIN <strong class="text-dark font-monospace">${not empty pinDetails ? pinDetails.rawPin : 'XXXXXX'}</strong> followed by the <strong>#</strong> key. The lock will chime and open for 5 seconds.
                                            </p>
                                        </div>
                                    </div>

                                    <!-- Step 3: Room key retrieval -->
                                    <div class="how-to-enter-step">
                                        <span class="how-to-step-badge">3</span>
                                        <div>
                                            <div class="how-to-step-title">Access Room ${e:esc(roomNumber)} (Floor ${floor})</div>
                                            <p class="how-to-step-desc">
                                                Proceed to Floor ${floor}. The key locker for Room ${e:esc(roomNumber)} is mounted next to the room entrance to collect your physical door key.
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Action Buttons -->
                            <div class="d-flex flex-wrap align-items-center justify-content-center gap-2 no-print">
                                <button type="button" onclick="window.print()" class="btn btn-outline-soft" style="min-height: 44px;">
                                    <i class="bi bi-printer me-1"></i>Print Access Pass
                                </button>
                                <a href="${pageContext.request.contextPath}/guest/bookings/detail?id=${bookingId}" class="btn btn-caramel" style="min-height: 44px;">
                                    <i class="bi bi-receipt me-1"></i>View Booking
                                </a>
                                <a href="${pageContext.request.contextPath}/home" class="btn btn-outline-soft" style="min-height: 44px;">
                                    <i class="bi bi-house me-1"></i>Back to Home
                                </a>
                            </div>
                        </t:if>

                        <!-- =======================================================
                             2. FAILED STATE
                             ======================================================= -->
                        <t:if test="${paymentStatus == 'FAILED'}">
                            <div class="text-center py-4">
                                <div class="d-inline-flex align-items-center justify-content-center bg-danger bg-opacity-10 text-danger rounded-circle mb-3" style="width: 72px; height: 72px;">
                                    <i class="bi bi-x-circle-fill fs-1"></i>
                                </div>
                                <h1 class="h4 fw-bold text-danger mb-2">Payment Unsuccessful</h1>
                                <p class="text-muted small mb-4">
                                    Your payment attempt could not be verified by the banking gateway or was declined.
                                </p>
                                <div class="alert alert-light border small text-muted text-start mb-4 mx-auto" style="max-width: 480px;">
                                    <div class="d-flex justify-content-between mb-1">
                                        <span>Booking Code:</span>
                                        <span class="font-monospace fw-bold text-dark">${e:esc(bookingCode)}</span>
                                    </div>
                                    <div class="d-flex justify-content-between mb-1">
                                        <span>Room:</span>
                                        <span class="fw-semibold text-dark">${e:esc(e:roomName(roomNumber))} (Room ${e:esc(roomNumber)})</span>
                                    </div>
                                    <div class="d-flex justify-content-between">
                                        <span>Amount:</span>
                                        <span class="fw-bold text-dark">${e:money(totalAmount)}</span>
                                    </div>
                                </div>
                                <div class="d-flex flex-wrap justify-content-center gap-2">
                                    <a href="${pageContext.request.contextPath}/guest/bookings/detail?id=${bookingId}" class="btn btn-caramel" style="min-height: 44px;">
                                        <i class="bi bi-arrow-repeat me-1"></i>Try Payment Again
                                    </a>
                                    <a href="${pageContext.request.contextPath}/guest/bookings" class="btn btn-outline-soft" style="min-height: 44px;">
                                        <i class="bi bi-list-ul me-1"></i>My Bookings
                                    </a>
                                </div>
                            </div>
                        </t:if>

                        <!-- =======================================================
                             3. EXPIRED STATE
                             ======================================================= -->
                        <t:if test="${paymentStatus == 'EXPIRED' || bookingStatus == 'EXPIRED'}">
                            <div class="text-center py-4">
                                <div class="d-inline-flex align-items-center justify-content-center bg-warning bg-opacity-10 text-warning rounded-circle mb-3" style="width: 72px; height: 72px;">
                                    <i class="bi bi-hourglass-bottom fs-1"></i>
                                </div>
                                <h1 class="h4 fw-bold text-dark mb-2">Payment Session Expired</h1>
                                <p class="text-muted small mb-4">
                                    The 15-minute reservation hold time expired before payment completion. Your held room slot(s) have been released back to availability.
                                </p>
                                <div class="d-flex flex-wrap justify-content-center gap-2">
                                    <a href="${pageContext.request.contextPath}/rooms" class="btn btn-caramel" style="min-height: 44px;">
                                        <i class="bi bi-search me-1"></i>Search Available Rooms
                                    </a>
                                    <a href="${pageContext.request.contextPath}/guest/bookings" class="btn btn-outline-soft" style="min-height: 44px;">
                                        <i class="bi bi-list-ul me-1"></i>My Bookings
                                    </a>
                                </div>
                            </div>
                        </t:if>

                        <!-- =======================================================
                             4. CANCELLED STATE
                             ======================================================= -->
                        <t:if test="${paymentStatus == 'CANCELLED' || bookingStatus == 'CANCELLED'}">
                            <div class="text-center py-4">
                                <div class="d-inline-flex align-items-center justify-content-center bg-secondary bg-opacity-10 text-secondary rounded-circle mb-3" style="width: 72px; height: 72px;">
                                    <i class="bi bi-slash-circle fs-1"></i>
                                </div>
                                <h1 class="h4 fw-bold text-dark mb-2">Payment Cancelled</h1>
                                <p class="text-muted small mb-4">
                                    You cancelled the checkout process. The temporary reservation for booking <strong>${e:esc(bookingCode)}</strong> has been released.
                                </p>
                                <div class="d-flex flex-wrap justify-content-center gap-2">
                                    <a href="${pageContext.request.contextPath}/rooms" class="btn btn-caramel" style="min-height: 44px;">
                                        <i class="bi bi-search me-1"></i>Find Another Room
                                    </a>
                                    <a href="${pageContext.request.contextPath}/guest/bookings" class="btn btn-outline-soft" style="min-height: 44px;">
                                        <i class="bi bi-list-ul me-1"></i>My Bookings
                                    </a>
                                </div>
                            </div>
                        </t:if>

                        <!-- =======================================================
                             5. REVIEW_REQUIRED STATE (Late payment)
                             ======================================================= -->
                        <t:if test="${paymentStatus == 'REVIEW_REQUIRED'}">
                            <div class="text-center py-4">
                                <div class="d-inline-flex align-items-center justify-content-center bg-warning bg-opacity-10 text-warning rounded-circle mb-3" style="width: 72px; height: 72px;">
                                    <i class="bi bi-exclamation-triangle-fill fs-1"></i>
                                </div>
                                <h1 class="h4 fw-bold text-warning mb-2">Payment Under Review</h1>
                                <p class="text-muted small mb-4">
                                    Your transfer was received after the link expired. Our homestay operations team has been notified to review your reservation and issue your access PIN shortly.
                                </p>
                                <div class="d-flex flex-wrap justify-content-center gap-2">
                                    <a href="${pageContext.request.contextPath}/guest/bookings/detail?id=${bookingId}" class="btn btn-caramel" style="min-height: 44px;">
                                        <i class="bi bi-receipt me-1"></i>View Booking Details
                                    </a>
                                    <a href="${pageContext.request.contextPath}/guest/bookings" class="btn btn-outline-soft" style="min-height: 44px;">
                                        <i class="bi bi-list-ul me-1"></i>My Bookings
                                    </a>
                                </div>
                            </div>
                        </t:if>

                        <!-- =======================================================
                             6. PENDING / VERIFYING STATE
                             ======================================================= -->
                        <t:if test="${paymentStatus != 'PAID' && paymentStatus != 'FAILED' && paymentStatus != 'EXPIRED' && paymentStatus != 'CANCELLED' && paymentStatus != 'REVIEW_REQUIRED' && bookingStatus != 'CONFIRMED' && bookingStatus != 'CHECKED_IN'}">
                            <div id="polling-container" class="text-center py-5">
                                <div class="spinner-border text-caramel my-3" style="width: 3rem; height: 3rem;" role="status">
                                    <span class="visually-hidden">Loading...</span>
                                </div>
                                <h1 class="h4 fw-bold text-dark mb-2">Verifying Payment...</h1>
                                <p class="text-muted small" id="status-hint">
                                    We are awaiting confirmation from the bank transfer gateway for booking <strong>${e:esc(bookingCode)}</strong>. Please do not close this window.
                                </p>
                            </div>
                        </t:if>

                    </div>
                </div>

            </div>
        </div>
    </div>

    <!-- Copy feedback & Realtime Polling Script -->
    <script>
        (function() {
            // Copy buttons feedback
            document.querySelectorAll('.btn-copy-field').forEach(function(btn) {
                btn.addEventListener('click', function(e) {
                    e.preventDefault();
                    var text = btn.getAttribute('data-copy-value') || '';
                    if (navigator.clipboard && navigator.clipboard.writeText) {
                        navigator.clipboard.writeText(text).then(function() {
                            showCopiedFeedback(btn);
                        });
                    } else {
                        var ta = document.createElement('textarea');
                        ta.value = text;
                        ta.style.position = 'fixed';
                        ta.style.opacity = '0';
                        document.body.appendChild(ta);
                        ta.select();
                        document.execCommand('copy');
                        document.body.removeChild(ta);
                        showCopiedFeedback(btn);
                    }
                });
            });

            function showCopiedFeedback(btn) {
                var origHtml = btn.innerHTML;
                btn.innerHTML = '<i class="bi bi-check2 text-success me-1"></i><span class="text-success fw-medium">Copied</span>';
                btn.classList.add('btn-copied');
                setTimeout(function() {
                    btn.innerHTML = origHtml;
                    btn.classList.remove('btn-copied');
                }, 1500);
            }

            // Polling for pending states
            var currentStatus = '${paymentStatus}';
            var bStatus = '${bookingStatus}';
            var terminalStatuses = ['PAID', 'FAILED', 'REVIEW_REQUIRED', 'EXPIRED', 'CANCELLED'];
            var bookingId = ${bookingId};
            var pollUrl = '${pageContext.request.contextPath}/guest/payment/status?bookingId=' + bookingId;

            if (terminalStatuses.indexOf(currentStatus) === -1 && bStatus !== 'CONFIRMED' && bStatus !== 'CHECKED_IN') {
                var interval = setInterval(function() {
                    var xhr = new XMLHttpRequest();
                    xhr.open('GET', pollUrl, true);
                    xhr.setRequestHeader('Cache-Control', 'no-store');
                    xhr.setRequestHeader('Accept', 'application/json');
                    xhr.onreadystatechange = function() {
                        if (xhr.readyState === 4 && xhr.status === 200) {
                            try {
                                var res = JSON.parse(xhr.responseText);
                                if (res && res.status && terminalStatuses.indexOf(res.status) !== -1) {
                                    clearInterval(interval);
                                    window.location.reload();
                                }
                            } catch (e) {}
                        }
                    };
                    xhr.send();
                }, 2000);
            }
        })();
    </script>
</t:layout>
