import { desktopVote, desktopSelection, desktopClaims, desktopClaimPlayers, desktopHome, desktopSkill, expectNoEnabledVote } from '../test-fixtures/desktop-navigation.js'
import { completeLocalReading } from '../test-fixtures/presentation-test.js'
import { drainEvents as readEvents } from './drain-events.js'
import { characterDisplayName } from '../src/characterDisplay.js'
import { test, expect } from '../test-fixtures/presentation-test.js'

const storageKey = 'vocr.guest.v1'

async function saved(page) {
  return page.evaluate(key => JSON.parse(sessionStorage.getItem(key)), storageKey)
}

async function start(page, configuration = '簡易') {
  await page.goto('/')
  await page.getByRole('radio', { name: configuration + '配役' }).check()
  await page.getByRole('button', { name: '対局を始める' }).click()
  await expect(page.getByRole('region', { name: '当前事件' })).toBeVisible()
}



async function release(request, id) {
  if (id) await request.delete('/api/v1/games/' + id)
}

test('选择配役、逐条阅读与刷新恢复已确认游标', async ({ page, request }) => {
  await start(page, '猫又')
  const id = (await saved(page)).sessionId
  try {
    await expect(page.getByRole('heading', { level: 1 })).toContainText('猫又村')
    await expectNoEnabledVote(page)
    await page.getByTestId('next-event').click()
    await expect.poll(async () => (await saved(page)).eventCursor).toBe(1)
    await completeLocalReading(page)
    const event = await page.getByRole('region', { name: '当前事件' }).textContent()
    await page.reload()
    await expect(page.getByRole('region', { name: '当前事件' })).toHaveText(event)
    expect((await saved(page)).sessionId).toBe(id)
    expect(Object.keys(await saved(page)).sort()).toEqual(['eventCursor', 'sessionId'])
    await readEvents(page)
    await expect(page.getByTestId('desktop-vote-window')).toBeVisible()
  } finally { await release(request, id) }
})

test('真实对局从浏览器指定投票直至终局，并刷新恢复揭晓', async ({ page, request }, testInfo) => {
  await start(page)
  const id = (await saved(page)).sessionId
  try {
    let rounds = 0
    while (true) {
      await readEvents(page)
      if (await page.getByRole('heading', { name: /村人勝利|人狼勝利|妖狐勝利/ }).count()) break
      expect(++rounds).toBeLessThan(60)
      await desktopSelection(page)
      // 只根据页面的公开 CO 选择候补；共有者回避后取消该目标。
      for (const checkbox of await page.locator('.player-row').filter({ hasText: '共有者' })
        .getByRole('checkbox').all()) {
        if (await checkbox.isChecked() && await checkbox.isEnabled()) {
          await checkbox.click()
          await expect(checkbox).not.toBeChecked()
        }
      }
      await desktopSelection(page)
      const candidate = page.locator('.player-row:not(.departed)')
        .filter({ hasNotText: '共有者' }).getByRole('checkbox').first()
      if (!await candidate.isChecked()) {
        await candidate.click()
        await expect(candidate).toBeChecked()
      }
      await expect(page.getByTestId('desktop-vote-window')).toHaveAttribute('data-command-blocked','false')
      await desktopVote(page,'DESIGNATED')
      await expect(page.getByTestId('next-event')).toBeVisible()
    }
    await expect(page.locator('.player-identity').first()).toContainText('実際の役職')
    await expect(page.getByRole('region', { name: '最近投票结果' })).toBeVisible()
    await page.screenshot({ path: testInfo.outputPath('finished.png'), fullPage: true })
    await page.reload()
    await expect(page.getByRole('heading', { name: /村人勝利|人狼勝利|妖狐勝利/ })).toBeVisible()
    await expect(page.getByRole('button', { name: '新しい対局' })).toBeVisible()
    await expect(page.getByRole('region', { name: '職業別CO確認' })).toHaveCount(0)
    await expect(page.getByRole('region', { name: 'プレイヤー別CO確認' })).toHaveCount(0)
    await expect(page.getByRole('switch', { name: '投票回避CO' })).toHaveCount(0)
  } finally { await release(request, id) }
})

test('409 同步权威选择，未自动重试失败的写命令', async ({ page, request }) => {
  await start(page)
  await readEvents(page)
  const id = (await saved(page)).sessionId
  try {
    const snapshot = await (await request.get('/api/v1/games/' + id)).json()
    const alive = snapshot.view.players.filter(player => player.alive)
    const staleTarget = alive[1]
    const changed = await request.put('/api/v1/games/' + id + '/vote-target', {
      data: { expectedRevision: snapshot.revision, target: alive[0].playerNumber, selected: true },
    })
    expect(changed.ok()).toBeTruthy()
    let writes = 0
    page.on('request', req => {
      if (req.method() === 'PUT' && req.url().endsWith('/vote-target')) writes++
    })
    await desktopSelection(page)
    const target = page.getByRole('checkbox', { name: '指定 ' + characterDisplayName(staleTarget.character) + '（' + staleTarget.playerNumber + '号）', exact: true })
    await target.click()
    await expect(page.getByRole('alert')).toContainText('已重新同步')
    await expect(target).not.toBeChecked()
    expect(writes).toBe(1)
    await expect(page.getByRole('checkbox', { name: '指定 ' + characterDisplayName(alive[0].character) + '（' + alive[0].playerNumber + '号）', exact: true })).toBeChecked()
  } finally { await release(request, id) }
})

