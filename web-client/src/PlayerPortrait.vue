<script setup>
import { computed } from 'vue'
import AssetImage from './AssetImage.vue'
import { playerVisual } from './playerVisual.js'
const props = defineProps({ player: { type: Object, required: true } })
const visual = computed(() => playerVisual(props.player))
</script>

<template>
  <span class="player-portrait">
    <AssetImage class="portrait-image" :name="visual.portrait" />
    <AssetImage v-if="visual.claimIcon" class="claim-icon" :name="visual.claimIcon" />
    <AssetImage v-if="visual.deathIcon" class="death-marker" :name="visual.deathIcon" />
    <AssetImage v-if="visual.characterText" class="character-title-image" :name="visual.characterText" />
  </span>
</template>
