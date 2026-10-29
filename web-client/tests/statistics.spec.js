import { test, expect } from '../test-fixtures/presentation-test.js'
const dialog=page=>page.getByRole('dialog',{name:'ブラウザーの戦績'})
const opener=page=>page.getByRole('button',{name:'戦績を見る',exact:true})
async function put(page,configuration,result,index) {
  return page.evaluate(async({configuration,result,index})=>{
    const{createRecordStore}=await import('/src/recordStore.js')
    return createRecordStore().record({sessionId:'00000000-0000-4000-8000-'+String(index).padStart(12,'0'),configuration,result})
  },{configuration,result,index})
}
async function open(page) {await opener(page).click();await expect(dialog(page).getByTestId('record-play')).toBeVisible()}
async function metrics(page,expected) {
  for(const [key,value] of Object.entries(expected))await expect(dialog(page).getByTestId('record-'+key)).toHaveText(value)
}
// Fixed interleaved results; expectations are hand-calculated, never copied from read()/accumulate().
const script=[['jianyi','V'],['tongchang','V'],['jianyi','V'],['yaoohu','F'],['jianyi','W'],['tongchang','V'],['jianyi','V'],['kuangxin','W'],['jianyi','V'],['beide','V'],['jianyi','V'],['maoyou','W'],['jianyi','F'],['yaoohu','V'],['maoyou','V'],['daxing','F']]
const results={V:'VILLAGE_WIN',W:'WOLF_WIN',F:'FOX_WIN'}
const expected=[
  ['総合',16,'62.50000%',3,null,10,3,3],
  ['簡易村',7,'71.42857%',3,0,5,1,1],
  ['通常村',2,'100.00000%',2,2,2,0,0],
  ['妖狐村',2,'50.00000%',1,1,1,0,1],
  ['狂信村',1,'0.00000%',0,0,0,1,0],
  ['背徳村',1,'100.00000%',1,1,1,0,0],
  ['猫又村',2,'50.00000%',1,1,1,1,0],
  ['大型村',1,'0.00000%',0,0,0,0,1],
]
test('fixed result script renders all eight desktop statistics and survives reopening/new tab',async({page,context},info)=>{
  await page.goto('/')
  for(const [i,[configuration,result]] of script.entries())await put(page,configuration,results[result],i+1)
  await open(page)
  for(const [name,play,rate,max,current,village,wolf,fox] of expected){
    await dialog(page).getByRole('button',{name,exact:true}).click()
    await expect(dialog(page).getByRole('heading',{level:3})).toHaveText(name)
    await metrics(page,{play:play+'回',rate,max:max+'回',village:village+'回',wolf:wolf+'回',fox:fox+'回'})
    if(current===null)await expect(dialog(page).getByTestId('record-current')).toHaveCount(0)
    else await metrics(page,{current:current+'回'})
  }
  await dialog(page).getByRole('button',{name:'総合',exact:true}).click()
  await page.screenshot({path:info.outputPath('statistics-fixed-results.png')})
  await page.keyboard.press('Escape');await expect(opener(page)).toBeFocused()
  await page.reload();await open(page);await metrics(page,{play:'16回',rate:'62.50000%',max:'3回'})
  const other=await context.newPage();await other.goto('/');await open(other);await metrics(other,{play:'16回'});await other.close()
})
test('empty ledger is zero, keyboard focus stays modal and every metric is reachable without clipping',async({page},info)=>{
  await page.goto('/');await opener(page).focus();await page.keyboard.press('Enter')
  await expect(dialog(page).getByText('この集計には、まだ保存済みの対局がありません。')).toBeVisible()
  await metrics(page,{play:'0回',rate:'0.00000%',max:'0回'})
  for(let n=0;n<15;n++){await page.keyboard.press('Tab');expect(await dialog(page).evaluate(el=>el.contains(document.activeElement))).toBe(true)}
  for(const width of [1440,1024,800]){
    await page.setViewportSize({width,height:720})
    await dialog(page).getByRole('button',{name:'大型村',exact:true}).click()
    for(const item of await dialog(page).locator('.record-metrics > div').all()){
      await item.scrollIntoViewIfNeeded()
      const g=await item.evaluate(el=>{const r=el.getBoundingClientRect(),d=el.closest('dialog').getBoundingClientRect();return{inside:r.top>=d.top&&r.bottom<=d.bottom,clipped:el.scrollHeight>el.clientHeight+1,width:el.scrollWidth>el.clientWidth+1}})
      expect(g).toEqual({inside:true,clipped:false,width:false})
    }
    expect(await dialog(page).evaluate(el=>el.scrollWidth<=el.clientWidth+1)).toBe(true)
    await dialog(page).evaluate(el=>{el.scrollTop=0})
    await page.screenshot({path:info.outputPath('statistics-'+width+'.png')})
  }
  await page.keyboard.press('Escape');await expect(dialog(page)).toHaveCount(0);await expect(opener(page)).toBeFocused()
})
test('viewing and refreshing during a live game does not read/write authority, move cursor or change storage',async({page,request})=>{
  await page.goto('/');await page.getByRole('button',{name:'対局を始める',exact:true}).click();await expect(page.getByTestId('next-event')).toBeVisible()
  const state=()=>page.evaluate(()=>({session:{...sessionStorage},local:{...localStorage}}))
  const before=await state(),id=JSON.parse(before.session['vocr.guest.v1']).sessionId
  const authority=await (await request.get('/api/v1/games/'+id+'?eventCursor=0')).json()
  const calls=[],media=[];page.on('request',r=>{if(r.url().includes('/api/'))calls.push(r.method());if(/\.(mp3|webp)(\?|$)/.test(r.url()))media.push(r.url())})
  try{
    await open(page);await dialog(page).getByRole('button',{name:'大型村',exact:true}).click();await dialog(page).getByRole('button',{name:'戦績を再読み込み',exact:true}).click();await metrics(page,{play:'0回'})
    await page.keyboard.press('Escape');await expect(page.getByTestId('next-event')).toBeVisible()
    expect(await state()).toEqual(before);expect(calls).toEqual([]);expect(media).toEqual([])
    expect(await (await request.get('/api/v1/games/'+id+'?eventCursor=0')).json()).toEqual(authority)
  }finally{await request.delete('/api/v1/games/'+id)}
})
test('refresh reads committed cross-tab results, corruption removes stale totals without deleting data',async({page,context})=>{
  await page.goto('/');await put(page,'jianyi','VILLAGE_WIN',1);await open(page);await metrics(page,{play:'1回'})
  const other=await context.newPage();await other.goto('/');await put(other,'jianyi','WOLF_WIN',2)
  await metrics(page,{play:'1回'});await dialog(page).getByRole('button',{name:'戦績を再読み込み'}).click();await metrics(page,{play:'2回',rate:'50.00000%'})
  await other.evaluate(()=>new Promise((resolve,reject)=>{const q=indexedDB.open('vocr.records.v1');q.onsuccess=()=>{const db=q.result,t=db.transaction('results','readwrite');t.objectStore('results').add({id:'bad',configuration:'jianyi',result:'VILLAGE_WIN'});t.oncomplete=()=>{db.close();resolve()};t.onabort=()=>reject(t.error)}}))
  await dialog(page).getByRole('button',{name:'戦績を再読み込み'}).click();await expect(dialog(page).getByRole('alert')).toContainText('INVALID_HISTORY');await expect(dialog(page).getByTestId('record-play')).toHaveCount(0)
  await expect(dialog(page).getByRole('alert')).toContainText('復旧にはまだ対応していません')
  expect(await other.evaluate(()=>new Promise(resolve=>{const q=indexedDB.open('vocr.records.v1');q.onsuccess=()=>{const db=q.result,t=db.transaction('results'),r=t.objectStore('results').count();r.onsuccess=()=>resolve(r.result);t.oncomplete=()=>db.close()}}))).toBe(3)
  await page.keyboard.press('Escape');await expect(opener(page)).toBeFocused();await other.close()
})
test('storage denial is explicit, retry succeeds and closing during a pending read does not reopen modal',async({page})=>{
  await page.addInitScript(()=>{const original=IDBFactory.prototype.open;window.recordsDenied=true;IDBFactory.prototype.open=function(name,...args){if(name==='vocr.records.v1'&&window.recordsDenied)throw new DOMException('denied','SecurityError');return original.call(this,name,...args)}})
  await page.goto('/');await opener(page).click();await expect(dialog(page).getByRole('alert')).toContainText('STORAGE_UNAVAILABLE');await expect(dialog(page).getByTestId('record-play')).toHaveCount(0)
  await page.evaluate(()=>{window.recordsDenied=false});await dialog(page).getByRole('button',{name:'戦績を再読み込み'}).click();await metrics(page,{play:'0回'})
  await page.keyboard.press('Escape')
  // Hold the store's read transaction behind a real IDB write transaction, rather than sleeping.
  await page.evaluate(()=>new Promise(resolve=>{const q=indexedDB.open('vocr.records.v1');q.onsuccess=()=>{const db=q.result,t=db.transaction('results','readwrite'),s=t.objectStore('results');window.holdRecordRead=true;const cycle=()=>{const r=s.get(1);r.onsuccess=()=>{if(window.holdRecordRead)cycle()}};cycle();t.oncomplete=()=>db.close();resolve()}}))
  await opener(page).click();await expect(dialog(page).getByRole('status')).toContainText('読み込んでいます');await page.keyboard.press('Escape');await expect(dialog(page)).toHaveCount(0)
  await page.evaluate(()=>{window.holdRecordRead=false});await open(page);await metrics(page,{play:'0回'})
})
test('display preserves five decimal HALF_UP values including 1/128 and a repeating fraction',async({page})=>{
  await page.goto('/')
  // Valid independent ledger fixture, not a precomputed aggregate injected into the component.
  await page.evaluate(async()=>{
    const{createRecordStore}=await import('/src/recordStore.js');await createRecordStore().read()
    await new Promise((resolve,reject)=>{const q=indexedDB.open('vocr.records.v1');q.onsuccess=()=>{const db=q.result,t=db.transaction('results','readwrite'),s=t.objectStore('results');
      for(let i=0;i<131;i++)s.add({id:i.toString(16).padStart(64,'0'),configuration:i<128?'jianyi':'tongchang',result:i===0||i===128?'VILLAGE_WIN':'WOLF_WIN'})
      t.oncomplete=()=>{db.close();resolve()};t.onabort=()=>reject(t.error)
    }})
  })
  await open(page);await metrics(page,{play:'131回',rate:'1.52672%',max:'1回'})
  await dialog(page).getByRole('button',{name:'簡易村',exact:true}).click();await metrics(page,{play:'128回',rate:'0.78125%',current:'0回'})
  await dialog(page).getByRole('button',{name:'通常村',exact:true}).click();await metrics(page,{play:'3回',rate:'33.33333%'})
})
