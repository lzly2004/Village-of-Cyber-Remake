import { test,expect } from '../test-fixtures/presentation-test.js'
import { readFileSync } from 'node:fs'
import { replayId } from '../src/replayStore.js'
const displayCases=JSON.parse(readFileSync(new URL('../../src/test/resources/replay/web034-display-cases.json',import.meta.url),'utf8'))
const fixture=JSON.parse(readFileSync(new URL('../../src/test/resources/replay/exchange-v3.json',import.meta.url),'utf8'))[0]
const library=page=>page.getByRole('region',{name:'ローカルReplay',exact:true})
const dialog=page=>page.getByRole('dialog',{name:/Replay再生/})
const rows=page=>library(page).locator('.replay-entries > li')
async function seed(page,replay=fixture) {
  const id=await replayId(replay)
  await page.evaluate(async entry=>new Promise((resolve,reject)=>{
    const r=indexedDB.open('vocr.replays.v1',1)
    r.onupgradeneeded=()=>r.result.createObjectStore('recordings',{keyPath:'id'})
    r.onsuccess=()=>{const db=r.result,tx=db.transaction('recordings','readwrite');tx.objectStore('recordings').clear();tx.objectStore('recordings').put(entry);tx.oncomplete=()=>{db.close();resolve()};tx.onabort=()=>reject(tx.error)}
    r.onerror=()=>reject(r.error)
  }),{id,replay,bytes:Buffer.byteLength(JSON.stringify(replay)),savedAt:'2026-01-02T03:04:05.000Z'})
}
async function play(page) {
  await library(page).getByRole('button',{name:'Replay一覧',exact:true}).click()
  await rows(page).getByRole('button',{name:'再生する',exact:true}).click()
  await expect(dialog(page)).toBeVisible()
}
const panel=page=>dialog(page).getByRole('region',{name:'Replay情報・履歴',exact:true})
const selectDay=(page,day)=>dialog(page).getByRole('combobox',{name:'再生日',exact:true}).selectOption(String(day))
const saved=page=>page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))

test('real running game stays authoritative while history, vote rounds and final role groups are browsed and closed',async({page,request},info)=>{
  await page.goto('/');await page.getByRole('button',{name:'対局を始める'}).click();await expect(page.getByTestId('next-event')).toBeVisible()
  const stored=await saved(page),credentials=JSON.parse(stored)
  const endpoint='/api/v1/games/'+credentials.sessionId+'?eventCursor='+credentials.eventCursor
  try{
    const before=await(await request.get(endpoint)).json(),dialogue=await page.getByTestId('event-dialogue').textContent()
    await seed(page);let writes=0,replayReads=0
    page.on('request',r=>{if(r.url().includes('/api/v1/games')){if(r.method()!=='GET')writes++;if(r.url().includes('/replay?'))replayReads++}})
    await play(page)
    await expect(page.getByTestId('replay-normal-info')).toHaveText(displayCases[0].normalText)
    await panel(page).getByRole('button',{name:'投票結果',exact:true}).click()
    await expect(panel(page).getByRole('table')).toContainText('棄権')
    await panel(page).getByRole('button',{name:'次の投票回'}).click()
    await expect(panel(page).getByRole('status')).toContainText('第2回')
    await expect(dialog(page).getByRole('combobox',{name:'再生日',exact:true})).toBeDisabled()
    await expect(dialog(page).getByRole('button',{name:'次の日',exact:true})).toBeDisabled()
    await panel(page).getByRole('button',{name:'前の投票回'}).click()
    await expect(dialog(page).getByRole('combobox',{name:'再生日',exact:true})).toBeEnabled()
    await panel(page).getByRole('button',{name:'情報・技能履歴',exact:true}).click()
    await selectDay(page,3)
    await expect(page.getByTestId('replay-normal-info')).toHaveText(displayCases[1].normalText)
    await expect(page.getByTestId('replay-normal-info')).not.toContainText('破綻')
    await expect(panel(page).getByRole('button',{name:'投票結果',exact:true})).toBeDisabled()
    await expect(panel(page)).toContainText('この日の投票記録はありません')
    await selectDay(page,4)
    await expect(page.getByTestId('replay-role-groups')).toContainText('狂人')
    await expect(page.getByTestId('replay-normal-info')).toHaveCount(0)
    await expect(dialog(page)).toContainText('既定曲に戻ります')
    await selectDay(page,3)
    await panel(page).getByRole('button',{name:'情報・技能履歴',exact:true}).scrollIntoViewIfNeeded()
    await panel(page).screenshot({path:info.outputPath('history-day3.png')})
    expect(writes).toBe(0);expect(replayReads).toBe(0);expect(await saved(page)).toBe(stored)
    expect(await(await request.get(endpoint)).json()).toEqual(before)
    const records=await page.evaluate(async()=>{const {createReplayStore}=await import('/src/replayStore.js');return(await createReplayStore().list())[0].replay})
    expect(records).toEqual(fixture)
    await page.keyboard.press('Escape');await expect(dialog(page)).toHaveCount(0)
    await expect(page.getByTestId('event-dialogue')).toHaveText(dialogue)
    await expect(page.locator('.player-identity').first()).not.toContainText('実際の役職')
    await page.reload();expect(await saved(page)).toBe(stored);await expect(dialog(page)).toHaveCount(0)
  }finally{await request.delete('/api/v1/games/'+credentials.sessionId)}
})