test('固定种子 CO 中断显示提示、先读下午发言且刷新保留事件', async ({ page, request }) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  try {
    let interrupted = false
    for (let attempt = 0; attempt < 40; attempt++) {
      await readEvents(page)
      await desktopSelection(page)
      const candidate = page.locator('.player-row:not(.departed)')
        .filter({ hasNotText: '共有者' }).getByRole('checkbox').first()
      await expect(candidate).toBeVisible()
      if (!await candidate.isChecked()) {
        await candidate.click()
        await expect(candidate).toBeChecked()
      }
      const response = page.waitForResponse(res => res.request().method() === 'POST'
        && res.url().endsWith('/votes'))
      await desktopVote(page,'DESIGNATED')
      const result = await (await response).json()
      if (result.voteCompleted) continue
      interrupted = true
      expect(result.update.phase).toBe('DAY_DECISION')
      expect(result.update.presentedEvents.some(event => event.stage === 'AFTERNOON_DIALOGUE')).toBeTruthy()
      await expect(page.getByRole('alert')).toContainText('投票被回避 CO 中断')
      await expectNoEnabledVote(page)
      await expect(page.getByTestId('next-event')).toBeVisible()
      await completeLocalReading(page)
      const event = await page.getByRole('region', { name: '当前事件' }).textContent()
      const state = await saved(page)
      const reread = await (await request.get('/api/v1/games/' + id
        + '?eventCursor=' + result.update.eventCursor)).json()
      expect(reread).toEqual(result.update)
      await page.reload()
      await expect(page.getByRole('region', { name: '当前事件' })).toHaveText(event)
      expect(await saved(page)).toEqual(state)
      await expectNoEnabledVote(page)
      await readEvents(page)
      await expect(page.getByTestId('desktop-vote-window')).toHaveAttribute('data-command-blocked','false')
      break
    }
    expect(interrupted, 'seed 4202 must actually interrupt voting, not merely complete a game').toBeTruthy()
  } finally { await release(request, id) }
})

test('失效会话清除本地记录并可重新开局', async ({ page, request }) => {
  await start(page)
  const id = (await saved(page)).sessionId
  await release(request, id)
  await page.reload()
  await expect(page.getByRole('alert')).toContainText('已失效')
  await expect(page.getByRole('button', { name: '対局を始める' })).toBeVisible()
  expect(await saved(page)).toBeNull()
})

test('写入已完成但响应丢失时，再同期恢复结果而不重复提交', async ({ page, request }) => {
  await start(page)
  await readEvents(page)
  const id = (await saved(page)).sessionId
  try {
    await page.route('**/api/v1/games/**', async route => {
      await route.fetch()
      await route.abort('failed')
    })
    await desktopSelection(page)
    const target = page.locator('.player-row:not(.departed)').getByRole('checkbox').first()
    await target.click()
    await expect(page.getByRole('alert')).toContainText('连接未能确认')
    await expectNoEnabledVote(page)
    await page.unroute('**/api/v1/games/**')
    await page.getByRole('button', { name: '再同期' }).click()
    await expect(page.getByRole('alert')).toHaveCount(0)
    await expect(target).toBeEnabled()
    await expect(target).toBeChecked()
    expect((await saved(page)).sessionId).toBe(id)
  } finally { await release(request, id) }
})

test('显式释放后回到配役选择', async ({ page }) => {
  await start(page)
  await page.getByRole('button', { name: '試遊を終了' }).click()
  await page.getByRole('button', { name: '解放を確定' }).click()
  await expect(page.getByRole('button', { name: '対局を始める' })).toBeVisible()
  expect(await saved(page)).toBeNull()
})

test('按职业询问 CO 使用权威候补，未读时禁用且刷新恢复发言', async ({ page, request }, testInfo) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  try {
    const panel = page.getByTestId('desktop-vote-window')
    const ask = page.getByRole('button', { name: '狩人のCOを聞く', exact: true })
    await expectNoEnabledVote(page)
    await readEvents(page)
    await desktopClaims(page)
    await desktopClaims(page)
    await expect(ask).toBeEnabled()
    const before = await (await request.get('/api/v1/games/' + id)).json()
    let writes = 0
    page.on('request', req => { if (req.url().endsWith('/claims/by-role')) writes++ })
    const response = page.waitForResponse(res => res.url().endsWith('/claims/by-role'))
    await ask.click()
    const result = await (await response).json()
    expect(result.revision).toBe(before.revision + 1)
    expect(result.events.length).toBeGreaterThan(0)
    expect(result.view.players.some(player => player.claimedRole === 'lie')).toBeTruthy()
    await expect(page.getByTestId('next-event')).toBeVisible()
    await expectNoEnabledVote(page)
    await completeLocalReading(page)
    const event = await page.getByRole('region', { name: '当前事件' }).textContent()
    const state = await saved(page)
    await page.reload()
    await expect(page.getByRole('region', { name: '当前事件' })).toHaveText(event)
    expect(await saved(page)).toEqual(state)
    const restored = await (await request.get('/api/v1/games/' + id)).json()
    expect(restored.revision).toBe(result.revision)
    expect(restored.view.players).toEqual(result.view.players)
    await expectNoEnabledVote(page)
    await readEvents(page)
    await desktopClaims(page)
    await expect(ask).toBeEnabled()
    expect(writes).toBe(1)
    await page.screenshot({ path: testInfo.outputPath('role-claims.png'), fullPage: true })
  } finally { await release(request, id) }
})

test('职业 CO 的 409 只同步，不重放询问', async ({ page, request }) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  try {
    await readEvents(page)
    const before = await (await request.get('/api/v1/games/' + id)).json()
    const target = before.legalTargetSelections.find(action => action.type === 'VOTE').targets[0]
    const changed = await (await request.put('/api/v1/games/' + id + '/vote-target', {
      data: { expectedRevision: before.revision, target, selected: true },
    })).json()
    let writes = 0
    page.on('request', req => { if (req.url().endsWith('/claims/by-role')) writes++ })
    await desktopClaims(page)
    await page.getByRole('button', { name: '狩人のCOを聞く', exact: true }).click()
    await expect(page.getByRole('alert')).toContainText('已重新同步')
    expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual({
      ...changed, eventCursor: 0,
      events: before.events.concat(changed.events),
      presentedEvents: before.presentedEvents.concat(changed.presentedEvents),
    })
    expect(writes).toBe(1)
  } finally { await release(request, id) }
})

