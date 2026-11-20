import {test,expect} from '@playwright/test'
import {publicSnapshot} from '../test-fixtures/public-players.js'
import {preparePresentation} from '../test-fixtures/presentation-test.js'
import {startDesktopGame} from '../test-fixtures/desktop-start.js'
import {mkdirSync,readFileSync} from 'node:fs'
const replay=JSON.parse(readFileSync(new URL('../../src/test/resources/replay/exchange-v3.json',import.meta.url)))[0]

async function enter(page){await page.getByTestId('game-only-enter').click();await expect(page.locator('.app-shell')).toHaveClass(/game-only/)}
async function escape(page){await page.keyboard.press('Escape');await expect(page.locator('.app-shell')).not.toHaveClass(/game-only/)}
async function geometry(page,canvas){
 await expect.poll(async()=>{const r=await canvas.boundingBox(),v=page.viewportSize();return Boolean(r && r.x>=-1 && r.y>=-1 && r.x+r.width<=v.width+1 && r.y+r.height<=v.height+1)}).toBe(true)
 const r=await canvas.boundingBox(),v=page.viewportSize()
 expect(r.width/r.height).toBeCloseTo(16/9,3)
 expect(r.x).toBeGreaterThanOrEqual(-1);expect(r.y).toBeGreaterThanOrEqual(-1)
 expect(r.x+r.width).toBeLessThanOrEqual(v.width+1);expect(r.y+r.height).toBeLessThanOrEqual(v.height+1)
 expect(Math.abs(r.x-(v.width-r.width)/2)).toBeLessThan(1)
 expect(Math.abs(r.y-(v.height-r.height)/2)).toBeLessThan(1)
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth && document.documentElement.scrollHeight<=innerHeight)).toBe(true)
}

test('game-only is the default on load and reload, fits each viewport and never creates a session',async({page})=>{
 const writes=[];page.on('request',r=>{if(r.url().includes('/api/')&&r.method()!=='GET')writes.push(r.method())})
 await page.goto('/');await expect(page.getByTestId('desktop-title')).toBeVisible()
 await expect(page.locator('.app-shell')).toHaveClass(/game-only/)
 await expect(page.locator('.sidebar')).toBeHidden();await geometry(page,page.getByTestId('desktop-title'))
 await page.getByTestId('title-start').click();await expect(page.getByTestId('desktop-select')).toBeVisible();await expect(page.locator('.app-shell')).toHaveClass(/game-only/)
 await page.getByTestId('setup-return').click();await escape(page)
 mkdirSync('../build/verify/game-only',{recursive:true})
 await page.screenshot({path:'../build/verify/game-only/title-full-page.png',fullPage:true})
 await enter(page)
 await expect(page.getByTestId('game-only-enter')).toBeHidden();await expect(page.locator('.sidebar')).toBeHidden();await expect(page.locator('.topbar')).toBeHidden()
 for(const [width,height] of [[1440,1000],[800,600],[1600,600]]){
  await page.setViewportSize({width,height});await geometry(page,page.getByTestId('desktop-title'))
 }
 await page.setViewportSize({width:1440,height:1000});await page.getByTestId('game-only-enable-audio').click();await expect(page.getByTestId('game-only-status')).toHaveCount(0);await page.screenshot({path:'../build/verify/game-only/title-game-only.png'})
 await escape(page);await expect(page.getByTestId('game-only-enter')).toBeFocused();await expect(page.locator('.sidebar')).toBeVisible()
 await page.reload();await expect(page.locator('.app-shell')).toHaveClass(/game-only/);await expect(page.getByTestId('game-only-enter')).toBeHidden();await geometry(page,page.getByTestId('desktop-title'));expect(writes).toEqual([])
})

test('game-only Esc cancels native confirmation first, but preserves Info, Record and Replay scenes',async({page})=>{
 await page.goto('/');await expect(page.locator('.app-shell')).toHaveClass(/game-only/)
 await page.getByTestId('title-help').click();await geometry(page,page.getByTestId('desktop-help'))
 await escape(page);await expect(page.getByTestId('desktop-help')).toBeVisible()
 await page.keyboard.press('Escape');await expect(page.getByTestId('desktop-title')).toBeVisible()
 await enter(page);await page.getByTestId('title-records').click();await geometry(page,page.getByTestId('desktop-records'))
 await escape(page);await expect(page.getByTestId('desktop-records')).toBeVisible()
 await page.keyboard.press('Escape');await expect(page.getByTestId('desktop-title')).toBeVisible()
 await enter(page);await page.getByTestId('title-replay').click();await expect(page.getByTestId('desktop-replay-browser')).toBeVisible();await geometry(page,page.getByTestId('desktop-replay-browser'))
 await expect(page.locator('.replay-browser-management')).toBeHidden()
 await page.getByTestId('desktop-replay-export').click();const dialog=page.locator('dialog[open]');await expect(dialog).toBeVisible()
 await page.keyboard.down('Escape');await expect(dialog).toHaveCount(0);await expect(page.locator('.app-shell')).toHaveClass(/game-only/)
 await page.keyboard.down('Escape');await expect(page.locator('.app-shell')).toHaveClass(/game-only/);await page.keyboard.up('Escape')
 await escape(page)
 await page.getByTestId('replay-return').click();await expect(page.getByTestId('desktop-title')).toBeVisible()
 await page.evaluate(async replay=>{const{createReplayStore}=await import('/src/replayStore.js');await createReplayStore().saveMany([replay])},replay)
 await enter(page);await page.getByTestId('title-replay').click();await page.locator('[data-slot="0"]').click();await geometry(page,page.getByTestId('desktop-replay-player'))
 await escape(page);await expect(page.getByTestId('desktop-replay-player')).toBeVisible()
 await page.locator('.desktop-replay-player').focus();await page.keyboard.press('Escape');await expect(page.getByTestId('desktop-replay-browser')).toBeVisible()
})

