/**
 * Unit tests for tap-confirm.js (Task V3): node web/assets/js/tap-confirm.test.js
 */
'use strict';

var tc = require('./tap-confirm.js');

var passed = 0;
var failed = 0;

function check(cond, msg) {
    if (cond) {
        console.log('  [PASS] ' + msg);
        passed++;
    } else {
        console.error('  [FAIL] ' + msg);
        failed++;
    }
}

function fakeButton() {
    var set = {};
    return {
        classList: {
            add: function (c) { set[c] = true; },
            remove: function (c) { delete set[c]; },
            contains: function (c) { return !!set[c]; }
        },
        textEl: { textContent: 'Mark as clean' }
    };
}

function fakeClock() {
    var timers = [];
    return {
        setTimeout: function (fn, ms) { timers.push({ fn: fn, ms: ms, live: true }); return timers.length - 1; },
        clearTimeout: function (id) { if (timers[id]) { timers[id].live = false; } },
        fire: function () { timers.forEach(function (t) { if (t.live) { t.live = false; t.fn(); } }); },
        last: function () { return timers[timers.length - 1]; }
    };
}

console.log('TAP CONFIRM TESTS');

var b1 = fakeButton();
var c1 = fakeClock();
var m1 = tc.createTapConfirm(b1, { setTimeout: c1.setTimeout, clearTimeout: c1.clearTimeout });
check(m1.tap() === false, 'First tap does not submit');
check(m1.isArmed() && b1.classList.contains('ss-tap-confirm--armed'), 'First tap arms the button');
check(b1.textEl.textContent === 'Tap again to confirm', 'Armed label asks for a second tap');
check(c1.last().ms === 3000, 'Confirmation window is 3 seconds');
check(m1.tap() === true, 'Second tap inside the window submits');
check(!m1.isArmed() && b1.classList.contains('ss-tap-confirm--sending'), 'After submit the button shows sending state');

var b2 = fakeButton();
var c2 = fakeClock();
var m2 = tc.createTapConfirm(b2, { setTimeout: c2.setTimeout, clearTimeout: c2.clearTimeout });
m2.tap();
c2.fire();
check(!m2.isArmed() && !b2.classList.contains('ss-tap-confirm--armed'), 'Window running out resets the button');
check(b2.textEl.textContent === 'Mark as clean', 'Label goes back to the action name');
check(m2.tap() === false, 'A tap after the reset only arms again');

var b3 = fakeButton();
var c3 = fakeClock();
var m3 = tc.createTapConfirm(b3, { setTimeout: c3.setTimeout, clearTimeout: c3.clearTimeout });
m3.tap();
m3.reset();
check(!m3.isArmed(), 'reset() (e.g. on blur) disarms');
check(tc.WINDOW_MS === 3000, 'Exported window constant is 3000 ms');

console.log('TEST RESULTS: ' + passed + ' PASSED, ' + failed + ' FAILED');
if (failed > 0) {
    process.exit(1);
}
