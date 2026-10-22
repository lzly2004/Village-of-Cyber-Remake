import { loadImageManifest, resolveImage } from './imageAssets.js'

async function decodeImage(asset) {
  const image = new Image()
  image.src = asset.url
  await image.decode()
  if (image.naturalWidth !== asset.width || image.naturalHeight !== asset.height) throw new Error('IMAGE_DIMENSIONS')
  return image
}
// Keep at most16 recent decoded images alive, including backgrounds. No future-event prefetch.
export function createStageImageCache(manifestLoader, decode, limit = 16) {
  const entries = new Map()
  function touch(name, value) {
    entries.delete(name); entries.set(name, value)
    while (entries.size > limit) entries.delete(entries.keys().next().value)
  }
  function ensure(name) {
    if (entries.has(name)) { const entry = entries.get(name); touch(name, entry); return entry.promise }
    const entry = { ready: null }
    entry.promise = (async () => {
      const asset = resolveImage(await manifestLoader(), name)
      if (!asset) throw new Error('MISSING_STAGE_IMAGE')
      const image = await decode(asset)
      entry.ready = { ...asset, image }
      return entry.ready
    })()
    touch(name, entry)
    return entry.promise
  }
  return {
    ready(names) {
      const values = names.map(name => entries.get(name)?.ready)
      if (!values.every(Boolean)) return null
      names.forEach(name => touch(name, entries.get(name)))
      return values
    },
    load: names => Promise.all(names.map(ensure)),
    size: () => entries.size,
  }
}
export const stageImageCache = createStageImageCache(loadImageManifest, decodeImage)
