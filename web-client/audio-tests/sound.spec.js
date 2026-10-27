import {test,expect} from '@playwright/test'
import {readFileSync} from 'node:fs'
import {publicSnapshot} from '../test-fixtures/public-players.js'
test.setTimeout(30000)
const catalog=JSON.parse(readFileSync(new URL('../public/assets/audio/manifest.json',import.meta.url),'utf8'))
const id='00000000-0000-4000-8000-000000000028',key='vocr.audio.v1'
const ui=page=>page.locator('.bgm-control')
const soundPath=id=>'/assets/audio/'+catalog.sounds[id].file
async function setup(page,{snapshot=publicSnapshot(),settings,denied=false}={}) {
  const paths=[];page.on('request',r=>{if(r.url().includes('/assets/audio/'))paths.push(new URL(r.url()).pathname)})
  await page.addInitScript(({key,settings,denied})=>{
    if(settings!==undefined && !localStorage.getItem(key))localStorage.setItem(key,settings)
    if(denied)Object.defineProperty(window,'localStorage',{get(){throw new DOMException('denied','SecurityError')}})
    window.soundProbe={media:[],gains:[],contexts:[],urls:[],revoked:[],effectPlays:0,nextClicks:0}
    document.addEventListener('click',event=>{if(event.target.closest?.('[data-testid="next-event"]'))window.soundProbe.nextClicks++},true)
    const p=window.soundProbe,Context=window.AudioContext,Audio=window.Audio
    window.AudioContext=class extends Context{
      constructor(...args){super(...args);p.contexts.push(this)}
      createGain(){const gain=super.createGain();p.gains.push(gain);return gain}
    }
    window.Audio=function(...args){const m=new Audio(...args);m.addEventListener('playing',()=>{if(!m.loop)p.effectPlays++});p.media.push(m);return m}
    const create=URL.createObjectURL.bind(URL),revoke=URL.revokeObjectURL.bind(URL)
    URL.createObjectURL=blob=>{const url=create(blob);p.urls.push(url);return url}
    URL.revokeObjectURL=url=>{p.revoked.push(url);revoke(url)}
  },{key,settings,denied})
  await page.route('**/api/v1/games',r=>r.fulfill({status:201,json:{sessionId:id,update:snapshot}}))
  await page.route('**/api/v1/games/'+id+'?**',r=>{
    const cursor=Number(new URL(r.request().url()).searchParams.get('eventCursor')||0)
    return r.fulfill({json:{...snapshot,eventCursor:cursor,events:snapshot.events.slice(cursor),presentedEvents:snapshot.presentedEvents.slice(cursor)}})
  })
  await page.goto('/');return paths
}
async function enable(page){await page.getByRole('button',{name:'音楽を有効にする'}).click();await expect(ui(page)).toHaveAttribute('data-status','playing')}
async function level(page,name,value){await page.getByRole('slider',{name,exact:true}).evaluate((node,value)=>{node.value=String(value);node.dispatchEvent(new Event('input',{bubbles:true}))},value)}
async function probe(page){return page.evaluate(()=>{
  const p=window.soundProbe
  return{contexts:p.contexts.map(c=>c.state),gains:p.gains.map(g=>g.gain.value),bgm:p.media.filter(m=>m.loop).length,
    effects:p.media.filter(m=>!m.loop).length,active:p.urls.filter(url=>!p.revoked.includes(url)).length}
})}
async function finishSound(page){await page.evaluate(()=>{const m=window.soundProbe.media.find(m=>!m.loop);m.currentTime=Math.max(0,m.duration-.08)})}
function presented(snapshot,stage,eventname){snapshot.presentedEvents=[{stage,event:{eventname,ch1:'Abel',ch2:'NONE',ch3:'NONE'}}];snapshot.events=snapshot.presentedEvents.map(p=>p.event);snapshot.nextEventCursor=1;return snapshot}

