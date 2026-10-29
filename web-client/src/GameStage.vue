<script setup>
import { computed } from 'vue'
import { characterKana } from './characterDisplay.js'
import StageArtwork from './StageArtwork.vue'

const emit = defineEmits(['consequence','art-ready','art-failed'])
const props = defineProps({
  event: { type: Object, default: null },
  artwork: { type: Object, default: () => ({ mode: 'deferred' }) },
  eventKey: { type: String, default: '' },
  stage: { type: String, default: '' },
  reading: { type: Boolean, default: true },
})
// Only the already-public event's character keys. This is NOT the final image mapping.
const participants = computed(() => props.event?.eventname === 'wsw' ? []
  : [...new Set([props.event?.ch1, props.event?.ch2, props.event?.ch3]
    .filter(value => typeof value === 'string' && value !== '' && value !== 'NONE'))])
const positions = computed(() => participants.value.map((character, index, all) => ({
  character, x: 640 + (index - (all.length - 1) / 2) * 250,
})))
</script>

<template>
  <div class="game-stage" :data-stage="stage" data-testid="game-stage">
    <div class="stage-surface" data-testid="stage-surface">
      <StageArtwork v-if="artwork.mode === 'dialogue' || artwork.mode === 'scene'" :plan="artwork" @consequence="emit('consequence', eventKey)" @ready="emit('art-ready')" @failed="emit('art-failed')" />
      <svg v-else lang="zh-CN" class="stage-artwork" data-testid="stage-artwork" viewBox="0 0 1280 720"
        preserveAspectRatio="xMidYMid meet" aria-hidden="true" focusable="false">
        <rect class="stage-sky" width="1280" height="720" />
        <path class="stage-horizon" d="M0 360 L180 240 L330 330 L540 180 L760 320 L970 215 L1280 360 V720 H0 Z" />
        <path class="stage-ground" d="M0 470 Q640 360 1280 470 V720 H0 Z" />
        <path class="stage-guides" d="M0 360 H1280 M640 0 V720" />
        <text x="40" y="60" class="stage-caption">舞台占位预览 · 正式素材待接</text>
        <g v-for="(actor, index) in positions" :key="actor.character" :transform="`translate(${actor.x} 0)`"
          data-testid="stage-actor">
          <text y="205" text-anchor="middle" class="stage-actor-label">{{ characterKana(actor.character) }}</text>
          <circle cy="265" r="40" class="stage-figure" />
          <rect x="-64" y="315" width="128" height="220" rx="36" class="stage-figure" />
          <text y="375" text-anchor="middle" class="stage-slot-label">占位 {{ index + 1 }}</text>
        </g>
        <text v-if="!positions.length" x="640" y="270" text-anchor="middle" class="stage-empty">场景占位</text>
      </svg>
      <slot name="scene-overlay" />
      <div v-if="reading" class="stage-text" role="region" lang="zh-CN" aria-label="舞台文字阅读区" tabindex="0"
        aria-describedby="stage-reading-help" data-testid="stage-text">
        <slot />
      </div>
    </div>
    <p v-if="reading" id="stage-reading-help" lang="zh-CN" class="stage-reading-help">文字较长时可在阅读区滚动；按 Tab 聚焦后，可用方向键阅读。确认按钮位于舞台下方。</p>
  </div>
</template>
