import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { bgmScene, isLateBgm } from '../src/bgmScene.js'
import { createBgmPlayer, BGM_GAIN } from '../src/bgmPlayer.js'
import { resolveAudio } from '../src/audioAssets.js'
const read = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const catalog = JSON.parse(read('../public/assets/audio/manifest.json'))
const tick = () => new Promise(resolve => setImmediate(resolve))
const deferred = () => { let resolve, reject; const promise = new Promise((a,b) => { resolve=a; reject=b }); return {promise,resolve,reject} }
function rig(overrides={}) {
  const events=[], states=[], blobs=new Set(), requests=[]
  const context = { state:'suspended', destination:{}, createGain:()=>gain,
    createMediaElementSource:()=>source,
    resume:async()=>{ events.push('resume'); context.state='running' },
    suspend:async()=>{ events.push('suspend'); context.state='suspended' },
    close:async()=>{ events.push('close'); context.state='closed' } }
  const gain = {gain:{value:1},connect:()=>{},disconnect:()=>events.push('gain-disconnect')}
  const source = {connect:()=>{},disconnect:()=>events.push('source-disconnect')}
  const media = {src:'',paused:true,preload:null,loop:false,
    pause:()=>{events.push('pause');media.paused=true}, removeAttribute:()=>{media.src=''},load:()=>events.push('clear'),
    play:async()=>{events.push('play');media.paused=false} }
  let counter=0
  const player=createBgmPlayer({
    resources:{load:async(kind,id,{signal})=>{requests.push({id,signal});return {asset:{mime:'audio/mpeg'},data:new Uint8Array([1,2]).buffer}}},
    createContext:()=>{events.push('context');return context},createMedia:()=>media,
    urls:{createObjectURL:()=>{const url='blob:'+counter++;blobs.add(url);return url},revokeObjectURL:url=>{assert.ok(blobs.delete(url));events.push('revoke')}},
    notify:state=>states.push(state),...overrides,
  })
  return {player,context,media,gain,events,states,requests,blobs}
}

