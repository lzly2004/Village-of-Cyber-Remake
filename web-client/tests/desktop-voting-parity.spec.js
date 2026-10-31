import {test,expect} from '@playwright/test'
import {publicSnapshot} from '../test-fixtures/public-players.js'
import {mkdirSync,writeFileSync} from 'node:fs'
import {createHash} from 'node:crypto'
const id='00000000-0000-4000-8000-000000000051'
async function setup(page){
 const snapshot=publicSnapshot();snapshot.view.gameDay=3
 snapshot.legalVoteActions=[{method:'FREE',candidates:snapshot.view.players.map(p=>p.playerNumber)}]
 snapshot.claimInquiry.roles=['zhan','ling','lie','gong','mao'];snapshot.claimInquiry.players=[1,2]
 snapshot.publicInformation.skillReports=[{playerNumber:1,claimedRole:'zhan',claimedRoleOrder:1,exposed:false,results:[{skillDay:1,targetPlayerNumber:2,color:'WHITE'},{skillDay:2,targetPlayerNumber:3,color:'BLACK'}]}]
 snapshot.view.players[0].claimedRole='zhan';snapshot.view.players[0].claimedRoleOrder=1
 await page.route('**/api/v1/games',r=>r.fulfill({status:201,json:{sessionId:id,update:snapshot}}))
 await page.route('**/api/v1/games/'+id+'?**',r=>r.fulfill({json:snapshot}))
 await page.goto('/');await page.getByRole('button',{name:'対局を始める'}).click();await expect(page.getByTestId('desktop-vote-window')).toBeVisible()
 return snapshot
}
async function capture(page,name){const data=await page.getByTestId('stage-surface').screenshot();const dir=new URL('../../build/verify/desktop-voting-parity/',import.meta.url);mkdirSync(dir,{recursive:true});writeFileSync(new URL(name+'.png',dir),data);writeFileSync(new URL(name+'.json',dir),JSON.stringify({source:'Synthetic audited public DTO; actual browser render, not user playtest',bytes:data.length,sha256:createHash('sha256').update(data).digest('hex')},null,2))}
test('main board is desktop five-section text with calendar art and public black/white markers',async({page})=>{
 await setup(page);const writes=[];page.on('request',r=>{if(['POST','PUT','DELETE'].includes(r.method()))writes.push(r.url())})
 await expect(page.getByTestId('desktop-information')).toContainText('[占い師]')
 await expect(page.getByTestId('desktop-information')).toContainText('[護衛先]')
 await expect(page.locator('.desktop-vote-information .public-info-help')).toHaveCount(0)
 await expect(page.locator('.desktop-vote-information input')).toHaveCount(0)
 await expect(page.locator('.desktop-calendar img')).toBeVisible()
 await expect(page.locator('.skill-report-marker')).toHaveCount(2)
 for(const width of [1440,1024,800]){
  await page.setViewportSize({width,height:1000});const surface=await page.getByTestId('stage-surface').boundingBox(),info=await page.locator('.desktop-vote-information').boundingBox()
  expect(Math.abs((info.y-surface.y)/surface.width-198/1280)).toBeLessThan(.002)
  const rows=page.locator('.desktop-vote-players .desktop-roster-row');const last=rows.last();await last.evaluate(n=>n.scrollIntoView({block:'nearest',inline:'nearest'}))
  const box=await last.boundingBox(),roster=await page.locator('.desktop-vote-players').boundingBox();expect(box.y+box.height).toBeLessThanOrEqual(roster.y+roster.height+1)
  await capture(page,'information-'+width)
 }
 await page.setViewportSize({width:1440,height:1000})
 await page.getByRole('button',{name:'指示する',exact:true}).click();await expect(page.getByRole('button',{name:'指定する',exact:true})).toBeVisible()
 await capture(page,'orders-menu')
 await page.getByRole('button',{name:'指定する',exact:true}).click();await page.getByRole('button',{name:'指定投票',exact:true}).click()
 await expect(page.getByRole('region',{name:'指定投票',exact:true})).toBeVisible();await capture(page,'selection')
 expect(writes).toEqual([])
})
test('completed vote shows authoritative tie and final result before execution, with local-only continues',async({page})=>{
 const snapshot=await setup(page);const round={round:1,maximumVotes:2,leaders:[1,2],lines:[{playerNumber:1,targetPlayerNumber:2,receivedVotes:2},{playerNumber:2,targetPlayerNumber:1,receivedVotes:2}]}
 const vote={day:3,method:'FREE',candidates:[1,2],rounds:[round,{...round,round:2,maximumVotes:4,leaders:[2]}]}
 const event={stage:'EXECUTION',event:{eventname:'cxs',ch1:'Abel',ch2:'Erich',ch3:'NONE'}}
 await page.route('**/api/v1/games/'+id+'/votes',r=>r.fulfill({json:{voteCompleted:true,update:{...snapshot,revision:2,voteHistory:[vote],nextEventCursor:1,events:[event.event],presentedEvents:[event]}}}))
 await page.getByTestId('vote').click();await page.getByTestId('vote-FREE').click()
 await expect(page.getByTestId('desktop-vote-review')).toContainText('投票が同点')
 await expect(page.getByTestId('desktop-message')).toHaveCount(0);await expect(page.getByTestId('stage-character')).toHaveCount(0)
 const before=await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'));const writes=[];page.on('request',r=>{if(['POST','PUT','DELETE'].includes(r.method()))writes.push(r.url())})
 await capture(page,'vote-tie');await page.getByTestId('vote-review-continue').click()
 await expect(page.getByTestId('desktop-vote-review')).toContainText('4票で');await capture(page,'vote-final')
 await page.getByTestId('vote-review-continue').click();await expect(page.getByTestId('next-event')).toBeVisible()
 expect(await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))).toBe(before);expect(writes).toEqual([])
})

