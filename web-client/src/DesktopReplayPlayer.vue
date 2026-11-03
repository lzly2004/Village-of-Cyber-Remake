<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AssetImage from './AssetImage.vue'
import { roles } from './labels.js'
import { replayRoles,replayDay,replayFrame } from './replayPlayback.js'
import { replayInformation,replayVotes } from './replayInformation.js'
import { validateReplay } from './replayValidation.js'
import { characterDisplayName } from './characterDisplay.js'
const props=defineProps(['replay','audioControls'])
const emit=defineEmits(['close','playing'])
const root=ref(null),day=ref(2),mode=ref('info'),round=ref(0),valid=ref(false)
let recording
try{recording=validateReplay(props.replay,{extensions:true});day.value=replayDay(recording);valid.value=true}catch{/* Never substitute a live game or repair a file. */}
const frame=computed(()=>valid.value?replayFrame(recording,day.value):null)
const info=computed(()=>valid.value?replayInformation(recording,day.value):null)
const votes=computed(()=>info.value?replayVotes(info.value.current,day.value,round.value):null)
const dateLocked=computed(()=>mode.value==='votes'&&round.value>0)
const roleName=n=>n>0?(roles[replayRoles[n]]||'不明'):'不明'
function accessiblePlayer(p){return characterDisplayName(p.character)+'（'+(info.value.terminal?'真の職業：'+(p.actualRole==='NONE'?'不明':roles[p.actualRole]||'不明'):'公称職業：'+p.claimText+(p.claimOrder?' '+p.claimOrder:''))+'）'}
const roleText=computed(()=>info.value?'一役職一\n'+info.value.groups.map(g=>'['+roleName(g.role)+']\n'+g.players.map(p=>p.name).join('　')+'\n').join(''):'')
const ballot=computed(()=>{
 if(!votes.value)return null
 const v=votes.value,extra=v.rule===0?'自由投票\n':v.rule===1?'グレラン：\n'+v.candidates.join(''):v.rule===2?'指定投票：\n'+v.candidates.join(','):''
 return {title:`-投票結果/${day.value}日目-第${v.round}轮${extra}\n`,lines:v.rows.map(r=>`${r.name}：${r.votes}票  投票先→${r.targetName}`)}
})
watch(day,()=>{round.value=0;if(!votes.value)mode.value='info'})
function changeDay(offset){if(!dateLocked.value)day.value=replayDay(recording,day.value+offset)}
function toggleVote(){if(!votes.value)return;mode.value=mode.value==='info'?'votes':'info';round.value=0}
function close(){emit('close')}
onMounted(async()=>{emit('playing',true);await nextTick();root.value?.focus({preventScroll:true})})
onBeforeUnmount(()=>emit('playing',false))
</script>
<template>
 <section ref="root" class="replay-player desktop-replay-player" role="dialog" aria-label="Replay再生" lang="ja" tabindex="-1" @keydown.esc.stop.prevent="close">
  <div class="desktop-replay-canvas" data-testid="desktop-replay-player">
   <AssetImage class="desktop-replay-background" name="images/PVBG.png" />
   <template v-if="valid">
    <div class="replay-avatar-list" role="region" aria-label="Replay人物一覧" tabindex="0" :style="{'--replay-columns':Math.ceil(frame.players.length/2)||1}">
     <article v-for="(p,index) in frame.players" :key="p.playerIndex" class="replay-player-card" :style="{gridColumn:index%Math.ceil(frame.players.length/2)+1,gridRow:index<Math.ceil(frame.players.length/2)?1:2}" :data-player-index="p.playerIndex" :data-portrait="p.portrait" :data-departed="p.departed" :aria-label="accessiblePlayer(p)" tabindex="0">
      <span class="player-portrait"><AssetImage class="portrait-image" :name="p.portrait" />
       <AssetImage v-if="p.claimIcon" class="claim-icon" :name="p.claimIcon" /><AssetImage v-if="p.deathIcon" class="death-marker" :name="p.deathIcon" />
       <AssetImage v-if="p.characterText" class="character-title-image" :name="p.characterText" />
       <AssetImage v-for="(marker,i) in info.icons.filter(m=>m.target===p.playerIndex&&m.icon)" :key="i" class="skill-report-marker" :name="marker.icon" :style="{right:(marker.role-1)*18/64*100+'%',top:(marker.order-1)*17/98*100+'%'}" :data-report-day="marker.day" />
      </span>
     </article>
    </div>
    <div v-if="frame.population" class="desktop-calendar"><AssetImage name="images/hiduke.png" /><span class="desktop-calendar-text" data-testid="replay-population">{{'    '+day+'日目\n 生存者:'+frame.population.alive+'\n 死亡者:'+frame.population.dead+'\n 吊り縄:'+frame.population.ropes}}</span></div>
    <AssetImage class="desktop-replay-board" name="images/frame #19252.png" />
    <section class="replay-information desktop-replay-information" aria-label="Replay情報・履歴" tabindex="0">
     <pre v-if="info.terminal" class="replay-history-text replay-final-roles" data-testid="replay-role-groups">{{roleText}}</pre>
     <pre v-else-if="mode==='info'" class="replay-history-text" data-testid="replay-normal-info">{{info.normalText}}</pre>
     <div v-else-if="ballot" class="replay-ballot-columns" data-testid="replay-votes"><pre>{{ballot.title+ballot.lines.slice(0,10).join('\n')}}</pre><pre>{{'\n\n'+ballot.lines.slice(10).join('\n')}}</pre></div>
    </section>
    <button v-if="day<frame.lastDay" class="desktop-art-button replay-nav-next" aria-label="次の日" :disabled="dateLocked" @click="changeDay(1)"><AssetImage name="images/PVBnext.png" /></button>
    <button v-else class="desktop-art-button replay-nav-next" aria-label="前の日" :disabled="dateLocked||day<=frame.firstDay" @click="changeDay(-1)"><AssetImage name="images/PVBreturn.png" /></button>
    <button v-if="day<frame.lastDay" class="desktop-art-button replay-nav-back" :aria-label="day===frame.firstDay?'Replayを閉じる':'前の日'" :disabled="day!==frame.firstDay&&dateLocked" @click="day===frame.firstDay?close():changeDay(-1)"><AssetImage name="images/PVBreturn.png" /></button>
    <button v-if="day===frame.lastDay" class="desktop-art-button replay-nav-title" aria-label="Replayを閉じる" @click="close"><AssetImage name="images/PVBtitile.png" /></button>
    <button v-else class="desktop-art-button replay-nav-title" :aria-label="mode==='info'?'投票結果':'情報・技能履歴'" @click="toggleVote"><AssetImage :name="mode==='info'?'images/PVtohyo.png':'images/PVBreturn.png'" /></button>
    <button v-if="mode==='votes'&&votes?.index>0" class="desktop-art-button replay-round-button" aria-label="前の投票回" @click="round--"><AssetImage name="images/PVRirekiBack.png" /></button>
    <button v-if="mode==='votes'&&votes&&votes.index<votes.total-1" class="desktop-art-button replay-round-button next" aria-label="次の投票回" @click="round++"><AssetImage name="images/PVRirekiNext.png" /></button>
   </template>
   <template v-else><p role="alert">録画を表示できません（INVALID_REPLAY）。保存データは変更していません。</p><button class="desktop-art-button replay-nav-title" aria-label="Replayを閉じる" @click="close"><AssetImage name="images/PVBreturn.png" /></button></template>
  </div>
  <details v-if="valid" class="replay-browser-management"><summary>ブラウザーの録画診断・操作補助</summary>
   <p>録画の閲覧です。進行中の対局・戦績は変更しません。{{day}}日 / {{frame.lastDay}}日</p>
   <label>再生日<select :disabled="dateLocked" :value="day" @change="day=replayDay(recording,Number($event.target.value))"><option v-for="d in frame.lastDay-frame.firstDay+1" :key="d" :value="d+frame.firstDay-1">第{{d+frame.firstDay-1}}日</option></select></label>
   <p v-for="d in [...frame.diagnostics,...info.diagnostics]" :key="JSON.stringify(d)" role="status">表示診断：{{d.code}}</p>
   <p>Replayを閉じると現在の場面の既定曲に戻ります。</p>
   <button @click="audioControls?.enable()">録画の音楽を有効にする</button><button @click="audioControls?.stop()">録画の音楽を停止</button>
  </details>
 </section>
</template>
