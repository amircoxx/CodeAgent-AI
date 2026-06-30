# GitHub OAuth Repo Connection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the GitHub App installation connection with GitHub OAuth using `repo` scope while preserving the existing connected repository, pull request, and review workflow.

**Architecture:** The backend owns the OAuth flow: it creates a per-user state, redirects to GitHub OAuth, exchanges the callback code server-side, stores the user token in `github_connections`, and uses that token for connected GitHub reads. The frontend keeps the same API calls and selection UI, with copy updated from installation language to OAuth authorization language.

**Tech Stack:** Spring Boot, Spring Security/JWT, JPA/Hibernate, Java `HttpClient`, Jackson, H2 tests, Next.js, React Query, TypeScript.

---

## File Structure

- Modify `backend/src/main/java/com/codeguard/backend/shared/config/CodeGuardGitHubProperties.java`: add OAuth client, secret, scope, authorize URL, token URL, and callback URL properties.
- Modify `backend/src/main/resources/application.properties`: wire OAuth environment variables with local-safe defaults.
- Modify `backend/src/main/java/com/codeguard/backend/github/entity/GitHubConnectionEntity.java`: replace installation-specific fields with OAuth token/account metadata.
- Modify `backend/src/main/java/com/codeguard/backend/github/dto/GitHubConnectionResponse.java`: keep response token-free and make installation id optional/removed from active behavior.
- Create `backend/src/main/java/com/codeguard/backend/github/GitHubOAuthToken.java`: value object for OAuth token exchange result.
- Create `backend/src/main/java/com/codeguard/backend/github/GitHubAuthenticatedUser.java`: value object for `/user` metadata.
- Modify `backend/src/main/java/com/codeguard/backend/github/GitHubClient.java`: add OAuth exchange, authenticated user fetch, and OAuth repository listing methods.
- Modify `backend/src/main/java/com/codeguard/backend/github/HttpGitHubClient.java`: implement OAuth token exchange and OAuth-backed GitHub reads.
- Modify `backend/src/main/java/com/codeguard/backend/github/GitHubConnectionService.java`: generate OAuth authorize URL, complete OAuth callback, and expose stored access token for connected reads.
- Modify `backend/src/main/java/com/codeguard/backend/github/GitHubController.java`: change setup callback parameters from `installation_id` to `code`.
- Modify `backend/src/main/java/com/codeguard/backend/github/GitHubPullRequestReviewService.java`: use the OAuth access token for selected PR review.
- Modify `backend/src/test/java/com/codeguard/backend/github/GitHubControllerTest.java`: update and add backend TDD coverage for OAuth behavior.
- Modify `frontend/src/components/github-pr-review-form.tsx`: update disconnected and empty-repository copy.
- Modify `frontend/src/types/review.ts`: remove active frontend dependency on `installationId`.
- Modify `frontend/tests/e2e/review-flow.spec.ts`: add disconnected GitHub OAuth copy coverage if no existing test asserts that state.
- Modify `README.md` and `docs/deployment-render-vercel.md`: document OAuth App env vars and callback setup.

## Task 1: OAuth Config, DTOs, and Entity Shape

**Files:**
- Modify: `backend/src/test/java/com/codeguard/backend/github/GitHubControllerTest.java`
- Modify: `backend/src/main/java/com/codeguard/backend/shared/config/CodeGuardGitHubProperties.java`
- Modify: `backend/src/main/resources/application.properties`
- Modify: `backend/src/main/java/com/codeguard/backend/github/entity/GitHubConnectionEntity.java`
- Modify: `backend/src/main/java/com/codeguard/backend/github/dto/GitHubConnectionResponse.java`
- Create: `backend/src/main/java/com/codeguard/backend/github/GitHubOAuthToken.java`
- Create: `backend/src/main/java/com/codeguard/backend/github/GitHubAuthenticatedUser.java`

- [ ] **Step 1: Write failing OAuth connection status tests**

Update `GitHubControllerTest` test properties to add:

