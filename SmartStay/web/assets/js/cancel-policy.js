/**
 * 48-hour cancellation line for the room-detail booking summary (Task R3)
 * Tini SmartStay - PRJ301
 *
 * Reads the selection that slot-matrix.js writes into #bookingSlotDate / #bookingSlotCodes
 * (it does not touch slot-matrix.js or its DOM) and fills every [data-cancel-line] element with
 * "Free cancellation until dd/MM/yyyy HH:mm" or a "Non-refundable" badge.
 * "Now" is the server's virtual clock (data-clock-now on #matrix-calendar), so time travel applies.
 * Same rule as util.CancellationPolicy: refundable while now <= first slot start - 48 h.
 */
(function (root, factory) {
    if (typeof module === 'object' && module.exports) {
        // Node.js environment (for unit testing)
        module.exports = factory();
    } else {
        // Browser environment
        root.CancelPolicy = factory();
        root.CancelPolicy.init();
    }
}(typeof self !== 'undefined' ? self : this, function () {
    'use strict';

    var DEFAULT_HOURS = 48;

    function pad2(n) {
        return (n < 10 ? '0' : '') + n;
    }

    /** "2026-10-26T09:30[:00]" or "2026-10-26 09:30" (server local time) -> Date. */
    function parseLocal(iso) {
        if (!iso) {
            return null;
        }
        var m = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/.exec(String(iso));
        if (!m) {
            return null;
        }
        return new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +(m[6] || 0));
    }

    /** Slot start from its date "yyyy-MM-dd" and time range "HH:mm–HH:mm (+1 day)". */
    function slotStart(dateStr, timeRange) {
        var t = /(\d{1,2}):(\d{2})/.exec(String(timeRange || ''));
        if (!dateStr || !t) {
            return null;
        }
        return parseLocal(dateStr + 'T' + pad2(+t[1]) + ':' + t[2]);
    }

    /** Earliest start among the selected slots of one date ({time, sort} objects). */
    function firstSlotStart(dateStr, slots) {
        var first = null;
        (slots || []).forEach(function (s) {
            var start = slotStart(dateStr, s.time);
            if (start && (!first || start.getTime() < first.getTime())) {
                first = start;
            }
        });
        return first;
    }

    function deadline(start, hours) {
        if (!start) {
            return null;
        }
        return new Date(start.getTime() - (hours || DEFAULT_HOURS) * 3600000);
    }

    /** Refundable while now is not after the deadline (the exact 48 h boundary still refunds). */
    function isRefundable(start, now, hours) {
        var d = deadline(start, hours);
        return !!(d && now && now.getTime() <= d.getTime());
    }

    function formatDateTime(d) {
        return pad2(d.getDate()) + '/' + pad2(d.getMonth() + 1) + '/' + d.getFullYear()
            + ' ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes());
    }

    /** {refundable, deadline, deadlineText} for a selection, or null when nothing is selected. */
    function describe(dateStr, slots, now, hours) {
        var start = firstSlotStart(dateStr, slots);
        if (!start) {
            return null;
        }
        var d = deadline(start, hours);
        return {
            refundable: isRefundable(start, now, hours),
            deadline: d,
            deadlineText: formatDateTime(d)
        };
    }

    // ---------------------------------------------------------------------
    // Browser wiring
    // ---------------------------------------------------------------------
    function render(lines, info, hours) {
        for (var i = 0; i < lines.length; i++) {
            var line = lines[i];
            line.innerHTML = '';
            if (!info) {
                line.hidden = true;
                continue;
            }
            line.classList.toggle('ss-cancel-line--free', info.refundable);
            line.classList.toggle('ss-cancel-line--locked', !info.refundable);
            if (info.refundable) {
                var icon = document.createElement('i');
                icon.className = 'bi bi-calendar-check ss-cancel-line__icon';
                icon.setAttribute('aria-hidden', 'true');
                var text = document.createElement('span');
                text.appendChild(document.createTextNode('Free cancellation until '));
                var time = document.createElement('span');
                time.className = 'ss-cancel-line__time';
                time.textContent = info.deadlineText;
                text.appendChild(time);
                line.appendChild(icon);
                line.appendChild(text);
            } else {
                var badge = document.createElement('span');
                badge.className = 'ss-badge ss-badge--warning';
                badge.textContent = 'Non-refundable';
                var why = document.createElement('span');
                why.textContent = 'Check-in is less than ' + hours + ' hours away, so this booking cannot be refunded.';
                line.appendChild(badge);
                line.appendChild(why);
            }
            line.hidden = false;
        }
    }

    function init() {
        if (typeof document === 'undefined') {
            return;
        }
        var start = function () {
            var container = document.getElementById('matrix-calendar');
            var dateInput = document.getElementById('bookingSlotDate');
            var codesBox = document.getElementById('bookingSlotCodes');
            var lines = document.querySelectorAll('[data-cancel-line]');
            if (!container || !dateInput || !codesBox || !lines.length) {
                return;
            }
            var hours = parseInt(container.getAttribute('data-cancel-free-hours'), 10) || DEFAULT_HOURS;
            var serverNow = parseLocal(container.getAttribute('data-clock-now'));
            var skew = serverNow ? serverNow.getTime() - Date.now() : 0;

            var update = function () {
                var dateStr = dateInput.value;
                var inputs = codesBox.querySelectorAll('input[name="slotCodes"]');
                var slots = [];
                for (var i = 0; i < inputs.length; i++) {
                    var btn = container.querySelector('.matrix-slot-available[data-date="' + dateStr
                        + '"][data-code="' + inputs[i].value + '"]');
                    if (btn) {
                        slots.push({ time: btn.getAttribute('data-time') });
                    }
                }
                render(lines, slots.length ? describe(dateStr, slots, new Date(Date.now() + skew), hours) : null, hours);
            };

            if (typeof MutationObserver !== 'undefined') {
                new MutationObserver(update).observe(codesBox, { childList: true });
            } else {
                container.addEventListener('click', function () { setTimeout(update, 0); });
            }
            update();
        };
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', start);
        } else {
            start();
        }
    }

    return {
        parseLocal: parseLocal,
        slotStart: slotStart,
        firstSlotStart: firstSlotStart,
        deadline: deadline,
        isRefundable: isRefundable,
        formatDateTime: formatDateTime,
        describe: describe,
        init: init
    };
}));
