import { NextRequest, NextResponse } from "next/server";

const DEFAULT_BACKEND_URL = "http://localhost:8080";

function getBackendBaseUrl() {
  return (
    process.env.TYPEAHEAD_API_BASE_URL ??
    process.env.NEXT_PUBLIC_API_URL ??
    DEFAULT_BACKEND_URL
  );
}

function buildBackendUrl(path: string, searchParams: URLSearchParams) {
  const baseUrl = getBackendBaseUrl();
  const url = new URL(path, baseUrl);
  url.search = searchParams.toString();
  return url;
}

type ProxyOptions = {
  backendPath: string;
  forwardBody?: boolean;
  searchParams?: URLSearchParams;
};

export async function proxyToBackend(
  request: NextRequest,
  options: ProxyOptions,
) {
  const url = buildBackendUrl(
    options.backendPath,
    options.searchParams ?? request.nextUrl.searchParams,
  );

  const headers = new Headers();
  headers.set("accept", "application/json");

  const contentType = request.headers.get("content-type");
  if (contentType) {
    headers.set("content-type", contentType);
  }

  try {
    const response = await fetch(url, {
      method: request.method,
      headers,
      body: options.forwardBody ? await request.text() : undefined,
      cache: "no-store",
    });

    const body = await response.text();
    const responseHeaders = new Headers();
    responseHeaders.set(
      "content-type",
      response.headers.get("content-type") ?? "application/json",
    );

    return new NextResponse(body, {
      status: response.status,
      headers: responseHeaders,
    });
  } catch {
    return NextResponse.json(
      {
        message: `Failed to reach backend at ${url.origin}. Set TYPEAHEAD_API_BASE_URL if the server is running elsewhere.`,
      },
      { status: 502 },
    );
  }
}
