import { setCachedText } from "./text-cache";

export interface StreamCallbacks {
  onProgress: (partialText: string, loaded: number, total: number) => void;
  onComplete: (fullText: string) => void;
  onError: (error: Error) => void;
}

export function streamText(
  url: string,
  callbacks: StreamCallbacks,
): () => void {
  const xhr = new XMLHttpRequest();
  let lastLength = 0;
  let cancelled = false;

  xhr.onprogress = (event: ProgressEvent) => {
    if (cancelled) return;
    if (xhr.readyState !== XMLHttpRequest.LOADING && xhr.readyState !== XMLHttpRequest.DONE) return;

    const partialText = xhr.responseText || "";
    const newChunk = partialText.slice(lastLength);
    lastLength = partialText.length;

    if (newChunk) {
      callbacks.onProgress(
        newChunk,
        event.loaded,
        event.total || partialText.length,
      );
    }
  };

  xhr.onload = () => {
    if (cancelled) return;
    if (xhr.status >= 200 && xhr.status < 300) {
      const fullText = xhr.responseText || "";
      setCachedText(url, fullText);
      callbacks.onComplete(fullText);
    } else {
      callbacks.onError(
        new Error(`Server returned HTTP ${xhr.status}`),
      );
    }
  };

  xhr.onerror = () => {
    if (cancelled) return;
    callbacks.onError(new Error("Network request failed"));
  };

  xhr.ontimeout = () => {
    if (cancelled) return;
    callbacks.onError(new Error("Request timed out"));
  };

  xhr.open("GET", url);
  xhr.setRequestHeader("User-Agent", "Bookyapa/1.0");
  xhr.timeout = 30000;
  xhr.send();

  return () => {
    cancelled = true;
    xhr.abort();
  };
}
