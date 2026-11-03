import {test,expect} from '@playwright/test'
import {publicSnapshot} from '../test-fixtures/public-players.js'
import {startDesktopGame} from '../test-fixtures/desktop-start.js'
import {mkdirSync,writeFileSync} from 'node:fs'
import {createHash} from 'node:crypto'
const id='00000000-0000-4000-8000-000000000052'
async function mock(page,{finished=false}={}){
 const snapshot=publicSnapshot();snapshot.view.gameDay=3
 snapshot.claimInquiry={roles:['zhan','ling','lie','gong','mao'],players:[1,2]}
 if(finished){snapshot.phase='FINISHED';snapshot.view.result='VILLAGE_WIN';snapshot.revealedRoles=snapshot.view.players.map(p=>({playerNumber:p.playerNumber,role:'wu'}))}
 const calls=[]
 await page.route('**/api/v1/games',async route=>{calls.push({method:route.request().method(),body:route.request().postDataJSON()});await route.fulfill({status:201,json:{sessionId:id,update:snapshot}})})
 await page.route('**/api/v1/games/'+id,async route=>{calls.push({method:route.request().method()});await route.fulfill({status:204})})
 await page.route('**/api/v1/games/'+id+'?**',route=>route.fulfill({json:snapshot}))
 return {snapshot,calls}
}
async function claims(page){await page.getByRole('button',{name:'指示する',exact:true}).click();await page.getByRole('button',{name:'COを聞く',exact:true}).click()}
async function saved(page){return page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))}
async function shot(page,name){const b=await page.locator('.desktop-menu-journal').screenshot();const dir='build/verify/desktop-scene-loop';mkdirSync('../'+dir,{recursive:true});writeFileSync('../'+dir+'/'+name+'.png',b);writeFileSync('../'+dir+'/'+name+'.json',JSON.stringify({sha256:createHash('sha256').update(b).digest('hex'),bytes:b.length,source:'browser render; user visual acceptance pending'}))}

test('scene-title: source menu geometry, insertion order, noop and select never create a session',async({page})=>{
 const {calls}=await mock(page);await page.goto('/')
 const title=page.getByTestId('desktop-title');await expect(title).toBeVisible()
 expect(await title.locator('button').evaluateAll(nodes=>nodes.map(n=>n.dataset.testid))).toEqual(['title-start','title-continue','title-replay','title-records','title-help','title-collections'])
 await page.getByTestId('title-continue').click();await page.getByTestId('title-collections').click();await expect(title).toBeVisible()
 for(const width of [1440,1024,800]){
  await page.setViewportSize({width,height:1000});const surface=await title.boundingBox(),button=await page.getByTestId('title-start').boundingBox()
  expect(Math.abs((button.x-surface.x)/surface.width-660/1280)).toBeLessThan(.001)
  expect(Math.abs(button.width/surface.width-309/1280)).toBeLessThan(.001)
  expect(Math.abs((button.y-surface.y)/surface.height-400/720)).toBeLessThan(.001)
  await shot(page,'title-'+width)
 }
 await page.getByTestId('title-start').focus();await page.keyboard.press('Tab');await expect(page.getByTestId('title-continue')).toBeFocused()
 await page.getByTestId('title-start').click();await expect(page.getByTestId('desktop-select')).toBeVisible();await expect(page.locator('.desktop-setup-button')).toHaveCount(7)
 const surface=await page.getByTestId('desktop-select').boundingBox(),last=await page.getByTestId('setup-daxing').boundingBox()
 expect(Math.abs((last.x-surface.x)/surface.width-495/1280)).toBeLessThan(.001)
 expect(Math.abs((last.y-surface.y)/surface.height-356/720)).toBeLessThan(.001)
 const back=await page.getByTestId('setup-return').boundingBox(),panel=await page.locator('.desktop-menu-journal').boundingBox()
 expect(Math.abs(back.width/surface.width-116/1280)).toBeLessThan(.0001)
 expect(Math.abs(back.height/surface.height-76/720)).toBeLessThan(.0001)
 expect(back.x+back.width).toBeLessThanOrEqual(panel.x+panel.width)
 expect(await page.locator('.desktop-menu-journal').evaluate(n=>n.scrollWidth<=n.clientWidth)).toBe(true)
 await shot(page,'configuration');await page.getByTestId('setup-return').click();expect(calls).toEqual([]);expect(await saved(page)).toBeNull()
})

test('scene-entry-return: replay, record and help return to the same title with no session commands',async({page})=>{
 const {calls}=await mock(page);await page.goto('/')
 await page.getByTestId('title-replay').click();await expect(page.getByTestId('desktop-replay')).toBeVisible();await expect(page.locator('#desktop-replay-slot .replay-library')).toBeVisible()
 await page.getByTestId('replay-return').click();await expect(page.getByTestId('desktop-title')).toBeVisible()
 await page.getByTestId('title-records').click();await expect(page.getByTestId('desktop-records')).toBeVisible();await page.keyboard.press('Escape')
 await page.getByTestId('title-help').click();await expect(page.getByTestId('desktop-help')).toBeVisible();await page.getByTestId('help-title-return').click()
 await expect(page.getByTestId('desktop-title')).toBeVisible();expect(calls).toEqual([]);expect(await saved(page)).toBeNull()
})

