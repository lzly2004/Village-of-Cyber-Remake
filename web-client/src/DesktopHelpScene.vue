<script setup>
import {computed,onBeforeUnmount,onMounted,ref} from 'vue'
import AssetImage from './AssetImage.vue'
import {helpGroups} from './helpContent.js'
import {configurations,roles} from './labels.js'
const root=ref(null)
function escape(event){if(event.key==='Escape'){event.preventDefault();emit('close')}}
onMounted(()=>{root.value?.querySelector('button')?.focus({preventScroll:true});window.addEventListener('keydown',escape)})
onBeforeUnmount(()=>window.removeEventListener('keydown',escape))
const props=defineProps({initialPage:String})
const emit=defineEmits(['close'])
const groupId=ref(props.initialPage==='configurations'?'1':''),topicId=ref(props.initialPage==='configurations'?'1-3':'')
const group=computed(()=>helpGroups.find(g=>g.id===groupId.value)),topic=computed(()=>group.value?.topics.find(t=>t.id===topicId.value))
const choices=computed(()=>group.value?group.value.topics:helpGroups)
const originals=import.meta.glob('../../src/main/resources/help/Info*-*-1.txt',{query:'?raw',import:'default',eager:true})
const original=computed(()=>topic.value?originals['../../src/main/resources/help/'+topic.value.source]:null)
function select(item){if(group.value)topicId.value=item.id;else groupId.value=item.id}
function back(){if(topicId.value)topicId.value='';else groupId.value=''}
</script>
<template>
 <section ref="root" lang="ja" aria-label="電脳村ガイド">
  <section class="desktop-information-scene desktop-help-scene" :class="{'desktop-help-root':!group}" data-testid="desktop-help" aria-label="説明">
   <AssetImage class="desktop-information-background" name="images/PVBG.png" />
   <AssetImage class="desktop-info-panel" name="images/avg1_resized(3).png" />
   <template v-if="!topic"><button v-for="(item,i) in choices" :key="item.id" class="desktop-help-choice" data-no-click-sound :style="{top:(10+70*(i+1))/7.2+'%'}" :aria-label="item.title" @click="select(item)"><AssetImage name="images/avg_button2.png" /><span class="desktop-help-choice-text">{{item.title}}</span></button></template>
   <article v-else class="desktop-help-body" tabindex="0" :aria-label="topic.title"><h3>{{topic.title}}</h3><p data-testid="desktop-help-body">{{topic.body}}</p></article>
   <button v-if="group" class="desktop-information-button desktop-help-return" data-no-click-sound data-testid="help-return" aria-label="前の説明へ戻る" @click="back"><AssetImage name="images/PVBreturn.png" /></button>
   <button class="desktop-information-button desktop-help-exit" data-no-click-sound data-testid="help-title-return" aria-label="タイトルに戻る" @click="emit('close','title')"><AssetImage name="images/PVBtitile.png" /></button>
  </section>
  <details class="desktop-browser-notes"><summary>Web版の補足・原文・配役説明</summary><p>既存の中国語ヘルプ36項目の日本語訳です。原版の日本語原文ではありません。説明の閲覧では開村・投票・保存をしません。</p><aside v-if="topic?.webNote"><strong>Web版の補足</strong><p>{{topic.webNote}}</p></aside><section v-if="topic" aria-label="参照した原文"><p>出典：help/{{topic.source}}</p><pre lang="zh-CN">{{original}}</pre></section><section aria-label="配役説明"><article v-for="item in configurations" :key="item.value" :data-configuration="item.value"><h3>{{item.name}}村 · {{item.playerCount}}人</h3><dl><template v-for="(count,role) in item.roleCounts" :key="role"><dt>{{roles[role]}}</dt><dd>{{count}}人</dd></template></dl></article></section></details>
 </section>
</template>
