import { nightDayPlan, nightDayImages, nightDayTimes, dayStartFormat } from '../src/nightDayPresentation.js'
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { sceneArtwork, sceneImages, outcomes, consequenceFormats, transitionTimes } from '../src/sceneArtwork.js'
import { dialogueArtwork, eventImageNames } from '../src/eventArtwork.js'
import { settlementReveal, settlementPortrait, settlementDetails } from '../src/settlementVisual.js'
import { characters } from '../src/characterDisplay.js'
import { roles } from '../src/labels.js'
const read = path => readFileSync(new URL('../../' + path, import.meta.url),'utf8')
const manifest = JSON.parse(read('web-client/public/assets/images/manifest.json'))
const event = (eventname, ch1 = 'Abel') => ({ eventname, ch1, ch2: 'NONE', ch3: 'NONE' })
const p = (eventname, stage) => ({ event: event(eventname), stage })
const scene = (eventname, stage, result = 'VILLAGE_WIN', source) => sceneArtwork([], 0, p(eventname, stage), result, false, source)

test('all scene backgrounds/icons, including024 day/afternoon, are locked to UiImage; outcomes to GameStrings', () => {
  const images = read('src/main/java/com/vocr/resources/UiImage.java')
  const value = key => 'images/' + images.match(new RegExp(key + '\\("([^"]+)"\\)'))[1]
  assert.equal(sceneImages.day, value('DIALOGUE_DAY_BACKGROUND'))
  assert.equal(sceneImages.afternoon, value('DIALOGUE_AFTERNOON_BACKGROUND'))
  assert.equal(sceneImages.review, value('REVIEW_BOARD'))
  for (const [stage, key] of [['DAY_DIALOGUE','DIALOGUE_DAY_BACKGROUND'],['AFTERNOON_DIALOGUE','DIALOGUE_AFTERNOON_BACKGROUND']]) {
    const current=p('zco7',stage)
    assert.equal(dialogueArtwork([current],0,current).background,value(key))
  }
  const strings = read('src/main/java/com/vocr/ui/GameStrings.java')
  for (const [result, name, text] of [['VILLAGE_WIN','VILLAGE','VILLAGER'],['WOLF_WIN','WOLF','WOLF'],['FOX_WIN','FOX','FOX']]) {
    assert.equal(outcomes[result].background,value('END_'+name))
    assert.equal(outcomes[result].icon,value('END_'+name+'_ICON'))
    assert.equal(outcomes[result].text,strings.match(new RegExp('WIN_'+text+'\\s*=\\s*"([^"]+)"'))[1])
  }
})
test('death/execution ordered arrays remain one actor per frame, 44x4 complete sources; wsw no person', () => {
  for (const character of Object.keys(characters)) for (const [key,stage] of [['yjsw','NIGHT_RESULT'],['cxs','EXECUTION'],['hzsw','EXECUTION'],['mzsw','EXECUTION']]) {
    const current={event:event(key,character),stage}
    const result=sceneArtwork([],0,current,'NONE')
    assert.equal(result.diagnostic,null)
    assert.deepEqual(result.frames.map(frame=>frame.map(actor=>actor.name)),eventImageNames(current.event).names.map(name=>[name]))
    assert.equal(result.actors.length,1)
    assert.ok(result.frames.flat().every(actor=>actor.x===null&&Object.hasOwn(manifest.images,actor.name)))
  }
  assert.deepEqual(scene('wsw','NIGHT_RESULT').actors,[])
  const constants=read('src/main/java/com/vocr/ui/UIConstants.java')
  assert.equal(transitionTimes.NIGHT_RESULT,Number(constants.match(/TRANSITION_SHORT_MS = (\d+)/)[1]))
  assert.equal(transitionTimes.EXECUTION,Number(constants.match(/TRANSITION_MEDIUM_MS = (\d+)/)[1]))
  const strings=read('src/main/java/com/vocr/ui/GameStrings.java')
  for(const [key,constant]of [['cxs','EXECUTED_FORMAT'],['hzsw','FOLLOW_DEATH_FORMAT'],['mzsw','CAT_CURSE_FORMAT']]){
    assert.equal(consequenceFormats[key],strings.match(new RegExp(constant+'\\s*=\\s*"([^"]+)"'))[1])
    assert.equal(scene(key,'EXECUTION').consequence,consequenceFormats[key].replace('%s','青年 アーベル'))
  }
})
test('settlement event images use public outcome; review board no people until caller reveal gate', () => {
  for(const result of Object.keys(outcomes))for(const key of ['crsl','rlsl','krsl','yhsl']){
    const resultPlan=scene(key,'SETTLEMENT',result)
    assert.equal(resultPlan.background,outcomes[result].background)
    assert.deepEqual(resultPlan.actors.map(a=>a.name),eventImageNames(event(key)).names)
  }
  const final=sceneArtwork([],0,null,'WOLF_WIN',true)
  assert.equal(final.background,sceneImages.review);assert.deepEqual(final.actors,[])
  assert.equal(sceneArtwork([],0,null,'NONE',true).diagnostic,'UNKNOWN_OUTCOME')
})
test('wrong phases/missing arrays/unknown outcomes and special empty are explicit, never silent or inferred', () => {
  for(const [key,stage]of [['wsw','SETTLEMENT'],['cxs','NIGHT_RESULT'],['yjsw','EXECUTION'],['NONE','SETTLEMENT']])assert.equal(scene(key,stage).diagnostic,'STAGE_EVENT_MISMATCH')
  assert.equal(scene('crsl','SETTLEMENT','toString').diagnostic,'UNKNOWN_OUTCOME')
  for(const entry of [{default:'one.png'},{default:[]},{default:null}])assert.ok(scene('yjsw','NIGHT_RESULT','NONE',{Abel:{yjsw:entry}}).diagnostic)
  const current=p('yjsw','NIGHT_RESULT');current.event.ch2='Betti'
  assert.equal(sceneArtwork([],0,current,'NONE',false,{Abel:{yjsw:{default:['a.png','b.png'],special:{Betti:null}}}}).diagnostic,'EMPTY_IMAGE')
  for(const field of ['actualRole','skillTarget','revealedRoles','deathReason','ch3'])Object.defineProperty(current.event,field,{get(){throw Error('private read')}})
  assert.equal(sceneArtwork([],0,current,'NONE').frames.length,2)
})
test('reveal data is untouched before finished; malformed identities fail closed, valid mapping uses numbers not order', () => {
  const poison=new Proxy([],{get(){throw Error('premature reveal')}})
  assert.equal(settlementReveal(false,poison,poison).diagnostic,null)
  const players=[{playerNumber:2},{playerNumber:5}]
  const entries=[{playerNumber:5,actualRole:'mao'},{playerNumber:2,actualRole:'renlang'}]
  const result=settlementReveal(true,players,entries)
  assert.equal(result.roles.get(2),'renlang');assert.equal(result.diagnostic,null)
  for(const invalid of [null,{},[],entries.slice(1),[entries[0],entries[0]],[entries[0],{playerNumber:2,actualRole:'NONE'}],
    [entries[0],{playerNumber:2,actualRole:'toString'}],[entries[0],{playerNumber:8,actualRole:'wu'}],[null,entries[0]]]){
    const result=settlementReveal(true,players,invalid)
    assert.ok(result.diagnostic);assert.equal(result.roles.size,0)
  }
})
test('44x11 alive/dead result portraits match ImageNames role switch and hit existing manifest, no private player reads', () => {
  const java=read('src/main/java/com/vocr/resources/ImageNames.java').split('private static String rolePortraitSuffix')[1]
  const suffixes=new Map()
  for(const match of java.matchAll(/case ([\d, ]+) -> "(\w+)"/g))for(const number of match[1].split(','))suffixes.set(Number(number),match[2])
  const roleNames=read('src/main/java/com/vocr/domain/Role.java').replace(/\/\/[^\r\n]*/g,'').match(/\{([\s\S]*)\}/)[1].split(',').map(s=>s.trim())
  assert.deepEqual(roleNames,Object.keys(roles))
  const numbers=Object.fromEntries(roleNames.map((name,index)=>[name,index]))
  for(const [character,details]of Object.entries(characters))for(const role of Object.keys(roles).filter(r=>r!=='NONE'))for(const alive of [true,false]){
    const player={character,alive}
    for(const field of ['actualRole','skillTarget','nonHumanMarked'])Object.defineProperty(player,field,{get(){throw Error('private player field')}})
    const expected='images/'+String(details.number).padStart(2,'0')+(alive?(suffixes.get(numbers[role])||'s'):'gs')+'.png'
    assert.equal(settlementPortrait(player,role),expected)
    assert.ok(Object.hasOwn(manifest.images,expected))
  }
  assert.equal(settlementPortrait({character:'NONE',alive:true},'wu'),null)
  assert.equal(settlementPortrait({character:'Abel',alive:true},'toString'),null)
})


