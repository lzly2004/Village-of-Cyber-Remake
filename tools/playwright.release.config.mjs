import base from '../web-client/playwright.production.config.js'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

if (process.env.VOCR_WEB_SERVER_COMMAND !== undefined) throw new Error('RELEASE_SERVER_OVERRIDE_FORBIDDEN')
const root = fileURLToPath(new URL('..', import.meta.url))
const release = process.env.VOCR_RELEASE_DIR
const output = process.env.VOCR_RELEASE_TEST_OUTPUT
const inBuild = value => typeof value === 'string' && path.isAbsolute(value) && path.relative(path.join(root, 'build'), value) !== '' && !path.relative(path.join(root, 'build'), value).startsWith('..')
if (!inBuild(release) || !inBuild(output)) throw new Error('CHECKED_RELEASE_PATH_REQUIRED')
// Shell arguments are quoted as POSIX words; JSON escaping is not shell escaping.
const quote = value => "'" + value.replaceAll("'", "'\\''") + "'"
export default {
  ...base,
  testDir: '../web-client/production-tests',
  outputDir: output,
  workers: 1,
  retries: 0,
  timeout: 180_000,
  reporter: 'list',
  use: { baseURL: 'http://127.0.0.1:5180', browserName: 'chromium', viewport: { width: 1440, height: 1000 }, trace: 'off' },
  webServer: [
    {
      command: 'bash tools/run-gradle-safe.sh runPackagedWeb --offline ' + quote('-PvocrReleaseDir=' + release),
      cwd: root, port: 17074, timeout: 120_000, reuseExistingServer: false,
      gracefulShutdown: { signal: 'SIGTERM', timeout: 5_000 },
    },
    {
      command: 'npm run preview -- --outDir ' + quote(path.join(release, 'frontend')) + ' --port 5180 --strictPort',
      cwd: path.join(root, 'web-client'), url: 'http://127.0.0.1:5180',
      env: { VOCR_API_URL: 'http://127.0.0.1:17074' }, reuseExistingServer: false,
    },
  ],
}
