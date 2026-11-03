import {test,expect} from '@playwright/test'
import {readFileSync,mkdirSync,writeFileSync} from 'node:fs'
import {execFileSync} from 'node:child_process'
import {createHash} from 'node:crypto'
import {helpGroups} from '../src/helpContent.js'
const replay=JSON.parse(readFileSync(new URL('../../src/test/resources/replay/exchange-v3.json',import.meta.url)))[0]
const output=new URL('../../build/verify/desktop-closeout/',import.meta.url)
async function capture(page,node,name){
 await page.evaluate(()=>document.fonts.ready);await expect(node.locator('[data-image-state="loading"],[data-image-state="waiting"]')).toHaveCount(0)
 mkdirSync(output,{recursive:true});const data=await node.screenshot({path:new URL(name+'.png',output).pathname})
 const metadata={name,bytes:data.length,sha256:createHash('sha256').update(data).digest('hex'),source:'actual Chromium render; fixed legitimate Replay and empty Record; not user acceptance',git_head:execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),working_tree_dirty:execFileSync('git',['status','--porcelain=v1'],{encoding:'utf8'}).length>0}
 writeFileSync(new URL(name+'.json',output),JSON.stringify(metadata,null,2)+'\n')
}
test('desktop scene acceptance: 768/800 and 125/150 percent retain text, source controls and zero game requests',async({page})=>{
 const calls=[];page.on('request',r=>{if(new URL(r.url()).pathname.startsWith('/api/'))calls.push(r.method())})
 await page.goto('/');await page.evaluate(async replay=>{const{createReplayStore}=await import('/src/replayStore.js');await createReplayStore().saveMany([replay])},replay)
 for(const [width,zoom] of [[1440,1],[1024,1],[800,1],[768,1],[1440,1.25],[1440,1.5]]){
  await page.setViewportSize({width,height:1000});await page.evaluate(zoom=>{document.body.style.zoom=String(zoom)},zoom)
  await expect(page.getByTestId('desktop-title')).toBeVisible();await capture(page,page.getByTestId('desktop-title'),'title-'+width+'-'+zoom)
  await page.getByTestId('title-help').click();const help=page.getByTestId('desktop-help');await help.getByRole('button',{name:helpGroups[4].title,exact:true}).click();await help.getByRole('button',{name:helpGroups[4].topics[8].title,exact:true}).click()
  const body=help.locator('.desktop-help-body');await body.focus();await page.keyboard.press('Control+End');await expect.poll(()=>body.evaluate(el=>el.scrollHeight-el.clientHeight-el.scrollTop)).toBeLessThan(3)
  await expect(body).toContainText('2026-06-24');await expect(help.getByTestId('help-title-return')).toBeVisible();await capture(page,help,'help-'+width+'-'+zoom);await help.getByTestId('help-title-return').click()
  await page.getByTestId('title-records').click();const records=page.getByTestId('desktop-records');await expect(records.getByTestId('record-play')).toHaveText('0回')
  for(let n=0;n<7;n++)await records.getByTestId('record-next').click();await expect(records.getByRole('heading',{level:3})).toHaveText('大型村');await capture(page,records,'record-'+width+'-'+zoom);await records.getByTestId('record-title-return').click()
  await page.getByTestId('title-replay').click();await page.locator('[data-slot="0"]').click();const player=page.getByTestId('desktop-replay-player');await expect(player).toBeVisible();await capture(page,player,'replay-'+width+'-'+zoom)
  await page.getByRole('button',{name:'Replayを閉じる',exact:true}).click();await page.getByTestId('replay-return').click()
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1)).toBe(true)
 }
 expect(calls).toEqual([]);expect(await page.evaluate(()=>performance.getEntriesByType('resource').filter(r=>/\.(woff2?|ttf|otf)(\?|$)/i.test(r.name)).length)).toBe(0)
})
