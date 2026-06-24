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

function pullRequestLabel(review: ReviewResponse): string | undefined {
  if (
    review.source !== "GITHUB_PR" ||
    !review.githubOwner ||
    !review.githubRepo ||
    !review.githubPullRequestNumber
  ) {
    return undefined;
  }

  return `${review.githubOwner}/${review.githubRepo}#${review.githubPullRequestNumber}`;
}

export function ReviewHistory({
  data,
  error,
  isLoading,
  selectedReviewId,
  onSelectReview,
}: ReviewHistoryProps) {
  return (
    <Card>
      <CardHeader>
        <div className="ledger-icon mb-3 flex h-10 w-10 items-center justify-center rounded">
          <FileSearch className="h-5 w-5" />
        </div>
        <CardTitle>Review history</CardTitle>
        <CardDescription>
          Saved reviews from the Spring Boot API, sorted newest first.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="flex items-center gap-3 rounded border border-[#bdb5a1] bg-[#fffdf8] p-4 text-sm font-medium text-[#4f4b41]">
            <Clock3 className="h-4 w-4 animate-pulse text-[#315f72]" />
            Loading saved reviews
          </div>
        ) : null}

        {error ? (
          <div className="flex gap-3 rounded border border-[#bf3b2d] bg-[#f1d7d1] p-4 text-sm font-medium text-[#8d281f]">
            <AlertCircle className="mt-0.5 h-4 w-4 flex-none" />
            {error.message}
          </div>
        ) : null}

        {!isLoading && !error && data?.length === 0 ? (
          <div className="rounded border border-[#bdb5a1] bg-[#fffdf8] p-4 text-sm text-[#4f4b41]">
            No saved reviews yet. Submit a review to create the first saved result.
          </div>
        ) : null}

        <div className="space-y-3" data-testid="review-history-list">
          {data?.map((review) => {
            const isSelected = review.id === selectedReviewId;
            const risk = riskDisplay(review.riskScore);
            const prLabel = pullRequestLabel(review);

            return (
              <button
                key={review.id}
                data-testid="review-history-item"
                type="button"
                onClick={() => onSelectReview(review.id)}
                className={[
                  "w-full rounded border p-4 text-left transition-colors",
                  isSelected
                    ? "border-[#bf3b2d] bg-[#f1d7d1] shadow-[4px_4px_0_rgba(191,59,45,0.16)]"
                    : "border-[#bdb5a1] bg-[#fffdf8] hover:border-[#315f72] hover:bg-[#eef3f1]",
                ].join(" ")}
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="mb-2 flex flex-wrap items-center gap-2">
                      <Badge variant={review.source === "GITHUB_PR" ? "medium" : "outline"}>
                        {review.source === "GITHUB_PR" ? "GitHub PR" : "Manual"}
                      </Badge>
                      {prLabel ? (
                        <Badge variant="outline">PR: {prLabel}</Badge>
                      ) : null}
                    </div>
                    <h3 className="font-display text-lg font-semibold text-[#171711]">{review.title}</h3>
                    <p className="mt-1 text-sm text-[#6a6659]">
                      {review.language}
                    </p>
                    {review.source === "GITHUB_PR" && review.githubPullRequestTitle ? (
                      <p className="mt-1 text-sm text-[#4f4b41]">
                        Title: {review.githubPullRequestTitle}
                      </p>
                    ) : null}
                    {review.projectName ? (
                      <p className="mt-1 text-sm font-bold text-[#315f72]">
                        Project: {review.projectName}
                      </p>
                    ) : null}
                  </div>
                  <Badge variant={risk.variant}>
                    {risk.label}: {review.riskScore}
                  </Badge>
                </div>
                <p className="mt-3 line-clamp-2 text-sm leading-6 text-[#4f4b41]">
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
