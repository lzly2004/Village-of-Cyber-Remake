import { test, expect } from '../test-fixtures/presentation-test.js'
import { publicSnapshot } from '../test-fixtures/public-players.js'
import { drainEvents } from './drain-events.js'
import { readFileSync } from 'node:fs'
const replayFile=JSON.parse(readFileSync(new URL('../../src/test/resources/replay/exchange-v3.json',import.meta.url),'utf8'))
const id='12345678-1234-4234-8234-123456789012'
const status=page=>page.getByRole('region',{name:'ローカル戦績保存'})
const read=page=>page.evaluate(async()=>{const{createRecordStore}=await import('/src/recordStore.js');return createRecordStore().read()})
async function synthetic(page,{denied=false}={}) {
  const snapshot=publicSnapshot();snapshot.phase='FINISHED';snapshot.view.result='VILLAGE_WIN'
  snapshot.view.players=snapshot.view.players.slice(0,3)
  snapshot.revealedRoles=snapshot.view.players.map(p=>({playerNumber:p.playerNumber,actualRole:'wu'}))
  const presented={stage:'SETTLEMENT',event:{ch1:'Abel',ch2:'NONE',ch3:'NONE',eventname:'crsl'}}
  snapshot.events=[presented.event];snapshot.presentedEvents=[presented];snapshot.nextEventCursor=1
  await page.route('**/api/v1/games/'+id+'?**',route=>{
    const cursor=Number(new URL(route.request().url()).searchParams.get('eventCursor'))
    return route.fulfill({json:{...snapshot,eventCursor:cursor,events:cursor?[]:snapshot.events,presentedEvents:cursor?[]:snapshot.presentedEvents}})
  })
  await page.addInitScript(({id,denied})=>{
    if(!sessionStorage.getItem('vocr.guest.v1'))sessionStorage.setItem('vocr.guest.v1',JSON.stringify({sessionId:id,eventCursor:0}))
    if(denied){const open=IDBFactory.prototype.open;IDBFactory.prototype.open=function(name,...rest){if(name==='vocr.records.v1')throw new DOMException('denied','SecurityError');return open.call(this,name,...rest)}}
  },{id,denied})
  await page.goto('/');await expect(page.getByTestId('next-event')).toBeVisible()
}
test('completion waits for acknowledgement, persists across refresh/resync and never alters authority',async({page})=>{
  let writes=0;page.on('request',r=>{if(r.url().includes('/api/')&&r.method()!=='GET')writes++})
  await synthetic(page);await expect(status(page)).toHaveCount(0)
  expect((await read(page)).totalPlayCnt).toBe(0)
  const before=await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))
  await page.getByTestId('next-event').click();await expect(status(page)).toContainText('保存しました')
  expect((await read(page)).totalPlayCnt).toBe(1)
  await page.getByRole('button',{name:'再同期',exact:true}).click();await expect(status(page)).toContainText('保存済み')
  await page.reload();await expect(status(page)).toContainText('保存済み');expect((await read(page)).totalPlayCnt).toBe(1)
  expect(JSON.parse(await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1')))).toEqual({...JSON.parse(before),eventCursor:1})
  expect(writes).toBe(0)
  const geometry=await status(page).evaluate(el=>({height:el.clientHeight,content:el.scrollHeight,width:el.clientWidth,contentWidth:el.scrollWidth}))
  expect(geometry.content).toBeLessThanOrEqual(geometry.height+1);expect(geometry.contentWidth).toBeLessThanOrEqual(geometry.width+1)
  await page.screenshot({path:test.info().outputPath('records-recovered.png'),fullPage:true})
})
test('independent tabs serialize duplicate settlement; conflicting result and corrupt row fail without overwrite',async({page,context})=>{
  await page.goto('/')
  const second=await context.newPage();await second.goto('/')
  const put=(p,result='VILLAGE_WIN')=>p.evaluate(async({id,result})=>{const{createRecordStore}=await import('/src/recordStore.js');try{return await createRecordStore().record({sessionId:id,configuration:'jianyi',result})}catch(e){return e.code}},{id,result})
  const both=await Promise.all([put(page),put(second)]);expect(both.filter(r=>r.duplicate===false)).toHaveLength(1);expect(both.filter(r=>r.duplicate===true)).toHaveLength(1)
  expect((await read(second)).totalPlayCnt).toBe(1)
  expect(await put(second,'FOX_WIN')).toBe('RESULT_CONFLICT');expect((await read(page)).totalFoxWin).toBe(0)
  await page.evaluate(()=>new Promise((resolve,reject)=>{const q=indexedDB.open('vocr.records.v1');q.onsuccess=()=>{const db=q.result,t=db.transaction('results','readwrite');t.objectStore('results').add({id:'bad',configuration:'jianyi',result:'VILLAGE_WIN'});t.oncomplete=()=>{db.close();resolve()};t.onabort=()=>reject(t.error)}}))
  const error=await page.evaluate(async()=>{const{createRecordStore}=await import('/src/recordStore.js');try{await createRecordStore().read()}catch(e){return e.code}});expect(error).toBe('INVALID_HISTORY')
  expect(await put(page)).toBe('INVALID_HISTORY')
  await second.close()
})
test('denied storage is visible, retry remains available and game confirmation still works',async({page})=>{
  await synthetic(page,{denied:true});await page.getByTestId('next-event').click()
  await expect(status(page)).toContainText('STORAGE_UNAVAILABLE');await expect(page.locator('.settlement')).toBeVisible()
  await page.getByRole('button',{name:'戦績保存を再試行'}).click();await expect(status(page)).toContainText('STORAGE_UNAVAILABLE')
  expect(await page.evaluate(()=>JSON.parse(sessionStorage.getItem('vocr.guest.v1')).eventCursor)).toBe(1)
})
test('aborted quota transaction is not success; explicit retry counts once and preserves audio/replay keys',async({page})=>{
  await synthetic(page)
  await page.evaluate(()=>{localStorage.setItem('unrelated-test','keep');const add=IDBObjectStore.prototype.add;IDBObjectStore.prototype.add=function(...args){if(this.name==='results'&&!window.quotaReleased){this.transaction.abort();throw new DOMException('quota','QuotaExceededError')}return add.apply(this,args)}})
  await page.getByTestId('next-event').click();await expect(status(page)).toContainText('QUOTA_EXCEEDED')
  expect((await read(page)).totalPlayCnt).toBe(0)
  await page.evaluate(()=>{window.quotaReleased=true});await page.getByRole('button',{name:'戦績保存を再試行'}).click();await expect(status(page)).toContainText('保存しました')
  expect((await read(page)).totalPlayCnt).toBe(1);expect(await page.evaluate(()=>localStorage.getItem('unrelated-test'))).toBe('keep')
})
test('two real completed games accumulate, abandoning and replay import do not; new tab retains history',async({page,context,request})=>{
  await page.goto('/')
  const ids=[]
  try {
    for(let game=0;game<2;game++){
      await page.getByRole('button',{name:'対局を始める',exact:true}).click();await expect(page.getByTestId('next-event')).toBeVisible()
      const sid=await page.evaluate(()=>JSON.parse(sessionStorage.getItem('vocr.guest.v1')).sessionId);ids.push(sid)
      for(let turn=0;turn<40;turn++){
        await drainEvents(page)
        if(await page.locator('.settlement').count())break
        await page.getByTestId('vote-FREE').click()
      }
      await expect(page.locator('.settlement')).toBeVisible();await expect(status(page)).toContainText('保存しました')
      expect((await read(page)).totalPlayCnt).toBe(game+1)
      await page.reload();await expect(status(page)).toContainText('保存済み');expect((await read(page)).totalPlayCnt).toBe(game+1)
      await request.delete('/api/v1/games/'+sid)
      await page.evaluate(()=>sessionStorage.removeItem('vocr.guest.v1'));await page.reload()
    }
    await page.getByRole('button',{name:'対局を始める',exact:true}).click();await expect(page.getByTestId('next-event')).toBeVisible();ids.push(await page.evaluate(()=>JSON.parse(sessionStorage.getItem('vocr.guest.v1')).sessionId))
    expect((await read(page)).totalPlayCnt).toBe(2)
    const other=await context.newPage();await other.goto('/');expect((await read(other)).totalPlayCnt).toBe(2);await other.close()
    await page.evaluate(async file=>{const{createReplayStore}=await import('/src/replayStore.js');await createReplayStore().saveMany(file.filter(Boolean))},replayFile)
    expect((await read(page)).totalPlayCnt).toBe(2)
    const raw=await page.evaluate(()=>new Promise(resolve=>{const q=indexedDB.open('vocr.records.v1');q.onsuccess=()=>{const db=q.result,t=db.transaction('results'),r=t.objectStore('results').getAll();r.onsuccess=()=>resolve(JSON.stringify(r.result));t.oncomplete=()=>db.close()}}))
    for(const sid of ids)expect(raw).not.toContain(sid)
  }finally{for(const sid of ids)await request.delete('/api/v1/games/'+sid)}
})
