"use client";

import { useMutation } from "@tanstack/react-query";

import { CodeReviewForm } from "@/components/code-review-form";
import { ReviewResults } from "@/components/review-results";
import { createReview } from "@/lib/api";
import type { ReviewRequest, ReviewResponse } from "@/types/review";

export default function Home() {
  const reviewMutation = useMutation<ReviewResponse, Error, ReviewRequest>({
    mutationFn: createReview,
  });

  return (
    <main className="code-grid min-h-screen px-4 py-8 sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-7xl flex-col gap-8">
        <header className="max-w-3xl">
          <div className="mb-4 inline-flex items-center rounded-md border border-cyan-300/20 bg-cyan-300/10 px-3 py-1 text-xs font-semibold uppercase text-cyan-100">
            Full-stack starter
          </div>
          <h1 className="text-4xl font-bold tracking-normal text-white sm:text-6xl">
            CodeGuard AI
          </h1>
          <p className="mt-4 max-w-2xl text-base leading-7 text-slate-300 sm:text-lg">
            AI-powered code review for bugs, security risks, performance issues,
            and maintainability improvements.
          </p>
        </header>

        <section className="grid gap-6 lg:grid-cols-[0.92fr_1.08fr]">
          <CodeReviewForm
            isPending={reviewMutation.isPending}
            onSubmit={(payload) => reviewMutation.mutate(payload)}
          />
          <ReviewResults
            data={reviewMutation.data}
            error={reviewMutation.error}
            isPending={reviewMutation.isPending}
          />
        </section>
      </div>
    </main>
  );
}
