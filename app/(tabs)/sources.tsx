import React, { useCallback, useEffect, useState } from "react";
import {
  StyleSheet,
  Switch,
  Text,
  View,
  FlatList,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { sourceManager, SourceInfo } from "../../sources/SourceManager";

export default function SourcesScreen() {
  const [sources, setSources] = useState<SourceInfo[]>([]);

  useEffect(() => {
    setSources(sourceManager.getAllSources());
  }, []);

  const handleToggle = useCallback((name: string, value: boolean) => {
    sourceManager.toggleSource(name, value);
    setSources(sourceManager.getAllSources());
  }, []);

  const renderItem = ({ item }: { item: SourceInfo }) => (
    <View style={styles.row}>
      <View style={styles.info}>
        <Text style={styles.name}>{item.name}</Text>
        <Text style={styles.status}>
          {item.enabled ? "Active" : "Disabled"}
        </Text>
      </View>
      <Switch
        value={item.enabled}
        onValueChange={(v) => handleToggle(item.name, v)}
        trackColor={{ false: "#2d2d2d", true: "#008080" }}
        thumbColor={item.enabled ? "#ffffff" : "#666666"}
      />
    </View>
  );

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.heading}>Book Sources</Text>
      <Text style={styles.subtext}>
        Toggle sources on or off. Disabled sources will not appear in search or
        browse results.
      </Text>
      <FlatList
        data={sources}
        renderItem={renderItem}
        keyExtractor={(item) => item.name}
        contentContainerStyle={styles.list}
      />
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
  subtext: {
    color: "#666666",
    fontSize: 13,
    paddingHorizontal: 20,
    paddingBottom: 16,
    lineHeight: 18,
  },
  list: { paddingHorizontal: 16 },
  row: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    backgroundColor: "#1a1a1a",
    borderRadius: 10,
    padding: 16,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: "#262626",
  },
  info: { flex: 1, paddingRight: 12 },
  name: { color: "#ffffff", fontSize: 16, fontWeight: "bold", marginBottom: 4 },
  status: { color: "#666666", fontSize: 12 },
});
