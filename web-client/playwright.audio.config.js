import { defineConfig } from '@playwright/test'

// Audio artifact/loader acceptance only: no JVM, game rules or actual speaker playback.
export default defineConfig({
  testDir: './audio-tests', workers: 1, retries: 0, timeout: 180_000, reporter: 'list',
  use: { baseURL:'http://127.0.0.1:5177', browserName:'chromium', trace:'retain-on-failure' },
  webServer: { command:'npm run preview -- --host 127.0.0.1 --port 5177 --strictPort',
    url:'http://127.0.0.1:5177',reuseExistingServer:false },
})