test('职业 CO 已执行但响应丢失，先同步且不重复询问', async ({ page, request }) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  try {
    await readEvents(page)
    const before = await (await request.get('/api/v1/games/' + id)).json()
    let writes = 0
    page.on('request', req => { if (req.url().endsWith('/claims/by-role')) writes++ })
    await page.route('**/claims/by-role', async route => {
      await route.fetch()
      await route.abort('failed')
    })
    const ask = page.getByRole('button', { name: '狩人のCOを聞く', exact: true })
    await desktopClaims(page)
    await ask.click()
    await expect(page.getByRole('alert')).toContainText('连接未能确认')
    await expectNoEnabledVote(page)
    const applied = await (await request.get('/api/v1/games/' + id)).json()
    expect(applied.revision).toBe(before.revision + 1)
    await page.unroute('**/claims/by-role')
    await page.getByRole('button', { name: '再同期' }).click()
    await expect(page.getByTestId('next-event')).toBeVisible()
    await expectNoEnabledVote(page)
    await readEvents(page)
    await desktopClaims(page)
    await expect(ask).toBeEnabled()
    expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual(applied)
    expect(writes).toBe(1)
  } finally { await release(request, id) }
})

test('按玩家批量询问 CO 保留选择顺序，恢复公开状态与未读事件', async ({ page, request }, testInfo) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  try {
    const panel = page.getByRole('region', { name: 'CO確認対象', exact:true })
    const ask = page.getByRole('button', { name:'戻る',exact:true })
    await expect(panel).toHaveCount(0)
    await readEvents(page)
    await desktopClaimPlayers(page)
    const before = await (await request.get('/api/v1/games/' + id)).json()
    await expect(panel.getByRole('checkbox')).toHaveCount(before.view.players.length)
    const targets = [9, before.claimInquiry.players[0]] // fixed-seed public player numbers, not actual-role reads
    for (const target of targets) {
      expect(before.claimInquiry.players).toContain(target)
      await panel.getByRole('checkbox', { name: new RegExp('^' + target + '番 ') }).check()
    }
    expect((await (await request.get('/api/v1/games/' + id)).json()).revision).toBe(before.revision)
    const response = page.waitForResponse(res => res.url().endsWith('/claims/by-players'))
    await ask.click()
    const applied = await response
    expect(applied.request().postDataJSON()).toEqual({ expectedRevision: before.revision, players: targets })
    const result = await applied.json()
    expect(result.revision).toBe(before.revision + 1)
    expect(result.events.length).toBeGreaterThan(0)
    expect(result.view.players.find(player => player.playerNumber === 9).claimedRole).toBe('lie')
    await expect(panel.getByRole('checkbox',{checked:true})).toHaveCount(0)
    await expect(panel.getByRole('checkbox', { checked: true })).toHaveCount(0)
    await completeLocalReading(page)
    const event = await page.getByRole('region', { name: '当前事件' }).textContent()
    const state = await saved(page)
    await page.reload()
    await expect(page.getByRole('region', { name: '当前事件' })).toHaveText(event)
    expect(await saved(page)).toEqual(state)
    const restored = await (await request.get('/api/v1/games/' + id)).json()
    expect(restored.revision).toBe(result.revision)
    expect(restored.view.players).toEqual(result.view.players)
    await readEvents(page)
    await desktopClaimPlayers(page)
    await expect(panel.locator('[role=checkbox]:not(:disabled)').first()).toBeEnabled()
    await expect(panel.getByRole('checkbox',{checked:true})).toHaveCount(0)
    await page.screenshot({ path: testInfo.outputPath('player-claims.png'), fullPage: true })
  } finally { await release(request, id) }
})

test('按玩家 CO 的未发送选择可取消，刷新不提交也不恢复草稿', async ({ page, request }) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  try {
    await readEvents(page)
    await desktopClaimPlayers(page)
    const panel = page.getByRole('region', { name: 'CO確認対象', exact:true })
    const ask = page.getByRole('button', { name:'戻る',exact:true })
    const target = panel.getByRole('checkbox').first()
    const before = await (await request.get('/api/v1/games/' + id)).json()
    let writes = 0
    page.on('request', req => { if (req.url().endsWith('/claims/by-players')) writes++ })
    await target.check()
    await expect(panel.getByRole('checkbox',{checked:true})).toHaveCount(1)
    await target.uncheck()
    await expect(panel.getByRole('checkbox',{checked:true})).toHaveCount(0)
    await target.check()
    await page.reload()
    await expect(page.getByTestId('desktop-vote-window')).toBeVisible()
    await desktopClaimPlayers(page)
    await expect(target).not.toBeChecked()
    await expect(panel.getByRole('checkbox',{checked:true})).toHaveCount(0)
    expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual(before)
    expect(Object.keys(await saved(page)).sort()).toEqual(['eventCursor', 'sessionId'])
    expect(writes).toBe(0)
  } finally { await release(request, id) }
})

