import { test,expect } from '../test-fixtures/presentation-test.js'
import { readFileSync,mkdirSync,writeFileSync } from 'node:fs'
const read=p=>readFileSync(new URL(p,import.meta.url))
const fixture=JSON.parse(read('../../src/test/resources/replay/exchange-v3.json'))
const desktop=read('../../src/test/resources/replay/desktop-saved-v3.json')
const library=page=>page.getByRole('region',{name:'ローカルReplay',exact:true})
const entries=page=>library(page).locator('.replay-entries > li')
const fileInput=page=>library(page).getByLabel('録画ファイルを選ぶ')
const preview=page=>library(page).getByRole('group',{name:'取り込みの確認（未保存）'})
async function open(page){await page.goto('/');await library(page).getByRole('button',{name:'Replay一覧',exact:true}).click()}
async function upload(page,buffer){await fileInput(page).setInputFiles({name:'recording.json',mimeType:'application/json',buffer:Buffer.from(buffer)})}
async function selectAll(page,expected){
  const checks=preview(page).getByRole('checkbox')
  await expect(checks).toHaveCount(expected)
  for(let i=0;i<expected;i++)await checks.nth(i).check()
  await expect(preview(page).getByRole('button',{name:'選択した録画の取り込みを確定',exact:true})).toBeEnabled()
}
async function submit(page){await preview(page).getByRole('checkbox').first().check();await preview(page).getByRole('button',{name:'選択した録画の取り込みを確定',exact:true}).click()}
async function imported(page,expected){
  // Existing-count/duplicate imports also need completion, not only a matching old row count.
  await expect(preview(page)).toHaveCount(0)
  await expect(entries(page)).toHaveCount(expected)
  await expect(library(page)).toContainText(/\d+局を取り込みました。重複\d+局/)
}
async function confirm(page,expected){await submit(page);await imported(page,expected)}
async function raw(page){return page.evaluate(async()=>new Promise((resolve,reject)=>{const r=indexedDB.open('vocr.replays.v1',1);r.onsuccess=()=>{const db=r.result,t=db.transaction('recordings'),q=t.objectStore('recordings').getAll();q.onsuccess=()=>{db.close();resolve(q.result)};q.onerror=()=>reject(q.error)};r.onerror=()=>reject(r.error)}))}
async function download(page,button){const event=page.waitForEvent('download');await button.click();const item=await event;return {item,bytes:readFileSync(await item.path())}}

test('desktop actual saved game imports only after confirmation, plays, exports through browser and preserves raw records; live authority is untouched',async({page,request},info)=>{
  await page.goto('/');await page.getByRole('button',{name:'対局を始める'}).click();await expect(page.getByTestId('next-event')).toBeVisible()
  const stored=await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1')),credentials=JSON.parse(stored)
  const path='/api/v1/games/'+credentials.sessionId+'?eventCursor='+credentials.eventCursor
  try{
    const before=await(await request.get(path)).json();let writes=0,replayReads=0
    page.on('request',r=>{if(r.url().includes('/api/v1/games')){if(r.method()!=='GET')writes++;if(r.url().includes('/replay?'))replayReads++}})
    await library(page).getByRole('button',{name:'Replay一覧',exact:true}).click();await upload(page,desktop)
    await expect(preview(page)).toContainText('7日');expect(await raw(page)).toEqual([])
    await confirm(page,1);await expect(entries(page)).toHaveCount(1)
    const saved=(await raw(page))[0].replay;expect(saved.rawRecords).toEqual(JSON.parse(desktop)[0].rawRecords);expect(saved.daySnapshots).toEqual(JSON.parse(desktop)[0].daySnapshots)
    await entries(page).getByRole('button',{name:'再生する',exact:true}).click();const dialog=page.getByRole('dialog',{name:/Replay再生/})
    await expect(dialog.locator('.replay-player-card')).toHaveCount(12)
    await dialog.getByRole('combobox',{name:'再生日',exact:true}).selectOption('7');await expect(page.getByTestId('replay-role-groups')).toBeVisible()
    await page.keyboard.press('Escape')
    const exported=await download(page,entries(page).getByRole('button',{name:'この録画をエクスポート',exact:true}))
    const slots=JSON.parse(exported.bytes);expect(slots).toHaveLength(9);expect(slots.slice(1)).toEqual(Array(8).fill(null));expect(slots[0]).toEqual(JSON.parse(desktop)[0])
    mkdirSync(new URL('../../build/web035/',import.meta.url),{recursive:true});writeFileSync(new URL('../../build/web035/browser-export.json',import.meta.url),exported.bytes)
    await library(page).screenshot({path:info.outputPath('file-library-imported.png')})
    expect(writes).toBe(0);expect(replayReads).toBe(0);expect(await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))).toBe(stored);expect(await(await request.get(path)).json()).toEqual(before)
    await upload(page,exported.bytes);await confirm(page,1);await expect(entries(page)).toHaveCount(1);await expect(library(page)).toContainText('重複1局')
  }finally{await request.delete('/api/v1/games/'+credentials.sessionId)}
})