test('WEB-028 live BGM/effect gain, keyboard volume, serial click cleanup and restore never auto-unlock',async({page})=>{
  const paths=await setup(page,{settings:JSON.stringify({version:1,bgmVolume:40,soundVolume:60,muted:false})})
  expect(paths).toEqual([]);expect((await probe(page)).contexts).toEqual([])
  await expect(page.getByRole('slider',{name:'BGM音量',exact:true})).toHaveValue('40')
  await page.getByRole('button',{name:'対局を始める'}).click();await enable(page)
  await page.getByRole('button',{name:'試遊を終了'}).click()
  await expect(ui(page)).toHaveAttribute('data-sound-status','playing');await expect(ui(page)).toHaveAttribute('data-sound','CLICK')
  expect(await probe(page)).toMatchObject({contexts:['running'],bgm:1,effects:1,active:2})
  expect((await probe(page)).gains).toEqual([Math.fround(.1),Math.fround(.15)])
  await expect(ui(page)).toHaveAttribute('data-sound-status','idle');expect((await probe(page)).active).toBe(1)
  await page.getByRole('slider',{name:'BGM音量',exact:true}).focus();await page.keyboard.press('Home');await page.keyboard.press('ArrowRight')
  await expect(page.getByRole('slider',{name:'BGM音量',exact:true})).toHaveValue('1')
  expect((await probe(page)).gains[0]).toBe(Math.fround(.0025))
  await page.getByRole('switch',{name:'消音',exact:true}).click();expect((await probe(page)).gains[0]).toBe(0)
  const count=paths.length;await page.reload();await expect(ui(page)).toHaveAttribute('data-status','off')
  await expect(page.getByRole('switch',{name:'消音',exact:true})).toHaveAttribute('aria-checked','true')
  await expect(page.getByRole('slider',{name:'BGM音量',exact:true})).toHaveValue('1')
  expect((await probe(page)).contexts).toEqual([]);expect(paths.length).toBe(count)
})

test('WEB-028 mute cancels a held effect and suppresses future/retroactive playback; no second context',async({page})=>{
  let held;const intercepted=new Promise(resolve=>{held=resolve})
  await page.route('**'+soundPath('CLICK'),r=>held(r))
  const paths=await setup(page);await page.getByRole('button',{name:'対局を始める'}).click();await enable(page)
  await page.getByRole('button',{name:'試遊を終了'}).click();const route=await intercepted
  await page.getByRole('switch',{name:'消音',exact:true}).click();await route.abort('aborted')
  await expect(ui(page)).toHaveAttribute('data-sound-status','idle')
  await page.getByRole('button',{name:'対局を残す'}).click()
  expect(paths.filter(p=>p===soundPath('CLICK'))).toHaveLength(1)
  await page.getByRole('switch',{name:'消音',exact:true}).click()
  expect(paths.filter(p=>p===soundPath('CLICK'))).toHaveLength(1)
  expect((await probe(page)).contexts).toEqual(['running']);expect((await probe(page)).active).toBe(1)
})

for(const [eventname,last] of [['yjsw','NIGHT_DEATH'],['wsw','PEACEFUL_NIGHT']])test('WEB-028 current night serial intro/result '+eventname+'; native ended releases and manual frame never replays death',async({page})=>{
  const snapshot=presented(publicSnapshot(),'NIGHT_RESULT',eventname)
  const paths=await setup(page,{snapshot});await enable(page)
  await page.getByRole('button',{name:'対局を始める'}).click()
  await expect(ui(page)).toHaveAttribute('data-sound','NIGHTFALL');await expect(ui(page)).toHaveAttribute('data-sound-status','playing')
  expect(paths).not.toContain(soundPath('WOLF_HOWL'));await finishSound(page)
  await expect(ui(page)).toHaveAttribute('data-sound','WOLF_HOWL');await expect(ui(page)).toHaveAttribute('data-sound-status','playing')
  await finishSound(page);await expect(ui(page)).toHaveAttribute('data-sound',last);await expect(ui(page)).toHaveAttribute('data-sound-status','playing')
  expect((await probe(page)).active).toBe(1);expect((await probe(page)).effects).toBe(1)
  await finishSound(page);await expect(ui(page)).toHaveAttribute('data-sound-status','idle');expect((await probe(page)).active).toBe(0)
  if(eventname==='yjsw'){
    await page.getByTestId('stage-frame-toggle').click();await page.getByTestId('stage-frame-toggle').click()
    expect(paths.filter(p=>p===soundPath(last))).toHaveLength(1)
  }
})

test('WEB-028 execution sound follows second displayed frame once and is canceled on confirmation',async({page})=>{
  const snapshot=presented(publicSnapshot(),'EXECUTION','cxs')
  const paths=await setup(page,{snapshot});await enable(page);await page.getByRole('button',{name:'対局を始める'}).click()
  await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-frame','1')
  expect(paths).not.toContain(soundPath('DAY_EXECUTION'))
  await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-frame','2')
  await expect(ui(page)).toHaveAttribute('data-sound','DAY_EXECUTION');await expect(ui(page)).toHaveAttribute('data-sound-status','playing')
  await page.getByTestId('next-event').click();await expect(ui(page)).toHaveAttribute('data-sound-status','idle')
  expect(paths.filter(p=>p===soundPath('DAY_EXECUTION'))).toHaveLength(1)
})

