<script setup>
import { ref } from 'vue'
import AssetImage from './AssetImage.vue'
const props=defineProps({day:Number,alive:Number,dead:Number,blocked:Boolean})
const emit=defineEmits(['menu'])
const content=ref(null)
function show(selector) {
  const target=content.value?.querySelector(selector)
  if(target){content.value.scrollTop+=target.getBoundingClientRect().top-content.value.getBoundingClientRect().top;target.focus({preventScroll:true})}
}
</script>
<template>
  <section class="desktop-vote-window" lang="ja" data-testid="desktop-vote-window">
    <AssetImage class="vote-background" name="images/komorebi002yuu.png" />
    <div class="desktop-vote-players"><slot name="players" /></div>
    <div class="desktop-calendar" role="status">{{ day }}日目<br>生存者:{{ alive }}<br>死亡者:{{ dead }}<br>吊り縄:{{ Math.floor((alive-1)/2) }}</div>
    <div ref="content" class="desktop-vote-information" role="region" aria-label="投票情報と指示" tabindex="0">
      <AssetImage class="vote-board-background" name="images/frame #19252.png" />
      <div class="vote-information-content"><slot name="information" /><slot name="orders" /><slot name="history" /></div>
    </div>
    <nav class="desktop-vote-actions" aria-label="昼の操作">
      <button class="desktop-art-button" :disabled="blocked" aria-label="メニュー" @click="emit('menu')"><AssetImage name="images/IntroTitle.png" /><span class="visually-hidden">メニュー</span></button>
      <slot name="avoidance" />
      <button class="desktop-art-button" aria-label="指示する" @click="show('.vote-orders')"><AssetImage name="images/shiji.png" /><span class="visually-hidden">指示する</span></button>
      <button class="desktop-art-button" aria-label="記録確認" @click="show('.public-information')"><AssetImage name="images/check.png" /><span class="visually-hidden">記録確認</span></button>
      <slot name="vote" />
    </nav>
  </section>
</template>
