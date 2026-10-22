<script setup>
import { computed, onBeforeUnmount, shallowRef, watch } from 'vue'
import { stageImageCache } from './stageImageCache.js'
const props = defineProps({ plan: { type: Object, required: true } })
const names = computed(() => [props.plan.background, ...props.plan.actors.map(actor => actor.name)])
const assets = shallowRef(stageImageCache.ready(names.value))
const failed = shallowRef(false)
let version = 0
watch(names, list => {
  const current = ++version
  failed.value = false
  assets.value = stageImageCache.ready(list)
  if (assets.value) return
  stageImageCache.load(list).then(result => {
    if (current === version) assets.value = result
  }).catch(() => { if (current === version) failed.value = true })
}, { immediate: true })
onBeforeUnmount(() => { version++ })
const actors = computed(() => assets.value ? props.plan.actors.map((actor, index) => ({
  ...actor, ...assets.value[index+1],
})).sort((a,b) => a.layer-b.layer) : [])
</script>

<template>
  <svg class="stage-artwork" data-testid="stage-artwork" :data-art-ready="Boolean(assets)" viewBox="0 0 1280 720"
    preserveAspectRatio="xMidYMid meet" aria-hidden="true" focusable="false">
    <rect width="1280" height="720" class="stage-sky" />
    <g v-if="assets && !failed" data-testid="decoded-stage-scene">
      <image :href="assets[0].url" x="0" y="0" width="1280" height="720" preserveAspectRatio="none" data-testid="stage-background" @error="failed = true" />
      <image v-for="(actor,index) in actors" :key="index + actor.name" :href="actor.url"
        :x="actor.x ?? (1280-actor.width)/2" :y="690-actor.height" :width="actor.width" :height="actor.height"
        :data-source="actor.name" :data-layer="actor.layer" data-testid="stage-character" @error="failed = true" />
    </g>
  </svg>
  <span v-if="failed || plan.diagnostic || !assets" class="stage-art-status" role="status" data-testid="stage-art-status">
    {{ failed ? '画像表示診断：立ち絵を読み込めません。台詞は引き続き読めます。'
      : plan.diagnostic ? '画像表示診断：表示情報が不足しているため代替表示です。'
      : '画像読込中…台詞は引き続き読めます。' }}
  </span>
</template>
