import { drizzle } from "drizzle-orm/expo-sqlite";
import { openDatabaseSync } from "expo-sqlite";
import * as schema from "./schema";

const expoDb = openDatabaseSync("bookyapa.db");

expoDb.execSync(`
  CREATE TABLE IF NOT EXISTS reading_progress (
    book_id TEXT PRIMARY KEY,
    book_title TEXT NOT NULL,
    cover_url TEXT,
    book_url TEXT,
    chapter_index INTEGER NOT NULL DEFAULT 0,
    character_offset INTEGER NOT NULL DEFAULT 0,
    font_size INTEGER NOT NULL DEFAULT 18,
    line_height REAL NOT NULL DEFAULT 1.5,
    reading_mode TEXT NOT NULL DEFAULT 'page',
    updated_at INTEGER NOT NULL
  );
`);

expoDb.execSync(`
  CREATE TABLE IF NOT EXISTS source_settings (
    source_name TEXT PRIMARY KEY,
    enabled INTEGER NOT NULL DEFAULT 1
  );
`);

try {
  expoDb.execSync(`ALTER TABLE reading_progress ADD COLUMN epub_url TEXT;`);
} catch {}

export const db = drizzle(expoDb, { schema });
