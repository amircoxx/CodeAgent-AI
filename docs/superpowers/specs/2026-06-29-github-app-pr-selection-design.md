# GitHub App PR Selection Design

## Purpose

CodeGuard AI should let a logged-in user connect a GitHub.com account, grant CodeGuard access to selected repositories, browse pull requests from those repositories, select a PR, and generate a saved CodeGuard grade inside the app.

The first version will not post comments back to GitHub. It will only fetch repository and pull request data, run the existing review pipeline, and save the result in CodeGuard review history.

## Current Context

CodeGuard already has:

- JWT-backed CodeGuard user accounts.
- User-owned projects and saved review history.
- A manual code review flow through `POST /api/reviews`.
- A GitHub PR review flow through `POST /api/github/pull-request-review` that accepts a public PR URL.
- Backend GitHub API integration that can fetch PR metadata, fetch changed files, filter reviewable patches, and call the existing analysis service.
- Optional server-token PR comment support, which stays out of this first connected-user flow.

The new feature should extend the existing GitHub package and React Query frontend patterns instead of creating a parallel review system.

## Scope

In scope:

- GitHub.com only.
- GitHub App installation flow with selected repository access.
- Binding a GitHub App installation to the currently logged-in CodeGuard user.
- Listing repositories available to the installation.
- Listing open pull requests for a selected repository.
- Reviewing a selected pull request and saving the grade to CodeGuard.
- Preserving the existing URL-based PR review endpoint behavior where practical.
- Clear disconnected, connected, loading, empty, and error UI states.

Out of scope:

- GitHub Enterprise Server.
- Posting comments to pull requests from the connected-user flow.
- Webhooks.
- Background synchronization.
- Multi-installation management UI beyond supporting the connected installation needed for this flow.
- Automatic PR comments or checks.

## Recommended Approach

Use a GitHub App installation flow rather than a plain OAuth App.

The GitHub App model matches the product requirement because users can grant CodeGuard access to selected repositories during installation. CodeGuard can then create short-lived installation access tokens server-side and use those tokens to list installation repositories, list pull requests, fetch PR metadata, and fetch PR files.

The first implementation should use installation identity, not user-to-server OAuth tokens. CodeGuard only needs to know which installation belongs to the current CodeGuard user. It does not need long-lived user GitHub tokens for the first flow.

## Backend Design

Add GitHub App configuration to `CodeGuardGitHubProperties`:

- `appId`
- `appSlug`
- `privateKey`
- `setupCallbackUrl`
- `frontendConnectedRedirectUrl`

Keep the existing `apiBaseUrl`, timeout, max files, max patch chars, and comment settings.

Add a `github_connections` table:

- `id`
- `user_id`
- `installation_id`
- `account_login`
- `account_type`
- `created_at`
- `updated_at`

Add a repository for user-scoped lookup by `user_id` and by `(user_id, installation_id)`. Treat the connection as owned by the CodeGuard user. A user can reconnect by installing or selecting the app again; the backend should upsert the connection for that user.

Add a GitHub App token service:

- Build a signed JWT using the app id and private key.
- Exchange the app JWT for an installation access token.
- Use the installation token for GitHub REST requests.
- Do not persist installation access tokens; they are short-lived and should be generated on demand.

Add or extend GitHub client methods:

- `fetchInstallation(Long installationId)`
- `listInstallationRepositories(String installationToken)`
- `listPullRequests(String installationToken, owner, repo)`
- Existing PR metadata and PR files methods should support an installation token for private or selected repositories.

Add endpoints under `/api/github`:

- `GET /api/github/connection`
  - Returns whether the current user has a GitHub connection and basic account metadata.

- `GET /api/github/connect-url`
  - Returns a GitHub App installation URL for the frontend to open.
  - Creates a short-lived pending connection nonce tied to the current CodeGuard user.
  - Includes that nonce in the GitHub install flow state when supported by the final install URL.

- `GET /api/github/setup`
  - Accepts GitHub's `installation_id` and state.
  - Verifies the CodeGuard user/state relationship.
  - Fetches installation account metadata.
  - Upserts the `github_connections` row for the user.
  - Redirects the user back to the frontend connected page.

- `GET /api/github/repositories`
  - Requires an existing connection.
  - Lists repositories available to the installation.

- `GET /api/github/repositories/{owner}/{repo}/pull-requests`
  - Requires an existing connection.
  - Lists open pull requests for a selected accessible repository.

