import { test, expect } from '@playwright/test'
import { readFileSync } from 'node:fs'
import { publicSnapshot } from '../test-fixtures/public-players.js'
const manifest=JSON.parse(readFileSync(new URL('../public/assets/audio/manifest.json',import.meta.url),'utf8'))
const loader=readFileSync(new URL('../src/audioAssets.js',import.meta.url),'utf8')
async function inject(page) {
  //026 does not wire playback into App. Exercise the real standalone resource module explicitly.
  await page.evaluate(async source=>{
    const url=URL.createObjectURL(new Blob([source],{type:'text/javascript'}))
    try {const module=await import(url);window.audioResources=module.createAudioResources((url,options)=>fetch(url,options))}
    finally {URL.revokeObjectURL(url)}
  },loader)
}

test('WEB-026 actual lobby/first stage request zero audio; explicit resource access fetches only named effect/description',async({page})=>{
  const paths=[];page.on('request',r=>{if(r.url().includes('/assets/audio/'))paths.push(new URL(r.url()).pathname)})
  await page.route('**/api/v1/games',route=>route.fulfill({json:{sessionId:'00000000-0000-0000-0000-000000000026',update:publicSnapshot()}}))
  await page.goto('/');await expect(page.getByRole('heading',{name:/每一次发言/})).toBeVisible()
  expect(paths).toEqual([])
  await page.getByRole('button',{name:'开始对局'}).click();await expect(page.locator('.player-row')).toHaveCount(44)
  expect(paths).toEqual([])
  await inject(page)
  expect(paths).toEqual([])
  const result=await page.evaluate(async()=>{
    window.Audio=()=>{throw Error('resource layer must not play')}
    window.AudioContext=()=>{throw Error('resource layer must not allocate playback context')}
    const effect=await window.audioResources.load('sounds','CLICK')
    const description=await window.audioResources.description('VILLAGE_08')
    return {bytes:effect.data.byteLength,description}
  })
  expect(result.bytes).toBe(manifest.sounds.CLICK.bytes)
  expect(result.description).toBe(readFileSync(new URL('../../src/main/resources/help/村08.txt',import.meta.url),'utf8').replaceAll('\r\n','\n'))
  expect(paths).toEqual(['/assets/audio/manifest.json','/assets/audio/'+manifest.sounds.CLICK.file,
    '/assets/audio/'+manifest.bgm.VILLAGE_08.description.file])
})

test('WEB-026 every unique MP3 fully decodes in Chromium with original channel/rate and gapless sample tolerance',async({browser},info)=>{
  const entries=[...new Map(['bgm','sounds'].flatMap(kind=>Object.values(manifest[kind])).map(entry=>[entry.file,entry])).values()]
  expect(entries).toHaveLength(31)
  const measurements=[]
  for(const entry of entries){
    // Close the document/context between files so a complete58-minute bank never accumulates decoded buffers.
    const page=await browser.newPage()
    try{
      await page.goto('http://127.0.0.1:5177/')
      const result=await page.evaluate(async entry=>{
        const response=await fetch('/assets/audio/'+entry.file)
        if(!response.ok)throw Error('audio HTTP failure')
        const bytes=await response.arrayBuffer()
        const context=new OfflineAudioContext(entry.channels,1,entry.sampleRate)
        const buffer=await context.decodeAudioData(bytes)
        const channel=buffer.getChannelData(0)
        let nonzero=false
        for(let i=0;i<channel.length;i+=Math.max(1,Math.floor(channel.length/4096)))if(Math.abs(channel[i])>1e-7)nonzero=true
        return{file:entry.file,rate:buffer.sampleRate,channels:buffer.numberOfChannels,samples:buffer.length,nonzero}
      },entry)
      expect(result.rate).toBe(entry.sampleRate);expect(result.channels).toBe(entry.channels)
      expect(Math.abs(result.samples-entry.samples)).toBeLessThanOrEqual(1152)
      expect(result.nonzero).toBe(true);measurements.push(result)
    }finally{await page.close()}
  }
  await info.attach('audio-decode.json',{contentType:'application/json',body:JSON.stringify(measurements)})
})

test('WEB-026 missing hash SPA HTTP200 and corrupted chunk are rejected; failures do not prefetch/fallback',async({page})=>{
  await page.goto('/');await inject(page)
  await page.route('**/assets/audio/'+manifest.sounds.CLICK.file,route=>route.fulfill({status:200,contentType:'text/html',body:'<html>SPA</html>'}))
  const errors=await page.evaluate(async()=>{
    const codes=[]
    for(const id of ['UNKNOWN','CLICK'])try{await window.audioResources.load('sounds',id)}catch(error){codes.push(error.code)}
    const data=await(await fetch('/assets/audio/sounds/'+'0'.repeat(64)+'.mp3')).arrayBuffer()
    try{await new OfflineAudioContext(2,1,44100).decodeAudioData(data);codes.push('wrongly decoded SPA')}
    catch{codes.push('DECODE_REJECTED')}
    return codes
  })
  expect(errors).toEqual(['UNKNOWN_AUDIO','AUDIO_INTEGRITY_FAILED','DECODE_REJECTED'])
})

test('WEB-026 aborted in-flight current track is not returned and next request keeps shared manifest usable',async({page})=>{
  let held
  const intercepted=new Promise(resolve=>{held=resolve})
  await page.route('**/assets/audio/'+manifest.bgm.START_MENU.file,route=>{held(route)})
  await page.goto('/');await inject(page)
  await page.evaluate(()=>{
    window.controller=new AbortController()
    window.result=window.audioResources.load('bgm','START_MENU',{signal:window.controller.signal}).then(()=> 'UNEXPECTED_SUCCESS',error=>error.name)
  })
  const route=await intercepted
  await page.evaluate(()=>window.controller.abort())
  expect(await page.evaluate(()=>window.result)).toBe('AbortError')
  await route.abort('aborted')
  const bytes=await page.evaluate(async()=>(await window.audioResources.load('sounds','CLICK')).data.byteLength)
  expect(bytes).toBe(manifest.sounds.CLICK.bytes)
})
