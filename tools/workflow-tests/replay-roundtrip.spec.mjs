import { createRequire } from 'node:module'
import { readFileSync } from 'node:fs'
const require = createRequire(new URL('../../web-client/package.json', import.meta.url))
const { test, expect } = require('@playwright/test')
// Required: never fall back to a committed fixture or an unrelated prior run.
if (!process.env.VOCR_VERIFY_DESKTOP_EXPORT) throw new Error('VOCR_VERIFY_DESKTOP_EXPORT is required')
const bytes = readFileSync(process.env.VOCR_VERIFY_DESKTOP_EXPORT)
const slots = JSON.parse(bytes)
const expected = slots[0]
if (!expected || slots.slice(1).some(Boolean) || expected.slotIndex !== 0) throw new Error('Expected the current desktop single-slot export')

test('current desktop file imports, plays and exports through browser without changing live authority', async ({ page, request }) => {
  await page.goto('/')
  await page.getByRole('button', { name: '対局を始める' }).click()
  await expect(page.getByTestId('next-event')).toBeVisible()
  const stored = await page.evaluate(() => sessionStorage.getItem('vocr.guest.v1'))
  const credentials = JSON.parse(stored)
  const path = '/api/v1/games/' + credentials.sessionId + '?eventCursor=' + credentials.eventCursor
  try {
    const before = await (await request.get(path)).json()
    let writes = 0, replayReads = 0
    page.on('request', r => {
      if (r.url().includes('/api/v1/games')) {
        if (r.method() !== 'GET') writes++
        if (r.url().includes('/replay?')) replayReads++
      }
    })
    const library = page.getByRole('region', { name: 'ローカルReplay', exact: true })
    await library.getByRole('button', { name: 'Replay一覧', exact: true }).click()
    await library.getByLabel('録画ファイルを選ぶ').setInputFiles({ name: 'desktop-current.json', mimeType: 'application/json', buffer: bytes })
    const preview = library.getByRole('group', { name: '取り込みの確認（未保存）' })
    await expect(preview.getByRole('checkbox')).toHaveCount(1)
    await preview.getByRole('checkbox').check()
    await preview.getByRole('button', { name: '選択した録画の取り込みを確定', exact: true }).click()
    await expect(preview).toHaveCount(0)
    const entries = library.locator('.replay-entries > li')
    await expect(entries).toHaveCount(1)
    await entries.getByRole('button', { name: '再生する', exact: true }).click()
    const dialog = page.getByRole('dialog', { name: /Replay再生/ })
    await expect(dialog.locator('.replay-player-card')).toHaveCount(expected.daySnapshots[0].players.length)
    await dialog.getByRole('combobox', { name: '再生日', exact: true }).selectOption(String(expected.totalDays))
    await expect(page.getByTestId('replay-role-groups')).toBeVisible()
    await page.keyboard.press('Escape')
    const pending = page.waitForEvent('download')
    await entries.getByRole('button', { name: 'この録画をエクスポート', exact: true }).click()
    const download = await pending
    expect(JSON.parse(readFileSync(await download.path()))).toEqual(slots)
    expect(writes).toBe(0)
    expect(replayReads).toBe(0)
    expect(await page.evaluate(() => sessionStorage.getItem('vocr.guest.v1'))).toBe(stored)
    expect(await (await request.get(path)).json()).toEqual(before)
  } finally {
    await request.delete('/api/v1/games/' + credentials.sessionId)
  }
})
