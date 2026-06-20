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

  await page.getByRole("button", { name: "Sign out" }).click();
  await expect(page.getByRole("heading", { name: "Sign in" })).toBeVisible();

  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();

  await expect(page.getByText("E2E Auth User")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Create a project" })).toBeVisible();
});
