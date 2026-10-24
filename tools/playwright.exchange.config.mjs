// Preserve default-e2e commands, cwd, safe JVM entry, ports and worker/retry policy.
import { fileURLToPath } from 'node:url'
import base from '../web-client/playwright.config.js'
export default {
  ...base,
  testDir: './workflow-tests',
  webServer: base.webServer.map(server => ({
    ...server,
    cwd: server.cwd ?? fileURLToPath(new URL('../web-client/', import.meta.url)),
  })),
}
