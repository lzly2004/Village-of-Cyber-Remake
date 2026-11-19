<script setup>
import { computed, ref } from 'vue'
import AssetImage from './AssetImage.vue'
import PlayerPortrait from './PlayerPortrait.vue'
import {characterDisplayName} from './characterDisplay.js'
import {playerVisual} from './playerVisual.js'
const props=defineProps({type:String,players:Array,bindings:Object,targets:Array,selected:Array,toggleTarget:Function,playerName:Function,markers:Map})
const tool=ref('all')
const b=computed(()=>props.bindings)
const skill=computed(()=>b.value.claimedSkills.find(s=>s.type===(props.type==='divination'?'DIVINATION':'GUARD')))
const actors=computed(()=>['vote','claim'].includes(props.type)?[]:skill.value?.actors||[])
const frame=computed(()=>({vote:'frameSBlue',claim:'frameSRed',divination:'frameSRed',guard:'frameOrange'})[props.type])
const actionImage=computed(()=>({vote:'touhyou',claim:'uranaiAll',divination:'uranaiAll',guard:'goeiAll'})[props.type])
function selectedFor(player) {
 if(['vote','claim'].includes(props.type))return props.selected.includes(player)
 if(tool.value==='all'||tool.value==='delete')return (props.type==='divination'?b.value.selectedDivinationTargets:b.value.selectedGuardTargets).has(player)
 return actors.value.find(a=>a.playerNumber===tool.value)?.targets.includes(player)||false
}
function selectedAnywhere(player){
 if(['vote','claim'].includes(props.type))return props.selected.includes(player)
 return (props.type==='divination'?b.value.selectedDivinationTargets:b.value.selectedGuardTargets).has(player)
}
function hiddenSelected(player){return props.type==='vote'||props.type==='claim'?props.selected.includes(player):b.value.update.selections[props.type].hiddenTargets.includes(player)}
function actorIcon(actor){return 'images/result'+(props.type==='divination'?1:3)+'_'+actor.claimedRoleOrder+(props.type==='divination'?'white':'')+'.png'}
function apply(player,dropped=false) {
 if(!b.value.canDecide||!props.targets.includes(player))return
 const enabled=tool.value==='delete'?false:dropped?true:!selectedFor(player)
 if(['vote','claim'].includes(props.type))props.toggleTarget(player,enabled)
 else if(tool.value==='all'||tool.value==='delete') (props.type==='divination'?b.value.setDivinationTarget:b.value.setGuardTarget)(player,enabled)
 else b.value.setClaimedTarget(props.type==='divination'?'DIVINATION':'GUARD',tool.value,player,enabled)
}
function drop(event,player){if(event.dataTransfer?.getData('text/plain')==='vocr-selection')apply(player,true)}
function drag(event,value){tool.value=value;event.dataTransfer.effectAllowed='copy';event.dataTransfer.setData('text/plain','vocr-selection')}
</script>
<template>
 <section class="desktop-target-scene" :data-selection-type="type" :aria-label="type==='claim'?'CO確認対象':type==='vote'?'指定投票':type==='divination'?'占い指定':'護衛指定'">
  <div class="desktop-target-roster" tabindex="0">
   <div class="desktop-target-grid" :style="{'--columns':Math.ceil(players.length/2)}">
    <div v-for="(player,index) in players" :key="player.playerNumber" class="player-row desktop-target-row" :class="{departed:!player.alive}" :style="{gridColumn:index%Math.ceil(players.length/2)+1,gridRow:index<Math.ceil(players.length/2)?1:2}" :data-player-number="player.playerNumber">
    <button class="desktop-target" role="checkbox" :aria-label="type==='vote'?'指定 '+characterDisplayName(player.character)+'（'+player.playerNumber+'号）':type==='claim'?player.playerNumber+'番 '+characterDisplayName(player.character)+'をCO確認対象に選択':playerName(player.playerNumber)" :aria-checked="selectedFor(player.playerNumber)" :disabled="!b.canDecide||!targets.includes(player.playerNumber)" @click="apply(player.playerNumber)" @dragover.prevent @drop.prevent="drop($event,player.playerNumber)">
     <PlayerPortrait :show-name="false" :status-markers="false" :player="player" :markers="markers?.get(player.playerNumber)||[]" />
     <AssetImage v-if="selectedAnywhere(player.playerNumber)" class="target-frame" :name="'images/'+frame+'.png'" />
     <AssetImage v-if="type==='vote' && hiddenSelected(player.playerNumber)" class="target-vote-result" name="images/result2_all.png" />
     <AssetImage v-if="type==='claim' && b.update.selections.voteTargets.includes(player.playerNumber)" class="target-vote-result" name="images/result2_all.png" />
     <AssetImage v-if="hiddenSelected(player.playerNumber)" class="target-result-all" :name="'images/'+({vote:'result2_all',claim:'result1_all',divination:'result1_all',guard:'result3_all'})[type]+'.png'" />
     <template v-for="actor in actors" :key="actor.playerNumber"><AssetImage v-if="actor.targets.includes(player.playerNumber)" class="target-actor-selection" :name="actorIcon(actor)" :style="{top:(actor.claimedRoleOrder*17/98*100)+'%'}" /></template>
    </button>
    <span class="player-identity visually-hidden"><strong>{{ characterDisplayName(player.character) }}</strong><small>{{ playerVisual(player).roleText }} {{ playerVisual(player).orderText }}</small></span>
    </div>
   </div>
  </div>
  <div class="desktop-drag-tools" :class="{'desktop-tools-overlap':type==='divination' && actors.some(a=>a.claimedRoleOrder===1)}" aria-label="指定操作">
   <button class="desktop-drag-tool" :aria-label="type==='claim'?'CO確認対象を選択':type==='vote'?'指定投票を設定':type==='divination'?'全員の占いを指定':'全員の護衛を指定'" :aria-pressed="tool==='all'" :disabled="!b.canDecide" draggable="true" @dragstart="drag($event,'all')" @click="tool='all'"><AssetImage :name="'images/'+actionImage+'.png'" /></button>
   <button v-for="actor in actors" :key="actor.playerNumber" class="desktop-drag-tool desktop-actor-tool" :style="{left:((props.type==='divination'?actor.claimedRoleOrder*100+150:actor.claimedRoleOrder*150+250)-250)/1280*100+'cqw'}" :aria-label="playerName(actor.playerNumber)+'の'+(type==='divination'?'占い':'護衛')+'を指定'" :aria-pressed="tool===actor.playerNumber" :disabled="!b.canDecide" draggable="true" @dragstart="drag($event,actor.playerNumber)" @click="tool=actor.playerNumber"><AssetImage :name="'images/'+(type==='divination'?'uranai':'goei')+actor.claimedRoleOrder+'.png'" /></button>
   <button class="desktop-drag-tool" aria-label="指定を削除" :aria-pressed="tool==='delete'" :disabled="!b.canDecide" draggable="true" @dragstart="drag($event,'delete')" @click="tool='delete'"><AssetImage name="images/delete.png" /></button>
  </div>
 </section>
</template>
