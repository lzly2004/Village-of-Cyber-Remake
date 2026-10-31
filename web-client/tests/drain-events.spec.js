import { test, expect } from '../test-fixtures/presentation-test.js'
import { drainEvents } from './drain-events.js'

for (const end of ['decision', 'settlement']) {
  test('drain waits through a controlled missing-controls gap before ' + end, async ({ page }) => {
    await page.setContent('<button data-testid="next-event">next</button><button data-testid="vote" disabled>vote</button>')
    await page.evaluate(() => {
      window.clicks = 0
      document.querySelector('[data-testid="next-event"]').onclick = event => {
        window.clicks++
        event.target.remove()
        window.inGap = true
      }
    })
    let done = false
    const reading = drainEvents(page).then(() => { done = true })
    await expect.poll(() => page.evaluate(() => window.inGap)).toBe(true)
    // Inspect over animation frames while the gap is held, rather than sleeping for network timing.
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))))
    expect(done).toBe(false)
    await page.evaluate(end => {
      const button = document.createElement('button')
      button.dataset.testid = 'next-event'
      button.textContent = 'remaining event'
      button.onclick = () => {
        window.clicks++
        button.remove()
        if (end === 'decision') document.querySelector('[data-testid="vote"]').disabled = false
        else { const verdict = document.createElement('div'); verdict.className = 'settlement'; document.body.append(verdict) }
      }
      document.body.append(button)
    }, end)
    await reading
    expect(await page.evaluate(() => window.clicks)).toBe(2)
    await expect(page.getByTestId('next-event')).toHaveCount(0)
  })
}
