import { AlertCircle, CheckCircle2, Code2 } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import type { IssueSeverity, ReviewResponse, RiskLevel } from "@/types/review";

type ReviewResultsProps = {
  data?: ReviewResponse;
  error?: Error | null;
  isPending: boolean;
};

const severityVariant: Record<IssueSeverity | RiskLevel, "low" | "medium" | "high"> = {
  LOW: "low",
  MEDIUM: "medium",
  HIGH: "high",
};

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
              Results will appear here with summary, risk, issues, and improved
              code after you run the first review.
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="border-white/10 bg-slate-950/76 backdrop-blur">
      <CardHeader>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <CardTitle>Review results</CardTitle>
            <CardDescription>{data.summary}</CardDescription>
          </div>
          <Badge variant={severityVariant[data.riskLevel]}>
            {data.riskLevel} RISK
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="flex flex-wrap gap-2 text-sm text-muted-foreground">
          <Badge variant="outline">Language: {data.language}</Badge>
          <Badge variant="outline">{data.issues.length} issues</Badge>
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
              <p className="mt-2 text-sm leading-6 text-slate-300">
                {issue.description}
              </p>
              <div className="mt-3 flex gap-2 rounded-md border border-emerald-300/15 bg-emerald-300/10 p-3 text-sm text-emerald-100">
                <CheckCircle2 className="mt-0.5 h-4 w-4 flex-none" />
                <span>{issue.suggestion}</span>
              </div>
            </div>
          ))}
        </div>

        <div className="space-y-3">
          <h3 className="text-sm font-semibold text-slate-200">Improved code</h3>
          <pre className="overflow-x-auto rounded-lg border border-white/10 bg-black/45 p-4 font-mono text-[13px] leading-6 text-slate-100">
            <code>{data.improvedCode}</code>
          </pre>
        </div>
      </CardContent>
    </Card>
  );
}
