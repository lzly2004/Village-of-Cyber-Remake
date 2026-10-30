<script setup>
import { computed, ref, watch, defineAsyncComponent, h, nextTick } from 'vue'
import { useGuestGame } from './useGuestGame'
import PublicInformationPanel from './PublicInformationPanel.vue'
import VoteHistoryPanel from './VoteHistoryPanel.vue'
import EventDialogue from './EventDialogue.vue'
import GameStage from './GameStage.vue'
import BgmControl from './BgmControl.vue'
import PlayerPortrait from './PlayerPortrait.vue'
import ReplayEntry from './ReplayEntry.vue'
import RecordSettlement from './RecordSettlement.vue'
import SettlementBoard from './SettlementBoard.vue'
import DesktopVoteWindow from './DesktopVoteWindow.vue'
import GameOrders from './GameOrders.vue'
import RecordPage from './RecordPage.vue'
import AssetImage from './AssetImage.vue'
import { characterDisplayName } from './characterDisplay.js'
import { playerVisual } from './playerVisual.js'
import { sceneArtwork } from './sceneArtwork.js'
import { settlementReveal } from './settlementVisual.js'
import { useEventReading } from './useEventReading.js'
import { useNightDayPresentation } from './useNightDayPresentation.js'
import { configurations, configurationName, roles, claimRoleNames, stages } from './labels'

const {
  sessionId, cursor, update, busy, uncertain, notice, storageWarning, presentationHistory,
  pending, currentEvent, canDecide, canVote, alternativeVotes, claimRoles, claimPlayers, claimPlayerDraft, canAskClaimPlayers,
  divinationTargets, selectedDivinationTargets, setDivinationTarget,
  guardTargets, selectedGuardTargets, setGuardTarget,
  setClaimedTarget,
  start, nextEvent, toggleTarget, vote, askClaimRole, toggleClaimPlayer, askClaimPlayers, setAvoidance, refresh, release,
} = useGuestGame()
const {phase:presentationPhase,artwork:preludeArtwork,text:dayStartText,completeText:dayStartReady,
  diagnostic:presentationDiagnostic,sceneReady,continueLocal,fail:failPrelude} = useNightDayPresentation(()=>({
  sessionId:sessionId.value,cursor:cursor.value,current:currentEvent.value,history:presentationHistory.value,
  day:update.value?.view.gameDay,uncertain:uncertain.value,
}))
const decisionWindow = computed(()=>update.value?.phase==='DAY_DECISION' && !currentEvent.value && presentationPhase.value==='event')
const finished = computed(() => update.value?.phase === 'FINISHED' && !currentEvent.value)
const artwork = computed(() => preludeArtwork.value || sceneArtwork(presentationHistory.value, cursor.value, currentEvent.value, update.value?.view.result, finished.value))
const audioControl = ref(null)
const reading = useEventReading(()=>({key:sessionId.value+':'+cursor.value,
  enabled:presentationPhase.value==='event',current:currentEvent.value,artwork:artwork.value,
  blocked:busy.value||uncertain.value}))
const {text:readingText,fullText:readingFullText,complete:readingComplete,visible:readingVisible,
  step:readingStep,controlledFrame,diagnostic:readingDiagnostic,language:readingLanguage}=reading