test('shared desktop cases render complete text, both vote rounds and source-preserved indices through actual UI',async({page})=>{
  for(const c of displayCases){
    await page.goto('/');await seed(page,c.replay);await play(page);await selectDay(page,c.day)
    if(c.day<c.replay.totalDays){
      await expect(page.getByTestId('replay-normal-info')).toHaveText(c.normalText.replaceAll('破绽','破綻'))
      if(c.vote){
        await panel(page).getByRole('button',{name:'投票結果',exact:true}).click()
        if(c.vote.index>0)await panel(page).getByRole('button',{name:'次の投票回'}).click()
        const rows=panel(page).locator('tbody tr')
        await expect(rows).toHaveCount(c.vote.rows.length)
        for(let i=0;i<c.vote.rows.length;i++)await expect(rows.nth(i)).toHaveText(c.vote.rows[i][0]+c.vote.rows[i][1]+'票'+c.vote.rows[i][2])
        for(const name of c.vote.candidates)await expect(panel(page).locator('.replay-votes')).toContainText(name)
      }
    }else await expect(page.getByTestId('replay-role-groups')).toBeVisible()
    await page.keyboard.press('Escape')
  }
})

test('snapshot result icons remain independent of late history and load only when their list is explicitly opened',async({page},info)=>{
  await page.goto('/');await seed(page);await play(page);await selectDay(page,3)
  const images=JSON.parse(readFileSync(new URL('../public/assets/images/manifest.json',import.meta.url),'utf8')).images
  const entry=images['images/result1_1black.png']
  let requested=0;page.on('request',r=>{if(r.url().includes(entry.file))requested++})
  expect(requested).toBe(0)
  await expect(page.getByTestId('replay-normal-info')).not.toContainText('双姉●')
  await panel(page).locator('summary').click()
  const rows=panel(page).locator('.replay-skill-records li')
  await expect(rows).toHaveCount(2)
  await expect(rows.first()).toContainText('第2日（記録 第2日）：双弟 · 占い師 1 → 双姉●')
  await expect(rows.first().locator('img')).toBeVisible()
  await expect(rows.last()).toContainText('第3日（記録 第3日）：双弟 · 占い師 1 → 音楽○')
  await expect.poll(()=>requested).toBe(1)
  await rows.first().scrollIntoViewIfNeeded();await panel(page).screenshot({path:info.outputPath('history-icons.png')})
})

