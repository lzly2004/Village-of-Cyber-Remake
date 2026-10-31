import {desktopHome} from '../test-fixtures/desktop-navigation.js'
import { sceneTest as test, expect, completeLocalReading } from '../test-fixtures/presentation-test.js'
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { publicSnapshot } from '../test-fixtures/public-players.js'
const read = path => JSON.parse(readFileSync(new URL(path,import.meta.url),'utf8'))
const mapping = read('../../src/main/resources/config/event_image_mapping.json')
const manifest = read('../public/assets/images/manifest.json')
const url = name => '/assets/images/' + manifest.images['images/'+name].file
const key = 'vocr.guest.v1'
const saved = page => page.evaluate(key=>JSON.parse(sessionStorage.getItem(key)),key)
const p = (eventname,stage,ch1='Abel') => ({stage,event:{eventname,ch1,ch2:'NONE',ch3:'NONE'}})
// Keep the actual rendered core screenshots outside Playwright's transient
// test-results directory, which later gate suites reuse. CI includes build/verify.
async function captureCore(page,info,name) {
  const bytes=await page.getByTestId('stage-surface').screenshot({path:info.outputPath(name)})
  const folder=new URL('../../build/verify/desktop-core/',import.meta.url)
  mkdirSync(folder,{recursive:true});writeFileSync(new URL(name,folder),bytes)
  const file=new URL('screenshots.json',folder)
  let captures={}
  try{captures=JSON.parse(readFileSync(file,'utf8')).captures}catch(error){if(error.code!=='ENOENT')throw error}
  captures[name]={bytes:bytes.length,sha256:createHash('sha256').update(bytes).digest('hex')}
  writeFileSync(file,JSON.stringify({source:'Synthetic public snapshot rendered by the real browser; not a user playtest or pixel golden',captures},null,2)+'\n')
}
async function synthetic(page,request,list,edit=()=>{}) {
  const created=await(await request.post('/api/v1/games',{data:{configuration:'tongchang'}})).json()
  const snapshot=publicSnapshot()
  snapshot.view.players=snapshot.view.players.slice(0,3)
  snapshot.events=list.map(p=>p.event);snapshot.presentedEvents=list;snapshot.nextEventCursor=list.length
  edit(snapshot)
  await page.route('**/api/v1/games/'+created.sessionId+'?**',route=>{
    const cursor=Number(new URL(route.request().url()).searchParams.get('eventCursor')||0)
    return route.fulfill({json:{...snapshot,eventCursor:cursor,events:snapshot.events.slice(cursor),presentedEvents:list.slice(cursor)}})
  })
  await page.addInitScript(({key,id})=>{if(!sessionStorage.getItem(key))sessionStorage.setItem(key,JSON.stringify({sessionId:id,eventCursor:0}))},{key,id:created.sessionId})
  await page.goto('/');await expect(page.getByTestId('game-stage')).toHaveAttribute('data-presentation-phase','event')
  return created.sessionId
}
async function ready(page) {
  await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-ready','true')
  await expect(page.getByTestId('decoded-stage-scene')).toHaveCount(1)
}
async function image(page,name) {
  await expect(page.getByTestId('stage-character')).toHaveCount(1)
  await expect(page.getByTestId('stage-character')).toHaveAttribute('data-source','images/'+name)
  await expect(page.getByTestId('stage-character')).toHaveAttribute('href',url(name))
  const size=manifest.images['images/'+name].size
  await expect(page.getByTestId('stage-character')).toHaveAttribute('x',String((1280-size[0])/2))
  await expect(page.getByTestId('stage-character')).toHaveAttribute('y',String(690-size[1]))
}
function terminal(snapshot,result='WOLF_WIN') {
  snapshot.phase='FINISHED';snapshot.view.result=result
  snapshot.revealedRoles=[{playerNumber:3,actualRole:'yaohu'},{playerNumber:1,actualRole:'renlang'},{playerNumber:2,actualRole:'mao'}]
  snapshot.view.players[1].alive=false;snapshot.view.players[1].deathReason='chuxing';snapshot.view.players[1].deathDay=1
}

