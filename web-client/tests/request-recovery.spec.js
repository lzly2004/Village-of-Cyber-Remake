import { test, expect } from '@playwright/test'
import { drainEvents } from './drain-events.js'

const key='vocr.guest.v1'
const saved=page=>page.evaluate(key=>JSON.parse(sessionStorage.getItem(key)),key)
const sync=page=>page.getByRole('button',{name:'再同期',exact:true})
const errorBody=code=>JSON.stringify({code,message:'controlled rejection',currentRevision:null})
async function start(page){
  await page.goto('/')
  await page.getByRole('button',{name:'対局を始める',exact:true}).click()
  await expect(page.getByTestId('next-event')).toBeVisible()
  return (await saved(page)).sessionId
}
const read= (request,id)=>request.get('/api/v1/games/'+id).then(r=>r.json())

test('known 429 write rejection does not execute or retry and keeps authoritative choices available',async({page,request})=>{
  const id=await start(page)
  try{
    await drainEvents(page)
    const before=await read(request,id),local=await saved(page)
    let writes=0
    await page.route('**/vote-target',route=>{
      writes++;return route.fulfill({status:429,contentType:'application/json',headers:{'Retry-After':'1'},body:errorBody('REQUEST_LIMIT')})
    })
    const choice=page.locator('.player-row:not(.departed)').getByRole('checkbox').first()
    await choice.click()
    await expect(page.getByRole('alert')).toContainText('本次操作未执行')
    await expect(choice).toBeEnabled()
    expect(writes).toBe(1);expect(await saved(page)).toEqual(local)
    expect(await read(request,id)).toEqual(before)
    await page.unroute('**/vote-target')
    await choice.click();await expect(choice).toBeChecked()
    expect((await read(request,id)).revision).toBe(before.revision+1)
  }finally{await request.delete('/api/v1/games/'+id)}
})

test('known 429 read preserves cursor, locks decisions, and never performs the cursor fallback',async({page,request})=>{
  const id=await start(page)
  try{
    await drainEvents(page)
    const before=await saved(page),reads=[]
    await page.route('**/api/v1/games/'+id+'?*',route=>{
      reads.push(new URL(route.request().url()).searchParams.get('eventCursor'))
      return route.fulfill({status:429,contentType:'application/json',body:errorBody('REQUEST_LIMIT')})
    })
    await sync(page).click()
    await expect(page.getByRole('alert')).toContainText('读取暂被限流')
    expect(reads).toEqual([String(before.eventCursor)]);expect(await saved(page)).toEqual(before)
    await expect(page.locator('.player-row:not(.departed)').getByRole('checkbox').first()).toBeDisabled()
    await page.unroute('**/api/v1/games/'+id+'?*')
    await sync(page).click()
    await expect(page.locator('.player-row:not(.departed)').getByRole('checkbox').first()).toBeEnabled()
  }finally{await request.delete('/api/v1/games/'+id)}
})

test('known 429 creation is rejected without capability and only an explicit new click retries',async({page,request})=>{
  let creates=0,id
  await page.goto('/')
  await page.route('**/api/v1/games',route=>{
    creates++;return route.fulfill({status:429,contentType:'application/json',body:errorBody('REQUEST_LIMIT')})
  })
  await page.getByRole('button',{name:'対局を始める',exact:true}).click()
  await expect(page.getByRole('alert')).toContainText('本次操作未执行')
  expect(creates).toBe(1);expect(await saved(page)).toBeNull()
  await page.unroute('**/api/v1/games')
  try{
    await page.getByRole('button',{name:'対局を始める',exact:true}).click()
    await expect(page.getByTestId('next-event')).toBeVisible();id=(await saved(page)).sessionId
  }finally{if(id)await request.delete('/api/v1/games/'+id)}
})

