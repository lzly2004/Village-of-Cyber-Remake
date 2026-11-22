<script setup>
import { computed,nextTick,onBeforeUnmount,ref,watch } from 'vue'
import { createReplayStore } from './replayStore.js'
import { readReplayFile,exportReplayFile,downloadReplayBytes } from './replayFiles.js'
import { configurationName } from './labels.js'
const props=defineProps({slots:Array,disabled:Boolean})
const emit=defineEmits(['saved','notice'])
const input=ref(null),dialog=ref(null),phase=ref(null),busy=ref(false),problem=ref(''),candidates=ref([]),selected=ref(0),target=ref(0),expectedId=ref(null)
const candidate=computed(()=>candidates.value[Number(selected.value)])
const available=computed(()=>props.slots.map((entry,index)=>({entry,index})).filter(row=>row.entry))
let opener,alive=true
onBeforeUnmount(()=>{alive=false})
watch(phase,async value=>{await nextTick();if(value){if(dialog.value&&!dialog.value.open)dialog.value.showModal()}else if(opener?.isConnected)opener.focus({preventScroll:true})})
const codes=new Set(['INVALID_JSON','INVALID_UTF8','UNSUPPORTED_VERSION','FILE_TIMEOUT','INVALID_REPLAY','LEGACY_COMPATIBILITY_DIAGNOSTIC','QUOTA_EXCEEDED','LIBRARY_FULL','SLOT_CHANGED','INVALID_LIBRARY','REPLAY_TOO_LARGE','STORAGE_BLOCKED','STORAGE_TIMEOUT','STORAGE_UNAVAILABLE','HASH_UNAVAILABLE'])
async function run(action){
 if(busy.value||props.disabled)return
 busy.value=true;problem.value=''
 try{await action()}catch(error){if(alive){problem.value='録画ファイルの操作を完了できません（'+(codes.has(error?.code)?error.code:'FILE_OPERATION_FAILED')+'）。自動修復や上書きはしていません。';if(!phase.value)phase.value='error'}}finally{busy.value=false}
}
function chooseFile(event){opener=event.currentTarget;input.value.click()}
function parse(event){
 const file=event.target.files?.[0];event.target.value=''
 if(!file)return
 return run(async()=>{const values=await readReplayFile(file);if(alive){candidates.value=values;selected.value=0;phase.value='preview'}})
}
function chooseTarget(){if(candidate.value&&!candidate.value.readOnly){target.value=0;phase.value='target'}}
function confirmTarget(){expectedId.value=props.slots[Number(target.value)]?.id??null;phase.value='confirm'}
function save(){return run(async()=>{
 const slot=Number(target.value),record=candidate.value
 if(!record||record.readOnly)return
 const result=await createReplayStore().saveToSlot(JSON.parse(JSON.stringify(record.replay)),slot,expectedId.value)
 if(alive){emit('saved');emit('notice',result.duplicate?'同じ録画がこの保存枠に保存済みです。':'保存枠 '+slot+' に録画を取り込みました。');phase.value=null}
})}
function chooseExport(event){opener=event.currentTarget;selected.value=available.value[0]?.index??0;phase.value=available.value.length?'export':'empty'}
function exportOne(){return run(async()=>{
 const entry=props.slots[Number(selected.value)]
 if(!entry)throw Object.assign(new Error(),{code:'SLOT_CHANGED'})
 downloadReplayBytes(exportReplayFile([JSON.parse(JSON.stringify(entry.replay))]));phase.value=null
})}
</script>
<template>
 <div class="desktop-replay-exchange">
  <button class="desktop-file-button" data-no-click-sound data-testid="desktop-replay-import" :disabled="busy||disabled" @click="chooseFile">録画ファイル読込</button>
  <button class="desktop-file-button export" data-no-click-sound data-testid="desktop-replay-export" :disabled="busy||disabled" @click="chooseExport">録画ファイル書出</button>
  <input ref="input" class="visually-hidden" tabindex="-1" aria-hidden="true" type="file" accept=".json,application/json" aria-label="保存枠へ取り込むファイル" :disabled="busy||disabled" @change="parse">
  <dialog v-if="phase" ref="dialog" class="replay-slot-confirm" aria-label="録画ファイル交換" @cancel.prevent="!busy&&(phase=null)">
   <p v-if="problem" role="alert">{{problem}}</p>
   <template v-if="phase==='preview'">
    <p>ファイルを検証しました。取り込む終局録画を選んでください。まだ保存していません。</p>
    <label>取り込む録画<select v-model="selected" :disabled="busy"><option v-for="(c,i) in candidates" :key="c.fileSlot" :value="i">ファイル位置{{c.fileSlot}} · {{configurationName(c.replay.peiyiName)}}村 · {{c.replay.totalDays}}日 · v{{c.sourceVersion}}{{c.readOnly?'（未完・概要のみ）':''}}</option></select></label>
    <p v-if="candidate?.readOnly">未完録画は概要のみです。保存・対局再開・戦績加算はしません。</p>
    <button data-no-click-sound :disabled="busy||!candidate||candidate.readOnly" @click="chooseTarget">保存枠を選ぶ</button>
   </template>
   <template v-else-if="phase==='target'">
    <p>ローカルの保存枠を選んでください。ファイル内の位置は保存先を決めません。</p>
    <label>保存先<select v-model="target" :disabled="busy"><option v-for="(entry,i) in slots" :key="i" :value="i">保存枠 {{i}} {{entry?'（録画あり・確認後に置き換え）':'（空き）'}}</option></select></label>
    <button data-no-click-sound :disabled="busy" @click="confirmTarget">内容を確認</button>
   </template>
   <template v-else-if="phase==='confirm'">
    <p>ファイル位置{{candidate.fileSlot}}の録画を保存枠{{target}}へ書き込みますか？{{expectedId?'以前の録画を置き換えます。':'空き枠に保存します。'}}キャンセルは書き込みません。</p>
    <button data-no-click-sound :disabled="busy" @click="save">書き込みを確定</button>
   </template>
   <template v-else-if="phase==='export'">
    <p>一局を九槽ファイルの位置0に書き出します。元の保存枠は変えません。</p>
    <label>書き出す録画<select v-model="selected" :disabled="busy"><option v-for="row in available" :key="row.index" :value="row.index">保存枠 {{row.index}} · {{configurationName(row.entry.replay.peiyiName)}}村 · {{row.entry.replay.saveTime}}</option></select></label>
    <button data-no-click-sound :disabled="busy" @click="exportOne">ファイルを書き出す</button>
   </template>
   <p v-else-if="phase==='empty'">書き出せる録画はありません。</p>
   <button data-no-click-sound :disabled="busy" @click="phase=null">{{['empty','error'].includes(phase)?'閉じる':'キャンセル'}}</button>
  </dialog>
 </div>
</template>
