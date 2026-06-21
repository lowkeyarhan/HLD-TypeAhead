"use client";

import dashboardContent from "@/data/dashboard-content.json";
import { CachePanel } from "@/components/dashboard/CachePanel";
import { SearchSection } from "@/components/dashboard/SearchSection";
import { StatusPill } from "@/components/dashboard/StatusPill";
import { TrendingPanel } from "@/components/dashboard/TrendingPanel";
import { RuntimePanel } from "@/components/dashboard/RuntimePanel";
import { useTypeaheadDashboard } from "@/hooks/useTypeaheadDashboard";

export function TypeaheadDashboard() {
  const dashboard = useTypeaheadDashboard();

  return (
    <main className="min-h-screen bg-[hsl(var(--bg-base))] px-4 py-4 text-[hsl(var(--text-primary))] sm:px-6 sm:py-6 lg:px-8">
      <div className="mx-auto flex min-h-[calc(100vh-2rem)] w-full max-w-[1200px] flex-col gap-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="max-w-2xl">
            <h1 className="mt-3 max-w-2xl text-balance font-display text-[1.5rem] font-medium leading-[1.04] text-[hsl(var(--text-primary))] sm:text-[1.95rem] lg:text-[2.5rem]">
              {dashboardContent.heroTitle}
            </h1>
            <p className="mt-3 max-w-[52ch] text-pretty text-[0.92rem] leading-6 text-[hsl(var(--text-muted))]">
              {dashboardContent.heroDescription}
            </p>
          </div>

          <div className="self-start">
            <StatusPill online={dashboard.backendOnline} />
          </div>
        </header>

        <section
          className="rounded-[36px] p-5 text-[hsl(var(--text-inverted))] sm:p-7 lg:p-8"
          style={{
            background:
              "radial-gradient(800px circle at 50% 0%, rgba(255,255,255,0.075), rgba(0,0,0,0) 34%), linear-gradient(180deg, #111318 0%, #0C0E12 100%)",
          }}
        >
          <div className="mx-auto w-full max-w-[860px]">
            <SearchSection
              commandLabel={dashboardContent.commandLabel}
              commandTitle={dashboardContent.commandTitle}
              searchPlaceholder={dashboardContent.searchPlaceholder}
              emptySuggestions={dashboardContent.emptySuggestions}
              listboxId={dashboard.listboxId}
              inputRef={dashboard.inputRef}
              query={dashboard.query}
              onQueryChange={dashboard.handleQueryChange}
              onSearchKeyDown={dashboard.handleSearchKeyDown}
              onSubmit={dashboard.handleSearchSubmit}
              isSubmitting={dashboard.isSubmitting}
              isLoadingSuggestions={dashboard.isLoadingSuggestions}
              suggestions={dashboard.visibleSuggestions}
              suggestionsError={dashboard.visibleSuggestionsError}
              activeIndex={dashboard.activeIndex}
              onSuggestionHover={dashboard.handleSuggestionHover}
              onSuggestionPick={dashboard.handleSuggestionPick}
              submittedQuery={dashboard.submittedQuery}
              submissionStatus={dashboard.submissionStatus}
            />
          </div>
        </section>

        <div className="grid gap-6 lg:grid-cols-[minmax(0,1.2fr)_minmax(20rem,0.8fr)]">
          <RuntimePanel
            metricBlocks={dashboard.metricBlocks}
            nodeRates={dashboard.nodeRates}
            docsLinks={dashboardContent.docsLinks}
          />

          <div className="grid gap-6">
            <TrendingPanel
              trending={dashboard.trending}
              dashboardError={dashboard.dashboardError}
              isLoadingDashboard={dashboard.isLoadingDashboard}
              onSubmit={dashboard.handleTrendingSubmit}
            />
            <CachePanel
              cachePrefix={dashboard.cachePrefix}
              onCachePrefixChange={dashboard.setCachePrefix}
              onResolve={dashboard.resolveCacheNode}
              isResolvingCache={dashboard.isResolvingCache}
              cacheError={dashboard.cacheError}
              cacheLookupResult={dashboard.cacheLookupResult}
              cacheStateColor={dashboard.cacheStateColor}
            />
          </div>
        </div>
      </div>
    </main>
  );
}
