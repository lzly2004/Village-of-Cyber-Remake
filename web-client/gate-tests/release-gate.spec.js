import {desktopHome,desktopClaims,desktopClaimPlayers,desktopSelection,desktopSkill,desktopVote,desktopRecords,completeVoteReview} from '../test-fixtures/desktop-navigation.js'
import { characterDisplayName,characters } from '../src/characterDisplay.js'
import { test, expect } from '../test-fixtures/presentation-test.js'
import { readFileSync, readdirSync, mkdirSync, writeFileSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { fileURLToPath } from 'node:url'
import { resolve } from 'node:path'

const root = fileURLToPath(new URL('../..', import.meta.url))
const key = 'vocr.guest.v1'
const stageMapping = JSON.parse(readFileSync(resolve(root, 'src/main/resources/config/event_image_mapping.json'), 'utf8'))
const imageManifest = JSON.parse(readFileSync(resolve(root, 'web-client/public/assets/images/manifest.json'), 'utf8'))
const roles = { zhan: '占い師', ling: '霊能者', lie: '狩人', gong: '共有', mao: '猫又' }
const configurations = ['jianyi', 'tongchang', 'yaoohu', 'kuangxin', 'beide', 'maoyou', 'daxing']
const names = ['簡易', '通常', '妖狐', '狂信', '背徳', '猫又', '大型']
const saved = page => page.evaluate(key => JSON.parse(sessionStorage.getItem(key)), key)

function sourceFingerprint() {
  const hash = createHash('sha256')
  function files(directory) {
    return readdirSync(resolve(root, directory), { withFileTypes: true }).flatMap(entry =>
      entry.isDirectory() ? files(directory + '/' + entry.name)
        : entry.name.endsWith('.java') ? [directory + '/' + entry.name] : [])
  }
  for (const directory of ['src/main/java', 'src/test/java', 'src/web/java'])
    for (const path of files(directory).sort()) hash.update(path).update('\0').update(readFileSync(resolve(root, path)))
  return hash.digest('hex')
}
let fingerprint
test.beforeAll(() => { fingerprint = sourceFingerprint() })

function oracle(configuration, strategy) {
  const data = JSON.parse(readFileSync(resolve(root, `build/web017/${configuration}-${strategy}.json`), 'utf8'))
  expect(data.schema).toBe(1)
  expect(data.configuration).toBe(configuration)
  expect(data.strategy).toBe(strategy)
  expect(data.seed).toBe(4201 + configurations.indexOf(configuration))
  expect(data.sourceFingerprint, '先运行Web017JourneyTest；不使用源码变化前的旧产物').toBe(fingerprint)
  return data
}

async function drain(page, snapshot) {
  for (const event of snapshot.presentedEvents) {
    await expect(page.getByTestId('next-event')).toBeVisible()
    if (event.event.ch1 && event.event.ch1 !== 'NONE')
      // Speaker identity now lives in the desktop message-frame header rather
      // than auxiliary metadata inside the body. Keep checking the same name.
      await expect(page.getByTestId('desktop-message').locator('.message-speaker')).toHaveText(characterDisplayName(event.event.ch1))
    if (['DAY_DIALOGUE', 'AFTERNOON_DIALOGUE', 'NIGHT_RESULT', 'EXECUTION', 'SETTLEMENT'].includes(event.stage)) {
      await expect(page.getByTestId('stage-artwork')).toHaveAttribute('data-art-ready', 'true')
      await expect(page.getByTestId('stage-art-status')).toHaveCount(0)
      const sources = await page.getByTestId('stage-character').evaluateAll(nodes => nodes.map(node => node.dataset.source))
      if (event.event.eventname === 'wsw') expect(sources).toEqual([])
      else {
        const entry = stageMapping[event.event.ch1][event.event.eventname]
        const name = Object.hasOwn(entry.special || {}, event.event.ch2) ? entry.special[event.event.ch2] : entry.default
        if (Array.isArray(name)) {
          expect(sources).toHaveLength(1)
          expect(name.map(n=>'images/'+n)).toContain(sources[0])
        } else expect(sources).toContain('images/' + name)
        expect(sources.length).toBeLessThanOrEqual(2)
      }
    }
    await page.getByTestId('next-event').click()
  }
  await expect(page.getByTestId('next-event')).toHaveCount(0)
  expect((await saved(page)).eventCursor).toBe(snapshot.nextEventCursor)
}

async function display(page, snapshot, beforeDrain = false) {
  await completeVoteReview(page)
  if(await page.getByTestId('desktop-vote-window').count())await desktopHome(page)
  await expect(page.locator('.player-row,.desktop-roster-row')).toHaveCount(snapshot.view.players.length)
  for (const player of snapshot.view.players) {
    const row = page.locator('.player-row,.desktop-roster-row').nth(player.playerNumber - 1)
    await expect(row.locator('strong')).toHaveText(characterDisplayName(player.character))
    if (!player.alive) await expect(row).toHaveClass(/departed/)
  }
  const pending = beforeDrain && snapshot.presentedEvents.length > 0
  if (snapshot.phase === 'FINISHED') {
    if (pending) {
      await expect(page.locator('.player-identity').first()).not.toContainText('実際の役職')
      await expect(page.getByRole('region', { name: '公開情報', exact: true })).toHaveCount(0)
    } else {
      await expect(page.locator('.player-identity').first()).toContainText('実際の役職')
      await expect(page.getByTestId('reveal-diagnostic')).toHaveCount(0)
      const player = snapshot.view.players[0]
      const role = snapshot.revealedRoles.find(entry=>entry.playerNumber===player.playerNumber).actualRole
      const number = Object.keys(stageMapping).indexOf(player.character)+1
      const suffix = !player.alive ? 'gs' : ({mao:'cs',yaohu:'fs',beidezhe:'hs',renlang:'ws',kuangren:'ks',kuangxinzhe:'ks'}[role]||'s')
      const name = 'images/'+String(number).padStart(2,'0')+suffix+'.png'
      await page.locator('.player-row,.desktop-roster-row').first().scrollIntoViewIfNeeded()
      await expect(page.locator('.player-row,.desktop-roster-row').first().locator('.portrait-image img')).toHaveAttribute('src','/assets/images/'+imageManifest.images[name].file)
      await expect(page.getByTestId('stage-background')).toHaveAttribute('href','/assets/images/'+imageManifest.images['images/frame #19252.png'].file)
      await expect(page.getByRole('region', { name: '公開死亡履歴', exact: true })).toBeVisible()
    }
  } else {
    const window=page.getByTestId('desktop-vote-window')
    if(await window.count()){
      await desktopHome(page)
      const text=await page.getByTestId('desktop-information').textContent()
      expect(text.match(/\[[^\]]+\]/g)).toEqual(['[占い師]','[霊能者]','[処刑]','[死体]','[護衛先]'])
      for(const report of snapshot.publicInformation.skillReports){
        const player=snapshot.view.players.find(p=>p.playerNumber===report.playerNumber)
        expect(text).toContain(characters[player.character].shortName+' :')
      }
      await expect(page.getByRole('switch',{name:'投票回避CO'})).toHaveAttribute('aria-checked',String(snapshot.selections.avoidanceEnabled))
    }else{
      await expect(page.getByRole('region',{name:'公開情報',exact:true})).toBeVisible()
      await expect(page.getByRole('region',{name:'公開技能報告',exact:true}).locator('article')).toHaveCount(snapshot.publicInformation.skillReports.length)
      await expect(page.getByRole('switch',{name:'投票回避CO'})).toHaveCount(0)
    }
  }
  if(await page.getByTestId('desktop-vote-window').count() && snapshot.voteHistory.length){
    const short=n=>characters[snapshot.view.players.find(p=>p.playerNumber===n).character].shortName
    for(const v of snapshot.voteHistory){
      await desktopRecords(page,'history')
      await page.getByRole('button',{name:v.day+'日目の投票結果',exact:true}).click()
      const previous=page.getByRole('button',{name:'前の投票結果',exact:true}),next=page.getByRole('button',{name:'次の投票結果',exact:true})
      await expect(previous).toBeDisabled()
      for(const [i,r]of v.rounds.entries()){
        await expect(page.locator('.desktop-history pre').first()).toContainText(v.day+'日目')
        await expect(page.locator('.history-line')).toHaveText(r.lines.map(l=>`${short(l.playerNumber)}：${l.receivedVotes}票  投票先→${short(l.targetPlayerNumber)}`))
        await expect(page.locator('.history-summary')).toContainText(r.leaders.length>1?'同点':r.maximumVotes+'票')
        if(i+1<v.rounds.length)await next.click()
      }
      await expect(next).toBeDisabled()
    }
    await desktopHome(page)
    return
  }

  const history = page.getByRole('region', { name: '最近投票结果', exact: true })
  if (!snapshot.voteHistory.length) await expect(history).toHaveCount(0)
  else {
    await expect(history.locator('details')).toHaveCount(snapshot.voteHistory.length)
    for (const vote of snapshot.voteHistory) {
      const day = page.getByTestId('vote-day-' + vote.day)
      if (!await day.evaluate(element => element.open)) await day.locator('summary').click()
      await expect(day.locator('.history-round')).toHaveCount(vote.rounds.length)
      for (const [index, round] of vote.rounds.entries()) {
        expect(round.leaders.length > 1).toBe(index < vote.rounds.length - 1)
        const section = page.getByTestId(`vote-round-${vote.day}-${round.round}`)
        await expect(section.locator('tbody tr')).toHaveCount(round.lines.length)
        await expect(section.locator('.round-summary')).toContainText(
          index < vote.rounds.length - 1 ? '同票・再投票' : '処刑')
      }
    }
  }
}

