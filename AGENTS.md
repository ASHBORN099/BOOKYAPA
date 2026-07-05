# Bookyapa — Session State

## Project
Book reader app for Android (Kotlin/Compose). Migrated from React Native.

## Changes made (Jun 23)

### Contrast fix (blank screen fix)
- Root cause: `#666666` text on `#121212` background = 3.28:1 contrast (WCAG AA minimum is 4.5:1)
- Created `AppColors.bodyText` = `Color(0xFF999999)` in `ui/theme/Colors.kt`
- Replaced all 40 `Color(0xFF666666)` occurrences across 10 screen files
- **Files changed**: ExploreScreen, SourcesScreen, SearchScreen, HistoryScreen, AddEditSourceScreen, LibraryScreen, BookDetailScreen, BottomNavBar, CatalogScreen, RepoManagerScreen

## Changes (Jun 24)

### LibraryScreen green background fix
- Root cause: `LibraryScreen.kt` had `containerColor = Color(0xFF1B5E20)` (dark green). Text `#999999` on `#1B5E20` had only 2.76:1 contrast — text was invisible, making the screen appear black.
- Changed to `Color(0xFF121212)` (consistent with theme background). `#999999` on `#121212` = 6.58:1 contrast.
- Added `--enable-native-access=ALL-UNNAMED` to `gradlew.bat` for JDK 24 compat.

### Explore/Browse fixes
- `fetchExploreBooks()` in `BookRepository.kt` now throws error when CSS selectors return empty results (was silently showing "No content found")
- `RemoteDataSource.kt`: Added Gutenberg 404 page detection, better 403 error messages
- Fixed RoyalRoad search URL: `keyword` param → `title` param

### RoyalRoad Browse now working
- Root cause: `container.selectFirst("h2.fiction-title a")` returned the cover `<a>` (inside `<figure>`) instead of the title `<a>` due to JSoup 1.18.1 element-scoped `selectFirst` bug with descendant selectors.
- Fix: Added `selectFirstScoped(root, cssQuery)` in `HtmlParser.kt` — uses `root.select(cssQuery)` then prefers the first result with non-blank text. This correctly picks the title link over the textless cover link.
- Verified: 20 books from `/fictions/best-rated` display correctly in a 2-column grid (Mother of Learning, The Perfect Run, etc.)

### HTTP client improvements (Jun 24 session 2)
- **OkHttp engine revert**: Tried ktor-client-okhttp → caused hanging requests (never completing). Reverted to CIO engine. Removed `ktor-client-okhttp` dependency.
- **`Accept-Encoding: gzip, deflate` removed**: CIO engine can't decompress gzip → crashes with `Input length = 1`. Let servers choose default encoding.
- **Headers added**: `Upgrade-Insecure-Requests: 1`, `Cache-Control: max-age=0` added to CIO client in `NetworkModule.kt`.
- **HttpCookies plugin**: Installed in `NetworkModule.kt` for cookie persistence across requests.

### Fallback explore URL feature
- Added `exploreUrlPatternFallback` field (default `""`) to `SourceConfig.kt`
- `BookRepository.kt`: When primary explore URL fails and fallback is non-blank, tries fallback URL automatically.
- ScribbleHub fallback `/latest-series/` added to `sources_catalog.json` (uses same CSS selectors as `/series-ranking/`).

### Better error logging & detection
- `RemoteDataSource.kt`: Logs HTTP status code, response preview (first 500 chars), explicit "Page Not Found" detection (<50KB), 403 detection.
- Debug URL logging on fetch and on failure.

### Stale Room DB config discovery
- `sources_catalog.json` changes only take effect after `adb shell pm clear com.bookyapa.app`.
- Sources store config in Room DB at install time — NOT re-read from JSON at runtime.
- After `pm clear`, sources must be re-added via **Sources → Browse Catalog → tap "+"** for updated URLs to take effect.

### ScribbleHub & Gutenberg now working
- **ScribbleHub**: `/series-ranking/` returns status 200, 4 books displayed (Rebirth of the Nephilim, Collide Gamer, etc.). Root cause was stale DB config using old URL.
- **Gutenberg**: `/ebooks/search/?sort_order=downloads` returns status 200, 6 books displayed (Moby Dick, Pride and Prejudice, etc.). HTML has anti-scraping comment `<!-- DON'T USE THIS PAGE FOR SCRAPING -->` but book data below it still parses.
- **FictionPress**: Removed from catalog — `/browse/` returns 404 (endpoint deprecated).

### Other changes
- Added `fallbackToDestructiveMigration()` to Room database builder in `DatabaseModule.kt`
- Fixed JSoup scoping bug workaround: `selectFirstScoped` with text-content preference

## Changes (Jun 24 session 3) — Book detail, covers, and UI polish

### Gutenberg book detail selectors fixed
- `bookCover`: `".cover img"` → `"#cover img"` in `sources_catalog.json`. `.cover` is a CSS class selector — doesn't match `id="cover"`.
- `bookTitle`: `"h1"` → `"td[itemprop=headline]"`. `h1` returned "Moby Dick; Or, The Whale, by Herman Melville" (includes author suffix). `td[itemprop=headline]` returns clean title.
- Author `"table.bibrec a"` returns "Melville, Herman, 1819-1891" with dates (functional but includes lifespan).

