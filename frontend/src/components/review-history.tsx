"use client";

import { AlertCircle, Clock3, FileSearch } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import type { ReviewResponse } from "@/types/review";

type ReviewHistoryProps = {
  data?: ReviewResponse[];
  error?: Error | null;
  isLoading: boolean;
  selectedReviewId?: number;
  onSelectReview: (id: number) => void;
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

export function ReviewHistory({
  data,
  error,
  isLoading,
  selectedReviewId,
  onSelectReview,
}: ReviewHistoryProps) {
  return (
    <Card className="border-white/10 bg-slate-950/76 backdrop-blur">
      <CardHeader>
        <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-md border border-cyan-300/25 bg-cyan-300/10 text-cyan-200">
          <FileSearch className="h-5 w-5" />
        </div>
        <CardTitle>Review history</CardTitle>
        <CardDescription>
          Saved reviews from the Spring Boot API, sorted newest first.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="flex items-center gap-3 rounded-md border border-white/10 bg-slate-900/68 p-4 text-sm text-slate-300">
            <Clock3 className="h-4 w-4 animate-pulse text-cyan-200" />
            Loading saved reviews
          </div>
        ) : null}

        {error ? (
          <div className="flex gap-3 rounded-md border border-red-400/20 bg-red-950/20 p-4 text-sm text-red-100">
            <AlertCircle className="mt-0.5 h-4 w-4 flex-none" />
            {error.message}
          </div>
        ) : null}

        {!isLoading && !error && data?.length === 0 ? (
          <div className="rounded-md border border-white/10 bg-slate-900/68 p-4 text-sm text-slate-300">
            No saved reviews yet. Submit a review to create the first saved result.
          </div>
        ) : null}

        <div className="space-y-3">
          {data?.map((review) => {
            const isSelected = review.id === selectedReviewId;
            const risk = riskDisplay(review.riskScore);

            return (
              <button
                key={review.id}
                type="button"
                onClick={() => onSelectReview(review.id)}
                className={[
                  "w-full rounded-md border p-4 text-left transition-colors",
                  isSelected
                    ? "border-emerald-300/45 bg-emerald-300/10"
                    : "border-white/10 bg-slate-900/68 hover:border-cyan-300/35 hover:bg-slate-900",
                ].join(" ")}
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h3 className="font-semibold text-slate-100">
                      {review.title}
                    </h3>
                    <p className="mt-1 text-sm text-slate-400">
                      {review.language}
                    </p>
                    {review.projectName ? (
                      <p className="mt-1 text-sm text-cyan-200">
                        Project: {review.projectName}
                      </p>
                    ) : null}
                  </div>
                  <Badge variant={risk.variant}>
                    {risk.label}: {review.riskScore}
                  </Badge>
                </div>
                <p className="mt-3 line-clamp-2 text-sm leading-6 text-slate-300">
                  {review.summary}
                </p>
                <div className="mt-3 flex flex-wrap gap-2">
                  <Badge variant="outline">Review #{review.id}</Badge>
                  <Badge variant="outline">{formatDate(review.createdAt)}</Badge>
                  {review.projectName ? (
                    <Badge variant="outline">{review.projectName}</Badge>
                  ) : null}
                  <Badge variant="outline">{review.issues.length} issues</Badge>
                </div>
              </button>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
}
