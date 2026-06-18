// sources/GutenbergSource.ts
import { BookSource } from './BookSource';
import { StreamableBook, BookChapter } from './types';

export class GutenbergSource extends BookSource {
  readonly sourceName = 'Project Gutenberg';
  readonly baseUrl = 'https://gutendex.com';

  async searchBooks(query: string): Promise<StreamableBook[]> {
    const formattedQuery = encodeURIComponent(query);
    const response = await fetch(`${this.baseUrl}/books?search=${formattedQuery}&languages=en`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const data = await response.json();
    return this.mapResults(data.results || []);
  }

  async fetchPopularBooks(page: number = 1): Promise<StreamableBook[]> {
    const response = await fetch(`${this.baseUrl}/books?page=${page}&languages=en`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const data = await response.json();
    return this.mapResults(data.results || []);
  }

  async fetchChapterList(bookEpubUrl: string): Promise<BookChapter[]> {
    return [{ title: 'Begin Reading', contentUrl: bookEpubUrl }];
  }

  private mapResults(results: any[]): StreamableBook[] {
    return results
      .map((book) => {
        const rawAuthor = book.authors && book.authors.length > 0 ? book.authors[0].name : 'Unknown Author';

        const cleanAuthor = rawAuthor.includes(',')
          ? rawAuthor.split(',').reverse().map((name: string) => name.trim()).join(' ')
          : rawAuthor;

        const formats = book.formats || {};

        const rawHtmlUrl = formats['text/html'] ||
                           formats['text/html; charset=utf-8'] ||
                           formats['text/html; charset=iso-8859-1'] || '';

        return {
          id: `gutenberg-${book.id}`,
          title: book.title,
          author: cleanAuthor,
          coverUrl: formats['image/jpeg'] || 'https://via.placeholder.com/150x220.png?text=No+Cover',
          epubUrl: formats['application/epub+zip'] || '',
          htmlUrl: rawHtmlUrl,
          sourceName: this.sourceName,
        };
      })
      .filter((book) => book.htmlUrl !== '' || book.epubUrl !== '');
  }
}

export const gutenbergSource = new GutenbergSource();
