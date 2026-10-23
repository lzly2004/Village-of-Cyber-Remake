import { test,expect } from '@playwright/test'
import { readFileSync } from 'node:fs'
import { replayId } from '../src/replayStore.js'
const fixture=JSON.parse(readFileSync(new URL('../../src/test/resources/replay/exchange-v3.json',import.meta.url),'utf8'))[0]
const library=page=>page.getByRole('region',{name:'ローカルReplay',exact:true})
const dialog=page=>page.getByRole('dialog',{name:/Replay再生/})
const rows=page=>library(page).locator('.replay-entries > li')
async function seed(page,replay=fixture) {
  const id=await replayId(replay)
  await page.evaluate(async entry=>new Promise((resolve,reject)=>{
    const r=indexedDB.open('vocr.replays.v1',1)
    r.onupgradeneeded=()=>r.result.createObjectStore('recordings',{keyPath:'id'})
    r.onsuccess=()=>{const db=r.result,tx=db.transaction('recordings','readwrite');tx.objectStore('recordings').put(entry);tx.oncomplete=()=>{db.close();resolve()};tx.onabort=()=>reject(tx.error)}
    r.onerror=()=>reject(r.error)
  }),{id,replay,bytes:Buffer.byteLength(JSON.stringify(replay)),savedAt:'2026-01-02T03:04:05.000Z'})
}
async function play(page) {
  await library(page).getByRole('button',{name:'Replay一覧',exact:true}).click()
  await rows(page).getByRole('button',{name:'再生する',exact:true}).click()
  await expect(dialog(page)).toBeVisible()
}
// All cards keep their entire vertical content; horizontal browsing must never hide a row.
async function completeCards(page) {
  const metrics=await dialog(page).locator('.replay-avatar-list').evaluate(el=>{
    const region=el.getBoundingClientRect()
    return {height:el.clientHeight,contentHeight:el.scrollHeight,cards:[...el.children].map(card=>{
      const box=card.getBoundingClientRect()
      return {inside:box.top>=region.top && box.bottom<=region.bottom,children:[...card.children].map(child=>{
        const r=child.getBoundingClientRect()
        return r.top>=box.top && r.bottom<=box.bottom && r.left>=box.left && r.right<=box.right
      })}
    })}
  })
  expect(metrics.contentHeight).toBeLessThanOrEqual(metrics.height+1)
  expect(metrics.cards.every(card=>card.inside && card.children.every(Boolean))).toBe(true)
}
const saved=page=>page.evaluate(()=>JSON.parse(sessionStorage.getItem('vocr.guest.v1')))

test('frozen desktop dates/portraits play read-only beside a real running game, Escape and refresh preserve authority and local records',async({page,request},info)=>{
  await page.goto('/');await page.getByRole('button',{name:'対局を始める'}).click()
  await expect(page.getByTestId('next-event')).toBeVisible()
  const credentials=await saved(page),path='/api/v1/games/'+credentials.sessionId+'?eventCursor='+credentials.eventCursor
  try{
    const before=await(await request.get(path)).json()
    const current=await page.getByTestId('event-dialogue').textContent()
    await seed(page)
    let writes=0,reads=0,audio=0
    page.on('request',r=>{if(r.url().includes('/api/v1/games')){if(r.method()!=='GET')writes++;if(r.url().includes('/replay?'))reads++}if(r.url().includes('/assets/audio/'))audio++})
    await play(page)
    await expect(page.getByTestId('replay-day')).toHaveText('第2日 / 4日')
    await expect(page.getByTestId('replay-population')).toHaveText('生存者 3 · 死亡者 0 · 吊り縄 1')
    const wolf=dialog(page).locator('[data-player-index="2"]')
    await expect(wolf).toHaveAttribute('data-portrait','images/28ws.png')
    await expect(wolf).toHaveAttribute('data-departed','false')
    const hunter=dialog(page).locator('[data-player-index="3"]')
    await expect(hunter).toContainText('狩人') // avatar day 3 CO is shown at selected day 2, exactly as desktop.
    await expect(hunter.locator('.claim-icon img')).toBeVisible()
    await expect(dialog(page).getByRole('button',{name:'前の日',exact:true})).toBeDisabled()
    await dialog(page).getByRole('button',{name:'次の日',exact:true}).click()
    await expect(page.getByTestId('replay-day')).toHaveText('第3日 / 4日')
    await expect(page.getByTestId('replay-population')).toHaveText('生存者 2 · 死亡者 1 · 吊り縄 0')
    await expect(wolf).toHaveAttribute('data-portrait','images/28ws.png')
    await expect(wolf).toHaveAttribute('data-departed','true')
    await expect(wolf.locator('.death-marker img')).toBeVisible()
    await dialog(page).getByRole('combobox',{name:'再生日',exact:true}).selectOption('4')
    await expect(dialog(page).getByRole('button',{name:'次の日',exact:true})).toBeDisabled()
    await expect(dialog(page).locator('.visual-diagnostic')).toHaveCount(0)
    for(const width of [1440,1280,1024]) {
      await page.setViewportSize({width,height:1000});await completeCards(page)
    }
    await page.setViewportSize({width:1440,height:1000})
    await dialog(page).screenshot({path:info.outputPath('replay-day4.png')})
    expect(await saved(page)).toEqual(credentials)
    expect(writes).toBe(0);expect(reads).toBe(0);expect(audio).toBe(0)
    expect(await(await request.get(path)).json()).toEqual(before)
    await page.keyboard.press('Escape');await expect(dialog(page)).toHaveCount(0)
    await expect(page.getByTestId('event-dialogue')).toHaveText(current)
    await expect(page.locator('.player-identity').first()).not.toContainText('実際の役職')
    await expect(rows(page).getByRole('button',{name:'再生する'})).toBeFocused()
    await page.reload();await expect(page.getByTestId('event-dialogue')).toHaveText(current)
    expect(await saved(page)).toEqual(credentials)
    await play(page);await expect(page.getByTestId('replay-day')).toHaveText('第2日 / 4日')
    const data=await page.evaluate(async()=>{const{createReplayStore}=await import('/src/replayStore.js');return (await createReplayStore().list())[0].replay})
    expect(data).toEqual(fixture)
  }finally{await request.delete('/api/v1/games/'+credentials.sessionId)}
})