test('按玩家 CO 冲突只同步并清理草稿，不重放请求', async ({ page, request }) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  try {
    await readEvents(page)
    await desktopClaimPlayers(page)
    const panel = page.getByRole('region', { name: 'CO確認対象', exact:true })
    await panel.getByRole('checkbox').first().check()
    const before = await (await request.get('/api/v1/games/' + id)).json()
    expect((await request.put('/api/v1/games/' + id + '/vote-target', {
      data: { expectedRevision: before.revision, target: before.claimInquiry.players[0], selected: true },
    })).ok()).toBeTruthy()
    const authority = await (await request.get('/api/v1/games/' + id)).json()
    let writes = 0
    page.on('request', req => { if (req.url().endsWith('/claims/by-players')) writes++ })
    await page.getByRole('button',{name:'戻る',exact:true}).click()
    await expect(page.getByRole('alert')).toContainText('已重新同步')
    await expect(panel.getByRole('checkbox', { checked: true })).toHaveCount(0)
    await expect(panel.getByRole('checkbox',{checked:true})).toHaveCount(0)
    expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual(authority)
    expect(writes).toBe(1)
  } finally { await release(request, id) }
})

test('按玩家 CO 已执行但丢失响应，先同步且不重复提交', async ({ page, request }) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  try {
    await readEvents(page)
    await desktopClaimPlayers(page)
    const panel = page.getByRole('region', { name: 'CO確認対象', exact:true })
    await panel.getByRole('checkbox', { name: /^9番 / }).check()
    const before = await (await request.get('/api/v1/games/' + id)).json()
    let writes = 0
    page.on('request', req => { if (req.url().endsWith('/claims/by-players')) writes++ })
    await page.route('**/claims/by-players', async route => { await route.fetch(); await route.abort('failed') })
    await page.getByRole('button',{name:'戻る',exact:true}).click()
    await expect(page.getByRole('alert')).toContainText('连接未能确认')
    await expect(panel.getByRole('checkbox',{checked:true})).toHaveCount(0)
    await expect(panel).toHaveCount(0)
    const applied = await (await request.get('/api/v1/games/' + id)).json()
    expect(applied.revision).toBe(before.revision + 1)
    await page.unroute('**/claims/by-players')
    await page.getByRole('button', { name: '再同期' }).click()
    await expect(page.getByTestId('next-event')).toBeVisible()
    await expect(panel.getByRole('checkbox', { checked: true })).toHaveCount(0)
    await readEvents(page)
    await desktopClaimPlayers(page)
    await expect(panel.locator('[role=checkbox]:not(:disabled)').first()).toBeEnabled()
    expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual(applied)
    expect(writes).toBe(1)
  } finally { await release(request, id) }
})

test('回避开关等待权威响应，刷新恢复且新局重置默认值', async ({ page, request }, testInfo) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  let nextId
  let proceed = () => {}
  try {
    const toggle = page.getByRole('switch', { name: '投票回避CO' })
    await expect(toggle).toHaveCount(0)
    await readEvents(page)
    await expect(toggle).toHaveAttribute('aria-checked', 'true')
    const before = await (await request.get('/api/v1/games/' + id)).json()
    const gate = new Promise(resolve => { proceed = resolve })
    await page.route('**/avoidance', async route => { await gate; await route.continue() })
    const sent = page.waitForRequest(req => req.method() === 'PUT' && req.url().endsWith('/avoidance'))
    const response = page.waitForResponse(res => res.url().endsWith('/avoidance'))
    await toggle.click()
    expect((await sent).postDataJSON()).toEqual({ expectedRevision: before.revision, enabled: false })
    await expect(toggle).toBeDisabled()
    await expect(toggle).toHaveAttribute('aria-checked', 'true')
    proceed()
    const changed = await (await response).json()
    expect(changed.revision).toBe(before.revision + 1)
    expect(changed.selections.avoidanceEnabled).toBe(false)
    await expect(toggle).toHaveAttribute('aria-checked', 'false')
    await page.unroute('**/avoidance')
    await page.reload()
    await expect(toggle).toHaveAttribute('aria-checked', 'false')
    await expect(toggle).toBeEnabled()
    expect((await (await request.get('/api/v1/games/' + id)).json()).revision).toBe(changed.revision)
    await page.screenshot({ path: testInfo.outputPath('avoidance.png'), fullPage: true })
    await toggle.click()
    await expect(toggle).toHaveAttribute('aria-checked', 'true')
    await release(request, id)
    await start(page, '通常')
    nextId = (await saved(page)).sessionId
    expect(nextId).not.toBe(id)
    await expect(toggle).toHaveCount(0)
  } finally { proceed(); await release(request, id); await release(request, nextId) }
})

test('回避开关旧版本只同步权威值，不自动重放切换', async ({ page, request }) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  try {
    await readEvents(page)
    const before = await (await request.get('/api/v1/games/' + id)).json()
    expect((await request.put('/api/v1/games/' + id + '/avoidance', {
      data: { expectedRevision: before.revision, enabled: false },
    })).ok()).toBeTruthy()
    const authority = await (await request.get('/api/v1/games/' + id)).json()
    let writes = 0
    page.on('request', req => { if (req.method() === 'PUT' && req.url().endsWith('/avoidance')) writes++ })
    const toggle = page.getByRole('switch', { name: '投票回避CO' })
    await toggle.click()
    await expect(page.getByRole('alert')).toContainText('已重新同步')
    await expect(toggle).toHaveAttribute('aria-checked', 'false')
    expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual(authority)
    expect(writes).toBe(1)
  } finally { await release(request, id) }
})

