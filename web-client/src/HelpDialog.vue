<script setup>
import { computed, onMounted, ref } from 'vue'
import { configurations, roles } from './labels.js'
import { helpGroups } from './helpContent.js'
const props = defineProps({ initialPage: { type: String, default: 'help' } })
const emit = defineEmits(['close'])
const dialog = ref(null)
const page = ref(props.initialPage)
const groupId = ref('1')
const topicId = ref('1-1')
const group = computed(() => helpGroups.find(item => item.id === groupId.value))
const topic = computed(() => group.value.topics.find(item => item.id === topicId.value))
const originals = import.meta.glob('../../src/main/resources/help/Info*.txt', { query: '?raw', import: 'default', eager: true })
const original = computed(() => originals['../../src/main/resources/help/' + topic.value.source])
function chooseGroup(event) {
  groupId.value = event.target.value
  topicId.value = group.value.topics[0].id
}
function keepFocus(event) {
  if (event.key !== 'Tab') return
  const nodes = [...dialog.value.querySelectorAll('button:not(:disabled), select:not(:disabled), summary, [tabindex="0"]')]
    .filter(node => node.getClientRects().length)
  const first = nodes[0], last = nodes.at(-1)
  if ((!event.shiftKey && document.activeElement === last) || (event.shiftKey && document.activeElement === first)) {
    event.preventDefault()
    ;(event.shiftKey ? last : first).focus()
  }
}
onMounted(() => dialog.value.showModal())
</script>

<template>
  <dialog ref="dialog" class="help-dialog" aria-labelledby="help-dialog-title" lang="ja" @keydown="keepFocus" @close="emit('close')">
    <header class="help-dialog-heading"><h2 id="help-dialog-title">電脳村ガイド</h2><button class="quiet-button" autofocus @click="dialog.close()">閉じる</button></header>
    <nav class="help-tabs" aria-label="ガイドのページ">
      <button :aria-pressed="page === 'help'" @click="page = 'help'">遊び方・ヘルプ</button>
      <button :aria-pressed="page === 'configurations'" @click="page = 'configurations'">配役説明</button>
    </nav>
    <section v-if="page === 'help'" aria-label="ヘルプ">
      <p class="help-provenance">既存の中国語ヘルプを基にしたWeb版の日本語要約・補足です。原版の日本語原文ではありません。操作や保存の違いは「Web版の補足」に記載しています。</p>
      <div class="help-selectors">
        <label>分類<select aria-label="分類" :value="groupId" @change="chooseGroup"><option v-for="item in helpGroups" :key="item.id" :value="item.id">{{ item.title }}</option></select></label>
        <label>項目<select aria-label="項目" v-model="topicId"><option v-for="item in group.topics" :key="item.id" :value="item.id">{{ item.title }}</option></select></label>
      </div>
      <article :key="topic.id" class="help-article" tabindex="0" aria-label="ヘルプ本文">
        <h3>{{ topic.title }}</h3><p>{{ topic.body }}</p>
        <aside v-if="topic.webNote" class="notice"><strong>Web版の補足</strong><p>{{ topic.webNote }}</p></aside>
        <details><summary>参照した原文（中国語・デスクトップ版）</summary><p>出典：help/{{ topic.source }}</p><pre lang="zh-CN" v-if="typeof original === 'string'">{{ original }}</pre><p v-else role="status">表示診断：原文を読み込めません（HELP_SOURCE_MISSING）。</p></details>
      </article>
    </section>
    <section v-else aria-label="配役説明">
      <p>村名は既存の選択画像、人数・役職の内訳はデスクトップ版のGameStatusに基づきます。これは公開の配役構成です。今回の人物と役職の対応を示すものではありません。</p>
      <div class="configuration-reference" role="region" aria-label="配役一覧" tabindex="0">
        <article v-for="item in configurations" :key="item.value" :data-configuration="item.value">
          <h3>{{ item.name }}村 · {{ item.playerCount }}人</h3>
          <dl><template v-for="(count, role) in item.roleCounts" :key="role"><dt>{{ roles[role] }}</dt><dd>{{ count }}人</dd></template></dl>
        </article>
      </div>
      <p>役職は対局ごとに割り振られます。ここで配役を閲覧しても開村・投票は行いません。対局を始めるには閉じて配役を選び、「対局を始める」を押してください。</p>
    </section>
  </dialog>
</template>
