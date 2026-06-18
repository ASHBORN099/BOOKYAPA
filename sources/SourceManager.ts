import { BookSource } from './BookSource';
import { openLibrarySource } from './OpenLibrarySource';
import { standardEbooksSource } from './StandardEbooksSource';
import { gutenbergSource } from './GutenbergSource';
import { NetworkSourceError, SourceError, TimeoutSourceError } from './errors';
import { StreamableBook } from './types';
import {
  initSourceSettings,
  getActiveSources,
  getAllSourceSettings,
  setSourceEnabled,
} from '../db/operations';

export interface SourceManagerResult {
  results: StreamableBook[];
  errors: Record<string, SourceError>;
}

export interface SourceInfo {
  name: string;
  enabled: boolean;
}

class SourceManager {
  private allSources: BookSource[] = [
    openLibrarySource,
    standardEbooksSource,
    gutenbergSource,
  ];

  private activeSources: BookSource[] = [...this.allSources];

  init(): void {
    const names = this.allSources.map((s) => s.sourceName);
    initSourceSettings(names);
    this.refresh();
  }

  refresh(): void {
    const activeNames = new Set(getActiveSources());
    this.activeSources = this.allSources.filter((s) =>
      activeNames.has(s.sourceName),
    );
  }

  getAllSources(): SourceInfo[] {
    const settings = getAllSourceSettings();
    const map = new Map(settings.map((s) => [s.sourceName, s.enabled === 1]));
    return this.allSources.map((s) => ({
      name: s.sourceName,
      enabled: map.get(s.sourceName) ?? true,
    }));
  }

  getSourceOrder(): string[] {
    return this.allSources.map((s) => s.sourceName);
  }

  toggleSource(sourceName: string, enabled: boolean): void {
    setSourceEnabled(sourceName, enabled);
    this.refresh();
  }

  async searchAllSources(query: string): Promise<SourceManagerResult> {
    return this.fanOut((source) => source.searchBooks(query));
  }

  async fetchAllPopular(page: number = 1): Promise<SourceManagerResult> {
    return this.fanOut((source) => source.fetchPopularBooks(page));
  }

  private async fanOut(
    invoke: (source: BookSource) => Promise<StreamableBook[]>,
  ): Promise<SourceManagerResult> {
    const TIMEOUT_MS = 10000;

    const withTimeout = (
      promise: Promise<StreamableBook[]>,
      sourceName: string,
    ): Promise<StreamableBook[]> =>
      new Promise((resolve, reject) => {
        const timer = setTimeout(
          () => reject(new TimeoutSourceError(sourceName)),
          TIMEOUT_MS,
        );
        promise.then(
          (v) => { clearTimeout(timer); resolve(v); },
          (e) => { clearTimeout(timer); reject(e); },
        );
      });

    const settled = await Promise.allSettled(
      this.activeSources.map((s) => withTimeout(invoke(s), s.sourceName)),
    );

    const results: StreamableBook[] = [];
    const errors: Record<string, SourceError> = {};

    settled.forEach((outcome, index) => {
      const source = this.activeSources[index];
      if (outcome.status === 'fulfilled') {
        results.push(...outcome.value);
      } else {
        const reason = outcome.reason;
        errors[source.sourceName] =
          reason instanceof SourceError
            ? reason
            : new NetworkSourceError(source.sourceName, reason);
      }
    });

    return { results, errors };
  }
}

export const sourceManager = new SourceManager();