test('WEB-025 night/death/execution frames follow ordered source, centered with source narratives; transitions never acknowledge',async({page,request},info)=>{
  const list=[p('wsw','NIGHT_RESULT','NONE'),p('yjsw','NIGHT_RESULT'),p('cxs','EXECUTION'),p('hzsw','EXECUTION'),p('mzsw','EXECUTION')]
  const id=await synthetic(page,request,list)
  let writes=0;page.on('request',r=>{if(['POST','PUT','DELETE'].includes(r.method()))writes++})
  try{
    await ready(page);await expect(page.getByTestId('stage-character')).toHaveCount(0)
    await completeLocalReading(page);await expect(page.getByTestId('event-dialogue')).toHaveText('犠牲者はいませんでした。\n')
    for(let cursor=1;cursor<list.length;cursor++){
      await completeLocalReading(page);await page.clock.runFor(1);await page.getByTestId('next-event').click();await ready(page)
      const event=list[cursor].event, names=mapping.Abel[event.eventname].default
      await image(page,names[0])
      await expect(page.getByTestId('stage-background')).toHaveAttribute('href',url(cursor===1?'haikei3.png':'haikei.png'))
      if(cursor===2)await page.getByTestId('stage-surface').screenshot({path:info.outputPath('execution-before.png')})
      if(cursor>1){await page.clock.runFor(Number(await page.getByTestId('game-stage').getAttribute('data-reading-length'))*50+1);await page.clock.runFor(1);await page.getByTestId('execution-continue').click()}
      else await page.clock.runFor(1000)
      await image(page,names[1])
      await completeLocalReading(page)
      if(cursor===2)await page.getByTestId('stage-surface').screenshot({path:info.outputPath('execution-after.png')})
      if(cursor>1)await expect(page.getByTestId('event-consequence')).toContainText('青年 アーベル')
      expect((await saved(page)).eventCursor).toBe(cursor)
      await expect(page.getByTestId('next-event')).toBeEnabled()
      await expect(page.locator('.player-identity').first()).not.toContainText('実際の役職')
    }
    expect(writes).toBe(0)
  }finally{await request.delete('/api/v1/games/'+id)}
})

test('WEB-025 reduced motion starts final frame, keyboard can revisit first without changing cursor; refresh repeats local presentation',async({page,request})=>{
  await page.emulateMedia({reducedMotion:'reduce'})
  const id=await synthetic(page,request,[p('yjsw','NIGHT_RESULT')])
  const names=mapping.Abel.yjsw.default
  try{
    await ready(page);await image(page,names[1])
    const button=page.getByTestId('stage-frame-toggle');await button.focus();await page.keyboard.press('Enter')
    await image(page,names[0]);expect((await saved(page)).eventCursor).toBe(0)
    await page.reload();await ready(page);await image(page,names[1])
    expect(Object.keys(await saved(page)).sort()).toEqual(['eventCursor','sessionId'])
  }finally{await request.delete('/api/v1/games/'+id)}
})

for(const [outcome,background,text,event]of [['VILLAGE_WIN','endVillage.png','村人勝利','crsl'],['WOLF_WIN','endWolf.png','人狼勝利','krsl'],['FOX_WIN','endFox.png','妖狐勝利','yhsl']]){
  test('WEB-025 '+outcome+' terminal stage and colored role portraits respect all-event gate and refresh',async({page,request},info)=>{
    const requests=[];page.on('request',r=>requests.push(new URL(r.url()).pathname))
    const id=await synthetic(page,request,[p('cxs','EXECUTION'),p(event,'SETTLEMENT')],s=>terminal(s,outcome))
    try{
      await ready(page)
      const row=page.locator('.player-row').first()
      await row.scrollIntoViewIfNeeded()
      await expect(row.locator('.portrait-image img')).toHaveAttribute('src',url('01s.png'))
      for(let cursor=0;cursor<2;cursor++){
        await expect(page.getByTestId('settlement-board')).toHaveCount(0)
        await expect(row).not.toContainText('実際の役職')
        expect(requests).not.toContain(url('01ws.png'));expect(requests).not.toContain(url('03fs.png'))
        if(cursor===1){await ready(page);await expect(page.getByTestId('stage-background')).toHaveAttribute('href',url(background));await image(page,mapping.Abel[event].default)}
        await completeLocalReading(page);await page.clock.runFor(1);await page.getByTestId('next-event').click()
      }
      await expect(row).toContainText('実際の役職 · 人狼');await ready(page)
      await expect(page.getByTestId('stage-background')).toHaveAttribute('href',url('frame #19252.png'))
      await expect(page.locator('.settlement h2')).toHaveText(text)
      await expect(page.getByTestId('settlement-board').locator('.settlement-card')).toHaveCount(3)
      await expect(page.getByTestId('settlement-board')).toContainText('人狼')
      await row.scrollIntoViewIfNeeded()
      await expect(row.locator('.portrait-image img')).toHaveAttribute('src',url('01ws.png'))
      await expect(page.locator('.player-row').nth(1).locator('.portrait-image img')).toHaveAttribute('src',url('02gs.png'))
      await expect(page.locator('.player-row').nth(2).locator('.portrait-image img')).toHaveAttribute('src',url('03fs.png'))
      await expect(page.getByTestId('stage-frame-toggle')).toHaveCount(0)
      await expect(page.getByTestId('stage-character')).toHaveCount(0)
      await page.locator('.players.panel').screenshot({path:info.outputPath('revealed-players.png')})
      await captureCore(page,info,'settlement-'+outcome+'.png')
      const before=await saved(page);await page.reload();await expect(row).toContainText('実際の役職 · 人狼')
      await row.scrollIntoViewIfNeeded()
      await expect(row.locator('.portrait-image img')).toHaveAttribute('src',url('01ws.png'))
      expect(await saved(page)).toEqual(before)
    }finally{await request.delete('/api/v1/games/'+id)}
  })
}

