/**
 * Security alerts inbox - Tini SmartStay (Task R10)
 * - Acknowledge / Resolve forms are posted with fetch and the row is updated in place
 *   (the plain form POST + redirect stays as the no-JS fallback).
 * - Click (or Enter) on an item opens the detail drawer; Esc closes it.
 * - ALERT_CREATED / ALERT_UPDATED realtime events pull fresh rows from the server and put new
 *   ones on top with a short highlight, without reloading the page.
 */
(function () {
    'use strict';

    var STATUS_TEXT = { NEW: 'Open', ACKNOWLEDGED: 'Acknowledged', RESOLVED: 'Resolved' };

    function init() {
        var inbox = document.getElementById('alertInbox');
        var list = document.getElementById('alertList');
        if (!inbox || !list) {
            return;
        }
        var drawer = document.getElementById('alertDrawer');
        var lastRow = null;

        // ---------- header badges and tab counts ----------
        function setNavCount(n) {
            if (typeof n !== 'number') {
                return;
            }
            document.querySelectorAll('#navAlertBadge, [data-alert-count]').forEach(function (b) {
                b.textContent = n;
                b.style.display = n > 0 ? '' : 'none';
            });
        }

        function bumpTab(status, delta) {
            var el = document.querySelector('[data-tab-count="' + status + '"]');
            if (el) {
                var v = parseInt(el.textContent, 10) || 0;
                el.textContent = Math.max(0, v + delta);
            }
        }

        // ---------- in-place actions ----------
        function doneText(label) {
            var span = document.createElement('span');
            span.className = 'ss-inbox__done';
            span.textContent = label;
            return span;
        }

        function applyStatus(row, status, res) {
            var old = row.getAttribute('data-status');
            row.classList.remove('ss-inbox__item--acked');
            void row.offsetWidth;
            row.classList.add('ss-inbox__item--acked');
            row.setAttribute('data-status', status);
            row.setAttribute('data-d-status', status);
            var now = new Date();
            var stamp = pad(now.getDate()) + '/' + pad(now.getMonth() + 1) + '/' + now.getFullYear() + ' '
                + pad(now.getHours()) + ':' + pad(now.getMinutes());
            var actions = row.querySelector('[data-alert-actions]');
            if (status === 'ACKNOWLEDGED') {
                row.setAttribute('data-d-ackby', 'You');
                row.setAttribute('data-d-ackat', stamp);
                var tpl = document.getElementById('alertResolveTpl');
                actions.innerHTML = '';
                if (tpl) {
                    var html = tpl.innerHTML.replace(/__ID__/g, row.getAttribute('data-alert-id'));
                    actions.innerHTML = html;
                    wireForms(actions);
                } else {
                    actions.appendChild(doneText('Acknowledged by you'));
                }
            } else if (status === 'RESOLVED') {
                row.setAttribute('data-d-resby', 'You');
                row.setAttribute('data-d-resat', stamp);
                if (res && res.note) {
                    row.setAttribute('data-d-note', res.note);
                }
                actions.innerHTML = '';
                actions.appendChild(doneText('Resolved by you · just now'));
                row.classList.add('ss-inbox__item--done');
            }
            if (old && old !== status) {
                bumpTab(old, -1);
                bumpTab(status, 1);
            }
        }

        function pad(n) {
            return (n < 10 ? '0' : '') + n;
        }

        function toast(message, ok) {
            if (typeof window.ssRealtimeToast === 'function') {
                window.ssRealtimeToast(ok ? 'Alert updated' : 'Could not update', message, ok);
            }
        }

        function wireForms(scope) {
            scope.querySelectorAll('form[data-alert-form]').forEach(function (form) {
                if (form.getAttribute('data-wired')) {
                    return;
                }
                form.setAttribute('data-wired', '1');
                form.addEventListener('submit', function (e) {
                    if (!window.fetch || !window.URLSearchParams) {
                        return; // fallback: normal POST + redirect
                    }
                    e.preventDefault();
                    var row = form.closest('[data-alert-row]');
                    var btn = form.querySelector('button[type="submit"]');
                    if (btn) { btn.disabled = true; }
                    var body = new URLSearchParams(new FormData(form));
                    var csrf = form.querySelector('input[name="_csrf"]');
                    var note = form.querySelector('textarea[name="note"]');
                    // V4: getAttribute, because the form has an <input name="action"> that shadows form.action
                    fetch(form.getAttribute('action'), {
                        method: 'POST',
                        headers: {
                            'X-Requested-With': 'XMLHttpRequest',
                            'X-CSRF-TOKEN': csrf ? csrf.value : '',
                            'Accept': 'application/json',
                            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                        },
                        body: body.toString(),
                        credentials: 'same-origin'
                    }).then(function (r) {
                        return r.json().catch(function () { return { success: false, message: 'Unexpected response. Please reload.' }; });
                    }).then(function (res) {
                        if (typeof res.newAlertCount === 'number') {
                            setNavCount(res.newAlertCount);
                        }
                        if (res.success && row) {
                            applyStatus(row, res.status, { note: note ? note.value.trim() : '' });
                            toast(res.message, true);
                        } else {
                            if (btn) { btn.disabled = false; }
                            toast(res.message || 'Could not update this alert.', false);
                            if (res.status && row && res.status !== row.getAttribute('data-status')) {
                                applyStatus(row, res.status, null);
                            }
                        }
                    }).catch(function () {
                        if (btn) { btn.disabled = false; }
                        toast('Network error. Please try again.', false);
                    });
                });
            });
        }
        wireForms(list);

        // ---------- detail drawer ----------
        function addDt(dl, label, value) {
            if (!value) {
                return;
            }
            var dt = document.createElement('dt');
            dt.textContent = label;
            var dd = document.createElement('dd');
            dd.textContent = value;
            dl.appendChild(dt);
            dl.appendChild(dd);
        }

        function addStep(ol, label, when, extra) {
            var li = document.createElement('li');
            var strong = document.createElement('span');
            strong.className = 'ss-inbox__tl-step';
            strong.textContent = label;
            li.appendChild(strong);
            if (when) {
                var t = document.createElement('span');
                t.className = 'ss-inbox__tl-when';
                t.textContent = when;
                li.appendChild(t);
            }
            if (extra) {
                var p = document.createElement('span');
                p.className = 'ss-inbox__tl-note';
                p.textContent = extra;
                li.appendChild(p);
            }
            ol.appendChild(li);
        }

        function openDrawer(row) {
            if (!drawer) {
                return;
            }
            var g = function (k) { return row.getAttribute('data-d-' + k) || ''; };
            document.getElementById('alertDrawerTitle').textContent = g('title') || 'Alert detail';
            document.getElementById('alertDrawerMsg').textContent = g('message');
            var snap = document.getElementById('alertDrawerSnap');
            if (snap) {
                var snapSrc = g('snapshot');
                snap.hidden = !snapSrc;
                document.getElementById('alertDrawerSnapImg').src = snapSrc;
                document.getElementById('alertDrawerSnapLink').href = snapSrc || '#';
            }
            var dl = document.getElementById('alertDrawerList');
            dl.innerHTML = '';
            addDt(dl, 'Status', STATUS_TEXT[g('status')] || g('status'));
            addDt(dl, 'Severity', g('severity') ? g('severity').charAt(0).toUpperCase() + g('severity').slice(1) : '');
            addDt(dl, 'Type code', g('type'));
            addDt(dl, 'Device', g('device'));
            addDt(dl, 'Room', g('room'));
            addDt(dl, 'Booking', g('booking'));
            addDt(dl, 'Alert ID', row.getAttribute('data-alert-id'));
            var ol = document.getElementById('alertDrawerTimeline');
            ol.innerHTML = '';
            addStep(ol, 'Raised', g('created'));
            if (g('ackat') || g('ackby')) {
                addStep(ol, 'Acknowledged' + (g('ackby') ? ' by ' + g('ackby') : ''), g('ackat'));
            }
            if (g('resat') || g('resby')) {
                addStep(ol, 'Resolved' + (g('resby') ? ' by ' + g('resby') : ''), g('resat'), g('note'));
            }
            lastRow = row;
            drawer.classList.add('ss-drawer--open');
            drawer.setAttribute('aria-hidden', 'false');
            document.getElementById('alertDrawerTitle').focus();
        }

        function closeDrawer() {
            if (!drawer) {
                return;
            }
            drawer.classList.remove('ss-drawer--open');
            drawer.setAttribute('aria-hidden', 'true');
            if (lastRow && document.body.contains(lastRow)) {
                lastRow.focus();
            }
        }

        list.addEventListener('click', function (e) {
            if (e.target.closest('[data-alert-actions]')) {
                return;
            }
            var row = e.target.closest('[data-alert-row]');
            if (row) {
                openDrawer(row);
            }
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && drawer && drawer.classList.contains('ss-drawer--open')) {
                closeDrawer();
                return;
            }
            if (e.key === 'Enter' && e.target.matches && e.target.matches('[data-alert-row]')) {
                e.preventDefault();
                openDrawer(e.target);
            }
        });
        var closeBtn = drawer ? drawer.querySelector('[data-alert-close]') : null;
        if (closeBtn) {
            closeBtn.addEventListener('click', closeDrawer);
        }

        // ---------- realtime: pull fresh rows ----------
        var refreshing = false;
        function refreshFromServer() {
            if (refreshing || !window.fetch || !window.DOMParser) {
                return;
            }
            refreshing = true;
            fetch(window.location.href, { credentials: 'same-origin', headers: { 'Accept': 'text/html' } })
                .then(function (r) { return r.text(); })
                .then(function (html) {
                    var doc = new DOMParser().parseFromString(html, 'text/html');
                    // tab counts
                    doc.querySelectorAll('[data-tab-count]').forEach(function (c) {
                        var mine = document.querySelector('[data-tab-count="' + c.getAttribute('data-tab-count') + '"]');
                        if (mine) { mine.textContent = c.textContent; }
                    });
                    var fresh = doc.getElementById('alertList');
                    if (!fresh) {
                        return;
                    }
                    var added = [];
                    Array.prototype.slice.call(fresh.querySelectorAll('[data-alert-row]')).reverse().forEach(function (item) {
                        var id = item.getAttribute('data-alert-id');
                        var mine = list.querySelector('[data-alert-row][data-alert-id="' + id + '"]');
                        if (!mine) {
                            var node = document.importNode(item, true);
                            node.classList.add('ss-inbox__item--fresh');
                            list.insertBefore(node, list.firstChild);
                            added.push(node);
                        } else if (mine.getAttribute('data-status') !== item.getAttribute('data-status')) {
                            var repl = document.importNode(item, true);
                            list.replaceChild(repl, mine);
                            wireForms(repl);
                        }
                    });
                    added.forEach(function (n) {
                        wireForms(n);
                        setTimeout(function () { n.classList.remove('ss-inbox__item--fresh'); }, 4000);
                    });
                    if (added.length) {
                        var empty = document.getElementById('alertEmpty');
                        if (empty) { empty.remove(); }
                    }
                })
                .catch(function () { /* next event will try again */ })
                .then(function () { refreshing = false; });
        }

        window.addEventListener('smartstay:realtime', function (ev) {
            var d = ev.detail || {};
            if (d.event === 'ALERT_CREATED' || d.event === 'ALERT_UPDATED') {
                if (typeof d.newAlertCount === 'number') {
                    setNavCount(d.newAlertCount);
                }
                if (inbox.getAttribute('data-page') === '1' || inbox.getAttribute('data-page') === '') {
                    refreshFromServer();
                }
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
