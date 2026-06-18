export interface DetectedChapter {
  title: string;
  startOffset: number;
}

const CHAPTER_PATTERNS = [
  /^(?:CHAPTER|Chapter|CHAP\.|Chap\.)\s*(\d+|[IVXLCDM]+)\b/gim,
  /^(?:Part|PART)\s*(\d+|[IVXLCDM]+)\b/gim,
  /^(?:Book|BOOK)\s*(\d+|[IVXLCDM]+)\b/gim,
  /^(?:Section|SECTION)\s*(\d+)\b/gim,
  /^\d+\.\s+(?:The|A|An|In|On|At|When|Where|How|Why|What)\b/gim,
];

export function looksLikeChapterLine(line: string): string | null {
  const trimmed = line.trim();
  if (!trimmed || trimmed.length > 100) return null;
  for (const pattern of CHAPTER_PATTERNS) {
    if (pattern.test(trimmed)) {
      pattern.lastIndex = 0;
      return trimmed;
    }
  }
  return null;
}

export function detectChapters(text: string): DetectedChapter[] {
  if (!text) return [];

  const chapters: DetectedChapter[] = [];
  const lines = text.split("\n");
  let offset = 0;

  for (let i = 0; i < lines.length; i++) {
    const match = looksLikeChapterLine(lines[i]);
    if (match) {
      chapters.push({ title: match, startOffset: offset });
    }
    offset += lines[i].length + 1;
  }

  if (chapters.length === 0) return [];

  return chapters;
}