test('known 409 INVALID_STATE synchronizes exactly once without replaying the rejected vote',async({page,request})=>{
  const id=await start(page)
  try{
    await drainEvents(page)
    const choice=page.locator('.player-row:not(.departed)').getByRole('checkbox').first()
    await choice.click();await expect(choice).toBeChecked()
    await expect(page.getByTestId('vote')).toBeEnabled()
    const before=await read(request,id),local=await saved(page),reads=[];let writes=0
    page.on('request',req=>{if(req.method()==='GET'&&new URL(req.url()).pathname==='/api/v1/games/'+id)reads.push(req.url())})
    await page.route('**/votes',route=>{writes++;return route.fulfill({status:409,contentType:'application/json',body:errorBody('INVALID_STATE')})})
    await page.getByTestId('vote').click()
    await expect(page.getByRole('alert')).toContainText('已重新同步')
    expect(writes).toBe(1);expect(reads).toHaveLength(1)
    expect(await saved(page)).toEqual(local);expect(await read(request,id)).toEqual(before)
  }finally{await request.delete('/api/v1/games/'+id)}
})

for(const [status,code] of [[400,'HTTP_ERROR'],[400,'OTHER'],[409,'STALE_REVISION'],[404,'HTTP_ERROR']]){
  test(`GET ${status}/${code} never rewinds or retries and locks stale decisions`,async({page,request})=>{
    const id=await start(page)
    try{
      await drainEvents(page)
      await page.locator('.player-row:not(.departed)').getByRole('checkbox').first().click()
    await expect(page.locator('.player-row:not(.departed)').getByRole('checkbox').first()).toBeChecked()
      await expect(page.getByTestId('vote')).toBeEnabled()
      const before=await saved(page), authoritative=await read(request,id), reads=[]
      await page.route('**/api/v1/games/'+id+'?*',route=>{
        reads.push(new URL(route.request().url()).searchParams.get('eventCursor'))
        return route.fulfill({status,contentType:'application/json',body:errorBody(code)})
      })
      await sync(page).click()
      await expect(page.getByRole('alert')).toContainText('无法读取权威状态')
      expect(reads).toEqual([String(before.eventCursor)])
      expect(await saved(page)).toEqual(before)
      await expect(page.getByTestId('vote')).toBeDisabled()
      expect(await read(request,id)).toEqual(authoritative)
      await page.unroute('**/api/v1/games/'+id+'?*')
      await sync(page).click()
      await expect(page.getByTestId('vote')).toBeEnabled()
      expect(await saved(page)).toEqual(before)
    }finally{await request.delete('/api/v1/games/'+id)}
  })
}

test('GET cursor rejection reads C=0 once; a failed fallback preserves the original cursor',async({page,request})=>{
  const id=await start(page)
  try{
    await page.getByTestId('next-event').click()
    const before=await saved(page), reads=[]
    expect(before.eventCursor).toBe(1)
    await page.route('**/api/v1/games/'+id+'?*',route=>{
      reads.push(new URL(route.request().url()).searchParams.get('eventCursor'))
      return route.fulfill({status:400,contentType:'application/json',body:errorBody('INVALID_REQUEST')})
    })
    await sync(page).click()
    await expect(page.getByRole('alert')).toContainText('无法读取权威状态')
    expect(reads).toEqual(['1','0'])
    expect(await saved(page)).toEqual(before)
    await page.unroute('**/api/v1/games/'+id+'?*')
    // The actual server rejects an out-of-range cursor, then returns the real full window.
    await page.evaluate(key=>{const v=JSON.parse(sessionStorage.getItem(key));v.eventCursor=999999;sessionStorage.setItem(key,JSON.stringify(v))},key)
    const actual=[]
    page.on('request',req=>{if(req.method()==='GET'&&new URL(req.url()).pathname==='/api/v1/games/'+id)actual.push(new URL(req.url()).searchParams.get('eventCursor'))})
    await page.reload()
    await expect(page.getByTestId('next-event')).toBeVisible()
    expect(actual).toEqual(['999999','0'])
    expect((await saved(page)).eventCursor).toBe(0)
    expect((await saved(page)).sessionId).toBe(id)
  }finally{await request.delete('/api/v1/games/'+id)}
})

