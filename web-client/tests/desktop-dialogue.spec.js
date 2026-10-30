import {test,expect} from '@playwright/test'
import {mkdirSync,writeFileSync} from 'node:fs'
import {createHash} from 'node:crypto'
import {publicSnapshot} from '../test-fixtures/public-players.js'
import {eventDialogue} from '../src/eventDialogue.js'
const id='00000000-0000-4000-8000-000000000050',key='vocr.guest.v1'
const stage=page=>page.getByTestId('game-stage')
async function setup(page,{kind='DAY_DIALOGUE',name='gyfo1',long=false}={}){
 const snapshot=publicSnapshot();const event={eventname:name,ch1:'Abel',ch2:'Erich',ch3:'NONE'}
 snapshot.presentedEvents=[{stage:kind,event},{stage:'DAY_DIALOGUE',event:{...event,eventname:'gyfo1'}}];snapshot.events=snapshot.presentedEvents.map(p=>p.event);snapshot.nextEventCursor=2
 if(long)await page.route('**/src/eventDialogue.js',r=>r.fulfill({contentType:'text/javascript',body:`export function eventDialogue(){return ${JSON.stringify({source:'character',diagnostic:null,text:Array.from({length:80},(_,i)=>`${i}行目：長い台詞を最後まで読む。`).join('\n')})}}`}))
 await page.route('**/api/v1/games',async r=>{const now=new Date(Date.now());await page.clock.install({time:now});await page.clock.setFixedTime(now);await page.clock.pauseAt(now);await page.clock.setSystemTime(now);await page.clock.runFor(0);await r.fulfill({status:201,json:{sessionId:id,update:snapshot}})})
 await page.route('**/api/v1/games/'+id+'?**',r=>{const cursor=Number(new URL(r.request().url()).searchParams.get('eventCursor'));return r.fulfill({json:{...snapshot,eventCursor:cursor,events:snapshot.events.slice(cursor),presentedEvents:snapshot.presentedEvents.slice(cursor)}})})
 await page.goto('/');await page.getByRole('button',{name:'対局を始める'}).click()
 await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-ready','true')
 return {snapshot,event}
}
const saved=page=>page.evaluate(k=>sessionStorage.getItem(k),key)
async function click(page,testid){await page.clock.runFor(1);await page.getByTestId(testid).click()}
async function full(page){await page.clock.runFor(50*Number(await stage(page).getAttribute('data-reading-length'))+1)}
async function capture(page,info,name){
 const data=await page.getByTestId('stage-surface').screenshot({path:info.outputPath(name)})
 const meta={name,source:'Synthetic public snapshot, real browser rendering; not user playtest',bytes:data.length,sha256:createHash('sha256').update(data).digest('hex')}
 for(const folder of [new URL('../../build/web050/dialogue/screenshots/',import.meta.url),new URL('../../build/verify/desktop-core/',import.meta.url)]){
  mkdirSync(folder,{recursive:true});writeFileSync(new URL(name,folder),data);writeFileSync(new URL(name+'.json',folder),JSON.stringify(meta,null,2))
 }
}

