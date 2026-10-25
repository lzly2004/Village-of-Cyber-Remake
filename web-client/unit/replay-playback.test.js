import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { replayDay,replayFrame,replaySnapshot,replayRoles,replayBackground } from '../src/replayPlayback.js'
import { validateReplay } from '../src/replayValidation.js'
import { bgmScene } from '../src/bgmScene.js'
const read=p=>readFileSync(new URL(p,import.meta.url),'utf8')
const fixture=JSON.parse(read('../../src/test/resources/replay/exchange-v3.json'))[0]
const handler=read('../../src/main/java/com/vocr/ui/ReplayPlayerHandler.java')
const fresh=()=>structuredClone(fixture)

test('desktop source locks start/clamp, day+1 avatar source, strict death day, colored portraits and integer ropes',()=>{
  assert.match(handler,/if \(day < 2\) day = 2/)
  assert.match(handler,/if \(day > ui.currentReplaySave.totalDays\) day = ui.currentReplaySave.totalDays/)
  assert.match(handler,/getSnapshot\(ui, day \+ 1\)/)
  assert.match(handler,/ps.deathDay > 0 && ps.deathDay < day/)
  assert.match(handler,/ImageNames.characterRolePortrait\(\s*ps.characterNumber, ps.actualRole\)/)
  assert.match(handler,/if \(ps.comingOutDay > 0\)/)
  assert.match(handler,/int idx = day - 1/)
  assert.match(handler,/aliveCount > 1\) \? \(aliveCount - 1\) \/ 2 : 0/)
  const role=read('../../src/main/java/com/vocr/domain/Role.java')
  const names=role.slice(role.indexOf('{')+1,role.lastIndexOf('}')).replace(/\/\/[^\r\n]*/g,'').split(',').map(s=>s.trim()).filter(Boolean)
  assert.deepEqual(replayRoles,names)
  const ui=read('../../src/main/java/com/vocr/resources/UiImage.java')
  assert.equal(replayBackground,'images/'+ui.match(/REPLAY_BACKGROUND\("([^"]+)"\)/)[1])
})

test('frozen exchange-v3 desktop samples: next-day CO, death cutoff and data snapshot remain deliberately different',()=>{
  validateReplay(fixture)
  const before=JSON.stringify(fixture)
  // Independent expected table transcribed from the frozen Java sample; not produced by replayFrame.
  for(const [day,avatarDay,population,departed] of [
    [2,3,{alive:3,dead:0,ropes:1},false],
    [3,4,{alive:2,dead:1,ropes:0},true],
    [4,4,{alive:2,dead:1,ropes:0},true],
  ]) {
    const frame=replayFrame(fixture,day)
    assert.equal(frame.dataDay,day);assert.equal(frame.avatarDay,avatarDay)
    assert.deepEqual(frame.population,population)
    assert.deepEqual(frame.players.map(p=>p.portrait),['images/27s.png','images/28ws.png','images/44ks.png'])
    assert.deepEqual(frame.players.map(p=>p.playerIndex),[1,2,3])
    assert.equal(frame.players[1].departed,departed)
    assert.equal(frame.players[1].deathIcon,departed?'images/turi.png':null)
    assert.equal(frame.players[2].claimIcon,'images/yaku3_1.png') // cod=3 is shown at selected day 2 by the desktop avatar path.
    assert.deepEqual(frame.diagnostics,[])
  }
  assert.equal(JSON.stringify(fixture),before)
})

test('date boundaries include single-day recordings and cannot escape the file range',()=>{
  assert.equal(replayDay(fixture),2);assert.equal(replayDay(fixture,1),2)
  assert.equal(replayDay(fixture,999),4);assert.equal(replayDay(fixture,NaN),2)
  const one=fresh();one.totalDays=1;one.daySnapshots=one.daySnapshots.slice(0,1)
  const frame=replayFrame(one)
  assert.equal(frame.day,1);assert.equal(frame.firstDay,1);assert.equal(frame.lastDay,1)
})

test('missing exact date retains desktop positional fallback with explicit diagnosis; missing data is never invented',()=>{
  const r=fresh();r.daySnapshots.splice(1,1)
  assert.equal(replaySnapshot(r,2).snapshot.dayNumber,3)
  const frame=replayFrame(r,2)
  assert.equal(frame.dataDay,3)
  assert.ok(frame.diagnostics.some(d=>d.section==='data'&&d.code==='SNAPSHOT_INDEX_FALLBACK'&&d.sourceDay===3))
  r.daySnapshots=r.daySnapshots.slice(0,1)
  const absent=replayFrame(r,3)
  assert.equal(absent.population,null);assert.deepEqual(absent.players,[])
  assert.ok(absent.diagnostics.some(d=>d.code==='SNAPSHOT_MISSING'))
})

test('44 appearances and 11 role suffixes use existing assets; death never grays a Replay role portrait',()=>{
  const images=JSON.parse(read('../public/assets/images/manifest.json')).images
  for(let n=1;n<=44;n++)for(let role=1;role<=11;role++){
    const r=fresh();r.daySnapshots=r.daySnapshots.map(s=>({...s,players:[{...s.players[0],characterNumber:n,actualRole:role,deathDay:1,deathReason:4}]}))
    const p=replayFrame(r,3).players[0]
    const suffix=({5:'cs',7:'ws',8:'ks',9:'ks',10:'fs',11:'hs'})[role]||'s'
    assert.equal(p.portrait,`images/${String(n).padStart(2,'0')}${suffix}.png`)
    assert.ok(images[p.portrait]);assert.equal(p.deathIcon,'images/kami.png')
  }
})

test('unavailable CO order yields a diagnostic without changing actual order or fabricating an icon',()=>{
  const r=fresh();r.daySnapshots[2].players[0].claimedRoleOrder=6
  const p=replayFrame(r,2).players[0]
  assert.equal(p.claimIcon,null);assert.equal(p.claimDiagnostic,'CO_ICON_UNAVAILABLE');assert.equal(p.claimOrder,'CO順 6')
})

test('Replay audio is an explicit local scene; returning selects the current live scene without creating a context',()=>{
  const props={sessionId:'live',update:{phase:'DAY_DECISION',view:{gameDay:2,aliveCount:8}},uncertain:false}
  assert.equal(bgmScene(props).id,'EMOTIONALLY_UNSTABLE')
  assert.deepEqual(bgmScene({...props,replaying:true,uncertain:true}),{key:'replay',id:'REPLAY_PLAYER'})
  assert.equal(bgmScene({...props,replaying:false}).id,'EMOTIONALLY_UNSTABLE')
})