test('GET at C=0 gets no recursive retry even for INVALID_REQUEST',async({page,request})=>{
  const id=await start(page)
  try{
    let reads=0
    await page.route('**/api/v1/games/'+id+'?*',route=>{reads++;return route.fulfill({status:400,contentType:'application/json',body:errorBody('INVALID_REQUEST')})})
    await sync(page).click()
    await expect(page.getByRole('alert')).toContainText('无法读取权威状态')
    expect(reads).toBe(1);expect((await saved(page)).eventCursor).toBe(0)
  }finally{await request.delete('/api/v1/games/'+id)}
})

for(const [status,code] of [[400,'HTTP_ERROR'],[404,'HTTP_ERROR'],[409,'HTTP_ERROR'],[429,'HTTP_ERROR'],[503,'SESSION_LIMIT'],[500,'INTERNAL_ERROR']]){
  test(`write already committed then ${status}/${code}: uncertain, no replay, authoritative sync`,async({page,request})=>{
    const id=await start(page)
    try{
      await drainEvents(page)
      const before=await read(request,id), local=await saved(page)
      let writes=0
      await page.route('**/vote-target',async route=>{
        writes++
        const committed=await route.fetch()
        expect(committed.status()).toBe(200)
        await route.fulfill({status,contentType:'application/json',body:errorBody(code)})
      })
      await page.locator('.player-row:not(.departed)').getByRole('checkbox').first().click()
      await expect(page.getByRole('alert')).toContainText('操作可能已在服务器完成')
      await expect(page.getByTestId('vote')).toBeDisabled()
      expect(writes).toBe(1);expect(await saved(page)).toEqual(local)
      const after=await read(request,id)
      expect(after.revision).toBe(before.revision+1)
      await sync(page).click()
      await expect(page.getByTestId('vote')).toBeEnabled()
      expect(writes).toBe(1)
      expect((await read(request,id)).revision).toBe(after.revision)
    }finally{await request.delete('/api/v1/games/'+id)}
  })
}

test('known INVALID_REQUEST rejects without rewinding, auto sync or command replay',async({page,request})=>{
  const id=await start(page)
  try{
    await drainEvents(page)
    await page.locator('.player-row:not(.departed)').getByRole('checkbox').first().click()
    await expect(page.locator('.player-row:not(.departed)').getByRole('checkbox').first()).toBeChecked()
    await expect(page.getByTestId('vote')).toBeEnabled()
    const before=await saved(page), authority=await read(request,id)
    let writes=0,reads=0
    page.on('request',req=>{if(req.method()==='GET'&&new URL(req.url()).pathname==='/api/v1/games/'+id)reads++})
    await page.route('**/vote-target',route=>{writes++;return route.fulfill({status:400,contentType:'application/json',body:errorBody('INVALID_REQUEST')})})
    await page.locator('.player-row:not(.departed)').getByRole('checkbox').nth(1).click()
    await expect(page.getByRole('alert')).toHaveText('controlled rejection')
    await expect(page.getByTestId('vote')).toBeEnabled()
    expect(writes).toBe(1);expect(reads).toBe(0)
    expect(await saved(page)).toEqual(before);expect(await read(request,id)).toEqual(authority)
  }finally{await request.delete('/api/v1/games/'+id)}
})

test('offline GET preserves cursor, blocks decisions, and explicit online sync recovers',async({page,context,request})=>{
  const id=await start(page)
  try{
    await drainEvents(page)
    await page.locator('.player-row:not(.departed)').getByRole('checkbox').first().click()
    await expect(page.locator('.player-row:not(.departed)').getByRole('checkbox').first()).toBeChecked()
    await expect(page.getByTestId('vote')).toBeEnabled()
    const before=await saved(page)
    await context.setOffline(true)
    await sync(page).click()
    await expect(page.getByRole('alert')).toContainText('无法读取权威状态')
    expect(await saved(page)).toEqual(before)
    await expect(page.getByTestId('vote')).toBeDisabled()
    await context.setOffline(false)
    await sync(page).click()
    await expect(page.getByTestId('vote')).toBeEnabled()
    expect(await saved(page)).toEqual(before)
  }finally{await context.setOffline(false);await request.delete('/api/v1/games/'+id)}
})

