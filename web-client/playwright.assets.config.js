import { defineConfig } from '@playwright/test'

// Static image acceptance needs neither a Java service nor a game session.
export default defineConfig({
  testDir: './asset-tests',
  workers: 1,
  retries: 0,
  timeout: 120_000,
  reporter: 'list',
  use: { baseURL: 'http://127.0.0.1:5176', browserName: 'chromium', trace: 'retain-on-failure' },
  webServer: {
    command: 'npm run preview -- --host 127.0.0.1 --port 5176 --strictPort',
    url: 'http://127.0.0.1:5176', reuseExistingServer: false,
  },
})
