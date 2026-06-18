import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TextInput,
  Image,
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { sourceManager } from '../../sources/SourceManager';
import { SourceError } from '../../sources/errors';
import { StreamableBook } from '../../sources/types';
import { getRecentBooks } from '../../db/operations';
import type { ReadingProgress } from '../../db/operations';

const CARD_WIDTH = 120;
const CARD_HEIGHT = CARD_WIDTH * 1.45;

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

export default function BrowseScreen() {
  const router = useRouter();
  const sourceOrder = useRef(sourceManager.getSourceOrder()).current;

  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [popularBooks, setPopularBooks] = useState<StreamableBook[]>([]);
  const [searchResults, setSearchResults] = useState<StreamableBook[]>([]);

  const [isHomeLoading, setIsHomeLoading] = useState(true);
  const [isSearchLoading, setIsSearchLoading] = useState(false);

  const [homeErrors, setHomeErrors] = useState<Record<string, SourceError>>({});
  const [searchErrors, setSearchErrors] = useState<Record<string, SourceError>>({});

  const [recentBooks, setRecentBooks] = useState<ReadingProgress[]>([]);

  useEffect(() => {
    try {
      const books = getRecentBooks(8);
      setRecentBooks(books.filter((b) => b.characterOffset > 0));
    } catch (e) { console.error("Failed to load recent books:", e); }
  }, []);

  const searchCache = useRef<{ [key: string]: StreamableBook[] }>({});
  const searchTransactionId = useRef(0);

  const fetchHome = useCallback(() => {
    setIsHomeLoading(true);
    setHomeErrors({});
    sourceManager
      .fetchAllPopular(1)
      .then(({ results, errors }) => {
        setPopularBooks(results);
        setHomeErrors(errors);
        setIsHomeLoading(false);
      })
      .catch(() => setIsHomeLoading(false));
  }, []);

  const executeSearch = useCallback((query: string) => {
    const cleanQuery = query.trim().toLowerCase();
    if (!cleanQuery) return;

    if (searchCache.current[cleanQuery]) {
      setSearchResults(searchCache.current[cleanQuery]);
      setSearchErrors({});
      setIsSearchLoading(false);
      return;
    }

    setIsSearchLoading(true);
    setSearchErrors({});
    const myToken = ++searchTransactionId.current;

    sourceManager
      .searchAllSources(cleanQuery)
      .then(({ results, errors }) => {
        if (myToken !== searchTransactionId.current) return;
        searchCache.current[cleanQuery] = results;
        setSearchResults(results);
        setSearchErrors(errors);
        setIsSearchLoading(false);
      })
      .catch(() => {
        if (myToken !== searchTransactionId.current) return;
        setIsSearchLoading(false);
      });
  }, []);

  useEffect(() => {
    fetchHome();
  }, [fetchHome]);

  const handleSearchTextChange = (text: string) => {
    setSearchQuery(text);
    if (!text || text.trim() === '') {
      searchTransactionId.current++;
      setDebouncedQuery('');
      setSearchResults([]);
      setSearchErrors({});
      setIsSearchLoading(false);
    }
  };

  useEffect(() => {
    if (!searchQuery.trim()) return;
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery);
    }, 400);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  useEffect(() => {
    if (!debouncedQuery.trim()) return;
    executeSearch(debouncedQuery);
  }, [debouncedQuery, executeSearch]);

  const handleRefresh = useCallback(() => {
    if (isDisplayingSearch) {
      const cleanQuery = debouncedQuery.trim().toLowerCase();
      if (cleanQuery) delete searchCache.current[cleanQuery];
      executeSearch(debouncedQuery);
    } else {
      fetchHome();
    }
  }, [isDisplayingSearch, debouncedQuery, executeSearch, fetchHome]);

  const isDisplayingSearch =
    searchQuery.trim().length > 0 && debouncedQuery.trim().length > 0;

  const activeData = isDisplayingSearch ? searchResults : popularBooks;
  const activeErrors = isDisplayingSearch ? searchErrors : homeErrors;
  const isLoading = isDisplayingSearch ? isSearchLoading : isHomeLoading;

  const sections = buildSections(activeData, activeErrors, sourceOrder);
  const hasAnyContent = activeData.length > 0 || Object.keys(activeErrors).length > 0;

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.searchContainer}>
        <TextInput
          style={styles.searchBar}
          placeholder="Search classics in English..."
          placeholderTextColor="#666666"
          value={searchQuery}
          onChangeText={handleSearchTextChange}
          clearButtonMode="while-editing"
          autoCorrect={false}
        />
        {searchQuery.length > 0 && (
          <TouchableOpacity
            style={styles.clearTextContainer}
            onPress={() => handleSearchTextChange('')}
          >
            <Text style={styles.clearTextButton}>✕</Text>
          </TouchableOpacity>
        )}
      </View>

      {isSearchLoading && <View style={styles.miniProgressBar} />}

      {isLoading && !hasAnyContent ? (
        <View style={styles.centered}>
          <ActivityIndicator size="large" color="#008080" />
          <Text style={styles.loadingText}>
            {isDisplayingSearch
              ? `Searching indices for "${debouncedQuery}"...`
              : 'Synchronizing secure public index...'}
          </Text>
        </View>
      ) : hasAnyContent ? (
        <ScrollView
          refreshControl={
            <RefreshControl refreshing={false} onRefresh={handleRefresh} tintColor="#008080" />
          }
        >
          {!isDisplayingSearch && recentBooks.length > 0 && (
            <View style={styles.recentSection}>
              <Text style={styles.sectionTitle}>Continue Reading</Text>
              <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.horizontalList}>
                {recentBooks.map((item) => (
                  <TouchableOpacity
                    key={item.bookId}
                    style={styles.recentCard}
                    activeOpacity={0.7}
                    onPress={() =>
                      router.push({
                        pathname: "/reader",
                        params: {
                          id: item.bookId,
                          title: item.bookTitle,
                          bookUrl: item.bookUrl || "",
                          coverUrl: item.coverUrl || "",
                          epubUrl: item.epubUrl || "",
                        },
                      })
                    }
                  >
                    <Image
                      source={{ uri: item.coverUrl || undefined }}
                      style={styles.recentCover}
                      resizeMode="cover"
                    />
                    <Text style={styles.recentTitle} numberOfLines={1}>
                      {item.bookTitle}
                    </Text>
                    <View style={styles.recentProgressBar}>
                      <View
                        style={[
                          styles.recentProgressFill,
                          { width: `${Math.min((item.characterOffset / 100000) * 100, 100)}%` },
                        ]}
                      />
                    </View>
                  </TouchableOpacity>
                ))}
              </ScrollView>
            </View>
          )}

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
                          pathname: '/reader',
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
                        {book.author || 'Unknown Author'}
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
      ) : isDisplayingSearch && !isLoading ? (
        <View style={styles.centered}>
          <Text style={styles.emptyText}>{`No English results for "${searchQuery}"`}</Text>
          <Text style={styles.emptySubtext}>
            Check spellings or browse global legendary classics.
          </Text>
        </View>
      ) : !isLoading ? (
        <View style={styles.centered}>
          <Text style={styles.emptyText}>No books available right now</Text>
          <Text style={styles.emptySubtext}>Try again in a moment.</Text>
        </View>
      ) : null}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#121212' },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 15,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderColor: '#1f1f1f',
  },
  searchBar: {
    flex: 1,
    backgroundColor: '#1a1a1a',
    color: '#ffffff',
    height: 46,
    borderRadius: 8,
    paddingHorizontal: 15,
    fontSize: 15,
    borderWidth: 1,
    borderColor: '#2d2d2d',
    paddingRight: 40,
  },
  clearTextContainer: { position: 'absolute', right: 30, padding: 5 },
  clearTextButton: { color: '#666666', fontSize: 16, fontWeight: 'bold' },
  miniProgressBar: {
    height: 2,
    backgroundColor: '#00ffff',
    width: '100%',
    position: 'absolute',
    top: 71,
    zIndex: 10,
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 40,
  },
  loadingText: { marginTop: 15, color: '#aaaaaa', fontSize: 14, textAlign: 'center' },
  emptyText: { color: '#ffffff', fontSize: 16, fontWeight: 'bold', textAlign: 'center' },
  emptySubtext: { color: '#666666', fontSize: 13, textAlign: 'center', marginTop: 6 },

  section: { marginBottom: 24 },
  sectionTitle: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: 'bold',
    paddingHorizontal: 16,
    marginBottom: 12,
  },
  horizontalList: { paddingHorizontal: 12, gap: 10 },
  horizontalCard: { width: CARD_WIDTH },
  horizontalCover: {
    width: CARD_WIDTH,
    height: CARD_HEIGHT,
    borderRadius: 6,
    backgroundColor: '#262626',
  },
  horizontalTitle: {
    color: '#ffffff',
    fontSize: 11,
    lineHeight: 14,
    marginTop: 6,
  },
  horizontalAuthor: {
    color: '#aaaaaa',
    fontSize: 10,
    marginTop: 2,
  },
  errorCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#3a1a1a',
    borderColor: '#ff6b6b',
    borderWidth: 1,
    borderRadius: 8,
    marginHorizontal: 16,
    padding: 12,
  },
  errorIcon: { color: '#ff6b6b', fontSize: 16, marginRight: 10 },
  errorMessage: { color: '#ff6b6b', fontSize: 13, flex: 1 },

  recentSection: { marginBottom: 24 },
  recentCard: { width: 100 },
  recentCover: { width: 100, height: 145, borderRadius: 6, backgroundColor: '#262626' },
  recentTitle: { color: '#ffffff', fontSize: 11, marginTop: 6, lineHeight: 14 },
  recentProgressBar: { height: 3, backgroundColor: '#2d2d2d', borderRadius: 2, marginTop: 6, overflow: 'hidden' },
  recentProgressFill: { height: '100%', backgroundColor: '#008080', borderRadius: 2 },
});
