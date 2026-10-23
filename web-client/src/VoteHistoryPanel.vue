<script setup>
import { votingMethodText, voteRoundText } from './voteHistoryText'

defineProps({
  history: { type: Array, required: true },
  playerName: { type: Function, required: true },
})
</script>

<template>
  <section v-if="history.length" class="panel vote-history" aria-labelledby="vote-history-title" lang="ja">
    <span id="vote-history-title" class="visually-hidden" lang="zh-CN">最近投票结果</span>
    <header class="panel-heading"><span>投票履歴</span><span>完了した投票・全日程 / 全ラウンド</span></header>
    <p class="history-help">回避COで中断された投票は含みません。閲覧しても投票は送信されません。</p>
    <details v-for="vote in history" :key="vote.day" :open="vote === history.at(-1)"
      class="history-day" :data-testid="'vote-day-' + vote.day">
      <summary>第{{ vote.day }}日 · {{ votingMethodText(vote.method) }} · {{ vote.rounds.length }}ラウンド</summary>
      <p class="history-candidates">候補：{{ vote.candidates.map(playerName).join('・') }}</p>
      <section v-for="round in vote.rounds" :key="round.round" class="history-round"
        :aria-label="'第' + vote.day + '日 第' + round.round + 'ラウンド'"
        :data-testid="'vote-round-' + vote.day + '-' + round.round">
        <h3>第{{ round.round }}ラウンド</h3>
        <p class="round-summary">{{ voteRoundText(round, playerName) }}</p>
        <table>
          <caption>投票先と得票数</caption>
          <thead><tr><th scope="col">投票者</th><th scope="col">投票先</th><th scope="col">得票数</th></tr></thead>
          <tbody><tr v-for="line in round.lines" :key="line.playerNumber">
            <th scope="row">{{ playerName(line.playerNumber) }}</th>
            <td>{{ playerName(line.targetPlayerNumber) }}</td><td>{{ line.receivedVotes }}票</td>
          </tr></tbody>
        </table>
      </section>
    </details>
  </section>
</template>

<style scoped>
.history-help, .history-candidates { margin: 16px 22px; font-size: 12px; line-height: 1.7; overflow-wrap: anywhere; }
.history-day { margin: 14px 22px; border-top: 1px solid #dce2d3; }
summary { padding: 14px 0; cursor: pointer; font-weight: 600; }
summary:focus-visible { outline: 2px solid #52693b; outline-offset: 3px; }
.history-round { margin: 0 0 22px; }
h3, .round-summary { font-size: 13px; overflow-wrap: anywhere; }
table { width: 100%; table-layout: fixed; border-collapse: collapse; font-size: 12px; }
caption { text-align: left; padding: 8px 0; }
th, td { text-align: left; padding: 8px; border-bottom: 1px solid #e0e5d9; overflow-wrap: anywhere; }
thead { background: #f0f3e9; }
</style>
