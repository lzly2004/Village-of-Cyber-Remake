import {test,expect} from '@playwright/test'
import {readFileSync} from 'node:fs'
const fixture=JSON.parse(readFileSync(new URL('../../src/test/resources/replay/hunter-guard-display.json',import.meta.url)))
test('Replay guard histories dim until CO, include true unclaimed hunter and exclude never-claiming fake; read-only navigation',async({page},info)=>{
 const calls=[];page.on('request',r=>{if(new URL(r.url()).pathname.startsWith('/api/'))calls.push(r.method())})
 await page.goto('/')
 await page.evaluate(async r=>{const{createReplayStore}=await import('/src/replayStore.js');await createReplayStore().save(r)},fixture.replay)
 const saved=()=>page.evaluate(async()=>{const{createReplayStore}=await import('/src/replayStore.js');return JSON.stringify(await createReplayStore().list())})
 const before=await saved()
 await page.getByTestId('title-replay').click();await page.locator('[data-slot="0"]').click()
 const board=page.getByTestId('desktop-replay-player'),row=id=>board.locator('[data-guard-player="'+id+'"]')
 await expect(row(1)).toHaveAttribute('data-guard-state','unannounced')
 await expect(row(3)).toHaveCount(0)
 await page.getByRole('button',{name:'次の日',exact:true}).click()
 for(const expected of fixture.expected['3']){
  await expect(row(expected.playerIndex)).toHaveText(expected.text)
  await expect(row(expected.playerIndex)).toHaveCSS('color',expected.unannounced?'rgb(102, 102, 102)':'rgb(0, 0, 0)')
 }
 await board.screenshot({path:info.outputPath('guards-before-co.png')})
 await page.getByRole('button',{name:'次の日',exact:true}).click();await page.getByRole('button',{name:'次の日',exact:true}).click()
 await expect(row(2)).toHaveAttribute('data-guard-state','announced');await expect(row(2)).toHaveCSS('color','rgb(0, 0, 0)')
 await expect(row(1)).toHaveCSS('color','rgb(102, 102, 102)')
 await board.screenshot({path:info.outputPath('guards-after-co.png')})
 await page.getByRole('button',{name:'前の日',exact:true}).click();await page.getByRole('button',{name:'前の日',exact:true}).click()
 await expect(row(2)).toHaveAttribute('data-guard-state','unannounced')
 await page.keyboard.press('Escape')
 expect(await saved()).toBe(before);expect(calls).toEqual([])
})

test('a true hunter executed before the first guard night has no unavailable-record row',async({page},info)=>{
 const replay=structuredClone(fixture.replay)
 for(const s of replay.daySnapshots){
  Object.assign(s.players[0],{deathDay:s.dayNumber>=3?2:0,deathReason:s.dayNumber>=3?1:0,skillTargets:[],skillTarget:0})
  s.aliveCount=s.players.filter(p=>p.deathDay===0||p.deathDay>=s.dayNumber).length
 }
 replay.rawRecords=replay.rawRecords.map(line=>{
  const day=Number(line.match(/\bday=(\d+)/)?.[1]),s=replay.daySnapshots.find(s=>s.dayNumber===day)
  if(!s)return line
  if(line.startsWith('DAILY_SNAPSHOT|'))return `DAILY_SNAPSHOT|day=${day}|alive=${s.aliveCount}`
  if(!line.startsWith('STATE|'))return line
  const p=s.players[0]
  return line.replace(/\{1:[^}]*\}/,`{1:n=${p.characterNumber},r=${p.actualRole},cr=${p.claimedRole},cro=${p.claimedRoleOrder},dd=${p.deathDay},wd=${p.deathReason},nm=0,cod=${p.comingOutDay},st=0}`)
 })
 const calls=[];page.on('request',r=>{if(new URL(r.url()).pathname.startsWith('/api/'))calls.push(r.method())})
 await page.goto('/')
 await page.evaluate(async r=>{const{createReplayStore}=await import('/src/replayStore.js');await createReplayStore().save(r)},replay)
 const saved=()=>page.evaluate(async()=>{const{createReplayStore}=await import('/src/replayStore.js');return JSON.stringify(await createReplayStore().list())})
 const before=await saved()
 await page.getByTestId('title-replay').click();await page.locator('[data-slot="0"]').click()
 await page.getByRole('button',{name:'次の日',exact:true}).click()
 await expect(page.locator('[data-guard-player="1"]')).toHaveCount(0)
 await expect(page.locator('[data-guard-player="2"]')).toBeVisible()
 await page.getByTestId('desktop-replay-player').screenshot({path:info.outputPath('guard-before-action-death.png')})
 expect(await saved()).toBe(before);expect(calls).toEqual([])
})
