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
import type { ReviewRequest } from "@/types/review";

const languages = ["JavaScript", "TypeScript", "Python", "Java", "SQL", "C++"];

type CodeReviewFormProps = {
  isPending: boolean;
  onSubmit: (payload: ReviewRequest) => void;
};

export function CodeReviewForm({ isPending, onSubmit }: CodeReviewFormProps) {
  const [language, setLanguage] = useState("JavaScript");
  const [code, setCode] = useState(
    "function test() {\n  console.log('hi')\n}",
  );

  const isDisabled = isPending || code.trim().length === 0;

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
            onSubmit({ language, code });
          }}
        >
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

          <Button className="w-full" disabled={isDisabled} type="submit" size="lg">
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
