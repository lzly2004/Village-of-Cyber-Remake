import {test,expect} from '@playwright/test'
import {readFileSync,mkdirSync,writeFileSync} from 'node:fs'
import {createHash} from 'node:crypto'
import {publicSnapshot} from '../test-fixtures/public-players.js'
const replay=JSON.parse(readFileSync(new URL('../../src/test/resources/replay/exchange-v3.json',import.meta.url)))[0]
const key='vocr.guest.v1',id='12345678-1234-4234-8234-123456789050'
async function store(page){return page.evaluate(async()=>{const {createReplayStore}=await import('/src/replayStore.js');return createReplayStore().listSlots()})}
async function seed(page,count=1){await page.evaluate(async({replay,count})=>{const {createReplayStore}=await import('/src/replayStore.js');await createReplayStore().saveMany(Array.from({length:count},(_,i)=>({...replay,totalVillageCount:9+i})))},{replay,count})}
async function open(page){await page.getByTestId('title-replay').click();await expect(page.getByTestId('desktop-replay-browser')).toBeVisible();await expect(page.locator('[data-slot="0"]')).toBeEnabled()}
async function capture(page,name){await expect(page.locator('.desktop-replay-browser [data-image-state="loading"],.desktop-replay-browser [data-image-state="waiting"]')).toHaveCount(0);const p=new URL('../../build/verify/desktop-replay/',import.meta.url);mkdirSync(p,{recursive:true});const b=await page.getByTestId('desktop-replay').screenshot({path:new URL(name+'.png',p).pathname});writeFileSync(new URL(name+'.json',p),JSON.stringify({source:'real browser rendering of fixed legitimate Replay fixture; not user playtest',bytes:b.length,sha256:createHash('sha256').update(b).digest('hex')},null,2))}

test('source three-slot pages, file buttons and coordinates; extra browser management stays outside the canvas',async({page})=>{
 await page.goto('/');await seed(page,4);const calls=[];page.on('request',r=>{if(new URL(r.url()).pathname.startsWith('/api/'))calls.push(r.method())});await open(page)
 await expect(page.locator('.desktop-save-slot')).toHaveCount(3)
 await expect(page.locator('[data-slot="0"]')).toContainText('簡易村')
 // ReplayBrowserHandler.getPeiyiColor indexes directly by ordinal, not enum name.
 await expect(page.locator('[data-slot="0"] .desktop-save-color')).toHaveCSS('background-color','rgb(93, 174, 90)')
 await expect(page.locator('.replay-browser-management')).not.toHaveAttribute('open','')
 for(const width of [1440,1024,800]){
  await page.setViewportSize({width,height:1000})
  const a=await page.getByTestId('desktop-replay-browser').boundingBox(),b=await page.locator('[data-slot="0"]').boundingBox(),next=await page.getByRole('button',{name:'次の保存枠'}).boundingBox()
  expect(Math.abs((b.x-a.x)/a.width*1280-90)).toBeLessThan(1);expect(Math.abs((b.y-a.y)/a.height*720-80)).toBeLessThan(1)
  expect(Math.abs(b.width/a.width*1280-770)).toBeLessThan(1);expect(Math.abs((next.x-a.x)/a.width*1280-1050)).toBeLessThan(1)
  const card=await page.locator('[data-slot="0"] .desktop-save-frame img').boundingBox()
  expect(Math.abs((card.x-b.x)/a.width*1280-12)).toBeLessThan(1);expect(Math.abs(card.width/a.width*1280-758)).toBeLessThan(1)
  for(const [name,x] of [['import',90],['export',300]]){
   const button=await page.getByTestId('desktop-replay-'+name).boundingBox()
   expect(Math.abs((button.x-a.x)/a.width*1280-x)).toBeLessThan(1);expect(Math.abs((button.y-a.y)/a.height*720-20)).toBeLessThan(1)
   expect(Math.abs(button.width/a.width*1280-190)).toBeLessThan(1);expect(Math.abs(button.height/a.height*720-40)).toBeLessThan(1)
  }
  await capture(page,'library-'+width)
 }
 await page.getByRole('button',{name:'次の保存枠'}).click();await expect(page.locator('[data-slot="3"]')).toContainText('簡易村');await expect(page.locator('[data-slot="4"]')).toContainText('空槽位');await expect(page.locator('[data-slot="4"] .desktop-save-color')).toHaveCSS('background-color','rgb(85, 85, 85)')
 await page.getByRole('button',{name:'前の保存枠'}).click();await page.getByTestId('replay-return').click();await expect(page.getByTestId('desktop-title')).toBeVisible();expect(calls).toEqual([])
})