test('preview cancel and invalid file preserve original bytes and the good library; source-file backup is byte exact',async({page})=>{
  await open(page);await upload(page,desktop);await confirm(page,1);const before=await raw(page)
  await upload(page,JSON.stringify(fixture));await preview(page).getByRole('button',{name:'取り込みを取り消す',exact:true}).click();expect(await raw(page)).toEqual(before)
  const invalid=Buffer.from('{not JSON\n');await upload(page,invalid);await expect(library(page).getByRole('alert')).toContainText('INVALID_JSON')
  const backup=await download(page,library(page).getByRole('button',{name:'選択ファイルの原本を保存',exact:true}));expect(backup.bytes).toEqual(invalid);expect(await raw(page)).toEqual(before)
})

test('legacy versions migrate only known defaults, preserve unknown ordinary fields and do not repair ambiguous legacy sample',async({page})=>{
  await open(page)
  const legacy=structuredClone(fixture);legacy[0].version=1;delete legacy[0].peiyiVillageCount;delete legacy[0].totalVillageCount
  for(const day of legacy[0].daySnapshots)for(const p of day.players)delete p.skillTargets
  legacy[0].notes={text:'<img src=x onerror=alert(1)> ä/ü',other:[1,true]}
  await upload(page,'\uFEFF'+JSON.stringify(legacy,null,2).replaceAll('\n','\r\n'));await expect(preview(page)).toContainText('v1→v3');await confirm(page,1)
  const entry=(await raw(page))[0].replay;expect(entry.peiyiVillageCount).toBe(-1);expect(entry.daySnapshots[0].players[0].skillTargets).toEqual([]);expect(entry.rawRecords).toEqual(legacy[0].rawRecords)
  const output=await download(page,entries(page).getByRole('button',{name:'この録画をエクスポート',exact:true}));expect(JSON.parse(output.bytes)[0].notes).toEqual(legacy[0].notes)
  expect(await page.locator('img[onerror]').count()).toBe(0)
  await upload(page,read('../../src/test/resources/replay/legacy-v1.json'));await expect(library(page).getByRole('alert')).toContainText('LEGACY_COMPATIBILITY_DIAGNOSTIC');expect(await raw(page)).toHaveLength(1)
})