for(const [role,label] of [['zhan','占い師'],['ling','霊能者'],['lie','狩人'],['gong','共有'],['mao','猫又']])test('scene-claim-role-empty-'+role+': confirmed empty response has source nonblocking one-second tip',async({page})=>{
 const {snapshot}=await mock(page);let writes=0
 await page.route('**/claims/by-role',async route=>{writes++;expect(route.request().postDataJSON()).toEqual({expectedRevision:1,role});await route.fulfill({json:snapshot})})
 await page.goto('/');await startDesktopGame(page);await claims(page);const before=await saved(page)
 await page.clock.install();await page.clock.pauseAt(new Date(Date.now()+1000));await page.getByRole('button',{name:label+'のCOを聞く',exact:true}).click()
 const tip=page.locator('.desktop-transient-tip');await expect(tip).toHaveText(label+'はいないようだ')
 const alpha=await tip.evaluate(n=>Number(getComputedStyle(n).backgroundColor.match(/[\d.]+/g).at(-1)))
 expect(Math.abs(alpha-180/255)).toBeLessThan(.002)
 await expect(page.getByRole('button',{name:'戻る',exact:true})).toBeEnabled();expect(await saved(page)).toBe(before);expect(writes).toBe(1)
 await page.clock.runFor(999);await expect(tip).toBeVisible();await page.clock.runFor(1);await expect(tip).toHaveCount(0)
})

test('scene-claim-events: real new dialogue stays pending and receives no empty tip',async({page})=>{
 const {snapshot}=await mock(page);const event={stage:'DAY_DIALOGUE',event:{eventname:'zco7',ch1:'Abel',ch2:'NONE',ch3:'NONE'}}
 await page.route('**/claims/by-role',route=>route.fulfill({json:{...snapshot,revision:2,events:[event.event],presentedEvents:[event],nextEventCursor:1}}))
 await page.goto('/');await startDesktopGame(page);await claims(page);const before=await saved(page)
 await page.getByRole('button',{name:'占い師のCOを聞く',exact:true}).click();await expect(page.getByTestId('next-event')).toBeVisible()
 await expect(page.locator('.desktop-transient-tip')).toHaveCount(0);expect(await saved(page)).toBe(before)
})
for(const failure of ['network','rejected'])test('scene-claim-error-'+failure+': failure never reports nobody CO and preserves credential',async({page})=>{
 await mock(page);await page.route('**/claims/by-role',route=>failure==='network'?route.abort('failed'):route.fulfill({status:400,json:{code:'INVALID_COMMAND',message:'request rejected'}}))
 await page.goto('/');await startDesktopGame(page);await claims(page);const before=await saved(page)
 await page.getByRole('button',{name:'占い師のCOを聞く',exact:true}).click();await expect(page.getByRole('alert')).toBeVisible();await expect(page.locator('.desktop-transient-tip')).toHaveCount(0);expect(await saved(page)).toBe(before)
})

test('scene-claim-players-empty: selected inquiry sends once and source 無人CO appears on return',async({page})=>{
 const {snapshot}=await mock(page);let writes=0;await page.route('**/claims/by-players',route=>{writes++;return route.fulfill({json:snapshot})})
 await page.goto('/');await startDesktopGame(page);await claims(page);await page.getByRole('button',{name:'プレイヤーのCOを聞く',exact:true}).click()
 await page.locator('.desktop-target').first().click();const before=await saved(page);await page.getByRole('button',{name:'戻る',exact:true}).click()
 await expect(page.locator('.desktop-transient-tip')).toHaveText('無人CO');expect(writes).toBe(1);expect(await saved(page)).toBe(before)
})

test('scene-live-title: no confirmation or writes; local menu roundtrip preserves game and cursor',async({page})=>{
 const {calls}=await mock(page);await page.goto('/');await startDesktopGame(page);await expect(page.getByTestId('desktop-vote-window')).toBeVisible();const before=await saved(page)
 await page.getByRole('button',{name:'メニュー',exact:true}).click();await expect(page.getByTestId('desktop-title')).toBeVisible();await expect(page.locator('.exit-confirm')).toHaveCount(0)
 await page.getByTestId('title-records').click();await page.keyboard.press('Escape');await page.getByTestId('resume-current-game').click();await expect(page.getByTestId('desktop-vote-window')).toBeVisible()
 expect(calls.map(c=>c.method)).toEqual(['POST']);expect(await saved(page)).toBe(before)
})

