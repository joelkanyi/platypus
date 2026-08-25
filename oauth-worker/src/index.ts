// Platypus OAuth backend-for-frontend, as a Cloudflare Worker.
//
// Bitbucket Cloud has no PKCE, so the authorization-code exchange needs the
// client secret. This Worker holds the secret and does the exchange/refresh so
// it never ships in the app. It is stateless: it stores nothing.
//
// Routes:
//   GET  /health         -> "ok"
//   GET  /callback       -> 302 to the app's custom scheme with ?code&state
//   POST /auth/exchange  -> { code, redirectUri } -> tokens
//   POST /auth/refresh   -> { refreshToken }      -> tokens

export interface Env {
  // Public OAuth consumer key. Safe to expose.
  BITBUCKET_CLIENT_ID: string
  // Consumer secret. Set with `wrangler secret put BITBUCKET_CLIENT_SECRET`.
  BITBUCKET_CLIENT_SECRET: string
  // Custom scheme the app registers, e.g. "platypus://oauth/callback".
  APP_REDIRECT: string
}

const TOKEN_ENDPOINT = "https://bitbucket.org/site/oauth2/access_token"

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url)
    const route = `${request.method} ${url.pathname}`
    switch (route) {
      case "GET /health":
        return new Response("ok")
      case "GET /callback":
        return handleCallback(url, env)
      case "POST /auth/exchange":
        return handleExchange(request, env)
      case "POST /auth/refresh":
        return handleRefresh(request, env)
      default:
        return new Response("not found", { status: 404 })
    }
  },
}

// Bounces the browser back into the app via its custom scheme, forwarding the
// code + state (or an error). Built as a string because URL() mangles the
// authority of non-http schemes.
function handleCallback(url: URL, env: Env): Response {
  const params = new URLSearchParams()
  const error = url.searchParams.get("error")
  const code = url.searchParams.get("code")
  const state = url.searchParams.get("state")
  if (error) params.set("error", error)
  if (code) params.set("code", code)
  if (state) params.set("state", state)
  const location = `${env.APP_REDIRECT}?${params.toString()}`
  return new Response(null, { status: 302, headers: { Location: location } })
}

async function handleExchange(request: Request, env: Env): Promise<Response> {
  if (!configured(env)) return json({ error: "oauth_not_configured" }, 503)
  const body = (await request.json()) as { code?: string; redirectUri?: string }
  if (!body.code) return json({ error: "missing_code" }, 400)
  const form = new URLSearchParams()
  form.set("grant_type", "authorization_code")
  form.set("code", body.code)
  if (body.redirectUri) form.set("redirect_uri", body.redirectUri)
  return relayToken(form, env)
}

async function handleRefresh(request: Request, env: Env): Promise<Response> {
  if (!configured(env)) return json({ error: "oauth_not_configured" }, 503)
  const body = (await request.json()) as { refreshToken?: string }
  if (!body.refreshToken) return json({ error: "missing_refresh_token" }, 400)
  const form = new URLSearchParams()
  form.set("grant_type", "refresh_token")
  form.set("refresh_token", body.refreshToken)
  return relayToken(form, env)
}

async function relayToken(form: URLSearchParams, env: Env): Promise<Response> {
  const auth = btoa(`${env.BITBUCKET_CLIENT_ID}:${env.BITBUCKET_CLIENT_SECRET}`)
  const resp = await fetch(TOKEN_ENDPOINT, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      Authorization: `Basic ${auth}`,
    },
    body: form.toString(),
  })
  if (!resp.ok) return json({ error: "bitbucket_error" }, resp.status)
  const t = (await resp.json()) as {
    access_token: string
    refresh_token?: string
    expires_in?: number
    scopes?: string
  }
  return json({
    accessToken: t.access_token,
    refreshToken: t.refresh_token ?? null,
    expiresIn: t.expires_in ?? 0,
    scopes: t.scopes ?? null,
  })
}

function configured(env: Env): boolean {
  return Boolean(env.BITBUCKET_CLIENT_ID) && Boolean(env.BITBUCKET_CLIENT_SECRET)
}

function json(obj: unknown, status = 200): Response {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { "Content-Type": "application/json" },
  })
}
