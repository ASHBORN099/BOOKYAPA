import { useEffect, useRef, useState, useCallback } from "react";
import { getCachedText, hasCachedText, clearCachedText } from "../services/text-cache";
import { streamText } from "../services/text-streamer";

interface UseStreamingTextOptions {
  url: string | null;
  enabled?: boolean;
}

interface UseStreamingTextResult {
  text: string;
  isComplete: boolean;
  progress: number;
  isStreaming: boolean;
  error: string | null;
  retry: () => void;
}

export function useStreamingText({
  url,
  enabled = true,
}: UseStreamingTextOptions): UseStreamingTextResult {
  const [text, setText] = useState("");
  const [isComplete, setIsComplete] = useState(false);
  const [progress, setProgress] = useState(0);
  const [isStreaming, setIsStreaming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const cancelRef = useRef<(() => void) | null>(null);
  const prevUrlRef = useRef<string | null>(null);
  const initialLoadRef = useRef(true);
  const [retryKey, setRetryKey] = useState(0);

  const reset = useCallback(() => {
    setText("");
    setIsComplete(false);
    setProgress(0);
    setIsStreaming(false);
    setError(null);
  }, []);

  const retry = useCallback(() => {
    if (cancelRef.current) {
      cancelRef.current();
      cancelRef.current = null;
    }
    if (url) clearCachedText(url);
    initialLoadRef.current = false;
    reset();
    setRetryKey((k) => k + 1);
  }, [url, reset]);

  useEffect(() => {
    if (!url || !enabled) {
      reset();
      return;
    }

    if (initialLoadRef.current && url === prevUrlRef.current && hasCachedText(url)) {
      const cached = getCachedText(url);
      if (cached !== undefined) {
        setText(cached);
        setIsComplete(true);
        setProgress(1);
        setIsStreaming(false);
        setError(null);
        prevUrlRef.current = url;
        return;
      }
    }
    initialLoadRef.current = false;

    reset();
    prevUrlRef.current = url;
    setIsStreaming(true);

    cancelRef.current = streamText(url, {
      onProgress: (newChunk, loaded, total) => {
        setText((prev) => prev + newChunk);
        setProgress(total > 0 ? loaded / total : 0.5);
      },
      onComplete: (fullText) => {
        setText(fullText);
        setIsComplete(true);
        setProgress(1);
        setIsStreaming(false);
        setError(null);
      },
      onError: (err) => {
        setError(err.message);
        setIsStreaming(false);
        setIsComplete(false);
      },
    });

    return () => {
      if (cancelRef.current) {
        cancelRef.current();
        cancelRef.current = null;
      }
    };
  }, [url, enabled, reset, retryKey]);

  return { text, isComplete, progress, isStreaming, error, retry };
}
