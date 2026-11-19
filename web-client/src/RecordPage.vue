<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { createRecordStore, recordConfigurations } from './recordStore.js'
import { configurationName } from './labels.js'
import AssetImage from './AssetImage.vue'
const props=defineProps({desktop:Boolean})
const emit=defineEmits(['close'])
const root=ref(null), dialog=ref(null), record=ref(null), selected=ref(0), loading=ref(false), error=ref('')
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
function escape(event){if(props.desktop&&event.key==='Escape'){event.preventDefault();emit('close')}}
onMounted(()=>{if(!props.desktop)dialog.value.showModal();else {root.value?.querySelector('button')?.focus({preventScroll:true});window.addEventListener('keydown',escape)}refresh()})
onBeforeUnmount(()=>{window.removeEventListener('keydown',escape);version++;dialog.value?.close();opener?.focus()})
</script>
<template>
  <section v-if="desktop" ref="root" lang="ja" aria-label="ブラウザーの戦績">
    <section class="desktop-information-scene desktop-record-scene" data-testid="desktop-records" aria-label="戦績">
      <AssetImage class="desktop-information-background" name="images/title_base_resized.png" />
      <AssetImage class="desktop-record-title" :name="'images/gameTitle'+selected+'.png'" />
      <h3 class="visually-hidden">{{pages[selected]}}</h3>
      <p v-if="loading" class="desktop-record-status" role="status">戦績を読み込んでいます。</p>
      <p v-else-if="error" class="desktop-record-status" role="alert">戦績を読み込めません（{{error}}）。保存済みの記録は削除・修復しません。<template v-if="error==='INVALID_HISTORY'">この画面からの復旧にはまだ対応していません。</template></p>
      <template v-else-if="values">
        <template v-for="(image,i) in ['SE_playNumber','SE_WinRate','SE_MaxWin','SE_WinNumber']" :key="image"><AssetImage class="desktop-record-label" :style="{top:(120+115*i)/7.2+'%'}" :name="'images/'+image+'.png'" /></template>
        <p class="desktop-record-value" style="top:24.305556%" data-testid="record-play">{{values.play}}回</p>
        <p class="desktop-record-value" style="top:40.277778%" data-testid="record-rate">{{values.rate.toFixed(5)}}%</p>
        <p class="desktop-record-value" style="top:56.25%"><span data-testid="record-max">{{values.max}}</span><template v-if="selected!==0">/Now:<span data-testid="record-current">{{values.current}}</span></template></p>
        <template v-for="(key,i) in ['village','wolf','fox']" :key="key"><AssetImage class="desktop-record-faction" :style="{top:(520+42*i)/7.2+'%'}" :name="'images/SE_WinNumber'+(i+1)+'.png'" /><p class="desktop-record-faction-value" :style="{top:(522+42*i)/7.2+'%'}" :data-testid="'record-'+key">{{values[key]}}回</p></template>
      </template>
      <button class="desktop-information-button desktop-record-next" aria-label="次の配役" data-testid="record-next" @click="selected=(selected+1)%8"><AssetImage name="images/PVBnext.png" /></button>
      <button v-if="selected>0" class="desktop-information-button desktop-record-return" aria-label="総合へ戻る" data-testid="record-return" @click="selected=0"><AssetImage name="images/PVBreturn.png" /></button>
      <button class="desktop-information-button desktop-record-exit" aria-label="タイトルに戻る" data-no-click-sound data-testid="record-title-return" @click="emit('close','title')"><AssetImage name="images/PVBtitile.png" /></button>
    </section>
    <details class="desktop-browser-notes"><summary>ブラウザーの戦績・保存範囲</summary><p>同じブラウザー・同じサイト内で保存が完了した対局だけの累計です。利用者間で共有されます。Replayの再生・取り込みや進行中の対局は加算しません。別のブラウザーやデスクトップ版とは同期しません。</p><p>勝率・連勝は村人陣営の勝利で計算します。総合の最大連勝は七配役の最大値です。別のタブで保存した場合は再読み込みしてください。</p><p v-if="error==='INVALID_HISTORY'">記録に不整合があります。この画面からの復旧にはまだ対応していません。保存済みの記録は変更していません。</p><button :disabled="loading" @click="refresh">戦績を再読み込み</button><p v-if="values?.play===0">この集計には、まだ保存済みの対局がありません。</p></details>
  </section>

  <dialog v-else ref="dialog" class="record-page" aria-labelledby="record-title" aria-describedby="record-scope" lang="ja" @cancel.prevent="emit('close')" @keydown="keepFocus">
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
