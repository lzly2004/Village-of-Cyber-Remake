import {expectNoEnabledVote} from '../test-fixtures/desktop-navigation.js'
import { test, expect } from '../test-fixtures/presentation-test.js'
import { readFileSync } from 'node:fs'

const catalog = JSON.parse(readFileSync(new URL('../../src/main/resources/lines/event_lines.json', import.meta.url), 'utf8'))
const storageKey = 'vocr.guest.v1'
const saved = page => page.evaluate(key => JSON.parse(sessionStorage.getItem(key)), storageKey)
const event = (eventname, ch2 = 'Erich', ch1 = 'Abel') => ({ eventname, ch1, ch2, ch3: 'Betti' })
const presented = events => events.map(item => ({ stage: 'DAY_DIALOGUE', event: item }))

async function synthetic(page, request, events, finished = false) {
  const created = await (await request.post('/api/v1/games', { data: { configuration: 'tongchang' } })).json()
  const snapshot = structuredClone(created.update)
  snapshot.presentedEvents = presented(events)
  snapshot.events = events
  snapshot.eventCursor = 0
  snapshot.nextEventCursor = events.length
  snapshot.phase = finished ? 'FINISHED' : 'DAY_DECISION'
  if (finished) {
    snapshot.view.result = 'VILLAGE_WIN'
    snapshot.revealedRoles = snapshot.view.players.map(player => ({ playerNumber: player.playerNumber, actualRole: 'wu' }))
  }
  await page.route('**/api/v1/games/' + created.sessionId + '?**', route => {
    const cursor = Number(new URL(route.request().url()).searchParams.get('eventCursor') || 0)
    return route.fulfill({ json: { ...snapshot, eventCursor: cursor,
      events: events.slice(cursor), presentedEvents: snapshot.presentedEvents.slice(cursor) } })
  })
  await page.addInitScript(({ key, id }) => {
    if (!sessionStorage.getItem(key)) sessionStorage.setItem(key, JSON.stringify({ sessionId: id, eventCursor: 0 }))
  }, { key: storageKey, id: created.sessionId })
  await page.goto('/')
  await expect(page.getByTestId('next-event')).toBeVisible()
  return created.sessionId
}

// No API or text fixtures: checks actual initial event dialogue against the desktop JSON.
test('WEB-019 real API events render desktop resources in order and survive refresh', async ({ page, request }) => {
  await page.goto('/')
  await page.getByRole('radio', { name: '通常配役' }).check()
  const response = page.waitForResponse(res => res.request().method() === 'POST' && res.url().endsWith('/api/v1/games'))
  await page.getByRole('button', { name: '対局を始める' }).click()
  const created = await (await response).json()
  try {
    const english = readFileSync(new URL('../../src/main/java/com/vocr/domain/CharacterEnglishName.java', import.meta.url), 'utf8')
    const kana = readFileSync(new URL('../../src/main/java/com/vocr/domain/CharacterKatakanaName.java', import.meta.url), 'utf8')
    const values = text => text.replace(/\/\/[^\n]*/g, '').split('{')[1].split('}')[0]
      .split(/[,;]/).map(value => value.trim()).filter(Boolean)
    const names = Object.fromEntries(values(english).map((key, index) => [key, values(kana)[index]]))
    expect(created.update.presentedEvents.length).toBeGreaterThan(1)
    for (const [index, item] of created.update.presentedEvents.entries()) {
      const current = item.event
      let text = '犠牲者はいませんでした。\n'
      if (current.eventname !== 'wsw') {
        const entry = catalog[current.ch1][current.eventname]
        const line = Object.hasOwn(entry.special || {}, current.ch2) ? entry.special[current.ch2] : entry.default
        text = line.replace(/<param=(\w+)>/g, (_, key) =>
          ['talkerName', 'targetName', 'partnerName'].includes(key)
            ? current.ch2 && current.ch2 !== 'NONE' ? names[current.ch2] : '' : '')
      }
      await expect(page.getByTestId('event-dialogue')).toHaveJSProperty('textContent', text)
      await expect(page.locator('[data-dialogue-source="fallback"]')).toHaveCount(0)
      await expectNoEnabledVote(page)
      expect((await saved(page)).eventCursor).toBe(index)
      if (index === 1) {
        await page.reload()
        await expect(page.getByTestId('event-dialogue')).toHaveJSProperty('textContent', text)
        expect((await saved(page)).eventCursor).toBe(index)
      }
      await page.getByTestId('next-event').click()
    }
    expect((await saved(page)).eventCursor).toBe(created.update.nextEventCursor)
  } finally { await request.delete('/api/v1/games/' + created.sessionId) }
})

