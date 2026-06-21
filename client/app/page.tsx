"use client";

import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

type Suggestion = {
  query: string;
  count: number;
};

type SuggestionResponse = {
  prefix: string;
  suggestions: Suggestion[];
};

type TrendingResponse = {
  trending: Suggestion[];
};

type SearchResponse = {
  message: string;
};

type MetricsResponse = {
  suggestP95LatencyMs: number;
  overallCacheHitRate: number;
  perNodeCacheHitRates: Record<string, number>;
  dbReadCount: number;
  dbWriteCount: number;
  requestsToFlushesRatio: number;
};

type CacheDebugResponse = {
  nodeId: string;
  hit: boolean;
};

type Metric = {
  label: string;
  value: string;
  detail: string;
};

type CacheLookupResult = {
  nodeId: string;
  status: "Hit" | "Miss";
  prefix: string;
};

const DEFAULT_QUERY = "search typeahead architecture";

const commandShadowLight =
  "0 7px 15px rgba(0,0,0,0.29), 0 26px 26px rgba(0,0,0,0.26), 0 59px 36px rgba(0,0,0,0.15), 0 106px 42px rgba(0,0,0,0.04), 0 165px 46px rgba(0,0,0,0.01)";

const commandShadowDark =
  "0 7px 15px rgba(255,255,255,0.29), 0 26px 26px rgba(255,255,255,0.26), 0 59px 36px rgba(255,255,255,0.15), 0 106px 42px rgba(255,255,255,0.04), 0 165px 46px rgba(255,255,255,0.01)";

const panelLight = "#F5F5F5";
const panelDark = "#0F0F0F";
const cardLight = "#FFFFFF";
const cardDark = "#1F1F1F";
const canvasLight = "#FEFEFE";
const canvasDark = "#000000";
const textPrimaryLight = "#120E14";
const textPrimaryDark = "#FAFBFE";
const textMutedLight = "#6B6A6E";
const textMutedDark = "#94939A";

function formatCount(value: number) {
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
  if (value >= 1_000) return `${(value / 1_000).toFixed(1)}K`;
  return `${value}`;
}

function formatPercentage(ratio: number) {
  return `${(ratio * 100).toFixed(1)}%`;
}

function formatLatency(value: number) {
  return `${value.toFixed(value >= 10 ? 0 : 2)}ms`;
}

function buildMetricBlocks(metrics: MetricsResponse | null): Metric[] {
  if (!metrics) {
    return [
      {
        label: "p95 Latency",
        value: "—",
        detail: "Waiting for backend metrics",
      },
      {
        label: "Cache Hit Rate",
        value: "—",
        detail: "Waiting for backend metrics",
      },
      { label: "DB Reads", value: "—", detail: "Waiting for backend metrics" },
      {
        label: "Req / Flush",
        value: "—",
        detail: "Waiting for backend metrics",
      },
    ];
  }

  return [
    {
      label: "p95 Latency",
      value: formatLatency(metrics.suggestP95LatencyMs),
      detail: "Rolling suggest latency from the backend",
    },
    {
      label: "Cache Hit Rate",
      value: formatPercentage(metrics.overallCacheHitRate),
      detail: "Combined hit rate across logical cache nodes",
    },
    {
      label: "DB Reads",
      value: formatCount(metrics.dbReadCount),
      detail: "Repository reads tracked by the service layer",
    },
    {
      label: "Req / Flush",
      value: metrics.requestsToFlushesRatio.toFixed(1),
      detail: `Batching pressure, writes so far: ${formatCount(metrics.dbWriteCount)}`,
    },
  ];
}

async function fetchJson<T>(
  input: RequestInfo,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(input, {
    ...init,
    cache: "no-store",
    headers: {
      accept: "application/json",
      ...(init?.body ? { "content-type": "application/json" } : {}),
      ...init?.headers,
    },
  });

  const data = (await response.json().catch(() => null)) as {
    message?: string;
  } | null;

  if (!response.ok) {
    throw new Error(
      data?.message ?? `Request failed with status ${response.status}`,
    );
  }

  return data as T;
}

