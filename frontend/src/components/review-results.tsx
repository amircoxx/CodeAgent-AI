import { AlertCircle, CheckCircle2, Code2, FlaskConical } from "lucide-react";

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

export function ReviewResults({ data, error, isPending }: ReviewResultsProps) {
  if (isPending) {
    return (
      <Card className="min-h-[645px] border-white/10 bg-slate-950/70 backdrop-blur">
        <CardContent className="flex min-h-[645px] flex-col items-center justify-center gap-4 text-center">
          <div className="h-12 w-12 animate-pulse rounded-md border border-cyan-300/25 bg-cyan-300/10" />
          <div>
            <h2 className="text-lg font-semibold">Review in progress</h2>
            <p className="mt-1 max-w-sm text-sm text-muted-foreground">
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
      <Card className="border-red-400/20 bg-red-950/20 backdrop-blur">
        <CardContent className="flex min-h-[260px] flex-col justify-center gap-3">
          <AlertCircle className="h-6 w-6 text-red-200" />
          <h2 className="text-lg font-semibold text-red-100">Review request failed</h2>
          <p className="text-sm text-red-100/75">{error.message}</p>
        </CardContent>
      </Card>
    );
  }

  if (!data) {
    return (
      <Card className="min-h-[645px] border-white/10 bg-slate-950/70 backdrop-blur">
        <CardContent className="flex min-h-[645px] flex-col items-center justify-center gap-4 text-center">
          <Code2 className="h-10 w-10 text-cyan-200" />
          <div>
            <h2 className="text-lg font-semibold">Awaiting analysis</h2>
            <p className="mt-1 max-w-sm text-sm text-muted-foreground">
              Select a saved review or submit code to see the summary, risk
              score, issues, and recommended tests.
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  const risk = riskDisplay(data.riskScore);

  return (
    <Card className="border-white/10 bg-slate-950/76 backdrop-blur">
      <CardHeader>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
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
          {data.projectName ? (
            <Badge variant="outline">Project: {data.projectName}</Badge>
          ) : null}
          <Badge variant="outline">Language: {data.language}</Badge>
          <Badge variant="outline">Review #{data.id}</Badge>
          <Badge variant="outline">{formatDate(data.createdAt)}</Badge>
          <Badge variant="outline">{data.issues.length} issues</Badge>
          <Badge variant="outline">{data.recommendedTests.length} tests</Badge>
        </div>

        <Separator />

        <div className="space-y-3">
          {data.issues.map((issue) => (
            <div
              key={`${issue.category}-${issue.title}`}
              className="rounded-lg border border-white/10 bg-slate-900/68 p-4"
            >
              <div className="mb-3 flex flex-wrap items-center gap-2">
                <Badge variant="secondary">{issue.category}</Badge>
                <Badge variant={severityVariant[issue.severity]}>
                  {issue.severity}
                </Badge>
              </div>
              <h3 className="font-semibold text-slate-100">{issue.title}</h3>
              {issue.lineNumber ? (
                <p className="mt-1 text-xs font-medium text-cyan-200">
                  Line {issue.lineNumber}
                </p>
              ) : null}
              <p className="mt-2 text-sm leading-6 text-slate-300">
                {issue.explanation}
              </p>
              <div className="mt-3 flex gap-2 rounded-md border border-emerald-300/15 bg-emerald-300/10 p-3 text-sm text-emerald-100">
                <CheckCircle2 className="mt-0.5 h-4 w-4 flex-none" />
                <span>{issue.suggestion}</span>
              </div>
            </div>
          ))}
        </div>

        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <FlaskConical className="h-4 w-4 text-cyan-200" />
            <h3 className="text-sm font-semibold text-slate-200">
              Recommended tests
            </h3>
          </div>
          <div className="space-y-2">
            {data.recommendedTests.map((test) => (
              <div
                key={test}
                className="rounded-md border border-cyan-300/15 bg-cyan-300/10 p-3 text-sm text-cyan-50"
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
