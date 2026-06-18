# Bookyapa — Project Log

## Architecture overview

| Component | Purpose |
|---|---|
| `app/_layout.tsx` → `app/(tabs)/_layout.tsx` | Bottom tabs (Home, Explore, Sources) |
| `app/(tabs)/index.tsx` | Browse — search + popular feed |
| `app/(tabs)/explore.tsx` | Category browsing with chip filters |
| `app/(tabs)/sources.tsx` | Toggle book sources on/off |
| `app/reader.tsx` | Reader — WebView-based, flip (page) + scroll mode |
| `services/webview-html.ts` | `buildFlipHtml` (JS pagination) + `buildScrollHtml` (native scroll) |
| `services/epub-parser.ts` | Downloads EPUB zip, parses XHTML, extracts images as data URIs |
| `sources/` | Plugin system: Gutenberg, OpenLibrary, Standard Ebooks |
| `hooks/use-epub-reader.ts` | Fetches EPUB → parses → returns html + textContent |
| `hooks/use-reader-store.ts` | Settings state (font, theme, brightness, mode) |
| `services/text-cleaner.ts` | Plain-text fallback pipeline |
| `services/html-stripper.ts` | Strip HTML to text |
| `services/chapter-detector.ts` | Detect chapter boundaries in plain text |
| `db/db.ts` + `db/operations.ts` | SQLite for reading progress + source settings |

## What's built and working

### Flip (page) mode
- `buildFlipHtml` in `services/webview-html.ts`
- Hidden `#measurer` div performs binary search to compute `charsPerPage`
- Sequential `pages[]` array — no gaps, per-page overflow verification
- Touch handlers on `document` with unconditional `preventDefault()`
- Tap zones: left third = prev page, center = toggle menu, right third = next page
- Swipe detection (dx > 30px) for prev/next
- Progress reported via `postMessage{type:'progress'}`
- Settings sync (font, line height, theme, brightness) via `postMessage{type:'updateSettings'}`
- `goToOffset` / `goPrevPage` / `goNextPage` via message handler
- Container height set via JS `window.innerHeight - 20`

### Scroll mode
- `buildScrollHtml` in `services/webview-html.ts`
- Native vertical scroll
- Images render inline with data URIs from EPUB parser
- Center-tap for menu
- `goToOffset` via scrollTop position
- Progress via `scrollTop / scrollHeight` ratio

### EPUB parsing (`services/epub-parser.ts`)
- Downloads EPUB zip, extracts XHTML spine items
- Strips `<script>` and `<style>` tags
- Replaces `<img src>` with base64 data URIs
- Returns `html` (with data URIs) and `textContent` (stripped text)
- Chapter offset detection

### Progress persistence
- SQLite `reading_progress` table
- Saves: character offset, font size, line height, reading mode, EPUB URL
- Restores on reader open (offset, settings)

### Source system
- 3 active sources: Gutenberg, OpenLibrary, Standard Ebooks
- All hard-filter to English
- Timeout handling (10s), per-source toggle

## Session history

### 2026-06-11 — Native pagination engine + text cleaning
- Character-offset pagination with RN hidden `<Text>` measurement
- `text-cleaner.ts`: boilerplate stripping, page markers, hard reflow, paragraph formatting
- `html-stripper.ts`: HTML → clean text
- **Deleted in 2026-06-12 rewrite**

### 2026-06-12 — WebView EPUB reader migration
- All 6 phases completed:
  1. Source `epubUrl` population
  2. Navigation params + types (`ReaderParams.epubUrl`)
  3. New files: `epub-parser.ts`, `webview-html.ts` (3D flip + scroll), `use-epub-reader.ts`
  4. DB schema: `ALTER TABLE reading_progress ADD COLUMN epub_url TEXT`
  5. Reader rewrite: WebView replaces native `<Text>`
  6. Delete old: `use-pagination.ts`
- 3D page-flip with CSS columns + rotateY drag-to-flip
- Android Fabric compatibility workarounds

### 2026-06-17 — Bug fixes: images, menu, flip stability, touch handling
- Images responsive: `img{max-width:100%;height:auto}`
- 4 touch handling attempts before `pointerEvents="none"` + RN overlay
- CSS column fallback chain for colW measurement

### 2026-06-18 — JS text pagination rewrite + Fabric touch fix (current)
- **Problem:** `pointerEvents="none"` overlay didn't fire on Fabric (native WebView consumes touches below RN layer)
- **Problem:** CSS columns `height:100%` collapses to 0 on Fabric → `tp=1` always
- **Fix:** Removed overlay + pointerEvents entirely. Replaced CSS columns with JS text pagination:
  - `#measurer` div binary search for `charsPerPage`
  - Sequential `pages[]` with per-page overflow verification
  - Touch handlers on `document` with unconditional `preventDefault()`
  - No CSS columns, no RN overlay, no Fabric compatibility issues
- **Files changed:**
  - `services/webview-html.ts` — `buildFlipHtml` complete rewrite
  - `app/reader.tsx` — removed overlay, pointerEvents, dead refs; added `textContent` option
  - `WebViewHtmlOptions` — added `textContent: string`

## Known issues

1. **Images not shown in flip mode** — flip mode uses only `textContent` (plain text). Images from EPUB HTML are discarded. Scroll mode has full image support.
2. **No test runner** — no tests configured. Manual testing only.
3. **React Compiler enabled** (`experiments.reactCompiler` in app.json) — may cause unexpected re-render behavior if rules of hooks are violated.
4. **`app/modal.tsx`** — unmodified Expo template content, can be removed.
5. **`expo-env.d.ts`** — gitignored, auto-generated.

## Future plans

### Short-term
- None currently queued.

### Medium-term
- None currently queued.

### Long-term — Option C: Layout-based pagination with images (flip mode)

**Goal:** Render EPUB images inline with text in page-flip mode, replacing character-offset text pagination with layout-based HTML pagination.

**Why current approach can't handle images:**
- Character-offset pagination measures text characters only
- Images have no "character length" — no position in `textareaContent`
- An `<img>` tag with unknown height breaks the charsPerPage → viewport calculation

**Implementation sketch:**

1. **`epub-parser.ts`**: Also expose image map (position in stripped text → data URI path in original HTML) to the paginator
2. **`webview-html.ts` `buildFlipHtml`**:
   - Replace `textContent`-based binary search with HTML fragment rendering in `#measurer`
   - Split EPUB HTML at block boundaries (`<p>`, `<div>`, `<img>`) instead of character offsets
   - Pre-load all images in hidden elements before computing page breaks (async, with timeout fallback to skip images)
   - Render pages via `innerHTML` instead of `textContent`, injecting escaped text + `<img>` data URIs
   - Re-run pagination on font/size/theme changes
3. **`reader.tsx`**: Pass original EPUB HTML (with images) to `buildFlipHtml` alongside `textContent` for fallback measurement when image loading fails

**Tradeoffs:**
- Slower initial pagination (must wait for images to load before page breaks are known)
- More complex edge cases (very tall images, SVG, threaded layout)
- Setting changes require full re-paginate (image re-measurement)

**Alternative approaches rejected:**
- Option A (scroll mode for image-heavy books): Works now, no changes needed
- Option B (full-page image overlays): Simpler but breaks reading flow — images appear as inserts between text pages

### Long-term — Other possibilities
- Dark mode themes for app chrome (not just reader)
- Download books for offline reading
- Search within book
- Bookmarks / annotations
- Reading stats (books completed, time spent)
