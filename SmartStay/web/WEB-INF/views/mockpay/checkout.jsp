<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Payment &bull; SmartStay" active="bookings">
    <div class="container py-3">

        <!-- Stepper: 1 Choose room › 2 Review › 3 Payment › 4 Get PIN -->
        <div class="checkout-stepper">
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
                <div class="stepper-step active">
                    <span class="stepper-num">3</span>
                    <span>3 Payment</span>
                </div>
                <div class="stepper-divider"></div>
                <div class="stepper-step upcoming">
                    <span class="stepper-num">4</span>
                    <span>4 Get PIN</span>
                </div>
            </div>
        </div>

        <!-- Main Checkout Single Card -->
        <div class="mockpay-card ss-checkout">

            <!-- Slim Top Bar -->
            <div class="mockpay-topbar d-flex align-items-center justify-content-between flex-wrap gap-2">
                <div class="d-flex align-items-center gap-2">
                    <span class="mockpay-brand-navy fw-bold fs-5">VietQR &middot; NAPAS 247 Gateway</span>
                    <span class="badge d-inline-flex align-items-center" style="background:#EBF3FF; color:#005baa; font-size:0.75rem; font-weight:600; border: 1px solid #C9E0FF;">
                        <i class="bi bi-patch-check-fill me-1 text-primary"></i>Automated Gateway
                    </span>
                </div>
                <%-- V4: the "256-Bit SSL Encrypted" badge was removed: decorative, off-palette, and not true for the local sandbox --%>
            </div>

            <!-- Error State: Missing or Invalid Payment Session -->
            <t:if test="${empty orderCode || empty bookingCode}">
                <div class="p-5 text-center">
                    <i class="bi bi-exclamation-circle text-danger fs-1 mb-3 d-block"></i>
                    <h2 class="h5 text-dark">This payment link has expired</h2>
                    <p class="text-muted small mb-4">This transaction link has either expired, already completed, or is invalid.</p>
                    <a href="${pageContext.request.contextPath}/guest/bookings" class="btn btn-outline-soft px-4">
                        <i class="bi bi-arrow-left me-1"></i>Back to My Bookings
                    </a>
                </div>
            </t:if>

            <t:if test="${not empty orderCode && not empty bookingCode}">
                <!-- Mobile Sticky Header: Amount + Countdown -->
                <div class="mobile-checkout-sticky d-md-none ss-paybar-m" data-ss-demo-avoid>
                    <div>
                        <div class="mockpay-amount-label mb-0" style="font-size:0.68rem;">Amount Due</div>
                        <div class="fw-bold" style="color:#2B1B12; font-size:1.15rem;">${e:money(amount)}</div>
                    </div>
                    <div class="text-end">
                        <div class="mockpay-amount-label mb-0" style="font-size:0.68rem;">Time Remaining</div>
                        <span class="mockpay-countdown-display ${isExpired ? 'is-expired' : ''}" style="font-size:1.15rem;" id="mobileTimerDisplay">
                            ${isExpired ? '00:00' : '05:00'}
                        </span>
                    </div>
                </div>

                <!-- Normal Checkout View / Interactive State -->
                <div id="checkoutActiveView" class="p-4 p-md-5">
                    <div class="row g-4 g-lg-5 align-items-start">

                        <!-- Left Column: Authentic VietQR + Countdown + Radar -->
                        <div class="col-lg-5 col-md-6 text-center mockpay-order-qr">

                            <!-- Banking Radar Scan Status Indicator -->
                            <div class="banking-radar-box d-flex align-items-center justify-content-center gap-2 p-2 mb-3 rounded-pill bg-white border shadow-2xs">
                                <span class="radar-dot"></span>
                                <span class="small fw-semibold text-espresso" style="font-size: 0.78rem;">
                                    Awaiting incoming bank transfer notification...
                                </span>
                            </div>

                            <!-- QR Box with Authentic VietQR & Napas Bar -->
                            <div class="mockpay-qr-box ss-qr-card mb-3 ${isExpired ? 'mockpay-qr-expired' : ''}" id="qrBox">
                                <!-- VietQR Header Bar -->
                                <div class="d-flex justify-content-between align-items-center px-1 pb-2 mb-2 border-bottom">
                                    <div class="d-flex align-items-center">
                                        <span class="fw-bold fs-6" style="color: #005baa; letter-spacing: -0.5px;">Viet<span style="color: #e31837;">QR</span></span>
                                    </div>
                                    <div class="d-flex align-items-center gap-1">
                                        <span class="badge border fw-bold px-2 py-1" style="background-color: #FFF5F5; border-color: #FED7D7; color: #E31837; font-size: 0.68rem; letter-spacing: 0.5px;">NAPAS 247</span>
                                    </div>
                                </div>

                                <!-- Authentic VietQR Image with Laser Scan Line -->
                                <div class="mockpay-qr-wrapper">
                                    <div class="mockpay-qr-laser"></div>
                                    <img src="${pageContext.request.contextPath}/assets/images/qr/qranh.jpg" alt="VietQR SmartStay Payment" class="img-fluid rounded" width="340" height="354" style="max-width: 220px; width: 100%; height: auto; display: block; margin: 0 auto; object-fit: contain;" onerror="this.onerror=null; this.src='${pageContext.request.contextPath}/assets/images/branding/hero_img2.jpg';">
                                </div>

                                <!-- Download QR Button (English only) -->
                                <div class="mt-2 pt-1">
                                    <a href="${pageContext.request.contextPath}/assets/images/qr/qranh.jpg" download="VietQR_SmartStay_${orderCode}.jpg" class="btn btn-sm btn-outline-soft w-100 fw-semibold d-inline-flex align-items-center justify-content-center gap-1" style="border-radius: 8px; font-size: 0.8rem; padding: 0.45rem 0.5rem; min-height: 44px;">
                                        <i class="bi bi-download"></i> Download QR
                                    </a>
                                </div>
                            </div>

                            <!-- VietQR & Banking Guidance -->
                            <div class="text-muted small mt-2" style="font-size: 0.82rem;">
                                <i class="bi bi-qr-code-scan me-1 text-success"></i>Scan the QR code with your banking app
                            </div>
                            <div class="text-muted small mb-3" style="font-size: 0.75rem;">
                                Supported by Vietcombank, MB, Techcombank, MoMo &amp; all NAPAS 247 banks
                            </div>

                            <!-- Live Countdown mm:ss + Progress Bar (Caramel -> Amber -> Red) -->
                            <div class="p-3 bg-light rounded-3 border text-center d-none d-md-block mb-3" id="countdownCard">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <span class="text-muted small fw-medium" id="countdownLabel">Hold time remaining:</span>
                                    <span class="mockpay-countdown-display ${isExpired ? 'is-expired' : ''}" id="timer-display">
                                        ${isExpired ? '00:00' : '05:00'}
                                    </span>
                                </div>
                                <div class="mockpay-progress-track">
                                    <div class="mockpay-progress-bar bg-caramel" id="timerProgress" role="progressbar"
                                         style="width: ${isExpired ? '0%' : '100%'};"
                                         aria-valuenow="${remainingSeconds}" aria-valuemin="0" aria-valuemax="900">
                                    </div>
                                </div>

                                <!-- Screen Reader Milestones -->
                                <div class="visually-hidden" aria-live="polite" id="countdownAria"></div>
                            </div>

                            <!-- Expired Message (shown on mobile & desktop when expired) -->
                            <div id="expiredNotice" class="p-3 bg-danger bg-opacity-10 border border-danger rounded-3 text-center mb-3 ${isExpired ? '' : 'd-none'}">
                                <div class="text-danger small fw-semibold mb-2">
                                    <i class="bi bi-x-circle-fill me-1"></i>Payment session expired. Reserved slots have been released.
                                </div>
                                <a href="${pageContext.request.contextPath}/guest/bookings" class="btn btn-outline-soft w-100 fw-semibold d-inline-flex align-items-center justify-content-center" style="min-height: 44px;">
                                    <i class="bi bi-arrow-left me-1"></i>Back to booking
                                </a>
                            </div>

                        </div>

                        <!-- Right Column: Booking Summary + Beneficiary Info + Actions -->
                        <div class="col-lg-7 col-md-6 d-flex flex-column">

                            <!-- Mobile Accordion for Booking Summary (< 768px) -->
                            <div class="accordion mb-3 d-md-none" id="bookingSummaryAccordionMobile">
                                <div class="accordion-item border rounded-3 overflow-hidden">
                                    <h2 class="accordion-header" id="headingSummary">
                                        <button class="accordion-button collapsed py-3 fw-semibold text-dark" type="button" data-bs-toggle="collapse" data-bs-target="#collapseSummary" aria-expanded="false" aria-controls="collapseSummary" style="min-height: 44px; font-size: 0.88rem;">
                                            <i class="bi bi-receipt me-2 text-caramel"></i>Booking Summary: <span class="font-monospace ms-1 text-caramel">${e:esc(bookingCode)}</span>
                                        </button>
                                    </h2>
                                    <div id="collapseSummary" class="accordion-collapse collapse" aria-labelledby="headingSummary">
                                        <div class="accordion-body bg-milk">
                                            <t:reservationSummary detail="${bookingDetail}" amount="${amount}" roomNumber="${roomNumber}" bookingCode="${bookingCode}"/>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Desktop Booking Summary Card (>= 768px) -->
                            <div class="card border rounded-3 p-3 mb-3 d-none d-md-block bg-milk ss-reservation-card-checkout">
                                <t:reservationSummary detail="${bookingDetail}" amount="${amount}" roomNumber="${roomNumber}" bookingCode="${bookingCode}"/>
                            </div>

                            <!-- Beneficiary Account Details Card -->
                            <div class="mb-4 mockpay-order-transfer">
                                <div class="mockpay-amount-label mb-2">Beneficiary Account Details</div>
                                <div class="p-3 bg-light rounded-3 border">
                                    <!-- Beneficiary Bank -->
                                    <div class="mockpay-dl-row">
                                        <span class="mockpay-dl-label">Beneficiary Bank</span>
                                        <div class="mockpay-dl-value-wrap">
                                            <div class="mockpay-bank-row">
                                                <span class="mockpay-vcb-badge"><i class="bi bi-bank me-1"></i>VCB</span>
                                                <span class="mockpay-bank-name">${e:esc(merchantBankName)}</span>
                                            </div>
                                            <button type="button" class="btn-copy-field" data-copy-value="${e:esc(merchantBankName)}" aria-label="Copy bank name" title="Copy bank name">
                                                <i class="bi bi-copy"></i>
                                                <span>Copy</span>
                                            </button>
                                        </div>
                                    </div>

                                    <!-- Account Number -->
                                    <div class="mockpay-dl-row">
                                        <span class="mockpay-dl-label">Account Number / Payment Code</span>
                                        <div class="mockpay-dl-value-wrap">
                                            <span class="mockpay-dl-val font-monospace fs-6 text-success fw-bold" id="mockAccNum">${e:esc(merchantAccountNumber)}</span>
                                            <button type="button" class="btn-copy-field" data-copy-value="${e:esc(merchantAccountNumber)}" aria-label="Copy account number" title="Copy account number">
                                                <i class="bi bi-copy"></i>
                                                <span>Copy</span>
                                            </button>
                                        </div>
                                    </div>

                                    <!-- Account Name (Company business name from config constant) -->
                                    <div class="mockpay-dl-row">
                                        <span class="mockpay-dl-label">Account Holder</span>
                                        <div class="mockpay-dl-value-wrap">
                                            <span class="mockpay-dl-val fw-bold text-dark">${e:esc(merchantAccountName)}</span>
                                            <button type="button" class="btn-copy-field" data-copy-value="${e:esc(merchantAccountName)}" aria-label="Copy account name" title="Copy account name">
                                                <i class="bi bi-copy"></i>
                                                <span>Copy</span>
                                            </button>
                                        </div>
                                    </div>

                                    <!-- Transfer Amount -->
                                    <div class="mockpay-dl-row">
                                        <span class="mockpay-dl-label">Amount</span>
                                        <div class="mockpay-dl-value-wrap">
                                            <span class="mockpay-dl-val font-monospace fw-bold text-dark">${e:money(amount)}</span>
                                            <button type="button" class="btn-copy-field" data-copy-value="${amount}" aria-label="Copy amount" title="Copy amount">
                                                <i class="bi bi-copy"></i>
                                                <span>Copy</span>
                                            </button>
                                        </div>
                                    </div>

                                    <!-- Transfer Note / Content -->
                                    <div class="mockpay-dl-row">
                                        <span class="mockpay-dl-label">Transfer Note / Content</span>
                                        <div class="mockpay-dl-value-wrap">
                                            <span class="mockpay-dl-val font-monospace mockpay-content-badge" id="mockTransferContent">${e:esc(bookingCode)}</span>
                                            <button type="button" class="btn-copy-field" data-copy-value="${e:esc(bookingCode)}" aria-label="Copy transfer content" title="Copy transfer content">
                                                <i class="bi bi-copy"></i>
                                                <span>Copy</span>
                                            </button>
                                        </div>
                                    </div>

                                    <!-- Order Code -->
                                    <div class="mockpay-dl-row">
                                        <span class="mockpay-dl-label">Payment Order Code</span>
                                        <div class="mockpay-dl-value-wrap">
                                            <span class="mockpay-dl-val font-monospace text-muted" id="mockTxnCode">${orderCode}</span>
                                            <button type="button" class="btn-copy-field" data-copy-value="${orderCode}" aria-label="Copy order code" title="Copy order code">
                                                <i class="bi bi-copy"></i>
                                                <span>Copy</span>
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Customer Action Buttons -->
                            <div class="d-flex flex-column gap-2 mockpay-order-actions mt-auto">
                                <!-- 1. Primary Button: Manual Check Payment Status -->
                                <button type="button" id="btnCheckStatus" class="btn btn-caramel w-100 fw-semibold d-inline-flex align-items-center justify-content-center ss-paybar-m__cta" style="min-height: 44px; font-size: 0.95rem;">
                                    <i class="bi bi-arrow-repeat me-2 fs-5"></i>Check payment status
                                </button>
                                <div id="manualCheckFeedback" class="small text-center text-muted mt-1 d-none"></div>

                                <!-- 2. Secondary Button: Cancel & return to booking (with confirm dialog) -->
                                <button type="button" id="btnCancelBooking" class="btn btn-outline-soft w-100 fw-semibold d-inline-flex align-items-center justify-content-center ss-paybar-m__cancel" style="min-height: 44px; font-size: 0.9rem;">
                                    <i class="bi bi-x-circle me-2"></i>Cancel &amp; return to booking
                                </button>
                            </div>

                        </div>

                    </div>
                </div>

                <!-- Auto-Confirmed Success State (Shown when status transitions to PAID) -->
                <div id="simulationSuccessView" class="p-5 text-center d-none">
                    <div class="mb-3">
                        <div class="d-inline-flex align-items-center justify-content-center bg-success bg-opacity-10 text-success rounded-circle" style="width: 76px; height: 76px;">
                            <i class="bi bi-check2-circle fs-1"></i>
                        </div>
                    </div>
                    <h2 class="h4 text-dark mb-1">Payment received</h2>
                    <p class="text-muted small mb-2">Transaction verified via NAPAS 24/7 bank transfer. Door access credentials have been generated.</p>
                    <p class="text-success small fw-semibold mb-4" id="redirectNoticeText">
                        <span class="spinner-border spinner-border-sm me-1" role="status"></span> Redirecting to payment result in <span id="redirectSecs">2</span>s...
                    </p>
                    <div class="p-3 bg-light rounded-3 border d-inline-block text-start mb-4" style="min-width: 280px;">
                        <div class="d-flex justify-content-between small text-muted mb-1">
                            <span>Booking Code:</span>
                            <span class="font-monospace fw-semibold text-dark">${e:esc(bookingCode)}</span>
                        </div>
                        <div class="d-flex justify-content-between small text-muted mb-1">
                            <span>Room:</span>
                            <span class="fw-semibold text-dark">Room ${e:esc(roomNumber)}</span>
                        </div>
                        <div class="d-flex justify-content-between small text-muted">
                            <span>Amount Paid:</span>
                            <span class="fw-bold text-success">${e:money(amount)}</span>
                        </div>
                    </div>
                    <div>
                        <a href="${pageContext.request.contextPath}/guest/payment/result?bookingId=${bookingId}" class="btn btn-caramel px-4 py-2 fw-semibold" style="min-height: 44px;">
                            View Booking Result &amp; PIN Code <i class="bi bi-arrow-right ms-1"></i>
                        </a>
                    </div>
                </div>

                <!-- Developer Sandbox Panel (docked at bottom-right, distinct from guest UI).
                     Replaced by the Demo drawer payment buttons when demo.mode is on (Task R2-B3). -->
                <t:if test="${simulatorEnabled && !ssDemoDrawer}">
                    <div class="sandbox-simulator-dock" id="sandboxSimulatorDock">
                        <div class="sandbox-dock-header" id="sandboxDockHeader" title="Click to collapse / expand developer simulation tool">
                            <div class="d-flex align-items-center gap-2">
                                <span class="sandbox-dock-badge">DEV TOOL</span>
                                <span>Sandbox &middot; Simulate bank transfer</span>
                            </div>
                            <i class="bi bi-chevron-down" id="sandboxDockChevron"></i>
                        </div>
                        <div class="sandbox-dock-body" id="sandboxDockBody">
                            <p class="small text-muted mb-2" style="font-size: 0.76rem;">
                                Simulates an incoming payment webhook from the banking gateway:
                            </p>
                            <div class="d-flex flex-column gap-2">
                                <button type="button" id="btnSandboxSimSuccess" class="sandbox-sim-btn-success">
                                    <i class="bi bi-check2-circle fs-6"></i>
                                    <span>Simulate successful transfer</span>
                                </button>
                                <button type="button" id="btnSandboxSimFail" class="sandbox-sim-btn-fail">
                                    <i class="bi bi-x-circle fs-6"></i>
                                    <span>Simulate failure</span>
                                </button>
                            </div>
                            <div id="sandboxDockStatus" class="small text-muted font-monospace mt-2 text-center" style="font-size: 0.72rem;">
                                Ready to simulate
                            </div>
                        </div>
                    </div>
                </t:if>

            </t:if>

        </div>
    </div>

    <!-- Client-side Realtime WebSocket & MockPay scripts -->
    <%-- V4: realtime.js is already loaded once by layout.tag (a second copy opened a second socket) --%>
    <script src="${pageContext.request.contextPath}/assets/js/mockpay.js"></script>
    <script>
        (function() {
            // 1. Initialize copy buttons
            if (window.MockPay && window.MockPay.initCopyButtons) {
                MockPay.initCopyButtons();
            }

            // 2. Initialize Countdown Timer
            var initialSecs = ${remainingSeconds};
            var qrBox = document.getElementById('qrBox');
            var btnCheck = document.getElementById('btnCheckStatus');
            var expiredNotice = document.getElementById('expiredNotice');
            var timerDisplay = document.getElementById('timer-display');
            var mobileTimerDisplay = document.getElementById('mobileTimerDisplay');
            var countdown = null;

            function onExpire() {
                if (qrBox) qrBox.classList.add('mockpay-qr-expired');
                if (btnCheck) {
                    btnCheck.setAttribute('disabled', 'disabled');
                    btnCheck.classList.add('disabled');
                }
                if (expiredNotice) expiredNotice.classList.remove('d-none');
                if (timerDisplay) {
                    timerDisplay.classList.add('is-expired');
                    timerDisplay.classList.remove('is-amber', 'is-warning', 'is-red');
                }
                if (mobileTimerDisplay) {
                    mobileTimerDisplay.classList.add('is-expired');
                    mobileTimerDisplay.classList.remove('is-amber', 'is-warning', 'is-red');
                }
                if (window.mockPayInstance && window.mockPayInstance.checkStatus) {
                    window.mockPayInstance.checkStatus();
                }
            }

            if (window.MockPay && window.MockPay.initCountdown) {
                countdown = MockPay.initCountdown({
                    initialSeconds: initialSecs,
                    totalDuration: 900,
                    timerElement: '#timer-display',
                    progressElement: '#timerProgress',
                    ariaElement: '#countdownAria',
                    onTick: function(sec, formatted) {
                        if (mobileTimerDisplay) {
                            mobileTimerDisplay.textContent = formatted;
                            if (sec <= 0) {
                                mobileTimerDisplay.className = 'mockpay-countdown-display is-expired';
                            } else if (sec <= 60) {
                                mobileTimerDisplay.className = 'mockpay-countdown-display is-red';
                            } else if (sec <= 120) {
                                mobileTimerDisplay.className = 'mockpay-countdown-display is-amber';
                            } else {
                                mobileTimerDisplay.className = 'mockpay-countdown-display';
                            }
                        }
                    },
                    onExpire: onExpire
                });
            }

            // 3. Initialize Auto-Confirmation Engine (WebSocket + 3s Polling fallback + Sandbox simulator wiring)
            if (window.MockPay && window.MockPay.initAutoConfirm) {
                window.mockPayInstance = MockPay.initAutoConfirm({
                    bookingId: ${bookingId},
                    bookingCode: '${e:esc(bookingCode)}',
                    orderCode: ${orderCode},
                    amount: ${amount},
                    statusUrl: '${pageContext.request.contextPath}/guest/payment/status',
                    resultUrl: '${pageContext.request.contextPath}/guest/payment/result',
                    simulateUrl: '${pageContext.request.contextPath}/mock-payos/simulate',
                    cancelRedirectUrl: '${pageContext.request.contextPath}/guest/bookings',
                    csrfToken: '${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}',
                    countdown: countdown
                });
            }
        })();
    </script>
</t:layout>
