// Origin-local history only. Never reads a Replay, private role or server-side record.
export const RECORD_DB = 'vocr.records.v1'
const STORE = 'results'
export const recordConfigurations = ['jianyi','tongchang','yaoohu','kuangxin','beide','maoyou','daxing']
const results = ['VILLAGE_WIN','WOLF_WIN','FOX_WIN']
export class RecordError extends Error {
  constructor(code) { super(code); this.code = code }
}
const fail = code => { throw new RecordError(code) }
function validResult(value) {
  if (!value || !recordConfigurations.includes(value.configuration) || !results.includes(value.result)) fail('INVALID_RESULT')
}
export function completedRecord(sessionId, update, eligible) {
  if (!eligible) return null
  if (typeof sessionId !== 'string' || !/^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i.test(sessionId)
      || update?.phase !== 'FINISHED') fail('INVALID_RESULT')
  const value = { configuration:update.view?.configuration, result:update.view?.result }
  validResult(value)
  return { sessionId:sessionId.toLowerCase(), ...value }
}
export function emptyRecord() {
  const value = { version:2, totalPlayCnt:0, totalWinRate:0, totalVillageWin:0, totalWolfWin:0, totalFoxWin:0, totalMaxStreak:0 }
  for (const key of ['playcnt','winrate','villageWincnt','wolfWincnt','foxWincnt','maxStreak','currentStreak','wincnt']) value[key] = Array(8).fill(0)
  return value
}
function rate(wins,total) {
  // Exact positive-integer HALF_UP at five decimal places, matching GameRecord.
  const n = BigInt(wins)*10000000n, d = BigInt(total)
  return Number((2n*n+d)/(2n*d))/100000
}
export function accumulate(record, value) {
  validResult(value)
  const i = recordConfigurations.indexOf(value.configuration)+1
  record.playcnt[i]++; record.totalPlayCnt++
  const win = value.result === 'VILLAGE_WIN'
  record.currentStreak[i] = win ? record.currentStreak[i]+1 : 0
  record.maxStreak[i] = Math.max(record.maxStreak[i],record.currentStreak[i])
  record.totalMaxStreak = Math.max(...record.maxStreak)
  const field = {VILLAGE_WIN:'villageWincnt',WOLF_WIN:'wolfWincnt',FOX_WIN:'foxWincnt'}[value.result]
  record[field][i]++
  record.totalVillageWin += Number(win)
  record.totalWolfWin += Number(value.result === 'WOLF_WIN')
  record.totalFoxWin += Number(value.result === 'FOX_WIN')
  record.winrate[i] = rate(record.villageWincnt[i],record.playcnt[i])
  record.totalWinRate = rate(record.totalVillageWin,record.totalPlayCnt)
  // Desktop wincnt remains zero; do not silently reinterpret that legacy field.
  return record
}
function classify(error) {
  return error instanceof RecordError ? error : new RecordError(error?.name === 'QuotaExceededError' ? 'QUOTA_EXCEEDED' : 'STORAGE_UNAVAILABLE')
}
function entryValid(entry) {
  if (!entry || Object.keys(entry).sort().join(',') !== 'configuration,id,result,sequence'
      || !Number.isSafeInteger(entry.sequence) || entry.sequence < 1 || !/^[0-9a-f]{64}$/.test(entry.id)) fail('INVALID_HISTORY')
  try { validResult(entry) } catch { fail('INVALID_HISTORY') }
}
export function createRecordStore(indexedDB) {
  async function open() {
    try {
      const factory = indexedDB === undefined ? globalThis.indexedDB : indexedDB
      if (!factory) fail('STORAGE_UNAVAILABLE')
      return await new Promise((resolve,reject) => {
        let done = false
        const timer = setTimeout(() => rejectOnce(new RecordError('STORAGE_TIMEOUT')),15000)
        function rejectOnce(error) { if (!done) { done=true; clearTimeout(timer); reject(classify(error)) } }
        let request
        try { request = factory.open(RECORD_DB,1) } catch(error) { rejectOnce(error); return }
        request.onerror = () => rejectOnce(request.error)
        request.onblocked = () => rejectOnce(new RecordError('STORAGE_BLOCKED'))
        request.onupgradeneeded = () => {
          if (done) { request.transaction.abort(); return }
          const store = request.result.createObjectStore(STORE,{keyPath:'sequence',autoIncrement:true})
          store.createIndex('id','id',{unique:true})
        }
        request.onsuccess = () => {
          const db=request.result
          if (done) { db.close(); return }
          done=true; clearTimeout(timer); db.onversionchange=()=>db.close(); resolve(db)
        }
      })
    } catch (error) { throw classify(error) }
  }
  async function transaction(mode,operation) {
    const db=await open()
    try {
      return await new Promise((resolve,reject) => {
        let value, problem
        const tx=db.transaction(STORE,mode),store=tx.objectStore(STORE)
        const timer=setTimeout(()=>abort(new RecordError('STORAGE_TIMEOUT')),15000)
        function abort(error) { problem=error; try { tx.abort() } catch { clearTimeout(timer); reject(classify(problem)) } }
        tx.onabort=()=>{clearTimeout(timer);reject(classify(problem || tx.error))}
        tx.onerror=()=>{ /* Final abort reports the transaction failure; never resolve a request early. */ }
        tx.oncomplete=()=>{clearTimeout(timer);resolve(value)}
        try { operation(store,result=>{value=result},abort) } catch (error) { abort(error) }
      })
    } catch (error) { throw classify(error) } finally { db.close() }
  }
  async function record(completed) {
    // Capture input before awaiting hash/open; new sessions cannot replace this intent.
    const value=completedRecord(completed?.sessionId,{phase:'FINISHED',view:completed},true)
    if (!globalThis.crypto?.subtle) fail('HASH_UNAVAILABLE')
    const hash=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(value.sessionId))
    const entry={id:[...new Uint8Array(hash)].map(n=>n.toString(16).padStart(2,'0')).join(''),configuration:value.configuration,result:value.result}
    return transaction('readwrite',(store,done,abort)=>{
      const scan=store.openCursor(), ids=new Set()
      scan.onsuccess=()=>{
        try {
          const current=scan.result
          if(current) {
            entryValid(current.value)
            if(ids.has(current.value.id))fail('INVALID_HISTORY')
            ids.add(current.value.id);current.continue();return
          }
          const get=store.index('id').get(entry.id)
          get.onsuccess=()=>{
            try {
              if (get.result) {
                if (get.result.configuration!==entry.configuration || get.result.result!==entry.result) fail('RESULT_CONFLICT')
                done({duplicate:true});return
              }
              store.add(entry);done({duplicate:false})
            } catch(error) { abort(error) }
          }
        } catch (error) { abort(error) }
      }
    })
  }
  async function read() {
    return transaction('readonly',(store,done,abort)=>{
      const record=emptyRecord(), ids=new Set()
      const cursor=store.openCursor()
      cursor.onsuccess=()=>{
        try {
          const entry=cursor.result
          if (!entry) { done(record);return }
          entryValid(entry.value)
          if(ids.has(entry.value.id)) fail('INVALID_HISTORY')
          ids.add(entry.value.id);accumulate(record,entry.value);entry.continue()
        } catch(error) { abort(error) }
      }
    })
  }
  return { record, read }
}
