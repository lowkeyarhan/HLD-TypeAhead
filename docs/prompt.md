# Search Typeahead System — Backend Build Prompts (10 Phases)

> Read `about.md` first if you haven't — it explains the _why_ behind every decision here.
> This doc is the _what to build, exactly_, broken into 10 phases.

## How to use this doc

- Paste **one phase at a time** into your AI coding tool, in order. Don't skip ahead — Phase 4 needs Phase 2's entity, Phase 9 replaces code written in Phase 6, etc.
- After each phase: build it, run it, read the generated code, run the tests, and make sure you can explain what was added before moving to the next phase. The assignment treats "can't explain it" as plagiarism even if it runs — don't let phases pile up unread.
- Every phase names the **design patterns / SOLID principles** it applies. These aren't decoration — they're things you'll be asked about in the viva, and they're part of why a "well-structured, scalable" submission scores well.
- Every phase lists **edge cases** explicitly. Treat these as a checklist, not flavor text — handling them is most of what separates a 60-mark submission from a 90-mark one.

---

## Global Standards (apply to every phase, every time)

**Tech stack:** Already defined in the server folder using jav 26 and modular monolith architecture

**Package-by-feature, modular monolith** (full diagram in `about.md` §5.1):
`com.<author>.typeahead.{suggestion, search, trending, cache, batch, index, data, ingestion, common}` — each module owns its own controller/service/DTOs; modules talk to each other only through public interfaces, never internals.

**Non-negotiable coding standards:**

- DTOs only cross API boundaries — controllers never accept or return JPA entities.
- Every service has an interface **and** an implementation class (`SuggestionService` / `SuggestionServiceImpl`), even with only one implementation — this is for testability and future extension, not ceremony.
- No business logic in controllers — they validate input shape and delegate to exactly one service call.
- Constructor injection everywhere. No field `@Autowired`.
- Every tunable constant (TTLs, decay half-life, batch size/interval, virtual node count) lives in `application.yml`, bound via a typed `@ConfigurationProperties` class — nothing hardcoded.
- Public classes and non-trivial methods get a short Javadoc explaining _intent_, not just signature.
- Never return a raw 500 for bad input. Validate early, fail with a clear, documented error response.

**Scope note:** these 10 phases cover the **backend only**, per your stack choice. The UI requirements from the assignment (search box, dropdown, trending section, loading/error states, keyboard nav) still apply to your submission — build that separately against the APIs these phases produce. `about.md` §2.3 and the original assignment's UI section have the full checklist.

### Design Patterns & SOLID — where they show up

| Principle / Pattern           | Where it's applied                                                                                                                                                     |
| ----------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Single Responsibility (S)** | `CacheNode` only stores/expires entries, `ConsistentHashRing` only routes, `BatchFlushScheduler` only orchestrates timing — never combine these jobs into one class    |
| **Open/Closed (O)**           | `RankingStrategy` (Phase 8) lets you add a new ranking algorithm without touching the controller, the index, or existing strategies                                    |
| **Liskov Substitution (L)**   | Any `RankingStrategy` or `DatasetSource` implementation must be swappable without breaking the caller — write your tests against the interface, not the concrete class |
| **Interface Segregation (I)** | Service interfaces expose only what callers actually need — e.g. `PrefixIndexService` never leaks whether it's backed by a Trie or a `TreeMap`                         |
| **Dependency Inversion (D)**  | Controllers depend on service interfaces; services depend on repository interfaces — concrete classes are wired by Spring, never `new`'d directly                      |
| **Strategy**                  | `RankingStrategy` (Phase 8), `DatasetSource` (Phase 3), `HashFunction` (Phase 7)                                                                                       |
| **Facade**                    | `CacheNodeManager` (Phase 7) hides the ring + node details; `MetricsService` (Phase 10) hides how percentiles are computed                                             |
| **Producer–Consumer**         | `SearchEventBuffer` (producer) + `BatchFlushScheduler` (consumer), decoupled, Phase 9                                                                                  |
| **Observer / Event Listener** | Ingestion-complete event triggering the initial index build, Phase 4                                                                                                   |
| **Repository**                | Spring Data JPA repositories, Phase 2                                                                                                                                  |
| **DTO**                       | Every API boundary, every phase                                                                                                                                        |
| **Decorator / Aspect**        | AOP-based latency timing wrapping `/suggest`, Phase 10                                                                                                                 |