```java
"codeguard.github.oauth-client-id=test-client-id",
"codeguard.github.oauth-client-secret=test-client-secret",
"codeguard.github.oauth-scope=repo",
"codeguard.github.oauth-authorize-url=https://github.com/login/oauth/authorize",
"codeguard.github.oauth-token-url=https://github.com/login/oauth/access_token",
"codeguard.github.oauth-callback-url=http://localhost:8080/api/github/setup",
```

Replace `connectionStatusReturnsInstallationAccountWhenConnected` with:

```java
@Test
void connectionStatusReturnsOAuthAccountWhenConnected() throws Exception {
  String token = register("amir@example.com");
  gitHubConnectionRepository.save(new GitHubConnectionEntity(
      userRepository.findActiveByEmail("amir@example.com").orElseThrow(),
      "oauth-token",
      "bearer",
      "repo",
      123456L,
      "amircox",
      "User"
  ));

  mockMvc.perform(get("/api/github/connection")
          .header("Authorization", "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.connected").value(true))
      .andExpect(jsonPath("$.installationId").doesNotExist())
      .andExpect(jsonPath("$.accountLogin").value("amircox"))
      .andExpect(jsonPath("$.accountType").value("User"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd backend && ./mvnw test -Dtest=GitHubControllerTest#connectionStatusReturnsOAuthAccountWhenConnected
```

Expected: compile failure because `GitHubConnectionEntity` does not have the OAuth constructor and/or `GitHubConnectionResponse` still emits installation-centric data.

- [ ] **Step 3: Add OAuth properties and value objects**

Update `CodeGuardGitHubProperties` record with:

```java
String oauthClientId,
String oauthClientSecret,
String oauthScope,
String oauthAuthorizeUrl,
String oauthTokenUrl,
String oauthCallbackUrl,
```

Add helper methods:

```java
public boolean hasOAuthCredentials() {
  return oauthClientId != null && !oauthClientId.isBlank()
      && oauthClientSecret != null && !oauthClientSecret.isBlank();
}

public String resolvedOAuthScope() {
  return oauthScope == null || oauthScope.isBlank() ? "repo" : oauthScope.trim();
}
```

Add application properties:

```properties
codeguard.github.oauth-client-id=${CODEGUARD_GITHUB_OAUTH_CLIENT_ID:}
codeguard.github.oauth-client-secret=${CODEGUARD_GITHUB_OAUTH_CLIENT_SECRET:}
codeguard.github.oauth-scope=${CODEGUARD_GITHUB_OAUTH_SCOPE:repo}
codeguard.github.oauth-authorize-url=${CODEGUARD_GITHUB_OAUTH_AUTHORIZE_URL:https://github.com/login/oauth/authorize}
codeguard.github.oauth-token-url=${CODEGUARD_GITHUB_OAUTH_TOKEN_URL:https://github.com/login/oauth/access_token}
codeguard.github.oauth-callback-url=${CODEGUARD_GITHUB_OAUTH_CALLBACK_URL:http://localhost:8080/api/github/setup}
```

Create `GitHubOAuthToken`:

```java
package com.codeguard.backend.github;

public record GitHubOAuthToken(String accessToken, String tokenType, String scope) {
}
```

Create `GitHubAuthenticatedUser`:

```java
package com.codeguard.backend.github;

public record GitHubAuthenticatedUser(Long id, String login, String type) {
}
```

- [ ] **Step 4: Convert connection entity and response to OAuth metadata**

Update `GitHubConnectionEntity` fields and constructor:

```java
@Column(nullable = false, length = 512)
private String accessToken;

@Column(nullable = false)
private String tokenType;

@Column(nullable = false)
private String scope;

@Column
private Long githubUserId;

@Column(nullable = false)
private String accountLogin;

@Column(nullable = false)
private String accountType;
```

Constructor:

```java
public GitHubConnectionEntity(
    UserEntity user,
    String accessToken,
    String tokenType,
    String scope,
    Long githubUserId,
    String accountLogin,
    String accountType
) {
  this.user = user;
  updateOAuthConnection(accessToken, tokenType, scope, githubUserId, accountLogin, accountType);
}
```

