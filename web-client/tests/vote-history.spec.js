import { characterDisplayName } from '../src/characterDisplay.js'
import { test, expect } from '@playwright/test'

const key = 'vocr.guest.v1'
const saved = page => page.evaluate(key => JSON.parse(sessionStorage.getItem(key)), key)
const read = async (request, id) => (await request.get('/api/v1/games/' + id)).json()
const playerName = (snapshot, number) => number + ' · ' + characterDisplayName(snapshot.view.players.find(player => player.playerNumber === number).character)
async function start(page) {
  await page.goto('/')
  await page.getByRole('radio', { name: '通常配役' }).check()
  await page.getByRole('button', { name: '対局を始める' }).click()
  await expect(page.getByTestId('next-event')).toBeVisible()
  return (await saved(page)).sessionId
}
async function drain(page) {
  let guard = 0
  while (await page.getByTestId('next-event').count()) {
    expect(++guard).toBeLessThan(120)
    await page.getByTestId('next-event').click()
  }
}
async function expectHistory(page, snapshot) {
  const panel = page.getByRole('region', { name: '最近投票结果', exact: true })
  if (!snapshot.voteHistory.length) { await expect(panel).toHaveCount(0); return }
  await expect(panel.locator('details')).toHaveCount(snapshot.voteHistory.length)
  for (const vote of snapshot.voteHistory) {
    const day = page.getByTestId('vote-day-' + vote.day)
    if (!await day.evaluate(element => element.open)) await day.locator('summary').click()
    await expect(day.locator('summary')).toContainText(
      ({ DESIGNATED: '指定投票', FREE: '自由投票', GREY: 'グレー投票' })[vote.method] || '未対応の投票方式')
    await expect(day.locator('.history-candidates')).toHaveText('候補：' + vote.candidates.map(n => playerName(snapshot, n)).join('・'))
    await expect(day.locator('.history-round')).toHaveCount(vote.rounds.length)
    for (const round of vote.rounds) {
      const section = page.getByTestId(`vote-round-${vote.day}-${round.round}`)
      await expect(section.locator('.round-summary')).toHaveText(`${round.maximumVotes}票：`
        + round.leaders.map(n => playerName(snapshot, n)).join('・')
        + (round.leaders.length > 1 ? '（同票・再投票）' : '（処刑）'))
      const rows = section.locator('tbody tr')
      await expect(rows).toHaveCount(round.lines.length)
      for (const [index, line] of round.lines.entries())
        expect((await rows.nth(index).locator('th, td').allTextContents()).map(text => text.trim()))
          .toEqual([playerName(snapshot, line.playerNumber), playerName(snapshot, line.targetPlayerNumber), line.receivedVotes + '票'])
    }
  }
}

test('実対局の全日全ラウンドを権威と照合し、閲覧と再読込では書き込まない', async ({ page, request }, testInfo) => {
  const id = await start(page)
  try {
    await expectHistory(page, await read(request, id))
    let turns = 0, checkedReadOnly = false
    while (true) {
      expect(++turns).toBeLessThan(30)
      await drain(page)
      const snapshot = await read(request, id)
      await expectHistory(page, snapshot)
      if (snapshot.voteHistory.length >= 2 && !checkedReadOnly) {
        const before = await saved(page)
        let writes = 0
        const count = req => { if (['POST', 'PUT', 'DELETE'].includes(req.method())) writes++ }
        page.on('request', count)
        const firstDay = page.getByTestId('vote-day-' + snapshot.voteHistory[0].day)
        await firstDay.locator('summary').focus()
        await page.keyboard.press('Enter')
        await expect(firstDay).not.toHaveAttribute('open', '')
        await page.keyboard.press('Enter')
        await expect(firstDay).toHaveAttribute('open', '')
        await page.reload()
        await expectHistory(page, snapshot)
        expect(await saved(page)).toEqual(before)
        expect(Object.keys(before).sort()).toEqual(['eventCursor', 'sessionId'])
        expect(await read(request, id)).toEqual(snapshot)
        expect(writes).toBe(0)
        page.off('request', count)
        await page.getByRole('region', { name: '最近投票结果' }).screenshot({ path: testInfo.outputPath('vote-history.png') })
        checkedReadOnly = true
      }
      if (snapshot.phase === 'FINISHED') break
      await page.getByTestId('vote-FREE').click()
      await expect(page.getByTestId('next-event')).toBeVisible()
    }
    expect(checkedReadOnly).toBe(true)
    await page.reload()
    await expectHistory(page, await read(request, id))
  } finally { await request.delete('/api/v1/games/' + id) }
})

async function fixture(page, request, unknown) {
  const created = await (await request.post('/api/v1/games', { data: { configuration: 'tongchang' } })).json()
  const snapshot = structuredClone(created.update)
  // Synthetic display fixture: not a claim that this seed naturally produces these ties.
  const tie = { round: 1, maximumVotes: 2, leaders: [2, 1], lines: [
    { playerNumber: 1, receivedVotes: 2, targetPlayerNumber: 2 },
    { playerNumber: 2, receivedVotes: 2, targetPlayerNumber: 1 },
    { playerNumber: 3, receivedVotes: 0, targetPlayerNumber: 2 },
    { playerNumber: 4, receivedVotes: 0, targetPlayerNumber: 1 },
  ] }
  const final = { round: 2, maximumVotes: 3, leaders: [2], lines: [
    { playerNumber: 1, receivedVotes: 1, targetPlayerNumber: 2 },
    { playerNumber: 2, receivedVotes: 3, targetPlayerNumber: 1 },
    { playerNumber: 3, receivedVotes: 0, targetPlayerNumber: 2 },
    { playerNumber: 4, receivedVotes: 0, targetPlayerNumber: 2 },
  ] }
  snapshot.voteHistory = ['DESIGNATED', 'FREE', 'GREY'].map((method, index) => ({
    day: index + 1, method: unknown ? 'FUTURE_METHOD' : method, candidates: [2, 1],
    rounds: [structuredClone(tie), structuredClone(final)], ignoredPrivateField: 'DO_NOT_RENDER_PRIVATE_SENTINEL',
  }))
  if (unknown) snapshot.legalVoteActions.push({ method: 'FUTURE_METHOD', candidates: [2, 1] })
  await page.route('**/api/v1/games/' + created.sessionId + '?**', route => route.fulfill({ json: snapshot }))
  await page.addInitScript(({ key, id, cursor }) => sessionStorage.setItem(key, JSON.stringify({ sessionId: id, eventCursor: cursor })),
    { key, id: created.sessionId, cursor: snapshot.nextEventCursor })
  await page.goto('/')
  return { id: created.sessionId, snapshot }
}

