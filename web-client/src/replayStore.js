import { ReplayError, REPLAY_LIMITS, validateReplay } from './replayValidation.js'
import { replaySlotLayout } from './replaySlots.js'
export const REPLAY_DB = 'vocr.replays.v1'
const STORE = 'recordings'
const SLOTS = 'slot-layout'
const encode = text => new TextEncoder().encode(text)
const canonical = value => Array.isArray(value) ? value.map(canonical)
  : value && typeof value === 'object' ? Object.fromEntries(Object.keys(value).sort().map(k => [k, canonical(value[k])])) : value
export async function replayId(replay) {
  const { saveTime, peiyiDisplayName, resultDisplayName, ...content } = validateReplay(replay,{extensions:true})
  if (!globalThis.crypto?.subtle) throw new ReplayError('HASH_UNAVAILABLE')
  const hash = await crypto.subtle.digest('SHA-256', encode(JSON.stringify(canonical(content))))
  return [...new Uint8Array(hash)].map(n => n.toString(16).padStart(2,'0')).join('')
}
function classify(error) {
  if (error instanceof ReplayError) return error
  return new ReplayError(error?.name === 'QuotaExceededError' ? 'QUOTA_EXCEEDED' : 'STORAGE_UNAVAILABLE')
}
function entryValid(entry) {
  if (!entry || Object.keys(entry).sort().join(',') !== 'bytes,id,replay,savedAt'
      || !/^[0-9a-f]{64}$/.test(entry.id) || typeof entry.savedAt !== 'string'
      || !/^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}Z$/.test(entry.savedAt)) throw new ReplayError('INVALID_LIBRARY')
  try { validateReplay(entry.replay,{extensions:true}) } catch { throw new ReplayError('INVALID_LIBRARY') }
  if (entry.bytes !== encode(JSON.stringify(entry.replay)).length) throw new ReplayError('INVALID_LIBRARY')
}
export function createReplayStore(indexedDB) {
  function open() {
    return new Promise((resolve,reject) => {
      let settled = false, request
      const fail = error => { if (!settled) { settled = true; clearTimeout(timer); reject(classify(error)) } }
      const timer = setTimeout(() => fail(new ReplayError('STORAGE_TIMEOUT')), 15000)
      try {
        const provider = indexedDB === undefined ? globalThis.indexedDB : indexedDB
        if (!provider) throw new ReplayError('STORAGE_UNAVAILABLE')
        request = provider.open(REPLAY_DB,2)
      } catch (error) { fail(error); return }
      request.onblocked = () => fail(new ReplayError('STORAGE_BLOCKED'))
      request.onerror = () => fail(request.error)
      request.onupgradeneeded = () => {
        if (settled) { request.transaction.abort(); return }
        if(!request.result.objectStoreNames.contains(STORE))request.result.createObjectStore(STORE,{keyPath:'id'})
        if(!request.result.objectStoreNames.contains(SLOTS))request.result.createObjectStore(SLOTS,{keyPath:'key'})
      }
      request.onsuccess = () => {
        const db = request.result
        if (settled) { db.close(); return }
        settled = true; clearTimeout(timer)
        db.onversionchange = () => db.close()
        resolve(db)
      }
    })
  }
  async function transaction(mode, operation) {
    const db = await open()
    try {
      return await new Promise((resolve,reject) => {
        let result, problem
        const tx = db.transaction([STORE,SLOTS],mode), store = tx.objectStore(STORE)
        const timer = setTimeout(() => {
          problem = new ReplayError('STORAGE_TIMEOUT')
          try { tx.abort() } catch { reject(problem) }
        },15000)
        const fail = error => { problem = error; tx.abort() }
        tx.onabort = () => { clearTimeout(timer); reject(classify(problem || tx.error)) }
        tx.onerror = () => { /* onabort supplies the final transaction result */ }
        tx.oncomplete = () => { clearTimeout(timer); resolve(result) }
        try { operation(store, value => { result = value }, fail, tx.objectStore(SLOTS)) } catch (error) { fail(error) }
      })
    } catch (error) { throw classify(error) } finally { db.close() }
  }
  async function list() {
    const entries = await transaction('readonly',(store,done) => {
      const get = store.getAll(); get.onsuccess = () => done(get.result)
    })
    if (entries.length > REPLAY_LIMITS.entries) throw new ReplayError('INVALID_LIBRARY')
    let total = 0
    for (const entry of entries) {
      entryValid(entry); total += entry.bytes
      if (entry.id !== await replayId(entry.replay)) throw new ReplayError('INVALID_LIBRARY')
    }
    if (total > REPLAY_LIMITS.libraryBytes) throw new ReplayError('INVALID_LIBRARY')
    return entries.sort((a,b) => b.savedAt.localeCompare(a.savedAt) || a.id.localeCompare(b.id))
  }
  async function saveMany(replays) {
    if(!Array.isArray(replays) || !replays.length || replays.length>9)throw new ReplayError('INVALID_REPLAY')
    const incoming=[]
    for(let replay of replays){
      replay=structuredClone(validateReplay(replay,{extensions:true}))
      const id=await replayId(replay)
      incoming.push({id,savedAt:new Date().toISOString(),bytes:encode(JSON.stringify(replay)).length,replay})
    }
    return mutateSlots((entries,layout,store)=>{
      const existing=new Set(entries.map(e=>e.id)),additions=new Map()
      for(const entry of incoming)if(!existing.has(entry.id))additions.set(entry.id,entry)
      if(entries.length+additions.size>REPLAY_LIMITS.entries || entries.reduce((n,e)=>n+e.bytes,0)+[...additions.values()].reduce((n,e)=>n+e.bytes,0)>REPLAY_LIMITS.libraryBytes)throw new ReplayError('LIBRARY_FULL')
      for(const entry of additions.values()){
        const slot=layout.ids.indexOf(null)
        if(slot<0)throw new ReplayError('LIBRARY_FULL')
        layout.ids[slot]=entry.id;store.add(entry)
      }
      return {added:additions.size,duplicates:incoming.length-additions.size,ids:incoming.map(e=>e.id)}
    })
  }
  async function mutateSlots(action) {
    for(let attempt=0;attempt<5;attempt++){
      const verified=new Map((await list()).map(e=>[e.id,JSON.stringify(e)]))
      try{return await transaction('readwrite',(store,done,fail,slots)=>{
        const get=store.getAll()
        get.onsuccess=()=>{
          const read=slots.get('layout')
          read.onsuccess=()=>{try{
              const entries=get.result
              if(entries.length!==verified.size || entries.some(e=>verified.get(e.id)!==JSON.stringify(e)))throw new ReplayError('LIBRARY_CHANGED')
              const layout=replaySlotLayout(entries,read.result)
              const result=action(entries,layout,store)
              if(JSON.stringify(layout)!==JSON.stringify(read.result))slots.put(layout)
              done(result)
            }catch(error){fail(error)}}
        }
      })}catch(error){if(error.code!=='LIBRARY_CHANGED')throw error}
    }
    throw new ReplayError('LIBRARY_CHANGED')
  }
  async function listSlots(){return mutateSlots((entries,layout)=>layout.ids.map(id=>id===null?null:entries.find(e=>e.id===id)))}
  async function saveToSlot(replay,slot,expectedId) {
    if(!Number.isInteger(slot)||slot<0||slot>8 || (expectedId!==null&&!/^[0-9a-f]{64}$/.test(expectedId)))throw new ReplayError('INVALID_SLOT')
    replay=structuredClone(validateReplay(replay,{extensions:true}))
    const id=await replayId(replay), entry={id,savedAt:new Date().toISOString(),bytes:encode(JSON.stringify(replay)).length,replay}
    return mutateSlots((entries,layout,store)=>{
      if(layout.ids[slot]!==expectedId)throw new ReplayError('SLOT_CHANGED')
      if(expectedId===id)return {duplicate:true,id,slot}
      // Multiple desktop slots may refer to the same recording; content remains deduplicated.
      const reused=entries.some(e=>e.id===id)
      const oldStillUsed=layout.ids.some((value,index)=>index!==slot&&value===expectedId)
      const remaining=entries.filter(e=>e.id!==expectedId||oldStillUsed)
      if(remaining.reduce((n,e)=>n+e.bytes,0)+(reused?0:entry.bytes)>REPLAY_LIMITS.libraryBytes)throw new ReplayError('LIBRARY_FULL')
      if(expectedId!==null&&!oldStillUsed)store.delete(expectedId)
      if(!reused)store.add(entry)
      layout.ids[slot]=id
      return {duplicate:false,reused,id,slot}
    })
  }
  async function save(replay) {
    const result=await saveMany([replay])
    return {duplicate:result.added===0,id:result.ids[0]}
  }
  async function backup() {
    // Recovery data is the existing IDB entry array, explicitly not an exchange file.
    return transaction('readonly',(store,done,fail)=>{
      const values=[];let size=0
      const cursor=store.openCursor()
      cursor.onsuccess=()=>{
        try{
          const entry=cursor.result
          if(!entry){done(values);return}
          size+=encode(JSON.stringify(entry.value)).length
          if(size>REPLAY_LIMITS.libraryBytes+1024*1024)throw new ReplayError('REPLAY_TOO_LARGE')
          values.push(entry.value);entry.continue()
        }catch(error){fail(error)}
      }
    })
  }
  async function remove(id) {
    if (!/^[0-9a-f]{64}$/.test(id)) throw new ReplayError('INVALID_LIBRARY')
    return mutateSlots((_entries,layout,store)=>{store.delete(id);layout.ids=layout.ids.map(value=>value===id?null:value);return null})
  }
  return { list, listSlots, saveToSlot, save, saveMany, remove, backup }
}
