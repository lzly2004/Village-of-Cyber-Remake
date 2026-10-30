import {test,expect} from '@playwright/test'
import {completeLocalReading} from '../test-fixtures/presentation-test.js'
import {readFileSync,mkdirSync,writeFileSync} from 'node:fs'
import {createHash} from 'node:crypto'
import {publicSnapshot} from '../test-fixtures/public-players.js'
const manifest=JSON.parse(readFileSync(new URL('../public/assets/images/manifest.json',import.meta.url),'utf8'))
const url=n=>'/assets/images/'+manifest.images['images/'+n].file
const p=(stage,eventname,ch1='Abel')=>({stage,event:{eventname,ch1,ch2:'NONE',ch3:'NONE'}})
const key='vocr.guest.v1',id='00000000-0000-4000-8000-000000000052'
const phase=page=>page.getByTestId('game-stage')
async function setup(page,list,{terminal=false}={}){
 const snap=publicSnapshot();snap.presentedEvents=list;snap.events=list.map(p=>p.event);snap.nextEventCursor=list.length
 if(terminal){snap.phase='FINISHED';snap.view.result='VILLAGE_WIN'}
 await page.route('**/api/v1/games',async r=>{if(frozen.has(page)){const now=Date.now();await page.clock.install({time:new Date(now)});await page.clock.setFixedTime(new Date(now));await page.clock.pauseAt(new Date(now));await page.clock.setSystemTime(new Date(now))}await r.fulfill({status:201,json:{sessionId:id,update:snap}})})
 await page.route('**/api/v1/games/'+id+'?**',r=>{const cursor=Number(new URL(r.request().url()).searchParams.get('eventCursor'));return r.fulfill({json:{...snap,eventCursor:cursor,events:snap.events.slice(cursor),presentedEvents:snap.presentedEvents.slice(cursor)}})})
 await page.goto('/');await page.getByRole('button',{name:'対局を始める'}).click()
}
async function ready(page,value){
 await expect(phase(page)).toHaveAttribute('data-presentation-phase',value)
 await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-ready','true')
}
// The strict timing fixture fixes wall time before pausing, avoiding a sampled-time/RPC race.
const frozen=new WeakSet()
async function clock(page){frozen.add(page)}
async function dawn(page){await ready(page,'night');await page.clock.runFor(7000);await ready(page,'dawn');await page.clock.runFor(2500);await expect(phase(page)).toHaveAttribute('data-presentation-phase','day-text');await page.clock.runFor(1000)}
// Vue timestamps newly mounted event handlers; advance one tick before a new synthetic click.
async function click(page,id){if(id==='next-event')await completeLocalReading(page);await expect(page.getByTestId(id)).toBeEnabled();if(frozen.has(page))await page.clock.runFor(1);await page.getByTestId(id).click()}
const captures=[]
async function captureScene(page,info,name){
 const bytes=await page.getByTestId('stage-surface').screenshot({path:info.outputPath(name)})
 const directory=new URL('../../build/verify/night-day/',import.meta.url);mkdirSync(directory,{recursive:true})
 writeFileSync(new URL(name,directory),bytes)
 captures.push({name,bytes:bytes.length,sha256:createHash('sha256').update(bytes).digest('hex')})
 writeFileSync(new URL('screenshots.json',directory),JSON.stringify({source:'synthetic public snapshot, real browser rendering; virtual timing checked separately',captures},null,2)+'\n')
}
async function saved(page){return page.evaluate(k=>sessionStorage.getItem(k),key)}