### ScribbleHub book detail selectors fixed
- `bookTitle`: `"div.fi_header h2"` → `"div.fic_title"`. Old selector matched nothing (wrong class prefix).
- `bookCover`: `"div.fi_image img"` → `"div.fic_image img"`. Old selector matched nothing (wrong prefix).
- `bookAuthor`: `"span.auth_name_fic a"` → `"span.auth_name_fic"`. Was looking for `<a>` child that doesn't exist; text is direct in the span.

### Book card UI improvements
- `ContentScale.Crop` → `ContentScale.Fit` in `ExploreScreen.kt`, `LibraryScreen.kt`, `BookDetailScreen.kt` (shows whole cover without cropping).
- `.height(180.dp)` → `.aspectRatio(0.67f)` for consistent card proportions across all screen sizes.
- Added `background(Color(0xFF2A2A2A))` behind covers so white/transparent areas don't blend into dark background.
- **Result**: Covers display fully with consistent aspect ratio, no cropping.

### Gutenberg cover quality improved
- `HtmlParser.kt:39`: Added `.cover.small.jpg` → `.cover.medium.jpg` URL replacement.
- Before: 100×150px (3KB) thumbnail. After: 300×450px (27KB) medium quality.
- Safe global replacement — only Gutenberg uses `.small` in cover URLs.

### Verified on emulator (all 3 sources working)
| Source | Explore | Book Detail (title/author/cover) | Library |
|--------|---------|----------------------------------|---------|
| **RoyalRoad** | ✅ 20 books | ✅ | ✅ (from prev session) |
| **ScribbleHub** | ✅ 4 books | ✅ Rebirth of the Nephilim | ✅ cover+title |
| **Gutenberg** | ✅ 6 books | ✅ Moby Dick | ✅ cover+title |

- **Chapters parsing**: ScribbleHub was broken (showing 0), Gutenberg doesn't use chapters (full book).

### Changes (Jun 24 session 4) — Chapters fixed + Gutenberg full book mode

#### ScribbleHub chapters fixed
- Root cause: CSS selectors `tr.chapter_tr` / `a.tippy_chap` matched nothing. Real TOC HTML uses `<li class="toc_w"><a class="toc_a" href="...">Chapter N: Title</a></li>`.
- `chapterListItem`: `"tr.chapter_tr"` → `"li.toc_w"`
- `chapterTitle` & `chapterLink`: `"a.tippy_chap"` → `"a.toc_a"`
- `chapterContent`: `"div.chp_text"` → `"div.chp_raw"`
- Verified: "Rebirth of the Nephilim" shows **Chapters (15)** with real titles (Chapter 710: Being Upfront, etc.)
- TOC is paginated (15 per page by default); only page 1 is parsed.

#### Gutenberg full book mode
- Problem: Gutenberg books are full-length public domain works, not serialized web novels. Chapter splitting is inappropriate.
- Solution: `bookContentUrlPattern` field in `SourceConfig.kt` — derives full book content URL from detail URL using `{id}` placeholder.
- Pattern: `https://www.gutenberg.org/cache/epub/{id}/pg{id}-images.html`
- In `BookRepository.fetchBookDetail()`: if pattern is set and `parseChapterList()` returns empty, creates a single `ChapterItem(title="Full Book", url=derivedContentUrl)`.
- `chapterContent: "body"` → Reader downloads full book HTML, `stripHtml()` extracts text, paginates.
- Verified: Moby Dick opens in Reader as one continuous book → "Page 1 / 692".

#### Known issues
- **ScribbleHub explore covers**: 100×67px thumbnails (2.6KB). Larger version available only on book detail page.
- **ScribbleHub TOC pagination**: Only first 15 chapters shown. Full chapter list requires AJAX pagination not yet implemented.
- **Gutenberg anti-scraping warning**: HTML includes `<!-- DON'T USE THIS PAGE FOR SCRAPING -->` comment.
- **Gutenberg RDF feed** (`catalog.rdf.bz2`) not yet explored as alternative.

#### Files changed
| File | Change |
|------|--------|
| `SourceConfig.kt` | Added `bookContentUrlPattern: String = ""` |
| `sources_catalog.json` | Fixed ScribbleHub & RoyalRoad chapter selectors; added Gutenberg `bookContentUrlPattern` |
| `BookRepository.kt` | `fetchBookDetail()` creates synthetic "Full Book" chapter when pattern set |

## Changes (Jun 24 session 5) — RoyalRoad chapters fixed

### RoyalRoad chapters & Reader now working
- Root cause: CSS selector `td h4 a` matched nothing — chapter rows don't have `<h4>` elements. RoyalRoad removed the `<h4>` wrapper from chapter table rows.
- Fix: `chapterTitle` & `chapterLink`: `"td h4 a"` → `"td:first-child a"` in `sources_catalog.json`.
- Verified: Mother of Learning shows **Chapters (109)** with real titles. Chapter 1 opens in Reader → "Page 1 / 24".

### Verified on emulator (all 3 sources fully working)

| Source | Explore | Chapters | Reader |
|--------|---------|----------|--------|
| **RoyalRoad** | ✅ 20 books | ✅ 109 chapters | ✅ Chapter text |
| **ScribbleHub** | ✅ 4 books | ✅ 15 chapters | ✅ (from prev session) |
| **Gutenberg** | ✅ 6 books | ✅ Full Book mode | ✅ Page 1/692 |

