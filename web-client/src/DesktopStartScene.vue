<script setup>
import AssetImage from './AssetImage.vue'
import { configurations } from './labels.js'
defineProps({ scene: String, blocked: Boolean })
const emit = defineEmits(['navigate', 'start'])
const buttons = [
  ['start', '対局を始める', 'startButton'], ['continue', '続きから', 'continueButton'],
  ['replay', '録画を見る', 'replayButton'], ['records', '戦績', 'recordButton'],
  ['help', '説明', 'infoButton'], ['collections', 'コレクション', 'collectionsButton'],
]
</script>
<template>
  <section class="desktop-start-surface" :data-testid="'desktop-'+scene" lang="ja" :aria-label="scene==='select'?'配役選択':'タイトル'">
    <AssetImage class="desktop-start-background" name="images/title_base_resized.png" />
    <template v-if="scene==='title'">
      <AssetImage class="desktop-title-logo" name="images/titleLogo.png" />
      <button v-for="([id,label,image],i) in buttons" :key="id" class="desktop-art-button desktop-title-button" :data-testid="'title-'+id" :aria-label="label" :disabled="blocked" :data-no-click-sound="id==='start' ? '' : undefined" :style="{left:(660+i%2*300)/1280*100+'%',top:(400+Math.floor(i/2)*80)/720*100+'%'}" @click="emit('navigate',id)"><AssetImage :name="'images/'+image+'.png'" /></button>
    </template>
    <template v-else>
      <button v-for="(item,i) in configurations" :key="item.value" class="desktop-art-button desktop-setup-button" :data-testid="'setup-'+item.value" :aria-label="item.name+'配役で開始'" :disabled="blocked" :style="{left:(30+Math.floor((i+1)/5)*465)/1280*100+'%',top:(20+i%4*168)/720*100+'%'}" @click="emit('start',item.value)"><AssetImage :name="item.sourceImage" /></button>
      <button class="desktop-art-button desktop-setup-return" data-testid="setup-return" data-no-click-sound aria-label="タイトルに戻る" :disabled="blocked" @click="emit('navigate','title')"><AssetImage name="images/PVBtitile.png" /></button>
    </template>
  </section>
</template>
