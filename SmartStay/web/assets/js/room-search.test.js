/**
 * Unit tests for room-search.js (Rooms page filters v2, Task R4).
 */
const assert = require('assert');
const rs = require('./room-search.js');

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
console.log('        ROOM SEARCH FILTER TEST (R4)              ');
console.log('==================================================');

console.log('\n--- 1. Adjacent slots ---');
test('adjacent selection is kept', () => {
    assert.deepStrictEqual(rs.enforceAdjacent([0, 1], 1, true), [0, 1]);
});
test('checking Evening after Morning keeps only Evening', () => {
    assert.deepStrictEqual(rs.enforceAdjacent([0, 2], 2, true), [2]);
});
test('checking a box joins the run it belongs to', () => {
    assert.deepStrictEqual(rs.enforceAdjacent([0, 2, 3], 3, true), [2, 3]);
});
test('unchecking a middle slot keeps the earlier run', () => {
    assert.deepStrictEqual(rs.enforceAdjacent([0, 2], 1, false), [0]);
});
test('empty selection stays empty', () => {
    assert.deepStrictEqual(rs.enforceAdjacent([], 0, false), []);
});

console.log('\n--- 2. URL state (same format as RoomFilter.State.toQuery) ---');
test('full state', () => {
    assert.strictEqual(rs.buildQuery({
        date: '2026-10-25', weekStart: '2026-10-25', slots: ['S1', 'S2'],
        minCapacity: '3', floor: '1', sort: 'price', available: true
    }), 'date=2026-10-25&slots=S1,S2&minCapacity=3&floor=1&sort=price&available=1');
});
test('defaults left out', () => {
    assert.strictEqual(rs.buildQuery({ date: '2026-10-25', weekStart: '2026-10-25', slots: [], minCapacity: '', floor: '', sort: 'room', available: false }),
        'date=2026-10-25');
});
test('weekStart kept when it differs', () => {
    assert.strictEqual(rs.buildQuery({ date: '2026-10-25', weekStart: '2026-10-22', slots: [] }), 'date=2026-10-25&weekStart=2026-10-22');
});
test('parseQuery reads back every field', () => {
    const s = rs.parseQuery('?date=2026-10-25&slots=S2,s1&minCapacity=3&floor=1&sort=price_desc&available=1');
    assert.deepStrictEqual(s.slots, ['S1', 'S2']);
    assert.strictEqual(s.minCapacity, '3');
    assert.strictEqual(s.floor, '1');
    assert.strictEqual(s.sort, 'price_desc');
    assert.strictEqual(s.available, true);
});
test('parseQuery ignores unknown slot codes', () => {
    assert.deepStrictEqual(rs.parseQuery('slots=S9,S4').slots, ['S4']);
});
test('round trip buildQuery(parseQuery(q)) === q', () => {
    const q = 'date=2026-10-25&slots=S3,S4&floor=2&available=1';
    assert.strictEqual(rs.buildQuery(rs.parseQuery(q)), q);
});

console.log('\n==================================================');
console.log('TEST RESULTS: ' + passed + ' PASSED, ' + failed + ' FAILED');
console.log('==================================================');
if (failed > 0) {
    process.exit(1);
}