test('night7000, dawn2500, local day text continue and death are distinct; local time never acknowledges',async({page},info)=>{
 await clock(page);await setup(page,[p('NIGHT_RESULT','yjsw'),p('DAY_DIALOGUE','gyfo1')]);await ready(page,'night')
 const before=await saved(page),writes=[];page.on('request',r=>{if(['POST','PUT','DELETE'].includes(r.method()))writes.push(r.url())})
 await expect(page.getByTestId('stage-background')).toHaveAttribute('href',url('komorebi000night01.png'))
 await expect(page.getByTestId('stage-character')).toHaveCount(0);await expect(page.getByTestId('event-dialogue')).toHaveCount(0);await expect(page.getByTestId('stage-text')).toHaveCount(0)
 await captureScene(page,info,'night.png')
 await page.clock.runFor(6999);await expect(phase(page)).toHaveAttribute('data-presentation-phase','night')
 await page.clock.runFor(1);await ready(page,'dawn');await expect(page.getByTestId('stage-background')).toHaveAttribute('href',url('komorebi002.png'))
 await expect(page.getByTestId('stage-character')).toHaveCount(0);await expect(page.getByTestId('stage-text')).toHaveCount(0)
 await captureScene(page,info,'dawn.png')
 await page.clock.runFor(2499);await expect(phase(page)).toHaveAttribute('data-presentation-phase','dawn')
 await page.clock.runFor(1);await expect(page.getByTestId('day-start-continue')).toBeDisabled();await page.clock.runFor(1000)
 await expect(page.getByTestId('day-start-text')).toHaveText('1日目になりました。')
 await page.getByTestId('day-start-continue').focus();await page.keyboard.press('Enter');await ready(page,'event')
 await expect(page.getByTestId('stage-character')).toHaveCount(1)
 await page.clock.runFor(1999);await expect(page.getByTestId('desktop-message')).toHaveCount(0)
 await page.clock.runFor(1);await expect(phase(page)).toHaveAttribute('data-reading-step','primary')
 await expect(page.getByTestId('event-dialogue')).toHaveText('')
 await page.clock.runFor(50);await expect(page.getByTestId('event-dialogue')).toBeVisible()
 expect(await saved(page)).toBe(before);expect(writes).toEqual([])
 await captureScene(page,info,'night-result.png')
 await click(page,'next-event');expect(JSON.parse(await saved(page)).eventCursor).toBe(1)
})
test('same-night multiple deaths do not repeat; next execution-to-night does',async({page})=>{
 await clock(page);await setup(page,[p('NIGHT_RESULT','yjsw'),p('NIGHT_RESULT','yjsw','Beatrice'),p('EXECUTION','cxs'),p('NIGHT_RESULT','wsw','NONE')])
 await dawn(page);await click(page,'day-start-continue');await ready(page,'event')
 await click(page,'next-event');await ready(page,'event');expect(JSON.parse(await saved(page)).eventCursor).toBe(1)
 await click(page,'next-event');await ready(page,'event')
 await click(page,'next-event');await ready(page,'night');expect(JSON.parse(await saved(page)).eventCursor).toBe(3)
})
test('execution-to-public-settlement has night only; no early settlement scene or role reveal',async({page})=>{
 await clock(page);await setup(page,[p('EXECUTION','cxs'),p('SETTLEMENT','crsl')],{terminal:true})
 await click(page,'next-event');await ready(page,'night')
 await expect(page.locator('.settlement')).toHaveCount(0);await expect(page.getByTestId('settlement-board')).toHaveCount(0)
 await expect(page.getByTestId('event-dialogue')).toHaveCount(0)
 await page.clock.runFor(7000);await ready(page,'event')
 await expect(page.getByTestId('stage-background')).toHaveAttribute('href',url('endVillage.png'))
 await expect(page.getByTestId('day-start-continue')).toHaveCount(0);expect(JSON.parse(await saved(page)).eventCursor).toBe(1)
})
test('refresh replays local first-night presentation without moving saved cursor',async({page})=>{
 await clock(page);await setup(page,[p('NIGHT_RESULT','wsw','NONE')]);await ready(page,'night');await page.clock.runFor(3000)
 const before=await saved(page);await page.reload();await ready(page,'night');expect(await saved(page)).toBe(before)
 await dawn(page);await click(page,'day-start-continue');await ready(page,'event');expect(await saved(page)).toBe(before)
})
test('broken transition image diagnoses and local exit does not consume current event',async({page})=>{
 await page.route('**'+url('komorebi000night01.png'),r=>r.abort('failed'))
 await setup(page,[p('NIGHT_RESULT','yjsw')]);await expect(phase(page)).toHaveAttribute('data-presentation-phase','error')
 const before=await saved(page);await expect(page.getByTestId('day-start-continue')).toBeEnabled()
 await click(page,'day-start-continue');await ready(page,'event');expect(await saved(page)).toBe(before)
 await expect(page.getByTestId('next-event')).toBeEnabled()
})
test('reduced motion keeps both static backgrounds and their order, but no day text animation',async({page})=>{
 await clock(page);await page.emulateMedia({reducedMotion:'reduce'});await setup(page,[p('NIGHT_RESULT','yjsw')]);await ready(page,'night')
 await page.clock.runFor(7000);await ready(page,'dawn');await page.clock.runFor(2500)
 await expect(page.getByTestId('day-start-text')).toHaveText('1日目になりました。');await expect(page.getByTestId('day-start-continue')).toBeEnabled()
 await click(page,'day-start-continue');await ready(page,'event');await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-frame','2')
})