test('game-only keeps live authority/audio mounted and exposes read recovery inside the canvas',async({page})=>{
 const id='00000000-0000-4000-8000-000000005018',snapshot=publicSnapshot(),calls=[];let failRead=false
 await page.route('**/api/v1/games',async route=>{calls.push(route.request().method());await route.fulfill({status:201,json:{sessionId:id,update:snapshot}})})
 await page.route('**/api/v1/games/'+id+'?**',route=>route.fulfill(failRead?{status:503,json:{error:{code:'UNAVAILABLE',message:'test failure'}}}:{json:snapshot}))
 await page.goto('/');await escape(page);await startDesktopGame(page)
 await page.getByRole('button',{name:'音楽を有効にする',exact:true}).click();await expect(page.locator('.bgm-control')).toHaveAttribute('data-status','playing')
 const saved=await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))
 await enter(page);await geometry(page,page.getByTestId('stage-surface'));await expect(page.locator('.bgm-control')).toHaveAttribute('data-status','playing')
 await expect(page.getByTestId('game-only-enable-audio')).toHaveCount(0)
 expect(await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))).toBe(saved)
 await page.screenshot({path:'../build/verify/game-only/game-game-only.png'})
 await escape(page);failRead=true;await page.getByRole('button',{name:'再同期',exact:true}).click();await expect(page.locator('main > .notice').first()).toBeVisible()
 await enter(page);await expect(page.getByTestId('game-only-status').getByRole('alert')).toBeVisible()
 failRead=false;await page.getByTestId('game-only-status').getByRole('button',{name:'再同期',exact:true}).click()
 await expect(page.getByTestId('game-only-status')).toHaveCount(0);expect(calls).toEqual(['POST'])
 expect(await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))).toBe(saved)
})

test('game-only actual first dialogue keeps read progression and restores the same live session',async({page})=>{
 await preparePresentation(page,{reading:false})
 await page.goto('/');await expect(page.locator('.app-shell')).toHaveClass(/game-only/);await startDesktopGame(page)
 await expect(page.locator('.message-surface')).toBeVisible();await geometry(page,page.getByTestId('stage-surface'))
 const session=await page.evaluate(()=>JSON.parse(sessionStorage.getItem('vocr.guest.v1')).sessionId)
 expect(session).toMatch(/^[0-9a-f-]{36}$/)
 await page.getByTestId('game-only-enable-audio').click();await expect(page.locator('.bgm-control')).toHaveAttribute('data-status','silent')
 await page.locator('.message-surface').click();await expect(page.getByTestId('game-stage')).toHaveAttribute('data-reading-complete','true')
 await page.screenshot({path:'../build/verify/game-only/dialogue-game-only.png'})
 await escape(page);expect(await page.evaluate(()=>JSON.parse(sessionStorage.getItem('vocr.guest.v1')).sessionId)).toBe(session)
 await page.screenshot({path:'../build/verify/game-only/game-full-page.png',fullPage:true})
 await enter(page);await page.locator('.message-surface').click();await expect(page.locator('.session-meta')).toContainText('確認済みイベント 1')
})

test('default canvas survives settlement, menu branches and second game without persisting shell preference',async({page})=>{
 const snapshot=publicSnapshot(),calls=[];let serial=0
 snapshot.phase='FINISHED';snapshot.view.result='VILLAGE_WIN'
 snapshot.revealedRoles=snapshot.view.players.map(p=>({playerNumber:p.playerNumber,role:'wu'}))
 await page.route('**/api/v1/games',route=>{calls.push('POST');serial++;return route.fulfill({status:201,json:{sessionId:'00000000-0000-4000-8000-'+String(serial).padStart(12,'0'),update:snapshot}})})
 await page.route(/\/api\/v1\/games\/[0-9a-f-]+$/,route=>{calls.push(route.request().method());return route.fulfill({status:204})})
 await page.goto('/');await startDesktopGame(page)
 await expect(page.getByTestId('settlement-board')).toBeVisible();await expect(page.locator('.app-shell')).toHaveClass(/game-only/)
 await expect.poll(()=>page.evaluate(async()=>{const{createRecordStore}=await import('/src/recordStore.js');return (await createRecordStore().read()).totalPlayCnt})).toBe(1)
 const before=await page.evaluate(async()=>{const{createRecordStore}=await import('/src/recordStore.js');return createRecordStore().read()})
 await page.getByTestId('settlement-board').getByRole('button',{name:'タイトルに戻る',exact:true}).click()
 for(const [entry,scene,back] of [['title-help','desktop-help','help-title-return'],['title-records','desktop-records','record-title-return'],['title-replay','desktop-replay-browser','replay-return']]){
  await page.getByTestId(entry).click();await expect(page.getByTestId(scene)).toBeVisible();await expect(page.locator('.app-shell')).toHaveClass(/game-only/)
  await page.getByTestId(back).click();await expect(page.getByTestId('desktop-title')).toBeVisible();await expect(page.locator('.app-shell')).toHaveClass(/game-only/)
 }
 expect(await page.evaluate(async()=>{const{createRecordStore}=await import('/src/recordStore.js');return createRecordStore().read()})).toEqual(before)
 await startDesktopGame(page,'tongchang');await expect(page.getByTestId('settlement-board')).toBeVisible();await expect(page.locator('.app-shell')).toHaveClass(/game-only/)
 expect(calls).toEqual(['POST','DELETE','POST'])
})
