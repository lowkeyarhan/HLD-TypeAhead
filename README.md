# TypeAhead - Search Suggestion Platform

TypeAhead is a small search suggestion platform built for a system design assignment. It serves prefix-based suggestions, tracks searches, ranks trending queries, and shows cache and performance details in a simple frontend.

The stack is split into a Spring Boot backend, a Next.js frontend, PostgreSQL for stored query counts, Redis in the Docker stack, and Caddy for HTTPS access to the frontend.

---

## Table of Contents

- [System Architecture](#system-architecture)
- [Tech Stack \& Tools](#tech-stack--tools)
- [Dataset \& Loading](#dataset--loading)
- [Caching, Ranking \& Batch Writes](#caching-ranking--batch-writes)
- [Frontend \& HTTPS Access](#frontend--https-access)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Setup \& Installation](#setup--installation)
- [Verification Notes](#verification-notes)
- [Project Docs](#project-docs)

---

## System Architecture

TypeAhead follows a modular monolith backend with a separate frontend.

High-level flow:

```text
Browser
  -> Caddy (HTTPS)
  -> Next.js frontend
  -> Next.js proxy routes
  -> Spring Boot backend
  -> cache ring / prefix index / batch writer
  -> PostgreSQL
```

Main runtime ideas:

1. The browser opens the frontend through Caddy on HTTPS.
2. The frontend sends browser requests to local Next.js API proxy routes.
3. The proxy forwards requests to the Spring backend.
4. Suggestion reads use a cache-first path.
5. Search writes are buffered and flushed in batches.

Detailed architecture notes:

- [Architecture Diagram](docs/architecture-diagram.md)
- [Project Report](docs/project-report.md)

---

## Tech Stack & Tools

### Core stack

| Layer | Technology | Purpose |
| --- | --- | --- |
| Frontend | Next.js 16, React 19, Tailwind v4 | Search UI, metrics UI, proxy routes |
| Backend | Spring Boot, Java | Suggest, search, trending, metrics APIs |
| Database | PostgreSQL | Stores query counts and timestamps |
| Cache infra | Redis in Docker stack | Runtime infra dependency in compose |
| HTTPS proxy | Caddy | Redirects HTTP to HTTPS and serves the frontend |

### Supporting tools

| Tool | Purpose |
| --- | --- |
| Docker Compose | Starts the full local stack |
| SpringDoc / Swagger | Backend API docs |
| Flyway | Database migrations |
| Maven | Backend build |
| npm | Frontend build |

---

## Dataset & Loading

The backend first looks for a CSV file at:

- `server/src/main/resources/dataset/queries.csv`

In the current repo, that file is not present.

So the backend falls back to:

- `SyntheticDatasetSource`

What that means:

- about `100000` synthetic queries are generated
- counts follow an uneven popularity curve
- the app still runs without a manual dataset import

Startup loading is handled by:

- `DatasetLoader`

Flow:

1. Check whether the database already has rows.
2. If yes, skip loading.
3. If no, try CSV.
4. If CSV is missing or empty, generate synthetic data.
5. Batch insert the rows.
6. Rebuild the in-memory prefix index.

---

## Caching, Ranking & Batch Writes

### Suggestion reads

Suggestions use:

- consistent hashing across logical cache nodes
- an in-memory prefix index for cache misses

Main behavior:

1. `/suggest` computes a cache key from prefix + limit
2. the hash ring picks a node
3. a hit returns fast
4. a miss falls through to the prefix index

### Trending

Trending results come from the backend ranking strategy exposed through:

- `GET /trending`

### Search submissions

`POST /search` does not write every request straight to the database.

Instead:

1. the query is normalized
2. the query is added to an in-memory buffer
3. a scheduler flushes buffered counts in batches

This reduces write pressure on PostgreSQL.

---

## Frontend & HTTPS Access

### Public URL

When the Docker stack is running, open:

- `https://localhost`

This is the main frontend URL.

### HTTP redirect

This URL:

- `http://localhost`

redirects to:

- `https://localhost`

### How HTTPS is enforced

Caddy is configured in:

- `infra/caddy/Caddyfile`

Compose service:

- `caddy`

Behavior:

1. listen on ports `80` and `443`
2. redirect HTTP to HTTPS
3. reverse proxy HTTPS traffic to the internal Next.js client container

Why `localhost`:

- it resolves on every local browser without host-file edits
- it keeps the HTTPS setup simple
- it avoids browser-specific issues with custom loopback aliases

### Local certificate note

Caddy uses:

- `tls internal`

So on first local use, your browser may ask you to trust the local certificate authority.

The local Caddy root certificate is stored under:

- `infra/caddy/data/caddy/pki/authorities/local/root.crt`

---

## API Documentation

### Backend docs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Main backend routes

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/ping` | health check |
| `GET` | `/suggest?q=<prefix>&limit=<n>` | prefix suggestions |
| `POST` | `/search` | submit a search |
| `GET` | `/trending?limit=<n>` | trending queries |
| `GET` | `/metrics` | runtime metrics |
| `GET` | `/cache/debug?prefix=<prefix>&limit=<n>` | cache node lookup |

### Frontend proxy routes

The browser uses these local Next.js routes:

- `/api/suggest`
- `/api/search`
- `/api/trending`
- `/api/metrics`
- `/api/cache-debug`

These routes forward requests to the Spring backend.

---

## Project Structure

```text
client/        Next.js frontend
server/        Spring Boot backend
docs/          project docs and report
infra/caddy/   HTTPS reverse proxy config
compose.yaml   local multi-container stack
```

Useful paths:

- `client/app/page.tsx` - main frontend page
- `client/lib/backend.ts` - shared proxy helper
- `server/src/main/resources/application.yml` - backend config
- `docs/project-report.md` - report for evaluation

---

## Setup & Installation

### Option 1: Docker Compose

Run the full stack:

```bash
docker compose up --build
```

After startup:

- frontend: `https://localhost`
- backend: `http://localhost:8080`

### Option 2: Run services directly

Backend:

```bash
cd server
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments=-Dspring.docker.compose.enabled=false
```

Frontend:

```bash
cd client
npm install
npm run dev -- --port 3001
```

Open:

- `http://localhost:3001`

---

## Verification Notes

What was verified during implementation:

- frontend lint passes
- frontend production build passes
- Docker Compose configuration resolves correctly
- live backend routes respond for suggest, search, trending, metrics, and cache debug

Important note:

- the backend test profile still expects PostgreSQL access
- if DB access is unavailable, backend tests can fail because of environment setup, not because of frontend changes

---

## Project Docs

- [Architecture Diagram](docs/architecture-diagram.md)
- [Project Report](docs/project-report.md)
- [Project Overview Notes](docs/about.md)
- [Backend Build Notes](docs/prompt.md)
