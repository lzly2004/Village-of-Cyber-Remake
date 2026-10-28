import { test,expect } from '@playwright/test'
import { readFileSync } from 'node:fs'
import { spawn } from 'node:child_process'
import http from 'node:http'
import https from 'node:https'
import path from 'node:path'
const plan=JSON.parse(readFileSync(process.env.VOCR_HOSTING_PLAN,'utf8')),ca=readFileSync(path.join(plan.private,'ca.pem'))
const replay=readFileSync(new URL('../../src/test/resources/replay/desktop-saved-v3.json',import.meta.url))
const library=page=>page.getByRole('region',{name:'ローカルReplay',exact:true})
function network(url,options={}){
 return new Promise((resolve,reject)=>{const u=new URL(url),client=u.protocol==='https:'?https:http
 const request=client.request(u,{ca,servername:u.hostname==='localhost'?'localhost':undefined,...options},r=>{const bytes=[];r.on('data',b=>bytes.push(b));r.on('end',()=>resolve({status:r.statusCode,headers:r.headers,body:Buffer.concat(bytes).toString()}))})
 request.on('error',reject);request.setTimeout(10_000,()=>request.destroy(Error('CONTROLLED_NETWORK_TIMEOUT')));request.end(options.body)
 })
}
async function caddy(config,ready){
 const child=spawn(process.env.VOCR_CADDY,['run','--config',path.join(plan.private,config)],{stdio:['ignore','pipe','pipe']})
 let logs='';for(const stream of [child.stdout,child.stderr])stream.on('data',d=>{logs+=d.toString();if(logs.length>65536)child.kill('SIGTERM')})
 const closed=new Promise(resolve=>child.once('close',resolve))
 const stop=async()=>{if(child.exitCode===null&&child.signalCode===null)child.kill('SIGTERM');await Promise.race([closed,new Promise((_,reject)=>{const t=setTimeout(()=>reject(Error('OWN_CADDY_DID_NOT_STOP')),5000);t.unref()})]);return logs}
 try{await expect.poll(async()=>{if(child.exitCode!==null)throw Error('CADDY_EARLY_EXIT');try{return(await network(ready)).status}catch{return 0}},{timeout:10_000}).toBe(200)}catch(e){await stop();throw e}
 return {stop}
}
async function inspect(page){
 return page.evaluate(async()=>{
  const rows=async(name,store)=>new Promise((resolve,reject)=>{const q=indexedDB.open(name,1);q.onsuccess=()=>{const db=q.result;if(!db.objectStoreNames.contains(store)){db.close();reject(Error('STORE_NOT_INITIALIZED'));return}const t=db.transaction(store),r=t.objectStore(store).getAll();r.onsuccess=()=>resolve(r.result);t.oncomplete=()=>db.close();t.onabort=()=>reject(t.error)};q.onerror=()=>reject(q.error)})
  return {guest:sessionStorage.getItem('vocr.guest.v1'),preference:localStorage.getItem('vocr.audio.v1'),records:await rows('vocr.records.v1','results'),replays:await rows('vocr.replays.v1','recordings')}
 })
}
async function openHistory(page){
 await page.getByRole('button',{name:'戦績を見る',exact:true}).click();await expect(page.getByRole('dialog',{name:'ブラウザーの戦績'}).getByTestId('record-play')).toBeVisible();await page.keyboard.press('Escape')
 await library(page).getByRole('button',{name:'Replay一覧',exact:true}).click();await expect(library(page).getByRole('button',{name:'一覧を再読込',exact:true})).toBeEnabled();await expect(library(page).locator('.replay-entries')).toHaveCount(1)
}
async function seed(page,url){
 await page.goto(url);await openHistory(page)
 await page.evaluate(()=>new Promise((resolve,reject)=>{const q=indexedDB.open('vocr.records.v1',1);q.onsuccess=()=>{const db=q.result,t=db.transaction('results','readwrite');t.objectStore('results').add({sequence:1,id:'a'.repeat(64),configuration:'jianyi',result:'VILLAGE_WIN'});t.oncomplete=()=>{db.close();resolve()};t.onabort=()=>reject(t.error)}}))
 await library(page).getByLabel('録画ファイルを選ぶ').setInputFiles({name:'fixture.json',mimeType:'application/json',buffer:replay})
 const preview=library(page).getByRole('group',{name:'取り込みの確認（未保存）'});await preview.getByRole('checkbox').first().check();await preview.getByRole('button',{name:'選択した録画の取り込みを確定',exact:true}).click();await expect(library(page).locator('.replay-entries > li')).toHaveCount(1)
 await page.evaluate(()=>localStorage.setItem('vocr.audio.v1',JSON.stringify({version:1,bgmVolume:23,soundVolume:41,muted:true})))
 await page.getByRole('button',{name:'対局を始める',exact:true}).click();await expect(page.getByTestId('next-event')).toBeVisible()
 return inspect(page)
}
async function emptyOrigin(page,url){
 const api=[];const on=r=>{if(new URL(r.url()).pathname.startsWith('/api/'))api.push(r.method())};page.on('request',on)
 try{await page.goto(url);await openHistory(page);const value=await inspect(page);expect(value.guest).toBeNull();expect(value.preference).toBeNull();expect(value.records).toEqual([]);expect(value.replays).toEqual([]);expect(api).toEqual([])}finally{page.off('request',on)}
}
for(const [label,url] of [['hostname','https://127.0.0.1:18443'],['port','https://localhost:18444']]){
 test('origin '+label+' isolates live credentials, records, Replay and preference without erasing old origin',async({page,request})=>{
  const old=await seed(page,'https://localhost:18443');const id=JSON.parse(old.guest).sessionId
  try{await emptyOrigin(page,url);await page.goto('https://localhost:18443');await expect(page.getByTestId('next-event')).toBeVisible();await openHistory(page);expect(await inspect(page)).toEqual(old)}finally{expect((await request.delete('/api/v1/games/'+id)).ok()).toBe(true)}
 })
}
test('protocol-only HTTP to HTTPS on identical hostname and port does not migrate or delete origin data',async({page,request})=>{
 let server=await caddy('plain-origin.json','http://localhost:18500');let id
 try{const old=await seed(page,'http://localhost:18500');id=JSON.parse(old.guest).sessionId;await server.stop();server=await caddy('tls-origin.json','https://localhost:18500');await emptyOrigin(page,'https://localhost:18500');await server.stop();server=await caddy('plain-origin.json','http://localhost:18500');await page.goto('http://localhost:18500');await expect(page.getByTestId('next-event')).toBeVisible();await openHistory(page);expect(await inspect(page)).toEqual(old)}finally{await server.stop();if(id)expect((await request.delete('/api/v1/games/'+id)).ok()).toBe(true)}
})
test('trusted local CA and hostname are verified; wrong trust and wrong hostname fail',async()=>{
 expect((await network('https://localhost:18443/')).status).toBe(200)
 await expect(network('https://localhost:18443/',{ca:[]})).rejects.toThrow()
 await expect(network('https://127.0.0.1:18443/',{servername:'not-localhost.invalid'})).rejects.toThrow()
})
test('real static host denies backend/config/unknown API, constrains methods and redirects without credentials',async()=>{
 for(const p of ['/backend/server.jar','/config/probability.json','/metadata/runtime-build.json','/tools/web_hosting.py','/assets/missing.webp']){const r=await network('https://localhost:18443'+p);expect(r.status).toBe(404);expect(r.body).not.toContain('<html')}
 const api=await network('https://localhost:18443/api/v1/not-real');expect(api.status).toBe(404);expect(api.body).not.toContain('<html')
 expect((await network('https://localhost:18443/',{headers:{Host:'evil.invalid'}})).status).toBe(421)
 expect((await network('https://localhost:18443/',{method:'POST',body:'x'})).status).toBe(405)
 const r=await network('http://localhost:18080/api/synthetic-secret?secret=marker');expect(r.status).toBe(308);expect(r.headers.location).toBe('https://localhost:18443/');expect(r.headers.location).not.toContain('synthetic-secret')
 expect((await network('http://localhost:18080/',{method:'POST',body:'x'})).status).toBe(426)
 const home=await network('https://localhost:18443/');expect(home.headers['cache-control']).toBe('no-store');expect(home.headers['referrer-policy']).toBe('no-referrer');expect(home.headers['x-content-type-options']).toBe('nosniff')
})
test('proxy preserves upstream API status/headers and strips forged forwarding headers; unknown failure is not retried or logged',async()=>{
 let writes=0
 const upstream=http.createServer((req,res)=>{
  if(req.url.startsWith('/api/drop')){writes++;req.socket.destroy();return}
  if(req.url.startsWith('/api/limit')){res.writeHead(429,{'Content-Type':'application/json','Retry-After':'7','Cache-Control':'no-store'});res.end(JSON.stringify({code:'REQUEST_LIMIT',message:'Synthetic refusal'}));return}
  res.writeHead(201,{'Content-Type':'application/json','Location':'/api/v1/games/synthetic-capability','Cache-Control':'no-store'});res.end(JSON.stringify({url:req.url,method:req.method,forwarded:req.headers.forwarded??null,xff:req.headers['x-forwarded-for']??null,real:req.headers['x-real-ip']??null}))
 });await new Promise(resolve=>upstream.listen(17101,'127.0.0.1',resolve))
 const proxy=await caddy('probe.json','https://localhost:18510/')
 let logs=''
 try{
  const r=await network('https://localhost:18510/api/probe?eventCursor=3',{method:'POST',body:'{}',headers:{'Content-Type':'application/json',Forwarded:'for=evil','X-Forwarded-For':'evil','X-Real-IP':'evil'}})
  expect(r.status).toBe(201);expect(r.headers.location).toBe('/api/v1/games/synthetic-capability');expect(JSON.parse(r.body)).toEqual({url:'/api/probe?eventCursor=3',method:'POST',forwarded:null,xff:null,real:null})
  const limited=await network('https://localhost:18510/api/limit');expect(limited.status).toBe(429);expect(limited.headers['retry-after']).toBe('7');expect(JSON.parse(limited.body).code).toBe('REQUEST_LIMIT')
  const dropped=await network('https://localhost:18510/api/drop/synthetic-capability?secret=synthetic-secret',{method:'POST',body:'{}'});expect(dropped.status).toBe(502);expect(dropped.body).toBe('Proxy request failed.');expect(writes).toBe(1)
  const large=await network('https://localhost:18510/api/probe',{method:'POST',body:'x'.repeat(16385),headers:{'Content-Length':'16385'}});expect(large.status).toBe(413)
 }finally{logs=await proxy.stop();await new Promise(resolve=>upstream.close(resolve))}
 expect(logs).not.toContain('synthetic-capability');expect(logs).not.toContain('synthetic-secret')
})