Updater and getters:

```java
public void updateOAuthConnection(
    String accessToken,
    String tokenType,
    String scope,
    Long githubUserId,
    String accountLogin,
    String accountType
) {
  this.accessToken = accessToken;
  this.tokenType = tokenType == null || tokenType.isBlank() ? "bearer" : tokenType;
  this.scope = scope == null || scope.isBlank() ? "repo" : scope;
  this.githubUserId = githubUserId;
  this.accountLogin = accountLogin;
  this.accountType = accountType == null || accountType.isBlank() ? "User" : accountType;
}
```

Ensure `GitHubConnectionResponse.connected(...)` no longer needs an installation id:

```java
public static GitHubConnectionResponse connected(String accountLogin, String accountType) {
  return new GitHubConnectionResponse(true, accountLogin, accountType);
}
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```bash
cd backend && ./mvnw test -Dtest=GitHubControllerTest#connectionStatusReturnsOAuthAccountWhenConnected
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/test/java/com/codeguard/backend/github/GitHubControllerTest.java backend/src/main/java/com/codeguard/backend/shared/config/CodeGuardGitHubProperties.java backend/src/main/resources/application.properties backend/src/main/java/com/codeguard/backend/github/entity/GitHubConnectionEntity.java backend/src/main/java/com/codeguard/backend/github/dto/GitHubConnectionResponse.java backend/src/main/java/com/codeguard/backend/github/GitHubOAuthToken.java backend/src/main/java/com/codeguard/backend/github/GitHubAuthenticatedUser.java
git commit -m "feat: model GitHub OAuth connections"
```

## Task 2: OAuth Connect URL and Callback

**Files:**
- Modify: `backend/src/test/java/com/codeguard/backend/github/GitHubControllerTest.java`
- Modify: `backend/src/main/java/com/codeguard/backend/github/GitHubClient.java`
- Modify: `backend/src/main/java/com/codeguard/backend/github/HttpGitHubClient.java`
- Modify: `backend/src/main/java/com/codeguard/backend/github/GitHubConnectionService.java`
- Modify: `backend/src/main/java/com/codeguard/backend/github/GitHubController.java`
- Modify: `backend/src/main/java/com/codeguard/backend/shared/error/GlobalExceptionHandler.java` if missing-code validation needs a mapped exception.

- [ ] **Step 1: Write failing connect URL and callback tests**

Replace the connect URL assertion with:

```java
.andExpect(jsonPath("$.connectUrl")
    .value(org.hamcrest.Matchers.startsWith("https://github.com/login/oauth/authorize?")))
.andExpect(jsonPath("$.connectUrl")
    .value(org.hamcrest.Matchers.containsString("client_id=test-client-id")))
.andExpect(jsonPath("$.connectUrl")
    .value(org.hamcrest.Matchers.containsString("scope=repo")))
.andExpect(jsonPath("$.connectUrl")
    .value(org.hamcrest.Matchers.containsString("redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fgithub%2Fsetup")))
