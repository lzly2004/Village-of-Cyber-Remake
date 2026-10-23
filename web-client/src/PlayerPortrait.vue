<script setup>
import { computed } from 'vue'
import AssetImage from './AssetImage.vue'
import { playerVisual } from './playerVisual.js'
import { settlementPortrait } from './settlementVisual.js'
const props = defineProps({ player: { type: Object, required: true }, revealed: Boolean, actualRole: { type: String, default: null } })
const visual = computed(() => playerVisual(props.player))
const portrait = computed(() => props.revealed ? settlementPortrait(props.player, props.actualRole) : visual.value.portrait)
</script>

<template>
  <span class="player-portrait">
    <AssetImage class="portrait-image" :name="portrait" />
    <AssetImage v-if="visual.claimIcon" class="claim-icon" :name="visual.claimIcon" />
    <AssetImage v-if="visual.deathIcon" class="death-marker" :name="visual.deathIcon" />
    <AssetImage v-if="visual.characterText" class="character-title-image" :name="visual.characterText" />
  </span>
</template>
