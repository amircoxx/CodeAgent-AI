# GitHub App PR Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a GitHub.com GitHub App connection flow so a logged-in CodeGuard user can install CodeGuard on selected repositories, browse accessible PRs, and save a CodeGuard grade for a selected PR.

**Architecture:** Extend the existing Spring Boot `github` package with user-owned connection persistence, pending setup state, GitHub App JWT and installation-token generation, repository/PR listing endpoints, and selected-PR review support. Replace the frontend URL-first GitHub PR card with a connected flow using React Query while preserving the existing manual review and review-history contracts.

**Tech Stack:** Spring Boot 3.3, Spring Security JWT app auth, Spring Data JPA/Hibernate, Java 21 `java.security` RSA signing, Java HTTP client, Next.js/React Query/TypeScript.

---

## File Structure

- Create `backend/src/main/java/com/codeguard/backend/github/entity/GitHubConnectionEntity.java`: persisted user-to-installation mapping.
- Create `backend/src/main/java/com/codeguard/backend/github/entity/GitHubPendingConnectionEntity.java`: short-lived state nonce used during setup.
- Create `backend/src/main/java/com/codeguard/backend/github/repository/GitHubConnectionRepository.java`: user-scoped connection lookup/upsert.
- Create `backend/src/main/java/com/codeguard/backend/github/repository/GitHubPendingConnectionRepository.java`: pending nonce lookup and cleanup.
- Create DTOs under `backend/src/main/java/com/codeguard/backend/github/dto/`: connection status, connect URL, repositories, pull requests, selected PR review request.
- Create `backend/src/main/java/com/codeguard/backend/github/GitHubConnectionRequiredException.java`: 409 when GitHub is not connected.
- Create `backend/src/main/java/com/codeguard/backend/github/GitHubSetupStateException.java`: 400 for invalid setup state.
- Create `backend/src/main/java/com/codeguard/backend/github/GitHubAppTokenService.java`: app JWT signing and installation-token exchange.
- Create `backend/src/main/java/com/codeguard/backend/github/GitHubConnectionService.java`: connection status, connect URL, setup binding, repo/PR listing.
- Modify `backend/src/main/java/com/codeguard/backend/shared/config/CodeGuardGitHubProperties.java`: add GitHub App settings.
- Modify `backend/src/main/resources/application.properties`: add env-backed GitHub App settings.
- Modify `backend/src/main/java/com/codeguard/backend/github/GitHubClient.java`: add token-aware methods.
- Modify `backend/src/main/java/com/codeguard/backend/github/HttpGitHubClient.java`: support installation-token requests and parse repos/PRs.
- Modify `backend/src/main/java/com/codeguard/backend/github/GitHubPullRequestReviewRequest.java`: allow either URL or selected PR payload.
- Modify `backend/src/main/java/com/codeguard/backend/github/GitHubPullRequestReviewService.java`: route selected PR reviews through the current user's installation token.
- Modify `backend/src/main/java/com/codeguard/backend/github/GitHubController.java`: add connection, setup, repository, PR list endpoints.
- Modify `backend/src/main/java/com/codeguard/backend/shared/error/GlobalExceptionHandler.java`: map connection/setup exceptions to safe responses.
- Modify `backend/src/test/java/com/codeguard/backend/github/GitHubControllerTest.java`: add backend behavior tests.
- Modify `frontend/src/types/review.ts`: add GitHub connection/repository/PR types and selected PR request shape.
- Modify `frontend/src/lib/api.ts`: add connection, connect URL, repository, PR list calls.
- Modify `frontend/src/components/github-pr-review-form.tsx`: connected repository/PR selector flow.
- Modify `frontend/src/app/page.tsx`: wire new queries and mutation payloads.

## Task 1: Backend Connection Persistence And Status

- [ ] Add failing tests in `GitHubControllerTest` for `GET /api/github/connection`: disconnected user returns `{"connected": false}`, connected user returns account metadata.
- [ ] Run `cd backend && ./mvnw -Dtest=GitHubControllerTest test` and verify the new tests fail because endpoint/entity support is missing.
- [ ] Add `GitHubConnectionEntity`, `GitHubPendingConnectionEntity`, repositories, connection DTOs, `GitHubConnectionService.getConnection()`, and `GET /api/github/connection`.
- [ ] Run `cd backend && ./mvnw -Dtest=GitHubControllerTest test` and verify the new tests pass with the existing GitHub tests.
- [ ] Commit backend connection persistence/status changes.

