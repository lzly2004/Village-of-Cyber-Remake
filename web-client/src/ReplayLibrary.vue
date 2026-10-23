<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { configurationName } from './labels.js'
import { createReplayStore } from './replayStore.js'
import { readCompletedReplay } from './replayApi.js'
const props = defineProps({ sessionId:String, cursor:Number, eligible:Boolean })
const store = createReplayStore()
const opened = ref(false), working = ref(false), entries = ref([]), message = ref(''), problem = ref('')
const query = ref(''), configuration = ref(''), result = ref(''), removing = ref(''), selected = ref('')
const resultNames = { 1:'村人勝利', 2:'人狼勝利', 3:'妖狐勝利' }
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
async function reload() { entries.value = await store.list() }
function open() { opened.value = !opened.value; if (opened.value) return run(reload) }
function save() {
  if (!props.eligible || working.value) return
  const id = props.sessionId, cursor = props.cursor
  return run(async () => {
    controller = new AbortController()
    const timer = setTimeout(() => controller.abort(),15000)
    let replay
    try { replay = await readCompletedReplay(id,cursor,controller.signal) } finally { clearTimeout(timer); controller = null }
    if (!props.eligible || props.sessionId !== id || props.cursor !== cursor) { const e = new Error(); e.code = 'SESSION_CHANGED'; throw e }
    const saved = await store.save(replay)
    opened.value = true
    await reload()
    message.value = saved.duplicate ? 'このReplayは保存済みです。重複して追加していません。' : 'Replayをこのブラウザーに保存しました。'
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
  <section class="replay-library panel" aria-label="ローカルReplay" lang="ja">
    <div class="claim-actions">
      <button class="quiet-button" :aria-expanded="opened" :disabled="working" @click="open">{{ opened ? 'Replay一覧を閉じる' : 'Replay一覧' }}</button>
      <button v-if="eligible" class="primary-button" data-testid="save-replay" :disabled="working" @click="save">{{ working ? '保存を確認中…' : 'この対局をReplayに保存' }}</button>
    </div>
    <p v-if="problem" role="alert">{{ problem }}</p><p v-if="message" role="status">{{ message }}</p>
    <div v-if="opened">
      <h2>Replay一覧</h2>
      <p>このブラウザー・同じサイト内の保存です。別端末や別サイトには移りません。閲覧履歴の消去・プライベートモード等で失われる場合があります。</p>
      <p>終局の全イベント確認後に保存できます。上限9局・合計64MiB。再生・ファイル入出力は準備中です。対局や戦績は再開・加算しません。</p>
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
    </div>
  </section>
</template>