test('回避设置已执行但响应丢失，暂停操作并同步权威值', async ({ page, request }) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  try {
    await readEvents(page)
    const before = await (await request.get('/api/v1/games/' + id)).json()
    let writes = 0
    page.on('request', req => { if (req.method() === 'PUT' && req.url().endsWith('/avoidance')) writes++ })
    await page.route('**/avoidance', async route => { await route.fetch(); await route.abort('failed') })
    const toggle = page.getByRole('switch', { name: '投票回避CO' })
    await toggle.click()
    await expect(page.getByRole('alert')).toContainText('连接未能确认')
    await expect(toggle).toHaveAttribute('aria-checked', 'true')
    await expect(toggle).toBeDisabled()
    const applied = await (await request.get('/api/v1/games/' + id)).json()
    expect(applied.revision).toBe(before.revision + 1)
    expect(applied.selections.avoidanceEnabled).toBe(false)
    await page.unroute('**/avoidance')
    await page.getByRole('button', { name: '再同期' }).click()
    await expect(toggle).toHaveAttribute('aria-checked', 'false')
    await expect(toggle).toBeEnabled()
    expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual(applied)
    expect(writes).toBe(1)
  } finally { await release(request, id) }
})

for (const method of ['FREE', 'GREY']) {
  test(method + ' 按权威候补完成真实对局，刷新保留事件与票型', async ({ page, request }, testInfo) => {
    await start(page, '通常')
    const id = (await saved(page)).sessionId
    try {
      await expectNoEnabledVote(page)
      let turns = 0
      let preferredVotes = 0
      let unavailableGrey = 0
      while (true) {
        await readEvents(page)
        const before = await (await request.get('/api/v1/games/' + id)).json()
        if (before.phase === 'FINISHED') break
        expect(++turns).toBeLessThan(80)
        const action = before.legalVoteActions.find(action => action.method === method)
          || before.legalVoteActions.find(action => action.method === 'FREE')
        if (action.method === method) preferredVotes++
        if (!before.legalVoteActions.some(action => action.method === 'GREY')) {
          unavailableGrey++
          await expect(page.getByTestId('vote-GREY')).toHaveCount(0)
          // The desktop menu omits unavailable methods rather than adding a Web explanation.
        }
        const response = page.waitForResponse(res => res.request().method() === 'POST' && res.url().endsWith('/votes'))
        await desktopVote(page,action.method)
        const voted = await response
        expect(voted.request().postDataJSON()).toEqual({
          expectedRevision: before.revision, method: action.method, candidates: action.candidates,
        })
        expect(voted.ok()).toBeTruthy()
        const result = await voted.json()
        expect(result.voteCompleted).toBe(true)
        expect(result.update.voteHistory.at(-1).method).toBe(action.method)
        await expect(page.getByTestId('next-event')).toBeVisible()
        if (turns === 1) {
          await completeLocalReading(page)
          const event = await page.getByRole('region', { name: '当前事件' }).textContent()
          await page.reload()
          await expect(page.getByRole('region', { name: '当前事件' })).toHaveText(event)
          await expectNoEnabledVote(page)
          expect((await (await request.get('/api/v1/games/' + id)).json()).voteHistory).toEqual(result.update.voteHistory)
        }
      }
      expect(preferredVotes).toBeGreaterThan(0)
      if (method === 'GREY') expect(unavailableGrey).toBeGreaterThan(0)
      await expect(page.getByRole('region', { name: '自由・グレー投票' })).toHaveCount(0)
      expect(Object.keys(await saved(page)).sort()).toEqual(['eventCursor', 'sessionId'])
      await page.screenshot({ path: testInfo.outputPath(method + '-finished.png'), fullPage: true })
    } finally { await release(request, id) }
  })

  test(method + ' 旧版本只同步不重复投票', async ({ page, request }, testInfo) => {
    await start(page, '通常')
    const id = (await saved(page)).sessionId
    try {
      await readEvents(page)
      const before = await (await request.get('/api/v1/games/' + id)).json()
      expect(before.legalVoteActions.some(action => action.method === method)).toBe(true)
      const changed = await request.put('/api/v1/games/' + id + '/avoidance', {
        data: { expectedRevision: before.revision, enabled: false },
      })
      expect(changed.ok()).toBeTruthy()
      const authority = await (await request.get('/api/v1/games/' + id)).json()
      let writes = 0
      page.on('request', req => { if (req.method() === 'POST' && req.url().endsWith('/votes')) writes++ })
      await desktopVote(page,method)
      await expect(page.getByRole('alert')).toContainText('已重新同步')
      await expect(page.getByTestId('desktop-vote-window')).toHaveAttribute('data-command-blocked','false')
      expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual(authority)
      expect(writes).toBe(1)
      if (method === 'GREY') await page.screenshot({ path: testInfo.outputPath('alternative-votes.png'), fullPage: true })
    } finally { await release(request, id) }
  })

  test(method + ' 已执行失响应后先同步，不自动重放', async ({ page, request }) => {
    await start(page, '通常')
    const id = (await saved(page)).sessionId
    try {
      await readEvents(page)
      const before = await (await request.get('/api/v1/games/' + id)).json()
      expect(before.legalVoteActions.some(action => action.method === method)).toBe(true)
      let writes = 0
      page.on('request', req => { if (req.method() === 'POST' && req.url().endsWith('/votes')) writes++ })
      await page.route('**/votes', async route => { await route.fetch(); await route.abort('failed') })
      await desktopVote(page,method)
      await expect(page.getByRole('alert')).toContainText('连接未能确认')
      await expectNoEnabledVote(page)
      const applied = await (await request.get('/api/v1/games/' + id)).json()
      expect(applied.revision).toBe(before.revision + 1)
      expect(applied.voteHistory.at(-1).method).toBe(method)
      await page.unroute('**/votes')
      await page.getByRole('button', { name: '再同期' }).click()
      await expect(page.getByTestId('next-event')).toBeVisible()
      expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual(applied)
      expect(writes).toBe(1)
    } finally { await release(request, id) }
  })
}

