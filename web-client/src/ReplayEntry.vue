<script setup>
import { defineAsyncComponent, h, onMounted, ref } from 'vue'
const props = defineProps(['sessionId','cursor','eligible','audioControls'])
const emit = defineEmits(['playing'])
const active = ref(false), action = ref('open'), intent=ref(null)
function enter(next) { intent.value={sessionId:props.sessionId,cursor:props.cursor}; action.value = next; active.value = true }
const ReplayLibrary = defineAsyncComponent({
  loader: () => import('./ReplayLibrary.vue'), timeout:15000, delay:0,
  loadingComponent:{render:()=>h('section',{class:'replay-library panel',role:'region','aria-label':'ローカルReplay',lang:'ja'},'Replayを読み込んでいます…')},
  errorComponent:{setup(){
    onMounted(()=>{action.value='cancel';intent.value=null})
    return ()=>h('section',{class:'replay-library panel',role:'region','aria-label':'ローカルReplay',lang:'ja'},[
    h('p',{role:'alert'},'Replayを読み込めません（REPLAY_LOAD_FAILED）。再読込後に再試行してください。対局は続けられます。'),
    h('button',{class:'quiet-button',onClick:()=>{active.value=false}},'Replayを閉じる'),
  ])}},
})
</script>
<template>
  <ReplayLibrary v-if="active" v-bind="props" :initial-action="action" :initial-intent="intent" @playing="emit('playing',$event)" />
  <section v-else class="replay-library panel" aria-label="ローカルReplay" lang="ja">
    <div class="claim-actions"><button class="quiet-button" aria-expanded="false" @click="enter('open')">Replay一覧</button>
      <button v-if="eligible" class="primary-button" data-testid="save-replay" @click="enter('save')">この対局をReplayに保存</button>
    </div>
  </section>
</template>
