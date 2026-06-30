# GitHub OAuth Repo Connection Design

## Goal

Replace the current GitHub App installation connection flow with a GitHub OAuth flow that authorizes the logged-in CodeGuard user with GitHub's `repo` scope. CodeGuard should then use the server-side OAuth token to list repositories, list pull requests, and review a selected pull request.

This design intentionally keeps the existing CodeGuard frontend workflow: a user signs in to CodeGuard, clicks "Connect GitHub", returns to CodeGuard, selects a repository, selects an open pull request, and generates a saved review.

## Current Context

CodeGuard already has:

- JWT-based CodeGuard auth.
- A `/api/github/connect-url` endpoint used by the frontend connect button.
- A `/api/github/setup` callback endpoint.
- A `github_connections` table with one connection per CodeGuard user.
- Repository listing, pull request listing, and selected pull request review endpoints.
- A public PR URL review path that does not require a connected GitHub account.

The current connection flow is a GitHub App installation model. It stores an installation id and creates short-lived installation access tokens. That model is not the requested OAuth experience and currently makes the UI copy and backend concepts feel less like a standard production OAuth account connection.

## Scope

In scope:

- GitHub.com OAuth App authorization.
- OAuth `repo` scope.
- Server-side OAuth code exchange.
- Per-user GitHub connection storage.
- Using the stored OAuth token for connected repository and pull request reads.
- Preserving the current frontend API shape where practical.
- Clear setup/configuration names for local and deployed environments.
- Tests for OAuth connection and connected PR review behavior.

Out of scope:

- GitHub Enterprise Server.
- GitHub App installation permissions.
- GitHub webhooks.
- Automatic PR comments.
- Token refresh, because classic GitHub OAuth access tokens do not require refresh by default.
- Multi-GitHub-account switching in one CodeGuard account.

## Architecture

Use a backend-owned OAuth flow.

The frontend continues to call `GET /api/github/connect-url` with the CodeGuard JWT. The backend creates a short-lived pending state tied to the current CodeGuard user and returns a GitHub OAuth authorize URL:

`https://github.com/login/oauth/authorize?client_id=...&redirect_uri=...&scope=repo&state=...`

GitHub redirects back to `GET /api/github/setup?code=...&state=...`. The backend validates the state against the current CodeGuard user, exchanges the code with GitHub's token endpoint, fetches the authenticated GitHub user, saves the token and account metadata in `github_connections`, deletes the pending state, and redirects back to the frontend success URL.

Repository listing, PR listing, and selected PR review use the stored OAuth access token as a Bearer token. The token remains server-side and is never returned to the browser.

## Configuration

Add these backend properties:

- `codeguard.github.oauth-client-id`
- `codeguard.github.oauth-client-secret`
- `codeguard.github.oauth-scope`, default `repo`
- `codeguard.github.oauth-authorize-url`, default `https://github.com/login/oauth/authorize`
- `codeguard.github.oauth-token-url`, default `https://github.com/login/oauth/access_token`
- `codeguard.github.oauth-callback-url`

Keep these existing properties:

- `codeguard.github.api-base-url`
- `codeguard.github.frontend-connected-redirect-url`
- `codeguard.github.pending-state-ttl-minutes`
- `codeguard.github.timeout-seconds`
- `codeguard.github.max-files`
- `codeguard.github.max-patch-chars`
- `codeguard.github.comments-enabled`
- `codeguard.github.token`, still only for the existing optional server-token PR comment path and unauthenticated public PR fetch fallback.

Production deployments need the OAuth callback URL configured in the GitHub OAuth App and in backend environment variables. Local development can use `http://localhost:8080/api/github/setup`.

## Data Model

Update `github_connections` so it stores OAuth connection data:

- `user_id`
- `access_token`, required
- `token_type`, default `bearer`
- `scope`, required
- `github_user_id`, nullable only for migration safety
- `account_login`, required
- `account_type`, default `User` when GitHub does not return a type
- `created_at`
- `updated_at`

The old `installation_id` column is no longer part of the active connection model. Because the project uses Hibernate `ddl-auto=update` locally, removed fields should not be required by the entity. Deployed databases may keep the old column unless a manual migration removes it.

