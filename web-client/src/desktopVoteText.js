// Desktop VoteInfoRenderer/VoteResultRenderer projection. Consume public DTOs only;
// never derive real roles, vote winners, hidden skill targets or future outcomes.
import { executionText } from './publicInformationText.js'
const roleNumbers={zhan:1,ling:2,lie:3}
const symbols={WHITE:'○',BLACK:'●',NOT_APPLICABLE:''}
const trim=text=>text.endsWith('→')?text.slice(0,-1):text
export function desktopInformation(information,name) {
  const sections={zhan:'',ling:'',lie:''}
  for(const report of information?.skillReports || []) {
    if(!Object.hasOwn(sections,report.claimedRole))continue
    let line=name(report.playerNumber)+' : '
    for(const result of report.results)line+=name(result.targetPlayerNumber)+(symbols[result.color]??'？')+'→'
    if(report.exposed)line+='破绽→'
    sections[report.claimedRole]+=trim(line).trimEnd()+'\n'
  }
  const deaths=information?.deathHistory || []
  const executions=deaths.flatMap(day=>day.executions.map(d=>executionText(d,name).replaceAll('（','(').replaceAll('）',')'))).join('→')
  const bodies=deaths.map(day=>day.nightDeaths.length?day.nightDeaths.map(name).join('+'):'平和').join('→')
  return '[占い師]\n'+sections.zhan+'[霊能者]\n'+sections.ling+'[処刑]\n'+executions+'\n[死体]\n'+bodies+'\n[護衛先]\n'+sections.lie
}
export function publicSkillMarkers(information,players,day) {
  const markers=new Map()
  for(const report of information?.skillReports || []) {
    const role=roleNumbers[report.claimedRole],order=report.claimedRoleOrder
    if(![1,2].includes(role)||!Number.isSafeInteger(order)||order<1||order>(role===1?5:4))continue
    const actor=players.find(p=>p.playerNumber===report.playerNumber)
    if(!actor)continue
    for(const result of report.results) {
      const displayDay=result.skillDay+1
      if(displayDay>day||(actor.deathDay>0&&actor.deathDay<displayDay)||!['WHITE','BLACK'].includes(result.color))continue
      const target=result.targetPlayerNumber
      const icon=`images/result${role}_${order}${result.color==='BLACK'?'black':'white'}.png`
      const entry={actor:report.playerNumber,day:displayDay,order,role,target,icon}
      markers.set(target,[...(markers.get(target)||[]),entry])
    }
  }
  return markers
}
export function desktopVoteRound(vote,index,name,{history=false}={}) {
  const round=vote?.rounds[index]
  if(!round)return null
  const candidates=vote.method==='GREY'?vote.candidates.map(name).join(''):vote.candidates.map(name).join(',')+(vote.candidates.length?',':'')
  let title=index>0?`-投票結果/${vote.day}日目-重新投票\n\n`:
    vote.method==='FREE'?`-投票結果/${vote.day}日目-自由投票\n\n`:
    vote.method==='GREY'?`-投票結果/${vote.day}日目-グレラン：\n${candidates}\n`:
    `-投票結果/${vote.day}日目-指定投票\n${candidates}\n`
  if(history){const extra=vote.method==='FREE'?'自由投票\n':vote.method==='GREY'?'グレラン：\n'+candidates:'指定投票：\n'+candidates;title=`-投票結果/${vote.day}日目-第${round.round}轮${extra}\n`}
  if(!['FREE','GREY','DESIGNATED'].includes(vote.method))title=`-投票結果/${vote.day}日目-未対応の投票方式\n\n`
  return {title,
    lines:round.lines.map(line=>`${name(line.playerNumber)}：${line.receivedVotes}票  投票先→${name(line.targetPlayerNumber)}`),
    conclusion:round.leaders.length>1?'投票が同点となりました。再投票を行います。':`${round.maximumVotes}票で${name(round.leaders[0])}さんが処刑されました。`}
}
