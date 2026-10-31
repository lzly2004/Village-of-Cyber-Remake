import test from 'node:test'
import assert from 'node:assert/strict'
import { skillResultText, suspicionText, executionText } from '../src/publicInformationText.js'

const name = number => `${number}番`
for (const [color, suffix] of [['WHITE', '○'], ['BLACK', '●'], ['NOT_APPLICABLE', '']]) {
  test(`skill report ${color} preserves supplied day and target`, () => {
    assert.equal(skillResultText({ skillDay: 2, targetPlayerNumber: 7, color }, name), '第2日：7番' + suffix)
  })
}
test('unknown result does not silently become white or guard', () => {
  assert.equal(skillResultText({ skillDay: 1, targetPlayerNumber: 2, color: 'NEW' }, name), '第1日：2番（未対応の結果）')
})
test('suspicion preserves supplied target order and all four comparison categories', () => {
  assert.equal(suspicionText({ playerNumber: 1, targets: [3, 2, 4], comparisons: ['CLOSE', 'SLIGHTLY_GREATER'] }, name), '1番：3番=2番≧4番')
  assert.equal(suspicionText({ playerNumber: 1, targets: [4, 2, 3], comparisons: ['GREATER', 'MUCH_GREATER'] }, name), '1番：4番>2番≫3番')
})
test('empty suspicion row is blank, not an observer with fabricated no-suspicion conclusion', () => {
  assert.equal(suspicionText({ playerNumber: 1, targets: [], comparisons: [] }, name), '')
})
test('unknown comparison has explicit fallback, never an invented strength', () => {
  assert.equal(suspicionText({ playerNumber: 1, targets: [3, 2], comparisons: ['NEW'] }, name), '1番：3番？2番')
})
test('public death prefixes and day causes are presentation only', () => {
  assert.equal(executionText({ playerNumber: 2, prefixPlayers: [], cause: 'EXECUTION' }, name), '2番')
  assert.equal(executionText({ playerNumber: 2, prefixPlayers: [3], cause: 'DAY_CAT_CURSE' }, name), '3番+2番（猫呪）')
  assert.equal(executionText({ playerNumber: 2, prefixPlayers: [4], cause: 'DAY_PURSUED' }, name), '4番+2番（後追）')
  assert.equal(executionText({ playerNumber: 2, prefixPlayers: [], cause: 'NEW' }, name), '2番（未対応の死因）')
})

// Desktop VoteInfoRenderer/GameStrings are the contract, not the former Web report cards.
import { desktopInformation, publicSkillMarkers, desktopVoteRound } from '../src/desktopVoteText.js'
test('desktop board contains only the five compact sections and public arrow chains', () => {
  const info={skillReports:[{playerNumber:1,claimedRole:'zhan',claimedRoleOrder:1,exposed:true,results:[{skillDay:1,targetPlayerNumber:2,color:'WHITE'}]},{playerNumber:3,claimedRole:'lie',claimedRoleOrder:1,exposed:false,results:[{skillDay:2,targetPlayerNumber:1,color:'NOT_APPLICABLE'}]}],deathHistory:[{day:1,executions:[{playerNumber:2,prefixPlayers:[],cause:'EXECUTION'}],nightDeaths:[]}]}
  assert.equal(desktopInformation(info,name),'[占い師]\n1番 : 2番○→破绽\n[霊能者]\n[処刑]\n2番\n[死体]\n平和\n[護衛先]\n3番 : 1番\n')
  assert.equal(desktopInformation(null,name),'[占い師]\n[霊能者]\n[処刑]\n\n[死体]\n\n[護衛先]\n')
})
test('public target markers preserve role, CO order, day and actor death cutoff without reading secrets',()=>{
 const actor={playerNumber:1,deathDay:0};Object.defineProperty(actor,'actualRole',{get(){throw Error('secret')}})
 const info={skillReports:[{playerNumber:1,claimedRole:'zhan',claimedRoleOrder:2,results:[{skillDay:1,targetPlayerNumber:3,color:'WHITE'},{skillDay:2,targetPlayerNumber:2,color:'BLACK'}]}]}
 assert.deepEqual(publicSkillMarkers(info,[actor],3).get(2),[{actor:1,day:3,order:2,role:1,target:2,icon:'images/result1_2black.png'}])
 assert.equal(publicSkillMarkers(info,[{playerNumber:1,deathDay:2}],3).get(2),undefined)
 assert.equal(publicSkillMarkers({skillReports:[{...info.skillReports[0],claimedRoleOrder:99}]},[actor],3).size,0)
})
test('desktop round preserves authoritative lines and maxima without recounting or leaking upcoming rounds',()=>{
 const vote={day:2,method:'FREE',candidates:[1,2],rounds:[{round:1,maximumVotes:9,leaders:[2,1],lines:[{playerNumber:1,targetPlayerNumber:2,receivedVotes:0}]}]}
 const text=desktopVoteRound(vote,0,name)
 assert.match(text.title,/-投票結果\/2日目-自由投票/)
 assert.deepEqual(text.lines,['1番：0票  投票先→2番'])
 assert.equal(text.conclusion,'投票が同点となりました。再投票を行います。')
 assert.equal(desktopVoteRound({...vote,rounds:[{...vote.rounds[0],leaders:[2]}]},0,name).conclusion,'9票で2番さんが処刑されました。')
})

test('desktop compact captions and vote rows remain anchored to GameStrings rather than duplicated Web copy',async()=>{
 const {readFileSync}=await import('node:fs')
 const java=readFileSync(new URL('../../src/main/java/com/vocr/ui/GameStrings.java',import.meta.url),'utf8')
 const constant=name=>JSON.parse(java.match(new RegExp('String\\s+'+name+'\\s*=\\s*("(?:\\\\.|[^"\\\\])*")'))[1])
 for(const [key,text] of [['SECTION_SEER','[占い師]\n'],['SECTION_MEDIUM','[霊能者]\n'],['SECTION_EXECUTION','[処刑]\n'],['SECTION_BODY','[死体]\n'],['MARKER_EXPOSED','破绽']])assert.equal(constant(key),text)
 const line=constant('VOTE_LINE_FORMAT').replace('%s','A').replace('%d','0').replace('%s','B').trimEnd()
 const row=desktopVoteRound({day:2,method:'FREE',candidates:[1,2],rounds:[{round:1,leaders:[2],maximumVotes:4,lines:[{playerNumber:1,targetPlayerNumber:2,receivedVotes:0}]}]},0,n=>n===1?'A':'B')
 assert.equal(row.lines[0],line)
 assert.equal(row.conclusion,constant('VOTE_RESULT_FORMAT').replace('%d','4').replace('%s','B'))
})
