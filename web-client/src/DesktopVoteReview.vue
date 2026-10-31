<script setup>
import { computed, ref, watch } from 'vue'
import AssetImage from './AssetImage.vue'
import DesktopRoster from './DesktopRoster.vue'
import DesktopCalendar from './DesktopCalendar.vue'
import { desktopVoteRound } from './desktopVoteText.js'
const props=defineProps({vote:Object,playerName:Function,blocked:Boolean,players:Array,markers:Map,day:Number,alive:Number,dead:Number})
const emit=defineEmits(['continue','menu','music'])
const index=ref(0)
watch(()=>props.vote,()=>{index.value=0})
const text=computed(()=>desktopVoteRound(props.vote,index.value,props.playerName))
function next(){if(props.blocked)return;if(index.value+1<props.vote.rounds.length)index.value++;else emit('continue')}
</script>
<template>
 <section class="desktop-vote-review" aria-label="投票結果" data-testid="desktop-vote-review">
  <AssetImage class="vote-background" name="images/komorebi002yuu.png" />
  <DesktopRoster :players="players" :markers="markers" />
  <DesktopCalendar :day="day" :alive="alive" :dead="dead" />
  <button class="desktop-art-button desktop-music-button" aria-label="ゲーム画面の曲を切替" :disabled="blocked" @click="emit('music')"><AssetImage name="images/musicBtn.png" /></button>
  <button class="desktop-art-button desktop-result-menu" aria-label="メニュー" :disabled="blocked" @click="emit('menu')"><AssetImage name="images/IntroTitle.png" /></button>
  <div class="desktop-result-board"><AssetImage class="vote-board-background" name="images/frame #19252.png" />
   <div v-if="text" class="desktop-result-content" tabindex="0">
    <div class="desktop-ballot-columns"><div><pre>{{ text.title }}</pre><div v-for="(line,i) in text.lines.slice(0,10)" :key="i">{{ line }}</div><div :style="{height:Math.max(0,10-text.lines.length)*2.34375+'cqw'}" aria-hidden="true"></div><p class="desktop-vote-conclusion">{{ text.conclusion }}</p></div><div><pre aria-hidden="true">{{ '\n\n' }}</pre><div v-for="(line,i) in text.lines.slice(10)" :key="i">{{ line }}</div></div></div>
   </div>
  </div>
  <button class="desktop-art-button desktop-result-next" :disabled="blocked" :aria-label="index+1<vote.rounds.length?'再投票結果を見る':'処刑場面へ進む'" data-testid="vote-review-continue" @click="next"><AssetImage :name="index+1<vote.rounds.length?'images/goTohyo.png':'images/nextDay.png'" /></button>
 </section>
</template>
