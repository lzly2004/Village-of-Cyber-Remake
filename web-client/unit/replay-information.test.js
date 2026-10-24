import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { replayInformation, replayVotes, visibleReplaySkill } from '../src/replayInformation.js'
import { validateReplay } from '../src/replayValidation.js'
const read=p=>readFileSync(new URL(p,import.meta.url),'utf8')
const cases=JSON.parse(read('../../src/test/resources/replay/web034-display-cases.json'))
const fixture=cases[0].replay
const desktopRoleNames=['','占卜师','灵媒师','猎人','共有者','猫又','村人','人狼','狂人','狂信者','妖狐','背德者']
for(const c of cases)test('shared Java formatter case: '+c.name,()=>{
  validateReplay(c.replay)
  const before=JSON.stringify(c.replay),info=replayInformation(c.replay,c.day)
  assert.equal(info.normalText,c.normalText)
  const final=replayInformation(c.replay,c.replay.totalDays)
  assert.equal('一役職一\n'+final.groups.map(g=>'['+desktopRoleNames[g.role]+']\n'+g.players.map(p=>p.name).join('　')+'\n').join(''),c.roleText)
  if(c.vote){
    const v=replayVotes(info.current,c.day,c.vote.request)
    assert.equal(v.index,c.vote.index);assert.equal(v.round,c.vote.round)
    assert.equal(v.rule,c.vote.rule);assert.deepEqual(v.candidates,c.vote.candidates)
    assert.deepEqual(v.rows.map(r=>[r.name,r.votes,r.targetName]),c.vote.rows)
  }
  assert.equal(JSON.stringify(c.replay),before)
})
test('cutoff, explicit zero and exact legacy fallback stay separate from positional formatter lookup',()=>{
  assert.deepEqual(visibleReplaySkill(fixture,1,1,2),{value:5,sourceDay:3,history:true,missing:false})
  assert.deepEqual(visibleReplaySkill(fixture,1,1,3),{value:0,sourceDay:4,history:true,missing:false})
  const r=structuredClone(fixture)
  r.daySnapshots.forEach(s=>s.players.forEach(p=>p.skillTargets=[]))
  assert.equal(visibleReplaySkill(r,1,1,3).value,5)
  r.daySnapshots=r.daySnapshots.filter(s=>s.dayNumber!==2)
  assert.equal(visibleReplaySkill(r,1,1,3).missing,true)
  assert.equal(visibleReplaySkill(r,1,1,3).value,0)
  assert.ok(replayInformation(r,2).diagnostics.some(d=>d.code==='SKILL_RECORD_MISSING'))
})
test('icon path keeps snapshot st, actual-role fallback, alive(d) cutoff and order default independently of history',()=>{
  const r=structuredClone(fixture)
  r.daySnapshots[1].players[0].claimedRole=0;r.daySnapshots[1].players[0].claimedRoleOrder=0
  const info=replayInformation(r,3)
  assert.deepEqual(info.icons.map(x=>[x.day,x.actor,x.target,x.icon]),[
    [2,1,2,'images/result1_1black.png'],[3,1,3,'images/result1_1white.png'],
  ])
  assert.ok(!info.normalText.includes('双姉●')) // latest history explicitly zero, while day-2 icon remains black.
  r.daySnapshots[1].players[0].deathDay=2
  assert.equal(replayInformation(r,3).icons.some(x=>x.day===2),false)
  r.daySnapshots[2].players[0].claimedRoleOrder=6
  const broken=replayInformation(r,3)
  assert.equal(broken.icons[0].icon,null)
  assert.ok(broken.diagnostics.some(d=>d.code==='SKILL_ICON_UNAVAILABLE'))
})
test('all available result icon ranges resolve, unavailable orders retain text without a fabricated asset',()=>{
  const images=JSON.parse(read('../public/assets/images/manifest.json')).images
  for(const role of [1,2])for(const order of [0,1,2,3,4,5,6,44])for(const black of [false,true]){
    const r=structuredClone(fixture),p=r.daySnapshots[1].players[0]
    p.claimedRole=role;p.claimedRoleOrder=order;p.skillTarget=black?5:2
    const icon=replayInformation(r,2).icons[0]
    assert.equal(icon.order,order||1);assert.equal(icon.target,2);assert.equal(icon.black,black)
    if((order||1)<=(role===1?5:4))assert.ok(images[icon.icon])
    else assert.equal(icon.icon,null)
  }
})
test('snapshot gaps and missing votes are diagnosed without fabricated results or free-vote assumptions',()=>{
  const r=structuredClone(fixture);r.daySnapshots=r.daySnapshots.filter(s=>[1,4].includes(s.dayNumber))
  const info=replayInformation(r,3)
  assert.ok(info.diagnostics.some(d=>d.code==='SNAPSHOT_MISSING'))
  assert.equal(replayVotes(info.current,3),null)
  const unknown=structuredClone(fixture.daySnapshots[1]);unknown.dailyVotingRule=-1
  assert.equal(replayVotes(unknown,2).rule,-1)
})
test('vote display preserves array-position lookup, same-day dead voters and abstention',()=>{
  const s=structuredClone(fixture.daySnapshots[1])
  s.players[0].deathDay=2;s.players[1].deathDay=1
  const v=replayVotes(s,2,0)
  assert.deepEqual(v.rows.map(r=>r.name),['双弟','音楽'])
  assert.equal(v.rows[0].votes,1);assert.equal(v.rows[1].targetName,'棄権')
  // Desktop text uses array positions, not playerIndex searching, for voters and target names.
  s.players.reverse()
  assert.deepEqual(replayVotes(s,2,0).rows.map(r=>r.name),['音楽','双弟'])
})
test('source locks both skill starts, history zero precedence and independent icon path',()=>{
  const formatter=read('../../src/main/java/com/vocr/ui/ReplayTextFormatter.java')
  assert.match(formatter,/mediumResults, player, index, playerSum, 1, day, true/)
  assert.match(formatter,/hunterResults, player, index, playerSum, 2, day, false/)
  assert.match(formatter,/findSnapshot\(replay, skillDay\)/)
  const save=read('../../src/main/java/com/vocr/replay/ReplaySave.java')
  assert.match(save,/int historyCutoffDay = visibleDay \+ 1/)
  assert.match(save,/if \(latestHistoryDay >= 0\) return historyTarget/)
  const handler=read('../../src/main/java/com/vocr/ui/ReplayPlayerHandler.java')
  assert.match(handler,/for \(int d = 2; d <= day; d\+\+\)/)
  assert.match(handler,/ps.claimedRole > 0 \? ps.claimedRole : ps.actualRole/)
  assert.match(handler,/if \(!ps.isAlive\(d\)\) continue/)
  assert.match(handler,/if \(day >= totalDays\)/)
})

test('034 O-1 missing exposure array entry produces a bounded diagnosis; public validation still rejects the malformed recording',()=>{
  const r=structuredClone(fixture)
  r.daySnapshots.forEach(s=>{s.players[0].claimedRole=0})
  r.daySnapshots[1].players=[]
  assert.throws(()=>validateReplay(r),/INVALID_REPLAY/)
  assert.ok(replayInformation(r,3).diagnostics.some(d=>d.section==='exposure'&&d.code==='SKILL_RECORD_MISSING'))
})
