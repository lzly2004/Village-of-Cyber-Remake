import catalog from '../../src/main/resources/config/event_image_mapping.json' with { type: 'json' }
import { characterNames } from './characterNames.js'

const own = (value, key) => value && typeof value === 'object' && Object.hasOwn(value, key)
const known = value => typeof value === 'string' && Object.hasOwn(characterNames, value)
const fallback = code => ({ names: ['images/frame.png'], diagnostic: code })
export function eventImageNames(event, source = catalog) {
  if (!event || typeof event !== 'object' || typeof event.eventname !== 'string') return fallback('INVALID_EVENT')
  if (event.eventname === 'wsw') return { names: [], diagnostic: null }
  if (event.eventname === 'NONE' || event.eventname === 'default') return fallback('SENTINEL_EVENT')
  if (!known(event.ch1)) return fallback('UNKNOWN_CHARACTER')
  if (event.ch2 != null && event.ch2 !== 'NONE' && !known(event.ch2)) return fallback('UNKNOWN_TARGET')
  if (!own(source, event.ch1)) return fallback('MISSING_CHARACTER')
  const entries = source[event.ch1]
  if (!own(entries, event.eventname)) return fallback('MISSING_EVENT')
  const entry = entries[event.eventname]
  if (!entry || typeof entry !== 'object' || Array.isArray(entry)) return fallback('INVALID_ENTRY')
  const target = event.ch2 == null ? '' : event.ch2
  const selected = target && own(entry.special, target) ? entry.special[target] : entry.default
  const names = typeof selected === 'string' ? [selected] : selected
  if (!Array.isArray(names) || !names.length) return fallback('EMPTY_IMAGE')
  if (!names.every(name => typeof name === 'string' && /^[^/\\]+\.png$/.test(name))) return fallback('INVALID_IMAGE')
  return { names: names.map(name => 'images/' + name), diagnostic: null }
}

export const leftEvents = ['gyfo1', 'qfjc5', 'zjgh8b', 'zjgb8', 'gprz11p', 'zcrh12']
export const connectEvents = ['jhdh8b', 'gyfo1r', 'qfjcqr5r', 'gprz11p', 'jbdh8r', 'gprz11r', 'zcrh12r']
const dialogueImage = (event, source) => {
  const selected = eventImageNames(event, source)
  return selected.names.length > 1 ? fallback('UNSUPPORTED_DIALOGUE_ARRAY') : selected
}
const supported = stage => stage === 'DAY_DIALOGUE' || stage === 'AFTERNOON_DIALOGUE'

// Fold only public presentation history. A cached image is the previous event, never guessed from ch2/ch3.
export function dialogueArtwork(history, cursor, current, source = catalog) {
  if (!supported(current?.stage)) return { mode: 'deferred', actors: [], background: null, diagnostic: null }
  const background = current.stage === 'DAY_DIALOGUE' ? 'images/haikei3.png' : 'images/haikei.png'
  const single = (event, diagnostic = null) => {
    const selected = dialogueImage(event, source)
    return { mode: 'dialogue', background, diagnostic: diagnostic || selected.diagnostic,
      actors: selected.names.length ? [{ name: selected.names[0], x: null, layer: 0 }] : [] }
  }
  if (!Array.isArray(history) || !Number.isSafeInteger(cursor) || cursor < 0 || cursor >= history.length
    || history[cursor]?.stage !== current.stage
    || ['eventname','ch1','ch2'].some(key => history[cursor]?.event?.[key] !== current.event?.[key])) return single(current.event, 'CONTEXT_UNAVAILABLE')
  let cache = null, special = false, result
  for (let index = 0; index <= cursor; index++) {
    const presented = history[index]
    const stage = presented?.stage
    if (!supported(stage)) { cache = null; special = false; continue }
    if (index && history[index-1]?.stage === 'DAY_DIALOGUE' && stage !== 'DAY_DIALOGUE') cache = null
    const event = presented.event
    const selected = dialogueImage(event, source)
    const name = selected.names[0]
    let actors = []
    let diagnostic = selected.diagnostic
    if (stage === 'DAY_DIALOGUE' && cache && !connectEvents.includes(event?.eventname)) cache = null
    if (cache && name) {
      diagnostic ||= cache.diagnostic
      actors = special && stage === 'DAY_DIALOGUE'
        ? [{ name, x: 300, layer: 1 }, { name: cache.name, x: 650, layer: 0 }]
        : [{ name: cache.name, x: 300, layer: 1 }, { name, x: 650, layer: 0 }]
      cache = null
      if (stage === 'DAY_DIALOGUE' && special) special = false
      else if (stage === 'DAY_DIALOGUE' && event?.eventname === 'gprz11r'
        && history[index+1]?.stage === stage && history[index+1]?.event?.eventname === 'gprz11p') {
        cache = { name, diagnostic: selected.diagnostic }; special = true
      }
    } else if (name) {
      const left = leftEvents.includes(event?.eventname)
      actors = [{ name, x: left ? 300 : null, layer: 0 }]
      if (left) cache = { name, diagnostic: selected.diagnostic }
    }
    result = { mode: 'dialogue', background, actors, diagnostic: diagnostic || null }
  }
  return result || single(current.event, 'CONTEXT_UNAVAILABLE')
}

export function mergePresentationHistory(history, snapshot) {
  const base = snapshot.eventCursor
  const events = snapshot.presentedEvents
  if (!Number.isSafeInteger(base) || base < 0 || !Array.isArray(events)
    || events.length !== snapshot.nextEventCursor - base || base > history.length) return null
  return history.slice(0, base).concat(events)
}
