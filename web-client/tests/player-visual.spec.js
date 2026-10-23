import { test, expect } from '@playwright/test'
import { readFileSync } from 'node:fs'
import { characterDisplayName } from '../src/characterDisplay.js'
import { publicSnapshot } from '../test-fixtures/public-players.js'

const manifest = JSON.parse(readFileSync(new URL('../public/assets/images/manifest.json', import.meta.url), 'utf8'))
const url = name => '/assets/images/' + manifest.images['images/' + name].file
const key = 'vocr.guest.v1'
const saved = page => page.evaluate(key => JSON.parse(sessionStorage.getItem(key)), key)
async function synthetic(page, request, edit = () => {}) {
  const created = await (await request.post('/api/v1/games', { data: { configuration: 'tongchang' } })).json()
  const snapshot = publicSnapshot()
  edit(snapshot)
  await page.route('**/api/v1/games/' + created.sessionId + '?**', route => {
    const cursor = Number(new URL(route.request().url()).searchParams.get('eventCursor') || 0)
    return route.fulfill({ json: { ...snapshot, eventCursor: cursor,
      events: snapshot.events.slice(cursor), presentedEvents: snapshot.presentedEvents.slice(cursor) } })
  })
  await page.addInitScript(({ key, id }) => {
    if (!sessionStorage.getItem(key)) sessionStorage.setItem(key, JSON.stringify({ sessionId: id, eventCursor: 0 }))
  }, { key, id: created.sessionId })
  await page.goto('/')
  await expect(page.locator('.player-row')).toHaveCount(snapshot.view.players.length)
  return { id: created.sessionId, snapshot }
}
async function expectImage(locator, name) {
  await expect(locator).toHaveAttribute('src', url(name))
  await expect.poll(() => locator.evaluate(image => image.complete && image.naturalWidth > 0)).toBe(true)
}

test('WEB-023 real public players use ordinal Japanese names and status images, refresh preserves cursor', async ({ page, request }, testInfo) => {
  await page.goto('/')
  const createdResponse = page.waitForResponse(res => res.request().method() === 'POST' && res.url().endsWith('/api/v1/games'))
  await page.getByRole('button', { name: '开始对局' }).click()
  const created = await (await createdResponse).json()
  try {
    const state = await saved(page)
    const text = await page.getByTestId('event-dialogue').textContent()
    for (const player of created.update.view.players) {
      const row = page.locator(`[data-player-number="${player.playerNumber}"]`)
      await expect(row.locator('.player-identity strong')).toHaveText(characterDisplayName(player.character))
    }
    const row = page.locator('.player-row').first()
    await row.scrollIntoViewIfNeeded()
    await expect.poll(() => row.locator('.portrait-image img').evaluateAll(images => images.length === 1 && images[0].naturalWidth === 64)).toBe(true)
    await expect(row.getByRole('checkbox')).toBeDisabled()
    await page.locator('.players.panel').screenshot({ path: testInfo.outputPath('players-original-assets.png') })
    await page.reload()
    await expect(page.getByTestId('event-dialogue')).toHaveText(text)
    expect(await saved(page)).toEqual(state)
    await expect(page.locator('.player-identity').first()).not.toContainText('真实身份')
  } finally { await request.delete('/api/v1/games/' + created.sessionId) }
})

test('WEB-023 all 44 appearances and public gray/death states are independent of playerNumber and private fields', async ({ page, request }) => {
  const { id, snapshot } = await synthetic(page, request, snapshot => {
    const reversed = snapshot.view.players.map(p => p.character).reverse()
    snapshot.view.players.forEach((p, i) => Object.assign(p, { character: reversed[i], alive: i % 2 === 0,
      deathDay: i % 2, deathReason: i % 2 ? 'NIGHT_DEATH' : 'NONE' }))
  })
  let writes = 0
  page.on('request', req => { if (['POST','PUT','DELETE'].includes(req.method())) writes++ })
  try {
    for (const [i, player] of snapshot.view.players.entries()) {
      const row = page.locator('.player-row').nth(i)
      await row.scrollIntoViewIfNeeded()
      await expect(row.locator('.player-identity strong')).toHaveText(characterDisplayName(player.character))
      await expectImage(row.locator('.portrait-image img'), String(44-i).padStart(2,'0') + (player.alive ? 's' : 'gs') + '.png')
      await expectImage(row.locator('.character-title-image img'), (44-i) + 'job.png')
      if (!player.alive) await expectImage(row.locator('.death-marker img'),'kami.png')
      await expect(row.locator('.claim-icon')).toHaveCount(0)
    }
    expect(writes).toBe(0)
    expect((await saved(page)).eventCursor).toBe(0)
    await expect(page.locator('.player-list')).not.toContainText('真实身份')
  } finally { await request.delete('/api/v1/games/' + id) }
})

