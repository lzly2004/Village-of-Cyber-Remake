export class ApiError extends Error {
  constructor(status, body) {
    super(typeof body?.message === 'string' ? body.message : '请求未能完成')
    this.status = status
    this.code = body && !Array.isArray(body) && typeof body.code === 'string' ? body.code : undefined
  }
}

export async function request(path, { method = 'GET', body } = {}) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 15_000)
  try {
    const response = await fetch('/api/v1/games' + path, {
      method,
      signal: controller.signal,
      headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
    const expectedStatus = method === 'DELETE' ? 204 : method === 'POST' && path === '' ? 201 : 200
    if (response.ok && response.status !== expectedStatus) throw new Error('服务响应状态无法确认')
    if (response.status === 204) return null
    if (response.headers.get('content-type')?.split(';')[0].trim().toLowerCase() !== 'application/json') {
      throw new Error('服务响应不是业务JSON')
    }
    const data = await response.json()
    if (!response.ok) throw new ApiError(response.status, data)
    return data
  } finally {
    clearTimeout(timeout)
  }
}
