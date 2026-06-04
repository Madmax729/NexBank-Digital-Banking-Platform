import { create } from 'zustand';
import api from '../lib/api';

interface User {
  userId: string;
  email: string;
  fullName: string;
  roles: string[];
}

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (fullName: string, email: string, password: string, phoneNumber?: string) => Promise<void>;
  logout: () => void;
  initialize: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,

  initialize: () => {
    const token = localStorage.getItem('accessToken');
    const userData = localStorage.getItem('user');
    if (token && userData) {
      set({ user: JSON.parse(userData), isAuthenticated: true, isLoading: false });
    } else {
      set({ isLoading: false });
    }
  },

  login: async (email, password) => {
    const res = await api.post('/auth/login', { email, password });
    const { accessToken, refreshToken, userId, email: userEmail, fullName, roles } = res.data.data;
    const user = { userId, email: userEmail, fullName, roles: Array.from(roles) as string[] };
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(user));
    set({ user, isAuthenticated: true });
  },

  register: async (fullName, email, password, phoneNumber) => {
    const res = await api.post('/auth/register', { fullName, email, password, phoneNumber });
    const { accessToken, refreshToken, userId, email: userEmail, fullName: name, roles } = res.data.data;
    const user = { userId, email: userEmail, fullName: name, roles: Array.from(roles) as string[] };
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(user));
    set({ user, isAuthenticated: true });
  },

  logout: () => {
    api.post('/auth/logout').catch(() => {});
    localStorage.clear();
    set({ user: null, isAuthenticated: false });
  },
}));
