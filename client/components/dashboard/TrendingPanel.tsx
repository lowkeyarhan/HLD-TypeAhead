import { Panel } from "@/components/dashboard/Panel";
import type { Suggestion } from "@/components/dashboard/types";
import { formatCount } from "@/components/dashboard/utils";

type TrendingPanelProps = {
  trending: Suggestion[];
  dashboardError: string | null;
  isLoadingDashboard: boolean;
  onSubmit: (query: string) => void;
};

export function TrendingPanel({
  trending,
  dashboardError,
  isLoadingDashboard,
  onSubmit,
}: TrendingPanelProps) {
  return (
    <Panel title="Trending">
      {dashboardError && trending.length === 0 ? (
        <div className="rounded-[16px] bg-[hsl(var(--bg-elevated))] p-4 text-[0.92rem] text-[hsl(var(--text-muted))]">
          {dashboardError}
        </div>
      ) : isLoadingDashboard && trending.length === 0 ? (
        <div className="rounded-[16px] bg-[hsl(var(--bg-elevated))] p-4 text-[0.92rem] text-[hsl(var(--text-muted))]">
          Loading trending queries...
        </div>
      ) : (
        <div className="space-y-3">
          {trending.map((item, index) => (
            <button
              key={item.query}
              type="button"
              onClick={() => onSubmit(item.query)}
              className="flex w-full items-center justify-between gap-4 rounded-[16px] bg-[hsl(var(--bg-elevated))] px-4 py-3 text-left transition-colors duration-200 hover:bg-[hsl(var(--bg-subtle))]"
            >
              <div className="flex min-w-0 items-center gap-3">
                <div className="w-7 text-[0.82rem] text-[hsl(var(--text-muted))]">
                  {String(index + 1).padStart(2, "0")}
                </div>
                <div className="min-w-0">
                  <div className="truncate font-display text-[0.96rem] font-medium text-[hsl(var(--text-primary))]">
                    {item.query}
                  </div>
                </div>
              </div>
              <div className="rounded-full bg-[hsl(var(--bg-subtle))] px-3 py-1 text-[0.8rem] text-[hsl(var(--text-secondary))]">
                {formatCount(item.count)}
              </div>
            </button>
          ))}
        </div>
      )}
    </Panel>
  );
}
