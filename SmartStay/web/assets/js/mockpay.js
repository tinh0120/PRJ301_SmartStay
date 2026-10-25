/**
 * MockPay Checkout Utilities & Interactive Hooks
 * PayOS Sandbox Gateway Simulation - Tini SmartStay
 */
(function (root, factory) {
    if (typeof module === 'object' && module.exports) {
        module.exports = factory();
    } else {
        root.MockPay = factory();
    }
}(typeof self !== 'undefined' ? self : this, function () {
    'use strict';

    /**
     * Formats seconds into "mm:ss" string (e.g. 276 -> "04:36").
     */
    function formatTime(seconds) {
        var s = Math.max(0, Math.floor(Number(seconds) || 0));
        var mins = Math.floor(s / 60);
        var remSecs = s % 60;
        var pad = function (n) { return (n < 10 ? '0' : '') + n; };
        return pad(mins) + ':' + pad(remSecs);
    }

    /**
     * Formats currency consistently: "289,000 VND".
     */
    function formatCurrency(amount) {
        if (amount == null || amount === '') return '0 VND';
        var num = Number(amount);
        if (isNaN(num)) return '0 VND';
        var rounded = Math.round(num);
        var formatted = rounded.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        return formatted + ' VND';
    }

    /**
     * Copy text to clipboard with modern API & fallback.
     */
    function copyToClipboard(text, callback) {
        if (!text) {
            if (callback) callback(false);
            return;
        }

        if (typeof navigator !== 'undefined' && navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(function () {
                if (callback) callback(true);
            }).catch(function () {
                fallbackCopy(text, callback);
            });
        } else {
            fallbackCopy(text, callback);
        }
    }

    function fallbackCopy(text, callback) {
        try {
            if (typeof document === 'undefined') {
                if (callback) callback(false);
                return;
            }
            var ta = document.createElement('textarea');
            ta.value = text;
            ta.style.position = 'fixed';
            ta.style.top = '-9999px';
            ta.style.left = '-9999px';
            ta.setAttribute('readonly', '');
            document.body.appendChild(ta);
            ta.select();
            var successful = document.execCommand('copy');
            document.body.removeChild(ta);
            if (callback) callback(successful);
        } catch (e) {
            if (callback) callback(false);
        }
    }

    /**
     * Live Countdown Timer Hook with Progress Bar and Milestone Announcements
     * Caramel theme color, amber under 2:00 (120s), red under 1:00 (60s).
     */
    function initCountdown(options) {
        options = options || {};
        var totalDuration = Math.max(1, Number(options.totalDuration || options.initialSeconds || 900));
        var currentSeconds = Math.max(0, Number(options.initialSeconds != null ? options.initialSeconds : totalDuration));
        var timerEl = typeof options.timerElement === 'string' ? document.querySelector(options.timerElement) : options.timerElement;
        var progressBar = typeof options.progressElement === 'string' ? document.querySelector(options.progressElement) : options.progressElement;
        var ariaEl = typeof options.ariaElement === 'string' ? document.querySelector(options.ariaElement) : options.ariaElement;
        var onExpire = typeof options.onExpire === 'function' ? options.onExpire : null;
        var onTick = typeof options.onTick === 'function' ? options.onTick : null;

        var intervalId = null;
        var expired = currentSeconds <= 0;

        function announceMilestone(sec) {
            if (!ariaEl) return;
            if (sec === 120) {
                ariaEl.textContent = 'Notice: 2 minutes remaining to complete your payment.';
            } else if (sec === 60) {
                ariaEl.textContent = 'Notice: 1 minute remaining to complete your payment.';
            } else if (sec === 30) {
                ariaEl.textContent = 'Notice: 30 seconds remaining to complete your payment.';
            } else if (sec === 10) {
                ariaEl.textContent = 'Warning: 10 seconds remaining before reservation hold expires.';
            } else if (sec === 0) {
                ariaEl.textContent = 'Payment session has expired. Reservation slots have been released.';
            }
        }

        function render() {
            var formatted = formatTime(currentSeconds);
            if (timerEl) {
                timerEl.textContent = formatted;

                // Color thresholds: Caramel > 120s, Amber 61-120s, Red <= 60s
                if (timerEl.classList) {
                    if (currentSeconds <= 0) {
                        timerEl.classList.add('is-expired');
                        timerEl.classList.remove('is-amber', 'is-warning');
                    } else if (currentSeconds <= 60) {
                        timerEl.classList.add('is-red');
                        timerEl.classList.remove('is-amber', 'is-warning', 'is-expired');
                    } else if (currentSeconds <= 120) {
                        timerEl.classList.add('is-amber');
                        timerEl.classList.remove('is-red', 'is-expired');
                    } else {
                        timerEl.classList.remove('is-amber', 'is-warning', 'is-red', 'is-expired');
                    }
                }
            }

            if (progressBar) {
                var pct = Math.max(0, Math.min(100, (currentSeconds / totalDuration) * 100));
                progressBar.style.width = pct + '%';
                progressBar.setAttribute('aria-valuenow', currentSeconds);
                progressBar.setAttribute('aria-valuemax', totalDuration);

                if (currentSeconds <= 0) {
                    progressBar.classList.add('bg-danger');
                    progressBar.classList.remove('bg-navy', 'bg-warning', 'bg-caramel');
                } else if (currentSeconds <= 60) {
                    progressBar.classList.add('bg-danger');
                    progressBar.classList.remove('bg-navy', 'bg-warning', 'bg-caramel');
                } else if (currentSeconds <= 120) {
                    progressBar.classList.add('bg-warning');
                    progressBar.classList.remove('bg-navy', 'bg-danger', 'bg-caramel');
                } else {
                    progressBar.classList.add('bg-caramel');
                    progressBar.classList.remove('bg-warning', 'bg-danger', 'bg-navy');
                }
            }

            if (currentSeconds === 120 || currentSeconds === 60 || currentSeconds === 30 || currentSeconds === 10 || currentSeconds === 0) {
                announceMilestone(currentSeconds);
            }

            if (onTick) {
                onTick(currentSeconds, formatted);
            }
        }

        function triggerExpire() {
            if (expired) return;
            expired = true;
            currentSeconds = 0;
            if (intervalId) {
                clearInterval(intervalId);
                intervalId = null;
            }
            render();
            if (onExpire) {
                onExpire();
            }
        }

        function tick() {
            if (currentSeconds <= 0) {
                triggerExpire();
                return;
            }
            currentSeconds--;
            render();
            if (currentSeconds <= 0) {
                triggerExpire();
            }
        }

        render();

        if (!expired) {
            intervalId = setInterval(tick, 1000);
        } else {
            triggerExpire();
        }

        return {
            stop: function () {
                if (intervalId) {
                    clearInterval(intervalId);
                    intervalId = null;
                }
            },
            getTimeRemaining: function () {
                return currentSeconds;
            },
            isExpired: function () {
                return expired;
            },
            expireNow: triggerExpire
        };
    }

    /**
     * Wire copy buttons with temporary feedback
     */
    function initCopyButtons(selector) {
        if (typeof document === 'undefined') return;
        var buttons = document.querySelectorAll(selector || '.btn-copy-field');
        buttons.forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                var textToCopy = btn.getAttribute('data-copy-value') || '';
                var targetId = btn.getAttribute('data-copy-target');
                if (!textToCopy && targetId) {
                    var targetEl = document.getElementById(targetId);
                    if (targetEl) textToCopy = targetEl.textContent.trim();
                }

                copyToClipboard(textToCopy, function (success) {
                    if (success) {
                        var originalHtml = btn.innerHTML;
                        btn.innerHTML = '<i class="bi bi-check2 text-success me-1"></i><span class="text-success fw-medium">Copied</span>';
                        btn.classList.add('btn-copied');
                        setTimeout(function () {
                            btn.innerHTML = originalHtml;
                            btn.classList.remove('btn-copied');
                        }, 1500);
                    }
                });
            });
        });
    }

    /**
     * Realtime WebSocket + Polling Auto-Confirmation Engine & Sandbox Simulator Controller
     */
    function initAutoConfirm(options) {
        options = options || {};
        var bookingId = options.bookingId;
        var bookingCode = options.bookingCode;
        var orderCode = options.orderCode;
        var statusUrl = options.statusUrl || '/guest/payment/status';
        var resultUrl = options.resultUrl || '/guest/payment/result';
        var simulateUrl = options.simulateUrl || '/mock-payos/simulate';
        var csrfToken = options.csrfToken || '';
        var onPaid = typeof options.onPaid === 'function' ? options.onPaid : null;
        var onFailed = typeof options.onFailed === 'function' ? options.onFailed : null;
        var onExpired = typeof options.onExpired === 'function' ? options.onExpired : null;

        var pollingTimer = null;
        var isResolved = false;

        function handlePaymentSuccess(data) {
            if (isResolved) return;
            isResolved = true;
            if (pollingTimer) {
                clearInterval(pollingTimer);
                pollingTimer = null;
            }

            console.log('[MockPay] Payment confirmed successfully:', data);

            // Trigger visual success state
            var activeView = document.getElementById('checkoutActiveView');
            var successView = document.getElementById('simulationSuccessView');
            if (activeView && successView) {
                activeView.classList.add('d-none');
                successView.classList.remove('d-none');
            }

            function resolveDestUrl(rawUrl, fallbackUrl) {
                if (!rawUrl) return fallbackUrl;
                if (rawUrl.indexOf('http://') === 0 || rawUrl.indexOf('https://') === 0) {
                    return rawUrl;
                }
                if (rawUrl.indexOf(resultUrl) === 0) {
                    return rawUrl;
                }
                var idx = resultUrl.indexOf('/guest/');
                var contextPrefix = idx >= 0 ? resultUrl.substring(0, idx) : '';
                return contextPrefix + (rawUrl.indexOf('/') === 0 ? rawUrl : '/' + rawUrl);
            }

            var destUrl = resolveDestUrl(data && data.redirectUrl, resultUrl + '?bookingId=' + bookingId);

            if (onPaid) {
                onPaid(data, destUrl);
            } else {
                var redirectSecs = 2;
                var rEl = document.getElementById('redirectSecs');
                var timer = setInterval(function () {
                    redirectSecs--;
                    if (rEl) rEl.textContent = redirectSecs;
                    if (redirectSecs <= 0) {
                        clearInterval(timer);
                        window.location.href = destUrl;
                    }
                }, 1000);
            }
        }

        function handlePaymentFailure(data) {
            if (isResolved) return;
            isResolved = true;
            if (pollingTimer) {
                clearInterval(pollingTimer);
                pollingTimer = null;
            }
            console.log('[MockPay] Payment failed or cancelled:', data);
            function resolveDestUrl(rawUrl, fallbackUrl) {
                if (!rawUrl) return fallbackUrl;
                if (rawUrl.indexOf('http://') === 0 || rawUrl.indexOf('https://') === 0) {
                    return rawUrl;
                }
                if (rawUrl.indexOf(resultUrl) === 0) {
                    return rawUrl;
                }
                var idx = resultUrl.indexOf('/guest/');
                var contextPrefix = idx >= 0 ? resultUrl.substring(0, idx) : '';
                return contextPrefix + (rawUrl.indexOf('/') === 0 ? rawUrl : '/' + rawUrl);
            }

            var destUrl = resolveDestUrl(data && data.redirectUrl, resultUrl + '?bookingId=' + bookingId);
            if (onFailed) {
                onFailed(data, destUrl);
            } else {
                alert('Payment was declined or cancelled.');
                window.location.href = destUrl;
            }
        }

        // 1. WebSocket Listener (Dispatched by realtime.js)
        function onWsPaymentConfirmed(e) {
            var detail = e.detail || {};
            if (detail.bookingId && Number(detail.bookingId) !== Number(bookingId)) {
                return;
            }
            if (detail.orderCode && Number(detail.orderCode) !== Number(orderCode)) {
                return;
            }
            handlePaymentSuccess(detail);
        }

        function onWsPaymentFailed(e) {
            var detail = e.detail || {};
            if (detail.bookingId && Number(detail.bookingId) !== Number(bookingId)) {
                return;
            }
            handlePaymentFailure(detail);
        }

        window.addEventListener('smartstay:PAYMENT_CONFIRMED', onWsPaymentConfirmed);
        window.addEventListener('smartstay:PAYMENT_FAILED', onWsPaymentFailed);

        // 2. Polling Fallback Every 3s
        function checkStatus(callback) {
            if (isResolved) return;
            var url = statusUrl + (statusUrl.indexOf('?') >= 0 ? '&' : '?') 
                    + 'bookingId=' + encodeURIComponent(bookingId) 
                    + '&orderCode=' + encodeURIComponent(orderCode)
                    + '&_=' + Date.now();

            fetch(url, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
            .then(function (res) {
                if (!res.ok) {
                    throw new Error('HTTP ' + res.status);
                }
                return res.json();
            })
            .then(function (data) {
                if (callback) callback(null, data);
                if (data && (data.status === 'PAID' || data.status === 'REVIEW_REQUIRED')) {
                    handlePaymentSuccess(data);
                } else if (data && data.status === 'FAILED') {
                    handlePaymentFailure(data);
                } else if (data && data.status === 'EXPIRED') {
                    if (options.countdown && options.countdown.expireNow) {
                        options.countdown.expireNow();
                    }
                    if (onExpired) onExpired(data);
                } else if (data && data.status === 'CANCELLED') {
                    window.location.href = data.redirectUrl || '/guest/bookings';
                }
            })
            .catch(function (err) {
                console.warn('[MockPay] Status polling check failed:', err);
                if (callback) callback(err, null);
            });
        }

        pollingTimer = setInterval(function () {
            checkStatus();
        }, 3000);

        // Re-check immediately when tab becomes visible again
        document.addEventListener('visibilitychange', function () {
            if (document.visibilityState === 'visible' && !isResolved) {
                checkStatus();
            }
        });

        // 3. Manual "Check payment status" Button
        var btnCheck = document.getElementById('btnCheckStatus');
        if (btnCheck) {
            btnCheck.addEventListener('click', function () {
                var origText = btnCheck.innerHTML;
                btnCheck.disabled = true;
                btnCheck.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Checking gateway status...';

                checkStatus(function (err, data) {
                    setTimeout(function () {
                        if (!isResolved) {
                            btnCheck.disabled = false;
                            btnCheck.innerHTML = origText;
                            if (data && data.status === 'PENDING') {
                                showStatusFeedback('Payment is pending. Please complete the bank transfer.');
                            } else if (err) {
                                showStatusFeedback('Could not reach server. Please try again.');
                            }
                        }
                    }, 600);
                });
            });
        }

        function showStatusFeedback(msg) {
            var fb = document.getElementById('manualCheckFeedback');
            if (fb) {
                fb.textContent = msg;
                fb.classList.remove('d-none');
                setTimeout(function () {
                    fb.classList.add('d-none');
                }, 4000);
            }
        }

        // 4. "Cancel & return to booking" Button with confirmation
        var btnCancel = document.getElementById('btnCancelBooking');
        if (btnCancel) {
            btnCancel.addEventListener('click', function (e) {
                e.preventDefault();
                var confirmed = window.confirm('Are you sure you want to cancel this payment? Your held slot(s) will be released.');
                if (!confirmed) return;

                // Send cancel request to simulate endpoint
                var params = new URLSearchParams();
                params.append('_csrf', csrfToken);
                params.append('orderCode', orderCode);
                params.append('amount', options.amount || 0);
                params.append('result', 'CANCEL');

                fetch(simulateUrl, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                        'Accept': 'application/json'
                    },
                    body: params.toString()
                }).finally(function () {
                    window.location.href = options.cancelRedirectUrl || '/guest/bookings';
                });
            });
        }

        // 5. Developer Sandbox Panel Wiring
        var dockHeader = document.getElementById('sandboxDockHeader');
        var dockBody = document.getElementById('sandboxDockBody');
        var dockChevron = document.getElementById('sandboxDockChevron');
        if (dockHeader && dockBody) {
            dockHeader.addEventListener('click', function () {
                var isHidden = dockBody.classList.contains('d-none');
                if (isHidden) {
                    dockBody.classList.remove('d-none');
                    if (dockChevron) dockChevron.className = 'bi bi-chevron-down';
                } else {
                    dockBody.classList.add('d-none');
                    if (dockChevron) dockChevron.className = 'bi bi-chevron-up';
                }
            });
        }

        function runSandboxSimulation(resultValue, btn) {
            if (btn) btn.disabled = true;
            var statusEl = document.getElementById('sandboxDockStatus');
            if (statusEl) {
                statusEl.textContent = 'Simulating ' + resultValue + '...';
                statusEl.className = 'small text-warning font-monospace mt-2';
            }

            var params = new URLSearchParams();
            params.append('_csrf', csrfToken);
            params.append('orderCode', orderCode);
            params.append('amount', options.amount || 0);
            params.append('result', resultValue);

            fetch(simulateUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: params.toString()
            })
            .then(function (res) { return res.json(); })
            .then(function (json) {
                if (statusEl) {
                    statusEl.textContent = 'Simulated ' + resultValue + ' sent. Waiting for auto-confirm...';
                    statusEl.className = 'small text-success font-monospace mt-2';
                }
                setTimeout(function () { checkStatus(); }, 300);
            })
            .catch(function (err) {
                if (statusEl) {
                    statusEl.textContent = 'Simulation error: ' + err.message;
                    statusEl.className = 'small text-danger font-monospace mt-2';
                }
            })
            .finally(function () {
                setTimeout(function () {
                    if (btn) btn.disabled = false;
                }, 1500);
            });
        }

        var btnSimSuccess = document.getElementById('btnSandboxSimSuccess');
        if (btnSimSuccess) {
            btnSimSuccess.addEventListener('click', function () {
                runSandboxSimulation('SUCCESS', btnSimSuccess);
            });
        }

        var btnSimFail = document.getElementById('btnSandboxSimFail');
        if (btnSimFail) {
            btnSimFail.addEventListener('click', function () {
                runSandboxSimulation('FAILED', btnSimFail);
            });
        }

        return {
            checkStatus: checkStatus,
            stop: function () {
                if (pollingTimer) {
                    clearInterval(pollingTimer);
                    pollingTimer = null;
                }
                window.removeEventListener('smartstay:PAYMENT_CONFIRMED', onWsPaymentConfirmed);
                window.removeEventListener('smartstay:PAYMENT_FAILED', onWsPaymentFailed);
            }
        };
    }

    return {
        formatTime: formatTime,
        formatCurrency: formatCurrency,
        copyToClipboard: copyToClipboard,
        initCountdown: initCountdown,
        initCopyButtons: initCopyButtons,
        initAutoConfirm: initAutoConfirm
    };
}));