## Changes (Jun 24 session 6) — ScribbleHub full AJAX chapter loading

### ScribbleHub now shows all 714 chapters
- Root cause: ScribbleHub TOC paginates at 15 chapters per page. Only latest 15 were server-rendered.
- Added `chapterListAjaxUrl` + `chapterListAjaxBody` fields to `SourceConfig.kt`.
- Added `fetchHtmlPost(url, body)` to `RemoteDataSource.kt` using Ktor `httpClient.post` with form-encoded body.
- In `BookRepository.fetchBookDetail()`: after parsing initial 15 chapters, makes a POST request to `wp-admin/admin-ajax.php` with `pagenum=-1` (Show All). Parses response using existing `parseChapterList()` → replaces initial 15 with all chapters.
- Series ID extracted from URL (`/series/664073/slug/` → `664073`).

### Files changed
| File | Change |
|------|--------|
| `RemoteDataSource.kt` | Added `fetchHtmlPost()` with POST support |
| `SourceConfig.kt` | Added `chapterListAjaxUrl`, `chapterListAjaxBody` |
| `sources_catalog.json` | Added AJAX config for ScribbleHub |
| `BookRepository.kt` | `fetchBookDetail()` now handles AJAX chapter loading |

### Verified on emulator
| Source | Explore | Chapters | Reader |
|--------|---------|----------|--------|
| **RoyalRoad** | ✅ 20 books | ✅ 109 chapters | ✅ Chapter text |
| **ScribbleHub** | ✅ 4 books | ✅ **714 chapters** (was 15) | ✅ (from prev sessions) |
| **Gutenberg** | ✅ 6 books | ✅ Full Book mode | ✅ Page 1/692 |

## Changes (Jun 24 session 7) — Chapter sort toggle

### Added chapter reverse toggle in BookDetailScreen
- Added `chaptersReversed` state + `toggleChapterOrder()` to `BookDetailViewModel.kt`.
- Added `SwapVert` icon button next to "Chapters (N)" header in `BookDetailScreen.kt`.
- Tapping toggles the chapter list between descending (newest first) and ascending (oldest first).
- Fixed latent bug in `BookRepository.addBookToLibrary()`: was using `mapIndexed` index as `order` instead of `ChapterItem.order`. Changed to preserve original chapter order regardless of display state.

### Files changed
| File | Change |
|------|--------|
| `BookDetailViewModel.kt` | Added `chaptersReversed` state + `toggleChapterOrder()` |
| `BookDetailScreen.kt` | Added `SwapVert` toggle button next to chapters header; fixed serial numbers to use `displayIndex + 1` instead of `chapter.order + 1` so they're sequential in both sort directions |
| `BookRepository.kt` | Fixed `addBookToLibrary()` to use `item.order` instead of index |

## Changes (Jun 25) — Save/restore page position + History direct open (COMPLETED)

### Scroll position save/restore — VERIFIED ✅
- Added `updateScrollPosition(id, scrollPosition)` query to `ChapterDao.kt`
- Added `saveChapterScrollPosition(chapterId, scrollPosition)` + `getChapterById(chapterId)` to `BookRepository.kt`
- Modified `ReaderViewModel.kt` with 3 changes:
  - `setPages()`: restores `currentPage` from `lastScrollPosition` via `pendingRestorePage` state
  - `onCleared()`: uses `runBlocking` to save scroll position before scope cancellation
  - `loadCurrentChapterContent()`: fetches fresh chapter from DB to get latest `lastScrollPosition`

### Two bugs found and fixed
1. **`onCleared()` race condition**: `viewModelScope.launch` was cancelled by `super.onCleared()` before the DB write could execute → fixed with `runBlocking`
2. **Stale in-memory data**: `setPages()` read `lastScrollPosition` from the stale in-memory `chapters` list (loaded once at init) instead of the DB → fixed by fetching fresh chapter from DB in `loadCurrentChapterContent()` and storing in `pendingRestorePage` state field

### Emulator test results (verified Jul 4)
- Re-added RoyalRoad via Browse Catalog after `pm clear`
- Added Mother of Learning to Library → opened reader → Page 1/24
- Navigated to Page 5/24 → exited → reopened → **Page 5/24 restored** ✅
- From History tab → tapped Mother of Learning → **opened directly to reader at restored page** ✅

### History tab: direct reader open
- Changed `HistoryScreen.kt` to navigate directly to `Screen.Reader.createRoute(book.id, book.lastChapterOrder ?: 0)` instead of `Screen.BookDetail.createRoute(book.id)`
- Reader already handles page restore via `lastScrollPosition` in `ChapterEntity`

### Files changed
| File | Change |
|------|--------|
| `ChapterDao.kt` | Added `updateScrollPosition(id, scrollPosition)` query |
| `BookRepository.kt` | Added `saveChapterScrollPosition()` + `getChapterById()` |
| `ReaderViewModel.kt` | Added `pendingRestorePage` to UiState; `setPages()` uses it; `onCleared()` uses `runBlocking`; `loadCurrentChapterContent()` fetches fresh chapter |
| `HistoryScreen.kt` | Changed onClick to navigate directly to Reader instead of BookDetail |

