"use client";

import { useEffect, useState } from "react";
import { GitPullRequest, Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type {
  GitHubConnectionResponse,
  GitHubPullRequestReviewRequest,
  GitHubPullRequestSummary,
  GitHubRepositoryResponse,
  ProjectResponse,
} from "@/types/review";

type GitHubPrReviewFormProps = {
  isPending: boolean;
  connection?: GitHubConnectionResponse;
  isConnectionLoading: boolean;
  connectionError?: Error | null;
  repositories?: GitHubRepositoryResponse[];
  areRepositoriesLoading: boolean;
  repositoriesError?: Error | null;
  pullRequests?: GitHubPullRequestSummary[];
  arePullRequestsLoading: boolean;
  pullRequestsError?: Error | null;
  projects?: ProjectResponse[];
  areProjectsLoading: boolean;
  projectsError?: Error | null;
  selectedRepositoryFullName?: string;
  isConnectPending: boolean;
  onConnect: () => void;
  onRepositoryChange: (repositoryFullName?: string) => void;
  onSubmit: (payload: GitHubPullRequestReviewRequest) => void;
};

const noProjectValue = "__no_project__";
const noRepositoryValue = "__no_repository__";
const noPullRequestValue = "__no_pull_request__";

export function GitHubPrReviewForm({
  isPending,
  connection,
  isConnectionLoading,
  connectionError,
  repositories,
  areRepositoriesLoading,
  repositoriesError,
  pullRequests,
  arePullRequestsLoading,
  pullRequestsError,
  projects,
  areProjectsLoading,
  projectsError,
  selectedRepositoryFullName,
  isConnectPending,
  onConnect,
  onRepositoryChange,
  onSubmit,
}: GitHubPrReviewFormProps) {
  const [projectId, setProjectId] = useState(noProjectValue);
  const [pullRequestNumber, setPullRequestNumber] = useState(noPullRequestValue);
  const [localError, setLocalError] = useState<string>();

  useEffect(() => {
    setPullRequestNumber(noPullRequestValue);
  }, [selectedRepositoryFullName]);

  const selectedRepository = repositories?.find(
    (repository) => repository.fullName === selectedRepositoryFullName,
  );
  const selectedPullRequest = pullRequests?.find(
    (pullRequest) => String(pullRequest.number) === pullRequestNumber,
  );

  return (
    <Card>
      <CardHeader>
        <div className="ledger-icon mb-3 flex h-10 w-10 items-center justify-center rounded">
          <GitPullRequest className="h-5 w-5" />
        </div>
        <CardTitle>Review a GitHub pull request</CardTitle>
        <CardDescription>
          Connect GitHub, choose an allowed repository, and grade an open PR.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {isConnectionLoading ? (
          <p className="text-sm font-medium text-[#4f4b41]">Checking GitHub connection...</p>
        ) : connectionError ? (
          <div className="space-y-3">
            <p className="text-sm font-bold text-[#8d281f]">{connectionError.message}</p>
            <Button type="button" onClick={onConnect} disabled={isConnectPending}>
              {isConnectPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              Connect GitHub
            </Button>
          </div>
        ) : !connection?.connected ? (
          <div className="space-y-4">
            <p className="text-sm leading-6 text-[#4f4b41]">
              Install the CodeGuard GitHub App on selected repositories before
              loading pull requests.
            </p>
            <Button type="button" onClick={onConnect} disabled={isConnectPending}>
              {isConnectPending ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Opening GitHub
                </>
              ) : (
                "Connect GitHub"
              )}
            </Button>
          </div>
        ) : (
          <form
            className="space-y-5"
            onSubmit={(event) => {
              event.preventDefault();
              setLocalError(undefined);

              if (!selectedRepository) {
                setLocalError("Select a GitHub repository first.");
                return;
              }
              if (!selectedPullRequest) {
                setLocalError("Select an open pull request first.");
                return;
              }

              onSubmit({
                ...(projectId === noProjectValue ? {} : { projectId: Number(projectId) }),
                owner: selectedRepository.owner,
                repo: selectedRepository.name,
                pullRequestNumber: selectedPullRequest.number,
              });
            }}
          >
            <div className="rounded border border-[#bdb5a1] bg-[#f2eee2] p-3 text-xs leading-5 text-[#4f4b41]">
              Connected to{" "}
              <span className="font-extrabold text-[#171711]">
                {connection.accountLogin}
              </span>
              {connection.accountType ? ` (${connection.accountType})` : null}
            </div>

            <div className="space-y-2">
              <label className="text-sm font-bold text-[#25251e]" htmlFor="github-repository">
                Repository
              </label>
              <Select
                value={selectedRepositoryFullName ?? noRepositoryValue}
                onValueChange={(value) => {
                  onRepositoryChange(value === noRepositoryValue ? undefined : value);
                }}
              >
                <SelectTrigger id="github-repository">
                  <SelectValue placeholder="Select repository" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={noRepositoryValue}>Select repository</SelectItem>
                  {repositories?.map((repository) => (
                    <SelectItem key={repository.id} value={repository.fullName}>
                      {repository.fullName}
                      {repository.privateRepository ? " (private)" : ""}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {areRepositoriesLoading ? (
                <p className="text-xs text-[#6a6659]">Loading repositories...</p>
              ) : repositoriesError ? (
                <p className="text-xs font-bold text-[#8d281f]">{repositoriesError.message}</p>
              ) : repositories?.length === 0 ? (
                <p className="text-xs text-[#6a6659]">
                  No repositories found. Update the GitHub App installation to allow repository access.
                </p>
              ) : null}
            </div>

            <div className="space-y-2">
              <label className="text-sm font-bold text-[#25251e]" htmlFor="github-pull-request">
                Pull request
              </label>
              <Select
                value={pullRequestNumber}
                onValueChange={setPullRequestNumber}
                disabled={!selectedRepository}
              >
                <SelectTrigger id="github-pull-request">
                  <SelectValue placeholder="Select pull request" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={noPullRequestValue}>Select pull request</SelectItem>
                  {pullRequests?.map((pullRequest) => (
                    <SelectItem key={pullRequest.number} value={String(pullRequest.number)}>
                      #{pullRequest.number} {pullRequest.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {arePullRequestsLoading ? (
                <p className="text-xs text-[#6a6659]">Loading pull requests...</p>
              ) : pullRequestsError ? (
                <p className="text-xs font-bold text-[#8d281f]">{pullRequestsError.message}</p>
              ) : selectedRepository && pullRequests?.length === 0 ? (
                <p className="text-xs text-[#6a6659]">No open pull requests for this repository.</p>
              ) : null}
            </div>

            <div className="space-y-2">
              <label className="text-sm font-bold text-[#25251e]" htmlFor="github-project">
                Project
              </label>
              <Select value={projectId} onValueChange={setProjectId}>
                <SelectTrigger id="github-project">
                  <SelectValue placeholder="Select project" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={noProjectValue}>No project</SelectItem>
                  {projects?.map((project) => (
                    <SelectItem key={project.id} value={String(project.id)}>
                      {project.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {areProjectsLoading ? (
                <p className="text-xs text-[#6a6659]">Loading projects...</p>
              ) : projectsError ? (
                <p className="text-xs font-bold text-[#8d281f]">{projectsError.message}</p>
              ) : projects?.length === 0 ? (
                <p className="text-xs text-[#6a6659]">
                  No projects yet. PR reviews can still be submitted without one.
                </p>
              ) : null}
            </div>

            {localError ? (
              <p className="text-sm font-bold text-[#8d281f]">{localError}</p>
            ) : null}

            <Button className="w-full" disabled={isPending} type="submit" size="lg">
              {isPending ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Grading PR
                </>
              ) : (
                "Grade Pull Request"
              )}
            </Button>
          </form>
        )}
      </CardContent>
    </Card>
  );
}
