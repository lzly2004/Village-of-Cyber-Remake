<script setup>
import { computed, ref } from 'vue'
import { useGuestGame } from './useGuestGame'
import { configurations, configurationName, roles, claimRoleNames, stages, results, eventLabel } from './labels'

const {
  sessionId, cursor, update, busy, uncertain, notice, storageWarning,
  pending, currentEvent, canDecide, canVote, alternativeVotes, claimRoles, claimPlayers, claimPlayerDraft, canAskClaimPlayers,
  divinationTargets, selectedDivinationTargets, setDivinationTarget,
  start, nextEvent, toggleTarget, vote, askClaimRole, toggleClaimPlayer, askClaimPlayers, setAvoidance, refresh, release,
} = useGuestGame()
const configuration = ref('jianyi')
const confirmingExit = ref(false)
const finished = computed(() => update.value?.phase === 'FINISHED' && !currentEvent.value)
const players = computed(() => update.value?.view.players || [])
const claimCandidates = computed(() => players.value.filter(player => claimPlayers.value.includes(player.playerNumber)))
const voteTargets = computed(() => update.value?.selections.voteTargets || [])
const legalTargets = computed(() =>
  update.value?.legalTargetSelections.find(action => action.type === 'VOTE')?.targets || [])
const revealed = computed(() => new Map(
  finished.value ? update.value.revealedRoles.map(player => [player.playerNumber, player.actualRole]) : [],
))
const lastVote = computed(() => update.value?.voteHistory.at(-1))
const lastRound = computed(() => lastVote.value?.rounds.at(-1))
function name(number) {
  const player = players.value.find(item => item.playerNumber === number)
  return player ? number + ' · ' + player.character : String(number)
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <a class="brand" href="/" aria-label="电脑村首页"><span class="brand-mark">村</span><span>电脑村<small>VILLAGE OF CYBER</small></span></a>
      <div class="sidebar-caption">推理始于倾听。</div>
      <div class="side-section"><span class="eyebrow">当前模式</span><strong>游客 · 文本试玩</strong><p>一局一会话，所有推演由本机游戏服务处理。</p></div>
      <div v-if="update" class="side-section session-meta">
        <span class="eyebrow">对局记录</span>
        <strong>{{ configurationName(update.view.configuration) }}村</strong>
        <span>状态版本 {{ update.revision }}</span>
        <span>已确认事件 {{ cursor }}</span>
        <button class="quiet-button" :disabled="busy" @click="refresh">重新同步</button>
      </div>
      <div class="side-footer"><span class="status-dot"></span>本机试玩<p>会话临时保留，服务重启后失效。<br>本页尚不支持图片、音频与复盘。</p></div>
    </aside>

    <main>
      <header class="topbar"><span>电脑村重制 / 网页实验室</span><span class="build-tag">TEXT EDITION · 01</span></header>
      <div v-if="notice" class="notice" role="alert">{{ notice }}</div>
      <div v-if="storageWarning" class="notice" role="status">{{ storageWarning }}</div>

      <section v-if="!update && !sessionId" class="lobby">
        <span class="eyebrow">WELCOME TO THE VILLAGE</span>
        <h1>每一次发言，<br>都留下线索。</h1>
        <p class="intro">选择一座村落，听取发言，决定今天的投票。<br>熟悉的推理，从浏览器继续。</p>
        <fieldset class="configuration-picker" :disabled="busy">
          <legend>选择配役<span>七种村落，各有暗流</span></legend>
          <label v-for="(item, index) in configurations" :key="item.value" class="configuration-card" :class="{ chosen: configuration === item.value }">
            <input v-model="configuration" type="radio" name="configuration" :value="item.value" :aria-label="item.name + '配役'">
            <span class="configuration-number">0{{ index + 1 }}</span>
            <strong>{{ item.name }}</strong><small>{{ item.description }}</small>
            <span class="radio-mark" aria-hidden="true"></span>
          </label>
        </fieldset>
        <div class="start-row"><button class="primary-button" :disabled="busy" @click="start(configuration)">{{ busy ? '正在开村…' : '开始对局' }} <span aria-hidden="true">↗</span></button><span>无需登录 · 战绩仅保留于本次服务进程</span></div>
      </section>

      <section v-else-if="!update" class="restoring" aria-live="polite">
        <span class="eyebrow">RECONNECTING</span><h1>回到你的村落。</h1>
        <p>{{ busy ? '正在读取会话与尚未确认的发言…' : '会话已保存，等待重新连接本机游戏服务。' }}</p>
        <button class="primary-button" :disabled="busy" @click="refresh">重新同步</button>
        <button class="quiet-button" :disabled="busy" @click="release">释放此会话</button>
      </section>

      <template v-else>
        <section class="game-heading">
          <div><span class="eyebrow">THE VILLAGE JOURNAL</span><h1>{{ configurationName(update.view.configuration) }}村 <span>第 {{ update.view.gameDay }} 日</span></h1></div>
          <div class="population"><strong>{{ update.view.aliveCount }}<small> 存活</small></strong><span>{{ update.view.deathCount }} 人离场</span></div>
        </section>

        <div class="game-layout">
          <section class="journal panel" aria-label="事件展示">
            <header class="panel-heading"><span class="eyebrow">01 / 村落纪事</span><span class="pill">{{ currentEvent ? stages[currentEvent.stage] : finished ? '对局结束' : '白天决策' }}</span></header>
            <div v-if="currentEvent" class="event-content" role="region" aria-label="当前事件" aria-live="polite">
              <span class="event-number">EVENT {{ String(cursor + 1).padStart(3, '0') }}</span>
              <h2>{{ eventLabel(currentEvent.event) }}</h2>
              <div class="event-participants">
                <span v-if="currentEvent.event.ch1 && currentEvent.event.ch1 !== 'NONE'">{{ currentEvent.event.ch1 }}</span>
                <span v-if="currentEvent.event.ch2 && currentEvent.event.ch2 !== 'NONE'"><small>关联玩家</small>{{ currentEvent.event.ch2 }}</span>
                <span v-if="currentEvent.event.ch3 && currentEvent.event.ch3 !== 'NONE'"><small>关联玩家</small>{{ currentEvent.event.ch3 }}</span>
              </div>
              <p class="event-hint">这里显示公开事件摘要。角色的 CO 和报告只代表其公开发言。</p>
              <div class="event-controls"><span>尚有 {{ pending.length }} 条待确认</span><button data-testid="next-event" class="primary-button" :disabled="busy" @click="nextEvent">{{ pending.length === 1 ? '读完，继续对局' : '继续阅读' }} <span aria-hidden="true">→</span></button></div>
            </div>
            <div v-else-if="finished" class="event-content settlement" role="status">
              <span class="event-number">THE FINAL VERDICT</span><h2>{{ results[update.view.result] || '对局结束' }}</h2><p>本局身份现已揭晓，查看玩家列表回顾各自阵营。</p>
            </div>
            <div v-else class="event-content decision">
              <span class="event-number">YOUR DECISION</span><h2>今天，投给谁？</h2><p>从玩家列表勾选指定投票候补，再确认投票。</p><p>遇到回避 CO 时，先听完发言，再重新判断。共有者回避后可取消其目标，另选候补。</p>
            </div>
            <footer class="decision-footer" v-if="!finished">
              <div><span class="eyebrow">指定候补</span><p>{{ voteTargets.length ? voteTargets.map(name).join('、') : '尚未选择' }}</p></div>
              <button class="primary-button" data-testid="vote" :disabled="!canVote" @click="vote('DESIGNATED')">{{ busy ? '处理中…' : '确认投票' }}</button>
              <small v-if="currentEvent">请先阅读完待确认事件。</small><small v-else-if="uncertain">请重新同步以确认服务器状态。</small>
            </footer>
          </section>

          <section class="players panel" aria-label="公开玩家列表">
            <header class="panel-heading"><span class="eyebrow">02 / 村中玩家</span><span>{{ players.length }} 人</span></header>
            <div class="player-list">
              <label v-for="player in players" :key="player.playerNumber" class="player-row" :class="{ departed: !player.alive, selected: voteTargets.includes(player.playerNumber) }">
                <span class="player-number">{{ String(player.playerNumber).padStart(2, '0') }}</span>
                <span class="player-identity"><strong>{{ player.character }}</strong><small>{{ finished ? '真实身份 · ' + roles[revealed.get(player.playerNumber)] : roles[player.claimedRole] }}<span v-if="!player.alive"> · 第 {{ player.deathDay }} 日离场</span></small></span>
                <input v-if="!finished" type="checkbox" :aria-label="'指定 ' + player.character + '（' + player.playerNumber + '号）'" :checked="voteTargets.includes(player.playerNumber)" :disabled="!canDecide || !legalTargets.includes(player.playerNumber)" @click.prevent="toggleTarget(player.playerNumber, !voteTargets.includes(player.playerNumber))">
                <span v-else class="player-state">{{ player.alive ? '存活' : '离场' }}</span>
              </label>
            </div>
          </section>
        </div>

        <section v-if="update.phase === 'DAY_DECISION'" class="panel claim-inquiry" aria-label="自由・グレー投票" lang="ja">
          <header class="panel-heading"><span>自由・グレー投票</span><span>候補はサーバーの最新状態に従います。</span></header>
          <div v-for="action in alternativeVotes" :key="action.method" class="claim-actions">
            <button class="primary-button" :data-testid="'vote-' + action.method" :disabled="!canDecide"
              @click="vote(action.method)">{{ action.method === 'FREE' ? '自由投票' : 'グレー投票' }}</button>
            <span :data-testid="'candidates-' + action.method">{{ action.candidates.map(name).join('、') }}</span>
          </div>
          <p v-if="!alternativeVotes.some(action => action.method === 'GREY')" class="avoidance-help">グレー投票の候補はいません。</p>
        </section>

        <section v-if="update.phase === 'DAY_DECISION'" class="panel claim-inquiry" aria-label="投票回避設定" lang="ja">
          <header class="panel-heading"><span>投票回避CO</span>
            <button class="primary-button" role="switch" aria-label="投票回避CO"
              :aria-checked="update.selections.avoidanceEnabled" :disabled="!canDecide"
              @click="setAvoidance(!update.selections.avoidanceEnabled)">{{ update.selections.avoidanceEnabled ? '回避あり' : '回避なし' }}</button>
          </header>
          <p class="avoidance-help">指定投票時の回避CO。共有の回避は無効化されません。</p>
        </section>

        <section v-if="divinationTargets.length" class="panel claim-inquiry" aria-label="全員の占い予約" lang="ja">
          <header class="panel-heading"><span>全員の占い予約</span><span>当日の予約です。結果は確約されません。</span></header>
          <p class="avoidance-help">潜伏・公開COの占い候補をまとめて設定／解除します。チェックは公開された予約のいずれかに対象が含まれることを示します。</p>
          <div class="claim-player-list">
            <label v-for="target in divinationTargets" :key="target" class="claim-player">
              <input type="checkbox" :aria-label="'全員占い対象 ' + target + '番 ' + name(target)"
                :checked="selectedDivinationTargets.has(target)" :disabled="!canDecide"
                @click.prevent="setDivinationTarget(target, !selectedDivinationTargets.has(target))">
              <span>{{ name(target) }}</span>
            </label>
          </div>
        </section>

        <section v-if="claimRoles.length" class="panel claim-inquiry" aria-label="職業別CO確認" lang="ja">
          <header class="panel-heading"><span>職業別CO確認</span><span>発言を読み終えてから操作してください。</span></header>
          <div class="claim-actions">
            <button v-for="role in claimRoles" :key="role" class="primary-button"
              :disabled="!canDecide" @click="askClaimRole(role)">{{ claimRoleNames[role] || role }}のCOを聞く</button>
          </div>
        </section>

        <section v-if="claimPlayers.length" class="panel claim-inquiry" aria-label="プレイヤー別CO確認" lang="ja">
          <header class="panel-heading"><span>プレイヤー別CO確認</span><span>未送信の選択は再読み込み・同期で解除されます。</span></header>
          <div class="claim-player-list">
            <label v-for="player in claimCandidates" :key="player.playerNumber" class="claim-player">
              <input type="checkbox" :aria-label="player.playerNumber + '番 ' + player.character + 'をCO確認対象に選択'"
                :checked="claimPlayerDraft.includes(player.playerNumber)" :disabled="!canDecide"
                @change="toggleClaimPlayer(player.playerNumber, $event.target.checked)">
              <span>{{ player.playerNumber }} · {{ player.character }}</span>
            </label>
          </div>
          <div class="claim-actions"><button class="primary-button" :disabled="!canAskClaimPlayers" @click="askClaimPlayers">選択したプレイヤーのCOを聞く</button></div>
        </section>

        <section v-if="lastRound" class="panel vote-history" aria-label="最近投票结果">
          <header class="panel-heading"><span class="eyebrow">03 / 最近投票</span><span>第 {{ lastVote.day }} 日 · {{ lastVote.rounds.length }} 轮</span></header>
          <p class="vote-summary">{{ lastRound.maximumVotes }} 票 · {{ name(lastRound.leaders[0]) }} 被处刑</p>
          <div class="vote-lines"><span v-for="line in lastRound.lines" :key="line.playerNumber">{{ name(line.playerNumber) }} <span aria-hidden="true">→</span> {{ name(line.targetPlayerNumber) }}<small>所得 {{ line.receivedVotes }} 票</small></span></div>
        </section>

        <footer class="session-footer">
          <span>临时会话 · 刷新页面可在本标签页恢复。</span>
          <button v-if="!confirmingExit" class="quiet-button" :disabled="busy" @click="confirmingExit = true">{{ finished ? '新开一局' : '结束本次试玩' }}</button>
          <div v-else class="exit-confirm"><span>释放后将无法继续此局。</span><button class="quiet-button" :disabled="busy" @click="confirmingExit = false">保留对局</button><button class="danger-button" :disabled="busy" @click="release().then(() => confirmingExit = false)">确认释放</button></div>
        </footer>
      </template>
    </main>
  </div>
</template>
