<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import { configurationName } from './labels.js'
import { createReplayStore } from './replayStore.js'
import { readCompletedReplay } from './replayApi.js'
import ReplayPlayer from './ReplayPlayer.vue'
import DesktopReplayPlayer from './DesktopReplayPlayer.vue'
import DesktopReplayExchange from './DesktopReplayExchange.vue'
import AssetImage from './AssetImage.vue'
import './desktop-replay.css'
import { readReplayFile, exportReplayFile, downloadReplayBytes } from './replayFiles.js'
const props = defineProps({ sessionId:String, cursor:Number, eligible:Boolean,initialAction:String,initialIntent:Object,audioControls:Object,desktop:Boolean })
const emit=defineEmits(['playing','close'])
const playing=ref(null), importFile=ref(null), candidates=ref([]), chosen=ref([])
const store = createReplayStore()
const opened = ref(false), working = ref(false), entries = ref([]), message = ref(''), problem = ref('')
const query = ref(''), configuration = ref(''), result = ref(''), removing = ref(''), selected = ref('')
const slots=ref(Array(9).fill(null)),slotPage=ref(0),slotIntent=ref(null)
const slotDialog=ref(null)
let playbackOpener
function playEntry(entry,event){playbackOpener=event.currentTarget;playing.value=JSON.parse(JSON.stringify(entry.replay))}
async function closePlayback(){playing.value=null;await nextTick();if(playbackOpener?.isConnected)playbackOpener.focus({preventScroll:true})}
let slotOpener
watch(slotIntent,async(value)=>{if(value){await nextTick();slotDialog.value?.showModal()}else{await nextTick();if(slotOpener?.isConnected)slotOpener.focus({preventScroll:true})}})
const pageSlots=computed(()=>slots.value.slice(slotPage.value*3,slotPage.value*3+3))
// Mirror the handler's actual ordinal lookup, including its NONE-offset quirk.
const colors=['#4a90d9','#5dae5a','#d9c54a','#d94a4a','#d98a4a','#9a4ad9','#888888']
const resultNames = { 0:'未完（概要のみ・取り込み不可）', 1:'村人勝利', 2:'人狼勝利', 3:'妖狐勝利' }
const filtered = computed(() => entries.value.filter(e => {
  const r = e.replay
  return (!configuration.value || r.peiyiName === configuration.value)
    && (!result.value || String(r.endResult) === result.value)
    && `${r.saveTime} ${configurationName(r.peiyiName)} ${resultNames[r.endResult]}`.toLocaleLowerCase().includes(query.value.trim().toLocaleLowerCase())
}))
const detail = computed(() => entries.value.find(e => e.id === selected.value))
const configurations = computed(() => [...new Set(entries.value.map(e => e.replay.peiyiName))])
let controller
onBeforeUnmount(() => controller?.abort())
const errors = {
  LEGACY_COMPATIBILITY_DIAGNOSTIC:'旧形式の記録を安全に確定できません。索引を推測して補いません。原本を保管してください。保存はしていません。',
  INVALID_JSON:'JSONが不正です。重複キー、過大な数値、深すぎる構造も受け付けません。',
  INVALID_UTF8:'UTF-8の録画ファイルを選択してください。',
  UNSUPPORTED_VERSION:'未対応の版または版の記載がないため読み込めません。',
  FILE_TIMEOUT:'ファイル読み取りが完了しませんでした。保存はしていません。',
  QUOTA_EXCEEDED:'ブラウザーの保存容量が不足しています。既存のReplayは変更していません。不要な録画を削除してから再試行してください。',
  LIBRARY_FULL:'Replayの保存上限（9局・合計64MiB）です。不要な録画を削除してから再試行してください。',
  LIBRARY_CHANGED:'別タブでReplay一覧が更新され続けています。一覧を再読込して再試行してください。',
  INVALID_LIBRARY:'保存済みReplayを検証できません。自動修復や上書きはしていません。',
  INVALID_REPLAY:'Replayの形式が不正なため保存していません。',
  REPLAY_TOO_LARGE:'Replayが8MiBの上限を超えたため保存していません。',
  STORAGE_BLOCKED:'別のタブが保存領域を使用中です。閉じてから再試行してください。',
  STORAGE_TIMEOUT:'保存領域の応答を確認できません。再読込して保存一覧を確認してください。',
  STORAGE_UNAVAILABLE:'ブラウザーの保存領域を使用できません。設定を確認して再試行してください。',
  HASH_UNAVAILABLE:'Replayの照合機能を使用できません。安全な接続で再試行してください。',
  SESSION_CHANGED:'対局の表示が変わったため保存を中止しました。',
  SLOT_CHANGED:'別のタブで保存枠が変更されました。一覧を再読込し、内容を確認してから保存してください。',
  INVALID_SLOT:'保存枠を確認できません。保存はしていません。',
}
async function run(action) {
  if (working.value) return
  working.value = true; problem.value = ''; message.value = ''
  try { await action() }
  catch (error) {
    const code = error?.code || (error?.name === 'AbortError' ? 'REPLAY_TIMEOUT' : 'REPLAY_OPERATION_FAILED')
    problem.value = (errors[code] || 'Replayの操作を完了できません。再同期または一覧の再読込後に再試行してください。') + `（${Object.hasOwn(errors,code) || ['REPLAY_READ_FAILED','REPLAY_TIMEOUT'].includes(code) ? code : 'REPLAY_OPERATION_FAILED'}）`
  } finally { working.value = false }
}
async function reload() { slots.value=await store.listSlots();entries.value=[...new Map(slots.value.filter(Boolean).map(e=>[e.id,e])).values()].sort((a,b)=>b.savedAt.localeCompare(a.savedAt)||a.id.localeCompare(b.id)) }
function open() { opened.value = !opened.value; if (opened.value) return run(reload) }
function save(requestedSlot=null) {
  if (!props.eligible || working.value) return
  const id = props.sessionId, cursor = props.cursor
  return run(async () => {
    controller = new AbortController()
    const timer = setTimeout(() => controller.abort(),15000)
    let replay
    try { replay = await readCompletedReplay(id,cursor,controller.signal) } finally { clearTimeout(timer); controller = null }
    if (!props.eligible || props.sessionId !== id || props.cursor !== cursor) { const e = new Error(); e.code = 'SESSION_CHANGED'; throw e }
    const saved = requestedSlot ? await store.saveToSlot(replay,requestedSlot.index,requestedSlot.expectedId) : await store.save(replay)
    opened.value = true
    await reload()
    if(requestedSlot && !saved.duplicate)props.audioControls?.click?.()
    message.value = saved.duplicate ? 'このReplayは保存済みです。重複して追加していません。' : 'Replayをこのブラウザーに保存しました。'
  })
}
function requestSlot(index){
  if(working.value)return
  if(!props.eligible){problem.value='保存できる終局録画はありません。対局を最後まで読んでから保存してください。';return}
  slotOpener=document.activeElement
  slotIntent.value={index,expectedId:slots.value[index]?.id ?? null,sessionId:props.sessionId,cursor:props.cursor}
}
function confirmSlot(){
  const intent=slotIntent.value;slotIntent.value=null
  if(!intent)return
  if(!props.eligible||intent.sessionId!==props.sessionId||intent.cursor!==props.cursor){problem.value=errors.SESSION_CHANGED;return}
  return save(intent)
}
onMounted(()=>{
  if(props.initialAction==='save') {
    if(!props.eligible || props.sessionId!==props.initialIntent?.sessionId || props.cursor!==props.initialIntent?.cursor)
      problem.value=errors.SESSION_CHANGED+'（SESSION_CHANGED）'
    else save()
  } else if(props.initialAction==='open')open()
  else if(props.initialAction==='cancel')problem.value='読み込みが中断されたため、保存は実行していません。もう一度操作してください。'
})
function previewFile(event) {
  const file=event.target.files?.[0]
  event.target.value='';candidates.value=[];chosen.value=[];importFile.value=file || null
  if(!file)return
  return run(async()=>{
    candidates.value=await readReplayFile(file)
    message.value='検証しました。取り込む終局録画を選び、確認してください。まだ保存していません。'
  })
}
function importChosen() {
  if(!chosen.value.length)return
  return run(async()=>{
    const selected=candidates.value.filter(c=>chosen.value.includes(c.fileSlot) && !c.readOnly).map(c=>JSON.parse(JSON.stringify(c.replay)))
    const result=await store.saveMany(selected)
    candidates.value=[];chosen.value=[];importFile.value=null;await reload()
    message.value=`${result.added}局を取り込みました。重複${result.duplicates}局は追加していません。対局と戦績は変更していません。`
  })
}
function exportEntries(values) {
  return run(async()=>{downloadReplayBytes(exportReplayFile(values.map(e=>JSON.parse(JSON.stringify(e.replay)))));message.value='交換ファイルを生成しました。ブラウザーのダウンロード先を確認してください。'})
}
function backupLibrary() {
  return run(async()=>{
    const entries=await store.backup()
    downloadReplayBytes(new TextEncoder().encode(JSON.stringify(entries)),'vocr-library-recovery.json')
    message.value='修復資料を生成しました。これは交換用Replayではありません。取り込みには使用できません。'
  })
}
function remove(id) {
  return run(async () => {
    await store.remove(id)
    removing.value = ''; if (selected.value === id) selected.value = ''
    await reload(); message.value = 'Replayを削除しました。対局と戦績は変更していません。'
  })
}
</script>

