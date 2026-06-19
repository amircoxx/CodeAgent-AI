# CodeGuard AI

CodeGuard AI is a full-stack starter app for AI-assisted code review. The current version lets a user register, sign in, create projects, paste code in a Next.js frontend, send it to a Spring Boot backend, save review results in PostgreSQL, and display user-scoped review history.

## Tech Stack

- Frontend: Next.js App Router, React, TypeScript, Tailwind CSS, shadcn/ui-style components, TanStack Query
- Backend: Java 21, Spring Boot, Spring Web, Spring Security, Spring Data JPA, Validation, Maven
- Database: PostgreSQL 16
- Cache and future jobs: Redis 7
- DevOps: Docker Compose
- Testing: JUnit starter test

## Current Milestone

This milestone focuses on the authenticated persisted review flow:

1. Register or log in.
2. Create optional projects.
3. Paste code in the frontend.
4. Select a language and optional project.
5. Click **Analyze Code**.
6. Send `POST /api/reviews` to the Spring Boot backend with a JWT bearer token.
7. Analyze with real AI when configured, otherwise use the mock analyzer.
8. Save the structured result under the authenticated user in PostgreSQL.
9. Render only that user's saved review history and review details.

GitHub integration, Redis jobs, AWS deployment, and CI are intentionally not implemented yet.

## Folder Structure

```text
codeguard-ai/
  frontend/
  backend/
  docker-compose.yml
  README.md
```

## Run Database Services

```bash
docker compose up -d
```

PostgreSQL will be available at `localhost:5433` with:

- Database: `codeguard`
- Username: `codeguard`
- Password: `codeguard`

Redis will be available at `localhost:6379`.

## Run Backend

Use Java 21.

```bash
cd backend
./mvnw spring-boot:run
```

The backend starts at [http://localhost:8080](http://localhost:8080).

## Run Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts at [http://localhost:3000](http://localhost:3000).

## Test the API With curl

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Amir Cox","email":"amir@example.com","password":"password123"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')

curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"Login Controller Review","language":"JavaScript","code":"function test() { console.log(\"hi\") }"}'
```

Expected response shape:

```json
{
  "id": 1,
  "projectId": null,
  "projectName": null,
  "title": "Login Controller Review",
  "language": "JavaScript",
  "summary": "This JavaScript code has a high-risk issue profile because it needs clearer input validation and stronger test coverage.",
  "riskScore": 78,
  "issues": [
    {
      "title": "Missing input validation",
      "severity": "HIGH",
      "category": "SECURITY",
      "explanation": "The submitted code accepts input without showing clear validation before the value is used.",
      "suggestion": "Add request validation with DTO constraints or guard clauses before passing data into business logic.",
      "lineNumber": 12
    }
  ],
  "recommendedTests": [
    "Test Login Controller Review with invalid input"
  ]
}
```

## Authentication Endpoints

`POST http://localhost:8080/api/auth/register`

```json
{
  "name": "Amir Cox",
  "email": "amir@example.com",
  "password": "password123"
}
```

`POST http://localhost:8080/api/auth/login`

```json
{
  "email": "amir@example.com",
  "password": "password123"
}
```

Both endpoints return:

```json
{
  "token": "...",
  "user": {
    "id": 1,
    "name": "Amir Cox",
    "email": "amir@example.com"
  }
}
```

`GET http://localhost:8080/api/auth/me` requires `Authorization: Bearer <token>` and returns the current user.

## Project and Review Endpoints

`POST http://localhost:8080/api/reviews`

Request body:

```json
{
  "projectId": 1,
  "title": "Login Controller Review",
  "language": "JavaScript",
  "code": "function test() { console.log('hi') }"
}
```

`GET http://localhost:8080/api/reviews`

`GET http://localhost:8080/api/reviews/{id}`

`POST http://localhost:8080/api/projects`

`GET http://localhost:8080/api/projects`

`GET http://localhost:8080/api/projects/{id}`

All project and review endpoints require `Authorization: Bearer <token>`. Projects and reviews are scoped to the authenticated user. A missing resource or another user's resource returns HTTP 404.

`projectId` is optional on review creation. The backend validates that `title`, `language`, and `code` are present and not blank. Invalid requests return HTTP 400. A missing or cross-user `projectId` returns HTTP 404.

## JWT Configuration

The backend uses a local development JWT secret by default:

```properties
codeguard.jwt.secret=${CODEGUARD_JWT_SECRET:dev-secret-change-me}
codeguard.jwt.expiration-ms=${CODEGUARD_JWT_EXPIRATION_MS:86400000}
```

For production, set `CODEGUARD_JWT_SECRET` to a strong secret value. Do not use the default dev secret outside local development.

## AI Analysis Configuration

The backend runs without an AI API key by default. In default local mode, review submission uses `MockCodeAnalysisService`, saves the result, and returns the normal `ReviewResponse` shape.

Default mock mode:

```bash
cd backend
./mvnw spring-boot:run
```

Enable real AI analysis by setting these environment variables before starting the backend:

```bash
export CODEGUARD_AI_ENABLED=true
export CODEGUARD_AI_API_KEY=your_api_key_here
export CODEGUARD_AI_MODEL=gpt-4o-mini
export CODEGUARD_AI_ENDPOINT=https://api.openai.com/v1/chat/completions
export CODEGUARD_AI_TIMEOUT_SECONDS=20

cd backend
./mvnw spring-boot:run
```

Fallback behavior is intentionally conservative:

- If AI is disabled, the mock analyzer is used.
- If `CODEGUARD_AI_API_KEY` is missing or blank, the mock analyzer is used.
- If the AI request fails, times out, returns non-2xx, or returns invalid JSON, the mock analyzer is used.
- If the AI response has invalid severity/category values, missing arrays, blank summary, or a `riskScore` outside `0..100`, the mock analyzer is used.

The API response contract does not change between mock and AI mode.

## Next Planned Features

- GitHub App integration
- GitHub webhooks
- Redis background jobs
- Dockerized deployment
- AWS deployment
- GitHub Actions CI
- Tests with JUnit, Mockito, Testcontainers, React Testing Library, Playwright
