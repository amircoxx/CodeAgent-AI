# CodeGuard AI

CodeGuard AI is a full-stack starter app for AI-assisted code review. The current version lets a user paste code in a Next.js frontend, send it to a Spring Boot backend, and display a structured mock review response.

## Tech Stack

- Frontend: Next.js App Router, React, TypeScript, Tailwind CSS, shadcn/ui-style components, TanStack Query
- Backend: Java 21, Spring Boot, Spring Web, Spring Security, Spring Data JPA, Validation, Maven
- Database: PostgreSQL 16
- Cache and future jobs: Redis 7
- DevOps: Docker Compose
- Testing: JUnit starter test

## Current Milestone

This milestone focuses only on the working full-stack connection:

1. Paste code in the frontend.
2. Select a language.
3. Click **Analyze Code**.
4. Send `POST /api/reviews` to the Spring Boot backend.
5. Render the structured mock review result.

Authentication, OpenAI integration, GitHub integration, Redis jobs, AWS deployment, and CI are intentionally not implemented yet.

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
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -d '{"language":"JavaScript","code":"function test() { console.log(\"hi\") }"}'
```

Expected response shape:

```json
{
  "summary": "Mock review summary here.",
  "riskLevel": "MEDIUM",
  "language": "JavaScript",
  "issues": [
    {
      "category": "BUG",
      "severity": "MEDIUM",
      "title": "Example logic issue",
      "description": "This is a mock issue returned by the backend.",
      "suggestion": "This is where a suggested fix would appear."
    }
  ],
  "improvedCode": "// improved code example"
}
```

## Backend Endpoint

`POST http://localhost:8080/api/reviews`

Request body:

```json
{
  "language": "JavaScript",
  "code": "function test() { console.log('hi') }"
}
```

The backend validates that `language` and `code` are present and not blank. Invalid requests return HTTP 400.

## Next Planned Features

- OpenAI API structured JSON integration
- Save reviews to PostgreSQL
- Review history page
- Authentication with Spring Security/JWT
- GitHub App integration
- GitHub webhooks
- Redis background jobs
- Dockerized deployment
- AWS deployment
- GitHub Actions CI
- Tests with JUnit, Mockito, Testcontainers, React Testing Library, Playwright
