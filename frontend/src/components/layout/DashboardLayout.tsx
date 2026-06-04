import { NavLink, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { useThemeStore } from '../../store/themeStore';
import { LayoutDashboard, CreditCard, ArrowLeftRight, Clock, Shield, LogOut, Menu, X, Zap, Bell, Sun, Moon } from 'lucide-react';
import { useState } from 'react';

const navItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Overview' },
  { to: '/accounts', icon: CreditCard, label: 'Accounts' },
  { to: '/transfer', icon: ArrowLeftRight, label: 'Transfers' },
  { to: '/history', icon: Clock, label: 'History' },
];

export default function DashboardLayout() {
  const { user, logout } = useAuthStore();
  const { resolvedTheme, setTheme } = useThemeStore();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const isAdmin = user?.roles.includes('ADMIN');

  const toggleTheme = () => {
    setTheme(resolvedTheme === 'dark' ? 'light' : 'dark');
  };

  return (
    <div className="flex min-h-screen bg-[var(--bg-primary)]">
      {/* Sidebar */}
      <aside className={`fixed inset-y-0 left-0 z-50 w-[260px] bg-[var(--bg-secondary)] border-r border-[var(--border)] transform transition-transform duration-300 ease-out lg:translate-x-0 lg:static flex flex-col ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}`}>

        {/* Logo */}
        <div className="flex items-center gap-3 px-6 py-6">
          <div className="w-9 h-9 rounded-xl bg-[var(--accent)] flex items-center justify-center">
            <Zap className="text-[var(--btn-primary-text)]" size={18} strokeWidth={2.5} />
          </div>
          <div>
            <h1 className="text-base font-bold tracking-tight">NexBank</h1>
            <p className="text-[10px] text-[var(--text-muted)] uppercase tracking-widest font-medium">Digital Banking</p>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 px-3 py-2">
          <p className="px-4 mb-2 text-[10px] font-semibold text-[var(--text-muted)] uppercase tracking-widest">Menu</p>
          <div className="space-y-1">
            {navItems.map((item) => (
              <NavLink key={item.to} to={item.to} onClick={() => setSidebarOpen(false)}
                className={({ isActive }) => `flex items-center gap-3 px-4 py-2.5 rounded-xl text-[13px] font-medium transition-all duration-200 ${
                  isActive
                    ? 'bg-[var(--accent-10)] text-[var(--accent)]'
                    : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-elevated)]'
                }`}>
                <item.icon size={18} />
                {item.label}
              </NavLink>
            ))}
          </div>

          {isAdmin && (
            <div className="mt-6">
              <p className="px-4 mb-2 text-[10px] font-semibold text-[var(--text-muted)] uppercase tracking-widest">Admin</p>
              <NavLink to="/admin" onClick={() => setSidebarOpen(false)}
                className={({ isActive }) => `flex items-center gap-3 px-4 py-2.5 rounded-xl text-[13px] font-medium transition-all duration-200 ${
                  isActive
                    ? 'bg-[var(--accent-10)] text-[var(--accent)]'
                    : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-elevated)]'
                }`}>
                <Shield size={18} />
                Control Center
              </NavLink>
            </div>
          )}
        </nav>

        {/* User card */}
        <div className="p-3">
          <div className="glass-card p-4">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-9 h-9 rounded-full bg-gradient-to-br from-[var(--accent)] to-cyan-400 flex items-center justify-center text-[var(--btn-primary-text)] text-sm font-bold">
                {user?.fullName?.charAt(0) || 'U'}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold truncate">{user?.fullName}</p>
                <p className="text-[11px] text-[var(--text-muted)] truncate">{user?.email}</p>
              </div>
            </div>
            <button onClick={logout}
              className="flex items-center gap-2 w-full px-3 py-2 text-[12px] font-medium text-[var(--text-muted)] hover:text-red-400 hover:bg-red-500/5 rounded-lg transition-all">
              <LogOut size={14} /> Sign Out
            </button>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-h-screen">
        {/* Top bar */}
        <header className="sticky top-0 z-40 bg-[var(--bg-primary)]/80 backdrop-blur-2xl border-b border-[var(--border)] px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => setSidebarOpen(!sidebarOpen)} className="lg:hidden text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors">
              {sidebarOpen ? <X size={22} /> : <Menu size={22} />}
            </button>
            <div className="hidden lg:block">
              <p className="text-xs text-[var(--text-muted)]">Good {new Date().getHours() < 12 ? 'Morning' : new Date().getHours() < 17 ? 'Afternoon' : 'Evening'}</p>
              <p className="text-sm font-semibold">{user?.fullName}</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            {/* Theme Toggle */}
            <button
              onClick={toggleTheme}
              id="theme-toggle"
              title={`Switch to ${resolvedTheme === 'dark' ? 'light' : 'dark'} mode`}
              className="w-9 h-9 rounded-xl bg-[var(--bg-secondary)] border border-[var(--border)] flex items-center justify-center text-[var(--text-muted)] hover:text-[var(--accent)] hover:border-[var(--accent)] transition-all duration-300"
            >
              {resolvedTheme === 'dark' ? <Sun size={16} /> : <Moon size={16} />}
            </button>
            {/* Notifications */}
            <button className="w-9 h-9 rounded-xl bg-[var(--bg-secondary)] border border-[var(--border)] flex items-center justify-center text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:border-[var(--border-hover)] transition-all">
              <Bell size={16} />
            </button>
          </div>
        </header>

        <main className="flex-1 p-6 lg:p-8 overflow-auto">
          <Outlet />
        </main>
      </div>

      {/* Mobile overlay */}
      {sidebarOpen && <div className="fixed inset-0 z-40 bg-[var(--overlay-bg)] backdrop-blur-sm lg:hidden" onClick={() => setSidebarOpen(false)} />}
    </div>
  );
}
