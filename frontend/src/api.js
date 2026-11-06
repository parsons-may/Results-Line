// Thin wrapper around the backend API. All calls go through /api (proxied to Spring Boot in dev).

async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!response.ok) {
    throw new Error(`Request failed (${response.status})`)
  }
  return response.json()
}

export const getInfo = () => request('/api/info')

export const startCall = () => request('/api/call/start', { method: 'POST' })

export const verify = (sessionId, fullName, dateOfBirth) =>
  request(`/api/call/${sessionId}/verify`, {
    method: 'POST',
    body: JSON.stringify({ fullName, dateOfBirth }),
  })

export const ask = (sessionId, question) =>
  request(`/api/call/${sessionId}/ask`, {
    method: 'POST',
    body: JSON.stringify({ question }),
  })
