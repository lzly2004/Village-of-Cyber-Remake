import {test,expect} from '@playwright/test'
import {helpGroups} from '../src/helpContent.js'
test('game frame double clicks and outside selections never swallow reads; title artwork cannot native-drag',async({page})=>{
 await page.goto('/')
 await expect(page.locator('[data-testid=desktop-title] img').first()).toBeVisible()
 expect(await page.locator('[data-testid=desktop-title] img').first().evaluate(el=>el.draggable)).toBe(false)
 await page.evaluate(async()=>{
  const{mountFrame}=await import('/test-fixtures/frame-harness.js');mountFrame()
 })
 const frame=page.locator('#frame-probe .message-surface');await frame.dblclick()
 expect(await page.evaluate(()=>window.getSelection().toString())).toBe('');expect(await page.evaluate(()=>window.frameReads)).toBe(2)
 await page.evaluate(()=>{const el=document.querySelector('.sidebar-caption'),r=document.createRange();r.selectNodeContents(el);const s=window.getSelection();s.removeAllRanges();s.addRange(r)})
 await frame.click();expect(await page.evaluate(()=>window.frameReads)).toBe(3)
 await page.evaluate(()=>window.getSelection().removeAllRanges());const box=await frame.boundingBox();await page.mouse.move(box.x+20,box.y+25);await page.mouse.down();await page.mouse.move(box.x+box.width-20,box.y+25,{steps:12});await page.mouse.up();expect(await page.evaluate(()=>window.getSelection().toString())).toBe('')
 await page.evaluate(()=>{const input=document.createElement('input');input.id='editable-probe';input.value='外側の診断';document.body.append(input)})
 const input=page.locator('#editable-probe');await input.fill('編集できる');await input.press('Control+a');expect(await input.evaluate(el=>el.value.slice(el.selectionStart,el.selectionEnd))).toBe('編集できる')
})
test('records uses source images and next/return/title inside journal, zero API writes',async({page})=>{
 const writes=[];page.on('request',r=>{if(r.url().includes('/api/')&&r.method()!=='GET')writes.push(r.method())});await page.goto('/')
 await page.evaluate(async()=>{const{createRecordStore}=await import('/src/recordStore.js');await createRecordStore().record({sessionId:'00000000-0000-4000-8000-000000000901',configuration:'jianyi',result:'VILLAGE_WIN'})})
 await page.getByTestId('title-records').click();const scene=page.getByTestId('desktop-records');await expect(scene).toBeVisible();await expect(scene.getByTestId('record-play')).toHaveText('1回');await expect(scene.getByTestId('record-rate')).toHaveText('100.00000%');await expect(scene.getByTestId('record-current')).toHaveCount(0)
 await scene.getByTestId('record-next').click();await expect(scene.getByTestId('record-current')).toHaveText('1');await scene.getByTestId('record-return').click();await expect(scene.getByTestId('record-current')).toHaveCount(0)
 await scene.getByTestId('record-title-return').click();await expect(page.getByTestId('desktop-title')).toBeVisible();expect(writes).toEqual([])
})
test('help source hierarchy preserves every Japanese topic and no game creation, with title return',async({page})=>{
 const calls=[];page.on('request',r=>{if(r.url().includes('/api/'))calls.push(r.method())});await page.goto('/');await page.getByTestId('title-help').click();const scene=page.getByTestId('desktop-help');await expect(scene).toBeVisible()
 for(const group of helpGroups){
  await scene.getByRole('button',{name:group.title,exact:true}).click()
  for(const topic of group.topics){await scene.getByRole('button',{name:topic.title,exact:true}).click();await expect(scene.getByTestId('desktop-help-body')).toHaveText(topic.body);await scene.getByTestId('help-return').click()}
  await scene.getByTestId('help-return').click()
 }
 await scene.getByTestId('help-title-return').click();await expect(page.getByTestId('desktop-title')).toBeVisible();expect(calls).toEqual([])
})

