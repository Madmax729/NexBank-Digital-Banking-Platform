import { create } from 'zustand';

type Theme = 'light' | 'dark' | 'system';

interface ThemeStore {
  theme: Theme;
  resolvedTheme: 'light' | 'dark';
  setTheme: (theme: Theme) => void;
  initialize: () => void;
}

export const useThemeStore = create<ThemeStore>((set, get) => ({
  theme: 'dark',
  resolvedTheme: 'dark',

  initialize: () => {
    const saved = localStorage.getItem('nexbank-theme') as Theme | null;
    const theme = saved || 'system';
    get().setTheme(theme);

    // Listen for system theme changes
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (get().theme === 'system') {
        const resolved = e.matches ? 'dark' : 'light';
        document.documentElement.setAttribute('data-theme', resolved);
        set({ resolvedTheme: resolved });
      }
    });
  },

  setTheme: (theme) => {
    const resolved = theme === 'system'
      ? (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
      : theme;
    document.documentElement.setAttribute('data-theme', resolved);
    localStorage.setItem('nexbank-theme', theme);
    set({ theme, resolvedTheme: resolved });
  },
}));