<template>
  <section class="replay-library panel" :class="{'desktop-replay-shell':desktop}" aria-label="ローカルReplay" lang="ja">
    <div v-if="!desktop" class="claim-actions">
      <button class="quiet-button" :aria-expanded="opened" :disabled="working" @click="open">{{ opened ? 'Replay一覧を閉じる' : 'Replay一覧' }}</button>
      <button v-if="eligible" class="primary-button" data-testid="save-replay" :disabled="working" @click="save()">{{ working ? '保存を確認中…' : 'この対局をReplayに保存' }}</button>
    </div>
    <p v-if="problem" role="alert">{{ problem }}</p><p v-if="message" role="status">{{ message }}</p>
    <div v-if="opened" v-show="!playing" class="desktop-replay-browser desktop-replay-canvas" data-testid="desktop-replay-browser">
      <AssetImage class="desktop-replay-background" name="images/PVBG.png" />
      <DesktopReplayExchange :slots="slots" :disabled="working" @saved="run(reload)" @notice="message=$event" />
      <button v-for="(entry,i) in pageSlots" :key="slotPage*3+i" class="desktop-save-slot" :style="{top:(80+i*200)/7.2+'%','--slot-color':entry ? (colors[entry.replay.peiyiOrdinal]||'#808080') : '#555555'}" :data-slot="slotPage*3+i" :aria-label="'保存枠 '+(slotPage*3+i)+(entry?' · '+configurationName(entry.replay.peiyiName)+'村':' · 空き')" :disabled="working" :data-no-click-sound="entry?undefined:''" @click="entry && playEntry(entry,$event)" @contextmenu.prevent="requestSlot(slotPage*3+i)" @keydown.shift.f10.prevent="requestSlot(slotPage*3+i)" @keydown.contextmenu.prevent="requestSlot(slotPage*3+i)">
        <span class="desktop-save-color" aria-hidden="true"></span><AssetImage class="desktop-save-frame" name="images/avg1_resized(3).png" />
        <span v-if="entry" class="desktop-save-description">{{ slotPage*3+i }}. {{ configurationName(entry.replay.peiyiName) }}村　　 {{ entry.replay.saveTime }}{{ '\n' }}　 {{ entry.replay.totalDays }}日目に  {{ resultNames[entry.replay.endResult] }}　　 {{ entry.replay.peiyiVillageCount>0 && entry.replay.totalVillageCount>0 ? entry.replay.peiyiVillageCount+'/'+entry.replay.totalVillageCount : '-/-' }}戦目</span>
        <span v-else class="desktop-save-description desktop-save-empty">{{ slotPage*3+i }}. (空槽位)</span>
      </button>
      <button class="desktop-art-button replay-nav-next" aria-label="次の保存枠" @click="slotPage=Math.min(2,slotPage+1)"><AssetImage name="images/PVBnext.png" /></button>
      <button v-if="slotPage>0" class="desktop-art-button replay-nav-back" aria-label="前の保存枠" @click="slotPage--"><AssetImage name="images/PVBreturn.png" /></button>
      <button class="desktop-art-button replay-nav-title" aria-label="タイトルに戻る" data-testid="replay-return" @click="emit('close')"><AssetImage name="images/PVBtitile.png" /></button>
    </div>
    <dialog v-if="slotIntent" ref="slotDialog" class="replay-slot-confirm" aria-label="保存確認" @cancel.prevent="slotIntent=null">
      <p>保存枠 {{ slotIntent.index }} にこの対局を保存しますか？{{slotIntent.expectedId?'以前の録画を置き換えます。':''}}</p>
      <button autofocus data-no-click-sound class="quiet-button" @click="slotIntent=null">キャンセル</button><button data-no-click-sound class="primary-button" @click="confirmSlot">保存を確定</button>
    </dialog>
    <details v-if="opened" v-show="!playing" class="replay-browser-management" :open="!desktop">
      <summary>ブラウザーの録画管理・交換・診断</summary>
      <div v-if="desktop" class="claim-actions"><button v-if="eligible" data-testid="save-replay" :disabled="working" @click="save()">この対局をReplayに保存</button></div>
      <h2>Replay一覧</h2>
      <p>このブラウザー・同じサイト内の保存です。別端末や別サイトには移りません。閲覧履歴の消去・プライベートモード等で失われる場合があります。</p>
      <p>終局の全イベント確認後に保存できます。上限9局・合計64MiB。交換ファイルはUTF-8 JSON・最大8MiBです。対局や戦績は再開・加算しません。</p>
      <p>同じ録画を複数の保存枠に置けます。この管理一覧で録画を削除すると、その録画を使うすべての保存枠が空になります。</p>
      <div class="replay-file-actions">
        <label>録画ファイルを選ぶ<input type="file" accept=".json,application/json" :disabled="working" @change="previewFile"></label>
        <p>ファイルには役職・技能・seedを含みます。選択後に全体を検証し、終局録画を確認してから保存します。未完録画は概要のみ表示します。取り込み・再生はできません。</p>
        <button class="quiet-button" :disabled="working || !entries.length" @click="exportEntries(entries)">一覧の全録画をエクスポート</button>
        <button class="quiet-button" :disabled="working" @click="backupLibrary">ローカル庫の修復資料を保存</button>
        <p>修復資料は未検証のローカル保存内容です。交換・再取り込みには使えません。第三者に共有せず保管してください。</p>
        <template v-if="importFile">
          <p>選択ファイル：{{ importFile.name }}</p>
          <button class="quiet-button" :disabled="working" @click="downloadReplayBytes(importFile,'vocr-selected-original.json')">選択ファイルの原本を保存</button>
          <p>原本は検証に失敗した内容もそのまま保管します。交換可能と判定したことにはなりません。</p>
        </template>
        <fieldset v-if="candidates.length" :disabled="working">
          <legend>取り込みの確認（未保存）</legend>
          <label v-for="c in candidates" :key="c.fileSlot"><input v-model="chosen" type="checkbox" :value="c.fileSlot" :disabled="c.readOnly">ファイル位置{{ c.fileSlot }} · {{ configurationName(c.replay.peiyiName) }}村 · {{ resultNames[c.replay.endResult] }} · {{ c.replay.totalDays }}日 · v{{ c.sourceVersion }}<span v-if="c.sourceVersion<3">→v3（技能履歴は補完しません）</span></label>
          <p>既存の録画を上書きしません。重複は追加せず、容量不足なら選択分を一括で取り消します。</p>
          <button class="primary-button" :disabled="!chosen.length" @click="importChosen">選択した録画の取り込みを確定</button>
          <button class="quiet-button" @click="candidates=[];chosen=[];importFile=null">取り込みを取り消す</button>
        </fieldset>
      </div>
      <div class="replay-filters">
        <label>日時・配役・勝敗を検索<input v-model="query" type="search" maxlength="120"></label>
        <label>配役<select v-model="configuration"><option value="">すべて</option><option v-for="c in configurations" :key="c" :value="c">{{ configurationName(c) }}村</option></select></label>
        <label>勝敗<select v-model="result"><option value="">すべて</option><option v-for="(name,id) in resultNames" :key="id" :value="id">{{ name }}</option></select></label>
        <button class="quiet-button" :disabled="working" @click="run(reload)">一覧を再読込</button>
      </div>
      <p role="status">{{ filtered.length }} / {{ entries.length }} 局</p>
      <p v-if="!filtered.length">該当するReplayはありません。</p>
      <ul class="replay-entries">
        <li v-for="entry in filtered" :key="entry.id" :data-replay-id="entry.id">
          <strong>{{ configurationName(entry.replay.peiyiName) }}村 · {{ resultNames[entry.replay.endResult] }}</strong>
          <span>{{ entry.replay.saveTime }} · 第{{ entry.replay.totalDays }}日 · {{ entry.replay.totalEvents }}記録</span>
          <button class="quiet-button" :disabled="working" @click="selected = entry.id">詳細を確認</button>
          <button class="quiet-button" :disabled="working" @click="exportEntries([entry])">この録画をエクスポート</button>
          <button class="quiet-button" :disabled="working" @click="playEntry(entry,$event)">再生する</button>
          <button v-if="removing !== entry.id" class="quiet-button" :disabled="working" @click="removing = entry.id">削除</button>
          <span v-else>このReplayを削除しますか？<button class="quiet-button" :disabled="working" @click="removing = ''">残す</button><button class="danger-button" :disabled="working" @click="remove(entry.id)">削除を確定</button></span>
        </li>
      </ul>
      <section v-if="detail" aria-label="Replay詳細">
        <h3>{{ configurationName(detail.replay.peiyiName) }}村 · {{ resultNames[detail.replay.endResult] }}</h3>
        <p>録画日時 {{ detail.replay.saveTime }} / ブラウザー保存 {{ new Date(detail.savedAt).toLocaleString('ja-JP') }}</p>
        <p>第{{ detail.replay.totalDays }}日 / {{ detail.replay.daySnapshots.length }}日分の記録 / {{ detail.replay.totalEvents }}記録 / {{ detail.bytes }} bytes / v{{ detail.replay.version }}</p>
        <button class="quiet-button" @click="selected = ''">詳細を閉じる</button>
      </section>
    </details>
    <component :is="desktop ? DesktopReplayPlayer : ReplayPlayer" v-if="playing" :replay="playing" :audio-controls="audioControls" @close="closePlayback" @playing="emit('playing',$event)" />
  </section>
</template>
