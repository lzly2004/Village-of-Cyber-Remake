import { test, expect } from '@playwright/test'

const storageKey = 'vocr.guest.v1'

async function saved(page) {
  return page.evaluate(key => JSON.parse(sessionStorage.getItem(key)), storageKey)
}

async function start(page, configuration = '简易') {
  await page.goto('/')
  await page.getByRole('radio', { name: configuration + '配役' }).check()
  await page.getByRole('button', { name: '开始对局' }).click()
  await expect(page.getByRole('region', { name: '当前事件' })).toBeVisible()
}

async function readEvents(page) {
  let guard = 0
  while (await page.getByTestId('next-event').count()) {
    expect(++guard).toBeLessThan(120)
    await page.getByTestId('next-event').click()
  }
}

async function release(request, id) {
  if (id) await request.delete('/api/v1/games/' + id)
}

test('选择配役、逐条阅读与刷新恢复已确认游标', async ({ page, request }) => {
  await start(page, '猫又')
  const id = (await saved(page)).sessionId
  try {
    await expect(page.getByRole('heading', { level: 1 })).toContainText('猫又村')
    await expect(page.getByTestId('vote')).toBeDisabled()
    await page.getByTestId('next-event').click()
    await expect.poll(async () => (await saved(page)).eventCursor).toBe(1)
    const event = await page.getByRole('region', { name: '当前事件' }).textContent()
    await page.reload()
    await expect(page.getByRole('region', { name: '当前事件' })).toHaveText(event)
    expect((await saved(page)).sessionId).toBe(id)
    expect(Object.keys(await saved(page)).sort()).toEqual(['eventCursor', 'sessionId'])
    await readEvents(page)
    await expect(page.getByRole('heading', { name: '今天，投给谁？' })).toBeVisible()
  } finally { await release(request, id) }
})

test('真实对局从浏览器指定投票直至终局，并刷新恢复揭晓', async ({ page, request }, testInfo) => {
  await start(page)
  const id = (await saved(page)).sessionId
  try {
    let rounds = 0
    while (true) {
      await readEvents(page)
      if (await page.getByRole('heading', { name: /阵营胜利|妖狐胜利/ }).count()) break
      expect(++rounds).toBeLessThan(60)
      // 只根据页面的公开 CO 选择候补；共有者回避后取消该目标。
      for (const checkbox of await page.locator('.player-row').filter({ hasText: '共有者' })
        .getByRole('checkbox').all()) {
        if (await checkbox.isChecked() && await checkbox.isEnabled()) {
          await checkbox.click()
          await expect(checkbox).not.toBeChecked()
        }
      }
      const candidate = page.locator('.player-row:not(.departed)')
        .filter({ hasNotText: '共有者' }).getByRole('checkbox').first()
      if (!await candidate.isChecked()) {
        await candidate.click()
        await expect(candidate).toBeChecked()
      }
      await expect(page.getByTestId('vote')).toBeEnabled()
      await page.getByTestId('vote').click()
      await expect(page.getByTestId('next-event')).toBeVisible()
    }
    await expect(page.locator('.player-identity').first()).toContainText('真实身份')
    await expect(page.getByRole('region', { name: '最近投票结果' })).toBeVisible()
    await page.screenshot({ path: testInfo.outputPath('finished.png'), fullPage: true })
    await page.reload()
    await expect(page.getByRole('heading', { name: /阵营胜利|妖狐胜利/ })).toBeVisible()
    await expect(page.getByRole('button', { name: '新开一局' })).toBeVisible()
    await expect(page.getByRole('region', { name: '職業別CO確認' })).toHaveCount(0)
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
    const target = page.getByRole('checkbox', { name: '指定 ' + staleTarget.character + '（' + staleTarget.playerNumber + '号）', exact: true })
    await target.click()
    await expect(page.getByRole('alert')).toContainText('已重新同步')
    await expect(target).not.toBeChecked()
    expect(writes).toBe(1)
    await expect(page.getByRole('checkbox', { name: '指定 ' + alive[0].character + '（' + alive[0].playerNumber + '号）', exact: true })).toBeChecked()
  } finally { await release(request, id) }
})

