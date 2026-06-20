import { expect, type Page, test } from "@playwright/test";

const password = "password123";

function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${test.info().workerIndex}@example.com`;
}

async function register(page: Page, prefix: string) {
  const email = uniqueEmail(prefix);
  await page.goto("/");
  await page.getByRole("button", { name: "Register" }).click();
  await page.getByLabel("Name").fill(`E2E ${prefix} User`);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Create Account" }).click();
  await expect(page.getByText(email)).toBeVisible();
  return email;
}

test("user can create a project and submit a manual code review", async ({ page }) => {
  await register(page, "manual-review");

  const projectName = `CodeGuard E2E ${Date.now()}`;
  const reviewTitle = `Manual Review ${Date.now()}`;

  await page.getByLabel("Project name").fill(projectName);
  await page.getByLabel("Description").fill("Project created by Playwright E2E.");
  await page.getByRole("button", { name: "Create Project" }).click();

  await page.locator("#project").click();
  await page.getByRole("option", { name: projectName }).click();
  await page.getByLabel("Review title").fill(reviewTitle);
  await page.getByPlaceholder("Paste code to review...").fill("function login(input) {\n  return input.password;\n}");
  await page.getByRole("button", { name: "Analyze Code" }).click();

  const results = page.getByTestId("review-results");
  await expect(results.getByRole("heading", { name: reviewTitle })).toBeVisible();
  await expect(results.getByText(/Risk:/)).toBeVisible();
  await expect(results.getByText("Recommended tests")).toBeVisible();
  await expect(results.getByText(/issues/)).toBeVisible();

  const history = page.getByTestId("review-history-list");
  await expect(history.getByTestId("review-history-item").filter({ hasText: reviewTitle })).toBeVisible();
});

test("user can submit a GitHub PR review without posting a comment", async ({ page }) => {
  const githubReview = {
    id: 9001,
    projectId: null,
    projectName: null,
    title: "GitHub PR Review: octocat/Hello-World#1",
    language: "Multiple",
    summary: "Mocked GitHub PR review for browser E2E coverage.",
    riskScore: 42,
    source: "GITHUB_PR",
    githubOwner: "octocat",
    githubRepo: "Hello-World",
    githubPullRequestNumber: 1,
    githubPullRequestUrl: "https://github.com/octocat/Hello-World/pull/1",
    githubPullRequestTitle: "Improve greeting",
    githubCommentPosted: false,
    githubCommentUrl: null,
    githubCommentError: null,
    createdAt: new Date().toISOString(),
    issues: [
      {
        title: "Missing regression coverage",
        severity: "MEDIUM",
        category: "TESTING",
        explanation: "The PR changes behavior without an accompanying regression test.",
        suggestion: "Add a test for the updated greeting behavior.",
        lineNumber: null,
      },
    ],
    recommendedTests: ["Test the changed greeting behavior"],
  };
  let githubReviewCreated = false;

  await page.route("**/api/github/pull-request-review", async (route) => {
    const requestBody = route.request().postDataJSON() as {
      pullRequestUrl?: string;
      postComment?: boolean;
    };

    expect(requestBody.pullRequestUrl).toBe("https://github.com/octocat/Hello-World/pull/1");
    expect(requestBody.postComment).toBe(false);
    githubReviewCreated = true;

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(githubReview),
    });
  });
  await page.route("**/api/reviews/9001", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(githubReview),
    });
  });
  await page.route("**/api/reviews", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(githubReviewCreated ? [githubReview] : []),
    });
  });

  await register(page, "github-review");

  await page.getByLabel("Pull request URL").fill("https://github.com/octocat/Hello-World/pull/1");
  await expect(page.getByLabel("Post review summary comment to GitHub")).not.toBeChecked();
  await page.getByRole("button", { name: "Review Pull Request" }).click();

  const results = page.getByTestId("review-results");
  await expect(results.getByText("GitHub PR", { exact: true })).toBeVisible();
  await expect(results.getByText("octocat/Hello-World", { exact: true })).toBeVisible();
  await expect(results.getByText("#1", { exact: true })).toBeVisible();
  await expect(results.getByText("Improve greeting", { exact: true })).toBeVisible();

  const history = page.getByTestId("review-history-list");
  await expect(
    history.getByTestId("review-history-item").filter({ hasText: "GitHub PR Review: octocat/Hello-World#1" }),
  ).toBeVisible();
});
