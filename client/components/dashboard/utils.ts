import type { CSSProperties } from "react";

import type { Metric, MetricsResponse, PanelTone } from "@/components/dashboard/types";

export const commandShadowLight =
  "0 7px 15px rgba(0,0,0,0.29), 0 26px 26px rgba(0,0,0,0.26), 0 59px 36px rgba(0,0,0,0.15), 0 106px 42px rgba(0,0,0,0.04), 0 165px 46px rgba(0,0,0,0.01)";

export const successColor = "hsl(142 71% 45%)";
export const errorColor = "hsl(0 84% 60%)";

export function formatCount(value: number) {
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
  if (value >= 1_000) return `${(value / 1_000).toFixed(1)}K`;
  return `${value}`;
}

export function formatPercentage(ratio: number) {
  return `${(ratio * 100).toFixed(1)}%`;
}

export function formatLatency(value: number) {
  return `${value.toFixed(value >= 10 ? 0 : 2)}ms`;
}

export function buildMetricBlocks(metrics: MetricsResponse | null): Metric[] {
  if (!metrics) {
    return [
      { label: "p95 Latency", value: "Pending" },
      { label: "Cache Hit Rate", value: "Pending" },
      { label: "DB Reads", value: "Pending" },
      { label: "Req / Flush", value: "Pending" },
    ];
  }

  return [
    {
      label: "p95 Latency",
      value: formatLatency(metrics.suggestP95LatencyMs),
    },
    {
      label: "Cache Hit Rate",
      value: formatPercentage(metrics.overallCacheHitRate),
    },
    {
      label: "DB Reads",
      value: formatCount(metrics.dbReadCount),
    },
    {
      label: "Req / Flush",
      value: metrics.requestsToFlushesRatio.toFixed(1),
    },
  ];
}

export function getSurfaceStyle(tone: PanelTone): CSSProperties {
  switch (tone) {
    case "elevated":
      return { backgroundColor: "hsl(var(--bg-elevated))" };
    case "glass":
      return {
        backgroundColor: "var(--glass-bg)",
        border: "1px solid var(--glass-border)",
        backdropFilter: "blur(12px)",
      };
    case "inverted":
      return { backgroundColor: "hsl(var(--bg-inverted))" };
    default:
      return { backgroundColor: "hsl(var(--bg-muted))" };
  }
}
