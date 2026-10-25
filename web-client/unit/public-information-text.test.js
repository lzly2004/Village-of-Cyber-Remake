import test from 'node:test'
import assert from 'node:assert/strict'
import { skillResultText, suspicionText, executionText } from '../src/publicInformationText.js'

const name = number => `${number}番`
for (const [color, suffix] of [['WHITE', '○'], ['BLACK', '●'], ['NOT_APPLICABLE', '']]) {
  test(`skill report ${color} preserves supplied day and target`, () => {
    assert.equal(skillResultText({ skillDay: 2, targetPlayerNumber: 7, color }, name), '第2日：7番' + suffix)
  })
}
test('unknown result does not silently become white or guard', () => {
  assert.equal(skillResultText({ skillDay: 1, targetPlayerNumber: 2, color: 'NEW' }, name), '第1日：2番（未対応の結果）')
})
test('suspicion preserves supplied target order and all four comparison categories', () => {
  assert.equal(suspicionText({ playerNumber: 1, targets: [3, 2, 4], comparisons: ['CLOSE', 'SLIGHTLY_GREATER'] }, name), '1番：3番=2番≧4番')
  assert.equal(suspicionText({ playerNumber: 1, targets: [4, 2, 3], comparisons: ['GREATER', 'MUCH_GREATER'] }, name), '1番：4番>2番≫3番')
})
test('empty suspicion row is blank, not an observer with fabricated no-suspicion conclusion', () => {
  assert.equal(suspicionText({ playerNumber: 1, targets: [], comparisons: [] }, name), '')
})
test('unknown comparison has explicit fallback, never an invented strength', () => {
  assert.equal(suspicionText({ playerNumber: 1, targets: [3, 2], comparisons: ['NEW'] }, name), '1番：3番？2番')
})
test('public death prefixes and day causes are presentation only', () => {
  assert.equal(executionText({ playerNumber: 2, prefixPlayers: [], cause: 'EXECUTION' }, name), '2番')
  assert.equal(executionText({ playerNumber: 2, prefixPlayers: [3], cause: 'DAY_CAT_CURSE' }, name), '3番+2番（猫呪）')
  assert.equal(executionText({ playerNumber: 2, prefixPlayers: [4], cause: 'DAY_PURSUED' }, name), '4番+2番（後追）')
  assert.equal(executionText({ playerNumber: 2, prefixPlayers: [], cause: 'NEW' }, name), '2番（未対応の死因）')
})
