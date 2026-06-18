const CACHE_TTL_MS = 30 * 60 * 1000;
const MAX_CACHE_BYTES = 50 * 1024 * 1024;

interface CacheEntry {
  text: string;
  timestamp: number;
  bytes: number;
}

const store = new Map<string, CacheEntry>();

export function getCachedText(url: string): string | undefined {
  const entry = store.get(url);
  if (!entry) return undefined;
  if (Date.now() - entry.timestamp > CACHE_TTL_MS) {
    store.delete(url);
    return undefined;
  }
  return entry.text;
}

export function setCachedText(url: string, text: string): void {
  if (store.has(url)) {
    const existing = store.get(url)!;
    totalBytes -= existing.bytes;
  }
  const bytes = text.length * 2;
  totalBytes += bytes;
  store.set(url, { text, timestamp: Date.now(), bytes });
  evictIfNeeded();
}

export function clearCachedText(url: string): void {
  const entry = store.get(url);
  if (entry) {
    totalBytes -= entry.bytes;
    store.delete(url);
  }
}

export function clearCache(): void {
  store.clear();
  totalBytes = 0;
}

export function hasCachedText(url: string): boolean {
  return getCachedText(url) !== undefined;
}

let totalBytes = 0;

function evictIfNeeded(): void {
  if (totalBytes <= MAX_CACHE_BYTES) return;
  const entries = [...store.entries()].sort(
    (a, b) => a[1].timestamp - b[1].timestamp,
  );
  while (totalBytes > MAX_CACHE_BYTES && entries.length > 0) {
    const [key, entry] = entries.shift()!;
    store.delete(key);
    totalBytes -= entry.bytes;
  }
}