for (const { type, skill, label } of [
  { type: 'DIVINATION', skill: 'divination', label: '全員の占い予約' },
  { type: 'GUARD', skill: 'guard', label: '全員の護衛予約' },
]) {
  const otherSkill = skill === 'guard' ? 'divination' : 'guard'
  test(label + ' 全体技能预约等待权威值，支持多目标、取消与刷新', async ({ page, request }, testInfo) => {
    await start(page, '通常')
    const id = (await saved(page)).sessionId
    let proceed = () => {}
    try {
      const panel = page.getByRole('region', { name: skill==='divination'?'占い指定':'護衛指定',exact:true })
      await expect(panel).toHaveCount(0)
      await readEvents(page)
      await desktopSkill(page,otherSkill)
      const otherPanel = page.getByRole('region', { name: otherSkill==='divination'?'占い指定':'護衛指定',exact:true })
      await otherPanel.getByRole('checkbox').nth(2).click()
      await expect(otherPanel.getByRole('checkbox').nth(2)).toBeChecked()
      await desktopSkill(page,skill)
      const before = await (await request.get('/api/v1/games/' + id)).json()
      const targets = before.legalTargetSelections.find(action => action.type === type).targets
      expect(await panel.getByRole('checkbox').count()).toBe(before.view.players.length)
      const first = panel.getByRole('checkbox').nth(targets[0]-1)
      const second = panel.getByRole('checkbox').nth(targets[1]-1)
      const gate = new Promise(resolve => { proceed = resolve })
      await page.route('**/target-selections', async route => { await gate; await route.continue() })
      const sent = page.waitForRequest(req => req.method() === 'PUT' && req.url().endsWith('/target-selections'))
      const response = page.waitForResponse(res => res.url().endsWith('/target-selections'))
      await first.click()
      expect((await sent).postDataJSON()).toEqual({ expectedRevision: before.revision,
        type, target: targets[0], selected: true })
      await expect(first).not.toBeChecked()
      await expect(second).toBeDisabled()
      proceed()
      const applied = await (await response).json()
      expect(applied.selections[skill].hiddenTargets).toEqual([targets[0]])
      for (const actor of applied.selections[skill].claimedActors) expect(actor.targets).toContain(targets[0])
      await expect(first).toBeChecked()
      await page.unroute('**/target-selections')
      await second.click()
      await expect(second).toBeChecked()
      await first.click()
      await expect(first).not.toBeChecked()
      await expect(second).toBeChecked()
      await page.reload()
      await expect(page.getByTestId('desktop-vote-window')).toBeVisible()
      await desktopSkill(page,skill)
      await expect(first).not.toBeChecked()
      await expect(second).toBeChecked()
      expect(Object.keys(await saved(page)).sort()).toEqual(['eventCursor', 'sessionId'])
      const after = await (await request.get('/api/v1/games/' + id)).json()
      expect(after.selections[skill].hiddenTargets).toEqual([targets[1]])
      expect(after.selections[otherSkill]).toEqual(before.selections[otherSkill])
      expect(after.selections.voteTargets).toEqual(before.selections.voteTargets)
      await page.screenshot({ path: testInfo.outputPath(skill + '.png'), fullPage: true })
    } finally { proceed(); await release(request, id) }
  })

  test(label + ' 技能预约随真实投票跨日更新，不把旧预约保留到次日或终局', async ({ page, request }) => {
    await start(page, '通常')
    const id = (await saved(page)).sessionId
    try {
      await readEvents(page)
      await desktopSkill(page,skill)
      const panel = page.getByRole('region', { name: skill==='divination'?'占い指定':'護衛指定',exact:true })
      await panel.getByRole('checkbox').first().click()
      await expect(panel.getByRole('checkbox').first()).toBeChecked()
      await desktopSkill(page,skill)
      const before = await (await request.get('/api/v1/games/' + id)).json()
      let turns = 0
      while (true) {
        const response = page.waitForResponse(res => res.request().method() === 'POST' && res.url().endsWith('/votes'))
        await desktopVote(page,'FREE')
        const voted = await (await response).json()
        expect(++turns).toBeLessThan(80)
        await readEvents(page)
        if (voted.update.phase === 'FINISHED') break
        expect(voted.update.view.gameDay).toBeGreaterThan(before.view.gameDay)
        expect(voted.update.selections[skill].hiddenTargets).toEqual([])
        await desktopSkill(page,skill)
        for (const checkbox of await panel.getByRole('checkbox').all()) await expect(checkbox).not.toBeChecked()
        await page.reload()
        await expect(page.getByTestId('desktop-vote-window')).toBeVisible()
        await desktopSkill(page,skill)
        await expect(panel.locator('[role=checkbox]:not(:disabled)').first()).toBeEnabled()
      }
      await expect(panel).toHaveCount(0)
      await page.reload()
      await expect(page.getByRole('button', { name: '新しい対局' })).toBeVisible()
      await expect(panel).toHaveCount(0)
    } finally { await release(request, id) }
  })

  test(label + ' 技能预约409只同步权威目标，不重放', async ({ page, request }) => {
    await start(page, '通常')
    const id = (await saved(page)).sessionId
    try {
      await readEvents(page)
      await desktopSkill(page,skill)
      const before = await (await request.get('/api/v1/games/' + id)).json()
      const targets = before.legalTargetSelections.find(action => action.type === type).targets
      expect((await request.put('/api/v1/games/' + id + '/target-selections', { data: {
        expectedRevision: before.revision, type, target: targets[1], selected: true,
      } })).ok()).toBeTruthy()
      const authority = await (await request.get('/api/v1/games/' + id)).json()
      let writes = 0
      page.on('request', req => { if (req.method() === 'PUT' && req.url().endsWith('/target-selections')) writes++ })
      const boxes = page.getByRole('region', { name: skill==='divination'?'占い指定':'護衛指定',exact:true }).getByRole('checkbox')
      await boxes.nth(0).click()
      await expect(page.getByRole('alert')).toContainText('已重新同步')
      await expect(boxes.nth(0)).not.toBeChecked()
      await expect(boxes.nth(1)).toBeChecked()
      expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual(authority)
      expect(writes).toBe(1)
    } finally { await release(request, id) }
  })

  test(label + ' 技能预约已执行失响应后暂停，先同步不重复预约', async ({ page, request }) => {
    await start(page, '通常')
    const id = (await saved(page)).sessionId
    try {
      await readEvents(page)
      await desktopSkill(page,skill)
      const before = await (await request.get('/api/v1/games/' + id)).json()
      let writes = 0
      page.on('request', req => { if (req.method() === 'PUT' && req.url().endsWith('/target-selections')) writes++ })
      await page.route('**/target-selections', async route => { await route.fetch(); await route.abort('failed') })
      const checkbox = page.getByRole('region', { name: skill==='divination'?'占い指定':'護衛指定',exact:true }).getByRole('checkbox').first()
      await checkbox.click()
      await expect(page.getByRole('alert')).toContainText('连接未能确认')
      await expect(checkbox).not.toBeChecked()
      await expect(checkbox).toBeDisabled()
      const authority = await (await request.get('/api/v1/games/' + id)).json()
      expect(authority.revision).toBe(before.revision + 1)
      expect(authority.selections[skill].hiddenTargets.length).toBe(1)
      await page.unroute('**/target-selections')
      await page.getByRole('button', { name: '再同期' }).click()
      await expect(checkbox).toBeChecked()
      await expect(checkbox).toBeEnabled()
      expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual(authority)
      expect(writes).toBe(1)
    } finally { await release(request, id) }
  })
}