test('multi-slot import is atomic under capacity failure and deduplicates within a successful batch without overwriting',async({page})=>{
  await open(page)
  const duplicated=structuredClone(fixture);duplicated[8]=structuredClone(duplicated[0]);duplicated[8].slotIndex=8
  await upload(page,JSON.stringify(duplicated));await selectAll(page,2);await preview(page).getByRole('button',{name:'選択した録画の取り込みを確定',exact:true}).click();await expect(entries(page)).toHaveCount(1);await expect(library(page)).toContainText('重複1局')
  await entries(page).getByRole('button',{name:'削除',exact:true}).click();await entries(page).getByRole('button',{name:'削除を確定',exact:true}).click();await expect(entries(page)).toHaveCount(0)
  const full=Array.from({length:9},(_,i)=>{const r=structuredClone(fixture[0]);r.slotIndex=i;r.rawRecords=[`unique:${i}`,...r.rawRecords];r.totalEvents=r.rawRecords.length;return r})
  await upload(page,JSON.stringify(full));await selectAll(page,9);await preview(page).getByRole('button',{name:'選択した録画の取り込みを確定',exact:true}).click();await expect(entries(page)).toHaveCount(9)
  const before=await raw(page),more=structuredClone(fixture);more[0].rawRecords.unshift('not-in-library');more[0].totalEvents++
  more[1]=structuredClone(full[0]);more[1].slotIndex=1
  await upload(page,JSON.stringify(more));await selectAll(page,2);await preview(page).getByRole('button',{name:'選択した録画の取り込みを確定',exact:true}).click()
  await expect(library(page).getByRole('alert')).toContainText('LIBRARY_FULL');expect(await raw(page)).toEqual(before)
})

test('duplicate decoded keys, unsupported/missing versions, invalid UTF-8, path keys and over-8MiB files never modify storage',async({page})=>{
  await open(page);await upload(page,desktop);await confirm(page,1);const before=await raw(page)
  const text=JSON.stringify(fixture)
  const cases=[Buffer.from(text.replace('"version":3','"version":3,"ver\\u0073ion":3')),Buffer.from(text.replace('"version":3','"version":4')),Buffer.from(text.replace('"version":3,','')),Buffer.of(255),Buffer.from(text.replace('"version":3','"version":3,"path":"../private"')),Buffer.alloc(8*1024*1024+1,32)]
  for(const bytes of cases){await upload(page,bytes);await expect(library(page).getByRole('alert')).toBeVisible();expect(await raw(page)).toEqual(before);await expect(preview(page)).toHaveCount(0)}
})

test('unfinished historical file remains read-only material and cannot be imported or resume a game',async({page})=>{
  const unfinished=structuredClone(fixture);unfinished[0].endResult=0;unfinished[0].rawRecords=unfinished[0].rawRecords.filter(r=>!r.startsWith('END|'));unfinished[0].totalEvents=unfinished[0].rawRecords.length
  await open(page);await upload(page,JSON.stringify(unfinished));await expect(preview(page)).toContainText('未完（概要のみ・取り込み不可）');await expect(preview(page).getByRole('checkbox')).toBeDisabled();await expect(preview(page).getByRole('button',{name:'選択した録画の取り込みを確定',exact:true})).toBeDisabled();expect(await raw(page)).toEqual([])
  unfinished[0].rawRecords.push('END|result=UNFINISHED|day=0');unfinished[0].totalEvents++
  await upload(page,JSON.stringify(unfinished));await expect(preview(page).getByRole('checkbox')).toBeDisabled();expect(await raw(page)).toEqual([])
})

test('storage quota aborts the whole selected batch and a corrupt library can be backed up without validation, deletion or repair',async({page})=>{
  await open(page)
  await page.evaluate(()=>{const original=IDBObjectStore.prototype.add;globalThis.restoreAdd=()=>{IDBObjectStore.prototype.add=original};IDBObjectStore.prototype.add=function(value){if(this.name==='recordings')throw new DOMException('controlled quota','QuotaExceededError');return original.apply(this,arguments)}})
  await upload(page,desktop);await submit(page);await expect(library(page).getByRole('alert')).toContainText('QUOTA_EXCEEDED');expect(await raw(page)).toEqual([])
  await page.evaluate(async()=>{globalThis.restoreAdd();await new Promise((resolve,reject)=>{const r=indexedDB.open('vocr.replays.v1',1);r.onsuccess=()=>{const db=r.result,t=db.transaction('recordings','readwrite');t.objectStore('recordings').put({id:'corrupt',replay:{kept:'DO_NOT_DELETE'}});t.oncomplete=()=>{db.close();resolve()};t.onabort=()=>reject(t.error)}})})
  const before=await raw(page);await library(page).getByRole('button',{name:'一覧を再読込',exact:true}).click();await expect(library(page).getByRole('alert')).toContainText('INVALID_LIBRARY')
  const backup=await download(page,library(page).getByRole('button',{name:'ローカル庫の修復資料を保存',exact:true}));expect(JSON.parse(backup.bytes)).toEqual(before);expect(await raw(page)).toEqual(before)
  await upload(page,backup.bytes);await expect(library(page).getByRole('alert')).toBeVisible();expect(await raw(page)).toEqual(before)
})


