/**
 * Demo tools drawer - Tini SmartStay (Task R2-B3)
 *
 * Talks to the /demo/* JSON API (DemoController):
 *   GET  /demo/state, POST /demo/time/jump (with dryRun preview), POST /demo/time/reset,
 *   POST /demo/payment/simulate.
 * Markup is rendered by layout.tag only when demo.mode is on and a guest/owner/admin is
 * logged in. Vanilla JS, no dependencies.
 */
(function () {
    'use strict';

    var STORE_KEY = 'ssDemoDrawer';
    var TICK_MS = 15000;
    var STEPS = [
        { target: 'BEFORE_CHECKIN_15', label: '15 min before check-in' },
        { target: 'CHECKIN_NOW', label: 'Check-in now' },
        { target: 'BEFORE_CHECKOUT_15', label: '15 min before check-out' },
        { target: 'CHECKOUT_NOW', label: 'Check-out now' }
    ];
    var STATUS_TEXT = {
        PENDING_PAYMENT: 'Awaiting payment',
        CONFIRMED: 'Confirmed',
        CHECKED_IN: 'Checked in',
        CHECKED_OUT: 'Checked out',
        CANCELLED: 'Cancelled',
        EXPIRED: 'Expired'
    };

    var drawer = document.getElementById('ssDemoDrawer');
    var tab = document.getElementById('ssDemoTab');
    var banner = document.getElementById('ssDemoBanner');
    var bannerTime = document.getElementById('ssDemoBannerTime');

    var ctx = '';
    var csrf = '';
    var offsetMs = banner ? parseInt(banner.getAttribute('data-offset-ms'), 10) || 0 : 0;
    var bookings = [];
    var loaded = false;
    var busy = false;

    // ---------------------------------------------------------------------
    // Small helpers
    // ---------------------------------------------------------------------
    function pad2(n) {
        return (n < 10 ? '0' : '') + n;
    }

    /** "2026-10-26T09:30:00" (server local time) -> Date, without timezone guessing. */
    function parseLocal(iso) {
        if (!iso) {
            return null;
        }
        var m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?/.exec(iso);
        if (!m) {
            return null;
        }
        return new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +(m[6] || 0));
    }

    function fmt(d) {
        return pad2(d.getDate()) + '/' + pad2(d.getMonth() + 1) + '/' + d.getFullYear()
            + ' ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes());
    }

    function simulatedNow() {
        return new Date(Date.now() + offsetMs);
    }

    function offsetText(ms) {
        var mins = Math.round(ms / 60000);
        if (mins <= 0) {
            return 'Real time';
        }
        var days = Math.floor(mins / 1440);
        var hours = Math.floor((mins % 1440) / 60);
        var rest = mins % 60;
        var parts = [];
        if (days) { parts.push(days + (days === 1 ? ' day' : ' days')); }
        if (hours) { parts.push(hours + ' h'); }
        if (rest || !parts.length) { parts.push(rest + ' min'); }
        return parts.join(' ') + ' ahead of real time';
    }

    function store(data) {
        try {
            if (data) {
                window.sessionStorage.setItem(STORE_KEY, JSON.stringify(data));
            } else {
                window.sessionStorage.removeItem(STORE_KEY);
            }
        } catch (e) {
            // Storage blocked: the drawer simply reopens closed.
        }
    }

    function readStore() {
        try {
            var raw = window.sessionStorage.getItem(STORE_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function el(tag, cls, text) {
        var node = document.createElement(tag);
        if (cls) { node.className = cls; }
        if (text != null) { node.textContent = text; }
        return node;
    }

    function setStatus(text, kind) {
        var s = document.getElementById('ssDemoStatus');
        if (!s) { return; }
        s.textContent = text || '';
        s.className = 'ss-demo-status' + (kind ? ' ss-demo-status--' + kind : '');
    }

    function request(method, path, params) {
        var opts = {
            method: method,
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
        };
        if (method === 'POST') {
            var body = new URLSearchParams();
            body.append('_csrf', csrf);
            Object.keys(params || {}).forEach(function (k) { body.append(k, params[k]); });
            opts.headers['Content-Type'] = 'application/x-www-form-urlencoded';
            opts.headers['X-CSRF-TOKEN'] = csrf;
            opts.body = body.toString();
        }
        return fetch(ctx + path, opts).then(function (res) {
            var type = res.headers.get('Content-Type') || '';
            if (type.indexOf('application/json') === -1) {
                throw new Error(res.status === 403
                    ? 'Your session expired. Reload the page and try again.'
                    : 'Demo tools are not available right now (HTTP ' + res.status + ').');
            }
            return res.json().then(function (json) {
                if (!res.ok || json.success === false) {
                    throw new Error(json.message || ('Request failed (HTTP ' + res.status + ').'));
                }
                return json;
            });
        });
    }

    // ---------------------------------------------------------------------
    // Clock (drawer + banner)
    // ---------------------------------------------------------------------
    function renderClock() {
        var now = simulatedNow();
        if (bannerTime) {
            bannerTime.textContent = fmt(now);
        }
        if (!drawer) { return; }
        var nowEl = document.getElementById('ssDemoNow');
        var metaEl = document.getElementById('ssDemoOffset');
        var clock = document.getElementById('ssDemoClock');
        if (nowEl) { nowEl.textContent = fmt(now); }
        if (metaEl) { metaEl.textContent = offsetText(offsetMs); }
        if (clock) { clock.classList.toggle('ss-demo-clock--shifted', offsetMs > 0); }
    }

    // ---------------------------------------------------------------------
    // Booking selector + stay timeline
    // ---------------------------------------------------------------------
    function selectedBooking() {
        var sel = document.getElementById('ssDemoBooking');
        if (!sel) { return null; }
        for (var i = 0; i < bookings.length; i++) {
            if (bookings[i].code === sel.value) { return bookings[i]; }
        }
        return null;
    }

    function stepMoment(target, b) {
        var start = parseLocal(b.checkin);
        var end = parseLocal(b.checkout);
        if (!start || !end) { return null; }
        switch (target) {
            case 'BEFORE_CHECKIN_15': return new Date(start.getTime() - 15 * 60000);
            case 'CHECKIN_NOW': return start;
            case 'BEFORE_CHECKOUT_15': return new Date(end.getTime() - 15 * 60000);
            default: return new Date(end.getTime() + 1000);
        }
    }

    function renderBookings(preferred) {
        var sel = document.getElementById('ssDemoBooking');
        var hint = document.getElementById('ssDemoBookingHint');
        if (!sel) { return; }
        sel.innerHTML = '';
        if (!bookings.length) {
            sel.disabled = true;
            sel.appendChild(el('option', null, 'No active bookings'));
            if (hint) { hint.textContent = 'Book a room first, then come back to travel through the stay.'; }
            renderSteps();
            return;
        }
        sel.disabled = false;
        if (hint) { hint.textContent = ''; }
        var chosen = null;
        bookings.forEach(function (b) {
            var start = parseLocal(b.checkin);
            var opt = el('option', null, b.code + ', room ' + b.room.split(' ')[0]
                + (start ? ', ' + fmt(start) : ''));
            opt.value = b.code;
            sel.appendChild(opt);
            if (preferred && b.code === preferred) { chosen = b.code; }
        });
        sel.value = chosen || bookings[0].code;
        renderSteps();
    }

    function renderSteps() {
        var list = document.getElementById('ssDemoSteps');
        var note = document.getElementById('ssDemoStepsNote');
        if (!list) { return; }
        list.innerHTML = '';
        hideConfirm();
        var b = selectedBooking();
        if (!b) {
            if (note) { note.textContent = ''; }
            return;
        }

        var now = simulatedNow();
        var blocked = b.status === 'PENDING_PAYMENT';
        if (note) {
            note.textContent = blocked
                ? 'Pay for this booking first, then the stay can be played forward.'
                : STATUS_TEXT[b.status] ? 'Booking is ' + STATUS_TEXT[b.status].toLowerCase() + '.' : '';
        }

        var nowPlaced = false;
        function placeNow() {
            var marker = el('li', 'ss-demo-steps__now', 'Now ' + fmt(now));
            marker.setAttribute('aria-label', 'Simulated time ' + fmt(now));
            list.appendChild(marker);
            nowPlaced = true;
        }

        STEPS.forEach(function (step) {
            var at = stepMoment(step.target, b);
            var passed = !at || at.getTime() < now.getTime();
            if (!passed && !nowPlaced) { placeNow(); }

            var item = el('li', 'ss-demo-steps__item' + (passed ? ' ss-demo-steps__item--passed' : ''));
            var btn = el('button', 'ss-demo-steps__btn');
            btn.type = 'button';
            btn.setAttribute('data-target', step.target);
            btn.appendChild(el('span', 'ss-demo-steps__label', step.label));
            btn.appendChild(el('span', 'ss-demo-steps__time', at ? (passed ? 'Passed, ' : '') + fmt(at) : ''));
            btn.disabled = passed || blocked;
            btn.addEventListener('click', function () { preview(step, item); });
            item.appendChild(btn);
            list.appendChild(item);
        });
        if (!nowPlaced) { placeNow(); }
    }

    // ---------------------------------------------------------------------
    // Jump: preview (dry run) -> inline confirm -> jump -> reload
    // ---------------------------------------------------------------------
    function hideConfirm() {
        var box = document.getElementById('ssDemoConfirm');
        if (box) {
            box.hidden = true;
            box.innerHTML = '';
        }
        var items = document.querySelectorAll('.ss-demo-steps__item--selected');
        for (var i = 0; i < items.length; i++) {
            items[i].classList.remove('ss-demo-steps__item--selected');
        }
    }

    function preview(step, item) {
        var b = selectedBooking();
        if (!b || busy) { return; }
        hideConfirm();
        item.classList.add('ss-demo-steps__item--selected');
        setStatus('Checking what this jump changes...');
        busy = true;
        request('POST', '/demo/time/jump', { bookingCode: b.code, target: step.target, dryRun: 'true' })
            .then(function (json) {
                setStatus('');
                showConfirm(step, b, json);
            })
            .catch(function (err) {
                item.classList.remove('ss-demo-steps__item--selected');
                setStatus(err.message, 'error');
            })
            .then(function () { busy = false; });
    }

    function showConfirm(step, b, info) {
        var box = document.getElementById('ssDemoConfirm');
        if (!box) { return; }
        box.innerHTML = '';
        box.appendChild(el('p', 'ss-demo-confirm__title', 'Jump to ' + info.targetAtDisplay + '?'));
        if (info.checkInFirst) {
            box.appendChild(el('p', 'ss-demo-confirm__text',
                'The guest is checked in first, so the stay is not marked as a no-show.'));
        }
        if (info.affectedCount > 0) {
            box.appendChild(el('p', 'ss-demo-confirm__text', 'The clock is shared, so '
                + info.affectedCount + (info.affectedCount === 1 ? ' other booking moves' : ' other bookings move')
                + ' too:'));
            var ul = el('ul', 'ss-demo-confirm__list');
            (info.affectedBookings || []).forEach(function (a) {
                ul.appendChild(el('li', null, a.code + ', room ' + a.room.split(' ')[0]
                    + ' (' + (STATUS_TEXT[a.status] || a.status) + ')'));
            });
            box.appendChild(ul);
        } else {
            box.appendChild(el('p', 'ss-demo-confirm__text', 'No other bookings are affected.'));
        }
        box.appendChild(el('p', 'ss-demo-confirm__text', 'Time only moves forward. Reset returns to real time.'));

        var actions = el('div', 'ss-demo-confirm__actions');
        var go = el('button', 'ss-demo-btn ss-demo-btn--primary', 'Jump');
        go.type = 'button';
        var cancel = el('button', 'ss-demo-btn ss-demo-btn--quiet', 'Cancel');
        cancel.type = 'button';
        go.addEventListener('click', function () { jump(step, b, go, cancel); });
        cancel.addEventListener('click', hideConfirm);
        actions.appendChild(go);
        actions.appendChild(cancel);
        box.appendChild(actions);
        box.hidden = false;
        go.focus();
    }

    function jump(step, b, go, cancel) {
        if (busy) { return; }
        busy = true;
        go.disabled = true;
        cancel.disabled = true;
        go.textContent = 'Jumping...';
        setStatus('Moving the clock and running the scheduled jobs...');
        request('POST', '/demo/time/jump', { bookingCode: b.code, target: step.target })
            .then(function (json) {
                var msg = 'Jumped to ' + json.targetAtDisplay + '. Booking ' + json.bookingCode + ' is '
                    + (STATUS_TEXT[json.bookingStatus] || json.bookingStatus).toLowerCase() + '.';
                store({ open: true, booking: b.code, message: msg });
                window.location.reload();
            })
            .catch(function (err) {
                busy = false;
                go.disabled = false;
                cancel.disabled = false;
                go.textContent = 'Jump';
                setStatus(err.message, 'error');
            });
    }

    // ---------------------------------------------------------------------
    // Reset + payment
    // ---------------------------------------------------------------------
    function reset(btn) {
        if (busy) { return; }
        busy = true;
        if (btn) { btn.disabled = true; }
        request('POST', '/demo/time/reset', {})
            .then(function () {
                var sel = document.getElementById('ssDemoBooking');
                store({
                    open: !!(drawer && drawer.classList.contains('ss-demo-drawer--open')),
                    booking: sel ? sel.value : null,
                    message: 'Clock is back to real time. Data changes made while time was shifted stay.'
                });
                window.location.reload();
            })
            .catch(function (err) {
                busy = false;
                if (btn) { btn.disabled = false; }
                if (drawer && !drawer.classList.contains('ss-demo-drawer--open')) { open(); }
                setStatus(err.message, 'error');
            });
    }

    function pay(result, btn) {
        var code = drawer ? drawer.getAttribute('data-checkout-booking') : '';
        if (!code || busy) { return; }
        busy = true;
        var buttons = drawer.querySelectorAll('[data-ss-demo-pay]');
        for (var i = 0; i < buttons.length; i++) { buttons[i].disabled = true; }
        setStatus(result === 'SUCCESS' ? 'Sending a successful transfer...' : 'Sending a failed transfer...');
        request('POST', '/demo/payment/simulate', { bookingCode: code, result: result })
            .then(function (json) {
                setStatus(result === 'SUCCESS'
                    ? 'Transfer received. Payment is ' + String(json.paymentStatus || '').toLowerCase() + '.'
                    : 'Failed transfer sent. The guest can try again.', 'ok');
                // Let the checkout page confirm right away instead of waiting for its next poll.
                if (window.mockPayInstance && window.mockPayInstance.checkStatus) {
                    window.mockPayInstance.checkStatus();
                }
            })
            .catch(function (err) {
                setStatus(err.message, 'error');
            })
            .then(function () {
                busy = false;
                for (var j = 0; j < buttons.length; j++) { buttons[j].disabled = false; }
            });
    }

    // ---------------------------------------------------------------------
    // Open / close + layout (push on desktop, sheet above bottom bars on mobile)
    // ---------------------------------------------------------------------
    var AVOID = '.mobile-booking-bar, .booking-mobile-paybar, .ss-bookbar, [data-ss-demo-avoid]';

    function bottomBarsHeight() {
        var max = 0;
        var nodes = document.querySelectorAll(AVOID);
        for (var i = 0; i < nodes.length; i++) {
            var cs = window.getComputedStyle(nodes[i]);
            if (cs.position !== 'fixed' || cs.display === 'none' || cs.visibility === 'hidden') { continue; }
            var r = nodes[i].getBoundingClientRect();
            if (r.height > 0 && r.bottom >= window.innerHeight - 2) {
                max = Math.max(max, r.height);
            }
        }
        return max;
    }

    function updateLayout() {
        var root = document.documentElement;
        root.style.setProperty('--ss-demo-bottom', bottomBarsHeight() + 'px');
        var isOpen = drawer && drawer.classList.contains('ss-demo-drawer--open');
        root.style.setProperty('--ss-demo-sheet-h', (isOpen ? drawer.offsetHeight : 0) + 'px');
    }

    function open() {
        if (!drawer) { return; }
        drawer.classList.add('ss-demo-drawer--open');
        document.body.classList.add('ss-demo-open');
        if (tab) { tab.setAttribute('aria-expanded', 'true'); }
        updateLayout();
        var title = document.getElementById('ssDemoTitle');
        if (title) { title.focus(); }
        if (!loaded) { loadState(); }
    }

    function close() {
        if (!drawer) { return; }
        drawer.classList.remove('ss-demo-drawer--open');
        document.body.classList.remove('ss-demo-open');
        if (tab) {
            tab.setAttribute('aria-expanded', 'false');
            tab.focus();
        }
        updateLayout();
    }

    function loadState(preferred) {
        setStatus('Loading bookings...');
        return request('GET', '/demo/state')
            .then(function (json) {
                loaded = true;
                var sim = parseLocal(json.simulatedNow);
                var real = parseLocal(json.realNow);
                offsetMs = (sim && real) ? Math.max(0, sim.getTime() - real.getTime()) : 0;
                bookings = json.bookings || [];
                renderClock();
                renderBookings(preferred || drawer.getAttribute('data-page-booking'));
                setStatus('');
                updateLayout();
            })
            .catch(function (err) {
                setStatus(err.message, 'error');
            });
    }

    // ---------------------------------------------------------------------
    // Wiring
    // ---------------------------------------------------------------------
    function init() {
        var resets = document.querySelectorAll('[data-ss-demo-reset]');
        for (var i = 0; i < resets.length; i++) {
            (function (btn) {
                btn.addEventListener('click', function () { reset(btn); });
            })(resets[i]);
        }

        renderClock();
        window.setInterval(function () {
            renderClock();
        }, TICK_MS);

        if (!drawer) { return; }
        ctx = drawer.getAttribute('data-ctx') || '';
        csrf = drawer.getAttribute('data-csrf') || '';

        if (tab) { tab.addEventListener('click', open); }
        var closeBtn = drawer.querySelector('[data-ss-demo-close]');
        if (closeBtn) { closeBtn.addEventListener('click', close); }
        document.addEventListener('keydown', function (e) {
            if ((e.key === 'Escape' || e.key === 'Esc') && drawer.classList.contains('ss-demo-drawer--open')) {
                close();
            }
        });

        var sel = document.getElementById('ssDemoBooking');
        if (sel) { sel.addEventListener('change', renderSteps); }

        var pays = drawer.querySelectorAll('[data-ss-demo-pay]');
        for (var j = 0; j < pays.length; j++) {
            (function (btn) {
                btn.addEventListener('click', function () { pay(btn.getAttribute('data-ss-demo-pay'), btn); });
            })(pays[j]);
        }

        window.addEventListener('resize', updateLayout);

        // Reopen after a jump/reset reload and show what happened.
        var saved = readStore();
        if (saved) {
            store(null);
            if (saved.open) {
                drawer.classList.add('ss-demo-drawer--open');
                document.body.classList.add('ss-demo-open');
                if (tab) { tab.setAttribute('aria-expanded', 'true'); }
                loadState(saved.booking).then(function () {
                    if (saved.message) { setStatus(saved.message, 'ok'); }
                });
            }
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
