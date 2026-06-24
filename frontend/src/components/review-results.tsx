import {
  AlertCircle,
  CheckCircle2,
  Code2,
  ExternalLink,
  FlaskConical,
  MessageSquare,
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import type { ReviewResponse, Severity } from "@/types/review";

type ReviewResultsProps = {
  data?: ReviewResponse;
  error?: Error | null;
  isPending: boolean;
};

const severityVariant: Record<Severity, "low" | "medium" | "high"> = {
  LOW: "low",
  MEDIUM: "medium",
  HIGH: "high",
  CRITICAL: "high",
};

function riskDisplay(score: number): {
  label: string;
  variant: "low" | "medium" | "high";
} {
  if (score >= 90) {
    return { label: "Critical Risk", variant: "high" };
  }

  if (score >= 70) {
    return { label: "High Risk", variant: "high" };
  }

  if (score >= 40) {
    return { label: "Medium Risk", variant: "medium" };
  }

  return { label: "Low Risk", variant: "low" };
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("en", {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(value));
}

function repositoryLabel(data: ReviewResponse): string | undefined {
  if (!data.githubOwner || !data.githubRepo) {
    return undefined;
  }

  return `${data.githubOwner}/${data.githubRepo}`;
}

export function ReviewResults({ data, error, isPending }: ReviewResultsProps) {
  if (isPending) {
    return (
      <Card className="min-h-[645px]">
        <CardContent className="flex min-h-[645px] flex-col items-center justify-center gap-4 text-center">
          <div className="h-12 w-12 animate-pulse rounded border border-[#25251e] bg-[#bf3b2d]/15 shadow-[4px_4px_0_rgba(23,23,17,0.12)]" />
          <div>
            <h2 className="font-display text-xl font-semibold text-[#171711]">Review in progress</h2>
            <p className="mt-1 max-w-sm text-sm leading-6 text-muted-foreground">
              The frontend is calling the Spring Boot API and waiting for the
              structured review payload.
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card className="border-[#bf3b2d] bg-[#f1d7d1]">
        <CardContent className="flex min-h-[260px] flex-col justify-center gap-3">
          <AlertCircle className="h-6 w-6 text-[#8d281f]" />
          <h2 className="font-display text-xl font-semibold text-[#8d281f]">Review request failed</h2>
          <p className="text-sm font-medium text-[#8d281f]">{error.message}</p>
        </CardContent>
      </Card>
    );
  }

  if (!data) {
    return (
      <Card className="min-h-[645px]">
        <CardContent className="flex min-h-[645px] flex-col items-center justify-center gap-4 text-center">
          <Code2 className="h-10 w-10 text-[#315f72]" />
          <div>
            <h2 className="font-display text-xl font-semibold text-[#171711]">Awaiting analysis</h2>
            <p className="mt-1 max-w-sm text-sm leading-6 text-muted-foreground">
              Select a saved review or submit code to see the summary, risk
              score, issues, and recommended tests.
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  const risk = riskDisplay(data.riskScore);
  const repo = repositoryLabel(data);

  return (
    <Card data-testid="review-results">
      <CardHeader>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="max-w-2xl">
            <CardTitle>{data.title}</CardTitle>
            <CardDescription>{data.summary}</CardDescription>
          </div>
          <Badge variant={risk.variant}>
            {risk.label}: {data.riskScore}
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="flex flex-wrap gap-2 text-sm text-muted-foreground">
          <Badge variant={data.source === "GITHUB_PR" ? "medium" : "outline"}>
            {data.source === "GITHUB_PR" ? "GitHub PR" : "Manual"}
          </Badge>
          {data.projectName ? (
            <Badge variant="outline">Project: {data.projectName}</Badge>
          ) : null}
          <Badge variant="outline">Language: {data.language}</Badge>
          <Badge variant="outline">Review #{data.id}</Badge>
          <Badge variant="outline">{formatDate(data.createdAt)}</Badge>
          <Badge variant="outline">{data.issues.length} issues</Badge>
          <Badge variant="outline">{data.recommendedTests.length} tests</Badge>
        </div>

        {data.source === "GITHUB_PR" ? (
          <div className="rounded border border-[#315f72] bg-[#dce8eb] p-4">
            <h3 className="text-sm font-extrabold text-[#214756]">
              GitHub pull request
            </h3>
            <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-2">
              {repo ? (
                <div>
                  <dt className="text-[#6a6659]">Repository</dt>
                  <dd className="mt-1 font-bold text-[#171711]">{repo}</dd>
                </div>
              ) : null}
              {data.githubPullRequestNumber ? (
                <div>
                  <dt className="text-[#6a6659]">Pull request number</dt>
                  <dd className="mt-1 font-bold text-[#171711]">
                    #{data.githubPullRequestNumber}
                  </dd>
                </div>
              ) : null}
              {data.githubPullRequestTitle ? (
                <div className="sm:col-span-2">
                  <dt className="text-[#6a6659]">Pull request title</dt>
                  <dd className="mt-1 font-bold text-[#171711]">
                    {data.githubPullRequestTitle}
                  </dd>
                </div>
              ) : null}
              {data.githubPullRequestUrl ? (
                <div className="sm:col-span-2">
                  <dt className="text-[#6a6659]">Pull request URL</dt>
                  <dd className="mt-1">
                    <a
                      href={data.githubPullRequestUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-2 break-all font-bold text-[#315f72] hover:text-[#214756]"
                    >
                      {data.githubPullRequestUrl}
                      <ExternalLink className="h-3.5 w-3.5 flex-none" />
                    </a>
                  </dd>
                </div>
              ) : null}
              {data.githubCommentPosted && data.githubCommentUrl ? (
                <div className="sm:col-span-2">
                  <dt className="text-[#6a6659]">GitHub comment</dt>
                  <dd className="mt-1">
                    <a
                      href={data.githubCommentUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-2 break-all font-bold text-[#3f5821] hover:text-[#2f4318]"
                    >
                      Comment posted
                      <ExternalLink className="h-3.5 w-3.5 flex-none" />
                    </a>
                  </dd>
                </div>
              ) : data.githubCommentError ? (
                <div className="sm:col-span-2">
                  <dt className="text-[#6a6659]">GitHub comment</dt>
                  <dd className="mt-1 flex gap-2 rounded border border-[#aa7a1d] bg-[#f3e5be] p-3 text-[#735116]">
                    <MessageSquare className="mt-0.5 h-4 w-4 flex-none" />
                    <span>{data.githubCommentError}</span>
                  </dd>
                </div>
              ) : null}
            </dl>
          </div>
        ) : null}

        <Separator />

        <div className="space-y-3">
          {data.issues.map((issue) => (
            <div
              key={`${issue.category}-${issue.title}`}
              className="rounded border border-[#bdb5a1] bg-[#fffdf8] p-4 shadow-[4px_4px_0_rgba(23,23,17,0.06)]"
            >
              <div className="mb-3 flex flex-wrap items-center gap-2">
                <Badge variant="secondary">{issue.category}</Badge>
                <Badge variant={severityVariant[issue.severity]}>
                  {issue.severity}
                </Badge>
              </div>
              <h3 className="font-display text-lg font-semibold text-[#171711]">{issue.title}</h3>
              {issue.lineNumber ? (
                <p className="mt-1 text-xs font-extrabold uppercase tracking-[0.12em] text-[#315f72]">
                  Line {issue.lineNumber}
                </p>
              ) : null}
              <p className="mt-2 text-sm leading-6 text-[#4f4b41]">
                {issue.explanation}
              </p>
              <div className="mt-3 flex gap-2 rounded border border-[#5e7b37] bg-[#e8eedb] p-3 text-sm font-medium text-[#3f5821]">
                <CheckCircle2 className="mt-0.5 h-4 w-4 flex-none" />
                <span>{issue.suggestion}</span>
              </div>
            </div>
          ))}
        </div>

        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <FlaskConical className="h-4 w-4 text-[#315f72]" />
            <h3 className="text-sm font-extrabold uppercase tracking-[0.12em] text-[#25251e]">
              Recommended tests
            </h3>
          </div>
          <div className="space-y-2">
            {data.recommendedTests.map((test) => (
              <div
                key={test}
                className="rounded border border-[#315f72] bg-[#dce8eb] p-3 text-sm font-medium text-[#214756]"
              >
                {test}
              </div>
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