test('missing-date fallback is explicit; missing population never becomes a fabricated count and navigation remains usable',async({page})=>{
  await page.goto('/')
  const gap=structuredClone(fixture);gap.daySnapshots=gap.daySnapshots.filter(s=>[1,4].includes(s.dayNumber))
  await seed(page,gap);await play(page)
  await expect(dialog(page)).toContainText('SNAPSHOT_INDEX_FALLBACK')
  await dialog(page).getByRole('combobox',{name:'再生日',exact:true}).selectOption('3')
  await expect(dialog(page)).toContainText('SNAPSHOT_MISSING')
  await expect(page.getByTestId('replay-population')).toHaveCount(0)
  await expect(dialog(page)).toContainText('この日の人数記録はありません')
  await dialog(page).getByRole('button',{name:'次の日',exact:true}).click()
  await expect(page.getByTestId('replay-population')).toBeVisible()
})

test('44-player synthetic boundary keeps both desktop rows, keyboard access and final-date boundaries without page overflow',async({page},info)=>{
  await page.goto('/')
  const large=structuredClone(fixture)
  large.peiyiName='daxing';large.peiyiOrdinal=7;large.totalDays=50
  large.rawRecords=large.rawRecords.filter(r=>!r.startsWith('END|')).concat('END|result=VILLAGE_WIN|day=50');large.totalEvents=large.rawRecords.length
  const sample=structuredClone(large.daySnapshots[0])
  large.daySnapshots=[1,49,50].map(day=>({...sample,dayNumber:day,aliveCount:44,voteRounds:[],greyTargetCharNums:[],designatedTargetCharNums:[],players:Array.from({length:44},(_,i)=>({...sample.players[0],playerIndex:i+1,characterNumber:i+1,actualRole:6,claimedRole:0,claimedRoleOrder:0,comingOutDay:-1,deathDay:0,deathReason:0}))}))
  await seed(page,large);await play(page)
  await dialog(page).getByRole('combobox',{name:'再生日',exact:true}).selectOption('50')
  await expect(dialog(page).locator('.replay-player-card')).toHaveCount(44)
  for(const width of [1440,1280,1024]) {
    await page.setViewportSize({width,height:1000});await completeCards(page)
  }
  await page.setViewportSize({width:1440,height:1000})
  await dialog(page).screenshot({path:info.outputPath('replay-44-start.png')})
  const last=dialog(page).locator('.replay-player-card').last()
  await last.focus();await expect(last).toBeFocused()
  await completeCards(page)
  const region=dialog(page).getByRole('region',{name:'Replay人物一覧',exact:true})
  expect(await region.evaluate(el=>el.scrollLeft)).toBeGreaterThan(0)
  expect(await dialog(page).evaluate(el=>el.scrollWidth<=el.clientWidth+1)).toBe(true)
  await expect(dialog(page).getByRole('button',{name:'次の日',exact:true})).toBeDisabled()
  await dialog(page).screenshot({path:info.outputPath('replay-44.png')})
})