test('create committed but response lost: no credential or automatic retry; explicit new start only',async({page,request})=>{
  let posts=0;const ids=[]
  await page.route('**/api/v1/games',async route=>{
    if(route.request().method()!=='POST')return route.continue()
    posts++;const response=await route.fetch();ids.push((await response.json()).sessionId)
    if(posts===1)await route.abort('failed');else await route.fulfill({response})
  })
  try{
    await page.goto('/')
    const begin=page.getByRole('button',{name:'対局を始める',exact:true})
    await begin.click()
    await expect(page.getByRole('alert')).toContainText('服务器可能已创建对局')
    expect(posts).toBe(1);expect(await saved(page)).toBeNull()
    await begin.click();await expect(page.getByTestId('next-event')).toBeVisible()
    expect(posts).toBe(2);expect((await saved(page)).sessionId).toBe(ids[1]);expect(ids[0]).not.toBe(ids[1])
  }finally{for(const id of ids)await request.delete('/api/v1/games/'+id)}
})

test('two pages sharing one capability conflict once; local reading makes no background writes or polling',async({page,context,request})=>{
  const id=await start(page),other=await context.newPage()
  try{
    await drainEvents(page);const local=await saved(page)
    await other.addInitScript(({key,local})=>sessionStorage.setItem(key,JSON.stringify(local)),{key,local})
    await other.goto('/');await expect(other.locator('.player-row:not(.departed)').getByRole('checkbox').first()).toBeEnabled()
    let writes=0
    other.on('request',req=>{if(req.method()!=='GET'&&req.url().includes('/api/v1/games'))writes++})
    await page.locator('.player-row:not(.departed)').getByRole('checkbox').first().click()
    await expect(page.locator('.player-row:not(.departed)').getByRole('checkbox').first()).toBeChecked()
    await other.locator('.player-row:not(.departed)').getByRole('checkbox').nth(1).click()
    await expect(other.getByRole('alert')).toContainText('已重新同步')
    expect(writes).toBe(1)
    await expect(other.locator('.player-row:not(.departed)').getByRole('checkbox').first()).toBeChecked()
    await expect(other.locator('.player-row:not(.departed)').getByRole('checkbox').nth(1)).not.toBeChecked()
    expect((await saved(other)).eventCursor).toBe(local.eventCursor)
    // Explicitly re-read full events; the confirmation loop itself performs no HTTP.
    await page.evaluate(key=>{const v=JSON.parse(sessionStorage.getItem(key));v.eventCursor=0;sessionStorage.setItem(key,JSON.stringify(v))},key)
    await page.reload();await expect(page.getByTestId('next-event')).toBeVisible()
    const calls=[];page.on('request',req=>{if(req.url().includes('/api/v1/games'))calls.push(req.method())})
    await drainEvents(page)
    expect(calls).toEqual([])
    expect((await read(request,id)).revision).toBe(2)
  }finally{await other.close();await request.delete('/api/v1/games/'+id)}
})

for(const [status,body,type] of [[400,'<html>gateway</html>','text/html'],[200,'{}','application/json']]){
  test(`unusable GET response ${status}/${type} preserves the last state with no C=0 retry`,async({page,request})=>{
    const id=await start(page)
    try{
      await page.getByTestId('next-event').click()
      const before=await saved(page);let reads=0
      await page.route('**/api/v1/games/'+id+'?*',route=>{reads++;return route.fulfill({status,body,contentType:type})})
      await sync(page).click()
      await expect(page.getByRole('alert')).toContainText('无法读取权威状态')
      expect(reads).toBe(1);expect(await saved(page)).toEqual(before)
      await expect(page.getByTestId('next-event')).toBeVisible()
    }finally{await request.delete('/api/v1/games/'+id)}
  })
}