test('desktop replay day/vote/role scenes and return are local; round lock and source navigation preserved',async({page})=>{
 await page.goto('/');await seed(page);await open(page);await capture(page,'library-one');const before=await store(page),writes=[];page.on('request',r=>{if(new URL(r.url()).pathname.startsWith('/api/'))writes.push(r.method())})
 await page.locator('[data-slot="0"]').click();const player=page.getByTestId('desktop-replay-player')
 await expect(player).toBeVisible();await expect(page.getByTestId('replay-normal-info')).toContainText('[占い師]');await capture(page,'player-day2')
 await expect(player.locator('.replay-player-card')).toHaveCount(replay.daySnapshots[0].players.length)
 await expect(player.locator('.replay-player-card').first()).toHaveAttribute('data-player-index','1')
 await page.getByRole('button',{name:'投票結果',exact:true}).click();await expect(page.getByTestId('replay-votes')).toContainText('投票先→');await capture(page,'player-day2-votes')
 await page.getByRole('button',{name:'次の投票回'}).click();await expect(page.getByRole('button',{name:'次の日',exact:true})).toBeDisabled()
 await page.getByRole('button',{name:'情報・技能履歴',exact:true}).click();await expect(page.getByRole('button',{name:'次の日',exact:true})).toBeEnabled()
 await page.getByRole('button',{name:'次の日',exact:true}).click();await page.getByRole('button',{name:'次の日',exact:true}).click()
 await expect(page.getByTestId('replay-role-groups')).toContainText('一役職一');await expect(page.getByTestId('replay-normal-info')).toHaveCount(0)
 await capture(page,'player-final')
 await page.getByRole('button',{name:'Replayを閉じる',exact:true}).click();await expect(page.getByTestId('desktop-replay-browser')).toBeVisible()
 expect(await store(page)).toEqual(before);expect(writes).toEqual([])
})

async function finished(page){
 const s=publicSnapshot();s.phase='FINISHED';s.view.result='VILLAGE_WIN';s.events=[];s.presentedEvents=[];s.eventCursor=0;s.nextEventCursor=0;s.revealedRoles=s.view.players.map(p=>({playerNumber:p.playerNumber,actualRole:'wu'}))
 await page.addInitScript(({id,key})=>sessionStorage.setItem(key,JSON.stringify({sessionId:id,eventCursor:0})),{id,key})
 await page.route('**/api/v1/games/'+id+'?**',r=>r.fulfill({json:s}))
 await page.route('**/api/v1/games/'+id+'/replay?**',r=>r.fulfill({json:[replay,...Array(8).fill(null)]}))
 await page.goto('/');await expect(page.getByTestId('settlement-board')).toBeVisible();await page.getByTestId('settlement-board').getByRole('button',{name:'タイトルに戻る'}).click();await open(page)
}
test('slot save and replacement require confirmation; cancel is zero request and existing recording stays unchanged',async({page})=>{
 await finished(page);await page.getByRole('button',{name:'次の保存枠',exact:true}).click();await page.getByRole('button',{name:'次の保存枠',exact:true}).click();let reads=0;page.on('request',r=>{if(r.url().includes('/replay?'))reads++})
 await page.locator('[data-slot="6"]').click({button:'right'});await expect(page.getByRole('dialog',{name:'保存確認'})).toBeVisible()
 await page.getByRole('button',{name:'キャンセル',exact:true}).click();expect(reads).toBe(0);expect((await store(page)).filter(Boolean)).toHaveLength(0)
 await page.locator('[data-slot="6"]').click({button:'right'});await page.getByRole('button',{name:'保存を確定',exact:true}).click()
 await expect.poll(async()=>(await store(page))[6]?.replay.totalDays).toBe(4);expect(reads).toBe(1)
 const before=await store(page)
 await page.locator('[data-slot="6"]').click({button:'right'});await expect(page.getByRole('dialog',{name:'保存確認'})).toContainText('置き換え')
 await page.keyboard.press('Escape');expect(await store(page)).toEqual(before);expect(reads).toBe(1)
})