async function publicSkill(page, request, id, type) {
  await expectNoEnabledVote(page)
  await readEvents(page)
  await desktopClaims(page)
  const response = page.waitForResponse(res => res.url().endsWith('/claims/by-role'))
  await page.getByRole('button', { name: type === 'DIVINATION' ? '占い師のCOを聞く' : '狩人のCOを聞く', exact: true }).click()
  expect((await response).ok()).toBeTruthy()
  await readEvents(page)
  const snapshot = await (await request.get('/api/v1/games/' + id)).json()
  const action = snapshot.legalTargetSelections.find(action => action.type === type)
  expect(action.claimedActors.length).toBeGreaterThan(0)
  const actor=action.claimedActors[0].playerNumber
  const skill=type==='DIVINATION'?'divination':'guard'
  const actorName=(await import('../src/characterDisplay.js')).characters
  // Tool names are public character short names; never derive them from actual roles.
  const player=snapshot.view.players.find(p=>p.playerNumber===actor)
  const name=actorName[player.character]?.shortName
  expect(name).toBeTruthy()
  await desktopSkill(page,skill,name)
  return { snapshot, action, actor, name, skill }
}

for (const type of ['DIVINATION','GUARD']) {
 test(type+' 個体予約は他者・潜伏予約を変えず、応答待ち・取消・再読込に対応',async({page,request},testInfo)=>{
  await start(page,'通常');const id=(await saved(page)).sessionId;let proceed=()=>{}
  try{
   const {snapshot,action,actor,name,skill}=await publicSkill(page,request,id,type)
   const scene=page.getByRole('region',{name:skill==='divination'?'占い指定':'護衛指定',exact:true})
   const boxes=scene.getByRole('checkbox');expect(await boxes.count()).toBe(snapshot.view.players.length)
   const first=boxes.nth(action.targets[0]-1),second=boxes.nth(action.targets[1]-1)
   const gate=new Promise(resolve=>{proceed=resolve})
   await page.route('**/claimed-target-selections',async route=>{await gate;await route.continue()})
   const sent=page.waitForRequest(req=>req.method()==='PUT'&&req.url().endsWith('/claimed-target-selections'))
   const response=page.waitForResponse(res=>res.url().endsWith('/claimed-target-selections'))
   await first.click()
   expect((await sent).postDataJSON()).toEqual({expectedRevision:snapshot.revision,type,claimedPlayer:actor,target:action.targets[0],selected:true})
   await expect(first).not.toBeChecked();await expect(second).toBeDisabled();proceed()
   const applied=await(await response).json();await expect(first).toBeChecked()
   expect(applied.selections[skill].hiddenTargets).toEqual(snapshot.selections[skill].hiddenTargets)
   expect(applied.selections[skill].claimedActors.filter(a=>a.playerNumber!==actor)).toEqual(snapshot.selections[skill].claimedActors.filter(a=>a.playerNumber!==actor))
   expect(applied.selections[skill==='guard'?'divination':'guard']).toEqual(snapshot.selections[skill==='guard'?'divination':'guard'])
   await page.unroute('**/claimed-target-selections');await second.click();await expect(second).toBeChecked()
   await first.click();await expect(first).not.toBeChecked()
   await page.reload();await expect(page.getByTestId('desktop-vote-window')).toBeVisible();await desktopSkill(page,skill,name)
   await expect(second).toBeChecked();await expect(first).not.toBeChecked()
   expect(Object.keys(await saved(page)).sort()).toEqual(['eventCursor','sessionId'])
   await page.screenshot({path:testInfo.outputPath('claimed-'+skill+'.png'),fullPage:true})
  }finally{proceed();await release(request,id)}
 })
 test(type+' 個体設定と全体上書き・取消を組み合わせ、跨日と終局を復元',async({page,request})=>{
  await start(page,'通常');const id=(await saved(page)).sessionId
  try{
   const {action,name,actor,skill}=await publicSkill(page,request,id,type)
   if(type==='DIVINATION')expect(action.claimedActors.length).toBeGreaterThan(1)
   const boxes=page.getByRole('region',{name:skill==='divination'?'占い指定':'護衛指定',exact:true}).getByRole('checkbox')
   const first=boxes.nth(action.targets[0]-1),second=boxes.nth(action.targets[1]-1)
   await first.click();await expect(first).toBeChecked();await second.click();await expect(second).toBeChecked()
   await desktopSkill(page,skill);await expect(first).toBeChecked();await first.click();await expect(first).not.toBeChecked()
   await desktopSkill(page,skill,name);await expect(second).toBeChecked()
   await desktopSkill(page,skill);await first.click();await expect(first).toBeChecked()
   await desktopSkill(page,skill,name);await first.click();await expect(first).not.toBeChecked()
   await desktopSkill(page,skill);await expect(first).toBeChecked();await first.click();await expect(first).not.toBeChecked()
   const composed=await(await request.get('/api/v1/games/'+id)).json()
   expect(composed.selections[skill].hiddenTargets).not.toContain(action.targets[0])
   for(const value of composed.selections[skill].claimedActors)expect(value.targets).not.toContain(action.targets[0])
   expect(composed.selections[skill].claimedActors.find(a=>a.playerNumber===actor).targets).toContain(action.targets[1])
   let turns=0
   while(true){
    const response=page.waitForResponse(res=>res.request().method()==='POST'&&res.url().endsWith('/votes'))
    await desktopVote(page,'FREE');const result=await(await response).json();await readEvents(page);expect(++turns).toBeLessThan(80)
    if(result.update.phase==='FINISHED')break
    await desktopSkill(page,skill)
    for(const box of await boxes.all())await expect(box).not.toBeChecked()
    await page.reload();await expect(page.getByTestId('desktop-vote-window')).toHaveAttribute('data-command-blocked','false')
   }
   await expect(page.getByTestId('desktop-vote-window')).toHaveCount(0)
   await page.reload();await expect(page.getByRole('button',{name:'新しい対局'})).toBeVisible()
  }finally{await release(request,id)}
 })
 test(type+' 409は同期のみで個体予約を再送しない',async({page,request})=>{
  await start(page,'通常');const id=(await saved(page)).sessionId
  try{
   const {snapshot,action,actor,skill}=await publicSkill(page,request,id,type)
   expect((await request.put('/api/v1/games/'+id+'/claimed-target-selections',{data:{expectedRevision:snapshot.revision,type,claimedPlayer:actor,target:action.targets[1],selected:true}})).ok()).toBeTruthy()
   const authority=await(await request.get('/api/v1/games/'+id)).json();let writes=0
   page.on('request',req=>{if(req.method()==='PUT'&&req.url().endsWith('/claimed-target-selections'))writes++})
   const boxes=page.getByRole('region',{name:skill==='divination'?'占い指定':'護衛指定',exact:true}).getByRole('checkbox')
   await boxes.nth(action.targets[0]-1).click();await expect(page.getByRole('alert')).toContainText('已重新同步')
   await expect(boxes.nth(action.targets[0]-1)).not.toBeChecked();await expect(boxes.nth(action.targets[1]-1)).toBeChecked()
   expect(await(await request.get('/api/v1/games/'+id)).json()).toEqual(authority);expect(writes).toBe(1)
  }finally{await release(request,id)}
 })
 test(type+' 実行済み応答喪失は同期して復元し、再送しない',async({page,request})=>{
  await start(page,'通常');const id=(await saved(page)).sessionId
  try{
   const {snapshot,action,skill}=await publicSkill(page,request,id,type);let writes=0
   page.on('request',req=>{if(req.method()==='PUT'&&req.url().endsWith('/claimed-target-selections'))writes++})
   await page.route('**/claimed-target-selections',async route=>{await route.fetch();await route.abort('failed')})
   const box=page.getByRole('region',{name:skill==='divination'?'占い指定':'護衛指定',exact:true}).getByRole('checkbox').nth(action.targets[0]-1)
   await box.click();await expect(page.getByRole('alert')).toContainText('连接未能确认');await expect(box).not.toBeChecked();await expect(box).toBeDisabled()
   const authority=await(await request.get('/api/v1/games/'+id)).json();expect(authority.revision).toBe(snapshot.revision+1)
   await page.unroute('**/claimed-target-selections');await page.getByRole('button',{name:'再同期'}).click()
   await expect(box).toBeChecked();await expect(box).toBeEnabled()
   expect(await(await request.get('/api/v1/games/'+id)).json()).toEqual(authority);expect(writes).toBe(1)
  }finally{await release(request,id)}
 })
}

test('安全公开投影与夜死分类在刷新后保持一致', async ({ page, request }) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  try {
    const before = await (await request.get('/api/v1/games/' + id)).json()
    expect(before.phase).toBe('DAY_DECISION')
    expect(before.view.players.some(player => player.deathReason === 'NIGHT_DEATH')).toBe(true)
    expect(JSON.stringify(before)).not.toMatch(/beiyao|nightmaozhou|nighthouzhui|zhousha/)
    expect(before.publicInformation.suspicion).toEqual({ available: false, rows: [] })
    await page.reload()
    await expect(page.getByRole('region', { name: '当前事件' })).toBeVisible()
    expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual(before)
    expect(Object.keys(await saved(page)).sort()).toEqual(['eventCursor', 'sessionId'])
  } finally { await release(request, id) }
})

test('手机视口无横向溢出，配役与主要操作仍可访问', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/')
  await expect(page.getByRole('radio')).toHaveCount(7)
  await expect(page.getByRole('button', { name: '対局を始める' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390)
  await page.screenshot({ path: testInfo.outputPath('mobile.png'), fullPage: true })
})
