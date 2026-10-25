import { test, expect } from '@playwright/test'
import { readFileSync } from 'node:fs'
import { publicSnapshot } from '../test-fixtures/public-players.js'
import { drainEvents } from './drain-events.js'
const file=JSON.parse(readFileSync(new URL('../../src/test/resources/replay/exchange-v3.json',import.meta.url),'utf8'))
const key='vocr.guest.v1'
const saved=page=>page.evaluate(key=>JSON.parse(sessionStorage.getItem(key)),key)
const library=page=>page.getByRole('region',{name:'ローカルReplay',exact:true})
const rows=page=>library(page).locator('.replay-entries > li')
const stored=page=>page.evaluate(async()=>{const{createReplayStore}=await import('/src/replayStore.js');return createReplayStore().list()})
async function synthetic(page, response=file) {
  const id='12345678-1234-1234-1234-123456789012'
  const snapshot=publicSnapshot();snapshot.phase='FINISHED';snapshot.view.result='VILLAGE_WIN'
  snapshot.view.players=snapshot.view.players.slice(0,3)
  snapshot.revealedRoles=snapshot.view.players.map(p=>({playerNumber:p.playerNumber,actualRole:'wu'}))
  const presented={stage:'SETTLEMENT',event:{ch1:'Abel',ch2:'NONE',ch3:'NONE',eventname:'crsl'}}
  snapshot.events=[presented.event];snapshot.presentedEvents=[presented];snapshot.eventCursor=0;snapshot.nextEventCursor=1
  await page.route('**/api/v1/games/'+id+'?**',route=>{
    const cursor=Number(new URL(route.request().url()).searchParams.get('eventCursor'))
    return route.fulfill({json:{...snapshot,eventCursor:cursor,events:cursor?[]:snapshot.events,presentedEvents:cursor?[]:snapshot.presentedEvents}})
  })
  await page.route('**/api/v1/games/'+id+'/replay?*',route=>route.fulfill({json:response}))
  await page.addInitScript(({key,id})=>{if(!sessionStorage.getItem(key))sessionStorage.setItem(key,JSON.stringify({sessionId:id,eventCursor:0}))},{key,id})
  await page.goto('/');await expect(page.getByTestId('next-event')).toBeVisible()
  return id
}

test('real finished game saves compatible v3 exactly once; search/detail/delete/reload stay local and preserve authority',async({page,request},info)=>{
  let replayReads=0
  page.on('request',req=>{if(req.url().includes('/replay?'))replayReads++})
  await page.goto('/');await page.getByRole('button',{name:'対局を始める'}).click()
  await expect(page.getByTestId('next-event')).toBeVisible()
  const id=(await saved(page)).sessionId
  try {
    expect((await request.get(`/api/v1/games/${id}/replay?eventCursor=0`)).status()).toBe(409)
    await expect(page.getByTestId('save-replay')).toHaveCount(0)
    let turns=0
    while(true) {
      await drainEvents(page)
      if(await page.locator('.settlement').count())break
      expect(++turns).toBeLessThan(40)
      await page.getByTestId('vote-FREE').click();await expect(page.getByTestId('next-event')).toBeVisible()
      await expect(page.getByTestId('save-replay')).toHaveCount(0)
    }
    expect(replayReads).toBe(0)
    const beforeStorage=await saved(page), path=`/api/v1/games/${id}?eventCursor=${beforeStorage.eventCursor}`
    const before=await(await request.get(path)).json()
    const legacy=await(await request.get(`/api/v1/games/${id}/replay?eventCursor=${beforeStorage.eventCursor}`)).json()
    expect((await request.get(`/api/v1/games/${id}/replay?eventCursor=${beforeStorage.eventCursor-1}`)).status()).toBe(400)
    let writes=0;page.on('request',req=>{if(req.url().includes('/api/v1/games')&&req.method()!=='GET')writes++})
    await page.getByTestId('save-replay').click()
    await expect(rows(page)).toHaveCount(1)
    await expect(library(page)).toContainText('保存しました')
    await page.getByTestId('save-replay').click()
    await expect(library(page)).toContainText('保存済み')
    expect(replayReads).toBe(2)
    const entries=await stored(page);expect(entries).toHaveLength(1)
    const actual=entries[0].replay;delete actual.saveTime;delete legacy[0].saveTime
    expect(actual).toEqual(legacy[0])
    expect(JSON.stringify(entries)).not.toContain(id)
    expect(await saved(page)).toEqual(beforeStorage)
    expect(await(await request.get(path)).json()).toEqual(before)
    expect(writes).toBe(0)
    await rows(page).getByRole('button',{name:'詳細を確認'}).click()
    await expect(page.getByRole('region',{name:'Replay詳細'})).toContainText('v3')
    await library(page).getByRole('searchbox').fill('存在しない日時')
    await expect(rows(page)).toHaveCount(0)
    await library(page).getByRole('searchbox').fill('簡易')
    await expect(rows(page)).toHaveCount(1)
    await page.screenshot({path:info.outputPath('replay-library.png'),fullPage:true})
    await request.delete('/api/v1/games/'+id)
    await page.reload();await expect(page.getByRole('button',{name:'対局を始める'})).toBeVisible()
    await library(page).getByRole('button',{name:'Replay一覧',exact:true}).click()
    await expect(rows(page)).toHaveCount(1)
    await rows(page).getByRole('button',{name:'削除',exact:true}).click()
    await rows(page).getByRole('button',{name:'残す',exact:true}).click()
    await expect(rows(page)).toHaveCount(1)
    await rows(page).getByRole('button',{name:'削除',exact:true}).click()
    await rows(page).getByRole('button',{name:'削除を確定',exact:true}).click()
    await expect(rows(page)).toHaveCount(0)
    await page.reload();expect(await stored(page)).toHaveLength(0)
  }finally{await request.delete('/api/v1/games/'+id)}
})

