import {test,expect} from '@playwright/test'
import {readFileSync,mkdirSync,writeFileSync} from 'node:fs'
import {createHash} from 'node:crypto'
import {desktopClaims,desktopHome,desktopSelection,desktopClaimPlayers} from '../test-fixtures/desktop-navigation.js'
const bytes=readFileSync(new URL('../test-fixtures/desktop-reference-public.json',import.meta.url))
const reference=JSON.parse(bytes)
const sourceHash=createHash('sha256').update(bytes).digest('hex')
async function capture(page,name){
 const stage=page.getByTestId('stage-surface'),image=await stage.screenshot()
 const folder=new URL('../../build/verify/desktop-reference-pairs/',import.meta.url);mkdirSync(folder,{recursive:true})
 writeFileSync(new URL(name+'.png',folder),image)
 writeFileSync(new URL(name+'.json',folder),JSON.stringify({kind:'Web render of actual Swing public DTO',fixture_sha256:sourceHash,bytes:image.length,sha256:createHash('sha256').update(image).digest('hex'),surface:await stage.boundingBox()},null,2))
}
for(const scene of ['vote','settlement'])test('same-state desktop public reference: '+scene,async({page})=>{
 const snapshot=reference[scene],id='00000000-0000-4000-8000-000000000052'
 await page.setViewportSize({width:1920,height:1080})
 await page.route('**/api/v1/games',r=>r.fulfill({status:201,json:{sessionId:id,update:snapshot}}))
 await page.route('**/api/v1/games/'+id+'?**',r=>r.fulfill({json:snapshot}))
 await page.addInitScript(({id,cursor})=>sessionStorage.setItem('vocr.guest.v1',JSON.stringify({sessionId:id,eventCursor:cursor})),{id,cursor:snapshot.eventCursor})
 await page.goto('/')
 const writes=[];page.on('request',r=>{if(['POST','PUT','DELETE'].includes(r.method()))writes.push(r.url())})
 if(scene==='vote'){
  await expect(page.getByTestId('desktop-information')).toBeVisible();await capture(page,'vote')
  await desktopClaims(page);await capture(page,'claims-menu')
  await desktopHome(page);await page.getByRole('button',{name:'指示する',exact:true}).click();await capture(page,'orders-menu')
  await page.getByRole('button',{name:'指定する',exact:true}).click();await capture(page,'designation-menu')
  for(const type of ['vote','divination','guard']){await desktopSelection(page,type);await capture(page,type==='vote'?'vote-selection':type+'-selection')}
  await desktopClaimPlayers(page);await capture(page,'claim-selection')
  await desktopHome(page);await page.getByTestId('vote').click();await capture(page,'vote-confirm')
 }else{
  await expect(page.getByTestId('settlement-board')).toBeVisible()
  await expect(page.getByTestId('settlement-fate')).toHaveCount(snapshot.view.players.length)
  await capture(page,'settlement')
 }
 expect(writes).toEqual([])
 expect(JSON.parse(await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))).eventCursor).toBe(snapshot.eventCursor)
})
