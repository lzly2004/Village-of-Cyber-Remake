import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { eventDialogue } from '../src/eventDialogue.js'
import { characterNames } from '../src/characterNames.js'

const read = path => readFileSync(new URL('../../' + path, import.meta.url), 'utf8')
const catalog = JSON.parse(read('src/main/resources/lines/event_lines.json'))
const event = (eventname = 'gyfo1', ch2 = 'Erich', ch1 = 'Abel') => ({ eventname, ch1, ch2, ch3: 'Betti' })
const fixture = entry => ({ Abel: { gyfo1: entry } })
const enumValues = name => read(`src/main/java/com/vocr/domain/${name}.java`)
  .replace(/\/\/[^\n]*/g, '').split('{')[1].split('}')[0]
  .split(/[,;]/).map(value => value.trim()).filter(Boolean)
const expected = (line, target) => line.replace(/<param=(\w+)>/g,
  (_, key) => /^(talkerName|targetName|partnerName)$/.test(key) ? target : '')

test('all 44 wire/kana pairs follow actual enums including Unicode and twins', () => {
  const english = enumValues('CharacterEnglishName').slice(1)
  const kana = enumValues('CharacterKatakanaName').slice(1)
  assert.equal(english.length, 44)
  assert.deepEqual(characterNames, Object.fromEntries(english.map((key, i) => [key, kana[i]])))
  assert.equal(characterNames['Matthäus'], 'マテウス')
  assert.equal(characterNames['Günther'], 'ギュンター')
  assert.equal(characterNames.Volker, 'フォルカー')
  assert.equal(characterNames.Eva, 'エーファ')
})

