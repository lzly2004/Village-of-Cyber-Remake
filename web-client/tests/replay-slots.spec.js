import {test,expect} from '@playwright/test'
import {readFileSync} from 'node:fs'
const replay=JSON.parse(readFileSync(new URL('../../src/test/resources/replay/exchange-v3.json',import.meta.url)))[0]
async function run(page,fn,arg){return page.evaluate(async({fn,arg})=>{const m=await import('/src/replayStore.js');return Function('m','arg','return ('+fn+')(m,arg)')(m,arg)},{fn:fn.toString(),arg})}

test('v1 entries migrate unchanged to stable nine slots; holes remain after delete and reload',async({page})=>{
 await page.goto('/')
 await page.evaluate(async replay=>{
  const {replayId}=await import('/src/replayStore.js'),id=await replayId(replay)
  await new Promise((resolve,reject)=>{const q=indexedDB.open('vocr.replays.v1',1);q.onupgradeneeded=()=>q.result.createObjectStore('recordings',{keyPath:'id'});q.onsuccess=()=>{const d=q.result,t=d.transaction('recordings','readwrite');t.objectStore('recordings').add({id,replay,bytes:new TextEncoder().encode(JSON.stringify(replay)).length,savedAt:'2026-01-01T00:00:00.000Z'});t.oncomplete=()=>{d.close();resolve()};t.onabort=()=>reject(t.error)}})
 },replay)
 const before=await run(page,async m=>m.createReplayStore().list())
 const slots=await run(page,async m=>m.createReplayStore().listSlots())
 expect(slots).toHaveLength(9);expect(slots[0]).toEqual(before[0]);expect(slots.slice(1)).toEqual(Array(8).fill(null))
 const other={...replay,totalVillageCount:37}
 await run(page,async(m,r)=>m.createReplayStore().saveToSlot(r,7,null),other)
 await run(page,async(m,id)=>m.createReplayStore().remove(id),before[0].id)
 await page.reload();const final=await run(page,async m=>m.createReplayStore().listSlots())
 expect(final[0]).toBeNull();expect(final[7].replay).toEqual(other);expect(final.filter(Boolean)).toHaveLength(1)
})

test('same-slot concurrent writes require fresh confirmation; no silent overwrite',async({page})=>{
 await page.goto('/')
 const values=await run(page,async(m,r)=>{
  const s=m.createReplayStore();await s.listSlots()
  const result=await Promise.allSettled([s.saveToSlot(r,3,null),s.saveToSlot({...r,totalVillageCount:38},3,null)])
  return {states:result.map(v=>v.status),errors:result.filter(v=>v.status==='rejected').map(v=>v.reason.code),slots:await s.listSlots()}
 },replay)
 expect(values.states.sort()).toEqual(['fulfilled','rejected']);expect(values.errors).toEqual(['SLOT_CHANGED']);expect(values.slots.filter(Boolean)).toHaveLength(1)
})

test('quota-aborted replacement preserves original bytes and map atomically',async({page})=>{
 await page.goto('/')
 const values=await run(page,async(m,r)=>{
  const s=m.createReplayStore();await s.saveToSlot(r,4,null);const before=await s.listSlots(),original=IDBObjectStore.prototype.add
  let code
  try{IDBObjectStore.prototype.add=function(){if(this.name==='recordings')throw new DOMException('controlled quota','QuotaExceededError');return original.apply(this,arguments)};await s.saveToSlot({...r,totalVillageCount:39},4,before[4].id)}catch(e){code=e.code}finally{IDBObjectStore.prototype.add=original}
  return {before,after:await s.listSlots(),code}
 },replay)
 expect(values.code).toBe('QUOTA_EXCEEDED');expect(values.after).toEqual(values.before)
})

test('corrupt map fails closed and does not rebuild or overwrite existing recordings',async({page})=>{
 await page.goto('/')
 const result=await run(page,async(m,r)=>{
  const s=m.createReplayStore();await s.save(r);const before=await s.list()
  await new Promise((resolve,reject)=>{const q=indexedDB.open('vocr.replays.v1');q.onsuccess=()=>{const d=q.result,t=d.transaction('slot-layout','readwrite');t.objectStore('slot-layout').put({key:'layout',version:1,ids:Array(9).fill('bad')});t.oncomplete=()=>{d.close();resolve()};t.onabort=()=>reject(t.error)}})
  const codes=[];for(const action of [()=>s.listSlots(),()=>s.save({...r,totalVillageCount:40})])try{await action()}catch(e){codes.push(e.code)}
  return {codes,before,after:await s.list()}
 },replay)
 expect(result.codes).toEqual(['INVALID_LIBRARY','INVALID_LIBRARY']);expect(result.after).toEqual(result.before)
})


test('explicitly saving the same game to another slot preserves both slots; replacing one keeps the other unchanged',async({page})=>{
 await page.goto('/')
 const result=await run(page,async(m,r)=>{
  const s=m.createReplayStore();await s.saveToSlot(r,1,null);const before=await s.listSlots()
  const saved=await s.saveToSlot(r,8,null)
  await s.saveToSlot({...r,totalVillageCount:93},1,before[1].id)
  return {saved,before,after:await s.listSlots(),entries:await s.list()}
 },replay)
 expect(result.saved).toMatchObject({duplicate:false,reused:true,slot:8})
 expect(result.after[8]).toEqual(result.before[1]);expect(result.after[1].replay.totalVillageCount).toBe(93);expect(result.entries).toHaveLength(2)
})
