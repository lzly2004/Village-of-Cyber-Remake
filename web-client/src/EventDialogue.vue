<script setup>
import { computed } from 'vue'
import { eventDialogue } from './eventDialogue.js'

const props = defineProps({ event: { type: Object, default: null } })
const dialogue = computed(() => eventDialogue(props.event))
</script>

<template>
  <div :data-dialogue-source="dialogue.source">
    <p v-if="dialogue.diagnostic" class="dialogue-diagnostic" role="status" lang="zh-CN"
      :data-diagnostic="dialogue.diagnostic">{{ dialogue.text }}此为界面诊断，非日文原文。可继续确认本条事件。</p>
    <p v-else class="event-dialogue" data-testid="event-dialogue" lang="ja">{{ dialogue.text }}</p>
  </div>
</template>
