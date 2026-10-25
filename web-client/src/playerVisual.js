import { characters } from './characterDisplay.js'
import { roles } from './labels.js'

const claimIcons = { zhan: [1, 5], ling: [2, 4], lie: [3, 4] }
const deathIcons = {
  EXECUTION: 'turi', DAY_CAT_CURSE: 'noroi', DAY_PURSUED: 'atooi', NIGHT_DEATH: 'kami',
  chuxing: 'turi', daymaozhou: 'noroi', dayhouzhui: 'atooi',
  beiyao: 'kami', nightmaozhou: 'kami', nighthouzhui: 'kami', zhousha: 'kami',
}
const image = name => 'images/' + name + '.png'

// Only VisiblePlayer public fields. Never inspect actualRole, skill results or revealedRoles.
export function playerVisual(player) {
  const character = typeof player?.character === 'string' && Object.hasOwn(characters, player.character)
    ? characters[player.character] : null
  const alive = player?.alive
  const role = player?.claimedRole
  const order = player?.claimedRoleOrder
  const knownRole = typeof role === 'string' && Object.hasOwn(roles, role)
  const needsOrder = typeof role === 'string' && Object.hasOwn(claimIcons, role)
  const validOrder = Number.isSafeInteger(order) && order > 0
  let claimIcon = null
  let claimDiagnostic = knownRole ? null : 'CO_ROLE_UNKNOWN'
  if (needsOrder) {
    const [number, maximum] = claimIcons[role]
    if (!validOrder) claimDiagnostic = 'CO_ORDER_INVALID'
    else if (order > maximum) claimDiagnostic = 'CO_ICON_UNAVAILABLE'
    else claimIcon = image('yaku' + number + '_' + order)
  } else if (role === 'gong' || role === 'mao') claimIcon = image(role === 'gong' ? 'yaku4' : 'yaku5')
  const death = player?.deathReason
  const deathIcon = alive === false && typeof death === 'string' && Object.hasOwn(deathIcons, death)
    ? image(deathIcons[death]) : null
  return {
    portrait: character && typeof alive === 'boolean'
      ? image(String(character.number).padStart(2, '0') + (alive ? 's' : 'gs')) : null,
    characterText: character ? image(character.number + 'job') : null,
    claimIcon, claimDiagnostic, deathIcon,
    deathDiagnostic: alive === false && !deathIcon ? 'DEATH_ICON_UNAVAILABLE' : null,
    roleText: knownRole ? roles[role] : '職業不明（表示診断）',
    orderText: needsOrder ? (validOrder ? 'CO順 ' + order : 'CO順不明（表示診断）') : '',
  }
}
