<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import AssetImage from './AssetImage.vue'
import { replayInformation, replayVotes } from './replayInformation.js'
import { replayRoles } from './replayPlayback.js'
import { roles } from './labels.js'
const props=defineProps(['replay','day'])
const emit=defineEmits(['dateLock'])
const mode=ref('info'),round=ref(0)
const info=computed(()=>replayInformation(props.replay,props.day))
const votes=computed(()=>replayVotes(info.value.current,props.day,round.value))
watch(()=>props.day,()=>{round.value=0;if(!votes.value)mode.value='info'})
watch([round,mode],()=>emit('dateLock',mode.value==='votes' && round.value>0),{immediate:true})
onBeforeUnmount(()=>emit('dateLock',false))
function show(next){mode.value=next;round.value=0}
const rules={0:'自由投票',1:'グレラン',2:'指定投票','-1':'投票方式の記録なし'}
</script>
<template>
  <section class="replay-information" aria-label="Replay情報・履歴" lang="ja">
    <h3>第{{ day }}日 · {{ info.terminal?'役職一覧':'情報・履歴' }}</h3>
    <p>保存済み録画の記録です。役職・未COの結果図も含み、進行中の対局の公開情報とは異なります。</p>
    <p v-for="d in info.diagnostics" :key="d.key" class="visual-diagnostic" role="status">表示診断 {{ d.section }}：{{ d.code }}（要求 第{{ d.requestedDay }}日<span v-if="d.sourceDay!==null">、記録 第{{ d.sourceDay }}日</span>）</p>
    <template v-if="info.terminal">
      <p>最終日の情報欄はデスクトップと同じ役職別一覧です。過去の情報・投票は日付を戻して確認できます。</p>
      <div v-if="info.groups.length" class="replay-role-groups" data-testid="replay-role-groups">
        <section v-for="group in info.groups" :key="group.role"><h4>{{ roles[replayRoles[group.role]] }}</h4><p>{{ group.players.map(p=>p.name).join('　') }}</p></section>
      </div>
      <p v-else role="alert">役職の記録を表示できません（ROLE_RECORD_MISSING）。日付切替と終了は利用できます。</p>
    </template>
    <template v-else>
      <div class="claim-actions">
        <button class="quiet-button" :aria-pressed="mode==='info'" @click="show('info')">情報・技能履歴</button>
        <button class="quiet-button" :aria-pressed="mode==='votes'" :disabled="!votes" @click="show('votes')">投票結果</button>
      </div>
      <p v-if="!votes">この日の投票記録はありません。</p>
      <pre v-if="mode==='info'" class="replay-history-text" data-testid="replay-normal-info">{{ info.normalText.replaceAll('破绽','破綻') }}</pre>
      <div v-else-if="votes" class="replay-votes" data-testid="replay-votes">
        <nav class="claim-actions" aria-label="Replay投票回切替">
          <button class="quiet-button" :disabled="votes.index===0" @click="round=votes.index-1">前の投票回</button>
          <span role="status">第{{ votes.round }}回（{{ votes.index+1 }} / {{ votes.total }}）</span>
          <button class="quiet-button" :disabled="votes.index===votes.total-1" @click="round=votes.index+1">次の投票回</button>
        </nav>
        <p v-if="round>0">日付を変える場合は第1回または情報・技能履歴に戻ってください。</p>
        <p>{{ rules[votes.rule] }}<span v-if="votes.candidates.length">：{{ votes.candidates.join('、') }}</span>（記録 第{{ info.current.dayNumber }}日）</p>
        <p>空欄の投票と0は、デスクトップと同じく棄権として表示します。死亡日当日の人物を含みます。</p>
        <table><caption>第{{ day }}日 第{{ votes.round }}回の投票記録</caption><thead><tr><th scope="col">人物</th><th scope="col">得票</th><th scope="col">投票先</th></tr></thead>
          <tbody><tr v-for="row in votes.rows" :key="row.position"><th scope="row">{{ row.name }}</th><td>{{ row.votes }}票</td><td>{{ row.targetName }}</td></tr></tbody>
        </table>
      </div>
    </template>
    <details class="replay-skill-records">
      <summary>各日の結果図（{{ info.icons.length }}件）</summary>
      <p>結果図は各日の記録値、情報欄は表示日までの技能履歴を使います。記録時点の違いで一致しない場合があります。図がない場合も対象と結果は文字で確認できます。</p>
      <p v-if="!info.icons.length">この日までの結果図の記録はありません。</p>
      <ul aria-label="Replay結果図一覧"><li v-for="(item,i) in info.icons" :key="i">
        <span>第{{ item.day }}日（記録 第{{ item.sourceDay }}日）：{{ item.name }} · {{ roles[replayRoles[item.role]] }} {{ item.order }} → {{ item.targetName }}{{ item.black?'●':'○' }}</span>
        <AssetImage v-if="item.icon" :name="item.icon" class="replay-skill-icon" />
        <span v-else class="visual-diagnostic">結果図なし（SKILL_ICON_UNAVAILABLE）</span>
      </li></ul>
    </details>
  </section>
</template>
