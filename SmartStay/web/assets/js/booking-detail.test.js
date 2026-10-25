/**
 * Unit tests for booking-detail.js formatters and countdown hook.
 */
const assert = require('assert');
const utils = require('./booking-detail.js');

let passed = 0;
let failed = 0;

function test(name, fn) {
    try {
        fn();
        console.log('  [PASS] ' + name);
        passed++;
    } catch (err) {
        console.error('  [FAIL] ' + name, err.message);
        failed++;
    }
}

console.log('==================================================');
console.log('   CLIENT-SIDE JS FORMATTERS & COUNTDOWN TEST     ');
console.log('==================================================');

// 1. Currency formatting
console.log('\n--- 1. Currency Formatter ---');
test('formatCurrency formats 289000 to "289,000 VND"', () => {
    assert.strictEqual(utils.formatCurrency(289000), '289,000 VND');
});
test('formatCurrency formats 0 to "0 VND"', () => {
    assert.strictEqual(utils.formatCurrency(0), '0 VND');
});
test('formatCurrency handles null and empty string', () => {
    assert.strictEqual(utils.formatCurrency(null), '0 VND');
    assert.strictEqual(utils.formatCurrency(''), '0 VND');
});

// 2. Date/Time formatters
console.log('\n--- 2. Date & Time Formatter ---');
test('formatDate formats ISO string to "23/10/2026"', () => {
    assert.strictEqual(utils.formatDate('2026-10-23T16:30:00'), '23/10/2026');
});
test('formatTime formats ISO string to "16:30"', () => {
    assert.strictEqual(utils.formatTime('2026-10-23T16:30:00'), '16:30');
});
test('formatDateTime formats to "23/10/2026 · 16:30"', () => {
    assert.strictEqual(utils.formatDateTime('2026-10-23T16:30:00'), '23/10/2026 · 16:30');
});
test('formatWeekdayDateTime formats to "Fri, 23/10/2026 · 16:30"', () => {
    assert.strictEqual(utils.formatWeekdayDateTime('2026-10-23T16:30:00'), 'Fri, 23/10/2026 · 16:30');
});

// 3. Duration & Pluralization
console.log('\n--- 3. Duration & Pluralization ---');
test('formatDuration computes 3 hours correctly', () => {
    assert.strictEqual(utils.formatDuration('2026-10-23T16:30:00', '2026-10-23T19:30:00'), '3 hours');
});
test('formatDuration computes 1 hour correctly', () => {
    assert.strictEqual(utils.formatDuration('2026-10-23T16:30:00', '2026-10-23T17:30:00'), '1 hour');
});
test('pluralizeMinutes handles 1 minute correctly', () => {
    assert.strictEqual(utils.pluralizeMinutes(1), '1 minute');
});
test('pluralizeMinutes handles multiple minutes correctly', () => {
    assert.strictEqual(utils.pluralizeMinutes(14), '14 minutes');
    assert.strictEqual(utils.pluralizeMinutes(0), '0 minutes');
});

// 4. Live Countdown Hook
console.log('\n--- 4. Countdown Timer Hook ---');
test('Countdown timer displays mm:ss while active', () => {
    const mockTimer = { textContent: '' };
    const mockBanner = { classList: { add() {}, remove() {} }, innerHTML: '', setAttribute() {} };
    const futureDeadline = new Date(Date.now() + 14 * 60 * 1000 + 35 * 1000); // 14 mins 35 secs

    const timer = utils.initCountdownTimer({
        deadline: futureDeadline,
        timerElement: mockTimer,
        bannerElement: mockBanner
    });

    assert.ok(mockTimer.textContent.startsWith('14:'), `Expected timer to start with 14:, got ${mockTimer.textContent}`);
    assert.strictEqual(timer.isExpired(), false);
    timer.stop();
});

test('Countdown timer transitions to expired state at 0', () => {
    const mockTimer = { textContent: '' };
    let bannerClasses = ['alert', 'alert-warning'];
    const mockBanner = {
        classList: {
            add: (c) => bannerClasses.push(c),
            remove: (c) => { bannerClasses = bannerClasses.filter(x => x !== c); },
            contains: (c) => bannerClasses.includes(c)
        },
        innerHTML: '',
        setAttribute: () => {}
    };
    const mockButton = {
        disabled: false,
        classList: { add: () => {} },
        setAttribute: (attr, val) => { if (attr === 'disabled') mockButton.disabled = true; },
        textContent: 'Proceed to Payment'
    };
    const mockHelper = { innerHTML: '' };
    let expiredCallbackCalled = false;

    // Past deadline
    const pastDeadline = new Date(Date.now() - 5000);

    const timer = utils.initCountdownTimer({
        deadline: pastDeadline,
        timerElement: mockTimer,
        bannerElement: mockBanner,
        paymentButtonSelector: null,
        expiryHelperSelector: mockHelper,
        onExpire: () => { expiredCallbackCalled = true; }
    });

    // Manually pass button to triggerExpire
    mockBanner.innerHTML = '';
    timer.triggerExpire();

    assert.strictEqual(timer.isExpired(), true);
    assert.ok(bannerClasses.includes('alert-danger'), 'Banner should have alert-danger class');
    assert.ok(!bannerClasses.includes('alert-warning'), 'Banner should not have alert-warning class');
    assert.ok(mockBanner.innerHTML.includes('Hold expired, slots released'), 'Banner message should show hold expired');
    assert.ok(mockHelper.innerHTML.includes('Hold expired, slots released'), 'Helper text should show hold expired');
    assert.strictEqual(expiredCallbackCalled, true, 'onExpire callback should be invoked');
});

console.log('\n==================================================');
console.log(`TEST RESULTS: ${passed} PASSED, ${failed} FAILED`);
console.log('==================================================');

if (failed > 0) {
    process.exit(1);
}
