# CodeGuard AI

[![CI](../../actions/workflows/ci.yml/badge.svg)](../../actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)
![Next.js](https://img.shields.io/badge/Next.js-15-black)
![TypeScript](https://img.shields.io/badge/TypeScript-5-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791)

CodeGuard AI is a full-stack AI-assisted code review platform. It lets developers register, organize reviews by project, submit manual code snippets or public GitHub pull request URLs, and receive structured review feedback with risk scores, issues, remediation guidance, recommended tests, and saved review history.

The app is built to feel like a developer workflow tool rather than a chatbot: reviews are authenticated, persisted, user-scoped, project-aware, and clearly labeled as manual reviews or GitHub PR reviews.

## Key Features

- JWT registration/login with BCrypt password hashing.
- User-owned projects and review history.
- Manual code review flow saved with `source: "MANUAL"`.
- GitHub pull request review flow saved with `source: "GITHUB_PR"`.
- Optional GitHub PR summary comment posting after review approval.
- GitHub PR metadata display: owner, repo, PR number, URL, and PR title.
- AI analysis abstraction with a safe mock fallback for local/demo use.
- Clean JSON error responses for validation, auth, and ownership failures.
- Public health endpoint for deployment checks.
- Dockerfiles and Docker Compose support for demo deployment.
- GitHub Actions CI for backend tests and frontend production builds.

## Screenshots

Coming soon. Add real screenshots under [docs/screenshots](docs/screenshots/README.md) after running the local app.

- Auth screen
- Dashboard
- Manual review result
- GitHub PR review result
- Review history/detail view

## Resume Highlights

- Built a full-stack AI-powered code review platform using Spring Boot, Next.js, PostgreSQL, and JWT authentication.
- Integrated GitHub Pull Request analysis by fetching PR diffs, running AI-assisted review, persisting structured findings, and optionally posting a summary comment back to GitHub.
- Implemented user-owned projects and reviews with secure JWT authentication, BCrypt password hashing, and protected API routes.
- Designed a resilient AI analysis layer with strict JSON validation and mock fallback for local development and demos.
- Added CI checks, Docker support, health checks, and structured API error handling for production readiness.

## Tech Stack

- Frontend: Next.js App Router, React, TypeScript, Tailwind CSS, TanStack Query
- Backend: Java 21, Spring Boot, Spring Security, Spring Data JPA, Jakarta Validation, Maven
- Database: PostgreSQL 16
- Test database: H2 for backend tests
- Local services: Docker Compose, Redis 7 placeholder for future background jobs
- CI: GitHub Actions

## Architecture Overview

Frontend:

- Next.js/React/TypeScript UI with Tailwind styling.
- TanStack Query for API calls, loading states, cache invalidation, and detail/history refresh.
- JWT stored in `localStorage` for the MVP.
- `NEXT_PUBLIC_API_BASE_URL` controls the backend URL and defaults to `http://localhost:8080`.

Backend:

- Spring Boot REST API with thin controllers and service-layer business logic.
- Spring Security JWT authentication and user ownership checks.
- JPA/PostgreSQL persistence for users, projects, reviews, issues, and recommended tests.
- `CodeAnalysisService` abstraction supports AI-backed analysis with mock fallback.
- GitHub PR fetcher parses PR URLs, calls GitHub's REST API, filters noisy patches, and reviews the combined diff.
- GitHub PR comment posting is opt-in and server-side only, using `CODEGUARD_GITHUB_TOKEN` when explicitly requested.
- Global exception handling returns consistent JSON errors without leaking stack traces.

Database:

- `users`
- `projects`
- `code_reviews`
- `review_issues`
- `code_review_recommended_tests`

## Demo Flow

Use this flow for a recruiter/interviewer demo:

1. Register a new account.
2. Log in.
3. Create a project.
4. Submit a manual code review.
5. Submit a public GitHub PR URL:

```text
https://github.com/octocat/Hello-World/pull/1
```

6. View review history.
7. Open review details.
8. Observe `Manual` and `GitHub PR` badges.
9. Confirm GitHub PR metadata appears in the detail view.
10. Optionally request a GitHub PR summary comment when backend comments are configured.
11. Log out and confirm protected UI is hidden.

## Demo Script

Narration flow:

1. Register or log in and point out JWT-protected routes.
2. Create a project to show reviews can be organized.
3. Submit a small manual code snippet and explain that the app can use real AI when configured, but safely falls back to mock analysis for local demos.
4. Submit `https://github.com/octocat/Hello-World/pull/1` to show GitHub PR diff review.
5. Open review history and show the `Manual` and `GitHub PR` badges.
6. Open review details and walk through risk score, issue severity, explanations, suggestions, and recommended tests.
7. Show GitHub PR metadata in the detail view.
8. Optionally enable the GitHub comment checkbox to post a concise review summary back to the PR.
9. Mention user ownership: another user cannot see this user's projects or reviews.
10. Mention repo readiness: backend tests, frontend build, Docker support, health endpoint, and GitHub Actions CI.

Seed demo data with the helper script after the backend is running:

```bash
cd /Users/amircox/Code/codeguard-ai
chmod +x scripts/demo-seed.sh
./scripts/demo-seed.sh
```

The script uses `http://localhost:8080` by default. Override it with `API_BASE_URL` if needed:

```bash
API_BASE_URL=http://localhost:8081 ./scripts/demo-seed.sh
```

The script uses `demo@example.com` / `password123`. If the user already exists, it logs in with that account.

## Running Locally

Local development runs Postgres and Redis in Docker, then runs the backend and frontend directly on your machine.

Start database services:

```bash
cd /Users/amircox/Code/codeguard-ai
docker compose up -d postgres redis
```

Start the backend:

```bash
cd /Users/amircox/Code/codeguard-ai/backend
./mvnw spring-boot:run
```

The backend starts at [http://localhost:8080](http://localhost:8080).

Start the frontend:

```bash
cd /Users/amircox/Code/codeguard-ai/frontend
npm install
npm run dev
```

The frontend usually starts at [http://localhost:3000](http://localhost:3000). If that port is occupied, Next.js chooses another port.

## Docker Setup

Run the full demo stack:

```bash
cd /Users/amircox/Code/codeguard-ai
docker compose up --build
```

Services:

- PostgreSQL: `localhost:5433`
- Redis: `localhost:6379`
- Backend: `localhost:8080`
- Frontend: `localhost:3000`

If port 3000 is already in use, run Compose with a different host port:

```bash
cd /Users/amircox/Code/codeguard-ai
FRONTEND_PORT=3001 docker compose up --build
```

If Docker reports local storage or Docker Desktop errors, restart Docker Desktop or prune Docker build cache, then rerun the command. The app can still be run in local development mode with only Postgres/Redis in Docker.

## Environment Variables

Copy the example file for local overrides:

```bash
cd /Users/amircox/Code/codeguard-ai
cp .env.example .env
```

Do not commit real secrets.

Database:

```bash
POSTGRES_DB=codeguard
POSTGRES_USER=codeguard
POSTGRES_PASSWORD=codeguard
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/codeguard
SPRING_DATASOURCE_USERNAME=codeguard
SPRING_DATASOURCE_PASSWORD=codeguard
```

Auth:

```bash
PORT=8080
CODEGUARD_JWT_SECRET=replace-with-a-long-random-secret
CODEGUARD_JWT_EXPIRATION_MS=86400000
CODEGUARD_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001,http://localhost:3002
```

AI:

```bash
CODEGUARD_AI_ENABLED=false
CODEGUARD_AI_API_KEY=
CODEGUARD_AI_MODEL=gpt-4o-mini
CODEGUARD_AI_ENDPOINT=https://api.openai.com/v1/chat/completions
CODEGUARD_AI_TIMEOUT_SECONDS=20
```

GitHub:

```bash
CODEGUARD_GITHUB_TOKEN=
CODEGUARD_GITHUB_API_BASE_URL=https://api.github.com
CODEGUARD_GITHUB_OAUTH_CLIENT_ID=
CODEGUARD_GITHUB_OAUTH_CLIENT_SECRET=
CODEGUARD_GITHUB_OAUTH_SCOPE=repo
CODEGUARD_GITHUB_OAUTH_CALLBACK_URL=http://localhost:8080/api/github/setup
CODEGUARD_GITHUB_FRONTEND_CONNECTED_REDIRECT_URL=http://localhost:3000/?github=connected
CODEGUARD_GITHUB_TIMEOUT_SECONDS=15
CODEGUARD_GITHUB_MAX_FILES=20
CODEGUARD_GITHUB_MAX_PATCH_CHARS=30000
CODEGUARD_GITHUB_COMMENTS_ENABLED=false
```

Frontend:

```bash
FRONTEND_PORT=3000
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

Connected repository and pull request selection uses a GitHub OAuth App. For local development, create a GitHub OAuth App with callback URL `http://localhost:8080/api/github/setup`, then set `CODEGUARD_GITHUB_OAUTH_CLIENT_ID` and `CODEGUARD_GITHUB_OAUTH_CLIENT_SECRET`. The OAuth scope defaults to `repo` so CodeGuard can load public and private repositories for the connected user. OAuth access tokens are stored server-side and are never sent to the frontend.

Public GitHub PR URL review fetches still work without OAuth, subject to GitHub rate limits. Posting PR comments is opt-in per review and requires `CODEGUARD_GITHUB_COMMENTS_ENABLED=true` plus `CODEGUARD_GITHUB_TOKEN`; that server token is used only by the backend and is never sent to the frontend.

## Deployment

CodeGuard AI is deployment-ready as two services plus a hosted PostgreSQL database:

- Backend: Spring Boot API container or Java service.
- Frontend: Next.js app.
- Database: hosted PostgreSQL.

For the current recommended deployment target, use the exact Render and Vercel checklist in [docs/deployment-render-vercel.md](docs/deployment-render-vercel.md).

Deployment checklist:

1. Create a hosted PostgreSQL database.
2. Set backend database variables:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
3. Set a secure `CODEGUARD_JWT_SECRET`. Do not use the local development default in production.
4. Set AI variables if using real AI:
   - `CODEGUARD_AI_ENABLED=true`
   - `CODEGUARD_AI_API_KEY`
   - `CODEGUARD_AI_MODEL`
   - `CODEGUARD_AI_ENDPOINT`
   - `CODEGUARD_AI_TIMEOUT_SECONDS`
5. Create a GitHub OAuth App with callback URL `https://your-backend.example.com/api/github/setup`, then set:
   - `CODEGUARD_GITHUB_OAUTH_CLIENT_ID`
   - `CODEGUARD_GITHUB_OAUTH_CLIENT_SECRET`
   - `CODEGUARD_GITHUB_OAUTH_SCOPE=repo`
   - `CODEGUARD_GITHUB_OAUTH_CALLBACK_URL=https://your-backend.example.com/api/github/setup`
   - `CODEGUARD_GITHUB_FRONTEND_CONNECTED_REDIRECT_URL=https://your-frontend.example.com/?github=connected`
6. Optionally set `CODEGUARD_GITHUB_TOKEN` for higher public GitHub API rate limits or GitHub PR comment posting.
7. If enabling GitHub PR comments, set:
   - `CODEGUARD_GITHUB_COMMENTS_ENABLED=true`
8. Deploy the backend and set `PORT` if the hosting platform requires a dynamic port.
9. Confirm backend health:

```bash
curl https://your-backend.example.com/api/health
```

10. Deploy the frontend with:

```bash
NEXT_PUBLIC_API_BASE_URL=https://your-backend.example.com
```

10. Set backend CORS to the deployed frontend origin:

```bash
CODEGUARD_CORS_ALLOWED_ORIGINS=https://your-frontend.example.com
```

For multiple allowed origins, use a comma-separated list:

```bash
CODEGUARD_CORS_ALLOWED_ORIGINS=https://your-frontend.example.com,https://preview.example.com
```

11. Test register/login, project creation, manual review, GitHub PR review, optional PR comment posting, and review history.

Production safety notes:

- Do not commit real API keys, database credentials, or JWT secrets.
- AI can remain disabled; the mock fallback still works for demos.
- GitHub token is optional for public PR review fetches, but required for opt-in PR comment posting.
- `localStorage` JWT storage is acceptable for this MVP, but secure HTTP-only cookies would be a better production hardening step later.
- `GET /api/health` is public and intended for deployment health checks.

## Running Tests

Backend:

```bash
cd /Users/amircox/Code/codeguard-ai/backend
./mvnw test
```

Frontend:

```bash
cd /Users/amircox/Code/codeguard-ai/frontend
npm run build
```

End-to-end browser tests:

Install Playwright browsers once:

```bash
cd /Users/amircox/Code/codeguard-ai/frontend
npx playwright install
```

Start the local services before running E2E tests:

```bash
cd /Users/amircox/Code/codeguard-ai
docker compose up -d postgres redis

cd /Users/amircox/Code/codeguard-ai/backend
CODEGUARD_AI_ENABLED=false CODEGUARD_GITHUB_COMMENTS_ENABLED=false ./mvnw spring-boot:run

cd /Users/amircox/Code/codeguard-ai/frontend
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 npm run dev
```

Run the tests from another terminal:

```bash
cd /Users/amircox/Code/codeguard-ai/frontend
PLAYWRIGHT_BASE_URL=http://localhost:3000 npm run test:e2e
```

Use the interactive runner when debugging:

```bash
cd /Users/amircox/Code/codeguard-ai/frontend
PLAYWRIGHT_BASE_URL=http://localhost:3000 npm run test:e2e:ui
```

The E2E suite covers registration/login/logout, project creation, manual code review, review result/history display, and the GitHub PR review UI path with comment posting left unchecked. The GitHub PR browser test mocks the PR-review API response so it does not require a real GitHub token, real AI, comment posting, or GitHub availability.

E2E is documented as a manual quality check for now. CI currently runs backend tests and the frontend production build; adding E2E to CI should be a separate reliability slice that starts the database, backend, frontend, and any required API stubs deterministically.

Compose validation:

```bash
cd /Users/amircox/Code/codeguard-ai
docker compose config --quiet
```

CI runs the same backend and frontend checks on `push` and `pull_request`.

## API Overview

Public:

- `GET /api/health`
- `POST /api/auth/register`
- `POST /api/auth/login`

Protected:

- `GET /api/auth/me`
- `POST /api/projects`
- `GET /api/projects`
- `GET /api/projects/{id}`
- `POST /api/reviews`
- `GET /api/reviews`
- `GET /api/reviews/{id}`
- `POST /api/github/pull-request-review`

Projects and reviews are scoped to the authenticated user. Missing resources and cross-user resources return HTTP 404.

## Curl Smoke Tests

Health check:

```bash
curl http://localhost:8080/api/health
```

Register:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo User","email":"demo@example.com","password":"password123"}'
```

Login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","password":"password123"}'
```

Create a project:

```bash
TOKEN="<paste_token_here>"

curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"CodeGuard Backend","description":"Spring Boot backend review project"}'
```

Manual review:

```bash
TOKEN="<paste_token_here>"
PROJECT_ID=1

curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"projectId\":$PROJECT_ID,\"title\":\"Login Controller Review\",\"language\":\"JavaScript\",\"code\":\"function login(input) { return input }\"}"
```

GitHub PR review:

```bash
TOKEN="<paste_token_here>"
PROJECT_ID=1

curl -X POST http://localhost:8080/api/github/pull-request-review \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"projectId\":$PROJECT_ID,\"pullRequestUrl\":\"https://github.com/octocat/Hello-World/pull/1\"}"
```

GitHub PR review with an opt-in summary comment:

```bash
TOKEN="<paste_token_here>"
PROJECT_ID=1

curl -X POST http://localhost:8080/api/github/pull-request-review \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"projectId\":$PROJECT_ID,\"pullRequestUrl\":\"https://github.com/octocat/Hello-World/pull/1\",\"postComment\":true}"
```

The comment request saves the review first. If comment posting is disabled, missing a token, or rejected by GitHub, the response still returns the saved review with `githubCommentPosted: false` and a clean `githubCommentError`.

## Final Verification Checklist

```bash
cd /Users/amircox/Code/codeguard-ai/backend
./mvnw test

cd /Users/amircox/Code/codeguard-ai/frontend
npm run build

cd /Users/amircox/Code/codeguard-ai/frontend
PLAYWRIGHT_BASE_URL=http://localhost:3000 npm run test:e2e

cd /Users/amircox/Code/codeguard-ai
docker compose config --quiet
curl http://localhost:8080/api/health
```

Manual browser verification:

- Register or log in.
- Create a project.
- Submit a manual review.
- Submit `https://github.com/octocat/Hello-World/pull/1`.
- Confirm review history includes both reviews.
- Confirm `Manual` and `GitHub PR` badges appear.
- Open detail view and inspect issues, severity, suggestions, and recommended tests.
- For a repository where your backend token can comment, enable the PR comment checkbox and confirm the GitHub comment result appears in the review detail.
- Refresh the page and confirm saved reviews remain.
- Log out and confirm protected UI is hidden.

## Current Limitations

- GitHub webhooks are not implemented yet.
- GitHub PR comments are opt-in only; automatic PR comments are not implemented.
- Redis is included for future background jobs but not used by current review flows.
- JWT is stored in `localStorage` for MVP simplicity.
- Docker full-stack verification can be affected by local Docker Desktop disk/cache state.

## Future Improvements

- GitHub App integration.
- Background review jobs using Redis-backed queues.
- Automatic PR comments after review approval.
- CI publishing and deployment previews.
- Playwright smoke tests for the full browser demo flow.
