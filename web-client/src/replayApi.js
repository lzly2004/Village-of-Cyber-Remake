import { ApiError } from './api.js'
import { ReplayError, REPLAY_LIMITS, replayFromFile } from './replayValidation.js'

export async function readCompletedReplay(id, cursor, signal) {
  if (!/^[0-9a-f-]{36}$/i.test(id) || !Number.isSafeInteger(cursor) || cursor < 0) throw new ReplayError('INVALID_REQUEST')
  const response = await fetch(`/api/v1/games/${id}/replay?eventCursor=${cursor}`, { signal, cache:'no-store' })
  if (!response.ok) {
    // Error bodies are deliberately not rendered; only bounded status classification is needed here.
    throw new ApiError(response.status, { code:'REPLAY_READ_FAILED', message:'Replayを取得できません。再同期して状態を確認してください。' })
  }
  if (!response.headers.get('content-type')?.toLowerCase().startsWith('application/json')) throw new ReplayError('INVALID_REPLAY')
  const reader = response.body.getReader(), chunks = []
  let size = 0
  try {
    while (true) {
      const { done,value } = await reader.read()
      if (done) break
      size += value.length
      if (size > REPLAY_LIMITS.fileBytes) throw new ReplayError('REPLAY_TOO_LARGE')
      chunks.push(value)
    }
  } catch (error) { await reader.cancel().catch(() => {}); throw error }
  const bytes = new Uint8Array(size); let offset = 0
  for (const chunk of chunks) { bytes.set(chunk,offset); offset += chunk.length }
  try { return replayFromFile(JSON.parse(new TextDecoder('utf-8',{fatal:true}).decode(bytes))) }
  catch (error) { if (error instanceof ReplayError) throw error; throw new ReplayError('INVALID_REPLAY') }
}