test('CO interruption does not invent a vote result and does not reveal execution',async({page})=>{
 const snapshot=await setup(page)
 const event={stage:'DAY_DIALOGUE',event:{eventname:'gyfo1',ch1:'Abel',ch2:'NONE',ch3:'NONE'}}
 await page.route('**/api/v1/games/'+id+'/votes',r=>r.fulfill({json:{voteCompleted:false,update:{...snapshot,revision:2,nextEventCursor:1,events:[event.event],presentedEvents:[event]}}}))
 await page.getByTestId('vote').click();await page.getByTestId('vote-FREE').click()
 await expect(page.getByTestId('next-event')).toBeVisible();await expect(page.getByTestId('desktop-vote-review')).toHaveCount(0)
 expect(JSON.parse(await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))).eventCursor).toBe(0)
})

test('selection tool uses original drag resources and sends only the existing scoped target command',async({page})=>{
 const snapshot=await setup(page);const writes=[]
 await page.route('**/api/v1/games/'+id+'/vote-target',async r=>{
  const body=r.request().postDataJSON();writes.push(body)
  await r.fulfill({json:{...snapshot,revision:2,selections:{...snapshot.selections,voteTargets:[body.target]},legalVoteActions:[{method:'DESIGNATED',candidates:[body.target]}]}})
 })
 await page.getByRole('button',{name:'指示する',exact:true}).click();await page.getByRole('button',{name:'指定する',exact:true}).click();await page.getByRole('button',{name:'指定投票',exact:true}).click()
 const target=page.locator('.desktop-target').first()
 await target.dispatchEvent('drop',{dataTransfer:await page.evaluateHandle(()=>{const data=new DataTransfer();data.setData('text/plain','external file or text');return data})})
 expect(writes).toEqual([])
 await page.getByRole('button',{name:'指定投票を設定',exact:true}).dragTo(target)
 await expect(target).toHaveAttribute('aria-checked','true');expect(writes).toEqual([{expectedRevision:1,target:1,selected:true}])
 await expect(target.locator('.character-title-image')).toHaveCount(0);await expect(target.locator('.death-marker')).toHaveCount(0)
 await page.getByRole('button',{name:'戻る',exact:true}).click();await page.getByTestId('vote').click()
 await expect(page.getByTestId('vote-DESIGNATED')).toBeVisible();await expect(page.getByTestId('vote-FREE')).toHaveCount(0)
 await capture(page,'vote-confirm-designated')
})

test('holding the seer CO button never moves it into the right-hand menu column',async({page})=>{
 await setup(page)
 await page.getByRole('button',{name:'指示する',exact:true}).click()
 await page.getByRole('button',{name:'COを聞く',exact:true}).click()
 const button=page.getByRole('button',{name:'占い師のCOを聞く',exact:true})
 const before=await button.boundingBox()
 await page.mouse.move(before.x+before.width/2,before.y+before.height/2)
 await page.mouse.down()
 try{
  // Inspect the real pressed state, rather than forcing a click past instability.
  await expect.poll(()=>button.evaluate(n=>getComputedStyle(n).transitionDuration)).toBe('0s')
  const pressed=await button.boundingBox()
  expect(Math.abs(pressed.x-before.x)).toBeLessThan(.5)
  expect(Math.abs(pressed.y-before.y)).toBeLessThan(.5)
 }finally{await page.mouse.up()}
})


test('desktop one-second tip overlays the unchanged information and leaves commands and cursor untouched',async({page})=>{
 const snapshot=await setup(page)
 await page.clock.install({time:new Date('2030-01-01T00:00:00Z')})
 await page.clock.pauseAt(new Date('2030-01-01T00:00:01Z'))
 snapshot.view.gameDay=2
 await page.getByRole('button',{name:'再同期',exact:true}).click()
 const info=page.getByTestId('desktop-information'),before=await info.textContent(),writes=[]
 page.on('request',r=>{if(['POST','PUT','DELETE'].includes(r.method()))writes.push(r.url())})
 const cursor=await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))
 await page.getByRole('button',{name:'記録確認',exact:true}).click()
 const tip=page.locator('.desktop-transient-tip');await expect(tip).toContainText('投票の履歴もない')
 await expect(info).toHaveText(before)
 const tipBox=await tip.boundingBox(),surface=await page.getByTestId('stage-surface').boundingBox()
 expect(Math.abs((tipBox.x-surface.x)/surface.width-300/1280)).toBeLessThan(.002)
 await page.clock.runFor(999);await expect(tip).toBeVisible()
 await page.clock.runFor(1);await expect(tip).toHaveCount(0)
 expect(writes).toEqual([]);expect(await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))).toBe(cursor)
})
