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
};

type CacheLookupResult = {
  nodeId: string;
  status: "Hit" | "Miss";
  prefix: string;
};

const DEFAULT_QUERY = "search typeahead architecture";

const commandShadowLight =
  "0 7px 15px rgba(0,0,0,0.29), 0 26px 26px rgba(0,0,0,0.26), 0 59px 36px rgba(0,0,0,0.15), 0 106px 42px rgba(0,0,0,0.04), 0 165px 46px rgba(0,0,0,0.01)";

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
        value: "Pending",
      },
      {
        label: "Cache Hit Rate",
        value: "Pending",
      },
      {
        label: "DB Reads",
        value: "Pending",
      },
      {
        label: "Req / Flush",
        value: "Pending",
      },
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

function surfaceStyle(tone: "muted" | "elevated" | "glass" | "inverted") {
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

function Panel({
  title,
  subtitle,
  tone = "muted",
  children,
  className = "",
}: {
  title: string;
  subtitle?: string;
  tone?: "muted" | "elevated" | "glass" | "inverted";
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <section
      className={`rounded-[24px] p-5 sm:p-6 ${className}`}
      style={surfaceStyle(tone)}
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

function SuggestionRow({
  suggestion,
  selected,
  onMouseEnter,
  onClick,
}: {
  suggestion: Suggestion;
  selected: boolean;
  onMouseEnter: () => void;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      role="option"
      aria-selected={selected}
      onMouseEnter={onMouseEnter}
      onClick={onClick}
      className="flex w-full items-center justify-between rounded-[16px] px-4 py-3 text-left transition-colors duration-200"
      style={{
        backgroundColor: selected ? "#FFFFFF" : "transparent",
      }}
    >
      <div className="min-w-0">
        <div
          className="truncate font-display text-[0.98rem] font-medium"
          style={{
            color: selected
              ? "hsl(var(--text-primary))"
              : "hsl(var(--text-inverted))",
          }}
        >
          {suggestion.query}
        </div>
      </div>
      <div
        className="ml-4 rounded-full px-3 py-1 text-[0.8rem]"
        style={{
          backgroundColor: selected
            ? "hsl(var(--bg-subtle))"
            : "rgba(255,255,255,0.08)",
          color: selected
            ? "hsl(var(--text-secondary))"
            : "hsl(var(--text-muted-inverted))",
        }}
      >
        {formatCount(suggestion.count)}
      </div>
    </button>
  );
}

export default function App() {
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);
  const [suggestionsError, setSuggestionsError] = useState<string | null>(null);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [submittedQuery, setSubmittedQuery] = useState(DEFAULT_QUERY);
  const [submissionMessage, setSubmissionMessage] = useState("Ready.");
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
        const result = await fetchJson<CacheDebugResponse>(
          `/api/cache-debug?${new URLSearchParams({ prefix, limit: "10" }).toString()}`,
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
        await fetchJson<SearchResponse>("/api/search", {
          method: "POST",
          body: JSON.stringify({ query: finalQuery }),
        });

        setSubmittedQuery(finalQuery);
        setQuery(finalQuery);
        setDebouncedQuery(finalQuery);
        setSubmissionMessage("Query submitted.");
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

  const hasActiveQuery = debouncedQuery.trim().length > 0;
  const visibleSuggestions = hasActiveQuery ? suggestions : [];
  const visibleSuggestionsError = hasActiveQuery ? suggestionsError : null;

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

  const nodeRates = useMemo(
    () =>
      Object.entries(metrics?.perNodeCacheHitRates ?? {}).sort(
        ([left], [right]) => left.localeCompare(right),
      ),
    [metrics],
  );

  const backendStateColor = backendOnline
    ? "hsl(142 71% 45%)"
    : "hsl(0 84% 60%)";

  const cacheStateColor =
    cacheLookupResult.status === "Hit" ? "hsl(142 71% 45%)" : "hsl(0 84% 60%)";

  return (
    <div>
      <main className="min-h-screen bg-[hsl(var(--bg-base))] px-4 py-4 text-[hsl(var(--text-primary))] sm:px-6 sm:py-6 lg:px-8">
        <div className="mx-auto flex min-h-[calc(100vh-2rem)] w-full max-w-[1200px] flex-col gap-6">
          <header className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div className="max-w-2xl">
              <p className="text-[0.78rem] font-medium uppercase tracking-[0.22em] text-[hsl(var(--text-muted))]">
                TypeAhead
              </p>
              <h1 className="mt-3 max-w-2xl text-balance font-display text-[1.5rem] font-medium leading-[1.04] text-[hsl(var(--text-primary))] sm:text-[1.95rem] lg:text-[2.5rem]">
                Search suggestions with live results and simple runtime insight.
              </h1>
              <p className="mt-3 max-w-[52ch] text-pretty text-[0.92rem] leading-6 text-[hsl(var(--text-muted))]">
                Search, submit, and inspect the system without extra noise.
              </p>
            </div>

            <div className="flex items-center gap-3 self-start">
              <div className="inline-flex items-center gap-2 rounded-full bg-[hsl(var(--bg-elevated))] px-4 py-2 text-[0.88rem] text-[hsl(var(--text-secondary))]">
                <span
                  className="inline-block h-2.5 w-2.5 rounded-full"
                  style={{ backgroundColor: backendStateColor }}
                />
                {backendOnline ? "Backend connected" : "Backend unavailable"}
              </div>
            </div>
          </header>

          <section
            className="rounded-[40px] p-5 text-[hsl(var(--text-inverted))] sm:p-6 lg:p-8"
            style={{
              background:
                "radial-gradient(1100px circle at 50% 0%, rgba(255,255,255,0.10), rgba(0,0,0,0) 28%), #0E1014",
            }}
          >
            <div className="max-w-4xl space-y-5">
              <div className="space-y-5">
                <div>
                  <p className="text-[0.76rem] font-medium uppercase tracking-[0.2em] text-[hsl(var(--text-muted-inverted))]">
                    Command surface
                  </p>
                  <h2 className="mt-3 max-w-2xl text-balance font-display text-[1.35rem] font-medium leading-[1.06] text-[hsl(var(--text-inverted))] sm:text-[1.75rem] lg:text-[2.2rem]">
                    Search with fast feedback.
                  </h2>
                </div>

                <div
                  className="rounded-[28px] p-2"
                  style={surfaceStyle("glass")}
                >
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                    <label className="sr-only" htmlFor="typeahead-query">
                      Search query
                    </label>
                    <input
                      id="typeahead-query"
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
                      className="h-14 flex-1 rounded-full bg-transparent px-5 text-[0.98rem] text-[hsl(var(--text-inverted))] outline-none placeholder:text-[hsl(var(--text-faint-inverted))] sm:h-16"
                    />

                    <button
                      type="button"
                      onClick={() => void handleSubmit(query)}
                      disabled={isSubmitting}
                      className="inline-flex h-14 items-center justify-center rounded-full px-7 text-[0.94rem] font-medium transition-transform duration-200 hover:-translate-y-0.5 active:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60 sm:h-16"
                      style={{
                        backgroundColor: "#FAFBFE",
                        color: "#120E14",
                        boxShadow: commandShadowLight,
                      }}
                    >
                      {isSubmitting ? "Submitting..." : "Search"}
                    </button>
                  </div>
                </div>

                <div
                  className="rounded-[26px] p-3 sm:p-4"
                  style={surfaceStyle("glass")}
                >
                  <div className="flex items-center justify-between gap-4 px-2 pb-3">
                    <p className="text-[0.76rem] font-medium uppercase tracking-[0.18em] text-[hsl(var(--text-muted-inverted))]">
                      Suggestions
                    </p>
                    <div className="text-[0.82rem] text-[hsl(var(--text-muted-inverted))]">
                      {isLoadingSuggestions
                        ? "Loading..."
                        : `${visibleSuggestions.length} results`}
                    </div>
                  </div>

                  {visibleSuggestionsError ? (
                    <div className="rounded-[20px] bg-[rgba(255,255,255,0.04)] px-4 py-10 text-center">
                      <div className="font-display text-[1rem] font-medium text-[hsl(var(--text-inverted))]">
                        Suggestions unavailable
                      </div>
                      <div className="mt-2 text-[0.9rem] text-[hsl(var(--text-muted-inverted))]">
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
                      {visibleSuggestions.map((suggestion, index) => (
                        <SuggestionRow
                          key={`${suggestion.query}-${index}`}
                          suggestion={suggestion}
                          selected={index === activeIndex}
                          onMouseEnter={() => setActiveIndex(index)}
                          onClick={() => handleSuggestionPick(suggestion)}
                        />
                      ))}
                    </div>
                  ) : (
                    <div className="rounded-[20px] bg-[rgba(255,255,255,0.04)] px-4 py-10 text-center">
                      <div className="font-display text-[1rem] font-medium text-[hsl(var(--text-inverted))]">
                        Start typing.
                      </div>
                    </div>
                  )}
                </div>

                <div className="flex flex-wrap gap-3">
                  <div className="rounded-full bg-[rgba(255,255,255,0.06)] px-4 py-3 text-[0.88rem] text-[hsl(var(--text-inverted))]">
                    Last submitted:{" "}
                    <span className="font-medium">{submittedQuery}</span>
                  </div>
                  <div className="rounded-full bg-[rgba(255,255,255,0.06)] px-4 py-3 text-[0.88rem] text-[hsl(var(--text-inverted))]">
                    {submissionError ?? submissionMessage}
                  </div>
                </div>
              </div>
            </div>
          </section>

          <div className="grid gap-6 lg:grid-cols-[minmax(0,1.2fr)_minmax(20rem,0.8fr)]">
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
                  <a
                    href="http://localhost:8080/swagger-ui.html"
                    target="_blank"
                    rel="noreferrer"
                    className="block rounded-[16px] bg-[hsl(var(--bg-elevated))] p-4 transition-colors duration-200 hover:bg-[hsl(var(--bg-subtle))]"
                  >
                    <div className="font-display text-[0.98rem] font-medium text-[hsl(var(--text-primary))]">
                      Swagger UI
                    </div>
                  </a>
                  <a
                    href="http://localhost:8080/v3/api-docs"
                    target="_blank"
                    rel="noreferrer"
                    className="block rounded-[16px] bg-[hsl(var(--bg-elevated))] p-4 transition-colors duration-200 hover:bg-[hsl(var(--bg-subtle))]"
                  >
                    <div className="font-display text-[0.98rem] font-medium text-[hsl(var(--text-primary))]">
                      OpenAPI JSON
                    </div>
                  </a>
                </div>
              </div>
            </Panel>

            <div className="grid gap-6">
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
                        onClick={() => void handleSubmit(item.query)}
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

              <Panel title="Cache">
                <div className="flex flex-col gap-3 sm:flex-row">
                  <input
                    value={cachePrefix}
                    onChange={(event) => setCachePrefix(event.target.value)}
                    placeholder="Prefix, e.g. search"
                    className="h-12 flex-1 rounded-full bg-[hsl(var(--bg-elevated))] px-4 text-[0.94rem] text-[hsl(var(--text-primary))] outline-none placeholder:text-[hsl(var(--text-faint))]"
                  />
                  <button
                    type="button"
                    onClick={() => void resolveCacheNode()}
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
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
