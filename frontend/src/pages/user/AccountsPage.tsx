import { useEffect, useState } from 'react';
import api from '../../lib/api';
import { Plus, CreditCard, X, Wallet } from 'lucide-react';

export default function AccountsPage() {
  const [accounts, setAccounts] = useState<any[]>([]);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ accountHolderName: '', accountType: 'SAVINGS', currency: 'INR' });
  const [loading, setLoading] = useState(false);

  const fetchAccounts = () => {
    api.get('/user/accounts').then((res) => setAccounts(res.data.data || [])).catch(() => {});
  };
  useEffect(fetchAccounts, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault(); setLoading(true);
    try { await api.post('/user/accounts', form); setShowCreate(false); setForm({ accountHolderName: '', accountType: 'SAVINGS', currency: 'INR' }); fetchAccounts(); }
    catch { alert('Failed to create account'); }
    finally { setLoading(false); }
  };

  const currencies = [
    { code: 'INR', symbol: '₹', flag: '🇮🇳' },
    { code: 'USD', symbol: '$', flag: '🇺🇸' },
    { code: 'EUR', symbol: '€', flag: '🇪🇺' },
    { code: 'GBP', symbol: '£', flag: '🇬🇧' },
  ];

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Accounts</h1>
          <p className="text-sm text-[var(--text-muted)] mt-1">Manage your bank accounts and currencies</p>
        </div>
        <button onClick={() => setShowCreate(true)} className="btn-primary flex items-center gap-2 text-sm">
          <Plus size={16} /> New Account
        </button>
      </div>

      {/* Create Account Modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" onClick={() => setShowCreate(false)}>
          <div className="glass-card p-8 w-full max-w-md animate-slide-up" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-bold tracking-tight">Open New Account</h3>
              <button onClick={() => setShowCreate(false)} className="w-8 h-8 rounded-lg bg-[var(--bg-elevated)] flex items-center justify-center text-[var(--text-muted)] hover:text-white transition-colors">
                <X size={16} />
              </button>
            </div>
            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider mb-2">Account Holder</label>
                <input type="text" value={form.accountHolderName} onChange={(e) => setForm({...form, accountHolderName: e.target.value})} required className="input-field" placeholder="Full legal name" />
              </div>
              <div>
                <label className="block text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider mb-2">Account Type</label>
                <div className="grid grid-cols-2 gap-3">
                  {['SAVINGS', 'CURRENT'].map(type => (
                    <button key={type} type="button" onClick={() => setForm({...form, accountType: type})}
                      className={`p-3 rounded-xl border text-sm font-medium transition-all ${form.accountType === type ? 'border-[var(--accent)] bg-[var(--accent)]/5 text-[var(--accent)]' : 'border-[var(--border)] text-[var(--text-secondary)] hover:border-[var(--border-hover)]'}`}>
                      {type.charAt(0) + type.slice(1).toLowerCase()}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider mb-2">Currency</label>
                <div className="grid grid-cols-4 gap-2">
                  {currencies.map(c => (
                    <button key={c.code} type="button" onClick={() => setForm({...form, currency: c.code})}
                      className={`p-3 rounded-xl border text-center transition-all ${form.currency === c.code ? 'border-[var(--accent)] bg-[var(--accent)]/5' : 'border-[var(--border)] hover:border-[var(--border-hover)]'}`}>
                      <span className="text-lg">{c.flag}</span>
                      <p className="text-xs font-medium mt-1">{c.code}</p>
                    </button>
                  ))}
                </div>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="submit" disabled={loading} className="btn-primary flex-1">{loading ? 'Creating...' : 'Create Account'}</button>
                <button type="button" onClick={() => setShowCreate(false)} className="btn-ghost">Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Account Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {accounts.map((acc: any, i: number) => {
          const cur = currencies.find(c => c.code === acc.currency) || currencies[0];
          return (
            <div key={acc.id} className="glass-card glass-card-hover p-6 animate-slide-up" style={{ animationDelay: `${i * 0.08}s` }}>
              <div className="flex items-start justify-between mb-5">
                <div className="w-12 h-12 rounded-2xl bg-[var(--accent)]/10 flex items-center justify-center">
                  <CreditCard size={20} className="text-[var(--accent)]" />
                </div>
                <span className={`badge ${acc.status === 'ACTIVE' ? 'bg-[var(--success)]/10 text-[var(--success)]' : 'bg-[var(--danger)]/10 text-[var(--danger)]'}`}>
                  {acc.status}
                </span>
              </div>
              <div className="space-y-1 mb-5">
                <p className="text-xs text-[var(--text-muted)] font-mono tracking-wider">{acc.accountNumber}</p>
                <div className="flex items-center gap-2">
                  <span className="text-sm">{cur.flag}</span>
                  <span className="text-xs text-[var(--text-muted)] uppercase tracking-wider">{acc.accountType} • {acc.currency}</span>
                </div>
              </div>
              <p className="text-3xl font-bold tracking-tight">
                {cur.symbol}{parseFloat(acc.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </p>
              <p className="text-xs text-[var(--text-muted)] mt-2">{acc.accountHolderName}</p>
            </div>
          );
        })}

        {/* Add new card */}
        <button onClick={() => setShowCreate(true)}
          className="glass-card p-6 flex flex-col items-center justify-center gap-3 border-dashed border-[var(--border)] hover:border-[var(--accent)]/30 transition-all group min-h-[200px]">
          <div className="w-12 h-12 rounded-2xl bg-[var(--bg-elevated)] flex items-center justify-center group-hover:bg-[var(--accent)]/10 transition-all">
            <Plus size={20} className="text-[var(--text-muted)] group-hover:text-[var(--accent)] transition-colors" />
          </div>
          <p className="text-sm font-medium text-[var(--text-muted)] group-hover:text-[var(--text-primary)] transition-colors">Open New Account</p>
        </button>
      </div>
    </div>
  );
}
