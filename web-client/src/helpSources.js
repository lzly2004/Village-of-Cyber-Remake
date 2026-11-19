import { createHelpGroups } from './helpContent.js'
const raw = import.meta.glob('../../src/main/resources/help/Info*.txt', { query: '?raw', import: 'default', eager: true })
const sources = Object.fromEntries(Object.entries(raw).map(([path, text]) => [path.split('/').at(-1), text]))
export const helpGroups = createHelpGroups(sources)
