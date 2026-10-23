import { audioResources } from './audioAssets.js'
import { createSoundChannel } from './soundChannel.js'
import { defaultAudioSettings, validAudioSettings } from './audioSettings.js'

// -12.04dB: measured +3.86dBFS decoded peak leaves >8dB headroom for BGM alone.
// At most one BGM plus one effect voice, sharing the gesture-unlocked context.
export const BGM_GAIN = 0.25
const errorCode = error => /^[A-Z][A-Z0-9_]*$/.test(error?.code) ? error.code : 'BGM_PLAY_FAILED'
export function createBgmPlayer({ resources = audioResources, createContext = () => new AudioContext(),
  createMedia = () => new Audio(), urls = URL, notify = () => {}, timeoutMs = 15000 } = {}) {
  let context, media, source, gain, url, aborter, timer, cancelUnlock
  let version = 0, enabled = false, disposed = false, selected = null, playing = null
  let status = 'off', error = null
  let settings = defaultAudioSettings(), sound = {status:'idle',id:null,error:null}
  const emit = () => notify({ enabled, status, id: playing, error, sound })
  const effects = createSoundChannel({resources,getContext:()=>context,createMedia,urls,timeoutMs,
    allowed:()=>enabled&&!disposed&&!settings.muted&&settings.soundVolume>0,
    notify:next=>{sound=next;emit()} })
  function clearTrack() {
    clearTimeout(timer); timer = null
    aborter?.abort(); aborter = null
    if (media) { media.onerror = null; media.pause(); media.removeAttribute('src'); media.load() }
    if (url) { urls.revokeObjectURL(url); url = null }
    playing = null
  }
  function disable(reason = null) {
    ++version; enabled = false; cancelUnlock?.(); clearTrack(); effects.reset()
    status = reason ? 'error' : 'off'; error = reason; emit()
    if (context && context.state !== 'closed') context.suspend().catch(() => {
      if (!disposed && !enabled) { status = 'error'; error = 'BGM_SUSPEND_FAILED'; emit() }
    })
  }
  function fail(token, code) { if (token === version && !disposed) disable(code) }
  function closeGraph() {
    if (!context) return
    context.onstatechange = null; source?.disconnect(); gain?.disconnect()
    const closing = context
    context = media = source = gain = null
    closing.close().catch(() => { status = 'error'; error = 'BGM_RELEASE_FAILED'; emit() })
  }
  async function play() {
    const token = ++version
    clearTrack(); error = null
    if (!enabled || disposed) return
    if (!selected) { status = 'silent'; emit(); return }
    playing = selected; status = 'loading'; emit()
    const id = selected
    aborter = new AbortController()
    timer = setTimeout(() => fail(token, 'BGM_TIMEOUT'), timeoutMs)
    try {
      const { data, asset } = await resources.load('bgm', id, { signal: aborter.signal })
      if (token !== version || !enabled || disposed) return
      url = urls.createObjectURL(new Blob([data], { type: asset.mime }))
      media.src = url
      media.onerror = () => fail(token, 'BGM_MEDIA_FAILED')
      await media.play()
      if (token !== version || !enabled || disposed) return
      if (context.state !== 'running') throw Error('context interrupted')
      clearTimeout(timer); timer = null
      status = 'playing'; emit()
    } catch (error) { fail(token, error?.name === 'NotAllowedError' ? 'BGM_GESTURE_REQUIRED' : errorCode(error)) }
  }
  return {
    select(id) {
      if (disposed || id === selected) return
      selected = id
      if (enabled) void play()
    },
    // Called synchronously by the explicit enable button, before any network await.
    async enable() {
      if (disposed || enabled || status === 'unlocking') return
      const token = ++version
      status = 'unlocking'; error = null; emit()
      try {
        if (!context) {
          try {
            context = createContext()
            media = createMedia(); media.preload = 'none'; media.loop = true
            source = context.createMediaElementSource(media)
            gain = context.createGain(); gain.gain.value = settings.muted ? 0 : BGM_GAIN*settings.bgmVolume/100
            source.connect(gain); gain.connect(context.destination)
            context.onstatechange = () => {
              if (enabled && context.state !== 'running') disable('BGM_GESTURE_REQUIRED')
            }
          } catch (error) {
            clearTrack(); closeGraph()
            throw error
          }
        }
        const resume = context.resume() // in the user's activation, even when manifest/track are cold
        let unlockTimer
        try {
          await Promise.race([resume, new Promise((_, reject) => {
            cancelUnlock = () => reject(Error('unlock canceled'))
            unlockTimer = setTimeout(() => reject(Error('unlock timeout')), timeoutMs)
          })])
        } finally { clearTimeout(unlockTimer); cancelUnlock = null }
        if (token !== version || disposed) return
        if (context.state !== 'running') throw Error('resume did not unlock')
        enabled = true
        await play()
      } catch { fail(token, context ? 'BGM_GESTURE_REQUIRED' : 'BGM_UNAVAILABLE') }
    },
    effects(ids) { effects.enqueue(ids) },
    click() { effects.enqueue(['CLICK'],{click:true}) },
    cancelSounds() { effects.reset() },
    settings(value) {
      if (!validAudioSettings(value)) return false
      settings = {...value}
      if (gain) gain.gain.value = settings.muted ? 0 : BGM_GAIN*settings.bgmVolume/100
      effects.level(settings.soundVolume/100)
      return true
    },
    disable,
    dispose() {
      if (disposed) return
      disable(); effects.dispose(); disposed = true
      closeGraph()
    },
  }
}
