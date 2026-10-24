import { ReplayError } from './replayValidation.js'

// One bounded syntax pass detects duplicate keys before JSON.parse can discard them.
export function parseReplayJson(text) {
  let i=0,nodes=0
  const bad=()=>{throw new ReplayError('INVALID_JSON')}
  const space=()=>{while(/[\x20\t\r\n]/.test(text[i]||'!'))i++}
  function string(){
    const start=i++
    while(i<text.length){const ch=text[i++];if(ch==='"'){try{return JSON.parse(text.slice(start,i))}catch{bad()}}if(ch==='\\')i++}
    bad()
  }
  function decimal(token) {
    const negative=token.startsWith('-'),[mantissa,exponent='0']=token.replace(/^-/, '').toLowerCase().split('e'),parts=mantissa.split('.')
    let power=Number(exponent)-(parts[1]?.length||0),digits=parts.join('').replace(/^0+/,'')
    if(!Number.isSafeInteger(power) || Math.abs(power)>400)bad()
    if(!digits)return '0'
    while(digits.endsWith('0')){digits=digits.slice(0,-1);power++}
    return (negative?'-':'')+digits+'e'+power
  }
  function value(depth){
    if(depth>32 || ++nodes>1000000)bad()
    space();const ch=text[i]
    if(ch==='"'){string();return}
    if(ch==='{' || ch==='['){
      const object=ch==='{',close=object?'}':']',keys=new Set();i++;space()
      if(text[i]===close){i++;return}
      while(i<text.length){
        if(object){if(text[i]!=='"')bad();const key=string();if(keys.has(key))bad();keys.add(key);space();if(text[i++]!==':')bad()}
        value(depth+1);space();if(text[i]===close){i++;return}if(text[i++]!==',')bad();space()
      }
      bad()
    }
    const rest=text.slice(i),literal=rest.match(/^(true|false|null)/)
    if(literal){i+=literal[0].length;return}
    const number=rest.match(/^-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?/)
    if(!number || number[0].length>128)bad()
    const token=number[0],n=Number(token)
    if(!Number.isFinite(n) || (Number.isInteger(n) && !Number.isSafeInteger(n)))bad()
    // Refuse values whose decimal meaning would change during the JS round trip.
    if(decimal(token)!==decimal(String(n)))bad()
    i+=token.length
  }
  value(1);space();if(i!==text.length)bad()
  try{return JSON.parse(text)}catch{bad()}
}