---

## Required Dependencies (`pom.xml`)

| Dependency              | Coordinates                                                      | Why you need it                                                                      |
| ----------------------- | ---------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| Web                     | `org.springframework.boot:spring-boot-starter-web`               | REST controllers, embedded Tomcat                                                    |
| Validation              | `org.springframework.boot:spring-boot-starter-validation`        | `@NotBlank` etc. on request DTOs                                                     |
| Data JPA                | `org.springframework.boot:spring-boot-starter-data-jpa`          | `QueryCountRepository`, entity mapping                                               |
| Postgres driver         | `org.postgresql:postgresql` (runtime)                            | Production database                                                                  |
| H2                      | `com.h2database:h2` (runtime/test)                               | Zero-setup local dev + test profile                                                  |
| Flyway                  | `org.flywaydb:flyway-core`                                       | Versioned schema migrations instead of relying on `ddl-auto`                         |
| Lombok                  | `org.projectlombok:lombok` (provided)                            | Cuts boilerplate on entities/DTOs (getters, builders)                                |
| AOP                     | `org.springframework.boot:spring-boot-starter-aop`               | Latency-timing aspect in Phase 10                                                    |
| Actuator                | `org.springframework.boot:spring-boot-starter-actuator`          | Health checks, foundation for the `/metrics` work in Phase 10                        |
| OpenAPI/Swagger         | `org.springdoc:springdoc-openapi-starter-webmvc-ui`              | Auto-generated API docs — satisfies the submission's "API documentation" requirement |
| CSV parsing             | `org.apache.commons:commons-csv`                                 | Reading the dataset file in Phase 3                                                  |
| Test starter            | `org.springframework.boot:spring-boot-starter-test` (test scope) | JUnit 5, Mockito, AssertJ, MockMvc                                                   |
| Concurrency test helper | `org.awaitility:awaitility` (test scope)                         | Cleanly testing the scheduled batch flush in Phase 9                                 |

**Deliberately not included:** `spring-boot-starter-cache`, Redis, Caffeine, Ehcache, or any off-the-shelf caching library. The entire point of this assignment is hand-rolling the distributed cache + consistent hashing yourself (Phase 7) — using a pre-built caching abstraction would defeat the graded objective and you wouldn't be able to defend it in the viva.

---

## Phase 1 — Project Bootstrap & Foundation

**Objective:** a clean, runnable skeleton with the module structure, config backbone, and exception handling base — nothing functional yet.

```
Create a new Spring Boot 3.x Maven project named "typeahead" using Java 17+.

- Set up package-by-feature structure under com.<author>.typeahead:
  suggestion, search, trending, cache, batch, index, data, ingestion, common — each as an
  empty package for now (a package-info.java with a one-line description each is fine).
- Add application.yml with two profiles: "local" (H2 in-memory, console enabled) and
  "default" (Postgres, connection settings via env vars). Externalize every tunable constant
  we'll need later (cache node count, virtual nodes per node, cache TTL seconds, batch flush
  interval ms, batch size threshold, recency decay half-life minutes) as placeholders now,
  even before anything reads them.
- Create one typed @ConfigurationProperties class per concern (e.g. CacheProperties,
  BatchProperties, RankingProperties) in common.config, each @Validated so the app fails fast
  with a clear message if a required property is missing — don't let it silently start broken.
- Add a global exception handling skeleton in common.exception: a GlobalExceptionHandler
  (@RestControllerAdvice), a base ApiException, and an ErrorResponseDTO (timestamp, message,
  path) — even though nothing throws custom exceptions yet.
- Add a trivial GET /ping endpoint returning {"status":"ok"} just to prove the skeleton runs.

DESIGN PATTERNS / SOLID FOR THIS PHASE
- Single Responsibility: one configuration class per concern, not one giant AppConfig god class.
- Dependency Inversion: even at this stage, set the convention that future services will be
  defined as interface + impl — don't create any concrete-only service classes later.

EDGE CASES
- Missing or malformed required config values must fail application startup with a clear,
  readable error (via @Validated config properties) — not a confusing downstream NPE later.
- The "local" profile must require zero external setup (no Postgres, no Docker) to run.

ACCEPTANCE CRITERIA
- `mvn spring-boot:run` (or the equivalent for "local" and "default" profiles) starts cleanly.
- GET /ping returns 200 with {"status":"ok"} on both profiles.

TESTS
- A @SpringBootTest "context loads" smoke test for each profile.
```

