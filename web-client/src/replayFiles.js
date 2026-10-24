import { ReplayError, REPLAY_LIMITS, validateReplay } from './replayValidation.js'
import { parseReplayJson } from './replayJson.js'
const bytes=text=>new TextEncoder().encode(text)
const bad=code=>{throw new ReplayError(code)}
export function parseReplayFile(input) {
  if(!(input instanceof Uint8Array) || input.length>REPLAY_LIMITS.fileBytes)bad('REPLAY_TOO_LARGE')
  let text
  try{text=new TextDecoder('utf-8',{fatal:true,ignoreBOM:true}).decode(input)}catch{bad('INVALID_UTF8')}
  if(text.startsWith('\uFEFF'))text=text.slice(1)
  const slots=parseReplayJson(text)
  if(!Array.isArray(slots) || slots.length!==9 || slots.every(s=>s===null))bad('INVALID_REPLAY')
  return slots.flatMap((source,fileSlot)=>{
    if(source===null)return []
    if(!source || typeof source!=='object' || !Number.isInteger(source.version) || source.version<0 || source.version>3)bad('UNSUPPORTED_VERSION')
    if(source.slotIndex!==fileSlot)bad('INVALID_REPLAY')
    const replay=structuredClone(source),version=replay.version
    replay.version=3;replay.slotIndex=0
    for(const key of ['peiyiVillageCount','totalVillageCount']){
      if(version<=1){if(replay[key]===undefined || (Number.isSafeInteger(replay[key]) && replay[key]<=0))replay[key]=-1}
      else if(replay[key]===undefined)replay[key]=0
    }
    if(Array.isArray(replay.daySnapshots))for(const day of replay.daySnapshots){
      if(!day || typeof day!=='object')bad('INVALID_REPLAY')
      if(day.dailyVotingRule===undefined)day.dailyVotingRule=-1
      for(const key of ['voteRounds','greyTargetCharNums','designatedTargetCharNums'])if(day[key]===undefined)day[key]=[]
      if(Array.isArray(day.players))for(const p of day.players){if(p && p.skillTargets===undefined)p.skillTargets=[]}
    }
    try{validateReplay(replay,{extensions:true,unfinished:true})}catch(error){if(version<3 && error.code==='INVALID_REPLAY')bad('LEGACY_COMPATIBILITY_DIAGNOSTIC');throw error}
    return [{fileSlot,sourceVersion:version,replay,readOnly:replay.endResult===0}]
  })
}
export function exportReplayFile(replays) {
  if(!Array.isArray(replays) || !replays.length || replays.length>9)bad('INVALID_REPLAY')
  const slots=Array(9).fill(null)
  replays.forEach((replay,index)=>{validateReplay(replay,{extensions:true,unfinished:true});slots[index]=structuredClone(replay);slots[index].slotIndex=index})
  const result=bytes(JSON.stringify(slots))
  if(result.length>REPLAY_LIMITS.fileBytes)bad('REPLAY_TOO_LARGE')
  // Reuse the untrusted entry so exports obey the same public envelope.
  parseReplayFile(result)
  return result
}
export async function readReplayFile(file) {
  if(!file || file.size>REPLAY_LIMITS.fileBytes)bad('REPLAY_TOO_LARGE')
  let timer
  const data=await Promise.race([file.arrayBuffer(),new Promise((_,reject)=>{timer=setTimeout(()=>reject(new ReplayError('FILE_TIMEOUT')),15000)})]).finally(()=>clearTimeout(timer))
  return parseReplayFile(new Uint8Array(data))
}
export function downloadReplayBytes(data,filename='vocr-replay-'+new Date().toISOString().replace(/[-:]/g,'').slice(0,15)+'.json') {
  const url=URL.createObjectURL(new Blob([data],{type:'application/json;charset=utf-8'})),anchor=document.createElement('a')
  anchor.href=url;anchor.download=filename;document.body.append(anchor);anchor.click();anchor.remove()
  setTimeout(()=>URL.revokeObjectURL(url),1000)
}
