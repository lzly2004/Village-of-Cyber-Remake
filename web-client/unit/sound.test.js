import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { defaultAudioSettings,readAudioSettings,saveAudioSettings,validAudioSettings,AUDIO_SETTINGS_KEY } from '../src/audioSettings.js'
import { enteringSounds,transitionSound } from '../src/soundScene.js'
import { createSoundChannel,SOUND_GAIN } from '../src/soundChannel.js'
import { createBgmPlayer,BGM_GAIN } from '../src/bgmPlayer.js'
const read=path=>readFileSync(new URL(path,import.meta.url),'utf8')
const tick=()=>new Promise(resolve=>setImmediate(resolve))
const event=(stage,eventname)=>({stage,event:{eventname}})
function rig(extra={}) {
  const requests=[],states=[],nodes=[],media=[],urls=new Set();let count=0,allowed=true
  const context={state:'running',destination:{},createMediaElementSource:()=>({connect(){},disconnect(){}}),
    createGain:()=>{const node={gain:{value:1},connect(){},disconnect(){}};nodes.push(node);return node},
    resume:async()=>{context.state='running'},suspend:async()=>{context.state='suspended'},close:async()=>{context.state='closed'}}
  const config={getContext:()=>context,allowed:()=>allowed,timeoutMs:1000,
    resources:{load:async(kind,id,{signal})=>{requests.push({kind,id,signal});return {asset:{mime:'audio/mpeg',duration:.1},data:new ArrayBuffer(4)}}},
    createMedia:()=>{const m={src:'',pause(){},removeAttribute(){this.src=''},load(){},play:async()=>{}};media.push(m);return m},
    urls:{createObjectURL:()=>{const url='blob:'+count++;urls.add(url);return url},revokeObjectURL:url=>{assert.ok(urls.delete(url))}},
    notify:state=>states.push(state),...extra}
  return{config,context,requests,states,nodes,media,urls,setAllowed:value=>{allowed=value}}
}