## Changes (Jul 5) — Phase 1 + Reader improvements

### Gutenberg author cleanup
- Added regex `,\s*\d{4}(-\d{4})?\s*$` to `HtmlParser.kt:67` to strip lifespan dates from author names
- "Melville, Herman, 1819-1891" → "Melville, Herman"

### Book status changer
- Added `updateBookStatus(bookId, status)` to `BookRepository.kt:193-196`
- Added `changeStatus(newStatus)` to `BookDetailViewModel.kt:128-133`
- Added FilterChip row (Reading / Plan to Read / Completed / Dropped) to `BookDetailScreen.kt:202-230` — visible in library mode between description and chapters

### ProGuard rules
- Expanded `proguard-rules.pro` from 3 lines to 58 lines
- Added keep rules for: Ktor CIO ServiceLoader, DataStore protobuf, Room entities/DAOs, Hilt generated code, enum `valueOf()`/`values()`, app data classes
- Prevents release build crashes (Ktor `IllegalStateException: No engine factory found`, DataStore protobuf crash)

### Real device testing (ZA222MVQYH)
- First build installed on physical Android device via `adb install`

### Top bar position fix
- Root cause: Double status bar inset — outer Scaffold in `NavGraph.kt` and inner Scaffold's TopAppBar both consumed top insets
- Fix: `NavGraph.kt` — outer Scaffold now uses `contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)`
- Only bottom+horizontal insets consumed by outer Scaffold; each screen's TopAppBar handles top inset

### Gutenberg lost line between pages
- Root cause: Constraints mismatch — Paginator computed pages using full screen height, but Text rendered in area 32dp shorter (padding). Last line(s) of each page overflowed and were clipped.
- Fix: `ReaderScreen.kt:214-215` — subtract display padding from constraints before passing to Paginator:
  - `horizontalPaddingPx = 40.dp` (20dp × 2)
  - `verticalPaddingPx = 32.dp` (16dp × 2)

### Kindle-style reader
- Bars (TopAppBar + page indicator) hidden by default for full-screen reading
- Double-tap anywhere → toggle bars visibility
- Bars auto-hide after 3 seconds
- Swipe anywhere → page turns (HorizontalPager native)
- Removed Prev/Next buttons from bottom bar (page indicator only)
- Bottom bar has opaque background (`MaterialTheme.colorScheme.surface`)
- Chapters drawer swipe disabled (`gesturesEnabled = false`) — chapters only open via button in top bar

### ScribbleHub browse blocked on real device
- Root cause: Cloudflare bot detection returns 403 Forbidden
- Ktor CIO engine has non-standard TLS fingerprint that Cloudflare detects
- Both explore URLs (`/series-ranking/` and `/latest-series/`) return 403
- **Not fixable** without switching to OkHttp engine (which caused hanging requests previously)

### Files changed
| File | Change |
|------|--------|
| `HtmlParser.kt` | Added regex to strip Gutenberg author dates |
| `BookRepository.kt` | Added `updateBookStatus()` method |
| `BookDetailViewModel.kt` | Added `changeStatus()` method, `BookStatus` import |
| `BookDetailScreen.kt` | Added FilterChip status picker, imports for `FilterChip`, `FilterChipDefaults`, `BookStatus`, `horizontalScroll` |
| `proguard-rules.pro` | Expanded to 58 lines with Ktor, DataStore, Room, Hilt, enum rules |
| `NavGraph.kt` | Added `WindowInsets` imports; outer Scaffold uses `contentWindowInsets` for bottom+horizontal only |
| `ReaderScreen.kt` | Kindle-style reader: double-tap toggle, removed Prev/Next buttons, opaque bottom bar, drawer gestures disabled, constraints fix |

## Changes (Jul 5 session 2) — Phase 2 bug fixes + features

### Fix: `updatedAt` never updated — library sort broken
- Root cause: `BookDao.getAllBooks()` sorts by `updatedAt DESC` but it was only set at creation time
- Fix: Added `updatedAt = System.currentTimeMillis()` to `ReaderViewModel.markChapterRead()` and `BookRepository.updateBookStatus()`
- Library now correctly sorts by most recently interacted-with book

### Fix: History delete removed entire book
- Root cause: `HistoryViewModel.deleteBook()` called `bookDao.deleteBookById()` which cascaded FK delete — removed book AND all chapters from DB
- Fix: Added `clearHistoryEntry(bookId)` and `clearAllHistory()` queries to `BookDao` that set `lastReadAt = NULL`
- History delete now only clears history entry, preserving the book in library
- Added `clearAllHistory()` method for bulk history clearing

### Fix: No delete confirmation dialogs
- All three screens (Library, History, Sources) deleted immediately on tap with zero confirmation
- Added `AlertDialog` confirmation before every delete action
- Library: "Remove from Library?" / History: "Clear History?" / Sources: "Delete Source?"
- Pattern: `var showDeleteDialog by remember { mutableStateOf<BookEntity?>(null) }` + AlertDialog

### Fix: `Converters.toBookStatus()` crash
- Root cause: `BookStatus.valueOf(value)` threw `IllegalArgumentException` on unrecognized strings
- Fix: Wrapped in try-catch, falls back to `PLAN_TO_READ`

