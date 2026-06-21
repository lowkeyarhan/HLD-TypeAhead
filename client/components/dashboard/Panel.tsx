import type { ReactNode } from "react";

import type { PanelTone } from "@/components/dashboard/types";
import { getSurfaceStyle } from "@/components/dashboard/utils";

type PanelProps = {
  title: string;
  subtitle?: string;
  tone?: PanelTone;
  children: ReactNode;
  className?: string;
};

export function Panel({
  title,
  subtitle,
  tone = "muted",
  children,
  className = "",
}: PanelProps) {
  return (
    <section
      className={`rounded-[24px] p-5 sm:p-6 ${className}`}
      style={getSurfaceStyle(tone)}
    >
      <header className="mb-5">
        <p className="font-display text-[1.05rem] font-medium text-[hsl(var(--text-primary))]">
          {title}
        </p>
        {subtitle ? (
          <p className="mt-1 max-w-[62ch] text-[0.92rem] leading-6 text-[hsl(var(--text-muted))]">
            {subtitle}
          </p>
        ) : null}
      </header>
      {children}
    </section>
  );
}
