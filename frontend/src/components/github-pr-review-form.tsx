"use client";

import { useState } from "react";
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
  GitHubPullRequestReviewRequest,
  ProjectResponse,
} from "@/types/review";

type GitHubPrReviewFormProps = {
  isPending: boolean;
  projects?: ProjectResponse[];
  areProjectsLoading: boolean;
  projectsError?: Error | null;
  onSubmit: (payload: GitHubPullRequestReviewRequest) => void;
};

const noProjectValue = "__no_project__";
const pullRequestUrlPattern =
  /^https:\/\/github\.com\/[^/\s]+\/[^/\s]+\/pull\/\d+$/i;

export function GitHubPrReviewForm({
  isPending,
  projects,
  areProjectsLoading,
  projectsError,
  onSubmit,
}: GitHubPrReviewFormProps) {
  const [projectId, setProjectId] = useState(noProjectValue);
  const [pullRequestUrl, setPullRequestUrl] = useState(
    "https://github.com/owner/repo/pull/123",
  );
  const [localError, setLocalError] = useState<string>();

  return (
    <Card className="border-white/10 bg-slate-950/76 backdrop-blur">
      <CardHeader>
        <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-md border border-cyan-300/25 bg-cyan-300/10 text-cyan-100">
          <GitPullRequest className="h-5 w-5" />
        </div>
        <CardTitle>Review a GitHub pull request</CardTitle>
        <CardDescription>
          Fetch public PR changes and run the same structured review pipeline.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form
          className="space-y-5"
          onSubmit={(event) => {
            event.preventDefault();
            setLocalError(undefined);

            const trimmedUrl = pullRequestUrl.trim();
            if (!pullRequestUrlPattern.test(trimmedUrl)) {
              setLocalError("Use a GitHub PR URL like https://github.com/owner/repo/pull/123.");
              return;
            }

            onSubmit({
              ...(projectId === noProjectValue ? {} : { projectId: Number(projectId) }),
              pullRequestUrl: trimmedUrl,
            });
          }}
        >
          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-200" htmlFor="pull-request-url">
              Pull request URL
            </label>
            <input
              id="pull-request-url"
              value={pullRequestUrl}
              onChange={(event) => setPullRequestUrl(event.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-slate-950/80 px-3 py-2 text-sm text-slate-100 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
              placeholder="https://github.com/owner/repo/pull/123"
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-200" htmlFor="github-project">
              Project
            </label>
            <Select value={projectId} onValueChange={setProjectId}>
              <SelectTrigger id="github-project" className="bg-slate-950/80">
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
              <p className="text-xs text-slate-400">Loading projects...</p>
            ) : projectsError ? (
              <p className="text-xs text-red-200">{projectsError.message}</p>
            ) : projects?.length === 0 ? (
              <p className="text-xs text-slate-400">
                No projects yet. PR reviews can still be submitted without one.
              </p>
            ) : null}
          </div>

          {localError ? (
            <p className="text-sm text-red-200">{localError}</p>
          ) : null}

          <Button className="w-full" disabled={isPending} type="submit" size="lg">
            {isPending ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Reviewing PR
              </>
            ) : (
              "Review Pull Request"
            )}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
