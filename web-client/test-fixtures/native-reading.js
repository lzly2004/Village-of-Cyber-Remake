import { expect } from '@playwright/test'

// Real media suites keep native time. Reveal only the current public line by
// clicking its text region (no button CLICK effect), never the final next-event.
export async function completeNativeReading(page) {
  const stage=page.getByTestId('game-stage')
  const text=page.getByTestId('stage-text')
  await expect(text).toBeVisible()
  if(await stage.getAttribute('data-reading-complete')!=='true')await text.click()
  if(await page.getByTestId('execution-continue').count()){
    await page.getByTestId('execution-continue').click()
    await expect(stage).toHaveAttribute('data-reading-step','consequence')
    await text.click()
  }
  await expect(stage).toHaveAttribute('data-reading-complete','true')
}
