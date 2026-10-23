import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { characters, characterDisplayName, characterKana } from '../src/characterDisplay.js'
import { playerVisual } from '../src/playerVisual.js'
import { createImageManifestLoader, resolveImage } from '../src/imageAssets.js'
import { roles } from '../src/labels.js'
const read = path => readFileSync(new URL('../../' + path, import.meta.url), 'utf8')
const manifest = JSON.parse(read('web-client/public/assets/images/manifest.json'))
const publicPlayer = (character = 'Abel', alive = true) => ({ character, alive, claimedRole: 'NONE',
  claimedRoleOrder: 0, deathReason: alive ? 'NONE' : 'NIGHT_DEATH', playerNumber: 44 })
const english = read('src/main/java/com/vocr/domain/CharacterEnglishName.java').replace(/\/\/[^\n]*/g, '')
  .split('{')[1].split('}')[0].split(/[,;]/).map(s => s.trim()).filter(Boolean).slice(1)
const titles = [...read('src/main/java/com/vocr/domain/CharacterKanjiName.java').matchAll(/^\s*([^\s(]+)\("([^"\n]*)"\)/gm)].slice(1)
const kana = read('src/main/java/com/vocr/domain/CharacterKatakanaName.java').replace(/\/\/[^\n]*/g, '')
  .split('{')[1].split('}')[0].split(/[,;]/).map(s => s.trim()).filter(Boolean).slice(1)

test('all 44 display names, titles and ordinal appearances match Java; not playerNumber', () => {
  assert.equal(english.length, 44)
  assert.equal(titles.length, 44)
  english.forEach((key, i) => {
    assert.deepEqual(characters[key], { number: i + 1, title: titles[i][1], shortName: titles[i][2], kana: kana[i] })
    assert.equal(characterDisplayName(key), titles[i][1] + ' ' + kana[i])
    assert.equal(characterKana(key), kana[i])
    for (const alive of [true, false]) {
      const v = playerVisual(publicPlayer(key, alive))
      assert.equal(v.portrait, `images/${String(i+1).padStart(2,'0')}${alive ? 's' : 'gs'}.png`)
      assert.equal(v.characterText, `images/${i+1}job.png`)
      assert.ok(resolveImage(manifest, v.portrait))
      assert.ok(resolveImage(manifest, v.characterText))
    }
  })
  assert.equal(characterDisplayName('Volker'), '双生児弟 フォルカー')
  assert.equal(characterDisplayName('Eva'), '双生児姉 エーファ')
})

test('all normal role labels follow ZY_NAMES; NONE stays a public CO status', () => {
  const java = read('src/main/java/com/vocr/ui/UIComponentFactory.java')
    .match(/ZY_NAMES\s*=\s*\{([^}]+)\}/s)[1]
  const names = [...java.matchAll(/"([^"]*)"/g)].map(m => m[1]).slice(1)
  assert.deepEqual(Object.values(roles).slice(1), names)
  assert.equal(playerVisual(publicPlayer()).roleText, '未CO')
})

test('all public CO icon domains and positive overflow retain exact order without aliasing', () => {
  for (const [role, id, max] of [['zhan',1,5],['ling',2,4],['lie',3,4]]) {
    for (let order = 1; order <= max; order++) {
      const v = playerVisual({ ...publicPlayer(), claimedRole: role, claimedRoleOrder: order })
      assert.equal(v.claimIcon, `images/yaku${id}_${order}.png`)
      assert.ok(resolveImage(manifest,v.claimIcon))
      assert.equal(v.orderText, 'CO順 ' + order)
      assert.equal(v.claimDiagnostic,null)
    }
    const over = playerVisual({ ...publicPlayer(),claimedRole:role,claimedRoleOrder:max+1 })
    assert.equal(over.claimIcon,null)
    assert.equal(over.claimDiagnostic,'CO_ICON_UNAVAILABLE')
    assert.equal(over.orderText,'CO順 '+(max+1))
    for (const order of [null,-1,0,'1',1.5,NaN,Infinity,Number.MAX_SAFE_INTEGER+1]) {
      const v=playerVisual({...publicPlayer(),claimedRole:role,claimedRoleOrder:order})
      assert.equal(v.claimIcon,null)
      assert.equal(v.claimDiagnostic,'CO_ORDER_INVALID')
    }
  }
  for (const [role, id] of [['gong',4],['mao',5]]) {
    assert.equal(playerVisual({...publicPlayer(),claimedRole:role,claimedRoleOrder:999}).claimIcon,`images/yaku${id}.png`)
  }
  for (const role of ['NONE','wu','renlang','kuangren','kuangxinzhe','yaohu','beidezhe'])
    assert.equal(playerVisual({...publicPlayer(),claimedRole:role}).claimIcon,null)
})

test('death overlays never distinguish precise night deaths; status avatar never reveals a role', () => {
  for (const deathReason of ['NIGHT_DEATH','beiyao','nightmaozhou','nighthouzhui','zhousha']) {
    const v=playerVisual({...publicPlayer('Abel',false),deathReason})
    assert.equal(v.deathIcon,'images/kami.png')
    assert.equal(v.portrait,'images/01gs.png')
  }
  for (const [reason,name] of [['EXECUTION','turi'],['DAY_CAT_CURSE','noroi'],['DAY_PURSUED','atooi'],
    ['chuxing','turi'],['daymaozhou','noroi'],['dayhouzhui','atooi']]) {
    assert.equal(playerVisual({...publicPlayer('Abel',false),deathReason:reason}).deathIcon,`images/${name}.png`)
  }
  const source=publicPlayer()
  for(const key of ['actualRole','skillTarget','nonHumanMarked','revealedRoles'])
    Object.defineProperty(source,key,{get(){throw new Error('private field read: '+key)}})
  assert.equal(playerVisual(source).portrait,'images/01s.png')
})

test('sentinels, prototype keys, malformed character/role/status have explicit bounded fallback', () => {
  for (const character of [null,undefined,'NONE','toString','__proto__','unknown','<img onerror=x>',12,{}]) {
    assert.equal(characterDisplayName(character),'人物不明（表示診断）')
    const v=playerVisual({...publicPlayer(),character})
    assert.equal(v.portrait,null)
    assert.equal(v.characterText,null)
  }
  for(const alive of [null,undefined,'true',0]) assert.equal(playerVisual({...publicPlayer(),alive}).portrait,null)
  for(const role of [null,undefined,'toString','__proto__','unknown',{}])
    assert.equal(playerVisual({...publicPlayer(),claimedRole:role}).claimDiagnostic,'CO_ROLE_UNKNOWN')
  assert.equal(playerVisual({...publicPlayer('Abel',false),deathReason:'unknown'}).deathDiagnostic,'DEATH_ICON_UNAVAILABLE')
})

test('manifest resolver only accepts known content hashes and dimensions, no arbitrary URLs', () => {
  for(const name of ['images/01s.png','images/yaku4.png','images/kami.png']) {
    const value=resolveImage(manifest,name)
    assert.match(value.url,/^\/assets\/images\/[0-9a-f]{64}\.webp$/)
  }
  for(const name of ['toString','__proto__','images/yaku1_6.png',null,{}]) assert.equal(resolveImage(manifest,name),null)
  for(const entry of [{file:'https://outside.invalid/image',size:[1,1]},
    {file:'../outside.webp',size:[1,1]},{file:'0'.repeat(64)+'.webp',size:[1,-1]},
    {file:'0'.repeat(64)+'.webp',size:[1,'1']}])
    assert.equal(resolveImage({images:{test:entry}},'test'),null)
})

test('manifest loader shares one request and failures remain rejected, never masquerade as images', async () => {
  let calls=0
  const load=createImageManifestLoader(async () => {calls++;return {ok:true,json:async()=>manifest}})
  const [a,b]=await Promise.all([load(),load()])
  assert.equal(calls,1);assert.equal(a,b)
  for(const response of [{ok:false}, {ok:true,json:async()=>({schemaVersion:0,images:{}})},
    {ok:true,json:async()=>{throw new Error('invalid json')}}]) {
    const reject=createImageManifestLoader(async()=>response)
    await assert.rejects(reject())
  }
})