test('固定种子 CO 中断显示提示、先读下午发言且刷新保留事件', async ({ page, request }) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  try {
    let interrupted = false
    for (let attempt = 0; attempt < 40; attempt++) {
      await readEvents(page)
      const candidate = page.locator('.player-row:not(.departed)')
        .filter({ hasNotText: '共有者' }).getByRole('checkbox').first()
      await expect(candidate).toBeVisible()
      if (!await candidate.isChecked()) {
        await candidate.click()
        await expect(candidate).toBeChecked()
      }
      const response = page.waitForResponse(res => res.request().method() === 'POST'
        && res.url().endsWith('/votes'))
      await page.getByTestId('vote').click()
      const result = await (await response).json()
      if (result.voteCompleted) continue
      interrupted = true
      expect(result.update.phase).toBe('DAY_DECISION')
      expect(result.update.presentedEvents.some(event => event.stage === 'AFTERNOON_DIALOGUE')).toBeTruthy()
      await expect(page.getByRole('alert')).toContainText('投票被回避 CO 中断')
      await expect(page.getByTestId('vote')).toBeDisabled()
      await expect(page.getByTestId('next-event')).toBeVisible()
      const event = await page.getByRole('region', { name: '当前事件' }).textContent()
      const state = await saved(page)
      const reread = await (await request.get('/api/v1/games/' + id
        + '?eventCursor=' + result.update.eventCursor)).json()
      expect(reread).toEqual(result.update)
      await page.reload()
      await expect(page.getByRole('region', { name: '当前事件' })).toHaveText(event)
      expect(await saved(page)).toEqual(state)
      await expect(page.getByTestId('vote')).toBeDisabled()
      await readEvents(page)
      await expect(page.getByTestId('vote')).toBeEnabled()
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
  await expect(page.getByRole('button', { name: '开始对局' })).toBeVisible()
  expect(await saved(page)).toBeNull()
})

test('写入已完成但响应丢失时，重新同步恢复结果而不重复提交', async ({ page, request }) => {
  await start(page)
  await readEvents(page)
  const id = (await saved(page)).sessionId
  try {
    await page.route('**/api/v1/games/**', async route => {
      await route.fetch()
      await route.abort('failed')
    })
    const target = page.locator('.player-row:not(.departed)').getByRole('checkbox').first()
    await target.click()
    await expect(page.getByRole('alert')).toContainText('连接未能确认')
    await expect(page.getByTestId('vote')).toBeDisabled()
    await page.unroute('**/api/v1/games/**')
    await page.getByRole('button', { name: '重新同步' }).click()
    await expect(page.getByRole('alert')).toHaveCount(0)
    await expect(target).toBeEnabled()
    await expect(target).toBeChecked()
    expect((await saved(page)).sessionId).toBe(id)
  } finally { await release(request, id) }
})

test('显式释放后回到配役选择', async ({ page }) => {
  await start(page)
  await page.getByRole('button', { name: '结束本次试玩' }).click()
  await page.getByRole('button', { name: '确认释放' }).click()
  await expect(page.getByRole('button', { name: '开始对局' })).toBeVisible()
  expect(await saved(page)).toBeNull()
})

test('按职业询问 CO 使用权威候补，未读时禁用且刷新恢复发言', async ({ page, request }, testInfo) => {
  await start(page, '通常')
  const id = (await saved(page)).sessionId
  try {
    const panel = page.getByRole('region', { name: '職業別CO確認' })
    const ask = page.getByRole('button', { name: '狩人のCOを聞く', exact: true })
    await expect(panel.getByRole('button')).toHaveCount(5)
    await expect(ask).toBeDisabled()
    await readEvents(page)
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
    await expect(ask).toBeDisabled()
    const event = await page.getByRole('region', { name: '当前事件' }).textContent()
    const state = await saved(page)
    await page.reload()
    await expect(page.getByRole('region', { name: '当前事件' })).toHaveText(event)
    expect(await saved(page)).toEqual(state)
    const restored = await (await request.get('/api/v1/games/' + id)).json()
    expect(restored.revision).toBe(result.revision)
    expect(restored.view.players).toEqual(result.view.players)
    await expect(ask).toBeDisabled()
    await readEvents(page)
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
    await ask.click()
    await expect(page.getByRole('alert')).toContainText('连接未能确认')
    await expect(ask).toBeDisabled()
    const applied = await (await request.get('/api/v1/games/' + id)).json()
    expect(applied.revision).toBe(before.revision + 1)
    await page.unroute('**/claims/by-role')
    await page.getByRole('button', { name: '重新同步' }).click()
    await expect(page.getByTestId('next-event')).toBeVisible()
    await expect(ask).toBeDisabled()
    await readEvents(page)
    await expect(ask).toBeEnabled()
    expect(await (await request.get('/api/v1/games/' + id)).json()).toEqual(applied)
    expect(writes).toBe(1)
  } finally { await release(request, id) }
})

test('手机视口无横向溢出，配役与主要操作仍可访问', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/')
  await expect(page.getByRole('radio')).toHaveCount(7)
  await expect(page.getByRole('button', { name: '开始对局' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390)
  await page.screenshot({ path: testInfo.outputPath('mobile.png'), fullPage: true })
})
