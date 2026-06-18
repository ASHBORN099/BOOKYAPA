import React, { useEffect, useRef, useState, useCallback, useMemo } from "react";
import {
  ActivityIndicator,
  Platform,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { WebView } from "react-native-webview";
import { useStreamingText } from "../hooks/use-streaming-text";
import { useEpubReader } from "../hooks/use-epub-reader";
import { detectChapters, DetectedChapter } from "../services/chapter-detector";
import { looksLikeHtml, stripHtml } from "../services/html-stripper";
import {
  stripPageMarkers,
  stripGutenbergBoilerplate,
  reflowHardBreaks,
  breakLongWords,
  formatParagraphs,
} from "../services/text-cleaner";
import { buildWebViewHtml } from "../services/webview-html";
import {
  ReaderStoreProvider,
  useReaderStore,
  THEME_BG_COLORS,
} from "../hooks/use-reader-store";
import { getProgress, upsertProgress } from "../db/operations";

function ReaderContent() {
  const params = useLocalSearchParams();
  const safe = (key: string) => {
    const v = params[key];
    return Array.isArray(v) ? v[0] : v;
  };
  const id = (safe("id") ?? "") as string;
  const title = (safe("title") ?? "") as string;
  const bookUrl = (safe("bookUrl") ?? "") as string;
  const coverUrl = safe("coverUrl") as string | undefined;
  const epubUrlParam = safe("epubUrl") as string | undefined;
  const router = useRouter();
  const insets = useSafeAreaInsets();

  const {
    state,
    toggleMenu,
    setShowMenu,
    setReadingMode,
    setThemeBg,
    setSettings,
    increaseFont,
    decreaseFont,
    increaseLineHeight,
    decreaseLineHeight,
  } = useReaderStore();
  const { fontSize, lineHeight, readingMode, themeBg } = state.settings;
  const colors = THEME_BG_COLORS[themeBg];

  const isEpub = !!epubUrlParam && epubUrlParam.length > 0;
  const pageMode = readingMode === "page";
  const [epubFailed, setEpubFailed] = useState(false);
  const shouldUseEpub = isEpub && !epubFailed;

  const epubReader = useEpubReader(shouldUseEpub ? epubUrlParam : null);

  useEffect(() => {
    if (isEpub && epubReader.error) {
      console.warn("EPUB fallback:", epubReader.error);
      setEpubFailed(true);
    }
  }, [isEpub, epubReader.error]);

  const [retryFallbackIdx, setRetryFallbackIdx] = useState(0);

  const fallbackUrls = useMemo(() => {
    const iaMatch = bookUrl?.match(/\/download\/([^/]+)\/\1[_\.]djvu\.txt$/);
    if (!iaMatch) return [bookUrl];
    const iaId = iaMatch[1];
    return [
      `https://archive.org/download/${iaId}/${iaId}_djvu.txt`,
      `https://archive.org/download/${iaId}/${iaId}.djvu.txt`,
      `https://archive.org/download/${iaId}/${iaId}_text.txt`,
      `https://archive.org/download/${iaId}/${iaId}.txt`,
    ];
  }, [bookUrl]);

  const activeUrl = fallbackUrls[retryFallbackIdx] || bookUrl;

  const {
    text: rawText,
    progress: streamProgress,
    isStreaming,
    error: streamError,
    retry,
  } = useStreamingText({ url: shouldUseEpub ? null : activeUrl, enabled: !shouldUseEpub });

  const bookText = useMemo(() => {
    if (shouldUseEpub) return "";
    const cleaned = looksLikeHtml(rawText) ? stripHtml(rawText) : rawText;
    const withoutBoilerplate = stripGutenbergBoilerplate(cleaned);
    const withoutMarkers = stripPageMarkers(withoutBoilerplate, title);
    const reflowed = reflowHardBreaks(withoutMarkers);
    const withBreaks = breakLongWords(reflowed);
    return formatParagraphs(withBreaks);
  }, [rawText, title, shouldUseEpub]);

  const [chapters, setChapters] = useState<DetectedChapter[]>([]);
  const [currentChapterIdx, setCurrentChapterIdx] = useState(0);
  const lastChapterTextLen = useRef(0);

  useEffect(() => {
    if (shouldUseEpub) return;
    if (!bookText) return;
    if (bookText.length - lastChapterTextLen.current < 5000) return;
    lastChapterTextLen.current = bookText.length;
    const detected = detectChapters(bookText);
    setChapters((prev) => (detected.length > prev.length ? detected : prev));
  }, [bookText, shouldUseEpub]);

  const effectiveChapters = useMemo(() => {
    if (shouldUseEpub && epubReader.chapters.length > 0) {
      return epubReader.chapters.map((c) => ({
        title: c.title,
        startOffset: c.offset,
      }));
    }
    return chapters;
  }, [shouldUseEpub, epubReader.chapters, chapters]);

  const [currentOffset, setCurrentOffset] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const savedOffsetRef = useRef(0);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const saveProgress = useCallback(
    (offset: number) => {
      if (!id) return;
      savedOffsetRef.current = offset;
      if (debounceRef.current) clearTimeout(debounceRef.current);
      debounceRef.current = setTimeout(() => {
        try {
          upsertProgress({
            bookId: id,
            bookTitle: title || "Unknown",
            coverUrl: coverUrl || null,
            bookUrl: bookUrl || null,
            epubUrl: epubUrlParam || null,
            chapterIndex: 0,
            characterOffset: offset,
            fontSize,
            lineHeight,
            readingMode,
            updatedAt: Date.now(),
          });
        } catch (e) {
          console.error("Failed to save progress:", e);
        }
      }, 500);
    },
    [id, title, coverUrl, bookUrl, epubUrlParam, fontSize, lineHeight, readingMode],
  );

  const webviewHtml = useMemo(() => {
    const content = shouldUseEpub && !epubReader.error ? epubReader.html : bookText;
    const totalLen = shouldUseEpub && !epubReader.error ? epubReader.textContent.length : bookText.length;
    if (!content) return "";
    return buildWebViewHtml({
      content,
      textContent: shouldUseEpub ? (epubReader.textContent || '') : (bookText || ''),
      isEpub: shouldUseEpub,
      fontSize,
      lineHeight,
      themeBg,
      brightness: state.settings.brightness,
      pageMode,
      totalTextLength: totalLen,
      savedOffset: savedOffsetRef.current,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    shouldUseEpub,
    epubReader.html,
    epubReader.textContent.length,
    bookText,
    pageMode,
  ]);

  const webViewSource = useMemo(() => ({ html: webviewHtml }), [webviewHtml]);

  const webViewRef = useRef<WebView>(null);

  const handleMessage = useCallback(
    (event: { nativeEvent: { data: string } }) => {
      try {
        const data = JSON.parse(event.nativeEvent.data);
        if (data.type === "progress") {
          setCurrentOffset(data.offset);
          setCurrentPage(data.page);
          setTotalPages(data.totalPages);
          if (effectiveChapters.length > 0) {
            let idx = 0;
            for (let i = effectiveChapters.length - 1; i >= 0; i--) {
              if (data.offset >= effectiveChapters[i].startOffset) {
                idx = i;
                break;
              }
            }
            setCurrentChapterIdx(idx);
          }
          if (data.offset > 0) saveProgress(data.offset);
        } else if (data.type === "toggleMenu") {
          toggleMenu();
        }
      } catch {}
    },
    [effectiveChapters, saveProgress, toggleMenu],
  );

  const prevSettingsRef = useRef({
    fontSize,
    lineHeight,
    themeBg,
    brightness: state.settings.brightness,
  });

  useEffect(() => {
    if (!webviewHtml || !webViewRef.current) return;
    const prev = prevSettingsRef.current;
    if (
      prev.fontSize !== fontSize ||
      prev.lineHeight !== lineHeight ||
      prev.themeBg !== themeBg ||
      prev.brightness !== state.settings.brightness
    ) {
      const tc = THEME_BG_COLORS[themeBg];
      webViewRef.current.postMessage(
        JSON.stringify({
          type: "updateSettings",
          fontSize,
          lineHeight,
          themeColors: { text: tc.text, bg: tc.bg },
          brightness: state.settings.brightness,
        }),
      );
    }
    prevSettingsRef.current = {
      fontSize,
      lineHeight,
      themeBg,
      brightness: state.settings.brightness,
    };
  }, [
    fontSize,
    lineHeight,
    themeBg,
    state.settings.brightness,
    webviewHtml,
  ]);

  const jumpToChapter = useCallback(
    (offset: number) => {
      webViewRef.current?.postMessage(
        JSON.stringify({ type: "goToOffset", offset }),
      );
      setShowMenu(false);
    },
    [setShowMenu],
  );

  const hasRestoredRef = useRef(false);

  useEffect(() => {
    if (!webviewHtml || !webViewRef.current || hasRestoredRef.current) return;
    hasRestoredRef.current = true;
    if (id) {
      const saved = getProgress(id);
      if (saved) {
        setSettings({
          fontSize: saved.fontSize,
          lineHeight: saved.lineHeight,
          readingMode: saved.readingMode as "page" | "scroll",
        });
        if (saved.characterOffset > 0) {
          savedOffsetRef.current = saved.characterOffset;
          setCurrentOffset(saved.characterOffset);
          webViewRef.current?.postMessage(
            JSON.stringify({ type: "goToOffset", offset: saved.characterOffset }),
          );
        }
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [webviewHtml]);

  const handleReaderRetry = useCallback(() => {
    const nextIdx = Math.min(retryFallbackIdx + 1, fallbackUrls.length - 1);
    setRetryFallbackIdx(nextIdx);
    if (nextIdx === retryFallbackIdx) {
      retry();
    }
  }, [retryFallbackIdx, fallbackUrls.length, retry]);

  if ((!bookUrl && !epubUrlParam) || !id) {
    return (
      <View style={[styles.centered, { backgroundColor: colors.bg }]}>
        <Text style={[styles.errorText, { color: "#ff6b6b" }]}>
          Invalid book data
        </Text>
        <TouchableOpacity style={styles.retryBtn} onPress={() => router.back()}>
          <Text style={styles.retryBtnText}>Go Back</Text>
        </TouchableOpacity>
      </View>
    );
  }

  if (!shouldUseEpub && streamError && !rawText) {
    const attemptLabel =
      fallbackUrls.length > 1
        ? ` (attempt ${retryFallbackIdx + 1}/${fallbackUrls.length})`
        : "";
    return (
      <View style={[styles.centered, { backgroundColor: colors.bg }]}>
        <Text style={[styles.errorText, { color: "#ff6b6b" }]}>
          {streamError}
          {attemptLabel}
        </Text>
        <TouchableOpacity
          style={styles.retryBtn}
          onPress={handleReaderRetry}
          activeOpacity={0.7}
        >
          <Text style={styles.retryBtnText}>Retry</Text>
        </TouchableOpacity>
      </View>
    );
  }

  if (epubFailed && !bookUrl) {
    return (
      <View style={[styles.centered, { backgroundColor: colors.bg }]}>
        <Text style={[styles.errorText, { color: "#ff6b6b" }]}>
          This book requires EPUB format and could not be loaded.
        </Text>
        <TouchableOpacity style={styles.retryBtn} onPress={() => router.back()}>
          <Text style={styles.retryBtnText}>Go Back</Text>
        </TouchableOpacity>
      </View>
    );
  }

  const isLoadingEpub = shouldUseEpub && epubReader.isLoading && !epubReader.html;
  const isLoadingText = !shouldUseEpub && (!rawText || isStreaming);
  const isContentReady = shouldUseEpub ? !!epubReader.html : (!!bookText && !isStreaming);

  if ((isLoadingEpub || isLoadingText) && !isContentReady) {
    return (
      <View
        style={[
          styles.centered,
          { backgroundColor: colors.bg },
        ]}
      >
        <ActivityIndicator size="large" color="#008080" />
        <Text style={[styles.loadingText, { color: colors.text }]}>
          {shouldUseEpub ? "Loading EPUB..." : "Loading book..."}
        </Text>
        {!shouldUseEpub && isStreaming && (
          <View
            style={[
              styles.progressBar,
              { backgroundColor: "rgba(255,255,255,0.1)", marginTop: 20 },
            ]}
          >
            <View
              style={[
                styles.progressFill,
                {
                  backgroundColor: "#00ffff",
                  width: `${Math.max(streamProgress * 100, 5)}%`,
                },
              ]}
            />
          </View>
        )}
      </View>
    );
  }

  if (!isContentReady) {
    return (
      <View style={[styles.centered, { backgroundColor: colors.bg }]}>
        <Text style={[{ color: colors.text }]}>No content available</Text>
      </View>
    );
  }

  return (
    <View style={[styles.root, { backgroundColor: colors.bg }]}>
      <View style={{ flex: 1, paddingTop: insets.top }}>
        <WebView
          ref={webViewRef}
          source={webViewSource}
          onMessage={handleMessage}
          style={{ backgroundColor: "transparent" }}
          javaScriptEnabled
          domStorageEnabled
          originWhitelist={["*"]}
          bounces={false}
          showsVerticalScrollIndicator={false}
          showsHorizontalScrollIndicator={false}
        />
        {!pageMode && (
          <TouchableOpacity
            style={styles.floatingMenuBtn}
            onPress={toggleMenu}
            activeOpacity={0.7}
          >
            <Text style={styles.floatingMenuBtnText}>☰</Text>
          </TouchableOpacity>
        )}

        {!shouldUseEpub && isStreaming && (
          <View
            style={[
              styles.progressBar,
              { backgroundColor: "rgba(255,255,255,0.1)" },
            ]}
          >
            <View
              style={[
                styles.progressFill,
                {
                  backgroundColor: "#00ffff",
                  width: `${Math.max(streamProgress * 100, 5)}%`,
                },
              ]}
            />
          </View>
        )}
      </View>

      <ReaderMenu
        visible={state.showMenu}
        onClose={() => setShowMenu(false)}
        fontSize={fontSize}
        lineHeight={lineHeight}
        readingMode={readingMode}
        themeBg={themeBg}
        brightness={state.settings.brightness}
        onFontIncrease={increaseFont}
        onFontDecrease={decreaseFont}
        onLineHeightIncrease={increaseLineHeight}
        onLineHeightDecrease={decreaseLineHeight}
        onToggleMode={() =>
          setReadingMode(pageMode ? "scroll" : "page")
        }
        onSetTheme={setThemeBg}
        onSetBrightness={(v) =>
          setSettings({ brightness: Math.max(0.1, Math.min(1, v)) })
        }
        pageInfo={
          pageMode && totalPages > 0
            ? `Page ${currentPage + 1} of ${totalPages}`
            : undefined
        }
        chapters={effectiveChapters}
        currentChapterIdx={currentChapterIdx}
        onJumpToChapter={jumpToChapter}
        percentComplete={
          currentOffset > 0
            ? Math.round(
                (currentOffset /
                  (shouldUseEpub
                    ? epubReader.textContent.length
                    : bookText.length)) *
                  100,
              )
            : 0
        }
        wordsRead={Math.floor(currentOffset / 5)}
      />
    </View>
  );
}

interface ReaderMenuProps {
  visible: boolean;
  onClose: () => void;
  fontSize: number;
  lineHeight: number;
  readingMode: "page" | "scroll";
  themeBg: "white" | "sepia" | "dark";
  brightness: number;
  onFontIncrease: () => void;
  onFontDecrease: () => void;
  onLineHeightIncrease: () => void;
  onLineHeightDecrease: () => void;
  onToggleMode: () => void;
  onSetTheme: (bg: "white" | "sepia" | "dark") => void;
  onSetBrightness: (v: number) => void;
  pageInfo?: string;
  chapters: DetectedChapter[];
  currentChapterIdx: number;
  onJumpToChapter: (offset: number) => void;
  percentComplete: number;
  wordsRead: number;
}

function ReaderMenu({
  visible,
  onClose,
  fontSize,
  lineHeight,
  readingMode,
  themeBg,
  brightness,
  onFontIncrease,
  onFontDecrease,
  onLineHeightIncrease,
  onLineHeightDecrease,
  onToggleMode,
  onSetTheme,
  onSetBrightness,
  pageInfo,
  chapters,
  currentChapterIdx,
  onJumpToChapter,
  percentComplete,
  wordsRead,
}: ReaderMenuProps) {
  if (!visible) return null;
  return (
    <View style={styles.overlay}>
      <TouchableOpacity style={styles.overlayClose} onPress={onClose} />
      <View style={styles.menuPanel}>
        <Text style={styles.menuTitle}>Settings</Text>

        <View style={styles.menuRow}>
          <Text style={styles.menuLabel}>Font Size</Text>
          <View style={styles.menuControls}>
            <TouchableOpacity style={styles.menuBtn} onPress={onFontDecrease}>
              <Text style={styles.menuBtnText}>A-</Text>
            </TouchableOpacity>
            <Text style={styles.menuValue}>{fontSize}</Text>
            <TouchableOpacity style={styles.menuBtn} onPress={onFontIncrease}>
              <Text style={styles.menuBtnText}>A+</Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.menuRow}>
          <Text style={styles.menuLabel}>Line Spacing</Text>
          <View style={styles.menuControls}>
            <TouchableOpacity
              style={styles.menuBtn}
              onPress={onLineHeightDecrease}
            >
              <Text style={styles.menuBtnText}>−</Text>
            </TouchableOpacity>
            <Text style={styles.menuValue}>{lineHeight.toFixed(1)}</Text>
            <TouchableOpacity
              style={styles.menuBtn}
              onPress={onLineHeightIncrease}
            >
              <Text style={styles.menuBtnText}>+</Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.menuRow}>
          <Text style={styles.menuLabel}>Theme</Text>
          <View style={styles.menuControls}>
            {(["white", "sepia", "dark"] as const).map((bg) => (
              <TouchableOpacity
                key={bg}
                style={[
                  styles.themeDot,
                  {
                    backgroundColor: THEME_BG_COLORS[bg].bg,
                    borderWidth: themeBg === bg ? 2 : 0,
                    borderColor: themeBg === bg ? "#008080" : "transparent",
                  },
                ]}
                onPress={() => onSetTheme(bg)}
              />
            ))}
          </View>
        </View>

        <View style={styles.menuRow}>
          <Text style={styles.menuLabel}>Mode</Text>
          <TouchableOpacity style={styles.modeToggle} onPress={onToggleMode}>
            <Text style={styles.modeToggleText}>
              {readingMode === "page" ? "Page Flip" : "Scroll"}
            </Text>
          </TouchableOpacity>
        </View>

        <View style={styles.menuRow}>
          <Text style={styles.menuLabel}>Brightness</Text>
          <View style={styles.brightnessControls}>
            <TouchableOpacity
              style={styles.menuBtn}
              onPress={() => onSetBrightness(brightness - 0.1)}
            >
              <Text style={styles.menuBtnText}>−</Text>
            </TouchableOpacity>
            <View style={styles.brightnessTrack}>
              <View
                style={[
                  styles.brightnessFill,
                  { width: `${brightness * 100}%` },
                ]}
              />
            </View>
            <TouchableOpacity
              style={styles.menuBtn}
              onPress={() => onSetBrightness(brightness + 0.1)}
            >
              <Text style={styles.menuBtnText}>+</Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.statsRow}>
          <View style={styles.statBox}>
            <Text style={styles.statValue}>{percentComplete}%</Text>
            <Text style={styles.statLabel}>Complete</Text>
          </View>
          <View style={styles.statBox}>
            <Text style={styles.statValue}>
              {wordsRead > 1000
                ? `${(wordsRead / 1000).toFixed(1)}K`
                : wordsRead}
            </Text>
            <Text style={styles.statLabel}>Words Read</Text>
          </View>
        </View>

        {chapters.length > 1 && (
          <View style={styles.menuSection}>
            <Text style={styles.menuLabel}>Chapters</Text>
            <View style={styles.chapterList}>
              {chapters.map((ch, i) => (
                <TouchableOpacity
                  key={`ch-${i}`}
                  style={[
                    styles.chapterItem,
                    i === currentChapterIdx && styles.chapterItemActive,
                  ]}
                  onPress={() => onJumpToChapter(ch.startOffset)}
                >
                  <Text
                    style={[
                      styles.chapterItemText,
                      i === currentChapterIdx && styles.chapterItemTextActive,
                    ]}
                    numberOfLines={1}
                  >
                    {ch.title}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        )}
        {pageInfo && <Text style={styles.pageInfoText}>{pageInfo}</Text>}
      </View>
    </View>
  );
}

export default function ReaderScreen() {
  return (
    <ReaderStoreProvider>
      <ReaderContent />
    </ReaderStoreProvider>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  centered: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    padding: 32,
  },
  loadingText: { marginTop: 16, fontSize: 15, textAlign: "center" },
  errorText: { fontSize: 15, textAlign: "center", marginBottom: 20 },
  retryBtn: {
    backgroundColor: "#008080",
    paddingVertical: 10,
    paddingHorizontal: 24,
    borderRadius: 8,
  },
  retryBtnText: {
    color: "#ffffff",
    fontSize: 14,
    fontWeight: "bold",
  },
  progressBar: {
    height: 3,
    width: "100%",
  },
  progressFill: { height: "100%" },
  overlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(0,0,0,0.6)",
    justifyContent: "flex-end",
  },
  overlayClose: { flex: 1 },
  menuPanel: {
    backgroundColor: "#1a1a1a",
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    paddingHorizontal: 24,
    paddingVertical: 20,
    paddingBottom: 40,
  },
  menuTitle: {
    color: "#ffffff",
    fontSize: 18,
    fontWeight: "bold",
    marginBottom: 20,
    textAlign: "center",
  },
  menuRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 16,
  },
  menuLabel: { color: "#aaaaaa", fontSize: 14, flex: 1 },
  menuControls: { flexDirection: "row", alignItems: "center", gap: 8 },
  menuBtn: {
    width: 40,
    height: 32,
    borderRadius: 6,
    backgroundColor: "#2d2d2d",
    justifyContent: "center",
    alignItems: "center",
  },
  menuBtnText: { color: "#ffffff", fontSize: 16, fontWeight: "bold" },
  menuValue: {
    color: "#ffffff",
    fontSize: 14,
    minWidth: 30,
    textAlign: "center",
  },
  themeDot: {
    width: 28,
    height: 28,
    borderRadius: 14,
    marginLeft: 8,
  },
  modeToggle: {
    backgroundColor: "#008080",
    borderRadius: 6,
    paddingVertical: 6,
    paddingHorizontal: 16,
  },
  modeToggleText: { color: "#ffffff", fontSize: 13, fontWeight: "bold" },
  pageInfoText: {
    color: "#666666",
    fontSize: 12,
    textAlign: "center",
    marginTop: 12,
  },
  menuSection: {
    marginTop: 8,
    marginBottom: 8,
  },
  chapterList: {
    maxHeight: 160,
    marginTop: 8,
  },
  chapterItem: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 6,
    marginBottom: 4,
    backgroundColor: "#2d2d2d",
  },
  chapterItemActive: {
    backgroundColor: "#008080",
  },
  chapterItemText: {
    color: "#cccccc",
    fontSize: 13,
  },
  chapterItemTextActive: {
    color: "#ffffff",
    fontWeight: "bold",
  },
  brightnessControls: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    flex: 1,
    justifyContent: "flex-end",
  },
  brightnessTrack: {
    flex: 1,
    height: 6,
    backgroundColor: "#2d2d2d",
    borderRadius: 3,
    overflow: "hidden",
    minWidth: 60,
  },
  brightnessFill: {
    height: "100%",
    backgroundColor: "#008080",
    borderRadius: 3,
  },
  statsRow: {
    flexDirection: "row",
    gap: 12,
    marginBottom: 12,
  },
  statBox: {
    flex: 1,
    backgroundColor: "#1a1a1a",
    borderRadius: 8,
    padding: 12,
    alignItems: "center",
    borderWidth: 1,
    borderColor: "#262626",
  },
  statValue: {
    color: "#ffffff",
    fontSize: 18,
    fontWeight: "bold",
  },
  statLabel: {
    color: "#666666",
    fontSize: 11,
    marginTop: 4,
    textTransform: "uppercase",
  },
  brightnessOverlay: {
    ...StyleSheet.absoluteFillObject,
    zIndex: 5,
  },
  floatingMenuBtn: {
    position: 'absolute',
    bottom: 30,
    right: 20,
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#008080',
    justifyContent: 'center',
    alignItems: 'center',
    ...Platform.select({
      ios: { shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.3, shadowRadius: 4 },
      android: { elevation: 4 },
    }),
  },
  floatingMenuBtnText: {
    color: '#ffffff',
    fontSize: 20,
  },
});
