<script setup>
import { computed } from 'vue'
import { desktopInformation } from './desktopVoteText.js'
const props=defineProps({information:Object,playerName:Function})
const text=computed(()=>desktopInformation(props.information,props.playerName))
</script>
<template><pre class="desktop-information-text" role="region" aria-label="公開情報" tabindex="0" data-testid="desktop-information">{{ information ? text : 'このサーバーは公開情報表示に対応していません。' }}</pre></template>
