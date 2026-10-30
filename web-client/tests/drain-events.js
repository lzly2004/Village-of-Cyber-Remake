import { expect } from '@playwright/test'
import { completeLocalReading } from '../test-fixtures/presentation-test.js'

// Read all current events, including those arriving while an authoritative request is pending.
// A missing next button alone never proves convergence. Inspect one coherent DOM snapshot.
export async function drainEvents(page) {
  for (let count = 0; count < 120;) {
    let state
    await expect.poll(async () => {
      state = await page.evaluate(() => {
        const phase = document.querySelector('[data-testid="game-stage"]')?.dataset.presentationPhase
        if (phase && phase !== 'event') return 'presentation'
        if (document.querySelector('[data-testid="game-stage"]')?.dataset.readingBlocking === 'true') return 'reading'
        const next = document.querySelector('[data-testid="next-event"]')
        if (next && !next.disabled) return 'event'
        const vote = document.querySelector('button[data-testid^="vote"]:not(:disabled)')
        if (!next && ((vote && !vote.disabled) || document.querySelector('.settlement'))) return 'ready'
        return 'waiting'
      })
      return state
    }, { message: 'event reading must converge to a readable event, decision or settlement' }).not.toBe('waiting')
    if (state === 'presentation') {
      // The opted-in journey fixture completes local scenes only; it never acknowledges an event.
      await expect(page.getByTestId('game-stage')).toHaveAttribute('data-presentation-phase', 'event')
      continue
    }
    if (state === 'ready') {
      await expect(page.getByTestId('next-event')).toHaveCount(0)
      return
    }
    if (state === 'reading') {
      await completeLocalReading(page)
      continue
    }
    await page.getByTestId('next-event').click()
    count++
  }
  throw new Error('event reading exceeded 120 events without convergence')
}