test('scene-finished-title-second-game: source return and replacement preserve records with DELETE before new POST',async({page})=>{
 const {calls}=await mock(page,{finished:true});await page.goto('/');await startDesktopGame(page)
 await expect(page.getByRole('region',{name:'ローカル戦績保存'})).toContainText('保存しました')
 const before=await page.evaluate(async()=>{const{createRecordStore}=await import('/src/recordStore.js');return createRecordStore().read()})
 await page.locator('.settlement-board').getByRole('button',{name:'タイトルに戻る',exact:true}).click();await expect(page.getByTestId('desktop-title')).toBeVisible();await expect(page.locator('.exit-confirm')).toHaveCount(0)
 expect(calls.map(c=>c.method)).toEqual(['POST']);await startDesktopGame(page,'tongchang');await expect(page.locator('.settlement-board')).toBeVisible()
 expect(calls.map(c=>c.method)).toEqual(['POST','DELETE','POST']);expect(calls.at(-1).body).toEqual({configuration:'tongchang'})
 expect(await page.evaluate(async()=>{const{createRecordStore}=await import('/src/recordStore.js');return createRecordStore().read()})).toEqual(before)
})

test('scene-replacement-failure: a lost release response cannot start a new session',async({page})=>{
 const {calls}=await mock(page);await page.goto('/');await startDesktopGame(page);await expect(page.getByTestId('desktop-vote-window')).toBeVisible();const before=await saved(page)
 await page.getByRole('button',{name:'メニュー',exact:true}).click();await page.route('**/api/v1/games/'+id,route=>route.abort('failed'))
 await startDesktopGame(page,'tongchang');await expect(page.getByRole('alert')).toContainText('连接未能确认');expect(await saved(page)).toBe(before);expect(calls.map(c=>c.method)).toEqual(['POST'])
})


test('scene-uncertain-title: unknown read failure disables title rather than silently ignoring it',async({page})=>{
 await mock(page);await page.goto('/');await startDesktopGame(page);await expect(page.getByTestId('desktop-vote-window')).toBeVisible()
 const before=await saved(page)
 await page.route('**/api/v1/games/'+id+'?**',route=>route.abort('failed'))
 await page.getByRole('button',{name:'再同期',exact:true}).click();await expect(page.getByRole('alert')).toBeVisible()
 await expect(page.getByRole('button',{name:'メニュー',exact:true})).toBeDisabled()
 await expect(page.getByTestId('desktop-title')).toBeHidden()
 await page.getByRole('button',{name:'指示する',exact:true}).click()
 await page.getByRole('button',{name:'COを聞く',exact:true}).click()
 await expect(page.getByRole('button',{name:'占い師のCOを聞く',exact:true})).toBeDisabled()
 expect(await saved(page)).toBe(before)
})

test('scene-busy-title: in-flight CO disables title and cannot navigate or duplicate a command',async({page})=>{
 const {snapshot,calls}=await mock(page);let finish
 const held=new Promise(resolve=>{finish=resolve})
 await page.route('**/claims/by-role',async route=>{await held;await route.fulfill({json:snapshot})})
 await page.goto('/');await startDesktopGame(page);await claims(page)
 const sent=page.waitForRequest(r=>r.url().endsWith('/claims/by-role'))
 await page.getByRole('button',{name:'占い師のCOを聞く',exact:true}).click();await sent
 await expect(page.getByRole('button',{name:'メニュー',exact:true})).toBeDisabled();await expect(page.getByTestId('desktop-title')).toBeHidden()
 finish();await expect(page.locator('.desktop-transient-tip')).toHaveText('占い師はいないようだ');expect(calls.map(c=>c.method)).toEqual(['POST'])
})

for(const input of ['mouse','keyboard'])test('replacement-'+input+': delayed release and creation admit only one replacement',async({page})=>{
 const {snapshot,calls}=await mock(page);await page.goto('/');await startDesktopGame(page);await page.getByRole('button',{name:'メニュー',exact:true}).click();await page.getByTestId('title-start').click()
 let releaseDelete,releaseCreate;const deleted=new Promise(r=>{releaseDelete=r}),created=new Promise(r=>{releaseCreate=r});let deletes=0,posts=0
 await page.route('**/api/v1/games/'+id,async r=>{deletes++;await deleted;await r.fulfill({status:204})})
 await page.route('**/api/v1/games',async r=>{posts++;await created;await r.fulfill({status:201,json:{sessionId:'00000000-0000-4000-8000-000000000055',update:snapshot}})})
 const button=page.getByTestId('setup-tongchang')
 if(input==='mouse')await button.dblclick();else{await button.focus();await page.keyboard.press('Enter');await page.keyboard.press('Enter')}
 await expect.poll(()=>deletes).toBe(1);expect(posts).toBe(0);await expect(button).toBeDisabled()
 releaseDelete();await expect.poll(()=>posts).toBe(1);await expect(page.getByTestId('title-start')).toBeDisabled();releaseCreate();await expect(page.getByTestId('desktop-vote-window')).toBeVisible()
 expect(deletes).toBe(1);expect(posts).toBe(1);expect(calls[0].method).toBe('POST')
 await expect(page.getByRole('alert')).toHaveCount(0)
})
