<script setup>
import {computed} from 'vue'
import AssetImage from './AssetImage.vue'
const props=defineProps({day:Number,alive:Number,dead:Number})
// Same four public values and text inset as VoteInfoRenderer, over the original art.
const text=computed(()=>`    ${props.day}日目\n 生存者:${props.alive}\n 死亡者:${props.dead}\n 吊り縄:${Math.floor((props.alive-1)/2)}`)
</script>
<template><div class="desktop-calendar" role="status"><AssetImage name="images/hiduke.png" /><span class="desktop-calendar-text">{{ text }}</span></div></template>
