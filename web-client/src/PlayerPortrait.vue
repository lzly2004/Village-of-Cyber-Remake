<script setup>
import { computed } from 'vue'
import AssetImage from './AssetImage.vue'
import { playerVisual } from './playerVisual.js'
import { settlementPortrait } from './settlementVisual.js'
const props = defineProps({ player: { type: Object, required: true }, revealed: Boolean, actualRole: { type: String, default: null }, markers:{type:Array,default:()=>[]}, showName:{type:Boolean,default:true}, statusMarkers:{type:Boolean,default:true}, claimMarkers:{type:Boolean,default:true} })
const visual = computed(() => playerVisual(props.player))
const portrait = computed(() => props.revealed ? settlementPortrait(props.player, props.actualRole) : visual.value.portrait)
</script>

<template>
  <span class="player-portrait">
    <AssetImage class="portrait-image" :name="portrait" />
    <AssetImage v-if="claimMarkers && visual.claimIcon" class="claim-icon" :name="visual.claimIcon" />
    <AssetImage v-if="statusMarkers && visual.deathIcon" class="death-marker" :name="visual.deathIcon" />
    <AssetImage v-for="marker in markers" :key="marker.actor+':'+marker.day" class="skill-report-marker" :name="marker.icon" :style="{right:(marker.role-1)*18/64*100+'%',top:(marker.order-1)*17/98*100+'%'}" :data-report-actor="marker.actor" :data-report-day="marker.day" />
    <AssetImage v-if="showName && visual.characterText" class="character-title-image" :name="visual.characterText" />
  </span>
</template>
