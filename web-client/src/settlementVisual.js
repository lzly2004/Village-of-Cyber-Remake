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