for(const mode of ['missing','duplicate','unknown-role','not-array']){
  test('WEB-025 malformed reveal '+mode+' gives explicit diagnosis and no fabricated role image',async({page,request})=>{
    const id=await synthetic(page,request,[p('crsl','SETTLEMENT')],s=>{
      terminal(s,'VILLAGE_WIN')
      if(mode==='missing')s.revealedRoles.pop()
      if(mode==='duplicate')s.revealedRoles[1]=s.revealedRoles[0]
      if(mode==='unknown-role')s.revealedRoles[1].actualRole='toString'
      if(mode==='not-array')s.revealedRoles=null
    })
    try{
      await completeLocalReading(page);await page.clock.runFor(1);await page.getByTestId('next-event').click()
      await expect(page.getByTestId('reveal-diagnostic')).toBeVisible()
      await expect(page.locator('.player-identity')).toContainText(Array(3).fill('身分不明（表示診断）'))
      await expect(page.locator('.portrait-image img')).toHaveCount(0)
      await expect(page.locator('.player-list')).not.toContainText('undefined')
      await expect(page.getByRole('button',{name:'新しい対局'})).toBeEnabled()
      expect((await saved(page)).eventCursor).toBe(1)
    }finally{await request.delete('/api/v1/games/'+id)}
  })
}

test('WEB-025 unknown outcome and wrong stage show diagnostic, never silently blank, and remain confirmable',async({page,request})=>{
  const id=await synthetic(page,request,[p('wsw','EXECUTION'),p('crsl','SETTLEMENT')],s=>terminal(s,'unknown'))
  try{
    for(let cursor=0;cursor<2;cursor++){
      await expect(page.getByTestId('stage-art-status')).toContainText('画像表示診断')
      await expect(page.getByTestId('next-event')).toBeEnabled()
      await completeLocalReading(page);await page.clock.runFor(1);await page.getByTestId('next-event').click()
    }
    await expect(page.locator('.settlement h2')).toHaveText('勝敗不明（表示診断）')
    await expect(page.getByTestId('stage-art-status')).toContainText('画像表示診断')
  }finally{await request.delete('/api/v1/games/'+id)}
})

test('WEB-025 blocked/corrupt second frame never publishes partial scene; stale decode cannot replace next event',async({page,request})=>{
  let release;const blocked=new Promise(resolve=>{release=resolve})
  await page.route('**'+url(mapping.Abel.yjsw.default[1]),async route=>{await blocked;await route.fulfill({status:200,contentType:'image/webp',body:'bad image'})})
  const id=await synthetic(page,request,[p('yjsw','NIGHT_RESULT'),p('zco7','DAY_DIALOGUE')])
  try{
    await expect(page.getByTestId('stage-art-status')).toContainText('画像読込中')
    await expect(page.getByTestId('decoded-stage-scene')).toHaveCount(0)
    await page.getByTestId('read-without-image').click()
    await completeLocalReading(page);await page.clock.runFor(1);await page.getByTestId('next-event').click();await ready(page)
    await image(page,mapping.Abel.zco7.default)
    release();await expect(page.getByTestId('stage-art-status')).toHaveCount(0)
    await image(page,mapping.Abel.zco7.default)
    expect((await saved(page)).eventCursor).toBe(1)
  }finally{release();await request.delete('/api/v1/games/'+id)}
})