test('synthetic final event gate, duplicate/capacity and concurrent multi-tab transactions do not overwrite entries',async({page,context})=>{
  await synthetic(page)
  await expect(page.getByTestId('save-replay')).toHaveCount(0)
  await page.getByTestId('next-event').click()
  await page.getByTestId('save-replay').click();await expect(rows(page)).toHaveCount(1)
  const other=await context.newPage();await other.goto('/')
  const write=async(target,n)=>target.evaluate(async({file,n})=>{
    const{createReplayStore}=await import('/src/replayStore.js')
    const replay=structuredClone(file[0]);replay.rawRecords.unshift('# fixture '+n);replay.totalEvents++
    try{return await createReplayStore().save(replay)}catch(e){return {code:e.code}}
  },{file,n})
  const duplicates=await Promise.all([write(page,1),write(other,1),write(page,1)])
  expect(duplicates.filter(r=>!r.duplicate)).toHaveLength(1)
  for(let n=2;n<9;n++)await write(page,n)
  const before=await stored(page);expect(before).toHaveLength(9)
  expect(await write(other,9)).toEqual({code:'LIBRARY_FULL'})
  expect(await stored(page)).toEqual(before)
  await library(page).getByRole('button',{name:'一覧を再読込'}).click();await expect(rows(page)).toHaveCount(9)
  await library(page).getByRole('combobox',{name:'勝敗',exact:true}).selectOption('2');await expect(rows(page)).toHaveCount(0)
  await library(page).getByRole('combobox',{name:'勝敗',exact:true}).selectOption('1');await expect(rows(page)).toHaveCount(9)
  await rows(page).first().getByRole('button',{name:'削除',exact:true}).click()
  await rows(page).first().getByRole('button',{name:'削除を確定'}).click()
  await expect(rows(page)).toHaveCount(8)
  expect(await write(other,9)).toMatchObject({duplicate:false})
  await other.close()
})

test('quota failure aborts transaction, keeps existing good data, permits explicit retry',async({page})=>{
  await synthetic(page);await page.getByTestId('next-event').click()
  await page.evaluate(async file=>{const{createReplayStore}=await import('/src/replayStore.js');const replay=structuredClone(file[0]);replay.rawRecords.unshift('# already good');replay.totalEvents++;await createReplayStore().save(replay)},file)
  const before=await stored(page)
  await page.evaluate(()=>{
    window.originalReplayAdd=IDBObjectStore.prototype.add
    IDBObjectStore.prototype.add=function(...args){if(this.name==='recordings')throw new DOMException('test quota','QuotaExceededError');return window.originalReplayAdd.apply(this,args)}
  })
  await page.getByTestId('save-replay').click()
  await expect(library(page).getByRole('alert')).toContainText('QUOTA_EXCEEDED')
  expect(await stored(page)).toEqual(before)
  await page.evaluate(()=>{IDBObjectStore.prototype.add=window.originalReplayAdd})
  await page.getByTestId('save-replay').click();await expect(rows(page)).toHaveCount(2)
})

