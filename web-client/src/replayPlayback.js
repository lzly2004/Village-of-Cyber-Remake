import { characters } from './characterDisplay.js'
import { playerVisual } from './playerVisual.js'
import { settlementPortrait } from './settlementVisual.js'

// Role/whyDie ordinals are the frozen desktop file protocol, not live HTTP strings.
export const replayRoles = ['NONE','zhan','ling','lie','gong','mao','wu','renlang','kuangren','kuangxinzhe','yaohu','beidezhe']
const deathReasons = ['NONE','chuxing','daymaozhou','dayhouzhui','beiyao','nightmaozhou','nighthouzhui','zhousha']
const byNumber = new Map(Object.entries(characters).map(([name,value]) => [value.number,name]))
export const replayBackground = 'images/PVBG.png'
export function replayDay(replay, requested = 2) {
  const first = Math.min(2,replay.totalDays)
  return Math.max(first,Math.min(Number.isSafeInteger(requested) ? requested : first,replay.totalDays))
}
// Mirrors ReplayPlayerHandler.getSnapshot, but makes its positional fallback visible.
export function replaySnapshot(replay, day) {
  const exact = replay.daySnapshots.find(s => s.dayNumber === day)
  if (exact) return { snapshot:exact, diagnostic:null }
  const fallback = replay.daySnapshots[day-1]
  return { snapshot:fallback || null, diagnostic:fallback ? 'SNAPSHOT_INDEX_FALLBACK' : 'SNAPSHOT_MISSING' }
}
export function replayFrame(replay, requested) {
  const day = replayDay(replay,requested)
  const current = replaySnapshot(replay,day)
  let avatars = replaySnapshot(replay,day+1)
  // A missing next-day snapshot is normal at the end. A positional substitute is diagnosed.
  const missingNext = !avatars.snapshot && day < replay.totalDays
  if (!avatars.snapshot) avatars = current
  const diagnostics = missingNext ? [{section:'avatars',code:'AVATAR_NEXT_DAY_MISSING',sourceDay:avatars.snapshot?.dayNumber ?? null}] : []
  for (const [section,record] of [['data',current],['avatars',avatars]]) {
    if (record.diagnostic) diagnostics.push({ section,code:record.diagnostic,sourceDay:record.snapshot?.dayNumber ?? null })
  }
  const playerCount = replay.daySnapshots[0].players.length
  const alive = current.snapshot?.aliveCount
  return {
    day, firstDay:Math.min(2,replay.totalDays), lastDay:replay.totalDays,
    dataDay:current.snapshot?.dayNumber ?? null, avatarDay:avatars.snapshot?.dayNumber ?? null,
    population:current.snapshot ? { alive,dead:playerCount-alive,ropes:alive>1 ? Math.trunc((alive-1)/2) : 0 } : null,
    diagnostics,
    players:(avatars.snapshot?.players || []).map(p => {
      const character = byNumber.get(p.characterNumber)
      const departed = p.deathDay>0 && p.deathDay<day
      const claimed = p.claimedRole>0 && p.claimedRole<6 && p.comingOutDay>0
      const visual = playerVisual({ character,alive:!departed,claimedRole:claimed ? replayRoles[p.claimedRole] : 'NONE',
        claimedRoleOrder:p.claimedRoleOrder,deathReason:deathReasons[p.deathReason] })
      return {
        playerIndex:p.playerIndex,character,actualRole:replayRoles[p.actualRole],departed,
        // Desktop Replay keeps colored role portraits even after death (unlike settlement).
        portrait:settlementPortrait({character,alive:true},replayRoles[p.actualRole]),
        claimIcon:visual.claimIcon,claimText:visual.roleText,claimOrder:visual.orderText,
        claimDiagnostic:visual.claimDiagnostic,deathIcon:visual.deathIcon,deathDiagnostic:visual.deathDiagnostic,
        characterText:visual.characterText,
      }
    }),
  }
}
