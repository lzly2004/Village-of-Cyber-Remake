// Presentation only: consume the audited DTO, never recompute skills or suspicion.
export const comparisons = { CLOSE: '=', SLIGHTLY_GREATER: '≧', GREATER: '>', MUCH_GREATER: '≫' }
const colors = { WHITE: '○', BLACK: '●', NOT_APPLICABLE: '' }
const causes = { EXECUTION: '', DAY_CAT_CURSE: '（猫呪）', DAY_PURSUED: '（後追）' }

export function skillResultText(result, name) {
  return `第${result.skillDay}日：${name(result.targetPlayerNumber)}${colors[result.color] ?? '（未対応の結果）'}`
}

export function suspicionText(row, name) {
  // Desktop appendVotePanel emits only a newline for empty targets, no observer name.
  if (!row.targets.length) return ''
  return name(row.playerNumber) + '：' + row.targets.map((target, index) => name(target)
    + (index < row.targets.length - 1 ? (comparisons[row.comparisons[index]] ?? '？') : '')).join('')
}

export function executionText(death, name) {
  return [...death.prefixPlayers, death.playerNumber].map(name).join('+')
    + (causes[death.cause] ?? '（未対応の死因）')
}
