import { characters } from './characterDisplay.js'
import { replayDay, replaySnapshot } from './replayPlayback.js'

const appearances = Object.values(characters)
const shortName = n => appearances[n-1]?.shortName || '?' + n
const playerName = (snapshot,index) => shortName(snapshot.players[index-1]?.characterNumber || index)
const trimArrow = text => text.length>2 && text.endsWith('→') ? text.slice(0,-1) : text

// ReplaySave deliberately uses exact days and playerIndex for this path, unlike the formatter's positional lookup.
export function visibleReplaySkill(replay,playerIndex,skillDay,visibleDay) {
  let latest=-1,value=0
  for(const snapshot of replay.daySnapshots) {
    if(snapshot.dayNumber>visibleDay+1 || snapshot.dayNumber<=latest) continue
    const p=snapshot.players.find(p=>p.playerIndex===playerIndex)
    if(!p || skillDay>=p.skillTargets.length) continue
    latest=snapshot.dayNumber;value=p.skillTargets[skillDay]
  }
  if(latest>=0) return {value,sourceDay:latest,history:true,missing:false}
  const snapshot=replay.daySnapshots.find(s=>s.dayNumber===skillDay+1)
  const p=snapshot?.players.find(p=>p.playerIndex===playerIndex)
  return {value:p?.skillTarget || 0,sourceDay:snapshot?.dayNumber ?? null,history:false,missing:!p}
}