function Panel({
  title,
  subtitle,
  children,
  className = "",
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <section
      className={`rounded-[28px] p-5 sm:p-6 ${className}`}
      style={{ backgroundColor: "var(--panel-bg)" }}
    >
      <div className="mb-5">
        <h2
          className="text-[1.05rem] sm:text-[1.15rem] font-medium"
          style={{ fontFamily: "Gilroy, Inter, system-ui, sans-serif" }}
        >
          {title}
        </h2>
        {subtitle ? (
          <p
            className="mt-1 text-[0.92rem] leading-6"
            style={{
              color: "var(--text-muted)",
              fontFamily: "Inter, system-ui, sans-serif",
            }}
          >
            {subtitle}
          </p>
        ) : null}
      </div>
      {children}
    </section>
  );
}

function MetricBlock({ label, value, detail }: Metric) {
  return (
    <div
      className="rounded-[18px] p-4 sm:p-5"
      style={{ backgroundColor: "var(--card-bg)" }}
    >
      <div
        className="text-[0.8rem] font-medium uppercase tracking-[0.18em]"
        style={{
          color: "var(--text-muted)",
          fontFamily: "Inter, system-ui, sans-serif",
        }}
      >
        {label}
      </div>
      <div
        className="mt-2 text-[1.4rem] sm:text-[1.7rem] font-medium"
        style={{ fontFamily: "Gilroy, Inter, system-ui, sans-serif" }}
      >
        {value}
      </div>
      <div
        className="mt-2 text-[0.9rem] leading-6"
        style={{
          color: "var(--text-muted)",
          fontFamily: "Inter, system-ui, sans-serif",
        }}
      >
        {detail}
      </div>
    </div>
  );
}