async function operate(page, step) {
  const body=step.body
  switch(step.path){
    case '/claims/by-role':
      await desktopClaims(page);await page.getByRole('button',{name:roles[body.role]+'のCOを聞く',exact:true}).click();break
    case '/claims/by-players':
      await desktopClaimPlayers(page)
      for(const number of body.players)await page.getByRole('checkbox',{name:new RegExp('^'+number+'番 .*をCO確認対象に選択$')}).check()
      await page.getByRole('button',{name:'戻る',exact:true}).click();break
    case '/avoidance':
      await desktopHome(page);await page.getByRole('switch',{name:'投票回避CO'}).click();break
    case '/target-selections':
      await desktopSkill(page,body.type==='DIVINATION'?'divination':'guard')
      await page.locator('.desktop-target-row[data-player-number="'+body.target+'"]').getByRole('checkbox').click();break
    case '/claimed-target-selections':{
      const row=page.locator('.desktop-roster-row[data-player-number="'+body.claimedPlayer+'"]')
      const full=await row.locator('strong').textContent()
      // Resolve the tool from the public roster name; no hidden role is read.
      await desktopSelection(page,body.type==='DIVINATION'?'divination':'guard')
      const publicName=Object.keys(characters).find(key=>characterDisplayName(key)===full)
      await desktopSkill(page,body.type==='DIVINATION'?'divination':'guard',characters[publicName].shortName)
      await page.locator('.desktop-target-row[data-player-number="'+body.target+'"]').getByRole('checkbox').click();break
    }
    case '/vote-target':
      await desktopSelection(page);await page.locator('.desktop-target-row[data-player-number="'+body.target+'"]').getByRole('checkbox').click();break
    case '/votes':await desktopVote(page,body.method);break
    default:throw new Error('unknown UI policy command: '+step.path)
  }
}