---

## Phase 2 — Data Model & Persistence Layer

**Objective:** the one real entity this project needs, its repository, and a proper schema migration.

```
Add the persistence layer for query-count data.

- Create the QueryCount JPA entity in the `data` package: id (Long, generated), queryText
  (String, NOT NULL, unique), totalCount (Long, default 0), recentCount (Long, default 0),
  lastSearchedAt (Instant, nullable), createdAt (Instant, set on insert).
- Create QueryCountRepository extends JpaRepository<QueryCount, Long> with:
  - Optional<QueryCount> findByQueryText(String queryText)
  - a @Modifying native upsert query you will wire up in Phase 9 (write it now, don't call it
    yet): INSERT ... ON CONFLICT (query_text) DO UPDATE SET total_count = total_count + ?,
    recent_count = recent_count + ?, last_searched_at = ? for a single row, and note in a
    comment that Phase 9 will need a *batch* version of this.
- Add Flyway migration V1__init_query_count.sql creating the table with a UNIQUE index on
  query_text (this index is what makes prefix lookups and upserts efficient).
- Decide and document via a code comment: all query text is normalized to lowercase + trimmed
  BEFORE it ever reaches this layer (the normalization itself happens in a shared utility you'll
  build in Phase 6) — so query_text in the DB is always already-normalized, no case-insensitive
  index trickery needed.

DESIGN PATTERNS / SOLID FOR THIS PHASE
- Repository pattern (Spring Data JPA itself is this).
- Dependency Inversion: every future service depends on QueryCountRepository the interface,
  never on a concrete JDBC implementation.

EDGE CASES
- Attempting to insert a duplicate query_text without using the upsert path must fail loudly
  (constraint violation) — that's the unique index doing its job, prove it in a test.
- created_at / lastSearchedAt must use a consistent time source (Instant.now() via a single
  injectable Clock bean, not scattered System.currentTimeMillis() calls) — this also makes
  time-dependent code testable later (Phase 8's decay math needs this).

ACCEPTANCE CRITERIA
- App starts, Flyway migration runs cleanly on both H2 and Postgres.
- Repository can save and find-by-query-text in an integration test.

TESTS
- @DataJpaTest: save + findByQueryText round-trip.
- @DataJpaTest: duplicate query_text insert (outside the upsert path) throws a constraint
  violation, proving the unique index exists and works.
```

---

## Phase 3 — Dataset Ingestion

**Objective:** load 100,000+ query/count rows into the DB on startup, idempotently, from a real or synthetic source.

```
Implement dataset ingestion so the app has real data to serve suggestions from.

- Define a DatasetSource interface: Stream<QueryCountRow> load(), where QueryCountRow is a
  simple (query, count) pair.
- Implement CsvDatasetSource reading src/main/resources/dataset/queries.csv (columns: query,
  count) using Apache Commons CSV.
- Implement SyntheticDatasetSource as an automatic fallback used when the CSV file is absent:
  generate 100,000+ realistic-looking query strings (mix of single words, two/three-word
  phrases, product-name-style strings) with a Zipfian-ish count distribution — a small number
  of very popular queries, a long tail of rare ones, not a flat random distribution.
- Implement DatasetLoader as an ApplicationRunner that: checks if query_count already has rows
  (skip + log if so — idempotent across restarts), otherwise streams the chosen DatasetSource in
  batches of ~1000 rows via JDBC batch insert (not one INSERT per row — that would take forever
  at 100k+ rows), and logs total rows loaded plus time taken.
- Within the loader, aggregate (sum) any duplicate query strings appearing in the source data
  BEFORE inserting — don't create two rows for the same normalized query.

DESIGN PATTERNS / SOLID FOR THIS PHASE
- Strategy: DatasetSource lets you swap CSV vs synthetic (or a future real dataset) without
  touching DatasetLoader at all.
- Single Responsibility: parsing, aggregation, and persistence are separate methods/classes —
  don't let DatasetLoader also know CSV parsing details.

EDGE CASES
- Malformed CSV rows (missing count, non-numeric count, empty query string) are skipped with a
  warning log — never crash the whole ingestion over one bad row.
- Re-running the app with data already present must skip loading entirely and log that it did,
  not silently double-insert or silently no-op without telling you.
- All ingested query text passes through the same normalization (lowercase + trim) used
  everywhere else in the app, so it lines up with what /suggest and /search will produce.

ACCEPTANCE CRITERIA
- Cold start with an empty DB loads 100,000+ rows, logged with a row count and duration.
- A second start with data already present logs that it skipped loading.

TESTS
- Unit test: SyntheticDatasetSource produces the requested row count, no duplicate query
  strings, all counts positive.
- Unit test: CsvDatasetSource skips malformed rows without throwing, and correctly parses
  well-formed ones.
```

