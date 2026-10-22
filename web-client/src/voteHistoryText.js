const methods = { DESIGNATED: '指定投票', FREE: '自由投票', GREY: 'グレー投票' }

export function votingMethodText(method) {
  return Object.hasOwn(methods, method) ? methods[method] : '未対応の投票方式'
}

// Only format authoritative leaders/counts; never count ballots or choose a winner.
export function voteRoundText(round, playerName) {
  const leaders = round.leaders.map(playerName).join('・')
  return `${round.maximumVotes}票：${leaders}（${round.leaders.length > 1 ? '同票・再投票' : '処刑'}）`
}
