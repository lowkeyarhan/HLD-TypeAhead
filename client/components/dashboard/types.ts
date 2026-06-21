export type Suggestion = {
  query: string;
  count: number;
};

export type SuggestionResponse = {
  prefix: string;
  suggestions: Suggestion[];
};

export type TrendingResponse = {
  trending: Suggestion[];
};

export type SearchResponse = {
  message: string;
};

export type MetricsResponse = {
  suggestP95LatencyMs: number;
  overallCacheHitRate: number;
  perNodeCacheHitRates: Record<string, number>;
  dbReadCount: number;
  dbWriteCount: number;
  requestsToFlushesRatio: number;
};

export type CacheDebugResponse = {
  nodeId: string;
  hit: boolean;
};

export type Metric = {
  label: string;
  value: string;
};

export type CacheLookupResult = {
  nodeId: string;
  status: "Hit" | "Miss";
  prefix: string;
};

export type DocsLink = {
  label: string;
  href: string;
};

export type PanelTone = "muted" | "elevated" | "glass" | "inverted";