const pendingContinueFocus=ref(false)
function rememberContinueFocus() {
  if(document.activeElement?.closest('.message-actions'))pendingContinueFocus.value=true
}
watch([pendingContinueFocus,cursor,presentationPhase,readingVisible,readingComplete,readingStep,dayStartReady,busy,uncertain],()=>{
  if(!pendingContinueFocus.value||busy.value||uncertain.value)return
  // Restore only focus lost with the old frame; do not steal it from a menu,
  // modal, resync control or any other place the user deliberately selected.
  if(document.activeElement!==document.body&&!document.activeElement?.closest('.message-actions')){
    pendingContinueFocus.value=false;return
  }
  const button=document.querySelector('[data-testid="game-stage"] .message-actions button:not(:disabled)')
  if(button){button.focus();pendingContinueFocus.value=false}
},{flush:'post'})
function advanceReading() {
  if(busy.value||uncertain.value)return
  rememberContinueFocus()
  if(reading.canConfirm.value)nextEvent()
  else reading.advanceLocal()
}
function advanceDayReading(){rememberContinueFocus();continueLocal()}
function stageFailed() { failPrelude();if(presentationPhase.value==='event')reading.imageFailed() }
function artReady() {
  if(presentationPhase.value==='event'){reading.ready();audioControl.value?.presented(sessionId.value+':'+cursor.value)}
  else if(sceneReady())audioControl.value?.presentation(presentationPhase.value,sessionId.value+':'+cursor.value)
}
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
const recordPage = ref(false)
const configuration = ref('jianyi')
const confirmingExit = ref(false)
async function openGameMenu(){
  confirmingExit.value=true
  await nextTick()
  const menu=document.querySelector('.exit-confirm')
  menu?.scrollIntoView({block:'nearest'});menu?.querySelector('button')?.focus({preventScroll:true})
}
const players = computed(() => update.value?.view.players || [])
const claimCandidates = computed(() => players.value.filter(player => claimPlayers.value.includes(player.playerNumber)))
const voteTargets = computed(() => update.value?.selections.voteTargets || [])
const legalTargets = computed(() =>
  update.value?.legalTargetSelections.find(action => action.type === 'VOTE')?.targets || [])
