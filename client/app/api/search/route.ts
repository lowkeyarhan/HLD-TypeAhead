import { NextRequest } from "next/server";

import { proxyToBackend } from "@/lib/backend";

export const dynamic = "force-dynamic";

export async function POST(request: NextRequest) {
  return proxyToBackend(request, {
    backendPath: "/search",
    forwardBody: true,
  });
}
