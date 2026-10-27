/**
 * Mobile booking bar empty state - Tini SmartStay (Task R5)
 *
 * slot-matrix.js hides #mobile-booking-bar when nothing is selected. On phones the bar now stays
 * visible: with no slot it shows "Select a slot" and a disabled Book Now button. This file only
 * watches the selection slot-matrix.js writes into #bookingSlotCodes; it does not change
 * slot-matrix.js, its totals or its DOM.
 */
(function () {
    'use strict';

    function init() {
        var bar = document.getElementById('mobile-booking-bar');
        var codes = document.getElementById('bookingSlotCodes');
        var book = document.getElementById('mobileBarBookBtn');
        if (!bar || !codes) {
            return;
        }
        document.body.classList.add('ss-has-bookbar');

        // V2: combo ladder step + a short bump on the totals when the amount changes
        var tiers = document.querySelectorAll('[data-combo-tier]');
        var totals = ['mobile-bar-price', 'sticky-total'].map(function (id) {
            return document.getElementById(id);
        }).filter(Boolean);
        var lastTier = 0;
        var lastTotal = totals.length ? totals[0].textContent : '';

        function bump(el) {
            el.classList.remove('ss-total-bump');
            void el.offsetWidth; // restart the animation
            el.classList.add('ss-total-bump');
        }

        function updateLadder(count) {
            var reached = 0;
            tiers.forEach(function (t) {
                var n = parseInt(t.getAttribute('data-combo-tier'), 10);
                if (count >= n) {
                    reached = Math.max(reached, n);
                }
            });
            tiers.forEach(function (t) {
                var on = parseInt(t.getAttribute('data-combo-tier'), 10) === reached;
                t.classList.toggle('ss-combo-ladder__tier--active', on);
                if (on && reached !== lastTier) {
                    t.classList.remove('ss-combo-ladder__tier--pulse');
                    void t.offsetWidth;
                    t.classList.add('ss-combo-ladder__tier--pulse');
                }
            });
            lastTier = reached;
        }

        function update() {
            var picked = codes.querySelectorAll('input[name="slotCodes"]').length;
            var empty = picked === 0;
            // slot-matrix.js shows #matrix-error when the slots are not next to each other: no combo then
            var err = document.getElementById('matrix-error');
            var broken = err && !err.classList.contains('d-none');
            updateLadder(broken ? 0 : picked);
            if (totals.length && totals[0].textContent !== lastTotal) {
                lastTotal = totals[0].textContent;
                totals.forEach(bump);
            }
            bar.classList.toggle('ss-bookbar-m--empty', empty);
            if (book) {
                book.disabled = empty;
                if (empty) {
                    book.setAttribute('aria-disabled', 'true');
                } else {
                    book.removeAttribute('aria-disabled');
                }
            }
            // V4: screen readers hear which slot tiles are picked (slot-matrix.js sets the class)
            document.querySelectorAll('.matrix-slot-available[aria-pressed]').forEach(function (b) {
                b.setAttribute('aria-pressed', b.classList.contains('matrix-slot-selected') ? 'true' : 'false');
            });
        }

        if (typeof MutationObserver !== 'undefined') {
            new MutationObserver(update).observe(codes, { childList: true });
        }
        update();
        initSheet();
    }

    /**
     * V4: the mobile booking sheet behaves as a modal dialog for keyboard and screen-reader users.
     * slot-matrix.js still opens and closes it (is-open class); this only reacts to that class:
     * focus moves into the sheet, Tab stays inside, Esc closes it, focus returns to the opener.
     */
    function initSheet() {
        var sheet = document.getElementById('mobile-booking-drawer');
        var closeBtn = document.getElementById('mobileDrawerCloseBtn');
        var toggle = document.getElementById('mobileBarToggleBtn');
        if (!sheet || !closeBtn || typeof MutationObserver === 'undefined') {
            return;
        }
        var opener = null;
        var wasOpen = false;

        function focusables() {
            return Array.prototype.filter.call(
                sheet.querySelectorAll('a[href], button:not([disabled]), input:not([disabled]), select, textarea, [tabindex]:not([tabindex="-1"])'),
                function (el) { return el.offsetParent !== null; });
        }

        function sync() {
            var open = sheet.classList.contains('is-open');
            if (open === wasOpen) {
                return;
            }
            wasOpen = open;
            if (toggle) {
                toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
            }
            if (open) {
                sheet.setAttribute('aria-modal', 'true');
                opener = document.activeElement;
                closeBtn.focus();
            } else {
                sheet.removeAttribute('aria-modal');
                if (opener && document.body.contains(opener) && opener.offsetParent !== null) {
                    opener.focus();
                }
                opener = null;
            }
        }

        new MutationObserver(sync).observe(sheet, { attributes: true, attributeFilter: ['class'] });

        sheet.addEventListener('keydown', function (e) {
            if (!sheet.classList.contains('is-open')) {
                return;
            }
            if (e.key === 'Escape' || e.key === 'Esc') {
                e.preventDefault();
                closeBtn.click();
                return;
            }
            if (e.key !== 'Tab') {
                return;
            }
            var items = focusables();
            if (!items.length) {
                return;
            }
            var first = items[0];
            var last = items[items.length - 1];
            if (e.shiftKey && document.activeElement === first) {
                e.preventDefault();
                last.focus();
            } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