.andExpect(jsonPath("$.state").isString());
```

Replace setup success test with:

```java
@Test
void setupExchangesOAuthCodeAndBindsConnectionToCurrentUserWhenStateIsValid() throws Exception {
  String token = register("amir@example.com");
  Long userId = userRepository.findActiveByEmail("amir@example.com").orElseThrow().getId();
  String state = objectMapper.readTree(mockMvc.perform(get("/api/github/connect-url")
          .header("Authorization", "Bearer " + token))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString()).get("state").asText();

  when(gitHubClient.exchangeOAuthCode("abc123"))
      .thenReturn(new GitHubOAuthToken("oauth-token", "bearer", "repo"));
  when(gitHubClient.fetchAuthenticatedUser("oauth-token"))
      .thenReturn(new GitHubAuthenticatedUser(123456L, "amircox", "User"));

  mockMvc.perform(get("/api/github/setup")
          .header("Authorization", "Bearer " + token)
          .param("code", "abc123")
          .param("state", state))
      .andExpect(status().is3xxRedirection())
      .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
          .isEqualTo("http://localhost:3000/?github=connected"));

  assertThat(gitHubConnectionRepository.findAll())
      .singleElement()
      .satisfies(connection -> {
        assertThat(connection.getUser().getId()).isEqualTo(userId);
        assertThat(connection.getAccessToken()).isEqualTo("oauth-token");
        assertThat(connection.getScope()).isEqualTo("repo");
        assertThat(connection.getGithubUserId()).isEqualTo(123456L);
        assertThat(connection.getAccountLogin()).isEqualTo("amircox");
        assertThat(connection.getAccountType()).isEqualTo("User");
      });
  assertThat(gitHubPendingConnectionRepository.findAll()).isEmpty();
}
```

Add missing code test:

```java
@Test
void setupReturnsBadRequestWhenOAuthCodeIsMissing() throws Exception {
  String token = register("amir@example.com");
  String state = objectMapper.readTree(mockMvc.perform(get("/api/github/connect-url")
          .header("Authorization", "Bearer " + token))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString()).get("state").asText();

  mockMvc.perform(get("/api/github/setup")
          .header("Authorization", "Bearer " + token)
          .param("state", state))
      .andExpect(status().isBadRequest());
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
cd backend && ./mvnw test -Dtest=GitHubControllerTest#connectUrlCreatesPendingStateForCurrentUser,GitHubControllerTest#setupExchangesOAuthCodeAndBindsConnectionToCurrentUserWhenStateIsValid,GitHubControllerTest#setupReturnsBadRequestWhenOAuthCodeIsMissing
```

Expected: failures because connect URL still uses GitHub App installation and setup still expects `installation_id`.

- [ ] **Step 3: Extend GitHubClient and HttpGitHubClient for OAuth**

Add to `GitHubClient`:

```java
GitHubOAuthToken exchangeOAuthCode(String code);

GitHubAuthenticatedUser fetchAuthenticatedUser(String accessToken);
```

In `HttpGitHubClient.exchangeOAuthCode`, POST form data to `properties.oauthTokenUrl()`:

```java
String form = "client_id=" + encode(properties.oauthClientId())
    + "&client_secret=" + encode(properties.oauthClientSecret())
    + "&code=" + encode(code)
    + "&redirect_uri=" + encode(properties.oauthCallbackUrl());
```

Use headers:

```java
.header("Accept", "application/json")
.header("Content-Type", "application/x-www-form-urlencoded")
```

Parse `access_token`, `token_type`, and `scope`. If `access_token` is blank, throw `GitHubFetchException("GitHub OAuth token response was not valid")`.

In `fetchAuthenticatedUser`, call `sendGet("/user", accessToken)` and return:

```java
return new GitHubAuthenticatedUser(
    root.path("id").isNumber() ? root.path("id").asLong() : null,
    root.path("login").asText(""),
    root.path("type").asText("User")
);
```

- [ ] **Step 4: Convert connection service and controller to OAuth callback**

In `GitHubConnectionService.createConnectUrl`, validate OAuth credentials and build:

```java
String connectUrl = properties.oauthAuthorizeUrl()
    + "?client_id=" + URLEncoder.encode(properties.oauthClientId(), StandardCharsets.UTF_8)
    + "&redirect_uri=" + URLEncoder.encode(properties.oauthCallbackUrl(), StandardCharsets.UTF_8)
    + "&scope=" + URLEncoder.encode(properties.resolvedOAuthScope(), StandardCharsets.UTF_8)
    + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
```

Replace `completeSetup(Long installationId, String state)` with:

```java
@Transactional
public String completeSetup(String code, String state) {
  if (code == null || code.isBlank()) {
    throw new GitHubSetupStateException("GitHub OAuth code is required.");
  }
  UserEntity user = currentUserService.getCurrentUser();
  GitHubPendingConnectionEntity pending = validatePendingState(user, state);
  GitHubOAuthToken token = gitHubClient.exchangeOAuthCode(code.trim());
  GitHubAuthenticatedUser gitHubUser = gitHubClient.fetchAuthenticatedUser(token.accessToken());
  GitHubConnectionEntity connection = gitHubConnectionRepository.findByUserId(user.getId())
      .orElseGet(() -> new GitHubConnectionEntity(
          user,
          token.accessToken(),
          token.tokenType(),
          token.scope(),
          gitHubUser.id(),
          gitHubUser.login(),
          gitHubUser.type()
      ));
  connection.updateOAuthConnection(
      token.accessToken(),
      token.tokenType(),
      token.scope(),
      gitHubUser.id(),
      gitHubUser.login(),
      gitHubUser.type()
  );
  gitHubConnectionRepository.save(connection);
  gitHubPendingConnectionRepository.delete(pending);
  return properties.frontendConnectedRedirectUrl();
}
```

Update controller:

```java
@GetMapping("/setup")
public ResponseEntity<Void> completeSetup(
    @RequestParam String code,
    @RequestParam String state
) {
  return ResponseEntity
      .status(302)
      .header("Location", gitHubConnectionService.completeSetup(code, state))
      .build();
}
```

If `GitHubSetupStateException` has only a fixed message, add a constructor that accepts a message and keep the default invalid-state message unchanged.

- [ ] **Step 5: Run tests to verify they pass**

Run:

```bash
cd backend && ./mvnw test -Dtest=GitHubControllerTest#connectUrlCreatesPendingStateForCurrentUser,GitHubControllerTest#setupExchangesOAuthCodeAndBindsConnectionToCurrentUserWhenStateIsValid,GitHubControllerTest#setupReturnsBadRequestWhenOAuthCodeIsMissing,GitHubControllerTest#setupReturnsBadRequestForInvalidState
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/test/java/com/codeguard/backend/github/GitHubControllerTest.java backend/src/main/java/com/codeguard/backend/github/GitHubClient.java backend/src/main/java/com/codeguard/backend/github/HttpGitHubClient.java backend/src/main/java/com/codeguard/backend/github/GitHubConnectionService.java backend/src/main/java/com/codeguard/backend/github/GitHubController.java backend/src/main/java/com/codeguard/backend/github/GitHubSetupStateException.java backend/src/main/java/com/codeguard/backend/shared/error/GlobalExceptionHandler.java
git commit -m "feat: complete GitHub OAuth callback"
```

## Task 3: OAuth-Backed Repository, Pull Request, and Selected Review Reads

**Files:**
- Modify: `backend/src/test/java/com/codeguard/backend/github/GitHubControllerTest.java`
- Modify: `backend/src/main/java/com/codeguard/backend/github/GitHubClient.java`
- Modify: `backend/src/main/java/com/codeguard/backend/github/HttpGitHubClient.java`
- Modify: `backend/src/main/java/com/codeguard/backend/github/GitHubConnectionService.java`
- Modify: `backend/src/main/java/com/codeguard/backend/github/GitHubPullRequestReviewService.java`

- [ ] **Step 1: Write failing connected-read tests**

Update `connectUser` helper:

```java
private void connectUser(String email, String accessToken) {
  gitHubConnectionRepository.save(new GitHubConnectionEntity(
      userRepository.findActiveByEmail(email).orElseThrow(),
      accessToken,
      "bearer",
      "repo",
      123456L,
      "amircox",
      "User"
  ));
}
```

Update repository test to expect OAuth token usage:

```java
connectUser("amir@example.com", "oauth-token");
when(gitHubClient.listAuthenticatedUserRepositories("oauth-token"))
    .thenReturn(List.of(
        new GitHubRepositoryMetadata(101L, "owner", "repo", "owner/repo", false),
        new GitHubRepositoryMetadata(102L, "owner", "private-repo", "owner/private-repo", true)
    ));
```

Update PR listing test:

```java
connectUser("amir@example.com", "oauth-token");
when(gitHubClient.listPullRequests(
    eq("oauth-token"),
    argThat(ref -> "owner".equals(ref.owner()) && "repo".equals(ref.repo()) && ref.number() == 0)
)).thenReturn(List.of(
    new GitHubPullRequestSummary(
        123,
        "Add validation",
        "octocat",
        "https://github.com/owner/repo/pull/123"
    )
));
```

Update selected review test:

```java
connectUser("amir@example.com", "oauth-token");
stubGitHubPullRequest("oauth-token");
```

Verify:

```java
verify(gitHubClient).fetchPullRequest(
    eq("oauth-token"),
    argThat(pr -> "owner".equals(pr.owner()) && "repo".equals(pr.repo()) && pr.number() == 123)
);
verify(gitHubClient).fetchPullRequestFiles(
    eq("oauth-token"),
    argThat(pr -> "owner".equals(pr.owner()) && "repo".equals(pr.repo()) && pr.number() == 123)
);
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
cd backend && ./mvnw test -Dtest=GitHubControllerTest#repositoriesReturnInstallationRepositoriesForConnectedUser,GitHubControllerTest#pullRequestsReturnOpenPullRequestsForSelectedRepository,GitHubControllerTest#selectedPullRequestReviewUsesConnectedInstallationTokenAndSavesReview
```

Expected: compile or assertion failures because service code still asks for installation tokens.

- [ ] **Step 3: Rename tests for OAuth behavior**

Rename test methods:

```java
repositoriesReturnAuthenticatedUserRepositoriesForConnectedUser
pullRequestsReturnOpenPullRequestsForSelectedRepository
selectedPullRequestReviewUsesConnectedOAuthTokenAndSavesReview
```

- [ ] **Step 4: Add OAuth repository listing to client**

Add to `GitHubClient`:

```java
List<GitHubRepositoryMetadata> listAuthenticatedUserRepositories(String accessToken);
```

Implement in `HttpGitHubClient` using:

```java
JsonNode root = sendGet(
    "/user/repos?affiliation=owner,collaborator,organization_member&sort=updated&per_page=100",
    accessToken
);
```

Parse each repository with the same metadata mapping used by installation repository listing.

- [ ] **Step 5: Replace installation token service usage in connection service**

In `GitHubConnectionService`, remove `GitHubAppTokenService` dependency.

Replace `listRepositories` with:

```java
return gitHubClient.listAuthenticatedUserRepositories(connection.getAccessToken());
```

Replace `listPullRequests` token argument with:

```java
connection.getAccessToken()
```

Replace `createInstallationAccessToken` with:

```java
@Transactional(readOnly = true)
public String getConnectedAccessToken() {
  return getRequiredConnection("Connect GitHub before reviewing pull requests.").getAccessToken();
}
```

Update `GitHubPullRequestReviewService.reviewSelectedPullRequest`:

```java
String accessToken = gitHubConnectionService.getConnectedAccessToken();
GitHubPullRequestMetadata metadata = gitHubClient.fetchPullRequest(accessToken, pullRequest);
List<GitHubPullRequestFile> reviewableFiles = gitHubClient.fetchPullRequestFiles(accessToken, pullRequest)
```

- [ ] **Step 6: Run tests to verify they pass**

Run:

```bash
cd backend && ./mvnw test -Dtest=GitHubControllerTest#repositoriesReturnAuthenticatedUserRepositoriesForConnectedUser,GitHubControllerTest#pullRequestsReturnOpenPullRequestsForSelectedRepository,GitHubControllerTest#selectedPullRequestReviewUsesConnectedOAuthTokenAndSavesReview,GitHubControllerTest#selectedPullRequestReviewRequiresGitHubConnection,GitHubControllerTest#reviewPullRequestFetchesDiffAnalyzesAndSavesReview
```

Expected: PASS, including public PR URL regression.

- [ ] **Step 7: Commit**

```bash
git add backend/src/test/java/com/codeguard/backend/github/GitHubControllerTest.java backend/src/main/java/com/codeguard/backend/github/GitHubClient.java backend/src/main/java/com/codeguard/backend/github/HttpGitHubClient.java backend/src/main/java/com/codeguard/backend/github/GitHubConnectionService.java backend/src/main/java/com/codeguard/backend/github/GitHubPullRequestReviewService.java
git commit -m "feat: use OAuth token for connected GitHub reads"
```

## Task 4: Frontend Copy and Types

**Files:**
- Modify: `frontend/src/components/github-pr-review-form.tsx`
- Modify: `frontend/src/types/review.ts`
- Modify: `frontend/tests/e2e/review-flow.spec.ts`

- [ ] **Step 1: Write/update failing frontend assertion**

In `frontend/tests/e2e/review-flow.spec.ts`, if there is no disconnected-state coverage, add or update a lightweight route-backed assertion:

```ts
await page.route("**/api/github/connection", async (route) => {
  await route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify({ connected: false }),
  });
});

