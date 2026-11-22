import { ReplayError } from './replayValidation.js'

// A local display map, never inserted into the exported desktop Replay object.
export function replaySlotLayout(entries, stored) {
  const ids=new Set(entries.map(e=>e.id))
  if(entries.length>9 || ids.size!==entries.length)throw new ReplayError('INVALID_LIBRARY')
  if(stored===undefined) {
    const old=[...entries].sort((a,b)=>a.savedAt.localeCompare(b.savedAt)||a.id.localeCompare(b.id))
    return {key:'layout',version:1,ids:Array.from({length:9},(_,i)=>old[i]?.id ?? null)}
  }
  if(!stored || Object.keys(stored).sort().join(',')!=='ids,key,version' || stored.key!=='layout'
    || stored.version!==1 || !Array.isArray(stored.ids) || stored.ids.length!==9)throw new ReplayError('INVALID_LIBRARY')
  const occupied=stored.ids.filter(id=>id!==null)
  if(occupied.some(id=>!ids.has(id)) || new Set(occupied).size!==ids.size)
    throw new ReplayError('INVALID_LIBRARY')
  return {key:'layout',version:1,ids:[...stored.ids]}
}
