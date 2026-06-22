"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";

import { AuthPanel } from "@/components/auth-panel";
import { CodeReviewForm } from "@/components/code-review-form";
import { GitHubPrReviewForm } from "@/components/github-pr-review-form";
import { ProjectForm } from "@/components/project-form";
import { ReviewHistory } from "@/components/review-history";
import { ReviewResults } from "@/components/review-results";
import {
  createProject,
  createGitHubPullRequestReview,
  createReview,
  getMe,
  getReview,
  getReviews,
  getProjects,
  login,
  register,
} from "@/lib/api";
import type {
  AuthResponse,
  GitHubPullRequestReviewRequest,
  LoginRequest,
  ProjectRequest,
  ProjectResponse,
  RegisterRequest,
  ReviewRequest,
  ReviewResponse,
} from "@/types/review";

const tokenStorageKey = "codeguard.auth.token";

export default function Home() {
  const queryClient = useQueryClient();
  const [token, setToken] = useState<string>();
  const [selectedReviewId, setSelectedReviewId] = useState<number>();
  const [submittedReview, setSubmittedReview] = useState<ReviewResponse>();
  const [sessionMessage, setSessionMessage] = useState<string>();

  useEffect(() => {
    const savedToken = window.localStorage.getItem(tokenStorageKey);
    if (savedToken) {
      setToken(savedToken);
    }
  }, []);

  const handleAuthSuccess = (response: AuthResponse) => {
    window.localStorage.setItem(tokenStorageKey, response.token);
    setSessionMessage(undefined);
    setToken(response.token);
    queryClient.setQueryData(["me", response.token], response.user);
  };

  const handleLogout = (message?: string) => {
    window.localStorage.removeItem(tokenStorageKey);
    setSessionMessage(message);
    setToken(undefined);
    setSelectedReviewId(undefined);
    setSubmittedReview(undefined);
    queryClient.clear();
  };

  const meQuery = useQuery({
    queryKey: ["me", token],
    queryFn: () => getMe(token as string),
    enabled: token !== undefined,
    retry: false,
  });

  const reviewsQuery = useQuery<ReviewResponse[], Error>({
    queryKey: ["reviews", token],
    queryFn: () => getReviews(token as string),
    enabled: token !== undefined,
  });

  const projectsQuery = useQuery<ProjectResponse[], Error>({
    queryKey: ["projects", token],
    queryFn: () => getProjects(token as string),
    enabled: token !== undefined,
  });

  const selectedReviewQuery = useQuery<ReviewResponse, Error>({
    queryKey: ["reviews", token, selectedReviewId],
    queryFn: () => getReview(selectedReviewId as number, token as string),
    enabled: token !== undefined && selectedReviewId !== undefined,
  });

  const reviewMutation = useMutation<ReviewResponse, Error, ReviewRequest>({
    mutationFn: (payload) => createReview(payload, token as string),
    onMutate: () => {
      setSubmittedReview(undefined);
    },
    onSuccess: (review) => {
      setSubmittedReview(review);
      setSelectedReviewId(undefined);
      queryClient.invalidateQueries({ queryKey: ["reviews", token] });
      queryClient.setQueryData(["reviews", token, review.id], review);
    },
  });

  const githubReviewMutation = useMutation<ReviewResponse, Error, GitHubPullRequestReviewRequest>({
    mutationFn: (payload) => createGitHubPullRequestReview(payload, token as string),
    onMutate: () => {
      setSubmittedReview(undefined);
    },
    onSuccess: (review) => {
      setSubmittedReview(review);
      setSelectedReviewId(undefined);
      queryClient.invalidateQueries({ queryKey: ["reviews", token] });
      queryClient.setQueryData(["reviews", token, review.id], review);
    },
  });

  const projectMutation = useMutation<ProjectResponse, Error, ProjectRequest>({
    mutationFn: (payload) => createProject(payload, token as string),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["projects", token] });
    },
  });

  const loginMutation = useMutation<AuthResponse, Error, LoginRequest>({
    mutationFn: login,
    onSuccess: handleAuthSuccess,
  });

  const registerMutation = useMutation<AuthResponse, Error, RegisterRequest>({
    mutationFn: register,
    onSuccess: handleAuthSuccess,
  });

  const activeReview = selectedReviewQuery.data ?? submittedReview;
  const activeError =
    submittedReview && selectedReviewId === undefined
      ? null
      : githubReviewMutation.error ??
        reviewMutation.error ??
        (selectedReviewId === undefined ? null : selectedReviewQuery.error);
  const isReviewLoading =
    reviewMutation.isPending || githubReviewMutation.isPending || selectedReviewQuery.isFetching;
  const authError =
    loginMutation.error ??
    registerMutation.error ??
    (sessionMessage ? new Error(sessionMessage) : null);

  useEffect(() => {
    if (meQuery.error) {
      handleLogout(meQuery.error.message);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [meQuery.error]);

  return (
    <main className="code-grid min-h-screen px-4 py-8 sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-7xl flex-col gap-8">
        <header className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
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
          </div>

          {token && meQuery.data ? (
            <div className="rounded-md border border-white/10 bg-slate-950/70 px-4 py-3 text-sm text-slate-200">
              <div className="font-semibold text-white">{meQuery.data.name}</div>
              <div className="text-slate-400">{meQuery.data.email}</div>
              <button
                className="mt-2 text-sm font-medium text-emerald-200 hover:text-emerald-100"
                type="button"
                onClick={() => handleLogout()}
              >
                Sign out
              </button>
            </div>
          ) : null}
        </header>

        {token && meQuery.isLoading ? (
          <section className="rounded-md border border-white/10 bg-slate-950/76 p-6 text-sm text-slate-300">
            Loading your workspace...
          </section>
        ) : !token ? (
          <AuthPanel
            isPending={loginMutation.isPending || registerMutation.isPending}
            error={authError}
            onLogin={(payload) => loginMutation.mutate(payload)}
            onRegister={(payload) => registerMutation.mutate(payload)}
          />
        ) : (
          <>
            <section className="grid gap-6 xl:grid-cols-[0.84fr_1.16fr]">
              <div className="space-y-6">
                <ProjectForm
                  isPending={projectMutation.isPending}
                  error={projectMutation.error}
                  onSubmit={(payload) => projectMutation.mutate(payload)}
                />
                <CodeReviewForm
                  isPending={reviewMutation.isPending}
                  projects={projectsQuery.data}
                  areProjectsLoading={projectsQuery.isLoading}
                  projectsError={projectsQuery.error}
                  onSubmit={(payload) => reviewMutation.mutate(payload)}
                />
                <GitHubPrReviewForm
                  isPending={githubReviewMutation.isPending}
                  projects={projectsQuery.data}
                  areProjectsLoading={projectsQuery.isLoading}
                  projectsError={projectsQuery.error}
                  onSubmit={(payload) => githubReviewMutation.mutate(payload)}
                />
              </div>
              <ReviewResults
                data={activeReview}
                error={activeError}
                isPending={isReviewLoading}
              />
            </section>

            <ReviewHistory
              data={reviewsQuery.data}
              error={reviewsQuery.error}
              isLoading={reviewsQuery.isLoading}
              selectedReviewId={selectedReviewId}
              onSelectReview={(id) => {
                setSelectedReviewId(id);
                setSubmittedReview(undefined);
              }}
            />
          </>
        )}
      </div>
    </main>
  );
}
