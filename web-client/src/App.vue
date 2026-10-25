<script setup>
import { computed, ref, defineAsyncComponent, h } from 'vue'
import { useGuestGame } from './useGuestGame'
import PublicInformationPanel from './PublicInformationPanel.vue'
import VoteHistoryPanel from './VoteHistoryPanel.vue'
import EventDialogue from './EventDialogue.vue'
import GameStage from './GameStage.vue'
import BgmControl from './BgmControl.vue'
import PlayerPortrait from './PlayerPortrait.vue'
import ReplayEntry from './ReplayEntry.vue'
import AssetImage from './AssetImage.vue'
import { characterDisplayName } from './characterDisplay.js'
import { playerVisual } from './playerVisual.js'
import { sceneArtwork, outcomes } from './sceneArtwork.js'
import { settlementReveal } from './settlementVisual.js'
import { configurations, configurationName, roles, claimRoleNames, stages } from './labels'

const {
  sessionId, cursor, update, busy, uncertain, notice, storageWarning, presentationHistory,
  pending, currentEvent, canDecide, canVote, alternativeVotes, claimRoles, claimPlayers, claimPlayerDraft, canAskClaimPlayers,
  divinationTargets, selectedDivinationTargets, setDivinationTarget,
  guardTargets, selectedGuardTargets, setGuardTarget,
  setClaimedTarget,
  start, nextEvent, toggleTarget, vote, askClaimRole, toggleClaimPlayer, askClaimPlayers, setAvoidance, refresh, release,
} = useGuestGame()
const artwork = computed(() => sceneArtwork(presentationHistory.value, cursor.value, currentEvent.value, update.value?.view.result, finished.value))
const audioControl = ref(null)
const replayPlaying = ref(false)
const replayAudio = { state:()=>audioControl.value?.status, enable:()=>audioControl.value?.enable(), stop:()=>audioControl.value?.stop() }
function clickSound(event) {
  const button = event.target.closest('main button')
  if (button && !button.disabled) audioControl.value?.click()
}
const HelpDialog = defineAsyncComponent({
  loader: () => import('./HelpDialog.vue'),
  timeout: 15000,
  errorComponent: { render: () => h('section', { class: 'notice', role: 'alert', lang: 'ja' }, [
    h('p', 'ヘルプを読み込めません（HELP_LOAD_FAILED）。対局の操作は続けられます。'),
    h('button', { class: 'quiet-button', onClick: () => { helpPage.value = '' } }, 'ヘルプを閉じる'),
  ]) },
})
const helpPage = ref('')
const configuration = ref('jianyi')
const confirmingExit = ref(false)
const finished = computed(() => update.value?.phase === 'FINISHED' && !currentEvent.value)
const players = computed(() => update.value?.view.players || [])
const claimCandidates = computed(() => players.value.filter(player => claimPlayers.value.includes(player.playerNumber)))
const voteTargets = computed(() => update.value?.selections.voteTargets || [])
const legalTargets = computed(() =>
  update.value?.legalTargetSelections.find(action => action.type === 'VOTE')?.targets || [])
const reveal = computed(() => settlementReveal(finished.value, players.value, finished.value ? update.value.revealedRoles : null))
const outcome = computed(() => finished.value && typeof update.value.view.result === 'string' && Object.hasOwn(outcomes, update.value.view.result) ? outcomes[update.value.view.result] : null)
const claimedSkills = computed(() => (update.value?.legalTargetSelections || [])
  .filter(action => ['DIVINATION', 'GUARD'].includes(action.type))
  .map(action => {
    const skill = action.type === 'DIVINATION' ? 'divination' : 'guard'
    return { ...action, label: action.type === 'DIVINATION' ? '個別の占い予約' : '個別の護衛予約',
      actors: action.claimedActors.map(actor => ({ ...actor,
        targets: update.value.selections[skill].claimedActors
          .find(selected => selected.playerNumber === actor.playerNumber)?.targets || [],
      })),
    }
  }))
function name(number) {
  const player = players.value.find(item => item.playerNumber === number)
  return player ? number + ' · ' + characterDisplayName(player.character) : String(number)
}
</script>

