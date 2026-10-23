// WEB-026 resource access only. No AudioContext/element, playback, scene inference or import-time fetch.
export class AudioResourceError extends Error {
  constructor(code) { super(code); this.name = 'AudioResourceError'; this.code = code }
}
const validId = id => typeof id === 'string' && /^[A-Z][A-Z0-9_]*$/.test(id)
const validHash = hash => typeof hash === 'string' && /^[a-f0-9]{64}$/.test(hash)
const object = value => value && typeof value === 'object' && !Array.isArray(value)
const own = (value,key) => object(value) && Object.hasOwn(value,key)
const check = (condition,code) => { if (!condition) throw new AudioResourceError(code) }
const canceled = signal => { if (signal?.aborted) throw new DOMException('Audio request aborted','AbortError') }
function observe(promise,signal) {
  if (!signal) return promise
  return new Promise((resolve,reject)=>{
    const abort=()=>reject(new DOMException('Audio request aborted','AbortError'))
    signal.addEventListener('abort',abort,{once:true})
    if (signal.aborted) abort()
    promise.then(resolve,reject).finally(()=>signal.removeEventListener('abort',abort))
  })
}

export function resolveAudio(manifest,kind,id) {
  if (!['bgm','sounds'].includes(kind) || !validId(id) || !own(manifest?.[kind],id)) return null
  const entry = manifest[kind][id]
  if (!object(entry) || !validHash(entry.sha256) || entry.file !== kind+'/'+entry.sha256+'.mp3'
    || entry.mime !== 'audio/mpeg' || !Number.isSafeInteger(entry.bytes) || entry.bytes <= 0
    || entry.bytes > (kind === 'bgm' ? 5*1024**2 : 1024**2)
    || entry.channels !== 2 || ![44100,48000].includes(entry.sampleRate)
    || !(kind === 'bgm' ? [128000,160000,192000] : [192000]).includes(entry.bitrate)
    || !Number.isSafeInteger(entry.samples) || entry.samples <= 0
    || !Number.isFinite(entry.duration) || entry.duration <= 0
    || Math.abs(entry.duration*entry.sampleRate-entry.samples) > .001) return null
  return { url: '/assets/audio/'+entry.file, sha256: entry.sha256, bytes: entry.bytes,
    mime: entry.mime, channels: entry.channels, sampleRate: entry.sampleRate,
    samples: entry.samples, duration: entry.duration }
}
export function resolveAudioDescription(manifest,id) {
  if (!resolveAudio(manifest,'bgm',id)) return null
  const entry = manifest.bgm[id].description
  if (!object(entry) || !validHash(entry.sha256) || entry.file !== 'descriptions/'+entry.sha256+'.txt'
    || !Number.isSafeInteger(entry.bytes) || entry.bytes <= 0 || entry.bytes > 1024**2) return null
  return { url: '/assets/audio/'+entry.file, sha256: entry.sha256, bytes: entry.bytes }
}
function validateManifest(manifest) {
  check(manifest?.schemaVersion === 1 && manifest?.policy?.version === 1
    && manifest.policy.format === 'mp3' && object(manifest.playlists),'MANIFEST_INVALID')
  for (const kind of ['bgm','sounds']) {
    check(object(manifest[kind]) && Object.keys(manifest[kind]).length > 0,'MANIFEST_INVALID')
    for (const id of Object.keys(manifest[kind])) {
      check(resolveAudio(manifest,kind,id),'MANIFEST_INVALID')
      if (kind === 'bgm') check(manifest.bgm[id].description === null || resolveAudioDescription(manifest,id),'MANIFEST_INVALID')
    }
  }
  check(Object.keys(manifest.playlists).sort().join(',') === 'LATE_GAME_PLAYLIST,NORMAL_GAME_PLAYLIST','MANIFEST_INVALID')
  for (const ids of Object.values(manifest.playlists)) check(Array.isArray(ids) && ids.length > 0
    && new Set(ids).size === ids.length && ids.every(id=>resolveAudio(manifest,'bgm',id)),'MANIFEST_INVALID')
  return manifest
}
const defaultDigest = async bytes => {
  const hash = await globalThis.crypto.subtle.digest('SHA-256',bytes)
  return Array.from(new Uint8Array(hash),byte=>byte.toString(16).padStart(2,'0')).join('')
}
export function createAudioResources(fetcher,digest = defaultDigest,timeoutMs = 15000) {
  let pending
  const manifest = () => {
    if (!pending) pending = (async () => {
      const controller = new AbortController()
      let timer
      const deadline = new Promise((_,reject)=>{
        timer=setTimeout(()=>{reject(new AudioResourceError('MANIFEST_TIMEOUT'));controller.abort()},timeoutMs)
      })
      try {
        const response = await Promise.race([fetcher('/assets/audio/manifest.json',{signal:controller.signal}),deadline])
        check(response.ok,'MANIFEST_LOAD_FAILED')
        return validateManifest(await Promise.race([response.json(),deadline]))
      } catch (error) {
        throw error instanceof AudioResourceError ? error : new AudioResourceError('MANIFEST_LOAD_FAILED')
      } finally { clearTimeout(timer) }
    })()
    return pending
  }
  async function bytes(asset,signal) {
    try {
      canceled(signal)
      const response = await fetcher(asset.url,{signal})
      check(response.ok,'AUDIO_LOAD_FAILED')
      const data = await response.arrayBuffer()
      canceled(signal)
      check(data.byteLength === asset.bytes && await digest(data) === asset.sha256,'AUDIO_INTEGRITY_FAILED')
      canceled(signal)
      return data
    } catch (error) {
      if (error instanceof AudioResourceError || error?.name === 'AbortError') throw error
      throw new AudioResourceError('AUDIO_LOAD_FAILED')
    }
  }
  return {
    manifest,
    async load(kind,id,{signal} = {}) {
      check(['bgm','sounds'].includes(kind) && validId(id),'UNKNOWN_AUDIO')
      canceled(signal)
      const asset = resolveAudio(await observe(manifest(),signal),kind,id)
      check(asset,'UNKNOWN_AUDIO')
      return { asset, data: await bytes(asset,signal) }
    },
    async description(id,{signal} = {}) {
      check(validId(id),'UNKNOWN_AUDIO'); canceled(signal)
      const catalog = await observe(manifest(),signal)
      check(resolveAudio(catalog,'bgm',id),'UNKNOWN_AUDIO')
      if (catalog.bgm[id].description === null) return null
      const asset = resolveAudioDescription(catalog,id)
      const data = await bytes(asset,signal)
      try { return new TextDecoder('utf-8',{fatal:true}).decode(data) }
      catch { throw new AudioResourceError('DESCRIPTION_INVALID') }
    },
  }
}
export const audioResources = createAudioResources((url,options)=>fetch(url,options))
