/**
 * Unit tests for cancel-policy.js (48-hour cancellation line on room detail, Task R3).
 */
const assert = require('assert');
const policy = require('./cancel-policy.js');

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

function at(y, mo, d, h, mi, s) {
    return new Date(y, mo - 1, d, h, mi, s || 0);
}

console.log('==================================================');
console.log('     CANCELLATION POLICY LINE TEST (R3)           ');
console.log('==================================================');

console.log('\n--- 1. Parsing ---');
test('slotStart reads the start of "09:30–13:30"', () => {
    assert.strictEqual(policy.slotStart('2026-10-28', '09:30–13:30').getTime(), at(2026, 10, 28, 9, 30).getTime());
});
test('slotStart reads the start of an overnight range "20:00–09:00 (+1 day)"', () => {
    assert.strictEqual(policy.slotStart('2026-10-28', '20:00–09:00 (+1 day)').getTime(), at(2026, 10, 28, 20, 0).getTime());
});
test('parseLocal accepts "2026-10-25T22:00" without seconds', () => {
    assert.strictEqual(policy.parseLocal('2026-10-25T22:00').getTime(), at(2026, 10, 25, 22, 0).getTime());
});
test('missing date or time -> null', () => {
    assert.strictEqual(policy.slotStart('', '09:30'), null);
    assert.strictEqual(policy.slotStart('2026-10-28', ''), null);
});

console.log('\n--- 2. Boundary ---');
const start = at(2026, 10, 28, 9, 30);
test('exactly 48 h before -> refundable', () => {
    assert.strictEqual(policy.isRefundable(start, at(2026, 10, 26, 9, 30), 48), true);
});
test('47 h 59 min before -> non-refundable', () => {
    assert.strictEqual(policy.isRefundable(start, at(2026, 10, 26, 9, 31), 48), false);
});
test('deadline text is dd/MM/yyyy HH:mm', () => {
    assert.strictEqual(policy.formatDateTime(policy.deadline(start, 48)), '26/10/2026 09:30');
});

console.log('\n--- 3. Selections ---');
test('multi-slot uses the earliest slot', () => {
    const info = policy.describe('2026-10-28', [{ time: '14:00–18:00' }, { time: '09:30–13:30' }], at(2026, 10, 26, 12, 0), 48);
    assert.strictEqual(info.deadlineText, '26/10/2026 09:30');
    assert.strictEqual(info.refundable, false);
});
test('overnight deadline counts from its start (20:00), not its end', () => {
    const info = policy.describe('2026-10-28', [{ time: '20:00–09:00 (+1 day)' }], at(2026, 10, 26, 19, 59), 48);
    assert.strictEqual(info.deadlineText, '26/10/2026 20:00');
    assert.strictEqual(info.refundable, true);
});
test('slot less than 48 h away -> Non-refundable before paying', () => {
    const info = policy.describe('2026-10-28', [{ time: '09:30–13:30' }], at(2026, 10, 27, 9, 0), 48);
    assert.strictEqual(info.refundable, false);
});
test('empty selection -> null (line hidden)', () => {
    assert.strictEqual(policy.describe('2026-10-28', [], at(2026, 10, 20, 9, 0), 48), null);
});
test('missing hours falls back to 48', () => {
    assert.strictEqual(policy.formatDateTime(policy.deadline(start)), '26/10/2026 09:30');
});

console.log('\n==================================================');
console.log('TEST RESULTS: ' + passed + ' PASSED, ' + failed + ' FAILED');
console.log('==================================================');
if (failed > 0) {
    process.exit(1);
}
