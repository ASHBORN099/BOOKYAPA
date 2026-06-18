import { BookSource } from "./BookSource";
import { StreamableBook, BookChapter } from "./types";
import {
  HttpSourceError,
  NetworkSourceError,
  ParseSourceError,
  SourceError,
  TimeoutSourceError,
} from "./errors";

const REQUEST_TIMEOUT_MS = 10000;
const PAGE_SIZE = 24;

const PLACEHOLDER_COVER = "https://via.placeholder.com/150x220.png?text=No+Cover";

export class StandardEbooksSource extends BookSource {
  readonly sourceName = "Standard Ebooks";
  readonly baseUrl = "https://standardebooks.org";

  async searchBooks(query: string): Promise<StreamableBook[]> {
    const formattedQuery = encodeURIComponent(query);
    const url = `${this.baseUrl}/ebooks?query=${formattedQuery}`;
    const html = await this.fetchHtml(url);
    return this.parseBooksFromHtml(html);
  }

  async fetchPopularBooks(page: number = 1): Promise<StreamableBook[]> {
    const url = `${this.baseUrl}/ebooks?page=${page}&per-page=${PAGE_SIZE}`;
    const html = await this.fetchHtml(url);
    return this.parseBooksFromHtml(html);
  }

  async fetchChapterList(bookEpubUrl: string): Promise<BookChapter[]> {
    return [{ title: "Begin Reading", contentUrl: bookEpubUrl }];
  }

  private async fetchHtml(url: string): Promise<string> {
    let timer: ReturnType<typeof setTimeout> | null = null;
    try {
      const response = await new Promise<Response>((resolve, reject) => {
        timer = setTimeout(
          () => reject(new TimeoutSourceError(this.sourceName)),
          REQUEST_TIMEOUT_MS,
        );
        fetch(url, {
          headers: { "User-Agent": "Bookyapa/1.0" },
        }).then(resolve, reject);
      });

      if (!response.ok) {
        throw new HttpSourceError(this.sourceName, response.status);
      }

      return await response.text();
    } catch (err) {
      if (err instanceof SourceError) throw err;
      if (err instanceof TypeError) {
        throw new NetworkSourceError(this.sourceName, err);
      }
      throw err;
    } finally {
      if (timer) clearTimeout(timer);
    }
  }

  private parseBooksFromHtml(html: string): StreamableBook[] {
    const results: StreamableBook[] = [];
    const bookBlockRegex = /<li\s+typeof="schema:Book"\s+about="([^"]+)"[^>]*>[\s\S]*?<\/li>/gi;
    let blockMatch: RegExpExecArray | null;

    while ((blockMatch = bookBlockRegex.exec(html)) !== null) {
      const block = blockMatch[0];
      const about = blockMatch[1];

      const titleMatch = block.match(
        /<a\s+href="[^"]*"\s+property="schema:url">\s*<span\s+property="schema:name">([^<]+)<\/span>/,
      );
      if (!titleMatch) continue;

      const authorMatch = block.match(
        /<p\s+class="author"[^>]*>[\s\S]*?<span\s+property="schema:name">([^<]+)<\/span>/,
      );
      const coverMatch = block.match(
        /<img[^>]+src="([^"]+)"[^>]*property="schema:image"/,
      );

      const title = this.decodeEntities(titleMatch[1]).trim();
      const author = authorMatch
        ? this.decodeEntities(authorMatch[1]).trim()
        : "Unknown Author";
      const coverPath = coverMatch ? coverMatch[1] : "";
      const coverUrl = coverPath
        ? coverPath.startsWith("http")
          ? coverPath
          : `https://standardebooks.org${coverPath}`
        : PLACEHOLDER_COVER;
      const htmlUrl = `https://standardebooks.org${about}/text/single-page`;
      const idSlug = about.replace("/ebooks/", "").replace(/\//g, "-");
      const epubSlug = about.split('/').slice(2).join('-');

      results.push({
        id: `se-${idSlug}`,
        title,
        author,
        coverUrl,
        epubUrl: `https://standardebooks.org${about}/downloads/${epubSlug}.epub`,
        htmlUrl,
        sourceName: this.sourceName,
      });
    }

    return results;
  }

  private decodeEntities(s: string): string {
    return s
      .replace(/&amp;/g, "&")
      .replace(/&lt;/g, "<")
      .replace(/&gt;/g, ">")
      .replace(/&quot;/g, '"')
      .replace(/&#39;/g, "'")
      .replace(/&nbsp;/g, " ");
  }
}

export const standardEbooksSource = new StandardEbooksSource();
