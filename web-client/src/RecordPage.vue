<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { createRecordStore, recordConfigurations } from './recordStore.js'
import { configurationName } from './labels.js'
const emit=defineEmits(['close'])
const dialog=ref(null), record=ref(null), selected=ref(0), loading=ref(false), error=ref('')
const store=createRecordStore()
let version=0
const opener=document.activeElement
const pages=['総合',...recordConfigurations.map(key=>configurationName(key)+'村')]
const values=computed(()=>{
  if(!record.value)return null
  const r=record.value,i=selected.value
  return i===0 ? {play:r.totalPlayCnt,rate:r.totalWinRate,max:r.totalMaxStreak,village:r.totalVillageWin,wolf:r.totalWolfWin,fox:r.totalFoxWin}
    : {play:r.playcnt[i],rate:r.winrate[i],max:r.maxStreak[i],current:r.currentStreak[i],village:r.villageWincnt[i],wolf:r.wolfWincnt[i],fox:r.foxWincnt[i]}
})
async function refresh() {
  const current=++version
  loading.value=true;error.value='';record.value=null
  try { const result=await store.read(); if(current===version)record.value=result }
  catch(e) { if(current===version)error.value=['INVALID_HISTORY','STORAGE_UNAVAILABLE','STORAGE_BLOCKED','STORAGE_TIMEOUT'].includes(e.code)?e.code:'STORAGE_UNAVAILABLE' }
  finally { if(current===version)loading.value=false }
}
function keepFocus(event) {
  if(event.key!=='Tab')return
  const nodes=[...dialog.value.querySelectorAll('button:not(:disabled)')].filter(node=>node.getClientRects().length)
  const first=nodes[0],last=nodes.at(-1)
  if((event.shiftKey&&document.activeElement===first)||(!event.shiftKey&&document.activeElement===last)){
    event.preventDefault();(event.shiftKey?last:first)?.focus()
  }
}
onMounted(()=>{dialog.value.showModal();refresh()})
onBeforeUnmount(()=>{version++;dialog.value?.close();opener?.focus()})
</script>
<template>
  <dialog ref="dialog" class="record-page" aria-labelledby="record-title" aria-describedby="record-scope" lang="ja" @cancel.prevent="emit('close')" @keydown="keepFocus">
    <header class="record-heading"><h2 id="record-title">ブラウザーの戦績</h2><button autofocus class="quiet-button" @click="emit('close')">戦績を閉じる</button></header>
    <p id="record-scope">同じブラウザー・同じサイト内で保存が完了した対局だけの累計です。利用者間で共有されます。Replayの再生・取り込みや、進行中の対局は加算しません。</p>
    <p>勝率・連勝は村人陣営の勝利で計算します。他の配役の結果は、その村の現在の連勝を中断しません。総合の最大連勝は七配役の最大値です。</p>
    <p>画面に収まらない場合は、枠内を上下にスクロールできます。</p>
    <nav class="record-pages" aria-label="戦績の配役"><button v-for="(name,index) in pages" :key="name" :aria-pressed="selected===index" @click="selected=index">{{ name }}</button></nav>
    <div class="record-refresh"><button :disabled="loading" @click="refresh">戦績を再読み込み</button><span>表示は読み込み時点の記録です。別のタブで保存した場合は再読み込みしてください。</span></div>
    <p v-if="loading" role="status">戦績を読み込んでいます。</p>
    <section v-else-if="error" role="alert"><p>戦績を読み込めません（{{ error }}）。保存済みの記録は削除・修復しません。</p><p v-if="error==='INVALID_HISTORY'">記録に不整合があります。この画面からの復旧にはまだ対応していません。サイトデータを削除すると、他の保存データも失う場合があります。</p><p>再読み込みを試すか、この画面を閉じてください。対局の操作は続けられます。</p></section>
    <section v-else-if="values" aria-labelledby="record-selected">
      <h3 id="record-selected">{{ pages[selected] }}</h3>
      <p v-if="values.play===0" role="status">この集計には、まだ保存済みの対局がありません。</p>
      <dl class="record-metrics">
        <div><dt>対局数</dt><dd data-testid="record-play">{{ values.play }}回</dd></div>
        <div><dt>村人陣営の勝率</dt><dd data-testid="record-rate">{{ values.rate.toFixed(5) }}%</dd></div>
        <div><dt>最大連勝</dt><dd data-testid="record-max">{{ values.max }}回</dd></div>
        <div v-if="selected!==0"><dt>現在の連勝</dt><dd data-testid="record-current">{{ values.current }}回</dd></div>
        <div><dt>村人陣営の勝利</dt><dd data-testid="record-village">{{ values.village }}回</dd></div>
        <div><dt>人狼陣営の勝利</dt><dd data-testid="record-wolf">{{ values.wolf }}回</dd></div>
        <div><dt>妖狐陣営の勝利</dt><dd data-testid="record-fox">{{ values.fox }}回</dd></div>
      </dl>
    </section>
    <p>別のブラウザー・端末やデスクトップ版とは同期しません。サイトデータの削除・ブラウザーの制限で失われる場合があります。</p>
  </dialog>
</template>
