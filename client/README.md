# Client

This folder contains the Next.js frontend for TypeAhead.

## Local run

```bash
npm install
npm run dev -- --port 3001
```

Open:

- `http://localhost:3001`

## Notes

- Browser calls go through local Next.js proxy routes in `app/api/`.
- The proxy forwards requests to the Spring backend using `TYPEAHEAD_API_BASE_URL`.
- In Docker, Caddy is the public entry point and serves the frontend over HTTPS.

See the root `README.md` for the full project guide.
