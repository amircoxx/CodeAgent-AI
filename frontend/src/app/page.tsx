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
  getGitHubConnection,
  getGitHubConnectUrl,
  getGitHubPullRequests,
  getGitHubRepositories,
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
  GitHubPullRequestSummary,
  GitHubRepositoryResponse,
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
  const [selectedGitHubRepository, setSelectedGitHubRepository] = useState<string>();

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
    setSelectedGitHubRepository(undefined);
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

  const githubConnectionQuery = useQuery({
    queryKey: ["github-connection", token],
    queryFn: () => getGitHubConnection(token as string),
    enabled: token !== undefined,
  });

  const githubRepositoriesQuery = useQuery<GitHubRepositoryResponse[], Error>({
    queryKey: ["github-repositories", token],
    queryFn: () => getGitHubRepositories(token as string),
    enabled: token !== undefined && githubConnectionQuery.data?.connected === true,
  });

  const selectedRepository = githubRepositoriesQuery.data?.find(
    (repository) => repository.fullName === selectedGitHubRepository,
  );

  const githubPullRequestsQuery = useQuery<GitHubPullRequestSummary[], Error>({
    queryKey: ["github-pull-requests", token, selectedGitHubRepository],
    queryFn: () =>
      getGitHubPullRequests(
        selectedRepository?.owner ?? "",
        selectedRepository?.name ?? "",
        token as string,
      ),
    enabled:
      token !== undefined &&
      githubConnectionQuery.data?.connected === true &&
      selectedRepository !== undefined,
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

  const githubConnectMutation = useMutation({
    mutationFn: () => getGitHubConnectUrl(token as string),
    onSuccess: (response) => {
      window.location.assign(response.connectUrl);
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
    <main className="code-grid min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      <div className="relative mx-auto flex max-w-7xl flex-col gap-7">
        <header className="ledger-panel rounded-md">
          <div className="flex flex-col gap-5 border-b border-[#25251e] bg-[#fffdf8]/72 px-5 py-5 lg:flex-row lg:items-end lg:justify-between sm:px-7">
            <div className="max-w-3xl">
              <div className="mb-4 inline-flex items-center rounded border border-[#25251e] bg-[#171711] px-3 py-1 text-xs font-extrabold uppercase tracking-[0.18em] text-[#fffdf8]">
                Security review cockpit
              </div>
              <h1 className="font-display text-5xl font-semibold tracking-normal text-[#171711] sm:text-7xl">
                CodeGuard AI
              </h1>
              <p className="mt-4 max-w-2xl text-base leading-7 text-[#4f4b41] sm:text-lg">
                Inspect code snippets and GitHub pull requests for bugs,
                security risks, performance issues, and maintainability gaps.
              </p>
            </div>

            {token && meQuery.data ? (
              <div className="relative">
                <button
                  className="flex w-full min-w-64 items-center justify-between gap-4 rounded border border-[#25251e] bg-[#f5f1e6] px-4 py-3 text-left text-sm text-[#25251e] transition-colors hover:bg-[#eee7d8] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#bf3b2d] lg:w-auto"
                  type="button"
                  aria-expanded={isAccountMenuOpen}
                  aria-haspopup="menu"
                  onClick={() => setIsAccountMenuOpen((isOpen) => !isOpen)}
                >
                  <span className="min-w-0">
                    <span className="block truncate font-extrabold text-[#171711]">{meQuery.data.name}</span>
                    <span className="block truncate text-[#6a6659]">{meQuery.data.email}</span>
                  </span>
                  <ChevronDown
                    className={`h-4 w-4 shrink-0 text-[#6a6659] transition-transform ${
                      isAccountMenuOpen ? "rotate-180" : ""
                    }`}
                  />
                </button>

                {isAccountMenuOpen ? (
                  <div
                    className="absolute right-0 z-20 mt-2 w-full min-w-64 rounded border border-[#25251e] bg-[#fffdf8] p-1 text-sm shadow-[8px_8px_0_rgba(23,23,17,0.14)] lg:w-64"
                    role="menu"
                  >
                    <button
                      className="flex w-full items-center gap-2 rounded px-3 py-2 text-left font-bold text-[#25251e] hover:bg-[#eee7d8] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#bf3b2d]"
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        setIsAccountMenuOpen(false);
                        setIsAccountSettingsOpen(true);
                      }}
                    >
                      <Settings className="h-4 w-4 text-[#315f72]" />
                      Settings
                    </button>
                    <button
                      className="flex w-full items-center gap-2 rounded px-3 py-2 text-left font-bold text-[#8d281f] hover:bg-[#f1d7d1] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#bf3b2d]"
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
          </div>
          <div className="grid gap-px bg-[#25251e] text-xs font-extrabold uppercase tracking-[0.14em] text-[#4f4b41] sm:grid-cols-3">
            <div className="bg-[#eee7d8] px-5 py-3 sm:px-7">Manual code audit</div>
            <div className="bg-[#eee7d8] px-5 py-3 sm:px-7">GitHub PR inspection</div>
            <div className="bg-[#eee7d8] px-5 py-3 sm:px-7">Saved risk ledger</div>
          </div>
        </header>

        {token && isAccountSettingsOpen ? (
          <div
            className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-[#171711]/70 px-4 py-6 backdrop-blur-sm sm:py-10"
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
                  className="flex h-10 w-10 items-center justify-center rounded border border-[#25251e] bg-[#fffdf8] text-[#25251e] transition-colors hover:bg-[#f1d7d1] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#bf3b2d]"
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
          <section className="ledger-panel rounded-md p-6 text-sm font-medium text-[#4f4b41]">
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
                  connection={githubConnectionQuery.data}
                  isConnectionLoading={githubConnectionQuery.isLoading}
                  connectionError={githubConnectionQuery.error}
                  repositories={githubRepositoriesQuery.data}
                  areRepositoriesLoading={githubRepositoriesQuery.isLoading}
                  repositoriesError={githubRepositoriesQuery.error}
                  pullRequests={githubPullRequestsQuery.data}
                  arePullRequestsLoading={githubPullRequestsQuery.isLoading}
                  pullRequestsError={githubPullRequestsQuery.error}
                  projects={projectsQuery.data}
                  areProjectsLoading={projectsQuery.isLoading}
                  projectsError={projectsQuery.error}
                  selectedRepositoryFullName={selectedGitHubRepository}
                  isConnectPending={githubConnectMutation.isPending}
                  onConnect={() => githubConnectMutation.mutate()}
                  onRepositoryChange={setSelectedGitHubRepository}
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
