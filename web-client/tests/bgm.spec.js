import { test, expect } from '@playwright/test'
const saved=page=>page.evaluate(()=>JSON.parse(sessionStorage.getItem('vocr.guest.v1')))

test('WEB-027 real session BGM controls never write game state or confirm an event',async({page,request})=>{
  const writes=[]
  page.on('request',r=>{if(r.url().includes('/api/v1/games')&&r.method()!=='GET')writes.push(r.method()+' '+new URL(r.url()).pathname)})
  await page.goto('/');await page.getByRole('button',{name:'开始对局'}).click()
  await expect(page.getByTestId('next-event')).toBeVisible()
  while(await page.getByTestId('next-event').count())await page.getByTestId('next-event').click()
  await expect(page.getByRole('button',{name:'曲を切替'})).toBeVisible()
  const before=await saved(page)
  const state=await(await request.get('/api/v1/games/'+before.sessionId+'?eventCursor='+before.eventCursor)).json()
  const count=writes.length
  await page.getByRole('button',{name:'音楽を有効にする'}).click()
  await expect(page.locator('.bgm-control')).toHaveAttribute('data-status','playing')
  await page.getByRole('button',{name:'曲を切替'}).click()
  await expect(page.locator('.bgm-control')).toHaveAttribute('data-status','playing')
  await page.getByRole('button',{name:'音楽を停止',exact:true}).click()
  await expect(page.locator('.bgm-control')).toHaveAttribute('data-status','off')
  expect(await saved(page)).toEqual(before);expect(writes.length).toBe(count)
  const after=await(await request.get('/api/v1/games/'+before.sessionId+'?eventCursor='+before.eventCursor)).json()
  expect(after).toEqual(state)
})
