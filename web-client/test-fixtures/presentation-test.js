import { test as base, expect } from '@playwright/test'

// Legacy business/geometry journeys explicitly opt into completing the new local
// presentation. Dedicated night-day.spec.js uses the base fixture and checks every
// real production timing boundary. This helper NEVER clicks next-event or a vote.
const prepared = new WeakSet()
export async function preparePresentation(page) {
    if (prepared.has(page)) return
    prepared.add(page)
    await page.clock.install()
    const prelude = page.locator('[data-testid="game-stage"][data-presentation-phase]:not([data-presentation-phase="event"])')
    await page.addLocatorHandler(prelude, async () => {
      await page.clock.pauseAt(new Date(await page.evaluate(() => Date.now()) + 10))
      const stage = page.getByTestId('game-stage')
      let phase = await stage.getAttribute('data-presentation-phase')
      if (phase === 'night') {
        await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-ready', 'true')
        await page.clock.runFor(7000)
        phase = await stage.getAttribute('data-presentation-phase')
      }
      if (phase === 'dawn') {
        await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-ready', 'true')
        await page.clock.runFor(2500)
        phase = await stage.getAttribute('data-presentation-phase')
      }
      if (phase === 'day-text') {
        await page.clock.runFor(1000)
        await expect(page.getByTestId('day-start-continue')).toBeEnabled()
        await page.clock.runFor(1)
        await page.getByTestId('day-start-continue').click()
      } else if (phase !== 'event') {
        throw new Error('Unexpected local presentation phase: ' + phase)
      }
      await expect(stage).toHaveAttribute('data-presentation-phase', 'event')
      await page.clock.resume()
    })
}
export const test = base.extend({
  page: async ({ page }, use) => {
    await preparePresentation(page)
    await use(page)
  },
})
export { expect }
