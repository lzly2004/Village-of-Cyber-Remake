import { dialogueArtwork, eventImageNames } from './eventArtwork.js'
import { characterDisplayName } from './characterDisplay.js'

// UiImage and GameStrings values; source assertions lock these to desktop call sites.
export const sceneImages = {
  day: 'images/haikei3.png', afternoon: 'images/haikei.png', review: 'images/frame #19252.png',
}
export const outcomes = {
  VILLAGE_WIN: { background: 'images/endVillage.png', icon: 'images/Icon1_0.png', text: '村人勝利' },
  WOLF_WIN: { background: 'images/endWolf.png', icon: 'images/Icon2.png', text: '人狼勝利' },
  FOX_WIN: { background: 'images/endFox.png', icon: 'images/Icon4.png', text: '妖狐勝利' },
}
export const transitionTimes = { NIGHT_RESULT: 1000, EXECUTION: 2000 }
export const consequenceFormats = {
  cxs: '投票の結果、%sは処刑されました。', hzsw: '%s後追いで死亡した。', mzsw: '%s猫の呪いによって死亡した。',
}
const own = (object, key) => typeof key === 'string' && Object.hasOwn(object, key)
const centered = name => [{ name, x: null, layer: 0 }]
const invalid = (background, diagnostic) => ({ mode: 'scene', background,
  actors: centered('images/frame.png'), diagnostic })

// Reads current public event and outcome only. No actual roles, private skills or death-reason inference.
export function sceneArtwork(history, cursor, current, result, finished = false, source) {
  if (finished) return { mode: 'scene', background: sceneImages.review, actors: [],
    diagnostic: own(outcomes, result) ? null : 'UNKNOWN_OUTCOME' }
  const stage = current?.stage
  if (stage === 'DAY_DIALOGUE' || stage === 'AFTERNOON_DIALOGUE') return dialogueArtwork(history, cursor, current, source)
  if (!['NIGHT_RESULT', 'EXECUTION', 'SETTLEMENT'].includes(stage)) return { mode: 'deferred' }
  const event = current.event
  const background = stage === 'NIGHT_RESULT' ? sceneImages.day : stage === 'EXECUTION' ? sceneImages.afternoon
    : own(outcomes, result) ? outcomes[result].background : 'images/frame.png'
  if (stage === 'SETTLEMENT' && !own(outcomes, result)) return invalid(background, 'UNKNOWN_OUTCOME')
  const allowed = stage === 'NIGHT_RESULT' ? ['wsw','yjsw'] : stage === 'EXECUTION' ? ['cxs','hzsw','mzsw'] : ['crsl','rlsl','krsl','yhsl']
  if (!allowed.includes(event?.eventname)) return invalid(background, 'STAGE_EVENT_MISMATCH')
  const selected = eventImageNames(event, source)
  if (selected.diagnostic) return invalid(background, selected.diagnostic)
  if (event.eventname === 'wsw') return { mode: 'scene', background, actors: [], diagnostic: null }
  if (stage === 'SETTLEMENT') {
    if (selected.names.length !== 1) return invalid(background, 'INVALID_SETTLEMENT_IMAGES')
    return { mode: 'scene', background, actors: centered(selected.names[0]), diagnostic: null }
  }
  if (selected.names.length !== 2) return invalid(background, 'INVALID_TRANSITION_IMAGES')
  return { mode: 'scene', background, actors: centered(selected.names[0]),
    frames: selected.names.map(centered), transitionMs: transitionTimes[stage], diagnostic: null,
    consequence: stage === 'EXECUTION' ? consequenceFormats[event.eventname].replace('%s', characterDisplayName(event.ch1)) : null }
}
