export type Severity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type IssueCategory =
  | "SECURITY"
  | "PERFORMANCE"
  | "MAINTAINABILITY"
  | "TESTING"
  | "READABILITY"
  | "BUG_RISK"
  | "ARCHITECTURE";

export type ReviewRequest = {
  projectId?: number;
  title: string;
  language: string;
  code: string;
};

export type GitHubPullRequestReviewRequest = {
  projectId?: number;
  pullRequestUrl: string;
};

export type ReviewIssue = {
  title: string;
  severity: Severity;
  category: IssueCategory;
  explanation: string;
  suggestion: string;
  lineNumber?: number | null;
};

export type ReviewResponse = {
  id: number;
  projectId?: number | null;
  projectName?: string | null;
  title: string;
  language: string;
  summary: string;
  riskScore: number;
  createdAt: string;
  issues: ReviewIssue[];
  recommendedTests: string[];
};

export type ProjectRequest = {
  name: string;
  description: string;
};

export type ProjectResponse = {
  id: number;
  name: string;
  description: string;
  createdAt: string;
  updatedAt: string;
};

export type RegisterRequest = {
  name: string;
  email: string;
  password: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type UserResponse = {
  id: number;
  name: string;
  email: string;
};

export type AuthResponse = {
  token: string;
  user: UserResponse;
};
