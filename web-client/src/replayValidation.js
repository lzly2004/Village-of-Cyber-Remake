// Fresh API recordings remain strict. File migrations are isolated in replayFiles.js.
export const REPLAY_LIMITS = Object.freeze({ fileBytes: 8 * 1024 * 1024, entries: 9, libraryBytes: 64 * 1024 * 1024 })
const configurations = ['jianyi', 'tongchang', 'yaoohu', 'kuangxin', 'beide', 'maoyou', 'daxing']
export class ReplayError extends Error {
  constructor(code) { super(code); this.code = code }
}
const fail = () => { throw new ReplayError('INVALID_REPLAY') }
const integer = (n, min, max) => { if (!Number.isSafeInteger(n) || n < min || n > max) fail() }
const schemaFields = (object, required, optional = [], extensions = false) => {
  if (!object || typeof object !== 'object' || Array.isArray(object)) fail()
  if (required.some(k => !Object.hasOwn(object, k)) || (!extensions && Object.keys(object).some(k => !required.includes(k) && !optional.includes(k)))) fail()
}
const bytes = text => new TextEncoder().encode(text).byteLength
const array = (a, min, max) => { if (!Array.isArray(a) || a.length < min || a.length > max) fail() }
const uniqueIntegers = (a, min, max) => {
  array(a, 0, max)
  for (const n of a) integer(n, min, max)
  if (new Set(a).size !== a.length) fail()
}
export function validateReplay(replay, { extensions=false, unfinished=false } = {}) {
  const fields=(object,required,optional=[])=>schemaFields(object,required,optional,extensions)
  let nodes=0
  function safe(value,depth=0,key='') {
    if(depth>32 || ++nodes>1000000 || (value!==null && !['object','string','number','boolean'].includes(typeof value))) fail()
    if(value && typeof value==='object' && !Array.isArray(value) && ![Object.prototype,null].includes(Object.getPrototypeOf(value)))fail()
    if(typeof value==='number' && (!Number.isFinite(value) || (Number.isInteger(value) && !Number.isSafeInteger(value))))fail()
    if(typeof value==='string' && key!=='rawRecords' && (bytes(value)>65536 || /^(?:[a-z][a-z0-9+.-]*:|[/\\]|\.\.[/\\])/i.test(value)))fail()
    if(Array.isArray(value))value.forEach(v=>safe(v,depth+1,key))
    else if(value && typeof value==='object') for(const k of Object.keys(value)) {
      if(/^(?:__proto__|prototype|constructor|sessionid|sessiontoken|accesstoken|authorization|password|cookie|url|href|src|path)$/i.test(k))fail()
      safe(value[k],depth+1,k)
    }
  }
  safe(replay)
  fields(replay, ['version','slotIndex','saveTime','peiyiName','peiyiOrdinal','peiyiVillageCount','totalVillageCount','totalDays','endResult','totalEvents','daySnapshots','rawRecords'], ['peiyiDisplayName','resultDisplayName'])
  if (replay.version !== 3 || replay.slotIndex !== 0) fail()
  if (typeof replay.saveTime !== 'string' || !/^\d{4}\/\d{2}\/\d{2} \d{2}:\d{2}:\d{2}$/.test(replay.saveTime)) fail()
  if (!configurations.includes(replay.peiyiName) || configurations[replay.peiyiOrdinal - 1] !== replay.peiyiName) fail()
  integer(replay.peiyiOrdinal, 1, 7)
  for (const key of ['peiyiVillageCount','totalVillageCount']) integer(replay[key], -1, Number.MAX_SAFE_INTEGER)
  integer(replay.totalDays, 1, 50); integer(replay.endResult, unfinished ? 0 : 1, 3)
  array(replay.rawRecords, 1, 20000)
  if (replay.totalEvents !== replay.rawRecords.length) fail()
  for (const line of replay.rawRecords) if (typeof line !== 'string' || bytes(line) > 65536) fail()
  const end = replay.rawRecords.filter(line => line.startsWith('END|')).at(-1)
  const result = ['VILLAGE_WIN','WOLF_WIN','FOX_WIN'][replay.endResult - 1]
  const endMatch = end?.match(/^END\|result=(VILLAGE_WIN|WOLF_WIN|FOX_WIN)\|day=([1-9][0-9]*)$/)
  let endDay=0
  if(replay.endResult===0) {
    const partial=end?.match(/^END\|result=UNFINISHED\|day=(0|[1-9][0-9]*)$/)
    if(end && !partial)fail()
    endDay=partial?Number(partial[1]):0;integer(endDay,0,replay.totalDays)
  } else {
    if(!endMatch || endMatch[1]!==result)fail()
    endDay=Number(endMatch[2]);integer(endDay,1,replay.totalDays)
  }
  array(replay.daySnapshots, 1, 50)
  let lastDay = 0, identities
  for (const day of replay.daySnapshots) {
    fields(day, ['dayNumber','aliveCount','players','voteRounds','dailyVotingRule','greyTargetCharNums','designatedTargetCharNums'])
    integer(day.dayNumber, lastDay + 1, replay.totalDays); lastDay = day.dayNumber
    array(day.players, 1, 44)
    const count = day.players.length
    const ids = new Map(), characters = new Set()
    for (const p of day.players) {
      fields(p, ['playerIndex','characterNumber','actualRole','claimedRole','claimedRoleOrder','comingOutDay','skillTarget','skillTargets','deathDay','deathReason','nonHumanMarked'])
      integer(p.playerIndex, 1, count); integer(p.characterNumber, 1, 44)
      if (ids.has(p.playerIndex) || characters.has(p.characterNumber)) fail()
      ids.set(p.playerIndex, p.characterNumber); characters.add(p.characterNumber)
      integer(p.actualRole, 1, 11); integer(p.claimedRole, 0, 11); integer(p.claimedRoleOrder, 0, 44)
      integer(p.comingOutDay, -1, 50); integer(p.deathDay, 0, 50); integer(p.deathReason, 0, 7)
      if (typeof p.nonHumanMarked !== 'boolean') fail()
      integer(p.skillTarget, 0, count * 2); array(p.skillTargets, 0, 51)
      for (const n of p.skillTargets) integer(n, 0, count * 2)
    }
    const identity = [...ids].sort((a,b) => a[0]-b[0])
    if (identities && JSON.stringify(identity) !== JSON.stringify(identities)) fail()
    identities = identity
    integer(day.aliveCount, 0, count); integer(day.dailyVotingRule, -1, 2)
    uniqueIntegers(day.greyTargetCharNums, 1, count); uniqueIntegers(day.designatedTargetCharNums, 1, 44)
    if (day.designatedTargetCharNums.some(n => !characters.has(n))) fail()
    array(day.voteRounds, 0, 100)
    let round = 0
    for (const vote of day.voteRounds) {
      fields(vote, ['round','votes']); integer(vote.round, round + 1, 100); round = vote.round
      array(vote.votes, 0, count)
      const voters = new Set()
      for (const pair of vote.votes) {
        array(pair, 2, 2); integer(pair[0], 1, count); integer(pair[1], 0, count)
        if (voters.has(pair[0])) fail(); voters.add(pair[0])
      }
    }
  }
  if (replay.totalDays !== Math.max(lastDay,endDay)) fail()
  for (const key of ['peiyiDisplayName','resultDisplayName']) if (Object.hasOwn(replay,key) && (typeof replay[key] !== 'string' || replay[key].length > 256)) fail()
  if (bytes(JSON.stringify(replay)) > REPLAY_LIMITS.fileBytes) throw new ReplayError('REPLAY_TOO_LARGE')
  return replay
}
export function replayFromFile(file) {
  array(file, 9, 9)
  if (file.slice(1).some(value => value !== null)) fail()
  return validateReplay(file[0])
}
