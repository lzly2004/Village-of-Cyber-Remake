import { defineConfig } from '@playwright/test'
import base from './playwright.config.js'

// Fail before starting any server; a default-e2e override may omit the baseline flag.
if (process.env.VOCR_WEB_SERVER_COMMAND !== undefined) {
  throw new Error('WEB-017 gate does not accept VOCR_WEB_SERVER_COMMAND. Unset it and use the standard --release-baseline entry. Offline overrides in README apply only to default e2e.')
}

export default defineConfig({
  ...base,
  testDir: './gate-tests',
  timeout: 120_000,
  webServer: [
    { ...base.webServer[0], command: base.webServer[0].command
      .replace('--args=17071', '--args="17071 --release-baseline"') },
    base.webServer[1],
  ],
})
