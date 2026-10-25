<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { loadImageManifest, resolveImage } from './imageAssets.js'

const props = defineProps({ name: { type: String, default: null } })
const element = ref(null)
const visible = ref(false)
const asset = ref(null)
const state = ref('waiting')
let observer
let version = 0
function imageError(event) {
  if (event.target.getAttribute('src') === asset.value?.url) state.value = 'failed'
}
onMounted(() => {
  if (typeof IntersectionObserver === 'undefined') { visible.value = true; return }
  observer = new IntersectionObserver(entries => {
    if (entries.some(entry => entry.isIntersecting)) {
      visible.value = true
      observer.disconnect()
    }
  }, { rootMargin: '64px' })
  observer.observe(element.value)
})
onBeforeUnmount(() => { version++; observer?.disconnect() })
watch([() => props.name, visible], async ([name, ready]) => {
  const current = ++version
  asset.value = null
  state.value = name ? 'waiting' : 'failed'
  if (!ready || !name) return
  state.value = 'loading'
  try {
    const manifest = await loadImageManifest()
    if (version !== current) return
    asset.value = resolveImage(manifest, name)
    state.value = asset.value ? 'ready' : 'failed'
  } catch {
    if (version === current) state.value = 'failed'
  }
}, { immediate: true })
</script>

<template>
  <span ref="element" class="asset-image" :data-image-state="state">
    <img v-if="asset && state === 'ready'" :key="asset.url" :src="asset.url" :width="asset.width" :height="asset.height"
      alt="" aria-hidden="true" decoding="async" @error="imageError">
    <span v-else-if="state === 'failed'" class="asset-diagnostic" role="img" aria-label="表示診断：画像を表示できません">画像未表示</span>
    <span v-else class="asset-placeholder" aria-hidden="true">…</span>
  </span>
</template>
