import React, { useCallback, useEffect, useRef, useState } from "react";
import {
  ActivityIndicator,
  ScrollView,
  Image,
  RefreshControl,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useRouter } from "expo-router";
import { sourceManager } from "../../sources/SourceManager";
import { SourceError } from "../../sources/errors";
import { StreamableBook } from "../../sources/types";

const CARD_WIDTH = 120;
const CARD_HEIGHT = CARD_WIDTH * 1.45;

const CATEGORIES = [
  { label: "Classic Fiction", query: "subject:classic_fiction" },
  { label: "Romance", query: "subject:romance" },
  { label: "Science Fiction", query: "subject:science_fiction" },
  { label: "Mystery", query: "subject:mystery" },
  { label: "Adventure", query: "subject:adventure" },
  { label: "History", query: "subject:history" },
  { label: "Poetry", query: "subject:poetry" },
  { label: "Philosophy", query: "subject:philosophy" },
  { label: "Horror", query: "subject:horror" },
  { label: "Drama", query: "subject:drama" },
];

interface BookSection {
  sourceName: string;
  books: StreamableBook[];
  errorMessage: string | null;
}

function buildSections(
  books: StreamableBook[],
  errors: Record<string, SourceError>,
  sourceOrder: string[],
): BookSection[] {
  const grouped: Record<string, StreamableBook[]> = {};
  for (const book of books) {
    if (!grouped[book.sourceName]) grouped[book.sourceName] = [];
    grouped[book.sourceName].push(book);
  }

  const sections: BookSection[] = [];
  const seen = new Set<string>();

  for (const name of sourceOrder) {
    seen.add(name);
    const sourceBooks = grouped[name] || [];
    if (sourceBooks.length > 0) {
      sections.push({ sourceName: name, books: sourceBooks, errorMessage: null });
    } else if (errors[name]) {
      sections.push({ sourceName: name, books: [], errorMessage: `Could not reach ${name}. The server may be down.` });
    }
  }

  for (const [name, sourceBooks] of Object.entries(grouped)) {
    if (!seen.has(name) && sourceBooks.length > 0) {
      sections.push({ sourceName: name, books: sourceBooks, errorMessage: null });
    }
  }

  return sections;
}