<template>
  <div class="app-shell" @click.capture="clickSound">
    <aside class="sidebar">
      <a class="brand" href="/" aria-label="電脳村ホーム"><span class="brand-mark">村</span><span>電脳村<small>VILLAGE OF CYBER</small></span></a>
      <div class="sidebar-caption">まずは、声に耳を。</div>
      <div class="side-section"><span class="eyebrow">プレイモード</span><strong>ゲスト・試遊版</strong><p>対局は一時セッションです。ゲーム処理はローカルサーバーで行います。</p></div>
      <div v-if="update" class="side-section session-meta">
        <span class="eyebrow">対局情報</span>
        <strong>{{ configurationName(update.view.configuration) }}村</strong>
        <span>状態バージョン {{ update.revision }}</span>
        <span>確認済みイベント {{ cursor }}</span>
        <button class="quiet-button" :disabled="busy" @click="refresh">再同期</button>
      </div>
      <nav class="help-menu" aria-label="メニュー"><button class="quiet-button" @click="helpPage = 'help'">遊び方・ヘルプ</button><button class="quiet-button" @click="helpPage = 'configurations'">配役説明</button></nav>
      <BgmControl :replaying="replayPlaying" ref="audioControl" :cursor="cursor" :session-id="sessionId" :update="update" :current-event="currentEvent" :finished="finished" :uncertain="uncertain" :can-decide="canDecide" />
      <div class="side-footer"><span class="status-dot"></span>ローカル試遊<p>セッションは一時保存です。サービス再起動後は無効になります。<br>画像・音声に対応。終局Replayはこのブラウザーに保存できます。</p></div>
    </aside>

    <main>
      <header class="topbar"><span>電脳村リメイク / Web試遊版</span><span class="build-tag">TEXT EDITION · 01</span></header>
      <div v-if="notice" class="notice" role="alert" lang="zh-CN">{{ notice }}</div>
      <div v-if="storageWarning" class="notice" role="status" lang="zh-CN">{{ storageWarning }}</div>

      <ReplayEntry :session-id="sessionId" :cursor="cursor" :eligible="finished && !busy && !uncertain" :audio-controls="replayAudio" @playing="replayPlaying=$event" />

      <section v-if="!update && !sessionId" class="lobby">
        <span class="eyebrow">WELCOME TO THE VILLAGE</span>
        <h1>一つひとつの発言が、<br>手掛かりになる。</h1>
        <p class="intro">配役を選び、発言を読み、今日の投票を決めましょう。<br>ブラウザーから、村の推理を。</p>
        <fieldset class="configuration-picker" :disabled="busy">
          <legend>配役を選択<span>七つの村から選べます</span></legend>
          <label v-for="(item, index) in configurations" :key="item.value" class="configuration-card" :class="{ chosen: configuration === item.value }">
            <input v-model="configuration" type="radio" name="configuration" :value="item.value" :aria-label="item.name + '配役'">
            <span class="configuration-number">0{{ index + 1 }}</span>
            <strong>{{ item.name }}</strong><small>{{ item.description }}</small>
            <span class="radio-mark" aria-hidden="true"></span>
          </label>
        </fieldset>
        <div class="start-row"><button class="primary-button" :disabled="busy" @click="start(configuration)">{{ busy ? '開村中…' : '対局を始める' }} <span aria-hidden="true">↗</span></button><span>ログイン不要・戦績はサービス終了までの一時記録です</span></div>
      </section>

      <section v-else-if="!update" class="restoring" aria-live="polite">
        <span class="eyebrow">RECONNECTING</span><h1>村へ戻りましょう。</h1>
        <p>{{ busy ? 'セッションと未確認の発言を読み込んでいます…' : '保存済みのセッションがあります。ローカルサーバーへの再接続を待っています。' }}</p>
        <button class="primary-button" :disabled="busy" @click="refresh">再同期</button>
        <button class="quiet-button" :disabled="busy" @click="release">セッションを解放</button>
      </section>

      <template v-else>
        <section class="game-heading">
          <div><span class="eyebrow">THE VILLAGE JOURNAL</span><h1>{{ configurationName(update.view.configuration) }}村 <span>第 {{ update.view.gameDay }} 日</span></h1></div>
          <div class="population"><strong>{{ update.view.aliveCount }}<small> 生存</small></strong><span>{{ update.view.deathCount }} 人離場</span></div>
        </section>

        <div class="game-layout">
          <section class="journal panel" aria-labelledby="journal-label">
            <span id="journal-label" class="visually-hidden" lang="zh-CN">事件展示</span>
            <header class="panel-heading"><span class="eyebrow" lang="zh-CN">01 / 村落纪事</span><span class="pill" :lang="currentEvent ? 'zh-CN' : 'ja'">{{ currentEvent ? stages[currentEvent.stage] : finished ? '対局終了' : '昼の指示' }}</span></header>
            <!-- Each event/display mode gets a fresh scroller; old native keyboard animation cannot follow it. -->
            <GameStage :key="(currentEvent ? 'event-' : finished ? 'finished-' : 'decision-') + cursor" :event-key="sessionId + ':' + cursor" @consequence="audioControl?.transition($event)" :event="currentEvent?.event" :stage="currentEvent?.stage" :artwork="artwork">
              <template #scene-overlay>
                <div v-if="finished" class="settlement-board" role="region" aria-label="結算役職一覧" tabindex="0" data-testid="settlement-board" lang="ja">
                  <p>配役一覧 · {{ players.length }}人 · 一覧内をスクロールして確認できます。</p>
                  <div class="settlement-grid">
                    <div v-for="player in players" :key="player.playerNumber" class="settlement-card">
                      <PlayerPortrait :player="player" revealed :actual-role="reveal.roles.get(player.playerNumber)" />
                      <strong>{{ player.playerNumber }} · {{ characterDisplayName(player.character) }}</strong>
                      <small>{{ roles[reveal.roles.get(player.playerNumber)] || '身分不明（表示診断）' }}</small>
                    </div>
                  </div>
                </div>
              </template>
              <div v-if="currentEvent" class="event-content" role="region" aria-labelledby="current-event-label" lang="ja" aria-live="polite">
                <span id="current-event-label" class="visually-hidden" lang="zh-CN">当前事件</span>
                <span class="event-number">EVENT {{ String(cursor + 1).padStart(3, '0') }}</span>
                <EventDialogue :event="currentEvent.event" />
                <p v-if="artwork.consequence" class="event-consequence" lang="ja" data-testid="event-consequence">{{ artwork.consequence }}</p>
                <div v-if="currentEvent.event" class="event-participants">
                  <span v-if="currentEvent.event.ch1 && currentEvent.event.ch1 !== 'NONE'">{{ characterDisplayName(currentEvent.event.ch1) }}</span>
                  <span v-if="currentEvent.event.ch2 && currentEvent.event.ch2 !== 'NONE'"><small>関連人物</small>{{ characterDisplayName(currentEvent.event.ch2) }}</span>
                  <span v-if="currentEvent.event.ch3 && currentEvent.event.ch3 !== 'NONE'"><small>関連人物</small>{{ characterDisplayName(currentEvent.event.ch3) }}</span>
                </div>
                <p class="event-hint">台詞・叙述は既存リソースを使用しています。COと報告は公開発言であり、真偽を保証しません。</p>
              </div>
              <div v-else-if="finished" class="event-content settlement" role="status" lang="ja">
                <span class="event-number">THE FINAL VERDICT</span><AssetImage v-if="outcome" class="settlement-win-icon" :name="outcome.icon" /><h2 lang="ja">{{ outcome?.text || '勝敗不明（表示診断）' }}</h2><p>全イベントを確認しました。役職はサーバーの公開結果に従います。</p>
                <p v-if="reveal.diagnostic" class="visual-diagnostic" role="status" data-testid="reveal-diagnostic">表示診断：身分情報が不完全です。人物の役職画像を表示できません。</p>
              </div>
              <div v-else class="event-content decision" lang="ja">
                <span class="event-number">YOUR DECISION</span><h2>今日は、誰に投票しますか？</h2><p>プレイヤー一覧で指定候補にチェックして、投票を確定してください。</p><p>回避COがあれば発言を読んで再検討してください。共有者の回避後は候補を選び直せます。</p>
              </div>
            </GameStage>
            <div v-if="currentEvent" class="event-controls"><span>未確認 {{ pending.length }} 件</span><button data-testid="next-event" class="primary-button" :disabled="busy" @click="nextEvent">{{ pending.length === 1 ? '読み終えて対局を続ける' : '続きを読む' }} <span aria-hidden="true">→</span></button></div>
            <footer class="decision-footer" v-if="!finished">
              <div><span class="eyebrow">指定候補</span><p>{{ voteTargets.length ? voteTargets.map(name).join('、') : '未選択' }}</p></div>
              <button class="primary-button" data-testid="vote" :disabled="!canVote" @click="vote('DESIGNATED')">{{ busy ? '処理中…' : '投票を確定' }}</button>
              <small v-if="currentEvent">未確認のイベントを先に読み終えてください。</small><small v-else-if="uncertain">再同期してサーバーの状態を確認してください。</small>
            </footer>
          </section>

          <section class="players panel" aria-label="公开玩家列表" lang="zh-CN">
            <header class="panel-heading"><span class="eyebrow" lang="zh-CN">02 / 村中玩家</span><span>{{ players.length }} 人</span></header>
            <p id="player-list-help" class="list-help">名单较长时可在列表内滚动。按 Tab 聚焦列表后可用方向键；继续按 Tab 可逐个选择候补。</p>
            <div class="player-list" role="region" aria-label="公开玩家列表滚动区域" lang="zh-CN" tabindex="0" aria-describedby="player-list-help">
              <label v-for="player in players" :key="player.playerNumber" class="player-row" :data-player-number="player.playerNumber" :class="{ departed: !player.alive, selected: voteTargets.includes(player.playerNumber) }">
                <PlayerPortrait :player="player" :revealed="finished" :actual-role="finished ? reveal.roles.get(player.playerNumber) : null" />
                <span class="player-number">{{ String(player.playerNumber).padStart(2, '0') }}</span>
                <span class="player-identity" lang="ja"><strong lang="ja">{{ characterDisplayName(player.character) }}</strong><small>{{ finished ? '実際の役職 · ' + (roles[reveal.roles.get(player.playerNumber)] || '身分不明（表示診断）') : playerVisual(player).roleText + (playerVisual(player).orderText ? ' · ' + playerVisual(player).orderText : '') }}<span v-if="!player.alive"> · 第 {{ player.deathDay }} 日離場</span></small><small v-if="playerVisual(player).claimDiagnostic" class="visual-diagnostic">表示診断：CO図なし</small><small v-if="playerVisual(player).deathDiagnostic" class="visual-diagnostic">表示診断：死因図なし</small></span>
                <input v-if="!finished" type="checkbox" :aria-label="'指定 ' + characterDisplayName(player.character) + '（' + player.playerNumber + '号）'" :checked="voteTargets.includes(player.playerNumber)" :disabled="!canDecide || !legalTargets.includes(player.playerNumber)" @click.prevent="toggleTarget(player.playerNumber, !voteTargets.includes(player.playerNumber))">
                <span v-else class="player-state" lang="ja">{{ player.alive ? '生存' : '離場' }}</span>
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
          <p class="avoidance-help">全体操作は、この対象の個別設定も一括で上書きします。他の対象の設定は変更しません。</p>
          <div class="claim-player-list">
            <label v-for="target in divinationTargets" :key="target" class="claim-player">
              <input type="checkbox" :aria-label="'全員占い対象 ' + target + '番 ' + name(target)"
                :checked="selectedDivinationTargets.has(target)" :disabled="!canDecide"
                @click.prevent="setDivinationTarget(target, !selectedDivinationTargets.has(target))">
              <span>{{ name(target) }}</span>
            </label>
          </div>
        </section>

        <section v-if="guardTargets.length" class="panel claim-inquiry" aria-label="全員の護衛予約" lang="ja">
          <header class="panel-heading"><span>全員の護衛予約</span><span>当日の予約です。結果は確約されません。</span></header>
          <p class="avoidance-help">潜伏・公開COの護衛候補をまとめて設定／解除します。チェックは公開された予約のいずれかに対象が含まれることを示します。</p>
          <p class="avoidance-help">全体操作は、この対象の個別設定も一括で上書きします。他の対象の設定は変更しません。</p>
          <div class="claim-player-list">
            <label v-for="target in guardTargets" :key="target" class="claim-player">
              <input type="checkbox" :aria-label="'全員護衛対象 ' + target + '番 ' + name(target)"
                :checked="selectedGuardTargets.has(target)" :disabled="!canDecide"
                @click.prevent="setGuardTarget(target, !selectedGuardTargets.has(target))">
              <span>{{ name(target) }}</span>
            </label>
          </div>
        </section>

        <div v-if="claimedSkills.length" class="claimed-skills-layout">
          <section v-for="skill in claimedSkills" :key="skill.type" class="panel claim-inquiry" :aria-label="skill.label" lang="ja">
            <header class="panel-heading"><span>{{ skill.label }} · {{ skill.actors.length }}人</span><span>公開COの行動者だけを表示します。予約は結果を保証しません。</span></header>
            <p v-if="!skill.actors.length" class="avoidance-help">この技能の公開CO行動者はいません。</p>
            <template v-else>
              <p class="list-help" :id="'claimed-help-' + skill.type">行ごとに折りたためます。長い一覧は枠内でスクロールできます。Tabで枠に移動し、方向キーで閲覧できます。</p>
              <div class="claimed-actor-list" role="region" :aria-label="skill.label + ' 一覧'" tabindex="0"
                :aria-describedby="'claimed-help-' + skill.type">
                <details v-for="actor in skill.actors" :key="actor.playerNumber" class="claimed-actor" open>
                  <summary>{{ name(actor.playerNumber) }} · CO順 {{ actor.claimedRoleOrder }}<span>予約 {{ actor.targets.length }}件</span></summary>
                  <fieldset class="claim-player-list" :data-testid="'claimed-' + skill.type + '-' + actor.playerNumber" :disabled="!canDecide">
                    <legend class="visually-hidden">{{ name(actor.playerNumber) }} · CO順 {{ actor.claimedRoleOrder }}</legend>
                    <label v-for="target in skill.targets" :key="target" class="claim-player">
                      <input type="checkbox" :aria-label="skill.label + ' ' + actor.playerNumber + '番から' + target + '番へ'"
                        :checked="actor.targets.includes(target)"
                        @click.prevent="setClaimedTarget(skill.type, actor.playerNumber, target, !actor.targets.includes(target))">
                      <span>{{ name(target) }}</span>
                    </label>
                  </fieldset>
                </details>
              </div>
            </template>
          </section>
        </div>

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
              <input type="checkbox" :aria-label="player.playerNumber + '番 ' + characterDisplayName(player.character) + 'をCO確認対象に選択'"
                :checked="claimPlayerDraft.includes(player.playerNumber)" :disabled="!canDecide"
                @change="toggleClaimPlayer(player.playerNumber, $event.target.checked)">
              <span>{{ player.playerNumber }} · {{ characterDisplayName(player.character) }}</span>
            </label>
          </div>
          <div class="claim-actions"><button class="primary-button" :disabled="!canAskClaimPlayers" @click="askClaimPlayers">選択したプレイヤーのCOを聞く</button></div>
        </section>

        <PublicInformationPanel v-if="update.phase === 'DAY_DECISION' || finished"
          :information="update.publicInformation" :players="players" :finished="finished" :player-name="name" />

        <VoteHistoryPanel :history="update.voteHistory" :player-name="name" />

        <footer class="session-footer">
          <span>一時セッション・このタブを再読み込みして再開できます。</span>
          <button v-if="!confirmingExit" class="quiet-button" :disabled="busy" @click="confirmingExit = true">{{ finished ? '新しい対局' : '試遊を終了' }}</button>
          <div v-else class="exit-confirm"><span>解放すると、この対局は再開できません。</span><button class="quiet-button" :disabled="busy" @click="confirmingExit = false">対局を残す</button><button class="danger-button" :disabled="busy" @click="release().then(() => confirmingExit = false)">解放を確定</button></div>
        </footer>
      </template>
    </main>
    <HelpDialog v-if="helpPage" :initial-page="helpPage" @close="helpPage = ''" />
  </div>
</template>
