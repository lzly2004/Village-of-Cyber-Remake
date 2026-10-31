import { computed, onMounted, ref, shallowRef, watch } from 'vue'
import { ApiError, request } from './api'
import { ReadFailure, recoveryKind, requireUpdate, validCredential } from './requestRecovery.js'
import { mergePresentationHistory } from './eventArtwork.js'

export const STORAGE_KEY = 'vocr.guest.v1'

export function useGuestGame() {
  const sessionId = ref('')
  const cursor = ref(0)
  const update = shallowRef(null)
  const presentationHistory = shallowRef([])
  const completedVotePresentation = shallowRef(null)
  let presentationVersion = 0
  function rememberPresentation(snapshot) {
    const version = ++presentationVersion
    const merged = mergePresentationHistory(presentationHistory.value, snapshot)
    if (merged) { presentationHistory.value = merged; return }
    presentationHistory.value = []
    const id = sessionId.value
    // Presentation recovery is read-only and cannot delay/replace an authoritative game update.
    request('/' + id + '?eventCursor=0').then(full => {
      if (version !== presentationVersion || sessionId.value !== id) return
      if (full.revision !== snapshot.revision || full.eventCursor !== 0) return
      const recovered = mergePresentationHistory([], full)
      if (recovered) presentationHistory.value = recovered
    }).catch(() => { /* GameStage visibly diagnoses unavailable context; no command is retried. */ })
  }
  const busy = ref(false)
  const uncertain = ref(false)
  const notice = ref('')
  const storageWarning = ref('')
  const pending = computed(() => update.value
    ? update.value.presentedEvents.slice(Math.max(0, cursor.value - update.value.eventCursor))
    : [])
  const currentEvent = computed(() => pending.value[0] || null)
  const canDecide = computed(() => update.value?.phase === 'DAY_DECISION'
    && !currentEvent.value && !busy.value && !uncertain.value)
  const designatedVote = computed(() =>
    update.value?.legalVoteActions.find(action => action.method === 'DESIGNATED'))
  const canVote = computed(() => canDecide.value && Boolean(designatedVote.value))
  const alternativeVotes = computed(() => update.value?.legalVoteActions.filter(
    action => action.method === 'FREE' || action.method === 'GREY') || [])
  const claimRoles = computed(() => update.value?.claimInquiry.roles || [])
  const divinationTargets = computed(() => update.value?.legalTargetSelections.find(
    action => action.type === 'DIVINATION')?.targets || [])
  const selectedDivinationTargets = computed(() => {
    const selections = update.value?.selections.divination
    return new Set(selections ? [...selections.hiddenTargets,
      ...selections.claimedActors.flatMap(actor => actor.targets)] : [])
  })
  const claimPlayers = computed(() => update.value?.claimInquiry.players || [])
  const guardTargets = computed(() => update.value?.legalTargetSelections.find(
    action => action.type === 'GUARD')?.targets || [])
  const selectedGuardTargets = computed(() => {
    const selections = update.value?.selections.guard
    return new Set(selections ? [...selections.hiddenTargets,
      ...selections.claimedActors.flatMap(actor => actor.targets)] : [])
  })
  const claimPlayerDraft = ref([])
  const canAskClaimPlayers = computed(() => canDecide.value && claimPlayerDraft.value.length > 0
    && claimPlayerDraft.value.every(player => claimPlayers.value.includes(player)))
  // Unsubmitted choices are UI drafts, never authoritative or replayed after synchronization.
  watch(update, () => { claimPlayerDraft.value = [] })

  function persist() {
    try {
      if (sessionId.value) sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
        sessionId: sessionId.value, eventCursor: cursor.value,
      }))
      else sessionStorage.removeItem(STORAGE_KEY)
    } catch {
      storageWarning.value = '浏览器未允许保存会话，刷新后将无法自动恢复。'
    }
  }

  function forget() {
    sessionId.value = ''
    cursor.value = 0
    update.value = null
    presentationVersion++
    presentationHistory.value = []
    completedVotePresentation.value = null
    uncertain.value = false
    persist()
  }

  async function sync() {
    if (!sessionId.value) return
    let snapshot, nextCursor = cursor.value
    try {
      try {
        snapshot = await request('/' + sessionId.value + '?eventCursor=' + nextCursor)
      } catch (error) {
        if (recoveryKind(error, 'read') !== 'cursor' || nextCursor === 0) throw error
        // Only this route's documented business cursor rejection permits one C=0 read.
        snapshot = await request('/' + sessionId.value + '?eventCursor=0')
        nextCursor = 0
      }
      requireUpdate(snapshot)
      if (snapshot.eventCursor !== nextCursor) throw new Error('续读窗口与请求不一致')
    } catch (error) {
      throw new ReadFailure(error)
    }
    cursor.value = nextCursor
    update.value = snapshot
    rememberPresentation(snapshot)
    uncertain.value = false
    persist()
  }

  async function accept(snapshot) {
    requireUpdate(snapshot)
    // 命令窗口若晚于已确认游标，先补读其间事件，不能跳过未展示内容。
    if (snapshot.eventCursor > cursor.value || snapshot.nextEventCursor < cursor.value) {
      await sync()
    } else {
      update.value = snapshot
      rememberPresentation(snapshot)
      persist()
    }
  }

  async function handleError(error, operation) {
    if (error instanceof ReadFailure) { error = error.cause; operation = 'read' }
    const kind = recoveryKind(error, operation)
    if (kind === 'limited') {
      if (operation === 'read') uncertain.value = Boolean(sessionId.value)
      notice.value = operation === 'read'
        ? '读取暂被限流，已保留本局与确认游标；请稍后主动重新同步。'
        : '请求暂被限流，本次操作未执行。请稍后主动重试，不会自动重发。'
    } else if (kind === 'expired') {
      forget()
      notice.value = '这局游戏已失效，可能是闲置过期、达到最长时限、服务重启或会话已被释放。请选择配役重新开局。'
    } else if (kind === 'capacity') {
      notice.value = '游客会话已达上限，请稍后重试或释放你持有的旧对局。未创建新局。'
    } else if (kind === 'sync') {
      await sync()
      notice.value = '对局状态已更新，已重新同步。请查看最新事件与选择后再操作。'
    } else if (kind === 'rejected') {
      notice.value = error.message
    } else if (operation === 'read') {
      uncertain.value = Boolean(sessionId.value)
      notice.value = '暂时无法读取权威状态；已保留本局与确认游标，请重新同步后继续。'
        + (error instanceof ApiError ? ' ' + error.message : '')
    } else if (operation === 'create') {
      uncertain.value = true
      notice.value = sessionId.value
        ? '开局状态未能确认，已保留本局凭据。请重新同步后继续，不会自动重试开局。'
        : '开局响应未能确认，服务器可能已创建对局，但未取得可恢复的凭据。不会自动重试；你可以主动重新开局，孤儿局将按时限回收。'
    } else {
      uncertain.value = Boolean(sessionId.value)
      notice.value = '连接未能确认，操作可能已在服务器完成。请重新同步后继续。'
    }
  }

  async function run(action, operation = 'write') {
    if (busy.value) return
    busy.value = true
    notice.value = ''
    try {
      await action()
    } catch (error) {
      try {
        await handleError(error, operation)
      } catch (syncError) {
        if (syncError instanceof ReadFailure) syncError = syncError.cause
        if (syncError instanceof ApiError && syncError.status === 404 && syncError.code === 'GAME_NOT_FOUND') {
          forget()
          notice.value = '对局已失效，请重新开局。'
        } else {
          uncertain.value = true
          notice.value = '暂时无法同步对局，请检查本机服务后重试。'
        }
      }
    } finally {
      busy.value = false
    }
  }

  function start(configuration) {
    if (sessionId.value) return
    return run(async () => {
      const created = await request('', { method: 'POST', body: { configuration } })
      if (!validCredential(created?.sessionId)) throw new Error('开局凭据无法确认')
      sessionId.value = created.sessionId
      cursor.value = 0
      persist()
      requireUpdate(created.update)
      if (created.update.eventCursor !== 0) throw new Error('开局事件窗口无法确认')
      update.value = created.update
      rememberPresentation(created.update)
      uncertain.value = false
      persist()
    }, 'create')
  }

  function nextEvent() {
    if (!currentEvent.value || busy.value) return
    cursor.value++
    persist()
  }

  function toggleTarget(target, selected) {
    if (!canDecide.value) return
    return run(async () => {
      await accept(await request('/' + sessionId.value + '/vote-target', {
        method: 'PUT', body: { expectedRevision: update.value.revision, target, selected },
      }))
    })
  }

  function vote(method) {
    const action = update.value?.legalVoteActions.find(action => action.method === method)
    if (!canDecide.value || !action) return
    return run(async () => {
      const voteSession=sessionId.value, voteDay=update.value.view.gameDay
      // Keep only the already-public pre-vote display, matching the still-mounted Swing scene.
      const before={view:update.value.view,information:update.value.publicInformation}
      const result = await request('/' + sessionId.value + '/votes', {
        method: 'POST', body: {
          expectedRevision: update.value.revision,
          method: action.method,
          candidates: action.candidates,
        },
      })
      if (typeof result?.voteCompleted !== 'boolean') throw new Error('投票响应无法确认')
      await accept(result.update)
      if(result.voteCompleted && sessionId.value===voteSession && update.value.revision===result.update.revision){
        const completed=result.update.voteHistory.find(item=>item.day===voteDay)
        if(completed)completedVotePresentation.value={sessionId:voteSession,vote:completed,before}
      }
      if (!result.voteCompleted) notice.value = '投票被回避 CO 中断。读完新发言后，请重新确认投票目标。'
    })
  }

  function askClaimRole(role) {
    if (!canDecide.value || !claimRoles.value.includes(role)) return
    return run(async () => {
      await accept(await request('/' + sessionId.value + '/claims/by-role', {
        method: 'POST', body: { expectedRevision: update.value.revision, role },
      }))
    })
  }

  function setDivinationTarget(target, selected) {
    if (!canDecide.value || !divinationTargets.value.includes(target)) return
    return run(async () => {
      await accept(await request('/' + sessionId.value + '/target-selections', {
        method: 'PUT', body: {
          expectedRevision: update.value.revision, type: 'DIVINATION', target, selected,
        },
      }))
    })
  }

  function setGuardTarget(target, selected) {
    if (!canDecide.value || !guardTargets.value.includes(target)) return
    return run(async () => {
      await accept(await request('/' + sessionId.value + '/target-selections', {
        method: 'PUT', body: {
          expectedRevision: update.value.revision, type: 'GUARD', target, selected,
        },
      }))
    })
  }

  function setClaimedTarget(type, claimedPlayer, target, selected) {
    const action = update.value?.legalTargetSelections.find(action => action.type === type)
    if (!canDecide.value || !['DIVINATION', 'GUARD'].includes(type)
      || !action?.targets.includes(target)
      || !action.claimedActors.some(actor => actor.playerNumber === claimedPlayer)) return
    return run(async () => {
      await accept(await request('/' + sessionId.value + '/claimed-target-selections', {
        method: 'PUT', body: { expectedRevision: update.value.revision, type, claimedPlayer, target, selected },
      }))
    })
  }

  function refresh() { return run(sync, 'read') }

  function setAvoidance(enabled) {
    if (!canDecide.value) return
    return run(async () => {
      await accept(await request('/' + sessionId.value + '/avoidance', {
        method: 'PUT', body: { expectedRevision: update.value.revision, enabled },
      }))
    })
  }

  function toggleClaimPlayer(player, selected) {
    if (!canDecide.value || !claimPlayers.value.includes(player)) return
    if (selected && !claimPlayerDraft.value.includes(player)) claimPlayerDraft.value.push(player)
    else if (!selected) claimPlayerDraft.value = claimPlayerDraft.value.filter(value => value !== player)
  }

  function askClaimPlayers() {
    if (!canAskClaimPlayers.value) return
    const players = [...claimPlayerDraft.value]
    return run(async () => {
      await accept(await request('/' + sessionId.value + '/claims/by-players', {
        method: 'POST', body: { expectedRevision: update.value.revision, players },
      }))
    })
  }

  function release() {
    return run(async () => {
      await request('/' + sessionId.value, { method: 'DELETE' })
      forget()
      notice.value = '已结束这次试玩，可以开始新的一局。'
    })
  }

  onMounted(() => {
    try {
      const saved = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || 'null')
      if (saved && validCredential(saved.sessionId)
          && Number.isSafeInteger(saved.eventCursor) && saved.eventCursor >= 0) {
        sessionId.value = saved.sessionId
        cursor.value = saved.eventCursor
        refresh()
      } else {
        if (saved !== null) notice.value = '本地会话凭据无效，已清除。请选择配役重新开局。'
        persist()
      }
    } catch {
      notice.value = '无法读取本地会话记录，不能自动恢复。请选择配役重新开局。'
      persist()
    }
  })

  return {
    sessionId, cursor, update, busy, uncertain, notice, storageWarning, presentationHistory, completedVotePresentation,
    pending, currentEvent, canDecide, canVote, alternativeVotes, claimRoles, claimPlayers, claimPlayerDraft, canAskClaimPlayers,
    divinationTargets, selectedDivinationTargets, setDivinationTarget,
    guardTargets, selectedGuardTargets, setGuardTarget,
    setClaimedTarget,
    start, nextEvent, toggleTarget, vote, askClaimRole, toggleClaimPlayer, askClaimPlayers, setAvoidance, refresh, release,
  }
}
