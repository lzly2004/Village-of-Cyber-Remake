<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { audioResources } from './audioAssets.js'
import { bgmScene } from './bgmScene.js'
import { createBgmPlayer } from './bgmPlayer.js'
const props = defineProps(['sessionId', 'update', 'currentEvent', 'finished', 'uncertain', 'canDecide'])
const state = ref({ enabled: false, status: 'off', id: null, error: null })
const scene = computed(() => bgmScene(props))
const player = createBgmPlayer({ notify: next => { state.value = next } })
let count = 0, selectionVersion = 0
const choosing = ref(false)
watch(() => [props.sessionId, scene.value.key], () => {
  ++selectionVersion; choosing.value = false
  player.select(scene.value.id)
}, { immediate: true })
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
onMounted(() => { document.addEventListener('visibilitychange', hide); window.addEventListener('pagehide', leave) })
onBeforeUnmount(() => {
  ++selectionVersion; document.removeEventListener('visibilitychange', hide); window.removeEventListener('pagehide', leave)
  player.dispose()
})
const labels = { off: '停止中', unlocking: '音声を準備中', loading: '読み込み中', playing: '再生中', silent: 'この場面は無音', error: '音楽を再生できません' }
</script>
<template>
  <section class="side-section bgm-control" aria-label="BGM" lang="ja" :data-status="state.status" :data-track="state.id || ''">
    <strong>BGM</strong>
    <button v-if="!state.enabled && state.status !== 'unlocking'" class="quiet-button" @click="player.enable">音楽を有効にする</button>
    <button v-else class="quiet-button" @click="stop">音楽を停止</button>
    <button v-if="canDecide" class="quiet-button" :disabled="!state.enabled || choosing" @click="nextTrack">曲を切替</button>
    <span role="status">{{ labels[state.status] }}</span>
    <small v-if="state.error" class="visual-diagnostic">音楽の表示診断：{{ state.error }}。有効化を押して再試行できます。対局操作は続けられます。</small>
    <small>初期状態は無音です。背景に移動・再読込後は再度有効化してください。</small>
  </section>
</template>
