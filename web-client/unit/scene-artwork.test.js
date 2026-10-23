import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { sceneArtwork, sceneImages, outcomes, consequenceFormats, transitionTimes } from '../src/sceneArtwork.js'
import { dialogueArtwork, eventImageNames } from '../src/eventArtwork.js'
import { settlementReveal, settlementPortrait } from '../src/settlementVisual.js'
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
