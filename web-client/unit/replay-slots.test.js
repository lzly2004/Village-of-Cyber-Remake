import { test } from 'node:test'
import assert from 'node:assert/strict'
import { replaySlotLayout } from '../src/replaySlots.js'
const rows=[{id:'b',savedAt:'2026-02'},{id:'a',savedAt:'2026-01'}]
test('legacy library assigns oldest first to nine slots without changing any entry',()=>{
 const before=JSON.stringify(rows),map=replaySlotLayout(rows)
 assert.deepEqual(map.ids,['a','b',null,null,null,null,null,null,null]);assert.equal(JSON.stringify(rows),before)
})
test('slot map preserves holes and stable positions across reload; callers receive a copy',()=>{
 const stored={key:'layout',version:1,ids:[null,'b',null,'a',null,null,null,null,null]}
 const value=replaySlotLayout(rows,stored);assert.deepEqual(value,stored);value.ids[1]=null;assert.equal(stored.ids[1],'b')
})
test('corrupt slot maps fail closed instead of recreating or losing local entries',()=>{
 const base=replaySlotLayout(rows)
 for(const mutate of [m=>m.ids.pop(),m=>m.ids[0]=null,m=>m.ids[0]='unknown',m=>m.version=2,m=>m.extra=true,m=>m.ids[0]={id:'a'}]){
  const copy=structuredClone(base);mutate(copy);assert.throws(()=>replaySlotLayout(rows,copy),e=>e.code==='INVALID_LIBRARY')
 }
})

test('different fixed slots may reference the same recording without copying content',()=>{
 const map=replaySlotLayout(rows);map.ids[8]='a';assert.deepEqual(replaySlotLayout(rows,map),map)
})
