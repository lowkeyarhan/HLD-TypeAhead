import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "TypeAhead Search Console",
  description:
    "Frontend for the TypeAhead backend, metrics, and cache diagnostics.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
