<script setup>
import { claimRoleNames } from './labels'
import { skillResultText, suspicionText, executionText } from './publicInformationText'

defineProps({
  information: { type: Object, default: null },
  players: { type: Array, required: true },
  finished: { type: Boolean, required: true },
  playerName: { type: Function, required: true },
})
</script>

<template>
  <section tabindex="-1" class="panel public-information" aria-label="公開情報" lang="ja">
    <header class="panel-heading"><span>公開情報</span><span>サーバーの最新の公開状態</span></header>
    <p v-if="!information" class="avoidance-help">このサーバーは公開情報の表示に対応していません。</p>
    <div v-else class="public-info-content">
      <section v-if="!finished" aria-label="公開技能報告">
        <h3>公開技能報告</h3>
        <p class="public-info-help">公開COの報告であり、真の職業や実際の技能成功を示すものではありません。予約とは別の履歴です。</p>
        <p v-if="!information.skillReports.length">公開COの技能報告はありません。</p>
        <article v-for="report in information.skillReports" :key="report.playerNumber"
          class="public-report" :data-testid="'report-' + report.playerNumber">
          <h4>{{ playerName(report.playerNumber) }} · {{ claimRoleNames[report.claimedRole] || '未対応のCO' }} · CO順 {{ report.claimedRoleOrder }}
            <span v-if="players.find(player => player.playerNumber === report.playerNumber)?.alive === false"> · 離場済み</span>
            <span v-if="report.exposed" class="report-exposed"> · 破綻</span>
          </h4>
          <ol v-if="report.results.length">
            <li v-for="result in report.results" :key="result.skillDay">{{ skillResultText(result, playerName) }}</li>
          </ol>
          <p v-else>公開された結果はまだありません。</p>
        </article>
      </section>

      <section v-if="!finished" aria-label="疑い先">
        <h3>疑い先</h3>
        <p class="public-info-help">現在の公開対象と比較記号だけを表示します。数値や真の職業を示すものではありません。</p>
        <p v-if="!information.suspicion.available">第3日以降に公開されます。</p>
        <template v-else>
          <p v-if="!information.suspicion.rows.length">表示する行はありません。</p>
          <div v-for="row in information.suspicion.rows" :key="row.playerNumber" class="suspicion-row"
            :data-testid="'suspicion-' + row.playerNumber"
            :aria-label="row.targets.length ? undefined : '表示する疑い先はありません'">{{ suspicionText(row, playerName) }}</div>
        </template>
      </section>

      <section aria-label="公開死亡履歴">
        <h3>公開死亡履歴</h3>
        <p v-if="!information.deathHistory.length">公開された履歴はまだありません。</p>
        <article v-for="day in information.deathHistory" :key="day.day" class="public-death-day" :data-testid="'death-day-' + day.day">
          <h4>第{{ day.day }}日</h4>
          <p>処刑：{{ day.executions.length ? day.executions.map(death => executionText(death, playerName)).join('→') : 'なし' }}</p>
          <p>死体：{{ day.nightDeaths.length ? day.nightDeaths.map(playerName).join('+') : '平和' }}</p>
        </article>
      </section>
    </div>
  </section>
</template>