---

## Phase 4 — In-Memory Prefix Index

**Objective:** fast prefix lookups against the full dataset, built at startup, updatable incrementally later.

```
Build the in-memory structure that makes /suggest fast even on a cache miss.

- Define PrefixIndexService interface:
  - List<QueryCount> search(String prefix, int limit)
  - void applyDelta(String queryText, long countDelta, Instant searchedAt)
  - void rebuild()
- Implement it backed by a TreeMap<String, QueryCount>, using subMap(prefix, prefix +
  Character.MAX_VALUE) for prefix range scans. (Document in a comment why TreeMap was chosen
  over a Trie for this assignment: simpler to implement correctly, and at 100k-scale the
  subMap() range scan is fast enough — a Trie would be the better choice at a much larger scale,
  worth mentioning as a known trade-off if asked in the viva.)
- rebuild() loads the full dataset from QueryCountRepository and populates the map. Trigger this
  automatically once DatasetLoader (Phase 3) finishes — publish an ApplicationEvent
  (e.g. DatasetLoadedEvent) from DatasetLoader and have an @EventListener in the index module
  call rebuild(). This keeps ingestion and indexing decoupled.
- search(prefix, limit) returns matches sorted by totalCount descending for now (smarter ranking
  arrives in Phase 8) — return at most `limit` results, don't fetch everything and truncate.

DESIGN PATTERNS / SOLID FOR THIS PHASE
- Encapsulation / Interface Segregation: callers only ever see PrefixIndexService — whether it's
  a TreeMap or a Trie underneath is a private implementation detail.
- Observer: DatasetLoadedEvent decouples "ingestion finished" from "index should rebuild."

EDGE CASES
- Empty prefix ("") returns an empty list — document this choice explicitly (returning all 100k+
  rows for an empty prefix would be both useless and slow).
- A prefix longer than any existing query, or one with zero matches, returns an empty list
  cleanly — never null, never an exception.
- All lookups normalize the prefix (lowercase, trim) the same way ingestion normalized stored
  data, or nothing will ever match.
- Concurrent reads during a rebuild() must not throw or return a half-built result — rebuild into
  a new map and atomically swap the reference, don't mutate the live map in place.

ACCEPTANCE CRITERIA
- A prefix search against 100k+ entries returns in single-digit milliseconds locally.
- rebuild() runs once automatically after ingestion completes, with no manual trigger needed.

TESTS
- Exact-prefix match, case-insensitivity, no-match, and empty-prefix behavior, each as a
  separate test.
- A basic performance assertion: search against a 100k-entry generated index completes under a
  defined time threshold.
```

---

## Phase 5 — Suggestion API (Basic Ranking)

**Objective:** `GET /suggest?q=<prefix>` end-to-end, count-only ranking, full edge-case handling — no cache yet.

