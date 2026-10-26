import { test, expect } from '@playwright/test'
import { drainEvents } from '../tests/drain-events.js'

// Real production runWeb, built UI, no routes/fixtures/seed selection or outcome hunting.
test('unseeded production game restores, reaches settlement and opens local records/replay', async ({ page, request }, info) => {
  const errors = []
  page.on('pageerror', error => errors.push(error.name))
  await page.goto('/')
  await expect(page.getByRole('button', { name: '対局を始める', exact: true })).toBeEnabled()
  const created = page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname === '/api/v1/games')
  await page.getByRole('button', { name: '対局を始める', exact: true }).click()
  const response = await created
  expect(response.status()).toBe(201)
  expect(Object.keys(response.request().postDataJSON())).toEqual(['configuration'])
  const { sessionId } = await response.json()
  try {
    await expect(page.getByTestId('next-event')).toBeEnabled()
    await page.getByTestId('next-event').click()
    const stored = await page.evaluate(() => sessionStorage.getItem('vocr.guest.v1'))
    await page.reload()
    await expect.poll(() => page.evaluate(() => sessionStorage.getItem('vocr.guest.v1'))).toBe(stored)
    let turns = 0
    for (; turns < 80; turns++) {
      await drainEvents(page)
      if (await page.locator('.settlement').count()) break
      await expect(page.getByRole('region', { name: 'ローカル戦績保存' })).toHaveCount(0)
      await expect(page.getByTestId('vote-FREE')).toBeEnabled()
      await page.getByTestId('vote-FREE').click()
    }
    expect(turns, 'a single unseeded game must converge without replacing it').toBeLessThan(80)
    await expect(page.locator('.settlement')).toBeVisible()
    await expect(page.getByRole('region', { name: 'ローカル戦績保存' })).toContainText('保存しました')
    await page.getByRole('button', { name: '戦績を見る', exact: true }).click()
    const records = page.getByRole('dialog', { name: 'ブラウザーの戦績' })
    await expect(records.getByTestId('record-play')).toHaveText('1回')
    await page.screenshot({ path: info.outputPath('production-records.png') })
    await page.keyboard.press('Escape')
    await page.getByTestId('save-replay').click()
    await expect(page.getByRole('region', { name: 'ローカルReplay', exact: true })).toContainText('Replayをこのブラウザーに保存しました。')
    const library = page.getByRole('region', { name: 'ローカルReplay', exact: true })
    const download = page.waitForEvent('download')
    await library.getByRole('button', { name: '一覧の全録画をエクスポート', exact: true }).click()
    expect(await (await download).failure()).toBeNull()
    await library.getByRole('button', { name: '再生する', exact: true }).click()
    const replay = page.getByRole('dialog', { name: /Replay再生/ })
    await expect(replay).toBeVisible()
    const dates = replay.getByLabel('再生日')
    const last = await dates.locator('option').last().getAttribute('value')
    await dates.selectOption(last)
    await expect(replay.getByTestId('replay-day')).toHaveText('第' + last + '日 / ' + last + '日')
    // Check only images intersecting the current viewport; do not defeat lazy loading.
    await expect.poll(() => replay.locator('img').evaluateAll(images => images.filter(img => {
      const r = img.getBoundingClientRect()
      return r.width > 0 && r.height > 0 && r.top < innerHeight && r.bottom > 0 && r.left < innerWidth && r.right > 0
    }).every(img => img.complete && img.naturalWidth > 0))).toBe(true)
    await page.screenshot({ path: info.outputPath('production-replay.png') })
    await replay.getByRole('button', { name: 'Replayを閉じる', exact: true }).click()
    await page.screenshot({ path: info.outputPath('production-settlement.png'), fullPage: true })
    await page.reload()
    await expect(page.getByRole('region', { name: 'ローカル戦績保存' })).toContainText('保存済み')
    expect(errors).toEqual([])
  } finally {
    expect((await request.delete('/api/v1/games/' + sessionId)).ok()).toBe(true)
  }
})
