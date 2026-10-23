import { publicSnapshot } from '../test-fixtures/public-players.js'
import { test, expect } from '@playwright/test'
import { readFileSync } from 'node:fs'

test('WEB-022/023 static manifest images decode at original dimensions; lobby requests no player assets', async ({ page, request }) => {
  const imageRequests = []
  page.on('request', req => { if (req.url().endsWith('.webp')) imageRequests.push(req.url()) })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /一つひとつの発言/ })).toBeVisible()
  await expect(page.locator('.player-row')).toHaveCount(0)
  expect(imageRequests).toEqual([])
  const response = await request.get('/assets/images/manifest.json')
  expect(response.ok()).toBe(true)
  expect(response.headers()['content-type']).toContain('application/json')
  const manifest = await response.json()
  expect(Object.keys(manifest.images)).toHaveLength(1171)
  const decoded = await page.evaluate(async images => {
    const byFile = new Map(Object.values(images).map(entry => [entry.file, entry]))
    for (const entry of byFile.values()) {
      const image = new Image()
      image.src = '/assets/images/' + entry.file
      await image.decode()
      if (image.naturalWidth !== entry.size[0] || image.naturalHeight !== entry.size[1]) {
        throw new Error('Decoded dimensions mismatch: ' + entry.file)
      }
      image.src = ''
    }
    return byFile.size
  }, manifest.images)
  expect(decoded).toBe(manifest.summary.uniqueFiles)
  await expect(page.locator('.player-identity')).toHaveCount(0)
})

test('WEB-022 unknown hashed image cannot decode as a valid image', async ({ page }) => {
  await page.goto('/')
  const rejected = await page.evaluate(async () => {
    const image = new Image()
    image.src = '/assets/images/' + '0'.repeat(64) + '.webp'
    try { await image.decode(); return false } catch { return true }
  })
  expect(rejected).toBe(true)
})


test('WEB-023 cold lobby and first game obey transfer budgets; only visible public player images load', async ({ page, request }, testInfo) => {
  const manifest = await (await request.get('/assets/images/manifest.json')).json()
  const paths = []
  page.on('request', req => { if (req.url().includes('/assets/images/')) paths.push(new URL(req.url()).pathname) })
  const snapshot = publicSnapshot()
  const event = { eventname: 'zco7', ch1: 'Abel', ch2: 'NONE', ch3: 'NONE' }
  snapshot.events = [event]; snapshot.presentedEvents = [{ stage: 'DAY_DIALOGUE', event }]; snapshot.nextEventCursor = 1
  const imageMapping = JSON.parse(readFileSync(new URL('../../src/main/resources/config/event_image_mapping.json', import.meta.url), 'utf8'))
  const stageFiles = ['images/haikei3.png', 'images/' + imageMapping.Abel.zco7.default]
  await page.route('**/api/v1/games', route => route.fulfill({json:{
    sessionId: '00000000-0000-0000-0000-000000000023', update: snapshot,
  }}))
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /一つひとつの発言/ })).toBeVisible()
  await page.waitForLoadState('networkidle')
  expect(paths).toEqual([])
  const lobbyBytes = await page.evaluate(() => performance.getEntriesByType('navigation')[0].transferSize
    + performance.getEntriesByType('resource').reduce((sum, entry) => sum + entry.transferSize, 0))
  expect(lobbyBytes).toBeGreaterThan(0)
  expect(lobbyBytes).toBeLessThanOrEqual(1.5 * 1024 ** 2)
  const firstGame = await page.evaluate(() => performance.now())
  await page.getByRole('button', {name:'対局を始める'}).click()
  const rows = page.locator('.player-row')
  await expect(rows).toHaveCount(44)
  await rows.first().scrollIntoViewIfNeeded()
  await expect.poll(() => rows.first().locator('.portrait-image img').evaluateAll(images => images.length === 1 && images[0].naturalWidth === 64)).toBe(true)
  await page.waitForLoadState('networkidle')
  expect(paths.filter(path=>path.endsWith('manifest.json'))).toHaveLength(1)
  await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-ready', 'true')
  const allowed = new Set(Array.from({length:44},(_,i)=>[
    '/assets/images/'+manifest.images['images/'+String(i+1).padStart(2,'0')+'s.png'].file,
    '/assets/images/'+manifest.images['images/'+(i+1)+'job.png'].file,
  ]).flat().concat(stageFiles.map(name=>'/assets/images/'+manifest.images[name].file)))
  const requested = paths.filter(path=>path.endsWith('.webp'))
  expect(requested.length).toBeGreaterThan(0)
  expect(requested.length).toBeLessThan(90)
  expect(requested.every(path=>allowed.has(path))).toBe(true)
  await expect(rows.last().locator('.portrait-image img')).toHaveCount(0)
  const firstGameBytes = await page.evaluate(since => performance.getEntriesByType('resource')
    .filter(entry=>entry.startTime>=since).reduce((sum,entry)=>sum+entry.transferSize,0), firstGame)
  expect(firstGameBytes).toBeGreaterThan(0)
  expect(firstGameBytes).toBeLessThanOrEqual(3 * 1024 ** 2)
  const measurement = { lobbyBytes, firstGameBytes, initiallyRequestedImages: requested.length, logicalPlayerImages: 88, currentStageImages: stageFiles.length }
  console.info('WEB-023 static transfer: ' + JSON.stringify(measurement))
  await testInfo.attach('static-transfer.json', { body: JSON.stringify(measurement), contentType: 'application/json' })
  await rows.last().scrollIntoViewIfNeeded()
  await expect(rows.last().locator('.portrait-image img')).toHaveAttribute('src',
    '/assets/images/'+manifest.images['images/44s.png'].file)
})
