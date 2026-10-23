<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { audioResources } from './audioAssets.js'
import { bgmScene } from './bgmScene.js'
import { createBgmPlayer } from './bgmPlayer.js'
import { defaultAudioSettings, readAudioSettings, saveAudioSettings } from './audioSettings.js'
import { enteringSounds, transitionSound } from './soundScene.js'
const props = defineProps(['sessionId', 'update', 'currentEvent', 'finished', 'uncertain', 'canDecide', 'cursor'])
const state = ref({ enabled: false, status: 'off', id: null, error: null })
const scene = computed(() => bgmScene(props))
const player = createBgmPlayer({ notify: next => { state.value = next } })
let count = 0, selectionVersion = 0
const choosing = ref(false)
const settings = ref(defaultAudioSettings()), settingsWarning = ref(null)
let nightGroup = null, transitioned = false
function changeSettings(patch) {
  const next = {...settings.value,...patch}
  if (!player.settings(next)) { settingsWarning.value = 'AUDIO_SETTINGS_INVALID'; return }
  settings.value = next
  try { settingsWarning.value = saveAudioSettings(localStorage,settings.value) }
  catch { settingsWarning.value = 'AUDIO_SETTINGS_WRITE_FAILED' }
}
watch([() => props.sessionId, () => scene.value.key], () => {
  ++selectionVersion; choosing.value = false
  player.select(scene.value.id)
}, { immediate: true })
watch([() => props.sessionId, () => props.cursor, () => props.currentEvent?.stage, () => props.finished, () => props.uncertain], () => {
  transitioned = false; player.cancelSounds({preserveClick:!props.uncertain})
  if (props.uncertain) return
  const group = props.sessionId + ':' + props.update?.view.gameDay
  const first = props.currentEvent?.stage === 'NIGHT_RESULT' && nightGroup !== group
  if (first) nightGroup = group
  player.effects(enteringSounds(props.currentEvent,props.finished,props.finished ? props.update?.view.result : null,first))
}, {immediate:true})
function transition(key) {
  if (transitioned || props.uncertain || key !== props.sessionId + ':' + props.cursor) return
  transitioned = true
  const id = transitionSound(props.currentEvent)
  if (id) player.effects([id])
}
defineExpose({ click: () => player.click(), transition })
async function nextTrack() {
  if (!state.value.enabled || !props.canDecide || choosing.value || !scene.value.playlist) return
  const token = ++selectionVersion
  choosing.value = true
  try {
    const catalog = await audioResources.manifest()
    if (token !== selectionVersion || !state.value.enabled || !props.canDecide) return
    const list = catalog.playlists[scene.value.playlist]
    player.select(list[count++ % list.length])
  } catch { if (token === selectionVersion) player.disable('MANIFEST_LOAD_FAILED') }
  finally { if (token === selectionVersion) choosing.value = false }
}
function stop() { ++selectionVersion; choosing.value = false; player.disable() }
function hide() { if (document.hidden) stop() }
function leave(event) { stop(); if (!event.persisted) player.dispose() }
onMounted(() => {
  try { const loaded = readAudioSettings(localStorage); settings.value = loaded.settings; settingsWarning.value = loaded.warning }
  catch { settingsWarning.value = 'AUDIO_SETTINGS_READ_FAILED' }
  player.settings(settings.value)
  document.addEventListener('visibilitychange', hide); window.addEventListener('pagehide', leave)
})
onBeforeUnmount(() => {
  ++selectionVersion; document.removeEventListener('visibilitychange', hide); window.removeEventListener('pagehide', leave)
  player.dispose()
})
const labels = { off: '停止中', unlocking: '音声を準備中', loading: '読み込み中', playing: '再生中', silent: 'この場面は無音', error: '音楽を再生できません' }
</script>
<template>
  <section class="side-section bgm-control" aria-label="BGM" lang="ja" :data-status="state.status" :data-track="state.id || ''" :data-sound="state.sound?.id || ''" :data-sound-status="state.sound?.status || 'idle'">
    <strong>BGM・効果音</strong>
    <button v-if="!state.enabled && state.status !== 'unlocking'" class="quiet-button" @click="player.enable">音楽を有効にする</button>
    <button v-else class="quiet-button" @click="stop">音楽を停止</button>
    <button v-if="canDecide" class="quiet-button" :disabled="!state.enabled || choosing" @click="nextTrack">曲を切替</button>
    <span role="status">{{ labels[state.status] }}</span>
    <small v-if="state.error" class="visual-diagnostic">音楽の表示診断：{{ state.error }}。有効化を押して再試行できます。対局操作は続けられます。</small>
    <label>BGM音量 {{ settings.bgmVolume }}%
      <input type="range" min="0" max="100" step="1" aria-label="BGM音量" :value="settings.bgmVolume" @input="changeSettings({bgmVolume:Number($event.target.value)})">
    </label>
    <label>効果音量 {{ settings.soundVolume }}%
      <input type="range" min="0" max="100" step="1" aria-label="効果音量" :value="settings.soundVolume" @input="changeSettings({soundVolume:Number($event.target.value)})">
    </label>
    <button class="quiet-button" role="switch" aria-label="消音" :aria-checked="settings.muted" @click="changeSettings({muted:!settings.muted})">{{ settings.muted ? '消音中' : '消音なし' }}</button>
    <small v-if="settingsWarning" role="status">音声設定の保存・復元診断：{{ settingsWarning }}。現在のページでは操作できます。</small>
    <small v-if="state.sound?.error" role="status">効果音の表示診断：{{ state.sound.error }}。対局操作は続けられます。</small>
    <small>初期状態は無音です。背景に移動・再読込後は再度有効化してください。</small>
  </section>
</template>
