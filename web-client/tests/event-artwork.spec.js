import {test,expect} from '../test-fixtures/presentation-test.js'
import {readFileSync} from 'node:fs'
import {publicSnapshot} from '../test-fixtures/public-players.js'
const mapping=JSON.parse(readFileSync(new URL('../../src/main/resources/config/event_image_mapping.json',import.meta.url),'utf8'))
const manifest=JSON.parse(readFileSync(new URL('../public/assets/images/manifest.json',import.meta.url),'utf8'))
const key='vocr.guest.v1'
const e=(eventname,ch1='Abel',ch2='Betti')=>({eventname,ch1,ch2,ch3:'Helmut'})
const presented=(event,stage='DAY_DIALOGUE')=>({event,stage})
const source=event=>{const entry=mapping[event.ch1][event.eventname];return Object.hasOwn(entry.special||{},event.ch2)?entry.special[event.ch2]:entry.default}
const url=name=>'/assets/images/'+manifest.images['images/'+name].file
const saved=page=>page.evaluate(key=>JSON.parse(sessionStorage.getItem(key)),key)
async function synthetic(page,request,list,cursor=0,prefixHook=null){
 const created=await(await request.post('/api/v1/games',{data:{configuration:'tongchang'}})).json()
 const snapshot=publicSnapshot();snapshot.events=list.map(p=>p.event);snapshot.presentedEvents=list;snapshot.nextEventCursor=list.length
 await page.route('**/api/v1/games/'+created.sessionId+'?**',async route=>{
  const position=Number(new URL(route.request().url()).searchParams.get('eventCursor')||0)
  if(position===0&&prefixHook)await prefixHook(route,snapshot)
  else await route.fulfill({json:{...snapshot,eventCursor:position,events:snapshot.events.slice(position),presentedEvents:list.slice(position)}})
 })
 await page.addInitScript(({key,id,cursor})=>{if(!sessionStorage.getItem(key))sessionStorage.setItem(key,JSON.stringify({sessionId:id,eventCursor:cursor}))},{key,id:created.sessionId,cursor})
 await page.goto('/');await expect(page.getByTestId('next-event')).toBeVisible()
 return created.sessionId
}
async function ready(page){await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-ready','true');await expect(page.getByTestId('decoded-stage-scene')).toHaveCount(1)}
async function characters(page){return page.getByTestId('stage-character').evaluateAll(nodes=>nodes.map(n=>({name:n.dataset.source,x:Number(n.getAttribute('x')),y:Number(n.getAttribute('y')),width:Number(n.getAttribute('width')),height:Number(n.getAttribute('height'))})).sort((a,b)=>a.x-b.x))}
function expected(name,x=null){const entry=manifest.images['images/'+name];return{name:'images/'+name,x:x??(1280-entry.size[0])/2,y:690-entry.size[1],width:entry.size[0],height:entry.size[1]}}

test('WEB-024 exact special, previous-left/current-right and triple swap use source artwork, not ch2/ch3 portraits',async({page,request},testInfo)=>{
 const list=[presented(e('zco7')),presented(e('gyfo1')),presented(e('gyfo1r','Betti','Abel')),
  presented(e('zjgh8b')),presented(e('gprz11r','Betti','Abel')),presented(e('gprz11p','Erich','Betti')),presented(e('lrco'))]
 const id=await synthetic(page,request,list)
 let writes=0;page.on('request',r=>{if(['POST','PUT','DELETE'].includes(r.method()))writes++})
 try{
  const plans=[[expected(source(list[0].event))],[expected(source(list[1].event),300)],
   [expected(source(list[1].event),300),expected(source(list[2].event),650)],
   [expected(source(list[3].event),300)],
   [expected(source(list[3].event),300),expected(source(list[4].event),650)],
   [expected(source(list[5].event),300),expected(source(list[4].event),650)],
   [expected(source(list[6].event))]]
  for(let i=0;i<list.length;i++){
   await ready(page);expect(await characters(page)).toEqual(plans[i])
   await expect(page.getByTestId('stage-background')).toHaveAttribute('href',url('haikei3.png'))
   await expect(page.getByTestId('stage-art-status')).toHaveCount(0)
   if([0,2,5].includes(i))await page.getByTestId('stage-surface').screenshot({path:testInfo.outputPath('composition-'+i+'.png')})
   expect((await saved(page)).eventCursor).toBe(i)
   await page.getByTestId('next-event').click()
  }
  expect(writes).toBe(0)
 }finally{await request.delete('/api/v1/games/'+id)}
})

test('WEB-024 actual API event images preserve geometry across widths and refresh without changing dialogue/cursor',async({page,request})=>{
 await page.goto('/');const created=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/api/v1/games'))
 await page.getByRole('button',{name:'対局を始める'}).click();const data=await(await created).json()
 try{
  let cursor=0
  while(data.update.presentedEvents[cursor]?.stage!=='DAY_DIALOGUE'){
   expect(cursor).toBeLessThan(data.update.presentedEvents.length-1);await page.getByTestId('next-event').click();cursor++
  }
  await ready(page);const before=await characters(page);const state=await saved(page);const text=await page.getByTestId('event-dialogue').textContent()
  expect(before.some(a=>a.name==='images/'+source(data.update.presentedEvents[cursor].event))).toBe(true)
  for(const width of [1920,1440,1280,1024]){
   await page.setViewportSize({width,height:900});expect(await characters(page)).toEqual(before)
   const rect=await page.getByTestId('stage-surface').boundingBox();expect(Math.abs(rect.width/rect.height-16/9)).toBeLessThan(.005)
   await expect(page.getByTestId('event-dialogue')).toHaveText(text)
  }
  await page.reload();await ready(page);await expect(page.getByTestId('stage-art-status')).toHaveCount(0)
  expect(await characters(page)).toEqual(before);expect(await saved(page)).toEqual(state)
 }finally{await request.delete('/api/v1/games/'+data.sessionId)}
})

test('WEB-024 reload at paired cursor supplements read-only prefix and reproduces cached left without extra storage',async({page,request})=>{
 let prefix=0
 const list=[presented(e('gyfo1')),presented(e('gyfo1r','Betti','Abel'))]
 const id=await synthetic(page,request,list,1,async(route,snapshot)=>{prefix++;await route.fulfill({json:snapshot})})
 let writes=0;page.on('request',r=>{if(['POST','PUT','DELETE'].includes(r.method()))writes++})
 try{
  await ready(page);await expect(page.getByTestId('stage-art-status')).toHaveCount(0)
  const before=await characters(page);expect(before).toHaveLength(2);expect(prefix).toBe(1)
  await page.reload();await ready(page);await expect(page.getByTestId('stage-art-status')).toHaveCount(0)
  expect(await characters(page)).toEqual(before);expect(prefix).toBe(2);expect(writes).toBe(0)
  expect(Object.keys(await saved(page)).sort()).toEqual(['eventCursor','sessionId'])
  expect((await saved(page)).eventCursor).toBe(1)
 }finally{await request.delete('/api/v1/games/'+id)}
})

test('WEB-024 unavailable or late prefix cannot replace authoritative state or rewind a newly confirmed event',async({page,request})=>{
 let release;const blocked=new Promise(resolve=>{release=resolve})
 const list=[presented(e('gyfo1')),presented(e('gyfo1r','Betti','Abel')),presented(e('lrco','Erich'))]
 const id=await synthetic(page,request,list,1,async(route,snapshot)=>{await blocked;await route.fulfill({json:snapshot})})
 try{
  await expect(page.getByTestId('stage-art-status')).toContainText('表示情報が不足')
  await page.getByTestId('next-event').click();expect((await saved(page)).eventCursor).toBe(2)
  const text=await page.getByTestId('event-dialogue').textContent();release()
  await ready(page);await expect(page.getByTestId('stage-art-status')).toHaveCount(0)
  expect((await saved(page)).eventCursor).toBe(2);await expect(page.getByTestId('event-dialogue')).toHaveText(text)
  expect(await characters(page)).toEqual([expected(source(list[2].event))])
 }finally{release();await request.delete('/api/v1/games/'+id)}
})

test('WEB-024 afternoon and025 stages use source backgrounds without hidden role portraits',async({page,request})=>{
 const list=[presented(e('hblr'),'AFTERNOON_DIALOGUE'),presented(e('wsw','NONE'),'NIGHT_RESULT'),
  presented(e('yjsw'),'NIGHT_RESULT'),presented(e('cxs'),'EXECUTION'),presented(e('crsl'),'SETTLEMENT')]
 const id=await synthetic(page,request,list)
 try{
  await ready(page);await expect(page.getByTestId('stage-background')).toHaveAttribute('href',url('haikei.png'))
  for(let i=1;i<list.length;i++){
   await page.getByTestId('next-event').click();await ready(page)
   await expect(page.getByTestId('stage-character')).toHaveCount(i===1?0:1)
   await expect(page.locator('.player-identity').first()).not.toContainText('実際の役職')
  }
 }finally{await request.delete('/api/v1/games/'+id)}
})

test('WEB-024 cold decode commits pair atomically; warm remount first8 frames have no blank group',async({page,request},testInfo)=>{
 const list=[presented(e('gyfo1')),presented(e('gyfo1r','Betti','Abel')),presented(e('gyfo1')),presented(e('gyfo1r','Betti','Abel'))]
 const second=source(list[1].event);let release;const blocked=new Promise(resolve=>{release=resolve});let requests=0
 await page.route('**'+url(second),async route=>{requests++;await blocked;await route.continue()})
 const id=await synthetic(page,request,list)
 async function monitor(cursor){await page.evaluate(cursor=>{
  window.artFrames=[];window.artStop=false
  const sample=()=>{const state=JSON.parse(sessionStorage.getItem('vocr.guest.v1'));if(state.eventCursor===cursor){const art=document.querySelector('[data-testid="stage-artwork"]');window.artFrames.push({ready:art?.dataset.artReady,count:document.querySelectorAll('[data-testid="stage-character"]').length,status:Boolean(document.querySelector('[data-testid="stage-art-status"]'))})}if(!window.artStop)requestAnimationFrame(sample)}
  requestAnimationFrame(sample)
 },cursor)}
 try{
  await ready(page);await monitor(1);await page.getByTestId('next-event').click()
  await expect.poll(()=>page.evaluate(()=>window.artFrames.length)).toBeGreaterThanOrEqual(8)
  const cold=await page.evaluate(()=>{window.artStop=true;return window.artFrames})
  expect(cold.every(f=>f.count===0&&f.status&&f.ready==='false')).toBe(true)
  release();await ready(page);await expect(page.getByTestId('stage-character')).toHaveCount(2)
  await monitor(2);await page.getByTestId('next-event').click()
  await expect.poll(()=>page.evaluate(()=>window.artFrames.length)).toBeGreaterThanOrEqual(8)
  const warm=await page.evaluate(()=>{window.artStop=true;return window.artFrames.slice(0,8)})
  expect(warm.every(f=>f.ready==='true'&&f.count===1&&!f.status)).toBe(true)
  expect(requests).toBe(1)
  await testInfo.attach('decode-frames.json',{contentType:'application/json',body:JSON.stringify({coldFrames:cold.length,coldPartial:0,warmFrames:warm.length,warmBlank:0})})
 }finally{release();await request.delete('/api/v1/games/'+id)}
})

test('WEB-024 corrupt image and failed context show diagnostics while reading/confirmation remain usable',async({page,request})=>{
 const list=[presented(e('zco7')),presented(e('lrco'))]
 await page.route('**'+url(source(list[0].event)),route=>route.fulfill({status:200,contentType:'image/webp',body:'not an image'}))
 const id=await synthetic(page,request,list)
 try{
  await expect(page.getByTestId('stage-art-status')).toContainText('読み込めません')
  await expect(page.getByTestId('decoded-stage-scene')).toHaveCount(0)
  await expect(page.getByTestId('event-dialogue')).toBeVisible()
  await page.getByTestId('next-event').click();await ready(page)
  expect((await saved(page)).eventCursor).toBe(1)
 }finally{await request.delete('/api/v1/games/'+id)}
})


for (const failure of ['unavailable','revision-mismatch']) {
 test('WEB-024 '+failure+' prefix is diagnostic only and cannot update authority or disable decision',async({page,request})=>{
  const list=[presented(e('gyfo1')),presented(e('gyfo1r','Betti','Abel'))]
  const id=await synthetic(page,request,list,1,async(route,snapshot)=>{
   if(failure==='unavailable')await route.fulfill({status:503,body:'unavailable'})
   else await route.fulfill({json:{...snapshot,revision:snapshot.revision+1,view:{...snapshot.view,gameDay:50}}})
  })
  try{
   await ready(page);await expect(page.getByTestId('stage-art-status')).toContainText('表示情報が不足')
   expect(await characters(page)).toHaveLength(1)
   await expect(page.getByRole('heading',{level:1})).toContainText('第 1 日')
   await page.getByTestId('next-event').click()
   await expect(page.getByRole('switch',{name:'投票回避CO'})).toBeEnabled()
   expect((await saved(page)).eventCursor).toBe(2)
   await expect(page.locator('.notice[role="alert"]')).toHaveCount(0)
  }finally{await request.delete('/api/v1/games/'+id)}
 })
}
