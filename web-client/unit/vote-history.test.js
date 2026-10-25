import { test } from 'node:test'
import assert from 'node:assert/strict'
import { votingMethodText, voteRoundText } from '../src/voteHistoryText.js'

test('投票方式の既知値と未知値を区別する', () => {
  assert.deepEqual(['DESIGNATED', 'FREE', 'GREY'].map(votingMethodText), ['指定投票', '自由投票', 'グレー投票'])
  for (const value of ['FUTURE', 'toString', null, undefined])
    assert.equal(votingMethodText(value), '未対応の投票方式')
})

test('同票の全首位と権威の票数を順序どおり表示する', () => {
  const round = Object.freeze({ maximumVotes: 3, leaders: Object.freeze([4, 2]) })
  assert.equal(voteRoundText(round, n => 'P' + n), '3票：P4・P2（同票・再投票）')
  assert.deepEqual(round.leaders, [4, 2])
  assert.equal(voteRoundText({ maximumVotes: 5, leaders: [2] }, String), '5票：2（処刑）')
})

test('Linuxの既定e2eコマンドは安全入口のconsole引数を重複しない', async () => {
  if (process.platform !== 'linux') return
  const previous = process.env.VOCR_WEB_SERVER_COMMAND
  delete process.env.VOCR_WEB_SERVER_COMMAND
  try {
    const { default: config } = await import('../playwright.config.js?default-command-regression')
    assert.equal(config.webServer[0].command, 'bash tools/run-gradle-safe.sh runWebTestServer --args=17071')
  } finally {
    if (previous === undefined) delete process.env.VOCR_WEB_SERVER_COMMAND
    else process.env.VOCR_WEB_SERVER_COMMAND = previous
  }
})
