import { characterDisplayName } from '../src/characterDisplay.js'
import { test as base, expect } from '@playwright/test'
import { publicInformationDiagnostics } from './public-information-diagnostics.js'
const test = base.extend({
  diagnostics: async ({ page }, use, testInfo) => {
    const diagnostic = publicInformationDiagnostics(page, testInfo)
    await use(diagnostic)
    await diagnostic.finish()
  },
})

const key = 'vocr.guest.v1'
const name = (snapshot, number) => number + ' · ' + characterDisplayName(snapshot.view.players.find(player => player.playerNumber === number).character)
const read = async (request, id) => (await request.get('/api/v1/games/' + id)).json()
async function saved(page) { return page.evaluate(key => JSON.parse(sessionStorage.getItem(key)), key) }
async function start(page) {
  await page.goto('/')
  await page.getByRole('radio', { name: '通常配役' }).check()
  await page.getByRole('button', { name: '対局を始める' }).click()
  await expect(page.getByTestId('next-event')).toBeVisible()
  return (await saved(page)).sessionId
}
async function drain(page) {
  let count = 0
  while (await page.getByTestId('next-event').count()) {
    expect(++count).toBeLessThan(120)
    await page.getByTestId('next-event').click()
  }
}
async function expectReports(page, snapshot) {
  const panel = page.getByRole('region', { name: '公開技能報告', exact: true })
  await expect(panel.locator('article')).toHaveCount(snapshot.publicInformation.skillReports.length)
  for (const report of snapshot.publicInformation.skillReports) {
    const row = panel.getByTestId('report-' + report.playerNumber)
    await expect(row.locator('h4')).toContainText(name(snapshot, report.playerNumber))
    await expect(row.locator('h4')).toContainText('CO順 ' + report.claimedRoleOrder)
    await expect(row.locator('li')).toHaveText(report.results.map(result =>
      `第${result.skillDay}日：${name(snapshot, result.targetPlayerNumber)}`
        + ({ WHITE: '○', BLACK: '●', NOT_APPLICABLE: '' })[result.color]))
    if (!snapshot.view.players.find(player => player.playerNumber === report.playerNumber).alive)
      await expect(row.locator('h4')).toContainText('離場済み')
    if (report.exposed) await expect(row.locator('h4')).toContainText('破綻')
  }
}

test('公開情報は実対局のCO・跨日・終局に追従し、閲覧と再読込で書き込まない', async ({ page, request, diagnostics }, testInfo) => {
  const id = await diagnostics.step('start and first event visible', () => start(page))
  let sawDead = false, sawSuspicion = false, checkedRefresh = false
  try {
    await diagnostics.step('assert initial suspicion availability', () => expect(page.getByRole('region', { name: '疑い先', exact: true })).toContainText('第3日以降'))
    await diagnostics.step('drain public events', () => drain(page))
    for (const role of ['占い師', '霊能者', '狩人']) {
      await diagnostics.step('CO inquiry ' + role, () => page.getByRole('button', { name: role + 'のCOを聞く', exact: true }).click())
      await diagnostics.step('drain public events', () => drain(page))
    }
    let days = 0
    while (true) {
      expect(++days).toBeLessThan(30)
      const snapshot = await diagnostics.step('read authoritative snapshot', () => read(request, id))
      diagnostics.snapshot(snapshot)
      expect(snapshot.publicInformation).toBeTruthy()
      if (snapshot.phase === 'FINISHED') {
        // Authority has finished, but the browser must still read preceding events.
        await diagnostics.step('wait for current event', () => expect(page.getByTestId('next-event')).toBeVisible())
        await expect(page.getByRole('region', { name: '公開情報', exact: true })).toHaveCount(0)
        await expect(page.locator('.player-identity').first()).not.toContainText('実際の役職')
        await diagnostics.step('drain public events', () => drain(page))
        await expect(page.getByRole('region', { name: '公開死亡履歴', exact: true })).toBeVisible()
        await expect(page.getByRole('region', { name: '公開技能報告', exact: true })).toHaveCount(0)
        await expect(page.getByRole('region', { name: '疑い先', exact: true })).toHaveCount(0)
        break
      }
      await diagnostics.step('drain public events', () => drain(page))
      await diagnostics.step('assert public reports day ' + snapshot.view.gameDay, () => expectReports(page, snapshot))
      sawDead ||= snapshot.publicInformation.skillReports.some(report =>
        !snapshot.view.players.find(player => player.playerNumber === report.playerNumber).alive)
      if (snapshot.publicInformation.suspicion.available) {
        sawSuspicion = true
        const symbols = { CLOSE: '=', SLIGHTLY_GREATER: '≧', GREATER: '>', MUCH_GREATER: '≫' }
        for (const row of snapshot.publicInformation.suspicion.rows) {
          const expected = row.targets.length ? name(snapshot, row.playerNumber) + '：'
            + row.targets.map((target, index) => name(snapshot, target)
              + (index + 1 < row.targets.length ? symbols[row.comparisons[index]] : '')).join('') : ''
          await diagnostics.step('assert suspicion day ' + snapshot.view.gameDay + ' player ' + row.playerNumber, () => expect(page.getByTestId('suspicion-' + row.playerNumber)).toHaveText(expected))
        }
        if (!checkedRefresh) {
          const beforeText = await page.getByRole('region', { name: '公開情報', exact: true }).textContent()
          const beforeStorage = await saved(page)
          let writes = 0
          const countWrite = req => { if (['POST', 'PUT', 'DELETE'].includes(req.method())) writes++ }
          page.on('request', countWrite)
          await diagnostics.step('reload', () => page.reload())
          await expect(page.getByRole('region', { name: '公開情報', exact: true })).toHaveText(beforeText)
          expect(await saved(page)).toEqual(beforeStorage)
          expect(Object.keys(beforeStorage).sort()).toEqual(['eventCursor', 'sessionId'])
          expect(writes).toBe(0)
          expect(await read(request, id)).toEqual(snapshot)
          page.off('request', countWrite)
          await page.screenshot({ path: testInfo.outputPath('public-information.png'), fullPage: true })
          await diagnostics.step('public information panel screenshot', () => page.getByRole('region', { name: '公開情報', exact: true })
            .screenshot({ path: testInfo.outputPath('public-information-panel.png') }))
          checkedRefresh = true
        }
      }
      await diagnostics.step('free vote day ' + snapshot.view.gameDay, () => page.getByTestId('vote-FREE').click())
      await diagnostics.step('wait for current event', () => expect(page.getByTestId('next-event')).toBeVisible())
    }
    expect(sawDead).toBe(true)
    expect(sawSuspicion).toBe(true)
    expect(checkedRefresh).toBe(true)
    await diagnostics.step('reload', () => page.reload())
    await expect(page.getByRole('region', { name: '公開死亡履歴', exact: true })).toBeVisible()
  } finally { await request.delete('/api/v1/games/' + id) }
})

