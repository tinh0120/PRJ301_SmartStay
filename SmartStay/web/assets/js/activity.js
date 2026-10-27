/**
 * Activity logs page - Tini SmartStay (Task R9)
 * Click (or Enter) on a row opens the detail drawer with the technical IDs; Esc closes it and
 * returns focus to the row. Snapshot thumbnails open a lightbox.
 */
(function () {
    'use strict';

    var FIELDS = {
        door: [
            ['Time', 'time'], ['Result', 'result'], ['Reason', 'reason'], ['Guest', 'guest'],
            ['Booking', 'booking'], ['Room', 'room'], ['Door', 'door'], ['Device ID', 'device'],
            ['Credential ID', 'credential'], ['Request ID', 'request']
        ],
        // V3: the bookings list uses the same drawer
        booking: [
            ['Status', 'state'], ['Guest', 'guest'], ['E-mail', 'email'], ['Phone', 'phone'], ['Room', 'room'],
            ['Stay', 'when'], ['Total', 'total'], ['Payment', 'payment'], ['Booking code', 'code'], ['Created', 'created']
        ],
        motion: [
            ['Time', 'time'], ['Device', 'door'], ['Device ID', 'device'], ['Zone', 'zone'],
            ['Distance (cm)', 'distance'], ['Event ID', 'event'], ['Received by server', 'received']
        ]
    };

    function init() {
        var drawer = document.getElementById('actDrawer');
        var list = document.getElementById('actDrawerList');
        if (!drawer || !list) {
            return;
        }
        var tabAttr = list.getAttribute('data-tab');
        var tab = FIELDS[tabAttr] ? tabAttr : 'door';
        var openLink = document.getElementById('actDrawerOpen');
        var lastRow = null;

        function open(row) {
            list.innerHTML = '';
            FIELDS[tab].forEach(function (f) {
                var v = row.getAttribute('data-d-' + f[1]);
                if (v === null || v === '') {
                    return;
                }
                var dt = document.createElement('dt');
                dt.textContent = f[0];
                var dd = document.createElement('dd');
                dd.textContent = v;
                list.appendChild(dt);
                list.appendChild(dd);
            });
            lastRow = row;
            if (openLink) {
                var href = row.getAttribute('data-d-href');
                openLink.hidden = !href;
                if (href) {
                    openLink.setAttribute('href', href);
                }
            }
            drawer.classList.add('ss-drawer--open');
            drawer.setAttribute('aria-hidden', 'false');
            var title = document.getElementById('actDrawerTitle');
            if (title) { title.focus(); }
        }

        function close() {
            drawer.classList.remove('ss-drawer--open');
            drawer.setAttribute('aria-hidden', 'true');
            if (lastRow) { lastRow.focus(); }
        }

        document.addEventListener('click', function (e) {
            // links, buttons, forms and row menus inside a row keep their own behaviour
            if (e.target.closest('.ss-act__thumb, a, button, form, .dropdown, .dropdown-menu')) {
                return;
            }
            var row = e.target.closest('[data-act-row]');
            if (row) {
                open(row);
            }
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && drawer.classList.contains('ss-drawer--open')) {
                close();
                return;
            }
            if (e.key === 'Enter' && e.target.matches && e.target.matches('[data-act-row]')) {
                e.preventDefault();
                open(e.target);
            }
        });
        var closeBtn = drawer.querySelector('[data-act-close]');
        if (closeBtn) {
            closeBtn.addEventListener('click', close);
        }

        // Snapshot lightbox
        var modalEl = document.getElementById('actSnapModal');
        var img = document.getElementById('actSnapImg');
        var modal = (modalEl && window.bootstrap) ? new bootstrap.Modal(modalEl) : null;
        document.querySelectorAll('.ss-act__thumb').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                if (btn.classList.contains('ss-act__thumb--broken') || !modal || !img) {
                    return;
                }
                img.src = btn.getAttribute('data-snap-src');
                modal.show();
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