test('WEB-019 synthetic display edges: default, special, Unicode, narrative, system and explicit diagnostics', async ({ page, request }, testInfo) => {
  const events = [event('gyfo1'), event('gyfo1', 'Betti'), event('gyfo1r', 'Matthäus'),
    event('yjsw', null, 'Günther'), event('wsw', null, 'NONE'), event('NONE'),
    event('unknown'), event('gyfo1', 'Erich', 'unknown')]
  const id = await synthetic(page, request, events)
  let writes = 0
  page.on('request', req => { if (['POST', 'PUT', 'DELETE'].includes(req.method())) writes++ })
  try {
    for (const [index, text] of [
      '共有COするよ。\n相方はエーリッヒだ。\nよろしくね。',
      '共有COするよ。\n相方はベッティちゃんだ。\n一緒に頑張ろう！',
      '相方はマテウスで間違いないよ。一緒に頑張ろう！',
      catalog['Günther'].yjsw.default,
      '犠牲者はいませんでした。\n',
    ].entries()) {
      await expect(page.getByTestId('event-dialogue')).toHaveJSProperty('textContent', text)
      await expect(page.getByTestId('event-dialogue')).toHaveCSS('white-space', 'pre-wrap')
      if (index === 1) {
        const before = await saved(page)
        await page.reload()
        await expect(page.getByTestId('event-dialogue')).toHaveJSProperty('textContent', text)
        expect(await saved(page)).toEqual(before)
        await page.getByRole('region', { name: '当前事件' }).screenshot({ path: testInfo.outputPath('special-dialogue.png') })
      }
      await page.getByTestId('next-event').click()
    }
    for (const diagnostic of ['SENTINEL_EVENT', 'MISSING_EVENT', 'UNKNOWN_CHARACTER']) {
      const warning = page.locator('[data-diagnostic="' + diagnostic + '"]')
      await expect(warning).toContainText('此为界面诊断，非日文原文')
      await expect(page.getByTestId('accessible-dialogue')).toHaveAttribute('lang','zh-CN')
      await expect(page.getByTestId('event-dialogue')).toHaveCount(0)
      await expectNoEnabledVote(page)
      await page.getByTestId('next-event').click()
    }
    expect((await saved(page)).eventCursor).toBe(events.length)
    expect(writes).toBe(0)
    expect(Object.keys(await saved(page)).sort()).toEqual(['eventCursor', 'sessionId'])
  } finally { await request.delete('/api/v1/games/' + id) }
})

test('WEB-019 synthetic terminal dialogue and diagnostic must be acknowledged before identity reveal', async ({ page, request }) => {
  const id = await synthetic(page, request, [event('crsl'), event('NONE')], true)
  try {
    await expect(page.getByTestId('event-dialogue')).toHaveJSProperty('textContent', catalog.Abel.crsl.default)
    await expect(page.locator('.player-identity').first()).not.toContainText('実際の役職')
    await page.getByTestId('next-event').click()
    await page.reload()
    await expect(page.locator('[data-diagnostic="SENTINEL_EVENT"]')).toBeVisible()
    await expect(page.locator('.player-identity').first()).not.toContainText('実際の役職')
    expect((await saved(page)).eventCursor).toBe(1)
    await page.getByTestId('next-event').click()
    await expect(page.locator('.player-identity').first()).toContainText('実際の役職')
    expect((await saved(page)).eventCursor).toBe(2)
  } finally { await request.delete('/api/v1/games/' + id) }
})

test('WEB-019 renderer treats markup as literal text (synthetic text provider only)', async ({ page, request }) => {
  const literal = '<img src=x onerror="window.dialogueExecuted=true">\n<b>原文 & Unicode 😀</b>'
  // Isolate Vue rendering from lookup here; lookup branches are tested with the real resource above.
  await page.route('**/src/eventDialogue.js', route => route.fulfill({ contentType: 'text/javascript',
    body: `export function eventDialogue() { return ${JSON.stringify({ source: 'character', diagnostic: null, text: literal })} }` }))
  const id = await synthetic(page, request, [event('gyfo1')])
  try {
    await expect(page.getByTestId('event-dialogue')).toHaveJSProperty('textContent', literal)
    await expect(page.getByTestId('event-dialogue').locator('img, b')).toHaveCount(0)
    expect(await page.evaluate(() => window.dialogueExecuted)).toBeUndefined()
  } finally { await request.delete('/api/v1/games/' + id) }
})

// Malformed public event fixture: outer participants must not mask the diagnostic.
test('WEB-019 null event is diagnosed without blocking cursor confirmation', async ({ page, request }) => {
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  const id = await synthetic(page, request, [null, event('wsw', null, 'NONE')])
  try {
    await expect(page.locator('[data-diagnostic="INVALID_EVENT"]')).toContainText('非日文原文')
    await expect(page.locator('.event-participants')).toHaveCount(0)
    await page.getByTestId('next-event').click()
    await expect(page.getByTestId('event-dialogue')).toHaveJSProperty('textContent', '犠牲者はいませんでした。\n')
    expect((await saved(page)).eventCursor).toBe(1)
    expect(errors).toEqual([])
  } finally { await request.delete('/api/v1/games/' + id) }
})
