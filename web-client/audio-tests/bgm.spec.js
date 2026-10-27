import { test, expect } from '@playwright/test'
import { readFileSync } from 'node:fs'
import { publicSnapshot } from '../test-fixtures/public-players.js'
const manifest=JSON.parse(readFileSync(new URL('../public/assets/audio/manifest.json',import.meta.url),'utf8'))
const id='00000000-0000-4000-8000-000000000027'
const ui=page=>page.locator('.bgm-control')
const path=id=>'/assets/audio/'+manifest.bgm[id].file
async function setup(page,snapshot=publicSnapshot()) {
  const paths=[]
  page.on('request',r=>{if(r.url().includes('/assets/audio/'))paths.push(new URL(r.url()).pathname)})
  await page.addInitScript(()=>{
    window.bgmProbe={contexts:[],media:[],gains:[],urls:[],revoked:[]}
    const probe=window.bgmProbe,NativeContext=window.AudioContext,NativeAudio=window.Audio
    window.AudioContext=class extends NativeContext{
      constructor(...args){super(...args);probe.contexts.push(this)}
      createGain(){const node=super.createGain();probe.gains.push(node);return node}
    }
    window.Audio=function(...args){const media=new NativeAudio(...args);probe.media.push(media);return media}
    const create=URL.createObjectURL.bind(URL),revoke=URL.revokeObjectURL.bind(URL)
    URL.createObjectURL=blob=>{const url=create(blob);probe.urls.push(url);return url}
    URL.revokeObjectURL=url=>{probe.revoked.push(url);revoke(url)}
  })
  await page.route('**/api/v1/games',route=>route.fulfill({status:201,json:{sessionId:id,update:snapshot}}))
  await page.route('**/api/v1/games/'+id+'?**',route=>{
    const cursor=Number(new URL(route.request().url()).searchParams.get('eventCursor')||0)
    return route.fulfill({json:{...snapshot,eventCursor:cursor,events:snapshot.events.slice(cursor),presentedEvents:snapshot.presentedEvents.slice(cursor)}})
  })
  await page.route('**/api/v1/games/'+id,route=>route.fulfill({status:204}))
  await page.goto('/')
  return paths
}
async function enable(page,id) {
  await page.getByRole('button',{name:'音楽を有効にする',exact:true}).click()
  await expect(ui(page)).toHaveAttribute('data-status','playing')
  await expect(ui(page)).toHaveAttribute('data-track',id)
}
async function probe(page) {
  return page.evaluate(()=>{
    const p=window.bgmProbe,m=p.media.find(m=>m.loop)
    return{contexts:p.contexts.map(c=>c.state),media:p.media.filter(m=>m.loop).length,soundMedia:p.media.filter(m=>!m.loop).length,src:m?.getAttribute('src'),
      paused:m?.paused,time:m?.currentTime,gain:p.gains[0]?.gain.value,loop:m?.loop,
      active:m?.getAttribute('src') && !p.revoked.includes(m.getAttribute('src')) ? 1 : 0,totalActive:p.urls.filter(url=>!p.revoked.includes(url)).length}
  })
}
function events(snapshot,list) {
  snapshot.presentedEvents=list.map(([stage,eventname])=>({stage,event:{eventname,ch1:'Abel',ch2:'NONE',ch3:'NONE'}}))
  snapshot.events=snapshot.presentedEvents.map(p=>p.event);snapshot.nextEventCursor=list.length
  return snapshot
}

test('WEB-027 cold lobby is silent until keyboard gesture; real playback loops at attenuated gain and stops cleanly',async({page})=>{
  const paths=await setup(page)
  await expect(ui(page)).toHaveAttribute('data-status','off');expect(paths).toEqual([])
  expect((await probe(page)).contexts).toEqual([])
  await page.getByRole('button',{name:'音楽を有効にする'}).focus();await page.keyboard.press('Enter')
  await expect(ui(page)).toHaveAttribute('data-status','playing')
  expect(paths).toEqual(['/assets/audio/manifest.json',path('START_MENU')])
  await expect.poll(async()=>(await probe(page)).time).toBeGreaterThan(.1)
  expect(await probe(page)).toMatchObject({contexts:['running'],media:1,paused:false,gain:.25,loop:true,active:1})
  // Cross a real native media loop without downloading/decoding the next track.
  await page.evaluate(()=>{const m=window.bgmProbe.media[0];m.currentTime=m.duration-.15})
  await expect.poll(async()=>(await probe(page)).time).toBeLessThan(2)
  expect(paths).toHaveLength(2)
  const enabledLobbyBytes=await page.evaluate(()=>[...performance.getEntriesByType('navigation'),...performance.getEntriesByType('resource')].reduce((sum,e)=>sum+e.transferSize,0))
  expect(enabledLobbyBytes).toBeGreaterThan(manifest.bgm.START_MENU.bytes)
  expect(enabledLobbyBytes).toBeLessThanOrEqual(1.5*1024**2)
  console.info('WEB-027 cold enabled lobby bytes: '+enabledLobbyBytes)
  await page.screenshot({path:'/tmp/vocr-web027-bgm.png',fullPage:true})
  await page.getByRole('button',{name:'音楽を停止',exact:true}).click()
  await expect.poll(async()=>(await probe(page)).contexts).toEqual(['suspended'])
  expect(await probe(page)).toMatchObject({src:null,paused:true,active:0})
  await enable(page,'START_MENU');expect((await probe(page)).media).toBe(1)
  expect(paths.filter(p=>p.endsWith('manifest.json'))).toHaveLength(1)
})

