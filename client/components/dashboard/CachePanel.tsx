import type { CacheLookupResult } from "@/components/dashboard/types";
import { Panel } from "@/components/dashboard/Panel";
import { commandShadowLight } from "@/components/dashboard/utils";

type CachePanelProps = {
  cachePrefix: string;
  onCachePrefixChange: (value: string) => void;
  onResolve: () => void;
  isResolvingCache: boolean;
  cacheError: string | null;
  cacheLookupResult: CacheLookupResult;
  cacheStateColor: string;
};

export function CachePanel({
  cachePrefix,
  onCachePrefixChange,
  onResolve,
  isResolvingCache,
  cacheError,
  cacheLookupResult,
  cacheStateColor,
}: CachePanelProps) {
  return (
    <Panel title="Cache">
      <div className="flex flex-col gap-3 sm:flex-row">
        <input
          value={cachePrefix}
          onChange={(event) => onCachePrefixChange(event.target.value)}
          placeholder="Prefix, e.g. search"
          className="h-12 flex-1 rounded-full bg-[hsl(var(--bg-elevated))] px-4 text-[0.94rem] text-[hsl(var(--text-primary))] outline-none placeholder:text-[hsl(var(--text-faint))]"
        />
        <button
          type="button"
          onClick={onResolve}
          disabled={isResolvingCache}
          className="inline-flex h-12 items-center justify-center rounded-full px-5 text-[0.92rem] font-medium transition-transform duration-200 hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60"
          style={{
            backgroundColor: "#171717",
            color: "#FAFBFE",
            boxShadow: commandShadowLight,
          }}
        >
          {isResolvingCache ? "Resolving..." : "Resolve node"}
        </button>
      </div>

      {cacheError ? (
        <div className="mt-4 rounded-[16px] bg-[hsl(var(--bg-elevated))] p-4 text-[0.92rem] text-[hsl(var(--text-muted))]">
          {cacheError}
        </div>
      ) : (
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <div className="rounded-[16px] bg-[hsl(var(--bg-elevated))] p-4">
            <div className="text-[0.76rem] uppercase tracking-[0.18em] text-[hsl(var(--text-muted))]">
              Prefix
            </div>
            <div className="mt-2 font-display text-[0.98rem] font-medium text-[hsl(var(--text-primary))]">
              {cacheLookupResult.prefix}
            </div>
          </div>

          <div className="rounded-[16px] bg-[hsl(var(--bg-elevated))] p-4">
            <div className="text-[0.76rem] uppercase tracking-[0.18em] text-[hsl(var(--text-muted))]">
              Node
            </div>
            <div className="mt-2 font-display text-[0.98rem] font-medium text-[hsl(var(--text-primary))]">
              {cacheLookupResult.nodeId}
            </div>
          </div>

          <div className="rounded-[16px] bg-[hsl(var(--bg-elevated))] p-4">
            <div className="text-[0.76rem] uppercase tracking-[0.18em] text-[hsl(var(--text-muted))]">
              Last lookup
            </div>
            <div className="mt-2 flex items-center gap-2 text-[0.95rem] text-[hsl(var(--text-primary))]">
              <span
                className="inline-block h-2.5 w-2.5 rounded-full"
                style={{ backgroundColor: cacheStateColor }}
              />
              <span className="font-display font-medium">
                {cacheLookupResult.status}
              </span>
            </div>
          </div>
        </div>
      )}
    </Panel>
  );
}
