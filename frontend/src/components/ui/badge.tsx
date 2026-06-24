import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded border px-2.5 py-0.5 text-xs font-extrabold uppercase tracking-[0.08em] transition-colors",
  {
    variants: {
      variant: {
        default:
          "border-[#171711] bg-[#171711] text-[#fffdf8]",
        secondary:
          "border-[#315f72] bg-[#dce8eb] text-[#214756]",
        outline: "border-[#8c8574] bg-[#fffdf8] text-[#4f4b41]",
        low: "border-[#5e7b37] bg-[#e8eedb] text-[#3f5821]",
        medium: "border-[#aa7a1d] bg-[#f3e5be] text-[#735116]",
        high: "border-[#bf3b2d] bg-[#f1d7d1] text-[#8d281f]",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  },
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return (
    <div className={cn(badgeVariants({ variant }), className)} {...props} />
  );
}

export { Badge, badgeVariants };
