import type { KeyboardEvent, RefObject } from "react";

import type { Suggestion } from "@/components/dashboard/types";
import {
  commandShadowLight,
  formatCount,
  getSurfaceStyle,
} from "@/components/dashboard/utils";

type SearchSectionProps = {
  commandLabel: string;
  commandTitle: string;
  searchPlaceholder: string;
  emptySuggestions: string;
  listboxId: string;
  inputRef: RefObject<HTMLInputElement | null>;
  query: string;
  onQueryChange: (value: string) => void;
  onSearchKeyDown: (event: KeyboardEvent<HTMLInputElement>) => void;
  onSubmit: () => void;
  isSubmitting: boolean;
  isLoadingSuggestions: boolean;
  suggestions: Suggestion[];
  suggestionsError: string | null;
  activeIndex: number;
  onSuggestionHover: (index: number) => void;
  onSuggestionPick: (suggestion: Suggestion) => void;
  submittedQuery: string;
  submissionStatus: string;
};

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
      style={{ backgroundColor: selected ? "#FFFFFF" : "transparent" }}
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

export function SearchSection({
  commandLabel,
  commandTitle,
  searchPlaceholder,
  emptySuggestions,
  listboxId,
  inputRef,
  query,
  onQueryChange,
  onSearchKeyDown,
  onSubmit,
  isSubmitting,
  isLoadingSuggestions,
  suggestions,
  suggestionsError,
  activeIndex,
  onSuggestionHover,
  onSuggestionPick,
  submittedQuery,
  submissionStatus,
}: SearchSectionProps) {
  return (
    <div className="max-w-4xl space-y-5">
      <div>
        <p className="text-[0.76rem] font-medium uppercase tracking-[0.2em] text-[hsl(var(--text-muted-inverted))]">
          {commandLabel}
        </p>
        <h2 className="mt-3 max-w-2xl text-balance font-display text-[1.35rem] font-medium leading-[1.06] text-[hsl(var(--text-inverted))] sm:text-[1.75rem] lg:text-[2.2rem]">
          {commandTitle}
        </h2>
      </div>

      <div className="rounded-[28px] p-2" style={getSurfaceStyle("glass")}>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <label className="sr-only" htmlFor="typeahead-query">
            Search query
          </label>
          <input
            id="typeahead-query"
            ref={inputRef}
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            onKeyDown={onSearchKeyDown}
            placeholder={searchPlaceholder}
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
            onClick={onSubmit}
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
        style={getSurfaceStyle("glass")}
      >
        <div className="flex items-center justify-between gap-4 px-2 pb-3">
          <p className="text-[0.76rem] font-medium uppercase tracking-[0.18em] text-[hsl(var(--text-muted-inverted))]">
            Suggestions
          </p>
          <div className="text-[0.82rem] text-[hsl(var(--text-muted-inverted))]">
            {isLoadingSuggestions
              ? "Loading..."
              : `${suggestions.length} results`}
          </div>
        </div>

        {suggestionsError ? (
          <div className="rounded-[20px] bg-[rgba(255,255,255,0.04)] px-4 py-10 text-center">
            <div className="font-display text-[1rem] font-medium text-[hsl(var(--text-inverted))]">
              Suggestions unavailable
            </div>
            <div className="mt-2 text-[0.9rem] text-[hsl(var(--text-muted-inverted))]">
              {suggestionsError}
            </div>
          </div>
        ) : suggestions.length > 0 ? (
          <div
            id={listboxId}
            role="listbox"
            aria-label="Search suggestions"
            className="space-y-2"
          >
            {suggestions.map((suggestion, index) => (
              <SuggestionRow
                key={`${suggestion.query}-${index}`}
                suggestion={suggestion}
                selected={index === activeIndex}
                onMouseEnter={() => onSuggestionHover(index)}
                onClick={() => onSuggestionPick(suggestion)}
              />
            ))}
          </div>
        ) : (
          <div className="rounded-[20px] bg-[rgba(255,255,255,0.04)] px-4 py-10 text-center">
            <div className="font-display text-[1rem] font-medium text-[hsl(var(--text-inverted))]">
              {emptySuggestions}
            </div>
          </div>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <div className="rounded-full bg-[rgba(255,255,255,0.06)] px-4 py-3 text-[0.88rem] text-[hsl(var(--text-inverted))]">
          Last submitted: <span className="font-medium">{submittedQuery}</span>
        </div>
        <div className="rounded-full bg-[rgba(255,255,255,0.06)] px-4 py-3 text-[0.88rem] text-[hsl(var(--text-inverted))]">
          {submissionStatus}
        </div>
      </div>
    </div>
  );
}
