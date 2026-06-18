import { sqliteTable, text, integer, real } from "drizzle-orm/sqlite-core";

export const readingProgress = sqliteTable("reading_progress", {
  bookId: text("book_id").primaryKey(),
  bookTitle: text("book_title").notNull(),
  coverUrl: text("cover_url"),
  bookUrl: text("book_url"),
  epubUrl: text("epub_url"),
  chapterIndex: integer("chapter_index").notNull().default(0),
  characterOffset: integer("character_offset").notNull().default(0),
  fontSize: integer("font_size").notNull().default(18),
  lineHeight: real("line_height").notNull().default(1.5),
  readingMode: text("reading_mode").notNull().default("page"),
  updatedAt: integer("updated_at").notNull(),
});

export const sourceSettings = sqliteTable("source_settings", {
  sourceName: text("source_name").primaryKey(),
  enabled: integer("enabled").notNull().default(1),
});
