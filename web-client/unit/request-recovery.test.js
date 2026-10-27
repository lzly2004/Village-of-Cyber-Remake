import { test } from 'node:test'
import assert from 'node:assert/strict'
import { ApiError, request } from '../src/api.js'
import { recoveryKind, requireUpdate, validCredential } from '../src/requestRecovery.js'

test('recovery requires exact status/code AND operation; unknown replies never prove rejection', () => {
  const cases = [
    [400,'INVALID_REQUEST','read','cursor'], [400,'HTTP_ERROR','read','unknown'],
    [400,'INVALID_REQUEST','write','rejected'], [409,'STALE_REVISION','write','sync'],
    [409,'INVALID_STATE','write','sync'], [409,'STALE_REVISION','read','unknown'],
    [409,'GATEWAY','write','unknown'], [404,'GAME_NOT_FOUND','read','expired'],
    [404,'HTTP_ERROR','write','unknown'], [404,'GAME_NOT_FOUND','create','unknown'],
    [503,'SESSION_LIMIT','create','capacity'], [503,'SESSION_LIMIT','write','unknown'],
    [503,'HTTP_ERROR','create','unknown'], [500,'INTERNAL_ERROR','write','unknown'],
    [413,'HTTP_ERROR','write','rejected'], [415,'JSON_REQUIRED','create','rejected'],
    [429,'HTTP_ERROR','write','unknown'], [200,'INVALID_REQUEST','write','unknown'],
    [401,'INVALID_REQUEST','write','unknown'], [400,'toString','write','unknown'],
  ]
  for (const [status,code,operation,want] of cases)
    assert.equal(recoveryKind(new ApiError(status,{code}),operation),want,`${status}/${code}/${operation}`)
  for(const value of [new Error('offline'),null,{status:400,code:'INVALID_REQUEST'}])
    assert.equal(recoveryKind(value,'write'),'unknown')
})

test('malformed business envelopes cannot masquerade as a known code', () => {
  for(const body of [null,undefined,[],['INVALID_REQUEST'],'INVALID_REQUEST',9]) {
    const error=new ApiError(400,body)
    assert.equal(error.code,undefined)
    assert.equal(recoveryKind(error,'write'),'unknown')
  }
})

test('credential requires a string UUID v4, not coercible arrays or lookalike ids', () => {
  const id='00000000-0000-4000-8000-000000000040'
  assert.equal(validCredential(id),true)
  for(const v of [[id],null,'../../id',id.toUpperCase().replace('8000','A000'),id.replace('4000','0000')])
    assert.equal(validCredential(v),false)
})

test('success requires a coherent event window before state replacement', () => {
  const base={revision:1,phase:'DAY_DECISION',eventCursor:2,nextEventCursor:3,events:[{}],presentedEvents:[{}],view:{players:[]},selections:{},legalVoteActions:[],legalTargetSelections:[],claimInquiry:{roles:[],players:[]}}
  assert.equal(requireUpdate(base),base)
  for(const v of [null,{}, {...base,revision:NaN},{...base,nextEventCursor:4}, {...base,eventCursor:-1},{...base,presentedEvents:[]},{...base,view:null},{...base,phase:'UNKNOWN'},{...base,claimInquiry:null}])
    assert.throws(()=>requireUpdate(v),/响应结构/)
})

test('request timeout aborts its single fetch and clears timer without retry',async()=>{
  const old={fetch:globalThis.fetch,setTimeout:globalThis.setTimeout,clearTimeout:globalThis.clearTimeout}
  let expire, calls=0,cleared=false
  try{
    globalThis.setTimeout=(fn,ms)=>{assert.equal(ms,15000);expire=fn;return 40}
    globalThis.clearTimeout=id=>{assert.equal(id,40);cleared=true}
    globalThis.fetch=(_url,{signal})=>{calls++;return new Promise((_,reject)=>signal.addEventListener('abort',()=>reject(new DOMException('timeout','AbortError'))))}
    const pending=request('/capability',{method:'PUT',body:{expectedRevision:1}})
    expire()
    await assert.rejects(pending,{name:'AbortError'})
    assert.equal(calls,1);assert.equal(cleared,true)
  }finally{Object.assign(globalThis,old)}
})

test('unexpected successful HTTP status never acknowledges a command or deletion',async()=>{
  const original=globalThis.fetch
  try{
    for(const [method,path,status] of [['GET','/id',204],['DELETE','/id',200],['POST','',200],['PUT','/id/vote-target',202]]){
      globalThis.fetch=async()=>new Response(status===204?null:'{}',{status})
      await assert.rejects(request(path,{method}),/响应状态/)
    }
  }finally{globalThis.fetch=original}
})

test('a gateway HTML response containing a known JSON code is not business evidence',async()=>{
  const original=globalThis.fetch
  try{
    globalThis.fetch=async()=>new Response(JSON.stringify({code:'INVALID_REQUEST',message:'proxy'}),{status:400,headers:{'content-type':'text/html'}})
    await assert.rejects(request('/id?eventCursor=9'),error=>!(error instanceof ApiError)&&/业务JSON/.test(error.message))
  }finally{globalThis.fetch=original}
})