- `POST /api/github/pull-request-review`
  - Preserve the existing URL-based request.
  - Add support for selected PR payloads: `owner`, `repo`, and `pullRequestNumber`.
  - When selected PR fields are used, fetch through the user's installation token rather than a global backend token.
  - Continue saving with source `GITHUB_PR` and existing review metadata fields.
  - Ignore or reject `postComment` for connected-user selected PR reviews in this version.

## Frontend Design

Replace the current GitHub PR card's URL-first experience with a connected flow:

- Disconnected state:
  - Show GitHub connection status.
  - Provide a "Connect GitHub" button.
  - Keep copy direct: the user is granting CodeGuard access to selected repositories.

- Connected state:
  - Show connected GitHub account or organization login.
  - Repository select populated from `GET /api/github/repositories`.
  - Pull request select populated from the selected repository.
  - Existing project select.
  - "Grade Pull Request" action.

- Result handling:
  - Reuse the existing `ReviewResults` panel.
  - Invalidate saved review history after successful grade generation.

The manual code review card, project form, account settings, and saved review history should remain unchanged except where small data wiring is needed.

## Data Flow

1. User logs into CodeGuard.
2. Frontend calls `GET /api/github/connection`.
3. If disconnected, user clicks "Connect GitHub."
4. Frontend calls `GET /api/github/connect-url`, stores the pending nonce in browser storage, and navigates to the returned GitHub App install URL.
5. User installs the GitHub App on selected repositories.
6. GitHub redirects to the setup callback with `installation_id` and state.
7. Backend verifies the pending state, binds the installation to the CodeGuard user, clears the pending state, and redirects to the frontend.
8. Frontend reloads connection status and repositories.
9. User selects a repository and open PR.
10. Frontend submits selected PR details to `POST /api/github/pull-request-review`.
11. Backend creates an installation token, fetches PR metadata and files, filters reviewable patches, runs the existing analysis pipeline, saves a `GITHUB_PR` review, and returns `ReviewResponse`.
12. Frontend displays the grade and refreshes review history.

## Error Handling

- Missing connection: return `409 Conflict` with a clear message such as "Connect GitHub before loading repositories."
- Invalid or stale state: return a safe setup failure and redirect to the frontend with a connection error.
- GitHub installation removed or inaccessible: return a clear reconnect message and allow the user to connect again.
- Empty repository list: show an empty state explaining that the GitHub App may need repository access.
- Empty PR list: show "No open pull requests for this repository."
- PR with no reviewable text patches: reuse the existing GitHub fetch error behavior.
- GitHub API failures: return concise user-safe messages; do not expose tokens, private keys, or raw response bodies.

## Security

- Store the GitHub App private key only in backend environment configuration.
- Do not expose installation tokens to the frontend.
- Do not persist installation access tokens.
- Scope all connection, repository, PR, and review actions to the current CodeGuard user.
- Validate setup state before binding any installation.
- Keep GitHub permissions minimal for the first version: repository metadata and pull request read access.
- Do not request write permissions or comment permissions for this slice.

## Testing

Backend tests:

- Connection status returns disconnected for a user with no connection.
- Setup callback upserts a connection for the current user when state is valid.
- Repository listing requires an existing connection and uses an installation token.
- PR listing requires an existing connection and returns open pull requests.
- Selected PR review fetches through the installation token, saves a `GITHUB_PR` review, and appears in review history.
- Existing URL-based PR review tests continue to pass.
- Missing connection and invalid setup state produce safe errors.

Frontend tests/build checks:

- Disconnected state renders a connect action.
- Connected state loads repositories and pull requests.
- Selecting a PR submits the selected PR payload and renders the returned review.
- Existing manual review and saved history behavior remains intact.
- `npm run build` passes.

Final local verification should include:

- `cd /Users/amircox/Code/codeguard-ai/backend && ./mvnw test`
- `cd /Users/amircox/Code/codeguard-ai/frontend && npm run build`
- `cd /Users/amircox/Code/codeguard-ai && docker compose config --quiet`

## Open Implementation Notes

- The exact GitHub App install URL should be verified against the current GitHub App setup URL behavior during implementation. The intended primary flow is `https://github.com/apps/{appSlug}/installations/new` with a state nonce. If that URL cannot safely round-trip state, route the GitHub setup callback to a frontend callback page that reads the pending nonce from browser storage and calls an authenticated backend bind endpoint with `installation_id`; the backend must still require a matching short-lived pending connection for the current CodeGuard user.
- The backend should prefer adding token-aware overloads to the existing GitHub client instead of duplicating PR fetching code.
- The UI should keep the existing compact operational dashboard style. This is a workflow upgrade, not a visual redesign.
