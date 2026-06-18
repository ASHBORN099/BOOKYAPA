# AGENTS.md

## Expo has changed — read the versioned docs first
Before writing any Expo/RN code, consult https://docs.expo.dev/versions/v54.0.0/. This project pins **Expo SDK 54**, **RN 0.81**, **React 19.1**, **expo-router 6**. Do not copy patterns from older Expo tutorials (e.g. `expo-router` v3/v4 APIs differ).

## Commands
- `npm start` — Metro dev server (interactive; `w`/`a`/`i` to open web/Android/iOS)
- `npm run web` / `android` / `ios` — target a platform directly
- `npm run lint` — `expo lint` (uses `eslint-config-expo/flat`)
- `npm run reset-project` — **DESTRUCTIVE.** Moves `app/`, `components/`, `hooks/`, `constants/`, `scripts/` into `app-example/` (or deletes them). Never run unless the user asks for a reset.
- No test runner is configured. Do not invent one.

## Architecture (what matters to change)
- **Entry:** `app/_layout.tsx` → `app/(tabs)/_layout.tsx` (bottom tabs: Home, Explore, Sources)
- **Browse screen:** `app/(tabs)/index.tsx` — search + popular feed, debounced 400ms, in-RAM cache, transaction-id counter for race-safe async (pure JS, no `AbortController`)
- **Explore screen:** `app/(tabs)/explore.tsx` — category-based browsing with chip filters
- **Sources screen:** `app/(tabs)/sources.tsx` — toggle book sources on/off (enabled sources persist to SQLite `source_settings` table)
- **Reader screen:** `app/reader.tsx` — **built.** WebView-based EPUB reader with JS text-pagination (flip mode) and native scroll (scroll mode), chapter detection, SQLite-backed reading progress, configurable font size / line height / theme / brightness.
  - **Flip mode:** `services/webview-html.ts:buildFlipHtml` — hidden `#measurer` div in same Chrome engine performs binary search to compute exact `charsPerPage`. Sequential page boundary array (`pages[]`) with per-page overflow verification. No CSS columns. Touch handlers on `document` with unconditional `preventDefault()` (avoids Fabric touch suppression). Tap zones (left/center/right), swipe (dx > 30px), menu toggle all handled in WebView JS via `postMessage`.
  - **Scroll mode:** `services/webview-html.ts:buildScrollHtml` — WebView without columns, native scroll, progress via `scrollTop / scrollHeight` ratio. Images render inline with data URIs from `epub-parser.ts`.
  - EPUB path: downloads EPUB zip from source, parses XHTML via `jszip`, renders HTML in scroll mode or stripped text in flip mode.
  - Plain-text fallback: `services/text-cleaner.ts` pipeline (strip boilerplate, page markers, hard reflow, format paragraphs) → WebView.
  - Progress bridge: `postMessage`/`onMessage` between WebView and React Native for offset, page changes, chapter jumps, settings sync.
- **Source/plugin system** (`sources/`) — plug-in architecture with 3 registered sources:
  - `BookSource.ts` — abstract base (`searchBooks`, `fetchPopularBooks`, `fetchChapterList`)
  - `GutenbergSource.ts` — **active**; hits `https://gutendex.com`, English-only (`languages=en`), provides `epubUrl` from `formats['application/epub+zip']`
  - `OpenLibrarySource.ts` — **active**; hits `https://openlibrary.org/search.json`, English-only (`language=eng`), provides `epubUrl` from archive.org
  - `StandardEbooksSource.ts` — **active**; scrapes `https://standardebooks.org/ebooks`, provides `epubUrl` via `…/downloads/{slug}.epub`
  - `SourceManager.ts` — fans out queries across all active sources with timeout handling (10s); supports per-source toggle from Sources screen
  - `types.ts` — `StreamableBook`, `BookChapter`, `ReaderParams` (includes `epubUrl?: string`)
  - `errors.ts` — `NetworkSourceError`, `TimeoutSourceError`, `HttpSourceError`, `ParseSourceError`, `PartialSourceError`

