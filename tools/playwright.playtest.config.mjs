import base from '../web-client/playwright.production.config.js'
import { mkdirSync, realpathSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
const root = fileURLToPath(new URL('..', import.meta.url))
if (process.env.VOCR_WEB_SERVER_COMMAND !== undefined) throw Error('PLAYTEST_SERVER_OVERRIDE_FORBIDDEN')
const checked = name => {
  const value = process.env[name]
  if (!value || !path.isAbsolute(value) || !value.startsWith(path.join(root, 'build/'))) throw Error('CHECKED_PLAYTEST_PATH_REQUIRED')
  mkdirSync(value, { recursive: true, mode: 0o700 })
  if (realpathSync(value) !== value) throw Error('PLAYTEST_SYMLINK_FORBIDDEN')
  return value
}
const payload = checked('VOCR_PLAYTEST_PAYLOAD'), run = checked('VOCR_PLAYTEST_RUN'), output = checked('VOCR_PLAYTEST_OUTPUT')
const quote = x => "'" + x.replaceAll("'", "'\\''") + "'"
if (!process.env.JAVA_HOME || !process.env.VOCR_CADDY) throw Error('PLAYTEST_FIXED_TOOLS_REQUIRED')
export default {
  ...base, testDir: '../web-client/production-tests', outputDir: output,
  workers: 1, retries: 0, timeout: 180_000, reporter: 'list',
  use: { baseURL: 'http://localhost:18500', browserName: 'chromium', viewport: { width: 1440, height: 1000 }, trace: 'off' },
  webServer: {
    command: 'python3 -B ' + quote(path.join(root, 'tools/guarded-process.py'))
      + ' -- python3 -B ' + quote(path.join(root, 'tools/playtest_service.py')) + ' --payload ' + quote(payload)
      + ' --run-dir ' + quote(run) + ' --java ' + quote(path.join(process.env.JAVA_HOME, 'bin/java'))
      + ' --caddy ' + quote(process.env.VOCR_CADDY),
    cwd: root, url: 'http://localhost:18500/', timeout: 60_000, reuseExistingServer: false,
    gracefulShutdown: { signal: 'SIGTERM', timeout: 6000 },
  },
}
