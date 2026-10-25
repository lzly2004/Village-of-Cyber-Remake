import test from 'node:test'
import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { publicInformationDiagnostics } from '../tests/public-information-diagnostics.js'

test('public-info failed-step evidence is bounded, redacts session/query and survives page loss', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'vocr-diagnostics-'))
  try {
    const page = new EventEmitter()
    page.evaluate = async () => { throw Error('closed') }
    page.screenshot = async () => { throw Error('closed') }
    const attachments = []
    const info = { status: 'timedOut', expectedStatus: 'passed', outputPath: name => join(directory, name), attach: async (name, item) => attachments.push({name,...item}) }
    const diagnostic = publicInformationDiagnostics(page, info)
    for (let i = 0; i < 45; i++) {
      const req = { method: () => 'GET', url: () => 'http://localhost/api/v1/games/PRIVATE_ID?eventCursor=SECRET' }
      page.emit('request', req); page.emit('response', { request: () => req, status: () => 200 })
    }
    await diagnostic.step('assert earlier reports', async () => {})
    diagnostic.snapshot({ phase: 'DAY_DECISION', revision: 7, eventCursor: 0, nextEventCursor: 8,
      presentedEvents: [], view: { gameDay: 2 }, actualRole: 'SECRET_ROLE' })
    await assert.rejects(diagnostic.step('assert current reports', async () => { throw Error('failed assertion') }))
    await diagnostic.finish()
    const raw = await readFile(join(directory, 'public-information-diagnostic.json'), 'utf8')
    const state = JSON.parse(raw)
    assert.equal(state.network.length, 40)
    assert.equal(state.current, 'assert current reports')
    assert.equal(state.lastCompleted, 'assert earlier reports')
    assert.equal(state.latestSnapshot.phase, 'DAY_DECISION')
    assert.equal(state.browser.unavailable, 'page closed/crashed')
    assert.ok(Number.isFinite(state.currentElapsedMs))
    assert.doesNotMatch(raw, /PRIVATE_ID|SECRET|actualRole/)
    assert.deepEqual(attachments.map(a => a.name), ['public-information-diagnostic', 'public-information-screenshot-unavailable'])
    assert.equal(page.listenerCount('request'), 0)
  } finally { await rm(directory, {recursive:true,force:true}) }
})

test('successful public-info test adds no capture, request, wait or attachments', async () => {
  const page = new EventEmitter()
  page.evaluate = () => { throw Error('must not capture') }
  const diagnostic = publicInformationDiagnostics(page, {status:'passed',expectedStatus:'passed'})
  assert.equal(await diagnostic.step('assert done', async () => 42), 42)
  await diagnostic.finish()
  assert.equal(page.listenerCount('request'), 0)
})
