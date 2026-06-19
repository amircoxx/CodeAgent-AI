"use client";

import { useState } from "react";
import { FolderPlus, Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import type { ProjectRequest } from "@/types/review";

type ProjectFormProps = {
  isPending: boolean;
  error?: Error | null;
  onSubmit: (payload: ProjectRequest) => void;
};

export function ProjectForm({ isPending, error, onSubmit }: ProjectFormProps) {
  const [name, setName] = useState("CodeGuard Backend");
  const [description, setDescription] = useState(
    "Spring Boot backend code review project",
  );
  const [localError, setLocalError] = useState<string>();

  const displayError = localError ?? error?.message;

  return (
    <Card className="border-white/10 bg-slate-950/76 backdrop-blur">
      <CardHeader>
        <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-md border border-cyan-300/25 bg-cyan-300/10 text-cyan-200">
          <FolderPlus className="h-5 w-5" />
        </div>
        <CardTitle>Create a project</CardTitle>
        <CardDescription>
          Group saved reviews under a named codebase or service.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault();
            setLocalError(undefined);

            if (name.trim().length === 0) {
              setLocalError("Project name is required.");
              return;
            }

            if (description.trim().length === 0) {
              setLocalError("Project description is required.");
              return;
            }

            onSubmit({
              name: name.trim(),
              description: description.trim(),
            });
          }}
        >
          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-200" htmlFor="project-name">
              Project name
            </label>
            <input
              id="project-name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-slate-950/80 px-3 py-2 text-sm text-slate-100 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
              placeholder="CodeGuard Backend"
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-200" htmlFor="project-description">
              Description
            </label>
            <textarea
              id="project-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              className="min-h-24 w-full resize-none rounded-md border border-input bg-slate-950/80 px-3 py-2 text-sm leading-6 text-slate-100 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
              placeholder="Spring Boot backend code review project"
            />
          </div>

          {displayError ? (
            <p className="text-sm text-red-200">{displayError}</p>
          ) : null}

          <Button className="w-full" disabled={isPending} type="submit">
            {isPending ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Creating
              </>
            ) : (
              "Create Project"
            )}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