export default function ExploreScreen() {
  const router = useRouter();
  const sourceOrder = useRef(sourceManager.getSourceOrder()).current;

  const [selectedCategory, setSelectedCategory] = useState(CATEGORIES[0].query);
  const [books, setBooks] = useState<StreamableBook[]>([]);
  const [errors, setErrors] = useState<Record<string, SourceError>>({});
  const [loading, setLoading] = useState(false);

  const fetchCategory = useCallback((query: string) => {
    setLoading(true);
    setSelectedCategory(query);
    setErrors({});
    sourceManager
      .searchAllSources(query)
      .then(({ results, errors: errs }) => {
        setBooks(results.slice(0, 30));
        setErrors(errs);
        setLoading(false);
      })
      .catch((e) => { console.error("Explore fetch failed:", e); setLoading(false); });
  }, []);

  useEffect(() => {
    fetchCategory(CATEGORIES[0].query);
  }, [fetchCategory]);

  const handleRefresh = useCallback(() => {
    fetchCategory(selectedCategory);
  }, [fetchCategory, selectedCategory]);

  const sections = buildSections(books, errors, sourceOrder);
  const hasContent = books.length > 0 || Object.keys(errors).length > 0;

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.heading}>Browse by Category</Text>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.chipList}
      >
        {CATEGORIES.map((item) => (
          <TouchableOpacity
            key={item.query}
            style={[
              styles.chip,
              selectedCategory === item.query && styles.chipActive,
            ]}
            onPress={() => fetchCategory(item.query)}
          >
            <Text
              style={[
                styles.chipText,
                selectedCategory === item.query && styles.chipTextActive,
              ]}
            >
              {item.label}
            </Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      {loading && !hasContent ? (
        <View style={styles.centered}>
          <ActivityIndicator size="large" color="#008080" />
        </View>
      ) : hasContent ? (
        <ScrollView
          refreshControl={
            <RefreshControl refreshing={false} onRefresh={handleRefresh} tintColor="#008080" />
          }
        >
          {sections.map((section) => (
            <View key={section.sourceName} style={styles.section}>
              <Text style={styles.sectionTitle}>{section.sourceName}</Text>
              {section.books.length > 0 ? (
                <ScrollView
                  horizontal
                  showsHorizontalScrollIndicator={false}
                  contentContainerStyle={styles.horizontalList}
                >
                  {section.books.map((book) => (
                    <TouchableOpacity
                      key={book.id}
                      style={styles.horizontalCard}
                      activeOpacity={0.7}
                      onPress={() =>
                        router.push({
                          pathname: "/reader",
                          params: {
                            id: book.id,
                            title: book.title,
                            bookUrl: book.htmlUrl,
                            coverUrl: book.coverUrl,
                            epubUrl: book.epubUrl,
                          },
                        })
                      }
                    >
                      <Image
                        source={{ uri: book.coverUrl || undefined }}
                        style={styles.horizontalCover}
                        resizeMode="cover"
                      />
                      <Text style={styles.horizontalTitle} numberOfLines={2}>
                        {book.title}
                      </Text>
                      <Text style={styles.horizontalAuthor} numberOfLines={1}>
                        {book.author || "Unknown"}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>
              ) : section.errorMessage ? (
                <View style={styles.errorCard}>
                  <Text style={styles.errorIcon}>⚠</Text>
                  <Text style={styles.errorMessage}>{section.errorMessage}</Text>
                </View>
              ) : null}
            </View>
          ))}
        </ScrollView>
      ) : !loading ? (
        <View style={styles.centered}>
          <Text style={styles.emptyText}>No books found for this category</Text>
        </View>
      ) : null}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#121212" },
  heading: {
    color: "#ffffff",
    fontSize: 22,
    fontWeight: "bold",
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 8,
  },
  chipList: { paddingHorizontal: 16, paddingBottom: 12, gap: 8 },
  chip: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
    backgroundColor: "#1a1a1a",
    borderWidth: 1,
    borderColor: "#2d2d2d",
    marginRight: 8,
  },
  chipActive: { backgroundColor: "#008080", borderColor: "#008080" },
  chipText: { color: "#aaaaaa", fontSize: 13 },
  chipTextActive: { color: "#ffffff", fontWeight: "bold" },
  centered: { flex: 1, justifyContent: "center", alignItems: "center" },
  emptyText: { color: "#666666", fontSize: 15 },

  section: { marginBottom: 24 },
  sectionTitle: {
    color: "#ffffff",
    fontSize: 18,
    fontWeight: "bold",
    paddingHorizontal: 20,
    marginBottom: 12,
  },
  horizontalList: { paddingHorizontal: 14, gap: 10 },
  horizontalCard: { width: CARD_WIDTH },
  horizontalCover: {
    width: CARD_WIDTH,
    height: CARD_HEIGHT,
    borderRadius: 6,
    backgroundColor: "#262626",
  },
  horizontalTitle: {
    color: "#ffffff",
    fontSize: 11,
    lineHeight: 14,
    marginTop: 6,
  },
  horizontalAuthor: {
    color: "#aaaaaa",
    fontSize: 10,
    marginTop: 2,
  },
  errorCard: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#3a1a1a",
    borderColor: "#ff6b6b",
    borderWidth: 1,
    borderRadius: 8,
    marginHorizontal: 20,
    padding: 12,
  },
  errorIcon: { color: "#ff6b6b", fontSize: 16, marginRight: 10 },
  errorMessage: { color: "#ffcccc", fontSize: 13, flex: 1 },
});