export function replayInformation(replay,requestedDay) {
  const day=replayDay(replay,requestedDay),count=replay.daySnapshots[0].players.length
  const diagnostics=[],seen=new Set()
  function diagnose(section,code,requestedDay,sourceDay=null) {
    const key=[section,code,requestedDay,sourceDay].join(':')
    if(!seen.has(key)){seen.add(key);diagnostics.push({key,section,code,requestedDay,sourceDay})}
  }
  function marked(s,index,d) {
    const p=s?.players?.[index]
    if(!p)diagnose('exposure','SKILL_RECORD_MISSING',d,s?.dayNumber ?? null)
    return !!p?.nonHumanMarked
  }
  function snapshot(d,section) {
    const result=replaySnapshot(replay,d)
    if(result.diagnostic) diagnose(section,result.diagnostic,d,result.snapshot?.dayNumber ?? null)
    return result.snapshot
  }
  // Missing next-day data at the end is the desktop's ordinary current-day fallback.
  const baseRecord=replaySnapshot(replay,day+1)
  const base=baseRecord.snapshot ? snapshot(day+1,'claims') : snapshot(day,'claims')
  if(!baseRecord.snapshot && day<replay.totalDays) diagnose('claims','AVATAR_NEXT_DAY_MISSING',day+1,base?.dayNumber ?? null)
  const sections={1:'',2:'',3:''}
  for(const [index,actor] of (base?.players || []).entries()) {
    if(![1,2,3].includes(actor.claimedRole)) continue
    let line=shortName(actor.characterNumber)+' : ',exposed=false
    const start=actor.claimedRole===3 ? 2 : 1
    for(let skillDay=start;skillDay<day;skillDay++) {
      const next=snapshot(skillDay+1,'skills')
      if(!next) continue
      const p=next.players[index]
      if(p.deathDay>0 && skillDay>=p.deathDay) break
      const target=visibleReplaySkill(replay,p.playerIndex,skillDay,day)
      if(target.missing) diagnose('skills','SKILL_RECORD_MISSING',skillDay+1)
      if(target.value!==0) {
        const black=target.value>count,position=black?target.value-count:target.value
        line+=playerName(next,position)+(actor.claimedRole===3?'':black?'●':'○')+'→'
      }
      // Exposure is read from the same day, never from the next-day skill record.
      if(!exposed) exposed=marked(snapshot(skillDay,'exposure'),index,skillDay)
    }
    if(!exposed) exposed=marked(snapshot(day,'exposure'),index,day)
    if(exposed) line+='破绽→'
    sections[actor.claimedRole]+=trimArrow(line)+'\n'
  }
  let executions='',bodies=''
  for(let d=1;d<day;d++) {
    const s=snapshot(d+1,'deaths')
    if(!s) continue
    const role=n=>s.players.find(p=>p.actualRole===n)
    let executed='',night=[]
    for(const p of s.players) {
      if(p.deathDay!==d) continue
      const name=shortName(p.characterNumber)
      if(p.deathReason===1) {
        if(p.actualRole===10) {const deviant=role(11);if(deviant?.deathDay>0 && deviant.deathDay<p.deathDay)executed+=name}
        else if(p.actualRole!==5)executed+=name
      } else if(p.deathReason===2)executed+=(role(5)?shortName(role(5).characterNumber)+'+':'')+name+'(猫呪)'
      else if(p.deathReason===3)executed+=(role(10)?shortName(role(10).characterNumber)+'+':'')+name+'(後追)'
      else if(p.deathReason>=4)night.push(name)
    }
    if(executed)executions+=executed+'→'
    bodies+=(night.length?night.join('+'):'平和')+'→'
  }
  const normalText='[占い師]\n'+sections[1]+'[霊能者]\n'+sections[2]+'[処刑]\n'+trimArrow(executions)+'\n[死体]\n'+trimArrow(bodies)+'\n[護衛先]\n'+sections[3]
  const current=snapshot(day,'votes')
  let final=null
  if(day>=replay.totalDays)final=snapshot(replay.totalDays,'roles') || snapshot(replay.totalDays-1,'roles')
  const groups=Array.from({length:11},(_,i)=>({role:i+1,players:(final?.players || []).filter(p=>p.actualRole===i+1).map(p=>({playerIndex:p.playerIndex,name:shortName(p.characterNumber)}))})).filter(g=>g.players.length)
  const icons=[]
  for(let d=2;d<=day;d++) {
    const s=snapshot(d,'icons')
    if(!s)continue
    for(const p of s.players) {
      const role=p.claimedRole>0?p.claimedRole:p.actualRole
      if(![1,2].includes(role) || (p.deathDay!==0 && p.deathDay<=d) || p.skillTarget===0)continue
      const black=p.skillTarget>count,target=black?p.skillTarget-count:p.skillTarget
      const order=p.claimedRoleOrder>0?p.claimedRoleOrder:1
      const icon=order<=(role===1?5:4)?`images/result${role}_${order}${black?'black':'white'}.png`:null
      if(!icon)diagnose('icons','SKILL_ICON_UNAVAILABLE',d,s.dayNumber)
      icons.push({day:d,sourceDay:s.dayNumber,actor:p.playerIndex,name:shortName(p.characterNumber),role,order,target,targetName:playerName(s,target),black,icon})
    }
  }
  return {day,terminal:day>=replay.totalDays,normalText,groups,icons,diagnostics,current}
}

export function replayVotes(snapshot,day,requestedRound=0) {
  if(!snapshot?.voteRounds.length)return null
  const index=Math.max(0,Math.min(requestedRound,snapshot.voteRounds.length-1)),vote=snapshot.voteRounds[index]
  const count=snapshot.players.length,targets=Array(count+1).fill(0),counts=Array(count+1).fill(0)
  for(const [voter,target] of vote.votes)if(target>=1){targets[voter]=target;counts[target]++}
  const rows=[]
  for(let position=1;position<=count;position++) {
    const p=snapshot.players[position-1]
    if(p.deathDay!==0 && p.deathDay<day)continue
    const target=targets[position]
    rows.push({position,name:shortName(p.characterNumber),votes:counts[position],target,targetName:target?playerName(snapshot,target):'棄権'})
  }
  const candidates=snapshot.dailyVotingRule===1?snapshot.greyTargetCharNums.map(n=>playerName(snapshot,n)):
    snapshot.dailyVotingRule===2?snapshot.designatedTargetCharNums.map(shortName):[]
  return {index,round:vote.round,total:snapshot.voteRounds.length,rule:snapshot.dailyVotingRule,candidates,rows}
}
