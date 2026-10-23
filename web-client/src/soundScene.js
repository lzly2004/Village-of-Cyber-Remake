// Only current public presentation; no private roles, death subtype or future events.
export function enteringSounds(current, finished, result, firstNight) {
  if (finished) {
    const sound={VILLAGE_WIN:'VILLAGE_VICTORY',WOLF_WIN:'WOLF_VICTORY',FOX_WIN:'FOX_VICTORY'}
    return typeof result==='string' && Object.hasOwn(sound,result)?[sound[result]]:[]
  }
  if (current?.stage !== 'NIGHT_RESULT' || !['wsw','yjsw'].includes(current.event?.eventname)) return []
  return [...(firstNight?['NIGHTFALL','WOLF_HOWL']:[]),...(current.event.eventname==='wsw'?['PEACEFUL_NIGHT']:[])]
}
export function transitionSound(current) {
  if (current?.stage==='NIGHT_RESULT' && current.event?.eventname==='yjsw') return 'NIGHT_DEATH'
  if (current?.stage==='EXECUTION' && ['cxs','hzsw','mzsw'].includes(current.event?.eventname)) return 'DAY_EXECUTION'
  return null
}