test('desktop BGM intent uses exact alive arithmetic and current presentation only',()=>{
  const source=read('../../src/main/java/com/vocr/ui/UIHelpers.java')
  assert.match(source,/\(ui.ctx.getAliveCounter\(\) - 1\) \/ 2 == 1[\s\S]*?BgmTrack.LIGHTLESS_GAZE[\s\S]*?BgmTrack.EMOTIONALLY_UNSTABLE/)
  assert.deepEqual([0,1,2,3,4,5,44].map(isLateBgm),[false,false,false,true,true,false,false])
  const update={phase:'FINISHED',view:{gameDay:2,aliveCount:4,result:'FOX_WIN'}}
  const base={sessionId:'a',update}
  for(const stage of ['DAY_DIALOGUE','AFTERNOON_DIALOGUE'])assert.equal(bgmScene({...base,currentEvent:{stage}}).id,'LIGHTLESS_GAZE')
  for(const stage of ['NIGHT_RESULT','EXECUTION','unknown'])assert.equal(bgmScene({...base,currentEvent:{stage}}).id,null)
  assert.equal(bgmScene({...base,currentEvent:{stage:'SETTLEMENT'}}).id,'DEFEAT_SCREEN')
  assert.equal(bgmScene({...base,finished:true}).id,'DEFEAT_SCREEN')
  update.view.result='VILLAGE_WIN';assert.equal(bgmScene({...base,finished:true}).id,'VICTORY_SCREEN')
  update.view.result='toString';assert.equal(bgmScene({...base,finished:true}).id,null)
  assert.equal(bgmScene({}).id,'START_MENU');assert.equal(bgmScene({sessionId:'a'}).id,null)
  assert.equal(bgmScene({...base,finished:true,uncertain:true}).id,null)
  update.phase='DAY_DECISION';update.view.aliveCount=44
  assert.equal(bgmScene(base).playlist,'NORMAL_GAME_PLAYLIST')
  assert.equal(bgmScene({...base,currentEvent:{stage:'DAY_DIALOGUE'}}).key,bgmScene(base).key)
})
test('scene selection does not read private roles, pending/future events or hidden data',()=>{
  const forbidden=()=>{throw Error('private read')}
  const update={phase:'DAY_DECISION',view:{gameDay:1,aliveCount:3}}
  for(const key of ['actualRole','revealedRoles','presentedEvents','events'])Object.defineProperty(update,key,{get:forbidden})
  Object.defineProperty(update.view,'players',{get:forbidden});Object.defineProperty(update.view,'result',{get:forbidden})
  assert.equal(bgmScene({update,currentEvent:{stage:'DAY_DIALOGUE'}}).id,'LIGHTLESS_GAZE')
})
test('fixed desktop call sites and playlist switching remain source locked',()=>{
  const start=read('../../src/main/java/com/vocr/ui/StartSceneHandler.java')
  const execution=read('../../src/main/java/com/vocr/ui/DialogueChuxingHandler.java')
  const night=read('../../src/main/java/com/vocr/ui/GameSceneHandler.java')
  const end=read('../../src/main/java/com/vocr/ui/EndGameHandler.java')
  const vote=read('../../src/main/java/com/vocr/ui/GameSceneVoteHandler.java')
  assert.match(start,/playBgm\(BgmTrack.START_MENU\)/);assert.match(execution,/stopBgm\(\)/)
  assert.match(night,/private void renderNight[\s\S]*?stopBgm\(\)/)
  assert.match(end,/case END_FOX:[\s\S]*?BgmTrack.DEFEAT_SCREEN[\s\S]*?case END_WOLF:[\s\S]*?BgmTrack.DEFEAT_SCREEN[\s\S]*?BgmTrack.VICTORY_SCREEN/)
  assert.match(vote,/\(ui.ctx.getAliveCounter\(\) - 1\) \/ 2 == 1/)
  assert.match(vote,/playlist.get\(ui.count % playlist.size\(\)\)[\s\S]*?ui.count\+\+/)
})
test('resource acceptance stays aligned to generated bitrate/budget policy',()=>{
  // The resolver remains restrictive; this test detects POLICY/JS drift when regenerating artifacts.
  for(const kind of ['bgm','sounds'])for(const [id,entry] of Object.entries(catalog[kind])){
    assert.ok(resolveAudio(catalog,kind,id))
    const rates=kind==='bgm'?catalog.policy.bgmBitrates:[catalog.policy.soundBitrate]
    for(const bitrate of rates)assert.ok(resolveAudio({...catalog,[kind]:{[id]:{...entry,bitrate}}},kind,id))
    const budget=kind==='bgm'?catalog.policy.singleBgmBudget:catalog.policy.soundBudget
    assert.ok(resolveAudio({...catalog,[kind]:{[id]:{...entry,bytes:budget}}},kind,id))
    assert.equal(resolveAudio({...catalog,[kind]:{[id]:{...entry,bytes:budget+1}}},kind,id),null)
  }
})
test('no import/selection allocation; explicit enable resumes first and uses one attenuated looping voice',async()=>{
  const r=rig();r.player.select('START_MENU');assert.deepEqual(r.events,[]);assert.deepEqual(r.requests,[])
  await r.player.enable();assert.equal(r.events[0],'context');assert.equal(r.events[1],'resume')
  assert.equal(r.media.loop,true);assert.equal(r.media.preload,'none');assert.equal(r.gain.gain.value,BGM_GAIN)
  const maxPeak=Math.max(...Object.values(catalog.bgm).map(e=>e.analysis.peakDb))
  assert.ok(maxPeak+20*Math.log10(BGM_GAIN)<-8)
  assert.equal(r.states.at(-1).status,'playing');assert.equal(r.blobs.size,1)
  r.player.select('START_MENU');await tick();assert.equal(r.requests.length,1)
  r.player.dispose();assert.equal(r.blobs.size,0);assert.equal(r.media.src,'');assert.equal(r.context.state,'closed')
})
test('switch stops/revokes before replacement and silence cancels without fallback',async()=>{
  const r=rig();r.player.select('START_MENU');await r.player.enable()
  const old=r.requests[0].signal;r.player.select('LIGHTLESS_GAZE');await tick()
  assert.equal(old.aborted,true);assert.equal(r.blobs.size,1);assert.equal(r.requests.length,2)
  assert.equal(r.events.filter(e=>e==='context').length,1)
  r.player.select(null);await tick();assert.equal(r.media.paused,true);assert.equal(r.blobs.size,0)
  assert.equal(r.states.at(-1).status,'silent');r.player.dispose()
})
test('out of order unabortable downloads never replace newer audio; disable invalidates all late completions',async()=>{
  const jobs=[]
  const r=rig({resources:{load:async()=>{const job=deferred();jobs.push(job);return job.promise}}})
  r.player.select('FIRST');const enable=r.player.enable();await tick()
  r.player.select('SECOND');await tick()
  const payload={asset:{mime:'audio/mpeg'},data:new Uint8Array([1]).buffer}
  jobs[1].resolve(payload);await tick();assert.equal(r.states.at(-1).id,'SECOND')
  jobs[0].resolve(payload);await enable;assert.equal(r.blobs.size,1);assert.equal(r.states.at(-1).id,'SECOND')
  r.player.select('THIRD');await tick();r.player.disable();jobs[2].resolve(payload);await tick()
  assert.equal(r.blobs.size,0);assert.equal(r.states.at(-1).enabled,false);r.player.dispose()
})
test('disable during context unlock prevents cold track fetch; reenable reuses same context',async()=>{
  const r=rig(),hold=deferred();r.context.resume=()=>hold.promise
  r.player.select('START_MENU');const pending=r.player.enable();await r.player.enable();r.player.disable()
  r.context.state='running';hold.resolve();await pending;assert.equal(r.requests.length,0)
  r.context.resume=async()=>{r.context.state='running'}
  await r.player.enable();assert.equal(r.events.filter(e=>e==='context').length,1)
  r.player.dispose()
})
test('blocked play, media errors and context interruption fail closed without automatic retries',async()=>{
  const r=rig();r.media.play=async()=>{throw new DOMException('blocked','NotAllowedError')}
  r.player.select('START_MENU');await r.player.enable()
  assert.equal(r.states.at(-1).error,'BGM_GESTURE_REQUIRED');assert.equal(r.blobs.size,0)
  r.media.play=async()=>{};await r.player.enable();assert.equal(r.states.at(-1).status,'playing')
  r.media.onerror();assert.equal(r.states.at(-1).error,'BGM_MEDIA_FAILED');assert.equal(r.blobs.size,0)
  await r.player.enable();r.context.state='suspended';r.context.onstatechange()
  assert.equal(r.states.at(-1).error,'BGM_GESTURE_REQUIRED');r.player.dispose()
})
test('load and unlock timeout have explicit diagnostics and release resources',async()=>{
  const r=rig({timeoutMs:10,resources:{load:()=>new Promise(()=>{})}})
  r.player.select('START_MENU');void r.player.enable();await new Promise(resolve=>setTimeout(resolve,30))
  assert.equal(r.states.at(-1).error,'BGM_TIMEOUT');assert.equal(r.media.src,'');r.player.dispose()
  const q=rig({timeoutMs:10});q.context.resume=()=>new Promise(()=>{})
  await q.player.enable();assert.equal(q.states.at(-1).error,'BGM_GESTURE_REQUIRED');q.player.dispose()
})
test('unsupported audio context fails explicitly without fetching or throwing into game UI',async()=>{
  const r=rig({createContext:()=>{throw Error('not supported')}})
  r.player.select('START_MENU');await r.player.enable()
  assert.equal(r.states.at(-1).error,'BGM_UNAVAILABLE');assert.equal(r.requests.length,0)
  r.player.dispose();r.player.select('OTHER');await r.player.enable();assert.equal(r.requests.length,0)
})
test('partially failed Web Audio graph is closed; retry cannot bypass attenuation or use an unconnected media element',async()=>{
  const r=rig();r.context.createMediaElementSource=()=>{throw Error('device graph unavailable')}
  r.player.select('START_MENU');await r.player.enable();await r.player.enable()
  assert.equal(r.requests.length,0)
  assert.equal(r.events.filter(e=>e==='close').length,2)
  assert.equal(r.states.at(-1).enabled,false);r.player.dispose()
})
test('dispose cancels pending unlock immediately and reports a failed close rather than hiding it',async()=>{
  const r=rig();r.context.resume=()=>new Promise(()=>{})
  const pending=r.player.enable();r.player.dispose();await pending
  assert.equal(r.requests.length,0);assert.equal(r.context.state,'closed')
  const q=rig();await q.player.enable();q.context.close=()=>Promise.reject(Error('close failed'))
  q.player.dispose();await tick();assert.equal(q.states.at(-1).error,'BGM_RELEASE_FAILED')
})
