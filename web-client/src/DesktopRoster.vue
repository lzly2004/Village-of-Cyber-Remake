<script setup>
import PlayerPortrait from './PlayerPortrait.vue'
import {characterDisplayName} from './characterDisplay.js'
import {playerVisual} from './playerVisual.js'
defineProps({players:Array,markers:Map})
</script>
<template><div class="desktop-vote-players"><section class="players panel" aria-label="公開プレイヤー">
 <div class="player-list" role="region" aria-label="公开玩家列表滚动区域" tabindex="0">
  <div v-for="(player,index) in players" :key="player.playerNumber" class="desktop-roster-row" :class="{departed:!player.alive}" tabindex="0" :style="{gridColumn:index%Math.ceil(players.length/2)+1,gridRow:index<Math.ceil(players.length/2)?1:2}" :data-player-number="player.playerNumber" :aria-label="characterDisplayName(player.character)+' '+playerVisual(player).roleText+' '+playerVisual(player).orderText">
   <PlayerPortrait :player="player" :markers="markers?.get(player.playerNumber)||[]" />
   <span class="player-identity"><strong>{{ characterDisplayName(player.character) }}</strong><small>{{ playerVisual(player).roleText }} {{ playerVisual(player).orderText }}</small><small v-if="playerVisual(player).claimDiagnostic">表示診断：CO図なし</small></span>
  </div>
 </div>
</section></div></template>
