import { test as base, expect } from '@playwright/test'
import { publicInformationDiagnostics } from '../tests/public-information-diagnostics.js'
const test = base.extend({
  diagnostics: async ({page}, use, info) => {
    const diagnostics = publicInformationDiagnostics(page, info)
    await use(diagnostics)
    await diagnostics.finish()
  },
})
// Deliberately fails, proving the real browser failure-attachment path. Never a product regression.
test('DIAGNOSTIC_PROBE: intentional public-info evidence capture', async ({page,request,diagnostics}) => {
  let id
  try {
    await page.goto('/')
    await page.getByRole('button',{name:'対局を始める'}).click()
    await diagnostics.step('first event visible', () => expect(page.getByTestId('next-event')).toBeVisible())
    id = await page.evaluate(() => JSON.parse(sessionStorage.getItem('vocr.guest.v1')).sessionId)
    diagnostics.snapshot(await (await request.get('/api/v1/games/'+id)).json())
    throw new Error('DIAGNOSTIC_PROBE: intentional failure to verify attachments')
  } finally { if (id) await request.delete('/api/v1/games/'+id) }
})