test('WEB-027 day dialogue and CO keep one track; decision playlist cycles exact source order; refresh stays off',async({page})=>{
  const snapshot=events(publicSnapshot(),[['DAY_DIALOGUE','gyfo1'],['AFTERNOON_DIALOGUE','gyfo1r']])
  const paths=await setup(page,snapshot)
  await page.getByRole('button',{name:'対局を始める'}).click();await enable(page,'EMOTIONALLY_UNSTABLE')
  await page.getByTestId('next-event').click();await expect(ui(page)).toHaveAttribute('data-track','EMOTIONALLY_UNSTABLE')
  await page.getByTestId('next-event').click();await expect(page.getByRole('button',{name:'曲を切替'})).toBeEnabled()
  expect(paths.filter(p=>p.includes('/bgm/') && p.endsWith('.mp3'))).toEqual([path('EMOTIONALLY_UNSTABLE')])
  for(const id of manifest.playlists.NORMAL_GAME_PLAYLIST.slice(0,2)){
    await page.getByRole('button',{name:'曲を切替'}).click()
    await expect(ui(page)).toHaveAttribute('data-status','playing');await expect(ui(page)).toHaveAttribute('data-track',id)
    expect((await probe(page)).active).toBe(1)
  }
  expect((await probe(page)).contexts).toEqual(['running'])
  const before=paths.length;await page.reload();await expect(ui(page)).toHaveAttribute('data-status','off')
  expect(paths.length).toBe(before);expect((await probe(page)).contexts).toEqual([])
  await expect(page.getByRole('button',{name:'曲を切替'})).toBeDisabled()
})

test('WEB-027 execution/night are silent; settlement music waits for displayed stage, never leaks future result',async({page})=>{
  const snapshot=events(publicSnapshot(),[['EXECUTION','cxs'],['NIGHT_RESULT','wsw'],['DAY_DIALOGUE','gyfo1'],['SETTLEMENT','crsl']])
  snapshot.phase='FINISHED';snapshot.view.result='VILLAGE_WIN'
  const paths=await setup(page,snapshot)
  await page.getByRole('button',{name:'対局を始める'}).click()
  await page.getByRole('button',{name:'音楽を有効にする'}).click();await expect(ui(page)).toHaveAttribute('data-status','silent')
  expect(paths).toEqual([])
  await page.getByTestId('next-event').click();expect(paths.filter(p=>p.includes('/bgm/'))).toEqual([])
  await page.getByTestId('next-event').click();await expect(ui(page)).toHaveAttribute('data-track','EMOTIONALLY_UNSTABLE')
  await expect(ui(page)).toHaveAttribute('data-status','playing')
  expect(paths).not.toContain(path('VICTORY_SCREEN'))
  await page.getByTestId('next-event').click();await expect(ui(page)).toHaveAttribute('data-track','VICTORY_SCREEN')
  await expect(ui(page)).toHaveAttribute('data-status','playing')
  expect((await probe(page)).active).toBe(1)
  const count=paths.filter(p=>p.includes('/bgm/')).length;await page.getByTestId('next-event').click()
  await expect(page.getByTestId('settlement-board')).toBeVisible();expect(paths.filter(p=>p.includes('/bgm/')).length).toBe(count)
})