for(const [result,effect] of [['VILLAGE_WIN','VILLAGE_VICTORY'],['WOLF_WIN','WOLF_VICTORY'],['FOX_WIN','FOX_VICTORY']])test('WEB-028 win effect gated by final confirmation '+result,async({page})=>{
  const snapshot=presented(publicSnapshot(),'SETTLEMENT',result==='VILLAGE_WIN'?'crsl':result==='WOLF_WIN'?'rlsl':'yhsl')
  snapshot.phase='FINISHED';snapshot.view.result=result
  const paths=await setup(page,{snapshot});await enable(page);await page.getByRole('button',{name:'対局を始める'}).click()
  await expect(page.getByTestId('next-event')).toBeVisible();expect(paths).not.toContain(soundPath(effect))
  await page.getByTestId('next-event').click();await expect(ui(page)).toHaveAttribute('data-sound',effect)
  await expect(ui(page)).toHaveAttribute('data-sound-status','playing')
  await page.getByRole('button',{name:'再同期'}).click();expect(paths.filter(p=>p===soundPath(effect))).toHaveLength(1)
  expect((await probe(page)).active).toBeLessThanOrEqual(2)
})

test('WEB-028 same-day refresh preserves pending playlist selection (027 array-getter observation)',async({page})=>{
  let held;const intercepted=new Promise(resolve=>{held=resolve})
  await page.route('**/assets/audio/manifest.json',r=>held(r))
  await setup(page);await page.getByRole('button',{name:'対局を始める'}).click()
  await page.getByRole('button',{name:'音楽を有効にする'}).click();const route=await intercepted
  await expect(ui(page)).toHaveAttribute('data-status','loading')
  await page.getByRole('button',{name:'曲を切替'}).click()
  await expect(page.getByRole('button',{name:'曲を切替'})).toBeDisabled()
  await page.getByRole('button',{name:'再同期'}).click()
  await expect(page.getByRole('button',{name:'曲を切替'})).toBeDisabled()
  await route.fulfill({json:catalog})
  await expect(ui(page)).toHaveAttribute('data-track',catalog.playlists.NORMAL_GAME_PLAYLIST[0])
  await expect(ui(page)).toHaveAttribute('data-status','playing')
})

test('WEB-028 corrupt effect is diagnosed without stopping BGM or game sync',async({page})=>{
  await page.route('**'+soundPath('CLICK'),r=>r.fulfill({status:200,contentType:'text/html',body:'<html>SPA</html>'}))
  await setup(page);await page.getByRole('button',{name:'対局を始める'}).click();await enable(page)
  await page.getByRole('button',{name:'試遊を終了'}).click()
  await expect(ui(page)).toContainText('AUDIO_INTEGRITY_FAILED');await expect(ui(page)).toHaveAttribute('data-status','playing')
  expect((await probe(page)).active).toBe(1);await expect(page.getByRole('button',{name:'再同期'})).toBeEnabled()
})

for(const denied of [false,true])test('WEB-028 invalid/blocked settings remain explicit and editable '+denied,async({page})=>{
  await setup(page,{settings:'{"version":999,"muted":"<img onerror>"}',denied})
  await expect(ui(page)).toContainText('AUDIO_SETTINGS_READ_FAILED');await expect(ui(page).locator('img')).toHaveCount(0)
  await level(page,'効果音量',42);await expect(page.getByRole('slider',{name:'効果音量',exact:true})).toHaveValue('42')
  if(denied)await expect(ui(page)).toContainText('AUDIO_SETTINGS_WRITE_FAILED')
  else await expect(ui(page)).not.toContainText('AUDIO_SETTINGS_READ_FAILED')
  expect((await probe(page)).contexts).toEqual([])
})

test('WEB-028 early confirmation cancels delayed execution sound; reduced motion emits second-frame sound immediately',async({page})=>{
  const snapshot=presented(publicSnapshot(),'EXECUTION','cxs')
  const paths=await setup(page,{snapshot});await enable(page);await page.getByRole('button',{name:'対局を始める'}).click()
  await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-frame','1')
  await page.getByTestId('next-event').click()
  // Deliberately outlive the 2000ms transition; no stale audio may arrive after confirmation.
  await page.waitForTimeout(2200);expect(paths).not.toContain(soundPath('DAY_EXECUTION'))
  await page.emulateMedia({reducedMotion:'reduce'})
  await page.evaluate(()=>sessionStorage.clear());await page.reload();await enable(page)
  await page.getByRole('button',{name:'対局を始める'}).click()
  await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-frame','2')
  await expect(ui(page)).toHaveAttribute('data-sound','DAY_EXECUTION')
})

