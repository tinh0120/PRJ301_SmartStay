/**
 * Room search helpers - Tini SmartStay (Task R4)
 *
 * 1. [data-slot-group] (home hero + rooms form): only adjacent slots can be checked, the same
 *    rule as slot-matrix.js / PricingService (Morning+Afternoon ok, Morning+Evening not).
 * 2. #roomsFilterForm + #roomsResults: any change re-queries /rooms after 300 ms, swaps the
 *    server-rendered results in place and keeps the state in the URL (history.replaceState).
 *    Without JS the form and links work as plain GET navigation.
 * 3. #matrix-calendar[data-preselect-slots] (room detail): clicks the passed slots once so the
 *    existing slot-matrix.js logic selects them (its DOM is not touched).
 */
(function (root, factory) {
    if (typeof module === 'object' && module.exports) {
        module.exports = factory();
    } else {
        root.RoomSearch = factory();
        root.RoomSearch.init();
    }
}(typeof self !== 'undefined' ? self : this, function () {
    'use strict';

    var SLOT_ORDER = ['S1', 'S2', 'S3', 'S4'];
    var DEBOUNCE_MS = 300;

    /**
     * Keeps only adjacent slot indexes (0..3).
     * When the user checks a box that breaks adjacency, the run containing that box wins;
     * when unchecking a middle box splits the run, the earlier run is kept.
     */
    function enforceAdjacent(checked, changedIndex, changedChecked) {
        var sorted = checked.slice().sort(function (a, b) { return a - b; });
        var runs = [];
        sorted.forEach(function (i) {
            var last = runs[runs.length - 1];
            if (last && i === last[last.length - 1] + 1) {
                last.push(i);
            } else {
                runs.push([i]);
            }
        });
        if (runs.length <= 1) {
            return sorted;
        }
        if (changedChecked) {
            for (var r = 0; r < runs.length; r++) {
                if (runs[r].indexOf(changedIndex) >= 0) {
                    return runs[r];
                }
            }
        }
        return runs[0];
    }

    /** Same query string as util.RoomFilter.State.toQuery (default values left out). */
    function buildQuery(s) {
        var parts = [];
        function add(k, v) {
            parts.push(k + '=' + encodeURIComponent(v).replace(/%2C/g, ','));
        }
        if (s.date) { add('date', s.date); }
        if (s.weekStart && s.weekStart !== s.date) { add('weekStart', s.weekStart); }
        if (s.slots && s.slots.length) { add('slots', s.slots.join(',')); }
        if (s.minCapacity) { add('minCapacity', s.minCapacity); }
        if (s.floor !== undefined && s.floor !== null && s.floor !== '') { add('floor', s.floor); }
        if (s.sort && s.sort !== 'room') { add('sort', s.sort); }
        if (s.available) { add('available', '1'); }
        return parts.join('&');
    }

    /** Reads {date, weekStart, slots, minCapacity, floor, sort, available} from a query string. */
    function parseQuery(search) {
        var out = { slots: [] };
        String(search || '').replace(/^\?/, '').split('&').forEach(function (pair) {
            if (!pair) { return; }
            var kv = pair.split('=');
            var k = decodeURIComponent(kv[0]);
            var v = decodeURIComponent((kv[1] || '').replace(/\+/g, ' '));
            if (k === 'slots' || k === 'slot') {
                v.split(',').forEach(function (c) {
                    c = c.trim().toUpperCase();
                    if (SLOT_ORDER.indexOf(c) >= 0 && out.slots.indexOf(c) < 0) { out.slots.push(c); }
                });
            } else if (k === 'available') {
                out.available = (v === '1' || v === 'true');
            } else {
                out[k] = v;
            }
        });
        out.slots.sort(function (a, b) { return SLOT_ORDER.indexOf(a) - SLOT_ORDER.indexOf(b); });
        return out;
    }

    // ---------------------------------------------------------------------
    // 1. Adjacent slots in every slot group
    // ---------------------------------------------------------------------
    function initSlotGroups() {
        var groups = document.querySelectorAll('[data-slot-group]');
        for (var g = 0; g < groups.length; g++) {
            (function (group) {
                var boxes = group.querySelectorAll('input[type="checkbox"][name="slots"]');
                var form = group.closest ? group.closest('form') : null;
                var hint = form ? form.querySelector('[data-slot-hint]') : null;
                var hintText = hint ? hint.textContent : '';
                var hintTimer = null;
                for (var b = 0; b < boxes.length; b++) {
                    boxes[b].addEventListener('change', function (e) {
                        var changed = e.target;
                        var checked = [];
                        var changedIndex = SLOT_ORDER.indexOf(changed.value);
                        for (var i = 0; i < boxes.length; i++) {
                            if (boxes[i].checked) { checked.push(SLOT_ORDER.indexOf(boxes[i].value)); }
                        }
                        var keep = enforceAdjacent(checked, changedIndex, changed.checked);
                        if (keep.length !== checked.length) {
                            for (var j = 0; j < boxes.length; j++) {
                                boxes[j].checked = keep.indexOf(SLOT_ORDER.indexOf(boxes[j].value)) >= 0;
                            }
                            if (hint) {
                                hint.textContent = 'Time slots must be next to each other, so the other slot was cleared.';
                                hint.classList.add('ss-rooms-filter__hint--warn');
                                clearTimeout(hintTimer);
                                hintTimer = setTimeout(function () {
                                    hint.textContent = hintText;
                                    hint.classList.remove('ss-rooms-filter__hint--warn');
                                }, 4000);
                            }
                        }
                    });
                }
            })(groups[g]);
        }
    }

    // ---------------------------------------------------------------------
    // 2. Rooms page: auto-apply + in-place results
    // ---------------------------------------------------------------------
    function initRoomsPage() {
        var form = document.getElementById('roomsFilterForm');
        var region = document.getElementById('roomsResults');
        if (!form || !region || !window.fetch || !window.DOMParser || !window.history) {
            return;
        }
        var live = document.getElementById('roomsLive');
        var path = form.getAttribute('action');
        var timer = null;
        var requestId = 0;

        function formState() {
            var slots = [];
            var boxes = form.querySelectorAll('input[name="slots"]');
            for (var i = 0; i < boxes.length; i++) {
                if (boxes[i].checked) { slots.push(boxes[i].value); }
            }
            return {
                date: form.elements.date ? form.elements.date.value : '',
                weekStart: form.elements.weekStart ? form.elements.weekStart.value : '',
                slots: slots,
                minCapacity: form.elements.minCapacity ? form.elements.minCapacity.value : '',
                floor: form.elements.floor ? form.elements.floor.value : '',
                sort: form.elements.sort ? form.elements.sort.value : 'room',
                available: form.elements.available ? form.elements.available.checked : false
            };
        }

        function syncForm(url) {
            var s = parseQuery(url.split('?')[1] || '');
            if (form.elements.date && s.date) { form.elements.date.value = s.date; }
            if (form.elements.weekStart) { form.elements.weekStart.value = s.weekStart || s.date || form.elements.weekStart.value; }
            var boxes = form.querySelectorAll('input[name="slots"]');
            for (var i = 0; i < boxes.length; i++) { boxes[i].checked = s.slots.indexOf(boxes[i].value) >= 0; }
            if (form.elements.minCapacity) { form.elements.minCapacity.value = s.minCapacity || ''; }
            if (form.elements.floor) { form.elements.floor.value = s.floor || ''; }
            if (form.elements.sort) { form.elements.sort.value = s.sort || 'room'; }
            if (form.elements.available) { form.elements.available.checked = !!s.available; }
        }

        function load(url) {
            var id = ++requestId;
            region.classList.add('ss-rooms-results--loading');
            region.setAttribute('aria-busy', 'true');
            fetch(url, { credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' } })
                .then(function (res) {
                    if (!res.ok) { throw new Error('HTTP ' + res.status); }
                    return res.text();
                })
                .then(function (html) {
                    if (id !== requestId) { return; } // a newer request is on its way
                    var doc = new DOMParser().parseFromString(html, 'text/html');
                    var fresh = doc.getElementById('roomsResults');
                    if (!fresh) { throw new Error('No results in response'); }
                    region.innerHTML = fresh.innerHTML;
                    region.setAttribute('data-status-line', fresh.getAttribute('data-status-line') || '');
                    history.replaceState(null, '', url);
                    if (live) { live.textContent = fresh.getAttribute('data-status-line') || ''; }
                    region.classList.remove('ss-rooms-results--loading');
                    region.removeAttribute('aria-busy');
                })
                .catch(function () {
                    if (id === requestId) { window.location.href = url; }
                });
        }

        function scheduleApply() {
            clearTimeout(timer);
            timer = setTimeout(function () {
                var q = buildQuery(formState());
                load(q ? path + '?' + q : path);
            }, DEBOUNCE_MS);
        }

        form.addEventListener('change', scheduleApply);
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            clearTimeout(timer);
            var q = buildQuery(formState());
            load(q ? path + '?' + q : path);
        });

        // Strip days, prev/next, chip removal, clear all
        region.addEventListener('click', function (e) {
            var link = e.target.closest ? e.target.closest('a[data-rooms-nav]') : null;
            if (!link || e.ctrlKey || e.metaKey || e.shiftKey || e.button === 1) { return; }
            e.preventDefault();
            var url = link.getAttribute('href');
            syncForm(url);
            load(url);
        });
    }

    // ---------------------------------------------------------------------
    // 3. Room detail: preselect slots passed from the Rooms page
    // ---------------------------------------------------------------------
    function initPreselect() {
        var container = document.getElementById('matrix-calendar');
        if (!container) { return; }
        var date = container.getAttribute('data-preselect-date');
        var codes = (container.getAttribute('data-preselect-slots') || '').split(',').filter(Boolean);
        if (!date || !codes.length) { return; }
        codes.forEach(function (code) {
            var btn = container.querySelector('.matrix-slot-available[data-date="' + date + '"][data-code="' + code + '"]');
            if (btn && !btn.disabled) {
                btn.click();
            }
        });
    }

    function init() {
        if (typeof document === 'undefined') { return; }
        var run = function () {
            initSlotGroups();
            initRoomsPage();
            initPreselect();
        };
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', run);
        } else {
            run();
        }
    }

    return {
        enforceAdjacent: enforceAdjacent,
        buildQuery: buildQuery,
        parseQuery: parseQuery,
        init: init
    };
}));
