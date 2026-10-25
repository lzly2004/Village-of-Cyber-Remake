import { expect } from '@playwright/test'

// Read all current events, including those arriving while an authoritative request is pending.
// A missing next button alone never proves convergence. Inspect one coherent DOM snapshot.
export async function drainEvents(page) {
  for (let count = 0; count < 120; count++) {
    let state
    await expect.poll(async () => {
      state = await page.evaluate(() => {
        const next = document.querySelector('[data-testid="next-event"]')
        if (next && !next.disabled) return 'event'
        const vote = document.querySelector('button[data-testid^="vote"]:not(:disabled)')
        if (!next && ((vote && !vote.disabled) || document.querySelector('.settlement'))) return 'ready'
        return 'waiting'
      })
      return state
    }, { message: 'event reading must converge to a readable event, decision or settlement' }).not.toBe('waiting')
    if (state === 'ready') {
      await expect(page.getByTestId('next-event')).toHaveCount(0)
      return
    }
    await page.getByTestId('next-event').click()
  }
  throw new Error('event reading exceeded 120 events without convergence')
}
