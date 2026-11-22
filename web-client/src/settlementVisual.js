import { characters } from './characterDisplay.js'
import { roles } from './labels.js'

const knownRole = role => typeof role === 'string' && role !== 'NONE' && Object.hasOwn(roles, role)
// Fail closed for malformed/duplicate/missing identities. Never inspect entries before the existing reveal gate.
export function settlementReveal(finished, players, entries) {
  const result = { roles: new Map(), diagnostic: null }
  if (!finished) return result
  if (!Array.isArray(entries) || !Array.isArray(players) || entries.length !== players.length || !players.length) {
    result.diagnostic = 'INVALID_REVEAL'; return result
  }
  const numbers = new Set(players.map(player => player.playerNumber))
  if (numbers.size !== players.length || [...numbers].some(number => !Number.isSafeInteger(number) || number < 1)) {
    result.diagnostic = 'INVALID_PLAYERS'; return result
  }
  for (const entry of entries) {
    if (!entry || !numbers.has(entry.playerNumber) || result.roles.has(entry.playerNumber) || !knownRole(entry.actualRole)) {
      return { roles: new Map(), diagnostic: 'INVALID_REVEAL' }
    }
    result.roles.set(entry.playerNumber, entry.actualRole)
  }
  return result
}
// Caller supplies a role only after the gate. Death uses gray gs even when a valid role is known.
export function settlementPortrait(player, role) {
  if (!knownRole(role)) return null
  const character = typeof player?.character === 'string' && Object.hasOwn(characters, player.character) ? characters[player.character] : null
  if (!character || typeof player.alive !== 'boolean') return null
  const suffix = !player.alive ? 'gs' : ({ mao: 'cs', yaohu: 'fs', beidezhe: 'hs', renlang: 'ws', kuangren: 'ks', kuangxinzhe: 'ks' }[role] || 's')
  return 'images/' + String(character.number).padStart(2,'0') + suffix + '.png'
}

const deathSuffix = { beiyao:'狼噛', chuxing:'処刑', zhousha:'呪殺',
  dayhouzhui:'後追', nighthouzhui:'後追', daymaozhou:'猫呪', nightmaozhou:'猫呪' }
// EndAnimeHandler / GameStrings: an alive losing survivor has an end-state label,
// not a fabricated death date. Terminal facts must remain exact, never inferred
// from the earlier, deliberately sanitized NIGHT_DEATH public category.
export function settlementDetails(finished, player, actualRole, result) {
  if (!finished) return null
  const knownClaim = typeof player?.claimedRole==='string' && Object.hasOwn(roles,player.claimedRole)
  const actualKnown = knownRole(actualRole)
  const value = { claim:knownClaim?(player.claimedRole==='NONE'?'無し':roles[player.claimedRole]):'公称職業不明（表示診断）',
    actual:actualKnown?roles[actualRole]:'身分不明（表示診断）', fate:'結末不明（表示診断）',
    diagnostic:!knownClaim?'INVALID_CLAIM':!actualKnown?'INVALID_REVEAL':null }
  function invalid(code, text) { value.fate=text;value.diagnostic ||= code;return value }
  if(player?.alive===false) {
    if(typeof player.deathReason!=='string'||!Object.hasOwn(deathSuffix,player.deathReason))return invalid('INVALID_DEATH_REASON','死因不明（表示診断）')
    if(!Number.isSafeInteger(player.deathDay)||player.deathDay<1)return invalid('INVALID_DEATH_DAY','死亡日不明（表示診断）')
    value.fate=player.deathDay+'日目'+deathSuffix[player.deathReason]
  } else if(player?.alive===true) {
    if(result==='VILLAGE_WIN')value.fate='最終存活'
    else if(actualKnown && (result==='WOLF_WIN'||result==='FOX_WIN')) {
      const winner=result==='WOLF_WIN'?['renlang','kuangren','kuangxinzhe'].includes(actualRole):['yaohu','beidezhe'].includes(actualRole)
      value.fate=winner?'最終胜利':'最終死亡'
    } else return invalid('INVALID_OUTCOME','結末不明（表示診断）')
  } else return invalid('INVALID_ALIVE','生死不明（表示診断）')
  return value
}