// Public local-night presentation contracts share the existing scene test entry.
{
const nightEvent = (stage,eventname) => ({stage,event:{eventname}})
test('night/day desktop images, timing and day text are source locked',()=>{
 const image=readFileSync(new URL('../../src/main/java/com/vocr/resources/UiImage.java',import.meta.url),'utf8')
 const constants=readFileSync(new URL('../../src/main/java/com/vocr/ui/UIConstants.java',import.meta.url),'utf8')
 const strings=readFileSync(new URL('../../src/main/java/com/vocr/ui/GameStrings.java',import.meta.url),'utf8')
 for(const [key,name] of [['night','NIGHT_BACKGROUND'],['dawn','GAME_SETUP_DIALOGUE_BACKGROUND']])assert.equal(nightDayImages[key],'images/'+image.match(new RegExp(name+'\\("([^"]+)"\\)'))[1])
 for(const [key,name] of [['night','NIGHT_SCREEN_DURATION_MS'],['dawn','HOWL_TRANSITION_MS'],['character','TYPEWRITER_DELAY_MS']])assert.equal(nightDayTimes[key],Number(constants.match(new RegExp(name+'\\s*=\\s*(\\d+)'))[1]))
 assert.equal(dayStartFormat,strings.match(/DAY_START_FORMAT\s*=\s*"([^"]+)"/)[1])
})
test('one introduction per public contiguous night group, including initial night',()=>{
 const history=[nightEvent('NIGHT_RESULT','wsw'),nightEvent('DAY_DIALOGUE','gyfo1'),nightEvent('EXECUTION','cxs'),nightEvent('NIGHT_RESULT','yjsw'),nightEvent('NIGHT_RESULT','yjsw')]
 assert.deepEqual(nightDayPlan(history,0,history[0],1),{key:0,nightOnly:false,dayText:'1日目になりました。'})
 assert.equal(nightDayPlan(history,1,history[1],1),null)
 assert.deepEqual(nightDayPlan(history,3,history[3],2),{key:3,nightOnly:false,dayText:'2日目になりました。'})
 assert.equal(nightDayPlan(history,4,history[4],2),null)
})
test('public settlement after execution has night only; no future outcome or private getter read',()=>{
 const history=[nightEvent('EXECUTION','cxs'),nightEvent('SETTLEMENT','crsl')]
 Object.defineProperty(history[1],'result',{get(){throw Error('result read')}})
 Object.defineProperty(history[1],'actualRole',{get(){throw Error('private read')}})
 assert.deepEqual(nightDayPlan(history,1,history[1],2),{key:1,nightOnly:true,dayText:''})
 assert.equal(nightDayPlan([history[1]],0,history[1],2),null)
 const followed=[nightEvent('NIGHT_RESULT','yjsw'),history[1]]
 assert.equal(nightDayPlan(followed,1,followed[1],2),null)
})
test('missing or mismatched history is diagnosed, never guessed as another night',()=>{
 const current=nightEvent('NIGHT_RESULT','yjsw')
 assert.equal(nightDayPlan([],4,current,2).diagnostic,'NIGHT_CONTEXT_UNAVAILABLE')
 const gap=[];gap[4]=current;assert.equal(nightDayPlan(gap,4,current,2).diagnostic,'NIGHT_CONTEXT_UNAVAILABLE')
 assert.equal(nightDayPlan([nightEvent('DAY_DIALOGUE','gyfo1')],0,current,1).diagnostic,'NIGHT_CONTEXT_UNAVAILABLE')
 assert.equal(nightDayPlan([current],0,current,0).diagnostic,'DAY_NUMBER_UNAVAILABLE')
 assert.equal(nightDayPlan([nightEvent('NIGHT_RESULT','invalid')],0,nightEvent('NIGHT_RESULT','invalid'),1),null)
})

test('local presentation mutes BGM without consulting an already-public future outcome',async()=>{
 const {bgmScene}=await import('../src/bgmScene.js')
 const view={};Object.defineProperty(view,'result',{get(){throw Error('early outcome read')}})
 for(const presentationPhase of ['night','dawn','day-text','error']){
  assert.equal(bgmScene({sessionId:'test',update:{view},currentEvent:nightEvent('SETTLEMENT','crsl'),presentationPhase}).id,null)
 }
})
}