test('44 native Replay portraits, vote text and last person remain keyboard reachable at narrow widths',async({page})=>{
 const large=structuredClone(replay);large.peiyiName='daxing';large.peiyiOrdinal=7
 for(const s of large.daySnapshots){
  const sample=s.players[0]
  s.aliveCount=44;s.players=Array.from({length:44},(_,i)=>({...sample,playerIndex:i+1,characterNumber:i+1,actualRole:6,claimedRole:0,comingOutDay:-1,claimedRoleOrder:0,skillTarget:0,skillTargets:[],deathDay:0,deathReason:0,nonHumanMarked:false}))
  s.greyTargetCharNums=[];s.designatedTargetCharNums=[]
  if(s.dayNumber===2){s.dailyVotingRule=0;s.voteRounds=[{round:1,votes:s.players.map(p=>[p.playerIndex,44])}]}
 }
 await page.goto('/');await page.evaluate(async r=>{const{createReplayStore}=await import('/src/replayStore.js');await createReplayStore().save(r)},large);await open(page)
 await page.locator('[data-slot="0"]').click();const player=page.getByTestId('desktop-replay-player')
 await expect(player.locator('.replay-player-card')).toHaveCount(44)
 for(const width of [1440,1024,800]){
  await page.setViewportSize({width,height:1000})
  const last=player.locator('.replay-player-card').last();await last.focus();await expect(last).toBeFocused()
  expect(await last.evaluate(el=>{const a=el.getBoundingClientRect(),b=el.parentElement.getBoundingClientRect();return a.left>=b.left-1&&a.right<=b.right+1})).toBe(true)
  expect(await player.evaluate(el=>el.scrollWidth<=el.clientWidth+1)).toBe(true)
 }
 await page.getByRole('button',{name:'投票結果',exact:true}).click();await expect(page.getByTestId('replay-votes')).toContainText('音楽：44票')
 await page.keyboard.press('Escape');await expect(page.locator('[data-slot="0"]')).toBeFocused()
})

async function importNative(page,value){
 const chooser=page.waitForEvent('filechooser')
 await page.getByTestId('desktop-replay-import').click()
 await(await chooser).setFiles({name:'recording.json',mimeType:'application/json',buffer:Buffer.from(JSON.stringify([value,...Array(8).fill(null)]))})
 const modal=page.getByRole('dialog',{name:'録画ファイル交換',exact:true})
 await expect(modal).toBeVisible();return modal
}
async function nativeTarget(modal,index){
 await modal.getByRole('button',{name:'保存枠を選ぶ',exact:true}).click()
 await modal.getByRole('combobox',{name:'保存先',exact:true}).selectOption(String(index))
 await modal.getByRole('button',{name:'内容を確認',exact:true}).click()
}
test('source file buttons import through preview, target and confirmation; cancel writes nothing, single export preserves recording and slot',async({page})=>{
 await page.goto('/');await open(page)
 const calls=[];page.on('request',r=>{if(new URL(r.url()).pathname.startsWith('/api/'))calls.push(r.method())})
 await page.getByTestId('desktop-replay-export').click();await expect(page.getByRole('dialog',{name:'録画ファイル交換'})).toContainText('書き出せる録画はありません')
 await page.getByRole('button',{name:'閉じる',exact:true}).click()
 let modal=await importNative(page,replay);await nativeTarget(modal,8);await modal.getByRole('button',{name:'キャンセル',exact:true}).click()
 expect((await store(page)).filter(Boolean)).toEqual([])
 modal=await importNative(page,replay);await nativeTarget(modal,8);await modal.getByRole('button',{name:'書き込みを確定',exact:true}).click()
 await expect(modal).toHaveCount(0);await expect.poll(async()=>(await store(page))[8]?.replay).toEqual(replay)
 const before=await store(page)
 await page.getByTestId('desktop-replay-export').click();await page.getByRole('combobox',{name:'書き出す録画',exact:true}).selectOption('8')
 const download=page.waitForEvent('download');await page.getByRole('button',{name:'ファイルを書き出す',exact:true}).click()
 const bytes=readFileSync(await(await download).path()),file=JSON.parse(bytes)
 expect(file[0]).toEqual(replay);expect(file.slice(1)).toEqual(Array(8).fill(null));expect(await store(page)).toEqual(before);expect(calls).toEqual([])
})

