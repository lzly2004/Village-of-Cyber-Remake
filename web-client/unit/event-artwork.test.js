import test from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import catalog from '../../src/main/resources/config/event_image_mapping.json' with {type:'json'}
import {eventImageNames,dialogueArtwork,mergePresentationHistory,leftEvents,connectEvents} from '../src/eventArtwork.js'
import {createStageImageCache} from '../src/stageImageCache.js'
const read=path=>readFileSync(new URL('../../'+path,import.meta.url),'utf8')
const manifest=JSON.parse(read('web-client/public/assets/images/manifest.json'))
const event=(eventname='gyfo1',ch1='Abel',ch2='Betti')=>({eventname,ch1,ch2,ch3:'Helmut'})
const p=(e,stage='DAY_DIALOGUE')=>({event:e,stage})
const plan=(list,index=list.length-1,source=catalog)=>dialogueArtwork(list,index,list[index],source)

test('all1848 default/744 special selections follow raw JSON, including ordered two-image transitions reserved for025',()=>{
 let defaults=0,specials=0,arrays=0
 for(const [character,events] of Object.entries(catalog))for(const [key,entry]of Object.entries(events)){
  if(key==='default')continue
  for(const [target,value]of [[null,entry.default],...Object.entries(entry.special||{})]){
   const result=eventImageNames(event(key,character,target))
   const expected=(Array.isArray(value)?value:[value]).map(n=>'images/'+n)
   assert.deepEqual(result,{names:expected,diagnostic:null})
   for(const name of expected)assert.ok(Object.hasOwn(manifest.images,name),name)
   target===null?defaults++:specials++
   if(Array.isArray(value))arrays++
  }
 }
 assert.equal(defaults,1848);assert.equal(specials,744);assert.equal(arrays,176)
})
test('special exact presence wins even null/empty; ch3 never selects image; malformed values get labeled frame',()=>{
 for(const value of [null,[],undefined,'']){
  const source={Abel:{gyfo1:{default:'good.png',special:{Betti:value}}}}
  assert.equal(eventImageNames(event(),source).diagnostic!==null,true)
  assert.deepEqual(eventImageNames(event(),source).names,['images/frame.png'])
 }
 const e=event();assert.deepEqual(eventImageNames({...e,ch3:'Erich'}),eventImageNames(e))
 for(const value of ['../bad.png','x/y.png','x\\y.png',['a.png',null],5,{}]){
  assert.ok(eventImageNames(e,{Abel:{gyfo1:{default:value}}}).diagnostic)
 }
})
test('system/sentinels/prototype keys and absent configs never fabricate a normal character',()=>{
 assert.deepEqual(eventImageNames(event('wsw',null)),{names:[],diagnostic:null})
 for(const e of [null,{},event('NONE'),event('default'),event('toString'),event('gyfo1','toString'),
  event('gyfo1','NONE'),event('gyfo1','Abel','__proto__'),event('gyfo1','Abel',{})])assert.ok(eventImageNames(e).diagnostic)
 assert.equal(eventImageNames(event(),{}).diagnostic,'MISSING_CHARACTER')
 assert.equal(eventImageNames(event(),{Abel:{}}).diagnostic,'MISSING_EVENT')
 assert.equal(eventImageNames(event(),{Abel:{gyfo1:null}}).diagnostic,'INVALID_ENTRY')
})
test('layout event sets and 1280/720 bottom30 left300 right650 follow Java call sites',()=>{
 const helper=read('src/main/java/com/vocr/ui/UIHelpers.java')
 assert.deepEqual(leftEvents,helper.match(/case ([^:]+) -> true/)[1].split(',').map(s=>s.trim()))
 const handler=read('src/main/java/com/vocr/ui/DialogueDayHandler.java')
 const connect=[...handler.slice(handler.indexOf('switch (event.eventname())'),handler.indexOf('DebugLogger.log(')).matchAll(/case (\w+):/g)].map(m=>m[1])
 assert.deepEqual(connectEvents,connect)
 assert.match(helper,/\? 300 :/);assert.match(helper,/Simple_Label, 650,/)
 const constants=read('src/main/java/com/vocr/ui/UIConstants.java')
 assert.match(constants,/WINDOW_WIDTH = 1280/);assert.match(constants,/WINDOW_HEIGHT = 720/)
 assert.match(constants,/CHAR_ICON_BOTTOM_MARGIN = 30/)
})
test('single center, six left initiators, normal left-cache/right-current pairing; not ch2 guessed portraits',()=>{
 assert.equal(plan([p(event('zco7'))]).actors[0].x,null)
 for(const key of leftEvents)assert.equal(plan([p(event(key))]).actors[0].x,300)
 const list=[p(event('gyfo1')),p(event('gyfo1r','Betti','Abel'))]
 const result=plan(list)
 assert.deepEqual(result.actors,[{name:'images/01w3.png',x:300,layer:1},{name:eventImageNames(list[1].event).names[0],x:650,layer:0}])
 const interrupted=[list[0],p(event('zco7')),list[1]]
 assert.equal(plan(interrupted).actors.length,1)
})
test('gprz11r lookahead only controls triple presentation swap; no future image or fourth actor',()=>{
 const list=[p(event('zjgh8b')),p(event('gprz11r','Betti','Abel')),p(event('gprz11p','Erich','Betti'))]
 const middle=plan(list,1)
 assert.equal(middle.actors.length,2)
 assert.equal(middle.actors.find(a=>a.x===650).name,eventImageNames(list[1].event).names[0])
 const end=plan(list)
 assert.equal(end.actors.find(a=>a.x===300).name,eventImageNames(list[2].event).names[0])
 assert.equal(end.actors.find(a=>a.x===650).name,eventImageNames(list[1].event).names[0])
 assert.equal(plan([...list,p(event('zco7'))]).actors.length,1)
})
test('afternoon uses background haikei and preserves cached left across adjacent afternoon events',()=>{
 const list=[p(event('gyfo1'),'AFTERNOON_DIALOGUE'),p(event('zco7','Erich'),'AFTERNOON_DIALOGUE')]
 const result=plan(list);assert.equal(result.background,'images/haikei.png');assert.equal(result.actors.length,2)
 const crossed=[p(event('gyfo1')),list[1]]
 assert.equal(plan(crossed).actors.length,1)
})
test('025 stages are deferred; exact prefixes reconstruct pairs without changing the current event',()=>{
 for(const stage of ['NIGHT_RESULT','EXECUTION','SETTLEMENT',undefined]){
  const current=p(event('yjsw'),stage)
  if(stage===undefined)current.stage=undefined
  assert.equal(dialogueArtwork([current],0,current).mode,'deferred')
 }
 const list=[p(event()),p(event('gyfo1r','Betti','Abel'))]
 const copy=structuredClone(list)
 assert.deepEqual(plan(copy),plan(list))
 assert.deepEqual(list,copy)
 assert.equal(dialogueArtwork([],1,list[1]).diagnostic,'CONTEXT_UNAVAILABLE')
 assert.equal(dialogueArtwork([],1,list[1]).actors.length,1)
 const wrongStage=plan([p(event('yjsw'))])
 assert.equal(wrongStage.diagnostic,'UNSUPPORTED_DIALOGUE_ARRAY')
 assert.deepEqual(wrongStage.actors,[{name:'images/frame.png',x:null,layer:0}])
})
test('presentation windows merge by absolute cursor, reject gaps and wrong lengths; no private fields read',()=>{
 const list=[p(event()),p(event('gyfo1r'))]
 assert.deepEqual(mergePresentationHistory([],{eventCursor:0,nextEventCursor:2,presentedEvents:list}),list)
 assert.deepEqual(mergePresentationHistory(list,{eventCursor:1,nextEventCursor:2,presentedEvents:list.slice(1)}),list)
 assert.equal(mergePresentationHistory([],{eventCursor:1,nextEventCursor:2,presentedEvents:list.slice(1)}),null)
 assert.equal(mergePresentationHistory([],{eventCursor:0,nextEventCursor:3,presentedEvents:list}),null)
 for(const field of ['actualRole','skillTarget','revealedRoles','ch3'])Object.defineProperty(list[1].event,field,{get(){throw Error('private')}})
 assert.equal(plan(list).actors.length,2)
})
test('decoded cache coalesces reads, is synchronous on warm remount and retains at most16 entries',async()=>{
 let reads=0,decodes=0
 const cache=createStageImageCache(async()=>{reads++;return manifest},async()=>{decodes++;return {}},16)
 const names=['images/haikei3.png','images/01d.png']
 assert.equal(cache.ready(names),null)
 const [a,b]=await Promise.all([cache.load(names),cache.load(names)])
 assert.deepEqual(a,b);assert.equal(reads,2);assert.equal(decodes,2)
 assert.ok(cache.ready(names))
 for(let i=1;i<=44;i++)await cache.load([`images/${String(i).padStart(2,'0')}s.png`])
 assert.equal(cache.size(),16);assert.equal(cache.ready(names),null)
 const fail=createStageImageCache(async()=>manifest,async()=>{throw Error('decode')})
 await assert.rejects(fail.load(['images/01d.png']));assert.equal(fail.ready(['images/01d.png']),null)
 await assert.rejects(cache.load(['images/missing.png']))
})
