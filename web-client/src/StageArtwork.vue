<script setup>
import { computed, onBeforeUnmount, shallowRef, watch } from 'vue'
import { stageImageCache } from './stageImageCache.js'
const emit = defineEmits(['consequence'])
const props = defineProps({ plan: { type: Object, required: true } })
const frames = computed(() => props.plan.frames || [props.plan.actors])
const names = computed(() => [...new Set([props.plan.background, ...frames.value.flat().map(actor => actor.name)])])
const assets = shallowRef(stageImageCache.ready(names.value))
const failed = shallowRef(false)
const frame = shallowRef(0)
let timer
let version = 0
let notified = false
function showFrame(value) {
  frame.value = value
  if (value === frames.value.length - 1 && !notified && !failed.value && !props.plan.diagnostic) { notified = true; emit('consequence') }
}
function startTransition(current) {
  if (frames.value.length < 2) return
  if (matchMedia('(prefers-reduced-motion: reduce)').matches) { showFrame(frames.value.length - 1); return }
  timer = setTimeout(() => { if (current === version) showFrame(1) }, props.plan.transitionMs)
}
function toggleFrame() {
  clearTimeout(timer)
  showFrame((frame.value + 1) % frames.value.length)
}
watch(names, list => {
  const current = ++version
  clearTimeout(timer)
  frame.value = 0; notified = false
  failed.value = false
  assets.value = stageImageCache.ready(list)
  if (assets.value) { startTransition(current); return }
  stageImageCache.load(list).then(result => {
    if (current === version) { assets.value = result; startTransition(current) }
  }).catch(() => { if (current === version) failed.value = true })
}, { immediate: true })
onBeforeUnmount(() => { version++; clearTimeout(timer) })
const actors = computed(() => assets.value ? frames.value[frame.value].map(actor => ({
  ...actor, ...assets.value[names.value.indexOf(actor.name)],
})).sort((a,b) => a.layer-b.layer) : [])
</script>

<template>
  <svg class="stage-artwork" data-testid="stage-artwork" :data-art-ready="Boolean(assets)" :data-art-frame="frame + 1" viewBox="0 0 1280 720"
    preserveAspectRatio="xMidYMid meet" aria-hidden="true" focusable="false">
    <rect width="1280" height="720" class="stage-sky" />
    <g v-if="assets && !failed" data-testid="decoded-stage-scene">
      <image :href="assets[0].url" x="0" y="0" width="1280" height="720" preserveAspectRatio="none" data-testid="stage-background" @error="failed = true" />
      <image v-for="(actor,index) in actors" :key="index + actor.name" :href="actor.url"
        :x="actor.x ?? (1280-actor.width)/2" :y="690-actor.height" :width="actor.width" :height="actor.height"
        :data-source="actor.name" :data-layer="actor.layer" data-testid="stage-character" @error="failed = true" />
    </g>
  </svg>
  <button v-if="assets && !failed && frames.length > 1" class="stage-frame-toggle quiet-button" data-testid="stage-frame-toggle" @click="toggleFrame">画像を切替（{{ frame + 1 }}/{{ frames.length }}）</button>
  <span v-if="failed || plan.diagnostic || !assets" class="stage-art-status" role="status" data-testid="stage-art-status">
    {{ failed ? '画像表示診断：立ち絵を読み込めません。台詞は引き続き読めます。'
      : plan.diagnostic ? '画像表示診断：表示情報が不足しているため代替表示です。'
      : '画像読込中…台詞は引き続き読めます。' }}
  </span>
</template>
