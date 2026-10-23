// Read-only cross-language fixture contract. This is NOT the browser import implementation.
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
const root = new URL('../../', import.meta.url)
const text = path => readFileSync(new URL(path, root), 'utf8')
const current = () => JSON.parse(text('src/test/resources/replay/exchange-v3.json'))

test('JS JSON round trip preserves every known structured field and each opaque raw record', () => {
  const slots = current()
  assert.equal(slots.length, 9)
  assert.equal(slots.filter(Boolean).length, 1)
  const save = slots[0]
  assert.equal(save.version, 3)
  assert.equal(save.totalEvents, save.rawRecords.length)
  assert.deepEqual(JSON.parse(JSON.stringify(slots)), slots)
  assert.match(save.rawRecords.find(s => s.startsWith('JSON|')), /"seed": 9007199254740993/)
  assert.match(save.rawRecords.find(s => s.includes('ä/ü')), /ä\/ü\n/)
  assert.equal(save.daySnapshots.length, 4)
  assert.equal(save.totalDays, 4)
  assert.match(save.rawRecords.at(-1), /day=3$/)
  assert.deepEqual(save.daySnapshots[1].greyTargetCharNums, [1, 3])
  assert.deepEqual(save.daySnapshots[1].designatedTargetCharNums, [28])
  assert.deepEqual(save.daySnapshots[1].players.map(p => p.characterNumber), [27, 28, 44])
  assert.deepEqual(save.daySnapshots[1].voteRounds[0].votes, [[1,2],[2,1],[3,0]])
  assert.equal(save.daySnapshots[1].voteRounds.length, 2)
  assert.deepEqual(save.daySnapshots[3].players[0].skillTargets, [0,0,3,0])
  assert.deepEqual(save.daySnapshots[3].players[2].skillTargets, [0,2,1,0])
})

test('fixture ordinal meanings remain Java ordinals and array indices remain player indices', () => {
  const enumMembers = path => text(path).replace(/\/\/[^\n]*/g, '').split('{')[1].split('}')[0].split(';')[0].split(',').map(s=>s.trim()).filter(Boolean)
  const roles = enumMembers('src/main/java/com/vocr/domain/Role.java')
  const characters = enumMembers('src/main/java/com/vocr/domain/CharacterEnglishName.java')
  const configurations = enumMembers('src/main/java/com/vocr/domain/peiyi.java')
  const save = current()[0]
  assert.equal(configurations[save.peiyiOrdinal], save.peiyiName)
  assert.deepEqual(save.daySnapshots[0].players.map(p=>roles[p.actualRole]), ['zhan','renlang','kuangren'])
  assert.deepEqual(save.daySnapshots[0].players.map(p=>characters[p.characterNumber]), ['Volker','Eva','Helmut'])
  assert.deepEqual(save.daySnapshots[0].players.map(p=>p.playerIndex), [1,2,3])
})

test('legacy v1/raw source remains readable as fixture input; BOM/CRLF do not turn record strings into lines', () => {
  const legacy = JSON.parse(text('src/test/resources/replay/legacy-v1.json'))
  assert.equal(legacy[0].version, 1)
  assert.equal(legacy[0].legacyOnly, 'ignored')
  assert.equal(legacy[0].daySnapshots[0].players[0].skillTargets, undefined)
  const raw = text('src/test/resources/replay/legacy-record-v0.txt')
  assert.match(raw, /END\|result=FOX_WIN\|day=3/)
  // Exercise only JSON transport, not a second migration algorithm.
  const crlf = '\ufeff' + JSON.stringify(current(), null, 2).replaceAll('\n', '\r\n')
  assert.deepEqual(JSON.parse(crlf.replace(/^\ufeff/, '')), current())
  assert.throws(()=>JSON.parse(crlf.slice(1, -20)), SyntaxError)
})
