import { ApiError } from './api.js'

// Status alone is never evidence that a game command was rejected before execution.
export function recoveryKind(error, operation) {
  if (!(error instanceof ApiError)) return 'unknown'
  const pair = error.status + ':' + error.code
  if (pair === '404:GAME_NOT_FOUND' && operation !== 'create') return 'expired'
  if (operation === 'read') return pair === '400:INVALID_REQUEST' ? 'cursor' : 'unknown'
  if (operation === 'write' && ['409:STALE_REVISION', '409:INVALID_STATE'].includes(pair)) return 'sync'
  if (['400:INVALID_REQUEST', '413:HTTP_ERROR', '415:JSON_REQUIRED'].includes(pair)) return 'rejected'
  if (operation === 'create' && pair === '503:SESSION_LIMIT') return 'capacity'
  return 'unknown'
}

export class ReadFailure extends Error {
  constructor(cause) { super('权威状态读取失败', { cause }) }
}

export const validCredential = id => typeof id === 'string'
  && /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(id)

// Reject an incomplete/HTML-as-JSON success before replacing the last usable state.
// Detailed public-information/artwork diagnostics keep their existing ownership.
export function requireUpdate(value) {
  if (!value || !Number.isSafeInteger(value.revision) || value.revision < 1
      || !['DAY_DECISION', 'FINISHED'].includes(value.phase)
      || !Number.isSafeInteger(value.eventCursor) || value.eventCursor < 0
      || !Number.isSafeInteger(value.nextEventCursor) || value.nextEventCursor < value.eventCursor
      || !Array.isArray(value.events) || !Array.isArray(value.presentedEvents)
      || value.nextEventCursor - value.eventCursor !== value.events.length
      || value.events.length !== value.presentedEvents.length
      || !Array.isArray(value.view?.players) || !value.selections
      || !Array.isArray(value.legalVoteActions) || !Array.isArray(value.legalTargetSelections)
      || !Array.isArray(value.claimInquiry?.roles) || !Array.isArray(value.claimInquiry?.players)) {
    throw new Error('游戏响应结构无法确认')
  }
  return value
}
