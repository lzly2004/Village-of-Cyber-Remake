import { defineConfig } from '@playwright/test'
import base from './playwright.config.js'

// Explicit opt-in invocation only; independent directory keeps default tests discovery clean.
export default defineConfig({ ...base, testDir: './diagnostic-probes',
  outputDir: '../build/web030-review/probe-results', retries: 0, workers: 1 })
