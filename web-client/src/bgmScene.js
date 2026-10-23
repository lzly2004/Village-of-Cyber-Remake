// Desktop integer division intentionally selects BOTH 3 and 4 living players.
export const isLateBgm = alive => Number.isSafeInteger(alive) && Math.trunc((alive - 1) / 2) === 1
export function bgmScene({ sessionId, update, currentEvent, finished, uncertain }) {
  if (uncertain) return { key: 'uncertain', id: null }
  if (!update) return { key: sessionId ? 'restoring' : 'lobby', id: sessionId ? null : 'START_MENU' }
  const stage = currentEvent?.stage
  if (stage === 'SETTLEMENT' || finished) {
    const result = update.view.result
    const id = result === 'VILLAGE_WIN' ? 'VICTORY_SCREEN'
      : ['WOLF_WIN', 'FOX_WIN'].includes(result) ? 'DEFEAT_SCREEN' : null
    return { key: 'settlement-' + (id || 'unknown'), id }
  }
  if (stage === 'NIGHT_RESULT' || stage === 'EXECUTION') return { key: stage, id: null }
  if (['DAY_DIALOGUE', 'AFTERNOON_DIALOGUE'].includes(stage) || (!currentEvent && update.phase === 'DAY_DECISION')) {
    const late = isLateBgm(update.view.aliveCount)
    return { key: 'day-' + update.view.gameDay, id: late ? 'LIGHTLESS_GAZE' : 'EMOTIONALLY_UNSTABLE',
      playlist: late ? 'LATE_GAME_PLAYLIST' : 'NORMAL_GAME_PLAYLIST' }
  }
  return { key: 'unknown', id: null }
}