## Task 2: GitHub App Setup URL And Callback Binding

- [ ] Add failing tests for `GET /api/github/connect-url` creating a pending state and `GET /api/github/setup` binding an installation to the current user when state is valid.
- [ ] Add a failing test for invalid setup state returning a safe bad-request response.
- [ ] Run `cd backend && ./mvnw -Dtest=GitHubControllerTest test` and verify failures are for missing setup behavior.
- [ ] Extend `CodeGuardGitHubProperties` and `application.properties` with `app-id`, `app-slug`, `private-key`, `setup-callback-url`, `frontend-connected-redirect-url`, and `pending-state-ttl-minutes`.
- [ ] Implement pending nonce creation, setup URL generation, setup callback binding, and safe redirect URL handling.
- [ ] Run `cd backend && ./mvnw -Dtest=GitHubControllerTest test` and verify pass.
- [ ] Commit setup URL and callback binding changes.

## Task 3: Installation Tokens, Repository Listing, And PR Listing

- [ ] Add failing tests for `GET /api/github/repositories` requiring a connection and returning accessible repositories through an installation token.
- [ ] Add failing tests for `GET /api/github/repositories/{owner}/{repo}/pull-requests` requiring a connection and returning open PRs.
- [ ] Run `cd backend && ./mvnw -Dtest=GitHubControllerTest test` and verify failures are for missing listing behavior.
- [ ] Implement `GitHubAppTokenService` with Java RSA SHA-256 JWT signing and installation token exchange.
- [ ] Extend `GitHubClient`/`HttpGitHubClient` with installation-aware methods for installation metadata, repositories, and pull requests.
- [ ] Implement repository and PR listing service/controller methods.
- [ ] Run `cd backend && ./mvnw -Dtest=GitHubControllerTest test` and verify pass.
- [ ] Commit listing and token changes.

## Task 4: Selected Pull Request Review

- [ ] Add failing tests for `POST /api/github/pull-request-review` with `owner`, `repo`, and `pullRequestNumber` using the current user's installation token and saving a `GITHUB_PR` review.
- [ ] Add failing tests that selected PR review without a connection returns 409 and that existing URL-based review behavior still works.
- [ ] Run `cd backend && ./mvnw -Dtest=GitHubControllerTest test` and verify the selected-PR tests fail.
- [ ] Extend `GitHubPullRequestReviewRequest` validation to accept either `pullRequestUrl` or selected PR fields.
- [ ] Update `GitHubPullRequestReviewService` to resolve selected PRs through `GitHubConnectionService` and token-aware GitHub client calls.
- [ ] Keep `postComment` disabled/ignored for selected PR reviews.
- [ ] Run `cd backend && ./mvnw -Dtest=GitHubControllerTest test` and then `cd backend && ./mvnw test`.
- [ ] Commit selected PR review changes.

## Task 5: Frontend Connected GitHub Flow

- [ ] Update frontend types and API client functions for connection status, connect URL, repository list, PR list, and selected PR review payload.
- [ ] Rewrite `GitHubPrReviewForm` to render disconnected connect action, connected repository select, PR select, project select, and grade action.
- [ ] Wire React Query in `frontend/src/app/page.tsx` for connection, repositories, and PRs.
- [ ] Ensure successful selected PR reviews reuse `ReviewResults` and invalidate review history.
- [ ] Run `cd frontend && npm run build`; fix TypeScript/build issues.
- [ ] Commit frontend connected GitHub flow.

## Task 6: Final Verification

- [ ] Run `cd backend && ./mvnw test`.
- [ ] Run `cd frontend && npm run build`.
- [ ] Run `docker compose config --quiet`.
- [ ] Review `git status --short` and confirm only intended feature files are modified in the worktree.
- [ ] Summarize local setup requirements for a real GitHub App: app slug, app id, private key, callback URL, and read-only repository/pull-request permissions.
