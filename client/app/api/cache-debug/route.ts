import { NextRequest } from "next/server";

import { proxyToBackend } from "@/lib/backend";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest) {
  return proxyToBackend(request, {
    backendPath: "/cache/debug",
    searchParams: request.nextUrl.searchParams,
  });
}