await expect(
  page.getByText("Authorize CodeGuard with GitHub to load repositories and pull requests."),
).toBeVisible();
```

- [ ] **Step 2: Run frontend e2e test to verify it fails**

Run:

```bash
cd frontend && npm run test:e2e -- review-flow.spec.ts
```

Expected: failure if old copy is still rendered, or no script if the repo only supports build-time validation. If no e2e script exists, run `npm run build` after the copy change in Step 4.

- [ ] **Step 3: Update frontend type**

In `frontend/src/types/review.ts`, change:

```ts
export type GitHubConnectionResponse = {
  connected: boolean;
  accountLogin?: string | null;
  accountType?: string | null;
};
```

- [ ] **Step 4: Update copy**

In `GitHubPrReviewForm`, replace disconnected copy with:

```tsx
Authorize CodeGuard with GitHub to load repositories and pull requests.
```

Replace empty repository copy with:

```tsx
No repositories found. Confirm the connected GitHub account has repository access.
```

- [ ] **Step 5: Run frontend validation**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/github-pr-review-form.tsx frontend/src/types/review.ts frontend/tests/e2e/review-flow.spec.ts
git commit -m "feat: update GitHub OAuth connection UI"
```

## Task 5: Documentation and Full Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/deployment-render-vercel.md`

