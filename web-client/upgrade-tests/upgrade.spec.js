import {test,expect} from '../test-fixtures/presentation-test.js'
import {startDesktopGame} from '../test-fixtures/desktop-start.js'
import {readFileSync} from 'node:fs'
import {spawn} from 'node:child_process'
import {fileURLToPath} from 'node:url'
import path from 'node:path'
import net from 'node:net'
import {createHash} from 'node:crypto'
const root=fileURLToPath(new URL('../..',import.meta.url))
const plan=JSON.parse(readFileSync(process.env.VOCR_UPGRADE_PLAN,'utf8'))
const replay=readFileSync(path.join(root,'src/test/resources/replay/desktop-saved-v3.json'))
const library=page=>page.getByRole('region',{name:'ローカルReplay',exact:true})
const delay=ms=>new Promise(resolve=>{const timer=setTimeout(resolve,ms);timer.unref()})
async function free(port){await new Promise((resolve,reject)=>{const s=net.createServer();s.once('error',reject);s.listen(port,'127.0.0.1',()=>s.close(resolve))})}
async function child(command,cwd,env={}){
 const processChild=spawn('python3',['-B',path.join(root,'tools/guarded-process.py'),'--watch-parent='+process.pid,'--',...command],{cwd,env:{...process.env,...env},stdio:['ignore','pipe','pipe']})
 let output='';for(const stream of [processChild.stdout,processChild.stderr])stream.on('data',b=>{output+=b.toString();if(output.length>65536&&processChild.exitCode===null)processChild.kill('SIGTERM')})
 const closed=new Promise(resolve=>processChild.once('close',resolve))
 return {process:processChild,output:()=>output,stop:async()=>{
  if(processChild.exitCode===null&&processChild.signalCode===null)processChild.kill('SIGTERM')
  await Promise.race([closed,delay(10000).then(()=>{throw Error('OWN_UPGRADE_CHILD_DID_NOT_STOP')})])
 }}
}
async function start(row,request){
 await free(17074);await free(5180)
 let backend,frontend
 const stop=async()=>{try{if(frontend)await frontend.stop()}finally{if(backend)await backend.stop()}}
 try{
  backend=await child(['choom','-n','500','--','bash',path.join(row.directory,'backend/run.sh'),'17074'],row.directory)
  await expect.poll(async()=>{if(backend.process.exitCode!==null)throw Error('UPGRADE_BACKEND_EARLY_EXIT');try{return(await request.get('http://127.0.0.1:17074/api/v1/not-real',{timeout:1000})).status()}catch{return 0}},{timeout:20000}).toBe(404)
  frontend=await child([process.execPath,path.join(root,'web-client/node_modules/vite/bin/vite.js'),'preview','--outDir',path.join(row.directory,'frontend'),'--host','127.0.0.1','--port','5180','--strictPort'],path.join(root,'web-client'),{VOCR_API_URL:'http://127.0.0.1:17074'})
  await expect.poll(async()=>{if(frontend.process.exitCode!==null)throw Error('UPGRADE_FRONTEND_EARLY_EXIT');try{return(await request.get('/',{timeout:1000})).status()}catch{return 0}},{timeout:20000}).toBe(200)
  return {stop}
 }catch(error){await stop();throw error}
}
async function snapshot(page){return page.evaluate(async()=>{
 const database=async name=>new Promise((resolve,reject)=>{const q=indexedDB.open(name);q.onerror=()=>reject(q.error);q.onsuccess=()=>{
  const db=q.result,names=[...db.objectStoreNames],values={},tx=db.transaction(names)
  for(const name of names){const r=tx.objectStore(name).getAll();r.onsuccess=()=>{values[name]=r.result}}
  tx.oncomplete=()=>{db.close();resolve({version:db.version,stores:values})};tx.onabort=()=>{db.close();reject(tx.error)}
 }})
 return {records:await database('vocr.records.v1'),replays:await database('vocr.replays.v1'),audio:localStorage.getItem('vocr.audio.v1')}
})}
async function history(page){
 await page.getByRole('button',{name:'戦績を見る',exact:true}).click()
 await expect(page.getByTestId('desktop-records').getByTestId('record-play')).toHaveText('1回')
 await page.keyboard.press('Escape')
 if(!(await library(page).getByRole('button',{name:'一覧を再読込',exact:true}).isVisible()))await library(page).getByRole('button',{name:'Replay一覧',exact:true}).click()
 await expect(library(page).locator('.replay-entries > li')).toHaveCount(1)
}
async function verifyPlayback(page){
 await history(page)
 await library(page).getByRole('button',{name:'再生する',exact:true}).click()
 const dialog=page.getByRole('dialog',{name:/Replay再生/});await expect(dialog).toBeVisible()
 const dates=dialog.getByLabel('再生日'),last=await dates.locator('option').last().getAttribute('value')
 await dates.selectOption(last);await expect(dialog.getByTestId('replay-day')).toContainText('第'+last+'日')
 await dialog.getByRole('button',{name:'Replayを閉じる',exact:true}).click()
 await library(page).getByRole('button',{name:'Replay一覧を閉じる',exact:true}).click()
}
async function newGame(page){
 const created=page.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname==='/api/v1/games')
 await startDesktopGame(page);const response=await created;expect(response.status()).toBe(201)
 await expect(page.getByTestId('next-event')).toBeVisible()
 return (await response.json()).sessionId
}
test('packaged A to B, B restart and rollback A preserve exact browser history while invalidating old games',async({page,request},info)=>{
 let service;const writes=[];page.on('request',r=>{if(r.method()==='POST'&&new URL(r.url()).pathname==='/api/v1/games')writes.push('create')})
 const states=[]
 try{
  service=await start(plan.current,request);await page.goto('/')
  await page.getByRole('button',{name:'戦績を見る',exact:true}).click();await expect(page.getByTestId('desktop-records')).toBeVisible();await page.keyboard.press('Escape')
  await page.evaluate(()=>new Promise((resolve,reject)=>{const q=indexedDB.open('vocr.records.v1',1);q.onsuccess=()=>{const db=q.result,t=db.transaction('results','readwrite');t.objectStore('results').add({sequence:1,id:'a'.repeat(64),configuration:'jianyi',result:'VILLAGE_WIN'});t.oncomplete=()=>{db.close();resolve()};t.onabort=()=>reject(t.error)}}))
  await library(page).getByRole('button',{name:'Replay一覧',exact:true}).click()
  await library(page).getByLabel('録画ファイルを選ぶ').setInputFiles({name:'existing-replay.json',mimeType:'application/json',buffer:replay})
  const preview=library(page).getByRole('group',{name:'取り込みの確認（未保存）'})
  await preview.getByRole('checkbox').first().check();await preview.getByRole('button',{name:'選択した録画の取り込みを確定',exact:true}).click()
  await expect(library(page).locator('.replay-entries > li')).toHaveCount(1)
  await page.evaluate(()=>localStorage.setItem('vocr.audio.v1',JSON.stringify({version:1,bgmVolume:23,soundVolume:41,muted:true})))
  await library(page).getByRole('button',{name:'Replay一覧を閉じる',exact:true}).click()
  let previousId=await newGame(page);const original=await snapshot(page)
  for(const [label,row] of [['upgrade',plan.candidate],['restart',plan.candidate],['rollback',plan.current]]){
   await service.stop();service=null;await free(17074);await free(5180)
   service=await start(row,request);const count=writes.length
   expect((await request.get('/api/v1/games/'+previousId)).status()).toBe(404)
   await page.reload();await expect(page.getByRole('alert').filter({hasText:'已失效'})).toBeVisible()
   expect(await page.evaluate(()=>sessionStorage.getItem('vocr.guest.v1'))).toBeNull()
   await verifyPlayback(page);expect(await snapshot(page)).toEqual(original);expect(writes).toHaveLength(count)
   await page.screenshot({path:info.outputPath(label+'-history.png')})
   const nextId=await newGame(page);expect(nextId).not.toBe(previousId);previousId=nextId
   expect(await snapshot(page)).toEqual(original)
   states.push({transition:label,source:row.source_sha,historySha256:createHash('sha256').update(JSON.stringify(original)).digest('hex'),explicitNewGame:true,oldSessionStatus:404,automaticCreates:0})
  }
  await info.attach('transition-results',{body:JSON.stringify(states,null,2),contentType:'application/json'})
 }finally{if(service)await service.stop()}
})
