import { defineConfig } from '@playwright/test'
import { fileURLToPath } from 'node:url'

export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  workers: 1,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: 'http://127.0.0.1:5175',
    browserName: 'chromium',
    viewport: { width: 1440, height: 1000 },
    trace: 'retain-on-failure',
  },
  webServer: [
    {
      command: process.env.VOCR_WEB_SERVER_COMMAND
        || (process.platform === 'win32' ? 'gradlew.bat' : './gradlew')
          + ' runWeb --args=17071 --console=plain',
      cwd: fileURLToPath(new URL('..', import.meta.url)),
      port: 17071,
      timeout: 120_000,
      reuseExistingServer: false,
    },
    {
      command: 'npm run dev -- --port 5175',
      url: 'http://127.0.0.1:5175',
      env: { VOCR_API_URL: 'http://127.0.0.1:17071' },
      reuseExistingServer: false,
    },
  ],
})