### Fix: Bookmark saved chapter title instead of text snippet
- Root cause: `text = chapter.title` wasted the `BookmarkEntity.text` field
- Fix: Changed to `text = state.pages.getOrElse(state.currentPage) { chapter.title }.take(200)`

### Fix: Back stack cleared after adding to library
- Root cause: `popUpTo(navController.graph.startDestinationId)` destroyed all navigation context
- Fix: Changed to `navController.popBackStack()` — just pops BookDetail, returns to where user came from

### Feature: Font size persistence
- Font size now saved in DataStore preferences via `ThemeManager.setFontSize()` / `getFontSize()`
- `ReaderViewModel` loads saved font size on init, saves on every change
- Added `@ApplicationContext context: Context` to `ReaderViewModel` constructor

### Feature: Remove from Library on BookDetailScreen
- Added delete icon in TopAppBar (visible only in library mode) with confirmation dialog
- After removal, navigates back via `navController.popBackStack()`
- Added `isRemovedFromLibrary` state to `BookDetailViewModel.UiState`

### Feature: Clear All History button
- Added `Icons.Default.DeleteSweep` action in History TopAppBar
- Shows confirmation dialog, then calls `bookRepository.clearAllHistory()`

### Feature: Reader progress bar
- Added thin `LinearProgressIndicator` above page indicator in reader bottom bar
- Shows visual progress from 0% to 100% as user swipes through pages

### Feature: Go to Page jump dialog
- Made page indicator text tappable — opens `AlertDialog` with number input
- Validates page number is within 1..totalPages, then calls `viewModel.goToPage(page - 1)`

### Contrast fix: `#444444` low-contrast text
- Replaced `Color(0xFF444444)` with `AppColors.bodyText` in 3 locations:
  - `SearchScreen.kt:228` (no results subtitle)
  - `AddEditSourceScreen.kt:256` (form field placeholder)
  - `RepoManagerScreen.kt:101` (instruction text)

### Files changed
| File | Change |
|------|--------|
| `Converters.kt` | Try-catch in `toBookStatus()` |
| `BookDao.kt` | Added `clearHistoryEntry()`, `clearAllHistory()` queries |
| `BookRepository.kt` | Added `clearHistoryEntry()`, `clearAllHistory()`, `updatedAt` in `updateBookStatus()` |
| `ReaderViewModel.kt` | `updatedAt` in `markChapterRead()`, font size persistence, bookmark text snippet, `@ApplicationContext` |
| `HistoryViewModel.kt` | Renamed `deleteBook()` → `clearHistory()`, added `clearAllHistory()` |
| `HistoryScreen.kt` | Delete confirmation dialog, Clear All History button, imports |
| `LibraryScreen.kt` | Delete confirmation dialog, imports |
| `SourcesScreen.kt` | Delete confirmation dialog, imports |
| `BookDetailViewModel.kt` | `isRemovedFromLibrary` state, `removeFromLibrary()` method |
| `BookDetailScreen.kt` | Delete icon in TopAppBar, remove confirmation dialog, fixed back stack |
| `ThemeManager.kt` | Added `getFontSize()`, `setFontSize()` methods |
| `SearchScreen.kt` | `#444444` → `AppColors.bodyText` |
| `AddEditSourceScreen.kt` | `#444444` → `AppColors.bodyText` |
| `RepoManagerScreen.kt` | `#444444` → `AppColors.bodyText` |
| `ReaderScreen.kt` | Progress bar, Go to Page dialog, imports |

## Changes (Jul 5 session 3) — Cloudflare bypass architecture

### Ktor engine switch: CIO → OkHttp
- Root cause of ScribbleHub 403: Ktor CIO engine uses a non-standard TLS fingerprint (Java SSLEngine) that Cloudflare's bot detection identifies as non-browser traffic
- Switched from `ktor-client-cio` to `ktor-client-okhttp` in `gradle/libs.versions.toml`
- Updated `build.gradle.kts` to use `ktor-client-okhttp` dependency
- Updated `proguard-rules.pro`: replaced CIO `ServiceLoader` keep rules with OkHttp + okio rules (`okhttp3.internal.platform.**`, `okio.**`, `org.conscrypt.**`, `org.bouncycastle.**`, `org.openjsse.**`)
- OkHttp uses BoringSSL (same TLS stack as Chrome), producing a Chrome-compatible TLS fingerprint

### CloudflareInterceptor (core detection + bypass)
- **Detection**: Checks response for `code in [403, 503]` + `Server: cloudflare` header + challenge HTML elements (`challenge-error-title`, `challenge-error-text`, `challenge-running`)
- **IUAM auto-solve**: Background WebView loads the challenge page on main thread via `Handler(Looper.getMainLooper())`, waits up to 30s for `cf_clearance` cookie via `CountDownLatch`
- **Turnstile detection**: Was loading a separate background WebView and evaluating `document.documentElement.outerHTML` at `onPageStarted` — this didn't work (see Jul 6 section)
- **Cookie management**: Uses `AndroidCookieJar` to read/write `cf_clearance` from system `CookieManager`
- **Retry logic**: If existing `cf_clearance` cookie found, retries without WebView; if retry still gets Cloudflare challenge, clears stale cookie and falls through

