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
  const [postComment, setPostComment] = useState(false);
  const [localError, setLocalError] = useState<string>();

  return (
    <Card>
      <CardHeader>
        <div className="ledger-icon mb-3 flex h-10 w-10 items-center justify-center rounded">
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
              postComment,
            });
          }}
        >
          <div className="space-y-2">
            <label className="text-sm font-bold text-[#25251e]" htmlFor="pull-request-url">
              Pull request URL
            </label>
            <input
              id="pull-request-url"
              value={pullRequestUrl}
              onChange={(event) => setPullRequestUrl(event.target.value)}
              className="audit-input flex h-10 w-full rounded border px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-50"
              placeholder="https://github.com/owner/repo/pull/123"
            />
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

          <label className="flex cursor-pointer items-start gap-3 rounded border border-[#bdb5a1] bg-[#f2eee2] p-3 text-sm text-[#25251e]">
            <input
              checked={postComment}
              onChange={(event) => setPostComment(event.target.checked)}
              type="checkbox"
              className="mt-0.5 h-4 w-4 rounded border-[#8c8574] bg-[#fffdf8] text-[#bf3b2d] focus:ring-[#bf3b2d]"
            />
            <span>
              <span className="block font-bold text-[#171711]">
                Post review summary comment to GitHub
              </span>
              <span className="mt-1 block text-xs leading-5 text-[#6a6659]">
                Requires backend GitHub comment configuration. The token never leaves the server.
              </span>
            </span>
          </label>

          {localError ? (
            <p className="text-sm font-bold text-[#8d281f]">{localError}</p>
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
