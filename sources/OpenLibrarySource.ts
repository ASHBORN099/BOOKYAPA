// sources/OpenLibrarySource.ts
import { BookSource } from './BookSource';
import {
  HttpSourceError,
  NetworkSourceError,
  ParseSourceError,
  SourceError,
  TimeoutSourceError,
} from './errors';
import { StreamableBook, BookChapter } from './types';

const REQUEST_TIMEOUT_MS = 8000;
const HOME_RESULT_LIMIT = 30;
const PLACEHOLDER_COVER = 'https://via.placeholder.com/150x220.png?text=No+Cover';

export class OpenLibrarySource extends BookSource {
  readonly sourceName = 'Open Library';
  readonly baseUrl = 'https://openlibrary.org';

  async searchBooks(query: string): Promise<StreamableBook[]> {
    const formattedQuery = encodeURIComponent(query);
    const url = `${this.baseUrl}/search.json?q=${formattedQuery}&language=eng&fields=key,title,author_name,cover_i,ia,public_scan_b&limit=${HOME_RESULT_LIMIT}`;
    const data = await this.fetchJson(url);
    return this.mapResults(data.docs || []);
  }

  async fetchPopularBooks(page: number = 1): Promise<StreamableBook[]> {
    const url = `${this.baseUrl}/search.json?q=subject:classic_fiction&language=eng&sort=editions&page=${page}&fields=key,title,author_name,cover_i,ia,public_scan_b&limit=${HOME_RESULT_LIMIT}`;
    const data = await this.fetchJson(url);
    return this.mapResults(data.docs || []);
  }

  async fetchChapterList(bookEpubUrl: string): Promise<BookChapter[]> {
    return [{ title: 'Begin Reading', contentUrl: bookEpubUrl }];
  }

  private async fetchJson(url: string): Promise<any> {
    let timer: ReturnType<typeof setTimeout> | null = null;
    try {
      const response = await new Promise<Response>((resolve, reject) => {
        timer = setTimeout(
          () => reject(new TimeoutSourceError(this.sourceName)),
          REQUEST_TIMEOUT_MS,
        );
        fetch(url).then(resolve, reject);
      });

      if (!response.ok) {
        throw new HttpSourceError(this.sourceName, response.status);
      }

      try {
        return await response.json();
      } catch (err) {
        throw new ParseSourceError(this.sourceName, err);
      }
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

  private mapResults(docs: any[]): StreamableBook[] {
    const mapped: (StreamableBook | null)[] = docs.map((doc) => {
      const workId = typeof doc?.key === 'string' ? doc.key.split('/').pop() : null;
      if (!workId) return null;

      if (doc.public_scan_b !== true && doc.public_scan_b !== "true") return null;

      const iaId =
        Array.isArray(doc.ia)
          ? doc.ia.find((id: string) => {
              if (typeof id !== "string") return false;
              if (!/^[a-z]/.test(id)) return false;
              if (/^(bwb_|isbn_|synapseml_)/i.test(id)) return false;
              if (/librivox|emory\.edu|goog|gut$/i.test(id)) return false;
              return true;
            })
          : null;
      if (!iaId) return null;

      const rawTitle = typeof doc.title === 'string' ? doc.title.trim() : '';
      if (!rawTitle || rawTitle.length > 300) return null;

      const title = this.stripHtmlEntities(rawTitle);
      const author = this.cleanAuthor(
        Array.isArray(doc.author_name) && doc.author_name.length > 0
          ? doc.author_name[0]
          : null,
      );

      const coverUrl =
        typeof doc.cover_i === 'number'
          ? `https://covers.openlibrary.org/b/id/${doc.cover_i}-M.jpg`
          : PLACEHOLDER_COVER;

      return {
        id: `openlibrary-${workId}`,
        title,
        author,
        coverUrl,
        epubUrl: `https://archive.org/download/${iaId}/${iaId}.epub`,
        htmlUrl: `https://archive.org/download/${iaId}/${iaId}_djvu.txt`,
        sourceName: this.sourceName,
      };
    });

    return mapped.filter((book): book is StreamableBook => book !== null);
  }

  private stripHtmlEntities(s: string): string {
    return s
      .replace(/&#(\d+);/g, (_, dec: string) =>
        String.fromCharCode(parseInt(dec, 10)),
      )
      .replace(/&#x([0-9a-fA-F]+);/g, (_, hex: string) =>
        String.fromCharCode(parseInt(hex, 16)),
      )
      .replace(/&amp;/g, '&')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&quot;/g, '"')
      .replace(/&apos;/g, "'")
      .replace(/&nbsp;/g, ' ');
  }

  private cleanAuthor(raw: string | null): string {
    if (!raw) return 'Unknown Author';
    const trimmed = raw.trim();
    if (!trimmed) return 'Unknown Author';
    if (trimmed.includes(',')) {
      return trimmed
        .split(',')
        .reverse()
        .map((part) => part.trim())
        .filter((part) => part.length > 0)
        .join(' ');
    }
    return trimmed;
  }
}

export const openLibrarySource = new OpenLibrarySource();