export default function App() {
  const [isDark, setIsDark] = useState(false);
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);
  const [suggestionsError, setSuggestionsError] = useState<string | null>(null);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [submittedQuery, setSubmittedQuery] = useState(DEFAULT_QUERY);
  const [submissionMessage, setSubmissionMessage] = useState(
    "Submit a query to register it with the backend.",
  );
  const [submissionError, setSubmissionError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [trending, setTrending] = useState<Suggestion[]>([]);
  const [metrics, setMetrics] = useState<MetricsResponse | null>(null);
  const [dashboardError, setDashboardError] = useState<string | null>(null);
  const [isLoadingDashboard, setIsLoadingDashboard] = useState(true);
  const [backendOnline, setBackendOnline] = useState(true);
  const [cachePrefix, setCachePrefix] = useState("search");
  const [cacheLookupResult, setCacheLookupResult] = useState<CacheLookupResult>(
    {
      nodeId: "node-0",
      status: "Miss",
      prefix: "search",
    },
  );
  const [cacheError, setCacheError] = useState<string | null>(null);
  const [isResolvingCache, setIsResolvingCache] = useState(false);

  const inputRef = useRef<HTMLInputElement | null>(null);
  const listboxId = "search-typeahead-listbox";

  useEffect(() => {
    const handle = window.setTimeout(() => {
      setDebouncedQuery(query);
    }, 300);

    return () => window.clearTimeout(handle);
  }, [query]);

  const loadDashboard = useCallback(async () => {
    setIsLoadingDashboard(true);
    setDashboardError(null);

    const [trendingResult, metricsResult] = await Promise.allSettled([
      fetchJson<TrendingResponse>("/api/trending?limit=8"),
      fetchJson<MetricsResponse>("/api/metrics"),
    ]);

    let hadFailure = false;

    if (trendingResult.status === "fulfilled") {
      setTrending(trendingResult.value.trending);
    } else {
      hadFailure = true;
    }

    if (metricsResult.status === "fulfilled") {
      setMetrics(metricsResult.value);
    } else {
      hadFailure = true;
    }

    setBackendOnline(!hadFailure);
    if (hadFailure) {
      setDashboardError(
        "Live dashboard data is unavailable. Start the Spring server to populate this view.",
      );
    }

    setIsLoadingDashboard(false);
  }, []);

  const resolveCacheNode = useCallback(
    async (prefixValue?: string) => {
      const prefix = (prefixValue ?? cachePrefix).trim();
      if (!prefix) {
        setCacheLookupResult({
          nodeId: "node-0",
          status: "Miss",
          prefix: "empty",
        });
        setCacheError(null);
        return;
      }

      setIsResolvingCache(true);
      setCacheError(null);

      try {
        const params = new URLSearchParams({ prefix, limit: "10" });
        const result = await fetchJson<CacheDebugResponse>(
          `/api/cache-debug?${params.toString()}`,
        );
        setCacheLookupResult({
          nodeId: result.nodeId,
          status: result.hit ? "Hit" : "Miss",
          prefix,
        });
        setBackendOnline(true);
      } catch (error) {
        setCacheError(
          error instanceof Error ? error.message : "Cache lookup failed.",
        );
        setBackendOnline(false);
      } finally {
        setIsResolvingCache(false);
      }
    },
    [cachePrefix],
  );

  useEffect(() => {
    const handle = window.setTimeout(() => {
      void loadDashboard();
      void resolveCacheNode("search");
    }, 0);

    return () => window.clearTimeout(handle);
  }, [loadDashboard, resolveCacheNode]);

  useEffect(() => {
    const normalized = debouncedQuery.trim();
    if (!normalized) {
      return;
    }

    const controller = new AbortController();

    void fetchJson<SuggestionResponse>(
      `/api/suggest?${new URLSearchParams({ q: normalized, limit: "10" }).toString()}`,
      { signal: controller.signal },
    )
      .then((response) => {
        setSuggestions(response.suggestions);
        setBackendOnline(true);
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        setSuggestions([]);
        setSuggestionsError(
          error instanceof Error
            ? error.message
            : "Suggestions are unavailable right now.",
        );
        setBackendOnline(false);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setIsLoadingSuggestions(false);
        }
      });

    return () => controller.abort();
  }, [debouncedQuery]);

  const handleSubmit = useCallback(
    async (value: string) => {
      const finalQuery = value.trim();
      if (!finalQuery) {
        return;
      }

      setIsSubmitting(true);
      setSubmissionError(null);

      try {
        const response = await fetchJson<SearchResponse>("/api/search", {
          method: "POST",
          body: JSON.stringify({ query: finalQuery }),
        });

        setSubmittedQuery(finalQuery);
        setQuery(finalQuery);
        setDebouncedQuery(finalQuery);
        setSubmissionMessage(response.message);
        setBackendOnline(true);
        await loadDashboard();
      } catch (error) {
        setSubmissionError(
          error instanceof Error ? error.message : "Search submission failed.",
        );
        setBackendOnline(false);
      } finally {
        setActiveIndex(-1);
        setIsSubmitting(false);
      }
    },
    [loadDashboard],
  );

  const handleSuggestionPick = useCallback(
    (suggestion: Suggestion) => {
      void handleSubmit(suggestion.query);
      inputRef.current?.focus();
    },
    [handleSubmit],
  );

  const onSearchKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (visibleSuggestions.length === 0 && event.key !== "Enter") {
      return;
    }

    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveIndex((current) => {
        const next = current + 1;
        return next >= visibleSuggestions.length ? 0 : next;
      });
      return;
    }

    if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex((current) => {
        const next = current - 1;
        return next < 0 ? visibleSuggestions.length - 1 : next;
      });
      return;
    }

    if (event.key === "Enter") {
      event.preventDefault();
      const chosen = visibleSuggestions[activeIndex];
      void handleSubmit(chosen?.query ?? query);
      return;
    }

    if (event.key === "Escape") {
      setActiveIndex(-1);
      inputRef.current?.blur();
    }
  };

  const metricBlocks = useMemo(() => buildMetricBlocks(metrics), [metrics]);
  const hasActiveQuery = debouncedQuery.trim().length > 0;
  const visibleSuggestions = hasActiveQuery ? suggestions : [];
  const visibleSuggestionsError = hasActiveQuery ? suggestionsError : null;
  const nodeRates = useMemo(
    () =>
      Object.entries(metrics?.perNodeCacheHitRates ?? {}).sort(
        ([left], [right]) => left.localeCompare(right),
      ),
    [metrics],
  );

  const panelBg = isDark ? panelDark : panelLight;
  const cardBg = isDark ? cardDark : cardLight;
  const canvasBg = isDark ? canvasDark : canvasLight;
  const textPrimary = isDark ? textPrimaryDark : textPrimaryLight;
  const textMuted = isDark ? textMutedDark : textMutedLight;

  return (
    <div
      className={isDark ? "dark" : ""}
      style={
        {
          "--panel-bg": panelBg,
          "--card-bg": cardBg,
          "--canvas-bg": canvasBg,
          "--text-primary": textPrimary,
          "--text-muted": textMuted,
        } as React.CSSProperties
      }
    >
      <main
        className="min-h-screen px-4 py-4 sm:px-6 sm:py-6 lg:px-8"
        style={{
          backgroundColor: "var(--canvas-bg)",
          color: "var(--text-primary)",
          fontFamily: "Inter, system-ui, sans-serif",
        }}
      >
        <div className="mx-auto flex min-h-[calc(100vh-2rem)] w-full max-w-7xl flex-col gap-5 sm:gap-6">
          <header className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div className="max-w-3xl">
              <p
                className="text-[0.76rem] font-medium uppercase tracking-[0.24em]"
                style={{ color: "var(--text-muted)" }}
              >
                Search Typeahead
              </p>
              <h1
                className="mt-2 text-[1.55rem] sm:text-[2rem] lg:text-[2.4rem] font-medium"
                style={{ fontFamily: "Gilroy, Inter, system-ui, sans-serif" }}
              >
                Frontend wired to the live suggestion, search, metrics, and
                cache APIs.
              </h1>
            </div>

            <div className="flex items-center gap-3 self-start">
              <div
                className="rounded-full px-4 py-2 text-[0.85rem] font-medium"
                style={{
                  backgroundColor: isDark ? "#1F1F1F" : "#FFFFFF",
                  color: backendOnline ? "hsl(142 70% 45%)" : "hsl(0 72% 58%)",
                  fontFamily: "Inter, system-ui, sans-serif",
                }}
              >
                {backendOnline ? "Backend connected" : "Backend unavailable"}
              </div>

              <button
                type="button"
                onClick={() => setIsDark((value) => !value)}
                className="rounded-full px-4 py-2 text-[0.9rem] font-medium transition-transform duration-200 hover:-translate-y-0.5"
                style={{
                  backgroundColor: isDark ? "#FAFBFE" : "#171717",
                  color: isDark ? "#120E14" : "#FAFBFE",
                  boxShadow: isDark ? commandShadowDark : commandShadowLight,
                  fontFamily: "Inter, system-ui, sans-serif",
                }}
              >
                {isDark ? "Light mode" : "Dark mode"}
              </button>
            </div>
          </header>

          <section
            className="rounded-[34px] p-5 sm:p-6 lg:p-8"
            style={{
              background: isDark
                ? "radial-gradient(1200px circle at 50% 0%, rgba(255,255,255,0.045), rgba(0,0,0,0) 45%), #0F0F0F"
                : "radial-gradient(1200px circle at 50% 0%, rgba(255,255,255,0.12), rgba(0,0,0,0.72) 46%, rgba(0,0,0,0.94) 100%)",
              color: "#FAFBFE",
            }}
          >
            <div className="mx-auto flex max-w-4xl flex-col gap-6">
              <div className="space-y-3">
                <p
                  className="text-[0.78rem] font-medium uppercase tracking-[0.22em] text-white/70"
                  style={{ fontFamily: "Inter, system-ui, sans-serif" }}
                >
                  Live query surface
                </p>
                <h2
                  className="max-w-2xl text-[1.6rem] sm:text-[2.1rem] lg:text-[2.8rem] font-medium"
                  style={{ fontFamily: "Gilroy, Inter, system-ui, sans-serif" }}
                >
                  Query the backend directly, submit searches, and inspect the
                  system state without leaving the page.
                </h2>
              </div>

              <div className="relative">
                <div
                  className="flex flex-col gap-3 rounded-full p-2 sm:flex-row sm:items-center"
                  style={{
                    backgroundColor: isDark
                      ? "#1F1F1F"
                      : "rgba(255,255,255,0.08)",
                    backdropFilter: "blur(12px)",
                    border: "1px solid rgba(255,255,255,0.08)",
                  }}
                >
                  <div className="relative flex-1">
                    <input
                      ref={inputRef}
                      value={query}
                      onChange={(event) => {
                        const nextValue = event.target.value;
                        setQuery(nextValue);
                        setActiveIndex(-1);
                        setSuggestionsError(null);
                        setIsLoadingSuggestions(nextValue.trim().length > 0);
                      }}
                      onKeyDown={onSearchKeyDown}
                      placeholder="Type a prefix to load live suggestions."
                      aria-autocomplete="list"
                      aria-controls={listboxId}
                      aria-activedescendant={
                        activeIndex >= 0
                          ? `${listboxId}-option-${activeIndex}`
                          : undefined
                      }
                      className="h-14 w-full rounded-full bg-transparent px-5 pr-6 text-[1rem] outline-none placeholder:text-white/50 sm:h-16 sm:text-[1.02rem]"
                      style={{
                        color: "#FAFBFE",
                        fontFamily: "Gilroy, Inter, system-ui, sans-serif",
                      }}
                    />
                    <div className="pointer-events-none absolute inset-y-0 right-4 flex items-center text-white/35">
                      <span className="text-sm">⌘</span>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => void handleSubmit(query)}
                    disabled={isSubmitting}
                    className="inline-flex h-14 items-center justify-center rounded-full px-7 text-[0.95rem] font-medium transition-transform duration-200 hover:-translate-y-0.5 active:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60 sm:h-16"
                    style={{
                      backgroundColor: isDark ? "#FAFBFE" : "#171717",
                      color: isDark ? "#120E14" : "#FAFBFE",
                      boxShadow: isDark
                        ? commandShadowDark
                        : commandShadowLight,
                      fontFamily: "Gilroy, Inter, system-ui, sans-serif",
                    }}
                  >
                    {isSubmitting ? "Submitting..." : "Search"}
                  </button>
                </div>

                <div
                  className="absolute left-0 right-0 top-[calc(100%+0.75rem)] z-20 overflow-hidden rounded-[28px]"
                  style={{
                    backgroundColor: isDark ? "#1F1F1F" : "#FFFFFF",
                    boxShadow:
                      "0 16px 44px rgba(0,0,0,0.10), 0 62px 88px rgba(0,0,0,0.12)",
                  }}
                >
                  <div className="flex items-center justify-between px-5 pt-4">
                    <p
                      className="text-[0.78rem] font-medium uppercase tracking-[0.18em]"
                      style={{ color: "var(--text-muted)" }}
                    >
                      Suggestions
                    </p>
                    <div
                      className="text-[0.82rem]"
                      style={{
                        color: "var(--text-muted)",
                        fontFamily: "Inter, system-ui, sans-serif",
                      }}
                    >
                      {isLoadingSuggestions
                        ? "Loading..."
                        : `${visibleSuggestions.length} results`}
                    </div>
                  </div>

                  <div className="p-3">
                    {visibleSuggestionsError ? (
                      <div
                        className="rounded-[22px] px-4 py-10 text-center"
                        style={{
                          backgroundColor: isDark ? "#1F1F1F" : "#FFFFFF",
                        }}
                      >
                        <div
                          className="text-[0.95rem] font-medium"
                          style={{
                            fontFamily: "Gilroy, Inter, system-ui, sans-serif",
                          }}
                        >
                          Suggestions unavailable
                        </div>
                        <div
                          className="mt-2 text-[0.9rem]"
                          style={{ color: "var(--text-muted)" }}
                        >
                          {visibleSuggestionsError}
                        </div>
                      </div>
                    ) : visibleSuggestions.length > 0 ? (
                      <div
                        id={listboxId}
                        role="listbox"
                        aria-label="Search suggestions"
                        className="space-y-2"
                      >
                        {visibleSuggestions.map((suggestion, index) => {
                          const selected = index === activeIndex;

                          return (
                            <button
                              key={`${suggestion.query}-${index}`}
                              id={`${listboxId}-option-${index}`}
                              role="option"
                              aria-selected={selected}
                              type="button"
                              onMouseEnter={() => setActiveIndex(index)}
                              onClick={() => handleSuggestionPick(suggestion)}
                              className="flex w-full items-center justify-between rounded-[18px] px-4 py-3 text-left transition-colors duration-150"
                              style={{
                                backgroundColor: selected
                                  ? isDark
                                    ? "#2A2A2A"
                                    : "#F2F2F2"
                                  : isDark
                                    ? "#1F1F1F"
                                    : "#FFFFFF",
                                color: "var(--text-primary)",
                              }}
                            >
                              <div className="min-w-0">
                                <div
                                  className="truncate text-[0.96rem] font-medium"
                                  style={{
                                    fontFamily:
                                      "Gilroy, Inter, system-ui, sans-serif",
                                  }}
                                >
                                  {suggestion.query}
                                </div>
                                <div
                                  className="mt-1 text-[0.8rem]"
                                  style={{ color: "var(--text-muted)" }}
                                >
                                  Live prefix match from `/suggest`
                                </div>
                              </div>

                              <div
                                className="ml-4 shrink-0 rounded-full px-3 py-1 text-[0.8rem] font-medium"
                                style={{
                                  backgroundColor: isDark
                                    ? "#0F0F0F"
                                    : "#F5F5F5",
                                  color: "var(--text-muted)",
                                  fontFamily: "Inter, system-ui, sans-serif",
                                }}
                              >
                                {formatCount(suggestion.count)}
                              </div>
                            </button>
                          );
                        })}
                      </div>
                    ) : (
                      <div
                        className="rounded-[22px] px-4 py-10 text-center"
                        style={{
                          backgroundColor: isDark ? "#1F1F1F" : "#FFFFFF",
                        }}
                      >
                        <div
                          className="text-[0.95rem] font-medium"
                          style={{
                            fontFamily: "Gilroy, Inter, system-ui, sans-serif",
                          }}
                        >
                          Start typing to fetch live suggestions.
                        </div>
                        <div
                          className="mt-2 text-[0.9rem]"
                          style={{ color: "var(--text-muted)" }}
                        >
                          The UI debounces input for 300ms before calling the
                          backend.
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-3 pt-16">
                <div
                  className="rounded-full px-4 py-3 text-[0.9rem]"
                  style={{
                    backgroundColor: isDark
                      ? "rgba(255,255,255,0.06)"
                      : "rgba(255,255,255,0.12)",
                    color: "#FAFBFE",
                    fontFamily: "Inter, system-ui, sans-serif",
                  }}
                >
                  Last submitted:{" "}
                  <span className="font-medium">{submittedQuery}</span>
                </div>

                <div
                  className="rounded-full px-4 py-3 text-[0.9rem]"
                  style={{
                    backgroundColor: isDark
                      ? "rgba(255,255,255,0.06)"
                      : "rgba(255,255,255,0.12)",
                    color: "#FAFBFE",
                    fontFamily: "Inter, system-ui, sans-serif",
                  }}
                >
                  {submissionError ?? submissionMessage}
                </div>
              </div>
            </div>
          </section>

          <div className="grid gap-5 lg:grid-cols-12">
            <div className="lg:col-span-5">
              <Panel
                title="Trending Searches"
                subtitle="Live `/trending` results from the recency-aware ranking strategy."
              >
                {dashboardError && trending.length === 0 ? (
                  <div
                    className="rounded-[18px] p-4 text-[0.92rem]"
                    style={{
                      backgroundColor: isDark ? "#1F1F1F" : "#FFFFFF",
                      color: "var(--text-muted)",
                    }}
                  >
                    {dashboardError}
                  </div>
                ) : isLoadingDashboard && trending.length === 0 ? (
                  <div
                    className="rounded-[18px] p-4 text-[0.92rem]"
                    style={{
                      backgroundColor: isDark ? "#1F1F1F" : "#FFFFFF",
                      color: "var(--text-muted)",
                    }}
                  >
                    Loading trending queries...
                  </div>
                ) : (
                  <div className="flex flex-wrap gap-3">
                    {trending.map((item) => (
                      <button
                        key={item.query}
                        type="button"
                        onClick={() => void handleSubmit(item.query)}
                        className="inline-flex items-center gap-2 rounded-full px-4 py-2 text-[0.9rem] transition-transform duration-200 hover:-translate-y-0.5"
                        style={{
                          backgroundColor: isDark ? "#1F1F1F" : "#FFFFFF",
                          color: "var(--text-primary)",
                          fontFamily: "Inter, system-ui, sans-serif",
                        }}
                      >
                        <span
                          className="inline-block h-2 w-2 rounded-full"
                          style={{
                            backgroundColor: isDark ? "#C8C8C8" : "#4A4A4A",
                          }}
                        />
                        <span className="truncate">{item.query}</span>
                        <span style={{ color: "var(--text-muted)" }}>
                          {formatCount(item.count)}
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </Panel>
            </div>

            <div className="lg:col-span-7">
              <Panel
                title="System Diagnostics & Metrics"
                subtitle="Live backend telemetry and cache node inspection."
              >
                <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                  {metricBlocks.map((metric) => (
                    <MetricBlock key={metric.label} {...metric} />
                  ))}
                </div>

                <div className="mt-4 grid gap-3 lg:grid-cols-2">
                  <div
                    className="rounded-[22px] p-4 sm:p-5"
                    style={{ backgroundColor: isDark ? "#1F1F1F" : "#FFFFFF" }}
                  >
                    <p
                      className="text-[0.8rem] font-medium uppercase tracking-[0.18em]"
                      style={{ color: "var(--text-muted)" }}
                    >
                      Cache debugger
                    </p>
                    <h3
                      className="mt-2 text-[1.05rem] font-medium"
                      style={{
                        fontFamily: "Gilroy, Inter, system-ui, sans-serif",
                      }}
                    >
                      Resolve the cache node for a prefix.
                    </h3>
                    <p
                      className="mt-1 text-[0.92rem]"
                      style={{ color: "var(--text-muted)" }}
                    >
                      Calls `/cache/debug` through the Next proxy and reports
                      the routed node plus last hit status.
                    </p>

                    <div className="mt-4 flex w-full flex-col gap-3 sm:flex-row">
                      <input
                        value={cachePrefix}
                        onChange={(event) => setCachePrefix(event.target.value)}
                        placeholder="Prefix, e.g. search"
                        className="h-12 w-full rounded-full px-4 text-[0.95rem] outline-none"
                        style={{
                          backgroundColor: isDark ? "#0F0F0F" : "#F5F5F5",
                          color: "var(--text-primary)",
                          fontFamily: "Inter, system-ui, sans-serif",
                        }}
                      />
                      <button
                        type="button"
                        onClick={() => void resolveCacheNode()}
                        disabled={isResolvingCache}
                        className="h-12 rounded-full px-5 text-[0.92rem] font-medium transition-transform duration-200 hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60"
                        style={{
                          backgroundColor: isDark ? "#FAFBFE" : "#171717",
                          color: isDark ? "#120E14" : "#FAFBFE",
                          boxShadow: isDark
                            ? commandShadowDark
                            : commandShadowLight,
                          fontFamily: "Gilroy, Inter, system-ui, sans-serif",
                        }}
                      >
                        {isResolvingCache ? "Resolving..." : "Resolve node"}
                      </button>
                    </div>

                    {cacheError ? (
                      <div
                        className="mt-4 rounded-[18px] p-4 text-[0.9rem]"
                        style={{
                          backgroundColor: isDark ? "#0F0F0F" : "#F5F5F5",
                          color: "var(--text-muted)",
                        }}
                      >
                        {cacheError}
                      </div>
                    ) : (
                      <div className="mt-4 grid gap-3 sm:grid-cols-3">
                        <div
                          className="rounded-[18px] p-4"
                          style={{
                            backgroundColor: isDark ? "#0F0F0F" : "#F5F5F5",
                          }}
                        >
                          <div
                            className="text-[0.78rem] uppercase tracking-[0.16em]"
                            style={{ color: "var(--text-muted)" }}
                          >
                            Prefix
                          </div>
                          <div
                            className="mt-2 text-[0.95rem] font-medium"
                            style={{
                              fontFamily:
                                "Gilroy, Inter, system-ui, sans-serif",
                            }}
                          >
                            {cacheLookupResult.prefix}
                          </div>
                        </div>

                        <div
                          className="rounded-[18px] p-4"
                          style={{
                            backgroundColor: isDark ? "#0F0F0F" : "#F5F5F5",
                          }}
                        >
                          <div
                            className="text-[0.78rem] uppercase tracking-[0.16em]"
                            style={{ color: "var(--text-muted)" }}
                          >
                            Cache Node ID
                          </div>
                          <div
                            className="mt-2 text-[0.95rem] font-medium"
                            style={{
                              fontFamily:
                                "Gilroy, Inter, system-ui, sans-serif",
                            }}
                          >
                            {cacheLookupResult.nodeId}
                          </div>
                        </div>

                        <div
                          className="rounded-[18px] p-4"
                          style={{
                            backgroundColor: isDark ? "#0F0F0F" : "#F5F5F5",
                          }}
                        >
                          <div
                            className="text-[0.78rem] uppercase tracking-[0.16em]"
                            style={{ color: "var(--text-muted)" }}
                          >
                            Last lookup
                          </div>
                          <div
                            className="mt-2 flex items-center gap-2 text-[0.95rem] font-medium"
                            style={{
                              fontFamily:
                                "Gilroy, Inter, system-ui, sans-serif",
                            }}
                          >
                            <span
                              className="inline-block h-2.5 w-2.5 rounded-full"
                              style={{
                                backgroundColor:
                                  cacheLookupResult.status === "Hit"
                                    ? "hsl(142 70% 52%)"
                                    : "hsl(0 91% 71%)",
                              }}
                            />
                            {cacheLookupResult.status}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>

                  <div
                    className="rounded-[22px] p-4 sm:p-5"
                    style={{ backgroundColor: isDark ? "#1F1F1F" : "#FFFFFF" }}
                  >
                    <p
                      className="text-[0.8rem] font-medium uppercase tracking-[0.18em]"
                      style={{ color: "var(--text-muted)" }}
                    >
                      Per-node hit rates
                    </p>
                    <h3
                      className="mt-2 text-[1.05rem] font-medium"
                      style={{
                        fontFamily: "Gilroy, Inter, system-ui, sans-serif",
                      }}
                    >
                      Logical cache node breakdown
                    </h3>
                    <p
                      className="mt-1 text-[0.92rem]"
                      style={{ color: "var(--text-muted)" }}
                    >
                      Derived from `/metrics` to show how consistent hashing is
                      distributing hot prefixes.
                    </p>

                    <div className="mt-4 space-y-3">
                      {nodeRates.length > 0 ? (
                        nodeRates.map(([nodeId, rate]) => (
                          <div
                            key={nodeId}
                            className="flex items-center justify-between rounded-[18px] px-4 py-3"
                            style={{
                              backgroundColor: isDark ? "#0F0F0F" : "#F5F5F5",
                            }}
                          >
                            <span
                              className="text-[0.92rem] font-medium"
                              style={{
                                fontFamily:
                                  "Gilroy, Inter, system-ui, sans-serif",
                              }}
                            >
                              {nodeId}
                            </span>
                            <span
                              className="text-[0.9rem]"
                              style={{ color: "var(--text-muted)" }}
                            >
                              {formatPercentage(rate)}
                            </span>
                          </div>
                        ))
                      ) : (
                        <div
                          className="rounded-[18px] p-4 text-[0.92rem]"
                          style={{
                            backgroundColor: isDark ? "#0F0F0F" : "#F5F5F5",
                            color: "var(--text-muted)",
                          }}
                        >
                          No per-node samples yet. Issue some `/suggest`
                          requests to warm the cache.
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </Panel>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