test('WEB-028 actual browser mix contains signal and master mute produces zero samples',async({page})=>{
  await setup(page);await page.getByRole('button',{name:'対局を始める'}).click();await enable(page)
  await page.getByRole('button',{name:'試遊を終了'}).click()
  await expect(ui(page)).toHaveAttribute('data-sound-status','playing')
  await page.evaluate(()=>{
    const p=window.soundProbe,c=p.contexts[0],mix=c.createAnalyser(),silent=c.createGain()
    mix.fftSize=2048;silent.gain.value=0
    p.gains.slice(0,2).forEach(g=>g.connect(mix));mix.connect(silent);silent.connect(c.destination)
    window.mixProbe=mix
  })
  const peak=()=>page.evaluate(async()=>{
    const data=new Float32Array(window.mixProbe.fftSize);let peak=0
    for(let frame=0;frame<16;frame++){
      await new Promise(requestAnimationFrame);window.mixProbe.getFloatTimeDomainData(data)
      for(const sample of data)peak=Math.max(peak,Math.abs(sample))
    }
    return peak
  })
  const audible=await peak();expect(audible).toBeGreaterThan(1e-7);expect(audible).toBeLessThan(.65)
  await page.getByRole('switch',{name:'消音',exact:true}).click()
  await page.evaluate(async()=>{for(let i=0;i<8;i++)await new Promise(requestAnimationFrame)})
  expect(await peak()).toBe(0)
  console.info('WEB-028 sampled mix peak: '+audible+'; muted=0 (not physical speaker listening)')
  await page.screenshot({path:'/tmp/vocr-web028-audio.png',fullPage:true})
})


test('WEB-028 review F1/F2/F3 advancing mouse and keyboard each play one CLICK without duplicate fetch',async({page},info)=>{
  const snapshot=publicSnapshot()
  snapshot.presentedEvents=['gyfo1','gyfo1r','gyho2'].map(eventname=>({stage:'DAY_DIALOGUE',event:{eventname,ch1:'Abel',ch2:'NONE',ch3:'NONE'}}))
  snapshot.events=snapshot.presentedEvents.map(p=>p.event);snapshot.nextEventCursor=3
  const failures=[]
  page.on('requestfailed',request=>{if(new URL(request.url()).pathname===soundPath('CLICK'))failures.push(request.failure()?.errorText)})
  const paths=await setup(page,{snapshot})
  await page.getByRole('button',{name:'対局を始める'}).click();await enable(page)
  const samples=[]
  try {
    for(const mode of ['mouse','keyboard']){
      if(mode==='mouse')await page.getByTestId('next-event').click()
      else {await page.getByTestId('next-event').focus();await page.keyboard.press('Enter')}
      await expect(ui(page)).toHaveAttribute('data-sound','CLICK')
      await expect(ui(page)).toHaveAttribute('data-sound-status','playing')
      await expect(ui(page)).toHaveAttribute('data-status','playing')
      await expect(ui(page)).toHaveAttribute('data-track','EMOTIONALLY_UNSTABLE')
      await expect(ui(page)).toHaveAttribute('data-sound-status','idle')
      const counts=await page.evaluate(()=>({clicks:window.soundProbe.nextClicks,plays:window.soundProbe.effectPlays}))
      const saved=await page.evaluate(()=>JSON.parse(sessionStorage.getItem('vocr.guest.v1')))
      samples.push({mode,...counts,cursor:saved.eventCursor,requests:paths.filter(p=>p===soundPath('CLICK')).length,failures:[...failures]})
      expect(samples.at(-1)).toMatchObject({clicks:samples.length,plays:samples.length,cursor:samples.length,requests:samples.length,failures:[]})
      expect((await probe(page)).effects).toBe(1)
      expect((await probe(page)).active).toBe(1)
    }
  }finally{
    const counts=await page.evaluate(()=>({clicks:window.soundProbe.nextClicks,plays:window.soundProbe.effectPlays}))
    const evidence={samples,final:{...counts,requests:paths.filter(p=>p===soundPath('CLICK')).length,failures}}
    console.info('review CLICK evidence: '+JSON.stringify(evidence))
    await info.attach('review-click.json',{body:JSON.stringify(evidence),contentType:'application/json'})
  }
})
