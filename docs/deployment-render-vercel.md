# Render and Vercel Deployment Checklist

Use this checklist to deploy CodeGuard AI as:

- PostgreSQL on Render
- Spring Boot backend on Render as a Docker Web Service
- Next.js frontend on Vercel

Do not commit real secrets. Keep AI disabled for the first deployment unless you are ready to configure a real provider key.

## 1. Create Render PostgreSQL

1. In Render, create a new PostgreSQL database.
2. Use a clear name such as `codeguard`.
3. After creation, copy these values from Render:
   - Internal host
   - Database name
   - Username
   - Password
   - Port, usually `5432`

The backend should use Render's internal database host, not the external connection host.

## 2. Create Render Backend

Create a new Render Web Service from the GitHub repo.

Settings:

```text
Root Directory: backend
Runtime: Docker
Dockerfile Path: Dockerfile
Health Check Path: /api/health
```

The backend Dockerfile is written to work when Render's root directory is `backend`.

## 3. Backend Environment Variables

Set these on the Render backend service:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://<internal-host>:5432/<database-name>
SPRING_DATASOURCE_USERNAME=<render-db-user>
SPRING_DATASOURCE_PASSWORD=<render-db-password>
CODEGUARD_JWT_SECRET=<long-random-secret>
CODEGUARD_JWT_EXPIRATION_MS=86400000
CODEGUARD_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001,http://localhost:3002
CODEGUARD_AI_ENABLED=false
CODEGUARD_GITHUB_API_BASE_URL=https://api.github.com
CODEGUARD_GITHUB_TIMEOUT_SECONDS=15
CODEGUARD_GITHUB_MAX_FILES=20
CODEGUARD_GITHUB_MAX_PATCH_CHARS=30000
```

Optional:

```bash
CODEGUARD_GITHUB_TOKEN=<github-token>
CODEGUARD_AI_API_KEY=<openai-api-key>
CODEGUARD_AI_MODEL=gpt-4o-mini
CODEGUARD_AI_ENDPOINT=https://api.openai.com/v1/chat/completions
CODEGUARD_AI_TIMEOUT_SECONDS=20
```

Notes:

- Render provides `PORT`; the backend reads it through `server.port=${PORT:8080}`.
- Do not use `dev-secret-change-me` or the `.env.example` placeholder as the production JWT secret.
- Public GitHub PR reviews work without `CODEGUARD_GITHUB_TOKEN`, subject to GitHub rate limits.
- AI can stay disabled; the mock fallback still supports demo reviews.

## 4. Backend Smoke Test

After Render deploys the backend, test:

```bash
curl https://<render-backend>.onrender.com/api/health
```

Expected shape:

```json
{
  "status": "UP",
  "service": "codeguard-backend",
  "timestamp": "..."
}
```

## 5. Deploy Vercel Frontend

Import the same GitHub repo in Vercel.

Settings:

```text
Root Directory: frontend
Framework Preset: Next.js
Install Command: npm ci
Build Command: npm run build
```

Environment variable:

```bash
NEXT_PUBLIC_API_BASE_URL=https://<render-backend>.onrender.com
NEXT_PUBLIC_API_TIMEOUT_MS=15000
```

Deploy the frontend and copy the Vercel production URL.

## 6. Final Backend CORS Update

After the Vercel frontend URL exists, update the Render backend env var:

```bash
CODEGUARD_CORS_ALLOWED_ORIGINS=https://<vercel-frontend>.vercel.app
```

If you also need local development origins during a temporary test window:

```bash
CODEGUARD_CORS_ALLOWED_ORIGINS=https://<vercel-frontend>.vercel.app,http://localhost:3000,http://localhost:3001,http://localhost:3002
```

Restart or redeploy the Render backend after changing CORS.

## 7. Live Verification

Verify the deployed app end to end:

1. `GET https://<render-backend>.onrender.com/api/health` returns `status: "UP"`.
2. Open `https://<vercel-frontend>.vercel.app`.
3. Register a new user.
4. Log in.
5. Create a project.
6. Submit a manual code review.
7. Submit a GitHub PR review with:

```text
https://github.com/octocat/Hello-World/pull/1
```

8. Confirm review history shows the saved reviews.
9. Open review detail.
10. Log out.
11. Confirm browser requests are not blocked by CORS.

## Production Safety

- Keep real API keys, database credentials, and JWT secrets only in Render or Vercel environment variables.
- Restrict `CODEGUARD_CORS_ALLOWED_ORIGINS` to the deployed frontend URL after verification.
- `localStorage` JWT storage is acceptable for this MVP; HTTP-only cookies are a future hardening step.
- `spring.jpa.hibernate.ddl-auto=update` is acceptable for this portfolio deployment, but schema migrations would be safer for a production product.