test('source file import quota rejection cannot overwrite the selected slot; explicit retry after failure commits once',async({page})=>{
 await page.goto('/');await seed(page);await open(page);const before=await store(page)
 const incoming={...replay,totalVillageCount:198}
 let modal=await importNative(page,incoming);await nativeTarget(modal,0);await modal.getByRole('button',{name:'キャンセル',exact:true}).click();expect(await store(page)).toEqual(before)
 modal=await importNative(page,incoming);await nativeTarget(modal,0)
 await page.evaluate(()=>{window.replayOriginalAdd=IDBObjectStore.prototype.add;IDBObjectStore.prototype.add=function(){if(this.name==='recordings')throw new DOMException('controlled quota','QuotaExceededError');return window.replayOriginalAdd.apply(this,arguments)}})
 await modal.getByRole('button',{name:'書き込みを確定',exact:true}).click();await expect(modal.getByRole('alert')).toContainText('QUOTA_EXCEEDED');expect(await store(page)).toEqual(before)
 await page.evaluate(()=>{IDBObjectStore.prototype.add=window.replayOriginalAdd;delete window.replayOriginalAdd})
 await modal.getByRole('button',{name:'書き込みを確定',exact:true}).click();await expect(modal).toHaveCount(0)
 await expect.poll(async()=>(await store(page))[0]?.replay).toEqual(incoming);expect((await store(page)).filter(Boolean)).toHaveLength(1)
})

test('Replay accessible names follow visible CO before terminal and use Japanese terminal roles',async({page})=>{
 await page.goto('/');await seed(page);await open(page);await page.locator('[data-slot="0"]').click()
 const cards=page.locator('.replay-player-card');await expect(cards).toHaveCount(replay.daySnapshots[0].players.length)
 const roles=['未CO','占い師','霊能者','狩人','共有者','猫又','村人','人狼','狂人','狂信者','妖狐','背徳者']
 const claims=await cards.evaluateAll(nodes=>nodes.map(n=>n.getAttribute('aria-label')))
 // No exact true-role sentence on an ordinary date; original colored portraits remain unchanged.
 expect(claims.every(t=>/（公称職業：.+）$/.test(t)&&!t.includes('真の職業'))).toBe(true)
 for(let d=2;d<replay.totalDays;d++)await page.getByRole('button',{name:'次の日',exact:true}).click()
 const last=replay.daySnapshots.find(s=>s.dayNumber===replay.totalDays)||replay.daySnapshots.at(-1)
 for(const p of last.players){await expect(page.locator('.replay-player-card[data-player-index="'+p.playerIndex+'"]')).toHaveAttribute('aria-label',new RegExp('真の職業：'+roles[p.actualRole]))}
 await expect(page.getByTestId('replay-role-groups')).not.toContainText('占卜师')
 await expect(page.getByTestId('replay-role-groups')).not.toContainText('猎人')
})
