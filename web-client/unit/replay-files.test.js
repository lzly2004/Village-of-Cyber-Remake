import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parseReplayFile, exportReplayFile } from '../src/replayFiles.js'
import { validateReplay } from '../src/replayValidation.js'
const read=p=>readFileSync(new URL(p,import.meta.url))
const base=JSON.parse(read('../../src/test/resources/replay/exchange-v3.json'))
const cases=JSON.parse(read('../../src/test/resources/replay/web035-file-cases.json'))
const encode=value=>new TextEncoder().encode(typeof value==='string'?value:JSON.stringify(value))
for(const c of cases)test('file boundary shared case: '+c.name,()=>{
  const slots=structuredClone(base)
  for(const patch of c.patches){
    const path=patch.path.split('/').slice(1),key=path.pop()
    let parent=slots;for(const p of path)parent=parent[p]
    if(patch.remove){if(Array.isArray(parent))parent.splice(Number(key),1);else delete parent[key]}
    else Object.defineProperty(parent,key,{value:structuredClone(patch.value),writable:true,enumerable:true,configurable:true})
  }
  const bytes=encode(slots),original=bytes.slice()
  if(c.ok){
    const [item]=parseReplayFile(bytes)
    assert.equal(item.replay.version,3);assert.equal(item.replay.slotIndex,0)
    assert.deepEqual(item.replay.rawRecords,slots[0].rawRecords)
    assert.deepEqual(parseReplayFile(exportReplayFile([item.replay]))[0].replay,item.replay)
    if(c.name.startsWith('unfinished-')){assert.equal(item.readOnly,true);assert.throws(()=>validateReplay(item.replay),/INVALID_REPLAY/)}
    if(c.name.startsWith('version-')){const version=slots[0].version;assert.equal(item.replay.peiyiVillageCount,version<=1?-1:0);assert.deepEqual(item.replay.daySnapshots[0].players[0].skillTargets,[])}
    if(c.name==='ordinary-unknown-fields'){assert.deepEqual(item.replay.notes,slots[0].notes);assert.throws(()=>validateReplay(item.replay),/INVALID_REPLAY/)}
  }else assert.throws(()=>parseReplayFile(bytes))
  assert.deepEqual(bytes,original)
})
test('UTF-8/BOM/CRLF, duplicate decoded keys, trailing data and unsafe numeric tokens fail closed',()=>{
  const text=JSON.stringify(base)
  assert.deepEqual(parseReplayFile(encode('\uFEFF'+JSON.stringify(base,null,2).replaceAll('\n','\r\n')))[0].replay,base[0])
  for(const data of [Uint8Array.of(255),encode('\uFEFF\uFEFF'+text),encode(text+'{}'),encode(text.replace('"version":3','"version":3,"notes":0.1000000000000000001')),encode(text.replace('"version":3','"version":3,"ver\\u0073ion":3')),encode(text.replace('"totalDays":4','"totalDays":1.000000000000000001')),encode(text.replace('"totalDays":4','"totalDays":9007199254740993'))])assert.throws(()=>parseReplayFile(data))
})
test('file byte limit includes whitespace, and multiple populated slots preserve source positions but normalize local slot 0',()=>{
  const text=JSON.stringify(base),boundary=new Uint8Array(8*1024*1024).fill(32);boundary.set(encode(text))
  assert.equal(parseReplayFile(boundary).length,1)
  assert.throws(()=>parseReplayFile(new Uint8Array(boundary.length+1)),/REPLAY_TOO_LARGE/)
  const slots=structuredClone(base);slots[8]=structuredClone(slots[0]);slots[8].slotIndex=8
  assert.deepEqual(parseReplayFile(encode(slots)).map(c=>[c.fileSlot,c.replay.slotIndex]),[[0,0],[8,0]])
  assert.deepEqual(parseReplayFile(exportReplayFile([slots[0],{...slots[8],slotIndex:0}])).map(c=>c.fileSlot),[0,1])
})
test('the untouched legacy-v1 sample is a compatibility diagnostic, not silently repaired by guessing grey indices',()=>{
  assert.throws(()=>parseReplayFile(new Uint8Array(read('../../src/test/resources/replay/legacy-v1.json'))),/LEGACY_COMPATIBILITY_DIAGNOSTIC/)
})
