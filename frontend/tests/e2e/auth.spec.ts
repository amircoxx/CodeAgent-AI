import { expect, test } from "@playwright/test";

const password = "password123";

function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${test.info().workerIndex}@example.com`;
}

test("user can register, sign out, and sign in again", async ({ page }) => {
  const email = uniqueEmail("auth");

  await page.goto("/");
  await page.getByRole("button", { name: "Register" }).click();
  await page.getByLabel("Name").fill("E2E Auth User");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Create Account" }).click();

  await expect(page.getByText("E2E Auth User")).toBeVisible();
  await expect(page.getByText(email)).toBeVisible();
  await expect(page.getByRole("heading", { name: "Create a project" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Account security" })).toBeHidden();

  await page.getByRole("button", { name: new RegExp(`E2E Auth User\\s+${email}`) }).click();
  await page.getByRole("menuitem", { name: "Settings" }).click();
  await expect(page.getByRole("dialog", { name: "Account security" })).toBeVisible();
  await page.getByRole("button", { name: "Close settings" }).click();
  await expect(page.getByRole("dialog", { name: "Account security" })).toBeHidden();

  await page.getByRole("button", { name: new RegExp(`E2E Auth User\\s+${email}`) }).click();
  await page.getByRole("menuitem", { name: "Sign out" }).click();
  await expect(page.getByRole("heading", { name: "Sign in" })).toBeVisible();

  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();

  await expect(page.getByText("E2E Auth User")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Create a project" })).toBeVisible();
});

test("stale saved session returns to sign in instead of staying on workspace loading", async ({ page }) => {
  await page.route("**/api/auth/me", async (route) => {
    await new Promise((resolve) => {
      setTimeout(resolve, 250);
    });

    await route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({
        message: "Your session expired. Please log in again.",
        status: 401,
        path: "/api/auth/me",
      }),
    });
  });

  await page.goto("/");
  await page.evaluate(() => {
    window.localStorage.setItem("codeguard.auth.token", "stale-token");
  });
  await page.reload();

  await expect(page.getByText("Loading your workspace...")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Sign in" })).toBeVisible();
  await expect(page.getByText("Your session expired. Please log in again.")).toBeVisible();
  await expect(page.getByText("Loading your workspace...")).toBeHidden();
});