## Backend API

Keep these endpoints stable:

- `GET /api/github/connection`
- `GET /api/github/connect-url`
- `GET /api/github/setup`
- `GET /api/github/repositories`
- `GET /api/github/repositories/{owner}/{repo}/pull-requests`
- `POST /api/github/pull-request-review`

Behavior changes:

- `GET /api/github/connect-url` returns an OAuth authorize URL instead of a GitHub App installation URL.
- `GET /api/github/setup` requires `code` and `state`, not `installation_id` and `state`.
- `GET /api/github/connection` returns connected account metadata without relying on an installation id.
- Connected repository and PR reads use the stored OAuth token.
- Selected PR review requires a GitHub OAuth connection.
- Public PR URL review continues to work without a GitHub OAuth connection.

## GitHub Client

Extend the GitHub client boundary with OAuth-specific methods:

- Exchange OAuth authorization code for access token.
- Fetch authenticated user with an OAuth access token.
- List authenticated user repositories with an OAuth access token.

Keep token-aware overloads for:

- Fetching PR metadata.
- Fetching PR files.
- Listing open PRs.

Repository listing should use an endpoint appropriate for the connected user, such as `/user/repos?affiliation=owner,collaborator,organization_member&sort=updated&per_page=100`. The first implementation can fetch the first page only; pagination is deferred.

## Frontend UX

Keep the same interaction model and state management. Update user-facing copy:

- Disconnected state should say "Authorize CodeGuard with GitHub to load repositories and pull requests."
- Empty repository state should mention that the connected GitHub account may not have accessible repositories, not GitHub App installation permissions.
- Connected state should show the GitHub login and account type as it does today.

No OAuth access token or client secret should ever appear in frontend code, browser storage, logs, or API responses.

## Error Handling

- Missing OAuth client id or secret: return a safe setup error from connect/start or callback.
- Invalid or expired state: return the existing bad request message for invalid GitHub connection state.
- Missing `code`: return a bad request message that the GitHub OAuth code is required.
- Token exchange failure: return a user-safe GitHub connection failure message.
- GitHub user fetch failure: return a user-safe GitHub connection failure message.
- Missing connection for repository, PR listing, or selected PR review: keep the existing `409 Conflict` connection-required responses.
- GitHub API failures while loading repositories or PRs: keep concise messages and do not include tokens or raw response bodies.

## Security

- Validate `state` before exchanging or storing OAuth credentials.
- Bind pending state to the current CodeGuard user.
- Expire pending states using the existing TTL.
- Store OAuth tokens only server-side.
- Never return tokens in DTOs.
- Use HTTPS URLs in production OAuth configuration.
- Request the `repo` scope because the product needs production-ready access to public and private repositories for the connected user.

## Testing

Backend tests should cover:

- Disconnected connection status.
- Connected OAuth account status.
- Connect URL generation with GitHub OAuth authorize URL, `client_id`, `scope=repo`, callback URL, and state.
- Callback rejecting invalid state.
- Callback rejecting missing code.
- Callback exchanging code, fetching the GitHub user, saving the connection, deleting pending state, and redirecting to the frontend.
- Repository listing using the stored OAuth token.
- PR listing using the stored OAuth token.
- Selected PR review using the stored OAuth token.
- Public PR URL review still working without a connection.

Frontend tests should cover:

- Disconnected copy and connect button.
- Existing selected repository and PR review flow still submitting the same selected PR payload.

Validation should include:

- `./mvnw test`
- `npm run build`
- `docker compose config --quiet`

## Acceptance Criteria

- A logged-in CodeGuard user can start GitHub OAuth from the app.
- The callback stores an OAuth-backed GitHub connection for that CodeGuard user.
- The GitHub OAuth token is not exposed to the frontend.
- The connected flow lists accessible repositories and open pull requests using the stored OAuth token.
- Reviewing a selected PR creates the same saved review shape used by the current frontend.
- The public PR URL review path remains usable without OAuth.
- Existing auth, project, manual review, and review history behavior remain unchanged.
