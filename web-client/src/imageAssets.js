// Manifest is fetched once on first visible image, never bundled or prefetched in the lobby.
export function createImageManifestLoader(fetcher) {
  let pending
  return () => {
    if (!pending) pending = (async () => {
      const response = await fetcher('/assets/images/manifest.json')
      if (!response.ok) throw new Error('MANIFEST_LOAD_FAILED')
      const manifest = await response.json()
      if (manifest?.schemaVersion !== 1 || !manifest.images || typeof manifest.images !== 'object'
        || Array.isArray(manifest.images)) throw new Error('MANIFEST_INVALID')
      return manifest
    })()
    return pending
  }
}
export const loadImageManifest = createImageManifestLoader(url => fetch(url))

export function resolveImage(manifest, name) {
  if (typeof name !== 'string' || !Object.hasOwn(manifest?.images || {}, name)) return null
  const entry = manifest.images[name]
  if (!entry || typeof entry.file !== 'string' || !/^[a-f0-9]{64}\.webp$/.test(entry.file)
    || !Array.isArray(entry.size) || entry.size.length !== 2
    || !entry.size.every(value => Number.isSafeInteger(value) && value > 0)) return null
  return { url: '/assets/images/' + entry.file, width: entry.size[0], height: entry.size[1] }
}
