// Locked to GameSceneHandler, UiImage, UIConstants and GameStrings by source tests.
export const nightDayImages = {night:'images/komorebi000night01.png',dawn:'images/komorebi002.png'}
// Web-only bound on a hung image; separate from source-locked display durations.
export const nightDayImageWaitMs = 15000
export const nightDayTimes = {night:7000,dawn:2500,character:50}
export const dayStartFormat = '%d日目になりました。'
const same = (a,b) => a?.stage === b?.stage && ['eventname','ch1','ch2','ch3'].every(k=>a?.event?.[k]===b?.event?.[k])

// Only a current public boundary, never a future outcome or hidden player state.
export function nightDayPlan(history,cursor,current,day) {
  const night = current?.stage === 'NIGHT_RESULT' && ['wsw','yjsw'].includes(current.event?.eventname)
  const end = current?.stage === 'SETTLEMENT'
  if (!night && !end) return null
  if (!Array.isArray(history) || !Number.isSafeInteger(cursor) || cursor < 0 || !same(history[cursor],current) || (cursor > 0 && !history[cursor-1])) {
    return {diagnostic:'NIGHT_CONTEXT_UNAVAILABLE'}
  }
  if (history[cursor-1]?.stage === 'NIGHT_RESULT') return null
  if (end) return history[cursor-1]?.stage === 'EXECUTION' ? {key:cursor,nightOnly:true,dayText:''} : null
  if (!Number.isSafeInteger(day) || day < 1) return {diagnostic:'DAY_NUMBER_UNAVAILABLE'}
  return {key:cursor,nightOnly:false,dayText:dayStartFormat.replace('%d',String(day))}
}
