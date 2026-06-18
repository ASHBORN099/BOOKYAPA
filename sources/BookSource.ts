// sources/BookSource.ts
import { StreamableBook, BookChapter } from './types';

export abstract class BookSource {
  // Every website extension must declare its own name and home URL
  abstract readonly sourceName: string;
  abstract readonly baseUrl: string;

  /**
   * Search query resolver: Fired when typing into the search bar
   */
  abstract searchBooks(query: string): Promise<StreamableBook[]>;

  /**
   * Catalog explorer: Fired when browsing trending books page-by-page
   */
  abstract fetchPopularBooks(page: number): Promise<StreamableBook[]>;

  /**
   * Chapter parser: Resolves table of contents mapping
   */
  abstract fetchChapterList(bookEpubUrl: string): Promise<BookChapter[]>;
}