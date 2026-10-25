import test from 'node:test'
import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { readFileSync } from 'node:fs'
import { createAudioResources, resolveAudio, resolveAudioDescription } from '../src/audioAssets.js'
const raw = new Uint8Array([1,2,3,4])
const text = new TextEncoder().encode('村08\n')
const sha = bytes=>createHash('sha256').update(bytes).digest('hex')
function entry(kind='bgm') { return { file:kind+'/'+sha(raw)+'.mp3',sha256:sha(raw),bytes:raw.length,mime:'audio/mpeg',
  channels:2,sampleRate:44100,samples:44100,duration:1,bitrate:kind==='bgm'?128000:192000,
  description:kind==='bgm'?{file:'descriptions/'+sha(text)+'.txt',sha256:sha(text),bytes:text.length}:undefined } }
function fixture() { return {schemaVersion:1,policy:{version:1,format:'mp3'},bgm:{FIRST:entry(),SECOND:{...entry(),description:null}},
  sounds:{CLICK:entry('sounds')},playlists:{LATE_GAME_PLAYLIST:['SECOND'],NORMAL_GAME_PLAYLIST:['FIRST']}} }
function response(bytes=raw) { return {ok:true,arrayBuffer:async()=>bytes.slice().buffer} }

test('manifest is lazy and shared; only specifically requested chunk/description fetched, no playback',async()=>{
  const calls=[];const catalog=fixture()
  const resources=createAudioResources(async(url)=>{
    calls.push(url)
    return url.endsWith('manifest.json')?{ok:true,json:async()=>catalog}:response(url.endsWith('.txt')?text:raw)
  })
  assert.deepEqual(calls,[])
  await Promise.all([resources.manifest(),resources.manifest()])
  assert.equal(calls.length,1)
  const result=await resources.load('sounds','CLICK')
  assert.deepEqual(new Uint8Array(result.data),raw)
  assert.equal(calls.length,2);assert.ok(calls[1].startsWith('/assets/audio/sounds/'))
  assert.equal(await resources.description('FIRST'),'村08\n')
  assert.equal(calls.length,3);assert.ok(calls[2].includes('/descriptions/'))
  assert.equal(await resources.description('SECOND'),null);assert.equal(calls.length,3)
})
test('resolver accepts content-addressed same-origin filenames only and rejects malformed metadata/prototype keys',()=>{
  assert.ok(resolveAudio(fixture(),'bgm','FIRST'))
  for(const [kind,id]of [['toString','FIRST'],['bgm','__proto__'],['bgm','toString'],['sounds','UNKNOWN'],['bgm',null]])assert.equal(resolveAudio(fixture(),kind,id),null)
  for(const patch of [{file:'https://evil/audio.mp3'},{file:'../audio.mp3'},{sha256:'A'.repeat(64)},
    {bytes:-1},{bytes:6*1024**2},{channels:1},{sampleRate:0},{samples:10},{duration:NaN},{bitrate:320000},{mime:'text/html'}]){
    const data=fixture();Object.assign(data.bgm.FIRST,patch)
    assert.equal(resolveAudio(data,'bgm','FIRST'),null)
  }
  const data=fixture();data.bgm.FIRST.description.file='../not-text.txt'
  assert.equal(resolveAudioDescription(data,'FIRST'),null)
})
test('bad manifest and playlist fail once without retry loops or original-WAV fallback',async()=>{
  for(const change of [d=>d.schemaVersion=2,d=>d.playlists.NORMAL_GAME_PLAYLIST=['UNKNOWN'],d=>d.sounds=[],
    d=>d.bgm.FIRST.file='//evil/x.mp3',d=>d.bgm.FIRST.description={file:'bad'}]){
    const data=fixture();change(data);let count=0
    const resources=createAudioResources(async()=>{count++;return{ok:true,json:async()=>data}})
    await assert.rejects(resources.load('bgm','FIRST'),{code:'MANIFEST_INVALID'})
    await assert.rejects(resources.manifest(),{code:'MANIFEST_INVALID'})
    assert.equal(count,1)
  }
  const resources=createAudioResources(async()=>({ok:false}))
  await assert.rejects(resources.manifest(),{code:'MANIFEST_LOAD_FAILED'})
})
test('missing IDs never request a chunk; false HTTP200 SPA, truncation and changed bytes fail integrity',async()=>{
  for(const bytes of [new TextEncoder().encode('<html>'),new Uint8Array([1]),new Uint8Array([4,3,2,1])]){
    const calls=[];const resources=createAudioResources(async url=>{
      calls.push(url);return url.endsWith('manifest.json')?{ok:true,json:async()=>fixture()}:response(bytes)
    })
    await assert.rejects(resources.load('sounds','UNKNOWN'),{code:'UNKNOWN_AUDIO'})
    assert.equal(calls.length,1)
    await assert.rejects(resources.load('sounds','CLICK'),{code:'AUDIO_INTEGRITY_FAILED'})
    assert.equal(calls.length,2)
  }
})
test('caller abort is forwarded, never cached, and cannot cancel shared manifest for other callers',async()=>{
  const controller=new AbortController();let seen
  const resources=createAudioResources(async(url,options)=>{
    if(url.endsWith('manifest.json'))return{ok:true,json:async()=>fixture()}
    seen=options.signal;controller.abort();return response()
  })
  await assert.rejects(resources.load('bgm','FIRST',{signal:controller.signal}),{name:'AbortError'})
  assert.equal(seen,controller.signal)
  assert.ok(await resources.manifest())
  let calls=0;const before=createAudioResources(async()=>{calls++;throw Error('must not fetch')})
  await assert.rejects(before.load('bgm','FIRST',{signal:controller.signal}),{name:'AbortError'})
  assert.equal(calls,0)
})
test('actual manifest semantic IDs/playlists/descriptions match source enums, not a duplicated title table',()=>{
  const read=path=>readFileSync(new URL('../../'+path,import.meta.url),'utf8')
  const catalog=JSON.parse(read('web-client/public/assets/audio/manifest.json'))
  for(const [kind,file]of [['bgm','BgmTrack'],['sounds','SoundEffect']]){
    const source=read('src/main/java/com/vocr/resources/'+file+'.java')
    const entries=[...source.matchAll(/\b([A-Z][A-Z_0-9]*)\(\s*"([^"\n]+\.wav)"\s*(?:,\s*"([^"\n]+\.txt)"\s*)?\)/g)]
    assert.deepEqual(Object.keys(catalog[kind]).sort(),entries.map(m=>m[1]).sort())
    for(const [,id,filename,description]of entries){
      assert.equal(catalog[kind][id].source,(kind==='bgm'?'BGM':'sounds')+'/'+filename)
      assert.ok(resolveAudio(catalog,kind,id))
      if(description){assert.equal(catalog[kind][id].description.source,'help/'+description);assert.ok(resolveAudioDescription(catalog,id))}
      else assert.equal(catalog[kind][id].description,null)
    }
    if(kind==='bgm')for(const key of ['LATE_GAME_PLAYLIST','NORMAL_GAME_PLAYLIST']){
      assert.deepEqual(catalog.playlists[key],source.match(new RegExp(key+'\\s*=\\s*List\\.of\\(([^)]+)\\)'))[1].split(',').map(s=>s.trim()))
    }
  }
})
test('cancel while shared manifest is pending rejects that caller promptly and preserves remaining consumer',async()=>{
  let release,calls=0
  const blocked=new Promise(resolve=>{release=resolve})
  const resources=createAudioResources(async(url)=>{
    calls++;if(url.endsWith('manifest.json')){await blocked;return{ok:true,json:async()=>fixture()}}
    return response()
  })
  const controller=new AbortController()
  const one=resources.load('bgm','FIRST',{signal:controller.signal})
  const two=resources.load('sounds','CLICK')
  controller.abort();await assert.rejects(one,{name:'AbortError'})
  assert.equal(calls,1);release()
  assert.equal((await two).data.byteLength,4);assert.equal(calls,2)
})
test('stalled manifest times out once and aborts transport, with no hidden retry or dangling timer',async()=>{
  let calls=0,signal
  const resources=createAudioResources(async(_,options)=>{calls++;signal=options.signal;return new Promise(()=>{})},undefined,10)
  await assert.rejects(resources.manifest(),{code:'MANIFEST_TIMEOUT'})
  assert.equal(signal.aborted,true)
  await assert.rejects(resources.load('sounds','CLICK'),{code:'MANIFEST_TIMEOUT'})
  assert.equal(calls,1)
})
