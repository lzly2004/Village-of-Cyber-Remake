export class ApiError extends Error {
  constructor(status, body) {
    super(body.message || '请求未能完成')
    this.status = status
    this.code = body.code
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
    if (response.status === 204) return null
    const data = await response.json()
    if (!response.ok) throw new ApiError(response.status, data)
    return data
  } finally {
    clearTimeout(timeout)
  }
}