test('WEB-025 long revealed board retains every player and scrolls by keyboard without advancing or writing',async({page,request})=>{
  const id=await synthetic(page,request,[p('crsl','SETTLEMENT')],s=>{
    terminal(s,'VILLAGE_WIN')
    s.view.players=publicSnapshot().view.players
    s.revealedRoles=s.view.players.map(player=>({playerNumber:player.playerNumber,actualRole:'wu'}))
  })
  let writes=0;page.on('request',r=>{if(['POST','PUT','DELETE'].includes(r.method()))writes++})
  try{
    await completeLocalReading(page);await page.clock.runFor(1);await page.getByTestId('next-event').click()
    const board=page.getByRole('region',{name:'結算役職一覧',exact:true})
    await expect(board.locator('.settlement-card')).toHaveCount(44)
    await board.focus();for(let i=0;i<44;i++)await page.keyboard.press('Tab')
    await expect(board.locator('.settlement-card').last()).toBeFocused()
    await expect.poll(()=>board.evaluate(node=>node.scrollLeft)).toBeGreaterThan(0)
    await expect(board.locator('.settlement-card').last().locator('.portrait-image img')).toHaveAttribute('src',url('44s.png'))
    expect((await saved(page)).eventCursor).toBe(1);expect(writes).toBe(0)
  }finally{await request.delete('/api/v1/games/'+id)}
})

test('WEB-025 corrupt transition image is diagnosed and cannot block confirmation or terminal reveal',async({page,request})=>{
  await page.route('**'+url(mapping.Abel.cxs.default[1]),route=>route.fulfill({status:200,contentType:'image/webp',body:'bad image'}))
  const id=await synthetic(page,request,[p('cxs','EXECUTION')],s=>terminal(s))
  try{
    await expect(page.getByTestId('stage-art-status')).toContainText('読み込めません')
    await expect(page.getByTestId('decoded-stage-scene')).toHaveCount(0)
    await expect(page.locator('.player-identity').first()).not.toContainText('実際の役職')
    await completeLocalReading(page);await page.clock.runFor(1);await page.getByTestId('next-event').click()
    await expect(page.locator('.player-identity').first()).toContainText('実際の役職 · 人狼')
  }finally{await request.delete('/api/v1/games/'+id)}
})

for(const width of [1280,1024,800])test('WEB-050 complete settlement fields and keyboard reach at '+width,async({page,request},info)=>{
  await page.setViewportSize({width,height:1000})
  const id=await synthetic(page,request,[p('crsl','SETTLEMENT')],s=>{
    terminal(s,'VILLAGE_WIN')
    s.view.players=publicSnapshot().view.players
    const reasons=['beiyao','chuxing','zhousha','dayhouzhui','nighthouzhui','daymaozhou','nightmaozhou']
    s.view.players.forEach((player,i)=>{
      player.claimedRole=i%2?'zhan':'NONE'
      if(i<7){player.alive=false;player.deathDay=i+1;player.deathReason=reasons[i]}
    })
    s.revealedRoles=s.view.players.map(player=>({playerNumber:player.playerNumber,actualRole:'wu'}))
  })
  const writes=[];page.on('request',r=>{if(['POST','PUT','DELETE'].includes(r.method()))writes.push(r.url())})
  try{
    await expect(page.getByTestId('settlement-board')).toHaveCount(0)
    await completeLocalReading(page);await page.clock.runFor(1);await page.getByTestId('next-event').click()
    const board=page.getByTestId('settlement-board'),cards=board.locator('.settlement-card')
    await expect(cards).toHaveCount(44)
    await expect(cards.first().getByTestId('settlement-claim')).toHaveText('無し')
    await expect(cards.nth(1).getByTestId('settlement-claim')).toHaveText('占い師')
    for(const [i,text]of ['1日目狼噛','2日目処刑','3日目呪殺','4日目後追','5日目後追','6日目猫呪','7日目猫呪'].entries())await expect(cards.nth(i).getByTestId('settlement-fate')).toHaveText(text)
    await expect(cards.last().getByTestId('settlement-fate')).toHaveText('最終存活')
    await expect(cards.last().getByTestId('settlement-actual')).toHaveText('村人')
    // Focus each card through the native keyboard path; the browser scrolls it
    // into the roster. No selector-only visibility claim for the last player.
    const roster=page.getByRole('region',{name:'結算役職一覧',exact:true})
    await roster.focus()
    for(let i=0;i<44;i++)await page.keyboard.press('Tab')
    await expect(cards.last()).toBeFocused()
    expect(await roster.evaluate(node=>node.scrollLeft)).toBeGreaterThan(0)
    expect(await roster.evaluate(node=>node.scrollHeight<=node.clientHeight+1)).toBe(true)
    expect(await cards.evaluateAll(nodes=>nodes.every(node=>{
      const b=node.getBoundingClientRect()
      return node.scrollHeight<=node.clientHeight+1&&[...node.children].every(child=>{
        const c=child.getBoundingClientRect();return c.top>=b.top-1&&c.bottom<=b.bottom+1&&c.left>=b.left-1&&c.right<=b.right+1
      })
    }))).toBe(true)
    await expect(page.locator('.stage-text')).toHaveCount(0)
    await captureCore(page,info,'settlement-'+width+'.png')
    const before=await saved(page);await page.reload()
    await expect(cards.first().getByTestId('settlement-fate')).toHaveText('1日目狼噛')
    expect(await saved(page)).toEqual(before);expect(writes).toEqual([])
  }finally{await request.delete('/api/v1/games/'+id)}
})

