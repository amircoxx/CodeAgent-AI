"use client";

import { useState } from "react";
import { Loader2, ShieldCheck } from "lucide-react";

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
import { Textarea } from "@/components/ui/textarea";
import type { ProjectResponse, ReviewRequest } from "@/types/review";

const languages = ["JavaScript", "TypeScript", "Python", "Java", "SQL", "C++"];

type CodeReviewFormProps = {
  isPending: boolean;
  projects?: ProjectResponse[];
  areProjectsLoading: boolean;
  projectsError?: Error | null;
  onSubmit: (payload: ReviewRequest) => void;
};

const noProjectValue = "__no_project__";

export function CodeReviewForm({
  isPending,
  projects,
  areProjectsLoading,
  projectsError,
  onSubmit,
}: CodeReviewFormProps) {
  const [title, setTitle] = useState("Login Controller Review");
  const [projectId, setProjectId] = useState(noProjectValue);
  const [language, setLanguage] = useState("JavaScript");
  const [code, setCode] = useState(
    "function test() {\n  console.log('hi')\n}",
  );
  const [localError, setLocalError] = useState<string>();

  return (
    <Card className="border-white/10 bg-slate-950/76 backdrop-blur">
      <CardHeader>
        <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-md border border-emerald-300/25 bg-emerald-300/10 text-emerald-200">
          <ShieldCheck className="h-5 w-5" />
        </div>
        <CardTitle>Analyze a code snippet</CardTitle>
        <CardDescription>
          Paste code, choose the language, and request a structured mock review
          from the Spring Boot API.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form
          className="space-y-5"
          onSubmit={(event) => {
            event.preventDefault();
            setLocalError(undefined);

            if (title.trim().length === 0) {
              setLocalError("Review title is required.");
              return;
            }

            if (language.trim().length === 0) {
              setLocalError("Language is required.");
              return;
            }

            if (code.trim().length === 0) {
              setLocalError("Code is required.");
              return;
            }

            onSubmit({
              ...(projectId === noProjectValue ? {} : { projectId: Number(projectId) }),
              title: title.trim(),
              language: language.trim(),
              code: code.trim(),
            });
          }}
        >
          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-200" htmlFor="title">
              Review title
            </label>
            <input
              id="title"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-slate-950/80 px-3 py-2 text-sm text-slate-100 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
              placeholder="Login Controller Review"
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-200" htmlFor="project">
              Project
            </label>
            <Select value={projectId} onValueChange={setProjectId}>
              <SelectTrigger id="project" className="bg-slate-950/80">
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
                No projects yet. Reviews can still be submitted without one.
              </p>
            ) : null}
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-200" htmlFor="language">
              Language
            </label>
            <Select value={language} onValueChange={setLanguage}>
              <SelectTrigger id="language" className="bg-slate-950/80">
                <SelectValue placeholder="Select language" />
              </SelectTrigger>
              <SelectContent>
                {languages.map((item) => (
                  <SelectItem key={item} value={item}>
                    {item}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-200" htmlFor="code">
              Code
            </label>
            <Textarea
              id="code"
              value={code}
              onChange={(event) => setCode(event.target.value)}
              spellCheck={false}
              className="min-h-[410px] resize-none bg-slate-950/80 font-mono text-[13px] leading-6 text-slate-100"
              placeholder="Paste code to review..."
            />
          </div>

          {localError ? (
            <p className="text-sm text-red-200">{localError}</p>
          ) : null}

          <Button className="w-full" disabled={isPending} type="submit" size="lg">
            {isPending ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Analyzing
              </>
            ) : (
              "Analyze Code"
            )}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
