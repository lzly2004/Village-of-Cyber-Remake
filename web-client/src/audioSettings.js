export const AUDIO_SETTINGS_KEY = 'vocr.audio.v1'
export const defaultAudioSettings = () => ({version:1,bgmVolume:100,soundVolume:100,muted:false})
export function validAudioSettings(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
    && Object.keys(value).sort().join(',') === 'bgmVolume,muted,soundVolume,version'
    && value.version === 1 && typeof value.muted === 'boolean'
    && [value.bgmVolume,value.soundVolume].every(n=>Number.isInteger(n) && n>=0 && n<=100)
}
export function readAudioSettings(storage) {
  try {
    const text=storage.getItem(AUDIO_SETTINGS_KEY)
    if (text === null) return {settings:defaultAudioSettings(),warning:null}
    const settings=JSON.parse(text)
    if (!validAudioSettings(settings)) throw Error('invalid settings')
    return {settings,warning:null}
  } catch {return {settings:defaultAudioSettings(),warning:'AUDIO_SETTINGS_READ_FAILED'}}
}
export function saveAudioSettings(storage,settings) {
  if (!validAudioSettings(settings)) return 'AUDIO_SETTINGS_INVALID'
  try {storage.setItem(AUDIO_SETTINGS_KEY,JSON.stringify(settings));return null}
  catch {return 'AUDIO_SETTINGS_WRITE_FAILED'}
}
