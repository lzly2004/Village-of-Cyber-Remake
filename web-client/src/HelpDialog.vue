<script setup>
import { computed, onMounted, ref } from 'vue'
import { configurations, roles } from './labels.js'
import { helpGroups } from './helpSources.js'
const props = defineProps({ initialPage: { type: String, default: 'help' } })
const emit = defineEmits(['close'])
const dialog = ref(null)
const page = ref(props.initialPage)
const groupId = ref('1')
const topicId = ref('1-1')
const group = computed(() => helpGroups.find(item => item.id === groupId.value))
const topic = computed(() => group.value.topics.find(item => item.id === topicId.value))
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
  <dialog ref="dialog" class="help-dialog" aria-labelledby="help-dialog-title" lang="zh-CN" @keydown="keepFocus" @close="emit('close')">
    <header class="help-dialog-heading"><h2 id="help-dialog-title">電脳村ガイド</h2><button class="quiet-button" autofocus @click="dialog.close()">閉じる</button></header>
    <nav class="help-tabs" aria-label="ガイドのページ">
      <button :aria-pressed="page === 'help'" @click="page = 'help'">遊び方・ヘルプ</button>
      <button :aria-pressed="page === 'configurations'" @click="page = 'configurations'">配役説明</button>
    </nav>
    <section v-if="page === 'help'" aria-label="ヘルプ">
      <p class="help-provenance">直接复用桌面版帮助原文。浏览器专有的操作和保存差异单列在“网页版补充”。</p>
      <div class="help-selectors">
        <label>分類<select aria-label="分類" :value="groupId" @change="chooseGroup"><option v-for="item in helpGroups" :key="item.id" :value="item.id">{{ item.title }}</option></select></label>
        <label>項目<select aria-label="項目" v-model="topicId"><option v-for="item in group.topics" :key="item.id" :value="item.id">{{ item.title }}</option></select></label>
      </div>
      <article :key="topic.id" class="help-article" tabindex="0" aria-label="ヘルプ本文">
        <p>{{ topic.body }}</p>
        <aside v-if="topic.webNote" class="notice"><strong>网页版补充</strong><p>{{ topic.webNote }}</p></aside>
        <p>来源：help/{{ topic.source }}（正文直接使用原文。）</p>
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
      <p>役職は対局ごとに割り振られます。ここで配役を閲覧しても開村・投票は行いません。対局を始めるには閉じてタイトルの「Start」から配役画像を選んでください。</p>
    </section>
  </dialog>
</template>
