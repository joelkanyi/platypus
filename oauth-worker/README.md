# Platypus OAuth Worker

The stateless OAuth backend-for-frontend for "Sign in with Bitbucket", deployed on
Cloudflare Workers. Bitbucket Cloud has no PKCE, so the token exchange needs the client
secret; this Worker holds it so it never ships in the app. It stores nothing.

## Routes

- `GET /health` — liveness check.
- `GET /callback` — Bitbucket redirects here after consent; bounces back to the app via
  `platypus://oauth/callback?code=...&state=...`.
- `POST /auth/exchange` — `{ code, redirectUri }` → `{ accessToken, refreshToken, expiresIn, scopes }`.
- `POST /auth/refresh` — `{ refreshToken }` → the same token shape.

## Deploy

1. Install deps: `npm install`
2. Log in: `npx wrangler login`
3. Set the consumer secret: `npx wrangler secret put BITBUCKET_CLIENT_SECRET`
4. Put the consumer key in `wrangler.toml` under `[vars] BITBUCKET_CLIENT_ID`.
5. Deploy: `npx wrangler deploy`

Wrangler prints the URL, e.g. `https://platypus-oauth.<subdomain>.workers.dev`.

## Register the Bitbucket consumer

In a Bitbucket workspace: Settings → OAuth consumers → Add consumer.

- Callback URL: `https://<worker-url>/callback`
- Permissions: Account (read), Repositories (read), Pull requests (read/write),
  Pipelines (read/write) as the app needs.

Copy the Key into `BITBUCKET_CLIENT_ID` (wrangler.toml) and the app's `PlatypusConfig`,
and the Secret into the Worker secret above. Then set `BACKEND_BASE_URL` in the app's
`PlatypusConfig` to the Worker URL.
