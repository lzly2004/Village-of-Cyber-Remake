import base from '../web-client/playwright.production.config.js'
import { readFileSync, lstatSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
const root=fileURLToPath(new URL('..',import.meta.url)),value=process.env.VOCR_HOSTING_PLAN
if(process.env.VOCR_WEB_SERVER_COMMAND!==undefined)throw Error('HOSTING_SERVER_OVERRIDE_FORBIDDEN')
if(!value||!path.isAbsolute(value)||!value.startsWith(path.join(root,'build/hosting-private/'))||lstatSync(value).isSymbolicLink())throw Error('PRIVATE_HOSTING_PLAN_REQUIRED')
const plan=JSON.parse(readFileSync(value,'utf8'))
if(!/^[A-Za-z0-9+/]{43}=$/.test(plan.spki)||plan.private!==path.dirname(value))throw Error('INVALID_HOSTING_PLAN')
const quote=s=>"'"+s.replaceAll("'","'\\''")+"'"
export default {
 ...base,testDir:'../web-client',testMatch:['**/production-tests/experience.spec.js','**/hosting-tests/*.spec.js'],outputDir:'../build/verify/hosting-browser',
 workers:1,retries:0,timeout:180_000,
 use:{baseURL:'https://localhost:18443',browserName:'chromium',viewport:{width:1440,height:1000},trace:'off',ignoreHTTPSErrors:false,
      launchOptions:{args:['--ignore-certificate-errors-spki-list='+plan.spki]}},
 webServer:[
  {command:'bash tools/run-gradle-safe.sh runWeb --args=17075 --offline',cwd:root,port:17075,timeout:120_000,reuseExistingServer:false,gracefulShutdown:{signal:'SIGTERM',timeout:5000}},
  {command:'python3 -B tools/host-web.py serve --caddy '+quote(process.env.VOCR_CADDY)+' --directory '+quote(plan.private),cwd:root,
   port:18443,timeout:20_000,reuseExistingServer:false,gracefulShutdown:{signal:'SIGTERM',timeout:5000}},
 ],
}
