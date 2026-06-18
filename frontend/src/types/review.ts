export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type IssueCategory = "BUG" | "SECURITY" | "PERFORMANCE" | "STYLE";
export type IssueSeverity = "LOW" | "MEDIUM" | "HIGH";

export type ReviewRequest = {
  language: string;
  code: string;
};

export type ReviewIssue = {
  category: IssueCategory;
  severity: IssueSeverity;
  title: string;
  description: string;
  suggestion: string;
};

export type ReviewResponse = {
  summary: string;
  riskLevel: RiskLevel;
  language: string;
  issues: ReviewIssue[];
  improvedCode: string;
};
