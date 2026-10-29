import { test, expect } from '@playwright/test'

// The launcher uses this already-existing 404 contract; never create a "probe game".
test('read-only playtest readiness probes preserve a live game and browser history', async ({ page, request }) => {
  await page.goto('/')
  const created = page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname === '/api/v1/games')
  await page.getByRole('button', { name: '対局を始める', exact: true }).click()
  const response = await created
  expect(response.status()).toBe(201)
  const { sessionId } = await response.json()
  try {
    await expect(page.getByTestId('next-event')).toBeEnabled()
    const snapshot = await (await request.get('/api/v1/games/' + sessionId + '?eventCursor=0')).json()
    const storage = () => page.evaluate(() => ({ session: { ...sessionStorage }, local: { ...localStorage } }))
    const before = await storage()
    const writes = []
    page.on('request', r => {
      if (new URL(r.url()).pathname.startsWith('/api/') && !['GET', 'HEAD'].includes(r.method())) writes.push(r.method())
    })
    const probes = await page.evaluate(async () => {
      const rows = []
      for (let i = 0; i < 6; i++) {
        const response = await fetch('/api/v1/__playtest_ready__')
        rows.push({ status: response.status, code: (await response.json()).code })
      }
      return rows
    })
    expect(probes).toEqual(Array.from({ length: 6 }, () => ({ status: 404, code: 'HTTP_ERROR' })))
    expect(await (await request.get('/api/v1/games/' + sessionId + '?eventCursor=0')).json()).toEqual(snapshot)
    expect(await storage()).toEqual(before)
    expect(writes).toEqual([])
  } finally {
    expect((await request.delete('/api/v1/games/' + sessionId)).ok()).toBe(true)
  }
})
