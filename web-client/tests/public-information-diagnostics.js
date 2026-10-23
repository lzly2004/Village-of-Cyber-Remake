import { writeFile } from 'node:fs/promises'
// Passive, bounded evidence only. No new requests, sleeps, retries, or game operations.
export function publicInformationDiagnostics(page, testInfo) {
  const state = { lastCompleted: 'setup', current: 'setup', latestSnapshot: null, network: [] }
  const inFlight = new Map()
  const record = entry => { state.network.push(entry); if (state.network.length > 40) state.network.shift() }
  const path = req => new URL(req.url()).pathname.replace(/\/games\/[^/]+/, '/games/[session]')
  const started = req => { if (req.url().includes('/api/v1/games')) inFlight.set(req, { method: req.method(), path: path(req), at: Date.now() }) }
  const ended = (req, status) => {
    const entry = inFlight.get(req)
    if (!entry) return
    inFlight.delete(req); record({ ...entry, elapsedMs: Date.now() - entry.at, status })
  }
  const response = res => ended(res.request(), res.status())
  const failed = req => ended(req, 'failed')
  page.on('request', started); page.on('response', response); page.on('requestfailed', failed)
  return {
    async step(label, action) {
      state.current = label
      state.currentStartedAt = Date.now()
      const result = await action()
      state.lastCompleted = label
      state.lastDurationMs = Date.now() - state.currentStartedAt
      return result
    },
    snapshot(snapshot) {
      state.latestSnapshot = { phase: snapshot.phase, revision: snapshot.revision,
        eventCursor: snapshot.eventCursor, nextEventCursor: snapshot.nextEventCursor,
        presentedCount: snapshot.presentedEvents?.length, day: snapshot.view?.gameDay }
    },
    async finish() {
      page.off('request', started); page.off('response', response); page.off('requestfailed', failed)
      if (testInfo.status === testInfo.expectedStatus) return
      state.currentElapsedMs = Date.now() - state.currentStartedAt
      state.inFlight = [...inFlight.values()].map(entry => ({ ...entry, elapsedMs: Date.now() - entry.at }))
      // Page evaluation can be unavailable after a crash: still attach passive evidence.
      let deadline
      try {
        state.browser = await Promise.race([page.evaluate(() => {
          let saved
          try { saved = JSON.parse(sessionStorage.getItem('vocr.guest.v1')) } catch { /* diagnostic only */ }
          return { cursor: saved?.eventCursor, pendingText: document.querySelector('.event-controls > span')?.textContent,
            nextEventCount: document.querySelectorAll('[data-testid="next-event"]').length,
            stage: document.querySelector('[data-testid="game-stage"]')?.getAttribute('data-stage'),
            voteDisabled: document.querySelector('[data-testid="vote-FREE"]')?.disabled,
            visibility: document.visibilityState }
        }), new Promise(resolve => { deadline = setTimeout(() => resolve({ unavailable: 'evaluation deadline' }), 1500) })])
      } catch { state.browser = { unavailable: 'page closed/crashed' } }
      finally { clearTimeout(deadline) }
      const report = testInfo.outputPath('public-information-diagnostic.json')
      await writeFile(report, JSON.stringify(state, null, 2))
      await testInfo.attach('public-information-diagnostic', { path: report, contentType: 'application/json' })
      try {
        const image = testInfo.outputPath('public-information-failure.png')
        await page.screenshot({ path: image, timeout: 2000 })
        await testInfo.attach('public-information-failure', { path: image, contentType: 'image/png' })
      }
      catch { await testInfo.attach('public-information-screenshot-unavailable', { body: 'Page capture unavailable', contentType: 'text/plain' }) }
    },
  }
}
