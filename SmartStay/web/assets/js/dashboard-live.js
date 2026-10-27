/**
 * Dashboard live refresh - Tini SmartStay (Task R6)
 *
 * Reuses the events realtime.js already dispatches ("smartstay:realtime") to refresh the KPI
 * counts, the Needs-attention list, room tiles and strips without a page reload: the dashboard
 * page is fetched again and #dashLive is swapped in place. The revenue chart (outside #dashLive)
 * is not touched. A quiet refresh every 60 s also catches device heartbeats (real time), which
 * have no event of their own.
 */
(function () {
    'use strict';

    var EVENTS = ['BOOKING_UPDATED', 'BOOKING_CHECKED_IN', 'ROOM_STATUS_CHANGED', 'ALERT_CREATED',
        'ALERT_UPDATED', 'PAYMENT_CONFIRMED', 'PAYMENT_SUCCESS', 'PAYMENT_UPDATED', 'SYNC_COMPLETED'];
    var DEBOUNCE_MS = 1000;
    var PERIOD_MS = 60000;

    function init() {
        var live = document.getElementById('dashLive');
        if (!live || !window.fetch || !window.DOMParser) {
            return;
        }
        var timer = null;
        var busy = false;

        function refresh() {
            if (busy) {
                return;
            }
            busy = true;
            fetch(window.location.href, { credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' } })
                .then(function (res) {
                    if (!res.ok) { throw new Error('HTTP ' + res.status); }
                    return res.text();
                })
                .then(function (html) {
                    var fresh = new DOMParser().parseFromString(html, 'text/html').getElementById('dashLive');
                    if (fresh) {
                        live.innerHTML = fresh.innerHTML;
                    }
                })
                .catch(function () {
                    // Keep the current figures; the next event or tick tries again.
                })
                .then(function () { busy = false; });
        }

        function schedule() {
            clearTimeout(timer);
            timer = setTimeout(refresh, DEBOUNCE_MS);
        }

        window.addEventListener('smartstay:realtime', function (e) {
            var data = e && e.detail;
            if (data && EVENTS.indexOf(data.event) >= 0) {
                schedule();
            }
        });

        setInterval(function () {
            if (!document.hidden) {
                refresh();
            }
        }, PERIOD_MS);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
