import React, { createContext, useCallback, useContext, useReducer } from "react";

export type ReadingMode = "page" | "scroll";
export type ThemeBg = "white" | "sepia" | "dark";

export interface ReaderSettings {
  fontSize: number;
  lineHeight: number;
  readingMode: ReadingMode;
  themeBg: ThemeBg;
  brightness: number;
}

interface ReaderStoreState {
  settings: ReaderSettings;
  showMenu: boolean;
}

type Action =
  | { type: "adjustFontSize"; payload: number }
  | { type: "adjustLineHeight"; payload: number }
  | { type: "setFontSize"; payload: number }
  | { type: "setLineHeight"; payload: number }
  | { type: "setReadingMode"; payload: ReadingMode }
  | { type: "setThemeBg"; payload: ThemeBg }
  | { type: "setBrightness"; payload: number }
  | { type: "toggleMenu" }
  | { type: "setShowMenu"; payload: boolean }
  | { type: "setSettings"; payload: Partial<ReaderSettings> };

const MIN_FONT = 12;
const MAX_FONT = 32;
const MIN_LINE = 1.0;
const MAX_LINE = 2.5;

const defaultSettings: ReaderSettings = {
  fontSize: 18,
  lineHeight: 1.5,
  readingMode: "page",
  themeBg: "dark",
  brightness: 1,
};

function clampFontSize(v: number): number {
  return Math.max(MIN_FONT, Math.min(MAX_FONT, Math.round(v / 2) * 2));
}

function clampLineHeight(v: number): number {
  return Math.max(MIN_LINE, Math.min(MAX_LINE, Math.round(v * 10) / 10));
}

function reducer(state: ReaderStoreState, action: Action): ReaderStoreState {
  switch (action.type) {
    case "adjustFontSize":
      return {
        ...state,
        settings: {
          ...state.settings,
          fontSize: clampFontSize(state.settings.fontSize + action.payload),
        },
      };
    case "adjustLineHeight":
      return {
        ...state,
        settings: {
          ...state.settings,
          lineHeight: clampLineHeight(state.settings.lineHeight + action.payload),
        },
      };
    case "setFontSize":
      return {
        ...state,
        settings: {
          ...state.settings,
          fontSize: clampFontSize(action.payload),
        },
      };
    case "setLineHeight":
      return {
        ...state,
        settings: {
          ...state.settings,
          lineHeight: clampLineHeight(action.payload),
        },
      };
    case "setReadingMode":
      return {
        ...state,
        settings: { ...state.settings, readingMode: action.payload },
      };
    case "setThemeBg":
      return {
        ...state,
        settings: { ...state.settings, themeBg: action.payload },
      };
    case "setBrightness":
      return {
        ...state,
        settings: {
          ...state.settings,
          brightness: Math.max(0.1, Math.min(1, action.payload)),
        },
      };
    case "toggleMenu":
      return { ...state, showMenu: !state.showMenu };
    case "setShowMenu":
      return { ...state, showMenu: action.payload };
    case "setSettings":
      return {
        ...state,
        settings: { ...state.settings, ...action.payload },
      };
    default:
      return state;
  }
}

interface ReaderStoreContextValue {
  state: ReaderStoreState;
  increaseFont: () => void;
  decreaseFont: () => void;
  increaseLineHeight: () => void;
  decreaseLineHeight: () => void;
  setReadingMode: (mode: ReadingMode) => void;
  setThemeBg: (bg: ThemeBg) => void;
  setBrightness: (v: number) => void;
  toggleMenu: () => void;
  setShowMenu: (v: boolean) => void;
  setSettings: (s: Partial<ReaderSettings>) => void;
}

const ReaderStoreContext = createContext<ReaderStoreContextValue | null>(null);

export function ReaderStoreProvider({ children }: { children: React.ReactNode }) {
  const [state, dispatch] = useReducer(reducer, {
    settings: defaultSettings,
    showMenu: false,
  });

  const increaseFont = useCallback(() => dispatch({ type: "adjustFontSize", payload: 2 }), []);
  const decreaseFont = useCallback(() => dispatch({ type: "adjustFontSize", payload: -2 }), []);
  const increaseLineHeight = useCallback(() => dispatch({ type: "adjustLineHeight", payload: 0.2 }), []);
  const decreaseLineHeight = useCallback(() => dispatch({ type: "adjustLineHeight", payload: -0.2 }), []);
  const setReadingMode = useCallback((mode: ReadingMode) => dispatch({ type: "setReadingMode", payload: mode }), []);
  const setThemeBg = useCallback((bg: ThemeBg) => dispatch({ type: "setThemeBg", payload: bg }), []);
  const setBrightness = useCallback((v: number) => dispatch({ type: "setBrightness", payload: v }), []);
  const toggleMenu = useCallback(() => dispatch({ type: "toggleMenu" }), []);
  const setShowMenu = useCallback((v: boolean) => dispatch({ type: "setShowMenu", payload: v }), []);
  const setSettings = useCallback((s: Partial<ReaderSettings>) => dispatch({ type: "setSettings", payload: s }), []);

  const value: ReaderStoreContextValue = {
    state,
    increaseFont,
    decreaseFont,
    increaseLineHeight,
    decreaseLineHeight,
    setReadingMode,
    setThemeBg,
    setBrightness,
    toggleMenu,
    setShowMenu,
    setSettings,
  };

  return (
    <ReaderStoreContext.Provider value={value}>
      {children}
    </ReaderStoreContext.Provider>
  );
}

export function useReaderStore(): ReaderStoreContextValue {
  const ctx = useContext(ReaderStoreContext);
  if (!ctx) throw new Error("useReaderStore must be used within ReaderStoreProvider");
  return ctx;
}

export const THEME_BG_COLORS: Record<ThemeBg, { bg: string; text: string }> = {
  white: { bg: "#ffffff", text: "#1a1a1a" },
  sepia: { bg: "#f5e6c8", text: "#3b2b1a" },
  dark: { bg: "#121212", text: "#e0e0e0" },
};
