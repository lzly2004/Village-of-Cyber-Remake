import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { validateReplay, replayFromFile, REPLAY_LIMITS } from '../src/replayValidation.js'
import { replayId, createReplayStore } from '../src/replayStore.js'
import { readCompletedReplay } from '../src/replayApi.js'
const fixture=JSON.parse(await readFile(new URL('../../src/test/resources/replay/exchange-v3.json',import.meta.url),'utf8'))
const copy=()=>structuredClone(fixture[0])
test('v3 preserves raw Unicode, multiline JSON, opaque large seed, order and both index kinds',async()=>{
  const replay=copy(); assert.equal(validateReplay(replay),replay)
  assert.deepEqual(replayFromFile(structuredClone(fixture)),replay)
  const before=JSON.stringify(replay)
  assert.match(await replayId(replay),/^[0-9a-f]{64}$/)
  assert.equal(JSON.stringify(replay),before)
  const again=copy(); again.saveTime='2026/02/03 04:05:06';again.peiyiDisplayName='ignored display cache'
  assert.equal(await replayId(again),await replayId(replay))
  again.rawRecords.reverse(); assert.notEqual(await replayId(again).catch(()=>null),await replayId(replay))
})
for(const [name,edit] of [
  ['future version',r=>r.version=4],['unfinished',r=>r.endResult=0],['slot',r=>r.slotIndex=1],
  ['credential',r=>r.sessionId='DO_NOT_STORE'],['prototype',r=>Object.defineProperty(r,'__proto__',{value:{},enumerable:true})],
  ['unsafe integer',r=>r.totalDays=2**53],['wrong config',r=>r.peiyiOrdinal=2],['record count',r=>r.totalEvents++],
  ['bad END',r=>r.rawRecords[r.rawRecords.length-1]='END|result=UNFINISHED|day=0'],
  ['duplicate day',r=>r.daySnapshots[1].dayNumber=r.daySnapshots[0].dayNumber],
  ['duplicate player',r=>r.daySnapshots[0].players[1].playerIndex=1],
  ['unknown role',r=>r.daySnapshots[0].players[0].actualRole=12],
  ['unknown death',r=>r.daySnapshots[0].players[0].deathReason=8],
  ['invalid target',r=>r.daySnapshots[0].players[0].skillTarget=99],
  ['identity drift',r=>r.daySnapshots[1].players[0].characterNumber=43],
  ['bad raw type',r=>r.rawRecords[0]={}],['oversize line',r=>r.rawRecords[0]='あ'.repeat(22000)],
  ['too many lines',r=>{r.rawRecords=Array(20001).fill('x');r.totalEvents=r.rawRecords.length}],
  ['missing snapshots',r=>r.daySnapshots=[]],['oversize history',r=>r.daySnapshots[0].players[0].skillTargets=Array(52).fill(0)],
  ['missing field',r=>delete r.daySnapshots[0].players[0].actualRole],
]) test('reject '+name+' without mutating the input',()=>{
  const replay=copy();edit(replay);const before=JSON.stringify(replay)
  assert.throws(()=>validateReplay(replay),/INVALID_REPLAY/)
  assert.equal(JSON.stringify(replay),before)
})
test('multi-game or malformed file is not accepted by the fresh recording entry',()=>{
  assert.throws(()=>replayFromFile({replays:fixture}),/INVALID_REPLAY/)
  const f=structuredClone(fixture);f[1]=copy();assert.throws(()=>replayFromFile(f),/INVALID_REPLAY/)
  assert.equal(REPLAY_LIMITS.entries,9)
})
test('unavailable storage never silently falls back to localStorage',async()=>{
  await assert.rejects(createReplayStore(null).list(),/STORAGE_UNAVAILABLE/)
})
test('read validates content type, bounded streaming, status, UTF-8 and schema',async()=>{
  const original=globalThis.fetch
  const id='12345678-1234-1234-1234-123456789012'
  try {
    const attempts=[
      [()=>new Response('html',{headers:{'content-type':'text/html'}}),'INVALID_REPLAY'],
      [()=>new Response('{}',{status:409}),'Replayを取得'],
      [()=>new Response('[]',{headers:{'content-type':'application/json'}}),'INVALID_REPLAY'],
      [()=>new Response(Uint8Array.from([0xff]),{headers:{'content-type':'application/json'}}),'INVALID_REPLAY'],
      [()=>new Response(new ReadableStream({start(c){c.enqueue(new Uint8Array(REPLAY_LIMITS.fileBytes+1));c.close()}}),{headers:{'content-type':'application/json'}}),'REPLAY_TOO_LARGE'],
    ]
    for(const [response,message] of attempts){globalThis.fetch=async()=>response();await assert.rejects(readCompletedReplay(id,3),new RegExp(message))}
    globalThis.fetch=async(url,options)=>{
      assert.equal(url,`/api/v1/games/${id}/replay?eventCursor=3`);assert.equal(options.cache,'no-store')
      return new Response(JSON.stringify(fixture),{headers:{'content-type':'application/json; charset=utf-8'}})
    }
    assert.deepEqual(await readCompletedReplay(id,3),fixture[0])
    await assert.rejects(readCompletedReplay('../private',3),/INVALID_REQUEST/)
  }finally{globalThis.fetch=original}
})

test('whole recording above 8MiB is rejected, never truncated',()=>{
  const replay=copy()
  replay.rawRecords.unshift(...Array(129).fill('x'.repeat(65536)))
  replay.totalEvents=replay.rawRecords.length
  assert.throws(()=>validateReplay(replay),/REPLAY_TOO_LARGE/)
  assert.equal(replay.rawRecords.length,fixture[0].rawRecords.length+129)
})

test('denied IndexedDB capability getter is handled lazily, never throws during component setup',async()=>{
  const original=Object.getOwnPropertyDescriptor(globalThis,'indexedDB')
  try {
    Object.defineProperty(globalThis,'indexedDB',{configurable:true,get(){throw new DOMException('blocked','SecurityError')}})
    const store=createReplayStore()
    await assert.rejects(store.list(),/STORAGE_UNAVAILABLE/)
  }finally{if(original)Object.defineProperty(globalThis,'indexedDB',original);else delete globalThis.indexedDB}
})