test('WEB-023 public CO icons preserve exact order, overflow has text fallback and day14 stays textual', async ({ page, request }, testInfo) => {
  const cases = [['zhan',1,'yaku1_1.png'],['zhan',5,'yaku1_5.png'],['ling',4,'yaku2_4.png'],
    ['lie',4,'yaku3_4.png'],['gong',99,'yaku4.png'],['mao',99,'yaku5.png'],
    ['zhan',6,null],['ling',5,null],['lie',5,null],['zhan',0,null]]
  const { id } = await synthetic(page, request, snapshot => {
    snapshot.view.gameDay = 14
    snapshot.view.players = snapshot.view.players.slice(0,cases.length)
    snapshot.view.players.forEach((p,i) => Object.assign(p,{claimedRole:cases[i][0],claimedRoleOrder:cases[i][1]}))
  })
  try {
    await expect(page.getByRole('heading', { level: 1 })).toContainText('第 14 日')
    for(const [i,[role,order,name]] of cases.entries()) {
      const row=page.locator('.player-row').nth(i)
      await row.scrollIntoViewIfNeeded()
      if(name) await expectImage(row.locator('.claim-icon img'),name)
      else {
        await expect(row.locator('.claim-icon')).toHaveCount(0)
        await expect(row).toContainText('表示診断：CO図なし')
        await expect(row).toContainText(order ? 'CO順 '+order : 'CO順不明')
      }
    }
    await page.locator('.player-list').screenshot({path:testInfo.outputPath('public-co-icons.png')})
    // No date bitmap is introduced for day14; the existing heading is the deliberate fallback.
    const images=await page.locator('.player-list img').evaluateAll(nodes=>nodes.map(n=>n.getAttribute('src')))
    expect(images.every(src=>src.startsWith('/assets/images/'))).toBe(true)
  } finally { await request.delete('/api/v1/games/' + id) }
})

test('WEB-023 unknown characters and roles remain pure-text diagnostics without a fabricated portrait', async ({ page, request }) => {
  const { id }=await synthetic(page,request,snapshot=>{
    snapshot.view.players=snapshot.view.players.slice(0,3)
    for(const [i,p] of snapshot.view.players.entries()) Object.assign(p,{character:['NONE','toString','<img onerror="window.bad=1">'][i],claimedRole:'unknown'})
    snapshot.view.gameDay=50
  })
  try {
    await expect(page.getByRole('heading',{level:1})).toContainText('第 50 日')
    await page.locator('.player-row').first().scrollIntoViewIfNeeded()
    await expect(page.locator('.player-identity strong')).toHaveText(Array(3).fill('人物不明（表示診断）'))
    await expect(page.locator('.player-list img')).toHaveCount(0)
    await expect(page.locator('.portrait-image .asset-diagnostic')).toHaveCount(3)
    await expect(page.locator('.player-identity').first()).toContainText('職業不明（表示診断）')
    expect(await page.evaluate(()=>window.bad)).toBeUndefined()
  } finally {await request.delete('/api/v1/games/'+id)}
})

test('WEB-023 broken image retains explicit fallback and cannot skip terminal dialogue or change identity gate', async ({page,request})=>{
  await page.route('**'+url('01s.png'),route=>route.fulfill({status:404,body:'missing'}))
  const {id}=await synthetic(page,request,snapshot=>{
    snapshot.phase='FINISHED';snapshot.view.result='VILLAGE_WIN'
    snapshot.view.players=snapshot.view.players.slice(0,2)
    snapshot.events=[{eventname:'crsl',ch1:'Abel',ch2:'NONE',ch3:'NONE'}, {eventname:'crsl',ch1:'Erich',ch2:'NONE',ch3:'NONE'}]
    snapshot.presentedEvents=snapshot.events.map(event=>({stage:'SETTLEMENT',event}))
    snapshot.nextEventCursor=2
    snapshot.revealedRoles=snapshot.view.players.map(p=>({playerNumber:p.playerNumber,actualRole:'renlang'}))
  })
  let writes=0
  page.on('request',req=>{if(['POST','PUT','DELETE'].includes(req.method()))writes++})
  try{
    const row=page.locator('.player-row').first()
    await row.scrollIntoViewIfNeeded()
    await expect(row.locator('.portrait-image .asset-diagnostic')).toBeVisible()
    await expect(row.locator('.portrait-image img')).toHaveCount(0)
    await expect(row).not.toContainText('真实身份')
    await page.getByTestId('next-event').click()
    await expect(row).not.toContainText('真实身份')
    await page.getByTestId('next-event').click()
    await expect(row).toContainText('真实身份 · 人狼')
    //025 changes to the role portrait only after every terminal event has been acknowledged.
    await expectImage(row.locator('.portrait-image img'),'01ws.png')
    expect((await saved(page)).eventCursor).toBe(2)
    expect(writes).toBe(0)
  }finally{await request.delete('/api/v1/games/'+id)}
})

test('WEB-023 manifest failure is visible; reload can recover images without altering the session',async({page,request})=>{
  const block=route=>route.fulfill({status:503,body:'unavailable'})
  await page.route('**/assets/images/manifest.json',block)
  const {id}=await synthetic(page,request,s=>{s.view.players=s.view.players.slice(0,2)})
  try{
    const row=page.locator('.player-row').first()
    await row.scrollIntoViewIfNeeded()
    await expect(row.locator('.portrait-image .asset-diagnostic')).toBeVisible()
    await expect(row.getByRole('checkbox')).toBeEnabled()
    const before=await saved(page)
    await page.unroute('**/assets/images/manifest.json',block)
    await page.reload()
    await row.scrollIntoViewIfNeeded()
    await expectImage(row.locator('.portrait-image img'),'01s.png')
    expect(await saved(page)).toEqual(before)
  }finally{await request.delete('/api/v1/games/'+id)}
})
