"use client";

import { useState } from "react";
import { LockKeyhole, Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import type { LoginRequest, RegisterRequest } from "@/types/review";

type AuthMode = "login" | "register";

type AuthPanelProps = {
  isPending: boolean;
  error?: Error | null;
  onLogin: (payload: LoginRequest) => void;
  onRegister: (payload: RegisterRequest) => void;
};

export function AuthPanel({
  isPending,
  error,
  onLogin,
  onRegister,
}: AuthPanelProps) {
  const [mode, setMode] = useState<AuthMode>("login");
  const [name, setName] = useState("Amir Cox");
  const [email, setEmail] = useState("amir@example.com");
  const [password, setPassword] = useState("password123");
  const [localError, setLocalError] = useState<string>();

  const isRegistering = mode === "register";
  const displayError = localError ?? error?.message;

  return (
    <Card className="max-w-xl border-white/10 bg-slate-950/80 backdrop-blur">
      <CardHeader>
        <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-md border border-emerald-300/25 bg-emerald-300/10 text-emerald-200">
          <LockKeyhole className="h-5 w-5" />
        </div>
        <CardTitle>{isRegistering ? "Create your account" : "Sign in"}</CardTitle>
        <CardDescription>
          Reviews and projects are stored under your own workspace.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="mb-5 grid grid-cols-2 rounded-md border border-white/10 bg-slate-900/70 p-1">
          <button
            className={`rounded px-3 py-2 text-sm font-medium transition ${
              mode === "login"
                ? "bg-emerald-300 text-slate-950"
                : "text-slate-300 hover:text-white"
            }`}
            type="button"
            onClick={() => {
              setLocalError(undefined);
              setMode("login");
            }}
          >
            Login
          </button>
          <button
            className={`rounded px-3 py-2 text-sm font-medium transition ${
              mode === "register"
                ? "bg-emerald-300 text-slate-950"
                : "text-slate-300 hover:text-white"
            }`}
            type="button"
            onClick={() => {
              setLocalError(undefined);
              setMode("register");
            }}
          >
            Register
          </button>
        </div>

        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault();
            setLocalError(undefined);

            if (isRegistering && name.trim().length === 0) {
              setLocalError("Name is required.");
              return;
            }

            if (email.trim().length === 0) {
              setLocalError("Email is required.");
              return;
            }

            if (!email.includes("@")) {
              setLocalError("Enter a valid email address.");
              return;
            }

            if (password.length === 0) {
              setLocalError("Password is required.");
              return;
            }

            if (password.length < 8) {
              setLocalError("Password must be at least 8 characters.");
              return;
            }

            if (isRegistering) {
              onRegister({
                name: name.trim(),
                email: email.trim(),
                password,
              });
              return;
            }

            onLogin({
              email: email.trim(),
              password,
            });
          }}
        >
          {isRegistering ? (
            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-200" htmlFor="auth-name">
                Name
              </label>
              <input
                id="auth-name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-slate-950/80 px-3 py-2 text-sm text-slate-100 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                placeholder="Amir Cox"
              />
            </div>
          ) : null}

          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-200" htmlFor="auth-email">
              Email
            </label>
            <input
              id="auth-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-slate-950/80 px-3 py-2 text-sm text-slate-100 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
              placeholder="amir@example.com"
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-200" htmlFor="auth-password">
              Password
            </label>
            <input
              id="auth-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-slate-950/80 px-3 py-2 text-sm text-slate-100 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
              placeholder="password123"
            />
          </div>

          {displayError ? (
            <p className="text-sm text-red-200">{displayError}</p>
          ) : null}

          <Button className="w-full" disabled={isPending} type="submit" size="lg">
            {isPending ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                {isRegistering ? "Creating" : "Signing in"}
              </>
            ) : isRegistering ? (
              "Create Account"
            ) : (
              "Sign In"
            )}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
