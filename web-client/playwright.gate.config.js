import { defineConfig } from '@playwright/test'
import base from './playwright.config.js'

export default defineConfig({
  ...base,
  testDir: './gate-tests',
  timeout: 120_000,
  webServer: [
    { ...base.webServer[0], command: process.env.VOCR_WEB_SERVER_COMMAND
      || base.webServer[0].command.replace('--args=17071', '--args="17071 --release-baseline"') },
    base.webServer[1],
  ],
})
