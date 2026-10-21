import { computed, onMounted, ref, shallowRef, watch } from 'vue'
import { ApiError, request } from './api'

export const STORAGE_KEY = 'vocr.guest.v1'

export function useGuestGame() {
  const sessionId = ref('')
  const cursor = ref(0)
  const update = shallowRef(null)
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
  const claimRoles = computed(() => update.value?.claimInquiry.roles || [])
  const claimPlayers = computed(() => update.value?.claimInquiry.players || [])
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
    uncertain.value = false
    persist()
  }

  async function sync() {
    if (!sessionId.value) return
    let snapshot
    try {
      snapshot = await request('/' + sessionId.value + '?eventCursor=' + cursor.value)
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 400) throw error
      // 损坏或超出范围的本地游标从头恢复，确保不丢事件。
      snapshot = await request('/' + sessionId.value + '?eventCursor=0')
      cursor.value = 0
    }
    update.value = snapshot
    uncertain.value = false
    persist()
  }

  async function accept(snapshot) {
    // 命令窗口若晚于已确认游标，先补读其间事件，不能跳过未展示内容。
    if (snapshot.eventCursor > cursor.value || snapshot.nextEventCursor < cursor.value) {
      await sync()
    } else {
      update.value = snapshot
      persist()
    }
  }

  async function handleError(error) {
    if (error instanceof ApiError && error.status === 404) {
      forget()
      notice.value = '这局游戏已失效，可能是服务重启或会话已被释放。请选择配役重新开局。'
    } else if (error instanceof ApiError && error.status === 409) {
      await sync()
      notice.value = '对局状态已更新，已重新同步。请查看最新事件与选择后再操作。'
    } else if (error instanceof ApiError && error.status < 500) {
      notice.value = error.message
    } else if (!sessionId.value) {
      notice.value = error instanceof ApiError
        ? error.message : '无法连接本机游戏服务，请确认服务已启动后重试。'
    } else {
      uncertain.value = Boolean(sessionId.value)
      notice.value = '连接未能确认，操作可能已在服务器完成。请重新同步后继续。'
    }
  }

  async function run(action) {
    if (busy.value) return
    busy.value = true
    notice.value = ''
    try {
      await action()
    } catch (error) {
      try {
        await handleError(error)
      } catch (syncError) {
        if (syncError instanceof ApiError && syncError.status === 404) {
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
    return run(async () => {
      const created = await request('', { method: 'POST', body: { configuration } })
      sessionId.value = created.sessionId
      cursor.value = 0
      update.value = created.update
      uncertain.value = false
      persist()
    })
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

  function vote() {
    if (!canVote.value) return
    return run(async () => {
      const result = await request('/' + sessionId.value + '/votes', {
        method: 'POST', body: {
          expectedRevision: update.value.revision,
          method: 'DESIGNATED',
          candidates: designatedVote.value.candidates,
        },
      })
      await accept(result.update)
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

  function refresh() { return run(sync) }

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
      if (saved && /^[0-9a-f-]{36}$/i.test(saved.sessionId)
          && Number.isSafeInteger(saved.eventCursor) && saved.eventCursor >= 0) {
        sessionId.value = saved.sessionId
        cursor.value = saved.eventCursor
        refresh()
      } else {
        persist()
      }
    } catch {
      persist()
    }
  })

  return {
    sessionId, cursor, update, busy, uncertain, notice, storageWarning,
    pending, currentEvent, canDecide, canVote, claimRoles, claimPlayers, claimPlayerDraft, canAskClaimPlayers,
    start, nextEvent, toggleTarget, vote, askClaimRole, toggleClaimPlayer, askClaimPlayers, refresh, release,
  }
}