### AndroidCookieJar
- Bridges `android.webkit.CookieManager` ↔ OkHttp `CookieJar`
- `loadForRequest()`: reads cookies from `CookieManager.getInstance().getCookie()` for the request URL
- `saveFromResponse()`: parses `Set-Cookie` headers and stores via `CookieManager.getInstance().setCookie()`
- `flush()`: calls `CookieManager.getInstance().flush()` for persistence
- `remove(url, cookieNames)`: removes specific cookies for a domain
- `removeAll()`: clears all cookies

### TurnstileBypassException
- Simple exception class that carries the URL string
- Thrown as cause of `IOException("Cloudflare Turnstile requires verification", TurnstileBypassException(url))`
- ViewModels extract the URL via `findTurnstileException()` recursive cause-chain search

### CloudflareVerifyActivity
- Standalone `ComponentActivity` (not Fragment-based) with Compose UI
- Contains a `WebView` that loads the Cloudflare-protected URL
- User manually solves the Turnstile challenge in the WebView
- `onPageFinished` calls `CookieManager.getInstance().flush()` to persist cookies
- Registered in `AndroidManifest.xml` with `exported="false"`
- Launched via `ActivityResultContracts.StartActivityForResult` from screens

### ViewModel changes (ExploreViewModel, SearchViewModel, BookDetailViewModel)
- Added `var turnstileUrl by mutableStateOf<String?>(null)` to UiState
- Added `fun retryAfterTurnstile()` — clears `turnstileUrl`, re-fetches data
- Added `fun findTurnstileException(throwable: Throwable?): TurnstileBypassException?` — recursively searches exception cause chain

### UI changes (ExploreScreen, SearchScreen, BookDetailScreen)
- Replaced "Open in Chrome" dialog with "Solve in App" dialog
- "Solve in App" launches `CloudflareVerifyActivity` via `ActivityResultLauncher`
- `LaunchedEffect(Unit)` watches for `turnstileUrl` → shows dialog
- `rememberLauncherForActivityResult` handles Activity result → calls `viewModel.retryAfterTurnstile()`

### Deleted files
| File | Reason |
|------|--------|
| `CloudflareBypassState.kt` | Replaced by simpler `turnstileUrl` state in ViewModels |
| `CloudflareWebViewFetcher.kt` | Replaced by `CloudflareInterceptor` (OkHttp-based) |
| `CloudflareBypassDialog.kt` | Replaced by inline dialogs in each screen |
| `CloudflareViewModel` in `MainActivity.kt` | Removed — logic moved to per-screen ViewModels |

### Files changed
| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | `ktor-client-cio` → `ktor-client-okhttp` |
| `build.gradle.kts` | Updated Ktor dependency to OkHttp engine |
| `proguard-rules.pro` | CIO rules → OkHttp + okio + conscrypt + bouncycastle rules |
| `di/NetworkModule.kt` | `HttpClient(OkHttp)` with preconfigured `OkHttpClient`, `CloudflareInterceptor` injected |
| `network/CloudflareInterceptor.kt` | New: detection, IUAM auto-solve, Turnstile detection, retry logic |
| `network/AndroidCookieJar.kt` | New: CookieManager ↔ OkHttp bridge |
| `network/TurnstileBypassException.kt` | New: carries URL for Turnstile fallback |
| `network/CloudflareVerifyActivity.kt` | New: in-app WebView for manual Turnstile solving |
| `AndroidManifest.xml` | Registered `CloudflareVerifyActivity` |
| `ui/explore/ExploreViewModel.kt` | Added `turnstileUrl` state, `retryAfterTurnstile()`, `findTurnstileException()` |
| `ui/explore/ExploreScreen.kt` | "Solve in App" dialog via `ActivityResultLauncher` |
| `ui/search/SearchViewModel.kt` | Same Turnstile state + retry |
| `ui/search/SearchScreen.kt` | Same "Solve in App" dialog |
| `ui/screens/BookDetailViewModel.kt` | Same Turnstile state + retry |
| `ui/screens/BookDetailScreen.kt` | Same "Solve in App" dialog |
| `MainActivity.kt` | Removed `CloudflareViewModel`, cleaned up imports |

## Changes (Jul 6) — Cloudflare Turnstile bypass working

### What didn't work (approach history)

#### 1. CIO engine alone
- **Attempt**: Use Ktor CIO engine (default) to fetch ScribbleHub
- **Result**: 403 Forbidden — Cloudflare detects CIO's non-standard TLS fingerprint
- **Why it failed**: CIO uses Java `SSLEngine` which produces a TLS fingerprint (JA3 hash) that doesn't match any real browser. Cloudflare's bot detection fingerprints this immediately.

#### 2. OkHttp engine alone (without interceptor)
- **Attempt**: Switch to `ktor-client-okhttp` for Chrome-compatible TLS fingerprint
- **Result**: 403 Forbidden — TLS fingerprint now matches Chrome, but Cloudflare still serves the IUAM/Turnstile challenge page
- **Why it failed**: TLS fingerprint alone isn't enough. Cloudflare also checks for JavaScript execution, `X-Requested-With` header, and other browser signals. An OkHttp client without a proper interceptor just gets the challenge page HTML.

