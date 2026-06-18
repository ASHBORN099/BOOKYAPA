import { useEffect, useRef, useState } from "react";
import { parseEpub, EpubChapter } from "../services/epub-parser";

export interface UseEpubReaderResult {
  html: string;
  chapters: EpubChapter[];
  textContent: string;
  isLoading: boolean;
  error: string | null;
}

export function useEpubReader(
  epubUrl: string | null,
): UseEpubReaderResult {
  const [html, setHtml] = useState("");
  const [chapters, setChapters] = useState<EpubChapter[]>([]);
  const [textContent, setTextContent] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const prevUrlRef = useRef<string | null>(null);

  useEffect(() => {
    if (!epubUrl) {
      setHtml("");
      setChapters([]);
      setTextContent("");
      setIsLoading(false);
      setError(null);
      return;
    }

    if (epubUrl === prevUrlRef.current) return;
    prevUrlRef.current = epubUrl;

    let cancelled = false;
    setIsLoading(true);
    setError(null);

    parseEpub(epubUrl)
      .then((result) => {
        if (cancelled) return;
        setHtml(result.html);
        setChapters(result.chapters);
        setTextContent(result.textContent);
        setIsLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        const msg = err instanceof Error ? err.message : "Failed to parse EPUB";
        setError(msg);
        setHtml("");
        setChapters([]);
        setTextContent("");
        setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [epubUrl]);

  return { html, chapters, textContent, isLoading, error };
}