const reveal = computed(() => settlementReveal(finished.value, players.value, finished.value ? update.value.revealedRoles : null))
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
const orderBindings=computed(()=>({update:update.value,alternativeVotes:alternativeVotes.value,canDecide:canDecide.value,vote,divinationTargets:divinationTargets.value,selectedDivinationTargets:selectedDivinationTargets.value,setDivinationTarget,guardTargets:guardTargets.value,selectedGuardTargets:selectedGuardTargets.value,setGuardTarget,claimedSkills:claimedSkills.value,setClaimedTarget,claimRoles:claimRoles.value,askClaimRole,claimRoleNames,claimPlayers:claimPlayers.value,claimCandidates:claimCandidates.value,claimPlayerDraft:claimPlayerDraft.value,toggleClaimPlayer,characterDisplayName,canAskClaimPlayers:canAskClaimPlayers.value,askClaimPlayers,name,setAvoidance}))
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
      <nav class="help-menu" aria-label="メニュー"><button class="quiet-button" @click="helpPage = 'help'">遊び方・ヘルプ</button><button class="quiet-button" @click="helpPage = 'configurations'">配役説明</button><button class="quiet-button" @click="recordPage = true">戦績を見る</button></nav>
      <BgmControl :presentation-phase="presentationPhase" :replaying="replayPlaying" ref="audioControl" :cursor="cursor" :session-id="sessionId" :update="update" :current-event="currentEvent" :finished="finished" :uncertain="uncertain" :can-decide="canDecide" />
      <div class="side-footer"><span class="status-dot"></span>ローカル試遊<p>セッションは一時保存です。通信のない状態が30分続くか、開村から24時間経過すると無効になります。サービス再起動・解放でも無効になります。<br>画像・音声に対応。終局Replayはこのブラウザーに保存できます。</p></div>
    </aside>

    <main>
      <header class="topbar"><span>電脳村リメイク / Web試遊版</span><span class="build-tag">デスクトップ表示 · 050（調整中）</span></header>
      <div v-if="notice" class="notice" role="alert" lang="zh-CN">{{ notice }}</div>
      <div v-if="storageWarning" class="notice" role="status" lang="zh-CN">{{ storageWarning }}</div>

      <ReplayEntry :session-id="sessionId" :cursor="cursor" :eligible="finished && !busy && !uncertain" :audio-controls="replayAudio" @playing="replayPlaying=$event" />
      <RecordPage v-if="recordPage" @close="recordPage=false" />
      <RecordSettlement :session-id="sessionId" :update="update" :eligible="finished && !busy && !uncertain && !replayPlaying" />

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
        <div class="start-row"><button class="primary-button" :disabled="busy" @click="start(configuration)">{{ busy ? '開村中…' : '対局を始める' }} <span aria-hidden="true">↗</span></button><span>ログイン不要・戦績はこのブラウザーに保存します</span></div>
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
            <GameStage :key="(currentEvent ? 'event-' : finished ? 'finished-' : 'decision-') + cursor + ':' + presentationPhase" :data-presentation-phase="presentationPhase" :reading="!finished && !decisionWindow && !['night','dawn'].includes(presentationPhase) && (presentationPhase!=='event' || !currentEvent || readingVisible)" :message="Boolean(currentEvent) || ['day-text','error'].includes(presentationPhase)" :speaker="presentationPhase==='event' && currentEvent?.event?.eventname!=='wsw' ? characterDisplayName(currentEvent?.event?.ch1) : ''" :frame-index="controlledFrame" :review-allowed="readingStep==='consequence' && readingComplete" :data-reading-step="readingStep" :data-reading-complete="readingComplete" :data-reading-length="Array.from(readingFullText).length" :data-reading-blocking="Boolean(currentEvent) && presentationPhase==='event' && !busy && !uncertain && (!readingComplete || (readingStep==='primary' && controlledFrame===0))" @read-continue="advanceReading" :event-key="sessionId + ':' + cursor" @art-ready="artReady" @art-failed="stageFailed" @consequence="audioControl?.transition($event)" :event="currentEvent?.event" :stage="currentEvent?.stage" :artwork="artwork">
              <template #scene-overlay>
                <DesktopVoteWindow v-if="decisionWindow" :day="update.view.gameDay" :alive="update.view.aliveCount" :dead="update.view.deathCount" :blocked="busy" @menu="openGameMenu">
                  <template #players>          <section class="players panel" aria-label="公开玩家列表" lang="zh-CN">
            <header class="panel-heading"><span class="eyebrow" lang="zh-CN">02 / 村中玩家</span><span>{{ players.length }} 人</span></header>
            <p id="player-list-help" class="list-help">人物は上下2段です。左右にスクロールできます。Tabで各候補へ移動できます。</p>
            <div class="player-list" role="region" aria-label="公开玩家列表滚动区域" lang="zh-CN" tabindex="0" aria-describedby="player-list-help">
              <label v-for="(player,index) in players" :key="player.playerNumber" class="player-row" :style="{gridColumn:index%Math.ceil(players.length/2)+1,gridRow:index<Math.ceil(players.length/2)?1:2}" :data-player-number="player.playerNumber" :title="characterDisplayName(player.character) + ' · ' + playerVisual(player).roleText + ' ' + playerVisual(player).orderText" :class="{ departed: !player.alive, selected: voteTargets.includes(player.playerNumber) }">
                <PlayerPortrait :player="player" :revealed="finished" :actual-role="finished ? reveal.roles.get(player.playerNumber) : null" />
                <span class="player-number">{{ String(player.playerNumber).padStart(2, '0') }}</span>
                <span class="player-identity" lang="ja"><strong lang="ja">{{ characterDisplayName(player.character) }}</strong><small>{{ finished ? '実際の役職 · ' + (roles[reveal.roles.get(player.playerNumber)] || '身分不明（表示診断）') : playerVisual(player).roleText + (playerVisual(player).orderText ? ' · ' + playerVisual(player).orderText : '') }}<span v-if="!player.alive"> · 第 {{ player.deathDay }} 日離場</span></small><small v-if="playerVisual(player).claimDiagnostic" class="visual-diagnostic">表示診断：CO図なし</small><small v-if="playerVisual(player).deathDiagnostic" class="visual-diagnostic">表示診断：死因図なし</small></span>
                <input v-if="!finished" type="checkbox" :aria-label="'指定 ' + characterDisplayName(player.character) + '（' + player.playerNumber + '号）'" :checked="voteTargets.includes(player.playerNumber)" :disabled="!canDecide || !legalTargets.includes(player.playerNumber)" @click.prevent="toggleTarget(player.playerNumber, !voteTargets.includes(player.playerNumber))">
                <span v-else class="player-state" lang="ja">{{ player.alive ? '生存' : '離場' }}</span>
              </label>
            </div>
          </section></template>
                  <template #information><PublicInformationPanel :information="update.publicInformation" :players="players" :finished="false" :player-name="name" /></template>
                  <template #orders><GameOrders v-bind="orderBindings" :show-avoidance="false" /></template>
                  <template #history><VoteHistoryPanel :history="update.voteHistory" :player-name="name" /></template>
                  <template #avoidance>            <button class="desktop-art-button" role="switch" aria-label="投票回避CO"
              :aria-checked="update.selections.avoidanceEnabled" :disabled="!canDecide"
              @click="setAvoidance(!update.selections.avoidanceEnabled)"><AssetImage :name="update.selections.avoidanceEnabled ? 'images/关闭回避.png' : 'images/开启回避.png'" /><span class="visually-hidden">{{ update.selections.avoidanceEnabled ? '回避あり' : '回避なし' }}</span></button></template>
                  <template #vote><button class="desktop-art-button" data-testid="vote" :disabled="!canVote" @click="vote('DESIGNATED')"><AssetImage name="images/goTohyo.png" /><span class="visually-hidden">{{ busy ? '処理中…' : '投票を確定' }}</span></button></template>
                </DesktopVoteWindow>
                <SettlementBoard v-if="finished" :players="players" :reveal="reveal" :result="update.view.result" />
              </template>
              <div v-if="presentationPhase==='day-text'" class="event-content" lang="ja" role="status" data-testid="day-start-text">{{ dayStartText }}</div>
              <div v-else-if="presentationPhase==='error'" class="event-content" lang="ja" role="status">夜明けの画像を表示できません。現在の出来事へ進めます。</div>
              <div v-else-if="presentationPhase==='event' && currentEvent" class="event-content" role="region" aria-labelledby="current-event-label" lang="ja">
                <span id="current-event-label" class="visually-hidden" lang="zh-CN">当前事件</span>
                <EventDialogue v-if="readingStep!=='consequence'" :event="currentEvent.event" :visible-text="readingText" aria-hidden="true" />
                <p v-else class="event-consequence event-dialogue" lang="ja" aria-hidden="true" data-testid="event-consequence">{{ readingText }}</p>
                <span class="visually-hidden" :lang="readingLanguage" data-testid="accessible-dialogue">{{ readingFullText }}</span>
              </div>
              <div v-else-if="presentationPhase==='event'" class="event-content decision" lang="ja">
                <span class="event-number">YOUR DECISION</span><h2>今日は、誰に投票しますか？</h2><p>プレイヤー一覧で指定候補にチェックして、投票を確定してください。</p><p>回避COがあれば発言を読んで再検討してください。共有者の回避後は候補を選び直せます。</p>
              </div>
              <template #controls>
                <button v-if="['day-text','error'].includes(presentationPhase)" data-testid="day-start-continue" class="message-continue" :disabled="!dayStartReady" @click.stop="advanceDayReading">続きを読む</button>
                <button v-else-if="currentEvent && presentationPhase==='event'" :data-testid="readingStep==='primary' && controlledFrame===0 && readingComplete ? 'execution-continue' : 'next-event'" class="message-continue" :disabled="busy || uncertain || !readingVisible" @click.stop="advanceReading">{{ readingVisible ? '続きを読む' : '場面表示中…' }}</button>
              </template>
            </GameStage>
            <button v-if="currentEvent && readingStep==='waiting-image'" class="quiet-button image-reading-fallback" data-testid="read-without-image" :disabled="busy || uncertain" @click="reading.imageFailed()">画像なしで文章を読む</button>
            <details v-if="currentEvent" class="display-details" lang="ja">
              <summary>表示情報・読み方</summary>
              <p>未確認 {{ pending.length }} 件 · {{ currentEvent.stage }} · イベント {{ cursor + 1 }}</p>
              <p>台詞・叙述は既存リソースを使用しています。COと報告は公開発言であり、真偽を保証しません。</p>
              <p>文章の表示中に続きを読むを押すと全文を表示します。読み終わってからもう一度押すと進みます。</p>
              <p v-if="readingDiagnostic" role="status">表示診断：{{ readingDiagnostic }}。画像なしでも文章を読めます。</p>
            </details>
            <small v-if="presentationDiagnostic" class="visual-diagnostic" role="status" lang="ja">場面表示診断：{{ presentationDiagnostic }}</small>
            <footer class="decision-footer" v-if="!finished && !decisionWindow">
              <div><span class="eyebrow">指定候補</span><p>{{ voteTargets.length ? voteTargets.map(name).join('、') : '未選択' }}</p></div>
              <button class="primary-button" data-testid="vote" :disabled="!canVote" @click="vote('DESIGNATED')">{{ busy ? '処理中…' : '投票を確定' }}</button>
              <small v-if="currentEvent">未確認のイベントを先に読み終えてください。</small><small v-else-if="uncertain">再同期してサーバーの状態を確認してください。</small>
            </footer>
          </section>

          <section v-if="!decisionWindow" class="players panel" aria-label="公开玩家列表" lang="zh-CN">
            <header class="panel-heading"><span class="eyebrow" lang="zh-CN">02 / 村中玩家</span><span>{{ players.length }} 人</span></header>
            <p id="player-list-help" class="list-help">名单较长时可在列表内滚动。按 Tab 聚焦列表后可用方向键；继续按 Tab 可逐个选择候补。</p>
            <div class="player-list" role="region" aria-label="公开玩家列表滚动区域" lang="zh-CN" tabindex="0" aria-describedby="player-list-help">
              <label v-for="player in players" :key="player.playerNumber" class="player-row" :data-player-number="player.playerNumber" :title="characterDisplayName(player.character) + ' · ' + playerVisual(player).roleText + ' ' + playerVisual(player).orderText" :class="{ departed: !player.alive, selected: voteTargets.includes(player.playerNumber) }">
                <PlayerPortrait :player="player" :revealed="finished" :actual-role="finished ? reveal.roles.get(player.playerNumber) : null" />
                <span class="player-number">{{ String(player.playerNumber).padStart(2, '0') }}</span>
                <span class="player-identity" lang="ja"><strong lang="ja">{{ characterDisplayName(player.character) }}</strong><small>{{ finished ? '実際の役職 · ' + (roles[reveal.roles.get(player.playerNumber)] || '身分不明（表示診断）') : playerVisual(player).roleText + (playerVisual(player).orderText ? ' · ' + playerVisual(player).orderText : '') }}<span v-if="!player.alive"> · 第 {{ player.deathDay }} 日離場</span></small><small v-if="playerVisual(player).claimDiagnostic" class="visual-diagnostic">表示診断：CO図なし</small><small v-if="playerVisual(player).deathDiagnostic" class="visual-diagnostic">表示診断：死因図なし</small></span>
                <input v-if="!finished" type="checkbox" :aria-label="'指定 ' + characterDisplayName(player.character) + '（' + player.playerNumber + '号）'" :checked="voteTargets.includes(player.playerNumber)" :disabled="!canDecide || !legalTargets.includes(player.playerNumber)" @click.prevent="toggleTarget(player.playerNumber, !voteTargets.includes(player.playerNumber))">
                <span v-else class="player-state" lang="ja">{{ player.alive ? '生存' : '離場' }}</span>
              </label>
            </div>
          </section>
        </div>

        <GameOrders v-if="!decisionWindow" v-bind="orderBindings" :show-avoidance="true" />
        <PublicInformationPanel v-if="!decisionWindow && (update.phase === 'DAY_DECISION' || finished)" :information="update.publicInformation" :players="players" :finished="finished" :player-name="name" />
        <VoteHistoryPanel v-if="!decisionWindow" :history="update.voteHistory" :player-name="name" />

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