test('coverage prerequisites: exactly 44 characters x 42 normal events, every default and special', () => {
  const keys = [...read('src/main/java/com/vocr/domain/EventName.java').matchAll(/^\s+(\w+)\("/gm)]
    .map(match => match[1]).filter(key => !['NONE', 'wsw'].includes(key))
  assert.equal(keys.length, 42)
  assert.deepEqual(Object.keys(catalog).sort(), Object.keys(characterNames).sort())
  let defaults = 0, specials = 0
  for (const [character, entries] of Object.entries(catalog)) {
    assert.deepEqual(Object.keys(entries).sort(), [...keys].sort())
    for (const [key, entry] of Object.entries(entries)) {
      assert.equal(typeof entry.default, 'string')
      assert.ok(entry.default.length > 0)
      const absent = Object.keys(characterNames).find(target => !Object.hasOwn(entry.special || {}, target))
      assert.ok(absent)
      assert.deepEqual(eventDialogue(event(key, absent, character)), {
        source: 'character', diagnostic: null, text: expected(entry.default, characterNames[absent]),
      })
      defaults++
      for (const [target, line] of Object.entries(entry.special || {})) {
        assert.ok(Object.hasOwn(characterNames, target))
        assert.equal(typeof line, 'string')
        assert.ok(line.length > 0)
        assert.deepEqual(eventDialogue(event(key, target, character)), {
          source: 'character', diagnostic: null, text: expected(line, characterNames[target]),
        })
        specials++
      }
    }
  }
  assert.equal(defaults, 1848)
  assert.ok(specials > 0)
  console.log(`resource branches: ${defaults} defaults, ${specials} specials`)
})

test('source samples preserve line breaks, special forms of address, and ch2 semantics', () => {
  assert.equal(eventDialogue(event()).text, '共有COするよ。\n相方はエーリッヒだ。\nよろしくね。')
  assert.equal(eventDialogue(event('gyfo1', 'Betti')).text, '共有COするよ。\n相方はベッティちゃんだ。\n一緒に頑張ろう！')
  assert.equal(eventDialogue(event('gyfo1r')).text, '相方はエーリッヒで間違いないよ。一緒に頑張ろう！')
})

test('all three recognized params use ch2; unknown params empty; ch3 has no effect', () => {
  const input = fixture({ default: '<param=talkerName>|<param=targetName>|<param=partnerName>|<param=newName>\nＣＯ 😀 <b>原文</b>' })
  assert.equal(eventDialogue(event(), input).text, 'エーリッヒ|エーリッヒ|エーリッヒ|\nＣＯ 😀 <b>原文</b>')
  assert.equal(eventDialogue({ ...event(), ch3: 'Günther' }, input).text, eventDialogue(event(), input).text)
  for (const ch2 of [null, undefined, 'NONE']) {
    assert.equal(eventDialogue({ ...event(), ch2 }, input).text, '|||\nＣＯ 😀 <b>原文</b>')
  }
})

test('special exact key wins, including empty/null content; never retry default', () => {
  for (const line of ['', null, undefined]) {
    const result = eventDialogue(event(), fixture({ default: 'default must not appear', special: { Erich: line } }))
    assert.equal(result.diagnostic, 'EMPTY_LINE')
    assert.equal(result.source, 'fallback')
  }
  assert.equal(eventDialogue(event(), fixture({ default: 'default', special: { erich: 'wrong case' } })).text, 'default')
  assert.equal(eventDialogue(event(), fixture({ default: 'default', special: { Erich: 'exact' } })).text, 'exact')
})

test('wsw bypasses character resource and equals GameStrings.NO_SACRIFICE, NONE is diagnostic', () => {
  const literal = read('src/main/java/com/vocr/ui/GameStrings.java').match(/NO_SACRIFICE\s*=\s*("(?:\\.|[^"\\])*")/)[1]
  for (const ch1 of [null, 'NONE', 'Unknown', 'Abel']) {
    assert.deepEqual(eventDialogue(event('wsw', null, ch1), null), {
      source: 'system', diagnostic: null, text: JSON.parse(literal),
    })
  }
  assert.equal(eventDialogue(event('NONE')).diagnostic, 'SENTINEL_EVENT')
})

for (const [label, value, data, diagnostic] of [
  ['null event', null, catalog, 'INVALID_EVENT'],
  ['missing event key', {}, catalog, 'INVALID_EVENT'],
  ['null event key', event(null), catalog, 'INVALID_EVENT'],
  ['NONE character', event('gyfo1', null, 'NONE'), catalog, 'UNKNOWN_CHARACTER'],
  ['missing character', { eventname: 'gyfo1' }, catalog, 'UNKNOWN_CHARACTER'],
  ['unknown character', event('gyfo1', null, 'Unknown'), catalog, 'UNKNOWN_CHARACTER'],
  ['prototype character', event('gyfo1', null, 'toString'), catalog, 'UNKNOWN_CHARACTER'],
  ['unknown target', event('gyfo1', 'unknown'), catalog, 'UNKNOWN_TARGET'],
  ['missing catalog', event(), null, 'MISSING_CHARACTER'],
  ['missing character entry', event(), {}, 'MISSING_CHARACTER'],
  ['unknown event', event('newEvent'), catalog, 'MISSING_EVENT'],
  ['prototype event', event('toString'), catalog, 'MISSING_EVENT'],
  ['missing event entry', event(), { Abel: {} }, 'MISSING_EVENT'],
  ['invalid character entry', event(), { Abel: [] }, 'INVALID_ENTRY'],
  ['invalid event entry', event(), fixture(null), 'INVALID_ENTRY'],
  ['invalid special map', event(), fixture({ default: 'ok', special: [] }), 'INVALID_ENTRY'],
  ['invalid default type', event(), fixture({ default: 123 }), 'INVALID_ENTRY'],
  ['invalid special type', event(), fixture({ default: 'ok', special: { Erich: {} } }), 'INVALID_ENTRY'],
  ['empty default', event(), fixture({ default: '' }), 'EMPTY_LINE'],
  ['missing default', event(), fixture({}), 'EMPTY_LINE'],
]) {
  test(`explicit bounded fallback: ${label}`, () => {
    const result = eventDialogue(value, data)
    assert.equal(result.source, 'fallback')
    assert.equal(result.diagnostic, diagnostic)
    assert.match(result.text, /^台词显示异常：/)
    assert.ok(!result.text.includes('Unknown'))
  })
}

// Shared local presentation clock: no game callbacks, fetch or storage are involved.
import { createTypewriter } from '../src/typewriter.js'
function controlledWriter() {
  let now=0,id=0;const jobs=new Map(),states=[]
  const writer=createTypewriter({publish:s=>states.push(s),schedule:(fn,ms)=>{jobs.set(++id,{at:now+ms,fn});return id},cancel:key=>jobs.delete(key)})
  function advance(ms){const until=now+ms;for(;;){const next=[...jobs].sort((a,b)=>a[1].at-b[1].at)[0];if(!next||next[1].at>until)break;now=next[1].at;jobs.delete(next[0]);next[1].fn()}now=until}
  return {writer,advance,states,jobs}
}
test('typewriter: 50ms boundaries, Unicode code points and line breaks are preserved',()=>{
  const {writer,advance,states}=controlledWriter();writer.reset('村😀\n人');advance(49);assert.equal(states.at(-1).text,'');advance(1);assert.equal(states.at(-1).text,'村');advance(50);assert.equal(states.at(-1).text,'村😀');advance(100);assert.deepEqual(states.at(-1),{text:'村😀\n人',complete:true})
  assert.match(read('src/main/java/com/vocr/ui/UIConstants.java'),/TYPEWRITER_DELAY_MS\s*=\s*50/)
})
test('typewriter: explicit reveal is local and late ticks cannot resurrect a reset or disposed line',()=>{
  const {writer,advance,states,jobs}=controlledWriter();writer.reset('古い台詞');writer.reveal();assert.deepEqual(states.at(-1),{text:'古い台詞',complete:true});assert.equal(jobs.size,0);writer.reset('新');advance(49);assert.equal(states.at(-1).text,'');advance(1);assert.deepEqual(states.at(-1),{text:'新',complete:true});writer.reset('未完');writer.dispose();const count=states.length;advance(10000);assert.equal(states.length,count)
})
test('typewriter: paused and reduced-motion/empty lines have finite deterministic completion',()=>{
  const {writer,advance,states}=controlledWriter();writer.reset('甲乙',{paused:true});advance(500);assert.equal(states.at(-1).text,'');writer.play();advance(50);assert.equal(states.at(-1).text,'甲');writer.pause();advance(500);assert.equal(states.at(-1).text,'甲');writer.play();advance(50);assert.deepEqual(states.at(-1),{text:'甲乙',complete:true});writer.reset('即時',{instant:true});assert.deepEqual(states.at(-1),{text:'即時',complete:true});writer.reset('');assert.deepEqual(states.at(-1),{text:'',complete:true})
})

test('typewriter: an obsolete callback cannot clear the replacement timer or double its cadence',()=>{
  const {writer,jobs,states,advance}=controlledWriter()
  writer.reset('旧');const obsolete=[...jobs.values()][0].fn
  writer.reset('新文');obsolete();writer.play()
  assert.equal(jobs.size,1)
  advance(50);assert.deepEqual(states.at(-1),{text:'新',complete:false})
  writer.pause();assert.equal(jobs.size,0)
})

test('typewriter: default clock is resolved when playback starts, allowing later clock instrumentation',()=>{
  const savedSet=globalThis.setTimeout,savedClear=globalThis.clearTimeout,states=[];const writer=createTypewriter({publish:s=>states.push(s)});let callback
  try{globalThis.setTimeout=(fn,ms)=>{assert.equal(ms,50);callback=fn;return 42};globalThis.clearTimeout=()=>{};writer.reset('字');assert.equal(typeof callback,'function');callback();assert.deepEqual(states.at(-1),{text:'字',complete:true});writer.dispose()}finally{globalThis.setTimeout=savedSet;globalThis.clearTimeout=savedClear}
})