async function journey(page, request, testInfo, configuration, strategy) {
  const data = oracle(configuration, strategy)
  await page.goto('/')
  await page.getByRole('radio', { name: names[configurations.indexOf(configuration)] + '配役' }).check()
  const createdResponse = page.waitForResponse(res => res.request().method() === 'POST' && res.url().endsWith('/api/v1/games'))
  await page.getByRole('button', { name: '対局を始める' }).click()
  const created = await (await createdResponse).json()
  const id = created.sessionId
  const path = '/api/v1/games/' + id
  try {
    expect(created.update).toEqual(data.initial)
    await display(page, data.initial, true)
    await drain(page, data.initial)
    let refreshed = false, writes = 0, interruptions = 0
    const countWrites = req => { if (req.method() === 'POST' || req.method() === 'PUT') writes++ }
    page.on('request', countWrites)
    for (const step of data.steps) {
      const resultPromise = page.waitForResponse(res => res.request().method() === step.method && res.url().endsWith(path + step.path))
      await operate(page, step)
      const response = await resultPromise
      expect(response.status()).toBe(200)
      expect(response.request().postDataJSON()).toEqual(step.body)
      const result = await response.json()
      expect(result).toEqual(step.response)
      expect(await (await request.get(path)).json()).toEqual(step.full)
      const snapshot = step.path === '/votes' ? result.update : result
      if (step.path === '/votes' && !result.voteCompleted) interruptions++
      await display(page, snapshot, true)
      await drain(page, snapshot)
      await display(page, snapshot)
      if (!refreshed && step.full.voteHistory.length) {
        const before = await saved(page), beforeWrites = writes
        await page.reload()
        await display(page, step.full)
        expect(await saved(page)).toEqual(before)
        expect(Object.keys(before).sort()).toEqual(['eventCursor', 'sessionId'])
        expect(writes).toBe(beforeWrites)
        refreshed = true
      }
    }
    expect(writes).toBe(data.steps.length)
    page.off('request', countWrites)
    const final = data.steps.at(-1).full
    if (strategy === 'interruption') expect(interruptions).toBeGreaterThan(0)
    else {
      expect(final.phase).toBe('FINISHED')
      expect(refreshed).toBe(true)
      await page.reload()
      await display(page, final)
      await page.getByRole('region', { name: '公开玩家列表', exact: true })
        .screenshot({ path: testInfo.outputPath('settlement-players.png') })
      await page.screenshot({ path: testInfo.outputPath('settlement.png'), fullPage: true })
      // One real seeded API journey documents the implemented core window;
      // synthetic layout cases have a separate provenance index.
      if(configuration==='jianyi' && strategy==='designated') {
        const folder=resolve(root,'build/verify/desktop-core')
        mkdirSync(folder,{recursive:true})
        const image=await page.getByTestId('stage-surface').screenshot({path:resolve(folder,'settlement-real-jianyi.png')})
        writeFileSync(resolve(folder,'settlement-real-jianyi.json'),JSON.stringify({source:'Real seeded API/HTTP/browser gate journey; not user random playtest',configuration,strategy,seed:data.seed,bytes:image.length,sha256:createHash('sha256').update(image).digest('hex')},null,2)+'\n')
      }
    }
    for (const endpoint of ['/debug', '/context', '/recorder', '/suspicion'])
      expect((await request.get(path + endpoint)).status()).toBe(404)
    await testInfo.attach('journey-summary', { contentType: 'application/json', body: JSON.stringify({
      configuration, strategy, seed: data.seed, commands: data.steps.length, interruptions,
      result: final.view.result, days: final.view.gameDay,
      methods: [...new Set(final.voteHistory.map(vote => vote.method))],
      rounds: final.voteHistory.map(vote => ({ day: vote.day, rounds: vote.rounds.length })),
      individualSkills: [...new Set(data.steps.filter(step => step.path === '/claimed-target-selections').map(step => step.body.type))],
    }, null, 2) })
  } finally { await request.delete(path) }
}

for (const configuration of configurations)
  for (const strategy of ['designated', 'mixed'])
    test(`${configuration} / ${strategy}：应用直连・HTTP・浏览器逐命令对等`, async ({ page, request }, testInfo) => {
      await journey(page, request, testInfo, configuration, strategy)
    })

test('tongchang / interruption：真实CO中断与同日事件确认', async ({ page, request }, testInfo) => {
  await journey(page, request, testInfo, 'tongchang', 'interruption')
})