#### 3. Desktop Chrome UA in main client + default Android UA in Activity WebView
- **Attempt**: Main client uses `Chrome/125 (Windows NT 10.0; Win64; x64)` desktop UA; Activity WebView uses default Android Chrome UA (`Chrome/149 (Linux; Android 14)`)
- **Result**: Activity loads page (200), but main client retry still gets 403
- **Why it failed**: Cloudflare ties the `cf_clearance` cookie to the browser session's User-Agent. Cookie issued for Android UA → rejected when main client retries with desktop UA. The UA mismatch makes Cloudflare think it's a different client.

#### 4. `shouldInterceptRequest` intercepting ALL requests via OkHttp
- **Attempt**: Activity's `shouldInterceptRequest` intercepted main frame + sub-resources, forwarded all to `nonCloudflareClient` (plain OkHttp without Cloudflare interceptor)
- **Result**: "unable to host www.scribblehub.com" error
- **Why it failed**: The `nonCloudflareClient` (without interceptor) was used for sub-resources like scripts, images, CSS. Some of these requests failed because the OkHttp client didn't have the right headers, cookies, or referrer that the WebView normally sends. The WebView's built-in network stack handles sub-resources correctly; bypassing it broke the page.

#### 5. `detectTurnstile()` loading separate background WebView
- **Attempt**: `CloudflareInterceptor.detectTurnstile()` created a background WebView, loaded the URL, evaluated `document.documentElement.outerHTML` at `onPageStarted` to check for "turnstile" keyword
- **Result**: Always returned `false` — Turnstile was never detected → went to IUAM auto-solve path instead
- **Why it failed**: The `onPageStarted` callback fires before JavaScript executes. The Turnstile challenge HTML doesn't contain the word "turnstile" in the initial HTML — it's loaded dynamically via `<script src="challenges.cloudflare.com/cdn-cgi/challenge-platform/...">`. By the time `onPageStarted` fires, the Turnstile scripts haven't loaded yet. The 10-second timeout expired before detection could succeed.

#### 6. Stale `cf_clearance` cookie without clearing
- **Attempt**: When existing `cf_clearance` cookie found, retry request with it; if still 403, throw exception
- **Result**: Stale cookie retry always returned 403, but the interceptor returned the 403 response directly to the caller without falling through to Turnstile/IUAM handling
- **Why it failed**: The original code had `return chain.proceed(request)` for the stale cookie retry path. When the retry still got Cloudflare challenge (stale cookie rejected), it returned the 403 response. The caller (RemoteDataSource) saw 403 and logged "Cloudflare interceptor will handle it" but the interceptor never got a chance to detect Turnstile or solve IUAM because it already returned.

#### 7. `BookyapaApp.getPackageName()` override without webkit stack check
- **Attempt**: Override `getPackageName()` to return `"com.android.chrome"` for all `org.chromium.*` calls → spoofs `X-Requested-With` header
- **Result**: App crashed on startup with `InvocationTargetException`
- **Why it failed**: `WebViewFactory.getProvider()` calls `getPackageName()` during WebView initialization (before any `org.chromium.*` classes are loaded). Our override returned `com.android.chrome` → system tried to find Chrome package → `InvocationTargetException`. The fix: check if any `android.webkit.*` class is in the call stack and return the real package name during WebView init.

### What worked (the final architecture — 3 layers)

#### Layer 1: Package name spoofing (`BookyapaApp.getPackageName()`)
- Overrides `Application.getPackageName()` to return `"com.android.chrome"` for `org.chromium.*` calls
- This makes the `X-Requested-With` header (auto-added by WebView) report `com.android.chrome` instead of `com.bookyapa.app`
- Cloudflare sees a real Chrome browser instead of an embedded WebView
- **Critical**: Includes `android.webkit.*` stack check — if any `android.webkit.*` class is in the call stack, returns real package name to avoid crash during WebView init

#### Layer 2: Turnstile detection from response body (`CloudflareInterceptor.detectTurnstile()`)
- Instead of loading a separate background WebView (which timed out), directly checks the Cloudflare challenge HTML body for:
  - `challenges.cloudflare.com` (script source domain)
  - `cf-turnstile` (CSS class/ID)
  - `turnstile` (keyword)
- This is fast (no WebView overhead), reliable (body is already available from the initial 403 response), and correctly detects Turnstile challenges

#### Layer 3: UA matching (`NetworkModule.kt` + `CloudflareVerifyActivity`)
- **The key fix**: Both the main HTTP client and the Activity WebView use the **same User-Agent**
- `NetworkModule.kt`: `val userAgent = WebSettings.getDefaultUserAgent(context)` — gets the real Android Chrome UA at runtime
- `CloudflareVerifyActivity`: No custom `userAgentString` set — WebView uses its default (same Android Chrome UA)
- When Activity solves Turnstile → `cf_clearance` cookie issued for Android Chrome UA
- When main client retries → same Android Chrome UA → cookie accepted by Cloudflare
- **Why this matters**: Cloudflare ties `cf_clearance` to the browser session fingerprint. If the UA differs between the challenge-solving session and the retry, the cookie is rejected.

