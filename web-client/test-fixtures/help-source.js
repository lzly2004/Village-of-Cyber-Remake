import { readFileSync, readdirSync } from 'node:fs'
import { createHelpGroups } from '../src/helpContent.js'
const folder = new URL('../../src/main/resources/help/', import.meta.url)
export const helpGroups = createHelpGroups(Object.fromEntries(readdirSync(folder).filter(n => /^Info.*\.txt$/.test(n)).map(n => [n, readFileSync(new URL(n, folder), 'utf8')])))