test('desktop dialogue types at50ms; reveal is local, next confirm persists exactly once',async({page},info)=>{
 const {event}=await setup(page);const before=await saved(page),writes=[];page.on('request',r=>{if(['POST','PUT','DELETE'].includes(r.method()))writes.push(r.url())})
 await expect(stage(page)).toHaveAttribute('data-reading-step','primary');await expect(page.getByTestId('event-dialogue')).toHaveText('')
 await page.clock.runFor(49);await expect(page.getByTestId('event-dialogue')).toHaveText('')
 await page.clock.runFor(1);await expect(page.getByTestId('event-dialogue')).toHaveText(Array.from(eventDialogue(event).text)[0])
 await click(page,'next-event');await expect(page.getByTestId('event-dialogue')).toHaveText(eventDialogue(event).text)
 expect(await saved(page)).toBe(before);expect(writes).toEqual([])
 await capture(page,info,'dialogue-complete.png')
 await click(page,'next-event');expect(JSON.parse(await saved(page)).eventCursor).toBe(1)
 await expect(stage(page)).toHaveAttribute('data-reading-complete','false');await page.clock.runFor(50);expect(JSON.parse(await saved(page)).eventCursor).toBe(1)
})
test('execution primary and consequence are distinct local steps before one acknowledgement',async({page},info)=>{
 await setup(page,{kind:'EXECUTION',name:'cxs'});const before=await saved(page)
 await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-frame','1');await full(page)
 await expect(page.getByTestId('execution-continue')).toBeVisible();expect(await saved(page)).toBe(before)
 await click(page,'execution-continue');await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-frame','2');await expect(page.getByTestId('desktop-message')).toHaveCount(0)
 await page.clock.runFor(1999);await expect(page.getByTestId('desktop-message')).toHaveCount(0)
 await page.clock.runFor(1);await expect(stage(page)).toHaveAttribute('data-reading-step','consequence');await expect(page.getByTestId('event-consequence')).toHaveText('')
 await click(page,'next-event');await expect(page.getByTestId('event-consequence')).not.toHaveText('');expect(await saved(page)).toBe(before)
 await capture(page,info,'execution-result.png');await click(page,'next-event');expect(JSON.parse(await saved(page)).eventCursor).toBe(1)
})
test('message sits at stage bottom and masks portrait cutoff at1280/1024/800',async({page},info)=>{
 await setup(page);await full(page)
 for(const width of [1280,1024,800]){
  await page.setViewportSize({width,height:900});const canvas=await page.getByTestId('stage-surface').boundingBox(),box=await page.getByTestId('desktop-message').boundingBox(),actor=await page.getByTestId('stage-character').first().boundingBox()
  expect(Math.abs(box.y+box.height-canvas.y-canvas.height)).toBeLessThan(1)
  expect(Math.abs((box.x-canvas.x)/canvas.width-260/1280)).toBeLessThan(.002)
  expect(box.y).toBeLessThan(actor.y+actor.height);expect(box.y+box.height).toBeGreaterThan(actor.y+actor.height)
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1)).toBe(true)
  const button=await page.getByTestId('next-event').boundingBox();expect(button.y+button.height).toBeLessThanOrEqual(canvas.y+canvas.height+1)
  await capture(page,info,'dialogue-'+width+'.png')
 }
})
test('long dialogue keyboard scroll keeps separate continue reachable and first Enter only reveals',async({page},info)=>{
 await setup(page,{long:true});const before=await saved(page)
 const reading=page.getByTestId('stage-text');await reading.focus();await page.clock.runFor(1);await page.keyboard.press('Enter')
 await expect(stage(page)).toHaveAttribute('data-reading-complete','true');expect(await saved(page)).toBe(before)
 expect(await reading.evaluate(n=>n.scrollHeight>n.clientHeight)).toBe(true)
 await reading.focus();await page.keyboard.press('PageDown');await page.clock.runFor(500);await expect.poll(()=>reading.evaluate(n=>n.scrollTop)).toBeGreaterThan(0)
 expect(await saved(page)).toBe(before);await capture(page,info,'dialogue-long.png')
 await page.getByTestId('next-event').focus();await page.keyboard.press('Enter');expect(JSON.parse(await saved(page)).eventCursor).toBe(1)
})
test('reduced motion presents full text immediately but still requires explicit next',async({page})=>{
 await page.emulateMedia({reducedMotion:'reduce'});const {event}=await setup(page)
 await expect(page.getByTestId('event-dialogue')).toHaveText(eventDialogue(event).text);expect(JSON.parse(await saved(page)).eventCursor).toBe(0)
 await click(page,'next-event');expect(JSON.parse(await saved(page)).eventCursor).toBe(1)
})
test('keyboard continue retains focus across a new dialogue and still needs reveal before acknowledgement',async({page})=>{
 await setup(page);await full(page)
 await page.getByTestId('next-event').focus();await page.clock.runFor(1);await page.keyboard.press('Enter')
 await expect(stage(page)).toHaveAttribute('data-reading-step','primary')
 await expect(page.getByTestId('next-event')).toBeFocused()
 await page.clock.runFor(1);await page.keyboard.press('Enter')
 expect(JSON.parse(await saved(page)).eventCursor).toBe(1)
 await expect(stage(page)).toHaveAttribute('data-reading-complete','true')
 await page.clock.runFor(1);await page.keyboard.press('Enter')
 expect(JSON.parse(await saved(page)).eventCursor).toBe(2)
})
test('late previous text timer cannot overwrite refreshed event or acknowledge while uncertain',async({page})=>{
 await setup(page);await page.clock.runFor(50);const before=await saved(page)
 await page.route('**/api/v1/games/'+id+'?**',r=>r.fulfill({status:503,json:{code:'UNAVAILABLE'}}))
 await page.clock.runFor(1);await page.getByRole('button',{name:'再同期',exact:true}).click();await expect(page.getByRole('alert')).toBeVisible()
 await page.clock.runFor(10000);await expect(page.getByTestId('next-event')).toBeDisabled();expect(await saved(page)).toBe(before)
})