## Conventions specific to this repo
- All book sources **must hard-filter to English** at the database level (Gutenberg: `languages=en`; OpenLibrary: `language=eng`). Do not filter client-side.
- Search inputs are debounced 400ms; results are cached in a `useRef` map keyed by lowercased query.
- Async work that can race (search/typing) uses a pure-JS transaction-id counter, not `AbortController`. Mirror the pattern in `app/(tabs)/index.tsx:38-114`.
- Path alias `@/*` → repo root (see `tsconfig.json`). Use `@/components/...`, `@/hooks/...`, `@/constants/...`, etc.
- Dark theme is hardcoded (`#121212`/`#1a1a1a`) in the browse screen — do not switch to `ThemedText`/`ThemedView` there without a reason.
- `app.json` enables `newArchEnabled`, `experiments.typedRoutes`, `experiments.reactCompiler` — keep these in mind if something looks "off."

## Known stale / cruft to be aware of
- `app/modal.tsx` is unmodified Expo template content. Safe to delete when cleaning up.
- `app-example/` is gitignored; only appears after `npm run reset-project`.
- `.expo/`, `expo-env.d.ts`, `web-build/`, `dist/` are gitignored — do not commit edits to them.
- `components/`, `hooks/`, `constants/` still hold template defaults; replace as needed for the real product.

## When adding a new book source
1. Subclass `BookSource` in `sources/`.
2. Normalize raw API JSON into `StreamableBook[]` (use `GutenbergSource.mapResults` as the template).
3. Register the instance in `SourceManager.sources`.
4. Hard-filter to English in the request URL.

---

## Completed work

### 2026-06-11 — Native pagination engine + text cleaning
- Built character-offset pagination with hidden `<Text>` measurement, dual-column layout, safety factor tuning (settled at 0.97)
- Created `services/text-cleaner.ts`: `stripGutenbergBoilerplate`, `stripPageMarkers`, `reflowHardBreaks`, `breakLongWords`, `formatParagraphs`
- Roman numeral stripping (line-based pre-pass + pattern matching), title filter (exact-line with optional punctuation), hyphenated hard-wrap fix
- `stripHtml` whitespace normalization in `services/html-stripper.ts`
- All items self-contained to `reader.tsx`, `use-pagination.ts`, `text-cleaner.ts`, `html-stripper.ts`

### 2026-06-12 — WebView EPUB reader migration (all 6 phases completed)
| Phase | What | Key files |
|---|---|---|
| 1 | Source `epubUrl` population | `StandardEbooksSource.ts`, `OpenLibrarySource.ts` (Gutenberg already had it) |
| 2 | Navigation params + types | `types.ts` (ReaderParams.epubUrl), `index.tsx`, `explore.tsx` |
| 3 | New files | `services/epub-parser.ts` (zip → XHTML), `services/webview-html.ts` (3D flip + scroll templates), `hooks/use-epub-reader.ts` (fetch → parse → build HTML) |
| 4 | DB schema | `ALTER TABLE reading_progress ADD COLUMN epub_url TEXT` in `db/db.ts` |
| 5 | Reader rewrite | `reader.tsx` — WebView replaces native `<Text>`, `postMessage`/`onMessage` bridge, settings sync, chapter jump, progress restore |
| 6 | Delete old files | `hooks/use-pagination.ts` deleted; kept text-cleaner/html-stripper/chapter-detector/text-streamer for fallback |

**3D page-flip (`services/webview-html.ts`):**
- `buildFlipHtml`: CSS columns + `overflow:hidden`, `#flip-layer` overlay with `rotateY` transform, drag-to-flip (finger angle tracking) + animated tap-to-flip, bounds protection
- `buildScrollHtml`: vertical scroll, `scrollTop/scrollHeight` progress ratio

**Android compatibility fixes:**
- `touch-action:none` on `#container`, `#content`, and all children (`#content *`) — `touch-action` is NOT inherited
- `touchstart` changed from `passive:true` → `passive:false` (Android Chrome skips passive touchstart on fast taps)
- WebView source memoized to prevent reload on re-render (which reset touch handlers mid-gesture)

