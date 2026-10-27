/**
 * Two-step "Tap again to confirm" button - Tini SmartStay (Task V3, staff "Mark as clean")
 *
 * First tap: the button turns amber, says "Tap again to confirm" and a 3 s bar runs out.
 * Second tap within the window submits the form; otherwise the button resets. No browser dialog.
 * The state machine is exported for tap-confirm.test.js (node, no DOM needed).
 */
(function (root) {
    'use strict';

    var WINDOW_MS = 3000;

    /**
     * @param {object} btn   element-like: classList, setAttribute, textEl (optional) and form (optional)
     * @param {object} opts  { windowMs, setTimeout, clearTimeout, armedText, idleText, submit }
     * @returns {{tap: function(): boolean, isArmed: function(): boolean, reset: function()}}
     *          tap() returns true when this tap should submit the form.
     */
    function createTapConfirm(btn, opts) {
        opts = opts || {};
        var windowMs = opts.windowMs || WINDOW_MS;
        var setT = opts.setTimeout || setTimeout;
        var clearT = opts.clearTimeout || clearTimeout;
        var idleText = opts.idleText || 'Mark as clean';
        var armedText = opts.armedText || 'Tap again to confirm';
        var armed = false;
        var timer = null;

        function label(text) {
            if (btn.textEl) {
                btn.textEl.textContent = text;
            }
        }

        function reset() {
            armed = false;
            if (timer !== null) {
                clearT(timer);
                timer = null;
            }
            btn.classList.remove('ss-tap-confirm--armed');
            label(idleText);
        }

        function tap() {
            if (armed) {
                if (timer !== null) {
                    clearT(timer);
                    timer = null;
                }
                armed = false;
                btn.classList.remove('ss-tap-confirm--armed');
                btn.classList.add('ss-tap-confirm--sending');
                return true;
            }
            armed = true;
            btn.classList.add('ss-tap-confirm--armed');
            label(armedText);
            timer = setT(reset, windowMs);
            return false;
        }

        return {
            tap: tap,
            reset: reset,
            isArmed: function () { return armed; }
        };
    }

    function init() {
        if (typeof document === 'undefined') {
            return;
        }
        document.querySelectorAll('[data-tap-confirm]').forEach(function (el) {
            var form = el.closest('form');
            var text = el.querySelector('.ss-tap-confirm__text');
            var machine = createTapConfirm({
                classList: el.classList,
                textEl: text
            }, { idleText: text ? text.textContent : undefined });
            el.addEventListener('click', function (e) {
                e.preventDefault();
                if (machine.tap() && form) {
                    el.disabled = true;
                    form.submit();
                }
            });
            el.addEventListener('blur', function () {
                if (machine.isArmed()) {
                    machine.reset();
                }
            });
        });
    }

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = { createTapConfirm: createTapConfirm, WINDOW_MS: WINDOW_MS };
    } else if (typeof document !== 'undefined') {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', init);
        } else {
            init();
        }
    }
})(this);
