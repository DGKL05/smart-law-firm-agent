const CLIENT_ID = 'smart-law-spa'
const SCOPE = 'smart-law-api'
const FLOW_KEY = 'smart-law.oauth.flow'
const TOKEN_KEY = 'smart-law.oauth.token'
const PROFILE_KEY = 'smart-law.oauth.profile'
const FLOW_LIFETIME = 10 * 60 * 1000

function readSession(key) {
  try {
    return JSON.parse(sessionStorage.getItem(key) || 'null')
  } catch {
    return null
  }
}

function base64Url(bytes) {
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function randomValue() {
  return base64Url(crypto.getRandomValues(new Uint8Array(32)))
}

export async function beginAuthorization() {
  if (!globalThis.crypto?.subtle) throw new Error('请通过 localhost 或 HTTPS 打开登录页面')
  const state = randomValue()
  const verifier = randomValue()
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier))
  const redirectUri = `${window.location.origin}/oauth/callback`
  sessionStorage.setItem(FLOW_KEY, JSON.stringify({ state, verifier, redirectUri, startedAt: Date.now() }))
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: CLIENT_ID,
    scope: SCOPE,
    redirect_uri: redirectUri,
    state,
    code_challenge: base64Url(new Uint8Array(digest)),
    code_challenge_method: 'S256'
  })
  window.location.assign(`/api/oauth2/authorize?${params}`)
}

export async function completeAuthorization(query) {
  const flow = readSession(FLOW_KEY)
  if (!flow || flow.state !== query.state || Date.now() - flow.startedAt > FLOW_LIFETIME) {
    throw new Error('登录请求已失效，请重新登录')
  }
  sessionStorage.removeItem(FLOW_KEY)
  if (query.error || typeof query.code !== 'string') throw new Error('授权未完成，请重新登录')
  const response = await fetch('/api/oauth2/token', {
    method: 'POST',
    credentials: 'same-origin',
    cache: 'no-store',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', Accept: 'application/json' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: CLIENT_ID,
      code: query.code,
      code_verifier: flow.verifier,
      redirect_uri: flow.redirectUri
    })
  })
  if (!response.ok) throw new Error('登录凭证兑换失败，请重新登录')
  const token = await response.json()
  if (typeof token.access_token !== 'string' || token.token_type?.toLowerCase() !== 'bearer'
      || !Number.isFinite(token.expires_in) || token.expires_in <= 0) {
    throw new Error('认证服务未返回有效凭证')
  }
  return token
}

export function storeAccessToken(token) {
  sessionStorage.setItem(TOKEN_KEY, JSON.stringify({
    value: token.access_token,
    expiresAt: Date.now() + token.expires_in * 1000
  }))
}

export function getAccessToken() {
  const token = readSession(TOKEN_KEY)
  return token?.expiresAt > Date.now() && typeof token.value === 'string' ? token.value : ''
}

export function getProfile() {
  return getAccessToken() ? readSession(PROFILE_KEY) : null
}

export function storeProfile(profile) {
  sessionStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
}

export function clearOAuthSession() {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(PROFILE_KEY)
}