test('settings strict versioned namespace, roundtrip, defaults and storage denial; never stores gesture enable',()=>{
  const store=new Map(),storage={getItem:key=>store.get(key)??null,setItem:(key,value)=>store.set(key,value)}
  assert.deepEqual(readAudioSettings(storage).settings,defaultAudioSettings())
  const settings={version:1,bgmVolume:23,soundVolume:42,muted:true}
  assert.equal(saveAudioSettings(storage,settings),null);assert.deepEqual(readAudioSettings(storage),{settings,warning:null})
  assert.deepEqual([...store.keys()],[AUDIO_SETTINGS_KEY])
  for(const bad of [null,[],{}, {...settings,version:2},{...settings,bgmVolume:-1},{...settings,soundVolume:101},
    {...settings,bgmVolume:1.5},{...settings,muted:1},{...settings,enabled:true}])assert.ok(!validAudioSettings(bad))
  storage.setItem(AUDIO_SETTINGS_KEY,'<img onerror>');assert.equal(readAudioSettings(storage).warning,'AUDIO_SETTINGS_READ_FAILED')
  const denied={getItem(){throw Error('denied')},setItem(){throw Error('quota')}}
  assert.equal(readAudioSettings(denied).warning,'AUDIO_SETTINGS_READ_FAILED')
  assert.equal(saveAudioSettings(denied,settings),'AUDIO_SETTINGS_WRITE_FAILED')
})
test('all nine sound purposes follow desktop call sites, with no hidden subtype or future-result lookup',()=>{
  assert.deepEqual(enteringSounds(event('NIGHT_RESULT','wsw'),false,null,true),['NIGHTFALL','WOLF_HOWL','PEACEFUL_NIGHT'])
  assert.deepEqual(enteringSounds(event('NIGHT_RESULT','yjsw'),false,null,false),[])
  assert.equal(transitionSound(event('NIGHT_RESULT','yjsw')),'NIGHT_DEATH')
  for(const name of ['cxs','hzsw','mzsw'])assert.equal(transitionSound(event('EXECUTION',name)),'DAY_EXECUTION')
  for(const [result,id]of [['VILLAGE_WIN','VILLAGE_VICTORY'],['WOLF_WIN','WOLF_VICTORY'],['FOX_WIN','FOX_VICTORY']]){
    assert.deepEqual(enteringSounds(event('SETTLEMENT','crsl'),false,result,false),[])
    assert.deepEqual(enteringSounds(null,true,result,false),[id])
  }
  assert.deepEqual(enteringSounds(null,true,'toString',false),[])
  assert.equal(transitionSound(event('EXECUTION','toString')),null)
  for(const [file,ids]of [['GameSceneHandler',['CLICK','NIGHTFALL','WOLF_HOWL']],['DialogueChuxingHandler',['DAY_EXECUTION']],
    ['DialogueDayDeathHandler',['NIGHT_DEATH','PEACEFUL_NIGHT']],['EndGameHandler',['VILLAGE_VICTORY','WOLF_VICTORY','FOX_VICTORY']]]){
    const source=read('../../src/main/java/com/vocr/ui/'+file+'.java')
    for(const id of ids)assert.ok(source.includes('playSound(SoundEffect.'+id+')'))
  }
  const current=event('NIGHT_RESULT','yjsw')
  for(const key of ['actualRole','deathReason','ch3'])Object.defineProperty(current.event,key,{get(){throw Error('private')}})
  assert.equal(transitionSound(current),'NIGHT_DEATH')
})
test('channel allocates nothing before permission; serial queue releases ended URLs and bounds one voice',async()=>{
  const r=rig(),channel=createSoundChannel(r.config);r.setAllowed(false)
  channel.enqueue(['CLICK']);assert.equal(r.requests.length,0);assert.equal(r.media.length,0)
  r.setAllowed(true);channel.enqueue(['NIGHTFALL','WOLF_HOWL']);await tick()
  channel.enqueue(['NIGHT_DEATH']);assert.equal(r.urls.size,1)
  channel.enqueue(['CLICK'],{click:true});assert.equal(r.requests.length,1)
  r.media[0].onended();await tick();assert.deepEqual(r.requests.map(r=>r.id),['NIGHTFALL','WOLF_HOWL'])
  r.media[0].onended();await tick();assert.equal(r.requests.at(-1).id,'NIGHT_DEATH')
  r.media[0].onended();await tick();assert.equal(r.urls.size,0);assert.equal(r.states.at(-1).status,'idle')
  assert.equal(r.media.length,1);assert.equal(r.media[0].loop,false);assert.equal(r.nodes[0].gain.value,SOUND_GAIN)
  channel.dispose()
})
test('new scene/reset and mute cancel both queued and late sound work; no hidden replay when unmuted',async()=>{
  let resolve
  const r=rig({resources:{load:()=>new Promise(done=>{resolve=done})}}),channel=createSoundChannel(r.config)
  channel.enqueue(['CLICK']);await tick();channel.reset()
  resolve({asset:{mime:'audio/mpeg',duration:.1},data:new ArrayBuffer(4)});await tick()
  assert.equal(r.urls.size,0);assert.equal(r.media.length,0)
  const q=rig(),next=createSoundChannel(q.config);next.enqueue(['NIGHTFALL','WOLF_HOWL']);await tick()
  q.setAllowed(false);next.level(.5);assert.equal(q.urls.size,0);assert.equal(q.requests[0].signal.aborted,true)
  q.setAllowed(true);next.level(1);await tick();assert.equal(q.requests.length,1)
  channel.dispose();next.dispose()
})
test('load/media/play/init failures are explicit, clear the source and do not retry or affect BGM context',async()=>{
  for(const failure of ['load','media','play','init']){
    const r=rig()
    if(failure==='load')r.config.resources.load=async()=>{throw {code:'AUDIO_INTEGRITY_FAILED'}}
    if(failure==='play')r.config.createMedia=()=>({pause(){},removeAttribute(){},load(){},play:async()=>{throw new DOMException('denied','NotAllowedError')}})
    if(failure==='init')r.context.createMediaElementSource=()=>{throw Error('init')}
    const channel=createSoundChannel(r.config);channel.enqueue(['CLICK']);await tick()
    if(failure==='media')r.media[0].onerror()
    assert.equal(r.states.at(-1).status,'error');assert.equal(r.urls.size,0);assert.equal(r.context.state,'running')
    channel.dispose()
  }
})
test('sound timeout and queue overflow fail closed and cancel in-flight work',async()=>{
  const r=rig({timeoutMs:5,resources:{load:()=>new Promise(()=>{})}}),channel=createSoundChannel(r.config)
  channel.enqueue(['CLICK']);await new Promise(done=>setTimeout(done,15))
  assert.equal(r.states.at(-1).error,'SOUND_TIMEOUT');channel.dispose()
  const q=rig(),next=createSoundChannel(q.config);next.enqueue(['CLICK','CLICK','CLICK','CLICK'])
  assert.equal(q.states.at(-1).error,'SOUND_QUEUE_INVALID');assert.equal(q.requests.length,0);next.dispose()
})
test('BGM and sound share exactly one context, live gain changes and mute cancel effects without restarting BGM',async()=>{
  const r=rig();let contexts=0
  const player=createBgmPlayer({...r.config,createContext:()=>{contexts++;return r.context}})
  player.select('START_MENU');await player.enable();player.click();await tick()
  assert.equal(contexts,1);assert.equal(r.media.length,2);assert.equal(r.urls.size,2)
  const settings={version:1,bgmVolume:40,soundVolume:20,muted:false}
  assert.equal(player.settings(settings),true)
  assert.equal(r.nodes[0].gain.value,BGM_GAIN*.4);assert.equal(r.nodes[1].gain.value,SOUND_GAIN*.2)
  const count=r.requests.length;player.settings({...settings,muted:true})
  assert.equal(r.nodes[0].gain.value,0);assert.equal(r.urls.size,1)
  player.click();assert.equal(r.requests.length,count)
  assert.equal(player.settings({...settings,bgmVolume:200}),false)
  player.settings(settings);assert.equal(r.requests.length,count)
  player.dispose();assert.equal(r.urls.size,0);assert.equal(r.context.state,'closed')
})
test('measured BGM plus one maximum-level effect retains numerical peak headroom',()=>{
  const catalog=JSON.parse(read('../public/assets/audio/manifest.json'))
  const max=kind=>Math.max(...Object.values(catalog[kind]).map(e=>10**(e.analysis.peakDb/20)))
  assert.ok(max('bgm')*BGM_GAIN+max('sounds')*SOUND_GAIN<1)
})
