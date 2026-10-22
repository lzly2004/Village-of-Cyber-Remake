import { test, expect } from '@playwright/test'
import { characterNames } from '../src/characterNames.js'

const storageKey = 'vocr.guest.v1'
const saved = page => page.evaluate(key => JSON.parse(sessionStorage.getItem(key)), storageKey)
async function start(page) {
  await page.goto('/')
  await page.getByRole('radio', { name: '通常配役' }).check()
  await page.getByRole('button', { name: '开始对局' }).click()
  await expect(page.getByTestId('next-event')).toBeVisible()
  return (await saved(page)).sessionId
}
async function synthetic(page, request, edit) {
  const created = await (await request.post('/api/v1/games', { data: { configuration: 'tongchang' } })).json()
  const snapshot = structuredClone(created.update)
  snapshot.eventCursor = snapshot.nextEventCursor = 0
  snapshot.presentedEvents = snapshot.events = []
  snapshot.phase = 'DAY_DECISION'
  edit(snapshot)
  await page.route('**/api/v1/games/' + created.sessionId + '?**', route => {
    const cursor = Number(new URL(route.request().url()).searchParams.get('eventCursor') || 0)
    return route.fulfill({ json: { ...snapshot, eventCursor: cursor,
      events: snapshot.events.slice(cursor), presentedEvents: snapshot.presentedEvents.slice(cursor) } })
  })
  await page.addInitScript(({ key, id }) => {
    if (!sessionStorage.getItem(key)) sessionStorage.setItem(key, JSON.stringify({ sessionId: id, eventCursor: 0 }))
  }, { key: storageKey, id: created.sessionId })
  await page.goto('/')
  await expect(page.getByTestId('game-stage')).toBeVisible()
  return { id: created.sessionId, snapshot }
}
function manyPlayers(snapshot, withActors = false) {
  snapshot.view.players = Object.keys(characterNames).map((character, index) => ({
    ...snapshot.view.players[0], playerNumber: index + 1, character, alive: true,
    claimedRole: 'NONE', claimedRoleOrder: 0, deathDay: 0, deathReason: 'NONE',
  }))
  snapshot.view.aliveCount = 44
  snapshot.view.deathCount = 0
  const targets = snapshot.view.players.map(player => player.playerNumber)
  snapshot.legalTargetSelections = [{ type: 'VOTE', targets, claimedActors: [] }]
  snapshot.selections.voteTargets = [1]
  snapshot.legalVoteActions = [{ method: 'DESIGNATED', candidates: [1] }]
  if (withActors) {
    for (const [type, skill, numbers] of [['DIVINATION', 'divination', [1, 2, 3, 4, 5]], ['GUARD', 'guard', [6, 7, 8, 9]]]) {
      const actors = numbers.map((playerNumber, index) => ({ playerNumber, claimedRoleOrder: index + 1 }))
      snapshot.legalTargetSelections.push({ type, targets, claimedActors: actors })
      snapshot.selections[skill] = { hiddenTargets: [],
        claimedActors: actors.map(actor => ({ ...actor, targets: [44] })) }
    }
  }
}
async function expectContainedStage(page) {
  const rect = await page.getByTestId('stage-surface').boundingBox()
  expect(Math.abs(rect.width / rect.height - 16 / 9)).toBeLessThan(0.005)
  const drawing = await page.getByTestId('stage-artwork').boundingBox()
  expect(Math.abs(drawing.width - rect.width)).toBeLessThan(1)
  expect(Math.abs(drawing.height - rect.height)).toBeLessThan(1)
  expect(rect.x).toBeGreaterThanOrEqual(0)
  expect(rect.x + rect.width).toBeLessThanOrEqual(await page.evaluate(() => innerWidth) + 1)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true)
}