test('desktop settlement details preserve claims, exact death days and all seven core reasons', () => {
  const suffixes={beiyao:'狼噛',chuxing:'処刑',zhousha:'呪殺',dayhouzhui:'後追',nighthouzhui:'後追',daymaozhou:'猫呪',nightmaozhou:'猫呪'}
  for(const [deathReason,suffix] of Object.entries(suffixes)) {
    const value=settlementDetails(true,{alive:false,deathReason,deathDay:4,claimedRole:'zhan'},'renlang','WOLF_WIN')
    assert.equal(value.claim,'占い師');assert.equal(value.actual,'人狼');assert.equal(value.fate,'4日目'+suffix);assert.equal(value.diagnostic,null)
  }
})
test('desktop alive outcome matches EndAnimeHandler factions rather than fabricating death dates',()=>{
  for(const role of Object.keys(roles).filter(r=>r!=='NONE'))for(const result of ['VILLAGE_WIN','WOLF_WIN','FOX_WIN']) {
    const value=settlementDetails(true,{alive:true,deathReason:'NONE',deathDay:0,claimedRole:'NONE'},role,result)
    const winner=result==='WOLF_WIN'?['renlang','kuangren','kuangxinzhe'].includes(role):['yaohu','beidezhe'].includes(role)
    assert.equal(value.fate,result==='VILLAGE_WIN'?'最終存活':winner?'最終胜利':'最終死亡')
    assert.equal(value.claim,'無し');assert.equal(value.actual,roles[role]);assert.equal(value.diagnostic,null)
  }
})
test('settlement details do not read fields before reveal and diagnose missing or sanitized terminal facts',()=>{
  const poison=new Proxy({},{get(){throw Error('premature read')}})
  assert.equal(settlementDetails(false,poison,poison,poison),null)
  for(const deathReason of ['NIGHT_DEATH','EXECUTION','toString',null]){
    const v=settlementDetails(true,{alive:false,deathDay:2,deathReason,claimedRole:'NONE'},'wu','VILLAGE_WIN')
    assert.equal(v.fate,'死因不明（表示診断）');assert.equal(v.diagnostic,'INVALID_DEATH_REASON')
  }
  for(const day of [null,0,-1,2.5,'3']){
    const v=settlementDetails(true,{alive:false,deathDay:day,deathReason:'beiyao',claimedRole:'NONE'},'wu','VILLAGE_WIN')
    assert.equal(v.fate,'死亡日不明（表示診断）');assert.equal(v.diagnostic,'INVALID_DEATH_DAY')
  }
  assert.match(settlementDetails(true,{alive:true,claimedRole:'toString'},'bad','bad').actual,/不明/)
})