test('WEB-027 late playlist uses four survivors too; release selects lobby and hidden/pagehide require a new gesture',async({page})=>{
  const snapshot=publicSnapshot();snapshot.view.aliveCount=4
  const paths=await setup(page,snapshot)
  await page.getByRole('button',{name:'対局を始める'}).click();await enable(page,'LIGHTLESS_GAZE')
  await page.getByRole('button',{name:'曲を切替'}).click()
  await expect(ui(page)).toHaveAttribute('data-track',manifest.playlists.LATE_GAME_PLAYLIST[0])
  await expect(ui(page)).toHaveAttribute('data-status','playing')
  // Actual visibility event contract: js property override is confined to this test.
  await page.evaluate(()=>{Object.defineProperty(document,'hidden',{configurable:true,get:()=>true});document.dispatchEvent(new Event('visibilitychange'))})
  await expect(ui(page)).toHaveAttribute('data-status','off');expect((await probe(page)).active).toBe(0)
  const before=paths.length
  await page.evaluate(()=>{Object.defineProperty(document,'hidden',{configurable:true,get:()=>false});document.dispatchEvent(new Event('visibilitychange'))})
  await expect(ui(page)).toHaveAttribute('data-status','off');expect(paths.length).toBe(before)
  await enable(page,'LAST_BOSS_02')
  await page.evaluate(()=>window.dispatchEvent(new PageTransitionEvent('pagehide',{persisted:true})))
  await expect(ui(page)).toHaveAttribute('data-status','off')
  await enable(page,'LAST_BOSS_02')
  await page.getByRole('button',{name:'試遊を終了'}).click()
  await page.getByRole('button',{name:'解放を確定'}).click()
  await expect(ui(page)).toHaveAttribute('data-track','START_MENU')
  await expect(ui(page)).toHaveAttribute('data-status','playing')
  expect((await probe(page)).active).toBe(1)
  await page.evaluate(()=>window.dispatchEvent(new PageTransitionEvent('pagehide',{persisted:false})))
  await expect.poll(async()=>(await probe(page)).contexts).toEqual(['closed'])
  expect((await probe(page)).active).toBe(0)
})

test('WEB-027 late cold request is canceled on new scene; no stale source, overlap or unhandled play rejection',async({page})=>{
  const errors=[];page.on('pageerror',error=>errors.push(error.message))
  let held;const intercepted=new Promise(resolve=>{held=resolve})
  await page.route('**'+path('START_MENU'),route=>held(route))
  const paths=await setup(page)
  await page.getByRole('button',{name:'音楽を有効にする'}).click()
  const route=await intercepted
  await expect(ui(page)).toHaveAttribute('data-status','loading')
  await page.getByRole('button',{name:'対局を始める'}).click()
  await expect(ui(page)).toHaveAttribute('data-track','EMOTIONALLY_UNSTABLE')
  await expect(ui(page)).toHaveAttribute('data-status','playing')
  await route.abort('aborted')
  expect((await probe(page)).active).toBe(1);expect((await probe(page)).media).toBe(1)
  expect(paths.filter(p=>p.includes('/bgm/') && p.endsWith('.mp3'))).toEqual([path('START_MENU'),path('EMOTIONALLY_UNSTABLE')])
  expect(errors).toEqual([])
})

test('WEB-027 corruption and denied play are visible, silent failures; game controls remain usable',async({page})=>{
  await setup(page)
  await page.route('**'+path('START_MENU'),route=>route.fulfill({status:200,contentType:'text/html',body:'<html>SPA</html>'}))
  await page.getByRole('button',{name:'音楽を有効にする'}).click()
  await expect(ui(page)).toHaveAttribute('data-status','error');await expect(ui(page)).toContainText('AUDIO_INTEGRITY_FAILED')
  expect((await probe(page)).active).toBe(0)
  await page.getByRole('button',{name:'対局を始める'}).click();await expect(page.locator('.player-row')).toHaveCount(44)
  // Simulate browser/device denial, preserving actual resource loading and the UI failure path.
  await page.evaluate(()=>{HTMLMediaElement.prototype.play=()=>Promise.reject(new DOMException('denied','NotAllowedError'))})
  await page.getByRole('button',{name:'音楽を有効にする'}).click()
  await expect(ui(page)).toContainText('BGM_GESTURE_REQUIRED');expect((await probe(page)).active).toBe(0)
  await expect(page.getByRole('button',{name:'再同期'})).toBeEnabled()
})