function sourceRect(element,scene){return element.evaluate((el,selector)=>{const s=el.closest(selector).getBoundingClientRect(),r=el.getBoundingClientRect();return{x:(r.x-s.x)/s.width*1280,y:(r.y-s.y)/s.height*720,w:r.width/s.width*1280,h:r.height/s.height*720}},scene)}
function near(actual,expected){for(const key of Object.keys(expected))expect(Math.abs(actual[key]-expected[key])).toBeLessThan(.05)}
test('record and Info source geometry and assets stay intact at three viewport sizes',async({page},info)=>{
 await page.goto('/')
 await page.evaluate(async()=>{const{createRecordStore}=await import('/src/recordStore.js');await createRecordStore().record({sessionId:'00000000-0000-4000-8000-000000000902',configuration:'jianyi',result:'VILLAGE_WIN'})})
 for(const width of [1440,1024,800]){
  await page.setViewportSize({width,height:1000});await page.getByTestId('title-records').click();const records=page.getByTestId('desktop-records');await expect(records.getByTestId('record-play')).toBeVisible()
  near(await sourceRect(records.getByTestId('record-next'),'[data-testid=desktop-records]'),{x:1050,y:250,w:194,h:126})
  near(await sourceRect(records.locator('.desktop-record-title'),'[data-testid=desktop-records]'),{x:30,y:20,w:200,h:60})
  near(await sourceRect(records.getByTestId('record-play'),'[data-testid=desktop-records]'),{x:43,y:175,h:50})
  expect(await records.evaluate(el=>el.scrollWidth<=el.clientWidth+1)).toBe(true)
  await records.screenshot({path:info.outputPath('record-'+width+'.png')});if(width===1440){await records.getByTestId('record-next').click();await records.screenshot({path:info.outputPath('record-jianyi.png')})}await records.getByTestId('record-title-return').click()
  await page.getByTestId('title-help').click();const help=page.getByTestId('desktop-help')
  near(await sourceRect(help.locator('.desktop-help-choice').first(),'[data-testid=desktop-help]'),{x:80,y:80,w:220,h:50})
  near(await sourceRect(help.getByTestId('help-title-return'),'[data-testid=desktop-help]'),{x:1050,y:560,w:194,h:127})
  expect(await help.locator('.desktop-help-choice-text').first().evaluate(el=>parseFloat(getComputedStyle(el).fontSize)/el.closest('.desktop-information-scene').getBoundingClientRect().width*1280)).toBeCloseTo(20,1)
  near(await sourceRect(help.locator('.desktop-info-panel'),'[data-testid=desktop-help]'),{x:10,y:10,w:980,h:660})
  expect(await help.evaluate(el=>el.scrollWidth<=el.clientWidth+1)).toBe(true)
  await help.screenshot({path:info.outputPath('help-'+width+'.png')});if(width===1440){await help.getByRole('button',{name:helpGroups[0].title,exact:true}).click();await help.screenshot({path:info.outputPath('help-group.png')});await help.getByRole('button',{name:helpGroups[0].topics[0].title,exact:true}).click();await expect(help.getByRole('heading',{name:helpGroups[0].topics[0].title,exact:true})).toBeVisible();await help.screenshot({path:info.outputPath('help-topic.png')})}await help.getByTestId('help-title-return').click()
 }
})

test('sidebar Info and Record temporarily hide the native Replay scene and Escape restores the same library',async({page})=>{
 await page.goto('/');await page.getByTestId('title-replay').click()
 const library=page.getByTestId('desktop-replay-browser');await expect(library).toBeVisible()
 await library.evaluate(el=>{el.dataset.sameLibrary='preserved'})
 for(const label of ['遊び方・ヘルプ','戦績を見る']){
  await page.getByRole('button',{name:label,exact:true}).click()
  await expect(page.getByTestId(label==='戦績を見る'?'desktop-records':'desktop-help')).toBeVisible()
  await expect(library).toBeHidden()
  await page.keyboard.press('Escape');await expect(library).toBeVisible();await expect(library).toHaveAttribute('data-same-library','preserved')
 }
})