```
Expose the suggestion endpoint using the prefix index from Phase 4.

- Create SuggestionService interface + SuggestionServiceImpl, calling PrefixIndexService.search().
- Create DTOs: SuggestResultDTO(String query, long count), SuggestionResponseDTO(String prefix,
  List<SuggestResultDTO> suggestions).
- Create SuggestionController: GET /suggest with @RequestParam(required = false) String q.
  Normalize input (trim, lowercase, cap to a sane max length e.g. 100 chars) before passing to
  the service. Wrap the service result in SuggestionResponseDTO.
- Extract the normalization logic into a small shared QueryNormalizer utility in `common` —
  Phase 6's search endpoint will reuse the exact same normalization, and they must never drift
  apart from each other.

DESIGN PATTERNS / SOLID FOR THIS PHASE
- MVC layering: controller does shape validation + delegation only, all logic lives in the service.
- Dependency Inversion: SuggestionController depends on the SuggestionService interface, never
  the impl class directly.

EDGE CASES (handle every one explicitly, with a test for each)
- Missing `q` param → 200 OK with an empty suggestions list, not a 400 (the spec says handle
  this gracefully, not reject it).
- `q=` (empty string) → same as missing, empty list, 200 OK.
- `q` containing only whitespace → treated as empty after normalization.
- Mixed-case input ("IpHoNe") → normalized before lookup, matches lowercase-stored data.
- Prefix with genuinely zero matches → empty list, 200 OK, not an error.
- Extremely long `q` (potential abuse) → defensively capped at a documented max length rather
  than processed as-is.
- Special/control characters in `q` → since this only ever touches the in-memory index (no raw
  SQL string concatenation involved), confirm and document that this is inherently safe from
  injection, but still strip control characters defensively before using the string as a cache
  key in Phase 7.

ACCEPTANCE CRITERIA
- Manual testing (Postman) of every edge case above returns a sane response, never a 500.

TESTS
- @WebMvcTest covering each edge case above as its own test method.
- Unit test for SuggestionServiceImpl against a mocked PrefixIndexService.
```

---

## Phase 6 — Search Submission API (Synchronous, Temporary)

**Objective:** a working `POST /search` baseline that writes directly to the DB — Phase 9 will replace the write mechanism, not the API contract.

```
Implement search submission with a direct (temporary) write path.

- Create DTOs: SearchRequestDTO(String query) with @NotBlank, SearchResponseDTO(String message).
- Create SearchService interface + impl: normalize the query via the same QueryNormalizer from
  Phase 5, then find-or-create the QueryCount row and increment totalCount + update
  lastSearchedAt, via QueryCountRepository. (This direct-write approach is intentionally
  temporary — Phase 9 replaces the internals of this method with a buffered write, but the
  method signature and the controller stay the same.)
- Create SearchController: POST /search, validates the request body, always responds
  {"message": "Searched"} on success.

DESIGN PATTERNS / SOLID FOR THIS PHASE
- Single Responsibility: normalization lives in one shared place, not duplicated between
  suggestion and search modules.
- Dependency Inversion: SearchService depends on the QueryCountRepository interface.

EDGE CASES
- Blank/empty query in the request body → this endpoint SHOULD reject with 400 and a clear
  validation message (unlike /suggest — submitting nothing isn't a valid action, there's a real
  difference between "show me nothing" and "record nothing").
- Query string over a sane max length → reject with 400.
- Concurrent submissions of the identical query from multiple simultaneous requests must not
  lose increments — use either an atomic DB increment statement or correctly scoped
  @Transactional handling, and prove it under concurrency in a test.
- "iPhone ", "iphone", " IPHONE" must all resolve to the exact same stored row (same
  normalization as Phase 5).

ACCEPTANCE CRITERIA
- Repeated POST /search with the same query visibly increments total_count in the DB.
- A brand-new query gets created with count 1 on first submission.

TESTS
- Integration test: post the same query from 5 concurrent threads, assert the final count is
  exactly 5 — no lost updates.
```

---

## Phase 7 — Distributed Cache Layer + Consistent Hashing

**Objective:** the logical multi-node cache, the consistent hash ring, and wiring it into the suggestion read path as a cache-aside layer.