for(const width of [1440,1024,800])test('WEB-050 desktop voting window exposes original controls and complete roster at '+width,async({page,request},info)=>{
  await page.setViewportSize({width,height:1000})
  const id=await synthetic(page,request,[],s=>{
    s.view.players=publicSnapshot().view.players
    s.view.aliveCount=44;s.view.deathCount=0
  })
  const writes=[];page.on('request',r=>{if(['POST','PUT','DELETE'].includes(r.method()))writes.push(r.url())})
  try{
    const window=page.getByTestId('desktop-vote-window')
    await expect(window).toBeVisible()
    await expect(page.getByTestId('desktop-message')).toHaveCount(0)
    await expect(page.getByTestId('stage-text')).toHaveCount(0)
    const list=page.getByRole('region',{name:'公开玩家列表滚动区域',exact:true})
    await expect(list.getByRole('checkbox')).toHaveCount(0)
    await expect(list.locator('.desktop-roster-row')).toHaveCount(44)
    await list.focus()
    for(let i=0;i<44;i++)await page.keyboard.press('Tab')
    await expect(list.locator('.desktop-roster-row').last()).toBeFocused()
    expect(await page.locator('.desktop-vote-players').evaluate(n=>n.scrollLeft)).toBeGreaterThan(0)
    // Both rows including their name artwork remain inside the vertical viewport.
    expect(await page.locator('.desktop-vote-players').evaluate(n=>{
      const r=n.getBoundingClientRect();return [...n.querySelectorAll('.desktop-roster-row')].every(p=>{const c=p.getBoundingClientRect();return c.top>=r.top-1&&c.bottom<=r.top+n.clientHeight+1})
    })).toBe(true)
    await page.getByRole('button',{name:'指示する',exact:true}).click()
    await expect(page.locator('.vote-information-content')).toBeFocused()
    await expect(window).toHaveAttribute('data-menu','orders')
    await desktopHome(page)
    await page.getByRole('button',{name:'記録確認',exact:true}).click()
    await expect(page.getByRole('region',{name:'公開情報',exact:true})).toBeVisible()
    await expect(page.getByRole('region',{name:'公開情報',exact:true})).toBeVisible()
    await desktopHome(page)
    await expect(page.getByTestId('vote')).toBeEnabled()
    await page.locator('.desktop-vote-players').evaluate(n=>{n.scrollLeft=0})
    await captureCore(page,info,'vote-'+width+'.png')
    await page.getByRole('button',{name:'メニュー',exact:true}).click()
    await expect(page.getByRole('button',{name:'対局を残す',exact:true})).toBeFocused()
    expect(writes).toEqual([]);expect((await saved(page)).eventCursor).toBe(0)
  }finally{await request.delete('/api/v1/games/'+id)}
})
