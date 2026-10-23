<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AssetImage from './AssetImage.vue'
import { replayDay, replayFrame, replayBackground } from './replayPlayback.js'
import { validateReplay } from './replayValidation.js'
import { characterDisplayName } from './characterDisplay.js'
import { configurationName, roles } from './labels.js'
const props = defineProps(['replay','audioControls'])
const emit = defineEmits(['close','playing'])
const dialog = ref(null), selectedDay = ref(2), valid = ref(false)
let recording
try { recording=validateReplay(props.replay); selectedDay.value=replayDay(recording); valid.value=true } catch { /* Bounded diagnostic below, never substitute a new game. */ }
const audioState=computed(()=>props.audioControls?.state?.() || {status:'off'})
const frame = computed(()=>valid.value ? replayFrame(recording,selectedDay.value) : null)
const days = computed(()=>frame.value ? Array.from({length:frame.value.lastDay-frame.value.firstDay+1},(_,i)=>frame.value.firstDay+i) : [])
function close() { emit('close') }
onMounted(()=>{dialog.value.showModal();emit('playing',true)})
onBeforeUnmount(()=>{emit('playing',false);dialog.value?.close()})
</script>
<template>
  <dialog ref="dialog" class="replay-player" aria-labelledby="replay-player-title" lang="ja" @cancel.prevent="close">
    <header class="replay-player-header"><div><h2 id="replay-player-title">Replay再生<span v-if="valid"> · {{ configurationName(recording.peiyiName) }}村</span></h2>
      <p>録画の閲覧です。役職を含み、進行中の対局や戦績は変更しません。</p></div>
      <button class="quiet-button" autofocus @click="close">Replayを閉じる</button>
    </header>
    <p v-if="!valid" role="alert">録画を表示できません（INVALID_REPLAY）。元の保存データは変更していません。</p>
    <template v-else>
      <nav class="replay-navigation" aria-label="Replay日付切替">
        <button class="quiet-button" :disabled="frame.day<=frame.firstDay" @click="selectedDay=frame.day-1">前の日</button>
        <label>再生日<select :value="frame.day" @change="selectedDay=Number($event.target.value)"><option v-for="day in days" :key="day" :value="day">第{{ day }}日</option></select></label>
        <button class="quiet-button" :disabled="frame.day>=frame.lastDay" @click="selectedDay=frame.day+1">次の日</button>
        <span role="status" data-testid="replay-day">第{{ frame.day }}日 / {{ frame.lastDay }}日</span>
      </nav>
      <div class="replay-surface">
        <AssetImage class="replay-background" :name="replayBackground" />
        <div class="replay-record-content">
          <p>人物・CO：第{{ frame.avatarDay ?? '不明' }}日の記録（次の日を優先）。死亡印は選択日より前の離場を示します。</p>
          <p v-for="d in frame.diagnostics" :key="d.section+':'+d.code" class="visual-diagnostic" role="status">表示診断 {{ d.section==='data'?'人数':'人物' }}：{{ d.code }}<span v-if="d.sourceDay!==null">（第{{ d.sourceDay }}日の記録を代替表示）</span></p>
          <p id="replay-player-scroll">人物が多い場合は枠内をスクロールできます。Tabで枠や各人物に移動できます。</p>
          <div class="replay-avatar-list" role="region" aria-label="Replay人物一覧" aria-describedby="replay-player-scroll" tabindex="0" :style="{'--replay-columns':Math.ceil(frame.players.length/2)||1}">
            <article v-for="player in frame.players" :key="player.playerIndex" class="replay-player-card" :data-player-index="player.playerIndex" :data-portrait="player.portrait" :data-departed="player.departed" tabindex="0">
              <span class="player-portrait"><AssetImage class="portrait-image" :name="player.portrait" />
                <AssetImage v-if="player.claimIcon" class="claim-icon" :name="player.claimIcon" />
                <AssetImage v-if="player.deathIcon" class="death-marker" :name="player.deathIcon" />
                <AssetImage v-if="player.characterText" class="character-title-image" :name="player.characterText" />
              </span>
              <strong>{{ player.playerIndex }} · {{ characterDisplayName(player.character) }}</strong><span>実際の役職 · {{ roles[player.actualRole] }}</span>
              <span>{{ player.claimText }} {{ player.claimOrder }}</span><span>{{ player.departed?'離場済み':'死亡印なし' }}</span>
              <small v-if="player.claimDiagnostic || player.deathDiagnostic" class="visual-diagnostic">表示診断：CO図または死亡図を表示できません。</small>
            </article>
          </div>
          <section aria-label="Replay人数" class="replay-population">
            <h3>第{{ frame.day }}日</h3><p v-if="frame.population" data-testid="replay-population">生存者 {{ frame.population.alive }} · 死亡者 {{ frame.population.dead }} · 吊り縄 {{ frame.population.ropes }}</p>
            <p v-else>この日の人数記録はありません。日付を切り替えて確認してください。</p>
          </section>
        </div>
      </div>
      <p role="status" data-testid="replay-audio-status">録画BGM：{{ audioState.status==='playing'?'再生中':audioState.status==='error'?'再生できません':'停止・準備中' }}</p>
      <p v-if="audioState.error" role="alert">音声診断：{{ audioState.error }}。有効化で再試行できます。録画の閲覧は続けられます。</p>
      <div class="claim-actions"><button class="quiet-button" @click="audioControls?.enable()">録画の音楽を有効にする</button><button class="quiet-button" @click="audioControls?.stop()">録画の音楽を停止</button></div>
    </template>
  </dialog>
</template>
