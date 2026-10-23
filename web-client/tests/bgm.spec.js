import { test, expect } from '@playwright/test'
const saved=page=>page.evaluate(()=>JSON.parse(sessionStorage.getItem('vocr.guest.v1')))

test('WEB-027/028 real session audio controls and settings never write game state or confirm an event',async({page,request})=>{
  const writes=[]
  page.on('request',r=>{if(r.url().includes('/api/v1/games')&&r.method()!=='GET')writes.push(r.method()+' '+new URL(r.url()).pathname)})
  await page.goto('/');await page.getByRole('button',{name:'対局を始める'}).click()
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
  for (const [name,value] of [['BGM音量',25],['効果音量',50]]) {
    await page.getByRole('slider',{name,exact:true}).evaluate((node,value)=>{node.value=String(value);node.dispatchEvent(new Event('input',{bubbles:true}))},value)
  }
  await page.getByRole('switch',{name:'消音',exact:true}).click()
  expect(await page.evaluate(()=>JSON.parse(localStorage.getItem('vocr.audio.v1')))).toEqual({version:1,bgmVolume:25,soundVolume:50,muted:true})
  await page.getByRole('button',{name:'音楽を停止',exact:true}).click()
  await expect(page.locator('.bgm-control')).toHaveAttribute('data-status','off')
  expect(await saved(page)).toEqual(before);expect(writes.length).toBe(count)
  const after=await(await request.get('/api/v1/games/'+before.sessionId+'?eventCursor='+before.eventCursor)).json()
  expect(after).toEqual(state)
})
