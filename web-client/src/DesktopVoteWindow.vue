<script setup>
import { computed, ref, nextTick, onBeforeUnmount } from 'vue'
import AssetImage from './AssetImage.vue'
import DesktopRoster from './DesktopRoster.vue'
import DesktopCalendar from './DesktopCalendar.vue'
import DesktopInformation from './DesktopInformation.vue'
import DesktopTargetSelection from './DesktopTargetSelection.vue'
import { desktopVoteRound } from './desktopVoteText.js'
import { suspicionText } from './publicInformationText.js'
import { configurations } from './labels.js'
const props=defineProps({day:Number,alive:Number,dead:Number,blocked:Boolean,update:Object,players:Array,bindings:Object,playerName:Function,voteTargets:Array,legalTargets:Array,toggleTarget:Function,markers:Map})
const emit=defineEmits(['menu','music'])
const mode=ref('home'),historyIndex=ref(0),message=ref('')
let tipTimer
function tip(text){clearTimeout(tipTimer);message.value=text;tipTimer=setTimeout(()=>{message.value=''},1000)}
onBeforeUnmount(()=>clearTimeout(tipTimer))
const b=computed(()=>props.bindings)
const historySetup=computed(()=>configurations.findIndex(c=>c.value===props.update.view.configuration)+1)
const historySetupWidths=[0,155,206,206,232,232,232,255]
const historyDay=ref(null)
const rounds=computed(()=>props.update.voteHistory.filter(vote=>vote.day===historyDay.value).flatMap(vote=>vote.rounds.map((round,index)=>({vote,index}))))
function chooseDay(day){historyDay.value=day;historyIndex.value=0}
const historyText=computed(()=>{const r=rounds.value[historyIndex.value];return r?desktopVoteRound(r.vote,r.index,props.playerName,{history:true}):null})
async function open(value){mode.value=value;message.value='';await nextTick();document.querySelector('[data-testid="desktop-vote-window"] .vote-information-content')?.focus({preventScroll:true})}
async function back(){
 const previous=mode.value
 if(previous==='claimPlayers'&&b.value.canAskClaimPlayers)await b.value.askClaimPlayers()
 await open('home');await nextTick()
 const label=['records','suspicion','history'].includes(previous)?'記録確認':previous==='voteConfirm'?'投票へ':'指示する'
 document.querySelector('[data-testid="desktop-vote-window"] [aria-label="'+label+'"]')?.focus({preventScroll:true})
}
const buttons=computed(()=>{
 const list=[{slot:0,label:'メニュー',image:'IntroTitle',click:()=>emit('menu')}]
 const add=(slot,label,image,click,extra={})=>list.push({slot,label,image,click,...extra})
 if(mode.value==='home'){
  add(2,'指示する','shiji',()=>open('orders'));add(3,'記録確認','check',()=>{if(props.day===2){tip('まだ特に疑い先もなく、\n投票の履歴もないようだ。');return}open('records')});add(4,'投票へ','goTohyo',()=>open('voteConfirm'),{disabled:!b.value.canDecide})
 }else{
  if(mode.value==='orders'){add(2,'COを聞く','doCO',()=>open('claims'));add(3,'指定する','doShitei',()=>open('designations'))}
  if(mode.value==='claims'){
   for(const [role,label,image,slot,left] of [['zhan','占い師','uranaiCO',2,true],['ling','霊能者','reiCO',2,false],['lie','狩人','kariCO',3,false],['gong','共有','kyouyuCO',3,true],['mao','猫又','catCO',4,true]])add(slot,label+'のCOを聞く',image,()=>{if(b.value.claimRoles.includes(role))b.value.askClaimRole(role);else tip(label+'はいないようだ')},{left,disabled:!b.value.canDecide})
   add(1,'プレイヤーのCOを聞く','询问CO',()=>open('claimPlayers'))
  }
  if(mode.value==='designations'){
   add(2,'指定投票','tohyoShitei',()=>open('vote'));add(3,'占い指定','shiteiUranai',()=>open('divination'));add(4,'護衛指定','shiteiGoei',()=>open('guard'))
  }
  if(mode.value==='voteConfirm'){
   if(props.voteTargets.length)add(2,'指定投票を確定','tohyoShitei',()=>b.value.vote('DESIGNATED'),{test:'vote-DESIGNATED',disabled:!b.value.canDecide||!props.update.legalVoteActions.some(a=>a.method==='DESIGNATED')})
   else for(const action of b.value.alternativeVotes)add(action.method==='GREY'?2:3,action.method==='GREY'?'グレー投票':'自由投票',action.method==='GREY'?'tohyoGrey':'tohyoFree',()=>b.value.vote(action.method),{test:'vote-'+action.method,disabled:!b.value.canDecide})
  }
  if(mode.value==='records'){add(2,'疑い先','checkUtagai',()=>open('suspicion'));add(3,'投票履歴','checkTohyo',()=>{historyDay.value=null;historyIndex.value=0;open('history')})}
  // The desktop designation sub-menu has three buttons; selecting a scene restores Return.
  if(mode.value==='history'&&historyDay.value!==null){if(historyIndex.value>0)add(2,'前の投票結果','rirekiBack',()=>historyIndex.value--);if(historyIndex.value+1<rounds.value.length)add(3,'次の投票結果','rirekiNext',()=>historyIndex.value++)}
  if(mode.value!=='designations')add(4,'戻る','return',back)
 }
 return list
})
const selection=computed(()=>({vote:{targets:props.legalTargets,selected:props.voteTargets,toggle:props.toggleTarget},divination:{targets:b.value.divinationTargets},guard:{targets:b.value.guardTargets},claimPlayers:{targets:b.value.claimPlayers,selected:b.value.claimPlayerDraft,toggle:b.value.toggleClaimPlayer}})[mode.value])
const selectionSummary=computed(()=>{
 let text='-指定内容-\n'
 if(props.voteTargets.length)text+='[指定投票]\n'+props.voteTargets.map(props.playerName).join(' ')+'\n'
 for(const [key,label] of [['divination','指定占い'],['guard','指定護衛']]){
  const s=props.update.selections[key];if(!s.hiddenTargets.length&&!s.claimedActors.some(a=>a.targets.length))continue
  text+='['+label+']\n'
  for(const actor of s.claimedActors)if(actor.targets.length)text+=props.playerName(actor.playerNumber)+'→'+actor.targets.map(props.playerName).join(',')+'\n'
  if(s.hiddenTargets.length)text+='潜伏→'+s.hiddenTargets.map(props.playerName).join(',')+'\n'
 }
 return text
})
</script>
<template>
 <section class="desktop-vote-window" :data-menu="mode" :data-command-blocked="!b.canDecide" lang="ja" data-testid="desktop-vote-window">
  <AssetImage class="vote-background" name="images/komorebi002yuu.png" />
  <button class="desktop-art-button desktop-music-button" aria-label="ゲーム画面の曲を切替" :disabled="blocked" @click="emit('music')"><AssetImage name="images/musicBtn.png" /></button>
  <DesktopRoster :players="players" :markers="markers" />
  <DesktopCalendar :day="day" :alive="alive" :dead="dead" />
  <div class="desktop-vote-information" role="region" aria-label="投票情報と指示">
   <AssetImage class="vote-board-background" name="images/frame #19252.png" />
   <div class="vote-information-content" tabindex="-1">
    <DesktopInformation v-if="!selection && !['voteConfirm','history','suspicion'].includes(mode)" :information="update.publicInformation" :player-name="playerName" />
    <DesktopTargetSelection v-else-if="selection" :key="mode" :type="mode==='claimPlayers'?'claim':mode" :players="players" :bindings="bindings" :player-name="playerName" :markers="markers" :targets="selection.targets" :selected="selection.selected||[]" :toggle-target="selection.toggle" />
    <pre v-else-if="mode==='voteConfirm'" class="desktop-selection-summary">{{ selectionSummary }}</pre>
    <div v-else-if="mode==='suspicion'" class="desktop-suspicion desktop-ballot-columns" role="region" aria-label="疑い先"><div><pre>{{ '- 疑い先 - 全体表示\n\n' }}</pre><template v-if="update.publicInformation?.suspicion.available"><div v-for="row in update.publicInformation.suspicion.rows.slice(0,10)" :key="row.playerNumber" :data-testid="'suspicion-'+row.playerNumber">{{ suspicionText(row,playerName) }}</div></template><p v-else>第3日以降に公開されます。</p></div><div><pre aria-hidden="true">{{ '\n\n' }}</pre><div v-for="row in update.publicInformation?.suspicion.rows.slice(10) || []" :key="row.playerNumber" :data-testid="'suspicion-'+row.playerNumber">{{ suspicionText(row,playerName) }}</div></div></div>
    <section v-else-if="mode==='history'" class="desktop-history" aria-label="投票履歴">
     <div v-if="historyText" class="desktop-selection-summary desktop-ballot-columns"><div><pre>{{ historyText.title }}</pre><span class="visually-hidden history-summary">{{ historyText.conclusion }}</span><div v-for="(line,i) in historyText.lines.slice(0,10)" class="history-line" :key="i">{{ line }}</div></div><div><pre aria-hidden="true">{{ '\n\n' }}</pre><div v-for="(line,i) in historyText.lines.slice(10)" class="history-line" :key="i">{{ line }}</div></div></div>
     <p v-else-if="!update.voteHistory.length">投票履歴はありません。</p>
    </section>
   </div>
  </div>
  <div v-if="mode==='history'&&historyDay===null" class="desktop-history-days" role="group" aria-label="投票履歴の日付"><button v-for="vote in update.voteHistory" :key="vote.day" class="desktop-art-button" :aria-label="vote.day+'日目の投票結果'" @click="chooseDay(vote.day)"><AssetImage :name="'images/'+vote.day+'day.png'" /></button></div>
  <AssetImage v-if="mode==='history'&&historySetup" class="desktop-history-setup" :name="'images/game'+historySetup+'-1.png'" :style="{width:historySetupWidths[historySetup]/1280*100+'%'}" />
  <p v-if="message" class="desktop-transient-tip" role="status">{{ message }}</p>
  <nav class="desktop-vote-actions" aria-label="昼の操作">
   <template v-for="button in buttons" :key="button.label"><button class="desktop-art-button" :class="['desktop-slot-'+button.slot,{'desktop-claim-seer':button.left}]" :aria-label="button.label" :data-testid="button.test || (button.label==='投票へ'?'vote':undefined)" :disabled="blocked||button.disabled" @click="button.click"><AssetImage :name="'images/'+button.image+'.png'" /></button></template>
   <slot v-if="mode==='home'" name="avoidance" />
  </nav>
 </section>
</template>