test('GET timeout aborts after the real client deadline, keeps the cursor, and has no late state replacement',async({page,request})=>{
  const id=await start(page);let unblock
  const hold=new Promise(resolve=>{unblock=resolve})
  try{
    await drainEvents(page)
    const before=await saved(page)
    await page.clock.install()
    let reads=0
    await page.route('**/api/v1/games/'+id+'?*',async route=>{reads++;await hold;try{await route.continue()}catch{/* timed-out browser request */}})
    const sent=page.waitForRequest(req=>req.method()==='GET'&&new URL(req.url()).pathname==='/api/v1/games/'+id)
    await sync(page).click();await sent
    await page.clock.fastForward(15001)
    await expect(page.getByRole('alert')).toContainText('无法读取权威状态')
    expect(reads).toBe(1);expect(await saved(page)).toEqual(before)
    unblock();await page.unroute('**/api/v1/games/'+id+'?*')
    await page.clock.resume()
    await sync(page).click()
    await expect(page.getByRole('alert')).toHaveCount(0)
    expect(await saved(page)).toEqual(before)
  }finally{unblock();await request.delete('/api/v1/games/'+id)}
})

test('rapid double vote sends one command while in flight; accepted response is not replayed',async({page,request})=>{
  const id=await start(page);let unblock
  const hold=new Promise(resolve=>{unblock=resolve})
  try{
    await drainEvents(page)
    await page.locator('.player-row:not(.departed)').getByRole('checkbox').first().click()
    await expect(page.locator('.player-row:not(.departed)').getByRole('checkbox').first()).toBeChecked()
    await expect(page.getByTestId('vote')).toBeEnabled()
    const before=await read(request,id);let writes=0
    await page.route('**/votes',async route=>{writes++;await hold;const result=await route.fetch();await route.fulfill({response:result})})
    await page.getByTestId('vote').dblclick({force:true})
    await expect(page.getByTestId('vote')).toBeDisabled()
    expect(writes).toBe(1);expect((await read(request,id)).revision).toBe(before.revision)
    unblock()
    await expect(page.getByTestId('next-event')).toBeVisible()
    expect(writes).toBe(1);expect((await read(request,id)).revision).toBe(before.revision+1)
  }finally{unblock();await request.delete('/api/v1/games/'+id)}
})

test('DELETE committed then response lost: keep capability until explicit GET proves GAME_NOT_FOUND',async({page,request})=>{
  const id=await start(page)
  try{
    const before=await saved(page);let deletes=0
    await page.route('**/api/v1/games/'+id,async route=>{
      if(route.request().method()!=='DELETE')return route.continue()
      deletes++;expect((await route.fetch()).status()).toBe(204);await route.abort('failed')
    })
    await page.getByRole('button',{name:'試遊を終了',exact:true}).click()
    await page.getByRole('button',{name:'解放を確定',exact:true}).click()
    await expect(page.getByRole('alert')).toContainText('操作可能已在服务器完成')
    expect(await saved(page)).toEqual(before);expect(deletes).toBe(1)
    await sync(page).click()
    await expect(page.getByRole('alert')).toContainText('已失效')
    expect(await saved(page)).toBeNull();expect(deletes).toBe(1)
  }finally{await request.delete('/api/v1/games/'+id)}
})

test('create with a valid capability but malformed update retains only that capability for explicit sync',async({page,request})=>{
  let id,posts=0
  await page.route('**/api/v1/games',async route=>{
    if(route.request().method()!=='POST')return route.continue()
    posts++;const response=await route.fetch();const created=await response.json();id=created.sessionId
    await route.fulfill({status:201,contentType:'application/json',body:JSON.stringify({...created,update:{}})})
  })
  try{
    await page.goto('/');await page.getByRole('button',{name:'対局を始める',exact:true}).click()
    await expect(page.getByRole('alert')).toContainText('已保留本局凭据')
    expect(await saved(page)).toEqual({sessionId:id,eventCursor:0});expect(posts).toBe(1)
    // Both sidebar and reconnect-panel buttons invoke the same explicit read.
    await sync(page).first().click()
    await expect(page.getByTestId('next-event')).toBeVisible()
    expect(posts).toBe(1);expect((await saved(page)).sessionId).toBe(id)
  }finally{if(id)await request.delete('/api/v1/games/'+id)}
})