async function fixture(page, request, edit) {
  const created = await (await request.post('/api/v1/games', { data: { configuration: 'tongchang' } })).json()
  const snapshot = structuredClone(created.update)
  edit(snapshot)
  await page.route('**/api/v1/games/' + created.sessionId + '?**', route =>
    route.fulfill({ json: snapshot }))
  await page.addInitScript(({ key, id }) => sessionStorage.setItem(key, JSON.stringify({ sessionId: id, eventCursor: 0 })),
    { key, id: created.sessionId })
  await page.goto('/')
  await expect(page.getByRole('region', { name: '公開情報', exact: true })).toBeVisible()
  return { id: created.sessionId, snapshot }
}

test('表示端の合成境界：死亡CO・過去報告・球・破綻・空行・公開接頭辞を忠実に表示', async ({ page, request }) => {
  // Synthetic display-edge fixture only; NOT evidence these reports occur in this seed.
  const { id, snapshot } = await fixture(page, request, update => {
    expect(update.view.players.length).toBeGreaterThanOrEqual(8)
    update.view.gameDay = 4
    for (const player of update.view.players) Object.assign(player, { alive: true, deathDay: 0, deathReason: 'NONE' })
    Object.assign(update.view.players[0], { alive: false, deathDay: 3, deathReason: 'NIGHT_DEATH', comingOutDay: 3 })
    Object.assign(update.view.players[5], { alive: false, deathDay: 2, deathReason: 'EXECUTION' })
    Object.assign(update.view.players[6], { alive: false, deathDay: 2, deathReason: 'DAY_CAT_CURSE' })
    Object.assign(update.view.players[7], { alive: false, deathDay: 2, deathReason: 'EXECUTION' })
    for (const [index, role] of ['zhan', 'ling', 'lie', 'zhan'].entries())
      Object.assign(update.view.players[index], { claimedRole: role, claimedRoleOrder: index === 3 ? 2 : 1 })
    update.view.aliveCount = update.view.players.length - 4
    update.view.deathCount = 4
    for (const action of update.legalTargetSelections)
      action.claimedActors = action.claimedActors.filter(actor => update.view.players[actor.playerNumber - 1].alive)
    update.publicInformation = {
      skillReports: [
        { playerNumber: 1, claimedRole: 'zhan', claimedRoleOrder: 1, exposed: true,
          results: [{ skillDay: 1, targetPlayerNumber: 2, color: 'WHITE' }, { skillDay: 2, targetPlayerNumber: 3, color: 'BLACK' }] },
        { playerNumber: 2, claimedRole: 'ling', claimedRoleOrder: 1, exposed: false,
          results: [{ skillDay: 2, targetPlayerNumber: 4, color: 'BLACK' }] },
        { playerNumber: 3, claimedRole: 'lie', claimedRoleOrder: 1, exposed: false,
          results: [{ skillDay: 2, targetPlayerNumber: 5, color: 'NOT_APPLICABLE' }] },
        { playerNumber: 4, claimedRole: 'zhan', claimedRoleOrder: 2, exposed: false, results: [] },
      ],
      suspicion: { available: true, rows: [
        { playerNumber: 2, targets: [4, 3, 5], comparisons: ['CLOSE', 'SLIGHTLY_GREATER'] },
        { playerNumber: 3, targets: [5, 2, 4], comparisons: ['GREATER', 'MUCH_GREATER'] },
        { playerNumber: 4, targets: [], comparisons: [] },
      ] },
      deathHistory: [
        { day: 1, executions: [], nightDeaths: [] },
        { day: 2, executions: [{ playerNumber: 6, cause: 'EXECUTION', prefixPlayers: [] },
          { playerNumber: 7, cause: 'DAY_CAT_CURSE', prefixPlayers: [8] }], nightDeaths: [] },
        { day: 3, executions: [], nightDeaths: [1] },
      ],
      ignoredFutureField: 'DO_NOT_RENDER_PRIVATE_SENTINEL',
    }
  })
  try {
    await expectReports(page, snapshot)
    await expect(page.getByTestId('report-4')).toContainText('公開された結果はまだありません')
    await expect(page.getByTestId('report-3')).not.toContainText('○')
    await expect(page.getByTestId('report-3')).not.toContainText('●')
    await expect(page.getByTestId('suspicion-4')).toHaveText('')
    await expect(page.getByTestId('suspicion-2')).toHaveText(name(snapshot, 2) + '：' + name(snapshot, 4) + '=' + name(snapshot, 3) + '≧' + name(snapshot, 5))
    await expect(page.getByTestId('suspicion-3')).toHaveText(name(snapshot, 3) + '：' + name(snapshot, 5) + '>' + name(snapshot, 2) + '≫' + name(snapshot, 4))
    await expect(page.getByTestId('death-day-1')).toContainText('死体：平和')
    await expect(page.getByTestId('death-day-2')).toContainText(name(snapshot, 8) + '+' + name(snapshot, 7) + '（猫呪）')
    await expect(page.getByTestId('death-day-3')).toContainText('死体：' + name(snapshot, 1))
    await expect(page.getByRole('region', { name: '公開情報', exact: true })).not.toContainText('DO_NOT_RENDER_PRIVATE_SENTINEL')
  } finally { await request.delete('/api/v1/games/' + id) }
})