test('timed-out file read never publishes late preview or saves automatically; a new explicit selection works',async({page})=>{
  await open(page)
  // Clock is already installed by the local-presentation fixture.
  await page.evaluate(()=>{const original=File.prototype.arrayBuffer;globalThis.restoreFileRead=()=>{File.prototype.arrayBuffer=original};File.prototype.arrayBuffer=function(){return new Promise(resolve=>{globalThis.finishOldFile=()=>resolve(new ArrayBuffer(0))})}})
  await upload(page,desktop);await page.clock.fastForward(15001)
  await expect(library(page).getByRole('alert')).toContainText('FILE_TIMEOUT');await expect(preview(page)).toHaveCount(0);expect(await raw(page)).toEqual([])
  await page.evaluate(()=>globalThis.restoreFileRead());await upload(page,desktop);await expect(preview(page)).toContainText('7日')
  await page.evaluate(()=>globalThis.finishOldFile());await expect(preview(page)).toContainText('7日');expect(await raw(page)).toEqual([])
})


test('import completion waits for a controlled pending digest before reading committed storage',async({page})=>{
  await open(page)
  await page.evaluate(()=>{
    const original=SubtleCrypto.prototype.digest
    SubtleCrypto.prototype.digest=function(...args){
      SubtleCrypto.prototype.digest=original
      return new Promise(resolve=>{globalThis.releaseImportDigest=()=>resolve(original.apply(this,args))})
    }
  })
  await upload(page,desktop);await submit(page)
  await page.waitForFunction(()=>typeof globalThis.releaseImportDigest==='function',null,{timeout:5000})
  await expect(preview(page)).toBeVisible()
  await expect(preview(page).getByRole('button',{name:'選択した録画の取り込みを確定',exact:true})).toBeDisabled()
  expect(await raw(page)).toEqual([])
  await page.evaluate(()=>globalThis.releaseImportDigest())
  await imported(page,1)
  expect((await raw(page))[0].replay).toEqual(JSON.parse(desktop)[0])
})


test('multi-slot selection waits for controlled file parsing before counting and checking candidates',async({page})=>{
  await open(page)
  await page.evaluate(()=>{
    const original=File.prototype.arrayBuffer
    File.prototype.arrayBuffer=function(...args){
      File.prototype.arrayBuffer=original
      return new Promise(resolve=>{globalThis.releaseCandidateRead=()=>resolve(original.apply(this,args))})
    }
  })
  const duplicated=structuredClone(fixture);duplicated[8]=structuredClone(duplicated[0]);duplicated[8].slotIndex=8
  await upload(page,JSON.stringify(duplicated))
  await page.waitForFunction(()=>typeof globalThis.releaseCandidateRead==='function',null,{timeout:5000})
  await expect(preview(page)).toHaveCount(0)
  const selecting=selectAll(page,2)
  await page.evaluate(()=>globalThis.releaseCandidateRead())
  await selecting
  const checks=preview(page).getByRole('checkbox')
  await expect(checks).toHaveCount(2)
  await expect(checks.nth(0)).toBeChecked();await expect(checks.nth(1)).toBeChecked()
  await preview(page).getByRole('button',{name:'選択した録画の取り込みを確定',exact:true}).click()
  await imported(page,1);await expect(library(page)).toContainText('重複1局')
})
