import { test, expect } from '@playwright/test'
import { readFileSync } from 'node:fs'
import { helpGroups } from '../src/helpContent.js'
import { configurations, roles } from '../src/labels.js'

const saved = page => page.evaluate(() => sessionStorage.getItem('vocr.guest.v1'))

test('Japanese guide covers every help topic and source; keyboard modal closes back to its opener', async ({ page }) => {
  const media = [], writes = []
  page.on('request', req => {
    if (req.url().includes('/assets/')) media.push(req.url())
    if (['POST', 'PUT', 'DELETE'].includes(req.method())) writes.push(req.url())
  })
  await page.goto('/')
  await expect(page.locator('html')).toHaveAttribute('lang', 'ja')
  const before = await saved(page)
  const opener = page.getByRole('button', { name: '遊び方・ヘルプ', exact: true })
  await opener.focus(); await page.keyboard.press('Enter')
  const dialog = page.getByRole('dialog', { name: '電脳村ガイド' })
  await expect(dialog).toBeVisible()
  await expect(dialog).toContainText('原版の日本語原文ではありません')
  for (const group of helpGroups) {
    await dialog.getByLabel('分類', { exact: true }).selectOption(group.id)
    for (const topic of group.topics) {
      await dialog.getByLabel('項目', { exact: true }).selectOption(topic.id)
      await expect(dialog.getByRole('heading', { name: topic.title, exact: true })).toBeVisible()
      const original = readFileSync(new URL('../../src/main/resources/help/' + topic.source, import.meta.url), 'utf8').replaceAll('\r\n', '\n')
      await dialog.getByText('参照した原文（中国語・デスクトップ版）', { exact: true }).click()
      await expect(dialog.locator('pre')).toHaveText(original)
      if (topic.webNote) await expect(dialog).toContainText(topic.webNote)
    }
  }
  for (let n = 0; n < 12; n++) {
    await page.keyboard.press('Tab')
    expect(await dialog.evaluate(el => el.contains(document.activeElement))).toBe(true)
  }
  await page.keyboard.press('Escape')
  await expect(dialog).toHaveCount(0)
  await expect(opener).toBeFocused()
  expect(await saved(page)).toBe(before)
  expect(writes).toEqual([]); expect(media).toEqual([])
})

test('configuration page contains all seven exact public rosters, scrolls and never starts a game', async ({ page }, info) => {
  await page.setViewportSize({ width: 1024, height: 720 })
  let writes = 0
  page.on('request', req => { if (['POST', 'PUT', 'DELETE'].includes(req.method())) writes++ })
  await page.goto('/')
  await page.getByRole('button', { name: '配役説明', exact: true }).click()
  const dialog = page.getByRole('dialog')
  for (const item of configurations) {
    const row = dialog.locator('[data-configuration="' + item.value + '"]')
    await row.scrollIntoViewIfNeeded()
    await expect(row.getByRole('heading')).toHaveText(item.name + '村 · ' + item.playerCount + '人')
    await expect(row.locator('dt')).toHaveText(Object.keys(item.roleCounts).map(key => roles[key]))
    await expect(row.locator('dd')).toHaveText(Object.values(item.roleCounts).map(n => n + '人'))
  }
  await dialog.getByRole('heading', { name: '電脳村ガイド' }).scrollIntoViewIfNeeded()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await dialog.screenshot({ path: info.outputPath('configuration-help-1024.png') })
  await dialog.getByRole('button', { name: '閉じる', exact: true }).click()
  await expect(page.getByRole('radio', { name: '簡易配役' })).toBeChecked()
  expect(writes).toBe(0); expect(await saved(page)).toBeNull()
})

test('reading guide during a real pending event preserves game node, cursor, scroll, storage and authority', async ({ page, request }) => {
  await page.goto('/')
  await page.getByRole('button', { name: '対局を始める' }).click()
  await expect(page.getByTestId('next-event')).toBeVisible()
  const before = await saved(page), id = JSON.parse(before).sessionId
  try {
    const authority = await (await request.get('/api/v1/games/' + id)).json()
    const text = await page.getByTestId('stage-text').textContent()
    await page.getByTestId('stage-text').evaluate(el => { el.dataset.helpProbe = 'same-node'; el.scrollTop = 4 })
    const scroll = await page.getByTestId('stage-text').evaluate(el => el.scrollTop)
    let writes = 0
    page.on('request', req => { if (['POST', 'PUT', 'DELETE'].includes(req.method())) writes++ })
    await page.getByRole('button', { name: '遊び方・ヘルプ', exact: true }).click()
    await page.getByRole('dialog').getByRole('button', { name: '配役説明', exact: true }).click()
    await page.keyboard.press('Escape')
    await expect(page.getByTestId('stage-text')).toHaveAttribute('data-help-probe', 'same-node')
    await expect(page.getByTestId('stage-text')).toHaveText(text)
    expect(await page.getByTestId('stage-text').evaluate(el => el.scrollTop)).toBe(scroll)
    expect(await saved(page)).toBe(before)
    expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual(authority)
    expect(writes).toBe(0)
    await page.getByTestId('next-event').click()
    expect(JSON.parse(await saved(page)).eventCursor).toBe(JSON.parse(before).eventCursor + 1)
  } finally { await request.delete('/api/v1/games/' + id) }
})

test('failed lazy help chunk shows a bounded diagnosis and leaves game controls usable', async ({ page }) => {
  await page.route('**/src/HelpDialog.vue', route => route.abort('failed'))
  await page.goto('/')
  await page.getByRole('button', { name: '遊び方・ヘルプ', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('HELP_LOAD_FAILED')
  await expect(page.getByRole('button', { name: '対局を始める' })).toBeEnabled()
  expect(await saved(page)).toBeNull()
  await page.getByRole('button', { name: 'ヘルプを閉じる', exact: true }).click()
  await expect(page.getByRole('alert')).toHaveCount(0)
})

test('retained Chinese structural copy declares its language; Japanese player names override it', async ({ page, request }) => {
  await page.goto('/')
  await page.getByRole('button', { name: '対局を始める' }).click()
  await expect(page.getByTestId('next-event')).toBeVisible()
  const id = JSON.parse(await saved(page)).sessionId
  try {
  const reading = page.getByTestId('stage-text')
  await expect(reading).toHaveAttribute('lang', 'zh-CN')
  await expect(page.locator('#stage-reading-help')).toHaveAttribute('lang', 'zh-CN')
  await expect(page.getByRole('region', { name: '公开玩家列表', exact: true })).toHaveAttribute('lang', 'zh-CN')
  await expect(page.locator('.pill')).toHaveAttribute('lang', 'zh-CN')
  await expect(page.locator('.player-identity').first()).toHaveAttribute('lang', 'ja')
  await expect(page.getByTestId('event-dialogue')).toHaveAttribute('lang', 'ja')
  await page.route('**/api/v1/games/*?eventCursor=*', r => r.abort())
  await page.getByRole('button', { name: '再同期' }).click()
  await expect(page.getByRole('alert')).toHaveAttribute('lang', 'zh-CN')
  } finally { await request.delete('/api/v1/games/' + id) }
})