test('WEB-021 real session stage scales uniformly at desktop widths without changing cursor or text', async ({ page, request }, testInfo) => {
  const media = []
  page.on('request', req => { if (/\/images\/|\/BGM\/|\/sounds\//.test(req.url())) media.push(req.url()) })
  const id = await start(page)
  try {
    const state = await saved(page)
    const text = await page.getByTestId('event-dialogue').textContent()
    for (const viewport of [{ width: 1920, height: 1080 }, { width: 1440, height: 900 },
      { width: 1280, height: 720 }, { width: 1024, height: 768 }]) {
      await page.setViewportSize(viewport)
      await expectContainedStage(page)
      await expect(page.getByTestId('event-dialogue')).toHaveJSProperty('textContent', text)
      expect(await saved(page)).toEqual(state)
      await expect(page.getByTestId('vote')).toBeDisabled()
      if (viewport.width === 1280 || viewport.width === 1440) {
        await page.getByRole('region', { name: '事件展示', exact: true })
          .screenshot({ path: testInfo.outputPath('stage-' + viewport.width + '.png') })
      }
    }
    expect(media).toEqual([])
    await page.reload()
    await expect(page.getByTestId('event-dialogue')).toHaveJSProperty('textContent', text)
    expect(await saved(page)).toEqual(state)
  } finally { await request.delete('/api/v1/games/' + id) }
})

test('WEB-021 synthetic long dialogue scrolls by keyboard; confirmation stays outside and guards reveal', async ({ page, request }) => {
  const text = Array.from({ length: 100 }, (_, index) => `${index}行目：長い台詞の表示と読み取り。`).join('\n')
  await page.route('**/src/eventDialogue.js', route => route.fulfill({ contentType: 'text/javascript',
    body: `export function eventDialogue() { return ${JSON.stringify({ source: 'character', diagnostic: null, text })} }` }))
  const { id } = await synthetic(page, request, snapshot => {
    snapshot.phase = 'FINISHED'
    snapshot.view.result = 'VILLAGE_WIN'
    const event = { eventname: 'crsl', ch1: 'Abel', ch2: 'Erich', ch3: 'Betti' }
    snapshot.events = [event, { ...event, ch1: 'Erich' }]
    snapshot.presentedEvents = snapshot.events.map(event => ({ stage: 'SETTLEMENT', event }))
    snapshot.nextEventCursor = 2
    snapshot.revealedRoles = snapshot.view.players.map(player => ({ playerNumber: player.playerNumber, actualRole: 'wu' }))
  })
  try {
    await expect(page.getByTestId('stage-actor')).toHaveCount(3)
    await expect(page.getByTestId('event-dialogue')).toHaveJSProperty('textContent', text)
    const reading = page.getByRole('region', { name: '舞台文字阅读区', exact: true })
    expect(await reading.evaluate(node => node.scrollHeight > node.clientHeight)).toBe(true)
    await reading.focus()
    await page.keyboard.press('PageDown')
    await expect.poll(() => reading.evaluate(node => node.scrollTop)).toBeGreaterThan(0)
    expect((await saved(page)).eventCursor).toBe(0)
    await expect(page.locator('.player-identity').first()).not.toContainText('真实身份')
    await expect(reading.getByTestId('next-event')).toHaveCount(0)
    await page.getByTestId('next-event').focus()
    await page.keyboard.press('Enter')
    expect((await saved(page)).eventCursor).toBe(1)
    await expect(page.locator('.player-identity').first()).not.toContainText('真实身份')
    await expect.poll(() => reading.evaluate(node => node.scrollTop)).toBe(0)
    await page.keyboard.press('Enter')
    await expect(page.locator('.player-identity').first()).toContainText('真实身份')
    expect((await saved(page)).eventCursor).toBe(2)
  } finally { await request.delete('/api/v1/games/' + id) }
})

test('WEB-021 synthetic long player list exposes scroll instructions and supports keyboard and wheel', async ({ page, request }) => {
  const { id } = await synthetic(page, request, snapshot => manyPlayers(snapshot))
  let writes = 0
  page.on('request', req => { if (['POST', 'PUT', 'DELETE'].includes(req.method())) writes++ })
  try {
    const list = page.getByRole('region', { name: '公开玩家列表滚动区域', exact: true })
    await expect(page.locator('#player-list-help')).toContainText('按 Tab')
    await expect(list.getByRole('checkbox')).toHaveCount(44)
    expect(await list.evaluate(node => node.scrollHeight > node.clientHeight)).toBe(true)
    await list.focus()
    await page.keyboard.press('End')
    await expect.poll(() => list.evaluate(node => node.scrollTop)).toBeGreaterThan(0)
    await list.getByRole('checkbox').last().focus()
    await expect(list.getByRole('checkbox').last()).toBeFocused()
    const before = await list.evaluate(node => node.scrollTop)
    await list.hover()
    await page.mouse.wheel(0, -1000)
    await expect.poll(() => list.evaluate(node => node.scrollTop)).toBeLessThan(before)
    expect(writes).toBe(0)
    expect((await saved(page)).eventCursor).toBe(0)
    await expect(page.locator('.player-identity').first()).not.toContainText('真实身份')
  } finally { await request.delete('/api/v1/games/' + id) }
})

test('WEB-021 synthetic many public actors remain bounded, fold with keyboard, and reset browsing on refresh', async ({ page, request }, testInfo) => {
  const { id } = await synthetic(page, request, snapshot => manyPlayers(snapshot, true))
  let writes = 0
  page.on('request', req => { if (['POST', 'PUT', 'DELETE'].includes(req.method())) writes++ })
  try {
    const before = await saved(page)
    for (const label of ['個別の占い予約', '個別の護衛予約']) {
      const list = page.getByRole('region', { name: label + ' 一覧', exact: true })
      expect(await list.evaluate(node => node.scrollHeight > node.clientHeight)).toBe(true)
      expect((await list.boundingBox()).height).toBeLessThan(430)
      await list.focus()
      await page.keyboard.press('PageDown')
      await expect.poll(() => list.evaluate(node => node.scrollTop)).toBeGreaterThan(0)
      const first = list.locator('summary').first()
      await first.focus()
      await page.keyboard.press('Enter')
      await expect(list.locator('details').first()).not.toHaveAttribute('open')
      await expect(list.locator('fieldset').first()).toBeHidden()
      await page.keyboard.press('Tab')
      await expect(list.locator('summary').nth(1)).toBeFocused()
      await first.focus()
      await page.keyboard.press('Enter')
      await page.keyboard.press('Tab')
      await expect(list.locator('fieldset').first().getByRole('checkbox').first()).toBeFocused()
      await first.focus()
      await page.keyboard.press('Enter')
    }
    expect(writes).toBe(0)
    expect(await saved(page)).toEqual(before)
    await page.reload()
    await expect(page.getByTestId('claimed-DIVINATION-1')).toBeVisible()
    expect(await saved(page)).toEqual(before)
    expect(Object.keys(before).sort()).toEqual(['eventCursor', 'sessionId'])
    await page.locator('.claimed-skills-layout').screenshot({ path: testInfo.outputPath('bounded-actor-panels.png') })
  } finally { await request.delete('/api/v1/games/' + id) }
})

test('WEB-021 CSS zoom stress keeps stage ratio, readable text and controls within the viewport', async ({ page, request }) => {
  const { id } = await synthetic(page, request, snapshot => manyPlayers(snapshot, true))
  try {
    for (const zoom of [1.25, 2]) {
      await page.evaluate(zoom => { document.body.style.zoom = String(zoom) }, zoom)
      await expectContainedStage(page)
      expect(await page.getByTestId('stage-text').evaluate(node => parseFloat(getComputedStyle(node).fontSize))).toBeGreaterThanOrEqual(14)
      await page.getByTestId('vote').focus()
      await expect(page.getByTestId('vote')).toBeFocused()
    }
  } finally { await request.delete('/api/v1/games/' + id) }
})
