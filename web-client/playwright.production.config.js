import { defineConfig } from '@playwright/test'
import { fileURLToPath } from 'node:url'

// Deliberately independent of the seeded/default config and its server override.
if (process.env.VOCR_WEB_SERVER_COMMAND !== undefined) {
  throw new Error('PRODUCTION_SMOKE_SERVER_OVERRIDE_FORBIDDEN')
}
export default defineConfig({
  testDir: './production-tests',
  outputDir: '../build/verify/production-browser',
  workers: 1,
  retries: 0,
  timeout: 180_000,
  reporter: 'list',
  use: {
    baseURL: 'http://127.0.0.1:5179', browserName: 'chromium',
    viewport: { width: 1440, height: 1000 },
    // No trace containing real session identifiers or full authority responses.
    trace: 'off',
  },
  webServer: [
    {
      command: 'bash tools/run-gradle-safe.sh runWeb --args=17072 --offline',
      cwd: fileURLToPath(new URL('..', import.meta.url)),
      port: 17072, timeout: 120_000, reuseExistingServer: false,
      gracefulShutdown: { signal: 'SIGTERM', timeout: 5_000 },
    },
    {
      command: 'npm run preview -- --port 5179 --strictPort',
      url: 'http://127.0.0.1:5179',
      env: { VOCR_API_URL: 'http://127.0.0.1:17072' },
      reuseExistingServer: false,
    },
  ],
})
