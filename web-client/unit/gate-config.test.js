import { test } from 'node:test'
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'

function readConfig(path, override) {
  const env = { ...process.env }
  delete env.VOCR_WEB_SERVER_COMMAND
  if (override !== undefined) env.VOCR_WEB_SERVER_COMMAND = override
  // Import configuration only. Never execute the configured command or start a server.
  return spawnSync(process.execPath, ['--input-type=module', '-e',
    `import config from ${JSON.stringify(new URL(path, import.meta.url).href)};
     console.log(JSON.stringify({ command: config.webServer[0].command, workers: config.workers, testDir: config.testDir }));`],
  { env, encoding: 'utf8', timeout: 10_000 })
}

test('gate keeps its standard baseline command and single worker', () => {
  const result = readConfig('../playwright.gate.config.js')
  assert.equal(result.status, 0, result.stderr)
  const config = JSON.parse(result.stdout)
  assert.equal(config.command, process.platform === 'win32'
    ? 'gradlew.bat --no-daemon --max-workers=1 --console=plain runWebTestServer --args="17071 --release-baseline"'
    : 'bash tools/run-gradle-safe.sh runWebTestServer --args="17071 --release-baseline"')
  assert.equal(config.workers, 1)
  assert.equal(config.testDir, './gate-tests')
})

for (const [label, override] of [
  ['empty override', ''],
  ['default-e2e offline override', 'bash tools/run-gradle-safe.sh runWebTestServer --args=17071 --offline'],
  ['private command', 'DO_NOT_ECHO_PRIVATE_COMMAND'],
]) {
  test('gate rejects ' + label + ' before startup', () => {
    const result = readConfig('../playwright.gate.config.js', override)
    assert.equal(result.status, 1, result.stderr)
    assert.match(result.stderr, /gate does not accept VOCR_WEB_SERVER_COMMAND/)
    assert.match(result.stderr, /Unset it/)
    assert.equal(result.stdout, '')
    assert.ok(!result.stderr.includes('DO_NOT_ECHO_PRIVATE_COMMAND'))
  })
}

test('default e2e still permits its documented override', () => {
  const result = readConfig('../playwright.config.js', 'CUSTOM_COMMAND_NOT_EXECUTED')
  assert.equal(result.status, 0, result.stderr)
  assert.equal(JSON.parse(result.stdout).command, 'CUSTOM_COMMAND_NOT_EXECUTED')
})
