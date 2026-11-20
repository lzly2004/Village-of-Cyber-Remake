import { defineConfig } from '../web-client/node_modules/@playwright/test/index.mjs'
import { readFileSync,realpathSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
const root=fileURLToPath(new URL('..',import.meta.url)),file=process.env.VOCR_UPGRADE_PLAN
if(process.env.VOCR_WEB_SERVER_COMMAND!==undefined)throw Error('UPGRADE_SERVER_OVERRIDE_FORBIDDEN')
if(!file||!path.isAbsolute(file)||realpathSync(file)!==file||!file.startsWith(path.join(root,'build/')))throw Error('CHECKED_UPGRADE_PLAN_REQUIRED')
const plan=JSON.parse(readFileSync(file,'utf8')),directory=path.dirname(file)
for(const name of ['current','candidate'])if(plan[name].directory!==path.join(directory,name)||realpathSync(plan[name].directory)!==plan[name].directory)throw Error('UPGRADE_DIRECTORY_REFUSED')
if(plan.state!=='PREPARED_NOT_STARTED'||plan.current_origin!=='http://127.0.0.1:5180'||plan.origin_changed)throw Error('UPGRADE_ORIGIN_REFUSED')
export default defineConfig({testDir:'../web-client/upgrade-tests',workers:1,retries:0,timeout:180_000,
 outputDir:path.join(directory,'browser'),reporter:'list',
 use:{baseURL:plan.current_origin,browserName:'chromium',viewport:{width:1440,height:1000},trace:'off'}})
