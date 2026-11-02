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
import DesktopStartScene from './DesktopStartScene.vue'
import './desktop-start.css'
import DesktopVoteWindow from './DesktopVoteWindow.vue'
import DesktopVoteReview from './DesktopVoteReview.vue'
import { publicSkillMarkers } from './desktopVoteText.js'
import { characters } from './characterDisplay.js'
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
  sessionId, cursor, update, busy, uncertain, notice, storageWarning, presentationHistory, completedVotePresentation,
  pending, currentEvent, canDecide, canVote, alternativeVotes, claimRoles, claimPlayers, claimPlayerDraft, canAskClaimPlayers,
  divinationTargets, selectedDivinationTargets, setDivinationTarget,
  guardTargets, selectedGuardTargets, setGuardTarget,
  setClaimedTarget,
  start, startReplacing, nextEvent, toggleTarget, vote, askClaimRole, toggleClaimPlayer, askClaimPlayers, setAvoidance, refresh, release,
} = useGuestGame()
const voteReview=ref(null)
const voteReviewVisual=ref(null)
const {phase:presentationPhase,artwork:preludeArtwork,text:dayStartText,completeText:dayStartReady,
  diagnostic:presentationDiagnostic,sceneReady,continueLocal,fail:failPrelude} = useNightDayPresentation(()=>({
  sessionId:sessionId.value,cursor:cursor.value,current:voteReview.value?null:currentEvent.value,history:presentationHistory.value,
  day:update.value?.view.gameDay,uncertain:uncertain.value,
}))
const reviewedVotes=new Set()
let reviewSession=''
watch([sessionId,completedVotePresentation,update,currentEvent,busy,uncertain],()=>{
  if(reviewSession!==sessionId.value){reviewSession=sessionId.value;reviewedVotes.clear();voteReview.value=null;voteReviewVisual.value=null}
  if(busy.value||uncertain.value||!update.value)return
  const explicit=completedVotePresentation.value
  const vote=explicit?.sessionId===sessionId.value && explicit.vote.day===update.value.voteHistory.at(-1)?.day?explicit.vote:
    currentEvent.value?.stage==='EXECUTION'?update.value.voteHistory.at(-1):null
  if(!vote||reviewedVotes.has(vote.day))return
  if(!update.value.voteHistory.some(v=>v.day===vote.day))return
  voteReviewVisual.value=explicit?.vote===vote?explicit.before:null
  voteReview.value=vote
},{flush:'sync'})
function continueVoteReview(){if(busy.value||uncertain.value||!voteReview.value)return;reviewedVotes.add(voteReview.value.day);voteReview.value=null;voteReviewVisual.value=null}
const decisionWindow = computed(()=>!voteReview.value && update.value?.phase==='DAY_DECISION' && !currentEvent.value && presentationPhase.value==='event')
const finished = computed(() => update.value?.phase === 'FINISHED' && !currentEvent.value && !voteReview.value)
const artwork = computed(() => preludeArtwork.value || sceneArtwork(presentationHistory.value, cursor.value, currentEvent.value, update.value?.view.result, finished.value))
const audioControl = ref(null)
const reading = useEventReading(()=>({key:sessionId.value+':'+cursor.value,
  enabled:presentationPhase.value==='event' && !voteReview.value,current:currentEvent.value,artwork:artwork.value,
  blocked:busy.value||uncertain.value}))
const {text:readingText,fullText:readingFullText,complete:readingComplete,visible:readingVisible,
  step:readingStep,controlledFrame,diagnostic:readingDiagnostic,language:readingLanguage}=reading
