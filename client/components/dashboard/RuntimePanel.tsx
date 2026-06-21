import type { DocsLink, Metric } from "@/components/dashboard/types";
import { Panel } from "@/components/dashboard/Panel";
import { formatPercentage } from "@/components/dashboard/utils";

type RuntimePanelProps = {
  metricBlocks: Metric[];
  nodeRates: Array<[string, number]>;
  docsLinks: DocsLink[];
};

function MetricCard({ label, value }: Metric) {
  return (
    <div
      className="rounded-[16px] p-4 sm:p-5"
      style={{ backgroundColor: "hsl(var(--bg-elevated))" }}
    >
      <div className="text-[0.76rem] font-medium uppercase tracking-[0.18em] text-[hsl(var(--text-muted))]">
        {label}
      </div>
      <div className="mt-2 font-display text-[1.35rem] font-medium text-[hsl(var(--text-primary))] sm:text-[1.65rem]">
        {value}
      </div>
    </div>
  );
}

function NodeRateRow({ nodeId, rate }: { nodeId: string; rate: number }) {
  return (
    <div className="space-y-2 rounded-[16px] bg-[hsl(var(--bg-elevated))] p-4">
      <div className="flex items-center justify-between gap-4">
        <span className="font-display text-[0.92rem] font-medium text-[hsl(var(--text-primary))]">
          {nodeId}
        </span>
        <span className="text-[0.88rem] text-[hsl(var(--text-muted))]">
          {formatPercentage(rate)}
        </span>
      </div>
      <div className="h-2 rounded-full bg-[hsl(var(--bg-subtle))]">
        <div
          className="h-full rounded-full bg-[hsl(var(--accent-500))] transition-[width] duration-300"
          style={{ width: `${Math.max(rate * 100, 4)}%` }}
        />
      </div>
    </div>
  );
}

export function RuntimePanel({
  metricBlocks,
  nodeRates,
  docsLinks,
}: RuntimePanelProps) {
  return (
    <Panel title="Runtime">
      <div className="grid gap-3 sm:grid-cols-2">
        {metricBlocks.map((metric) => (
          <MetricCard key={metric.label} {...metric} />
        ))}
      </div>

      <div className="mt-5 grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(16rem,0.9fr)]">
        <div>
          <div className="mb-3 text-[0.76rem] font-medium uppercase tracking-[0.18em] text-[hsl(var(--text-muted))]">
            Node hit rates
          </div>
          <div className="space-y-3">
            {nodeRates.length > 0 ? (
              nodeRates.map(([nodeId, rate]) => (
                <NodeRateRow key={nodeId} nodeId={nodeId} rate={rate} />
              ))
            ) : (
              <div className="rounded-[16px] bg-[hsl(var(--bg-elevated))] p-4 text-[0.92rem] text-[hsl(var(--text-muted))]">
                No node samples yet.
              </div>
            )}
          </div>
        </div>

        <div className="space-y-3">
          <div className="text-[0.76rem] font-medium uppercase tracking-[0.18em] text-[hsl(var(--text-muted))]">
            API docs
          </div>
          {docsLinks.map((link) => (
            <a
              key={link.label}
              href={link.href}
              target="_blank"
              rel="noreferrer"
              className="block rounded-[16px] bg-[hsl(var(--bg-elevated))] p-4 transition-colors duration-200 hover:bg-[hsl(var(--bg-subtle))]"
            >
              <div className="font-display text-[0.98rem] font-medium text-[hsl(var(--text-primary))]">
                {link.label}
              </div>
            </a>
          ))}
        </div>
      </div>
    </Panel>
  );
}
