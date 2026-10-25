import { characterNames } from '../src/characterNames.js'

// Synthetic public-only snapshot for static loading/boundary tests. Not a rule oracle.
export function publicSnapshot() {
  const players = Object.keys(characterNames).map((character, index) => ({
    playerNumber: index + 1, character, alive: true, deathReason: 'NONE', deathDay: 0,
    claimedRole: 'NONE', claimedRoleOrder: 0, comingOutDay: 0,
  }))
  return { revision: 1, eventCursor: 0, nextEventCursor: 0, events: [], presentedEvents: [], phase: 'DAY_DECISION',
    view: { configuration: 'tongchang', gameDay: 1, aliveCount: 44, deathCount: 0, result: 'NONE', players },
    selections: { voteTargets: [], avoidanceEnabled: true,
      divination: { hiddenTargets: [], claimedActors: [] }, guard: { hiddenTargets: [], claimedActors: [] } },
    legalTargetSelections: [{ type: 'VOTE', targets: players.map(p => p.playerNumber), claimedActors: [] }],
    legalVoteActions: [], claimInquiry: { roles: [], players: [] }, revealedRoles: [],
    voteHistory: [],
    publicInformation: { skillReports: [], suspicion: { available: false, rows: [] }, deathHistory: [] },
  }
}
