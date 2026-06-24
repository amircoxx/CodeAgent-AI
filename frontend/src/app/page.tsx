"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronDown, Settings, X } from "lucide-react";
import { useEffect, useState } from "react";

import { AccountSettings } from "@/components/account-settings";
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
  const [isAccountMenuOpen, setIsAccountMenuOpen] = useState(false);
  const [isAccountSettingsOpen, setIsAccountSettingsOpen] = useState(false);

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
    setIsAccountMenuOpen(false);
    setIsAccountSettingsOpen(false);
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

  useEffect(() => {
    if (!isAccountSettingsOpen) {
      return;
    }

    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setIsAccountSettingsOpen(false);
      }
    };

    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [isAccountSettingsOpen]);

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
            <div className="relative">
              <button
                className="flex w-full min-w-64 items-center justify-between gap-4 rounded-md border border-white/10 bg-slate-950/70 px-4 py-3 text-left text-sm text-slate-200 transition-colors hover:border-cyan-300/25 hover:bg-slate-900/85 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-200 lg:w-auto"
                type="button"
                aria-expanded={isAccountMenuOpen}
                aria-haspopup="menu"
                onClick={() => setIsAccountMenuOpen((isOpen) => !isOpen)}
              >
                <span className="min-w-0">
                  <span className="block truncate font-semibold text-white">{meQuery.data.name}</span>
                  <span className="block truncate text-slate-400">{meQuery.data.email}</span>
                </span>
                <ChevronDown
                  className={`h-4 w-4 shrink-0 text-slate-400 transition-transform ${
                    isAccountMenuOpen ? "rotate-180" : ""
                  }`}
                />
              </button>

              {isAccountMenuOpen ? (
                <div
                  className="absolute right-0 z-20 mt-2 w-full min-w-64 rounded-md border border-white/10 bg-slate-950 p-1 text-sm shadow-2xl shadow-black/40 lg:w-64"
                  role="menu"
                >
                  <button
                    className="flex w-full items-center gap-2 rounded px-3 py-2 text-left font-medium text-slate-200 hover:bg-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-200"
                    type="button"
                    role="menuitem"
                    onClick={() => {
                      setIsAccountMenuOpen(false);
                      setIsAccountSettingsOpen(true);
                    }}
                  >
                    <Settings className="h-4 w-4 text-cyan-200" />
                    Settings
                  </button>
                  <button
                    className="flex w-full items-center gap-2 rounded px-3 py-2 text-left font-medium text-emerald-200 hover:bg-slate-900 hover:text-emerald-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-200"
                    type="button"
                    role="menuitem"
                    onClick={() => handleLogout()}
                  >
                    Sign out
                  </button>
                </div>
              ) : null}
            </div>
          ) : null}
        </header>

        {token && isAccountSettingsOpen ? (
          <div
            className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-slate-950/82 px-4 py-6 backdrop-blur-sm sm:py-10"
            role="dialog"
            aria-modal="true"
            aria-labelledby="account-settings-title"
            onMouseDown={(event) => {
              if (event.target === event.currentTarget) {
                setIsAccountSettingsOpen(false);
              }
            }}
          >
            <div className="w-full max-w-2xl">
              <div className="mb-3 flex justify-end">
                <button
                  className="flex h-10 w-10 items-center justify-center rounded-md border border-white/10 bg-slate-950/90 text-slate-200 transition-colors hover:border-cyan-300/25 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-200"
                  type="button"
                  aria-label="Close settings"
                  onClick={() => setIsAccountSettingsOpen(false)}
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
              <AccountSettings
                token={token}
                currentEmail={meQuery.data?.email ?? ""}
                onAuthRefresh={handleAuthSuccess}
                onLogout={handleLogout}
              />
            </div>
          </div>
        ) : null}

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