test('gaps show source/failure diagnostics without inventing a skill or a vote, and dates remain navigable',async({page})=>{
  const gap=structuredClone(fixture);gap.daySnapshots=gap.daySnapshots.filter(s=>[1,4].includes(s.dayNumber))
  gap.daySnapshots.forEach(s=>s.players.forEach(p=>p.skillTargets=[]))
  await page.goto('/');await seed(page,gap);await play(page)
  await expect(panel(page)).toContainText('SNAPSHOT_INDEX_FALLBACK')
  await expect(panel(page)).toContainText('SKILL_RECORD_MISSING')
  await selectDay(page,3);await expect(panel(page)).toContainText('SNAPSHOT_MISSING')
  await expect(panel(page).getByRole('button',{name:'投票結果',exact:true})).toBeDisabled()
  await selectDay(page,4);await expect(page.getByTestId('replay-role-groups')).toBeVisible()
})

test('missing icon range retains exact order and result text rather than fabricating another asset',async({page})=>{
  const unusual=structuredClone(fixture);unusual.daySnapshots[1].players[0].claimedRoleOrder=44
  await page.goto('/');await seed(page,unusual);await play(page)
  await expect(panel(page)).toContainText('SKILL_ICON_UNAVAILABLE')
  await panel(page).locator('summary').click()
  const row=panel(page).locator('.replay-skill-records li').first()
  await expect(row).toContainText('占い師 44 → 双姉●')
  await expect(row.locator('img')).toHaveCount(0)
  await expect(row).toContainText('結果図なし')
  await selectDay(page,3);await expect(page.getByTestId('replay-normal-info')).toBeVisible()
})

test('damaged result image is diagnosed while text, date navigation and closing still work',async({page})=>{
  const images=JSON.parse(readFileSync(new URL('../public/assets/images/manifest.json',import.meta.url),'utf8')).images
  await page.route('**/'+images['images/result1_1black.png'].file,r=>r.abort())
  await page.goto('/');await seed(page);await play(page);await panel(page).locator('summary').click()
  await expect(panel(page).getByRole('img',{name:'表示診断：画像を表示できません'})).toBeVisible()
  await expect(page.getByTestId('replay-normal-info')).toContainText('双姉●')
  await selectDay(page,3);await expect(page.getByTestId('replay-normal-info')).toContainText('音楽○')
  await page.keyboard.press('Escape');await expect(dialog(page)).toHaveCount(0)
})

test('44-player information and vote rows keep every person, wrap names and remain reachable without nested vertical clipping',async({page},info)=>{
  const large=structuredClone(fixture);large.peiyiName='daxing';large.peiyiOrdinal=7
  for(const s of large.daySnapshots){
    const sample=s.players[0]
    s.aliveCount=44;s.players=Array.from({length:44},(_,i)=>({...sample,playerIndex:i+1,characterNumber:i+1,actualRole:6,claimedRole:0,comingOutDay:-1,claimedRoleOrder:0,skillTarget:0,skillTargets:[],deathDay:0,deathReason:0,nonHumanMarked:false}))
    s.greyTargetCharNums=[];s.designatedTargetCharNums=[]
    if(s.dayNumber===2){s.dailyVotingRule=0;s.voteRounds=[{round:1,votes:s.players.map(p=>[p.playerIndex,44])}]}
  }
  await page.goto('/');await seed(page,large);await play(page)
  await panel(page).getByRole('button',{name:'投票結果',exact:true}).click()
  await expect(panel(page).locator('tbody tr')).toHaveCount(44)
  const last=panel(page).locator('tbody tr').last()
  await last.scrollIntoViewIfNeeded();await expect(last).toHaveText('音楽44票音楽')
  for(const width of [1440,1280,1024]){
    await page.setViewportSize({width,height:1000})
    expect(await dialog(page).evaluate(el=>el.scrollWidth<=el.clientWidth+1)).toBe(true)
    expect(await panel(page).locator('table').evaluate(el=>[...el.querySelectorAll('th,td')].every(x=>x.scrollWidth<=x.clientWidth+1))).toBe(true)
  }
  await dialog(page).screenshot({path:info.outputPath('history-44-votes.png')})
  await selectDay(page,4)
  await expect(page.getByTestId('replay-role-groups')).toContainText('村人')
})
