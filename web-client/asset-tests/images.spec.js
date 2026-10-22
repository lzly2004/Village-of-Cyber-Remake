import { test, expect } from '@playwright/test'

test('WEB-022 static manifest images all decode at original dimensions without preloading on home', async ({ page, request }) => {
  const imageRequests = []
  page.on('request', req => { if (req.url().endsWith('.webp')) imageRequests.push(req.url()) })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /每一次发言/ })).toBeVisible()
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
