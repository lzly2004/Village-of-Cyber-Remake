import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync, readdirSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { createHelpGroups, helpSourceHashes } from '../src/helpContent.js'
import { helpGroups } from '../test-fixtures/help-source.js'
import { configurations, roles } from '../src/labels.js'
const read = path => readFileSync(new URL('../../' + path, import.meta.url), 'utf8')

test('all five Java help groups and all 36 topics are accounted for; originals cannot silently drift', () => {
  const ui = read('src/main/java/com/vocr/ui/UI.java')
  const groups = [...ui.matchAll(/INFO_SCENE_(\d)\(INFO_SCENE,\s*\d,\s*(\d+)\)/g)]
  assert.equal(groups.length, 5)
  assert.deepEqual(helpGroups.map(g => [g.id, g.topics.length]), groups.map(m => [m[1], Number(m[2])]))
  const expected = []
  for (const group of helpGroups) {
    expected.push('Info' + group.id + '.txt')
    assert.equal(group.title, read('src/main/resources/help/Info' + group.id + '.txt').replaceAll('\r\n', '\n'))
    group.topics.forEach((topic, i) => {
      assert.equal(topic.id, group.id + '-' + (i + 1))
      assert.equal(topic.source, 'Info' + topic.id + '-1.txt')
      expected.push('Info' + topic.id + '.txt', topic.source)
      assert.ok(topic.title && topic.body)
      assert.equal(topic.title, read('src/main/resources/help/Info' + topic.id + '.txt').replaceAll('\r\n', '\n'))
      assert.equal(topic.body, read('src/main/resources/help/' + topic.source).replaceAll('\r\n', '\n'))
    })
  }
  const actual = readdirSync(new URL('../../src/main/resources/help/', import.meta.url)).filter(n => /^Info.*\.txt$/.test(n)).sort()
  assert.equal(actual.length, 77)
  assert.deepEqual(expected.sort(), actual)
  assert.deepEqual(Object.keys(helpSourceHashes).sort(), actual)
  for (const file of actual) {
    const normalized = read('src/main/resources/help/' + file).replaceAll('\r\n', '\n')
    assert.equal(createHash('sha256').update(normalized).digest('hex'), helpSourceHashes[file], file + ' needs source review')
  }
})

test('seven public configuration rosters match every GameStatus role entry, independent of comments', () => {
  const java = read('src/main/java/com/vocr/core/GameStatus.java')
  const keys = read('src/main/java/com/vocr/domain/peiyi.java').replace(/\/\/[^\n]*/g, '').split('{')[1].split('}')[0].split(',').map(x => x.trim()).filter(Boolean).slice(1)
  const roleKeys = read('src/main/java/com/vocr/domain/Role.java').replace(/\/\/[^\n]*/g, '').split('{')[1].split('}')[0].split(';')[0].split(',').map(x => x.trim())
  assert.deepEqual(configurations.map(c => c.value), keys)
  assert.deepEqual(configurations.map(c => c.name), ['簡易', '通常', '妖狐', '狂信', '背徳', '猫又', '大型'])
  configurations.forEach((item, i) => {
    const block = java.split('case ' + item.value + ':')[1].split('break;')[0]
    const roleNumbers = block.match(/zysz\s*=\s*new int\[\]\{([^}]+)\}/)[1].split(',').map(Number)
    const expected = {}
    for (const n of roleNumbers) { assert.ok(roles[roleKeys[n]]); expected[roleKeys[n]] = (expected[roleKeys[n]] || 0) + 1 }
    assert.deepEqual(item.roleCounts, expected)
    assert.equal(item.playerCount, roleNumbers.length)
    assert.equal(Object.values(item.roleCounts).reduce((sum, n) => sum + n, 0), item.playerCount)
    assert.equal(item.sourceImage, 'images/game' + (i + 1) + '.png')
  })
})

test('desktop-only saving/replay/drag explanations have explicit Web differences', () => {
  for (const id of ['1-5', '1-6', '1-7', '3-2', '3-3', '3-4', '3-5', '5-3', '5-9']) {
    const topic = helpGroups.flatMap(g => g.topics).find(t => t.id === id)
    assert.ok(topic.webNote, id)
  }
  assert.match(helpGroups[0].topics[5].webNote, /服务重启、会话过期或释放后不能续局/)
  assert.match(helpGroups[0].topics[6].webNote, /右键并确认保存/);assert.match(helpGroups[0].topics[6].webNote, /不会自动保存/)
})

 test("missing desktop source is diagnosed instead of silently substituting a translation", () => { assert.throws(() => createHelpGroups({}), /HELP_SOURCE_MISSING/) })