for (const missing of [false, true]) {
  test(missing ? '旧サーバーの投影欠落は未対応と表示する' : '空の公開報告は未対応と混同しない', async ({ page, request }) => {
    const { id } = await fixture(page, request, update => {
      if (missing) delete update.publicInformation
      else update.publicInformation = { skillReports: [], suspicion: { available: false, rows: [] }, deathHistory: [] }
    })
    try {
      const panel = page.getByRole('region', { name: '公開情報', exact: true })
      await expect(panel).toContainText(missing ? '対応していません' : '公開COの技能報告はありません')
      if (!missing) await expect(panel).not.toContainText('対応していません')
    } finally { await request.delete('/api/v1/games/' + id) }
  })
}

test('会話失効後は公開情報も消え、新局に持ち越さない', async ({ page, request }) => {
  const id = await start(page)
  await request.delete('/api/v1/games/' + id)
  await page.getByRole('button', { name: '再同期', exact: true }).click()
  await expect(page.getByRole('region', { name: '公開情報', exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '対局を始める' })).toBeVisible()
  await page.getByRole('button', { name: '対局を始める' }).click()
  await expect(page.getByTestId('next-event')).toBeVisible()
  const nextId = (await saved(page)).sessionId
  try {
    expect(nextId).not.toBe(id)
    await expectReports(page, await read(request, nextId))
  } finally { await request.delete('/api/v1/games/' + nextId) }
})

for (const lost of [false, true]) {
  test(lost ? 'CO応答喪失後は同期で公開情報を復元し再送しない' : 'CO旧版競合は権威の公開情報に同期し再送しない', async ({ page, request }) => {
    const id = await start(page)
    try {
      await drain(page)
      let authority, writes = 0
      if (!lost) {
        const before = await read(request, id)
        authority = await (await request.post('/api/v1/games/' + id + '/claims/by-role', {
          data: { expectedRevision: before.revision, role: 'zhan' },
        })).json()
      }
      await page.route('**/api/v1/games/' + id + '/claims/by-role', async route => {
        writes++
        if (lost) { authority = await (await route.fetch()).json(); await route.abort() }
        else await route.continue()
      })
      await page.getByRole('button', { name: '占い師のCOを聞く', exact: true }).click()
      await expect(page.getByRole('alert')).toContainText(lost ? '重新同步' : '已重新同步')
      if (lost) await page.getByRole('button', { name: '再同期', exact: true }).click()
      await expectReports(page, authority)
      expect(writes).toBe(1)
    } finally { await request.delete('/api/v1/games/' + id) }
  })
}
