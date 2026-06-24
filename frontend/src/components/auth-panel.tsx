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
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [localError, setLocalError] = useState<string>();

  const isRegistering = mode === "register";
  const displayError = localError ?? error?.message;

  return (
    <Card className="max-w-xl">
      <CardHeader>
        <div className="ledger-icon mb-3 flex h-10 w-10 items-center justify-center rounded">
          <LockKeyhole className="h-5 w-5" />
        </div>
        <CardTitle>{isRegistering ? "Create your account" : "Sign in"}</CardTitle>
        <CardDescription>
          Reviews and projects are stored under your own workspace.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="mb-5 grid grid-cols-2 rounded border border-[#25251e] bg-[#eee7d8] p-1">
          <button
            className={`rounded px-3 py-2 text-sm font-medium transition ${
              mode === "login"
                ? "bg-[#171711] text-[#fffdf8]"
                : "text-[#4f4b41] hover:bg-[#fffdf8] hover:text-[#171711]"
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
                ? "bg-[#171711] text-[#fffdf8]"
                : "text-[#4f4b41] hover:bg-[#fffdf8] hover:text-[#171711]"
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
              <label className="text-sm font-bold text-[#25251e]" htmlFor="auth-name">
                Name
              </label>
              <input
                id="auth-name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                className="audit-input flex h-10 w-full rounded border px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-50"
                placeholder="Your name"
              />
            </div>
          ) : null}

          <div className="space-y-2">
            <label className="text-sm font-bold text-[#25251e]" htmlFor="auth-email">
              Email
            </label>
            <input
              id="auth-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="audit-input flex h-10 w-full rounded border px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-50"
              placeholder="you@example.com"
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-bold text-[#25251e]" htmlFor="auth-password">
              Password
            </label>
            <input
              id="auth-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="audit-input flex h-10 w-full rounded border px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-50"
              placeholder="At least 8 characters"
            />
          </div>

          {displayError ? (
            <p className="text-sm font-bold text-[#8d281f]">{displayError}</p>
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
