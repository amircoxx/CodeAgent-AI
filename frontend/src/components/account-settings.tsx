"use client";

import { useMutation } from "@tanstack/react-query";
import { KeyRound, Mail, ShieldCheck, Trash2 } from "lucide-react";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  completePasswordChange,
  deleteAccount,
  requestPasswordChange,
  updateAccountEmail,
  verifyPasswordChange,
} from "@/lib/api";
import type {
  AuthResponse,
  CompletePasswordChangeRequest,
  DeleteAccountRequest,
  MessageResponse,
  PasswordChangeRequestResponse,
  PasswordVerificationRequest,
  UpdateEmailRequest,
} from "@/types/review";

type AccountSettingsProps = {
  token: string;
  currentEmail: string;
  onAuthRefresh: (response: AuthResponse) => void;
  onLogout: (message?: string) => void;
};

export function AccountSettings({
  token,
  currentEmail,
  onAuthRefresh,
  onLogout,
}: AccountSettingsProps) {
  const [passwordCode, setPasswordCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newEmail, setNewEmail] = useState(currentEmail);
  const [emailPassword, setEmailPassword] = useState("");
  const [deletePassword, setDeletePassword] = useState("");
  const [deleteConfirmation, setDeleteConfirmation] = useState("");
  const [passwordMessage, setPasswordMessage] = useState<string>();
  const [emailMessage, setEmailMessage] = useState<string>();
  const [deleteMessage, setDeleteMessage] = useState<string>();
  const [localPasswordError, setLocalPasswordError] = useState<string>();
  const [localEmailError, setLocalEmailError] = useState<string>();
  const [localDeleteError, setLocalDeleteError] = useState<string>();

  const requestMutation = useMutation<PasswordChangeRequestResponse, Error>({
    mutationFn: () => requestPasswordChange(token),
    onSuccess: (response) => {
      setPasswordMessage(
        response.devVerificationCode
          ? `${response.message} Development code: ${response.devVerificationCode}`
          : response.message,
      );
      setLocalPasswordError(undefined);
    },
  });

  const verifyMutation = useMutation<MessageResponse, Error, PasswordVerificationRequest>({
    mutationFn: (payload) => verifyPasswordChange(payload, token),
    onSuccess: (response) => {
      setPasswordMessage(response.message);
      setLocalPasswordError(undefined);
    },
  });

  const completeMutation = useMutation<MessageResponse, Error, CompletePasswordChangeRequest>({
    mutationFn: (payload) => completePasswordChange(payload, token),
    onSuccess: (response) => {
      onLogout(response.message);
    },
  });

  const emailMutation = useMutation<AuthResponse, Error, UpdateEmailRequest>({
    mutationFn: (payload) => updateAccountEmail(payload, token),
    onSuccess: (response) => {
      setEmailPassword("");
      setEmailMessage("Email updated.");
      setLocalEmailError(undefined);
      onAuthRefresh(response);
    },
  });

  const deleteMutation = useMutation<MessageResponse, Error, DeleteAccountRequest>({
    mutationFn: (payload) => deleteAccount(payload, token),
    onSuccess: (response) => {
      onLogout(response.message);
    },
  });

  const passwordError =
    localPasswordError ??
    requestMutation.error?.message ??
    verifyMutation.error?.message ??
    completeMutation.error?.message;
  const emailError = localEmailError ?? emailMutation.error?.message;
  const deleteError = localDeleteError ?? deleteMutation.error?.message;
  const isPasswordPending =
    requestMutation.isPending || verifyMutation.isPending || completeMutation.isPending;

  return (
    <Card className="border-white/10 bg-slate-950/76">
      <CardHeader>
        <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-md border border-cyan-300/25 bg-cyan-300/10 text-cyan-100">
          <ShieldCheck className="h-5 w-5" />
        </div>
        <CardTitle>Account security</CardTitle>
        <CardDescription>Manage sign-in credentials for this workspace.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <section className="space-y-3 border-b border-white/10 pb-5">
          <div className="flex items-center gap-2 text-sm font-semibold text-slate-100">
            <KeyRound className="h-4 w-4 text-emerald-200" />
            Password
          </div>
          <div className="grid gap-3 sm:grid-cols-[1fr_auto]">
            <Button
              type="button"
              variant="secondary"
              disabled={isPasswordPending}
              onClick={() => requestMutation.mutate()}
            >
              Request verification
            </Button>
            <Button
              type="button"
              variant="ghost"
              disabled={isPasswordPending}
              onClick={() => {
                if (passwordCode.trim().length === 0) {
                  setLocalPasswordError("Verification code is required.");
                  return;
                }
                verifyMutation.mutate({ verificationCode: passwordCode.trim() });
              }}
            >
              Verify code
            </Button>
          </div>
          <input
            value={passwordCode}
            onChange={(event) => setPasswordCode(event.target.value)}
            className="flex h-10 w-full rounded-md border border-input bg-slate-950/80 px-3 py-2 text-sm text-slate-100 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
            placeholder="Verification code"
          />
          <form
            className="grid gap-3 sm:grid-cols-[1fr_auto]"
            onSubmit={(event) => {
              event.preventDefault();
              if (passwordCode.trim().length === 0) {
                setLocalPasswordError("Verification code is required.");
                return;
              }
              if (newPassword.length < 8) {
                setLocalPasswordError("Password must be at least 8 characters.");
                return;
              }
              completeMutation.mutate({
                verificationCode: passwordCode.trim(),
                newPassword,
              });
            }}
          >
            <input
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-slate-950/80 px-3 py-2 text-sm text-slate-100 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
              placeholder="New password"
            />
            <Button type="submit" disabled={isPasswordPending}>
              Update password
            </Button>
          </form>
          {passwordError ? <p className="text-sm text-red-200">{passwordError}</p> : null}
          {passwordMessage ? <p className="text-sm text-emerald-200">{passwordMessage}</p> : null}
        </section>

        <section className="space-y-3 border-b border-white/10 pb-5">
          <div className="flex items-center gap-2 text-sm font-semibold text-slate-100">
            <Mail className="h-4 w-4 text-cyan-200" />
            Email
          </div>
          <form
            className="space-y-3"
            onSubmit={(event) => {
              event.preventDefault();
              if (newEmail.trim().length === 0 || !newEmail.includes("@")) {
                setLocalEmailError("Enter a valid email address.");
                return;
              }
              if (emailPassword.length === 0) {
                setLocalEmailError("Current password is required.");
                return;
              }
              emailMutation.mutate({
                email: newEmail.trim(),
                currentPassword: emailPassword,
              });
            }}
          >
            <input
              type="email"
              value={newEmail}
              onChange={(event) => setNewEmail(event.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-slate-950/80 px-3 py-2 text-sm text-slate-100 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
              placeholder="you@example.com"
            />
            <div className="grid gap-3 sm:grid-cols-[1fr_auto]">
              <input
                type="password"
                value={emailPassword}
                onChange={(event) => setEmailPassword(event.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-slate-950/80 px-3 py-2 text-sm text-slate-100 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                placeholder="Current password"
              />
              <Button type="submit" disabled={emailMutation.isPending}>
                Update email
              </Button>
            </div>
          </form>
          {emailError ? <p className="text-sm text-red-200">{emailError}</p> : null}
          {emailMessage ? <p className="text-sm text-emerald-200">{emailMessage}</p> : null}
        </section>

        <section className="space-y-3">
          <div className="flex items-center gap-2 text-sm font-semibold text-slate-100">
            <Trash2 className="h-4 w-4 text-red-200" />
            Delete account
          </div>
          <form
            className="space-y-3"
            onSubmit={(event) => {
              event.preventDefault();
              if (deletePassword.length === 0) {
                setLocalDeleteError("Current password is required.");
                return;
              }
              if (deleteConfirmation !== "DELETE") {
                setLocalDeleteError("Type DELETE to confirm account deletion.");
                return;
              }
              deleteMutation.mutate({
                currentPassword: deletePassword,
                confirmation: deleteConfirmation,
              });
            }}
          >
            <input
              type="password"
              value={deletePassword}
              onChange={(event) => setDeletePassword(event.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-slate-950/80 px-3 py-2 text-sm text-slate-100 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
              placeholder="Current password"
            />
            <div className="grid gap-3 sm:grid-cols-[1fr_auto]">
              <input
                value={deleteConfirmation}
                onChange={(event) => setDeleteConfirmation(event.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-slate-950/80 px-3 py-2 text-sm text-slate-100 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                placeholder="Type DELETE"
              />
              <Button
                type="submit"
                disabled={deleteMutation.isPending}
                className="bg-red-500 text-white hover:bg-red-400"
              >
                Delete account
              </Button>
            </div>
          </form>
          {deleteError ? <p className="text-sm text-red-200">{deleteError}</p> : null}
          {deleteMessage ? <p className="text-sm text-emerald-200">{deleteMessage}</p> : null}
        </section>
      </CardContent>
    </Card>
  );
}