test('settlement fate wording follows GameStrings, normalizing only invisible trailing whitespace',()=>{
 const java=read('src/main/java/com/vocr/ui/GameStrings.java')
 const constant=key=>JSON.parse(java.match(new RegExp('String\\s+'+key+'\\s*=\\s*("(?:\\\\.|[^"\\\\])*")'))[1])
 for(const [reason,key]of [['beiyao','DEATH_BITE_FORMAT'],['chuxing','DEATH_EXECUTE_FORMAT'],['zhousha','DEATH_CURSE_FORMAT'],['dayhouzhui','DEATH_FOLLOW_FORMAT'],['nighthouzhui','DEATH_FOLLOW_FORMAT'],['daymaozhou','DEATH_CAT_FORMAT'],['nightmaozhou','DEATH_CAT_FORMAT']]){
  assert.equal(settlementDetails(true,{alive:false,deathReason:reason,deathDay:4,claimedRole:'NONE'},'wu','VILLAGE_WIN').fate,constant(key).replace('%d','4').trimEnd())
 }
 for(const [result,role,key]of [['VILLAGE_WIN','wu','END_SURVIVE'],['WOLF_WIN','renlang','END_WIN'],['WOLF_WIN','wu','END_DEAD'],['FOX_WIN','yaohu','END_WIN'],['FOX_WIN','zhan','END_DEAD']])assert.equal(settlementDetails(true,{alive:true,claimedRole:'NONE'},role,result).fate,constant(key))
})
