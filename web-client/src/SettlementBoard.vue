<script setup>
import { computed } from 'vue'
import PlayerPortrait from './PlayerPortrait.vue'
import AssetImage from './AssetImage.vue'
import { characterDisplayName } from './characterDisplay.js'
import { settlementDetails } from './settlementVisual.js'
import { outcomes } from './sceneArtwork.js'
const props=defineProps({players:{type:Array,required:true},reveal:{type:Object,required:true},result:{type:String,required:true}})
const emit=defineEmits(['menu'])
const outcome=computed(()=>Object.hasOwn(outcomes,props.result)?outcomes[props.result]:null)
const cards=computed(()=>props.players.map(player=>({player,
  detail:settlementDetails(true,player,props.reveal.roles.get(player.playerNumber),props.result)})))
</script>
<template>
  <section class="settlement-board" lang="ja" data-testid="settlement-board">
    <AssetImage class="settlement-background" name="images/frame #19252.png" />
    <header class="settlement desktop-verdict" role="status">
      <AssetImage v-if="outcome" class="settlement-win-icon" :name="outcome.icon" />
      <h2>{{ outcome?.text || '勝敗不明（表示診断）' }}</h2>
    </header>
    <div class="settlement-roster" role="region" aria-label="結算役職一覧" tabindex="0" aria-describedby="settlement-help">
      <div class="settlement-grid" :style="{'--settlement-columns':Math.ceil(players.length/2)}">
        <article v-for="({player,detail},index) in cards" :key="player.playerNumber" class="settlement-card" :style="{gridColumn:index%Math.ceil(players.length/2)+1,gridRow:index<Math.ceil(players.length/2)?1:2}" :data-player-number="player.playerNumber" tabindex="0">
          <PlayerPortrait :player="player" :claim-markers="false" revealed :actual-role="reveal.roles.get(player.playerNumber)" />
          <strong class="visually-hidden">{{ player.playerNumber }} · {{ characterDisplayName(player.character) }}</strong>
          <dl><dt>公称職業:</dt><dd data-testid="settlement-claim">{{ detail.claim }}</dd>
            <dt>真の職業:</dt><dd data-testid="settlement-actual">{{ detail.actual }}</dd></dl>
          <p data-testid="settlement-fate">{{ detail.fate }}</p>
          <small v-if="detail.diagnostic" class="visual-diagnostic" role="status">表示診断：{{ detail.diagnostic }}</small>
        </article>
      </div>
    </div>
    <button class="desktop-art-button settlement-title" aria-label="タイトルに戻る" @click="emit('menu')"><AssetImage name="images/PVBtitile.png" /></button>
    <footer id="settlement-help" class="visually-hidden">上下2段の一覧です。左右・上下にスクロールできます。Tabで各人物へ移動できます。
      <span v-if="reveal.diagnostic" class="visual-diagnostic" role="status" data-testid="reveal-diagnostic">表示診断：身分情報が不完全です。人物の役職画像を表示できません。</span>
    </footer>
  </section>
</template>