test('held night image starts the seven seconds only after decode; no future background prefetch',async({page})=>{
 let release,seen=false
 const gate=new Promise(resolve=>{release=resolve})
 await page.route('**'+url('komorebi000night01.png'),async r=>{seen=true;await gate;await r.continue()})
 const images=[];page.on('request',r=>images.push(new URL(r.url()).pathname))
 try{
  await clock(page);await setup(page,[p('NIGHT_RESULT','wsw','NONE')])
  await expect.poll(()=>seen).toBe(true)
  await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-ready','false')
  await page.clock.runFor(9000)
  await expect(phase(page)).toHaveAttribute('data-presentation-phase','night')
  expect(images).not.toContain(url('komorebi002.png'))
  release();await ready(page,'night');await page.clock.runFor(6999)
  await expect(phase(page)).toHaveAttribute('data-presentation-phase','night')
  await page.clock.runFor(1);await ready(page,'dawn')
 }finally{release()}
})

test('uncertain synchronization cancels local timers without acknowledging or revealing future settlement',async({page})=>{
 await clock(page);await setup(page,[p('NIGHT_RESULT','yjsw')]);await ready(page,'night')
 const before=await saved(page)
 await page.route('**/api/v1/games/'+id+'?**',r=>r.fulfill({status:503,json:{code:'UNAVAILABLE',message:'test read unavailable'}}))
 await page.clock.runFor(1);await page.getByRole('button',{name:'再同期',exact:true}).click()
 await expect(page.getByRole('alert')).toContainText('暂时无法读取权威状态')
 await page.clock.runFor(20000)
 await expect(phase(page)).toHaveAttribute('data-presentation-phase','event')
 await expect(page.getByTestId('day-start-continue')).toHaveCount(0)
 expect(await saved(page)).toBe(before)
})

test('missing history prefix is diagnosed rather than guessed as another night',async({page})=>{
 const snapshot=publicSnapshot(),current=p('NIGHT_RESULT','yjsw')
 snapshot.eventCursor=1;snapshot.nextEventCursor=2;snapshot.events=[current.event];snapshot.presentedEvents=[current]
 await page.addInitScript(({id,key})=>sessionStorage.setItem(key,JSON.stringify({sessionId:id,eventCursor:1})),{id,key})
 await page.route('**/api/v1/games/'+id+'?**',r=>new URL(r.request().url()).searchParams.get('eventCursor')==='0'
  ?r.fulfill({status:503,json:{code:'UNAVAILABLE'}}):r.fulfill({json:snapshot}))
 await page.goto('/')
 await expect(phase(page)).toHaveAttribute('data-presentation-phase','event')
 await expect(page.getByText('場面表示診断：NIGHT_CONTEXT_UNAVAILABLE')).toBeVisible()
 await expect(page.getByTestId('day-start-continue')).toHaveCount(0)
 expect(JSON.parse(await saved(page)).eventCursor).toBe(1)
})

test('hung background has a bounded local exit; late completion cannot restore a canceled night',async({page})=>{
 let release,seen=false
 const gate=new Promise(resolve=>{release=resolve})
 await page.route('**'+url('komorebi000night01.png'),async r=>{seen=true;await gate;await r.continue()})
 try{
  await clock(page);await setup(page,[p('NIGHT_RESULT','yjsw')]);await expect.poll(()=>seen).toBe(true)
  const before=await saved(page)
  await page.clock.runFor(15000)
  await expect(phase(page)).toHaveAttribute('data-presentation-phase','error')
  await expect(page.getByText('場面表示診断：NIGHT_IMAGE_TIMEOUT')).toBeVisible()
  await click(page,'day-start-continue');await ready(page,'event')
  release();await page.clock.runFor(10000)
  await expect(phase(page)).toHaveAttribute('data-presentation-phase','event')
  expect(await saved(page)).toBe(before)
 }finally{release()}
})
