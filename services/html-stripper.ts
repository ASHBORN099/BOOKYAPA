export function looksLikeHtml(text: string): boolean {
  const trimmed = text.trimStart();
  if (
    trimmed.startsWith("<!DOCTYPE") ||
    trimmed.startsWith("<html") ||
    trimmed.startsWith("<HTML")
  ) {
    return true;
  }
  return /<[a-z][\s\S]*>/i.test(trimmed.slice(0, 500));
}

export function stripHtml(html: string): string {
  const ENTITY_MAP: Record<string, string> = {
    '&amp;': '&', '&lt;': '<', '&gt;': '>',
    '&quot;': '"', '&nbsp;': ' ', '&#39;': "'", '&apos;': "'",
  };
  return html
    .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
    .replace(/<style[^>]*>[\s\S]*?<\/style>/gi, '')
    .replace(/<[^>]+>/g, '')
    .replace(/&(#\d+|#x[\da-fA-F]+|\w+);/g, (match, group1) => {
      if (group1.startsWith('#x') || group1.startsWith('#X')) {
        return String.fromCharCode(parseInt(group1.slice(2), 16));
      }
      if (group1.startsWith('#')) {
        return String.fromCharCode(parseInt(group1.slice(1), 10));
      }
      return ENTITY_MAP[match] || match;
    })
    .replace(/\n[^\S\n]*\n/g, '\n\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}