```
Build the distributed cache and wire it in front of the prefix index.

- Implement HashFunction interface (e.g. a single method `long hash(String key)`), with a
  concrete implementation using a well-distributed hash (e.g. MurmurHash3 or MD5-based) — keep
  it swappable, don't hardcode the algorithm directly into the ring.
- Implement ConsistentHashRing: TreeMap<Long, CacheNode> internally, a configurable number of
  virtual nodes per physical node (default 150+), addNode(CacheNode), removeNode(String nodeId),
  getNode(String key) using ceilingEntry() with wraparound to firstEntry() when needed.
- Implement CacheNode: an id, its own ConcurrentHashMap<String, CacheEntry>, and its own
  AtomicLong hitCount / missCount.
- Implement CacheEntry: value + expiresAt (Instant), with an isExpired() helper.
- Implement CacheNodeManager as a facade: Optional<T> get(key), void put(key, value, ttl), void
  invalidate(key), and CacheDebugInfo getDebugInfo(key) returning the owning node id plus
  whether the corresponding lookup was a hit or miss. This is the ONLY class the rest of the app
  is allowed to talk to for caching — nothing outside `cache` should ever touch ConsistentHashRing
  or CacheNode directly.
- Update SuggestionServiceImpl (Phase 5) to check CacheNodeManager.get(cacheKey) first; on miss,
  call PrefixIndexService.search(), then CacheNodeManager.put() the result with the configured
  TTL before returning. Treat expired entries as misses, not stale hits.
- Create CacheController: GET /cache/debug?prefix=<prefix>, returning CacheDebugResponseDTO
  (owning node id, hit/miss) via CacheNodeManager.getDebugInfo() — using the EXACT SAME cache key
  construction logic as the real suggestion lookup (don't let these drift apart, or debug output
  becomes misleading).

DESIGN PATTERNS / SOLID FOR THIS PHASE
- Strategy: HashFunction is swappable without touching ring logic.
- Facade: CacheNodeManager hides the ring + per-node detail from the rest of the app.
- Single Responsibility: CacheNode only stores/expires, ConsistentHashRing only routes,
  CacheNodeManager only orchestrates the two.

EDGE CASES
- A TTL-expired entry must be treated as a miss on read, not returned as stale data.
- The ring must work correctly with zero or one configured node (degenerate but valid case —
  everything just routes to the same place).
- Concurrent reads/writes on the same CacheNode must not corrupt state — use thread-safe
  collections (ConcurrentHashMap, AtomicLong), never a plain HashMap.
- Cache key construction must be centralized in one place and reused identically by the real
  suggestion path and the debug endpoint.

ACCEPTANCE CRITERIA
- Hitting /suggest?q=iph twice in a row shows a miss then a hit (visible in logs or via
  /cache/debug).
- Calling /cache/debug for several different prefixes shows them landing on different node ids —
  real evidence of distribution, not everything piling onto one node.

TESTS
- ConsistentHashRingTest: reasonably even key distribution across nodes for a large sample of
  random keys; adding one node to an existing ring only remaps a small fraction of previously
  assigned keys (the actual point of consistent hashing — assert this numerically, don't just
  assert "it works").
- CacheNodeTest: TTL expiry behavior.
- Integration test: /suggest is a miss on first call, a hit on the second, for the same prefix.
```

---

## Phase 8 — Trending / Recency-Aware Ranking

**Objective:** upgrade ranking from count-only to recency-aware decay scoring, without breaking the existing `/suggest` contract, and expose `/trending`.

```
Add recency-aware ranking as a swappable strategy, plus a trending endpoint.

- Define RankingStrategy interface: List<SuggestResultDTO> rank(List<QueryCount> candidates,
  int limit).
- Implement PopularityRankingStrategy: sort by totalCount descending (formalizes what Phase 5
  already did as a named strategy).
- Implement RecencyAwareRankingStrategy using:
  score = totalCount + (recentCount * 0.5^(minutesSinceLastSearch / halfLifeMinutes))
  with halfLifeMinutes bound from RankingProperties (Phase 1). Use the injected Clock (Phase 2)
  to compute minutesSinceLastSearch, never System.currentTimeMillis() directly, so this is
  testable with fixed times.
- Inject the active strategy into SuggestionServiceImpl via config
  (typeahead.ranking.strategy: popularity | recency-aware), defaulting to recency-aware. Use a
  simple factory/switch in a @Configuration class to select which RankingStrategy bean is
  primary, based on this property.
- Create TrendingController: GET /trending?limit=<n> (default limit e.g. 10), returning the top
  N queries under whichever strategy is currently active, as TrendingResponseDTO.
- Add a documented, runnable way to demonstrate both strategies on the same sample data (a small
  test or a logged comparison) — directly satisfies the spec's "demonstrate the difference using
  sample data or logs" requirement.
- Add a short, explicit code comment (or README section) addressing the freshness vs. latency
  vs. implementation-complexity trade-off this approach makes — the assignment asks for this by
  name, don't skip it.

DESIGN PATTERNS / SOLID FOR THIS PHASE
- Strategy (the main one): RankingStrategy lets you add a third ranking algorithm later without
  touching the controller, the index, or either existing strategy.
- Open/Closed: adding RecencyAwareRankingStrategy must not require modifying
  PopularityRankingStrategy or SuggestionController.

EDGE CASES
- A query with recentCount > 0 but a very old lastSearchedAt must decay toward (never below) its
  totalCount floor — verify decay never produces a negative contribution.
- A brand-new query with recentCount = 0 scores identically under both strategies.
- lastSearchedAt in the future (clock skew, shouldn't happen but defend anyway) must not produce
  a decay factor above 1.0 — clamp it.
- Tied scores fall back to a secondary, deterministic sort key (e.g. alphabetical on queryText)
  so suggestion order never flickers between identical requests.

ACCEPTANCE CRITERIA
- A recently-searched but historically-unpopular query visibly outranks an old, untouched,
  popular query under recency-aware ranking, and does NOT under popularity-only ranking, for the
  same input data — this is your core demo for the 20 trending marks.

TESTS
- RecencyAwareRankingStrategy: decay math at multiple time deltas (just-searched, at the
  half-life mark, long after), the clamping edge case, and the tie-breaking rule.
```

