import { test as base, expect } from '@playwright/test'

// Legacy business/geometry journeys explicitly opt into completing the new local
// presentation. Dedicated night-day.spec.js uses the base fixture and checks every
// real production timing boundary. This helper NEVER clicks next-event or a vote. It can complete local dialogue and the explicit execution-only continue.

// Explicit legacy-journey precondition. It completes ONLY already-public local
// reading/animation, never next-event, a vote, fetch or storage mutation.
const completingReading=new WeakSet()
export async function completeLocalReading(page) {
  if(completingReading.has(page))return
  completingReading.add(page)
  try {
  const stage=page.getByTestId('game-stage')
  await expect.poll(async()=>await stage.getAttribute('data-reading-step')!=='waiting-image').toBe(true)
  let step=await stage.getAttribute('data-reading-step')
  if(['waiting-death','execution-transition'].includes(step))await page.clock.runFor(2000)
  // Legacy journeys need a fully read line, not thousands of individual ticks.
  // Use the approved local reveal action; strict cadence stays in base-fixture tests.
  if(await stage.getAttribute('data-reading-complete')!=='true'){
    await page.clock.runFor(1);await page.getByTestId('stage-text').click()
  }
  if(await page.getByTestId('execution-continue').count()){
    await page.clock.runFor(1)
    await page.getByTestId('execution-continue').click()
    await page.clock.runFor(2000)
    if(await stage.getAttribute('data-reading-complete')!=='true'){
      await page.clock.runFor(1);await page.getByTestId('stage-text').click()
    }
  }
  await expect(stage).toHaveAttribute('data-reading-complete','true')
  } finally { completingReading.delete(page) }
}

const prepared = new WeakSet()
export async function preparePresentation(page,{reading=true}={}) {
    if (prepared.has(page)) return
    prepared.add(page)
    await page.clock.install()
    // Explicit callers may click the local execution button themselves. Do not
    // recursively consume that same button from its actionability handler.
    if(reading)await page.addLocatorHandler(page.locator('[data-testid="game-stage"][data-reading-blocking="true"]'),()=>completeLocalReading(page),{noWaitAfter:true})
    const prelude = page.locator('[data-testid="game-stage"][data-presentation-phase]:not([data-presentation-phase="event"])')
    await page.addLocatorHandler(prelude, async () => {
      // Relative runFor disables real-time sync while advancing internally.
      // Never sample Date.now then pauseAt: transport delay can make that time past.
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
    })
}
export const test = base.extend({
  page: async ({ page }, use) => {
    await preparePresentation(page)
    await use(page)
  },
})
export const sceneTest=base.extend({page:async({page},use)=>{await preparePresentation(page,{reading:false});await use(page)}})
export { expect }
