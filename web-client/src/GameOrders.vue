<script setup>
defineProps(['update', 'alternativeVotes', 'canDecide', 'vote', 'divinationTargets', 'selectedDivinationTargets', 'setDivinationTarget', 'guardTargets', 'selectedGuardTargets', 'setGuardTarget', 'claimedSkills', 'setClaimedTarget', 'claimRoles', 'askClaimRole', 'claimRoleNames', 'claimPlayers', 'claimCandidates', 'claimPlayerDraft', 'toggleClaimPlayer', 'characterDisplayName', 'canAskClaimPlayers', 'askClaimPlayers', 'name', 'setAvoidance', 'showAvoidance'])
</script>
<template>
<div class="vote-orders" tabindex="-1">
        <section v-if="update.phase === 'DAY_DECISION'" class="panel claim-inquiry" aria-label="自由・グレー投票" lang="ja">
          <header class="panel-heading"><span>自由・グレー投票</span><span>候補はサーバーの最新状態に従います。</span></header>
          <div v-for="action in alternativeVotes" :key="action.method" class="claim-actions">
            <button class="primary-button" :data-testid="'vote-' + action.method" :disabled="!canDecide"
              @click="vote(action.method)">{{ action.method === 'FREE' ? '自由投票' : 'グレー投票' }}</button>
            <span :data-testid="'candidates-' + action.method">{{ action.candidates.map(name).join('、') }}</span>
          </div>
          <p v-if="!alternativeVotes.some(action => action.method === 'GREY')" class="avoidance-help">グレー投票の候補はいません。</p>
        </section>

        <section v-if="update.phase === 'DAY_DECISION'" class="panel claim-inquiry" aria-label="投票回避設定" lang="ja">
          <header class="panel-heading"><span>投票回避CO</span><button v-if="showAvoidance" class="primary-button" role="switch" aria-label="投票回避CO" :aria-checked="update.selections.avoidanceEnabled" :disabled="!canDecide" @click="setAvoidance(!update.selections.avoidanceEnabled)">{{ update.selections.avoidanceEnabled ? '回避あり' : '回避なし' }}</button>

          </header>
          <p class="avoidance-help">指定投票時の回避CO。共有の回避は無効化されません。</p>
        </section>

        <section v-if="divinationTargets.length" class="panel claim-inquiry" aria-label="全員の占い予約" lang="ja">
          <header class="panel-heading"><span>全員の占い予約</span><span>当日の予約です。結果は確約されません。</span></header>
          <p class="avoidance-help">潜伏・公開COの占い候補をまとめて設定／解除します。チェックは公開された予約のいずれかに対象が含まれることを示します。</p>
          <p class="avoidance-help">全体操作は、この対象の個別設定も一括で上書きします。他の対象の設定は変更しません。</p>
          <div class="claim-player-list">
            <label v-for="target in divinationTargets" :key="target" class="claim-player">
              <input type="checkbox" :aria-label="'全員占い対象 ' + target + '番 ' + name(target)"
                :checked="selectedDivinationTargets.has(target)" :disabled="!canDecide"
                @click.prevent="setDivinationTarget(target, !selectedDivinationTargets.has(target))">
              <span>{{ name(target) }}</span>
            </label>
          </div>
        </section>

        <section v-if="guardTargets.length" class="panel claim-inquiry" aria-label="全員の護衛予約" lang="ja">
          <header class="panel-heading"><span>全員の護衛予約</span><span>当日の予約です。結果は確約されません。</span></header>
          <p class="avoidance-help">潜伏・公開COの護衛候補をまとめて設定／解除します。チェックは公開された予約のいずれかに対象が含まれることを示します。</p>
          <p class="avoidance-help">全体操作は、この対象の個別設定も一括で上書きします。他の対象の設定は変更しません。</p>
          <div class="claim-player-list">
            <label v-for="target in guardTargets" :key="target" class="claim-player">
              <input type="checkbox" :aria-label="'全員護衛対象 ' + target + '番 ' + name(target)"
                :checked="selectedGuardTargets.has(target)" :disabled="!canDecide"
                @click.prevent="setGuardTarget(target, !selectedGuardTargets.has(target))">
              <span>{{ name(target) }}</span>
            </label>
          </div>
        </section>

        <div v-if="claimedSkills.length" class="claimed-skills-layout">
          <section v-for="skill in claimedSkills" :key="skill.type" class="panel claim-inquiry" :aria-label="skill.label" lang="ja">
            <header class="panel-heading"><span>{{ skill.label }} · {{ skill.actors.length }}人</span><span>公開COの行動者だけを表示します。予約は結果を保証しません。</span></header>
            <p v-if="!skill.actors.length" class="avoidance-help">この技能の公開CO行動者はいません。</p>
            <template v-else>
              <p class="list-help" :id="'claimed-help-' + skill.type">行ごとに折りたためます。長い一覧は枠内でスクロールできます。Tabで枠に移動し、方向キーで閲覧できます。</p>
              <div class="claimed-actor-list" role="region" :aria-label="skill.label + ' 一覧'" tabindex="0"
                :aria-describedby="'claimed-help-' + skill.type">
                <details v-for="actor in skill.actors" :key="actor.playerNumber" class="claimed-actor" open>
                  <summary>{{ name(actor.playerNumber) }} · CO順 {{ actor.claimedRoleOrder }}<span>予約 {{ actor.targets.length }}件</span></summary>
                  <fieldset class="claim-player-list" :data-testid="'claimed-' + skill.type + '-' + actor.playerNumber" :disabled="!canDecide">
                    <legend class="visually-hidden">{{ name(actor.playerNumber) }} · CO順 {{ actor.claimedRoleOrder }}</legend>
                    <label v-for="target in skill.targets" :key="target" class="claim-player">
                      <input type="checkbox" :aria-label="skill.label + ' ' + actor.playerNumber + '番から' + target + '番へ'"
                        :checked="actor.targets.includes(target)"
                        @click.prevent="setClaimedTarget(skill.type, actor.playerNumber, target, !actor.targets.includes(target))">
                      <span>{{ name(target) }}</span>
                    </label>
                  </fieldset>
                </details>
              </div>
            </template>
          </section>
        </div>

        <section v-if="claimRoles.length" class="panel claim-inquiry" aria-label="職業別CO確認" lang="ja">
          <header class="panel-heading"><span>職業別CO確認</span><span>発言を読み終えてから操作してください。</span></header>
          <div class="claim-actions">
            <button v-for="role in claimRoles" :key="role" class="primary-button"
              :disabled="!canDecide" @click="askClaimRole(role)">{{ claimRoleNames[role] || role }}のCOを聞く</button>
          </div>
        </section>

        <section v-if="claimPlayers.length" class="panel claim-inquiry" aria-label="プレイヤー別CO確認" lang="ja">
          <header class="panel-heading"><span>プレイヤー別CO確認</span><span>未送信の選択は再読み込み・同期で解除されます。</span></header>
          <div class="claim-player-list">
            <label v-for="player in claimCandidates" :key="player.playerNumber" class="claim-player">
              <input type="checkbox" :aria-label="player.playerNumber + '番 ' + characterDisplayName(player.character) + 'をCO確認対象に選択'"
                :checked="claimPlayerDraft.includes(player.playerNumber)" :disabled="!canDecide"
                @change="toggleClaimPlayer(player.playerNumber, $event.target.checked)">
              <span>{{ player.playerNumber }} · {{ characterDisplayName(player.character) }}</span>
            </label>
          </div>
          <div class="claim-actions"><button class="primary-button" :disabled="!canAskClaimPlayers" @click="askClaimPlayers">選択したプレイヤーのCOを聞く</button></div>
        </section>


</div>
</template>