**Bug fixes:**
- `db/db.ts`: ALTER TABLE with try-catch for existing databases
- Body padding moved from `#content` to `<body>` (eliminated ~48px column drift)
- EPUB download page fallback: checks `content-type` header, re-fetches binary if HTML detected
- Strip `<script>`/`<style>` from EPUB XHTML to prevent DOM breakage
- Progress save only when `offset > 0` (prevents page-0 overwrite)
- `goToOffset` handled in scroll mode, restore effect sends offset to WebView
- Removed `rp()` call inside `go()` to avoid re-render disturbing touch handlers

### 2026-06-17 — Bug fixes: images responsive, settings menu, flip stability, touch handling
- **Bug fix: images not responsive** — added `img{max-width:100%;height:auto}` to both `buildScrollHtml` and `buildFlipHtml` CSS in `services/webview-html.ts`
- **Bug fix: settings blocked by PanResponder** — added `showMenuRef` + `useEffect` in `reader.tsx` so `onStartShouldSetPanResponder` returns `false` when menu is visible (later superseded by overlay approach)
- **Bug fix: flip mode stuck at page 0** — defensive `colW` measurement fallback chain in `buildFlipHtml` `measure()`: `clientWidth` → `getBoundingClientRect().width` → `documentElement.clientWidth-48` → `1`
- **Touch handling overhaul** — 3 failed approaches before final working solution:
  - Attempt 1: `react-native-gesture-handler` `Gesture.Pan()` + `Gesture.Tap()` via `GestureDetector` — crashed with Fabric worklet serialization error (`Cannot read property 'current' of undefined`, even with `.runOnJS(true)`)
  - Attempt 2: RN responder capture (`onStartShouldSetResponderCapture`) on parent View — WebView's TextureView bypasses RN touch dispatch on Fabric
  - Attempt 3: `onTouchStart`/`onTouchEnd` on WebView props — `react-native-webview` doesn't forward RN touch events from native WebView to JS
  - **Final: `pointerEvents="none"` on WebView + separate RN overlay View** (`reader.tsx:419`). `pointerEvents` is a core RN View prop that rejects touches at the native level, guaranteeing the overlay receives all touches. Combined with `e.preventDefault()` in WebView JS `touchstart` handler (`webview-html.ts:228`). Works for both tap (left/third center right) and swipe (dx > 30px) gestures. No library dependencies, no worklet serialization, no Fabric compatibility issues.
- **WebView JS touch fix** — added `if(flipDir!==0)e.preventDefault();` in `touchstart` handler of `buildFlipHtml` to explicitly claim the touch, preventing WebView from consuming it internally

### 2026-06-18 — JS text pagination rewrite + Fabric touch fix
- **Root cause discovered:** `touch-action:none` CSS on Fabric WebView suppresses touch dispatch to main thread entirely. `pointerEvents="none"` overlay also fails (native TextureView consumes touches below RN layer).
- **Fix:** Removed `touch-action:none` CSS. Moved all touch handlers to `document` with unconditional `preventDefault()` on `touchstart`, `touchmove`, `touchend`. No RN overlay or pointerEvents needed.
- **Root cause discovered:** CSS `column-width` + `height:100%` collapses to 0 on Fabric (height cascade failure), producing `tp=1` always.
- **Fix:** Replaced CSS multi-column with JS text pagination:
  - Hidden `#measurer` div (same font/width) performs binary search for exact `charsPerPage`
  - Sequential `pages[]` array built with per-page overflow verification on measurer
  - No gaps between pages — page N+1 starts exactly where page N's word-break ended
- **Bug fix: vanished lines** — each page's content is verified on measurer; if overflow detected, binary-search within segment for exact fit before word-boundary backup
- **Container height** set via JS `window.innerHeight - 20` instead of CSS `100vh` (avoids `vh` vs JS measurement mismatch)
- **Files changed:**
  - `services/webview-html.ts` — complete `buildFlipHtml` rewrite (lines 117-230)
  - `app/reader.tsx` — removed overlay, pointerEvents, dead refs; added `textContent` option
  - `services/webview-html.ts` `WebViewHtmlOptions` — added `textContent: string`