---

## Phase 9 — Batch Writes (Replace Synchronous Writes)

**Objective:** replace Phase 6's direct write with the buffered, scheduled batch-flush design — the single biggest behavioral change in the project, isolate it carefully.

```
Replace the synchronous write from Phase 6 with a buffered, batched write path.

- Implement SearchEventBuffer: a ConcurrentHashMap<String, LongAdder> for pending counts plus a
  ConcurrentHashMap<String, Instant> for last-seen-at per query. Expose increment(String
  queryText) and a single atomic Map<String, BufferedEvent> drainAndSwap() method that replaces
  the live maps with fresh empty ones and returns exactly what was drained — this swap must be
  synchronized/atomic so no increment is ever lost or double-counted across a flush boundary.
- Update SearchServiceImpl (Phase 6) so it now calls searchEventBuffer.increment(normalizedQuery)
  instead of writing to the repository directly. The controller's immediate response is
  unchanged — still {"message": "Searched"} right away.
- Implement BatchFlushScheduler: @Scheduled(fixedDelayString =
  "${typeahead.batch.flush-interval-ms}"). On each run: drainAndSwap(); if empty, skip entirely
  (no pointless DB round-trip); otherwise perform ONE bulk upsert covering all drained entries
  using the native ON CONFLICT (query_text) DO UPDATE query (extend the single-row version from
  Phase 2 into a true batch statement), then call prefixIndexService.applyDelta(...) for each
  drained entry, then either invalidate the matching cache keys via CacheNodeManager or
  explicitly rely on TTL expiry — pick one and state which in a comment.
- Add a size-triggered early flush: if SearchEventBuffer's distinct-key count crosses
  typeahead.batch.size-threshold, trigger an immediate flush instead of waiting for the next
  scheduled tick. Guard against two flushes (scheduled + size-triggered) running concurrently
  with a simple lock/flag.
- Track and log, per flush: number of distinct queries flushed, total increments applied, and a
  running ratio of (search requests received) : (flush operations performed) since startup —
  this is the exact evidence the performance report needs.
- Add the resilience comment the assignment explicitly grades: right where drainAndSwap() is
  called in BatchFlushScheduler, explain in a comment that anything sitting in the buffer at
  crash time is lost (bounded data loss of at most one flush interval), why that trade-off is
  acceptable for this use case, and what a write-ahead-log approach would look like as a stronger
  (but unbuilt) alternative.

DESIGN PATTERNS / SOLID FOR THIS PHASE
- Producer–Consumer: SearchServiceImpl (producer) and BatchFlushScheduler (consumer) are fully
  decoupled via SearchEventBuffer — neither knows about the other directly.
- Single Responsibility: SearchEventBuffer only buffers, BatchFlushScheduler only orchestrates
  timing + delegates persistence, the actual upsert SQL stays in the repository layer.

EDGE CASES
- New increments arriving WHILE a flush is draining must not be lost or double-counted — this is
  exactly what drainAndSwap()'s atomicity guarantees; prove it under concurrent load in a test.
- Two scheduled/triggered flushes must never run simultaneously if one runs long — guard with a
  lock so a slow flush can't be double-processed by an overlapping run.
- A query inserted by Phase 3's dataset loader but never searched again must be left completely
  untouched by flushes — no phantom updates to rows nobody searched.
- A single flush batch containing both brand-new queries and existing ones must be handled
  correctly by the bulk upsert (both INSERT and UPDATE branches in the same statement).

ACCEPTANCE CRITERIA
- A load test submitting thousands of searches over a short window results in a small, bounded
  number of flush-driven DB writes — capture and log this ratio for the performance report.

TESTS
- SearchEventBufferTest: concurrent writer threads, assert no lost increments.
- BatchFlushSchedulerTest: mock the repository, assert the flush produces the expected aggregated
  upsert call, and that an empty buffer produces zero DB calls.
- An integration test numerically proving the request-to-write ratio improvement (e.g. 1000
  searches → far fewer than 1000 writes).
```