- [ ] **Step 1: Update docs**

Replace GitHub App installation references for connected repository selection with GitHub OAuth App setup:

```text
Create a GitHub OAuth App with callback URL:
Local: http://localhost:8080/api/github/setup
Production: https://<backend-host>/api/github/setup

Set:
CODEGUARD_GITHUB_OAUTH_CLIENT_ID=<client-id>
CODEGUARD_GITHUB_OAUTH_CLIENT_SECRET=<client-secret>
CODEGUARD_GITHUB_OAUTH_SCOPE=repo
CODEGUARD_GITHUB_OAUTH_CALLBACK_URL=https://<backend-host>/api/github/setup
CODEGUARD_GITHUB_FRONTEND_CONNECTED_REDIRECT_URL=https://<frontend-host>/?github=connected
```

Keep notes that `CODEGUARD_GITHUB_TOKEN` is only for optional server-side PR comments and fallback public PR API rate limits.

- [ ] **Step 2: Run backend tests**

Run:

```bash
cd backend && ./mvnw test
```

Expected: PASS.

- [ ] **Step 3: Run frontend build**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 4: Run compose config validation**

Run:

```bash
docker compose config --quiet
```

Expected: no output and exit code 0.

- [ ] **Step 5: Check git status**

Run:

```bash
git status --short
```

Expected: only intentional docs/code changes are present.

- [ ] **Step 6: Commit**

```bash
git add README.md docs/deployment-render-vercel.md
git commit -m "docs: document GitHub OAuth setup"
```

## Self-Review Notes

- Spec coverage: OAuth config, state, callback exchange, token storage, connected reads, frontend copy, docs, public PR URL regression, and validation are covered.
- Placeholder scan: no `TBD`, `TODO`, or unspecified implementation steps should remain.
- Type consistency: plan uses `GitHubOAuthToken`, `GitHubAuthenticatedUser`, `getConnectedAccessToken`, and `listAuthenticatedUserRepositories` consistently.