test('WEB-033 Replay reuses the single gesture-unlocked context, switches to source track and releases on close without game writes',async({page})=>{
  const {replayId}=await import('../src/replayStore.js')
  const replay=JSON.parse(readFileSync(new URL('../../src/test/resources/replay/exchange-v3.json',import.meta.url),'utf8'))[0]
  const recordingId=await replayId(replay)
  await setup(page)
  await page.getByRole('button',{name:'対局を始める'}).click()
  await enable(page,'EMOTIONALLY_UNSTABLE')
  const before=await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))
  await page.evaluate(async entry=>new Promise((resolve,reject)=>{
    const r=indexedDB.open('vocr.replays.v1',1);r.onupgradeneeded=()=>r.result.createObjectStore('recordings',{keyPath:'id'})
    r.onsuccess=()=>{const db=r.result,tx=db.transaction('recordings','readwrite');tx.objectStore('recordings').put(entry);tx.oncomplete=()=>{db.close();resolve()};tx.onabort=()=>reject(tx.error)}
    r.onerror=()=>reject(r.error)
  }),{id:recordingId,replay,bytes:Buffer.byteLength(JSON.stringify(replay)),savedAt:'2026-01-02T03:04:05.000Z'})
  let writes=0;page.on('request',r=>{if(r.url().includes('/api/v1/games')&&r.method()!=='GET')writes++})
  await page.getByRole('button',{name:'Replay一覧',exact:true}).click()
  await page.getByRole('button',{name:'再生する',exact:true}).click()
  const modal=page.getByRole('dialog',{name:/Replay再生/})
  await expect(ui(page)).toHaveAttribute('data-track','REPLAY_PLAYER')
  await expect(ui(page)).toHaveAttribute('data-status','playing')
  expect(await probe(page)).toMatchObject({contexts:['running'],media:1,active:1})
  await modal.getByRole('button',{name:'録画の音楽を停止',exact:true}).click()
  await expect(ui(page)).toHaveAttribute('data-status','off')
  await modal.getByRole('button',{name:'録画の音楽を有効にする',exact:true}).click()
  await expect(ui(page)).toHaveAttribute('data-track','REPLAY_PLAYER')
  await expect(ui(page)).toHaveAttribute('data-status','playing')
  await modal.getByRole('button',{name:'次の日',exact:true}).click()
  await expect(ui(page)).toHaveAttribute('data-track','REPLAY_PLAYER')
  expect(await probe(page)).toMatchObject({contexts:['running'],media:1,active:1})
  await page.keyboard.press('Escape')
  await expect(ui(page)).toHaveAttribute('data-track','EMOTIONALLY_UNSTABLE')
  await expect(ui(page)).toHaveAttribute('data-status','playing')
  expect(await probe(page)).toMatchObject({contexts:['running'],media:1,active:1})
  expect(writes).toBe(0);expect(await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))).toBe(before)
})

test('WEB-033 Replay audio failure is visible inside the modal, navigation remains usable and explicit retry keeps one context',async({page})=>{
  const {replayId}=await import('../src/replayStore.js')
  const replay=JSON.parse(readFileSync(new URL('../../src/test/resources/replay/exchange-v3.json',import.meta.url),'utf8'))[0]
  const recordingId=await replayId(replay)
  await setup(page)
  await page.evaluate(async entry=>new Promise((resolve,reject)=>{
    const r=indexedDB.open('vocr.replays.v1',1);r.onupgradeneeded=()=>r.result.createObjectStore('recordings',{keyPath:'id'})
    r.onsuccess=()=>{const db=r.result,tx=db.transaction('recordings','readwrite');tx.objectStore('recordings').put(entry);tx.oncomplete=()=>{db.close();resolve()};tx.onabort=()=>reject(tx.error)}
    r.onerror=()=>reject(r.error)
  }),{id:recordingId,replay,bytes:Buffer.byteLength(JSON.stringify(replay)),savedAt:'2026-01-02T03:04:05.000Z'})
  await page.getByRole('button',{name:'Replay一覧',exact:true}).click()
  await page.getByRole('button',{name:'再生する',exact:true}).click()
  const modal=page.getByRole('dialog',{name:/Replay再生/})
  expect((await probe(page)).contexts).toEqual([])
  await page.route('**'+path('REPLAY_PLAYER'),r=>r.abort())
  await modal.getByRole('button',{name:'録画の音楽を有効にする',exact:true}).click()
  await expect(modal.getByRole('alert')).toContainText('音声診断')
  await modal.getByRole('button',{name:'次の日',exact:true}).click()
  await expect(modal.getByTestId('replay-day')).toHaveText('第3日 / 4日')
  await page.unroute('**'+path('REPLAY_PLAYER'))
  await modal.getByRole('button',{name:'録画の音楽を有効にする',exact:true}).click()
  await expect(modal.getByTestId('replay-audio-status')).toContainText('再生中')
  await expect(modal.getByRole('alert')).toHaveCount(0)
  expect((await probe(page)).contexts).toEqual(['running'])
})