for (const unknown of [false, true]) {
  test(unknown ? '未知の投票方式は既知方式に偽装せず、公開票型は閲覧できる' : '合成表示境界：全方式・同票の全首位・再投票・ゼロ票・候補順序', async ({ page, request }) => {
    const { id, snapshot } = await fixture(page, request, unknown)
    try {
      const panel = page.getByRole('region', { name: '最近投票结果' })
      await expect(page.getByTestId('vote-day-3')).toHaveAttribute('open', '')
      await expect(page.getByTestId('vote-day-1')).not.toHaveAttribute('open', '')
      await expectHistory(page, snapshot)
      await expect(panel).not.toContainText('DO_NOT_RENDER_PRIVATE_SENTINEL')
      if (unknown) {
        await expect(panel).not.toContainText('指定投票')
        await expect(page.getByTestId('vote-FUTURE_METHOD')).toHaveCount(0)
      }
    } finally { await request.delete('/api/v1/games/' + id) }
  })
}

for (const lost of [false, true]) {
  test(lost ? '投票実行後の応答喪失は同期で履歴を復元し再投票しない' : '旧版投票は同期して既存の履歴を表示し再投票しない', async ({ page, request }) => {
    const id = await start(page)
    try {
      await drain(page)
      let authority, writes = 0
      if (!lost) {
        const before = await read(request, id)
        const action = before.legalVoteActions.find(action => action.method === 'FREE')
        authority = (await (await request.post('/api/v1/games/' + id + '/votes', {
          data: { expectedRevision: before.revision, ...action },
        })).json()).update
      }
      await page.route('**/api/v1/games/' + id + '/votes', async route => {
        writes++
        if (lost) { authority = (await (await route.fetch()).json()).update; await route.abort() }
        else await route.continue()
      })
      await page.getByTestId('vote-FREE').click()
      await expect(page.getByRole('alert')).toContainText(lost ? '重新同步' : '已重新同步')
      if (lost) await page.getByRole('button', { name: '再同期', exact: true }).click()
      expect(authority.voteHistory.length).toBe(1)
      await expectHistory(page, authority)
      expect(writes).toBe(1)
      expect((await read(request, id)).voteHistory).toEqual(authority.voteHistory)
    } finally { await request.delete('/api/v1/games/' + id) }
  })
}

test('CO中断は未完了の票型を追加せず、再読込でも同じ履歴を保つ', async ({ page, request }) => {
  const id = await start(page)
  try {
    await drain(page)
    // Existing HTTP/browser regression fixes this public seed/target as an actual CO interruption.
    const snapshot = await read(request, id)
    expect(snapshot.legalTargetSelections.find(action => action.type === 'VOTE').targets).toContain(9)
    const selected = await request.put('/api/v1/games/' + id + '/vote-target', {
      data: { expectedRevision: snapshot.revision, target: 9, selected: true },
    })
    expect(selected.ok()).toBe(true)
    await page.getByRole('button', { name: '再同期', exact: true }).click()
    await expect(page.getByTestId('vote')).toBeEnabled()
    const response = page.waitForResponse(res => res.request().method() === 'POST' && res.url().endsWith('/votes'))
    await page.getByTestId('vote').click()
    const result = await (await response).json()
    expect(result.voteCompleted).toBe(false)
    expect(result.update.voteHistory).toEqual(snapshot.voteHistory)
    await expectHistory(page, result.update)
    await page.reload()
    await expect(page.getByTestId('next-event')).toBeVisible()
    await expectHistory(page, result.update)
  } finally { await request.delete('/api/v1/games/' + id) }
})

test('失効と新局で前局の票型を持ち越さない', async ({ page, request }) => {
  const id = await start(page)
  let nextId
  try {
    await drain(page)
    const before = await read(request, id)
    expect(before.voteHistory).toEqual([])
    await page.getByTestId('vote-FREE').click()
    await expect(page.getByTestId('next-event')).toBeVisible()
    const after = await read(request, id)
    expect(after.voteHistory.length).toBe(1)
    expect(after.voteHistory[0].day).toBe(before.view.gameDay)
    await expectHistory(page, after)
    await request.delete('/api/v1/games/' + id)
    await page.getByRole('button', { name: '再同期', exact: true }).click()
    await expect(page.getByRole('region', { name: '最近投票结果' })).toHaveCount(0)
    await page.getByRole('button', { name: '対局を始める' }).click()
    await expect(page.getByTestId('next-event')).toBeVisible()
    nextId = (await saved(page)).sessionId
    expect(nextId).not.toBe(id)
    await expectHistory(page, await read(request, nextId))
  } finally {
    await request.delete('/api/v1/games/' + id)
    if (nextId) await request.delete('/api/v1/games/' + nextId)
  }
})