---

## Phase 10 — Observability, Resilience, Testing & Documentation

**Objective:** close out the non-functional requirements and produce the documentation artifacts the rubric explicitly asks for.

```
Add observability, finish hardening, and produce the submission documentation.

- Implement request-latency capture for /suggest via a Spring AOP @Around aspect (or a
  HandlerInterceptor) wrapping the controller method — don't scatter manual timer calls through
  the suggestion service code. Store samples in a bounded rolling structure and compute p95 on
  demand.
- Implement MetricsService as a facade exposing: p95 latency for /suggest, overall + per-node
  cache hit rate (pulled from CacheNodeManager), DB read count vs DB write count since startup,
  and the search-requests-to-flushes ratio from Phase 9.
- Expose this via GET /metrics (or wire selected values into Spring Boot Actuator's existing
  endpoints — either is fine, document which you chose).
- Do a pass over every endpoint from Phases 5-8 confirming GlobalExceptionHandler (Phase 1)
  produces clean, documented error responses for every invalid-input case listed in those
  phases' edge-case sections — no stack traces should ever reach the client.
- Add springdoc-openapi annotations/config so Swagger UI fully documents all five endpoints
  (/suggest, /search, /cache/debug, /trending, /metrics) with example requests and responses.
- Write the README: setup steps for both H2 and Postgres profiles, dataset source/regeneration
  instructions, an architecture summary (reuse the about.md diagram or link to it), and a
  "Performance" section containing the ACTUAL measured p95 latency, cache hit rate, and write-
  reduction ratio from a local run — not placeholders.
- Hardening pass: re-verify that cache-hit-rate metrics still correctly count hits/misses now
  that the recency-aware strategy (Phase 8) is wired in; re-verify the consistent-hashing demo
  (/cache/debug) still shows multi-node distribution after everything is connected end to end.

DESIGN PATTERNS / SOLID FOR THIS PHASE
- Decorator/Aspect: AOP latency timing wraps existing behavior without modifying the controller
  or service code it measures.
- Facade: MetricsService hides percentile computation and metric-gathering details from callers.

EDGE CASES
- Latency sampling itself must add negligible overhead — don't synchronize on every single
  request if it can be avoided; prefer a lock-free or low-contention structure for recording
  samples.
- /metrics must return sane (zero/empty, not error) values on a freshly started app with no
  traffic yet, rather than dividing by zero on an empty hit-rate calculation.

ACCEPTANCE CRITERIA
- A full local run (with a basic load test via k6/JMeter against /suggest) produces real,
  non-placeholder numbers for latency, hit rate, and write reduction, pasted into the README.
- Swagger UI at /swagger-ui.html correctly documents all endpoints.

TESTS
- A final end-to-end integration test exercising the full flow in one scenario: ingest → search
  submission → suggest (miss then hit) → batch flush → suggest again reflecting the updated
  ranking. This is your "demo in code form" and the best evidence you actually understand how
  every phase connects.
```

---

## After all 10 phases

Cross-check against `about.md` §2.5 (grading rubric) and §7 (viva checklist) before you submit. If you can't explain why a class in Phase 7 or Phase 9 looks the way it does, go back and read that phase's design-pattern notes again — that's exactly what they're there for.