for(const failure of ['invalid','network','html','storage','corrupt','hash'])test('explicit '+failure+' failure never overwrites a good local Replay or disables game operations',async({page})=>{
  await synthetic(page);await page.getByTestId('next-event').click()
  await page.evaluate(async file=>{const{createReplayStore}=await import('/src/replayStore.js');await createReplayStore().save(file[0])},file)
  const before=await stored(page)
  if(failure==='invalid')await page.route('**/replay?*',route=>route.fulfill({json:{secret:'DO_NOT_RENDER'}}))
  if(failure==='network')await page.route('**/replay?*',route=>route.abort())
  if(failure==='html')await page.route('**/replay?*',route=>route.fulfill({body:'<b>DO_NOT_RENDER</b>',contentType:'text/html'}))
  if(failure==='storage')await page.evaluate(()=>{window.originalReplayOpen=indexedDB.open.bind(indexedDB);indexedDB.open=()=>{throw new DOMException('denied','SecurityError')}})
  if(['corrupt','hash'].includes(failure))await page.evaluate(async failure=>{
    await new Promise((resolve,reject)=>{const req=indexedDB.open('vocr.replays.v1',1);req.onsuccess=()=>{const db=req.result,tx=db.transaction('recordings','readwrite'),s=tx.objectStore('recordings'),all=s.getAll();all.onsuccess=()=>{const entry=all.result[0];if(failure==='corrupt')entry.bytes=0;else entry.replay.daySnapshots[0].players[0].actualRole=2;s.put(entry)};tx.oncomplete=()=>{db.close();resolve()};tx.onabort=()=>reject(tx.error)}})
  },failure)
  const rawBefore=failure==='storage'?before:await rawEntries(page)
  await page.getByTestId('save-replay').click()
  await expect(library(page).getByRole('alert')).toBeVisible()
  await expect(library(page)).not.toContainText('DO_NOT_RENDER')
  await expect(page.getByRole('button',{name:'再同期',exact:true})).toBeEnabled()
  if(failure==='storage')await page.evaluate(()=>{indexedDB.open=window.originalReplayOpen})
  expect(await rawEntries(page)).toEqual(rawBefore)
  if(!['corrupt','hash'].includes(failure))expect(await stored(page)).toEqual(before)
})


test('late read after session release is discarded without storing the previous game',async({page})=>{
  await synthetic(page);await page.getByTestId('next-event').click()
  let proceed
  const held=new Promise(resolve=>{proceed=resolve})
  await page.route('**/replay?*',async route=>{await held;await route.fulfill({json:file})})
  const sent=page.waitForRequest(r=>r.url().includes('/replay?'))
  await page.getByTestId('save-replay').click();await sent
  await page.route('**/api/v1/games/*',route=>route.request().method()==='DELETE'?route.fulfill({status:204}):route.continue())
  await page.getByRole('button',{name:'新しい対局',exact:true}).click()
  await page.getByRole('button',{name:'解放を確定',exact:true}).click()
  await expect(page.getByRole('button',{name:'対局を始める'})).toBeVisible()
  proceed()
  await expect(library(page).getByRole('alert')).toContainText('SESSION_CHANGED')
  expect(await stored(page)).toHaveLength(0)
})

async function rawEntries(page) {
  return page.evaluate(async()=>new Promise((resolve,reject)=>{
    const req=indexedDB.open('vocr.replays.v1',1)
    req.onsuccess=()=>{const db=req.result,tx=db.transaction('recordings','readonly'),get=tx.objectStore('recordings').getAll();get.onsuccess=()=>{db.close();resolve(get.result)};get.onerror=()=>reject(get.error)}
    req.onerror=()=>reject(req.error)
  }))
}

test('IndexedDB getter denied at boot still allows normal play and reports local library failure',async({page,request})=>{
  await page.addInitScript(()=>Object.defineProperty(window,'indexedDB',{configurable:true,get(){throw new DOMException('blocked','SecurityError')}}))
  await page.goto('/')
  await library(page).getByRole('button',{name:'Replay一覧',exact:true}).click()
  await expect(library(page).getByRole('alert')).toContainText('STORAGE_UNAVAILABLE')
  await page.getByRole('button',{name:'対局を始める'}).click()
  await expect(page.getByTestId('next-event')).toBeVisible()
  await request.delete('/api/v1/games/'+(await saved(page)).sessionId)
})
