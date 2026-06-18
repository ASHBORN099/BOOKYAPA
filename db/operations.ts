import { eq, inArray, desc } from "drizzle-orm";
import { db } from "./db";
import { readingProgress, sourceSettings } from "./schema";
import type { InferSelectModel, InferInsertModel } from "drizzle-orm";

export type ReadingProgress = InferSelectModel<typeof readingProgress>;
export type NewReadingProgress = InferInsertModel<typeof readingProgress>;
export type SourceSetting = InferSelectModel<typeof sourceSettings>;

export function getProgress(bookId: string): ReadingProgress | undefined {
  return db
    .select()
    .from(readingProgress)
    .where(eq(readingProgress.bookId, bookId))
    .get();
}

export function upsertProgress(progress: NewReadingProgress): void {
  db.insert(readingProgress)
    .values(progress)
    .onConflictDoUpdate({
      target: readingProgress.bookId,
      set: {
        bookTitle: progress.bookTitle,
        coverUrl: progress.coverUrl,
        bookUrl: progress.bookUrl,
        epubUrl: progress.epubUrl,
        chapterIndex: progress.chapterIndex,
        characterOffset: progress.characterOffset,
        fontSize: progress.fontSize,
        lineHeight: progress.lineHeight,
        readingMode: progress.readingMode,
        updatedAt: Date.now(),
      },
    })
    .run();
}

export function getRecentBooks(limit: number = 10): ReadingProgress[] {
  return db
    .select()
    .from(readingProgress)
    .orderBy(desc(readingProgress.updatedAt))
    .limit(limit)
    .all();
}

export function getActiveSources(): string[] {
  const rows = db
    .select()
    .from(sourceSettings)
    .where(eq(sourceSettings.enabled, 1))
    .all();
  return rows.map((r) => r.sourceName);
}

export function getAllSourceSettings(): SourceSetting[] {
  return db.select().from(sourceSettings).all();
}

export function setSourceEnabled(sourceName: string, enabled: boolean): void {
  db.insert(sourceSettings)
    .values({ sourceName, enabled: enabled ? 1 : 0 })
    .onConflictDoUpdate({
      target: sourceSettings.sourceName,
      set: { enabled: enabled ? 1 : 0 },
    })
    .run();
}

export function hasSourceSettings(): boolean {
  const count = db.select().from(sourceSettings).all();
  return count.length > 0;
}

export function initSourceSettings(sourceNames: string[]): void {
  const existing = getAllSourceSettings();
  const existingNames = new Set(existing.map((s) => s.sourceName));
  for (const name of sourceNames) {
    if (!existingNames.has(name)) {
      db.insert(sourceSettings)
        .values({ sourceName: name, enabled: 1 })
        .run();
    }
  }
}
