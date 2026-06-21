import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "TypeAhead Console",
  description:
    "Monotone frontend for TypeAhead suggestions, trending queries, cache diagnostics, and metrics.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full flex flex-col bg-background text-foreground">
        <a
          href="#main-content"
          className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-full focus:bg-[hsl(var(--bg-elevated))] focus:px-4 focus:py-2"
        >
          Skip to content
        </a>
        <div id="main-content" className="flex min-h-full flex-col">
          {children}
        </div>
      </body>
    </html>
  );
}
