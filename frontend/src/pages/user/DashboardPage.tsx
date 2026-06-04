import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../lib/api';
import { useAuthStore } from '../../store/authStore';
import { Wallet, ArrowUpRight, ArrowDownRight, TrendingUp, CreditCard, ArrowLeftRight, Plus, Sparkles, Globe } from 'lucide-react';

function AnimatedNumber({ value, prefix = '', suffix = '' }: { value: number; prefix?: string; suffix?: string }) {
  const [display, setDisplay] = useState(0);
  useEffect(() => {
    const duration = 1200;
    const start = Date.now();
    const animate = () => {
      const elapsed = Date.now() - start;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 4);
      setDisplay(Math.floor(value * eased));
      if (progress < 1) requestAnimationFrame(animate);
      else setDisplay(value);
    };
    animate();
  }, [value]);
  return <>{prefix}{display.toLocaleString('en-IN', { minimumFractionDigits: suffix ? 2 : 0, maximumFractionDigits: 2 })}{suffix}</>;
}

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const [accounts, setAccounts] = useState<any[]>([]);
  const [totalBalance, setTotalBalance] = useState(0);

  useEffect(() => {
    api.get('/user/accounts').then((res) => {
      const accts = res.data.data || [];
      setAccounts(accts);
      setTotalBalance(accts.reduce((sum: number, a: any) => sum + parseFloat(a.balance || 0), 0));
    }).catch(() => {});
  }, []);

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Bento Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Main Balance Card — 2 cols */}
        <div className="md:col-span-2 glass-card p-6 relative overflow-hidden group">
          <div className="floating-gradient w-[300px] h-[300px] bg-[var(--accent)] -top-20 -right-20" />
          <div className="relative z-10">
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-[var(--accent)]/10 flex items-center justify-center">
                  <Wallet size={16} className="text-[var(--accent)]" />
                </div>
                <span className="text-xs text-[var(--text-muted)] uppercase tracking-wider font-medium">Total Balance</span>
              </div>
              <div className="badge bg-[var(--success)]/10 text-[var(--success)]">
                <div className="w-1.5 h-1.5 rounded-full bg-[var(--success)] mr-1.5 animate-pulse" />
                Live
              </div>
            </div>
            <p className="text-4xl lg:text-5xl font-black tracking-tight animate-count-up">
              ₹<AnimatedNumber value={totalBalance} />
            </p>
            <p className="text-sm text-[var(--text-muted)] mt-2">{accounts.length} active account{accounts.length !== 1 ? 's' : ''}</p>
          </div>
        </div>

        {/* Income Card */}
        <div className="glass-card glass-card-hover p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="w-10 h-10 rounded-xl bg-[var(--success)]/10 flex items-center justify-center">
              <ArrowDownRight size={18} className="text-[var(--success)]" />
            </div>
            <span className="badge bg-[var(--success)]/10 text-[var(--success)]">+12.5%</span>
          </div>
          <p className="text-2xl font-bold tracking-tight">₹0</p>
          <p className="text-xs text-[var(--text-muted)] mt-1">Income this month</p>
        </div>

        {/* Expenses Card */}
        <div className="glass-card glass-card-hover p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="w-10 h-10 rounded-xl bg-[var(--danger)]/10 flex items-center justify-center">
              <ArrowUpRight size={18} className="text-[var(--danger)]" />
            </div>
            <span className="badge bg-[var(--danger)]/10 text-[var(--danger)]">-3.2%</span>
          </div>
          <p className="text-2xl font-bold tracking-tight">₹0</p>
          <p className="text-xs text-[var(--text-muted)] mt-1">Spent this month</p>
        </div>
      </div>

      {/* Quick Actions — Bento Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { to: '/transfer', icon: ArrowLeftRight, label: 'Transfer', desc: 'Send money', color: 'var(--accent)' },
          { to: '/accounts', icon: Plus, label: 'New Account', desc: 'Open account', color: '#06b6d4' },
          { to: '/history', icon: TrendingUp, label: 'Analytics', desc: 'View trends', color: '#f59e0b' },
          { to: '/accounts', icon: Globe, label: 'Multi-Currency', desc: 'Manage FX', color: '#a855f7' },
        ].map((action) => (
          <Link key={action.label} to={action.to}
            className="glass-card glass-card-hover p-5 group cursor-pointer">
            <div className="w-11 h-11 rounded-xl flex items-center justify-center mb-4 transition-transform duration-300 group-hover:scale-110"
              style={{ background: `${action.color}15` }}>
              <action.icon size={20} style={{ color: action.color }} />
            </div>
            <p className="font-semibold text-sm">{action.label}</p>
            <p className="text-xs text-[var(--text-muted)] mt-0.5">{action.desc}</p>
          </Link>
        ))}
      </div>

      {/* Accounts Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold tracking-tight">Your Accounts</h2>
          <Link to="/accounts" className="text-xs text-[var(--accent)] hover:underline font-medium flex items-center gap-1">
            View all <ArrowUpRight size={12} />
          </Link>
        </div>

        {accounts.length === 0 ? (
          <div className="glass-card p-10 text-center">
            <div className="w-16 h-16 rounded-2xl bg-[var(--bg-elevated)] flex items-center justify-center mx-auto mb-4">
              <Sparkles className="text-[var(--accent)]" size={24} />
            </div>
            <h3 className="font-semibold mb-1">No accounts yet</h3>
            <p className="text-sm text-[var(--text-muted)] mb-5">Create your first bank account to get started</p>
            <Link to="/accounts" className="btn-primary inline-flex items-center gap-2 text-sm">
              <Plus size={16} /> Create Account
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {accounts.map((acc: any, i: number) => (
              <div key={acc.id} className="glass-card glass-card-hover p-5 animate-slide-up" style={{ animationDelay: `${i * 0.1}s` }}>
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-[var(--accent)]/10 flex items-center justify-center">
                      <CreditCard size={18} className="text-[var(--accent)]" />
                    </div>
                    <div>
                      <p className="text-xs text-[var(--text-muted)]">{acc.accountNumber}</p>
                      <p className="text-[10px] text-[var(--text-muted)] uppercase tracking-wider">{acc.accountType} • {acc.currency}</p>
                    </div>
                  </div>
                  <span className={`badge ${acc.status === 'ACTIVE' ? 'bg-[var(--success)]/10 text-[var(--success)]' : 'bg-[var(--danger)]/10 text-[var(--danger)]'}`}>
                    {acc.status}
                  </span>
                </div>
                <p className="text-2xl font-bold tracking-tight">
                  {acc.currency} {parseFloat(acc.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
