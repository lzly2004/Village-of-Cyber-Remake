<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { completedRecord, createRecordStore } from './recordStore.js'
const props=defineProps({sessionId:String,update:Object,eligible:Boolean})
const state=ref(''), code=ref('')
const store=createRecordStore()
let version=0
async function save() {
  const current=++version
  state.value=''; code.value=''
  if (!props.eligible) return
  state.value='saving'
  try {
    const intent=completedRecord(props.sessionId,props.update,true)
    const result=await store.record(intent)
    if (current===version) state.value=result.duplicate?'duplicate':'saved'
  } catch(error) {
    if(current!==version)return
    state.value='error'
    code.value=['INVALID_RESULT','INVALID_HISTORY','RESULT_CONFLICT','HASH_UNAVAILABLE','QUOTA_EXCEEDED','STORAGE_UNAVAILABLE','STORAGE_BLOCKED','STORAGE_TIMEOUT'].includes(error.code)?error.code:'STORAGE_UNAVAILABLE'
  }
}
watch([()=>props.sessionId,()=>props.eligible,()=>props.update?.view?.configuration,()=>props.update?.view?.result],save,{immediate:true})
onBeforeUnmount(()=>{version++})
</script>
<template>
  <section v-if="state" class="notice" aria-label="ローカル戦績保存" lang="ja">
    <p role="status" v-if="state==='saving'">この対局の戦績を保存しています。</p>
    <p role="status" v-else-if="state==='saved'">この対局の戦績をブラウザーに保存しました。</p>
    <p role="status" v-else-if="state==='duplicate'">この対局の戦績は保存済みです。重複して加算しません。</p>
    <div v-else role="alert"><p>戦績を保存できません（{{ code }}）。保存完了とは扱いません。対局の操作は続けられます。</p><button @click="save">戦績保存を再試行</button></div>
    <p>同じブラウザー・同じサイト内の累計です。新しい対局でも残ります。別の利用者と共有され、サイトデータの削除やブラウザーの制限で失われる場合があります。Replayの再生・取り込みは加算しません。</p>
  </section>
</template>