### The complete flow (what happens when user taps ScribbleHub Browse)
1. `ExploreViewModel` calls `fetchExploreBooks()` → `RemoteDataSource.fetchHtml()` → Ktor/OkHttp makes request
2. `CloudflareInterceptor.intercept()` detects Cloudflare challenge (403 + server header + HTML elements)
3. Clears any stale `cf_clearance` cookie
4. `detectTurnstile()` checks response body → finds `challenges.cloudflare.com` → returns `true`
5. Throws `IOException("Cloudflare Turnstile requires verification", TurnstileBypassException(url))`
6. `ExploreViewModel` catches exception, sets `turnstileUrl = url`
7. `ExploreScreen` shows "Cloudflare Verification Required" dialog with "Solve in App" button
8. User taps "Solve in App" → launches `CloudflareVerifyActivity`
9. Activity loads URL in WebView (same Android Chrome UA) → Cloudflare serves Turnstile challenge
10. Turnstile auto-solves in the real Android WebView → `cf_clearance` cookie set in `CookieManager`
11. User taps back → `retryAfterTurnstile()` called → clears `turnstileUrl`, re-fetches
12. `CloudflareInterceptor` detects existing `cf_clearance` cookie → retries with cookie → gets 200 ✅

### Verified on real device (ZA222MVQYH)
| Source | Explore | Chapters | Reader | Cloudflare |
|--------|---------|----------|--------|------------|
| **RoyalRoad** | ✅ 20 books | ✅ 109 chapters | ✅ Chapter text | N/A (no CF) |
| **ScribbleHub** | ✅ 4 books | ✅ 714 chapters | ✅ Chapter text | ✅ Turnstile bypass working |
| **Gutenberg** | ✅ 6 books | ✅ Full Book mode | ✅ Page 1/692 | N/A (no CF) |

### Known limitations
- **ScribbleHub TOC pagination**: Only first 15 chapters shown. Full chapter list requires AJAX pagination (already implemented via `chapterListAjaxUrl`).
- **Turnstile auto-solve time**: Activity may need 5-10 seconds for Turnstile to auto-solve. If it doesn't auto-solve (rare), user may need to tap the Turnstile checkbox manually.
- **ScribbleHub explore covers**: 100×67px thumbnails. Larger version available only on book detail page.
- **Package name spoofing is fragile**: If Cloudflare changes how they detect embedded WebViews (e.g., checking `navigator.webdriver` property), this layer may stop working.

### Files changed (Jul 6)
| File | Change |
|------|--------|
| `BookyapaApp.kt` | Added `getPackageName()` override with `android.webkit.*` stack check |
| `network/CloudflareInterceptor.kt` | Rewrote `detectTurnstile()` to check response body directly; added stale cookie clearing on retry failure |
| `network/CloudflareVerifyActivity.kt` | Removed hardcoded desktop Chrome UA, uses default WebView UA; simplified to plain WebView without `shouldInterceptRequest` |
| `di/NetworkModule.kt` | Changed from hardcoded desktop Chrome UA to `WebSettings.getDefaultUserAgent(context)` |

## To do next session

### Bug: HistoryScreen.kt brace nesting broken
- **Reported**: "clearing the progress in the history tab doesn't change anything"
- **Root cause**: The dialogs (`showDeleteDialog?.let` and `showClearAllDialog`) were placed inside the `when` block's `else` branch due to mismatched braces during the edit
- **Evidence**: Lines 189-190 have extra closing braces (`    }` and `}`). Line 142's `}` closes LazyColumn but at wrong indentation (8 spaces instead of 16)
- **Fix**: Move dialogs outside the `when` block, remove extra braces on lines 189-190, fix indentation
- **File**: `HistoryScreen.kt:142-190`
- **Verify**: Tap delete icon on a history entry → dialog should appear → confirm → book should disappear from list

### Feature: Per-book font size (not global)
- **Reported**: "Font size persistence saves it for all the books i read instead of separate settings for separate books"
- **Current**: Global `reader_font_size` in DataStore via `ThemeManager` — same for all books
- **Plan**:
  1. Add `val fontSize: Int = 0` to `BookEntity` (0 = use default 16)
  2. Add `@Query("UPDATE books SET fontSize = :fontSize WHERE id = :bookId")` to `BookDao`
  3. Add `suspend fun updateBookFontSize(bookId: Long, fontSize: Int)` to `BookRepository`
  4. `ReaderViewModel.init`: load `book.fontSize` instead of `ThemeManager.getFontSize()`. If 0, use 16.
  5. `ReaderViewModel.updateFontSize()`: call `bookRepository.updateBookFontSize(bookId, newSize)` instead of `ThemeManager.setFontSize()`
  6. Remove `ThemeManager.getFontSize()` / `setFontSize()` (or keep as default for new books)
  7. DB version bump — `fallbackToDestructiveMigration()` handles it (same as `pm clear`)
- **Files**: `BookEntity.kt`, `BookDao.kt`, `BookRepository.kt`, `ReaderViewModel.kt`, `ThemeManager.kt`

### Other planned items
1. Theme colors across screens (141 hardcoded colors → MaterialTheme.colorScheme)
2. ScribbleHub TOC pagination (only page 1 shown; full chapter list requires AJAX pagination with page numbers)
3. Settings screen improvements (theme persistence, about page, clear data)

### Important emulator notes
- `pm clear com.bookyapa.app` wipes ALL sources from Room DB
- After pm clear: re-add via **Sources → Browse Catalog → tap each "+ Add" button**
- Browse Catalog button requires `uiautomator dump` to get exact coords — visual tapping doesn't work reliably
- Bottom nav coords: Library(100,2232), History(320,2232), Search(540,2232), Explore(760,2232), Sources(980,2232)
