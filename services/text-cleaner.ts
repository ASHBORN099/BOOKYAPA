import { looksLikeChapterLine } from "./chapter-detector";

const PAGE_ARTIFACT_PATTERNS = [
  /^Page\s+(?:\d+|[ivxlcdm]{1,8})\.?$/i,
  /^—\s*(?:\d+|[ivxlcdm]{1,8})\s*—$/,
  /^\[?[Pp]g\.?\s*(?:\d+|[ivxlcdm]{1,8})\]?$/i,
  /^\d{1,4}$/,
  /^[ivxlcdm]{1,8}[.)\]]?$/i,
];

const LINE_SAFE_PATTERNS = [
  /^Page\s+(?:\d+|[ivxlcdm]{1,8})\.?$/i,
  /^—\s*(?:\d+|[ivxlcdm]{1,8})\s*—$/,
  /^\[?[Pp]g\.?\s*(?:\d+|[ivxlcdm]{1,8})\]?$/i,
  /^[ivxlcdm]{1,8}[.)\]]?$/i,
];

function escapeRegex(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

export function stripPageMarkers(text: string, bookTitle?: string): string {
  if (!text) return text;

  const lines = text.split("\n");
  const lineFiltered = lines.filter((line) => {
    const trimmed = line.trim();
    if (!trimmed) return true;
    for (const p of LINE_SAFE_PATTERNS) {
      if (p.test(trimmed)) return false;
    }
    return true;
  });
  const afterLines = lineFiltered.join("\n");

  const paragraphs = afterLines.split(/\n{2,}/);
  const cleanTitle = bookTitle?.trim().toLowerCase();

  const filtered = paragraphs.filter((para) => {
    const trimmed = para.trim();
    if (!trimmed) return true;

    for (const pattern of PAGE_ARTIFACT_PATTERNS) {
      if (pattern.test(trimmed)) return false;
    }

    if (cleanTitle) {
      const escaped = escapeRegex(cleanTitle);
      const exactPattern = new RegExp(
        `^${escaped}[.,!?;:'")\\]–—]?$`,
        "i",
      );
      if (exactPattern.test(trimmed)) return false;
    }

    return true;
  });

  return filtered.join("\n\n");
}

const GUTENBERG_START = /\*\*\*\s*START\s+OF\s+(THE|THIS)\s+PROJECT\s+GUTENBERG\s+EBOOK/i;
const GUTENBERG_END = /\*\*\*\s*END\s+OF\s+(THE|THIS)\s+PROJECT\s+GUTENBERG\s+E?BOOK/i;

export function stripGutenbergBoilerplate(text: string): string {
  if (!text) return text;

  const startMatch = GUTENBERG_START.exec(text);
  if (!startMatch) return text;

  const startIdx = startMatch.index;
  const afterStart = text.slice(startIdx + startMatch[0].length);

  const endMatch = GUTENBERG_END.exec(afterStart);
  if (!endMatch) return afterStart.trim();

  return afterStart.slice(0, endMatch.index).trim();
}

export function reflowHardBreaks(text: string): string {
  if (!text) return text;
  const normalized = text.replace(/\r\n/g, "\n").replace(/\n{3,}/g, "\n\n");
  return normalized
    .split(/\n\n/)
    .map((para) => para.replace(/-\s*\n/g, "").replace(/\n/g, " ").trim())
    .join("\n\n");
}

export function isChapterHeader(paragraph: string): boolean {
  const trimmed = paragraph.trim();
  if (!trimmed) return false;
  return looksLikeChapterLine(trimmed) !== null;
}

export function breakLongWords(text: string, maxLen = 25): string {
  if (!text) return text;
  const re = new RegExp(`\\S{${maxLen + 1},}`, "g");
  return text.replace(re, (match) => {
    const parts: string[] = [];
    for (let i = 0; i < match.length; i += 15) {
      parts.push(match.slice(i, i + 15));
    }
    return parts.join("\u200B");
  });
}

export function formatParagraphs(text: string): string {
  if (!text) return text;
  const paragraphs = text.split(/\n{2,}/);
  const parts: string[] = [];
  let afterChapter = true;

  for (const para of paragraphs) {
    const trimmed = para.trim();
    if (!trimmed) continue;

    if (isChapterHeader(trimmed)) {
      parts.push(trimmed);
      afterChapter = true;
    } else {
      if (afterChapter) {
        parts.push(trimmed);
      } else {
        parts.push(`  ${trimmed}`);
      }
      afterChapter = false;
    }
  }

  return parts.join("\n\n");
}
