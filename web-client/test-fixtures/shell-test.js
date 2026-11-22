import {test as base,expect} from '@playwright/test'

// Explicit precondition for existing auxiliary-panel business journeys. Production
// defaults stay untouched. Default-entry and pure-canvas tests use the base fixture.
export async function showShell(page){
  const shell=page.locator('.app-shell')
  await expect(shell).toBeVisible()
  if((await shell.getAttribute('class')).split(/\s+/).includes('game-only'))await page.keyboard.press('Escape')
  await expect(shell).not.toHaveClass(/game-only/)
}
export const test=base.extend({page:async({page},use)=>{
  const goto=page.goto.bind(page),reload=page.reload.bind(page)
  page.goto=async(...args)=>{const response=await goto(...args);await showShell(page);return response}
  page.reload=async(...args)=>{const response=await reload(...args);await showShell(page);return response}
  await use(page)
}})
export {expect}