test('an already-open recording stays detached when another tab deletes it; closing and reloading reflects deletion',async({page})=>{
  await page.goto('/');await seed(page);await play(page)
  await page.evaluate(async()=>{const{createReplayStore}=await import('/src/replayStore.js');const store=createReplayStore(),entry=(await store.list())[0];await store.remove(entry.id)})
  await dialog(page).getByRole('button',{name:'次の日',exact:true}).click()
  await expect(page.getByTestId('replay-day')).toHaveText('第3日 / 4日')
  await dialog(page).getByRole('button',{name:'Replayを閉じる',exact:true}).click()
  await library(page).getByRole('button',{name:'一覧を再読込',exact:true}).click()
  await expect(rows(page)).toHaveCount(0)
})

test('lazy Replay module failure is bounded and leaves the current game usable',async({page,request})=>{
  await page.route('**/src/ReplayLibrary.vue*',r=>r.abort())
  await page.goto('/');await page.getByRole('button',{name:'対局を始める'}).click();await expect(page.getByTestId('next-event')).toBeVisible()
  const before=await saved(page)
  try{
    await library(page).getByRole('button',{name:'Replay一覧',exact:true}).click()
    await expect(library(page).getByRole('alert')).toContainText('REPLAY_LOAD_FAILED')
    await expect(page.getByTestId('next-event')).toBeEnabled()
    await library(page).getByRole('button',{name:'Replayを閉じる',exact:true}).click()
    expect(await saved(page)).toEqual(before)
  }finally{await request.delete('/api/v1/games/'+before.sessionId)}
})

test('lazy save intent cannot attach to a different session when the module arrives late',async({page,request})=>{
  const id='12345678-1234-1234-1234-123456789033'
  const {publicSnapshot}=await import('../test-fixtures/public-players.js')
  const snapshot=publicSnapshot();snapshot.phase='FINISHED';snapshot.view.result='VILLAGE_WIN'
  snapshot.events=[];snapshot.presentedEvents=[];snapshot.eventCursor=0;snapshot.nextEventCursor=0
  await page.route('**/api/v1/games/'+id+'?**',r=>r.fulfill({json:snapshot}))
  await page.route('**/api/v1/games/'+id,r=>r.fulfill({status:204}))
  await page.addInitScript(id=>sessionStorage.setItem('vocr.guest.v1',JSON.stringify({sessionId:id,eventCursor:0})),id)
  let resume
  const gate=new Promise(resolve=>{resume=resolve})
  await page.route('**/src/ReplayLibrary.vue*',async r=>{const response=await r.fetch();await gate;await r.fulfill({response})})
  let reads=0;page.on('request',r=>{if(r.url().includes('/replay?'))reads++})
  await page.goto('/')
  await page.getByTestId('save-replay').click()
  await page.getByRole('button',{name:'新しい対局',exact:true}).click()
  await page.getByRole('button',{name:'解放を確定',exact:true}).click()
  await page.getByRole('button',{name:'対局を始める'}).click();await expect(page.getByTestId('next-event')).toBeVisible()
  const current=await saved(page)
  try{
    resume()
    await expect(library(page).getByRole('alert')).toContainText('SESSION_CHANGED')
    expect(reads).toBe(0);expect(current.sessionId).not.toBe(id)
  }finally{resume();await request.delete('/api/v1/games/'+current.sessionId)}
})

test('timed-out lazy save does not auto-save when the chunk eventually arrives',async({page})=>{
  const id='12345678-1234-1234-1234-123456789034'
  const {publicSnapshot}=await import('../test-fixtures/public-players.js')
  const snapshot=publicSnapshot();snapshot.phase='FINISHED';snapshot.view.result='VILLAGE_WIN'
  snapshot.events=[];snapshot.presentedEvents=[];snapshot.eventCursor=0;snapshot.nextEventCursor=0
  await page.route('**/api/v1/games/'+id+'?**',r=>r.fulfill({json:snapshot}))
  await page.addInitScript(id=>sessionStorage.setItem('vocr.guest.v1',JSON.stringify({sessionId:id,eventCursor:0})),id)
  await page.clock.install()
  let resume,held=false,reads=0
  const gate=new Promise(resolve=>{resume=resolve})
  await page.route('**/src/ReplayLibrary.vue*',async r=>{const response=await r.fetch();held=true;await gate;await r.fulfill({response})})
  page.on('request',r=>{if(r.url().includes('/replay?'))reads++})
  try{
    await page.goto('/');await page.getByTestId('save-replay').click()
    await expect.poll(()=>held).toBe(true)
    await page.clock.fastForward(15001)
    await expect(library(page).getByRole('alert')).toContainText('REPLAY_LOAD_FAILED')
    resume()
    await expect(library(page).getByRole('alert')).toContainText('保存は実行していません')
    expect(reads).toBe(0)
  }finally{resume()}
})
