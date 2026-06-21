"use client";

import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import type { KeyboardEvent } from "react";

import dashboardContent from "@/data/dashboard-content.json";
import { fetchJson } from "@/lib/fetch-json";
import type {
  CacheDebugResponse,
  CacheLookupResult,
  MetricsResponse,
  SearchResponse,
  Suggestion,
  SuggestionResponse,
  TrendingResponse,
} from "@/components/dashboard/types";
import {
  buildMetricBlocks,
  errorColor,
  successColor,
} from "@/components/dashboard/utils";

const LISTBOX_ID = "search-typeahead-listbox";

export function useTypeaheadDashboard() {
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);
  const [suggestionsError, setSuggestionsError] = useState<string | null>(null);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [submittedQuery, setSubmittedQuery] = useState<string | null>(null);
  const [submissionMessage, setSubmissionMessage] = useState<string | null>(
    null,
  );
  const [submissionError, setSubmissionError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [trending, setTrending] = useState<Suggestion[]>([]);
  const [metrics, setMetrics] = useState<MetricsResponse | null>(null);
  const [dashboardError, setDashboardError] = useState<string | null>(null);
  const [isLoadingDashboard, setIsLoadingDashboard] = useState(true);
  const [backendOnline, setBackendOnline] = useState(true);
  const [cachePrefix, setCachePrefix] = useState(
    dashboardContent.defaultCachePrefix,
  );
  const [cacheLookupResult, setCacheLookupResult] = useState<CacheLookupResult>(
    {
      nodeId: "node-0",
      status: "Miss",
      prefix: dashboardContent.defaultCachePrefix,
    },
  );
  const [cacheError, setCacheError] = useState<string | null>(null);
  const [isResolvingCache, setIsResolvingCache] = useState(false);

  const inputRef = useRef<HTMLInputElement | null>(null);

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
      fetchJson<TrendingResponse>(
        `/api/trending?limit=${dashboardContent.trendingLimit}`,
      ),
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
        const queryString = new URLSearchParams({
          prefix,
          limit: String(dashboardContent.cacheDebugLimit),
        }).toString();

        const result = await fetchJson<CacheDebugResponse>(
          `/api/cache-debug?${queryString}`,
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
      void resolveCacheNode(dashboardContent.defaultCachePrefix);
    }, 0);

    return () => window.clearTimeout(handle);
  }, [loadDashboard, resolveCacheNode]);

  useEffect(() => {
    const normalized = debouncedQuery.trim();

    if (!normalized) {
      return;
    }

    const controller = new AbortController();
    const queryString = new URLSearchParams({
      q: normalized,
      limit: String(dashboardContent.suggestLimit),
    }).toString();

    void fetchJson<SuggestionResponse>(`/api/suggest?${queryString}`, {
      signal: controller.signal,
    })
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

  const visibleSuggestions = useMemo(
    () => (debouncedQuery.trim().length > 0 ? suggestions : []),
    [debouncedQuery, suggestions],
  );

  const visibleSuggestionsError = useMemo(
    () => (debouncedQuery.trim().length > 0 ? suggestionsError : null),
    [debouncedQuery, suggestionsError],
  );

  const handleSearchKeyDown = useCallback(
    (event: KeyboardEvent<HTMLInputElement>) => {
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
    },
    [activeIndex, handleSubmit, query, visibleSuggestions],
  );

  const handleQueryChange = useCallback((value: string) => {
    setQuery(value);
    setActiveIndex(-1);
    const hasValue = value.trim().length > 0;
    setSuggestionsError(null);
    setIsLoadingSuggestions(hasValue);

    if (!hasValue) {
      setSuggestions([]);
      setIsLoadingSuggestions(false);
    }
  }, []);

  const metricBlocks = useMemo(() => buildMetricBlocks(metrics), [metrics]);

  const nodeRates = useMemo(
    () =>
      Object.entries(metrics?.perNodeCacheHitRates ?? {}).sort(([left], [right]) =>
        left.localeCompare(right),
      ),
    [metrics],
  );

  const submissionStatus = submissionError ?? submissionMessage;
  const cacheStateColor =
    cacheLookupResult.status === "Hit" ? successColor : errorColor;

  return {
    inputRef,
    listboxId: LISTBOX_ID,
    query,
    handleQueryChange,
    handleSearchKeyDown,
    handleSearchSubmit: () => void handleSubmit(query),
    isSubmitting,
    isLoadingSuggestions,
    visibleSuggestions,
    visibleSuggestionsError,
    activeIndex,
    handleSuggestionHover: setActiveIndex,
    handleSuggestionPick,
    submittedQuery,
    submissionStatus,
    trending,
    dashboardError,
    isLoadingDashboard,
    handleTrendingSubmit: (value: string) => void handleSubmit(value),
    metricBlocks,
    nodeRates,
    cachePrefix,
    setCachePrefix,
    resolveCacheNode: () => void resolveCacheNode(),
    isResolvingCache,
    cacheError,
    cacheLookupResult,
    backendOnline,
    cacheStateColor,
  };
}
