// Reuse the desktop resource directly; no copied or independently translated lines.
import eventLines from '../../src/main/resources/lines/event_lines.json' with { type: 'json' }
import { characterNames } from './characterNames.js'

const own = (object, key) => Object.hasOwn(object, key)
const record = value => value !== null && typeof value === 'object' && !Array.isArray(value)
const diagnostics = {
  INVALID_EVENT: '事件数据无效',
  SENTINEL_EVENT: 'NONE 不是人物台词事件',
  UNKNOWN_CHARACTER: '人物键无效',
  UNKNOWN_TARGET: '对象人物键无效',
  MISSING_CHARACTER: '缺少人物台词资源',
  MISSING_EVENT: '缺少事件台词资源',
  INVALID_ENTRY: '台词资源结构无效',
  EMPTY_LINE: '所选台词为空',
}
const fallback = diagnostic => ({
  source: 'fallback', diagnostic, text: '台词显示异常：' + diagnostics[diagnostic] + '。',
})

// Matches Resources.getEventText / replaceParams, including empty special precedence.
// The optional catalog is for isolated negative tests; production uses the bundled JSON.
export function eventDialogue(event, catalog = eventLines) {
  if (!record(event) || typeof event.eventname !== 'string' || !event.eventname) {
    return fallback('INVALID_EVENT')
  }
  // DialogueDayDeathHandler bypasses per-character resources for peaceful nights.
  if (event.eventname === 'wsw') {
    return { source: 'system', diagnostic: null, text: '犠牲者はいませんでした。\n' }
  }
  if (event.eventname === 'NONE') return fallback('SENTINEL_EVENT')
  if (typeof event.ch1 !== 'string' || !own(characterNames, event.ch1)) return fallback('UNKNOWN_CHARACTER')
  if (event.ch2 != null && event.ch2 !== 'NONE' && (typeof event.ch2 !== 'string' || !own(characterNames, event.ch2))) {
    return fallback('UNKNOWN_TARGET')
  }
  if (!record(catalog) || !own(catalog, event.ch1)) return fallback('MISSING_CHARACTER')
  const character = catalog[event.ch1]
  if (!record(character)) return fallback('INVALID_ENTRY')
  if (!own(character, event.eventname)) return fallback('MISSING_EVENT')
  const entry = character[event.eventname]
  if (!record(entry) || (entry.special != null && !record(entry.special))) {
    return fallback('INVALID_ENTRY')
  }
  const line = event.ch2 != null && entry.special != null && own(entry.special, event.ch2)
    ? entry.special[event.ch2] : entry.default
  if (line == null || line === '') return fallback('EMPTY_LINE')
  if (typeof line !== 'string') return fallback('INVALID_ENTRY')
  const target = event.ch2 == null || event.ch2 === 'NONE' ? '' : characterNames[event.ch2]
  const text = line.replace(/<param=(\w+)>/g, (_match, key) =>
    ['talkerName', 'targetName', 'partnerName'].includes(key) ? target : '')
  return { source: 'character', diagnostic: null, text }
}
