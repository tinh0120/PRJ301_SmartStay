/**
 * Booking Detail Page Utilities & Live Countdown Hook
 * Tini SmartStay - PRJ301
 */
(function (root, factory) {
    if (typeof module === 'object' && module.exports) {
        // Node.js environment (for unit testing)
        module.exports = factory();
    } else {
        // Browser environment
        root.BookingDetailUtils = factory();
    }
}(typeof self !== 'undefined' ? self : this, function () {
    'use strict';

    /**
     * Parse date input into a valid Date object.
     * Supports ISO string ('2026-10-23T16:30:00', '2026-10-23 16:30'), epoch ms, or Date instance.
     */
    function parseDate(input) {
        if (!input) return null;
        if (input instanceof Date) return isNaN(input.getTime()) ? null : input;
        if (typeof input === 'number') {
            var d = new Date(input);
            return isNaN(d.getTime()) ? null : d;
        }
        if (typeof input === 'string') {
            var s = input.trim();
            if (!s) return null;
            // Normalize space-separated ISO 'YYYY-MM-DD HH:mm[:ss]' to 'YYYY-MM-DDTHH:mm[:ss]'
            if (/^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}/.test(s)) {
                s = s.replace(/\s+/, 'T');
            }
            var parsed = new Date(s);
            if (!isNaN(parsed.getTime())) return parsed;
            // Fallback: manually parse YYYY-MM-DDTHH:mm
            var m = s.match(/^(\d{4})-(\d{2})-(\d{2})(?:[T\s](\d{2}):(\d{2})(?::(\d{2}))?)?/);
            if (m) {
                return new Date(
                    parseInt(m[1], 10),
                    parseInt(m[2], 10) - 1,
                    parseInt(m[3], 10),
                    m[4] ? parseInt(m[4], 10) : 0,
                    m[5] ? parseInt(m[5], 10) : 0,
                    m[6] ? parseInt(m[6], 10) : 0
                );
            }
        }
        return null;
    }

    function pad2(n) {
        return (n < 10 ? '0' : '') + n;
    }

    /**
     * Formats Date to "DD/MM/YYYY" (e.g. 23/10/2026).
     */
    function formatDate(input) {
        var d = parseDate(input);
        if (!d) return '';
        var day = pad2(d.getDate());
        var month = pad2(d.getMonth() + 1);
        var year = d.getFullYear();
        return day + '/' + month + '/' + year;
    }

    /**
     * Formats time to 24h "HH:mm" (e.g. 16:30).
     */
    function formatTime(input) {
        var d = parseDate(input);
        if (!d) return '';
        var hours = pad2(d.getHours());
        var mins = pad2(d.getMinutes());
        return hours + ':' + mins;
    }

    /**
     * Formats Date and time to "DD/MM/YYYY · HH:mm" (e.g. 23/10/2026 · 16:30).
     */
    function formatDateTime(input) {
        var d = parseDate(input);
        if (!d) return '';
        return formatDate(d) + ' · ' + formatTime(d);
    }

    /**
     * Formats Date with weekday: "Fri, 23/10/2026 · 16:30".
     */
    function formatWeekdayDateTime(input) {
        var d = parseDate(input);
        if (!d) return '';
        var weekdays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
        var w = weekdays[d.getDay()];
        return w + ', ' + formatDate(d) + ' · ' + formatTime(d);
    }

    /**
     * Computes human-readable duration between start and end (e.g. "3 hours", "1 hour", "45 minutes").
     */
    function formatDuration(startInput, endInput) {
        var s = parseDate(startInput);
        var e = parseDate(endInput);
        if (!s || !e) return '';
        var diffMs = e.getTime() - s.getTime();
        if (diffMs <= 0) return '0 hours';
        var totalMinutes = Math.round(diffMs / 60000);
        if (totalMinutes < 60) {
            return totalMinutes + ' minute' + (totalMinutes !== 1 ? 's' : '');
        }
        var hours = Math.floor(totalMinutes / 60);
        var remMins = totalMinutes % 60;
        if (remMins === 0) {
            return hours + ' hour' + (hours !== 1 ? 's' : '');
        }
        return hours + ' hour' + (hours !== 1 ? 's ' : ' ') + remMins + ' mins';
    }

    /**
     * Formats currency consistently: "289,000 VND". Always uses "VND".
     */
    function formatCurrency(amount) {
        if (amount == null || amount === '') return '0 VND';
        var num = Number(amount);
        if (isNaN(num)) return '0 VND';
        var rounded = Math.round(num);
        // Uses standard US grouping with comma, appended with VND
        var parts = rounded.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        return parts + ' VND';
    }

    /**
     * Helper for minute pluralization (1 minute / N minutes).
     */
    function pluralizeMinutes(minutes) {
        var count = Math.round(Number(minutes) || 0);
        return count + ' minute' + (count === 1 ? '' : 's');
    }

    /**
     * Live Countdown Timer Hook
     * Updates every second, formatted as mm:ss.
     * Transitions banner to expired state at 0 and disables payment buttons.
     */
    function initCountdownTimer(options) {
        options = options || {};
        var deadline = parseDate(options.deadline);
        var timerEl = typeof options.timerElement === 'string'
            ? document.querySelector(options.timerElement)
            : options.timerElement;
        var bannerEl = typeof options.bannerElement === 'string'
            ? document.querySelector(options.bannerElement)
            : options.bannerElement;
        var paymentBtns = options.paymentButtonSelector
            ? document.querySelectorAll(options.paymentButtonSelector)
            : [];
        var helperEl = typeof options.expiryHelperSelector === 'string'
            ? document.querySelector(options.expiryHelperSelector)
            : options.expiryHelperSelector;
        var onExpire = typeof options.onExpire === 'function' ? options.onExpire : null;

        if (!deadline) {
            return { stop: function () {}, isExpired: function () { return false; } };
        }

        var intervalId = null;
        var expired = false;

        function markExpired() {
            expired = true;
            if (intervalId) {
                clearInterval(intervalId);
                intervalId = null;
            }

            // Update banner styling and copy
            if (bannerEl) {
                bannerEl.classList.remove('alert-warning');
                bannerEl.classList.add('alert-danger');
                bannerEl.setAttribute('aria-live', 'polite');
                bannerEl.innerHTML =
                    '<div class="d-flex align-items-center gap-2">' +
                    '  <i class="bi bi-x-circle-fill text-danger fs-5"></i>' +
                    '  <div><strong>Hold expired, slots released.</strong> Please create a new booking if you wish to reserve.</div>' +
                    '</div>';
            }

            // Disable all payment buttons
            if (paymentBtns && paymentBtns.length) {
                for (var i = 0; i < paymentBtns.length; i++) {
                    paymentBtns[i].setAttribute('disabled', 'disabled');
                    paymentBtns[i].classList.add('disabled');
                    paymentBtns[i].textContent = 'Hold expired';
                }
            }

            // Update helper note
            if (helperEl) {
                helperEl.innerHTML = '<span class="text-danger fw-semibold">Hold expired, slots released</span>';
            }

            if (onExpire) {
                try {
                    onExpire();
                } catch (e) {
                    console.error('onExpire error', e);
                }
            }
        }

        function tick() {
            var now = Date.now();
            var diffMs = deadline.getTime() - now;

            if (diffMs <= 0) {
                markExpired();
                return;
            }

            var totalSeconds = Math.floor(diffMs / 1000);
            var mins = Math.floor(totalSeconds / 60);
            var secs = totalSeconds % 60;
            var timeFormatted = pad2(mins) + ':' + pad2(secs);

            if (timerEl) {
                timerEl.textContent = timeFormatted;
            }
        }

        // Run immediately
        tick();

        if (!expired) {
            intervalId = setInterval(tick, 1000);
        }

        return {
            stop: function () {
                if (intervalId) {
                    clearInterval(intervalId);
                    intervalId = null;
                }
            },
            isExpired: function () {
                return expired;
            },
            tickNow: tick,
            triggerExpire: markExpired
        };
    }

    /**
     * Auto-initialization when running on booking detail page.
     */
    function autoInit() {
        if (typeof document === 'undefined') return;

        var banner = document.getElementById('paymentPendingBanner');
        if (banner) {
            var rawDeadline = banner.getAttribute('data-deadline');
            if (rawDeadline) {
                initCountdownTimer({
                    deadline: rawDeadline,
                    timerElement: '#countdownTimer',
                    bannerElement: '#paymentPendingBanner',
                    paymentButtonSelector: '.btn-pay-action',
                    expiryHelperSelector: '.slot-held-until-container'
                });
            }
        }

        // Cancellation reason synchronization
        var reasonInput = document.getElementById('cancelReasonInput');
        var modalReason = document.getElementById('modalCancelReason');
        var cancelModalEl = document.getElementById('cancelModal');
        var cancelForm = document.getElementById('cancelBookingForm');

        function syncReason() {
            if (reasonInput && modalReason) {
                modalReason.value = reasonInput.value ? reasonInput.value.trim() : '';
            }
        }

        if (reasonInput) {
            reasonInput.addEventListener('input', syncReason);
            reasonInput.addEventListener('change', syncReason);
        }
        if (cancelModalEl) {
            cancelModalEl.addEventListener('show.bs.modal', syncReason);
        }
        if (cancelForm) {
            cancelForm.addEventListener('submit', syncReason);
        }
    }

    if (typeof document !== 'undefined') {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', autoInit);
        } else {
            autoInit();
        }
    }

    return {
        parseDate: parseDate,
        formatDate: formatDate,
        formatTime: formatTime,
        formatDateTime: formatDateTime,
        formatWeekdayDateTime: formatWeekdayDateTime,
        formatDuration: formatDuration,
        formatCurrency: formatCurrency,
        pluralizeMinutes: pluralizeMinutes,
        initCountdownTimer: initCountdownTimer,
        autoInit: autoInit
    };
}));
