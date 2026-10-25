/**
 * Unit tests for mockpay.js utilities, formatters, and countdown hook.
 */
const assert = require('assert');
const MockPay = require('./mockpay.js');

let passed = 0;
let failed = 0;

async function test(name, fn) {
    try {
        await fn();
        console.log('  [PASS] ' + name);
        passed++;
    } catch (err) {
        console.error('  [FAIL] ' + name, err.message);
        failed++;
    }
}

async function runTests() {
    console.log('==================================================');
    console.log('       MOCKPAY CLIENT UTILITIES UNIT TESTS        ');
    console.log('==================================================');

    // 1. Time formatting
    console.log('\n--- 1. Time Formatter (mm:ss) ---');
    await test('formatTime converts 276 seconds to "04:36"', () => {
        assert.strictEqual(MockPay.formatTime(276), '04:36');
    });
    await test('formatTime converts 60 seconds to "01:00"', () => {
        assert.strictEqual(MockPay.formatTime(60), '01:00');
    });
    await test('formatTime converts 5 seconds to "00:05"', () => {
        assert.strictEqual(MockPay.formatTime(5), '00:05');
    });
    await test('formatTime converts 0 seconds to "00:00"', () => {
        assert.strictEqual(MockPay.formatTime(0), '00:00');
    });
    await test('formatTime handles negative and null inputs gracefully', () => {
        assert.strictEqual(MockPay.formatTime(-10), '00:00');
        assert.strictEqual(MockPay.formatTime(null), '00:00');
    });

    // 2. Currency formatting
    console.log('\n--- 2. Currency Formatter (VND) ---');
    await test('formatCurrency formats 289000 to "289,000 VND"', () => {
        assert.strictEqual(MockPay.formatCurrency(289000), '289,000 VND');
    });
    await test('formatCurrency formats 0 to "0 VND"', () => {
        assert.strictEqual(MockPay.formatCurrency(0), '0 VND');
    });
    await test('formatCurrency handles null and empty string', () => {
        assert.strictEqual(MockPay.formatCurrency(null), '0 VND');
        assert.strictEqual(MockPay.formatCurrency(''), '0 VND');
    });

    // 3. Countdown Hook & Progress
    console.log('\n--- 3. Countdown Hook & Progress ---');
    await test('Countdown initializes and renders mm:ss correctly', () => {
        const mockTimer = { textContent: '' };
        const mockProgress = {
            style: { width: '' },
            classList: {
                classes: new Set(),
                add(c) { this.classes.add(c); },
                remove(c) { this.classes.delete(c); },
                contains(c) { return this.classes.has(c); }
            },
            setAttribute(k, v) { this[k] = v; }
        };

        const countdown = MockPay.initCountdown({
            initialSeconds: 276,
            totalDuration: 900,
            timerElement: mockTimer,
            progressElement: mockProgress
        });

        assert.strictEqual(mockTimer.textContent, '04:36');
        assert.strictEqual(countdown.isExpired(), false);
        assert.strictEqual(countdown.getTimeRemaining(), 276);
        countdown.stop();
    });

    await test('Countdown triggers expireNow and switches to expired state', () => {
        const mockTimer = { textContent: '' };
        let expiredFired = false;
        const mockProgress = {
            style: { width: '' },
            classList: {
                classes: new Set(),
                add(c) { this.classes.add(c); },
                remove(c) { this.classes.delete(c); },
                contains(c) { return this.classes.has(c); }
            },
            setAttribute(k, v) { this[k] = v; }
        };
        const mockAria = { textContent: '' };

        const countdown = MockPay.initCountdown({
            initialSeconds: 30,
            totalDuration: 900,
            timerElement: mockTimer,
            progressElement: mockProgress,
            ariaElement: mockAria,
            onExpire: () => { expiredFired = true; }
        });

        countdown.expireNow();
        assert.strictEqual(mockTimer.textContent, '00:00');
        assert.strictEqual(countdown.isExpired(), true);
        assert.strictEqual(expiredFired, true);
        assert.ok(mockProgress.classList.contains('bg-danger'));
        assert.ok(mockAria.textContent.includes('Payment session has expired'));
    });

    // 4. Copy to Clipboard
    console.log('\n--- 4. Copy to Clipboard ---');
    await test('copyToClipboard invokes callback with true when navigator.clipboard is available', async () => {
        let written = null;
        Object.defineProperty(navigator, 'clipboard', {
            value: {
                writeText: (t) => {
                    written = t;
                    return Promise.resolve();
                }
            },
            configurable: true,
            writable: true
        });

        const result = await new Promise((resolve) => {
            MockPay.copyToClipboard('BK000146', (success) => {
                resolve(success);
            });
        });

        assert.strictEqual(result, true);
        assert.strictEqual(written, 'BK000146');
    });

    console.log('\n==================================================');
    console.log(`TEST RESULTS: ${passed} PASSED, ${failed} FAILED`);
    console.log('==================================================');
    if (failed > 0) process.exit(1);
}

runTests();