const pendingContinueFocus=ref(false)
function rememberContinueFocus() {
  if(document.activeElement?.closest('.message-surface'))pendingContinueFocus.value=true
}
watch([pendingContinueFocus,cursor,presentationPhase,readingVisible,readingComplete,readingStep,dayStartReady,busy,uncertain],()=>{
  if(!pendingContinueFocus.value||busy.value||uncertain.value)return
  // Restore only focus lost with the old frame; do not steal it from a menu,
  // modal, resync control or any other place the user deliberately selected.
  if(document.activeElement!==document.body&&!document.activeElement?.closest('.message-surface')){
    pendingContinueFocus.value=false;return
  }
  const button=document.querySelector('[data-testid="game-stage"] .message-surface:not([aria-disabled="true"])')
  if(button){button.focus();pendingContinueFocus.value=false}
},{flush:'post'})
function advanceReading() {
  if(busy.value||uncertain.value)return
  if(['day-text','error'].includes(presentationPhase.value)){
    if(dayStartReady.value)audioControl.value?.click()
    return advanceDayReading()
  }
  if(reading.complete.value)audioControl.value?.click()
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
  if (button && !button.disabled && !button.hasAttribute('data-no-click-sound')) audioControl.value?.click()
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
async function switchDesktopMusic(){if(!audioControl.value?.status?.enabled)await audioControl.value?.enable();await audioControl.value?.nextTrack()}
const menuScene=ref('title')
const replayEntry=ref(null)
watch(update,(value,previous)=>{
  if(value&&!previous)menuScene.value='game'
  if(!value&&previous)menuScene.value='title'
})
const menuMusic=computed(()=>sessionId.value&&!update.value?null:helpPage.value?'INFO':menuScene.value==='replay'?'REPLAY_BROWSER':menuScene.value!=='game'?'START_MENU':null)
async function openGameMenu(){
  if(busy.value||uncertain.value)return
  menuScene.value='title';confirmingExit.value=false
  await nextTick();document.querySelector('[data-testid=title-start]')?.focus({preventScroll:true})
}
async function navigateMenu(target){
  if(busy.value||(uncertain.value&&sessionId.value))return
  if(target==='continue'||target==='collections')return // Desktop source listeners are intentionally empty.
  if(target==='start')menuScene.value='select'
  else if(target==='records')recordPage.value=true
  else if(target==='help')helpPage.value='help'
  else if(target==='replay'){menuScene.value='replay';await nextTick();replayEntry.value?.open()}
  else {replayEntry.value?.close();menuScene.value='title'}
}
async function startConfiguration(value){
  configuration.value=value
  await startReplacing(value)
}

const players = computed(() => update.value?.view.players || [])
const skillMarkers=computed(()=>publicSkillMarkers(update.value?.publicInformation,players.value,update.value?.view.gameDay))
function shortName(number){const player=players.value.find(p=>p.playerNumber===number);return characters[player?.character]?.shortName || String(number)}
const voteReviewPlayers=computed(()=>voteReviewVisual.value?.view.players || players.value)
const voteReviewMarkers=computed(()=>publicSkillMarkers(voteReviewVisual.value?.information || update.value?.publicInformation,voteReviewPlayers.value,voteReview.value?.day))
const audioUpdate=computed(()=>voteReviewVisual.value?{...update.value,view:voteReviewVisual.value.view}:update.value)
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
      <BgmControl :menu-music="menuMusic" :presentation-phase="presentationPhase" :replaying="replayPlaying" ref="audioControl" :cursor="cursor" :session-id="sessionId" :update="audioUpdate" :current-event="voteReview ? null : currentEvent" :finished="finished" :uncertain="uncertain" :can-decide="canDecide" :vote-reviewing="Boolean(voteReview)" />
      <div class="side-footer"><span class="status-dot"></span>ローカル試遊<p>セッションは一時保存です。通信のない状態が30分続くか、開村から24時間経過すると無効になります。サービス再起動・解放でも無効になります。<br>画像・音声に対応。終局Replayはこのブラウザーに保存できます。</p></div>
    </aside>

    <main>
      <header class="topbar"><span>電脳村リメイク / Web試遊版</span><span class="build-tag">デスクトップ表示 · 050（調整中）</span></header>
      <div v-if="notice" class="notice" role="alert" lang="zh-CN">{{ notice }}</div>
      <div v-if="storageWarning" class="notice" role="status" lang="zh-CN">{{ storageWarning }}</div>

      <div id="replay-home"></div>
      <Teleport :to="menuScene==='replay' ? '#desktop-replay-slot' : '#replay-home'" defer><ReplayEntry ref="replayEntry" :session-id="sessionId" :cursor="cursor" :eligible="finished && !busy && !uncertain" :audio-controls="replayAudio" @playing="replayPlaying=$event" /></Teleport>
      <RecordPage v-if="recordPage" @close="recordPage=false" />
      <RecordSettlement :session-id="sessionId" :update="update" :eligible="finished && !busy && !uncertain && !replayPlaying" />

      <section v-show="menuScene!=='game' && (!sessionId || update)" class="journal panel desktop-menu-journal" aria-label="村落紀事のメニュー">
        <header class="panel-heading"><span class="eyebrow">01 / 村落纪事</span><span class="pill">{{ menuScene==='select'?'配役選択':menuScene==='replay'?'録画':'タイトル' }}</span></header>
        <DesktopStartScene v-show="menuScene!=='replay'" :scene="menuScene==='select' ? 'select' : 'title'" :blocked="busy||(uncertain&&Boolean(sessionId))" @navigate="navigateMenu" @start="startConfiguration" />
        <section v-show="menuScene==='replay'" class="desktop-start-surface" lang="ja" data-testid="desktop-replay">
          <AssetImage class="desktop-start-background" name="images/title_base_resized.png" />
          <div id="desktop-replay-slot" class="desktop-library-content"></div>
          <button class="desktop-art-button desktop-setup-return" aria-label="タイトルに戻る" data-testid="replay-return" @click="navigateMenu('title')"><AssetImage name="images/PVBtitile.png" /></button>
        </section>
        <button v-if="update" class="quiet-button" data-testid="resume-current-game" :disabled="busy||uncertain" @click="menuScene='game'">{{finished?'現在の結末に戻る':'現在の対局に戻る'}}</button>
      </section>

      <section v-if="!update && sessionId" class="restoring" aria-live="polite">
        <span class="eyebrow">RECONNECTING</span><h1>村へ戻りましょう。</h1>
        <p>{{ busy ? 'セッションと未確認の発言を読み込んでいます…' : '保存済みのセッションがあります。ローカルサーバーへの再接続を待っています。' }}</p>
        <button class="primary-button" :disabled="busy" @click="refresh">再同期</button>
        <button class="quiet-button" :disabled="busy" @click="release">セッションを解放</button>
      </section>

      <div v-if="update" v-show="menuScene==='game'">
        <section class="game-heading">
          <div><span class="eyebrow">THE VILLAGE JOURNAL</span><h1>{{ configurationName(update.view.configuration) }}村 <span>第 {{ update.view.gameDay }} 日</span></h1></div>
          <div class="population"><strong>{{ update.view.aliveCount }}<small> 生存</small></strong><span>{{ update.view.deathCount }} 人離場</span></div>
        </section>

        <div class="game-layout">
          <section class="journal panel" aria-labelledby="journal-label">
            <span id="journal-label" class="visually-hidden" lang="zh-CN">事件展示</span>
            <header class="panel-heading"><span class="eyebrow" lang="zh-CN">01 / 村落纪事</span><span class="pill" :lang="currentEvent ? 'zh-CN' : 'ja'">{{ currentEvent ? stages[currentEvent.stage] : finished ? '対局終了' : '昼の指示' }}</span></header>
            <!-- Each event/display mode gets a fresh scroller; old native keyboard animation cannot follow it. -->
            <GameStage :key="(voteReview ? 'vote-review-' : currentEvent ? 'event-' : finished ? 'finished-' : 'decision-') + cursor + ':' + presentationPhase" :data-presentation-phase="presentationPhase" :reading="!voteReview && !finished && !decisionWindow && !['night','dawn'].includes(presentationPhase) && (presentationPhase!=='event' || !currentEvent || readingVisible)" :message="Boolean(currentEvent) || ['day-text','error'].includes(presentationPhase)" :speaker="presentationPhase==='event' && currentEvent?.event?.eventname!=='wsw' ? characterDisplayName(currentEvent?.event?.ch1) : ''" :frame-index="controlledFrame" :review-allowed="readingStep==='consequence' && readingComplete" :read-test-id="['day-text','error'].includes(presentationPhase) ? 'day-start-continue' : readingStep==='primary' && controlledFrame===0 && readingComplete ? 'execution-continue' : 'next-event'" :read-disabled="busy || uncertain || (presentationPhase==='event' && !readingVisible)" :data-reading-step="readingStep" :data-reading-complete="readingComplete" :data-reading-length="Array.from(readingFullText).length" :data-reading-blocking="!voteReview && Boolean(currentEvent) && presentationPhase==='event' && !busy && !uncertain && (!readingComplete || (readingStep==='primary' && controlledFrame===0))" @read-continue="advanceReading" :event-key="sessionId + ':' + cursor" @art-ready="artReady" @art-failed="stageFailed" @consequence="audioControl?.transition($event)" :event="voteReview ? null : currentEvent?.event" :stage="currentEvent?.stage" :artwork="voteReview ? {mode:'deferred'} : artwork">
              <template #scene-overlay>
                <DesktopVoteReview v-if="voteReview" :vote="voteReview" :players="voteReviewPlayers" :markers="voteReviewMarkers" :day="voteReview.day" :alive="voteReviewVisual?.view.aliveCount ?? update.view.aliveCount" :dead="voteReviewVisual?.view.deathCount ?? update.view.deathCount" :player-name="shortName" :blocked="busy||uncertain" @continue="continueVoteReview" @menu="openGameMenu" @music="switchDesktopMusic" />
                <DesktopVoteWindow v-if="decisionWindow" :update="update" :players="players" :bindings="orderBindings" :player-name="shortName" :vote-targets="voteTargets" :legal-targets="legalTargets" :toggle-target="toggleTarget" :markers="skillMarkers" :day="update.view.gameDay" :alive="update.view.aliveCount" :dead="update.view.deathCount" :blocked="busy" @menu="openGameMenu" @music="switchDesktopMusic">
                  <template #avoidance>            <button class="desktop-art-button" role="switch" aria-label="投票回避CO"
              :aria-checked="update.selections.avoidanceEnabled" :disabled="!canDecide"
              @click="setAvoidance(!update.selections.avoidanceEnabled)"><AssetImage :name="update.selections.avoidanceEnabled ? 'images/关闭回避.png' : 'images/开启回避.png'" /><span class="visually-hidden">{{ update.selections.avoidanceEnabled ? '回避あり' : '回避なし' }}</span></button></template>

                </DesktopVoteWindow>
                <SettlementBoard :blocked="busy||uncertain" v-if="finished" :players="players" :reveal="reveal" :result="update.view.result" @menu="openGameMenu" />
              </template>
              <div v-if="presentationPhase==='day-text'" class="event-content" lang="ja" role="status" data-testid="day-start-text">{{ dayStartText }}</div>
              <div v-else-if="presentationPhase==='error'" class="event-content" lang="ja" role="status">場面の画像を表示できません。現在の出来事へ進めます。</div>
              <div v-else-if="presentationPhase==='event' && currentEvent" class="event-content" role="region" aria-labelledby="current-event-label" lang="ja">
                <span id="current-event-label" class="visually-hidden" lang="zh-CN">当前事件</span>
                <EventDialogue v-if="readingStep!=='consequence'" :event="currentEvent.event" :visible-text="readingText" aria-hidden="true" />
                <p v-else class="event-consequence event-dialogue" lang="ja" aria-hidden="true" data-testid="event-consequence">{{ readingText }}</p>
                <span class="visually-hidden" :lang="readingLanguage" data-testid="accessible-dialogue">{{ readingFullText }}</span>
              </div>
              <div v-else-if="presentationPhase==='event'" class="event-content decision" lang="ja">
                <span class="event-number">YOUR DECISION</span><h2>今日は、誰に投票しますか？</h2><p>プレイヤー一覧で指定候補にチェックして、投票を確定してください。</p><p>回避COがあれば発言を読んで再検討してください。共有者の回避後は候補を選び直せます。</p>
              </div>

            </GameStage>
          </section>

          <section v-if="!voteReview && !decisionWindow" class="players panel" aria-label="公开玩家列表" lang="zh-CN">
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

        <button v-if="!voteReview && currentEvent && readingStep==='waiting-image'" class="quiet-button image-reading-fallback" data-testid="read-without-image" :disabled="busy || uncertain" @click="reading.imageFailed()">画像なしで文章を読む</button>

        <details v-if="currentEvent" class="display-details" lang="ja">
              <summary>表示情報・読み方</summary>
              <p>未確認 {{ pending.length }} 件 · {{ currentEvent.stage }} · イベント {{ cursor + 1 }}</p>
              <p>台詞・叙述は既存リソースを使用しています。COと報告は公開発言であり、真偽を保証しません。</p>
              <p>文章の表示中に対話枠を押すと全文を表示します。読み終わってからもう一度押すと進みます。</p>
              <p v-if="readingDiagnostic" role="status">表示診断：{{ readingDiagnostic }}。画像なしでも文章を読めます。</p>
        </details>
        <small v-if="presentationDiagnostic" class="visual-diagnostic" role="status" lang="ja">場面表示診断：{{ presentationDiagnostic }}</small>

        <PublicInformationPanel v-if="!voteReview && !decisionWindow && (update.phase === 'DAY_DECISION' || finished)" :information="update.publicInformation" :players="players" :finished="finished" :player-name="name" />
        <VoteHistoryPanel v-if="!voteReview && !decisionWindow" :history="update.voteHistory" :player-name="name" />

        <footer class="session-footer">
          <span>一時セッション・このタブを再読み込みして再開できます。</span>
          <button v-if="finished" class="quiet-button" :disabled="busy||uncertain" @click="openGameMenu">タイトルに戻る</button>
          <button v-else-if="!confirmingExit" class="quiet-button" :disabled="busy" @click="confirmingExit = true">試遊を終了</button>
          <div v-else-if="!finished" class="exit-confirm"><span>解放すると、この対局は再開できません。</span><button class="quiet-button" :disabled="busy" @click="confirmingExit = false">対局を残す</button><button class="danger-button" :disabled="busy" @click="release().then(() => confirmingExit = false)">解放を確定</button></div>
        </footer>
      </div>
    </main>
    <HelpDialog v-if="helpPage" :initial-page="helpPage" @close="helpPage = ''" />
  </div>
</template>
