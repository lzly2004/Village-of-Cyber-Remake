import { test, expect } from '@playwright/test'
import { readFileSync } from 'node:fs'
const replayFixture = JSON.parse(readFileSync(new URL('../../src/test/resources/replay/exchange-v3.json', import.meta.url), 'utf8'))

const key = 'vocr.guest.v1'
const begin = page => page.getByRole('button', { name: '対局を始める', exact: true })
const saved = page => page.evaluate(key => JSON.parse(sessionStorage.getItem(key)), key)
async function start(page) {
  await page.goto('/')
  await expect(page.locator('.side-footer')).toContainText('30分')
  await expect(page.locator('.side-footer')).toContainText('24時間')
  await begin(page).click()
  await expect(page.getByTestId('next-event')).toBeVisible()
  return (await saved(page)).sessionId
}

test('isolated browser guests use distinct capabilities; another guest is unchanged by mutation/release', async ({ page, browser, request }) => {
  const other = await browser.newContext()
  const second = await other.newPage()
  let a, b
  try {
    a = await start(page)
    b = await start(second)
    expect(a).not.toBe(b)
    const before = await (await request.get('/api/v1/games/' + b)).json()
    const source = await (await request.get('/api/v1/games/' + a)).json()
    const selection = source.legalTargetSelections.find(s => s.type === 'VOTE')
    expect((await request.put('/api/v1/games/' + a + '/vote-target', {
      data: { expectedRevision: source.revision, target: selection.targets[0], selected: true },
    })).status()).toBe(200)
    expect((await request.delete('/api/v1/games/' + a)).status()).toBe(204)
    expect(await (await request.get('/api/v1/games/' + b)).json()).toEqual(before)
    await second.reload()
    await expect(second.getByTestId('next-event')).toBeVisible()
    expect((await saved(second)).sessionId).toBe(b)
    const local = await second.evaluate(() => JSON.stringify(localStorage))
    expect(local).not.toContain(a)
    expect(local).not.toContain(b)
    const unknown = await request.get('/api/v1/games/00000000-0000-4000-8000-000000000000')
    expect(unknown.status()).toBe(404)
    expect((await unknown.json()).code).toBe('GAME_NOT_FOUND')
  } finally {
    if (a) await request.delete('/api/v1/games/' + a)
    if (b) await request.delete('/api/v1/games/' + b)
    await other.close()
  }
})

test('known expired credential response clears only current capability and explains all expiry causes', async ({ page, request }) => {
  const id = await start(page)
  try {
    await page.evaluate(() => {
      sessionStorage.setItem('unrelated-session', 'keep')
      localStorage.setItem('unrelated-local', 'keep')
    })
    const before = await page.evaluate(async replay => {
      const { createRecordStore } = await import('/src/recordStore.js')
      const { createReplayStore } = await import('/src/replayStore.js')
      await createRecordStore().record({ sessionId: '00000000-0000-4000-8000-000000000039', configuration: 'jianyi', result: 'VILLAGE_WIN' })
      await createReplayStore().save(replay)
      return { records: await createRecordStore().read(), replay: await createReplayStore().list() }
    }, replayFixture[0])
    expect(before.replay).toHaveLength(1)
    expect(before.records.totalPlayCnt).toBe(1)
    let writes = 0
    page.on('request', req => { if (req.url().includes('/api/v1/games') && req.method() !== 'GET') writes++ })
    await page.route('**/api/v1/games/' + id + '?*', route => route.fulfill({
      status: 404, contentType: 'application/json',
      body: JSON.stringify({ code: 'GAME_NOT_FOUND', message: '游客对局已失效或不存在，请重新开局', currentRevision: null }),
    }))
    await page.reload()
    await expect(page.getByRole('alert')).toContainText('闲置过期')
    await expect(page.getByRole('alert')).toContainText('最长时限')
    expect(await saved(page)).toBeNull()
    await expect(begin(page)).toBeVisible()
    expect(writes).toBe(0)
    expect(await page.evaluate(() => [sessionStorage.getItem('unrelated-session'), localStorage.getItem('unrelated-local')])).toEqual(['keep', 'keep'])
    expect(await page.evaluate(async () => {
      const { createRecordStore } = await import('/src/recordStore.js')
      const { createReplayStore } = await import('/src/replayStore.js')
      return { records: await createRecordStore().read(), replay: await createReplayStore().list() }
    })).toEqual(before)
  } finally { await request.delete('/api/v1/games/' + id) }
})

for (const code of ['HTTP_ERROR', 'UNKNOWN_PROXY_ERROR']) {
  test('unrecognized 404 ' + code + ' never discards the capability', async ({ page, request }) => {
    const id = await start(page)
    try {
      const before = await saved(page)
      await page.route('**/api/v1/games/' + id + '?*', route => route.fulfill({
        status: 404, contentType: 'application/json', body: JSON.stringify({ code, message: 'gateway unavailable' }),
      }))
      await page.reload()
      await expect(page.getByRole('alert')).toContainText('gateway unavailable')
      expect(await saved(page)).toEqual(before)
    } finally { await request.delete('/api/v1/games/' + id) }
  })
}

test('capacity rejection does not create or automatically replay; explicit retry can start', async ({ page, request }) => {
  let posts = 0
  await page.route('**/api/v1/games', async route => {
    if (route.request().method() !== 'POST') return route.continue()
    posts++
    if (posts === 1) return route.fulfill({ status: 503, contentType: 'application/json',
      body: JSON.stringify({ code: 'SESSION_LIMIT', message: 'capacity', currentRevision: null }) })
    await route.continue()
  })
  let id
  try {
    await page.goto('/')
    await begin(page).click()
    await expect(page.getByRole('alert')).toContainText('未创建新局')
    await expect(begin(page)).toBeEnabled()
    expect(await saved(page)).toBeNull()
    expect(posts).toBe(1)
    await begin(page).click()
    await expect(page.getByTestId('next-event')).toBeVisible()
    id = (await saved(page)).sessionId
    expect(posts).toBe(2)
  } finally { if (id) await request.delete('/api/v1/games/' + id) }
})

for (const stored of [JSON.stringify({ sessionId: '../not-a-credential', eventCursor: 0 }), '{bad json']) {
  test('invalid local credential is removed with bounded notice: ' + stored, async ({ page }) => {
    await page.addInitScript(({ key, stored }) => sessionStorage.setItem(key, stored), { key, stored })
    const calls = []
    page.on('request', req => { if (req.url().includes('/api/v1/games')) calls.push(req.url()) })
    await page.goto('/')
    await expect(page.getByRole('alert')).toContainText('重新开局')
    expect(await saved(page)).toBeNull()
    expect(calls).toEqual([])
    await expect(begin(page)).toBeEnabled()
  })
}
